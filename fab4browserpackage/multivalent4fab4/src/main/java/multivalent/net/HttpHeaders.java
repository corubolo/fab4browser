package multivalent.net;

import java.net.URI;
import java.util.Map;
import java.util.Iterator;

import com.pt.net.HTTP;
import com.pt.net.MIME;

import multivalent.*;



/**
	Set HTTP headers: User-Agent, Accept, Accept-Encoding, Keep-Alive, Referer [sic], and so on.
	Possible to masquerade as another user agent by setting the <code>agent</code> attribute in the hub.

<!--
	LATER: Give referer policy as attribute: POLICY="never|always|samehost|ask"
-->

<!--
GET /openDocument?http://www.cs.berkeley.edu/ HTTP/1.1
Host: localhost:5549
User-Agent: Mozilla/5.0 (Windows; U; Win98; en-US; rv:0.9.8) Gecko/20020204
Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,
	video/x-mng,
	image/png,image/jpeg,image/gif;q=0.2,
	text/css,
	* /*;q=0.1
Accept-Language: en-us
Accept-Encoding: gzip, deflate, compress;q=0.9
Accept-Charset: ISO-8859-1, utf-8;q=0.66, *;q=0.66
Keep-Alive: 300
Connection: keep-alive
-->

	@version $Revision: 1.3 $ $Date: 2002/10/26 17:40:29 $
*/
public class HttpHeaders extends Behavior {
  public static final String ATTR_USER_AGENT = "User-Agent";
  public static final String ATTR_ACCEPT_LANGUAGE = "Accept-Language";

  /**
	Set of acceptable media types is given in Preferences.txt by MIME types in <code>mediaadaptor</code> lines.
	This can set the <code>q</code> values.
  public static final String ATTR_ACCEPT = "Accept";
  */

  private String agent_, language_;


  /** Stuffs "User-Agent: Multivalent m.n" in headers of HTTP traffic.  Done in <i>before</i> so it can be overridden. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	Object arg=se.getArg();
	if (Document.MSG_OPEN==msg && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;
		String scheme = di.uri.getScheme().toLowerCase();
		if ("http".equals(scheme) || "https".equals(scheme)) setHeaders(di.headers, di);
	}
	return false;
  }

  private void setHeaders(Map<String,String> headers, DocInfo di) {
	headers.put(HTTP.HEADER_USER_AGENT, agent_);
	//headers.put("User-Agent", "Mozilla/5.0 (Windows; U; Win98; en-US; rv:0.9.8) Gecko/20020204");

	String ae = (String)headers.get(HTTP.HEADER_ACCEPT_ENCODING);
	StringBuffer sb = new StringBuffer(ae==null || ae.length()==0? "gzip": ae.toLowerCase());
	if (sb.indexOf("gzip")==-1) sb.append(", gzip");
	if (sb.indexOf("deflate")==-1) sb.append(", deflate");
	headers.put(HTTP.HEADER_ACCEPT_ENCODING, sb.toString());


	// Extend mime types automatically from discovered media adaptors.
	String accept = (String)headers.get(HTTP.HEADER_ACCEPT);
	sb = new StringBuffer(accept==null || accept.length()==0? MIME.TYPE_TEXT_HTML: accept.toLowerCase());
	for (Iterator<String> i = Multivalent.getInstance().getGenreMap().keySet().iterator(); i.hasNext(); ) {
		String key = i.next().toLowerCase();
		if (key.indexOf('/')!=-1 && sb.indexOf(key)==-1) sb.append(", ").append(key);
	}
	if (sb.indexOf("*/*")==-1) sb.append(", */*");
	//sb.append(",text/xml,application/xml,application/xhtml+xml");   // what does wired news want?
	headers.put(HTTP.HEADER_ACCEPT, sb.toString());

	headers.put(HTTP.HEADER_ACCEPT_LANGUAGE, language_);
	headers.put(HTTP.HEADER_ACCEPT_CHARSET, "ISO-8859-1, utf-8");
	headers.put(HTTP.HEADER_CONNECTION, "keep-alive");
	headers.put(HTTP.HEADER_KEEP_ALIVE, "300");

	// Set "referer" [sic] iff new document in same domain as old.  (HTTP spells "referer" that way to remain compatible with an early misspelling.)
	URI docURI = di.doc!=null? di.doc.getURI(): null;
//System.out.println("*** ADD REFERER?  "+docURI+" vs "+di.uri);
	if (docURI!=null && "http".equalsIgnoreCase(di.uri.getScheme()) && docURI.getHost()!=null && docURI.getAuthority().equalsIgnoreCase(di.uri.getAuthority())) {
//System.out.println("*** ADD REFERER ***");
		di.headers.put(HTTP.HEADER_REFERRER, docURI.toString());
	}

	//if (uri.getHost()!=null) headers.put("Host", uri.getHost()); => have to reset on redirect
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);
	agent_ = getAttr(ATTR_USER_AGENT, "Multivalent/1.0");
	language_ = getAttr(ATTR_ACCEPT_LANGUAGE, "en-us");	// take from current locale
	//accept_ = getAttr(ATTR_ACCEPT, "");
  }
}
