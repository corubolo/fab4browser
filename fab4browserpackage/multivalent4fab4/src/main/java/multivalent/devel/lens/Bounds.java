package multivalent.devel.lens;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.std.lens.Lens;



/**
	Show bounds of tree nodes.

	@version    $Revision: 1.2 $ $Date: 2002/02/02 12:35:39 $
*/
public class Bounds extends Lens {
  public static final Color[] RAINBOW = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE };
  //static int RAINBOWi_=0;

  protected NFont font_ = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 7f);
//	boolean absolute_ = false;


  /** Redraw underlying text in light colors. */
  public boolean appearance(Context cx, boolean all) {
	//cx.foreground=cx.foreground.brighter(); cx.background=cx.background.brighter();
	cx.foreground=Color.LIGHT_GRAY; cx.background=Color.WHITE;
	return false;
  }

  public boolean paintAfter(Context cx, Node node) {
	Graphics2D g = cx.g;
	g.setColor(Color.RED);
	Rectangle clip = getContentBounds();
	Graphics2D g2 = (Graphics2D)g.create(); g2.clip(clip);
	paintRecurse(g2, g2.getClipBounds(), getBrowser().getRoot());
	g2.dispose();

	return super.paintAfter(cx, node);
  }

  void paintRecurse(Graphics2D g, Rectangle cliprect, Node n) {
	Rectangle bbox = n.bbox;

	if (cliprect.intersects(bbox)) {
		if (n.isStruct()) {
			//INode in = (INode)n;
			//int dx=bbox.x + in.padding.left, dy=bbox.y + in.padding.top;
			int dx=n.dx(), dy=n.dy();
			g.translate(dx,dy); cliprect.translate(-dx,-dy);
			INode p = (INode)n;
			for (int i=0,imax=p.size(); i<imax; i++) paintRecurse(g, cliprect, p.childAt(i));
			g.translate(-dx,-dy); cliprect.translate(dx,dy);
		} else {
			g.setColor(RAINBOW[bbox.x % RAINBOW.length]); g.draw(bbox);
//			  if (absolute_) { bbox.x+= }

//			g.setColor(Color.WHITE); g.fillRect(x+1,y, w+2,font_.getHeight());

			String ul = "("+bbox.x+","+bbox.y+")"; // +bbox.width+"x"+bbox.height;
//			String lr = "("+(bbox.x+bbox.width)+","+(bbox.y+bbox.height)+")";
			g.setColor(Color.BLACK); font_.drawString(g, ul, bbox.x+2,bbox.y+9);
//			g.drawString(lr, bbox.x+bbox.width-2-
		}
	}
  }
}
