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
package multivalent.std.adaptor;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;

import javax.swing.JFileChooser;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.CHashMap;
import multivalent.Context;
import multivalent.ContextListener;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.EventListener;
import multivalent.IDInfo;
import multivalent.IDInfo.Confidence;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.MediaAdaptor;
import multivalent.Multivalent;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.StyleSheet;
import multivalent.SystemEvents;
import multivalent.VObject;
import multivalent.gui.VButton;
import multivalent.gui.VCheckbox;
import multivalent.gui.VEntry;
import multivalent.gui.VMenu;
import multivalent.gui.VMenuButton;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;
import multivalent.gui.VScrollbar;
import multivalent.gui.VTextArea;
import multivalent.node.IHBox;
import multivalent.node.INodeZero;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafImage;
import multivalent.node.LeafText;
import multivalent.node.LeafUnicode;
import multivalent.node.LeafZero;
import multivalent.node.Root;
import multivalent.std.span.HyperlinkSpan;
import multivalent.std.span.SubSupSpan;
import multivalent.std.ui.DocumentPopup;
import phelps.awt.Colors;
import phelps.lang.Booleans;
import phelps.lang.Integers;
import phelps.lang.Maths;
import phelps.lang.Strings;
import phelps.net.URIs;
import phelps.util.Arrayss;
import uk.ac.liverpool.fab4.CharsetIdentifier;

import com.pt.awt.NFont;
import com.pt.awt.font.NFontManager;

import static multivalent.IDInfo.Confidence.*;

/**
 * Media adaptor for <a href='http://www.w3c.org/MarkUp/'>HTML</a> (.html =>
 * document tree).
 * 
 * <p>
 * Parts of HTML 4.01 not yet supported:
 * <ul>
 * <li>bidirectional text (DIR attribute, BDO tag)
 * <li>COLGROUP and COL styles
 * <li>new FORM controls
 * </ul>
 * 
 * <!-- future
 * <li>define style sheet in CSS (use existing <tt>html.css</tt>?), make one
 * static data structure copy, clone and hack for individual page style sheets
 * as modified by CSS -->
 * 
 * <p>
 * Use as parser, see instructions in superclass {@link MediaAdaptor}. Tree
 * looks like this:
 * <ul>
 * <li>structural HTML tags as {@link multivalent.Node}
 * <li>spans as {@link multivalent.Span}s, which can cross structure
 * </ul>
 * 
 * 
 * <!-- N.B. The parser does not retain page's comments!
 * 
 * takes the garbage HTML found on the web and constructs a clean parse tree,
 * with spans like B, TT and A (which are non-structural), implemented as links
 * between structural nodes.
 * 
 * format for layout coordinates on all words and structure -->
 * 
 * @version $Revision$ $Date$
 */
public class HTML extends ML /* implements EventListener */{
	static final boolean DEBUG = false && multivalent.Meta.DEVEL;
	static final boolean showtags = false;
	// boolean test = false;
	// keep various parts for DOM and other clients such as Crawler

	/**
	 * Submit HTML <code>FORM</code> to server.
	 * <p>
	 * <tt>"submitForm"</tt>: <tt>arg=</tt> {@link multivalent.Node}
	 * <var>top-of-form</var>,
	 */
	public static final String MSG_FORM_SUBMIT = "submitForm";

	/**
	 * Reset settings of HTML <code>FORM</code>.
	 * <p>
	 * <tt>"resetForm"</tt>: <tt>arg=</tt> {@link multivalent.Node}
	 * <var>top-of-form</var>,
	 */
	public static final String MSG_FORM_RESET = "resetForm";
	public static final String SET_URL = "setUrl";
	/**
	 * Give chance for client-side processing by another behavior before sending
	 * to server.
	 * <p>
	 * <tt>"processForm"</tt>: <tt>arg=</tt> {@link java.util.Map}
	 * <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root
	 * of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_FORM_PROCESS = "processForm";

	/**
	 * Set values of HTML <code>FORM</code>.
	 * <p>
	 * <tt>"populateForm"</tt>: <tt>arg=</tt> {@link java.util.Map}
	 * <var>attributes</var>, <tt>in=</tt> {@link java.util.Map} name-value
	 * pairs.
	 */
	public static final String MSG_FORM_POPULATE = "populateForm";

	public static final java.io.FileFilter FILTER = new phelps.io.FileFilterPattern(
			"(?i)\\.htm(l?)$");

	private static final String EV_FORM_SUBMIT = "event " + MSG_FORM_SUBMIT
			+ " <node>";
	private static final String EV_FORM_RESET = "event " + MSG_FORM_RESET
			+ " <node>";

	static final CSSGeneral EMPTYGS = new CSSGeneral(StyleSheet.PRIORITY_BLOCK
			+ StyleSheet.PRIORITY_CLASS/* .PRIORITY_ID/PRIORITY_OBJECTREF */);

	private static final Map<String, String> entity2unicode_, unicode2entity_;
	static { // could make Simple string and probably save some memory
		String[] str = { // See
							// http://www.w3.org/TR/REC-html40/sgml/entities.html
				// symbols and accented letters
				"160", "nbsp", "iexcl", "cent", "pound", "curren", "yen",
				"brvbar", "sect", "uml", "copy", "ordf", "laquo", "not", "shy",
				"reg", "macr",
				/* u00b0 */"deg", "plusmn", "sup2", "sup3", "acute", "micro",
				"para", "middot", "cedil", "sup1", "ordm", "raquo", "frac14",
				"frac12", "frac34", "iquest",
				/* u00c0 */"Agrave", "Aacute", "Acirc", "Atilde", "Auml",
				"Aring", "AElig", "Ccedil", "Egrave", "Eacute", "Ecirc",
				"Euml", "Igrave", "Iacute", "Icirc", "Iuml", "ETH", "Ntilde",
				"Ograve", "Oacute", "Ocirc", "Otilde", "Ouml", "times",
				"Oslash", "Ugrave", "Uacute", "Ucirc", "Uuml", "Yacute",
				"THORN", "szlig",
				/* u00e0 */"agrave", "aacute", "acirc", "atilde", "auml",
				"aring", "aelig", "ccedil", "egrave", "eacute",
				"ecirc",
				"euml",
				"igrave",
				"iacute",
				"icirc",
				"iuml",
				"eth",
				"ntilde",
				"ograve",
				"oacute",
				"ocirc",
				"otilde",
				"ouml",
				"divide",
				"oslash",
				"ugrave",
				"uacute",
				"ucirc",
				"uuml",
				"yacute",
				"thorn",
				"yuml",
				"338",
				"OElig",
				"oelig",
				"352",
				"Scaron",
				"scaron",
				"376",
				"Yuml",
				"402",
				"fnof",
				// Greek
				"913", "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta",
				"Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi",
				"Omicron", "Pi", "Rho", /* no Sigmaf or U+03a2 */"931",
				"Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega", "945",
				"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta",
				"theta",
				"iota",
				"kappa",
				"lambda",
				"mu",
				"nu",
				"xi",
				"omicron",
				"pi",
				"rho",
				"sigmaf",
				"sigma",
				"tau",
				"upsilon",
				"phi",
				"chi",
				"psi",
				"omega",
				"977",
				"thetasym",
				"upsih",
				"982",
				"piv",
				// mathematics
				"8226", "bull", "8230", "hellip", "8242", "prime", "Prime",
				"8254", "oline", "8260", "frasl", "8472", "weierp", "8465",
				"image", "8476", "real", "8482", "trade", "8501", "alefsym",
				"8592", "larr", "uarr", "rarr", "darr", "harr", "8629",
				"crarr", "8656", "lArr", "uArr", "rArr", "dArr", "hArr",
				/* U+2200 */"8704", "forall", "8706", "part", "exist", "8709",
				"empty", "8711", "nabla", "isin", "notin", "8715", "ni",
				"8719", "prod", "8721", "sum", "8722", "minus", "8727",
				"lowast", "8730", "radic", "8733", "prop", "infin", "8736",
				"ang", "8743", "and", "or", "cap", "cup", "int", "8756",
				"there4", "8764", "sim", "8773", "cong", "8776", "asymp",
				"8800", "ne", "equiv", "8804", "le", "ge", "8834", "sub",
				"sup", "nsub", "8838", "sube", "supe", "8853", "oplus", "8855",
				"otimes", "8869", "perp", "8901", "sdot", "8968", "lceil",
				"rceil", "lfloor", "rfloor", "9001", "lang", "rang", "9674",
				"loz", "9824", "spades", "9827",
				"clubs",
				"9829",
				"hearts",
				"9830",
				"diams",
				// general
				"34", "quot", "38", "amp", "60", "lt", "62", "gt", "710",
				"circ", "732", "tilde", "8194", "ensp", "emsp", "8201",
				"thinsp", "8204", "zwnj", "zwj", "lrm", "rlm", "8211", "ndash",
				"mdash", "8216", "lsquo", "rsquo", "sbquo", "8220", "ldquo",
				"rdquo", "bdquo", "8224", "dagger", "8225", "Dagger", "8240",
				"permil", "8249", "lsaquo", "rsaquo", "8364", "euro",
				// robust to HTML errors in the wild
				"169", "copyright", "34", "quote", "34", "QUOT", // http://www.acm.org/pubs/contents/journals/cacm/1998-41/
		};

		// populate htmlEntity
		entity2unicode_ = new HashMap<String, String>(2 * str.length);
		unicode2entity_ = new HashMap<String, String>(2 * str.length);
		int num = -1;
		for (int i = 0, imax = str.length; i < imax; i++) {
			String e = str[i];
			if (Character.isDigit(e.charAt(0))) {
				try {
					num = Integer.parseInt(e);
				} catch (NumberFormatException nfe) {
					assert false : e;
				}
			} else {
				String sch = Strings.valueOf((char) num); // should be
															// character, but
															// temporarily have
															// "mdash"="--"
				entity2unicode_.put(e, sch);
				unicode2entity_.put(sch, e);
				// System.out.println(e+"	"+charNum);
				num++;
			}
		}

		// spot checks
		assert getUnicode("uacute") == 250;
		assert getUnicode("phi") == 966;
		assert getUnicode("sim") == 8764;
		assert getUnicode("euro") == 8364;
	}

	/* package-private */static float[] validpoints = { 9f, 10f, 12f, 14f, 18f,
			24f, 36f, 48f, 72f, 144f };
	/* package-private */static int[] points2size = new int[144 + 1];
	static {
		// maybe replace 0s with closest real size
		for (int i = 0, imax = validpoints.length; i < imax; i++)
			points2size[(int) validpoints[i]] = i + 1;
	}

	// *
	// *************************************************************************
	// Parser for hardcoded HTML tag set, which has so many errors that a
	// grammar-based parser would spend all its time in error correction anyhow

	// I want an enum, at least! Order not important here as long as matches
	// constant table immediately following.
	/* public /since returned, but take a lot of space in JavaDoc */static final int TAG_UNKNOWN = -1,
			TAG_HTML = 0,
			TAG_HEAD = TAG_HTML + 1,
			TAG_TITLE = TAG_HEAD + 1,
			TAG_BASE = TAG_HEAD + 2,
			TAG_META = TAG_HEAD + 3,
			TAG_LINK = TAG_HEAD + 4,
			TAG_STYLE = TAG_HEAD + 5,
			TAG_FRAMESET = TAG_STYLE + 1,
			TAG_FRAME = TAG_FRAMESET + 1,
			TAG_IFRAME = TAG_FRAMESET + 2,
			TAG_NOFRAMES = TAG_FRAMESET + 3,
			TAG_BODY = TAG_NOFRAMES + 1,
			TAG_FORM = TAG_BODY + 1,
			TAG_INPUT = TAG_FORM + 1,
			TAG_BUTTON = TAG_FORM + 2,
			TAG_TEXTAREA = TAG_FORM + 3,
			TAG_SELECT = TAG_FORM + 4,
			TAG_OPTION = TAG_FORM + 5,
			TAG_OPTGROUP = TAG_FORM + 6,
			TAG_FIELDSET = TAG_FORM + 7,
			TAG_HR = TAG_FIELDSET + 1,
			TAG_BR = TAG_HR + 1,
			TAG_MAP = TAG_BR + 1,
			TAG_AREA = TAG_MAP + 1,
			TAG_TABLE = TAG_AREA + 1,
			TAG_CAPTION = TAG_TABLE + 1,
			TAG_TR = TAG_TABLE + 2,
			TAG_TD = TAG_TABLE + 3,
			TAG_TH = TAG_TABLE + 4,
			TAG_COL = TAG_TABLE + 5,
			TAG_COLGROUP = TAG_TABLE + 6,
			TAG_THEAD = TAG_TABLE + 7,
			TAG_TFOOT = TAG_TABLE + 8,
			TAG_TBODY = TAG_TABLE + 9,
			TAG_IMG = TAG_TBODY + 1,
			TAG_PRE = TAG_IMG + 1,
			TAG_OBJECT = TAG_PRE + 1,
			TAG_PARAM = TAG_OBJECT + 1,
			TAG_SCRIPT = TAG_PARAM + 1,
			TAG_NOSCRIPT = TAG_SCRIPT + 1,

			TAG_P = TAG_NOSCRIPT + 1,
			TAG_UL = TAG_P + 1,
			TAG_OL = TAG_UL + 1,
			TAG_LI = TAG_OL + 1,
			TAG_DL = TAG_LI + 1,
			TAG_DT = TAG_DL + 1,
			TAG_DD = TAG_DL + 2,
			TAG_H1 = TAG_DD + 1,
			TAG_H2 = TAG_H1 + 1,
			TAG_H3 = TAG_H1 + 2,
			TAG_H4 = TAG_H1 + 3,
			TAG_H5 = TAG_H1 + 4,
			TAG_H6 = TAG_H1 + 5,
			TAG_DIV = TAG_H6 + 1,
			TAG_BLOCKQUOTE = TAG_DIV + 1,

			TAG_SPAN = TAG_BLOCKQUOTE + 1,
			TAG_VIDEO = TAG_SPAN + 1,
			TAG_A = TAG_VIDEO + 1,
			TAG_B = TAG_A + 1,
			TAG_I = TAG_B + 1,
			TAG_TT = TAG_B + 2,
			TAG_SUP = TAG_B + 3,
			TAG_SUB = TAG_B + 4,
			TAG_EM = TAG_B + 5,
			TAG_STRONG = TAG_B + 6,
			TAG_CODE = TAG_B + 7,
			TAG_SAMP = TAG_B + 8,
			TAG_VAR = TAG_B + 9,
			TAG_CITE = TAG_B + 10,
			TAG_KBD = TAG_B + 11,
			TAG_SMALL = TAG_B + 12,
			TAG_BIG = TAG_B + 13,
			TAG_DEL = TAG_B + 14,
			TAG_INS = TAG_DEL + 1,
			TAG_ABBR = TAG_INS + 1,
			TAG_ACRONYM = TAG_ABBR + 1,
			TAG_DFN = TAG_ACRONYM + 1,
			TAG_Q = TAG_DFN + 1,
			TAG_ADDRESS = TAG_Q + 1,
			TAG_BDO = TAG_ADDRESS + 1,
			TAG_LEGEND = TAG_BDO + 1,
			TAG_LABEL = TAG_LEGEND + 1,

			// deprecated: parser tries to rewrite in current HTML 4.0 tags
			TAG_BASEFONT = TAG_LABEL + 1,
			TAG_MENU = TAG_BASEFONT + 1,
			TAG_DIR = TAG_MENU + 1,
			TAG_ISINDEX = TAG_DIR + 1,
			TAG_APPLET = TAG_ISINDEX + 1,
			TAG_CENTER = TAG_APPLET + 1,
			TAG_FONT = TAG_CENTER + 1,
			TAG_STRIKE = TAG_FONT + 1,
			TAG_S = TAG_FONT + 2,
			TAG_U = TAG_FONT + 3,

			// bogus; theoretically should be able to ignore, but can't in
			// practice
			TAG_NOLAYER = TAG_U + 1,
			TAG_WBR = TAG_NOLAYER + 1,
			TAG_NOBR = TAG_WBR + 1, TAG_BLINK = TAG_NOBR + 1,

			// FABIO: added experimental video TAG (HTML 5) for testing embedded
			// video

			TAG_LASTTAG = TAG_BLINK + 1;

	


	private Vector<Leaf> vids = null;
	/** Maps from tag name to integer id. */
	private static final Map<String, Integer> tagID_ = new CHashMap<Integer>(
			TAG_LASTTAG * 2);
	/**
	 * Maps from integer id to (interned, all-lowercase) tag name. Access with
	 * getTag().
	 */
	private static final String[] tagList_;

	/** Number of times open-tag of given id is used in document. */
	public int[] TagUse = new int[TAG_LASTTAG];
	static int[] tracktag = { TAG_TABLE, TAG_THEAD, TAG_TFOOT, TAG_TBODY,
			TAG_TR, TAG_TD, TAG_P, TAG_HEAD, TAG_BODY/* 0,1,2 */, TAG_META };

	// static final String[] alltags;
	static {
		tagList_ = new String[TAG_LASTTAG];

		String[] tags = { // all HTML tags
		/* vbox, fyi */"html",
		/* special */"head", "title", "base", "meta", "link", "style",
				"frameset", "frame", "iframe", "noframes", "body", "form",
				"input", "button", "textarea", "select", "option", "optgroup",
				"fieldset", "hr", "br", "map", "area", "table", "caption",
				"tr", "td", "th", "col", "colgroup", "thead", "tfoot", "tbody",
				"img", "pre", "object", "param", "script", "noscript", /*
																		 * special
																		 * or
																		 * doesn
																		 * 't
																		 * matter
																		 * : HR
																		 * and
																		 * afterward
																		 * --
																		 * should
																		 * have
																		 * UL OL
																		 */
				/* parabox */"p", "ul", "ol", "li", "dl", "dt", "dd", "h1",
				"h2", "h3", "h4", "h5", "h6", "div", "blockquote",
				/* span */"span", "video", "a", "b", "i", "tt", "sup", "sub",
				"em", "strong", "code", "samp", "var", "cite", "kbd", "small",
				"big", "del", "ins", "abbr", "acronym", "dfn", "q", "address",
				"bdo", "legend", "label",
				/* deprecated */"basefont", "menu", "dir", "isindex", "applet",
				"center", "font", "strike", "s",
				"u", // more to come as reclassify
				/* bogus--browser-specific extensions */"nolayer", "wbr",
				"nobr", "blink", // "LAYER" and "ILAYER" too?
		};
		assert tags[tags.length - 1].equals("blink");

		assert tags.length == TAG_LASTTAG;
		for (int id = 0, idmax = tags.length; id < idmax; id++) {
			String name = tags[id];
			assert name.equals(name.toLowerCase()) : name;
			tagList_[id] = name;
			assert tagID_.get(name) == null : "duplicate: " + name;
			// alltags[id] = name;
			tagID_.put(name, Integers.getInteger(id)); // binary search faster
														// than hash for small
														// tables? no
		}
		// Arrays.sort(alltags);
		// for (int i=0,imax=alltags.length; i<imax; i++)
		// System.out.println(alltags[i]);

		// a few spot checks
		assert TAG_TABLE == getTagID("table");
		assert TAG_H1 == getTagID("h1");
		assert TAG_SPAN == getTagID("span");
		assert TAG_INS == getTagID("ins");
		assert TAG_NOSCRIPT == getTagID("noscript");
		assert TAG_BLINK == getTagID("blink"); // last tag
	}

	/** Return ID corresponding to passed HTML tag. */
	/* public */static int getTagID(String tag) {
		if (tag == null)
			return TAG_UNKNOWN;
		Object o = tagID_.get(tag); // binary search faster? NO--too many
									// .equals()
		return (o != null ? ((Integer) o).intValue() : TAG_UNKNOWN);
	}

	/** Return (lowercase, interned) String HTML tag corresponding to passed ID. */
	/* public */static String getTag(int id) {
		return (id >= 0 && id < tagList_.length ? tagList_[id] : null);
	}

	public static final int TAGTYPE_UNKNOWN = -1, TAGTYPE_EMPTY = 0,
			TAGTYPE_SPAN = 1, TAGTYPE_NEST = 2, TAGTYPE_NONEST = 3;
	// /static final String[] parseTypes = { "EMPTY", "SPAN", "NEST" }; // rest
	// are "NONEST"
	static final int[] parseType = new int[TAG_LASTTAG];
	static {
		String[] tags = {
		/* EMPTY */"area", "base", "basefont", "br", "frame", "hr", "img",
				"input", "isindex", "link", "meta", "param", "wbr", "col",
				null,
				/* SPAN */"span", "a", "b", "i", "tt", "s", "strike", "u",
				"sup", "sub", "del", "ins", "em", "strong", "cite", "var",
				"kbd", "code", "samp", "font", "small", "big", "abbr",
				"acronym", "dfn", "q", "address", "bdo", "legend", "label",
				"blink", "video", null,
				/* NEST */"dl", "ol", "ul", "menu", "dir", "table", "frameset",
				"object",
		// don't nest: h1..h6, p, center, map, area, font, li, form, tr, td
		};

		Arrays.fill(parseType, TAGTYPE_NONEST);
		for (int i = 0, imax = tags.length, cat = TAGTYPE_EMPTY; i < imax; i++) {
			String tag = tags[i];
			if (tag == null) {
				cat++;
				assert cat <= TAGTYPE_NONEST;
				continue;
			}
			int tagid = getTagID(tag);
			assert tagid >= 0 && tagid < TAG_LASTTAG : tag + " => " + tagid;
			parseType[tagid] = cat;
		}
	}

	// long refreshat = -1;
	TimerTask tt = null;
	// URI refreshURI_ = null;
	/**
	 * Can be different than document URL if use &ltbase href=''&gt. URL rather
	 * than URI because (1) URI too restrictive for sloppy practice and (2) URI
	 * w/o protocol handler not needed because always used to load and hence need
	 * protocol handler.
	 */
	URI baseURI_ = null;
	/**
	 * Sometimes URI too restrictive, e.g.,
	 * <tt>http://www.dli2.nsf.gov/web_style[1].css</tt>
	 */
	// URL baseURL_ = null; // -- not good to allow [] in URL either, since IPv6
	// there just as much
	/*
	 * LINK rel=spreadsheet FRAME, X iframe form INPUT type=image IMG -- could
	 * be lots
	 */

	private Layer scratchlayer_ = null;
	private INode head_ = null, body_ = null; // take LINKs from HEAD for a
												// menu. should have exactly one
												// title too.


	public HTML() {
		entity_ = entity2unicode_;
	}

	public void destroy() {
		// System.out.println("*** HTML destroyed: kill pending META HTTP-EQUIV refresh");
		// refreshat = -1;
		if (tt != null) {
			tt.cancel();
			tt = null;
		}
		if (vids!=null){
			for (Leaf v:vids)
				v.remove();
		}
		super.destroy();
	}

	/*
	 * HTML UTILITIES
	 */

	/**
	 * Return Unicode character corresponding to given HTML entity reference. If
	 * no such character, return '\0'.
	 */
	public static char getUnicode(String entity) {
		assert entity != null;
		String sch = entity2unicode_.get(entity);
		return (sch == null ? '\0' : sch.charAt(0));
	}

	/**
	 * Return entity corresponding to given Unicode character, if any. If no
	 * such entity, return null.
	 */
	public static String getEntity(int codepoint) {
		// String sch = Strings.valueOf(unicode);
		return unicode2entity_.get(Strings.valueOf((char) codepoint));
	}

	/** Less efficient than {@link #getParseType(int)}. */
	public static int getParseType(String tag) {
		return getParseType(getTagID(tag));
	}

	/**
	 * Parse type is TAGTYPE_EMPTY, TAGTYPE_SPAN, TAGTYPE_NEST, TAGTYPE_NONEST,
	 * or TAGTYPE_UNKNOWN if tag is unknown.
	 */
	public static int getParseType(int tagid) {
		// assert tagid>=-1 && tagid<TAG_LASTTAG;
		return (tagid >= 0 && tagid < TAG_LASTTAG ? parseType[tagid]
				: TAGTYPE_UNKNOWN);
	}

	static int getAbsPct(String innum, int base) {
		return getAbsPct(innum, base, base/* Integer.Mir_VALUE */);
	}

	static int getAbsPct(String innum, int base, int outnum) {
		if (innum != null) {
			innum = innum.trim();
			if (innum.endsWith("%")) {
				innum = innum.substring(0, innum.length() - 1);
				try {
					outnum = base * Integer.parseInt(innum) / 100;
				} catch (NumberFormatException pctex) {
				}
			} else {
				try {
					outnum = Integer.parseInt(innum);
				} catch (NumberFormatException absex) {
				}
			}
		}
		return outnum;
	}

	/*
	 * static Map align2int=new HashMap(10), valign2int=new HashMap(10); static
	 * { align2int.put("left", new Integer(Node.LEFT)); align2int.put("right",
	 * new Integer(Node.RIGHT)); align2int.put("center", new
	 * Integer(Node.CENTER)); //align2int.put("middle", new
	 * Integer(Node.CENTER)); // not in spec, conflicts with valign=middle
	 * align2int.put("justify", new Integer(Node.FILL)); align2int.put("char",
	 * new Integer(Node.MIDDLE));
	 * 
	 * align2int.put("top", new Integer(Node.TOP)); align2int.put("middle", new
	 * Integer(Node.MIDDLE)); align2int.put("bottom", new Integer(Node.BOTTOM));
	 * align2int.put("baseline", new Integer(Node.BASELINE)); }
	 */
	/*

  */
	public static byte getAlign(String spec) { // for floats, not justification
		if (spec == null)
			return Node.NONE;

		byte align/* INVALID? */; // default
		spec = spec.toLowerCase();
		if ("left".equals(spec))
			align = Node.LEFT; // faster to intern than .equals chain?
		else if ("center".equals(spec) || /* not spec */"middle".equals(spec))
			align = Node.CENTER;
		else if ("right".equals(spec))
			align = Node.RIGHT;
		else if ("justify".equals(spec))
			align = Node.FILL;
		else if ("char".equals(spec)) { // -- cellhalign only?
			align = Node.NONE; // not supported
			// getAttr("char");
		} else
			align = Node.NONE;
		return align;
	}

	public static byte getVAlign(String spec) { // for floats, not justification
		if (spec == null)
			return Node.NONE;

		byte align;
		spec = spec.toLowerCase();
		if ("top".equals(spec))
			align = Node.TOP;
		else if ("middle".equals(spec) || "center".equals(spec))
			align = Node.MIDDLE;
		else if ("bottom".equals(spec))
			align = Node.BOTTOM;
		else if ("baseline".equals(spec))
			align = Node.BASELINE;
		else
			align = Node.NONE;

		return align;
	}

	void addAttrs(Map<String, Object> attrs, VObject into) {
		if (attrs != null)
			for (Iterator<Map.Entry<String, Object>> i = attrs.entrySet()
					.iterator(); i.hasNext();) {
				Map.Entry<String, Object> e = i.next();
				into.putAttr(e.getKey(), e.getValue());
			}
	}

	/**
	 * Overrides because "In HTML, only the following characters are defined as
	 * white space characters:
	 * 
	 * ASCII space (&#x0020;) ASCII tab (&#x0009;) ASCII form feed (&#x000C;)
	 * Zero-width space (&#x200B;)" ...
	 * "All line breaks constitute white space."
	 */
	protected void eatSpace() throws IOException {
		char ch;
		ispace = false;
		while ((ch = readChar()) == ' ' || ch == '\t' || ch == '\f'
				|| ch == 0x200b || ch == '\n' || ch == '\r')
			ispace = true;
		ir_.unread(ch);
	}

	private String _txt;
    private String viewerid;
    private String script;
    private Leaf viewerObject;

	/*
	 * (non-Javadoc)
	 * 
	 * @see multivalent.std.adaptor.ML#getReader()
	 */
	@Override
	protected Reader getReader() throws IOException {
		if (ir_ == null) {
			// DocInfo d = d
			// nsDetector det = new nsDetector( nsPSMDetector.ALL) ;
			// File f = MediaLoader.FileCache.get(getURI());
			// // charset detection
			// if (f!=null) {
			// // TODO: add charset detection (using html charset= if not
			// Mozilla nsDetector
			//
			// }
			//
			Reader r = null;
			if (_txt !=null)
				r = new StringReader(_txt);
			else {
				String ct = getInputUni().getContentType();
				InputStream is = getInputUni().getInputStreamRaw();
				if (is == null)
					is = getInputUni().getInputStream();
				r= CharsetIdentifier.getReader(is, ct, true);
			}
			//
			//Reader r = CharsetIdentifier.getReader(getInputUni(), true);
			//			BufferedReader br = new BufferedReader(r);
			//			String l;
			//			while (( l = br.readLine())!=null)
			//				System.out.println(l);
			//System.out.println(r);
			//new InputStreamReader(getInputUni().getInputStreamRaw(),
			//	"UTF-8");

			ir_ = new PushbackReader(r, 10);

		}
		return ir_;
	}


	/*
	 * Map linkTypes; static { StringTokenizer st = new StringTokenizer( //
	 * Stylesheet
	 * "Alternate Start Next Prev/Previous Previous Contents ToC/Contents Index "
	 * + "Glossary Copyright Chapter Section Subsection Appendix Help Bookmark"
	 * ); }
	 */
	// if used Nodes with style sheet could do this automatically(?)
	// LATER parseFragment() - so other behaviors can throw HTML at this and get
	// back a tree, but not a full HTML/HEAD/BODY tree
	// also should have parseFragment, where assume already taken care of
	// HTML/HEAD/BODY, if want to be robust to them
	/**  */

	/**
	 * Normalizes in direction of XHTML: lowercase tag and attribute names, well
	 * nested (except for spans), ... Within generated tree, all tags (GIs) are
	 * interned. This fact is exploited while parsing, but not afterward (when
	 * gleaning FORM, say) as other behaviors could have hacked tree and not
	 * been careful to intern (or always use literal Strings).
	 */
	public Object parse(INode parent) throws Exception {
		if (getReader() == null)
			return null; // HTML is used as a helper

		// long timein = System.currentTimeMillis(); => external measurement
		baseURI_ = getURI();
		if (baseURI_ == null)
			baseURI_ = URI.create("http://www.nowhere.com/index.html");
		// if (baseURI_!=null) System.out.println("parse: baseURI = "+baseURI_);
		Browser br = getBrowser();

		// reset instance vars for re-entrant use
		head_ = body_ = null;
		Arrays.fill(TagUse, 0);

		Span[] pspan = new Span[100]; // later make this expanding? not going to
										// have more than 100 open spans, but
										// would like typed ArrayList
		int pspani = 0;
		int[] structspanfirst = new int[100]; // in TD (and other tags?) close
												// off all tags started in it
												// when exit it
		int structspani = 0;

		Document doc = parent.getDocument();
		scratchlayer_ = doc.getLayer(Layer.SCRATCH);

		CSS/* StyleSheet */ss = new CSS(); // replace generic stylesheet with
											// CSS
		ss.setCascade(doc);
		ss.getContext(null, null).zoom = getZoom();

		// INode htmlroot = new HTMLHTML("html",null, zoom/*doc*/, this); //
		// required in doc, so create here and link in as appropriate
		INode htmlroot = new IVBox("html", null, parent); // required in doc, so
															// create here and
															// link in as
															// appropriate
		Map<String, VRadiogroup> radiogroups = new CHashMap<VRadiogroup>(10);

		// if (br!=null) br.setCurDocument(doc); // doc not set when used by
		// RobustHyperlink, FRAME => done elsewhere
		// INode absvis = null;
		// //*htmlroot*/doc.getVisualLayer("FLOAT","multivalent.node.IRootAbs");
		// => after BODY so drawn on top!
		// new LeafText(" ",null, absvis);
		// Leaf absleaf = new LeafText("ABSOLUTE",null, absvis);
		// absleaf.bbox.setBounds(50,100, 50,15);

		head_ = new INodeZero("head", null, htmlroot); // required in doc, so
														// create here and link
														// in as appropriate

		INode p = htmlroot, newp, newtd, n = null; // = root; // parent of
													// current node
		Node newn;
		Node isindex = null;

		int prcnt = 0; // for progressive rendering

		// StringBuffer txtraw = new StringBuffer(1000); // build up text
		// context (as interrupted by span tags)
		Leaf txtnode = null;

		// boolean premode=false;
		// boolean seenBODY = false;
		INode curFRAMESET = null; // for fixups on NOFRAMEs, which should go
									// under FRAMESET, but people put it
									// anywhere
		INode pendingNOFRAMES = null;
		// URI href;
		String attr;
		int ival;
		Color attrcolor;

		// in order to write out preserving version, could suck up everything
		// before first < and after </HTML>
		int closecnt = 0;
		String name = null, pendingName = null;
		char ptype = 'X', pendingPtype = 'X';
		Map<String, Object> attrs = null, pendingAttrs = null;
		int parsetype = TAGTYPE_UNKNOWN, pendingParsetype = TAGTYPE_UNKNOWN;
		int id = -1, pendingId = -1;
		char ch = 'X';

		Map<String, String> badtag = new HashMap<String, String>(100); // hold
																		// non-HTML
																		// tags
																		// so
																		// can
																		// use
																		// ==
																		// (and
																		// .intern()
																		// surprisingly
																		// expensive)
		// int sccnt=0; -- count single-char strings
		// int badconcatcnt=0;
		// Map anchors = (Map)doc.getVar(Document.VAR_ANCHORS); // everybody can
		// have 'id' attr
		// invariant: sgs always available; replace if used. Should ask
		// StyleSheet for instances.
		CSSGeneral sgs = new CSSGeneral(CSS.PRIORITY_BLOCK + CSS.PRIORITY_CLASS/*
																				 * PRIORITY_ID/
																				 * PRIORITY_OBJECTREF
																				 */); // generic
																						// spans/structure
																						// specific
																						// to
																						// node
																						// (style
																						// attribute
																						// or
																						// random
																						// other
																						// attributes)

		// update screen after every so many tags, if at clean point
		// boolean pendingupdate=false;

		boolean fInter = (getHints() & HINT_NO_INTERACTIVE) == 0;
		eatSpace();
		/* try { */while (true) {
			if (closecnt == -1)
				closecnt = 0;
			if (closecnt == 0 && pendingName == null) {
				// eatSpace();
				try {
					eatComment();
					ch = readChar();
				} catch (IOException e) {
					// closecnt=0; for (newp=p; newp!=null && newp!=htmlroot;
					// newp=newp.getParentNode()) closecnt++;
					closecnt = Integer.MAX_VALUE; // close up when run into
													// htmlroot
					// System.out.println("readchar after comment, closecnt="+closecnt);
				}
			}

			if (ch == '<' || closecnt > 0 || pendingName != null) {
				// System.out.println(""+parsei+' '+(int)ch);
				// if (tagsb.length()>0)
				// System.out.println("getTag seeded with "+tagsb.substring(0)));
				// could save space by interning names
				ESISNode t = null;
				// ESISNode t = new ESISNode(""); // communications with ML
				// parser

				if (closecnt > 0) {
					if (pendingName == null) {
						pendingName = name;
						pendingId = id;
						pendingAttrs = attrs;
						pendingPtype = ptype;
						pendingParsetype = parsetype;
					}
					closecnt--;
					// System.out.print("/"+closecnt+"/	");
					// do { } while (name==null &&
					// (p=p.getParentNode())!=htmlroot); // sometimes have
					// null-named nodes in tree
					name = p.getName();
					// System.out.print("closing "+name+" @ "+(closecnt+1));
					// if (name==null) { n=p; p=p.getParentNode(); continue; }
					// // nothing in switch to close
					/*
					 * if (name==null) {
					 * System.out.println("name==null in closecnt>0, pendingName="
					 * +pendingName); p.getParentNode().getParentNode().dump();
					 * System.exit(0); }
					 */
					id = getTagID(name);
					// System.out.println("closecnt id="+id+" / "+name+"/"+getTag(id));
					attrs = null;
					ptype = '/'; // implicit
					parsetype = TAGTYPE_NEST; // implicit

				} else if (pendingName != null) {
					// System.out.print("again  ");
					name = pendingName;
					// if (name==null)
					// System.out.println("name==null in pendingName!=null");
					id = pendingId;
					attrs = pendingAttrs;
					ptype = pendingPtype;
					parsetype = pendingParsetype;
					// System.out.println("pending "+ptype+" "+name+", id="+id+"/"+getTag(id));

					pendingName = null; // make available

				} else if (isStopped()) {
					// if about to fetch new tag, close up and stop instead
					closecnt = matchTag(p, "html", htmlroot);
					if (closecnt > 0)
						continue;
					else {
						p = new IParaBox("h1", attrs, p);
						new LeafUnicode("Transfer interrupted", null, p);
						// doc.stop_ = true; -- so doesn't save layer when
						// moving to new document
						break;
					}

				} else { // process new tag
					try {
						t = getTag();
						// have seen "< ", which other browsers treat as content
					} catch (IOException e) { // no </HTML>
						// closecnt=0; for (newp=p; newp!=null &&
						// newp!=htmlroot; newp=newp.getParentNode())
						// closecnt++;
						// System.out.println("*** no /HTML: closecnt="+closecnt);
						// if blow up in middle of tag, close up properly
						break;
					}
					name = t.getGI();
					id = getTagID(name); // canonicalize
					if (id >= 0) {
						name = getTag(id); // use interned string, so can use ==
											// over .equals() internally --
											// would like to do before creating
											// String. would like to do for
											// attributes too
						parsetype = getParseType(id); // gives hardcoded tag
														// knowledge precedence
														// over empty tag syntax
						// parsetype = (t.empty? TAGTYPE_EMPTY:
						// getParseType(id)); // seen "<a name='' />"
					} else {
						String iname = badtag.get(name);
						if (iname != null)
							name = iname;
						else {
							name = name.intern();
							badtag.put(name, name);
						} // already lc
						parsetype = (t.empty ? TAGTYPE_EMPTY : TAGTYPE_NEST);
					}
					// if (name==null) System.out.println("*** name = "+name);
					// System.out.println(tag+"	 "+attrs);
					// ptype = name.charAt(0);
					// ptype = (t.open? name.charAt(0):'/');
					ptype = t.ptype;
					// if (ptype=='/') { name = name.substring(1);
					// t.setGI(name); } => inefficient: memory alloc on every
					// close tag!

					// various clean ups on real tags, before branching between
					// open or close
					if (ptype == '/') {
						if (TAG_BODY == id)
							continue; // ignore </body>, as people put these in
										// randomly. Let </html> or </noframes>
										// force it closed.
						attrs = null; // this one error possibility that I've
										// never seen

					} else { // open
						attrs = t.attrs; // t.getAttributes(); -- don't need
											// clone()
						if (attrs != null && attrs.size() == 0)
							attrs = null;

						if (TAG_A == id /* || ?==id || ?==id */) { // Non-nestable
																	// spans.
																	// Some
																	// nestable
																	// (Q,I),
																	// some not
																	// (A), some
																	// not
																	// meaningful
																	// either
																	// way but
																	// nestable
																	// (B)
							if (attrs == null)
								ptype = '/'; // error: <a href=...> ... <a> =>
												// <a href=...> ... </a>
							else { // <a href=...> ... <a href=...> => <a> ...
									// </a><a href=...>
								for (int i = pspani - 1; i >= 0; i--) {
									if (name == pspan[i].getName()) {
										if (DEBUG)
											System.out
													.println("runaway A: match on "
															+ name
															+ ", attrs="
															+ pspan[i]
																	.getAttributes()
															+ " @ " + attrs);
										pendingName = name;
										pendingId = id;
										pendingAttrs = attrs;
										pendingPtype = ptype;
										pendingParsetype = parsetype; // push <a
																		// ...>
										ptype = '/';
										attrs = null; // close previous
										break;
									}
								}
							}
							// } else if (?==id) ...
							// } else if (-1==id) { // unknown tag
							// maybe: if (name.startsWith("href")) {
							// t.attrs.put("href",
							// name.substring(name.indexOf('=')+1)); name="a"; }
						}
					}
				}

				if (ptype == '!') { // SPECIAL (always EMPTY)
					// special. for now ignore
					// could be DOCTYPE (others?)
					// System.out.println("special: "+name);
					// eatSpace();

					// *** CLOSE ***
				} else if (ptype == '/') {
					// LATER: close up spans at various structural boundaries
					// (now just TD, but want FRAME, ...)

					if (showtags)
						System.out.println("/" + name);
					// if ("p".equals(name)) System.out.println("/"+name);
					// assert pairsWith(subtree,t):
					// "mismatched tags "+subtree.getName()+" with "+t; -- not
					// an assertion, it's a runtime error
					// if (!pairsWith(subtree,t)) {
					// System.out.println("mismatched tags "+subtree.getName()+" with "+t+"	@ line "+linenum);
					// errcnt++; }
					// System.out.println("									   ".substring(0,parsei-1)+(parsei-1)+">"+name+"<"+(t.getAttr("width")!=null?t.getAttr("width"):""));

					/*
					 * tagcnt++; if (tagcnt % 100 == 0) pendingupdate=true; if
					 * (pendingupdate) {
					 * System.out.println("updating @ "+tagcnt); pendingupdate =
					 * false; br.repaint(); Thread.currentThread().yield(); }
					 */

					if (TAGTYPE_EMPTY == parsetype) {
						// ignore
						txtnode = null;
						n = p;

					} else if (TAGTYPE_SPAN == parsetype) {

						// OK if missequenced end tags (e.g, <i><b>some
						// text</i></b>
						// System.out.println("closing "+name);
						for (int i = pspani - 1; i >= 0; i--) {
							if (name == pspan[i].getName()) {
								// System.out.println("\tclosed @ "+i);
								pspan[i].close(p);
								// if ((attr=pspan[i].getAttr("id"))!=null)
								// anchors.put(attr, pspan[i]); => search on
								// demand
								pspani--;
								for (int j = i; j < pspani; j++)
									pspan[j] = pspan[j + 1]; // no
																// System.arraycopy
																// because this
																// should be
																// very small,
																// zero if well
																// nested (which
																// is always the
																// case in
																// XHTML)
								break;
							}
						}
						// if no matching open span, ignore close tag -- or
						// maybe have </i> close up a <b>, say

					} else { // NEST or NONEST
						txtnode = null;

						// System.out.println("closing "+p.getName()+", size="+p.size());
						// if (p.size()==16) p.dump();
						// empty internal node: all internal nodes must have at
						// least one child
						/*
						 * if (p.size()==0) { // some killed outright, others
						 * given empty leaf
						 * System.out.println("empty "+p.getName
						 * ()+", id="+id+"/"
						 * +getTag(id)+" vs P="+P+", closecnt="+closecnt); //X
						 * => P handled specially, in pre-open P if
						 * (P==id/"table"==zname /|| more to come /) { // NOT:
						 * TD/TH/TR (would mess up grid), TABLE as following
						 * TR/TD rely on a nested one, ...
						 * System.out.println("axe"); n=p; p=p.getParentNode();
						 * n.remove(); continue; } else new LeafUnicode("",null,
						 * p);
						 * //System.out.println("internal node with no children: "
						 * +p.getName()); }
						 */

						// structural -- can't enumerate because assume unknown
						// tags are structural
						// (can't throw them out because may want to save
						// preserving version)
						// else - check stack for matching. if found, pop stack
						// to match, else ignore
						// why look, have stack of pending tags right here!
						if (closecnt == 0 && name != null) {
							closecnt = matchTag(p, name, htmlroot);
							if (closecnt > 0) { // ignore spurious close even if
												// nested within tag that would
												// "match"
								int confinecnt = -1;
								if (TAG_TR == id || TAG_TD == id
										|| TAG_TH == id)
									confinecnt = matchTag(p, "table", htmlroot); // ignore
																					// these.
																					// NYT
																					// has
																					// switched
																					// tags
																					// deep
																					// in
																					// tables
																					// AND
																					// spurious
																					// </tr>
																					// that
																					// closes
																					// up
																					// table
																					// way
																					// too
																					// soon
								else if (TAG_P == id) {
									confinecnt = matchTag(p, "td", htmlroot);
									if (confinecnt == -1)
										confinecnt = matchTag(p, "th", htmlroot);
								}
								if (confinecnt != -1 && confinecnt < closecnt)
									closecnt = -1;
							}

							if (closecnt == -1) { // no corresponding open tag
													// -- usually just ignore
								// mismatched close on H1-H6
								if (id >= TAG_H1 && id <= TAG_H6) {
									int cc = 0;
									for (newp = p; newp != null; newp = newp
											.getParentNode(), cc++) {
										String hname = newp.getName();
										if (hname != null
												&& hname.length() == 2
												&& hname.charAt(0) == 'h'
												&& hname.charAt(1) >= '1'
												&& hname.charAt(1) <= '6') {
											closecnt = cc;
											break;
										}
									}
								}
							}

							// >0: close up intermediates (which might have
							// special closings)
							// <0: no match: nothing to close up
							if (closecnt != 0)
								continue;
						}

						// empty internal node: all internal nodes must have at
						// least one child
						if (p.size() == 0) { // some killed outright, others
												// given empty leaf
						// System.out.println("empty "+p.getName()+", id="+id+"/"+getTag(id)+" vs P="+P+", closecnt="+closecnt);
						// X => P handled specially, in pre-open P
							// HTML 4.01 spec:
							// "We discourage authors from using empty P elements. User agents should ignore empty P elements."
							/*
							 * if (P==id/"table"==zname /|| more to come /) { //
							 * NOT: TD/TH/TR (would mess up grid), TABLE as
							 * following TR/TD rely on a nested one, ...
							 * //System.out.println("axe"); p.remove(); // to be
							 * caught by empty case P //n=p;
							 * p=p.getParentNode(); n.remove(); } else
							 */new LeafUnicode("", null, p);
							// System.out.println("internal node with no children: "+p.getName());
						}

						n = p;
						p = p.getParentNode();
					}

					switch (id) {
					case TAG_TITLE: // not supported: lang, dir
						if (p != head_)
							head_.appendChild(n); // move to HEAD if necessary
						if (n.size() > 0 /*
										 * &&
										 * !"frame".equals(htmlroot.getParentNode
										 * ().getName())
										 */) {
							newp = (INode) n;
							StringBuffer titlesb = new StringBuffer(80);
							for (Node tn = n.getFirstLeaf(); tn != null
									&& newp.contains(tn); tn = tn.getNextLeaf())
								titlesb.append(' ').append(tn.getName());
							if (doc != null)
								doc.putAttr(Document.ATTR_TITLE, titlesb
										.substring(1));
							// if (br!=null) br.setTitle(title); -- happens as
							// part of br.setCurDocument()
						}
						// p = head_; // -- no, people stick in TITLE randomly
						break;

					// HTML 4.01 spec:
					case TAG_P:
						// if (n.size()==0) n.remove(); //
						// "User agents should ignore empty P elements." --
						// lotsa pages rely on this to make vertical space
						break;

					case TAG_PRE: // width attribute
						keepWhitespace = false;
						break;

					case TAG_MENU:
					case TAG_DIR: // deprecated; rewrite as UL (do on close for
									// tag matching code)
						n.setName("ul");
						// fall through to UL
					case TAG_UL: // make sure children are LI's
					case TAG_OL:
						break;
					case TAG_DL: // make sure children are (DT|DD)
						break;

					case TAG_CENTER: // LATER: if conincident with P or other
										// tag, just use that
						n.setName("div"); // rewrite at close so </center>
											// matches up
						break;

					case TAG_MAP: // need to handle percentage specifications
						String imagemapname = n.getAttr("name");
						if (imagemapname != null) {
							Map<Shape, String> imagemap = HTMLIMG.makeMap(n);
							if (imagemap.size() > 0) /* htmlroot */
								doc.putVar(imagemapname, imagemap); // => should
																	// use map
																	// namespace
						}
						break;

					case TAG_TABLE: // maybe move this to table class
									// ("normalizeSpans")
						((HTMLTABLE) n).normalize();
						break;

					case TAG_THEAD:
					case TAG_TFOOT:
					case TAG_TBODY: // make sure children are all TRs
					// System.out.println("closing TBODY/"+n.childNum()+", # children = "+n.size()+", child 0 = |"+n.childAt(0)+"|, first leaf = |"+n.getFirstLeaf()+"|");
						newtd = null;
						for (int i = 0, imax = n.size(); i < imax; i++) {
							Node tr = n.childAt(i);
							String trgi = tr.getName();
							if (tr.isLeaf() || "tr" != trgi) {
								if (newtd == null) {
									n.setChildAt(newp = new HTMLTR("tr", null,
											null), i);
									newtd = new HTMLTD("td", null, newp);
								} else {
									i--;
									imax--;
								}
								newtd.appendChild(tr);
							} else
								newtd = null;
						}
						// fall through to TAG_TABLE parent check
					case TAG_COLGROUP:
					case TAG_CAPTION:
						// make sure under TABLE node -- we used to do this
						// during tag open, but can generate TBODY in other
						// tags, so need at close
						if ("table" != p.getName()) {
							newp = new HTMLTABLE("table", null, null);
							p.setChildAt(newp, /* n.childNum() */p.size() - 1);
							newp.appendChild(n);
							p = newp;
						}
						break;
					case TAG_COL:
						if ("colgroup" != p.getName()) {
							newp = new INodeZero("colgroup", null, null);
							p.setChildAt(newp, /* n.childNum() */p.size() - 1);
							newp.appendChild(n);
							p = newp;
						}
						break;

					case TAG_TR: // make sure all children are TDs (Salon has
									// "&nbsp;" immediately under TABLE
						// maybe remove empty rows from tree
						// System.out.println("closing TR/"+n.childNum()+", # children = "+n.size()+", child 0 = |"+n.childAt(0)+"|, first leaf = |"+n.getFirstLeaf()+"|");
						// n.dump();
						// important to repair after parse so add an open tag
						// that's not closed properly
						INode prevtd = null;
						// boolean mutate=false;
						for (int i = 0, imax = n.size(); i < imax; i++) {
							Node td = n.childAt(i);
							if (td instanceof HTMLTD)
								prevtd = (INode) td; // td.isStruct() &&
														// ("td"==td.getName()
														// || "th"==td.getName()
							else {
								// needed in Charlie Rose weekly schedule
								// (show.html)
								// System.out.println("inserting TD");
								if (prevtd == null) {
									prevtd = new HTMLTD("td", null, null);
									n.setChildAt(prevtd, i);
								} else {
									i--;
									imax--;
								}
								prevtd.appendChild(td); // removes from old
														// parent, which was n
														// -- do this after
														// added new child, for
														// if remove old one
														// first, might detatch
														// from document tree
								// mutate=true;
								// System.out.println("moving "+td.getName());
							}
							// System.out.println("=======");
							// n.dump();
							// System.exit(0);
						}
						// if (mutate) { n.dump(); System.exit(0); }
						// System.out.println("\tafter fixup: child 0 = |"+n.childAt(0)+"|, first leaf = |"+n.getFirstLeaf()+"|");
						// System.out.println("closing TR, parent=|"+p.getName()+"|");
						if ("tbody" != p.getName() && "thead" != p.getName()
								&& "tfoot" != p.getName()) {
							newp = new INode("tbody", null, null);
							p.setChildAt(newp, /* n.childNum() */p.size() - 1);
							newp.appendChild(n);
							p = newp;
						}
						break;

					case TAG_TH:
					case TAG_TD:
						// close all pending spans started in cell when exit
						// table cell
						Leaf en = n.getLastLeaf();
						// System.out.println("</TD>, zapping "+pspani+".."+(structspanfirst[structspani-1]+1));
						for (int inpspani = structspanfirst[--structspani]; pspani > inpspani;) {
							pspani--;
							// System.out.println("</TD>, closing span w/o close tag: "+pspan[pspani].getName());
							// //+", "+sn.getName()+"/"+si+".."+en.getName()+"/"+ei);
							if (en != null)
								pspan[pspani].close(en); // bad HTML, e.g.,
															// final text is
															// "<p>"
						}
						// System.out.println("closing TD, parent=|"+p.getName()+"|");
						if ("tr" != p.getName()) {
							newp = new HTMLTR("tr", null, null);
							p.setChildAt(newp, /* n.childNum() */p.size() - 1);
							newp.appendChild(n);
							p = newp;
						}
						break;

					case TAG_SELECT:
						VMenu vm = (VMenu) ((INode) n).childAt(0);
						if (vm.getSelected() == null)
							vm.setSelected(vm.childAt(0)); // undefined
															// according to spec
							// System.out.println("setting selected to "+vm.childAt(0).getFirstLeaf());
							// for (int i=0,imax=vm.size(); i<imax; i++)
							// System.out.println("  "+vm.childAt(i));
						break;

					default:
						// OK -- no special close
					}

					// end of document (any final fix-ups?)
					// if (p==parent || "html"==name)
					// System.out.println("*** end of doc: "+p+" / "+name);
					// if (p==parent/*doc*/ || "html"==name) break; => HTML tag
					// still has to properly close up open tags by running
					// through switch
					if (p == parent/* doc */|| TAG_HTML == id/* "html"==name */)
						break;

					// *** OPEN ***
				} else { // when create new node, use literal string for name
							// where possible so get automatic interning
					if (showtags)
						System.out.println("OPEN: " + name + ": " + attrs);
					// if ("NYT_FOOTER".equals(name)) {
					// System.out.println("STOP");
					// }
					if (id != -1)
						TagUse[id]++; // -- could efficiently track tag usage
					// attrcnt += ... -- and number of attributes used

					// progressive rendering -- should be time-based and allow
					// scrolling
					prcnt += 10; // tags +=10, content +=1
					if (prcnt > 2000 && br != null) {
						// ... but not if in TABLE or probably some other bad
						// tags
						// so pages that are entirely within a table better be
						// on fast servers
						boolean now = true;
						for (INode pr = p; now && pr != parent; pr = pr
								.getParentNode()) {
							if ("table" == pr.getName())
								now = false;
						}
						// if (now) br.repaintNow(); //else prcnt--; // if bad
						// now, repaint at first opportunity?
						if (now)
							br.paintImmediately(br.getBounds());
						prcnt = 0;
					}

					// special cases (then individual tags)

					// people unbelievably casual about HEAD and its tags -- so
					// gotta special case
					// if (!(seenBODY || "html"==name || "head"==name ||
					// "title"==name || "meta"==name || "style"==name)) {
					// FRAMESET
					// *** need to generalize this transition into BODY somehow,
					// in presence of unknown tags. keep a list of known BODY
					// tags?
					// if (!seenBODY && "body"!=name && ((name.startsWith("h")
					// && name.length()==2 && Character.isDigit(name.charAt(1))
					// || "p"==name || "img"==name || "table"==name) &&
					// !"frameset"==name && "noframes"==name)) {
					if (body_ == null
							&& id != TAG_BODY
							&& ((id >= TAG_H1 && id <= TAG_H6) || TAG_P == id
									|| TAG_IMG == id || TAG_TABLE == id)
							&& TAG_FRAMESET != id && TAG_NOFRAMES != id) {
						if ((closecnt = matchTag(p, "html", htmlroot)) > 0)
							continue;
						p = body_ = new HTMLBODY("body", null, p, baseURI_);
					}

					// P==id -- id valid here?
					/*
					 * if (P==id/"p"==name /) { // P closes immediate P only, if
					 * any--doesn't clambor up tree arbitrarily high like other
					 * tags txtnode=null; // special case as P used both to
					 * start and end paragraphs if ("p"==p.getName()) { if
					 * (p.size()==0) p.remove(); //new LeafUnicode(/"X" marks
					 * the spot /" ",null, p); p=p.getParentNode(); } } else
					 */if (TAGTYPE_SPAN == parsetype) {
						// if (id==FONT)
						// System.out.println("txtnode = |"+txtnode+"|");
						// if first thing in document is open of span, then
						// p==htmlroot && p.size()==1 (it has head)
						Span ok = makeSpanType(name, id,
								(txtnode != null ? (Node) txtnode : p), attrs,
								baseURI_);
						if (ok != null) {
							pspan[pspani++] = ok;
							if ((attr = ok.getAttr("style")) != null) {
								// System.out.println(name+" => "+attr);
								CSSGeneral gs = new CSSGeneral(
										CSS.PRIORITY_INLINE/* PRIORITY_STRUCT */
												+ CSS.PRIORITY_CLASS/*
																	 * PRIORITY_ID/
																	 * PRIORITY_OBJECTREF
																	 * +10
																	 */); // no
																			// chance
																			// already
																			// exists
								ss.setAttrs(gs, attr);
								ss.put(ok, gs);
							}
							// System.out.println(" => "+ok.getAttr("style"));
						}
						// System.out.println("made span for "+name+"/"+attrs+" => "+db);
						// pspan[pspani++] = db;//makeSpanType(name, id,
						// p,p.size()-(txtnode!=null?1:0),
						// null,(txtnode!=null?txtnode.size():0), attrs,
						// baseURI_);
						// if (FONT==id)
						// System.out.println("FONT "+t.getAttributes().get("size")+" txtnode=|"+txtnode+"|");
					} else if (TAGTYPE_UNKNOWN == parsetype) { // null => not
																// EMPTY, SPAN,
																// NEST
						txtnode = null;
						// check stack for matching. if found, pop stack to
						// match, recurse. else recurse
						// not immediately self-nesting
						if (p != null && name == p.getName())
							p.getParentNode();
						// p = matchParent(p,name);
					} else {
						assert TAGTYPE_EMPTY == parsetype
								|| TAGTYPE_NEST == parsetype
								|| TAGTYPE_NONEST == parsetype;
						txtnode = null;

						// HTML 4.01 spec:
						// "The P element represents a paragraph. It cannot contain block-level elements (including P itself)."
						// if (TAGTYPE_NEST==parsetype && p.getName()=="p") {
						// closecnt=1; continue; } -- requires subsequent rescue
						// don't pop off any tags
					}

					newn = null;

					switch (id) {
					case TAG_UNKNOWN:
						// newn=p=new INode(name,attrs, p); // unrecognized
						// spans must nest. keep in parse tree for possible
						// write out
						break;

					case TAG_HTML: // O/O
						addAttrs(attrs, htmlroot);
						// newn = p = htmlroot; // else ignore
						break;

					case TAG_HEAD: // O/O
						addAttrs(attrs, head_); // every document give a default
												// HEAD at creation. if has a
												// head (or two), update
												// attributes
						if (head_.size() == 0) { // only first time(!) see head
							if ((closecnt = matchTag(p, "html", htmlroot)) > 0)
								continue;
							newn = p = head_;
						}
						// maybe put link to metadata profile in doc popup/Go
						break;

					// just create these HEAD tags under prevailing parent,
					// which bad HTML may have in BODY section; move under HEAD
					// when close so that rest of BODY not moved to HEAD
					case TAG_META:
						newn = new Leaf(name, attrs, head_); // regardless of
																// where it is
																// in document,
																// it should be
																// a child of
																// HEAD --
																// ordinarily
																// relocate in
																// close, but
																// TITLE, META,
																// BASE, LINK
																// empty!
						processMeta(newn);
						break;

					case TAG_BASE:
						newn = new Leaf(name, attrs, head_);
						if ((attr = newn.getAttr("href")) != null) {
							baseURI_ = baseURI_.resolve(attr); // *baseURL_=baseURI_.toURL();*/
																// } catch
																// (URISyntaxException
																// baduri) {}
						}
						break;

					case TAG_TITLE: // required element
						newn = p = new INodeZero(name, attrs, p); // relocate to
																	// head at
																	// close,
																	// after
																	// adding
																	// children
						break;

					case TAG_LINK:
						// practically no one uses LINK, except for point to
						// style sheet
						newn = new Leaf(name, attrs, head_);
						if ("stylesheet".equalsIgnoreCase(newn.getAttr("rel"))
								&& (attr = newn.getAttr("href")) != null) {
							// System.out.println("*** LINK to stylesheet @ "+attr+" rel to "+baseURI_);
							// java.awt.Toolkit.getDefaultToolkit().beep();
							// try {
							// ((CSS)p.getDocument().getStyleSheet()).parse((baseURI_.resolve(attr));
							// } catch (URISyntaxException e) {}
							// try {
							// ss/*((CSS)doc.getStyleSheet())*/.parse(baseURI_.resolve(attr).toURL());
							// } catch (IllegalArgumentException e) {} catch
							// (MalformedURLException e2) {}
							// URL baseURL_ = baseURI_.toURL(); // special case
							// for http://www.dli2.nsf.gov/web_style[1].css --
							// "[]" used for IPv6
							// try {
							// ss/*((CSS)doc.getStyleSheet())*/.parse(baseURI_.resolve(attr));
							// } catch (MalformedURLException e) {}
							ss.parse(baseURI_.resolve(attr).toURL());
							// catch (Exception e) {
							// System.out.println("couldn't load LINK stylesheet: "+e);
							// }
						}
						// System.out.println("****** LINK: rel="+attrs.get("rel")+", rev="+attrs.get("rev"));
						break;

					case TAG_ISINDEX: // deprecated => rewrite as FORM (at end
										// of method, after HTML/HEAD/BODY
										// battle it out for position)
						isindex = newn = new Leaf(name, attrs, null); // placeholder:
																		// rewritten
																		// at
																		// end
																		// of
																		// parse
						break;

					case TAG_STYLE:
					case TAG_SCRIPT:
						newn = new INodeZero(name, attrs, p); // for now --
																// don't set p
																// because
																// gobble
																// content
																// yourself

						// gobble until see </style> / </script>
						String endhunk = (TAG_STYLE == id ? "</style"
								: "</script"); // actually spec says any "</"
												// ends
						char endhunkch = Character.toLowerCase(endhunk
								.charAt(endhunk.length() - 1));
						StringBuffer spsb = new StringBuffer(5000); // special
																	// contents:
																	// SCRIPT,
																	// STYLE,
																	// ...
						while (true) {
							int splen = spsb.length();
							if ((ch = readChar()) == '>'
									&& splen >= 7
									&& Character.toLowerCase(spsb
											.charAt(splen - 1)) == endhunkch
									&& endhunk
											.equalsIgnoreCase(spsb
													.substring(splen
															- endhunk.length()))) { // "</style>".length
																					// --
																					// brittle
								spsb.setLength(splen - endhunk.length());
								break;
							} else
								spsb.append(ch);
						}
						String  spstr = spsb.toString();
						new Leaf(spstr, null, (INode) newn);

						// now tag-specific action
						if (TAG_STYLE == id) {
							// ((CSS)p.getDocument().getStyleSheet()).parse(spstr);
							ss
									/* ((CSS)doc.getStyleSheet()) */.parse(
											spstr, null/*
														 * baseURI_.toURL() --
														 * no imports here
														 */);
						} else if (TAG_SCRIPT == id) {
							// eval script -- which can generate more text to
							// parse, ugh!
						        script = spstr;
						       
						}
						break;
					case TAG_NOSCRIPT:
						// newn = p = new IGroup(name,attrs, p); // when link in
						// Netscape's JavaScript-in-Java, change to INodeZero
						// ignore tag -- don't put in tree!
						// LATER, when integrate JavaScript, collect tree and
						// excise
						break;

					case TAG_BASEFONT: // deprecated => update style sheet?
						// ss.get("body")).setFont...
						break;

					case TAG_FRAMESET:
						// if ((closecnt = matchTag(p,"html", htmlroot))>0 ||
						// (closecnt = matchTag(p,"html", htmlroot))>0)
						// continue;
						newn = p = new HTMLFRAMESET(name, attrs, p);
						if (pendingNOFRAMES != null) {
							p.appendChild(pendingNOFRAMES);
							pendingNOFRAMES = null;
						}
						break;
					case TAG_FRAME:
						if ((closecnt = matchTag(p, "frameset", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new HTMLFRAMESET("frameset", null, p); // don't
																		// know
																		// how
																		// to
																		// divide
																		// up
																		// FRAMESET
						newn = new HTMLFRAME(name, attrs, p/*
															 * get a long chain
															 * of info: baseURI_
															 */, baseURI_/*
																		 * should
																		 * be
																		 * baseURL_
																		 */);
						break;
					case TAG_IFRAME:
						newn = p = new HTMLIFRAME(name, attrs, p, baseURI_);
						newn = p = new INodeZero(null, null, p); // supress
																	// content
																	// (show
																	// target of
																	// SRC
																	// attribute)
						break;

					case TAG_BODY: // O/O -- can't rely on BODY appearing in
									// document
					// System.out.println("BODY matchTag NOFRAMES="+matchTag(p,"noframes",
					// htmlroot)+", matchTag HTML="+matchTag(p,"html",
					// htmlroot));
					// System.out.print("open body: "+(body==null)+", p="+p.getName());
						if (body_ != null) {
							addAttrs(attrs, body_);
							if (head_.contains(p))
								p = body_;
							continue;
						} // ignore duplicate BODY, which are more common than
							// you'd think, except to add/override attributes
						else if ((closecnt = matchTag(p, "noframes", htmlroot)) > 0)
							continue;
						else if (closecnt == -1
								&& (closecnt = matchTag(p, "html", htmlroot)) > 0)
							continue;
						// System.out.println(" => "+p.getName());

						// System.out.println("opened body under "+p.getName());
						newn = p = body_ = new HTMLBODY(name, attrs, p,
								baseURI_);
						break;

					case TAG_CENTER: // deprecated, rewrite as
										// "DIV ALIGN=CENTER" (rewrite gi at
										// close so </center> matches up)
						if (attrs == null)
							attrs = new CHashMap<Object>();
						else
							attrs.clear();
						attrs.put("align", "center");
						// fall through to DIV
					case TAG_DIV:
						newn = p = new IParaBox(name, attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setAlign(getAlign(attr));
						break;

					case TAG_H1:
					case TAG_H2:
					case TAG_H3:
					case TAG_H4:
					case TAG_H5:
					case TAG_H6: // -/- ALIGN l/r/c/j
						// stupid authoring programs nest headers!
						/*
						 * int cc=0; for (newp=p; newp!=null;
						 * newp=newp.getParentNode(), cc++) { String
						 * hname=newp.getName(); if (hname!=null &&
						 * hname.length()==2 && hname.charAt(0)=='h' &&
						 * hname.charAt(1)>='1' && hname.charAt(1)<='6') {
						 * closecnt=cc; break; } } if (closecnt>0) continue;
						 */

						// robust to unclosed list, e.g.,
						// http://www.cs.berkeley.edu/~pasula/
						assert closecnt == 0;
						String nestedin = p.getName();
						// System.out.println(name+" under "+nestedin);
						if ("ul".equals(nestedin) || "ol".equals(nestedin)
								|| "li".equals(nestedin)) {
							closecnt++;
							continue;
						}

						newn = p = new IParaBox(name, attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setAlign(getAlign(attr));
						break;

					case TAG_BLOCKQUOTE: // cite=<source>
						newn = p = new IParaBox(name, attrs, p);
						break;

					case TAG_NOBR:
						newn = p = new HTMLNOBR(name, attrs, p);
						break;

					case TAG_WBR:
						newn = new Leaf(name, attrs, p);
						break; // possible break within otherwise NOBR span --
								// Netscape-specific (worse than deprecated)

					case TAG_HR:
						newn = new HTMLHR(name, attrs, p);
						// byte b = getAlign(getAttr("align"));
						// if (b==Node.CENTER) newn.floats=b; else if
						// (b==Node.LEFT || b==Node.RIGHT) newn.align=b;
						// if ((attr = newn.getAttr("align"))!=null)
						// sgs.setAlign(getAlign(attr));
						if ((attr = newn.getAttr("align")) != null)
							newn.align = getAlign(attr); // ... until set align
															// on leaf nodes
						break;

					case TAG_P: // "cannot contain block-level elements (including P itself)"
								// -- so should close up
						if ("p" == p.getName()) {
							if (p.size() > 0) {
								closecnt = 1;
								continue;
							} // <p>text<p> => close previous <p>
							else
								newn = p; // <p><p><p>text => reuse previous <p>
						} else if (p.size() > 0
								&& (newn = p.childAt(p.size() - 1)).isStruct()
								&& newn.size() == 1 && newn.getName() == "p"
								&& ((INode) newn).childAt(0).getName() == "") {
							p = (INode) newn;
							p.removeAllChildren();
							// p.removeChildAt(p.size()-1); // <p></p><p>text...
						} else if (p.size() == 0
								&& ("td" == p.getName() || "th" == p.getName())) { // special
																					// case:
																					// <td><p>
																					// =>
																					// <td>
																					// --
																					// it's
																					// just
																					// the
																					// way
																					// it
																					// is
							newn = p; // <td><p>text...
						} else
							newn = p = new IParaBox(name, attrs, p); // new <p>
						// if ((closecnt = matchTag(p,"p", htmlroot))>0)
						// continue; -- other structural tags have to do this
						// too
						// System.out.print("new P  ");
						// if ((p.getName()!="td" && p.getName()!="th") ||
						// p.size()>0 || attrs!=null) { // don't nest within TD,
						// unless special -- Mozilla doesn't do this

						// newn=p=new IParaBox(name,attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setAlign(getAlign(attr));
						// }
						// System.out.println("*** align attribute = "+attr+"/"+getAlign(attr)+" on "+name+" => "+sgs.getAlign());
						break;

					case TAG_BR:
						newn = new HTMLBR(name, attrs, p);
						break;

					// Lists
					case TAG_MENU:
					case TAG_DIR: // deprecated; rewrite as UL at close
					case TAG_UL: // -/- (LI)+
					case TAG_OL: // -/- (LI)+
						// LATER; convert deprecated "style" and "compact" to
						// style sheet
						// LATER: convert OL's deprecated "start" to style sheet
						newn = p = new IParaBox(name, attrs, p);
						break;
					case TAG_LI: // -/O
						// LATER; convert deprecated "value" to style sheet
						// need to find OL or UL
						for (newp = p; newp != null; newp = newp
								.getParentNode())
							if ("ol" == newp.getName()
									|| "ul" == newp.getName()) {
								p = newp;
								break;
							}
						if (newp == null)
							p = new IParaBox("ul", null, p);
						newn = p = new HTMLLI(name, attrs, p); // child draws
																// own
																// bullet/number
						break;

					case TAG_DL: // -/- (DT|DD)
						newn = p = new IParaBox(name, attrs, p); // should be
																	// VBox but,
																	// e.g.,
																	// Berkeley
																	// DL Botany
																	// FAQ
						break;
					case TAG_DT: // -/O
						if ((closecnt = matchTag(p, "dl", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new IParaBox("dl", null, p); // -- empirically,
																// leave
																// structurally
																// broken
						if ((closecnt = matchTag(p, "dt", htmlroot)) >= 0) {
							closecnt++;
							continue;
						} // closecnt++ => adding a sibling vs usual child
						newn = p = new IParaBox(name, attrs, p);
						break;
					case TAG_DD: // -/O
						if ((closecnt = matchTag(p, "dl", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new IParaBox("dl", null, p); // -- empirically,
																// leave
																// structurally
																// broken
						if ((closecnt = matchTag(p, "dd", htmlroot)) >= 0) {
							closecnt++;
							continue;
						}
						newn = p = new IParaBox(name, attrs, p);
						break;

					case TAG_TABLE:
						newn = p = new HTMLTABLE(name, attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setFloats(getAlign(attr)); // LEFT, RIGHT,
															// special case
															// CENTER
						/*
						 * if ((attr = newn.getAttr("align"))!=null) { byte bval
						 * = getAlign(attr); if (bval==Node.LEFT ||
						 * bval==Node.RIGHT) sgs.setFloats(bval); else if
						 * (bval==Node.CENTER) sgs.setAlign(bval); // take all
						 * horizontal space and center within }
						 */
						if ((attrcolor = Colors.getColor(newn
								.getAttr("bgcolor"), null)) != null)
							sgs.setBackground(attrcolor);

						break;

					case TAG_THEAD:
					case TAG_TFOOT: // "TFOOT must appear before TBODY"
					case TAG_TBODY: // can have multiple TBODY for row groups
						if ((closecnt = matchTag(p, "table", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new HTMLTABLE("table", null, p); // check at
																	// close
																	// too, but
																	// put here
																	// so TBODY
																	// siblings
																	// efficiently
																	// share
																	// same
																	// parent
						newn = p = new INode(name, attrs, p);
						// LATER: align, valign for cells
						break;

					case TAG_CAPTION: // only permitted immediately after the
										// TABLE start tag
						if ((closecnt = matchTag(p, "table", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new HTMLTABLE("table", null, p);
						// newn=p=new IParaBox(name,attrs, p); => ...
						newn = p = new HTMLTD(name, attrs, p); // HTMLTD so
																// flush floats
																// at end
						// if ((attr = newn.getAttr("align"))!=null)
						// sgs.setAlign(getAlign(attr)); // top, bottom, left,
						// right -- not supported
						// must be first child of table, and only one caption
						// assert p.size()==0: "...";
						// p.insertChildAt(new IParaBox(name,attrs, null), 0);
						break;
					case TAG_COLGROUP: // span and width extracted by HTMLTABLE
						if ((closecnt = matchTag(p, "table", htmlroot)) > 0)
							continue;
						else if (closecnt == -1)
							p = new HTMLTABLE("table", null, p);
						newn = p = new INodeZero(name, attrs, p);
						break;
					case TAG_COL: // span and width extracted by HTMLTABLE
						if ((closecnt = matchTag(p, "colgroup", htmlroot)) > 0)
							continue;
						// else if ((closecnt = matchTag(p,"table",
						// htmlroot))>0) continue;
						// else if (closecnt==-1) p=new
						// INodeZero("colgroup",null, p); // sometimes implicit
						// -- at close
						newn = new LeafZero(name, attrs, p);
						break;

					case TAG_TR: // must be in THEAD, TFOOT, or TBODY. if not,
									// create TBODY
						closecnt = 0;
						// System.out.print("TR p="+p.getName()+", matchTag('tbody')="+matchTag(p,"tbody",htmlroot)+" / 'table' = "+matchTag(p,"table",htmlroot));
						String pname = null;
						for (INode pp = p; pp != htmlroot; pp = pp
								.getParentNode(), closecnt++) { // custom
																// matchTag()
							if ((pname = pp.getName()) == "thead"
									|| pname == "tfoot" || pname == "tbody"
									|| pname == "table")
								break;
						}
						// System.out.print(" => closecnt="+closecnt+" to "+p.getName());
						if (closecnt > 0)
							continue;
						// else if (p==htmlroot) { p=new HTMLTABLE("table",null,
						// p); p=new INode("tbody",null, p); } -- at close
						// else if (p.getName()=="table") p=new
						// INode("tbody",null, p); // naked TR => make a TBODY
						// -- at close
						if (p.getName() == "table")
							p = new INode("tbody", null, p); // naked TR => make
																// a TBODY
						// else thead/tfoot/tbody
						// System.out.println(" => "+p.getName());
						newn = p = new HTMLTR(name, attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setAlign(getAlign(attr)); // left, center,
															// justify, char
						if ((attr = newn.getAttr("valign")) != null)
							sgs.setVAlign(getVAlign(attr)); // top, middle,
															// bottom, baseline
						if ((attrcolor = Colors.getColor(newn
								.getAttr("bgcolor"), null)) != null)
							sgs.setBackground(attrcolor);
						break;

					case TAG_TH:
					case TAG_TD:
						int maxtdclose = matchTag(p, "table", htmlroot) - 1; // if
																				// (maxtdclose==-1)
																				// maxtdclose=Integer.MAX_VALUE;
						if (maxtdclose > 0
								&& (closecnt = Math.min(matchTag(p, "tr",
										htmlroot), maxtdclose)) > 0)
							continue;
						// else if (closecnt=matchTag(p,"table", htmlroot)>0)
						// continue; // have to have this in practice
						// else if (closecnt==-1) { p=new HTMLTR("tr",null, p);
						// System.out.println("new TR @ "+p.childNum()); }
						newn = p = new HTMLTD(name, attrs, p);

						if ((attr = newn.getAttr("align")) != null)
							sgs.setAlign(getAlign(attr)); // left, center,
															// justify, char
						else if (TAG_TH == id)
							sgs.setAlign(Node.CENTER); // new.putAttr("blow
						if ((attr = newn.getAttr("valign")) != null)
							sgs.setVAlign(getVAlign(attr)); // top, middle,
															// bottom, baseline
						if ((attrcolor = Colors.getColor(newn
								.getAttr("bgcolor"), null)) != null)
							sgs.setBackground(attrcolor);

						// for (int i=pspani-1; i>=0; i--)
						// pspan[--pspani].close(en); // => not right
						structspanfirst[structspani++] = pspani; // push all
																	// pending
																	// spans
						break;

					case TAG_FORM:
						// not special except check for required ACTION (though
						// don't complain if not found?)
						// newn=p=new IParaBox(name,attrs, p); // according to
						// spec, but FORM indiscriminantly mixed with TABLE
						// (www.nytimes.com)
						// newn=new Leaf(name,attrs, p);
						newn = p = new IParaBox(name, attrs, p); // if according
																	// to spec,
																	// preserve,
																	// but in
																	// interpretation,
																	// FORM
																	// elements
																	// don't
																	// need to
																	// be in
																	// this
																	// subtree
						radiogroups.clear();
						/*
						 * if ((attr=getAttr("action"))!=null) { try {
						 * putAttr("action", baseURI_.resolve(attr).toString());
						 * } catch (URISyntaxException ignore) {} }
						 */
						break;

					case TAG_INPUT: // subtypes: submit, reset, checkbox, radio,
									// text
						// if not in FORM already, can't fake it... hopefully
						// some FORM earlier in tree. maybe send to host
						// (path=/)?
						// warning: attrs can be null here
						String inputType = (String) attrs.get("type"); // values
																		// not
																		// canonicalized
						if (inputType != null)
							inputType = inputType.toLowerCase();
						else
							inputType = "text"; // else attrs.put("type",
												// "TEXT");
						String /*
								 * inputName=(String)attrs.get("name"),--used at
								 * collection time
								 */inputValue = (String) attrs.get("value");

						if ("hidden".equals(inputType)) {
							newn = new INodeZero(name, attrs, p);
							new LeafUnicode("", null, (INode) newn);
							// newn = new LeafZero(name,attrs, p); -- don't want
							// leaves, because ... uh
						} else if ("submit".equals(inputType)
								|| "image".equals(inputType)) {
							VButton submit = new VButton(name, attrs, p);
							if ("image".equals(inputType)
									&& (attr = getAttr("src")) != null) { // HTML
																			// hack
																			// until
																			// defined
																			// button
																			// with
																			// content
								try {
									new LeafImage(null, null, submit, baseURI_
											.resolve(attr));
								} catch (IllegalArgumentException usetext) {
								}
							}
							if (submit.size() == 0)
								new LeafUnicode(
										(inputValue != null ? inputValue
												: "Submit"), null, submit);
							// just send submitForm event, and catch
							if (submit.getAttr("script") == null && fInter)
								submit.putAttr("script", EV_FORM_SUBMIT);
							newn = submit;
						} else if ("reset".equals(inputType)) {
							VButton reset = new VButton(name, attrs, p);
							new LeafUnicode((inputValue != null ? inputValue
									: "Reset"), null, reset);
							// just send resetForm event, and catch
							if (reset.getAttr("script") == null && fInter)
								reset.putAttr("script", EV_FORM_RESET);
						} else if ("button".equals(inputType)) { // need SRIPT
																	// to do
																	// anything
							VButton button = new VButton(name, attrs, p);
							new LeafUnicode((inputValue != null ? inputValue
									: "button"), null, button);
						} else if ("checkbox".equals(inputType)) {
							VCheckbox checkbox = new VCheckbox(name, attrs, p);
							new LeafUnicode(""/*
											 * (inputValue!=null? inputValue:
											 * "checkbox")
											 */, null, checkbox);
							if (checkbox.getAttr("checked") != null)
								checkbox.setState(true);
						} else if ("radio".equals(inputType)) {
							String rgname = (String) attrs.get("name");
							VRadiogroup rg = null;
							if (rgname != null) {
								rgname = rgname.toLowerCase();
								rg = radiogroups.get(rgname);
								if (rg == null)
									radiogroups.put(rgname,
											rg = new VRadiogroup());
							}
							VRadiobox radiobox = new VRadiobox(name, attrs, p,
									rg);
							new LeafUnicode(""/*
											 * (inputValue!=null? inputValue:
											 * "")
											 */, null, radiobox);
							if (rg != null
									&& (radiobox.getAttr("checked") != null || rg
											.getActive() == null))
								rg.setActive(radiobox);
						} else if ("file".equals(inputType)) {
							INode filelayout = new IHBox("file", null, p);
							new VEntry("filetypein", null, filelayout, 40,
									(inputValue != null ? inputValue : ""));
							new HTMLFileButton("filebutton", null, filelayout);
							newn = filelayout;
							// value give initial file name
						} else /*
								 * if ("text".equals(inputType) ||
								 * inputType==null || bad spec )
								 */{ // TEXT, PASSWORD, null, unrecognized,
										// error (I've seen TYPE=name) => TEXT
							// if ("password".equals(inputType)) ...
							// if (!"text".equals(inputType))
							// putAttr("type","TEXT"); // fix
							newn = new VEntry(name, attrs, p, 0/*-1*/,
									(String) attrs.get("value"));
							((VEntry) newn).setWidthChars(Integers.parseInt(
									newn.getAttr("size"), 20));
							if (inputType == null
									|| !"password".equals(inputType))
								newn.putAttr("type", "TEXT");
							if (newn.getAttr("script") == null && fInter)
								newn.putAttr("script", EV_FORM_SUBMIT);
							// System.out.println("result of INPUT in HTML");
							// newn.dump();
						}

						// LATER: common setting text or image content here
						break;
					case TAG_BUTTON: // with content as opposed to INPUT
										// TYPE=BUTTON--it's about time
						VButton button = new VButton("button", attrs, p);
						newn = p = button;
						String type = newn.getAttr("type"); // type=submit,
															// reset, button--in
															// which case need
															// SCRIPT to do
															// anything
						if ("submit".equalsIgnoreCase(type) && fInter)
							newn.putAttr("script", EV_FORM_SUBMIT);
						if ("reset".equalsIgnoreCase(type) && fInter)
							newn.putAttr("script", EV_FORM_RESET);
						break;
					case TAG_TEXTAREA:
						VTextArea textarea = new VTextArea("textarea", attrs, p);
						textarea.setSizeChars(Integers.parseInt(textarea
								.getAttr("cols"), 60), Integers.parseInt(
								textarea.getAttr("rows"), 20));
						newn = textarea;
						p = (INode) textarea;// .childAt(0);
						// suck text verbatim until </TEXTAREA>
						// System.out.println("DUMP"); newn.dump(); p.dump();
						break;
					case TAG_SELECT:
						newn = p = new VMenuButton("select", attrs, p);
						new VMenu(null, null, p); // null name == not
													// actual/valid content
						// if MULTIPLE or SIZE>=2, then use VList<> instead
						break;
					case TAG_OPTION:
						if ((closecnt = matchTag(p, "select", htmlroot)) > 0)
							continue;
						else if (closecnt == -1) {
							p = new VMenuButton("select", null, p);
							new VMenu(null, null, p);
						}
						p = (INode) p.childAt(0); // get the associated menu
						newn = new IHBox("option", attrs, p);
						if (newn.getAttr("selected") != null)
							((VMenu) p).setSelected(newn);
						p = (INode) newn;
						break;
					case TAG_FIELDSET:
						newn = p = new IParaBox(name, attrs, p);
						break;
					case TAG_OPTGROUP: // cascaded menu
						// for now strip!
						// newn = => nothing!
						break;

					case TAG_IMG: // not deprecated but could use the more
									// general object
						if ((HINT_NO_IMAGE /*
											 * FIX: and HINT_NO_LAYOUT or have
											 * width/height attributes
											 */& getHints()) != 0) {
							new Leaf(name, attrs, p);
							break;
						}

						// LATER: initially set image to broken image taken from
						// JAR resource
						URI imguri = null;
						try {
							// System.out.println("SRC="+attrs.get("src")+" vis-a-vis "+baseURI_);
							String src = (attrs != null ? (String) attrs
									.get("src") : null);
							src = (src != null ? src.trim() : "missing.gif");
							imguri = baseURI_.resolve(src);
							// System.out.println("IMG URI = "+imguri);
							// } catch (URISyntaxException ei) { /*href=from
							// resources. was: img = LeafImage.broken;*/
						} catch (IllegalArgumentException ignore) { // e.g.,
																	// http://ad.doubleclick.net/ad/N771.salon/B955669.14;sz=144x48;ord=[timestamp]?
							if (DEBUG)
								System.err.println("URI syntax in "
										+ attrs.get("src"));
						}
						/*
						 * catch (IllegalArgumentException ae) { String src =
						 * (String)attrs.get("src"); ae.printStackTrace();
						 * System
						 * .out.println(src+" vis-a-vis "+baseURI_+", len="
						 * +src.length()+", char 67="+(int)src.charAt(67));
						 * System.exit(1); }
						 */
						Leaf l = new HTMLIMG(name, attrs, p, imguri);
						newn = l;
						// br.prepareImage(img, l); => load on demand?

						if ((attr = newn.getAttr("align")) != null) {
							byte bval = getAlign(attr);
							// if (bval==Node.LEFT || bval==Node.RIGHT)
							// sgs.setFloats(bval); else if (bval!=Node.NONE)
							// sgs.setVAlign(bval);
							// until IMG/APPLET/OBJECT leaves looked up in style
							// sheet, HACK
							if (bval == Node.LEFT || bval == Node.RIGHT)
								newn.floats = bval;
							else if (bval != Node.NONE)
								newn.valign = getVAlign(attr);
							// System.out.println("align attribute = "+attr+"/"+getAlign(attr)+" on "+name);
						}

						if ((ival = Integers
								.parseInt(newn.getAttr("border"), 0)) > 0)
							sgs.setBorder(ival);
						if ((ival = Integers
								.parseInt(newn.getAttr("hspace"), 3)) > 0) {
							sgs.marginleft = sgs.marginright = ival;
						}
						if ((ival = Integers
								.parseInt(newn.getAttr("vspace"), 3)) > 0) {
							sgs.margintop = sgs.marginbottom = ival;
						}
						break;

					// FABIO: Video Tag to test embedded video
					case TAG_VIDEO:
						URI videoUri = null;
						
						try {
							String src = (attrs != null ? (String) attrs
									.get("src") : null);
							if (src == null)
								break;
							videoUri = baseURI_.resolve(src);
						} catch (IllegalArgumentException ignore) { // e.g.,
																	// http://ad.doubleclick.net/ad/N771.salon/B955669.14;sz=144x48;ord=[timestamp]?
							if (DEBUG)
								System.err.println("URI syntax in "
										+ attrs.get("src"));
						}

						//Map<String, Object> vattrs = new HashMap<String,Object>(1);
						attrs.put("resize", false);
						attrs.put("embedded", true);
						attrs.put("uri", videoUri);
						
//						for (Entry a:attrs.entrySet()){
//							attrs.put((String) a.getKey(), a.getValue());
//							System.out.println(a.getKey()+"  =  "+ a.getValue());
//						}
						ClassLoader clo = getClass().getClassLoader();
							try {
						Class cl = Class.forName("uk.ac.liverpool.fab4.LeafVideo", true, clo);
						Class<Leaf> lc = cl.asSubclass(Leaf.class);
						Constructor<Leaf> c = lc.getConstructor(String.class , Map.class, INode.class);
						Leaf vid = c.newInstance("video", attrs, p);
						System.out.println("Video tag");
						if (vids == null) vids = new Vector<Leaf>(4);
						vids.add(vid);
						newn = vid;
							

						if ((attr = newn.getAttr("align")) != null) {
							byte bval = getAlign(attr);
							// if (bval==Node.LEFT || bval==Node.RIGHT)
							// sgs.setFloats(bval); else if (bval!=Node.NONE)
							// sgs.setVAlign(bval);
							// until IMG/APPLET/OBJECT leaves looked up in style
							// sheet, HACK
							if (bval == Node.LEFT || bval == Node.RIGHT)
								newn.floats = bval;
							else if (bval != Node.NONE)
								newn.valign = getVAlign(attr);
							// System.out.println("align attribute = "+attr+"/"+getAlign(attr)+" on "+name);
						}

						if ((ival = Integers
								.parseInt(newn.getAttr("border"), 0)) > 0)
							sgs.setBorder(ival);
						if ((ival = Integers
								.parseInt(newn.getAttr("hspace"), 3)) > 0) {
							sgs.marginleft = sgs.marginright = ival;
						}
						if ((ival = Integers
								.parseInt(newn.getAttr("vspace"), 3)) > 0) {
							sgs.margintop = sgs.marginbottom = ival;
						}
							} catch (ClassNotFoundException e){
								e.printStackTrace();
								System.out.println("Video is not supported. Missing media engine");
							}
							
						
						break;
					case TAG_PRE: // already partially digested by parser? no,
									// children done below
						keepWhitespace = true;
						newn = p = new IVBox(name, attrs, p);
						if ((attr = newn.getAttr("align")) != null)
							sgs.setFloats(getAlign(attr));
						break;

					case TAG_APPLET: // deprecated => convert to OBJECT
					    // translate attributes into OBJECT's
					case TAG_OBJECT:
					    // FABIO: handle JT viewer object 
					    if (((String)attrs.get("classid")).equals("clsid:AD0DEF5C-DEC1-4950-AC57-1533F90C6BAD")) {
					        viewerid = ((String)attrs.get("id"));
					        clo = getClass().getClassLoader();
					        try {
					            Class cl = Class.forName("uk.ac.liverpool.fab4.jreality.SoftViewerLeaf", true, clo);
					            Class<Leaf> lc = cl.asSubclass(Leaf.class);
					            Constructor<Leaf> c = lc.getConstructor(String.class , Map.class, INode.class);        
					            attrs.put("width",""+Integers.parseInt((String)attrs.get("width"), 400));
					            attrs.put("height",""+Integers.parseInt((String)attrs.get("height"), 400));
					            
					            Leaf vid = c.newInstance("3d", attrs, p);
					            
					            newn = vid;
					            viewerObject = vid;
					        } catch (Exception x){
					            x.printStackTrace();
					        }
					    } else { 
						newn = new LeafUnicode("object", attrs, p);
						if ((ival = Integers
								.parseInt(newn.getAttr("border"), 0)) > 0)
							sgs.setBorder(ival);
						if ((ival = Integers
								.parseInt(newn.getAttr("hspace"), 3)) > 0) {
							sgs.marginleft = sgs.marginright = ival;
						}
						if ((ival = Integers
								.parseInt(newn.getAttr("vspace"), 3)) > 0) {
							sgs.margintop = sgs.marginbottom = ival;
						}
					    }
						break;

					case TAG_MAP:
						newn = p = new INode("map", attrs, p);
						break;
					case TAG_AREA:
						newn = new Leaf(name, attrs, p);
						break;

					case TAG_NOFRAMES:
						// if ((closecnt = matchTag(p,"noframe", htmlroot))>0)
						// continue;
						newn = p = new INodeZero(name, attrs,
								(curFRAMESET != null ? curFRAMESET : p));
						break;

					case TAG_NOLAYER: // can't ignore, which the HTML convention
										// for unfamiliar tags says you should:
										// have to recognize and ignore content
						// if ((closecnt = matchTag(p,"noframe", htmlroot))>0)
						// continue;
						newn = p = new INodeZero(name, attrs, p);
						break;

					default:
						if (TAGTYPE_SPAN != parsetype) { // SPAN out of band
							// unrecognized -- preserve so can write out
							// (corrected) copy -- assume type NEST
							// newn=p=new INode(name,attrs, p); // no
							// unrecognized spans
						}
						break;
					}

					// *** END of OPEN
					if (newn != null) {
						// element-specific style
						if ((attr = newn.getAttr("style")) != null)
							ss.setAttrs(sgs, attr);

						// CSSGeneral tweaked by hardcoded attributes
						if ((attrs != null || TAG_TH == id)/*--TH w/o attrs sets align=CENTER; would kill attrs!=null but have to check performance*/
								&& !EMPTYGS.equals(sgs)) {
							ss.put(newn, sgs);
							// System.out.println("new sgs @ "+newn.getName()+": "+attrs+" => "+sgs+", sgs.align="+sgs.getAlign());
							sgs = new CSSGeneral(CSS.PRIORITY_BLOCK
									+ CSS.PRIORITY_CLASS/*
														 * PRIORITY_ID/PRIORITY_OBJECTREF
														 */);
						}

						// if ((attr=newn.getAttr("id"))!=null)
						// anchors.put(attr, newn);
					}
				}

			} else { // CONTENT
				if (body_ == null && p == htmlroot
						&& !Character.isWhitespace(ch)) { // needed for people
															// who halfway down
															// ASCII document
															// start putting in
															// tags
					// if ((closecnt = matchTag(p,"html", htmlroot))>0)
					// continue;
					// System.out.println("switching into BODY at seeing content: "+ch+"/"+(int)ch);
					p = body_ = new HTMLBODY("body", null, p, baseURI_);
				}

				ir_.unread(ch); // restore first character
				// txtraw.setSize(0); -- later maintain running StringBuffer
				// txtraw.append(readString());
				// validate(parsei>0,
				// "shouldn't begin document with content: "+content);

				// String content =
				// (txtnode!=null?txtnode.getName():"")+readString('\0', '\0',
				// "<& \t\r\n", '&');
				String content = readString();
				// if (txtnode!=null &&
				// Character.isWhitespace(content.charAt(0))) badconcatcnt++;
				// //System.out.println("old txtnode: >"+txtnode.getName()+"<");
				// // + >"+content+"<");
				/*
				 * if (txtnode!=null) { if (!keepWhitespace &&
				 * Character.isWhitespace(content.charAt(0)))
				 * System.out.println("extra concat: "+content); if
				 * (!Character.isWhitespace(content.charAt(0))) content =
				 * txtnode.getName() + content; // plus space in between? else
				 * txtnode=null; }
				 */
				// String content =
				// (txtnode!=null?txtnode.getName():"")+readString(); // concat
				// even when not needed!
				// int clen = content.length(); if (clen==0) content=""; else if
				// (clen==1 && content.charAt(0)<0x100)
				// content=Strings.valueOf(content.charAt(0)
				// wrap naked content in a paragraph tag
				// if (p.getParentNode()==doc) p=new IParaBox("P",null, p);
				// System.out.println("content: >"+content+"<");
				if (p == htmlroot) {
					// nothing
				} else if (keepWhitespace) {
					// System.out.println("content = "+content);
					for (int i = 0, imax = content.length(); i < imax;) {
						int nextcr = content.indexOf('\n', i);
						if (nextcr == -1)
							nextcr = imax;
						int vend = nextcr
								+ (nextcr > 0
										&& content.charAt(nextcr - 1) == '\r' ? -1
										: 0);
						String hunk = content.substring(i, vend);
						// System.out.println("hunk=|"+hunk+"|");
						if (txtnode == null)
							txtnode = new LeafUnicode(hunk, null, p);
						else
							txtnode.setName(txtnode.getName() + hunk); // cr's
																		// implicit
																		// in
																		// PRE's
																		// IVBox
						if (nextcr < imax
								&& (content.charAt(nextcr) == '\r' || content
										.charAt(nextcr) == '\n'))
							txtnode = null;
						i = nextcr + 1;
					}
					// System.out.println("txtnode = "+txtnode);

				} else {
					// for now, leaf=word, but later longer, bounded by
					// structural tags, maybe limited to 1000 chars

					if (content.length() > 0
							&& Character.isWhitespace(content.charAt(0)))
						txtnode = null;
					// invariant: start=first non-whitespace after run of
					// whitespace (or at position 0)
					for (int i = 0, imax = content.length(); i < imax;) {
						int start = i;
						while (start < imax
								&& Character
										.isWhitespace(content.charAt(start)))
							start++;
						i = start + 1;
						while (i < imax
								&& !Character.isWhitespace(content.charAt(i)))
							i++;
						if (start >= imax) {
							// don't make a node exclusively of whitespace
							// txtnode=null;
							// => on second thought, give spans something to
							// hold onto
							// if (txtnode==null) new LeafUnicode(" ",null,p);
							txtnode = null;
						} else /* if (start<i) */{
							String hunk = i - start == 1 ? Strings
									.valueOf(content.charAt(start)) : content
									.substring(start, i);
							// System.out.println("hunk="+hunk);
							// if (i-start==1 && content.charAt(start)<0x100)
							// System.out.println("single-char String "+(++sccnt));
							if (txtnode == null)
								txtnode = new LeafUnicode(hunk, null, p);
							else
								txtnode.setName(txtnode.getName() + hunk);
							prcnt++;
							if (i < imax)
								txtnode = null;
						}
					}
				}
			}
		}

		// assert no remaining nested tags. // everybody implicitly closed.
		// LATER: if no children add "this space intentionally left blank"

		// System.out.println("done parsing");
		// htmlroot.dump();
		// if (htmlroot.getLastLeaf()==null) htmlroot.dump();
		// attach remaining pending spans
		Leaf en = htmlroot.getLastLeaf();
		if (en != null)
			while (pspani > 0)
				pspan[--pspani].close(en);
		assert Span.closeAll(htmlroot) == 0;

		if (head_.size() == 0)
			new Leaf("", null, head_);

		/*
		 * // INode absvis = doc.getVisualLayer("multivalent.node.IRootAbs");
		 * LeafUnicode msg = new LeafUnicode("TEST of ABSOLUTE",null, absvis);
		 * msg.bbox.setLocation(150,100); // use 150,70 to put on top of
		 * hyperlinks in links.html
		 * 
		 * INode screenvis = doc.getVisualLayer("multivalent.node.IRootScreen");
		 * LeafUnicode screenmsg = new LeafUnicode("TEST of SCREEN",null,
		 * screenvis); screenmsg.bbox.setLocation(200,150);
		 */

		// System.out.println("html child = "+htmlroot.childAt(1)+", baseURI_="+baseURI_);
		if (isindex != null)
			rewriteISINDEX(isindex, p, htmlroot);

		/*
		 * long delta = System.currentTimeMillis()-timein; if (delta>0)
		 * System.out
		 * .println("parsing time "+delta+" ms, "+badconcatcnt+" bad concat");
		 * // widgets typically instantaneous if (badtags.size()>0)
		 * System.out.println(badtag.size()+" non-HTML tags: "+badtags);
		 */
		// System.out.println("SELECTED TAG USAGE");
		boolean ftrack = false;
		if (Booleans.parseBoolean(getPreference("DebugMode", null), false))
			for (int i = 0, imax = tracktag.length; i < imax; i++) {
				id = tracktag[i];
				if (TagUse[id] > 0) {
					ftrack = true;
					System.out.print(tagList_[id] + "=" + TagUse[id] + "	");
				}
			}
		if (ftrack)
			System.out.println(); // some GUI widgets use HTML

		ir_.close();
		// System.out.println("done building HTML tree "+htmlroot.getFirstLeaf().getName());
		return htmlroot/* doc */; // already added to passed root, but maybe
									// not used that way
		// return doc;
	}

	// if match along path to root, return that, else be idempotent
	/*
	 * INode matchParent(INode p, String matchme) { for (INode m=p; m!=null;
	 * m=m.getParentNode()) { if (matchme.equals(m.getName())) {
	 * p=m.getParentNode(); break; } } return p; }
	 */

	/**
	 * Climb tree loking for matching tag. Tag names must be interned (even if
	 * not HTML tag) as "==" is used in comparison.
	 * 
	 * @param top
	 *            stop at document top (sometimes top of TABLE?), not go up up
	 *            up to root
	 */
	int matchTag(INode n, String matchme, Node top) {
		assert n != null && matchme != null && top != null;

		int cnt = 0;

		// matchme can be null if put null-named node into tree
		// check HTML interned strings first
		// if (top!=null) top = top.getParentNode();
		for (top = top.getParentNode()/* match on top */; n != top/* !=null */; n = n
				.getParentNode(), cnt++) {
			String name = n.getName();
			// System.out.println("matchTag "+n.getName()+" vs "+matchme);
			// use .equals() over == because have non-HTML tags that much match
			// (.equals() not more expensive than == when are ==)
			if (/* matchme==name || */matchme.equals(name))
				return cnt;
		}

		return -1; // not found
	}

	// abstracted so parsing flow clearer and can clean up all unclosed spans at
	// end of document
	static Map<String, Object> bigattrs = new CHashMap<Object>(1),
			smallattrs = new CHashMap<Object>(1);
	static {
		bigattrs.put("size", "+1");
		smallattrs.put("size", "-1");
	}

	Span makeSpanType(String name, int id, Node sn,/* int si, int ei, */
			Map<String, Object> attrs, URI baseURI) {
		// assert name!=null && parseType[id] == TAGTYPE_SPAN && baseURI!=null:
		// name+" "+id+" "+parseType[id]+" "+baseURI; // base null
		// System.out.println("makeSpanType, name="+name+", id="+id+" (A="+A+", FONT="+FONT+", I="+I+")");

		// 1. pick span
		Span span = null;

		String bename = (attrs != null ? (String) attrs.get(ATTR_BEHAVIOR)
				: null); // HTML extension to use Multivalent behavior, such as
							// ScriptSpan
		if (bename == null)
			switch (id) {
			// LATER: parser rewrites FONT as style sheet spec
			case TAG_FONT:
				span = new HTMLFONT(name);
				span.restore(null, attrs, scratchlayer_);
				break; // deprecated, but not obvious how to efficiently
						// translate to style sheet
			case TAG_BIG:
				span = new HTMLFONT(name);
				span.restore(null, bigattrs, scratchlayer_);
				break;
			case TAG_SMALL:
				span = new HTMLFONT(name);
				span.restore(null, smallattrs, scratchlayer_);
				break;

			case TAG_A:
				// System.out.println("A tag, href="+attrs.get("href")+" vs "+attrs.get("HREF"));
				// modernize "name" to universal "id"
				String aname = (String) attrs.get("name");
				if (attrs.get("id") == null && aname != null) {
					attrs.put("id", aname);
					attrs.remove("name");
				}

				// "href"
				String attr;
				if ((attr = (String) attrs.get("href")) != null) {
					// protocols: ftp, http, mailto, gopher + about, manpage,
					// ...
					if (attr.startsWith("mailto")
							|| attr.startsWith("javascript")) {
					    if (viewerid!=null   ) {
					        if ((script!= null && script.indexOf(viewerid)!=-1) || attr.indexOf(viewerid)!=-1){
					           String toOpen = attr.substring(attr.indexOf('\'')+1, attr.lastIndexOf('\''));
					           URI href = baseURI.resolve(URIs.fix(toOpen));
					           HyperlinkSpan hspan = (HyperlinkSpan) new HTMLACTION(viewerObject, href);
                                                   hspan.restore(null, attrs, scratchlayer_);
                                                   hspan.setTarget(href);
                                                   
                                                   hspan.setSeen(getGlobal().getCache().isSeen(href)); // have
                                                
                                                   span = hspan; 
					        }
					    }
					    
					} // not implemented yet -- retain for now
					else
						try {
							URI href = baseURI.resolve(URIs.fix(attr)); // canonicalize
																		// for
																		// index
																		// to
																		// URIs
																		// seen
							// System.out.println(href);
							// if (attr.indexOf('%')!=-1)
							// System.out.println("resolve "+attr+" vs "+baseURI+" => "+href.toString()+" / "+href.toASCIIString());
							// +href.getPath()+"/"+href.getRawPath());
							// if (href.getRef()!=null)
							// System.out.println("ref = "+href.getRef());
							/*
							 * if (sn!=null && sn==en && si==0 && ei==1 && sn
							 * instanceof HTMLIMG) { // special because of image
							 * maps ((HTMLIMG)sn).putHref((URI)href);
							 * //System.out
							 * .println("special casing span on "+sn);
							 * Toolkit.getDefaultToolkit().beep(); } else {
							 */

							// HyperlinkSpan hspan =
							// (HyperlinkSpan)Behavior.getInstance("HTMLA",
							// attrs, scratchlayer_); -- HTMLA not public
							HyperlinkSpan hspan = (HyperlinkSpan) new HTMLA();
							hspan.restore(null, attrs, scratchlayer_);
							hspan.setTarget(href);
							hspan.setSeen(getGlobal().getCache().isSeen(href)); // have
																				// link
																				// do
																				// this
																				// during
																				// paint?
							span = hspan; // pick up moveq at end
							// }
						} catch (IllegalArgumentException badhref) { /* ignore */
						}
				}

				if (span == null)
					bename = "multivalent.Span";

				break;

			case TAG_BLINK:
				bename = "BlinkSpan";
				break;
			case TAG_SUB:
			case TAG_SUP:
				bename = "SubSupSpan";
				break;

			/*
			 * // following display only and handled by style sheet case
			 * TAG_DEL: // later special span? case TAG_INS: // cite=uri,
			 * datetime=cs, title -- hyperlink if cite (in doc popup?), do
			 * something with datetime case TAG_B: case TAG_STRONG: case TAG_I:
			 * case TAG_EM: case TAG_CITE: case TAG_VAR: case TAG_ADDRESS: //
			 * WRONG => display: block case TAG_TT: case TAG_CODE: case TAG_KBD:
			 * case TAG_SAMP: case TAG_U: case TAG_STRIKE: case TAG_S: case
			 * TAG_Q: // span: add quotes appropriate for language, nested go
			 * double quotes to single case TAG_ABBR: case TAG_ACRONYM: // look
			 * up in acronym server? that's a general behavior case TAG_DFN:
			 * case TAG_BDO: case TAG_LEGEND: case TAG_LABEL: // nothing special
			 * for now, just retain in tree case TAG_SPAN:
			 * 
			 * default: // style sheet defined new tag to have "display: span"
			 * bename = "multivalent.Span"; break;
			 */
			}

		// 2. instantiate span
		if (span == null) {
			if (bename != null)
				try {
					span = (Span) Behavior.getInstance(name, bename, attrs,
							scratchlayer_);
				} catch (Exception e) {
				} // Behavior not a Span

			// 'A' with name/id only or bad href, retain all protocols, a name
			// w/o href
			if (span == null)
				span = (Span) Behavior.getInstance(name, "multivalent.Span",
						attrs, scratchlayer_);
		}

		// 3. configure span
		if (span != null) {
			switch (id) { // set non-attr
			// case TAG_A: anchors.add(span); break; => done in AnchorSpan
			// case TAG_A:
			// ((AnchorSpan)span).setAnchorName((String)attrs.get("name"));
			// break;
			// case TAG_A: if (attrs.get("href")!=null)
			// ((HyperlinkSpan)span).putTarget(baseURI_); break;
			case TAG_SUB:
				((SubSupSpan) span).setDelta(Integer.MIN_VALUE/*-5*/);
				break;
			case TAG_SUP:
				((SubSupSpan) span).setDelta(Integer.MAX_VALUE/* +5 */);
				break;
			default: // OK
			}

			span.open(sn);
			// if (id==FONT)
			// System.out.println("added span "+name+" "+sn+"/"+si+".. null/"+ei);
		} // else span=new Span(); // plain span to retain attrs

		return span;
	}

	// end of tags
	// **************************************************************************

	/** Rewrite the deprecated ISINDEX from leaf in HEAD, to FORM in BODY. */
	void rewriteISINDEX(Node isindex, INode p, INode htmlroot) {
		if (htmlroot.size() >= 2
				&& "body" == (p = (INode) htmlroot.childAt(1)).getName()) {
			INode newp = new IParaBox("p", null, null);
			Node newn = newp;
			p.insertChildAt(newn, 0);
			// new HTMLHR("hr",null, newp); // traditional style
			newn = new Leaf("form", null, newp);
			// radiogroups.clear(); -- why?
			if (baseURI_ != null)
				newn.putAttr("action", baseURI_.getScheme() + "://"
						+ baseURI_.getAuthority() + "/");
			newn.putAttr("method", "POST");
			new LeafUnicode(isindex.getAttr("prompt",
					"This is a searchable index. Enter search keywords:"),
					null, newp);
			newn = new VEntry("input", null, newp); // no NAME
			// newn.putAttr("script",
			// "event "+Document.MSG_OPEN+" "+action+"$TEXT"); // need to
			// URLEncode words.
			if ((getHints() & HINT_NO_INTERACTIVE) == 0/* fInter out of scope */)
				newn.putAttr("script", EV_FORM_SUBMIT);
			// new HTMLHR("hr",null, newp);
			new HTMLBR("br", null, newp);
			// no submit button, oddly
		}
	}

	/** Process various META tags. */
	void processMeta(Node n) {
		String attr = n.getAttr("http-equiv");

		// http-equiv => HTTP headers
		if (attr != null) {
			attr = attr.toLowerCase();

			/*
			 * http://www.w3.org/TR/REC-html40/struct/global.html#edef-META
			 * states
			 * "Authors should not use this technique to forward users to different pages, as this	makes the page inaccessible to some users."
			 * However, everybody does.
			 */
			if ("refresh".equals(attr)) { // <META HTTP-EQUIV ="Refresh" CONTENT
											// = "0; URL=index.html">
				attr = n.getAttr("content");
				try {
					URI refreshURI = baseURI_;
					StringTokenizer st = new StringTokenizer(attr, "; \t\n\r");
					String sec = st.nextToken();
					while (st.hasMoreTokens()) {
						String tok = st.nextToken();
						int eqi = tok.indexOf('=');
						if (eqi != -1) {
							// String n=tok.substring(0,eqi),
							// v=tok.subString(eqi+1);
							if ("URI".equalsIgnoreCase(tok.substring(0, eqi))) {
								// String val =
								// VScript.getVal(tok.substring(eqi+1), doc,
								// attrs); //,
								// "systemressource:/sys/About.html");
								String val = tok.substring(eqi + 1);
								refreshURI = baseURI_.resolve(val);
							}
							// System.out.println("refreshURI = "+refreshURI);
						}
					}
					long refreshat = /* System.currentTimeMillis() + */1000 * Integer
							.parseInt(sec);

					// if both new time and URI succeed, hook up to timer
					// getGlobal().getInstance().getTimer().addObserver(this);
					final URI frefreshURI = refreshURI;
					tt = new TimerTask() {
						public void run() {
							getBrowser().eventq(Document.MSG_OPEN, frefreshURI);
						}
					};
					getGlobal().getTimer().schedule(tt, refreshat);

				} catch (NumberFormatException nfe) {
				} catch (IllegalArgumentException male) {
				}

			} // else ... add to HTTP headers

			// copy Author, Title, ... to enclosing Document
			// Dublin Core-ish: author, title, ...
		} // else "".equals(...) //

	}

	/** Adds LINKs to Go menu and document popup. */
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg))
			return true;
		else if (VMenu.MSG_CREATE_GO == msg
				|| (DocumentPopup.MSG_CREATE_DOCPOPUP == msg && se.getIn() != getBrowser()
						.getSelectionSpan())) { // -- just plain-doc popup
			String cat = (VMenu.MSG_CREATE_GO == msg ? "Go" : "NAVIGATE");
			INode menu = (INode) se.getOut();
			for (int i = 0, imax = (head_ != null ? head_.size() : 0); i < imax; i++) {
				Node n = head_.childAt(i);
				String gi = n.getName(), rel = n.getAttr("rel"), title = n
						.getAttr("title"), href = n.getAttr("href");
				if (title == null)
					title = rel;
				if ("link".equals(gi) && href != null
						&& (rel == null || !rel.equalsIgnoreCase("stylesheet"))) {
					try {
						createUI("button", title, "event " + Document.MSG_OPEN
								+ " " + baseURI_.resolve(href), menu, cat,
								false);
					} catch (IllegalArgumentException male) {
					}
				}
			}
		}
		return false;
	}

	/**
	 * Form processing. Later, submittedForm so can intercept for client-side
	 * forms processing.
	 */
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Object arg = se.getArg();
		Browser br = getBrowser();

		if (MSG_FORM_SUBMIT == msg) {
			submitForm((Node) arg, msg);
			return true;

		} else if (MSG_FORM_PROCESS == msg) {
			// gave client-side forms processors a chance, now submit to web
			// server
			System.out.println("sending form to server: " + arg);
			br.eventq(Document.MSG_OPEN, arg);
			return true;

		} else if (MSG_FORM_RESET == msg) {
			submitForm((Node) arg, msg);
			// else if ("submittedForm"==msg) submitForm((Node)arg, true); =>
			// "processForm"
			return true;

		} else if (MSG_FORM_POPULATE == msg) {
			submitForm((Node) arg, msg, (Map<Object, Object>) se.getIn());
			// System.out.println("* * POPULATE");
			return true;
		}

		return super.semanticEventAfter(se, msg);
	}

	/**
	 * <p>
	 * An old implementation followed the HTML spec -- in which FORM controls
	 * are contained within its subtree. But HTML in the wild does not put
	 * content in form hierarchically, and there's no way to fix with parsing
	 * special cases, as in general FORMs intermingled with other structural
	 * entities, especially tables, so no tree can simultaneously capture both.
	 * (Can't put table in form because table may have multiple forms.)
	 */
	void submitForm(Node formOrSubmit, String act) {
		submitForm(formOrSubmit, act, null);
	}

	void submitForm(Node formOrSubmit, String act, Map<Object, Object> pop) {
		assert act == MSG_FORM_SUBMIT || act == MSG_FORM_RESET
				|| act == MSG_FORM_POPULATE;

		// find enclosing FORM = first FORM earlier in tree
		// with bad HTML this is not necessarily an ancestor of the submit
		// button
		// OK to take first as forms not nested (in parser, new FORM closes any
		// prevailing one)
		Node start = null, end = null; // start of form, start of next form =
										// end of this + 1 node
		if (formOrSubmit == null || !getDocument().contains(formOrSubmit))
			return;

		// walk leaves, scan up tree until find FORM. Keep list of scanned nodes
		// so don't repeat work (though lotsa scanning)
		// not so fast, but don't have to be as just do one time per page at
		// most, and then swamped by network
		/*
		 * in the wild, FORM not necessarily in parent while (form!=null &&
		 * !"form".equals(form.getName())) {
		 * System.out.println("searching for FORM GI: "+form.getName());
		 * form=form.getParentNode(); }
		 */

		// System.out.println("SUBMIT FORM @ "+formOrSubmit+", docURI_="+docURI+", "+hashCode()+", "+pop);
		// find first FORM earlier in tree (if HTML is good and has FORM as head
		// of hierarchy, then this finds it fast)
		List<Node> seen = new ArrayList<Node>(100);
		for (Node n = formOrSubmit.getFirstLeaf(); n != null && start == null; n = n
				.getPrevLeaf()) {
			for (Node m = n.getParentNode(); m != null; m = m.getParentNode()) {
				if (seen.indexOf(m) != -1)
					break;
				if ("form".equals(m.getName())) {
					start = m;
					break;
				}
				seen.add(m);
				// System.out.println("\tsearching "+m);
			}
		}
		// System.out.println("head = "+start);
		if (start == null)
			return; // should be guaranteed by virtue of construction

		String method = start.getAttr("method", "get").toLowerCase().intern(); // get|post
		String action = start.getAttr("action");
		if (action != null)
			action = action.intern();// if (action==null) return; -- could be
										// "event"

		StringBuffer sb = new StringBuffer(200); // valid to collect hash, then
													// paste string in random
													// order?

		// for (Leaf
		// l=form.getFirstLeaf(),lastl=form.getLastLeaf().getNextLeaf(); l!=null
		// && l!=lastl; l=l.getNextLeaf()) {
		// Stack dfs = new Stack(); // depth-first search of tree
		seen.clear();
		VMenu lastselect = null;
		for (Node n = start.getFirstLeaf(); n != null && end == null; n = n
				.getNextLeaf()) {
			int knowngood = sb.length(); // searching will overstep into next
											// FORM, so checkpoint known good
											// state and truncate back when go
											// too far
			for (INode m = n.getParentNode(); m != null; seen.add(m), m = m
					.getParentNode()) {
				if (seen.lastIndexOf(m) != -1)
					break; // seen everything higher too. add new at end so
							// start searching there

				String gi = m.getName();
				// System.out.println(gi);
				if ("form".equals(gi) && m != start) {
					end = m;
					sb.setLength(knowngood);
					break;
				}

				String name = m.getAttr("name"), value = m.getAttr("value");
				if (act == MSG_FORM_POPULATE) {
					if (name == null
							|| (value = (String) pop.get(name)) == null)
						continue;
					// if (name!=null)
					// System.out.println("populate "+name+" with "+value);
				}

				if (gi == null || name == null)
					continue; // null VALUE ok
				boolean checked = m.getAttr("checked") != null;
				if ("input".equals(gi)) {
					String type = m.getAttr("type"); // if (type==null)
														// continue;
					// System.out.println("INPUT, TYPE = |"+type+"|");
					if (type != null)
						type = type.toLowerCase();
					// if (reset) ... else ...
					if ("hidden".equals(type)) {
						// ok with attrs as is, both for submit and reset... and
						// populate?
					} else if (type == null || "text".equals(type)
							|| "password".equals(type)) { // for now, password
															// same as TEXT
						if (m instanceof VEntry) {
							VEntry entry = (VEntry) m;
							if (act == MSG_FORM_SUBMIT)
								value = entry.getContent();
							else
								entry.setContent(value);
							// System.out.println("value = "+value);
						}
					} else if ("checkbox".equals(type)) {
						if (m instanceof VCheckbox) {
							VCheckbox checkbox = (VCheckbox) m;
							if (act == MSG_FORM_SUBMIT) {
								if (!checkbox.getState())
									name = value = null; // if off, cancel
							} else {
								checkbox.setState(checked);
							}
						}
					} else if ("radio".equals(type)) {
						if (m instanceof VRadiobox) {
							VRadiobox radiobox = (VRadiobox) m;
							if (act == MSG_FORM_SUBMIT) {
								if (!radiobox.getState())
									name = value = null; // if off, cancel
							} else {
								VRadiogroup rg = radiobox.getRadiogroup();
								radiobox
										.setState(checked
												|| (rg != null
														&& rg.getGroup().size() > 0 && rg
														.getGroup().get(0) == m));
							}
						}
					} else if ("file".equals(type)) {
						boolean valid = false;
						value = null;
						for (int i = 0, imax = m.size(); i < imax
								&& (value == null || !valid); i++) {
							Node child = m.childAt(i);
							if (child instanceof VEntry)
								value = ((VEntry) child).getContent();
							if (child instanceof HTMLFileButton)
								valid = true;
						}
						if (!valid)
							value = null;
					} else if ("submit".equals(type)) {
						if (m != formOrSubmit)
							name = value = null;
					} else
						name = value = null; // reset, ...

				} else if ("textarea".equals(gi)) {
					if (m instanceof VTextArea) {
						VTextArea textarea = (VTextArea) m;
						if (act == MSG_FORM_SUBMIT)
							value = textarea.getContent(); // else ??
					}

				} else if ("select".equals(gi)) {
					// System.out.println("SELECT "+name+"= "+((VMenu)m.childAt(0)).getSelected().getAttr("value"));
					if (m instanceof VMenuButton) {
						VMenu vm = (VMenu) m.childAt(0);
						if (act == MSG_FORM_SUBMIT) {
							Node sel = vm.getSelected();
							value = sel.getAttr("value");
							if (value == null) {
								value = "";// ((INode)sel).childAt(0).getName();
											// => <BR> in DL!
								for (Leaf l = sel.getFirstLeaf(), e = sel
										.getLastLeaf().getNextLeaf(); l != e; l = l
										.getNextLeaf()) {
									if (l.bbox.width > 0 && l.bbox.height > 0)
										value += " " + l.getName();
								}
								value = value.trim();
								// System.out.println("value = |"+value+"|");
							}
						} else
							vm.setSelected(vm.childAt(0)); // default to first,
															// or none if
															// multiple; changed
															// by CHECKED on
															// OPTIONs
						lastselect = vm;
					}
				} else if ("option".equals(gi)) {
					if (act != MSG_FORM_SUBMIT && checked && lastselect != null)
						lastselect.setSelected(m);

				} else
					name = value = null;

				if (name != null && value != null/* null value ok? */) {
					// System.out.println(gi+" => "+name+"="+value);
					sb.append('&').append(name).append('=').append(
							URIs.encode(value));
				}
			}
		}

		// if (5>4) {
		// System.out.println("FORM method="+method+", action="+action+" => "+sb.substring(0));
		// } else
		System.out.println("FORM method=" + method + ", action=" + action
				+ " => " + sb);
		if (act == MSG_FORM_SUBMIT /* && sb.length()>0 */) { // && action!=null
																// => action not
																// needed for
																// client-side
																// processing

			Browser br = getBrowser();
			// only works for GET
			// maybe later send all submits though "processForm" for client-side
			// massaging
			if ("get" == method) {
				if (action != null)
					try {
						/* if (action.indexOf('?')==-1)/Hao's form */sb
								.setCharAt(0, '?'); // replace first '&' with
													// '?'
						sb.insert(0, (action.indexOf('?') == -1 ? action
								: action.substring(0, action.indexOf('?'))));
						// System.out.println("FORM/get: baseURI = "+baseURI_+", sb="+sb);
						// if (baseURI_!=null)
						br.eventq(Document.MSG_OPEN, baseURI_.resolve(sb
								.toString()));
					} catch (IllegalArgumentException badaction) {
					}
			} else if ("post" == method) {
				if (action != null)
					try {
						DocInfo di = new DocInfo(baseURI_.resolve(action)); // resolved
																			// against
																			// baseURI_
																			// during
																			// parsing
						di.method = "POST";
						di.attrs.put("POST", sb.substring(1)/* +"\r\n" */); // skip
																			// first
																			// '&'
						System.out.println("posting to |" + di.uri + "|, with "
								+ sb.substring(1));
						br.eventq(Document.MSG_OPEN, di);
					} catch (IllegalArgumentException badaction) {
						System.out.println("error posting " + badaction);
					}
			} else { // client-side forms processing: method='event',
						// action='<eventName>'
				// make name-value hash table for easy use by clients
				StringTokenizer st = new StringTokenizer(sb.substring(0), "&");
				Map<String, String> map = new CHashMap<String>(
						st.countTokens() + 1);
				if (start.getAttr("name") != null)
					map.put("name", start.getAttr("name"));
				while (st.hasMoreTokens()) {
					String nameval = st.nextToken();
					int eq = nameval.indexOf('=');
					if (eq != -1)
						map.put(nameval.substring(0, eq), URIs.decode(nameval
								.substring(eq + 1)));
					else
						map.put(nameval, "");
				}

				// System.out.println("sending internal event 1");
				if ("event".equals(method)) {
					// with a behavior already loaded in document
					SemanticEvent se = new SemanticEvent(br,
							(action != null ? action
									: SystemEvents.MSG_FORM_DATA), map,
							formOrSubmit, null); // so client-side forms
													// processor can run
					// System.out.println("sending internal event 2");
					br.eventq(se);
				} /*
				 * else if ("class"==method) { // arbitrary class that
				 * implements FormProcessor (not a Behavior) try { Object o =
				 * Class.forName(action).newInstance(); // not a behavior, so
				 * don't use Behavior.getInstance... but no remapping if (o
				 * instanceof EventListener) { ((EventListener)o).event(se,
				 * null); // want to pass node so callee can update page } }
				 * catch (Exception ignore) {} }
				 */
			}
		}
	}

	/**
	 * TARGET-aware hyperlink. Shared by A HREF and IMG MAP
	 */
	public static void go(Node startn, Object replace, Object ouri) {
		Browser br = startn.getBrowser();
		INode replacep = null;
		String target = null;
		String cmd = null;
		Object eattrs = null;
		// Document doc = startn.getDocument();
		Root root = br.getRoot();
		Node found = null;
		Node n;

		if (replace instanceof String)
			target = (String) replace;
		else if (replace instanceof INode)
			replacep = (INode) replace;

		// String target = startn.getAttr("target"); => parameter
		if (replacep == null && target == null) {
			found = startn.getDocument().findDFS(Layer.BASE);
			if (found != null)
				target = found.getAttr("target");
		}
		if (DEBUG && target != null)
			System.out.println("*** TARGET = " + target + ", search from "
					+ startn.getName());

		if (replacep != null) {
			// done!

		} else if (null == target || "_self".equals(target)) {
			for (n = startn; n != root; n = n.getParentNode()) {
				if ("frame".equals(n.getName()) || "iframe".equals(n.getName())) {
					replacep = (INode) n;
					break;
				}
				// System.out.println("*** checking = "+n.getName());
			}
			// if (startn.getName().startsWith("3") && replacep==null)
			// System.exit(0);
			if (replacep == null /* ||replacep==docroot */) {
				cmd = Document.MSG_OPEN;
				eattrs = ouri;
			} // if not in FRAME, same as _top

		} else if ("_parent".equals(target)) { // immediate FRAMESET parent
			for (n = startn; n != root; n = n.getParentNode()) {
				if ("frameset".equals(n.getName())) {
					replacep = (INode) n;
					break;
				}
			}
			if (replacep == null /* ||replacep==docroot */) {
				cmd = Document.MSG_OPEN;
				eattrs = ouri;
			} // if not in FRAMESET, same as _top

		} else if ("_blank".equals(target)) {
			cmd = Browser.MSG_NEW;
			Map<String, Object> emap = new CHashMap<Object>();
			emap.put("uri", ouri);
			eattrs = emap;

		} else if ("_top".equals(target)) { // full, original window
			cmd = Document.MSG_OPEN;
			eattrs = ouri;

		} else { // search for target name
			// LATER: need getDocContentRoot()
			// dfs in current window
			// if FRAME has same name as IFRAME but comes after in DFS, then
			// these two lines return wrong result
			found = br.getRoot().findDFS("frame", "name", target);
			if (found == null)
				found = br.getRoot().findDFS("iframe", "name", target);
			// else dfs in all windows
			for (Iterator<Browser> i = Multivalent.getInstance()
					.browsersIterator(); found == null && i.hasNext();) {
				Browser otherdoc = i.next();
				if (otherdoc != br)
					found = br.getRoot().findDFS("frame", "name", target);
			}
			// else create new named instance
			if (found != null) {
				replacep = (INode) found;
			} else {
				cmd = Browser.MSG_NEW;
				Map<String, Object> emap = new CHashMap<Object>();
				emap.put("uri", ouri);
				emap.put("target", target);
				eattrs = emap;
			}
		}

		if (replacep != null) {
			try {
				DocInfo di = new DocInfo(new URI(ouri.toString()));
				replacep.removeAllChildren();
				di.doc = new Document("HTML", null, replacep);
				if (br != null)
					br.eventq(Document.MSG_OPEN, di);
			} catch (Exception e) {
				new LeafUnicode(e.toString(), null, replacep);
			}
			/*
			 * replacep.removeAllChildren(); //if (replacep instanceof
			 * IScrollPane) ((IScrollPane)replacep).getVsb().setValue(0); --
			 * part of content //HTML html = (doc!=null?
			 * (HTML)Behavior.getInstance("HTML","HTML",null,
			 * (Layer)br.getLayer(Layer.BASE)): new HTML()); HTML html =
			 * (doc!=null? (HTML)Behavior.getInstance("HTML","HTML",null,
			 * (Layer)br.getCurDocument().getLayers().getBehavior(Layer.BASE)):
			 * new HTML());
			 * System.out.println("replacing "+replacep.getName()+"/"
			 * +target+" with "
			 * +ouri+", layer="+br.getCurDocument().getLayers().getBehavior
			 * (Layer.BASE)); try { Document doc = new Document("HTML",null,
			 * replacep); URI uri = new URI(ouri.toString()); html.docURI = uri;
			 * html.parse(uri.openStream(), doc); doc.markDirtySubtree(false);
			 * // essential! (goes up tree) if (doc!=null) br.repaint(1000); }
			 * catch (Exception e) { // ignore System.err.println("error "+e); }
			 */
		} else if (cmd != null) {
			if (DEBUG)
				System.out.println("HTML event " + cmd + "  " + eattrs);
			br.eventq(cmd, eattrs);
		} // else can't make heads or tails of it
	}



	// content for createUI can be specified as HTML
	// private static MediaAdaptor htmlparser__ = null;
	// private static Root htmlroot__;

	/*
	 * Helper method to parse String as HTML. public INode helper(String str) {
	 * MediaAdaptor html =
	 * (MediaAdaptor)Behavior.getInstance("helper","HTML",null, getLayer());
	 * html.setInput(new java.io.StringReader(report(grace, surl,
	 * RobustHyperlink.getSignatureWords(sig)))); INode full =
	 * (INode)html.parse(doc); return doc; }
	 */

	static final Pattern SUFFIX_PAT = Pattern.compile("\\.html?$",
			Pattern.CASE_INSENSITIVE);
	static final Pattern MAJOR_PAT = Pattern.compile(
			"</?(html|head|meta|h[1-5])[^>]*>", Pattern.CASE_INSENSITIVE);
	static final Pattern MINOR_PAT = Pattern.compile(
			"</?(p|br|a|ul|ol|li|dl|dt|dd|center|table|tr|td|hr|div|span|font)[^>]*>",
			Pattern.CASE_INSENSITIVE);

	static final int MAJOR_MATCH_CNT = 2;
	static final int MINOR_MATCH_CNT = 10;

    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);

        if (inRange(min, HEURISTIC, max) || inRange(min, MAGIC, max)) {
            BufferedReader in = new BufferedReader(getInputUni().getReader());
            String line;
            int majorFound = 0;
            int minorFound = 0;
            Confidence checkAt = inRange(min, HEURISTIC, max) ?
                    HEURISTIC :
                    MAGIC;
            for (int i = 0; i < 1000 && (line = in.readLine()) != null; i++) {
                Matcher m = MAJOR_PAT.matcher(line);
                while (m.find()) {
                    if (++majorFound >= MAJOR_MATCH_CNT) {
                        infos.add(new IDInfo(checkAt, this, "text/html"));
                        return infos;
                    }
                }

                if (checkAt == HEURISTIC) {
                    m = MINOR_PAT.matcher(line);
                    while (m.find()) {
                        if (++minorFound >= MINOR_MATCH_CNT) {
                            infos.add(new IDInfo(checkAt, this, "text/html"));
                            return infos;
                        }
                    }
                }
            }
        } else if (inRange(min, SUFFIX, max)) {
            if (path != null && SUFFIX_PAT.matcher(path).find()) {
                infos.add(new IDInfo(SUFFIX, this, "text/html"));
            }
        }

        return infos;
    }

    public void setInput(String txt) {
		_txt = txt;

	}
}

/**
 * Like IParaBox but know about HTML-specific things class HParaBox extends
 * IParaBox { }
 */

/*
 * Make HTML layout classes available for general use: in UI layout, ... So make
 * put in package multivalent.adaptor
 * 
 * + have symbolic instantiation as done above or at least make them inner
 * classes
 */

// attributes on all elements: id, class,
// attributes: VERSION (deprecated by DTD declaration), [lang], [dir]
/**
 * Root of HTML document. Replace parent's style sheet with CSS and populate.
 * <p>
 * LATER: support lang and dir attributes. Read default style sheet from file.
 * which may be in user space.
 */

// class HTMLParaBox extends IParaBox {
// }
/**
 * Deprecated: BACKGROUND Deprecated attributes translated into equivalent in
 * style sheet: text, bgColor, link, alink, vlink. No effect: profile.
 * Unsupported: onload/onunload/other events. Would like to translate everything
 * into style sheet and axe this class, but don't presently support background
 * image in style sheet and have new HTMLBODY() in several places.
 */
class HTMLBODY extends IParaBox implements ImageObserver, ContextListener {
	Image background = null;

	HTMLBODY(String name, Map<String, Object> attrs, INode parent, URI baseURI) {
		super(name, attrs, parent);

		// after possible filtering, establish cached values from attributes
		// do this business here rather than in HTML class because of nested
		// frames (FRAME, IFRAME, maybe Notes)

		// => do this during parsing as we do for other tags. need to keep
		// "background", though.

		// all these deprecated in HTML 4.0, but will have to keep around
		// indefinitely, alas
		String attr;
		Color c;
		StyleSheet ss = getDocument().getStyleSheet();
		CSSGeneral gs = new CSSGeneral(CSS.PRIORITY_BLOCK + CSS.PRIORITY_CLASS/*
																			 * PRIORITY_ID/
																			 * LOT
																			 */);
		// if (gs==null) { System.out.println("NO BODY"); System.exit(1); }
		if ((attr = getAttr("text")) != null
				&& (c = Colors.getColor(attr)) != null)
			gs.setForeground(c);
		if ((attr = getAttr("bgcolor")) != null
				&& (c = Colors.getColor(attr)) != null)
			gs.setBackground(c);
		// System.out.println("fg="+gs.getForeground()+", bg="+gs.getBackground());
		if (!HTML.EMPTYGS.equals(gs))
			ss.put("body", gs);

		// => A:link, A:visited, A:active
		if ((attr = getAttr("link")) != null
				&& (c = Colors.getColor(attr)) != null) {
			gs = new CSSGeneral(CSS.PRIORITY_INLINE + CSS.PRIORITY_CLASS/*
																		 * PRIORITY_ID/
																		 * PRIORITY_OBJECTREF
																		 */);
			gs.setForeground(c);
			gs.setUnderline(Context.COLOR_INHERIT);
			ss.put("a:link", gs);
		}
		if ((attr = getAttr("vlink")) != null
				&& (c = Colors.getColor(attr)) != null) {
			gs = new CSSGeneral(CSS.PRIORITY_INLINE + CSS.PRIORITY_CLASS/*
																		 * PRIORITY_ID/
																		 * PRIORITY_OBJECTREF
																		 */);
			gs.setForeground(c);
			gs.setUnderline(Context.COLOR_INHERIT);
			ss.put("a:visited", gs);
		}
		if ((attr = getAttr("alink")) != null
				&& (c = Colors.getColor(attr)) != null) {
			gs = new CSSGeneral(CSS.PRIORITY_INLINE + CSS.PRIORITY_CLASS/*
																		 * PRIORITY_ID/
																		 * PRIORITY_OBJECTREF
																		 */);
			gs.setForeground(c);
			gs.setUnderline(Context.COLOR_INHERIT);
			ss.put("a:active", gs);
		}

		if ((attr = getAttr("background")) != null) {
			try {
				background = Toolkit.getDefaultToolkit().getImage(
						baseURI.resolve(attr).toURL());
				// bgColor = null; => can't do this in a CSSGeneral since
				// null==not valid, and don't do this now since the image may
				// not load successfully
				// if ((attr=getAttr("bgcolor"))!=null &&
				// (c=Colors.getColor(attr))!=null)
				// ((CSSGeneral)ss.get("body")).setBackground(c);
				/* if (background!=null) */ss.put(this, this);
			} catch (IllegalArgumentException badbackuri) {
			} catch (MalformedURLException badbackurl) {
			}
		}

		// leftmargin, rightmargin, topmargin, bottommargin, marginwidth
		// Microsoft specific?
		/*
		 * int lm=-1, rm=-1, tm=-1, bm=-1, mw=-1, mh=-1; if
		 * ((attr=getAttr("leftmargin"))!=null &&
		 * (c=Colors.getColor(attr))!=null) { gs=new
		 * CSSGeneral(CSS.PRIORITY_INLINE+CSS.PRIORITY_CLASS);
		 * gs.setForeground(c); gs.setUnderline(Context.COLOR_INHERIT);
		 * ss.put("a:link", gs); }
		 */
	}

	public boolean appearance(Context cx, boolean all) {
		// cx.foreground = textColor;
		// if (background!=null) cx.pagebackground = cx.background=null; // use
		// image instead -- null is transparent
		// else if (bgColor!=null) cx.pagebackground=cx.background=bgColor;
		return false;
	}

	public int getPriority() {
		return CSS.PRIORITY_BLOCK/* PRIORITY_OBJECTREF */;
	}

	// for background image
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
		boolean loaded = (infoflags & ImageObserver.ALLBITS) != 0;
		if (loaded) { /* bgcolor=null; */
			getBrowser().repaint(250);
		}
		return !loaded;
	}

	/** Fills out to full width x height available. */
	public boolean formatNode(int width, int height, Context cx) {
		// System.out.println("BODY layout "+width);
		boolean ret = super.formatNode(width, height, cx);
		// System.out.println("BODY: w="+width+", h="+height);
		// bbox.setSize(width,height); // fill up all available space for
		// background color/image
		// bbox.add(width,height); // make sure point at lower right is included
		bbox.width = Math.max(bbox.width, cx.getFloatWidth(BOTH));
		cx.flush = BOTH;
		bbox.height += cx.flushFloats(width); // flush remaining

		int effw = width, // - border.left-border.right -
							// padding.left-padding.right,
		effh = height;// - border.top-border.bottom -
						// padding.top-padding.bottom;
		if (width < PROBEWIDTH / 2 && height > 0)
			bbox.setSize(Math.max(bbox.width, effw), Math
					.max(bbox.height, effh)); // => put in HTML tag?
			// System.out.println("BODY: w="+width+"=>"+bbox.width+", h="+height+"=>"+bbox.height);

		return ret;
	}

	public void paintNode(Rectangle docclip, Context cx) {
		if (background != null) {
			int w = background.getWidth(this), h = background.getHeight(this);
			if (w != -1 && h != -1) {
				Graphics2D g = cx.g;
				int pt = padding.top, pb = padding.bottom, pl = padding.left, pr = padding.right;
				int xoff = docclip.x - (docclip.x % w) - pl, yoff = docclip.y
						- (docclip.y % h) - pt;
				// System.out.println("xoff = "+xoff+", docclip.x="+docclip.x+", body pad left = "+padding.left+", w="+w+", mod="+(docclip.x%w)+", h="+h);

				// for (int i=0,imax=docclip.height/h+1/*+1 for integer
				// truncation*/, y=yoff; i<imax; i++, y+=h) {
				int imax = (docclip.height + pt + pb) / h + 2/*
															 * extra for top and
															 * bottom
															 */, jmax = (docclip.width
						+ pl + pr)
						/ w + 2;
				for (int i = 0, y = yoff; i < imax; i++, y += h) {
					for (int j = 0, x = xoff; j < jmax; j++, x += w) {
						// g.drawImage(background, j*w+xoff, i*h+yoff,
						// cx.background/*bgColor*/, this);
						g.drawImage(background, x, y,
								cx.background/* bgColor */, this);
					}
				}
			}
		}
		super.paintNode(docclip, cx);
	}
}

/**
 * Sets bbox width to 0, height to current font height + whatever additional
 * needed to clear pending floats. BR is last element on current line, as
 * opposed to first on line following break.
 */
class HTMLBR extends Leaf {
	HTMLBR(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
	}

	public boolean formatNodeContent(Context cx, int start, int end) {
		valid_ = true;
		return !valid_;
	}

	public boolean breakBefore() {
		return false;
	}

	public boolean breakAfter() {
		return true;
	} // BR of a line, the last element on line

	public boolean formatNode(int width, int height, Context cx) {
		boolean shortcircuit = super.formatNode(width, height, cx); // gotta
																	// keep

		// This is ugly but BR is freakish: its height depends on other words on
		// line.
		// If sole word on line, then height is prevailing text height; else,
		// it's bbox is 0x0 and only the break after flag has any effect.
		int num = childNum();
		INode p = getParentNode();
		Node prev = (num > 0 ? p.childAt(num - 1) : null);
		int h = 0;
		if (prev == null || prev.breakAfter()) {
			NFont f = cx.getFont();
			h = (int) f.getHeight();
			baseline = h - (int) f.getDescent();
		} else
			h = baseline = 1/* 0 */; // not only word on line, so take min of
										// other words

		// flush floats?
		String clear = getAttr("clear"); // none, left, right, all
		byte align = Node.NONE;
		if ("left".equalsIgnoreCase(clear))
			align = Node.LEFT;
		if ("right".equalsIgnoreCase(clear))
			align = Node.RIGHT;
		else if ("all".equalsIgnoreCase(clear))
			align = Node.BOTH;
		// if flushing, set height to 0 so not taller than flushed floats(?)
		if (align != Node.NONE) {
			cx.flush = align;
			h = baseline = 0;
		}

		bbox.setSize(0, h); // "<br> <br>..." eats up whitespace -- was
							// setSize(1,h) but some tables give no width for BR

		valid_ = !shortcircuit;
		return shortcircuit;
	}

	public boolean paintNodeContent(Context cx, int start, int end) {
		return false;
	}
}

class HTMLHR extends Leaf {
	int barwidth, size; // WxH
	static final int PADDING = 2; // since Leaf, don't get padding field

	public boolean breakBefore() {
		return true;
	}

	public boolean breakAfter() {
		return true;
	}

	// attributes NOSHADE (solid), SIZE=(thinkness), WIDTH="xx%", ALIGN= all
	// easy to implement
	HTMLHR(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
	}

	/**
	 * Formats to full width in bbox, then painted according to "width"
	 * attribute.
	 */
	public boolean formatNode(int width, int height, Context cx) {
		super.formatNode(width, height, cx); // handle stickies

		cx.flush = BOTH; // who knew?

		valid_ = (width > 0 && width < PROBEWIDTH / 2);

		size = Integers.parseInt(getAttr("size"), 2);
		int w = 1;
		if (valid_) {
			w = width - cx.getFloatWidth(BOTH); // overall bbox gets full width
												// available so painting and
												// events intersect; maintain
												// separate width for drawing
			barwidth = HTML.getAbsPct(getAttr("width", "100%"), w);
			// if (barwidth > width) ...
		}
		bbox.setSize(w, size + PADDING * 2);
		baseline = size + PADDING;

		return false; // !valid_; => don't shortcircuit even if not valid!
	}

	/**
	 * HR has to handle own LEFT and RIGHT align, as IParaBox needs those set on
	 * it itself.
	 */
	public boolean paintNodeContent(Context cx, int start, int end) {
		int xoff = 0;
		if (/* cx. */align == CENTER || /* cx. */align == RIGHT) {
			int hdiff = bbox.width - barwidth;
			if (hdiff > 0)
				xoff = (align == CENTER ? hdiff / 2 : hdiff);
		}

		Graphics2D g = cx.g;
		g.setColor(cx.foreground);
		if (getAttr("noshade") != null)
			g.fillRect(xoff, PADDING, barwidth, size);
		else
			g.draw3DRect(xoff, PADDING, barwidth, size, true);
		return false;
	}

	/** Usually leaf nodes don't care about prevailing width, but HR does. */
	public void markDirtySubtreeDown(boolean leavestoo) {
		setValid(false);
	}
}

class HTMLLI extends IParaBox {
	/** Width of UL's symbol. */
	public static final int SWIDTH = 6; // size of symbol unaffected by font
										// size

	static final int INVALID = Integer.MIN_VALUE;
	int value = INVALID; // number for OLs, nesting level for UL

	HTMLLI(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
	}

	/**
	 * Compute number for OL and nesting level for UL here, rather than
	 * scrambling during painting.
	 */
	public boolean formatNode(int width, int height, Context cx) {
		boolean ret = super.formatNode(width, height, cx); // room left for
															// bullet or number
															// done by style
															// sheet for UL and
															// OL

		// VALUE=<whole-number> -- since next item is VALUE++, going to have to
		// compute these at layout time, as don't want to scramble arbitrarily
		// far looking for VALUE among children
		INode p = getParentNode();
		int newvalue = INVALID;
		if (p != null && "ol".equals(p.getName())) {
			newvalue = Integers.parseInt(getAttr("value"), INVALID);
			if (newvalue == INVALID) {
				for (int cn = childNum() - 1; cn >= 0; cn--) { // assumes always
																// format
																// top-down
					Node prev = p.childAt(cn);
					if (prev instanceof HTMLLI) {
						newvalue = 1 + ((HTMLLI) prev).value;
						break;
					} // prev of type HTMLLI not guaranteed by parser
				}
				if (newvalue == INVALID)
					newvalue = Integers.parseInt(p.getAttr("start"), 1); // first
																			// LI
																			// under
																			// OL
																			// --
																			// LATER:
																			// take
																			// from
																			// style
																			// sheet
			}
		} else { // nesting level for UL
			newvalue = 0;
			for (INode pp = p; pp != null; pp = pp.getParentNode()) {
				String ppname = pp.getName();
				if ("ul".equals(pp.getName()))
					newvalue++;
				else if (!"li".equals(ppname))
					break;
			}
			// System.out.println("level = "+newvalue);
		}

		// if value changed, invalidate following siblings so they recompute
		// their value
		if (value != INVALID && newvalue != value)
			for (int i = childNum() + 1, imax = p.size(); i < imax; i++)
				p.childAt(i).setValid(false);
		value = newvalue;

		return ret;
	}

	/** Draw number or symbol in the left margin. */
	public void paintNode(Rectangle docclip, Context cx) {
		super.paintNode(docclip, cx);

		Graphics2D g = cx.g;
		g.setColor(cx.foreground);

		// dynamically determine if in OL or UL and display appropriately
		Node n = getFirstLeaf(); // if (n==null) return; -- check for no
									// children done by paintBeforeAfter(...)
									// (?)
		Point loff = n.getRelLocation(this);
		NFont f = cx.getFont();
		int sw = (int) f.charAdvance(' ').getX();
		int x = loff.x - sw - sw, y = loff.y + n.baseline - 1;// (int)f.getAscent();
																// // of max too
																// big;//n.baseline;

		INode p = getParentNode();
		String type = p.getAttr("type"); // LATER: take from style sheet
											// ("list-style-type")
		if ("ol".equals(p.getName())) {
			// should compute text according to style, such as a/b/c, i/ii/iii,
			// Roman numerals
			// LI: TYPE=(1|A|a|i|I)
			String txt;
			char ctype = (type == null ? '1' : type.charAt(0));
			// String sval=null;
			if (ctype == 'a' || ctype == 'A')
				txt = "" + (char) (ctype + value - 1); // why doesn't
														// String.valueOf((char)...)
														// work?
			else if ((ctype == 'i' || ctype == 'I') && value > 0
					&& value < 4000) {
				// txt = Utility.int2Roman(value); // replace this when Java can
				// compute Roman numerals, but this good enough for now
				txt = Integers.toRomanString(value);
				if (ctype == 'i')
					txt = txt.toLowerCase();
			} else
				txt = Integer.toString(value);

			f
					.drawString(g, txt, x - (float) f.stringAdvance(txt).getX()
							- 2, y);
			g.drawLine(x, y - 1, x, y - 1); // period

		} else { // UL: TYPE=(disc|circle|square)
			if (type == null)
				type = (value == 1 ? "circle" : "fill-square"); // compute based
																// on nesting
																// level

			// x-=SWIDTH; y-=SWIDTH;
			if ("fill-square".equals(type))
				g.fillRect(x - SWIDTH, y - SWIDTH, SWIDTH, SWIDTH);
			else if ("disc".equals(type))
				g.fillOval(x - SWIDTH, y - SWIDTH, SWIDTH, SWIDTH);
			else if ("square".equals(type))
				g.drawRect(x - SWIDTH, y - SWIDTH, SWIDTH, SWIDTH);
			else
				/* if ("circle".equals(type)) */f.drawString(g, "o", x
						- (float) f.stringAdvance("o").getX(), y); // g.drawOval(x,y,
																	// SWIDTH,SWIDTH);
																	// -- looks
																	// awful!
		}
	}
}

/*
 * attributes: SUMMARY, ALIGN=(left|right|center), WIDTH, bgcolor (background
 * color) frame, rules, border (borders and rules) cellspacing, cellpadding
 * (cell margins)
 */
/**
 * Implement HTML TABLEs, which are hairy ugly beasts. < 900 lines, compared
 * with 13000+ for Mozilla. Doesn't quite do all that Mozilla does (COLGROUP and
 * COL styles not passed to TD), but when it does, still less than an order of
 * magnitude smaller.
 */
class HTMLTABLE extends INode {
	static final boolean DEBUG = false;

	static final int MINPRIORITY = 0, NATURAL = 0, MULTILEN = 1, ABSOLUTE = 2,
			PERCENTAGE = 3, MAXPRIORITY = 3; // last has highest priority order
												// in gobbling space

	int cellspacing = 1, cellpadding = 0;
	Insets cellpadinsets = INSETS_ZERO; // => CSS padding, somehow
	int border = 0; // width (in pixels) of the frame around a table --
					// non-standard: doesn't include caption
	String frame, rules;
	Node caption = null, tfoot = null;
	int row1; // TBODY, past any CAPTION/THEAD/TFOOT
	// int xoff; // in centered table, so can draw rules
	Insets auxborder = INSETS_ZERO; // can't use standard border because CAPTION
									// not in border
	/**
	 * Format through TBODY and TR to pick up styles, and TR climbs up to TABLE
	 * to get column width.
	 */
	/* public */int[] colw = null;
	int minwidth = -1, maxwidth = -1;

	HTMLTABLE(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);

		cellspacing = Integers.parseInt(getAttr("cellspacing"), 1);
		cellpadding = Integers.parseInt(getAttr("cellpadding"), 0);
		if (cellpadding > 0)
			cellpadinsets = (cellpadding < INSETS.length ? INSETS[cellpadding]
					: new Insets(cellpadding, cellpadding, cellpadding,
							cellpadding));

		String sborder = getAttr("border");
		if (sborder == null)
			border = 0;
		else if (sborder.equals("border")/* sborder==HTML.DEFINED */)
			border = 1;
		else
			try {
				border = Math.max(Integer.parseInt(sborder), 0);
			} catch (NumberFormatException male) {
				border = 2;
			}

		// "border" attribute implies frame and rules
		if (border <= 0) {
			frame = "void";
			rules = "none";
		} else {
			frame = getAttr("frame", "border").intern();
			rules = "all";
		}
		rules = getAttr("rules", rules).intern();
		putAttr("frame", frame);
		putAttr("rules", rules); // put back fix-ups
	}

	/**
	 * Normalize random crap into compilant HTML table:
	 * <ul>
	 * <li>update to HTML 4.0 TBODY/THEAD/TFOOT/CAPTION
	 * <li>for ROWSPAN and COLSPAN, fill in dummy cells
	 * </ul>
	 * 
	 * Invoke exactly once, after parsing entire table subtree.
	 */
	void normalize() {
		// dump();
		// System.exit(0);
		// System.out.println("closing TABLE/"+childNum()+", # children = "+size()+", child 0 = |"+childAt(0)+"|, first leaf = |"+getFirstLeaf()+"|");
		// find first THEAD, TFOOT, TBODY
		// row1=...
		// special treatment for TFOOT: rearrange or special case everywhere
		int row1 = 0;
		boolean fcaption = false;
		for (int imax = size(); row1 < imax; row1++) {
			String gi = childAt(row1).getName();
			if (gi == "caption") {
				// if (fcaption==true) ... // two captions => collapse into one
				fcaption = true;
			} else if (/* gi!="caption" && */gi != "colgroup")
				break; // COLs normalized into COLGROUPs
		}

		// make sure all children are THEAD/TFOOT/TBODY
		// either that or leaf, since TD tucked in TR and TR in TBODY, cascading
		// during close on the way up tree
		// System.out.println("rows="+size()+", row1="+row1); dump();
		// System.exit(0);
		INode newtd = null; // consecutive leaf nodes collected in single TD
		for (int i = row1, imax = size(); i < imax; i++) {
			Node tbody = childAt(i);
			String tbgi = tbody.getName();
			// System.out.println("immediately under table: "+tbgi);
			if (tbody.isLeaf()
					|| ("tbody" != tbgi && "thead" != tbgi && "tfoot" != tbgi)) {
				if (newtd == null) {
					if (!fcaption) { // special case: content => caption if not
										// one already (doesn't help with
										// Charlie Rose weekly show, though)
						setChildAt(newtd = new HTMLTD("caption", null, null), i); // HTMLTD
																					// so
																					// flush
																					// floats
																					// at
																					// end
						fcaption = true;
						row1++;
					} else {
						INode newp = new INode("tbody", null, null);
						setChildAt(newp, i);
						newp = new HTMLTR("tr", null, newp);
						newtd = new HTMLTD("td", null, newp);
					}
				} else {
					i--;
					imax--;
				}
				newtd.appendChild(tbody);
			} else
				newtd = null; // back on track w/tbody, so create new TD of
								// error again
		}
		// dump(); System.exit(0);

		// special case, which authoring tools put in
		// int rowcnt=size(); -- not accurate
		// System.out.println("TABLE filling in spans, "+(rowcnt-1)+".."+row1);
		// int waste=0;

		// insert dummy cells where we have ROWSPAN or COLSPAN
		// When we see such a cell, we can't immediately add dummy cells,
		// because
		// (1) if iterating rows bottom-up or columns right-to-left, we don't
		// know both final (row,col) of the current cell, so insertion points
		// may be off if it's subsequently adjusted due to ROWSPAN or COLSPAN
		// further on, and
		// (2) if iterating top-down or left-to-right, the cells further on
		// don't have their final positions.
		// So, we insert dummy cells as we go along, as we come to that cell
		// position.
		for (int imax = size() - 1, i = imax; i >= row1; i--) { // bottom to top
																// -- shouldn't
																// matter among
																// TBODYs
			INode tbody = (INode) childAt(i);
			int bodysize = tbody.size();

			// if only one row in group, BOTH rowspan and colspan bogus (colspan
			// because no cells in rows above or below to span)
			// needed for "You are now leaving DARPA", which has one cell in
			// table but a colspan=2
			// (this assumes that spans across TBODY groups is illegal... and
			// doesn't happen, sigh)
			if (bodysize == 1) {
				HTMLTR tr = (HTMLTR) tbody.childAt(0);
				for (int k = 0, kmax = tr.size(); k < kmax; k++) {
					HTMLTD td = (HTMLTD) tr.childAt(k);
					if (td.rowspan > 1) {
						if (DEBUG)
							System.out.println("1-row bogus rowspan: "
									+ td.rowspan); /* td.dump(); */
						td.rowspan = 1;
						td.removeAttr("rowspan");
					}
					if (td.colspan > 1) {
						if (DEBUG)
							System.out.println("1-row bogus colspan: "
									+ td.colspan); /* td.dump(); */
						td.colspan = 1;
						td.removeAttr("colspan");
					}
				}
			}

			int[] icell = new int[1000];
			int maxcol = 0;
			for (int j = 0; j < bodysize; j++) { // top-down over rows
			/*
			 * if (!(tbody.childAt(j) instanceof HTMLTR)) {
			 * tbody.childAt(j).putAttr("BAD", "*****************************");
			 * //break; //tbody.dump(); System.out.println("==========");
			 * tbody.childAt(j).dump(); }//System.exit(0); }
			 */

				HTMLTR tr = (HTMLTR) tbody.childAt(j);
				if (tr.size() > icell.length)
					icell = Arrayss.resize(icell, tr.size());
				for (int k = 0, kmax = tr.size(); k < kmax; k++) { // left-to-right
																	// over
																	// columns
																	// so index
																	// of cell
																	// right
																	// when
																	// filling
																	// in row
					if (icell[k] > 0) {
						tr.insertChildAt(new HTMLTDSPAN(null), k);
						icell[k]--;
						kmax++;
						continue;
					}

					HTMLTD td = (HTMLTD) tr.childAt(k);
					int rowspan = td.getRowspan(), colspan = td.getColspan();

					// don't let ROWSPAN go past current THEAD, TFOOT, TBODY
					// if ROWSPAN and past last row, confine to last row
					if (rowspan == Integer.MAX_VALUE)
						rowspan = td.rowspan = bodysize - j;
					else if (rowspan + j > bodysize) { // i==rowcnt &&
														// rowspan>1) {
						if (DEBUG)
							System.out.print("bogus rowspan=" + rowspan
									+ " vs body size=" + bodysize + " @ " + j
									+ "," + k + ", " + td.getName());
						rowspan = td.rowspan = bodysize - j;
						if (DEBUG)
							System.out.println(" => " + rowspan);
						if (rowspan == 1)
							td.removeAttr("rowspan");
						else
							td.putAttr("rowspan", Integer.toString(rowspan));
					}

					// if first row don't let add bogus columns
					if (colspan == Integer.MAX_VALUE)
						colspan = td.colspan = maxcol - k + 1; // test
					// later, don't let COLSPAN go past COLGROUP
					// if (j==0 /*&&i==row1?*/ && colspan+k>maxcol) colspan =
					// td.colspan = maxcol - k;
					// if COLSPAN and last child
					// if (k==kmax) waste=Math.max(waste, colspan-1);
					// may be valid by other internal if (k==kmax) fillercnt =
					// Math.min(fillercnt, (colspan==0?0:colspan-1));

					// System.out.println("ROWSPAN="+rowspan+", COLSPAN="+colspan+" @ "+i+","+k);
					if (rowspan > 1 || colspan > 1) {
						// know that icell[k]==0, but could have conflicts
						// further on (LATER report conflicts)
						for (int ci = 0; ci < colspan; ci++)
							icell[k + ci] += rowspan;
						icell[k]--; // doing self now
						if (k + colspan > maxcol)
							maxcol = k + colspan + 1; // in case short row
						// kmax += colspan - 1;
					}
					// System.out.println("-------------------------");
					// td.dump();
				}

				// adjust icell on short rows
				int spsize = tr.size(); // after spans
				if (spsize > maxcol)
					maxcol = spsize;
				else if (spsize < maxcol)
					for (int k = spsize; k < maxcol; k++)
						if (icell[k] > 0) {
							tr.appendChild(new HTMLTDSPAN(null));
							icell[k]--;
						}
			}
			// dump();
			// System.exit(0);
		}
	}

	/**
	 * The black art of HTML table formatting.
	 * <p>
	 * 600+ lines of excruciatingly careful code:
	 * <ul>
	 * <li>cells can span rows and columns, spans that are bigger than spanned
	 * cells
	 * <li>column widths given by some mix of colgroup/col, percentage, absolute
	 * pixels, computed from content
	 * <li>borders, rules, cell padding, cell spacing, frame, groups, floats
	 * internal and external, ...
	 * <li>caption, colgroup/col, thead/tfoot/tbody, table, tr, th, td
	 * <li>provided space vs layout side: not enough space, more than minimal
	 * space, more than requested maximum
	 * <li>and of course, overwhelming all that, is erroneous markup
	 * <li>nested tables, of mixed width spec (natural, percentage, absolute)
	 * <li>finally, take the cross product to yield complexity
	 * </ul>
	 */
	public boolean formatNode(int width, int height, Context cx) {
		// long before = System.currentTimeMillis();
		if (size() == 0) {
			bbox.setSize(0, 0);
			valid_ = true;
			return false;
		}
		boolean maxprobe = (width > PROBEWIDTH / 2), minprobe = (width <= 0), probe = (minprobe || maxprobe);
		// System.out.println("table layout "+probe+" ("+width+")  ");

		// if deeply nested table, which is determined by multiple probes
		// between final widths, store and return min and max widths
		if (minprobe && minwidth != -1) {
			bbox.width = minwidth;
			return false;
		} else if (maxprobe && maxwidth != -1) {
			bbox.width = maxwidth;
			return false;
		}
		// dump(); System.exit(0);
		// System.out.println("table layout "+probe+" ("+width+")  ");

		String swidth = getAttr("width");
		int tbltype = (swidth == null ? NATURAL
				: swidth.endsWith("%") ? PERCENTAGE : ABSOLUTE);
		int formatw = HTML.getAbsPct(swidth, width - cx.getFloatWidth(BOTH)); // absolute
																				// pixels
																				// or
																				// percentage
																				// of
																				// non-bogus
																				// passed
																				// width
		// System.out.println("formatw="+formatw+", minwidth="+minwidth);

		// old: if (maxprobe && swidth!=null) { bbox.width = maxwidth =
		// (swidth.endsWith("%")? 0: formatw); return false; }
		if (maxprobe && tbltype != NATURAL) {
			if (tbltype == PERCENTAGE)
				tbltype = NATURAL; // % of infinity => masquerade as NATURAL,
									// whose width = sum of cols
			else {
				bbox.width = maxwidth = formatw;
				return false;
			}
		}
		// else min probe or natural width, which are all computed

		cx.pushFloat(); // table formatted as a block, no cutouts for insets
		// markDirtySubtree(); // have to save max width, have TD compute in its
		// reformat

		final boolean dump = false;// = true;//false &&
									// "body".equals(getParentNode().getName());

		// COUNT NUMBER OF COLUMNS

		// Method A: COLGROUP/COL
		// should compute column count during parsing (at </table>)
		row1 = 0;
		caption = null;
		List<String> scol = new ArrayList<String>(50);
		// skip over caption, col/colgroup (supposed to be in that order,
		// but...)
		for (int imax = size(); row1 < imax; row1++) {
			Node child = childAt(row1);
			String name = child.getName();
			// System.out.println("name = "+name+" ==? "+(name=="caption"));
			if ("caption" == name)
				caption = child;
			else if ("colgroup" == name) {
				INode cg = (INode) child;
				int cgspan = Integers.parseInt(child.getAttr("span"), 1); // "User agents must ignore this attribute if the COLGROUP element contains one or more COL elements."
				String cgswidth = child.getAttr("width", "0*"); // "0*"=>compute
				for (int j = 0, jmax = cg.size(); j < jmax; j++) {
					Node cchild = cg.childAt(j);
					if ("col" == cchild.getName()) { // better be
						cgspan = 0;
						int cspan = Integers
								.parseInt(cchild.getAttr("span"), 1);
						String cswidth = cchild.getAttr("width", cgswidth);
						for (int k = 0, kmax = cspan; k < kmax; k++)
							scol.add(cswidth);
					}
				}
				for (int k = 0, kmax = cgspan; k < kmax; k++)
					scol.add(cgswidth);
				// } else if ("col"==name) -- parser puts all "col" in a
				// "colgroup"
			} else
				break; // if have caption/colgroup/col past here, you lose
		}

		int colcnt = scol.size();

		// if (cols spec) LATER, else autosize
		// Method B: iterate over rows taking maximum number of TDs (rowspan and
		// colspan have been normalized by now)
		// could have easily computed this during parsing, but may have hacked
		// tree after than, and besides cheap to compute
		if (colcnt == 0)
			for (int i = row1, imax = size(); i < imax; i++) {
				INode tbody = (INode) childAt(i);
				for (int j = 0, jmax = tbody.size(); j < jmax; j++) {
					if (!(tbody.childAt(j) instanceof HTMLTR)) {
						System.out.println("NOT TR");
						dump();
						System.out.println("============");
						System.out.println(tbody.childAt(j).getClass()
								.getName());
						tbody.childAt(j).dump();
						System.out.println("NOT TR: " + tbody.childAt(j));
						System.exit(0);
						continue;
					}
					INode tr = (HTMLTR) tbody.childAt(j); // don't need to cast
					colcnt = Math.max(colcnt, tr.size()); // parser can't fix
															// this up (parser
															// normalizes
															// colspans)
				}
			}
		else
			System.out.println("*** colcnt via colgroup/col = " + colcnt);

		// COMPUTE MINIMUM AND MAXIMUM/REQUESTED COLUMN WIDTHS and SET COLUMN
		// TYPES (natural, percentage, absolute)
		// (ignore column spanners until after first round of fighting over
		// extra space, as usually spanner's spanned cells wide enough for it)

		int[] colmaxw = new int[colcnt], colminw = new int[colcnt], type = new int[colcnt];

		// border
		int lb = 0, rb = 0, tb = 0, bb = 0; // "void"==frame
		if ("box" == frame || "border" == frame)
			lb = rb = tb = bb = border;
		else if ("vsides" == frame)
			lb = rb = border;
		else if ("hsides" == frame)
			tb = bb = border;
		else if ("lhs" == frame)
			lb = border;
		else if ("rhs" == frame)
			rb = border;
		else if ("above" == frame)
			tb = border;
		else if ("below" == frame)
			bb = border;
		auxborder = (lb == rb && tb == bb && lb == tb && border < INSETS.length ? INSETS[border]
				: new Insets(tb, lb, bb, rb));

		// rules
		int colrulesep = ("all" == rules || "cols" == rules ? 1 : 0); // handle
																		// colgroups
																		// later
		int rowrulesep = ("all" == rules || "rows" == rules ? 1 : 0); // handle
																		// rowgroups
																		// later

		int colseps = (colcnt + 1) * cellspacing + colcnt
				* (cellpadding + colrulesep) * 2 + lb + rb; // - colcnt*2 for
															// box around each
															// cell

		int tablew = Math.max(formatw - colseps, 0); // width available for cell
														// contents
		// System.out.println("\nformatting TABLE "+/*getFirstLeaf()+*/", width="+width+" => "+swidth+"/"+formatw);
		// if (dump)
		// System.out.println("TABLE format "+getFirstLeaf()+" "+swidth+" * "+width+" => "+tablew);

		// Source A of max/req: As specified COLGROUP/COL, which can leave some
		// width to be computed from contents ("0*")
		boolean fcol = false; // all columns specified (to non-natural)? =>
								// doesn't matter because iterate over everybody
								// anyway to collect min widths, width attr
								// overrides, and fix errors of max/req < min
		if (scol.size() > 0) {
			fcol = true;
			int mlsum = 0, mlw = tablew;
			for (int i = 0, imax = scol.size(); i < imax; i++) {
				String cswidth = scol.get(i); // should never get null as
												// everything defaulted
				boolean fmulti = cswidth.endsWith("*"), fpct = cswidth
						.endsWith("%");
				if (probe && (fmulti || fpct)) { // taking portions of available
													// space, which is 0 or
													// unknown-say-0
					colmaxw[i] = 0;
					type[i] = ABSOLUTE;
				} else {
					if (fmulti) { // sum up multilens
						try {
							mlsum += Integer.parseInt(cswidth.substring(0,
									cswidth.length() - 1));
						} catch (NumberFormatException nfe) {
							cswidth = "0*";
							scol.set(i, cswidth);
						}
					} else { // absolute or percentage
						try {
							mlw -= (colmaxw[i] = HTML
									.getAbsPct(cswidth, tablew/* effw */));
						} catch (NumberFormatException nfe) {
							cswidth = "0*";
							scol.set(i, cswidth);
						}
					}
					// set type (errors fall down to "0*"/compute)
					if (cswidth.equals("0*")) {
						fcol = false;
						type[i] = NATURAL;
						colmaxw[i] = 0;
					} else if (fmulti)
						type[i] = MULTILEN;
					else
						type[i] = (fpct ? PERCENTAGE : ABSOLUTE);
				}
			}

			// resolve multilen into pixels
			// if have some >0 and at least one 0, the 0s get squeezed out!
			if (/* fcol && */mlsum > 0) {
				if (mlw < 0)
					mlw = 0; // on probes, can go negative with padding digs
				for (int i = 0, imax = scol.size(); i < imax; i++) {
					String cswidth = scol.get(i); // should never get null as
													// everything defaulted
					if (cswidth.endsWith("*")) {
						try {
							colmaxw[i] = (mlw * Integer.parseInt(cswidth
									.substring(0, swidth.length() - 1)))
									/ mlsum;
						} catch (NumberFormatException nfe) {
						} // no exceptions in this pass
					}
				}
			}
			System.out.println("max widths as computed from COLGROUP/COL\n\t");
			for (int i = 0, imax = colmaxw.length; i < imax; i++)
				System.out.print("  " + colmaxw[i]);
		}

		// Iterate over rows and column cells
		// - Compute min width for every cell (errors abound)
		// - if width attribute on cell specifies %/abs, it overrides whatever
		// specified before
		// - if no width specified (type==NATURAL), compute max/req width from
		// content
		List<HTMLTD> colspans = new ArrayList<HTMLTD>(size()), rowspans = new ArrayList<HTMLTD>(
				size());
		String colswidth;
		/* if (!fcol) */for (int i = row1, imax = size(); i < imax; i++) {
			INode tbody = (INode) childAt(i);
			// tbody.formatBeforeAfter(0,height, cx); // min probe through
			// TBODY&TR

			for (int j = 0, jmax = tbody.size(); j < jmax; j++) {
				HTMLTR tr = (HTMLTR) tbody.childAt(j);
				// System.out.println(tr.getFirstLeaf().getName());
				for (int k = 0, kmax = tr.size(); k < kmax; k++) {
					// if (!(tr.childAt(k).isStruct())) {
					// System.out.println("td not an INode: "); tr.dump(); }
					// if (!(tr.childAt(k) instanceof HTMLTD)) { tr.dump();
					// System.exit(0); }
					HTMLTD td = (HTMLTD) tr.childAt(k);

					int cs = td.getColspan();
					// if (dump)
					// System.out.println("["+i+","+k+"] colspan="+cs);
					if (cs > 1) {
						colspans.add(td);
						k += cs - 1;
						continue;
					} // skip over dummy nodes (get +1 from loop iterator)

					// compute min -- always compute for all types always from
					// content, because %/abs often too narrow
					// Usually computing the min is wasted work,
					// and it would be more efficient to compute from %/abs as
					// that may be %/abs the final width too,
					// and if %/abs is too narrow minwidth will bump it larger,
					// but need actual min for scaling when have extra or more
					// especially not enough space and need to scale or set to
					// mins.
					// bbox.width = maxwidth; // set for nowrap: min=max -- no
					// max may be absolute whereas contents are larger
					int mnw = td.computeMin(height, cx); // don't have to
															// cx.valid=false
															// because (1) every
															// cell gets
															// formatted, so not
															// skipping, and (2)
															// cells
															// indepdendent of
															// one another
															// anyhow
					// int mnw = td.bbox.width;
					if (mnw > colminw[k])
						colminw[k] = mnw;

					// compute max width of cell
					if (minprobe)
						colmaxw[k] = 0; // if minprobe, going to return sum of
										// mins, so don't compute max
					else {
						int mxw = -1;
						if ((colswidth = td.getAttr("width")) != null) { // subsequent
																			// width
																			// attribute
																			// settings
																			// override
																			// for
																			// that
																			// column
							boolean fpct = colswidth.endsWith("%");
							mxw = (maxprobe && fpct ? 0 : HTML.getAbsPct(
									colswidth, tablew, -1));
							if (mxw < 0) {
								td.removeAttr("width"); // error--practice is to
														// ignore, so fix =
														// remove. (Error seen
														// in
														// http://fman.sacredsoulrecords.com/)
								// fall through to take mxw from previously set,
								// or compute from content
							} else {
								type[k] = (fpct ? PERCENTAGE : ABSOLUTE);
								mxw = Math.max(colminw[k], mxw);
								colmaxw[k] = mxw/* or 0 OK */; // replace any
																// previous
																// setting on
																// column, which
																// may have been
																// larger -- or
																// keep largest?
							}
							// if (mxw>100)
							// System.out.println("max: "+colswidth+" x "+tablew+" @ "+k+" => "+mxw);
						} /* no else--maybe error in attr */
						if (type[k] == NATURAL
								|| (type[k] == PERCENTAGE && maxprobe)/*
																	 * better
																	 * than 0
																	 * width
																	 */) {
							td.markDirtySubtree(false); // make dirty from after
														// computing min (was:
														// ready for computing
														// minimum width)
							td.formatBeforeAfter(PROBEWIDTH, Integer.MAX_VALUE,
									cx); // 10x width to get non-forced
											// linebroken width
							mxw = td.bbox.width;
							// System.out.println("natural mxw of cell "+td.getFirstLeaf()+" = "+mxw);
						} else if (mxw < 0)
							mxw = colmaxw[k]; // column %/abs from before
							// System.out.println("WIDTH = "+colswidth+", type="+type[k]);

						// if (mnw>mxw) mxw=mnw; => done after all colmin/colmax
						// computations
						if (mxw > colmaxw[k])
							colmaxw[k] = mxw;
						// if (type[k]==PERCENTAGE)
						// System.out.println(td.getFirstLeaf()+"  "+colswidth+
						// " of "+tablew+" = "+mxw);
						// if (type[k]==ABSOLUTE) colminw[k]=mxw; // right?
					}
				}
			}
		}
		// System.out.println("width="+width);
		// System.out.println("COL TYPES for "+getAttr("id")+"/"+swidth); for
		// (int i=0; i<colcnt; i++)
		// System.out.println("	"+type[i]+":	"+colminw[i]/*+".."+colnatw[i]*/+".."+colmaxw[i]);
		// System.out.print("minmax = "); for (int
		// i=0,imax=colminw.length;i<imax;i++)
		// System.out.print(" "+colminw[i]+"/"+colmaxw[i]);
		// System.out.println();

		// DO NOT handle column spanners here -- see how widths play out after
		// fighting for extra space
		// even though could affect correctness of probe quick exit

		// col spanners can be wider than spanned cells
		// only expand natural width cols, if possible?
		// colspans.clear();
		for (int i = 0, imax = colspans.size(); i < imax; i++) {
			HTMLTD td = colspans.get(i);
			int j = td.childNum(), cs = td.getColspan(), cmax = Math.min(
					j + cs, colcnt);
			int ws = (cs - 1)
					* (2 * cellpadding + cellspacing + 2 * colrulesep);
			int wmax = 0;
			for (int k = j; k < cmax; k++)
				wmax += colmaxw[k];

			if (!maxprobe) { // need on minprobe to ensure enough space when
								// format with final width
				int wmin = 0;
				for (int k = j; k < cmax; k++)
					wmin += colminw[k]; // must achieve minimum so take from any
										// type[]
				int diff = td.computeMin(height, cx) - wmin - ws;
				if (diff > 0) {
					int dleft = diff, plus = diff / cs;
					for (int k = j; k < cmax; k++) {
						// if (wmin>0) plus = (colminw[k]*diff)/wmin; // used to
						// have separate looks to save comparison
						if (wmax > 0)
							plus = (colmaxw[k] * diff) / wmax; // scale
																// according to
																// max as that
																// accurate
																// approximation
																// of final
																// width whereas
																// mins subject
																// to word
																// widths
						dleft -= plus;
						colminw[k] += plus; // colminw[k]==0 if wmin==0
						// if (colminw[k] > colmaxw[k]) colmaxw[k]=colminw[k];
						// => after loop
					}
					colminw[cmax - 1] += dleft; // rounding error all goes to
												// last cell in span
				}
			}

			if (!minprobe) { // needed on maxprobe to request width, and needed
								// on final width to fight for space as natural
								// will mold to non-span contents otherwise
				int wnmax = 0;
				for (int k = j; k < cmax; k++)
					if (type[k] == NATURAL)
						wnmax += colmaxw[k]; // but max discretionary, so don't
												// override %/abs
				// look for width attribute?
				td.markDirtySubtree(false);
				td.formatBeforeAfter(PROBEWIDTH, Integer.MAX_VALUE, cx);
				int diff = td.bbox.width - wmax - ws;
				// System.out.println("span natural width="+td.bbox.width+" vs "+(wmax+ws)+" already, diff="+diff);
				if (diff > 0 && wnmax > 0) {
					int dleft = diff, lastnat = j;
					for (int k = j; k < cmax; k++) {
						if (type[k] == NATURAL) {
							int plus = (colmaxw[k] * diff) / wnmax;
							// System.out.println("colspan "+colmaxw[k]+" + "+plus+" = "+(colmaxw[k]+plus));
							// would be hard to be equitable among spanners, so
							// first come-first serve
							dleft -= plus;
							colmaxw[k] += plus;
							lastnat = k;
						}
					}
					colmaxw[lastnat/* cmax-1 */] += dleft;
				}
			}
		}

		// total min,max columnwidths over cols
		int colmaxwsum = 0, colminwsum = 0;
		for (int i = 0; i < colcnt; i++) {
			if (colmaxw[i] < colminw[i])
				colmaxw[i] = colminw[i];
			colminwsum += colminw[i];
			colmaxwsum += colmaxw[i];
		}
		// if (!probe/*colcnt==5*/) { for (int i=0; i<colcnt; i++)
		// System.out.print("	"+colminw[i]+"/"+colmaxw[i]+"/"+type[i]);
		// System.out.println(" => "+colminwsum+"/"+colmaxwsum); }

		// if (probe)
		// System.out.println("probe table layout ("+tablew+")  ");//+(System.currentTimeMillis()-before));
		if (probe) {
			if (minprobe) {
				// bbox.width = minwidth = colminwsum+colseps;
				minwidth = colminwsum + colseps;
				if (tbltype == ABSOLUTE)
					minwidth = Math.max(formatw, minwidth); // unlike width on
															// columns, width on
															// table sets
															// minimum width
				bbox.width = minwidth;
			} else {
				bbox.width = maxwidth = colmaxwsum + colseps;
				/*
				 * if (swidth!=null) maxwidth = Math.max((swidth.endsWith("%")?
				 * 0: formatw), maxwidth); bbox.width = maxwidth;
				 */
				// System.out.println("probe max "+tablew+" vis-a-vis "+colminwsum+"/"+colmaxwsum+" => "+maxwidth);//+(System.currentTimeMillis()-before));
			}
			// bbox.width = (minprobe? colminwsum: colmaxwsum)+colseps;
			cx.popFloat();
			return false;
		} // recursive min of computeMin
		minwidth = maxwidth = -1; // formatting with final width; clear so
									// recompute next format in case content
									// changes

		// COMPUTE COLUMN WIDTHS BASED ON MAX/REQ FOR EACH COLUMN
		// can leave extra space if col sum < table width=%/abs
		/* int[]if (colw==null || colw.length!=colcnt) */colw = new int[colcnt]; // final
																					// column
																					// widths,
																					// can
																					// be
																					// adjusted
																					// multiple
																					// times
		int space; // = tablew - colminwsum; -- make sure to set at each branch

		/*
		 * System.out.print("tablew = "+tablew+":	");//, minw="+colminw+" /
		 * maxw="+colmaxw); for (int i=0,imax=colcnt; i<imax; i++)
		 * System.out.print(colminw[i]+"/"+colmaxw[i]+" ");
		 * System.out.println();
		 */
		if (fcol) {
			System.arraycopy(colmaxw, 0, colw, 0, colcnt); // don't do
															// colw=colmaxw
															// because may need
															// to adjust for
															// spanners, at
															// which time need
															// reliable min/max
			tablew = colmaxwsum;
			space = 0; // no adjustments for extra width?

		} else if (colminwsum >= tablew) {
			System.arraycopy(colminw, 0, colw, 0, colcnt);
			if (DEBUG)
				System.out.println("colminwsum>=tablew (" + colminwsum
						+ "), expanding width to " + colminwsum);
			tablew = colminwsum; // have to make table wider
			space = 0;

		} else if (colmaxwsum <= tablew) {
			System.arraycopy(colmaxw, 0, colw, 0, colcnt);
			space = tablew - colmaxwsum;
			if (DEBUG)
				System.out.println("all max satisfied with " + colmaxwsum + "-"
						+ tablew + "=" + space + " to spare");

		} else { // between min and max requested, so scale
			System.arraycopy(colminw, 0, colw, 0, colcnt);

			space = tablew - colminwsum;
			if (DEBUG)
				System.out.println("extra space=" + space);
			// only fight over space among like elements here
			// for (int j=0; j<colcnt; j++)
			// System.out.print(""+colw[j]+"/"+colmaxw[j]+" ");
			for (int i = MAXPRIORITY; i >= MINPRIORITY && space > 0; i--) { // top
																			// off
																			// to
																			// max
																			// as
																			// follows:
																			// fixed,
																			// %,
																			// finally
																			// among
																			// content
				int sum = 0;// min=0; ... min+=colminw[j]; -- diff max and min
							// not good
				// assume fit at max/req, but scale down if don't
				for (int j = 0; j < colcnt; j++)
					if (type[j] == i) {
						sum += (colmaxw[j] - colminw[j]);
						colw[j] = colmaxw[j];
					}
				if (false && i == ABSOLUTE && sum > 0) {
					System.out.print("\ntype=" + i + ", space=" + space
							+ ", sum=" + sum + "	(");
				}
				if (sum > space) { // doesn't fit, so scale DOWN proportionally
					for (int j = 0; j < colcnt; j++)
						if (type[j] == i)
							colw[j] = colminw[j] + space
									* (colmaxw[j] - colminw[j]) / sum;
					// if (DEBUG)
					// System.out.println("scaling "+colminw[j]+" => "+colw[j]);}
					space = 0;
				} else
					space -= sum;

				if (false && i == ABSOLUTE && sum > 0) {
					for (int j = 0; j < colcnt; j++)
						if (type[j] == i)
							System.out.print("" + colw[j] + " ");
					System.out.println(")");
				}
			}
		}

		// NOW CONSIDER COLUMN SPANNERS. hopefully spanned cells wide enough
		// already
		// colspanners (e.g., in "Leaving DARPA" page where have single cell in
		// table, but it's "colspan=2")
		/*
		 * for (int i=0,imax=colspans.size(); i<imax; i++) { HTMLTD td =
		 * colspans.get(i); int j = td.childNum(); int w=0, cs=td.getColspan(),
		 * cmax=Math.min(j+cs,colcnt); for (int k=j; k<cmax; k++) w += colw[k];
		 * int ws = w + (cs-1) (2cellpadding + cellspacing + 2colrulesep);
		 * td.formatBeforeAfter(ws,Integer.MAX_VALUE, cx); int csmin =
		 * td.computeMin(height, cx); //getMinWidth() - w; int diff = csmin -
		 * ws;
		 * 
		 * // /no else--maybe error in attr / if (type[k]==NATURAL) { -- look
		 * for width attribute? td.markDirtySubtree(false); // make dirty from
		 * after computing min (was: ready for computing minimum width)
		 * td.formatBeforeAfter(PROBEWIDTH,Integer.MAX_VALUE, cx); // 10x width
		 * to get non-forced linebroken width int csmax = td.bbox.width;
		 * 
		 * // would be hard to be equitable among spanners, so first come-first
		 * serve if (space > csmax-ws) diff = csmax-ws +2;
		 * 
		 * //int diff = td.bbox.width - ws; // haven't reformatted since min
		 * probe
		 * System.out.println("+colspanner "+td.getName()+", space="+space+
		 * " vs "
		 * +csmin+"/"+csmax+", diff="+diff+", tablew="+tablew+", colmaxsum="
		 * +colmaxwsum); if (diff>0) { // not enough space in spanned cells, so
		 * increase widths proportionally if (w>0) for (int k=j; k<cmax; k++)
		 * colw[k] += (colw[k]diff)/w; else for (int k=j; k<cmax; k++) colw[k] =
		 * diff/cs; // all 0-width, divide equally (give to first?) if (space >
		 * diff) space -= diff; else { tablew += (diff-space); space=0; } }
		 * System.out.println("colspan, space="+space); }
		 */

		// IF STILL HAVE SPACE AFTER FULFILLING MAX/REQ AND COL SPANNERS, AND
		// TABLE WIDTH=%/ABS, MAKE COLUMNS WIDER STILL
		// for (int j=0;j<colcnt;j++) System.out.print("  "+colw[j]);
		// System.out.println("\nextra space = "+space+", tablew="+tablew+", colminwsum="+colminwsum+", fcontent="+fcontent+", type[0]="+type[0]);
		if (tbltype == NATURAL) {
			tablew -= space;
			space = 0;
		} // width of natural table = width of contents
		else if (space > 0) {
			// first try to give all extra to any natural-size column
			int fight = 0;
			for (int j = 0; j < colcnt; j++)
				if (type[j] == NATURAL)
					fight += colmaxw[j];
			if (fight > 0) {
				// System.out.println("giving extra space to natural: "+space);
				for (int j = 0; j < colcnt; j++)
					if (type[j] == NATURAL)
						colw[j] += (space * colmaxw[j]) / fight;
				space = 0;
			}

			/*
			 * if (space>0) { // else apportion to natural spans for (int
			 * i=0,imax=colspans.size(); /space>0 && / i<imax; i++) { HTMLTD td
			 * = colspans.get(i); int j = td.childNum(); int w=0,
			 * cs=td.getColspan(); for (int k=j,kmax=j+cs; k<kmax; k++) w +=
			 * colw[k] + (k>j? 2cellpadding + cellspacing: 0); int diff =
			 * td.getMinWidth() - w;
			 * //System.out.println("space="+space+", colspanner fightw = "
			 * +td.getMinWidth()); if (diff>0) { //diff = Math.min(diff, space);
			 * for (int k=j,kmax=j+cs; k<kmax; k++) colw[k] += (colw[k]diff)/w;
			 * //(diff/cs); space -= diff; } }
			 */
			if (space > 0) { // only % and abs: scale all proportionally
				int sum = 0;
				for (int i = 0; i < colcnt; i++)
					sum += colw[i]; // don't have to check type as no NATURAL
				if (dump)
					System.out.println("rest: space=" + space + ", sum=" + sum);
				if (sum > 0)
					for (int i = 0; i < colcnt; i++)
						colw[i] += (colw[i] * space) / sum;
				// else apportion to spans
			}
		}

		// if (dump) {
		// if (/*colcnt==5*/ !probe) {
		// System.out.print("FINAL COLS for "+swidth+":"); for (int i=0;
		// i<colcnt; i++) System.out.print("	"+colw[i]); System.out.println(); }
		// assert space==0: "";
		formatw = tablew + colseps;

		// FINAL FORMATTING PASS OVER ALL CELLS
		// cx.valid = false; -- cx still valid: cells independent of one another
		int x = 0, y = 0, // border+cellspacing, y=border+cellspacing, maxcolh;
		ix = colrulesep + cellspacing + colrulesep, iy = rowrulesep
				+ cellspacing + rowrulesep, pad2 = 2 * cellpadding, cb2 = colrulesep * 2, rb2 = rowrulesep * 2, roww = formatw
				- auxborder.left - auxborder.right - 2 * cellspacing;
		// System.out.println("formatw="+formatw+", roww="+roww+", auxborder="+auxborder);

		if (caption != null) {
			caption.formatBeforeAfter(formatw, height, cx);
			caption.bbox.setLocation(x, y);
			y += caption.bbox.height + cellspacing;
			// System.out.println("caption "+caption.getName()+" width="+width+", bbox = "+caption.bbox);
		}

		y += auxborder.top + cellspacing + rowrulesep;
		// System.out.println("y0="+y+" = "+auxborder.top+" + "+cellspacing+" + "+rowrulesep);
		for (int i = row1, imax = size(); i < imax; i++) {
			INode tbody = (INode) childAt(i);
			if ("tfoot" == tbody.getName() && tfoot == null)
				tfoot = tbody; // only recognize first TFOOT, treat rest as
								// TBODY
			int rowcnt = tbody.size();
			int[] rowh = new int[rowcnt];
			int by = y;
			// System.out.println("by="+by);

			// want to use HTML to describe GUIs, but no height=% on table, as
			// HTML has an infinite scroll as opposed to the screen window
			// We fix that by extending HTML with TR widht=% attribute. For now,
			// assume single TBODY.
			int rowseps = (rowcnt + 1) * cellspacing + rowcnt
					* (cellpadding + rowrulesep) * 2 + tb + bb, tableh = Math
					.max(height - rowseps, 0), hspace = tableh, rowhsum = 0;
			int[] rowhpct = new int[rowcnt];
			String pctattr;

			// WITH HARD-WON FINAL COLUMN WIDTHS, FORMAT AND SET WIDTH AND
			// COORDINATES
			// two passes for TR height %: 1. measure heights needed by normal
			// cells, 2. compoute height % and format those rows
			for (int trpct = 0; trpct < 2; trpct++)
				for (int j = 0, jmax = rowcnt; j < jmax; j++) {
					INode tr = (HTMLTR) tbody.childAt(j);

					int tdh = height; // height in formatting cell. defaults to
										// unlimited
					if (trpct == 0) { // pass 1: collect height % for divving up
										// extra
						if ((pctattr = tr.getAttr("height")) != null
								&& pctattr.endsWith("%")) { // my extension,
															// needed for GUI
															// layout, which
															// doesn't have
															// infinitely long
															// scroll
							try {
								rowhpct[j] = Math
										.max(Integer.parseInt(pctattr
												.substring(0,
														pctattr.length() - 1)),
												0);
								rowhsum += rowhpct[j];
								continue;
							} catch (NumberFormatException nfe) {
							}

						} // else fall-through to normal format

					} else { // pass 2
						if (j == 0) { // 2a: compute TR height %s
							// extension for GUI:
							// apportion extra space before assigning final
							// heights to all cells
							// System.out.println("height="+height+", rowhsum="+rowhsum+", hspace="+hspace);
							if (rowhsum > 0 && hspace > 0) {
								if (rowhsum > 100)
									for (int jp = 0, jpmax = rowcnt; jp < jpmax; jp++)
										rowhpct[jp] = (rowhpct[jp] * 100)
												/ rowhsum; // scale down to 100%
								for (int jp = 0, jpmax = rowcnt; jp < jpmax; jp++)
									if (rowhpct[jp] > 0)
										rowh[jp] = (rowhpct[jp] * hspace) / 100;
							} else
								break; // no TR height % -- if (rowhsum>0 &&
										// hspace<=0) TR height% rows left
										// unformatted!
								// for (int jp=0,jpmax=rowcnt; jp<jpmax; jp++)
								// System.out.print(rowh[jp]+" ");
								// System.out.print("");
						}
						// 2b: if affected, adjust TD formatting height
						if (rowhpct[j] == 0)
							continue;
						// System.out.println("rowhpct["+j+"] => "+rowh[j]);
						tdh = rowh[j];
					}

					x = cellspacing;
					int maxrowh = 0;
					for (int k = 0, kmax = tr.size(); k < kmax; k++) {
						HTMLTD td = (HTMLTD) tr.childAt(k);

						int tdw = colw[k];
						if (td.size() == 0) {
							td.bbox.setBounds(x, 0, tdw, 0);
							continue;
						} // when used in GUI
						int cs = td.getColspan(), rs = td.getRowspan();
						if (cs > 1) {
							for (int l = k + 1, lmax = Math.min(k + cs, kmax); l < lmax; l++)
								tdw += ix + colw[l] + pad2;
							k += cs - 1;
						}
						// if (td.bbox.width > tdw) { => almost always need to
						// reformat: if given column width, then not formatted
						// at minwidth, and if natural size, formatted at max
						// width, and even if max width < cell width, still want
						// to reformat to catch things like HR which depends on
						// prevailing width
						td.markDirtySubtreeDown(false); // false=>leaves still
														// have valid formatting
						td.formatBeforeAfter(tdw, tdh/* height */, cx); // format
																		// with
																		// final
																		// width
						td.padding = cellpadinsets; // bad: should set in style
													// on entry to formatting
													// cell, but not easy to
													// pass in actives (can't
													// inherit from TABLE or TR)
													// -- make sure include in
													// bbox
						td.border = INSETS[border >= 1 && td.getName() != null
								&& td.getFirstChild().getName() != "" ? border
								: 0]; // bad; skip filler cells
						tdw += pad2 + cb2;

						int h = td.bbox.height;
						if (rs > 1)
							rowspans.add(td); // collected per TBODY
						else {
							maxrowh = Math.max(maxrowh, h);
							String stdh = td.getAttr("height"); // HTML 4.01 has
																// "height" on
																// TD/TH only,
																// not TABLE or
																// TR
							if (stdh != null)
								try {
									maxrowh = Math.max(maxrowh, Integer
											.parseInt(stdh));
								} catch (NumberFormatException nfe) {
								}
						}

						// should probably skip HTMLTDSPAN
						td.bbox.setBounds(x, 0, tdw, h);
						// System.out.println("x += "+tdw+"/"+colw[i]+" = "+(x+tdw));
						x += tdw + cellspacing;
					}
					rowh[j] = maxrowh + pad2 + rb2;
					if (trpct == 1)
						System.out.println("rowh[" + j + "] = " + rowh[j]);
					hspace -= rowh[j];
				}

			// WITH FORMAT ON FINAL WIDTH, KNOW HEIGHTS, SO MAKE SURE ROW
			// SPANNERS HAVE ENOUGH
			// alternative two-pass method: assume rowspanners will be ok but
			// record heights, fix if not (move rows down, increase height on
			// cells) -- faster (usually no work) but have to fix valign too
			for (int k = 0, kmax = rowspans.size(); k < kmax; k++) {
				HTMLTD td = rowspans.get(k);
				INode tr = td.getParentNode();
				int j = tr.childNum();
				int h = 0, rs = td.getRowspan(), rmax = Math
						.min(j + rs, rowcnt); // rowspan is bounded by rowcnt
												// during parsing, but leave in
												// to be robust after tree
												// hacking
				for (int l = j; l < rmax; l++)
					h += rowh[l];
				int hs = h + (rs - 1) * (/* pad2 -- already included + */iy);
				int diff = td.bbox.height - hs;
				// System.out.println("space="+space+", rowspanner fightw = "+td.bbox.height);
				if (diff > 0) { // not enough space in spanned cells, so
								// increase heights proportionally
					if (h > 0)
						for (int l = j; l < rmax; l++)
							rowh[l] += (rowh[l] * diff) / h;
					else
						for (int l = j; l < rmax; l++)
							rowh[l] = diff / rs; // all 0-width, divide equally
													// (give to first?)
				}
			}
			rowspans.clear();

			// (UNIFORM) HEIGHT AND VALIGN
			// (if no row spanners or all fit in spanned cells, then could
			// integrate with above two loops -- which would be faster, but I
			// don't want to repeat the valign code)
			for (int j = 0, jmax = rowcnt; j < jmax; j++) {
				INode tr = (HTMLTR) tbody.childAt(j);
				int h = rowh[j], maxh = h;
				tr.formatBeforeAfter(roww, h, cx); // tr.bbox.setLocation(0,y-by);
													// // pick up, um, attrs?
													// LATER: don't bypass
													// possible observers.
													// //tr.formatBeforeAfter(x,maxrowh,
													// cx);/*tr.format just sets
													// valid bit*/
				byte trvalign = tr.valign;

				for (int k = 0, kmax = tr.size(); k < kmax; k++) {
					HTMLTD td = (HTMLTD) tr.childAt(k);
					int cs = td.getColspan(), rs = td.getRowspan();
					int effh = h - pad2;
					if (cs > 1)
						k += cs - 1;
					if (rs > 1) {
						for (int l = j + 1, lmax = Math.min(j + rs, rowcnt); l < lmax; l++)
							effh += rowh[l];
						effh += (rs - 1) * iy; // (/*pad2--already included +
												// */cellspacing +
												// 2*rowrulesep);
						maxh = Math.max(maxh, effh);
						// effh = h-pad2;
					}

					// VALIGN -- bbox is full cell, but move contents down
					// (exclusive of padding here)
					// if (td.valign==NONE)
					// System.out.println("td.valign => "+trvalign);
					byte valign = td.valign;
					if (valign == NONE)
						valign = trvalign; // inherited one level, sheesh
					int dy; // require some branch to set this
					if (valign == NONE || valign == MIDDLE)
						dy = (effh - td.bbox.height) / 2;
					else if (valign == BOTTOM)
						dy = effh - td.bbox.height;
					else if (valign == BASELINE)
						dy = 0; // not supported
					else
						/* if (valign==TOP or unknown) */dy = 0;
					// if (valign!=NONE)
					// System.out.println("valign = "+valign+" vs TOP=="+TOP);
					// if (valign==TOP)
					// System.out.println(td.getFirstChild().getName());
					if (dy > 0)
						for (int l = 0, lmax = td.size(); l < lmax; l++)
							td.childAt(l).bbox.y += dy; // just need one level
														// due to relative
														// coordinates

					td.bbox.height = effh + pad2; // even if cell is empty, give
													// it dimensions so can draw
													// background
				}

				// assert x==roww: ""; //-- x!=formatw iff don't have max cells
				// in row
				// give all rowspan height to that cell. can have row consisting
				// entirely of ROWSPAN TDs
				// http://cusg.eecs.berkeley.edu/ relies on y+=0 (all cells
				// ROWSPAN>0) but to show text and select have TR height=maxh --
				// horrid!
				tr.bbox.setBounds(0, y - by, roww, maxh/* NOT rowh[j] */); // tr.setValid(true);
																			// //
																			// LATER:
																			// don't
																			// bypass
																			// possible
																			// observers.
																			// //tr.formatBeforeAfter(x,maxrowh,
																			// cx);/*tr.format
																			// just
																			// sets
																			// valid
																			// bit*/
				// System.out.println("child #"+i+" "+tr.getName()+" bounds "+tr.bbox.getBounds());
				y += rowh[j]/* NOT maxh */+ iy;
			}

			// % heights

			tbody.bbox.setBounds(auxborder.left, by, roww, y - by - iy);
			tbody.setValid(true); // LATER: don't bypass possible observers.
			if (tbody == tfoot)
				y = by; // skip footer here; position after after loop
		}
		if (tfoot != null) {
			tfoot.bbox.y = y;
			y += tfoot.bbox.height + iy;
		}
		y += auxborder.bottom - rowrulesep;

		// entire table
		bbox.setSize(formatw, y); // top border included at top

		// => CENTERing done by floats=CENTER
		/*
		 * if (align==CENTER && width>bbox.width) { // handle centering yourself
		 * by gobbling all horitzonal space and moving children (same way as HR)
		 * xoff = (width-bbox.width)/2;
		 * //System.out.println("centering table: "+
		 * tablew+" in "+width+" => +"+xoff); bbox.setSize(width, bbox.height);
		 * for (int i=0/not row1, which skips caption /,imax=size(); i<imax;
		 * i++) childAt(i).bbox.x += xoff; } else xoff=0;
		 */
		// System.out.println("tablew="+tablew+", computed x="+x);
		// System.out.println("\nTABLE width = "+bbox.width+", WIDTH="+getAttr("width")+", width in="+width);
		// System.out.println("reformatted TABLE to "+x+"x"+y);
		// dump();
		cx.popFloat();
		// if (floats==Node.NONE) cx.eatHeight(bbox.height, this,size()); -- not
		// a flow!
		// dump();

		assert checkLayout();

		// System.out.println("* table layout "+(System.currentTimeMillis()-before)+" ms");
		valid_ = true;
		// System.out.println("width in="+width+", table width="+tablew);
		return !valid_;
	}

	boolean checkLayout() {
		// HTMLTDSPAN 0x0
		// no overlapping cells

		return true;
	}

	// a change in any cell can affect whole table
	// LATER: don't call parent; compensate for table width taken as percentage
	// here
	public void reformat(Node dirty) {
		// System.out.println("re<= table width="+bbox.width);
		/*
		 * markDirtySubtree(); // have to save max width, have TD compute in its
		 * reformat getParentNode().reformat();
		 */
		// punt on reformat => batch reformattings with dirty/repaint
		// setDirty();
		// for now, have to mark dirty up to root...
		// for (Node n=this; n!=null && n.isValid(); n=n.getParentNode())
		// n.setDirty();
		markDirty();
		// getParentNode().repaint(100);
		repaint(100);
		// System.out.println("re=> table width="+bbox.width);
	}

	/**
	 * Paint rules and groups. Stops painting when row passes bottom of clip
	 * region.
	 */
	public void paintNode(Rectangle docclip, Context cx) {
		/*
		 * if (bbox.width<300) { g.setColor((bbox.width/2)2==bbox.width?
		 * Color.BLUE:Color.ORANGE); g.drawRect(0,0,bbox.width-1,bbox.height-1);
		 * g.setColor(Color.WHITE); g.fillRect(3,3, 30,15);
		 * g.setColor(Color.BLACK); g.drawString("w="+bbox.width,
		 * 3,cx.getFont().getAscent()+3); }
		 */

		// if (fg==Color.BLACK) fg=Color.LIGHT_GRAY; else if (fg==Color.WHITE)
		// fg=Color.DARK_GRAY;
		// else if (fg.getRed()+fg.getGreen()+fg.getBlue() > (256/2)*3)
		// fg=fg.darker().darker(); else fg.brighter().brighter();
		// g.setColor(cx.foreground);
		Graphics2D g = cx.g;
		g.setColor(Color.DARK_GRAY);

		int formatw = bbox.width, t = (caption == null ? 0 : caption.bbox.y
				+ caption.bbox.height + cellspacing);
		;
		// if (row1<size()) { Rectangle r=childAt(row1).bbox;
		// formatw=r.width/*+auxborder.left+auxborder.right*/; /*t=r.y;*/ }
		int l = 0/* +xoff */, r = l + formatw, b = bbox.height, x, y, w, h;

		// frame -- doesn't include caption (otherwise, could use INode support)
		if (auxborder != INSETS_ZERO/* "void"!=frame */) { // default
		// System.out.println("border="+border+", l="+l+", r="+r+", t="+t+", b="+b);
			g.setColor(Color.LIGHT_GRAY);
			x = l;
			y = t/*-auxborder.top-cellspacing*/;
			w = formatw;
			h = b - y;
			if (auxborder.top > 0)
				g.fillRect(x, y, w, auxborder.top);
			if (auxborder.bottom > 0)
				g.fillRect(x, b - auxborder.bottom, w, auxborder.bottom);
			if (auxborder.left > 0)
				g.fillRect(x, y, auxborder.left, h);
			if (auxborder.right > 0)
				g.fillRect(r - auxborder.right, y, auxborder.right, h);
		}

		// rules -- draw first so spanners erase bisections
		Rectangle cbbox;
		if ("none" == rules) {
		} else if ("groups" == rules) {
			// can do this now
			// groups: Rules will appear between row groups (see THEAD, TFOOT,
			// and TBODY) and column groups (see COLGROUP and COL) only.
			x = l;
			y = t;
			for (int i = row1, imax = size(); i < imax; i++) { // top row
																// borders merge
																// into border
																// -- or skips
																// caption if
																// any
				INode tbody = (INode) childAt(i);
				cbbox = tbody.bbox;
				x = l + cbbox.x;
				y = t + cbbox.y;
				w = cbbox.width;
				h = cbbox.height;
				g.drawLine(x, y, x + w, y);
				y += h;
				g.drawLine(x, y, x + w, y);
			}
		} else if ("rows" == rules) {
			for (int i = row1, imax = size(); i < imax; i++) {
				INode tbody = (INode) childAt(i);
				cbbox = tbody.bbox;
				x = l + cbbox.x;
				y = cbbox.y - 1;
				w = cbbox.width;
				h = cbbox.height;
				for (int j = 0, jmax = tbody.size(); j < jmax; j++) {
					Node tr = tbody.childAt(j);
					// System.out.println(""+x+","+y+" .. "+w);
					g.drawLine(x, y, x + w, y);
					y += tr.bbox.height + 1;
					g.drawLine(x, y, x + w, y);
					y += cellspacing + 1;
				}
			}
		} else if ("cols" == rules) {
			x = l + auxborder.left + cellspacing;
			y = 0;
			h = b - y;// -auxborder.bottom-cellspacing;
			for (int i = 0, imax = colw.length; i < imax; i++) {
				g.drawLine(x, y, x, y + h);
				x += colw[i] + 1;
				g.drawLine(x, y, x, y + h);
				x += cellspacing + 1;
			}

		} // else { // "all" => done with general border, though in black vs
			// darkGray

		// need stopy in TR too, as TBODY can have many rows (and often single
		// TBODY, so stop code in TABLE has no effect)
		int starty = docclip.y, stopy = starty + docclip.height;
		// System.out.println("paint TABLE "+bbox.y+".."+(bbox.y+bbox.height)+" within "+starty+"..+"+stopy);
		for (int i = 0, imax = size(); i < imax; i++) {
			Node tbody = childAt(i);
			Rectangle tbbox = tbody.bbox;
			/*
			 * if (tbbox.y + tbbox.height < starty) { cx.valid=false;
			 * /System.out
			 * .println("TR skip: "+(cbbox.y+cbbox.height)+" < "+starty); / }
			 * else
			 */if (tbbox.y < stopy)
				tbody.paintBeforeAfter(docclip, cx); // standard collision check
														// efficient enough, and
														// does smart
														// cx.valid=false
			else {
				// cx.valid=false;
				// /*System.out.println("TBODY stop @ +"+cbbox.y);*/ => still
				// valid as spans contained with TDs
				if (tbody != tfoot)
					break;
			}
		}
		// if (!cx.valid) cx.reset(this,-1);
	}

	/*
	 * could stop looking on events too public boolean eventNode(AWTEvent e,
	 * Point rel) { boolean hitchild=false;
	 * //System.out.println("me = "+getName()); boolean shortcircuit=false;
	 * 
	 * //for (int i=size()-1; i>=0 && !shortcircuit; i--) { int i=0; // used for
	 * backtracking on !hitchild int prevbaseline=-1, curbaseline=-1; for (int
	 * imax=size(); i<imax && !shortcircuit && !hitchild; i++) { // for stop to
	 * work Node child=childAt(i); Rectangle cbbox=child.bbox; int bl =
	 * child.baseline + cbbox.y; if (bl!=curbaseline) {
	 * prevbaseline=curbaseline; curbaseline=bl; }
	 * 
	 * if (rel==null) {} else if (cbbox.contains(rel)) hitchild=true; // not
	 * Node.contains(Point) else if (cbbox.y > stopy) break; // stop looking: no
	 * hits possible on subsequent nodes
	 * 
	 * shortcircuit = child.eventBeforeAfter(e,rel); // regardless of bbox! } if
	 * (!shortcircuit && !hitchild) { int eid=e.getID(); boolean setcur =
	 * eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED ||
	 * eid==TreeEvent.FIND_NODE; Browser br = getBrowser(); if (!hitchild)
	 * br.setCurNode(this,0); // set to lowest enclosing node -- or Leaf only?
	 * 
	 * 
	 * return shortcircuit; }
	 */
}

// cache min, max of child widths?
class HTMLTR extends INode/* IHBox--pick up formatNode *//* implements Comparable */{
	HTMLTR(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
	}

	/**
	 * Called to pick up attributes from stylesheet, but empty body as table
	 * node has to have global knowledge.
	 */
	public boolean formatNode(int width, int height, Context cx) {
		bbox.setSize(width, height);
		valid_ = true;
		return !valid_;
	}

	/*
	 * public boolean eventNode(AWTEvent e, Point rel) { boolean ret =
	 * super.eventNode(e,rel);
	 * System.out.println("in TR .. "+getFirstLeaf().getName()+", ss="+ret);
	 * return ret; }
	 */
	/**
	 * TRs can overlap (when have ROWSPAN or COLSPAN), so don't claim
	 * br.setCurMark if no hit in child.
	 */
	public boolean eventNode(AWTEvent e, Point rel) {
		// System.out.println("me = "+getName());
		boolean shortcircuit = false;
		for (int i = 0, imax = size(); i < imax && !shortcircuit; i++) { // optimize
																			// over
																			// TRs
																			// in
																			// TABLE
			// for (int i=size()-1; i>=0 && !shortcircuit; i--) {
			Node child = childAt(i);
			shortcircuit = child.eventBeforeAfter(e, rel);
		}
		return shortcircuit;
	}

	/**
	 * HTMLTABLE does all the heavy lifting, but passes through TBODY and TR in
	 * order to pick up any style settings. public boolean formatNode(int
	 * width,int height, Context cx) { boolean probe = (width<=0 ||
	 * width>PROBEWIDTH/2); int colw[] = null; if (probe) { HTMLTABLE table =
	 * (HTMLTABLE)getParentNode().getParentNode(); // TBODY, TABLE colw =
	 * table.colw; }
	 * 
	 * for (int i=0,imax=size(); i<imax; i++) { Node td=childAt(i);
	 * td.formatBeforeAfter((probe? width: colw[i]),height, cx); }
	 * 
	 * if (!probe) valid_=true; return false; }
	 */

	public void reformat(Node dirty) {
		// System.out.println("TR reformat()");
		markDirty();
		getParentNode().reformat(this); // can't reformat on TABLE as may be
										// using percentage width
	}

	/** Stop painting when rows pass clipping region. */
	public void paintNode(Rectangle docclip, Context cx) {
		// need stopy in TR too, as can have single TBODY with many rows
		int starty = docclip.y, stopy = starty + docclip.height;
		for (int i = 0, imax = size(); i < imax; i++) {
			Node child = childAt(i);
			Rectangle cbbox = child.bbox;
			/*
			 * if (cbbox.y + cbbox.height < starty) { cx.valid=false;
			 * /System.out
			 * .println("TR skip: "+(cbbox.y+cbbox.height)+" < "+starty); / }
			 * else
			 */if (cbbox.y < stopy)
				childAt(i).paintBeforeAfter(docclip, cx);
			else
				break; // { cx.valid=false;
						// /*System.out.println("TR stop @ "+cbbox.y);*/ break;
						// } => still valid since spans contained within TDs
		}
	}
}

class HTMLTD extends IParaBox {
	/* byte would give spans of 127, short plenty big */int rowspan = -1,
			colspan = -1; // if ==0, span until end

	// int minwidth=-1;

	HTMLTD(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);

		String attr = getAttr("rowspan");
		if (attr == null)
			rowspan = 1;
		else
			try {
				rowspan = Integer.parseInt(attr);
			} catch (NumberFormatException nfe) {
			}
		if (rowspan == 0)
			rowspan = Integer.MAX_VALUE;
		else if (rowspan < 0) {
			rowspan = 1;
			removeAttr("rowspan");
		} // can't parse or <=0

		attr = getAttr("colspan");
		if (attr == null)
			colspan = 1;
		else
			try {
				colspan = Integer.parseInt(attr);
			} catch (NumberFormatException nfe) {
			}
		if (colspan == 0)
			rowspan = Integer.MAX_VALUE;
		else if (colspan < 0) {
			colspan = 1;
			removeAttr("colspan");
		} // can't parse or <=0
	}

	public int getRowspan() {
		return rowspan;
	}

	public int getColspan() {
		return colspan;
	}

	protected int computeMin(int height, Context cx) {
		// if (valid_) return minwidth; // ?

		// supposed to be able to progressively render percentage specs,
		// but in the wild (as on deja.com), need to scale them down

		// would like to set minwidth=maxwidth on absolute and percentage, but
		// bad HTML makes that unreliable

		// compute min -- not necessarily largest leaf!
		formatBeforeAfter((getAttr("nowrap") == null ? 0/*-1 confuses %*/
				: PROBEWIDTH), Integer.MAX_VALUE, cx); // doesn't handle spans
														// that start but don't
														// end within cell--but
														// parser should have
														// prevented those
		// minwidth = bbox.width;
		// markDirtySubtree(false); // ready for final width -- everybody gets
		// this regardless

		// return minwidth;
		return bbox.width;
	}

	// have to have two-stage TD format for computed tables: (1) min and max,
	// (2) actual width
	// dumb format controlled by TABLE or computeMinMax
	public boolean formatBeforeAfter(int width, int height, Context cx) {
		boolean ret = super.formatBeforeAfter(width, height, cx);
		cx.flush = BOTH; // REFACTOR
		return ret;
	}

	public boolean formatNode(int width, int height, Context cx) {
		// int w = (width<=0 && getAttr("nowrap")!=null? PROBEWIDTH: width);
		// boolean shortcircuit = super.formatNode(w,Integer.MAX_VALUE, cx);
		// if (probe) return false;
		boolean shortcircuit = super.formatNode(width,
				height/* Integer.MAX_VALUE */, cx);

		bbox.width = Math.max(bbox.width, cx.getFloatWidth(BOTH));
		// like spans, floats entirely contained within TD
		cx.flush = BOTH;
		// bbox.height += cx.flowFloats(bbox.height, w); // flush remaining
		bbox.height += cx.flushFloats(Node.BOTH); // flush remaining

		// bbox.height = Math.max(bbox.height,
		// Integers.parseInt(getAttr("height"),-1)); => report natural height so
		// can valign contents
		// System.out.println((getFirstLeaf()!=null?getFirstLeaf().getName():"null")
		// + " TD width = "+bbox.width+", WIDTH="+getAttr("width"));

		valid_ = !shortcircuit;
		return shortcircuit;
	}

	/*
	 * public void reformat() { System.out.println("TD reformat"); int
	 * owidth=bbox.width, oheight=bbox.height; super.reformat(); // if
	 * (bbox.width!=owidth || bbox.height!=oheight) repaint( ,
	 * bbox.widthcolspan, bbox.heightrowspan); bbox.setSize(bbox.widthcolspan,
	 * bbox.heightrowspan); super.reformat(); bbox.setSize(bbox.width/colspan,
	 * bbox.height/rowspan); }
	 */

	public void paintNode(Rectangle docclip, Context cx) {
		// draw background explicitly for now so picked up in GUI widgets
		// Color bg = Colors.getColor(getAttr("bgcolor"),null); // not horribly
		// inefficient, even though often many cells, because usually no
		// attributes at all and get quick if (attr_==null) return null;
		// if (bg!=null) { g.setColor(bg); g.fillRect(0,0,
		// bbox.width,bbox.height); }
		// if (bg!=null)
		// System.out.println("filling TD with "+getAttr("bgcolor")+"/"+bg);

		// overwrite rules in span
		// if (rowcnt>1 || colcnt>1) { g.setColor(cx.foreground);
		// g.fillRect(0,0, bbox.width,bbox.height); }
		super.paintNode(docclip, cx);
		/*
		 * g.setColor(Color.BLUE); g.drawRect(0,0,bbox.width,bbox.height);
		 * g.setColor(Color.WHITE); g.fillRect(0,0, 20,12);
		 * g.setColor(Color.RED); g.drawString(""+bbox.width, 3,10);
		 * g.setColor((bbox.width/2)2==bbox.width? Color.BLUE:Color.ORANGE);
		 * g.drawRect(1,1,bbox.width-1-1,bbox.height-1-1);
		 */
		/*
		 * g.setColor(Color.WHITE); g.fillRect(15,15, 30,15);
		 * g.setColor(Color.BLACK); g.drawString("w="+bbox.width,
		 * 15,15+cx.getFont().getAscent()+3);
		 */
		/*
		 * if (getParentNode().childNum()==0) { String txt = "w="+bbox.width;
		 * Font f = cx.getFont(); g.setColor(Color.WHITE); g.fillRect(3,0,
		 * f.stringAdvance(txt).getX(), f.getAscent()); g.setColor(Color.RED);
		 * g.drawString("w="+bbox.width, 3,f.getAscent()); }
		 */
	}
}

/** Make structurally facile tree when have ROWSPAN or COLSPAN */
class HTMLTDSPAN extends HTMLTD /*
								 * waste space on some fields by subclassing
								 * here
								 */{
	// static LeafText dummyChild = new LeafText("BAH ",null,null);
	// HTMLTD real_;

	HTMLTDSPAN(HTMLTD real) {
		super(null/* "FILLER" */, null, null);
		// real_=real;
		// appendChild(dummyChild); // all internal nodes have to have at least
		// one child
		appendChild(new LeafText(""/* "BAH" */, null, null)); // -- still need
																// this?
																// getNextLeaf()
																// can step over
		valid_ = true;
	}

	// public HTMLTD getRealTD() { return real_; }
	public int getRowspan() {
		return 1;
		// return real_.getRowspan();
	}

	public int getColspan() {
		return 1; // 0?
		// return real_.getColspan();
	}

	public int getMinWidth() {
		return 0/* real_.getMinWidth() */;
	}

	public boolean formatNode(int width, int height, Context cx) {
		// bbox.setSize(width, 0); // height reset to be uniform across row
		bbox.setSize(0, 0);
		// bbox.setSize(real_.bbox.width, 1); -- so intersects paint, so can
		// tell real cell to paint
		childAt(0).setValid(true);
		valid_ = true; // X safer and quick enough to always update => skipped
						// during formatting
		return false;
	}

	/** Paints nothing: real cells overwrite. */
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
	}
}

class HTMLFRAMESET extends INode {
	int[] rows, cols;

	HTMLFRAMESET(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);

		// FRAMEs may have scrollbar, but FARMESET never does
		Document doc = getDocument();
		if (doc != null)
			doc.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
		// would like to handle ROWS,COLS here but need to know screen width and
		// height
	}

	protected int[] computeSizes(String sizes, int max) {
		int[] size = null;
		if (sizes != null) {
			StringTokenizer st = new StringTokenizer(sizes, ",");
			size = new int[st.countTokens()];
			// first pass: sum absolutes and percentages, and coefficients
			int sum = 0, cosum = 0, coeff, idx;
			for (int i = 0; i < size.length; i++) {
				String num = st.nextToken().trim();
				if ((idx = num.indexOf("*")) != -1) { // maybe put this in
														// getAbsPct too
					if (idx == 0)
						cosum++;
					else
						try {
							cosum += Integer.parseInt(num.substring(0, idx));
						} catch (NumberFormatException coex) {
							cosum++;
						}
				} else {
					size[i] = HTML.getAbsPct(num, max, 10);
				}
				sum += size[i];
			}

			// second pass: resolve coefficients
			st = new StringTokenizer(sizes, ",");
			int starspace = max - sum;
			for (int i = 0; i < size.length; i++) {
				String num = st.nextToken().trim();
				if ((idx = num.indexOf("*")) != -1) {
					if (idx == 0)
						coeff = 1;
					else
						try {
							coeff = Integer.parseInt(num.substring(0, idx));
						} catch (NumberFormatException coex) {
							coeff = 1;
						}
					size[i] = starspace * coeff / cosum;
					sum += size[i];
				}
			}

			// if bigger or smaller than available, resize proportionally to
			// 100%
			if (sum != max) {
				for (int i = 0; i < size.length; i++)
					size[i] = (size[i] * max) / sum;
			}
		}
		if (size == null) {
			size = new int[1];
			size[0] = max;
		}
		return size;
	}

	public boolean formatNode(int width, int height, Context cx) {
		bbox.setSize(width, height);

		// format children according to computed max bboxes
		rows = computeSizes(getAttr("rows"), height);
		cols = computeSizes(getAttr("cols"), width);
		// System.out.print("rows = "+getAttr("rows")+"/"+height+" into "); for
		// (int i=0;i<rows.length;i++) System.out.print(" "+rows[i]);
		// System.out.println();
		// System.out.print("cols = "+getAttr("cols")+"/"+width+" into "); for
		// (int i=0;i<cols.length;i++) System.out.print(" "+cols[i]);
		// System.out.println();

		/*
		 * for (int i=0,imax=size(),x=0,y=0,w,h; i<imax; i++,x+=w,y+=h) { if
		 * (i%cols.length==0) x=0; if (i%rows.length==0) y=0; // seems like only
		 * need one of these w=cols[i%cols.length]; h=rows[i%rows.length]; Node
		 * child = childAt(i); // FRAME semi-independent so pass cx or not?
		 * shortcircuit = shortcircuit || child.formatBeforeAfter(w,h, cx);
		 * child.bbox.setLocation(x,y); }
		 */

		boolean shortcircuit = false;
		int i = 0, imax = size();
		int y = 0, w = 0, h = 0;
		for (int r = 0, rmax = rows.length; r < rmax; r++, y += h) {
			int x = 0;
			h = rows[r];
			for (int c = 0, cmax = cols.length; c < cmax; c++, x += w) {
				w = cols[c];
				for (; i < imax; i++) {
					Node child = childAt(i);
					// System.out.println("checking "+child.getName());
					if ("frame".equals(child.getName())
							|| "frameset".equals(child.getName())) {
						// FRAME semi-independent so pass cx or not?
						// shortcircuit = shortcircuit ||
						// child.formatBeforeAfter(w,h, cx);
						child.formatBeforeAfter(w, h, cx);
						child.bbox.setLocation(x, y);
						// System.out.println("setting "+child.getAttr("name")+"/"+r+","+c+": "+w+"x"+h+" => "+child.bbox+", valid="+child.isValid());
						i++;
						break;
					}
				}
				if (i == imax)
					new LeafUnicode("(EMPTY FRAME)", null, this);
			}
		}
		for (; i < imax; i++)
			childAt(i).bbox.setBounds(0, 0, 0, 0);

		// if (!shortcircuit) valid_=true;
		valid_ = !shortcircuit;
		return shortcircuit;
	}

	// clip to proper height by intersecting docclip with FRAME size
	// public void paintNode(Rectangle docclip, Context cx) {
	// }
}

/*
 * marginwidth %Pixels; #IMPLIED -- margin widths in pixels -- marginheight
 * %Pixels; #IMPLIED -- margin height in pixels --
 * 
 * used to have own root, no children
 */
class HTMLFRAME extends IVBox { // was extends Document
	boolean border = true;

	HTMLFRAME(String name, Map<String, Object> attrs, INode parent, URI baseURI) {
		super(name, attrs, parent);

		String frameborder = getAttr("frameborder");
		if ("0".equals(frameborder))
			border = false;

		// should send event to load page rather than doing on instantiation
		String src = getAttr("src");
		// if (src!=null) try { HTML.go(this, this, baseURI.resolve(src)); }
		// catch (URISyntaxException leaveempty) {}
		Document doc = getDocument();
		Browser br = doc.getBrowser();
		Document newdoc = new Document(null, null, this, br);
		new LeafUnicode("Loading " + src, null, newdoc);

		if (src != null && doc != null && br != null)
			try {
				DocInfo di = new DocInfo(baseURI.resolve(src));
				di.doc = newdoc;
				br.eventq(Document.MSG_OPEN, di);
			} catch (IllegalArgumentException leaveempty) {
			}
	}

	public boolean formatNode(int width, int height, Context cx) {
		String scroll = getAttr("scrolling");
		byte policy = VScrollbar.SHOW_AS_NEEDED; // auto is default
		if ("no".equalsIgnoreCase(scroll))
			policy = VScrollbar.SHOW_NEVER;
		else if ("yes".equalsIgnoreCase(scroll))
			policy = VScrollbar.SHOW_ALWAYS;
		for (int i = 0, imax = size(); i < imax; i++) {
			Node child = childAt(i);
			if (child instanceof Document) {
				((Document) child).setScrollbarShowPolicy(policy);
				break;
			}
		}

		return super.formatNode(width, height, cx);
	}

	public void paintNode(Rectangle docclip, Context cx) {
		// need to handle margin
		// System.out.println("painting FRAME "+bbox); dump(2);
		super.paintNode(docclip, cx);
		if (border) {
			Graphics2D g = cx.g;
			g.setColor(cx.foreground);
			g.drawRect(0, 0, bbox.width, bbox.height);
		}
	}
}

/**
 * Same as HTMLFRAME, except computes its size rather than being assigned one
 * from FRAMESET.
 * 
 * HTML 4.0 spec:"The contents of the IFRAME element ... should only be displayed by user agents that do not support frames or are configured not to display frames."
 */
class HTMLIFRAME extends HTMLFRAME {
	HTMLIFRAME(String name, Map<String, Object> attrs, INode parent, URI baseURI) {
		super(name, attrs, parent, baseURI); // "src", ...
	}

	public boolean formatNode(int width, int height, Context cx) {
		int w = HTML.getAbsPct(getAttr("width"), width, Math.min(width, 200)), h = HTML
				.getAbsPct(getAttr("height"), height, Math.min(height, 200));
		bbox.setSize(w, h);
		// handle margin

		return super.formatNode(w, h, cx);
	}
}

// SPANS

class HTMLFONT extends Span {
	int fontsize = 0;
	int fontdelta = 0;
	Color newcolor = null;
	String face = null;

	public HTMLFONT(String name) {
		setName(name);
	}

	// System.out.println(ln+"/"+li+".."+rn+"/"+ri+"   COLOR="+getAttr("color")+", SIZE="+getAttr("size")+", FACE="+getAttr("face"));
	public void restore(ESISNode n, Map<String, Object> attrs, Layer layer) {
		super.restore(n, attrs, layer);

		newcolor = Colors.getColor(getAttr("color"), null);

		String attr;
		if ((attr = getAttr("size")) != null) {
			try {
				int num = Integer.parseInt(attr.startsWith("+") ? attr
						.substring(1) : attr); // parseInt can't handle leading
												// "+"
				if (attr.startsWith("+") || attr.startsWith("-"))
					fontdelta = num; // "+2" different than "2"
				else
					fontsize = Maths.minmax(1, num, 7);
			} catch (NumberFormatException nfe2) {
			}
		}

		if ((attr = getAttr("face")) != null) {
			StringTokenizer st = new StringTokenizer(attr, ",");
			while (st.hasMoreTokens()) {
				String fam = NFontManager.getDefault().getAvailableFamily(
						st.nextToken(), -1);
				// System.out.println("FACE=|"+family+"| => "+face+" "+inx);
				if (fam != null) {
					face = fam;
					break;
				}
			}
		}
	}

	public boolean appearance(Context cx, boolean all) {
		if (newcolor != null)
			cx.foreground = newcolor;
		// System.out.print("fontdelta="+fontdelta+", fontsize="+fontsize+", cx.size: "+cx.size+"=>");
		if (fontdelta != 0) {
			// will probably need to be more flexible: slide around in table
			// until find first valid. maybe just scale by 1.2
			int size = (int) cx.size;
			if (size < HTML.points2size.length && HTML.points2size[size] != 0) {
				cx.size = HTML.validpoints[Maths.minmax(1,
						HTML.points2size[size] + fontdelta, 7) - 1];
			}
		} else if (fontsize > 0) {
			cx.size = HTML.validpoints[fontsize - 1];
		}
		// System.out.println(cx.size);
		if (face != null)
			cx.family = face;
		return false;
	}

	public String toString() {
		return "multivalent.std.adaptor.HTMLFONT, " + attr_ + " "
				+ super.toString();
	}
}

/**
 * Same as HyperlinkSpan except knows about HTML ALINK/VLINK/LINK attributes
 * Handles FRAME TARGETs, except not added to forward/backward history, so like
 * Netscape's initial implementation of FRAME, for now.
 */
class HTMLACTION extends HyperlinkSpan {


        private Leaf viewerObject;
        URI href;
        public HTMLACTION(Leaf viewerObject, URI href) {
                setName("a");
                this.viewerObject = viewerObject;
                this.href = href;
        }

        /**
         * Everything done by styles, so empty body here to supress HyperlinkSpan's
         * default colors.
         */
        public boolean appearance(Context cx, boolean all) {
                return false;
        }

        /**
         * When click with TARGET set, search for named frame or create new window;
         * else br.open() replaced frames inherit menubar of toplevel document.
         */
        public void go() {
                System.out.println("* " + href);
                //getBrowser().eventq(HTML.SET_URL, this.target_);
                viewerObject.eventBeforeAfter(new SemanticEvent(this, HTML.SET_URL, href), null) ;
        }
}

/**
 * Same as HyperlinkSpan except knows about HTML ALINK/VLINK/LINK attributes
 * Handles FRAME TARGETs, except not added to forward/backward history, so like
 * Netscape's initial implementation of FRAME, for now.
 */
class HTMLA extends HyperlinkSpan {
	public HTMLA() {
		setName("a");
	}

	/**
	 * Everything done by styles, so empty body here to supress HyperlinkSpan's
	 * default colors.
	 */
	public boolean appearance(Context cx, boolean all) {
		return false;
	}

	/**
	 * When click with TARGET set, search for named frame or create new window;
	 * else br.open() replaced frames inherit menubar of toplevel document.
	 */
	public void go() {
		HTML.go(getStart().leaf, getAttr("target"), target_);
	}
}

// subclass for image map, border
/**
 * Show image, with possible image map.
 */
class HTMLIMG extends LeafImage implements EventListener {
	/** Refers to a HashMap in global namespace. */
	String mapname_;
	// String href_=null;
	URI href_ = null;
	boolean ismap_ = false;
	URI hit_ = null;

	// int border=1;
	// int hspace, vspace;

	HTMLIMG(String name, Map<String, Object> attrs, INode parent, URI imguri) {
		super(name, attrs, parent, imguri);
		mapname_ = getAttr("usemap");
		if (mapname_ != null && mapname_.startsWith("#"))
			mapname_ = mapname_.substring(1); // just supports maps included in
												// same page
		// border = Integers.parseInt(getAttr("border"), 0);
		// border=0; // until redesign insets
		ismap_ = (getAttr("ismap") != null);
		// System.out.println("img "+getAttr("src"));

		// hspace = Integers.parseInt(getAttr("hspace"), -1);
		// /vspace = Integers.parseInt(getAttr("vspace"), -1);
		// => translate to padding? margin?
		// leaves don't have insets -- though IMG, APPLET and OBJECT need them!
		// if (hspace==vspace && hspace<INSETS.length) insets=INSETS[hspace];
		// else insets=new Insets(vspace,hspace, vspace,hspace);
	}

	/*
	 * => put in public boolean formatNode(int width,int height, Context cx) {
	 * if (hspace!=-1) cx.paddingleft = cx.paddingright = hspace; if
	 * (vspace!=-1) cx.paddingtop = cx.paddingbottom = vspace; if (border!=-1)
	 * cx.setBorder }
	 */
	/**
	 * Given an parsed imagemap specification, make {@link java.util.Map} with
	 * {@link java.awt.Shape}s as keys and {@link java.net.URI}s as values.
	 */
	public static Map<Shape, String> makeMap(INode n) {
		Map<Shape, String> imagemap = new HashMap<Shape, String>(10);
		for (int i = 0, imax = n.size(); i < imax; i++) {
			Node area = n.childAt(i);
			if ("area".equals(area.getName())) {
				String shape = area.getAttr("shape");
				if (shape == null)
					shape = "rect";
				else
					shape = shape.toLowerCase();
				String dest = area.getAttr("href"), coords = area
						.getAttr("coords"); // sentinal
				if (coords == null || dest == null)
					continue;
				coords = coords/* .toLowerCase() */+ ","; // sentinal

				// comma-separated list... but NYT puts space between (x,y)
				// pairs and Netscape allows it
				// should separate this out into method--what else processes
				// coordinates?
				int npts = 0;
				boolean even = true;
				int[] shapex = new int[100], shapey = new int[100]; // should
																	// use
																	// StringTokenizer
																	// and size
																	// exactly

				// for (int start=0,end=coords.indexOf(','),posn=0; end!=-1 &&
				// posn<shapex.length; start=end+1,end=coords.indexOf(',',
				// start),even=!even) {
				for (int j = 0, jmax = coords.length(), posn = 0, start = 0; j < jmax
						&& posn < shapex.length; j++) {
					char ch = coords.charAt(j);
					if (ch != ',' && !Character.isWhitespace(ch))
						continue;
					int val = HTML.getAbsPct(coords.substring(start, j), 100,
							-1);
					if (val != -1) {
						if (even)
							shapex[posn] = val;
						else {
							shapey[posn] = val;
							posn++;
						}
						even = !even;
						npts++;
						start = j + 1;
					}
				}

				if (shape.startsWith("rect")) {
					// System.out.println("RECT npts="+npts+" "+shapex[0]+","+shapey[0]+" "+shapex[1]+","+shapey[1]);
					if (npts == 4) {
						imagemap.put(new Rectangle(shapex[0], shapey[0], Math
								.abs(shapex[1] - shapex[0]), Math.abs(shapey[1]
								- shapey[0])), dest);
					}
				} else if (shape.startsWith("circle")) { // x-center, y-center,
															// radius
					// (x,y) center, radius if (npts==3) { imagemap.put(new
					// Circle(shapex[0],shapey[0], shapex[1]), dest); }
					if (npts == 3) {
						imagemap.put(new Ellipse2D.Double(
								shapex[0] - shapex[1], shapey[0] - shapex[1],
								2 * shapex[1], 2 * shapex[1]), dest);
					}
				} else if (shape.startsWith("poly")) { // www.pbs.org/charlierose
														// has "polygon"
					if (npts > 0 /* && npts%2==0 */) {
						imagemap.put(new Polygon(shapex, shapey, npts / 2),
								dest);
					}
				} else if (shape.startsWith("default")) {
					// entire region
					// ...
					// if using sequenced data structure, could put default last
				} // else ignore
			}
		}
		// System.out.println("map size = "+imagemap.size());
		// want imagemap namespace
		return imagemap;
	}

	public boolean formatNodeContent(Context cx, int start, int end) {
		if (start == 0) {
			// int neww = Integers.parseInt(getAttr("width"), bbox.width),
			// newh=Integers.parseInt(getAttr("height"),bbox.height);
			// int w = Integers.parseInt(getAttr("width"), -1),
			// h=Integers.parseInt(getAttr("height"), -1);
			int wattr = Integers.parseInt(getAttr("width"), -1), hattr = Integers
					.parseInt(getAttr("height"), -1);

			if (wattr == -1 && hattr == -1) {
			} // usually not set
			else if (wattr != -1 && hattr != -1)
				setSize(wattr, hattr);
			else {
				Image img = getImage();
				int w = img.getWidth(this), h = img.getHeight(this);
				if (w != -1 && h != -1) { // one dimension but not other, scale
											// proportionally
					if (wattr != -1)
						setSize(wattr, (h * wattr) / w);
					else
						setSize((w * hattr) / h, hattr);
				}
			}
			// if (neww > width) neww=Math.max(1,width); //if (newh > height)
			// newh=Math.max(1,height);
			// System.out.print("neww="+neww+", width="+width+", newh="+newh+", height="+height);
			// System.out.println(" => "+neww+"x"+newh);
			// if (width==0) valid_=false;
			// bbox.setSize(neww, newh); baseline = newh;

			// => if just on axis, scale other proprotionally

			// WIDTH and HEIGHT override images natural dimensions (which are
			// then scaled)
			// bbox.setSize(Integers.parseInt(getAttr("width"),bbox.width),
			// Integers.parseInt(getAttr("height"),bbox.height));
			// bbox.grow(hspace*2+border*2, vspace*2+border*2); // should be
			// insets, not part of IMG space
			// baseline = bbox.height;

			// valid_=true;
			// bbox.grow(border,border); // should make this part of basic model
			// -- not sure if border part of WxH or adds to WxH
			// bbox.grow(border*2,border*2); margin.left+=border;
			// margin.top+=border; // should make this part of basic model --
			// not sure if border part of WxH or adds to WxH
		}
		// System.out.println("IMG "+getAttr("src")+" width="+bbox.width);

		/*
		 * // vertical alignment String align = getAttr("align"); if
		 * ("TOP".equalsIgnoreCase(align)) baseline=cx.size; else if
		 * ("MIDDLE".equalsIgnoreCase(align) || /non-spec (RW)
		 * /"CENTER".equalsIgnoreCase(align)) baseline=bbox.height/2; else
		 * baseline=bbox.height;
		 */
		/*
		 * else BOTTOM default, already handled in superclass:
		 * baseline=bbox.height;
		 */

		return super.formatNodeContent(cx, start, end);
	}

	// ** Width depends on formatting width (if image width smaller than
	// formatting width, scale down), so mark invalid. */
	// public void markDirtySubtreeDown(boolean leavestoo) { setValid(false); }

	/*
	 * same as superclass public void reformat(Node dirty) { int
	 * w=image_.getWidth(this), h=image_.getHeight(this);//, b=border2; //if
	 * (w!=-1 && h!=-1 && (w+b!=bbox.width || h+b!=bbox.height)) { if (w!=-1 &&
	 * h!=-1 && (w!=bbox.width || h!=bbox.height)) {
	 * //System.out.println("img width: "+bbox.width+" => "+(w+b)); //for (Node
	 * n=this; n!=null; n=n.getParentNode()) n.setDirty(); markDirty();
	 * getBrowser().repaint(500); } }
	 */

	/*
	 * public boolean paintNodeContent(Context cx, int start, int end) { // if
	 * some hyperlink active in cx.actives (maybe set a signal), then draw with
	 * blue outline // if (!loaded) { if (getAttr("alt")!=null) {
	 * g.setColor(Color.BLACK); g.drawString(getAttr("alt")); return false; }
	 * //if (href_!=null) g.setColor(Color.BLUE); for (int i=0; i<border; i++)
	 * g.drawRect(i,i, bbox.width-i,bbox.height-i); // fillRect?
	 * 
	 * //int dx=hspace+border, dy=vspace+border; //docclip.translate(-dx,-dy);
	 * g.translate(dx,dy); boolean ret = super.paintNodeContent(cx,start,end);
	 * //docclip.translate(dx,dy); g.translate(-dx,-dy);
	 * //g.setColor(cx.foreground); for (int i=0; i<border; i++) g.drawRect(i,i,
	 * bbox.width-i,bbox.height-i); return ret; }
	 */

	// if hyperlink exactly covers image, handle it in this class as may be
	// imagemap
	void putHref(URI href) {
		href_ = href/* .toString() */;
	}

	public boolean eventNode(AWTEvent e, Point rel) {
		if (super.eventNode(e, rel))
			return true;
		// System.out.println("eventNode in HTMLIMG "+getAttr("src"));
		if (href_ == null && !ismap_ && mapname_ == null)
			return false;

		Browser br = getBrowser();
		// UPDATE to Document's vars: if ((mapname_==null ||
		// (map=(Map)doc.getVar(mapname_))==null) && href_==null) return false;

		int eid = e.getID();
		if (eid == MouseEvent.MOUSE_MOVED) {
			URI oldhit = hit_;
			hit_ = null;

			Document doc = getDocument();
			Map<Shape, String> map = null;
			if (mapname_ != null)
				map = (Map<Shape, String>) doc.getVar(mapname_);
			// System.out.println(mapname_+" = "+map);

			if (map != null) {
				for (Iterator<Shape> n = map.keySet().iterator(); n.hasNext();) { // parallel
																					// arrays?
					Shape s = n.next();
					// assert o instanceof Shape:
					// o.getClass()+" in MAP doesn't implement Shape";
					if (s.contains(rel)) {
						try {
							hit_ = getDocument().getURI().resolve(map.get(s));
						} catch (IllegalArgumentException badref) {
						}
						// to do: default area...
						break;
					}
				}
			} else if (href_ != null) {
				// br.eventq(Browser.MSG_STATUS, "go to "+href_);
				// System.out.println("cur URI = "+br.getCurDocument().getURI()+", href="+href_);
				if (ismap_)
					try {
						hit_ = getDocument().getURI().resolve(
								href_.toString() + "?" + rel.x + "," + rel.y);
					} catch (IllegalArgumentException badpoint) {
					}
				else
					hit_ = href_;
			}

			if (hit_ != null) {
				if (oldhit != hit_) {
					// br.eventq(Browser.MSG_STATUS,
					// "Go to "+URIs.relativeURI(getDocument().getURI(), hit_));
					br.eventq(Browser.MSG_STATUS, "Go to "
							+ getDocument().getURI().relativize(hit_));
					br
							.setCursor(Cursor
									.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
			} else {
				br.setCursor(Cursor.getDefaultCursor());
				br.eventq(Browser.MSG_STATUS, "");
			}
			// System.out.println(hit_);

			// intercepted by hyperlink span!
		} else if (eid == MouseEvent.MOUSE_PRESSED) {
			if (hit_ != null) {
				br.setGrab(this);
			}

			// } else if (eid==MouseEvent.MOUSE_ENTERED) { => enter/exit regions
			// *within* map
		} else if (eid == MouseEvent.MOUSE_EXITED) { // map exit won't give a
														// move to within map
														// for exiting there
			br.setCursor(Cursor.getDefaultCursor());
			br.eventq(Browser.MSG_STATUS, "");
			hit_ = null; // just in case

		} // else
		return false;
		// return true;
	}

	public void event(AWTEvent e) {
		int eid = e.getID();
		Browser br = getBrowser();

		if (eid == MouseEvent.MOUSE_RELEASED) {
			// if (hit_!=null) br.eventq(openDocument", hit_);
			br.releaseGrab(this);
			if (hit_ != null)
				HTML.go(this, getAttr("target"), hit_); // TARGET-aware
		}
	}
}

/**
 * NOBR is not part of the HTML standard, and slashdot.org has pathological
 * <nobr> .. </nobr> with internal <br>
 * .
 */
class HTMLNOBR extends IHBox {
	public HTMLNOBR(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
	}

	// break only if breakBefore()/breakAfter()
	public boolean formatNode(int width, int height, Context cx) {
		boolean ss = super.formatNode(width, height, cx);

		// re-place children and set own dimensions
		int x = 0, y = 0, xmax = 0;
		for (int i = 0, imax = size(); i < imax;) {
			int jmax = imax, basemax = 0;

			// collect line
			for (int j = i; j < jmax; j++) {
				Node child = childAt(j);

				if (child.breakBefore() && j > i) {
					jmax = j;
					break;
				}

				basemax = Math.max(basemax, child.baseline);

				if (child.breakAfter()) {
					jmax = j + 1;
					break;
				}
			}

			// set that line
			int hmax = 0;
			for (int j = i; j < jmax; j++) {
				Node child = childAt(j);
				Rectangle cbbox = child.bbox;

				int dy = basemax - child.baseline;
				cbbox.setLocation(x, y + dy);
				// System.out.println(child.getName()+" "+x+","+(y+dy)+"  "+basemax+" - "+child.baseline+", hmax = "+hmax);

				x += cbbox.width + 2;
				xmax = Math.max(xmax, x);
				if ((cbbox.width != 0 || cbbox.height != 0)
						&& child instanceof LeafText)
					x += cx.getFont().charAdvance(' ').getX(); // bad bad bad
				hmax = Math.max(dy + cbbox.height, hmax);
			}

			x = 0;
			y += hmax;
			i = jmax;
		}
		// if (child.floats==LEFT || child.floats==RIGHT) ... ?

		bbox.setSize(xmax, y);

		return ss;
	}

	public boolean breakBefore() {
		return false;
	}

	public boolean breakAfter() {
		return false;
	}
}

/**
 * File upload button {"Browse..."). LATER: replace JFileChooser with Browser
 * instance with custom behavior set.
 */
class HTMLFileButton extends VButton {
	static JFileChooser jc = null; // static OK because modal

	public HTMLFileButton(String name, Map<String, Object> attrs, INode parent) {
		super(name, attrs, parent);
		new LeafUnicode("Browse...", null, this);
	}

	// can't do this with a script yet
	public void invoke() {
		// find associated type-in
		INode p = getParentNode();
		VEntry typein = null;
		for (int i = 0, imax = p.size(); i < imax; i++)
			if (p.childAt(i) instanceof VEntry)
				typein = (VEntry) p.childAt(0);

		// set initial file from typein
		// System.out.println("typein = "+typein);
		if (typein != null) {
			String content = typein.getContent();
			File filein = (content.length() <= 1 ? null : new File(content));
			if (jc == null)
				jc = new JFileChooser();
			if (filein == null)
				jc.setCurrentDirectory(null);
			else if (filein.exists())
				jc.setCurrentDirectory(filein.isDirectory() ? filein : filein
						.getParentFile());
		}

		Browser br = getBrowser(); // Browser is a Component
		if (br != null && jc.showOpenDialog(br) == JFileChooser.APPROVE_OPTION) {
			// set type to initial file
			// System.out.println("setting typein content to "+jc.getSelectedFile());
			if (typein != null)
				typein.setContent(jc.getSelectedFile().toString());
		}
	}
}
