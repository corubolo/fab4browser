package edu.berkeley;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.INode;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import phelps.lang.Booleans;



/**
	COG highlighter and mover after that described in
	"Creating Reusable Well-Structured PDF as a Sequence of Component Object Graphic (COG) Elements"
	Steven R. Bagley, David F. Brailsford and Matthew R. B. Hardy
	Multivalent behavior by T.A. Phelps, 2003 August 9, 5:40am - 6:50am = 1 hour 10 minutes.
 */
public class COG extends Behavior implements EventListener {
	/** <p><tt>"cogShow"</tt>: <tt>arg=</tt> boolean on/off/null to toggle. */
	public static final String MSG_SHOW = "cogShow";


	/** Conveniently, all COGs at same level of tree, since Forms off content stream, so cache parent (of null then no COGs). */
	INode cogp_ = null;
	boolean fshow_ = false;

	// for dragging
	int x0_, y0_;
	Node cog_;
	Rectangle bbox0_ = new Rectangle();



	@Override
	public boolean paintAfter(Context cx, Node node) {
		// highlight COGs
		if (fshow_) {
			Graphics2D g = cx.g;
			g.setColor(Color.ORANGE);
			for (int i=0,imax=cogp_.size(); i<imax; i++) {
				Node child = cogp_.childAt(i);
				Rectangle bbox = child.bbox;
				//System.out.println(bbox.x+"x"+bbox.y);
				g.drawRect(bbox.x, bbox.y, bbox.width, bbox.height);
			}
		}
		return super.paintAfter(cx, node);
	}

	@Override
	public void buildAfter(Document doc) {
		// collect COGs
		cogp_ = findCogP(doc);
		if (cogp_ != null) doc.addObserver(this);
		super.buildAfter(doc);
	}

	private INode findCogP(Node n) {
		if (n.isLeaf()) return null;
		else if (n.getName().startsWith("cog0")) return n.getParentNode();
		else if (n.size()>0) return findCogP(((INode)n).childAt(0));
		else return null;
	}

	@Override
	public boolean eventBefore(AWTEvent e, Point rel, Node n) {
		int eid = e.getID();
		if (super.eventBefore(e, rel, n)) return true;    // respect superclass short-circuit
		else if (fshow_ && MouseEvent.MOUSE_FIRST <= eid && eid <= MouseEvent.MOUSE_LAST) {
			// see if in a COG and if so can click and move
			if (MouseEvent.MOUSE_PRESSED==eid) {
				MouseEvent me = (MouseEvent)e;
				int x=rel.x, y=rel.y;	// in relative coordinates space
				//System.out.println("pressed "+x+","+y);
				for (int i=0,imax=cogp_.size(); i<imax; i++) {
					Node child = cogp_.childAt(i);
					Rectangle bbox = child.bbox;
					if (bbox.contains(x,y)) {
						//System.out.println("in cog "+child.getName());
						x0_ = me.getX(); y0_ = me.getY();	// event in absolute space
						cog_ = child; bbox0_.setBounds(bbox);
						getBrowser().setGrab(this);
					}
				}
			}
			return true;
		}

		return false;
	}

	public void event(AWTEvent e) {
		int eid = e.getID();
		if (MouseEvent.MOUSE_DRAGGED==eid) {
			MouseEvent me = (MouseEvent)e;
			int x=me.getX(), y=me.getY();
			Rectangle bbox = cog_.bbox;
			//System.out.println("dragged to "+x+","+y);
			bbox.setLocation(bbox0_.x + x - x0_, bbox0_.y + y - y0_);
			getBrowser().repaint(100);

		} else if (MouseEvent.MOUSE_RELEASED==eid) {
			cog_ = null;
			getBrowser().releaseGrab(this);
		}
	}

	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg)) return true;    // respect superclass short-circuit
		else if (cogp_!=null && VMenu.MSG_CREATE_VIEW==msg) {
			INode menu = (INode)se.getOut();
			VCheckbox cb = (VCheckbox)createUI("checkbox", "Show COGs", "event "+COG.MSG_SHOW, menu, VMenu.CATEGORY_MEDIUM, false);
			cb.setState(fshow_);
		}
		return false;
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (cogp_!=null && COG.MSG_SHOW==msg) {
			boolean fshow = Booleans.parseBoolean(se.getArg(), !fshow_);
			if (fshow != fshow_) { fshow_ = fshow; getBrowser().repaint(100); }
		}
		return super.semanticEventAfter(se, msg);
	}
}
