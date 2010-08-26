package edu.berkeley.span;

import java.awt.Color;

import multivalent.*;


/**
	Darken text in span -- identical to {@link edu.berkeley.lens.Darken}, except <code>extends Span</code> vs <code>extends Lens</code>.

	@version $Revision$ $Date$
*/
public class DarkenSpan extends Span {
  static Color lastfg=null, lastdfg=null, lastbg=null, lastdbg=null;


  public boolean appearance(Context cx, boolean all) {
	Color fg = cx.foreground;
	if (fg!=null) {
		if (fg==lastfg) fg = lastdfg;
		else { lastfg=fg; fg = lastdfg = fg.darker(); }
		cx.foreground=fg;
	}

	Color bg = cx.background;
	if (bg!=null) {
		if (bg==lastbg) bg = lastdbg;
		else { lastbg=bg; bg = lastdbg = bg.darker(); }
		cx.background=bg;
	}

	//if (cx.foreground!=null) cx.foreground = cx.foreground.darker();
	//if (cx.background!=null) cx.background = cx.background.darker();

	return false;
  }

  public int getPriority() { return ContextListener.PRIORITY_SPAN+ContextListener.LITTLE; }
}
