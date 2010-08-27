package multivalent.std.lens;

import java.util.Map;

import multivalent.*;

/**
	Scriptable lens for effects that just need a signal set.

	@see multivalent.std.span.SignalSpan

	@version $Revision: 1.2 $ $Date: 2003/06/02 05:52:10 $
*/
public class SignalLens extends Lens {
  public static final String ATTR_SIGNAL = "signal";
  public static final String ATTR_VALUE = "value";

  public String signal=null, value=null;

  public boolean appearance(Context cx, boolean all) {
//System.out.println("appearance "+signal+"="+value);
	if (signal!=null) {
		if (value!=null) cx.signal.put(signal, value); else cx.signal.remove(signal);
	}
	return false;
  }

  public ESISNode save() {
	putAttr(ATTR_SIGNAL, signal);	// could have changed during lifetime
	putAttr(ATTR_VALUE, value);    // ... maybe not String
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);	// bounds
	signal=getAttr(ATTR_SIGNAL); value=getAttr(ATTR_VALUE);
	String title = getAttr("title", signal+"="+value);  // maybe take default from WindowUI title
	//if (getAttr("title")==null) putAttr("title", signal+"="+value);   // "Signal" not a good title
	win_.setTitle(title);
//System.out.println("RESTORE "+signal+"="+value);
	//if (attr_==null) return true;
  }
}
