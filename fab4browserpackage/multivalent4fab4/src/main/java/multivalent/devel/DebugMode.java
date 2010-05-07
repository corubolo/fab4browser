package multivalent.devel;

import java.util.Map;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.gui.VMenuButton;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;

import phelps.lang.Booleans;



/**
	Set flag that controls availability/visibility of debugging behaviors (those in Debug layer and those in Debug menu).
	Shows memory use, ....

	@see Debug
	@see ShowDocTree

	@version $Revision: 1.4 $ $Date: 2002/02/17 18:32:22 $
*/
public class DebugMode extends Behavior {
  static final boolean DEBUG = false;

  public static final String MSG_SET_DEBUGMODE = "debugMode";

  boolean active_ = true;


  /** Add/remove "Debug" menu to end of menubar. */
  public void setActive(boolean active) {
	active_ = active;

	Browser br = getBrowser();
	//Layer syslayer = (Layer)br.getRoot().getLayers().getBehavior(Layer.SYSTEM);
	Layer syslayer = br.getRoot().getLayers().getInstance(Layer.SYSTEM);
	if (syslayer!=null) {
		//Layer debuglayer = (Layer)syslayer.getBehavior("debug");
		Layer debuglayer = syslayer.getInstance("debug");
		if (debuglayer!=null) debuglayer.setActive(active);
	}

	putPreference("DebugMode", active_? "true": "false");
  }


  public void buildAfter(Document doc) {
	setActive(Booleans.parseBoolean(getPreference("DebugMode", "false"), false));
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_HELP==msg) {
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Debug Mode", "event "+MSG_SET_DEBUGMODE, menu, "DEVELOPER", false);
		cb.setState(active_);
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SET_DEBUGMODE==msg) {
		setActive(Booleans.parseBoolean(se.getArg(), !active_));

		Browser br = getBrowser();
		br.eventq(Document.MSG_RELOAD, null);

	} else if (Document.MSG_CURRENT==msg && active_) {
		//Map anchors = (Map)getBrowser().getRoot().getVar(Document.VAR_ANCHORS);
		//INode menubar = (INode)anchors.get("menubar");
		INode menubar = (INode)getBrowser().getRoot().findBFS(null, "id", "menubar");

		VMenuButton mb = new VMenuButton("menubutton",null, menubar);
		mb.setDynamic("Debug");
		new LeafUnicode("Debug",null, mb);
	}
	return super.semanticEventAfter(se, msg);
  }
}
