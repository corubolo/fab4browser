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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/** generic popup listener to show a popup on right button pressed */
public class PopupListener extends MouseAdapter {
	JPopupMenu pop;

	PopupListener(JPopupMenu pop) {
		this.pop = pop;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			pop.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			pop.show(e.getComponent(), e.getX(), e.getY());
	}
}