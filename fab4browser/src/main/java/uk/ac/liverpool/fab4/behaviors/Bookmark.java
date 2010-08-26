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

package uk.ac.liverpool.fab4.behaviors;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.SemanticEvent;
import multivalent.gui.VMenu;
import phelps.net.RobustHyperlink;
import phelps.net.URIs;

import com.pt.io.Cache;
import com.pt.io.InputUni;

/**
 * List of URIs to remember. LATER: read/write Netscape-format bookmarks.
 * 
 * @version $Revision$ $Date$
 */
public class Bookmark extends Behavior {
	/**
	 * Add current document to bookmarks.
	 * <p>
	 * <tt>"addBookmark"</tt>: <tt>arg=</tt> {@link java.util.HashMap}
	 * <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root
	 * of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_ADD = "bookmarkAdd";

	/**
	 * Delete current document from bookmarks.
	 * <p>
	 * <tt>"deleteBookmark"</tt>.
	 */
	public static final String MSG_DELETE = "bookmarkDelete";

	private static final String FILENAME = "Bookmarks.txt";

	public static class Entry {
		public String uri = ""; // not null
		public String title = "";
		long create, read, modified; // UNIX-like

		Image icon;

		// Entry parent; -- when nested
		Entry(String uri, String title) {
			this.uri = uri;
			this.title = title;
			long now = System.currentTimeMillis();
			create = read = modified = now;
		}

		Entry(String uri, String title, int create, int read, int modified) {
			this.uri = uri;
			this.title = title;
			this.create = create;
			this.read = read;
			this.modified = modified;
		}

		Entry(String parseme) {
			StringTokenizer st = new StringTokenizer(parseme);
			if (st.hasMoreTokens())
				uri = st.nextToken();
			if (st.hasMoreTokens())
				title = URIs.decode(st.nextToken());
			if (st.hasMoreTokens())
				try {
					create = Long.parseLong(st.nextToken());
				} catch (NumberFormatException cnfe) {
				}
				if (st.hasMoreTokens())
					try {
						read = Long.parseLong(st.nextToken());
					} catch (NumberFormatException rnfe) {
					}
					if (st.hasMoreTokens())
						try {
							modified = Long.parseLong(st.nextToken());
						} catch (NumberFormatException mnfe) {
						}
		}

		@Override
		public String toString() {
			return uri + "   " + URIs.encode(title) + "   "
			+ Long.toString(create) + "   " + Long.toString(read) + " " + Long
			.toString(modified);
		}
	}

	public static Vector<Entry> bookmarks_ = null;

	int find(URI uri) {
		return find(uri.toString());
	}

	int find(String suri) {
		int cnt = 0;
		for (Iterator<Entry> i = Bookmark.bookmarks_.iterator(); i.hasNext(); cnt++) {
			Entry e = i.next();
			String stripuri = RobustHyperlink.stripSignature(e.uri);
			if (stripuri.equals(suri))
				return cnt;
		}
		return -1;
	}

	void read() throws IOException {
		// have default set of bookmarks in systemresource that read in if no
		// user copy, like Preferences
		InputUni iu = getGlobal().getCache().getInputUni(null, Bookmark.FILENAME,
				Cache.GROUP_PERSONAL);
		BufferedReader bookin = null;
		if (iu != null)
			try {
				bookin = new BufferedReader(iu.getReader());
			} catch (FileNotFoundException e) {
			}
			if (bookin == null)
				bookin = new BufferedReader(new InputStreamReader(getClass()
						.getResourceAsStream("/sys/" + Bookmark.FILENAME)));
			for (String line; (line = bookin.readLine()) != null;)
				if (!line.startsWith("#"))
					Bookmark.bookmarks_.add(new Entry(line));

			bookin.close();
	}

	public void write() throws IOException {
		Cache cache = getGlobal().getCache();
		BufferedWriter bookout = new BufferedWriter(new OutputStreamWriter(
				cache.getOutputStream(null, Bookmark.FILENAME, Cache.GROUP_PERSONAL)));
		for (int i = 0, imax = Bookmark.bookmarks_.size(); i < imax; i++) {
			Entry e = Bookmark.bookmarks_.get(i);
			bookout.write(e.toString());
			bookout.newLine();
		}
		bookout.close();
	}

	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg))
			return true;
		else if (VMenu.MSG_CREATE_BOOKMARK == msg) {
			INode menu = (INode) se.getOut();
			// choose which of following depending on if page is already in
			// bookmark? may want to file in a couple of places
			int inx = find(getBrowser().getCurDocument().getURI());
			createUI("button",
					(inx == -1 ? "Add" : "Delete") + " Current Page", "event "
					+ (inx == -1 ? Bookmark.MSG_ADD : Bookmark.MSG_DELETE), menu, null,
					false);

			if (Bookmark.bookmarks_.size() > 0) {
				createUI("separator", "_PAGES", null, menu, "Go", false);
				for (Entry e : Bookmark.bookmarks_)
					createUI("button", e.title, "event " + Document.MSG_OPEN
							+ " " + e.uri, menu, null, false);
			}
		}
		return false;
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		// update last visited field, if new page is bookmarked
		Object arg = se.getArg();

		if (Document.MSG_OPENED == msg && arg instanceof DocInfo) {
			int inx = find(((DocInfo) arg).uri);
			if (inx != -1)
				Bookmark.bookmarks_.get(inx).read = System.currentTimeMillis();
			return false;

		} else if (Bookmark.MSG_ADD == msg || Bookmark.MSG_DELETE == msg) {
			boolean add = Bookmark.MSG_ADD == msg;

			Browser br = getBrowser();
			Document doc = br.getCurDocument();
			// String suri = RobustHyperlink.computeSignature(doc,
			// doc.getURI()); => sign all pages (even if file:/...), so should
			// already be on queue
			String suri = doc.getURI().toString();
			int inx = find(suri);

			boolean change = true;
			// if (add && inx==-1) bookmarks_.add(new Entry(suri,
			// br.getTitle())); // no duplicates (right thing?)
			if (add && inx == -1)
				Bookmark.bookmarks_
				.add(new Entry(suri, doc.getAttr(Document.ATTR_TITLE))); // no
			// duplicates
			// (
			// right
			// thing
			// ?
			// )
			else if (!add && inx != -1)
				Bookmark.bookmarks_.remove(inx);
			else
				change = false;

			if (change)
				try {
					write();
				} catch (IOException ioa) {
				} // if crash before quitting time
		}

		return super.semanticEventAfter(se, msg);
	}

	/**
	 * Read in bookmarks -- LATER maybe put on timer so load a few seconds after
	 * basic startup.
	 */
	@Override
	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);
		if (Bookmark.bookmarks_ == null) { // may want to put bookmarks in general
			// namespace)
			Bookmark.bookmarks_ = new Vector<Entry>(100);
			try {
				read();
			} catch (IOException e) {
			} // have to do as instance so have a cache object
		}
	}
}
