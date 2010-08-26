package multivalent.gui;

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.awt.Point;
import java.net.URI;
import java.util.Map;

import multivalent.*;
import multivalent.node.IVBox;
import multivalent.node.IParaBox;
import multivalent.node.LeafUnicode;



/**
	Automatically sizes to include content as given by a URL, and horizontally centers in window, and has no window controls.
	As in Apple's Aqua, Alerts are not free to roam about the screen, but rather are bound to the window to which they refer and hence remain clearly associate to it.
	Remember that URL can be systemresource:/.

	To this content is appended a horizontal rule and close button.

	@version $Revision: 1.4 $ $Date: 2002/11/08 06:03:17 $
*/
public class VAlert extends IVBox {
  public VAlert(String name,Map<String,Object> attrs, INode parent, String src) {
	super(name,attrs, parent);
	INode p = new IParaBox("para",null, this);
	new LeafUnicode(src,null, p);
	appendOK();
  }

  public VAlert(String name,Map<String,Object> attrs, INode parent,  URI src) {
	super(name,attrs, parent);
	new LeafUnicode(src.toString(),null, this);
	appendOK();
  }

  void appendOK() {
	//content.appendChild();  // horizontal rule

	VButton butt = new VButton("ok",null, this);
	butt.putAttr(VButton.ATTR_SCRIPT, new SemanticEvent(getBrowser(), SystemEvents.MSG_DESTROY, this));
	new LeafUnicode("OK",null, butt);

  }


  /**
	Block events to rest of tree.
  */
  public boolean eventNode(AWTEvent e, Point rel) {
	super.eventNode(e, rel); // let people get to close button

	//if (java.awt.event.MouseEvent.MOUSE_CLICKED == e.getID()) remove();  // take any click? double click?
//System.out.println("eventNode "+e+" "+rel);

	return true;    // short-circuit: events to own tree only
  }


  public boolean formatNode(int width, int height, Context cx) {
	super.formatNode(/*width*/300, height, cx);

	// make large enough to encompass content without scrolling
	// => done by IVBox

	// center in parent (absolute positioning)
	Rectangle r = getParentNode().bbox;
	bbox.setLocation((r.width-bbox.width)/2, (r.height-bbox.height)/4);
	//bbox.setLocation((r.width-bbox.width)/2, 0);    // hanging off top, like Aqua

	valid_ = true;
	return false;
  }
}
