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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.INode;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.gui.VFrame;

/**
 * 
 * The Fab4 extension to the VFrame (internal Frame). This class contanins improved appereance and many changes from the original.
 * 
 * @author fabio
 *
 */

public class Fab4VFrame extends VFrame {

	Node destination;

	private int px;

	private int py;

	public Fab4VFrame(String name, Map<String, Object> attr, INode parent) {
		super(name, attr, parent);
	}

	/** Content given by passed URL; pass null if want to build subtree yourself. */
	public Fab4VFrame(String name, Map<String, Object> attr, INode parent, URI docuri) {
		this(name, attr, parent);

		Browser br = getBrowser();
		if (br != null && docuri != null)
			try {
				Document cdoc = new Document("content"/* null? */, null, this);
				// setTitle("Test");
				DocInfo di = new DocInfo(docuri);
				di.doc = cdoc;
				br.event(new SemanticEvent(br, Document.MSG_OPEN, di));
				getDocument().repaint(100);
			} catch (Exception e) {
				System.err.println("can't create document");
				e.printStackTrace();
			}
	}

	/**
	 * Should be fast so can interactively move window -- in the past have minimized amount of redrawing, but at 500MHz Pentium and HotSpot, plenty fast enough to redraw entire document.
	 */
	@Override
	public void setLocation(int x, int y) {
		bbox.setLocation(Math.max(0, x), Math.max(0, y));
		markDirty();
		Browser br = getBrowser();
		if (br != null) {
			br.eventq(VFrame.MSG_MOVED, this);
			br.repaint(100);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.INode#paintBeforeAfter(java.awt.Rectangle, multivalent.Context)
	 */
	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		if (destination != null) {
			Point pp = destination.getRelLocation(doc);
			if (movelens_)
				cx.g.drawRect(pp.x, pp.y, destination.getBbox().width, destination.getBbox().height);
		}
		super.paintBeforeAfter(docclip, cx);
	}


	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		Browser br = getBrowser();
		int eid = e.getID();

		int ex = rel.x, ey = rel.y;


		if (eid == MouseEvent.MOUSE_PRESSED && (((MouseEvent) e).getModifiers() & InputEvent.BUTTON1_MASK) != 0) {

			// temporarily handle close box on mouse down
			if (ey < VFrame.titleh) {
				metagrab_ = true; // don't let BindingsDefault grab mouse
				// clicks!
				// int ex=-1;
				if (ex > bbox.width - 15 && closable)
					close();
				else if (ex > bbox.width - 26 && ex < bbox.width - 15)
					setLampshade(!isLampshade());
				// getBrowser().repaint(200, bbox.x - 2, bbox.y - 2,
				// bbox.width + 2, bbox.height + 2);
				else {
					movelens_ = true;
					// Double click on the title bar to shade
					long now = System.currentTimeMillis();
					if (now - pclick < 350) {
						setLampshade(!isLampshade());
						// getBrowser().repaint(200, bbox.x - 2, bbox.y - 2,
						// bbox.width + 2, bbox.height + 2);
						pclick = 1000;
					} else
						pclick = now;
				}

			} else if (lampshade)
				return true;

			// movelens_ = (ey<titleh);
			resizelens_ = resizable && ex > bbox.width - 10 && ey > bbox.height - 10;

			/*
			 * } else if (eid==MouseEvent.MOUSE_PRESSED && (((MouseEvent)e).getModifiers() & MouseEvent.BUTTON3_MASK) !=0) { System.out.println("b3"); br.eventq(new SemanticEvent(br,
			 * DocumentPopup.MSG_CREATE_DOCPOPUP, null, this, null)); return true;
			 */
		}
		// MouseEvent me = (MouseEvent)e;
		// System.out.println("button "+me.getModifiers()+" /
		// "+MouseEvent.BUTTON3_MASK);


		if (metagrab_ || movelens_ || resizelens_) {
			p0_ = br.getCurScrn();
			bbox0_.setBounds(bbox);
			if (movelens_ && MouseEvent.MOUSE_FIRST <= eid && eid <= MouseEvent.MOUSE_LAST && !((MouseEvent) e).isAltDown())
				raise(); // alt-click moves without raising to top

			br.setCurNode(this, 0); // so titlebar doesn't disappear when drag
			br.setGrab(this);

		} else
			super.eventNode(e, rel);

		return true; // no bleed through! (handles title bar here)
	}

	/** Handle events while moving, resizing, .... */
	@Override
	public void event(AWTEvent e) {
		int eid = e.getID();

		if (eid == MouseEvent.MOUSE_DRAGGED) {
			Browser br = getBrowser();
			Point cp = br.getCurScrn();

			if (p0_ != null && movelens_) {
				Point cpt = ((MouseEvent) e).getPoint();
				Document doc = (Document) getBrowser().getRoot().findBFS("content");
				cpt.x += doc.getHsb().getValue();
				cpt.y += doc.getVsb().getValue();
				br.setCurNode(null);
				getRoot().eventBeforeAfter(e, br.getCurScrn());
				Node destn = br.getCurNode();
				br.getCurOffset();
				int ry, rx;
				if (destn != null && destn.isLeaf()) {
					Point rp = destn.getRelLocation(doc);
					ry = cpt.y - rp.y;// ;//AbsLocation().y;
					rx = cpt.x - rp.x;// destn.getRelLocation(doc).x;//destn.getAbsLocation().x;
					// System.out.println("("+rx+":"+ry+")");
					destination = destn;
				} else {
					ry = cpt.y;
					rx = cpt.x;
					destination = null;
					px = rx;
					py = ry;
				}
			}
			if (p0_ != null) {
				int dx = cp.x - p0_.x, dy = cp.y - p0_.y;
				int adx = Math.abs(dx), ady = Math.abs(dy);
				if (/* adx>3*ady || ( */adx > 10 && ady <= 5/* ) */)
					dy = 0;
				else if (/* ady>3*adx || ( */adx <= 5 && ady > 10/* ) */)
					dx = 0;
				if (resizelens_)
					setSize(bbox0_.width + dx, bbox0_.height + dy);
				else if (movelens_)
					setLocation(bbox0_.x + dx, bbox0_.y + dy);
			}

		} else if (eid == MouseEvent.MOUSE_RELEASED) {
			Browser br = getBrowser();
			movelens_ = resizelens_ = false;
			br.releaseGrab(this);
			metagrab_ = false;
		}

	}


}
