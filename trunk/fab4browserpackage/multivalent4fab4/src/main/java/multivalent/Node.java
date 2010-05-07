package multivalent;

import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import multivalent.node.Root;

//import org.w3c.dom.*; -- not a good match, but keep in mind



/**
	Base class for nodes of the document tree (both user interface and content), providing common tree manipulation methods
	as well as implementing <i>before</i> and <i>after</i> phases of tree walk protocols,
	calling another method that can be easily overridden.
	Do not subclass this class; use or subclass either of {@link INode} for internal nodes or {@link Leaf} for leaves.


	<ul>
	<li>state: {@link #getName() name}, {@link #getParentNode() parent}, {@link #bbox}, {@link #baseline}, {@link #align}, {@link #valign}, {@link #floats}
	<li>tree manipulation operations: {@link #remove()},
	<li>tree navigation operations: {@link #getParentNode()}, {@link #getNextNode()}, {@link #getPrevNode()}, {@link #getFirstLeaf()}, {@link #getLastLeaf()},
	<li>general tree operations: {@link #commonAncestor(Node)}, {@link #cmp(Node, int, Node, int, INode)},
		{@link #findDFS(String, String, String, int)}, {@link #findBFS(String, String, String, int)}
	<li>marking of format-dirty bit: {@link #isValid()}, {@link #setValid(boolean)}, {@link #markDirty()}, {@link #markDirtySubtree(boolean)}, {@link #markDirtyTo(Node)}
	<li>parallels to {@link Behavior}'s protocols:
	<li>translation between relative and absolute coordinate systems: {@link #dx()} and {@link #dy()}, {@link #getAbsLocation()}, {@link #getRelLocation(Node)}
	<li>{@link #addObserver(Behavior) observers}
	<li>convenience hooks to {@link #getBrowser() Browser}, {@link #getRoot() Root}, {@link #getDocument() Document}, {@link #getIScrollPane() IScrollPane}.
	<li>for efficient operation, spans keep {@link #getSticky(int) stickies}, or summary information in leaves
	</ul>

<!--
	Maybe should implement W3C's DOM2 interface, but it's not so great.	It mixes internal nodes and leaves,
	has inefficient child access, a hardcoded list of node types (including attributes, which we don't consider
	nodes) that limits new document types, would require adding inefficienies (such as pointers from attributes to nodes)
	and introduces namespaces, which we don't implement yet -- yet there's all that JavaScript.
-->

<!--
<P>During the build phase, behaviors are passed handles to the user
interface and document trees.  Behaviors can augment, and with care,
otherwise modify these trees.
-->

<!--
  To do
  change some static methods to require valid node?  then caller has to check non-null...
  NODES SHOULD BE BEHAVIORS TOO!
  carefully define classes: instance variables, what's protected, *accessors*, functionality
  resort observers when reorder behaviors.	(rather than effectively so every time walk tree)

  NOTES
  structure hacking nondestructive, 'cause too hard to attach locations otherwise
  each layer stores old root, supplies new one... or keeps its own root to report to others asking for attachment points
	   can also do multiple views this way by hacking bbox
	   as side benefit, greater efficiency when rebuilding as now only rebuild from changed layer up
-->

	@see multivalent.MediaAdaptor
	@see multivalent.node.Root
	@see multivalent.Document
	@see multivalent.Behavior

	@version $Revision: 1.16 $ $Date: 2003/06/02 05:09:28 $
 */
public abstract class Node extends VObject implements Cloneable {//,ContextListener {//, org.w3c.dom.Node {
	private static final boolean DEBUG = false;


	/**
	Associated {@link multivalent.std.VScript} script.
	@see multivalent.gui.VButton
	 */
	public static final String ATTR_SCRIPT = "script";

	public static final String ATTR_ID = "id";


	/** Alignment. */
	public static final byte ALIGN_INVALID=-1,
	NONE=0, INHERIT=1,
	LEFT=10, RIGHT=11, BOTH=12, FILL=12, JUSTIFY=12, CENTER=13, CHAR=14,
	//TOP=0, BOTTOM=1, MIDDLE=2, BASELINE=3;	// valign
	TOP=20, BOTTOM=21, MIDDLE=22, BASELINE=23;	// valign -- different values than horizontal because HTML mixes in align attribute, so don't know if LEFT or TOP

	/**
	Some layouts ask contents how big they'd like to be, and some content will take all they can get,
	as when centering or HTML HR.  If passed layout width > PROBEWIDTH<b>/2</b> (so not folled by chipped off margins/borders/padding/general fuzz),
	then be nice and take little.
	Used by HTML TABLE.
	 */
	public static final int PROBEWIDTH = Integer.MAX_VALUE / 10000;/* divide by max num of cols = max average width of individual col */

	protected static final int MAXNONSTRUCTDEPTH=25;



	/**
	Name of node, like PARA or SECT for structural nodes, ASCII translation for leaves.
	Non-textual leaves should put something sensible here, e.g., HTML's ALT for images.
	Media adaptor may intern name String, but such is not guaranteed by this class.
	Access with getName()/setName().
	 */
	protected String name_;

	/** Parent node in tree - access with {@link #getParentNode()}. */
	protected INode parent_;	// X managed by Node's exclusively -- invisible to clients

	/**
	Bounding box, in pixels, location relative to parent node.
	Bounding box of internal node encloses all children.
	 */
	public /*final--clone()*/ Rectangle bbox = new Rectangle();	// LATER: incorporate as short x 3 + int for y (halves object allocation, save 6 bytes + object handle)
	// usually OK if local vars shadow these instance vars as parents set children and don't refer to own very often
	//short x, width;	// 2^15 enough at 600dpi enough for 54 inches wide
	//int y, height;	// tall documents (big height because top node contains whole document)

	/** Baseline, relative to top of bounding box. */
	public int baseline;	// LATER: byte would almost be ok, short definitely
	/** Justification (LEFT, RIGHT, CENTER, FILL/JUSTIFY) -- set with style sheet if possible. */
	public byte align=Node.NONE;	// could put align, valign, floats all in one byte, but would have to mask all the time to save just a couple bytes
	/** Vertical alignment -- set with style sheet if possible. */
	public byte valign=Node.NONE;
	/** If floating object, side (LEFT or RIGHT) -- set with style sheet if possible. */
	public byte floats=Node.NONE;
	//public boolean floating=false;	// set to true iff can and are now floating (left or right) -- that is, support floating in primordial core

	/**
	Behaviors that have registered interest in this node.
	For each phase, these behaviors have their <i>phase</i>Before and <i>phase</i>After methods called
	every time the tree walk for that protocol traverses the node.
	<!--
	if gets to be performance taxing, which I doubt it will, change this to
	parallel arrays of Behavior, protocol interest flags (in an int), Object arg
	=> no need since few observers on any one node
	-->
	 */
	protected List<Behavior> observers_ = null;

	/** List<> of marks on leaves, subtree summaries on internal nodes.  Not for general use. */
	protected Mark[] sticky_ = null;
	protected short stickycnt_ = 0;	// if sticky_==null, pay extra.  byte probably long enough, but pay extra byte to guard against perverse

	/** Layout dirty bit.  True after formatting, false if need to be formatted before next painting. */
	protected boolean valid_ = false;


	/**
	Deep copy of Rectangle; observers and sticky set to null.
<!--	Observers and sticky should be nulled in either original or copy (not clear()'ed as original and copy point to same Vector for these two). -->
	Need to clone when edit and split a Node.
	 */
	/*public?*/@Override
	protected Object clone() throws CloneNotSupportedException {
		Node n = (Node)super.clone();	// low-level bit copy
		// OK as it: name_, parent_ (not among parent's children), baselin, align, valign, floats, valid
		n.bbox = new Rectangle(bbox);
		n.observers_=null;	//List<> obs=n.observers_; if (obs!=null) { obs=new ArrayList<>(obs.size()); obs.addAll(observers_); }
		n.stickycnt_ = 0;
		n.sticky_=null;	//List<> stick=n.sticky_; if (stick!=null) { stick=new ArrayList<>(stick.size()); stick.addAll(sticky_); }
		//observers_ = null; -- user decides original or copy
		//sticky_ = null; -- user decides original or copy

		if (attr_!=null) {
			n.attr_ = new CHashMap<Object>(attr_.size()*2);
			attr_.putAll(n.attr_);
		}

		return n;
	}


	/**
	Create a node with the given name, attributes and parent, any of which can be null.
	A null name is used for non-structural internal nodes, null attributes are the same
	as an empty set but use less memory, and a null parent can be set later with INode.appendChild().
	Map attributes object is not cloned.

	@see multivalent.INode
	 */
	public Node(String name, Map<String,Object> attrs, INode parent) {
		//assert ... all params can be null

		setName(name);
		attr_ = attrs;	// should clone?  usually come from ESISNode, in which case we want to steal.
		if (parent!=null) parent.appendChild(this);
	}

	/**
	Transfers content into passed Node, destroying original and replacing it in tree.
	Current node becomes invalid and should not be used subsequently.
	 */
	public void morphInto(Node l) {
		assert l!=null && isLeaf() && l.isLeaf() || isStruct() && l.isStruct(): "Nodes not same type: "+getClass().getName()+" vs "+(l!=null? l.getClass().getName(): null);

		l.name_ = name_; name_=null;
		l.attr_ = attr_; attr_=null;
		l.bbox = bbox; bbox=null;
		l.baseline = baseline;
		l.align = align;
		l.valign = valign;
		l.floats = floats;
		l.observers_ = observers_; observers_=null;
		if (l.isLeaf()) {
			Leaf leaf = (Leaf)l;
			for (int i=0,imax=stickycnt_; i<imax; i++) sticky_[i].leaf = leaf;	// Mark's point to new Node
		}
		l.sticky_ = sticky_; sticky_=null;
		l.stickycnt_ = stickycnt_; stickycnt_=0;
		for (int i=0,imax=l.sizeSticky(), size=l.size(); i<imax; i++) { Mark m = l.getSticky(i); if (m.offset > size) m.offset=size; }	// LeafUnicode => LeafOcr w/size=1
		l.valid_ = valid_; valid_=false;
		if (parent_!=null) parent_.setChildAt(l, childNum()); else l.parent_=null;	// parent points to new Node
	}

	/** DOM2 nomenclature -- since method on a Node, why not "getName()"? */
	public final String getNodeName() { return getName(); }

	@Override
	public final/*?*/ String getName() { return name_; }
	public abstract void setName(String name);

	/** Return node bounding box.  (Not a copy.) */
	public Rectangle getBbox() { return bbox; }

	/*  public int getBaseline() { return baseline; }
  public void setBaseline(int baseline) {
	//assert baseline>=0; ?
	baseline_ = baseline;
  }

  public byte getAlign() { return align_; }
  public void setAlign(byte align) {
	align_ = align;
  }

  public byte getVAlign() { return valign_; }
  public void setVAlign(byte valign) {
	assert TOP<=valign && valign<=BASELINE;
	valign_ = valign;
  }

  public byte getFloats() { return floats_; }
  public void setFloats(byte floats) {
	assert LEFT==floats || RIGHT==floats || NONE==floats;
	floats_ = floats;
  }*/


	/**
	Number of addressable components in node: number of children in INode,
	number of letters in text leaf, zero for invisible nodes such as comments,
	and otherwise usually one (the default).
	 */
	public int size() { return 1; }

	public boolean breakBefore() { return false; }
	public boolean breakAfter() { return false; }

	public boolean isLeaf() { return !isStruct(); }
	public boolean isStruct() { return !isLeaf(); }

	/** DOM2 nomenclature (why not "getParent()"?). */
	public final INode getParentNode() { return parent_; }

	/** Used by IScrollPane to point scrollbars at it without it pointing at scrollbar.  Not for general use. */
	public void setParentNode(INode p) { parent_=p; }	// no setParent -> insertChildAt and other methods in INode


	/** Needed by Document to do clipboardBeforeAfter on root before selection.	Not for general use. */
	public final List<Behavior> getObservers() { return observers_; }

	/**
	Observers get called when node is encountered in tree walk protocols (format, paint, low-level event),
	<i>protocol</i>Before and <i>protocol</i>After methods.
	Patterned after java.util.Observable where applicable.
	Observers not sorted in priority order, as they don't necessary have a priority (implements ContextListener), though they probably should be sorted.
	At most one instance of a behavior be in the set of observer on a node at a time.
	@see java.util.Observable
	 */
	public final void addObserver(Behavior be) {
		assert be!=null /*&& (observers_==null || observers_.indexOf(be)==-1*/;

		if (observers_==null) observers_ = new ArrayList<Behavior>(5);
		if (observers_.indexOf(be)==-1) observers_.add(be); //Context.priorityInsert(be, observers_); //observers_.add(be);	// sort after build phase
	}
	public void deleteObserver(Behavior be) {
		if (observers_!=null) observers_.remove(be);
	}


	/** Chains up parent links.  @return null if none. */
	public Browser getBrowser() { Root root=getRoot(); return root!=null? root.getBrowser(): null; }
	/** Chains up parent links.  @return null if none. */
	public Root getRoot() { INode p=getParentNode(); return p!=null? p.getRoot(): null; }
	/** Chains up parent links.  @return null if none. */
	public IScrollPane getIScrollPane() { INode p=getParentNode(); return p!=null? p.getIScrollPane(): null; }
	/** Chains up parent links until find (lowest enclosing) Document.  @return null if none. */
	public Document getDocument() { INode p=getParentNode(); return p!=null? p.getDocument(): null; }


	/** X-coordinate transformation needed when entering this node's relative coordinate system. */
	public abstract int dx();
	/** Y-coordinate transformation needed when entering this node's relative coordinate system. */
	public abstract int dy();


	/** Nodes say whether it wants to see activity in rectangular region, regardless of bbox. */
	public boolean intersects(Rectangle r) {
		assert r!=null;
		return bbox.intersects(r);
	}

	/** Nodes say whether it wants to see activity at point, regardless of bbox. */
	public boolean contains(Point p) {
		assert p!=null;

		return bbox.contains(p);
	}

	public boolean contains(Node n) {
		return this == n;
	}

	public Mark getSticky(int inx) { return sticky_[inx]; }
	public int sizeSticky() { return stickycnt_; }

	public int indexSticky(Mark m) {
		assert m!=null;

		for (int i=0,imax=stickycnt_; i<imax; i++) if (m==sticky_[i]) return i;
		return -1;
	}

	public void removeSticky(Mark m) { removeSticky(indexSticky(m)); }
	public void removeSticky(int inx) {
		assert inx==-1 || inx>=0 && inx<stickycnt_;

		if (inx!=-1/*>=0 && inx < stickycnt_*/) {
			System.arraycopy(sticky_,inx+1, sticky_,inx, stickycnt_-inx-1);
			//for (int i=inx+1,imax=stickycnt_; i<imax; i++) sticky_[i-1]=sticky_[i];	// move everybody else over
			//if (n.stickycnt_==0) n.sticky_=null;	// saves space, good on Leaf, but wastes time in INode during creation and build and destory
			sticky_[--stickycnt_]=null;	// prevent memory leak
			assert stickycnt_ >= 0;
		}
	}

	public void addSticky(Mark m) { addSticky(m,true); }
	public void addSticky(Mark m, boolean sequence) {
		assert m!=null;

		if (sticky_==null) {
			sticky_ = new Mark[3];	// grows: 2,4,8,... time vs. space -- measure
			sticky_[0] = m;
			stickycnt_ = 1;

		} else {
			assert indexSticky(m)==-1: "already have sticky "+m+"  /  "+m.getOwner();

			if (stickycnt_==sticky_.length) { Mark[] newstick=new Mark[stickycnt_*2]; System.arraycopy(sticky_,0, newstick,0, stickycnt_); sticky_=newstick; }

			if (sequence) {
				int off=m.offset; // if (off>=0 && off<=size()) -- don't enforce this here as can have inconsistent states during editing
				assert stickycnt_ >= 0;
				for (int i=stickycnt_-1; ; i--)
					if (i==-1 || off >= sticky_[i].offset) {
						i++;
						if (i<stickycnt_) System.arraycopy(sticky_,i, sticky_,i+1, stickycnt_-i);	// move over
						//System.out.println("insert "+this+" @ "+i);
						sticky_[i] = m;	// insert new
						break;
					}
			} else
				//assert stickycnt_>=0 && stickycnt_<sticky_.length: "stickycnt_="+stickycnt_+", sticky_.length="+sticky_.length;
				sticky_[stickycnt_] = m;	// add at cheapest place: the end

			stickycnt_++;
			assert stickycnt_ >= 0: getName();	// no rollover 65K to -1!
		}
	}



	/* *************************
	 *
	 * general tree manipulation
	 *
	 **************************/


	/** DOM2
  public Node cloneNode(boolean deep) {
	try {
	  Node n = (Node)super.cloneNode(deep);

	} catch (CloneNotSupportedException bad) {}
  }*/

	/**
	Simple remove node from parent.
	@see #removeTidy(INode)
	 */
	public void remove() {
		if (parent_!=null) parent_.removeChild(this);	//parent_=null; }	//-- nodes will do a repaint or something after remove
	}

	/**
	Remove node from tree tidily:
	don't leave behind empty INode, recursively up to <var>root</var>.
	@see #remove()
	 */
	public void removeTidy(INode root) {	// can't assume root same as getIScrollPane() (it's FixedI mediabox in PDF)
		remove();
		for (INode p = getParentNode(); p!=null && p.size()==0 && p!=root; p=p.getParentNode()) p.remove();
	}


	public final int childNum() {
		return parent_!=null? parent_.childNum(this): -1;
	}

	public final int structChildNum() {
		return parent_!=null? parent_.structChildNum(this): -1;
	}

	public abstract Leaf getFirstLeaf();
	public abstract Leaf getLastLeaf();

	/** Get the node immediately following in a depth-first tree walk, returning null if none. */
	public Node getNextNode() {
		Node n = null;	// next
		INode p = getParentNode();

		// go up while rightmost child
		for (n=this; p!=null && n==p.getLastChild(); n=p, p=p.getParentNode()) {}

		// hop to right sibling and descend left
		// if p==null, already at last child of entire tree
		int nposn = n.childNum();
		return p!=null && nposn+1<p.size()? p.childAt(nposn+1): null;
	}

	/** Get the node immediately previous in a depth-first tree walk, returning null if none. */
	public Node getPrevNode() {
		Node n=null; INode p=getParentNode();
		for (n=this; p!=null && n.childNum()==0; n=p, p=p.getParentNode()) {};	// dual of nextNode
		int nposn = n.childNum();
		return p!=null && nposn>0? p.childAt(nposn-1): null;
	}

	/**
	Get the following leaf in a left-to-right traversal, returning null if none.
	Same as getNextNode().getFirstLeaf() plus a null check.
	Easy to collect the content of a span with this method.
	 */
	public Leaf getNextLeaf() { Node n = getNextNode(); return n!=null? n.getFirstLeaf(): null; }

	/**
	Get the previous leaf in a left-to-right traversal, returning null if none.
	Same as getPrevNode().getLastLeaf() with null check.
	Easy to collect the content of a span with this method.
	 */
	public Leaf getPrevLeaf() { Node n = getPrevNode(); return n!=null? n.getLastLeaf(): null; }



	public Node commonAncestor(Node b) { return commonAncestor(b, null); }

	/**
	@param top - guaranteed common ancestor of ln and rn, e.g., some Document, or null if unknown.
	@return null iff nodes not in same tree
	 */
	public Node commonAncestor(Node b, Node top) {
		//assert ... nulls ok

		// fast paths
		//if (this==null) return null;	// can't happen
		if (b==null) return null;	// bad
		if (this==b) return this;	// same node
		INode tp=getParentNode(), bp=b.getParentNode();
		if (this==bp) return this; else if (tp==bp || tp==b) return tp;	// same parent or one parent of other

		//System.out.print("common ancestor of "+a+" and "+b+"\n\t");
		List<Node> path = new ArrayList<Node>(20);
		for (Node n=this; n!=null && n!=top; n=n.getParentNode()) path.add(n);	// make sure to add 'top'
		for (Node n=b; n!=null; n=n.getParentNode()) if (path.indexOf(n)!=-1) return n;

		return null;	// this is pretty bad: not even in same tree!
	}


	/**
	Does first (node,offset) come before (-1), at (0), or after (1) second (node,offset)?
	Also returns 0 if two nodes are not in same subtree and so are incomparable, or one node is null.
	Spans, especially the selection, use to swap endpoints if necessary to ensure that start comes before end.
	@param top - guaranteed common ancestor of ln and rn, e.g., some Document, or null if unknown.
	 */
	public static int cmp(Node ln, int lo, Node rn, int ro, INode top) {
		//assert ... nulls ok

		// fast paths
		if (ln==null || rn==null) return 0;	// incomparable
		if (ln==rn) return lo==ro? 0: lo<ro? -1: 1;	// same node
		INode lp=ln.getParentNode(), rp=rn.getParentNode();
		if (lp==rp) return ln.childNum() < rn.childNum()? -1: 1;	// same parent (common)
		if (lp==rn) return -1; else if (ln==rp) return 1;	// freakish: incomparable but report consistent order


		//System.out.println("cmp "+ln.getName()+" vs "+rn.getName());
		//System.out.println("path:");
		// general case: different parents
		List<Node> cmppath = new ArrayList<Node>(50);

		// would like to use commonAncestor, but need to know about previous leaves along path
		// for l, climb to root (path inclusive of l because want to guarantee nonempty paths)
		for (Node n=ln/*.getParentNode() would be slightly faster*/, endn=top!=null? top.getParentNode(): null; n!=endn; n=n.getParentNode()) cmppath.add(n);

		// climb up r until intersect with l's path
		for (Node n=rn, p=n.getParentNode(); p!=null; n=p, p=p.getParentNode()) {
			//for (int i=1; i<cmpi; i++) if (p==cmppath[i]) return (cmppath[i-1].childNum() < n.childNum()? -1: 1);
			int inx = cmppath.indexOf(p);
			if (inx!=-1)
				return cmppath.get(inx-1).childNum() < n.childNum()? -1: 1;
		}

		//assert false: "Node.cmp() on nodes in different trees: "+ln+" and "+rn;
		return 0;	// shouldn't happen!
	}



	/* *************************
	 *
	 * PROTOCOLS -- should abstract commonalities of format, paint, select
	 *
	 **************************/

	/**
	In the depth-first tree traversal to format tree,
	Before and after methods of behavior observers invoked here; actual formatting passed on to formatNode().
	In documents with layed out content, children set their dimensions and parents set their location.
	In documents with absolutely positioned content, usually leaves position themselves and internal nodes compute relative coordinates.
	If node is valid, its entire subtree is assumed formatted and this method returns without doing anything.

	<!--
	LATER: parallel physical layout trees rather that on structure tree, for more flexible layout and multiple views.
	LATER: pass in List<> of flow regions (Dimension's), cut out what you need, protocol to ask for more (new column or page).
	pass Graphics2D object so can get FontMetrics and maybe other things?
	-->
	Behavior observers return <tt>true</tt> to shortcircuit traversal of subtree, which this method on Node passes back up the tree.

	@param width of screen, or fraction thereof available to be taken by node
	@param height of screen, or fraction thereof available to be taken by node (largely ignored in HTML, which has infinitely long scroll, except in FRAME)

	@see multivalent.Behavior
	 */
	public boolean formatBeforeAfter(int width, int height, Context cx) {
		//System.out.println(width+" x "+height);
		//if (width<0 || height<0) { new Exception().printStackTrace(); System.exit(1); }
		//assert width>=0 && height>=0 && cx!=null: "width="+width+", height="+height+", Context="+cx+" @ "+getName();
		//if (width<0 || height<0) setBounds(0,0, 0,0); -- ?

		int i=0, imax=0;
		boolean shortret = false;

		// prehandlers for behaviors that have registered interest
		if (observers_!=null) for (imax=observers_.size(); i<imax; i++)
			if ((observers_.get(i)).formatBefore(this)) break;	// true here jumps to low->high at this point

		// if no shortcircuit, format self
		// true if didn't format all children
		if (/*no shortcircuit*/i==imax) {
			shortret = formatNode(width, height, cx);
			i--;	// set proper starting index for low->high
		}

		// posthandlers for behaviors that have registered interest
		// formatAfter has available valid bbox
		// but if it changes move/resize, it's responsible for updating bbox
		for (/* start from same behavior you left off */; i>=0; i--)
			if (observers_.get(i).formatAfter(this)) break;	// true here returns immediately

		return shortret;
	}

	/** Override this to specialize the natual layout of a node. */
	public abstract boolean formatNode(int width, int height, Context cx);

	/**
	High performance reformatting of subtree.
	Edits to the document tree need to reformat the tree to update the display.
	Ordinarily this is quite efficient.  The path from the change to the root is marked dirty,
	all changes are batch together in a format that happens at the next paint,
	and during reformatting nodes that are still clean are not reformatted.

	<p>However some applications, such as interactive editing and the display of status messages,
	need the best possible performance.  If the range of changes can be bounded in some subtree,
	such that if the dimensions (width and height) of the subtree remain the same after reformatting,
	then reformatting can be speeded up.
	This rules out floats and HTML tables that compute cell widths based on the needs of their contents.
	(If the new dimensions are not the same, then
	fall back to the above guaranteed correct but possibly slower reformatting.)

	<p>Subclasses override this method for correctness for that node or for greater performance.
	An HTML table may compute cell widths based on the relative needs of cell contents,
	and so for correctness, in general, needs a complete reformat of the table.
	{@link multivalent.node.IParaBox} can improve performance by skipping over
	previous words in the paragraph, and stop reformatting if the change fits on the same line.

<!--
	<p>subclasses
	new dimensions same or smaller as old
	don't use if want to batch changes

	uses old width and height in formatting new,
	so if one of those formats to a fraction of incoming,
	reformat on parent instead.
-->

	<p>To use, <b>first {@link #setValid(boolean)} with <code>false</code> on node that contains all the changes</b>,
	then make changes in the subtree, and then invoke this method on that node.

	@param smallerok  set to <code>true</code> if reformatting should be considered complete
	if the dimensions of the new subtree are smaller, in addition to the case where they are exactly the same.

	 */
	public void reformat(Node bogus/*, boolean smallerok*/) {
		assert getParentNode()!=null && !getParentNode().isValid(): "should mark node setValid(false) before modifying contents of subtree / reformat one subtree at a time -- don't use if want to batch";

		// save old dimensions
		int owidth=bbox.width, oheight=bbox.height;

		// reformat
		Document doc = getDocument();
		Context cx = doc.getStyleSheet().getContext();
		cx.reset(this, -1);
		formatBeforeAfter(owidth,oheight/*Integer.MAX_VALUE*/, cx);

		// new dimensions same? (or, with flag, smaller?)
		//boolean done = (smallerok? bbox.width<=owidth && bbox.height<=oheight: bbox.width==owidth && bbox.height==oheight);
		boolean done = bbox.width==owidth && bbox.height==oheight;
		if (cx.getFloatWidth(Node.BOTH) > 0) done = false;	// check for initiated but not completed floats (earlier floats reflected in different dimensions)
		if (done) repaint(15);
		//else { setValid(false); INode p = getParentNode(); if (p!=null) p.reformat(this); }	// diminishing returns to chaining
		else {
			markDirty();	// fall through to usual way
			getParentNode().getDocument().repaint();	// who knows how much may have changed
		}
	}



	/*
	 * PAINT -- clipping rectangle in absolute coordinates, compute absolute from relative as we go along
	 */
	/**
	Depth-first tree traversal to paint tree.
	Before and after methods of observers invoked here; actual painting passed on to paintNode().
	Leaves actually do painting; parents just iterate over children.
	Efficiently redraws just area overlapping clipping rectangle.

	<p>Subclasses should check to see that node overlaps clip, and if so, update origin and clip, call super.paintBeforeAfter() (which will call paintNode()), then restore origin and clip.
	Determine if node within the current clipping region;
	if so, translate origin and clip, call observers (paintBeforeAfter), paint self (and children if any), restore origin and clip.
	Classes INode and Leaf do that, and almost nodes subclasses them, so subclasses can freely
	override paintNode (by INode's) and paintNodeContent (by Leaf's).
	Use Graphics.translate() and Graphics.setClipBounds() in place
	of any temptation to use Graphics.create() as sometimes new Graphics objects are substituted
	and Graphics.create() doesn't copy the tweaks.

	<li>Translate Graphics and clipping rectangle for node's (x,y) and margin (margins)
	in same coordinate space as node, with same graphics context
	Invariant: Graphics and cliprect set correctly at paintBeforeAfter().
	INode changes according to child's bbox and own margin in paintNode.


	<p>LATER: parallel physical layout trees rather that on structure tree, for more flexible layout and multiple views.
	LATER: pass in List<> of flow regions (Dimension's), cut out what you need, protocol to ask for more (new column or page).
	@return true to shortcircuit traversal of subtree.
	 */
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		// check to see if should paint done by subclass, probably INode or Leaf

		// observers + content
		int dx=dx(), dy=dy();
		Graphics2D g = cx.g;
		g.translate(dx,dy);	// painting done in screen coordinates, so add in relative coordinate contribution
		//if (bbox.width==700) System.out.println("translate ("+dx+","+dy+")");
		docclip.translate(-dx,-dy);	// clipping checked in relative coordinates, so cut down screen to relative -- maybe shrink WxH too


		int i=0, imax=0;
		// prehandlers
		if (observers_!=null) for (imax=observers_.size(); i<imax; i++) {
			Behavior be = observers_.get(i);
			if (be.paintBefore(cx, this)) break;
		}

		// if no shortcircuit, format self
		if (i==imax) {
			/*		//System.out.println("paint on "+b);
		List<> v = cx.be_everywhere;
		for (int j=0,jmax=v.size(); j<jmax; j++) ((Behavior)v.get(j)).paintBefore(g, cx, this);*/
			paintNode(docclip, cx);
			/*		for (int j=v.size()-1; j>=0; j--) ((Behavior)v.get(j)).paintAfter(g, cx, this);*/
			i--;
		}

		// posthandlers
		for ( ; i>=0; i--) {
			Behavior be = observers_.get(i);
			if (be.paintAfter(cx, this)) break;
		}

		//if (bbox.width==700) System.out.println("translate (-"+dx+",-"+dy+")");
		g.translate(-dx,-dy); docclip.translate(dx,dy);
	}

	/**
	 */
	public abstract void paintNode(Rectangle docclip, Context cx);


	/*
	 * SELECT
	 */
	/**
	Depth-first tree traversal to build selection.
	Before and after methods invoked here; actual selection passed on to clipboardNode().
	 */
	public final void clipboardBeforeAfter(StringBuffer txt) {
		if (bbox.width==0 || bbox.height==0) return;	// don't include invisible regions in selection

		int i=0, imax=0;
		if (observers_!=null) for (imax=observers_.size(); i<imax; i++)
			if (observers_.get(i).clipboardBefore(txt, this)) break;

		if (i==imax) {
			clipboardNode(txt);
			i--;
		}

		for (/* start from same behavior you left off */ ; i>=0; i--)
			if (observers_.get(i).clipboardAfter(txt, this)) break;
	}

	/**
	To build up selection, pass a StringBuffer to media-specific leaves,
	which fill it as appropriate for that medium.
	Need to fix this to respect subelement addressing
	 */
	public abstract void clipboardNode(StringBuffer txt);


	/*
   Depth-first tree traversal to build selection.
   Before and after methods invoked here; actual selection passed on to clipboardNode().
	 */
	/*final--should make this final again, but conflicts with VScrollbar's being an EventListener */
	/**
	Pass tree event.  Translates coordinates to relative, calls observers, untranslates coordinates.
	 */
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		assert e!=null /*&& rel!=null -- ok*/;

		if (!isValid()) return false;
		int i=0;
		boolean shortcircuit = false;

		int dx=dx(), dy=dy();
		// assert coordinates at entry same as at exit
		if (rel!=null) rel.translate(-dx,-dy);	// cut down as enter relative coordinates <=> increase relative running total
		// invariant: node and observers operate within local coordinate space: (0,0) is at node's upper left (exclusive of margin, border, padding on INode -- refers to content region)

		// prehandlers for behaviors that have registered interest
		//Behavior be1 = null;
		if (observers_!=null) for (int imax=/*(observers_==null/*||grab!=null=>want Magnify transformations?0:*/observers_.size(); i<imax; i++) {
			Behavior be = observers_.get(i);
			//System.out.println("before "+be.getClass().getName());
			//if (i==1) be1=be;
			if (be.eventBefore(e, rel, this)) {
				shortcircuit=true;
				// if some runtime debugging flag set, report this.  try to integrate with doc tree display
				//System.out.println("shortcircuit on before: "+be.getName());
				break;
			}
		}

		if (!shortcircuit/*i==imax /*|| grab!=null*/ /*|| getParentNode()==null -- root non-maskable?*/) {
			shortcircuit = eventNode(e, rel);	// give node crack at it even if not in its bbox
			i--;
		}

		i = Math.min(i, (observers_==null?0:observers_.size()-1));	// can lose behavior, as due to Document's setCurDocument, which triggers ShowHeaders to move, which drops it from entering before's.  need something like Layer's semanticBefore/After nesting
		//if (i==1 && observers_.size()==1) System.out.println("problem case: @ "+getName()+" with "+be1.getName()+", short="+shortcircuit);
		for (/* start from same behavior you left off */ ; i>=0; i--) {
			Behavior be = observers_.get(i);
			//System.out.println("after "+b.getClass().getName());
			if (be.eventAfter(e, rel, this)) {
				shortcircuit=true;
				//System.out.println("shortcircuit on after: "+be.getName());
				break;
			}
		}

		if (rel!=null) rel.translate(dx,dy);

		return shortcircuit;
	}

	/**
	Process java.awt.Event or multivalent.TreeEvent (not multivalent.SemanticEvent).
	@see multivalent.TreeEvent
	@see multivalent.SemanticEvent
	 */
	public abstract boolean eventNode(AWTEvent e, Point rel);



	/* *************************
	 *
	 * tree management: dirty bit, getRelLocation, repaint, getActivesAt, find{DFS,BFS}
	 *
	 **************************/

	public final boolean isValid() { return valid_; }

	/**
	Set dirty bit <i>in this node only</i>.
	@see #markDirty()
	 */
	public final void setValid(boolean state) { valid_=state; }

	/**
	Mark dirty--setValid(false)--and chain of nodes up to lowest IScrollPane.
	Presently, formatting takes place top down from the root (unless you call reformat) immediately before painting,
	so to get formatted, use this to mark a dirty path <i>from the node up to the root</i> (up tree only, including self).
	If an INode needs to reformat all content too (as to reflow based on new width),
	as opposed to reformatting an affected subtree, also call markDirtySubtree().
	<!-- => need fully formatted tree in order to set scrollbar.  (Tk's text fakes scrollbar by countine hard lines.)
	Later, a dirty node will be formatted as encountered in the tree during paint,
	at which time this method will be unecessary.
	-->
	 */
	public final void markDirty() {
		// later still don't need at all as reformat as necessary when paint--or just check at IScrollPane?
		setValid(false);
		IScrollPane isp = getIScrollPane();	// don't need to go all the way to Root
		for (Node n=getParentNode(), stop=isp!=null? isp.getParentNode(): null; n!=stop && n.isValid(); n=n.getParentNode()) n.setValid(false);
		//for (Node n=this, stop=(isp!=null? isp.getParentNode(): null); n!=stop && n.isValid(); n=n.getParentNode()) n.setValid(false); => if self already dirty, don't stop there
	}

	//public void markDirtySubtree() { markDirtySubtree(true); } -- force explicit decision about leaves
	/**
	Mark dirty all nodes in subtree and path to root (up and down tree).
	If leaf node dimensions are still valid, can save about half the reformatting time by not marking them dirty (pass <tt>false</tt>).
	When markDirty() is obsolete, kill and rename markDritySubtreeDown to this.
	 */
	public void markDirtySubtree(boolean leavestoo) {
		markDirty();
		markDirtySubtreeDown(leavestoo);
	}

	/** Mark dirty all nodes in subtree (down tree only). */
	public abstract void markDirtySubtreeDown(boolean leavestoo);

	/**
	Mark dirty all nodes in a span, parents included.  Used by Span but possibly generally useful.
	 */
	public void markDirtyTo(Node/*Leaf*/ rn) {
		assert rn!=null;

		for (Node n=this/*.getFirstLeaf()*/,endn=rn.getNextLeaf(); n!=null && n!=endn; n=n.getNextLeaf())
			//System.out.print(n.getName()+"  ");
			n.markDirty();	// most of these stopped immediately as parent is dirty too, after marking by first child in group
	}


	/**
	Redraw portion of node within <i>ms</i> milliseconds.
	Translates coordinates from relative to screen as ascend tree.
	Like java.awt.Component's repaint().
	 */
	public void repaint(long ms, int x, int y, int w, int h) {
		//if (!isValid()) { System.out.println(getName()+" not valid on repaint"); return; }	// already have pending paint
		INode p = getParentNode();
		//if (p!=null) p.repaint(ms, x+bbox.x,y+bbox.y, w, h);
		if (p!=null) p.repaint(ms, x+dx(),y+dy(), w, h);
	}
	public void repaint(int x, int y, int w, int h) { repaint(0, x,y, w,h); }
	public void repaint() { repaint(0); }
	/** Repaint node itself -- not in content coordinates. */
	public void repaint(long ms) {
		//repaint(ms, 0,0, bbox.width+1,bbox.height+1); => NOT THIS
		//repaint(ms, dx(),dy(), bbox.width+1,bbox.height+1); => AND NOT THIS
		INode p = getParentNode();
		if (p!=null) p.repaint(ms, bbox.x,bbox.y, bbox.width+1,bbox.height+1);
	}



	/** Determine location of node in absolute coordinates, as opposed to parent-relative. */
	public Point getAbsLocation() { return getRelLocation(null); }

	/** Determine location of node relative to passed node, as for instance location of image relative to its Document. */
	public final Point getRelLocation(Node relto) {
		//assert relto!=null -- ok

		Point pt = new Point(bbox.x, bbox.y);	// exclusive of internal weirdness represented by dx(),dy()
		//System.out.println("scroll to "+getName()+" +"+bbox.x+","+bbox.y);
		if (this!=relto)	// relative to self => no change
			for (Node p=getParentNode(); p!=null && p!=relto; p=p.getParentNode())
				//System.out.println("@ "+p.getName()+" +"+p.dx()+","+p.dy());
				pt.translate(p.dx(), p.dy());
		return pt;
	}


	/** Scroll to show the node on the screen. */
	public void scrollTo() { scrollTo(0,0, false); }

	/**
	Scroll to show the node on the screen.
	@see multivalent.IScrollPane for explanation of pickplace option.
	 */
	public void scrollTo(int dx, int dy, boolean pickplace) {
		IScrollPane sp = getIScrollPane();
		if (sp!=null) sp.scrollTo(this, dx,dy, pickplace);
		//System.out.println("Node scrollTo "+this);  //+" @ "+s.getRelLocation(s.getDocument()));
	}



	// => MOVE THIS TO Context AS COLLECTION SPANS, STRUCTURAL, AND AD HOC(?) FOR ALL ACTIVE BEHAVIORS AT POINT
	public List<ContextListener> getActivesAt(int offset) { return getActivesAt(offset, null, false); }
	public List<ContextListener> getActivesAt(int offset, boolean spansonly) {
		//assert offset>=-1 && offset<=size();

		return getActivesAt(offset, null, spansonly);
	}

	/**
	Collect active behaviors at point: spans, style sheet settings.
	Want to pick up formatting and painting at an arbitrary point, INode or Leaf,
	so have to establish graphics context anyplace.
	 */
	public /*synchronized*/List<ContextListener> getActivesAt(int offset, List<ContextListener> base, boolean spansonly) {
		assert offset>=-1 && offset<=size(): "offset="+offset;	// -1 ok too

		// HEAVILY CALLED, SO MAKE AS EFFICIENT AS POSSIBLE
		// cache results somehow? (the below not cached, just already allocated

		Document doc = getDocument();
		StyleSheet ss = doc.getStyleSheet();	// lowest style sheet is fully correct, as Documents are independent
		List<Span> spans = new ArrayList<Span>(20); List<Node> structs = new ArrayList<Node>(20);	// make big arrays so don't have to cast?

		// 1. Collect actives
		// Scan passed node, its left siblings, up parents (skipping immediate parents--whose summaries have been computed more accurately--collecting left siblings)

		// A. base
		// passed in as parameter

		// B. structural
		if (!spansonly)
			for (INode p=getParentNode(),pend=doc.getParentNode(); p!=null && p!=pend; p=p.getParentNode())
				structs.add(p);

		// C. Spans
		int ndelta=0;	// passed start node special case: it's processed
		Node n=this;
		for (INode p=getParentNode(),pend=doc.getParentNode(); p!=null && p!=pend; n=p, p=p.getParentNode(), ndelta=-1)
			// scan left across siblings, up to and exclusive of current node
			for (int i=n.childNum()+ndelta; i>=0; i--, ndelta=-1) {
				Node child = p.childAt(i);
				Mark[] stickies = child.sticky_;

				// mixed processing of leave and inodes because can have both at same level of tree
				if (stickies==null || child.sizeSticky()==0) {
					// common on INode (most spans started and ended on same node, or one up)
					// can be a few on leaves, but only process left siblings at one (usually) level

				} else if (child.isStruct())
					// transfer summaries over
					for (int j=ndelta==0? -1: child.sizeSticky()-1; j>=0; j--) {
						Mark m = stickies[j];
						Span s = (Span)m.getOwner();
						int inx=spans.indexOf(s); if (inx==-1) spans.add(s); else spans.remove(inx);
					}
				//spans.add(child);
				else { assert child.isLeaf();
				// scan left until done with node
				for (int j=child.sizeSticky()-1, size=size(); j>=0; j--) {
					Mark m = stickies[j];
					if (ndelta==0 && m.offset >/*NOT =*/ offset /*|| m.offset==size ???*/) continue;	// very first node only
					Object e = m.getOwner();
					if (e instanceof Span) {
						Span s = (Span)e;
						int inx=spans.indexOf(s); if (inx==-1) spans.add(s); else spans.remove(inx);
					}
				}
				}
			}


		// 2. Prioritize actives (+ copy into fresh, disposable ArrayList)
		// add root=>child so child highest priority on ties
		// should profile or keep stats on usual sizes
		List<ContextListener> actives = new ArrayList<ContextListener>((spans.size() + structs.size())* 2);

		// A. base
		if (base!=null && !spansonly) actives.addAll(base);	//  already in correct priority ordering

		// B. structural
		if (ss!=null) for (int i=structs.size()-1; i>=0; i--) {	// top down
			n = structs.get(i);
			ss.activesAdd(actives, n, n.getParentNode());
		}

		// C. spans
		for (int i=spans.size()-1; i>=0; i--) {	// backward to resolve ties to last added (also lower priority likely to be toward end and have less moving in priority list)
			Span span = spans.get(i);
			if (span.isSet()) {
				if (!spansonly && ss!=null) ss.activesAdd(actives, span, span.getStart().leaf.getParentNode());
				Context.priorityInsert(span, actives);
			}
		}

		return actives;
	}


	/*
	geoReplace - geometric-based tree hacking (mainly for pre-formatted formats)

	find nodes in tree that overlap with replacement text
	save child number of first overlap
	delete all overlaps
	insert new passed tree at saved child number
	public void geoReplace(Node nnode) {}
	can't do this in general since need to format to get positions, but build comes before format
	 */


	/* ***************************************
	 *
	 * find node
	 *
	 ****************************************/

	// given (x,y) find smallest enclosing region containing that point
	//	public abstract Node findDFS(Point rel);

	// given (x,y) find enclosing region of given type (node name)
	/*
  public Node findDFS(Point rel, String name) {
	// do findDFS(x,y), then climb up tree checking for right type
	Node n = findDFS(x,y);
	while (n!=null && !n.name_.equals(name)) n = n.getParentNode();
	return n;
  }*/

	/**
	Depth-first search for node with requested combination of name, attribute name, and attribute value.
	Set to <tt>null</tt> if don't care/match anything, e.g., set node name and attribute value to
	null if just want node to have some node with the given attribute.
	Setting attribute name to null and attribute value to non-null has the same effect as setting both to null.
	More efficient than {@link #findBFS(String)}.
	<!-- should this ignore null-named nodes? -->
	For example, this can be used to find node with given <tt>id</tt> attribute
	with the following <code>doc.findDFS(null, "id", <var>desired-id-value</var>, Integer.MAX_VALUE)</code>.
	 */
	public Node findDFS(String searchname, String attrname, String attrval, int maxlevel) {
		return findDFS(searchname, attrname, attrval, 0, maxlevel);
	}

	protected Node findDFS(String searchname, String attrname, String attrval, int level, int maxlevel) {
		assert searchname!=null || attrname!=null || attrval!=null: "must specify at least one of the search criteria";
		//if ((searchname==null || searchname.equals(name_)) && (attrname==null || attrval.equals(getAttr(attrname)))) return new Mark(this,-1);

		if (name_!=null
				&& (searchname==null || searchname.equals(name_))
				&& (attrname==null || getAttr(attrname)!=null)
				&& (attrval==null || attrname==null || attrval.equals(getAttr(attrname))))
			return this;

		//if (attrname==null && attrval!=null) ... iterate of attrs for value

		return null;
	}

	/** Depth-first search for Node with given name. */
	public final Node findDFS(String searchname) { return findDFS(searchname, null,null, Integer.MAX_VALUE); }

	/** Depth-first search for Node with given name and (name, val) attribute pair. */
	public final Node findDFS(String searchname, String attrname, String attrval) { return findDFS(searchname, attrname, attrval, Integer.MAX_VALUE); }


	/**
	Breadth-first search for node with given name (generic identifier), attribute name, and attribute value.
	Set parameter to <code>null</code> if don't care/match anything, e.g., set node name and attribute value to
	<code>null</code> if just want any node that has the given attribute.
	Setting attribute name to null and attribute value to non-null has the same effect as setting both to null.
	<!-- should this ignore null-named nodes? -->
	 */
	public Node findBFS(String searchname, String attrname, String attrval, int maxlevel) {
		assert searchname!=null || attrname!=null || attrval!=null: "must specify at least one of the search criteria";
		//if (attrname==null) attrval=null; -- make explicit check each time to match DFS
		//if (searchname==null && attrname==null && attrval==null) return this;   -- happens automatically

		int level = 0;
		LinkedList<Node> queue = new LinkedList<Node>();	// cheap to remove first element
		queue.add(this);
		while (queue.size()>0) {
			Node n = queue.removeFirst();	// malloc vs shifting

			if (maxlevel!=Integer.MAX_VALUE && n!=this && n == n.getParentNode().getFirstChild()) {	// on first child of set of children, compute level
				level = 0;
				for (Node nn=n; nn!=this; nn=nn.getParentNode()) level++;
				if (level >= maxlevel) break;
			}

			String name = n.getName();
			if (name!=null
					&& (searchname==null || searchname.equals(name))
					&& (attrname==null || n.getAttr(attrname)!=null)
					&& (attrname==null || attrval==null || attrval.equals(n.getAttr(attrname))))
				return n;

			if (n.isStruct()) {	// if internal, queue children
				INode p = (INode)n;
				for (int i=0,imax=p.size(); i<imax; i++) queue.add(p.childAt(i));
			}
		}
		return null;
	}

	/** Breath-first search for node with given name (generic identifier). */
	public final Node findBFS(String searchname) { return findBFS(searchname, null,null); }

	public final Node findBFS(String searchname, String attrname, String attrval) { return findBFS(searchname, attrname, attrval, Integer.MAX_VALUE); }


	/* ***************************************
	 *
	 * tree slices
	 *
	 ****************************************/


	// given two nodes, return list of everything inbetween,
	// version 1: of same type, version 2: with chunky (largest structural intermediate) insides
	public static Node[] spanChunky(Mark l, Mark r) { return spanChunky(l.leaf, r.leaf); }
	public static Node[] spanChunky(Node l/*eft*/, Node r/*ight*/) {
		assert l!=null && r!=null;

		//System.out.println("spanChunky "+l+".."+r);
		Node list[];
		if (l==r) { list=new Node[1]; list[0]=l; return list; }

		List<Node> vlist = new ArrayList<Node>(30);

		// for now assume l to the left of r.  when intersect can determine if need to swap

		// climb tree from l and r until find common ancestor
		List<Node> lpath = new ArrayList<Node>(10);	// could use templates
		List<Node> rpath = new ArrayList<Node>(10);
		Node n;


		//	INode intersect = (INode)Node.commonAncestor(l, r);
		INode intersect = (INode)l.commonAncestor(r);
		assert intersect!=null: "no intersection between start and end points of span!";


		// for l, climb to root (path inclusive of l because want to guarantee nonempty paths)
		for (n=l; n!=intersect; n=n.getParentNode()) lpath.add(n);

		// climb up r until intersect with l's path, then trim l's path
		for (n=r; n!=intersect; n=n.getParentNode()) rpath.add(n);

		// if r was to left of l, need to swap
		// maybe should return null in that case
		//INode common = (INode)n;	// intersection has to be internal node (l==r handled at entry)
		/*
	int li = intersect.children.indexOf(lpath.lastElement());
	int ri = intersect.children.indexOf(rpath.lastElement());
		 */
		/*	int li = ((Node)lpath.lastElement()).childNum();
	int ri = ((Node)rpath.lastElement()).childNum();*/
		int li = lpath.get(lpath.size()-1).childNum();
		int ri = rpath.get(rpath.size()-1).childNum();
		if (li > ri) {
			Node ntmp = l; l=r; r=ntmp;
			List<Node> vtmp = lpath; lpath=rpath; rpath=vtmp;
			int itmp = li; li=ri; ri=itmp;
		}

		// if at endpoint of region, chunk to that region
		//System.out.println("lpath is "+lpath);
		while (lpath.size()>1) {
			INode parent = (INode)lpath.get(1);
			if (l != parent.childAt(0)) break;
			l = parent;
			//System.out.println("left chunking up to "+l);
			lpath.remove(0);
		}

		//System.out.println("rpath is "+rpath);
		//System.out.println("last child is "+intersect.childAt(-1)+" vs "+r+", equal? "+(r==intersect.childAt(-1))+", #children="+intersect.children.size());
		while (rpath.size()>1) {
			INode parent = (INode)rpath.get(1);
			//System.out.println("last child is "+parent.childAt(-1)+" vs "+r+", equal? "+(r==parent.childAt(-1))+", child #="+parent.children.indexOf(r)+", # of children="+parent.children.size());
			if (r != parent.getLastChild()) break;
			r = parent;
			//System.out.println("right chunking up to "+r);
			rpath.remove(0);
		}

		//System.out.println("intersect is "+intersect+", first="+intersect.childAt(0)+", last="+intersect.childAt(-1));
		//System.out.println("l = "+l+"("+(l==intersect.childAt(0))+"), r="+r+"("+(r==intersect.childAt(-1))+")");
		if (l==intersect.childAt(0)  &&  r==intersect.getLastChild()) { list=new Node[1]; list[0]=intersect; return list; }

		// ASCEND LEFT path adding all subtrees to the right
		INode parent; Node onpath;
		vlist.add(l);
		onpath = l;
		for (int i=1; i<lpath.size(); i++, onpath=parent) {
			parent = (INode)lpath.get(i);
			for (int j=onpath.childNum()+1,jmax=parent.size(); j<jmax; j++)
				vlist.add(parent.childAt(j));
		}


		// add siblings between endpoints on path (that end just before intersecting)
		for (int i=li+1; i<ri; i++)
			vlist.add(intersect.childAt(i));


		// DESCEND RIGHT path adding all subtrees to the left
		onpath = rpath.get(rpath.size()-1);
		for (int i=rpath.size()-1; i>=1; i--) {
			parent = (INode)onpath;
			onpath = rpath.get(i-1);
			for (int j=0,jmax=onpath.childNum(); j<jmax; j++)
				vlist.add(parent.childAt(j));
		}
		vlist.add(r);


		// later, in different method: coalese bounding boxes: first line, to end of column, columns, end of last col to next to last line, start of last line to last word
		list = new Node[vlist.size()];
		//	vlist.copyInto(list);
		vlist.toArray(list);

		if (Node.DEBUG && false) {
			System.out.print("Chunky span\n\t");
			for (Node element : list)
				System.out.print(element.name_+"  "); System.out.println();
		}

		return list;
	}


	// span leaves in specified range
	// later create span for whole document, refreshing on rebuilds (called by Browser)
	// then just copy out excerpt rather than recursing, building up ArrayList, converting ArrayList
	// span all leaves -- make this return an enumeration?  not so bad as just returning array of pointers
	// cache last result, maybe clever if new request just slightly different
	/*
  public static Node[] spanLeaves(INode root) {
	Node left = root;
	while (left.isStruct()) left=((INode)left).childAt(0);
	return spanLeaves(root, left,0, null,0);
  }

  // later: respect offsets
  public static Node[] spanLeaves(Mark l, Mark r) {
	return spanLeaves(l.leaf,l.offset, r.leaf,r.offset);
  }
  public static Node[] spanLeaves(Node l/*eft* /, int lo, Node r/*ight* /, int ro) {
	Node n = l.commonAncestor(r);
	if (n.isStruct()) return spanLeaves((INode)n, l,lo, r,ro);
	else {
	  Node narray[] = new Node[1]; narray[0]=n;
	  return narray;
	}
  }

  public static Node[] spanLeaves(INode root, Node l/*eft* /, int lo/*ffset* /, Node r/*ight* /, int ro/*ffset* /) {
	// dive in from root, scan leaves left to right, start collecting at l, stop at r
	List<> vlist = new ArrayList<>(100);
	List<> nlist = new ArrayList<>(200);

	// scan depth first
	boolean collect = false;
	nlist.add(root);
	while (nlist.size()>0) {
	  Node node = (Node)nlist.remove(nlist.size()-1);

	  if (node==r && collect==false) { Node tmp=l; l=r; r=tmp; }
	  if (node==l) collect=true;
	  //System.out.println("spanning "+node.name_+", collect="+collect);

	  if (node.isStruct()) {
		INode parent = (INode)node;
		for (int j=parent.size()-1; j>=0; j--) {
		  Node child = parent.childAt(j);
		  // can do contains method because Stack subclasses ArrayList
		  // actually, no loops so don't need to check
		  //if (!nlist.contains(child)) nlist.add(child);
		  nlist.add(child);
		}
	  } else {
		if (collect) vlist.add(node);
	  }

	  if (node==r) break;	//collect=false;
	}

	Node list[] = new Leaf[vlist.size()];
//	vlist.copyInto(list);
	vlist.toArray(list);

	return list;
  }
	 */


	/*
  public String getNodeValue() throws DOMException {}
  public void setNodeValue(String nodeValue) throws DOMException {}

  public short getNodeType();
  public void normalize() {}

  public boolean isSupported(String feature, String version) {
  }

  /** DOM2: Not implemented. * /
  public String getNamespaceURI() {}
  /** DOM2: Not implemented. * /
  public String getPrefix() {};
  /** DOM2: Not implemented. * /
  public void setPrefix(String prefix) throws DOMException {};
  /** DOM2: Not implemented. * /
  public String getLocalName() {};
	 */
	public Node getElementById(String elementId) {
		return elementId!=null && elementId.equals(getAttr(Node.ATTR_ID))? this: null;
	}


	/* *************************
	 * DEBUGGING
	 **************************/

	@Override
	public boolean checkRep() {
		assert super.checkRep();

		assert valid_: getName();
		assert parent_!=null && parent_.childNum(this)>=0;	// checkable outside of tree?
		//assert parent_.bbox.contains(bbox); => ok to lie outside of parent bbox
		assert bbox.width>=0 && bbox.height>=0: bbox.width+"x"+bbox.height;
		assert isStruct() && this instanceof INode || isLeaf() && this instanceof Leaf;	// all nodes must subclass either INode or Leaf (or subclasses thereof), not immediate subclass of Node
		assert stickycnt_ >= 0;

		return true;
	}


	public void dump() { dump(0, Integer.MAX_VALUE); }
	public void dump(int maxlevel) { dump(0, maxlevel); }
	/** Dump more verbose than toString(). */
	public void dump(int level, int maxlevel) {
		System.out.print(level); for (int i=0,imax=level*2; i<imax; i++) System.out.print(" ");
		System.out.println(
				("img".equals(name_)? getAttr("src"): name_)
				+"/"+childNum()
				//		+", bbox="+bbox
				//		+ ", nextLeaf = "+getNextLeaf()
				+", class="+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1)
				+", bbox="+bbox.width+"x"+bbox.height+"@("+bbox.x+","+bbox.y+")/"+ baseline
				+(sticky_==null? "": ", sticky="+sticky_)
				//		+", owner="+owner
				//		+", parent="+parent_
				//		+", baseline="+baseline
				+(valid_? "": ", valid=false")
				//		+", observers="+observers_
				+(attr_==null? "" : ", attrs cnt="+attr_.size())
				+", "+attr_
		);
	}

	@Override
	public String toString() { return getName(); }
	//return "Node: "+getName(); }
	//"|"+name_+"|  "+getClass().getName()+"	"+bbox+", observers="+observers_+", valid="+valid_; }

}
