package multivalent.std.span;

import multivalent.*;



/**
	Convenience span for setting foreground color.
	Same as BackgroundSpan except for Context property set.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class ForegroundSpan extends BackgroundSpan {
  public boolean appearance(Context cx, boolean all) { if (color_!=Context.COLOR_INVALID) cx.foreground=color_; return false; }
}
