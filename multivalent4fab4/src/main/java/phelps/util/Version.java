package phelps.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phelps.lang.Strings;



/**
	Manager version strings with multiple and alphanumeric subversions, such as "3.0", "3.0a3", "3.0.2p5", and "3.0.20040316".
	Can compare, e.g., "3.0a3" and "3.0b1", and report that the second is later and {@link #isCompatible(Object)} with the first.

	<p>Letters are interpreted as pre-release test versions, except that 'p' is interpreted as "patch".
	Thus we have 3.0a2 (alpha) < 3.0a3 < 3.0a123 < 3.0b1 (beta) < <b>3.0</b> (release) < 3.2.5 < 3.21 < 3.0.1p1 (patch) < 3.5a1 < 4.0.

	<ul>
	<li>instantiation from {@link #Version(String) String}, {@link #Version(long,long) two numbers}, {@link #Version(long,long,long) three numbers}
	<li>comparison: {@link #compareTo(Object)}, and convenience methods against {@link #compareTo(long,long,long) numbers} and {@link #compareTo(String,String) two version in Strings}
	<li>{@link #isCompatibleWith(Version)}
	<li>{@link #setMin(Version)}
	</ul>

	@see java.lang.Package

	@version $Revision: 1.3 $ $Date: 2004/05/09 00:30:45 $
*/
public class Version implements Comparable {
  private static final Pattern REGEXP_PARSE = Pattern.compile("([0-9][^ \t\n\r\f-]*)");	// allow negative sign?
  private static final Pattern REGEXP_ALLNUM = Pattern.compile("[0-9]+");	// allow negative sign?
  private static final String[] VERSION_NONE = { "0" };

  private String version_;
  private String[] v_;


  public Version(String version) /*throws ParseException*/ {	
	version_ = version;	// preserve as passed, not reconstituted from parsed number parts

	// parse
	Matcher m = REGEXP_PARSE.matcher(version);
	boolean ok = m.find(); //if (!ok) throw new ParseException(); ... VERSION_NONE?
	if (ok) {
		String s = m.group(1);
		List<String> l = new ArrayList<String>(10);
		for (int i=0,imax=s.length(),start=0; i<=imax; i++) {	// split at '.' (discarded) and alpha (kept)
			if (i==imax) l.add(s.substring(start));
			else if (s.charAt(i)=='.') {
				l.add(s.substring(start,i));
				start = i+1;	// exclusive
			} else if (i>start && Character.isDigit(s.charAt(i)) ^ Character.isDigit(s.charAt(i-1))) {	// nondig <=> dig transition
				l.add(s.substring(start,i));
				start=i;	// inclusive
			} // else accumulate
		}
		v_ = (String[])l.toArray(Strings.STRING0);
//System.out.println(version+" => "+l);
	} else {
		version_ = "0";
		v_ = VERSION_NONE;
	}
//System.out.println(version+": "+v_[0]+"."+"...");
  }

  public Version(long major, long minor) {
	version_ = major+"."+minor;
	v_ = new String[] { Long.toString(major), Long.toString(minor) };
  }

  public Version(long major, long minor, long minorminor) {	// convenience method
	version_ = major+"."+minor+"."+minorminor;
	v_ = new String[] { Long.toString(major), Long.toString(minor), /*"p"+ -- NO*/Long.toString(minorminor) };
  }

	/*
  public Version(String major, String minor, String patch) {	// convenience method
	version_ = major+"."+minor+"."+patch;
	v_ = new String[] { major, minor, patch };
  }*/



  public int compareTo(Object o) {
	if (o==null) throw new NullPointerException();
	if (!(o instanceof Version)) throw new ClassCastException("can't compare "+o.getClass().getName()+" to phelps.util.Version");

	int cmp = 0;
	String[] v1 = v_, v2 = ((Version)o).v_;
	int v1len=v1.length, v2len=v2.length;
	for (int i=0, imax=Math.min(v1len,v2len); i<imax && cmp==0; i++) cmp = comparePart(v1[i], v2[i]);

	if (cmp !=0) {}
	else if (v1len < v2len) {
		char ch = Character.toLowerCase(v2[v1len].charAt(0));
		cmp = ch!='p'? 1: -1;	// 'a'lpha / 'b'eta / 'g'amma before number; only 'p'atch greater
	} else if (v1len > v2len) {
		char ch = Character.toLowerCase(v1[v2len].charAt(0));
		cmp = ch!='p'? -1: 1;
	}

	return cmp;
  }

  private int comparePart(String p1, String p2) {
	int cmp = 0;

	// if both number, compare as number
	boolean fd1 = Character.isDigit(p1.charAt(0)), fd2 = Character.isDigit(p2.charAt(0));	// homogeneous parts
	if (fd1 && fd2) {
		//&& REGEXP_ALLNUM.matcher(p1).matches() && REGEXP_ALLNUM.matcher(p1).matches()) {
		try {
			long diff = Long.parseLong(p1) - Long.parseLong(p2);
			cmp = diff<0L? -1: diff>0L? 1: 0;
		} catch (NumberFormatException lotsofdigits) { /*fall through*/}
	}

	// else character-by-character
	if (!fd1 && fd2) cmp=-1; else if (fd1 && !fd2) cmp = 1;	// more numbers always later than alpha/beta/patch
	int p1len=p1.length(), p2len=p2.length();
	for (int i=0, imax=Math.min(p1len,p2len); i<imax && cmp==0; i++) cmp = Character.toLowerCase(p1.charAt(i)) - Character.toLowerCase(p2.charAt(i));

	if (cmp==0) cmp = p1len - p2len;
	return cmp<0? -1: cmp>0? 1: 0;
  }

  /** Convenience method for <code>compareTo(new Version(<var>major</var>, <var>minor</var>))</code>. */
  public int compareTo(long major, long minor) { return compareTo(new Version(major, minor)); }

  /** Convenience method for <code>compareTo(new Version(<var>major</var>, <var>minor</var>, <var>minorminor</var>))</code>. */
  public int compareTo(long major, long minor, long minorminor) { return compareTo(new Version(major, minor, minorminor)); }

  /** Convenience method for <code>new Version(<var>v1</var>).compareTo(new Version(<var>v2</var>))</code>. */
  public static int compareTo(String v1, String v2) { return new Version(v1).compareTo(new Version(v2)); }


  /**
	A version is compatible with another if it is the same version, or a later version with the same major version (first component).
	This follows the convention that incompatible changes increase the major version number.
  */
  public boolean isCompatibleWith(Version v) {
	return comparePart(v_[0], v.v_[0])==0 && compareTo(v) <= 0;
  }


  /**
	Sets version to greater of current and version <var>v</var>.
	In other words, the version can only go up.
  */
  public void setMin(Version v) {
	if (compareTo(v) < 0) { version_=v.version_; v_=v.v_; }
  }


  public boolean equals(Object o) {
	if (this==o) return true;
	if (!(o instanceof Version)) return false;
	Version v2 = (Version)o;
	return Arrays.equals(v_, v2.v_);
  }

  // return version of <var>places</var> places
  //public String toString(int places) { return version_; }

  /** If instantiated version with String, this is what is returned. */
  public String toString() { return version_; }



	/*
  // tests
  public static void main(String[] argv) {
	Version v = new Version("3.0");
	System.out.println(v+": "+v.compareTo(new Version(3,0)));
	System.out.println(v.compareTo(new Version(3,1)));
	System.out.println(v.compareTo(new Version(2,9)));

	v = new Version("3.5");
	System.out.println(v+": "+v.compareTo(new Version(3,5)));
	//System.out.println(v.compareTo(3,0,1));
	String[] vv = { "3.5a2", "3.5a4", "3.5p1", "3.6p1", "3.5a121", "3.5b3", "preceding 3.5b3 following" };
	for (String v2: vv) System.out.println(v.compareTo(new Version(v2)));
	v = new Version("3.5a3"); System.out.println(v);
	for (String v2: vv) System.out.println(v.compareTo(new Version(v2)));

	v = new Version(System.getProperty("java.version"));
  }*/
}
