package multivalent.std;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import multivalent.*;
import multivalent.gui.VScrollbar;
//import multivalent.std.SearchHit;



/**
	Visualization of search results on scrollbar.
	Somebody at Berkeley ACM DL Conference did it, but TkMan had it first, and probably others before it.  TkMan's version is also more general.

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:56:53 $
*/
public class ScrollbarViz extends Behavior {
  public static int tbwidth=4;

  Map<VScrollbar,List[]> doc2hits_ = new HashMap<VScrollbar,List[]>(20);


  /**
	On "searchHits", stash results and relevant document.
	On {@link Document#MSG_CLOSE}, clean up stash.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	Object in = se.getIn();

	if (super.semanticEventBefore(se,msg)) return true;
	else if (Search.MSG_HITS==msg) {
//System.out.println("searchHits, in="+in);
		if (in instanceof Node) {
			VScrollbar vsb = ((Node)in).getDocument().getVsb();
			vsb.addObserver(this);
//System.out.println("addObserver on "+vsb);
			doc2hits_.put(vsb, (List[])arg);
		}
	} else if (Document.MSG_CLOSE==msg) {
		if (arg instanceof Document) {
			VScrollbar vsb = ((Document)arg).getVsb();
			vsb.deleteObserver(this);
			doc2hits_.remove(vsb);
		}
	}
	return false;
  }


  /** Decorate scrollbar, using stashed information. */
  public boolean paintAfter(Context cx, Node n) {
	// positions already computed, so just draw in scrollbar space
	VScrollbar vsb = (VScrollbar)n;
//System.out.println("decorate "+vsb);
	//g.setColor(Color.RED); g.drawLine(0+vsb.bbox.x,vsb.arrowH, vsb.SIZE+vsb.bbox.x,vsb.bbox.height-vsb.arrowH);

	int doch = vsb.getMax()-vsb.getMin();
	int /*sbx=vsb.bbox.x, sby=vsb.bbox.y,*/ sbw=vsb.SIZE, sbh=vsb.bbox.height; // bbox of scrollbar trough
	//sbx=0; sby=0;   // observers in same coordinate space
	Color[] colors = Search.colors;
	List[] hits = doc2hits_.get(vsb);
	Graphics2D g = cx.g;
	if (hits!=null) for (int i=0,imax=hits.length; i<imax && i*tbwidth<sbw; i++) {
		g.setColor(colors[i]);
		List<SearchHit> hitlist = (List<SearchHit>)hits[i];
		// this draws boxes.  should also have TkMan bars
		for (Iterator<SearchHit> j=hitlist.iterator(); j.hasNext(); ) {
			SearchHit hit = j.next();
			int x = /*sbx+*/i*tbwidth, y = /*sby+*/hit.y * sbh / doch;
//System.out.println("drawing at "+x+","+y);
			g.drawRect(x,y, 2,2);
		}
	}
	return false;
  }
}
