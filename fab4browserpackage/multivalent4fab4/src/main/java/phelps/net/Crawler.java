package phelps.net;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.io.*;

import com.pt.awt.NFont;

import multivalent.node.Root;   // builds document trees and collects links
//import multivalent.std.adaptor.HTML;
//import multivalent.std.adaptor.ASCII;
import multivalent.std.span.HyperlinkSpan;



/**
	<i>Under development</i>.
	Crawler over network or file system, reporting to clients.

	Given a start page, crawls.
	Specify:	this page only, subtree only, site only.
	And:		levels deep

	Multithreaded, no cycles, respects ROBOTS.TXT, ...

	Add as an observer to take action after each page parse.

	@see phelps.net.RobustHyperlink

	@version $Revision: 1.2 $ $Date: 2003/06/01 08:13:48 $
*/
public class Crawler /* not a Behavior--utility, like Location */ extends/*!*/ Observable implements Runnable {
  public boolean fTrace=false;

  public static final int PAGE=0, SUBTREE=1, SITE=2, ANY=3;

  int maxThreads_ = 20;
  int threadCnt_ = 0;
  int totalLinks = 0;
  int scope_ = SUBTREE;
  int maxDepth_ = Integer.MAX_VALUE;
  URL start_;

  Map<String,String> seen = new HashMap<String,String>(200);
  List<LinkRec> pool = new LinkedList<LinkRec>();
  List<LinkRec> active = new LinkedList<LinkRec>();
  CrawlerDisplay dis = new CrawlerDisplay(active);


  public Crawler(URL start) {
	start_ = start;
  }

  public void setMaxThreads(int max) { maxThreads_=max; }
  public int getMaxThreads() { return maxThreads_; }
  public void setScope(int scope) { scope_=scope; }
  public int getScope() { return scope_; }
  public void setMaxDepth(int maxDepth) { maxDepth_=maxDepth; }
  public int getMaxDepth() { return maxDepth_; }


  synchronized void addLinks(Collection<HyperlinkSpan> links, int depth) {
	if (depth>=maxDepth_) return;

	for (Iterator<HyperlinkSpan> i=links.iterator(); i.hasNext(); ) {
		HyperlinkSpan span = i.next();
//System.out.println("testing "+span.getTarget()+", class="+span.getTarget().getClass());
		URL url = (URL)span.getTarget();
		String proto=url.getProtocol(), ref=url.getRef();
		if (ref!=null) try { url = new URL(proto, url.getHost(), url.getFile()); } catch (MalformedURLException canthappend) {}
		String surl = url.toString();

		if ((!"file".equals(proto) && !"http".equals(proto)) || seen.get(surl)!=null) continue;

		switch (scope_) {
		case PAGE:
			continue;
		case SUBTREE:
//System.out.println(url.getFile()+" contains "+start_.getFile());
			if (!start_.getHost().equals(url.getHost()) || url.getFile().indexOf(start_.getFile())!=0) continue;
			break;
		case SITE:
			if (!start_.getHost().equals(url.getHost())) continue;
			break;
		case ANY:
			break;
		}


		setChanged(); notifyObservers(url);

		pool.add(new LinkRec(url, depth));
		seen.put(surl, surl);
		totalLinks++;
		//System.out.println("+ "+url.toString());
	}
  }


  synchronized void workerDone(LinkRec rec) {
	synchronized (active) { active.remove(rec); }

	threadCnt_--;
	if (rec!=null) {
		setChanged();
		notifyObservers(rec.root);
	}

	if (fTrace) System.out.println(""+threadCnt_+"/"+Thread.currentThread().getThreadGroup().activeCount()+" threads, "+pool.size()+" links to go");
	while (pool.size()>0 && threadCnt_<maxThreads_) {
		LinkRec nextrec = pool.remove(0);
		synchronized (active) { active.add(nextrec); }
		new CrawlerThread(this, nextrec).start();
		threadCnt_++;
		if (fTrace) System.out.println("thread #"+threadCnt_+": "+nextrec.url);
		//System.out.println(nextrec.url);
	}

	if (pool.size()==0 && threadCnt_==0) {
		System.out.println("Done -- scanned "+totalLinks+" pages");
	}

	dis.repaint(200);
  }

  public void run() {
	pool.add(new LinkRec(start_,0));
	threadCnt_=1; workerDone(null);
  }


  class TestObserver implements Observer {
	public void update(Observable o, Object arg) {
		//Root root = (Root)arg;
		//System.out.println("hi there "+root.childAt(0));
	}
  }

  class CopyObserver implements Observer {
	public void update(Observable o, Object arg) {
		if (arg instanceof URL) {
			// copy from cache into separate tree
		} else if (arg instanceof Root) {
		}
	}
  }

  public static void main(String[] argv) {
	try {
		Crawler crawler = new Crawler(new URL(argv[0]));
		crawler.addObserver(crawler.new TestObserver());
		new Thread(crawler).start();
	} catch (MalformedURLException male) {
		System.err.println("bad URL: "+male);
//	  } catch (InterruptedException e) {
//		  System.err.println("Interrupted: "+e);
	}
//	System.out.println("DONE");
//	System.exit(0);
  }
}



/** Shared data record between Crawler and CrawlerThread */
class LinkRec {
  public URL url;
  public int depth;
  public long bytesLength=-1;
  public long bytesRead=0;
  public Root root = new Root(null, null);

  public LinkRec(URL url, int depth) { this.url=url; this.depth=depth; }
  public void setLength(long len) { bytesLength=len; }
  public void setRead(long read) { bytesRead=read; }
}


class CrawlerDisplay extends Panel {
  static NFont FONT_LEN = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 9f);

  protected Frame frame_ = new Frame();
  protected List<LinkRec> active_;

  CrawlerDisplay(List<LinkRec> active) {
	active_ = active;
	frame_.add(this);
	frame_.pack();
	frame_.show();
  }

  public Dimension getPreferredSize() { return new Dimension(500,300); }
  public Dimension getSize() { return getPreferredSize(); }

  public void paint(Graphics g_old_api) {
	Graphics2D g = (Graphics2D)g_old_api;
	int len=active_.size();
	if (len==0) return;
	int w=getWidth(), h=getHeight();
	int y=0, yinc=h/len;

	NFont f = FONT_LEN;
	yinc=10;
	g.setColor(Color.BLACK);
	synchronized (active_) {
		// should display amount read, total to read, URL
		for (Iterator<LinkRec> i=active_.iterator(); i.hasNext(); y+=yinc) {
			LinkRec rec = i.next();
			f.drawString(g, Long.toString(rec.bytesLength), 0+2,y+yinc);
			f.drawString(g, rec.url.toString(), 50,y+yinc);
			/*
			g.setColor(Color.WHITE); g.fillRect(0,y, w,y+yinc);
			g.setColor(Color.RED); g.fillRect(0,y, w*rec.bytesRead/rec.bytesLength,y+yinc);
			*/
		}
	}
  }
}


/**
	Worker thread: take page, if HTML parse and report links, call observers, request more work.
*/
class CrawlerThread extends Thread {
  protected Crawler control_;
  protected LinkRec rec_;

  CrawlerThread(Crawler control, LinkRec rec) {
	control_ = control;
	rec_ = rec;
  }


  public void run() {
//System.out.println("file = "+url_.getFile());
	String cmpfile = rec_.url.getFile().toLowerCase();

	try {
		URLConnection con = rec_.url.openConnection();
		String type = con.getHeaderField("Content-Type");
		String length = con.getHeaderField("Content-Length");
		if (length!=null) rec_.bytesLength = Long.parseLong(length);
		//System.out.println("length = "+(length!=null? length: "unknown"));
//System.out.println("encoding = "+type);
		InputStream in = new BufferedInputStream(con.getInputStream());

		// => use Multivalent's MIME and suffix  to  media adaptor table
		if ("text/html".equalsIgnoreCase(type) || cmpfile.endsWith(".htm") || cmpfile.endsWith(".html")) {
			//HTML html = new HTML(); html.docURI=rec_.url;
			//html.parse(in, rec_.root);	 // should be able to get status
//System.out.println("added "+html.links.size()+" links");
//RESTORE			control_.addLinks(html.links, rec_.depth+1);
		} else { // treat as ASCII
			//new ASCII().parse(in, rec_.root);
		}
	} catch (Exception e) {
		// if program, ignore and keep on chugging
		rec_.root=null;
	}

	control_.workerDone(rec_);
  }
}
