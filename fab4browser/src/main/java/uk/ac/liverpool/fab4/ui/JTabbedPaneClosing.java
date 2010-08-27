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

package uk.ac.liverpool.fab4.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JTabbedPane;


/**
 * A JTabbnedPane that add feature to close a specific tab with an x button (a la Firefox).
 * As a bonus, adds the capablity of usng the scroll wheel to move across tabs.
 * @author fabio
 *
 */
public class JTabbedPaneClosing extends JTabbedPane implements MouseListener,
MouseWheelListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private static BufferedImage im = null;

	private static BufferedImage im2 = null;

	private static Image sim = null;

	private static Image sim2 = null;

	private List<TabCloseListener> closeListeners = null;

	private int in = -1;
	boolean veryIn = false, redAlert = false;

	private static int dx = 3, dy = 4, dw = 10;

	public JTabbedPaneClosing() {
		super();
		listen();
	}

	public JTabbedPaneClosing(int tabPlacement, int tabLayoutPolicy) {
		super(tabPlacement, tabLayoutPolicy);
		listen();
	}

	public JTabbedPaneClosing(int tabPlacement) {
		super(tabPlacement);
		listen();
	}

	private void listen() {
		try {
			JTabbedPaneClosing.im = ImageIO.read(getClass().getResource("/res/close1.png"));
		} catch (Exception e) {
		}
		try {
			JTabbedPaneClosing.im2 = ImageIO.read(getClass().getResource("/res/close2.png"));
		} catch (Exception e) {
		}
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
	}

	@Override
	public void setTitleAt(int index, String title) {
		super.setTitleAt(index, title + " ");
	}

	@Override
	public Component add(String title, Component component) {
		return super.add(title + " ", component);
	}

	@Override
	public void addTab(String title, Component component) {
		super.addTab(title + " ", component);
	}

	@Override
	public void addTab(String title, Icon icon, Component component, String tip) {
		super.addTab(title + " ", icon, component, tip);

	}

	@Override
	public void addTab(String title, Icon icon, Component component) {
		super.addTab(title + " ", icon, component);
	}

	@Override
	public int indexOfTab(String title) {
		return super.indexOfTab(title + " ");
	}

	public void addCloseListener(TabCloseListener l) {
		if (closeListeners == null) {
			closeListeners = new Vector<TabCloseListener>(1);
			closeListeners.add(l);
		}

	}

	public void removeCloseListener(TabCloseListener l) {
		if (closeListeners != null)
			closeListeners.remove(l);

	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintIcon(g, in);
		paintIcon(g, getSelectedIndex());

	}

	/**
	 * @param g
	 * @param i
	 */
	private void paintIcon(Graphics g, int i) {
		Rectangle r;
		if (i == -1 || i >= getTabCount())
			return;
		r = getBoundsAt(i);
		int dh = r.height + 1 - 2 * JTabbedPaneClosing.dy;
		if (JTabbedPaneClosing.sim == null && !(JTabbedPaneClosing.im == null)) {
			JTabbedPaneClosing.sim = JTabbedPaneClosing.im.getScaledInstance(JTabbedPaneClosing.dw - 1, dh - 1, Image.SCALE_SMOOTH);
			JTabbedPaneClosing.sim2 = JTabbedPaneClosing.im2.getScaledInstance(JTabbedPaneClosing.dw - 1, dh - 1, Image.SCALE_SMOOTH);
			JTabbedPaneClosing.im = null;
			JTabbedPaneClosing.im2 = null;
		}

		r.setBounds(r.x + r.width - JTabbedPaneClosing.dw - JTabbedPaneClosing.dx, r.y + JTabbedPaneClosing.dy, JTabbedPaneClosing.dw, dh);
		if (JTabbedPaneClosing.sim != null) {
			if (veryIn && i == in)
				g.drawImage(JTabbedPaneClosing.sim2, r.x + 1, r.y + 1, this);// , r.width - 1,
			// r.height - 1,
			// this);
			else
				g.drawImage(JTabbedPaneClosing.sim, r.x + 1, r.y + 1, this);// r.width - 1,
			// r.height - 1,
			// this);
		} else {
			g.drawLine(r.x + 1, r.y + 1, r.x + r.width - 1, r.y + r.height - 1);
			g.drawLine(r.x + 1, r.y + 1, r.x + r.width - 1, r.y + r.height - 1);
		}
		// if (i == in && veryIn) {
		if (redAlert && veryIn && i == in) {
			Color cp = g.getColor();
			g.setColor(Color.GRAY);
			g.draw3DRect(r.x, r.y, r.width - 1, r.height - 1, false);
			g.setColor(cp);
		} else {
			Color cp = g.getColor();
			g.setColor(Color.GRAY);
			g.draw3DRect(r.x, r.y, r.width - 1, r.height - 1, true);
			g.setColor(cp);
		}
	}

	public void mouseClicked(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		if (veryIn) {
			redAlert = true;
			repaint();
		}
	}

	public void mouseReleased(MouseEvent e) {
		Rectangle r;
		if (e.getButton() == MouseEvent.BUTTON1) {
			// for (int i = 0; i < getTabCount(); i++) {
			int i = indexAtLocation(e.getX(), e.getY());
			if (i != -1 && redAlert && i == in) {
				r = getBoundsAt(i);
				int dh = r.height + 1 - 2 * JTabbedPaneClosing.dy;
				r.setBounds(r.x + r.width - JTabbedPaneClosing.dw - JTabbedPaneClosing.dx, r.y + JTabbedPaneClosing.dy, JTabbedPaneClosing.dw, dh);
				if (r.contains(e.getPoint())) {
					CloseEvent ev = new CloseEvent();
					ev.closeIndex = i;
					for (TabCloseListener l : closeListeners)
						l.tabClosing(ev);
					if (ev.isClosing)
						// e.consume();
						removeTabAt(i);
				}
			}
		}
		if (redAlert) {
			redAlert = false;
			repaint();
		}
	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {
		if (in != -1 && in < getTabCount()) {
			Rectangle r = getBoundsAt(in);
			int dh = r.height + 1 - 2 * JTabbedPaneClosing.dy;
			r.setBounds(r.x + r.width - JTabbedPaneClosing.dw - JTabbedPaneClosing.dx, r.y + JTabbedPaneClosing.dy, JTabbedPaneClosing.dw, dh);
			repaint(r);
			in = -1;
		}

	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		int i = (getSelectedIndex() + e.getWheelRotation())
		% getComponentCount();
		if (i == -1)
			i = getComponentCount() - 1;
		setSelectedIndex(i);
	}

	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	public void mouseMoved(MouseEvent e) {
		Rectangle r;
		int prin = in;
		boolean prVeryIn = veryIn;
		veryIn = false;
		in = indexAtLocation(e.getX(), e.getY());
		if (in != -1) {
			r = getBoundsAt(in);
			if (r.contains(e.getPoint())) {
				int dh = r.height + 1 - 2 * JTabbedPaneClosing.dy;
				r.setBounds(r.x + r.width - JTabbedPaneClosing.dw - JTabbedPaneClosing.dx, r.y + JTabbedPaneClosing.dy, JTabbedPaneClosing.dw, dh);
				if (r.contains(e.getPoint()))
					veryIn = true;
				if (in != prin && in != getSelectedIndex()
						|| veryIn != prVeryIn)
					repaint(r);
			}
		}
		if (prin != -1 && prin < getTabCount() && prin != in) {
			r = getBoundsAt(prin);
			int dh = r.height + 1 - 2 * JTabbedPaneClosing.dy;
			r.setBounds(r.x + r.width - JTabbedPaneClosing.dw - JTabbedPaneClosing.dx, r.y + JTabbedPaneClosing.dy, JTabbedPaneClosing.dw, dh);
			repaint(r);
		}

	}

}
