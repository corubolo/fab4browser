package phelps.io;

import java.io.Reader;
import java.io.IOException;



/**
	Utilities for {@link java.io.Reader}s.

	@version $Revision: 1.1 $ $Date: 2005/01/02 10:57:09 $
*/
public class Readers {
  private Readers() {}


  /**
	Reads contents of Reader into String and closes Reader.
  */
  public static String toString(Reader r) throws IOException {
	StringBuffer sb = toStringBuffer(r);
	return sb!=null? sb.toString(): null;
  }

  /**
	Reads contents of Reader into StringBuffer and closes Reader.
  */
  public static StringBuffer toStringBuffer(Reader r) throws IOException {
	if (r==null) return null;
	StringBuffer sb = new StringBuffer(2000);
	//BufferedReader r = new BufferedReader(r);
	for (int c; ((c=r.read())!=-1); ) sb.append((char)c);   // undoubtedly a faster way
//System.out.println(sb.toString());
	r.close();
	return sb;
  }
}
