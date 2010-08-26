package multivalent.std.adaptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import multivalent.INode;
import multivalent.MediaAdaptor;
import phelps.lang.reflect.Attribute_info;
import phelps.lang.reflect.ClassFile;
import phelps.lang.reflect.Code_attribute;
import phelps.lang.reflect.Cp_info;
import phelps.lang.reflect.Field_info;
import phelps.lang.reflect.Method_info;
import phelps.lang.reflect.SourceFile_attribute;



/**
	Media adaptor for displaying information about Java <tt>.class</tt> files.
	Doesn't handle inner classes, yet.

<!--
	To do: class disassembly.
-->

	@version $Revision: 1.3 $ $Date: 2002/10/14 11:46:49 $
*/
public class JavaClass extends MediaAdaptor {
  static final boolean DEBUG = false;


  /**
	Could use Properties System.getProperties().get("java.class.path"), but Java's built-in classes are in a JAR.
  */
  protected File base_ = null;
  public void linkClass(String classname, StringBuffer sb) {
	if (base_==null || (!classname.startsWith("java/") && new File(base_, classname+".class").exists())) {
		sb.append("<a href='").append(classname).append(".class'>").append(classname).append("</a>");
	} else sb.append(classname);
  }

//  public void buildBefore(Document doc) {}
//	throw new ParseException("Use the class phelps.reflect.ClassFile to parse", -1);
  public Object parse(INode parent) throws Exception {
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }

  public String toHTML() throws IOException {
	ClassFile cf=null;
	InputStream is = getInputUni().getInputStream();
	URI uri = getURI();
	try { cf = new ClassFile(is);
	} catch (ClassFormatError cfe) { return "Not a Java class (bad magic number): "+cfe;
	} catch (IOException ioe) { return "IOException reading "+uri+": "+ioe;
	}

	String urifile = uri.getPath();
	String pkg = cf.getPackage();
	String basef = urifile.substring(0, urifile.lastIndexOf('/') - pkg.length());
	base_ = ("file".equals(uri.getScheme())? new File(basef): null);


	// make an HTML table -- make HTML table nodes public
	StringBuffer sb = new StringBuffer(30000);

	sb.append("<html>\n<head>");
	sb.append("<style type=\"text/css\">\n" + 
			"body {\n" + 
			"	color: black; background-color: white;\n" + 
			"	font-size: 14pts;	/* Mozilla: 16 for proportional, 13 for fixed */\n" + 
			"	padding: 10px;}\n" +
			"\n" + 
			"a:link { color: blue; }\n" + 
			"a:visited { color: magenta; }\n" + 
			"a:hover { color: red; }\n" + 
			"a:active { color: red; }\n" + 
			"\n" + 
			"a:link, a:visited, \n" + 
			"a:active, a:hover {\n" + 
			"	text-decoration: underline;\n" + 
			"}\n" + 
			"\n" + 
			"p {\n" + 
			"	margin-top: 10px;\n" + 
			"}\n" +
			"text { padding: 5px; }\n" + 
			"pre { font-family: monospace; }\n" + 
			"h1 { font-size: 24pt; font-weight: bold; margin: 10px 0px; }\n" + 
			"h2 { font-size: 18pt; font-weight: bold; margin: 9px 0px; }\n" + 
			"h3 { font-size: 14pt; font-weight: bold; margin: 7px 0px; }\n" + 
			"h4 { font-size: 12pt; font-weight: bold; margin: 6px 0px; }\n" + 
			"h5 { font-size: 10pt; font-weight: bold; margin: 5px 0px; }\n" + 
			"h6 { font-size:  9pt; font-weight: bold; margin: 5px 0px; }\n" + 
	"</style>");
	sb.append("\t<title>").append(urifile).append("</title>\n");
	URI baseuri = uri;
	try { baseuri = baseuri.resolve(basef); } catch (IllegalArgumentException male) {}
	sb.append("\t<base href='").append(baseuri).append("'>\n");
	sb.append("</head>\n");
	sb.append("<body>\n");

	//sb.append("package ").append(pkg_);
	sb.append(ClassFile.access2String(cf.getModifiers())).append('\n');
	sb.append("<font size='+1'><b>").append(cf.getName()).append("</b></font><br>\n");
	sb.append("extends "); linkClass(cf.getSuperclass(), sb);

	// INTERFACES implemented
	//String[] ia = cf.getInterfaces();
	String[] ia = cf.interfaces;
	if (ia.length > 0) {
		sb.append("<br>implements ");
		for (int i=0,imax=ia.length; i<imax; i++) {
			if (i>0) sb.append(", ");
			linkClass(ia[i], sb);
		}
	}


	// CLASS FILE INTERNALS -- make optional
	sb.append("\n\n\n<h3>Class file</h3>\n");
	//Cp_info[] cp = cf.getConstantPool();
	Cp_info[] cp = cf.constant_pool;
	sb.append("class file format version ").append(cf.major_version).append('.').append(cf.minor_version);
	sb.append(", constant pool: ").append(cp.length).append(cp.length>0?" entries":"entry");
	//Attribute_info[] aa = cf.getAttributes();
	Attribute_info[] aa = cf.attributes;
	if (aa.length > 0) {
		sb.append("<p>Attributes\n");
		for (int i=0,imax=aa.length; i<imax; i++) {
			sb.append(i>0?"<br>":"");
			if ("SourceFile".equals(aa[i].attribute_name)) {
				SourceFile_attribute sfa = (SourceFile_attribute)aa[i];
//System.out.println("source code = "+sfa.source);
				sb.append("<a href='file:").append(sfa.source).append("'>source code</a>");
			} else sb.append(aa[i]).append("\n");
		}
	}


	// FIELDS
	//Field_info[] fa = cf.getFields();
	Field_info[] fa = cf.fields;
	if (fa.length > 0) {
		sb.append("\n\n\n<h3>Fields</h3>\n");
		//sb.append(fa.length+" fields");

		int size=0, ssize=0;
		for (int i=0,imax=fa.length; i<imax; i++) {
			int fs = cf.sizeof(fa[i].descriptor);
			if ((fa[i].access_flags & Field_info.ACC_STATIC)==0) size+=fs; else ssize+=fs;
		}
		sb.append(fa.length).append(" field").append(fa.length>1?"s":"");
		sb.append(", size = ").append(size).append(" byte").append(size>1?"s":"").append(" per instance");
		if (ssize>0) sb.append(" + >=").append(ssize).append(" static");

		for (int i=0,imax=fa.length; i<imax; i++) {
			Field_info f = fa[i];
			sb.append("<br>").append(f.access2String(f.access_flags)).append(' ');
			type2String(f.descriptor, sb); sb.append(' ');
			sb.append("<b>").append(f.name).append("</b>");
			sb.append('\n');
		}
	}


	// METHODS -- later click in outline to see bytecode
	// table?  type, name w/sig, length
	//Method_info[] ma = cf.getMethods();
	Method_info[] ma = cf.methods;
	if (ma.length > 0) {
		sb.append("\n\n\n<h3>Methods</h3>\n");
/*		sb.append("<table width='90%'>\n");
		sb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'	title='Sort table'>");
		sb.append("<th>Type<th>Name<th>Size</span>\n");*/

		for (int i=0,imax=ma.length; i<imax; i++) {
			Method_info m = ma[i];

			sb.append("<p>");
			sb.append(m.access2String()).append(' ');

			String msig = m.descriptor;
			int inx = msig.lastIndexOf(')');
			String parms=msig.substring(1,inx), rettype=msig.substring(inx+1);

			if ("<init>".equals(m.name)) {
				sb.append("<b><i>constructor</i></b>");
			} else if ("<clinit>".equals(m.name)) {
				sb.append("<b><i>class_init</i></b>");
			} else {
				type2String(rettype, sb);
				sb.append(' ');
				sb.append("<b>").append(m.name).append("</b>");
			}

			sb.append(" (");
			type2String(parms, sb);
			sb.append(")\n");


			Code_attribute ca = (Code_attribute)cf.getAttribute(m.attributes, "Code");
			if (ca!=null) sb.append("<font size='-2'>(offset=").append(ca.code_offset).append(", length=").append(ca.code_length).append(" bytes, max stack=").append(ca.max_stack).append(", max locals=").append(ca.max_locals).append(")</font>");
			for (int j=0,jmax=m.attributes.length; j<jmax; j++) {
				Attribute_info ai = m.attributes[j];
				//if (ai.attribute_name.equals("Code")) ((Code_attribute)ai).disasm(constant_pool, this);
				//else
				if (!"Code".equals(ai.attribute_name)) sb.append("<br>").append(ai);
				//System.out.println("\t"+m.attributes[j]);
			}
			sb.append('\n');
		}
	}

//System.out.println("**** JavaClass sb length = "+sb.length());
//System.out.println(sb.substring(0, Math.min(1500,sb.length())));
	sb.append("\n</body></html>\n");

	return sb.toString();
  }

  /** Inserts links to classes. */
  public void type2String(String type, StringBuffer sb) {
	int acnt=0;
	for (int i=0,imax=type.length(); i<imax; i++) {
		if (i>0 && acnt==0) sb.append(", ");

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
		case '[': acnt++; break;
		case 'L':
			int starti=i+1;
			for (i++; i<imax; i++) {
				if (type.charAt(i)==';') { linkClass(type.substring(starti,i), sb); break; }
			}
			break;
		default: assert false: ch;
		}
		if (subtype!=null) sb.append(subtype);
		if (ch!='[') { for (int j=0; j<acnt; j++) sb.append("[]");  acnt=0; }
	}
  }
}
