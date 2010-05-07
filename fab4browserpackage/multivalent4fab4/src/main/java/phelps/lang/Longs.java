package phelps.lang;



/**
	Extensions to {@link java.lang.Long}.

	@version $Revision: 1.1 $ $Date: 2003/02/02 12:00:14 $
*/
public class Longs {
  private Longs() {}

  /**
	Tries to parse <var>value</var> as an <code>long</code>,
	but if String is <code>null</code> or can't be parsed as an <code>int</code> returns <var>defaultval</var> .
  */
  public static long parseLong(String value, long defaultval) {
	if (value==null) return defaultval;
	try { return Long.parseLong(value.trim()); } catch (NumberFormatException e) { return defaultval; }
  }
}
