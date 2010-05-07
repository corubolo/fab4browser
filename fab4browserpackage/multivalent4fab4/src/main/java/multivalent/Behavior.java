package multivalent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import multivalent.gui.VButton;
import multivalent.gui.VCheckbox;
import multivalent.gui.VEntry;
import multivalent.gui.VMenu;
import multivalent.gui.VMenuButton;
import multivalent.gui.VRadiobox;
import multivalent.gui.VSeparator;
import multivalent.node.LeafUnicode;
import multivalent.node.Root;
import multivalent.std.VScript;



/**
	The Behavior is the primary way to extend the system.
	In fact, behaviors realize all user-level functionality.
	Behaviors have great power and access to all parts of the system.

	<ul>
	<li>{@link #getInstance(String,String,ESISNode, Map,Layer) behavior creation}
	<li>accessors: {@link #getName()}, {@link #getLayer()},
	<li>Protocols:
	{@link #restore(ESISNode, Map, Layer) restore},
	build ({@link #buildBefore(Document) before} / {@link #buildAfter(Document) after},
	format ({@link #formatBefore(Node) before} / {@link #formatAfter(Node) after},
	paint ({@link #paintBefore(Context, Node) before} / {@link #paintAfter(Context, Node)},
	clipboard ({@link #clipboardBefore(StringBuffer, Node)} / {@link #clipboardAfter(StringBuffer, Node)},
	semantic events ({@link #semanticEventBefore(SemanticEvent, String)} / {@link #semanticEventAfter(SemanticEvent, String)},
	low-level events ({@link #eventBefore(AWTEvent, Point, Node)} / {@link #eventAfter(AWTEvent, Point, Node)}),
	{@link #destroy() destroy}.
	<li>convenience: {@link #getDocument()}, {@link #getRoot()}, {@link #getBrowser()}, preferences {@link #getPreference(String,String) get} / {@link #setPreference(String,String) set}
	<li>software engineering: {@link #getLogger()}, {@link #checkRep()}
	<li>(temporary user interface building: {@link #createUI(String, String, Object, INode, String, boolean)})
	</ul>

	<!-- implementation -->
	<p><em>All behaviors must subclass this class</em> and override the proper protocol methods
		(build, format, paint, and so on, most with before and after phases)
		and take the appropriate action when called upon by the system framework.
	The system framework coordinates behaviors along protocols, and if a behavior
		adheres to proper implementation of the protocols, it should cooperate well
		with arbitrary other behaviors.

	<p id='constructor'><font color='red'>Behaviors should not define constructors.</font>
	The system needs a <tt>public</tt> no-argument constructor,
	to restore by name (with {@link #getInstance(String, String, ESISNode, Map, Layer)}
	which in turn uses {@link Class#forName(String)} and {@link Class#newInstance()}).
	The easiest way to accomplish this is to declare the class public and rely on the default no-argument constructor.
	(Do initialization in {@link #restore(ESISNode, Map, Layer)}.)

	<p>The file <tt>multivalent/devel/MyBehavior.java</tt> in the source code .zip file
	is a convenient template for writing general behaviors.
	If a behavior is a {@link Span} or {@link multivalent.std.lens.Lens} or resembles the function of another behavior,
	you can instead copy that source code and modify it.

<!--
  put in assertions and other error checking everywhere
  for setters, return pointer to class so can cascade and approach Tk syntactic convenience
	=> conflicts with Java convention of returning void (except for StringBuffer.append)
-->

	@version $Revision: 1.15 $ $Date: 2005/07/05 04:23:24 $
 */
public abstract class Behavior /*implements EventListener--on individual elective basis*/ /*implements Observer?*/ extends VObject {
	/** Class name to instantiate for behavior.  Saved/restored in hubs. */
	public static final String ATTR_BEHAVIOR = "behavior";


	/** Keep track of behaviors that can't be loaded. */
	private static Map<String,String> deadbe__ = new HashMap<String,String>(10);

	// content for createUI can be specified as HTML
	private static MediaAdaptor htmlparser__ = null;
	//private static Root htmlroot__;

	// for loading effects
	private static final Color[] RAINBOW = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GRAY, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE, Color.YELLOW };
	private static int rainbowi_ = 0;
	//private static NFont rfont_ = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFONT.FLAG_SERIF, 12f);
	private static int y_ = 0;


	// BEHAVIOR STATE: handle to layer, attributes Map (maybe null)
	/** Logical name. */
	protected String name_ = null;

	/**
	Class name, which is short name that can be remapped or can be same as <code>getClass().getName()</code>.
	<!-- When restoring from hub, this comes from {@link #ATTR_BEHAVIOR}. => mention in MultivalentAdaptor -->
	 */
	private String classname_ = null;	// behavior name, can be different than <code>getClass().getName()</code>, so we can remap.  Also saves BEHAVIOR attribute slot, sometimes Hash creation, and hence memory
	//protected Map attr_ = null; => VObject
	/*package-private for Multivalent.Layer*/ Layer layer_ = null;	// layer has link to doc, anno color/font, the view (later)
	// protected cname_ = null;	// creation name, as should always use #getInstance(...)
	// END STATE: 12 bytes over Object (8 + 4 in VObject) -- actually not that many Behaviors, so space efficiency not critical (Nodes on the other hand...)



	/**
	Returns the String used to instantiate the Behavior via {@link #getInstance(String, String, Map, Layer)}.
	This can be the same as getClass().getName(), such as <code>multivalent.std.lens.Note</code>,
	but usually should be a short name referring to the type of behavior, such as "Note",
	which can then be mapped into a specific Java class name via the table in a user's <tt>Preferences.txt</tt> file.
	 */
	@Override
	public String getName() { return name_; }

	public/*grr*/ void setName(String name) {	// => kill, so can't change name after instantiation
		//assert name!=null;	// ?
		//if (name_==null) {
		name_ = name!=null? name.toLowerCase(): null;	// can't save uppercase in XML hubs
		//name_ = name;	//(name!=null? name: getClass().getName());
		//}
	}

	/** Link to containing layer, which links to Document and so on to Root, Browser, Multivalent. */
	public Layer getLayer() { return layer_; }

	/** Convenience method to fetch Document from Layer. */
	public Document getDocument() { Layer l=getLayer(); return l!=null? l.getDocument(): null; }
	/** Convenience method to fetch Root from Browser. */
	public Root getRoot() { Browser br=getBrowser(); return br!=null? br.getRoot(): null; }
	/** Convenience method to fetch Browser from Layer. */
	public Browser getBrowser() { Layer l=getLayer(); return l!=null? l.getBrowser(): null; }

	// move to VObject?
	/** Convenience method for getting preference in class Multivalent. */
	public String getPreference(String name, String defaultval) { return getGlobal().getPreference(name, defaultval); }
	/** Convenience method for putting preference in class Multivalent. */
	public void putPreference(String name, String val) { getGlobal().putPreference(name, val); }

	// too much policy here.  maybe make property of layer
	public boolean isEditable() {
		Layer layer = getLayer();
		return layer!=null && layer.isEditable();
	}



	/**
	Centralized behavior instantiation <i>factory</i>: <b>instantiate all behaviors through this method</b>
	-- never use <code>new <var>behavior</var>(...)</code>.
	<ul>
	<li>remapping of names so can dynamically replace references in old hubs
	<li>centralized, correct instantiation and error handling
	</ul>
	{@link #restore(ESISNode, Map, Layer)} is called as part of instantiation, serving the place of arguments to a constructor.
	Logical names are normalized to all lowercase.
	 */
	public static Behavior getInstance(String logicalname, String behaviorclass, ESISNode children, Map<String,Object> attr, Layer layer) {
		assert logicalname!=null && behaviorclass!=null /*&& (layer!=null || xxx instanceof Layer)--topmost Layer*/: "logical="+logicalname+", class="+behaviorclass;
		if (behaviorclass==null) return null;	// if not testing (asserts on), keep on truckin'

		if (multivalent.Meta.MONITOR/*false*/ && layer!=null && layer.getBrowser()!=null) {
			Browser br = layer.getBrowser();
			Graphics g = br.getGraphics();	// -- works, but not well
			if (Behavior.rainbowi_==Behavior.RAINBOW.length) Behavior.rainbowi_=0;
			if (g!=null) {	// !Multivalent.standalone_ / embedded in Swing
				g.setColor(Behavior.RAINBOW[Behavior.rainbowi_++]);
				g.fillRect(0,0, 10/*rainbowi_*/,10);//g.drawLine(0,10, 10,0);
			}

			/*		g.setColor(Color.WHITE); g.fillRect(250,y_, 600,20);
		g.setColor(Color.BLACK); rfont_.drawString(g, logicalname+" / "+behaviorclass, 300, y_+15);
		y_ += 20; if (y_>20*10) y_ = 0;	// set to 600 to conver page, but can only take in 10 lines at a time
			 */
		}
		//long start = System.currentTimeMillis();

		//String oname = logicalname;
		logicalname = logicalname.toLowerCase();
		//if (!(oname.equals(logicalname))) System.out.println("uppercase logical: "+oname);	// warn of this inefficiency

		Multivalent v = Multivalent.getInstance();
		String bname = v.remapBehavior(behaviorclass);
		Logger log = getLogger();
		//getLogger().finest((/*"getInstance "+logicalname+"/"+behaviorclass+"=>"+bname);

		if (Behavior.deadbe__.get(bname) == null)
			try {
				ClassLoader cl = /*v.getJARsClassLoader();*/v.getClass().getClassLoader();
				//assert cl == Behavior.class.getClassLoader();	// true now with new bootstrapping
				Object bc = Class.forName(bname, true, cl).newInstance();
				Behavior be = (Behavior)bc;
				//be.setName(logicalname);	// can't save uppercase
				be.name_ = logicalname;
				//be.name_ = (logicalname!=null? logicalname.toLowerCase(): null);
				be.classname_ = behaviorclass;

				//System.out.println(bname);
				// see save() -- this still ok
				//if (attr==null) {} else if (attr.get(ATTR_BEHAVIOR)!=null) { if (attr.size()==1) attr=null; else attr.remove(ATTR_BEHAVIOR); }

				/*
		long time = System.currentTimeMillis() - start;
		if (false && g!=null/* && time > 0L* /) {
			g.drawString(Long.toString(time), 250,15);
			try { Thread.currentThread().sleep(2000L); } catch (Exception e) {}
		}
				 */
				be.restore(children,attr, layer);	// after added to layer, so can climb around and find things during restore
				//log.finest("created "+bname);

				//assert layer!=null || be instanceof Layer;	// => temporary use of behavior that don't want to make logical part of document
				return be;

			} catch (ClassNotFoundException e) {
				log.warning("couldn't find behavior "+bname+" -- ignored");

			} catch (ClassCastException e) {
				log.severe("class "+bname+" is not a behavior (does not subclass from Behavior) -- ignored");
			} catch (InstantiationException e) {
				log.severe("couldn't instantiate "+bname+" -- is it abstract?");
			} catch (IllegalAccessException e) {
				log.severe(bname+": "+e+" -- perhaps class or constructor needs to be public");

			} catch (Exception e) {
				e.printStackTrace();
				log.severe("unanticipated error while restoring "+logicalname+"/"+bname+": "+e);
			}

			// error fall through
			Behavior.deadbe__.put(bname, bname);
			return null;
	}


	/** Used in hub instantiation. */
	public static Behavior getInstance(String logicalname, String behaviorclass, Map<String,Object> attr, Layer layer) {
		return getInstance(logicalname, behaviorclass, null,attr, layer);
	}



	/* *************************
	 * PROTOCOLS
	 * As much as possible, protocol methods should be empty here in the ur-superclass, so subclasses don't have to worry about calling super.<protocol>(...)
	 **************************/

	/**
	Build up save data as ESIS tree, then write that out.  Makes node with GI = behavior name, same attributes, and possibly some children.
	Attributes are cloned, so if you want to stuff attributes from state, do that before <code>super.save()</code>.
	Always have {@link #ATTR_BEHAVIOR} from save (short) classname.
	 */
	public ESISNode save() {
		//if (getAttr(ATTR_BEHAVIOR)==null) putAttr(ATTR_BEHAVIOR, classname_);	//NOT getName(), NOT getClass().getName());	// temporary.  X remove BEHAVIOR attribute in favor of name => want names for things other than behvaior so no way to distinguish be from non-be
		Map<String,Object> attrs = getAttributes();
		if (attrs!=null) { Map<String,Object> newattrs = new HashMap<String,Object>(attrs.size()); newattrs.putAll(attrs); attrs=newattrs; }	// make copy (ok if inefficient here)
		ESISNode e = new ESISNode(getName(), attrs);

		// take ATTR_BEHAVIOR if exists, else use value cached in field
		e.putAttr(Behavior.ATTR_BEHAVIOR, getAttr(Behavior.ATTR_BEHAVIOR, classname_));	// maintain classname_ in attribute, since few behaviors and above is disgusting

		return e;
	}


	/* *
	If behavior has custom output for <outputformat>, stuff it in StringBuffer and return true
	else return false
	 */
	/*public boolean save(StringBuffer sb, int level, String outputformat) { -- LATER make this the protocol
	return false;
  }*/


	/**
	Takes the place of a constructor's initialization functions; that is, it is invoked exactly once immediately after object instantiation.
	When restored from hub, passed XML content subtree if any as first parameter and attributes from XML tag's attributes.
	This protocol cannot be short-circuited.
	 */
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		//assert ... any can be null
		//assert attr_ == null: "restore exactly once";

		/*if (n!=null || attr!=null)*/ attr_=attr;	// subsequent restores keep original attributes => single restore

		//System.out.println("Behavior "+getName()+" layer = "+(layer!=null?layer.getName():"|null|"));
		if (layer!=null) layer.addBehavior(this);
	}

	/**
	Recursively process ESIS children, restore children as behaviors.
	Not called by default from restore() because behaviors may selectively process children.
	 */
	public void restoreChildren(ESISNode n, Layer layer) {	// NB: overridden by Layer, so propagate changes.
		//System.out.println("restoring "+n.getGI());
		// with inner classes pass function to tree traversal method?
		assert layer!=null;

		// traverse bottom up, so complex nodes higher up have components built first
		if (n!=null) for (int i=0; i<n.size() /* count can change while enumerating--still?  that would be bad */; i++) {
			Object child = n.childAt(i);
			//if (child instanceof ESISNode) System.out.println("child = "+((ESISNode)child).getGI());
			if (child instanceof ESISNode) {
				ESISNode m = (ESISNode)child;
				String bname = m.getAttr(Behavior.ATTR_BEHAVIOR);
				if (bname!=null) {
					Behavior be = getInstance(m.getGI()/*bname*/,bname, m,m.attrs, layer);
					be.removeAttr(Behavior.ATTR_BEHAVIOR);	// kept in classname_
				} // else assume subclass has some use for it.  DO NOT take GI as behavior name (with remapping)!

				// most behaviors with internal structure call restoreChildren first and then (rest of) self
			}
		}
	}


	/**
	A round robin protocol, here traversing all behaviors from highest to lowest priority, during which main contributors of document content (called media adaptors) hack their content into the tree,
	and annotators set Marks to robust anchor to locations to hack in buildAfter so as not to disturb positioning by other annotations.
	That is, behaviors build the document tree by constructing nodes, which themselves are usually specialized for concrete document types.
	This protocol cannot be short-circuited.

	@see multivalent.Mark
	 */
	public void buildBefore(Document doc) {}

	/** Traverses all behaviors from lowest to highest priority, during which tweaks hack their content into the tree, */
	public void buildAfter(Document doc) {}


	/**
	A tree walk protocol, called before observed node has been formatted.
	@return <tt>true</tt> to short-circuit to formatAfter at that node, bypassing formatting of the subtree.
	Could be used on collapsed outline sections to save work formatting content that wouldn't be displayed anyhow.
	 */
	public boolean formatBefore(Node node) { return false; }

	/**
	A tree walk protocol, called after observed node has been formatted.
	For instance, Search uses this to keep its coordinates of matches current.
	@return <tt>true</tt> to short-circuit formatting of rest of protocol, which consists exclusively of higher-priority behaviors, which almost never want to do.
	 */
	public boolean formatAfter(Node node) { return false; }


	/**
	A tree walk protocol, called before observed node has been painted.
	Called in same coordinate space as node's painting.
	Can be used draw special background, but usual background setting is done by spans or style sheets.
	@return <tt>true</tt> to short-circuit to paintAfter at that node, bypassing painting of the subtree.
	 */
	public boolean paintBefore(Context cx, Node node) { return false; }

	/**
	A tree walk protocol, called before observed node has been painted.
	Called in same coordinate space as node's painting.
	Use to draw annotations at node's location.
	For instance, Search uses this to decorate the scrollbar (a type of node) with location of matches.
	 */
	public boolean paintAfter(Context cx, Node node) { return false; }


	/**
	A tree walk protocol, called before observed node has been given a chance to contribute to the growing selection content in the passed StringBuffer.
	As a special case, observers on the root are always called, even if the selection is for only a part of the document.
	This way, a behavior could add author and title attribution to all selections.
	Remember that in addition to appending text to a StringBuffer, text can be inserted anywhere.
	@return <tt>true</tt> to short-circuit to selectAfter at that node, bypassing selection of the subtree.
	For instance, if you're selecting bibliographic entries, you might want to generate BibTeX or IEEE formatting rather
	than straight text; likewise, on math, generate Lisp, TeX, or Mathematica.
	 */
	public boolean clipboardBefore(StringBuffer sb, Node node) { return false; }

	/**
	A tree walk protocol, called after observed node has been given a chance to contribute to the growing selection content in the passed StringBuffer.
	As a special case, observers on the root are always called, even if the selection is for only a part of the document.
	 */
	public boolean clipboardAfter(StringBuffer sb, Node node) { return false; }


	/**
	Round robin distribution to all behaviors.
	Message and clientData unpacked from SemanticEvent for convenience.
	Message is interned, so if you compare to a literal, you can use "==".
	Message can't be changed (it can be short-circuited out of), but client data can be mutated as it is passed along (so it's not passed as a parameter).
	 */
	public boolean semanticEventBefore(SemanticEvent se, String msg) { return null==msg; }

	/**
	Round robin distribution to all behaviors.
	Message and clientData unpacked from SemanticEvent for convenience.
	Message is interned, so if you compare to a literal, you can use "==".
	Message can't be changed (it can be short-circuited out of), but client data can be mutated as it is passed along (so it's not passed as a parameter).
	 */
	public boolean semanticEventAfter(SemanticEvent se, String msg) { return null==msg; }


	/**
	@see multivalent.SemanticEvent
	@see java.awt.AWTEvent
	@see multivalent.Browser
	@see multivalent.std.lens.LensMan
	 */
	public boolean eventBefore(AWTEvent e, Point rel, Node n) {
		//assert e!=null;	// ?
		return false;
	}

	/**
	During {@link #eventBefore(AWTEvent, Point, Node)}, the behaviors that take primary action to that event should do so,
	after having given other behaviors the opportunity to filter it during <tt>eventBefore</tt>.
	 */
	public boolean eventAfter(AWTEvent e, Point rel, Node n) {
		// elaborate/filter
		return false;
	}


	/** Future protocol: Name and all overloads <b>reserved</b> for future use. */
	public final void undo() {
	}

	/** Future protocol: Name and all overloads <b>reserved</b> for future use. */
	public final void redo() {
	}



	/**
	Protocol.
	Cleans up state before being decommissioned: remove from Layer, observed nodes, ....
	<!-- Guaranteed to happens at {@link Document#clear()}, and so is preferred to listening for {@link Document#MSG_CLOSE}. => different purposes -->
	Clients shouldn't hold a pointer/handle to object after destroy() as it is in an invalid state.
	This protocol cannot be short-circuited.
	 */
	public void destroy() {
		if (layer_!=null) layer_.removeBehavior(this);
		layer_ = null;	// expicitly invalidate
		//System.out.print("X"+getName()+"  ");
	}


	public static Logger getLogger() { Multivalent.getInstance();
	return Multivalent.getLogger(); }


	// share disabled behavior?  should make it an easy add-on rather than a parameter
	//private static Behavior disinst_ = null; => works but don't need to.
	/**
	Convenience function for UI building.  Returns created widget for further configuration.

	This will be replaced by a separate GUI-handling behavior, but not for a while.
	UI widgets can be made from a hub with SemanticUI.

	@param type  is one of "button", "checkbox", "radiobox", "menubutton", "separator", "entry", "label".
	@param title  can be HTML fragment.
	@param script  can be a {@link java.lang.String} or {@link multivalent.SemanticEvent}

	@see multivalent.std.ui.SemanticUI
	 */
	public Node createUI(String type, String title, Object script, INode parent, String category, boolean disabled) {
		//assert ... any param can be null

		type = type==null? "button": type.toLowerCase().intern();
		String name = parent instanceof VMenu? "menuitem": type;

		//System.out.println("createUI: "+type+", "+title+", "+script+", "+parent+", "+category+", "+disabled);
		Node butt=null;
		// later replace by getInstance()?
		if ("checkbox"==type)
			butt = new VCheckbox(name,null, null);
		else if ("radiobox"==type)
			butt = new VRadiobox(name,null, null, null);
		else if ("menubutton"==type)
			butt = new VMenuButton(name,null, null);
		else if ("separator"==type) {
			butt = new VSeparator(title,null, null);
			disabled = false;
		} else if ("entry"==type)
			butt = new VEntry(name,null, null);
		// caller has to size WxH
		else if ("label"==type)
			butt = null;
		else { assert "button"==type;
		butt = new VButton(name,null, null);
		}

		Node content=null;
		if (title!=null) {	// && butt.isStruct()) {	// VSeparator could have name like "<SEARCH-GROUP>"
			int tags = title.indexOf('<'), tage = tags!=-1? title.indexOf('>'): -1;
			if (tags!=-1 && tage!=-1 /*&& tage < title.length()-1*/)
				// interpret as XML... for now, HTML
				//MediaAdaptor htmlparser__ = (MediaAdaptor)getInstance("gui","HTML",null, getDocument().getLayer(Layer.SCRATCH));
				try {
					if (Behavior.htmlparser__==null) Behavior.htmlparser__ = (MediaAdaptor)getInstance("gui","HTML",null, getDocument().getLayer(Layer.SCRATCH));	// null no good because need Browser to make span instances, and layer_ no good because just random
					//Document doc = new Document(null, null, null, getBrowser());
					Document rootdoc = new Root(null, getBrowser());
					//if (htmlroot__==null) htmlroot__ = new Root(null, getBrowser()); else htmlroot__.getLayer(Layer.SCRATCH).clear();
					//Document rootdoc = htmlroot__;
					Behavior.htmlparser__.setInput(new com.pt.io.InputUniString(title, null));
					INode html = (INode)Behavior.htmlparser__.parse(rootdoc);
					//html.dump();
					Node bodyn = html.findDFS("body");
					//INode body = (INode)(html.findDFS("body"));
					//System.out.println("html = "+html+", body="+body); body.dump();
					INode body = bodyn instanceof INode? (INode)bodyn: null;	// could have leaf named "body"
					if (body==null) content=html.childAt(1); else if (body.size()==1) content=body.childAt(0); else content=body;
					/*if (parent!=null) {	// kludgy: HTML attributes put in style sheet
					StyleSheet ssp = body.getDocument().getStyleSheet(),
							   ssn=parent.getDocument().getStyleSheet();
					for (Iterator<> i=ssp.name2span.entrySet().iterator(); i.hasNext(); ) {
						Map.Entry<> e = i.next();
						if (e.getKey() instanceof Node) ssn.put(e.getKey(), (ContextListener)e.getValue());
					}
				}*/
					//body.dump();
					//System.out.println("============");
					//content.dump();
					//new LeafUnicode(title,null, n);
				} catch (Exception badhtml) {
					System.err.println("can't parse |"+title+"| as HTML => "+badhtml);
					badhtml.printStackTrace();
				} finally {
					try { Behavior.htmlparser__.close(); } catch (java.io.IOException ioe) {}
				}
				//htmlparser__.destroy();
				else {
					if (title.startsWith("$")) title=VScript.getVal(title, getBrowser().getCurDocument(), getAttributes());
					content = new LeafUnicode(title,null, null);
				}
		}

		if (content!=null)
			if (butt==null) butt=content;
			else if (butt.isStruct()) {
				Node pn = butt.getFirstLeaf();
				if (pn==null) pn=butt; else pn=pn.getParentNode();
				INode p = (INode)pn;
				p.removeAllChildren();
				p.appendChild(content);
				//((INode)butt).appendChild(content);
			}

		if (script!=null) butt.putAttr("script", script);

		if (parent!=null) parent.addCategory(butt, category);

		if (disabled)
			//		  Document doc = parent.getDocument();	// a popup menu itself may not have parent-hence-doc
			//System.out.println("disabled: "+br+", doc="+doc);
			//System.out.println("layer="+doc.getLayer(Layer.SCRATCH));
			//		if (disabled) butt.addObserver(getInstance("isableTree",null, doc.getLayer(Layer.SCRATCH)));
			//if (disinst_ == null) disinst_ = getInstance("disabled","DisableTree",null, null);
			//butt.addObserver(disinst_);
			butt.addObserver(getInstance("disabled","DisableTree",null, null));

		return butt;
	}



	@Override
	public boolean checkRep() {
		assert super.checkRep();

		return true;
	}

	/** Dump name of behavior and identifying/distinguishing short excerpt of content. */
	@Override
	public String toString() { return getName(); }
}
