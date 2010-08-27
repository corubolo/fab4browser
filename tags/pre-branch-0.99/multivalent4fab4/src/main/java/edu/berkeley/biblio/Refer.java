package edu.berkeley.biblio;

import java.util.*;

import multivalent.Behavior;
import multivalent.Node;
import multivalent.INode;


/**
	UNIX <a href='manualpage:refer(1)'>refer</a>-style output format.

	@version $Revision$ $Date$
*/
public class Refer extends Concrete {
  static final String attrsRefer[] = { "L", null, "T", "A", "J", "D", "D", "V", "P", null, "X" };


  public Object getTitle(int id) { return "Biblio: Refer"; }

  public String translate(INode bibinfo) {
	String attrsBib[] = null;	//JB  biblio.getAttributes();
	StringBuffer sb = new StringBuffer(200);
	String name, val;
	boolean gotdate=false;

	// split authors on "and", emit multiple "A"
	for (int i=2; i<attrsBib.length; i++) {
	  if ((name=attrsRefer[i])!=null && (val=(String)bibinfo.getAttr(attrsBib[i]))!=null) {
		if (name.equals("A")) {
		  int andIndex=0;
		  for (int ix=val.indexOf(" and "); ix!=-1; andIndex=ix+5, ix=val.indexOf(" and ",andIndex)) {
			sb.append("%A "); sb.append(val.substring(andIndex, ix)); sb.append('\n');
		  }
		  // last one, andIndex..end
		  sb.append("%A "); sb.append(val.substring(andIndex));

		} else if (name.equals("D")) {
		  if (gotdate) continue;
		  // synthesize "D"=date from month and year
		  String month = bibinfo.getAttr("month");
		  String year = bibinfo.getAttr("year");
		  if (month!=null || year!=null) {
			sb.append("%D ");
			if (month!=null) { sb.append(month); if (year!=null) sb.append(", "); }
			if (year!=null) sb.append(year);
		  }
		  gotdate=true;

		} else if (name.equals("P")) {
		  sb.append("%P "); sb.append(val);
		  if ((val=bibinfo.getAttr("pageend"))!=null) {
			sb.append('-'); sb.append(val);
		  }

		} else {
		  sb.append('%'); sb.append(name); sb.append(' '); sb.append(val);
		}
		sb.append('\n');
	  }
	}

	return sb.substring(0);
  }
}
