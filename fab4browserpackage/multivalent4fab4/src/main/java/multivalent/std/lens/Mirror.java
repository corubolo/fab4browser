package multivalent.std.lens;

import java.awt.*;
//import java.awt.geom.AffineTransform;

import multivalent.*;
import multivalent.gui.VCheckbox;
import multivalent.std.ui.DocumentPopup;


/**
	Like Cypher could be useful for reading coded messages, I guess, but mostly just a demonstration.

	@version $Revision: 1.2 $ $Date: 2002/01/15 00:28:39 $
*/
public class Mirror extends Lens {
  /**
	Toggle between mirror of portion within lens versus mirror of entire page, excerpted in lens.
	<p><tt>"lensMirrorSetType"</tt>.
  */
  public static final String MSG_SETTYPE = "lensMirrorSetType";

  boolean srcInWin = true;

  public boolean paintBefore(Context cx, Node node) {
	Graphics2D g = cx.g;
	if (srcInWin) {
		Rectangle r = getContentBounds();
		g.translate(r.x*2+r.width, 0);
//System.out.println("translate by "+(r.x+r.width));
	//g.translate(r.x+r.width/2, 0);	 // flip about center of window?
	// another valid way to do it
	} else {
		int w = getDocument().bbox.width;
		g.translate(w, 0);
	}

	g.scale(-1.0, 1.0);

	return false;
  }

  /** Spans should call super.semanticEventBefore to pick up morphing and deletion. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (this!=se.getIn()) {}
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg) {
		INode menu = (INode)se.getOut();	//attrs.get("MENU");
		Browser br = getBrowser();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Source from lens window", new SemanticEvent(br, MSG_SETTYPE, null, this, null), menu, "EDIT", false);
		cb.setState(srcInWin);
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (this!=se.getIn()) {}
	else if (MSG_SETTYPE==msg) {
		srcInWin = !srcInWin;
		// save to prefs
	}
	return super.semanticEventAfter(se, msg);
  }

/*
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);
	srcInWin = getPrefBoolean();
  }*/
}
