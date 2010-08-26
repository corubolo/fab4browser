package multivalent.std.span;

import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;


/**
	Underline a span and show a message at the start.

	To do
	these should just be front ends to functionality available elsewhere

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:16:26 $
*/
public class AwkSpan extends Span {
  public static final String ATTR_COMMENT = "comment";


  String awk_;
  LabelSpan label_=null;


  public void setLabel(String label) { awk_=label; label_.setLabel(awk_); }

  public boolean appearance(Context cx, boolean all) { cx.underline = getLayer().getAnnoColor(); return false; }

  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	super.moveq(ln,lo, rn,ro); label_.moveq(ln,lo, ln,lo+1);
//    System.out.println("label_.moveq("+ln.getName()+","+lo+", "+ln.getName()+","+(lo+1)+")");
  }

  public void destroy() {
   if (label_!=null) label_.destroy(); //label_=null;
   super.destroy();
 }


  /** Navigate to referring links in same document, in span's popup menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//if (getAttr("parent")!=null) System.out.println("*** semanticUI "+getAttr("parent")+" vs "+msg);
	// put into separate behavior?
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && this==se.getIn()) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();

		if (isEditable()) {
			createUI("button", "Edit Message", new SemanticEvent(br, Span.MSG_EDIT, this, this, null), menu, "EDIT", false);
		}
	}
	//return super.semanticEventBefore(se, msg);
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (this!=se.getIn()) {}    // nothing
	else if (SystemEvents.MSG_FORM_DATA==msg) {	// takes data from non-window/non-interactive source too
		Map map = (Map)se.getArg();
		if (map!=null && map.get("cancel")==null) {
			String val = (String)map.get(ATTR_COMMENT);
			if (val!=null) setLabel(val);
			return true;
		}
	}

	return super.semanticEventAfter(se,msg);
  }



  public ESISNode save() {
	putAttr(ATTR_COMMENT, awk_);
	// actually don't need to save end location
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	if (label_==null) label_=(LabelSpan)Behavior.getInstance(getName(), "LabelSpan", null, getDocument().getLayer(Layer.SCRATCH));

	setLabel(getAttr(ATTR_COMMENT, "(no comment)"));
	putAttr(ATTR_COMMENT, awk_);	// user wants to edit decoded version
	//if (label_==null) label_=(LabelSpan)Behavior.getInstance("LabelSpan", null, getBrowser().getLayer(Layer.SCRATCH));
  }
}
