package multivalent.std.span;

import java.util.Map;

import multivalent.*;

import phelps.lang.Integers;



/**
	Convenience span for subscripts and superscripts.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class SubSupSpan extends Span {
  public static final String ATTR_DELTA = "delta";

  private int delta_=0;


  public void setDelta(int y) { delta_=y; }

  public boolean appearance(Context cx, boolean all) {
	cx.ydelta += (delta_==Integer.MAX_VALUE? cx.getFont().getAscent()/3: delta_==Integer.MIN_VALUE? - cx.getFont().getAscent()/3: delta_);
	return false;
  }

  public ESISNode save() {
	//if (delta_==0) return null; // ?
	if (delta_!=0) putAttr(ATTR_DELTA, String.valueOf(delta_)); else removeAttr(ATTR_DELTA);
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	delta_ = Integers.parseInt(getAttr(ATTR_DELTA), 0);
  }
}
