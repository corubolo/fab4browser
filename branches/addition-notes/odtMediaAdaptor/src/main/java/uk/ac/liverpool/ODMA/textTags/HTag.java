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

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is a template for the tag parsing
 */
public class HTag extends ParagraphTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name */
	public String getTagName() {
		return "text:h";
	}

	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler dc) {
		super.init(dc);
		if (HTag.startMethods == null) {
			MethodName[][] m = initMethods(getClass(), true);
			HTag.startMethods = m[0];
			HTag.contentMethods = m[1];
			HTag.endMethods = m[2];
		}
	}

	private static MethodName[] startMethods = null;

	private static MethodName[] contentMethods = null;

	private static MethodName[] endMethods = null;

	@Override
	public MethodName[] getContentMethods() {
		return HTag.contentMethods;
	}

	@Override
	public MethodName[] getEndMethods() {
		return HTag.endMethods;
	}

	@Override
	public MethodName[] getStartMethods() {
		return HTag.startMethods;
	}

	/* CUT AND PASTE PART ENDS HERE */


	static int[] numbers = new int[20];

	int level = 0;


	@Override
	public boolean isEatingAll() {
		return false;
	}

	public void s_text_h() {
		assignStyle(para,"paragraph");
		Attributes a = getAttr();
		String t =  a.getValue("text:outline-level");
		if (t!=null)
			level = Integer.parseInt(t);
		else
			level = st.defaultOutlineLevel;
		HTag.numbers[level]=HTag.numbers[level]+1;
		for (int i=level+1;i<HTag.numbers.length;i++)
			HTag.numbers[i]=0;
		t =  a.getValue("text:restart-numbering");
		if (t!=null && t.equals("true"))
			HTag.numbers[level]=0;
		t =  a.getValue("text:start-value");
		if (t!=null)
			HTag.numbers[level] = Integer.parseInt(t);
		t =  a.getValue("text:is-list-header");
		if (t!=null && t.equals("true"))
			return;
		StringBuilder sb = new StringBuilder(9);
		if (level>0) {
			for (int i=1;i<=level;i++) {
				sb.append(HTag.numbers[i]);
				sb.append('.');
			}
			sb.setLength(sb.length()-1);
			t = sb.toString();
		}
		else
			t="h";
		para = new IParaBox(t, null, tch.parent);
		tch.parent = para;
		//ll = new LeafUnicode(t, null, para);
		//FIXME> temporary, has to be done with style *list stile or outline/style(default)


	}


	public void c_text_h() {
		contentToLeaves();
	}

	public void e_text_h() {
		tch.parent = para.getParentNode();
	}



}
