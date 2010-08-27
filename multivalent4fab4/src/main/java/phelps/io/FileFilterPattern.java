package phelps.io;

import java.io.FileFilter;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



/**
	Filter files by testing a regular expression against the <em>full pathname</em>.
	General {@link FileFilter}s are more flexible, but this saves defining a new class for the most common uses.

	@see FilenameFilterPattern

	@version $Revision: 1.2 $ $Date: 2003/01/19 17:59:14 $
*/
public class FileFilterPattern implements FileFilter {
  private Matcher m_;

	// saves import of regex in caller
	// throws PatternSyntaxException => Unchecked exception, good
  public FileFilterPattern(String regex) { this(Pattern.compile(regex)); }

  public FileFilterPattern(Pattern regex) {
	assert regex!=null;
	m_ = regex.matcher("");
  }

  public boolean accept(File pathname) {
	return m_.reset(pathname.getPath()).find();
  }


  /* useful?
  public static FileFilterPattern forSuffix(String suffix) {
	assert suffix!=null;
	return new FileFilterPattern("(?i)\\." + suffix + "$");
  }*/
}
