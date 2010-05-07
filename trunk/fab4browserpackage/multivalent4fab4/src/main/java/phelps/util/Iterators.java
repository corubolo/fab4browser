package phelps.util;

import java.util.Iterator;



/**
	Extensions to {@link java.util.Iterator}.
	@version $Revision$ $Date$
*/
public class Iterators {
  /** Counts number of elements in passed iterator, exhausting the iterator. */
  public static int count(Iterator i) {
	int cnt = 0;
	for ( ; i.hasNext(); i.next()) cnt++;
	return cnt;
  }

  //public static filter() {} -- easy to do these inine, especially with Java 1.5's enhanced-for
}
