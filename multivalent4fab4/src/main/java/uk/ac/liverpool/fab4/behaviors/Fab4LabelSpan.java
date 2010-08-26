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

package uk.ac.liverpool.fab4.behaviors;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;

import multivalent.Browser;
import multivalent.Context;
import multivalent.ContextListener;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.std.span.ActionSpan;
import multivalent.std.ui.DocumentPopup;

import com.pt.awt.NFont;

/**
 * Modified span to allow marking and disctinction of span annotations
 */
public class Fab4LabelSpan extends ActionSpan {

	public static final String MSG_SPAN_SEL = "FabspanSelected";

	static NFont annoFont = NFont.getInstance("Helvetica", NFont.WEIGHT_NORMAL,
			NFont.FLAG_SANSSERIF, 8f);

	String label_ = "no label";

	public Color defTextColor = Color.RED.darker();

	Color textColor = defTextColor;

	public Color defColor = new Color(255, 255, 200);

	Color color_ = defColor;

	private Composite cc = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
			0.80f);

	private boolean border = false;

	public void setLabel(String label) {
		label_ = label;
	}

	@Override
	public boolean paintBefore(Context cx, Node start) {
		Graphics2D g = cx.g;
		Shape r = g.getClip();
		g.setClip(-100, -15, 300, 240);
		g.setColor(textColor);
		Composite c = g.getComposite();
		g.setComposite(cc);
		Fab4LabelSpan.annoFont.drawString(g, label_, cx.x, Fab4LabelSpan.annoFont.getAscent() - 8);
		g.setComposite(c);

		g.setClip(r);
		return false;
	}

	/** Morphing and deletion menu items in popup. */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg))
			return true;
		else if (this != se.getIn()) {
		} else if (DocumentPopup.MSG_CREATE_DOCPOPUP == msg && isEditable()) {
			INode menu = (INode) se.getOut(); // attrs.get("MENU");
			Browser br = getBrowser();
			createUI("button", "Morph Span to Selection", new SemanticEvent(br,
					Span.MSG_MORPH, null, this, null), menu, "EDIT", !br
					.getSelectionSpan().isSet());
			createUI("button", "Delete Span"/* +" "+getClass().getName() */,
					new SemanticEvent(br, Span.MSG_DELETE, null, this, null), menu,
					null/* end of menu, after protective separator */, false);
		}
		return false;
	}

	/**
	 * Set to {@link Context#COLOR_INVALID} to invalidate, null for transparent.
	 */
	public void setColor(Color color) {
		color_ = color;
	}

	public Color getColor() {
		return color_;
	}

	@Override
	public int getPriority() {
		return ContextListener.PRIORITY_SPAN - ContextListener.LOT;
	}

	@Override
	public void moveq(Leaf ln, int lo, Leaf rn, int ro) {
		if (rn != null && ro > rn.size()) {
			Leaf next = rn.getNextLeaf();
			if (next != null) {
				rn = next;
				ro = 0;
			}
		}
		super.moveq(ln, lo, rn, ro);
	}

	@Override
	public boolean appearance(Context cx, boolean all) {
		if (color_ != Context.COLOR_INVALID)
			cx.background = color_;
		if (border)
			cx.borderleft = cx.borderbottom = cx.borderright = cx.bordertop = 1;
		return false;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.std.span.ActionSpan#action()
	 */
	@Override
	protected boolean action() {
		Browser br = getBrowser();
		if (br != null)
			br.eventq(Fab4LabelSpan.MSG_SPAN_SEL, this);
		return false;
	}

	public boolean isBorder() {
		return border;
	}

	public void setBorder(boolean border) {
		this.border = border;
	}

}
