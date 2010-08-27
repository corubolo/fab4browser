package multivalent.devel;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.Modifier;

import phelps.io.FileFilterPattern;
import phelps.lang.reflect.*;

import com.pt.io.FileList;


// definitions for old protocol method signatures
/*
import multivalent.Context;
import multivalent.Node;
import multivalent.INode;
import multivalent.Document;
import multivalent.ESISNode;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.AWTEvent;
*/


/**
	Check for likely coding problems.

	<ul>
	<li>Behaviors are public, with at least null constructor (others may or may not be ok).
		The system uses <code>Class.forName(<var>name</var>).newInstance() to instantiate behaviors,
		which requires them to be public and with a null consbtructor.
	<li>Protocol methods have right signature (methods with protocol names match corresponding in {@link multivalent.Behavior}).
		Overloading might be ok, but those methods won't be invoked by the system.
<!--
	<li>behavior remappings to subclass
	<li>protocols methods invoke their superclass's (if superclass method not empty) -- trace through bytecode, which is why we use our own reflection classes
	<li>use of obsolete code, like CHashMap (which has been replaced by Namespace)
	<li>if needed: usage of old protocols and reports replacement signature
	<li>LATER: check that always using buffered input, methods that start with capital letter and other points of Java style
-->
	</ul>

<!--
	<li>maybe eventually in some other tool, modify class files to
	<ul>
	<li>handle DEBUG flags by zapping that code
	<li>modifies source to import exactly those classes used (if > n from same package, import *)
	<li>variable shadowing ... other general Java checks
	</ul>
-->


	<h2>How to run</h2>

	<blockquote>
	<code>java -classpath .../Multivalent.jar devel.Check [<var>options</var>] <var>class-or-JAR-or-directory...</var></code>
	</blockquote>
	where <var>options</var>
	<ul>
	<li>-census <var>file</var> - write HTML report to <var>file</var>
	</ul>


	@version $Revision: 1.9 $ $Date: 2003/06/02 05:12:18 $
*/
public class Check {
  public static final String USAGE = "java devel.Check <class-or-directory...>";
  public static final String VERSION = "1.0";


  static String[] protonames = {
	"restore", "save", "buildBefore", "buildAfter", "formatBefore", "formatAfter",
	"paintBefore", "paintAfter", "eventBefore", "eventAfter", "clipboardBefore", "clipboardAfter",
	"undo", "redo", "destroy",
	"checkRep"
  };
  static ClassFile[][] protosigs = new ClassFile[protonames.length][];

  static String[] nprotonames = {
	"formatBeforeAfter", "formatNode", "paintBeforeAfter", "paintNode",
	"clipboardBeforeAfter", "clipboardNode", "eventBeforeAfter", "eventNode",
	"checkRep"
  };
  static ClassFile[][] nprotosigs = new ClassFile[nprotonames.length][];


  /* * LEVEL_ERROR almost definitely wrong. */
  static final int LEVEL_ERROR=0;
  /* * LEVEL_WARNING probably wrong (e.g., override protocol without calling superclass, if it's non-null). */
  static final int LEVEL_WARNING=1;
  /* * LEVEL_PEDANTIC probably ok (e.g., w/o calling null superclass). */
  static final int LEVEL_PEDANTIC=2;
  /* * LEVEL_VERBOSE extra info (e.g., which protocols overridden by which behaviors, which behaviors are Spans, ...). */
  static final int LEVEL_VERBOSE=4;
  /* * LEVEL_DISTRIB make JAR: check that code is optimized (no debug tables). */
  static final int LEVEL_DISTRIB=5;



  List<ClassFile> classes_;
	//List/*of Class*/ Behaviors = new ArrayList<Behavior>(100);
  boolean[] shownote = new boolean[10/*NOTES.length*/];
  int level = LEVEL_DISTRIB;	// while testing, LEVEL_PEDANTIC in production


  public Check(FileList files) {
	long start = System.currentTimeMillis();
	classes_ = collect(files);
	long done = System.currentTimeMillis();
	System.out.println("class collection took "+(done-start)+" ms");

	if (ClassFile.forName("multivalent.Behavior") == null) {    // need for reference, but don't put in report (unless reporting on core)
		// compute JAR path
		//if (cf == null) { System.err.println("must include Multivalent.jar"); System.exit(1); }
		//if (vfiles!=null) collect(vfile); // populate class pool with multivalent.Behavior et cetera => do automatically if multivalent.Behavior not found
	}

	if (classes_.size()==0) System.err.println("no classes");
	//System.out.println("name = "+getClass().getName()); => dots
// multivalent.std.adaptor.pdf.FixedLeafShade extends multivalent.Leaf
  }

  /**
	Collect all files in iteration over classes and JARs.
  */
  List<ClassFile> collect(FileList files) {
	List<ClassFile> l = new ArrayList<ClassFile>(200);

	for (Iterator<File> i = files.breadthFirstIterator(); i.hasNext(); ) {
		File f = i.next();
		String tail = f.getName();

		try {
			if (tail.endsWith(".jar")) collectJar(f, l);
			else collectClassFile(new ClassFile(f), l);
		} catch (IOException ioe) { System.out.println("can't load "+f+": "+ioe); }
	}

	return l;
  }

  private void collectJar(File file, List<ClassFile> l) throws IOException {
	ZipFile zip = new ZipFile(file);
	for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
		ZipEntry z = (ZipEntry)e.nextElement();
		if (!z.getName().endsWith(".class")) continue;

		InputStream zis = zip.getInputStream(z);
		try { collectClassFile(new ClassFile(zis), l);
		} catch (IOException ioe) { System.out.println("can't load "+file+" / "+z.getName());
		} finally { zis.close(); }
	}
	zip.close();
  }

  private void collectClassFile(ClassFile cf, List<ClassFile> l) {
	String name = cf.getName();
	if (name.indexOf('$')!=-1) return;  // no inner classes
	//if (ClassFile.forName(name) != null) return;    // no dups

	l.add(cf);
	ClassFile.putPool(cf);
	//System.out.println(cf.getName()+" extends "+cf.getSuperclass());
  }




  boolean warning(boolean test, String warnmsg) { return warning(test,warnmsg,-1); }
  boolean warning(boolean test, String warnmsg, int notenum) {
	if (!test && level>=LEVEL_WARNING) {
		System.out.print("\tWARNING: "+warnmsg);
		if (notenum>=0) { System.out.print("  (note "+notenum+")"); shownote[notenum]=true; }
		System.out.println();
	}
	return test;
  }
  boolean error(boolean test, String errmsg) { return error(test,errmsg,-1); }
  boolean error(boolean test, String errmsg, int notenum) {
	if (!test && level>=LEVEL_ERROR) {
		System.out.print("\tERROR: "+errmsg);
		if (notenum>=0) { System.out.print("  (note "+notenum+")"); shownote[notenum]=true; }
		System.out.println();
	}
	return test;
  }
  void selfcheck(boolean test, String errmsg) { if (!test) System.out.println("SELFCHECK: "+errmsg); }
  //void ASSERT(boolean test, String errmsg) { if (!test) LEVEL_ERROR(errmsg); }
  //void LEVEL_ERROR(String errmsg) { System.err.println(errmsg); System.exit(1); }

  void dump(Class c) {
//	System.out.println(c.getName()+" "+c.getClasses().length+"/"+c.getDeclaredClasses().length+", methods "+c.getMethods().length+"/"+c.getDeclaredMethods().length);
	System.out.println(c.getName()+" "+c.getClasses().length+"/"+c.getDeclaredClasses().length+", methods "+c.getMethods().length+"/"+c.getDeclaredMethods().length);
  }


  void check() {
	shownote[0]=true;	// general notes

	// set up class Behavior
	final ClassFile BE = ClassFile.forName("multivalent.Behavior"); assert BE != null;
	Arrays.sort(protonames);
	Method_info[] methods = BE.getDeclaredMethods();
	for (int i=0,imax=methods.length; i<imax; i++) {
		Method_info m = methods[i];
		int inx = Arrays.binarySearch(protonames, m.name);
		if (inx>=0) {
			//assert inx>=0: "multivalent.Behavior does not define protocol "+m.name;
			assert protosigs[inx]==null: "multivalent.Behavior overloads "+m.name;
			protosigs[inx] = m.getParameterTypes();
		}
	}
	for (int i=0,imax=protonames.length; i<imax; i++) assert protosigs[i]!=null: "protocol "+protonames[i]+" not defined as method";

	final ClassFile NODE = ClassFile.forName("multivalent.Node"); assert NODE != null;
	Arrays.sort(nprotonames);
	methods = NODE.getDeclaredMethods();
	for (int i=0,imax=methods.length; i<imax; i++) {
		Method_info m = methods[i];
		int inx = Arrays.binarySearch(nprotonames, m.name);
		if (inx>=0) {
			//assert inx>=0: "multivalent.Behavior does not define protocol "+m.name;
			assert nprotosigs[inx]==null: "multivalent.Node overloads "+m.name;
			nprotosigs[inx] = m.getParameterTypes();
		}
	}
	for (int i=0,imax=nprotonames.length; i<imax; i++) assert nprotosigs[i]!=null: "protocol "+nprotonames[i]+" not defined as method";




	// check all behaviors and nodes
	for (int i=0,imax=classes_.size(); i<imax; i++) {
		ClassFile cf = classes_.get(i);
		//if (cf.getName().indexOf('$')!=-1) continue; -- do inner classes too
		// doesn't matter if private or abstract as not getting an instance

		checkClass(cf);
		if (BE.isAssignableFrom(cf)) checkBehavior(cf);
		else if (NODE.isAssignableFrom(cf)) checkNode(cf);
		//break;}
	}



	// remappings to subclass(?)



	// are non-behaviors checked at all? just random Java code I think
  }

  //int MSG_XXX = 1; -- tickle check below

  void checkClass(ClassFile checkme) {
	Field_info[] fields = checkme.fields;
	for (int i=0,imax=fields.length; i<imax; i++) {
		Field_info fi = fields[i];

		// semantic events, wherever they are defined
		if (fi.name.startsWith("MSG_")) {
//System.out.println(checkme.getName()+" "+fi.name+" "+fi.descriptor+"  "+fi.access_flags);
			if ((fi.access_flags & fi.ACC_PUBLIC) == 0
				|| (fi.access_flags & fi.ACC_STATIC) == 0
				|| (fi.access_flags & fi.ACC_FINAL) == 0
				|| !"Ljava/lang/String;".equals(fi.descriptor)) {
				warning(false, checkme.getName()+" "+fi.name+" not 'public static final String'");
			}
		}
	}
  }


  void checkBehavior(ClassFile checkme) {
	System.out.println("behavior "+checkme.getName());

	// classes are public, with null constructor (others may or may not be ok)
	int mods = checkme.getModifiers();
	error(Modifier.isAbstract(mods) || Modifier.isPublic(mods), "class must be public", 1);

	// if has null constructor, it's gotta be public as Java doesn't allow overrides to be less accessible
	Method_info[] methods = checkme.getDeclaredMethods();   // BCLASS.getDeclaredMethods()
	boolean fgotnull = false, fgotnonnull = false;
	for (int i=0,imax=methods.length; i<imax; i++) {
		Method_info m = methods[i];
		if ("<init>".equals(m.name)) {
			if ("()V".equals(m.descriptor)) { fgotnull = true; }
			else { fgotnonnull=true; warning(false, "don't define non-null constructors on behaviors", 1); }
		}
	}
	if (fgotnonnull && !fgotnull) {}

	checkProto(methods, protonames, protosigs);
  }


  void checkNode(ClassFile checkme) {
	System.out.println("node "+checkme.getName());

/*
	// classes are public, with null constructor (others may or may not be ok)
	int mods = checkme.getModifiers();
	error(Modifier.isAbstract(mods) || Modifier.isPublic(mods), "class must be public", 1);
*/

	// if has null constructor, it's gotta be public as Java doesn't allow overrides to be less accessible
	Method_info[] methods = checkme.getDeclaredMethods();   // BCLASS.getDeclaredMethods()
/*	boolean fgotnull = false, fgotnonnull = false;
	for (int i=0,imax=methods.length; i<imax; i++) {
		Method_info m = methods[i];
		if ("<init>".equals(m.name)) {
			if ("()V".equals(m.descriptor)) { fgotnull = true; }
			else { fgotnonnull=true; warning(false, "don't define non-null constructors on behaviors", 1); }
		}
	}
	if (fgotnonnull && !fgotnull) {}
*/
	checkProto(methods, nprotonames, nprotosigs);

  }


	// protocol methods have right signature (methods with protocol names match corresponding in class)
	// guaranteed to have same return type by Java language spec
  // used by checkBehavior and checkNode
  void checkProto(Method_info[] methods, String[] pnames, ClassFile[][] psigs) {
	for (int i=0,imax=methods.length; i<imax; i++) {
		Method_info m = methods[i];
		String mname = m.name;
		int inx = Arrays.binarySearch(pnames, mname);
		if (inx>=0) {
			boolean overload=false;
			ClassFile[] sig = m.getParameterTypes();
			ClassFile[] protosig = psigs[inx];
			overload = (sig.length!=protosig.length);
			for (int j=0,jmax=sig.length; j<jmax && !overload; j++) overload = !(protosig[j].isAssignableFrom(sig[j]));
//if (overload) /*System.out.println(mname+" "+sig.length+" -- OK"); else*/ System.out.println("BOOM");
			if (overload) {
				String msg = "overloaded protocol " + mname + "(" + m.descriptor + ") -- system won't call it";
				warning(!overload, msg, 2);	// might be intensional
			}

			// if overrides protocol && level>=LEVEL_VERBOSE, report it
		}
	}
  }




  // put subclasses ahead of superclasses to prevent miscategorization
  static final String[] census = {
	"multivalent.Span", "multivalent.std.lens.Lens", "multivalent.MediaAdaptor", "multivalent.Behavior",    // behaviors
	"multivalent.Node", /** separate out Leaf and INode? */  // nodes
	// ...
  };
  static final String[] Census = { "Spans", "Lenses", "Media Adaptors", "Other Behaviors", "Nodes" };


  ClassFile[] gcf_;
  List[] group_;
  List<String> seml_;

  void inspect() {
	int glen = census.length;
	gcf_ = new ClassFile[glen]; for (int i=0; i<glen; i++) gcf_[i] = ClassFile.forName(census[i]);
	group_ = new List[glen]; for (int i=0; i<glen; i++) group_[i] = new ArrayList<ClassFile>(100);


	// 1. collect information
	seml_ = new ArrayList<String>(200);
	for (int i=0,imax=classes_.size(); i<imax; i++) {
		ClassFile cf = classes_.get(i);
		String cname = cf.getName();

		// census: behaviors, media adaptors, spans, lenses, ...
		for (int j=0; j<glen; j++) {
			if (cf.isAssignableFrom(gcf_[j])) {
				// if (cf != ccf[j]) -- I guess count, e.g., multivalent.Span as a multivalent.Span, not as multivalent.Behavior
				group_[j].add(cf);
				break;
			}
		}


		// MSG_ -- not just on Behaviors(?)
		List<String> msgl = new ArrayList<String>(10);
		for (int j=0,jmax=cf.fields.length; j<jmax; j++) {
			Field_info fi = cf.fields[j];
			String fname = fi.name;
			if (fname.startsWith("MSG_")) msgl.add(fname);
		}
		if (msgl.size() > 0) { seml_.add(cname); seml_.addAll(msgl); }
	}

  }

  static final Comparator<ClassFile> CFC = new Comparator<ClassFile>() {
	public int compare(ClassFile cf1, ClassFile cf2) {
		String s1 = cf1.getName(), s2 = cf2.getName();
		int x = s1.lastIndexOf('.'); if (x!=-1) s1 = s1.substring(x+1);
		x = s2.lastIndexOf('.'); if (x!=-1) s2 = s2.substring(x+1);
		return String.CASE_INSENSITIVE_ORDER.compare(s1,s2);
	}
  };

  /**
	Report additional information over Javadoc:
	<ul>
<!--
	media adaptors with toHTML()?

	classes that have main()?


	<li>source code stats: # line, # files, # classes (behaviors, media adaptors, spans, lenses, ...)
	<li>for behaviors: which protocols specialized (overridden)?
	<li>subclasses of behavior with methods of same name, but different signature (probably failed to update these)
	<li>maybe lines of code, average size of behaviors, classes over 10K in size (the most important ones?)
-->
	</ul>

	<p>The following information is contained in the Javadoc API:
	<ul>
	<li>groups of behaviors / spans / lenses / nodes / other classes &rArr; hierarchy in overview page
	<li>list of semantic events and other constants &rArr; constant values link by each constant
	<li>deprecated classes
	</ul>

	The report is in HTML, so you probably want to redirect the output to a file and read it with a browser.
  */
  void report(PrintStream out) {
	inspect();

	out.println("<html>\n<body>\n");

	for (int i=0,imax=census.length; i<imax; i++) {
		List<ClassFile> gl = group_[i];
		if (gl.size() == 0) continue;
		Collections.sort(gl, CFC);

		out.println("\n\n<h3>" + Census[i] + " ("+gl.size()+")</h3>");
		for (int j=0,jmax=gl.size(); j<jmax; j++) {
			ClassFile cf = gl.get(j);
			if (j>0) out.println(", ");

			String s = cf.getName();
			int x = s.lastIndexOf('.');
			out.print("<a href='"+s.replace('.','/')+"'>" + (x!=-1? s.substring(x+1): s) + "</a>");
		}
	}


	if (seml_.size() > 0) {
		out.println("\n\n<h2>Semantic Events</h2>\n");
		out.println("<ul>\n");
		String href = null;
		for (int i=0,imax=seml_.size(), mcnt=0; i<imax; i++) {
			String s = seml_.get(i);
			if (s.startsWith("MSG_")) {
				if (mcnt>0) out.print(", ");
				out.print("<a href='"+href+"#"+s+"'>" + s.substring("MSG_".length()) + "</a>");
				mcnt++;

			} else {
				if (href!=null) out.println();
				href = s.replace('.','/');
				mcnt = 0;
				out.println("<li>"+s+":");
			}
		}
		out.println("</ul>\n");
	}


		out.println("");
		out.println("");

		out.println("");


	out.println("</body>\n</html>");
  }



  public static void main(String[] argv) {
	PrintStream out = System.out;

	// collect command-line arguments
	int argi = 0, argc = argv.length;
	for (String arg; argi<argc && (arg = argv[argi]).startsWith("-"); argi++) {
		if (!arg.startsWith("-out")) {
			try { out = new PrintStream(new FileOutputStream(argv[++argi])); }
			catch (FileNotFoundException fnfe) { System.err.println("can't write to "+argv[argi]+": "+fnfe); System.exit(1); }
		}

		//else if (arg.startsWith("-verb")) fverbose = true;
		else if (arg.startsWith("-v"/*ersion -- after verbose!*/)) { System.out.println(VERSION); System.exit(0); }
		else if (arg.startsWith("-help")) { System.out.println(USAGE); System.exit(0); }
		else { System.err.println(USAGE); System.exit(1); }
	}


	FileList files = new FileList(argv, argi, new FileFilterPattern("\\.(class|jar)$"));

	Check c = new Check(files);
	c.check();
	//if (freport) c.report(out);

	System.exit(0);
  }
}
