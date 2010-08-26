package multivalent.std.ui;

import java.net.URI;

import multivalent.*;
import multivalent.std.adaptor.ASCII;
import multivalent.gui.VMenu;

import phelps.net.RobustHyperlink;



/**
	NOT IMPEMENTED.
	View report on robust computations, with degree of confidence in alignments.
	(Different than {@link multivalent.std.RestoreReport}, which shows only those that failed.)

	(1) URI - signature,
	(2) internal - attached, moved, confidence

	@version $Revision$ $Date$
*/
public class RobustReport extends Behavior {
  /**
	Show robust report.
	<p><tt>"robustReport"</tt>.
  */
  public static final String MSG_REPORT = "robustReport";


  boolean active = false;
  boolean valid = false;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (super.semanticEventBefore(se, msg)) return true;    // respect superclass short-circuit

	// new code

	// create user interface widgets with unusual properties
	// (where possible, for ordinary widget use, specify it in the corresponding hub with attributes to a multivalent.std.ui.SemanticUI instance)
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		// general document state
		Browser br = getBrowser();
		Document doc = getDocument();

		// based on message, decode other fields from events
		INode menu = (INode)se.getOut();

		createUI("button", "Robust Report", "event "+MSG_REPORT, menu, null, false);

	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// since event survived the <i>before</i> phase, no behavior wants to prevent it

	// respond to semantic events implemented by this behavior
	if (MSG_REPORT==msg) {   // can use "==" here since passed String has been intern()'ed
		Browser br = getBrowser();
		String sig = RobustHyperlink.computeSignature(br.getRoot());
		System.out.println("page signature = "+sig);

		URI docuri = br.getCurDocument().getURI();
		System.out.println("protocol = |"+docuri.getScheme()+"|");
//	    if ("http".equals(docuri.getScheme()) /*--already marked by the time it gets here && !getControl().getCache().isSeen(docuri)*/) RobustHyperlink.checkSignature(docuri, sig);
	}

	return super.semanticEventAfter(se, msg);
  }

  public void buildAfter(Document doc) {
	valid = false;
	//if (active) compute();
  }
}
