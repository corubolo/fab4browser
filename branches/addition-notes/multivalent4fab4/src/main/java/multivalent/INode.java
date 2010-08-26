package multivalent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import multivalent.node.LeafZero;



/**
	Internal nodes have children.  To implement most protocols, pass protocol on to children.
	Superclass for all internal--that is, structural--nodes in the doc tree.

	<p>Bounding box contains border, padding, and content, but not margin, which is taken into account in (x,y) location.

<!--
	(for shadow doc tree use same class, just have multiple pointers to shared state
	-- good things there's gc to handle when the target objects are freed)
	guarantee: all internal line nodes have at least one child

	<p>I think there is too much policy in this class.  Should have subclasses in multivalent.Node and keep this one flexibly overrideable.
-->

	@see multivalent.Leaf
	@see multivalent.Span

	@version $Revision: 1.10 $ $Date: 2003/06/02 05:06:47 $
 */
/*abstract*/ public class INode extends Node {
	//static int bgcnt_=0;

	/**
	Canonical Insets with equal bounds to use to save space over creating new Insets for these common cases.
	For example, INSETS[5] has top == bottom == left == right == 5.
	<em>Treat all Insets as read-only.</em>
	 */
	public static final Insets[] INSETS = new Insets[50+1];
	static { for (int i=0,imax=INode.INSETS.length; i<imax; i++) INode.INSETS[i]=new Insets(i,i,i,i); }
	/** Special name, points to INSETS[0]. */
	public static final Insets INSETS_ZERO = INode.INSETS[0];

	/** Internal array of children -- access through appendChild()/removeChild()/.... */
	private Node[] children_ = new Node[10];	// LATER: average children/node much lower with larger leaves
	//protected List<Node> children_ = new ArrayList<Node>(10);	=> manage own extensible array so save on method calls and type casts

	/** Internal count of children -- access with size(). */
	private int childcnt_=0;

	/**
	Transparent external space around bounding box (<b>not</b> part of bounding box width or height, as effects incorporated into bounding box (x,y) location).
	Usually layout node will collapse bottom margin of previous Node and top of this; likewise with left and right.
	Stored here rather than being passed about during formatting so that incremental formatting is simpler (and there aren't that many internal nodes).
	In other words, margin is used during formatting, but otherwise drops out as its effect
	is incorporated into bbox.x and bbox.y.
	Components (top, left, bottom, right) should be treated read only;
	 */
	public Insets margin = INode.INSETS_ZERO;

	/**
	Border, with possible different widths on each side (counted in bounding box dimensions).
	Components (top, left, bottom, right) should be treated read only.
	 */
	public Insets border = INode.INSETS_ZERO;

	/**
	Internal space inside border, with same background as content (counted in bounding box dimensions).
	Components (top, left, bottom, right) should be treated read only.
	 */
	public Insets padding = INode.INSETS_ZERO;



	@Override
	protected Object clone() throws CloneNotSupportedException {
		INode n = (INode)super.clone();
		//Node[] ch=n.children_; if (ch!=null) { ch=new Node[childcnt_]; System.arraycopy(children_,0, ch,0, childcnt_); } => don't share children
		n.children_ = new Node[10];
		n.childcnt_ = 0;
		// margin, border, padding treated read only, and shared among nodes anyway, so bit copy ok
		return n;
	}


	public INode(String name,Map<String,Object> attr, INode parent) { super(name,attr, parent); }

	/**
	Names of internal nodes are normalized to all-lowercase (but not interned).
	If name set to null, this indicates a nonstructural node in an otherwise structural tree.
	Nonstructural nodes can be useful to better balance trees leaning to improved performance.
	Nonstructural nodes are ignored by Location and other classes.
	 */
	@Override
	public void setName(String name) {
		//name_ = (name!=null? name.toLowerCase().intern(): null);	// lc + intern here, or be more general purpose?
		// use canonical single-letter lowercase Strings here, relieving media adaptors?
		//if (name!=null && !name.equals(name.toLowerCase())) System.out.println("uppercase INode: "+name);
		name_ = name!=null? name.toLowerCase(): null;	// lc + intern here, or be more general purpose? => don't force to lowercase here.  PDF wants to store some ops in nodes and necessary to case preserved
	}

	@Override
	public final boolean isStruct() { return true; }	// cheaper than instanceof Leaf(?)

	/** Skip over border, padding to content area. */
	@Override
	public int dx() { return bbox.x + /*margin.left--included in bbox.x +*/ border.left + padding.left; }
	/** Skip over border, padding to content area. */
	@Override
	public int dy() { return bbox.y + /*+margin.top +*/ border.top + padding.top; }


	@Override
	public boolean breakBefore() { return true; }
	@Override
	public boolean breakAfter() { return true; }



	/* *************************
	 *
	 * general tree manipulation
	 *
	 **************************/

	/** If first child is leaf, returns that.  If first child is INode, recursively returns that node's getFirstLeaf().  If no chldren, returns null. */
	@Override
	public final Leaf getFirstLeaf() {
		Leaf l = null;
		if (childcnt_ > 0)
			for (int i=0, imax=childcnt_; i<imax; i++) {
				l = children_[i].getFirstLeaf();
				if (l!=null) break;
			}
		//return (childcnt_==0? null: children_[0].getFirstLeaf());
		return l;
	}

	/** If last child is leaf, returns that.  If last child is INode, recursively returns that node's getLastLeaf().  If no chldren, returns null. */
	@Override
	public final Leaf getLastLeaf() {
		//return (childcnt_==0? null: children_[childcnt_-1].getFirstLeaf());
		Leaf l = null;
		for (int i=childcnt_-1; i>=0; i--) {
			l = children_[i].getLastLeaf();
			if (l!=null) break;
		}
		return l;
	}

	/** Returns first child, or null if no children.  Same as <code>childAt(0)</code>. */
	public final Node getFirstChild() { return childcnt_>0? children_[0]: null; }

	/** Returns last child, or null if no children. Same as <code>childAt(size()-1)</code>. */
	public final Node getLastChild() { return childcnt_>0? children_[childcnt_-1]: null; }

	// for now, modify children only during build phase
	// as child mod doesn't maintain Span correctness
	/** @return number of children. */
	@Override
	public int size() { return childcnt_; }

	/** Returns sequence number node in list of children, -1 iff no such child. */
	public int childNum(Node n) {
		assert n!=null;

		for (int i=0; i<childcnt_; i++) if (children_[i]==n) return i;
		return -1;
	}

	// not any more ** Use negative numbers to index from end (so end==-1, next to last==-2, ...). */
	/** Returns child at given offset, 0 .. size()-1, inclusive. */
	public Node childAt(int num) {
		assert num>=0 && num<childcnt_: "invalid child #"+num;
		//if (num<0) num = childcnt_+num;	// negative number indexes from end
		//if (num>=childcnt_) return null;
		return children_[num];
	}

	/** DOM2 -- childAt() more efficient. */
	public Node getPreviousSibling() {
		INode p = getParentNode();
		if (p!=null) {
			int num = childNum()-1;
			if (num>=0) return p.childAt(num);
		}
		return null;
	}

	/** DOM2 -- childAt() more efficient. */
	public Node getNextSibling() {
		INode p = getParentNode();
		if (p!=null) {
			int num = childNum()+1;
			if (num < p.size()) return p.childAt(num);
		}
		return null;
	}

	/**
	Adds <var>child</var> to end of list of children.
	Child automatically removed from old parent, if any.
	If child alread among children, child moved to end of child list.
	Node constructors take a parent, which automatically calls appendChild(),
	or you can pass null as parent and appendChild() or insertChildAt() later.
	 */
	public void appendChild(Node child) {
		assert child!=null;

		child.remove();	// remove from old parent, if any

		if (childcnt_==children_.length) {
			Node[] newchildren = new Node[children_.length * 2];
			System.arraycopy(children_,0, newchildren,0, children_.length);
			children_ = newchildren;
		}
		children_[childcnt_] = child;
		//child.parent_ = this;
		child.setParentNode(this);
		//child.setValid(false);	// ? -- maybe if last parent non-null
		childcnt_++;

		// during tree building, this stops at this same node, so just pay method call+boolean check
		markDirty();	// this and up
		// much LATER: may split node if tree too imbalanced
	}

	// => move to some UI class
	/** Add child to correct category, as in UI panel or menu with groups. */
	public Node addCategory(Node n, String category) {
		assert n!=null /*&& category!=null -- ok*/;

		if (category==null) appendChild(n);
		else {
			if (!category.startsWith("_")) category="_"+category;
			Node cat = findBFS(category);
			// add new spontaneous categories at end
			if (cat==null)
				cat=new LeafZero(category,null, this);
			//VSeparator(category,null, this);
			int inx = cat.childNum();
			insertChildAt(n, inx);
		}
		return n;	// maybe true/false reporting success/failure
	}


	/* * Negative number indexes from end. */
	// LATER: move any spans on replaced child to new one?
	// useful for zapping ads, PDF's XDOC-like rewriting
	public void setChildAt(Node child, int num) {
		//if (num<0) num = childcnt_+num;
		assert child!=null;	// "to remove child use removeChild() or removeChildAt()";
		assert num>=0 && num<childcnt_: num;

		//children_.set(num, child);
		children_[num].parent_ = null;	// disabuse old one
		children_[num] = child;	// install new one
		/*if (child!=null)?*/ child.parent_ = this;
		//child.setDirty(); child.parent_.setDirty();
		markDirty();
	}

	public void insertChildAt(Node child, int num) {
		//if (num<0) num = childcnt_-1+num;	// negative => offset from end
		assert num>=0 && num<=childcnt_: num+" vs "+childcnt_;

		appendChild(child);	// grow array if needed, then move into right place
		// should use System.arraycopy,
		// but doesn't seem to be implemented correctly if start and dest same array,
		// even though explicitly declared to work in Gosling/Joy/Steele
		//for (int i=childcnt_-1; i>num; i--) { children_[i].setDirty(); children_[i]=children_[i-1]; }
		//for (int i=childcnt_-1; i>num; i--) children_[i]=children_[i-1];
		System.arraycopy(children_,num, children_,num+1, childcnt_ - num - 1);
		children_[num] = child;
		//child.setDirty();
		//markDirty();	-- already, with appendChild()
		//children_.add(num, child);
	}

	/*
  public void moveChildAt(int num, INode newparent) { moveChildAt(num, newparent,newparent.size()); }
  public void moveChildAt(int num, INode newparent, int tonum) {
	if (num<0) num = childcnt_+num;
	assert num>=0 || num<childcnt_: "invalid child #"+num;

	Node child = children_[num];
	child.remove();
	newparent.insertChildAt(child, tonum);
  }*/


	/** @return child removed */
	public Node removeChildAt(int num) {
		//if (num<0) num = childcnt_-1+num;	// negative => offset from end => remove this as just as easy and efficient for caller (w/size())
		//if (num<0 || num>=childcnt_) return null;
		assert num>=0 && num<childcnt_: num;

		// do the right thing with spans.  Which is... what?
		// call remove on all sticky pointers? maybe shove them to previous or next leaf
		// fix spans and marks now, while it (and parent!) still attached

		Node zap = children_[num];
		childcnt_--;
		//for (int i=num; i<childcnt_; i++) children_[i]=children_[i+1];
		int len = childcnt_/*-1 already -- 1 less*/-num/*0..num-1 already in place*/;
		/*if (len>0)*/ System.arraycopy(children_,num+1, children_,num, len);
		children_[childcnt_]=null;	// no lingering objects -- don't do zap.parent_=null; in order to give child chance to clean up

		markDirty();
		//if (childcnt_==0) remove(); -- internal node without children illegal, but maybe putting something back before format+paint
		return zap;
	}

	/** Removes <var>child</var> if it exists among list of children. */
	public void removeChild(Node child) {
		assert child!=null;

		int index = childNum(child);
		//assert index!=-1; => possible for child->point to parent but not parent->child, as in IScrollPane's scrollbars
		if (index>=0) removeChildAt(index);
	}

	/** Faster than removing one at a time. */
	public void removeAllChildren() {
		//Arrays.fill(children_, 0,childcnt_/*exclusive*/, null);	// no lingering object references
		children_ = new Node[10];	// better since shrinks array size too (or worse?)
		childcnt_=0;
		markDirty();
	}


	/** Passed node (null OK) somewhere in subtree? */
	@Override
	public boolean contains(Node n) {
		//assert /*n!=null -- OK: null Node => false */;
		for ( ; n!=null; n=n.getParentNode()) if (n==this) return true;
		return false;
	}



	/** WARNING: This doesn't climb up to lowest structural parent, inconsistently with structChildNum */
	public int structsize() {
		int sum=childcnt_;
		for (int i=0; i<childcnt_; i++)
			if (children_[i].getName()==null) sum += ((INode)children_[i]).structsize() - 1;
		//System.out.print("structCountChildren "+this+" = "+sum);
		return sum;
	}

	// not general but good enough for now
	public int structChildNum(Node n) {
		assert n!=null;

		int num = childNum(n);
		// collect up struct from preceding siblings
		for (int i=0,imax=num; i<imax; i++) {
			Node sib = childAt(i);
			if (sib.getName()==null) {
				num--;
				if (sib.isStruct()) num += ((INode)sib).structsize();
			}
			//((INode)children_[i]).structsize() /*- 1*/;
		}
		//System.out.print("structChildNum "+n+" = "+num);
		if (getName()==null) num+=structChildNum();
		//System.out.println(" => "+num);
		return num;
	}

	/**
	Some nonstructural nodes (internal nodes with name==null) may be used to better balance trees for better performance.
	This method bypasses nonstructural nodes.
	 */
	/*synchronized*/ public final Node structChildAt(int num) {
		INode[] parents = new INode[Node.MAXNONSTRUCTDEPTH];
		int[] posns = new int[Node.MAXNONSTRUCTDEPTH];
		int[] childcnts = new int[Node.MAXNONSTRUCTDEPTH];

		int recursecnt=0+1;	// implicit sentinal in 0

		int absposn=0;
		INode p = this;
		while (p!=null && p.getName()==null) p=p.getParentNode();
		Node child = p.childAt(0);
		int childcnt = p.size();
		//System.out.println("structChildAt "+num+" @ "+p.getName());
		for (int now=0; child!=null /*otherwise explicit exit*/; absposn++, child=p.childAt(now)) {
			// invariant: now validating child #now of parent p, with absolute position absposn
			while (child.getName()==null) {	// better be an internal node if null
				parents[recursecnt]=p; posns[recursecnt]=now+1; childcnts[recursecnt]=childcnt; recursecnt++;	// save previous position
				p=(INode)child; now=0; child=p.childAt(now); childcnt=p.size();	// go down
			}

			//System.out.println("num="+num+", absposn="+absposn+", now="+now+", child="+child);
			if (absposn==num) return child;	// current value of child has structural node

			now++;
			while (now==childcnt && recursecnt>0) {	// end of the line, pop up
				recursecnt--; p=parents[recursecnt]; now=posns[recursecnt]; childcnt=childcnts[recursecnt];
			}
			if (recursecnt==0) break;
		}
		return null;
	}


	// trim off content leaves
	/*
  public void trim() {
	for (int i=size(); i>=0; i--) {
		Node child = childAt(i);
		if (child.isLeaf()) child.remove();
	}
  }*/


	/* *************************
	 *
	 * PROTOCOLS
	 *
	 **************************/


	/**
	Internal nodes might have a structural style
	LATER: externalize format to a behavior and choose with style sheet.
	 */
	@Override
	public boolean formatBeforeAfter(int width,int height, Context cx) {
		if (valid_) {
			if (cx.valid && stickycnt_>0) cx.valid=false;
			return false;
		}

		if (!cx.valid) cx.reset(this,-1);	// could have skipped intermediate nodes

		// put this here?  INode not supposed to draw much on its own (borders, background)
		if (size()==0) { bbox.setSize(0,0); valid_=true; return false; }

		cx.clearNonInherited();

		boolean fstyle=false; StyleSheet ss=cx.styleSheet; List<ContextListener> sact=new ArrayList<ContextListener>(10);
		// add structural settings, if any
		// IF CHANGE SOMETHING HERE, REFLECT IN Node.getActivesAt()
		if (name_!=null && /*temporarily*/ss!=null) {	// null name_ used on nonstructural nodes introduced to balance tree
			ss.activesAdd(sact, this, getParentNode());

			if (sact.size() > 0) {
				for (int i=0, imax=sact.size(); i<imax; i++) {
					ContextListener cl = sact.get(i);
					cx.addq(cl);
					cl.appearance(cx, true);	// execute non-inherited ones too
				}
				//cx.reset(); -- NO! want non-inherited too, and just this node
				fstyle = true;
			}
		}


		// NEED TO DO THIS IN CONTEXT (so can subclass), somehow
		// set margin, padding, border -- formatNode can tweak
		int effw=width, effh=height;
		//System.out.println(getName()+ " fBA in="+width+"x"+height);
		if (!cx.elide && fstyle) {	// no margin, border, padding within elided region
			int lm=cx.marginleft, rm=cx.marginright, tm=cx.margintop, bm=cx.marginbottom;
			int lp=cx.paddingleft, rp=cx.paddingright, tp=cx.paddingtop, bp=cx.paddingbottom;
			int lb=cx.borderleft, rb=cx.borderright, tb=cx.bordertop, bb=cx.borderbottom;

			// margins may be adjusted by layout node.	e.g., paragraph layout collapses bottom of one-top of next into max of the two (by zapping from bottom of first)
			// these set before formatNode so they can be tweaked by node attributes and code
			margin = lm>=0 && lm<INode.INSETS.length && lm==rm && rm==tm && tm==bm? INode.INSETS[lm]: new Insets(tm,lm, bm,rm);
			padding = lp>=0 && lp<INode.INSETS.length && lp==rp && rp==tp && tp==bp? INode.INSETS[lp]: new Insets(tp,lp, bp,rp);
			border = lb>=0 && lb<INode.INSETS.length && lb==rb && rb==tb && tb==bb? INode.INSETS[lb]: new Insets(tb,lb, bb,rb);

			effw -= lm+rm + lb+rb + lp+rp; effh -= tm+bm + tb+bb+ tp+bp;
			//if (lb>0) System.out.println("setting border to "+lb);
			cx.eatHeight(tb+tp, this,0);

		} else
			margin = padding = border = INode.INSETS_ZERO;

		//if (getAttributes()!=null) System.out.println(getName()+" "+getAttributes()+": align="+cx.align+", "+structcx);
		//if (cx.align!=Node.ALIGN_INVALID) System.out.println("INode "+getName()+" align to "+cx.align);
		if (cx.align!=Node.ALIGN_INVALID) align=cx.align;
		if (cx.valign!=Node.ALIGN_INVALID) valign=cx.valign;
		//	System.out.println("setting align="+align+" on "+getName()); }
		if (cx.floats!=Node.ALIGN_INVALID) floats=cx.floats;
		//if (cx.floats!=NONE) System.out.println("setting floats="+floats+" on "+getName());

		if (fstyle) cx.reset();


		// assume margin, border, padding set by style sheet, but node itself can change by setting object (not fields) and adjusting WxH
		boolean shortcircuit = super.formatBeforeAfter(effw,effh, cx);	// before, node, after


		// grow by padding and border
		if (bbox.width>=0 ||/*&&*/ bbox.height>=0) {
			if (border!=INode.INSETS_ZERO || padding!=INode.INSETS_ZERO) {
				//int oh = bbox.height;
				// node may have directly set border and padding
				bbox.width += border.left+border.right + padding.left+padding.right;
				bbox.height += border.top+border.bottom + padding.top+padding.bottom;
				// margins considered by parent -- what margin would be without setting here

				//if (baseline==oh) baseline=bbox.height; else baseline += border.top + padding.top;  //Math.min(baseline + border.top + padding.top, bbox.height);
				cx.eatHeight(padding.bottom+border.bottom, this,size());
				//cx.eatHeight(dh, this,size());
				//System.out.println(getName()+ " fBA b:"+border+" + p:"+padding+" => "+bbox.width+"x"+bbox.height);
			}
		} else margin = padding = border = INode.INSETS_ZERO;


		// remove effects from this Node
		if (fstyle) {
			//ss.activesRemove(actives, this, getParentNode());
			List<ContextListener> actives = cx.vactive_;
			for (int i=sact.size()-1; i>=0; i--) actives.remove(sact.get(i));	// directly on a List<> object
			if (cx.valid) cx.reset(); else cx.valid=false;	// if was valid, maintain that; else mark invalid -- BUT DON'T MARK VALID IF WAS INVALID!
			// maybe could establish an invariant to eliminate the need for cx.reset() and the counterpart in paint(), but probably not time critical
		}

		//valid_=!shortcircuit; -- here or leave to formatNode?  when would this not be right?
		return shortcircuit;
	}


	/**
	<b>Children report dimensions (width and height), parent places at (x,y)</b>.
	Bbox = union of children's bboxes
	Children stacked vertically like TeX vbox; override to implement other layout strategies
	<!--Later: Just format as much as necessary to fill screen.-->
	To implement a new layout manager, override this method.

	@see multivalent.node.FixedI
	 */
	/*abstract*/ @Override
	public boolean formatNode(int width,int height, Context cx) {
		// no visible representation to format
		//int xin=bbox.x, yin=bbox.y;	// parent in charge of positioning
		int ccnt=size();
		if (ccnt==0) bbox.setBounds(0,0,0,0);
		else {
			Node n = childAt(0); n.formatBeforeAfter(width,height, cx); bbox.setBounds(n.bbox);
			for (int i=1,imax=ccnt; i<imax; i++) { n=childAt(i); n.formatBeforeAfter(width,height, cx); bbox.add(n.bbox); }
		}
		bbox.add(0, 0);	// this or make children relative
		//System.out.println(getName()+" "+bbox+", "+childAt(0).bbox);
		//bbox.add(xin,yin);  //bbox.setLocation(xin,yin);
		valid_ = true;
		return !valid_;
	}


	/**
	<ol>
	<li>Check to see subtree area within clipping region.  If not <i>and span transitions</i>, mark cx.valid=false and return.
	<li>Add styles from style sheet, if any.
	<!--li>Add style for this particular node, if any (as for HTML TD with background but w/o class)-->
	<li>Draw background if differs from enclosing subtree.
	<li>Call superclass paintBeforeAfter, which sets coordinate space, calls observers, calls paintNode.
	<li>Draw border, if any.
	<li>Undo styles
	</ol>
	 */
	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		// fast path to skip large invisible subtree
		// could clip children but in object oriented-style leave that decision to children
		//if (!cx.g.getClipBounds().equals(docclip)) System.out.println(cx.g.getClipBounds()+" vs "+docclip);
		//if (!cx.g.hitClip(bbox.x, bbox.y, bbox.width, bbox.height) || (bbox.width==0 && bbox.height==0)) {
		if (!intersects(docclip)
				//bbox.x > docclip.width || bbox.x + bbox.width < docclip.x || bbox.y > docclip.height || bbox.y + bbox.height < docclip.y	// docclip (x,y) moved, but WxH not adjusted, so right=W and height=H
				|| bbox.width==0 && bbox.height==0
		) {
			// gc invalid only if have spans that start or stop within subtree, but not those entirely contained
			//if (cx.valid && sticky_!=null && sticky_.size()>0) System.out.println("skipping INode @ "+name_);
			if (stickycnt_ > 0) cx.valid=false;	// pessimistic
			//System.out.print(" C"+size()+"@"+(-docclip.x)+","+(-docclip.y));
			// which we can repair LATER
			// ... maybe add in stickies and maintain validity
			return;
		}

		// later take into consideration prevailing scale in Context
		// (with inlined, specialized version of intersects)
		//System.out.println("me="+cbbox+" "+docclip);

		//if (!cx.valid) { cx.reset(this,-1); System.out.println("resetting INode @ "+name_+", size="+cx.size); }
		if (!cx.valid) cx.reset(this,-1);	// may not have right ContextListeners
		//Color pbgin = cx.pagebackground, bgin=cx.background;
		//if (pbgin!=Color.WHITE) System.out.println("INode "+pbgin);
		//System.out.println("node="+getName()+", valid="+isValid()+", # children="+size()+", bbox="+bbox);

		// X can't move this into Context because have to set before other observers too, which get same coordinate space and same Context
		/*
	boolean fstyle=false; StyleSheet ss=cx.styleSheet; List<ContextListener> sact=getList();
	if (name_!=null && ss!=null) {
		ss.activesAdd(sact, this, getParentNode());

//System.out.println("paint "+getName()+"/"+getAttr("class")+"/"+getAttr("id")+": def="+defstructcx+", tweak="+structcx);
		if (sact.size() > 0) {
			//for (int i=0, imax=sact.size(); i<imax; i++) cx.addq((ContextListener)sact.get(i));
			cx.background = CLGeneral.COLORINVALID;	// special case: both inherited and non-inherited
			for (int i=0, imax=sact.size(); i<imax; i++) {
				ContextListener cl = (ContextListener)sact.get(i);
				cx.addq(cl);
				cl.appearance(cx, true);	// execute non-inherited ones too
			}
			cx.pagebackground = cx.background;

			cx.reset();
			fstyle = true;
		}
	}
		 */

		/*
	// would really like to move background drawing to Context
	int x=bbox.x, y=bbox.y, w=bbox.width, h=bbox.height;	// we're in parent's coordinate space
	Color newbg = cx.background;
	//if (cx.background!=null && !cx.background.equals(bgin)/*not from span* / && !cx.background.equals(pbgin)) {	// this is not right but usually right so leave for now
	if ((newbg==null && bgin!=null && pbgin!=null) || (newbg!=null && !newbg.equals(bgin)/*not from span* / && !newbg.equals(pbgin))) {	// this is not right but usually right so leave for now
		if (newbg!=null) {
			Graphics2D g = cx.g;
			// margin already handled: transparent + incorporated in location (x,y)
			// let border overwrite below
			g.setColor(newbg); g.fillRect(x,y, w,h);
bgcnt_++;
		}
//*if (newbg!=Color.WHITE)* / System.out.println("new pagebg = "+newbg);
		cx.pagebackground = newbg;	// pick up null (transparent) background
	}
		 */
		/*
	// border -- used to draw after content, but want node to be able to paint over -- and also Context may be invalid then and don't want to reset() just to pick up border color
	if (border!=INSETS_ZERO) {
//System.out.println("drawing border in "+cx.foreground+": "+border.left+" => "+new Rectangle(x,y, border.left,h));
		// also, border-color and border-style
		//cx.reset();
		Graphics2D g = cx.g;
		//g.setColor(cx.foreground);	// black around black text looks bad
		g.setColor(cx.foreground==Color.BLACK && cx.background!=Color.LIGHT_GRAY? Color.LIGHT_GRAY: cx.foreground);
		// handles both side and width
		if (border.left>0) g.fillRect(x,y, border.left,h);
		if (border.right>0) g.fillRect(x+w-border.right,y, border.right,h);
		if (border.top>0) g.fillRect(x,y, w,border.top);
		if (border.bottom>0) g.fillRect(x,y+h-border.bottom, w,border.bottom);
	}
		 */

		cx.paintBefore(cx, this);	// do here rather than Node.beforeAfter so system switches on type (INode vs Leaf)
		Color pbgin = cx.pagebackground;//, bgin=cx.background;
		//if (!cx.valid) cx.reset(this,-1);

		super.paintBeforeAfter(docclip, cx);	// before, node, after

		cx.paintAfter(cx, this);
		//cx.g.setColor(Color.RED); cx.g.drawLine(x,y, x+w,y+h);


		/*
	if (fstyle) {
		List<ContextListener> actives = cx.vactive_;	// cx.vactive_ handle may have received brand new list between start of method and here if some child called cx.reset(node,off) which does getActvesAt()
		//ss.activesRemove(actives, this, getParentNode());
		for (int i=sact.size()-1; i>=0; i--) actives.remove(sact.get(i));
		putList(sact);
		if (cx.valid) cx.reset(); else cx.valid=false;	// if not valid, can't make it so just by removing your cl's and exiting!!!
	}
		 */
		cx.pagebackground = pbgin;

		//	g.setColor(rainbow[rainbowi++%rainbow.length]); g.drawRect(1,1, bbox.width-2,bbox.height-2);
		// if popping out of a float, cx.reset() (cx.reset(node,-1) not necessary)
		//	  if (getAttr("align")!=null) cx.reset(); -- should be done automatically as remove that structure
	}


	/** To paint internal node, paint all children. */
	@Override
	public void paintNode(Rectangle docclip, Context cx) {	// overridden by outliner nodes, OL, UL
		Color pbg = cx.pagebackground;
		for (int i=0,imax=size(); i<imax; i++) {
			childAt(i).paintBeforeAfter(docclip, cx);
			cx.pagebackground = pbg;	// maintain setting from enclosing parent
		}
	}


	/** To select structural region, select all children */
	@Override
	public void clipboardNode(StringBuffer txt) {
		int len = txt.length();
		if (breakBefore() && len>0 && txt.charAt(len-1)!='\n') txt.append('\n');
		for (int i=0,imax=size(); i<imax; i++)
			childAt(i).clipboardBeforeAfter(txt);
		if (breakAfter()) txt.append('\n');
	}


	/** Adjust for bbox (which itself has been adjusted for margins), padding, border. */
	@Override
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		if (rel!=null && !contains(rel)) return false;
		return super.eventBeforeAfter(e, rel);
	}


	/**
	Internal nodes pass on to children.
	Events are propogated from last child to first (and painted first to last), which means that
	later children have implicitly higher priority as they get first chance at setting a grab.
	Event passed on only if (x,y) within child's bounding box, or if TreeEvent.VALIDATE event.
	Event is translated to be in child's coordinate system (that is, (0,0) corresponds to the child's origin).

	<!--

	boolean shortcircuit = false;
	for (int i=size()-1; i>=0 && !shortcircuit; i--) shortcircuit = childAt(i).eventBeforeAfter(e,rel);

	-->
	 */
	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		boolean hitchild=false;
		//System.out.println(getName()+" "+rel);
		boolean shortcircuit=false;
		//for (int i=0,imax=size(); i<imax && !shortcircuit; i++) {

		for (int i=size()-1; i>=0 && !shortcircuit; i--) {
			Node child = childAt(i);
			Rectangle cbbox = child.bbox;
			//if (i==0) System.out.println(rel+" in "+cbbox+" "+(rel!=null && cbbox.contains(rel)));
			if (rel!=null && cbbox.contains(rel))
				hitchild=true;
			//System.out.println("setcur "+child.getName()+"/"+child.getFirstLeaf().getName());
			//break;	// overlaps go to one on top ONLY? => have to send shortcircuit, not automatic
			//			if (child.eventBeforeAfter(e, rel)) return true;	// BUT could have overlaps... -- short circuits all the way up, so don't care that we don't fix coordinates (I hope)
			shortcircuit = child.eventBeforeAfter(e,rel);	// regardless of bbox!
			//if (shortcircuit) System.out.println("ss @ "+child.getClass().getName()+"  "+child.getName()+"/"+child.getFirstLeaf().getName());
		}
		//if (!hitchild) System.out.println("setcur "+this);


		// for mouse events inside bounding box but not in any child, Browser.setCurNode(this)
		if (!shortcircuit && !hitchild) {
			/*
System.out.println(rel+" not in children of INode "+getName()+" "+phelps.text.Rectangles2D.pretty(bbox));
System.out.print("\t");
for (int i=0,imax=size(); i<imax; i++) { Node child=childAt(i); System.out.print(child.getName()+phelps.text.Rectangles2D.pretty(child.bbox)+" / "); }
System.out.println();
			 */
			Browser br = getBrowser();
			int eid = e.getID();
			//System.out.println("I setcur "+getName());
			//if (br.getCurNode()==null && (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED || eid==TreeEvent.FIND_NODE)) {
			if ((MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST || eid==TreeEvent.FIND_NODE) && br.getCurNode()==null)
				//System.out.println(getName()+" "+rel+" "+bbox);
				br.setCurNode(this,0/*-1?*/);	// set to lowest enclosing node -- Leaf only?
		}

		return shortcircuit;
	}




	/* *************************
	 *
	 * tree management: dirty bit, findDFS
	 *
	 **************************/

	@Override
	public void markDirtySubtreeDown(boolean leavestoo) {
		setValid(false);	// not markDirty(), as that goes up
		for (int i=0,imax=size(); i<imax; i++) childAt(i).markDirtySubtreeDown(leavestoo);
	}

	// do weird transformations in findNode (scrolled, absolute, screen, ...), factor out commonality in findNodeSmaller
	/*
  public Node findDFS(Point rel) {
	Mark m=null;
	if (bbox.contains(rel)) {
				rel.translate(-bbox.x-margin.left,-bbox.y-margin.top);
		m = findNodeSmaller(rel);
		rel.translate(bbox.x+margin.left,bbox.y+margin.top);
	}
	return m;
  }

  public Mark findNodeSmaller(Point rel) {
	Mark smallest = null;
	int area = bbox.width * bbox.height;
	// invariant: (x,y) in n.  can you find a more specific match in children?
	for (int i=0,imax=size(); i<imax; i++) {
		Node c = childAt(i);
		Node hit = childAt(i).findDFS(rel);
		if (hit!=null) {
			// choose intersection with smallest bbox
			Rectangle bbox=hit.bbox;
			int hitarea = bbox.width * bbox.height;
			if (hitarea <= area) { smallest=hit; area=hitarea; }	// "<=" for single-word paragraphs, regions--i.e., in ties prefer children
		}
	}
	return (smallest!=null?smallest:new Mark(this,-1));
  }*/

	@Override
	public Node getElementById(String elementId) {
		Node n = null;

		if (elementId == null) n = null;
		else if (elementId.equals(getAttr(Node.ATTR_ID))) n = this;
		else for (int i=0,imax=size(); i<imax; i++) if ((n = childAt(i).getElementById(elementId)) != null) break;

		return n;
	}

	@Override
	protected Node findDFS(String searchname, String attrname, String attrval, int level, int maxlevel) {
		Node n = super.findDFS(searchname, attrname, attrval, level, maxlevel);

		// could use nextNode() so can user can start at any node (and hence throw out a result and resume at that point), at the cost of some speed
		// if so, don't need to override to examine children
		// recurse through children
		if (level < maxlevel) for (int i=0,imax=size(); n==null && i<imax; i++)
			n = childAt(i).findDFS(searchname, attrname,attrval, level+1, maxlevel);

		return n;
	}


	// in addition to findDFS(x,y)
	// useful for finding floats (caller needs to throw out non-floats... or maybe have a special visual layer just for floats)
	/*
  public List<Node> findNodesClip(Rectangle clip) {
	List<Node> hits = null;
	for (int i=0,imax=size(); i<imax; i++) {
		Node n = childAt(i);
		if (clip.intersects(n.bbox)) {
			if (hits==null) hits=new ArrayList<Node>(10);
			hits.add(n);
		}
	}
	return hits;
	}*/


	@Override
	public boolean checkRep() {
		assert super.checkRep();

		// 1. self
		if (childcnt_ == 0) {	// not menu, toolbar2
			assert stickycnt_==0;
			return true;
		}


		// 2. children
		for (int i=0,imax=childcnt_; i<imax; i++) {
			Node child = children_[i];
			assert child!=null;
			assert child.getParentNode() == this: child.getParentNode();	// reverse of check in Node
			if (isValid()) assert child.isValid(): getName()+" valid => child #"+i+" "+child.getName()+" not valid";	// if valid, then all nodes in subtree must be valid.  checked one level here, other levels in recursion to children below
			//assert /*!valid_ ||*/ bbox.contains(child.bbox): bbox+" "+child.bbox;	// => this is OK!  clipping, generally
			// children relative to parent?

			// recurse
			assert child.checkRep();
		}
		assert getFirstLeaf() != null;
		assert getLastLeaf() != null;



		// 3. summary stickies (after recursed to children)
		// compute what should be: unpaired span open or close within subtree
		List<Span> comp = new ArrayList<Span>(10);
		for (Leaf l = getFirstLeaf(), endl = getLastLeaf().getNextLeaf(); l != endl; l = l.getNextLeaf())
			// already validated leaf and span
			for (int i=0, imax = l.stickycnt_; i<imax; i++) {
				Mark m = l.sticky_[i];
				Object owner = m.getOwner();
				if (owner instanceof Span) {
					Span span = (Span)owner;
					int inx = comp.indexOf(span);
					if (inx != -1) {
						assert span.getEnd() == m;	// already collected one endpoint, so must be end
						comp.remove(inx);	// found pair
					} else comp.add(span);
				}
			}

		// compare to what is
		int scnt = 0;	// other things than spans can be sticky, so count spans
		for (int i=0,imax=stickycnt_; i<imax; i++) {
			Object owner = sticky_[i].getOwner();
			if (owner instanceof Span) {
				scnt++;
				assert comp.contains(owner);
			}
		}
		assert comp.size() == scnt++: comp+" vs "+java.util.Arrays.asList(sticky_);	// leftovers

		return true;
	}


	// tree manipulation methods
	// dump subtree (recursive; call with root dumps whole tree)
	@Override
	public void dump(int level, int maxlevel) {
		super.dump(level, maxlevel);
		//for (int i=0; i<level; i++) System.out.print("  ");
		//System.out.println(name+" ("+size()+" children_)");
		if (level < maxlevel) for (int i=0,imax=size(); i<imax; i++) childAt(i).dump(level+1, maxlevel);
	}
}
