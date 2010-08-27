package phelps.lang;



/**
	Extensions to {@link java.lang.Boolean}.

	@version $Revision: 1.1 $ $Date: 2003/02/02 11:54:49 $
*/
public class Booleans {
  private Booleans() {}

  /**
	Tries to parse <var>value</var> as an <code>boolean</code> --
	<code>true</code> iff (case insensitive) "true", "yes", "on" (not "no"!), "1", {@link Boolean#TRUE} --
	but if String is <code>null</code> or can't be parsed as an <code>int</code> returns <var>defaultval</var> .
  */
  public static boolean parseBoolean(Object value, boolean defaultval) {
	boolean bval = defaultval;

	if (value==null) {} // toggle?
	else if (value instanceof String) {
		String sval = ((String)value).trim().toLowerCase();
		bval = "true".equals(sval) || "yes".equals(sval) || "on".equals(sval) || "1".equals(sval);
	} else if (value instanceof Boolean) {
		bval = ((Boolean)value).booleanValue();
	}

	return bval;
  }
}
