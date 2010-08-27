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

import phelps.util.Units;
import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.Nodes.LazyLeafImage;
import uk.ac.liverpool.ODMA.Nodes.ODTFrame;


/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * Common methods for paragraph text (P and H)
 * 
 */
public class FrameTag extends GenericContentTag {

	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name */
	public String getTagName() {
		return "draw:frame";
	}

	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler dc) {
		super.init(dc);
		if (FrameTag.startMethods == null) {
			MethodName[][] m = initMethods(getClass(), true);
			FrameTag.startMethods = m[0];
			FrameTag.contentMethods = m[1];
			FrameTag.endMethods = m[2];
		}
	}

	private static MethodName[] startMethods = null;

	private static MethodName[] contentMethods = null;

	private static MethodName[] endMethods = null;

	@Override
	public MethodName[] getContentMethods() {
		return FrameTag.contentMethods;
	}

	@Override
	public MethodName[] getEndMethods() {
		return FrameTag.endMethods;
	}

	@Override
	public MethodName[] getStartMethods() {
		return FrameTag.startMethods;
	}

	/* CUT AND PASTE PART ENDS HERE */

	int w=0, h=0, x, y;

	ODTFrame curr;

	public void s_draw_frame() {
		String t = getAttr().getValue("svg:width");
		if (t != null)
			w = (int) Units.getLength(t, "px");
		t = getAttr().getValue("svg:height");
		if (t != null)
			h = (int) Units.getLength(t, "px");

		curr = new ODTFrame("Fr", null, tch.parent);
		curr.bbox.setSize(w, h);
		//Style st =
		assignStyle(curr, "graphic");
		tch.parent = curr;
	}

	public void c_draw_frame() {

	}

	public void e_draw_frame() {
		tch.parent = curr.getParentNode();

	}

	public void s_draw_image() {
		String address = bc.getAttr().getValue("xlink:href");
		if (!address.startsWith("Pictures/"))
			return;
		try {
			LazyLeafImage leaf = new LazyLeafImage("I", null, tch.parent, tch.zipFile, address,true);
			//
			leaf.setSize(w, h);

		} catch (NullPointerException e) {
			System.out.println(address);
		}
	}



}
