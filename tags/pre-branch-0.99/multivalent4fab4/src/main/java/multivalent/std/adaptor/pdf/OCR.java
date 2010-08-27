package multivalent.std.adaptor.pdf;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Shape;
import java.awt.image.FilteredImageSource;
import java.awt.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;

import multivalent.*;
import multivalent.node.Fixed;
import multivalent.node.FixedLeafOCR;
import multivalent.node.FixedLeafUnicode;
import multivalent.node.FixedLeafShape;
import multivalent.node.FixedLeafImage;
import multivalent.std.OcrView;

import phelps.awt.image.TransparentFilter;



/**
	Normalize OCR + text, which can be implemented in various ways, into a document tree with
	hybrid image-text leaves ({@link FixedLeafOCR}s).

	Various ways to implement OCR + text in PDF:
	<ol>
	<li>image: full page, over content only (not margins), patches, strips width of screen but only 100 pixels high so you need 30 separate FAX images per page
	<li>text: invisible text (<code>Tr 3</code>), white text on white background
	<li>image + text: image over text, image under text, image under recognized text that's removed from background image (as in maps)
	<li>explicit background, which would erase background images drawn in Xdoc strategy
	<li>small bounding boxes from Capture, resulting in clipped endpoints
	</ol>

	<p>If determined to be scanned paper chunk, convert to method used in Xdoc.

<!--
	<p>Maybe make this a behavior running in buildAfter.
	Instead of collecting FAX images during parsing, add prescan here?
-->

	@version $Revision: 1.9 $ $Date: 2003/08/29 04:10:18 $
*/
public class OCR {
  /** Key to {@link multivalent.Layer} for OCR-specific behaviors. */
  public static final String VAR_LAYER = "layerOCR";

  static final int BACKGROUND_FRINGE = 10;


  static void transform(INode root, List<FixedLeafImage> imgsl, PDF pdf, Component br) {
	if (imgsl.size() == 0) return;

	Document doc = root.getDocument();
	Layer ocrLayer = (Layer)doc.getVar(VAR_LAYER);
	if (ocrLayer!=null) ocrLayer.setActive(false);

//System.out.println("OCR image frags "+imgsl.size());

	// init
	int imgslen = imgsl.size();
	FixedLeafImage[] imgs = imgsl.toArray(new FixedLeafImage[imgslen]);
	Rectangle[] imgibbox = new Rectangle[imgslen];
	for (int i=0; i<imgslen; i++) {imgibbox[i] = imgs[i].getIbbox(); /*System.out.println("  "+imgibbox[i]);*/ }

	Layer scratchLayer = doc.getLayer(Layer.SCRATCH);


	// interesting leaves
	Map<Leaf,Leaf> seen = new IdentityHashMap<Leaf,Leaf>(imgslen);	// IdentitySet?
	Map<Leaf,Leaf> trans = new IdentityHashMap<Leaf,Leaf>(imgslen);	// IdentitySet?
	List<FixedLeafImage> hybrid = new ArrayList<FixedLeafImage>(imgslen), nonhybrid = new ArrayList<FixedLeafImage>(imgslen);   // "hybrid" if overlap with LeafOCR, non if not; can be both

	// 1. look for overlapping text
	FixedLeafOCR prevOcr = null;
	SpanPDF spanTr3=null, spanColor=null; Leaf startTr3 = null;
	for (Leaf l=root.getFirstLeaf(), endl=root.getLastLeaf().getNextLeaf(), prevl=null; l!=endl; prevl=l, l=l.getNextLeaf()) {
		// track fill color and Tr 3
		for (int i=l.sizeSticky()-1; i>=0; i--) {    
			Mark m=l.getSticky(i); Object o=m.getOwner(); if (!(o instanceof SpanPDF)) continue;
			SpanPDF span=(SpanPDF)o;  assert span.fill==Context.COLOR_INVALID || span.Tr<0;
			if (m == span.getEnd()) {   // just care about starts here, ends below
			} else if (span.fill!=Context.COLOR_INVALID) {
				if (spanTr3!=null && spanColor!=null) { spanColor.moveq(null); spanColor.destroy(); }    // changes within Tr=3 span (rare!) thrown away
				spanColor = span;  // just track color
			} else if (span.Tr==3) {
				assert spanTr3==null && m.offset==0: spanTr3+" "+m.offset+" @ "+l;
//System.out.println("Tr3 @ "+l);
				if (spanColor!=null) {  // no colors on invisible text, so close up prevailing color (LATER: Tr=3 just affects text, so could have splines....)
					if (prevl!=null) {
						SpanPDF split = (SpanPDF)Behavior.getInstance("fill", "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
						split.fill = span.fill; split.getEnd().leaf = spanColor.getEnd().leaf; split.getEnd().offset = spanColor.getEnd().offset;
//System.out.println(spanColor.fill+" "+spanColor.getStart()+".."+spanColor.getEnd()+" =>   .. "+prevl);
						spanColor.moveq(spanColor.getStart().leaf,spanColor.getStart().offset, prevl,prevl.size());
						spanColor = split;
					} //else reuse
				}
				spanTr3=span; startTr3 = null; //System.out.println(" open Tr3 @ "+l);
			}
		}

		if (l instanceof FixedLeafUnicode) {
			Fixed f = (Fixed)l;
			Rectangle ibbox = f.getIbbox();

			// A. intersects, so possible OCR-image hybrid?
			// seen PDFs with 30 fragments and 550 leaves, so optimize this
			FixedLeafImage scan = null;
//	right > tx) && bottom > ty && tw > rx && th > ry
			//for (int i=imgslen-1; i>=0; i--) if (imgibbox[i].intersects(ibbox)) {
			for (int i=imgslen-1, oh; i>=0; i--)    // visual intersection order.  O(n*m) where n=#leaves and m=#images.  seen m=30 on FAX strips.  haven't seeen m==n
				//Rectangle r=imgibbox[i]; int rx=r.x, ry=r.y, rr=rx+r.width; => intersects fast
				//if (ry+r.height > ibbox.y && ry < ibbox.y+ibbox.height && rx+r.width > ibbox.x && rx < ibbox.x+ibbox.width) {   // visual intersection order
				if (imgibbox[i].intersects(ibbox)
					&& ((oh=ibbox.y+ibbox.height - imgibbox[i].y) >= ibbox.height/2 || oh<0)) { // FAX slices and overlap two -- need parts of both, but at least take larger
					scan=imgs[i]; break;
				}
//if (i>0) System.out.println(l.getName()+" / "+phelps.text.Rectangles2D.pretty(ibbox)+"  overlaps  #"+i+" "+phelps.text.Rectangles2D.pretty(imgibbox[i]));

			// B. check for OCR types: image-over-text and invisible text-over-image
			if (scan!=null) {
				if (seen.get(scan)==null) {   // image above || ...
					// image-over-text OCR

				} else if (spanTr3!=null) {   // remove Tr=3 on these leaves, so close up Tr=3 that we're keeping because doesn't overlap image   // ... (/*below &&*/ Tr=3)
					spanTr3.moveq(startTr3,0, prevl,prevl.size());  assert prevl!=null;
					SpanPDF split = (SpanPDF)Behavior.getInstance("Tr"/*+Tr*/, "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
//System.out.println("split @ "+l);   // should rare, but not so much
					split.Tr = spanTr3.Tr;
					spanTr3=split; startTr3=null;
				} else {   // regular (non-OCR) text on top of image
					if (!nonhybrid.contains(scan)) nonhybrid.add(scan);
					scan=null;
				}

			} else {    // no overlap
				if (spanTr3!=null && startTr3==null) startTr3=l;    // Tr3 to keep
				scan=null;
			}

			// C. is hybrid, transform leaf to {@link multivalent.node.FixedLeafOCR}
			if (scan!=null) {
				if (!hybrid.contains(scan)) hybrid.add(scan);

				if (trans.get(scan) == null && br!=null/*automated testing*/) {
					scan.setImage(br.createImage(new FilteredImageSource(scan.getImage().getSource(), new TransparentFilter(Color.WHITE))));
					trans.put(scan,scan);
				}

//System.out.println("zapping "+l+" / "+scan+" / "+ibbox);
				FixedLeafOCR newl = new FixedLeafOCR(null, null, null, scan, ibbox);
				l.morphInto(newl); newl.ibaseline = l.baseline;
				l = newl;   // for l.getNextLeaf()!
//System.out.println("OCR "+newl); newl.dump(1,1);

				// widen bbox if next leaf on same line
				if (prevOcr!=null) {
					Rectangle prevr = prevOcr.getIbbox();
					int newwidth = prevr.width;
					if (prevOcr.getParentNode()==l.getParentNode() && prevr.x < ibbox.x) {
						//System.out.print(newwidth+" => ");
						newwidth = Math.max(newwidth, ibbox.x /*- 2 abut*/ - prevr.x);
						//System.out.println(newwidth);
					} else newwidth += 5;    // last on line
					prevOcr.getBbox().width = prevr.width = newwidth;
				}
				prevOcr = newl;
			}

		} else if (l instanceof FixedLeafImage) seen.put(l, l);
		//else if (l instanceof FixedLeafShape) {}    // maybe background

//System.out.println(l);
		if (spanTr3!=null) for (int i=0,imax=l.sizeSticky(), size=l.size(); i<imax; i++) { // close Tr3?
//if ("1991".equals(l.getName())) System.out.println("end @ "+l+", start="+startTr3+", Tr3="+Tr3+"  "+m);
			if (l.getSticky(i)==spanTr3.getEnd()) {
				if (startTr3==null) { spanTr3.moveq(null); spanTr3.destroy(); /*System.out.println("remove Tr3");*/} else {/*System.out.println("Tr3 close "+startTr3+"/"+(startTr3==null)+",0 .. "+l+","+size);*/ spanTr3.moveq(startTr3,0, l,size); }
				spanTr3 = null;
				if (spanColor!=null) {  // restart color
					Leaf nextl=l.getNextLeaf();
//System.out.println("restart color @ "+nextl);
					if (nextl!=endl) spanColor.moveq(nextl,0, spanColor.getEnd().leaf, spanColor.getEnd().offset); else { spanColor.moveq(null); spanColor.destroy(); }
				}
				break;
			}
		}
	}


	// 2. OCR somewhere, turn on overall machinery
	if (hybrid.size() == 0) return;
	//if (PDF.DEBUG) System.out.println("=> OCR");

	// A. transform leaves and remove Tr 3 spans (easier to manage spans here)
	// already done above

	// B. load OCR hub
	if (ocrLayer == null) {
		ocrLayer = doc.getLayers().getInstance("Ocr");
		doc.putVar(VAR_LAYER, ocrLayer);
	}
	ocrLayer.setActive(true);

	// C. remove image(s) and give to OCRView behavior
	for (int i=0,imax=hybrid.size(); i<imax; i++) {
		Leaf l = (Leaf)hybrid.get(i);
		if (!nonhybrid.contains(l)) l.removeTidy(root);
	}
	// always drawn underneath

	// communication with OCRView
	doc.removeAttr(Fixed.ATTR_REFORMATTED);
	doc.putVar(OcrView.VAR_FULLIMAGE, hybrid);

/*	StyleSheet ss = root.getDocument().getStyleSheet();
	CLGeneral cl = (CLGeneral)ss.get("pdf");
//System.out.println("OCR bg = white!");
	cl.setBackground(Color.WHITE);    // white in intrisic stylesheet, but possible to override
	*/
  }


  static Color extractBackground(INode root, PDF pdf) {
	Color bgcolor = null;
	FixedLeafShape bkgnd = null;    // has to be one of first few leaves -- has to be very first?

	// find big covering shape, and determine color at that point
	int ctr=0;
	Rectangle cropbox = pdf.getCropBox(); int wmin = cropbox.width - BACKGROUND_FRINGE/* *9/10*/, hmin=cropbox.height - BACKGROUND_FRINGE /*9)/10*/;
	for (Leaf l=root.getFirstLeaf(), endl=root.getLastLeaf().getNextLeaf(); ctr<5 && bkgnd==null && l!=endl; ctr++, l=l.getNextLeaf()) {
		bkgnd = null;
		if (l instanceof FixedLeafShape) {
			FixedLeafShape fls = (FixedLeafShape)l;
			Shape shape = fls.getShape();
			if (shape instanceof Rectangle && shape.getBounds().width > wmin && shape.getBounds().height > hmin) {
				bkgnd = fls;
				//System.out.println(shape+" vs "+phelps.text.Rectangles2D.pretty(cropbox)+" "+bgcolor);
			}
		}

		if (bkgnd!=null && l.sizeSticky()==2) {	// track fill color and Tr 3
			Mark m = l.getSticky(0);
			Object o = m.getOwner();
			if (o instanceof SpanPDF) {
				SpanPDF span = (SpanPDF)o;
				if (span.getEnd().leaf==l && span.fill!=Context.COLOR_INVALID) { bgcolor=span.fill; break; }
			}
		}
	}


	// relocate, if any, to stylesheet
	if (bkgnd==null) bgcolor=null;
	else if (bgcolor!=null) {   // else drawing shape w/o color
//if (PDF.DEBUG) System.out.println(bkgnd.getIbbox()+" "+bgcolor);
		bkgnd.removeTidy(root);

		StyleSheet ss = root.getDocument().getStyleSheet();
		CLGeneral cl = (CLGeneral)ss.get("pdf");
		//if (PDF.DEBUG) cl.setBackground(Color.MAGENTA); else
		cl.setBackground(bgcolor);  // should set in intrinsic stylesheet so can override
	}

	return bgcolor;
  }
}
