package multivalent.devel.demo;

import java.awt.*;
//import java.awt.geom.AffineTransform;
import java.util.Map;

import phelps.lang.Integers;

import multivalent.*;
import multivalent.std.lens.Lens;
import multivalent.EventListener;
import multivalent.gui.VMenu;



/**
	Template to use in writing a new lens.

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:16:32 $
*/
/*abstract final -- don't instantiate or subclass MyLens*/
public class MyLens extends Lens {
  /**
	Effect: Context attributes and signals.
	@see multivalent.ContextListener
	@see multivalent.std.lens.SignalLens

	@return false so it composes with other lenses
  */
  public boolean appearance(Context cx, boolean all) {
	return false;
  }


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

	@see Ruler
	@see Bounds
	@see multivalent.std.lens.Cypher

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
	Event recieved only if event coordinates fall within lens bounds.
  */
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	super.eventAfter(e, rel, obsn);
	return false;
  }


  /** Catch corresponding VFrame's windowClosed, windowRaised, .... */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// superclass handles VFrame.MSG_CLOSED and VFrame.MSG_CLOSED semantic events
	return super.semanticEventAfter(se, msg);
  }


  public ESISNode save() {
	// stuff attributes, as from fields -- Lens class saves dimensions of lens window
	putAttr("settingX", Integer.toString(5));
	putAttr("random", "comment");

	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);	// Lens class handles window dimensions and title

	// convert attributes to fields or whatever
	int settingX = Integers.parseInt(getAttr("settingX"), 5);	// get attribute as integer, with default value
	String random = getAttr("random", "scintillating");
  }

  public void destroy() {
	super.destroy();	// Lens class handles disengaging from LensMan

	// additional shutdown cleanup
  }


  /** Removes from LensMan. */
  public void close() {
	// cleanup while still attached to doc tree

	super.close();	// disengage from LensMan and GUI

	// additional cleanup
  }
}
