package phelps.awt.geom;

import java.awt.geom.AffineTransform;



/**
	Extensions to {@link AffineTransform}.

	<ul>
	<li>pretty printing: {@link #pretty(AffineTransform)}.
	</ul>

	@version $Revision$ $Date$
*/
public class AffineTransforms {
  private AffineTransforms() {}


  /** Pretty printing. */
  public static String pretty(AffineTransform x) { return x.toString(); }
}
