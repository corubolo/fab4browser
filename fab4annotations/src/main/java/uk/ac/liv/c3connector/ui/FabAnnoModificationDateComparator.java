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
import java.util.Date;

import uk.ac.liv.c3connector.FabAnnotation;

/**
 * This is a class used to compare and this way sort the annotations by modification date (default).
 * 
 * It's used to sort the annotation list on the right screen.
 * 
 * @author fabio
 * 
 */
public class FabAnnoModificationDateComparator implements Comparator {


	boolean ascending;


	/**
	 * 
	 */
	public FabAnnoModificationDateComparator(boolean ascending) {
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
				Date c1 = a1.getAnn().getDateModified();
				Date c2 = a2.getAnn().getDateModified();
				if (ascending)
					return c1.compareTo(c2);
				return -c1.compareTo(c2);


			}
			throw new ClassCastException("Can compare only FabAnnotation");
		}
		throw new ClassCastException("Can compare only FabAnnotation");

	}

}
