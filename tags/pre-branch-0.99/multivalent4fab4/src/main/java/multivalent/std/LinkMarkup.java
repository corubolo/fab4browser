package multivalent.std;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;

import multivalent.*;
import multivalent.gui.VCheckbox;
import multivalent.std.span.HyperlinkSpan;
import multivalent.gui.VMenu;

import phelps.lang.Booleans;



/**
	For plain Jane document formats without hyperlinks,
	scan text looking for patterns to make into links,
	such as "protocol://host.domain", "www.host.domain", "ftp.host.domain", "user@host.domain".
	Manual pages not referred to much outside of other man pages, so that scanning left with that media adaptor.
	Better to be conservative and fast than exact, as also have alt-button-on-word for dynamic determination of a link type.
	Not necessary as can always treat any text as some link with the LinkMe behavior.

	<p>LATER:
	use regexp,
	load patterns from hub (then easy to inclue man pages or not),
	look for email and USENET quoted text.

	@see multivalent.std.ui.LinkMe

	@version $Revision: 1.4 $ $Date: 2002/05/13 05:09:42 $
*/
public class LinkMarkup extends Behavior {
  /**
	Scan for implicit links.
	<p><tt>"linkScan"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
  */
  public static final String MSG_LINKSCAN = "linkScan";

  /**
	Sets active state on/off.
	<p><tt>"linkScanSetActive"</tt>: <tt>arg=</tt> {@link java.lang.String} or {@link java.lang.Boolean} or <code>null</code> to toggle.
  */
  public static final String MSG_SET_ACTIVE = "linkScanSetActive";

  /** Boolean. */
  public static final String PREF_AUTO = "LinkMarkup/Scan";

  public static final Pattern PATTERN = Pattern.compile("(http:|ftp:|file|www\\.|ftp\\.|://)[a-z0-9/~_:#?&\\.-]*[a-z0-9/]", Pattern.CASE_INSENSITIVE);

  static final String URLVALID = "/~-._:#?&";   // but don't want punctuation (.;?) at end of match


  /** Keep track of links so can delete them if turned off. */
  List<HyperlinkSpan> links = new ArrayList<HyperlinkSpan>(20);



  public boolean isActive() {
	return Booleans.parseBoolean(getPreference(PREF_AUTO, "true"), true);
  }

  public void scan() {
	links.clear();

	// scan
	Document doc = getDocument();
	Layer scratch = doc.getLayer(Layer.SCRATCH);

	int inx, si, ei;
	if (doc.getFirstLeaf()!=null) for (Leaf n=doc.getFirstLeaf(), endn=doc.getLastLeaf().getNextLeaf(); n!=endn && n!=null; n=n.getNextLeaf()) {
		// if (text node) ...
		String name=n.getName(); int len;
		if (name==null || (len=ei=name.length())<5) continue;
		String target=null; si=0;
		if (len<5) {}
		else if ((inx=name.indexOf("://"))>0 && len>=inx+5 && name.indexOf('.',inx+3)!=-1) { si=inx-1; ei=inx+3; target=""; }	  // ftp:x
		else if ((inx=name.indexOf("file:/"))>=0 && len>=inx+7) { si=inx; ei=inx+6; target=""; }
		else if ((inx=name.indexOf("www."))>=0 && len>=inx+9) { si=inx; ei=inx+4; target="http://"; }     // www.x.com
		else if ((inx=name.indexOf("ftp."))>=0 && len>=inx+9) { si=inx; ei=inx+4; target="ftp://"; }  // ftp.x.com
		else if ((inx=name.indexOf("@"))>0 && len>=inx+7 && name.indexOf('.',inx+1)!=-1) { si=inx-1; ei=inx+1; target="mailto:"; }   // x@y.com
		if (target!=null) {
			// scan forward and back
			char ch;
			while (si>0 && (Character.isLetterOrDigit(ch=name.charAt(si-1)) || URLVALID.indexOf(ch)!=-1)) si--;
			while (ei<len && (Character.isLetterOrDigit(ch=name.charAt(ei)) || URLVALID.indexOf(ch)!=-1)) ei++;
			target += name.substring(si,ei);
//System.out.println("linked "+target);

			// no hyperlink already
			boolean fexisting=false;
			for (int i=0,imax=n.sizeSticky(); i<imax; i++) {
				Mark m = n.getSticky(i);
				if (m.getOwner() instanceof HyperlinkSpan) { fexisting=true; break; }
			}

			if (!fexisting) {
				HyperlinkSpan link = (HyperlinkSpan)Behavior.getInstance("computed","HyperlinkSpan", null, scratch);
				link.setTarget(target);
				link.moveq(n,si, n,ei);
				links.add(link);
			}
		}
	}

	if (links.size()>0) doc.repaint(100);
  }

  public void setActive(boolean active) {
	if (active != isActive()) putPreference(PREF_AUTO, active? "true": "false");
	if (active) scan();
	else {
		// turn off: remove links
		for (int i=0,imax=links.size(); i<imax; i++) ((Span)links.get(i)).destroy();
		links.clear();
	}
  }

  /** Add entry to View menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Auto Scan for Links", "event "+MSG_SET_ACTIVE, (INode)se.getOut(), "AuxSelect", false);
		cb.setState(isActive());
	}
	return false;
  }

  /** Semantic events: toggleLinkScan, closeDocument cleans up link list. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_LINKSCAN==msg && isActive()) scan();
	else if (MSG_SET_ACTIVE==msg) setActive(Booleans.parseBoolean(se.getArg(), !isActive()));
	else if (Document.MSG_CLOSE==msg && getDocument()==se.getArg()) links.clear();
	return super.semanticEventAfter(se,msg);
  }

  public void buildAfter(Document doc) {
	super.buildAfter(doc);
	getBrowser().eventq(MSG_LINKSCAN, doc);
  }
}
