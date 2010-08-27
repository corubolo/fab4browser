package jbig2dec;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;


public class LibJPEGNestedReaderSPI extends ImageReaderSpi {
	private static final String version = "v8.0";
	private static final String name = "Libjpeg-Nestedvm-MIPS-emulated-fab4";
	public static final String [] names = {"JPEG", "jpeg", "JPG", "jpg"};
	public static final String [] suffixes = {"jpg", "jpeg"};
	public static final String [] mimeTypes = {"image/jpeg"};

	public LibJPEGNestedReaderSPI() {
		super(name,version,names, suffixes,mimeTypes, "jbig2dec.LibJpegDecoder", STANDARD_INPUT_TYPE,
				null,false, null, null, null, null, false, null, null, null, null);
	}

	@Override
	public boolean canDecodeInput(Object source) throws IOException {
		if (!(source instanceof ImageInputStream)) {
			return false;
		}
		ImageInputStream iis = (ImageInputStream) source;
		iis.mark();
		int byte1 = iis.read();
		int byte2 = iis.read();
		iis.reset();
		if ((byte1 == 0xFF) && (byte2 == 0xD8)) {
			return true;
		}
		return false;
	}

	@Override
	public ImageReader createReaderInstance(Object extension)
	throws IOException {
		return new LibJpegDecoder(this, extension);
	}

	@Override
	public String getDescription(Locale locale) {
		
		return name + version;
	}

}
