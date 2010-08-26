package multivalent.std.ui;

import java.util.Map;

import multivalent.*;
import multivalent.gui.VCheckbox;
import multivalent.std.VScript;


/**
	For now, toggles preference.
	Later: other values (in cascade?), ...

<!--
	For UI that can be described in text file: one that just sends a semantic event.
	LATER: eval a script.
	maybe signal/onvalue/offvalue
-->

	TO DO: assignable default (now set to "on"), should rename to "toggleAttrUI" or make more general, need counterpart for window variables

	@see multivalent.SemanticEvent
	@see multivalent.std.ui.SemanticUI

	@version $Revision: 1.2 $ $Date: 2002/02/01 07:59:06 $
*/
public class AttrUI extends Behavior {
  /**
	Toggle <var>variable</var> between true and false.
	<p><tt>"toggleVariable"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>variable-name</var>.
  */
  public static final String MSG_TOGGLE_VAR = "toggleVariable";

  /** Hub attribute that gives menu (<tt>createWidget/<var>parent</var></tt>). */
  public static final String ATTR_PARENT = "parent";
  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_TITLE = "title";
  /** Hub attribute that sets category group within menu. */
  public static final String ATTR_CATEGORY = "category";

  /** Hub attribute that sets category group within menu. */
  public static final String ATTR_VARIABLE = "variable";
  /** Hub attribute to set {@link multivalent.std.VScript} script to execute when widget is activated. */
  public static final String ATTR_SCRIPT = "script";


  String variable_, seed_;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	if (!msg.startsWith("createWidget/")) return false;
	String parent=getAttr(ATTR_PARENT);
	if (variable_==null || parent==null || !msg.endsWith(parent) || msg.length()!="createWidget/".length()+parent.length()) return false;

	// getAttr("type") -- later?
	VCheckbox cb = (VCheckbox)createUI("checkbox", getAttr(ATTR_TITLE), "event "+MSG_TOGGLE_VAR+" "+variable_+"; "+getAttr(ATTR_SCRIPT), (INode)se.getOut(), getAttr(ATTR_CATEGORY), false);
	//cb.setState(getControl().getPreferenceBoolean(variable_, seed_));
	Document doc = getBrowser().getCurDocument();
	cb.setState(VScript.getBoolean("$"+variable_, doc, attr_, seed_));

	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_TOGGLE_VAR==msg && variable_.equals(se.getArg())) {
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		//INode root = br.getDocRoot();
		//Multivalent control = getControl();
		//boolean val = control.getPreferenceBoolean(variable_, seed_);
		boolean val = VScript.getBoolean("$"+variable_, doc, attr_, seed_);
		//control.putPreference(variable_, (val?"false":"true"));
		VScript.putVal(variable_, (val?"false":"true"), doc, attr_);
//System.out.println("setting "+variable_+" to "+(val?"false":"true"));

		//if (getAttr("reformat")!=null) { root.markDirtySubtree(); br.repaint(); } // => script: "event reformatDocument"
		//else if (getAttr("repaint")!=null) br.repaint(); // => script: "event repaintDocument"
	}
	return super.semanticEventAfter(se,msg);
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	variable_ = getAttr(ATTR_VARIABLE);
	if (variable_!=null && variable_.startsWith("$")) variable_=variable_.substring(1);
	seed_ = getAttr("seed"); if (seed_==null) seed_="off";
//System.out.println("AttrUI restored OK");
  }
}
