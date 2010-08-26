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
import java.awt.Point;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Context;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.SystemEvents;
import multivalent.std.ui.DocumentPopup;
import uk.ac.liverpool.fab4.ImageInternalDataFrame;

import com.pt.awt.NFont;

/**
 * A note on a span of text
 * 
 * @author fabio
 * 
 */
public class CitesSpanNote extends Fab4LabelSpan {

	public static final String CITED_URI = "cited_uri";
	public static final String ATTR_SELECTION = "selection";
	private String theText;

	private boolean visible = true;

	static NFont annoFont = NFont.getInstance("Helvetica", NFont.WEIGHT_NORMAL,
			NFont.FLAG_SANSSERIF, 13f);
	private Composite cc = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
			0.80f);
	private ImageInternalDataFrame textFrame;

	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {

		if (super.semanticEventBefore(se, msg))
			return true;
		else if (DocumentPopup.MSG_CREATE_DOCPOPUP == msg && this == se.getIn()) {
			INode menu = (INode) se.getOut();
			Browser br = getBrowser();
			if (isEditable())
				createUI("button", "Edit Message", new SemanticEvent(br,
						Span.MSG_EDIT, theText, this, null), menu, "EDIT",
						false);

		} else if (Fab4LabelSpan.MSG_SPAN_SEL == msg && this == se.getArg())
			showContents();
		return false;
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (this != se.getIn()) {
		} else if (SystemEvents.MSG_FORM_DATA == msg) {
			Map map = (Map) se.getArg();
			if (map != null && map.get("cancel") == null) {
				String val = (String) map.get(CitesSpanNote.CITED_URI);
				if (val != null)
					theText = val;
				putAttr(CitesSpanNote.CITED_URI, theText);
				return true;
			}
		}

		return super.semanticEventAfter(se, msg);
	}

	@Override
	public boolean paintBefore(Context cx, Node start) {
		if (visible) {
			Graphics2D g = cx.g;
			Shape r = g.getClip();
			g.setClip(-100, -100, 300, 300);
			g.setColor(Color.BLACK);
			Composite c = g.getComposite();
			g.setComposite(cc);
			CitesSpanNote.annoFont.drawString(g, theText, cx.x - 100,
					CitesSpanNote.annoFont.getAscent() - 100);
			g.setClip(r);
			g.setComposite(c);
			return false;
		} else
			return super.paintBefore(cx, start);
	}

	@Override
	public ESISNode save() {
		putAttr(CitesSpanNote.CITED_URI, theText);
		ESISNode e = super.save();
		e.appendChild(theText);
		return e;
	}

	@Override
	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);
		theText = getAttr(CitesSpanNote.CITED_URI, "(nothing)");
		System.out.println(theText);
		putAttr(CitesSpanNote.CITED_URI, theText);

	}

	public String getTheText() {
		return theText;
	}

	public void setTheText(String theText) {
		this.theText = theText;
	}

	public void showContents() {
		if (textFrame == null) {
			Layer sc = getBrowser().getCurDocument().getLayer(Layer.SCRATCH);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("text", theText);
			m.put("background", "FFFF33");
			m.put("foreground", "000000");
			m.put("links", theText);

			textFrame = (ImageInternalDataFrame) Behavior.getInstance("ImageInternalDataFrame",
					"uk.ac.liverpool.fab4.ImageInternalDataFrame", null, m, sc);
			textFrame.setTitle("Citation of the document (URI): ");
			textFrame.getFrame().setSize(120, 60);
			textFrame.getFrame().setTransparent(false);
		}
		Point absLocation = getStart().leaf.getAbsLocation();
		textFrame.getFrame().setLocation(absLocation.x, absLocation.y);
		textFrame.getFrame().raise();
	}

	public void hideNoteContents() {
		if (textFrame != null)

			textFrame.getFrame().close();
	}

}
