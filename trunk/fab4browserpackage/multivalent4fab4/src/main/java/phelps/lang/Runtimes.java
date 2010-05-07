package phelps.lang;

import java.io.IOException;



/**
	Extensions to {@link java.lang.Runtime}.

	<ul>
	<li>convenience methods: {@link #execAntWait(String[])}
	</ul>

	@version $Revision$ $Date$
*/
public class Runtimes {
  private Runtimes() {}

  /** Exec's args and returns exit value. */
  public static int execAndWait(String[] args) throws IOException {
	Process p = Runtime.getRuntime().exec(args);
	try { p.waitFor(); } catch (InterruptedException ie) {}
	return p.exitValue();
  }
}
