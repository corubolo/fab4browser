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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import uk.ac.liverpool.fab4.behaviors.Bookmark;

/**
 * @author fabio
 * This class implements a simple UI for editing bookmarks.
 */
public class EditBookmarks extends JDialog {
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	JList jList = null;
	private JButton jButton = null;
	Bookmark bm;

	/**
	 * This method initializes jList
	 * 
	 * @return javax.swing.JList
	 */
	private JList getJList() {
		if (jList == null) {
			jList = new JList(Bookmark.bookmarks_);
			jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		return jList;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Delete Item");
			jButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (jList.getSelectedIndex() != -1) {
						int ind = jList.getSelectedIndex();
						// jList.(ind);
						// System.out.println("++"+Bookmark.bookmarks_.size());
						Bookmark.bookmarks_.removeElementAt(ind);
						for (Fab4 fra : Fab4.fr)
							fra.mbookmarks.remove(ind + 4);
						jList.repaint();
						try {
							bm.write();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}

				}
			});
		}
		return jButton;
	}

	/**
	 * This is the default constructor
	 */
	public EditBookmarks(Bookmark bm) {
		super();
		this.bm = bm;
		initialize();
	}

	/**
	 * This method initializes this
	 */
	private void initialize() {
		this.setSize(443, 290);
		setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJButton(), java.awt.BorderLayout.SOUTH);
			jContentPane.add(getJList(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

} // @jve:decl-index=0:visual-constraint="79,25"
