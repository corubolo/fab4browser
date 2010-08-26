package multivalent.std.ui;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

import multivalent.*;
import multivalent.gui.VMenu;



/**
	General annotation functionality: wipe annos from document, ...

	@version $Revision: 1.3 $ $Date: 2002/01/14 21:50:20 $
*/
public class Annos extends Behavior {
  /**
	Wipes annotations from current page.  In a multipage document this is one page of many; in HTML, this is the entire document.
	<p><tt>"wipeAnnos"</tt>
  */
  public static final String MSG_WIPEPAGE = "wipeAnnos";

  /**
	Wipes annotations from all pages of current document.  In a multipage document this more than one page; in HTML, this the same as {@link #MSG_WIPEPAGE}.
	<p><tt>"wipeAnnos"</tt>
  */
  public static final String MSG_WIPEALL = "wipeAllAnnos";

  public static final String CATEGORY_ZAP = "zap";


// *** MOVE THIS OUT OF CORE AND USE SHARED ANNOTATION MANAGER ***
// or at least property of Layer
/*
	"Black", "#000000", "Green", "#008000", "Silver", "#C0C0C0", "Lime", "#00FF00",
	"Gray", "#808080",	"Olive", "#808000", "White", "#FFFFFF",  "Yellow", "#FFFF00",
	"Maroon", "#800000", "Navy", "#000080",  "Red", "#FF0000",	 "Blue", "#0000FF",
	"Purple", "#800080","Teal", "#008080",	"Fuchsia", "#FF00FF","Aqua", "#00FFFF"
*/
  protected static final Color[] annoColors_ = {
	/*Color.BLACK*/ /*hard to see these: Color.GREEN, new Color(0xc0c0c0), new Color(0x00ff00),
	Color.GRAY, new Color(0x808000),*/ /*Color.WHITE*/ /*Color.YELLOW*/
	new Color(0x800000), new Color(0x000080), Color.RED, Color.BLUE,	/* red and blue common so put later on */
	new Color(0x800080), new Color(0x008080), new Color(0xff00ff), new Color(0x00ffff)
  };
  protected List<Color> availableColors_ = new ArrayList<Color>(annoColors_.length);

  public Color getAnnoColor(Color c) {
	for (int i=0,imax=availableColors_.size(); i<imax; i++) {
		if (c.equals(availableColors_.get(i))) {
			availableColors_.remove(i);
			return c;
		}
	}
	return null;
  }

  public Color getAnnoColor() {
	Color c = availableColors_.get(0);
	availableColors_.remove(0);
	return c;
  }

  public void restoreAnnoColor(Color c) {
	availableColors_.add(c);
  }
  // when zap layer, restore its color
//*****************************************************


  /**
	On VMenu.MSG_CREATE_ANNO, make a menu entries for wiping annotations from page and, if multipage, from all pages.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_ANNO==msg) {
		Document doc = getBrowser().getCurDocument();
		Layer personal = doc.getLayer(Layer.PERSONAL);

		INode menu = (INode)se.getOut();
		createUI("button", "Wipe Annos from Page", "event "+MSG_WIPEPAGE, menu, CATEGORY_ZAP, personal.size()==0);
		String page = doc.getAttr(Document.ATTR_PAGE);
		if (page!=null) createUI("button", "Wipe Annos from All Pages", "event "+MSG_WIPEALL, menu, CATEGORY_ZAP, personal.size()==0 && personal.auxSize()==0);
	}
	return false;
  }


  /**
	Implements {@link #MSG_WIPEPAGE} and {@link #MSG_WIPEALL}.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_WIPEPAGE==msg || MSG_WIPEALL==msg) {
		Document doc = getBrowser().getCurDocument();
		String page = doc.getAttr(Document.ATTR_PAGE);
		Layer personal = doc.getLayer(Layer.PERSONAL);

		if (page==null) {
			personal.clear();	// clearBehaviors to retain subtrees?
		} else {
			if (MSG_WIPEALL==msg) personal.clear();
			else personal.clearBehaviors();
		}

		// cache file dealt with when move leave page (which could be quit, but not ABORT)
	}
	return super.semanticEventAfter(se,msg);
  }
}
