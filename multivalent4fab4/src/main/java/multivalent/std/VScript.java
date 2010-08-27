package multivalent.std;

import java.util.StringTokenizer;
import java.util.Map;
import java.awt.AWTEvent;

import phelps.lang.Booleans;

import multivalent.Document;
import multivalent.Node;
import multivalent.Multivalent;



/*
	Expression evaluator: String => parse tree => value
		make extensible so, say, a spreadsheet can augment with cell references and financial formulae

	language definition - should look at JavaScript, I guess, though it's awful
	semicolon separates commands
	event <name> [<arg>] - sends semantic event


// () +-* /  numbers
*/


/**
	Micro scripting language.
	Statement evaluator: mini scripting language that can tickle Preferences, Browser attributes, and local (node or behavior) attributes.
	Not fast, but fast enough; not powerful, but powerful enough.
	Rather than growing into a real language, it will be replaced if necessary, or alternatives will be available.
	Like Tcl, as far as it goes.


	<p>Syntax
	<ul>
	<li>body = cmd; cmd; ...
	<li>cmd = fn args<BR>
	<li>fn = get | set | event<BR>
	<li>args = literal | $variable<BR>
	<li>literal = String<BR>
	<li>variable = [namespace.]field<BR>
	<li>namespace = pref | doc
	</ul>


	<p>Namespaces
	<ul>
	<li>no namespace => local attrs (passed in call)
	<li>pref => preferences
	<li>doc => current document attrs
	</ul>


	<p>Commands (return value of script is return value of last command)
	<ul>
	<li>set (literal | variable) => return value of (literal | variable)
	<li>$(variable) => shorthand for above
	<li>set (literal | variable) (literal | variable) => eval first pair to get name, set to value of eval of second pair
	<li>event <name> <arg> => trigger semantic event
	<!--<li><object>.<method>-->
	</ul>

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:30:36 $
*/
public class VScript {
  static final boolean DEBUG = false;

  public static final String[] commands = { "eval" /*, "expr"*/, "get", "set", "event" };

  // possible pure String=>value.  If create instance, caches parse tree?  What interface to parse tree so can change vars and re-eval (incrementally)
  // maybe in the future make instances with relevant namespaces/environment
  private VScript() {}

  public static String getVal(String name, Document doc, Map<String,Object> locals, String seed) {
	String val = getVal(name,doc,locals);
	if (val.equals("") && name!=null && name.startsWith("$")) {
		val = seed;
		putVal(name.substring(1), seed, doc, locals);
	}
	return val;
  }

  public static boolean getBoolean(String name, Document doc, Map<String,Object> locals, String seed) {
//System.out.println("getBoolean: "+name+" => "+sval);
	return Booleans.parseBoolean(getVal(name,doc,locals, seed), false);
  }

  public static String getVal(String name, Document doc, Map<String,Object> locals) {
	if (name==null || name.length()==0) return "";

	int inx = name.indexOf('$');
	if (inx==-1) return name;	// literal
	if (inx==0 && name.indexOf('$',1)==-1) return getVar(name.substring(1), doc, locals);	// fast path for common case

	// embedded
	StringBuffer sb = new StringBuffer(Math.max(name.length(),100));
	int nlen = name.length();
	int lastinx=0;
	for ( ; inx!=-1; lastinx=inx, inx=name.indexOf('$',inx)) {
		if (inx>lastinx) sb.append(name.substring(lastinx,inx));	// piece up to var
		inx++; lastinx=inx;	// embedded var
		for (inx++; inx<nlen; inx++) {
			char ch=name.charAt(inx);
			if (ch!='.' && !Character.isLetter(ch)) break;
		}
		if (inx>lastinx) sb.append(getVar(name.substring(lastinx,inx), doc, locals));
	}
	if (lastinx<name.length()) sb.append(name.substring(lastinx));	// piece after all vars

	return sb.substring(0);
  }

  public static String getVar(String name, Document doc, Map<String,Object> locals) {
	String ns = null;
	String val;
	int inx=name.indexOf('.');
	if (inx!=-1) { ns=name.substring(0,inx); name=name.substring(inx+1); }
	if ("pref".equals(ns)) val=Multivalent.getInstance().getPreference(name, null);
	else if ("doc".equals(ns)) val=doc.getAttr(name);
	else val = (String)locals.get(name);

	return val!=null? val: "";
  }

  public static String putVal(String name, String val, Document doc, Map<String,Object> locals) {
	String ns = null;
	int inx=name.indexOf('.');
	if (inx!=-1) { ns=name.substring(0,inx); name=name.substring(inx+1); }

	if (val==null || val.length()==0) {
		// removeAttr
		if ("pref".equals(ns)) Multivalent.getInstance().removePreference(name);
		else if ("doc".equals(ns)) doc.removeAttr(name);
		else locals.remove(name);
	} else {
		val = getVal(val, doc, locals);
		if ("pref".equals(ns)) Multivalent.getInstance().putPreference(name, val);
		else if ("doc".equals(ns)) doc.putAttr(name, val);
		else locals.put(name, val);
	}

	return val;
  }

  /** Evaluate an expression, returning result in a String. */
  public static String eval(Object expr, Document doc, Map<String,Object> locals, Node node) {
	if (expr==null) return null;
	String retval = null;

	if (expr instanceof AWTEvent) {
		doc.getRoot().getBrowser().eventq((AWTEvent)expr);
		return null;
	}

	String[] args = new String[10];
	int argcnt = 0;
	StringTokenizer cmds = new StringTokenizer((String)expr, ";\n");	// handle null expr?
	while (cmds.hasMoreTokens()) {
		StringTokenizer tokens = new StringTokenizer(cmds.nextToken(), " \t");
		String cmd = tokens.nextToken().toLowerCase().intern();
		for (argcnt=0; tokens.hasMoreTokens(); argcnt++) args[argcnt]=getVal(tokens.nextToken(), doc, locals);

//System.out.print("script cmd="+cmd); for (int i=0; i<argcnt; i++) System.out.print(" arg="+args[i]); System.out.println();
		if ("get"==cmd || ("set"==cmd && argcnt==1)) {
			// assert(argcnt==1)
			retval = getVar(args[0], doc, locals);
		} else if ("set"==cmd) {
			// assert(argcnt==2)
			retval = putVal(args[0], args[1], doc, locals);
		} else if ("event"==cmd) {
			// assert(argcnt==1 || argcnt==2)
			Object cd = (argcnt==1? null: args[1]);
			if ("<node>".equals(cd)) cd=node;
if (DEBUG) System.out.println("VScript eval:  semanticevent "+args[0]+" "+cd);
			doc.getRoot().getBrowser().eventq(args[0], cd);
			retval = null;
		} // else "unrecognized command"
	}
	return retval;
  }



/*
	char[] nestch = new char[50];	// nestable: ""[]{}
	int[] nestposn = new int[50];
	int nesti=0;
	String[] args = new String[10];
	int argsi=0;

	for (int argstart=0,cmdi,cmdmax=expr.length(); cmdi<=cmdmax; cmdi++) {
		// find end of command, taking into consideration nesting and multiple commands separated by semicolon (or return?)
		char c = (cmdi<cmdmax? expr.charAt(cmdi): ';');	// flushes final command

		if (nesti==-1 && (c==' ' || c=='\t')) {
			// whitespace separating args
			if (cmdi>argstart) args[argsi++] = eval(expr.substring(argstart,cmdi));
			argstart=cmdi+1;

			for (cmdi++; cmdi<cmdmax && ((c=expr.charAt(cmdi))==' ' || c=='\t'); cmdi++) {}
			argstart=cmdi;
		} else if (nesti==-1 && c==';') {
			// eval

		} else if (c==']' && nesti>=0 && nestch[nesti-1]=='[') {
			// subeval
			args[argsi++] = eval(expr.substring(nestposn[--nesti]+1,cmdi));
			argstart=cmdi+1;
		} else if (c=='"' && nesti>=0 && nestch[nesti-1]=='"') {
			// part of a string
			args[argsi++] = expr.substring(nestposn[--nesti]+1,cmdi);
			argstart=cmdi+1;
//		  } else if (c=='}' && nesti>=0 && nest[nesti]=='}') {
//			  // just grouping
		} else {	// ordinary char

		}
	}
*/
}
