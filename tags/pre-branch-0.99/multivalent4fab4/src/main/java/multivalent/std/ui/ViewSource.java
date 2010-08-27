package multivalent.std.ui;

import multivalent.*;
import multivalent.gui.VMenu;



/**
	Opens new window and displays current document as ASCII.

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:49:28 $
*/
public class ViewSource extends Behavior {
  /**
	View source of current document.
	<p><tt>"viewSource"</tt>.
  */
  public static final String MSG_VIEW = "viewSource";

  /**
	Name of browser showing source.
  */
  public static final String BROWSER = "SOURCE";


  boolean active_=false;



  /** Add to View menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		INode menu = (INode)se.getOut();
		createUI("button", "Page Source", "event "+MSG_VIEW, menu, "View", false);  	// viewSource $URL
	}
	return false;
  }

  /** At viewSource semantic event, open new window and show ASCII. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();

	if (Document.MSG_OPEN==msg) {
		if (active_ && arg instanceof DocInfo) {	// br.getName()==SOURCEWINDOW -- may have multiple windows(?)
			DocInfo di = (DocInfo)arg;
			di.genre = "ASCII"; // override
			//active_=false;
		}
	} else if (MSG_VIEW==msg && "active".equals(arg)) {
		active_=true;

	} else if (MSG_VIEW==msg) {
		// check system preference to see whether should open new window or use same for all
		// just a frame with an ASCII node
		Browser br=getBrowser(), srcbr=getGlobal().getBrowser(BROWSER);
		srcbr.eventq(MSG_VIEW, "active");	// turn on in new browser

		DocInfo di = new DocInfo(br.getCurDocument().getURI());
		di.genre = "ASCII";     // until cache is a behavior
		srcbr.eventq(Document.MSG_OPEN, di);
	}
	return super.semanticEventAfter(se,msg);
  }
}
