package multivalent.std.adaptor;

import java.io.*;
import java.net.URI;
import java.util.Iterator;

import phelps.util.Units;
import phelps.util.Dates;

import com.pt.io.InputUni;
import com.pt.io.FileSystemZip;
import com.pt.io.FSFileZip;

import multivalent.*;



/**
	Show contents of Zip files, with links so can click and view internal file.

	@version $Revision: 1.4 $ $Date: 2005/01/06 04:01:27 $
*/
public class Zip extends MediaAdaptor {
  public Object parse(INode parent) throws Exception {
	//X br.eventq(TableSort.MSG_ASCENDING, doc.findDFS("File")); => retain natural order
	String html; try { html = toHTML(); } catch (IOException ioe) { html = ioe.toString(); }
	return parseHelper(html, "HTML", getLayer(), parent);
  }


  public String toHTML() throws IOException {
	FileSystemZip zip = new FileSystemZip(getInputUni());

	URI uri = getURI();
	long now = System.currentTimeMillis();
	// make an HTML table -- make HTML table nodes public

	StringBuffer sb = new StringBuffer(5000);
	sb.append("<html>\n<head>");
	sb.append("\t<title>").append("Contents of zip file ").append(uri.getPath()).append("</title>\n");
	sb.append("\t<base href='").append(uri).append("/'>\n");
	sb.append("</head>\n");
	sb.append("<body>\n");

	int filesi=sb.length(), filecnt=0;
	sb.append(" files, ");
	int sizei=sb.length(); long sizeun=0, sizec=0;
	//<h3>Contents of zip file ").append(urifile).append("</h3>\n"); => apparent in URI entry
	//zhsb.append("<p>").append(dirlist.length).append(" file"); if (dirlist.length!=1) hsb.append('s');
	sb.append("\n<table width='90%'>\n");

	// headers.  click to sort
	sb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'  title='Sort table'>");
	sb.append("<th align='left'>File / <b>Directory<th align='right'>Compressed<th align='right'>Size<th align='right'>Method<th align='right'>Last Modified</b></span>\n");

	// element list -- not necessarily sorted any particular way
	for (Iterator/*<FSFileZip>*/ i = zip.iterator(); i.hasNext(); ) {
		FSFileZip f = (FSFileZip)i.next();
		filecnt++;
		String name = f.getPath();	// should use casify, but only bad old MS-DOS would benefit
//System.out.println(name+" "+ze.getTime()+" "+ze.getCompressedSize()+"/"+ze.getSize()+" "+ze.getExtra());

		sb.append("<tr>");
		// icon
		//hsb.append("<td><img src='systemresource:/images/").append(isDir?"file_dir":"file").append(".xbm'>");
		// filename and size
		if (f.isDirectory()) {
			sb.append("<td><b>").append(name).append("</b></td>");
			sb.append("<td align='right'><span Behavior='ElideSpan'>0</span> --<td align='right'><span Behavior='ElideSpan'>0</span> --");
		} else {
			sb.append("<td><a href='").append(name).append("'>").append(name).append("</a>"); 
			sb.append("<td align='right'>").append(Long.toString(f.lengthCompressed())).append("<td align='right'>").append(Long.toString(f.length()));
			sizec += f.lengthCompressed(); sizeun += f.length();
		}
		// compression
		sb.append("<td align='right'>").append(f.getMethod()==FSFileZip.METHOD_DEFLATED? "deflated":"STORED");
		// last mod
		long lastmod = f.lastModified();
		sb.append("<td align='right'><span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));
		// comment
		if (f.getComment()!=null) sb.append("<td>").append(f.getComment());

		//ze.getComment(); ze.getSize(); ze.getCompressedSize(); ze.getMethod(); name; ze.getTime(); ze.getExtra();
	}
	sb.insert(sizei, Units.prettySize(sizec)+" / "+Units.prettySize(sizeun));
	sb.insert(filesi, filecnt);
	sb.append("</table>\n</body></html>\n");

	zip.close();

	return sb.toString();
  }
}
