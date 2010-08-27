package multivalent.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TimerTask;

import phelps.lang.Maths;

import multivalent.*;



/**
	Scrollbars, both vertical and horizontal.
	Communication is done with logical values, as opposed to visual or display coordinates.
	Unless otherwise noted, method parameters and return values that are geometric
	are relative to the source being scrolled, not the internal scrollbar display.
	The scrollbar visually indicates the amount of the document displayed.

<!--
	NOTES
	with Java 2D can just set CTM and paint both with same code?
-->

	@see multivalent.IScrollPane

	@version $Revision: 1.11 $ $Date: 2003/06/02 05:20:34 $
*/
public class VScrollbar extends Leaf implements /*Adjustable,*/EventListener {
  /** Policy for showing scrollbar. */
  public static final byte SHOW_NEVER=0, SHOW_AS_NEEDED=1, SHOW_ALWAYS=2;

  /** Scrollbar orientation to pass in constructor. */
  public static final int VERTICAL=0, HORIZONTAL=1;

  /** Logical parts of scrollbar reported by {@link #idLocation(Point)}. */
  public static final int NOWHERE=0, TOPARROW=1, TOPTROUGH=2, NIB=3, BOTTOMTROUGH=4, BOTTOMARROW=5;


  /** For pickplace. */
  static final int FAR=500, FUZZ=50;

  /** Width of scrollbar (height if horizontal). */
  public static int SIZE=15;	// configurable?
  /** */
  //public static int HEIGHT_MIN = 2*arrowH + 10;

  /** Length of arrows at end of scrollbar. */
  /*final?*/ static int arrowH=10;


  // static because only one scrollbar being dragged at a time
  private static int y0_, val0_, partID_=NOWHERE;
  private static EventListener oldgrab_=null;
  // but this affects painting of all scrollbars, so can't be shared
  private boolean active_=false;	// currently dragging nib? (maybe implied by partID_)

  int lineInc_=5, blockInc_=10;	// stored or computed when needed?
  int orientation_=VERTICAL;
  int val_=0, min_=0, max_=100, sh_=max_-min_;	// source values
  int sbh_=0, esbh_=0, nibh_=0;	// scrollbar: sbh_/height (in terms of vertical), esbh_/effective height = sbh-arrow apparatus, nibh_/nib height
  byte show_ = SHOW_AS_NEEDED;

  private TimerTask tt = null;


  public VScrollbar(int orientation) { this(orientation==VERTICAL?"vscrollbar":"hscrollbar",null,null,orientation); }

  public VScrollbar(String name, Map<String,Object> attr, INode parent, int orientation) {
	super(name,attr, parent);
	orientation_=orientation;   assert orientation>=VERTICAL && orientation<=HORIZONTAL;
  }

  /** Set min and max values of source. */
  public void setMinMax(int minimum, int maximum) {
	assert minimum <= maximum;
//	if (minimum > maximum) { int tmp=minimum; minimum=maximum; maximum=tmp; }	// good?  masks errors
//	if ((min!=minimum || max_!=maximum) && min>max_) {
		min_=minimum; max_=maximum; sh_=Math.max(max_-min_,1);
		//setValue(val);	// make sure still valid.  usually client sets value afterward, but set to min in case not
		//val_ = min_;
		computeValues();
//	}
  }
  public int getMin() { return min_; }
  public int getMax() { return max_; }
  public int getValue() { return val_; }

  public void setShowPolicy(byte policy) {
	assert policy>=SHOW_NEVER && policy<=SHOW_ALWAYS;
	show_=policy;
  }
  public byte getShowPolicy() { return show_; }


  /**
	Value is in source coordinates.  Cannot set past end of scrollbar minus source equivalent of height of nib.

	@param pickplace    Smart scrolling, after Tk's text widget -pickplace option:
	If the location is already shown, do nothing;
	if it's a little way off the screen (top or bottom), scroll just enough to bring it into view;
	if it's far away, show it centered.
  */
  public void setValue(int value, boolean pickplace) {
	//if (bbox.width==0 || bbox.height==0) return;
	//if (nibh_==esbh_) return;	// fully displayed

//if (value>0) System.out.println(oldval+" => "+value+" within "+min_+".."+max_+"(=="+sh_+"?), sbh_="+sbh_);
//if (!isValid()) System.out.println("SCROLLBAR NOT VALID @ setValue="+value);
	if (!isValid()) val_ = Maths.minmax(min_, value, max_);	// if not formatted yet
	else {
		if (pickplace && sh_ > sbh_/*nibh_ < esbh_*/) {
			int top=val_, bot=val_+sbh_;
			if (value<top-FAR || value>bot+FAR) value -= sbh_/2;	// center
			else if (value-FUZZ<top) value -= FUZZ;	// just bring into top
			else if (value+FUZZ>bot) value += FUZZ - sbh_;	// just bring into bottom
			else /*if (top <= val_ && val_<=bot)*/ value=top;	// if already on screen, don't scroll
		}
//System.out.println("scroll to "+val_+"=>"+value);

		value = Math.max(min_, Math.min(value, max_-sbh_));	// X Maths.minmax(min_, value, max_-sbh_); => max_-shb_ can be negative
		if (val_!=value) {
//System.out.println("sb "+val_+" => "+value+" in "+min_+".."+max_+"/"+(max_-sbh_));
//if (val_==86 && value==0) new Throwable().printStackTrace();
			val_ = value;
			IScrollPane isp = getIScrollPane();
			if (isp.isValid()) isp.repaint(250);	// don't need to reformat, just repaint -- whole document, not just scrollbar
		}
//	   	  getRoot().event(new Event(this, Event.ACTION_EVENT, null));
//System.out.println("val: "+value+" => "+oldval+"->"+val_);
	}
  }
  /** Same as <code>setValue(<i>value</i>, false)</code>. */
  public void setValue(int value) { setValue(value, false); }

  //** Report maximum logical value currently visible. */
  //public synchronized int getVizMax() { return val_+sbh_; }

  public int getBlockIncrement() { return blockInc_; }
  public int getLineIncrement() { return lineInc_; }
/*	public void setActive(boolean b) { -- for button 2 drag-scroll
	if (b!=active_) { active_=b; repaint(250); }
  }*/


  public boolean formatNode(int width,int height, Context cx) {
	int minh = 2*arrowH + 10;

	valid_=true;
	//valid_=true;	//-- may as well always reformat as cheap, and have weird relationship with Root

	if (width==0 || height==0) bbox.setSize(0,0);
	else {
		if (orientation_==VERTICAL) bbox.setSize(SIZE,sbh_=Math.max(height,minh));
		else bbox.setSize(sbh_=Math.max(width,minh),SIZE);

		esbh_ = sbh_-2*arrowH;
//if (orientation_==VERTICAL) System.out.println("width="+width+", height="+height+", sh="+sh_+", esbh_="+esbh_+", fraction="+(esbh_/(double)sh)+", nibh_="+nibh_);
		computeValues();
//System.out.println("orientation="+orientation_+", "+min_+".."+val_+".."+max_);
	}

	return !valid_;
  }

  private void computeValues() {
	if (isValid() && sh_>0) {
//System.out.println(sh_+" "+sbh_);
		if (sh_ <= /*e*/sbh_) {
			nibh_ = esbh_;
			lineInc_ = blockInc_ = 0;
			val_ = min_;
		} else {
			nibh_ = (esbh_ * sbh_)/sh_ /*+1?*/;	// fraction in terms of sbh_, map back to esbh_ coords
			lineInc_ = Math.max(sbh_/10,10);
			blockInc_ = Math.max(sbh_-lineInc_,0);	// inc by page, with some overlapping context
//System.out.println("sh_="+sh_+", sbh_="+sbh_+", max_="+max_+", max-sbh="+(max_-sbh_)+" vs "+val_);
			val_ = Math.max(min_, Math.min(val_, max_-sbh_));	// X Maths.minmax(min_, val_, max_-sbh_);
		}
//System.out.println("val => "+val+", max-sbh_="+(max_-sbh_)+", max="+max_+", sbh_="+sbh_);
//System.out.println(name_+": val="+val+", sh_="+sh+", max_="+max_+", min="+min+", sh="+sh_);
//System.out.println("sh="+sh_+", lineInc_="+lineInc_+", blockInk="+blockInc_);
	}
  }


  /** Subclass for different scrollbar appearances. */
  public boolean paintNodeContent(Context cx, int start, int end) {
//if (orientation_==VERTICAL) System.out.println("vsb "+min_+".."+val_+".."+max_);
	if (show_==SHOW_NEVER || (show_==SHOW_AS_NEEDED && nibh_==esbh_)) return true;	// don't show
//System.out.println(getName()+": "+min_+" .. "+val_+"/"+sh_+" .. "+max_+", check: "+nibh_+"=="+esbh_+", start="+start);
	//if ((val<=min_ && val+sbh_>=max_) || start>0) return true;

	int w=bbox.width,h=bbox.height;
	Graphics2D g = cx.g;
	g.setColor(Color.WHITE);
	//g.setColor(cx.background);	// match page background?
	g.fillRect(0,0, w,h);

	g.setStroke(Context.STROKE_DEFAULT);
	g.setColor(Color.BLACK);
	//g.setColor(cx.foreground);	// match page foreground?

	if (orientation_==VERTICAL) {
		int mid=w/2, arrowHB=h-arrowH;
		// top arrow
		g.drawLine(mid,0, 0,arrowH); g.drawLine(0,arrowH, w,arrowH); g.drawLine(w,arrowH, mid,0);
		// bottom arrow
		g.drawLine(mid,h, 0,arrowHB); g.drawLine(0,arrowHB, w,arrowHB); g.drawLine(w,arrowHB, mid,h);
		// connecting line
		g.drawLine(mid,arrowH, mid,arrowHB); g.drawLine(mid+1,arrowH, mid+1,arrowHB);
		// nib
		g.setColor(Color.LIGHT_GRAY);
		g.fill3DRect(mid-arrowH/2,(val_*esbh_)/sh_+arrowH+1, arrowH+1,Math.max(nibh_,5), true/*!active_*/);
	} else {	// dual of VERTICAL -- just use ATM with Java 2D
		int mid=h/2, arrowHB=w-arrowH;
		g.drawLine(0,mid, arrowH,0); g.drawLine(arrowH,0, arrowH,h); g.drawLine(arrowH,h, 0,mid);
		g.drawLine(w,mid, arrowHB,0); g.drawLine(arrowHB,0, arrowHB,h); g.drawLine(arrowHB,h, w,mid);
		g.drawLine(arrowH,mid, arrowHB,mid); g.drawLine(arrowH,mid+1, arrowHB,mid+1);
		g.setColor(Color.LIGHT_GRAY);
		g.fill3DRect((val_*esbh_)/sh_+arrowH+1,mid-arrowH/2, Math.max(nibh_,5),arrowH+1, true/*!active_*/);
	}

	return true;
  }


  public boolean eventNode(AWTEvent e, Point rel) {
	// if not visible, ignore
	if ((val_<=min_ && val_+sbh_>=max_) || rel==null) return false;

	int y = (orientation_==VERTICAL? rel.y: rel.x);
	if (e.getID()==MouseEvent.MOUSE_PRESSED) {
		Browser br = getBrowser();
		oldgrab_=br.getGrab(); if (oldgrab_!=null) br.releaseGrab(oldgrab_);	// temporarily steal grab away from previous owner
		br.setGrab(this);	// so UP doesn't go to random target
		partID_ = idLocation(rel);

		// action on DOWN!
		switch (partID_) {	// eliminate duplication by sending a TreeEvent to event()?
		  case TOPARROW: setValue(val_-lineInc_); break;
		  case BOTTOMARROW: setValue(val_+lineInc_); break;
		  case TOPTROUGH: setValue(val_-blockInc_); break;
		  case BOTTOMTROUGH: setValue(val_+blockInc_); break;
		  case NIB: y0_=y; val0_=val_; break;
		  default: assert false: partID_;
		}
		active_=true;
		//repaint();	// show change in active state -- setValue take care of repainting Document => however, in weird coordinate space show have to redraw parent
		//getParentNode().repaint(250);	// just plain repaint() should be enough

		tt = new TimerTask() {
			public void run() {
				//event(new TreeEvent(getBrowser(), TreeEvent.HEARTBEAT));
				switch (partID_) {
				case TOPARROW: setValue(val_-lineInc_); break;
				case BOTTOMARROW: setValue(val_+lineInc_); break;
				case TOPTROUGH: setValue(val_-blockInc_); break;
				case BOTTOMTROUGH: setValue(val_+blockInc_); break;
				default:	//assert false: partID_; -- NIB
				}
			}
		};
		getGlobal().getTimer().schedule(tt, 1000L, 100L);	// press-scrolls

	} else return false;
	return true;
  }


  public void event(AWTEvent e) {
	Browser br = getBrowser();

	Point abs = getAbsLocation();
	Point scrn = br.getCurScrn();
	int y = (orientation_==VERTICAL? scrn.y-abs.y: scrn.x-abs.x) - getValue();
//System.out.println("scrollbar scrn=("+scrn.x+","+scrn.y+"), abs=("+abs.x+","+abs.y+") => "+y);
	int eid = e.getID();
/*	if (eid==TreeEvent.HEARTBEAT) {
		// keep repeating last one -- recompute partID?
		switch (partID_) {
		case TOPARROW: setValue(val_-lineInc_); break;
		case BOTTOMARROW: setValue(val_+lineInc_); break;
		case TOPTROUGH: setValue(val_-blockInc_); break;
		case BOTTOMTROUGH: setValue(val_+blockInc_); break;
		default: assert false: partID_:
		}
	} else*/ if (eid==MouseEvent.MOUSE_DRAGGED) {
		if (partID_==NIB) {
			// translate screen distance into virtual distance
			setValue(val0_ + ((y-y0_)*sh_)/esbh_);
		}

	} else if (eid==MouseEvent.MOUSE_RELEASED) {
		tt.cancel();	// press-scrolls

		br.releaseGrab(this);
		if (oldgrab_!=null) br.setGrab(oldgrab_);
		partID_=NOWHERE;
		active_=false;
		//getParentNode().repaint(100);
	}
  }


  /** Figure out what scrollbar part lies under the passed point, such as {@link #TOPARROW}. */
  public int idLocation(Point rel) {
	int y,h;

	if (orientation_==VERTICAL) { y=rel.y; h=bbox.height; } else { y=rel.x; h=bbox.width; }

//System.out.println("y="+y+", (val_+sh_)*esbh_/sh_="+(val_+sh_)*esbh_/sh_+", h="+h);
	int id=NOWHERE;
	if (y <= arrowH) id=TOPARROW;
	else if (y >= h-arrowH) id=BOTTOMARROW;
	else if (y < (val_*esbh_)/sh_) id=TOPTROUGH;
	else if (y > (val_*esbh_)/sh_ + nibh_) id=BOTTOMTROUGH;
	else id=NIB;
	return id;
  }
}
