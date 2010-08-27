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

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is a template for the tag parsing
 */
public class FontDeclsTag extends GenStyleTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name  */
	public String getTagName() {
		return "office:font-face-decls";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler ac) {
		super.init(ac);
		if (FontDeclsTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			FontDeclsTag.startMethods = m[0];FontDeclsTag.contentMethods = m[1];FontDeclsTag.endMethods = m[2];
		}
	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return FontDeclsTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return FontDeclsTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return FontDeclsTag.startMethods;
	}
	/* CUT AND PASTE PART ENDS HERE*/

	public void s_style_font__face() {
		FontDeclaration fd = new FontDeclaration(getAttr());
		tsh.fontdecls.put(getAttr().getValue("style:name"), fd);
	}

	/* since it appears only at the start of the document, we can remove it at the end,
	 * saving some time (?)
	 */
	public void e_office_font__face__decls() {
		bc.removeAutomaticRedirection(getTagName());
		//System.out.println("---------");
	}


}
