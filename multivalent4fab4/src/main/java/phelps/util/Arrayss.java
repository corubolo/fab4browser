package phelps.util;

//import java.util.Arrays;	// for Javadoc references
import java.util.Comparator;



/**
	Extensions to {@link java.util.Arrays}.

	<ul>
	<li>{@link #subset(byte[],int,int)} returns new array of passed length, copying all the elements of original array that fit.
	<li>{@link #resize(byte[], int)} returns new array of passed length, copying all the elements of original array that fit.  If new size is same as old size, passed array is returned.
		This can subset an existing array, or increase the array length.
	<li>{@link #concat()} returns new array that is concatenation of passed arrays
	<li>{@link #indexOf(Object[], Object)} - like List.indexOf(val)
	<li>{@link #isSorted(Object[])} - verify that array is sorted, useful in assertions
	<li>{@link #fillIdentity(int[])}
	</ul>

	@version $Revision: 1.3 $ $Date: 2003/12/28 05:28:29 $
*/
public class Arrayss {
  private Arrayss() {}


  public static byte[] subset(byte[] a, int start, int length) {
	byte[] b = new byte[length]; System.arraycopy(a,start, b,0, Math.min(length,a.length-start)); return b;
  }

  public static char[] subset(char[] a, int start, int length) {
	char[] b = new char[length]; System.arraycopy(a,start, b,0, Math.min(length,a.length-start)); return b;
  }

  public static int[] subset(int[] a, int start, int length) {
	int[] b = new int[length]; System.arraycopy(a,start, b,0, Math.min(length,a.length-start)); return b;
  }

  public static String[] subset(String[] a, int start, int length) {
	String[] b = new String[length]; System.arraycopy(a,start, b,0, Math.min(length,a.length-start)); return b;
  }

  public static Object[] subset(Object[] a, int start, int length) {
	Object[] b = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), length);
	System.arraycopy(a,start, b,0, Math.min(length,a.length-start));
	return b;
  }


	/*
if (a.length == newlength) return a;
else if (a==null) 

	 */

  public static byte[] resize(byte[] a, int newlength) {
	assert newlength>=0: newlength;

	byte[] b;
	if (a==null) b = new byte[newlength];
	else if (a.length == newlength) b = a;
	else { b = new byte[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static short[] resize(short[] a, int newlength) {
	assert newlength>=0: newlength;
	short[] b;
	if (a==null) b = new short[newlength];
	else if (a.length == newlength) b = a;
	else { b = new short[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static char[] resize(char[] a, int newlength) {
	assert newlength>=0: newlength;
	char[] b;
	if (a==null) b = new char[newlength];
	else if (a.length == newlength) b = a;
	else { b = new char[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static int[] resize(int[] a, int newlength) {
	assert newlength>=0: newlength;
	int[] b;
	if (a==null) b = new int[newlength];
	else if (a.length == newlength) b = a;
	else { b = new int[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static long[] resize(long[] a, int newlength) {
	assert newlength>=0: newlength;
	long[] b;
	if (a==null) b = new long[newlength];
	else if (a.length == newlength) b = a;
	else { b = new long[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static float[] resize(float[] a, int newlength) {
	assert newlength>=0: newlength;
	float[] b;
	if (a==null) b = new float[newlength];
	else if (a.length == newlength) b = a;
	else { b = new float[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static String[] resize(String[] a, int newlength) {	// special case of Object[] so don't have to cast result
	assert newlength>=0: newlength;
	String[] b;
	if (a==null) b = new String[newlength];
	else if (a.length == newlength) b = a;
	else { b = new String[newlength]; System.arraycopy(a,0, b,0, Math.min(a.length, newlength)); }
	return b;
  }

  public static Object[] resize(Object[] a, int newlength) {
	assert newlength>=0: newlength;
//if (a!=null) System.out.println("resize, class = "+cl.getName()+", length = "+newlength+", superclass = "+cl.getSuperclass().getName());
	Object[] b;
	if (a==null) b = new Object[newlength];
	else if (a.length == newlength) b = a;
	else {
		b = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), newlength);	// superclass of array is simple type
		System.arraycopy(a,0, b,0, Math.min(a.length, newlength));
	}
	return b;
  }

	/* caller has to cast result
  public static Object resize(Object a, int newlength) {
	assert newlength>=0: newlength;

	Object b = java.lang.reflect.Array.newInstance(a.getClass(), newlength);
	System.arraycopy(a,0, b,0, Math.min(java.lang.reflect.Array.getLength(a), newlength));
	return b;
  }*/

  public static Object[] concat(Object[] a, Object[] b) {
	Object[] out = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getSuperclass(), a.length + b.length);
	System.arraycopy(a,0, out,0, a.length);
	System.arraycopy(b,0, out,a.length, b.length);
	return out;
  }


  // for this could use Arrays.asList(a).indexOf(), but have so parallel to those for primitives
  public static int indexOf(Object[] a, Object b) {
	assert a!=null && b!=null;	// allow null for b?
	//if (a==null) return -1;
	for (int i=0,imax=a.length; i<imax; i++) if (b.equals(a[i])) return i;
	return -1;
  }

  /**
	Finds first index of <var>key</var> in <var>a</var> or -1 if not present.
	If array is sorted, use {@link Arrays#binarySearch(byte[], byte)} instead.
  */
  public static int indexOf(byte[] a, byte key) {
	if (a==null) return -1;
	for (int i=0,imax=a.length; i<imax; i++) if (key == a[i]) return i;
	return -1;
  }

  public static int indexOf(int[] a, int val) {
	if (a==null) return -1;
	for (int i=0,imax=a.length; i<imax; i++) if (a[i]==val) return i;
	return -1;
  }

  /**
	Finds first index of <var>key<var> in <var>a</var> or -1 if not present.
	To search for string of 8-bit characters, first convert string to <code>byte[]</code> with {@link phelps.lang.Strings#getBytes8(String)}.
  */
  public static int indexOf(byte[] a, byte[] key) { return indexOf(a, key, 0,a.length); }
  public static int indexOf(byte[] a, byte[] key, int start) { return indexOf(a, key, start, a.length); }
  public static int indexOf(byte[] a, byte[] key, int start, int end) {
	end = Math.min(end, a.length);
	if (a==null || key==null || end < key.length) return -1;
	byte key0 = key[0];
	for (int i=start,imax=end - key.length; i<imax; i++) {
		if (key0 == a[i]) {
			boolean fmatch = true;
			for (int j=0+1,jmax=key.length; j<jmax; j++) if (key[j] != a[i+j]) { fmatch=false; break; }
			if (fmatch) return i;
		}
	}
	return -1;
  }

  public static int lastIndexOf(byte[] a, byte[] key) {
	if (a==null || key==null || key.length==0) return -1;
	byte k0 = key[0];
	for (int i=a.length - key.length; i>=0; i--) {
		if (a[i] == k0) {
			boolean found = true;
			for (int j=0+1, jmax=key.length; j<jmax; j++) if (key[j]!=a[i+j]) { found=false; break; }
			if (found) return i;
		}
	}
	return -1;
  }

	/*  public static int indexOf(Object[] oa, Object o) {
	for (int i=0, imax=oa.length; i<imax; i++) if (oa[i]==o) return i;
	return -1;
  }*/

  //public static boolean equals(byte[] a, int starta, byte[] b) { return equals(a, starta, b, 0, b.length); }
  public static boolean equals(byte[] a, int starta, byte[] b, int startb, int length) {
	if (a==b) return true;
	if (a==null || b==null) return false;
	assert starta + length <= a.length && startb + length <= b.length;

	for (int i=0; i<length; i++) {
		if (a[starta + i] != b[startb + i]) return false;
	}
	return true;
  }


  /**
	Returns true if the array is sorted.
	Array should contain objects that are {@link Comparable}.
  */
  public static boolean isSorted(Object[] oa) {
	if (oa!=null) for (int i=0,imax=oa.length-1; i<imax; i++) if (((Comparable)oa[i]).compareTo(oa[i+1]) > 0) return false;
	return true;
  }

  /** Returns true if the array is sorted according to <var>comp</var>. */
  public static boolean isSorted(Object[] oa, Comparator comp) {
	if (oa!=null) for (int i=0,imax=oa.length-1; i<imax; i++) if (comp.compare(oa[i],oa[i+1]) > 0) return false;
	return true;
  }

  public static boolean isSorted(int[] ia) {
	if (ia!=null) for (int i=0,imax=ia.length-1; i<imax; i++) if (ia[i] > ia[i+1]) return false;
	return true;
  }



  public static int[] fillIdentity(int[] a) {
	for (int i=0,imax=a.length; i<imax; i++) a[i] = i;
	return a;
  }

  public static char[] fillIdentity(char[] a) {
	for (int i=0,imax=a.length; i<imax; i++) a[i] = (char)i;
	return a;
  }

  public static byte[] fillIdentity(byte[] a) {
	for (int i=0,imax=a.length; i<imax; i++) a[i] = (byte)i;
	return a;
  }
}
