package multivalent.std;

import java.awt.*;
import java.net.URI;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VScrollbar;
import multivalent.gui.VCheckbox;



/**
	Show various metadata: HTTP headers, HTML META tags.

<!--
	also, word count, ...
	LATER: if cursor within header area, don't show headers (it's obscuring text)
	(Rename to "DocInfo"?)
-->

	@version $Revision: 1.2 $ $Date: 2002/02/01 03:12:03 $
*/
public class ShowHeaders extends Behavior {
  /**
	Toggle viewing of last modified and other headers.
	<p><tt>"viewLastModified"</tt>.
  */
  public static final String MSG_VIEW = "viewLastModified";

  static NFont defaultFont_ = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 10f);


  String[] header=null;
  String[] vals=null;
  boolean active_=false;
  Document curdoc_ = null;
  Map<URI,Integer> doc2wcnt = new HashMap<URI,Integer>(100);


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		INode menu = (INode)se.getOut();
		// choose which of following depending on if page is already in bookmark?  may want to file in a couple of places
		// "Show HTTP headers"
		VCheckbox ui = (VCheckbox)createUI("checkbox", "Show Date Last Modified", "event "+MSG_VIEW, menu, null, false);
		ui.setState(active_);
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// update last visited field, if new page is bookmarked
	if (MSG_VIEW==msg) {
		active_ = !active_;
		//if (ui!=null) ui.setState(active_);
		getBrowser().getCurDocument().repaint();

	} else if (Document.MSG_OPENED==msg) {
		Object arg = se.getArg();
		if (arg instanceof DocInfo) {
			DocInfo di = (DocInfo)arg;
			Document doc = di.doc;
			if (doc!=null) {
//long start = System.currentTimeMillis();
				int wcnt=0;
				for (Leaf l=doc.getFirstLeaf(),lend=doc.getLastLeaf(); l!=lend && l!=null; l=l.getNextLeaf()) wcnt++;
//System.out.println("word counting took "+(System.currentTimeMillis() - start)+" ms"); -- take little time(?)
				if (di.uri!=null) doc2wcnt.put(di.uri, new Integer(wcnt));
			}
		}

	} else if (Document.MSG_CURRENT==msg) {
		if (curdoc_!=null) curdoc_.deleteObserver(this);
		curdoc_ = (Document)se.getArg();
		if (curdoc_!=null) curdoc_.addObserver(this);
	}

	return super.semanticEventAfter(se,msg);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	active_ = !"FALSE".equalsIgnoreCase(getAttr("active", "FALSE"));	// biased toward ON
  }


  /** Don't draw background box as would obscure too much of page; instead draw "smeared" text. */
  void drawOnBackground(Graphics2D g, String msg, int x, int y) {
	if (null==msg) return;

//System.out.println("header: "+msg);
	NFont f = defaultFont_;
	g.setColor(Color.WHITE); f.drawString(g, msg, x-1,y); f.drawString(g, msg, x+1,y); f.drawString(g, msg, x,y-1); f.drawString(g, msg, x,y+1);
	g.setColor(Color.BLACK); f.drawString(g, msg, x,y);
  }


  public boolean paintAfter(Context cx, Node node) {
	if (!active_) return false;
	if (curdoc_.getVsb().getValue()!=0 || curdoc_.bbox.width<=300) return false;

	// instead place content on absolute position layer?
	Browser br = getBrowser();
//	Dimension dim = br.getRoot().bbox.getSize();
	// if header==null, show all; if header.size()==1, just show value (presumably you know key); else show named key-value pairs
	// convert dates -- maybe attempt on all names matching pattern "*date*"
	// convert dates from int/long's?  to relative?

	// LATER
	String headerlist = (getAttr("headers", "*"));
	if (headerlist=="*") {	  // refresh each document
		// collect with br.getHeaderKeys()
	}
	Map curheaders = (Map)curdoc_.getVar("headers");
	if (curheaders!=null) for (StringTokenizer st = new StringTokenizer(headerlist); st.hasMoreTokens(); ) {
		String header = st.nextToken();
		String val = (String)curheaders.get(header);
		if (val!=null) {
		}
	}

	// first pass: get max width of box
	//String val = br.getHeaderField("Last-Modified");
	//String val = "disconnected for now";
//System.out.println("show headers last mod = "+val);
	NFont f = defaultFont_;
	int h=(int)f.getHeight()-2/*squeeze*/, y=0+h, right=node.bbox.width - VScrollbar.SIZE - 2;

	Graphics2D g = cx.g;
	String val = (curheaders!=null? (String)curheaders.get("Last-Modified"): null);
	if (val!=null) {
		drawOnBackground(g, val, right-(int)f.stringAdvance(val).getX(), y);
		y += h;
	}

	URI uri = curdoc_.getURI();
	Integer wcnto;
	if (uri!=null && (wcnto=doc2wcnt.get(uri))!=null && wcnto.intValue()>10) {
		val = ""+wcnto+" words";
		drawOnBackground(g, val, right-(int)f.stringAdvance(val).getX(), y);
		y += h;
	}

	return false;
  }
}
