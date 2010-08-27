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
package uk.ac.liverpool.ODMA.ODT;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.ContextListener;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.Node;

import com.pt.awt.NFont;

/**
 * @author fabio
 *
 * This class tries to emulate the Span class from Multivalent,
 * but binds the span to a single node, since in
 * ODF files spans are always well formed.
 * 
 *
 *
 */
public class NodeSpan extends Behavior implements ContextListener {

	/**
	 * 
	 */
	public NodeSpan(Node n) {
		//n.addObserver(this);
		Leaf l1 = n.getFirstLeaf();
		Leaf l2 = n.getLastLeaf();
		l1.addSticky(new Mark(l1,0), true);
		l2.addSticky(new Mark(l2,l2.size()), true);
	}


	/* (non-Javadoc)
	 * @see multivalent.ContextListener#appearance(multivalent.Context, boolean)
	 */

	public boolean appearance(Context cx, boolean all) {
		cx.weight = NFont.WEIGHT_BOLD;
		System.out.println("a");
		return false;
	}

	/* (non-Javadoc)
	 * @see multivalent.ContextListener#getPriority()
	 */
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

}
