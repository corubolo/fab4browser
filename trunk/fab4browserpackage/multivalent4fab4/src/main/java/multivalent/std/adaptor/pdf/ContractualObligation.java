package multivalent.std.adaptor.pdf;

import multivalent.*;
import multivalent.std.Print;



/**
	Enforce permissions (don't print, don't copy and paste, ...),
	which is hard and unnatural in Multivalent,
	but required by Adobe.
	See PDF Reference, page 77.

<!--
Enforced:
PERM_PRINT, PERM_PRINT_GOOD,
PERM_COPY, PERM_COPY_R3

Not Available:
PERM_FILL

Not Applicable:
PERM_MODIFY
PERM_ASSEMBLE

Not Enforced:
PERM_ANNO
-->

	@version $Revision: 1.3 $ $Date: 2003/06/01 07:01:01 $
*/
public class ContractualObligation extends Behavior {
  static final boolean DEBUG = false;

  boolean frestrict_ = false;
  int R_ = 3;
  int perms_ = SecurityHandlerStandard.PERM_ALL;    // symbol importing in Java 1.5


  /** If encrypted and permissions set, observe root in order to enforce. */
  public void buildAfter(Document doc) {
	//doc.removeObserver(this);  // automatically removed when document replaced

//System.out.println("co buildAfter "+doc+", "+doc.getMediaAdaptor()+", pdfr="+((PDF)doc.getMediaAdaptor()).getReader());
	MediaAdaptor me = doc.getMediaAdaptor(); if (!(me instanceof PDF)) return;
	PDFReader pdfr = ((PDF)me).getReader(); if (pdfr==null) return; // error reading file
	Encrypt e = pdfr.getEncrypt();
//System.out.println("sh = "+e.getStmF()+", "+e.getStmF().getClass()+", instanceof? "+(e.getStmF() instanceof SecurityHandlerStandard));
	SecurityHandler sh = e.getSecurityHandler(); if (!(sh instanceof SecurityHandlerStandard)) return;
	SecurityHandlerStandard shs = (SecurityHandlerStandard)sh;
	int V = e.getV();
	R_ = shs.getR();
	perms_ = shs.getPerm();

	// if (well defined restriction interpretation && exist restrictions) then enforce them
	frestrict_ = (V==1 || V==2) && (R_==2 || R_==3) && (perms_&SecurityHandlerStandard.PERM_ALL)!=SecurityHandlerStandard.PERM_ALL;
//System.out.println("restrict? "+R_+", "+Integer.toBinaryString(perms_));
//if ((perms_ & SecurityHandlerStandard.PERM_ALL) == SecurityHandlerStandard.PERM_ALL) System.out.println("encrypted but no restrictions");

	if (frestrict_) doc.addObserver(this);
  }


  /**
	Enforce printing restriction.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (!frestrict_) {}
	else if (Print.MSG_PRINT==msg && ((R_==2 && (perms_&SecurityHandlerStandard.PERM_PRINT)==0) || (R_==3 && (perms_&SecurityHandlerStandard.PERM_PRINT_GOOD)==0))) {
//System.out.println("restrict: "+R_+", "+Integer.toBinaryString(perms_));
		return true;    // short-circuit
	}
/*
multivalent.std.ui.StandardEdit
  public static final String MSG_CUT = "editCut";
  public static final String MSG_COPY = "editCopy";
*/
	return false;
  }


  /**
	Enforce copying/extraction restriction.
  */
  public boolean clipboardAfter(StringBuffer sb, Node node) {
//System.out.println("clip, R="+R_+", perms_="+Integer.toBinaryString(perms_));
	if (super.clipboardBefore(sb, node)) return true;
	else if (!frestrict_) {}
	else if ((R_==2 && (perms_&SecurityHandlerStandard.PERM_COPY)==0) || (R_==3 && (perms_&SecurityHandlerStandard.PERM_COPY_R3)==0)) {
		sb.setLength(0);
		sb.append("Text extaction disallowed by document's PDF permissions.");
	}
	return false;
  }
}
