package multivalent.std.adaptor;

import java.io.*;
import java.net.URI;
import java.util.Iterator;

import phelps.util.Units;
import phelps.util.Dates;

import com.pt.io.InputUni;
import com.pt.io.FileSystemRPM;
import com.pt.io.FSFileRPM;

import multivalent.*;



/**
	Media adaptor for RPM files.

	@version $Revision: 1.4 $ $Date: 2005/01/01 12:42:54 $
*/
public class RPM extends MediaAdaptor {
  private static final boolean DEBUG = false;


  //public void buildBefore(Document doc) {throw new ParseException("Use the class java.util.ZipFile to parse", -1);}
  public Object parse(INode parent) throws Exception {
	//X br.eventq(TableSort.MSG_ASCENDING, doc.findDFS("File")); => keep archive order
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }


  public String toHTML() throws IOException {
	URI uri = getURI();
	long now = System.currentTimeMillis();

	StringBuffer sb = new StringBuffer(5000);

	sb.append("<html>\n<head>");
	sb.append("\t<title>").append("RPM ").append(uri.getPath()).append("</title>\n");
	sb.append("\t<base href='").append(uri).append("/'>\n");	// .zip as if directory!
	sb.append("</head>\n");
	sb.append("<body>\n");

	//int filecnt = files.size();
	//sb.append(filecnt).append(" file").append(filecnt>0?"s":"").append(", ");
	int sizei = sb.length(); long sizec=0;

	//<h3>Contents of zip file ").append(urifile).append("</h3>\n"); => apparent in URI entry
	//zhsb.append("<p>").append(dirlist.length).append(" file"); if (dirlist.length!=1) hsb.append('s');
	sb.append("\n<table width='90%'>\n");

	// headers.  click to sort
	sb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'	title='Sort table'>");
	sb.append("<th align='left'>File / <b>Directory<th align='right'>Size<th align='right'>Last Modified</b></span>\n");

	FileSystemRPM rpm = new FileSystemRPM(getInputUni());
	for (Iterator i = rpm.iterator(); i.hasNext(); ) {
		FSFileRPM f = (FSFileRPM)i.next();
		String name = f.getPath();

		sb.append("<tr><td>");
		if (f.isDirectory()) sb.append("<b>").append(name).append("</b>"); else sb.append(name);
		sb.append("\n");

		long size = f.length();
		sb.append("<td align='right'>").append(size);
		sizec += size;
//System.out.println("size = "+size);
//		sb.append("<td><span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));

		long lastmod = f.lastModified();
//System.out.println("lastmod = "+lastmod+", rel="+Dates.relative(lastmod, now)+", rel*1000="+Dates.relative(lastmod*1000, now));
		sb.append("<td align='right'><span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));
	}

	sb.insert(sizei, Units.prettySize(sizec));
	sb.append("</table>\n</body></html>\n");

	return sb.toString();
  }
}
