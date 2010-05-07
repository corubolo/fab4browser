package multivalent.std.span;

import multivalent.*;


/**
	Same as FamilySpan(..., "Monospaced").
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class MonospacedSpan extends Span {
  public boolean appearance(Context cx, boolean all) { cx.family="Monospaced"; return false; }
}

