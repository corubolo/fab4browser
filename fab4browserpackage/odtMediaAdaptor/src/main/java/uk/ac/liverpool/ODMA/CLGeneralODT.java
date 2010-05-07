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

import multivalent.CLGeneral;
import multivalent.Context;

/**
 * @author fabio
 *
 */
public class CLGeneralODT extends CLGeneral {





	@Override
	public boolean appearance(Context cx, boolean all) {
		super.appearance(cx, all);
		//		if (this.foreground_ == Color.RED) System.out.println("CSS General: fg = red");

		// ...

		if (all) {  // attributes that are not inherited
			//ContextODT cssx = (ContextODT)cx;
			/** NOW DO SOMETHING particular to the ContextODT! */
			//				if (borderstyle!=Context.STRING_INVALID) cssx.borderstyle = borderstyle;
			//
			//				if (cborderleft!=Context.COLOR_INVALID) cssx.cborderleft = cborderleft; //System.out.println("CSSGeneral borderleft = "+cborderleft); }
			//				if (cborderright!=Context.COLOR_INVALID) cssx.cborderright = cborderright;
			//				if (cbordertop!=Context.COLOR_INVALID) cssx.cbordertop = cbordertop;
			//				if (cborderbottom!=Context.COLOR_INVALID) cssx.cborderbottom = cborderbottom;
		}
		return false;
	}


}
