package multivalent.std;

import java.util.regex.*;
import java.util.Map;

import multivalent.*;



/**
	Wraps other behaviors in hub and enables them only when page URI matches passed pattern.

	@version $Revision: 1.1 $ $Date: 2002/02/01 03:43:42 $
*/
public class PerPage extends Layer {
  public static final String ATTR_URI = "uri";

  Matcher urim_ = null;

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	// change active flag BEFORE superclass
	if (Document.MSG_OPENED==msg && urim_!=null) {
		DocInfo di = (DocInfo)se.getArg();
		urim_.reset(di.uri.toString());
		setActive(urim_.find());
	}

	return super.semanticEventBefore(se,msg);
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	String arg;
	if ((arg = getAttr(ATTR_URI))!=null) {
		try {
			urim_ = Pattern.compile(arg).matcher("");
		} catch (PatternSyntaxException bad) { System.err.println("PerPage bad URI regexp: "+arg); }
	}

	setActive(false);
  }
}
