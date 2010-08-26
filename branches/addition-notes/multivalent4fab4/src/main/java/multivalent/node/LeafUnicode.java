package multivalent.node;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Map;

import phelps.awt.Colors;

import com.pt.awt.NFont;

import multivalent.INode;
import multivalent.Context;
import multivalent.MediaAdaptor;



/**
	Leaf subclass for Unicode.

	<p>Each word gets its own leaf, more or less.  I used to think this was wasteful, as opposed
	to having a leaf per screen line, say, but if a word costs 50 bytes instead of 5,
	that just means that a 10,000 word page (which is big) costs 500K instead of 50K,
	and 100,000 words cost 5MB instead of 500K.  No sweat for today's memory sizes.

	@see multivalent.std.adaptor.ASCII

	@version $Revision: 1.4 $ $Date: 2005/12/31 09:59:42 $
*/
public class LeafUnicode extends LeafText {

  public LeafUnicode(String name, Map<String,Object> attr, INode parent) { this(name,null, attr, parent); }
  public LeafUnicode(String name, String estr,  Map<String,Object> attr, INode parent) {
	super(name,estr,  attr, parent);
	assert name!=null: "No null words -- but zero-length OK";
  }

  /** Similar to paintNodeContent, set right font, measure. */
  public boolean formatNodeContent(Context cx, int start, int end) {
	int len = name_.length();
	//if (len==0) { bbox.setSize(1,1); baseline=1; return false; }

	if (end>=len) end=len-1;
	assert start>=0: "start<0 in "+getName();
	//if (start<0) { start=0; System.out.println("start<0: "+start+"!"); }
	NFont spot = cx.spot; boolean fenc = spot!=null && estr_!=null;
	String txt = fenc? estr_: name_;
	txt = start!=0 || end<len-1? txt.substring(start,end+1): txt;
	if (!fenc && txt!=null && !txt.equals("\t")) {
		String trans = cx.texttransform;    // this one interned
		// wrong place for this!
		if (null==trans || "none"==trans) {}
		else if ("uppercase"==trans || ("capitalize"==trans && start==0 && txt.length()<=1)) txt=txt.toUpperCase();
		else if ("lowercase"==trans) txt=txt.toLowerCase();
		else if ("capitalize"==trans) txt = Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
		//else if ("inherit"/STRINGINHERIT==trans) ...
	}

	NFont f = cx.getFont();
	//bbox.height = (int)f.getHeight(); => outliners (divisor bar, doubly-accented chars, accented lowercase l...)
	//baseline = (int)f.getAscent();
	baseline = (int)Math.ceil(f.getAscent());
	bbox.height = (int)Math.ceil(f.getSize() + 2);	// e.g., 10-on-11 (looks better than 10-on-12 for screen)

	if (fenc) {
		bbox.width = (int)(spot.estringAdvance(txt, 0,txt.length()).getX() + 0.5);
	} else if (len==0) {
		bbox.width = (int)/*0;*/f.charAdvance(' ').getX();
//System.out.println("len = "+bbox.width+", height="+bbox.height);
	//} else if (txt.equals("\t")) { // LATER more sophisticated handling of tabs
//System.out.println("tab @ "+cx.x);
		//bbox.width = 4*f.charAdvance(' ').getX();
//		  bbox.height = baseline = 0;
	} else {
		bbox.width = spot!=null? (int)(spot.stringAdvance(txt).getX() + 0.5): (int)f.stringAdvance(txt).getX();
		//if (end==len-1) bbox.width += (bbox.height>20?7:3); // f.charAdvance(' '); -- too much
	}
	return false;
  }


  /** To paint content, set right font, use Graphics.drawString(); */
  public boolean paintNodeContent(Context cx, int start, int end) {
	assert 0<=start /*&& start<=end -- restore this*/ && end<=size();   // start<end?
//if (childNum()==0) System.out.println("painting "+getName()+"  "+start+".."+end);
	int len=size(); //name_.length();  // use size(), overriding size() if necessary (which I doubt)
	if (start>/*=*/end/*put in invariant*/ || start>=len || len==0) return false;   // Mark after last character -- Graphics2D.drawString (actually TextLayout constructor) doesn't like 0-length String

	NFont spot = cx.spot; boolean fenc = spot!=null && estr_!=null;
	AffineTransform at = spot!=null && spot.isTransformed()? spot.getTransform(): null;
	float w = bbox.width, h = 0f;
	String txt = fenc? estr_: name_;
	if (end>=len) { end=len-1; if (name_.charAt(end)=='\n') end--; }
	NFont f = /*spot!=null?*/ cx.getFont()/*: null*/;
	if (start!=0 || end<len-1) {
		txt = end-start==0? phelps.lang.Strings.valueOf(txt.charAt(start)): txt.substring(start,end+1);
		//txt = name_.substring(start,end+1);  // would be nice to have Graphics.drawChars(String str, int offset, int length, int x, int y)
		if (fenc) { Point2D pt = spot.estringAdvance(txt, 0,txt.length()); w=(float)pt.getX(); h=(float)pt.getY(); }
		else if (spot!=null) { Point2D pt = spot.stringAdvance(txt, 0,txt.length()); w=(float)pt.getX(); h=(float)pt.getY(); }
		else w = (float)f.stringAdvance(txt).getX();   // could create a million String's, but usually don't start or end span in middle of node, so don't substring after all
//System.out.println("painting subportion "+start+".."+end+" (coords "+cx.x+".."+w+")");
	}
	Graphics2D g = cx.g;
	if (cx.background!=Colors.TRANSPARENT && cx.background!=null && cx.background!=cx.pagebackground/*was: ...!=null*/) {   // should be in Leaf, but have to know width before can draw background
		g.setColor(cx.background);	// may be highlighted... => Spans should do this (usually, with help for weirdness like image-OCR)
		g.fillRect((int)cx.x,(int)(baseline-f.getAscent()-cx.ydelta), /*Math.ceil*/(int)w+1/*rounding*/,bbox.height/*(int)f.getHeight() + /*rounding*/);   // can't do by system as only leaf knows width of text
	}
//if (!Color.BLACK.equals(cx.foreground)) System.out.println("fg = "+cx.foreground+" on "+getName());
	if (cx.foreground!=Colors.TRANSPARENT && txt!=null /*&& !txt.equals("\t")*/) {   // => DO THIS ON FORMAT ONLY; if transformation, store display name in name_, original as attribute
//if (txt.length()==0) { System.out.println(name_+" "+start+".."+end); return false; }
		String trans = cx.texttransform;    // this one interned
		if (fenc || null==trans || "none"==trans) {}
		else if ("uppercase"==trans || ("capitalize"==trans && start==0 && txt.length()<=1)) txt=txt.toUpperCase();
		else if ("lowercase"==trans) txt=txt.toLowerCase();
		else if ("capitalize"==trans && start==0) txt = Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
		//else if ("inherit"/STRINGINHERIT==trans) ...

//System.out.println(txt+", spot = "+spot);
		g.setColor(cx.foreground);
		int mode = cx.mode;
		if (NFont.MODE_STROKE==mode || NFont.MODE_FILL_STROKE==mode || NFont.MODE_STROKE_ADD==mode || NFont.MODE_FILL_STROKE_ADD==mode) g.setStroke(cx.getStroke());
//System.out.println("paint "+txt+" @ "+cx.x);
//System.out.println(spot+" "+estr_+" "+txt);
		if (fenc) spot.drawEstring(g, txt, cx.x, cx.y + baseline - cx.ydelta, NFont.LAYOUT_NONE, mode, cx.strokeColor);
		else if (spot!=null) spot.drawString(g, txt, cx.x, cx.y + baseline - cx.ydelta, NFont.LAYOUT_NONE, mode, cx.strokeColor);
		else { f.drawString(g, txt, cx.x, /*bbox.y +*/cx.y + baseline - cx.ydelta/*relative to baseline*/, NFont.LAYOUT_MINIMUM, mode, cx.strokeColor); }
	}
	cx.x += w; cx.y -= h;	// update current point

	return false;
  }
}
