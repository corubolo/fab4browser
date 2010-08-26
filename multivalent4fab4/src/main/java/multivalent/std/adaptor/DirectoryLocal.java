package multivalent.std.adaptor;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import multivalent.Browser;
import multivalent.Document;
import multivalent.INode;
import multivalent.MediaAdaptor;
import multivalent.SemanticEvent;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.std.TableSort;
import phelps.io.Files;
import phelps.lang.Booleans;
import phelps.lang.Strings;
import phelps.util.Dates;
import phelps.util.Units;



/**
	MediaAdaptor that displays contents of a local directory.
	Reads directory and generates HTML which is passed on to HTML media adaptor.
	HTML nodes should be publicly available so nodes can be directly created, rather than have a string generated only to be immediately parsed.
	Initial sorting by filename done by throwing a {@link TableSort} event, rather than built in.

	@version $Revision: 1.9 $ $Date: 2005/01/06 06:10:37 $
*/
public class DirectoryLocal extends MediaAdaptor {
  /**
	Toggle between short name-only and full with dates and so on.
	<p><tt>"setDirectoryType"</tt>: <tt>arg=</tt> {@link #SHORT} or {@link #FIELDS} or <code>null</code> to toggle. */
  public static final String MSG_SET_DIRTYPE = "setDirectoryType";

  /**
	Toggle between showing backup files ("...~", "....bkup", ...).
	<p><tt>"setShowBackup"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>boolean</var> or {@link java.lang.Boolean} or <code>null</code> to toggle.
  */
  public static final String MSG_SET_BKUP = "setShowBackup";

  public static final String PREF_TYPE = "Directory/Type";
  public static final String PREF_NOBKUP = "ZapBkup";

  public static final String SHORT = "short", FIELDS = "fields";



  static final int SHORTCOLS = 5;


  //public void buildBefore(Document doc) {
//	throw new java.lang.UnsupportedOperationException();
  public Object parse(INode parent) throws Exception {
	Browser br = getBrowser();
	Document doc = getDocument();
	URI uri = getURI();
//System.out.println("DirectoryLocal URI = "+uri+", file="+uri.getFile());
	String path="";
	if (uri!=null)
	 path = uri.getPath();
	// check validity of file
	if (!path.endsWith("/")) {	// trailing slash important for further resolution
		path += "/";
		try { uri = doc.uri = uri.resolve(path); } catch (IllegalArgumentException canthappen) {}
	}

	Object htmlroot = parseHelper(toHTML(path), "HTML", getLayer(), doc);

	/* RESTORE from new directory sorting behavior
	String stype = getPreference(PREF_SORTBY, FILENAME);
	String header = "Name", order = TableSort.MSG_ASCENDING;
	if (stype.equals(LENGTH)) { header="Size"; order = TableSort.MSG_DESCENDING; }
	else if (stype.equals(LASTMOD)) { header="Last"; order = TableSort.MSG_DESCENDING; }
	*/

	//if (!stype.equals(NONE)) br.event/*no q*/(order, doc.findDFS(header));
	//RESTORE: if (!stype.equals(NONE) && br!=null) br.event/*no q*/(new SemanticEvent(br, order, doc.findDFS(header)));
	// since sort stable, could have secondary sort by... well, name is unique in directory

	return htmlroot;
  }

  public String toHTML(String path) {
	File f = new File(path);
	boolean isRoot = ("/".equals(path));
	//String tail = path.substring(Math.max(0,path.lastIndexOf('/',path.lastIndexOf('/')-1))
	File[] dirlist = isRoot? File.listRoots(): f.listFiles();
	//String tail = f.getName();
//System.out.println("tail: "+path+" => "+tail);
	/*
	String error = null;
	if (!f.exists()) error="doesn't exist";
	else if (!f.isDirectory()) error="isn't a directory";	// shouldn't happen at this point
	else if (!f.canRead()) error="isn't readable";
	if (error!=null) { new LeafUnicode("ERROR: "+error,null, docroot); return; }
	*/

	//long showimgmaxlen = Longs.getLong(getAttr("ShowImageMaxLen"), Long.MAX_VALUE);
	//long showimgmaxlen = 10*1024;

	// build up HTML -- LATER create nodes directly as directories can be long?  I don't think the HTML creation and parsing takes very long.
	StringBuffer hsb = new StringBuffer(10000);	// measure this
	hsb.append("<html>\n<head>");
	hsb.append("\t<title>").append("Directory ").append(path).append("</title>\n");
	//hsb.append("\t<title>").append(tail).append(" - Directory</title>\n");	// better when iconified
	hsb.append("\t<base href='").append(f.toURI()).append("'>\n");
	hsb.append("<style type=\"text/css\">\n" + 
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
	hsb.append("</head>\n");
	hsb.append("<body>\n");
	//.append(path)

	if (isRoot) {
		hsb.append("<h3>Roots:	");
		if (dirlist.length == 1) hsb.append("/ (shown below), ");
		else for (int i=0,imax=dirlist.length; i<imax; i++) hsb.append("<a href='").append(dirlist[i].toURI()).append("'>").append(dirlist[i]).append("</a>, ");

		String homedir=System.getProperty("user.home"), username=System.getProperty("user.name");
		if (homedir!=null && username!=null) {
			//String dir = (homedir.endsWith(username)? homedir: homedir+File.separatorChar+username);
			//try { hsb.append("<a href='").append(new File(dir).toURI()).append("'>HOME").append("</a>, "); } catch (MalformedURLException canthappen) {}
			hsb.append("<a href='~/'>HOME").append("</a>, ");
		}

		String tmpdir = System.getProperty("java.io.tmpdir");
		if (tmpdir!=null) hsb.append("<a href='").append(new File(tmpdir).toURI()).append("'>TMP").append("</a>, ");

		hsb.setLength(hsb.length()-2);
		hsb.append("</h3>");
	}

	if (!isRoot || dirlist.length==1) {
		// text redundant with URI entry, but useful for links to each of higher directories
		hsb.append("<h3>Directory ");
		//if (path.length()>1) {
		if (!isRoot) {
			// up button convenient and in constant place
			hsb.append(" &nbsp; <a href='..'>").append("<img src='systemresource:/sys/images/Up16.gif'>").append("</a> &nbsp; ");

			hsb.append("<a href='file:///'>").append("(roots)").append("</a> ");
			for (int i=path.indexOf('/',1),imax=path.lastIndexOf('/'),lasti=1; i<imax; lasti=i+1, i=path.indexOf('/', i+1)) {
				hsb.append('/').append("<a href='file:").append(path.substring(0,i+1)).append("'>").append(path.substring(lasti,i)).append("</a>");
			}

			String tail = path.substring(Math.max(path.indexOf('/'), path.lastIndexOf('/',path.length()-1-1))+1, path.length()-1);
			hsb.append('/').append(tail);
		}
		hsb.append('/');	// trailing slash -- or root on UNIX
		hsb.append("</h3>\n");

		if (isRoot) dirlist = dirlist[0].listFiles();
		hsb.append("<p>").append(dirlist.length).append(" file"); if (dirlist.length!=1) hsb.append('s');
		int filespos = hsb.length();

		String type = getPreference(PREF_TYPE, FIELDS);
		long filessize = SHORT.equals(type)? buildShort(hsb, f, dirlist): buildFields(hsb, f, dirlist);

		if (filessize > 0) hsb.insert(filespos, ", "+Units.prettySize(filessize)+" in directory");
//System.out.println(hsb.substring(0));
//System.out.println("	 add to "+doc);
	}
	hsb.append("</body></html>\n");

	return hsb.toString();
  }


  /**
	Concise, 5-across list, like UNIX ls, except list across not down so that table sorting works right.
	@return sum of file lengths.
  */
  private long buildShort(StringBuffer hsb, File f, File[] dirlist) {
	hsb.append("<table width='90%' cellpadding=0 cellspacing=0>\n");
	//hsb.append("<tr><td><td><a href='file:").append(f.getParentNode()).append("'>Up to higher level directory</a>");

	String[] tail = new String[dirlist.length];
	for (int i=0,imax=dirlist.length; i<imax; i++) tail[i]=dirlist[i].getName();

	// one-time sort
	Arrays.sort(tail, Strings.DICTIONARY_CASE_INSENSITIVE_ORDER);

//	  int dirlen=dirlist.length, hunk=(dirlen + SHORTCOLS - 1) / SHORTCOLS;
	long filessize=0;
//	for (int i=0,imax=hunk; i<imax; i++) {	-- UNIX ls order interacts badly with slide show
	for (int i=0,imax=dirlist.length; i<imax; i+=SHORTCOLS) {
		hsb.append("<tr>");
//		for (int j=i,jmax=Math.min((j+SHORTCOLS)*hunk, dirlen); j<jmax; j+=hunk) {
		for (int j=i,jmax=Math.min((j+SHORTCOLS), imax); j<jmax; j++) {
			File dirf = dirlist[j];
			boolean isDir = dirf.isDirectory();
			if (!isDir) filessize += dirf.length();
			hsb.append("<td><a href='").append(tail[i]).append(isDir?"/":"").append("'>").append(tail[i]).append(isDir?"/":"").append("</a>");
		}
		hsb.append('\n');
	}
	hsb.append("</table>\n");

	return filessize;
  }


  /**
	LATER: control which fields to show from Preferences, like BeOS.
	@return sum of file lengths.
  */
  private long buildFields(StringBuffer hsb, File f, File[] dirlist) {
	hsb.append("<table width='95%'>\n");
	//hsb.append("<tr><td><td><a href='file:").append(f.getParentNode()).append("'>Up to higher level directory</a>");

	// click on headers to sort by that column.  (Need "Type" so have something to click on.)
	hsb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'  title='Sort table'>");
	hsb.append("<th align='left' width=1><b>Type</b><!--icon--><th align='left'><b>Name</b><th align='right'><b>Size</b><th align='right'><b>Last Modified</b></span></tr>");

	// see lengths as bytes or xxMB
	boolean shouldbepref = true;	// pref so (1) can change, (2) other archive media adaptors can do the same

	// files list
	long now = System.currentTimeMillis();
	long filessize=0;
	boolean fzapbkup = Booleans.parseBoolean(getPreference(PREF_NOBKUP, "true"), true);
	dirlist = (File[])dirlist.clone(); Arrays.sort(dirlist, Files.DICTIONARY_CASE_INSENSITIVE_ORDER);
	for (int i=0,imax=dirlist.length; i<imax; i++) {
		File dirf = dirlist[i];
		String subf = dirf.getName();//, lcf=subf.toLowerCase();
		//URI urif = dirf.toURI();    // need escaping
		//String subf=urif.getPath(), lcf=subf.toLowerCase();
		//String escf = subf;
		//String escf = Utility.UriEncode(subf);     // dirf.toURI().toString()/*subf -- need escaping*/
		String escf = dirf.toURI().toString();  // can't just escape tail because fail on URI.resolve, sigh

		if (fzapbkup && Files.isBackup(subf)/*(subf.endsWith("~") || lcf.endsWith(".bak") || lcf.endsWith(".bkup"))*/) continue;
		boolean isDir = dirf.isDirectory();
		long flen = isDir? 0: dirf.length();
		long lastmod = dirf.lastModified();
		/*if (!isDir)*/ filessize += flen;

		hsb.append("<tr>");
		// icon -- if file is image and small, show it instead (hidden type suffix)
		//if (!isDir && flen<showimgmaxlen && (lcf.endsWith(".png") || lcf.endsWith(".jpg") || lcf.endsWith(".xbm") || lcf.endsWith(".gif"))) hsb.append("<td><img src='"+subf+"'>");
		//else
		int inx = subf.lastIndexOf('.');
		hsb.append("<td><span Behavior='ElideSpan'>").append(inx!=-1? subf.substring(inx+1): "--").append("</span>");
		//hsb.append("<img src='systemresource:/sys/images/").append(isDir?"file_dir":"file").append(".xbm'>");
		//if (isDir) hsb.append("<img src='systemresource:/sys/images/file_dir.xbm'>");
		if (isDir) hsb.append("<img src='systemresource:/sys/images/Open24.gif'>");
		// filename:
		String q = escf.indexOf('\'')==-1? "'": "\"";
		hsb.append("<td><a href=").append(q).append(escf)/*.append(isDir?"/":"")*/.append(q).append(">").append(subf).append("</a>");
		// size -- need rel size in Utility
		hsb.append("<td align='right'>");
		if (isDir || shouldbepref) hsb.append("<span Behavior='ElideSpan'>").append(flen).append("</span> ");
		if (isDir) hsb.append("--"); else hsb.append(shouldbepref? Units.prettySize(flen): Long.toString(flen));
		//hsb.append("<td>").append(isDir?"<span Behavior='ElideSpan'>0</span> --"/*dirf.list().length*/: (shouldbepref? Units.prettySize(flen): Long.toString(flen)));

		// last mod (with hidden millis -- later sort figures out dates)
		hsb.append("<td align='right'>").append("<span Behavior='ElideSpan'>").append(lastmod).append("</span> ").append(Dates.relative(lastmod, now));
		hsb.append('\n');
	}
	hsb.append("</table>\n");
//System.out.println(hsb.substring(0,Math.min(2000,hsb.length())));

	return filessize;
  }


  /** Choose between short and fielded displays. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		String type = getPreference(PREF_TYPE, FIELDS);
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Short Directory Display", "event "+MSG_SET_DIRTYPE, (INode)se.getOut(), VMenu.CATEGORY_MEDIUM, false);
		cb.setState(SHORT.equals(type));

		cb = (VCheckbox)createUI("checkbox", "Don't show backup files", "event "+MSG_SET_BKUP, (INode)se.getOut(), VMenu.CATEGORY_MEDIUM, false);
		cb.setState(Booleans.parseBoolean(getPreference(PREF_NOBKUP, "true"), true));
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	Object arg = se.getArg();
	if (MSG_SET_DIRTYPE==msg) {
		String curtype = getPreference(PREF_TYPE, FIELDS);
		String newtype = (FIELDS.equals(arg)? FIELDS: SHORT.equals(arg)? SHORT: /*toggle*/FIELDS.equals(curtype)? SHORT: FIELDS);
		if (!newtype.equals(curtype)) {
			putPreference(PREF_TYPE, newtype);
			br.eventq(Document.MSG_RELOAD, null);
		}

	} else if (MSG_SET_BKUP==msg) {
		boolean fzap = Booleans.parseBoolean(getPreference(PREF_NOBKUP, "true"), true);
		boolean newzap = Booleans.parseBoolean(arg, !fzap);
		if (newzap != fzap) {
			putPreference(PREF_NOBKUP, !newzap? "true": "false");
			br.eventq(Document.MSG_RELOAD, null);
		}
	}

	return super.semanticEventAfter(se, msg);
  }
}
