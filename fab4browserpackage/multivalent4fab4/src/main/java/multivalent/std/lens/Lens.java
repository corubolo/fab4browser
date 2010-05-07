package multivalent.std.lens;

import java.awt.Rectangle;
import java.awt.Graphics2D;
//import java.awt.event.WindowEvent;
import java.awt.Color;
import java.util.Map;

import multivalent.*;
import multivalent.node.LeafZero;
import multivalent.gui.VFrame;

import phelps.lang.Integers;



/**
	VWindows that transform their content and compose with one another.
	Lenses are tools, such as a magnifying glass or show OCR content, that are associated with the window as a whole
	(specifically, with the Root), as opposed to individual Documents.
	Unlike other VWindows, Lenses are coordinated by a <i>Lens Manager</i>, which composes their effects.
	During paintBefore, as lenses implement ContextListener, they can add themselves to a Context's list of omnipresent
	in order to modify in all Documents within its bounds.
	To put lens creation in UI, add a reference to WindowUI in hub.
	Can write a lens with opaque content, but then it's not really a lens, which transforms their content, so use a VWindow or Note.


	<blockquote>
	Xerox PARC Magic Lenses reference
	</blockquote>

<!--
1. painting
on first pass, don't draw corresponding VFrame so don't, say, magnify title bar
	paintBefore short-circuit, but hard to maintain across all lenses
paint composed lenses
	(LensMan needs to be observer on root so always called regardless of clip)
then draw (empty) VFrames
	need to know clip, which isn't passed to behaviors

2. events
get resize/move/raise events, so can raise lens behavior order and restrict sizes (as with ruler)

	Shouldn't lose title bar when pop up lens menu.
-->

	@see multivalent.std.lens.LensMan
	@see multivalent.Context
	@see multivalent.ContextListener
	@see multivalent.std.ui.WindowUI

	@version $Revision: 1.5 $ $Date: 2003/06/02 05:48:56 $
*/
public abstract class Lens extends Behavior implements ContextListener {
  public static final String ATTR_X = "x";
  public static final String ATTR_Y = "y";
  public static final String ATTR_WIDTH = "width";
  public static final String ATTR_HEIGHT = "height";
  public static final String ATTR_TITLE = "title";

  public static final String VAR_SHARED_LAYER = "<lens>";


  protected VFrame win_ = null;
  //protected Leaf content_ = null;
  //protected static Rectangle lastbbox_ = new Rectangle(100,100, 300,200);

  /** Bounds of corresponding VFrame's content area, exclusive of title bar. */
  public Rectangle getContentBounds() { return win_.getContentBounds(); }

  /**
	Return lens manager behavior that coordinates lens intersections.
	Dynamically creates if doesn't already exist.
	@see multivalent.std.lens.LensMan
  */
  protected LensMan getLensMan() {
//Layer rl = getBrowser().getRoot().getLayers();
//System.out.println("root layers = "+rl);
//for (Iterator<> i=rl.behaviorIterator(); i.hasNext(); ) System.out.println("\t"+i.next());
	return (LensMan)getBrowser().getRoot().getLayer("<lens>").getBehavior("LensMan", "multivalent.std.lens.LensMan");	// creates if doesn't already exist
  }



  // PAINT

  /** <tt>final</tt> because lens priority given by window stacking order. */
  public final int getPriority() { return ContextListener.PRIORITY_LENS; }

  /* * Override and add self to Context during paintBefore() to play tricks. */
  /* * Override to play tricks with Context. */
  /**
	Effect: Context attributes and signals.
	@see multivalent.ContextListener
	@see multivalent.std.lens.SignalLens
	@return false so it composes with other lenses
  */
  public boolean appearance(Context cx, boolean all) { return false; }

  /**
	Effect: Graphics2D transformation matrix.
	@see multivalent.std.lens.Magnify
  */
  public boolean paintBefore(Context cx, Node node) {
//System.out.println("paintBefore on "+getClass().getName());
	return super.paintBefore(cx, node);
  }

  /**
	Effect: arbitrary drawing on top.
	Can even traverse tree for special effects (that don't compose with other lenses).
	Warning: this type of effects don't compose as well with other lenses.

	@see multivalent.devel.lens.Ruler
	@see multivalent.devel.lens.Bounds
	@see Cypher

	@return false so it composes with other lenses
  */
  public boolean paintAfter(Context cx, Node node) {
	//win_.paintBeforeAfter(g, cx);
	//public void paintBeforeAfter(Rectangle docclip, Context cx) {
	// draw bounds or lens can disappear!
	Graphics2D g = cx.g;
	Rectangle r=getContentBounds(); g.setColor(Color.BLACK); g.drawRect(r.x,r.y, r.width-1,r.height-1);
	return super.paintAfter(cx, node);
  }

	// by default, pass events through (corresponds to transparent lenses)
	// XXX by default, catch all events.  have to explicitly allow events to fall through
	// some lenses transform coordinates in e, pass on
  /**
	Lenses that warp coordinates should replicate that here.
	Event received only if event coordinates fall within lens bounds.
  */
/*
  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	if (super.eventBefore(e, rel, obsn)) return true;

	win_.eventBeforeAfter(e, rel);
	return false;
  }
*/
  /**
	Lenses that warp coordinates should <em>reverse</em> that here.
  */
/*
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	return super.eventAfter(e, rel, obsn);
  }
*/


  /** Catch corresponding VFrame's windowClosed, windowRaised, .... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println(msg+", "+(se.getArg()==win_));
	if (win_!=null && se.getArg()==win_) {
		if (VFrame.MSG_RAISED==msg) {
			//close(); show();
			getLensMan().raiseLens(this);
//System.out.println("raising lens");
		} else if (VFrame.MSG_CLOSED==msg) {
			close();
		}
	}
	return super.semanticEventAfter(se, msg);
  }


  public ESISNode save() {
	//if (lensman_.containsLens(this)) putAttr("posted", phelps.Utility.DEFINED); else removeAttr("posted");
	// also save pinned, lampshade, resizable, ...
	//if (lensman.containsLens(this)) putAttr("posted", phelps.Utility.DEFINED); else removeAttr("posted");

	// should have some superclass for this
	Rectangle bbox = win_.bbox;
	putAttr(ATTR_X,Integer.toString(bbox.x)); putAttr(ATTR_Y,Integer.toString(bbox.y));
	putAttr(ATTR_WIDTH,Integer.toString(bbox.width)); putAttr(ATTR_HEIGHT,Integer.toString(bbox.height));

	return super.save();
  }


  /** Creates corresponding VFrame, and sets title and bounds, which are available for subclasses to tweak, */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);

	// set up frame
	String cname = getClass().getName();
	win_ = new VFrame(cname,null, null/*initially not shown*/);
	win_.setPinned(false);

	//content_ =
	new LeafZero("OVERWRITTEN",null, win_);
	//content_.addObserver(this);
	//win_.addObserver(this);

	// set up bbox
	//win_.setBounds(Integers.parseInt(getAttr(ATTR_X),lastbbox_.x), Integers.parseInt(getAttr(ATTR_Y),lastbbox_.y), Integers.parseInt(getAttr(ATTR_WIDTH),lastbbox_.width), Integers.parseInt(getAttr(ATTR_HEIGHT),lastbbox_.height));
	win_.setBounds(Integers.parseInt(getAttr(ATTR_X),100), Integers.parseInt(getAttr(ATTR_Y),100),  Integers.parseInt(getAttr(ATTR_WIDTH),300), Integers.parseInt(getAttr(ATTR_HEIGHT),200));
//System.out.println("win bbox = "+win_.bbox);


	// take title from attrs
	String title = getAttr(ATTR_TITLE, null);
	if (title==null) {
		// set default title; overrideable by TITLE attribute in restore or by setting field in subclass constructor
		title = cname;
		int inx=title.lastIndexOf('.'); if (inx!=-1) title=title.substring(inx+1);
		if (title.endsWith("Lens") && title.length()>4) title=title.substring(0,title.length()-"Lens".length());
	}
	win_.setTitle(title);

	//if (getAttr("posted")!=null) lensman_.addLens(this);
	show(); //-- client's control?
  }


  public void destroy() {
	getLensMan().deleteLens(this);
	super.destroy();
  }

  /** Removes from LensMan. */
  public void close() {
	//save(null, -1);
	//lastbbox_.setBounds(win_.bbox);

	win_.remove();	// just in case
	win_=null;

	getBrowser().repaint(100);	// repaint everything because lenses like Magnify have greater range
	destroy();
//System.out.println("*** closing lens: "+getClass().getName());
  }


  /** Adds to LensMan. */
  public void show() {
//System.out.println("showing "+getName());
	// make sure last position in whatever Document now fits in current Document
	Document doc = getDocument();
	win_.setLocation(Math.min(win_.bbox.x, doc.bbox.width-10), Math.min(win_.bbox.y, doc.bbox.height-10));

	LensMan lm = getLensMan();
	//INode viz = getRoot().getVisualLayer("lens", "multivalent.std.node.IRootAbs");
	lm.getVisualLayer().appendChild(win_);

//System.out.println("*** show lens: "+getClass().getName()+", win_="+win_);
	lm.addLens(this);
	getBrowser().repaint(100);
  }
}
