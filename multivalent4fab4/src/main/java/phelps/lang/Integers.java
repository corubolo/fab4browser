package phelps.lang;

import java.util.Arrays;



/**
	Extensions to {@link java.lang.Integer}.

	<ul>
	<li>parsing with default value: {@link #parseInt(String, int)}
	<li>possible object sharing: {@link #getInteger(int)}
	<li>formatting: {@link #toRomanString(int)}
	<li>algorithms: {@link #bitCount(int)}
	</ul>

	@version $Revision: 1.5 $ $Date: 2003/11/27 18:40:29 $
*/
public class Integers {
  //public static final Integer[] ARRAY0 = new Integer[0];
  public static final int[] ARRAY0 = new int[0];

  public static final Integer ZERO = new Integer(0);
  public static final Integer ONE = new Integer(1);
  public static final Integer TWO = new Integer(2);
  //public static final Integer NEGATIVE_ONE = Integers.getInteger(-1);


  // cache of common, immutable, primitive-ish objects
  private static final int INTS_MIN=-100, INTS_MAX=1000;  // (4ref + 4val + 4objwrap) * 1100 = >12K
  //int[] ihist =  new int[INTS_MAX - INTS_MIN + 1];
  private static /*final--lazy /*const*/ Integer[] INTS = null;
  //static { for (int i=0, imax=INTS.length, val=INTS_MIN; i<imax; val++, i++) INTS[i]=new Integer(val); } => don't immediate give gc a 1000 objects to manage

  private static final int STRS_MIN=-10, STRS_MAX=100;
  private static /*final--lazy /*const*/ String[] STRS = null;

  private static final String[] ROMAN = {
	"(no zero)", "I","II","III","IV","V","VI","VII","VIII","IX",
	"X", "XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX",
	"XX", "XXI","XXII","XXIII","XXIV","XXV","XXVI","XXVII","XXVIII","XXIX",
	"XXX", "XXXI","XXXII","XXXIII","XXXIV","XXXV","XXXVI","XXXVII","XXXVIII","XXXIX",
	"XL", "XLI","XLII","XLIII","XLIV","XLV","XLVI","XLVII","XLVIII","XLIX",
   };

  private static /*final--lazy*/ int[] RADIX = null; // could be byte[] since values are all < 0x80 so positive as bytes

  private static final byte[] BIT_COUNT = new byte[256];	// max value is 8
  static {
	BIT_COUNT[0]=0;
	for (int base=1; base<256; base*=2) {
		for (int i=0; i<base; i++) BIT_COUNT[base+i] = (byte)(BIT_COUNT[i] + 1);	// old count + high bit set
	}
  }


  private Integers() {}


  /**
	Tries to parse <var>value</var> as an <code>int</code>,
	but if String is <code>null</code> or can't be parsed as an <code>int</code> returns <var>defaultval</var> .
  */
  public static int parseInt(String value,/* int radix, */ int defaultval) {
	if (value==null) return defaultval;
	try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return defaultval; }
  }

  /**
	Interprets <var>ch</var> as ASCII character in radix up to 36, and returns value, or if not hex returns -1.
	Radixes > 10 use letters, which may be lower or uppercase.
  */
  public static int parseInt(int ch) {
	if (RADIX==null) {
		RADIX = new int[0x100];
		Arrays.fill(RADIX, -1);   // not 0 because that's a valid value
		for (int i=0, O='0'; i<10; i++, O++) RADIX[O] = i;
		for (int i=10, a='a', A='A'; a<='z'; i++, a++, A++) RADIX[a] = RADIX[A] = i;
	}

	//assert Characters.isHexDigit(ch);
	//return val = '0'<=ch&&ch<'9'? ch-'0': 'a'<=ch&&ch<='f'? ch-'a': 'A'<=ch&&ch<='F'? ch-'A': -1;
	return 0 <= ch&&ch <=255? RADIX[ch]: -1;
  }
  /** Interprets <var>ch1</var> and <var>ch2</var> as pair of hexidecimal characters, and returns combined 8-bit value. */
  public static int parseHex(int ch1, int ch2) { return (parseInt(ch1)<<4) | parseInt(ch2); }


  /** Caches {@link java.lang.Integer} objects, so the 1000s of instances of 0 all share the same Java object. */
  public static Integer getInteger(int val) {
	//return INTS_MIN<=val && val<=INTS_MAX? INTS[val - INTS_MIN]: new Integer(val);
	Integer into;
	if (INTS_MIN <= val&&val <= INTS_MAX) {
		if (INTS==null) {
			INTS = new Integer[(int)(INTS_MAX - INTS_MIN + 1/*including 0*/)];
			INTS[-INTS_MIN]=ZERO; INTS[-INTS_MIN+1]=ONE; INTS[-INTS_MIN+2]=TWO;
		}

		into = INTS[val - INTS_MIN];
		if (into==null) into = INTS[val - INTS_MIN] = new Integer(val);

	} else into = new Integer(val);	// later maybe keep hash of last N

	return into;
  }
//if (INTS_MIN<=val && val<=INTS_MAX) ihist[val-INTS_MIN]++;


  /**
	Returns Roman numeral representation of numbers >0 && <=4000 (no numbers which require a bar to multiply by 1000).
	Numeral returned in uppercase; client can convert to lowercase.
	Numbers <0 or >=4000 are returned in Arabic.
	Used by HTML OL.
  */
  public static String toRomanString(int val) {   // like toHexString
	//assert => return Arabic
	if (val<=0 || val>=4000) /*throw NumberFormatException()*/return Integer.toString(val);

	if (val<ROMAN.length) return ROMAN[val];  // fast path for small numbers -- no object creation

	// i=1, v=5, x=10, l=50, c=100, d=500, m=1000, bar=multiply by 1000, no 0
	StringBuffer rn = new StringBuffer(10);
	while (val>=1000) { rn.append('M'); val-=1000; }
	if (val>=900) { rn.append("CM"); val-=900; }
	if (val>=500) { rn.append('D'); val-=500; }
	if (val>=400) { rn.append("CD"); val-=400; }
	while (val>=100) { rn.append('C'); val-=100; }
	if (val>=90) { rn.append("XC"); val-=90; }
	if (val>=50) { rn.append('L'); val-=50; }
	assert ROMAN.length >= 50;
	//if (val>=40) { rn.append("XL"); val-=40; }
	//while (val>=10) { rn.append('X'); val-=10; }
	//if (val>0) rn.append(ROMAN[val]);
	if (val>0) rn.append(ROMAN[val]);

	return rn.toString();
  }


  /**
	Returns number of 1-bits in 2's complement representation of 4-byte integer <var>value</var>.
	To be replaced by <code>Integer.bitCount(int)</code> in Java 1.5.
  */
  public static int bitCount(int value) {
	return BIT_COUNT[value&0xff] + BIT_COUNT[(value>>8)&0xff] + BIT_COUNT[(value>>16)&0xff] + BIT_COUNT[(value>>24)&0xff];
  }

  public static String toString(int val) {
	String s;
	if (STRS_MIN<=val && val<=STRS_MAX) {
		if (STRS==null) STRS = new String[(int)(STRS_MAX - STRS_MIN + 1)];

		s = STRS[val - STRS_MIN];
		if (s==null) s = STRS[val - STRS_MIN] = Integer.toString(val);
	} else s = Integer.toString(val);

	return s;
  }
}
