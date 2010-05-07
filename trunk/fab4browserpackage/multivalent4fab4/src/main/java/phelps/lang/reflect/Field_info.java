package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Field_info {
  public static final int ACC_PUBLIC=0x0001, ACC_PRIVATE=0x0002, ACC_PROTECTED=0x0004, ACC_STATIC=0x0008,
	ACC_FINAL=0x0010, ACC_VOLATILE=0x0040, ACC_TRANSIENT=0x0080;

  public short access_flags;
  public String name;	//u2 index
  public String descriptor;	//u2 index
//	u2 attributes_count;
//	Attribute_info attributes[attributes_count];
  public Attribute_info[] attributes;

  public Field_info(Cp_info[] cp, ClassFile cf) {
	access_flags = cf.readu2();
	name = cp[cf.readu2()].getString(cp, cf);
	descriptor = cp[cf.readu2()].getString(cp, cf);
	attributes = new Attribute_info[cf.readu2()];
	for (int i=0,imax=attributes.length; i<imax; i++) attributes[i]=cf.getAttributeInfo(cp);
  }

  public static String access2String(int access) {
	if (access==0) return " ";
	StringBuffer sb = new StringBuffer(25);
	if ((access&ACC_PUBLIC)!=0) sb.append("public ");
	if ((access&ACC_PRIVATE)!=0) sb.append("private ");
	if ((access&ACC_PROTECTED)!=0) sb.append("protected ");
	if ((access&ACC_STATIC)!=0) sb.append("static ");
	if ((access&ACC_FINAL)!=0) sb.append("final ");
	if ((access&ACC_VOLATILE)!=0) sb.append("volatile ");
	if ((access&ACC_TRANSIENT)!=0) sb.append("transient ");
	return sb.substring(0); // if 0 or 1 set, cheapter to return literal string
  }

  public String toString() {
	int size = ClassFile.sizeof(descriptor);
	return access2String(access_flags)+ClassFile.type2String(descriptor)+name+" ("+size+(size>1?" bytes)":" byte)");
	//return name+" "+descriptor;
  }
}
