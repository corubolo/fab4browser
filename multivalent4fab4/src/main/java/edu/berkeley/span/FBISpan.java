package edu.berkeley.span;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;



/**
	Black out region and choose reason and comment - one hour hack based on Hyperlinks.
	Obviously not secure: doesn't fuse image with black out.

	@version $Revision: 1.3 $ $Date: 2002/02/13 16:29:34 $
*/
public class FBISpan extends Span {

  public static final String ATTR_COMMENT = "comment";
  public static final String ATTR_REASON = "reason";


  private String comment_;
  private String reason_;


  public void setReason(String reason) { reason_=(reason!=null?reason:""); putAttr(ATTR_REASON, reason_); }
  public void setComment(String comment) { comment_=(comment!=null?comment:null); putAttr(ATTR_COMMENT, comment_); }

  public boolean appearance(Context cx, boolean all) {
	cx.foreground = cx.background = Color.BLACK;
	return false;
  }
  /** Not secure, but at least can't see if simply select. */
  public int getPriority() { return ContextListener.PRIORITY_MAX; }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;

	if (this!=se.getIn()) {}
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		if (isEditable()) {
			createUI("button", "Edit Redaction Metadata", new SemanticEvent(br, Span.MSG_EDIT, this, this, null), menu, "EDIT", false);
		}
	}
	return false;
  }


  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	Browser br = getBrowser();
	int eid=e.getID();

	if (eid==MouseEvent.MOUSE_ENTERED) {
		//br.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		br.eventq(Browser.MSG_STATUS, "Removed for reason "+reason_+(comment_!=null? " ("+comment_.substring(0,Math.min(40,comment_.length()))+")": ""));

	} else if (eid==MouseEvent.MOUSE_EXITED) {
		//br.setCursor(Cursor.getDefaultCursor());
		br.eventq(Browser.MSG_STATUS, "");
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();

	if (this!=se.getIn()) {}
	else if (SystemEvents.MSG_FORM_DATA==msg) {
		Map map = (Map)arg;
		if (map!=null && map.get("cancel")==null) {
			setComment((String)map.get(ATTR_COMMENT));
			setReason((String)map.get(ATTR_REASON));
		}
		return true;
	}

	return super.semanticEventAfter(se,msg);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	reason_ = getAttr(ATTR_REASON, "(no reason)");
	comment_ = getAttr(ATTR_COMMENT, "");
  }
}
