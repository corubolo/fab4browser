package phelps.io;

import java.io.IOException;
import java.net.URI;

import com.pt.io.RandomAccess;



/**
	Wrapper around a {@link RandomAccess} that provides read/write of multibyte quantities, big endian.

	@see RandomAccessDataLE

	@version $Revision$ $Date$
*/
public class RandomAccessDataBE implements RandomAccess /*extends RandomAccessFilter*/ {
  private RandomAccess ra_;

  public RandomAccessDataBE(RandomAccess ra) {
	ra_ = ra;
  }

  // pass through
  public void seek(long pos) throws IOException { ra_.seek(pos); } // and/or mark/reset
  public long getFilePointer() throws IOException { return ra_.getFilePointer(); }
  public long length() throws IOException { return ra_.length(); }
  public int skipBytes(int n) throws IOException { return ra_.skipBytes(n); }
  public int read() throws IOException { return ra_.read(); }
  public int read(byte[] b, int off, int len) throws IOException { return ra_.read(b, off, len); }
  public int read(byte[] b) throws IOException { return ra_.read(b); }
  public void readFully(byte[] b) throws IOException { ra_.readFully(b); }
  public void readFully(byte[] b, int off, int len) throws IOException { ra_.readFully(b, off, len); }
  public void write(byte[] b) throws IOException { ra_.write(b); }
  public void write(byte[] b, int off, int len) throws IOException { ra_.write(b, off, len); }
  public void write(int b) throws IOException { ra_.write(b); }
  public void writeString8(String s) throws IOException { ra_.writeString8(s); }
  public void writeString16(String s) throws IOException { ra_.writeString16(s); }
  public void close() throws IOException { ra_.close(); }


	/*
// read of 1,2,3,4 byte quantities; signed and unsigned
  public int read8() throws IOException { return ra_.read(); }
  public int read8U() throws IOException { int val = ra_.read(); return val!=-1? val&0xff: -1; }
  public int read
  */

  public int readInt() throws IOException { return (ra_.read()<<24) | (ra_.read()<<16) | (ra_.read()<<8) | (ra_.read()); }
  public int readShort() throws IOException { return (short)((ra_.read()<<8) | (ra_.read())); }
  public int readUnsignedShort() throws IOException { return (ra_.read()<<8) | (ra_.read()); }
  // more to come

  public void writeInt(int v) throws IOException { ra_.write(v>>24); ra_.write(v>>16); ra_.write(v>>8); ra_.write(v); }
  public void writeShort(int v) throws IOException { ra_.write(v>>8); ra_.write(v); }
  public void slice(long start, long length) throws IOException { ra_.slice(start, length); }
}
