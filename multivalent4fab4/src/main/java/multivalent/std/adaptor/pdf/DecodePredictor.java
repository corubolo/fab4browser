package multivalent.std.adaptor.pdf;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.EOFException;

import phelps.io.InputStreams;



/**
	Predictor filter, for LZW or Flate.

	@see <a href='http://www.w3.org/TR/REC-png.html#Filters'>W3C PNG Filters</a>

	@version $Revision: 1.9 $ $Date: 2004/08/25 01:55:43 $
*/
public class DecodePredictor extends FilterInputStream {

  private boolean eof_ = false;

  private int pred_;
  private int colors_, bitsper_;
  //private int cols_;

  private byte[] lastline_, line_;	// not byte[] as bitsper_ can be 16
  private int linei_;
  private int col0_;	// left for first byte
  private int rowlen_;


  /**
	If prediction is 1 (TIFF no prediction) or 10 (PNG no prediction), there is no use in applying a DecodePredictor filter.
  */
  public DecodePredictor(InputStream in, int pred, Dict parms, PDFReader pdfr) throws IOException {
	super(in);
	assert pred!=1 && pred!=10;	// InputStreamComposite shouldn't instantiate DecodePredictor if no prediction
	pred_ = pred;

	// parameters
	Object o;
	colors_ = (o=parms.get("Colors"))!=null? pdfr.getObjInt(o): 1; assert colors_>=1;
	bitsper_ = (o=parms.get("BitsPerComponent"))!=null? pdfr.getObjInt(o): 8;	//assert bitsper_==1 || 2 || 4 || 8 || 16;
if (bitsper_>8) multivalent.Meta.sampledata("Predictor "+pred+" bitsper="+bitsper_);
	int cols = (o=parms.get("Columns"))!=null? pdfr.getObjInt(o): 1;
	col0_ = (colors_ * bitsper_ + 7) / 8;	// same as bpp
//System.out.println("predictor "+pred+": cols="+cols+", bitsper_="+bitsper_+" "+parms);

	rowlen_ = (cols * colors_ * bitsper_ + 7) / 8 + col0_/*0 for left predictions*/;	// exclusive of predictor
//System.out.println("colors_="+colors_+", bitsper_="+bitsper_+", col0_="+col0_+", rowlen_="+rowlen_);
//System.out.println("len = "+(rowlen_-col0_)+" vs "+cols+"*"+colors_+"="+(cols*colors_)+" + "+(pred_==15? 1: 0));
//System.out.println("need ("+cols+" * "+colors_+" * "+bitsper_+"+7)/8 + "+(pred==15/*PDF spec says >=10*/? 1: 0)+" = "+(rowlen_-col0_)+" per row");
	line_=new byte[rowlen_]; lastline_=new byte[line_.length];
//System.out.print("pred = "); for (int line=0, c; line<100 && (c=in.read())!=-1; line++) { System.out.print(c+"/"+in.read(line_, 0, rowlen_-col0_)+" "); }  System.exit(0);

	nextRow();
  }

  private void nextRow() throws IOException {
	// reset output
	byte[] tmp=lastline_; lastline_=line_; line_=tmp;	// line_ becomes lastline_ without copying
	byte[] l=line_, ll=lastline_;
	linei_ = col0_;

	// per line predictor?
	int pred = pred_;
	if (pred==15) {
		pred = in.read();	// take from line_ -- so have to be before read line
		if (pred==-1) { eof_=true; return; }
		pred += 10;	// embedded numbered like this
//System.out.println("pred = "+pred);
//System.out.print(/*"pred = "*/" "+pred);
		assert pred>=10 && pred<15: pred;
	} else if (pred>=10) in.read();	// WRONG! BUT! Acrobat seems to do it, as for isaacs-15.pdf w/Predictor 12 on PDF 1.5 Xref

	int len = rowlen_;
	try { InputStreams.readFully(in, l, 0+col0_, len - col0_); }
	catch (EOFException eof) { eof_=true; return; }	// empty or short line
	// if TIFF type or bitsper_==16, separate into own components here, recombine at end of method
//System.out.print("in:"); for (int i=0; i<len; i++) System.out.print(" "+Integer.toHexString(l[i]&0xff));

	// process prediction for row
	int bpp = (colors_ * bitsper_ + 7) / 8;
	switch (pred) {
	case 1:	// No prediction (default)
		assert false;
		break;

	case 2:	// TIFF Predictor 2
		for (int i=0+col0_; i<len; i++) l[i] += l[i-bpp];	// FIX: respect BitsPerPixel
		break;

	// PNG prediction
	// "Filtering algorithms are applied to bytes, not to pixels, regardless of the bit depth or color type of the image."
	case 10:	// None
		break;
	case 11:	// Sub -- left
		for (int i=0+col0_; i<len; i++) l[i] += l[i-bpp];	// signed bytes OK
		break;
	case 12:	// Up -- above
		//for (int i=0+col0_; i<len; i++) System.out.print((l[i]&0xff)+" ");
		for (int i=0+col0_; i<len; i++) l[i] += ll[i];	// signed bytes OK
		//System.out.print(" => "); for (int i=0+col0_; i<len; i++) System.out.print((l[i]&0xff)+" ");  System.out.println();
		break;
	case 13:	// Average -- (left + above) / 2
		//for (int i=0; i<col0_; i++) l[i] += ll[i]/2;	// left==0 so just above/2, or leave untouched ???
		for (int i=0+col0_; i<len; i++) l[i] += ((l[i-bpp]&0xff) + (ll[i]&0xff))/2;
		break;
	case 14:	// Paeth -- closest of left, above, upper-left
		//for (int i=0; i<col0_; i++) l[i] += ll[i];	// ???
		for (int i=0+col0_; i<len; i++) {
			int a=l[i-bpp]&0xff, b=ll[i]&0xff, c=ll[i-bpp]&0xff;	// Java should have unsigned bytes
			int p = a + b - c;	// initial estimate
			int pa=Math.abs(p-a), pb=Math.abs(p-b), pc=Math.abs(p-c);	// distances to a, b, c

			int val = pa<=pb && pa<=pc? a: pb<=pc? b: c;

			l[i] += (byte)val;
		}
		break;
	case 15:	// optimum -- per line determination
		assert false;	// already mapped into other code
		break;

	default:
		assert false: pred+" vs "+pred_;	// unknown algorithm
	}
//System.out.print("  ||  out: "); for (int i=0; i<len; i++) System.out.print(" "+Integer.toHexString(l[i]&0xff));  System.out.println();
  }


  public int read(byte[] b, int off, int len) throws IOException {
	assert b!=null && off>=0 && len>=0 && len+off <= b.length;

	if (eof_) return -1;

	else if (linei_==rowlen_) {
		nextRow();
		return read(b, off, len);

	} else {
		len = Math.min(len, rowlen_-linei_);
		assert len>=0 && len<=rowlen_-col0_;
		System.arraycopy(line_,linei_, b,off, len);
		linei_ += len;
	}

	return len;
  }

  public int read() throws IOException {
	int b;

	if (eof_) b=-1;
	else if (linei_ < rowlen_) b = (line_[linei_++] & 0xff);
	else {
		nextRow();
		return read();
	}

	return b;
  }

  public boolean markSupported() { return false; }
}
