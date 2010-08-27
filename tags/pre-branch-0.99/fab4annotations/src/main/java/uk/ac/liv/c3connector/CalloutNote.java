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


package uk.ac.liv.c3connector;

import java.util.Map;

import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;
import phelps.lang.Integers;



/**
 * 
 * This is a simple extension to the FabNote class that creates callout notes , using a ArrowVFrame and storing the Robust Location (and position)
 * pointed by the arrow. The use of Robust Location make the arrow stick to the right poin independently of the file format and layout of the document.
 * 
 * 
 * @author fabio
 *
 */
public class CalloutNote extends FabNote {


	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		Document doc = getDocument();
		ArrowVFrame arro = new ArrowVFrame("Note",null, doc);

		//sel.g

		win_ = arro;
		super.restore(n,attr, layer);
		int px,py;
		px = Integers.parseInt(getAttr("px"),10);
		py = Integers.parseInt(getAttr("py"),10);
		arro.setPx(px);
		arro.setPy(py);

	}






	@Override
	public ESISNode save() {
		ArrowVFrame arro = (ArrowVFrame) win_;
		putAttr("px", String.valueOf(arro.getPx()));
		putAttr("py", String.valueOf(arro.getPy()));
		ESISNode e = super.save();  // after updating attrs
		return e;
	}
}
