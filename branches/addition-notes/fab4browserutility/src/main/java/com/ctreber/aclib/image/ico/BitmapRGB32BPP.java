package com.ctreber.aclib.image.ico;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.ctreber.aclib.codec.AbstractDecoder;

/**
 * <p>
 * ARGB bitmap with 8 bits per color (32 bits per sample).
 * </p>
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public class BitmapRGB32BPP extends AbstractBitmapRGB {
	/**
	 * @param pDescriptor The image descriptor.
	 */
	public BitmapRGB32BPP(final BitmapDescriptor pDescriptor) {
		super(pDescriptor);
	}

	/**
	 * According to Microsoft, the topmost byte simply is not used, but I found
	 * the fourth byte seems to be the alpha channel.
	 * @param pDec
	 *            The decoder.
	 * @throws IOException
	 */
	@Override
	void read(final AbstractDecoder pDec) throws IOException {
		for (int lRowNo = 0; lRowNo < getHeight(); lRowNo++) {
			final byte[] lRow = pDec.readBytes(getWidth() * 4, null);
			int lRowByte = 0;
			int lOutputPos = (getHeight() - lRowNo - 1) * getWidth();
			for (int lColNo = 0; lColNo < getWidth(); lColNo++)
				// BGRA -> ARGB, 8 bits per component.
				_samples[lOutputPos++] = lRow[lRowByte++]
				                              + (lRow[lRowByte++] << 8) + (lRow[lRowByte++] << 16)
				                              + (lRow[lRowByte++] << 24);
		}
	}

	/**
	 * @return Create an ARGB image.
	 */
	@Override
	public BufferedImage createImageRGB() {
		final BufferedImage lImage = new BufferedImage(getWidth(), getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		lImage.setRGB(0, 0, getWidth(), getHeight(), _samples, 0, getWidth());

		return lImage;
	}
}
