package multivalent.std.adaptor;

import java.awt.Color;

import multivalent.Context;
import multivalent.CLGeneral;



/**
	Adds CSS-specific attributes to CLGeneral.

	@version $Revision: 1.2 $ $Date: 2002/02/01 08:47:55 $
*/
public class CSSGeneral extends CLGeneral {
  public Color cbordertop, cborderbottom, cborderleft, cborderright;
  public String borderstyle;
  public String texttransform;

  public CSSGeneral() { super(); }
  public CSSGeneral(int priority) { super(priority); }

  public void invalidate() {
	super.invalidate();
	borderstyle = texttransform = Context.STRING_INVALID;
	cborderleft = cborderright = cbordertop = cborderbottom = Context.COLOR_INHERIT;
  }

  /** All attributes or just inherited ones? */
  public boolean appearance(Context cx, boolean all) {
	super.appearance(cx, all);
//if (this.foreground_ == Color.RED) System.out.println("CSS General: fg = red");

	// ...

	if (all) {  // attributes that are not inherited
		CSSContext cssx = (CSSContext)cx;
		if (borderstyle!=Context.STRING_INVALID) cssx.borderstyle = borderstyle;

		if (cborderleft!=Context.COLOR_INVALID) cssx.cborderleft = cborderleft; //System.out.println("CSSGeneral borderleft = "+cborderleft); }
		if (cborderright!=Context.COLOR_INVALID) cssx.cborderright = cborderright;
		if (cbordertop!=Context.COLOR_INVALID) cssx.cbordertop = cbordertop;
		if (cborderbottom!=Context.COLOR_INVALID) cssx.cborderbottom = cborderbottom;
	}
	return false;
  }

  public void copyInto(CLGeneral dest) {
	super.copyInto(dest);
	// ...
  }

  public boolean equals(Object o) {
	if (!super.equals(o)) return false;
	// check more atributes
	return true;    //this==o;
  }

  public int hashCode() {
	return super.hashCode();
  }
}
