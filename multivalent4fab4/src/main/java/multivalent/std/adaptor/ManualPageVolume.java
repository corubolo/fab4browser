package multivalent.std.adaptor;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.net.URI;

import phelps.io.Files;
import phelps.lang.Booleans;
//import phelps.util.Version;

import com.pt.io.InputUniString;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.gui.VMenu;



/**
	Collect all man pages in user's MANPATH environment variable.
	Big list of all pages read, alphabetized, by volume.

	Use standard system help mechanism, multiple media types (for possible Texinfo)

	<p>Intercepts manpage protocol (e.g., "manpage:ls(1)" or "manpage:ascii.5" or "manpage:sort") to look up in its database

	<p>Principle: Separate UI from media adaptor.

	<p>Discarded ideas:
	<ul>
	<li>Could check for misfiled man pages (e.g., some-page.1 in .../man rather than .../man/man1) and offer to move
	</ul>

	<p>Later, perhaps
	<ul>
	<li>if volume number in text title list, put it there, else fold into parent (single-letter) volume
	</ul>

	@see multivalent.std.adaptor.ManualPage

	@version $Revision: 1.7 $ $Date: 2003/06/02 05:42:14 $
*/
public class ManualPageVolume extends Behavior {
  static final boolean DEBUG = !false;


  //public static final String //DIRMSG = "manpageToggleDirectory", RECENTMSG = "manpageToggleRecent",
  /**
	Determine whether manual page exists in database, in any section.
	<p><tt>"manualpageExists"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>name-of-page</var>
  */
  public static final String MSG_EXISTS = "manualpageExists";

  /**
	Determine whether manual page section / volume letter is valid.
	<p><tt>"manualpageValidSection"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>string-to-check</var> / {@link java.lang.Boolean} <var>returned-answer</var>
  */
  public static final String MSG_SECTION_VALID = "manualpageValidSection";

  /**
	Request database to be rebuilt, to pick up changes made.
	<p><tt>"rebuildManPageDatabaseNewCommand2"</tt>.
  */
  public static final String MSG_DATABASE = "rebuildManPageDatabase";

  /**
	Formatting on today's machines is so fast that there is no need for cached formatted "cat" pages; request to delete them.
	Cat pages can be regenerated if needed, except in the cast of "stray cats" in which there is no source version.
	<p><tt>"manpageDeleteCats"</tt>.
  */
  public static final String MSG_KILLCATS = "manpageKillCats";


  public static final String PROTOCOL = "manualpagevolume";


  /** Boolean indicating whether or not to show file system directories of man page roots. */
  public static final String PREF_DIR = "ManPage/SeeDirectories";

  /** Boolean indicating whether or not to scan for and show recently added (or changed) man pages. */
  public static final String PREF_RECENT = "ManPage/SeeRecent";

  /* List of directories that are man page roots taken from MANPATH environment variable. */
  public static final String ENV_MANPATH = "MANPATH";

  /** List of letters separated by colons corresponding to volume extensions. */
  public static final String PREF_VOLUMES = "MANVOLUMES";

  /** List of volume names separated by colons, parallel to {@link #PREF_RECENT}. */
  public static final String PREF_VOLNAMES = "MANVOLNAMES";


  static final String PATH_CONFIG = "systemresource:/multivalent/std/adaptor/ManualPage-config.html";

  // defaults that can be overridden in Preferences.txt (as MANLETTERS and MANNAMES)... which will have a nice UI soon
  static final String ALLVOL = "ALL";
  static final String VOLLETTERS = "1:2:3:4:5:6:7:8:l:o:n:p:pod";
  static final String VOLNAMES =
	"User Commands:System Calls:Subroutines:Devices:File Formats"+
	":Games:Miscellaneous:System Administration"+
	":Local:Old:New:Public"+	// LATER: recently added/changed (in this list?)
	":Perl";	// Perl could be a user extension, but put it in here.	Useful as test for multi-letter volume names.

  static String[] volletters_=null, volnames_=null;
  static String warnings_ = "";
  /** Keep old MANPATH around so can automatically rebuild database if MANPATH changes. */
  //static String oldMANPATH_ = null;
  static int mancnt_=0;	// number of pages in database
  static String errhtml = null;	// don't exit but report errors => LATER: errors_ parallel to warnings_
  static String[] validpaths_ = null;
  static String[] manpath_ = null;	 // cleaned up MANPATH
  static String[] recent_ = null;
  /** Any cat directories? These are obsolete. */
  private static boolean damncats_ = false;
  /** Is database built/valid? */
  private static boolean fdb_ = false;

  // store database statically (can have multiple instances of ManualPageUI)
  // generate on demand, signalled by null
  static Map<String,Object> Name2dirs_ = null;	// name of page => directory or List<> of directories
  static Map<String,String[]> Vol2names_ = null;	// name of volume => List<> of pages in that volume ... also used for regexp and fuzzy name matches
  static List<String> Dotsdot_ = null;	// list of pages with dot in proper name, such as a.out



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if ("ManualPageVolume".equals(getBrowser().getCurDocument().getAttr(Document.ATTR_GENRE))) {   // always around in order to catch "manpage" protocol
		INode menu = (INode)se.getOut();
		/*if (VMenu.MSG_CREATE_ANNO==msg && errhtml!=null) {
			INode menu = (INode)se.getOut();
			createUI("button", "See Manual Page error log", "event "+Document.MSG_OPEN+" manualPageVolume:errors", menu, "ManualPage", false);
		} else *//*if (VMenu.MSG_CREATE_VIEW==msg) { -- put these in pref page as nobody will see them in View
			VCheckbox dcb = (VCheckbox)createUI("checkbox", "See Man Directories", "event "+DIRMSG, menu, "ManualPage", false);
			dcb.setState(Booleans.parseBoolean(getPreference(PREF_DIR,"true")));
			VCheckbox rcb = (VCheckbox)createUI("checkbox", "Compute Recently Changed", "event "+RECENTMSG, menu, "ManualPage", false);
			rcb.setState(Booleans.parseBoolean(getPreference(PREF_RECENT,"true")));
//not in View menu:		createUI("button", "Rebuild Database", "event "+MSG_DATABASE, menu, "ManualPage", false);
		} else */if (VMenu.MSG_CREATE_EDIT==msg) {
			createUI("button", "Configure Man Pages", "event "+Document.MSG_OPEN+" "+PATH_CONFIG, menu, "configure", false);
		}
	}
	return false;
  }


  /**
	Intercept openDocument with manpage protocol.
	eventAfter to let newcomers filter or intercept.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	Browser br = getBrowser();

	//String openme;
	//if (Document.MSG_OPEN==msg && arg instanceof String && (openme=(String)arg).startsWith("manualPage")) {   //arg instanceof URL && ((URL)arg).getProtocol().equals("manualpage")) {
	if (MSG_DATABASE==msg) {
		buildDatabase();

	} else if (MSG_SECTION_VALID==msg) {
		if (arg instanceof String) {
			char v = ((String)arg).charAt(0);
			boolean valid = false;
			for (int i=0,imax=volletters_.length; i<imax; i++) {
				if (v == volletters_[i].charAt(0)) { valid=true; break; }
			}
			se.setArg(valid? Boolean.TRUE: Boolean.FALSE);
		}

	} else if (MSG_EXISTS==msg) {
		if (!fdb_/*Name2dirs_==null*/) buildDatabase();

		// ManualPage media adaptor asks whether page exists before adding a link
		if (arg instanceof String) {
			String name = (String)arg;
//System.out.println("manualpageExists name in |"+name+"|");
			int inx=name.indexOf('('); if (inx!=-1) name=name.substring(0,inx);
			inx=name.indexOf('.' /*&& Dotsdot_*/); if (inx!=-1) name=name.substring(0,inx);
//System.out.println("	short name |"+name+"| => "+Name2dirs_.get(name));
			se.setArg(Name2dirs_.get(name)!=null? Boolean.TRUE: Boolean.FALSE);
		}

/*	} else if (MSG_DIR==msg) {
		boolean b = Booleans.parseBoolean(getPreference(PREF_DIR, "true"));
		b = !b;
		putPreference(PREF_DIR, (b? "true":"false"));
		br.eventq(Document.MSG_RELOAD, null);

	} else if (MSG_RECENT==msg) {
		boolean b = Booleans.parseBoolean(getPreference(PREF_RECENT, "true"));
		b = !b;
		putPreference(PREF_RECENT, (b? "true":"false"));
		if (b && recent_==null) buildDatabase();
		br.eventq(Document.MSG_RELOAD, null);
*/
	} else if (MSG_KILLCATS==msg) {
		//System.out.println("LATER: delete cat dirs");
		boolean all=true;
		for (int i=0,imax=manpath_.length; i<imax; i++) {
			File dir = new File(manpath_[i]);
			String[] ssubdirs = dir.list();
			for (int j=0,jmax=ssubdirs.length; j<jmax; j++) {
				String ssubdir = ssubdirs[j];
				if (ssubdir.startsWith("cat") && ssubdir.length()>=4) {
					//boolean ok = phelps.Unix.rm(panpath_[i], true, true);
					//if (!ok) all=false;
					System.out.println("would rm -rf "+manpath_[i]+"/"+ssubdir);
				}
			}
		}
		if (all) damncats_ = false;
		br.eventq(Document.MSG_RELOAD, null);	// remove "Delete Cats" message

	} else if (Document.MSG_OPEN==msg) {
		if (arg instanceof DocInfo) {
			DocInfo di = (DocInfo)arg;
			URI uri = di.uri;
			//String file=url.getFile(), lcfile=(file!=null?file.substring(file.charAt(0)=='/'?1:0).toLowerCase():null);
			String protocol= uri.getScheme(), page=uri.getSchemeSpecificPart();
//System.out.println("manpagevol proto="+protocol+", page = |"+page+"|");
			if (protocol!=null && protocol.startsWith("man")) {
//				String page = file.substring(file.indexOf('/')+1);
//System.out.println("*** manual page protocol, page name = |"+page+"|");
				if (!fdb_/*Name2dirs_==null*/ /*|| !getGlobal().getPreference(ENV_MANPATH, "").equals(oldMANPATH_)*/) buildDatabase();	// generate database on demand
				String htmldoc = null;

				if (errhtml!=null) {
					htmldoc = errhtml;
				} else if (PROTOCOL.equals(protocol) || "manpagevol".equals(protocol)) {
					//displayHistory(di.doc, url);	 // want br.getDocRoot() but it's not a Document
					htmldoc = showVolume(page);
				} else {//if ("manualpage".equals(protocols) || "manpage".equals(protocol) || "man".equals(protocol)) {
					htmldoc = showPage(page);
				}

//				if (htmldoc!=null) {

				Document doc = di.doc; //if (doc==null) doc=br.getCurDocument();
				di.returncode = 200;
				//doc.clear();  => kills too much, like "URI"
				doc.removeAllChildren();
				Layer baseLayer = doc.getLayer(Layer.BASE);

				if (htmldoc.indexOf('<')==-1) new LeafUnicode(htmldoc,null, doc);
				else {
					br.event/*no q*/(new SemanticEvent(this, Document.MSG_BUILD, di));
					//MediaAdaptor.parseHelper(htmldoc, "HTML", baseLayer, doc);
					MediaAdaptor html = (MediaAdaptor)Behavior.getInstance("helper","HTML",null, baseLayer);  //doc.getLayer(Layer.BASE));
					try {
						html.setInput(new InputUniString(htmldoc, null, uri, null));
						html.parse(doc);
						doc.putAttr(Document.ATTR_GENRE, "ManualPageVolume");
					} catch (Exception e) {
						new LeafUnicode("ERROR "+e,null, doc);
						e.printStackTrace();
					} finally { try { html.close(); } catch (IOException ioe) {} }

				} //else return true;	// no document to see at this URL so don't add to history lists; event superceded
				//return true; -- NO, want to add to history list, set stylesheet, ...
			}
		}
		//return true;	// short-circuit?
	}
	return false;
  }


  /**
	Writes HTML page that list all pages in specified volume number, or all pages in database for "*".
  */
  public String showVolume(String volnum) {
	boolean allvols = ALLVOL.equals(volnum);
	StringBuffer sb = new StringBuffer((allvols? 10: 2) * 1000);
	sb.append("<html><head>\n");
	sb.append("<title>man page vol ").append(volnum).append("</title>\n");
	sb.append("</head><body>");

	int infoi = sb.length();
	sb.append("<button script='event "+MSG_DATABASE+"; event reloadDocument'>Refresh database</button>");
	//sb.append("<span behavior=ScriptSpan script='event manualpageRebuildDatabase; event openDocument manpagevol:").append(volnum).append("'>Regnerate manual page database</span>");
	//sb.append("<a href='"+PATH_CONFIG+"'>Configure</a>");

	int pagecnt=0;
	boolean firstvol = true;
	for (int i=0,imax=volletters_.length; i<imax; i++) {
		String vol = volletters_[i];
		if (allvols || vol.equals(volnum)) {
			String[] names = Vol2names_.get(vol); if (names==null) continue;
			pagecnt += names.length;
			//sb.append("<span behavior='BoldSpan'>");
			if (allvols) sb.append("<span behavior='OutlineSpan'>");    // ... class='...'
			sb.append("\n<h2>");
			sb.append("<a name='volume").append(vol).append("'>(").append(vol).append(") ").append(volnames_[i]).append("</a>");
			if (allvols) sb.append("  <font size=2><i>").append(names.length).append(firstvol? " pages": "").append("</i></font>");
			sb.append("</h2>\n");
			//sb.append("<small>").append(names.length).append(" page").append(s</small>\n");
			sb.append("<span behavior='ScriptSpan' title='See man page' script='event openDocument manpage:$node(").append(vol).append(")'>\n");
			char lastlet = (char)-1;  //names[0].charAt(0);
			for (int j=0,jmax=names.length; j<jmax; j++) {
				String name = names[j];
				char let = Character.toLowerCase(name.charAt(0));
				if (let!=lastlet) { sb.append("</p>\n<p>"); lastlet=let; } else sb.append(" &nbsp; ");
				sb.append(name);
			}
			sb.append("</p>\n</span>\n");
			if (allvols) sb.append("</span>\n");
			firstvol=false;
		}
	}

	if (mancnt_==0) sb.append("<p>No pages found in "+ENV_MANPATH+" environment variable -- <a href='"+PATH_CONFIG+"'>reset "+ENV_MANPATH+"</a>.");
	else if (pagecnt==0) sb.append("<p>No manual pages in volume <b>").append(volnum).append("</b>.");
	else sb.insert(infoi, pagecnt+" manual pages"+(allvols? "": " in section")+".");

	// this should be controlled by a preference
	if (Booleans.parseBoolean(getPreference(PREF_DIR, "true"), true) && allvols && validpaths_.length > 0) {
		sb.append("<h2>Directories</h2>");	   // kind of like mandesc
		sb.append("[<a href='"+PATH_CONFIG+"'>configure</a>]&nbsp;&nbsp;&nbsp;");
		for (int i=0,imax=validpaths_.length; i<imax; i++) {
			String dir = validpaths_[i]; //String cdir=dir.replace('\\','/');
			File f = new File(dir);
			sb.append("<a href='").append(f.toURI()).append("'>").append(dir).append("</a>, ");
if (DEBUG) System.out.println(dir);
		}
	}

	// this should be controlled by a preference
	if (Booleans.parseBoolean(getPreference(PREF_RECENT, "true"), true) && allvols && recent_!=null && recent_.length > 0) {
		sb.append("<h2>Recent</h2>");
		sb.append("<span behavior='ScriptSpan' title='See man page' script='event "+Document.MSG_OPEN+" manpage:$node'>\n");
		for (int i=0,imax=recent_.length; i<imax; i++) sb.append(recent_[i]).append("	");
//System.out.println("recent "+recent_[i]);
		sb.append("</span>\n");
	}

	if (warnings_.length() > 0) {
		sb.append("<p><h2>Warnings</h2>\n");
		for (StringTokenizer st=new StringTokenizer(warnings_, "\n"); st.hasMoreTokens(); ) sb.append(st.nextToken()).append("<br>\n");
		//sb.append("<p><a href='"+PATH_CONFIG+"'>Fix MANPATH</a>.");
	}

	if (damncats_) {
		sb.append("<h2>Obsolete Cat Pages</h2>");
		sb.append("<p>The Multivalent browser reads man pages from roff source, rendering the quasi-ASCII cached formatted versions in 'cat' directories obsolete. ");
		sb.append("You can clean up your disk by deleting cat pages, and do so safely as other man page readers will generate a cat version as needed.");
		sb.append("<form method='event' action='"+MSG_KILLCATS+"'><input type='submit' value='Delete Cats'></form>");
	}

	sb.append("\n</body></html>");
//System.out.println(sb.substring(0));
	return sb.toString();
  }


  /**
	Parse page spec, look up in database, report if no found or multiple matches, or openDocument on single match.
  */
  public String showPage(String page) {
//System.out.println("** showPage "+page);
	// extract volume if "<name>.<vol>" or "<name>(<vol>)"
	String volnum=null; // String because volumes can be "numbered", e.g., "3x"
	String shortpage=page;
	int chop=page.indexOf('('), chop2;
	if (chop!=-1) {
		chop2=page.indexOf(')'); if (chop2==-1) chop2=page.length();
		volnum = page.substring(chop+1,chop2);
		shortpage = page.substring(0,chop);
		// anything after ')' thrown away, such as punctuation
	} else {
		// check Dotsdot_...
		chop = page.lastIndexOf('.');
		if (chop!=-1) shortpage=page.substring(0,chop);
	}


	// look up in database
	Object dirs = Name2dirs_.get(shortpage);
//System.out.println("Name2dirs_.get("+page+") = "+dirs+", class="+dirs.getClass());


	// multiple matches => try to winnow based on volume number
	if (dirs instanceof List && volnum!=null) {
		List<String> matches = (List<String>)dirs;
		List<String> winnow = new ArrayList<String>(matches.size());

if (DEBUG) System.out.println("trying to winnow down from "+matches+", based on |"+volnum+"|");
		// full match (e.g., "1V")
		if (volnum.length()>1) {
			for (Iterator<String> i=matches.iterator(); i.hasNext(); ) {
				String cdir = i.next();
				if (cdir.endsWith(volnum)) winnow.add(cdir);
			}
		}

		// single volume letter only (e.g., "1", matches ".../man/man1V")
		if (winnow.size()==0) {	// none matched strictist
			char volchar = volnum.charAt(0);
			for (Iterator<String> i=matches.iterator(); i.hasNext(); ) {
				String cdir = i.next();
				//int inx = cdir.lastIndexOf('/');	// canonicalized, not File so no File.separatorChar
//System.out.println("	"+volchar+" =? "+sdir.charAt(inx+4));
				//if (inx!=-1 && inx+4<cdir.length() && cdir.charAt(inx+4)==volchar) winnow.add(cdir);
				if (cdir.charAt(cdir.lastIndexOf('/')+4)==volchar) winnow.add(cdir);
			}
		}

		// if were able to winnow it down, reset list of dirs
		if (winnow.size()==1) dirs=winnow.get(0); else if (winnow.size()>1) dirs=winnow;
	}


	// report
	if (dirs==null) {	// no match => should happen much with smart link creation, and if happen externally use popup rather than separate page
		return "<p>Manual page <b>"+page+"</b> not found.";	// maybe URL of static page with info on setting MANPATH

	} else if (dirs instanceof String) {  // single match => return it even if volume doesn't match
		try {
			//volnum = dir.getName().substring(3);
			Browser br = getBrowser();
//System.out.println("redirected to "+dir+File.separator+page+"."+volnum+" -- should work");
			// maybe if don't send event but dial direct, can keep manualpage:XXX as URL
			//br.eventq(Document.MSG_OPEN, new URL("file:/"+new File(dir,page+"."+volnum).getAbsolutePath()));
			// somebody needs to see if file is compressed
			//br.eventq(Document.MSG_OPEN, "file:/"+dirs.toString().replace('\\','/')+"/"+shortpage+"."+volnum);	// not File.separator since canonical URL, not File.toURL since that's just directory portion
			//String dir = (String)dirs;
			//String file = findInDir(String)dirs, shortpage);
			br.eventq(Document.MSG_OPEN, findInDir((String)dirs, shortpage).toURL().toString());
			br.eventq(Document.MSG_REDIRECTED, null);
			//return true;	// short-circuit
		} catch (MalformedURLException canthappen) {}
		//dir = (File)dirs;

		//return null;	// no html doc => implies short-circuit
		return "Redirecting";   // gotta have some document

	} else {	// if (dirs instanceof List) -- multiple matches
		List<String> matches = (List<String>)dirs;
		//return "<p>Several possiblilites for
		//dir = (File)((List<>)dirs).get(0);	// for now just pick first one
		StringBuffer sb = new StringBuffer(200 + matches.size()*100);
		sb.append("<p>").append(matches.size()).append(" matches of ").append(page).append("<ul>");
		for (Iterator<String> i=matches.iterator(); i.hasNext(); ) {
			String cdir=i.next();
			//int inx = cdir.lastIndexOf('/');
			//if (inx!=-1 && inx+4<cdir.length()) {
			//String vol = (inx!=-1 && inx+4<cdir.length()? cdir.substring(inx+4): "");
			//String match = "file:/"+cdir+"/"+shortpage+"."+cdir.lastIndexOf('/'+4);
			File match = findInDir(cdir, shortpage);
			sb.append("<li><a href='").append(match.toURI()).append("'>").append(match.getPath()).append("</a>");
		}
		sb.append("</ul>");

		//return matches.size()+" matches of "+page+": "+matches;
		return sb.toString();
	}
  }

  /**
	Database strips volume information, which would be fine,
	except that pages are often filed in the wrong directory, e.g., ls.1V in .../man/man1 rather than .../man/man1V,
	so sometimes have to search for page.
	@return File of found page
  */
  File findInDir(String sdir, String filename) {
	File dir = new File(sdir.replace('/',File.separatorChar));
	String vol = sdir.substring(sdir.lastIndexOf('/')+4);
	String vname = filename+"."+vol;

	// fast path: maybe did follow convention
	File f = new File(dir, vname); if (f.exists()) return f;
	try { f = Files.getFuzzyFile(dir, vname); if (f.exists()) return f; } catch (IOException ignore) {}
	f = new File(dir, filename+".man"); if (f.exists()) return f;

	// have to iterate over each file in directory
	// could use FilenameFilter
if (DEBUG) System.out.println("have to search for "+vname);
	String[] files = dir.list();
	String dname = filename+".";	// any volume number
	for (int i=0,imax=files.length; i<imax; i++) {
		if (files[i].startsWith(dname) && !Files.isBackup(files[i])) return new File(files[i]);
	}

	//return null;	  // shouldn't happen
	return new File(dir, vname);	// shouldn't happen
  }


  /**
	Set up MANPATH, volume letters and names.
	Separated from buildDatabase() so can show UI without necessarily building database.
  */
  public void configure() {
	errhtml=null; Name2dirs_ = null;

	// get MANPATH, volume letters, volume names
	//Multivalent control = getGlobal();

	//String manpath = control.getPreference(ENV_MANPATH, "systemresource:/demo/man");
	//if (manpath==null) { manpath="systemresource:/demo/man"; sep=";"; }
	// ok if missing
	// LATER: diagnostics on MANPATH: missing dirs, duplicate dirs, ...

	volletters_ = getPreference(PREF_VOLUMES, VOLLETTERS).split(":");
	volnames_ = getPreference(PREF_VOLNAMES, VOLNAMES).split(":");

	if (volletters_.length==volnames_.length) {
		fdb_ = false;
	} else {
		errhtml += "<p>Man page volume letters not same length as volume names ("+volletters_.length+" vs "+volnames_.length+").\n"
			+ "Fix in "+PREF_VOLUMES+" and "+PREF_VOLNAMES+" in Preferences.";
		return;
	}
  }


  /**
	Scan directories of {@link #ENV_MANPATH MANPATH} and collect page names.
  */
  public/*anybody can ask for a recount*/ void buildDatabase() {
	// collect pages (set data structures regardless if MANPATH is empty so other features can access)
	Name2dirs_ = new HashMap<String,Object>(1000);
	Map<String,Set<String>> v2n  = new HashMap<String,Set<String>>(20);
	Dotsdot_ = new ArrayList<String>(20);
	damncats_ = false;


	//Multivalent control = getGlobal();
	//Version v = new Version(System.getProperty("java.vm.version");
//System.out.println(Package.getPackage("java")+", 1.4? "+Package.getPackage("java.lang").isCompatibleWith("1.4")+", 1.5? "+Package.getPackage("java.lang").isCompatibleWith("1.5")); -- bug in Apple's Java 1.5
	String manpath = System.getProperty("java.version").startsWith("1.5")/*temporary -- until Java 5 flag day*/? System.getenv(ENV_MANPATH): null;
	if (manpath==null) manpath = "/usr/man:/usr/share/man:/usr/local/man:/opt/local/man";	// not Java 1.5 or not set
//if (DEBUG) System.out.println(ENV_MANPATH+" = "+manpath);
	if (manpath==null) { warnings_ = "No "+ENV_MANPATH+" set."; return; }

	//String manpath = "D:/prj/Multivalent/www/jar/demo/man";
	//String manpath = "D:/prj/Multivalent/www/jar/demo/man;C:/cygnus/full-man/man;D:/prj/TkMan/man;D:/ICache/tcl/man";
	//if (manpath==null) { System.err.println("No MANPATH"); return; }	// allows initialization of data structures

	//int nvols = volletters_.length; -- for now, read what's there

//System.out.println("MANPATH = "+manpath+", sep="+sep);
	StringBuffer warnings = new StringBuffer(1000);
	mancnt_=0;
	String knownfile = "/multivalent/Multivalent.class";
	String syspfx = getClass().getResource(knownfile).getFile();
	syspfx = syspfx.substring(0,syspfx.lastIndexOf(knownfile)); //.lastIndexOf('/'));

	boolean isDoze = File.pathSeparatorChar==';';	// good enough
if (DEBUG) System.out.println("syspfx = "+syspfx);
	List<String> valids = new ArrayList<String>(50);	// cheaper than st.countTokens(), which is not necessarily exact anyhow
	List<String> recents = new ArrayList<String>(100);
	long now=System.currentTimeMillis(), recent=now-(30L * 24*60*60*1000);
	boolean seerecent = Booleans.parseBoolean(getPreference(PREF_RECENT, "true"), true);
//System.out.println("now="+now+", recent="+recent+", "+(now-(30L*24*60*60*1000)));

	List<String> pathseen = new ArrayList<String>(20);	// guard against duplicate paths
	for (String pdir: manpath.split(File.pathSeparator)) {	// can't universally split on ':' because if on Doze that's for the odious drive letter
		pdir = pdir.replace('\\','/');
		// fix up, normalize
		if (pdir.startsWith("systemresource:")) pdir = syspfx + pdir.substring("systemresource:".length());

		if (pdir.endsWith("/") && pdir.length()>1) pdir=pdir.substring(0,pdir.length()-1);

		// validate
		// since in control of MANPATH, may as well clean it up rather than reporting error and forcing user to edit... but would be bad to mysteriously kill path without telling user why, who would go crazy adding it back again and again
		File p = new File(pdir);
if (DEBUG) System.out.println("reading "+p);
		if (!p.isAbsolute()) { warnings.append(pdir).append(" -- not absolute path.\n"); continue; }
		if (!p.exists()) { warnings.append(pdir).append(" -- doesn't exist!\n"); continue; }
		if (!pdir.endsWith("/man") && new File(p, "man").exists()) p=new File(p,"man");  //warnings.append(" Try ").append(pdir).append("/man."); => fix up silently
		if (!p.canRead()) { warnings.append(pdir).append(" -- not readable!\n"); continue; }
		if (!p.isDirectory()) { warnings.append(pdir).append(" -- not a directory.\n"); continue; }

		String pcdir = null;	try { pcdir = p.getCanonicalPath(); } catch (IOException canthappen) {}
		//if (pathseen.get(pcdir)!=null) { warnings.append(pdir).append(" -- repeated in MANPATH.\n"); continue; }
		if (pathseen.indexOf(pcdir)!=-1) { /*warnings.append(pdir).append(" -- repeated in MANPATH.\n");*/ continue; }	// => fix up silently (just ignore)
		//pathseen.put(pcdir, Files.DEFINED);
		pathseen.add(pcdir);
		// maybe write new MANPATH into Preferences here


		// man1, man2, ... (ignore cats completely -- sorry SGI)
		File[] voldirs = p.listFiles();
		int voldircnt=0, mancnt=0;
		for (int i=0,imax=voldirs.length; i<imax; i++) {
			File dir=voldirs[i]; String cdir=dir.getPath().replace('\\','/');
			String dirname = dir.getName();	// last directory
			if (dirname.length()<=3 || !dir.isDirectory()) { /*ignore*/ }
			else if (dirname.startsWith("cat")) damncats_=true;
			else if (dirname.startsWith("man")) {	// Solaris should convert SGML "sman"
//System.out.println("\treading "+dir);
				voldircnt++;
				String volnum = dirname.substring(3);
				if (volnum.startsWith(".")) volnum=volnum.substring(1); // some lame OS names subdirectories "man.1"
				boolean isRecent = (dir.lastModified() > recent || isDoze); // Doze doesn't update directory date when adding and removing files, but has fast file stat
				// expect.1, ascii.5, ...
				String[] men = dir.list();
				// if (men.length==0) that's ok
				for (int j=0,jmax=(men==null?0:men.length); j<jmax; j++) {
					mancnt++;
					String n = men[j];
					if (seerecent && isRecent && new File(dir, n).lastModified() > recent) recents.add(n);
//System.out.println("recent "+n);

					//int namei = n.lastIndexOf(File.separator);
					//if (namei==-1) continue; else n=n.substring(namei+1);
//System.out.println("\t\t"+n);
					//String n = men[j].getName();	// if use listFiles()
					if (Files.isBackup(n)) continue;
					//if (n.endsWith("~") || n.endsWith(".bkup") || n.endsWith(".backup")) continue;
					int endi = n.length();
					if (Files.isCompressed(n)) endi = n.lastIndexOf('.', endi-1);     // chop off compression suffix
					// chop off volume suffix ("expect.1" => "expect") as implicit in data structures
					// could check that suffix matches directory (e.g., "ls.1V" is in ".../man/man1V/"), but just deal with it
					endi=n.lastIndexOf('.',endi-1);
					if (endi==-1) continue;  // not in conventional man page naming, so probably not a man page
					n=n.substring(0,endi);
					if (n.indexOf('.')!=-1) Dotsdot_.add(n);

//System.out.println("\t\tadd "+n+" to "+cdir);
					Object o = Name2dirs_.get(n);
					if (o==null) Name2dirs_.put(n, cdir); // list len 0 + 1 => put dir
					else if (o instanceof String) {	// list len 1 + 1 => make List
						List<String> nl = new ArrayList<String>(2);
						nl.add((String)o);
						nl.add(cdir);	// ok to save dir since just a handle
						Name2dirs_.put(n, nl);	// overwrite File with List
					} else {	// list len >1 + 1 => add to existing List
						List<String> nl = (List<String>)o;
						nl.add(cdir);
					}

//System.out.println("volnum of "+dirname+" = "+volnum);
					Set<String> vl = v2n.get(volnum);
					if (vl!=null) {
						vl.add(n);
					} else {
						vl=new HashSet<String>(200); v2n.put(volnum, vl); vl.add(n);
					}
				}
			}
		}

		if (voldircnt==0) {
			warnings.append(pdir).append(" -- no manN subdirectories.");
			//if (!pcdir.endsWith("/man") && new File(pcdir, "man").exists()) warnings.append(" Try ").append(pdir).append("/man.");
			//else if ( -- // .../man/man1 => .../man
			warnings.append("\n");
			//continue;
		} else if (mancnt==0) {
			warnings.append(pdir).append(" -- contains no man pages.\n");
		} else {
			valids.add(pcdir);
			mancnt_ += mancnt;
		}
	}

	// when done collecting, convert Lists to typed, sorted arrays
	final String[] TYPE_STRINGARRAY = new String[0];	// defined by Java anywhere?
	Vol2names_ = new HashMap<String,String[]>(v2n.size());
	for (Iterator<Map.Entry<String,Set<String>>> i=v2n.entrySet().iterator(); i.hasNext(); ) {
		Map.Entry<String,Set<String>> e = i.next();
		String[] names = e.getValue().toArray(TYPE_STRINGARRAY);
		Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
		Vol2names_.put(e.getKey(), names);
		//Collections.sort((Set)e.getValue(), String.CASE_INSENSITIVE_ORDER); => convert to List, end result is array
//System.out.println("key = "+key+", names.length="+names.length);
	}
	validpaths_ = valids.toArray(TYPE_STRINGARRAY);
	manpath_ = pathseen.toArray(TYPE_STRINGARRAY);
	recent_ = seerecent? recents.toArray(TYPE_STRINGARRAY): null;

	warnings_ = warnings.toString();

	//oldMANPATH_ = manpath;

	fdb_ = true;

	// dump timing and page count statistics (take from main())
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	configure();
  }


  /** Test database reading. */
  public static void main(String[] argv) { if (DEBUG) {
	ManualPageVolume mpv = new ManualPageVolume();
	Runtime rt = Runtime.getRuntime();
	rt.gc(); rt.gc(); long membefore = rt.freeMemory();
	long before = System.currentTimeMillis();
	mpv.buildDatabase();
	long after = System.currentTimeMillis();
	rt.gc(); rt.gc(); long memafter = rt.freeMemory();

	try {
		BufferedWriter w = new BufferedWriter(new FileWriter("~/tmp/manpage.txt"));
		w.write("*** VOL => PAGES ***"); w.newLine();
		int cnt=0;
		for (Iterator<Map.Entry<String,String[]>> i=Vol2names_.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,String[]> e = i.next();
			String key = e.getKey();
			w.write(key); w.newLine();
			String[] names = e.getValue();
			cnt += names.length;
			for (int j=0,jmax=names.length; j<jmax; j++) { w.write("   "); w.write(names[j]); } w.newLine();
		}
		w.write("*** PAGE => DIRS ***"); w.newLine();
		for (Iterator<Map.Entry<String,Object>> i=Name2dirs_.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,Object> e = i.next();
			String key = e.getKey();
			w.write(key); w.write("   "); w.write(e.getValue().toString()); w.newLine();
		}
		w.write("*** STATS ***"); w.newLine();
		w.write("Page count: "+cnt); w.newLine();
		w.write("Memory use: "+(memafter-membefore)); w.newLine();
		w.write("Time (ms): "+(after-before)); w.newLine();
		w.close();
	} catch (IOException ioe) {
		System.err.println(ioe);
	}
  }}
}
