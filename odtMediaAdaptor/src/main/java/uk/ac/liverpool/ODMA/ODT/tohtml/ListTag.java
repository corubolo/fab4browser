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
package uk.ac.liverpool.ODMA.ODT.tohtml;

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * ListTag
 */
public class ListTag extends TagAdapter {


	/* CUT AND PASTE PART STARTS HERE */
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public void init(BasicContentHandler cc) {
		super.init(cc);
		de = (ToHtmlContentHandler)cc;
		if (ListTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			ListTag.startMethods = m[0];
			ListTag.contentMethods = m[1];
			ListTag.endMethods = m[2];
		}

	}
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
	/* CUT AND PASTE PART ENDS HERE*/

	private int num=1;

	private String styleName;
	static int level = 0;
	ToHtmlContentHandler de;


	public void s_text_list() {
		ListTag.level++;
		styleName = de.getAttr().getValue("text:style-name");
	}
	public void e_text_list() {
		ListTag.level--;
	}


	public void s_text_list__item() {
		for (int i=1;i<ListTag.level;i++)
			de.outputBuffer.append("&nbsp;&nbsp;&nbsp;");
		de.outputBuffer.append(num+") ");
		num++;
		printa();
	}
	private void printa() {
		//System.out.println(level);
		Attributes a = de.getAttr();
		for (int i=0;i<a.getLength();i++)
			System.out.println(a.getQName(i) +" = "+ a.getValue(i));
	}
	/* (non-Javadoc)
	 * @see uk.ac.liverpool.ODMA.SCECallable#getTagName()
	 */
	public String getTagName() {
		return "text:list";
	}


}
