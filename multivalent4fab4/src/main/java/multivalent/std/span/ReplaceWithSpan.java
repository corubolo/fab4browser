package multivalent.std.span;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;



/**
	A combination of InsertSpan and DeleteSpan.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:16:27 $
*/
public class ReplaceWithSpan extends ActionSpan {
  public static final String ATTR_INSERT = "insert";


  String inserttxt_ = null;
  LabelSpan label_=null;

  public void setInsertText(String inserttxt) {
	inserttxt_ = (inserttxt!=null? inserttxt: "");
	label_.setLabel("REPLACE WITH: "+inserttxt_);
	putAttr(ATTR_INSERT, inserttxt_);	// attributes automatically encoded
  }

  public void moveq(Leaf ln,int lo, Leaf rn,int ro) {
	super.moveq(ln,lo, rn,ro);
	label_.moveq(ln,lo, ln,lo+1);
  }

  //public void remove() { super.remove(); if (label_!=null) label_.remove(); label_=null; }
  public void destroy() {
	if (label_!=null) label_.destroy(); //label_=null;
	super.destroy();
  }


  public boolean appearance(Context cx, boolean all) { cx.overstrike = getLayer().getAnnoColor(); return false; }
  public boolean paintBefore(Context cx, Node start) {
	Rectangle bbox = start.bbox;
	Graphics2D g = cx.g;
	g.setColor(getLayer().getAnnoColor());
	int x = (int)cx.x;
	//g.drawLine(cx.x,bbox.y+10, cx.x,bbox.y+bbox.height); g.drawLine(cx.x+1,bbox.y+10, cx.x+1,bbox.y+bbox.height);
	g.drawLine(x,10, x,bbox.height); g.drawLine(x+1,10, x+1,bbox.height);
	return false;
  }


  public boolean action() {
	Leaf startn=(Leaf)getStart().leaf, endn=(Leaf)getEnd().leaf;
	int startoff=getStart().offset, endoff=getEnd().offset;

	destroy();

	endn.insert(endoff, inserttxt_, null);	// delete may zap node
	startn.delete(startoff, endn,endoff);
// NO, before!	  startn.insert(startoff, inserttxt_);

	return true;
  }


  /** Navigate to referring links in same document, in span's popup menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//if (getAttr("parent")!=null) System.out.println("*** semanticUI "+getAttr("parent")+" vs "+msg);
	// put into separate behavior?
	if (super.semanticEventBefore(se, msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && this==se.getIn()) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();

		if (isEditable()) {
			createUI("button", "Edit Message", new SemanticEvent(br, Span.MSG_EDIT, this, this, null), menu, "EDIT", false);
		}
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();

	if (this!=se.getIn()) {}
	else if (SystemEvents.MSG_FORM_DATA==msg) {	// takes data from non-window/non-interactive source too
		Map map = (Map)arg;
		if (map!=null && map.get("cancel")==null) {
			String val = (String)map.get(ATTR_INSERT);
			if (val!=null) setInsertText(val);
			return true;
		}
	}

	return super.semanticEventAfter(se,msg);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	if (label_==null) label_ = (LabelSpan)Behavior.getInstance(getName(), "LabelSpan", null,  getDocument().getLayer(Layer.SCRATCH));
	String txt = getAttr(ATTR_INSERT);
	setInsertText(txt!=null? txt: "(nothing)");
  }
}
