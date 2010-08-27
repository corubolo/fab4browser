package multivalent.std.span;

import java.util.Map;

import multivalent.*;
import phelps.lang.Booleans;



/**
	Convenience span for setting elided (aka hidden) spans.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class ElideSpan extends Span {
  /** Boolean flag, true=elide on. */
  public static final String ATTR_ELIDE = "elide";

  // need to unelide in menu some time or may as well delete!
  boolean elide_=true;

  public void setElide(boolean elide) { elide_=elide; }

  public boolean appearance(Context cx, boolean all) {
	cx.elide = elide_;	// in not elided, maybe strikethrough to show that elidable
	return false;
  }

  public ESISNode save() {
	putAttr(ATTR_ELIDE, (elide_?"true":"false"));
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	elide_ = Booleans.parseBoolean(getAttr(ATTR_ELIDE), true);
  }
}
