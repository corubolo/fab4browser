package edu.berkeley;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.INode;
import multivalent.SemanticEvent;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import phelps.lang.Booleans;
import phelps.net.RobustHyperlink;



/**
	Compute and show (stdout for now, note later) signature for current page.

	@version $Revision: 1.2 $ $Date: 2003/02/09 01:04:46 $
 */
public class RobustUI extends Behavior {
	static boolean verbose_ = true;

	/**
	Compute robust signature of current document and dump to stdout.
	<p><tt>"robustSignatureDump"</tt>: <tt>arg=</tt> {@link multivalent.Document} <var>document</var>.
	 */
	public static final String MSG_DUMP = "robustSignatureDump";

	public static final String MSG_SET_ACTIVE = "robustSignatureSetActive";


	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se,msg)) return true;
		else if (VMenu.MSG_CREATE_VIEW==msg) {
			INode menu = (INode)se.getOut();
			createUI("button", "Dump Robust Signature", "event "+RobustUI.MSG_DUMP, menu, "View", false);
			VCheckbox cb = (VCheckbox)createUI("checkbox", "Verbose Robust Information", "event "+RobustUI.MSG_SET_ACTIVE, menu, "View", false);
			cb.setState(RobustUI.verbose_);
		}
		return false;
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (RobustUI.MSG_SET_ACTIVE==msg)
			RobustUI.verbose_ = Booleans.parseBoolean(se.getArg(), !RobustUI.verbose_);
		else if (RobustUI.MSG_DUMP==msg) {
			Document doc = getBrowser().getCurDocument();
			RobustHyperlink.Verbose=RobustUI.verbose_;
			String words = RobustHyperlink.computeSignature(doc);	  //getBrowser().getDocRoot());

			System.out.println("Signature for "+doc.getURI()+"\n	"+words);
		}
		return super.semanticEventAfter(se,msg);
	}
}
