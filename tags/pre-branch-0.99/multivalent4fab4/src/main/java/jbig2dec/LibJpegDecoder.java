package jbig2dec;

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

import org.ibex.nestedvm.Runtime;
import org.ibex.nestedvm.Runtime.FStat;

import com.drew.lang.NullOutputStream;

import uk.ac.liverpool.fab4.Fab4utils;
import uk.co.mmscomputing.imageio.ppm.PPMImageReader;

public class LibJpegDecoder extends ImageReader{

	BufferedImage img = null;

	public LibJpegDecoder(LibJPEGNestedReaderSPI libJPEGNestedReaderSPI, Object extension) {
		super(libJPEGNestedReaderSPI);
	}


	BufferedImage decode () throws IOException{
		ImageIO.setUseCache(false);
		File out;
		out = File.createTempFile("multv", ".pbm");
		out.deleteOnExit();
		//File in = File.createTempFile("multv", ".jpg");
		//in.deleteOnExit();
		//FileOutputStream os = new FileOutputStream(in);
		//Fab4utils.copyInputStream(((ImageInputStream)input), os); 

		//((ImageInputStream)input).close();
		//os.close();

		djpeg me = new djpeg();

		me.closeFD(2);
		me.closeFD(1);
		me.closeFD(0);
		me.addFD(new Runtime.InputOutputStreamFD(new InputStream() {
			ImageInputStream i =  ((ImageInputStream)input);
			@Override
			public int read() throws IOException {
				return i.read();
			}
			@Override
			public void close() throws IOException {
				//i.close();
			}
			@Override
			public int read(byte[] b) throws IOException {
				return i.read(b);
			}
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return i.read(b, off, len);
			}
			@Override
			public synchronized void reset() throws IOException {
				i.reset();
			}
			@Override
			public long skip(long n) throws IOException {
				return i.skipBytes(n);
			}


		}));
		me.addFD(new Runtime.InputOutputStreamFD(new FileOutputStream(out)));
		me.addFD(
				new Runtime.InputOutputStreamFD(new OutputStream() {	
					@Override
					public void write(int b) throws IOException {
					}
				}));
//		String[] args = new String[3];
//		args[0] = "-outfile";
//		args[1] = out.getAbsolutePath();
//		args[2] = in.getAbsolutePath();
//		int status = me.run("djpeg", args);
		int status = me.run();
		System.out.println(status);
		PPMImageReader r = new PPMImageReader();
		BufferedImage img = r.read(ImageIO.createImageInputStream(new File( out.getAbsolutePath())));
		System.out.println("Libjpeg decoded " + img);
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
