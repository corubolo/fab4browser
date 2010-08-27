package phelps.io;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.File;
import java.io.IOException;



/**
	InputStream that reads a portion of another.

	@version $Revision: 1.1 $ $Date: 2003/06/12 17:43:45 $
*/
public class InputStreamSlice extends FilterInputStream {
  private long offset_, limit_, mark_;
  //private long length_;

  /**
	Creates InputStream that reads from <var>is</var> 
	skipping the next <var>offset</var> bytes and
	reading the following <var>length</var> bytes or until end of stream whichever comes first.
  */
  public InputStreamSlice(InputStream is, long offset, long length) throws IOException {
	super(is);
	InputStreams.skipFully(is, offset);
	offset_ = 0; limit_ = length;
	mark_ = offset;
  }

  public int read() throws IOException {
	if (offset_ == limit_) return -1;
	int b = super.read();
	if (b!=-1) offset_++;
	return b;
  }
  public int read(byte[] b) throws IOException { return read(b,0,b.length); }
  public int read(byte[] b, int off, int len) throws IOException {
	if (offset_ == limit_) return -1;
	len = Math.min(len, (int)(limit_ - offset_));
	len = super.read(b, off, len);
	if (len!=-1) offset_ += len;
	return len;
  }

  public long skip(long n) throws IOException {
	n = Math.min(n, limit_ - offset_);
	long cnt = super.skip(n);
	offset_ += cnt;
	return cnt;
  }

  public void mark(int readlimit) { mark_ = offset_; }
  public void reset() throws IOException { super.reset(); offset_ = mark_; }
  //public boolean markSupported() { return super.markSupported(); }
}
