package multivalent.std;

import java.util.Comparator;
import java.util.Arrays;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;

import phelps.lang.Strings;



/**
	Sort structural table at given column, heuristically determining data type.
	Assume HTML/CALS-style table (row major; TABLE tag with THEAD, TBODYs, and TFOOT as children, and TRs as their children).
	Relies on HTML parser normalizing table by filling in cells not explicit in rowspan or colspan,
	though painting and formatting will still be weird if these are around.
	Unlike most behaviors, which work on all document types, TableSort works only on flowed documents.

	<p>To do: generalize to range of lines, as tables aren't used for tabular information much, as least in HTML.

	@version $Revision: 1.3 $ $Date: 2003/06/02 06:00:49 $
*/
public class TableSort extends Behavior {
  /**
	Sort table in ascending order, or reverse order if already sorted in ascending order.
	<p><tt>"tableSort"</tt>: <tt>arg=</tt> {@link Node} <var>column-to-sort</var>
  */
  public static final String MSG_SORT = "tableSort";

  /**
	Sort table in ascending order.
	<p><tt>"tableSortAscending"</tt>: <tt>arg=</tt> {@link Node} <var>column-to-sort</var>
  */
  public static final String MSG_ASCENDING = "tableSortAscending";

  /**
	Sort table in descending order.
	<p><tt>"tableSortDescending"</tt>: <tt>arg=</tt> {@link Node} <var>column-to-sort</var>
  */
  public static final String MSG_DESCENDING = "tableSortDescending";


  /** Cache data, and keep row synchronized with its column data during sorting. */
  static class Pair {
	public INode row;	// used when reattach after sorting
	public String s; public long l; public double d;
	Pair(INode row, String val) { this.row=row; s=val; }
	public boolean equals(Object o2) { return this==o2; }   // uses Comparator instead -- good for Arrays.equals()
	public int hashCode() { return System.identityHashCode(this); }
  }

  static class PairCompare implements Comparator<Pair> {
	static final int TYPE_STRING=0, TYPE_LONG=1, TYPE_DOUBLE=2;
	int sortType = TYPE_STRING;
	int direction_;

	PairCompare(int type, int direction) {
		assert type>=TYPE_STRING && type<=TYPE_DOUBLE;
		sortType = type;
		direction_ = direction;
	}

	public int compare(Pair p1, Pair p2) {
		int cmp;
		if (sortType==TYPE_LONG) { long diff=p1.l-p2.l; cmp = (diff<0? -1: diff>0? 1: 0); }
		else if (sortType==TYPE_DOUBLE) cmp = Double.compare(p1.d, p2.d);
		//else cmp = p1.s.compareToIgnoreCase(p2.s);
		else cmp = Strings.compareDictionary(p1.s, p2.s, true);

		return (direction_ >= 0? cmp: -cmp);
	}
  }


  /** Since not all man page references are recognizable as such, have docpopup choice to treat current word as man page ref. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	// offer to sort any current tables, but that can lead to trouble -- which can be fixed with a reload
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()!=getBrowser().getSelectionSpan()) {	//null)) { -- just plain-doc popup
		Browser br = getBrowser();
		Node curn = br.getCurNode();
//System.out.println("**** table sort curn = "+curn);
		if (curn!=null) {
			Document doc = getDocument();
			INode col = curn.getParentNode(), row=col;
			for (INode p=col; p!=null && p!=doc; col=row, row=p, p=p.getParentNode()) {
//System.out.println("	 "+p.getName());
				//if ("table".equals(p.getName())) {
				if ("tbody".equals(p.getName())) {
					int colnum = col.childNum();
					//colnum_ = col.childNum(); => sortTable should figure it out for itself, but check possibility here
					INode menu = (INode)se.getOut();
					createUI("button", "Sort table on column #"+(colnum+1), new SemanticEvent(br, MSG_SORT, col), menu, "EDIT", false);
					createUI("button", "Sort table, descending", new SemanticEvent(br, MSG_DESCENDING, col), menu, "EDIT", false);
					//break; -- keep going for nested tables
				}
			}
		}
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SORT==msg) sortTable(se.getArg(), 0);
	else if (MSG_ASCENDING==msg) sortTable(se.getArg(), 1);
	else if (MSG_DESCENDING==msg) sortTable(se.getArg(), -1);

	return super.semanticEventAfter(se,msg);
  }


  /**
	@param colin  some node within column to sort
	@param direction: -1=descending order, 1=ascending order, 0=ascending but descending if already in ascending.
  */
  public void sortTable(Object colin, int direction) {

	// 1. validate and setup
	Node col=null;
	if (colin==null) {}
	else if (colin instanceof Node) col=(Node)colin;
	else if (colin instanceof Mark) col=((Mark)colin).leaf;
	if (col==null) return;


	INode table=null;
	Node row=col;
//System.out.println("col = "+col.getFirstLeaf().getName());

	// find top of table and chosen column
	Document doc = getDocument();
	for (INode p=col.getParentNode(); p!=doc; col=row, row=p, p=p.getParentNode()) {
		//if ("table".equals(p.getName())) {
		if ("tbody".equals(p.getName())) {
			table=p;
			break;
		}
	}
	if (table==null || row==null || !"tr".equals(row.getName())) return;


	int colnum = col.childNum();
//System.out.println("sorting "+table+", col="+colnum+", direction="+direction+"/dir_="+direction_);
	INode headerrow = ("th".equals(col.getName())? col.getParentNode(): null);	// iff first table cell is TH, assume whole row is header (error to be elsewise?)


	// 2. collect data in column and determine type
	int headcnt=(headerrow==null?0:1), rowcnt=table.size();
	Pair[] data = new Pair[rowcnt-headcnt];
	if (data.length<=1) return;	// nothing to sort

	boolean maybeLong=true, maybeDouble=true;
	for (int ri=0,r=0+headcnt,rmax=rowcnt; r<rmax; ri++,r++) {
		// be wary of nulls, as HTML in the wild is full of crap
		INode rown = (INode)table.childAt(r);
		INode coln = (INode)(rown.size()>=colnum? rown.childAt(colnum): null);
		Leaf l = (coln!=null? coln.getFirstLeaf(): null);
		String raw = (l!=null? l.getName(): null);
//System.out.println("raw["+ri+"] = "+raw);
		//if (txt==null && l.getNextLeaf().getParentNode()==l.getParentNode()) txt=l.getNextLeaf().getName();	// null pointers lurk
		Pair d = data[ri] = new Pair(rown, raw);

		// type the data (integer, floating point, or just strings)
		if (maybeDouble) {
			if (raw==null) d.d=0.0;
			else try { d.d = Double.parseDouble(raw); } catch (NumberFormatException dnfe) { maybeDouble = maybeLong = false; }
		}
		if (maybeLong) {
			if (raw==null) d.l=0;
			else try { d.l = Long.parseLong(raw); } catch (NumberFormatException lnfe) { maybeLong = false; }
		}
		if (raw==null) d.s="";
	}
//System.out.println("raw[0] = "+data[0].s);
	int type = (maybeLong? PairCompare.TYPE_LONG: maybeDouble? PairCompare.TYPE_DOUBLE: PairCompare.TYPE_STRING);


	// 3. sort
	Pair[] orig = (Pair[])data.clone();
	Arrays.sort(data, new PairCompare(type, direction)/*this*/);
	// if end up in same order, reverse the order
	// unfortunately orig.equals(data) doesn't do element-wise comparison, and Arrays.equals() uses .equals() not ==
	boolean same = Arrays.equals(orig, data);
	if (direction==0 && same) {
		for (int ri=0,rimid=data.length/2,re=data.length-1; ri<rimid; ri++,re--) {
			Pair tmp=data[ri]; data[ri]=data[re]; data[re]=tmp;
		}
		same = false;
	}


	// 4. hack tree
	if (!same) {
		table.removeAllChildren();
		if (headerrow!=null) table.appendChild(headerrow);
		for (int r=0,rmax=data.length; r<rmax; r++) table.appendChild(data[r].row);
		table.markDirtySubtree(false);	// leaves ok
		table.repaint();
	}
  }
}
