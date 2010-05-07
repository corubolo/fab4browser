package phelps.imageio.spi;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Locale;

import phelps.imageio.plugins.XWindowImageReader;



public class XWindowImageReaderSpi extends ImageReaderSpi {
  static final String[] names = { "xbm" };  // maybe support xpm some day, but probably not
  static final String[] suffixes = { "xbm" };
  static final String[] mime = { "image/x-bitmap" };

  public XWindowImageReaderSpi() {
	super(
		"Multivalent", "1.0",   // vendorName, version

		names, suffixes, mime,

		"phelps.imageio.plugins.XWindowImageReader", STANDARD_INPUT_TYPE, null,     // readerClassName, Class[] inputTypes, String[] writerSpiNames

// boolean supportsStandardStreamMetadataFormat,
// String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName,
// String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames,
		false, null, null, null, null,

// boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName
//, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames
		false, null, null, null, null
		);
//System.out.println("XWindowImageReaderSpi");
  }


  public ImageReader createReaderInstance(Object extension) {
	return new XWindowImageReader(this, extension);
  }

  public boolean canDecodeInput(Object source) {
	boolean fdecode = false;
	if (source instanceof ImageInputStream) {
		ImageInputStream iis = (ImageInputStream)source;
		iis.mark();
		try {
			String line = iis.readLine();
			fdecode = line!=null && line.startsWith("#define") && line.indexOf('_')>="#define x".length();
			iis.reset();
		} catch (java.io.IOException ignore) {}
	}
	return fdecode;
  }

  public String getDescription(Locale locale) {
	return "XBM only.  No metadata.";
  }
}
