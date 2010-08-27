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
package uk.ac.liverpool.ODMA.styleTags;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is the base for all style tags
 * 
 */
public abstract class GenStyleTag extends TagAdapter {

	TextStylesHandler tsh;
	// Map styles;

	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler ac) {
		super.init(ac);
		if (ac instanceof TextStylesHandler)
			tsh = (TextStylesHandler) ac;
		//       styles = tsh.styles;
		else System.out.println("ERROR in styles (genstyleTag)");
	}

}
