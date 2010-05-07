package phelps;



/**
	Various funtions that should be part of Java, but aren't.

	<ul>
	<li>{@link #error(String)}, {@link #warning(String)}
	</ul>

	@version $Revision: 1.11 $ $Date: 2003/02/02 11:59:02 $
 */
public class Utility {
	static final boolean DEBUG = !true;

	private Utility() {}    // no instances, no subclasses

	//public static final Object DEFINED = new Object();	// universal non-null => Map.put(key, key)


	/**
	For bad situations that system can fix, it thinks.
	Print message to System.err, but keep running.
	deprecated by Java 1.4 logging
	 */
	public static void warning(String msg) { System.err.println("WARNING: "+msg); }

	/**
	Print message to System.err, exit via <code>System.exit(1)</code>.
	deprecated by Java 1.4 logging
	 */
	public static void error(String msg) { System.err.println("FATAL ERROR: "+msg); System.exit(1); }
}
