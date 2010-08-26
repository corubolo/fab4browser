package multivalent.std.ui;

import java.awt.*;
import java.awt.event.*;

import multivalent.*;



/**
	Pluggable events duplicating some Windoze key bindings.
	Re-throws some events to BindingsDefault.

	@see multivalent.std.ui.BindingsMacintosh

	@version $Revision: 1.4 $ $Date: 2005/07/28 03:42:59 $
*/
public class BindingsWindoze extends Behavior implements EventListener {
  public void buildAfter(Document doc) {
	if (! System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
		doc.getRoot().addObserver(this);
	// does Linux use Control like Windows?
  }


  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	int eid=e.getID();
	Browser br = getBrowser();
	//CursorMark curs = br.getCursorMark();
	//INode scope = br.getScope();
	//Span sel = br.getSelectionSpan();

	if (eid==KeyEvent.KEY_PRESSED) {
//System.out.println("WindozeBindings");
		KeyEvent ke = (KeyEvent)e;
		int keycode = ke.getKeyCode();

		boolean grab = true;

		// Ctrl
		if (ke.isControlDown()) {
//System.out.println("C-"+(char)keycode);
//System.out.println("WindozeBindings: control + "+keycode+"/"+(char)keycode+", curs.isSet="+curs.isSet()+", scope="+scope);
			switch (keycode) {
			// re-throw these to BindingsDefault
			case 'V': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_PASTE, ke.getKeyChar(), ke.getKeyLocation())); grab=false; break;
			case 'C': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_COPY, ke.getKeyChar(), ke.getKeyLocation())); grab=false; break;
			default:
				grab = false;
			}

			if (grab) { br.setGrab(this); System.out.println("GRAB on "+keycode); return true; }

		} else if (ke.isShiftDown()) {
			switch (keycode) {
			case KeyEvent.VK_END:
			default: // OK
			}
		}
	}

	return false;
  }


  public void event(AWTEvent e) {
	if (e.getID()==KeyEvent.KEY_RELEASED) getBrowser().releaseGrab(this);
//System.out.println(e.getID()+" => "+KeyEvent.KEY_RELEASED);
  }
}
