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

package uk.ac.liv.c3connector.ui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import uk.ac.liv.c3connector.TrustedIdentity;

/**
 * 
 * The cell list renderer for the trusted identity list.
 * This class is used by the Preferences dialog to render the trusted identity list.
 *
 * @author fabio
 *
 */
public class TILrender extends JLabel implements ListCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		TrustedIdentity ti = (TrustedIdentity) value;
		setText(ti.getName());

		if (isSelected) {
			setBackground(Color.BLUE);
			setForeground(Color.WHITE);
			setOpaque(true);
		} else {
			setOpaque(false);
			setForeground(Color.BLACK);
		}
		return this;
	}

}