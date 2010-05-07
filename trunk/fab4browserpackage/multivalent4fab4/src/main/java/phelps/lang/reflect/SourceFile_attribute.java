package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class SourceFile_attribute extends Attribute_info {
  public String source;
  SourceFile_attribute(Cp_info[] cp, ClassFile cf, String name, int off) {
	super(cp, name, off);
	source = cp[cf.readu2()].getString(cp, cf);
  }
  public String toString() { return "SourceFile="+source; }
}
