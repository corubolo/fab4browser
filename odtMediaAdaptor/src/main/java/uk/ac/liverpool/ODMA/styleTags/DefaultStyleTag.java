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

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is for proper styles (common)
 * 
 */
public class DefaultStyleTag extends StyleTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name  */
	@Override
	public String getTagName() {
		return "style:default-style";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler ac) {
		super.init(ac);
		if (DefaultStyleTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			DefaultStyleTag.startMethods = m[0];DefaultStyleTag.contentMethods = m[1];DefaultStyleTag.endMethods = m[2];
		}
	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return DefaultStyleTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return DefaultStyleTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return DefaultStyleTag.startMethods;
	}
	/* CUT AND PASTE PART ENDS HERE*/


	public void s_style_default__style() {
		Attributes att = bc.getAttr();
		printAttr(att);
		s.type = Style.DEFAULT;
		s.family= att.getValue("style:family");
	}

	public void e_style_default__style() {
		if (TagAdapter.prints)
			System.out.println("---------");
		tsh.styles.put(s.getId(), s);
	}


}
