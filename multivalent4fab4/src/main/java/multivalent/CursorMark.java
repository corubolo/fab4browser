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
package multivalent;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import multivalent.std.SyncTimerTask;


/**
    Blinking cursor.
    Could be Mark subclass, but as Behavior we can remap.

    @version $Revision$ $Date$
 */
public class CursorMark extends Behavior implements /*EventListener,*/ Observer {
	/**
    Announce cursor has been moved to new point.
    <p><tt>"setCursor"</tt>: <tt>arg=</tt> {@link CursorMark} <var>this</var>.
	 */
	public static final String MSG_SET = "setCursor";


	private static SyncTimerTask stt_ = new multivalent.std.SyncTimerTask();
	static {
		Multivalent.getInstance().getTimer().schedule(CursorMark.stt_, 1000, 500);
	}


	private static boolean viz_=true;
	//boolean insert_=true;   // insert or replace
	private Mark posn_ = new Mark(null,-1, this);


	public Mark getMark() { return posn_; }
	// should be that only Browser can build one?

	public boolean isSet() { return posn_.isSet(); }

	public void move(Mark m) { if (m==null) move(null,-1); else move(m.leaf, m.offset); }

	public void move(Leaf n, int offset) {
		if (posn_.isSet()) repaint(100);   // unshow old
		posn_.move(n,offset);
		moved(50L);
	}

	public void move(int delta) {    // handle jumping from leaf to leaf here
		if (posn_.isSet()) repaint(100);
		posn_.move(delta);
		moved(10L);
	}

	private void moved(long ms) {
		if (posn_.isSet()) {
			//protected boolean knownviz_=false;    // when cursor moves, scroll as necessary to show it
			// ShowHeaders moves...
			//Browser br = getBrowser();
			//Document doc = getStart().leaf.getDocument();
			//if (doc!=br.getCurDocument()) br.setCurDocument(doc);
			CursorMark.viz_=true;
			repaint(10);
			getBrowser().eventq(CursorMark.MSG_SET, this);
		}
	}


	public void update(Observable o, Object arg) {
		if (o == CursorMark.stt_.getObservable()) {
			CursorMark.viz_ = ((Boolean)arg).booleanValue();
			repaint(100);
		}
	}


	@Override
	public boolean paintAfter(Context cx, Node start) {
		if (CursorMark./*insert_ &&*/ viz_) {
			Graphics2D g = cx.g;
			g.setColor(Color.BLACK);
			Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			//g.drawLine(cx.x,bbox.y, cx.x,bbox.y+start.baseline);
			int del = start.bbox.height / 7;
			int del2 = start.bbox.height / 9;
			g.drawLine((int)cx.x,del, (int)cx.x,start.bbox.height-del2/*start.baseline*/);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,prevAA);
			//xoff_ = cx.x-bbox.x; => accurate if painted, but may be offscreen
			//System.out.println("height = "+start.baseline+", @ "+cx.x+", y=0.."+start.bbox.height); //xoff_);
		}
		return false;
	}

	public int getPriority() { return ContextListener.PRIORITY_SPAN + ContextListener.LITTLE; }


	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		super.restore(n,attr, layer);
		CursorMark.stt_.getObservable().addObserver(this);
	}

	@Override
	public void destroy() {
		move(null);
		CursorMark.stt_.getObservable().deleteObserver(this);
		super.destroy();
	}

	public void repaint() { repaint(0); }
	public void repaint(long ms) {
		if (isSet()) posn_.leaf.repaint(ms);
	}

	//public void remove() { move(null); }     // don't remove from layer

	/** Remove self when referenced document is closed. */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (Document.MSG_CLOSE==msg && isSet() && getMark().leaf.getDocument()==se.getArg()) move(null);
		return false;
	}
}
