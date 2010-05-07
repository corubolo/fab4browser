package multivalent.node;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import multivalent.*;



/**
	{@link java.lang.CharSequence} over the document tree, as for used by {@link java.util.regex.Pattern regular rexpressions}.
	The sequence consists of the text of each leaf ({@link Node#getName()}, separated by spaces.
	The same instance can be used for many searches or other operations.
	At its creation, this class takes a snapshot of the tree and ignores subsequent modification to it.

	@version $Revision$ $Date$
*/
public class NodeCharSequence implements CharSequence {
  String txt_;
  Leaf[] leaves_;
  int start_, end_;


  public NodeCharSequence(Node root) {
	//assert root!=null; -- not legal here
	this(root.getFirstLeaf(), root.getLastLeaf());
  }

  public NodeCharSequence(Leaf startl, Leaf endl) {
	assert startl!=null /*&& endl!=null*/;

	if (endl!=null && Node.cmp(startl,0, endl,endl.size(), null) <= 0) { Leaf tmpl=startl; startl=endl; endl=tmpl; }

	StringBuffer sb = new StringBuffer(2000 * 10);
	List<Leaf> ll = new ArrayList<Leaf>(2000);
	INode lastp = null;
	for (Leaf l=startl, e = (endl!=null? endl.getNextLeaf(): endl); l!=e; l=l.getNextLeaf()) {
		ll.add(l);

		INode p = l.getParentNode();
		if (p==lastp) sb.append(' '); else if (lastp!=null) sb.append('\n');
		sb.append(l.getName());
		lastp = p;
	}
	txt_ = sb.substring(0);

	Leaf[] leaves = new Leaf[sb.length()];
	for (int i=0,imax=ll.size(), j=0; i<imax; i++) {
		Leaf l = (Leaf)ll.get(i);
		for (int jmax = j + l.getName().length() + 1/*space*/; j<jmax; j++) leaves[j] = l;  // could leave slots as null and search, but no more memory and only a little time to fill
	}
	leaves_ = leaves;

	start_ = 0; end_ = sb.length();
  }

  /** Constructor for subSequence. */
  private NodeCharSequence(String txt, Leaf[] leaves, int start, int end) {
	txt_ = txt;
	leaves_ = leaves;
	start_ = start; end_ = end;
  }


  public char charAt(int index) { return txt_.charAt(index + start_); }
  public int length() { return end_ - start_; }
  public CharSequence subSequence(int start, int end) { return new NodeCharSequence(txt_, leaves_, start_, end_); }

  public Leaf nodeAt(int index) { return leaves_[index + start_]; }
  public Mark markAt(int index) {
	Leaf l = leaves_[index + start_];
	int off = 0;
	for (int i=index+start_-1; i>=start_; i--) if (leaves_[i] == l) off++; else break;
	return new Mark(l, off);
  }
  public int textIndexOf(Leaf l) {
	return Arrays.asList(leaves_).indexOf(l);
  }

  public String toString() { return txt_.substring(start_, end_+1/*String.substring exclusive*/); }
}
