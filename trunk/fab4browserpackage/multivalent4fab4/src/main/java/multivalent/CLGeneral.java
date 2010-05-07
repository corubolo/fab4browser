package multivalent;

import java.awt.Color;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.pt.awt.NFont;



/**
<!--
used in style sheets, and has to be generic so can handle any settings from ASCII specifications
-->

	Generic context modifier, for runtime creation of new spans and structure, as by style sheets.
	Not as efficient as special-purpose spans, such as multivalent.std.span.BoldSpan.
	Use: client creates new GenericContextListener, sets desired parameters, uses like span or structure.
	Handles all appearance and priority, but nothing about event bindings (need scripting language).
	Don't need accessors for everything now that assuming everything affectsLayout().

	<p>Used frequently by style sheets, same as CLGeneral except priority set to structural priority
	and box-only attributes such as margin and padding.
	INode responsible for slipping into/out of Context base__ as enter/exit.

	<p>CLGeneral should inherit from CLGeneral because it adds structural attributes: margin, border, padding, ....
	But CLGeneral should inherit from CLGeneral because it doesn't want the Span fields and methods.

	<p>Different style sheets should subclass multivalent.StyleSheet,
	and their generic ContextListeners should subclass this class,
	in both cases so that other behaviors can in various situations
	get an object of type StyleSheet or type CLGeneral and make hay.

	@version $Revision: 1.5 $ $Date: 2003/06/02 04:58:39 $
 */
public class CLGeneral implements ContextListener /*EventListener--Java 1.1*/ {
	// make sure keep synched with Context attributes/fields
	//Context cx = new Context();	  // just use the values, not the methods
	public double zoom_;		 // or pagewise?
	//public int x_, baseline_;			// current point a la PostScript (OUT)
	public int xdelta_, ydelta_;	// {sub,super}scripts, opening gaps for spliced in text
	public Color xor_;
	public Color foreground_;     // aka fillColor_
	public Color background_;
	public Color strokeColor_;

	public float linewidth_;
	public int linecap_;
	public int linejoin_;
	public float miterlimit_;
	public float[] dasharray_;
	public float dashphase_;

	//public Color pagebackground_;	// special case
	public String family_; // font properties set independently of one another
	public float size_;
	public int flags_;
	public int weight_;
	public String texttransform_;

	public String display_;  // not CSS specific--fundamental

	public Color underline_, underline2_, overline_, overstrike_;
	public byte elide_;  // for Notemarks (blink makes set foreground color to background)
	//boolean sidebar_;	// how to implement?
	public int justify_;
	public int spaceabove_, spacebelow_;	// for use during spans to open up space for annotations

	public int marginleft;	// maybe move to GenericStruct => Span can usefully affect these, as ManualPageOutline's excerpt span squishes space between excerpts... but it doesn't use CLGeneral
	public int marginright;
	public int margintop;
	public int marginbottom;

	// maybe move border to Span to have boxed span
	public int borderleft;
	public int borderright;
	public int bordertop;
	public int borderbottom;

	public int paddingleft;
	public int paddingright;
	public int paddingtop;
	public int paddingbottom;

	byte align_, valign_;
	byte floats_;


	//public boolean opaque_;	  // settable by client
	protected int priority_; // = ContextListener.PRIORITY_STRUCT;

	public Map<Object,Object> signal_ = null;	// KEY NOT NORMALIZED!	Key not necessarily a string!


	// range on same node
	//	CLGeneral(Node n, int li, int ri) { this(n,li, n,ri, layer); }
	public CLGeneral() { this(ContextListener.PRIORITY_STRUCT); }
	public CLGeneral(int priority) { priority_=priority; invalidate(); }
	//public CLGeneral(Map map) { this(); attr_=map; flattenAttrs(); }

	public void invalidate() {
		// set all values to invalid settings
		zoom_ = 1.0;
		xdelta_ = ydelta_ = Context.INT_INVALID;
		xor_ = null; foreground_ = background_ = strokeColor_ = null;
		linewidth_ = Context.FLOAT_INVALID; linecap_ = Context.INT_INVALID; linejoin_ = Context.INT_INVALID; miterlimit_ = Context.FLOAT_INVALID;
		dasharray_ = Context.FLOATARRAY_INVALID; dashphase_ = Context.FLOAT_INVALID;
		family_ = null; flags_ = Context.INT_INVALID; weight_ = Context.INT_INVALID; size_ = Context.FLOAT_INVALID;
		display_ = texttransform_ = Context.STRING_INVALID;
		underline_ = underline2_ = overline_ = overstrike_ = Context.COLOR_INVALID;
		elide_ = Context.BOOL_INHERIT; justify_ = Context.INT_INVALID; spaceabove_ = spacebelow_ = Context.INT_INVALID;
		marginleft = marginright = margintop = marginbottom = Context.INT_INVALID;
		paddingleft = paddingright = paddingtop = paddingbottom = Context.INT_INVALID;
		borderleft = borderright = bordertop = borderbottom = Context.INT_INVALID;
		align_ = valign_ = floats_ = Node.ALIGN_INVALID;
		//signal = null;
		//opaque_ = false;
	}

	/** Interpret attributes and translate to fields. */
	void flattenAttrs() {
	}


	// settors
	public void setXdelta(int xdelta) { xdelta_ = xdelta; }
	public void setYdelta(int ydelta) { ydelta_ = ydelta; }
	public void setXor(Color xor) { xor_ = xor; }
	public void setForeground(Color foreground) { foreground_ = foreground; }
	public void setBackground(Color background) { background_ = background; }
	public void setStroke(Color stroke) { strokeColor_ = stroke; }
	//public void setFont(NFont font) { cx.font = font; }
	/** Convenience method for setFamily, setStyle, and setSize. */
	public void setFont(NFont font) { family_ = font.getFamily()/*.intern()*/; weight_ = font.getWeight(); flags_ = font.getFlags(); size_ = font.getSize(); }
	public void setFamily(String family) { family_ = family/*.intern()*/; }
	public void setWeight(int weight) { weight_=weight; }
	public void setFlags(int flags) { flags_=flags; }
	public void setSize(float size) { size_ = size; }
	public void setDisplay(String display) { display_=display; }
	public void setTextTransform(String transform) { texttransform_=transform!=null? transform.intern(): null; }
	public void setUnderline(Color underline) { underline_ = underline; }
	public void setUnderline2(Color underline2) { underline2_ = underline2; }
	public void setOverline(Color overline) { overline_ = overline; }
	public void setOverstrike(Color overstrike) { overstrike_ = overstrike; }
	public void setElide(byte elide) { elide_=elide; }
	// move justify to GenericStruct
	public void setJustify(int justify) { if (justify==Context.INT_INVALID || justify>=Node.LEFT /*&& justify<Node.LASTJUSTIFY*/) justify_ = justify; }
	public void setSpaceAbove(int spaceabove) { spaceabove_ = spaceabove; }
	public void setSpaceBelow(int spacebelow) { spacebelow_ = spacebelow; }


	//if (margin>=0 && margin < INode.INSETS.length? -- not as important to save space because not as many Generic Spans vs INodes (true?)
	public void setMargins(int margin) { marginleft = marginright = margintop = marginbottom = margin; }
	public void setMargins(Insets margins) { setMargins(margins.top, margins.left, margins.top, margins.bottom); }
	// same order as for Insets
	public void setMargins(int top, int left, int bottom, int right) { margintop=top; marginleft=left; marginbottom=bottom; marginright=right; }

	public void setBorder(int border) { borderleft = borderright = bordertop = borderbottom = border; }
	public void setBorder(Insets borders) { setBorder(borders.top, borders.left, borders.top, borders.bottom); }
	// same order as for Insets
	public void setBorder(int top, int left, int bottom, int right) { bordertop=top; borderleft=left; borderbottom=bottom; borderright=right; }

	public void setPadding(int padding) { paddingleft = paddingright = paddingtop = paddingbottom = padding; }
	public void setPadding(Insets padding) { setPadding(padding.top, padding.left, padding.top, padding.bottom); }
	// same order as for Insets
	public void setPadding(int top, int left, int bottom, int right) { paddingtop=top; paddingleft=left; paddingbottom=bottom; paddingright=right; }

	public void setAlign(byte align) { align_=align; }
	public void setVAlign(byte valign) { valign_=valign; }
	public void setFloats(byte floats) { floats_=floats; }


	// as well as
	//public void setOpaque(boolean opaque) { opaque_ = opaque; }

	public void setSignal(Object name, Object value) {
		if (signal_==null) signal_ = new HashMap<Object,Object>(2);
		signal_.put(name, value);
	}


	// getters
	public int getXdelta() { return xdelta_; }
	public int getYdelta() { return ydelta_; }
	public Color getXor() { return xor_; }
	public Color getForeground() { return foreground_; }
	public Color getBackground() { return background_; }
	public Color getstroke() { return strokeColor_; }
	//public Font getFont() { return font_; }   // may not be valid if subsequently set family, style or size separately
	public String getFamily() { return family_; }
	public int getWeight() { return weight_; }
	public int getFlags() { return flags_; }
	public float getSize() { return size_; }
	public String getDisplay() { return display_; }
	public String getTextTransform() { return texttransform_; }
	public Color getUnderline() { return underline_; }
	public Color getUnderline2() { return underline2_; }
	public Color getOverline() { return overline_; }
	public Color getOverstrike() { return overstrike_; }
	public byte getElide() { return elide_; }
	public int getJustify() { return justify_; }
	public int getSpaceAbove() { return spaceabove_; }
	public int getSpaceBelow() { return spacebelow_; }

	public byte getAlign() { return align_; }
	public byte getVAlign() { return valign_; }
	public byte getFloats() { return floats_; }

	// as well as
	public int getPriority() { return priority_; }
	public void setPriority(int priority) { priority_ = priority; }


	//public boolean getOpaque() { return opaque_; }



	/** All attributes or just inherited ones? */
	public boolean appearance(Context cx, boolean all) {
		// could keep track of number of changes to be made and have quick exit if ==0, but if ==0 why have span in first place?

		// foreach (attribute) { if (valid) set; }
		if (xdelta_!=Context.INT_INVALID) cx.xdelta = xdelta_;
		if (ydelta_!=Context.INT_INVALID) cx.ydelta = ydelta_;
		if (xor_!=null) cx.xor = xor_;
		if (foreground_!=null) cx.foreground = foreground_;
		if (background_!=null) cx.background = background_;
		if (strokeColor_!=null) cx.strokeColor = strokeColor_;

		if (linewidth_!=Context.FLOAT_INVALID) cx.linewidth = linewidth_;
		if (linecap_!=Context.INT_INVALID) cx.linecap = linecap_;
		if (linejoin_!=Context.INT_INVALID) cx.linejoin = linejoin_;
		if (miterlimit_!=Context.FLOAT_INVALID) cx.miterlimit = miterlimit_;
		if (dasharray_!=Context.FLOATARRAY_INVALID) cx.dasharray = dasharray_;
		if (dashphase_!=Context.FLOAT_INVALID) cx.dashphase = dashphase_;

		// NO!	if (cx.font!=null) cx.font = cx.font;
		if (family_!=null) cx.family = family_;
		if (weight_!=Context.INT_INVALID) cx.weight = weight_;
		if (flags_!=Context.INT_INVALID) cx.flags |= flags_;    // cx.style = style_;   ?
		if (size_!=Context.FLOAT_INVALID) cx.size = size_;
		if (display_!=Context.STRING_INVALID) cx.display = display_;
		if (texttransform_!=Context.STRING_INVALID) cx.texttransform = texttransform_;
		if (underline_!=Context.COLOR_INVALID) //{
			cx.underline = underline_;
		//System.out.println("CLGeneral.appearance, underline="+underline_+", pri="+getPriority());}
		if (underline2_!=Context.COLOR_INVALID) cx.underline2 = underline2_;
		if (overline_!=Context.COLOR_INVALID) cx.overline = overline_;
		if (overstrike_!=Context.COLOR_INVALID) cx.overstrike = overstrike_;
		if (elide_!=Context.BOOL_INVALID && elide_!=Context.BOOL_INHERIT) cx.elide = elide_==Context.BOOL_TRUE; //System.out.println("gen elide="+elide_); }
		if (justify_!=Context.INT_INVALID) cx.justify = justify_;
		if (spaceabove_!=Context.INT_INVALID) cx.spaceabove = spaceabove_;
		if (spacebelow_!=Context.INT_INVALID) cx.spacebelow = spacebelow_;

		if (all) {  // attributes that are not inherited
			if (marginleft!=Context.INT_INVALID) cx.marginleft = marginleft;
			if (marginright!=Context.INT_INVALID) cx.marginright = marginright;
			if (margintop!=Context.INT_INVALID) cx.margintop = margintop;
			if (marginbottom!=Context.INT_INVALID) cx.marginbottom = marginbottom;

			if (paddingleft!=Context.INT_INVALID) cx.paddingleft = paddingleft;
			if (paddingright!=Context.INT_INVALID) cx.paddingright = paddingright;
			if (paddingtop!=Context.INT_INVALID) cx.paddingtop = paddingtop;
			if (paddingbottom!=Context.INT_INVALID) cx.paddingbottom = paddingbottom;

			if (borderleft!=Context.INT_INVALID) cx.borderleft = borderleft;
			if (borderright!=Context.INT_INVALID) cx.borderright = borderright;
			if (bordertop!=Context.INT_INVALID) cx.bordertop = bordertop;
			if (borderbottom!=Context.INT_INVALID) cx.borderbottom = borderbottom;

			if (align_!=Node.ALIGN_INVALID) cx.align=align_;
			//if (align_!=Node.ALIGN_INVALID) System.out.println("CLGeneral setting align to "+align_);
			if (valign_!=Node.ALIGN_INVALID) cx.valign=valign_;
			if (floats_!=Node.ALIGN_INVALID) cx.floats=floats_;
		}

		//if (attrs!=null) {
		// iterate over attrs, adding to attrs
		//}
		if (signal_!=null)
			for (Entry<Object, Object> e : signal_.entrySet()) {
				//Object key=i.next(), val=signal_.get(key); -- faster?
				//if (val==INVALIDATESIGNAL) cx.signal.remove(key); else cx.signal.
				cx.signal.put(e.getKey(), e.getValue());
				//System.out.println("setting signal "+key+"="+val);
			}

		//return opaque_;
		return false;
	}


	/** All attributes or just inherited ones? */
	public void copyInto(CLGeneral dest) {
		// could keep track of number of changes to be made and have quick exit if ==0, but if ==0 why have span in first place?

		// foreach (attribute) { if (valid) set; }
		if (xdelta_!=Context.INT_INVALID) dest.xdelta_ = xdelta_;
		if (ydelta_!=Context.INT_INVALID) dest.ydelta_ = ydelta_;
		if (xor_!=null) dest.xor_ = xor_;
		if (foreground_!=null) dest.foreground_ = foreground_;
		if (background_!=null) dest.background_ = background_;
		if (strokeColor_!=null) dest.strokeColor_ = strokeColor_;
		// NO!	if (dest.font!=null) dest.font = dest.font;
		if (family_!=null) dest.family_ = family_;
		if (weight_!=Context.INT_INVALID) dest.weight_ = weight_;
		if (flags_!=Context.INT_INVALID) dest.flags_ = flags_;
		if (size_!=Context.FLOAT_INVALID) dest.size_ = size_;
		if (display_!=Context.STRING_INVALID) dest.display_ = display_;
		if (texttransform_!=Context.STRING_INVALID) dest.texttransform_ = display_;
		if (underline_!=Context.COLOR_INVALID) dest.underline_ = underline_;
		if (underline2_!=Context.COLOR_INVALID) dest.underline2_ = underline2_;
		if (overline_!=Context.COLOR_INVALID) dest.overline_ = overline_;
		if (overstrike_!=Context.COLOR_INVALID) dest.overstrike_ = overstrike_;
		if (elide_!=Context.BOOL_INVALID) dest.elide_ = elide_; //System.out.println("gen elide="+elide_); }
		if (justify_!=Context.INT_INVALID) dest.justify_ = justify_;
		if (spaceabove_!=Context.INT_INVALID) dest.spaceabove_ = spaceabove_;
		if (spacebelow_!=Context.INT_INVALID) dest.spacebelow_ = spacebelow_;


		// attributes that are not inherited
		//if (all) {
		if (marginleft!=Context.INT_INVALID) dest.marginleft = marginleft;
		if (marginright!=Context.INT_INVALID) dest.marginright = marginright;
		if (margintop!=Context.INT_INVALID) dest.margintop = margintop;
		if (marginbottom!=Context.INT_INVALID) dest.marginbottom = marginbottom;

		if (paddingleft!=Context.INT_INVALID) dest.paddingleft = paddingleft;
		if (paddingright!=Context.INT_INVALID) dest.paddingright = paddingright;
		if (paddingtop!=Context.INT_INVALID) dest.paddingtop = paddingtop;
		if (paddingbottom!=Context.INT_INVALID) dest.paddingbottom = paddingbottom;

		if (borderleft!=Context.INT_INVALID) dest.borderleft = borderleft;
		if (borderright!=Context.INT_INVALID) dest.borderright = borderright;
		if (bordertop!=Context.INT_INVALID) dest.bordertop = bordertop;
		if (borderbottom!=Context.INT_INVALID) dest.borderbottom = borderbottom;

		if (align_!=Node.ALIGN_INVALID) dest.align_=align_;
		if (valign_!=Node.ALIGN_INVALID) dest.valign_=valign_;
		if (floats_!=Node.ALIGN_INVALID) dest.floats_=floats_;
		//}

		//if (attrs!=null) {
		// iterate over attrs, adding to attrs
		//}
		if (signal_!=null)
			for (Entry<Object, Object> e : signal_.entrySet()) {
				//Object key=i.next(), val=signal_.get(key);
				//if (val==INVALIDATESIGNAL) dest.signal.remove(key); else dest.signal.
				dest.signal_.put(e.getKey(), e.getValue());
				//System.out.println("setting signal "+key+"="+val);
			}
	}


	/**
	Are all attributes equal to those of another CLGeneral?
	Can see if any fields have been set by comparing to a known-unmodified ContextSpan.
	 */
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof CLGeneral)) return false;
		CLGeneral s = (CLGeneral)o;

		return zoom_ == s.zoom_
		//&& x_==s.x_
		//&& baseline_ == s.baseline_
		&& xdelta_ == s.xdelta_ && ydelta_ == s.ydelta_
		&& xor_ == s.xor_
		&& foreground_==s.foreground_ && background_==s.background_ && strokeColor_==s.strokeColor_ // && pagebackground_==s.pagebackground_
		&& family_==s.family_ && size_==s.size_ && weight_==s.weight_ && flags_==s.flags_
		&& display_==s.display_ && texttransform_==s.texttransform_
		&& underline_==s.underline_ && underline2_==s.underline2_
		&& overline_==s.overline_ && s.overstrike_==s.overstrike_
		&& elide_==s.elide_ && justify_==s.justify_
		&& spaceabove_==s.spaceabove_ && spacebelow_==s.spacebelow_

		&& marginleft==s.marginleft && marginright==s.marginright && margintop==s.margintop && marginbottom==s.marginbottom
		&& borderleft==s.borderleft && borderright==s.borderright && bordertop==s.bordertop && borderbottom==s.borderbottom
		&& paddingleft==s.paddingleft && paddingright==s.paddingright && paddingtop==s.paddingtop && paddingbottom==s.paddingbottom

		&& align_ == s.align_
		&& valign_ == s.valign_
		&& floats_ == s.floats_;
	}

	@Override
	public int hashCode() { return System.identityHashCode(this); }

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("CLGeneral");
		//if (underline_!=Context.COLOR_INVALID) sb.append(underline_);

		if (family_!=null) sb.append('/').append(family_);
		if (weight_!=Context.INT_INVALID) sb.append("/weight");
		if (flags_!=Context.INT_INVALID) sb.append("/flags");
		if (size_!=Context.FLOAT_INVALID) sb.append('/').append(size_);
		if (display_!=null) sb.append("/"+display_);
		if (texttransform_!=null) sb.append("/"+texttransform_);
		if (underline_!=Context.COLOR_INVALID) sb.append("/underline");
		if (elide_!=Context.BOOL_INVALID) sb.append("/elide="+elide_);
		if (spaceabove_!=Context.INT_INVALID) sb.append("/spaceabove");
		if (foreground_!=null) sb.append("/fg");
		if (background_!=null) sb.append("/bg");
		if (strokeColor_!=null) sb.append("/stroke");
		if (margintop!=Context.INT_INVALID || marginleft!=Context.INT_INVALID || marginbottom!=Context.INT_INVALID || marginright!=Context.INT_INVALID) sb.append("/mar="+margintop);
		if (paddingtop!=Context.INT_INVALID || paddingleft!=Context.INT_INVALID || paddingbottom!=Context.INT_INVALID || paddingright!=Context.INT_INVALID) sb.append("/pad");
		if (bordertop!=Context.INT_INVALID || borderleft!=Context.INT_INVALID || borderbottom!=Context.INT_INVALID || borderright!=Context.INT_INVALID) sb.append("/bor");

		sb.append("/pri=").append(priority_);
		return sb.substring(0);
	}

	// need subclassing or scripting language for bindings
	// but if subclass, don't need generic appearance method

}
