package multivalent.std.ui;

import multivalent.*;
import multivalent.gui.VEntry;
import multivalent.gui.VMenuButton;

import phelps.lang.Integers;



/**
	<p>Generates from a hub description UI elements that trigger a semantic event when activated.

	<p>A good portion of the functionality of the system can be triggered
	by lone semantic event.  It's easy to add a menu item or button that
	fires a semantic event with a line in a hub document.  In fact, much
	of the default system is built that way.  For instance, the
	menu item named "Home" that shows your home page is generated with the
	following line in the system hub:

	<blockquote>
	<tt>&lt;MenuItem Behavior=SemanticUI event=&quot;goHome&quot; title=&quot;Home&quot; category=&quot;GoInter&quot; /&gt;</tt>
	</blockquote>

	<p>Likewise, the Home button is generated with the following:

	<blockquote>
	<tt>&lt;Button Behavior=SemanticUI event=&quot;goHome&quot; title=&quot;<img src='systemresource:/sys/images/Home16.gif' height=16 width=16>&quot; category=&quot;Toolbar&quot; script='goHome' /&gt;</tt>
	</blockquote>

<!--
	<p>LATER:
	take tree path, such as _MENUBAR / _FILE / _SAVE
-->

	@see multivalent.SemanticEvent

	@version $Revision: 1.2 $ $Date: 2002/02/01 06:04:05 $
*/
public class SemanticUI extends Behavior {
  /** Hub attribute that gives menu (<tt>createWidget/<var>parent</var></tt>). */
  public static final String ATTR_PARENT = "parent";
  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_TITLE = "title";
  /** Hub attribute that sets category group within menu. */
  public static final String ATTR_CATEGORY = "category";

  /** Hub attribute to set type of widget as acceptable by {@link Behavior#createUI(String, String, Object, INode, String, boolean)}. */
  public static final String ATTR_TYPE = "type";
  /** Hub attribute to set {@link multivalent.std.VScript} script to execute when widget is activated. */
  public static final String ATTR_SCRIPT = "script";

  /** Hub attribute to set width of VEntry widgets. */
  public static final String ATTR_WIDTH = "width";
  /** Hub attribute to set name of dynamic menu of VMenubutton widgets. */
  public static final String ATTR_GENERATE = "generate";


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	if (!msg.startsWith("createWidget/")) return false;
	String parent = getAttr(ATTR_PARENT); if (parent==null || !msg.endsWith(parent) || msg.length()!="createWidget/".length()+parent.length()) return false;

	String type = getAttr(ATTR_TYPE, "button").toLowerCase();
	Node n = createUI(type, getAttr(ATTR_TITLE), getAttr(ATTR_SCRIPT), (INode)se.getOut(), getAttr(ATTR_CATEGORY), false);

//System.out.println("*** semanticUI "+getAttr(ATTR_TITLE)+", parent="+n.getParentNode());

	if (n==null) {
		System.err.println("SemanticUI: couldn't create widget, type="+type+", title="+getAttr(ATTR_TITLE));
	} else if ("entry".equals(type)) {
		VEntry e = (VEntry)n;
		e.setWidthChars(Integers.parseInt(getAttr(ATTR_WIDTH), 20));
	} else if ("menubutton".equals(type)) {
		((VMenuButton)n).setDynamic(getAttr(ATTR_GENERATE));
	}

	return false;
  }
}
