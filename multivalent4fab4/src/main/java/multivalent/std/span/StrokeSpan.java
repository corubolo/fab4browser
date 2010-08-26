package multivalent.std.span;

import java.awt.Font;
import java.awt.BasicStroke;

import multivalent.*;



/**
	Set attributes of line: width, cap style, join style, miter limit, dash.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@see java.awt.BasicStroke

	@version $Revision: 1.1 $ $Date: 2002/05/24 05:06:15 $
*/
public class StrokeSpan extends Span {
  public float linewidth = Context.FLOAT_INVALID;
  public int linecap = Context.INT_INVALID;
  public int linejoin = Context.INT_INVALID;
  public float miterlimit = Context.FLOAT_INVALID;
  public float[] dasharray = Context.FLOATARRAY_INVALID;
  public float dashphase = Context.FLOAT_INVALID;

  public boolean appearance(Context cx, boolean all) {
	if (linewidth!=Context.FLOAT_INVALID) cx.linewidth = linewidth;
	if (linecap!=Context.INT_INVALID) cx.linecap = linecap;
	if (linejoin!=Context.INT_INVALID) cx.linejoin = linejoin;
	if (miterlimit!=Context.FLOAT_INVALID) cx.miterlimit = miterlimit;
	if (dasharray!=Context.FLOATARRAY_INVALID) cx.dasharray = dasharray;
	if (dashphase!=Context.FLOAT_INVALID) cx.dashphase = dashphase;

	return false;
  }

  public void setStroke(BasicStroke bs) {
	linewidth = bs.getLineWidth();
	linecap = bs.getEndCap(); linejoin = bs.getLineJoin(); miterlimit = bs.getMiterLimit();
	dasharray = bs.getDashArray(); dashphase = bs.getDashPhase();
  }
}
