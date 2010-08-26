package multivalent.std;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Map;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VFrame;
import multivalent.gui.VCheckbox;
import multivalent.gui.VTextArea;
import multivalent.gui.VScrollbar;
import multivalent.node.LeafUnicode;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.std.ui.DocumentPopup;

import phelps.lang.Integers;
import phelps.lang.Booleans;
import phelps.awt.Colors;



/**
	Show a PostIt-like note in a {@link VFrame}.

	If URI attribute set, content taken from, and if file:/// written to, to that file,
	which may be of any document type.
	Otherwise, saved inline as lines of ASCII (tree: <i>content</i> containing <code>line</code>+).
	Annotations are saved simply by recursing.

<!--
	NOTES
	should recognize scope attribute (PAGE, DOCUMENT, ALL)
-->

	@version $Revision: 1.8 $ $Date: 2002/02/17 18:23:33 $
*/
public class Note extends Behavior {
  /**
	Show note (visibile = true).
	<p><tt>"showNote"</tt>: <tt>arg=</tt> {@link java.lang.Boolean} (or <code>null</code> to toggle), <tt>in=</tt> {@link Note} <var>Note-instance</var>.
  */
  public static final String MSG_SHOW = "showNote";

  /**
	Delete note.
	<p><tt>"deleteNote"</tt>: <tt>in=</tt> {@link Note} <var>Note-instance</var>.
  */
  public static final String MSG_DELETE = "deleteNote";

  /**
	Pin/unpin note to associate document visually.
	<p><tt>"windowSetPinned"</tt>: <tt>arg=</tt> {@link java.lang.Boolean} (or <code>null</code> to toggle), <tt>in=</tt> {@link Note} <var>Note-instance</var>.
  */
  public static final String MSG_SET_PINNED = "windowSetPinned";

  /**
	Set background color of note.
	<p><tt>"editBackgroundColor"</tt>: <tt>arg=</tt> {@link java.awt.Color} / {@link java.lang.String} <var>color</var>, <tt>in=</tt> {@link Note} <var>Note-instance</var>.
  */
  public static final String MSG_BACKGROUND = "editBackgroundColor";

  public static final String ATTR_CLOSED = "closed";
  public static final String ATTR_FLOATING = "floating";
  // name, uri, x,y,width,height, foreground,background

  protected static Color deffg_=Color.BLACK, defbg_=Color.YELLOW.brighter();
  protected static final Random random = new Random(System.currentTimeMillis());


  /** Floating window. */
  protected VFrame win_ = null;
  /** Document nested in VFrame. */
  protected Document doc_ = null;
  /** Visible on screen or available in menu. */
  protected boolean viz_ = true;	// set in restore

  public Node getContent() { return win_; }


  /** Recurse to nested document. */
  public void buildBefore(Document doc) { super.buildBefore(doc); doc_.getLayers().buildBefore(doc_); }
  /** Recurse to nested document. */
  public void buildAfter(Document doc) { super.buildAfter(doc); doc_.getLayers().buildAfter(doc_); }


  /** Change background color, pinned status, .... */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//System.out.println(msg+", "+(se.getArg()==win_));
	//random.nextInt() -- regularly tickle?

	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_ANNO==msg) {
		if (!viz_) {
			INode menu = (INode)se.getOut();
			Browser br = getBrowser();
			//createUI("button", win_.getTitle(), new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, win_, this, null), menu, "Note", false);
			String title=win_.getTitle(); if (title==null || title.length()==0) title="(Note)";
			createUI("button", title, new SemanticEvent(br, MSG_SHOW, null/*win_*/, this, null), menu, "Note", false);
		}

	} else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_ && isEditable()) {
		Browser br = getBrowser();
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Pinned to Document", new SemanticEvent(br, MSG_SET_PINNED, null, this, null), menu, "EDIT", false);
		cb.setState(win_.isPinned());

		if (getAttr("uri")==null) {
			createUI("button", "Transparent", new SemanticEvent(br, MSG_BACKGROUND, "transparent", this, null), menu, "EDIT", false/*true if already this color*/);
			StringTokenizer st = new StringTokenizer(getPreference("colors", "Yellow Orange Green Blue"));  // don't use system colors menu?
			while (st.hasMoreTokens()) {
				String co = st.nextToken();
				createUI("button", co, new SemanticEvent(br, MSG_BACKGROUND, co, this, null), menu, "EDIT", false/*true if already this color*/);
			}
		}

		// X close box deletes, same as with lenses => removes from screen, have to go to menubar to restore (which for lens make new instance, for note retrieves old)
		createUI("button", "Delete Note", new SemanticEvent(br, MSG_DELETE, null, this, null), menu, "EDIT", false);
	}
	return false;
  }

  /** Catch corresponding VFrame's windowClosed, windowRaised, .... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	if (VFrame.MSG_CLOSED==msg && win_!=null && arg==win_) {
		//remove(); => don't show or put in menu
		viz_ = false;

	} else if (se.getIn()!=this) {
		// following are for event on this behavior instance

	} else if (MSG_DELETE==msg) {
		destroy();

	} else if (MSG_SHOW==msg) {
		boolean newviz = Booleans.parseBoolean(se.getArg(), !viz_);
		if (newviz!=viz_ && newviz) {
			Document doc = getDocument();
			doc.appendChild(win_);
System.out.println("adding "+win_+" to "+doc);
			//win_.repaint();
		}
		viz_ = newviz;

	} else if (MSG_SET_PINNED==msg) {
		boolean newpin = Booleans.parseBoolean(se.getArg(), !win_.isPinned());
		win_.setPinned(newpin);
		if (newpin) removeAttr(ATTR_FLOATING); else putAttr(ATTR_FLOATING, ATTR_FLOATING/*MediaAdaptor.DEFINED*/);
//System.out.println("FLOATING = "+getAttr(ATTR_FLOATING));

	} else if (MSG_BACKGROUND==msg) {
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
//System.out.println("setting "+getAttr("name")+" to background "+colorname+": "+gs);

			win_.repaint();
		}
	}

	return super.semanticEventAfter(se,msg);
  }

  /** Remove window node too. */
  public void destroy() {
	win_.close(); win_=null;
	super.destroy();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);

	Document doc = getDocument();
	win_ = new VFrame("Note",null, doc);
	win_.setPinned(getAttr(ATTR_FLOATING)==null);
	win_.setBounds(Integers.parseInt(getAttr("x"),100),Integers.parseInt(getAttr("y"),100), Integers.parseInt(getAttr("width"),300),Integers.parseInt(getAttr("height"),200));

	doc_ = new Document("Note",null, win_);	// free scrolling in Note!
	String name = "NOTE"+String.valueOf(Math.abs(random.nextInt()));
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
	viz_ = !Booleans.parseBoolean(getAttr(ATTR_CLOSED), false);
	if (!viz_) win_.remove();

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

	} else {		// simple editable
		INode body = new IVBox("body", null, null);
		//INode body = new IParaBox("lines",null, null);
		VTextArea/*INode*/ ed = new VTextArea("ed",null, doc_, body);
		ed.setScrollbarShowPolicy(VScrollbar.SHOW_AS_NEEDED);
		ed.editable = isEditable();
//System.out.println("editable = "+isEditable()+", layer name = "+getLayer().getName());
		//ed.dynamicDimensions(true,true);  //
		ed.setSizeChars(0,0);     // override VTextArea to set dimensions directly

		if (content!=null && content.size()>0 && ((String)content.childAt(0)).trim().length()>0) {
			win_.setIn(false);
			String txt = (String)content.childAt(0);
			if (!txt.endsWith("\\n")) txt += "\\n"; // sentinal

			// later: convert all escape characters first
			int lstart=0;
			for (int i=txt.indexOf("\\n"); i!=-1; lstart=i+2, i=txt.indexOf("\\n", lstart)) {
				INode line = new IParaBox("line",null, null);
//		line.breakafter = true;
				int wstart=lstart;
				for (int j=txt.indexOf(' ',lstart); j<i && j!=-1; wstart=j+1, j=txt.indexOf(' ', wstart)) {
					if (j-wstart > 1) new LeafUnicode(txt.substring(wstart,j), null, line);
				}
				if (i-wstart >= 1) new LeafUnicode(txt.substring(wstart,i), null, line);

				if (line.size()>0) body.appendChild(line);
			}

		} else content=null;

//System.out.println("body.size() = "+body.size());
		if (body.size()==0) {   // could have empty saved body, in addition to fresh
			INode protoline = new IParaBox("line",null, body);
			new LeafUnicode("",null, protoline);
		}

		if (content==null) { // new note
			//putAttr("foreground", Colors.getName(deffg_));
			//putAttr("background", Colors.getName(defbg_));
			if (win_.isPinned()) win_.bbox.translate(doc.getHsb().getValue(), doc.getVsb().getValue());     // adjust coordinates to scroll position

			// set focus and cursor here
			Leaf l = body.getFirstLeaf();
			br.getCursorMark().move(l,0);
			br.setCurNode(l,0);
			//br.setCurDocument(doc_); ... maybe
			br.setScope(doc_);
		}

		// colors
		StyleSheet ss = doc_.getStyleSheet();
		CLGeneral gs = new CLGeneral();
		gs.setForeground(Colors.getColor(getAttr("foreground"), deffg_));
		gs.setBackground(Colors.getColor(getAttr("background"), defbg_));
		// font...
		ss.put("note", gs);

		gs = new CLGeneral();
		gs.setPadding(5);
		ss.put("ed", gs);
	}

	//if (content!=null) sublay.buildBeforeAfter(doc_); => we're in restore phase, not build!
	return;
  }


/*
  public void setLabel() {
	//if (uipart==null || uipart.length==0) return;
	// identify note by excerpt
	Leaf leaf = root.getFirstLeaf();
	StringBuffer sb = new StringBuffer(30);
	for (int i=0; leaf!=null && i<3; leaf=leaf.getNextLeaf(), i++) leaf.clipboardBeforeAfter(sb);
	sb.setLength(Math.min(20, sb.length()));
	//uipart[0].setLabel(sb.length()==0?"Note":"Note: "+sb.substring(0));
  }*/



// X enlarge vertically (only) to contain content, if necessary
// => don't grow Note, scroll it: scroll if necessary to show content
// if everything deleted, make line with single space, so doesn't go transparent and impossible to set cursor


  public ESISNode save() {
	// maintained as attributes: foreground, background, float/pinned
//	if (getAttr(ATTR_BEHAVIOR)==null) putAttr(ATTR_BEHAVIOR, "Note");
	if (viz_) removeAttr(ATTR_CLOSED); else putAttr(ATTR_CLOSED, "true");

	Rectangle r = win_.bbox;
	putAttr("x", String.valueOf(r.x)); putAttr("y", String.valueOf(r.y)); putAttr("width", String.valueOf(r.width)); putAttr("height", String.valueOf(r.height));
//System.out.println("attrs = "+attr_);

	ESISNode e = super.save();  // after updating attrs

	if (getAttr("uri")==null) {  // inline -- for now only editable kind
		StringBuffer csb = new StringBuffer(2000);
		doc_.clipboardBeforeAfter(csb);  // doesn't save character attributes yet
		String trim = csb.substring(0).trim();
		csb.setLength(0);
		for (int i=0,imax=trim.length(); i<imax; i++) {
			char ch = trim.charAt(i);
			if (ch=='\n') csb.append("\\n"); else csb.append(ch);
		}

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
//System.out.println(layname+" vs "+l.getName());
		if (layname.equals(l.getName())) sube=l.save();
		if (sube!=null) e.appendChild(sube);
	}

	return e;
  }
}
