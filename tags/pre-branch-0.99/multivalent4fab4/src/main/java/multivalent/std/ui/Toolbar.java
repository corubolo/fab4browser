package multivalent.std.ui;

import multivalent.*;
import multivalent.node.Root;
//import multivalent.gui.VMenu; => menus created on demand



/**
	Populates toolbars.
	<!-- Toolbar is found by getting from Root's globals {@link Document xxx VAR_ANCHORS}, then taking key "toolbar" and "toolbar2". -->

	@version $Revision: 1.1 $ $Date: 2002/02/15 17:27:35 $
*/
public class Toolbar extends Behavior {
  //String[] Titles_ = null;

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_CURRENT==msg) {
		Browser br = getBrowser();
		//Document doc = (Document)se.getArg(); -- not needed
		Root root = br.getRoot();
		//Map anchors = (Map)root.getVar(Document.VAR_ANCHORS);
		Node o = root.findBFS(null, "id", "toolbar");

		//Object o = anchors.get("toolbar");
		if (o instanceof INode) {
			INode toolbar = (INode)o;
			toolbar.removeAllChildren();
			// can't use eventq because FRAME will load up several before event propagates
			br.event(new SemanticEvent(this, Browser.MSG_CREATE_TOOLBAR, null, null, toolbar));
		}

		//o = anchors.get("toolbar2");
		o = root.findBFS(null, "id", "toolbar2");
		if (o instanceof INode) {
			INode toolbar2 = (INode)o;
			toolbar2.removeAllChildren();
			br.event(new SemanticEvent(this, Browser.MSG_CREATE_TOOLBAR2, null, null, toolbar2));
		}
	}
	return false;
  }


  /**
	 Take menu titles from "Titles" attribute value, names separated by '|'.
	 Seems like there should be a way to set titles per document.
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	boolean ret = super.restore(n, attr, layer);

	// take from GUI, just transform from Leaf to VMenuButton!
	String titles = getAttr("titles", "File|Edit|Go|Bookmark|Style|Clipboard|View|Help");   // Tool, Lens/Anno/CopyEd=>too hardcoded, Debug=>controlled by DebugMode
	StringTokenizer st = new StringTokenizer(titles, "|");
	Titles_ = new String[st.countTokens()];
	for (int i=0; st.hasMoreTokens(); i++) Titles_[i] = st.nextToken().trim();
  }
  */
}
