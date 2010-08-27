package phelps.util.logging;

import java.util.logging.*;



/**
	Writes short long record (single line).

<!--
	LATER: configurable information?
-->

	@version $Revision$ $Date$
*/
public class FormatterShort extends Formatter {
  public FormatterShort() {
  }

  public String format(LogRecord record) {
	return phelps.lang.Classes.getTail(record.getSourceClassName()) 
		+ " " + record.getSourceMethodName()
		+ ": " + record.getMessage()
		+ System.getProperty("line.separator");
  }
}
