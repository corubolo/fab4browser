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

import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Map;

import multivalent.gui.VScrollbar;


public class IScrollPane extends INode /*implements EventListener -- dragging splitter */ {

	/************
	 * 
	 * This is the only addition needed
	 * 
	 * 
	 */

	public void setHsb(VScrollbar h) {

		hsb_=h;
		hsb_.setParentNode(this);
	}
	public void setVsb(VScrollbar v) {

		vsb_=v;
		vsb_.setParentNode(this);
	}














	/**
    Scroll document to point that shows <var>arg</var>.
    <p><tt>"scrollTo"</tt>: <tt>arg=</tt> {@link Span} <var>start-of-span-if-set</var><br />
    <p><tt>"scrollTo"</tt>: <tt>arg=</tt> {@link Node} <var>Node</var><br />
    <p><tt>"scrollTo"</tt>: <tt>arg=</tt> {@link Integer} <var>absolute-position</var>, <tt>in=</tt> {@link multivalent.Node} <var>node in tree</var> (usually IScrollPane or Document)<br />
    <p><tt>"scrollTo"</tt>: <tt>arg=</tt> {@link String} <var>name-of-anchor</var>, <tt>in=</tt> {@link multivalent.Node} <var>node in tree</var> (usually IScrollPane or Document)
	 */
	public static final String MSG_SCROLL_TO = "scrollTo";

	/**
    Report that contents of pane have been formatted.
    <p><tt>"formattedPane"</tt>: <tt>arg=</tt> {@link IScrollPane} <var>instance</var>
	 */
	public static final String MSG_FORMATTED = "formattedPane";


	/** Bounding box of content. */
	public Rectangle ibbox = new Rectangle();
	/** Is content editable?  This is the wrong way to do this, and will be changed in the future. */
	public boolean editable=false;    // NOT RIGHT PLACE

	//int zoom = 100;  // NOT RIGHT PLACE => zooming done in separate node (null name or external to content)

	protected int wchars_=0, hchars_=0;   // => VTextArea?
	/** Expand to available space? */
	boolean wexp_=false, hexp_=false;
	VScrollbar vsb_ = new VScrollbar(VScrollbar.VERTICAL), hsb_ = new VScrollbar(VScrollbar.HORIZONTAL);


	public IScrollPane(String name, Map<String,Object> attr, INode parent) {
		super(name,attr, parent);
		vsb_.setParentNode(this); hsb_.setParentNode(this);   // they point to me, but aren't part of content tree

		bbox.setSize(0,0);  // size should be set externally
	}


	@Override
	public IScrollPane getIScrollPane() { return this; }

	@Override
	public int dx() { return super.dx() - getHsb().getValue(); }
	@Override
	public int dy() { return super.dy() - getVsb().getValue(); }


	/** Scrollbars, which uncharacteristically aren't reachable by walking the tree.  That may change. */
	public VScrollbar getVsb() { return vsb_; }
	public VScrollbar getHsb() { return hsb_; }

	/* no! scrollbar just set display.  also have getScreenLocation to capture that
  public Point getAbsLocation() { return super.getAbsLocation(new Point(0,0)); }
  protected Point getAbsLocation(Point loc) {
    loc.translate(hsb_.getValue(), vsb_.getValue());
    return super.getAbsLocation(loc);
  }
	 */

	/**
    Convenience method to set both scrollbars to same {@link multivalent.gui.VScrollbar} policy.
    Access scrollbars individually to set to different policies.
	 */
	public void setScrollbarShowPolicy(byte policy) {
		//assert... policy validity checked by VScrollbar
		getVsb().setShowPolicy(policy); getHsb().setShowPolicy(policy);
	}

	/**
    Compute dimensions from number of characters of prevailing font.
    To disable, set to 0.
	 */
	public void setSizeChars(int widthchars, int heightchars) {
		assert widthchars>=0 && heightchars>=0: widthchars+"x"+heightchars;
		wchars_=widthchars; if (widthchars==0) { bbox.width=0; wexp_=false; }   //wchars_ = (widthchars==0);
		hchars_=heightchars; if (heightchars==0) { bbox.height=0; hexp_=false; }
		// mark dirty and repaint...
	}


	/**
    Usually set dimensions externally, but can
    set either or both dimensions to dynamically expand to fill the available width or height
    by setting the corresponding parameter to true.
    If neither bounding box nor setSizeChars is set prior to formatting, this triggers dynamic expansion.
  public void dynamicDimensions(boolean expwidth, boolean expheight) {
    wexp_=expwidth; if (wexp_) wchars_=-1;
    hexp_=expheight; if (hexp_) hchars_=-1;
  }
	 */


	/**
    If length of dimension {@linkplain #setSizeChars(int, int) set in characters} or to dynamic (these mutually cancel each other out--last one set wins),
    use that to set corresponding bounding box dimension.
    Else maintain length, which should have been set externally beforehand.
    Sends {@link #MSG_FORMATTED}.
	 */
	@Override
	public boolean formatNode(int width,int height, Context cx) {
		int vpol = getVsb().getShowPolicy(), hpol = getVsb().getShowPolicy();
		boolean fvsb = vpol!=VScrollbar.SHOW_NEVER, fhsb = hpol!=VScrollbar.SHOW_NEVER;

		//System.out.println("format IScrollPane "+getName()+" "+wexp_+" in="+height+": "+bbox.height+" =>"+bbox.height);
		int xin=bbox.x, yin=bbox.y, win=bbox.width, hin=bbox.height;

		if (wchars_>=1) win = (int)(wchars_ * cx.getFont().charAdvance('X').getX()) + (fvsb? VScrollbar.SIZE: 0);
		else if (wexp_/*previously expand*/ || win<=0/*first time*/) { win=width; wexp_=true; }
		//else keep explicitly set bbox width
		if (hchars_>=1) hin = (int)(hchars_ * cx.getFont().getHeight()) + (fhsb? VScrollbar.SIZE: 0);
		else if (hexp_ || hin<=0) { hin=height; hexp_=true; }

		int effw=win-(fvsb? VScrollbar.SIZE: 0), effh=hin-(fhsb? VScrollbar.SIZE: 0);
		//System.out.println("formatNode width = "+wchars_+" chars => "+bbox.width+" pixels");
		//System.out.println("w: "+win+"=>"+effw+", h: "+hin+"=>"+effh);


		boolean shortcircuit = super.formatNode(effw,effh, cx);
		//System.out.println(getName()+" ibbox "+bbox+", shortcircuit="+shortcircuit+", bbox="+win+"x"+hin+", wexp_="+wexp_+", hexp_="+hexp_);
		int cw=bbox.x+bbox.width, ch=bbox.y+bbox.height;    // just observe formatting that has already taken place
		ibbox.setBounds(0,0, cw,ch);
		// enlarge child to fill page? NO, e.g., TeX DVI

		bbox.setBounds(xin,yin, win,hin);   // unaffected by size of contents


		// scrollbars
		boolean
		needV = VScrollbar.SHOW_ALWAYS==vpol || VScrollbar.SHOW_AS_NEEDED==vpol && ibbox.height > effh,
		needH = VScrollbar.SHOW_ALWAYS==hpol || VScrollbar.SHOW_AS_NEEDED==hpol && ibbox.width > effw;

		if (VScrollbar.SHOW_AS_NEEDED==hpol && VScrollbar.SHOW_AS_NEEDED==vpol  &&  ibbox.width<=win && ibbox.height<=hin) needV=needH=false;   // edge case: each needed only because of other
		else {  // need at least one but maybe not both
			if (VScrollbar.SHOW_AS_NEEDED==vpol && !needH) needV = ibbox.height > hin;
			if (VScrollbar.SHOW_AS_NEEDED==hpol && !needV) needH = ibbox.width > win;     // X formatted content to effh width and can only display that much, so have to keep horizontal scrollbar as is = not true.  clip to full window and draw scrollbars on top
		}

		vsb_.markDirtySubtreeDown(true); vsb_.formatBeforeAfter(VScrollbar.SIZE, needH?effh:hin, cx); vsb_.bbox.setLocation(effw,0); vsb_.setMinMax(0, ibbox.height);
		hsb_.markDirtySubtreeDown(true); hsb_.formatBeforeAfter(needV?effw:win, VScrollbar.SIZE, cx); hsb_.bbox.setLocation(0,effh); hsb_.setMinMax(0, ibbox.width);


		Browser br = getBrowser();
		if (br!=null) br.event/*no q*/(new SemanticEvent(br, IScrollPane.MSG_FORMATTED, this));

		return shortcircuit;
	}


	/**
    If IScrollPane not valid when needs to be painted, format it now.
    Generally, formatting should be done lazily, on demand just before it needs to be painted (including in the IScrollPane subclass Document).
	 */
	@Override
	public void paintBeforeAfter(Rectangle docclip, Context cx) {
		if (!isValid()) {
			//int win = bbox.width, hin = bbox.height;
			if (size()==0) new Leaf("",null, this);   // otherwise empty => 0x0
			formatBeforeAfter(bbox.width, bbox.height, cx);   // Context valid for painting and formatting here => move to paintNode?
			//if (bbox.width==0) bbox.setSize(win, hin);
		}
		super.paintBeforeAfter(docclip, cx);
	}

	/**
    Paints contents in scrolled, clipped {@link java.awt.Graphics2D}.
	 */
	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		Graphics2D g = cx.g;

		int w=bbox.width, h=bbox.height;

		Rectangle saveclip = g.getClipBounds();

		int dx=getHsb().getValue(), dy=getVsb().getValue();
		int pl=padding.left, pt=padding.top;
		int bl=border.left, br=border.right, bt=border.top, bb=border.bottom;

		// already done?
		//System.out.println("fill ("+(dx-pl)+","+(dy-pt)+" "+(w-bl-br)+"x"+(h-bt-bb)+" with "+cx.background+"   "+getLastLeaf());
		if (cx.background!=null) {
			// LATER: don't fill in area with child that is instanceof IScrollPane
			g.setColor(cx.background/*java.awt.Color.ORANGE*/);
			g.fillRect(dx-pl,dy-pt, w-bl-br,h-bt-bb);   // IScrollPane always draws background
		}

		//  g.clipRect(dx,dy, w-pl-pr-bl-br,h-pt-pb-bt-bb);     // intersect clips!
		//g.clipRect(dx-pl,dy-pt, w-pr-bl-br,h-pb-bt-bb);       // intersect clips! ok
		//g.clipRect(dx-pl,dy-pt, w-pr,h-pb);       // intersect clips!
		g.clipRect(dx-bl-pl,dy-bt-pt, w,h);///*-bl-br,h-bt-bb);     // latest
		//g.clipRect(bbox.x,bbox.y, bbox.height,bbox.width);

		//System.out.println(g.getClipBounds()+" vs "+docclip);
		super.paintNode(docclip, cx);   // content

		g.setClip(saveclip);

		// Graphics2D already scrolled on entry, via {@link #dx()} and {@link #dy()},
		// so momentarily undo the content coordinates to paint scrollbar.
		// Scrollbars painted last as let content overflow that space, and paint over iff scrollbar isn't maxxed out.
		g.translate(dx,dy); docclip.translate(-dx,-dy); // reverse of usual to undo
		vsb_.paintBeforeAfter(docclip,cx); hsb_.paintBeforeAfter(docclip,cx);
		g.translate(-dx,-dy); docclip.translate(dx,dy);
	}




	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		boolean eat = false;

		// in scrollbars?
		int dx=getHsb().getValue(), dy=getVsb().getValue();
		if (rel!=null) rel.translate(-dx,-dy);  // back in screen space
		//System.out.println("IScrollPane "+rel+" in "+getVsb().bbox);
		eat = eat || vsb_.eventBeforeAfter(e, rel);     // could handle mouse wheel SCROLL_EVENT regardless of cursor position
		eat = eat || hsb_.eventBeforeAfter(e, rel);
		if (rel!=null) rel.translate(dx,dy);

		if (!eat) eat=super.eventNode(e, rel);

		if (!eat) {
			int eid = e.getID();
			if (eid == MouseEvent.MOUSE_WHEEL) { // seems like you'd handle this in VScrollbar, but want action if anywhere in pane
				MouseWheelEvent mwe = (MouseWheelEvent)e;
				int type = mwe.getScrollType();
				int inc = type==MouseWheelEvent.WHEEL_UNIT_SCROLL? vsb_.getLineIncrement(): type==MouseWheelEvent.WHEEL_BLOCK_SCROLL? vsb_.getBlockIncrement(): 1;
				
				eat = true;    // only innermost nested Document
				
				if (mwe.isShiftDown())
					hsb_.setValue(hsb_.getValue() + inc * mwe.getWheelRotation());
				else
					vsb_.setValue(vsb_.getValue() + inc * mwe.getWheelRotation());
			} else if (eid==MouseEvent.MOUSE_PRESSED && editable) {
				Browser br = getBrowser();
				if (br.getScope()==null) br.setScope(this);    // defer to smaller nested editable region
				//System.out.println("setting scope to "+this+" / "+curs.getMark()+", eat="+eat);
			}
		}

		return eat;
		//return true;    // no bleed through!  -- Document does this, and maybe lack gives IScrollPane some flexibility
	}

	/* current scroll position of node has nothing to do with position in absolute document coordinations
  public Point getRelLocation(Point loc, INode relto)
	 */

	// make these events?  current routing not well suited
	// => just have clients access scrollbars
	/** Scroll by a delta x and y.  Positive x moves right; positive y moves down. */
	public void scrollBy(int dx, int dy) { hsb_.setValue(hsb_.getValue()+dx); vsb_.setValue(vsb_.getValue()+dy); }

	// MAYBE AXE ALL THESE scrollTo and juat have people deal with scrollbars directly

	/** Scroll to an absolute x and y. */
	public void scrollTo(int x, int y) { scrollTo(null, x,y, false); }
	//public void scrollTo(int x, int y, boolean pickplace) { scrollTo(null, x,y, pickplace); }

	/**
    Pickplace option described in VScrollbar.
    @see multivalent.gui.VScrollbar
	 */
	public void scrollTo(Node node, int xoff, int yoff, boolean pickplace) {
		Document doc = getDocument();
		if (!doc.isValid())
			//formatBeforeAfter(bbox.width, bbox.height, new Context()); => go up to Document for style sheet
			doc.formatBeforeAfter(doc.bbox.width, doc.bbox.height, null);   // in case haven't painted

		// three traversals for scrolling: node up to find IScrollPane, up in IScrollPane to verify ancestory, up again in getRelLocation
		// but that's ok since scrolling doesn't get the pounding of a mousemove
		if (node!=null && contains(node)) {
			//System.out.println("IScrollPane.scrollTo "+node+", "+node.getRelLocation(this)+" vs ymax="+vsb_.getMax());
			Point p = node.getRelLocation(this);
			xoff+=p.x; yoff+=p.y;
		}
		//System.out.println("ISP "+getName()+", valid = "+doc.isValid()+", yoff="+yoff);
		vsb_.setValue(yoff, pickplace); hsb_.setValue(xoff, pickplace);
	}



	@Override
	public boolean checkRep() {
		assert super.checkRep();

		// exactly one child?  frequently for layout, but without get absolute positioning

		return true;
	}

	@Override
	public void dump(int level, int maxlevel) {
		//    System.out.println(ibbox);
		super.dump(level, maxlevel);
	}

}
