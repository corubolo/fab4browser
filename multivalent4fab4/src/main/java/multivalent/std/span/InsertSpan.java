package multivalent.std.span;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;



/**
	Copy editor markup: insert text at point.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:16:27 $
*/
public class InsertSpan extends ActionSpan {
  public static final String ATTR_INSERT = "insert";


  String inserttxt_;


  public boolean appearance(Context cx, boolean all) { cx.spaceabove=10; return false; }

  public boolean paintBefore(Context cx, Node node) {
	Rectangle bbox = getStart().leaf.bbox;
	int baseline = cx.baseline;
	Graphics2D g = cx.g;
	g.setColor(getLayer().getAnnoColor());
//	  g.drawLine(bbox.x,baseline, bbox.x+4,baseline-5); g.drawLine(bbox.x+4,baseline-5, bbox.x+8,baseline);
	int x = (int)cx.x;
	g.drawLine(x-3,baseline, x,baseline-6); g.drawLine(x,baseline-6, x+3,baseline);
	getLayer().getAnnoFont().drawString(g, inserttxt_, x,10);
	return false;
  }


  public boolean action() {
	Leaf startn=getStart().leaf;
	int startoff=getStart().offset;
	startn.insert(startoff, inserttxt_, null);

	destroy();
	return true;
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
	if (this!=se.getIn()) {}
	else if (SystemEvents.MSG_FORM_DATA==msg) {	// takes data from non-window/non-interactive source too
		Map map = (Map)se.getArg();
		if (map!=null && map.get("cancel")==null) { //  ok")!=null) {	// button=OK vs cancel
			String val = (String)map.get(ATTR_INSERT);
			if (val!=null) inserttxt_=val;
			return true;
		}
	}

	return super.semanticEventAfter(se,msg);
  }



  public ESISNode save() {
	putAttr(ATTR_INSERT, inserttxt_);
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	inserttxt_ = getAttr(ATTR_INSERT, "(nothing)");
	putAttr(ATTR_INSERT, inserttxt_);
  }
}
