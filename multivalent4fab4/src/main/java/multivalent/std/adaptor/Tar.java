package multivalent.std.adaptor;

import java.io.IOException;
import java.io.InputStream;

import multivalent.INode;
import multivalent.MediaAdaptor;
import phelps.util.Dates;
import phelps.util.Units;

import com.pt.io.InputUni;



/**
	Show contents of UNIX tape archive (Tar) files, with links so can click and view internal file.

	@version $Revision: 1.3 $ $Date: 2005/01/01 12:47:38 $
*/
public class Tar extends MediaAdaptor {
  private static final int BLOCKSIZE = 512;


  public Object parse(INode parent) throws Exception {
	//X br.eventq(TableSort.MSG_ASCENDING, doc.findDFS("File")); => keep archive order
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }

  public String toHTML() throws IOException {
	long now = System.currentTimeMillis();

	InputUni iu = getInputUni();

	// make an HTML table -- make HTML table nodes public
	StringBuffer sb = new StringBuffer((int)Math.max(iu.length(), 1024));

	sb.append("<html>\n<head>");
	//sb.append("\t<title>").append("Contents of tar file ").append(urifile).append("</title>\n");
	sb.append("\t<base href='").append(getURI()).append("/'>\n");	// .zip as if directory!
	sb.append("</head>\n");
	sb.append("<body>\n");

	int filesi=sb.length(), filecnt=0;
	sb.append(" files, ");
	int sizei=sb.length(); long sizec=0;
	//<h3>Contents of zip file ").append(urifile).append("</h3>\n"); => apparent in URI entry
	//zhsb.append("<p>").append(dirlist.length).append(" file"); if (dirlist.length!=1) hsb.append('s');
	sb.append("\n<table width='90%'>\n");

	// headers.  click to sort
	sb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'	title='Sort table'>");
	sb.append("<th align='left'>File / <b>Directory<th align='right'>Size<th align='right'>Last Modified</b></span>\n");

	// collect header fields => should go in getCatalog, which should take an InputStream
	byte[] buf = new byte[BLOCKSIZE];
	InputStream is = iu.getInputStream();
	for (int c; is.read(buf)==BLOCKSIZE; filecnt++) {
		if (buf[0]==0) continue;  // break?
		String name=null; for (int i=0; i<100; i++) if (buf[i]==0) { name=new String(buf, 0, i); break; }
System.out.println("name = |"+name+"|");
		if (name == null)
			continue;
		sb.append("<tr><td>");
		boolean dir = name.endsWith("/");
		if (dir) sb.append("<b>").append(name).append("</b>"); else sb.append(name);
		sb.append("\n");

		int size=0;
		for (int i=124; i<136; i++) {	// size in OCTAL!
			c=buf[i]; if (c>='0' && c<='7') size = size*8 + c-'0'; else if (c!=' ') break;
		}
		sb.append("<td align='right'>").append(size);
		sizec += size;
//System.out.println("size = "+size);
//			sb.append("<td><span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));

		long lastmod=0;
		for (int i=136; i<148; i++) {
			c=buf[i]; if (c>='0' && c<='7') lastmod = lastmod*8 + c-'0'; else if (c!=' ') break;
		}
		lastmod *= 1000;	// sec=>ms
//System.out.println("lastmod = "+lastmod+", rel="+Dates.relative(lastmod, now)+", rel*1000="+Dates.relative(lastmod*1000, now));
		sb.append("<td align='right'><span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));


		// skip data
		int toeat = size;
		while (toeat>0) toeat -= is.skip(BLOCKSIZE);
//break;
	}

	sb.insert(sizei, Units.prettySize(sizec));
	sb.insert(filesi, filecnt);
	sb.append("</table>\n</body></html>\n");

	is.close();

	return sb.toString();
  }
}
