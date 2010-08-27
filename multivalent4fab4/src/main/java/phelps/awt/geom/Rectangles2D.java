package phelps.awt.geom;

import java.awt.geom.Rectangle2D;



/**
	Extensions to {@link Rectangle2D}.

	<ul>
	<li>pretty printing: {@link #pretty(Rectangle2D)}.
	</ul>

	<p>Note that a {@link java.awt.Rectangle} is a subclass of {@link java.awt.geom.Rectangle2D}.

	@version $Revision: 1.4 $ $Date: 2003/07/26 02:14:32 $
*/
public class Rectangles2D {
  private Rectangles2D() {}

  public Rectangle2D parse(String line) {
	// parse WxH@(x,y)
	return null;
  }

  /** Compact output for a Rectangle: <code><var>width</var>x<var>height</var> @ (<var>x</var>,<var>y</var>)</code>. */
  public static String pretty(Rectangle2D r) { return ""+r.getWidth()+"x"+r.getHeight()+"@("+r.getX()+","+r.getY()+")"; }

}
