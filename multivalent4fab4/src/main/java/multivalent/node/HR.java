package multivalent.node;

import java.awt.Graphics2D;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import phelps.lang.Integers;

public class HR extends Leaf {
  int barwidth, size;	// WxH
  static final int PADDING = 2;	// since Leaf, don't get padding field

  public boolean breakBefore() { return true; }
  public boolean breakAfter() { return true; }

  // attributes NOSHADE (solid), SIZE=(thinkness), WIDTH="xx%", ALIGN= all easy to implement
  public HR(String name, Map<String,Object> attrs, INode parent) { super(name,attrs,parent); }

  /** Formats to full width in bbox, then painted according to "width" attribute. */
  public boolean formatNode(int width, int height, Context cx) {
	super.formatNode(width,height, cx); // handle stickies

	cx.flush = BOTH;	// who knew?

	valid_ = (width > 0  &&  width < PROBEWIDTH/2);

	size = Integers.parseInt(getAttr("size"), 2);
	int w = 1;
	if (valid_) {
		w = width - cx.getFloatWidth(BOTH); // overall bbox gets full width available so painting and events intersect; maintain separate width for drawing
		barwidth = 100;
		//if (barwidth > width) ...
	}
	bbox.setSize(w, size+PADDING*2); baseline=size+PADDING;

	return false;	 //!valid_; => don't shortcircuit even if not valid!
  }

  /** HR has to handle own LEFT and RIGHT align, as IParaBox needs those set on it itself. */
  public boolean paintNodeContent(Context cx, int start, int end) {
	int xoff=0;
	if (/*cx.*/align==CENTER || /*cx.*/align==RIGHT) {
		int hdiff=bbox.width-barwidth;
		if (hdiff>0) xoff = (align==CENTER? hdiff/2: hdiff);
	}

	Graphics2D g = cx.g;
	g.setColor(cx.foreground);
	if (getAttr("noshade")!=null) g.fillRect(xoff,PADDING, barwidth,size);
	else g.draw3DRect(xoff,PADDING, barwidth,size, true);
	return false;
  }

  /** Usually leaf nodes don't care about prevailing width, but HR does. */
  public void markDirtySubtreeDown(boolean leavestoo) { setValid(false); }
}