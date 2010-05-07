package phelps.lang;



/**
	Extensions to {@link java.lang.Float}.

	@version $Revision: 1.1 $ $Date: 2003/02/02 11:55:48 $
*/
public class Floats {
  public static final Float ZERO = new Float(0f);
  public static final Float ONE = new Float(1f);

  /** If difference between two <code>float</code>s is less than this, then they should be considered equal. */
  public static final float EPSILON = 0.00001f;


  private Floats() {}

  /**
	Tries to parse <var>value</var> as an <code>float</code>,
	but if String is <code>null</code> or can't be parsed as an <code>int</code> returns <var>defaultval</var> .
  */
  public static float parseFloat(String value, float defaultval) {
	if (value==null) return defaultval;
	try { return Float.parseFloat(value); } catch (NumberFormatException e) { return defaultval; }
  }


  public static Float getFloat(float val) {
	return val==0f? ZERO: val==1f? ONE: new Float(val);
  }
}
