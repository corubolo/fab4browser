package multivalent.std.ui;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Map;
import java.io.*;
import java.net.URI;

import phelps.net.URIs;
import phelps.net.RobustHyperlink;

import com.pt.io.InputUni;
import com.pt.io.Cache;

import multivalent.*;
import multivalent.gui.VMenu;



/**
	List of URIs to remember.
	LATER: read/write Netscape-format bookmarks.

	@version $Revision: 1.3 $ $Date: 2005/01/01 13:14:27 $
*/
public class Bookmark extends Behavior {
  /**
	Add current document to bookmarks.
	<p><tt>"addBookmark"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
  */
  public static final String MSG_ADD = "bookmarkAdd";

  /**
	Delete current document from bookmarks.
	<p><tt>"deleteBookmark"</tt>.
  */
  public static final String MSG_DELETE = "bookmarkDelete";


  private static final String FILENAME = "Bookmarks.txt";

  static class Entry {
	String uri="", title="";	// not null
	long create, read, modified; // UNIX-like

	//Entry parent; -- when nested
	Entry(String uri, String title) {
		this.uri=uri; this.title=title;
		long now = System.currentTimeMillis();
		create = read = modified = now;
	}
	Entry(String uri, String title, int create, int read, int modified) {
		this.uri=uri; this.title=title; this.create=create; this.read=read; this.modified=modified;
	}
	Entry(String parseme) {
		StringTokenizer st = new StringTokenizer(parseme);
		if (st.hasMoreTokens()) uri = st.nextToken();
		if (st.hasMoreTokens()) title = URIs.decode(st.nextToken());
		if (st.hasMoreTokens()) try { create = Long.parseLong(st.nextToken()); } catch (NumberFormatException cnfe) {}
		if (st.hasMoreTokens()) try { read = Long.parseLong(st.nextToken()); } catch (NumberFormatException rnfe) {}
		if (st.hasMoreTokens()) try { modified = Long.parseLong(st.nextToken()); } catch (NumberFormatException mnfe) {}
	}
	public String toString() {
		return (uri.toString()+"   "+URIs.encode(title)+"   "+Long.toString(create)+"   "+Long.toString(read)+"	"+Long.toString(modified));
	}
  }

  static List<Entry> bookmarks_ = null;



  int find(URI uri) { return find(uri.toString()); }
  int find(String suri) {
	int cnt=0;
	for (Iterator<Entry> i=bookmarks_.iterator(); i.hasNext(); cnt++) {
		Entry e = i.next();
		String stripuri = RobustHyperlink.stripSignature(e.uri);
		if (stripuri.equals(suri)) return cnt;
	}
	return -1;
  }

  private void read() throws IOException {
	// have default set of bookmarks in systemresource that read in if no user copy, like Preferences
	InputUni iu = getGlobal().getCache().getInputUni(null, FILENAME, Cache.GROUP_PERSONAL);
	BufferedReader bookin=null;
	if (iu != null) try { bookin = new BufferedReader(iu.getReader()); } catch (FileNotFoundException e) {}
	if (bookin==null) bookin = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/sys/"+FILENAME)));	
	for (String line; (line=bookin.readLine())!=null; ) if (!line.startsWith("#")) bookmarks_.add(new Entry(line));
	bookin.close();
  }

  private void write() throws IOException {
	Cache cache = getGlobal().getCache();
	BufferedWriter bookout = new BufferedWriter(new OutputStreamWriter(cache.getOutputStream(null, FILENAME, Cache.GROUP_PERSONAL)));
	for (int i=0,imax=bookmarks_.size(); i<imax; i++) {
		Entry e = bookmarks_.get(i);
		bookout.write(e.toString()); bookout.newLine();
	}
	bookout.close();
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_BOOKMARK==msg) {
		INode menu = (INode)se.getOut();
		// choose which of following depending on if page is already in bookmark?  may want to file in a couple of places
		int inx = find(getBrowser().getCurDocument().getURI());
		createUI("button", (inx==-1?"Add":"Delete")+" Current Page", "event "+(inx==-1? MSG_ADD : MSG_DELETE), menu, null, false);

		if (bookmarks_.size()>0) {
			createUI("separator", "_PAGES", null, menu, "Go", false);
			for (Iterator<Entry> i=bookmarks_.iterator(); i.hasNext(); ) {
				Entry e = i.next();
				createUI("button", e.title, "event "+Document.MSG_OPEN+" "+e.uri, menu, null, false);
			}
		}
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// update last visited field, if new page is bookmarked
	Object arg = se.getArg();

	if (Document.MSG_OPENED==msg && arg instanceof DocInfo) {
		int inx = find(((DocInfo)arg).uri);
		if (inx!=-1) bookmarks_.get(inx).read = System.currentTimeMillis();
		return false;

	} else if (MSG_ADD==msg || MSG_DELETE==msg) {
		boolean add = (MSG_ADD==msg);

		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		//String suri = RobustHyperlink.computeSignature(doc, doc.getURI()); => sign all pages (even if file:/...), so should already be on queue
		String suri = doc.getURI().toString();
		int inx = find(suri);

		boolean change=true;
		//if (add && inx==-1) bookmarks_.add(new Entry(suri, br.getTitle()));	// no duplicates (right thing?)
		if (add && inx==-1) bookmarks_.add(new Entry(suri, doc.getAttr(Document.ATTR_TITLE)));	// no duplicates (right thing?)
		else if (!add && inx!=-1) bookmarks_.remove(inx);
		else change=false;

		if (change) try { write(); } catch (IOException ioa) {}   // if crash before quitting time
	}

	return super.semanticEventAfter(se,msg);
  }

  /** Read in bookmarks -- LATER maybe put on timer so load a few seconds after basic startup. */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	if (bookmarks_==null) {	// may want to put bookmarks in general namespace)
		bookmarks_ = new ArrayList<Entry>(100);
		try { read(); } catch (IOException e) {}	// have to do as instance so have a cache object
	}
  }
}
