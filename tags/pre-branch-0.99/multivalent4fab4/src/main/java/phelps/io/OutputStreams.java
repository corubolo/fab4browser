package phelps.io;

import java.io.OutputStream;
import java.io.IOException;



/**
	Utility methods for {@link java.io.OutputStream}s.

	<ul>
	<li>{@link #DEVNULL}
	</ul>

	@version $Revision: 1.1 $ $Date: 2003/08/17 14:24:28 $
*/
public class OutputStreams {
  public static final OutputStream DEVNULL = new OutputStream() {
	public void close() {}
	public void flush() {}
	public void write(byte[] b) {}
	public void write(byte[] b, int off, int len) {}
	public void write(int b) {}
  };

  private OutputStreams() {}


  /**
	 Wraps <var>os</var> with another stream that compresses it.
	 @param fileOrType  filename with compression suffix, or compression type (<code>LZW</code>, <code>gzip</code>, <code>deflate</code>, <code>bzip2</code>).
   */
  public static OutputStream compress(OutputStream os, String fileOrType) throws IOException {
	int inx = fileOrType.lastIndexOf('.');
	String sfx = inx!=-1? fileOrType.substring(inx+1): fileOrType;
	String type = fileOrType.toLowerCase();

	/*if ("Z".equals(sfx) /*|| "z".equals(sfx)--pack format* /  ||  "lzw".equals(type)) os = new com.pt.io.OutputStreamLZW(os);
	else*/ if ("gz".equals(sfx) || "gzip".equals(type)) {		
		os = new java.util.zip.GZIPOutputStream(os);
		//try { } catch (IOException ioe) {}	// maybe check magic number
	} else if ("deflate".equals(type) || "flate".equals(type)) {
		os = new java.util.zip.DeflaterOutputStream(os);
	} else if ("bz2".equals(sfx) || "bzip2".equals(sfx)  ||  "bzip2".equals(type)) {
		os = new org.apache.tools.bzip2.CBZip2OutputStream(os);
	} //else keep as is

	return os;
  }
}
