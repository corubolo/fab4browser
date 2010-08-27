package multivalent.node;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import com.pt.awt.NFont;

import multivalent.Leaf;
import multivalent.INode;
import multivalent.Context;
import multivalent.Mark;
import multivalent.Span;
import multivalent.StyleSheet;
import multivalent.ContextListener;



/**
	Superclass for text leaf nodes.

	@version $Revision: 1.7 $ $Date: 2002/02/02 13:41:40 $
*/
/*abstract--HTML*/ public class LeafText extends Leaf {
  /** Array of letter x-positions. */
  protected static double[] Widths_ = new double[200];

  /** Name is Unicode, these are characters encoded according to font that will draw them. */
  protected String estr_ = null;


  public LeafText(String name, Map<String,Object> attr, INode parent) { this(name,null, attr, parent); }
  public LeafText(String name, String estr,  Map<String,Object> attr, INode parent) {
	super(name,attr, parent);
	estr_ = estr;
  }

  /** Returns Unicode text. */
  public String getText() { return super.getName(); }

  /* * Returns text in encoding needed for font. 
  public String getEstr() {
	return estr_;
  }*/

  public int size() { return estr_!=null? estr_.length(): name_!=null? name_.length(): 0; }

  public void append(Leaf l) {
	super.append(l);
	if (l instanceof LeafText) append(l.getName(), ((LeafText)l).estr_);
  }

  public void append(String text) { append(text, null); }
  public void append(String text, String estr) {
	//assert text!=null: name_;
	name_ = name_==null? text: text==null? name_: name_ + text;
	estr_ = estr_==null? estr: estr==null? estr_: estr_ + estr;
	setValid(false);
  }


  public void clipboardNode(StringBuffer sb) { sb.append(name_).append(' '); }
  public void clipboardBeforeAfter(StringBuffer sb, int start, int end) {
	sb.append(name_.substring(start,end));
  }


  /**
	Calculates an array of x-position at each letter in word.
	@param cx is up-to-date for this Node
  */
  public void subelementCalc(Context cx) {
//System.out.println("*** computing letter widths");	// monitor that cacheing is working (should compute only once, on entering leaf)
	StyleSheet ss = cx.styleSheet; List<ContextListener> actives = cx.vactive_;/*don't have to getActivesAt*/ INode p = getParentNode();
	int len = size();
	if (Widths_.length < len) Widths_=new double[len];

	// chunk by transitions rather than letters
	double w=0.0; int wi=0;
	NFont f = cx.getFont();
	Span lastspan = null;
	for (int i=0,imax=stickycnt_; i<imax; i++) {
		Mark m=sticky_[i]; int offset=m.offset;
		//assert m.leaf == this;
		// invariant: NFont valid

		// characters up to transition
		if (cx.elide) for ( ; wi<offset; wi++) Widths_[wi] = w;     // no increase (not 0)
		else for ( ; wi<offset; wi++) {
			char ch = name_.charAt(wi);
			w += f.charAdvance(ch).getX();
			Widths_[wi] = w;
		}

		// transition (if multiple at same point, just pick up on next loop iteration)
		if (m.getOwner() instanceof Span) {
			Span r = (Span)m.getOwner();
			if (m == r.getStart()) {
				int lastsize=actives.size();    // save a stylesheet lookup if didn't have anything -- not worth it as lookup is cheap?
				/*if (spanname!=null)--should always be*/ss.activesAdd(actives, r, p);
				lastspan = (actives.size()==lastsize? r: null);     // set to null so no intervening
				cx.addq(r);
			} else {
				cx.removeq(r);
				if (r!=lastspan) ss.activesRemove(actives, r, p);
			}
			cx.reset();
			f = cx.getFont();
		}
	}

	// characters after last transition
//if (len!=name_.length()) System.out.println("|"+name_+"| != "+len);
	if (!cx.elide) for ( ; wi<len; wi++) {
		char ch = name_.charAt(wi);
		w += f.charAdvance(ch).getX();
		Widths_[wi] = w;
	}
/*System.out.print("calc LeafText "+getName()+"   ");
for (int i=0,imax=size(); i<imax; i++) System.out.print(" "+Widths_[i]);
System.out.println();*/
  }


  public int subelementHit(Point rel) {
	super.subelementHit(rel);

	int x=rel.x+3, y=rel.y+3;	// fuzz
	// would have to write own binary search since java.util.binarySearch takes full array and it's only partially (very lightly!) populated
	//int inx = Arrays.binarySearch(Widths, x);
	//return (inx>=0? inx: -inx-1-1);
	for (int i=0,imax=size(); i<imax; i++) if (Widths_[i]>=x) return i;
	return size();
  }

  public Point offset2rel(int offset) {
	Point pt = super.offset2rel(offset);
	if (offset <= 0) pt.x = 0;
	else if (offset >= size()) pt.x = bbox.width;
	else pt.x = (int)Widths_[offset-1];

	return pt;
  }
}
