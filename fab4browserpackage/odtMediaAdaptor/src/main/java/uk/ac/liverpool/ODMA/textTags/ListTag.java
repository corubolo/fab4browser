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
package uk.ac.liverpool.ODMA.textTags;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.Nodes.ODTList;


/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * LISTS
 * 
 */
public class ListTag extends GenericContentTag {

	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name */
	public String getTagName() {
		return "text:list";
	}

	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler dc) {
		super.init(dc);
		if (ListTag.startMethods == null) {
			MethodName[][] m = initMethods(getClass(), true);
			ListTag.startMethods = m[0];
			ListTag.contentMethods = m[1];
			ListTag.endMethods = m[2];
		}
	}

	private static MethodName[] startMethods = null;

	private static MethodName[] contentMethods = null;

	private static MethodName[] endMethods = null;

	@Override
	public MethodName[] getContentMethods() {
		return ListTag.contentMethods;
	}

	@Override
	public MethodName[] getEndMethods() {
		return ListTag.endMethods;
	}

	@Override
	public MethodName[] getStartMethods() {
		return ListTag.startMethods;
	}

	/* CUT AND PASTE PART ENDS HERE */

	static int level = 0;
	int num = 0;
	private String styleName;

	ODTList curr;
	public void s_text_list() {
		ListTag.level++;
		styleName = getAttr().getValue("text:style-name");
		curr = new ODTList("Li",null,tch.parent, ListTag.level);
		tch.parent = curr;
	}
	public void e_text_list() {
		ListTag.level--;
		tch.parent = curr.getParentNode();
	}

}
