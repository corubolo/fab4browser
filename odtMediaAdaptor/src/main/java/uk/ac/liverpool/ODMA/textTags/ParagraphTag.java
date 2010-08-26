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

import multivalent.Behavior;
import multivalent.CLGeneral;
import multivalent.Context;
import multivalent.Span;
import multivalent.node.IParaBox;
import multivalent.node.LeafUnicode;
import multivalent.std.span.HyperlinkSpan;
import uk.ac.liverpool.ODMA.styleTags.Style;


/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * Common methods for paragraph text (P and H)
 * 
 */
public abstract class ParagraphTag extends GenericContentTag {

	// spaces
	IParaBox para;

	LeafUnicode ll;

	SSpan lsp;

	HyperlinkSpan h;

	//TODO> text:line-break">
	public void s_text_s() {
		int count = 1;
		try {
			String n = bc.getAttr().getValue("text:c");
			if (n != null)
				count = Integer.parseInt(n);

		} catch (Exception e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++)
			sb.append(' ');
		new LeafUnicode(sb.toString(), null, para);
	}

	public void s_text_tab() {
		int count = 1;
		try {
			String n = bc.getAttr().getValue("text:tab-ref");
			if (n != null)
				count = Integer.parseInt(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < count; i++)
			new LeafUnicode("   ", null, para);
	}


	// hiperlinks are supposed to be non tested I guess (double hiperlinks make
	// little sense and
	// for span the doc specifies that they can be nested :)
	public void s_text_a() {
		String n = getAttr().getValue("xlink:href");
		//String t =
		getAttr().getValue("office:target-frame-name");
		// System.out.println(n+" "+t);
		if (n != null) {
			h = (HyperlinkSpan) Behavior.getInstance("hyperlink",
					"multivalent.std.span.HyperlinkSpan", null, null, tch.doc
					.getLayers());
			// h = new HyperlinkSpan();
			h.setURI(n);
			if (ll != null)
				h.open(ll);
			else
				h.open(para);
		}
	}

	public void c_text_a() {
		contentToLeaves();
	}

	public void e_text_a() {
		if (h != null) { // ll!=null &&
			h.close(ll);
			h = null;
		}

	}

	public void s_text_note__citation() {
	}

	// note number
	public void c_text_note__citation() {
		contentToLeaves();
		//System.out.println("---------"+tch.getContent());
	}

	public void e_text_note__citation() {
	}

	public void c_text_sequence() {
		contentToLeaves();
	}


	public void s_text_note__body() {
	}

	public void c_text_note__body() {
		contentToLeaves();
		//System.out.println("---------"+tch.getContent());
	}

	public void e_text_note__body() {
	}



	public void s_text_span() {
		//Style st =
		tch.styles.get(Style.getId(getStyleName(), "text"));
		if (st != null && st.textProperties != null) {
			CLGeneral clg = st.getClg();
			lsp = new SSpan(clg, lsp);
			if (ll != null)
				lsp.open(ll);
			else
				lsp.open(para);
		}
	}

	public void e_text_span() {
		if (lsp != null) { // ll!=null &&
			lsp.close(ll);
			lsp = lsp.prev;
		}
	}

	public void c_text_span() {
		contentToLeaves();
	}


	protected void contentToLeaves() {
		StringBuilder cont = tch.getContent();
		int previ = 0;
		while (previ < cont.length() && cont.charAt(previ) == ' ')
			previ++;
		for (int i = previ, l = cont.length(); i < l; i++)
			if (cont.charAt(i) == ' ' || i == l - 1) {
				if (i == l - 1 && cont.charAt(i) != ' ')
					i++;
				String w = cont.substring(previ, i);
				if (w.equals(""))
					continue;
				ll = new LeafUnicode(w, null, para);
				previ = i + 1;


			}
	}


	public class SSpan extends Span {
		/**
		 * 
		 */
		CLGeneral clg;

		SSpan prev;

		public SSpan(CLGeneral clg, SSpan prev) {
			super();
			this.clg = clg;
			this.prev = prev;
		}

		@Override
		public boolean appearance(Context cx, boolean all) {
			return clg.appearance(cx, all);
		}
	}
}
