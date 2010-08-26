package multivalent.std.lens;

import java.awt.*;

import multivalent.*;

import com.pt.awt.NFont;



/**
	Try to make things as readable as possible:
	undo affine transformations,
	no signals,
	black text on white background,
	non-bold non-italic 12-point Times Roman font,
	no blink, ....
	Can't undo arbitrary drawing by other behaviors.

	<p>LATER: make a <i>filter</i>, affecting entire document.

	@version $Revision$ $Date$
*/
public class PlainView extends Lens {
//	protected final AffineTransform identityTransform_ = new AffineTransform();

  /** Identity affine transformation. */
  public boolean paintBefore(Context cx, Node node) {
	super.paintBefore(cx, node);
	Graphics2D g = cx.g;
	g.getTransform().setToIdentity();
//	g.setTransform(identityTransform_);
	//cx.clearBase(); -- effects can come from anywhere
	return false;
  }

  /**
	Black text on white background, plain Times Roman font, no signals...:
	everything that affects painting only, not formatting.
  */
  public boolean appearance(Context cx, boolean all) {
	cx.foreground=Color.BLACK; cx.background=Color.WHITE;
	cx.family="Times"; cx.weight=NFont.WEIGHT_NORMAL; cx.flags=NFont.FLAG_SERIF; cx.size=Math.max(cx.size, 9f); // making size larger causes overlaps in small text
	cx.xor = null;
	cx.underline = cx.underline2 = cx.overline = cx.overstrike = Context.COLOR_INVALID;	 // cx.foreground => don't show
	cx.signal.clear();
	// would affect formatting:
	//cx.elide = false;
	//cx.zoom = 1.0;
	return false;
  }
}
