package multivalent.node;

import java.awt.Rectangle;
import java.util.*;

import multivalent.INode;
import multivalent.Node;
import multivalent.Context;



/**
	Internal node for use with formats that are already layed out,
	such as scanned page images, PostScript, PDF, TeX DVI.
	Identical to INode except for formatting.
	If children in vertical or horizontal sequence, use FixedIVBox or FixedIHBox.
	Have to format entire page, rather than just enough to fill the screen,
	but this is fine as just processing a page or two or three at a time.
	Children must all be of type Fixed.

	@version $Revision: 1.5 $ $Date: 2002/06/01 14:24:47 $
*/
public class FixedI extends INode implements Fixed {
  protected Rectangle ibbox_ = new Rectangle(0,0,0,0); //-1,-1);	 // actually, (x,y) computed from current children

  public Rectangle getIbbox() { return ibbox_/*.clone()*/; }


  public FixedI(String name, Map<String,Object> attr, INode parent) {
	super(name,attr, null);
	// let ibbox initialize
	if (parent!=null) parent.appendChild(this);
  }

  /**
	Add children with original, absolute fixed coordinates.
	(Doesn't work for XDOC and perhaps others as add children before know coordinates
	XDOC doesn't know size until get fonts and fonts come at the end--so it has to compute ibboxes itself.)
  */

  /* * Grow ibbox as necessary to contain child's ibbox. => done in formatting, since many ways to lose this invariant
  public void appendChild(Node child) {
	super.appendChild(child);
	Rectangle cibbox = ((Fixed)child).getIbbox();
	if (size()==1) ibbox.setBounds(cibbox); else ibbox_.add(cibbox);
  }

  /** Shrink ibbox now that don't have to contain removed child's ibbox. * /
  public Node removeChildAt(int num) {
	Node zap = super.removeChildAt(num);
	Rectangle cibbox = ((Fixed)zap).getIbbox();
	if (!ibbox_.contains(cibbox)) {
		// recompute
		if (size()==0) ibbox_.setBounds(0,0,0,0);
		else {
			ibbox_.setBounds(((Fixed)childAt(0)).getIbbox());
			for (int i=1,imax=size(); i<imax; i++) ibbox_.add(((Fixed)childAt(i)).getIbbox());
		}
	}
	return zap;
  } */


  public boolean formatNode(int width,int height, Context cx) {
	return formatNode(this, true, width, height, cx);
  }

  /**
	Fixed internal nodes need to be able to format: make children relative, and reposition children in reponse to changing dimensions.
	However, generally they inherit from a primary flowed node, and so can't do reposition by inheritance.
	This is a generally available (<code>public static</code>) method that formats the passed fixed internal node.
	@param p            Fixed, internal node to format (N.B. fixed not type checked)
	@param shrinkwrap   if true set FixedI's ibbox to just enclose children, if false accept ibbox as is.  Usually true but false for FixedIClip.
  */
  public static boolean formatNode(INode p, boolean shrinkwrap, int width,int height, Context cx) {
	Rectangle ibbox = ((Fixed)p).getIbbox(); //int size=p.size();

	// 1. Format chlidren and determine origin
	for (int i=0,imax=p.size(); i<imax /*&& !shortcircuit*/; i++) {
		assert p.childAt(i) instanceof Fixed: "not Fixed: |"+p.childAt(i).getName()+"/"+p.childAt(i).getClass().getName()+"|";    // => assert of appendChild
		Node child = p.childAt(i);

		// format
		if (!child.isValid()) child.formatBeforeAfter(width,height, cx); else if (child.sizeSticky()>0) cx.valid=false;
		//child.getBbox().setBounds(cibbox); => WRONG.  bbox may have grown

		if (shrinkwrap) {
			Rectangle cibbox = ((Fixed)child).getIbbox();
			if (i>0) ibbox.add(cibbox); else ibbox.setBounds(cibbox);     // wait until formatted
		}
	}


	// 2. Make relative
	int x0=ibbox.x, y0=ibbox.y;
	for (int i=0,imax=p.size()/*, dx=-ibbox.x, dy=-ibbox.y*/; i<imax; i++) {
		Fixed child = (Fixed)p.childAt(i);
//if (child==null) { System.out.println("null child"); System.exit(1); }
//if (child.getBbox()==null || child.getIbbox()==null) { System.out.println(p.childAt(i).getName()+" bbox="+child.getBbox()+", ibbox="+child.getIbbox()); System.exit(1); }
		Rectangle cibbox = child.getIbbox();
		child.getBbox().setLocation(cibbox.x-x0, cibbox.y-y0);
//System.out.println(p.childAt(i).getName()+" ("+cibbox.x+","+cibbox.y+") - ("+x0+","+y0+")");
		//p.childAt(i).getBbox().translate(dx,dy);
	}
//System.out.println(getName()+" @ ("+x0+","+y0+")    ");


	// 3. adjust xposn+yposn by tracking increase in widths
	//	  no natural ordering, so have to search others for ones to the right or below
	int deltax=0, deltay=0;
	boolean fmoved = false;
	/*if (maxdeltaheight!=0)*/ /*if (false)*/ for (int i=0,imax=p.size(); i<imax; i++) {
		Fixed child = (Fixed)p.childAt(i);
		Rectangle cbbox=child.getBbox(), cibbox = child.getIbbox();

		// tracking
		int dx=(cbbox.width - cibbox.width), dy=(cbbox.height - cibbox.height);
		deltax += dx; deltay += dy;
//if (dx!=0 || dy!=0) System.out.println(child.getName()+", dx="+dx+", dy="+dy);

		// move others out of the way
		if (Math.abs(dx)>=2 || Math.abs(dy)>=2 /*dx!=0 || dy!=0*/) {	// allow some fuzz
//System.out.println("moving "+p.childAt(i).getName()+" dx="+deltax+", dy="+deltay+", setting MOVED");
			fmoved = true;

			for (int j=0; j<imax; j++) {    // have O(n^2) because no ordering guaranteed on children (no big deal since usually dealing with single page only)
				Rectangle ccbbox = p.childAt(j).bbox;
				// beneath
				if (dy!=0 && ccbbox.y > cbbox.y && (ccbbox.x < cbbox.x+cbbox.width && ccbbox.x+ccbbox.width > cbbox.x)) ccbbox.translate(0, dy);
				// to the right
				if (dx!=0 && ccbbox.x > cbbox.x && (ccbbox.y < cbbox.y+cbbox.height && ccbbox.y+ccbbox.height > cbbox.y)) ccbbox.translate(dx, 0);
			}
		}
	}
	if (fmoved) p.getDocument().putAttr(Fixed.ATTR_REFORMATTED, Fixed.ATTR_REFORMATTED);


	// 4. union bbox, after moving
	//if (p.size()==0) ibbox.setBounds(0,0,0,0);

	//bbox.setLocation(ibbox_.x,ibbox_.y);	// setBounds(ibbox)?
	//for (int i=0,imax=p.size()(); i<imax; i++) bbox.add(childAt(i).bbox);
//bbox.setBounds(ibbox_);
	p.getBbox().setBounds(ibbox.x,ibbox.y, ibbox.width + deltax, ibbox.height + deltay);
//System.out.println(p.getName()+" "+ibbox+" => "+p.getBbox()+" ("+deltax+","+deltay+")");
//if (p.size()==0) System.out.println(ibbox+" => "+p.getBbox());
//if ("mediabox".equalsIgnoreCase(getName())) System.out.println(getName()+" ibbox="+ibbox_+", bbox="+bbox);

//if (xdelta!=0) System.out.println("total xdelta = "+xdelta+", ydelta="+ydelta);
	//bbox.setBounds(ibbox_.x,ibbox_.y, ibbox_.width+xdelta+maxdeltawidth, ibbox_.height+ydelta+maxdeltaheight);

	p.setValid(true);
	return false;   //!valid_;
  }


  public void reformat(Node dirty) {
	// incremental words for fixed ?/!
	markDirty();
	repaint(50);
  }



  //public String toString() { return name_" "+getClass().getName()+" "+bbox+", ibbox="+ibboxobservers="+observers_+", valid="+valid_; }

  public void dump(int level, int maxlevel) {
	System.out.print(level); for (int i=0; i<level; i++) System.out.print("  ");
	System.out.println(
		("img".equals(name_)? getAttr("src"): name_)
		+"/"+childNum()
//		+ ", nextLeaf = "+getNextLeaf()
		+", class="+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1)
		+", bbox="+bbox.width+"x"+bbox.height+"@("+bbox.x+","+bbox.y+")/"+ baseline
		+", ibbox="+ibbox_.width+"x"+ibbox_.height+"@("+ibbox_.x+","+ibbox_.y+")/"//+ ibaseline
//		+", sticky="+sticky_
//		+", owner="+owner
//		+", parent="+parent_
//		+", baseline="+baseline
		+", valid="+valid_
//		+", observers="+observers_
		);

	if (level<maxlevel) for (int i=0,imax=size(); i<imax; i++) childAt(i).dump(level+1, maxlevel);
  }
}
