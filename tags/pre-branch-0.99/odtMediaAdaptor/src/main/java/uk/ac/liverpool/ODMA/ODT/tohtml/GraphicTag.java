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

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk

 */
public class GraphicTag extends TagAdapter {


	/* CUT AND PASTE PART STARTS HERE */

	private ToHtmlContentHandler de;
	/* Change this to return the tag name  */
	public String getTagName() {
		return "draw:g";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler cc) {
		super.init(cc);
		de = (ToHtmlContentHandler)cc;
		if (GraphicTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			GraphicTag.startMethods = m[0];GraphicTag.contentMethods = m[1];GraphicTag.endMethods = m[2];
		}
	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return GraphicTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return GraphicTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return GraphicTag.startMethods;
	}

	/* CUT AND PASTE PART ENDS HERE*/

	public void s_draw_g() {
		if (de.cropInlines)
			de.outputContent = false;
		de.outputBuffer.append("<gr>");
	}
	/* (non-Javadoc)
	 * @see uk.ac.liverpool.ODMA.TagAdapter#isEatingAll()
	 */
	@Override
	public boolean isEatingAll() {
		return true;
	}
	public void e_draw_g() {
		if (de.cropInlines)
			de.outputContent = true;
		de.outputBuffer.append("</gr>");
	}




}
