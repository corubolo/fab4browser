package phelps.lang;



/**
	Extensions to {@link java.lang.Math}.

	<ul>
	<li>{@link #minmax(int,int,int) bounded values}
	</ul>

	@version $Revision: 1.1 $ $Date: 2005/01/11 14:01:48 $
*/
public class Maths {
  private Maths() {}

  public static int minmax(int min, int val, int max) {
	return min <= val&&val <= max? val: val<min? min: max;
  }

  public static long minmax(long min, long val, long max) {
	return min <= val&&val <= max? val: val<min? min: max;
  }

  public static float minmax(float min, float val, float max) {
	return Math.max(min, Math.min(val, max));	// let java.lang.Math worry about NaN
  }

  public static double minmax(double min, double val, double max) {
	return Math.max(min, Math.min(val, max));	// let java.lang.Math worry about NaN
  }

  /*public static int max(int a, int b, int c) { -- not frequent enough, and Math.min(Math.min()) is readable
	return a>b? (a>c? a: c): (b>c? b: c);
  }*/
}
