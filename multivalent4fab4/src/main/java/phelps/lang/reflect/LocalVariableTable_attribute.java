package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class LocalVariableTable_attribute extends Attribute_info {
  Local_variable[] locals;
  LocalVariableTable_attribute(Cp_info[] cp, ClassFile cf, String name, int off) {
	super(cp, name, off);
	locals = new Local_variable[cf.readu2()];
//System.out.println("local variable table,l len="+locals.length);
	for (int i=0,imax=locals.length; i<imax; i++) locals[i]=new Local_variable(cp, cf);
  }

  public Local_variable getLocal(int index) {
//System.out.println("looking for index="+index/*+" in set of "+locals.length*/);
	for (int i=0,imax=locals.length; i<imax; i++) {
		if (locals[i].index==index) return locals[i];
	}
	return null;
  }

  public String toString() {
	return "LocalVariableTable, length="+locals.length;
  }
}


