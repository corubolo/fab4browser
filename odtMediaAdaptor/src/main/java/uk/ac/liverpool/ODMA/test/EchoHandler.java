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
package uk.ac.liverpool.ODMA.test;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class EchoHandler extends DefaultHandler {
	multivalent.Document doc;
	Object element;
	int depth = 0;

	//XMLConstants.

	public EchoHandler(multivalent.Document d) {
		doc = d;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		/**********           NB!!!           */
		if (element != null) {
			System.out.println(element);
			element = null;
		}


		depth++;
		for (int i = 0; i < depth; i++)
			System.out.print("\t");
		System.out.print("<" + qName + " ");
		// no name space awareness
		//System.out.print("<"+localName+" ");
		//System.out.print("<"+uri+" ");
		if (attributes != null) for (int i = 0; i < attributes.getLength(); i++) {
			//System.out.print(attributes.getType(i)+" | ");
			System.out.print(attributes.getQName(i) + "=\"");
			//System.out.print(attributes.get(i));
			System.out.print(attributes.getValue(i));
			System.out.print("\" ");
		}
		System.out.println(">");

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
	throws SAXException {

		if (element != null) {
			for (int i = 0; i < depth + 1; i++)
				System.out.print("\t");
			System.out.println(element);
			element = null;
		}

		for (int i = 0; i < depth; i++)
			System.out.print("\t");
		System.out.println("</" + qName + ">");

		depth--;

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#ignorableWhitespace(char[], int, int)
	 */
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		// TODO Auto-generated method stub
		super.ignorableWhitespace(ch, start, length);
		System.out.println("**WARNIINNNGGG**");
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

		if (element == null) element = new StringBuilder(100);
		((StringBuilder) element).append(ch, start, length);


	}

}