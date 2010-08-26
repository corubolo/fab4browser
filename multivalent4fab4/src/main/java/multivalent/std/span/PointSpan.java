package multivalent.std.span;

import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;

import phelps.lang.Floats;



/**
	Point size span editable by user.
	For non-editable spans, use a general span.

	@see multivalent.std.span.RelPointSpan

	@version $Revision: 1.4 $ $Date: 2002/02/02 13:16:27 $
*/
public class PointSpan extends Span {
  /**
	Change the point size dictated by this span.
	<p><tt>"changePoints"</tt>: <tt>arg=</tt> {@link java.lang.String} or {@link java.lang.Integer} <var>new-point-size</var>.
  */
  public static final String MSG_CHANGE = "changePoints";

  /**
	Interactive editing of point size.
	<p><tt>"editPoints"</tt>.
  */
  public static final String MSG_EDIT = "editPoints";


  public static final String ATTR_POINTS = "points";

  public static final String ATTR_POINT = "point";


  static String[] choices_ = null;   // shared among browser instances
  static String oldchoices_ = null;
  static float defaultPoints_ = 12;

  float points_ = defaultPoints_;


  // set attributes before moving
  public void setPoints(int points) { points_=points; }

  public boolean appearance(Context cx, boolean all) { cx.size=points_; return false; }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	if (this!=se.getIn()) {}
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && isEditable()) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();

		String curchoices = getAttr(ATTR_POINTS, "5 7 9 10 12 14 18 24 36 48 72");
		if (!curchoices.equals(oldchoices_)) {
			choices_ = curchoices.split("\\s+");
			oldchoices_ = curchoices;
		}

		//createUI("button", "Edit Points...", new SemanticEvent(br, MSG_EDIT, null, this, null), menu, "EDIT", false);
		for (int i=0, imax=choices_.length; i<imax; i++) {
			String pts = choices_[i];
			createUI("button", pts+" points", new SemanticEvent(br, MSG_CHANGE, pts, this, null), menu, "EDIT", false);
			// checkboxes and check current size
		}
	}

	//return super.semanticEventBefore(se,msg);  // local menu items go before superclass
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (this!=se.getIn()) {}

	else if (MSG_CHANGE==msg) {
		Object arg = se.getArg();
		int newpts = -1;

		if (arg==null) {
			// ask user
		} else if (arg instanceof Integer) newpts = ((Integer)arg).intValue();
		else if (arg instanceof String) try { newpts = Integer.parseInt((String)arg); } catch (NumberFormatException nfe) {}

		if (newpts > 0) {
			points_ = defaultPoints_ = newpts;
			markDirty();
			getBrowser().repaint();
		}

	} else if (MSG_EDIT==msg) {
		// dialog
		//"createDocPopup", this, ...
	}

	return super.semanticEventAfter(se,msg);
  }


  public ESISNode save() {
	putAttr(ATTR_POINT, String.valueOf(points_));
	return super.save();
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	points_ = Floats.parseFloat(getAttr(ATTR_POINT), 12.0f);
  }
}
