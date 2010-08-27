/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.liverpool.fab4.lens;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Map;

import multivalent.ESISNode;
import multivalent.Layer;
import multivalent.std.lens.Blur;
import multivalent.std.lens.LensOp;
import multivalent.std.lens.Sharpen;

/**
 * Edge detection lens, as in "Programmer�s Guide to the Java 2D� API".
 * 
 * @see Sharpen
 * @see Blur
 * @version $Revision$ $Date$
 */
public class Brighten extends LensOp {
	private static final ConvolveOp OP = new ConvolveOp(new Kernel(1, 1,
			new float[] { 1.2f / 1.0f }), ConvolveOp.EDGE_NO_OP, null); //1f/1.2f
	// rather
	// than
	// 0.8f
	// so
	// Lighten
	// +
	// Darken
	// =
	// unchanged

	@Override
	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);

		// take other kernels from attrs?

		op_ = Brighten.OP;
	}
}
