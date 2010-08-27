package multivalent.std.adaptor.pdf;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.util.Arrays;
import java.io.IOException;

import com.pt.awt.font.NFontSimple;
import com.pt.awt.font.Encoding;
import com.pt.awt.font.CMap;

import multivalent.*;
import multivalent.node.FixedIClip;
import multivalent.node.Fixed;
import multivalent.node.FixedLeafUnicode;



/**
	Type 3 font, with individual glyphs written in PDF.

	@version $Revision: 1.19 $ $Date: 2004/03/14 21:23:56 $
*/
public class NFontType3 extends NFontSimple implements Cloneable {
  public static final String FORMAT = "Type3";

  private static final FixedLeafUnicode NOGLYPH = new FixedLeafUnicode("",null, null);
  static { NOGLYPH.getIbbox().setBounds(0,0, 0,0); NOGLYPH.getBbox().setBounds(0,0, 0,0); NOGLYPH.setValid(true); }
  private static final Rectangle NO_CLIPPING = new Rectangle(-10000,-10000,10000,10000);

  private static final Rectangle EMPTY_SHAPE = new Rectangle(0,0, 0,0);

  private static final AffineTransform MATRIX_DEFAULT = new AffineTransform(0.001, 0.0, 0.0, 0.001, 0.0, 0.0);


  private Context cx_ = new Context();

  private PDF pdf_;
  private Dict charProcs_;
  /** Faked "page" for PDF.renderStream() when font has own /Resources. */
  private Dict localpage_ = null;
  private Dict page_;
  //private double[] advances_ = new double[256]; => same as required /Widths
  /** Translation from from glyph space through text space to user space. */
  private AffineTransform glyph2user_ = new AffineTransform(1.0,0.0, 0.0,-1.0, 0.0,0.0);
  /** Trees representing glyphs, often just a Leaf.  Glyphs created at user space dimensions to maximize quality. */
  private Node[] cache_ = new Node[256];	// can't rely on LastChar-FirstChar+1 because they refer to Widths array



  public NFontType3(Dict fontdict, PDFReader pdfr, PDF pdf) throws IOException {
	super(null);
	assert fontdict!=null && pdf!=null: fontdict;
	pdf_ = pdf;
	//X PDFReader pdfr = pdf_.getReader(); => extract text

	// glyph to txt
	Object[] fm = (Object[])pdfr.getObject(fontdict.get("FontMatrix"));
	if (fm!=null/*actually required*/) { double[] d = new double[6]; PDF.getDoubles(fm, d, 6); m_ = new AffineTransform(d); } else m_ = MATRIX_DEFAULT;
	//f.setGlyph2User();	// sometimes used at 1 point

	Rectangle bbox = COS.array2Rectangle((Object[])pdfr.getObject(fontdict.get("FontBBox")), null);
	bbox_ = new Rectangle2D.Double(bbox.getX(),bbox.getY(), bbox.getHeight(),bbox.getWidth());

	Dict res = (Dict)pdfr.getObject(fontdict.get("Resources"));
	if (res!=null) {	// font has own resources, so don't use page's
		localpage_ = new Dict(5);
		localpage_.put("Resources", res);
	}

	charProcs_ = (Dict)pdfr.getObject(fontdict.get("CharProcs"));
	intrinsic_ = null;	// none -- must set encoding_ externally
	touni_ = CMap.IDENTITY;

	cx_.styleSheet = new StyleSheet();
  }

  public NFontType3 deriveFont(float size) {
	NFontType3 f = null; try { f = (NFontType3)clone(); } catch (CloneNotSupportedException canthappen) {}
	f.size_ = size;
	f.max_ = null;
	f.cache_ = new Node[256];	// regenerate at new size -- don't rescale bitmaps!
	f.setGlyph2User(at_);
//System.out.println("derive Type 3 font, size="+size+", "+at_+" => glyph2txt2user = "+glyph2user_);
	return f;
  }

/*  public NFontType3 deriveFont(Encoding encoding, CMap toUnicode) {
	NFontType3 f = (NFontType3)super.deriveFont(encoding, toUnicode);
	//notdef_ = new GeneralPath(GeneralPath.WIND_EVEN_ODD); buildChar(charstrings_[char2glyph_[NOTDEF_CHAR]], notdef_);	// sets up getGlyph()
	//Arrays.fill(cache_, null); -- done by deriveFont(size_)
	// clear advances_... but implicit as validity based on cache_
	return f;
  }*/

  public NFontType3 deriveFont(Encoding encoding, CMap toUnicode) {
//System.out.println("Type 3 deviveFont");
	return (NFontType3)super.deriveFont(encoding, toUnicode!=null? toUnicode: CMap.IDENTITY);	// no guess
  }

  public NFontType3 deriveFont(AffineTransform at) {
	NFontType3 f = (NFontType3)super.deriveFont(at);
	//Arrays.fill(f.cache_, null); => done in deriveFont(size)
	f.setGlyph2User(at);
	return f;
  }

  private void setGlyph2User(AffineTransform Tm) {
	float size = getSize();
	glyph2user_ = new AffineTransform(m_);	// glyph to text
	AffineTransform Tm0 = new AffineTransform(Tm.getScaleX()*size, Tm.getShearY(), Tm.getShearX(), -Tm.getScaleY()*size, 0.0,0.0);
	glyph2user_.concatenate(Tm0);	// text to user
//System.out.println(m_+" * "+Tm0+" => "+glyph2user_);
//System.out.println("FontBBox "+bbox_+" => "+getMaxCharBounds());
  }

  /**
	Prime glyphs, but create individual glyphs on demand.
	Want to create on demand for performance and memory use, and have to since may require /Resources from <i>current</i> page and different pages for all the glyphs!
  */


  /**
	Type 3 fonts potentially need the <code>/Resources</code> dictionary for the <em>current</em> page,
	so set this before using this font on the page.
	(This is sloppy but valid PDF -- the document should have a local <code>/Resources</code>.)
  */
  public void setPage(Dict page) { page_ = localpage_!=null? localpage_: page; }



  private Node getGlyph(int gid) {
	assert firstch_<=gid && gid<=lastch_: firstch_+" > "+(int)gid+" > "+lastch_;
	//int inx = char2glyph[gid];	// always IDENTITY
	Node glyph = cache_[gid];
	if (glyph==null) try { glyph = cache_[gid] = createGlyph(gid); } catch (IOException ioe) {}
	assert glyph!=null: (int)gid;	// can have holes, but then shouldn't request those chars
	return glyph;
  }

  /**
	Converts stream into scaled document subtree.
  */
  private Node createGlyph(int gid) throws IOException {
	String cname = encoding_.getName((char)gid);
	Object cpref = charProcs_.get(cname);
//if (gid=='s' || gid=='h') System.out.println(gid+"/"+cname+"/"+cpref+", g2u = "+glyph2user_);
	PDFReader pdfr = pdf_.getReader();
	InputStreamComposite is = pdfr.getInputStream(cpref, true);
	// make tree
	Rectangle bounds = new Rectangle(0,0, Integer.MAX_VALUE,Integer.MAX_VALUE);	// no clip, no displacement
	FixedIClip clipp = new FixedIClip(cname,null, null, bounds, bounds);
	//FixedI top = new FixedI(cname,null, null);
//System.out.println("createGlyph "+glyph2user_);

	GraphicsState gs = new GraphicsState(); gs.ctm = new AffineTransform(glyph2user_);
	try { pdf_.buildStream(page_, clipp, gs, is, null);
	} catch (/*Parse, IO*/Exception e) {	// skip char
		//e.printStackTrace(); System.err.println(ech+"/"+(int)ech+" "+cpref); System.exit(1);
		new multivalent.node.FixedLeafShape("bad_glyph "+gid,null, clipp, EMPTY_SHAPE, false, true);
	} finally { is.close(); }

	Node glyph = clipp;
	//int baseline = Math.abs(glyph.bbox.height);	// backdoor => not used by all PDF generators
	if (clipp.size()==0) return NOGLYPH;	// some just d0/d1.  (also i/o error)

	if /*(false) while*/ (glyph.isStruct() && glyph.size()==1 && glyph.sizeSticky()==0) {
//System.out.print("=> "); glyph.dump();
		glyph = ((INode)glyph).removeChildAt(0);
		//glyph.setName(clipp.getName());
	}
	//glyph.setName(cname);

	Rectangle bbox = glyph.bbox;
	if (!glyph.isValid()) { cx_.reset(); glyph.formatNode(0,0, cx_); }
	glyph.baseline = bbox.height;	// for hyperlinks
	//bbox.setLocation(0,0);

	assert glyph.isValid();
//System.out.print("final => "); glyph.dump();
//System.out.print((char)(inx+firstch_)+"/"+cache_[inx].bbox.width);
	//assert glyph.sizeSticky() % 2 == 0: glyph.sizeSticky();
	return glyph;
  }



  public String getName() { return "(Type 3)"; }
  public String getFamily() { return "(Type 3)"; }
  public String getFormat() { return FORMAT; }
  public int getNumGlyphs() { return charProcs_.size(); }


  public boolean canDisplayEchar(int ech) {
	return NOTVALID_CHAR!=ech && charProcs_.get(encoding_.getName(ech))!=null
		/*&& (newwidths_==null || newwidths_[ech - firstch_] > 0)*/;
  }
  public char getSpaceEchar() { return NOTVALID_CHAR; }

  public Point2D echarAdvance(int ech) {
	double adv = ech<firstch_ || ech>lastch_ || !canDisplayEchar(ech)? 0.0: 
		newwidths_[ech - firstch_] * m_.getScaleX();
	if (adv==0.0 && canDisplayEchar(ech)) { Node n = getGlyph(ech); adv = n.bbox.width; /*System.out.println("0.0 in /Widths => "+w);*/ }	// /Widths botched by Ghostscript 5.10
//System.out.println("   "+(int)ech+"/"+cname_[ech-firstch_]+" "+w+" * "+m_.getScaleX()+" * "+atx+" * "+size_);
//double wout = w*atx*size_; if (wout<1.0 /*&& ch>0/*&&ch!=' '*/) System.out.println((int)ch+"/"/*+newwidths_[ch-firstch_]+"/"*/+widths_[ch]+", "+canDisplayChar(ch)+" "+firstch_+" "+lastch_+", m="+m_.getScaleX()+" / at="+at_+", size="+size_);
	return new Point2D.Double(adv * size_ * at_.getScaleX(), adv * size_ * at_.getShearY());
  }

  public void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor) {
	if (MODE_INVISIBLE==mode) return;

	AffineTransform at = g.getTransform();
	g.translate(x,y);
	//g.transform(m_); g.transform(at_); g.scale(size_, size_); => already factored into glyph
	cx_.g = g;
//System.out.print(estr+" @ "+getSize()+"*"+at_.getScaleX());
	double dx = 0.0;
	for (int i=0,imax=estr.length(); i<imax; i++) {
		char ech = estr.charAt(i); if (!canDisplayEchar(ech)) continue;
		Node glyph = getGlyph(ech);
//System.out.print("  "+ech+"=>"+glyph.getFirstLeaf()+"/"+echarAdvance(ech)+"/"+newwidths_[ech-firstch_]);
//System.out.println("  D"+(int)ech+" => ");
//if (ech=='s' || ech=='h') { System.out.println("Type 3 "+ech+": "+glyph); glyph.dump(); }
		//double dy = -glyph.baseline;
		double dy = glyph.bbox.y;	// skipping over paint() to paintNode()
		g.translate(dx, dy); glyph.paintNode(NO_CLIPPING, cx_); g.translate(-dx, -dy);
		dx += echarAdvance(ech).getX();
	}
//System.out.println();
	g.setTransform(at);
  }
}
