package multivalent.std.adaptor;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Color;

import phelps.lang.Strings;
import phelps.lang.Integers;
import phelps.awt.Colors;

import com.pt.awt.NFont;
import com.pt.awt.font.NFontManager;

import multivalent.StyleSheet;
import multivalent.Node;
import multivalent.ContextListener;
import multivalent.CLGeneral;
import multivalent.Context;
import multivalent.VObject;
import multivalent.std.span.HyperlinkSpan;



/**
	<a href="http://www.w3.org/Style/" target="supplemental">Cascading Style Sheet (CSS)</a> reader,
	with some CSS2.

	<p>For now, minimal support.
	Supported:
		&64;import
		<tag|CLASS-ID> { <color|font|...>:<value>; }</i>
		such as
		<pre>
		&lt;STYLE>
			.c1 { color:yellow; }
			.c2 { color:red; }
			.c3 { color:orange; }
		&lt;/STYLE>
		</pre>

	Not supported: nested selectors and everything else.


	<p>CSS has many attributes, which should be carried along in the graphics context.
	When asked about an attribute (even for its own use), a node looks at its local attributes,
	then the gc's, then the default.  But for now we just hardcode a couple special cases.


	<p>NOT supported:
	reference by LINK,
	contextual selectors: e.g, "OL OL", "H1.c1"
	pseudo classes, pseudo elements: e.g., "A:visited", "P:first-line", ...
	display property (which can be used to render XML directly, as it declares elements to be "block"/structural or "inline"/span)
	properties other than font and color

	<p>Since CSS is used by other specs, such as SVG, maybe just parse into name-value pairs
	and let clients put into native data types, if desired.

	general font/general color/underline/rest of Context properties


	@see CSSGeneral
	@see CSSContext

	@version $Revision: 1.9 $ $Date: 2004/11/04 05:27:55 $
*/
public class CSS extends StyleSheet {
  static final boolean DEBUG = false;

  protected static final int
	START_CSS=0,
	FONT=START_CSS, FONT_FAMILY=FONT+1, FONT_STYLE=FONT+2, FONT_VARIANT=FONT+3, FONT_WEIGHT=FONT+4, FONT_SIZE=FONT+5,
	COLOR=FONT_SIZE+1, BACKGROUND=COLOR+1, BACKGROUND_COLOR=BACKGROUND+1, BACKGROUND_IMAGE=BACKGROUND+2, BACKGROUND_REPEAT=BACKGROUND+3, BACKGROUND_ATTACHMENT=BACKGROUND+4, BACKGROUND_POSITION=BACKGROUND+5,
	WORD_SPACING=BACKGROUND_POSITION+1, LETTER_SPACING=WORD_SPACING+1, TEXT_DECORATION=LETTER_SPACING+1, VERTICAL_ALIGN=TEXT_DECORATION+1, TEXT_TRANSFORM=VERTICAL_ALIGN+1, TEXT_ALIGN=TEXT_TRANSFORM+1, TEXT_INDENT=TEXT_ALIGN+1, LINE_HEIGHT=TEXT_INDENT+1,
	MARGIN=LINE_HEIGHT+1, MARGIN_TOP=MARGIN+1, MARGIN_BOTTOM=MARGIN+2, MARGIN_LEFT=MARGIN+3, MARGIN_RIGHT=MARGIN+4,
	PADDING=MARGIN_RIGHT+1, PADDING_TOP=PADDING+1, PADDING_BOTTOM=PADDING+2, PADDING_LEFT=PADDING+3, PADDING_RIGHT=PADDING+4,
	BORDER_WIDTH=PADDING_RIGHT+1, BORDER_TOP_WIDTH=BORDER_WIDTH+1, BORDER_BOTTOM_WIDTH=BORDER_WIDTH+2, BORDER_LEFT_WIDTH=BORDER_WIDTH+3, BORDER_RIGHT_WIDTH=BORDER_WIDTH+4,
	BORDER_COLOR=BORDER_RIGHT_WIDTH+1, BORDER_STYLE=BORDER_COLOR+1,
	BORDER=BORDER_STYLE+1, BORDER_TOP=BORDER+1, BORDER_BOTTOM=BORDER+2, BORDER_LEFT=BORDER+3, BORDER_RIGHT=BORDER+4,
	WIDTH=BORDER_RIGHT+1, HEIGHT=WIDTH+1, FLOAT=HEIGHT+1, CLEAR=FLOAT+1,
	DISPLAY=CLEAR+1, WHITE_SPACE=DISPLAY+1,
	LIST_STYLE=WHITE_SPACE+1, LIST_STYLE_TYPE=LIST_STYLE+1, LIST_STYLE_IMAGE=LIST_STYLE+2, LIST_STYLE_POSITION=LIST_STYLE+3,
	END_CSS = LIST_STYLE_POSITION,

	// CSS2
	START_CSS2 = END_CSS+1,
	VISIBILITY = START_CSS2,
	END_CSS2 = VISIBILITY,

	// my own
	ELIDE = END_CSS2+1
	;

  protected static Map<String,Integer> keyword2int_;
  static {
	String[] keywords = {
	// Font
	"font", "font-family", "font-style", "font-variant", "font-weight", "font-size",
	// Color and background
	"color", "background", "background-color", "background-image", "background-repeat", "background-attachment", "background-position",
	// Text
	"word-spacing", "letter-spacing", "text-decoration", "vertical-align", "text-transform", "text-align", "text-indent", "line-height",
	// Box
	"margin", "margin-top", "margin-bottom", "margin-left", "margin-right",
	"padding", "padding-top", "padding-bottom", "padding-left", "padding-right",
	"border-width", "border-top-width", "border-bottom-width", "border-left-width", "border-right-width",
	"border-color", "border-style",
	"border", "border-top", "border-bottom", "border-left", "border-right",
	"width", "height", "float", "clear",
	// Classification
	"display", "white-space",
	"list-style", "list-style-type", "list-style-image", "list-style-position",

	// CSS2
	"visibility",

	// my own
	"elide",
	};

	keyword2int_ = new HashMap<String,Integer>(2 * keywords.length);
	for (int i=0,imax=keywords.length; i<imax; i++) keyword2int_.put(keywords[i], Integers.getInteger(i));

	assert keyword2int_.get("margin").intValue() == MARGIN;
	assert keyword2int_.get("border-style").intValue() == BORDER_STYLE;
	assert keyword2int_.get("list-style-position").intValue() == LIST_STYLE_POSITION;
	assert keyword2int_.get("elide").intValue() == ELIDE;
  }


  // Hashtable of GI-ContextListener pairs
  private static final int IDENT=0, ATKEYWORD=1, STRING=2, HASH=3, NUMBER=4, PERCENTAGE=5, DIMENSION=6, URL=7,
	RGB=8, UNICODERANGE=9, CDO=10, CDC=11, DELIM=12, WHITESPACE=13, COMMENT=14, ERROR=15,
	SEMICOLON=';', LBRACE='{', RBRACE='}', LPAR='(', RPAR=')', LBRACK='[', RBRACK=']';

  // special cases for hyperlink -- normalize ":link" as "*:link"?
  private static String[] astate2attr = { ":link", "a:link", ":visited", "a:visited", ":hover", "a:hover", ":active", "a:active" };


  //public CSS(StyleSheet cascade) { super(cascade); }
  private int[] nums_ = new int[10];
  private String[] units_ = new String[nums_.length];
  private Color[] colors_ = new Color[10];


  protected Context createContext() {
	return new CSSContext();
  }

  public void activesAdd(List<ContextListener> actives, VObject o, Node parent) {
	if (o==null) return;	// after cascade_....?  shouldn't happen so shouldn't matter
	String name = o.getName(), attr;
//if (name==null) System.out.println("null name in CSS actives add "+o);
	if (name==null) return;
	if (cascade_!=null) cascade_.activesAdd(actives, o, parent);

	ContextListener cl;

	// add in increasing order of priority as priority ties go to last added
	if (/*name!=null &&*/ (cl = get(name, parent)) !=null) {Context.priorityInsert(cl, actives);	// by GI
//System.out.println("add "+name+" = "+cl);
	}

	// can create a bunch of Strings, but not many id or class attrs... I think
	if ((attr=o.getAttr("class"))!=null) {
		if ((cl=get("."+attr, parent))!=null) Context.priorityInsert(cl, actives);
		if ((cl=get(name+"."+attr, parent))!=null) Context.priorityInsert(cl, actives);
	}

	// pseudo elements
	if ("a".equals(name) && o instanceof HyperlinkSpan/*hack*/) {	// "a:link", ... in addition to plain "a"
		HyperlinkSpan hs = (HyperlinkSpan)o;
//System.out.println("a => "+hs.getState()+" on "+hs.getTarget());
		int state = hs.getState() * 2;
		if ((cl=get(astate2attr[state], parent))!=null) Context.priorityInsert(cl, actives);
		if ((cl=get(astate2attr[state+1], parent))!=null) Context.priorityInsert(cl, actives);
	} else if ("p".equals(name)) {	// p:first-line ...
	}


	if ((attr=o.getAttr("id"))!=null) {
		if ((cl=get("#"+attr, parent))!=null) Context.priorityInsert(cl, actives);
		if ((cl=get(name+"#"+attr, parent))!=null) Context.priorityInsert(cl, actives);
	}

	// if match both class and id, that's ok, just get duplicates and priority decided in ContextListener not at lookup

	if ((cl = get(o)) !=null) Context.priorityInsert(cl, actives);	// by handle -- last
//if ("span".equals(name)) System.out.println("SPAN "+o.getAttributes()+" => "+get(o));
  }


  public void activesRemove(List<ContextListener> actives, VObject o, Node parent) {
	if (o==null) return;	// after casecade_....?  shouldn't happen so shouldn't matter
	String name = o.getName(), attr;
	if (name==null) return;
	if (cascade_!=null) cascade_.activesRemove(actives, o, parent);

	ContextListener cl;

	if (/*name!=null &&*/ (cl = get(name, parent)) !=null) actives.remove(cl);	// by GI

	// can create a bunch of Strings, but not many id or class attrs... I think
	if ((attr=o.getAttr("class"))!=null) {
		if ((cl=get("."+attr, parent))!=null) actives.remove(cl);
		if ((cl=get(name+"."+attr, parent))!=null) actives.remove(cl);
	}

	if ("a".equals(name) && o instanceof HyperlinkSpan) {
		HyperlinkSpan hs = (HyperlinkSpan)o;
//System.out.println("a => "+hs.getState()+" on "+hs.getTarget());
		int state = hs.getState() * 2;
		if ((cl=get(astate2attr[state], parent))!=null) actives.remove(cl);
		if ((cl=get(astate2attr[state+1], parent))!=null) actives.remove(cl);
	} // first char ...

	if ((attr=o.getAttr("id"))!=null) {
		if ((cl=get("#"+attr, parent))!=null) actives.remove(cl);
		if ((cl=get(name+"#"+attr, parent))!=null) actives.remove(cl);
	}

	// if match both class and id, that's ok, just get duplicates and priority decided in ContextListener not at lookup

	if ((cl = get(o)) !=null) actives.remove(cl);	// by handle
  }


  /*
  public ContextListener get(VObject o, Node parent) {
	String key;
	ContextListener cx;
	String name = o.getName();

//System.out.println("lookup "+name);
	// stupid pseudo-classes fake out name for other guys
	//if (name==null) return;	?
//System.out.println("looking up "+n.getName()+", class="+n.getAttr("class")+", id="+n.getAttr("id"));
	// check for CLASS and ID attributes -- temporary implementation: should have own namespace, at least
	//if ((key=n.getAttr("id"))!=null && (cx=get("#"+key))!=null) return cx;
	//else if ((key=n.getAttr("class"))!=null && (cx=get("."+key))!=null) return cx;
//if (key!=null) System.out.println(cx);
/*	  if (key!=null && name2span!=null) {
		// dump name2span
		Set keys = name2span.keySet();
		Iterator<> dump = keys.iterator();
		//while (dump.hasNext()) { Object key=dump.next(); System.out.println("\tkey="+key+", value="+name2span.get(key)); }
	}* /
	/*else* /return super.get(o, parent);
  }*/


/*
  public void put(String name, ContextListener cx) {	// special case
	// check for .<class> or #<id>
	/*if (name.startsWith(".")) { super.put(name.substring(1), "<CLASS>", cx);
	} else if (name.startsWith("#")) { super.put(name.substring(1), "<ID>", cx);
	} else* / super.put(name, cx);
  }*/
/*
  public void put(String name, List<> context, ContextListener cx) {
	// check for .<class> or #<id>
	/*if (name.startsWith(".")) { super.put(name.substring(1), "<CLASS>", context, cx);
	} else if (name.startsWith("#")) { super.put(name.substring(1), "<ID>", context, cx);
	} else* / //super.put(name, context, cx);
  }*/




/*  public int getLength(String val) {
	if (val==null) return 0;
	int len = 0;
	for (int i=0,imax=val.length(); i<imax; i++) {
		char ch = val.charAt(i);
		if (Character.isDigit(ch)) len = len*10 + ch - '0';
		else break;
	}
	// for now assume units is pixels
//System.out.println("len = "+len);
	return len;
  }*/
  public int getLength(String val) {
	int cnt = getLengths(val);
	//assert cnt==1;
	return nums_[0];
  }

  /** Returns count. */
  public int getLengths(String vals) {
	if (vals==null) return 0;

	int cnt = 0;
	char ch;
	StringBuffer sb = new StringBuffer(10);
	for (int i=0,imax=vals.length(); i<imax; cnt++) {
		while (i<imax && Character.isSpaceChar(vals.charAt(i))) i++;	// leading/interstitial whitespace
		// number
		int num = 0, start=i;
		while (i<imax && Character.isDigit(ch=vals.charAt(i))) { num = num * 10 + ch - '0'; i++; }
		if (start < i) nums_[cnt] = num; else break;

		while (i<imax && Character.isSpaceChar(vals.charAt(i))) i++;	// interstitial whitespace
		// unit
		sb.setLength(0); while (i<imax && !Character.isSpaceChar(ch=vals.charAt(i)) && !Character.isDigit(ch)) { sb.append(ch); i++; }
		if (sb.length() > 0) units_[cnt] = sb.substring(0); else break;
	}
	return cnt;
  }


  public int getBorderWidth(String val) {
	int cnt = getBorderWidths(val);
	//assert cnt==1;
	return nums_[0];
  }

  // thin | medium | thick | <length>
  /** Returns count. */
  public int getBorderWidths(String vals) {
	if (vals==null) return 0;

	int cnt = 0;
	char ch;
	//StringBuffer sb = new StringBuffer(10);
	for (int i=0,imax=vals.length(); i<imax && cnt<4; cnt++) {
		while (i<imax && Character.isSpaceChar(vals.charAt(i))) i++;	// leading/interstitial whitespace
		// number
		int num = 0, start=i;
		if (Character.isDigit(vals.charAt(i))) while (i<imax && Character.isDigit(ch=vals.charAt(i))) { num = num * 10 + ch - '0'; i++; }
		else {
			while (i<imax && !Character.isSpaceChar(vals.charAt(i))) i++;
			String lc = vals.substring(start,i).toLowerCase();
			if ("thin".equals(lc)) num=2;
			else if ("medium".equals(lc)) num=5;
			else if ("thick".equals(lc)) num=7;
		}
//System.out.println("width: "+vals.substring(start,i)+" => "+num);
		if (start < i) nums_[cnt] = num; else break;
	}
	return cnt;
  }

  /** Returns count. */
  public int getBorderColors(String vals) {
	if (vals==null) return 0;

	int cnt = 0;
	for (int i=0,imax=vals.length(); i<imax && cnt<4; cnt++) {
		while (i<imax && Character.isSpaceChar(vals.charAt(i))) i++;	// leading/interstitial whitespace
		int start=i;
		while (i<imax && !Character.isSpaceChar(vals.charAt(i))) i++;	// color

		//if (start < i) colors_[cnt] = Colors.getColor(vals.substring(start,i), Context.COLOR_INHERIT); else break;
		if (start < i) colors_[cnt] = getColor(vals.substring(start,i)); else break;
//System.out.println("color #"+cnt+" = |"+vals.substring(start,i)+"| => "+colors_[cnt]);
	}
//System.out.println("cnt = "+cnt);
	return cnt;
  }



  // have to override get too, so can handle P.CODE, which requires knowledge of CLASS
  /**
	Convert passed attribute name and value into setting in CSSGeneral.
	CSSGeneral has space overhead, but have limited number of style sheet specs per document.
	Method also used to process STYLE attribute in arbitrary tags (which means that all HTML tags must be Generic*).
	Subclass CSSGeneral to handle CSS2.
  */
  public void setAttr(CSSGeneral gs, String name, String value) {
	StringTokenizer st;

	Object o = keyword2int_.get(name);
	int cmd = (o!=null? ((Integer)o).intValue(): -1);
	String lcval = (value!=null? value.toLowerCase(): null);
	int cnt;

	switch (cmd) {
	case FONT_FAMILY:	// "font" is more general--can be an image
	  // generic names: 'serif' (e.g. Times), 'sans-serif' (e.g. Helvetica), 'cursive' (e.g. Zapf-Chancery), 'fantasy' (e.g. Western), 'monospace' (e.g. Courier)
	  // Java guaranteed: "Dialog", "DialogInput", "Monospaced", "Serif", "SansSerif", "Symbol", "Lucida"?
	  st = new StringTokenizer(value, ",");	// comma-separated list of possible font names
	  while (st.hasMoreTokens()) {
		String fam=Strings.trim(Strings.trimWhitespace(st.nextToken()), "\""), lcfam=fam.toLowerCase();
//System.out.println("fam = |"+fam+"|");
		int flags = NFont.FLAG_NONE;
		if ("serif".equals(lcfam)) { fam="Times"; flags=NFont.FLAG_SERIF | NFont.FLAG_NONSYMBOLIC; }
		else if ("sans-serif".equals(lcfam)) { fam="Helvetica"; flags = NFont.FLAG_NONSYMBOLIC; }
		else if ("cursive".equals(lcfam)) { fam="ZapfChancery"; flags=NFont.FLAG_SCRIPT; }	//"ZapfChancery";	// Windoze name
		else if ("fantasy".equals(lcfam)) { fam="Western"; flags = NFont.FLAG_NONSYMBOLIC; }	// doesn't matter if matches
		else if ("monospace".equals(lcfam)) { fam="Courier"; flags=NFont.FLAG_FIXEDPITCH | NFont.FLAG_NONSYMBOLIC; }

		String efam = NFontManager.getDefault().getAvailableFamily(fam, flags);
		if (efam!=null) { gs.setFamily(efam); break; }
		//if (DEBUG) System.out.println("fam=>"+efam);
	  }
	  break;

	case FONT_STYLE:
		// I think Java 2D has a different way to do this than old AWT Font object
		// normal | italic | oblique
		if ("italic".equals(lcval) || "oblique".equals(lcval)) gs.setFlags(NFont.FLAG_ITALIC);
		else gs.setFlags(NFont.FLAG_NONE);
		break;

	case FONT_WEIGHT:
		// normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
		int newweight = -1;	// 400=normal, 700=bold
		if ("normal".equals(lcval)) newweight = NFont.WEIGHT_NORMAL;
		else if ("bold".equals(lcval)) newweight = NFont.WEIGHT_BOLD;
		else if ("bolder".equals(lcval)) newweight = NFont.WEIGHT_BLACK;
		else if ("lighter".equals(lcval)) newweight = NFont.WEIGHT_LIGHT;
		else if (Character.isDigit(value.charAt(0))) try { newweight = Integer.parseInt(value); } catch (NumberFormatException wnfe) {}
//System.out.println("weight = |"+lcval+"| => "+newwweight);
		gs.setWeight(0 < newweight&&newweight <= 1000? newweight: NFont.WEIGHT_NORMAL);	// Math.min(newweight, Math.max(0,newweight)) ?
		break;

	case FONT_VARIANT:
		// modify family, maybe
		break;

	case FONT_SIZE:
		float sizenow = gs.getSize();
		// <absolute-size>:	xx-small | x-small | small | medium | large | x-large | xx-large
		// <relative-size>: larger | smaller
		// => need access to table for this
		// <length>
		// <percentage>: 'em' and 'ex' length values refer to the font size of the current element
		if (value.endsWith("pt") || value.endsWith("pts")) {
			try {
				int sfxi = value.indexOf('p');
				float px = Float.parseFloat(value.substring(0,sfxi).trim());
				int ppi = java.awt.GraphicsEnvironment.isHeadless()? 72: java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
				gs.setSize(px * ppi / 72f);
//if (DEBUG) System.out.println("\tfont-size "+gs.getSize()+" points => "+(px*ppi/72f));
			} catch (NumberFormatException nfe) {}
		} else if (value.endsWith("px")) {
			try { gs.setSize(Float.parseFloat(value.substring(0,value.length()-2).trim())); } catch (NumberFormatException nfe) {}
		} else {
			if (DEBUG) System.out.println("\tfont-size relative to "+sizenow+": "+value);
		}
		break;

	case FONT:
		// short forms for everything
		st = new StringTokenizer(value.toLowerCase());
		while (st.hasMoreTokens()) {
			String val = st.nextToken();
			String subname = "font-family";
			if ("italic".equals(val) || "oblique".equals(val) || "normal".equals(val)) subname="font-style";
			else if ("normal".equals(val) || "small-caps".equals(val)) subname="font-variant";
			else if ("normal".equals(val) || "bold".equals(val) || "bolder".equals(val) || "lighter".equals(val) || (Character.isDigit(val.charAt(0)) && val.length()==3 && val.endsWith("00")) ) subname="font-weight";
			else if (Character.isDigit(val.charAt(0))) {
				int inx = val.indexOf('/');
				if (inx!=-1) {
					setAttr(gs, "line-height", val.substring(inx+1));
					val = val.substring(0,inx);
				}
				subname="font-size";
			}

			setAttr(gs, subname, val);
		}
		break;

	case COLOR:
		gs.setForeground(getColor(lcval));
//System.out.println("foreground 'lcval' = "+gs.getForeground());
//System.out.println("set color to "+value+"/"+fg);
		break;
	case BACKGROUND_COLOR:	// "background" is more general--can be an image
		gs.setBackground(getColor(lcval));
//System.out.println("set background-color to "+value+"/"+Colors.getColor(lcval));
		break;

	// pixels, em, percentage, "auto", ...
	case MARGIN_TOP:   gs.margintop = getLength(lcval); break;
	case MARGIN_BOTTOM:gs.marginbottom = getLength(lcval); break;
	case MARGIN_LEFT:  gs.marginleft = getLength(lcval); break;
	case MARGIN_RIGHT: gs.marginright = getLength(lcval); break;
	case MARGIN:
		cnt = getLengths(lcval);
//System.out.println("cnt = "+cnt+"  "+nums_[0]+"/"+units_[0]);
		if (cnt==1) gs.margintop = gs.marginbottom = gs.marginleft = gs.marginright = nums_[0];
		else if (cnt==2) { gs.margintop = gs.marginbottom = nums_[0]; gs.marginleft = gs.marginright = nums_[1]; }
		else if (cnt==3) { gs.margintop=nums_[0]; gs.marginleft = gs.marginright = nums_[1]; gs.marginbottom=nums_[2]; }
		else if (cnt>=4) { gs.margintop=nums_[0]; gs.marginright=nums_[1]; gs.marginbottom=nums_[2]; gs.marginleft=nums_[3];  }
		break;

	case PADDING_TOP:   gs.paddingtop = getLength(lcval); break;
	case PADDING_BOTTOM:gs.paddingbottom = getLength(lcval); break;
	case PADDING_LEFT:  gs.paddingleft = getLength(lcval); break;
	case PADDING_RIGHT: gs.paddingright = getLength(lcval); break;
	case PADDING:
		cnt = getLengths(lcval);
		if (cnt==1) gs.paddingtop = gs.paddingbottom = gs.paddingleft = gs.paddingright = nums_[0];
		else if (cnt==2) { gs.paddingtop = gs.paddingbottom = nums_[0]; gs.paddingleft = gs.paddingright = nums_[1]; }
		else if (cnt==3) { gs.paddingtop=nums_[0]; gs.paddingleft = gs.paddingright = nums_[1]; gs.paddingbottom=nums_[2]; }
		else if (cnt>=4) { gs.paddingtop=nums_[0]; gs.paddingright=nums_[1]; gs.paddingbottom=nums_[2]; gs.paddingleft=nums_[3];  }
		break;

	case BORDER_TOP_WIDTH: gs.bordertop = getBorderWidths(lcval); break;
	case BORDER_BOTTOM_WIDTH: gs.borderbottom = getBorderWidth(lcval); break;
	case BORDER_LEFT_WIDTH: gs.borderleft = getBorderWidth(lcval); break;
	case BORDER_RIGHT_WIDTH: gs.borderright = getBorderWidth(lcval); break;
	case BORDER_WIDTH:
		cnt = getBorderWidths(lcval);
		if (cnt==1) gs.bordertop = gs.borderbottom = gs.borderleft = gs.borderright = nums_[0];
		else if (cnt==2) { gs.bordertop = gs.borderbottom = nums_[0]; gs.borderleft = gs.borderright = nums_[1]; }
		else if (cnt==3) { gs.bordertop=nums_[0]; gs.borderleft = gs.borderright = nums_[1]; gs.borderbottom=nums_[2]; }
		else if (cnt>=4) { gs.bordertop=nums_[0]; gs.borderright=nums_[1]; gs.borderbottom=nums_[2]; gs.borderleft=nums_[3];  }
		break;

	case BORDER_COLOR:
		cnt = getBorderColors(lcval);
		if (cnt==1) gs.cbordertop = gs.cborderbottom = gs.cborderleft = gs.cborderright = colors_[0];
		else if (cnt==2) { gs.cbordertop = gs.cborderbottom = colors_[0]; gs.cborderleft = gs.cborderright = colors_[1]; }
		else if (cnt==3) { gs.cbordertop=colors_[0]; gs.cborderleft = gs.cborderright = colors_[1]; gs.cborderbottom=colors_[2]; }
		else if (cnt>=4) { gs.cbordertop=colors_[0]; gs.cborderright=colors_[1]; gs.cborderbottom=colors_[2]; gs.cborderleft=colors_[3];  }
		break;

	case BORDER_STYLE:
		// none | dotted | dashed | solid | double | groove | ridge | inset | outset
		gs.borderstyle = lcval;//.intern()?
		break;

	case BORDER:
		break;
	case BORDER_TOP:
		break;
	case BORDER_BOTTOM:
		break;
	case BORDER_LEFT:
		break;
	case BORDER_RIGHT:
		// width, style and color
		break;

	case WIDTH:
		break;
	case HEIGHT:
		break;

	case BACKGROUND:
		gs.setBackground(getColor(lcval));	// just color for now
		break;
	case BACKGROUND_IMAGE:
		break;
	case BACKGROUND_REPEAT:
		break;
	case BACKGROUND_ATTACHMENT:
		break;
	case BACKGROUND_POSITION:
		break;

	case WORD_SPACING:
		break;
	case LETTER_SPACING:
		break;

	case TEXT_DECORATION:
		// none | [ underline || overline || line-through || blink ] | inherit
		//Color fg = gs.getForeground();
		if ("underline".equals(lcval)) gs.setUnderline(Context.COLOR_INHERIT);
		else if ("overline".equals(lcval)) gs.setOverline(Context.COLOR_INHERIT);
		else if ("line-through".equals(lcval)) gs.setOverstrike(Context.COLOR_INHERIT);
		// else if ("blink"
		else if ("none".equals(lcval)) { gs.setUnderline(Context.COLOR_INVALID); gs.setOverline(Context.COLOR_INVALID); gs.setOverstrike(Context.COLOR_INVALID); }
		break;

	case VERTICAL_ALIGN:
		// baseline | sub | super | top | text-top | middle | bottom | text-bottom | <percentage> | <length> | inherit
		if ("top".equals(lcval)) gs.setAlign(Node.TOP);
		else if ("middle".equals(lcval)) gs.setAlign(Node.MIDDLE);
		else if ("bottom".equals(lcval)) gs.setAlign(Node.BOTTOM);
		else if ("baseline".equals(lcval)) gs.setAlign(Node.BASELINE);
		//else if ("inherit".equals(lcval)) ...
//System.out.println("CSS text-align: |"+lcval+"| => "+gs.getAlign());
		break;

	case TEXT_TRANSFORM:
		// capitalize | uppercase | lowercase | none
		// CSS2: ... | inherit
		gs.setTextTransform(lcval.intern());
		break;

	case TEXT_ALIGN:
		// left | right | center | justify | <string> | inherit
		if ("left".equals(lcval)) gs.setAlign(Node.LEFT);
		else if ("right".equals(lcval)) gs.setAlign(Node.RIGHT);
		else if ("center".equals(lcval)) gs.setAlign(Node.CENTER);
		else if ("justify".equals(lcval)) gs.setAlign(Node.JUSTIFY);
		//else if ("inherit".equals(lcval)) gs.setAlign(Node.LEFT);
		//else /*some string*/
//System.out.println("CSS text-align: |"+lcval+"| => "+gs.getAlign());
		break;


	case TEXT_INDENT:
		break;
	case LINE_HEIGHT:
		break;

	case FLOAT:
		// left | right | none | inherit
		if ("left".equals(lcval)) gs.setAlign(Node.LEFT);
		else if ("right".equals(lcval)) gs.setAlign(Node.RIGHT);
		else if ("none".equals(lcval)) gs.setAlign(Node.NONE);
		//else if ("inherit".equals(lcval)) gs.setAlign(Node.INHERIT);
		break;

	case CLEAR:
		break;

	case DISPLAY:
		// inline | block | list-item | run-in | compact | marker | table | inline-table | table-row-group | table-header-group | table-footer-group | table-row | table-column-group | table-column | table-cell | table-caption | none | inherit
		if (lcval!=null) gs.display_ = lcval/*.intern()*/;
//System.out.println(lcval);
		//if ("inline".equals(lcval)) gs.priority_ = PRIORITY_SPAN;
		break;

	case WHITE_SPACE:
		break;
	case LIST_STYLE:
		break;
	case LIST_STYLE_TYPE:
		break;
	case LIST_STYLE_IMAGE:
		break;
	case LIST_STYLE_POSITION:
		// LATER
		break;



	// CSS2-specific
	case VISIBILITY:
		// visible | hidden | collapse | inherit
		break;


	// my own properties
	// not a CSS2 property (neither display: none doesn't allow overrides by children, visibility: hidden takes up space)
	case ELIDE:	// true, false, none
		if (lcval==null || lcval.equals("inherit")) gs.setElide(Context.BOOL_INHERIT);
		else if (lcval.equals("true")) gs.setElide(Context.BOOL_TRUE);
		else if (lcval.equals("false")) gs.setElide(Context.BOOL_FALSE);
		break;


	default:
		// "a declaration with an unknown property is ignored"
		//if (attrs==null) attrs=new HashMap(5);
		//gs.putAttr(name, value);
	}
  }


  /** Parse list of attribute name-value pairs into settings in CSSGeneral. */
  public void setAttrs(CSSGeneral gs, String pairs) {
	// name-attribute pairs in braces
//System.out.println("parse "+pairs);
	for (int lastinx=0,inx=pairs.length(); lastinx<inx; ) {
		// attribute: name1:value1; name2:value2; ...
		int inx1=pairs.indexOf(";", lastinx); if (inx1==-1 || inx1>inx) inx1=inx;
		int inx2=pairs.indexOf(":", lastinx); if (inx2==-1 || inx2>inx1) break;	//{ inx=-1; break; }	// must exist

		// trim whitespace off ends, NOT INTERNALLY
		String name = Strings.trimWhitespace(pairs, lastinx, inx2-1);
		String value = Strings.trimWhitespace(pairs, inx2+1, inx1-1);
//System.out.println("\tname="+name+", value="+value);

		setAttr(gs, name, value);

		lastinx=inx1+1;
	}
  }


  public void setPriority(String selector, CLGeneral cl) {
	// : tag, CLASS, ID--comma separated
	/*
	 CSS2' 6.4.3 Calculating a selector's specificity
	 count the number of ID attributes in the selector (= a) 100 [span + LOT*2]
	 count the number of other attributes and pseudo-classes in the selector (= b) 10 []
	 count the number of element names in the selector (= c) 1 [structural]
	 ignore pseudo-elements.
	*/
	int pri = ("inline".equals(cl.getDisplay())? PRIORITY_INLINE: PRIORITY_BLOCK);	// take from display type: INLINE vs not inline
	int hinx = selector.indexOf('#'), dinx=selector.indexOf('.'), cinx=selector.indexOf(':');

	// for purposes of display property and hence priority, take simple tag name
	int inx = Math.max(Math.max(dinx, hinx), cinx);
	String sel = (inx==-1? selector: selector.substring(0,inx));
//System.out.println("display of "+selector+" => "+sel);
	if (sel.length()>0) for (StyleSheet ss=this/*getCascade()*/; ss!=null && pri==PRIORITY_BLOCK; ss=ss.getCascade()) {
		CLGeneral gs = (CLGeneral)ss.get(sel);
//if (gs!=null) System.out.println(sel+" in "+ss.getName()+", display="+gs.getDisplay());
		if (gs!=null && "inline".equals(gs.getDisplay())) {
//System.out.println(sel+" => inline");
			pri = PRIORITY_INLINE;
		}
	}

	if (hinx!=-1) pri += PRIORITY_ID;
	if (dinx!=-1) pri += PRIORITY_CLASS;
	if (selector.charAt(0)!='.' && selector.charAt(0)!='#') pri += PRIORITY_ELEMENT;

//System.out.println(selector+" = "+pri+", "+cl.getDisplay());
	cl.setPriority(pri);
//	return pri; //ContextListener.PRIORITY_BLOCK;
  }

  Color getColor(String lcval) {
	Color color = Context.COLOR_INHERIT;

	if (lcval==null) {}
	else if (lcval.startsWith("rgb")) {
		// "rgb(0,0,0)"
		int p0=lcval.indexOf('('), p1=lcval.lastIndexOf(')');
		if (p0!=-1 && p1!=-1) {
			StringTokenizer st = new StringTokenizer(lcval.substring(p0+1,p1), ",");
			if (st.countTokens() == 3) {
				try {
					int r=Integer.parseInt(st.nextToken()), g=Integer.parseInt(st.nextToken()), b=Integer.parseInt(st.nextToken());
					color = new Color(r,g,b);
//System.out.println("rgb color = "+lcval+" => "+color);	// New Yorker
				} catch (NumberFormatException nfe) {}
			}
		}

	} else color = Colors.getColor(lcval, color);

	return color;
  }


  /**
	Parse stylesheet to hash of selector - CSSGeneral.
  */
  public void parse(String csstxt, URL base) {
//System.out.println("*** STYLE SHEET ***");
//System.out.println(csstxt);
	// normalize: remove all whitespace (only significant whitespace in CSS is in nested selectors and attribute values, which we don't support now)
	StringBuffer sb = new StringBuffer(csstxt.length());
	boolean incomment=false;
	for (int i=0,imax=csstxt.length(); i<imax; i++) {
		char ch = csstxt.charAt(i);
		if (incomment) {
			if (ch=='*' && i+1<imax && csstxt.charAt(i+1)=='/') { i++; incomment=false; }
		} else if (ch=='/' && i+1<imax && csstxt.charAt(i+1)=='*') { i++; incomment=true; }

		// The HTML comment tokens "<!--" and "-->" may occur before, after, and in between the statements
		// I think HTML should take these out
		// BUT DON'T FUNCTION AS COMMENTS WITHIN CSS ITSELF!
		else if (ch=='<' && i+3<imax && csstxt.charAt(i+1)=='!' && csstxt.charAt(i+2)=='-' && csstxt.charAt(i+3)=='-') i+=3;
		else if (ch=='-' && i+2<imax && csstxt.charAt(i+1)=='-' && csstxt.charAt(i+2)=='>') i+=2;
		//else if (!Character.isWhitespace(ch)) sb.append(ch); -- can't ignore whitespace wholesale
		else sb.append(ch);

	}
	String css=sb.substring(0);
//if (DEBUG) if (css.length()<200) 
//System.out.println(css);

	List<String> selector = new ArrayList<String>(10);
	CSSGeneral[] cssl = new CSSGeneral[100];
	int seli;
	String name,value,token;
	char ch;
	int inx=0,inx1,inx2;
	for (int lastinx=0,imax=css.length(); lastinx<imax; ) {
		 // @import, @page, ...
		if (css.charAt(lastinx)=='@') {
			inx = inx1 = lastinx+1;

			// @import, as in @import url("http://www.style.org/pastoral");
			if (css.regionMatches(inx, "import",0, "import".length())) {
				inx = eatSpace(css, inx + "import".length());
				if (css.regionMatches(inx, "url",0, "url".length())) inx = eatSpace(css, inx + "url".length());
				if (css.charAt(inx)=='(') inx++;	// '('..')' optional
				if (css.charAt(inx)=='"') inx++;	// "..." around string, which are often missing in practice

				inx1 = inx; while ((ch=css.charAt(inx1))!='"' && ch!=')' && ch!=';' /*&& ch!='\r' && ch!='\n'*/) inx1++;
				String ref = css.substring(inx,inx1).trim();

				if (ref!=null) {
//System.out.println("ref = "+ref+", relative to "+base);
					try { parse(new URL(base, ref)); } catch (MalformedURLException e) { System.out.println("bad URL on @import "+e); }
				}

				if (css.charAt(inx1)=='"') inx1++; if (css.charAt(inx1)==')') inx1++; if (css.charAt(inx1)==';') inx1++;

			} // else if (css.startsWith("@page")) ...

			lastinx = inx1;	// error recovery later
			continue;
		}

		// comma-separated list => individual selectors
		inx = css.indexOf("{", lastinx); if (inx==-1) break;	// no more specs
		selector.clear();
		while (lastinx<inx) {
			inx1 = css.indexOf(",", lastinx); if (inx1==-1 || inx1>inx) inx1=inx;
			String sel = Strings.trimWhitespace(css, lastinx, inx1-1).toLowerCase();
			if (sel.length() > 0) selector.add(sel);	// seen ".advisory a, advisory a:link, {..."
//if (DEBUG) System.out.println("selector |"+sel+"|");
			lastinx=inx1+1;
		}


		// retrieve existing CSSGeneral or make new
		int selcnt = selector.size();
		for (int i=0; i<selcnt; i++) {
			String sel = selector.get(i);
			Object csso = get(sel);

			if (csso instanceof CSSGeneral) {
				cssl[i] = (CSSGeneral)csso;
if (DEBUG) System.out.println(sel+ " existing");

			} else {
				cssl[i] = new CSSGeneral();	// or CSSGeneral, depending on display property of tag
				//cssl[i] = (CssListener)get(selector[i]);	// cascade by overriding existing -- when HTML established by CSS
				//cssl[i]=null;
				//if (cssl[i]==null) cssl[i]=new CssListener();	// don't share because may have individual difference (that I don't want to sort out later)
				put(sel, cssl[i]);
//if (DEBUG) System.out.println(sel+" new");
			}
//System.out.println(sel+" display = "+cssl[i].getDisplay());
		}

		inx = css.indexOf("}", lastinx); if (inx==-1) break;
		String pairs = css.substring(lastinx, inx);
		if (selcnt==1) {
//System.out.println("direct "+selector.get(0)+" => "+cssl[0]);
			setAttrs(cssl[0], pairs);
			setPriority(selector.get(0), cssl[0]);
		} else if (selcnt>=2) {
			// prototype + copy
			CSSGeneral proto = new CSSGeneral();
			setAttrs(proto, pairs);
//System.out.println("proto underline = "+proto.underline_);
			for (int i=0; i<selcnt; i++) {
				proto.copyInto(cssl[i]);
				setPriority(selector.get(i), cssl[i]);
//if (selector[i].equals("b"))
//System.out.println("copy "+selector[i]+" => "+cssl[i]);
			}
		}
		lastinx=inx+1;
	}
  }


/*
  // code to parse CSS specifications
  PushbackReader in=null;
  StringBuffer tsb = new StringBuffer(100);
  final int getToken() throws java.io.IOException {
	tsb.setLength(0);

	int ch = in.read();
	switch (ch) {
	case '\\':	// escape
		break;
	case '"': case '\'': // string
		break;
	case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
		do { tsb.append(ch); } while ((ch=in.read())>='0' && ch<='9');
		if (ch=='.') {
			while ((ch=in.read())>='0' && ch<='9') {}
			// convert as float
		} // else convert as integer
		in.unread(ch);
		break;
	case ' ': case '\t': case '\r': case '\n':
		while ((ch=in.read())==' ' || ch=='\t' || ch=='\r' || ch=='\n') {}
		in.unread(ch);
		return getToken();	// silently ignore whitespace(?)
	case '/':
		// if not comment, fall through to ident
	default:
		break;
	}
	return ERROR;
  }

/*
IDENT  {ident}
AT-KEYWORD	@{ident}
STRING	{string}
HASH  #{name}
NUMBER	{num}
PERCENTAGE	{num}%
DIMENSION  {num}{ident}
URL  url\({w}{string}{w}\)|url\({w}([^ \n\'\")]|\\\ |\\\'|\\\"|\\\))+{w}\)

RGB  rgb\({w}{num}%?{w}\,{w}{num}%?{w}\,{w}{num}%?{w}\)
UNICODE-RANGE  U\+[0-9A-F?]{1,8}(-[0-9A-F]{1,8})?
CDO  \<!--
CDC  -->
DELIM  [^][;{} \t\r\n()]
SEMICOLON  ;
LBRACE	\{
RBRACE	\}
LPAR  \(
RPAR  \)
LBRACK	\[
RBRACK	\]
WHITESPACE	[ \t\r\n]+
COMMENT  /\*([^*]|\*[^/])*\* /		[[C-style comments]]


The macros in curly braces ({}) above are defined as follows: Macro  Definition

--------------------------------------------------------------------------------

ident  {nmstart}{nmchar}*
nmstart  [a-zA-Z]|{nonascii}|{escape}
nonascii  [^\0-\177]
escape	\\[0-9a-fA-F]{1,6}
nmchar	{nmstart}|[-0-9]
num  [0-9]+|[0-9]*\.[0-9]+
string	\"({stringchar}|\')*\"|\'({stringchar}|\")*\'
stringchar	{escape}|{nonascii}|[\40-\176]
*/


/* SYNTAX
stylesheet	: (CDO | CDC | statement)*;
statement	: ruleset | at-rule;
at-rule	: AT-KEYWORD any* (block | ';');
block		: '{' (at-rule | any | block)* '}';
ruleset	: selector '{' declaration? (';' declaration)* '}';
selector	: any+;
declaration : property ':' value;
property	: IDENT;
value		: (any | block | AT-KEYWORD)+;
any		: IDENT | NUMBER | PERCENTAGE | DIMENSION | STRING
			  | DELIM | URL | RGB | HASH | UNICODE-RANGE
			  | '(' any* ')' | '[' any* ']';
* /

  public void parse() {

  }
*/
}
