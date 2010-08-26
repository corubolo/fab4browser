package multivalent.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import phelps.net.URIs;
import phelps.net.RobustHyperlink;

import com.pt.io.InputUniString;
import com.pt.net.MIME;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.SemanticEvent;
import multivalent.Document;
import multivalent.INode;
import multivalent.MediaAdaptor;
//import multivalent.std.adaptor.HTML;



/**
	Standard fix ups to DocInfo: URL
	When SystemEvents get {@link Document#MSG_OPEN} with a String URL, it passes it around for people to fix up.

	If incoming URL not robust, sign it in background and store in table (gdbm or rdbms).
	If 404 on robust URL, offer to fix it.

	@see <a href='http://www.cs.berkeley.edu/~phelps/Robust/'>Robust Web Site</a>

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:23:19 $
*/
public class Robust extends Behavior {
  private static final boolean DEBUG = false;

  //boolean fix_=false;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	Object arg=se.getArg();
	if (arg==null) {
		// ignore
	} else if (Document.MSG_OPEN==msg && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;
		// strip signature, if any, just to be safe
		String surl = di.uri.toString();
if (DEBUG) System.out.println("*** "+RobustHyperlink.stripSignature(surl)+"  +  "+RobustHyperlink.getSignature(surl));
		//try { di.uri = new URL(RobustHyperlink.stripSignature(surl)); } catch (MalformedURLException canthappen) {}
		di.attrs.put("lexical-signature", RobustHyperlink.getSignature(surl));
if (DEBUG) System.out.println("stripping to "+di.uri);

	} else if (Document.MSG_OPENED==msg && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;
		// if had signature, restore
		String surl = di.uri.toString();
		String sig = (String)di.attrs.get("lexical-signature");
		if (sig!=null) try { di.uri = new URI(surl+sig); } catch (URISyntaxException canthappen) {}
		// if not, get from cache if available, else send to cache to compute for map and next time


if (DEBUG) System.out.println("Robust: return code = "+di.returncode+", sig="+sig+", new URL="+di.uri);
		if (di.returncode==404) {
//			System.out.println("*** ROBUST REPAIR ***: "+sig+"/"+RobustHyperlink.getSignatureWords(sig));
			String grace = "it's a Robust Hyperlink";
			//if (sig==null) { try map; grace="in map"; }

			if (sig!=null) {
				// insert into server's HTML
				//System.out.println(report(grace, surl, RobustHyperlink.getSignatureWords(sig)));
				Browser br = getBrowser();
				MediaAdaptor html = (MediaAdaptor)Behavior.getInstance("helper","HTML",null, getLayer());
				try {
					Document doc = br.getCurDocument();
					/* this works
					html.parse(report(grace, surl, RobustHyperlink.getSignatureWords(sig)), doc.getParentNode());
					doc.remove();	// remove old content
					*/
					// splice into server's 404 message

					//doc.dump();
					//IVBox ivbox = new IVBox("STACK",null, doc.getParentNode());
					html.setInput(new InputUniString(report(grace, surl, RobustHyperlink.getSignatureWords(sig)), MIME.TYPE_TEXT_HTML));
					INode full = (INode)html.parse(doc);
					full.remove();	// had to attach temporarily to establish paths to root and so on
					full = (INode)full.findBFS("div");
					//System.out.println("full dump ***********");
					//full.dump();

					INode insert = (INode)doc.findBFS("body");
					//System.out.println("insert = "+insert);
					if (insert!=null) insert.insertChildAt(full,0);
					//insert.dump();
/*					//full.dump();
					Document fdoc = (Document)full.getParentNode();
					fdoc.bbox.setSize(0,300);

					doc.insertChildAt(full,0);
					//ivbox.appendChild(full);
					//ivbox.appendChild(doc);
					//doc.dump();*/
				} catch (Exception canthappen) {
					System.out.println("Exception in Robust: "+canthappen);
					canthappen.printStackTrace();
				} finally { try { html.close(); } catch (java.io.IOException ioe) {} }
			}
		}
	}
	return false;
  }


  String report(String grace, String url, String signature) {
	signature = URIs.encode(signature);

	StringBuffer sb = new StringBuffer(2000);	// measure this
	sb.append("<html><body background='#ff8080'>\n");
	sb.append("<div>");
	sb.append("<h3>").append("404 Page Not Found - fortunately, ").append(grace).append("</h3>\n");
	sb.append("<p>The web page formerly at URL ").append(url).append(" no longer exists there.\n");
	sb.append("<p>But since this is a <a href='http://www.cs.berkeley.edu/~phelps/Robust/'>robust hyperlink</a>, ");
	sb.append("you can perform a <i>content-based</i> search for it to see if it has moved elsewhere on the Web.\n");
	sb.append("<p>In the future, this can take place automatically, in a search engine of your preference.  For now, try ");
	sb.append("<a href='http://www.google.com/search?num=10&q=").append(signature).append("'>Google</a>, ");
	sb.append("<a href='http://ink.yahoo.com/bin/query?z=2&hc=0&hs=0&p=").append(signature).append("'>Yahoo (powered by Inktomi)</a>, ");
	sb.append("<a href='http://www.altavista.com/cgi-bin/query?pg=q&sc=on&kl=XX&stype=stext&q=").append(signature).append("'>Alta Vista</a>, ");
	sb.append(" or any other web search engine for the following words:");
	StringTokenizer st = new StringTokenizer(signature, "+"); while (st.hasMoreTokens()) sb.append(' ').append(st.nextToken());
	sb.append("<p><hr><p><p>");
	sb.append("<div>");
	sb.append("\n</body></html>\n");
//System.out.println(sb);
//System.out.println("length of report = "+sb.length());
	return sb.substring(0);
  }
}
