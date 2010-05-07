package edu.berkeley.span;

import multivalent.*;



/**
	Lighten text in span.
	Along with LightenLens, shows equivalent power of lenses and spans.

	@version $Revision$ $Date$
*/
public class LightenSpan extends Span {

  public boolean appearance(Context cx, boolean all) {
	cx.foreground = cx.foreground.brighter();
	cx.background = cx.background.brighter();
	return false;
  }

  public int getPriority() { return ContextListener.PRIORITY_SPAN+ContextListener.LITTLE; }
}
