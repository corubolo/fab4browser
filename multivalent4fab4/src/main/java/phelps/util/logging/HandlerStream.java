package phelps.util.logging;

import java.util.logging.*;
import java.io.OutputStream;



/**
	Unbuffered/autoflush.

<!--
	LATER: accepts {@link com.pt.io.OutputDestination}.
	LATER: configure to ignore certain classes?
-->

	@version $Revision$ $Date$
*/
public class HandlerStream extends StreamHandler {
  public HandlerStream(OutputStream out, Formatter formatter) {
	super(out, formatter);
  }

  public void publish(LogRecord record) {
	if (isLoggable(record)) { super.publish(record); super.flush(); }
  }
}
