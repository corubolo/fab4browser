package multivalent.std.adaptor.pdf;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;
import java.io.InputStream;
import java.io.IOException;

import phelps.lang.Integers;
import phelps.util.Arrayss;



/**
	Function objects: sampled (type 0), exponential (2), stitching (3), PostScript calculator (4).
	Used by colorspaces and shading.

	@version $Revision: 1.12 $ $Date: 2005/12/07 20:40:39 $
*/
public abstract class Function {
  /**
	Returns function instance corresponding to <var>dictref</var>, which should get an {@link IRef} or a Dict if the function is specified directly.
  */
  public static Function getInstance(Object dictref, PDFReader pdfr) throws IOException {
	assert dictref!=null && pdfr!=null;

	Dict dict = (Dict)pdfr.getObject(dictref);
	int type = pdfr.getObjInt(dict.get("FunctionType"));
if (type==3) multivalent.Meta.sampledata("function type "+type);

	Function fn;
	switch (type) {
		case 0: fn = new Sampled(dictref, dict, pdfr); break;
		case 2: fn = new Exponential(dict, pdfr); break;
		case 3: fn = new Stitching(dict, pdfr); break;
		case 4: fn = new Calculator(dictref, dict, pdfr); break;
		default: fn=null; assert false: type;	// invalid function type
	}

	return fn;
  }



  float[] domain_;
  float[] range_ = null;	// required for type 0 and 4 only

  /**
	Process common dictionary elements domain and range.
  */
  Function(Dict dict, PDFReader pdfr) throws IOException {
	domain_ = toFloatArray((Object[])pdfr.getObject(dict.get("Domain")), null);	assert domain_!=null;

	range_ = toFloatArray((Object[])pdfr.getObject(dict.get("Range")), null);	// null OK
  }

  /** Compute the function from <var>input</var> and placing result in <var>output</var>. */
  public abstract void compute(float[] input, float[] output);

  /** Returns input arity. */
  public int getM() { return domain_.length / 2; }
  /** Returns output arity. */
  public int getN() { return range_.length / 2; }


  // utility functions

  /** Convert from PDF number array to Java float[]. */	// => different that one in PDF.java since can supply default array
  float[] toFloatArray(Object[] oa, float[] deflt) throws IOException {
	if (oa==null) return deflt;
	float[] fa = new float[oa.length];
	for (int i=0,imax=fa.length; i<imax; i++) fa[i] = ((Number)oa[i]).floatValue();
	return fa;
  }

  int[] toIntArray(Object[] oa) throws IOException {
	int[] ia = new int[oa.length];
	for (int i=0,imax=ia.length; i<imax; i++) ia[i] = ((Number)oa[i]).intValue();
	return ia;
  }

  /** Clip <var>in</var> values against <var>clip</var> array. */
  void clip(float[] in, float[] clip) {
	int cliplen = clip.length / 2;
	//Xassert in.length*2 >=/*should be ==*/ cliplen: in.length+" vs "+cliplen;	// should be same size, but PDF.java passes same array of length 4 every time
//System.out.println("clip "+in[0]+"... len="+in.length+" in "+clip[0]+".."+clip[1]+"... len="+clip.length);
	for (int i=0; i<cliplen; i++) {
		float inval=in[i], min=clip[i*2], max=clip[i*2+1];
		if (inval<min) in[i]=min; else if (inval>max) in[i]=max;
	}
  }
}



// concrete subclasses

// e.g., /FunctionType 0 /Domain [ 0 1 ] /Range [ -1 1 ] /BitsPerSample 16 /Size [ 256 ] /Length 527 /Filter /FlateDecode >> stream ...
// e.g., /FunctionType 0 /Domain [ 0 1 ] /Range [ 0 1 0 1 0 1 ] /BitsPerSample 8 /Size [ 255 ]
class Sampled extends Function {
  int[] size_;
  float[] encode_, decode_;	// can be multidimensional, where element i,j,k is at (i*dim1 + j) * dim2 + k
  int order_;	// 1=linear, 3==cubic
  float[] samples_;	// one sample for array element regardless of bits per pixel

  Sampled(Object dictref, Dict dict, PDFReader pdfr) throws IOException {
	super(dict, pdfr);

	size_ = toIntArray((Object[])pdfr.getObject(dict.get("Size")));	assert size_.length==getM();

	Object o;
	if ((o = pdfr.getObject(dict.get("Encode")))!=null) encode_ = toFloatArray((Object[])o, null);
	else {
		encode_ = new float[size_.length * 2];
		for (int i=0,imax=size_.length; i<imax; i++) { encode_[i*2]=0; encode_[i*2+1]=size_[i]-1; }
	}
	decode_ = (o = pdfr.getObject(dict.get("Decode")))!=null? toFloatArray((Object[])o, null): range_;

	order_ = (o = pdfr.getObject(dict.get("Order")))!=null? ((Number)o).intValue(): 1;
	assert order_==1 || order_==3: order_;
	// if (/*order_!=1 &&*/ order_!=3) order_=1;

	int bps = ((Number)pdfr.getObject(dict.get("BitsPerSample"))).intValue();	// 1,2,4,8,12,16,24,32

	// dictionary with samples -- each sample in own array element
	int buflen=1; for (int i=0,imax=size_.length; i<imax; i++) buflen *= size_[i];
	buflen *= getN();
//System.out.println(dictref+"  "+size_[0]+"... samples x "+getN()+" components "+" => "+buflen+" @ "+bps);
	samples_ = new float[buflen];
	InputStream in = pdfr.getInputStream(dictref);
	long bits=0L, mask=(1L<<bps)-1L;
	float valmax=(1L<<bps);	// float so no truncate on integer division
	for (int i=0, valid=0; i<buflen; i++) {
		while (valid<bps) { bits = (bits<<8) + in.read(); valid+=8; }
		long samp = (bits >> (valid-bps)) & mask;  valid -= bps;
		samples_[i] = samp / valmax;	// sample is fraction of bit-range
//System.out.print(" "+samples_[i]);
	}
	in.close();

	//assert buf.length == buf.length * (bps==1? 8: bps==2? 4: bps==4? 2: 1);
  }

  public void compute(float[] input, float[] output) {
	clip(input, domain_);

	for (int i=0,imax=getM(), N=getN(), sampoff=0; i<imax; sampoff=(sampoff>0? sampoff: 1)*size_[i], i++) {
		// encode
		float x = input[i], xmin=domain_[i*2],xmax=domain_[i*2+1], ymin=encode_[i*2],ymax=encode_[i*2+1];
		float e = ymin + ((x-xmin) * (ymax-ymin)/(xmax-xmin));

		if (e<0f) e=0f; else if (e>size_[i]-1) e=size_[i]-1;	// clip to array
		float ceil = (float)Math.ceil(e), fract = 1f-(ceil-e);
		int inx0 = (sampoff + (int)e) * N, inx1 = (sampoff + (int)ceil) * N;	// * N for logical sample => array index

//System.out.println("  "+input[i]+" => sampled @" + e+"+"+sampoff+" / "+fract+" = "+inx0);
		for (int j=0,jmax=getN(); j<jmax; j++) {
			// interpolate between samples
			float samp0 = samples_[inx0 + j], samp1 = samples_[inx1 + j];	// For a function with multidimensional input (more than one input variable), the sample values in the first dimension vary fastest,
			float samp = order_==1? /*bilinear*/(samp0*(1f-fract) + fract*samp1): /*(should be) cubic*/(samp0*(1f-fract) + fract*samp1);

			// decode
			ymin=decode_[j*2]; ymax=decode_[j*2+1];
			float r = ymin + samp * (ymax-ymin);
			output[j] = r;
//System.out.println("  "+samp0+".."+samp1+" => output "+r+" "+range_[j*2]+".."+range_[j*2+1]);
		}

	}

	clip(output, range_);
  }
}



// e.g., /FunctionType 2 /Domain [ 0 1 ] /Range [ 0 1 0 1 0 1 ] /C0 [ 1 0.976 0.961 ] /C1 [ 1 0.965 0.94901 ] /N 1
class Exponential extends Function {
  static final float[] C0_DEFAULT = { 0f }, C1_DEFAULT = { 1f };	// Java won't allow static in nested classes

  float[] C0_, C1_;
  double N_;

  Exponential(Dict dict, PDFReader pdfr) throws IOException {
	super(dict, pdfr);

	C0_ = toFloatArray((Object[])pdfr.getObject(dict.get("C0")), C0_DEFAULT);
	C1_ = toFloatArray((Object[])pdfr.getObject(dict.get("C1")), C1_DEFAULT);
	N_ = ((Number)pdfr.getObject(dict.get("N"))).floatValue();
  }

  public int getN() { return C0_.length; }	// /Range not required

  public void compute(float[] input, float[] output) {
	clip(input, domain_); float x=input[0];  assert getM()<=input.length: getM()+" ==? "+input.length;	// m<=len because PDF.java uses common array for all inputs
	int n = getN();  assert n==output.length: n+" vs "+output.length;

	float[] C0=C0_, C1=C1_;
	if (N_==1f) for (int i=0; i<n; i++) output[i] = C0[i] + x * (C1[i]-C0[i]);	// special case
	else for (int i=0; i<n; i++) output[i] = C0[i] + (float)Math.pow(x, N_) * (C1[i]-C0[i]);

	if (range_!=null) clip(output, range_);
  }
}



/**
	Stitch together various 1-input subfunctions (all with same output arity).
*/
class Stitching extends Function {
  Function[] funs_;
  float[] bounds_;
  float[] encode_;
  int n_;

  Stitching(Dict dict, PDFReader pdfr) throws IOException {
	super(dict, pdfr);

	Object[] oa = (Object[])pdfr.getObject(dict.get("Functions"));
	funs_ = new Function[oa.length];
	for (int i=0,imax=oa.length; i<imax; i++) funs_[i] = Function.getInstance(oa[i], pdfr);
	n_ = funs_[0].getN();	// all should be the same

	bounds_ = toFloatArray((Object[])pdfr.getObject(dict.get("Bounds")), null);  assert bounds_.length == funs_.length - 1;

	encode_ = toFloatArray((Object[])pdfr.getObject(dict.get("Encode")), null);  assert encode_.length == funs_.length * 2;
  }

  public int getN() { return n_; }	// /Range not required

  public void compute(float[] input, float[] output) {
	clip(input, domain_);

	float x = input[0];
	int subi=bounds_.length-1; for (; subi>=0; subi--) if (x >= bounds_[subi]) break;
	subi++;

	float[] subinput = new float[1];
	float xmin=(subi>0? bounds_[subi-1]: domain_[0]), xmax=(subi<bounds_.length? bounds_[subi]: domain_[1]), ymin=encode_[subi*2], ymax=encode_[subi*2+1];
	subinput[0] = ymin + ((x-xmin) * (ymax-ymin)/(xmax-xmin));	// interpolate

	funs_[subi].compute(subinput, output);

	if (range_!=null) clip(output, range_);
  }
}



/**
	Process subset of PostScript language, with stack and conditionals.
	Some day could dynamically write new class bytecode -- which is also stack based, and Java VM would optimize!
	LATER: just pass stream to PostScript.java!
*/
class Calculator extends Function {
  private static final Integer FALSE=Integers.ZERO, TRUE=Integers.ONE;
  private static final Character PROC_OPEN=new Character('{'), PROC_CLOSE=new Character('}');
  private static final Map<String,Integer> op2code;
  static {
	String[] cmds =
		("abs add atan ceiling cos cvi cvr div exp floor idiv ln log mod mul neg sin sqrt sub round truncate"
		+" and bitshift eq false ge gt le lt ne not or true xor"
		+" if ifelse"
		+" copy exch pop dup index roll").split("\\s+");
	// also '{' '}' numbers

	int len = cmds.length;  assert len==42: cmds.length;
	op2code = new HashMap<String,Integer>(len * 2);
	for (int i=0,imax=len; i<imax; i++) op2code.put(cmds[i], new Integer(i));
  }

  static class Op {
	public String op;
	public int code;

	public Object[] iftrue;
	public Object[] iffalse;
	public Op(String op, int code) { this.op=op; this.code=code; }
	public String toString() { return op+"/"+code; }
  }


  Object[] cmds_;	// Number and Op

  Calculator(Object dictref, Dict dict, PDFReader pdfr) throws IOException {
	super(dict, pdfr);

	InputStreamComposite in = pdfr.getInputStream(dictref);
	readToken(in);	// assert '{'==xxx   // eat opening '{'
	cmds_ = parse(in);
  }

  public void compute(float[] input, float[] output) {
	clip(input, domain_);  assert getM() <= input.length: getM()+" <= "+input.length;	// should be: assert == but see Exponential.compute

	//double[] fstk = new float[100];	// double
	//int[] istk = new boolean[100];	// int and bool
	Number[] stk = new Number[100];	// Float and Integer immutable so could potentially make many many instances -- but should be in nursury so create and gc fast... enough?
	int si = 0;	// common pointer
	//for (int i=0,imax=getM(); i<imax; i++) fstk[si++] = (double)input[i];	// put input on stack
//System.out.print("Calculator,");
	for (int i=0,imax=getM(); i<imax; i++) /*{*/stk[si++] = new Double((double)input[i]);	// put input on stack
//System.out.print(" "+input[i]);}
//System.out.println();

	si = execute(cmds_, stk, si);

	assert si == getN(): si+" vs "+getN();
//System.out.println(); System.out.print(" => ");
	for (int i=0,imax=getN(); i<imax; i++) /*{*/output[i] = ((Number)stk[i]).floatValue();	// take output from stack
//System.out.print(" "+output[i]+"/"+range_[i*2+1]);}
//System.out.println();

	clip(output, range_);
//System.out.print(" => clip"); for (int i=0,imax=getN(); i<imax; i++) System.out.print(" "+output[i]); System.out.println();
  }


  Object[] parse(InputStreamComposite in) throws IOException {
	List<Object> l = new ArrayList<Object>(100);

	while (true) {
		Object tok = readToken(in);
		if (tok==PROC_OPEN) l.add(parse(in));
		else if (tok==PROC_CLOSE) break;
		else if (tok instanceof Number) l.add(tok);
		else if (tok instanceof Op) {
			Op op = (Op)tok;
			if (op.code==34) op.iftrue = (Object[])l.remove(l.size()-1);
			else if (op.code==35) { op.iffalse = (Object[])l.remove(l.size()-1); op.iftrue = (Object[])l.remove(l.size()-1); }
			l.add(op);
		}
	}

	return l.toArray();
  }

  /* @return String, Number, {, }. */
  Object readToken(InputStreamComposite in) throws IOException {
	int c;
	while ((c=in.read())!=-1 && Character.isWhitespace((char)c)) {}

	Object o;
	if (c=='{') o = PROC_OPEN;
	else if (c=='}' || c==-1) o = PROC_CLOSE;
	else if (c>='a' && c<='z') {	// operators all-lowercase
		StringBuffer sb = new StringBuffer(10); sb.append((char)c);
		while ((c=in.read())>='a' && c<='z') sb.append((char)c);
		in.unread(c);
		String op = sb.toString();
		Integer into = op2code.get(op); assert into!=null: sb;
		o = new Op(op, into.intValue());

	} else { assert ('0' <= c&&c <= '9') || c=='.' || c=='-': c+"/"+(char)c;
		StringBuffer sb = new StringBuffer(10); sb.append((char)c);
		boolean ffloat=false;
		while (true) {
			if ('0' <= (c=in.read())&&c <= '9') sb.append((char)c);
			else if (c=='.' /*|| c=='e'?*/) { sb.append((char)c); ffloat=true; }
			else { in.unread(c); break; }
		}
		String s = sb.toString();
		o = ffloat? (Number)new Double(s): new Integer(s);
	}
//System.out.println("token = "+o);

	return o;
  }

  int execute(Object[] cmds, Number[] stk, int si) {
	for (int i=0,imax=cmds.length,j,n; i<imax; i++) {
		Object o = cmds[i];

		if (o instanceof Number /*|| o instanceof Op[] -- in Op object, not stack*/) { stk[si++] = (Number)o; continue; }

		Op op = (Op)o;
		Number opn = si>0? stk[si-1]: null;	// could open command stream with 'true'

//for (j=0; j<si; j++) System.out.print(stk[j]+" ");  System.out.println(op.op);
		switch (op.code) {
		// arithmetic
		case 0: stk[si-1] = new Double(Math.abs(opn.doubleValue())); break;	// abs
		case 1: stk[si-2] = new Double(((Number)stk[si-2]).doubleValue() + opn.doubleValue()); si--; break;	// add
		case 2: stk[si-1] = new Double(Math.atan(opn.doubleValue())); break;	// atan
		case 3: stk[si-1] = new Double(Math.ceil(opn.doubleValue())); break;	// ceiling
		case 4: stk[si-1] = new Double(Math.cos(opn.doubleValue())); break;	// cos
		case 5: stk[si-1] = Integers.getInteger(opn.intValue()); break;	// cvi
		case 6: stk[si-1] = new Double(opn.doubleValue()); break;	// cvr
		case 7: stk[si-2] = new Double(stk[si-2].doubleValue() / opn.doubleValue()); si--; break;	// div
		case 8: stk[si-2] = new Double(Math.pow(stk[si-2].doubleValue(), opn.doubleValue())); si--; break;	// exp
		case 9: stk[si-1] = new Double(Math.floor(opn.doubleValue())); break;	// floor
		case 10: stk[si-2] = Integers.getInteger(stk[si-2].intValue() / opn.intValue()); si--; break;	// idiv
		case 11: stk[si-1] = new Double(Math.log(opn.doubleValue())); break;	// ln
		case 12: stk[si-1] = new Double(Math.log(opn.doubleValue()) / Math.log(10.0)); break;	// log
		case 13: stk[si-2] = Integers.getInteger(opn.intValue() % stk[si-2].intValue()); si--; break;	// mod
		case 14: stk[si-2] = new Double(stk[si-2].doubleValue() * opn.doubleValue()); si--; break;	// mul
		case 15: stk[si-1] = new Double(- opn.doubleValue()); break;	// neg
		case 16: stk[si-1] = new Double(Math.sin(opn.doubleValue())); break;	// sin
		case 17: stk[si-1] = new Double(Math.sqrt(opn.doubleValue())); break;	// sqrt
		case 18: stk[si-2] = new Double(stk[si-2].doubleValue() - opn.doubleValue()); si--; break;	// sub
		case 19: stk[si-1] = Integers.getInteger(Math.round(opn.floatValue())); break;	// round
		case 20: stk[si-1] = Integers.getInteger((int)opn.doubleValue()); break;	// truncate

		// relational, boolean, bitwise
		case 21: stk[si-2] = Integers.getInteger(stk[si-2].intValue() & opn.intValue()); si--; break;	// and (OK on bool)
		case 22: stk[si-2] = Integers.getInteger(stk[si-2].intValue() << opn.intValue()); si--; break;	// bitshift
		case 23: stk[si-2] = stk[si-2].doubleValue() == opn.doubleValue()? TRUE: FALSE; si--; break;	// eq
		case 24: stk[si++] = FALSE; break;	// false
		case 25: stk[si-2] = stk[si-2].doubleValue() >= opn.doubleValue()? TRUE: FALSE; si--; break;	// ge
		case 26: stk[si-2] = stk[si-2].doubleValue() >  opn.doubleValue()? TRUE: FALSE; si--; break;	// gt
		case 27: stk[si-2] = stk[si-2].doubleValue() <= opn.doubleValue()? TRUE: FALSE; si--; break;	// le
		case 28: stk[si-2] = stk[si-2].doubleValue() <  opn.doubleValue()? TRUE: FALSE; si--; break;	// lt
		case 29: stk[si-2] = stk[si-2].doubleValue() != opn.doubleValue()? TRUE: FALSE; si--; break;	// ne
		case 30: stk[si-1] = opn==TRUE? FALSE: opn==FALSE? TRUE: Integers.getInteger(~ opn.intValue()); break;	// not
		case 31: stk[si-2] = Integers.getInteger(stk[si-2].intValue() | opn.intValue()); si--; break;	// or (OK on bool)
		case 32: stk[si++] = TRUE; break;	// true
		case 33: stk[si-2] = Integers.getInteger(stk[si-2].intValue() ^ opn.intValue()); si--; break;	// xor (OK on bool)

		// conditional
		case 34: si--; if (opn.intValue()!=0) si = execute(op.iftrue, stk, si); break;	// if
		case 35: si--; si = execute((opn.intValue()!=0? op.iftrue: op.iffalse), stk, si); break;	// ifelse

		// stack
		case 36: n=opn.intValue(); si--; System.arraycopy(stk,si-n, stk,si, n); si+=n; break;	// copy
		case 37: stk[si-1]=stk[si-2]; stk[si-2]=opn; break;	// exch
		case 38: si--; break;	// pop
		case 39: stk[si++]=opn; break;	// dup (share immutable objects)
		case 40: n=opn.intValue(); si--; stk[si]=stk[si-n-1]; si++; break;	// index
		case 41:	// roll
			n=stk[si-2].intValue(); j=opn.intValue(); int jabs=Math.abs(j); si-=2;
//System.out.print("roll "+n+"+ "+j+": "+stk[0]+" "+stk[1]+" "+stk[2]+" "+stk[3]+" "+stk[4]);
			int a0,an, b0,bn; if (j>0) { a0=si-j; an=si-n; b0=si-n; bn=si-n+j; } else { j=-j; a0=si-n; an=si-j; b0=si-n+j; bn=si-n; }
			Number[] tmp = new Number[jabs]; System.arraycopy(stk, a0, tmp,0, jabs);
			System.arraycopy(stk,b0, stk,bn, n-j);
			System.arraycopy(tmp,0, stk,an, j);
//System.out.println(" => "+stk[0]+" "+stk[1]+" "+stk[2]+" "+stk[3]+" "+stk[4]);
			break;

		default: assert false: op.code;	// shouldn't happen
		}
//System.out.print(" => "); for (j=0; j<si; j++) System.out.print(stk[j]+" ");  System.out.println();
	}

	return si;
  }
}
