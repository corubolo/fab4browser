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
package multivalent.gui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.INode;
import multivalent.Node;
import multivalent.SemanticEvent;

import com.pt.awt.NFont;



/**
 * Movable, resizable internal window, with title bar. <i>Lens</i> subclasses to add composition of effects via a
 * LensMan(ager). The class Note subclasses to add a Document instance. Dialog subclasses to add HTML form.
 * 
 * <!--
 * <p>
 * LATER: popup for changing pinned status. maybe option to turn off apparatus => just use raw Document! maybe option to
 * write on JWindow as for menus (but what about subclassing for transparent Lens?) -->
 * 
 * @see multivalent.std.lens.Lens
 * 
 * @version $Revision$ $Date$
 * 
 * <!-- A VWindow that has a content document as given by a URL, which it automatically sizes to fit. For example, content
 * can be HTML page associated with behavior, and include a FORM for setting attributes. User closes with close box or
 * perhaps close button provided by client. Submit button should send XXX event to close. --> <!-- Design decision: 1.
 * extends Behavior + that's Lenses do it (and if switch, would want to ajust lenses to match)
 * 
 * 2. extends Document + need way for script as in HTML FORM to close and VSCript has "<doc>" A. content paint/event at
 * translations - want Document to be plain content, as for last mod's paintAfter B. content.y += titleh - super.paint()
 * for scrollbars wrong (could adjust but not in spirit of modularity)
 * 
 * 3. extends INode (content.y += titleh) -- WINNER - scrollbars require Document and everybody to respect that (which
 * usually happens, as that's required in rest of tree) + maybe useful to have plain without scrollbar, as for lens X in
 * common case when do want to attach Document that formats according to bounds, have to reach in and set bounds, which
 * isn't in spirit => use calculating size need to adjust content by titleh and BORDER - lenses need getContentBounds() -->
 */
public class VFrame extends INode implements EventListener {
	/**
	 * 
	 */
	private static final Cursor MOVEC = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

	/**
	 * Announce that Frame has changed size.
	 * <p>
	 * <tt>"frameResized"</tt>: <tt>arg=</tt> {@link VFrame} <var>instance</var>.
	 */
	public static final String MSG_RESIZED = "frameResized";

	/**
	 * Announce that Frame has been moved.
	 * <p>
	 * <tt>"frameMoved"</tt>: <tt>arg=</tt> {@link VFrame} <var>instance</var>.
	 */
	public static final String MSG_MOVED = "frameMoved";

	/**
	 * Announce that Frame has been closed.
	 * <p>
	 * <tt>"frameResized"</tt>: <tt>arg=</tt> {@link VFrame} <var>instance</var>.
	 */
	public static final String MSG_CLOSED = "frameClosed";

	/**
	 * Announce that Frame has raised above all other Frames.
	 * <p>
	 * <tt>"frameRaised"</tt>: <tt>arg=</tt> {@link VFrame} <var>instance</var>.
	 */
	public static final String MSG_RAISED = "frameRaised";

	/**
	 * Remove window controls when cursor not in frame?
	 * <p>
	 * Boolean <tt>"vanishingTitle"</tt>
	 */
	public static final String PREF_VANISHING = "vanishingTitle";

	/** Minimum dimension for window. */
	public int WIDTH_MIN = 75, HEIGHT_MIN = 40;


	
	public static NFont FONT_TITLE_NORMAL = null;

	public static NFont FONT_TITLE_BOLD = null;

	public static NFont FONT_TITLE_LIGHT = null;


	public static NFont FONT_TITLE_ITALIC = null;

	public static int titleh =17;
	protected NFont FONT_TITLE = null;

	boolean free = false;

	String title_ = null;

	/** "Pinned" to document (default) or float above. */
	boolean pinned_ = true;


	public Color titleBg = new Color(220, 220, 220);

	public static final Color grigietto = new Color(220, 220, 220);

	/** Refers to interactive resizing with mouse, not programmatic control. */
	public boolean resizable = true;

	/** Show just title bar or full window. Different than iconify. */
	public boolean lampshade = false; // should do this with another behavior

	// which hacks in

	/** Display window apparatus only when cursor within window. */
	protected boolean in_ = true; // initially on even though cursor not

	protected Point p0_;

	protected Rectangle bbox0_ = new Rectangle();

	protected Rectangle pbbox = new Rectangle();

	protected boolean resizelens_ = false;

	private boolean onresizelens_ = false;

	protected boolean movelens_ = false;

	protected boolean metagrab_ = false; // grab for contents of window

	protected long pclick = 0;

	private boolean transparent = false;

	protected boolean shrinkTitle = false;

	private boolean pr; 
	// boolean eatevent = false;

	protected boolean closable = true;



	public VFrame(String name, Map<String, Object> attr, INode parent) {
		super(name, attr, parent);
		FONT_TITLE_NORMAL = NFont.getInstance("Serif", NFont.WEIGHT_NORMAL,
				NFont.FLAG_EXPANDED, 12f);

		FONT_TITLE_BOLD = NFont.getInstance("Serif", NFont.WEIGHT_BOLD,
				NFont.FLAG_EXPANDED, 12f);

		FONT_TITLE_LIGHT = NFont.getInstance("Serif", NFont.WEIGHT_THIN,
				NFont.FLAG_EXPANDED, 12f);


		FONT_TITLE_ITALIC = NFont.getInstance("Serif", NFont.WEIGHT_SEMIBOLD,
				NFont.FLAG_ITALIC + NFont.FLAG_EXPANDED, 12f);

		titleh = (int) FONT_TITLE_NORMAL.getHeight() + 7;
		FONT_TITLE = FONT_TITLE_NORMAL;
		if (parent != null && parent == getDocument())
			setPinned(pinned_);
		setBounds(100, 100, 300, 200);
	}

	/** Content given by passed URL; pass null if want to build subtree yourself. */
	public VFrame(String name, Map<String, Object> attr, INode parent,
			URI docuri) {
		this(name, attr, parent);
		FONT_TITLE_NORMAL = NFont.getInstance("Serif", NFont.WEIGHT_NORMAL,
				NFont.FLAG_EXPANDED, 12f);

		FONT_TITLE_BOLD = NFont.getInstance("Serif", NFont.WEIGHT_BOLD,
				NFont.FLAG_EXPANDED, 12f);

		FONT_TITLE_LIGHT = NFont.getInstance("Serif", NFont.WEIGHT_THIN,
				NFont.FLAG_EXPANDED, 12f);


		FONT_TITLE_ITALIC = NFont.getInstance("Serif", NFont.WEIGHT_SEMIBOLD,
				NFont.FLAG_ITALIC + NFont.FLAG_EXPANDED, 12f);

		titleh = (int) FONT_TITLE_NORMAL.getHeight() + 7;
		FONT_TITLE = FONT_TITLE_NORMAL;

		Browser br = getBrowser();
		if (br != null && docuri != null)
			try {
				Document cdoc = new Document("content"/* null? */, null, this);
				// setTitle("Test");
				DocInfo di = new DocInfo(docuri);
				di.doc = cdoc;
				br.event(new SemanticEvent(br,Document.MSG_OPEN, di)); 
				getDocument().repaint(100);
			} catch (Exception e) {
				System.err.println("can't create document");
				e.printStackTrace();
			}
	}

	/**
	 * Bounds of window, with (x,y) relative to containing Document. public Rectangle getBounds() { Rectangle r =
	 * super.getBounds(); if (lampshade) r.setSize(r.width,titleh+1); //if (doc!=null) System.out.println("doc="+doc_+",
	 * x="+doc_.getHsb().getValue()+", y="+doc_.getVsb().getValue()); //System.out.print(r+" => "); Document
	 * doc=getDocument(); if (!pinned_) r.translate(-doc.getHsb().getValue(), -doc.getVsb().getValue());
	 * //System.out.println(" => "+r); // if (pir!=null) r.translate(-pir.x,-pir.y); return r; } public Rectangle
	 * getContentBounds() { Point p = getRelLocation(getDocument()); return new Rectangle(p.x,p.y,
	 * bbox.width,bbox.height-titleh); }
	 * 
	 */
	// resize, move maybe, but not if all lenses are interactive only
	public String getTitle() {
		if (title_ != null)
			return title_;
		Node n = getFirstLeaf();
		if (n != null) {
			return n.getName();
		}
		return "";
	}

	public void setTitle(String title) {
		title_ = title;
		repaint(100, 0, 0, bbox.width, titleh);
	}

	public void setIn(boolean in) {
		in_ = in;
	}






	/**
	 * Returns new Rectangle sized and positioned to cover content, not title bar.
	 */
	public Rectangle getContentBounds() {
		Rectangle r = new Rectangle(bbox.x, bbox.y + titleh, bbox.width,
				bbox.height - titleh);
		if (lampshade)
			r.setSize(0, 0);
		return r;
	}


	public boolean isPinned() {
		return pinned_;
	}

	/**
	 * Moves between RELATIVE and ABSOLUTE visual layers on class Document, translating coordinates so window appears at
	 * same location at present scroll.
	 */
	public void setPinned(boolean pinned) {

		Document doc = getDocument();
		if (doc != null) {
			if (pinned_ != pinned) { // translate coordinates
				int dx = doc.getHsb().getValue(), dy = doc.getVsb().getValue();
				if (pinned)
					bbox.translate(dx, dy);
				else
					bbox.translate(-dx, -dy);
			}

			remove();
			// String vizname = (pinned? "multivalent.node.IRootAbs":
			// "multivalent.node.IRootScreen");
			INode vp = doc.getVisualLayer(pinned ? "multivalent.node.IRootAbs"
					: "multivalent.node.IRootScreen");
			vp.appendChild(this);
			// vp.dump();

			pinned_ = pinned;
			doc.repaint(100);
		}
	}


	/** Windows added on top, so raise to top = remove + add. */
	public void raise() {

		INode p = getParentNode();
		Browser br = getBrowser();
		if (br != null)
			br.eventq(MSG_RAISED, this);
		if (p != null && p.getLastChild() != this) {
			//System.out.println("bbbbbbbbbbbb");
			Document doc = getDocument();
			//System.out.println(doc.bbox);
			p.removeChild(this);
			p.appendChild(this);
			//System.out.println(doc.bbox);
			// p.dump();
			if (doc != null) {
//				getBrowser().setCurDocument(doc);
				doc.repaint(100); // probably done as part of remove/add
			}
		}
	}

	public void raise(boolean a) {
		INode p = getParentNode();
		if (p != null && p.getLastChild() != this) {
			//System.out.println("bbbbbbbbbbbb");
			Document doc = getDocument();
			//System.out.println(doc.bbox);
			p.removeChild(this);
			p.appendChild(this);
			//System.out.println(doc.bbox);

			if (doc != null) {
//				getBrowser().setCurDocument(doc);
				doc.repaint(100); // probably done as part of remove/add
			}
		}
	}

	/** Identical to setLocation(x,y), setSize(width,height); */
	public void setBounds(int x, int y, int width, int height) {
		setLocation(x, y);
		setSize(width, height);
	}

	/**
	 * Set dimensions of window, including title bar. Should be fast so can interactively resize window. Use this instead
	 * of setting bbox dimensions directly (should probably enforce all Node bbox manipulation with
	 * setSize/setLocation/setBounds).
	 */
	public void setSize(int width, int height) {
		if (width != bbox.width || height != bbox.height) {
			int maxw = Math.max(bbox.width, width), maxh = Math.max(
					bbox.height, height);
			WIDTH_MIN = (int) FONT_TITLE.stringAdvance(getTitle()).getX() + 38;

			bbox.width = Math.max(width, WIDTH_MIN);
			bbox.height = Math.max(height, HEIGHT_MIN);
			markDirtySubtree(false);
			Browser br = getBrowser();
			if (br != null)
				br.eventq(MSG_RESIZED, this);
			repaint(100, 0, 0, maxw, maxh); // doesn't have to be quite as fast

		}
	}


	public void fitIntoPage() {
		int mx,my;
		mx=my=999999900;
		Document doc = ((Document) getBrowser().getRoot().findBFS("content"));
		if (getBrowser()!=null  && getBrowser().getRoot()!=null) {
			mx = doc.getHsb().getMax()- bbox.width;
			my = doc.getVsb().getMax()- titleh;
		}
		int x = Math.min(Math.max(0, bbox.x), mx);
		int y = Math.min(Math.max(0, bbox.y), my);
		if (x!=bbox.x || y!=bbox.y)
			bbox.setLocation(x,y);

	}

	/**
	 * Should be fast so can interactively move window -- in the past have minimized amount of redrawing, but at 500MHz
	 * Pentium and HotSpot, plenty fast enough to redraw entire document.
	 */
	public void setLocation(int x, int y) {
		int mx,my;
		mx=my=999999900;
		if (getBrowser()!=null && getBrowser().getRoot()!=null) {
			Document doc = ((Document) getBrowser().getRoot().findBFS("content"));
			if (doc!=null  && doc.getHsb()!=null) {
				//Document doc = ((Document) getBrowser().getRoot().findBFS("content"));
				if (!free) {
					
					mx = doc.getHsb().getMax()- bbox.width;
					my = doc.getVsb().getMax()- titleh;
				}
			}
		}
		bbox.setLocation(Math.min(Math.max(0, x), mx), Math.min(Math.max(0, y), my));
		//System.out.println("||"+ doc.getVsb().getValue());
		markDirty();
		Browser br = getBrowser();
		if (br != null) {
			//br.eventq(MSG_MOVED, this); // -- add if needed
			br.repaint(100);
		}
		//System.out.println("\\"+ doc.getVsb().getValue());
		// Document doc=getDocument(); if (doc!=null) doc.repaint(100); -- not
		// working anymore, for some reason
		// System.out.println("VFrame move repaint 100ms @ ("+x+","+y+"),
		// "+w+"x"+h);
	}



	/** Remove window from screen. */
	public void close() {
		Browser br = getBrowser();
		br.eventq(MSG_CLOSED, this); // for everybody else
		br.repaint(100);

		remove(); // simple as that!
	}

	// public static final 8int BORDER=1;
	@Override
	public boolean formatNode(int width, int height, Context cx) {
		// if (valid_) return !valid_;
		Rectangle r = new Rectangle(bbox);
		boolean ret = super.formatNode(bbox.width/*-BORDER*2*/, bbox.height
				- titleh/*-BORDER*2*/, cx);
		// bbox.add(0,0);
		int miny = Integer.MAX_VALUE;
		for (int i = 0, imax = size(); i < imax; i++)
			miny = Math.min(miny, childAt(i).bbox.y);
		int dy = titleh - miny;
		for (int i = 0, imax = size(); i < imax; i++)
			childAt(i).bbox.y += dy;
		bbox.setBounds(r); // contents have no effect on bounds
		return ret;
	}



	/*
	 * PAINT
	 */

	/**
	 * Draw content, then window apparatus (title bar, resize nib). LATER: take colors for window apparatus from style
	 * sheet.
	 */
	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		Graphics2D g = cx.g;
		Composite pc = g.getComposite();
		if (transparent) {
			AlphaComposite ac = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.4f);
			g.setComposite(ac);
		}
		if (!lampshade)
			super.paintNode(docclip, cx);

		// if (!in_ && !lampshade)
		// return;
		if (transparent) {
			AlphaComposite ac = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.9f);
			g.setComposite(ac);
		}

		// then draw window apparatus on top
		// System.out.println("bg="+cx.background);
		g.setColor(Color.BLACK);
		// g.draw3DRect(0,0, bbox.width-1,lampshade?titleh:bbox.height-1, true);
		// // -- client decides about box around body
		Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		// title bar: title, bars
		String title = getTitle();
		int titlew = (int) FONT_TITLE.stringAdvance(title).getX()+3;
		int W;
		W = bbox.width;
		g.setColor(titleBg);
		g.fillRoundRect(1, 1, W - 2, titleh, 6, 6);
		g.setColor(Color.BLACK);
		g.drawRoundRect(1, 1, W - 2, titleh, 6, 6);
		// int titlew=10;//compile hack
		FONT_TITLE.drawString(g, getTitle(), 5, FONT_TITLE.getAscent() + 5);
		// g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		// for (int i=0; i<titleh; i+=6) g.drawLine(titlew+8,i, bbox.width-1,i);
		// close box
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		//g.setColor(titleBg);
		// g.fillRoundRect(bbox.width - 14 - 1, 2, 14, titleh - 4, 6, 6);
		g.setColor(Color.BLACK);

		// close
		if (closable) {
			g.drawRoundRect(W - 12 - 2, (titleh - 10) / 2 + 1, 10, 10, 6, 6);
			g.drawLine(W - 12, (titleh - 10) / 2 + 9, W - 2 - 4,
					(titleh - 10) / 2 + 3);
			g.drawLine(W - 12, (titleh - 10) / 2 + 3, W - 2 - 4,
					(titleh - 10) / 2 + 9);
		}
		// lampshade
		//g.setColor(titleBg);
		// g.fillRoundRect(W - 28 - 1, 2, 14, titleh - 4, 6, 6);
		g.setColor(Color.BLACK);
		g.drawRoundRect(W - 26 - 1, (titleh - 10) / 2 + 1, 10, 10, 6, 6);
		// g.setColor(Color.DARK_GRAY);
		if (!lampshade)
			g.fillRoundRect(W - 26 - 1, (titleh - 10) / 2 + 7, 10, 4, 6, 6);
		else
			g.drawRoundRect(W - 25, (titleh - 10) / 2 + 3, 6, 6, 6, 6);
		g.setColor(Color.BLACK);

		// resize region
		if (resizable && !lampshade) {
			int bx = W - 2, by = bbox.height - 2;
			g.setColor(Color.BLACK);
			g.drawLine(bx - 10, by, bx, by - 10);
			g.drawLine(bx - 5, by, bx, by - 5);
		}
		g.setComposite(pc);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);

	}


	/*
	 * EVENTS
	 */
	// public boolean event(AWTEvent e, Point scrn) { eventBefore(e, rel);
	// return eventAfter(e, rel); }
	@Override
	public boolean eventBeforeAfter(AWTEvent e, Point rel) {
		int eid = e.getID();
		if (eid == MouseEvent.MOUSE_ENTERED) {
			in_ = true;
			//System.out.println("in");

			//repaint(100); /* System.out.println("* in VFrame "+getName()); */
		} else if (eid == MouseEvent.MOUSE_EXITED) {
			//&& Booleans.parseBoolean(getGlobal().getPreference(
			//	PREF_VANISHING, "true"), true)) {
			in_ = false;
		}
		if (eid == MouseEvent.MOUSE_MOVED) {
			if (!lampshade) {
				MouseEvent me = (MouseEvent) e;
				int ex = me.getX(), ey = me.getY();
				onresizelens_ = (resizable && (ex > bbox.x + bbox.width - 15) && (ey > bbox.y
						+ bbox.height - 12) && (ex <  bbox.x +bbox.width-1) && (ey < bbox.y + bbox.height -1));
				if (pr!=onresizelens_) {
					if (onresizelens_ ) {
						getBrowser().setCursor(
								Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
						//System.out.println("set");
					} else{ 
						getBrowser().setCursor(Cursor.getDefaultCursor());
						//System.out.println("set");

					}
					pr = onresizelens_;
				}	
			}
		}
		/*
		 * System.out.print("ev in "+getName()+", curnode="+getBrowser().getCurNode()+" => "); boolean ret=
		 * super.eventBeforeAfter(e, rel); System.out.println(getBrowser().getCurNode()+"
		 * "+(getBrowser().getCurNode()==this)); return ret;
		 */
		return super.eventBeforeAfter(e, rel);
	}

	/*
	 * @return true if ate event in window moving or resizing, false if in content area and available to subclass.
	 */
	// public boolean eventBefore(AWTEvent e, Point rel) {
	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		Browser br = getBrowser();
		int eid = e.getID();

		int ex = rel.x, ey = rel.y;


		if (eid == MouseEvent.MOUSE_PRESSED
				&& (((MouseEvent) e).getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			/*
			 * if (ey < titleh && (((MouseEvent)e).getModifiers() & MouseEvent.BUTTON3_MASK) !=0) { //if (ey < titleh &&
			 * (((MouseEvent)e).getModifiers() & MouseEvent.BUTTON3_MASK) !=0) { // not so great. maybe have pulldowns or
			 * something //br.eventq(new SemanticEvent(br, DocumentPopup.MSG_CREATE_DOCPOPUP, null, this, null)); //return
			 * false; return true; }
			 */

			// temporarily handle close box on mouse down
			if (ey < titleh) {
				metagrab_ = true; // don't let BindingsDefault grab mouse
				// clicks!
				// int ex=-1;
				if (ex > bbox.width - 15 && closable) { // close box
					close();
					//return false;
				} else if (ex > bbox.width - 26 && ex < bbox.width - 15 ) {
					setLampshade(!isLampshade());
					//return false;
					//getBrowser().repaint(200, bbox.x - 2, bbox.y - 2,
					//		bbox.width + 2, bbox.height + 2);

				} else {
					// Double click on the title bar to shade
					long now = System.currentTimeMillis();
					if ((now - pclick) < 350) {
						setLampshade(!isLampshade());
						pclick-=1000;
						//return false;
					} else { 
						movelens_ = true;
					}
					pclick = now;

				}

			} else if (lampshade)
				return true;

			// movelens_ = (ey<titleh);
			resizelens_ = (resizable && ex > bbox.width - 10 && ey > bbox.height - 10);

			/*
			 * } else if (eid==MouseEvent.MOUSE_PRESSED && (((MouseEvent)e).getModifiers() & MouseEvent.BUTTON3_MASK) !=0) {
			 * System.out.println("b3"); br.eventq(new SemanticEvent(br, DocumentPopup.MSG_CREATE_DOCPOPUP, null, this,
			 * null)); return true;
			 */
		}
		// MouseEvent me = (MouseEvent)e;
		// System.out.println("button "+me.getModifiers()+" /
		// "+MouseEvent.BUTTON3_MASK);


		if (metagrab_ || movelens_ || resizelens_) {
			p0_ = br.getCurScrn();
			bbox0_.setBounds(bbox);
			if (movelens_ && MouseEvent.MOUSE_FIRST <= eid
					&& eid <= MouseEvent.MOUSE_LAST
					&& !((MouseEvent) e).isAltDown())
				raise(); // alt-click moves without raising to top

			br.setCurNode(this, 0); // so titlebar doesn't disappear when drag
			br.setGrab(this); 

		} else {
			super.eventNode(e, rel);
		}

		return true; // no bleed through! (handles title bar here)
	}

	/** Handle events while moving, resizing, .... */
	public void event(AWTEvent e) {
		int eid = e.getID();

		if (eid == MouseEvent.MOUSE_DRAGGED) {
			Browser br = getBrowser();
			Point cpt = br.getCurScrn();
			if (p0_ != null) {
				int dx = cpt.x - p0_.x, dy = cpt.y - p0_.y;
				int adx = Math.abs(dx), ady = Math.abs(dy);
				if (/* adx>3*ady || ( */adx > 10 && ady <= 5/* ) */)
					dy = 0;
				else if (/* ady>3*adx || ( */adx <= 5 && ady > 10/* ) */)
					dx = 0;

				if (resizelens_)
					setSize(bbox0_.width + dx, bbox0_.height + dy);
				else if (movelens_) {
					setLocation(bbox0_.x + dx, bbox0_.y + dy);
					if (getBrowser().getCursor()!=MOVEC)
						getBrowser().setCursor(MOVEC);
				}
			}
		} else if (eid == MouseEvent.MOUSE_RELEASED) {
			Browser br = getBrowser();
			movelens_ = resizelens_ = false;
			getBrowser().setCursor(Cursor.getDefaultCursor()); 

			br.releaseGrab(this);
			metagrab_ = false;
		}

	}

	/**
	 * @return the transparent
	 */
	public boolean isTransparent() {
		return transparent;
	}

	/**
	 * @param transparent
	 *            the transparent to set
	 */
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	public boolean isLampshade() {
		return lampshade;
	}


	/**
	 * @param font_title the fONT_TITLE to set
	 */
	public void setFONT_TITLE(NFont font_title) {
		FONT_TITLE = font_title;
	}

	public void setLampshade(boolean lampshade) {
		if (this.lampshade != lampshade) {
			this.lampshade = lampshade;
			if (lampshade) {
				pbbox.setBounds(bbox);
				int titlew = (int) FONT_TITLE.stringAdvance(getTitle()).getX()+3;
				int W;
				if (lampshade && shrinkTitle)
					W = titlew + 35;
				else
					W = bbox.width;
				//paintNode(, cx)();
				//repaint(bbox.x,bbox.y,bbox.width,bbox.height);
				getBrowser().repaint(200, bbox.x - 2, bbox.y - 2,
						bbox.width + 2, bbox.height + 2);
				bbox.setBounds(bbox.x, bbox.y, W, titleh);

			} else {
				setSize(pbbox.width, pbbox.height);
				//repaint(bbox.x,bbox.y,bbox.width,bbox.height);

			}			
		}
	}

	public boolean isShrinkTitle() {
		return shrinkTitle;
	}

	public void setShrinkTitle(boolean shrinkTitle) {
		this.shrinkTitle = shrinkTitle;
	}

	public Color getTitleBg() {
		return titleBg;
	}

	public void setTitleBg(Color titleBg) {
		this.titleBg = titleBg;
		repaint(bbox.x,bbox.y,bbox.width,bbox.height);
	}

	public boolean isClosable() {
		return closable;
	}

	public void setClosable(boolean closable) {
		this.closable = closable;
	}

	public boolean isFree() {
		return free;
	}

	public void setFree(boolean free) {
		this.free = free;
	}
}
