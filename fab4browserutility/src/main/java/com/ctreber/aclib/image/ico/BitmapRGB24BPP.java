package com.ctreber.aclib.image.ico;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.ctreber.aclib.codec.AbstractDecoder;

/**
 * <p>
 * RGB bitmap with 8 bits per color (24 bits per sample).
 * </p>
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public class BitmapRGB24BPP extends AbstractBitmapRGB {
	/**
	 * @param pDescriptor
	 */
	public BitmapRGB24BPP(final BitmapDescriptor pDescriptor) {
		super(pDescriptor);
	}

	@Override
	void read(final AbstractDecoder pDec) throws IOException {
		for (int lRowNo = 0; lRowNo < getHeight(); lRowNo++) {
			final byte[] lRow = pDec.readBytes(getWidth() * 3, null);
			int lRowByte = 0;
			int lOutputPos = (getHeight() - lRowNo - 1) * getWidth();
			for (int lColNo = 0; lColNo < getWidth(); lColNo++)
				// BGR -> RGB, 8 bits per component.
				_samples[lOutputPos++] = lRow[lRowByte++]
				                              + (lRow[lRowByte++] << 8) + (lRow[lRowByte++] << 16);
		}
	}

	/**
	 * @return Create an RGB image.
	 */
	@Override
	public BufferedImage createImageRGB() {
		final BufferedImage lImage = new BufferedImage(getWidth(), getHeight(),
				BufferedImage.TYPE_INT_RGB);
		lImage.setRGB(0, 0, getWidth(), getHeight(), _samples, 0, getWidth());

		return lImage;
	}
}
