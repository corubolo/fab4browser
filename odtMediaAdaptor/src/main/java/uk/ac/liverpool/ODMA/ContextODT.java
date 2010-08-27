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
package uk.ac.liverpool.ODMA;

import multivalent.Context;
import multivalent.INode;

/**
 * @author fabio
 * 
 * @see multivalent.std.adaptor.CSSContext
 *
 */
public class ContextODT extends Context {


	@Override
	public boolean paintBefore(Context cx, INode node) {
		//			Insets border = node.border;
		//			node.border = INode.INSETS_ZERO;
		boolean ret = super.paintBefore(cx, node);
		//			node.border = border;
		//
		//			Rectangle bbox = node.bbox;
		//			int x=bbox.x, y=bbox.y, w=bbox.width, h=bbox.height;    // we're in parent's coordinate space
		//
		//
		//			// border: cross product of style x color x width, so lots of duplication
		//			// none | dotted | dashed | solid | double | groove | ridge | inset | outset
		////		System.out.println(borderstyle+", "+cborderleft+"/"+border.left);
		////		borderstyle = "solid";
		//			if (border==INode.INSETS_ZERO || BORDER_NONE.equals(borderstyle)) { // nothing
		//			} else {
		//				int bw, bh;
		//				if ((bw=border.left)>0) {
		//					g.setColor(cborderleft!=Context.COLOR_INHERIT? cborderleft: foreground);
		//					if (BORDER_SOLID.equals(borderstyle) || borderstyle==Context.STRING_INVALID) g.fillRect(x,y, bw,h);
		//					else if (BORDER_DOUBLE.equals(borderstyle)) { int dw=Math.max(bw/3,1); g.fillRect(x,y, dw,h); g.fillRect(x+bw-dw,y +bw, dw,h -bw-bw); }
		//					else if (BORDER_DASHED.equals(borderstyle)) { if (h>=10) for (int i=0,imax=h-5; i<imax; i+=10) g.fillRect(x,y+i, bw,5); else g.fillRect(x,y, bw,h); }
		//					else if (BORDER_DOTTED.equals(borderstyle)) { if (h>=bw) for (int i=0,imax=h-bw; i<imax; i+=bw+bw) g.fillOval(x,y+i, bw,bw); else g.fillRect(x,y, bw,h); }
		//					else if (BORDER_GROOVE.equals(borderstyle)) { int dw=bw/2; g.setColor(Color.GRAY); g.fillRect(x,y, dw,h); g.setColor(Color.WHITE); g.fillRect(x+dw,y, dw,h); }
		//					else if (BORDER_RIDGE.equals(borderstyle)) {}
		//					else if (BORDER_INSET.equals(borderstyle)) {}
		//					else if (BORDER_OUTSET.equals(borderstyle)) {}
		//				}
		//
		//				if ((bw=border.right)>0) {
		//					int xr = x+w-bw;
		//					g.setColor(cborderright!=Context.COLOR_INHERIT? cborderright: foreground);
		//					if (BORDER_SOLID.equals(borderstyle) || borderstyle==Context.STRING_INVALID) g.fillRect(xr,y, bw,h);
		//					else if (BORDER_DOUBLE.equals(borderstyle)) { int dw=Math.max(bw/3,1); g.fillRect(x+w-dw,y, dw,h); g.fillRect(xr,y +bw, dw,h -bw-bw); }
		//					else if (BORDER_DASHED.equals(borderstyle)) { if (h>=10) for (int i=0,imax=h-5; i<imax; i+=10) g.fillRect(xr,y+i, bw,5); else g.fillRect(xr,y, bw,h); }
		//					else if (BORDER_DOTTED.equals(borderstyle)) { if (h>=bw) for (int i=0,imax=h-bw; i<imax; i+=bw+bw) g.fillOval(xr,y+i, bw,bw); else g.fillRect(xr,y, bw,h); }
		//					else if (BORDER_GROOVE.equals(borderstyle)) { int dw=bw/2; g.setColor(Color.GRAY); g.fillRect(xr,y, dw,h); g.setColor(Color.WHITE); g.fillRect(xr+dw,y, dw,h); }
		//				}
		//
		//				if ((bh=border.top)>0) {
		//					g.setColor(cbordertop!=Context.COLOR_INHERIT? cbordertop: foreground);
		//					if (BORDER_SOLID.equals(borderstyle) || borderstyle==Context.STRING_INVALID) g.fillRect(x,y, w,bh);
		//					else if (BORDER_DOUBLE.equals(borderstyle)) { int dh=Math.max(bh/3,1); g.fillRect(x,y, w,dh); g.fillRect(x +bh,y+bh-dh, w -bh-bh,dh); }
		//					else if (BORDER_DASHED.equals(borderstyle)) { if (w>=10) for (int i=0,imax=w-5; i<imax; i+=10) g.fillRect(x+i,y, 5,bh); else g.fillRect(x,y, w,bh); }
		//					else if (BORDER_DOTTED.equals(borderstyle)) { if (w>=bh) for (int i=0,imax=w-bh; i<imax; i+=bh+bh) g.fillOval(x+i,y, bw,bw); else g.fillRect(x,y, w,bh); }
		//					else if (BORDER_GROOVE.equals(borderstyle)) { int dh=bh/2; g.setColor(Color.GRAY); g.fillRect(x,y, w,dh); g.setColor(Color.WHITE); g.fillRect(x +bh,y+dh, w -bh/*-bh*/,dh); }
		//				}
		//
		//				if ((bh=border.bottom)>0) {
		//					int ybot = y+h-bh;
		//					g.setColor(cborderbottom!=Context.COLOR_INHERIT? cborderbottom: foreground);
		//					if (BORDER_SOLID.equals(borderstyle) || borderstyle==Context.STRING_INVALID) g.fillRect(x,ybot, w,bh);
		//					else if (BORDER_DOUBLE.equals(borderstyle)) { int dh=Math.max(bh/3,1); g.fillRect(x,y+h-dh, w,dh); g.fillRect(x +bh,ybot, w -bh-bh,dh); }
		//					else if (BORDER_DASHED.equals(borderstyle)) { if (w>=10) for (int i=0,imax=w-5; i<imax; i+=10) g.fillRect(x+i,ybot, 5,bh); else g.fillRect(x,ybot, w,bh); }
		//					else if (BORDER_DOTTED.equals(borderstyle)) { if (w>=bh) for (int i=0,imax=w-bh; i<imax; i+=bh+bh) g.fillOval(x+i,ybot, bw,bw); else g.fillRect(x,ybot, w,bh); }
		//					else if (BORDER_GROOVE.equals(borderstyle)) { int dh=bh/2; g.setColor(Color.GRAY); g.fillRect(x +bh,ybot, w -bh-bh,dh); g.setColor(Color.WHITE); g.fillRect(x +bh,ybot+dh, w-bh,dh); }
		//				}
		//			}
		//
		////				g.setColor(foreground==Color.BLACK && background!=Color.LIGHT_GRAY? Color.LIGHT_GRAY: foreground);

		return ret;
	}

	@Override
	public boolean paintAfter(Context cx, INode node) {
		boolean ret = super.paintAfter(cx, node);
		return ret;
	}

}
