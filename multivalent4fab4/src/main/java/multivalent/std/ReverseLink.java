package multivalent.std;

import java.net.URI;
import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;

import phelps.net.URIs;



/**
	Reverse links: throw page URI to search engine.

	@version $Revision: 1.2 $ $Date: 2002/02/01 03:40:08 $
*/
public class ReverseLink extends Behavior {
  public static final String ATTR_ENGINE = "engine";

  String engine;

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==null/*too restrictive*/) {//in instanceof HyperlinkSpan) { -- reverse relative to current page
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		URI uri = doc.getURI();
//System.out.println("uri = "+uri);
		if (uri!=null && ("http".equals(uri.getScheme()) || "ftp".equals(uri.getScheme()))) {
			SemanticEvent nse = new SemanticEvent(getBrowser(), Document.MSG_OPEN, engine + URIs.encode(uri.toString()), null, null);
			createUI("button", "Reverse Links", nse, (INode)se.getOut(), "NAVIGATE", false);
		}
	}
	return false;
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);
	engine = getAttr(ATTR_ENGINE, "http://www.google.com/search?as_lq=");
  }
}
