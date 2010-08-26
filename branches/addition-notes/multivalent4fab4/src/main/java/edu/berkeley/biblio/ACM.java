package edu.berkeley.biblio;

import java.util.*;

import multivalent.Behavior;
import multivalent.Node;
import multivalent.INode;


/**
	ACM style output format.

	@version $Revision$ $Date$
*/
public class ACM extends Concrete {
  // ACM
  // parallel array to attrsBib giving BibTeX versions of standard names
  // null here if no correspondence to one in attrsBib
  // attrsBib should be union set of all types
/*
<i>How to forget the past without repeating it</i>
Jeffrey F. Naughton and Raghu Ramakrishnan
J. ACM <b>41<b>, 6 (Nov. 1994), pages 1151-1177.
*/
  static final String attrsACM[] = {
	"uid", "Type", "Title", "Author", "Journal", "Month", "Year", "Volume", "Pages", null, "Note"
  };

  public Object getTitle(int id) { return "Biblio: ACM"; }

  public String translate(INode bibinfo) {
	String attrsBib[] = null;		//JB  biblio.getAttributes();
	StringBuffer sb = new StringBuffer(200);
	String name, val;

	sb.append("ACM ref looks like BibTeX for now\n");

	val = bibinfo.getAttr("type");
	sb.append('@'); sb.append(val); sb.append('{');

	val = bibinfo.getAttr("uid");
	if (val!=null) { sb.append(val); sb.append(','); }
	sb.append('\n');

	for (int i=2; i<attrsBib.length; i++) {
	  if ((name=attrsACM[i])!=null && (val=bibinfo.getAttr(attrsBib[i]))!=null) {
		sb.append('\t'); sb.append(name); sb.append(" = ");
		if (name.equals("Pages")) {
		  sb.append(val);
		  if ((val=bibinfo.getAttr("pageend"))!=null) {
			// could have option for shortened refs, e.g., 1280--1297 => 1280--97
			sb.append("--"); sb.append(val);
		  }
		} else {
		  boolean spaces = (val.indexOf(' ')!=-1);
		  if (spaces) sb.append('"');
		  sb.append(val);
		  if (spaces) sb.append('"');
		}
		sb.append(",\n");
	  }
	}

	sb.setLength(sb.length()-2);	// chop off trailing comma
	sb.append("\n}\n");

	return sb.substring(0);
  }
}
