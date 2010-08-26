package phelps.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;	// unfortunately outside of MessageDigest hierarchy

import com.pt.io.RandomAccess;



/**
	Utilities for {@link java.io.InputStream}s.

	<ul>
	<li>special-purpose streams: {@link #DEVNULL}, {@link #DEVZERO}, {@link #DEVRANDOM}
	<li>{@link #uncompress(InputStream, String)}, {@link #crc32(InputStream)}
	<li>{@link #readFully(InputStream, byte[])}, {@link #readFully(InputStream, byte[], int, int)}
	<li>copy to {@link #copy(InputStream, OutputStream) OutputStream} or {@link #copy(InputStream, RandomAccess) RandomAccess} <!--, or {@link #copy(InputStream, File) File}-->
	<li>{@link #toByteArray(InputStream)} / {@link #toByteArray(InputStream,long)}
	</ul>

	@version $Revision: 1.5 $ $Date: 2005/01/15 13:07:42 $
*/
public class InputStreams {
  private InputStreams() {}


  /**
	InputStream that always reports end of file (<code>-1</code>) on read.
  */
  public static final InputStream DEVNULL = new InputStream() {
	public int read() { return -1; }
	public long skip(long n) { return 0; }
	public void reset() {}	// InputStream.reset() throws Exception
	public boolean markSupported() { return true; }
  };

  /**
	InputStream that always returns <code>0</code> on read.
  */
  public static final InputStream DEVZERO = new InputStream() {
	public int read() { return 0; }
	public int read(byte[] b, int off, int len) { len = Math.min(len, b.length-off); Arrays.fill(b, off,len, (byte)0); return len; }
	public long skip(long n) { return n; }
	public int available() { return Integer.MAX_VALUE; }
	public void reset() {}
	public boolean markSupported() { return true; }
  };

  /**
	InputStream that returns a random number (0..255) on read.
  */
  public static final InputStream DEVRANDOM = new InputStream() {
	Random rand_ = new Random(System.currentTimeMillis());
	public int read() { return rand_.nextInt() & 0xff; }
	public int available() { return Integer.MAX_VALUE; }
	public boolean markSupported() { return false; }
  };


  /**
	Returns CRC32 checksum of <var>is</var>.
  */
  public static long crc32(InputStream is) throws IOException {
	CRC32 crc32 = new CRC32();
	//MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}

	byte[] buf = new byte[8*1024];
	for (int len; (len = is.read(buf)) >= 0; ) crc32.update(buf, 0, len);
	return crc32.getValue();
  }


  /**
	Reads exactly <var>len</var> bytes from this file into the byte array.
	@exception  EOFException  if this file reaches the end before reading all the bytes.
  */               
  public static void readFully(InputStream is, byte[] b, int off, int len) throws IOException {
	for (int hunk=0; len > 0; off += hunk, len -= hunk) {
		hunk = is.read(b, off, len);	// read([]) can return less than full length
		if (hunk==-1) throw new EOFException();
	}
  }

  public static void readFully(InputStream is, byte[] b) throws IOException {
	readFully(is, b, 0, b.length);
  }

  /** Skips <var>length</var> bytes, unless reach end of stream. */
  public static void skipFully(InputStream is, long length) throws IOException {
	while (length > 0) {
		long hunk = is.skip(length);
		if (hunk!=-1) length -= hunk;
		else break;	// EOF
	}
  }

  /* Inspect input stream to determine type. => generic InputStream can't peek
  public static InputStream uncompress(InputStream is) throws IOException {
  	LZW magic number (1f 9d)
	LZW/PDF  /Filter /LZWDecode
	Flate 0x?8
	BZip2 (B Z h [1-9])
  }*/

  /**
	Wraps <var>is</var> with another stream that uncompresses it.
	Supports suffixes <code>Z</code>, <code>gz</code>, <code>bz2</code>/<code>bzip2</code>, 
	and all HTTP/1.1 <code>Content-Encoding</code>s (<code>gzip</code>, <code>compress</code>, <code>deflate</code>, <code>identity</code>).
	@param type  filename with compression suffix, or compression type (<code>LZW</code>, <code>gzip</code>, <code>deflate</code>, <code>bzip2</code>).
	@see Files#getEncoding(String)
   */
  public static InputStream uncompress(InputStream is, String type) throws IOException {
	if ("identity".equals(type) || type==null) {}	// keep as is
	else if ("compress".equals(type)) is = new com.pt.io.InputStreamLZW(is);
	else if ("gzip".equals(type)) is = new java.util.zip.GZIPInputStream(is);	// maybe check magic number
	else if ("deflate".equals(type)) is = new java.util.zip.InflaterInputStream(is);
	else if ("bzip2".equals(type)) is = new org.apache.tools.bzip2.CBZip2InputStream(is);
	//else if ("z".equals(type)) -- pack format
	else assert true: "unknown type "+type;

	return is;
  }


  /** Convenience method for <code>copy(<var>in</var>, <var>out</var>, <var>false</var>)</code>. */
  public static long copy(InputStream is, OutputStream out) throws IOException { return copy(is, out, false); }

  public static long copy(InputStream is, OutputStream out, boolean fclose) throws IOException { return copy(is, out, fclose, Integer.MAX_VALUE); }

  /**
	Copy contents of <var>is</var> to <var>out</var>.
	If <var>fclose</var> is <code>true</code> then close both streams,
	so a stream-to-stream copy is as simple as <code>copy(new InputStream(), new OutputStream(), true)</code>.
	Neither stream needs to be buffered as block reads and writes are used (which is all that buffered streams do).
	@return number of bytes copied
  */
  public static long copy(InputStream is, OutputStream out, boolean fclose, int length) throws IOException {
	byte[] buf = new byte[Math.min(length, Files.BUFSIZ)];
	long len = 0L;

	try {
		for (int togo = length, hunk; togo > 0 && (hunk = is.read(buf, 0, Math.min(togo,buf.length))) >= 0; togo -= hunk) {
			out.write(buf, 0, hunk);
			len += hunk;
		}

	} catch (IOException ioe) {
		throw new IOException(ioe.getMessage()+" @ "+len);

	} finally {
		if (fclose) {
			out.close();
			is.close();
		}
	}

	return len;
  }

  /* => one-liner
 Copies rest of <var>is</var> to <var>file</var>, which should be writable. 
  public static void copy(InputStream is, File file) throws IOException {
	copy(is, new FileOutputStream(file));
  }*/

  /** Copies rest of <var>is</var> to <var>ra</var>, which should be writable. */
  public static void copy(InputStream is, RandomAccess ra) throws IOException {
	byte[] buf = new byte[8*1024];
	for (int hunk; (hunk = is.read(buf)) != -1; ) ra.write(buf, 0, hunk);
  }

  /** Reads the rest of <var>is</var> and returns contents.  Closes <var>is</var>. */
  public static byte[] toByteArray(InputStream is) throws IOException {
	return toByteArray(is, 10*1024);
  }

  /** Reads the rest of <var>is</var> and returns contents.  Closes <var>is</var>. */
  public static byte[] toByteArray(InputStream is, long estlength) throws IOException {
	ByteArrayOutputStream bout = new ByteArrayOutputStream(estlength>=1? (int)estlength: 10*1024);
	copy(is, bout, true);
	return bout.toByteArray();
  }
}
