package multivalent.devel.lens;

import java.awt.*;
import java.util.Map;

import multivalent.*;
import multivalent.std.lens.Lens;
import multivalent.gui.VFrame;

import phelps.lang.Integers;



/**
	Screen ruler, used to measure layouts.

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:16:49 $
*/
public class Ruler extends Lens {
  static final int FIXEDHEIGHT=40;   // or WIDTH
  int smalltick=10, bigtick=100;

  public static final String ATTR_BIGTICK = "bigtick";
  public static final String ATTR_SMALLTICK = "smalltick";


  /** Draw tick marks on top of whatever. */
  public boolean paintAfter(Context cx, Node node) {
	Rectangle cbbox = getContentBounds();
	int pad=5, x0=cbbox.x+pad, y0=cbbox.y+pad,  w=cbbox.width-pad*2, h=cbbox.height-pad*2;

	Graphics2D g = cx.g;
	g.setColor(Color.BLUE);
	if (cbbox.width > cbbox.height) {
		g.drawLine(x0,y0, x0+w,y0); // bar
		// tick marks
		for (int x=x0,xmax=x0+w; x<xmax; x+=smalltick) {
			g.drawLine(x,y0, x,y0+5);
			if ((x-x0)%bigtick==0) g.drawLine(x,y0, x,y0+10);
		}
	} else {
		g.drawLine(x0,y0, x0,y0+h);
		for (int y=y0,ymax=y0+h; y<ymax; y+=smalltick) {
			g.drawLine(x0,y, x0+5,y);
			if ((y-y0)%bigtick==0) g.drawLine(x0,y, x0+10,y);
		}
	}

	return super.paintAfter(cx,node);
  }

  /**
	Fix height/width at FIXEDHEIGHT pixels, depending on orientation.
	Orentation implied by relative dimension lengths.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println(msg+", "+(se.getArg()==win_));
	if (VFrame.MSG_RESIZED==msg && se.getArg()==win_) {
		Rectangle r=win_.bbox;
		if (r.width >= r.height) r.height=FIXEDHEIGHT; else r.width=FIXEDHEIGHT;
		//win_.bbox.height = FIXEDHEIGHT;
	}
	return super.semanticEventAfter(se, msg);
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	attr.put(ATTR_HEIGHT, Integer.toString(FIXEDHEIGHT));
	super.restore(n,attr,layer);
//	win_.setSize(win_.bbox.width, 40); -- maybe restore this

	smalltick = Integers.parseInt(getAttr(ATTR_SMALLTICK), 10);
	bigtick = Integers.parseInt(getAttr(ATTR_BIGTICK), 100);
  }
}
