package multivalent.std.lens;

import java.awt.*;
//import java.awt.geom.AffineTransform;
import java.util.Map;

import multivalent.*;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;
//import multivalent.EventListener;
import multivalent.std.ui.DocumentPopup;

import phelps.lang.Integers;



/**
	Enlarge content by scaling <tt>Graphics2D</tt>.
	This lens composes well, as opposed to BitMagnify.

	To do
	<ul>
	<li>Transform coordinates within lens, so can select within.
	<li>Separate scaling on x and y axes?
	</ul>

	@version $Revision: 1.2 $ $Date: 2002/02/08 14:39:12 $
*/
public class Magnify extends Lens {
  /**
	Create a mangify lens.
	<p><tt>"magnifyLens"</tt>.
  */
  public static final String MSG_MAGNIFY = "magnifyLens";

  public static final String ATTR_ZOOM = "zoom";
  public static final String ATTR_ZOOMS = "zooms";


  static int izoom_ = -1;

  int[] zooms_ = null;
  int zoom_;
  //double scalex=2.0, scaley=2.0;

  protected VRadiogroup rg_ = new VRadiogroup();

  private Point rel_ = new Point();
//  private boolean warp_;


  /*
   * PAINT
   */
  public boolean paintBefore(Context cx, Node node) {
	//if (g==null) return true;

	super.paintBefore(cx, node);
	//Graphics2D magg = (Graphics2D)g.create();
	Rectangle r = getContentBounds();		// should adjust according to scale
//System.out.println("Magnify paintBefore");
	//g.setClip(r);
	Graphics2D g = cx.g;
	double f = zoom_/100.0;
	//g.translate(r.x,r.y);	// don't want these to concatenate
	//g.translate(-r.x,-r.y);	// don't want these to concatenate
	g.translate(r.x - r.x*f, r.y - r.y*f);
	//g.scale(scalex, scaley);
	g.scale(f,f);
	return false;
	//return true;
  }

/*
  public boolean paintAfter(Context cx, Node node) {
	//g.scale(1.0/scalex, 1.0/scaley);	-- different Graphics2D than paintBefore
	super.paintAfter(g, cx, node);
	return false;
  }*/

  /** Transform mouse coordinates to match magnified.
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
  //public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	if (super.eventBefore(e, rel, obsn)) return true;

	// transform magnified space => unmag space
	rel_.setLocation(rel);
	//double f = zoom_/100.0;
	Rectangle r = getContentBounds();
	//rel.setLocation(r.x + (int)((rel.x-r.x)/f), r.y + (int)((rel.y-r.y)/f));
//System.out.println(rel_+ " => "+rel);
	rel.x = ((rel.x - r.x)*100/zoom_) + r.x;
	rel.y = ((rel.y - r.y)*100/zoom_) + r.y;

	//return false;
	return super.eventAfter(e,rel, obsn);
  }

  public boolean eventAfterX(AWTEvent e, Point rel, Node obsn) {
	/*if (warp_) {* / rel.setLocation(rel_); //warp_=false; }
	return super.eventAfter(e,rel, obsn);
  }
*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	//scalex = scaley = Doubles.parseDouble(getAttr("scale"), scalex);
	//scalex=Doubles.parseDouble(getAttr("scalex"), scalex); scaley=Doubles.parseDouble(getAttr("scaley"), scaley);

	if (izoom_<0) zoom_ = izoom_ = Integers.parseInt(getAttr(ATTR_ZOOM),200);
	else zoom_ = Integers.parseInt(getAttr(ATTR_ZOOM),izoom_);

	String[] szooms = getAttr(ATTR_ZOOMS, "50,125,150,200,300,400").split(",");
	zooms_ = new int[szooms.length];
	for (int i=0,imax=szooms.length; i<imax; i++) {
		try { zooms_[i] = Integer.parseInt(szooms[i].trim()); } catch (NumberFormatException nfe) { zooms_[i]=100; }
	}

	win_.setTitle("Magnify "+zoom_+"%");
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_) {
		Browser br = getBrowser();
		INode menu = (INode)se.getOut();
		rg_.clear();
		for (int i=0,imax=zooms_.length; i<imax; i++) {
			VRadiobox rb = (VRadiobox)createUI("radiobox", "Magnify "+zooms_[i]+"%", new SemanticEvent(br, MSG_MAGNIFY, new Integer(zooms_[i]), this, null), menu, "VIEW", false);
			rb.setRadiogroup(rg_);
			rb.setState(zooms_[i]==zoom_);
		}
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	if (MSG_MAGNIFY==msg && se.getIn()==this && arg instanceof Integer) {
		int newscale = ((Integer)arg).intValue();
		if (newscale != zoom_) {
			zoom_ = izoom_ = newscale;
			win_.setTitle("Magnify "+zoom_+"%");
			getBrowser().repaint();
		}
	}
	return super.semanticEventAfter(se, msg);
  }
}
