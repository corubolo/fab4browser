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

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import uk.ac.liverpool.fab4.Fab4;

/**
 * Panel to choose the Look an feel of the program
 *
 */
public class LookAndFeelPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private void init() {
		setLayout(new FlowLayout(FlowLayout.CENTER, 2, 3));
		UIManager.installLookAndFeel("Liquid",
		"com.birosoft.liquid.LiquidLookAndFeel");
		LookAndFeelInfo[] lif = UIManager.getInstalledLookAndFeels();
		final String[] cnames = new String[lif.length];
		String[] vnames = new String[lif.length];

		String s = UIManager.getLookAndFeel().getClass().getCanonicalName();
		String t;
		int sel = -1;
		for (int i = 0; i < lif.length; i++) {
			t = lif[i].getClassName();
			cnames[i] = t;
			vnames[i] = lif[i].getName();
			if (s.equals(t))
				sel = i;
		}
		if (sel == -1)
			sel = lif.length;
		final JComboBox jcb = new JComboBox(vnames);
		jcb.setSelectedIndex(sel);
		JButton button = new JButton("Apply");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					UIManager.setLookAndFeel(cnames[jcb.getSelectedIndex()]);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		JButton b2 = new JButton("Save");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Fab4.mv.putPreference("LookAndFeel", (String) jcb
						.getSelectedItem());
				// System.out.println(Fab4.mv.getPreference("lookandfeel",
				// "nulllla"));
				try {
					UIManager.setLookAndFeel((String) jcb.getSelectedItem());
				} catch (Exception e1) {
					e1.printStackTrace();
					return;
				}
			}
		});
		add(jcb);
		add(button);
		add(b2);

		// add()
	}

	public LookAndFeelPanel() {
		super();
		init();
	}

	/**
	 * 
	 */

	public LookAndFeelPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		init();
	}

	public LookAndFeelPanel(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		init();
	}

	public LookAndFeelPanel(LayoutManager layout) {
		super(layout);
		init();
	}

}
