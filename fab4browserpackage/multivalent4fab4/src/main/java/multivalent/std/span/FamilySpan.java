package multivalent.std.span;

import java.util.Map;

import multivalent.*;


/**
	Convenience span for setting font family.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class FamilySpan extends Span {
  public static final String ATTR_FAMILY = "family";

  String family_=null;

  public void setFamily(String family) {
	// maybe valid family name or map to closest match
	family_=family;
  }

  public boolean appearance(Context cx, boolean all) { if (family_!=null) cx.family=family_; return false; }

  public ESISNode save() {
	putAttr(ATTR_FAMILY, family_);
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	String family = getAttr(ATTR_FAMILY);
	if (family!=null) family_=family;
  }
}
