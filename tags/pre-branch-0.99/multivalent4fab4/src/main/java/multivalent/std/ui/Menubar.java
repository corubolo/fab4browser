package multivalent.std.ui;

import java.util.Map;

import multivalent.*;
import multivalent.node.Root;
import multivalent.node.LeafUnicode;
//import multivalent.gui.VMenu; => menus created on demand
import multivalent.gui.VMenuButton;



/**
	Populates menubar (but not constituant menus).
	<!-- Menubar is found by getting from Root's globals {@link Document xxx VAR_ANCHORS}, then taking key "menubar". -->

	@version $Revision: 1.3 $ $Date: 2002/02/01 07:18:29 $
*/
public class Menubar extends Behavior {
  public static final String ATTR_TITLES = "titles";


  String[] Titles_ = null;

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_CURRENT==msg) {
		Browser br = getBrowser();
		//Document doc = (Document)se.getArg(); -- not needed
		Root root = br.getRoot();
		//Map anchors = (Map)root.getVar(Document.VAR_ANCHORS);
		//Object o = anchors.get("menubar");
		Node o = root.findBFS(null, "id", "menubar");

		if (o instanceof INode) {
			INode menubar = (INode)o;
//System.out.println("Menubar = "+menubar);
			menubar.removeAllChildren();
			for (int i=0,imax=Titles_.length; i<imax; i++) {
				VMenuButton mb = new VMenuButton(/*"_"+tmp[i]+*/"menubutton"/*.toLowerCase().intern()*/,null, menubar);
				mb.setDynamic(Titles_[i]); // happens to have same name
				/*Leaf l =*/ new LeafUnicode(Titles_[i],null, mb);
				//multivalent.std.span.FamilySpan span = new multivalent.std.span.FamilySpan();
				//span.setFamily("Arial"); => style sheet
				//span.moveq(l,0, l,l.size());
			}
		}
	}
	return false;
  }


  /**
	 Take menu titles from "Titles" attribute value, names separated by '|'.
	 Seems like there should be a way to set titles per document.
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// take from GUI, just transform from Leaf to VMenuButton!
	Titles_ = getAttr(ATTR_TITLES, "File|Edit|Go|Bookmark|Style|View|Help").split("\\s*\\|\\s*");   // Tool, Lens/Anno/CopyEd=>too hardcoded, Debug=>controlled by DebugMode
  }
}
