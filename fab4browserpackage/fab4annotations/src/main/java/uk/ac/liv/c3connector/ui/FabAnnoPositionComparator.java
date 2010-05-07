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


package uk.ac.liv.c3connector.ui;

import java.util.Comparator;

import multivalent.Behavior;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.Node;
import multivalent.Span;
import uk.ac.liv.c3connector.ArrowVFrame;
import uk.ac.liv.c3connector.FabAnnotation;
import uk.ac.liv.c3connector.FabNote;

/**
 * @author fabio
 * 
 * This is a class used to compare and this way sort the annotations by the position they appear on the documnet.
 * It is still a work in progress and NOT WORKING!.
 * 
 * It's used to sort the annotation list on the right screen.
 * 
 * 
 */
public class FabAnnoPositionComparator implements Comparator {


	boolean ascending;

	public FabAnnoPositionComparator(boolean ascending) {
		this.ascending = ascending;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		if (o1 instanceof FabAnnotation) {
			FabAnnotation a1 = (FabAnnotation) o1;
			if (o2 instanceof FabAnnotation) {
				FabAnnotation a2 = (FabAnnotation) o2;
				Behavior b1 = a1.getBehaviour();
				Behavior b2 = a2.getBehaviour();
				Mark m1 = getNode(b1);
				Mark m2 = getNode(b2);
				int r =0;
				System.out.println(m1);
				System.out.println(m2);
				if (m1 == null && m2!=null)
					r = 1;
				else if (m1 != null && m2==null)
					r = -1;
				else if (m1 == null && m2==null)
					r = 0;
				else {
					if (m1.leaf == m2.leaf)
						r = m1.offset - m2.offset;
					r = 1;
					Node n;
					n=m1.leaf;
					if (n!=null)
						while (n==n.getNextLeaf()) {
							if (n==null){
								r = -1;
								break;
							}
							if (n == m2.leaf){
								r = 1;
								break;
							}
						}
				}
				if (ascending)
					return r;
				return -r;


			}
			throw new ClassCastException("Can compare only FabAnnotation");
		}
		throw new ClassCastException("Can compare only FabAnnotation");

	}
	private Mark getNode(Behavior b1) {
		Mark m = null;
		if (b1 instanceof Span) {
			Span st = (Span) b1;
			m = st.getStart();
		} else if (b1 instanceof FabNote) {
			FabNote fn = (FabNote) b1;
			Node e = fn.getContent();
			if (e instanceof ArrowVFrame) {
				ArrowVFrame avf = (ArrowVFrame) e;
				Node n = avf.getDestination();
				if (n instanceof Leaf) {
					Leaf l = (Leaf) n;
					m = new Mark(l,0);
				} else if (n instanceof INode) {
					INode in = (INode) n;
					m = new Mark(in.getFirstLeaf(), 0);
				}
			}
		}
		return m;
	}

}
