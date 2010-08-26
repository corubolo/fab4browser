package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Cp_info {
  static final boolean VERBOSE=true;

  public static final int CLASS=7, FIELDREF=9, METHODREF=10, INTERFACEMETHODREF=11, STRING=8,
	INTEGER=3, FLOAT=4, LONG=5, DOUBLE=6, NAMEANDTYPE=12, UTF8=1; // no type=2

//static int tagcnt=1;
  // need either tag+off or Object
  public byte tag;
  int off;//,len;
  Object o_ = null;  // equivalent as Java object type


  public Cp_info(ClassFile cf) {
	tag = cf.readu1();
	//off=offset;
	off = cf.getPosition();

	/*long u8;*/ /*int u4;*/ short u2; /*byte u1;*/
//if (VERBOSE) System.out.print(tagcnt+". "+tag+"/");
//tagcnt++;
//if (tagcnt>50) System.exit(0);
	switch (tag) {
	case UTF8:
		u2 = cf.readu2();  // length
		o_ = new String(cf.raw_, off+2, u2);
		//offset += u2;
		cf.setPosition(off+2+u2);
		break;
	case INTEGER:
		//u4 = cf.readu4();
		o_ = new Integer(cf.readu4());
		break;
	case FLOAT:
		//u4 = cf.readu4();
		o_ = new Float(Float.intBitsToFloat(cf.readu4()));
		break;
	case LONG:
		//u8 = cf.readu8();
		o_ = new Long(cf.readu8());
		break;
	case DOUBLE:
		//u8 = cf.readu8();
		o_ = new Double(Double.longBitsToDouble(cf.readu8()));
		break;

	// Pointers:
	case CLASS:
		u2 = cf.readu2();
		break;
	case STRING:
		u2 = cf.readu2();  // pointer to constant pool constant
		break;
	case FIELDREF:
	case METHODREF:
	case INTERFACEMETHODREF:
		u2 = cf.readu2();	// pointer to class
		u2 = cf.readu2();	// pointer to name and type
		break;
	case NAMEANDTYPE:
		u2 = cf.readu2();	// pointer to name
		u2 = cf.readu2();	// pointer to type descriptor
		break;
	default:
		System.err.println("bad constant type "+tag);//+" @ tagcnt="+(tagcnt-1));
		//System.exit(1);
	 }
  }

  public boolean isNative() {
	return tag==INTEGER || tag==FLOAT || tag==LONG || tag==DOUBLE;
  }

  public String getString(Cp_info[] cp, ClassFile cf) { return getObject(cp, cf).toString(); }

  Object getObject(Cp_info[] cp, ClassFile cf) {
	int offin=cf.getPosition(); cf.setPosition(off);
	Object o = null;
	//long u8; int u4; short u2; byte u1; Class c;
	switch (tag) {
	case UTF8:
	case INTEGER:
	case FLOAT:
	case LONG:
	case DOUBLE:
		o = o_;
		break;

	case CLASS: o=cp[cf.readu2()].getString(cp, cf); break;
	case STRING: o=cp[cf.readu2()].getString(cp, cf); break;
	case FIELDREF:
	case METHODREF:
	case INTERFACEMETHODREF:
		String cls = cp[cf.readu2()].getString(cp, cf);
		cf.setPosition(cp[cf.readu2()].off);
		//System.out.println("offset = "+offset+"/"+raw.length+", name index="+cf.readu2());
		String name = cp[cf.readu2()].getString(cp, cf);
		String sig = cp[cf.readu2()].getString(cp, cf);
		o = cls+"."+name+(sig.startsWith("(")?"()":"");
		//String name = cp[cf.readu2()]
		//o=cp[cf.readu2()].getString(cp, cf)+"."+cp[cf.readu2()].getString(cp, cf);
		break;
	case NAMEANDTYPE:
		o=cp[cf.readu2()].getString(cp, cf)+" "+cp[cf.readu2()].getString(cp, cf);
		break;
	default: assert false: tag;
	}
	cf.setPosition(offin);
	return o;
  }

/*
  public String toString() {
	switch (tag) {
	case UTF8:
		System.out.println("UTF8 = "+o_);
		break;
	case INTEGER:
		System.out.println("int = "+o_);
		break;
	case FLOAT:
		System.out.println("float = "+o_);
		break;
	case LONG:
		System.out.println("long = "+o_);
		break;
	case DOUBLE:
		System.out.println("double ="+o_);
		break;

	case CLASS: o=cp[cf.readu2()].getString(cp, cf);
		System.out.println("Class => cp"+u2);
		break;
	case STRING: o=cp[cf.readu2()].getString(cp, cf);
		System.out.println("String => cp"+u2);
		break;
	case FIELDREF:
	case METHODREF:
	case INTERFACEMETHODREF:
		String cls = cp[cf.readu2()].getString(cp, cf);
		cf.setPosition(cp[cf.readu2()].off);
		//System.out.println("offset = "+offset+"/"+raw.length+", name index="+cf.readu2());
		String name = cp[cf.readu2()].getString(cp, cf);
		String sig = cp[cf.readu2()].getString(cp, cf);
		o = cls+"."+name+(sig.startsWith("(")?"()":"");
		//String name = cp[cf.readu2()]
		//o=cp[cf.readu2()].getString(cp, cf)+"."+cp[cf.readu2()].getString(cp, cf);
		{ if (tag==FIELDREF) System.out.print("FieldRef"); else if (tag==METHODREF) System.out.print("MethodRef"); else System.out.print("InterfaceMethodRef"); }
		System.out.print("  class => cp"+u2);
		System.out.println("  n&t => cp"+u2);
		break;
	case NAMEANDTYPE:
		o=cp[cf.readu2()].getString(cp, cf)+" "+cp[cf.readu2()].getString(cp, cf);
		System.out.print("NameAndType");
		System.out.print("  name => cp"+u2);
		System.out.println("  descriptor => cp"+u2);
		break;
	}
  }*/
}
