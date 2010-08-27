package multivalent.std.span;

import java.util.Map;

import multivalent.*;



/**
	Convenience span for setting some signal, as given by SIGNAL and VALUE attributes.

	@see multivalent.std.ui.SpanUI

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class SignalSpan extends Span {
  public static final String ATTR_SIGNAL = "signal";
  public static final String ATTR_VALUE = "value";

  public String signal=null, value=null;

  public boolean appearance(Context cx, boolean all) {
//System.out.println("setting "+signal+" to "+value);
	if (signal!=null) {
		if (value!=null) cx.signal.put(signal, value); else cx.signal.remove(signal);
//System.out.println("SignalSpan/appearance  "+signal+"="+value);
	}
	return false;
  }

  public ESISNode save() {
	putAttr(ATTR_SIGNAL, signal);
	putAttr(ATTR_VALUE, value);
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	signal=getAttr(ATTR_SIGNAL); value=getAttr(ATTR_VALUE);
//System.out.println("SignalSpan/restore  "+signal+"="+value);
  }
}
