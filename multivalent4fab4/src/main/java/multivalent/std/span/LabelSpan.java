package multivalent.std.span;

import java.awt.*;
import java.util.*;

import com.pt.awt.NFont;

import multivalent.*;



/**
	Helper span for copy editor marks that draws message above content text.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class LabelSpan extends Span {
  private static NFont annoFont = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 10f);

  String label_ = "no label";

  public void setLabel(String label) { label_=label; /*should trigger a reformat*/ }

  public boolean paintBefore(Context cx, Node start) {
	Rectangle bbox = start.bbox;
	Graphics2D g = cx.g;
	g.setColor(getLayer().getAnnoColor());
	NFont f = annoFont;	// layer_.getAnnoFont()
	annoFont.drawString(g, label_, cx.x,(float)/*bbox.y+*/annoFont.getAscent());
//	g.drawString(label_, cx.x,bbox.y+start.baseline+10/*cx.getFont().getAscent()8?/*2 points of descender*/);
	return false;
  }

  public int getPriority() { return ContextListener.PRIORITY_SPAN-ContextListener.LOT; }

  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	if (rn!=null && ro>rn.size()) {
		Leaf next = rn.getNextLeaf();
		if (next!=null) { rn=next; ro=0; } // else remove()?
	}
	super.moveq(ln,lo, rn,ro);
  }

  public boolean appearance(Context cx, boolean all) {
	if (label_!=null) cx.spaceabove = Math.max(cx.spaceabove,10);
//	  if (label_!=null) cx.spacebelow = Math.max(cx.spacebelow,10);
	return false;
  }

  /** Intert to events (so don't get double entries in popups). */
  public boolean semanticEventBefore(SemanticEvent se, String msg) { return false; }
  /** Intert to events. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) { return false; }

  /** Can't save -- Xjust helper span. -- just put in scratch layer
  public ESISNode save() {}*/
  /** Can't restore -- just helper span.
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {} */
}
