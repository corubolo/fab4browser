
/*
 * 
 * Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.liverpool.fab4.lens;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.Map;

import multivalent.Context;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Node;
import multivalent.node.LeafText;
import multivalent.std.lens.Lens;

/**
 * Simple class as an example, darkens all the area surrounding a keyhole shape. This is to demostrate how lenses / behavoiurs can
 * intervine outside of the box,
 * 
 * @author fabio
 *
 */
public class KeyHole extends Lens {

	Color fg = Color.BLACK, bg = Color.WHITE;
	/** Redraw underlying text in light colors. */
	@Override
	public boolean appearance(Context cx, boolean all) {

		return false;
	}
	@Override
	public boolean paintAfter(Context cx, Node node) {
		Graphics2D g = cx.g;
		g.setColor(new Color(0, 0, 0, 204));
		Rectangle clip = getContentBounds();
		Rectangle r = getBrowser().getRoot().getBbox();


		// base
		GeneralPath gp2 = new GeneralPath();
		gp2.moveTo(0.0f, 1.0f);
		gp2.lineTo(1.0f, 1.0f);
		gp2.lineTo(0.6f, 0.4f);
		gp2.lineTo(0.4f, 0.4f);
		gp2.closePath();
		Area aa = new Area(gp2);
		// circle
		Area bb = new Area(new Ellipse2D.Float(0.0f, 0.0f, 1.0f, 0.8f));
		// internal keyhole shape
		aa.add(bb);
		aa.transform(AffineTransform.getTranslateInstance(-aa.getBounds().x,
				-aa.getBounds().y));
		aa.transform(AffineTransform.getScaleInstance(clip.width, clip.height));
		// strand, have to bring it back
		aa.transform(AffineTransform.getTranslateInstance(clip.x, clip.y));
		// System.out.println(aa.getBounds());
		// inversion with bbox
		GeneralPath gp = new GeneralPath(Path2D.WIND_EVEN_ODD);
		gp.append(r, false);
		gp.append(aa, false);
		g.fill(gp);


		return false;
		// return super.paintAfter(cx, node);
	}


	public void paintRecurse(Context cx, Graphics2D g, Rectangle cliprect,
			Node n) {
		Rectangle bbox = n.bbox;

		if (cliprect.intersects(bbox))
			if (n.isStruct()) {
				int dx = n.dx(), dy = n.dy();
				g.translate(dx, dy);
				cliprect.translate(-dx, -dy);
				INode p = (INode) n;
				for (int i = 0, imax = p.size(); i < imax; i++)
					paintRecurse(cx, g, cliprect, p.childAt(i));
				g.translate(-dx, -dy);
				cliprect.translate(dx, dy);
			} else if (n instanceof LeafText) {
				g.setColor(Color.LIGHT_GRAY);

				int dx = n.dx(), dy = n.dy();
				g.translate(dx, dy);
				cx.g = g;

				n.paintNode(bbox, cx);
				g.translate(-dx, -dy);
			}
	}

	@Override
	public void restore(ESISNode arg0, Map<String, Object> arg1, Layer arg2) {
		super.restore(arg0, arg1, arg2);
		win_.setSize(150, 200);
	}
}
