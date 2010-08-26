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
import java.util.Iterator;

import multivalent.Document;

import org.xml.sax.SAXException;

import uk.ac.liverpool.ODMA.TagAdapter;
import uk.ac.liverpool.ODMA.styleTags.FontDeclsTag;

public class ToHtmlContentHandler extends ToTextContentHandler {

	static int TEXT_TAG = 1;

	/** output */
	StringBuilder notes = new StringBuilder(100);

	public static Class[] autoTags= new Class[] {
		ListTag.class, PTag.class, GraphicTag.class, HeadingTag.class,
		FontDeclsTag.class
	};

	boolean cropInlines = true;

	public boolean isCropInlines() {
		return cropInlines;
	}

	public void setCropInlines(boolean cropInlines) {
		this.cropInlines = cropInlines;
	}

	/**
	 * 
	 * @param d
	 */
	 public ToHtmlContentHandler(Document d) {
		 super(d);
		 super.setEscapeTextTags(true);
		 super.setSpaceEating(true);
		 for (Class autoTag : ToHtmlContentHandler.autoTags)
			 try {
				 super.addAutomaticRedirection(((TagAdapter)autoTag.newInstance()).getTagName(),autoTag);
			 } catch (InstantiationException e) {
				 e.printStackTrace();
			 } catch (IllegalAccessException e) {
				 e.printStackTrace();
			 }
	 }

	 @Override
	 public void startDocument() throws SAXException {
		 outputBuffer.append("<html>\n<head>" +
		 "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		 Iterator i =  doc.attrKeysIterator();

		 // metadata
		 while (i.hasNext()) {
			 String n =(String) i.next();
			 String v = doc.getAttr(n);
			 outputBuffer.append("<META "+n+"=\""+v+"\">\n");
		 }

		 outputBuffer.append("</head>\n");
		 outputBuffer.append("<body bgcolor=\"#FFFFFF\">\n");
		 parseTime = System.currentTimeMillis();
	 }

	 @Override
	 public void endDocument() throws SAXException {
		 if (notes.length()>0)
			 outputBuffer.append("<h1>NOTES</h1>");
		 outputBuffer.append(notes);

		 outputBuffer.append("</body></html>");
		 try {
			 FileWriter tt = new FileWriter("out.html");
			 tt.write(outputBuffer.toString());
			 tt.close();
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 parseTime = System.currentTimeMillis()-parseTime;
		 //System.out.println(maxDepth);
	 }

	 /**  NB: methods here can't be private, since the ancestor need to access them (method call) */


	 public StringBuilder swapOutput(StringBuilder o) {
		 StringBuilder tmp = outputBuffer;
		 outputBuffer = o;
		 return tmp;
	 }

}