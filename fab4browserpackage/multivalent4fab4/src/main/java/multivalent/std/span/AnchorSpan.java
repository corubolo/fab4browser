package multivalent.std.span;

import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.net.URL;

import multivalent.*;
//import multivalent.std.span.HyperlinkSpan;
//import multivalent.gui.VDialog;
import multivalent.std.ui.DocumentPopup;



/**
	Intra-document destination of a hyperlink: a named, robustly located point in document.

	@version $Revision: 1.4 $ $Date: 2002/05/11 08:29:21 $
*/
public class AnchorSpan extends Span {
  //public static final String Node.ATTR_ANCHOR = "name"; => "id"

  public void setAnchorName(String name) {
	putAttr(Node.ATTR_ID, name);
  }

  // in some mode could maybe draw an anchor icon in paintBefore
//	public boolean appearance(Context cx, boolean all) { cx.foreground=Color.BLUE; return false; }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	if (getAttr(Node.ATTR_ID)==null) putAttr(Node.ATTR_ID, "(name)");
  }


  /** Navigate to referring links in same document, in span's popup menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//if (getAttr("parent")!=null) System.out.println("*** semanticUI "+getAttr("parent")+" vs "+msg);
	// put into separate behavior?
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && this==se.getIn()) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		Document doc = br.getCurDocument();

		if (isEditable()) {
			createUI("button", "Edit Anchor Name (\""+getAttr(Node.ATTR_ID)+"\")", new SemanticEvent(br, Span.MSG_EDIT, this, this, null), menu, "EDIT", false);
		}

/*
		List<> links = null;
		String name = getAttr(Node.ATTR_ID);
//System.out.println("looking for reference to "+name+" in "+doc.getVar(Document.VAR_LINKS));
		if (doc!=null && (links=(List)doc.getVar(Document.VAR_LINKS))!=null) { // => different now.  have to scan document tree
			for (Iterator<> i=links.iterator(); i.hasNext(); ) {
				HyperlinkSpan link = (HyperlinkSpan)i.next();
				Object o = link.getTarget();
//System.out.println("target = "+o);
				if (link.isSet() && o instanceof URL && name.equals(((URL)o).getRef())) {
					// can invest lotsa work here as only a couple matches
					StringBuffer sb = new StringBuffer(100);
					Node n, s=link.getStart().leaf,e=link.getEnd().leaf;

					// collect up some context too
					INode p=s.getParentNode();
					Node xs=s, xe=e;
					for (int j=s.childNum()-1,jmin=Math.max(j-5,0); j>=jmin; j--) if ((n=p.childAt(j)).isLeaf()) xs=n;
					for (int j=e.childNum()+1,jmax=Math.min(j+5+1,p.size()); j<jmax; j++) if ((n=p.childAt(j)).isLeaf()) xe=n;
					int xsi=0, xei=0;
					for (n=xs; n!=xe; n=n.getNextLeaf()) {
						if (n==s) xsi=sb.length()+1;
						if (n.getName()!=null) sb.append(' ').append(n.getName());
						if (n==e) xei=sb.length();
					}
					if (xei==-1) xei=sb.length();
//System.out.println("0.."+xsi+" main "+xei+".."+sb.length()+": "+sb.substring(0));
//if (xsi>0) System.out.println("prev "+sb.substring(0,xsi));
//if (xei<sb.length()) System.out.println("follow "+sb.substring(xei));

					// compute title with some context, as long as not too long
					String main, prev="", follow="";
					int mainlen = xei-xsi;
					if (mainlen>40) {	// long body, so no context
						main = sb.substring(xsi,xsi+Math.min(mainlen,50));
					} else {	// context
						main=sb.substring(xsi,xei);
						//if (xsi>0) prev = "<i><small>"+sb.substring(xsi-Math.min(xsi,35),xsi)+"</i></small>";
						if (xsi>0) prev = "<i><small>"+sb.substring(0,xsi)+"</i></small>";
						if (xei<sb.length()) follow = "<i><small>"+sb.substring(xei,Math.min(xei+15,sb.length()))+"</i></small>";
					}
					String title = "<nobr><b>Reference</b>: " + prev + main + follow + "</nobr>";
//System.out.println(title);

					n = createUI("button", title, new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, link/*.getStart().leaf* /), menu, "NAVIGATE", false);
				}
			}
		}
*/
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();
	if (this!=se.getIn()) {}
	else if (SystemEvents.MSG_FORM_DATA==msg) {	// takes data from non-window/non-interactive source too
		Map map = (Map)arg;
		if (map!=null && map.get("cancel")==null) {
			String name = (String)map.get(Node.ATTR_ID);
			if (name!=null) setAnchorName(name);
			return true;
		}
	}
	return super.semanticEventAfter(se,msg);
  }


  public boolean eventAfter(AWTEvent e, Point rel, Node n) {
	Browser br = getBrowser();
	int eid=e.getID();
	if (eid==MouseEvent.MOUSE_ENTERED) {
		br.eventq(Browser.MSG_STATUS, "Anchor named \""+getAttr(Node.ATTR_ID)+"\"");

	} else if (eid==MouseEvent.MOUSE_EXITED) {
		br.eventq(Browser.MSG_STATUS, "");
	}

	return super.eventAfter(e, rel, n);
  }

  // give priority to hyperlink for br.eventq(Browser.MSG_STATUS, ...)
  public int getPriority() { return ContextListener.PRIORITY_SPAN-ContextListener.LOT; }
}
