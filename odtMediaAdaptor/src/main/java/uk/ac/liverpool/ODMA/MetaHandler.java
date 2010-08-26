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

import multivalent.Document;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * MetaHandler
 * 
 * The metadata is independent of the file format, so this handler should work fine with all the formats.
 * The extracted data is all put to the @see multivalent.Document attributes and can be accessed
 * from it using the specified keys. Statistics are also accessible using ODF meta-statistic
 * default attribute names
 * 
 */
public class MetaHandler extends DefaultHandler {

	/** (ODF meta) The generator (application). As with most of the others, can be accessed with Document.getAttr(MetaHandler.NAME) */
	public final static String GENERATOR = "meta:generator";//
	/** (ODF meta) The keywords . For access @see GENERATOR */
	public final static String KEYWORD = "meta:keyword";//
	/** (ODF meta) For access @see GENERATOR */
	public final static String INITIAL_CREATOR = "meta:initial-creator";//
	/** (ODF meta) For access @see GENERATOR */
	public final static String PRINTED_BY = "meta:printed-by";//
	/** (ODF meta) date and time :format (YYYY-MM-DDThh:mm:ss) . For access @see GENERATOR */
	public final static String CREATION_DATE = "meta:creation-date";//
	/** (ODF meta) date and time :format (YYYY-MM-DDThh:mm:ss) . For access @see GENERATOR */
	public final static String EDITING_CYCLES = "meta:editing-cycles";//
	/** (ODF meta) date and time :format (YYYY-MM-DDThh:mm:ss) . For access @see GENERATOR */
	public final static String EDITING_DURATION = "meta:editing-duration";//
	/** (ODF meta) date and time :format (YYYY-MM-DDThh:mm:ss) . For access @see GENERATOR */
	public final static String TEMPLATE_URI = "meta:template-uri";//

	/** (Dublic Core). For access @see GENERATOR */
	public final static String DESCRIPTION = "dc:description";//
	/** (Dublic Core). For access @see GENERATOR */
	public final static String SUBJECT = "dc:subject";//
	/** (Dublic Core) Modification date and time format (YYYY-MM-DDThh:mm:ss) . For access @see GENERATOR */
	public final static String DATE = "dc:date";//
	/** (Dublic Core) Language . For access @see GENERATOR */
	public final static String LANGUAGE = "dc:language";//

	/** (Dublin Core creator) Author . Can be also accessed with the standard Multivalent key @see Document.ATTR_AUTHOR */
	public final static String AUTHOR = "dc:creator";//
	/** (Dublin Core title) Title . Can be also accessed with the standard Multivalent key @see Document.ATTR_TITLE */
	public final static String TITLE = "dc:title";//

	multivalent.Document doc;
	Object element;

	//int depth = 0;

	public MetaHandler(multivalent.Document d) {
		doc = d;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		//depth++;
		if (qName.equals("meta:template"))
			doc.putAttr(MetaHandler.TEMPLATE_URI, attributes.getValue("xlink:href"));

		// this is to use only for the statistics!! //
		if (qName.equals("meta:document-statistic"))
			if (attributes != null) for (int i = 0; i < attributes.getLength(); i++)
				doc.putAttr(attributes.getQName(i), attributes.getValue(i));

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		if (element == null) return;
		String content = element.toString();
		if (qName.equals(MetaHandler.GENERATOR)) doc.putAttr(MetaHandler.GENERATOR, content);
		else if (qName.equals(MetaHandler.TITLE)) {
			doc.putAttr(Document.ATTR_TITLE, content);
			doc.putAttr(MetaHandler.TITLE, content);
		} else if (qName.equals(MetaHandler.AUTHOR)) {
			doc.putAttr(Document.ATTR_AUTHOR, content);
			doc.putAttr(MetaHandler.AUTHOR, content);
		} else if (qName.equals(MetaHandler.DESCRIPTION)) doc.putAttr(MetaHandler.DESCRIPTION, content);
		else if (qName.equals(MetaHandler.SUBJECT)) doc.putAttr(MetaHandler.SUBJECT, content);
		else if (qName.equals(MetaHandler.KEYWORD)) doc.putAttr(MetaHandler.KEYWORD, content);
		else if (qName.equals(MetaHandler.INITIAL_CREATOR)) doc.putAttr(MetaHandler.INITIAL_CREATOR, content);
		else if (qName.equals(MetaHandler.PRINTED_BY)) doc.putAttr(MetaHandler.PRINTED_BY, content);
		else if (qName.equals(MetaHandler.CREATION_DATE)) doc.putAttr(MetaHandler.CREATION_DATE, content);
		else if (qName.equals(MetaHandler.DATE)) doc.putAttr(MetaHandler.DATE, content);
		else if (qName.equals(MetaHandler.LANGUAGE)) doc.putAttr(MetaHandler.LANGUAGE, content);
		else if (qName.equals(MetaHandler.EDITING_CYCLES)) doc.putAttr(MetaHandler.EDITING_CYCLES, content);
		else if (qName.equals(MetaHandler.EDITING_DURATION)) doc.putAttr(MetaHandler.EDITING_DURATION, content);

		element = null;
		//depth--;
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