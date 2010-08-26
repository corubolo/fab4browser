/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.adaptor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.Iterator;

import com.pt.io.InputUni;

import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Behavior;
//import javax.xml.parsers.DocumentBuilder;



/**
	An ever-closer approximation to the XML specification, which itself is a moving target.
	Handles "xinclude:include".
	Note: This may be replaced with Sun's XML Parser to pick up namespaces and all the little points of the spec.

	<p>XML is used as the storage format for hubs and annotations.

<!--
	<p>NOTES
	profile--string handling may be really inefficient
	need to abstract readChar in XML to read entity references (&amp;)
	find out and follow XML attribute parsing behavior (e.g., ">" legal in quotes?)
	for now all tags must be well nested
	need to change from static to instance operation because documents can add ENTITY different definitions

	test by parsing test file with weird tag spacings, attributes, attributes without values

	<p>need to examine DOCTYPE and pass on control, e.g., svg => SVG media adaptor

// shares ELEMENT, ENTITY, ATTRIBUTE with SGML
-->

	@version $Revision: 1.2 $ $Date: 2008/09/08 09:15:29 $
*/
public class XML extends ML {
  static final boolean DEBUG = false;

  // should be a parameter from application
  static String[] specialattr = { "NAME"/*shouldn't have this*/, "Behavior", "URI", "CreatedAt", "ACTIVE", "Foreground", "Background", "X", "Y", "Width", "Height", "Posted" };
  static String[] specialattrlc;
  static {
	specialattrlc = new String[specialattr.length];
	for (int i=0,imax=specialattr.length; i<imax; i++) specialattrlc[i] = specialattr[i].toLowerCase();
  }

   private static String nl_ = System.getProperties().getProperty("line.separator");


  // more methods to grab content and build tree (approach parser for well behaived XML: no syntax errors, tags fully instantiated, no syntax mods in DTD)
  // later, read DTD so can validate

  @Override
public Object parse(INode parent) throws Exception {
	throw new UnsupportedOperationException("native XML display in a future version");
	//return new LeafUnicode(,null, parent);
  }

  /**
	XML can be also be treated not as a media adaptor but as an XML parsing utility.
	For general XML parsing instead use {@link javax.xml.parsers.SAXParser} or {@link javax.xml.parsers.DocumentBuilder}.
  */
  public static ESISNode parseDOM(URI uri) throws IOException {
	XML xml = (XML)Behavior.getInstance("xml","XML"/*getName()?*/,null, null);
//System.out.println("parseDOM XML "+uri+" => "+URIs.toURL(uri)+" / "+new java.net.URL(uri.toString()));
	xml.setInput(InputUni.getInstance(uri, null/*, getGlobal().getCache()*/, null));
	ESISNode xmln = xml.parse();

	return xmln;
  }

 public ESISNode parse() throws IOException {
	ESISNode[] stack = new ESISNode[100];
	ESISNode metatop = new ESISNode("xml");
	int stacki = 0;
	stack[stacki] = metatop;	// strip before returning;
//System.out.println("parsing XML");

	getReader();
	eatSpace();	// suck up opening whitespace
	while (true) {
		eatComment();
		// invariant: at start of a tag

		char ch = readChar();
		if (ch==-1) {
			ir_.close();
			return (metatop.size()==1? stack[1]: stack[0]);

		} else if (ch=='<') {
//if (tagsb.length()>0) System.out.println("getTag seeded with "+tagsb.substring(0));
		  ESISNode t = getTag();
//System.out.println("XML tag "+t);
		  //char type = (!t.open? '/': t.getGI().charAt(0));
		  switch (t.ptype) {
		  case '!':
			// special.  for now assume it's a comment
			//assert false: "shouldn't see any comments (for now)";
			// could be DOCTYPE (others?)
			//System.out.println("special: "+t.getGI());
			stack[stacki].appendChild(t);
			break;

		  case '?':	// "must pass to application"
			stack[stacki].appendChild(t);
			break;

		  case '/':
//System.out.println(t.getGI());
			if (!pairsWith(stack[stacki],t)) { System.out.println("mismatched tags "+stack[stacki].getGI()+" with "+t+" @ line "/*+linenum*/); errcnt++; }
			stacki--;
			if (stacki==0) {
//System.out.println("DUMPING XML, root childcnt="+stack[0].size()); stack[0].dump();
//				if (stack[0].size()==1) return stack[1]; else return stack[0];
				ir_.close();
				return stack[1];
			}
			break;
		  default:
//System.out.println(t);
			// open tag, recurse
			//ESISNode t = new ESISNode(t);
			//ESISNode t = new ESISNode(t.getGI(), t.attrs);

			if (/*ns.equals("xmlns") && */ "xinclude:include".equals(t.getGI()) && t.getAttr("href")!=null) {
//System.out.println("*** trying to include: "+docURI+" + "+t.getAttr("href"));
				try {
					// recursively read href
					URI uri = getURI().resolve(t.getAttr("href"));

//System.out.println("***2 trying to include: "+docURI+" + "+t.getAttr("href"));
					ESISNode xmln = parseDOM(uri);

					// transfer children to current parse (strip outermost tag)
					// can included XML be forest (sequence of trees)?  if so, need following; else never return metatop and just have p.appendChild()
					ESISNode p = stack[stacki];
					if (xmln.getGI()=="metatop") {
						for (int i=0,imax=xmln.size(); i<imax; i++) {
							Object o = xmln.childAt(i);
//if (o instanceof ESISNode) System.out.println("splicing "+((ESISNode)o).getGI());
							//p.appendChild(xmln.childAt(0));	// always first child
							if (o instanceof ESISNode) p.appendChild(xmln.childAt(i));	// LATER: preserve originating namespace
						}
					} else p.appendChild(xmln);
					//p.appendChild(xmln);

				} catch (MalformedURLException skip) {
					System.err.println("COULD NOT xinclude:include on "+t.getAttr("href"));
				}
			} else {
				stack[stacki].appendChild(t);
				//stack[++stacki] = t;
				//if (t.empty) stacki--;
				if (!t.empty) stack[++stacki] = t;
			}
		  }
		} else {
		  ir_.unread(ch);	// restore first character

		  // check that first child is an ESISNode (not content)
		  String content = readString(); if (ispace) content = " "+content;
//System.out.println("content = "+ch+"/"+content);
//		  validate(subtree!=null, "shouldn't begin document with content: "+content);
		  // maybe make content an ESIS node of specific type
		  if (content!=null) stack[stacki].appendChild(content);
		  if (DEBUG) System.out.println("content: "+content);
		}
	}

	//return null; -- not reached
  }

  /**
	Write opening tag with generic identifier <var>name</var> and attributes <var>attrs</var>
	into {@link StringBuffer} <var>sb</var>.  Indent according to nesting <var>level</var>.
	Does not write a final ">" so client can add more attributes, close tag and add content, or write as empty ("... />").
  */
  public static void write(String name, Map<String,Object> attrs, StringBuffer sb, int level) {
	indent(level,sb);

	sb.append("<").append(name); //o.print(" Behavior="+instance.getClass().getName()+source of class on network);

	int cnt=0;
	if (attrs!=null) {	// when interating through behaviors, have at least "Behavior" attribute, but for general purpose...
		// special attributes go first
		for (int i=0,imax=specialattr.length; i<imax; i++) {	// can do this in general as don't guarantee an order
			String attrname = specialattrlc[i], attrval = (String)attrs.get(attrname);//getAttr(attrname);
			if (attrval!=null) {
				sb.append("  ").append(specialattr[i]);	// keep case
				if (!attrval.equals(attrname)) { sb.append('='); encode(attrval, sb, true); }
			}
		}
		if (cnt>0) { sb.append(nl_); cnt=0; }

		// other attributes
		for (Iterator<Map.Entry<String,Object>> i=attrs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,Object> e = i.next();
			String attrname = e.getKey();

			boolean special=false;	// put specials into Hashtable?
			for (int j=0,jmax=specialattr.length; j<jmax; j++) if (specialattrlc[j].equals(attrname)) { special=true; break; }
			if (special) continue;

			if (cnt>0 && cnt%3==0) { sb.append(nl_); indent(level,sb); }	// 0,3,6,9,..., so three on every line

			sb.append("  ").append(attrname);

			String attrval = (String)e.getValue();	// always non-null -- can't Hashtable.put("name", null);
			if (!attrval.equals(attrname)) { sb.append('='); encode(attrval, sb, true); }
			cnt++;
		}
	}
	//sb.append(" />"); => don't know if empty
  }


  /**
	Encode characters from <var>str</var> as entity references (e.g., "<" => "&lt;") and write to <var>sb</var>,
	optionally surrounded by quotes (single quotes if no embedded single quote, else double quotes).
  */
  public static void encode(String str, StringBuffer sb, boolean q) {
	boolean dq = q && str.indexOf('\'')!=-1;
	if (q) sb.append(dq? '"': '\'');

	// => should work from table of entity refs
	for (int i=0,imax=str.length(); i<imax; i++) {
		char ch = str.charAt(i);
		if (ch=='"' && dq) sb.append("&quot;");
		else if (ch=='<') sb.append("&lt;");
		else if (ch=='>') sb.append("&gt;");	// doesn't screw up parsing if left in
		else if (ch=='&') sb.append("&amp;");
		else sb.append(ch);
	}

	if (q) sb.append(dq? '"': '\'');
  }

  /** Write out attribute names in this table first, in this order.  Should be a parameter. */
  public static void indent(int level, StringBuffer sb) { for (int i=0; i<level; i++) sb.append("   "); }
}
