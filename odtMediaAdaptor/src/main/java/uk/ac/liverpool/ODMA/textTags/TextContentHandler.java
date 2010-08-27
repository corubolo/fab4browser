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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Map;

import multivalent.Document;
import multivalent.INode;

import org.xml.sax.SAXException;

import phelps.util.Units;
import uk.ac.liverpool.ODMA.ODFpackage;
import uk.ac.liverpool.ODMA.TagAdapter;
import uk.ac.liverpool.ODMA.Nodes.ODTIVBox;
import uk.ac.liverpool.ODMA.ODT.ODT;
import uk.ac.liverpool.ODMA.styleTags.FontDeclaration;
import uk.ac.liverpool.ODMA.styleTags.Style;
import uk.ac.liverpool.ODMA.styleTags.TextStylesHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * TextContentHandler
 */
public class TextContentHandler extends TextStylesHandler {

	boolean inBody = false;
	INode root ;
	INode parent;
	public ODFpackage zipFile;
	int maxdepth;

	public final Class[] autoTags2 = new Class[] {
			TrackedChangesTag.class , PTag.class, HTag.class,
			FrameTag.class, ListTag.class, ListItemTag.class,
			//NoteTag.class
			// FIXME: STILL TO IMPLEMENT, SO DISABLED!!
	};

	public TextContentHandler(Document d, Map <String, Style>styless, Map <String, Style>Mstyless, Map <String, FontDeclaration>fontDecls, ODFpackage zipFile) {
		super(d, styless,Mstyless, fontDecls);
		this.zipFile=zipFile;
		for (Class element : autoTags2)
			try {
				super.addAutomaticRedirection(((TagAdapter) element.newInstance())
						.getTagName(), element);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			// the Standard page?
					masterStyles.get("Standard");
			Style pageLayout = styles.get(Style.getId(masterStyles.get(Style.getId("Standard",null)).pageLayoutName,null));
			ODT.printMap(pageLayout.pageLayoutProperties);
			Map <String, String>plp = pageLayout.pageLayoutProperties;
			int w  = (int) Units.getLength(plp.get("fo:page-width"), "px");
			int h  = (int)Units.getLength(plp.get("fo:page-height"), "px");
			int  mt  = (int)Units.getLength(plp.get("fo:margin-top"), "px");
			int  mb  = (int)Units.getLength(plp.get("fo:margin-bottom"), "px");
			int  ml  = (int)Units.getLength(plp.get("fo:margin-left"), "px");
			int  mr  = (int)Units.getLength(plp.get("fo:margin-right"), "px");
			ODTIVBox b;
			Insets i = new Insets(mt,ml,mb,mr);
			parent = root = b = new ODTIVBox("body",null, d, new Dimension(w,h),i);
			b.setPagebg(Color.WHITE);
			//root = new IParaBox("realBody",null, root);
			//        System.out.println("-------------");
			//        ODT.printMap(root.getAttributes());
			//System.out.println(pw+" "+ph+" - "+w+" "+h+" - "+ww+" "+hh);
			//root =
			//new LeafBlock("leaf",null, root,(int)w,(int)h);
			//root.bbox = new Rectangle(0,0,(int)w,(int)h);
			//new LeafBlock("cont",null,root,(int)w,(int)h);
			//root.bbox = new Rectangle(0,0,(int)w,(int)h);

	}

	public void s_office_body() {
		inBody = true;
		// remove the style tag elements, since they won't happen again :)
		for (Class autoTag : autoTags)
			try {
				super.removeAutomaticRedirection(((TagAdapter)autoTag.newInstance()).getTagName());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			completeStyles();

	}

	/*keep this... we don't want to complete the styles at doc end ... see TextStylesHandles */
	@Override
	public void endDocument() throws SAXException {

	}
}
