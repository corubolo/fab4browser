package phelps.lang.reflect;

import java.util.List;
import java.util.ArrayList;



/**
	@version $Revision: 1.2 $ $Date: 2003/06/01 07:59:28 $
*/
public class Method_info {
  public static final int ACC_PUBLIC=0x0001, ACC_PRIVATE=0x0002, ACC_PROTECTED=0x0004, ACC_STATIC=0x0008,
	ACC_FINAL=0x0010, ACC_SYNCHRONIZED=0x0020, ACC_NATIVE=0x00100, ACC_ABSTRACT=0x0400, ACC_STRICT=0x0800;

  public short access_flags;
  public String name; //	 u2 name_index;
  public String descriptor; //	u2 descriptor_index;
  //u2 attributes_count; => implicit in array length
  public Attribute_info[] attributes;

  ClassFile ret;
  ClassFile[] params;
  int acnt = 0;

  public ClassFile getReturnType() {
	String type = descriptor.substring(0,descriptor.indexOf('('));
	return ClassFile.forName(type);
  }

  public ClassFile[] getParameterTypes() {
	List<ClassFile> l = new ArrayList<ClassFile>(10);

	int acnt=0;
	String type = descriptor.substring(descriptor.indexOf('(')+1, descriptor.lastIndexOf(')'));
	for (int i=0,imax=type.length(), starti=0; i<imax; i++) {

		char ch = type.charAt(i);
		if (ch=='L') {
			for (i++; type.charAt(i)!=';' && i<imax; i++) {}
		}

		if (ch!='[') {
			l.add(ClassFile.forName(type.substring(starti, i + (ch=='L'? 0: 1))));
			starti = i + 1;
		}
	}

	ClassFile[] ret = new ClassFile[l.size()];
	l.toArray(ret);
	return ret;
  }

  Method_info(Cp_info[] cp, ClassFile cf) {
	access_flags = cf.readu2();
	name = cp[cf.readu2()].getString(cp, cf);
	descriptor = cp[cf.readu2()].getString(cp, cf);
	attributes = new Attribute_info[cf.readu2()];
	for (int i=0,imax=attributes.length; i<imax; i++) attributes[i]=cf.getAttributeInfo(cp);
  }

  public String access2String() { return access2String(access_flags); }

  public static String access2String(int access) {
	if (access==0) return " ";
	StringBuffer sb = new StringBuffer(25);
	if ((access&ACC_PUBLIC)!=0) sb.append("public ");
	if ((access&ACC_PRIVATE)!=0) sb.append("private ");
	if ((access&ACC_PROTECTED)!=0) sb.append("protected ");
	if ((access&ACC_STATIC)!=0) sb.append("static ");
	if ((access&ACC_FINAL)!=0) sb.append("final ");
	if ((access&ACC_SYNCHRONIZED)!=0) sb.append("synchronized ");
	if ((access&ACC_NATIVE)!=0) sb.append("native ");
	if ((access&ACC_ABSTRACT)!=0) sb.append("abstract ");
	if ((access&ACC_STRICT)!=0) sb.append("strict ");
	return sb.substring(0);
  }


  public String toString() {
	int inx = descriptor.lastIndexOf(')');
	String parms=descriptor.substring(1,inx), rettype=descriptor.substring(inx+1);
	return access2String(access_flags)+ClassFile.type2String(rettype)+" "+name+" ("+ClassFile.type2String(parms)+')';
	//return name+" "+descriptor;
  }
}
