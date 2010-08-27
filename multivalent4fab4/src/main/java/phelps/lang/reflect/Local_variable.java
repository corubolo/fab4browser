package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Local_variable {
  int start_pc, length;
  String name, descriptor;
  int index;

  public Local_variable(Cp_info[] cp, ClassFile cf) {
	start_pc=cf.readu2(); length=cf.readu2();
	name=cp[cf.readu2()].getString(cp, cf); descriptor=cp[cf.readu2()].getString(cp, cf);
	index=cf.readu2();
  }
  public String toString() { return name; }
}
