package multivalent.std.ui;

import java.util.Map;

import multivalent.*;



/**
	Create a instance of {@link multivalent.Span} describe by hub attributes and move to extent of current selection.
	See fields starting with {@link #ATTR_PARENT} for hub attributes available.

	@see multivalent.std.ui.SemanticUI
	@see multivalent.std.ui.WindowUI

	@version $Revision: 1.2 $ $Date: 2002/02/01 05:26:27 $
*/
public class SpanUI extends Behavior {
  /**
	Create a instance of {@link multivalent.Span} describe by hub attributes and move to extent of current selection.
	<p><tt>"createSpan"</tt>: <tt>arg=</tt> {@link SpanUI} <var>this</var>.
  */
  public static final String MSG_CREATE = "createSpan";

  /** Hub attribute that gives menu (<tt>createWidget/<var>parent</var></tt>). */
  public static final String ATTR_PARENT = "parent";
  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_TITLE = "title";
  /** Hub attribute that sets category group within menu. */
  public static final String ATTR_CATEGORY = "category";

  /** Hub attribute that gives new Span's logical name. */
  public static final String ATTR_LOGICAL = "logical";
  /** Hub attribute that gives new Span's Java class name (which must be a Span). */
  public static final String ATTR_SPANNAME = "spanname";
  /** Hub attribute that gives the new Span's additional attributes. */
  public static final String ATTR_ATTRS = "attrs";
  /** Hub attribute that, if present, immediately invokes editing by user to set other attributes. */
  public static final String ATTR_EDIT = "edit";

  /** Newly created attribute that records creation time, as given by {@link System#currentTimeMillis()}. */
  public static final String ATTR_CREATEDAT = "createdat";


  String spanname_ = null;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//if (getAttr("parent")!=null) System.out.println("*** semanticUI "+getAttr("parent")+" vs "+msg);
	if (super.semanticEventBefore(se,msg)) return true;
	if (spanname_==null || !msg.startsWith("createWidget/")) return false;
	String parent = getAttr(ATTR_PARENT); if (parent==null || !msg.endsWith(parent) || msg.length()!="createWidget/".length()+parent.length()) return false;
//System.out.println("*** semanticUI "+getAttr("parent"));

	SemanticEvent nse = new SemanticEvent(getBrowser(), MSG_CREATE, this);	// can't use "event createSpan "+spanname_ because may have several SpanUI instances all creating SignalUI
	createUI("button",getAttr(ATTR_TITLE), nse, (INode)se.getOut(), getAttr(ATTR_CATEGORY), !getBrowser().getSelectionSpan().isSet());

	return false;
  }

  /** On "createSpan", create span. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	if (MSG_CREATE==msg && this==arg) {	//arg instanceof String && spanname_!=null && spanname_.equals(arg)) {
		Browser br = getBrowser();
		Span sel = br.getSelectionSpan();
		if (sel.isSet()) {
			Map<String,Object> spanattrs = (Map)CHashMap.getInstance(getAttr(ATTR_ATTRS));	// pass on attributes to created span
			String logical = getAttr(ATTR_LOGICAL, spanname_);
			Span newspan = (Span)Behavior.getInstance(logical, spanname_, spanattrs, br.getCurDocument().getLayer(Layer.PERSONAL));
			newspan.move(sel);	// speed not important -- want reformatting more
			sel.moveq(null);	//remove();
			// LATER: stamp with author too => author should be in Layer
			newspan.putAttr(ATTR_CREATEDAT, Long.toString(System.currentTimeMillis()));	// don't create a Date first!
			//String nb=getAttr("nb"); if (nb!=null) newspan.makeNotemark(nb);
			// beep();
//if (getAttr("edit")!=null) System.out.println("*** immediate editSpan");
			if (getAttr(ATTR_EDIT)!=null) br.eventq(new SemanticEvent(br, Span.MSG_EDIT, null, newspan, null));

		} else br.eventq(Browser.MSG_STATUS, "Need to select range first");
	}
	return super.semanticEventAfter(se,msg);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	spanname_ = getAttr(ATTR_SPANNAME);
	if (spanname_==null) System.err.println("null on SPANNAME "+getAttr(ATTR_TITLE));
  }
}
