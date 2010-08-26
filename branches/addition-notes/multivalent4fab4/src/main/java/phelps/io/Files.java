package phelps.io;

import java.io.*;
import java.util.regex.*;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;

import phelps.lang.Strings;



/**
	Extensions to {@link java.io.File}.

	<ul>
	<li>{@link File} constructors: {@link #getFile(String)}, {@link #getCanonicalFile(String)}, {@link #getFuzzyFile(File, String)}
	<li>name tests: {@link #isCompressed(String)}, {@link #isBackup(String)}
	<li>parsers: {@link #getSuffix(String)}, {@link #getRoot(String)}
	<li>pretty printing: {@link #relative(File, File)}, {@link #shortpath(File, File)}
	<li>UNIX {@link #glob(String)}
	<li>content manipulation: {@link #toByteArray(File)}, {@link #copy(File, File)}, {@link #delete(File)}, {@link #secureDelete(File)}
	</ul>

	@version $Revision: 1.5 $ $Date: 2005/01/02 10:59:06 $
*/
public class Files {
  /** Compression types we can handle. */
  private static final String[] Z = { ".gz", ".Z", /*".z--no, pack",*/ ".bzip2", ".bz2" };
  //private static final Matcher ZREGEX = Pattern.compile("(?i)\\.(gz|Z|bzip2|bz2)$").matcher("");
  //public static final String[] bkup = { "~", ".bak", ".bkup", ".backup" };
  private static final Matcher ZBKUP = Pattern.compile("(?i)(~|\\.bak|\\.bkup|\\.backup)(\\.(gz|Z|z|bzip2|bz2))?$").matcher("");   // also catches ...~5~
  private static final Matcher GLOB_REGEX = Pattern.compile("[?*\\[\\]{}]").matcher("");


  /** Comparator that sorts filenames ({@link File#getName()}), not full path, by {@link phelps.lang.Strings#DICTIONARY_CASE_INSENSITIVE_ORDER. */
  public static final Comparator<File> DICTIONARY_CASE_INSENSITIVE_ORDER = new Comparator<File>() {
	public int compare(File f1, File f2) { return Strings.compareDictionary(f1.getName(), f2.getName(), true); }
  };



  private Files() {}


  /** Common size of disk blocks at the time of release (this can increase from release to release). */
  public static final int BUFSIZ = 8*1024;


  /** Like {@link java.io.File#File(String)} but also expands <code>~<var>user</var></code> to users home directory (as given by the <code>user.home</code> property). */
  public static File getFile(String path) {
	if (path.equals("~") || path.startsWith("~/") || path.startsWith("~\\")) path = System.getProperty("user.home") + path.substring(1);
	return new File(path);
  }

  /** Like {@link java.io.File#getCanonicalFile(String)} but also expands <code>~</code> to users home directory (as given by the <code>user.home</code> property). */
  public static File getCanonicalFile(String path) throws IOException {
	return getFile(path).getCanonicalFile();
  }

  /** 
	Returns existing file (not directory), looking around a little if necessary:
	<ul>
	<li>(UNIX) {@link #getCanonicalFile(String)}
	<li>(WWW) if points to directory, tries adding "index.html"
	<li>(compression) adding or removing a compression suffix
	</ul>
	If no existing file can be found, returns same as {@link #getCanonicalFile(String)}.
  */
  public static File getFuzzyFile(File base, String path) throws IOException {
	// absolute
	File f = new File(path); if (f.exists()) return f.getCanonicalFile();

	// UNIX
	if (path.equals("~") || path.startsWith("~/") || path.startsWith("~\\")) {
		path = System.getProperty("user.home") + path.substring(1);
		f = new File(path);
	}

	// relative
	if (base!=null && !f.isAbsolute()) f = new File(base, path);

	// www
	File fe;
	if (f.exists() && f.isDirectory()) {
		if ((fe = new File(path + "index.html")).exists()) f = fe;
		else if ((fe = new File(path + "index.htm")).exists()) f = fe;
		//else fail
	}

	// compression
	if (!f.exists()) {
		if (isCompressed(path)) {	// match file by dropping suffix?
			path = path.substring(0, path.lastIndexOf('.'));
			fe = new File(path);
			if (fe.exists()) f = fe;

		} else { // add suffix
			for (int i=0,imax=Z.length; i<imax; i++) {
				fe = new File(path  + Z);
				if (fe.exists()) { f  = fe; break; }
			}
		}
	}

	return f.getCanonicalFile();
  }



  /** Returns path of <var>file</var> relative to <var>base</var>. */
  public static String relative(File base, File file) {
	final char dirch = File.separatorChar;

//System.out.println("base = "+base+", file = "+file);
	try { file = getCanonicalFile(file.toString()); } catch (IOException ioe) { return file.toString(); }
	//if (file.isAbsolute()) return file.toString();
	if (base!=null) try { base = getCanonicalFile(base.toString()); } catch (IOException ioe) { base = null; }
	String f = file.toString(); if (file.isDirectory()) f += dirch;	// directories in '/'
	String rel = f;

	if (base!=null) {
		String b = base.toString(); if (base.isDirectory()) b += dirch; 
		int i=0, blen=b.length(), flen=f.length(); for (int imax=Math.min(blen, flen); i<imax; i++) if (b.charAt(i)!=f.charAt(i)) break;	// match from front
//System.out.print("match |"+b.substring(0,i)+"| @"+i);
		/*if (!(new File(b.substring(0,i))).isDirectory()) {*/ i--; while (i>0 && f.charAt(i)!=dirch) i--; i++;//}	// back up to nearest dir
//System.out.println(" => |"+b.substring(0,i)+"| @ "+i);

		StringBuffer sb = new StringBuffer(flen);
		for (int j=i; j<blen; j++) if (b.charAt(j)==dirch) sb.append("..").append(dirch);
//System.out.println("i="+i+", flen="+flen);
		//if (sb.length()>0) sb.setLength(sb.length()-1);
//System.out.println(f+" @ "+i+" => |"+f.substring(i)+"|");
		sb.append(f.substring(i));
		rel = sb.toString();
	}

	return rel;
  }

  /** Returns the shortest path: relative to <var>base</var>, relative to user home directory, or absolute. */
  public static String shortpath(File base, File file) {	// can't be "short" because that's reserved
	String rel = relative(base, file);

	// HOME-relative shorter?
	final String home = System.getProperty("user.home");
	String f = file.toString();
	if (f.startsWith(home)) {
		String rel2 = "~" + f.substring(home.length());
		if (rel2.length() < rel.length()) rel = rel2;
//System.out.println(rel+" vs "+home+" => "+rel2);
	}

	// absolute
	try {
		String abs = getCanonicalFile(f).toString();
		if (abs.length() < rel.length()) rel = abs;
	} catch (IOException ioe) {}
	
	return rel;
  }


  /** Like {@link File#getName()}, but works on {@link String}. 
  public static String getName(String path) {
	// find last forward- or backslash
	
  }*/

  /** Returns filename with suffix, if any, chopped off. */
  public static String getRoot(String filename) {
	if (filename==null) return null;
	int inx = filename.lastIndexOf('.');//, inx2 = filename.lastIndexOf('/');	// '\\'
	return inx>0? filename.substring(0,inx): filename;
  }

  public static String getTail(String path) {
	if (path==null) return null;
	int inx = path.lastIndexOf('/');
	return inx!=-1? path.substring(inx+1): path;
  }

  public static String getSuffix(File file) {
	if (file==null) return null;
	return getSuffix(file.getName());
  }

  /**
	Returns the portion of <var>filename</var> after and exclusive of the last dot (".").
  */
  public static String getSuffix(String filename) {
	if (filename==null) return null;
	int inx = filename.lastIndexOf('.'), inx2 = filename.lastIndexOf('/');
	// LATER: also '\'
	return inx>0 && inx>inx2? filename.substring(inx+1): "";
  }

  /**
	Like {@link java.io.File#renameTo(File)}, except guaranteed to work across file-systems, copying if necessary.
  */
  public static boolean renameTo(File file, File dest) {
	boolean ok = file.renameTo(dest);
	if (!ok) {
		try {
			copy(file, dest);
			ok = file.delete();
		} catch (IOException ioe) {
			dest.delete();
		}
		if (!ok) dest.delete();	// copied but can't delete: undo copy
	}
	return ok;
  }


  /**
	Returns true if <var>filename</var> has a compression suffix for a type we can handle,
	which are <code>.gz</code>, <code>.Z</code>, <code>.bzip2</code>/<code>.bz2</code>.
  */
  public static boolean isCompressed(String filename) {
	//ZREGEX.reset(filename);
	//return ZREGEX.find();
	return !"identity".equals(getEncoding(filename));
  }

  /**
	Returns HTTP Content-Encoding (compression type) as guessed from <var>path</var>'s suffix.
	No encoding and unknown encoding are returned as null;
  */
  public static String getEncoding(String path/*, String content-type-header -- files don't have headers*/) {
	String type = "identity";
	if (path!=null) {
		String x = Files.getSuffix(path);
		if ("Z".equals(x)) type = "compress";	// uppercase
		else if ("lzw".equals(x = x.toLowerCase())) type = "compress";	// lowercase
		else if ("gz".equals(x)) type = "gzip";
		else if ("flate".equals(x)) type = "flate";
		else if ("bz2".equals(x)) type = "bzip2";
	}
	return type;
  }

  /** Returns true if <var>filename</var> is a backup file (e.g., end with ".bkup", with possible additional compression suffix). */
  public static boolean isBackup(String filename) {
	ZBKUP.reset(filename);
	return ZBKUP.find();
  }


  /**
	Returns array of File's matching UNIX <em>glob</em> <var>pattern</var>, 
	with the extension that <code>**</code> searches the current directory and all subdirectories.
	A glob pattern is related to a regular expression as follows:
	the glob <code>*</code> is equivalent to regexp <code>.*</code>., <code>?</code> to <code>.</code>, and <code>{<var>one</var>,<var>two</var>}</code> to <code>{<var>(one</var>|<var>two</var>)</code>.
	Also, the <code>~</code>, <code>.</code>, and <code>..</code> strings have their same meaning as in {@link #getFile(String)}.

<!-- => just use find
	<p>The <code>**</code> pattern is incremental, so you can use it on the entire filesystem without....
	Examples: glob(/** /*InputStream.java).
-->
  */
  public static File[] glob(String pattern) throws IOException {
	File[] FILE0 = new File[0];
	if (pattern==null) return FILE0;

	// X just convert to regexp and pass to regexp-glob => weird to match entire path as regexp.  see also FilenameFilterPattern
	int inx = pattern.indexOf('/');
	File base = new File(".").getCanonicalFile();
	if (inx!=-1) {
		String root = pattern.substring(0,inx);
		if ("/".equals(root)) base = new File(root);
		else if ("~".equals(root)) base = new File(System.getProperty("user.home"));
		//else if (".".equals(root) || "..".equals(root)) => done in loop
		else inx = -1;
	}
	
	List<File> qfile = new LinkedList<File>(); qfile.add(base);
	List<String> qpat = new LinkedList<String>(); qpat.add(pattern.substring(inx+1));
	List<File> l = new ArrayList<File>(100);
	while (qfile.size() > 0) {
		File dir = qfile.remove(0); String pat0 = qpat.remove(0), pat = pat0, rest = "";
		inx = pat.indexOf('/'); if (inx!=-1) { rest = pat.substring(inx+1); pat = pat.substring(0,inx); }
		boolean fend = "".equals(rest);
//System.out.println(dir+", "+pat+"  /  "+rest);

		if (pat.length()==0) l.add(dir);
		else if (".".equals(pat)) { qfile.add(dir); qpat.add(rest); }
		else if ("..".equals(pat)) { qfile.add(dir.getParentFile()); qpat.add(rest); }
		else if ("**".equals(pat)) {	// all subdirectories
			qfile.add(dir); qpat.add(rest);	// current directory too
			// one level of subdirectories at a time so it's incremental and can search entire file system
			for (File f: dir.listFiles()) {
				if (f.isDirectory()) { qfile.add(f); qpat.add(pat0/*not rest*/); }
			}

		} else if ("*".equals(pat)) {	// fast path
			for (File f: dir.listFiles()) {
				if (fend) l.add(f); else { qfile.add(f); qpat.add(rest); }
			}
		} else if (GLOB_REGEX.reset(pat).find()) {	// pattern: transform to regexp to match
			StringBuffer sb = new StringBuffer(pat.length() * 2);
			for (int i=0,imax=pat.length(); i<imax; i++) {
				char ch = pat.charAt(i);
				if ('\\'==ch) sb.append("\\\\");	// no escaping in glob
				else if (false) sb.append('\\').append(ch);	// escape regexp metachars
				else if ('?'==ch) sb.append('.');	// translate glob to regexp
				else if ('*'==ch) sb.append(".*");	// not greedy?
				else if ('['==ch || ']'==ch) sb.append(ch);	// same as regexp
				else if ('{'==ch) {
					int inxend = pat.indexOf('}', i);
					inx = pat.indexOf(',', i);
					if (inxend==-1 || inx==-1) sb.append('{');	// should throw exception
					else {
						// send second half may have metachars so send around again
						String before = i>0? pat.substring(0,i): "", after = inxend+1<imax? pat.substring(inxend+1): "";
						qfile.add(dir); qpat.add(before + pat.substring(inx+1,inxend) + after);
						// take {..,..} out of pat and reset loop
						pat = pat.substring(i+1,inx) + after; i=0-1; imax = pat.length();
					}
				} else sb.append(ch);
			}
			Matcher m = Pattern.compile(sb.toString()).matcher("");
			for (File f: dir.listFiles()) {
				if (m.reset(f.getName()).matches()) {
					if (fend) l.add(f); else { qfile.add(f); qpat.add(rest); }
				}
			}

		} else {	// plain
			File f = new File(dir, pat);
			if (f.exists()) {
				if (fend) l.add(f); else { qfile.add(f); qpat.add(rest); }
			}
		}
	}

	return (File[])l.toArray(FILE0);
  }


  public static byte[] toByteArray(File file) throws IOException {
	return InputStreams.toByteArray(new FileInputStream(file), file.length());
  }


  /**
	Copies <var>filein</var> to <var>fileout</var>, creating parent directories as needed.
  */
  public static void copy(File filein, File fileout) throws IOException {	// => NIO
	filein = filein.getAbsoluteFile(); fileout = fileout.getAbsoluteFile();
	if (!filein.exists()) throw new FileNotFoundException(filein.getPath());
	if (!filein.canRead()) throw new IOException(filein.getPath()+" not readable");
	if (filein.equals(fileout)) return;

	File outdir = fileout.getParentFile();
	//if (filein.equals(outdir)) continue;
	if (!outdir.exists()) fileout.mkdirs();

	InputStreams.copy(new FileInputStream(filein), new FileOutputStream(fileout), true);
  }


  /**
	Delete directory, including all files and subdirectories.
  */
  public static boolean delete(File dir) {
	if (dir.isDirectory()) {
		for (File f: dir.listFiles()) {
			if (f.isDirectory()) delete(f);
			else f.delete();			
		}
	}
	return dir.delete();
  }

  /**
	Securely deletes a file by first overwriting it with random data several times.
	@return <code>true</code> iff successful
  */
  public static boolean secureDelete(File file) throws IOException {
	if (!file.canWrite()) return false;

	// maybe randomize from first bytes of file, so have to know contents before can know seed
	Random rand = new Random();
	byte[] buf = new byte[BUFSIZ];
	long length = file.length();
	RandomAccessFile raf = new RandomAccessFile(file, "rw");	// not buffered
	try {
		for (int i=0; i<10; i++) {
			for (long togo = length; togo > 0; togo -= buf.length) {
				rand.nextBytes(buf);
				raf.write(buf, 0, (int)Math.min(togo, buf.length));
			}
		}
	} finally { raf.close(); }

	return file.delete();
  }



	/*
  public static void main(String[] argv) throws Exception {
	File[] glob = glob(argv[0]);
	System.out.println(java.util.Arrays.asList(glob)+" = "+glob.length);
  }*/
}
