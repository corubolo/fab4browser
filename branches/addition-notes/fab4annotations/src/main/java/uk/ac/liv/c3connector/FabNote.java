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



package uk.ac.liv.c3connector;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.CHashMap;
import multivalent.CLGeneral;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.StyleSheet;
import multivalent.gui.VCheckbox;
import multivalent.gui.VFrame;
import multivalent.gui.VMenu;
import multivalent.gui.VScrollbar;
import multivalent.gui.VTextArea;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import multivalent.std.Note;
import multivalent.std.ui.DocumentPopup;
import phelps.awt.Colors;
import phelps.doc.RobustLocation;
import phelps.lang.Booleans;
import phelps.lang.Integers;
import uk.ac.liv.c3connector.ui.AnnotationProperties;
import uk.ac.liv.c3connector.ui.FabAnnoListRenderer;
import uk.ac.liverpool.fab4.Fab4;
import uk.ac.liverpool.fab4.Fab4utils;

import com.pt.awt.NFont;


/**
 * This class is the behaviour for the main type of notes (the sticky notes).
 * 
 * 
 * @author fabio
 *
 */

public class FabNote extends Behavior {
	/**
	Show note (visibile = true).
	<p><tt>"showNote"</tt>: <tt>arg=</tt> {@link java.lang.Boolean} (or <code>null</code> to toggle), <tt>in=</tt> {@link Note} <var>Note-instance</var>.
	 */
	public static final String MSG_SHOW = "showNote";

	public static NFont FONT_TEXT;
	/**
	 *
	 */
	public static final String ATTR_CREATION_DATE = "creationDate";
	/**
	 *
	 */
	public static final String ATTR_PAGE = "pageNumber";
	/**
	 *
	 */
	public static final String MSG_MOVE_POINT = "movePoint";

	/**
	Delete note.
	<p><tt>"deleteNote"</tt>: <tt>in=</tt> {@link Note} <var>Note-instance</var>.
	 */
	public static final String MSG_DELETE = "deleteNote";

	public static final String MSG_ANON = "anonymous_note";

	public static final String MSG_SET_PINNED = "windowSetPinned";

	/**
	Set background color of note.
	<p><tt>"editBackgroundColor"</tt>: <tt>arg=</tt> {@link java.awt.Color} / {@link java.lang.String} <var>color</var>, <tt>in=</tt> {@link Note} <var>Note-instance</var>.
	 */
	public static final String MSG_BACKGROUND = "editBackgroundColor";

	public static final String ATTR_CLOSED = "closed";
	public static final String ATTR_FLOATING = "floating";
	public static final String ATTR_ANONYMOUS = "anonymous";

	// name, uri, x,y,width,height, foreground,background

	protected Color deffg_=Color.BLACK, defbg_=Color.YELLOW.brighter();
	protected static final Random random = new Random(System.currentTimeMillis());

	public static final String MSG_PROPS = "showNoteProperties";

	Mark dest = null;

	/** Floating window. */
	protected VFrame win_ = null;
	/** Document nested in VFrame. */
	protected Document doc_ = null;
	/** Visible on screen or available in menu. */
	protected boolean viz_ = true;	// set in restore

	protected float rx=1.0f, ry=1.0f;

	public boolean callout = false;

	int tw,th;

	private String title = DistributedPersonalAnnos.author;

	public Node getContent() { return win_; }

	Rectangle opos;

	String ocont;

	Point opoint;

	private VCheckbox vanon;

	/** Recurse to nested document. */
	@Override
	public void buildBefore(Document doc) { super.buildBefore(doc); doc_.getLayers().buildBefore(doc_); }
	/** Recurse to nested document. */
	@Override
	public void buildAfter(Document doc) { super.buildAfter(doc); doc_.getLayers().buildAfter(doc_); }


	public boolean isModified() {

		//		FabAnnotation fa = (FabAnnotation) getValue(DistributedPersonalAnnos.FABANNO);
		//		if (fa == null)
		//			return false;

		if (win_.getBbox().x!=opos.x || win_.getBbox().y!=opos.y)
			//System.out.println("1");
			//System.out.println(opos);
			//System.out.println(win_.getBbox());
			return true;
		if (win_ instanceof ArrowVFrame) {
			Document doc = (Document) getBrowser().getRoot().findBFS("content");
			float zl = doc.getMediaAdaptor().getZoom();
			ArrowVFrame arro = (ArrowVFrame)win_;
			int px = arro.px;
			int py = arro.py;

			if (Math.abs(opoint.x-px)>1/zl+1 || Math.abs(opoint.y-py)>1/zl+1 )
				//System.out.println("2");
				//System.out.println(opoint);
				//System.out.println(px + " "+ py);
				return true;
		}
		if (!ocont.equals(Fab4utils.getTextSpaced(doc_)))
			//System.out.println("3");
			//System.out.println(ocont);
			//System.out.println(getStringContent().toString());
			return true;
		return false;
	}

	/** Change background color, pinned status, .... */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (VFrame.MSG_CLOSED==msg)
			if (se.getArg()==win_) {
				//System.out.println("closed");
				//Document doc = (Document) getBrowser().getRoot().findBFS("content");
				//Layer personal = doc.getLayer(Layer.PERSONAL);
				//personal.removeBehavior(this);
			}
		if (super.semanticEventBefore(se,msg))
			return true;
		else if (VMenu.MSG_CREATE_ANNO==msg) {
			if (!viz_) {
				INode menu = (INode)se.getOut();
				Browser br = getBrowser();
				//createUI("button", win_.getTitle(), new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, win_, this, null), menu, "Note", false);
				//String title=win_.getTitle(); if (title==null || title.length()==0) title="(Note)";
				createUI("button", title, new SemanticEvent(br, FabNote.MSG_SHOW, null/*win_*/, this, null), menu, "Note", false);
			}
		} else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_ && isEditable()) {
			Browser br = getBrowser();
			INode menu = (INode)se.getOut();
			FabAnnotation fa = (FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO);
			if (fa==null)
				createUI ("button", "Delete Note", new SemanticEvent(br, FabNote.MSG_DELETE, null, this, null), menu, "EDIT", false);
			else
				createUI ("button", "Note properties", new SemanticEvent(br, FabNote.MSG_PROPS, fa, this, null), menu, "EDIT", false);
			vanon = (VCheckbox) createUI( "checkbox", "Anonymous annotation",new SemanticEvent(br, FabNote.MSG_ANON, null/*win_*/, this, null), menu, "Note", false);

			if (getAttr("uri")==null) {
				createUI("button", "Transparent", new SemanticEvent(br, FabNote.MSG_BACKGROUND, "transparent", this, null), menu, "EDIT", false/*true if already this color*/);
				StringTokenizer st = new StringTokenizer(getPreference("colors", "Yellow Orange Green Blue"));  // don't use system colors menu?
				while (st.hasMoreTokens()) {
					String co = st.nextToken();
					createUI("button", co, new SemanticEvent(br, FabNote.MSG_BACKGROUND, co, this, null), menu, "EDIT", false/*true if already this color*/);
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see multivalent.Behavior#eventBefore(java.awt.AWTEvent, java.awt.Point, multivalent.Node)
	 */
	@Override
	public boolean eventAfter(AWTEvent e, Point rel, Node n) {
		if (callout && win_!=null)
			((ArrowVFrame)win_).arrowEvent(e, rel);
		return super.eventAfter(e, rel, n);
	}


	public boolean isLampshade() {
		if (win_!=null)
			return win_.isLampshade();
		return true;
	}


	public void setLampshade(boolean s) {
		if (win_!=null)
			if (s!=win_.isLampshade())
				win_.setLampshade(s);
	}

	public void setTitleBg(Color c) {
		if (win_!=null) {
			win_.setTitleBg(c);
			win_.repaint();
		}
	}
	public Color getTitleBg() {
		if (win_!=null)
			return win_.getTitleBg();
		return null;
	}

	/** Catch corresponding VFrame's windowClosed, windowRaised, .... */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Object arg = se.getArg();
		if (FabNote.MSG_SHOW==msg && win_!=null && arg==win_)
			show(true);
		//		else if (msg.equals(Zoom.MSG_SET)) {
		//
		//		}
		if (VFrame.MSG_CLOSED==msg && win_!=null && arg==win_)
			//remove(); => don't show or put in menu
			viz_ = false;
		else if (se.getIn()!=this) {
			// following are for event on this behavior instance

		} else if (FabNote.MSG_DELETE==msg)
			destroy();
		else if (FabNote.MSG_ANON==msg) {
			if (getAttr(FabNote.ATTR_ANONYMOUS)==null){
				putAttr(FabNote.ATTR_ANONYMOUS,	"true");
				vanon.setState(true);
				setTitle("anonymous");
				setTitleBg(Color.orange);

			}
			else{
				removeAttr(FabNote.ATTR_ANONYMOUS);
				vanon.setState(false);
				setTitle(title);
				setTitleBg(FabAnnoListRenderer.TOPUBLISH);
			}
			//System.out.println("anon");

		} else if (FabNote.MSG_PROPS==msg) {
			FabAnnotation fa = (FabAnnotation)arg;
			final Fab4 ff = Fab4.getMVFrame(getBrowser());
			AnnotationProperties ap  = new AnnotationProperties(ff, false,fa);
			ap.setVisible(true);
		} else if (FabNote.MSG_SET_PINNED==msg) {
			boolean newpin = Booleans.parseBoolean(se.getArg(), !win_.isPinned());
			win_.setPinned(newpin);
			if (newpin)
				removeAttr(FabNote.ATTR_FLOATING);
			else
				putAttr(FabNote.ATTR_FLOATING, FabNote.ATTR_FLOATING/*MediaAdaptor.DEFINED*/);
			//			System.out.println("FLOATING = "+getAttr(ATTR_FLOATING));

		} else if (FabNote.MSG_BACKGROUND==msg) {
			Color newcolor=null; String colorname=null;
			if (arg==null) {
				// ask user
			} else if (arg instanceof Color) { newcolor=(Color)arg; colorname=Colors.getName(newcolor); }
			else if (arg instanceof String) { colorname=(String)arg; newcolor = Colors.getColor(colorname); }

			if (/*newcolor!=null &&*/ colorname!=null) {
				defbg_ = newcolor;  // future notes default to new color
				putAttr("background", colorname);

				StyleSheet ss = doc_.getStyleSheet();
				CLGeneral gs = (CLGeneral)ss.get("note"/*doc_, null*/);
				gs.setBackground(newcolor);
				win_.repaint();
			}

		}


		return super.semanticEventAfter(se,msg);
	}

	public void setFontTitle(NFont f) {
		if (win_!=null)
			win_.setFONT_TITLE(f);
	}


	public void hide() {
		if (win_!=null)
			win_.close();
		System.out.println("hide");

	}
	public void show(boolean rise) {
		//boolean newviz = true;
		if (!viz_ ) {
			Document doc = getDocument();
			doc.appendChild(win_);
		}
		viz_ = true;
		if (win_!=null && rise)
			win_.raise();
		if (win_!=null && !rise)
			win_.raise(false);
		putAttr("closed", "false");
	}

	/** Remove window node too. */
	@Override
	public void destroy() {
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		if (win_!=null)
			win_.close();
		win_=null;
		if (getValue(DistributedPersonalAnnos.FABANNO)!=null)
			((FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO)).setLoaded(false);
		try {
			if (doc!=null)
				doc.deleteObserver(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}

	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		if (win_!=null)
			return;
		super.restore(n,attr, layer);
		//		System.out.println("----***!restore");
		//Document doc = getDocument();
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		float zl = doc.getMediaAdaptor().getZoom();
		doc = getDocument();
		Random r3 = new Random();
		int rr = r3.nextInt(100);
		callout = getAttr("callout")!=null;

		int aa = 20+rr;
		if (callout) {
			ArrowVFrame arro = new ArrowVFrame("Note",null, doc);
			win_ = arro;
			int px,py;
			px = Integers.parseInt(getAttr("px"),aa-12 + doc.getHsb().getValue());
			py = Integers.parseInt(getAttr("py"),aa-12 + doc.getVsb().getValue());
			if (n!=null)
				for (int i=0,imax=n.size(); i<imax; i++) {
					Object o = n.childAt(i);
					if (o instanceof ESISNode) {
						ESISNode child = (ESISNode)o;
						if ("destination".equals(child.getGI())) { dest = RobustLocation.attach(child.attrs, doc); break; }

					}
				}
			else {
				Span sel = getBrowser().getSelectionSpan();
				if (sel.isSet())
					dest = sel.getEnd();
			}

			if (dest!=null)
				arro.destination = dest.leaf;
			else {

			}
			px = (int)(px*zl);
			py = (int)(py*zl);
			opoint = new Point(px,py);
			arro.setPx(px);
			arro.setPy(py);
			doc.addObserver(this);

		} else
			win_ = new VFrame("Note",null, doc);
		win_.setShrinkTitle(true);
		win_.setTransparent(true);
		win_.setTitle(title);
		win_.setPinned(getAttr(FabNote.ATTR_FLOATING)==null);
		Rectangle r = new Rectangle();
		r.x = (int)(Integers.parseInt(getAttr("x"),aa) * zl);
		r.y = (int)(Integers.parseInt(getAttr("y"),aa) * zl);
		r.width = Integers.parseInt(getAttr("width"),220);
		r.height = Integers.parseInt(getAttr("height"),100);
		win_.setBounds(r.x,r.y,r.width,r.height);
		opos = new Rectangle(win_.getBbox());

		doc_ = new Document("Note",null, win_);	// free scrolling in Note!
		String name = "NOTE"+String.valueOf(Math.abs(FabNote.random.nextInt()));
		putAttr("name", name);
		doc_.padding = INode.INSETS[3];
		doc_.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);

		Layer sublay = doc_.getLayers();
		ESISNode content = null;
		if (n!=null) {
			sublay.restoreChildren(n, sublay);
			content = sublay.getAux("content");     // beautiful: pagewise annos and now this in aux
		}

		Browser br = getBrowser();
		viz_ = !Booleans.parseBoolean(getAttr(FabNote.ATTR_CLOSED), false);
		if (!viz_)
			win_.remove();

		String uri = getAttr("uri");
		if (uri!=null) {	// content stored elsewhere
			doc_.editable = false;
			new LeafUnicode("Loading "+uri,null, doc_);
			try {
				DocInfo di = new DocInfo(doc.getURI().resolve(uri));
				di.doc = doc_;
				br.eventq(Document.MSG_OPEN, di);
			} catch (IllegalArgumentException leaveempty) {}
			return;

		}
		final INode body = new IVBox("body", null, null);
		final VTextArea ed = new VTextArea("ed", null, doc_, body);
		FabAnnotation fa = (FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO);
		ed.editable = false;
		if (fa!=null)
			if (fa.getSigner()!=null)
				if (fa.getSigner().isItsme())
					ed.editable = true;
		ed.setScrollbarShowPolicy(VScrollbar.SHOW_AS_NEEDED);
		//ed.editable = isEditable();
		ed.setSizeChars(0, 0); // override VTextArea to set dimensions directly
		//ed.
		if (content != null && content.size() > 0
				&& ((String) content.childAt(0)).trim().length() > 0) {
			win_.setIn(false);
			String txt = (String) content.childAt(0);
			//ocont = txt;
			if (!txt.endsWith("<br/>"))
				txt += "<br/>"; // sentinal

			// later: convert all escape characters first
			int lstart = 0;
			//System.out.println(txt);
			for (int i = txt.indexOf("<br/>"); i != -1; lstart = i + 5, i = txt
			.indexOf("<br/>", lstart)) {
				final INode line = new IParaBox("line", null, null);
				// line.breakafter = true;
				int wstart = lstart;
				for (int j = txt.indexOf(' ', lstart); j < i && j != -1; wstart = j + 1, j = txt
				.indexOf(' ', wstart))
					if (j - wstart > 0)
						new LeafUnicode(txt.substring(wstart, j), null, line);
				if (i - wstart >= 1)
					new LeafUnicode(txt.substring(wstart, i), null, line);

				if (line.size() > 0)
					body.appendChild(line);
			}

		} else
			content = null;
		if (body.size() == 0) { // could have empty saved body, in addition to
			// fresh
			final INode protoline = new IParaBox("line", null, body);
			new LeafUnicode("", null, protoline);
		}
		//  ********* 	new note *************
		if (content == null) {
			if (win_.isPinned())
				win_.bbox.translate(doc.getHsb().getValue(), doc.getVsb()
						.getValue()); // adjust coordinates to scroll position
			// set focus and cursor here
			final Leaf l = body.getFirstLeaf();
			br.getCursorMark().move(l, 0);
			br.setCurNode(l, 0);
			br.setScope(doc_);
			br.setCurDocument(doc);
			win_.setClosable(false);
			setFontTitle(VFrame.FONT_TITLE_ITALIC);
			setTitleBg(FabAnnoListRenderer.TOPUBLISH);
			ed.editable = true;
			win_.raise();
		}

		// colors
		final StyleSheet ss = doc_.getStyleSheet();
		CLGeneral gs = new CLGeneral();
		gs.setForeground(Colors.getColor(getAttr("foreground"), deffg_));
		gs.setBackground(Colors.getColor(getAttr("background"), defbg_));
		// font...
		if (FabNote.FONT_TEXT == null)
		    FabNote.FONT_TEXT = NFont.getInstance("Sans", NFont.WEIGHT_NORMAL,
                        NFont.FLAG_EXPANDED, 14);
		gs.setFont(FabNote.FONT_TEXT);
		ss.put("note", gs);
		gs = new CLGeneral();
		gs.setPadding(5);
		ss.put("ed", gs);
		//if (win_!=null)
		win_.setTitle(title);
		ocont = Fab4utils.getTextSpaced(doc_);

		return;
	}


	public void setTitle(String s) {
		title = s;
		if (win_!=null)
			win_.setTitle(title);
	}



	@Override
	public ESISNode save() {
		// maintained as attributes: foreground, background, float/pinned
		if (viz_)
			removeAttr(FabNote.ATTR_CLOSED);
		else
			putAttr(FabNote.ATTR_CLOSED, "true");
		if (getAttr(FabNote.ATTR_PAGE)==null) {

		}
		Rectangle r = win_.bbox;
		ESISNode e;
		float zl = getDocument().getMediaAdaptor().getZoom();
		r.x = (int)(r.x / zl);
		r.y = (int)(r.y / zl);
		//		r.width = (int)(r.height / zl);
		//		r.height = (int)(r.width / zl);
		putAttr("x", String.valueOf(r.x)); putAttr("y", String.valueOf(r.y)); putAttr("width", String.valueOf(r.width)); putAttr("height", String.valueOf(r.height));
		if (callout) {
			ArrowVFrame arro = (ArrowVFrame) win_;
			int px = (int)(arro.getPx()/zl);
			int py = (int)(arro.getPy()/zl);
			putAttr("px", String.valueOf(px));
			putAttr("py", String.valueOf(py));
			e= super.save();  // after updating attrs
			if (e!=null && arro.destination!=null){
				CHashMap<Object> pdest_=new CHashMap<Object>(5);
				RobustLocation.descriptorFor(arro.destination,0, getDocument(), pdest_);
				e.appendChild(new ESISNode("destination", pdest_));
			}
			//			System.out.println("SAVE: "+ arro.getPx() +" - "+ arro.getPy());
			//			System.out.println("SAVE: "+arro.destination);
		} else
			e = super.save();  // after updating attrs

		if (getAttr("uri")==null) {  // inline -- for now only editable kind
			StringBuffer csb = getStringContent();
			ESISNode content = new ESISNode("content");
			e.appendChild(content);
			content.appendChild(csb.toString());
		}

		// save annos on note
		Layer sublay = doc_.getLayers();
		String layname = getLayer().getName();
		for (int i=0,imax=sublay.size(); i<imax; i++) {
			Layer l = (Layer)sublay.getBehavior(i);
			ESISNode sube = null;
			if (layname.equals(l.getName()))
				sube=l.save();
			if (sube!=null)
				e.appendChild(sube);
		}

		return e;
	}
	/**
	 * @return
	 */
	private StringBuffer getStringContent() {
		StringBuffer csb = new StringBuffer(2000);
		doc_.clipboardBeforeAfter(csb);  // doesn't save character attributes yet
		String trim = csb.substring(0).trim();
		csb.setLength(0);
		for (int i=0,imax=trim.length(); i<imax; i++) {
			char ch = trim.charAt(i);
			if (ch=='\n')
				csb.append("<br/>");
			else
				csb.append(ch);
		}
		// ampty string creates a <content/> that breaks MV's xml parser
		if (csb.length()==0)
			csb.append("<br/>");
		//System.out.println("00!!!!!");
		return csb;
	}
	/**
	 * 
	 */
	public void scrollTo() {
		if (win_!=null)
			win_.scrollTo();

	}

	public void fitIntoPage() {
		if (win_!=null)
			win_.fitIntoPage();

	}
}