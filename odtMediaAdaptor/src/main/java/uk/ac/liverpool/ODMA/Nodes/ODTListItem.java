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
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.ODMA.Nodes;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Node;
import multivalent.node.IParaBox;
import phelps.lang.Integers;

import com.pt.awt.NFont;

public class ODTListItem extends IParaBox {
	/** Width of UL's symbol. */
	public static final int SWIDTH=6;	// size of symbol unaffected by font size

	static final int INVALID=Integer.MIN_VALUE;
	int value=ODTListItem.INVALID;	// number for OLs, nesting level for UL

	private final int num;

	private final int level;

	public ODTListItem(String name, Map<String,Object> attrs, INode parent) {
		super(name,attrs,parent);
		ODTList p = (ODTList)parent;
		level = p.level;
		p.num++;
		num = p.num;
		//System.out.println("+"+level+"-"+num);
		StringBuilder sb = new StringBuilder(level*3+3);
		for (int i=0;i<level;i++)
			sb.append("   ");
		sb.append(num+")");
		//		new LeafUnicode(sb.toString(),null, this);
	}

	/** Compute number for OL and nesting level for UL here, rather than scrambling during painting. */
	@Override
	public boolean formatNode(int width, int height, multivalent.Context cx) {
		boolean ret = super.formatNode(width,height, cx);	// room left for bullet or number done by style sheet for UL and OL
		return ret;
	}

	/** Draw number or symbol in the left margin. */
	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		super.paintNode(docclip,cx);

		Graphics2D g = cx.g;
		g.setColor(cx.foreground);

		// dynamically determine if in OL or UL and display appropriately
		Node n = getFirstLeaf(); //if (n==null) return; -- check for no children done by paintBeforeAfter(...) (?)
		Point loff = n.getRelLocation(this);
		NFont f = cx.getFont();
		int sw = (int)f.charAdvance(' ').getX();
		int x = loff.x-sw-sw, y=loff.y + n.baseline - 1;//(int)f.getAscent();	// of max too big;//n.baseline;

		INode p = getParentNode();
		String type = p.getAttr("type");	// LATER: take from style sheet ("list-style-type")
		if (true) { // ORDERED LIST!
			// should compute text according to style, such as a/b/c, i/ii/iii, Roman numerals
			//	LI: TYPE=(1|A|a|i|I)
			String txt;
			char ctype = type==null? '1': type.charAt(0);
			//String sval=null;
			if (ctype=='a' || ctype=='A') txt=""+(char)(ctype+num-1);	// why doesn't String.valueOf((char)...) work?
			else if ((ctype=='i' || ctype=='I') && num>0 && num<4000) {
				//txt = Utility.int2Roman(num); // replace this when Java can compute Roman numerals, but this good enough for now
				txt = Integers.toRomanString(num);
				if (ctype=='i') txt=txt.toLowerCase();
			} else txt = Integer.toString(num);

			f.drawString(g, txt, x-(float)f.stringAdvance(txt).getX()-2,y); g.drawLine(x,y-1, x,y-1); // period

		} else {	// UL: TYPE=(disc|circle|square)
			if (type==null) type = num==1? "circle": "fill-square"; // compute based on nesting level

			//x-=SWIDTH; y-=SWIDTH;
			if ("fill-square".equals(type)) g.fillRect(x-ODTListItem.SWIDTH,y-ODTListItem.SWIDTH, ODTListItem.SWIDTH,ODTListItem.SWIDTH);
			else if ("disc".equals(type)) g.fillOval(x-ODTListItem.SWIDTH,y-ODTListItem.SWIDTH, ODTListItem.SWIDTH,ODTListItem.SWIDTH);
			else if ("square".equals(type)) g.drawRect(x-ODTListItem.SWIDTH,y-ODTListItem.SWIDTH, ODTListItem.SWIDTH,ODTListItem.SWIDTH);
			else /*if ("circle".equals(type))*/ f.drawString(g, "o", x-(float)f.stringAdvance("o").getX(),y); //g.drawOval(x,y, SWIDTH,SWIDTH); -- looks awful!
		}
	}

}

