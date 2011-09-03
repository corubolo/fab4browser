package multivalent.std.adaptor.pdf;

import static multivalent.IDInfo.Confidence.*;
import static multivalent.std.adaptor.pdf.COS.CLASS_ARRAY;
import static multivalent.std.adaptor.pdf.COS.CLASS_DICTIONARY;
import static multivalent.std.adaptor.pdf.COS.CLASS_NAME;
import static multivalent.std.adaptor.pdf.COS.CLASS_STRING;
import static multivalent.std.adaptor.pdf.COS.OBJECT_NULL;
import static multivalent.std.adaptor.pdf.COS.array2Rectangle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.pt.awt.NFont;
import com.pt.awt.font.NFontType0;
import com.pt.doc.PostScript;
import multivalent.Behavior;
import multivalent.Browser;
import multivalent.CLGeneral;
import multivalent.Context;
import multivalent.Document;
import multivalent.IDInfo;
import multivalent.IDInfo.Confidence;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.MediaAdaptor;
import multivalent.Node;
import multivalent.ParseException;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.StyleSheet;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.node.Fixed;
import multivalent.node.FixedI;
import multivalent.node.FixedIClip;
import multivalent.node.FixedIHBox;
import multivalent.node.FixedLeafBlock;
import multivalent.node.FixedLeafImage;
import multivalent.node.FixedLeafShape;
import multivalent.node.FixedLeafUnicode;
import multivalent.node.FixedLeafUnicodeKern;
import multivalent.node.LeafText;
import multivalent.node.LeafUnicode;
import multivalent.std.span.StrokeSpan;
import org.apache.commons.io.IOUtils;
import phelps.awt.color.ColorSpaceCMYK;
import phelps.lang.Booleans;
import phelps.lang.Doubles;
import phelps.lang.Integers;

import uk.ac.liverpool.fab4.Fab4utils;

import java.awt.*;
import java.awt.color.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static multivalent.IDInfo.Confidence.*;
import static multivalent.std.adaptor.pdf.COS.*;

/**
        Parse a page of PDF and display with Java 2's 2D API.


        <h3 id='doctree'>Document Tree Construction</h3>

        <p>The PDF content stream is translated into a Multivalent document tree as follows.
        The tree is live: reformat.  Objects drawn as appear in content stream, which usually but not necessarily follows reading order,
        To see the document tree for any particular PDF page, turn on the Debug switch in the Help menu, then select Debug/View Document Tree.

        <ul>
        <li>Text blocks (<code>BT</code>..<code>ET</code>) have subtrees rooted at a {@link FixedI} with name "text".
        Under that can be any number of lines, which collect text that have been determined to share the same baseline in {@link FixedIHBox}s named "line".
        (Some PDF generators generate an inordinate number of BT..ET blocks, as for instance on version of pdfTeX generated a block
        for each dot in a table of contents between header and page number, but most generators use for meaningful blocks of text.)
        PDF text streams are normalized to word chunks in {@link FixedLeafUnicodeKern}s, with special kerning between letters, whether from TJ or Tz or small TD/TM/...,  stored in the leaf.
        Text is translated into Unicode, from whatever original encoding (Macintosh, Macintosh Expert, Windows, PDF, Adobe Standard).  However, if the encoding is nonstandard and found only in font tables, it is not translated.
        Text content is available from the node via {@link Node#getName()}.

        <li>Images are stored in {@link FixedLeafImage}s.  The {@link java.awt.image.BufferedImage} is available via {@link multivalent.node.LeafImage#getImage()},
        and the image's colorspace via {@link java.awt.image.BufferedImage#getColorModel()}.
        Images from XObjects have the reference <code>/Name</code> as the GI,
        and inline images (<code>BI</code>..<code>ID</code>..<code>EI</code>) have the GI "[inline]".

        <li>Paths are {@link FixedLeafShape}s, with fill and stroke flags.
        Paths are kept as simple {@link java.awt.geom.Line2D} with GI "line" or {@link java.awt.Rectangle} with GI "rect" if possible, else {@link java.awt.geom.GeneralPath} with GI "path".
        , paths as Rectangle "rect" if possible, else "line", else GeneralPath "path",

        <li>For all leaf types (text, image, path), positioning is available from {@link Node#bbox},
        but the command positioning it there (<code>cm</code>, <code>Td</code>, ...) is not maintained.
        Transformation matrices (<code>cm</code>, <code>Tm</code>) are reflected in final sizes and not maintained as separate objects.

        <li>Colors are maintained as {@link SpanPDF}s, and all colors are translated into RGB.
        Fonts (family, size, style), text rise (<code>Ts</code>), text rendering mode (<code>Tr</code>) are all maintained as {@link SpanPDF}s.
        Other attributes (line width, line cap style, line join style, miter limit, dash array, ...) are all maintained as {@link SpanPDF}s
        such that if several change at once they are batched in same span and if any of the group changes a new span is started,
        which means that only one span for these attributes is active at any point.
        Sometimes a PDF generator produces redundant color/font/attribute changes (pdfTeX sets the color to <code>1 1 1 1 K</code> and again immediately to <code>1 1 1 1 K</code>)
        or useless changes (e.g., setting the color and then setting it to something else without drawing anything) --
        all redundent and useless changes are optimized away.

        <li>Marked points (<code>MP</code>/<code>DP</code>) are {@link Mark}s, with the point name as the Mark name.
        Marked regions (<code>BMC</code>/<code>BDC</code>..<code>EMC</code>) are simple {@link multivalent.Span}s, with the region name as the Span name and with any region attributes in span attributes.

        <li>Clipping regions (<code>W</code>/<code>W*</code>) are {@link FixedIClip}.
        Clipping regions cannot be enlarged (push the clip onto the graphics stack with <code>q</code>..<code>Q</code> to temporarily reduce it),
        but some PDF generators don't know this:  useless clipping changes are optimized away.

        <li>Shading patterns are {@link FixedLeafShade}.

        <li>If a large filled rectangle appears before any other drawing, its color extracted as the page background and put into the {@link Document} {@link StyleSheet}.

        <li>If the PDF is determined to be a scanned paper and has OCR (but hasn't replaced text with outline fonts), it is transformed.
        OCR text (which is drawn in invisible mode <code>Tr 3</code> or overdrawn with image)
        is associated with the corresponding image fragment and transformed into {@link multivalent.node.FixedLeafOCR}, and the independent image os removed.
        (This allows hybrid image-OCR PDFs to work as expected with other behaviors, such as select and paste and the Show OCR lens.)

        <li>Annotations such as hyperlinks, are sent as semantic events with message {@link Anno#MSG_CREATE}.
        Other behaviors can translate them into entities on the document tree, often spans.

        <!--li>PDF commands are not maintained explicitly: q..Q, BX..EX, comments, different text drawing commands (Tj, TJ, ', "), changes in transformation matrices (cm, Tm), Form XObject interpolated, -->

        </ul>


        <h3>See Also</h3>
        <ul>
        <li>{@link PDFReader}
        <li>{@link PDFWriter} to write new PDF data format from Java data structures
        </ul>

        @version $Revision: 1.156 $ $Date: 2005/05/01 03:32:32 $
*/
public class PDF extends MediaAdaptor {
  /*package-private*/ static final boolean DEBUG = false && multivalent.Meta.DEVEL;
  //static final boolean PERF = DEBUG;  //true; // performance testing flag
  private static boolean Dump_ = false;
  //public static final boolean DUMP = true;
  //int lcnt_ = 0;

  /** Message "pdfSetGoFast": faster rendering if sometimes less accurate: arg=boolean or null to toggle. */
  public static final String MSG_GO_FAST = "pdfSetGoFast";


  /** Message of semantic event to jump to a page (by number, name, ...) as specified by the event's <tt>arg</tt> field.
  public static final String MSG_GOTO = "pdfGoto";      // WRONG -- subset of action
*/

  /** Message of semantic event to set the user password so encrypted files can be read, with the password String passed in <tt>arg</tt>. */
  public static final String MSG_OWNER_PASSWORD = "pdfUserPassword";
  /** Message of semantic event to set the owner password so encrypted files can be read, with the password String passed in <tt>arg</tt>. */
  public static final String MSG_USER_PASSWORD = "pdfOwnerPassword";
  /** Message of semantic event to control dumping of uncompress and decrypted content stream to temporary file. */
  public static final String MSG_DUMP = "pdfDump";

  /**
        Optional content groups stored in {@link Document} under this key.
        The value there is a {@link java.util.Map} with names of optional content groups as keys and {@link #OCG_ON} and {@link #OCG_OFF} as values.
  */
  public static final String VAR_OCG = "PDFOptionalContentGroups";
  public static final String OCG_ON = "ON";
  public static final String OCG_OFF = "OFF";


  /** Metadata that may be in PDF and is useful to Multivalent. */
  private static final /*const*/ String[] METADATA = { "Author", "Title", "Keywords", "Subject", "Producer", "Creator" };

  static final String BLANK_PAGE = "";  // or "This page intentionally left blank"

  //static final double PIXEL_INVISIBLE = 0.1; => antialiasing

  private static final boolean[] WHITESPACE= PostScript.WHITESPACE, WSDL=PostScript.WSDL, OP=PDFReader.OP;


  static {      // Java's settings are same as PDF's, but verify
        assert BasicStroke.CAP_BUTT==0 && BasicStroke.CAP_ROUND==1 && BasicStroke.CAP_SQUARE==2;
        assert BasicStroke.JOIN_MITER==0 && BasicStroke.JOIN_ROUND==1 && BasicStroke.JOIN_BEVEL==2;
  }

  static Map<String,Integer> streamcmds_;
  static {
        String[] cmds = (       // commands are case sensitive
                "w/1 J/1 j/1 M/1 d/2 ri/1 i/1 gs/1   q/0 Q/0 cm/6   m/2 l/2 c/6 v/4 y/4 h/0 re/4   S/0 s/0 f/0 F/0 f*/0 B/0 B*/0 b/0 b*/0 n/0   W/0 W*/0"
                + " BT/0 ET/0   Tc/1 Tw/1 Tz/1 TL/1 Tf/2 Tr/1 Ts/1   Td/2 TD/2 Tm/6 T*/0   Tj/1 TJ/1 '/1 \"/3   d0/2 d1/6"
                + " CS/1 cs/1 SC/+ SCN/+ sc/+ scn/+ G/1 g/1 RG/3 rg/3 K/4 k/4   sh/1   BI/0 ID/0 EI/0   Do/1"
                + " MP/1 DP/2 BMC/1 BDC/2 EMC/0   BX/0 EX/0"
                + " %/0"
                + " PS/1"       // obsolete
                ).split("\\s+");
        streamcmds_ = new HashMap<String,Integer>(cmds.length *2);
        for (int i=0,imax=cmds.length; i<imax; i++) {
                String token = cmds[i];  assert streamcmds_.get(token)==null && token.length()>=1+2 && token.length()<=3+2: token;
                int x = token.indexOf('/');
                int arity = token.charAt(x+1)=='+'? Integer.MAX_VALUE: token.charAt(x+1)-'0';   // all arity single digit
                token = token.substring(0,x);
                assert "n".equals(token) || !token.startsWith("n");     // simple lookahead to see if need to copy clipping path in W/W*
                assert !Character.isDigit(token.charAt(0));     // simpler paths if lookahead not a point
                streamcmds_.put(token, Integers.getInteger(arity));
        }
  }

  /** Go fast or be exactly correct. */
  public static /*NOT final*/ boolean GoFast = true;


  //int pageheight_, pagewidth_;        // have to convert y-coords from PDF's from bottom-up to Java's from top-down
  private PDFReader pdfr_ = null;

  // per-page variables
  private Rectangle mediabox_, cropbox_;
  private AffineTransform ctm_ = null;  // zoom + rotate, as for annotations
  private Map<String,Object> form_ = null;
  /** If encounter error, exception message placed here. */
  private String fail_ = null;
  private boolean idCheck_ = false;


  public void setReader(PDFReader pdfr) throws IOException {
        if (pdfr_!=null) throw new IOException("Reader already set");
        pdfr_ = pdfr;
  }
  public PDFReader getReader() { return pdfr_; }

  public boolean isAuthorized() {return getReader().isAuthorized();}
  public void setPassword(String pw) {
        try { getReader().setPassword(pw); } catch (IOException doesntfitintoapi) {}
  }

  public Rectangle getMediaBox() { return new Rectangle(mediabox_); }
  public Rectangle getCropBox() { return new Rectangle(cropbox_); }
  // make available to annotations
  public AffineTransform getTransform() { return new AffineTransform(ctm_); }

  /**
        Returns interactive from as {@link java.util.Map} with keys the fully qualified.
        This Map represents the current settings of the form, as modified by the user or by PDF actions (reset, import) or programmatically.
  */
  public Map<String,Object> getForm() throws IOException {
        if (form_==null) form_ = Forms.export(getReader());
        return form_;
  }



//+ Content stream: readObject(in), eatSpace(in), getDoubles(), getFloats(), buildPage, buildStream, parse

  /** Helper method used in parsing content stream. */
  /*package-private*/ static void getDoubles(Object[] ops, double[] d, int cnt) {
        assert ops!=null && d!=null && cnt>0 && cnt <= d.length;
        // if (d==null) d=new double[cnt];
        for (int i=0; i<cnt; i++) d[i] = ((Number)ops[i]).doubleValue();
        //return d;
  }

  /** Used by color space creation and color value filling. */
  /*package-private*/ static void getFloats(Object[] ops, float[] f, int cnt) {
        assert ops!=null && f!=null /*&& cnt>0 -- pattern*/ && cnt <= f.length;
        for (int i=0; i<cnt; i++) f[i] = ((Number)ops[i]).floatValue(); // never seen an IRef
  }

  private static void clip(float[] cscomp, ColorSpace cs) {
        for (int i=0, imax=cs.getNumComponents(); i<imax; i++) {
                float val = cscomp[i];
                if (val < cs.getMinValue(i)) cscomp[i] = cs.getMinValue(i);
                else if (val > cs.getMaxValue(i)) cscomp[i] = cs.getMaxValue(i);
        }
  }

  /**
        Parse content stream of operators for <var>pagenum</var> into document tree.
        Pages are numbered 1 .. {@link #getPageCnt()}, inclusive.
        See PDF Reference 1.4, page 134.
        Colors and fonts are transformed into Span's.
        @param ctm  initial transform with scaling/zoom, to which the method adds page rotation and conversion to Java coordinates (Y goes down)
  */
  void buildPage(int pagenum, INode pageroot, AffineTransform ctm/*initial zoom*/) throws IOException, ParseException {
        PDFReader pdfr = pdfr_;
        assert pagenum>=1 && pagenum <= pdfr.getPageCnt(): pagenum+ " >= "+pdfr.getPageCnt()+" (1-based)";
        assert pageroot!=null;

        if (false && multivalent.Meta.MONITOR) System.out.print(pagenum+".  ");
        Dict page = pdfr.getPage(pagenum);


        // rotate
        Number rotate = (Number)page.get("Rotate");     // clockwise(!), multiple of 90
        int rot = rotate==null? 0: rotate.intValue() % 360;  if (rot<0) rot+=360;  assert rot%90==0 && rot>=0 && rot<360;
        ctm.rotate(Math.toRadians(-rot));
//System.out.println("rot="+rot+", ctm="+ctm);

        mediabox_ = array2Rectangle((Object[])pdfr.getObject(page.get("MediaBox")), ctm);       // mediabox in zoomed, rotated space
        int pw = mediabox_.width, ph = mediabox_.height, dx = mediabox_.x < 0? -mediabox_.x: 0, dy = mediabox_.y < 0? -mediabox_.y: 0;
        cropbox_ = page.get("CropBox")!=null? array2Rectangle((Object[])pdfr.getObject(page.get("CropBox")), ctm): new Rectangle(mediabox_);
//System.out.println("/Rotate "+rot+", /MediaBox "+mediabox_+", /CropBox "+cropbox_);

        cropbox_.setLocation(Math.max(cropbox_.x, mediabox_.x), Math.max(cropbox_.y, mediabox_.y));
        cropbox_.setSize(Math.min(cropbox_.width, pw - (cropbox_.x - mediabox_.x)), Math.min(cropbox_.height, ph - (cropbox_.y - mediabox_.y)));
//System.out.println("\tintersection "+pw+"x"+ph+" => "+cropbox_);

        cropbox_.translate(dx,dy);
//System.out.println("\ttranslate=> "+cropbox_);
        //if (rot==90 || rot==180) cropbox_.y += cropbox_.height;
        //cropbox_.y = Math.max(cropbox_.y, ph - cropbox_.y - cropbox_.height); // fix bad PDF
//System.out.println("\tto UY=> "+cropbox_);
        ctm.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));       // move to Q1
//System.out.println("\t/CropBox "+cropbox_+", "+AffineTransform.getTranslateInstance(dx,dy));

        // transform PDF coordinates (in /MediaBox space) to Java coordinates
//System.out.println("dy = "+cropbox_.y+" + "+cropbox_.height+" = "+(cropbox_.y+cropbox_.height));
        //int bottom = cropbox_.y - (rot==0||rot==270? 0: cropbox_.height);     // PDF space
        int bottom = Math.min(cropbox_.y, ph - cropbox_.y - cropbox_.height);   // + fix bad PDF
//System.out.println("\tbottom = "+bottom);
        AffineTransform pdf2java = new AffineTransform(1.0, 0.0, 0.0, -1.0, -cropbox_.x, ph - bottom);
        ctm.preConcatenate(pdf2java);
        cropbox_.setLocation(0,0);      // normalize to (0,0) -- cropbox outside of any rotation
//System.out.println("cropbox "+cropbox_+", xform="+ctm);

        ctm_ = new AffineTransform(ctm);

        GraphicsState gs = new GraphicsState(); gs.ctm = ctm;

        Object o = page.get("Contents");        // null if empty page
        InputStreamComposite in = o!=null? pdfr.getInputStream(o, true): null/*or phelps.InputStreams.DEVNULL*/;        // can be IRef or Object[] of IRef's
//System.out.println("building page "+pagenum);

        if (Dump_ && in!=null) try {
                System.out.println("Contents dict = "+o+"/"+pdfr.getObject(o));
                File tmpf = File.createTempFile("pdf", ".stream");
                tmpf.deleteOnExit();
                FileOutputStream out = new FileOutputStream(tmpf);
                for (int c; (c=in.read())!=-1; ) out.write(c);  // test char at a time so know exact point of failure
                //byte[] buf = new byte[4 * 1024];
                //int len = 0;
                //while (true) { int hunk=in.read(buf); if (hunk>0) { out.write(buf, 0, hunk); len+=hunk; } else if (hunk==-1) break; } // test block read
                in.close(); out.close();
                System.out.println("wrote PDF content "+pdfr.getPageRef(pagenum)+" to "+tmpf/*+".   len="+len*/);       // +" vs /Length="+pdfr.getObject(((HashMap--can be array)pdfr.getObject(o)).get("Length")));

                in = pdfr.getInputStream(o, true);      // restore eaten up stream
        } catch (IOException ignore) { System.err.println("error writing stream: "+ignore); }

        List<FixedLeafImage> ocrimgs = new ArrayList<FixedLeafImage>(10);       // collect FAX fragments for possible use in OCR
        if (in!=null) {
                Rectangle clipshape = new Rectangle(0,0, cropbox_.width,cropbox_.height);       // forcefully crop everything to cropbox
                //clipshape.translate(-clipshape.x,-clipshape.y);       // already at (0,0)
                FixedIClip clipp = new FixedIClip("crop", null, pageroot, clipshape, clipshape/*new Rectangle(0,0,cropbox_.width,cropbox_.height)*/);

                try {
//System.out.println("buildStream "+page+" "+clipp+" "+ctm+" "+in+" "+ocrimgs);
                        buildStream(page, clipp/*pageroot*/, gs, in, ocrimgs);
                } catch (IOException ioe) { throw ioe;
                } catch (ParseException/*, NumberFormat, ...*/ pe) {
                        if (DEBUG) { pe.printStackTrace(); System.exit(1); }
                        //throw new ParseException("corrupt content stream: "+pe.toString());
                        throw pe;
                } finally {
                        in.close();
                }
        }

//for (int i=0,imax=hist.length; i<imax; i++) if (hist[i].cnt>0) System.out.print(((char)i)+" "+hist[i]+"   ");  System.out.println();
        //if (DEBUG) Span.dumpPending();        // should be empty list (except in embedded form)

        //if (pageroot.size()==0) new FixedLeafUnicode(BLANK_PAGE,null, pageroot); => have clipping region at least
        if (pageroot.getFirstLeaf()==null) { pageroot.removeAllChildren(); new FixedLeafUnicode(BLANK_PAGE,null, pageroot); }
        assert checkTree("content stream", pageroot);

        OCR.extractBackground(pageroot, this);
        if (pageroot.size()==0) new FixedLeafUnicode("",null, pageroot);
        assert checkTree("bg", pageroot);

        OCR.transform(pageroot, ocrimgs, this, getBrowser());
        assert ocrimgs.size()==0 || checkTree("OCR", pageroot);

        createAnnots(page, pageroot);
        assert page.get("Annots")==null || checkTree("annos", pageroot);
  }



  /**
        Used by buildPage(), recursively by Form XObject, and by Type 3 fonts.
        <!--
        This is a very long method (over 1000 lines), but it's awkward to break parts into their own methods
        as for most PDF commands there is a lot of state to pass and not much computation on it.
        -->

        @return number of commands processed
  */
  /*package-private*/ int buildStream(Dict page, FixedIClip clipp/*INode pageroot*/, GraphicsState gs, InputStreamComposite in, List<FixedLeafImage> ocrimgs) throws IOException, ParseException {
        PDFReader pdfr = pdfr_;

        // PDF state
        AffineTransform ctm = gs.ctm;
        Color color=gs.strokecolor, fcolor=gs.fillcolor;
        ColorSpace sCS = gs.sCS, fCS = gs.fCS;

        // gs.fontdict, gs.xxx = current PDF state
        Dict fontdict = gs.fontdict;
        double pointsize = gs.pointsize;
        double Tc=gs.Tc, Tw=gs.Tw, Tz=gs.Tz, TL=gs.TL, Ts=gs.Ts; int Tr=gs.Tr;

        float linewidth = gs.linewidth;
        int linecap = gs.linecap, linejoin = gs.linejoin;
        float miterlimit = gs.miterlimit;
        float[] dasharray = gs.dasharray; float dashphase = gs.dashphase;

        List<GraphicsState> gsstack=new ArrayList<GraphicsState>(10);
        AffineTransform Tm=new AffineTransform(), Tlm=new AffineTransform(), tmpat=new AffineTransform();//, Trm=null;
        List<Span> markedseq = new ArrayList<Span>(5);  // can be nested
        double curx=0.0, cury=0.0;      // current point


        Object o = pdfr.getObject(page.get("Resources"));
        Dict resources = o!=null && CLASS_DICTIONARY==o.getClass()? (Dict)o: new Dict(1),       // "Resources" required but can have content that doesn't need one
         xores = (Dict)pdfr.getObject(resources.get("XObject")),
         fontres = (Dict)pdfr.getObject(resources.get("Font")),
         csres = (Dict)pdfr.getObject(resources.get("ColorSpace")),
         patres = (Dict)pdfr.getObject(resources.get("Pattern")),
         shres = (Dict)pdfr.getObject(resources.get("Shading")),
         propres = (Dict)pdfr.getObject(resources.get("Properties"));
//System.out.println("resources = "+page.get("Resources")+" / "+resources);


        // Multivalent state
        Document doc = clipp/*pageroot*/.getDocument(); // null for Type 3
        Layer scratchLayer = doc!=null? doc.getLayer(Layer.SCRATCH): null;
        //Point2D srcpt=new Point2D.Double(), transpt=new Point2D.Double();
        double[] d = new double[6];
        //X INode p = pageroot; // a tiny bit of hierarchy according to graphics state push/pop => push/pop done randomly
//System.out.println("cropbox = "+cropbox_);

        INode textp = null;
        FixedIHBox linep = null;
        double baseline = Double.MIN_VALUE;
        NFont tf = null; NFontType0 t0=null;
        Object[] Tja = new Object[1];
        char spacech = NFont.NOTDEF_CHAR;
        Point2D spaceadv = new Point2D.Double(0.0,0.0); double spacew = 0.0;    // width of space character in Tm coordinates, and in pixels
        Rectangle2D maxr = null;
        double lastX=0.0, totalW=0.0;   // right edge of last text hunk, and accurate word width without accumulation of rounding error
        boolean/*FixedLeafUnicode*/ fconcat = false;    // additional condition for concatenating (set to true after characters, false after spaces)

        Object[] ops = new Object[6];   // name, string, array.  cm, Tm, d1 need 6; SC, SCN, sc, scn variable but <= 5 (CMYK + 1).
        int opsi=0;
        GeneralPath path = new GeneralPath();
        //boolean fcompat = false;  BX..EX have no function
        float[] cscomp = new float[5];  // up to 4 for CMYK => seen 5 in Altona_Technical_x3.pdf given to Function to product CMYK

        SpanPDF fontspan=null, sspan=null, fillspan=null, Trspan=null;  // make sure that build in paint order so never need to swap endpoints, even if is drawn bottom-up
        StrokeSpan strokespan=null;
        Node lastleaf = clipp/*pageroot*/;//.getLastLeaf(); if (lastleaf==null) lastleaf=pageroot;      // set to last leaf created, but maybe no leaves when start or maybe in /Form
        boolean fnewfont=true, fnewline=true, fpop=false;       // flag (possible) change in xxx state.  E.g., if font family or size change, collect all changes at next text drawing, and likewise for line attribute changes
        boolean fstroke=false, ffill=false;
        boolean fvalidpath=false;
        Color newcolor=color, newfcolor=fcolor;
//System.out.println("buildStream fill color = "+newfcolor+", lastleaf="+lastleaf);
        int newTr = Tr;
        int pendingW = -1;

        Rectangle pathrect = null;      // if shape is simple rectangle, use it rather than more complex GeneralPath.  would like to query GeneralPath, but added shaped immediately flattened into segments.
        Line2D pathline = null;
        //boolean lastS = false, firstS=true;

        boolean fshow = (HINT_NO_SHOW & getHints()) == 0,
                fshape = (HINT_NO_SHAPE & getHints()) == 0, //fimage = (HINT_NO_IMAGE & getHints()) == 0,
                fexact = (HINT_EXACT & getHints()) != 0;

        // metrics
        int cmdcnt=0, hunkcnt=0, leafcnt=0, spancnt=0, vspancnt=0, concatcnt=0;
        int pathcnt=0, pathlen=0; int[] pathlens=new int[5000];
        long start = System.currentTimeMillis();


        // Pushes tokens onto stack, until operator, which uses operaands and clears stack.
        pdfr.eatSpace(in);
        for (int c, peek=-1; (c=in.peek())!=-1; ) {
        //for (int ccnt=0, pcnt=0; true; ccnt++, pcnt++) {
                //if (pcnt > 5000 && clipp.size()>0) { pcnt=0; System.out.println(ccnt); br.repaintNow(); }
                if (OP[c]) {    // OPERAND
                        if (opsi >= 6) throw new ParseException("too many operands: "+ops[0]+" "+ops[1]+" ... "+ops[5]+" + more");
                        ops[opsi++] = pdfr.readObject(in);
if (DEBUG) System.out.print(ops[opsi-1]+" ");

                } else {        // OPERATOR
                        c=in.read();    // only peek() above
                        int c2=in.read(), c3=-1, c2c3;  // second and third characters
                        if (c2==-1 || WSDL[c2] || c=='%') { peek=c2; c2c3=' '; }        // normalize whitespace AND delimiter immediately following BOTH to space
                        else if ((c3=in.read())==-1 || WSDL[c3]) { peek=c3; c2c3=c2; }
                        else { c2c3 = (c2<<8)+c3; peek=in.read();
                                if (peek!=-1 && !WSDL[peek]) {
                                        if (c=='e' && c2=='n' && c3=='d' && peek=='s') break;   // "ends[tream]" -- assume edited uncompressed command stream to make it shorter
                                        else throw new ParseException("bad command or no trailing whitespace "+(char)c+(char)c2+(char)c3+" + "+peek);
                                }
                        }

                        cmdcnt++;       //if (++hunkcnt == 5000) { br.repaint(); hunkcnt=0; }
                        if (DEBUG) {    // => when user is running, want to let fail to an Exception, so caller can catch (can't catch failed assertions)
                        StringBuffer scmd = new StringBuffer(3); scmd.append((char)c); if (c2c3!=' ') { scmd.append((char)c2); if (c2c3!=c2) scmd.append((char)c3); }
//for (int i=0; i<opsi; i++) System.out.print(" "+ops[i]);
System.out.println("  "+scmd);
                        Integer arity = streamcmds_.get(scmd.toString());
                        boolean ok = arity!=null && (arity.intValue()==opsi || (arity.intValue()==Integer.MAX_VALUE && opsi>0));
                        if (!ok) {
                                System.out.print((arity==null? "unknown command": ("bad arity "+opsi+" not "+arity))+": |"+scmd+"| ["+c+" "+c2+"] ");
                                for (int i=0; i<opsi; i++) System.out.println("\t"+ops[i]);
                                if (DEBUG) assert false;
                                return cmdcnt;
                        }
                        }
                        if (c!='%'/*"%\n" bad*/) while (peek!=-1 && WHITESPACE[peek]) peek=in.read();   // not PDFReader.eatSpace(in) here
                        in.unread(peek);



                        switch (c) {
                        case 'B':       // B, B*, BT, BI, BMC, BDC, BX
                        if (c2c3==' ') {        // -- 'B' - fill and then stroke the path, using the nonzero winding number rule
                                // NO path.closePath();
                                path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                                ffill = fstroke = true;

                        } else if (c2c3=='*') { // -- 'B*' -- fill and then stroke the path, using the even-odd rule
                                // NO path.closePath();
                                path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                                ffill = fstroke = true;

                        } else if (c2c3=='T') { // -- 'BT' -- begin text object
                                // if new BT..ET very very close to last, don't start new block (pdfTex-0.13d very inefficient this way)
                                if (clipp.size()>0 && textp == clipp.getLastChild() && Math.abs(ctm.getTranslateX() - Tm.getTranslateX()) < 5.0/*pixels*/ && Math.abs(ctm.getTranslateX() - Tm.getTranslateX()) < 0.001) {      // same baseline and close in X
                                        //System.out.print(" B"+Math.abs(ctm.getTranslateX() - Tm.getTranslateX()));    // should happen rarely, except on pdfTex table of contents

                                } else {        // assume new BT..ET means new structural block of text, although not necessarily true
                                        textp = new FixedI("text"/*+lcnt_++*/,null, clipp);
                                        linep = new FixedIHBox("line",null, textp); fconcat=false;
                                        baseline = Double.MIN_VALUE;
                                        //fontspan = new SpanPDF... => NO, not necessarily a Tf in each BT
                                }

                                fnewfont = true;        // TS-1175.pdf
                                Tm.setTransform(ctm);   // concat with identity...
                                Tlm.setTransform(Tm);


                        } else if (c2c3=='I') { // 'BI' - begin inline image
                                BufferedImage img = Images.createInline(in, csres, ctm, newfcolor, pdfr);
                                lastleaf = appendImage("[inline]",clipp, img, ctm); leafcnt++;
//System.out.println("inline img "+lastleaf.getBbox()+" "+ctm);

//System.out.println("image @ x,"+ctm.getTranslateY()+", height="+img.getHeight()+", scale="+ctm.getScaleY());

                        } else if (c2c3==('M'<<8)+'C'   //  tag 'BMC' - begin marked-content sequence
                                        || c2c3==('D'<<8)+'C') {        // tag properties 'BDC' - begin marked-content sequence
//System.out.println("marked: "+ops[0]+", "+ops[1]+" @ "+lastleaf);
                                Dict attrs = null;
                                if (c2=='D') attrs = (Dict)(CLASS_DICTIONARY==ops[1].getClass()? ops[1]: pdfr.getObject(propres.get(ops[1])));

                                Span seq = (Span)Behavior.getInstance((String)ops[0], "multivalent.Span", (Map)attrs, scratchLayer);
                                seq.open(lastleaf);
                                markedseq.add(seq);     // set start point

                        } else if (c2c3=='X') { // -- 'BX' - begin a compatibility section: don't report unrecognized operators
                                //fcompat = true;
                        }
                        break;

                        case 'b':       // b, b*
                        if (c2c3==' ') {        // -- 'b' - close, fill, and then stroke the path, using the nonzero winding number rule
                                assert fvalidpath: "b";
                                if (fvalidpath) {       // error to closePath() if empty
                                        if (pathrect==null) path.closePath();
                                        path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                                        ffill = fstroke = true;
                                }

                        } else if (c2c3=='*') { // -- 'b*' - close, fill, and then stroke the path, using the even-odd rule
                                assert fvalidpath: "b*";
                                if (fvalidpath) {
                                        if (pathrect==null) path.closePath();
                                        path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                                        ffill = fstroke = true;
                                }
                        }
                        break;

                        case 'C':       // CS
                        if (c2c3=='S') {        // name 'CS' - set the color space to use for stroking operations
                                sCS = pdfr.getColorSpace(ops[0], csres, patres); assert sCS!=null: "CS stroke "+pdfr.getObject(ops[0])+" in "+csres;
                        }
                        break;

                        case 'c':       // c, cm, cs
                        if (c2c3==' ') {        // (curpt-x, y-curpt) x1 y1 x2 y2 x3 y3 'c' - append a cubic B�zier curve to the current path: current point to x3 y3, with x1 y1 and x2 y2 as control points
                                getDoubles(ops,d,6); ctm.transform(d,0, d,0, 3);
                                /*if (pathlen==1 && peek=='S') simplepath = new CubicCurve2D.Double(d[0],d[1], d[2],d[3], d[4],d[5]);   // rare, so don't optimize
                                else*/ path.curveTo((float)d[0],(float)d[1], (float)d[2],(float)d[3], (float)(curx=d[4]),(float)(cury=d[5]));
                                pathlen+=100;   // probably more efficient than above

                        } else if (c2c3=='m') { // a b c d e f 'cm' - concatenate matrix to CTM
                                getDoubles(ops,d,6); tmpat.setTransform(d[0], d[1], d[2], d[3], d[4], d[5]);
                                if (!tmpat.isIdentity()) {
                                        ctm.concatenate(tmpat);
                                        if (tmpat.getType()!=AffineTransform.TYPE_TRANSLATION) fnewfont = true;
                                        // should recompute stroke attributes too
                                }

                        } else if (c2c3=='s') { // name 'cs' - set the color space to use for nonstroking operations
                                fCS = pdfr.getColorSpace(ops[0], csres, patres); assert fCS!=null: "cs fill "+pdfr.getObject(ops[0])+" in "+csres;
                        }
                        break;

                        case 'D':       // Do, DP
                        if (c2c3=='o') {        // name 'Do' -- paint XObject
                                GraphicsState gs2 = gs;
                                String xname = (String)ops[0];
                                Dict xobj = (Dict)pdfr.getObject(xores.get(xname));
                                if (xobj!=null && "Form".equals(pdfr.getObject(xobj.get("Subtype")))) {
                                        gs2 = new GraphicsState(gs);
                                        gs2.ctm = ctm;
                                        gs2.strokecolor = newcolor; gs2.fillcolor = newfcolor; gs2.sCS=sCS; gs2.fCS=fCS;
                                }
                                Leaf l = cmdDo(xname, xores, resources,  gs2, clipp,  d, ocrimgs);
                                if (l!=null) { lastleaf=l; leafcnt++; }

                        } else if (c2c3=='P') { // tag properties 'DP' - marked content point (see 'MP')
                                // maybe have "Mark extends VObject" so can stuff properties
                                if (lastleaf.isLeaf()) new Mark((Leaf)lastleaf, lastleaf.size());       // marked point lost if occurs before any content
                        }
                        break;

                        case 'd':       // d, d0, d1
                        if (c2c3==' ') {        // dash-array dash-phase 'd' - dash line (p.155; "[]0" or "[]n" to stop dashed, return to solid)
                                Object[] oa = (Object[])ops[0];
                                if (oa==OBJECT_NULL || oa.length==0) gs.dasharray = null; else getFloats(oa, gs.dasharray=new float[oa.length], oa.length);
                                gs.dashphase = ((Number)ops[1]).floatValue();
                                fnewline = true;

                        } else if (c2c3=='0') { // wx wy 'd0' - set glyph width, and declare that color specified too
                                //clipp.bbox.width = ((Number)ops[0]).intValue() /* ctm.getScaleX()*/;  // unscaled in backdoor communication so Type 3 font can get more accurate, non-truncated-to-int value
                                //fType3 = true;

                        } else if (c2c3=='1') { // wx wy llx lly urx ury 'd1' - set glyph width and bounding box, and declare that color not specified
                                //clipp.bbox.width = ((Number)ops[0]).intValue();
                                //clipp.bbox.height = (int)((((Number)ops[5]).doubleValue() /*- ((Number)ops[3]).doubleValue()*/) * ctm.getScaleY() /*+ 0.5--consistent with scaling*/);        // backdoor for baseline... need float
                                //clipp.bbox.height = (int)((((Number)ops[5]).doubleValue() /*- ((Number)ops[3]).doubleValue()*/) * ctm.getScaleY() /*+ 0.5--consistent with scaling*/);        // backdoor for baseline... need float
                                //if (clip.bbox.height == 0) ...        // GhostScript 5.10 doesn't treat as ascent
                                //fType3 = true;
                        }
                        break;

                        case 'E':       // ET, EI, EMC, EX
                        if (c2c3=='T') {        // -- 'ET'
                                // text mode paramters retained across BT..ET blocks (font, Ts, Tr, ...)
//System.out.println("textp.size() = "+textp.size()+" @ ET");
                                if (linep.size()==0) {
                                        if (linep!=lastleaf) linep.remove();
                                        else new FixedLeafUnicode("",null, linep).getIbbox().setBounds((int)Math.round(Tm.getTranslateX()), (int)Math.round(Tm.getTranslateY()), 0,0);
                                }
                                //else if (linep.size()==1) { textp.appendChild(linep.childAt(0)); linep.remove(); } -- keep structure even though less efficient (already allocated so only space inefficient)

                                if (textp.size()==0) textp.remove();
                                //else sortY(textp);

                                //textp=null; => recycled if next BT..ET very close

                        } else if (c2c3=='I') { // 'EI' - end inline image -- handled in BI
                                assert false;   // ignore if see out of context

                        } else if (c2c3==('M'<<8)+'C') {        // 'EMC' - end marked-content sequence
                                if (markedseq.size()>0) {
                                        Span seq = markedseq.remove(markedseq.size()-1);
                                        seq.close(lastleaf);
                                }

                        } else if (c2c3=='X') { // -- 'EX' - end a compatibility section
                                //fcompat = false;
                        }
                        break;

                        case 'F':       // F
                                assert c2c3==' ';       // 'F'=deprecated; identical to 'f'
                                // fall through
                        case 'f':       // f, f*
                        if (c2c3==' ') {        // -- 'f' - fill the path, using the nonzero winding number rule
                                path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                                ffill = true;

                        } else if (c2c3=='*') { // -- 'f*' - fill the path, using the even-odd rule to determine
                                path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                                ffill = true;
                        }
                        break;

                        case 'G':       // G
                        if (c2c3==' ') {        // gray 'G' - set DeviceGray color space and gray level for stroking
                                sCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                                float gray=((Number)ops[0]).floatValue(); clip(cscomp, sCS);
                                newcolor = gray==0f? Color.BLACK: gray==1f? Color.WHITE: new Color(gray, gray, gray, 1f);       // gray==0.5f: Color.GRAY... ?
                        }
                        break;

                        case 'g':       // gs, g
                        if (c2c3==' ') {        // gray 'g' - set DeviceGray color space and gray level for filling
                                fCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                                float gray=((Number)ops[0]).floatValue(); clip(cscomp, fCS);
                                newfcolor = gray==0f? Color.BLACK: gray==1f? Color.WHITE: new Color(gray, gray, gray, 1f);

                        } else if (c2c3=='s') { // dictName 'gs' - set the specified parameters in the graphics state.
                                Dict gsdicts = (Dict)pdfr.getObject(resources.get("ExtGState"));
                                Dict gsdict = (Dict)pdfr.getObject(gsdicts.get(ops[0]));
                                cmdgs(gsdict, fontres, ctm, d, gs);

                                if (gsdict.get("Font")!=null) fnewfont = true;
                                fnewline = true;        // too many attributes to check
                        }
                        break;

                        case 'h':       // h
                        if (c2c3==' ') {        // -- 'h' - close the current subpath by appending a straight line segment from the current point to the starting point
                                assert fvalidpath: "h";
                                if (fvalidpath) {
                                        if (pathrect==null) path.closePath();   // rectangles already closed, but have seen "36 464.25 633.75 6.75 re h W n"
                                }// catch (Exception e) { /*trying to close already-closed path*/ }
                        }
                        break;

                        case 'I':       // ID
                        if (c2c3=='D') {        // 'ID' - inline image data (between BI and EI)
                                assert false;   //  handled in BI -- ignore if see out of context
                        }
                        break;

                        case 'i':       // i
                        if (c2c3==' ') {        // 0..100 'i' - flatness tolerance [not settable in Graphics2D]
                                gs.flatness = ((Number)ops[0]).intValue();
                        }
                        break;

                        case 'J':       // J
                        if (c2c3==' ') {        // number 'J' - line cap style
                                gs.linecap = ((Number)ops[0]).intValue();
                                fnewline = true;
                        }
                        break;

                        case 'j':       // j
                        if (c2c3==' ') {        // number 'j' - line join style
                                gs.linejoin = ((Number)ops[0]).intValue();
                                fnewline = true;
                        }
                        break;

                        case 'K':       // K
                        if (c2c3==' ') {        // c m y k 'K' - set the color space to DeviceCMYK for stroking
                                sCS = ColorSpaceCMYK.getInstance();
                                getFloats(ops, cscomp, 4); clip(cscomp, sCS); float r=cscomp[0], g=cscomp[1], b=cscomp[2], k=cscomp[3]; // 'c' m y k - already used as variables -- and cmyk inverse of rgb so not so terrible
                                newcolor = r==0f && g==0f && b==0f && k==0f? Color.WHITE: r+k>=1f && g+k>=1f && b+k>=1f /*&& k==1f*/? Color.BLACK: new Color(sCS, cscomp, 1f);
                        }
                        break;

                        case 'k':       // k
                        if (c2c3==' ') {        // c m y k 'k' - set the color space to DeviceCMYK for nonstroking
                                fCS = ColorSpaceCMYK.getInstance();
                                getFloats(ops, cscomp, 4); clip(cscomp, fCS); float r=cscomp[0], g=cscomp[1], b=cscomp[2], k=cscomp[3];
                                newfcolor = r==0f && g==0f && b==0f && k==0f? Color.WHITE: r+k>=1f && g+k>=1f && b+k>=1f /*&& k==1f*/? Color.BLACK: new Color(fCS, cscomp, 1f);
                        }
                        break;

                        case 'l':       // l
                        if (c2c3==' ') {        // x y 'l' - append a straight line segment from the current point to the point (x, y)
//System.out.println(ops[0]+" "+ops[1]+" l");
                                getDoubles(ops,d,2);
                                //X assert pathline==null: d[0]+" "+d[1]; => m l h m l h ...
                                ctm.transform(d,0, d,0, 1);
//if (lcnt_++ < 10) System.out.println("line to "+srcpt+" / "+transpt);
                                if (pathlen==0) path.moveTo((float)d[0], (float)d[1]);  // error in pdfdb/000125.pdf, generated by PDFWriter 4.0 for Windows
                                else if (pathlen==1/*just 'm'*/ && (((peek=in.peek())<'0' || peek>'9') && peek!='.' && peek!='-'/*jdj200108*/)) pathline = new Line2D.Double(curx,cury, d[0],d[1]);     // JLS 15.7.4 Argument Lists are Evaluated Left-to-Right.  No need to set curx,cury because end of path
                                else path.lineTo((float)(curx=d[0]), (float)(cury=d[1]));
                                pathlen+=1000;
                                //path.reset(); => keep initial 'm' in case bogus closePath
                        }
                        break;

                        case 'M':       // M, MP
                        if (c2c3==' ') {        // number 'M' - miter limit
                                gs.miterlimit = ((Number)ops[0]).intValue();
                                fnewline = true;

                        } else if (c2c3=='P') { // tag 'MP' - marked content point (see 'DP')
                                if (lastleaf.isLeaf()) new Mark((Leaf)lastleaf, lastleaf.size());       // marked point lost if occurs before any content
                        }
                        break;

                        case 'm':       // m
                        if (c2c3==' ') {        // x y 'm' - begin a new subpath by moving the current point to coordinates (x, y)
                                //assert pathrect==null && pathline==null: pathrect+" "+pathline; -- usually but not necessarily. m l h m, e.g.
                                //path.reset(); -- NO
                                getDoubles(ops,d,2); ctm.transform(d,0, d,0, 1);
//if (lcnt_++ < 10) System.out.println("move to "+srcpt+" / "+transpt);
                                // no special case for one 'm' only or 'm'/'m' as these are rare
                                path.moveTo((float)(curx=d[0]), (float)(cury=d[1])); pathlen++;
                                fvalidpath = true;      //-- move-only alone with nothing to paint shouldn't make valid, but it does
                        }
                        break;

                        case 'P':       // P
                        if (c2c3=='S') {}//sampledata("PS operator");   // PDF 1.1
                        break;

                        case 'Q':       // Q
                        if (c2c3==' ') {        // 'Q' - pop graphics stack
                                if (gsstack.size()>0/*it happens*/) gs = gsstack.remove(gsstack.size()-1);

                                // rather than closing all spans in pushed graphics state to reestablish previous state, we set the changes.
                                // Some PDFs repeatedly push a graphics stack and set the exact same attributes within intervening drawing.
                                // In this case, the redundancy does not create useless spans.
                                fnewfont = true; newTr = gs.Tr;
                                Tc = gs.Tc; Tw = gs.Tw; Tz = gs.Tz; TL = gs.TL; Ts = gs.Ts; //Tm = gs.Tm; Tlm = gs.Tlm; // needed?
                                fCS = gs.fCS; sCS = gs.sCS; newcolor = gs.strokecolor; newfcolor=gs.fillcolor;
//System.out.println("Q pop: "+linewidth+" vs "+gs.linewidth);//gs.fontdictkey+" @ "+pointsize+", tf="+tf);
                                fnewline = fpop = true; // fix line width span for possible stroke font
                                ctm = gs.ctm;

                                if (clipp!=gs.clip && clipp.size()==0) clipp.remove();  // "Serving PDFs on the Web" starts with "q Q"
                                clipp = gs.clip;        // foolable: set clip, q, <nothing>, Q
                                //clipr = clipp.getClip();
//if (lcnt_ < 10) System.out.println("Q: pop "+ctm);

                                fvalidpath = false;     // "current path is not part of the graphics state"
                        }
                        break;

                        case 'q':       // q
                        if (c2c3==' ') {        // 'q' - push graphics stack
                                // stuff state -- use possibly new settings, in case of sequence: <new-setting> 'q' ... 'Q', which would drop change in setting
                                gs.Tr = newTr; gs.Tc = Tc; gs.Tw = Tw; gs.Tz = Tz; gs.TL = TL; gs.Ts = Ts; //gs.Tm = Tm; gs.Tlm = Tlm;
                                gs.fCS = fCS; gs.sCS = sCS; gs.strokecolor = newcolor; gs.fillcolor = newfcolor;
                                gs.ctm = ctm;
                                gs.clip = clipp;

                                gsstack.add(new GraphicsState(gs));     // push copy and keep using current

                                //assert !fvalidpath;   // good idea, but don't enforce
//if (lcnt_ < 10) System.out.println("q: push "+ctm);
                        }
                        break;

                        case 'R':       // RG
                        if (c2c3=='G') {        // r g b 'RG' - set DeviceRGB color space and stroke color
                                sCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);       // CS_LINEAR_RGB? => worse
                                getFloats(ops, cscomp, 3); clip(cscomp, sCS); float r=cscomp[0], g=cscomp[1], b=cscomp[2];
                                newcolor = r==0f && g==0f && b==0f? Color.BLACK: r==1f && g==1f && b==1f? Color.WHITE: new Color(r,g,b, 1f);    // r==1f && g==0f && b==0f: Color.RED: r==0f && g=1f && b==0f? Color.GREEN: r==0f && g==0f && b==1f: Color.BLUE ?
                                //else newcolor=new Color(sCS, cscomp, 1f);     // during color space testing -- "alpha value of 1.0 or 255 means that the color is completely opaque"
                        }
                        break;

                        case 'r':       // re, ri, rg
                        if (c2c3=='e') {        // x y width height 're' - append a rectangle to the current path as a complete subpath, with lower-left corner (x, y) and dimensions width and height
                                assert pathrect==null && pathline==null;        // Acrobat Core API Overview has rect after rect

                                getDoubles(ops,d,4);
//System.out.print("re "+newfcolor+" "+d[0]+" "+d[1]+" "+d[2]+" "+d[3]+" => ");
//System.out.print("re "+d[2]+"x"+d[3]+" @ "+d[0]+","+d[1]+" => ");
                                ctm.transform(d,0, d,0, 1); ctm.deltaTransform(d,2, d,2, 1);
                                // FIX: doesn't pick up shear
                                double x=curx=d[0], y=cury=d[1], w=d[2], h=d[3];
                                if (w<0.0) { x+=w; w=-w; } /*else--negative and small*/ if (w<1.0 /*&& antialiasing*/) w=1.0;
                                if (h<0.0) { y+=h; h=-h; } /*else*/ if (h<1.0) h=1.0;
//System.out.println("rectangle "+w+"x"+h+" @ "+x+","+y);
                                Rectangle r = new Rectangle((int)x,(int)(y /*- h/*ll=>ul*/), (int)Math.round(w),(int)Math.round(h));    // upside down, so lower-left => upper-left -- but Java coordinates flip Y so OK.  Math.ceil() so clip gets bottom line.
                                //assert r.width>0 && r.height>0: r.width+" "+r.height;
//System.out.println(" => "+Rectangles2D.pretty(r));
                                // if rectangle only shape in path, keep it simple and don't use GeneralPath
                                if (!fvalidpath && (((peek=in.peek())<'0' || peek>'9') && peek!='.' && peek!='-')) { pathrect=r; pathlen=1; }
                                //else if (pathlen==1 && ((peek<'0' || peek>'9') && peek!='.' && peek!='-')) { pathrect=r; pathlen=1; } // ignore initial 'm'
                                else { path.append(r, false); pathlen += 4; }
                                fvalidpath = true;

                        } else if (c2c3=='g') { // r g b 'rg' - set DeviceRGB color space and fill color
                                fCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                                getFloats(ops, cscomp, 3); clip(cscomp, fCS); float r=cscomp[0], g=cscomp[1], b=cscomp[2];
                                newfcolor = r==0f && g==0f && b==0f? Color.BLACK: r==1f && g==1f && b==1f? Color.WHITE: new Color(r,g,b, 1f);   //new Color(fCS, cscomp, 1f);

                        } else if (c2c3=='i') { // name 'ri' - color rendering intent [no equivalent in Java 2D]
                                gs.renderingintent = (String)ops[0];
                        }
                        break;

                        case 'S':       // S, SC, SCN
                        if (c2c3==' ') {        // -- 'S' - stroke the path
                                fstroke = true;

                        } else if (c2c3=='C' || c2c3==('C'<<8)+'N') {   // c1, ..., cn 'SC' - set stroking color; SCN "same as SC, but also supports Pattern, Separation, DeviceN, and ICCBased color spaces."
                                if (opsi>0 && CLASS_NAME==ops[opsi-1].getClass()) {     // scn
                                        assert c2c3==('C'<<8)+'N';
                                        sCS = pdfr.getColorSpace(ops[opsi-1], csres, patres);
                                        opsi--;
                                }

                                if (opsi>0) {
                                        getFloats(ops, cscomp, Math.min(opsi,5)); clip(cscomp, sCS);    // opsi can be 0 (Pattern color space)
                                        // scale to colorspace min..max
                                        //newcolor = new Color(sCS, cscomp, 1f);        // Java bug: Color(ColorSpace...) hardcodes component range to 0.0 .. 1.0
                                        float[] rgb = sCS.toRGB(cscomp);
                                        newcolor = new Color(rgb[0], rgb[1], rgb[2], 1f);
                                }
                        }
                        break;

                        case 's':       // s, sc, scn, sh
                        if (c2c3==' ') {        // -- 's' - close and stroke the path
                                assert fvalidpath: "s";
                                if (fvalidpath) {
                                        if (pathrect==null) path.closePath();
                                        fstroke=true;
//System.out.println("s(troke) - path bounds = "+path.getBounds2D());
                                }

                        } else if (c2c3=='c' || c2c3==('c'<<8)+'n') {   // c1, ..., cn 'scn' - set nonstroking color; 'scn' "same as sc, but also supports Pattern, Separation, DeviceN, and ICCBased color spaces."
                                if (opsi>0 && CLASS_NAME==ops[opsi-1].getClass()) {     // scn
                                        assert c2c3==('c'<<8)+'n';
//System.out.println("scn "+csname);
                                        fCS = pdfr.getColorSpace(ops[opsi-1], csres, patres);
                                        opsi--;
                                }

                                if (opsi>0) {
                                        getFloats(ops, cscomp, Math.min(opsi,5)); clip(cscomp, fCS);
//System.out.println("sc/scn  "+opsi+": "+cscomp[0]+" "+cscomp[1]+" "+cscomp[2]+" in "+fCS);
                                        //newfcolor = new Color(fCS, cscomp, 1f);       // Java bug: see 'S' above
                                        float[] rgb = fCS.toRGB(cscomp);
//System.out.println(" => "+rgb[0]+" "+rgb[1]+" "+rgb[2]);
                                        newfcolor = new Color(rgb[0], rgb[1], rgb[2], 1f);
//System.out.println(" => "+newfcolor);
                                }

                        } else if (c2c3=='h') { // name 'sh' - shading pattern
                                Dict shdict = (Dict)pdfr.getObject(shres.get(ops[0]));
                                ColorSpace cs = pdfr.getColorSpace(shdict.get("ColorSpace"), csres, patres);
                                Object[] oa = (Object[])pdfr.getObject(shdict.get("Bbox"));
                                Rectangle bbox = oa!=null? array2Rectangle(oa, ctm/*sh in user coords*/): clipp.getCrop();

                                FixedLeafShade l=FixedLeafShade.getInstance(shdict, cs, bbox, clipp, pdfr/*this*/); lastleaf=l; leafcnt++;
                                l.getBbox().setBounds(l.getIbbox()); l.setValid(true);
                        }
                        break;


                        case '"':       // "
                        if (c2c3==' ') {        // aw ac string " - move to the next line and show a text string
                                getDoubles(ops,d,2);
                                Tw=d[0]; Tc=d[1];
                                //Tlm.translate(0.0, -TL);
                                //Tm.setTransform(Tlm);
                                ops[0]=ops[2];
                        }
                        //break; => fall through to "'"!

                        case '\'':      // '
                        if (c2c3==' ') {        // string ' - move to the next line and show a text string
                                Tlm.translate(0.0, -TL);
                                Tm.setTransform(Tlm);
                                c2c3 = 'j';     // now pretend Tj
                        }
                        //break; => fall through to 'Tj'

                        case 'T':       // Tc, Tw, Tz, TL, Tf, Tr, Ts, Td, TD, Tm, T*, Tj, TJ
                        if (c2c3=='j' || c2c3=='J') {   // string 'Tj' / [string number ...] 'TJ' - show a text string
                                Object[] oa;
                                if (c2c3=='j') { oa = Tja/*new Object[1]*/; oa[0] = ops[0]; }   // make Tj look like TJ
                                else oa = (Object[])ops[0];     // array 'TJ' - show one or more text strings, allowing individual glyph positioning [often interword space, rather than kerning]

                                // if no content, skip
                                /*if (c2c3=='j' && ((StringBuffer)ops[0]).length()==0) break;
                                else {
                                        boolean ok = false;
                                        for (int i=0,imax=oa.length; i<imax; i++) { if (CLASS_STRING==oa[i].getClass() && ((StringBuffer)oa[i]).length() > 0) { ok = true; break; }
                                        if (!ok) break;
                                }*/

                                // would like to split off into own method, but so much state to pass (two methods maybe, font setting and text drawing)
                                if (lastleaf.isStruct()) lastleaf=linep;        // 'ET' will guarantee some text in initial BT..ET
                                //lastleaf!=clipp? lastleaf: linep);    // lastleaf initially clipp, but since added textp and linep which bump Span.close() to next subtree, but can use linep here because know going to add some text (sigh)

                                // 1. set font, maxr & spacew
//System.out.println("Tj "+fnewfont);   // +", tf="+tf);
                                if (fnewfont) { // fscale is advisory now: verify that did indeed change -- pdfTex-0.13d generates redundant font changes
                                        if (fontdict==gs.fontdict
                                                && !tf.isTransformed() && Tm.getScaleX() == -Tm.getScaleY() && Tm.getShearX()==0.0 && Tm.getShearY()==0.0
                                                && Math.abs(gs.pointsize * Tm.getScaleX() - tf.getSize()) < Doubles.EPSILON)
                                                fnewfont=false; //System.out.println("cancelled: "+newsize+" vs "+tf.getSize());
                                }

                                if (fnewfont) { // do here rather than at bottom with rest so to collect all changes immediately before drawing text (sometimes Tm set after Tf)
                                        // set new font and dependent state
                                        if (fontspan!=null) {
                                                //System.out.println("close |"+lastleaf+"|");
//System.out.println("close("+lastleaf.getName());
                                                fontspan.close(lastleaf);
                                                //assert !fshape || fontspan.isSet(): "can't add font span "+getName()+"  "+fontspan.getStart().leaf+" .. "+lastleaf;   // => can fail if no content between font changes
                                                //assert spancnt>0 || fontspan.getStart().leaf == clipp.getFirstLeaf(): "attached "+fontspan.getStart().leaf+" vs "+clipp.getFirstLeaf();       // not true if start with graphics
                                                //System.out.println("font span "+fontspan.getStart()+" .. "+fontspan.getEnd());
                                                spancnt++;
                                        } //else System.out.println("first @ "+lastleaf+"/"+lastleaf.size()+"  is "+lastleaf.getLastLeaf());
                                        //assert fontspan.getStart().leaf==span.getStart().leaf && fontspan.getStart().offset==span.getStart().offset && fontspan.getEnd().leaf==span.getEnd().leaf && fontspan.getEnd().offset==span.getEnd().offset: fontspan.getStart()+".."+fontspan.getEnd()+"  vs  "+span.getStart()+".."+span.getEnd();

                                        fontdict=gs.fontdict; pointsize=gs.pointsize;
                                        tf = pdfr.getFont(fontdict,  (float)pointsize, Tm,  this);
                                        maxr = tf.getMaxCharBounds();
//System.out.println("new font "+tf.getFamily()+", "+pointsize+"/"+tf.getSize()+", max bounds = "+maxr);

                                        //System.out.print("open |"+lastleaf+"| .. ");
                                        fontspan = (SpanPDF)Behavior.getInstance((DEBUG? tf.getName()+"/"+pointsize/*tf.getSize()*/: tf.getName()), "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                        fontspan.font = tf;
//System.out.print("open("+lastleaf.getName()+" .. ");
                                        fontspan.open(lastleaf);

                                        spacech = tf.getSpaceEchar();
                                        spaceadv = tf.echarAdvance(spacech);
                                        spacew = Math.max(4.0*Tm.getScaleX()*pointsize/12f, spaceadv.getY()==0.0? spaceadv.getX(): Math.sqrt(spaceadv.getX() * spaceadv.getX() + spaceadv.getY() * spaceadv.getY()));   // be conservative.  If wrong, long word-lines; if right, better word boundaries.
//System.out.println(tf.getName()+", spacech="+(int)spacech+", "+spaceadv+", spacew="+spacew+", size="+tf.getSize()+", sx="+Tm.getScaleX());
                                        if (tf instanceof NFontType3) ((NFontType3)tf).setPage(page);
                                        t0 = tf instanceof NFontType0? (NFontType0)tf: null;

                                        fnewfont = false;       // set to false only here.  Other places can only signal a change that needs scaling.
                                }
//System.out.println("font="+tf+", space bounds "+tf.getStringBounds(" ")*/);
//System.out.println("spacew (pixels) "+(spacew * Tm.getScaleX()));

                                if (newTr != Tr /*|| c==-1*/) { // lesk-superbook has "0  Tr 21.3343 0  TD 3  Tr -0.085  Tc (add) Tj" for each word
//System.out.println("close "+Trspan+" @ "+lastleaf+"/"+lastleaf.size());
                                        if (Trspan!=null) { Trspan.close(lastleaf); spancnt++; /*System.out.println("Tr "+Trspan.Tr+" "+Trspan.getStart()+".."+Trspan.getEnd());*/ }

                                        Tr = newTr;

                                        if (Tr==0) { Trspan=null; vspancnt++; }
                                        else {
                                                Trspan = (SpanPDF)Behavior.getInstance("Tr"/*+Tr*/, "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                                Trspan.Tr = Tr;
                                                Trspan.open(lastleaf);
                                        }
//Node openat=(lastleaf!=clipp? lastleaf: linep); /*if (lastleaf==clipp)*/ System.out.println("*** Tr = "+Tr+" => "+openat+"/"+openat.size()+", clipp="+clipp);
                                }

                                // 2. set up line
                                double newbaseline = Tm.getTranslateY();
                                if (/*Math.abs(subtract to compare?)*/newbaseline!=baseline && Math.abs(baseline-newbaseline) > 0.1) {  // if switch font, won't be exact
                                        if (linep.size() > 0) { // start new line?
                                                //if (linep.size()==1) { textp.appendChild(linep.childAt(0)); linep.remove(); } // actually should keep structure
                                                linep = new FixedIHBox("line",null, textp/*clipp*/); fconcat = false;
                                        }
                                        baseline = newbaseline; // for first word on line too
                                }

                                // 3. add text
                                double sTc = Tc * Tm.getScaleX();
                                //boolean fspace1 = false;      // if just single space, retain, because can be part of marked sequence
                                boolean frot = Math.abs(Tm.getScaleX()) < Math.abs(Tm.getShearX());
                                for (int i=0,imax=oa.length; i<imax; i++) {
                                        o = oa[i];

                                        if (o instanceof Number) {
                                                double kern = ((Number)o).doubleValue()/1000.0 * pointsize;     // not tf.getSize(), which incorporates Tm.getScaleX()
                                                Tm.translate(-kern, 0.0);       // regardless of possible concatenation adjust Tm as concat adjustment on leaf outside Tm translation due to hunk width

                                        } else if (CLASS_STRING==o.getClass()) {        // assert CLASS_STRING==o.getClass(); => FOP 0.20.3 generating, e.g., "[(REVISION)0/Tc-250(AUTHORIZATION)]TJ" -- "/Tc" ???
                                                // a. text as encoded and Unicode
                                                //if (sb.length()>0 && sb.charAt(sb.length()-1)==spacech) sb.deleteCharAt(sb.length()-1);
                                                // LATER: if (en.isUnicode()) go with Unicode only
                                                String txte = o.toString(), txtu = tf.toUnicode(txte);
//if (t0!=null) System.out.println(txte+" => "+t0.toCID(txte));
                                                if (t0!=null) txte = t0.toCID(txte);
                                                //assert txtu.length()==txte.length() => ligatures
//System.out.println("Tj show text |"+txt+"|/"+txt.length()+" @ ("+Tm.getTranslateX()+","+(ph - Tm.getTranslateY())+")");//  vs  "+transpt);

                                                for (int s=0,smax=txte.length(), e; s<smax; s=e) {      // could be empty
                                                        // b. split text chunks into words
                                                        e = smax;
                                                        char ch = txte.charAt(s);
                                                        if (ch==spacech) {
                                                                e = s;
                                                                if (e==0 && i==0 && s==0) {     // special case: if all spaces keep one as could be used for rendered effect or be sole child of clip or anchor for markedseq
                                                                        boolean fws=true; for (int j=0; j<smax; j++) if (txte.charAt(j)!=spacech) { fws=false; break; }
                                                                        if (fws) e=1;   //fspace1=true; }
                                                                }
                                                        } else if (ch==' ') {   // hardcoded word split though not space
                                                                e = s + 1;
                                                        } else if (sTc > 1.5*spacew) {  // big Tc so split adjacent chars, but 2* because higher threshold to rent asunder what's already joined.  E.g., "0.249 Tc  [(t)247(h)242(eb)242(a)253(s)251(es)251(y)269(s)251(t)247(e)240(m)254(:)]TJ" (blech!)
                                                                e = s + 1;
//multivalent.Meta.sampledata("rent asunder on Tc "+Tc+" / "+txte);
                                                        } else {
                                                                for (int j=s+1; j<smax; j++) {
                                                                        ch = txte.charAt(j);    //assert ch!=NFont.NOTDEF_CHAR: txte; -- done in ad9875.pdf by InDesign 1.5.2
                                                                        if (ch==spacech /*&& ch!=NFont.NOTDEF_CHAR/*in ad9875.pdf is 0!*/) { e=j; break; }      // split words at spacech
                                                                        else if (ch==' ') { e=j+1; break; }     // always split at char 32 so can apply Tw, then close up with concat
                                                                }
                                                        }

                                                        // c. create leaf for word, either concatenate or new word
                                                        if (s < e) {    // could start with space
                                                                // special case for 1-letter word?
                                                                String sube = txte.substring(s,e), subu = txtu.substring(s,e);
//*if (" ".equals(sub))*/ System.out.println("sub: |"+sub+"|, s="+s+", e="+e+", smax="+smax);

                                                                final double kern = sTc * (sube.length() - 1);
//System.out.println(tf.getName()+" "+(int)sube.charAt(s)+": "+s+" "+e+" "+smax);
                                                                Point2D adv = tf.estringAdvance(txte, s, e);    // in pixels
//if (frot) System.out.println("rotate |"+sube+"|: @ "+pointsize+" x "+Tm+" => "+adv);
//if (fw<1.0) System.out.println(fw+" width "+tf+", |"+txte+"|, "+s+".."+e+", "+(int)sube.charAt(0)+"  +  "+kern);
//+", spacew "+spacew+" vs "+(widths[32-firstch]*tf.getSize()/1000.0));

                                                                // put text hunk in Leaf
                                                                int bw, bh, ascent;
                                                                if (!frot) {
                                                                        bw = (int)Math.ceil(Math.abs(adv.getX()) + kern);
                                                                        bh = (int)Math.ceil(Math.abs(maxr.getHeight()));        // => tf.getSize()?
                                                                        ascent = (int)Math.ceil(Math.abs(-maxr.getY()));        // set baseline to ascent? => tf.getSize() * 0.8f ?
//if (bw==0) System.out.println(sube+"/"+Integer.toOctalString(sube.charAt(0))+" "+tf.getName()+"@"+tf.getSize()+" "+s+".."+e+" => "+adv+" => "+bw+"x"+bh);
                                                                } else {
                                                                        bw = (int)Math.abs(maxr.getHeight());
                                                                        bh = (int)Math.ceil(Math.abs(adv.getY()) + kern);
                                                                        ascent = bw;
                                                                }
                                                                //FixedLeafUnicodeKern l;
                                                                double dx = Tm.getTranslateX() - lastX; // in pixels
//System.out.println(sub+", fw="+fw+" @ x="+Tm.getTranslateX()+", concatthresh = "+spacew+", fconcat="+fconcat+", dx = "+dx);
//System.out.println("concat: |"+/*l.getName()+*/"| + |"+txt+"|, "+dx+" <? "+spacew));
                                                                if (frot /*or Tr special effects*/ && false/*until can handle*/) {
                                                                        // handled with AffineTranform on spot font
                                                                } else if (fconcat /* && starts with letter?*/
                                                                                   //&& baseline==lastBaseline -- guaranteed since same line
                                                                                   && -2.0*spacew < dx&&dx < spacew/4.0 /*-spacew < dx && dx < spacew/2.0*/
                                                                                   && /*for now*/Ts==0.0) {     // heuristic for determining if part of same word.  if so, concatenate with previous leaf
                                                                        //assert linep.size()>0 && linep.getLastChild().getName().length()>0;
                                                                        FixedLeafUnicodeKern l = (FixedLeafUnicodeKern)linep.getLastChild();  assert l==lastleaf;
                                                                        int oldlen = l.size();
//System.out.println(l.getName()+"/"+l.size()+" concat w/"+sub+", "+dx+" < "+spacew+", concat "+l.getKernAt(oldlen/*l.size()*/-1)+" => "+dx+", sTc="+sTc);
                                                                        l.append(subu,sube, (float)sTc);
                                                                        //assert l.isValid();
                                                                        l.setKernAt(oldlen-1, dx);      // adjust join point by inter-hunk kern from TJ number
                                                                        Rectangle ibbox = l.getIbbox();
                                                                        totalW += adv.getX() + dx;
                                                                        ibbox.width = (int)Math.ceil(totalW); ibbox.height = Math.max(ibbox.height, bh);        // FIX: max height depends on ascent
                                                                        l.bbox.setSize(ibbox.width, ibbox.height);      //l.baseline = Math.max(l.baseline, ascent); -- baseline is the same during TJ
                                                                        concatcnt++;

                                                                } else {        // new leaf
//if (Math.abs(sTc) > 1.0) System.out.println("|"+sub+"|  sTc = " + Tc + " * " + Tm.getScaleX()+" = " + sTc);
                                                                        FixedLeafUnicodeKern l = new FixedLeafUnicodeKern(subu,sube, null, linep/*clipp*/, sTc); lastleaf=l; leafcnt++; // should track kerning per character and bump by pixel when >= 1.0
                                                                        l.getIbbox().setBounds((int)Math.round(Tm.getTranslateX()), (int)Math.round(Tm.getTranslateY() + maxr.getY() + Ts * Tm.getScaleY()), bw,bh);    // FIX: Ts => span -- upside down
//System.out.println(sub+" ibbox = "+l.getIbbox()+": "+Tm.getTranslateX()+", "+Tm.getTranslateY()+" + "+maxr.getY() + " + "+Ts+" * "+Tm.getScaleY()+"  "+bw+"x"+bh);
                                                                        l.bbox.setBounds(l.getIbbox());
                                                                        //l.bbox.setSize(bw,bh);
                                                                        //l.baseline = ascent;  // => NO, rounding errors;
                                                                        l.baseline = Tm.getShearX()==0.0 && Tm.getShearY()==0.0? (int)Math.round(Math.abs(baseline - l.getIbbox().y)): ascent;
//System.out.println("baseline: "+ascent+" vs "+l.baseline);
                                                                        totalW = adv.getX();
                                                                }
                                                                lastleaf.setValid(true);        // efficiency hack (careful!)
//System.out.println(lastleaf.getName()+" "+Rectangles2D.pretty(((Fixed)lastleaf).getIbbox()));

                                                                // d. advance point according to width of text hunk vis-a-vis text parameters (Tc, ...)
                                                                //assert sTc*sube.length() - kern == 0.0;
//if (frot) System.out.print("adv = "+adv+", "+Tm);
                                                                tmpat.setToTranslation(adv.getX() + kern, -adv.getY()); Tm.preConcatenate(tmpat);       // correct total kerning after rounded to nearest integer between characters
//if (frot) System.out.println(" => "+Tm);
                                                                lastX = Tm.getTranslateX();
//System.out.println("Tc "+sTc+": "+lastX+" => "+Tm.getTranslateX());
                                                                if (sTc!=0.0) { tmpat.setToTranslation(sTc, 0.0); Tm.preConcatenate(tmpat); }
//System.out.println("\tdelta by word width "+fw + " + ("+sTc+"*"+sub.length()+"-"+kern+") = "+(fw+(sTc*sub.length()-kern))+" => "+lastX);
                                                                fconcat = !frot && /*Character.isLetter(*/subu.charAt(subu.length()-1) < 256/*)*/;      //weird metrics on quotes and may substitute fonts.     // possible for next time
//if (Tc!=0.0) System.out.println(sTc+" "+sub+": "+(sTc*sub.length())+" - "+kern);
                                                        }

                                                        // e. advance point according to spaces at breakpoint, set fconcat potential for next leaf
                                                        /*if (fspace1) {
                                                                //if (spacech==' ') Tm.translate(Tw, 0.0);      // word separator too
                                                                fspace1=false;
                                                                fconcat=false;  // fix earlier assumption
                                                        }*/
                                                        // gobble trailing spaces
                                                        int twcnt = e>0 && txte.charAt(e-1)==' '? 1: 0;
                                                        int spacecnt = 0; /*if (spacech!=NFont.NOTDEF_CHAR)*/ while (e<smax && txte.charAt(e)==spacech) { spacecnt++; e++; }
                                                        if (spacecnt > 0 || (s==0 && i==0 && txte.charAt(0)==spacech)) {
                                                                fconcat = false;        // no concat across spaces
                                                                if (spacech==' ') twcnt += spacecnt;
                                                        }
//System.out.print(spacew+", Tw="+Tw+", Tc="+Tc+", scale="+Tm.getScaleX()+" @ "+txt.substring(s,e)+"/"+txt.substring(e+1)+" "+Tm.getTranslateX());
                                                        if (spacecnt>0 || twcnt>0) {
                                                                tmpat.setToTranslation(spacecnt*spaceadv.getX(), -spacecnt*spaceadv.getY()); Tm.preConcatenate(tmpat);
                                                                Tm.translate(spacecnt*Tc + twcnt*Tw, 0.0);      // don't forget Tc on spaces
                                                        }
//System.out.println("\tspaces  ("+spacecnt+" or "+twcnt+") * ("+spaceadv.getX()+"+"+Tw+"+"+Tc+") => "+Tm.getTranslateX());
                                                }
                                        }
//if (DEBUG) System.out.println("TJ show text |"+txt+"|/"+txt.length()+" @ ("+Tm.getTranslateX()+","+(ph - Tm.getTranslateY())+")");// => "+transpt);
                                }

                        } else if (c2c3=='d') { // tx ty 'Td' - translate Tlm, set Tm to it
                                getDoubles(ops,d,2);
                                //Tlm.deltaTransform(d,0, d,0, 1); -- NO! concatenated, so picks up prevailing scale
                                Tlm.translate(d[0], d[1]);      // often see y=0 "1.9462 0 Td"
                                Tm.setTransform(Tlm);

                        } else if (c2c3=='D') { // tx ty 'TD' - same as 'Td' + set leading to ty
                                getDoubles(ops,d,2);
                                //Tlm.deltaTransform(d,0, d,0, 1); -- NO!
                                TL = -d[1];     // unscaled text space units -- minus!
                                Tlm.translate(d[0], d[1]);
                                Tm.setTransform(Tlm);

                        } else if (c2c3=='m') { // a b c d e f 'Tm' - set (not concat) the text matrix Tm, and the text line matrix Tlm
                                double m00=Tm.getScaleX(), m01=Tm.getShearX(), m10=Tm.getShearY(), m11=Tm.getScaleY();

                                getDoubles(ops,d,6); tmpat.setTransform(d[0], d[1], d[2], d[3], d[4], d[5]);
//System.out.print("\tTm x="+Tm.getTranslateX()+" over "+d[0]+" "+d[1]+" "+d[2]+" "+d[3]+" "+d[4]+" "+d[5]);
                                Tm.setTransform(ctm);
                                Tm.concatenate(tmpat);  // Tm actually Trm
//System.out.println("'Tm': "+ctm+" * "+tmpat+" = "+Tm);
                                Tlm.setTransform(Tm);
//System.out.println(" => "+Tm.getTranslateX());

                                // microbrowser.pdf by Ghostscript 5.01 does 1 0 0 1 x1 y1 Tm ...(txt) Tj ... 1 0 0 1 x2 y2 !
                                // livenotes.pdf by PDFMaker 5.0 for Word does Tm..Tj..Tm where Tm's have same scale
                                if (m00!=Tm.getScaleX() || m01!=Tm.getShearX() || m10!=Tm.getShearY() || m11!=Tm.getScaleY()) fnewfont=true;

                        } else if (c2c3=='*') { // -- 'T*' - move to the start of the next line.  This operator has the same effect as the code 0 Tl 'Td'
                                Tlm.translate(0.0, -TL);        // minus!
                                Tm.setTransform(Tlm);

                        // set text state
                        } else if (c2c3=='c') { // number 'Tc' - set the character spacing
                                getDoubles(ops,d,1);
                                //d[1]=0.0; Tm.deltaTransform(d,0, d,0, 1); -- unscaled text space units, but adjusted when text is drawn
                                Tc=d[0];

                        } else if (c2c3=='w') { // number 'Tw' - set the word spacing
                                getDoubles(ops,d,1);
                                //d[1]=0.0; Tm.deltaTransform(d,0, d,0, 1);
                                Tw=d[0];

                        } else if (c2c3=='z') { // number 'Tz' - set horizontal scaling (rare)
                                getDoubles(ops,d,1);
                                Tz=d[0];
if (Tz!=100.0) multivalent.Meta.sampledata("Tz "+Tz);

                        } else if (c2c3=='L') { // number 'TL' - text leading
                                getDoubles(ops,d,1);
                                //d[1]=0.0; Tm.deltaTransform(d,0, d,0, 1);
                                TL=d[0];

                        } else if (c2c3=='f') { // font size 'Tf' - set current font
                                assert fontres!=null: page;
                                gs.fontdict = (Dict)pdfr.getObject(fontres.get(ops[0]));  assert gs.fontdict!=null: ops[0]+" not in "+fontres;
                                gs.pointsize = ((Number)ops[1]).doubleValue();
                                fnewfont = true;

                        } else if (c2c3=='r') { // number 'Tr' - text rendering mode
                                newTr = ((Number)ops[0]).intValue();    // check for validity?
if (newTr >= 4) multivalent.Meta.sampledata("Tr "+newTr);

                        } else if (c2c3=='s') { // number 'Ts' - text rise
                                getDoubles(ops,d,1); d[1]=0.0; Tm.deltaTransform(d,0, d,0, 1);  // taken in V-space
                                Ts = Math.abs(d[0]);    // FIX: Math.abs(d[1]=0.0? d[0]: d[0]==0.0? d[1]: Math.sqrt(d[0]*d[0] + d[1]*d[1])) -- coordinate with 'Tj'
//if (Ts!=0.0) multivalent.Meta.sampledata(Ts+" Ts");
                        }
                        break;

                        case 'v':       // v
                        if (c2c3==' ') {        // x2 y2 x3 y3 'v' - append a cubic B�zier curve to the current path: current point to x3 y3, with current point and x2 y2 as control points
                                getDoubles(ops,d,4); ctm.transform(d,0, d,0, 2);
                                path.curveTo((float)curx,(float)cury, (float)d[0],(float)d[1], (float)(curx=d[2]),(float)(cury=d[3])); pathlen+=100;
                        }
                        break;

                        case 'n':       // n
                        if (c2c3==' ' && pendingW!=-1) {
                                c2c3 = pendingW;
                                // fall-through to 'W'
                        } else if (c2c3==' ') { // -- 'n' - end the path object without filling or stroking it.
                                //NO path.closePath();
                                path.reset(); pathlen=0;        // clipping ops have already used path
                                pathrect=null; pathline=null;
                                fvalidpath=false;
                                break;
                        } else break;
                        // possible fall-through

                        // PDF Ref: "after the last path construction operator and before the path-painting operator that terminates a path object."
                        case 'W':       // W, W*
                        if (c2c3=='*' || c2c3==' ') {   // 'W'/'W*' - clipping nonzero/even-odd winding rule
                                //assert fvalidpath: (c2c3=='*'? "W*": "W"); -- seen in matchingshapes.pdf
//System.out.println("empty clip");

                                if (!fvalidpath) pendingW = c2c3;       // Ghostscript buggy up through at least v6.0 and puts W before clip path.  Acrobat loophole.
                                else {
                                        //if (clipp.size()==0) clipp.remove(); -- BUT 'q', 'W', 'Q', add to initial clip.  possible scenario: set clip, q, set clip (at which point first clip has no children), Q, add to first clip
                                        Rectangle bounds;
                                        //if (newclip contained in prevailing clip) do nothing
                                        if (pathrect!=null /*&& !rotated*/) {   // simpler, and more common -- faster?
                                                bounds = new Rectangle(pathrect.x, pathrect.y, pathrect.width+1, pathrect.height+1);    // + 1 for inclusive right and bottom edges
                                                // winding rule moot -- both give same result

                                                Shape oldshape = clipp.getClip();
                                                if (!(oldshape instanceof Rectangle) || !pathrect.contains((Rectangle)oldshape))        // larger rect doesn't enlarge clip
                                                        clipp = new FixedIClip(c2c3=='*'? "W*": "W", null, clipp, new Rectangle(0,0, bounds.width, bounds.height), bounds);
                                                //else System.out.println("can't enlarge W "+bounds);   //-- e.g., 1677.pdf

                                                // no need to clear pathrect, even if in.peek=='n', since have to copy and transform anyhow

                                        } else {
                                                GeneralPath wpath;
                                                int rule = c2c3=='*'? GeneralPath.WIND_EVEN_ODD: GeneralPath.WIND_NON_ZERO;
                                                if (pathrect!=null) {
                                                        wpath = new GeneralPath(rule, 4);
                                                        wpath.append(pathrect, false);
                                                } else if (pathline!=null) {    // seen in jdj200108 and jdj/3-10.  probably an error in PDF generator
                                                        float x1=(float)pathline.getX1(), y1=(float)pathline.getY1(), x2=(float)pathline.getX2(),y2=(float)pathline.getY2();
                                                        wpath = new GeneralPath(rule, 4);
                                                        wpath.moveTo(x1,y1-linewidth); wpath.lineTo(x2,y1-linewidth); wpath.lineTo(x2,y1+linewidth); wpath.lineTo(x1,y1+linewidth); wpath.lineTo(x1,y1-linewidth);
                                                } else {
                                                        if (in.peek()=='n') { wpath=path; path=new GeneralPath();/*FixedIClip doesn't copy shape*/ } else { wpath = (GeneralPath)path.clone(); }        // don't need to clone if following token is 'n', which it almost always is
                                                        wpath.closePath();
                                                        wpath.setWindingRule(rule);
                                                }
//if (peek=='n') System.out.println("no clone W");

                                                //bounds = bounds.intersection(clipr);  // implicit in painting anyhow
                                                bounds = wpath.getBounds();

                                                tmpat.setToTranslation(-bounds.x, -bounds.y);   // clip (0,0) relative to bbox
                                                wpath.transform(tmpat);
//System.out.println((c2c3=='*'? "W*": "W")+" "+wpath+" in "+bounds);

                                                clipp = new FixedIClip(c2c3=='*'?"W*":"W", null, clipp, wpath, bounds);
                                        }
//System.out.println("clip: W"+(char)c2c3+": "+bounds);
                                        if (pendingW != -1) { pendingW=-1; path.reset(); pathlen=0; pathrect=null; pathline=null; fvalidpath=false; }
                                }       //else System.out.println("W w/o path ");
                        }
                        break;

                        case 'w':       // w
                        if (c2c3==' ') {        // number 'w' - line width
                                d[0]=((Number)ops[0]).doubleValue(); d[1]=0.0; ctm.deltaTransform(d,0, d,0, 1); // can be rotated or sheared
                                gs.linewidth = (float)Math.abs(d[1]==0.0? d[0]: d[0]==0.0? d[1]: Math.sqrt(d[0]*d[0] + d[1]*d[1]));     // in case rotated
                                fnewline = true;
                        }
                        break;

                        case 'y':       // y
                        if (c2c3==' ') {        // x1 y1 x3 y3 'y' - append a cubic B�zier curve to the current path: current point to the point x3 y3, using x1 y1 and x3 y3 as control points
                                getDoubles(ops,d,4); ctm.transform(d,0, d,0, 2);
                                path.curveTo((float)d[0],(float)d[1], (float)d[2],(float)d[3], (float)(curx=d[2]),(float)(cury=d[3])); pathlen+=100;
                        }
                        break;

                        case '%':       // %
                                // if want to save content of comment, have to disentangle c2c3
                                while ((c=in.read())!=-1 && c!='\r' && c!='\n') {};     // comment - ignore for now, LATER make comment node
                                //in.unread(c);
                                //if (c=='\r' && (c=in.read())!='\n') in.unread(c); -- zap all whitespace
                                pdfr.eatSpace(in);
                        break;

                        //case -1:      // end of page => handled in loop test

                        default:        // doesn't catch bad commands that start with same letter as valid command
                                assert false: (char)c+" / "+c;
                                throw new ParseException("invalid command: "+(char)c+"...");
                                //break;        // corrupt input: bail out to close up spans => no need
                        }

                        // CLEAN UP for next command
                        opsi = 0;       // clear stack


                        // "painted" PATH, make another (do before span attachment, which may rely on shape created here just now)
                        if (fnewline && (fstroke || ffill || fpop)) {
                                // collect all attributes just before shape, as with font attributes and text
//System.out.println("gs.linewidth = "+gs.linewidth);
                                fnewline = fpop = false;
                                if (((gs.linewidth = (gs.linewidth < 1f? 1f: gs.linewidth))!=linewidth
                                        || gs.linecap!=linecap || gs.linejoin!=linejoin // if checking all these attributes performance drag, set flag that triggers more complete check
                                        || gs.miterlimit!=miterlimit || gs.dashphase!=dashphase || !Arrays.equals(gs.dasharray, dasharray)/*[].equals() same as ==*/)
                                ) {
                                        if (strokespan!=null) { strokespan.close(lastleaf); spancnt++; }

                                        linewidth=gs.linewidth; linecap=gs.linecap; linejoin=gs.linejoin; miterlimit=gs.miterlimit; dasharray=gs.dasharray; dashphase=gs.dashphase;
//System.out.println("line diff @ "+cmdcnt+": "+linewidth+", cap="+linecap+", join="+linejoin+", miter="+miterlimit+", "+dasharray+", "+dashphase);
                                        if (dasharray != null) {        // Neither Java nor Acrobat like dash array elements of 0f, so filter out
                                                int dai=0; for (int i=0,imax=dasharray.length; i<imax; i++) if (dasharray[i] > 0f) dasharray[dai++] = dasharray[i];
                                                if (dai < dasharray.length) { float[] newda=new float[dai]; System.arraycopy(dasharray,0, newda,0, dai); dasharray=newda; }
                                        }
                                        BasicStroke bs = new BasicStroke(linewidth, linecap, linejoin, miterlimit, dasharray, dashphase);

                                        if (Context.STROKE_DEFAULT.equals(bs)) { strokespan=null; vspancnt++; }
                                        else {
                                        //if (/*linewidth!=1.0*/(linewidth - 1.0) > 0.25) {     // in pixels
//System.out.print(/*"non-STROKE_DEFAULT: "+*/linewidth+" "+linecap+" "+linejoin+" "+miterlimit+" "+dashphase+" [ "); if (dasharray!=null) for (int i=0,imax=dasharray.length; i<imax; i++) System.out.print(dasharray[i]+" "); System.out.println("]");
                                                strokespan = (StrokeSpan)Behavior.getInstance("width"/*+linewidth/*+linejoin*/, "multivalent.std.span.StrokeSpan", null, scratchLayer);
                                                strokespan.setStroke(bs);
//System.out.println("line width = "+linewidth);
                                                // should only set what changed from before... but only one active span at a time so can't combine effects
                                                //strokespan.linewidth = linewidth;
                                                //strokespan.linecap = linecap; strokespan.linejoin = linejoin; strokespan.miterlimit = miterlimit;
                                                //strokespan.dasharray = dasharray; strokespan.dashphase = dashphase;
                                                strokespan.open(lastleaf);
                                        }
                                }
                        }
// => method

                        if (fstroke || ffill) {
                                // bug: can fill and stroke path, so don't clear path until 'n' or 'Q'
                                //assert fvalidpath: (fstroke? "stroke": "")+" "+(ffill? "fill": "")+" "+(char)c;       // I guess this is ok, just ignore?
                                if (!fshape) {
                                        pathrect=null; pathline=null; path.reset();
                                        //System.out.print("S");

                                } else if (fvalidpath) {

                                        // try to make longer paths by appending to previous.  have to have same fill/stroke uninterrupted by text or color or other changes
                                        Shape shape; Rectangle bounds; String name;
                                        //Shape shape=null; Rectangle bounds=null; String name=null;
                                        if (pathrect!=null) { assert pathlen==1: pathlen;
//System.out.print("rect "+pathrect);
                                                bounds=pathrect;
                                                shape = new Rectangle(0,0, pathrect.width,pathrect.height);
                                                name = "rect";

                                                pathrect=null;
                                                assert pathline==null: pathline;
                                                //path.reset(); // just in case had close on rect => check before closepath, because no initial SEG_MOVETO

                                        } else if (pathline!=null) {    // line -- pretty rare, actually
                                                double x1=pathline.getX1(),y1=pathline.getY1(), x2=pathline.getX2(),y2=pathline.getY2(), xmin, ymin, w2d, h2d;
                                                if (x1<=x2) { xmin=x1; w2d=x2-x1; } else { xmin=x2; w2d=x1-x2; }
                                                if (y1<=y2) { ymin=y1; h2d=y2-y1; } else { ymin=y2; h2d=y1-y2; }

                                                /*if (!fexact  && w2d*linewidth<PIXEL_INVISIBLE && w2d<PIXEL_INVISIBLE /* && w2d>0.0 && h2d>0.0* /) { System.out.print("V"); }
                                                else {*/
                                                        bounds = new Rectangle((int)Math.round(xmin), (int)Math.round(ymin), (w2d>1.0? (int)Math.ceil(w2d): 1), (h2d>1.0? (int)Math.ceil(h2d): 1));
//System.out.print("line ");    //+x1+","+y1+".."+x2+","+y2+" => "+(x1-xmin)+","+(y1-ymin)+".."+(x2-xmin)+","+(y2-ymin));
                                                        shape = new Line2D.Double(x1-xmin,y1-ymin, x2-xmin,y2-ymin);    // also used for points, but Point2D can't be drawn (it does not implement Shape)
                                                        name = DEBUG? "line"+(pathcnt): "line";
                                                //}

                                                pathline=null; path.reset();

                                        } else {        // GeneralPath
                                                Rectangle2D r2d = path.getBounds2D();   // new floats to zero path
                                                double w2d=r2d.getWidth(), h2d=r2d.getHeight(); //assert (w2d>0.0 && h2d>0.0) || gs.linewidth>0.0;      // matchingshapes.pdf has 0x0 w/width=3
//if (r2d.getWidth()==0 || r2d.getHeight()==0) System.out.println(r.width+"x"+r.height+" w/line width="+gs.linewidth);
//if (lcnt_ < 10) System.out.println("create path bounds = "+/*l.getIbbox()+" vs "+*/r);
                                                //if (r2d.width!=0 || r2d.height!=0 || gs.linewidth>0.0) {
                                                /*if (!fexact  && w2d*linewidth<PIXEL_INVISIBLE && w2d<PIXEL_INVISIBLE  && w2d>0.0 && h2d>0.0) { path.reset(); System.out.print("V"); }
                                                else {*/
                                                        bounds = new Rectangle((int)Math.round(r2d.getX()), (int)Math.round(r2d.getY()), (w2d>1.0? (int)Math.ceil(w2d): 1), (h2d>1.0? (int)Math.ceil(h2d): 1));

                                                        tmpat.setToTranslation(-r2d.getX(), -r2d.getY());       // path (0,0) relative to bbox
                                                        path.transform(tmpat);
                                                        shape = path;   //path.createTransformedShape(tmpat); -- don't need to copy
//if (r2d.getX()<0.0 || r2d.getY()<0.0) System.out.println("negative path "+Rectangles2D.pretty(r2d));  // OK
                                                        name = (DEBUG? "path"+pathcnt: "path");

                                                        path = new GeneralPath();
                                                //}
                                        }
                                                //} else path.reset();

//if (ffill && Color.WHITE.equals(fcolor) && shape instanceof Rectangle) System.out.println("danger");
/*                                      // stroke after stroke w/o attribute change => make longer path
                                        if (lastS && c=='S' && c2c3==' ' && lastleaf.sizeSticky()==0 && lastleaf instanceof FixedLeafShape) {
                                                FixedLeafShape l = (FixedLeafShape)lastleaf;
                                                Shape s = l.getShape();
if (firstS) System.out.print(s+" / "+l.getIbbox()+"  +  "+shape+" / "+bounds);
                                                GeneralPath lastpath;
                                                if (s instanceof GeneralPath) lastpath=(GeneralPath)s;
                                                else { lastpath=new GeneralPath(); lastpath.append(s, false); l.setShape(lastpath); }

                                                lastpath.append((pathrect!=null? (Shape)pathrect: pathline!=null? (Shape)pathline: (Shape)path), false);
                                                l.getIbbox().add(bounds); l.getBbox().add(bounds);
if (firstS) { System.out.println("  =  "+lastpath+" / "+l.getIbbox()); firstS=false; }
System.out.print("+");
*/
                                        //} else {
                                        if (shape!=null) {
                                                FixedLeafShape l = new FixedLeafShape(name,null, clipp, shape, fstroke, ffill); lastleaf=l; leafcnt++;
                                                l.getIbbox().setBounds(bounds); l.getBbox().setBounds(bounds); l.setValid(true);        // prevent double format
                                                pathlens[pathcnt]=pathlen; pathlen=0; if (pathcnt+1<pathlens.length) pathcnt++;
                                        }

                                } else {
//multivalent.Meta.sampledata("additional "+(fstroke? "stroke": "fill")+" on "+lastleaf);
                                        // maybe stroking and filling: if last leaf was LeafShape, set other stroke/fill flag
                                        //System.out.println("fill/stroke invalid path: "+(char)c);
                                        if (lastleaf instanceof FixedLeafShape) {
                                                FixedLeafShape l = (FixedLeafShape)lastleaf;
                                                if (fstroke) l.setStroke(true); else l.setFill(true);
                                        }
                                }
                                fvalidpath = false;
                                fstroke = ffill = false;
                                //lastS = (c=='S');     // last fill/stroke op was 'S'
                        }


                        // Color state transitions, which apply to text and splines.
                        // problem with pageroot.getLastLeaf():  textp fooled if BT..ET w/o text to anchor and just for font, pageroot fooled by form which adds last child, clipp... -- should match close(), above and at end
                        // textp fooled if BT..ET w/o text to anchor, pageroot fooled by form which adds last child, clipp... -- should match close(), above and at end
                        if (color!=newcolor/*after every command so be fast*/ && !color.equals(newcolor)) {     // pdfTeX-0.13d generates redundant color changes: 1 1 1 1 k => 1 1 1 1 k
                                //boolean fok = true;   // can come out of q..Q to different color, then back to q..Q without drawing anything, leaving a useless span... which we can reuse
                                if (sspan!=null) { if (sspan.close(lastleaf)) spancnt++; else sspan.destroy()/*moveq(null)*/; } //System.out.println("stroke span "+color);
//if (sspan!=null && sspan.getStart().equals(sspan.getEnd())) System.out.println("0-len stroke: "+sspan.getName()/*(sspan.stroke.getRed()/255.0)+" "+(sspan.stroke.getGreen()/255.0)+ " "+(sspan.stroke.getBlue()/255.0)*/);

                                color = newcolor; assert color!=null: color;

                                //if (Color.BLACK.equals(color)) { sspan=null; vspancnt++; /*System.out.print("S");*/ } // X don't make span if same as stylesheet foreground setting => nested forms inherit prevailing color, as on Altona, ugh!
                                //else {
                                        sspan = (SpanPDF)Behavior.getInstance((DEBUG? "stroke "+Integer.toHexString(color.getRGB()): "stroke"), "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                        sspan.stroke = color;
                                        sspan.open(lastleaf);
                                //}
                        }
                        if (fcolor!=newfcolor && !fcolor.equals(newfcolor)) {
                                if (fillspan!=null) { if (fillspan.close(lastleaf)) spancnt++; else fillspan.destroy()/*moveq(null)*/; }        //System.out.println("fill "+fcolor+" @ "+(char)c);

                                fcolor = newfcolor; assert fcolor!=null: fcolor;

                                //if (Color.BLACK.equals(fcolor)) { fillspan=null; vspancnt++; /*System.out.print("F");*/ }
                                //else {
                                        fillspan = (SpanPDF)Behavior.getInstance("fill"/*+"_"+Integer.toHexString(fcolor.getRGB())*/, "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                        fillspan.fill = fcolor;
                                        fillspan.open(lastleaf);
                                //}
                        }
                }
        }

/*      if (PERF && pathcnt>0) {
                System.out.print(pathcnt+" paths:  ");
                pathlens[pathcnt++]=Integer.MAX_VALUE;/*sentinal* / Arrays.sort(pathlens, 0, pathcnt);
                for (int i=0, j=0, lasti=pathlens[i]; i<pathcnt; i++) if (pathlens[i]!=lasti) { System.out.print(lasti+"x"+j+"  "); j=1; lasti=pathlens[i]; } else j++;
                System.out.println();
        }*/


        // CLEAN UP at end of page / form
        // A. text only
//System.out.println("closeAll on "+pdf);
        spancnt += Span.closeAll(clipp);
//Span.dumpPending();

/*      if (fontspan!=null /*&& !fnewfont/*exists some unspanned text -- could have 'cm' with no more text* /) { fontspan.close(lastleaf); spancnt++; } // text only
        if (Trspan!=null) { Trspan.close(lastleaf); spancnt++; }
        if (strokespan!=null) { strokespan.close(lastleaf); spancnt++; }        // splines only
        if (sspan!=null) { sspan.close(lastleaf); spancnt++; }  // all objects
        if (fillspan!=null) { fillspan.close(lastleaf); spancnt++; }
        + possible unclosed marked sections
*/
// maybe one big SpanPDF with everything, and check against virgin SpanPDF to see if should keep

        if (false && multivalent.Meta.MONITOR && ocrimgs/*or doc*/!=null/*Type 3 use*/) {
                /*if (cmdcnt>0)*/ System.out.println(cmdcnt+" cmds, "+leafcnt+" leaves, "+spancnt+" spans ("+vspancnt+" saved), "+concatcnt+" concats, "+pathcnt+" paths, time="+(System.currentTimeMillis()-start));   // && # of leaves and spans
                //for (int i=INTS_MIN; i<=INTS_MAX; i++) if (ihist[i-INTS_MIN]>5) System.out.print(i+"/"+ihist[i-INTS_MIN]+" ");  System.out.println();
                //System.out.println("dcnt = "+dhist+", saved = "+dnot);
        }

//System.out.print("bS => "); clipp.dump();
        return cmdcnt;
  }


  private Leaf cmdDo(String xname, Dict xores, Dict resources,  GraphicsState gs, FixedIClip clipp,  double[] d, List<FixedLeafImage> ocrimgs) throws IOException,ParseException {
        Leaf l = null;

        PDFReader pdfr = pdfr_;
        IRef iref = (IRef)xores.get(xname);
        Dict xobj = (Dict)pdfr.getObject(iref); //assert xobj!=null: xname+" in "+xores+" -> "+iref;
        if (xobj==null) return null;    // ep/2002/p134-lehtonen.pdf
        String subtype = (String)pdfr.getObject(xobj.get("Subtype"));   // probably always a literal, but getObject cheap
        AffineTransform ctm = gs.ctm;
//System.out.println("XObject: "+xname+": "+xobj);

        int hints = getHints();
        if ("Image".equals(subtype)) {
//System.out.print(xname+" => "+iref+", ctm="+ctm);
                if ((HINT_NO_IMAGE&hints)!=0 && (HINT_NO_SHOW&hints)!=0) {
                        Rectangle r = new Rectangle(0,0, (int)ctm.getScaleX(), (int)-ctm.getScaleY());
                        new FixedLeafBlock(xname,null, clipp, r);       // always create a leaf in case only child of INode
//System.out.println("block for image: "+ctm.getScaleX()+" x "+ctm.getScaleY());

                } else try {
                        BufferedImage img = pdfr.getImage(iref/*NOT xobj*/, ctm, gs.fillcolor);
                        if (img!=null) {
                                FixedLeafImage imgl = appendImage(xname, clipp, img, ctm);

                                String imgtype = Images.getFilter(xobj, pdfr/*this*/);
                                if (("CCITTFaxDecode".equals(imgtype) || "JBIG2Decode".equals(imgtype)) && Boolean.TRUE!=pdfr.getObject(xobj.get("ImageMask")) && ocrimgs!=null/*during Type 3*/ /*&& .width>1 && height>1*/) ocrimgs.add(imgl);
//System.out.println(imgl.getIbbox()+" vs "+cropbox_);

                                l = imgl;

                        } else multivalent.Meta.sampledata("bad image "+xname+" => "+iref+", filter="+pdfr.getObject(xobj.get("Filter")));      // i/o error or JPEG flavor not supported by Java
                } catch (OutOfMemoryError err) {
                        System.err.println("Image too large -- increase JVM memory with -Xmx=... ["+iref+": "+xobj+"] x "+ctm);
                        //*if (DEBUG) err.printStackTrace();
                }

        } else if ("Form".equals(subtype)) {    // not like HTML form, rather more like subroutine
//System.out.println("Form XObject, bbox="+xobj.get("BBox"));
                // q, concat /Matrix with CTM, clip according to /BBox, paint content stream, Q
                // check that "FormType" is 1
                AffineTransform formAT = new AffineTransform(ctm);
                Object[] oa = (Object[])pdfr.getObject(xobj.get("Matrix"));
                if (oa!=null) { assert oa.length==6;
                        getDoubles(oa,d,6); formAT.concatenate(new AffineTransform(d[0], d[1], d[2], d[3], d[4], d[5]));
//if (!tmpat.isIdentity()) System.out.println(ctm+" => "+formAT);
                        //if (!tmpat.isIdentity()) multivalent.Meta.sampledata("Form with non-Identity /Matrix "+xname+"  "+tmpat);     // concat to formAT
                }
                // clip to BBox
                FixedIClip formClip = clipp;
                Object o = pdfr.getObject(xobj.get("BBox"));
                /*if (o!=null)--required {*/ assert CLASS_ARRAY==o.getClass();
                        Rectangle r = array2Rectangle((Object[])o, formAT);
//System.out.println("bbox = "+r);
                        formClip = new FixedIClip(xname/*"formclip"*/, null, clipp, new Rectangle(0,0, r.width, r.height), r);
                //}
                if (xobj.get("Resources")==null) xobj.put("Resources", resources);      // if no resources, inherit Page's

                gs.ctm = formAT;
                InputStreamComposite formin = pdfr.getInputStream(iref);
                // should push & pop graphics stack so form inherits attributes, rather than starting with defaults (except for CTM)
                /*int fcmdcnt =*/ buildStream(xobj, /*pageroot* /clipp*/formClip, gs, formin, ocrimgs);
                formin.close();
                //if (fcmdcnt==0) System.out.println("empty Form: "+iref);  -- EPodd has a lot of these => can have commands but still no additions to tree
//if (formClip.size()==7) dumpIbbox(formClip, 0);

                //if (formClip.size()==0 && formClip!=clipp) formClip.remove();
                //else l = clipp.getLastLeaf(); // forms self-contained, but marked content across form
                //if (lastform!=null) lastleaf=lastform;
                l = formClip.getLastLeaf();
                if (l==null && formClip!=clipp) formClip.remove();
//System.out.println("last leaf = "+l+" (==null? "+(l==null)+")");

        } else if ("Group".equals(subtype)) {   // /Type Group /S /Transparency
                multivalent.Meta.sampledata("Group Form "+xname);

        } else if ("PS".equals(subtype)) {      // "Note: Since PDF 1.3 encompasses all of the Adobe imaging model features of the PostScript language, there is no longer any reason to use PostScript XObjects.  This feature is likely to be removed from PDF in a future version."
                //System.err.println("contains embedded PostScript, which is obsolete in PDF -- redistill"); -- only shown in printed version anyhow

        } else { assert false: subtype; }

        return l;
  }


  private void cmdgs(Dict gsdict, Dict fontres, AffineTransform ctm, double[] d,  GraphicsState gs) throws IOException {
        PDFReader pdfr = pdfr_;
        Object o = pdfr.getObject(gsdict.get("Type"));  assert o==null || "ExtGState".equals(o);

        if ((o=pdfr.getObject(gsdict.get("Font")))!=null) {     // array - [font-dict-indirect-ref size] (same as Tf command)
                Object[] oa = (Object[])o;
                gs.fontdict = (Dict)pdfr.getObject(fontres.get(oa[0]));  assert gs.fontdict!=null: oa[0]+" not in "+fontres;
                gs.pointsize = ((Number)oa[1]).doubleValue();
        }
        if ((o=pdfr.getObject(gsdict.get("LW")))!=null) {       // number - line width
                d[0]=((Number)o).doubleValue(); d[1]=0.0; ctm.deltaTransform(d,0, d,0, 1);
                gs.linewidth = (float)Math.abs(d[1]==0.0? d[0]: d[0]==0.0? d[1]: Math.sqrt(d[0]*d[0] + d[1]*d[1]));
        }
        if ((o=pdfr.getObject(gsdict.get("LC")))!=null) gs.linecap = ((Number)o).intValue();    // integer - line cap style
        if ((o=pdfr.getObject(gsdict.get("LJ")))!=null) gs.linejoin = ((Number)o).intValue();   // integer - line join style
        if ((o=pdfr.getObject(gsdict.get("ML")))!=null) gs.miterlimit = ((Number)o).floatValue();       // number - miter limit

        if ((o=pdfr.getObject(gsdict.get("D")))!=null) {        // array - line dash pattern
                Object[] oa0 = (Object[])o, oa = (Object[])oa0;
                if (oa==OBJECT_NULL || oa.length==0) gs.dasharray = null; else getFloats(oa, gs.dasharray=new float[oa.length], oa.length);
                gs.dashphase = ((Number)oa0[1]).floatValue();
        }

        if ((o=pdfr.getObject(gsdict.get("RI")))!=null) gs.renderingintent = (String)o; // name - rendering intent

        /* overprinting not really applicable to screen
        if ((o=pdfr.getObject(gsdict.get("OP")))!=null) {}      // boolean - overprinting everywhere
        if ((o=pdfr.getObject(gsdict.get("op")))!=null) {}      // boolean - overprint on non-stroke
        if ((o=pdfr.getObject(gsdict.get("OPM")))!=null) {}     // integer - overprint mode     */

        /* not applicable -- RGB=>CMYK so print only
        if ((o=pdfr.getObject(gsdict.get("BG2")))!=null ||      // function or name - same as BG except can be "Default" (BG2 takes precedence)
                (o=pdfr.getObject(gsdict.get("BG")))!=null) {}  // function - black-generation function
        if ((o=pdfr.getObject(gsdict.get("UCR2")))!=null ||     // function or name - same as UCR except can be "Default" (UCR2 takes precedence)
                (o=pdfr.getObject(gsdict.get("UCR")))!=null) {} // function - undercolor-removal function       */

        if ((o=pdfr.getObject(gsdict.get("TR2")))!=null ||      // function or name - same as TR + "Default" (TR2 takes precedence)
                (o=pdfr.getObject(gsdict.get("TR")))!=null) {   // function, array - transfer function
        }
        if ((o=pdfr.getObject(gsdict.get("HT")))!=null) {       // dictionary - halftone dictionary or stream
        }
        if ((o=pdfr.getObject(gsdict.get("FL")))!=null) gs.flatness = ((Number)o).intValue();   // number - flatness tolerance
        if ((o=pdfr.getObject(gsdict.get("SM")))!=null) gs.smoothness = ((Number)o).doubleValue();      // number - smoothness tolerance

        if ((o=pdfr.getObject(gsdict.get("SA")))!=null) {       // boolean - apply auto stroke adjustment
                //RenderingHints.KEY_STROKE_xxx ?
        }
        if ((o=pdfr.getObject(gsdict.get("BM")))!=null) {       // name or array - blend mode
        }
        if ((o=pdfr.getObject(gsdict.get("SMask")))!=null) {    // dictionary or name - soft mask
        }
        if ((o=pdfr.getObject(gsdict.get("CA")))!=null) {       // number - stroking alpha constant
                gs.alphastroke = ((Number)o).floatValue();
if (gs.alphastroke != 1.0) multivalent.Meta.sampledata("transparency (CA - stroking alpha)");
        }
        if ((o=pdfr.getObject(gsdict.get("ca")))!=null) {       // number - nonstroking alpha constant
                gs.alphanonstroke = ((Number)o).floatValue();
if (gs.alphanonstroke != 1.0) multivalent.Meta.sampledata("transparency (ca - nonstroking alpha)");
        }
        if ((o=pdfr.getObject(gsdict.get("AIS")))!=null) {      // boolean - alpha source flag
        }
        if ((o=pdfr.getObject(gsdict.get("TK")))!=null) {       // boolean - text knockout
        }
  }


  private FixedLeafImage appendImage(String name, INode parent, BufferedImage img, AffineTransform ctm) {
        FixedLeafImage l = new FixedLeafImage(name,null, parent, img);  //lastleaf=l; leafcnt++;
//System.out.println("img "+img.getWidth()+"x"+img.getHeight()+" @ "+img.getMinX()+","+img.getMinY());
        double majorx = Math.abs(ctm.getScaleX()) > Math.abs(ctm.getShearX())? ctm.getScaleX(): ctm.getShearX(),        // don't be fooled by a little shearing
                   majory = Math.abs(ctm.getScaleY()) > Math.abs(ctm.getShearY())? ctm.getScaleY(): ctm.getShearY();
        double left = ctm.getTranslateX() - (majorx<0.0/*ctm.getScaleX()<0.0 ^ ctm.getShearX()<0.0*/? img.getWidth(): 0.0),
                top = ctm.getTranslateY() - (majory<0.0? img.getHeight(): 0.0);
/*      srcpt.setLocation(1.0, 1.0); ctm.deltaTransform(srcpt, transpt);        // valid method also
System.out.println(srcpt+" => "+transpt);
        double left = ctm.getTranslateX() - (transpt.getX()>=0.0? 0.0: -transpt.getX()),
                top = ctm.getTranslateY() - (transpt.getY()<0.0? -transpt.getY(): 0.0);
System.out.println(ctm+": "+img.getWidth()+"x"+img.getHeight()+" => @"+left+","+top);*/
        l.getIbbox().setBounds((int)left, (int)top, img.getWidth(), img.getHeight());
        // could also l.bbox.setBounds(l.getIbbox()); l.setValid(true);
//System.out.println("adding image "+name+" "+" @ "+l.getIbbox()+" "+ctm);

        return l;
  }



  /** Check that all leaves are valid and bbox.equals(ibbox). */
  private boolean checkTree(String id, INode pageroot) {
        for (Leaf l = pageroot.getFirstLeaf(), endl = (l!=null? pageroot.getLastLeaf().getNextLeaf(): null); l!=endl; l=l.getNextLeaf()) {
                Fixed f = (Fixed)l;
                assert l.isValid() || !(l instanceof FixedLeafUnicodeKern): id+": "+l+" "+l.getClass().getName()+" "+l.getBbox();       // not images
                //assert l.bbox!=f.getIbbox(): l;
                //assert l.bbox.equals(f.getIbbox()): l.bbox+" vs "+f.getIbbox();
        }

        return true;    // OK
  }



// works except screws up spans.  keep around in case clever in the future
  private static final Comparator<Node> YCOMP = new Comparator<Node>() {
        public int compare(Node n1, Node n2) {
                int y1 = ((Fixed)n1.getFirstLeaf()).getIbbox().y;
                int y2 = ((Fixed)n2.getFirstLeaf()).getIbbox().y;
                return y1 - y2;
        }
  };

  /** Sort lines by increasing Y. */
  // problem cases: two columns within same BT..ET
  private void sortY(INode textp) {
        if (!DEBUG) return;
        int size = textp.size(); if (size<=1) return;

        Node[] children = new Node[size];
        for (int i=0; i<size; i++) children[i] = textp.childAt(i);
        Node[] c0 = (Node[])children.clone();   // see if makes a difference (most PDFs written in top-to-bottom reading order?)

        Arrays.sort(children, YCOMP);

        if (Arrays.equals(children, c0)) return;
System.out.println("inc Y on "+textp.getFirstLeaf());

        // collect spans
        List<Span> ends=new ArrayList<Span>(10), starts=new ArrayList<Span>(10), swaps=new ArrayList<Span>(10);
        for (Leaf l=textp.getFirstLeaf(), endl=textp.getLastLeaf().getNextLeaf(); l!=endl; l=l.getNextLeaf()) {
                for (int i=0,imax=l.sizeSticky(); i<imax; i++) {
                        Mark m = l.getSticky(i);
                        Object o = m.getOwner();
                        if (o instanceof Span) {
                                Span span = (Span)o;
                                INode linep = l.getParentNode();  assert "line".equals(linep.getName()): linep.getName();
                                if (m == span.getStart()) {
                                        Mark end = span.getEnd();
                                        if (linep.contains(end.leaf)) {}        // ok as is
                                        else if (textp.contains(end.leaf)) swaps.add(span);
                                        else { starts.add(span); System.out.println(span+ " starts @ "+m.leaf.getName()); }

                                } else {        // end
                                        Mark start = span.getStart();
                                        if (linep.contains(start.leaf)) {}      // ok as is (already saw this earlier in loop)
                                        else if (textp.contains(start.leaf)) swaps.add(span);
                                        else { ends.add(span); System.out.println(span+" ends @ "+m.leaf.getName()); }
                                }
                        }
                }
        }


        // re-set children
//      textp.removeAllChildren();
//      for (int i=0; i<size; i++) textp.appendChild(children[i]);


        // reattach spans
        // 1. ok as is: include subtree but without transition within it, or contained within same line
        // 2. cauterize and replicate: spans that start or end within subtree
        // 3. swap endpoints: spans that start and end in different line
System.out.println("ends="+ends.size()+", starts="+starts.size()+", swaps="+swaps.size());
  }



  // => Anno class?
  private void createAnnots(Dict pagedict, INode root) throws IOException {
        Object o = pdfr_.getObject(pagedict.get("Annots"));
        Browser br = getBrowser();
//System.out.println(annots.length+" annots @ "+System.currentTimeMillis());
        if (br!=null && o!=null && OBJECT_NULL!=o) for (Object anno: (Object[])o) {
//System.out.print(" "+i); System.out.flush();
                br.eventq(new SemanticEvent(br, Anno.MSG_CREATE, pdfr_.getObject(anno), this, root));
        }
//System.out.println("done "+System.currentTimeMillis());
  }



  // PROTOCOLS

  /**
        Parses individual page indicated in {@link Document#ATTR_PAGE} of <var>parent</var>'s containing {@link Document}
        and returns formatted document tree rooted at <var>parent</var> as <a href='#doctree'>described</a> above.
        @return root of PDF subtree under <var>parent</var>
  */
  public Object parse(INode parent) throws IOException, ParseException {
        Document doc = parent.getDocument();
        if (getURI()==null)
                return parent;
        if (pdfr_ == null) init(doc);
        if (fail_ != null) { new LeafUnicode("Error opening PDF: "+fail_,null, parent); return parent; }
        if (idCheck_) return new INode("pdf", null, parent);
//System.out.println("layer = "+getLayer());

        //StyleSheet ss = new StyleSheet();   //PDFStyleSheet();
        //ss.setCascade(doc);
        //doc.setStyleSheet(ss);
        StyleSheet ss = doc.getStyleSheet();
        // we don't pick up PDF.css because not a CSS Style Sheet
        CLGeneral cl = (CLGeneral)ss.get("pdf");
        if (cl==null) { cl=new CLGeneral(); ss.put("pdf", cl); }
        cl.setStroke(Color.BLACK);      // augment CSS to be able to set this in stylesheet?
        cl.setBackground(Color.WHITE);  // refresh from last document -- FIX
        cl.setForeground(Color.BLACK);  // refresh from last document -- FIX


        // at start of each page, check that underling file still valid and has not changed
        PDFReader pdfr = pdfr_;
        //pdfr.refresh();       // may have regenerated


        if (!isAuthorized()) {
                // have to ask user before continuing -- LATER
                // put up password dialog
                //requestPassword();

                return new LeafUnicode("can handle encrypted PDFs only with null password for now",null, doc);
        }


        // first time only
        if (doc.getAttr(Document.ATTR_PAGECOUNT)==null) {
                // for Multipage protocol
                doc.putAttr(Document.ATTR_PAGECOUNT, Integer.toString(pdfr.getPageCnt()));

                Dict info = pdfr.getInfo();     //(Dict)pdfr.getObject(trailer.get("Info"));
                if (info!=null) {
//if (DEBUG) System.out.println("creator = "+info.get("Creator")+" / producer = "+info.get("Producer"));
                        Object o=null;
                        for (int i=0,imax=METADATA.length; i<imax; i++) if ((o=pdfr.getObject(info.get(METADATA[i])))!=null) doc.putAttr(METADATA[i], o.toString());
                }
        }


        //new LeafUnicode("PDF "+doc.getAttr(Document.ATTR_URI)+", page "+doc.getAttr(Document.ATTR_PAGE), null, parent);
        if (doc.getAttr(Document.ATTR_PAGE)==null) return new LeafUnicode("Loading...",null, parent);


        //Dict page = getPage(Integers.parseInt(doc.getAttr(Document.ATTR_PAGE),1));

        // rectangle, expressed in default user space units, defining the maximum imageable area
        INode pdf = new INode("pdf",null, parent);
        FixedI mediabox = new FixedI("MediaBox",null, pdf);     // need to move regions around, which INode doesn't do

        try {
                AffineTransform z = AffineTransform.getScaleInstance(getZoom(), getZoom());
                buildPage(Integers.parseInt(doc.getAttr(Document.ATTR_PAGE, "1"), 1), mediabox, z);
        } catch (Exception e) {
                if (DEBUG) e.printStackTrace();
                if (parent.size()==0) new LeafUnicode("bad page: "+e.getMessage(),null, parent);
        }
        //catch (Error err) { err.printStackTrace(); }
        //mediabox.addObserver(this);
        pdf.addObserver(this);

        return pdf;
  }



//+ Protocols: restore, format, semanticevent,

/*  public void buildBefore(Document doc) {
        if (pdfr_==null) init(doc);
        super.buildBefore(doc);
  }*/

  /**
        If URI ref is to named destination, set intial page to that.

        <p>(<code>...#page=nnn</code> handled by {@link multivalent.std.ui.Multipage}.
        The Acrobat plug-in supports a highlight file referred to like so <code>http://www.adobe.com/a.pdf#xml=http://www.adobe.com/a.txt</code>;
        but that's awkward and nobody uses it, so it's not supported.)
  */
  private void init(Document doc) {
/*  public void restore(ESISNode n, Dict attr, Layer layer) {
        super.restore(n, attr, layer);*/
//try { System.out.println("init on "+getFile()); } catch (IOException ioe) { System.out.println("IOE: "+ioe); }

        String ref = null;
        ref = getURI().getFragment();
//System.out.println("PDF initial page = "+doc.getAttr(Document.ATTR_PAGE));
//try { System.out.println("PDF getFile = "+getFile()+" for new PDFReader"); } catch (IOException ignore) {}
        try {
                pdfr_ = new PDFReader(getInputUni(), idCheck_);
        } catch (Exception fail) {      // IOException and ParseException
                if (DEBUG) { fail.printStackTrace(); System.exit(1); }
                System.out.println(fail);
                fail_ = fail.getLocalizedMessage();
                return;
         }
//System.out.println("pdfr_ = "+pdfr_);

        if (idCheck_)
            return;

        if (ref!=null && doc.getAttr(Document.ATTR_PAGE)==null) try {   // set initial page before multipage and before forward/backward
                PDFReader pdfr = pdfr_;

                Object dest = Action.resolveNamedDest(new StringBuffer(ref), pdfr);
                if (dest==null) Action.resolveNamedDest(ref, pdfr);     // try old style

//System.out.println(ref+" PDF=> "+dest);
                if (dest!=null) {
                        if (CLASS_DICTIONARY==dest.getClass()) dest = pdfr_.getObject(((Dict)dest).get("D"));   // extract from << /D [...] >>
//System.out.println(" /D=> "+dest+", "+dest.getClass().getName()+" "+(PDF.CLASS_ARRAY==dest.getClass()));
                        if (CLASS_ARRAY==dest.getClass()) {     // [page ... /XYZ left top zoom, /Fit, /FitH top, /FitV left, /FitR left bottom right top, /FitB, /FitBH top, /FitBV left
                                Object page = pdfr_.getObject(((Object[])dest)[0]);
//System.out.println(" [0]=> "+page);   // +" => "+(page!=null? String.valueOf(pdfr.getPageNum((Dict)page)): "XXX"));
                                if (/*page!=null &&*/ CLASS_DICTIONARY==page.getClass()) doc.putAttr(Document.ATTR_PAGE, String.valueOf(pdfr.getPageNum((Dict)page)));
//System.out.println("initial page = "+pdfr.getPageNum((Dict)page));
                        }
                }
        } catch (/*IO, Parse*/Exception ignore) { /*System.err.println(ignore); ignore.printStackTrace();*/ }

        GoFast = Booleans.parseBoolean(getPreference("pdfGoFast", "true"), true);
  }


  /** Enlarge content root to MediaBox. */
  public boolean formatAfter(Node node) {
//System.out.println(node.bbox+" => "+cropbox_);
        //node.bbox.setBounds(cropbox_);
        Rectangle bbox = node.bbox;
        //bbox.width = Math.max(bbox.width, cropbox_.width);
        //bbox.height = Math.max(bbox.height, cropbox_.height);
        node.bbox.setBounds(0,0, Math.max(node.bbox.x+node.bbox.width, cropbox_.x+cropbox_.width), Math.max(node.bbox.y+node.bbox.height, cropbox_.y+cropbox_.height));
        return super.formatAfter(node);
  }


  /** "Dump PDF to temp dir" in Debug menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
        if (super.semanticEventBefore(se,msg)) return true;
        else if (multivalent.gui.VMenu.MSG_CREATE_VIEW==msg) {
                VCheckbox cb = (VCheckbox)createUI("checkbox", "Accelerate PDF (less accurate)", "event "+MSG_GO_FAST, (INode)se.getOut(), VMenu.CATEGORY_MEDIUM, false);
                cb.setState(GoFast);

        } else if (multivalent.devel.Debug.MSG_CREATE_DEBUG==msg) {
                VCheckbox cb = (VCheckbox)createUI("checkbox", "Dump PDF to temp dir", "event "+MSG_DUMP, (INode)se.getOut(), VMenu.CATEGORY_MEDIUM, false);
                cb.setState(Dump_);
        }
        return false;
  }


  /** Implements {@link #MSG_DUMP}, {@link #MSG_USER_PASSWORD}, {@link #MSG_OWNER_PASSWORD}. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
        Object arg = se.getArg();
        if (MSG_DUMP==msg) {
                Dump_ = Booleans.parseBoolean(arg, !Dump_);

        } else if (MSG_USER_PASSWORD==msg) {    // as from dialog
                /*Encrypt e = pdfr_.getEncrypt();
                if (arg instanceof String && !e.isAuthorized()) {
                        if (!e.authUser((String)arg)) requestPassword();
                }*/

        } else if (MSG_OWNER_PASSWORD==msg) {
                /*Encrypt e = pdfr_.getEncrypt();
                if (arg instanceof String && !e.isAuthorized()) {
                        if (!e.authOwner((String)arg)) requestPassword();
                }*/

        } else if (MSG_GO_FAST==msg) GoFast = !GoFast;  // => hints

        return super.semanticEventAfter(se,msg);
  }

  public void close() throws IOException {
//System.out.println("close PDF / PDFReader "+getURI());
        if (pdfr_!=null) { pdfr_.close(); pdfr_ = null; }
        super.close();
  }


  /** If document is encrypted with non-null password, throw up dialog requesting user to enter it. */
  void requestPassword() {
  }

  /** Debugging.
  void dumpIbbox(Fixed n, int level) {
        System.out.print("                                   ".substring(0, level*3));
        System.out.println(((Node)n).getName()+" "+Rectangles2D.pretty(n.getIbbox()));
        if (n instanceof INode) {
                INode p = (INode)n;
                for (int i=0,imax=p.size(); i<imax; i++) dumpIbbox((Fixed)p.childAt(i), level+1);
        }
  } */

    private static final Map<String, String> PDF_SUFFIXES;

    static {
        PDF_SUFFIXES = new HashMap<String, String>();
        PDF_SUFFIXES.put("pdf", "application/pdf");
        PDF_SUFFIXES.put("fdf", "application/vnd.fdf");
        PDF_SUFFIXES.put("eps", "application/eps");
    }

    // Use fabio's code for ID
    boolean fabio = true;

    @Override
    protected Document newDocument(String path, Confidence level) {
        Document doc = super.newDocument(path, level);
        if (level == PROCESS) {
            // start processing at the first page
            doc.putAttr(Document.ATTR_PAGE, "1");
        }
        return doc;
    }

    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        // I am trying a different approach from yours, so instead of modifying your code, I use
        // this temporary hack to keep both codes live in the system.
        if (fabio) {
            return fabioId(min, max, path, complete);
        }

        SortedSet<IDInfo> infos = validateParams(min, max);

        try {
            // MAGIC: use idCheck, which does a very quick parse
            // HEURISTIC: get up to 100 objects
            // PARSE: get all objects and streams
            // PROCESS: page through the document
            Confidence checkedAt = null;
            if (inRange(min, PROCESS, max)) {
                INode top = (INode) parseDocument(path, PROCESS);
                // Parse did not succeed
                if (pdfr_ == null)
                    return infos;
                checkedAt = PROCESS;
                // Page numbers start at 1, not 0
                // start with page 2 -- already built first in parse()
                for (int i = 2; !failed() && i <= pdfr_.getPageCnt(); i++) {
                    AffineTransform aft = new AffineTransform();
                    buildPage(i, top, aft);
                }
            } else if (inRange(min, HEURISTIC, max)) {
                boolean parse = max.compareTo(PARSE) >= 0;
                parseDocument(path, parse ? PARSE : HEURISTIC);
                // Parse did not succeed
                if (pdfr_ == null)
                    return infos;
                checkedAt = parse ? PARSE : HEURISTIC;
                int end = pdfr_.getObjCnt();
                if (!parse)
                    end = Math.max(end, 100);
                byte[] buf = new byte[8 * 1024];
                for (int i = 0; !failed() && i < end; i++) {
                    Object obj = pdfr_.getObject(i);
                    if (obj != null && parse && !failed()) {
                        InputStream in = null;
                        try {
                            in = pdfr_.getInputStream(obj);
                            while (!failed() && in.read(buf) > 0)
                                continue;
                        } catch (ClassCastException ignored) {
                            // Means the object isn't type to get a stream from
                            // There is no simple way to ask this question
                            // This is OK
                        } finally {
                            IOUtils.closeQuietly(in);
                        }
                    }
                }
            } else if (inRange(min, MAGIC, max)) {
                idCheck_ = true;
                parseDocument(path, MAGIC);
                // Parse did not succeed
                if (pdfr_ == null)
                    return infos;
                checkedAt = MAGIC;
            } else if (inRange(min, SUFFIX, max)) {
                String type = lookupSuffix(path, PDF_SUFFIXES);
                if (type != null)
                    infos.add(new IDInfo(SUFFIX, this, type));
                // avoid going through the failed() "if" below
                return infos;
            }

            if (!failed()) {
                String version = pdfr_.getVersion().toString();
                String magic = pdfr_.getMagic();
                String which;
                if (magic.charAt(1) == 'P')
                    which = "pdf";
                else if (magic.charAt(1) == 'F')
                    which = "fdf";
                else
                    which = "eps";
                infos.add(new IDInfo(checkedAt, this, PDF_SUFFIXES.get(which),
                        version, which.toUpperCase()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return infos;
    }

    private SortedSet<IDInfo> fabioId(Confidence min, Confidence max,
            String path, boolean complete) {

        SortedSet<IDInfo> infos = validateParams(min, max);
        try {
            if (inRange(min, PROCESS, max)) {
                //pdfr_ = new PDFReader(getInputUni(), false);
                Document doc = new Document(path, null, null);
                doc.uri = new URI(path);
                doc.putAttr(Document.ATTR_PAGE, String.valueOf(1));
                parse(doc);

                // Didn't parse
                if (pdfr_ == null)
                    return infos;

                Fab4utils.getTextSpaced(doc);
                int numPages = pdfr_.getPageCnt();
                for (int p = 2; p <= numPages; p++) {
                    doc.putAttr(Document.ATTR_PAGE, String.valueOf(p));
                    parse(doc);
                    //System.out.println(Fab4utils.getTextSpaced(doc));
                    doc.removeAllChildren();
                    doc.getLayer(Layer.SCRATCH).clear();
                    doc.getLayers().buildBeforeAfter(doc);
//                    Dict page = pdfr_.getPage(p);
//                    AffineTransform aft = new AffineTransform();
//                    Document doc = new Document(path, null, null);
//                    doc.uri = new URI(path);
//                    INode pdf = new INode("pdf",null, doc);
//                    FixedI mediabox = new FixedI("MediaBox",null, pdf);
//
//                    buildPage(p, doc, aft);
//                    Fab4utils.getTextSpaced(doc);
                }

                infos.add(new IDInfo(PARSE, this, PDF_SUFFIXES.get(getType()),
                        pdfr_.getVersion().toString(), getType()));
            } else if (inRange(min, HEURISTIC, max)) {
                pdfr_ = new PDFReader(getInputUni(), false);
                infos.add(new IDInfo(HEURISTIC, this, getType()));
                // procedd dommon PDF information, not pages
            } else if (inRange(min, MAGIC, max)) {
                // MAGIC: use idCheck, which does a very quick parse
                pdfr_ = new PDFReader(getInputUni(), true);
                infos.add(new IDInfo(MAGIC, this, getType()));
            } else if (inRange(min, SUFFIX, max)) {
                String type = lookupSuffix(path, PDF_SUFFIXES);
                if (type != null)
                    infos.add(new IDInfo(SUFFIX, this, type));
            }
        } catch (ParseException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            if (pdfr_ != null) {
                try {
                    pdfr_.close();
                } catch (IOException ignored) {
                }
                pdfr_ = null;
            }
        }

        return infos;
    }

    private String getType() {
        String magic = pdfr_.getMagic();
        String which;
        if (magic.charAt(1) == 'P')
            which = "pdf";
        else if (magic.charAt(1) == 'F')
            which = "fdf";
        else
            which = "eps";
        return which;
    }

    private boolean failed() {
        return fail_ != null;
    }
    
    ///SAM
    
    /**
     * given a 'content' node, a center point, return ~ numOfLines/2 lines above the center point and numOfLines/2 below the center point in one column of the pdf
     * also, return the last two headers (sections/subsections) seen in these/or before these numOfLines lines.
     * @return pairs of key,value. keys are: lines (count:numOfLines), 'section1', 'section2' 
     */
    public static HashMap<String,String> getRelatedTextSnapshot(Node docrootcontent, double center_x, double center_y,float zoom, int numOfLines){
    	
    	HashMap<String,String> info = new HashMap<String, String>();
		
    	String[] lastSectionsFound = new String[2]; //"Title";
		lastSectionsFound[0] = "Title";
		lastSectionsFound[1] = "Authors";
		String previousline = null;
		int preTextSize = 0;
		boolean preallsamesize = true;
		boolean beforeabstract = true;
		int err = 1; //error in same text sizes in consequtive lines
		int errIn1Line = 0; //error in same text sizes in one line
    	
		double sizeofline = 305 * (zoom/1.25); // ??! anyway to compute? (this considered with: 320 for long lines, head to head, -15 for first paragraph lines
		double distanceBetweenlines = 15 * (zoom/1.25); // ??! anyway to compute?		
		
		String textsnapshot = "";//"<html> <title> Snapshot of annotated section </title> <body> ";
		
		String doctype = null; 
		
		Node rootcontent = null;
		if( docrootcontent != null ){
			if((rootcontent = docrootcontent.findBFS("html")) != null)
				doctype = "html";			
			else if((rootcontent = docrootcontent.findBFS("pdf"/*,null,null,2*/)) != null)
				doctype = "pdf";
			/*if( rootcontent != null && docrootcontent. == -1 ) //if 'pdf' or 'html' are not direct childs
				doctype = null;*/
		}
		

		try{
		
		if( doctype != null && doctype.equals("pdf")){
			INode text = (INode) rootcontent.findBFS("text");
			if( text != null){
//				Node line = text.getPrevNode(); //last line! (DFS)
				INode line = (INode) text.getFirstChild();
								
				int linescount = 0;
				
				while( line != null ){
					/*for( ; line != null && !line.getName().equals("line") ; line = (INode) line.getNextSibling()){
						System.out.print("");
					}*/
					if(!line.getName().equals("line"))
						break;
					/*if( line == null )
						break;*/
					
					//find section
					Node leaf = line.getFirstLeaf();
					
					String thisline = "";
//					boolean aheader = false;
					
					//take line and related statistics:
					int firstleafsizeofline = 0;
					
					if(leaf instanceof LeafText){								
//						firstleafsizeofline = ((LeafUnicode)leaf).baseline;
						firstleafsizeofline = ((LeafText)leaf).baseline;
					}					
					
					boolean endswithdot = false;
					boolean allsamesize = true;
					
					while( leaf != null){							
						if(leaf instanceof LeafText){								
							thisline += ((LeafText)leaf).getText() +" ";
							if(((LeafText)leaf).baseline != firstleafsizeofline 
									&& ((LeafText)leaf).baseline != firstleafsizeofline+errIn1Line 
									&& ((LeafText)leaf).baseline != firstleafsizeofline-errIn1Line)
								allsamesize = false;
						}
						
						if( leaf == line.getLastLeaf()){
							if( thisline.endsWith("."))
								endswithdot = true;
							break;
						}
						leaf = leaf.getNextLeaf();
					}
					
					thisline = thisline.trim();
					
					if(previousline == null)
						previousline = thisline;
					
					
					if( ((thisline.equalsIgnoreCase("abstract") || thisline.endsWith("abstract")
							|| thisline.endsWith("Abstract") || thisline.endsWith("ABSTRACT")) && allsamesize ) ){
						lastSectionsFound[0] = lastSectionsFound[1];
						lastSectionsFound[1] = "Abstract";
						beforeabstract = false;
					}
					else if(!beforeabstract){
						if (allsamesize && (firstleafsizeofline > preTextSize+err) && !endswithdot){
	//						aheader = true;
							lastSectionsFound[0] = lastSectionsFound[1];
							if(!previousline.endsWith(".") 
									&& (preTextSize == firstleafsizeofline || preTextSize-err == firstleafsizeofline || preTextSize+err == firstleafsizeofline))
								lastSectionsFound[1] = previousline+" "+thisline;
							else
								lastSectionsFound[1] = thisline;
						}
						else if(preallsamesize && (firstleafsizeofline+err < preTextSize) && (previousline.endsWith("."))){
							lastSectionsFound[0] = lastSectionsFound[1];
							lastSectionsFound[1] = previousline;
						}
					}
					
//					System.out.println("last section found: "+lastSectionsFound[0]+","+lastSectionsFound[1]);
					
//					while( leaf != null){
						/*if(leaf instanceof LeafText){ //find section
//							thisline += ((LeafUnicode)leaf).getText() +" ";
							if (((LeafUnicode)leaf).baseline > preTextSize ){
								Node sectionLeaf = leaf;
								aheader = true;
								lastSectionsFound[0] = lastSectionsFound[1];
								lastSectionsFound[1] = "";
								while( sectionLeaf != null){							
									if(sectionLeaf instanceof LeafText){								
										lastSectionsFound[1] += ((LeafUnicode)sectionLeaf).getText() +" ";
									}
									if( sectionLeaf == line.getLastLeaf())
										break;
									sectionLeaf = sectionLeaf.getNextLeaf();
								}
								System.out.println("last section found: "+lastSectionsFound[0]+","+lastSectionsFound[1]);
							}
							preTextSize = ((LeafUnicode)leaf).baseline;
						}*/
						/*if( leaf == line.getLastLeaf()){
							break;
						}
						leaf = leaf.getNextLeaf();
					}*/
					
					//
					
					if(line.getAbsLocation().getX() < center_x+15 //after
									&& 									
									line.getAbsLocation().getX()+sizeofline > center_x-15 //after
									) 
					{
									
						if(! (line.getAbsLocation().getY() < center_y +5*distanceBetweenlines 
									&& line.getAbsLocation().getY() > center_y -5*distanceBetweenlines
									|| (linescount < 10 && linescount != 0) ) ){
							
							line = (INode) line.getNextSibling();
							while(  line != null && line.getParentNode() != text ) //to avoid having annotation texts here
								line = (INode) line.getNextSibling();
							continue;
						}
						
						
						/*while( leaf != null){							
							if(leaf instanceof LeafText){								
								thisline += ((LeafUnicode)leaf).getText() +" ";
							}
							if( leaf == line.getLastLeaf())
								break;
							leaf = leaf.getNextLeaf();
						}*/
						if(allsamesize && firstleafsizeofline > preTextSize+err){							
							thisline = "<h3>"+thisline+"</h3>";
						}
//						textsnapshot += thisline +"<br>";
						textsnapshot += thisline +"\n";
//						textsnapshot += "\n";
						linescount ++;
					}
					if(linescount >= 10) //keep 10 lines
						break;
//					line = text.getNextNode();
					line = (INode) line.getNextSibling();
					while( line != null && line.getParentNode() != text ) //to avoid having annotation texts here
						line = (INode) line.getNextSibling();
				
					preTextSize = firstleafsizeofline;
					preallsamesize = allsamesize;
				}
				
			}
		}
		
		}
		catch(Exception e){
			System.out.println("cannot extract pdf info or text snapshot (partially or completely)");
			e.printStackTrace();			
		}
//		textsnapshot += "</body> </html>";
//		System.out.println("\n"+textsnapshot+"\n\n\n");
		
		info.put("lines", textsnapshot);
		info.put("section1", lastSectionsFound[0]);
		info.put("section2", lastSectionsFound[1]);
		return info;
	}

    /**
     * If this pdf is a scientific publication: extract:
     * title, abstract, and if possible: keywords, authors, author emails
     * instead of authors, it currently returns fullAuthorInfo  
     * assumes this is the first page of the pdf
     * @param docrootcontent: the 'content' node, proabably: child of '_docroot' child of '_uiroot' chile of 'root'
     * @return
     */
    public static HashMap<String,String> getPaperInfoAsaPublication(Node docrootcontent){
    	HashMap<String,String> info = new HashMap<String, String>();
    	
    	String[] lastSectionsFound = new String[2]; //"Title";
		lastSectionsFound[0] = "Title";
		lastSectionsFound[1] = "Authors";
		String previousline = null;
		int preTextSize = 0;
		boolean preallsamesize = true;
		boolean beforeabstract = true;
    	boolean titleDone = false;
    	boolean keywordsFound = false;
    	int maxTextSizeInNFirstLines = 0;
    	boolean preWasHeader = false;
    	
		String abstr = "";
		String title = "";
		String keywords ="";
		String authorsFullInfo ="";
		int err = 1; //error in same text sizes in consequtive lines
		int errIn1Line = 0; //error in same text sizes in one line
		
		String doctype = null; 
		
		Node rootcontent = null;
		if( docrootcontent != null ){
			if((rootcontent = docrootcontent.findBFS("html")) != null)
				doctype = "html";			
			else if((rootcontent = docrootcontent.findBFS("pdf"/*,null,null,2*/)) != null)
				doctype = "pdf";
			/*if( rootcontent != null && docrootcontent. == -1 ) //if 'pdf' or 'html' are not direct childs
				doctype = null;*/
			else if((rootcontent = docrootcontent.findBFS("Loading..."/*,null,null,2*/)) != null){
				info.put("loading", "");
				return info;
			}
		}
		
		if(doctype == null || doctype.equals("html"))
			return null;
		
		try{
		
		if( doctype.equals("pdf")){
			INode text = (INode) rootcontent.findBFS("text");
			if( text != null){
				INode line = (INode) text.getFirstChild();
								
				INode firstLines = line;
				//find title's text size
				for(int i = 0 ; i < 5 ; i++){
					if(!firstLines.getName().equals("line"))
						break;
					Node leaf = firstLines.getFirstLeaf();					
					
					if(leaf instanceof LeafText){								
						if(((LeafUnicode)leaf).baseline > maxTextSizeInNFirstLines )
							maxTextSizeInNFirstLines = ((LeafUnicode)leaf).baseline; 
					}
					
					firstLines = (INode) firstLines.getNextSibling();
					while( firstLines != null && firstLines.getParentNode() != text ) //to avoid having annotation texts here
						firstLines = (INode) firstLines.getNextSibling();
					if(firstLines == null)
						break;
				}
				
				while( line != null ){
					/*for( ; line != null && !line.getName().equals("line") ; line = (INode) line.getNextSibling()){
						System.out.print("");
					}*/
					if(!line.getName().equals("line"))
						break;
					/*if( line == null )
						break;*/
					
					//find section
					Node leaf = line.getFirstLeaf();
					
					String thisline = "";
//					boolean aheader = false;
					
					//take line and related statistics:
					int firstleafsizeofline = 0;
					
					if(leaf instanceof LeafText){								
						firstleafsizeofline = ((LeafUnicode)leaf).baseline;
					}					
					
					boolean endswithdot = false;
					boolean allsamesize = true;
					
					while( leaf != null){							
						if(leaf instanceof LeafText){								
							thisline += ((LeafUnicode)leaf).getText() +" ";
							if(((LeafText)leaf).baseline != firstleafsizeofline 
									&& ((LeafUnicode)leaf).baseline != firstleafsizeofline+errIn1Line 
									&& ((LeafUnicode)leaf).baseline != firstleafsizeofline-errIn1Line)
								allsamesize = false;
						}
						
						if( leaf == line.getLastLeaf()){
							if( thisline.endsWith("."))
								endswithdot = true;
							break;
						}
						leaf = leaf.getNextLeaf();
					}
					
					if(previousline == null){
						previousline = thisline.trim();						
						preTextSize = firstleafsizeofline;
					}
					
					if(!titleDone && !title.equals("") && 
							(preTextSize == firstleafsizeofline || preTextSize+err == firstleafsizeofline || preTextSize-err == firstleafsizeofline) ) //if title is splitted in more than one line
						title += thisline;
					else if(!titleDone && (maxTextSizeInNFirstLines-err <= firstleafsizeofline)){
						title += thisline;
					}					
					else if(!titleDone && !title.equals(""))
						titleDone = true;
					
					thisline = thisline.trim();
					boolean preWasHeaderSet = false;				
					if( ((thisline.equalsIgnoreCase("abstract") || thisline.endsWith("abstract")
							|| thisline.endsWith("Abstract") || thisline.endsWith("ABSTRACT")) && allsamesize ) ){
						lastSectionsFound[0] = lastSectionsFound[1];
						lastSectionsFound[1] = "Abstract";
						beforeabstract = false;
						preWasHeader = true;
						preWasHeaderSet = true;
					}
					else if(!beforeabstract){
						if (allsamesize && (firstleafsizeofline > preTextSize+err) && !endswithdot){
	//						aheader = true;
							lastSectionsFound[0] = lastSectionsFound[1];
							preWasHeader = true;
							preWasHeaderSet = true;
							if(!previousline.endsWith(".") 
									&& (preTextSize == firstleafsizeofline || preTextSize-err == firstleafsizeofline || preTextSize+err == firstleafsizeofline))
								lastSectionsFound[1] = previousline+" "+thisline;
							else
								lastSectionsFound[1] = thisline;
						}
						else if(!preWasHeader && preallsamesize && (firstleafsizeofline+err < preTextSize) && (!previousline.endsWith("."))){
							lastSectionsFound[0] = lastSectionsFound[1];
							lastSectionsFound[1] = previousline;
							preWasHeader = true;
							preWasHeaderSet = true;
						}
					}
					if(!preWasHeaderSet)
						preWasHeader = false;
					
					if(lastSectionsFound[1].equalsIgnoreCase("keywords") || lastSectionsFound[1].contains("Keywords")
							|| lastSectionsFound[1].contains("keywords")){
						if(!(thisline.equalsIgnoreCase("keywords") || thisline.contains("Keywords")
							|| thisline.contains("keywords"))){
							if(thisline.endsWith("-"))								
								keywords += thisline;
							else
								keywords += thisline +" ";
						}
						
					}
					
					if(lastSectionsFound[0].equalsIgnoreCase("keywords") || lastSectionsFound[0].contains("Keywords")
							|| lastSectionsFound[0].contains("keywords"))
						keywordsFound = true;
					
//					System.out.println("last section found: "+lastSectionsFound[0]+","+lastSectionsFound[1]);
									
					if(lastSectionsFound[1].equalsIgnoreCase("Abstract")){
						if( !(thisline.equalsIgnoreCase("abstract") || thisline.endsWith("abstract")
								|| thisline.endsWith("Abstract") || thisline.endsWith("ABSTRACT")))
								abstr += thisline +" ";
					}
					
					if(beforeabstract && titleDone)
						authorsFullInfo += thisline + "\n";
					
					if(!beforeabstract && keywordsFound)
						break;
					
					line = (INode) line.getNextSibling();
					while( line != null && line.getParentNode() != text ) //to avoid having annotation texts here
						line = (INode) line.getNextSibling();
					
					preTextSize = firstleafsizeofline;
					preallsamesize = allsamesize;
					
				}
				
			}
			info.put("abstract", abstr.trim());
	    	info.put("title", title.trim());
	    	info.put("keywords", keywords);
	    	info.put("fullAuthorInfo",authorsFullInfo);
		}

		}
		catch(Exception e){
			System.out.println("abstract and other paper info cannot be extracted (partially or completely)");
		}
    	
    	return info;
    }
    
}
