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
package uk.ac.liverpool.fab4; // move to multivalent.std ?

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.zip.CRC32;

import javax.swing.JComboBox;
import javax.swing.JList;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.node.LeafText;

import com.pt.io.Cache;
import com.pt.io.InputUni;

/**
 * This class is now @deprecated and a 1st attempt to distributed annotations, overridden by the Annotation plugin DistributedPersonalAnnos class.
 * It is better to ignore this implementation.
 * 
 * Automatically save and restore annotations from Personal layer. Doesn't need
 * to be page aware because Layer is.
 * 
 * @deprecated
 * 
 * @version $Revision$ $Date$
 */
@Deprecated
public class PersonalAnnos extends Behavior {
	static final boolean DEBUG = false;

	static final String PREFIX = "annos/";

	protected static final String MSG_GO_LOCAL = "goLocal";

	protected static final String MSG_GO_REMOTE = "goremote";

	protected static final String MSG_NOTIFY_CHANGE = "ChangedLocalRemote";

	public static final String MSG_DIST_ANNO_PREF_CREATE = "CreatePrefDialog";

	protected static final String MSG_SEARCH_ANNO = "search anno";

	protected static final String MSG_PUBLISH_ANNOS = "publishAnnos";

	protected static final String MSG_LOAD_REMOTE = "loadRemoteAnno";

	protected static final String MSG_REFRESH_LIST = "refreshAnnoList";

	protected static final String MSG_DELETE = "deleteRemoteAnno";

	protected static final String MSG_HIDE = "hideRemoteAnno";

	protected static final String MSG_ICON = "iconRemoteAnno";

	static String dest = "http://bodoni.lib.liv.ac.uk:8080/Anno/";

	/** Name of annotations hub within cache. */
	static final String FILENAME = "annos.hub"; // => <filename>.anno

	protected static final String MSG_COPY = "CopyNotes";

	public static boolean useRemoteServer = true;

	public SWListModel user = new SWListModel();

	/**
	 * {@link Document#MSG_OPENED} looks for and loads corresponding hub.
	 */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		Browser br = getBrowser();
		// Document doc = varies between di.doc and br.getCurDocument

		if (super.semanticEventBefore(se, msg))
			return true;
		else if (Document.MSG_OPENED == msg) {
			DocInfo di = (DocInfo) se.getArg();
			Document doc = di.doc;
			URI uri = doc.getURI(); // may have been normalized
			InputUni iu = getGlobal().getCache().getInputUni(uri, PersonalAnnos.FILENAME,
					Cache.GROUP_PERSONAL);
			if (iu != null) {
				br.eventq(new SemanticEvent(br, Layer.MSG_LOAD, iu.getURI(),
						null, doc));
				try {
					iu.close();
				} catch (IOException ioe) {
				}
			}
			if (PersonalAnnos.useRemoteServer && uri != null) {
				String txt = getText(doc);
				if (txt.trim().length() == 0)
					txt = uri.toString();

				CRC32 crc = new CRC32();
				crc.update(txt.getBytes());
				long res = crc.getValue();
				getNames(uri, res);

			}

		}
		return false;
	}

	/**
	 * @param uri
	 */
	private void getNames(URI uri, long cr) {
		String url = null;
		String crc = null;
		user.clear();
		try {
			url = URLEncoder.encode(uri.toString(), "UTF-8");
			crc = URLEncoder.encode("" + cr, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		URL co = null;
		String inputLine = null;
		try {
			String str = PersonalAnnos.dest + "load?url=" + url + "&crc=" + crc;
			System.out.println(str);
			co = new URL(str);
			HttpURLConnection connection;
			connection = (HttpURLConnection) co.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			int i = 0;
			while ((inputLine = in.readLine()) != null) {
				i++;
				// System.out.println("+" + URLDecoder.decode(inputLine,
				// "UTF-8"));
				user.add(inputLine);
			}
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * On {@link Document#MSG_CLOSE} save behaviors in layer to disk. In After
	 * in order to give everyone a chance to tidy up.
	 */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Browser br = getBrowser();

		// if (MSG_REFRESH_LIST_ONLOAD == msg) {
		// loadRemote = !loadRemote;
		// }

		if (PersonalAnnos.MSG_REFRESH_LIST == msg) {
			// Document doc = br.getCurDocument();
			Document doc = (Document) br.getRoot().findBFS("content");
			URI uri = doc.getURI();
			String txt = getText(doc);
			if (txt.trim().length() == 0)
				txt = uri.toString();
			CRC32 crc = new CRC32();
			crc.update(txt.getBytes());
			long res = crc.getValue();
			getNames(uri, res);
		} else if (PersonalAnnos.MSG_LOAD_REMOTE == msg) {
			// Document doc = br.getCurDocument();
			Document doc = (Document) br.getRoot().findBFS("content");
			String txt = getText(doc);
			URI uri = doc.getURI();
			if (txt.trim().length() == 0)
				txt = uri.toString();
			CRC32 crc = new CRC32();

			crc.update(txt.getBytes());
			try {
				String urlid = URLEncoder.encode(uri.toString(), "UTF-8");
				String crcid = URLEncoder.encode("" + crc.getValue(), "UTF-8");
				String userid = URLEncoder.encode("" + (String) se.getArg(),
				"UTF-8");
				if (user != null) {
					String str = PersonalAnnos.dest + "load?url=" + urlid + "&user=" + userid
					+ "&crc=" + crcid;
					System.out.println(str);
					br.eventq(new SemanticEvent(br, Layer.MSG_LOAD,
							new URI(str), null, doc));
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

		} else if (PersonalAnnos.MSG_PUBLISH_ANNOS == msg
				&& br.getCurDocument().getAttr(Document.ATTR_LOADING) == null) {
			Document doc = (Document) br.getRoot().findBFS("content");
			URI uri = doc.getURI();
			System.out.println(uri);
			String txt = getText(doc);
			if (txt.trim().length() == 0)
				txt = uri.toString();

			System.out.println(txt);
			CRC32 crc = new CRC32();
			crc.update(txt.getBytes());
			long res = crc.getValue();
			String userid = (String) se.getArg();
			String urlid = uri.toString();
			URL url = null;
			try {
				url = new URL(PersonalAnnos.dest + "publish");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			HttpURLConnection connection;
			Layer personal = doc.getLayer(Layer.PERSONAL);
			ESISNode e = personal.save();
			if (e != null)
				try {
					connection = (HttpURLConnection) url.openConnection();
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					PrintWriter out = new PrintWriter(connection
							.getOutputStream());
					out.println(URLEncoder.encode(urlid, "UTF-8"));
					out.println(URLEncoder.encode(userid, "UTF-8"));
					out.println(URLEncoder.encode("" + res, "UTF-8"));

					String hubtxt = URLEncoder.encode(e.writeXML(), "UTF-8");
					out.println(hubtxt);
					out.flush();
					out.close();
					BufferedReader in = new BufferedReader(
							new InputStreamReader(connection.getInputStream()));
					String inputLine;
					while ((inputLine = in.readLine()) != null)
						System.out.println(URLDecoder
								.decode(inputLine, "UTF-8"));
					in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
		} else if (Document.MSG_CLOSE == msg) {
			Document doc = (Document) se.getArg();
			if (doc == null)
				doc = br.getCurDocument();
			if (doc.getAttr(Document.ATTR_LOADING) == null) { // must finished
				// loading, else
				// could lose
				Cache cache = getGlobal().getCache();
				URI uri = doc.getURI();
				Layer personal = doc.getLayer(Layer.PERSONAL);
				ESISNode e = personal.save();
				if (e != null) { // layer without any behaviors useful? for
					// attrs only?
					if (e.getAttr(Document.ATTR_URI) == null)
						e.putAttr(Document.ATTR_URI, uri.toString());

					try {
						// write file
						OutputStream out = cache.getOutputStream(uri, PersonalAnnos.FILENAME,
								Cache.GROUP_PERSONAL);
						Writer w = new BufferedWriter(new OutputStreamWriter(
								out));
						String hubtxt = e.writeXML();
						w.write(hubtxt);
						w.close();
					} catch (Exception ioe) {
						System.err.println("couldn't write hub: " + ioe);
					}
				} else
					cache.delete(uri, PersonalAnnos.FILENAME, Cache.GROUP_PERSONAL);
			}
		}
		return super.semanticEventAfter(se, msg);
	}

	public String getText(Document doc) {
		StringBuilder ret = new StringBuilder();
		walk(doc.childAt(0), ret);
		return ret.toString();
	}

	public void walk(Node node, StringBuilder sb) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode) {
			INode inode = (INode) node;
			for (int i = 0, imax = inode.size(); i < imax; i++)
				walk(inode.childAt(i), sb);

		} else if (node instanceof LeafText)
			sb.append(node.getName());
	}

	public void addUIElementsToFab4List(final JList l, java.awt.Component[] dele,
			JComboBox cb) {
		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2
						&& e.getButton() == MouseEvent.BUTTON1) {
					int index = l.locationToIndex(e.getPoint());
					getBrowser().eventq(PersonalAnnos.MSG_LOAD_REMOTE,
							user.get(index));
				}
			}
		});
	}

}
