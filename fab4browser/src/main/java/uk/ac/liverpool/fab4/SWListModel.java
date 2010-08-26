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
package uk.ac.liverpool.fab4;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;


/**
 * A generic list model, used for the annotation list, based on a list. Includes sorting as an option.
 * 
 * @author fabio
 *
 * @param <E>
 */
public class SWListModel<E> extends AbstractListModel implements Iterable {

	private static final long serialVersionUID = -2263924818570522425L;

	/**
	 * The List containing the elements
	 */
	List<E> v;

	/** an optional comparator for sorted lists. */
	Comparator<E> comp = null;

	/** Generates the model based on the specified list */
	public SWListModel(List<E> v) {
		if (comp != null)
			Collections.sort(v, comp);
		this.v = v;
	}

	/** Generates the model on an empty list */
	public SWListModel() {
		this.v = new Vector<E>();
	}

	public void add(E o) {
		v.add(o);
		if (comp != null)
			Collections.sort(v, comp);
		final int i = v.indexOf(o);
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					fireIntervalAdded(this, i, i);
				}
			});
		else
			fireIntervalAdded(this, i, i);
	}

	public void remove(final int i) {
		v.remove(i);
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					fireIntervalRemoved(this, i, i);
				}
			});
		else
			fireIntervalRemoved(this, i, i);
	}

	public void remove(final E o) {
		final int i = v.indexOf(o);
		v.remove(o);
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					fireIntervalRemoved(this, i, i);
				}
			});
		else
			fireIntervalRemoved(this, i, i);
	}

	public int getSize() {
		return v.size();
	}

	public E getElementAt(int index) {
		try {
			return v.get(index);

		} catch (Exception d) {
			return null;
		}
	}

	public E get(int index) {
		return v.get(index);
	}

	public void clear() {
		final int s = getSize();
		v.clear();
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					fireIntervalRemoved(this, 0, s);
				}
			});
		else
			fireIntervalRemoved(this, 0, s);

	}

	public Iterator iterator() {
		return v.iterator();
	}

	public Comparator<E> getComp() {
		return comp;
	}

	public Object[] getArray() {
		return v.toArray();
	}

	/** Sets the comparator for the list, and sorts */
	public void setComp(Comparator<E> comp) {
		this.comp = comp;
		if (comp != null)
			Collections.sort(v, comp);
		final int s = getSize();
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					fireIntervalRemoved(this, 0, s);
				}
			});
		else
			fireIntervalRemoved(this, 0, s);
	}

}