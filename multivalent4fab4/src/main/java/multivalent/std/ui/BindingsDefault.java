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
import multivalent.CursorMark;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.Node;
import multivalent.Span;
import multivalent.TreeEvent;
import multivalent.std.span.SelectionSpan;



/**
	Event bindings that are the same across platforms, such as right arrow key moving the cursor right.
	Other bindings, as for Emacs and Windoze, convert their more involved bindings to an event for this,
	as for instance Emacs rethrows a C-y event as a KeyEvent.VK_PASTE event.

	Pluggable events via hub document: pan-document type mouse and keyboard events.
	Other modules can provide Emacs, vi, Macintosh, Windows, ...

	@see multivalent.std.ui.BindingsEmacs
	@see multivalent.std.ui.BindingsTk

	@version $Revision: 1.11 $ $Date: 2008/09/08 09:15:29 $
 */
public class BindingsDefault extends Behavior implements EventListener {
	static final boolean DEBUG=false;

	Mark selpivot = new Mark(null,-1);	// same as cursor.	need to draw this
	//int dragx=0, dragy=0;
	//boolean inmediasres=false; => same as grab
	boolean wordwise_ = false;

	public static final Point _DUMMY = new Point();

	/** Adds self as observer on root in order catch mouse and key events. */
	@Override
	public void buildAfter(Document doc) { doc.getRoot().addObserver(this); }



	@Override
	public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
		int eid=e.getID();
		
		//MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null);
		//System.out.println("+"+me);
	
		if (eid==MouseEvent.MOUSE_PRESSED/* && !(me.isMetaDown() && !((me.getModifiersEx()& MouseEvent.BUTTON3_DOWN_MASK)!=0))*/) 
			getBrowser().setScope(null);
		return super.eventBefore(e, rel, obsn);
	}



	@Override
	public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
//		System.out.println("eventAfter: BindingsDefault");

		Browser br = getBrowser();
		INode scope = br.getScope();
		//System.out.println("a");
		//if (br.getGrab()!=null && br.getGrab()!=this) return false;	// ugh
		Document doc = br.getCurDocument();
		CursorMark curs = br.getCursorMark();
		Mark cursm=curs.getMark(); Leaf cursn=cursm.leaf; int cursoff=cursm.offset;
		//Mark curm = br.getCurMark();
		//Node curp=br.getCurNode();
		//Leaf curn=curm.leaf; int curoff=curm.offset;
		Node curn=br.getCurNode(); int curoff=br.getCurOffset();
		Span sel = br.getSelectionSpan();
		//Point scrnpt = getBrowser().getCurScrn();
		//Point scrnpt = rel;
		int eid = e.getID();
		MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null);
		KeyEvent ke = (KeyEvent.KEY_FIRST<=eid && eid<=KeyEvent.KEY_LAST? (KeyEvent)e: null);
//		System.err.println("curoff = "+curoff);
//		if (eid!=MouseEvent.MOUSE_MOVED) System.out.println("Default event id="+eid+", mods="+((InputEvent)e).getModifiers());

		//System.out.println("meta="+e.metaDown()+", control="+e.controlDown()+", shift="+e.shiftDown()+", alt="+((e.modifiers&Event.ALT_MASK)!=0));
		if (e instanceof KeyEvent && rel==_DUMMY) { 
			//System.out.println("key="+((KeyEvent)e).getKeyCode()+", mods = "+((KeyEvent)e).getModifiers());
//			System.out.println(ke);
			//Character.ke.getKeyChar()
			//System.out.println("m"+(rel == _DUMMY) + " "+ ke);
		}

		if (eid==MouseEvent.MOUSE_PRESSED && me.isShiftDown()) {  // not e.shiftDown() because want SHIFT only
			// shift-click extends selection (ignore and pretent didn't see previous MOUSE_UP)
			// if no previous selection, treat as regular MOUSE_DOWN
			br.requestFocus();
			//!!! if (sel.isSet() || curs.isSet()) { e.modifiers = e.modifiers^Event.SHIFT_MASK; return eventAfter(e); }
			return false;

		} else if (eid==MouseEvent.MOUSE_PRESSED && (me.getModifiers() & InputEvent.BUTTON1_MASK) !=0) {

			if (curs.isSet() && me.getClickCount()==2) {
				Leaf n = cursm.leaf;
				sel.move(n,0, n,n.size());
				selpivot.move(n,0); curs.move(null,-1);
				n.repaint(100);

				/*inmediasres=true;*/ br.setGrab(this);	// lost grab with intermediate release!
				wordwise_ = true;

			} else {
				if (curn==null && scope!=null) {   // blank area of editable region move to end (should be start?)
					curn = scope.getLastLeaf();
					if (curn!=null) curoff=curn.size();
				}
				if (curn!=null && curn.isLeaf() && curoff>=0/* same as n.isLeaf()*/) {
					curs.move((Leaf)curn,curoff); //curs.viz=true;
//					System.out.println("setting cursor to "+curn+"/"+curoff);
					br.requestFocus();
					curn.repaint(100);

					/*inmediasres=true;*/ br.setGrab(this);
					wordwise_ = false;
				}
				//}

				// regardless of setting cursor or clicking out of bounds, zap selection
				if (sel.isSet()) {
					sel.repaint(100);	// put area on queue to be restored
					sel.moveq(null);	//remove();	// clean up old selection
					selpivot.remove();	// start new span -- selpivot set to cursor on drag
				}
//				save absolute bbox of node so can use for MOUSE_DRAG
				//} else return false; // don't eat if don't do anything
			}
		} else if (eid==KeyEvent.KEY_PRESSED && ke.isActionKey() && rel==_DUMMY) {
			boolean shift = ke.isShiftDown()/*, ctrl = ke.isControlDown()*/;
			if (ke.getModifiers()==0) switch (ke.getKeyCode()) {
			// should throw appropriate events (e.g., SCROLL_PAGE_DOWN) instead
			case KeyEvent.VK_PAGE_DOWN: doc.scrollBy(0, doc.bbox.height-20); break;
			case KeyEvent.VK_PAGE_UP: doc.scrollBy(0, -(doc.bbox.height-20)); break;
			case KeyEvent.VK_DOWN: doc.scrollBy(0, 20); break;
			case KeyEvent.VK_UP: doc.scrollBy(0, -20); break;
			case KeyEvent.VK_HOME:
				if (scope==null) {
					/*if (ke.isControlDown())*/ doc.scrollTo(0,0);
				} else { // apply to cursor as in Windows?  are we an editor or a browser?  if cursor set, we're an editor?
					// move to WindozeBindings?
					Leaf scopel=scope.getFirstLeaf();
					if (shift) {
						if (curs.isSet()) { selpivot.move(cursn, cursoff); curs.move(null); }
						sel.move(scopel,0, selpivot.leaf,selpivot.offset);	// maybe just move to end of line (while leaves have same baseline)
					} else {
						sel.move(null); curs.move(scopel, 0);
					}
				}
				break;
			case KeyEvent.VK_END:
//				System.err.println("Default, VK_END, scope="+scope);
				if (scope==null) {
					/*if (ke.isControlDown())*/ doc.scrollTo(0,Integer.MAX_VALUE);
				} else {
					Leaf scopel=scope.getLastLeaf(); int scopeend=scopel.size();
					if (shift) {
						if (curs.isSet()) { selpivot.move(cursn, cursoff); curs.move(null); }
						sel.move(selpivot.leaf,selpivot.offset, scopel,scopeend);
					} else {
						sel.move(null); curs.move(scopel, scopel.size());
					}
				}
				break;

			case KeyEvent.VK_RIGHT:
				curs.move(+1);
				if (scope!=null && !scope.contains(curs.getMark().leaf)) curs.move(-1);
				break;

			case KeyEvent.VK_LEFT:
				curs.move(-1);
				if (scope!=null && !scope.contains(curs.getMark().leaf)) curs.move(+1);
				break;

			case KeyEvent.VK_COPY:
//				System.out.println("BindingsDefault COPY");
				br.eventq(StandardEdit.MSG_COPY, null);
				break;

			case KeyEvent.VK_PASTE:
//				System.out.println("BindingsDefault PASTE");
				br.eventq(StandardEdit.MSG_PASTE, null);
				break;

			} else return false;

			if (scope!=null && scope.contains(cursn)) cursm.scrollTo();

		} else if (eid==KeyEvent.KEY_PRESSED && ke.isControlDown() && rel==_DUMMY) {
			switch (ke.getKeyCode()+'a'-1) {
			case 'l':
				br.getRoot().markDirtySubtree(true);	// until incremental reformat works on harder cases
				br.repaint();		 // useful for debugging incremental repaint
				break;
			default: return false;
			}

		} else if (eid==KeyEvent.KEY_PRESSED && rel==_DUMMY) {//&& (ke.getModifiers()==0 || ke.isShiftDown()) 
			int keycode = ke.getKeyCode();
			boolean precursset = curs.isSet();     // pre-delete


			// key pressed and span selected
			if (scope!=null && sel.isSet()) {   // centralized selection deletion
				Leaf l=sel.getStart().leaf; int off=sel.getStart().offset;
				l.delete(off, sel.getEnd().leaf,sel.getEnd().offset);
				cursn=cursm.leaf; cursoff=cursm.offset; // update to post delete values
//					System.out.println("AA");
			}

			switch (keycode) {
			case KeyEvent.VK_BACK_SPACE: // backspace
				if (scope!=null) {
					if (precursset) {
						Leaf l=null; int off=0;
						Mark m=curs.getMark(); l=m.leaf; off=m.offset;	// after moved cursor
						Leaf t = l.getPrevLeaf();
						if (t!=null && l!=null)
							if (t.getDocument() != l.getDocument())
								t=null;					
						if (off==0 && t==null){
							break;
						}
						curs.move(-1);	// delete previous character
						off--;

						if (off >=0 && off<cursoff)
							l.delete(off, cursn,cursoff);
						else if (off<0){
						}
						if (off==0 && t!=null){
							cursm.move(new Mark(t, t.size()));//, newoffset)rNode(t, t.size()-1);
							t.insert(t.size(), ' ', scope);
						} 
					}
				} else doc.scrollBy(0, -(doc.bbox.height-20));
				break;
			case KeyEvent.VK_DELETE: // delete
				if (scope!=null) {
					if (precursset) {
						Leaf l=null; int off=0;
						Mark m=curs.getMark(); l=m.leaf; off=m.offset;	// after moved cursor
						Leaf t = l.getNextLeaf();
						if (t!=null && l!=null)
							if (t.getDocument() != l.getDocument())
								t=null;					
						if (off==l.size() && t==null){
							break;
						}
						cursoff++;
//						System.out.println("--"+off+"  "+cursoff);

						if (cursoff <=l.size() && off>curoff)
							l.delete(off, cursn,cursoff);
						else if (off>l.size()){

						}
						//else break;
						if (off==l.size() && t!=null){
							//System.out.println(t);
							cursm.move(new Mark(t,0));//, newoffset)rNode(t, t.size()-1);
							//t.insert(t.size(), ' ', scope);

						} 
					}

				} else doc.scrollBy(0, -(doc.bbox.height-20));
				break;
			default:

			return false;
			}
			if (scope!=null && scope.contains(cursn)) cursm.scrollTo();

		} else if (eid==KeyEvent.KEY_TYPED && rel==_DUMMY) { 
			char keyChar = ke.getKeyChar();
			if (scope!=null && sel.isSet()) {   // centralized selection deletion
				Leaf l=sel.getStart().leaf; int off=sel.getStart().offset;
				l.delete(off, sel.getEnd().leaf,sel.getEnd().offset);
				cursn=cursm.leaf; cursoff=cursm.offset; // update to post delete values
//				System.out.println("nn");
			}

			if (keyChar == ' '&& scope==null){
				doc.scrollBy(0, doc.bbox.height-20);
				//System.out.println("1");
			}else if (scope==null) {
				//System.out.println("2");

			}else if (keyChar =='\n') {
				//System.out.println("3");
				if (curs.isSet())
					cursn.split(cursoff);
				if (scope.contains(cursn)) 
					cursm.scrollTo();
				br.repaint();
			}
			else if (keyChar!=KeyEvent.CHAR_UNDEFINED && keyChar != '\b' &&  keyChar != 127
					&& curs.isSet()) {
				//System.out.println("4");
				cursn.insert(cursoff, keyChar, scope);
				//System.out.println("AAA " + ke);
				if (scope.contains(cursn)) cursm.scrollTo();
			} else return false;

		}else return false;
		return true;
	}



	public void event(AWTEvent e) {
		int eid = e.getID();
		Browser br = getBrowser();
		CursorMark curs = br.getCursorMark();
		Mark cursm=curs.getMark(); //Leaf cursn=cursm.leaf; int cursoff=cursm.offset;
		Span sel = br.getSelectionSpan();

		if (eid == MouseEvent.MOUSE_RELEASED) { // regardless of modifiers
			/*inmediasres=false;*/ br.releaseGrab(this);
			br.clipboard();	// on demand in Java 1.1
			if (curs.isSet()) br.eventq(CursorMark.MSG_SET, curs);   // had grab when these fired
			else if (sel.isSet()) br.eventq(SelectionSpan.MSG_SET, sel);   // had grab when these fired


			//} else if (debug && eid==MouseEvent.MOUSE_MOVED) {
			//showStatus("x="+e.x+", y="+e.y); -- GridBag bug in Cafe
			//br.eventq(Browser.MSG_STATUS, "

			// selection
//			} else if (inmediasres) {
		} else if (eid==MouseEvent.MOUSE_DRAGGED) {
			// should restrict search to current inode? (cache its abs x,y and subtract from scrnpt)
			//scrnpt.translate(+3,0);
			//Mark m = /*br.getCurDocument()*/doc.getRoot().findDFS(scrnpt);
			//br.eventq("findNode",null); => semantic events not passed through tree anymore
			//br.event(new TreeEvent(this, TreeEvent.FIND_NODE)/*, scrnpt/*, null*/);
//			System.out.print(""+curn+" => ");
			br.setCurNode(null);
			br.getRoot().eventBeforeAfter(new TreeEvent(this, TreeEvent.FIND_NODE), br.getCurScrn());
			//Mark curm = br.getCurMark(); Leaf curn=curm.leaf; int curoff=curm.offset;	// update after FIND (grab set)
			Node curn=br.getCurNode(); int curoff=br.getCurOffset();
//			System.out.println(curn);

//			System.out.println("wordwise_ = "+wordwise_+", offset="+curoff);
			//scrnpt.translate(-3,0);
//			if (curm!=null) System.out.println("dragged over "+curn+"/"+curoff);
			//if (curoff>=0 /* same as curn.isLeaf() -- not anymore */) {
			if (curn!=null && curn.isLeaf() && curoff>=0) {
				Leaf curl=(Leaf)curn;
//				if (!(curn.isLeaf())) System.out.println("not a leaf!");
				//if (wordwise_) curoff = (curoff<curn.size()/2? 0: curn.size()); -- maybe, but refine

				Node oselendnode; int oselendoff;
				if (curs.isSet()) {	// dragging cursor into pivot+selection
					if (cursm.leaf.size()==1 && cursm.offset==1) cursm.move(-1);   // hack
					selpivot.move(cursm);
					oselendnode = cursm.leaf; oselendoff=cursm.offset;
					curs.move(null);
				} else {	// started dragging outside of leaf, so have to set pivot point
					if (!selpivot.isSet()) selpivot.move(curl,curoff);
					oselendnode = sel.getEnd().leaf; oselendoff=sel.getEnd().offset;
				}

				if (!(oselendnode==curn && oselendoff==curoff)) {
					sel.move(selpivot.leaf,selpivot.offset, curl,curoff);
//					System.out.println("extending selection to "+curn+"/"+curoff);
				}
//				System.out.println("moving endpoint to "+curn+"/"+curoff);
//				System.out.println("selection now "+sel.getStart().leaf+"/"+sel.getStart().offset+".."+sel.getEnd().leaf+"/"+sel.getEnd().offset);
			}
		}
	}
}
