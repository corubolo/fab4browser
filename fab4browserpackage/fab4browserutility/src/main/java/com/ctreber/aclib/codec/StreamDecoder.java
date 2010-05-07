package com.ctreber.aclib.codec;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * File decoder based on InputStream (Chris' implementation).
 * </p>
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public class StreamDecoder extends AbstractDecoder {
	private final InputStream _stream;

	/**
	 * Create a BIG_ENDIAN file decoder. See
	 * {@link AbstractDecoder#setEndianess}to change the default behavior.
	 * @param pStream
	 */
	public StreamDecoder(final InputStream pStream) {
		super();
		_stream = pStream;
	}

	@Override
	public void seek(final long pBytes) throws IOException {
		final long lSkip = pBytes - getPos();
		if (lSkip >= 0) {
			final long lBytesSkipped = _stream.skip(lSkip);
			if (lBytesSkipped != lSkip)
				throw new IOException("Tried to skip " + lSkip
						+ ", but skipped " + lBytesSkipped);
			_pos += lSkip;
		} else
			throw new IllegalArgumentException(
					"Can't seek a position already passed (skip " + lSkip + ")");
	}

	@Override
	public byte[] readBytes(final long pBytes, final byte[] pBuffer)
	throws IOException {
		byte[] lBuffer = pBuffer;
		if (lBuffer == null)
			lBuffer = new byte[(int) pBytes];
		else if (lBuffer.length < pBytes)
			throw new IllegalArgumentException(
					"Insufficient space in buffer");

		final int lBytesRead = _stream.read(lBuffer, 0, (int) pBytes);
		if (lBytesRead != pBytes)
			throw new IOException("Tried to read " + pBytes
					+ " bytes, but obtained " + lBytesRead);

		_pos += pBytes;

		return lBuffer;
	}

	/**
	 * @throws IOException
	 * @see com.ctreber.aclib.codec.AbstractDecoder#close()
	 */
	@Override
	public void close() throws IOException {
		_stream.close();
	}
}
