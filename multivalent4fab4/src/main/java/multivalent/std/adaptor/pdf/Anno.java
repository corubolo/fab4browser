package multivalent.std.adaptor.pdf;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import multivalent.Behavior;
import multivalent.Leaf;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.std.ui.Multipage;



/**
	Superclass for PDF annotations.
	Provides methods for mapping PDF's geometric positions to structural,
	Individual annotations are implemented with separate behaviors (see subclass links),
	which generally convert PDF-specific annotation to format-independent Multivalent.

<!--
Contents	text string
P		dictionary (page iref)
Rect    rectangle
NM		text (page-unique anno name)
M		date or string (date and time modified)
F		integer (flags)
BS		dictionary (border style)
Border		array
AP		dictionary (appearance)
AS		names (appearance state)
C		array (RGB color)
CA		number (constant opacity)
T		text (pop-up title)
Popup		dicionary (iref popup)
A		action
AA		additional-actions
StructParent	integer


19 annotation types:
Text Text annotation
Link Link annotation
FreeText (PDF 1.3) Free text annotation
Line (PDF 1.3) Line annotation
Square (PDF 1.3) Square annotation
Circle (PDF 1.3) Circle annotation
Highlight (PDF 1.3) Highlight annotation
Underline (PDF 1.3) Underline annotation
Squiggly (PDF 1.4) Squiggly-underline annotation
StrikeOut (PDF 1.3) Strikeout annotation
Stamp (PDF 1.3) Rubber stamp annotation
Ink (PDF 1.3) Ink annotation
Popup (PDF 1.3) Pop-up annotation
FileAttachment (PDF 1.3) File attachment annotation
Sound (PDF 1.2) Sound annotation
Movie (PDF 1.2) Movie annotation
Widget (PDF 1.2) Widget annotation
PrinterMark (PDF 1.4) Printer’s mark annotation
TrapNet (PDF 1.3) Trap network annotation
-->

	@version $Revision: 1.1 $ $Date: 2009/06/10 09:58:17 $
*/
public abstract class Anno extends Behavior {
  public static final String LAYER = "PDFAnno";

  /**
	Announce a PDF annotation found on the page that some handler (another behavior) should create.
	<p><tt>"pdfAnnoCreate"</tt>: <tt>arg=</tt> {@link Dict} <var>annotation-dictionary</var>, <tt>in=</tt> {@link PDF} <var>handle to this behavior (for fetching component objects)</var>, <tt>out=</tt><var>root of PDF document tree</var>.
  */
  public static final String MSG_CREATE = "pdfAnnoCreate";

  /**
	Request execution of PDF annotation.
	<p><tt>"pdfAnnoCreate"</tt>: <tt>arg=</tt> {@link Dict} <var>annotation-dictionary</var>, <tt>in=</tt> {@link PDF} <var>handle to this behavior (for fetching component objects)</var>, <tt>out=</tt><var>root of PDF document tree</var>.
  */
  public static final String MSG_EXECUTE = "pdfAnnoExecute";



  /** Cache leaves, for table of contents with hundreds of /Link's. */
  private List<Leaf> leaves_ = null;	// cached


  /** Returns true if semantic event is a PDF annotation, <tt>/Subtype</tt> matches <var>subtype</var>, and event's argument are valid. */
  protected boolean checkArgs(String subtype, SemanticEvent se) {
	boolean ok = false;
	Object arg = se.getArg();
	if ((MSG_CREATE==se.getMessage() || MSG_EXECUTE==se.getMessage()) && arg!=null && arg.getClass()==COS.CLASS_DICTIONARY && se.getIn() instanceof PDF && se.getOut() instanceof Node) {
		Dict dict = (Dict)arg;
		PDF pdf = (PDF)se.getIn();
		PDFReader pdfr = pdf.getReader();
		try { ok = subtype.equals(pdfr.getObject(dict.get("Subtype"))); } catch (IOException ignore) {}
	}
	return ok;
  }


  /**
	Given geometric area of interest in tree rooted at <var>root</var>, return all content leaves within.
	Used by hyperlink, highlight, ..., but not note, ink(?).
	@return 0-length List if no hits.
  */
  List<Leaf> findLeaves(SemanticEvent se) {
	List<Leaf> leaves = new ArrayList<Leaf>(10);
	Dict dict = (Dict)se.getArg();
	PDF pdf = (PDF)se.getIn();
	PDFReader pdfr = pdf.getReader();
	try {
		Object rect = pdfr.getObject(dict.get("Rect"));
		if (rect instanceof Object[]) {
			Rectangle r = COS.array2Rectangle((Object[])rect, pdf.getTransform());
			//System.out.println(r);
			r.grow(-1,-1);	// no sharing of borders (maybe anno writer just sloppy?) -- maybe even smaller since just intersecting

			//r.width -= 1; r.height -= 1;
			findLeaves(r, (Node)se.getOut(), leaves);
		}
	} catch (Exception ignore) {}

	return leaves;
  }


  List<Leaf> findLeaves(Rectangle r, Node root, List<Leaf> hits) {
	// slow for now, on leaves in absolute coordinates
//System.out.println("intersects "+r);
	List<Leaf> leaves = leaves_;
	if (leaves_==null) {
		leaves = leaves_ = new ArrayList<Leaf>(500);
		for (Leaf n=root.getFirstLeaf(), end=root.getLastLeaf().getNextLeaf(); n!=end; n=n.getNextLeaf()) leaves.add(n);
	}

	for (int i=0,imax=leaves.size(); i<imax; i++) {
		Leaf l = (Leaf)leaves.get(i);
//if (n.size()>1) System.out.println("  "+n.getName()+"\t"+l.bbox);
		
		Rectangle bbox = (Rectangle) l.bbox.clone();
		//System.out.println(l.getAbsLocation());
		//System.out.println("  "+l.getName()+"\t"+bbox);
		bbox.y = l.getAbsLocation().y;
		bbox.x = l.getAbsLocation().x;
		if (r.intersects(bbox) /*&& n instanceof LeafText/*wrong*/) {
			Rectangle overlap = r.intersection(bbox);
			if (overlap.width > bbox.width/2 && overlap.height > bbox.height/2)
				hits.add(l);
				//else System.out.println(n.getName()+" not 50%: "+overlap.width+"x"+overlap.height+" vs "+bbox.width+"x"+bbox.height);
		}
	}

	// if not hits, don't require 50% overlap
	if (hits.size()==0)
	for (int i=0,imax=leaves.size(); i<imax; i++) {
		Leaf l = (Leaf)leaves.get(i);
		if (r.intersects(l.bbox) /*&& n instanceof LeafText/*wrong*/) hits.add(l);
	}

	return hits;
  }


/* INodes not formatted... and don't want to wait for full formatting as contents may have shifted due to annos => want straight format
  List<Leaf> findLeaves(Rectangle r, Node n, List<> leaves) {
	if (r.intersects(n.bbox)) {
		int dx=n.dx(), dy=n.dy();
		r.translate(-dx, -dy);

		if (n instanceof INode) {
			INode p = (INode)n;
			for (int i=0,imax=p.size(); i<imax; i++) findLeaves(r, n, leaves);
		} else leaves.add(n);

		r.translate(dx, dy);
	}
	return leaves;
  }
*/

  /**
	Given /Rect (and handle to {@link PDF}) in semantic event, convert to {@link multivalent.Span}.
  */
  protected Span createSpan(SemanticEvent se, String logical, String bename) {
	Span span = null;
	List<Leaf> leaves = findLeaves(se);	// "Rect" => content (leaves)
//System.out.println("leaves = "+leaves);
	if (leaves.size()>0) {
//System.out.print("/"+leaves.size());
		span = (Span)Behavior.getInstance(logical, bename, null, getDocument().getLayer(LAYER));
		Leaf lastl = (Leaf)leaves.get(leaves.size()-1);
		span.moveq((Leaf)leaves.get(0),0, lastl,lastl.size());
		span.repaint(200);
//System.out.println(((Leaf)leaves.get(0)).getName()+" .. "+lastl.getName());
		// /Dest, /A, /AA

	} else {
		if (PDF.DEBUG) System.out.println("no leaves in "+se.getArg());
	}

	return span;
  }

  /** Applies appearance properties common to all (most) annotation types. */
  protected void decorate() {
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (msg==Multipage.MSG_OPENPAGE) {
		leaves_ = null;	// clear cache on new page
	}
	return super.semanticEventAfter(se, msg);
  }
}
