/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.liverpool.fab4.lens;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.TimerTask;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Context;
import multivalent.CursorMark;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.gui.VCheckbox;
import multivalent.gui.VFrame;
import multivalent.node.LeafText;
import multivalent.node.LeafZero;
import multivalent.std.ui.DocumentPopup;
import phelps.lang.Integers;

//import com.pt.awt.NFont;
/* Modifications include fixes to the key handling */

/**
 * Starting at cursor/selection, flash words up at some rate, after and superior
 * to <a href='http://www.halcyon.com/chigh/vortex.html'>Vortex</a>. Not sure if
 * this is a good way to do it, but it only took two hours to implement, and
 * makes a great example of Multivalent's advantages over that applet: don't
 * need three copies of text, can start at any point in document, works on any
 * page vs ones prepared with applet.
 * 
 * @version $Revision$ $Date$
 */
public class SpeedRead extends Behavior {
	/**
	 * Another semantic command, which should be given more descriptive name.
	 * <p>
	 * <tt>"togglePause"</tt>.
	 */
	public static final String MSG_PAUSE = "togglePause";

	/**
	 * Sets the delay between words, in milliseconds.
	 * <p>
	 * <tt>"speedreadDelay"</tt>: <tt>arg=</tt> {@link java.lang.Number}
	 * <var>delay</var>.
	 */
	public static final String MSG_DELAY = "speedreadDelay";

	/**
	 * Show words faster by decreasing the delay.
	 * <p>
	 * <tt>"speedreadFaster"</tt>.
	 */
	public static final String MSG_FASTER = "speedreadFaster";

	/**
	 * Another semantic command, which should be given more descriptive name.
	 * <p>
	 * <tt>"speedreadSlower"</tt>.
	 */
	public static final String MSG_SLOWER = "speedreadSlower";

	public static final String ATTR_DELAY = "delay";

	static final int DELAYINC = 10, IDELAY = 500; // 10ms

	static Color BACKGROUND = new Color(0xe0, 0xe0, 0xe0);
	static int ix_ = 100, iy_ = 100, iwidth_ = 300, iheight_ = 100;
	//	static NFont bigwordFont = NFont.getInstance("Times", NFont.WEIGHT_NORMAL,
	//			NFont.FLAG_SERIF, 24f);

	Node point_ = null;
	boolean stop_ = false;
	int delay_; // delay between words
	VFrame win_ = null;

	TimerTask tt = null;

	/** Entries in frame popup: pause, faster, slower, .... */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg))
			return true;
		else if (DocumentPopup.MSG_CREATE_DOCPOPUP == msg && se.getIn() == win_) {
			Browser br = getBrowser();
			INode menu = (INode) se.getOut();
			VCheckbox cb = (VCheckbox) createUI("checkbox",
					"Pause (also click in content)", new SemanticEvent(br,
							SpeedRead.MSG_PAUSE, win_, this, null), menu, "SPECIFIC",
							false);
			cb.setState(stop_);
			// createUI("button", "Delay "+delay_+"ms", null, menu, "EDIT",
			// true); => can't read number when disabled
			createUI("button", "Faster than " + delay_ + "ms  (also '+')",
					new SemanticEvent(br, SpeedRead.MSG_FASTER, win_, this, null), menu,
					"EDIT", delay_ <= SpeedRead.DELAYINC);
			createUI("button", "Slower (also '-')", new SemanticEvent(br,
					SpeedRead.MSG_SLOWER, win_, this, null), menu, "EDIT", false);
			return true;
		}
		return false;
	}

	/** Take action: windowClosed, pause, faster, .... */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (win_ != null && win_ == se.getArg()) {
			if (VFrame.MSG_CLOSED == msg)
				destroy();

			else if (VFrame.MSG_MOVED == msg || VFrame.MSG_RESIZED == msg) {
				Rectangle r = win_.bbox;
				SpeedRead.ix_ = r.x;
				SpeedRead.iy_ = r.y;
				SpeedRead.iwidth_ = r.width;
				SpeedRead.iheight_ = r.height;

			} else if (SpeedRead.MSG_PAUSE == msg)
				stop_ = !stop_;
			// on unpause, reset delay
			// if (!stop_) lastPing_ = System.currentTimeMillis() + IDELAY;
			else if (SpeedRead.MSG_SLOWER == msg) {
				delay_ += SpeedRead.DELAYINC;
				// System.out.println("-");
				tt.cancel();
				tt = new TimerTask() {
					@Override
					public void run() {
						next();
					}
				};
				getGlobal().getTimer().schedule(tt, delay_, delay_);
			}

			else if (SpeedRead.MSG_FASTER == msg) {
				if (delay_ - SpeedRead.DELAYINC > 0) {
					delay_ -= SpeedRead.DELAYINC;
					// System.out.println("+");
					tt.cancel();
					tt = new TimerTask() {
						@Override
						public void run() {
							next();
						}
					};
					getGlobal().getTimer().schedule(tt, delay_, delay_);
				}
			} else if (SpeedRead.MSG_DELAY == msg) {
			}
			// System.out.println("delay ="+delay_);
			// else if speed "EDIT"
		} else if (CursorMark.MSG_SET == msg)
			setStart();
		// lastPing_ += IDELAY;

		return super.semanticEventAfter(se, msg);
	}

	@Override
	public void destroy() {
		stop_ = true;
		tt.cancel();
		win_.deleteObserver(this);
		win_.remove();
		// getGlobal().getTimer().deleteObserver(this);
		getBrowser().repaint(100); // or leave for next repaint

		super.destroy();
	}

	@Override
	public boolean paintBefore(Context cx, Node node) {
		if (super.paintBefore(cx, node))
			return true;
		Graphics2D g = cx.g;
		Rectangle r = win_.bbox;
		g.setColor(SpeedRead.BACKGROUND/* Color.WHITE */); // g.fill(r); //g.fillRect(0,0,
		// bbox_.width,bbox_.height);
		g.fillRect(0, 0, r.width, r.height);
		return false;
	}

	/**
	 * Draw in frame, rather than change content node, format, paint cycle --
	 * probably wrong choice as doesn't compose.
	 */
	@Override
	public boolean paintAfter(Context cx, Node node) {
		// draw word, centered
		String txt = point_.getName();
		// System.out.println("grab = "+getBrowser().getGrab());
		if (txt != null && txt.length() > 0) {
			Graphics2D g = cx.g;
			Shape clipin = g.getClip();

			Rectangle r = win_.getContentBounds();
			r.translate(-win_.bbox.x, -win_.bbox.y);
			g.clip(r);

			// g.setColor(BACKGROUND/*Color.WHITE*/); g.fill(r);
			// //g.fillRect(0,0, bbox_.width,bbox_.height);

			//NFont f = bigwordFont;
			g.setColor(Color.BLACK);
			Font f = g.getFont();
			Rectangle2D stringBounds = f.getStringBounds(txt, g.getFontRenderContext());
			g.drawString(txt, (float) ((r.width - stringBounds.getX()) / 2), (float)((r.height -  stringBounds.getHeight()) / 2 + stringBounds.getHeight()));

			g.setClip(clipin);
		}

		return super.paintAfter(cx, node);
	}

	/** Click in content area to pause. */
	@Override
	public boolean eventBefore(AWTEvent e, Point rel, Node n) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			Rectangle r = win_.getContentBounds();
			if (rel != null && rel.y > r.y - win_.bbox.y)
				getBrowser().eventq(SpeedRead.MSG_PAUSE, win_);
			// return true; -- resize
		}
		return false;
	}

	/** Arrow keys adjust speed (actually delay). */
	@Override
	public boolean eventAfter(AWTEvent e, Point rel, Node n) {
		if (e.getID() == KeyEvent.KEY_RELEASED) {
			int key = ((KeyEvent) e).getKeyCode();
			Browser br = getBrowser();
			if (key == KeyEvent.VK_PLUS || key == KeyEvent.VK_ADD)
				br.eventq(SpeedRead.MSG_FASTER, win_);
			else if (key == KeyEvent.VK_MINUS || key == KeyEvent.VK_SUBTRACT)
				br.eventq(SpeedRead.MSG_SLOWER, win_);
		}
		return false;
	}

	/**
	 * Show next word at next Timer heartbeat. public void event(AWTEvent e) {
	 * if (e.getID()==TreeEvent.HEARTBEAT) { long ms =
	 * System.currentTimeMillis(); if (point_!=null && ms-lastPing_ > delay_ &&
	 * !stop_) { lastPing_=ms; } } }
	 */

	void next() {
		if (point_ != null && !stop_) {
			point_ = point_.getNextLeaf();
			while (point_ != null && !(point_ instanceof LeafText))
				point_ = point_.getNextLeaf();
			if (point_ == null)
				destroy()/* getBrowser().eventq(VFrame.MSG_CLOSE, this) */;
			else
				win_.getDocument()/*
				 * --compose with magnify, and rely on faster
				 * machines for faster readers
				 */.repaint(Math.min(delay_, 100));
		}
	}

	/** Set current point from cursor/selection/first word in document. */
	void setStart() {
		Browser br = getBrowser();
		CursorMark curs = br.getCursorMark();
		Span span = br.getSelectionSpan();

		if (curs.isSet()) {
			point_ = curs.getMark().leaf;
			curs.move(null, -1);
		} else if (br.getSelectionSpan().isSet()) {
			point_ = span.getStart().leaf;
			span.moveq(null); // remove();
		} else
			point_ = br.getCurDocument().getFirstLeaf();
	}

	/** Create VFrame. */
	@Override
	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);

		delay_ = Integers.parseInt(getAttr(SpeedRead.ATTR_DELAY), 280);

		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		// getGlobal().getTimer().addObserver(this);

		win_ = new VFrame("SpeedRead", null, doc);
		win_.setPinned(false);
		win_.setTitle("Speed Read");
		new LeafZero("TRANSPARENT", null, win_);
		win_.setBounds(Integers.parseInt(getAttr("x"), SpeedRead.ix_), Integers.parseInt(
				getAttr("y"), SpeedRead.iy_), Integers
				.parseInt(getAttr("width"), SpeedRead.iwidth_), Integers.parseInt(
						getAttr("height"), SpeedRead.iheight_));
		win_.addObserver(this);

		setStart();

		tt = new TimerTask() {
			@Override
			public void run() {
				next();
			}
		};
		getGlobal().getTimer().schedule(tt, SpeedRead.IDELAY, delay_);
	}
}
