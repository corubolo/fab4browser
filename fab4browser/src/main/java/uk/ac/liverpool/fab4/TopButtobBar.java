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

package uk.ac.liverpool.fab4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import multivalent.Document;
import uk.ac.liverpool.fab4.behaviors.ForwardBack4;

/**
 * 
 * This is the top address / button bar in Fab4, including forward, back etc.
 * 
 * @author fabio
 *
 */
public class TopButtobBar extends JPanel{

	private static final long serialVersionUID = 1L;

	JPopupMenu bkpopup;

	JPopupMenu fwpopup;

	JButton urlbutton;

	JButton bk, fw;

	private Fab4 fab;

	JTextField address = null;

	JButton stopButton;

	public TopButtobBar(Fab4 ff) {
		super(new BorderLayout(4, 0));

		JToolBar tb = new JToolBar();
		fab = ff;
		setBorder(new EmptyBorder(4, 4, 2, 4));

		JButton tm;
		bk = ff.getButton("bk");
		ff.setAction(bk, "back");
		tb.add(bk);
		bkpopup = new JPopupMenu();
		final JButton tm1 = ff.getButton("down");
		tb.add(tm1);
		tm1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buildHisPopup(false);
				bkpopup.show(tm1, 0, tm1.getHeight());
			}
		});
		// Popup with the list

		bk.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					buildHisPopup(false);
					if (bkpopup.getComponentCount() > 0)
						bkpopup.show(e.getComponent(), 0, e.getComponent()
								.getHeight());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mousePressed(e);
			}
		});
		fw = ff.getButton("fw");
		ff.setAction(fw, "forward");
		tb.add(fw);
		final JButton tm2 = ff.getButton("down");
		tb.add(tm2);
		fwpopup = new JPopupMenu();
		tm2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buildHisPopup(true);
				bkpopup.show(tm2, 0, tm2.getHeight());
			}
		});
		// Popup with the list

		fwpopup.pack();
		fw.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					buildHisPopup(true);
					if (fwpopup.getComponentCount() > 0)
						fwpopup.show(e.getComponent(), 0, e.getComponent()
								.getHeight());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mousePressed(e);
			}
		});

		tm = ff.getButton("nt", "Opens a new tab");
		ff.setAction(tm, "openNewTab");

		tb.add(tm);

		tm = ff.getButton("rl", "Reload");
		ff.setAction(tm, "reload");
		tb.add(tm);
		stopButton = tm = ff.getButton("st", "Stop");
		ff.setActionS(tm, Document.MSG_STOP);
		tb.add(tm);
		tm = ff.getButton("hm", "Go home");
		ff.setAction(tm, "goHome");

		tb.add(tm);
		tb.setFloatable(false);
		tb.setRollover(true);
		add(tb, BorderLayout.WEST);

		add(getAddress(), BorderLayout.CENTER);
		tm = ff.getButton("Go");
		ff.setAction(tm, "go");
		add(tm, BorderLayout.EAST);
		doLayout();

	}

	/** This method initializes the address bar where you can type in */
	JPanel getAddress() {
		address = new JTextField();
		address.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
		address.setMargin(new java.awt.Insets(1, 8, 1, 2));
		address.setMinimumSize(new java.awt.Dimension(100, 23));
		address.setPreferredSize(new java.awt.Dimension(100, 23));
		address.setMaximumSize(new java.awt.Dimension(1000, 23));
		address.setColumns(252);
		address.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 1,
				1, java.awt.Color.DARK_GRAY));
		address.setFont(address.getFont().deriveFont(13.0f));
		address.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getButton() == java.awt.event.MouseEvent.BUTTON1
						&& e.getClickCount() == 2) {
					address.selectAll();
					e.consume();
				}
			}
		});
		address.addKeyListener(new KeyListener() {
			public void keyPressed(java.awt.event.KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					fab.go();

			}

			public void keyTyped(java.awt.event.KeyEvent e) {
			}

		});
		JPanel ap = new JPanel(new BorderLayout());
		urlbutton = fab.getButton("url");
		urlbutton.setBorderPainted(true);
		urlbutton.setBackground(Color.WHITE);
		urlbutton.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1,
				1, 0, java.awt.Color.DARK_GRAY));
		urlbutton.setMinimumSize(new java.awt.Dimension(27, 18));
		urlbutton.setPreferredSize(new java.awt.Dimension(27, 18));
		urlbutton.setMargin(new java.awt.Insets(1, 8, 1, 3));
		urlbutton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
					address.selectAll();
					address.requestFocusInWindow();
					e.consume();

				}
			}
		});
		ap.add(urlbutton, BorderLayout.WEST);
		ap.add(address, BorderLayout.CENTER);
		return ap;
	}

	/** builds the History pop up */
	void buildHisPopup(boolean forward) {
		ForwardBack4 fb4 = (ForwardBack4) fab.getBe("fwbk");
		JMenuItem mi;
		if (forward) {
			fwpopup.removeAll();
			for (int i = fb4.pagesi_ + 1; i < fb4.pages_.size(); i++) {
				mi = new JMenuItem(fb4.pages_.get(i).title);
				mi.setActionCommand(fb4.pages_.get(i).uri.toString());
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						fab.open(e.getActionCommand());
					}
				});
				fwpopup.add(mi);
			}
			fwpopup.pack();
		} else {
			bkpopup.removeAll();
			for (int i = fb4.pagesi_ - 1; i >= 0; i--) {
				mi = new JMenuItem(fb4.pages_.get(i).title);
				mi.setActionCommand(fb4.pages_.get(i).uri.toString());
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						fab.open(e.getActionCommand());
					}
				});
				bkpopup.add(mi);
			}
			bkpopup.pack();
		}
	}

}
