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

import java.io.FileWriter;
import java.io.IOException;

import multivalent.Document;

import org.xml.sax.SAXException;

import uk.ac.liverpool.ODMA.DocumentContentHandler;

public class ToTextContentHandler extends DocumentContentHandler{

	/** output */
	public StringBuilder outputBuffer = new StringBuilder(10000);

	boolean outputContent = true;

	long parseTime;

	/**
	 * 
	 * @param d
	 */
	public ToTextContentHandler(Document d) {
		super(d,true,false);

	}

	@Override
	public void startDocument() throws SAXException {
		parseTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			FileWriter tt = new FileWriter("out.txt");
			tt.write(outputBuffer.toString());
			tt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		parseTime = System.currentTimeMillis()-parseTime;
		//System.out.println("done parsing in "+(System.currentTimeMillis()-startTime)+"ms");
	}



	@Override
	protected void manageContent() {
		super.manageContent();
		if (textDepth>0 && outputContent)
			outputBuffer.append(content);

	}

	// for now we ignore annotation
	public void s_office_annotation() {
		outputContent = false;
	}

	public void e_office_annotation() {
		outputContent = true;
	}

	// and tracked changes !!
	public void s_text_tracked__changes() {
		outputContent = false;
		//        System.out.println("track chng!!");
	}

	public void e_text_tracked__changes() {
		outputContent = true;
	}


	public void e_text_h() {
		outputBuffer.append("\n");
	}
	public void e_text_p() {
		outputBuffer.append("\n");
	}

	public void s_text_line__break() {
		outputBuffer.append("\n");
	}
	public void s_text_s() {
		int c = 1;
		try {
			String n =startAttirbs.getValue("text:c");
			if ( n != null) c = Integer.parseInt(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < c; i++)
			outputBuffer.append(' ');
	}
	public void s_text_tab() {
		int c = 1;
		try {
			String n =startAttirbs.getValue("text:tab-ref");
			if ( n != null) c = Integer.parseInt(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < c; i++)
			outputBuffer.append('\t');

	}

}