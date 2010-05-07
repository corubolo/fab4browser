// change package to your own hierarchy
package multivalent.devel.demo;

// whatever Java packages
import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.net.MalformedURLException;


// import basic classes
import multivalent.*;

// tree nodes
//import multivalent.node.LeafUnicode;

// graphical user interface widgets
import multivalent.gui.VMenu;
import multivalent.std.ui.DocumentPopup;
import multivalent.gui.VButton;
import multivalent.gui.VCheckbox;

import phelps.lang.Integers;



/**
	Template to use in writing new behaviors -- copy the source code and edit; don't subclass.
	This class is not packaged in the browser, and so cannot be subclassed.
	Use this for a fully general behavior.
	Better to take as model some behavior more closely matching what the new behavior will do,
	and cut and paste from it; {@link Behavior} has a long list of subclasses.
	However, other behaviors are specialized and drop protocols not used; find those here.
	If don't do anything special in some protocol, don't override.
	If do override, always invoke superclass; do this before new code in <i>before</i> phases, after new code in <i>after</i> phases,
	as shown in models below.
	Just guidelines; some behaviors may violate patterns, but one should do so knowingly.
	If you don't need to do anything in a particular protocol, simply delete the corresponding method.

	<p>In my code, I use Java's <code>assert</code> for conditions that if violated represent an error in the caller.
	Exceptions are thrown for those conditions, like parsing errors on bad input, that even correct code can suffer.
	Sun's code throws exceptions in both cases.

	@version $Revision: 1.5 $ $Date: 2005/01/03 08:55:57 $
*/
public class MyBehavior extends Behavior {
  // enable/disable internal debugging by inserting/removing "!" in front of boolean value, and leave base value set to most common setting
  static final boolean DEBUG = false;


  /**
	New semantic command defined (convention: "MSG_" prefix).
	All "high level" actions should be carried out through sending and receiving semantic events,
	rather than through direct method invocation, so that other behaviors have a chance to modify the action.
	In order to publish to clients, behaviors must
	define the {@link java.lang.String} name to use in string-based scripts and in case symbolic reference to this constant is impractical because the defining class may or may not be available
	(if the symbol is in a multivalent.* class, then it's guaranteed to be available, and so the symbolc reference should be used),
	and define the use of <tt>arg</tt>, <tt>in</tt>, and <tt>out</tt> fields.
	There is a form of method overloading, since events with the same message can have arguments of different types.
	Thus, clients should always check the type of the arguments.
	A possible definition is the following:

	<p><tt>"NewCommand1"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.

	<p>A behavior that takes action on the semantic event should document this in its
	{@link #semanticEventBefore(SemanticEvent, String)} and {@link #semanticEventAfter(SemanticEvent, String)} methods.

	@see multivalent.SystemEvents more more examples.
  */
  public static final String MSG_NEWCMD1 = "NewCommand1";

  /**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"NewCommand2"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
  */
  public static final String MSG_NEWCMD2 = "NewCommand2";


  /**
	Document use of Preferences (convention: "PREF_" prefix) by listing key/variable name and the type of value.
	Boolean value (Object) indicating whether to show x-rays or not.
  */
  public static final String PREF_XRAY = "mybehaviorXRay";

  /**
	Document use of Preferences by listing key/variable name and the type of value.
	String that is one of "fast", "slow", "stopped".
  */
  public static final String PREF_SPEED = "mybehaviorSpeed";

  /**
	Document use of attribute in hub document (convention: "ATTR_" prefix).
  */
  public static final String ATTR_XRAY = "mybehaviorXRay";


  /* Behaviors should not use constructors!
	 Instead, Behaviors initialize in the restore() method.
	 For more details see description in multivalent.Behavior.
  public MyBehavior() {
	// NO
  }
  */


  public ESISNode save() {
	// if any local variables are to be saved, stuff them into attributes
	Map<String,Object> attrs = getAttributes();
	attrs.put("Magic-number", String.valueOf(16));

	// invoke superclass to write to evolving XML document
	ESISNode e = super.save();

	// return root of XML tree built here
	return e;
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	// let superclass hook into layer and make attributes available via getAttr*()
	super.restore(n, attr, layer);

	// A behavior-specific an XML subtree as realized from a hub is rooted at <var>n</var>.
	// Processing of the subtree is entirely behavior-specific.


	// if changed use of some attribute or something, update old versions here
	// ...


	// Some behaviors allow nested behaviors in the tree;
	// if so, they first recursely restore them so that they are fully instantiated for other behavior-specific operations.
	restoreChildren(n, layer);   // recurse

	// step over tree for behavior-specific restoration, if any
	if (n!=null) for (int i=0; i<n.size() /* count can change while enumerating--still?  that would be bad */; i++) {
		Object child = n.childAt(i);
//if (child instanceof ESISNode) System.out.println("child = "+((ESISNode)child).getGI());
		if (child instanceof ESISNode) {
			ESISNode m = (ESISNode)child;
			if ("some-tag-name".equals(m.getGI())) {
				// ...
			} else if ("some-attr-val".equals(m.getAttr("some-attr-name"))) {
				// ...
			}
		}
	}


	// set variables from attributes for easier manipulation, if any
	int count = Integers.parseInt(getAttr("magic-number"), 0xDEADBEEF/*default*/);
  }


  public void destroy() {
	// new code to execute before behavior is removed from layer, which is only link to rest of system

	super.destroy();

	// new code to execute after
  }


  public void buildBefore(Document doc) {
	super.buildBefore(doc);

	// new code
  }


  public void buildAfter(Document doc) {
	// new code

	super.buildAfter(doc);
  }


  public boolean formatBefore(Node node) {
	if (super.formatBefore(node)) return true;    // respect superclass short-circuit
	else {
		// new code
	}

	// return true if want to short-circuit, which should be seldom
	return false;
  }

  public boolean formatAfter(Node node) {
	// new code

	// always invoke superclass.  Return true to short-circuit, which should be seldom
	return super.formatAfter(node) /*|| true*/;
  }


  public boolean paintBefore(Context cx, Node node) {
	if (super.paintBefore(cx, node)) return true;    // respect superclass short-circuit
	else {
		// new code
	}

	// return true if want to short-circuit, which should be seldom
	return false;
  }

  public boolean paintAfter(Context cx, Node node) {
	// new code

	return super.paintAfter(cx, node) /*|| true*/;
  }


  public boolean clipboardBefore(StringBuffer sb, Node node) {
	if (super.clipboardBefore(sb, node)) return true;    // respect superclass short-circuit
	else {
		// new code
	}

	// return true if want to short-circuit, which should be seldom
	return false;
  }

  /**
	A tree walk protocol, called after observed node has been given a chance to contribute to the growing selection content in the passed StringBuffer.
	As a special case, observers on the root are always called, even if the selection is for only a part of the document.
  */
  public boolean clipboardAfter(StringBuffer sb, Node node) {
	// new code

	return super.clipboardAfter(sb, node) /*|| true*/;
  }


  /**
	Creates widgets: menu items, ....
	Responds to "NewCommand1" by recording in statistics, and sending "NewCommand2".
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (super.semanticEventBefore(se, msg)) return true;    // respect superclass short-circuit

	// new code

	// create user interface widgets with unusual properties
	// (where possible, for ordinary widget use, specify it in the corresponding hub with attributes to a multivalent.std.ui.SemanticUI instance)
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		// general document state
		Browser br = getBrowser();
		Document doc = getDocument();

		// based on message, decode other fields from events
		INode menu = (INode)se.getOut();

		// add item to View menu (this particular case is straightforward and should be done by SemanticUI),
		// more specifically add to "Ocr" group and disable if the selection is not set.
		// createUI understands label, button, checkbox, radiobox, menubutton, separator, entry
		//public Node createUI(String type, String title, Object script, INode parent, String category, boolean disabled)
		createUI("button", "View as XXX", "event "+MSG_NEWCMD1, menu, VMenu.CATEGORY_MEDIUM, br.getSelectionSpan().isSet());

		// catch the return value to set further state
		// Widget content can be an arbitrary tree; createUI parses HTML.
		VCheckbox cb = (VCheckbox)createUI("checkbox", "<b>View as YYY</b>", "event "+MSG_NEWCMD2, menu, VMenu.CATEGORY_MEDIUM, false/*never disabled*/);
		cb.setState(true);

	} else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg) {
		// add item to a particular popup menu
	}

	// return true if want to short-circuit, which should be seldom
	return false;
  }

  /**
	On "NewCommand2", do something else.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// since event survived the <i>before</i> phase, no behavior wants to prevent it

	// respond to semantic events implemented by this behavior
	if (MSG_NEWCMD1==msg) {   // can use "==" here since passed String has been intern()'ed
		// commands often send other semantic events to invoke other behaviors, rather than directly invoking methods on a behavior
		// A list of semantic events implemented by the base system is available in the online developer documentation, under Architecture.
		Browser br = getBrowser();
		br.eventq(Document.MSG_OPEN, "http://multivalent.sourceforge.net/");

	// augment existing command
	} else if (Document.MSG_OPEN==msg) {
	}

	return super.semanticEventAfter(se, msg);
  }


  public boolean eventBefore(AWTEvent e, Point rel, Node n) {
	if (super.eventBefore(e, rel, n)) return true;    // respect superclass short-circuit
	else {
		// new code
	}

	// return true if want to short-circuit, which should be seldom
	return false;
  }

  public boolean eventAfter(AWTEvent e, Point rel, Node n) {
	// new code

	return super.eventAfter(e, rel, n) /*|| true*/;
  }


  public void stats(String type) {}


  public String toString() {
	// don't need to invoke superclass
	return getName();
  }


  public boolean checkRep() {
	assert super.checkRep();

	// additional checks ...
	// assert ...

	return true;
  }


  /**
	Runs unit tests that need to be in same class in order to access private or protected fields.
  public static void main(String[] args) {
  }
  */
}
