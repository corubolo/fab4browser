package multivalent.std.ui;

import java.awt.*;
import java.awt.event.*;
import multivalent.*;


/**
	Pluggable events binding behavior duplicating some Emacs key bindings.
	Re-throws some events to BindingsDefault.

	@see multivalent.std.ui.BindingsDefault

	@version $Revision: 1.3 $ $Date: 2002/02/01 07:30:32 $
*/
public class BindingsEmacs extends Behavior implements EventListener {

  public void buildAfter(Document doc) { doc.getRoot().addObserver(this); }


  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	int eid=e.getID();
	Browser br = getBrowser(); //if (br==null) { System.out.println("BE: "+e); return false; }
	CursorMark curs = br.getCursorMark();

	// Emacs keyboard bound (gotta work on tty's) + always has a cursor
	if (eid<KeyEvent.KEY_FIRST || eid>KeyEvent.KEY_LAST || !curs.isSet() /*|| ((KeyEvent)e).isActionKey()*/) return false;

	if (eid!=KeyEvent.KEY_PRESSED) {
//		br.setGrab(this);   // so get key up -- only grab if processing key, not for any old modifier key
		/*if (br.getGrab()==this && eid==KeyEvent.KEY_TYPED) { br.releaseGrab(this); return true; }
		else return false;*/
	}

	KeyEvent ke = (KeyEvent)e;
	int keycode = ke.getKeyCode();
//System.out.println("key code = "+ke.getKeyCode()+", action="+ke.isActionKey());
//	if (ke!=null) return false;

	INode scope=br.getScope();  //INode bounds = curs.scope;
	Span sel = br.getSelectionSpan();
	Leaf cursn = (Leaf)curs.getMark().leaf; int cursoff = curs.getMark().offset;
	INode cursp = cursn.getParentNode();
	//String txt = cursn.getName();
	Document doc = br.getCurDocument();
	// general purpose iterators
	//Node n;
	Leaf l;
	//INode p;

	if (ke.isControlDown()) {
		switch (keycode) {
		case 'A': curs.move(cursp.childAt(0).getFirstLeaf(),0); break;
		case 'E': curs.move(cursp.getLastChild().getLastLeaf(), ((Leaf)cursp.getLastChild()).size()); break;
		case 'N':
			l = cursp.getLastChild().getNextLeaf();
			if (l!=null) curs.move(l,0);
			break;
		case 'P':
			l = cursp.childAt(0).getPrevLeaf();
			if (l!=null) curs.move(l,l.size());
			break;
		case 'B': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_LEFT, ke.getKeyChar(), ke.getKeyLocation())); break;
		case 'F': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_RIGHT, ke.getKeyChar(), ke.getKeyLocation())); break;
		case 'D': curs.move(+1); cursn.delete(cursoff, (Leaf)curs.getMark().leaf,curs.getMark().offset); break;
		case 'T':
			if (cursoff>0 && cursoff<cursn.size()) {
				String cuttxt = cursn.eatme(cursoff, cursn,cursoff+1);
				cursn.insert(cursoff-1, cuttxt, scope);
			}
			break;
		case 'Y': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_PASTE, ke.getKeyChar(), ke.getKeyLocation())); break;    // re-throw to DefaultBinding's Paste
		case 'V': if (scope==null/*defer to Windoze*/) br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_PAGE_DOWN, ke.getKeyChar(), ke.getKeyLocation())); break;
		default: return false;
		}
		cursn.repaint(100);


	} else if (ke.isMetaDown() || ke.isAltDown() /*meta for Windows*/) {
//System.out.println("keycode="+keycode+" vs left="+KeyEvent.VK_LEFT+" vs "+KeyEvent.VK_RIGHT);
		switch (keycode) {
		case 'F':
		case KeyEvent.VK_RIGHT:
			if (cursoff!=cursn.size()) curs.move(cursn,cursn.size());
			else { l=cursn.getNextLeaf(); if (l!=null) curs.move(l,l.size()); }
			break;
		case 'B':
		case KeyEvent.VK_LEFT:
			if (cursoff!=0) curs.move(cursn,0);
			else { l=cursn.getPrevLeaf(); if (l!=null) curs.move(l,0); }
			break;
		case 'T':
			if (cursp.size()>1) {
				Leaf swapl=cursn, swapr=cursn.getNextLeaf();
				if (cursoff<=1) { swapl=cursn.getPrevLeaf(); swapr=cursn; }
				if (swapr.getParentNode()!=cursp) { swapr=swapl; swapl=swapl.getPrevLeaf(); }
				if (swapl.getParentNode()!=cursp) { swapl=swapr; swapr=swapl.getNextLeaf(); }
				String cuttxt = swapl.eatme(0, swapl,swapl.size());   // following space too so close up
				swapr.insert(swapr.size(), ' '+cuttxt, scope);
			}
			break;
		case 'V': br.eventq(new KeyEvent(br, eid, ke.getWhen()+1, 0, KeyEvent.VK_PAGE_UP, ke.getKeyChar(), ke.getKeyLocation())); break;
		default: return false;
		}


	} else if (ke.getModifiers()==0 || ke.isShiftDown()) {
//System.out.println("BindingsEmacs, keycode="+ke.getKeyCode());
		switch (ke.getKeyCode()) {
		case 8:
		case 0x7f:
			if (curs.isSet() || sel.isSet()) return false;	// let defaults handle it
			else { doc.scrollBy(0, -(br.getHeight()-20)); break; }
		case ' ':
			if (curs.isSet() || sel.isSet()) return false;	// let defaults handle it
			else { doc.scrollBy(0, br.getHeight()-20); break; }
		default:
			return false;   // default insert

			// insert character at cursor -- make reformatting incremental: line, paragraph, ... bbox reformats iff size changed
//System.out.println("key="+ke.getKeyCode()+", keychar="+ke.getKeyChar()+", mods = "+ke.getModifiers());
			//was: if (ke.getKeyChar()!='\0') cursn.insert(cursoff, ke.getKeyChar());
/*
			if (ke.getKeyCode()>=' ' && scope!=null) {
System.out.println("BindingsEmacs insert, sel.isSet()="+sel.isSet());
				if (sel.isSet()) {
					Leaf l = (Leaf)sel.getStart().leaf;
					l.insert(cursoff, ke.getKeyChar(), scope);
					l.delete(sel.getStart().offset, (Leaf)sel.getEnd().leaf,sel.getEnd().offset);
				} else cursn.insert(cursoff, ke.getKeyChar(), scope);
				// selection kind of remove()'d automatically
			}*/

/*
			if (ke.getKeyCode()<256)
			else return false;*/
			//cursn.markDirty();
			//cursn.reformat(cursn);	  // more efficient than reformatting from root down
		}
	} else return false;

	// if cursor moved, make sure you can still see it
	Node cursnow = curs.getMark().leaf;
	if (curs.getMark().offset!=cursoff || cursnow!=cursn) {
		IScrollPane sp = cursnow.getIScrollPane();
		// not right: should should x of curssor + FUZZ, rather than end of node (which could be very long)
		if (sp!=null) sp.scrollTo(cursnow, cursnow.bbox.width,0, true);
	}

	return true;
  }


  public void event(AWTEvent e) {
	if (e.getID()==KeyEvent.KEY_RELEASED) getBrowser().releaseGrab(this);
  }
}
