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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

import multivalent.Context;
import multivalent.Document;
import multivalent.INode;
import multivalent.Node;
import multivalent.gui.VScrollbar;
import multivalent.node.IVBox;

/**
 * @author fabio
 * 
 */
public class ODTFrame extends IVBox {
	boolean border1 = true;

	public ODTFrame(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);

		String frameborder = getAttr("frameborder");
		if ("0".equals(frameborder))
			border1 = false;

	}

	@Override
	public boolean formatNode(int width, int height, Context cx) {
		byte policy = VScrollbar.SHOW_NEVER; // auto is default
		policy = VScrollbar.SHOW_NEVER;
		for (int i = 0, imax = size(); i < imax; i++) {
			Node child = childAt(i);
			if (child instanceof Document) {
				((Document) child).setScrollbarShowPolicy(policy);
				break;
			}
		}

		//System.out.println(cx.marginbottom);
		//System.out.println(margin.bottom);

		int pw = bbox.width;
		//		int ph = bbox.height;
		super.formatNode(bbox.width!=0?bbox.width:width,bbox.height!=0?bbox.height:height, cx);
		if (pw!=0)
			bbox.width = pw;

		//		if (ph!=0)
		//			bbox.height= ph;
		return false;
	}

	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		super.paintNode(docclip, cx);
		if (border1) {
			Graphics2D g = cx.g;
			Color p = g.getColor();
			g.setColor(Color.YELLOW);
			g.drawRect(0, 0, bbox.width, bbox.height);
			g.setColor(p);
		}
	}
}