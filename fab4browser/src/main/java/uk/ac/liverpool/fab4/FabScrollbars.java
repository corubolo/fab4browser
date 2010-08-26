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

import java.awt.AWTEvent;
import java.awt.Adjustable;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;

import multivalent.IScrollPane;
import multivalent.gui.VScrollbar;

/**
 * A replacement for Multivalent scrollbars.
 * @author fabio
 *
 */
public class FabScrollbars extends VScrollbar implements AdjustmentListener {

	public JScrollBar s;
	int value = 0, min = 0, max = 100, visible = 0;
	int lineInc_ = 15, blockInc_ = 30;
	boolean du = false, dd = false;

	private void printVals(String st) {

	}

	public void setVisible(int vis) {
		visible = vis;
		blockInc_ = vis - 30 > 0 ? vis - 30 : 5;
		lineInc_ = blockInc_ / 8 > 10 ? blockInc_ / 8 : 10;

		s.setBlockIncrement(blockInc_);
		s.setUnitIncrement(lineInc_);

		updateVals();

		printVals("setVisible");
	}

	@Override
	public int getBlockIncrement() {
		return blockInc_;
	}

	@Override
	public int getLineIncrement() {
		return lineInc_;
	}

	@Override
	public void setMinMax(int minimum, int maximum) {
		min = minimum;
		max = maximum;
		updateVals();
		printVals("setminmax");
	}

	public void updateVals() {
		s.setValues(value, visible, min, max);
	}

	@Override
	public int getMin() {
		return min;
	}

	@Override
	public int getMax() {
		return max;
	}

	@Override
	public int getValue() {
		return value;
	}

	public FabScrollbars(int orientation) {
		super(orientation);
		if (orientation == VScrollbar.HORIZONTAL)
			s = new JScrollBar(Adjustable.HORIZONTAL);
		else
			s = new JScrollBar(Adjustable.VERTICAL);
		super.setShowPolicy(VScrollbar.SHOW_NEVER);
		s.addAdjustmentListener(this);
	}

	@Override
	public void setValue(int value, boolean pickplace) {

		value = Math.max(min, Math.min(value, max - visible));
		s.setValue(value);
		if (this.value == value)
			return;
		this.value = value;
		scrolla();
		printVals("setValue");
	}

	@Override
	public void setValue(int value) {
		setValue(value, false);

	}

	public void scrolla() {
		IScrollPane isp = getIScrollPane();
		if (isp.isValid())
			isp.repaint(250);

	}

	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		return false;
	}


	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getValue() == value)
			return;
		value = e.getValue();
		scrolla();
		printVals("adjValChanged");
	}

}
