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
package uk.ac.liverpool.ODMA.styleTags;

import java.util.Map;

import multivalent.Document;

import org.xml.sax.SAXException;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;
import uk.ac.liverpool.ODMA.ODT.ODTStyleSheet;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * StylesHandler
 */
public class TextStylesHandler extends BasicContentHandler {


	public Map <String, Style>styles;
	public Map <String, FontDeclaration>fontdecls;
	public Map <String, Style>masterStyles;
	public int styleGroup = 0;
	public ODTStyleSheet ss;
	public static final int OUTSIDE = 0;
	public static final int PROPER = 1;
	public static final int AUTOMATIC = 2;
	public static final int MASTER = 3;
	public final Class[] autoTags= new Class[] {
			FontDeclsTag.class, StyleTag.class, DefaultStyleTag.class, PageLayoutTag.class,
			MasterPageTag.class
	};


	public TextStylesHandler(Document d, Map <String, Style>styles,Map <String, Style>masterStyles, Map <String, FontDeclaration>fontDecls) {
		super(d);
		this.styles = styles;
		fontdecls = fontDecls;
		this.masterStyles = masterStyles ;
		ss = new ODTStyleSheet();
		d.setStyleSheet(ss);
		for (Class autoTag : autoTags)
			try {
				super.addAutomaticRedirection(((TagAdapter)autoTag.newInstance()).getTagName(),autoTag);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	}
	public void s_office_styles() {
		styleGroup=TextStylesHandler.PROPER;
	}
	public void e_office_styles() {
		styleGroup=TextStylesHandler.OUTSIDE;
	}
	public void s_office_automatic__styles() {
		styleGroup=TextStylesHandler.AUTOMATIC;
	}
	public void e_office_automatic__styles() {
		styleGroup=TextStylesHandler.OUTSIDE;
	}
	public void s_office_master__styles() {
		styleGroup=TextStylesHandler.MASTER;
	}
	public void e_office_master__styles() {
		styleGroup=TextStylesHandler.OUTSIDE;
	}

	/* in this we complete the styles, so at runtime we won't need to access the parent styles
	 * 
	 *  (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	 @Override
	 public void endDocument() throws SAXException {
		 super.endDocument();

	 }

	 /**
	  * 
	  */
	 protected void completeStyles() {
		 for (Style current:styles.values())
			 completeStyle(current);

	 }
	 /**
	  * @param current
	  */
	 private void completeStyle(Style current) {
		 if (current == null)
			 return;
		 if (current.isComplete())
			 return;

		 Style parent = styles.get(current.getParentId());
		 //        System.out.println(current.getParentId());
		 completeStyle(parent);
		 // now I have to merge
		 current.merge(parent);
		 current.copied=true;

	 }




}
