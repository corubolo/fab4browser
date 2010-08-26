package phelps.io;

import java.io.FilenameFilter;
import java.io.File;



/**
	Utilities for {@link java.io.FilenameFilter}.

	@version $Revision$ $Date$
*/
public class FilenameFilters {
  /** Accept all filenames. */
  public final FilenameFilter ALL = new FilenameFilter() {
	public boolean accept(File dir, String name) { return true; }
  };

  /** Accept no filenames. */
  public final FilenameFilter NONE = new FilenameFilter() {
	public boolean accept(File dir, String name) { return false; }
  };
}
