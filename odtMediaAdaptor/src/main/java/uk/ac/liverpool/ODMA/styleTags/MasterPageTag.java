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

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This is for automatic and proper styles, since the distinction between the two
 * lies only on the container. the type is determined by tsh.styleGroup
 * 
 * 
 */
public class MasterPageTag extends StyleTag {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name  */
	@Override
	public String getTagName() {
		return "style:master-page";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler ac) {
		super.init(ac);
		if (MasterPageTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			MasterPageTag.startMethods = m[0];MasterPageTag.contentMethods = m[1];MasterPageTag.endMethods = m[2];
		}
	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return MasterPageTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return MasterPageTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return MasterPageTag.startMethods;
	}
	/* CUT AND PASTE PART ENDS HERE*/
	boolean inHeader = true;


	// TODO: have to implement header and footer elements!

	@Override
	public boolean isEatingAll() {
		return true;
	}
	public void s_style_master__page() {
		s = new Style(tsh.fontdecls);
		Attributes att = bc.getAttr();
		printAttr(att);
		s.name = att.getValue("style:name");
		s.type = Style.MASTERPAGE;
		s.nextStyleName = att.getValue("style:next-style-name");
		s.pageLayoutName= att.getValue("style:page-layout-name");
	}
	/*
    public void s_style_page__layout__properties() {
        s.pageLayoutProperties = new HashMap<String, String>();
        printAtt(bc.getAttr());
        bc.copyAttributesTo(s.pageLayoutProperties);

    }
    public void s_style_footer__style() {
        inHeader = false;
    }
    public void s_style_header__style() {
        inHeader = true;
    }
    public void s_style_header__footer__properties() {
        if (inHeader) {
            s.pageHeaderProperties = new HashMap<String, String>();
            printAtt(bc.getAttr());
            bc.copyAttributesTo(s.pageHeaderProperties);
        } else {
            s.pageFooterProperties = new HashMap<String, String>();
            printAtt(bc.getAttr());
            bc.copyAttributesTo(s.pageFooterProperties);
        }


    }
	 */
	public void e_style_master__page() {
		//if (prints)
		System.out.println("---------"+s.getId());
		tsh.styles.put(s.getId(), s);
		tsh.masterStyles.put(s.getId(), s);
	}


}
