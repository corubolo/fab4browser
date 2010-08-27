package multivalent.node;

import java.util.List;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Browser;
import multivalent.Span;
import multivalent.INode;
import multivalent.Node;
import multivalent.Context;
import multivalent.ContextListener;
import multivalent.EventListener;



/**
	Not used -- done with spans instead, as in {@link multivalent.std.Outliner}.
	Open/collapable display of subtree.
	treats first node (i.e., subtree) specially: as title
	subsequent nodes (subtrees) can be outline nodes too, for nested outline structure
	Can override format method to fault in content on demand.

	LATER:
	pass along outline level in a signal(?)
	pass along outline numbering style in a signal(?)
	draw connecting lines from parent to children (of type OutlineSpan)
	various builtin icon types, including ability to supply open and closed icon images

	@version $Revision: 1.2 $ $Date: 2002/01/27 02:49:55 $
*/
public class IOutline extends IVBox implements EventListener {
/*  static class OutlineTitleSpan extends Span {   // anonymous?
	public boolean appearance(Context cx, boolean all) { cx.marginleft=IOutline.iconW; cx.marginright=10; return false; }
	public int getPriority() { return ContextListener.PRIORITY_SPAN-1; }  // wins over all other spans
  }*/

  static class OutlineSpan extends Span {   // anonymous?
	boolean open=true;
	public boolean appearance(Context cx, boolean all) { cx.elide = !open; cx.marginleft=IOutline.iconW; cx.marginright=10; return false; }
	public int getPriority() { return ContextListener.PRIORITY_SPAN-1; }  // wins over all other spans
  }

  //static OutlineTitleSpan titlespan = new OutlineTitleSpan();
  static final int NONE=0, ARROW=1, PLUSMINUS=2, BOXEDPLUSMINUS=3, /*last one*/ICONIMAGE=4;
  static int icontype = ARROW;	  // static for now
  static Image iconOpen=null, iconClosed=null;	  // static for now
  static /*protected*/ int iconW = 15, iconH=15;


  // INSTANCE
  OutlineSpan span = new OutlineSpan();

  boolean open_ = false;
  /** Can set whether to allow Notemarks (cool) or not (faster when collapsed) (this should be passed along?) */
  boolean nb = true;

  // event processing -- can be static because only clicking on one at a time
  private static boolean active=false;
  private static int screenx, screeny;


  // CONSTRUCTORS
  public IOutline(String name, Map<String,Object> attr, INode parent) {
	this(name, attr, parent, false);
  }

  public IOutline(String name, Map<String,Object> attr, INode parent, boolean open) {
	super(name,attr, parent);
	open_ = open;
	span.open = open_;
  }

  public void appendChild(Node child) {
	super.appendChild(child);
//RESTORE!	LATER	 if (size()==1) ((INode)child).addStruct(titlespan); else ((INode)child).addStruct(span);
  }

  // could be formatAfter
/*
  public boolean formatNode(int width,int height, Context cx) {
	boolean ret = super.formatNode(width-iconW-10,height, cx);
	for (int i=0,imax=size(); i<imax; i++) childAt(i).bbox.translate(iconW,0);
	bbox.grow(iconW,0);
	return ret;
  }
*/

/*
  // PROTOCOLS
  // modified from INode
  public boolean formatNode(int width,int height, Context cx) {
	boolean shortcircuit=false;

	// format children
	int y=0;   // within structural node, renormalize origin

	// first subtree special: always shown
	// LATER: automatic numbering (should this be done on the fly?)
	span.elide = false; cx.addStruct(span);
	Node titleNode = childAt(0);
	Rectangle titleBbox = titleNode.bbox;
	if (!titleNode.isValid()) titleNode.formatBeforeAfter(width-iconW,height, cx); else cx.valid=false;
	titleNode.bbox.move(iconW,y);
	if (titleBbox.height<iconH) { titleNode.baseline += (iconH-titleBbox.height); titleBbox.height=iconH; }
	y += titleBbox.height; height -= titleBbox.height;

	if (open_ || nb) {
		span.elide=!open_; cx.valid=false;
		for (int i=1,imax=size(); i<imax && !shortcircuit; i++) {
			Node child = childAt(i);
			Rectangle cbbox = child.bbox;
			if (!child.isValid()) shortcircuit = child.formatBeforeAfter(width-iconW,height, cx);
			else cx.valid=false;
if (debug && shortcircuit) {
	System.out.print("formatted enough at "+child.getName()+"/");
	for (Node m=child.getFirstLeaf(),p2=m.getParentNode(); m.getParentNode()==p2; m=m.getNextLeaf()) System.out.print(m.getName()+" ");
	System.out.println();
}
			cbbox.move(iconW,y);

			int h = cbbox.height;
			y += h; height -= h;
		}
	}
	bbox.resize(width, y);

	cx.removeStruct(span);

//	  valid_ = !shortcircuit;
	return !valid_;
  }
*/

  public void paintNode(Rectangle docclip, Context cx) {
	super.paintNode(docclip,cx);
	drawIcon(docclip, cx);

	//if (!valid) formatBeforeAfter(g, ) --> LATER
/*
	span.elide = false;
	cx.addStruct(span);

	childAt(0).paintBeforeAfter(g, docclip, cx);	// title always shown (except if user overrides with span...)
	drawIcon(g, docclip, cx);

	if (open_ || nb) {
		span.elide=!open_; cx.reset();
		for (int i=1,imax=size(); i<imax; i++) {
			Node child = childAt(i);
			if (docclip.intersects(child.bbox)) child.paintBeforeAfter(g, docclip, cx);
			else cx.valid=false;
		}
	}

	cx.removeStruct(span);
*/
  }


  /** if click in arrow, toggles */
  //protected final MouseEvent MOUSERELEASED = new MouseEvent(null/*onode*/, MouseEvent.MOUSE_RELEASED, 0/*System.currentTimeMillis()*/, 0, 0,0,/*curscrn.x,curscrn.y*/ 0, false);
  //public boolean event(AWTEvent e, Point scrn) { return eventNode(e,scrn); }	// TEMP!
  public void event(AWTEvent e) { eventNode(e,null); }    // WRONG.  split up eventNode()
  public boolean eventNode(AWTEvent e, Point rel) {
	Point scrn = getBrowser().getCurScrn();
	int x=scrn.x, y=scrn.y;
	boolean in = (/*e.x<iconW &&*/ rel.y<childAt(0).bbox.height);
	int eid = e.getID();
	if (eid==MouseEvent.MOUSE_PRESSED && in) {	  // in relative coordinates, remember
		active=true;
		getBrowser().setGrab(this);
		screenx=x; screeny=y;
	} else if (eid==MouseEvent.MOUSE_RELEASED && active) {
		active=false;
//SHOW!		/*if (in) {*/ toggle(); show(); /*}*/
		getBrowser().releaseGrab(this);
	} else if (active) {
		if (Math.abs(screenx-x)>5 || Math.abs(screeny-y)>5) {
			Browser br = getBrowser();
			active=false; br.releaseGrab(this);
			//XXXeid=MouseEvent.MOUSE_DOWN; // can't change anymore
			return false;	// reinterpret later
		} // else gobble
	} else return super.eventNode(e, rel);
	return true;
  }


  // for subclasses to override
  public void setIconType(int choice) {
	assert choice>=NONE && choice<=ICONIMAGE;
	icontype = choice;
  }

  public void setIconImages(Image open, Image closed) {
	iconOpen=open; iconClosed=closed; icontype=ICONIMAGE;
  }

  protected void drawIcon(Rectangle docclip, Context cx) {
	int midY = childAt(0).bbox.height / 2;

	Graphics2D g = cx.g;
	g.setColor(cx.foreground);
	switch (icontype) {
		case NONE: break;
		case ARROW:
			if (open_) {
				g.drawLine(2,midY, 8,midY); g.drawLine(8,midY, 5,midY+3); g.drawLine(5,midY+3, 2,midY);
			} else {
				g.drawLine(2,midY-3, 2,midY+3); g.drawLine(2,midY+3, 8,midY); g.drawLine(8,midY, 2,midY-3);
			}
			break;
		case PLUSMINUS: break;
		case BOXEDPLUSMINUS: break;
		case ICONIMAGE: break;
		default: assert false: icontype;
	}
  }


  public boolean isOpen() { return open_; }
  public void setOpen() { setOpen(true); }
  public void setOpen(boolean o) { if (open_!=o) toggle(); }
  public void toggle() {
//System.out.println("toggle "+name_);
	open_ = !open_;
	span.open = open_;

	if (nb) markDirtySubtree(true);
  }


/*  public boolean checkRep() {
	assert super.checkRep();

	// ok for children to be 0x0

	return true;
  }*/
}
