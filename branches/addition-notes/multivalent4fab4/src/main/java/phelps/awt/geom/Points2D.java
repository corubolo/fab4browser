package phelps.awt.geom;

import java.awt.geom.Point2D;



/**
	Extensions to {@link Point2D}.

	<ul>
	<li>pretty printing: {@link #pretty(Point2D)}.
	</ul>

	<p>Note that a {@link java.awt.Point} is a subclass of {@link java.awt.geom.Point2D}.

	@version $Revision$ $Date$
*/
public class Points2D {
  private Points2D() {}


  /** Compact output for a Point: <code><var>x</var>,<var>y</var></code>. */
  public static String pretty(Point2D p) { return p.getX()+","+p.getY(); }
}
