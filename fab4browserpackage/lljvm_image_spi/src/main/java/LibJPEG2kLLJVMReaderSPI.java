


import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;


public class LibJPEG2kLLJVMReaderSPI extends ImageReaderSpi {
	private static final String version = "v8.0";
	private static final String name = "openjpeg2k-1.2-lljvm0.2-compiled-fab4";
	public static final String [] names = {"j2k", "jp2", "jpf", "jpx", "jpm"};
	public static final String [] suffixes = {"j2k", "jp2", "jpf", "jpx", "jpm"};
	public static final String [] mimeTypes = {"image/jp2", "image/jpeg2000",
	    "image/jpeg2000-image", "image/x-jpeg2000-image"};

	public LibJPEG2kLLJVMReaderSPI() {
		super(name,version,names, suffixes,mimeTypes, "LibJpeg2kDecoder", STANDARD_INPUT_TYPE,
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
		int byte3 = iis.read();
		int byte4 = iis.read();
		iis.reset();
		if ((byte1 == 0xFF) && (byte2 == 0x4F)&& (byte3 == 0xfF)&& (byte4 == 0x51)) {
			return true;
		}
		return false;
	}

	@Override
	public ImageReader createReaderInstance(Object extension)
	throws IOException {
		return new LibJpeg2kDecoder(this, extension);
	}

	@Override
	public String getDescription(Locale locale) {
		
		return name + version;
	}

}
