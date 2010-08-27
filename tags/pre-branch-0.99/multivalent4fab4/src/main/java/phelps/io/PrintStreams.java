package phelps.io;

import java.io.PrintStream;



/**
	Utility methods for {@link java.io.PrintStream}s.

	<ul>
	<li>{@link #DEVNULL}
	</ul>

	@version $Revision: 1.1 $ $Date: 2003/08/17 14:26:24 $
*/
public class PrintStreams {
  public static final PrintStream DEVNULL = new PrintStream(OutputStreams.DEVNULL);

  private PrintStreams() {}
}
