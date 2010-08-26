package multivalent;

import java.awt.AWTEvent;


/**
	Classes can implement this in order to be able to seize the grab.

	@see Browser#setGrab(EventListener).

	@version $Revision: 1.2 $ $Date: 2002/02/16 21:46:14 $
 */
public interface EventListener extends java.util.EventListener {    // extend or not?
	void/*boolean -- no ss!  ss=>before/after */ event(AWTEvent e);    // no Point -- in event or available from Browser
	//boolean eventBefore(AWTEvent e);
	//boolean eventAfter(AWTEvent e);
}
