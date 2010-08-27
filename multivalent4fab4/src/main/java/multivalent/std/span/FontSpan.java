package multivalent.std.span;

import java.util.Map;

import phelps.lang.Integers;
import phelps.lang.Floats;

import com.pt.awt.NFont;

import multivalent.*;



/**
	Convenience span for setting font properties: family, size, style.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.1 $ $Date: 2002/02/02 13:16:26 $
*/
public class FontSpan extends Span {
  public static final String ATTR_FAMILY = "family";
  public static final String ATTR_SIZE = "size";
  public static final String ATTR_WEIGHT = "weight";
  public static final String ATTR_FLAGS = "flags";


  /** Family name, or null to disable. */
  public String family=null;
  /** Size in logical points.  Set to -1 to disable. */
  public float size = -1f;
  public int weight = -1;
  public int flags = -1;
  /** Alternatively, spot font. */
  public NFont spot = null;

  public boolean appearance(Context cx, boolean all) {
	if (family!=null) cx.family=family;
	if (size > 0.0) cx.size = size;
	if (weight != -1) cx.weight = weight;
	if (flags != -1) cx.flags = flags;
	if (spot!=null) cx.spot = spot;
	return false;
  }

  public ESISNode save() {
	putAttr(ATTR_FAMILY, family); // if null, removed
	if (size>0.0) putAttr(ATTR_SIZE, Float.toString(size));
	if (weight >= 0) putAttr(ATTR_WEIGHT, Integer.toString(weight));  // later symbolic
	if (flags >= 0) putAttr(ATTR_FLAGS, Integer.toString(flags));
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);

	family = getAttr(ATTR_FAMILY, null);
	size = Floats.parseFloat(getAttr(ATTR_SIZE), 0.0f);
	weight = Integers.parseInt(getAttr(ATTR_WEIGHT), -1);
	flags = Integers.parseInt(getAttr(ATTR_FLAGS), -1);
  }
}
