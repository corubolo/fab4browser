package multivalent.node;

import java.awt.Rectangle;
import java.util.Map;

import multivalent.Node;
import multivalent.INode;
import multivalent.Context;


/**
	FixedIVBox

	@version $Revision: 1.2 $ $Date: 2002/01/27 02:58:53 $
*/
public class FixedIVBox extends FixedI {
  public FixedIVBox(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  /** Maintain children in order of increasing y coordinate. */
  public void appendChild(Node child) {
	super.appendChild(child);
	// insertion sort (usually add at end -- no additional work over super.addChild)
/* good to do, but ibbox not established until superclass instantiated and called this
	int newy = ((Fixed)child).getIbbox().y;
	for (int i=size()-1; i>=0; i--) {
		if (newy >= ((Fixed)childAt(i)).getIbbox().y || i==0) {
			children_[i] = child;
			break;
		}
		children_[i]=children_[i-1];
	}*/
  }

  /**
	Adjust for changes in height and width of children.
	Precondition: ibbox is minimum container of children ibboxes.
	Formatting too late to sort by y as already made decorations whose locations are based on tree structure.
  */
  public boolean formatNode(int width,int height, Context cx) {
	// NO super.formatNode(width, height, cx); => completely replace

	// PASS ONE
	// 1. format children as necessary
	// 2. find max increase in width
	// 3. adjust xposn by tracking increase in heights
	int maxdeltawidth=-Integer.MIN_VALUE;
	for (int i=0,imax=size(); i<imax /*&& !shortcircuit*/; i++) {
		Node child=childAt(i); Fixed fchild=(Fixed)childAt(i);
		Rectangle /*cbbox=child.bbox,*/ cibbox=fchild.getIbbox();

		// format
		if (!child.isValid()) child.formatBeforeAfter(width,height, cx); else cx.valid=false;

		// ibbox containment
		if (i==0) ibbox_.setBounds(cibbox); else ibbox_.add(cibbox);
	}

	// PASS TWO
	// 1. set relative coordinates, adjusting yposn by tracking increase in heights
	// (need two passes because don't know own ibbox (x,y) during first pass), alas
	int ydelta=0, relx=ibbox_.x, rely=ibbox_.y;
	//int[] dys = new int[size()];     // cheap! not in heap!
	for (int i=0,imax=size(); i<imax; i++) {
		Node child=childAt(i); Fixed fchild=(Fixed)child;
		Rectangle cbbox=child.bbox, cibbox=fchild.getIbbox();

		// make relative
		cbbox.setLocation(cibbox.x-ibbox_.x, cibbox.y-ibbox_.y+ydelta);
		//cbbox.setLocation(cibbox.x-relx, cibbox.y-rely);

		// tracking
		ydelta += (cbbox.height - cibbox.height);
		//int dy = dys[i] = cbbox.height - cibbox.height;
		//ydelta += dy;

		// increase in width
		maxdeltawidth = Math.max(maxdeltawidth, cbbox.width - cibbox.width);
	}


/*
	// PASS THREE: relocating
	if (ydelta>0) for (int i=0,imax=size(); i<imax; i++) {
		int dy = dys[i];
		if (dy != 0) {   // don't assume sorted by y anymore, which is much more inefficient -- but fixed-formats are all single-page so ok
			Node child=childAt(i); int y0 = child.bbox.y /*+ child.bbox.height -1 * /;
//System.out.println("dy="+dy+" below "+y0);
			for (int j=0; j<imax; j++) {
				Rectangle r = childAt(j).bbox;
				if (r.y > y0) {r.translate(0, dy); System.out.println("bump "+childAt(j).getName()+" @ "+r.y);}
			}
		}
	}
*/
//if (ydelta!=0) System.out.println("total ydelta = "+ydelta);
	// so ScanWorkX doesn't report all ink on the page, so we draw the full image underneath, except if there's reformatting in which case that would be bad
	if (ydelta>2) getDocument().putAttr(Fixed.ATTR_REFORMATTED, Fixed.ATTR_REFORMATTED);
	bbox.setBounds(ibbox_.x,ibbox_.y, ibbox_.width+maxdeltawidth, ibbox_.height+ydelta);

	valid_=true;
	return !valid_;
  }


  /** Adjust for changes in height and width of children. */
  public boolean formatNodeOLD(int width,int height, Context cx) {
	//boolean shortcircuit=false; -- not allowed

	// format children and compute union of bounding boxes

	int imax = size();
	Node child;
//	Rectangle ibbox=getIbbox();
	Rectangle cbbox, cibbox;
	//int xdelta=0, ydelta=0; -- parts might come out of order; in line can specialize with delta, baseline

	// move to original positions, so translates below don't accumulate
	for (int i=0; i<imax /*&& !shortcircuit*/; i++) {
		child = childAt(i);
		if (!child.isValid()) child.formatBeforeAfter(width,height, cx);
		/*else {*/ cibbox=((Fixed)child).getIbbox(); child.bbox.setLocation(cibbox.x,cibbox.y); cx.valid=false; /*}*/
	}

	// adjustments for growing and shrinking
	for (int i=0; i<imax; i++) {
		child = childAt(i); cbbox = child.bbox;
		int xdelta=0, ydelta=0;
		if (child instanceof Fixed) {
			cibbox=((Fixed)child).getIbbox();
			xdelta=cbbox.width-cibbox.width; ydelta=cbbox.height-cibbox.height;
		} else {	// treat as though initial dimensions were 0x0
			xdelta=cbbox.width; ydelta=cbbox.height;
		}
		if (Math.abs(xdelta)>=2 || Math.abs(ydelta)>=2 /*xdelta!=0 || ydelta!=0*/) {	// allow some fuzz
//System.out.println("moving "+child.getName()+" x "+xdelta+", y "+ydelta+", setting MOVED");
			getDocument().putAttr(Fixed.ATTR_REFORMATTED, Fixed.ATTR_REFORMATTED);
			for (int j=0; j<imax; j++) {	// generalizes to line?
				if (j==i) continue;	// don't bump yourself
				Rectangle cbbox2 = childAt(j).bbox;
				// can't assume any ordering, unfortunately (maybe I should sort)
				if (ydelta!=0 && cbbox2.y >= cbbox.y && (cbbox.x+cbbox.width>cbbox2.x && cbbox.x<cbbox2.x+cbbox2.width)) cbbox2.translate(0,ydelta);
				if (xdelta!=0 && cbbox2.x >= cbbox.x && (cbbox.y+cbbox.height>cbbox2.y && cbbox.y<cbbox2.y+cbbox2.height)) cbbox2.translate(xdelta,0);
			}
		}
	}

	// containment
	cbbox = childAt(0).bbox; bbox.setBounds(cbbox);
	for (int i=0+1; i<imax; i++) bbox.add(childAt(i).bbox);

	// relative positioning
	for (int i=0; i<imax; i++) childAt(i).bbox.translate(-bbox.x, -bbox.y);
//if (!ibbox.equals(bbox)) System.out.println("v moved "+ibbox+" => "+bbox);
	//ibbox.setLocation(bbox.x, bbox.y);	   // new reference point for incremental reformatting


	/*if (!shortcircuit)*/ valid_ = true;
	return /*!valid_*/false;
  }
}
