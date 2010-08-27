package edu.berkeley;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Event;
import java.awt.Point;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Context;
import multivalent.ContextListener;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.Node;
import multivalent.Span;



/**
	<i>Broken</i>
	HiStruct, a generalized version of

	<blockquote>
	"Hi-Cites: Dynamically Created Citations with Active Highlighting"
	Michelle Q Wang Baldonado and Terry Winograd
	Proceedings of the ACM Conference on Human Factors in Computing Systems (CHI ’98), Los Angeles, California, April 1998.
	</blockquote>

	<p>I'm not sure this is a good idea, but it was trivial to implement (a two hour hack), so let's try it in practice and see.

	<p>TO DO
	make override nested struct patterns too

	@version $Revision$ $Date$
 */
public class HiStruct extends Behavior implements EventListener {
	// just wrap existing context listener, if any, but mark structure by setting foreground and background
	// needs to extend Span so don't bomb Browser.event's rg = (Span)ovrangeActive.get(orangei);
	static class HiStructWrapper extends Span implements ContextListener {
		ContextListener cl=null;

		@Override
		public boolean appearance(Context cx, boolean all) { boolean ret=cl!=null? cl.appearance(cx,all): false; cx.foreground=Color.BLACK; cx.background=Color.ORANGE; return ret; }
		@Override
		public boolean paintBefore(Context cx, Node n) { return cl!=null? paintBefore(cx,n): false; }
		@Override
		public boolean paintAfter(Context cx, Node n) { return cl!=null? paintAfter(cx,n): false; }
		@Override
		public int getPriority() { return cl!=null? cl.getPriority(): ContextListener.PRIORITY_SPAN; }
	}


	boolean active=false;
	HiStructWrapper wrapper = new HiStructWrapper();
	String laststruct=null;
	Node lastn=null;
	ContextListener cl=null;
	/*
  public int getType(int id) { return Valence.CHECKBOX; }
  public String getCategory(int id) { return "Tool"; }
  public Object getTitle(int id) { return "HiStruct"; }
  public String getStatusHelp(int id) { return "Highlight all structures like that under cursor"; }
  public void command(int id, Object arg) {
	active = !active;
	if (!active) {	  // clear prevailing decorations
		//StyleSheet ss = getBrowser().getStyleSheet(); => currentDocument()
		//if (laststruct!=null) { if (cl==null) ss.remove(laststruct); else ss.put(laststruct, cl); }
		laststruct=null; getBrowser().repaint();
	}
  }*/

	@Override
	public void buildAfter(Document doc) { doc.addObserver(this); }

	//public boolean event(AWTEvent e, Point scrn) { eventBefore(e, scrn); return eventAfter(e, scrn); }
	//public boolean eventAfter(AWTEvent e) { return false; }
	public void event(AWTEvent e) {}
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		if (!active) return false;

		Browser br = getBrowser();
		int eid = e.getID();
		if (eid==Event.MOUSE_DOWN) {
			if (laststruct!=null && lastn.getParentNode()!=null) {
				eid=Event.MOUSE_MOVE;
				//e.target=lastn.getParentNode();
				eventBefore(e, rel, lastn.getParentNode());
				br.setGrab(this); // eat up click too
				return true;
			}
		} else if (eid==Event.MOUSE_UP) {
			br.releaseGrab(this);
			return true;

		} else if (eid==Event.MOUSE_MOVE) {
			//StyleSheet ss = br.getStyleSheet();
			Node n = null;//(Node)e.target;
			if (n!=null && n.isLeaf()) n=n.getParentNode();
			lastn=n;

			if (n!=null) {
				String structname = n.getName();	// how to go to more than one level?
				if (structname!=laststruct) {	// still on same structural entity, don't have to repaint
					// restore laststruct's style
					//if (laststruct!=null) { if (cl==null) remove(laststruct); else ss.put(laststruct, cl); }

					br.eventq(Browser.MSG_STATUS, "NAME = "+structname);

					// set name's style
					//wrapper.cl = cl = (ContextListener)ss.get(structname);
					//ss.put(structname, wrapper);

					laststruct = structname;
					br.repaint();
				}
			}
		}

		return false;
	}
}
