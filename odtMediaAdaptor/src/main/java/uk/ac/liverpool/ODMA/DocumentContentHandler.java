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
package uk.ac.liverpool.ODMA;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import multivalent.Document;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * DocumentContentHandler
 * 
 * this class implements method oriented to text contens in documents:
 * space eating and escaping of content (the usual &lt; etc.)
 * keeps track of the text depth and of the max(generic)depth of the doc tree
 * Otherwise a basic handler.
 * 
 * TODO: document better this class and the model
 * 
 */
public class DocumentContentHandler extends BasicContentHandler {

	/**
	 * @param d the document
	 */
	public DocumentContentHandler(Document d) {
		super(d);
	}
	/**
	 * @param d
	 */
	public DocumentContentHandler(Document d, boolean spaceEat, boolean escapeContent) {
		super(d);
		escapeTextTags = escapeContent;
		spaceEating = spaceEat;
	}
	/** current the depth of text tags */
	protected int textDepth = 0;
	/** used to know if multiple space cropping is needed (like in html 4 )*/
	private boolean prevWasSpace = false;

	private boolean escapeTextTags = false;

	private boolean spaceEating = true;
	/** the maximum depth reached in the document (in all tags)*/
	public int maxDepth;

	/** Tags that require space processing (as in HTML) */
	private static Map <String, Integer>space_tags = new HashMap<String, Integer>(100);
	{
		String ts = "text:p text:h text:span text:a text:ref-point text:ref-point-start text:ref-point-end text:bookmark text:bookmark-start text:bookmark-end";
		StringTokenizer t = new StringTokenizer(ts, " ");
		int i = 0;
		while (t.hasMoreElements()) {
			DocumentContentHandler.space_tags.put(t.nextToken(), new Integer(i));
			i++;
		}
	}

	public boolean isSpaceEating() {
		return spaceEating;
	}

	public void setSpaceEating(boolean spaceEating) {
		this.spaceEating = spaceEating;
	}

	public boolean isEscapeTextTags() {
		return escapeTextTags;
	}

	public void setEscapeTextTags(boolean escapeTags) {
		escapeTextTags = escapeTags;
	}

	/** Implemented in ContentHandler
	 * TODO: DOC this and the othe methods
	 * 
	 * always call super if override!
	 * 
	 * */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals("text:h") || qName.equals("text:p"))
			textDepth++;
		super.startElement(uri, localName, qName, attributes);
		if (depth>maxDepth)
			maxDepth = depth;
	}

	/** Implemented in ContentHandler
	 * always call super if override!
	 * 
	 * */
	@Override
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		super.endElement(uri, localName, qName);
		if (qName.equals("text:h") || qName.equals("text:p"))
			textDepth--;
	}

	/** Implemented in ContentHandler */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (content == null) content = new StringBuilder(length);
		// in text elements (specified by space_tags), I have to compress spaces tabs etc. as per specification
		// HTML like
		if ((spaceEating || escapeTextTags)
				&& textDepth > 0 && DocumentContentHandler.space_tags.containsKey(tagName[depth])) {
			for (int i = start; i < start + length; i++)
				if (spaceEating
						&& (ch[i] == 0x0009 || ch[i] == 0x000D || ch[i] == 0x000A || ch[i] == 0x0020)) {
					if (!prevWasSpace) content.append(ch[i]);
					prevWasSpace = true;
				} else if (escapeTextTags) {
					switch (ch[i]) {
					case '<':
						content.append("&lt;");
					break;
					case '>':
						content.append("&gt;");
						break;
					case '&':
						content.append("&amp;");
						break;
					case '"':
						content.append("&quot;");
						break;
					default:
						content.append(ch[i]);
						break;
					}
					prevWasSpace = false;
				} else {
					content.append(ch[i]);
					prevWasSpace = false;
				}
		} else content.append(ch, start, length);
	}

}