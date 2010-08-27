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

import java.util.HashMap;

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is for automatic and proper styles, since the distinction between the two
 * lies only on the container. the type is determined by tsh.styleGroup
 * 
 * 
 */
public class StyleTag extends GenStyleTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name  */
	public String getTagName() {
		return "style:style";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler ac) {
		super.init(ac);
		if (StyleTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			StyleTag.startMethods = m[0];StyleTag.contentMethods = m[1];StyleTag.endMethods = m[2];
		}

		s = new Style(tsh.fontdecls);

	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return StyleTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return StyleTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return StyleTag.startMethods;
	}
	/* CUT AND PASTE PART ENDS HERE*/
	Style s;


	@Override
	public boolean isEatingAll() {
		return true;
	}
	public void s_style_style() {
		s = new Style(tsh.fontdecls);
		Attributes att = bc.getAttr();
		//prints=true;
		printAttr(att);
		s.name = att.getValue("style:name");
		s.type = tsh.styleGroup;
		s.family= att.getValue("style:family");
		s.displayName= att.getValue("style:display-name");
		s.parent = att.getValue("style:parent-style-name");
		s.nextStyleName = att.getValue("style:next-style-name");
		s.listStyleName= att.getValue("style:list-style-name");
		s.masterPageName = att.getValue("style:master-page-name");
		String at = att.getValue("style:default-outline-level");
		if (at!=null)
			s.defaultOutlineLevel=Integer.parseInt(at);

	}

	public void s_style_paragraph__properties() {
		s.paragraphProperties = new HashMap<String, String>();
		printAttr(bc.getAttr());
		bc.copyAttributesTo(s.paragraphProperties);

	}
	public void s_style_text__properties() {
		s.textProperties = new HashMap<String, String>();
		printAttr(bc.getAttr());
		bc.copyAttributesTo(s.textProperties);
	}

	public void s_style_graphic__properties(){
		s.graphicProperties = new HashMap<String, String>();
		printAttr(bc.getAttr());
		bc.copyAttributesTo(s.graphicProperties);
	}


	public void e_style_style() {
		if (TagAdapter.prints)
			System.out.println("---------");
		tsh.styles.put(s.getId(), s);
	}


}
