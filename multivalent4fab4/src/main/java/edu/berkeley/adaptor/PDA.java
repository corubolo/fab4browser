package edu.berkeley.adaptor;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.image.FilteredImageSource;
import java.awt.*;

import phelps.awt.image.TransparentFilter;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.node.FixedLeafOCR;
import multivalent.node.Fixed;
import multivalent.node.FixedI;
import multivalent.node.FixedIVBox;
import multivalent.node.FixedLeafImage;
import multivalent.std.OcrView;



/**
	Media adaptor for Caere PDA files (.pda => doc tree).
	<blockquote>
	Caere XXX, Appendix B, "PDA Format"
	</blockquote>

<!--
	<hr>To do
	If make more of these, update to multipage and check that not creating some bbox Rectangle's twice.
-->

	@see multivalent.node.FixedLeafOCR
	@see berkeley.adaptor.Xdoc

	@version $Revision: 1.8 $ $Date: 2005/01/03 10:00:03 $
*/
public class PDA extends MediaAdaptor {
  private static final boolean DEBUG = false;


  private static final int[] BMU = { -1, 1200, 600, 400, 300, 240, 200, -1, 150, -1, -1, -1, 100 };

  private static NFont defaultFont = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 12f);


  //private Document doc = null;
  //private Image fullpage_ = null;
  private FixedLeafImage full_ = null;
  private double scale = 1.0;   // set from .pda

  // shared class globals to reduce sending as parameters
  // don't have to be careful about synchronization because don't have multiple threads in same object
  //PushbackInputStream in = null;
  private int charcnt = 0;
  private int qcharcnt = 0; // count questionable characters to find bad scans, math

  private PushbackInputStream is_;

/*
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	//Browser br = getBrowser();
	//Document doc = layer.getDocument();
	//String url = doc.getAttr(Document.ATTR_URI);

	// set PAGECOUNT and optionally PAGE
	//String num = url.substring(url.lastIndexOf('/')+1, url.lastIndexOf('.'));
  }
*/


  public void buildBefore(Document doc) {
	Browser br = getBrowser();

	URL pdaurl=null, imageurl=null, biburl=null;
	// for now, hardcoded location of image, bib (and that there is bib)
	// later, take from attributes (again, which we did originally)
	try {
		pdaurl = new URL(getAttr(Document.ATTR_URI));
		imageurl = new URL(pdaurl, "./"+"dennis300"+".gif");  // yay, Java handles ".."
//		biburl = new URL(pdaurl, "../BIB/bib.rfc1357");
	} catch (MalformedURLException e) {
		// display error info line
		System.out.println("*** error making URLs: "+e);
		return;
	}
System.out.println("imageurl = "+imageurl);

	//ocrroot_ = new INode("PDA",null, doc);

	// 1. image
	Image src = Toolkit.getDefaultToolkit().getImage(imageurl);
	Image fullpage = br.createImage(new FilteredImageSource(src.getSource(), new TransparentFilter(Color.WHITE)));
	full_ = new FixedLeafImage("full",null, null, fullpage);
	doc.putVar(OcrView.VAR_FULLIMAGE, full_/*fullpage_*/);
	//br.prepareImage(fullpage_, this); // load image on demand, as may be quickly paging through OCR
	doc.removeAttr(Fixed.ATTR_REFORMATTED);

	// 2. PDA
	//Browser br = getBrowser();
	// URL format: "http://elib.cs.berkeley.edu/docs/data/0600/620/OCR-PDA/00000009.pda"
	try {
		//Document doc = new Document(null,null, docroot);
		/*ocrroot_ = (FixedI)*/parse(doc);	//ocrroot_);
	} catch (/*Parse*/Exception e) {
		System.out.println("couldn't parse .pda "+e);
		e.printStackTrace();
	}
/*
	// 3. bib
	try {
		BufferedReader r = new BufferedReader(new InputStreamReader(biburl.openStream()));
		String line;
		while ((line=r.readLine())!=null) {
			// <attr>::<val>
			int spliti=line.indexOf(':');
			String name=line.substring(0,spliti);
			spliti++; spliti++;   // skip "::"
			while (spliti<line.length() && Character.isWhitespace(line.charAt(spliti))) spliti++;
			String val=line.substring(spliti);
//System.out.println("name="+name+", val="+val);
			br.putAttr(name,val);
		}
	} catch (Exception bibe) { System.out.println("couldn't read bib "+bibe); }


	pagecnt = Integers.getInteger(doc.getAttr(Document.ATTR_PAGECOUNT, Integer.MAX_VALUE), 1);
*/
  }


  // parser helper methods
  private int readInt() throws IOException,ParseException { return readInt(1); }
  private int readInt(int digcnt) throws IOException,ParseException {
	int val=0;
	// should verify that adding digits
	while (digcnt-->0) {
		int c = is_.read();
		if (c>='0' && c<='9') val = val*10 + c-'0';	else throw new ParseException("asked to parse non-digit as digit "+c+"/"+(char)c, 0);
	}
	return val;
  }
/*
  byte[] readData() throws IOException {
	int len=readInt(4);
	byte b = new byte[len];
	int readlen = is_.read(b, 0, len);
	// if (readlen!=len) error
	return b;
  }
*/
  private int readDataAsInt() throws IOException,ParseException {
	int len=readInt(4);
	// if (len>4) error
	int val=0;
//System.out.print("len="+len);
	while (len-->0) val = (val<<8) + is_.read();
//System.out.println(", val="+val);
	return val;
  }


  // assume 300 ppi, adjust by scale
  private void adjustLeaf(FixedLeafOCR n) {
	Rectangle bbox = n.getIbbox();
//System.out.print(""+Rectangles2Dpretty(bbox));
	// need to adjust for skew as well
	bbox.x = (int)(((double)bbox.x) * scale) ;//+ 1 /*+1 for roundoff error*/;
	bbox.y = (int)(((double)bbox.y) * scale);
	bbox.width = (int)(((double)bbox.width) * scale) + 2 /*+ fuzz2*/;
	bbox.height = (int)(((double)bbox.height) * scale) + 2 /*+ fuzz2*/;

	n.ibaseline = (int)((double)n.ibaseline * scale);
///System.out.println("  =>  "+Rectangles2Dpretty(bbox));
  }

//int wcnt=0;
  protected void addWord(FixedI tome, String name, Rectangle bbox, int baseline /*, NFontOCR font*/) {
	if (name==null || bbox==null || (name=name.trim()).length()==0) return;

	FixedLeafOCR l = new FixedLeafOCR(name.trim(),null, null, full_/*fullpage_*/, bbox);
	l.ibaseline = (baseline>0? baseline-bbox.y: bbox.height);
	//l.ibaseline = font.capheight + 2;	 // not ibbox.y; -- baseline relative to top of ibbox
	//l.ibbox.y -= font.capheight; // computed on the fly from baseline
	//l.ibbox.height = font.capheight + font.descend + 2;
	//l.font = font;
	l.font = defaultFont; // for now
	adjustLeaf(l);

	tome.appendChild(l);

	//new FixedLeafOCR(name,null, tome, this, bbox); -- have to use above so set ibbox
//if (wcnt++<30) System.out.println("word = "+l.getName()+", bbox="+l.ibbox);
  }



  /*
  zone
	zone header
	zone body
	ctrl-K
  zone
  zone
  ...
  ctrl-L
  */
  public Object parse(INode parent) throws ParseException,IOException {
	INode pda = new INode("pda",null, parent);
	FixedI root = new FixedI("zones",null, pda);	// always return this, maybe with no children
	FixedIVBox zone = null;
	//FixedIHBox line = null;	  // -- don't know baseline, so this would screw up


	int zonetype;		// 0=uncompressed image, 1=group 3 1-D, 2=group 4 2-D, 3=text
	int CSI=0x9b;		// would like to make final, but =0x1b in 7-bit data

	// generic header (text and image)
	//int ulx,uly,width,height;	// bbox of zone in BMUs
	int rotation;		// clockwise rotation: 0=0 degrees, 1=90, 2=180, 3=270
	int density;		// 0=normal, 1=dark paper, 2=light text
	int resolution;		// 01=1200 pixels/inch, 02=600, 03=400, 04=300, 05=240, 06=200, 08=150, 12=100

	// generic header (text only)
	int charmarklev;	// 0=nothing, 1=only very suspicious, 2=all
	int contentrest;	// 0=unrestricted, 1=numeric only, 2=alphabetic only
	int txtformat;		// 0=best fit filled, 1=best fit not filled, 2=stream
	int charset;		// 0=US ASCII (seven-bit), 1=ANSI (eight-bit)
	int wordmarklev;	// 0=nothing, 1=marked words not in dictionary, 2=not dict and corrected
	int pitch;			// 0=variable, 1=fixed
	int spellmode;		// 0=suspected no-dict text, 1=suspected containg dict(, 2=backwards compat)
	int bboxes;			// 0=no, 1=char bbox, 2=word bbox, 3=both char and word
	int altchars;		// 0=no, 1=alt chars output
	int escapes;		// bitmask: 0=x,y, 1=points, 2=charbbox, 3=word bbox, 4=bold, 5=italic, 6=underline

	// generic header (image only)
	int bitswap;		// 0=leftmost msb, 1=rightmost


	// locals
	//byte[] key,data;
	final int VALCNTMAX=100;
	int[] vals = new int[VALCNTMAX];
	int x,y=-1,w,h;
	int c;
	StringBuffer sb = new StringBuffer(2048);

	Rectangle zbbox=null, cbbox=null, wbbox=null;
//	int wcnt=0;



	is_ = new PushbackInputStream(getInputUni().getInputStream(), 1);
	try {
	  while ((c=is_.read())!=0x0c && c!=-1) {
		if (c==' ' || c==0xa) continue; // strip leading whitespace and newlines
		is_.unread(c);

		// shared and packaging level 1
		zonetype = is_.read()-'0';
		zbbox = new Rectangle(readInt(5),readInt(5), readInt(5),readInt(5));
		rotation=readInt();

		// zones
		switch (zonetype) {
		case 0: // uncompressed image
		case 1: // group 3 1-D compressed image
		case 2: // group 4 2-D compressed image
//System.out.println("image zone, type="+zonetype+", bbox="+zbbox);
			density=readInt();	// packaging level 2 only
			resolution=readInt(2); scale = 72.0 / (double)BMU[resolution] /3.2;	// normalize to 72ppi

			bitswap=readInt();

			FixedLeafOCR l = new FixedLeafOCR("image",null, null, full_/*fullpage_*/, zbbox);
			adjustLeaf(l);
			root.appendChild(l);
			break;

		case 3: // text
System.out.println("text zone, bbox="+zbbox);
			zone = new FixedIVBox("textzone",null, null);	// disregards bbox for computed one
			root.appendChild(zone);	// roundabout way so ibbox gets set
//System.out.println("post text zone, bbox="+zbbox);

			// generic header
			// packaging level 2 adds
			density=readInt();
			/*reserved*/readInt();
			charmarklev=readInt();
			contentrest=readInt();
			txtformat=readInt();
			/*reserved*/readInt();
			charset=readInt();
			wordmarklev=readInt();
			pitch=readInt();
			spellmode=readInt();
			bboxes=readInt();	//if (bboxes!=2 && bboxes!=3) error
			altchars=readInt();
			escapes=readInt(3);
			resolution=readInt(2); scale = 75.0 / (double)BMU[resolution] /3.3/*magic number*/;  // normalize to 72ppi

//System.out.println("bboxes = "+bboxes+" (0=none, 1=char only, 2=word only, 3=char+word");

			//if (charset==0) CSI=0x1b; -- actually not

//System.out.println("resolution = "+resolution+" (02=600, 03=400, 04=300, ...) => scale="+scale);

			// specific data
			while ((c=is_.read())!=0x0a) {
				// so far as I've seen, keys and values in specific data all fit into a Java 4-byte int
				if (c!=',') throw new ParseException("can't parse text zone specific data (key)", 0);
				int key=readDataAsInt();
				is_.read(); // comma
				/*int data=*/ readDataAsInt();	// maybe let keys parse own values: some as bytes, some as int, ...
//System.out.println("key="+key+"/"+(char)((key>>24)&0xff)+""+(char)((key>>16)&0xff)+""+(char)((key>>8)&0xff)+""+(char)(key&0xff)+", val="+data);

				// interpret
				switch (key) {
				case (('L'<<8)+'L'):	// language -- no macros, but not that hard to read over "LL"
					break;
				case (('L'<<8)+'C'):	// country
					break;
				case (('Z'<<8)+'I'):	// popup
					break;
				case (('Z'<<8)+'P'):	// popup
					break;
				case (((((('S'<<8)+'K')<<8)+'E')<<8)+'W'):	// popup
					break;
				case (((((('S'<<8)+'P')<<8)+'E')<<8)+'D'):	// popup
					break;
				// where to find the full list of these?
				default: assert false: key;
				}
			}

			// body
//System.out.println("	reading body, ESC="+CSI);
			while ((c=is_.read())!=0x0b) {	// end marker only at chunk boundaries
				is_.unread(c);
//System.out.println("previous txt = |"+sb.toString()+"|");
//System.out.println("	  intro to new body "+c);
				int len=readInt(4);
//System.out.println("	  body len = "+len);
				//if (len<1 || len>2048) throw new ParseException("invalid body length "+len, 0); -- if do here, should do everywhere len is set
				while (len>0) {
					c=is_.read(); len--;	//if (--len==0) len=readInt(4);
					if (c!=CSI) sb.append((char)c); // let len==0 that happen here, let it be caught above
					else {
						if (len==0) len=readInt(4);	 // zone end can come anywhere!
						// collect parameters
						int val=0, valcnt=0;
						c=is_.read(); is_.unread(c);
						if (Character.isDigit((char)c)) c=';';	// might have no parameters
						while (c==';' && valcnt<VALCNTMAX) {
							val=0;
							while (true) {
								c=is_.read(); if (--len==0) len=readInt(4);
								if (c>='0' && c<='9') val=val*10 + c-'0'; else break;
							}
							vals[valcnt++] = val;
						}
//if (valcnt==VALCNTMAX) { System.out.println("read too many vals!"); System.exit(0); }
//System.out.println("\tESC "+(char)c+", valcnt="+valcnt+", vals: "+vals[0]+","+vals[1]+","+vals[2]+","+vals[3]+"...");

						// switch on code (still in 'c', val holds last value computed)
						switch (c) {
						case ' ':
							c=is_.read(); if (--len==0) len=readInt(4);
							if (c=='C') {	// graphic size selection (GSS)
								System.out.println("points = "+val);
							}
							break;
						case 'f':	// horizontal and vertical position (HVP)
							x=vals[0]; y=vals[1];
System.out.println("HVP x="+x+", y="+y);
							break;
						case '`':	// horizontal position absolute (HPA)
							x=val;
							break;
						case 'm':	// select graphic rendition (SGR)
							// up to three vals: 1=bold, 3=italic, 4=underline
							// add span
							break;
						case 'q':	// character bounding box (CBB)
							cbbox = new Rectangle(vals[0],vals[1], vals[2],vals[3]);
							break;
						case 'p':	// character marker (CMARK)
							// level: 1=somewhat, 2=very suspicious
							break;
						case 's':	// word bounding box (WBB)
							// output current word
//System.out.println("\t\tword "+sb.toString().trim()+", bbox="+Rectangles2Dpretty(wbbox));
							addWord(zone, sb.substring(0), wbbox, y);
							charcnt += sb.length();
							sb.setLength(0);

							// next word has this bbox
							wbbox = new Rectangle(vals[0],vals[1], vals[2],vals[3]);
//System.out.println("\tword bbox = "+wbbox);
							break;
						case 'r':	// word marker (WMARK)
							// flag: 2=corrected by dict, 3=corrected user dict, 5=no in any dict, 7=corrected by heuristic
							break;
						case 't':	// alternate character choice (ACC)
							// ASCII value of one choice
							// confidence
							break;
						case 'u':	// chosen characer confidence level (CCCL)
							// confidence
							break;
						default:
							// unknown escape code
							//System.err.println("unknown escape code "+c);
							assert false: (char)c;
						}
					}
				}
//System.out.println("\tword = "+sb.toString());
//if (wcnt++>10) System.exit(0);
			}
			addWord(zone, sb.substring(0), wbbox, y);	// final word not forced by anything else
			charcnt += sb.length();


			break;

		default:
			assert false: zonetype;
			// error -- unknown region type
		}
	  }


	  if (DEBUG) System.out.println("returning root of OCR = "+root);
//System.exit(0);
	  return pda;

	} catch (IOException e) {
	  throw new ParseException("I/O error "+e, 0/*could keep track of line number*/);

	} finally {
	  is_.close();
	}
  }
}
