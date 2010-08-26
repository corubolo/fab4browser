package phelps.util;

import java.awt.geom.Dimension2D;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import phelps.awt.geom.Dimension2D_Double;
import phelps.lang.Strings;
import phelps.util.Arrayss;



/**
	Units of measure.

	<ul>
	<li>length: <a href='#lengths'>built-in units</a>, {@link #getLength(String, String)}, {@link #convertLength(String, String)}, {@link #addLength(String, String, String, double)}
	<li>paper size: <a href='#papers'>built-in papers sizes</a>, {@link #getPaperSize(String, int)}, {@link #addPaperSize(String,String)}
	<li>number range, as of page numbers: {@link #parseRange(String, int)}, {@link #toRange(int[])}
	<li>size with metric suffix K/M/G/...: {@link #prettySize(long)}, {@link #parseSize(String)}
	</ul>

	@version $Revision: 1.12 $ $Date: 2005/03/06 05:51:29 $
*/
public class Units {
  /** Value returned in {@link parseRange(String,int,int)} array when invalid component is found. */
  public static final int RANGE_INVALID = Integer.MIN_VALUE;

  private static final Matcher MATCHER_LENGTH = Pattern.compile("([-+0-9.]+)\\s*(\\w+)?").matcher("");
  private static final Map<String,Double> length_ = new HashMap<String,Double>();
  static {
	String[] units = {
		"meter","meters","m", "centimeter","centimeters","cm", "millimeter","millimeters","mm",
		"inch","inches","in", "foot","feet","ft", "mile","miles","mile",
		"point","points","pt", "scaled point","scaled point","sp", "big point","big points","bp", "pica","picas","pc", "didot","didots","dd", "cicero","ciceros","cc",
		//"px" -- special case
	};
	final double IN = 2.54/100.0;
	double[] fac = {
		1.0, 1.0/100.0, 1.0/1000.0, 
		IN, 12*IN, 5280*12*IN,
		IN/72.27, IN/65536.0, IN/72.0, IN*12.0/72.0, IN*1238.0/(1157.0*72.27), IN*12.0*1238.0/(1157.0*72.27)
	};
	assert units.length == 3*fac.length;
	for (int i=0,imax=fac.length; i<imax; i++) addLength(units[i*3], units[i*3+1], units[i*3+2], fac[i]);
	int ppi = java.awt.GraphicsEnvironment.isHeadless()? 72: java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
	addLength("pixel","pixels","px", IN / ppi);
	// check that no dup abbrev
  }

  private static final Matcher PAPERMATCHER_ = Pattern.compile("([0-9.]+)\\s*x\\s*([0-9.]+)\\s*(\\w+)").matcher("");
  private static final Map<String,Dimension2D> paper_ = new HashMap<String,Dimension2D>();
  static {
	String[] papers = {
		"US","8.5x11in", "letter","8.5x11in", "legal","8.5x14in", "foolscap","13.5x17in",
		"4A0","1682x2378mm", "2A0","1189x1682mm", "A0","841x1189mm", "A1","594x841mm", "A2","420x594mm", "A3","297x420mm", "A4","210x297mm",
		"A5","148x210mm", "A6","105x148mm", "A7","74x105mm", "A8","52x74mm", "A9","37x52mm", "A10","26x37mm",
		"B0","1000x1414mm", "B1","707x1000mm", "B2","500x707mm", "B3","353x500mm", "B4","250x353mm", "B5","176x250mm", 
		"B6","125x176mm", "B7","88x125mm", "B8","62x88mm", "B9","44x62mm", "B10","31x44mm",
		"C0","917x1297mm", "C1","648x917mm", "C2","458x648mm", "C3","324x458mm", "C4","229x324mm", "C5","162x229mm", "C6","114x162mm", "C7","81x114mm", "C8","57x81mm", "C9","40x57mm", "C10","28x40mm", 
	};
	for (int i=0,imax=papers.length; i<imax; i+=2) addPaperSize(papers[i], papers[i+1]);
  }


  private static final String[] KSUFFIX = { "bytes", "KB", "MB", "GB", "TB", "PB", "EB" };
  private static final long[] KUNIT = { 1L, 1024L, 1024L*1024, 1024L*1024*1024, 1024L*1024*1024*1024, 1024L*1024*1024*1024*1024, 1024L*1024*1024*1024*1024*1024 };


  private Units() {}


  /** Parse <var>valunit</var> for number and unit (missing unit intepreted as 'px'), and return conversion to <var>unitout</var>. */
  public static double getLength(String valunit, String unitout) throws NumberFormatException {
	Matcher m = MATCHER_LENGTH;
	m.reset(valunit);
	if (!m.find()) return Double.MIN_VALUE;	//throws....

	double val = Double.parseDouble(m.group(1));
	String unit = m.groupCount()==2? m.group(2): "px";
	double fac = convertLength(unit, unitout);
//System.out.println(valunit+" => "+v.substring(0,x)+" / "+v.substring(x)+" => "+(val*fac)+" in "+unitout);
	return val * fac;
  }

  /**
	Return conversion factor from <var>unitin</var> to <var>unitout</var>.
  */
  public static double convertLength(String unitin, String unitout) {
	Double d1 = length_.get(unitin.trim().toLowerCase()), d2 = length_.get(unitout.trim().toLowerCase());
	assert d1!=null: d1;
	assert d2!=null: d2;
	return d1.doubleValue()/*into meters*/ / d2.doubleValue() /*back to non-meters*/;
  }

  /**
	Add a new unit of length by supplying <var>abbrev</var>iation for parsing and conversion <var>factor</var> to meters.
	Built-in units:
	<ul id='lengths'>
	<li>metric: m (meters), cm (centimeters), mm (millimeters), 
	<li>English: in (inches), ft (feet), mile (miles)
	<li>printer: pt (points -- 72.27/in), sp (scaled points -- TeX), bp (big points -- 72/in), pc (picas), dd (didot points), and cc (ciceros)
	<li>screen: px (pixel -- according to current screen)
	</ul>
  */
  public static void addLength(String name, String plural, String abbrev, double factor) {
	assert abbrev!=null && factor!=0.0;
	Double fac = new Double(factor);
	length_.put(name.trim().toLowerCase(), fac);
	length_.put(plural.trim().toLowerCase(), fac);
	length_.put(abbrev.trim().toLowerCase(), fac);
  }



  /**
	Returns size in units for passed paper size.
	Accepted paper sizes the <a href='#papers'>built-in names</a> and 
	explicit dimensions given in <var>number</var>x<var>number</var><var>unit</var> syntax.
	A trailing 'r' on <var>size</var> rotates by 90 degrees clockwise; e.g., "usr" returns 11x8.5in.
  */
  public static Dimension2D getPaperSize(String size, String unitout) {
	boolean frot = false;
	String s = Strings.removeWhitespace(size).toLowerCase();
	Dimension2D dim = paper_.get(s);
	if (dim==null && s.indexOf('x')!=-1) { dim = computeWxH(size); if (size.endsWith("r")) frot=true; }
	else if (dim==null && s.endsWith("r")) { frot=true; s=s.substring(0,s.length()-1); dim = paper_.get(s); }
	//if (dim==null) throw...

	double w=dim.getWidth(), h=dim.getHeight();
	if (frot) { double tmp=w; w=h; h=w; }
	double fac = convertLength("mm", unitout);
	w*=fac; h*=fac;

	return new Dimension2D_Double(w,h);
  }

  /**
	Add new paper size, giving <var>abbrev</var>iation and <var>dimensions</var> in <var>width-number</var>x<var>height-number</var><var>unit-of-length</var>.
	Built-in paper sizes:
	<ul id='papers'>
	<li>American: US (8.5x11in), legal (8.5x14in)
	<li>ISO: A0 - A10, B0 - B10, C0 - C10
	<li>foolscap (13.5x17in)
	</ul>
  */
  public static void addPaperSize(String abbrev, String dimensions) {
	assert abbrev!=null && dimensions!=null;
	paper_.put(abbrev.toLowerCase(), computeWxH(dimensions));
  }

  /** Parse dimension of WxHu syntax. */
  private static Dimension2D computeWxH(String whu) {
	whu = Strings.removeWhitespace(whu);
	Matcher m = PAPERMATCHER_;
	m.reset(whu);
	if (!m.find()) return null;	//throws....

	double w = Double.parseDouble(m.group(1)), h = Double.parseDouble(m.group(2));
	double fac = convertLength(m.group(3), "mm");

	return new Dimension2D_Double(w*fac,h*fac);
  }



  /** Given a byte count, returns a string in more human-readable form, at the possible loss of exactness, e.g., 13*1024*1024 => "13MB". */
  public static String prettySize(long bytes) {
	if (bytes==0) return "0 bytes"; else if (bytes==1) return "1 byte"; else if (bytes==-1) return "-1 byte";	 // special cases

	long sign = (bytes>=0? 1: -1);
	bytes = Math.abs(bytes);
	long K = 1024, div=0, rem=0;
	int sfxi = 0;
	while (bytes > K && sfxi+1<KSUFFIX.length) {
		div=bytes/K; rem=bytes - div*K;
		bytes = div;
		sfxi++;
	}
	return Long.toString(sign*bytes)+(rem>0?".":"")+(bytes<=10 && rem>0? Long.toString((rem*10)/K): "")+" "+KSUFFIX[sfxi];
  }

  public static long parseSize(String s) {
	s = Strings.removeWhitespace(s);
	long val = 0L;
	int i=0, imax=s.length();
	while (i<imax) {
		char ch=s.charAt(i);
		if ('0' <= ch&&ch <='9') val = val*10 + ch-'0';
		else break;
	}
	while (i<imax) if (!Character.isWhitespace(s.charAt('0'))) break;
	if (i<imax) {	// suffix?
		char ch = Character.toLowerCase(s.charAt(i));
		int inx = ch=='k'? 1: ch=='m'? 2: ch=='g'? 3: ch=='t'? 4: ch=='p'? 5: ch=='e'? 6: 0;
		val = val * KUNIT[inx];
	}
	return val;
  }



  /** Converts array of numbers into sequence of ranges, of the form parsed by {@link #parseRange(int[])}. */
  public static String toRange(int[] nums/*, int min, int max, String invalid*/) {
	assert nums!=null;
	StringBuffer sb = new StringBuffer(100);

	for (int i=0,imax=nums.length; i<imax; i++) {
		int pg1 = nums[i];
		sb.append(RANGE_INVALID==pg1? "b": /*min, max, ...*/ Integer.toString(pg1));

		if (i+1 < imax) {
			int j=i+1, step = nums[j]-pg1;
			j++; while (j < imax && nums[j]-nums[j-1]==step) j++;
			int cnt = j-i, pg2=nums[j-1]; assert cnt>=2;

			if (cnt==2) {}	// no pattern: two simple numbers
			else if (step==0) { sb.append("*").append(cnt); i = i+cnt-1; }	// copies
			else if (RANGE_INVALID==pg1 || RANGE_INVALID==pg2) {}	// no part of any ranges
			else if (step==1 /*&& assert cnt >=3*/) { sb.append("-").append(pg2); i = i+cnt-1; }
			else if (cnt >= 4) {	// "m-n % k"
				sb.append("-").append(pg2);
				/*if (pg1=min && pg2=max && step==2) sb.append("odd/even") => need to know min and max
				else*/ if (step==2 /*&& cnt>=3*/ && pg1>0) sb.append(pg1%2==1? "%odd": "%even");
				else if (step==-1) sb.append("%reverse");
				else sb.append("%").append(step);
				i = i+cnt-1;
			}
		}

		sb.append(",");
	}

	return sb.substring(0,sb.length()-1);
  }

  /**
	Parses range specification like "1-3,5,1,7-20" into array of <code>int</code>'s (<code>int[]</code>).
	See <a href='http://multivalent.sourceforge.net/Tools/HowToRun.html#range'><var>range</var> gammar</a>.

	If <var>range</var> is <code>null</code>, returns array <var>min</var>..<var>max</var>, inclusive.
  */
  public static int[] parseRange(String range, int min, int max) /*throws ParseException*/ {
	//assert /*range!=null && range.length()>=1 &&*/ max>=1; // could range from -50 to 2
	// normalize
	if (range==null || range.length()==0) range = "all";
	range = range.replace(';',',');
	int inx = range.lastIndexOf('/'); if (inx!=-1) range = range.substring(0,inx);
	range = Strings.removeWhitespace(range);

	int[] nums = new int[max-min+1];
	int numsi=0;
	boolean fback = false;

	for (StringTokenizer st = new StringTokenizer(range, ", "); st.hasMoreElements(); ) {
		String snum = st.nextToken().toLowerCase(); if (snum.length()==0) continue;
//System.out.println(" tok="+snum+"/"+nums.length);
		try {
			int pg1, pg2, step=1;

			String sstep = null;
			int stepinx = snum.lastIndexOf('%'); if (stepinx!=-1) { sstep = snum.substring(stepinx+1); snum = snum.substring(0,stepinx); }
			inx = snum.indexOf('-');


			// not page numbers
			if (snum.startsWith("back"/*map*/)) { fback = true; continue; }
			// page numbers
			else if ("all".equals(snum)) { pg1=min; pg2=max; }
			else if (snum.startsWith("rev"/*erse*/)) { pg1=max; pg2=min; }
			else if ("odd".equals(snum)) { pg1 = min + ((min%2)==0? 1: 0); pg2=max; step=2; }
			else if ("even".equals(snum)) { pg1 = min + ((min%2)==0? 0: 1); pg2=max; step=2; }
			else if (inx==-1) {
				char ch0 = snum.charAt(0);
				if (ch0=='-' || Character.isDigit(ch0)) pg1 = Integer.parseInt(snum);
				else if ("first".equals(snum)) pg1 = min;
				else if ("last".equals(snum)) pg1 = max;
				else pg1 = RANGE_INVALID;
				pg2 = pg1;
			} else {	// range
				String snum1 = snum.substring(0,inx), snum2 = snum.substring(inx+1);
				pg1 = "first".equals(snum1) || "".equals(snum1)? min: "end".equals(snum1) || "last".equals(snum1)? max: Integer.parseInt(snum1);
				pg2 = "first".equals(snum1)? min: "".equals(snum2) || "end".equals(snum2) || "last".equals(snum2)? max: Integer.parseInt(snum2);
			}

//System.out.println("Units "+snum+" => "+pg1+" .. "+pg2+" by "+sstep);
			pg1 = pg1==RANGE_INVALID? pg1: pg1<min? min: pg1>max? max: pg1;
			pg2 = pg2==RANGE_INVALID? pg2: pg2<min? min: pg2>max? max: pg2;
			if (sstep!=null) {
				if (sstep.startsWith("odd")) { step=2; if (pg1%2==0) pg1++; }
				else if (sstep.startsWith("even")) { step=2; if (pg1%2!=0) pg1++; }
				else if (sstep.startsWith("rev"/*erse*/)) { int tmp=pg1; pg1=pg2; pg2=tmp; }
				else step = Integer.parseInt(sstep);
			}

//System.out.println("snum = "+snum+", pg1="+pg1+", pg2="+pg2+", step="+step);
			int stepabs = Math.abs(step), len = pg1==pg2? stepabs: Math.abs(pg2-pg1) / stepabs + 1/*int-rounding*/;
//System.out.println("  "+pg1+".."+pg2+" / "+(nums.length-numsi)+" + "+len);
			if (len + numsi >= nums.length) nums = Arrayss.resize(nums, len + nums.length*2);
			if (pg1==pg2) for (int i=0; i<step/*not stepabs*/; i++) nums[numsi++] = pg1;
			else if (pg1<=pg2) for (int i=pg1; i<=pg2; i+=stepabs) nums[numsi++] = i;
			else for (int i=pg1; i>=pg2; i-=stepabs) nums[numsi++] = i;

		} catch (NumberFormatException nfe) { System.out.println(nfe); }  // ignore
	}
	nums = Arrayss.resize(nums, numsi);

	if (fback && numsi>=1) {	// sort indices 0,1,2,... by range_[]
		// O(n log n) sort + O(n log n) search = O(n log n)
		int[] sorted = (int[])nums.clone(); Arrays.sort(sorted);
		int[] back = new int[numsi];
		for (int i=0; i<numsi; i++) back[Arrays.binarySearch(sorted, nums[i])] = i + 1/*pages 1-based*/;
		nums = back;
	}

//System.out.print(range+" =>"); for (int n: nums) System.out.print(" "+n);  System.out.println();
	return nums;
  }


  /** Possibly multiple {@link #parseRange(String,int,int)}s, as divided by ';' or '/'. */
  public static int[][] parseRanges(String range, int min, int max) /*throws ParseException*/ {
	List<int[]> l = new ArrayList<int[]>(5);
	for (StringTokenizer st = new StringTokenizer(range, ";"); st.hasMoreTokens(); ) {
		String r = st.nextToken(); int group = -1;
		int inx = r.lastIndexOf('/');
		if (inx != -1) {
			try { group = Integer.parseInt(r.substring(inx+1)); } catch (NumberFormatException nfe) {}
			inx = r.indexOf('/'); r = r.substring(0,inx);	// strip multiple '/<num>'
		}

		int[] simple = parseRange(r, min,max);
		if (group > 0) for (int i=0,imax=simple.length; i<imax; i += group) l.add((int[])Arrayss.subset(simple, i,Math.min(group, imax-i)));
		else l.add(simple);
	}

	return (int[][])l.toArray(new int[l.size()][]);
  }

/*
  public static void main(String[] argv) {
	for (int[] r: parseRanges(argv[0], 1,100)) {
		System.out.print(" =>"); for (int n: r) System.out.print(" "+n);
		System.out.println(" => "+toRange(r));
	}
	System.exit(0);
  }*/
}
