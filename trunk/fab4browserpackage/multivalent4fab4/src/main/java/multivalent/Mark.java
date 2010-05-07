package multivalent;

import java.awt.Point;



/**
	(Leaf, offset) pair.
	Not added to a Leaf's sticky list until {@link #move(Leaf, int)}.

	@see multivalent.Span

	@version $Revision: 1.9 $ $Date: 2003/01/30 01:03:37 $
 */
public class Mark {
	static final boolean DEBUG=false;

	/** Owner can be Span, Behavior, a String to name the mark, or null. */
	private Object owner;

	/** Leaf attached to. */
	public Leaf leaf;	// must be private so correctly remove from old leaf

	/** Offset within leaf. */
	public int offset;	// maybe make this Object cookie -- but then wouldn't know how to update


	public Mark() { this(null,-1); }
	public Mark(Leaf n, int offset) { this(n, offset, null); }
	public Mark(Mark copyme) { this(copyme.leaf, copyme.offset, copyme.getOwner()); }	// not just clone() as need to attach
	public Mark(Leaf leaf, int offset, Object owner) {
		//assert... nulls ok for any
		this.leaf = leaf;
		this.offset = offset;
		this.owner = owner;
	}

	public final Object getOwner() { return owner; }

	public boolean isSet() {
		return leaf!=null && leaf.indexSticky(this)!=-1;
	}



	public void move(Mark m) { if (m==null) move(null,-1); else move(m.leaf, m.offset); }
	public void move(Leaf newleaf, int newoffset) {
		//assert leaf!=newleaf || offset!=newoffset;	//, "Mark move: shouldn't be called with nothing to do -- use remove()";
		remove();	// from old

		leaf=newleaf; offset=newoffset;	// to new
		if (leaf!=null) leaf.addSticky(this);
	}

	// X may want option to stop at structural boundaries (parent changed) => use move(delta, bounds)
	public void move(int delta) { move(delta, null); }

	/** Move by delta units, traversing leaf-to-leaf, bounded inside passed subtree. */
	public void move(int delta, INode bounds) {
		if (!isSet()) return;

		Leaf knowngood = leaf;
		remove();
		//System.out.print("   delta="+delta);
		if (delta>0) {
			for (leaf=knowngood; leaf!=null && delta+offset>leaf.size(); delta -= leaf.size()-offset+1/*space counts as 1*/, knowngood=leaf, leaf=leaf.getNextLeaf(), offset=0) {}	// handle overflow
			if (leaf!=null) offset = Math.min(leaf.size(),offset+delta); else { leaf=knowngood; offset=knowngood.size(); }	// within leaf

		} else if (delta<0) {
			delta = -delta;
			for (leaf=knowngood; leaf!=null && delta>offset; delta -= offset+1, knowngood=leaf, leaf=leaf.getPrevLeaf(), offset=leaf.size()) {}	// handle overflow
			if (leaf!=null) offset = Math.max(0,offset-delta); else { leaf=knowngood; offset=0; }	// within leaf
		}
		//System.out.println(", moved to "+leaf+"/"+offset);

		if (bounds!=null && !bounds.contains(leaf))
			if (delta<0) { leaf=bounds.getFirstLeaf(); offset=0; }
			else if (delta>0) { leaf=bounds.getLastLeaf(); offset=leaf.size(); }

		if (leaf!=null) leaf.addSticky(this);
	}

	public void remove() {
		if (leaf!=null) { leaf.removeSticky(this); leaf=null; }
	}


	/** Scroll containing IScrollPane as necessary to show cursor on screen. */
	public void scrollTo() {
		if (isSet()) {
			// to leaf.scrollTo() add point within leaf
			Point rel = leaf.offset2rel(offset);
			leaf.scrollTo(rel.x, rel.y, true);
		}
	}


	@Override
	public boolean equals(Object o) {
		if (this==o) return true; if (!(o instanceof Mark)) return false;
		Mark m = (Mark)o;
		return m.leaf==leaf && m.offset==offset && m.owner==owner;
	}

	@Override
	public int hashCode() { return System.identityHashCode(leaf) + offset; }

	@Override
	public String toString() {
		return leaf+"/"+offset;
		/*	if (leaf==null && owner_==null) return "(null)/"+offset;
	else if (owner_==null) return leaf.getName()+'/'+offset;
	else if (leaf==null) return "/"+offset+":"+(owner_.getClass().getName());
	else return leaf.getName()+"/"+offset+":"+(owner_.getClass().getName());*/
	}
}
