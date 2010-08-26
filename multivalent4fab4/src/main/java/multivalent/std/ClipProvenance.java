package multivalent.std;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;

import phelps.lang.Booleans;



/**
	Augment selection for clipboard with source URL.
	(Would like to augment with author and title, but that sort of metadata usually isn't available.)
	Elaborately commented to serve as a simple example of translating Multivalent protocols into Java methods.

	@see multivalent.std.span.HyperlinkSpan for another elaborately commented behavior.

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:24:17 $
*/
public class ClipProvenance extends Behavior {
  /** Menu category for pasting-related options ("AuxSelect"). */
  public static final String MENU_CATEGORY = "AuxSelect";

  /**
	Augment pasted text with provenance or not.
	<p><tt>"setProvenance"</tt>: <tt>arg=</tt> bollean or <code>null</code> to toggle.
  */
  public static final String MSG_SET = "setProvenance";


  boolean active_ = false;

  /**
	When the Clipboard menu announces it is being built by sending a semantic event with
	message {@link VMenu#MSG_CREATE_EDIT} and the node of the menu root in the out field,
	add an entry.  The entry uses {@link Behavior#createUI(String, String, Object, INode, String, boolean)}
	to create a "checkbox" widget with title "Provenance",
	which when invoked will execute the script which sends a semantic event named "setProvenance",
	added to the menu in category "AuxSelect", and which is not disabled (diabled flag = false).
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	// call superclass.  In before protocols, this call is usually done before the subclass.
	// If the superclass is short-circuiting (returns true), respect that.
	// (In this case, we could have omitted this since we're inheriting directly from Behavior, and Behavior doesn't have any default behavior, but that's dangerous.)
	if (super.semanticEventBefore(se, msg)) return true;

	else if (VMenu.MSG_CREATE_EDIT==msg) {
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Paste with Provenance", "event "+MSG_SET, menu, MENU_CATEGORY, false);
		cb.setState(active_);
	}

	// returning true would short-circuit other behaviors, which is seldom the right thing
	return false;
  }

  /**
	Catch the "setProvenance" event sent in semanticEventBefore,
	assuming it hasn't been short-circuited by some other behavior.
	This just sets the internal active flag, to determine whether
	to take action in the next clipboard procotol.  Note that any behavior
	can communicate with ClipProvenance at this high level by sending
	the message it understands, namely "setProvenance"; other behaviors
	don't have to work through its GUI or have hardcoded to some particular
	method signature--they just send in the event the right <tt>String</tt>
	as socially agreed upon (or proclaimed, that is, documented, by ClipProvenance).

	<p>This behavior has its effect (if it's active) during the clipboard protocol,
	but that protocol is based on a tree walk, so if it is to know of the tree
	walk, it has to hook into the tree.  So catch the {@link Document#MSG_OPENED} semantic
	event and hook onto its document field.  It registers interest on the root of the document
	in order to pick up clipboard extractions everywhere in the tree.
	It's common for a behavior to register interest at the root as opposed to
	smaller subtrees.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (MSG_SET==msg) active_ = Booleans.parseBoolean(se.getArg(), !active_);
	else if (Document.MSG_OPENED==msg && arg instanceof DocInfo) ((DocInfo)arg).doc.addObserver(this);

	// in after phases, we invoke the superclass afterwards,
	// and unless we want to short-circuit, the overall short-circuit is the superclass's short-circuit
	return super.semanticEventAfter(se, msg);
  }


  /**
	If this behavior were particular to this document, because it's in a
	genre- or document-specific hub, this we'd register interest this way,
	instead of listening for the Document.MSG_OPENED semantic event.
  */
/*  public void buildAfter(Document doc) {
	doc.addObserver(this);
	super.buildAfter(doc);
  }*/


  /**
	The clipboard protocol builds up the text in a <tt>StringBuffer</tt>.
	Since this is the after phase, we know that the tree walk has taken place,
	and therefore the <tt>StringBuffer</tt> holds the text one expects from
	copying the selection into the clipboard.  If the behavior is active (as
	set by its GUI, slip the URL in at the head of the <tt>StringBuffer</tt>.
  */
  public boolean clipboardAfter(StringBuffer sb, Node node) {
//System.out.println("ClipProvenance clipboardAfter "+node+", active_="+active_);
	if (active_) {
		Browser br = getBrowser();
		// simple action: insert URL at head
		// if excerpt is long, insert a blank line too.
		sb.insert(0, "From "+br.getCurDocument().getURI()+(sb.length()<80*4? "\n": "\n\n"));

		// If we had more metadata, such as author and title, that would be great.
		// If some convention were established, another behavior could check and further augment the clipboard.
		// Maybe the metadata is kept in a database and can be fetched with an ISBN attached to the document,
		// or perhaps one or more particular new document formats carry this metadata,
		// in which case site- or genre-specific behavior could take advantage of it.
		//Node root = getBrowser().getCurDocument();
		//sb.insert(0, "From \""+root.getAttr("title")+"\" by "+root.getAttr("author")+" in "+root.getAttr("source")+":\n\n");
	}
	return false;
  }
}
