package phelps.awt.geom;

import java.awt.geom.Dimension2D;



/**
	Subclass of {@link Dimension2D} with <code>double</code> dimensions.
	Dimension2D should provides a way with inner classes like Point2D, but doesn't.

	@version $Revision: 1.1 $ $Date: 2003/09/29 14:23:15 $
*/
public class Dimension2D_Double extends Dimension2D {
  public/*public in java.awt.Dimension*/ double width, height;

  public Dimension2D_Double() { this(0.0, 0.0); }
  public Dimension2D_Double(double width, double height) { setSize(width, height); }

  public double getWidth() { return width; }
  public double getHeight() { return height; }
  public void setSize(double width, double height) { this.width=width; this.height=height; }

  public String toString() {	// same as java.awt.Dimension
	return getClass().getName() + "[width=" + width + ",height=" + height + "]";
  }
}
