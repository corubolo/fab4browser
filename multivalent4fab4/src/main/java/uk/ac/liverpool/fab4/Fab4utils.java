/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/
package uk.ac.liverpool.fab4;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.Multivalent;
import multivalent.Node;
import multivalent.node.LeafText;
import multivalent.node.LeafUnicode;
import multivalent.std.MediaLoader;

import com.ctreber.aclib.image.ico.ICOFile;
import com.pt.io.InputUni;

/**
 * Utility methods are collected here.
 * 
 * @author fabio
 *
 */
public class Fab4utils {
	/** a cache for Favicons */
	private static Map<String, BufferedImage> icoCache = Collections
	.synchronizedMap(new HashMap<String, BufferedImage>(10));
	private static Date icoAge = null;
	static int n = 0;
	static int k = 0;

	static String cacheFileName = ".fab4icons.cache";
	static File fi = new File(new File(System.getProperty("user.home"),
	".Multivalent"), Fab4utils.cacheFileName);

	public static final DateFormat DATEFTIME_SHORT = DateFormat
	.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	public static final DateFormat DATE_SHORT = DateFormat
	.getDateInstance(DateFormat.SHORT);


	public static void printModel (PrintStream ps, Object o){
		Field[] fi = o.getClass().getDeclaredFields();
		for (Field f: fi){
			if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
				ps.print(f.getName() + " = ");
			try {
				ps.println(f.get(o));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ps.println();

	}


	public static Object parseHelper (File f, URI u, Document d, INode parent) throws Exception{
		FileInputStream is = new FileInputStream(f);
		String mime = null;
		String mime2 = null;
		byte[] bufferData = new byte[Math.max(1024, FileGuess.minBytes)];
		int a;
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {}
		while ((a = is.read(bufferData)) != -1) {
			if (mime2 == null && a >= FileGuess.minBytes){
				mime2 = FileGuess.guess(bufferData, u.toString());
				if (mime2==null)
					mime2="";
			}
			if (md != null)
				md.update(bufferData, 0, a);
		}
		is.close();
		is = new FileInputStream(f);
		InputUni iu = InputUni.getInstance(is, u, d.getGlobal().getCache());
		mime = iu.getContentType();
		byte[] digest = md.digest(); // 128 bit or 16 bytes
		MediaLoader.MD5Cache.put(u, digest);
		MediaLoader.FileCache.put(u, f);
		if (mime == null  || mime.equalsIgnoreCase("application/octet-stream") || mime.equals("text/html")) {
			mime = mime2;
			iu.setContentType(mime);
		}

		String genre = d.getGlobal().getGenre(mime, u.toString());
		System.out.println(genre + " " + mime);
		Layer dsl = d.getLayers();
		Layer baseLayer = dsl.getInstance(Layer.BASE);
		MediaAdaptor helper = (MediaAdaptor)Behavior.getInstance("helper",genre,null, baseLayer);
		helper.setInput(iu);
		float zoom = 1;
		try {
			zoom = Float.parseFloat(Multivalent.getInstance().getPreference(genre + "-zoom", "1"));
		} catch (NumberFormatException nfe) {
		}
		helper.setZoom(zoom);
		dsl.getInstance(genre);
		//getDocument().setMediaAdaptor(helper);

		Node root = null;
		try {
			root = (Node)helper.parse(parent);
		} catch (Exception e) {
			new LeafUnicode("ERROR "+e,null, parent);

			e.printStackTrace();
		} finally {
			try { helper.close(); } catch (IOException ioe) {}
		}
		//getDocument().removeAttr(Document.ATTR_LOADING);
		return root;
	}



	public static String stripXml(String xml){
		StringBuilder cont = new StringBuilder();
		int x = xml.indexOf('>');
		int y = xml.indexOf('<', x);
		while (x >= 0 && y >= 0) {
			String t = xml.substring(x + 1, y).trim();
			if (t.length() > 0) {
				cont.append(t);
				cont.append(' ');
			}
			x = xml.indexOf('>', y);
			y = xml.indexOf('<', x);
		}
		return cont.toString();
	}
	/** Utility to compress the contents of a Directory to a ZipOutputStream, if necessary recursing
	 * 
	 * @param zipDir the origin directory
	 * @param zos the ZipOutputStream output
	 * @param recurse should we recurse in subdirs
	 * @throws IOException
	 */


	public static void zipDir(File zipDir, ZipOutputStream zos, boolean recurse)
	throws IOException {
		String[] dirList = zipDir.list();
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		for (String element : dirList) {
			File f = new File(zipDir, element);
			if (f.isDirectory()) {
				if (recurse)
					zipDir(f, zos, recurse);
				// loop again
				continue;
			}
			FileInputStream fis = new FileInputStream(f);
			String en;
			if (recurse)
				en = f.getPath();
			else
				en = f.getName();
			ZipEntry anEntry = new ZipEntry(en);
			zos.putNextEntry(anEntry);
			while ((bytesIn = fis.read(readBuffer)) != -1)
				zos.write(readBuffer, 0, bytesIn);
			fis.close();
		}

	}
	/**
	 *  Utility to zip a set of files to a ZipOutputStream
	 * @param dirList
	 * @param zos
	 * @throws IOException
	 */
	public static void zipFiles(File[] dirList, ZipOutputStream zos)
	throws IOException {
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		for (File f : dirList) {
			if (f.isDirectory())
				continue;
			FileInputStream fis = new FileInputStream(f);
			ZipEntry anEntry = new ZipEntry(f.getName());
			zos.putNextEntry(anEntry);
			while ((bytesIn = fis.read(readBuffer)) != -1)
				zos.write(readBuffer, 0, bytesIn);
			fis.close();
		}
	}

	/**
	 * Reads the cache of Icons from the storage file. this is used for faster access to FavIcons and for bookmarks.
	 */
	public synchronized static void readIcoCache() {
		DataInputStream ois = null;
		try {
			ois = new DataInputStream(new FileInputStream(Fab4utils.fi));
		} catch (IOException e) {
			return;
		}
		String s;
		BufferedImage im;
		try {
			Fab4utils.icoAge = new Date(ois.readLong());
			if (System.currentTimeMillis() - Fab4utils.icoAge.getTime() > 1000 * 60
					* 60 * 24 * 21) { // 3 weeks
				System.out.println("DELETED ICON CACHE");
				// new File(System.getProperty("user.home"),
				// cacheFileName).delete();
				Fab4utils.fi.delete();
				Fab4utils.icoAge = null;
				return;
			}
			int nn = ois.readInt();
			for (int i = 0; i < nn; i++) {
				s = ois.readUTF();
				if (!ois.readBoolean()) {
					byte[] b = new byte[ois.readInt()];
					ois.read(b);
					ByteArrayInputStream bis = new ByteArrayInputStream(b);
					im = ImageIO.read(bis);
				} else
					im = null;
				Fab4utils.icoCache.put(s, im);
				// System.out.println(s + " - " + im);
			}
		} catch (IOException e) {
		} catch (Exception e) {
			e.printStackTrace();
			Fab4utils.icoCache.clear();
		} finally {
			try {
				ois.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Stores the favIcons to the storage.
	 */
	public synchronized static void writeIcoCache() {
		DataOutputStream oos = null;
		try {
			if (!Fab4utils.fi.exists())
				Fab4utils.fi.createNewFile();
			oos = new DataOutputStream(new FileOutputStream(Fab4utils.fi));
			if (Fab4utils.icoAge == null)
				Fab4utils.icoAge = new Date();// now
			oos.writeLong(Fab4utils.icoAge.getTime());
			oos.writeInt(Fab4utils.icoCache.size());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (Map.Entry<String, BufferedImage> im : Fab4utils.icoCache.entrySet())
			try {
				oos.writeUTF(im.getKey());
				oos.writeBoolean(im.getValue() == null);
				if (im.getValue() != null) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ImageIO.write(im.getValue(), "png", bos);
					byte[] ba = bos.toByteArray();
					oos.writeInt(ba.length);
					oos.write(ba);
					bos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try {
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

	}

	/** Stores a screenshot to the bynary array.
	 * format: "jpg", "png",..
	 * 
	 * 
	 * */
	public static byte[] screenShot(String format) {
		try {
			Robot robot = new Robot();
			Rectangle captureSize = new Rectangle(Toolkit.getDefaultToolkit()
					.getScreenSize());
			BufferedImage bufferedImage = robot
			.createScreenCapture(captureSize);
			// robot.mouseMove(100,100);
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, format, bo);
			return bo.toByteArray();
		} catch (AWTException e) {
		} catch (IOException e) {
		}
		return null;

	}
	/** Stores a screenshot of the selected component to the binary array.
	 * format: "jpg", "png",.. */
	public static byte[] screenShot(String format, Component c) {
		try {
			Robot robot = new Robot();
			// ImageWriter encoder =
			// ImageIO.getImageWritersByFormatName("JPEG").next();
			// JPEGImageWriteParam p = new JPEGImageWriteParam(null);
			// p.setOptimizeHuffmanTables(true);
			// p.setCompressionQuality(0.8f);
			// p.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
			// encoder.setOutput(out);
			// encoder.write((IIOMetadata) null, new IIOImage(image,null,null),
			// p);
			Rectangle captureSize = c.getBounds();

			BufferedImage bufferedImage = robot
			.createScreenCapture(captureSize);
			// robot.mouseMove(100,100);
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, format, bo);
			return bo.toByteArray();
		} catch (AWTException e) {
		} catch (IOException e) {
		}
		return null;

	}

	/**
	 * Find the behaviour with the specified name in the specified layer
	 * 
	 * @param name
	 *            the name (ex. System)
	 * @param l
	 *            the layer (ex. root)
	 * @return the Behavior, null if not found
	 */
	public static Behavior getBe(String name, Layer l) {
		Behavior be;
		for (int i = 0; i < l.size(); i++) {
			be = l.getBehavior(i);
			if (be.getName() != null)
				if (be.getName().compareToIgnoreCase(name) == 0)
					return be;
			if (be instanceof Layer)
				be = getBe(name, (Layer) be);
			if (be != null)
				if (be.getName() != null)
					if (be.getName().compareToIgnoreCase(name) == 0)
						return be;
		}
		return null;
	}
	/**
	 * Prints out a list of Behavoiurs in a Layer
	 * @param b
	 */
	public static void listBe(Behavior b) {
		Behavior be;
		Layer l;
		if (b instanceof Layer) {
			Fab4utils.n++;
			System.out.println(Fab4utils.n + "@" + b.getName() + " - " + b.getClass());
			l = (Layer) b;
			for (int i = 0; i < l.size(); i++) {
				be = l.getBehavior(i);
				Fab4utils.k++;
				listBe(be);
			}
			Fab4utils.k = 0;
		} else
			System.out.println(Fab4utils.n + " " + Fab4utils.k + " " + b.getName() + " - "
					+ b.getClass().getCanonicalName());

		return;
	}

	/**
	 * Get all the classes of behaviors (in the tree) that extend the specified
	 * class
	 * 
	 * @param l
	 *            Start layer
	 * @param cla
	 *            the class
	 * @return List of classes (in the tree) of behaviours extending class cla
	 */
	public static List<Class> getBehaviors(Layer l, Class cla) {
		Behavior be;

		List<Class> ve = new Vector<Class>(1);
		for (int i = 0; i < l.size(); i++) {
			be = l.getBehavior(i);
			// System.out.println("--"+be.getName());
			if (be instanceof Layer)
				ve.addAll(getBehaviors((Layer) be, cla));
			if (cla.isInstance(be))
				ve.add(be.getClass());
		}
		return ve;
	}

	/**
	 * Get all the classes of behaviors (in the tree)
	 * 
	 * @param l
	 *            Start layer
	 * @return List of classes (in the tree) of behaviours extending class cla
	 */
	public static List<Class> getBehaviors(Layer l) {
		return getBehaviors(l, Object.class);
	}

	/**
	 * Tries to load a fav icon at the specified URL, caching the results. Works
	 * with .ico file format
	 * 
	 * @param addr
	 *            the FavIcon URL
	 * @return the Icon, null if not found.
	 */

	@SuppressWarnings("unchecked")
	public static Icon getIcon(URL addr) {
		// System.out.println(addr);
		BufferedImage ii = null;
		Image ic;
		int w = 0, h = 0;
		if ((ii = Fab4utils.icoCache.get(addr.toString())) != null) {
			w = ii.getWidth();
			h = ii.getHeight();
			if (w != 16 || h != 16)
				ic = ii.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
			else
				ic = ii;
			// System.out.println("incache");
			return new ImageIcon(ic);
		} else if (Fab4utils.icoCache.containsKey(addr.toString()))
			// System.out.println("incache");
			return null;

		if (addr.getFile().toLowerCase().endsWith(".ico"))
			try {
				ICOFile ico = new ICOFile(addr);
				List images = ico.getImages();
				final List<BufferedImage> l = images;
				// ICOEntry ent = ico.getEntries().get(0);
				int aa = 0;
				for (int a = 0; a < l.size(); a++) {
					int iw = l.get(a).getWidth();
					if (iw == 16)
						aa = a;
					else if (iw > 16 && aa == 0)
						aa = a;
				}
				ii = l.get(aa);
				w = ii.getWidth();
				h = ii.getHeight();
			} catch (Exception e) {
			}
			if (ii == null)
				try {
					ii = ImageIO.read(addr.openStream());
					w = ii.getWidth();
					h = ii.getHeight();
				} catch (Exception e) {
					Fab4utils.icoCache.put(addr.toString(), null);
					return null;
				}
				if (w != 16 || h != 16)
					ic = ii.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
				else
					ic = ii;
				Fab4utils.icoCache.put(addr.toString(), ii);
				return new ImageIcon(ic);
	}

	/**
	 * Tries to load a fav icon based on an URI (same dir and root), caching the
	 * results. Works with .ico file format.
	 * 
	 * @param base
	 *            the document URI
	 * @return the Icon, null if not found.
	 */

	/** Used to get the java version as a float number ex. 1.502 */
	public static float getJavaVersion() {
		try {
			String javav = System.getProperty("java.version");
			StringBuilder sb = new StringBuilder();
			boolean b = true;
			for (int i = 0; i < javav.length(); i++)
				if (javav.charAt(i) == '.' && b) {
					sb.append('.');
					b = false;
				} else if (Character.isDigit(javav.charAt(i)))
					sb.append(javav.charAt(i));
			return Float.parseFloat(sb.toString());
		} catch (Exception e) {
			return 0f;
		}
	}

	public static Icon getFavIcon(URI base) {
		URL add1, add2;
		try {
			Icon ii = null;
			// Relative to the present directory
			add1 = base.resolve("favicon.ico").toURL();
			ii = getIcon(add1);
			if (ii != null)
				return ii;
			// Absolute for the server
			add2 = new URL("http", base.getHost(), "favicon.ico");
			if (!add2.sameFile(add1))
				ii = getIcon(add1);
			return ii;
		} catch (MalformedURLException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}

	}

	/**
	 * Copies content of an inputStream to a file
	 * @param f the output file
	 * @param is the input stram
	 * @param overwrite what if the file exists
	 * @throws IOException
	 */
	public static void copyToFile(InputStream is, File f, boolean overwrite)
	throws IOException {
		if (f.exists() && !overwrite)
			return;
		FileOutputStream os = new FileOutputStream(f);
		byte[] buf = new byte[16 * 1024];
		int i;
		while ((i = is.read(buf)) != -1)
			os.write(buf, 0, i);
		is.close();
		os.close();
	}

	/**
	 * Copies content of an inputStream to temp a file
	 * 
	 * @param is
	 * @return the file just created
	 * @throws IOException
	 */
	public static File copyToTemp(InputStream is) throws IOException {
		File f = File.createTempFile("img", null);
		FileOutputStream os = new FileOutputStream(f);
		byte[] buf = new byte[16 * 1024];
		int i;
		while ((i = is.read(buf)) != -1)
			os.write(buf, 0, i);
		is.close();
		os.close();
		return f;
	}

	/**
	 * Copies content of an inputStream to temp a file
	 * 
	 * @param is
	 * @return the file just created
	 * @throws IOException
	 */
	public static File copyToTemp(String s) throws IOException {
		File f = File.createTempFile("img", null);
		PrintWriter p = new PrintWriter(f);
		p.write(s);
		p.close();
		return f;
	}

	public static final void copyInputStream(InputStream in, OutputStream out)
	throws IOException {
		byte[] buffer = new byte[1024 * 16];
		int len;

		while ((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);

	}
	public static final void copyInputStream(ImageInputStream in, OutputStream out)
	throws IOException {
		byte[] buffer = new byte[1024 * 16];
		int len; 

		while ((len = in. read(buffer)) >= 0)
			out.write(buffer, 0, len);

	}
	public static File copyToTemp(InputStream is, String st, String en)
	throws IOException {
		if (en == null || en.length()<3 || en.indexOf("/")!=-1 ||  en.indexOf("\\")!=-1)
			en = null;
		File f;
		try {
			f= File.createTempFile(st, en);
		} catch (Exception x){
			f = File.createTempFile(st, null);
		}
		FileOutputStream os = new FileOutputStream(f);
		byte[] buf = new byte[16 * 1024];
		int i;
		while ((i = is.read(buf)) != -1)
			os.write(buf, 0, i);
		is.close();
		os.close();
		return f;

	}

	public static File copyToTemp(InputStream is, String st, String en, long length)
	throws IOException {
		if (en == null || en.length()<3 || en.indexOf("/")!=-1 ||  en.indexOf("\\")!=-1)
			en = null;
		File f;
		try {
			f= File.createTempFile(st, en);
		} catch (Exception x){
			f = File.createTempFile(st, null);
		}
		FileOutputStream os = new FileOutputStream(f);
		byte[] buf = new byte[16 * 1024];
		int i, read=0;
		while ((read+= i = is.read(buf)) != -1){
			os.write(buf, 0, i);
			if (read == length)
				break;
		}
		is.close();
		os.close();
		return f;

	}

	/**
	 * Unzips a zip file to the selected folder.
	 * @param zf
	 * @param destinationFolder
	 * @throws ZipException
	 * @throws IOException
	 */
	public static void unzipFile(File zf, File destinationFolder)
	throws ZipException, IOException {
		ZipFile zipFile = new ZipFile(zf);

		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.isDirectory())
				continue;
			File ff = new File(destinationFolder, entry.getName());
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ff));
			copyInputStream(zipFile.getInputStream(entry),
					out);
			zipFile.getInputStream(entry).close();
			out.close();
		}

		zipFile.close();
	}

	public static final String USER_HOME_DIR = System.getProperty("user.home");

	/**
	 * tries to get a sensible icon givena file and a filename.
	 * @param f
	 * @param fn
	 * @return
	 */
	static Icon getIconForExt(File f, String fn) {
		FabIcons.getIcons();
		int pos = fn.lastIndexOf('.');
		if (pos < 0)
			return null;
		String extension = fn.toLowerCase().substring(pos);
		if (extension == null)
			return FabIcons.getIcons().ICOURL;
		if (extension.equals(".pdf"))
			return FabIcons.getIcons().ICOPDF;
		else if (extension.equals(".txt"))
			return FabIcons.getIcons().ICOTXT;
		else if (extension.equals(".odt"))
			return FabIcons.getIcons().ICOODT;
		else if (extension.equals(".jpg") || extension.equals(".gif")
				|| extension.equals(".png") || extension.equals(".svg"))
			return FabIcons.getIcons().ICOIMA;
		else {
			if (f != null)
				return FileSystemView.getFileSystemView().getSystemIcon(f);
			return null;
		}
	}
	/**
	 * Convienence method to scale t a max w and h
	 * @param nMaxWidth max width
	 * @param nMaxHeight max height
	 * @param imgSrc
	 * @return
	 */
	public static BufferedImage scaleToSize(int nMaxWidth, int nMaxHeight,
			BufferedImage imgSrc) {
		int nHeight = imgSrc.getHeight();
		int nWidth = imgSrc.getWidth();
		double scaleX = (double) nMaxWidth / (double) nWidth;
		double scaleY = (double) nMaxHeight / (double) nHeight;
		return Fab4utils.scale(scaleX, scaleY, imgSrc);
	}

	/**
	 * Convenience method for scaling a BufferedImage
	 * @param scalex fraction
	 * @param scaley fraction
	 * @param srcImg
	 * @return
	 */
	public static BufferedImage scale(double scalex, double scaley,
			BufferedImage srcImg) {
		if (scalex == scaley && scaley == 1.0d)
			return srcImg;
		AffineTransformOp op = new AffineTransformOp(AffineTransform
				.getScaleInstance(scalex, scaley), null);
		return op.filter(srcImg, null);
	}


	/** Utility method to extract the text from a document, with white spaces
	 * 
	 * @param doc the Document from which the extraction is done
	 */
	public static String getTextSpaced(Document doc) {
		StringBuilder ret = new StringBuilder();
		walkAndSpace(doc.childAt(0), ret);
		return ret.toString();
	}

	/** Utility method to extract the text from a document, without white spaces
	 * 
	 * @param doc the Document from which the extraction is done
	 */
	public static String getTextNonSpaced(Document doc) {
		StringBuilder ret = new StringBuilder();
		walkNoSpace(doc.childAt(0), ret);

		return ret.toString();
	}


	private static void walkAndSpace(Node node, StringBuilder sb) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode) {
			INode inode = (INode) node;
			for (int i = 0, imax = inode.size(); i < imax; i++)
				walkAndSpace(inode.childAt(i), sb);

		} else if (LeafText.class.isAssignableFrom(node.getClass())) {
			String s = node.getName();
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (Character.isLetterOrDigit(ch))
					sb.append(ch);
			}
			sb.append(" ");
		}
	}
	private static void walkNoSpace(Node node, StringBuilder sb) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode) {
			INode inode = (INode) node;
			for (int i = 0, imax = inode.size(); i < imax; i++)
				walkNoSpace(inode.childAt(i), sb);

		} else if (node instanceof LeafText) {
			int i;
			String tt = node.getName().toLowerCase().trim();
			for (i = 0; i < tt.length(); i++)
				if (Character.isLetterOrDigit(tt.charAt(i)))
					sb.append(tt.charAt(i));
		}
	}



}
