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


package uk.ac.liv.c3connector;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.Document;
import multivalent.INode;
import multivalent.Node;
import multivalent.gui.VFrame;

/**
 * @author fabio
 * 
 * This is an extension of the VFrame class (Internal windows in Fab4) that adds a movable arrow, it's used by the Callout annotation.
 * 
 */

// TODO: add observer to destiantion nand fix protocol to reduce calls
public class ArrowVFrame extends VFrame {

	protected int px;

	protected int py;

	private boolean movePoint;

	private boolean onArrow;

	Node destination;

	public static Color aarrowColor = new Color(50,50,0);
	public Color arrowColor = ArrowVFrame.aarrowColor;

	private AWTEvent pe;
	public static Color barrowColor = new Color(210,180,0);

	/**
	 * @param name
	 * @param attr
	 * @param parent
	 */
	public ArrowVFrame(String name, Map<String, Object> attr, INode parent) {
		super(name, attr, parent);
		getBrowser().getRoot().findBFS("content");
	}

	/**
	 * @return the px
	 */
	public int getPx() {
		return px;
	}

	/**
	 * @return the py
	 */
	public int getPy() {
		return py;
	}

	/**
	 * @param py
	 *            the py to set
	 */
	public void setPy(int py) {
		this.py = py;
	}

	/**
	 * @param px
	 *            the px to set
	 */
	public void setPx(int px) {
		this.px = px;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.INode#paintBeforeAfter(java.awt.Rectangle,
	 *      multivalent.Context)
	 */
	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		super.paintBeforeAfter(docclip, cx);
		Color  cp = cx.g.getColor();
		if (destination != null)
			arrowColor=ArrowVFrame.barrowColor;
		else
			arrowColor=ArrowVFrame.aarrowColor;

		cx.g.setColor(arrowColor);
		int a,b;
		int px,py;
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		if (destination!=null) {
			Point pp = destination.getRelLocation(doc);
			px = pp.x + this.px;
			py = pp.y + this.py;
			//			int dx=destination.dx(), dy=destination.dy();
			//cx.g.translate(dx,dy);
			//			System.out.println(destination.bbox);
			//			System.out.println(dx+" "+dy);
			//			System.out.println(pp.x+" "+pp.y);
			if (movePoint)
				cx.g.drawRect(pp.x,pp.y,destination.getBbox().width,destination.getBbox().height);
			//cx.g.translate(-dx, -dy);

		} else {
			px = this.px;
			py = this.py;
		}
		int dx = bbox.x - px;
		int dy = bbox.y - py;
		if (px<bbox.x && dx>dy) {
			a = bbox.x;
			b = bbox.y + bbox.height / 2;
		} else if (py<bbox.y) {
			a = bbox.x + bbox.width/2;
			//if ()
			//	b = bbox.y;
			//else
			b = bbox.y;
		} else if (px>bbox.x+bbox.width) {
			a = bbox.x + bbox.width;
			b = bbox.y + bbox.height / 2;
		} else {
			a = bbox.x + bbox.width/2;
			b = bbox.y + bbox.height;

		}

		drawArrow(cx.g, a,b, px, py, 2.0f);
		cx.g.setColor(cp);

	}

	public static void drawArrow(Graphics2D g2d, int xCenter, int yCenter,
			int x, int y, float stroke) {
		double aDir = Math.atan2(xCenter - x, yCenter - y);
		Stroke s = g2d.getStroke();
		g2d.setStroke(new BasicStroke(stroke)); // make the arrow head solid even if


		g2d.drawLine(x, y, xCenter, yCenter);
		//g2d.setStroke(new BasicStroke(1f)); // make the arrow head solid even if
		// dash pattern has been specified
		Polygon tmpPoly = new Polygon();
		int i1 = 12 + (int) (stroke * 2);
		int i2 = 6 + (int) stroke; // make the arrow head the same size
		// regardless of the length length
		tmpPoly.addPoint(x, y); // arrow tip
		tmpPoly.addPoint(x + xCor(i1, aDir + .5), y + yCor(i1, aDir + .5));
		tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
		tmpPoly.addPoint(x + xCor(i1, aDir - .5), y + yCor(i1, aDir - .5));
		tmpPoly.addPoint(x, y); // arrow tip
		g2d.drawPolygon(tmpPoly);
		g2d.fillPolygon(tmpPoly); // remove this line to leave arrow head
		// unpainted
		g2d.setStroke(s);
	}

	private static int yCor(int len, double dir) {
		return (int) (len * Math.cos(dir));
	}

	private static int xCor(int len, double dir) {
		return (int) (len * Math.sin(dir));
	}

	@Override
	public void setLampshade(boolean lampshade) {
		if (this.lampshade != lampshade) {
			this.lampshade = lampshade;
			if (lampshade) {
				pbbox.setBounds(bbox);
				int titlew = (int) FONT_TITLE.stringAdvance(getTitle()).getX();
				int W;
				if (lampshade && shrinkTitle)
					W = titlew + 35;
				else
					W = bbox.width;
				//paintNode(, cx)();
				//repaint(bbox.x,bbox.y,bbox.width,bbox.height);
				getBrowser().repaint();//200, Math.min(bbox.x - 2, px), Math.min(bbox.y - 2,py),
				//Math.max(bbox.width + 2, px), Math.max(bbox.height + 2, py));
				bbox.setBounds(bbox.x, bbox.y, W, VFrame.titleh);

			} else {
				setSize(pbbox.width, pbbox.height);
				getBrowser().repaint();//200, Math.min(bbox.x - 2, px), Math.min(bbox.y - 2,py),
				//repaint(bbox.x,bbox.y,bbox.width,bbox.height);

			}
		}
	}




	@Override
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		if (pe == e)
			return false;
		pe = e;
		if (arrowEvent(e, rel))
			return true;
		return super.eventBeforeAfter(e, rel);
	}

	/**
	 * @param e
	 * @param rel
	 */
	boolean arrowEvent(AWTEvent e, Point rel) {
		int eid = e.getID();
		if (eid == MouseEvent.MOUSE_PRESSED && (((MouseEvent) e).getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			int ex = rel.x, ey = rel.y;
			int px1,py1;
			Document doc = (Document) getBrowser().getRoot().findBFS("content");
			if (destination!=null) {
				Point relLocation = destination.getRelLocation(doc);
				px1 = relLocation.x + px;
				py1 = relLocation.y + py;
			} else {
				px1 = px;
				py1 = py;
			}
			Browser br = getBrowser();

			if (ex > px1 - 10 && ex < px1 + 10 && ey > py1 - 10 && ey < py1 + 10) {
				startDrag(e, eid, br);
				return true;
			}

		}
		else if (eid== MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_EXITED || eid==MouseEvent.MOUSE_ENTERED) {
			int ex = rel.x, ey = rel.y;
			int px1,py1;
			Document doc = (Document) getBrowser().getRoot().findBFS("content");
			if (destination!=null) {
				Point relLocation = destination.getRelLocation(doc);
				px1 = relLocation.x + px;
				py1 = relLocation.y + py;
			} else {
				px1 = px;
				py1 = py;
			}
			boolean pr=onArrow;
			onArrow = ex > px1 - 12 && ex < px1 + 12 && ey > py1 - 12 && ey < py1 + 12;
			if (onArrow && !pr)
				getBrowser().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			else if (!onArrow && pr)
				getBrowser().setCursor(Cursor.getDefaultCursor());
			pr=onArrow;
		}
		return false;
	}

	/**
	 * @param e
	 * @param eid
	 * @param br
	 */
	private void startDrag(AWTEvent e, int eid, Browser br) {
		movePoint = true;
		if (MouseEvent.MOUSE_FIRST <= eid
				&& eid <= MouseEvent.MOUSE_LAST
				&& !((MouseEvent) e).isAltDown())
			raise();
		br.setCurNode(this, 0);
		br.setGrab(this);
	}



	/** Handle events while moving, resizing, .... */
	@Override
	public void event(AWTEvent e) {
		int eid = e.getID();
		Browser br = getBrowser();
		if (eid == MouseEvent.MOUSE_DRAGGED) {
			if (movePoint){
				Document doc = (Document) getBrowser().getRoot().findBFS("content");
				Point cpt = ((MouseEvent)e).getPoint();
				cpt.x+=doc.getHsb().getValue();
				cpt.y+=doc.getVsb().getValue();
				br.setCurNode(null);
				getRoot().eventBeforeAfter(e, br.getCurScrn());
				Node destn=br.getCurNode(); br.getCurOffset();
				int ry,rx;
				if (destn!=null && destn.isLeaf()) {
					Point rp = destn.getRelLocation(doc);
					ry = cpt.y -rp.y;//;//AbsLocation().y;
					rx = cpt.x - rp.x;//destn.getRelLocation(doc).x;//destn.getAbsLocation().x;
					destination = destn;
				} else {
					ry = cpt.y;
					rx = cpt.x;
					destination = null;
				}

				px =rx;
				py = ry;
				markDirty();
				br.repaint(100);
			}
		} else if (eid == MouseEvent.MOUSE_RELEASED) {
			movePoint = false;
			br.releaseGrab(this);
		}
		super.event(e);
	}

	public Node getDestination() {
		return destination;
	}

	public void setDestination(Node destination) {
		this.destination = destination;
	}
}
