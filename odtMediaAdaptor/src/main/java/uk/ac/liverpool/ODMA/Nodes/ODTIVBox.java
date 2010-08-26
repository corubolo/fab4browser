/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/
package uk.ac.liverpool.ODMA.Nodes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Node;

/**
 Lays out children vertically, top to bottom.
 modified to provide fixed page size

 @version $Revision$ $Date$
 */
public class ODTIVBox extends INode {
	private Insets pageMargin;

	public ODTIVBox(String name, Map<String, Object> attr, INode parent, Dimension size, Insets i) {
		super(name, attr, parent);
		pageSize=size;
		pageMargin=i;
		contSize = new Dimension();
		contSize.width =pageSize.width-pageMargin.left-pageMargin.right;
		contSize.height = pageSize.height-pageMargin.top-pageMargin.bottom;

		System.out.println(contSize);
		System.out.println(pageSize);
		System.out.println(i);
	}
	Dimension pageSize = null;
	Dimension contSize = null;

	Color pagebg;

	public Color getPagebg() {
		return pagebg;
	}

	public void setPagebg(Color pagebg) {
		this.pagebg = pagebg;
	}

	/**
     @param width ignored during formatting
     @param height ignored during formatting
	 */
	 @Override
	 public boolean formatNode(int width, int height, Context cx) {

		 // format children
		 int maxw = 0;
		 int lastbot = 0; // fake child(-1) as having 0-height bottom pageMargin
		 int y = pageMargin.top; // within structural node, renormalize origin
		 width = contSize.width;
		 height = contSize.height;
		 for (int i = 0, imax = size(); i < imax; i++) {
			 Node child = childAt(i);
			 Rectangle cbbox = child.bbox;
			 if (!child.isValid()) {
				 if (!cx.valid) cx.reset(this, -1);
				 child.formatBeforeAfter(width, height - y, cx);
			 } else if (child.sizeSticky() > 0) cx.valid = false;
			 // pageMargins (which only internal nodes have)
			 int h = cbbox.height, ml = 0, mr = 0;
			 if (child.isStruct()) { // top pageMargin negotiation, bottom pageMargin setting
				 INode sn = (INode) child;
				 int mh = Math.max(sn.margin.top, lastbot); // merge bottom-top pageMargins of siblings--only, not parent-child
				 y += mh;
				 cx.eatHeight(mh, child, child.size());
				 lastbot = sn.margin.bottom;
				 ml = sn.margin.left;
				 mr = sn.margin.right;
			 } else {
				 y += lastbot; // full bottom pageMargin if following INode, or 0 if following Leaf
				 cx.eatHeight(h + lastbot, child, child.size()); // node height only once, on leaves (lines in IParaNode)
				 lastbot = 0;
			 }

			 // get float contributions AFTER accounting for top pageMargin
			 cx.flowFloats(y, width);
			 int xoff = cx.getFloatWidth(Node.LEFT);
			 //int mwidth = width /*- cx.pageMarginleft - cx.pageMarginright*/ - xoff - cx.getFloatWidth(RIGHT); -- not used want to pass same width for when float expires in subtree
			 cbbox.setLocation(0 + xoff + ml + pageMargin.left, y); // parents position children (for now, infinite vertical scroll)

			 y += h; // for now, page height is infinite. later vector of Dimension's
			 maxw = Math.max(maxw, xoff + cbbox.width + ml + mr);
		 }
		 y += lastbot; // bottom pageMargin of last child

		 /* MODIFICATION HERE */
		 //        if (y<pageSize.height)
		 //y=pageSize.height;
		 //        else
		 //            y=y+pageMargin.top;
		 bbox.setSize(pageSize.width,y); // dimensions of perhaps partial contents, (x,y) set by parent
		 //bbox.setSize(maxw, y);
		 // growing by border and padding handled by formatBeforeAfter
		 //cx.eatHeight(lastbot , child,child.size());   // count pageMargins+border+padding here

		 //valid_ = !shortcircuit);
		 valid_ = true;
		 //    if (debug) { System.out.println(childAt(0).getName()); }
		 //System.out.println("formatted "+getName()+"/"+childNum());
		 //if (!valid_) System.out.println("\tIVBox not valid!");
		 return false; //shortcircuit;
	 }

	 /**
     Since children layed out top to bottom, can stop painting when child.bbox.y > clip.y+clip.height.
	  */
	 @Override
	 public void paintNode(Rectangle docclip, Context cx) {
		 //Color bgin = cx.pagebackground;
		 //if (bgin!=Color.WHITE) System.out.println("IVBox "+getName()+"  "+bgin);
		 //docclip.width = pageSize.width;
		 Color cp = cx.g.getColor();
		 cx.g.setColor(pagebg);
		 cx.g.fillRect(0,0,pageSize.width,pageSize.height);
		 //cx.g.translate(pageMargin.left, pageMargin.top);
		 //cx.g.setColor(Color.RED);
		 //docclip.translate(-pageMargin.left,-pageMargin.top);
		 int starty = docclip.y, stopy = starty + docclip.height;
		 //cx.g.drawRect(0, 0,contSize.width,contSize.height);
		 cx.g.setColor(cp);
		 for (int i = 0, imax = size(); i < imax; i++) {
			 Node child = childAt(i);
			 Rectangle cbbox = child.bbox;
			 if (cbbox.y + cbbox.height < starty) { /* nothing*/} else if (cbbox.y < stopy) {
				 cx.pagebackground = pagebg;
				 childAt(i).paintBeforeAfter(docclip, cx);
			 } else {
				 cx.valid = false;
				 break;
			 }
		 }
		 cp = cx.g.getColor();
		 cx.g.setColor(Color.RED);
		 ((Graphics)cx.g).drawRect(pageMargin.left, pageMargin.top,contSize.width,contSize.height);
		 //cx.g.translate(-pageMargin.left, -pageMargin.top);
		 //docclip.translate(pageMargin.left, pageMargin.top);
		 cx.g.setColor(cp);

	 }
}
