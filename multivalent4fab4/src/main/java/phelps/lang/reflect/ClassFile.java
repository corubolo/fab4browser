package phelps.lang.reflect;

import java.lang.reflect.*;	// cross-check results with built-in reflection, as far as it goes
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;



/**
	<tt>.class</tt> file reflection that reports more complete information than <tt>java.lang.reflect</tt>.

	<blockquote>
	The JavaTM Virtual Machine Specification, Second Edition<br />
	by Tim Lindholm and Frank Yellin
	</blockquote>

	<p>STATUS:	Doesn't read method exception table, inner classes.

	<p>Related work:
	<a href='http://www.cs.colorado.edu/~hanlee/BIT/index.html'>BIT:  Bytecode Instrumenting Tool</a>
	by Han Lee and Ben Zorn.  They determined basic blocks, supported insertion of calls (only; not arbitrary code), and implemented write().

	@see java.lang.reflect

	@version $Revision: 1.3 $ $Date: 2003/06/01 07:58:57 $
*/
public class ClassFile {
  static final boolean VERBOSE=true;

  public static final int MAGIC = 0xcafebabe;

  /** all classes loaded, for tracing subclassing relationships. */
  static Map<String,ClassFile> loaded_ = new HashMap<String,ClassFile>(101);

  static {
	String[] prim = { "byte","B", "short","S", "int","I", "long","J", "float","F", "double","D", "boolean","Z", "void","V", "char","C" };
	for (int i=0,imax=prim.length; i<imax; i+=2) {
		ClassFile cf = new ClassFile(prim[i+1]);
		putPool(cf);
		loaded_.put(prim[i], cf); // second name for same class
	}
  }


  /*package-private*/ byte[] raw_;
  private int offset_=0;

  long readu8() {
	long val=0;
	for (int i=0; i<8; i++) val = (val<<8) | (((int)raw_[offset_++])&0xff);
	//long up = readu(4); return (up<<8) + (readu(4)&0xffffffff);
	return val;
  }
  int readu4() { return readu(4); }
  short readu2() { return (short)readu(2); }
  byte readu1() { return raw_[offset_++]; }
  int readuu1() { return ((int)readu1())&0xff; }
  private int readu(int cnt) {
	int val = 0;
	for (int i=0; i<cnt; i++) {
		//int bval = raw_[offset_++]&0xff;
		//System.out.print("bval = "+bval+"/"+Integer.toHexString(bval));
		//if (bval<0) bval+=128;  // need unsigned types!
		//System.out.println(" => "+Integer.toHexString(bval));
		val = (val<<8)+(((int)raw_[offset_++])&0xff);
	}
	return val;
  }
  int getPosition() { return offset_; }
  void setPosition(int off) { offset_=off; }



  static final int ACC_PUBLIC=0x0001, ACC_FINAL=0x0010, ACC_SUPER=0x0020, ACC_INTERFACE=0x200, ACC_ABSTRACT=0x400;

  //int magic;	// 0xCAFEBEBE
  public short minor_version;	// version of the class file format
  public short major_version;
  //short constant_pool_count; => constant_pool.length(+1)
  public Cp_info[] constant_pool;
  public short access_flags;
  public String this_class;	//u2
  public String super_class;  //u2
  //short interfaces_count; // u2
  public String[] interfaces;  // u2[]
  //short fields_count; => implicit in array length
  public Field_info[] fields;
  //short methods_count; => implicit in array length
  public Method_info[] methods;
  //short attributes_count; => implicit in array length
  public Attribute_info[] attributes;

  // for arrays -- would like to put these fields in a subclass, but isAssignableFrom needs to see (the OO equals() problem)
  public ClassFile component = null;
  public int dimensions = 0;


  // extra computed attributes
  public String pkg;

  // for primitives only
  private ClassFile(String primitive) {
	this_class = primitive;
	super_class = null;
	raw_ = null;
	// other fields null
  }

  /** For arrays. */
  public ClassFile(ClassFile component, int dimensions) {
	this.component = component;
	this.dimensions = dimensions;
  }

  public ClassFile(File f) throws /*ClassNotFoundException,*/ IOException {
	this(phelps.io.Files.toByteArray(f));
	//if (!f.getName().endsWith(".class")) throw new ClassNotFoundException();
  }

  public ClassFile(InputStream is) throws IOException {
	this(phelps.io.InputStreams.toByteArray(is, 8*1024));
  }

  public ClassFile(byte[] bytes) {
	raw_ = bytes;
	load();
  }


  protected void load() {
	offset_=0;
	int magic = readu4();
	if (magic!=MAGIC) throw new ClassFormatError("bad magic number: "+Integer.toHexString(magic));

	minor_version = readu2();
	major_version = readu2();

	constant_pool = new Cp_info[readu2()];
	for (int i=1/*!*/,imax=constant_pool.length; i<imax; i++) {
		//System.out.print(i+"/"+imax+".  ");
		Cp_info cp = constant_pool[i] = new Cp_info(this);
		if (cp.tag==Cp_info.LONG || cp.tag==Cp_info.DOUBLE) i++;	// "In retrospect, making 8-byte constants take two constant pool entries was a poor choice."
	}

	access_flags = readu2();
	this_class = constant_pool[readu2()].getString(constant_pool, this).replace('/', '.');
	int inx = readu2();
	super_class = (inx>0? constant_pool[inx].getString(constant_pool, this): "").replace('/', '.');	// java.lang.Object has no superclass
	// package is implicit in name
	inx = this_class.lastIndexOf('/');
	pkg = (inx!=-1? this_class.substring(0,inx): "");//"(default)");

	interfaces = new String[readu2()];
	for (int i=0,imax=interfaces.length; i<imax; i++) interfaces[i]=constant_pool[readu2()].getString(constant_pool, this).replace('/', '.');

	fields = new Field_info[readu2()];
	for (int i=0,imax=fields.length; i<imax; i++) fields[i]=new Field_info(constant_pool, this);

	methods = new Method_info[readu2()];
	for (int i=0,imax=methods.length; i<imax; i++) methods[i]=new Method_info(constant_pool, this);

	attributes = new Attribute_info[readu2()];
	for (int i=0,imax=attributes.length; i<imax; i++) attributes[i]=getAttributeInfo(constant_pool);
  }


/*
  // should copy arrays
  public String[] getInterfaces() { return interfaces; }
  public Field_info[] getFields() { return fields; }
  public Method_info[] getMethods() { return methods; }
  public Attribute_info[] getAttributes() { return attributes; }
  public Cp_info[] getConstantPool() { return constant_pool; }
*/
  // same as java.lang.Class
  public int getModifiers() { return access_flags; }
  public String getSuperclass() { return super_class; }
  public String getName() { return this_class; }
  public String getPackage() { return pkg; }
  public Method_info[] getDeclaredMethods() { return methods; }


  public static String access2String(int access) {
	if (access==0) return "class ";
	// maybe worthwhile computing 2^5=32 combinations statically
	StringBuffer sb = new StringBuffer(100);
	if ((access&ACC_PUBLIC)!=0) sb.append("public ");
	if ((access&ACC_FINAL)!=0) sb.append("final ");
	//if ((access&ACC_SUPER)!=0) sb.append("super "); -- treat superclass methods specially: invokespecial
	if ((access&ACC_INTERFACE)!=0) sb.append("interface "); else sb.append("class ");
	if ((access&ACC_ABSTRACT)!=0) sb.append("abstract ");
	return sb.substring(0);
  }

  public static String type2String(String type) {
	StringBuffer sb = new StringBuffer(50);
	int acnt = 0;
	for (int i=0,imax=type.length(); i<imax; i++) {
		char ch = type.charAt(i);
		String subtype=null;
		switch (ch) {
		case 'B': subtype="byte"; break;
		case 'C': subtype="char"; break;
		case 'D': subtype="double"; break;
		case 'F': subtype="float"; break;
		case 'I': subtype="int"; break;
		case 'J': subtype="long"; break;
		case 'S': subtype="short"; break;
		case 'Z': subtype="boolean"; break;
		case 'V': subtype="void"; break;
		case 'L':
			for (i++; (ch=type.charAt(i))!=';'; i++) sb.append(ch);
			sb.append(' ');
			break;
		case '[':
			acnt++;
			break;
		default: assert false: ch;
		}
		if (subtype!=null) sb.append(subtype).append(' ');
	}
	for (int i=0; i<acnt; i++) sb.append("[]");
	return sb.substring(0);
  }

  /** Don't know sizes of arrays and Objects statically. */
  public static int sizeof(String type) {
	int size=0;
	//for (int i=0,imax=type.length(); i<imax; i++) {
		char ch = type.charAt(0);
		switch (ch) {
		case 'B': size+=1; break;
		case 'C': size+=8; break;
		case 'D': size+=0; break;	// what is this?
		case 'F': size+=4; break;
		case 'I': size+=4; break;
		case 'J': size+=8; break;	// can have long long
		case 'S': size+=2; break;
		case 'Z': size+=1; break;
		case 'V': break;
		case 'L':
			size=4;
			//for (i++; i<imax; i++) if (type.charAt(i)==';') break;
			break;
		case '[': size=4; break;
		default: assert false: ch;
		}
	//}
	return size;
  }

  Attribute_info getAttributeInfo(Cp_info[] cp) {
	// common to all attributes
	String name = cp[readu2()].getString(cp, this);
	int len = readu4();

	Attribute_info ai;
	/// return specific type
	/// class.forName...
	if ("Code".equals(name)) ai = new Code_attribute(cp, this, name, offset_);
	else if ("SourceFile".equals(name)) ai = new SourceFile_attribute(cp, this, name, offset_);
	else if ("LocalVariableTable".equals(name)) ai = new LocalVariableTable_attribute(cp, this, name, offset_);
	else if ("Deprecated".equals(name)) ai = new Deprecated_attribute(cp, name, offset_);
	else {
		ai=new Attribute_info(cp,name, offset_);
		offset_ += len;	// skip
	}
	return ai;
  }

  public Attribute_info getAttribute(Attribute_info[] ai, String name) {
	for (int i=0,imax=ai.length; i<imax; i++) {
		if (ai[i].attribute_name.equals(name)) return ai[i];
	}
	return null;
  }


  public String toString() {
	return access2String(getModifiers())+getName()+" extends "+getSuperclass();
/*	String info = "";
	info += ", v"+major_version+"."+minor_version;
	info += ", cpc="+constant_pool.length;
	return info;*/
  }


  /** Someday, write out new classfile, after possible modification. */
  /*
  public void write(OutputStream out) {
  }*/

  /**
	Returns true if this class is a subclass of <var>superclass</var>.
	Only knows about classes added with {@link #putPool(ClassFile)}.
  */
  public boolean isAssignableFrom(ClassFile subclass) {
	assert subclass!=null;

	// arrays
	if (isArray()) return dimensions == subclass.dimensions? component.isAssignableFrom(subclass.component): false;


	String name = getName();

//System.out.println(getName()+" superclass of "+subclass.getName());
	boolean is = subclass.getName().equals(name);
	for (ClassFile cf = subclass; !is && cf!=null; ) {
		String s = cf.getSuperclass();
		if (s.equals(name)) is = true;
		else cf = loaded_.get(s);
	}

	return is;
  }


  public boolean isArray() { return dimensions > 0; }
  public boolean isPrimitive() { return raw_==null && component==null; }

  public static ClassFile forName(String name) {
	assert name!=null;

	ClassFile cf = loaded_.get(name);

	// LATER: if not in pool, load.  for now, stub out
	if (cf == null) {
		int acnt = 0;
		for (int i=0,imax=name.length(); i<imax; i++) if (name.charAt(i)=='[') acnt++; else break;
		if (acnt == 0) cf = new ClassFile(name);
		else {
			ClassFile component = loaded_.get(name.substring(acnt));
			if (component != null) {
				cf = new ClassFile(component, acnt);
			} else {
				cf = new ClassFile(name);
				cf.dimensions = acnt;
			}
			putPool(cf);
		}
	}

	return cf;
  }

  /** Add ClassFile to pool of known classes, for better isAssignableFrom(). */
  public static void putPool(ClassFile cf) {
	assert cf!=null;
	loaded_.put(cf.getName(), cf);
  }
}
