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
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.ODMA.styleTags;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import uk.ac.liverpool.ODMA.BasicContentHandler;

import com.pt.awt.NFont;
import com.pt.awt.font.NFontManager;

/**
 * @author fabio
 * 
 * Really simple implementation of the font declarations. Has attributes name
 * and a map for the attributes (wich includes the name).
 */
public class FontDeclaration {

	//public String name;

	public String family;

	public Map<String, String> attibutes;

	private static Set<String> fam;

	public FontDeclaration(Attributes att) {

		//name = att.getValue("style:name");

		attibutes = new HashMap<String, String>();
		BasicContentHandler.copyAttributesTo(att, attibutes);

		family = attibutes.get("svg:font-family");
		family = family.replaceAll("'", "");

		if (FontDeclaration.fam == null) {
			NFontManager m = NFontManager.getDefault();
			FontDeclaration.fam = new HashSet<String>(Arrays.asList(m.getAvailableFamilies()));
		}
		boolean found = FontDeclaration.fam.contains(family);

		/*
		 * TODO: COMPUTE SUBSTITTUTE FONT in here, using
		 * style:font-family-generic = roman style:font-pitch = variable
		 * style:font-charset = x-symbol
		 * 
		 * and use them to find a suitable substitution font! (doc says pg. 476,
		 * SHOULD use CSS2 (css2 par. 15.5) font substitution algorithm
		 * 
		 */
		if (!found) {
			String ff2 = attibutes.get("style:font-pitch");
			// String ff2 = fd.attibutes.get("font-family-generic");
			if (ff2 != null && ff2.equals("fixed"))
				family = NFont.getInstance(family, 0, NFont.FLAG_FIXEDPITCH, 12.0f)
				.getFamily();
			String temp;
			/* these attributes are never used? */
			temp = attibutes.get("svg:font-weight");
			if (temp != null)
				System.out.println("**** weigth " + temp);
			temp = attibutes.get("svg:font-style");
			if (temp != null)
				System.out.println("**** style " + temp);
			temp = attibutes.get("svg:font-variant");
			if (temp != null)
				System.out.println("**** variant " + temp);
			temp = attibutes.get("svg:font-stretch");
			if (temp != null)
				System.out.println("**** stretch " + temp);
			temp = attibutes.get("svg:font-size");
			if (temp != null)
				System.out.println("**** size " + temp);
		}
	}
}
