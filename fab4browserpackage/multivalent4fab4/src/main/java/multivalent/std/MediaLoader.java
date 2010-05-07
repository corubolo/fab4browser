/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package multivalent.std;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.SwingUtilities;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.EventListener;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.Multivalent;
import multivalent.SemanticEvent;
import multivalent.node.LeafUnicode;
import uk.ac.liverpool.fab4.FileGuess;

import com.pt.io.Cache;
import com.pt.io.InputStreamTee;
import com.pt.io.InputUni;
import com.pt.io.InputUniString;

/**
 * Load documents in a new thread so GUI and other documents are still live. Presently not in same thread, however.
 * 
 * <!-- deprecated Functionality to be split between {@link multivalent.Browser} and {@link multivalent.MediaAdaptor}. -->
 * 
 * @version $Revision$ $Date$
 */
public class MediaLoader extends Behavior /* implements Runnable */{


	public static final String[] VFSschemes = new String[]{
		"tar","tgz","tbz2","jar","zip","gz","bz2","iso"};

	private static final String CONTENT_UNKNOWN = "content/unknown";

	private static final int MAXCACHESIZE = 9000000;

	DocInfo di_ = null;

	boolean stop_ = false;

	boolean loading_ = false;

	Cursor original = null;

	public static Hashtable<URI, byte[]> MD5Cache = new Hashtable<URI, byte[]>();

	public static Hashtable<URI, File> FileCache = new Hashtable<URI, File>();

	final EventListener owner = new EventListener() {
		public void event(AWTEvent e) {
		}
	};

	private void grabCursor() {
		Cursor t = di_.doc.getBrowser().getCursor();
		if (t != Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)) {
			original = t;
			di_.doc.getBrowser().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			di_.doc.getBrowser().setGrab(owner);
		}
	}

	/**
	 * When run in a thread, parse as usual (while checking asynchronous stop flag), then build with document-specific behaviors, finally call repaint() and send "fullformattedDocument" event.
	 */
	public void load() {
		load(false);
	}


	public void load(final boolean bootstrap) {
		final DocInfo di = di_;
		if (di == null)
			return;
		if (loading_) {
			System.out.println("*** DUOBLE LOAD!");
			return;
		}

		loading_ = true;
		HttpURLConnection.setFollowRedirects(true);
		if (bootstrap) {
			otherUriLoad(bootstrap, di);
			// System.out.println("***"+uri);
		} else {
			grabCursor();
			Thread tt = new Thread() {
				@Override
				public void run() {
					otherUriLoad(bootstrap, di);
				}
			};
			tt.start();
		}
	}

	/**
	 * @param bootstrap
	 * @param di2
	 * @param iu2
	 */

	protected void finalLoadWait(final boolean bootstrap, final DocInfo di2, final InputUni iu2) {
		try {
			if (!SwingUtilities.isEventDispatchThread()) {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						finalLoad(bootstrap, di2, iu2);
					}
				});
			} else {
				finalLoad(bootstrap, di2, iu2);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} finally {
			loading_ = false;
		}
	}


	public void finalLoad(boolean bootstrap, DocInfo di, InputUni iu) {
		if (di == null)
			return;
		MediaLoader ml = this;
		String genre = null;
		final Multivalent v = getGlobal();
		Document doc = di.doc;
		URI uri = di.uri;
		HttpURLConnection.setFollowRedirects(true);
		if (stop_) {
			getLayer().removeBehavior(ml);
			return;
		} // stop after HTTP connect to preserve current

		if (di.genre != null) {
			iu.setContentType(di.genre);
		}

		// 2. Instantiate media adaptor for genre
		try {
			uri = iu.getURI();// di.uri; // may have received a redirect
		} catch (NullPointerException e) {
			uri = di.uri;
		}

		// System.out.println(iu.getContentType());
		boolean isVFS = false;
		if (uri!=null) {
			String scheme = uri.getScheme();
			for (String s: VFSschemes ){
				if (scheme.contains(s)){
					isVFS = true;
				}
			}
		}
		String mime = null;
		if (isVFS){ 
			System.out.println("sss " + uri.toString());
			genre = "UNIXTapeArchive";

		} else {
			/** FILE GUESSING */
			mime = iu.getContentType();
			if (mime == null || mime.equals(CONTENT_UNKNOWN) &&  !(iu instanceof InputUniString) || mime.equalsIgnoreCase("application/octet-stream") || mime.equals("text/html")) {
				//System.out.println("** final guess");
				int bufferData = 0;
				byte[] start2 = new byte[FileGuess.minBytes];
				byte[] start;
				try {
					InputStreamTee ist = iu.getInputStream();
					ist.mark(FileGuess.minBytes + 1);
					bufferData = ist.read(start2);
					ist.reset();
					// ist.cancel();
					iu = InputUni.getInstance(ist, uri, v.getCache());
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (bufferData > 0) {
					start = new byte[bufferData];
					System.arraycopy(start2, 0, start, 0, bufferData);
					if (iu.getURI()!=null)
						mime = FileGuess.guess(start, iu.getURI().getPath());
					else 
						mime = FileGuess.guess(start, null);
					iu.setContentType(mime);
				}
			}

			if (uri==null)
				uri = di.uri;
			if (mime!=null && mime.equals("DirectoryLocal"))
				genre = "DirectoryLocal";
			else if (di.genre == null)
				genre = v.getGenre(mime, uri.getPath());
			else 
				genre = di.genre;
		}
		System.out.println(mime + " " + genre);
		Browser br = di.doc.getBrowser();
		// (3) when good chance that it's going to work (made connection to
		// InputStream, instantiated classes), kill old document and establish
		// new
		loading_ = true;
		doc.clear();
		doc.putAttr(Document.ATTR_GENRE, genre);
		doc.uri = uri;
		doc.putAttr(Document.ATTR_URI, uri.toString()); // make available to
		// scripts --
		// null==reload
		String title = uri.getPath();
		if (title == null)
			title = uri.toString();
		if (title != null) {
			int inx = title.lastIndexOf('/');
			if (inx != -1 && inx < title.length() - 1)
				title = title.substring(inx + 1);
			doc.putAttr(Document.ATTR_TITLE, title); // default title
			//System.out.println("-------"+br);
		}
		br.setCurDocument(doc);

		Layer dsl = doc.getLayers();
		dsl.addBehavior(ml); // guard partial loads
		// Layer baseLayer =
		// (Layer)Behavior.getInstance(Layer.BASE,"Layer",null, dsl);
		Layer baseLayer = dsl.getInstance(Layer.BASE);
		MediaAdaptor base = null;
		// System.out.println("*** Media loader making base for genre "+di.genre);
		try {
			base = (MediaAdaptor) Behavior.getInstance(genre, genre, null, baseLayer);

		} catch (ClassCastException cce) {
			new LeafUnicode(genre + ": genre mapped to non-MediaAdaptor class", null, doc);
			return;
		}

		if (base == null)
			System.out.println("NULL Media Adaptor!");
		doc.setMediaAdaptor(base);

		base.putAttr("uri", uri.toString());

		float zoom = 1;
		try {
			zoom = Float.parseFloat(Multivalent.getInstance().getPreference(genre + "-zoom", "1"));
		} catch (NumberFormatException nfe) {
		}
		base.setZoom(zoom);
		if (!bootstrap)
			dsl.getInstance(doc.getAttr(Document.ATTR_GENRE));
		/**
		 * BUILD Iterate through all behaviors, in high-to-low then low-to-high priority. (Note that can't be tree walk protocol, 'cause ain't no tree yet!
		 */
		// (3) build doc tree
		doc.putAttr(Document.ATTR_LOADING, "loading");
		// System.out.println(uri+" => "+basein);
		try {
			base.setInput(iu);
			// doc.removeAllChildren(); -- already done in clear
			/* if (!bootstrap) */br.event/* no q */(new SemanticEvent(this, Document.MSG_BUILD, di));
			dsl.buildBeforeAfter(doc);
		} catch (Exception e) {
			try {
				base.close();
			} catch (IOException ioe) {
			}
			//new LeafUnicode("COULDN'T RESTORE BASE " + e, null, doc);
			//e.printStackTrace();
		}

		// finish up
		if (stop_) {
			doc.putAttr(Document.ATTR_STOP, "STOP"/* MediaAdaptor.DEFINED */);
			// "Transfer interrupted" -- but in medium-specific way(?)
		} else {
			doc.removeAttr(Document.ATTR_LOADING);
			loading_ = false;
			/* if (!bootstrap) */br.event/* not q, now! */(new SemanticEvent(this, Document.MSG_OPENED, di));
			br.setCurDocument(doc);
			if (bootstrap)
				System.out.println("* bootstrap -- repainting fullscreen");
			if (bootstrap)
				br.repaint();
		}
	}

	/**
	 * @param bootstrap
	 * @param v
	 * @param di
	 */
	void otherUriLoad(final boolean bootstrap, final DocInfo di) {
		InputUni iu = null;
		URI uri = di.uri;
		String scheme = uri.getScheme();
		final Multivalent v = getGlobal();

		boolean isVFS = false;
		for (String s: VFSschemes ){
			if (scheme.contains(s)){
				isVFS = true;
			}
		}
		// LOCAL FILE SYSTEM
		if (scheme.contains("file")) {
			// In case it's an archive file
			if (isVFS){
				iu = InputUni.getInstance(uri, null, v.getCache());
				if (bootstrap)
					finalLoad(bootstrap, di, iu);
				else
					finalLoadWait(bootstrap, di, iu);
			}

			try { 
				URI uri3 = uri;
				if (uri.getFragment()!=null){
					String r = uri.toString();
					r = r.replaceAll("#"+uri.getFragment(), "");
					uri3= new URI(r);
				}
				/// we want to remove the fragment
				// Handle the directory case
				File f = new File(uri3);
				if (f.isDirectory()) {
					System.out.println("Dir" + uri);
					di.genre = "DirectoryLocal";
					iu = InputUni.getInstance(uri, null, v.getCache());

					if (bootstrap)
						finalLoad(bootstrap, di, iu);
					else
						finalLoadWait(bootstrap, di, iu);
					releaseCursor();
					return;
				}
				
				String mime = computeMd5Uni(uri3, v.getCache());
				InputStream is = new FileInputStream(f);
				URL u = uri.toURL();
				URLConnection c = u.openConnection();
				iu = InputUni.getInstance(is, uri, v.getCache());
				if (c.getContentType()==null || c.getContentType().equals(CONTENT_UNKNOWN) )
					iu.setContentType(mime);
				else
					iu.setContentType(c.getContentType());
				FileCache.put(uri3, f);

			} catch (IOException e1) {
				iu = new InputUniString("Error: " + uri + "\n" + e1.getMessage());
				iu.setContentType("text/plain");
				di.genre = "ASCII";
				finalLoad(bootstrap, di, iu);
				releaseCursor();
				return;
			} catch (Exception e1) {
				iu = new InputUniString("Error in: " + uri + "\n" + e1.getMessage());
				iu.setContentType("text/plain");
				di.genre = "ASCII";
				finalLoad(bootstrap, di, iu);
				releaseCursor();
				e1.printStackTrace();
				return;
			} finally {
				if (bootstrap)
					finalLoad(bootstrap, di, iu);
				else
					finalLoadWait(bootstrap, di, iu);
				releaseCursor();
			}

			// ALL THE OTHER CASES HERE:
		} else {
			try {
				URL u = uri.toURL();
				if (di.method.equals("post")) {
					iu = InputUni.getInstance(uri, null, v.getCache());
				} else {
					InputStream is;
					URLConnection c = u.openConnection();
					c.setRequestProperty("user-agent", "Fab4(Multivalent)");
					c.setConnectTimeout(30 * 1000);
					c.setReadTimeout(30 * 1000);
					if (c instanceof HttpURLConnection) {
						HttpURLConnection uu = (HttpURLConnection) c;
						int rc = uu.getResponseCode();
						if (rc >= 400) {
							iu = new InputUniString("Connection error: " + uri + "\n" + uu.getResponseMessage());
						}
						int contentLenght = uu.getContentLength();
						String ct = uu.getContentType();
						if (contentLenght > MAXCACHESIZE || (contentLenght > (MAXCACHESIZE/2) && isStreaming(ct))){
							System.out.println("Streaming");
							iu = InputUni.getInstance(uri, null, v.getCache());
							if (bootstrap)
								finalLoad(bootstrap, di, iu);
							else
								finalLoadWait(bootstrap, di, iu);
							releaseCursor();
						}
					}
					is = c.getInputStream();
					String mm = computeMd5AndCache(uri, is);
					is = new FileInputStream(FileCache.get(uri));
					if (iu == null) {
						iu = InputUni.getInstance(is, uri, v.getCache());
						if (c.getContentType()==null || c.getContentType().equals(CONTENT_UNKNOWN))
							iu.setContentType(mm);
						else
							iu.setContentType(c.getContentType());
					}
				}
				for (Iterator<Map.Entry<String, String>> i = di.headers.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, String> e = i.next();
					iu.putAttr(e.getKey(), e.getValue());
					//System.out.println(e.getKey()+" "+e.getValue());
				}
			} catch (MalformedURLException e1) {
				// THIS IS THE CASE FOR JAR RESOURCES
				iu = InputUni.getInstance(uri, null, v.getCache());

				try {
					String mt = computeMd5Uni(uri, v.getCache());
					if (iu.getContentType()==null || iu.getContentType().equals(CONTENT_UNKNOWN))
						iu.setContentType(mt);
				} catch (IOException e) {
					
					iu = new InputUniString("Connection error: " + uri + "\n\n"+ e1.getMessage() + "\n"+ e1.getClass().getName());
					iu.setContentType("text/plain");
					di.genre = "ASCII";
					e.printStackTrace();
				}
			} catch (IOException e1) {
				//System.out.println("e1");

				iu = new InputUniString("Connection error: " + uri + "\n\n"+ e1.getMessage() + "\n"+ e1.getClass().getName());
				iu.setContentType("text/plain");
				di.genre = "ASCII";
				e1.printStackTrace();
			} catch (Exception e1) {
				
				iu = new InputUniString("Error in: " + uri + "\n" + e1.getClass().getName());
				iu.setContentType("text/plain");
				di.genre = "ASCII";
				e1.printStackTrace();
			} finally {
				
				if (bootstrap)
					finalLoad(bootstrap, di, iu);
				else
					finalLoadWait(bootstrap, di, iu);
				releaseCursor();
			}
		}
	}


	static final String[] stramingTypes = new String[]{"application/ogg",
		"audio/ogg" , 
		"video/ogg" , 
		"audio/mpeg" , 
		"audio/x-mpeg" , 
		"audio/mp3" , 
		"audio/x-mp3" , 
		"audio/mpeg3" , 
		"audio/x-mpeg3" , 
		"audio/mpg" , 
		"audio/x-mpg" , 
	"audio/x-mpegaudio"};
	private boolean isStreaming(String ct) {
		for (String st:stramingTypes)
			if (st.equals(ct))
				return true;
		return false;
	}

	/**
	 * @param uri2
	 * @param is
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static String computeMd5AndCache(URI uri2, InputStream is) throws IOException, FileNotFoundException {
		File f = File.createTempFile("fab4c", "tmp");
		f.deleteOnExit();
		FileOutputStream ff = new FileOutputStream(f);
		byte[] buff = new byte[1024];
		int a;
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
		}
		String mime = null;
		while ((a = is.read(buff)) != -1) {
			ff.write(buff, 0, a);
			if (mime == null && a > FileGuess.minBytes)
				mime = FileGuess.guess(buff, uri2.getPath());
			if (md != null)
				md.update(buff, 0, a);
		}
		ff.close();
		byte[] digest = md.digest(); // 128 bit or 16 bytes
		MD5Cache.put(uri2, digest);
		FileCache.put(uri2, f);
		is.close();
		return mime;


	}

	public static String computeMd5Uni(URI uri2, Cache c) throws IOException {
		InputUni iu = InputUni.getInstance(uri2, null, c);
		InputStream in = iu.getInputStream();
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
		}
		byte[] b = new byte[1024];
		int a;
		String mime = null;
		while ((a = in.read(b)) != -1) {
			if (mime == null && a > FileGuess.minBytes)
				mime = FileGuess.guess(b, uri2.getPath());
			if (md != null)
				md.update(b, 0, a);
		}
		byte[] digest = md.digest(); // 128 bit or 16 bytes
		MD5Cache.put(uri2, digest);
		in.close();
		iu.close();
		return mime;
	}


	/**
	 * 
	 */
	protected void releaseCursor() {
		if (original != null) {
			di_.doc.getBrowser().releaseGrab(owner);
			di_.doc.getBrowser().setCursor(original);
		}
	}

	public void setDocInfo(DocInfo loadme) {
		di_ = loadme;
	}

}
