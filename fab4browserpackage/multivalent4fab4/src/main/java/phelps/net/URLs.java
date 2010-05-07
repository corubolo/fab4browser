package phelps.net;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;



/**
	URL-related utility classes.

	<ul>
	<li>{@link #toURI(URL)}
	</ul>

	@version $Revision: 1.4 $ $Date: 2003/06/01 08:16:57 $
*/
public class URLs {
  //static final boolean DEBUG = false;
  private URLs() {}

  /**
	With {@link URI#toURL(URI)}, safe interconversion between URI and URL, working around Java bugs.

	<ul>
	<li>URI.create() and new URI(String) go boom if path has space
	<li>Java URI doesn't recognize the URLs obtained from resources in JAR (protocol <code>jar:file</code>) -- treats scheme as <code>jar</code> and ssp as opaque
	</ul>

	Note also that File.toURL().getPath() is buggy if file contains a '#' [as of Java 1.4].
  */
  public static URI toURI(URL url) throws URISyntaxException {
	if (url==null) return null;
	String protocol = url.getProtocol(), path = url.getPath(), host = url.getHost();

	// "relative path in absolute URL" on "jar:file:/...".  And URI.getPath() on "jar:file:" returns null because path is "file:/..." which is opaque
	// So normalize to "jar": "jar:file:/D:/prj/Multivalent/www/Multivalent.jar!/sys/Splash.html" => "jar:/D:/prj/Multivalent/www/Multivalent.jar!/sys.Splash.html"
	if ("jar".equals(protocol) && path.startsWith("file:")) path = path.substring("file:".length());	// + consider protocol normalized as "jar"

	// normalize Windows absolute path to start with slash (JWS 1.2 has inital slash, JWS 1.4.2 doesn't -- and new URL() requires initial slash)
	// e.g, JWS 1.2 has "file:jar:/C:/DocumentsandSettings..." but JWS 1.4.2 has "file:jar:C:/Document%20and%Settings..."
	if (path.length()>=2 && path.charAt(1)==':' && File.separatorChar=='\\') {
		char ch = path.charAt(0);
		if (('a' <= ch&&ch <= 'z') || ('A' <= ch&&ch <= 'Z')) path = "/" + path;
	}

	if ("".equals(host)) host = null;

	// this constructor escapes the path
	return new URI(protocol, url.getUserInfo(), host, url.getPort(), path, url.getQuery(), url.getRef());
  }
}
