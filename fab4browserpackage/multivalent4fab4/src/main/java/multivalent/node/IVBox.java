package multivalent.node;

import java.awt.Rectangle;
import java.awt.Color;
import java.util.Map;

import multivalent.Node;
import multivalent.INode;
import multivalent.Context;


/**
	Lays out children vertically, top to bottom.

	@version $Revision: 1.4 $ $Date: 2002/02/02 13:41:39 $
*/
public class IVBox extends INode {
  public IVBox(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  /**
	@param width ignored during formatting
	@param height ignored during formatting
  */
  public boolean formatNode(int width,int height, Context cx) {
	boolean shortcircuit=false;

	// format children
	int maxw=0;
	int lastbot=0;	  // fake child(-1) as having 0-height bottom margin
	int y=0;   // within structural node, renormalize origin
	//cx.eatHeight(border.top+padding.top, this,size());  // top margin handled by parent

	for (int i=0,imax=size(); i<imax /*&& fillH>y -- wait for background format*//* && !shortcircuit*/; i++) {
		Node child = childAt(i);
		Rectangle cbbox = child.bbox;
		if (!child.isValid()) {
			if (!cx.valid) cx.reset(this,-1);
			shortcircuit = child.formatBeforeAfter(width, height-y, cx);
		} else if (child.sizeSticky()>0) cx.valid=false;
		//if (!child.isValid()) child.formatBeforeAfter(width, height, cx);
/*if (debug && shortcircuit) {
	System.out.print("formatted enough at "+child.getName()+"/");
	for (Node m=child.getFirstLeaf(),p2=m.getParentNode(); m.getParentNode()==p2; m=m.getNextLeaf()) System.out.print(m.getName()+" ");
	System.out.println();
}*/
		// margins (which only internal nodes have)
		int h = cbbox.height, ml=0, mr=0;
		if (child.isStruct()) { // top margin negotiation, bottom margin setting
			INode sn=(INode)child;
			int mh = Math.max(sn.margin.top, lastbot);	// merge bottom-top margins of siblings--only, not parent-child
			y+=mh;
			cx.eatHeight(mh, child,child.size());
			lastbot = sn.margin.bottom;
			ml=sn.margin.left; mr=sn.margin.right;
		} else {
			y += lastbot;	// full bottom margin if following INode, or 0 if following Leaf
			cx.eatHeight(h+lastbot, child,child.size());	 // node height only once, on leaves (lines in IParaNode)
			lastbot=0;
		}

		// get float contributions AFTER accounting for top margin
		cx.flowFloats(y, width);
		int xoff = cx.getFloatWidth(LEFT);
		//int mwidth = width /*- cx.marginleft - cx.marginright*/ - xoff - cx.getFloatWidth(RIGHT); -- not used want to pass same width for when float expires in subtree
		cbbox.setLocation(0+xoff + ml,y);	// parents position children (for now, infinite vertical scroll)

		y += h;	// for now, page height is infinite. later vector of Dimension's
		maxw = Math.max(maxw, xoff+cbbox.width + ml+mr);
	}
	y += lastbot;	// bottom margin of last child

	bbox.setSize(maxw, y);	// dimensions of perhaps partial contents, (x,y) set by parent

	// growing by border and padding handled by formatBeforeAfter
	//cx.eatHeight(lastbot , child,child.size());	// count margins+border+padding here

	//valid_ = !shortcircuit);
	valid_=true;
//	  if (debug) { System.out.println(childAt(0).getName()); }
//System.out.println("formatted "+getName()+"/"+childNum());
//if (!valid_) System.out.println("\tIVBox not valid!");
	return false;	//shortcircuit;
  }

  /**
	Since children layed out top to bottom, can stop painting when child.bbox.y > clip.y+clip.height.
  */
  public void paintNode(Rectangle docclip, Context cx) {
	Color bgin = cx.pagebackground;
//if (bgin!=Color.WHITE) System.out.println("IVBox "+getName()+"  "+bgin);
	int starty=docclip.y, stopy=starty + docclip.height;
	for (int i=0,imax=size(); i<imax; i++) {
		Node child=childAt(i); Rectangle cbbox=child.bbox;
		if (cbbox.y + cbbox.height < starty) { /* nothing*/ }
		else if (cbbox.y < stopy) { cx.pagebackground=bgin; childAt(i).paintBeforeAfter(docclip, cx); }
		else { cx.valid=false; break; }
	}
  }
}
