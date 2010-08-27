package com.ctreber.aclib.image.ico;

import java.io.IOException;

import com.ctreber.aclib.codec.AbstractDecoder;

/**
 * <p>
 * Bitmap with 2 color palette (black and white icon). Not tested, but seems to
 * work.
 * </p>
 * <p>
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public class BitmapIndexed1BPP extends AbstractBitmapIndexed {
	/**
	 * Create a 1BPP bitmap.
	 * @param pDescriptor
	 */
	public BitmapIndexed1BPP(final BitmapDescriptor pDescriptor) {
		super(pDescriptor);
	}

	@Override
	void readBitmap(final AbstractDecoder pDec) throws IOException {
		// One byte contains 8 samples.
		final int lBytesPerScanLine = getBytesPerScanLine(getWidth(), 1);
		for (int lRowNo = 0; lRowNo < getHeight(); lRowNo++) {
			final byte[] lBitmapBytes = pDec.readBytes(lBytesPerScanLine, null);
			int lBitmapByteNo = 0;
			int lTestBitMask = 0x80;
			int lPixelNo = (getHeight() - 1 - lRowNo) * getWidth();
			for (int lColNo = 0; lColNo < getWidth(); lColNo++) {
				_pixels[lPixelNo++] = (lBitmapBytes[lBitmapByteNo] & lTestBitMask) / lTestBitMask & 0xFF;

				if (lTestBitMask == 0x01) {
					// When the last bit (bit 0, mask 0x01) has been processed,
					// advance to the next bitmap byte, and set the test bit to
					// bit 7, mask 0x80.
					lTestBitMask = 0x80;
					lBitmapByteNo++;
				} else
					lTestBitMask >>= 1;
			}
		}
	}
}
