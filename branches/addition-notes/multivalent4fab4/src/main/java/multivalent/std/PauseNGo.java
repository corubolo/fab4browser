package multivalent.std;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.TimerTask;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.Node;

import phelps.lang.Integers;



/**
	Wait for mouse click for <tt>DELAY</tt> ms, then execute <tt>CMD</tt> with argument <tt>ARG</tt>.

	@version $Revision: 1.2 $ $Date: 2002/01/16 03:54:28 $
*/
public class PauseNGo extends Behavior {
  public static final String ATTR_DELAY = "delay";
  public static final String ATTR_CMD = "cmd";
  public static final String ATTR_ARG = "arg";


  TimerTask tt;

  public void buildBefore(Document doc) {
	try {
		doc.getRoot().addObserver(this);	// click anywhere

		tt = new TimerTask() { public void run() { go(); } };
		getGlobal().getTimer().schedule(tt, Integers.parseInt(getAttr(ATTR_DELAY), 5000));

	} catch (NumberFormatException e) {}
  }

  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	if (e.getID()==MouseEvent.MOUSE_CLICKED) go();
	return true;	// always shortcircuit
  }

  public void go() {
	tt.cancel();
	getBrowser().eventq(getAttr(ATTR_CMD), getAttr(ATTR_ARG));
  }

  public void destroy() {
//System.out.println("*** PauseNGo destroy");
	tt.cancel();
  }
}
