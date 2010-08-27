package multivalent.std.ui;

import multivalent.*;
import multivalent.gui.VButton;
//import multivalent.std.ui.DocumentPopup;



/**
	Stop button and popup item -- just sends stop event, but enable logic presently out of capabilities of scripting language.

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:54:09 $
*/
public class Stop extends Behavior {
  boolean enabled_=false;
  VButton toolbutt_ = null;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	Browser br = getBrowser();
	if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()!=br.getSelectionSpan()) {
		//createUI("button", "Stop", new SemanticEvent(br, Document.MSG_STOP, br.getCurDocument()), (INode)se.getOut(), "NAVIGATE", false);	  //!enabled_);

	} else if (Browser.MSG_CREATE_TOOLBAR==msg) {
		toolbutt_ = (VButton)createUI("button", "<img src='systemresource:/sys/images/Stop16.gif' width=16 height=16>", new SemanticEvent(br, Document.MSG_STOP, br.getCurDocument()), (INode)se.getOut(), null, false); //!enabled_);
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (Document.MSG_OPEN==msg && getDocument()==se.getIn()) {
		enabled_=true;
		if (toolbutt_!=null) toolbutt_.repaint(1000);

	} else if (Document.MSG_OPENED==msg && getDocument()==se.getIn()) {
		enabled_=false;
		if (toolbutt_!=null) toolbutt_.repaint(1000);
	}

	return super.semanticEventAfter(se,msg);
  }
}
