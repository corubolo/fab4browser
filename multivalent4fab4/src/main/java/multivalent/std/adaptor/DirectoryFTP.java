package multivalent.std.adaptor;

import java.io.*;
import java.util.regex.*;
import java.net.URI;

import com.pt.io.InputUniURI;

import multivalent.*;



/**
	Media adaptor that displays contents of a FTP directory
	with links to files and other directories.

	@version $Revision: 1.6 $ $Date: 2005/01/01 12:37:24 $
*/
public class DirectoryFTP extends MediaAdaptor {
  /** Pattern for matching file line. */
  public static Pattern REGEXP_LINE = Pattern.compile("^[dl-].........\\s+\\d+\\s+\\d+\\s+(\\d+)\\s+(.+?)\\s+(\\S+( -> \\S+)?)$");

  static Pattern
	//REGEXP_DATE = Pattern.compile(""),
	//REGEXP_TOTAL = Pattern.compile("total\\s+(\\d+)"),
	REGEXP_README = Pattern.compile("(?i)^readme")
	;


  public Object parse(INode parent) throws Exception {
	//new LeafUnicode("FTP directory",null, parent);
	return parseHelper(toHTML(), "HTML", getLayer(), parent);
  }


  public String toHTML() throws IOException {
	StringBuffer sb = new StringBuffer(20*1024);

	URI uri = getURI();
	String suri = uri.toString();
	sb.append("<html>\n<head>\n");
	sb.append("<title>").append(suri).append("</title>\n");
	sb.append("<base href='").append(suri).append("' />\n");
	sb.append("</head>\n<body>\n");
	sb.append("<h3>Index of ").append(suri).append("</h3>\n");    // redundant with URL type-in
	sb.append("<a href='../'><img src='systemresource:/sys/images/Open24.gif'>Up to a higher level directory</a>\n");

	int listi = sb.length();
	sb.append("<hr />\n");

	// parse URI with links on each
	// ...

	String readme = null;
	sb.append("<table width='80%'>");
	sb.append("<tr><th align='left'>Name<th align='right'>Size<th align='right'>Date\n");    // types are obvious but use for table sorting

	BufferedReader r = new BufferedReader(getInputUni().getReader());	// already buffered but want readLine()

	int cnt = 0;
	Matcher m = REGEXP_LINE.matcher(""), rm = REGEXP_README.matcher("");
	for (String line; (line = r.readLine()) != null; ) {
		if (line.length() <= 2) continue;   // blank
		if (line.startsWith("total")) continue; // ignore

		if (m.reset(line).find()) {
			cnt++;
			char type = line.charAt(0);
			// name
			sb.append("<tr><td>");
			String g3 = m.group(3);
			if (type=='d') {
				sb.append("<a href='").append(g3).append("/' >");
				sb.append("<img src='systemresource:/sys/images/Open24.gif'>");
				sb.append(g3).append("</a>");
				sb.append("<td>");  // suppress size

			} else if (type=='l') {
				int inx = line.lastIndexOf(' ');
				String ref = (inx>=0? line.substring(inx+1): line);
				sb.append("<a href='").append(ref).append("'><i>").append(g3).append("</i></a>");
				sb.append("<td>");  // suppress size

			} else {
				sb.append("<a href='").append(g3).append("'>").append(g3).append("</a>");
				if (readme==null && rm.reset(g3).find()) readme = g3;

				// size
				sb.append("<td align='right'>").append(m.group(1));

			}


			// date
			sb.append("<td align='right'>").append(m.group(2));

		} else assert false: "no match on "+line;

	}
	sb.append("</table>\n");
	sb.append("<hr />\n");

	if (cnt == 0) sb.setLength(listi);
	else if (readme!=null) {
		// automatically read and display "README"
		try {
			BufferedReader rr = new BufferedReader(new InputUniURI(uri.resolve(readme), getGlobal().getCache()).getReader());

			StringBuffer sbr = new StringBuffer(2*1024); sbr.append("\n<pre>\n");
			int linecnt = 1;
			for (String line; (line = rr.readLine()) != null; ) {
				if (linecnt <= 10) sbr.append(line).append("\n");
				else { sbr.append("[<a href='").append(readme).append("'>Read the rest</a>]\n"); break; }
				linecnt++;
			}

			sbr.append("\n</pre>\n");

			rr.close();

			sb.insert(listi, sbr.toString());
		} catch (Exception ignore) {}
	}


	r.close();

	sb.append("</body>\n</html>");
	return sb.toString();
  }
}
