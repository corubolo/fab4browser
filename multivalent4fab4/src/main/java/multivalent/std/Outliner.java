package multivalent.std;

import multivalent.*;
import multivalent.std.span.OutlineSpan;
import multivalent.gui.VScrollbar;



/**
	Take basic Manual Page tree, add outliner spans
	(which in turn add controls open/close arrow, click on title to toggle).

	<p>LATER generalize as outliner functionality:
		take list of important structural (if null, take immediate children of docroot),
		cover content (arrow iff replaces existing or something),
		generalize to nested outlines,
		if full page fits on screen, no outlining, else keep opening sections until fill up screen

	@see multivalent.std.adaptor.ManualPageVolume
	@see multivalent.std.adaptor.ManualPage

	@version $Revision: 1.3 $ $Date: 2002/01/27 02:52:25 $
*/
//public class ManualPageOutline extends Behavior implements EventListener {
public class Outliner extends Behavior {
  /**
	Announce that construted outline.
	<p><tt>"madeOutline"</tt>: <tt>arg=</tt> {@link multivalent.Node} <var>root-of-outline</var>.
  */
  public static final String MSG_MADE = "madeOutline";


  /**
	Tart up with open/close arrow and span on section bodies.
	Speeds up page display because don't have to format collapsed sections.
	Structure: ManualPage -> Section+ -> SectHead Para*
  */
  public void buildAfter(Document doc) {
	if (doc.size()==0 || doc.childAt(0).isLeaf()) return;
	INode mp = (INode)doc.childAt(0); if (mp.size()<=1/*2? 3?*/ || /*man page specific*/!"section".equals(mp.childAt(1).getName())) return;
	Browser br = getBrowser();
	Layer scratchLayer = doc.getLayer(Layer.SCRATCH);

	// collapse very short sections
	//INode sect = (INode)mp.childAt(0);
	//INode head = (INode)sect.childAt(0);
	for (int i=0,imax=mp.size(); i<imax; i++) {
		INode sect = (INode)mp.childAt(i);
		if (sect.childAt(0).isLeaf()) continue;
		INode head = (INode)sect.childAt(0);
		INode body = (INode)sect.childAt(1);

		// generalize... or move into page parser
		// just head + single body line, and body is short
		boolean isName = "Name".equals(head.getFirstLeaf().getName()); // && i==0?
		if (sect.size()==2 && (isName || body.size()<15)) {
			if (isName) head.removeAllChildren();	// "Name" is worthless => simply use actual name

			// move content onto header line (use span to maintain text size)
			//int headlen = head.size();
			Leaf body0=body.getFirstLeaf(), bodyn=body.getLastLeaf();
			for (int j=0,jmax=body.size(); j<jmax; j++) head.appendChild(body.childAt(0));

			Span span = (Span)Behavior.getInstance("concat", "multivalent.Span", null, scratchLayer);
			span.moveq(body0,0, bodyn,bodyn.size());
			body.remove();

			if (isName) {
				for (int j=0,jmax=head.size(); j<jmax; j++) {
					if ("-".equals(head.childAt(j).getName())) {
						span.moveq((Leaf)head.childAt(j),0, span.getEnd().leaf,span.getEnd().offset);
					}
				}
			}
		}
	}
//else System.out.println("*** sect.size()=="+sect.size()+", name=|"+head.getName()+"|");


	// if short-ish, see if whole man page fits on screen.	if so, leave it alone
	int paracnt=0; for (int i=0,imax=mp.size(); i<imax; i++) paracnt += mp.childAt(i).size();   // mp.childAt(i) is a sect, size() is paragraphs
//System.out.println("para count = "+paracnt);
	if (paracnt < 50) { //active_=false; return; }/*{
		//br.format(); => invalid IVNodes inbetween (markDirty stops as ISP)
		doc.formatBeforeAfter(doc.bbox.width, doc.bbox.height, null);
		VScrollbar vsb = doc.getVsb();
		int virtheight = vsb.getMax()-vsb.getMin();
//System.out.println("doc="+doc.getName()+"/"+doc.getFirstLeaf().getName()+", paracnt="+paracnt+", virt height = "+virtheight+" vs docheight="+doc.bbox.height);
		if (virtheight < doc.bbox.height + 50/*fuzz*/) return;
	}


	br.eventq(MSG_MADE, mp);


	// collapsible content
	StringBuffer sb = new StringBuffer(200);

	for (int i=0,imax=mp.size(); i<imax; i++) {
		INode sect = (INode)mp.childAt(i); if (sect.size()==1) continue;	// just the head
		if (sect.childAt(0).isLeaf()) continue;
		INode head = (INode)sect.childAt(0);
		sb.setLength(0); for (int j=0,jmax=head.size(); j<jmax; j++) {
			String name = head.childAt(j).getName();
			if (name!=null) sb.append(' ').append(name);
		}
		String headname = (sb.length()>1? sb.substring(1): "");  // skip initial space character
//System.out.println("headname=|"+headname+"|");

		OutlineSpan ospan = (OutlineSpan)Behavior.getInstance("section"+i,"OutlineSpan",null, scratchLayer);
		Leaf s=sect.getFirstLeaf(), e=sect.getLastLeaf();
		ospan.moveq(s,0, e,e.size());


		// special cases

		// initially open: Name, Synopsis, See Also/Related Information, Author/Authors
		boolean fopen = "Name".equals(headname) || "Synopsis".equals(headname) || "See Also".equals(headname) || "Related Information".equals(headname) || headname.startsWith("Author");
		boolean onlybold = sect.size()>15;
		if (fopen && !onlybold) {
			//ospan.setClosed(false); => excerpt whole section in order to survive exec sum
System.out.println(headname+" => open");
			Span span = (Span)Behavior.getInstance("excerpt","multivalent.Span",null, scratchLayer);
			Leaf l0=head.getNextLeaf(), ln=sect.getLastLeaf();
			span.moveq(l0,0, ln,ln.size());

		// excerpted: command line options, commands
		} else {  //if (headname.indexOf("Command")!=-1 || headname.indexOf("Option")!=-1) {
			boolean anything = (fopen || headname.indexOf("Command")!=-1) || (headname.indexOf("Option")!=-1);	// unless "Command", must have nonalpha initial character
			/*boolean onlybold = false;
			if (!anything && para.size()>20) for (int j=0+1/*skip head* /,jmax=sect.size(); j<jmax; j++) {
				Node para = sect.childAt(j); String name=para.getName();
				if (/*"tp".equals(name) ||* / "para".equals(name)) {
					if (para.getFirstLeaf().sizeSticky()>0) { onlybold=true; break; }
				}
			}*/

			for (int j=0+1/*skip head*/,jmax=sect.size(); j<jmax; j++) {   // 0+1 to skip head
				Node para = sect.childAt(j); String name=para.getName();
				if ("para".equals(name) || "tp".equals(name)) {   // also "indent"
					Leaf n=para.getFirstLeaf(); String nname=n.getName();
					if (nname!=null && (anything || nname.charAt(0)=='-') && (!onlybold || n.sizeSticky()>0)) {	//!Character.isLetter(nname.charAt(0)))) {
						INode p=n.getParentNode(); Leaf endn=p.childAt(Math.min(20,p.size()-1)).getLastLeaf();    // excerpt at most 20 words
						/*int k=1;
						for (int kmax=Math.min(20,p.size()-1); k<kmax; k++) if (p.childAt(k).isStruct()) break;
						Node endn=p.childAt(k-1);*/
						Span span = (Span)Behavior.getInstance("excerpt","multivalent.Span",null, scratchLayer);
						span.moveq(n,0, endn,endn.size());
						//excerptSpans_.add(span);    // have to mark dirty when style sheet changes
					}
				}
			}
		}
	}
  }
}
