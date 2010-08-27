package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Deprecated_attribute extends Attribute_info {
  Deprecated_attribute(Cp_info[] cp, String name, int off) { super(cp, name, off); }
  public String toString() { return "Deprecated"; }
}

