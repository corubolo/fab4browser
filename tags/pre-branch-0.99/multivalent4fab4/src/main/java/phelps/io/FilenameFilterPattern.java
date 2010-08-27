package phelps.io;

import java.io.FilenameFilter;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



/**
	Filter files by testing a regular expression against the <em>file name only</em>, the tail of pathname.
	General {@link FilenameFilter}s are more flexible, but this saves defining a new class for the most common uses.

	@see FileFilterPattern

	@version $Revision: 1.2 $ $Date: 2003/01/19 17:59:34 $
*/
public class FilenameFilterPattern implements FilenameFilter {
  private Matcher m_;

  public FilenameFilterPattern(String regex) { this(Pattern.compile(regex)); }

  public FilenameFilterPattern(Pattern regex) {
	assert regex!=null;
	m_ = regex.matcher("");
  }

  public boolean accept(File dir, String name) {
	return m_.reset(name).find();
  }
}
