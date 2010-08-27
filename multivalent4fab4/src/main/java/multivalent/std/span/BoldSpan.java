package multivalent.std.span;

import multivalent.*;

import com.pt.awt.NFont;



/**
	Convenience span for setting boldface.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class BoldSpan extends Span {
  // int weight = WEIGHT_THIN .. WEIGHT_BLACK
  public boolean appearance(Context cx, boolean all) { cx.weight = NFont.WEIGHT_BOLD; return false; }
}
