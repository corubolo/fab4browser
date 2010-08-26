/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.ui;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.Node;


public class BindingsMacintosh extends Behavior implements EventListener {
  @Override
public void buildAfter(Document doc) {
	if (System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
		doc.getRoot().addObserver(this);
  }
  


  
  @Override
public boolean eventBefore(AWTEvent e, Point rel, Node n) {
	// TODO Auto-generated method stub
		int eid=e.getID();
		MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null);
		//System.out.println("+"+me);
		if ((eid==MouseEvent.MOUSE_PRESSED ) && me.isMetaDown() && !((me.getModifiersEx()& InputEvent.BUTTON3_DOWN_MASK)!=0)) {
			Browser br = getBrowser();
			//S4ystem.out.println("2"+me);
			MouseEvent me2 = new MouseEvent(br, eid, me.getWhen()+1, InputEvent.BUTTON3_DOWN_MASK ,me.getX(), me.getY(),me.getClickCount(),true,MouseEvent.BUTTON3);
			//System.out.println("3"+me2);
			br.eventq(me2);
			return true;
//
			}
		
//			System.out.println("ss");
//			System.out.println(eid);
		
		return super.eventBefore(e, rel, n);
}
  /**
	Where possible, convert into a standard key and let BindingsDefault catch it;
	for example, M-v => ACTION_PASTE.
  */
  @Override
public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	int eid=e.getID();
	Browser br = getBrowser();
//	//CursorMark cur = br.getCursorMark();
//	MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null);
//	System.out.println(me);
//	if (me.)
//	if (eid==MouseEvent.MOUSE_PRESSED && me.isPopupTrigger()) {
//		br.eventq(new MouseEvent(br, eid, me.getWhen()+1, 0,me.getX(), me.getY(),me.getClickCount(),true,MouseEvent.BUTTON2));
//		System.out.println("ss");
//		System.out.println(eid);
//	}
	if (eid==KeyEvent.KEY_PRESSED) {
		//if (seized) br.setGrab(this);   // so get key up
		KeyEvent ke = (KeyEvent)e;
		int keycode = ke.getKeyCode();

		boolean grab = true;

		// Meta
		if (ke.isMetaDown()) {
			switch (keycode) {
			// re-throw these to BindingsDefault
			case 'V': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_PASTE, ke.getKeyChar(), ke.getKeyLocation())); grab=false; break;
			case 'C': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_COPY, ke.getKeyChar(), ke.getKeyLocation())); grab=false; break;
			//case 'W': br.eventq(UiBehavior.MSG_CLOSETAB,getBrowser());//)Fab4.getMVFrame(getBrowser()).closeTab();
			default:
				grab = false;
			}
		}
	}
	return false;
  }

public void event(AWTEvent e) {
	// TODO Auto-generated method stub
//	int eid=e.getID();
//	MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null);
//	System.out.println("+"+me);
//	if ((eid==MouseEvent.MOUSE_PRESSED ) && me.isMetaDown() && !((me.getModifiersEx()& MouseEvent.BUTTON3_DOWN_MASK)!=0)) {
//		Browser br = getBrowser();
//		System.out.println("2"+me);
//		MouseEvent me2 = new MouseEvent(br, eid, me.getWhen()+1, MouseEvent.BUTTON3_DOWN_MASK ,me.getX(), me.getY(),me.getClickCount(),true,MouseEvent.BUTTON3);
//		System.out.println("3"+me2);
//		br.eventq(me2);
//		//return false;
////
//		}
//	
}

//  public void event(AWTEvent e) {
//	if (e.getID()==KeyEvent.KEY_RELEASED) getBrowser().releaseGrab(this);
//  }
}
