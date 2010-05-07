package multivalent.net;

import java.io.*;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.ParseException;

import com.pt.io.Cache;
import com.pt.io.InputUni;
import com.pt.net.HTTP;

import multivalent.*;



/**
	Implements both
	<a href='http://www.netscape.com/newsref/std/cookie_spec.html'>Netscape spec</a> and
	<a href='http://www.w3.org/Protocols/rfc2109/rfc2109'>W3C spec</a>
	<!--a href="where?">Cookies v2</a-->.

	<p>For example:
	<blockquote><tt>Set-cookie: RMID=3fc51f1b38b7eeb0; expires=Fri, 31-Dec-2010 23:59:59 GMT; path=/; domain=.nytimes.com</tt></blockquote>

	<p>LATER: periodically save cookies, take cookie policy (none, all, ask, only from same host) from preferences.

	@version $Revision: 1.5 $ $Date: 2005/01/03 09:23:58 $
*/
public class Cookies extends Behavior {
  /** Filename of saved cookies. */
  public static final String FILENAME = "Cookies.txt";

  /** Inner class that holds parsed cookie string. */
  static class Cookie {
	long expires=-1;	// defaults to expire when session ends
	String in=null;	// Java can't write out dates it can read back in!	So keep around the date send by the server => and substring keeps copy of original string, so may as well write that out
	String domain=null;
	String path="/";
	String namevals=null;
	boolean secure=false;

	Cookie(String host, String nvs) {
		in = nvs;	// what to save out

		if (nvs!=null && nvs.length()>0) {
		StringBuffer sb = new StringBuffer(nvs.length());
		for (String tok: nvs.split("\\s;\\s")) {
			int eqi=tok.indexOf('='); if (eqi<=0 || eqi+1==tok.length()) continue;
			String name=tok.substring(0,eqi).toLowerCase().trim(), val=tok.substring(eqi+1).trim();
//System.out.println(tok);
//System.out.println("|"+name+"| = |"+val+"|");
			if ("expires".equals(name)) {
				try { expires = HTTP.parseDate(val); } catch (ParseException pe) {}
//System.out.println("expires = "+expires+" / "+val);
			} else if ("max-age".equals(name)) {
				expires = System.currentTimeMillis();
				try { expires += 1000*Integer.parseInt(val); } catch (NumberFormatException nfe) {}
			} else if ("domain".equals(name)) domain=val;
			else if ("path".equals(name)) path=val;
			else if ("secure".equals(name)) secure=true;
			else if ("comment".equals(name) || "version".equals(name)) {
				// ignore
			} else {
				if (sb.length()>0) sb.append("; ");
				sb.append(tok);
			}
		}
		if (sb.length()>0) namevals=sb.substring(0);	// if (namevals==null), worthless cookie
		if (domain==null) { domain=host; in+="; domain="+domain; }
		}
	}
	/*public Cookie(Long expires, String domain, String path, String namevals) {
		this.expires=expires; this.domain=domain; this.path=path; this.namevals=namevals;
	}*/
	public String toString() { return namevals+" in "+domain+path; }
  }


  private static List<Cookie> Cookies_ = null;	// if performance becomes an issue, keep sorted by primary=domain, secondary=path and do binary search



  private void read() {
	if (Cookies_!=null) return;

	getLogger().fine("reading cookies");
	Cookies_ = new ArrayList<Cookie>(100);
	try {	// f.canRead(), but have to check for exception anyhow
		InputUni iu = getGlobal().getCache().getInputUni(null, FILENAME, Cache.GROUP_PERSONAL);
		if (iu!=null) {
			BufferedReader rr = new BufferedReader(iu.getReader());	// already buffered but want readLine()
			// know to be unique so don't need to filter through slower put()
			for (String line; (line=rr.readLine())!=null; ) Cookies_.add(new Cookie(null, line));
			rr.close();
		}
	} catch (IOException ioe) {}
  }

  //public ESISNode save() { -- nothing in protocol
  private void write() {
	read();
	// should first write temp file to preserve against data loss
	Cache cache = getGlobal().getCache();
	getLogger().fine("writing cookies");
	try {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(cache.getOutputStream(null, FILENAME, Cache.GROUP_PERSONAL)));
		long now = System.currentTimeMillis();
	    for (int i=0,imax=Cookies_.size(); i<imax; i++) {
			Cookie c = Cookies_.get(i);
			if (c.expires > now) {	// clean out expired cookies
				w.write(c.in); w.newLine();
			}
		}
		w.close();

		// now rename to safely replace old cookies with new
		// ...

	} catch (IOException ioe) {}
  }

  /** Updates existing or adds new cookie to database. */
  private void put(String host, String namevals) {
	if (namevals==null || namevals.indexOf('=')==-1) return;
	read();

	Cookie newcookie = new Cookie(host, namevals);
	String domain=newcookie.domain, path=newcookie.path;
//System.out.println("cookie #"+Cookies.size()+" = "+namevals+" => "+newcookie.namevals+", "+newcookie.domain);
//System.out.println("new cookie "+newcookie+", expires="+newcookie.expires+", >now? "+(newcookie.expires>System.currentTimeMillis()));
	if (newcookie.namevals==null || newcookie.expires<System.currentTimeMillis() /*|| !host.endsWith(domain)*/ || host.endsWith(".doubleclick.net")) return;	// refuse cookies that don't match originating domain?

	// It's a keeper.  Maybe replace existing cookie.
	boolean replace=false; int inx=0;
	for (int i=0,imax=Cookies_.size(); i<imax; i++) {
		Cookie c = Cookies_.get(i);
		if (domain.equals(c.domain) && path.equals(c.path)) { Cookies_.set(inx, c); replace=true; break; }
	}
	if (!replace) Cookies_.add(newcookie);
//System.out.println((replace?"replace ":"new ")+newcookie);
  }

  /**
	Return all cookies matching domain and path.
	<a href='http://www.netscape.com/newsref/std/cookie_spec.html'>Netscape spec</a>:
	"When requesting a URL from an HTTP server, the browser will match the URL against all cookies
	and	if any of them match, a line containing the name/value pairs of all matching cookies
	will be included in	the HTTP request."
  */
  private String getAll(String domain, String path) {
	if (domain==null || path==null) return null;
	read();
	// if redirect, can build up duplicates of cookies, and no reason to seed ci with cookies, so ignore existing ones
//	if ((namevals=(String)ci.headers.get("Cookie"))!=null) sb.append(namevals);
//if (namevals!=null) System.out.println("incoming cookies "+namevals);

	// stuff *all* matching cookies into ci -- should sort by degree of match
	long now = System.currentTimeMillis();
	StringBuffer sb = new StringBuffer(200);
//System.out.println("domain="+domain+", path="+path+", "+now);
	for (int i=0,imax=Cookies_.size(); i<imax; i++) {
		Cookie c = Cookies_.get(i);
//System.out.println("	"+c.domain+", "+c.path+", "+c.expires);
		if (domain.endsWith(c.domain) && path.startsWith(c.path) && (c.expires==-1 || c.expires>now) && !c.secure) {
			if (sb.length()>0) sb.append("; ");
			sb.append(c.namevals);
//System.out.println("\tadding: |"+c.namevals+"|");
		}
	}
//System.out.println("*** "+url+" cookie: |"+sb.substring(0)+"|");
	return (sb.length()>0? sb.substring(0): null);
  }


  /** At semantic openDocument, add stored cookies to headers. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;

	Object arg=se.getArg();
	if (Document.MSG_OPEN==msg && arg instanceof DocInfo) {
//System.out.println("*** ADD COOKIES ***");
		DocInfo di = (DocInfo)arg;
//System.out.println("cookie uri = "+ci.uri+", raw scheme="+ci.uri.getRawSchemeSpecificPart()+", raw path="+ci.uri.getRawPath());
		String cookie = getAll(di.uri.getScheme(), di.uri.getPath());
		if (cookie!=null) di.headers.put("Cookie", cookie);	// multiple?
//if (cookie!=null) System.out.println("*** cookie: "+cookie);
	}
	return false;
  }


  /** At semantic openedDocument, extract cookies. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	if (Document.MSG_OPENED==msg && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;

		// "Multiple Set-Cookie headers can be issued in a single server response."
		String key="set-cookie", host=di.uri.getAuthority();
		String cookie = (String)di.headers.remove(key);	// remove because don't cache these headers
		if (cookie!=null) put(host, cookie);
//if (cookie!=null) System.out.println("*** set-cookie: "+cookie);
		if (cookie!=null) for (int i=1; ; i++) {	// can have multiple, grr!
			String altkey = key + ('0'+i);
			cookie = (String)di.headers.remove(altkey);
//System.out.println("aux cookie "+i+" = "+cookie);
			if (cookie!=null) put(host, cookie); else break;
		}
	}
	return super.semanticEventAfter(se,msg);
  }

  public void destroy() {
	write();	// happens when close any browser instance, but that's OK... in fact, good as a checkpoint
	super.destroy();
  }
}
