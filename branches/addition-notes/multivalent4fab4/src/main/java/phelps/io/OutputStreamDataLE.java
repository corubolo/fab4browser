package phelps.io;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.DataOutputStream;
import java.io.DataOutput;
import java.io.IOException;



/**
	Same as {@link java.io.DataOutputStream}, except data written low byte first (little endian) for <code>short</code>, <code>int</code>, <code>long</code>, and <code>char</code>.

	@see java.io.DataOutputStream for big-endian version

	@version $Revision$ $Date$
*/
public class OutputStreamDataLE extends FilterOutputStream implements DataOutput {	// can't "extends DataOutputStream" because most methods are final, just like java.io.RandomAccessFile
  private DataOutputStream dos_;

  public OutputStreamDataLE(OutputStream out) {
	super(null);
	dos_ = new DataOutputStream(out);
  }

  public synchronized void write(int b) throws IOException { dos_.write(b); }
  public synchronized void write(byte b[], int off, int len) throws IOException { dos_.write(b, off, len); }
  public void flush() throws IOException { dos_.flush(); }
  public final void writeBoolean(boolean v) throws IOException { dos_.writeBoolean(v); }
  public final void writeByte(int v) throws IOException { dos_.writeByte(v); }

  public final void writeShort(int v) throws IOException {
	writeByte(v & 0xff);
	writeByte((v>>>8) & 0xff);
  }

  public final void writeChar(int v) throws IOException {
	writeByte(v & 0xff);
	writeByte((v>>>8) & 0xff);
  }

  public final void writeInt(int v) throws IOException {
	writeByte(v & 0xff);
	writeByte((v>>>8) & 0xff);
	writeByte((v>>>16) & 0xff);
	writeByte((v>>>24) & 0xff);
  }

  public final void writeLong(long v) throws IOException {
	writeByte((int)(v & 0xff));
	writeByte((int)(v>>>8) & 0xff);
	writeByte((int)(v>>>16) & 0xff);
	writeByte((int)(v>>>24) & 0xff);
	writeByte((int)(v>>>32) & 0xff);
	writeByte((int)(v>>>40) & 0xff);
	writeByte((int)(v>>>48) & 0xff);
	writeByte((int)(v>>>56) & 0xff);
  }

  public final void writeFloat(float v) throws IOException {
	writeInt(Float.floatToIntBits(v));	// dos_.writeFloat?
  }

  public final void writeDouble(double v) throws IOException {
	writeLong(Double.doubleToLongBits(v));	// dos_.writeDouble?
  }

  public final void writeBytes(String s) throws IOException { dos_.writeBytes(s); }
  public final void writeChars(String s) throws IOException { dos_.writeChars(s); }
  public final void writeUTF(String str) throws IOException { dos_.writeUTF(str); }
  public final int size() { return dos_.size(); }
}
