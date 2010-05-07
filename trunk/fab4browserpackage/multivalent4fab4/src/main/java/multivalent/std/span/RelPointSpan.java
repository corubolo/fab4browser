package multivalent.std.span;

import java.util.Map;

import multivalent.*;

import phelps.lang.Integers;
import phelps.lang.Maths;



/**
	Common convenience span for changing the font size relative to the current size by <i>n</i> steps.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class RelPointSpan extends Span {
  public static final String ATTR_DELTA = "delta";

  int delta_=0;

  public void setDelta(int delta) { delta_=delta; }

  public static float[] validpoints = { 9f, 10f, 12f, 14f, 18f, 24f, 36f };

  public boolean appearance(Context cx, boolean all) {
	int now = (int)cx.size;
	for (int i=0,imax=validpoints.length; i<imax; i++) {
		if ((now - validpoints[i]) < 0.01) {
			int newsize = Maths.minmax(0, i+delta_, validpoints.length-1);
			cx.size = validpoints[newsize];
//System.out.println(now+" => "+cx.size);
			break;
		}
	}
	return false;
  }

  public ESISNode save() {
	putAttr(ATTR_DELTA, String.valueOf(delta_));
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	delta_ = Integers.parseInt(getAttr(ATTR_DELTA), 0);
  }
}
