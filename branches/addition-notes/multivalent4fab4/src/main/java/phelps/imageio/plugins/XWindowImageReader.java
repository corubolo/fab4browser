package phelps.imageio.plugins;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
//import java.io.BufferedInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.Color;
import java.util.Iterator;



/**
	Image reader for XBM images, which are used in X Windows.

	@author T.A. Phelps
	@version $Revision: 1.1 $ $Date: 2002/10/26 16:50:45 $
*/
public class XWindowImageReader extends ImageReader {
  private static int[] HEX = new int[128];
  static {
	java.util.Arrays.fill(HEX, -1);
	for (int i=0, O='0'; i<10; i++, O++) HEX[O] = i;
	for (int i=10, a='a', A='A'; i<16; i++, a++, A++) HEX[a] = HEX[A] = i;
  }

  private BufferedImage img_ = null;

  private static final IndexColorModel INDEX_BLACK_WHITE = new IndexColorModel(1, 2, new int[] { 0, Color.BLACK.getRGB() }, 0, true, 0/*trans*/, DataBuffer.TYPE_BYTE);

  public XWindowImageReader(ImageReaderSpi originatingProvider, Object extension) {
	super(originatingProvider);
	// no extension
  }

  public Iterator getImageTypes(int imageIndex) throws IOException {
	check(imageIndex);
	// LATER
	return null;
  }

  public int getNumImages(boolean allowSearch) throws IOException {
	check(0);
	return 1;
  }

  public int getHeight(int imageIndex) throws IOException {
	check(imageIndex);
	return img_.getHeight();
  }

  public int getWidth(int imageIndex) throws IOException {
	check(imageIndex);
	return img_.getWidth();
  }

  public IIOMetadata getStreamMetadata() throws IOException {
	check(0);
	return null;
  }

  public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
	check(imageIndex);
	return null;
  }

  public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
	check(imageIndex);
	return img_;
  }

  private void check(int imageIndex) throws IOException {
	if (imageIndex<0 || imageIndex>=1) throw new IndexOutOfBoundsException("only one image in XBM, at index 0");
	if (input==null) throw new IllegalStateException("input not set");

	// XBM images typically small and going to want it anyhow, so just parse it
	if (img_ == null) img_ = read((ImageInputStream)input);
  }


  private static BufferedImage read(ImageInputStream iis) throws IOException {
	int w=0, h=0;
	String line;
	int inx;
	while ((line = iis.readLine()) != null) {
		if (line.length() == 0) continue;
		if (line.startsWith("#define")) {
			try {
				if ((inx = line.indexOf("_width")) > 0) w = Integer.parseInt(line.substring(inx+"_width".length()+1).trim());
				else if ((inx = line.indexOf("_height")) > 0) h = Integer.parseInt(line.substring(inx+"_height".length()+1).trim());
			} catch (NumberFormatException nfe) { System.err.println("parse error on \""+line+"\""); }
		} else if ((inx = line.indexOf("_bits")) >= 0) {
			assert line.indexOf("0x") == -1: line;  // assume data never starts on same line
			assert w>0 && h>0;  // assume width and height set by now

			byte[] data = new byte[w * h];
			for (int i=0,imax=data.length; i<imax; ) {
				int c = iis.read();
				if (Character.isWhitespace((char)c) || c==',') continue;
				assert c=='0': c;
				c = iis.read(); assert c=='x' || c=='X';
				int val  = (HEX[iis.read()]<<4) | HEX[iis.read()];
				for (int j=0; j<8; j++) data[i++] = (byte)((val>>j)&1);  // expand to 1 pixel per byte to mesh with screen depth make fast to paint
			}
			WritableRaster r = WritableRaster.createInterleavedRaster(new DataBufferByte(data, data.length), w, h, w, 1, new int[] {0}, null);
			return new BufferedImage(INDEX_BLACK_WHITE, r, false, new java.util.Hashtable());
		}
	}

	return null;
  }
}
