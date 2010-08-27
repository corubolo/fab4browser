package phelps.lang;



/**
	Extensions to {@link java.lang.Double}.

	@version $Revision: 1.1 $ $Date: 2003/02/02 11:55:19 $
*/
public class Doubles {
  public static final Double ZERO = new Double(0.0);
  public static final Double ONE = new Double(1.0);

  /** If difference between two <code>double</code>s is less than this, then they should be considered equal. */
  public static final double EPSILON = 0.00001;


  private Doubles() {}

  /**
	Tries to parse <var>value</var> as an <code>double</code>,
	but if String is <code>null</code> or can't be parsed as an <code>int</code> returns <var>defaultval</var> .
  */
  public static double parseDouble(String value, double defaultval) {
	if (value==null) return defaultval;
	try { return Double.parseDouble(value); } catch (NumberFormatException e) { return defaultval; }
  }
}
