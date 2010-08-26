package phelps.net;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;



/**
	URI-related utility classes.

	<ul>
	<li>tidy: {@link #fix(String)}, {@link #canonicalize(URI)}
	<li>conversion: {@link #toURL(URI)}, {@link #encode(String)} and {@link #decode(String)} without throwing Exception
	</ul>

	@version $Revision: 1.5 $ $Date: 2005/01/02 07:57:11 $
*/
public class URIs {
  //static final boolean DEBUG = false;
  private URIs() {}

  /**
	Like {@link java.net.URLEncoder} but without the hassle.
	Java's URLEncode and URLDecode are too complicated.  First, they are separate classes of one method each, whereas they should both be stuffed in java.net.URL or java.net.URI.
	Second, starting in Java 1.4, you have to explicitly supply "UTF-8" in order to meet the W3C's recommendation, which is a hassle,
	and you have to catch "UnsupportedEncodingException", which should never happen.  Bah!
  */
  public static String encode(String s) {
	assert s!=null;
	/*int inx = s.indexOf('%'), len = s.length();
	if (s.indexOf(' ')==-1 && inx!=-1 && inx+2<len && Character.digit(s.charAt(inx+1),16)!=-1 && Character.digit(s.charAt(inx+2),16)!=-1) {
		// if already encoded, don't encode again
	} else*/ try {
		s = java.net.URLEncoder.encode(s, "UTF-8"); // UTF-8 guaranteed
	} catch (java.io.UnsupportedEncodingException canthappen) {}
	return s;
  }

  /**
	Like {@link java.net.URLDecoder} but without the hassle.
  */
  public static String decode(String s) {
	assert s!=null;
	try { s = java.net.URLDecoder.decode(s, "UTF-8"); } catch (java.io.UnsupportedEncodingException canthappen) {}
	return s;
  }


  /**
	Fixes up alleged URIs to be acceptable to the strict parsing of {@link java.net.URI}.
	In practice browsers have to be robust against invalid URIs.

<!-- not part of API
	<p>Fixes:
	<ul>
	<li>trim spaces from both ends, escape invalid characters (space, %, ...), ....
	<!--li>no <var>base</var> and no protocol: guesses "file" if file exists, "ftp" if start is "ftp....", or if nothing else assume "http"00<
	<li>If <code>file</code> protocol, UNIX <code>~</code> to user home directory<!--, as done by {@link phelps.io.Files#getCanonicalPath(String)}.-->
	<li>convert Netscape syntax for Windows drive letters to Java syntax
	</ul>
-->
  */
  public static String fix(/*URI base,*/ String s) {
	assert s!=null;
	//System.out.println("URIs.fix("+s+")");

	s = s.trim();

	StringBuffer sb = new StringBuffer(s.length() * 2);
	int i = 0;

	if (s.startsWith("~/")) { sb.append(System.getProperty("user.home")); i++; }
	//else if drive letters

	for (int imax=s.length(); i<imax; i++) {
		char ch = s.charAt(i);

		// illegal -- ignore
		if (ch=='\t' || ch=='\n' || ch=='\r') {

		// illegal -- escape: internal whitespace, ", ...
		} else if (ch==' ' || ch=='"') {

		// escaped: %<hex-digit><hex-digit>
		} else if (ch=='%') {   // make sure followed by hex.  http://libraryjournal.reviewsnews.com has a "... &industryid=%industryid%&verticalid=151"
			if (i+2>=imax || Character.digit(s.charAt(i+1),16)==-1 || Character.digit(s.charAt(i+2),16)==-1) sb.append("%25");
			else { sb.append(ch).append(s.charAt(i+1)).append(s.charAt(i+2)); i+=2; }

		// reserved: ",;:$&+="  "?/[]@"
		// unreserved: alnum + "_-!.~'()*"
		} else {
			sb.append(ch);
		}
	}

	String news = sb.toString();
	//if (!s.equals(news)) System.out.println("fixed: "+news);
	return news;
  }


  /**
	Canonicalizes URI.
	<!--@param full	apply additional canonicalizations beyond URI specification, but which other parts of Multivalent understand-->
  */
  public static URI canonicalize(URI uri/*, boolean full*/) {
	if (uri==null || !uri.isAbsolute()) return uri;

	// URI :== [scheme:]scheme-specific-part[#fragment]  
	String scheme = uri.getScheme(), ssp = uri.getSchemeSpecificPart(), frag = uri.getFragment();
	String auth = uri.getAuthority(), path = uri.getPath(), query = uri.getQuery();
	// authority :== [user-info@]host[:port]  
	String ui = uri.getUserInfo(), host = uri.getHost(); int port = uri.getPort();			

	boolean fchange = false;
	String s = scheme.toLowerCase(); if (!scheme.equals(s)) { scheme=s; fchange=true; }
	// Java bugs
//System.out.println("scheme = "+scheme+", path="+path+", query="+query);
	if ("jar".equals(scheme) && ssp.startsWith("file:")) {
		path = ssp = ssp.substring("file:".length());
		int inx = path.indexOf('?'); if (inx!=-1) { /*scheme="jar:file"?*/ query=path.substring(inx+1); path=path.substring(0,inx); }
		fchange=true;
//System.out.println("\tFIX: path="+path+", query="+query);
	}

	try {
		// scheme-specific changes
		if (/*uri.isOpaque() => Java bug*/ auth==null && path==null) {
			if (fchange) uri = new URI(scheme, ssp, frag);

		} else {	// hierarchical: [scheme:][//authority][path][?query][#fragment]
			if ("jar".equals(scheme)) {
				// Java bug handled above
			} else if ("file".equals(scheme)) {
				//File f = new File(path);
				//if (!path.endsWith("/") && f.exists() && f.isDirectory()) path += "/";
			} else if ("http".equals(scheme) || "https".equals(scheme)) {
				if (host==null || host.equals("")) { host = "localhost"; fchange=true; }
				//X if ("localhost".equals(host)) host = ""; => local web server just like foreign, just shorter name
				s = host.toLowerCase(); if (!host.equals(s)) { host=s; fchange=true; }
				// if (full) zap port if same as default for protocol
			}

			if (path==null || path.length()==0 /*|| just a "~xxx"*/) { path = "/"; fchange=true; }
			// X if (path.endsWith("/index.html") || path.endsWith("/index.htm") path = path.substring(0, path.length() - "index.html"); => not guaranteed

			//path = path.replace('\\', '/');	// MSDOS separators (possible File, but in URI too?)
			//if (full && Files.isCompressed(path)) path = path.substring(0, path.lastIndexOf('.'));
			//if (full) path = RobustHyperlink.stripSignature(path);	// remove lexical signature -- ?

			if (fchange) {
				//System.out.print("canon: "+uri+" => ");
				uri = new URI(scheme, ui, host, port, path, query, frag);
				//System.out.println(uri);
			}
		}
	} catch (URISyntaxException use) { System.err.println("bad canonicalization: "+uri); }

	return uri;
  }


  /** Given a URL spec, fix up missing pieces before using <tt>new java.net.URL</tt>
	Transformations include: missing protocol => assume http,
	as well as those for <tt>robustURL(URL)</tt>
  */
  /**
	java.net.URL accepts certain URLs that won't resolve in practice.
	Transformations include: one-word host wrapped in www. .com, empty path to "/"
  */


  //static final String[] HOSTPROTOCOLS = { "http", "https", "ftp", "telnet", "gopher" };
  /**
	Takes a valid URI and tweaks it to be more likely to resolve to an actual location.

	=> robustURI

	<ul>
	<li>"ocm"/"con"=>"com", commas in host name=>periods, single word host (assume "www.<word>.com")
	<li>expands shorthand: for example, "www.cs/~phelps" => "http://www.cs.berkeley.edu/~phelps/"
	<li>[Signature split off by RobustHyperlink behavior.]
	<li>convert the various conventions for MS-DOS drive letters (from Netscape and so on) into Java's representation (<code>file://c:/...</code>).
	</ul>

  */
  private/*public*/ static URL XXrobustURL(URL url) {
//System.out.println("robust2 <= "+url);
	if (url==null) return null;

	String protocol = url.getProtocol();	// set to something by now?
	String host = url.getHost();
	int port = url.getPort();
	String file = url.getFile();
	String ref = url.getRef();
	String p0=protocol, h0=host, f0=file, r0=ref;

/* have to have a protocol by the time you get here
	// these have hosts
	boolean hosted=false;
	for (int i=0,imax=HOSTPROTOCOLS.length; i<imax; i++) {
		if (HOSTPROTOCOLS[i].equals(protocol)) { hosted=true; break; }
	}
	if (!hosted) return url;
*/
	if ("".equals(file) /*don't get null*/) file = "/";
	else if (file.startsWith("?")) file="/"+file;

	if ("file".equals(protocol)) {
		host="";
		// maybe search around for nearest match
//System.out.println(protocol+"|   |"+host+"|   |"+port+"|   |"+file+"|   |"+ref);
		// Java shouldn't allow empty paths => set to '/'
//System.out.print(file+" => ");
		//int inx;
		// translate Netscape's bogus Windows drive letter: '|'=>':'.  Maybe a blind replace('|',':') would be ok
		if (file.length()>=4 && file.charAt(0)=='/' && Character.isLetter(file.charAt(1)) && file.charAt(2)=='|' && file.charAt(3)=='/') {
			file = "/"+file.charAt(1)+":"+file.substring("/D|".length());
		}

	} else {    // http, ftp, ...
		host=host.replace(',','.');	// accidentally hit ',' instead of '.'
		if (!host.equals("")/*as in file:*/ && host.indexOf('.')==-1) host = "www."+host+".com";
		if (host.endsWith(".ocm") || host.endsWith(".con")) host=host.substring(0,host.length()-4)+".com";

/*	    } else if ((inx=file.indexOf("/~"))!=-1) {  -- worthless on Windoze
			String home = System.getProperty("user.home");
			if (home!=null) file = "/"+home+"/"+file.substring(inx+2);*/
//System.out.println(file);
	}

//System.out.println("robust: file = |"+file+"|");
	if (protocol!=p0 || host!=h0 || file!=f0 || ref!=r0)
		try { return new URL(protocol, host, port, file); } catch (MalformedURLException canthappen) {}
	return url;
  }


  /**
	Returns target URL relative to base URL.
	Like {@link java.net.URI#relativize(URI)}, except actually does what you want.
	=> URI
  */
  private/*public*/ static String XXrelativeURL(URL base, URL target) {
	int chop;
	if (target==null) {
		return "(null destination)";

	} else if (base==null || !base.getProtocol().equals(target.getProtocol()) || !base.getHost().equals(target.getHost()) || base.getPort()!=target.getPort()) {
		return target.toString();

	} else if (base.equals(target)) {	// URL equals doesn't check #ref's
		String bref=base.getRef(), tref=target.getRef();
		if (bref!=null && bref.equals(tref)) return "(you are here)";
		else return tref;

	} else {
		String baseFile = base.getFile();
		//if ((chop=baseFile.lastIndexOf('/'))!=-1) baseFile=baseFile.substring(0,chop+1);	// include trailing slash
		String targetFile = target.getFile();
		String tref = (target.getRef()==null? "": "#"+target.getRef());
		if (baseFile.equals(targetFile)) {
			return tref;	// refs must differ else would be equal above
		} else if ((chop=baseFile.lastIndexOf('/'))!=-1 && targetFile.regionMatches(0, baseFile,0, chop)) {
			return targetFile.substring(chop+1)+tref;
		} else {
			// try one level of going up
			if (baseFile.length()!=0 && (chop=baseFile.substring(0,baseFile.length()-1).lastIndexOf('/'))!=-1) {
				baseFile=baseFile.substring(0,chop+1);
				if (targetFile.startsWith(baseFile)) {
					return "../"+targetFile.substring(baseFile.length())+tref;
				}
			}
			return targetFile;
		}

		// use "user.dir"
	}
  }

  /**
	With {@link URIs#toURL(URI)}, safe interconversion between URI and URL, working around Java bugs.

	<ul>
	<li>URI.toURL() "jar:" protocol (#4677045)
	<li>Java divergence from URL specification that allows spaces and other characters,
		and so can't distinguish proper URL with escaped characters in path!
	</ul>
  */
  public static URL toURL(URI uri) throws MalformedURLException {
	if (uri==null) return null;
	String scheme = uri.getScheme(), ss = uri.getSchemeSpecificPart(), frag = uri.getFragment();

	// getPath() starts with "file:": (1) unlike other paths, (2) URI calls relative path and won't accept => normalize "jar:file:" to just "jar:"
	if ("jar".equals(scheme) && !ss.startsWith("file:")) scheme += ":file";
	if (frag!=null) ss += "#" + frag;	// URI.getSSP() doesn't include fragment

	// unescape path: %xx => char
	ss = decode(ss);

	//return new URL(scheme, uri.getHost(), uri.getPort(), path); -- can't pass all information in any constructor?
	return new URL(scheme + ":" +ss);
  }
}
