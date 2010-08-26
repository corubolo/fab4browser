package multivalent.std.span;

import multivalent.*;


/**
	Span for setting invisible text (foreground==background).

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class InvisibleSpan extends Span {
  public Object bogus=new Object();

  // when doc passed, change this to cx.fg = cx.bg = doc's default bg
  public boolean appearance(Context cx, boolean all) {
	cx.foreground=cx.background;	// still occupies space
	//cx.overstrike = layer_.getAnnoColor();  // show extent of invisible => separate span if desired
	return false;
  }
}
