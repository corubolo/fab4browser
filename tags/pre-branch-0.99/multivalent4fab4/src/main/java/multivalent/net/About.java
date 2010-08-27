package multivalent.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

import multivalent.*;



/**
	Intercepts {@link Document#MSG_OPEN} events with "about" protocol and remaps to document as given in attributes.

	@version $Revision: 1.4 $ $Date: 2005/01/03 09:34:16 $
*/
public class About extends Behavior {
  public static final String ATTR_MAP = "map";

  private Map<String,String> remap_ = new HashMap<String,String>(20);


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println("About behavior with "+msg);
	if (super.semanticEventBefore(se,msg)) return true;

	Object arg = se.getArg();
	if (Document.MSG_OPEN==msg && arg instanceof URI) {
		URI uri = (URI)arg;

		if ("about".equals(uri.getScheme())) {
			String redirect = remap_.get(uri.getSchemeSpecificPart().toLowerCase());
//System.out.println("|"+uri.getSchemeSpecificPart()+"| => "+redirect);
			if (redirect!=null) try { getBrowser().eventq(Document.MSG_OPEN, new URI(redirect)); } catch (URISyntaxException e) {}
//System.out.println("cancelling in About");
			return true;
		}
	}
	return false;
  }

  /**
	Take name-to-URI remappings from hub entry's {@link ATTR_MAP} attribute,
	as <code><var>name</var> <var>uri</var>, <var>name2</var> <var>uri2</var> ...</code> pairings.
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);

	String[] map = getAttr(ATTR_MAP, "").split(",");
	for (int i=0,imax=map.length; i<imax; i++) {
		String nameval = map[i].trim();
		int inx = nameval.indexOf(' ');
		if (inx!=-1) remap_.put(nameval.substring(0,inx).trim().toLowerCase(), nameval.substring(inx+1).trim());
	}
  }
}
