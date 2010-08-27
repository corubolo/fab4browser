package phelps.lang;



/**
	Extensions to {@link java.lang.StringBuffer}, which is <code>final</code>.

	<ul>
	<li>translate/convert/format:
		{@link #getBytes8(StringBuffer)} without character set encoding,
		{@link #valueOf(byte[])}
	</ul>

	@version $Revision: 1.1 $ $Date: 2003/05/10 01:52:54 $
*/
public class StringBuffers {
  private StringBuffers() {}

  /** Converts low bytes from StringBuffer to byte[].  Java's String.getBytes() changes bytes vis-a-vis an encoding. */
  public static byte[] getBytes8(StringBuffer sb) {
	assert sb!=null;
	int len = sb.length();
	byte[] b = new byte[len];
	for (int i=0; i<len; i++) b[i] = (byte)sb.charAt(i);
	return b;
  }

  /** Converts from byte[] to StringBuffer.  Java's String.getBytes() changes bytes vis-a-vis an encoding. */
  public static StringBuffer valueOf(byte[] b) {
	assert b!=null;
	StringBuffer sb = new StringBuffer(b.length);
	for (int i=0,imax=b.length; i<imax; i++) sb.append((char)(b[i] & 0xff));
	return sb;
  }
}
