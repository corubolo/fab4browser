package multivalent.std;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;

import multivalent.*;
import multivalent.gui.VScrollbar;


/**
	Record last scroll positions on document and mark on scrollbar.
	One-hour hack, based on multivalent.std.ScrollbarViz.

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:27:51 $
*/
public class BrowseScrollViz extends Behavior {
  static final int MAXHISTORY = 5;
  static final int CLOSE = 50;

  Map<URI,int[]> doc2y_ = new HashMap<URI,int[]>(20);

  int[] getYs(URI uri) {
	int[] ys = doc2y_.get(uri);
	if (ys==null) { ys = new int[MAXHISTORY]; doc2y_.put(uri, ys); }
	return ys;
  }

  /**
	On {@link Document#MSG_FORMATTED} to add observer on scrollbar.
	On {@link Document#MSG_CLOSE} to record y-scroll position.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_FORMATTED==msg && arg instanceof Document) {
		((Document)arg).getVsb().addObserver(this);
//System.out.println("addObserver on "+vsb);

	} else if (Document.MSG_CLOSE==msg && arg instanceof Document) {
		Document doc = (Document)arg;
		VScrollbar vsb = doc.getVsb();

		// clean up old
		vsb.deleteObserver(this);

		// record Y
		int y = vsb.getValue();
		if (y > vsb.getMin()) { // interesting...
			int[] ys = getYs(doc.getURI());
			boolean already = false;    // ...and don't alreayd have it
			for (int i=0,imax=ys.length; i<imax; i++) {
				if (Math.abs(ys[i] - y) < CLOSE) {  // close enough?
					ys[i] = y;  // take new y
					already=true;
					break;
				}
			}
			if (!already) {
				System.arraycopy(ys,0, ys,1, ys.length-1);  // LRU
				ys[0] = y;
//System.out.println("recorded "+y);
			}
		}
	}
	return false;
  }


  /** Decorate scrollbar. */
  public boolean paintAfter(Context cx, Node n) {
	Document doc = n.getDocument();
	VScrollbar vsb = (VScrollbar)n;

	int[] ys = getYs(doc.getURI());
	int doch = vsb.getMax()-vsb.getMin();
	int sbh=vsb.bbox.height, sbw=vsb.bbox.width, min=vsb.getMin(), max=vsb.getMax();
	Graphics2D g = cx.g;
	g.setColor(Color.BLACK/*WHITE*/);
//System.out.println("painting "+ys[0]);
	for (int i=0, imax=ys.length; i<imax; i++) {
		int y = ys[i];
		if (y <= min) break; else if (y > max) continue;
		g.drawRect(0, y * sbh / doch, sbw,1);
	}
	return false;
  }
}
