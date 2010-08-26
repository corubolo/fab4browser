package multivalent.std.span;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import multivalent.*;



/**
	Box the word covered by the span (later, intelligently box arbitrarily long spans).
	LATER: color (and other properties) set by stylesheet and keyed on logical tag!

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class BoxSpan extends Span {
  Color color_=Color.RED;


  public void setColor(Color c) { if (c!=null) color_=c; }

  public boolean paintBefore(Context cx, Node n) {
	Node start = getStart().leaf;
	Rectangle bbox = start.bbox;
	Graphics2D g = cx.g;
	g.setColor(color_!=Context.COLOR_INVALID? color_: cx.foreground);
	int x = (int)cx.x;
	g.drawLine(x,0, x,start.baseline);    // left
	return false;
  }

  public boolean paintAfter(Context cx, Node n) {
	Node end = getEnd().leaf;
	Rectangle bbox = end.bbox;
	Graphics2D g = cx.g;
	g.setColor(color_!=Context.COLOR_INVALID? color_: cx.foreground);
	int x = (int)cx.x;
	g.drawLine(x-1,0, x-1,end.baseline);  // right
	return false;
  }

  public boolean appearance(Context cx, boolean all) {
	if (color_!=Context.COLOR_INVALID) cx.underline = cx.overline = color_;    // top and bottom
	return false;
  }
}
