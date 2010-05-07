package multivalent.std.adaptor.pdf;

import java.util.Arrays;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;



/**
	RunLengthDecode filter.  Never used in practice.

	<blockquote>
	length byte followed by 1 to 128 bytes of data.
	If the length byte is in the range 0 to 127, the following length +1 (1 to 128) bytes are copied literally during decompression.
	If length is in the range 129 to 255, the following single byte is to be copied 257 -length (2 to 128) times during decompression.
	A length value of 128 denotes EOD.
	</blockquote>

	@version $Revision: 1.9 $ $Date: 2003/07/30 20:48:22 $
*/
public class DecodeRunLength extends FilterInputStream {
  /** End of data marker. */
  public static final int EOD = 128;

  private int litcnt_=0, dupcnt_=0, dupbyte_=0;
  private boolean eof_ = false;


  public DecodeRunLength(InputStream in) {
	super(in);
  }

  public int read(byte[] b, int off, int len) throws IOException {
	assert b!=null && off>=0 && len>=0 && len+off <= b.length;

	if (eof_) return -1;

	len = Math.min(len, b.length-off);
	for (int i=off,imax=off+len, c; i<imax; i++) {
		if (dupcnt_ > 0) {
			int cnt = Math.min(imax-i, dupcnt_);
			Arrays.fill(b, i, i+cnt, (byte)dupbyte_);
			dupcnt_ -= cnt; i += cnt -1;

/*		} else if (litcnt_ > 0) {
			for (int i=0,imax=Math.min(litcnt_, len); i<imax; i++) b[i]=(byte)read();
*/
		} else if ((c=read())!=-1) b[i]=(byte)c;

		else return i-off;
	}
	return len;
  }

  public int read() throws IOException {
	int b;

	if (litcnt_ > 0) {
		b = in.read();
		litcnt_--;

	} else if (dupcnt_ > 0) {
		b = dupbyte_;
		dupcnt_--;

	} else if (eof_) b=-1;

	else {
		b = in.read();
		if (b==-1) {}	// repeated reads at EOF
		else if (b<EOD) { litcnt_=1+b; b=read(); }
		else if (b>EOD) { dupcnt_=257-b -1; b = dupbyte_ = in.read(); }
		else /*if (b==EOD)*/{ eof_=true; b=-1; }
	}

	return b;
  }


  public boolean markSupported() { return false; }
}
