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
package multivalent;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.Root;
import multivalent.std.MediaLoader;

import com.pt.awt.NFont;
import com.pt.io.InputUni;
import com.pt.io.InputUniString;



/**
	This class implements a browser window.
	It interfaces with the operating systems GUI,
	holds a document tree with both user interface and content,
	manages flow of control through associated behaviors according to the protocols,
	and holds resources shared among all documents in a window.
	Don't instantiate directly -- use {@link Multivalent#getBrowser(String)}.

	<p>State:
	<ul>
	<li>{@link #getName() browser name}
	<li>mouse location within window ({@link #getCurScrn()})
	<li>node and offset under mouse ({@link #getCurNode()} and {@link #getCurOffset()})
	<li>the {@link #getGrab() grab}
	<li>hooks to {@link CursorMark cursor} and {@link #getSelectionSpan() selection span}
		(at most one of which is valid at a time)
	 <!-- these could be put in Root's global vars, but for convenience and type checking available here -->
	<li>hook to {@link #getOffImage() offscreen image}
	<li>hook to overall {@link #getRoot()} of GUI and content
	<li>hook to "current" {@link #getCurDocument() document}, the one with the focus
	</ul>

	<p>Functionality:
	<ul>
	<li>{@link #eventq(AWTEvent)} add event to event queue, either low level or semantic
	</ul>

	<p>Browsers do <em>not</em> contain:
	<ul>
	<li>Attributes and variables (these can be put in the {@link Root})
	</ul>

<!--
	runtime switch to dump out everything, at various levels of detail
-->

	@version $Revision$ $Date$
 */
public class Browser extends JPanel { //implements WindowListener--many tentacled //ClipboardOwner,Transferable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	private static final boolean DEBUG = false;	// when true, keeps eye on formatting and other protocols


	/**
	Create new browser window.
	<p><tt>"newBrowserInstance"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_NEW = "newBrowserInstance";

	/**
	Close browser window, safely saving component documents as needed and releasing resources.
	<p><tt>"closeBrowserInstance"</tt>: <tt>arg=</tt> {@link multivalent.Browser} <var>browser to close</var>
	 */
	public static final String MSG_CLOSE = "closeBrowserInstance";

	/**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"showStatus"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>message-to-show</var>.
	 */
	public static final String MSG_STATUS = "showStatus";

	/**
	Construct toolbar by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Toolbar"</tt>: <tt>out=</tt> {@link INode} <var>instance-under-construction</var>.
	 */
	public static final String MSG_CREATE_TOOLBAR = "createWidget/Toolbar";

	/** Like {@link #MSG_CREATE_TOOLBAR}, but for a second toolbar, under the first. */
	public static final String MSG_CREATE_TOOLBAR2 = "createWidget/Toolbar2";


	//** If <code>true</code>, paint directly to screen, useful when tuning clipping regions for performance. */
	//static final boolean DIRECT_PAINT = false;

	// high level profiling -- remove by setting prof=false
	private static final boolean PROF = true;
	private static final int
	RESTORECNT=0, BUILDCNT=1, FORMATCNT=2, PAINTCNT=3, EVENTCNT=4, SAVECNT=5, SELECTCNT=6,
	RESTORETIME=7, BUILDTIME=8, FORMATTIME=9, PAINTTIME=10, EVENTTIME=11, SAVETIME=12, SELECTTIME=13,
	PAINTALL=17,
	EVENTMOVE=18,
	NUMPROF=19
	;
	private static long[] profs = new long[Browser.NUMPROF];

	public static final String PROTOCOL_RESTORE="restore", PROTOCOL_BUILD="build", PROTOCOL_FORMAT="format", PROTOCOL_REFORMAT="reformat", PROTOCOL_PAINT="paint", PROTOCOL_EVENT="event", PROTOCOL_SEMANTICEVENT="semanticEvent";

	private static int winxoff_=0, winyoff_=0;
	private final long start_ = System.currentTimeMillis();


	// synthetic exit/enter events for spans.  spans can get source themselves; dummied up fields not important
	private final MouseEvent MOUSE_EXIT = new MouseEvent(this/*can't have null, and has to be java.awt.Component*//*onode*/, MouseEvent.MOUSE_EXITED, 0/*System.currentTimeMillis()*/, 0, 0,0,/*curscrn_.x,curscrn_.y*/ 0, false);
	private final MouseEvent MOUSE_ENTER = new MouseEvent(this/*onode*/, MouseEvent.MOUSE_ENTERED, 0/*System.currentTimeMillis()*/, 0, 0,0,/*curscrn_.x,curscrn_.y*/ 0, false);
	private final Point POINT_BOGUS = new Point(-1,-1);

	private EventListener grabOwner_ = null;	// share across all Browsers?
	private int gdx_=0, gdy_=0;

	private DocBox docbox_ = null;
	private JLabel infobar = new JLabel(" ");	// temporary

	private Image offImage_ = null; //Graphics2D offg_=null;
	private int numBuffers_;
	private int winw_=100, winh_=100;

	private String name_=null;
	private Span sel_ = null;
	private CursorMark cur_ = null;
	INode scope_ = null;
	Root root_ = null;
	INode docroot_ = null;
	Document curdoc_=null;	// want to set this when cursor is set => need observers on spans!
	//protected static Behavior selectOwner = null;	// shared across all Browsers or per-Browser?
	INode toolbar=null, toolbar2=null;	// temporary, until get toolbar manager behavior
	//INode docroot = null;


	private Point curscrn_ = new Point(0,0);
	private Node curnode_ = null;
	private int curoff_ = -1;
	private List<ContextListener> vrangeActive_ = null;
	private boolean flost_ = false;
	private boolean finit_ = false;


	/** Java AWT Frame with Browser Canvas as only child. */
	class DocBox extends JFrame { // => move to standalone Multivalent, except for (1) startup feedback, (2) infobar
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Browser br_;

		DocBox(Browser br, JLabel infobar) {
			super("Multivalent");
			br_ = br;

			// set Frame and temporary UI -- later have multiple views, MVC and all that
			Container c = getContentPane();
			c.setLayout(new BorderLayout());
			c.add(br, BorderLayout.CENTER);
			c.add(infobar, BorderLayout.SOUTH);

			pack();	// when get native UI, will just have a Frame?
			//setVisible(true);

			enableEvents(AWTEvent.WINDOW_EVENT_MASK);   // | AWTEvent.COMPONENT_EVENT_MASK);
		}
		/*  public void setBounds(int x,int y, int width,int height) {
		System.out.println("docbox setbounds");
		super.setBounds(x,y, width,height);
	}*/
		protected void processEvent(AWTEvent e) {
			br_.processEvent(e);
			//if (e.getID()==WindowEvent.WINDOW_CLOSING) destroy();
			super.processEvent(e);	// passes out to WindowListeners, as for click on close box
			//System.out.println("DocBox event "+e.getID()+" "+e);
		}

		// don't clear blackground => done by JFrame
		//public void update(Graphics g) { paint(g); }


		/*protected AWTEvent coalesceEvents(AWTEvent e1, AWTEvent e2) {
		if (PaintEvent.PAINT==e1.getID() || PaintEvent.UPDATE==e1.getID()) {
			PaintEvent pe1 = (PaintEvent)e1, pe2 = (PaintEvent)e2;
			Rectangle r1 = pe1.getUpdateRect(), r2 = pe2.getUpdateRect();
			System.out.println("coalese "+r1+" + "+r2);
			Toolkit tk = this.getToolkit();
			EventQueue q = tk.getSystemEventQueue();
			// would like to march through queue and coalese all paint
			if (r1.intersects(r2)) return new PaintEvent((Component)e1.getSource(), e1.getID(), r1.union(r2));
		}
		return null;
	}*/
	}


	/**
	Builds a browser window, sets bounds, constructs UI document and content document via splash page (which in turn establishes system behaviors).
	Don't instantiate directly -- get instance by name via class Multivalent.
	<!-- LATER: start up new thread? -->
	 */
	/*package-private*/ Browser(String logicalname, String systemHub, boolean standalone) {
		assert systemHub!=null;

		name_ = logicalname;


		// 1. OS window
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int winw=640+60, winh=700, winx=0, winy=0;
		String sgeom=logicalname==null? null: Multivalent.getInstance().getPreference(logicalname+"-geom", null);
		if (sgeom!=null) {
			int inx = sgeom.indexOf('@');
			if (inx!=-1) {
				String windowdim=sgeom.substring(0,inx), windowposn=sgeom.substring(inx+1);
				try {
					if ((inx=windowdim.indexOf('x'))!=-1) {
						winw = Integer.parseInt(windowdim.substring(0,inx));
						winh = Integer.parseInt(windowdim.substring(inx+1));
					}
					if ((inx=windowposn.indexOf(','))!=-1) {
						winx = Integer.parseInt(windowposn.substring(0,inx));
						winy = Integer.parseInt(windowposn.substring(inx+1));
					}
				} catch (NumberFormatException e) {}
			}
		} else /* (sgeom==null/*logicalname==null || logicalname.length()==0)*/ {
			Browser.winxoff_+=30; if (winx+winw+Browser.winxoff_ > screen.width-100) Browser.winxoff_=-winx;
			Browser.winyoff_+=30; if (winy+winh+Browser.winyoff_ > screen.height-100) Browser.winyoff_=-winy;
		}
		//setSize(Math.min(winw,screen.width),Math.min(winh,screen.height-50));	// HTML pages designed for min 640x480
		winw_=winw; winh_=winh;

		if (standalone) {
			docbox_ = new DocBox(this, infobar);
			docbox_.setLocation(Browser.winxoff_+Math.min(winx,screen.width-winw), Browser.winyoff_+Math.min(winy,screen.height-winh));

			docbox_.show();	// show quickly during startup
		}

		//try { createBufferStrategy(1, new BufferCapabilities(new ImageCapabilities(false), new ImageCapabilities(false), null/*BufferCapabilities.FlipContents.COPIED*/)); } catch (AWTException e) { System.err.println(e); }
		setDoubleBuffered(false);	// handle own double buffering so image available for lenses
		setOpaque(true);
		//System.out.println("double? = "+isDoubleBuffered());


		// 2. GUI and content
		root_ = new Root(null, this);	// Root is Document subclass, so nodes always have a Document.  This one never goes away, so it can store pan-document attributes (not in Browser anymore).
		buildUI(root_);	// UI doc

		Document doc = new Document("content",null, docroot_);
		//	Document doc = new Document("CONTENT",null, root_);

		// Load in splash page as soon as possible, before loading in system behaviors (so conveniently isn't added to forward/back buttons' list)
		//try {
		//restore(new DocInfo(URI.create(getClass().getResource("/sys/Splash.html").toString())), doc);
		// synchronously, bootstrap=true flag
		// call HTML directly (buildBeforeAfter) since can guarantee genre of splash page?  NOT WORTH IT.

		// send no sem ev!  loading entanglements with getSelectionSpan(), which creates system layer, ...
		/*
	doc.uri = URI.create(getClass().getResource("/sys/Splash.html").toString());
	MediaAdaptor r = (MediaAdaptor)Behavior.getInstance("HTML", "HTML", null, doc.getLayers());
	r.setInputStream(getClass().getResourceAsStream("/sys/Splash.html"));
	try { r.parse(doc); r.closeInputStream(); } catch (Exception e) {}
	curdoc_ = doc;
	//update(getGraphics());
	repaint(0);
		 */
		//DocInfo di = null;
		//try { di = new DocInfo(URLs.toURI(getClass().getResource("/sys/Splash.html"))); } catch (URISyntaxException canthappen) {}
		DocInfo di = new DocInfo(URI.create(""));
		di.doc = doc;
		MediaLoader loader = (MediaLoader)Behavior.getInstance("loader", "multivalent.std.MediaLoader",null, doc.getLayers()/*null/*no Layer, which is unusual*/);
		loader.setDocInfo(di);
		InputUni iu = new InputUniString("Loading...");
		loader.finalLoad(true, di, iu);
		//new LeafUnicode()

		// do time-consuming things while showing splash page
		// ... => way fast on today's machines (1 GHz Pentium)
		//	buildUI(root_);	// UI doc

		//} catch (URISyntaxException canthappen) {}//eventq(Document.MSG_OPEN, getClass().getResource("/sys/Splash.html")); -- works, but adds splash page to f/b list
		//getToolkit().sync();	// needed to see splash

		// 3. behaviors
		Layer rootlayers = getRoot().getLayers();
		Layer sys = rootlayers.getInstance(systemHub);
		sys.setName(Layer.SYSTEM);

		rootlayers.buildBeforeAfter(doc);	// <SHARED> and <SCRATCH> built on demand

		//setCurDocument(doc); -- done in MediaLoader
		enableEvents(	// last: don't let user poke at it during startup
				//0x7fffffff
				//AWTEvent.COMPONENT_EVENT_MASK
				AWTEvent.KEY_EVENT_MASK
				| AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK
				| AWTEvent.ACTION_EVENT_MASK
				//| AWTEvent.PAINT_EVENT_MASK
				| AWTEvent.WINDOW_EVENT_MASK //| AWTEvent.WINDOW_STATE_EVENT_MASK
				//| AWTEvent.HIERARCHY_EVENT_MASK
		);
		//docbox_.show();

		finit_ = true;
	}

	public Dimension getPreferredSize() { return new Dimension(winw_, winh_); }


	public void setBounds(int x,int y, int width,int height) {
		int win=getWidth(), hin=getHeight();
		super.setBounds(x,y, width,height);
		//if (width!=before.width /*|| height!=before.height */) {	// height just paints more => have to elongate vertical scrollbar
		if (width!=win || height!=hin) {
			//System.out.println("**** WINDOW bounds changed");
			if (offImage_!=null) { offImage_.flush(); offImage_=null; /*offg_.dispose(); offg_=null;*/ }
			Root root = getRoot();
			if (/*width!=win &&*/ root!=null /* can happen during startup(?)*/)
				root.markDirtySubtree(true/*false probably ok, but this is rare so be safe*/);	// reformat when repainted
			//format();
			//eventq(Document.MSG_FORMATTED, root);	// need, but not received at right time?
		}
		//System.out.println("setBounds -> repaint");
		//repaint(1000);	// calling paint() but not update() in 1.4beta3, contrary to documentation
	}

	// Canvas.createBufferStrategy(2, false, true<accelerated>, null); won't wake up after full screen mode, => fixed in Java 1.4.1_01?
	// but would like to use it later
	public void createBufferStrategy(int numBuffers, BufferCapabilities caps) throws AWTException {
		assert numBuffers>=0: numBuffers;

		// clean up old
		if (numBuffers<=1 && numBuffers_>=2 && offImage_!=null) { offImage_.flush(); offImage_=null; }
		//System.out.println("cBS "+numBuffers_+" => "+numBuffers);

		// set new
		numBuffers_ = numBuffers;
		// ignore caps
	}


	/** Return logical internal name of browser window instance. */
	public String getName() { return name_; }

	/**
	Returns handle to offscreen {@link java.awt.Image} that holds the image of the Browser content.
	This can be used for special effects:
	BitMagnify redraws the portion of the page at a different scale.
	Interactive drawing tools could quickly re-establish the background as the user moves some object or adjusts a line
	(draw on {@link java.awt.Image#getGraphics()}).
	 */
	public Image getOffImage() {
		if (offImage_==null)
			//offImage_ = createImage(getWidth(), getHeight()); /*offg_=(Graphics2D)offImage_.getGraphics();*/	// creates BufferedImage TYPE_INT_ARGB_PRE under OS X 10.2 / JDK 1.4.1
			//System.out.println("old offscreen image = "+offImage_/*+", "+bi.getRaster().getDataBuffer()*/);
			//offImage_ = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());	// creates IntegerNIORaster of custom type under OS 10.2 / JDK 1.4.1 -- can be very slow on some machines/display settings
			offImage_ = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);	// optimized rendering loops apparently, though possible conversion from image to screen
		/*GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment(); -- no difference
		GraphicsDevice dev = env.getDefaultScreenDevice();
		GraphicsConfiguration con = dev.getDefaultConfiguration();
		offImage_ = con.createCompatibleImage(getWidth(), getHeight());*/
		// X HACK: don't put in VRAM! => no difference
		//if (offImage_ instanceof BufferedImage) ((BufferedImage)offImage_).getRaster().getDataBuffer();
		//BufferedImage bi = new BufferedImage(getWidth(),getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		//bi.getRaster().getDataBuffer();
		//offImage_ = bi;
		//System.out.println("created offscreen image = "+offImage_/*+", "+bi.getRaster().getDataBuffer()*/);
		return offImage_;
	}	// as for drawing scribbles fast

	/** Returns master {@link Root}, which contains both GUI and perhaps multiple content subdocument. */
	public Root getRoot() { return root_; }

	public INode getDocRoot() { return docroot_; }

	/** Returns handle to the {@link multivalent.std.span.SelectionSpan selection span}.  Check {@link Span#isSet()} to see if presently set or not. */
	public /*Selection*/Span getSelectionSpan() {
		// pluck out of System layer so user can change/eliminate
		if (sel_==null) sel_ = (Span)Behavior.getInstance("selection", "SelectionSpan", null, getRoot().getLayer(Layer.SYSTEM));	// subject to remapping
		return sel_;
	}

	/** Returns handle to {@link CursorMark cursor}. */
	public CursorMark getCursorMark() {
		if (cur_==null) cur_ = (CursorMark)Behavior.getInstance("cursor", "CursorMark", null, getRoot().getLayer(Layer.SYSTEM));
		return cur_;
	}


	/** Interactive editing should be done only within subtree rooted here.  So, if null, no editing. */
	public INode getScope() { return scope_; }
	public void setScope(INode scope) { scope_=scope; }


	/** Returns node under the cursor. */
	public Node getCurNode() { return curnode_; }
	/** Returns offset within node under the cursor. */
	public int getCurOffset() { return curoff_; }

	public void setCurNode(Mark m) { if (m==null) setCurNode(null,-1); else setCurNode(m.leaf, m.offset); }

	/** The current node under the cursor, either a Leaf or its corresponding IScrollPane. */
	public void setCurNode(Node newnode, int newoffset) {
		//	Node onode = curmark.leaf; int ooffset = curmark.offset;
		curnode_ = newnode; curoff_ = newoffset;
		//	if (onode==newnode && ooffset==newoffset) return;
		//if (newnode!=null) System.out.println("cur node="+newnode+" / "+newoffset+" "+newnode.bbox);

		//	if (getGrab()!=null) return;// vrangeActive_;

		// can be more efficient if caller passes actives, as from leaf
		//vrangeActive_ = (newnode!=null? newnode.getActivesAt(newoffset,false): null);
		//System.out.println("newnode="+newnode+", oldact="+ovrangeActive+", newact="+vrangeActive_);
		//System.out.println("grab = "+(getGrab()==null?"null":getGrab().getClass().getName()));
		//System.out.println(vrangeActive_);
		//	if (getGrab()!=null) return;// vrangeActive_;

		// synthesize span enter/exit (span only--structural accomodated in tree walk)
		// Enter and Leave special case: range different than document as a whole
		// would like to use tickleActives, but don't want to send EXIT and ENTER to same Behavior in same round
		// on the other hand, Notemark shortcircuits underlying hyperlink's ENTER so user doesn't think hyperlink active
		// on the other hand, want to directly click in man page volume list

		// as well as the event => handled in Leaf now => back so can fake between leaf nodes
		//	for (int i=0,imax=(vrangeActive_!=null?vrangeActive_.size():0); /*!shortcircuit &&*/ i<imax; i++) {
		//		EventListener rg = vrangeActive_.get(i);
		//		shortcircuit = rg.event/*Before*/(/*original event*/);
		//	}
	}

	/** Returns coordinates of mouse cursor, relative to top-level Frame. */
	public Point getCurScrn() { return new Point(curscrn_); }


	/**
	Current Document on which to attach annotations.
	While an Document's document tree is being built, it is not the current Document.
	 */
	public Document getCurDocument() { return curdoc_; }

	/**
	Switching from one completely formed independent document tree (Document) to another.
	Make this an event (instead of or in addition), so other things can hook in.
	 */
	public void setCurDocument(Document doc) {
		//if (/*doc!=null &&*/ curdoc_!=doc) {	-- could be reloading
		curdoc_=doc;
		if (doc==null) return;

		//	docbox_.setTitle(docURI.toString());	// HTML replaces with <TITLE>
		String title = doc.getAttr(Document.ATTR_TITLE, "(no title)");
		if (docbox_!=null) docbox_.setTitle(title+" - Multivalent");

		if (doc!=docroot_) {
			event(new SemanticEvent(this, Document.MSG_CURRENT, doc, null,null));
			eventq(new MouseEvent(this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, curscrn_.x, curscrn_.y, 0, false));
		}

		getRoot().markDirty();	// need for splash page to show up
		requestFocus();

		Multivalent.getLogger().finer(doc.toString());
		//System.out.println("repaint");
		repaint(500);	// doc draws border to show focus -- br.repaint undraws old, draw new, and either many be null
		//}
	}



	/** @deprecated in favor of {@link #MSG_STATUS}. */
	public final void showStatusX(String label) {	// => behavior + semantic message
		//if (infobar!=null) infobar.setText(label);
		if (infobar!=null) infobar.setText(label!=null && label.length()>0? label: " ");	// JLabel disappears if text is ""
	}




	/**
	LATER: take structure and style from an HTML/XML document rather than hardcoding.
	 */
	void buildUI2(Root root) {
		assert root!=null;

		URI uri;
		try { uri = new URI(Multivalent.getInstance().getPreference("GUIHTML", "systemresource:/sys/GUI.html")); } catch (URISyntaxException urie) { uri = URI.create("systemresource:/sys/GUI.html"); }

		DocInfo di = new DocInfo(uri);
		di.doc = root;
		MediaLoader loader = (MediaLoader)Behavior.getInstance("loader","multivalent.std.MediaLoader",null, root.getLayers());
		loader.setDocInfo(di);
		loader.load(true);


		/* RESTORE
	Map anchors = (Map)root.getVar(Document.VAR_ANCHORS);	// id's when ASCII
	docroot_ = (INode)anchors.get("content");
	docroot_.removeAllChildren();
		 */

		INode body = (INode)root.findBFS("body");
		System.out.println("body = "+body+", class="+body.getClass().getName());
		root.removeAllChildren();	// not root.clear()
		root.appendChild(body);
	}

	void buildUI(Root root) {
		assert root!=null;
		long timein = System.currentTimeMillis();

		// buildBefore pass iterates highest priority to lowest: lower build on base
		// need this newroot contortion or else get update() call in middle of parse(), sometimes
		// how to flush repaint queue?	Toolkit.getDefaultToolkit().sync() doesn't do it.

		// X root doc is going to come from HTML doc, so while building, use substitute root
		//Map anchors = (Map)root.getVar(Document.VAR_ANCHORS);	// id's when ASCII

		CLGeneral cx;

		StyleSheet ss = root.getStyleSheet();
		Color bg = Color.LIGHT_GRAY;//.brighter();
		//bg = Color.GREEN;

		cx = new CLGeneral();
		cx.setForeground(Color.BLACK);	// later get these from some preferences file
		//cx.setBackground(Color.WHITE);	// background of entire page
		cx.setBackground(bg);	// background of entire page
		//cx.setMarginTop(5); cx.setMarginBottom(5); cx.setMarginLeft(5); cx.setMarginRight(5); -- docs
		cx.setFont(NFont.getInstance("Dialog", NFont.WEIGHT_BOLD, NFont.FLAG_NONE, 12f));
		ss.put("root", cx);

		/*
	cx = new CLGeneral();
	cx.setMargins(2);
	//cx.setMarginLeft(5); cx.setMarginRight(0); cx.setMarginTop(2); cx.setMarginBottom(2);
	cx.setBackground(Color.WHITE);
	ss.put("iroot", cx);	// no more IROOTs anymore
		 */

		//Document uidoc_ = new Document(null,null, root);	// need visual layer and style sheet
		//uidoc_.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
		// styles for GUI widgets -- should read this from file
		// => move into a behavior + text configuration file
		cx = new CLGeneral();
		//cx.setMargins(3);
		cx.marginleft = cx.marginright = 3;
		cx.setPadding(5);
		//cx.setBackground(bg); cx.setForeground(Color.BLACK);
		ss.put("button", cx);
		ss.put("menubutton", cx);
		ss.put("checkbox", cx);

		cx = new CLGeneral();
		cx.setPadding(3);
		//cx.paddingtop = cx.paddingbottom = 3;
		//cx.paddingleft = 11+5; cx.paddingright=5;
		cx.setBorder(1);
		ss.put("menu", cx);

		cx = new CLGeneral();
		//	cx.setMargins(3);
		cx.paddingleft = cx.paddingright = 3;
		//cx.setPadding(3);
		//cx.setBackground(bg); cx.setForeground(Color.BLACK);
		ss.put("entry", cx);

		cx = new CLGeneral();
		cx.setPadding(25); cx.setBorder(3);
		cx.setForeground(Color.BLACK); cx.setBackground(Color.RED.brighter());

		ss.put("alert", cx);

		/*	cx = new CLGeneral();
	cx.setpaddingleft(11); cx.setpaddingright(5);
	//ss.put("button", cx);
	ss.put("checkbox", cx);
	ss.put("radiobox", cx);*/

		/*	cx = new CLGeneral();
	//cx.setMargins(5); -- should fix this
	//cx.setBackground(Color.LIGHTGRAY); cx.setForeground(Color.BLACK);
	ss.put("menubar", cx);*/

		cx = new CLGeneral();
		cx.paddingleft = 11+5; cx.paddingright = 5 +5/*cascade arrow*/;
		//cx.setBackground(bg); cx.setForeground(Color.BLACK);
		ss.put("menuitem", cx);

		/*	cx = new CLGeneral();
	cx.setMarginLeft(5); cx.setMarginRight(5);
	cx.setMarginTop(2); cx.setMarginBottom(2);
	//cx.setBackground(bg); cx.setForeground(Color.BLACK);*/

		// must move to HTML soon
		cx = new CLGeneral();
		cx.weight_ = NFont.WEIGHT_BOLD;
		ss.put("b", cx);
		cx = new CLGeneral();
		cx.flags_ = NFont.FLAG_ITALIC;
		ss.put("i", cx);
		cx = new CLGeneral();
		cx.underline_ = Context.COLOR_INHERIT;
		ss.put("u", cx);
		cx = new CLGeneral();
		cx.overstrike_ = Context.COLOR_INHERIT;
		ss.put("strike", cx);

		INode uidoc = new IVBox("_uiroot",null, root);

		INode menubar = new IParaBox("_menubar",null, uidoc); //anchors.put("menubar", menubar);
		menubar.putAttr("id", "menubar");

		// MOVE TO A BEHAVIOR!
		INode toolbar = new IParaBox("_toolbar",null, uidoc); //anchors.put("toolbar", toolbar);
		toolbar.putAttr("id", "toolbar");
		INode toolbar2 = new IParaBox("_toolbar2",null, uidoc); //anchors.put("toolbar2", toolbar2);
		toolbar2.putAttr("id", "toolbar2");
		//callSemanticEvent(new SemanticEvent(this, MSG_CREATE_TOOLBAR, null, null,toolbar));
		//eventq(new SemanticEvent(this, MSG_CREATE_TOOLBAR, null, null,toolbar));

		//IParaBox status = new IParaBox("status",null, uidoc); anchors.put("status", status);
		//new LeafUnicode("", null, status);	// always take vertical space

		//INode +toolbar = new IParaBox("_NAVBAR",null, uidoc);
		//Root uidoc = new Root(null/*attr_*/, this);
		// Should _DOCROOT be Document?  No, media adaptors have to provide own Documents anyhow since they may be nested.
		docroot_ = new IVBox("_docroot",null, uidoc);	// later findBFS for "_docroot"
		docroot_.putAttr("id", docroot_);

		//root_ = newroot;
		//new INode("CONTENT",null, root_);
		if (Browser.DEBUG) System.out.println("== BUILD ==");

		//new LeafUnicode("THIS SPACE INTENTIONALLY LEFT BLANK",null, uidoc); => wiped out by IVBox
		//} else { -- still keep System behaviors so can load other documents

		//root_ = root;

		//	uidoc.markDirtySubtree(true);	//=>doesn't seem to help	// can get premature paints, which leave some things erroneously marked valid
		//	format();
		if (Browser.PROF) { Browser.profs[Browser.BUILDCNT]++; Browser.profs[Browser.BUILDTIME] += System.currentTimeMillis()-timein; }
	}



	/**
	FORMAT - geometrically place doc tree elements.
	In depth-first tree traversal, call observing behaviors in each node in priority order.
	Default/lowest priority is implicitly the flow layout.
	Specialized formatters, e.g., CSS,  can intercept and prevent subsequent action.
	@deprecated
	 */
	public void format() {	//-- forced at paint
		long timein = System.currentTimeMillis();
		//	attr_.remove(Fixed.ATTR_REFORMATTED);	// when laying entire page, set "not move" flag (special XDOC hack)
		if (Browser.DEBUG) System.out.println("format all");

		int width=getWidth(), height=getHeight();
		//System.out.println("Browser FORMAT "+width+"X"+height);
		// later format everything on demand
		//Context cx = new Context(); //cx.styleSheet = getRoot().getStyleSheet();//cx.styleSheet = styleSheet;
		// LATER load up with style sheet for that view
		if (Browser.DEBUG) System.out.println("format:	"+width+"x"+height+"  root_.valid_="+root_.isValid());
		getRoot().formatBeforeAfter(width,height, null);
		//	  if (DEBUG) root_.validate(PROTOCOL_FORMAT);	// verify integrity of tree (frequently!)
		if (Browser.DEBUG) System.out.println(root_);
		if (Browser.PROF) { Browser.profs[Browser.FORMATCNT]++; Browser.profs[Browser.FORMATTIME] += System.currentTimeMillis()-timein; }
	}


	/*
	 * PAINT
	 */
	/*
  private static long time0 = System.currentTimeMillis();
  public void repaint(long ms, int x, int y, int w, int h) {
	System.out.println("Browser REpaint "+ms+"  "+x+","+y+"  "+w+"x"+h+"  @ "+(System.currentTimeMillis()-time0));
	super.repaint(Math.max(1, ms), x, y, w, h);
  }*/

	/*	// translate global coordinates into screen for clipping region
	// repaint, repair addressed in screen coordinates
	// don't override repaint.	other things like window resizing count on standard implementations
  private long redeadline_=0L; private int rex_=Integer.MAX_VALUE, rey_=Integer.MAX_VALUE, rew_=-1, reh_=-1;
  public void repaint(long ms, int x, int y, int w, int h) {
	if (x>getWidth() || y>getHeight() || x+w<0 || y+h<0) return;
	//System.out.print("Browser REpaint "+ms+"  "+x+","+y+"  "+w+"x"+h+"  @ "+(System.currentTimeMillis()-time0));

	// apparently Swing is not clustering repaints, which we rely on, so we cluster
	if (rex_ <= x && rex_+rew_ >= x+w) {} else if (rex_ <= x) rew_ = x+w - rex_; else { rex_=x; rew_=w; }
	if (rey_ <= y && rey_+reh_ >= y+h) {} else if (rey_ <= y) reh_ = y+h - rey_; else { rey_=y; reh_=h; }

	long now = System.currentTimeMillis();
	if (ms==0L || now >= redeadline_) {
		super.repaint(0L, rex_, rey_, rew_, reh_);
		redeadline_=Long.MAX_VALUE; rex_=Integer.MAX_VALUE; rey_=Integer.MAX_VALUE; rew_=-1; reh_=-1;
//System.out.println();
	} else {if (now+ms < redeadline_) redeadline_ = now+ms;
//System.out.println(" -- cluster");
	}
  }

	public void repaint(int x, int y, int w, int h) { repaint(0, x,y, w,h); }
	public void repaint() { repaint(0); }
	public void repaint(long ms) { repaint(0, 0,0, size().width,size().height); }
	 */
	public void repaint(long ms, int x, int y, int w, int h) {
		//getLogger().finer( System.out.println("browser repaint "+ms+" @ ("+x+","+y+"), "+w+"x"+h);
		//if (/*ms==0L &&*/ w>0) new Exception().printStackTrace();
		if (w>0) super.repaint(ms, x,y, w, h);
	}



	/*
  Color[] showBboxColor = { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.CYAN, Color.BLACK };
  int showBboxCnt = 0;
	 */


	public void paintComponent(Graphics g_old_api) {	// Swing already double-buffered
		//public void paint(Graphics g_old_api) {
		if (root_ == null) return;	// can happen during startup

		Graphics2D g = (Graphics2D)g_old_api;
		Rectangle gclip = g.getClipBounds();

		// X coalese repaint requests.  Java is supposed to do this, but Swing doesn't.  (Check in Java 5.0.) => coalesing not happening, but not the bottleneck
		//System.out.print("Browser.paint "+phelps.awt.geom.Rectangles2D.pretty(gclip));
		//EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
		//System.out.println("   peek "+q.peekEvent()+" / "+q.peekEvent(PaintEvent.UPDATE));

		//g.setClip(0,0,getWidth(),getHeight());	// can we get away with no clipping beyond current screen (simplifies Magnify lens, just as no bitblt scrolling simplified pinned lenses)


		long timein = System.currentTimeMillis();
		//System.out.println(timein+" "+gclip+", peek = "+getToolkit().getSystemEventQueue().peekEvent());
		if (Browser.PROF && g.getClipBounds().width==getWidth() && g.getClipBounds().height==getHeight()) Browser.profs[Browser.PAINTALL]++;


		// if (DIRECT_PAINT) ...

		//BufferedImage bi = new BufferedImage(getWidth(),getHeight(), BufferedImage.TYPE_INT_RGB);
		//bi.getRaster().getDataBuffer();
		//offImage_ = bi;
		//Graphics2D goff = (Graphics2D)bi.getGraphics();
		Graphics2D goff = (Graphics2D)getOffImage().getGraphics();
		//getOffImage(); Graphics2D goff = offg_;
		//RenderingHints oldhints = goff.getRenderingHints(); => create and dispose Graphics2D
		// => hints set in root?  no, client can control
		goff.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		goff.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		goff.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		goff.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// later pass along CTM as well as clipping region (both should be part of graphics context)
		goff.clip(gclip);

		//System.out.println(" paint() "+phelps.text.Rectangles2D.pretty(g.getClipBounds()));
		Context cx = root_.getStyleSheet().getContext(goff, null);
		long start = System.currentTimeMillis();
		root_.paintBeforeAfter(gclip, cx);
		long end = System.currentTimeMillis();
		//System.out.print("paintBeforeAfter "+(end-start));
		//System.out.println("  in "+(end - timein)+" "+root_);

		g.drawImage(offImage_, 0,0, this);
		//int x1=gclip.x, y1=gclip.y, x2=x1+gclip.width,  y2=y1+gclip.height;
		//g.drawImage(offImage_, x1,y1,x2,y2, x1,y1,x2,y2, Color.WHITE, this);
		goff.dispose();
		//offg_.setClip(0,0, offImage_.getWidth(this), offImage_.getHeight(this));

		long timeout = System.currentTimeMillis();
		if (Browser.PROF) { Browser.profs[Browser.PAINTCNT]++; Browser.profs[Browser.PAINTTIME] += timeout-timein; }
		Multivalent.getLogger().finer(gclip+", "+(end-start)/*+" / "+(timeout-timein)*/+" ms, @ +"+(timeout-start_)/*+", peek="+Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent()*/);
	}



	/*
	 * Events
	 *
	 * priorities:
	 * {0) filters (which are lenses that apply to entire screen -- different case for both better UI and more efficient implementation)
	 * (1) Spans as ad hoc overrides, which can shortcircuit...
	 * (2) structural (and on up tree?)
	 */
	// behaviors traverse tree as needed

	// maybe move this to Root
	/**
	Used by Leaf (which knows rel) and IHBox and IParaBox.
	 */
	public boolean tickleActives(AWTEvent e, Point rel, Node obsn) {
		//boolean shortcircuit = false;
		//for (int i=0,imax=(vrangeActive_!=null? vrangeActive_.size(): 0); !shortcircuit && i<imax; i++) {
		//System.out.println("vrangeActive_ = "+vrangeActive_);
		if (vrangeActive_!=null) {
			int imax = vrangeActive_.size(), i=imax-1;
			//System.out.println("\nEVENT "+e);
			for (; i>=0; i--) if (((Behavior)vrangeActive_.get(i)).eventBefore(e, rel, obsn)) break;	// high to low
			//System.out.println("before "+vrangeActive_.get(i));}
			if (i<0) i=0;
			for ( ; i<imax; i++) if (((Behavior)vrangeActive_.get(i)).eventAfter(e, rel, obsn)) return true;	// low to high
			//System.out.println("after "+vrangeActive_.get(i));}
		}
		return false;
	}

	/* * @return ordered list of active ContextListeners at mouse cursor (vs insertion cursor).
  public List<ContextListener> getActives() {
	return vrangeActive_;	// pass back to leave to pass event to spans (with possible short-circuit)
  }*/

	/**
	Treat mutation (filtering, augmenting, replacing) during semantic event pass as a method.
	<b>Usually semantic events are fired via eventq().</b>
	@return the mutated clientData.
	 */
	public Object callSemanticEvent(String msg, Object arg) {
		SemanticEvent se = new SemanticEvent(this, msg, arg);
		event(se);
		return se.getArg();
	}

	/*
  public Object callSemanticEvent(String msg, Object clientData) { return callSemanticEvent(new SemanticEvent(this, msg, clientData)); }
  public Object callSemanticEvent(SemanticEvent se) { event(se); return se.getArg(); }*/
	/** A convenience function for behaviors to create a semantic event and put on event queue.
  This is the usual way semantic events are invoked, as opposed to callSemanticEvent.
  public void fireSemanticEvent(String msg, Object clientData) { fireSemanticEvent(new SemanticEvent(this, msg, clientData)); }
  //public void fireSemanticEvent(SemanticEvent se) { eventq(se); }*/

	/**
	Hook into Java's event system.  <!-- No <code>process&lt;event&nbsp;type&gt;Event</code>. -->
	Event propagation within Multivalent is unrelated to however Java does it.
	 */
	protected void processEvent(AWTEvent e) {
		//System.out.println("event = "+e.getID());
		event(e);
	}

	/**
	JDK1.3 doesn't make union retangles on PAINT or UPDATE,
	coalescing only if on rectangle completely encloses the other.
	Doesn't work: seems you have to let the peer handle such coalescing.
	 */
	/*
  protected AWTEvent coalesceEventsX(AWTEvent existingEvent, AWTEvent newEvent) {
	switch (existingEvent.getID()) {
	case PaintEvent.PAINT:
	case PaintEvent.UPDATE:
		PaintEvent existingPaintEvent=(PaintEvent)existingEvent, newPaintEvent=(PaintEvent)newEvent;
		Rectangle existingRect=existingPaintEvent.getUpdateRect(), newRect=newPaintEvent.getUpdateRect();
		if (existingRect.intersects(newRect)) {
			//existingPaintEvent.setUpdateRect(existingRect.union(newRect));
//System.out.println(existingRect+" union with "+newRect+" => returning "+existingPaintEvent);
			//return existingPaintEvent;
			newPaintEvent.setUpdateRect(newRect.union(existingRect));
System.out.println(existingRect+" union with "+newRect+" => returning "+newPaintEvent);
			return newPaintEvent;
		}
	}
	return super.coalesceEvents(existingEvent, newEvent);
  }*/

	/**
	There are two types of events: low-level events and semantic events.
	Both are handled here, though only low-level events are described here.

	May want to allow nesting only for SemanticEvent's, as that's all we're set up to withstand now.

	@see multivalent.SemanticEvent
	 */
	public void eventq(AWTEvent e) {
		//e.setSource(this);	// java.util.EventObject doesn't allow counstruction with null source
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
	}

	/**
	A convenience function for behaviors to create a semantic event and put on event queue.
	Use this in preferences to {@link #event(AWTEvent)} whenever possible!
	 */
	public void eventq(String message, Object arg) {
		assert message!=null;
		//System.out.println(msg+" / "+clientData);
		eventq(new SemanticEvent(this, message, arg));
	}
	//public void event(String msg, Object clientData) { event(new SemanticEvent(this, msg, clientData)); }

	//	protected int eventCnt_=0;
	/**
	Process event.
	Most events should be added to the event queue with <code>eventq()</code>, instead on demanding instant attention.
	<!-- Some events, however, must be completely executed -->
	 */
	public boolean event(AWTEvent e) {
		//if (!finit_) System.out.println("throwing away "+e);
		if (!finit_) return false;
		long timein = System.currentTimeMillis();
		if (Browser.PROF && e.getID()==MouseEvent.MOUSE_MOVED) Browser.profs[Browser.EVENTMOVE]++;


		int eid = e.getID();
		//	if (eid==MouseEvent.MOUSE_ENTERED) { requestFocus(); return true; } -- interferes with clipboard
		//	if (SemanticEvent.SEMANTIC_FIRST<=eid && eid<=SemanticEvent.SEMANTIC_LAST) {//eventCnt_++;
		//System.out.println("event = "+eid);   //+", arg = "+((SemanticEvent)e).getArg());}


		boolean shortcircuit=false;	// once someone eats the event, that's it
		//Multivalent.getLogger().finest(e.toString());

		// semantic events go round robin through layers
		if (SemanticEvent.SEMANTIC_FIRST <= eid&&eid <= SemanticEvent.SEMANTIC_LAST) {
			SemanticEvent se = (SemanticEvent)e;
			String msg = se.getMessage();
			//System.out.println("Browser semantic event "+" "+msg+"   "+se.getArg()+", grab="+grabOwner_);
			Multivalent.getLogger().fine("'"+msg+"' "+se.getArg());
			//if (Document.MSG_FORMATTED==msg) Multivalent.getLogger().fine(""+getCurDocument().getURI());

			if (grabOwner_ instanceof Behavior) {
				//System.out.println(", grab by "+((Behavior)grabOwner_).getName());
				Behavior be = (Behavior)grabOwner_;
				shortcircuit = be.semanticEventBefore(se,msg) |/*not second '|'*/ be.semanticEventAfter(se,msg);	// always BOTH before and after -- don't use "||"!

			} else {
				Document curdoc = getCurDocument();
				//System.out.println(", round robin (curdoc="+curdoc+"/"+(curdoc!=null?curdoc.getFirstLeaf():null));
				//System.out.println(msg+" round robin, arg="+se.getArg());
				Layer global=getRoot().getLayers(), active = curdoc!=null && curdoc!=getRoot()? curdoc.getLayers(): null;
				// Don't get ideas about momentarily add curdoc's layers to globals, as nested events would add cur doc multiple times

				boolean iss = global.semanticEventBefore(se,msg);
				boolean jss=false;
				if (!iss && active!=null) {
					active.semanticEventBefore(se,msg);
					jss = active.semanticEventAfter(se,msg);
				}
				//if (!jss) -- always close up higher ones...?	if not, still need to clean up nesting counter
				iss = global.semanticEventAfter(se, (jss? null: msg));
				shortcircuit = iss || jss;

				// Semantic events sent to current document tree also, bypassing grab.
				// Point==null to indicate semantic (could also check id).
				// If efficient enough to send even to all behaviors, efficient enough to
				// Used by STOP document.  This way can add new events without chaining to id enum, which will lead to conflicts if done independently.
				//if (curdoc!=null) curdoc.eventBeforeAfter(se, null); => done above
			}
			//System.out.println("<= semantic event: "+msg);


		} else if (WindowEvent.WINDOW_FIRST<=eid && eid<=WindowEvent.WINDOW_LAST) {
			//|| ComponentEvent.COMPONENT_FIRST<=eid && eid<=ComponentEvent.COMPONENT_LAST) {
			//System.out.println("got window event "+eid);
			if (WindowEvent.WINDOW_CLOSING==eid) destroy();
			//else if (WindowEvent.WINDOW_ICONIFIED==eid) { offImage_.flush(); offImage_=null; System.out.println("iconify"); }
			//else if (WindowEvent.WINDOW_DEICONIFIED==eid) { System.out.println("deiconify"); }

			/*	} else if (eid==ComponentEvent.COMPONENT_RESIZED) {
System.out.println("resize");
		offImage_ = null;
		repaint(10);*/

		} else if (MouseEvent.MOUSE_MOVED==eid && getScope()!=null && getCursorMark().isSet() /*|| getSelectionSpan().isSet()*/) {
			// ignore -- click-to-focus special case

		} else {	// low-level events pass through tree (even if grab is set)
			MouseEvent me = null;
			// curscrn_ related to (last/current) mouse cursor position regardless of current type of event
			// always compute current (node,offset) and active behaviors
			if (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST) {
				// coalese moved/dragged events.	Not sure if this is good -- q.peekEvent() apparently doesn't queue events from environment
				/*EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
			System.out.println("pending = "+q.peekEvent(PaintEvent.PAINT)+" / "+q.peekEvent(PaintEvent.UPDATE));
			if (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED) {
				try {
					System.out.println("q.peekEvent() = "+q.peekEvent());
					while (q.peekEvent()!=null && q.peekEvent().getID()==eid) {
						e=q.getNextEvent(); System.out.println("coalesed"); }
				} catch (InterruptedException ie) {}
			}*/
				me = (MouseEvent)e;
				//curscrn_.setLocation(Math.max(me.getX(),0), Math.max(me.getY(),0));
				curscrn_.setLocation(me.getX(), me.getY());
				//System.out.println("setting curscrn "+getCurScrn());
			}


			//setCurNode(null,-1);	// assume none, and *Leaf* sets if it can
			if (grabOwner_!=null) {
				// special case: mouse events still find current leaf => NO, behavior with grab can invoke if it needs it
				//if (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST) root_.eventBeforeAfter(new TreeEvent(br, TreeEvent.TREE_FIRST));
				//System.out.println("sending "+getCurScrn()+" to "+grabOwner_);
				//grabOwner_.eventBefore(e/*, getCurScrn()*/); grabOwner_.eventAfter(e);
				if (me!=null) me.translatePoint(-gdx_, -gdy_);
				grabOwner_.event(e);
				if (me!=null) me.translatePoint(gdx_, gdy_);

			} else {
				// root_ is null while building tree, so can keep old document until replaced by new.
				// but with progressive rendering, need to loosen up a bit so can scroll
				Point pt = getCurScrn();
				//boolean cool = e.getID()==MouseEvent.MOUSE_PRESSED;
				//if (cool) System.out.println("ss = "+shortcircuit);
				Node onode=getCurNode(); int ooffset=getCurOffset();
				//if (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED || eid==TreeEvent.FIND_NODE) setCurNode(null);
				if (me!=null || eid==TreeEvent.FIND_NODE) setCurNode(null);

				if (root_!=null) shortcircuit = getRoot().eventBeforeAfter(e, pt);

				Node newnode=getCurNode(); int newoffset=getCurOffset();	// can be changed by keyboard events too, e.g., tab to move focus
				if (onode!=newnode) {

					// do nodes first so spans can override
					//System.out.println("setCurNode("+newnode+","+newoffset+")");
					Node top = onode!=null && newnode!=null? onode.commonAncestor(newnode): null;
					//if (onode!=null && onode.isLeaf()) onode.eventBeforeAfter/*Before*/(MOUSE_EXIT, POINT_BOGUS);
					//if (newnode!=null && newnode.isLeaf()) newnode.eventBeforeAfter/*Before*/(MOUSE_ENTER, POINT_BOGUS);
					// have to march through subtree since passing POINT_BOGUS
					//System.out.println("top = "+top+", out of "+onode+", into "+newnode);
					if (onode!=null /*&& ooffset>=0*/) for (Node n=onode; n!=top; n=n.getParentNode()) n.eventBeforeAfter(MOUSE_EXIT, POINT_BOGUS);
					if (newnode!=null /*&& newoffset>=0/*can be between*/) for (Node n=newnode; n!=top; n=n.getParentNode()) n.eventBeforeAfter(MOUSE_ENTER, POINT_BOGUS);
				}


				if (onode!=newnode || ooffset!=newoffset) {
					// if different mark, which almost always happens
					List<ContextListener> ovrangeActive = vrangeActive_;
					vrangeActive_ = newnode!=null? newnode.getActivesAt(newoffset,true): null;

					// first Leave old ones
					//boolean shortcircuit=false;
					if (ovrangeActive!=null) for (int i=ovrangeActive.size()-1; /*!shortcircuit && */ i>=0; i--) {
						Behavior/*EventListener*/ rg = (Behavior)/*(EventListener)*/ovrangeActive.get(i);
						if (vrangeActive_==null || vrangeActive_.indexOf(rg)==-1 /*&& rg.isSet()*/) {	//shortcircuit =
							rg.eventBefore(MOUSE_EXIT, POINT_BOGUS, onode); rg.eventAfter(MOUSE_EXIT, POINT_BOGUS, onode);
						}
						//System.out.println("exiting "+rg+", vrangeActive_="+vrangeActive_); }
					}

					// then Enter new ones
					if (vrangeActive_!=null) for (int i=0,imax=vrangeActive_.size(); /*!shortcircuit &&*/ i<imax; i++) {
						//System.out.println("i="+i+", imax="+imax+", size="+vrangeActive_.size()+", element = "+vrangeActive_.get(i));
						Behavior/*EventListener*/ rg = (Behavior)vrangeActive_.get(i);
						if (ovrangeActive==null || ovrangeActive.indexOf(rg)==-1) {//shortcircuit =
							rg.eventBefore(MOUSE_ENTER, POINT_BOGUS, newnode); rg.eventAfter(MOUSE_ENTER, POINT_BOGUS, newnode);
						}
						//System.out.println("entering "+rg+", ovrangeActive="+ovrangeActive); }
					}
				}


				//if (cool) System.out.println("ss = "+shortcircuit);

				// moved here from Leaf so can handle active spans between leaf nodes -- spans logically at leaves, and if here can get tangled with observers on root
				/*	        vrangeActive_ = getActives();
if (cool) System.out.println("actives "+vrangeActive_+", ss="+shortcircuit);
			for (int i=0,imax=(vrangeActive_!=null? vrangeActive_.size(): 0); !shortcircuit && i<imax; i++) {
				EventListener rg = vrangeActive_.get(i);
if (cool) System.out.println("click on "+rg);
				shortcircuit = rg.event(e, pt);
			}*/
			}
		}

		//	if (SemanticEvent.SEMANTIC_FIRST<=eid && eid<=SemanticEvent.SEMANTIC_LAST) eventCnt_--;
		if (Browser.PROF) { Browser.profs[Browser.EVENTCNT]++; Browser.profs[Browser.EVENTTIME] += System.currentTimeMillis()-timein; }
		return shortcircuit;
	}


	/** Returns owner of grab; <code>null</code> if none. */
	public EventListener getGrab() { return grabOwner_; }

	/**
	Delivers subsequent events directly to <varowner</var>, in absolute coordinates.
	Won't set grab if it already has an owner.
	 */
	public void setGrab(EventListener owner) { setGrab(owner, null); }

	/**
	Delivers subsequent events directly to <var>owner</var>, in coordinates relative to <var>relativeTo</var>.
	Won't set grab if it already has an owner.
	 */
	public void setGrab(EventListener owner, Node relativeTo) {
		//assert owner!=null && relativeTo!=null;

		if (grabOwner_!=null) return;	// or could relieve previous owner of grab
		grabOwner_ = owner;
		gdx_ = gdy_ = 0;
		if (relativeTo!=null) {
			Point pt = relativeTo.getAbsLocation();
			gdx_=pt.x; gdy_=pt.y;
		}
	}

	/**
	Release grab, assuming <var>owner</var> currently has grab.
	For unusual circumstances, it's possible to steal the grab by sending {@link #getGrab()}.
	 */
	public void releaseGrab(EventListener owner) {
		//if (owner == grabOwner_)	// can fake out sending getGrab()
		grabOwner_ = null;
		gdx_ = gdy_ = 0;
	}



	/*
	 * CLIPBOARD - construct selected text by side-effecting passed StringBuffer
	 * (easy to insert anywhere in StringBuffer)
	 */
	/*
  Node[] selchunks = null;	// spanChunky regions for current selection
  // should cache endpoints so can see if need to recompute
  public Node[] getSelchunks() {
	//if (selchunks==null && sel.isSet()) selchunks = Node.spanLeaves(sel.getStart(), sel.getEnd());
	if (selchunks==null && sel.isSet()) selchunks = Node.spanChunky(sel.getStart(), sel.getEnd());
	return selchunks;
  }
	 */

	// in Java 1.1, compute selection on demand
	public synchronized String clipboard() { return clipboard(getSelectionSpan()); }

	// => StandardEdit.copy()
	public synchronized String clipboard(Span span) {
		if (span==null || !span.isSet()) return null;

		if (Browser.DEBUG) System.out.println("in clipboard");
		long timein = System.currentTimeMillis();

		Mark startm=span.getStart(), endm=span.getEnd();
		Node startn=startm.leaf, endn=endm.leaf;
		int starti=startm.offset, endi=endm.offset;


		INode isp = startn.getDocument();
		StringBuffer sb = new StringBuffer(1000);
		Node list[] = null;

		//if (startn==endn) { ((Leaf)startn).clipboardBeforeAfter(sb, starti,endi); setSelection(sb.substring(0)); return; }

		// select on chunkySpan, plus root
		String starttxt="", endtxt="";
		if (startn==endn) { ((Leaf)startn).clipboardBeforeAfter(sb, starti,endi); starttxt=sb.substring(0); sb.setLength(0); }
		else {
			if (starti>0) { ((Leaf)startn).clipboardBeforeAfter(sb, starti,startn.size()); sb.append(' '); starttxt=sb.substring(0); sb.setLength(0); startn=startn.getNextLeaf(); }
			if (endi<endn.size()) { if (starttxt!="") sb.append(' '); ((Leaf)endn).clipboardBeforeAfter(sb, 0,endi); endtxt=sb.substring(0); sb.setLength(0); endn=endn.getPrevLeaf(); }
			if (Node.cmp(startn,0, endn,0, isp) == -1) list = Node.spanChunky(startn,endn);	//(startn!=endn.getNextLeaf()) {
		}

		boolean rootonly = list!=null && list.length==1 && list[0]==isp;
		int i=0,imax=0;
		// first root before
		Behavior be;
		List<Behavior> obs = isp.getObservers();
		if (!rootonly)
			for (i=0,imax=obs!=null?obs.size():0; i<imax; i++) {
				be = obs.get(i);
				if (be.clipboardBefore(sb, isp)) break;
			}

		// then nodes themselves, if no shortcircuit
		if (i==imax) {
			sb.append(starttxt);
			for (int j=0,jmax=list==null?0:list.length; j<jmax; j++) list[j].clipboardBeforeAfter(sb);
			sb.append(endtxt);
			i--;
		}

		// then root after
		if (!rootonly)
			for (/* start from same behavior you left off */ ; i>=0; i--) {
				be = (Behavior)obs.get(i);
				if (be.clipboardAfter(sb, isp)) break;  //return "";	// true here returns immediately
			}

		//setSelection(sb.substring(0));
		if (Browser.PROF) { Browser.profs[Browser.SELECTCNT]++; Browser.profs[Browser.SELECTTIME] += System.currentTimeMillis()-timein; }
		return sb.substring(0);
	}

	// selection
	/*
  public void selectOwn(Behavior newowner) {
	selectOwner = newowner;
  }
  public void selectRelease() {
	if (selectOwner!=null) selectOwner.selectRelease();
	sel_.remove();
	selectOwner = null;
  }
	 */
	/*
  // maybe Java actually implements this by now
  static final int STRING = 0;
  static final int PLAIN_TEXT = 1;
  protected DataFlavor transferDataFlavors_[] = { DataFlavor.stringFlavor, DataFlavor.plainTextFlavor };
  protected String seltxt_;

  public synchronized DataFlavor[] getTransferDataFlavors() {
System.out.println("getTransferDataFlavors");	// never called--why not?
	return transferDataFlavors_;
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
System.out.println("isDataFlavorSupported");
	for (int i=0; i<transferDataFlavors_.length; i++)
		if (flavor.equals(transferDataFlavors_[i])) return true;
	return false;
  }

  public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
System.out.println("getTransferData");
	  if (flavor.equals(transferDataFlavors_[STRING])) {
		return (Object)seltxt_;
	  } else if (flavor.equals(transferDataFlavors_[PLAIN_TEXT])) {
		return new StringReader(seltxt_);
	  } else {
		throw new UnsupportedFlavorException(flavor);
	  }
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
System.out.println("lostOwnership");
	seltxt_ = "";
	//getSelectionSpan().remove();	-- maintain X conventions, which are better
  }
	 */
	/*
  public void setSelection(String seltxt) {
//System.out.println("setting selection to "+seltxt);
	seltxt_ = seltxt;
//System.out.println("clipboard named "+docbox_.getToolkit().getSystemClipboard().getName());
//System.out.println("seizing clipboard");
	StringSelection javabug = new StringSelection(seltxt);
//	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(this, this);
	Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
	clip.setContents(javabug, javabug);
//Transferable xfer = clip.getContents(this);
//try { System.out.println("clip = |"+xfer.getTransferData(DataFlavor.stringFlavor)+"|"); } catch (Exception e) {}
  }
	 */
	//	public String getSelection() { return seltxt_; }



	/* *************************
	 * FUNCTIONALITY
	 **************************/

	// image
	/*
  public Image getImageX(URL url) {
	return getToolkit().getImage(url);
  }
	 */

	/**
	Kill document window, clean up behaviors.
	Could call with semantic event, but cleaner to separate out this important function.

	Close browser: save and release resources.
	Clears out certain data scructures: SCRATCH layer, Timer's list.
	 */
	/*package-private*/ void destroy() {
		// call save on everybody first!
		// save state, if any, of current, if any => should be done on exit event
		event/*not q, now!*/(new SemanticEvent(this, Document.MSG_CLOSE, null));	// give behaviors a chance to save state

		getRoot().clear();

		//	  showProfile(System.out);

		// save geometry of named windows
		Multivalent v = Multivalent.getInstance();
		if (getName()!=null && docbox_!=null) {
			Rectangle frame = docbox_.getBounds();
			Rectangle content = getBounds();
			v.putPreference(getName()+"-geom", content.width+"x"+content.height+"@"+frame.x+","+frame.y);
		}

		v.removeBrowser(this);
		if (docbox_!=null) docbox_.dispose();

		//Thread.currentThread().stop();
	}


	public boolean checkRep() {
		//assert super.checkRep(); => not a VObject

		assert false;	// does this get invoked?
		assert root_!=null;
		//assert getRoot().checkRep();

		return true;
	}


	public void resetProfile() {
		// zero out everything
		for (int i=0; i<Browser.NUMPROF; i++) Browser.profs[i]=0;
	}

	public void showProfile(StringBuffer sb) {
		// write out profs
		if (!Browser.PROF)
			sb.append("statistics collection turned off\n");
		else {
			sb.append("Restore	 "+Browser.profs[Browser.RESTORECNT]+", "+Browser.profs[Browser.RESTORETIME]+"ms\n");
			sb.append("Save  "+Browser.profs[Browser.SAVECNT]+", "+Browser.profs[Browser.SAVETIME]+"ms\n");
			sb.append("Build	 "+Browser.profs[Browser.BUILDCNT]+", "+Browser.profs[Browser.BUILDTIME]+"ms\n");
			sb.append("Format	 "+Browser.profs[Browser.FORMATCNT]+", "+Browser.profs[Browser.FORMATTIME]+"ms/"+Browser.profs[Browser.FORMATTIME]/Browser.profs[Browser.FORMATCNT]+" per\n");
			sb.append("Paint	 "+Browser.profs[Browser.PAINTCNT]+"/"+Browser.profs[Browser.PAINTALL]+" full, "+Browser.profs[Browser.PAINTTIME]+"ms\n");
			sb.append("  Update  "+Browser.profs[Browser.PAINTCNT]+"/"+Browser.profs[Browser.PAINTALL]+" full, "+Browser.profs[Browser.PAINTTIME]+"ms/"+Browser.profs[Browser.PAINTTIME]/Browser.profs[Browser.PAINTCNT]+" per\n");
			sb.append("Event	 "+Browser.profs[Browser.EVENTCNT]+"/"+Browser.profs[Browser.EVENTMOVE]+" move, "+Browser.profs[Browser.EVENTTIME]+"ms\n");
			sb.append("Select	 "+Browser.profs[Browser.SELECTCNT]+", "+Browser.profs[Browser.SELECTTIME]+"ms\n");

			// traverse tree collection #nodes, #children/node (min,max,average)

			// iterate through behaviors, calling showProfile?
		}
	}
}
