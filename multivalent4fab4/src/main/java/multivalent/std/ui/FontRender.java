package multivalent.std.ui;

import java.awt.RenderingHints;
import java.util.Map;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;

import phelps.lang.Booleans;

import com.pt.awt.NFont;



/**
	Font rendering control: fractional metrics.
	Bitmap placement usually pretty good but still not as good, so let user choose.

	@version $Revision: 2.0 $ $Date: 2005/07/28 19:37:26 $
*/
public class FontRender extends Behavior {
  public static final String MSG_FONT_BITMAP_CACHE = "font:bitmap cache";


  private boolean bitmap_;

  /** Stashed values outside of current paintBefore/paintAfter. */
  private boolean bitmapin_;


  public FontRender() {
	bitmap_ = Booleans.parseBoolean(getPreference("font:bitmaps", "true"), true);
  }

  public void buildBefore(Document doc) {
	super.buildBefore(doc);

	doc.getRoot().addObserver(this);
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		SemanticEvent seout = new SemanticEvent(getBrowser(), MSG_FONT_BITMAP_CACHE, null, this, null);
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Cache Fonts to Bitmaps", seout, (INode)se.getOut(), "View", false);
		cb.setState(bitmap_);
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_FONT_BITMAP_CACHE==msg) {
		bitmap_ = Booleans.parseBoolean(se.getArg(), !bitmap_);
		putPreference("font:bitmaps", bitmap_? "true": "false");
		getBrowser().repaint();
	}
	return super.semanticEventAfter(se, msg);
  }



  public boolean paintBefore(Context cx, Node node) {
	if (super.paintBefore(cx, node)) return true;
	//else if (java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getFullScreenWindow() != null) {}  // full screen is wacky enough as is
	else {
		bitmapin_ = NFont.isUseBitmaps();
//System.out.println("bitmaps? "+bitmap_);
		NFont.setUseBitmaps(bitmap_);
	}
	return false;
  }

  public boolean paintAfter(Context cx, Node node) {
	NFont.setUseBitmaps(bitmapin_);
	return super.paintAfter(cx, node);
  }
}
