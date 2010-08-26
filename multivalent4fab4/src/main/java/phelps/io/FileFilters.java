package phelps.io;

import java.io.FileFilter;
import java.io.File;



/**
	Utilities for {@link java.io.FilenameFilter}.

	@version $Revision$ $Date$
*/
public class FileFilters {
  /** Accept all filenames. */
  public final FileFilter ALL = new FileFilter() {
	public boolean accept(File pathname) { return true; }
  };

  /** Accept no filenames. */
  public final FileFilter NONE = new FileFilter() {
	public boolean accept(File pathname) { return false; }
  };
}
