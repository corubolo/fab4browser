package multivalent.std;

import java.awt.*;

import multivalent.*;


/**
	Set as observer on tree node to disable action in that subtree:	shortcircuits events, visually grays out content.
	Used to disable buttons.

<!--
	TO DO
	Associate a count to make a guard: e.g., click once to remove, again to click button
	(display differently in that case).
-->

	@version $Revision: 1.3 $ $Date: 2002/02/01 04:17:26 $
*/
public class DisableTree extends Behavior {
  public DisableTree() { setName("DisableTree"); }  // let creator set this

  public boolean paintAfter(Context cx, Node n) {
	Graphics2D g = cx.g;
	Rectangle bbox = n.bbox;

	//Color fg = cx.foreground;
	//g.setColor(fg.getRed() + fg.getGreen()+fg.getBlue() < 255*3/2? Color.BLACK: Color.WHITE);

	// effect: horizontal line through middle
	/*int mid = bbox.height/2;
	g.drawLine(0,mid, bbox.width,mid);
	g.drawLine(0,mid+1, bbox.width,mid+1);*/

	// effect: slash from top left bottom right
	//g.drawLine(0,0, bbox.width,bbox.height);
	//g.drawLine(0,1, bbox.width,bbox.height+1);

	// effect: set every other line to background color
	g.setColor(cx.background);
	for (int y=0,ymax=bbox.height; y<ymax; y+=3) g.drawLine(0,y, bbox.width,y);

	return false;
  }

  // shortcircuit to after, but normal after that
  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) { return true; }
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) { return false; }
}
