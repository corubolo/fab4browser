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
package uk.ac.liverpool.ODMA.ODT.tohtml;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * CommonContentTags
 */
public abstract class CommonContentTags extends TagAdapter{


	ToHtmlContentHandler de;
	boolean open=false;
	@Override
	public void init(BasicContentHandler dc) {
		super.init(dc);
		de = (ToHtmlContentHandler)dc;
	}
	// spaces
	public void s_text_s() {
		int c = 1;
		try {
			String n =de.getAttr().getValue("text:c");
			if ( n != null) c = Integer.parseInt(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < c; i++)
			de.outputBuffer.append("&nbsp;");
	}

	public void s_text_tab() {
		int c = 1;
		try {
			String n =de.getAttr().getValue("text:tab-ref");
			if ( n != null) c = Integer.parseInt(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < c; i++)
			de.outputBuffer.append("&nbsp;&nbsp;&nbsp;");

	}


	public void s_text_line__break() {
		de.outputBuffer.append("<br/>");
	}

	// hiperlinks
	public void s_text_a() {
		String n =de.getAttr().getValue("xlink:href");
		String t =de.getAttr().getValue("office:target-frame-name");
		if ( n != null) {
			open=true;
			de.outputBuffer.append("<a href=\""+n+"\""+
					(t==null?"":" target=\""+t+"\"")
					+">");
		}
	}
	public void e_text_a() {
		if (open)
			de.outputBuffer.append("</a>\n");
	}

	public void s_text_note__citation() {
		de.outputBuffer.append("<sup>[<a href=\"#note");
	}
	// note number
	public void c_text_note__citation() {
		de.outputBuffer.append(de.getContent()+"\">");
		de.notes.append("<a name=\"note"+de.getContent()+"\">"+de.getContent()+"</a>");
	}

	public void e_text_note__citation() {
		de.outputBuffer.append("</a>]</sup>");
	}

	public void s_text_note__body() {
		de.notes = de.swapOutput(de.notes);
	}
	public void e_text_note__body() {
		de.notes = de.swapOutput(de.notes);
	}
}
