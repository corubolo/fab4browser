package multivalent.std.ui;

import com.pt.io.Cache;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;



/**
	Cache control: validate cache vis-a-vis network, or browse offline.

	@version $Revision$ $Date$
*/
public class CacheControl extends Behavior {
  /**
	Sets cache policy to argument.
	<p><tt>"cachePolicySet"</tt>: <tt>arg=</tt> {@link com.pt.io.Cache}'s policy constant.
  */
  public static final String MSG_POLICY_SET = "cachePolicySet";


  private Object prev_ = null;


  /** On {@link VMenu#MSG_CREATE_FILE}, add <code>Offline</code> menu item. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_FILE==msg) {
		INode menu = (INode)se.getOut();
		Object now = getGlobal().getCache().getPolicy();
		SemanticEvent ev = new SemanticEvent(getBrowser(), MSG_POLICY_SET, Cache.POLICY_OFFLINE!=now? Cache.POLICY_OFFLINE: prev_);
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Browse Offline", ev, menu, null/*StandardFile.MENU_CATEGORY_OPEN*/, false);
		cb.setState(Cache.POLICY_OFFLINE == now);
	}
	return false;
  }

  /** Implements {@link #MSG_POLICY_SET}. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_POLICY_SET == msg) {
		Cache cache = getGlobal().getCache();
		Object cur = cache.getPolicy();
		try { cache.setPolicy(se.getArg()); prev_ = cur; } catch (Exception e) {}
	}
	return super.semanticEventAfter(se,msg);
  }
}
