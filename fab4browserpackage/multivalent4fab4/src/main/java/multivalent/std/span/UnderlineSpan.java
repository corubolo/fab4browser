package multivalent.std.span;

import multivalent.*;

/**
	Convenience span for setting underline.
	Applications should usually instead use a generic {@link multivalent.Span} with a name, and set display properties in the stylesheet.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class UnderlineSpan extends Span {
  public int getPriority() { return ContextListener.PRIORITY_SPAN+1; }   // let others go first so can pick up any new foreground color at this point
  public boolean appearance(Context cx, boolean all) { cx.underline=cx.foreground; return false; }
}
