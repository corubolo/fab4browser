package multivalent.std.span;

import java.awt.Color;
import java.util.Map;

import multivalent.*;


/**
	Convenience span for setting overstrike.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class OverstrikeSpan extends Span {
  public static final String ATTR_COLOR = "color";

  Color color_=null;

  public void setColor(Color color) { color_=color; }

  public boolean appearance(Context cx, boolean all) { cx.overstrike=(color_!=null? color_ : cx.foreground); return false; }

  public ESISNode save() {
	if (color_!=null) putAttr(ATTR_COLOR, "#"+Integer.toHexString(color_.getRGB()).substring(2)); else removeAttr(ATTR_COLOR);
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attrs, Layer layer) {
	super.restore(n,attrs,layer);
	String attr=getAttr(ATTR_COLOR);
	if (attr!=null) try { color_=new Color(Integer.parseInt(getAttr(ATTR_COLOR).substring(1),16)); } catch (NumberFormatException e) { System.out.println("bad color spec "+getAttr(ATTR_COLOR)); }
  }
}
