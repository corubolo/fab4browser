package multivalent.std.adaptor.pdf;

import java.io.*;
import java.util.*;

import com.pt.awt.NFont;



/**
	Parses Adobe font metrics files (<code>.afm</code>) and generates tables in {@link Core14AFM} source code.
	Core14AFM augments the AFM files with additional entries needed in practice.
	For one-time use; not included in JAR.
	Reads from stdin, writes to stdout.

	@version $Revision: 1.5 $ $Date: 2003/10/12 02:36:55 $
*/
public class Afm2pdf {
  public static void convert(InputStream in) throws IOException {    // if throws exception, crash
	BufferedReader r = new BufferedReader(new InputStreamReader(in));

	//String encoding = null;
	String fontname = null;
	int firstchar=Integer.MAX_VALUE, lastchar=Integer.MIN_VALUE, curchar = -1;
	int[] widths = new int[1024];
	boolean fsymbolic=false, ffixed=false, fserif=true, fscript=false, fitalic=false, fallcap=false, fsmallcap=false, fforcebold=false;
	int stemv, stemh;
	String italicangle=null, capheight=null, xheight=null, ascent=null, descent=null, leading=null;   // don't parse as int's here -- just pass along
	//Rectangle bbox = new Rectangle();
	String bboxx=null, bboxy=null, bboxwidth=null, bboxheight=null;
	Map map = null;//Encoding.uname2char_;

	int maxwidth=Integer.MIN_VALUE, avgwidth=0;
	int totalw=0, charcnt=0;

	String line;
	int w = -1;
	while ((line = r.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(line);
		st.nextToken();

		if (line.startsWith("ItalicAngle")) {   // "ItalicAngle 0"
			italicangle = st.nextToken();
			try { if (Float.parseFloat(italicangle)!=0) fitalic=true; } catch (NumberFormatException nfe) {}

		} else if (line.startsWith("FontBBox")) {   // "FontBBox -168 -218 1000 935"
			bboxx = st.nextToken();
			bboxy = st.nextToken();
			bboxwidth = st.nextToken();
			bboxheight = st.nextToken();

		} else if (line.startsWith("FontName")) {   // PostScript name.  "FontName Times-Bold"
			fontname = st.nextToken();

			if (fontname.startsWith("Helvetica")) fserif = false;

		//} else if (line.startsWith("EncodingScheme")) {   // "EncodingScheme AdobeStandardEncoding" => all either "AdobeStandardEncoding" or "FontSpecific"
		//	encoding = st.nextToken();

		//} else if (line.startsWith("")) {
		} else if (line.startsWith("Comment")) {
			System.out.println("Comment = "+line);

		} else if (line.startsWith("Weight")) {   // "Weight Bold"

		} else if (line.startsWith("IsFixedPitch")) {   // "IsFixedPitch true" -- Courier
			 ffixed = st.nextToken().equalsIgnoreCase("true");

		} else if (line.startsWith("CharacterSet")) {   // "CharacterSet Special" -- Symbol and ZapfDingbats
			 fsymbolic = st.nextToken().equalsIgnoreCase("Special");
			 if (fsymbolic) fserif = false;

		} else if (line.startsWith("CapHeight")) {   // "CapHeight 676"
			capheight = st.nextToken();

		} else if (line.startsWith("XHeight")) {   // "XHeight 461"
			xheight = st.nextToken();

		} else if (line.startsWith("Ascender")) {   // "Ascender 683"
			ascent = st.nextToken();

		} else if (line.startsWith("Descender")) {   // "Descender -217"
			descent = st.nextToken();

		} else if (line.startsWith("StartCharMetrics")) {
			while ((line = r.readLine()) != null && !line.startsWith("EndCharMetrics")) {
				// C 33 ; WX 333 ; N exclam ; B 81 -13 251 691 ;
				st = new StringTokenizer(line, ";");
				while (st.hasMoreTokens()) {
					StringTokenizer ste = new StringTokenizer(st.nextToken());
					String subline = ste.nextToken();
					if (subline.startsWith("C")) {
						curchar = Integer.parseInt(ste.nextToken());
						//if (curchar == -1) continue;    // break; happens to have same effect on these particular data
						//if (curchar<firstchar) firstchar=curchar; else if (curchar>lastchar) lastchar=curchar;
						//charcnt++;

					} else if (subline.startsWith("WX")) {
						w = Integer.parseInt(ste.nextToken());
//System.out.println("curchar = "+curchar);
						//widths[curchar] = w;  // set curchar later
						if (w>maxwidth) maxwidth=w;
						totalw += w;

					} else if (subline.startsWith("N")) {
						// map to code
						String name = ste.nextToken();
						String codes = (String)map.get(name);
						if (codes==null) System.out.println("can't map "+name);
						if (codes==null || curchar==-1) continue;
						char stdchar = codes.charAt(0); if (stdchar==0 && curchar==-1) continue;
						if ("ellipsis".equals(name)) System.out.println("* ellipsis code = "+(int)stdchar);
						assert stdchar!=0: name;

						if (stdchar<firstchar) firstchar=curchar; else if (stdchar>lastchar) lastchar=stdchar;
						charcnt++;

						// FYI, report differences with Adobe's "other" standard encoding
						if (stdchar!=curchar) System.out.println("difference on "+name+" "+(int)curchar+" in AFM vs "+(int)stdchar+" in PDF");

						// poke width, which comes earlier
						if (stdchar >= 256) { for (int i=0,imax=codes.length(); i<imax; i++) System.out.print(" "+(int)codes.charAt(i));  System.out.println(); }
						assert stdchar < widths.length: ((int)stdchar)+"/u"+Integer.toHexString(stdchar)+" "+name;
						widths[stdchar] = w;

					} else if (subline.startsWith("B")) {
					//} else if (subline.startsWith("")) {
					}
				}
			}

		}
		// what about MissingWidth, StemV, StemH, Leading
		// compute MaxWidth, AvgWidth
	}

	r.close();
	avgwidth = (totalw / charcnt);


	// report

	//System.out.println("\t// "+fontname);

	System.out.println("\tfontname = \""+fontname+"\";");
	System.out.println("\twidths_.put(fontname, new int[] {");
	for (int i=firstchar; i<=lastchar; i+=20) {
		System.out.print("\t\t");
		for (int j=i,jmax=i+20; j<jmax; j++) System.out.print(widths[j]+", ");
		System.out.println();
	}
	System.out.println("\t});");

	//System.out.println("\tfirsts_.put(\""+fontname+"\", new Integer("+firstchar+"));");
	assert firstchar==32;   // other code makes this assumption

	System.out.println("\tfdesc = new HashMap(15);");
	System.out.println("\tfdesc.put(\"FontName\", fontname);");
	System.out.println("\tfdesc.put(\"FontBBox\", new int[] { "+bboxx+", "+bboxy+", "+bboxwidth+", "+bboxheight+" });");
	System.out.println("\tfdesc.put(\"ItalicAngle\", new Double("+italicangle+"));");
	if (capheight!=null) System.out.println("\tfdesc.put(\"CapHeight\", new Integer("+capheight+"));");
	if (xheight!=null) System.out.println("\tfdesc.put(\"XHeight\", new Integer("+xheight+"));");
	if (ascent!=null) System.out.println("\tfdesc.put(\"Ascent\", new Integer("+ascent+"));");
	if (descent!=null) System.out.println("\tfdesc.put(\"Descent\", new Integer("+descent+"));");
	System.out.println("\tfdesc.put(\"MaxWidth\", new Integer("+maxwidth+"));");
	System.out.println("\tfdesc.put(\"AvgWidth\", new Integer("+avgwidth+"));");
	//System.out.println("fdesc.put(\"\"), new Integer("++")");

	int flags = (ffixed? NFont.FLAG_FIXEDPITCH: 0) | (fserif? NFont.FLAG_SERIF: 0) | (fsymbolic? NFont.FLAG_SYMBOLIC: NFont.FLAG_NONSYMBOLIC)
		| (fscript? NFont.FLAG_SCRIPT: 0) | (fitalic? NFont.FLAG_ITALIC: 0)
		| (fallcap? NFont.FLAG_ALLCAP: 0) | (fsmallcap? NFont.FLAG_SMALLCAP: 0) | (fforcebold? NFont.FLAG_FORCEBOLD: 0);
	/*if (flags != NFont.FLAG_DEFAULT)*/ System.out.println("\tfdesc.put(\"Flags\", new Integer("+flags+"));");
	System.out.println("\tfdesc_.put(fontname, fdesc);");

	System.out.println();
  }

  public static void main(String[] argv) {    // if throws exception, crash
	try { convert(System.in); } catch (IOException ioe) { ioe.printStackTrace(); }
  }
}

/* information needed

6 0 obj
<<
/Type /Font
/Subtype /TrueType
/Name /F0
/BaseFont /ComicSansMS
/FirstChar 32
/LastChar 255
/Widths [ 299 237 425 841 692 820 653 386 365 365 528 479 277 416 247 512
610 449 610 610 610 610 610 610 610 610 299 299 381 509 381 523
931 730 629 602 722 624 607 678 768 547 664 610 550 882 795 798
520 877 629 692 678 735 651 1040 724 634 692 376 550 376 580 626
555 512 594 515 588 547 509 531 577 280 403 539 275 776 523 525
534 520 479 487 471 520 485 683 591 520 536 365 422 365 596 501
509 501 509 509 509 509 509 509 509 509 509 509 509 501 509 501
501 509 509 509 509 509 509 509 509 509 509 509 509 501 509 509
299 237 624 792 509 634 536 634 555 795 525 577 479 416 795 602
408 479 651 651 555 520 694 596 555 651 449 577 651 651 520 523
299 624 1242 730 730 730 1087 602 624 624 730 730 547 624 547 547
403 795 798 547 798 730 798 651 798 735 798 735 735 509 520 444
512 512 512 512 512 512 912 515 547 547 547 547 280 280 280 280
722 523 525 525 525 525 525 179 525 520 520 520 520 634 534 722
]
/Encoding /WinAnsiEncoding
/FontDescriptor 7 0 R
>>
endobj
7 0 obj
<<
/Type /FontDescriptor
/FontName /ComicSansMS
/Flags 32
/FontBBox [ -250 -292 1492 1496 ]
/MissingWidth 376
/StemV 85
/StemH 85
/ItalicAngle 0
/CapHeight 1102
/XHeight 551
/Ascent 1102
/Descent -292
/Leading 394
/MaxWidth 1243
/AvgWidth 468
>>
endobj
*/
