package multivalent.node;	// maybe promote to package multivalent;

import java.util.List;
import java.util.Map;

import multivalent.Node;
import multivalent.Context;
import multivalent.Browser;
import multivalent.Document;
import multivalent.gui.VScrollbar;



/**
	{@link multivalent.Document}s can contain other documents, and this is the topmost instance.
	Within the Root are subdocuments corresponding to the user interface and content.
	Each Root corresponds to exactly one {@link Browser} and vice-versa.
	All Documents have shared variables; as the topmost Document, Root holds Browser-wide variables.

<!--
	+ Want stylesheet, visual layers, used to want end to Document() chain but no longer now that have getRelLocation.
	- Don't want scrollbars, whatever other extensions.
-->

	@see multivalent.Browser

	@version $Revision: 1.7 $ $Date: 2003/02/06 05:30:24 $
*/
public class Root extends Document {
  public Root(Map<String,Object> attr, Browser br) {
	super("root",attr, null, br);
	setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);	// children are entirely responsible for own display
  }

  /** Ends chain up tree, returning self. */
  public Root getRoot() { return this; }
  // already have public Document getDocument() { return this; }

  /** Ends chain up tree and calls redraws in Browser/Canvas. */
  public void repaint(long ms, int x,int y, int w,int h) {
	Browser br = getBrowser();
	if (br!=null) br.repaint(ms, x+dx(),y+dy(), w,h);
  }

  /**
	Takes width and height from containing Browser Window.
  */
  public boolean formatBeforeAfter(int width,int height, Context cx) {
	Browser br = getBrowser();
//System.out.println("Root format "+width+" x "+height);
//if (width<=0 || height<=0) { System.out.println("Browser w="+width+", h="+height); return true; }

	// if children change size, such as secondary toolbar popping in and out, reformat everybody
//System.out.println("ibbox "+ibbox.width+"x"+ibbox.height+" vs "+width+"x"+height);
	if (br==null || ibbox.width!=br.getWidth()/*maybe allow this for menu overlapping right edge*/ || ibbox.height!=br.getHeight()) {
//System.out.println("new dimensions "+", killing subtree");
		markDirtySubtree(true);
		width = br.getWidth(); height = br.getHeight();
		//shortcircuit = super.formatBeforeAfter(width,height, cx);
	}

//Document curdoc = getBrowser().getCurDocument(); System.out.println("before root, curdoc="+curdoc+"/"+curdoc.isValid());
	boolean shortcircuit = super.formatBeforeAfter(width,height, cx);
//System.out.println("after root, curdoc="+curdoc+"/"+curdoc.isValid());

	bbox.setBounds(0,0,width,height);	// may not have parent; if does, still OK to set (x,y)=(0,0) here
//System.out.println("root = "+width+"x"+height);

	return shortcircuit;	// valid set by superclass
  }


  public void reformat(Node dirty) {
	setValid(false);
	repaint(50);
  }


/* used to handle grab, but no more
  public boolean eventNode(AWTEvent e, Point rel) {
	EventListener grab=getBrowser().getGrab();
	if (grab!=null && grab!=e.getSource()) return grab.event(e, rel);	// grab handled here so always get lens before/after
	else return super.eventNode(e, rel);
	/*
	if (grabOwner!=null) {
	  EventListener el = grabOwner;	// eventBefore can set grabOwner to null ... generalize this for ranges
//		e.target = null;
	  el.event(e, new Point(curscrn), grabOwner);
	  //be.eventBefore(e, null); be.eventAfter(e, null);
	// send semantic events from event queue to right document + route callback events properly
//	} else if (eid==SemanticEvent.GENERAL && e.getSource()!=this) {
//		XXXBehaviorXXX be = (Behavior)e.getSource();
//		be.event(e);
	}

  }
*/

  public boolean checkRep() {
	//assert super.checkRep(); -- no parent

	assert getParentNode()==null;	// exactly one root per doc
	assert getBrowser()!=null;

	// repeated from INode
	assert size() > 0;
	//assert valid_;	// not when chosen from menu and before repaint/reformat
	assert stickycnt_==0: stickycnt_+" "+sticky_[0];	// no unpaired spans

	for (int i=0,imax=size(); i<imax; i++) {
		Node child = childAt(i);
		assert child!=null;
		assert child.getParentNode() == this: child.getParentNode();	// reverse of check in Node
		assert child.checkRep();

		//assert child.isStruct()?
	}

	return true;
  }

}
