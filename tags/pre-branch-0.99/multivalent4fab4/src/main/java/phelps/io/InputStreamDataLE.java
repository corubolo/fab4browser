package phelps.io;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.EOFException;



/**
	NOT IMPLEMENTED.
	DataInputStream, little endian.

	@see java.io.DataInputStream

	@version $Revision$ $Date$
*/
public class InputStreamDataLE extends FilterInputStream {

  public InputStreamDataLE(InputStream in) {
	super(in);
  }

  public final boolean readBoolean() throws IOException {
	int c = read();
	if (c==-1) throw new EOFException();
	return c != 0;
  }

  // ...

}
