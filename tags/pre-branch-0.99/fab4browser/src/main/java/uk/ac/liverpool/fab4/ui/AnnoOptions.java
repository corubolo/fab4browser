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
 * 
 * DEPRECATED class to express teh annotation options./ for the current ones look at the annotation extension classes.
 * 
 */
package uk.ac.liverpool.fab4.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AnnoOptions extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4841145318341268705L;
	private JButton jButton = null;
	private JPanel jPanel = null;
	private JLabel jLabel = null;
	private JTextField jTextField = null;
	private JLabel jLabel1 = null;
	private JTextField jTextField1 = null;

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Save");
		}
		return jButton;
	}

	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel1 = new JLabel();
			jLabel1.setText("User name:");
			jLabel = new JLabel();
			jLabel.setText("Annotation Server: ");
			FlowLayout flowLayout5 = new FlowLayout();
			flowLayout5.setAlignment(java.awt.FlowLayout.LEFT);
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout5);
			jPanel.add(jLabel1, null);
			jPanel.add(getJTextField1(), null);
			jPanel.add(getJTextField(), null);
			jPanel.add(jLabel, null);
		}
		return jPanel;
	}

	/**
	 * This method initializes jTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextField() {
		if (jTextField == null) {
			jTextField = new JTextField();
			jTextField.setText("http://bodoni.lib.liv.ac.uk:8080/Anno/");
		}
		return jTextField;
	}

	/**
	 * This method initializes jTextField1
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextField1() {
		if (jTextField1 == null) {
			jTextField1 = new JTextField();
			jTextField1.setText("User Name");
		}
		return jTextField1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * This is the default constructor
	 */
	public AnnoOptions() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		this.setSize(472, 266);
		this.add(getJPanel(), java.awt.BorderLayout.CENTER);
		this.add(getJButton(), java.awt.BorderLayout.SOUTH);
	}

} // @jve:decl-index=0:visual-constraint="-19,32"
