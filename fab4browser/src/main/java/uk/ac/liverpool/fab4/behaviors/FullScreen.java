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

package uk.ac.liverpool.fab4.behaviors;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.TimerTask;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Context;
import multivalent.Document;
import multivalent.INode;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.gui.VMenu;
import multivalent.node.Root;
import multivalent.std.SlideShowLinks;
import uk.ac.liverpool.fab4.Fab4;

import com.pt.awt.NFont;

/**
 * Full screen slide show for multipage documents. Almost get same effect by
 * enlarging window and zooming.
 * 
 * @see SlideShowLinks
 * @version $Revision$ $Date$
 */
public class FullScreen extends Behavior {
	static final boolean DEBUG = false;

	/**
	 * Take over full screen play slide show.
	 * <p>
	 * <tt>"slideShow"</tt>.
	 */
	public static final String MSG_START = "fullScreenStart";

	/**
	 * Stop full screen slide show, returning to normal window size.
	 * <p>
	 * <tt>"slideShow"</tt>.
	 */
	public static final String MSG_STOP = "fullScreenStop";

	static final int EDGE_MARGIN = 5;

	/**
	 * Toggle auto/manual advance.
	 * <p>
	 * <tt>"slideshowSetAuto"</tt>. public static final String MSG_SETAUTO =
	 * "slideshowSetAuto"; int interval_; boolean auto_ = true; // attribute?
	 * private boolean skip_ = false;
	 */
	GraphicsDevice gd_ = null;
	FrameFull full_ = null;
	Document doc_ = null;
	Rectangle bboxin_ = new Rectangle();
	Root root_ = null;
	TimerTask tt_ = null;

	class FrameFull extends Frame implements KeyListener, MouseListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Print p;
		FullScreen s;

		public FrameFull(FullScreen s) {
			setUndecorated(true);
			p = new Print();
			p.pageBorder = false;
			p.fromP = -1;
			p.toP = -1;
			p.z = 1f;
			p.curpage = 0;
			p.pageHeight = 0;
			p.br = getBrowser();
			p.doc = (Document) p.br.getRoot().findBFS("content");
			p.paginated = !(p.doc.getAttr(Document.ATTR_PAGE) == null);
			if (p.paginated) {
				p.curpage = p.doc.getAttr(Document.ATTR_PAGE) != null ? Integer
						.parseInt(p.doc.getAttr(Document.ATTR_PAGE)) - 1 : 0;
						p.numpages = p.doc.getAttr(Document.ATTR_PAGE) != null ? Integer
								.parseInt(p.doc.getAttr(Document.ATTR_PAGECOUNT)) - 1
								: 0;
			}
			this.s = s;
			addKeyListener(this);
			addMouseListener(this);
		}

		public void keyTyped(KeyEvent e) {
		}

		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_ENTER:
			case ' ':
			case '+':
			case 'n': {
				p.curpage++;
				if (p.curpage > p.numpages)
					s.stop();
				repaint();
				break;
			}
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_BACK_SPACE:
			case '-':
			case 'p': {
				if (p.curpage > 0)
					p.curpage--;
				repaint();
				break;
			}
			case KeyEvent.VK_HOME:
			case '1':
			case ',':
				p.curpage = 0;
				break;
			case KeyEvent.VK_END:
			case '.':
				p.curpage = p.numpages;
				break;
			case KeyEvent.VK_ESCAPE:
			case 'q':
				s.stop();
				break;
			default:

			}
		}

		@Override
		public void update(Graphics g) {
			paint(g);
		} // don't clear background (no need)

		// just paint relevant Document, not GUI or anything else
		@Override
		public void paint(Graphics g_old_api) {

			int w = full_.getWidth(), h = full_.getHeight();
			Rectangle docclip = new Rectangle(0, 0, w, h);
			Image i = p.paintToImage(docclip, p.curpage);
			g_old_api.drawImage(i, 0, 0, this);
			g_old_api.setFont(getFont().deriveFont(9));
			g_old_api.setColor(Color.RED);
			g_old_api.drawString("Press Esc key to exit", 12, h - 14);
		}

		public void mouseClicked(MouseEvent e) {

		}

		public void mousePressed(MouseEvent e) {

		}

		public void mouseReleased(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				p.curpage++;
				if (p.curpage > p.numpages)
					s.stop();
				repaint();
			}
			if (e.getButton() == MouseEvent.BUTTON3) {
				if (p.curpage > 0)
					p.curpage--;
				repaint();
			}

		}

		public void mouseEntered(MouseEvent e) {

		}

		public void mouseExited(MouseEvent e) {

		}

	}

	void start() {
		Browser br = getBrowser();
		Fab4 f = Fab4.getMVFrame(br);
		f.setEnabled(false);
		f.setVisible(false);
		GraphicsEnvironment ge = GraphicsEnvironment
		.getLocalGraphicsEnvironment();
		gd_ = ge.getDefaultScreenDevice();
		full_ = new FrameFull(this);
		gd_.setFullScreenWindow(full_);

	}

	// back to normal
	void stop() {
		if (!isActive())
			return;
		if (isActive()) {
			gd_.setFullScreenWindow(null);
			gd_ = null;
			full_.dispose();
			full_ = null;
		}
		Browser br = getBrowser();
		Fab4 f = Fab4.getMVFrame(br);
		NFont.setUseBitmaps(true);
		br.setBounds(f.getCurDoc().getBbox());
		f.setEnabled(true);
		f.setVisible(true);
		f.updateFormatted(br);
		br.requestFocus();
		br.repaint(1000L);
	}

	public boolean isActive() {
		return gd_ != null;
	}

	/**
	 * "Slide Show" in menu.
	 */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg))
			return true;
		if (VMenu.MSG_CREATE_GO == msg)
			createUI("button", "Full Screen Slide Show", "event " + FullScreen.MSG_START,
					(INode) se.getOut(), "GoPan", false);
		return false;
	}

	/** Start slide show, toggle auto, ... */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (FullScreen.MSG_START == msg) {
			start();
			return true;

		} else if (FullScreen.MSG_STOP == msg) {
			stop();
			return true;
		}

		return super.semanticEventAfter(se, msg);
	}

	// tree-based behaviors not called unless active
	@Override
	public boolean paintBefore(Context cx, Node node) {
		// full_.repaint(50); // repaint requests go to Browser not FrameFull --
		// delay to after
		// Browser's VolatileImage
		return true;
	}

	@Override
	public boolean paintAfter(Context cx, Node node) {
		return true;
	}

	/** Arrow keys, escape, home/end, mouse clicks. */
	// public void event(AWTEvent e) {
	@Override
	public boolean eventBefore(AWTEvent e, Point rel, Node n) { // not called
		// unless active
		return true;
	}

	@Override
	public boolean eventAfter(AWTEvent e, Point rel, Node n) {
		return true;
	}

}
