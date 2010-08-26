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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import multivalent.std.span.AnchorSpan;
import multivalent.std.span.AwkSpan;
import multivalent.std.span.HyperlinkSpan;
import multivalent.std.span.InsertSpan;
import multivalent.std.span.ReplaceWithSpan;
import multivalent.std.ui.DocumentPopup;
import phelps.doc.RobustLocation;
import phelps.lang.Integers;
import uk.ac.liverpool.fab4.behaviors.CitesSpanNote;
import uk.ac.liverpool.fab4.behaviors.TextSpanNote;

/* Changed the dialogs, to replace the old one. See semanticEventAfter() */

/**
	A Span (aka Range aka Tk tag), such as a hyperlink or executable copy editor mark,
	is a linear range of content that can control appearance and receives events.
	It efficiently covers any amount of tree, not restricted by structural node hierarchy.
	Spans are robust to insertions and deletions, during runtime and across save/restore editing of base document.

	<p>Span is not an abstract class, and in fact should be heavily used itself.
	It can be used to store metadata, via attributes.
	It can be created with a semantic name and its properties set in the style sheet.
	If a style sheet is independent of a style sheet (as with a user annotation), one should use a "general span".
	If different behavior is needed, as opposed to appearance, then subclasses are appropriate.

	<ul>
	<li>moving: {@link #open(Node)}, {@link #close(Node)} / {@link #closeAll(Node)},
		{@link #move(Leaf,int, Leaf,int)} / {@link #move(Span) Span} / {@link #move(Mark, Mark) Mark},
		{@link #moveq(Leaf,int, Leaf,int)} / {@link #moveq(Span) Span} / {@link #moveq(Mark, Mark) Mark}
	<li>relationship to tree: {@link #getStart()}, {@link #getEnd()}, {@link #isSet()},
		{@link #contains(Node,int)} / {@link #contains(Mark) Mark}
	<li>editing-related: {@link #stretch(Leaf,INode)},
	<li>display and events: {@link #getPriority()}, {@link #appearance(Context, boolean)},
		{@link #repaint()} / {@link #repaint(long) long}, {@link #markDirty()},
	<li>events: {@link #getPriority()}, {@link #event(AWTEvent)}
	</ul>


<!--
	<p>To do
	<ul>
	<li>Later have subclass that takes appearance parameters, creates new instance for each unique combination thereof (one for String owners, one for Behavior owners)
	(need to have boolean/distinguished value for each value so know whether to set it or not, as in Tk)
	<li>keep list of ranges so can quickly go to next, prev range?  probably not => this is really fast anyhow, and don't want bookkeeping headache
	<li>if can't reattach, put in global LOSTSPAN Map (create if first entry) => report via semantic event
	</ul>

<p>Most of the hard work for spans subclasses is done here, such
a registering in the tree and robustly saving and restoring the span
position.  Subclasses should be certain to call
<tt>super<i>.protocol</i></tt> when overriding a protocol.
During the extent of a span, other spans may begin and end,
and the span resets the graphics context as desired in its <tt>appearance</tt>
method, with its self-described priority relative to other spans
given by <tt>getPriority</tt>.

No author/... fields for annotations: can add as attribute, perhaps on given layer where all done by same author

-->
	@see multivalent.Mark

	@version $Revision$ $Date$
 */
public class Span extends Behavior implements ContextListener, EventListener {
	static final boolean DEBUG = false && multivalent.Meta.DEVEL;


	/**
	For use in interactive editing, deletes span, moving selection to old extent.
	<p><tt>"deleteSpan"</tt>.
	 */
	public static final String MSG_DELETE = "deleteSpan";

	/**
	For use in interactive editing, moves Span to extent of selection.
	<p><tt>"morphSpan"</tt>.
	 */
	public static final String MSG_MORPH = "morphSpan";

	/**
	Request for interactive editing of span attributes.
	<p><tt>"editSpan"</tt>.
	 */
	public static final String MSG_EDIT = "editSpan";

	/**
	Announce span that could not be reattached with confidence.
	<p><tt>"unattachedSpan"</tt>: <tt>arg=</tt> {@link Span} <var>instance</var>.
	 */
	public static final String MSG_UNATTACHED = "unattachedSpan";


	public static final String GI_START = "start";
	public static final String GI_END = "end";


	// should check that all pending spans eventually closed
	private static Map<Span,SoftReference<Node>> pending__ = new IdentityHashMap<Span,SoftReference<Node>>(10);	// pending open() spans, which requires client to close() all open(), but that's like File.close() so ok



	private final Mark start_ = new Mark(null,-1, this);	// owner assigned here (this, null, String)
	private final Mark end_ = new Mark(null,-1, this);	// (marks attached as part of creation)

	// maybe just leave in attributes
	public Map<String,Object> pstart=null;	// keep persistent hooks around to update positions when save -- needed by RestoreReport
	public Map<String,Object> pend=null;


	public boolean modified = false;


	/** (Node, offset) of start of span. */
	public final Mark getStart() { return start_; }	// should make defensive copies

	/** (Node, offset) of end of span. */
	public final Mark getEnd() { return end_; }

	/** Is Span attached to tree? */
	public boolean isSet() { return start_.leaf!=null && end_.leaf!=null; }



	// for ContextListener
	//public boolean paintBefore(Context cx, Node n) { return false; }
	//public boolean paintAfter(Context cx, Node n) { return false; }
	public int getPriority() { return ContextListener.PRIORITY_SPAN; }

	public boolean appearance(Context cx, boolean all) { return false; }


	/**
	During document creation, open span at first leaf to be created after passed node.
	Typically, document formats begin spans before the content to which they apply has been seen,
	which means bookkeeping for the span name, type, start point, attributes and so on for when then end of span is seen,
	and adjusting the start point to the first leaf created after last node known at <tt>open</tt>.
	Instead, media adaptors can create the span and configure it and set the start point with <tt>open</tt> with
	the last node created, which can be an internal node, and {@link #close(Node)} adjusts the start point.
	If both start and end points already exist, use {@link #moveq(Leaf,int, Leaf,int)}.
	All spans <code>open</code>ed should be <code>close</code>d.

	@see multivalent.std.adaptor.HTML and
	@see multivalent.std.adaptor.pdf.PDF for examples of use
	 */
	public void open(Node/*not necessarily a Leaf*/ start) {
		//assert start!=null; => null is first leaf in Document
		assert Span.pending__.get(this)==null: "double open()";

		Mark s = getStart();
		//while (s.isStruct()) { Node last=((INode)s).getLastChild(); if (last!=null) s=last; else break; } => may delete some (empty) nodes as build tree
		//X s.leaf = start;	=> can't do this because start isn't necessarily a Leaf and eventual leaf perhaps not yet created! ...
		Span.pending__.put(this, new SoftReference<Node>(start));	// ... so store start outside of span.  SoftRef in case never see close() can gc doubly-linked doc tree (no danger of bad gc'ed when correctly used).
		s.offset = start!=null? start.size(): 0;
		//System.out.print("open |"+start+"|/"+s.offset);
	}

	/**
	Close span at end of passed Node, and attach Span to tree.
	Since this is used during tree construction, no nodes are marked dirty as that would be redundant.
	If the span cannot be attached, the caller probably wants to {@link #destroy()} it;
	however, the HTML media adaptor takes the case of HTML 0-length <code>&gt;a name=...&lt;</code> spans
	and instead attaches and 'id' attribute to the previous node.
	@return true if attached span, false if couldn't attach or 0-length
	 */
	public boolean close(Node end) {
		//assert end!=null;
		if (end==null) /*moveq(null,-1,null,-1)? log.warning("...")*/ return false;	// aborted and cleaning up, or nulling span

		SoftReference<Node> ref = Span.pending__.remove(this); assert ref!=null: "no open()";
		Node s = ref.get()/*not getStart().leaf*/, e=end;
		//log.finest("attach "+getName()+": "+s+"/"+(s!=null? s.size(): -1)+" .. "+e+"/"+(e!=null? e.size(): -1));
		//X if (s==null) return false;	// aborted and cleaning up, but cleaned up tree first => indistinguishable from unknown start, so assume used correctly
		if (s==null) s = e.getDocument().getFirstLeaf();
		int si = getStart().offset;

		boolean fsuccess = false;
		if (s!=null && e!=null) {
			Leaf el = el = e.getLastLeaf();
			int ei = el!=null? el.size(): -1;

			Leaf sl;
			if (s.isStruct()) {
				//System.out.print("\tstruct "+s+" "+si+" vs "+s.size());
				if (si < s.size()) sl = ((INode)s).childAt(si).getFirstLeaf();	// subtree now instantiated
				else if (si == s.size()) {
					//assert si == 0;	// generally true but make allowance for caller
					s = ((INode)s).getLastChild();
					if (s!=null) sl=s.getFirstLeaf(); else sl=null;
					//sl = s.getFirstLeaf();
				} else {	// no such subtree, to get last in this and jump over to next subtree
					//s = s.getLastLeaf();
					//if (s.getNextLeaf()!=null) s = s.getNextLeaf();
					//s = s.getLastLeaf().getNextLeaf();
					s = s.getNextNode();
					if (s!=null) sl = s.getFirstLeaf(); else sl=null;
				}
				//System.out.println(" => "+sl);
				si = 0;

			} else { assert s.isLeaf();
			//System.out.println("\tleaf: "+s+"/"+si+" .. "+el+"/"+ei+", "+(s==el));
			//if (si >= s.size()) { sl = s.getNextLeaf(); si=0; }
			if (si < s.size()) sl=(Leaf)s;
			else if (s == el) { sl=(Leaf)s; si=ei; }	// probably error in caller
			else {
				Leaf next = s.getNextLeaf();
				/*if (next!=null && next.getName().equals("")) {s.getDocument().dump();
System.out.println("next = |"+next.getName()+"|, same doc? "+(s.getDocument()==next.getDocument()));
System.exit(0); }*/
				if (next!=null && s.getDocument()==next.getDocument()/*in same tree*/) { sl=next; si=0; }
				else { sl=(Leaf)s; si=s.size(); }
			}
			//else { sl=el; si=el.size(); }
			//System.out.println(" => "+sl+"/"+si);
			} // else concatenated and correct as is

			//if (sl==null || el==null) System.out.println("point attachment "+this+" "+this.getAttributes()+"   "+sl+"/"+el);
			//System.out.println(" => "+sl+"/"+getStart().offset+"=>"+si+".."+el+"/"+el.size());
			assert sl==null || el==null || Node.cmp(sl,si, el,ei, sl.getDocument())!=1: s+"=>"+sl+" > "+el+" for "+getName()+" "+getAttributes();
			//if (sl==null || el==null) System.out.println("0-length: "+getName());
			if (sl!=null && el!=null) { moveq(sl,si, el,ei); fsuccess=true; }
			else if (sl!=null) moveq(sl,si, sl,si);
			else if (el!=null) moveq(el,ei, el,ei);
			//else System.out.println("no attachment!  "+s+"/"+si+"=>"+sl+"  "+(sl!=null?sl.getAttributes():null)+"   ..   "+e+"=>"+el+"  "+(el!=null?el.getAttributes():null)/*+"/"+e.size()*/);
		}

		//System.out.println("attached to "+getStart().leaf+"/"+getStart().offset+" .. "+getEnd().leaf+"/"+getEnd().offset);
		return fsuccess && isSet();
	}

	/**
	Close all spans in <var>subtree</var>.
	Useful in a couple ways:
	for a media adaptor to conveniently and concisely close all pending spans at the end of a document, and
	to guarantee that all spans are closed even if an explicit end to the span is missing in the concrete document.
	@return count of spans successfully attached to tree
	 */
	public static int closeAll(Node subtree) {
		int cnt = 0;

		Leaf end = subtree.getLastLeaf();
		if (end != null) synchronized (Span.pending__) {
			List<Span> l = new ArrayList<Span>(10);

			// first collect...
			for (Entry<Span, SoftReference<Node>> e : Span.pending__.entrySet()) {
				Node n = e.getValue().get();
				//X if (n==null) i.remove(); => can't distinguish gc from valid-null
				if (n!=null && subtree.contains(n))
					//System.out.println("close "+e.getKey());
					l.add(e.getKey());
				//((Span)e.getKey()).close(end) => ConcurrentModificationException
			}

			// ... then close
			for (Span span : l) {
				boolean ok = span.close(end);
				if (ok) cnt++;
			}
		}

		return cnt;
	}

	/** For debugging, dump all spans that have been {@link #open(Node, int)}ed but not {@link #close(Node)}ed.
  public static void dumpPending() {
	if (pending__.size()>0) {
		System.out.print("pending spans: ("+pending__.size()+") ");
		for (Iterator<Map.Entry<>> i=pending__.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<> e = i.next();
			System.out.print("   "+e.getKey().getClass().getName()+" = "+e.getValue());
		}
		System.out.println();
		//System.exit(1);
	}
  }*/


	/**
	Save move, for interactive use: selection, annotations.
	Sets start point < end point, handles necessary reformatting and repainting.
	Reformats both old range and new range, as with style sheets and hardcode in subclasses, can't tell when that's not necessary.
	(Or, moveq() + swapping endpoints if necessary + reformatting/repainting.)
	 */
	public void move(Leaf ln,int lo, Leaf rn,int ro) {
		markDirty();	// old position

		moveqSwap(ln,lo, rn,ro);

		//if (notemark!=null) notemark.moveq(ln,lo, rn,ro);	// Notemark dominates span

		// new format, paint
		//if (this!=br.getSelectionSpan() && this!=br.getCursorMark()) { => now in subclasses
		markDirty();	// new position

		Browser br=getBrowser(); if (br!=null) br.repaint(100);	// can't just reformat+repaint old and new positions because reformatting may change locations
	}

	/** Span endpoints assumed valid; just moveq + reformatting and repainting. */
	public void move(Span s) {
		// assume span valid -- just mark dirty and repaint services
		if (isSet()) markDirty();	// old position
		moveq(s);
		if (isSet()) markDirty();	// new position
		Browser br=getBrowser(); if (br!=null) br.repaint(100);
	}

	public void move(Mark l, Mark r) {
		if (l==null || r==null) move(null); else move(l.leaf,l.offset, r.leaf,r.offset);
	}

	/**
	Like moveq(), swapping endpoints if necessary, so that left comes before right in tree order.
	 */
	public void moveqSwap(Leaf ln,int lo, Leaf rn,int ro) {	// you guarantee ln<rn || (ln==rn && li<ri)
		// if (ln,li) comes after (rn,ri), swap 'em
		//if ((ln==rn && lo>ro) || (ln!=rn && Node.cmp(ln,lo, rn,ro)>0)) { -- fast paths
		Document doc = ln.getDocument();	// null if behavior not instantiated properly
		if (Node.cmp(ln,lo, rn,ro, doc)>0) {
			Leaf ntmp=ln; ln=rn; rn=ntmp;
			int otmp=lo; lo=ro; ro=otmp;
		}

		moveq(ln,lo, rn,ro);	// handles summaries
	}

	/**
	"move quick" just updates summaries, so more efficient than move(), if caller:
	<ol>
	<li>guarantees that first node comes before second node (so the system doesn't have to spend time checking this), and
	<li>handles all reformatting and repainting (including marking tree nodes dirty)
	</ol>
	Useful using during initial tree builds, when adding spans and formatting in batch(es).
	Checks that endpoint offsets are valid, that is, >=0 and <size();
	if you want to achieve (temporarily!) invalid states, manipulate the endpoint Marks directly.
	 */
	public void moveq(Leaf ln,int lo, Leaf rn,int ro) {	// you guarantee ln<rn || (ln==rn && li<ri)
		//	if (ln==rn && lo==ro) { remove(); return; }	// 0-length span
		//	  if (ln==null || (ln==rn && lo==ro)) { remove(); return; }	// 0-length span
		/* belongs in a move, not moveq
	if (ln==null || rn==null || (ln==rn && lo==ro)) {
		if (ro<ln.size()) ro=lo+1; else { rn = rn.getNextLeaf(); ro=0; }
	}*/


		Node oldstart=start_.leaf, oldend=end_.leaf;
		int oldstartoff=start_.offset, oldendoff=end_.offset;

		// move to same spot => no work
		if (ln==oldstart && lo==oldstartoff && rn==oldend && ro==oldendoff) return;

		// just swapping endpoints
		if (ln==oldend && lo==oldendoff && rn==oldstart && ro==oldstartoff) {
			//Mark tmpmark=start_; start_=end_; end_=tmpmark; => start_, end_ final
			Leaf tmpleaf=start_.leaf; int tmpoffset=start_.offset; start_.leaf=end_.leaf; start_.offset=end_.offset; end_.leaf=tmpleaf; end_.offset=tmpoffset;
			return;
		}


		// zap old summaries
		//int oldstartoff=-1, oldendoff=-1;
		if (oldstart!=null && oldend!=null) {	// ==null when create, which is most common case since don't often move spans once created
			INode oldstartp=oldstart.getParentNode(), oldendp=oldend.getParentNode();
			if (oldstartp!=oldendp) { removeSummary(start_, oldstartp); removeSummary(start_/*not end--must be same as other removeSummary*/, oldendp); } //update(oldstartp, doc, false); update(oldendp, doc, false); }
		}

		// new location -- moveq assumes whatever was requested is correct, for possible low-lever trickiness as during editing
		if (ln!=null && rn!=null) {
			// check for out-of-bounds endpoints
			int lomax=ln.size(), romax=rn.size();
			if (ro>romax) ro=romax;
			if (lo>lomax) lo=0;	// maybe only if ln==rn?

			// marks
			//if (ln!=oldstart || lo!=oldstartoff)
			if (ln!=rn || lo!=ro) {	// HTML has 0-length "a name", but that should be converted to 'id' on nearest node
				start_.move(ln,lo);
				//if (rn!=oldend || ro!=oldendoff)
				//if (ro > rn.size()) { rn=rn.getNextLeaf(); ro=0; }	// ro=rn.size()?
				//if (ro > rn.size()) ro=rn.size();
				end_.move(rn,ro);
			}

			// new summaries
			INode lnp=ln.getParentNode(), rnp=rn.getParentNode();
			//System.out.println("l/r parents: "+ln+"/"+lnp+"   "+rn+"/"+rnp);
			INode stop = getDocument();	// could use commonAncestor--in expensive case, which more expensive: computing common, or adding and removing summaries
			if (lnp!=rnp/*saves a lot of work*/) { addSummary(start_, lnp, stop); addSummary(start_/*not end--same as other adsSummary()*/, rnp, stop); } // update(lnp, doc, true); update(rnp, doc, true); }

		} else {
			// record marks only
			start_.remove(); start_.leaf=ln; start_.offset=lo;
			end_.remove(); end_.leaf=rn; end_.offset=ro;
		}
	}


	public void moveq(Mark start, Mark end) {
		moveq(start!=null? start.leaf: null, start!=null? start.offset: 0,  end!=null? end.leaf: null, end!=null? end.offset: 0);
	}

	/** Useful to morph <i>to</i> the selection. */
	public void moveq(Span span) {
		if (span==null)
			moveq(null,start_.offset, end_.leaf,end_.offset);	// move offscreen, but keep in layer.  e.g., Cursor and Selection
		else {
			Mark s=span.getStart(), e=span.getEnd();
			//		moveq(s.leaf,s.offset, e.leaf,e.offset);	=> have to handle reformat, repaint
			moveq(s.leaf,s.offset, e.leaf,e.offset);	// known good, so moveq
		}
	}


	/** Is (Node, offset) contained within span? */
	public boolean contains(Node n, int off) {
		if (n==null || off<0) return false;
		assert /*n!=null &&*/ off>=0 && off<=n.size(): n+" / "+off;
		if (!isSet() /*|| n==null || off<0 || off>n.size()*/) return false;

		if (n.isStruct()) { n=n.getFirstLeaf(); off=0; }
		Node sn=getStart().leaf, en=getEnd().leaf;
		int si=getStart().offset, ei=getEnd().offset;

		// fast paths
		if (sn==en) return n==sn && si<=off && off<=ei;
		else if (n==sn) return si<=off;
		else if (n==en) return off<=ei;

		// in subtree?
		INode top = (INode)sn.commonAncestor(en);	// must be INode because start and end nodes known to be different
		if (!top.contains(n)) return false;

		// Don't march along every leaf in span.
		// already checked endpoints, so don't have to worry about offsets
		return Node.cmp(sn,si, n,off, top)==-1 && Node.cmp(n,off, en,ei, top)==-1;
	}

	public boolean contains(Mark m) {
		//assert m!=null;
		return m!=null? contains(m.leaf, m.offset): false;
	}

	/**
	Removing leaf from tree, but preserve its span transitions
	by stretching end transitions to previous node and start transitions to next node.
	If there is no previous node for and end transition, or next node for a start, the span is removed.
	 */
	public static void stretch(Leaf l, INode within) {
		assert l!=null;
		//System.out.println("stretch "+l.getName()+" "+l.sizeSticky()+" ");
		if (l.sizeSticky() == 0 /*l.getParentNode()==null => ok, just remove all spans*/) return;

		Leaf prevl = l.getPrevLeaf(), nextl = l.getNextLeaf();
		if (within!=null) {
			if (!within.contains(prevl)) prevl=null;
			if (!within.contains(nextl)) nextl=null;
		}

		for (int i=l.sizeSticky()-1; i>=0; i--) {
			Mark m = l.getSticky(i);
			Object o = m.getOwner();
			if (!(o instanceof Span)) continue;

			Span span = (Span)o;
			Mark startm = span.getStart(), endm = span.getEnd();
			//System.out.print(startm+" .. "+endm+" => ");
			if (startm.leaf == endm.leaf) { span.moveq(null); /*lost two, so additional*/i--; }
			else if (m == endm) span.moveq(startm.leaf,startm.offset, prevl,(prevl!=null? prevl.size(): -1));
			else span.moveq(nextl,0, endm.leaf,endm.offset);
			//System.out.println(span.getStart()+".."+span.getEnd());
		}

		assert l.sizeSticky() == 0;
	}


	/** Repaints smallest subtree containing both endpoints. */
	public void repaint(long ms) {
		if (isSet()) start_.leaf.commonAncestor(end_.leaf).repaint(ms);
	}
	public void repaint() { repaint(0); }


	/**
	Removes span from document and its layer.
	Different from moveq(null), which removes from document tree but not from layer.
	If caller will handle all reformatting and repainting, as during a batch destory(), first moveq(null) on span.
	 */
	@Override
	public void destroy() {
		// mark dirty and call for repaint before remove!
		//System.out.println("destroy "+getName());
		if (isSet()) {
			IScrollPane isp = getDocument().getIScrollPane();
			if (isp.isValid()) getBrowser().repaint(100);	// if removing many, batch these (don't rely on Java)
			//Document doc = getDocument(); if (doc!=null && doc.) br.repaint(100);
			markDirty();
			moveq(null);	// clean up stickies
			//System.out.println("move null");
		} else Span.pending__.remove(this);	// hopefully not needed but be safe

		super.destroy();
		//System.out.println("removing "+start.leaf+".."+end.leaf);
	}

	/* Caller responsible for marking tree dirty and reformatting/repainting. */
	//public void removeq() {
	//if (!isSet()) return;
	/*
	if (notemark!=null) {
		notemark.remove();	// subsumes this span
	} else if (affectsLayout()) {	// repaint whole thing: who knows what all changed
		markDirty();
		getBrowser().repaint(100);
	} else repaint(50);*/

	//	  if (start.leaf.getParentNode()!=end.leaf.getParentNode()) {	update(start.leaf.getParentNode(), -1); update(end.leaf.getParentNode(), 1); }
	//	  if (start.leaf.getParentNode()!=end.leaf.getParentNode()) {	update(start.leaf.getParentNode(), start);	update(end.leaf.getParentNode(), end); }

	//	if (isSet() && start.leaf.getParentNode()!=end.leaf.getParentNode()) { update(start.leaf.getParentNode()); update(end.leaf.getParentNode()); }
	//	start.remove(); end.remove();

	//super.remove();
	//}


	private void removeSummary(Mark sum, INode n) {
		assert sum!=null && n!=null;

		for (; n!=null; n=n.getParentNode()) {	// could stop at lowest common ancestor(-1), but being safe doesn't cost more as should to end in break (below)
			int inx = n.indexSticky(sum);
			if (inx!=-1) n.removeSticky(inx);
			else break;	// no summaries from here on up, so done
		}
	}

	/**
	Stores summary information up to lowest node that dominates all spans of this type.
	Slightly more efficient if know lowest common ancestor of start and end nodes of span; else pass null.
	Don't call directly; automatically done through move()/moveq().
	Only need summaries if start and end nodes of span have different parents,
	so if have, say, hyperlink on words in, say, same paragraph, no extra memory use.
	 */
	private void addSummary(Mark sum, INode n, Node ancestor) {
		assert sum!=null && n!=null: sum+" "+n;

		//	if (DEBUG) assert n0.isStruct()=>compile-time check, "update counts on internal nodes only";
		for ( ; n!=null && n!=ancestor; n=n.getParentNode()) { // could stop at lowest common ancestor(-1), but being safe doesn't cost more as should end in break (below)
			int inx = n.indexSticky(sum);
			if (inx==-1) n.addSticky(sum, false);	// didn't find=>add.  maybe add in priority order, with later among equals coming last
			else n.removeSticky(inx);	// found, so assume that added too many summaries for opening span, so remove in present closing span
		}
	}

	public void markDirty() { if (isSet()) start_.leaf.markDirtyTo(end_.leaf); }


	/**
	Stuff instance state into attributes; if save buffer not null, write out corresponding XML.
	Subclass should override if have interesting content (can stuff content into attr then super.save()).
	If span is not attached to tree at save time, its old attachment points are retained.
	This way, spans that can't be attached presently can be tried again without degradation.
	 */
	@Override
	public ESISNode save() {
		//assert isSet();	// maybe -- if not set, then maybe couldn't attach, so keep old settings

		//System.out.println("*** saving span "+getClass().getName());
		ESISNode e = null;

		e = super.save();

		if (isSet()) {	// => if deleted removed from layer and not called.  if not set, don't report ro lo

			Document doc = getDocument();

			Leaf startn=getStart().leaf, endn=getEnd().leaf;
			int starti=getStart().offset, endi=getEnd().offset;
			if (pstart==null) pstart=new CHashMap<Object>(5); else pstart.clear();
			RobustLocation.descriptorFor(startn,starti, doc, pstart);
			if (pend==null) pend=new CHashMap<Object>(5); else pend.clear();
			RobustLocation.descriptorFor(endn,endi, doc, pend);
			/// CHANGED: I am using clipboard here and I use e.putattr, otherwise the attribute is not stored in the annotation!!!
			// record length for use when one endpoint restorable but not the other
			e.putAttr("length", Integer.toString(startn.lengthTo(starti, endn,endi)));
			String sel = getBrowser().clipboard(this);
			if (sel.length()>255)
				sel = sel.substring(0, 255);
			e.putAttr("content", sel);
		}
		// NB attribute always current

		e.appendChild(new ESISNode(Span.GI_START, pstart));
		e.appendChild(new ESISNode(Span.GI_END, pend));
		modified = false;
		return e;
	}


	/**
	Given ESIS subtree, pluck class-specific information from attributes, call super.restore() for locations.
	Attributes named <code>start</code> and <code>end</code> are reserved to hold Robust Location data.
	@see phelps.doc.RobustLocation
	 */
	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		super.restore(n,attr,layer);

		// restore locations, stuff attributes in Map for descendants
		//System.out.println("restore "+getName()+", ch cnt="+(n!=null?n.size(): 0));
		if (n!=null) for (int i=0,imax=n.size(); i<imax; i++)
			if (n.childAt(i) instanceof ESISNode) {
				ESISNode child = (ESISNode)n.childAt(i);
				Map<String,Object> attrs = child.attrs;
				String gi = child.getGI();
				if (Span.GI_START.equals(gi)) pstart = attrs;
				else if (Span.GI_END.equals(gi)) pend = attrs;
			}

		//if (pstart==null && pend==null) System.out.println("bad Location spec for "+this);
	}


	/**
	Attach to document tree based on saved anchor description.
	Not used/needed during creation of fresh spans as during interactive annotation.
	 */
	@Override
	public void buildAfter(Document doc) {
		Document croot = doc;

		//System.out.println("attaching to "+croot+"/"+rootname+"/"+docroot+", "+getAttr(ATTR_BEHAVIOR)+" = "+pstart+"  ..  "+pend);
		if (pstart==null || pend==null) return;	// &&, as if one null can estimate the other... but this is a sign of corrupt data record
		Mark s=RobustLocation.attach(pstart,croot), e=RobustLocation.attach(pend,croot);
		//System.out.println("attaching to "+doc+", "+pstart+"  ..  "+pend);
		//System.out.println("resolved to "+s+"  ..  "+e);

		// if one endpoint failed but not the other, compute failed one as good+length
		//System.out.println("span "+getAttr(ATTR_BEHAVIOR)+" moved to "+s+".."+e);
		if ((s==null || e==null) && (s!=null || e!=null)) {	// XOR boolean op in Java?
			int length = Integers.parseInt(getAttr("length"),5);	// defensive on LENGTH since added later in development, but good anyhow as may have been deleted or, in the future, not generated by other software
			if (e==null) (e=new Mark(s)).move(length); else /*s==null*/ (s=new Mark(e)).move(-length);
			// if (getGlobal("STATS")!=null) show endpoints in last-chance color
		}

		if (s!=null && e!=null)
			move(s,e);
		//makeNotemark(getAttr("nb"));
		else
			System.out.println("couldn't attach "+getClass().getName()+"\n\t"+pstart+"\n\t"+pend+", layer="+getLayer().getName());
		//new Exception().printStackTrace(); //System.exit(1);
		//br.eventq(MSG_UNATTACHED, this);

		// once attached, still keep pstart&pend?
		super.buildAfter(doc);
	}


	/** Morphing and deletion menu items in popup. */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se,msg)) return true;
		else if (this!=se.getIn()) {}
		//	if (id==EDITSPAN) { sel.move(this); remove(); }
		//System.out.println("in "+hashCode());
		else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && isEditable()) {// && this!=getBrowser().getSelectionSpan()) {	// don't offer to morph sel span to sel span
			INode menu = (INode)se.getOut();	//attrs.get("MENU");
			Browser br = getBrowser();
			//createUI("button", getClass().getName()+" in "+getLayer().getName(), "", menu, "EDIT", true);
			createUI("button", "Morph Span to Selection", new SemanticEvent(br, Span.MSG_MORPH, null, this, null), menu, "EDIT", !br.getSelectionSpan().isSet());
			createUI("button", "Delete Span"/*+" "+getClass().getName()*/, new SemanticEvent(br, Span.MSG_DELETE, null, this, null), menu, null/*end of menu, after protective separator*/, false);
		}
		return false;
	}

	/** Recognize "deleteSpan <span>" and "morphSpan <span>". */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Browser br = getBrowser();
		Span sel = br.getSelectionSpan();

		if (this!=se.getIn()) {}

		else if (Span.MSG_DELETE==msg) {
			sel.moveq(this);	// in case want to create new span at same place
			destroy();

		} else if (Span.MSG_MORPH==msg) {
			if (sel.isSet()) { move(sel); sel.move(null); }

		} else if (Span.MSG_EDIT==msg) {
			// ask user
			String message=null, key=null;
			if (getClass() == HyperlinkSpan.class) {
				message =  "Enter the URI of destination:";
				key = HyperlinkSpan.ATTR_URI;
			} else if (getClass() == AnchorSpan.class) {
				message =  "Enter name of anchor";
				key = "id";
			} else if (getClass() == AwkSpan.class) {
				message =  "Enter the comment";
				key = "comment";
			} else if (getClass() == InsertSpan.class || getClass() == ReplaceWithSpan.class) {
				message =  "Enter suggested text to insert";
				key = "insert";
			} else if (getClass() == TextSpanNote.class) {
				message =  "Enter the comment";
				key = TextSpanNote.ATTR_TEXT;
			} else if (getClass() == CitesSpanNote.class) {
				message =  "Enter the URI for the citation:";
				key = CitesSpanNote.CITED_URI;
			}

			if (msg!=null && key!=null) {
				String ret  = JOptionPane.showInputDialog(br,message,  se.getArg());
				Map<String,String> map = new Hashtable<String, String>();
				if (ret == null)
					map.put("cancel", "true");
				//map.put(HyperlinkSpan.ATTR_URI, ret)
				//				new SemanticEvent(this,)
				//				br.eventq(new SemanticEvent(this,SystemEvents.MSG_FORM_DATA, map,this,this));
				else {
					map.put(key, ret);
					modified = true;
				}
				br.eventq(new SemanticEvent(br,SystemEvents.MSG_FORM_DATA, map,this,null));

				br.repaint(100);

			}
			//br.eventq(SystemEvents.MSG_FORM_DATA, map);

		}
		//		String cname = getClass().getName(); if (cname.lastIndexOf('.')!=-1) cname=cname.substring(cname.lastIndexOf('.')+1);
		//		URL url = getClass().getResource(cname+"-edit.html");
		//		if (url!=null) {
		////if (DEBUG) System.out.println("url = "+url+", proto="+url.getProtocol()+", host="+url.getHost()+", path="+url.getPath());
		//			URI uri=null; try { uri = URLs.toURI(url); } catch (URISyntaxException canthappen) {}
		////if (DEBUG) System.out.println("=> uri = "+uri);
		//
		//			/*VDialog dialog =*/ new VDialog("editspan",null, getRoot()/*getDocument()--drowns inside Note*/, uri, getAttributes(), this);
		//		}
		//		//dialog.setTitle(""); -- taken from .html's <title>
		//		// set fields: url, ... -- set from passed getAttributes()
		//
		//		// set cursor to first field
		//	}

		return super.semanticEventAfter(se,msg);
	}



	/**
	Receives synthesized {@link java.awt.event.MouseEvent#MOUSE_ENTERED}, {@link java.awt.event.MouseEvent#MOUSE_EXITED}, {@link java.awt.event.MouseEvent#MOUSE_PRESSED}, ....
	For the purposes of event passing, functions more as tree node than behavior (with before/after).
	 */
	public void event(AWTEvent e) {}



	@Override
	public boolean checkRep() {
		assert super.checkRep();

		if (!isSet()) return true;	// checked by Leaf, because outside of that leaf context ok not to be set

		// start.cmp(end) <= 0
		assert start_.getOwner()==this && end_.getOwner()==this;
		assert start_.leaf != null && end_.leaf != null;
		assert start_.offset >= 0 && start_.offset <= start_.leaf.size();
		assert end_.offset >= 0 && end_.offset <= end_.leaf.size();

		// nodes at endpoints point back to span
		//valid = ASSERT(sn.sticky_!=null && s.leaf.sticky_.indexOf(s)!=-1, errors, "Span start "+s+" not pointed to by node.") && valid;
		//valid = ASSERT(en.sticky_!=null && e.leaf.sticky_.indexOf(e)!=-1, errors, "Span end "+e+" not pointed to by node.") && valid;

		// summaries correct (both terminate at same node)
		//Node stop=sn.getParentNode(), etop=en.getParentNode();
		//while (stop!=null && stop.sticky_!=null && stop.sticky_.indexOf(this)!=-1) stop=stop.getParentNode();
		//while (etop!=null && etop.sticky_!=null && etop.sticky_.indexOf(this)!=-1) etop=etop.getParentNode();
		//valid = ASSERT(stop==etop, errors, this+": endpoint summaries terminate at different nodes: "+stop+" vs "+etop+" => "+sn.commonAncestor(en)) && valid;
		//System.out.println("Span "+getName());

		// endpoints in same Document as Behavior (this class)
		Document doc = getDocument();
		assert start_.leaf.getDocument() == end_.leaf.getDocument();	// endpoints in different documents
		assert doc==null || doc == start_.leaf.getDocument() || this==getBrowser().getSelectionSpan();	// not in same doc as behavior

		return true;
	}



	//public boolean equals(Object o) { return o == this; }

	/**
	Subclasses should extend to check any attributes they add.
	@param endpoints   if false, don't consider endpoints.
  public boolean equals(Object o, boolean endpoints) {
	return this == o; ?

	if (this == o) return true;
	if (o==null || !(o instanceof Span)) return false;

	Span span2 = (Span)o;
	boolean eq = true;
	if (eq && endpoints) {
		eq = (!isSet() && !span2.isSet()) || (getStart().equals(span2.getStart()) && getEnd().equals(span2.getEnd()));
		// check pstart and pend too?
	}

	if (eq) eq = getName().equals(span2.getName());

	// attributes ...


	return eq;
  }
	 */

	@Override
	public String toString() {
		return getName()+"{"+start_+".."+end_+"}";
		//return getClass().getName();
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}
}
