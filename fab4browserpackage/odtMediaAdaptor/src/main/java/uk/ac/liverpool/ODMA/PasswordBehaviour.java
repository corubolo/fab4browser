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
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.ODMA;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import multivalent.Behavior;
import multivalent.SemanticEvent;

/**
 * @author fabio
 *
 */
public class PasswordBehaviour extends Behavior {

	public static final String MSG_ASK_PASS="otd_ask_pass";
	public static final String MSG_RET_PASS="otd_ret_pass";


	/* (non-Javadoc)
	 * @see multivalent.Behavior#semanticEventBefore(multivalent.SemanticEvent, java.lang.String)
	 */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {

		if (msg==PasswordBehaviour.MSG_ASK_PASS)
			new PassDialog(this).init();


		return super.semanticEventBefore(se, msg);
	}

	class PassDialog extends JDialog implements ActionListener, KeyListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * 
		 */
		PasswordBehaviour pb;
		JPasswordField fi;
		public PassDialog(final PasswordBehaviour pb) {
			super();
			this.pb = pb;

		}

		/**
		 * 
		 */
		public void init() {
			JLabel l = new JLabel("The document is encrypted.\n Please enter the password:");
			JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
			fi = new JPasswordField(10);
			fi.addKeyListener(this);
			p2.add(fi);
			JButton b = new JButton("OK");
			JButton b2 = new JButton("Cancel");
			b2.setActionCommand("Cancel");
			JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
			p.add(b);
			p.add(b2);
			fi.setEchoChar('*');
			getContentPane().add(l,BorderLayout.NORTH);
			getContentPane().add(p2,BorderLayout.CENTER);
			getContentPane().add(p,BorderLayout.SOUTH);
			b.addActionListener(this);
			b2.addActionListener(this);
			pack();
			int x = (int)pb.getBrowser().getLocationOnScreen().getX() + 50;
			int y = (int)pb.getBrowser().getLocationOnScreen().getY() + 70;
			this.setLocation(x,y);
			setVisible(true);

		}

		public void actionPerformed(ActionEvent e) {
			String pass = new String(fi.getPassword());
			try {
				if (e!=null && e.getActionCommand().equals("Cancel")) {

				} else {
					MessageDigest md;
					md = MessageDigest.getInstance("SHA1");
					md.update(pass.getBytes());
					pb.getBrowser().eventq(PasswordBehaviour.MSG_RET_PASS,md.digest());
				}
			} catch (NoSuchAlgorithmException e2) {
				e2.printStackTrace();
			}
			finally {
				setVisible(false);
				fi = null;
				pass = null;
				System.gc();
			}


		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		public void keyTyped(KeyEvent e) {
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
				actionPerformed(null);

		}
	}


}
