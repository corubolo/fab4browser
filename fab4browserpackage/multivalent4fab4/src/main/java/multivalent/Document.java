package multivalent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multivalent.node.Root;



/**
	An independent document, generally with content subtree, stylesheet,
	{@link java.net.URI URI} (not {@link java.net.URL URL}),
	{@link Layer}s of behaviors that implementing genre- or document-specific functionality,
	and document-wide variables.

	Documents can contain other documents, as for a HTML FRAMESET.
	The topmost containing document is the {@link multivalent.node.Root},
	of which there is exactly one in a {@link Browser}.

	<p><code>Document</code>s contain:
	<ul>
	<li>{@link #getLayers() link} to document-specific behaviors
	<li>state: {@link #getURI() URI}, {@link #getStyleSheet() stylesheet},
		{@link #getVar(Object) global variables} in addition to attributes,
		primary {@link #getMediaAdaptor() media adaptor}

<!--
	<li>children are layers (content structural, absolute visual, on-screen visual, ...), iterates over all for protocols (new SemanticEvent(br, Layer.MSG_LOAD, dodoc
cURI, null, doc)formatting, painting, events, ...)
	<li>root for Robust Location computations, so can have nested documents (as in frames)
	<li>defines (but does not implement) various {@linkplain #MSG_OPEN semantic events}
	<li>{@linkplain #getVar(Object) document-wide vars} - like attributes and preferences, but keys and values of any Object type, aren't saved persistently, aren't wiped between documents.
	<li>take the focus(?)
-->
	</ul>


<!--
	<p>LATER: allow multiple parents for multiple views.


Media adaptor "extends Document"?
 **PRO**
	media adaptor writers don't have to worry about naked adaptor, as with raw images and ASCII, HTML FRAME and IFRAME can let contents scroll themselves
	something for _DOCROOT to be
	don't have non-structural node in tree (just name it null)
	clients that just parse on media adaptors (such as RobustHyperlink) don't have weird initial null-name to discard
		=> create Document in buildBefore, so clients calling parse directly don't get it!
		=> on second thought, easy to strip off one node, and Document useful collection of interesting document features, such as list of hyperlinks in HTML
 **CON**
	observors don't get same coordinate space as content, as with visual layers and scanned page's word wise vs full image
	Locations doesn't include root, but resolve vis-a-vis _DOCROOT
	HTML FRAME wants to control scrollbar policy

 *** DECISION ***
=> NO!
   media adaptors create Document but don't extend
-->

	@version $Revision: 1.8 $ $Date: 2003/06/02 05:04:14 $
 */
public class Document extends IScrollPane {
	/**
	Open new document; if this is the a different point in the current document, a shortcut may bypass "openedDocument".
	<!-- In <i>before</i> phase, augment DocInfo arg (cookies, referer).
	In <i>after</i> cache (sets {@link DocInfo}'s InputStream) and Document fires "openedDocument <i>DocInfo</i>". -->
	<p><tt>"openDocument"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>null</var> - same as reload<br />
	<tt>"openDocument"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>URI to open</var> - translated into...<br />
	<tt>"openDocument"</tt>: <tt>arg=</tt> {@link java.net.URL} <var>URI to open</var> - translated into...<br />
	<tt>"openDocument"</tt>: <tt>arg=</tt> {@link java.net.URI} <var>URI to open</var> - translated into..., replacing the current document<br />
	<tt>"openDocument"</tt>: <tt>arg=</tt> {@link multivalent.DocInfo} <var>URI, document to replace, and other details</var> - final form of all "openDocument"'s
	 */
	public static final String MSG_OPEN = "openDocument";

	/**
	Announce opening of document has finished, before it has been formatted (completely -- there may be incremental displays).
	<!-- In <i>Before</i> phase, check validity (404, redirect), after known good (add to history, forward/backward buttons, ...) -->
	<p><tt>"openedDocument"</tt>: <tt>arg=</tt> {@link multivalent.DocInfo} <var>data of document just opened</var>
	 */
	public static final String MSG_OPENED = "openedDocument";

	/**
	Request build of document, which comes after successful {@link #MSG_OPEN open}.
	<p><tt>"buildDocument"</tt>: <tt>arg=</tt> {@link Document} <var>instance</var>
	 */
	public static final String MSG_BUILD = "buildDocument";

	/**
	Close {@linkplain multivalent.Browser#getCurDocument() current document},
	stopping loading if necessary, saving annotations and so on.
	Document data structures can then be safely destroyed.
	<p><tt>"closeDocument"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_CLOSE = "closeDocument";

	/**
	Create new browser window.
	<p><tt>"reloadDocument"</tt>: <tt>arg=</tt> {@link multivalent.Node} <var>replace this document, or current document if null</var>.
	 */
	public static final String MSG_RELOAD = "reloadDocument";

	/**
	Announce a redirection has taken place.
	<p><tt>"redirectedDocument"</tt>: <tt>arg=</tt> <var>URI of new address, as in {@link Document#MSG_OPEN}</var>
	 */
	public static final String MSG_REDIRECTED = "redirectedDocument";

	/**
	Stop loading of current document.
	<p><tt>"stopDocument"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_STOP = "stopDocument";

	/**
	Announce document has been formatted, which means all geometric posisions are valid.
	<p><tt>"formattedDocument"</tt>: <tt>arg=</tt> {@link multivalent.Document} <var>instance</var>.
	 */
	public static final String MSG_FORMATTED = "formattedDocument";

	/**
	Request reformat of {@linkplain #MSG_CURRENT "current"} document.
	<p><tt>"reloadDocument"</tt>: <tt>arg=</tt> {@link java.net.URI} <var>location of hub</var>
	 */
	public static final String MSG_REFORMAT = "reformatDocument";

	/**
	Request repaint of {@linkplain #MSG_CURRENT "current"} document, which will invoke reformat if document layout is dirty.
	<p><tt>"repaintDocument"</tt>: <tt>arg=</tt> {@link java.net.URI} <var>location of hub</var>
	 */
	public static final String MSG_REPAINT = "repaintDocument";

	/**
	Announce document has taken the active one, taking the focus.
	<p><tt>"currentDocument"</tt>: <tt>arg=</tt> {@link multivalent.Document} <var>instance</var>.
	@see multivalent.Browser#setCurDocument(Document)
	 */
	public static final String MSG_CURRENT = "currentDocument";


	/** Key to {@link java.util.Map} of anchor {@link Span}s and {@link Node}s, which typically come from nodes with ID attributes and HTML a NAMEs.
  public static final String VAR_ANCHORS = "anchors";
  => fast enough to search for these, either in node or span*/

	/** Key to {@link java.util.Map} of HTTP headers (perhaps simulated if loading from file system). */
	public static final String VAR_HEADERS = "headers";

	// hyperlinks in doc, so can get reverse links on anchor
	//** Key to {@link java.util.Map} of anchor names to corresponding {@link Span}s and {@link Node}s, which typically come from nodes with ID attributes and HTML a NAMEs. */
	//public static final String VAR_LINKS = "links";	// lots of bookkeeping and not used much.  maybe keep as a cache


	/**
	Attribute in {@link multivalent.Document} giving current page number in a multipage document.
	Pages are logically numbered <i>starting from 1</i> to <var>total-pages</var> inclusive, regardless of "printed" page numbers.
	Media adaptors translate between this and their internal numbering scheme.
	 */
	public static final String ATTR_PAGE = "page";

	/**
	Attribute in {@link multivalent.Document} giving number of pages in a multipage document.
	This is set by the media adaptor, if intrinsic to the document (as in DVI, PDF), or otherwise must be set in a hub.
	 */
	public static final String ATTR_PAGECOUNT = "pages";

	/** Metadata: document author. */
	public static final String ATTR_AUTHOR = "author";
	/** Metadata: document title. */
	public static final String ATTR_TITLE = "title";

	// => DocInfo
	/** Document URI. */
	public static final String ATTR_URI = "uri";
	/** Document genre, such as "HTML" or "ManualPage". */
	public static final String ATTR_GENRE = "genre";


	/** Non-null while loading is in progress. */
	public static final String ATTR_LOADING = "loading";

	/**
	Non-null if loading has been aborted.
	Document is left in a partially loaded state.
	Behaviors that depend on a well-formed document should abort.
	 */
	public static final String ATTR_STOP = "stop";



	/**
	??? Actual URI of content, after redirects and whatever else, as opposed to logical URI which is an attribute. ???
	Used when saving, and as base for relative URIs.
	 */
	/*protected*/public URI uri;

	private MediaAdaptor me_;

	protected StyleSheet styleSheet_ = null;

	final Layer layers_ = Layer.getInstance("group", this);

	Map<Object,Object> global_ = new HashMap<Object,Object>(10);

	List<Document> subdocs_ = new ArrayList<Document>(5);

	/** Shortcut field (could climb tree all the time), and to retain a hook so that detached <tt>Document</tt> can be instantiate behaviors, as for <tt>Span</tt>s. */
	private Browser br_;	// needed because all Behavior instantiation done through Browser... which needs Control's remapping table



	public Document(String name, Map<String,Object> attr, INode parent) {
		this(name, attr, parent, parent!=null? parent.getRoot().getBrowser(): null);	// parent==null for Root
	}

	public Document(String name, Map<String,Object> attr, INode parent, Browser br/*Root passes directly*/) {
		super(name,attr, parent);
		//super("IROOT",attr, parent);

		//styleSheet.put(name, new CLGeneral());
		br_=br;

		clear();
		//if (doc!=null) br.setCurDocument(this); -- set by Document.open(), not at creation
	}

	/**
	Clean up state (globals, children, ...), {@link Behavior#destroy()} genre and document-specific behaviors, and recursively close sub-{@link Document}s.
	Whatever saving needs to be done should have already taken place.
	 */
	public void clear() {
		//long start = System.currentTimeMillis();
		// first disengage from other data structures
		removeAllChildren();

		for (int i=0,imax=subdocs_.size(); i<imax; i++) subdocs_.get(i).clear();

		// local behaviors
		/*if (layers_!=null)*/ layers_.destroy();
		//long cost = System.currentTimeMillis()-start;
		//if (cost>0) System.out.println("cleanliness costs "+cost+" ms");

		// Document state
		if (attr_!=null) attr_.clear();	// don't instantiate with attrs, so OK (otherwise would immediately wipe instantiation's)
		uri = null;
		global_.clear();

		observers_ = null;

		//setScrollbarShowPolicy(VScrollbar.SHOW_AS_NEEDED);
		getVsb().setValue(0);	// changed by #anchor, forward/backward menu saved position

		//	Browser br = getBrowser();
		//System.out.println("root="+getRoot()+", browser="+br+", root's browser="+getRoot().getBrowser());
		if (br_==null && getParentNode()!=null) br_=getParentNode().getBrowser();
		if (br_!=null) layers_.clear();	// layers_ = Layer.getInstance("group", this);

		styleSheet_ = new StyleSheet();	// default, but may be replaced, as by CSS
		me_ = null;
	}

	/*
  public void appendChild(Node child) {
	// should verify that adding visual layer kind of things
	super.appendChild(child);
  }*/


	@Override
	public void setParentNode(INode p) {
		// unregister with old
		Document pdoc = parent_!=null? parent_.getDocument(): null;
		if (pdoc!=null) pdoc.subdocs_.remove(this);

		super.setParentNode(p);

		// register with new
		pdoc = p!=null? p.getDocument(): null;
		if (pdoc!=null) pdoc.subdocs_.add(this);
	}


	/** Returns the topmost layer, which holds useful layers (system, genre, annotations, ...). */
	public Layer getLayers() { return layers_; }


	@Override
	public Browser getBrowser() { return br_; }

	@Override
	public Root getRoot() { return br_!=null? br_.getRoot(): null; }	// shortcut

	public URI getURI() { return uri; }

	/*
  protected void setURI(URI newurl) {
	docURI = newurl;
	docbox.setTitle(docURI.toString());	// HTML replaces with <TITLE>
	putAttr("uri", getURL().toString());	// saved to hub
 }*/

	/** End {@link Node}'s chain up tree by returning <var>this</var>. */
	@Override
	public Document getDocument() { return this; }

	/**
	Return first-level (immediately under {@link #getLayers() layer-of-layers}) layer of that name if it exists, null if not.
	Same as {@link #getLayers()}.{@link Layer#getInstance(String)}.
	 */
	//public Layer getLayer(String name) { return (Layer)layers_.getBehavior(name); }
	/** Convenience method for {@link #getLayers()} plus {@link Layer#getInstance(String)}. */
	public Layer getLayer(String name) {
		return layers_.getInstance(name);
	}

	public StyleSheet getStyleSheet() { return styleSheet_; }
	public void setStyleSheet(StyleSheet ss) { styleSheet_=ss; }

	/** Returns (primary) media adaptor that created document tree. */
	public MediaAdaptor getMediaAdaptor() { return me_; }
	public void setMediaAdaptor(MediaAdaptor me) { if (me_==null) me_ = me; }

	public final Object getVar(Object key) { return global_.get(key); }	// uses key.equals not ==, so don't have to intern strings
	public final void putVar(Object key, Object val) { global_.put(key, val); }
	public final void removeVar(Object key) { global_.remove(key); }



	/** <tt>Document</tt>s have own StyleSheet and associated Context. */
	@Override
	public boolean formatBeforeAfter(int width,int height, Context cx) {
		//System.out.println("Document format "+width+"x"+height);
		Context newcx = styleSheet_.getContext(cx!=null? cx.g: null/*(Graphics2D)br.getGraphics()--null*/, cx);
		newcx.valid = false;
		/*System.out.print(getName());
List<ContextListener> active = newcx.vactive_;
for (int i=0,imax=active.size(); i<imax; i++) System.out.println("  "+active.get(i));*/
		// maybe disregard Context, or cx.pushFloat(), cx.popFloat()
		boolean ret = super.formatBeforeAfter(width,height, newcx);
		//newcx.valid=false;	// entering context unaffected

		Browser br = getBrowser();
		if (br!=null && getDocument()!=br.getRoot()) br.event(new SemanticEvent(br, Document.MSG_FORMATTED, this));

		return ret;
	}

	@Override
	public boolean formatNode(int width,int height, Context cx) {
		int cin;
		boolean ret = false;
		//long start = System.currentTimeMillis();
		do {
			cin = size();
			ret = super.formatNode(width, height, cx);
		} while (size() != cin);	// can add children while format -- maybe put in IScrollPane
		//System.out.println("formatted "+getFirstLeaf()+" in "+(System.currentTimeMillis()-start));	//-- usually 0

		//valid_ = true; => in superclass
		//return !valid_; => just convention for shortcircuit, which take from superclass here
		return ret;
	}

	/** Substitutes own {@link StyleSheet} and associated {@link Context} in place of one in prevailing {@link Context}. */
	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		Context newcx = styleSheet_.getContext(cx.g, cx);
		newcx.valid = false;
		//System.out.println(getName()+", base in = "+cx.base_+", out="+newcx.base_);
		//System.out.println("painting Document "+getName()+", bbox="+bbox+", fg="+cx.foreground+", bg="+cx.background+", valid="+valid_);
		//System.out.println("DOC "+getFirstLeaf()+", "+docclip+", "+g);
		super.paintBeforeAfter(docclip, newcx);
		//newcx.g = null;	// X Graphics just valid long enough to paint => needed for events
	}

	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		//if (!cx.valid) cx.reset();
		//if (cx.pagebackground!=null) { g.setColor(cx.pagebackground); g.fillRect(0,0, bbox.width,bbox.height); }
		//System.out.println("Document pagebackground = "+cx.pagebackground);

		//System.out.println("paintNode Document "+getFirstLeaf());
		//System.out.println("doc "+getName()+"  "+cx.pagebackground+" => "+cx.background);
		//Color oldbg = cx.pagebackground;
		cx.pagebackground = cx.background;	// usually already set to this, but if transparent and floating over another document and want to see selection that is same color as topmost background

		super.paintNode(docclip, cx);

		// draw box around current document
		if (this==getBrowser().getCurDocument()) {
			Graphics2D g = cx.g;
			g.setStroke(Context.STROKE_DEFAULT);
			g.setColor(Color.BLACK); g.drawRect(getHsb().getValue()-padding.left,getVsb().getValue()-padding.top, bbox.width-1,bbox.height-1); //g.drawRect(1,1, bbox.width-1,bbox.height-1);
		}
		/*
	System.out.println("doc bbox="+bbox+", docclip="+docclip);
	for (int i=0,imax=size(); i<imax; i++) {
		xNode child = childAt(i);set
		System.out.println("\t"+child.getName()+", bbox="+child.bbox);

	}*/
		//cx.pagebackground = oldbg;
	}

	//* when go to click in non-content, doc mis-set
	/**
	On mouse click, set this document to the current one.
	Have to do this before behaviors that would rely on document being set so right behaviors get called,
	which means can't do it in BindingsDefault because that would be too late, and can't
	do in BindingsDefault.eventBefore() because don't know right document.
	 */
	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		if (e.getID()==MouseEvent.MOUSE_PRESSED) {	// click-for-focus.  delete for cursor-for-focus
			Browser br = getBrowser();
			Document curdoc = br.getCurDocument();
			if (curdoc!=this && getParentNode()!=null && !"_UIROOT".equals(getParentNode().getName()))
				br.setCurDocument(this);
			//CursorMark curs = br.getCursorMark();
			//if (curdoc.contains(curs.getStart().node)) curs.remove();
			//System.out.println("curdoc");
			//br.repaint(100);	// erase old current's border, draw mine
			//return true;	// so not snatched away by
		}
		//System.out.println("setting doc to parent of "+childAt(0));
		/*return*/ super.eventNode(e, rel);
		return true;	// event opaque
	}

	/**
	Returns visual layer of passed name, creating if necessary.
	<!--While any INode can have visual layers, it probably only makes sense to put them on Document's, Root's and perhaps a few others.-->
	Maybe move this to IScrollPane.

	@deprecated  visual layers not special
	 */
	@Deprecated
	public INode getVisualLayer(String name) {
		//INode n = getVisualLayer(name,name);
		//if (n!=null) n.name_=name;	// else all-lc
		//return n;
		return getVisualLayer(name,name);
	}

	public INode getVisualLayer(String name, String classname) {
		Node n;
		for (int i=0,imax=size(); i<imax; i++) {
			n = childAt(i);
			//System.out.println(name+" == "+n.getName());
			if (name.equals(n.getName())) return (INode)n;
		}

		// not found -- create according to passed name
		try {
			n = (Node)Class.forName(classname).newInstance();
			//n.setName(name);	// no attributes
			n.name_ = name;	// special case because if pass classname, it's zapped to all-lc so it doesn't match next time
			appendChild(n);	// sets parent
		} catch (Exception/*ClassNotFound or Instantiation or IllegalAccess*/ e) {
			System.out.println("can't create class "+classname+" -- "+e);
			n=null;
		}
		return (INode)n;
	}



	/*
  String saveX() {
	// first see if anything to save
	// more efficient but less secure to pass StringBuffer to everybody
	// for now, unsafe passing of output stream to behaviors (should have them report data so can be protected here)
	StringBuffer sb = new StringBuffer(10*1024);	// adjust once see size of actual files
	// LATER: inspect layer to see if should save inline or as a reference

	// REPLACE WITH CALL TO DOCUMENT-SPECIFIC LAYERS
	// + add destroy on DOCUMENT-SPECIFIC LAYERS
	//for (int i=0,imax=vlayer_.size(); i<imax; i++) ((Behavior)vlayer_.get(i)).save(sb,1);
	//XXXsyslayer_.save(sb, 1);
	if (sb.length()<=10) return null;	// don't write hub if didn't change anything

	// wrap behaviors' content
	StringBuffer hub = new StringBuffer(sb.length()+500);
	// sb.append("<!-- selected profiling statistics\n"); showProfile(sb); sb.append("-->\n");
	hub.append("<!--\n\tDocument saved "+DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG).format(new Date())+"\n-->\n");
	/*if (DEBUG) * / hub.append("<!-- selected profiling statistics\n"); showProfile(sb); hub.append("-->\n");

	//XXXXML.writeTagStart(hub, "MULTIVALENT", attr_);
//Object[] keys = attr_.keySet().toArray();
//for (int i=0,imax=keys.length; i<imax; i++) System.out.println("key="+keys[i]+", value="+attr_.get(keys[i]));
	hub.append(sb.substring(0));
	//XML.writeTagEnd(hub, "MULTIVALENT");

	return hub.toString();
  }
	 */

	@Override
	public boolean checkRep() {
		assert super.checkRep();

		//assert me_!=null;
		assert getLayers()!=null;
		assert getLayers().checkRep();	// checks behaviors
		assert styleSheet_!=null;
		//assert styleSheet_.checkRep();

		return true;
	}
}
