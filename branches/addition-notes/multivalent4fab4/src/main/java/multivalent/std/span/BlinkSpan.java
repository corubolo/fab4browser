package multivalent.std.span;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import multivalent.*;
import multivalent.std.SyncTimerTask;



/**
	Because you asked for it!
	Works on scanned pages, DVI, PDF, in menus....
	Works by drawing setting the foreground color to the background color, or not, hooked to a timer.

	@version $Revision: 1.3 $ $Date: 2002/01/16 05:00:20 $
*/
public class BlinkSpan extends Span implements Observer {
  static int Interval = 1000;	// 1 sec between blinks, though this can be changed

  static SyncTimerTask stt = new multivalent.std.SyncTimerTask();
  static {
	Multivalent.getInstance().getTimer().schedule(stt, 1000, Interval);
  }


  boolean viz_ = true;


  /** When blinked out, draw foreground same color as background. */
  public boolean appearance(Context cx, boolean all) {
	if (!viz_) cx.foreground = cx.underline = cx.underline2 = cx.overstrike = cx.overline = cx.background;
	return false;
  }

  /** Higher priority than color changes, but not as high as SELECTION. */
  public int getPriority() { return ContextListener.PRIORITY_SPAN+ContextListener.LOT; }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	stt.getObservable().addObserver(this);
  }

  public void update(Observable o, Object arg) {
	if (o == stt.getObservable()) {
		if (viz_ != ((Boolean)arg).booleanValue()) { viz_ = !viz_; repaint(100); }
	}
  }

  public void destroy() {
	stt.getObservable().deleteObserver(this);
//System.out.println("blink -- poof");
	super.destroy();    // remove from Document
  }
}
