package multivalent.node;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Node;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.Context;
import multivalent.Browser;
import multivalent.TreeEvent;



/**
	Fixed-format INode that adjusts to changes in size of children by moving them horizontally.
	@version $Revision: 1.2 $ $Date: 2002/01/27 03:01:20 $
*/
public class FixedIHBox extends FixedI {
/*  class MovedSpan extends Span {
	public MovedSpan(Node l,int lo, Node r,int ro, Layer layer) { super(l,lo, r,ro, layer); }
	public boolean appearance(Context cx, boolean all) { cx.foreground = Color.RED; return false; }
  }*/

  static final Leaf SPACE = new Leaf(" ", null, null);

  public FixedIHBox(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  public boolean breakBefore() { return false; }
  public boolean breakAfter() { return false; }

  /** Maintain children in order of increasing y coordinate. => nodes added by Node before fully instantiated
  public void appendChild(Node child) {
	super.appendChild(child);
	// insertion sort (usually add at end -- no additional work over super.addChild)
	int newx = ((Fixed)child).getIbbox().x;
	for (int i=size()-1; i>=0; i--) {
		if (newx >= ((Fixed)childAt(i)).getIbbox().x || i==0) {
			children_[i] = child;
			break;
		}
		children_[i]=children_[i-1];
	}
  }*/

  /**
	Adjust for changes in height and width of children.
	X Precondition: ibbox is minimum container of children ibboxes.
  */
  public boolean formatNode(int width,int height, Context cx) {
	// PASS ONE
	// 1. format children as necessary
	// 2. set ibbox
	// 3. find max increase in height
	int maxdeltaheight=-Integer.MIN_VALUE;
	for (int i=0,imax=size(); i<imax /*&& !shortcircuit*/; i++) {
		Node child=childAt(i); Fixed fchild=(Fixed)child;
		Rectangle cbbox=child.bbox, cibbox=fchild.getIbbox();
//if (cibbox==null) { System.out.println(child.getName()+" bbox="+cibbox); System.exit(0); }

		// format
		if (!child.isValid()) child.formatBeforeAfter(width,height, cx); else cx.valid=false;

		// ibbox -- slower but careful vis-a-vis precondition
		if (i==0) ibbox_.setBounds(cibbox); else ibbox_.add(cibbox);

		// increase in height
//if (cbbox.height!=cibbox.height) System.out.println(cbbox.height+" != "+cibbox.height+" @ "+child.getName()+" / "+child.getFirstLeaf().getName());
		maxdeltaheight = Math.max(maxdeltaheight, cbbox.height - cibbox.height);
//if (cbbox.height - cibbox.height !=0) System.out.println("delta height: "+cbbox.height+"<-"+cibbox.height+" @ "+child.getName());
	}


	// PASS TWO
	// 1. realign to same baseline
	// 2. set relative coordinates, adjusting xposn by tracking increase in widths
	ibbox_.x = childAt(0).bbox.x;
	int xdelta=0, x0=ibbox_.x, y0=ibbox_.y;
	/*if (maxdeltaheight!=0)*/ for (int i=0,imax=size(); i<imax; i++) {
		Node child=childAt(i); Fixed fchild=(Fixed)child;
		Rectangle cbbox=child.bbox, cibbox=fchild.getIbbox();

		// make relative
		// maintains baseline even if uneven word box bottoms (baseline field ok)
		cbbox.setLocation(cibbox.x-x0+xdelta, cibbox.y-y0 + maxdeltaheight - (cbbox.height - cibbox.height));
//if ("Camelot".equals(child.getName())) System.out.println(child.getName()+" ("+cibbox.x+","+cibbox.y+") - ("+x0+","+y0+")");

		// tracking (any additional increment from this leaf starts to apply next leaf)
		//xdelta += (cbbox.width - cibbox.width);   // restore this with figure out below
		xdelta = Math.max(0, xdelta + (cbbox.width - cibbox.width));    // HACK: string widths measured differently from construction in PDF
//if (cbbox.width - cibbox.width !=0) System.out.println("delta width: "+cbbox.width+"<-"+cibbox.width+" @ "+child.getName());
	}

//if (xdelta!=0) System.out.println("total xdelta = "+xdelta);
	bbox.setBounds(ibbox_.x,ibbox_.y, ibbox_.width+xdelta, ibbox_.height+maxdeltaheight);

	valid_=true;
	return !valid_;
  }


  /** Fill in interword-space, in particular for backgrounds for highlights and underlines for hyperlinks. */
  public void paintNode(Rectangle docclip, Context cx) {
	Color bgin = cx.pagebackground;
	Graphics2D g = cx.g;
	for (int i=0,imax=size(); i<imax; i++) {
		Node child = childAt(i);
		Rectangle cbbox = child.bbox;

		if (i>0 && cx.valid) {
			int x=(int)cx.x, y=cbbox.y, w=cbbox.x-x+1, h=cbbox.height;
			SPACE.bbox.setBounds(x,y, w,h);
			SPACE.baseline = child.baseline + y;

			//if (cx.background!=cx.pagebackground && cx.background!=null) {
			if (cx.background!=null && !cx.background.equals(bgin)) { g.setColor(cx.background); g.fillRect(x,y, w,h); }

			cx.paintBefore(cx, SPACE);
			cx.x = cbbox.x;
			cx.paintAfter(cx, SPACE);
		}

		cx.pagebackground = bgin;
		child.paintBeforeAfter(docclip, cx);
		cx.x = cbbox.x+cbbox.width;
	}
  }



  /** Assumes left-to-right on same baseline. */
  public boolean formatNodeOLD(int width,int height, Context cx) {
	//boolean shortcircuit=false; -- not allowed

	// format children and compute union of bounding boxes
	// for now just make enclosing bbox set a off.x,off.y
	// later re-enable doublespacing
	// adjust baseline and x
	int imax = size();
	Node child;
	Rectangle cbbox, cibbox;

	int maxbaseline = Integer.MIN_VALUE;
	// move to original positions, so translates below don't accrete
	for (int i=0; i<imax /*&& !shortcircuit*/; i++) {
		child = childAt(i);
		if (!child.isValid()) child.formatBeforeAfter(width,height, cx);
		else { cibbox=((Fixed)child).getIbbox(); child.bbox.setLocation(cibbox.x, cibbox.y); cx.valid=false; }
		int absbase = child.bbox.y + child.baseline;
		if (absbase>maxbaseline) maxbaseline=absbase;
	}

	// containment + adjustment to changed baselines
	for (int i=0; i<imax; i++) {
		child = childAt(i); cbbox = child.bbox;
		int absbase = child.bbox.y + child.baseline;
		int ydelta = maxbaseline-absbase;
//		if (ydelta!=0) {	// XDOC puts things on same line with varying baselines (observed: 11 pixels)
//			cibbox=((Fixed)child).getIbbox();
//			if (!cibbox.equals(cbbox))
		cbbox.translate(0,ydelta);	// baseline still ok it's relative to xn
//		  }
//		  if (ydelta!=0) new MovedSpan(child,0, child,child.size(), null);
		if (ydelta!=0) System.out.println("moved |"+child.getName()+"| by dy="+ydelta);
		if (i==0) bbox.setBounds(cbbox); else bbox.add(cbbox);
	}

	// relative positioning
	//for (int i=0; i<imax; i++) childAt(i).bbox.translate(-bbox.x, -bbox.y);
	//getIbbox().setLocation(bbox.x, bbox.y);	// new reference point for incremental reformatting => for now no incremental on fixed format, which are single page anyhow so not so expensive

	/*if (!shortcircuit)*/ valid_ = true;
	return /*!valid_*/false;
  }



  /**
	Can "hit" in between nodes to pick up prevailing spans (cur node set to first node following, with offset -1).
	Taken from IParaBox.  Maybe put in INode, as "interword".
  */
  public boolean eventNode(AWTEvent e, Point rel) {
	boolean hitchild=false;
//System.out.println("me = "+getName());
	boolean shortcircuit=false;

	//for (int i=size()-1; i>=0 && !shortcircuit; i--) {
	for (int i=0,imax=size(); i<imax && !shortcircuit; i++) {	// for stop to work
		Node child=childAt(i); Rectangle cbbox=child.bbox;
		if (rel!=null && cbbox.contains(rel)) hitchild=true;  // not Node.contains(Point)

		shortcircuit = child.eventBeforeAfter(e,rel);	// regardless of bbox!
	}


	Browser br = getBrowser();
	Node curnode = br.getCurNode();
	int eid=e.getID();
	if (!shortcircuit && !hitchild && curnode==null
		&& ((MouseEvent.MOUSE_MOVED<=eid && eid<=MouseEvent.MOUSE_DRAGGED) || eid==TreeEvent.FIND_NODE)) {

		// if between *leaf nodes*, fake cur node so that pick up prevailing spans
		int i=size()-1;
		for (Node child=null,prev=null; --i >= 0; prev=child) {
			child=childAt(i); Rectangle cbbox=child.bbox;
			if (cbbox.x < rel.x) {	// stop on this regardless
				if (cbbox.y <= rel.y && cbbox.y+cbbox.width >= rel.y	// y ok
					&& prev!=null	// else off right edge
					&& prev.isLeaf() && child.isLeaf()) {	// consecutive leaf nodes only
//System.out.println("between "+child.getName()+" and "+prev.getName());
					//br.setCurNode(prev,-1);	// -1 == fake offset
					prev.eventBeforeAfter(e, new Point(prev.bbox.x, prev.bbox.y));  // end of previous
					// no tickling here
					hitchild=true;
				}
				break;
			}
		}
		if (!hitchild) br.setCurNode(this,0);	// set to lowest enclosing node -- or Leaf only?


		//Node curn = br.getCurNode();
		//if (curn!=null && curn.isLeaf() /*&& curm.offset>=0--no*/) br.tickleActives(e, rel, curn);
	}

	return shortcircuit;
  }
}
