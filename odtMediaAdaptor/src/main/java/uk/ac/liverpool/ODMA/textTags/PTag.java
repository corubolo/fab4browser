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

import multivalent.node.IParaBox;
import multivalent.node.LeafUnicode;
import uk.ac.liverpool.ODMA.BasicContentHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is a template for the tag parsing
 */
public class PTag extends ParagraphTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name */
	public String getTagName() {
		return "text:p";
	}

	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler dc) {
		super.init(dc);
		if (PTag.startMethods == null) {
			MethodName[][] m = initMethods(getClass(), true);
			PTag.startMethods = m[0];
			PTag.contentMethods = m[1];
			PTag.endMethods = m[2];
		}
	}

	private static MethodName[] startMethods = null;

	private static MethodName[] contentMethods = null;

	private static MethodName[] endMethods = null;

	@Override
	public MethodName[] getContentMethods() {
		return PTag.contentMethods;
	}

	@Override
	public MethodName[] getEndMethods() {
		return PTag.endMethods;
	}

	@Override
	public MethodName[] getStartMethods() {
		return PTag.startMethods;
	}

	/* CUT AND PASTE PART ENDS HERE */


	@Override
	public boolean isEatingAll() {
		return false;
	}

	public void s_text_p() {
		para = new IParaBox("P", null, tch.parent);
		tch.parent = para;
		assignStyle(para,"paragraph");

	}


	public void c_text_p() {
		contentToLeaves();
	}

	public void e_text_p() {
		if (para.size() == 0)
			new LeafUnicode(" ", null, para);
		tch.parent = para.getParentNode();

	}



}
