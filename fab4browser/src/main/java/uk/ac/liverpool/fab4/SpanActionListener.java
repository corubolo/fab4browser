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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JMenuItem;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.Layer;
import multivalent.Mark;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.std.ui.SpanUI;


/**
 * A Listener for the actions (from Fab4 UI) that create spans of text.
 * Used by the AnnotationSidePanel class.
 * 
 * 
 * @author fabio
 *
 */
class SpanActionListener implements ActionListener {
	Map<String, Object> att;

	String name;

	String cln;

	boolean edit;

	Fab4 t;

	JButton bu;

	JMenuItem mi;

	public SpanActionListener(Map<String, Object> att, String name, String cln,
			boolean edit, Fab4 t, JButton tm) {
		this.att = att;
		this.name = name;
		this.edit = edit;
		this.cln = cln;
		this.t = t;
		bu = tm;
	}

	public SpanActionListener(Map<String, Object> att, String name, String cln,
			boolean edit, Fab4 t, JMenuItem tm) {
		this.att = att;
		this.name = name;
		this.edit = edit;
		this.cln = cln;
		this.t = t;
		mi = tm;
	}

	public void actionPerformed(ActionEvent e) {
		Span sel = t.getCurBr().getSelectionSpan();
		Mark cm = t.getCurBr().getCursorMark().getMark();
		if (att != null)
			if (att.get("point") != null && cm != null && !sel.isSet()
					&& cm.isSet()) {
				Span newspan = (Span) Behavior.getInstance(name, cln, att, t
						.getCurDoc().getLayer(Layer.PERSONAL));
				Mark cmm = new Mark(cm);
				cmm.offset = cmm.offset + 1;
				newspan.move(cm, cmm);
				newspan.putAttr(SpanUI.ATTR_CREATEDAT, Long.toString(System
						.currentTimeMillis()));
				if (edit)
					t.getCurBr().eventq(
							new SemanticEvent(t.getCurBr(), Span.MSG_EDIT,
									null, newspan, null));
				return;

			}
		if (sel.isSet())
			newSpan();
		else {
			t.getCurBr().eventq(Browser.MSG_STATUS,
			"Now select the area on the document");
			t.getCurBr().setCursor(FabIcons.getIcons().SelectCursor);
			if (bu != null)
				bu.setEnabled(false);
			UiBehavior.execureAfterSpanSelection(new Runnable() {

				public void run() {
					newSpan();
					t.getCurBr().setCursor(Cursor.getDefaultCursor());
					if (bu != null)
						bu.setEnabled(true);
				}

			});
		}
	}

	private void newSpan() {

		Document mvDocument = (Document) t.getCurBr().getRoot().findBFS(
		"content");
		Point p = new Point(mvDocument.getHsb().getValue(), mvDocument.getVsb()
				.getValue());

		Span sel = t.getCurBr().getSelectionSpan();
		Span newspan = (Span) Behavior.getInstance(name, cln, att, t
				.getCurDoc().getLayer(Layer.PERSONAL));

		newspan.move(sel);
		sel.moveq(null);
		newspan.putAttr(SpanUI.ATTR_CREATEDAT, Long.toString(System
				.currentTimeMillis()));
		if (edit)
			t.getCurBr().eventq(
					new SemanticEvent(t.getCurBr(), Span.MSG_EDIT, null,
							newspan, null));

		t.getCurBr().eventq(
				new SemanticEvent(t.getCurBr(), UiBehavior.MSG_SCROLLTO, p));
	}
}
