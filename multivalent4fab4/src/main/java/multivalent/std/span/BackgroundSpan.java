/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.span;

import java.awt.Color;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.std.ui.DocumentPopup;
import phelps.awt.Colors;



/**
	Background span with editable color.

	@version $Revision: 1.7 $ $Date: 2008/09/08 09:15:29 $
 */
public class BackgroundSpan extends Span {
	/**
	Change the color to <tt>arg</tt>, which can be {@link java.lang.String} or {@link java.awt.Color}.
	<p><tt>"changeColor"</tt>: <tt>arg=</tt> <var>color, as String or Color</var>.
	 */
	public static final String MSG_CHANGE = "changeColor";

	/**
	Pop up dialog asking user color choice.
	<p><tt>"editColor"</tt>.
	 */
	public static final String MSG_EDIT = "editColor";


	/** List of color names, separated by spaces, such as "Yellow Orange Green Blue". */
	public static final String ATTR_COLORS = "colors";

	/** Color of background. */
	public static final String ATTR_COLOR = "color";

	public static final String ATTR_COLOR_DEF = "colorDef";


	protected static String[] choices_ = null;
	protected static String oldchoices_ = null;
	protected static Color defaultColor_ = Color.YELLOW; //Context.COLOR_INVALID;


	protected Color color_ = defaultColor_;

	private String colorName;

	private boolean selected;

	private boolean visible = true;
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/** Set to {@link Context#COLOR_INVALID} to invalidate, null for transparent. */
	public void setColor(Color color) { color_=color; }

	public Color getColor() { return color_; }

	public String getColorName() { return colorName; }

	@Override
	public boolean appearance(Context cx, boolean all) { 
		if (!visible)
			cx.background=null;
		else if (color_!=Context.COLOR_INVALID){
			if (!selected){
				cx.background=new Color(color_.getRed(),color_.getGreen(),color_.getBlue(),150);
				//cx.borderleft= cx.borderbottom = cx.borderright = cx.bordertop=;
				//cx.
			}
			else{ 
				cx.background=color_;
			}
		}

		return false; }




	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se,msg)) return true;
		if (this!=se.getIn()) return false;     // quick exit

		if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && isEditable()) {
			INode menu = (INode)se.getOut();
			Browser br = getBrowser();

			// be responsive to change in preferences
			String curchoices = getPreference(ATTR_COLORS, "Yellow Orange Green Blue");
			if (!curchoices.equals(oldchoices_)) {
				choices_ = curchoices.split("\\s+");
				oldchoices_ = curchoices;
			}

			//createUI("button", "Edit Color...", new SemanticEvent(br, MSG_EDIT, null, this, null), menu, "EDIT", false);
			for (int i=0, imax=choices_.length; i<imax; i++) {
				String co = choices_[i];
				createUI("button", co, new SemanticEvent(br, MSG_CHANGE, co, this, null), menu, "EDIT", false); // true if same color
			}
		}

		//return super.semanticEventBefore(se,msg);  // local menu items go first -- unusual
		return false;   // try the other way
	}


	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (this!=se.getIn()) {
			// catch super.sEA() at bottom
		} else if (MSG_CHANGE==msg) {
			Object arg = se.getArg();
			Color newcolor = null;

			if (arg==null) {
				// ask user => null is valid
			} else if (arg instanceof Color) newcolor=(Color)arg;
			else if (arg instanceof String) newcolor = Colors.getColor((String)arg);

			if (newcolor!=null) {
				defaultColor_ = color_ = newcolor;
				repaint();  // don't need getBrowser().repaint();
			}

		} else if (MSG_EDIT==msg) {
			// dialog
		}

		return super.semanticEventAfter(se,msg);
	}

	public static final String defineColor(Color c){
		String r = Integer.toHexString(c.getRed());
		String g = Integer.toHexString(c.getGreen());
		String b = Integer.toHexString(c.getBlue());
		if (r.length()<2)
			r="0"+r;
		if (g.length()<2)
			g="0"+g;
		if (b.length()<2)
			b="0"+b;
		return "#" + r + g + b;



	}

	@Override
	public ESISNode save() {
		if (color_==null /*|| it's the system default */) removeAttr(ATTR_COLOR);
		else {
			putAttr(ATTR_COLOR, Colors.getName(color_));
			putAttr(ATTR_COLOR_DEF, defineColor(color_));
		}

		return super.save();
	}


	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		super.restore(n,attr,layer);
		Color c2 = null;
		String cs = getAttr(ATTR_COLOR_DEF);
		if (cs==null){
			cs = getAttr("colordef");
		}
		if (cs!=null){
			cs = cs.replaceAll("0", "00");
			c2 = Colors.getColor(cs);
		}
		if (c2 == null)
			c2 = defaultColor_;
		color_ = Colors.getColor(getAttr(ATTR_COLOR), c2);
		colorName = getAttr(ATTR_COLOR);
	}


	@Override
	public String toString() {
		//return getName();//+"{"+start+".."+end+"}";
		return "background="+color_;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
