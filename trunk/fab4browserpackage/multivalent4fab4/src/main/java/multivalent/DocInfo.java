package multivalent;

import java.net.URI;
import java.util.Map;



/**
	Record of various data about a Document,
	for parameters for loading and for reports of general information for various behaviors.

	@version $Revision: 1.4 $ $Date: 2002/10/29 16:30:57 $
 */
public class DocInfo {
	/** Requested URI.  Can be URL, search words, "about:multivalent", whatever. */
	public Object uriin = null;

	/** Actual URI found, after possible 300/301/302 redirect, generalization to match compressed version, redirection to cached copy, or whatever. */
	public URI uri = null;

	/** HTTP headers (mocked up for file).  in-out => change to List<> as can have duplicate keys, as with set-cookie.  Normalize key. */
	public final/*.clear() ok but not =null*/ Map<String,String> headers = new CHashMap<String>(10);

	/** in-out: URL in (which could be search words), signature stripped off, .... */
	public final Map<String,Object> attrs = new CHashMap<Object>(10);

	/** Location in cache, or null if not.
  public File cache = null;*/

	/** Genre mapping, such as "PDF" or "ManualPage". */
	public String genre = null;

	// HTTP return codes handled by throw exception
	/** -1==haven't connected yet. */
	public int returncode = -1;

	/** maybe change to String that you search for. */
	public Document doc = null;

	/** set to Browser or String name of Browser window. */
	public Object window = null;

	//** Actual data stream, maybe coming from cache, maybe filtered to uncompress or decrypt. */
	//public InputStream in = null;

	/** HTTP method: GET, POST, HEAD, ....  Stuff accompanying (escaped) data for POST in attrs. */
	public String method = "GET";


	public DocInfo(URI uri) {
		//System.out.println("di: "+uri+" => "+uri.getScheme());
		if (uri==null) {    // ok, reload
		} /*else if ("jar".equals(uri.getScheme()) && uri.getSchemeSpecificPart().startsWith("file:")) {
		String frag = uri.getFragment()==null? "": "#"+uri.getFragment();
		uri = URI.create("jar:" + uri.getSchemeSpecificPart().substring("file:".length()) + frag);
	}*/
		assert uri==null || !uri.getSchemeSpecificPart().startsWith("file:");

		uriin = uri;
		this.uri = uri;
	}

	/*
  public DocInfo(URL url) {
	this();
	try { uri = new URI(url.toString()); } catch (URISyntaxException canthappen) { System.out.println("can't URL=>URI: "+canthappen);}
  }*/

	@Override
	public String toString() {
		return "DocInfo: "+uri;
	}
}
