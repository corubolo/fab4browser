package multivalent.std.adaptor.pdf;

import java.io.IOException;

import multivalent.Browser;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.std.span.HyperlinkSpan;



/**
	Converts PDF annotation dictionary <code>/Subtype /Link</code> to native hyperlink.

<!--
Dest	array, name, or string
H		highlighting mode (None Invert Outline Push)
PA		URI action formerly associated
-->

	@version $Revision: 1.1 $ $Date: 2009/06/10 09:58:17 $
*/
public class AnnoLink extends Anno {

  /**
	Attach hyperlink and store incoming data for when/if clicked.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) /*throws IOException*/ {
	if (checkArgs("Link", se)) {
		Dict dict = (Dict)se.getArg();    // various attributes
		PDF pdf = (PDF)se.getIn();  // resolve objects
		PDFReader pdfr = pdf.getReader();  // resolve objects
//System.out.println("LINK: "+dict);

		if (Anno.MSG_CREATE==msg) {
			String key = (dict.get("Dest")!=null? "Dest": dict.get("A")!=null? "A": null);
if (key==null) System.out.println("bogus: "+dict);
			HyperlinkSpan span = (HyperlinkSpan)createSpan(se, "link", "HyperlinkSpan");    // "/Link"=>"link"
//try { System.out.println(span.getStart().leaf+" .. "+span.getEnd().leaf+"   "+pdfr.getObject(se.getArg())); } catch (Exception e) {}
			
			//System.out.println("..."+span);
			if (span!=null) {
				SemanticEvent linkse = new SemanticEvent(getBrowser(), Anno.MSG_EXECUTE, se.getArg(), se.getIn(), se.getOut());
				span.setTarget(linkse);

				if (key!=null) merge(span, linkse, key);    // also try merging with following
//System.out.println("make Link: "+se.getArg()+" on "+span.getStart()+".."+span.getEnd());
				
			}


		} else if (Anno.MSG_EXECUTE==msg) {
			Object A = null;
			try { A = pdfr.getObject(dict.get("A")); } catch (IOException ignore) {}
			Browser br = getBrowser();
//System.out.println("execute "+A);
			if (A!=null) {  // pass on Action
				br.eventq(new SemanticEvent(br, Action.MSG_EXECUTE, A, se.getIn(), se.getOut()));

			} else {  // translate plain link into GoTo Action
				Dict actdict = new Dict(5);
				actdict.put("S", "GoTo");
				try { actdict.put("D", pdfr.getObject(dict.get("Dest"))); } catch (IOException ignore) {}
				br.eventq(new SemanticEvent(br, Action.MSG_EXECUTE, actdict, se.getIn(), se.getOut()));
			}
		}
	}

	return super.semanticEventAfter(se, msg);
  }



  // generalize and put in Anno.java

  /** Merge with adjacent span to same target, if any.  Returns span merged with, or <code>null</code> if none. */
  Span merge(HyperlinkSpan span, SemanticEvent se, String key) {
	Mark start=span.getStart(), end=span.getEnd();
	Leaf prevl=start.leaf.getPrevLeaf(), nextl=span.getEnd().leaf.getNextLeaf();

	// keep and extend previous, destroy latest
	Span mergespan = null;
	if (prevl!=null && (mergespan=mergeAt(se, key, prevl,prevl.size()))!=null) {
		mergespan.moveq(mergespan.getStart(), end);
	} else if (nextl!=null && (mergespan=mergeAt(se, key, nextl,0))!=null) {
		mergespan.moveq(start, mergespan.getEnd());
	}

	if (mergespan!=null) { span.moveq(null); span.destroy(); }  // without moveq(null), destroy() marks dirty
	return mergespan;
  }

  Span mergeAt(SemanticEvent se, String key, Leaf leaf, int at) {
	if (leaf!=null) for (int i=leaf.sizeSticky()-1; i>=0; i--) {
		Mark m=leaf.getSticky(i); if (m.offset!=at || !(m.getOwner() instanceof HyperlinkSpan)) continue;
		HyperlinkSpan prevspan = (HyperlinkSpan)m.getOwner();
		if ("link".equals(prevspan.getName()) && prevspan.getTarget() instanceof SemanticEvent) {
			SemanticEvent prevse = (SemanticEvent)prevspan.getTarget();
/*Dict semap=(Dict)se.getArg(), prevmap=(Dict)prevse.getArg();
Object sedest=semap.get("Dest"), prevdest=prevmap.get("Dest");
System.out.println("Dest.equals() "+sedest.equals(prevdest));
System.out.println("Dest.equals() "+sedest.toString().equals(prevdest.toString()));
System.out.println("\t"+sedest); System.out.println("\t"+prevdest);
System.out.println("msg == "+(se.getMessage()==prevse.getMessage()));
System.out.println("comparing /Links: "+se.getArg()+" vs "+prevse.getArg());*/
			//if (se.equals(prevspan.getTarget())) {  => different /Rect
/*System.out.println(prevse);
System.out.println(prevse.getArg());
System.out.println(prevse.getMessage());
System.out.println(((Dict)prevse.getArg()).get("Dest"));
System.out.println(se);
System.out.println(se.getMessage());
System.out.println(se.getArg());
System.out.println(((Dict)se.getArg()).get("Dest"));*/
			if (se.getMessage()==prevse.getMessage() && prevse.getArg() instanceof Dict
				&& ((Dict)prevse.getArg()).get(key)!=null
				&& ((Dict)se.getArg()).get(key).toString().equals(((Dict)prevse.getArg()).get(key).toString())
			) {
				//System.out.println("merged "+se.getArg());
				return prevspan;
				//break;
			}
		}
	}
	return null;
  }
}
