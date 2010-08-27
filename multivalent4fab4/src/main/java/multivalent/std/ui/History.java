package multivalent.std.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import phelps.net.URIs;
import phelps.Utility;
import phelps.util.Dates;

import com.pt.io.InputUni;
import com.pt.io.InputUniString;
import com.pt.io.Cache;
import com.pt.net.MIME;

import multivalent.*;
import multivalent.node.LeafUnicode;



/**
	Saves last 1000 pages seen.
	Upon startup, reads list of from disk and salts cache's pages-seen list.
	On shutdown, saves list, truncating if necessary.
	During operation, records URL, title, time last seen.
	On "history:" URL, displays list, grouped by host URL, sorted most recent to least recent.
	Example of how to fault in outline sections on demand.

	@see multivalent.std.span.OutlineSpan
	@see multivalent.std.adaptor.ManualPage

	@version $Revision: 1.8 $ $Date: 2005/01/01 13:28:44 $
*/
public class History extends Behavior {
  public static final String FILENAME = "History.txt";


  private static final int MAX=500, PERIOD=100;
  private static final String FAULTID = "[FAULT]";

  private static List<DocRec> lrulist_ = null;
  //private static boolean dirty = true;  // need to regen HTML display?
  private static Map<String,DocRec> hash_ = null;    // probably fast enough to search lrulist_
  private static int periodcnt_=0;
  private List[] reclist_=null;	// elements sorted List's of DocRec


  private static class DocRec {
	public String uri;  // more efficient to keep as String rather than URI
	public String title=null;
	public long date=-1;

	DocRec(String u) { uri=u; }
	DocRec(String u, String t, long d) { uri=u; title=t; date=d; }
	public String toString() { return title+"  /  "+uri+" / "+date; }
  }


  /** Write out history at "EXIT", "exitBrowserInstance', and every so many {@link Document#MSG_CLOSE}s. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_CLOSE==msg) {
		// periodically write history in case of crash
		if (periodcnt_++ > PERIOD) { writeHistory(); periodcnt_=0; }

	} else if (Multivalent.MSG_EXIT==msg || Browser.MSG_CLOSE==msg) {
		writeHistory();
	}
	return false;
  }

  /**
	On "openDocument history:", dynamically generate history page.
	On "openedDocument <i>DocInfo</i>, add/update record for that URL.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (arg==null) {
	} else if (Document.MSG_OPENED==msg && arg instanceof DocInfo) {
		readHistory();

		// record all documents opened
		DocInfo di = (DocInfo)arg;
		Document doc = di.doc;
		URI uri = di.uri;
		if (uri!=null && !"history".equals(uri.getScheme())) {
			String suri = di.uri.toString();
			DocRec rec = (DocRec)hash_.get(suri);
			if (rec!=null) lrulist_.remove(rec); else { rec=new DocRec(suri); hash_.put(suri, rec); }
			rec.title = doc.getAttr(Document.ATTR_TITLE);
			rec.date = System.currentTimeMillis();
			lrulist_.add(0, rec);
//System.out.println("adding "+rec);
		}

	} else if (Document.MSG_OPEN==msg && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;
		URI uri = di.uri;
//System.out.println("generateDocument on "+di.uri);
		if (uri!=null && "history".equals(uri.getScheme())) {
			di.doc.removeAllChildren();
			displayHistory(di/*.doc*/, uri);   // want br.getDocRoot() but it's not a Document
			di.genre = "History";
			di.returncode = 200;
		}

/*	} else if ("outlineOpen"==msg && arg instanceof Span) {
		Span span = (Span)arg;
		int id=Integers.parseInt(span.getAttr("id"), -1);
System.out.println("fault in id #"+id);
		if (id!=-1) {
			Node n = span.getStart().leaf;
			INode p=n.getParentNode();
//System.out.println(n.getName()+"	 "+p.getName()+"/"+p.childNum()+"	"+pp.getParentNode().getName()+"/"+pp.size());
			INode repl = (INode)p.getParentNode().childAt(1+p.childNum());
			Leaf l = repl.getFirstLeaf();
//System.out.println(n.getName()+" => "+l.getName());
			if (FAULTID.equals(l.getName())) {
//if (id+1<Index_.size()) System.out.println(" to "+Index_.get(id+1));
				fault(id, span, repl);
			}
		}*/
/*
	} else if (MSG_FORWARD==msg) {


	} else if (MSG_BACKWARD==msg) {


	} else if (MSG_FULL==msg) {
*/
	}

	return super.semanticEventAfter(se,msg);
  }

  /**
	Collect list of pages, display grouped by host, host with most recently seen pages first.
	Could have others formats selected by attribute.
	Generates HTML page and passes off to HTML media adaptor translate into tree nodes.
  */
  void displayHistory(DocInfo di/*Document doc*/, URI uri) {
	readHistory();

	Browser br = getBrowser();
	Document doc = di.doc;
//System.out.println("GENERATE HISTORY NOW on "+doc);

	// make list
	//long now = System.currentTimeMillis();
	int lrulen = lrulist_.size();
	String[] hosts = new String[lrulen];
	List[] reclist = new List[lrulen];
	int hostlen=0;
	for (Iterator<DocRec> i=lrulist_.iterator(); i.hasNext(); ) {
		DocRec rec = i.next();
		String suri=rec.uri, host=null;
		if (suri.startsWith("http://")) {
			int inx1=7, inx2=suri.indexOf('/',inx1+1);
			if (suri.startsWith("http://www") || suri.startsWith("http://http")) inx1=Math.max(inx1,suri.indexOf('.',inx1+1)+1);
			if (inx1!=-1) host = suri.substring(inx1,inx2);
		} else {	// take protocol as host (e.g., "file")
			int inx = suri.indexOf('/');
			if (inx!=-1) host=suri.substring(0,inx);
		}
//System.out.println(suri+" => "+host);
		if (host!=null) {
			// find existing host -- if this becomes a drag, move to hash
			List<DocRec> hostlist = null;
			//int hit = Arrays.asList(hosts).indexOf(host);
			for (int j=0; j<hostlen; j++) if (host.equals(hosts[j])) { hostlist = (List<DocRec>)reclist[j]; break; }
			//if (hit!=-1) hostlist=reclist[hit]; else
			if (hostlist==null) { hosts[hostlen]=host; reclist[hostlen] = hostlist = new ArrayList<DocRec>(10); hostlen++; }
			hostlist.add(rec);
		}
	}
	reclist_ = reclist;	// save for faults

	doc.removeAllChildren();
	Layer baseLayer = doc.getLayer(Layer.BASE);


	//MediaAdaptor.parseHelper(toHTML(hosts, hostlen, reclist), "HTML", baseLayer, parent);

	MediaAdaptor html = (MediaAdaptor)Behavior.getInstance("helper","HTML",null, baseLayer);
	try {
		html.setInput(new InputUniString(toHTML(hosts, hostlen, reclist), MIME.TYPE_TEXT_HTML, uri, null));
		html.parse(doc);
	} catch (Exception e) { new LeafUnicode("ERROR "+e,null, doc);
	} finally { try { html.close(); } catch (IOException ioe) {} }

	br.event/*no q*/(new SemanticEvent(this, Document.MSG_BUILD, di));
//	baseLayer.removeBehavior(html); // don't want new HTML building up for each faulted in piece, and no specialness that needs HTML hanging around to process
  }


  String toHTML(String[] hosts, int hostlen, List[] reclist) {
	long now = System.currentTimeMillis();

	// translate to HTML
	StringBuffer sb = new StringBuffer(10000);
	sb.append("<html>\n<head>");
	sb.append("<title>History</title>");
	//sb.append("<style
	sb.append("</head>\n");
	sb.append("<body>\n");

	// alphabetical by host
	sb.append("\n<h2>Alphabetical Index</h2>\n");
	String[] ahost = new String[hostlen];
	System.arraycopy(hosts,0, ahost,0, hostlen);    // not clone()
	Arrays.sort(ahost);
	char lastlet=ahost[0].charAt(0);
	for (int i=0; i<hostlen; i++) {
		String host = ahost[i];
		char let = host.charAt(0);
		if (let!=lastlet) { sb.append("<br />"); lastlet=let; } else if (i>0) sb.append(" / ");
		sb.append("<a href='#").append(host).append("'>").append(host).append("</a>");
	}

	// chronological
	boolean big=false;
	sb.append("\n<h2>Chronological</h2>\n");	// history gets long so fault in most of these
	for (int i=0; i<hostlen; i++) {
		// open hosts visited today, up to some maximum
		if (big) {
			sb.append("<span behavior='OutlineSpan' id=").append(i).append(i<0? " open":"").append('>');
			sb.append("<h4><a name='").append(hosts[i]).append("'>").append(hosts[i]).append("</a>	").append(reclist[i].size()).append("</h4>");
			sb.append("<p>").append(FAULTID).append("</p></span>");
		} else {    // non-incremental version
			sb.append("<p>").append("<b><a name='").append(hosts[i]).append("'>").append(hosts[i]).append("</a></b>");
			for (Iterator<DocRec> j=((List<DocRec>)reclist[i]).iterator(); j.hasNext(); ) {
				DocRec rec = j.next();
				String suri = rec.uri;
				//sb.append("<br>\n").append("<a href='").append(suri).append("'>").append(rec.title).append("</a>");
				sb.append("<br>\n").append(rec.title);
				int lastslash = suri.lastIndexOf('/'); if (lastslash+1 == suri.length()) lastslash=suri.lastIndexOf('/',lastslash-1);
				int qinx=suri.indexOf('?', lastslash+1); if (qinx==-1) qinx=suri.length();
				sb.append("   (<a href='").append(suri).append("'>").append(suri.substring(lastslash+1, qinx)).append("</a>)  ");
				sb.append(Dates.relative(rec.date, now));
			}
		}
	}


	sb.append("</body>\n</html>\n");
//System.out.println("sb.length() = "+sb.substring(0));

	return sb.toString();
  }



  /**
	Read history tuples from USER directory: URL, encoded title, date last read.
	Also salts cache's pages-seen list.
  */
  public void readHistory() {
	if (lrulist_!=null) return;
	lrulist_ = new ArrayList<DocRec>(MAX);
	hash_ = new HashMap<String,DocRec>(MAX*2);

	Cache cache = getGlobal().getCache();
	InputUni iu = cache.getInputUni(null, FILENAME, Cache.GROUP_PERSONAL);
	if (iu != null) try {	// don't complain if doesn't exists, which it won't the first time
		// read from disk
		BufferedReader r = new BufferedReader(iu.getReader());
		String line;
		while ((line=r.readLine())!=null) {
			int x1=line.indexOf(' '), x2=line.indexOf(' ',x1+1);
			String suri=line.substring(0,x1), title=line.substring(x1+1,x2), sdate=line.substring(x2+1);
			title = URIs.decode(title);
			long date=-1; try { date=Long.parseLong(sdate); } catch (NumberFormatException male) {}
			DocRec rec = new DocRec(suri, title, date);

			hash_.put(suri, rec); lrulist_.add(rec);
			try { cache.setSeen(new URI(suri)); } catch (URISyntaxException use) {}
		}
		r.close();
	} catch (IOException ioe) {
		Utility.warning("can't read history list");
	}
  }


  /** Write tuples to disk. */
  public void writeHistory() {
	if (lrulist_==null) return;
System.out.println("saving history");

	Cache cache = getGlobal().getCache();
	OutputStream out = cache.getOutputStream(null, FILENAME, Cache.GROUP_PERSONAL);
	if (out!=null) try {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
		int cnt=0;
		for (Iterator<DocRec> i=lrulist_.iterator(); i.hasNext() && cnt<MAX; cnt++) {
			DocRec rec = i.next();
			w.write(rec.uri+" "+URIs.encode(rec.title)+" "+rec.date);
			w.newLine();
		}
		w.close();
	} catch (IOException ioe) {
		Utility.warning("can't write history list");
	}
  }


  public Node fault(int id, Span span, INode replace) {
	List<DocRec> reclist = (List<DocRec>)reclist_[id];
	long now = System.currentTimeMillis();
	StringBuffer sb = new StringBuffer(reclist.size()*100);
	sb.append("<p>");
	for (Iterator<DocRec> j=reclist.iterator(); j.hasNext(); ) {
		DocRec rec = j.next();
//System.out.println(rec);
		String suri = rec.uri;
		//sb.append("<br>\n").append("<a href='").append(suri).append("'>").append(rec.title).append("</a>");
		sb.append("\n<br>").append(rec.title);
		int lastslash = suri.lastIndexOf('/'); if (lastslash+1 == suri.length()) lastslash=suri.lastIndexOf('/',lastslash-1);
		int qinx=suri.indexOf('?', lastslash+1); if (qinx==-1) qinx=suri.length();
		sb.append("   (<a href='").append(suri).append("'>").append(suri.substring(lastslash+1, qinx)).append("</a>)  ");
		sb.append(Dates.relative(rec.date, now));
		sb.append("\n");
	}
//System.out.println(sb);

	MediaAdaptor html = (MediaAdaptor)Behavior.getInstance("helper","HTML",null, getLayer());
	try {
		// manage span bookkeeping
		Node n0=span.getStart().leaf;
		span.moveq(null);

		//replacep.removeAllChildren();
		Document doc = new Document("SPLASH",null, replace);   // don't expose current style sheet to HTML
		html.setInput(new InputUniString(sb.toString(), MIME.TYPE_TEXT_HTML, null, null));
		INode htmlroot = (INode)html.parse(doc);
		doc.remove();
//		span.getDocument());
//htmlroot.dump();
		// => rather than various childAt()'s, use search DFS for "p" or "div" tag!
		INode body = (INode)htmlroot.childAt(1);	// head is 0
		INode replacep = replace.getParentNode();
		replacep.setChildAt(body.childAt(1), replace.childNum());
		//replacep.appendChild(body.childAt(1));
		//Leaf l=replacep.getLastLeaf();
		Leaf l=body.getLastLeaf();
		span.moveq(n0.getFirstLeaf(),0, l,l.size());
n0.dump();
body.childAt(1).dump();
System.out.println("OutlineSpan now "+span);

	} catch (Exception e) {
		//new LeafUnicode("ERROR "+e,null, doc);
		e.printStackTrace();
	} finally {
		try { html.close(); } catch (IOException ioe) {}
	}

	return null;
  }


/*
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	//readHistory(); => on demand
  }
*/
}
