package multivalent.std.span;

import multivalent.*;

import com.pt.awt.NFont;



/**
	Convenience span for setting italics.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class ItalicSpan extends Span {
  public boolean appearance(Context cx, boolean all) { cx.flags ^= NFont.FLAG_ITALIC; return false; }
}
