package multivalent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
	Leaf nodes are words of text, XDOC words, images, and so on.
	Many media adaptors subclass Leaf for their own type of nodes.

	<ul>
	<li>Updates Context across spans on leaves during {@link #formatNode(int, int, Context)} and {@link #paintNode(Rectangle, Context)}.
	<li>Defines interface for hit detection ({@link #subelement()}, {@link #subelementHit(Point)}.
	<li>Defines interface for editing ({@link #cut(int, Leaf,int)}, {@link #copy(int, Leaf,int)}, {@link #paste(int)}, {@link #split(int)}, {@link #delete(int, Leaf,int)}, {@link #insert(int, String, INode)}).
	</ul>

	@version $Revision: 1.11 $ $Date: 2003/06/02 05:07:40 $
 */
/*!!!abstract*/ public class Leaf extends Node {
	static final int INTERWORD_SPACE=1;

	private static Leaf cacheLeaf_ = null;


	/**
	In most cases a Leaf's attributes are null, as in a word of text, but image nodes have attributes.
	 */
	public Leaf(String name, Map<String,Object> attrs, INode parent) { super(name, attrs, parent); }


	@Override
	public void setName(String name) {
		// LATER: error to have null name?  (zero-length string OK)
		name_ = name;	// Strings.valueOf(name); -- too late; already allocated String... but gc in nursery...
	}

	@Override
	public final boolean isLeaf() { return true; }	// cheaper than instanceof Leaf?  useless if not

	@Override
	public int dx() { return bbox.x; }
	@Override
	public int dy() { return bbox.y; }

	//public Object clone() throws CloneNotSupportedException { -- nothing special over Node



	/* *************************
	 *
	 * general tree manipulation
	 *
	 **************************/

	@Override
	public final Leaf getFirstLeaf() { return this; }
	@Override
	public final Leaf getLastLeaf() { return this; }



	/* *************************
	 *
	 * PROTOCOLS
	 *
	 **************************/

	@Override
	public boolean formatBeforeAfter(int width,int height, Context cx) {
		if (valid_) {
			if (cx.valid && stickycnt_>0) cx.valid=false;
			return false;
		}

		if (!cx.valid) cx.reset(this,-1);	// could have skipped intermediate nodes
		return super.formatBeforeAfter(width, height, cx);
	}


	/**
	Handles span transitions on node and elided portions.
	Called medium-specific leaf formatNodeContent in hunks of maximum-length homogeneous span coverage to get content size.
	Marks node valid, so if node not (e.g., LeafImage's image not loaded), set valid_=false in formatNodeContent.
	 */
	/*final--but HTMLHR and ShadowLeaf*/ @Override
	public boolean formatNode(int width, int height, Context cx) {
		//if (!cx.valid) System.out.println("context not valid in leaf "+this);
		if (!cx.valid) cx.reset(this,-1);	// always true as done in formatBeforeAfter?  what about reformats?
		//if ("Introduction".equals(getName())) System.out.println("formatNode "+getName()+", size="+cx.size);

		valid_ = true;	// if leaf sensitive to width, mark to false on probes

		// fast path: elided and no span transitions to change that
		if (cx.elide && sticky_==null) { bbox.setSize(0,0); baseline=0; valid_=true; return !valid_; }	// elide fast path

		StyleSheet ss = cx.styleSheet;
		Mark m = null;
		int start=0, end;
		int nW=0, nH=0, nB=0;	// overall width=sum of pieces; height, baseline = max of pieces

		// IF CHANGE SOMETHING HERE, REFLECT IN Node.getActivesAt()
		Span lastspan=null;	// save a stylesheet lookup if span starts and ends on same leaf with no intervening and has no styles (just itself for appearance()) -- could make static for start-end w/o intervening?
		List<ContextListener> actives = cx.vactive_;
		for (int i=0,imax=stickycnt_; i<imax; ) {
			// maybe give ranges their own ArrayList<> subclass if casting is a performance problem
			m = sticky_[i]; if (!(m.getOwner() instanceof Span)) { i++; continue; }

			end = m.offset-1;	// translate intercharacter position number to character number

			// draw piece up to transition... (which may be empty, including common case of span starting at first character)
			if (!cx.elide && start <= end) {
				formatNodeContent(cx, start, end);	// inclusive of endpoints
				bbox.height += cx.spaceabove + cx.spacebelow;
				baseline += cx.spaceabove;
				nW += bbox.width;
				nH = Math.max(nH, bbox.height); nB = Math.max(nB, baseline);
			}
			start = end+1;	// back to ==m.offset

			// then handle all transitions at that point...
			for ( ; i<imax; i++) {
				m = sticky_[i]; if (!(m.getOwner() instanceof Span)) continue;
				if (m.offset > start) break;
				Span r = (Span)m.getOwner();
				Mark open = r.getStart();
				//if (open==null) { System.out.println("span.getStart() == null"); System.exit(1); }
				//if (open.leaf==null) { System.out.println("span.getStart().leaf == null  @ "+getName()+"/"+m.offset+", sticky #"+i); System.exit(1); }
				INode p = open.leaf.getParentNode();
				//String spanname = r.getName();
				if (m==open /*&& start==open.offset*/) {
					int lastsize=actives.size();	// save a stylesheet lookup if didn't have anything -- not worth it as lookup is cheap?
					/*if (spanname!=null)--should always be*/ss.activesAdd(actives, r, p);
					lastspan = actives.size()==lastsize? r: null;	// set to null so no intervening
					cx.addq(r);	// ad hoc spans not in style sheet
				} else {
					cx.removeq(r);
					if (r!=lastspan) ss.activesRemove(actives, r, p);
				}
				//System.out.println((m==r.getStart()?"add":"remove")+"Span "+r+" at "+this+"."+start+" => "+cx.vactive_);
			}
			cx.reset();	// make valid for next round or remainder (below)
		}

		// piece after all transitions (which may be entire node)
		end = size();
		if (!cx.elide/* && start < end*/)
			if (start < end || end==0/*special case for LeafText=="" and we want to see cursor*/) {
				formatNodeContent(cx, start, end);	// inclusive of endpoints
				//		if (start==end) { bbox.height=nH; baseline=nB; }
				//if (cx.spaceabove>0 || cx.spacebelow>0) System.out.println("at end of "+getName()+", height="+bbox.height+" vs "+nH+", above="+cx.spaceabove+", below="+cx.spacebelow);
				bbox.height += cx.spaceabove + cx.spacebelow;
				baseline += cx.spaceabove;
				nW += bbox.width;
				nH = Math.max(nH, bbox.height); nB = Math.max(nB, baseline);
			} else {	//if (start==end) {
				/*nW += 0;*/ nH += cx.spaceabove + cx.spacebelow; nB += cx.spaceabove;
			}

		bbox.setSize(nW,nH); baseline = nB;
		if (nW==0)
			bbox.height = baseline = 0;

		return false;
	}

	public boolean formatNodeContent(Context cx, int start, int end) { bbox.setSize(0,0); return false; }


	// following three methods shouldn't be defined (force subclasses to) but Happlet...


	/** The logical size of the node.  Spans can be anchored from 0..size(). */
	//	abstract int size();


	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		//if (!cx.g.hitClip(bbox.x, bbox.y, bbox.width, bbox.height) || (bbox.width==0 && bbox.height==0)) {
		if (!intersects(docclip) || bbox.width==0 && bbox.height==0) {
			//if (cx.valid && sticky_!=null && sticky_.size()>0) System.out.println("skipping leaf @ "+name_);
			if (stickycnt_ > 0) cx.valid=false;	// LATER: check for matches on same node
			//System.out.print("c");
			return;
		}

		//if (!cx.valid) System.out.println("resetting leaf @ "+name_);
		if (!cx.valid) cx.reset(this,-1);
		// later take into consideration prevailing scale in Context
		// (with inlined, specialized version of intersects)
		//System.out.println("me="+cbbox+" "+docclip);

		super.paintBeforeAfter(docclip, cx);	// before, node, after
	}


	/** Call paintNodeContent with longest range of unchanged Context. */
	/*final--ShadowLeaf/* until reason to do otherwise*/ @Override
	public void paintNode(Rectangle docclip, Context cx) {
		//if (bbox.width==0 /*||*/&& bbox.height==0) return; -- in INode
		//if (!cx.valid) cx.reset(this,-1); -- valid from paintBeforeAfter

		Color pbgin = cx.pagebackground;	//=> would like to handle backgrounds centrally here, but don't know width of leaf hunk until draw it
		cx.x = 0f; cx.y = 0f; cx.baseline = baseline + bbox.y;
		int start=0, end;
		boolean completecontext;

		Behavior[] paintbefore = stickycnt_==0? null: new Behavior[stickycnt_];
		int paintbeforecnt = 0;
		boolean elidebefore = cx.elide; float xbefore=0f;//bbox.x;	// special cases
		StyleSheet ss = cx.styleSheet;

		Span lastspan = null;
		List<ContextListener> actives = cx.vactive_;	// can't change during because don't recurse to anybody that recomputes
		for (int i=0,imax=stickycnt_; i<imax; ) {
			// maybe give ranges their own ArrayList<> subclass if casting is a performance problem

			Mark m = sticky_[i];
			Object owner = m.getOwner();
			if (owner==null || !(owner instanceof Behavior)) { i++; continue; }

			end = m.offset-1;

			// draw piece up to transition...
			if (!cx.elide && start <= end) {
				//x0=cx.x;
				cx.paintBefore(cx, this);
				//if (paintbeforecnt>0) System.out.print("content @ x="+cx.x);
				completecontext = paintNodeContent(cx, start, end);	// inclusive of endpoints
				//if (info) System.out.println("Jaramillo "+start+".."+end+", ss="+completecontext+", actives="+cx.vactive_);
				//if (paintbeforecnt>0) System.out.println(".."+cx.x);
				// where to handle visibility?
				if (!completecontext) cx.paintAfter(cx, this);
			}
			start = end+1;

			// handle span paintBefore's after having drawn corresponding content (tricky)
			if (!elidebefore && paintbeforecnt>0) {
				boolean tmpelide = cx.elide; cx.elide = elidebefore;
				float tmpx=cx.x; cx.x=xbefore;
				//System.out.println("paintbefores @ x="+cx.x);
				for (int j=0; j<paintbeforecnt; j++) paintbefore[j].paintBefore(cx, this);	// unfortunately cx has been changed by this point
				paintbeforecnt=0;
				cx.elide=tmpelide; cx.x=tmpx;
			}

			// then handle all transitions at that point...
			for ( ; i<imax; i++) {
				m = sticky_[i];
				if (m.offset > start) break;
				xbefore = cx.x;	elidebefore = cx.elide;

				owner = m.getOwner();
				if (owner==null) {
				} else if (owner instanceof Span) {
					Span r = (Span)m.getOwner();
					Mark open = r.getStart();
					//String spanname = r.getName();
					INode p = open.leaf.getParentNode();
					if (m==open /*&& start==open.offset*/) {
						//r.paintBefore(g, cx); -- can't paint here as the content will overpaint it
						/*if (paintbeforecnt<paintbefore.length)*/ paintbefore[paintbeforecnt++]=r;

						int lastsize=actives.size();
						ss.activesAdd(actives, r, p);
						lastspan = actives.size()==lastsize? r: null;
						//System.out.println("add "+r);
						cx.addq(r);

					} else {
						r.paintAfter(cx, this);
						cx.removeq(r);
						//System.out.println("remove "+r);
						if (r!=lastspan) ss.activesRemove(actives, r, p); //else System.out.println("saved remove lookup for "+r.getName());
					}

				} else if (owner instanceof Behavior) {
					Behavior be = (Behavior)owner;
					// so after gets done before before
					/*if (paintbeforecnt<paintbefore.length)*/ paintbefore[paintbeforecnt++]=(Behavior)owner;
					//be.paintBefore(g, cx, this);
					be.paintAfter(cx, this);	// seems not to get overwritten

				} // else String or something -- ignore
				//System.out.println((m==r.getStart()?"add":"remove")+"Span "+r+" at "+this+"."+start+" => "+cx.vactive_);
			}
			cx.reset();
		}

		// piece after all transitions (which may be entire node)
		end = size();
		if (!cx.elide && start<=end) {	// = to pick up cursor after all content(?)
			//x0=cx.x;
			cx.paintBefore(cx, this);
			cx.pagebackground = pbgin;
			completecontext = paintNodeContent(cx, start, end);	// inclusive of endpoints
			//if (info) System.out.println("Jaramillo "+start+".."+end+", ss="+completecontext+", actives="+cx.vactive_);
			// where to handle visibility?
			if (!completecontext) cx.paintAfter(cx, this);
		}


		// handle span paintBefore's after having drawn corresponding content (tricky)
		//System.out.println("paintbeforecnt = "+paintbeforecnt);
		if (!elidebefore && paintbeforecnt>0) {
			boolean tmpelide = cx.elide; cx.elide = elidebefore;
			float tmpx=cx.x; cx.x = xbefore>=bbox.width && xbefore>0? xbefore-1: xbefore;
			for (int j=0; j<paintbeforecnt; j++) paintbefore[j].paintBefore(cx, this);
			cx.elide=tmpelide; cx.x=tmpx;
		}
	}

	// paintNodeContent returns boolean to indicate whether it handles ALL context
	/*abstract*/ public boolean paintNodeContent(Context cx, int start, int end) { return false; }
	// ranges don't modify content--yet
	// usually computed from painted representation, though overridden by low-resolution Xdoc


	protected synchronized void subelement() {
		if (this!=Leaf.cacheLeaf_) {
			// using formatNodeContent, but don't leave lasting effects, so save current settings
			int ox=bbox.x, oy=bbox.y, owidth=bbox.width, oheight=bbox.height;
			int obaseline=baseline;
			boolean ovalid=valid_;

			StyleSheet ss = getDocument().getStyleSheet();
			//Context cx = (ss!=null? ss.getContext(null): new Context());
			Context cx = ss.getContext();
			//Context cx = new Context();
			//cx.styleSheet = getDocument().getStyleSheet();
			if (cx==null /*&& valid_*/) dump();
			//if (cx!=null) {	// not painted yet
			cx.reset(this,-1);
			subelementCalc(cx);

			bbox.setBounds(ox,oy, owidth,oheight); baseline=obaseline;
			valid_=ovalid;

			Leaf.cacheLeaf_ = this;
			//}
		}

		//return subelementHit(rel);
	}

	/**
	Given a geometric point within the leaf, return index of corresponding subcomponent (e.g., letter within word).
	See offset2rel().
	 */
	/* maybe default implementation for 0..1 elements like LeafImage, ... */
	/*!!!abstract*/public int subelementHit(Point rel) {
		subelement();
		return 0;
	}

	/**
	Given an offset into a Leaf, return corresponding subcomponent geometric point.
	See offset2rel().
	 */
	public Point offset2rel(int offset) {
		subelement();
		return new Point(0,0);
	}

	/**
	Media leaves override this to map an (x,y) point into an internal location.
	For example, ASCII maps the point into a character position. Since the internal
	location must be represented as an integer, an image could encode an internal
	(x,y) position as (y*width)+x
	 */
	public void subelementCalc(Context cx) {}



	/*abstract*/ @Override
	public void clipboardNode(StringBuffer sb) {}
	/*abstract*/ public void clipboardBeforeAfter(StringBuffer txt, int start, int end) {}



	/**
	Point can be null when it's a semantic event sent to tree nodes.
	 */
	@Override
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		if (rel!=null && !contains(rel)) return false;
		return super.eventBeforeAfter(e, rel);
	}

	/** unless overridden, leaves see if owner is interested */
	/*final--scrollbar*/ @Override
	public boolean eventNode(AWTEvent e, Point rel) {
		//if (e.getID()==MouseEvent.MOUSE_EXITED) return false;
		//int eid=e.getID();
		Browser br = getBrowser();	// climb up tree, but just for one leaf
		//if (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED || eid==TreeEvent.FIND_NODE) {
		if (rel!=null) {
			//System.out.println("hit leaf |"+getName()+"|, offset="+offset+", curn was "+br.getCurNode());
			//List<> vrangeActive =
			//System.out.println("cur = "+getName());
			Node curn = br.getCurNode();
			if (curn==null || !curn.isLeaf())
				//if (curn!=null) System.out.println("overriding "+curn.getName()+"/"+curn.getClass().getName()+" "+curn.bbox);	// overlaps in PDF (clip) and HTML (table, as in CUSG)
				br.setCurNode(this, subelementHit(rel));	// synthesizes MOUSE_ENTER and MOUSE_EXIT
			//System.out.println(getName()+", offset="+offset+", rel=("+rel.x+","+rel.y+")");

			//if (grab!=null) return grab.event(e, rel);	// grabs at leaf?
		}

		//* handle actives in Browser so can fake active spans when between leaf nodes
		//System.out.println("in leaf "+getName()+", event="+eid);
		//if (br.getGrab()!=null) System.out.println("grab = "+br.getGrab());
		if (br.getGrab()!=null) return false;

		/*
	// spans and structure and style sheet
	boolean shortcircuit=false;
	List<EventListener> vrangeActive = br.getActives();
//if (eid==MouseEvent.MOUSE_PRESSED) System.out.println("actives @ "+getName()+": "+vrangeActive);
	for (int i=0,imax=(vrangeActive!=null? vrangeActive.size(): 0); !shortcircuit && i<imax; i++) {
		EventListener rg = (EventListener)vrangeActive.get(i);
		shortcircuit = rg.event/*Before* /(e, rel);
	}
	//if (!shortcircuit) eventNodeContent(e);
//if (shortcircuit) System.out.println("SHORTCIRCUITED");

	return shortcircuit;
//    return false;
		 */
		//System.out.println("tickle "+getName());
		return br.tickleActives(e, rel, this);
	}



	/* *************************
	 *
	 * tree management: dirty bit, findDFS, cut/copy/paste
	 *
	 **************************/

	/**
	Remove node from tree tidily:
	don't leave behind empty INode, recursively up to <var>root</var>,
	and brush span transitions to the size ({@link Span#stretch(Leaf, INode)}).
	 */
	@Override
	public void removeTidy(INode root) {	// can't assume root same as getIScrollPane() (it's FixedI mediabox in PDF)
		Span.stretch(this, root);
		super.removeTidy(root);
	}

	@Override
	public void markDirtySubtreeDown(boolean leavestoo) {
		if (leavestoo) setValid(false);
	}


	public int lengthTo(int starti, Leaf endn, int endi) {
		int length=0;

		// for now, assume startn<endn; later compare and swap if necessary
		if (this==endn) length = endi-starti+1;
		else {
			length = size()-starti;
			for (Node n=getNextLeaf(); n!=endn; n=n.getNextLeaf())
				//System.out.println("leaf = "+n.getName());
				length += Leaf.INTERWORD_SPACE + n.size();	// space before this word + this word
			length += Leaf.INTERWORD_SPACE+endi;
		}

		return length;
	}


	/**
	Insert character at point.
	@return endpoint of inserted text, so if cursor at startoff, it should move to return value.
	 */
	public void/*Mark*/ insert(int startoff, char ch, INode bounds) {
		//if (ch==' ' && (startoff==0 || startoff==size())) return new Mark(;	// don't allow double spaces, yet
		//return
		insert(startoff, phelps.lang.Strings.valueOf(ch), bounds);
		//insert(startoff, String.valueOf(ch));
	}

	/**
	Insert string of possibly many words at point.
	Preserves validity of marks and spans.
	If inserting at cursor, cursor moved to point after inserted text (that may be too much policy, or centralizing that at the lowest level here is exactly the right place)..
	MOVE TO LeafText, since bogus for everything else (images, ...).
	 */
	public /*Mark*/void insert(int startoff, String txt, INode bounds) {
		assert startoff>=0 && startoff<=size() && txt!=null /*&& bounds!=null -- non-interactive editing, as from ReplaceWith*/: "startoff="+startoff+" <=? "+size()+", txt="+txt+", bounds="+bounds;

		String name = name_;	// handle to original
		//System.out.println("insert |"+txt+" into |"+name+"|");
		//setValid(false);
		markDirty();	//-- try to be more efficient, because some day will be editing cell in big complicated table
		Browser br = getBrowser();
		CursorMark curs=br.getCursorMark(); Mark cursm=curs.getMark();
		boolean cursset = curs.isSet() && this==cursm.leaf && startoff==cursm.offset;

		int firstspace=txt.indexOf(' '), lastspace=txt.lastIndexOf(' '), ilen=txt.length();
		//System.out.println("insert |"+txt+"| into |"+name+"|, fs="+firstspace+", ls="+lastspace);

		// Easy and most frequent case: no word breaks
		if (firstspace==-1) {
			setName(name.substring(0,startoff) + txt + name.substring(startoff));
			// push down span transitions past insertion point
			for (int i=stickycnt_-1; i>=0; i--) {
				Mark m = sticky_[i];
				if (m.offset >= startoff) m.offset += ilen;	//m.move(this, m.offset+len);
				else break;	// few stickies on a node so not much efficiency improvement, unless we switch to big leaves
			}
			//markDirtySubtree(); getBrowser().repaint(); -- works
			//reformat(this);	// or repaint(15);?
			getIScrollPane().repaint(25);
			if (cursset) { curs.move(this, startoff+ilen); return; }
			//return new Mark(this, startoff+ilen);
			return;
		}


		// General case: node split

		// append text before first space to end of leaf
		setName(name.substring(0,startoff) + txt.substring(0,firstspace));

		// prepend text after insertion point and new text after last space to start of next leaf
		Leaf lastn = getNextLeaf();
		if (lastn!=null && lastspace+1 < ilen && (bounds==null || bounds.contains(lastn))) lastn.setValid(false);
		else try { lastn = (Leaf)clone(); lastn.setName(""); } catch (CloneNotSupportedException bad1) {}
		lastn.setName(txt.substring(lastspace+1) + name.substring(startoff) + lastn.getName());	// all parts can be empty
		INode p = getParentNode();
		int num = childNum();
		p.insertChildAt(lastn, num+1);

		// adjust Marks: bump up marks on lastn...
		int len = name.length()-startoff + ilen - lastspace - 1;
		for (int i=0, imax=lastn.sizeSticky(); i<imax; i++) lastn.getSticky(i).offset += len;
		// ... and move Marks after startoff to new last (same parents as before so don't have to worry about span summaries)
		for (int i=stickycnt_-1; i>=0; i--) {
			Mark m = sticky_[i];
			if (m.offset >= startoff) m.move(lastn, len);
			else break;
		}
		//Mark endpoint = new Mark(lastn, ilen-lastspace-1);
		if (cursset) curs.move(lastn, ilen-lastspace-1);

		//System.out.println("new endpoint = "+lastn.getName()+"/"+len);

		// new simple nodes for inbetween
		for (int i=firstspace+1, ins=num+1; i<lastspace; ins++) {
			int nexti = txt.indexOf(' ', i);
			if (nexti > i+1) try {	// space-space
				Leaf l = (Leaf)clone();
				l.setName(txt.substring(i, nexti));
				p.insertChildAt(l, ins);
			} catch (CloneNotSupportedException bad2) {}
			i = nexti+1;
		}

		getIScrollPane().repaint(25);
		//return endpoint;
		/*
	// and add lots of new words (balanced elsewhere, some day)
	if (lastspace!=firstspace) {
		StringTokenizer st = new StringTokenizer(txt.substring(firstspace+1,lastspace));
		for (int i=childNum()+1; st.hasMoreTokens(); i++) {
			//RECOVER:	newnode = (Leaf)this.clone(); newnode.sticky_=null;
			newnode.setName(st.nextToken());
			parent_.insertChildAt(newnode, i);
		}
	}

	Leaf newlastn=null, newnode=null;

//	  if (lastspace+1<txt.length() || startoff<name.length()) {
	//not used: if (startoff==name.length()) newlastn=getNextLeaf();

	if (startoff==name.length()) {	// keep as separate word?
		newlastn=getNextLeaf();
		//RECOVER:	newlastn = (Leaf)this.clone(); newlastn.sticky_=null; newlastn.bbox.x++;
		newlastn.setName(txt.substring(lastspace+1));
	} else {
		//RECOVER:	newlastn = (Leaf)this.clone(); newlastn.sticky_=null; newlastn.bbox.x++;
		try {
			//newlastn = (Leaf)clone(); newlastn.sticky_=null; newlastn.bbox=new Rectangle(); newlastn.setValid(false);
			newlastn = (Leaf)clone();
			newlastn.setName(txt.substring(lastspace+1) + name.substring(startoff));
		} catch (CloneNotSupportedException cant) { System.out.println("can't clone anymore guys!"); }
	}

	// split node, so move span endpoints
	for (int i=(sticky_==null?-1:sticky_.size()-1); i>=0; i--) {
		Mark m = (Mark)sticky_.get(i);
		if (m.offset >= startoff) m.move(newlastn, m.offset-startoff);	// handles case where newlastn=getNextLeaf()
	}
	parent_.insertChildAt(newlastn, childNum()+1);
//parent_.dump();
//	}


	//markDirtyTo(newlastn); getBrowser().repaint();	// make more efficient
	//markDirtyTo(newlastn); -- leaves already marked dirty and don't want to mark parents

	Context cx = new Context();
	cx.styleSheet = getDocument().getStyleSheet(); cx.reset(this);
	Node n=this, endn=newlastn.getNextLeaf();
	do {
		n.setValid(false); n.formatBeforeAfter(n.bbox.width,n.bbox.height, cx);	// fix WxH
		n = n.getNextLeaf();
	} while (n!=endn);
	//*parent_.setValid(false);* / parent_.reformat(this);	// fix (x,y)
	//parent_.reformat(null);
		 */
	}



	/**
	Append leaf to the end of this one, adjusting content, spans, and so on.
	The appended leaf must either immediately follow this one in the tree, or be unattached to the tree.
	The appended leaf should be considered invalid and not used subsequently.
	 */
	public void append(Leaf l) {
		//System.out.println("append: "+getName()+" <= "+l.getName());
		//l.delete();
		//if (l.getName()!=null) name_ += l.getName(); => text only
		// merge attr_ ?
		// keep parent, align, valign
		if (l.bbox!=null) bbox = bbox.union(l.bbox);
		//baseline = Math.max(baseline, l.baseline); => assume same baseline
		for (int i=l.sizeSticky()-1; i>=0; i--) {	// shift after move each sticky
			Mark m = l.getSticky(i);
			int newoff = size() + m.offset;
			/*Object o = m.getOwner();
			if (o instanceof Span) { => after l.delete(), spans begin and end on l
			Span s = (Span)o;
			s.moveq(this,newoff, s.getEnd().leaf, s.getEnd().offset);
		} else*/ m.move(this, newoff);	// since follow or unattached, can just move Span attachment points
		}
		assert l.sizeSticky() == 0: l.sizeSticky();
		setValid(false);
	}


	/**
	Split this node into two nodes starting at same position under current parent,
	and handling content, spans, and so on.
	 */
	public void split(int off) {
		assert off>=0 && off<=size(): "split @ "+off+" <=? "+size();

		INode p=getParentNode(), pp=p.getParentNode();
		INode newp=null;
		try { newp=(INode)p.clone(); } catch (CloneNotSupportedException cant) { System.out.println("can't clone anymore guys! "+cant); System.exit(1); }
		newp.setAttributes(null);	// same GI
		pp.insertChildAt(newp, p.childNum()+1);
		//newp.removeAllChildren();
		//pp.dump(); System.exit(0);

		insert(off, ' ', null);	// split current node (adjusting spans) -- have to change this later when have longer nodes
		Leaf n = getNextLeaf();
		//RECOVER:	INode newp = (INode)p.clone();	// new structural of same type
		//pp.dump(); System.exit(0);

		for (int point=n.childNum(),i=point,imax=p.size(); i<imax; i++) {
			Node child = p.childAt(point);
			//p.removeChildAt(point);
			newp.appendChild(child);
		}
		//pp.markDirtySubtree(true);
		//pp.dump(); System.exit(0);

		getBrowser().repaint(100);
		//reformat(null);
		//	  cur.move(+1); curn=cur.getStart().leaf; curp=curn.getParentNode(); -- leave to client
	}


	// delete range, tending to marks/spans in between
	// later need to do node balancing of some sort.	handle zapped parents at least
	public void delete(int startoff, Leaf endn,int endoff) {
		assert startoff>=0 && startoff<=size(): "delete @ "+startoff+" <=? "+size();
		assert endn!=null && endoff>=0 && endoff<=endn.size(): "end "+endn+" delete @ "+endoff+" <=? "+(endn!=null? size(): -1);

		/*String ignore =*/ eatme(startoff, endn,endoff);
		//CursorMark cur = getBrowser().getCursorMark();
		//if (cur.isSet()) cur.move(startn, startoff);
	}


	public String cut(int startoff, Leaf endn,int endoff) {
		assert startoff>=0 && startoff<=size(): "cut @ "+startoff+" <=? "+size();
		assert endn!=null && endoff>=0 && endoff<=endn.size(): "end "+endn+" cut @ "+endoff+" <=? "+(endn!=null? size(): -1);

		String cuttxt = eatme(startoff, endn,endoff);
		//	getBrowser().setSelection(cuttxt);
		StringSelection ss = new StringSelection(cuttxt);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);

		return cuttxt;
	}

	// handling the marks makes this tricky
	// should save spans for undo, paste
	// "cut" => zap and move clipboard to caller, "eatme" => "cut",
	/*protected--needed by editor bindings*/public String eatme(int startoff, Leaf endn,int endoff) {
		assert startoff>=0 && startoff<=size(): "eatme @ "+startoff+" <=? "+size();
		assert endn!=null && endoff>=0 && endoff<=endn.size(): "end "+endn+" eatme @ "+endoff+" <=? "+(endn!=null? size(): -1);

		// validity checking
		//System.out.println("eatme "+getName()+"/"+startoff+" .. "+endn.getName()+"/"+endoff);
		//System.out.println("eatme "+getName()+"/"+startoff+" .. "+endn.getName()+"/"+endoff);
		if (endn==null) return "";
		if (this==endn && startoff==endoff) return "";	// return null?

		Leaf startn=this;
		String cuttxt;
		Browser br = getBrowser();
		INode scope = br!=null? br.getScope(): null;	// possible to edit fragment w/o browser hook?
		Leaf rightsib = startn.getNextLeaf();
		//if (endoff==0) { endn=endn.getPrevLeaf(); endoff=endn.size(); } -- couldn't delete across the space
		CursorMark curs=br.getCursorMark();	//Mark curm=curs.getMark();
		Span sel = br.getSelectionSpan();
		boolean cursset=curs.isSet() || sel.isSet();	//&& this==curm.leaf && startoff==curm.offset; //|| sel.isSet();	// reestablish cursor afterwards if either was set
		curs.move(null); /*sel.move(null);*/	// get them out of the way
		if (sel.contains(this,startoff) || sel.contains(endn,endoff)) sel.move(null);

		markDirty();  endn.markDirty();	// zapping inbetween nodes triggers dirty in parent, but editing node's content doesn't


		// first tend to marks
		Map<Span,Mark> startm = new HashMap<Span,Mark>(10);
		String starttxt=startn.getName(), endtxt=endn.getName();

		int minoff=startoff, maxoff=startn==endn?endoff:Integer.MAX_VALUE;
		for (Node n=startn, endloop=endn.getNextLeaf(); n!=endloop; n=n.getNextLeaf(), minoff=0, maxoff=n==endn?endoff:Integer.MAX_VALUE)
			for (int i=n.sizeSticky()-1; i>=0; i--) {
				Mark m = n.getSticky(i);
				if (m.offset<minoff || m.offset>maxoff) continue;
				if (m.getOwner() instanceof Span) {
					Span r = (Span)m.getOwner();
					if (m == r.getStart()) startm.put(r,m);	// haven't seen end as start always comes before end
					else if (startm.get(r)!=null) { startm.remove(r); r.destroy(); }	// seen the pair, zap it
					else m.move(startn,startoff);	// can safely move these right now
				} else m.remove();
			}

		// merge start and end nodes -- this assumes text if partial leaves!
		//String mergetxt = starttxt.substring(0,startoff) + endtxt.substring(endoff);
		//if (startoff>0) startn.setName(mergetxt); else if (endoff<endn.size()) endn.setName(mergetxt); // else they're both completely zapped
		startn.setName(starttxt.substring(0,startoff) + endtxt.substring(endoff));
		boolean zap = size()==0;

		if (startn==endn) {
			cuttxt = starttxt.substring(startoff,endoff);
			// spans that started but did not end in range
			for (Span r : startm.keySet())
				if (zap) r.destroy(); else r.getStart().move(startn,startoff);
			if (zap) { startn.remove(); startn=null; }

		} else {
			StringBuffer cutbuf = new StringBuffer(100);
			cutbuf.append(starttxt.substring(startoff));

			// zap in between text
			for (Node n=startn.getNextLeaf(),next; n!=endn; n=next) {
				cutbuf.append(' ').append(n.getName());
				next=n.getNextLeaf(); n.remove();
			}

			// move existing Marks on endn
			for (int i=endn.sizeSticky()-1; i>=0; i--) {
				Mark m = endn.getSticky(i);
				if (m.offset>=endoff) m.move(m.leaf, m.offset-endoff+startoff); else m.remove();
			}
			// starts go end of deleted region (ends go to start of deleted region -- already done)
			for (Span r : startm.keySet())
				r.getStart().move(startn,startoff);
			cutbuf.append(' ').append(endtxt.substring(0,endoff));
			cuttxt = cutbuf.toString();

			if (zap) { startn.remove(); startn=null; }
			endn.remove();	// merging start and end -- start may have become new end
		}


		// if deleted all leaves in scope, put in empty node
		if (scope!=null && scope.getFirstLeaf()==null)
			for (INode pbot=scope, pnext=scope; pnext!=null; pbot=pnext) {
				pnext = (INode)pbot.getFirstChild();
				if (pnext==null) {
					//Leaf newn = (Leaf)clone(); -- should do this, probably
					startn = this;	// don't assume it's LeafUnicode, and don't depend on LeafUnicode
					name_=""; setAttributes(null);
					pbot.appendChild(this);
				}
			}


		// reset cursor
		if (cursset)
			if (startn==null) {
				if (rightsib!=null) curs.move(rightsib,0);	// else leftsib?...
				//br.repaint();
			} else
				curs.move(startn,startoff);
		//startn.setValid(false); startn.reformat(this);

		getParentNode().repaint();	// marked dirty, trigger reformat and redisplay

		return cuttxt;
	}


	public String copy(int startoff, Leaf endn,int endoff) {
		assert startoff>=0 && startoff<=size(): "copy @ "+startoff+" <=? "+size();
		assert endn!=null && endoff>=0 && endoff<=endn.size(): "end "+endn+" copy @ "+endoff+" <=? "+(endn!=null? size(): -1);

		Leaf startn=this;
		String cuttxt;

		if (startn==endn)
			cuttxt = startn.getName().substring(startoff,endoff);
		else {
			StringBuffer cutbuf = new StringBuffer(100);
			cutbuf.append(startn.getName().substring(startoff));
			for (Node n=startn.getNextLeaf(); n!=endn; n=n.getNextLeaf()) cutbuf.append(n.getName());
			cutbuf.append(endn.getName().substring(0,endoff));
			cuttxt = cutbuf.toString();
		}
		return cuttxt;
	}

	public void paste(int startoff) {
		Leaf startn=this;
		// get text from system clipboard
		String txt = "waiting for Java 1.1 to get text from system clipboard";
		startn.paste(txt, startoff);
	}
	public void paste(String txt, int startoff) { this.insert(startoff, txt, null); }


	/*
  public void dump(int level) {
	for (int i=0; i<level; i++) System.out.print("	");
	System.out.println(name);
	//behavior.toString(cookie);
  }
	 */

	@Override
	public boolean checkRep() {
		assert super.checkRep();

		assert getName()!=null;	// All leaves must have non-null names (HTML images can use accessibility ALT name)

		for (int i=0,imax=stickycnt_; i<imax; i++) {
			Mark m = sticky_[i];
			assert m.leaf == this;
			assert m.offset >= 0 && m.offset <= size();
			Object owner = m.getOwner();
			if (owner instanceof Span) {
				Span span = (Span)owner;
				assert span.isSet();
				assert span.checkRep();
			}
		}


		return true;
	}

}
