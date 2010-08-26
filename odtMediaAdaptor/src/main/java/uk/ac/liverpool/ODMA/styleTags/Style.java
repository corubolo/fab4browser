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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import multivalent.CLGeneral;
import multivalent.Node;
import phelps.util.Units;

import com.pt.awt.NFont;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * Style
 * 
 * everything goes to pixels here. The reason is explained down here....
 * 
 * 
 * ****************************************************************
 * important note:
 * *****************************************************************
 * 
 * In Java, by default, Graphics draws at screenres (pixel to pixel),
 * so if you draw a line long one inch, 72 pixels , and screenres is not 72DPI (common on windows and linux)
 * you get line long exactly 72 pixels, not one inch.
 * 
 * Example: screenres 85 DPI
 * 
 * To get the right size, two ways:
 * 
 * 1) get the screen res and adapt (like: line long 1 inch = 1*screenres
 *    so 1*85 = 85
 * 2) apply the Units.getLenght to convert to pixels. (same)
 * 3) when drawing, use (it's better to back up the graphics transformation 1st)
 * 	  GraphicsConfiguration gc = g.getGraphicsConfiguration();
      g.setTransform(gc.getDefaultTransform());
      g.transform(gc.getNormalizingTransform());
      because:
      gc.getNormalizingTransform():
      Returns a AffineTransform that can be concatenated with the default AffineTransform
      of a GraphicsConfiguration so that 72 units in user space equals 1 inch in device space.
 * 
 * 
 * MV fonts (and Java ones) specify size in pixels
 * ***********************************************
 * 
 * 1) in java.awt.Font size is in pixel actually, using the same pixel size as
 * in MV brings the same exact result!
 * 
 * 2) On linux, the 3 methods (java, MV and OO) bring the same results in
 * proportion, but OO 2 seem to believe that screen resolution is higher! using
 * a ruler, I found out that java (at least in this particular case) is right
 * and reports the precise screen res.
 * 
 * 3) On windows, OO, java and MV show the same size (oo in points, for java and
 * MV I have to convert in pixels as before) so the screen res used is the same
 * for the three of them.
 * 
 * Summing up: Linux - Java and MV use 86dpi, OO uses 95
 * Windows - java, MV and OO all use the same screen res.
 * 
 */
public class Style {

	public static final int DEFAULT = 0;

	public static final int STYLE = 1;

	public static final int AUTOMATIC = 2;

	public static final int PAGE = 3;

	public static final int MASTERPAGE = 4;

	public static final int UNDEFINED_DEFAULT_OUTLINE_LEVEL = -1;

	public int type = -1;

	public String name;

	public String family;

	public String parent;

	public String nextStyleName;

	public String displayName;

	public String listStyleName;

	public String masterPageName;

	public int defaultOutlineLevel = -1;

	public Map<String, String> textProperties;

	public Map<String, String> paragraphProperties;

	public Map<String, String> graphicProperties;

	/* page layout related */
	public String headerStyle;

	public String footerStyle;

	public String pageUsage;

	public Map<String, String> pageLayoutProperties;

	public Map<String, String> pageHeaderProperties;

	public Map<String, String> pageFooterProperties;

	private CLGeneral clg;

	/* master page related */
	public String pageLayoutName;

	public boolean copied = false;

	public Map<String, FontDeclaration> fontDecl;

	/**
	 * 
	 */
	public Style(Map<String, FontDeclaration> m) {
		fontDecl = m;
	}



	/**
	 * returns true if this style hass all the properties of its ancerstors
	 * allready inside
	 */
	public boolean isComplete() {
		if (copied || type == Style.DEFAULT || type == Style.PAGE)
			return true;
		return false;
	}

	// FIXME: add the other attributes

	public String getId() {
		return (family == null ? "" : family) + "\""
		+ (name == null ? "" : name);
	}

	public String getParentId() {
		if (type == Style.DEFAULT)
			return null;
		return (family == null ? "" : family) + "\""
		+ (parent == null ? "" : parent);
	}

	/**
	 * Merges this style with the specified one
	 * 
	 */
	/**
	 * @return Returns the clg.
	 */
	public CLGeneral getClg() {
		if (clg == null) {
			clg = new CLGeneral();
			stilizza(this, clg);
		}
		return clg;
	}

	public static String getId(String name, String family) {
		return (family == null ? "" : family) + "\""
		+ (name == null ? "" : name);
	}

	/**
	 * @param st
	 * @param clg
	 */
	public static void stilizza(Style st, CLGeneral clg) {
		String t;
		if (st.paragraphProperties != null) {
			margins(st.paragraphProperties, clg);
			paddings(st.paragraphProperties, clg);
			t = st.paragraphProperties.get("fo:text-align");
			if (t != null) {
				if (t.equals("center"))
					clg.setAlign(Node.CENTER);
				if (t.equals("left"))
					clg.setAlign(Node.LEFT);
				if (t.equals("right"))
					clg.setAlign(Node.RIGHT);
			}
		}
		if (st.graphicProperties != null) {
			margins(st.graphicProperties, clg);
			paddings(st.graphicProperties, clg);

		}

		if (st.textProperties != null)
			try {
				String w = st.textProperties.get("fo:font-weight");
				if (w != null)
					if (w.equals("bold"))
						clg.weight_ = NFont.WEIGHT_BOLD;
				w = st.textProperties.get("fo:font-style");
				if (w != null)
					if (w.equals("italic"))
						clg.flags_ = NFont.FLAG_ITALIC;
				w = st.textProperties.get("fo:color");
				if (w != null)
					clg.foreground_ = Color.decode(w);
				w = st.textProperties.get("style:font-name");
				if (w == null)
					w = st.textProperties.get("style:font-name-complex");
				if (w != null) {
					FontDeclaration fd = st.fontDecl.get(w);
					clg.family_ = fd.family;
				}
				w = st.textProperties.get("fo:font-size");
				if (w != null)
					clg.size_ = (float) Units.getLength(w, "px");
				// System.out.println(clg.size_);

			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/**
	 * @param clg
	 */
	private static void margins(Map<String, String> props, CLGeneral clg) {
		String t;
		t = props.get("fo:margin-top");
		if (t != null)
			clg.margintop = (int) Units.getLength(t, "px");
		t = props.get("fo:margin-bottom");
		if (t != null)
			clg.marginbottom = (int) Units.getLength(t, "px");
		t = props.get("fo:margin-left");
		if (t != null)
			clg.marginleft = (int) Units.getLength(t, "px");
		t = props.get("fo:margin-right");
		if (t != null)
			clg.marginright = (int) Units.getLength(t, "px");
	}

	private static void paddings(Map<String, String> props, CLGeneral clg) {
		String t;
		t = props.get("fo:padding-left");
		if (t != null)
			clg.paddingleft = (int) Units.getLength(t, "px");
		t = props.get("fo:padding-right");
		if (t != null)
			clg.paddingright = (int) Units.getLength(t, "px");
		t = props.get("fo:padding-top");
		if (t != null)
			clg.paddingtop = (int) Units.getLength(t, "px");
		t = props.get("fo:padding-bottom");
		if (t != null)
			clg.paddingbottom = (int) Units.getLength(t, "px");
		t = props.get("fo:padding");
		if (t != null) {
			int pad = (int) Units.getLength(t, "px");
			clg.paddingbottom = clg.paddingleft = clg.paddingright = clg.paddingtop = pad;
		}
	}

	public void merge(Style prnt) {
		// TODO: see if we must copy also the style attributes or only the
		// properties
		// FIXME: Fix the merge taking into account % and other features (tab
		// stops etc.)
		if (prnt == null)
			return;

		if (prnt.textProperties != null && textProperties == null)
			textProperties = new HashMap<String, String>();
		if (prnt.paragraphProperties != null && paragraphProperties == null)
			paragraphProperties = new HashMap<String, String>();
		if (prnt.graphicProperties != null && graphicProperties == null)
			graphicProperties = new HashMap<String, String>();
		if (prnt.pageLayoutProperties != null && pageLayoutProperties == null)
			pageLayoutProperties = new HashMap<String, String>();
		if (prnt.pageHeaderProperties != null && pageHeaderProperties == null)
			pageHeaderProperties = new HashMap<String, String>();
		if (prnt.pageFooterProperties != null && pageFooterProperties == null)
			pageFooterProperties = new HashMap<String, String>();
		// System.out.println("==========");
		// System.out.println(name+"->"+prnt.name+((prnt.type==DEFAULT)?"Default!":""));
		margeMap(textProperties, prnt.textProperties);
		margeMap(paragraphProperties, prnt.paragraphProperties);
		margeMap(graphicProperties, prnt.graphicProperties);
		margeMap(pageLayoutProperties, prnt.pageLayoutProperties);
		margeMap(pageHeaderProperties, prnt.pageHeaderProperties);
		margeMap(pageFooterProperties, prnt.pageFooterProperties);
		// System.out.println("__________");
	}

	/* p2 is the parent */
	private static void margeMap(Map<String, String> child,
			Map<String, String> parent) {
		if (parent == null)
			return;

		for (Entry<String, String> par : parent.entrySet())
			if (!child.containsKey(par.getKey()))
				child.put(par.getKey(), par.getValue());
		// System.out.println(par.getKey()+"="+par.getValue());
			else {
				String pcnt = par.getValue();
				String ccnt = child.get(par.getKey());
				try {
					if (par.getKey().equals("fo:font-size")
							&& ccnt.charAt(ccnt.length() - 1) == '%') {
						float sz = (float) Units.getLength(pcnt, "pt");
						float pc = Integer.parseInt(ccnt.substring(0, ccnt
								.length() - 1));
						pc /= 100.0f;
						sz = sz * pc;
						child.put(par.getKey(), Float.toString(sz) + "pt");
					}
				} catch (Exception e1) {
					System.out.println();
					System.out.print(pcnt);
					System.out.println(" " + ccnt);
				}
			}
	}
}
