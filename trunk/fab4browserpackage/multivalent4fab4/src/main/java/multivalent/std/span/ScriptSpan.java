package multivalent.std.span;

import java.awt.Color;
import java.util.Map;

import multivalent.*;
//import multivalent.std.span.ActionSpan;
import multivalent.std.VScript;



/**
	When click on span, execute script in {@link multivalent.std.VScript}.
	Name of node clicked upon put into attributes, which script can access with <code>$node</code>.
	(Appearance can be set in subclass or via style sheet.)

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:27 $
*/
public class ScriptSpan extends ActionSpan {
  public static final String ATTR_SCRIPT = "script";

  // later style sheet, for now look like hyperlink cliche
  public boolean appearance(Context cx, boolean all) {
	//boolean seen = getControl().seen(
	// don't use underline in appearance because used over large areas and massive underlining is hard to read
	//cx.foreground = cx.underline = (inmediasres_? Color.RED: Color.BLUE);
	cx.foreground = (inmediasres_? Color.RED: Color.BLUE);
	return false;
  }

  protected boolean action() {
	String script = getAttr(ATTR_SCRIPT);
	Browser br = getBrowser();
	if (script!=null) {
		Map<String,Object> attrs = new CHashMap<Object>(5);	// any attrs from span can be written directly into script
		//attrs.put("event", event_);   -- to collect modifiers (shift, control, ...) to maybe do something different
		Node curn = br.getCurNode();
		if (curn!=null) attrs.put("node", curn.getName());  // put in "in" field?
//System.out.println(script+", with node="+curn.getName());
		VScript.eval(script, getDocument(), attrs, curn);
	}
	return false;
  }
}
