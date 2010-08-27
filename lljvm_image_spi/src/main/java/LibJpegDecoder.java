


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import uk.co.mmscomputing.imageio.ppm.PPMImageReader;

public class LibJpegDecoder extends ImageReader{

	BufferedImage img = null;

	public LibJpegDecoder(LibJPEGLLJVMReaderSPI libJPEGNestedReaderSPI, Object extension) {
		super(libJPEGNestedReaderSPI);
	}


        public static final void copyInputStream(ImageInputStream in, OutputStream out)
        throws IOException {
                byte[] buffer = new byte[1024 * 16];
                int len;

                while ((len = in.read(buffer)) >= 0)
                        out.write(buffer, 0, len);

        }
	BufferedImage decode () throws IOException{
		ImageIO.setUseCache(false);
		File out;
		out = File.createTempFile("multv", ".pbm");
		out.deleteOnExit();
		File in = File.createTempFile("multv", ".jpg");
		in.deleteOnExit();
		FileOutputStream os = new FileOutputStream(in);
		copyInputStream(((ImageInputStream)input), os); 
//
//		(ImageInputStream)input).close();
		os.close();
		djpeg.main(new String[]{"djpeg",in.getAbsolutePath()});
		
//		String[] args = new String[3];
//		args[0] = "-outfile";
//		args[1] = out.getAbsolutePath();
//		args[2] = in.getAbsolutePath();
//		int status = me.run("djpeg", args);
		//int status = me.run();
		//System.out.println(status);
		PPMImageReader r = new PPMImageReader();
		BufferedImage img = r.read(ImageIO.createImageInputStream(new File( out.getAbsolutePath())));
		System.out.println("Libjpeg-lljvm decoded " + img);
		return img;
	}



	@Override
	public int getHeight(int imageIndex) throws IOException {
		if (img == null)
			img = decode();
		return img.getHeight();
	}
	@Override
	public int getWidth(int imageIndex) throws IOException {
		if (img == null)
			img = decode();
		return img.getWidth();
	}


	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param)
	throws IOException {
		if (img == null)
			img = decode();
		return img;
	}

	@Override
	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {

		return null;
	}


	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
	throws IOException {
		return null;
	}


	@Override
	public int getNumImages(boolean allowSearch) throws IOException {
		return 1;
	}


	@Override
	public IIOMetadata getStreamMetadata() throws IOException {

		return null;
	}




}
