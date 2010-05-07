package multivalent.std.adaptor.pdf;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;

import phelps.lang.Integers;



/**
	ASCII hex filter: two chars decoded to one byte.  Seldom used in practice.

	@version $Revision: 1.7 $ $Date: 2003/08/29 03:53:04 $
*/
public class DecodeASCIIHex extends FilterInputStream {
  private boolean eof_ = false;

  public DecodeASCIIHex(InputStream in) { super(in); }

  public int read(byte[] b, int off, int len) throws IOException {
	assert b!=null && off>=0 && len>=0 && len+off <= b.length;
	if (eof_) return -1;

	len = Math.min(len, b.length-off);
	for (int i=off,imax=off+len, c; i<imax; i++) {
		if ((c=read())!=-1) b[i]=(byte)c;
		else return i-off;
	}
	return len;
  }

  public int read() throws IOException {
	if (eof_) return -1;

	int b, val=0, v;

	while (true) {	// first char
		b = in.read();
		if (b==-1 || b=='>') { eof_=true; return -1; }	// EOF - handles repeated EOF reads too
		else if ((v=Integers.parseInt(b))>=0) { val=v<<4; break; }
		// else skip weird character
	}

	while (true) {	// second char
		b = in.read();
		if (b==-1 || b=='>') { eof_=true; break; }	// EOF - assume 0
		else if ((v=Integers.parseInt(b))>=0) { val+=v; break; }
		// else skip weird character
	}

	return val;
  }
}
