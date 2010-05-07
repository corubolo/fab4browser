package multivalent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;

import phelps.io.Files;
import phelps.util.logging.FormatterShort;
import phelps.util.logging.HandlerStream;

import com.pt.io.Cache;
import com.pt.io.CacheFile;
import com.pt.net.MIME;



/**
	Execute application startup, and act as a central repository for resources shared among all browser windows.

	<ul>
	<li>{@link #getPreference(String,String) preferences} - name-value pairs that are saved to disk and shared among all browsers.
		The system preferences are read in first, then the user-specific preferences, which can override.
	<li>{@link #getBrowser(String) list} of {@link Browser} instances, addressable by name
	<li>mapping from document URI to genre to behavior (Java class): {@link #getGenre(String,String)} and {@link #remapBehavior(String)}
	<li>shared resources: {@link #getCache() Cache}, {@link #getTimer() Timer}
	<li>software engineering: {@link #getLogger() Logger}
	</ul>

	Specification and implementation versions are available from the {@link java.lang.Package}.

	@version $Revision: 1.20 $ $Date: 2005/10/10 02:15:28 $
 */
public final class Multivalent {
	public static final String COPYRIGHT = "Copyright (c) 1997 - 2005  T.A. Phelps";

	public static final String VERSION = Multivalent.class.getPackage().getSpecificationVersion();

	/**
	<!--Invoke {link #destroy()} in the preferred way, one that can be cancelled.-->
	Safely destroy browsers and all their documents, and Destroy .
	<p><tt>"EXIT"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
	 */
	public static final String MSG_EXIT = "EXIT";

	public static final String FILENAME_PREFERENCES = "Preferences.txt";

	private/*public*/ static final String PREF_HOMEDIR = "HOMEDIR";
	public static final String PREF_CACHEDIR = "CACHEDIR";

	/** URI of Multivalent home WWW site. */
	public static final URI HOME_SITE = URI.create("http://multivalent.sourceforge.net/");


	/** Singleton instance. */
	private static Multivalent instance_ = null;

	/** Standalone browser vs library. */
	private boolean fstandalone_ = false;

	private Level loglevel_ = /*Meta.DEVEL? Level.FINE:*/ Level.CONFIG;	// DEVEL means logging to screen rather than file.  easy to give -verbose for Level.FINE

	/** Tables mapping suffix/MIME type to media adaptor name. */
	private CHashMap<String> defadaptor_, adaptor_=new CHashMap<String>(100);

	private HashMap<String,String> defberemap_, beremap_=new HashMap<String,String>(100);	// case sensitive -- maybe use LinkedHashMap

	private CHashMap<String> defpref_, pref_=new CHashMap<String>(100);	// don't return directly as could set non-String key or value

	private List<Browser> browsers_ = new ArrayList<Browser>(5);

	private Cache cache_ = null;

	private Timer timer_ = null;

	static {
		getLogger().setLevel(Level.OFF);
	}


	private/*force singleton via getInstance()*/ Multivalent() {
	}

	/**
	Returns singleton instance, from which preferences and other state can be accessed.
	 */
	public static Multivalent getInstance() {
		if (Multivalent.instance_==null) {
			Multivalent.instance_ = new Multivalent();	// loaded with different ClassLoader than behaviors and layers
			//try { instance_ = (Multivalent)cl_.loadClass("multivalent.Multivalent"/*Multivalent.class.getName()*/).newInstance(); } catch (Exception canthappen) { System.err.println(canthappen); }
			//assert cl_ == instance_.getClass().getClassLoader(): cl_+" vs "+instance_.getClass().getClassLoader();
			//System.out.println("Multivalent.class class loader = "+instance_.getClass().getClassLoader());

			Multivalent.instance_.config();
		}

		return Multivalent.instance_;
	}

	/** Read system preferences and user preferences, and establish cache. */
	private void config() {
		final String home = System.getProperty("user.home"), username = System.getProperty("user.name"), tmpdir = System.getProperty("java.io.tmpdir");
		String cache = getPreference(Multivalent.PREF_CACHEDIR, null);
		if (cache==null) {
			cache = home!=null? home: tmpdir;
			if (username!=null && !cache.endsWith(username)) cache += File.separatorChar + username;
			cache += File.separatorChar + ".Multivalent";	// invisible on UNIX, OS X
		}
		File cachedir = new File(cache);
		if (!cachedir.exists()) cachedir.mkdirs();

		putPreference(Multivalent.PREF_HOMEDIR, home);

		// can show all supported ImageIO types as RawImage.  Before reading Preferences.txt so that overrides.
		for (String s: ImageIO.getReaderMIMETypes()) { String key = s.toLowerCase(); if (adaptor_.get(key)==null) adaptor_.put(s, "RawImage"); }
		for (String s: ImageIO.getReaderFormatNames()) { String key = s.toLowerCase(); if (adaptor_.get(key)==null) adaptor_.put(key, "RawImage"); }	// don't override
		if (adaptor_.get("jpeg2000")!=null && adaptor_.get("jp2")==null) adaptor_.put("jp2", "RawImage");
		//System.out.println("image formats: "+adaptor_);
		//System.out.println("writers "+Arrays.asList(ImageIO.getWriterFormatNames()));


		// 2. logging
		Logger log = Logger.getLogger("");
		log.setLevel(Level.INFO/*WARNING?*/);
		for (Handler h: log.getHandlers()) log.removeHandler(h);
		if (fstandalone_) {	// nix default, add file, add screen
			// tracing-level to file
			Handler handler;
			try {
				handler = new FileHandler(new File(cachedir, "log.txt").toString());
				handler.setFormatter(new SimpleFormatter());	// defaults to XML
				handler.setLevel(Level.FINE);
				log.addHandler(handler);
			} catch (IOException ioe) {}

			// CONFIG to screen
			handler = new HandlerStream(System.out, new FormatterShort());
			handler.setLevel(Level.FINEST);	// limited by logger, such as root's INFO
			log.addHandler(handler);

		} else {	// don't log libraries
			String[] libs = { "com.pt", "phelps" };
			for (String lib: libs) Logger.getLogger(lib).setUseParentHandlers(false);
		}

		log = getLogger();
		log.setLevel(loglevel_);
		log.info("Multivalent "+Multivalent.VERSION);
		//log.info(COPYRIGHT);
		log.info("home site: "+Multivalent.HOME_SITE);
		log.config("home directory = "+home);
		log.config("cache directory = "+cache);
		// hardware info ... or tool.Info?


		// 2. preferences
		// a. system preferences, from all JARs, which set defaults
		try {
			Map<URL,URL> seen = new HashMap<URL,URL>(13);	// get dups, I suppose from parent and local?
			for (Enumeration e = getClass().getClassLoader().getResources("sys/"+Multivalent.FILENAME_PREFERENCES); e.hasMoreElements(); ) {
				URL url = (URL)e.nextElement(); if (seen.get(url)!=null) continue; else seen.put(url, url);
				//System.out.println("\t"+url);
				readPreferences(url.openStream());
			}
		} catch (IOException ioe) { log.warning("startup: "+ioe); }

		// copy to defaults (since keys and values are immutable String's, clone() suffices)
		defadaptor_ = (CHashMap<String>)adaptor_.clone();
		defberemap_ = (HashMap<String,String>)beremap_.clone();
		defpref_ = (CHashMap<String>)pref_.clone();

		// b. user preferences, which overwrite system prefs
		//try { readPreferences(cache_.getInputStream(new DocInfo(), PREFERENCES, Cache.USER)); } catch (IOException ok) { System.out.println("couldn't read user prefs, which is ok: "+ok); }
		File userpref = new File(cachedir, Multivalent.FILENAME_PREFERENCES);
		if (userpref.exists()) try { readPreferences(new FileInputStream(userpref)); } catch (IOException ioe) { log.warning("couldn't read user prefs: "+ioe); }
		//RESTORE: try { readPreferences(new FileInputStream(Cache.mapTo(null, FILENAME_PREFERENCES, Cache.USER));	// cache not set up yet


		// 3. cache
		// later, even cacheing is a behavior (substitute stream with tee to cache too)
		if (cachedir.canWrite()) try { cache_ = new CacheFile(cachedir, new File(home)); } catch (IOException ioe) {}
		if (cache_==null) cache_ = Cache.NONE;	//new CacheMemory()?
	}


	public Cache getCache() { return cache_; }

	/**
	Maps MIME Content-Type and path to Multivalent genre.
	Genre can be mapped to a Java class via {@link Behavior#getInstance(String,String,ESISNode,Map,Layer)}.
	 */
	public String getGenre(String contenttype, String path) {
		String genre = null;

		// 1. try MIME Content-Type => media adaptor
		if (contenttype!=null) {
			int inx = contenttype.indexOf(';'); if (inx>0) contenttype = contenttype.substring(0,inx).trim();
			genre = adaptor_.get(contenttype);
		}

		// 2. guess / maybe override
		String guess = null;
		// else suffix => media adaptor
		String sfx  = null;
		if (path !=null)
			sfx = Files.getSuffix(path).toLowerCase();
		else return null;
		if (guess==null && path!=null) guess = adaptor_.get(sfx);
		//System.out.println("guess ["+path+"] = "+guess);
		if (guess==null) {
			String ct = MIME.guessContentType(path);
			if (ct!=null) guess = adaptor_.get(ct);
			//System.out.println("guess ["+contenttype+"] = "+guess);
		}

		// special cases
		if (guess==null && new File(path).isDirectory()) guess = "DirectoryLocal";

		//System.out.print("genre = "+genre+", contentType = "+contenttype);
		if ((genre==null || MIME.TYPE_TEXT_PLAIN.equals(contenttype)) && guess!=null)
			//log.finest(... if (genre!=null) System.out.println("overriding "+contenttype+" => "+guess);
			genre = guess;

		// 3. bail
		// back up Content-Type to general type (e.g., text/* => text)
		if (genre==null && sfx==null) genre = adaptor_.get(MIME.TYPE_TEXT_PLAIN);
		if (genre==null && contenttype!=null) {
			// LATER...
		}

		// give up
		if (genre==null) genre = adaptor_.get(MIME.TYPE_TEXT_PLAIN);	// OCTETSTREAM ?

		getLogger().finest("genre: "+contenttype+" / "+path+" => "+genre);
		return genre;
	}

	public Map<String,String> getGenreMap() { return adaptor_; }


	/**
	Returns main logger (subsystems can use own logger).

	<p>Conventions for use of logging levels by behaviors:
	<ul>
	<li>{@link Level.SEVERE} (highest value) - errors
	<li>{@link Level.WARNING} - warnings
	<li>{@link Level.INFO} - significant system operations (e.g., opening a document -- but not all semantic events) <!-- or highly unusual, like charstring Type 1 4-byte int? -->
	<li>{@link Level.CONFIG} - version, JARs found, cache dir, fonts, hardware info
	<li>{@link Level.FINE} - semantic events, short-circuited sem ev
	<li>{@link Level.FINER} - performance metrics (memory, time), create fonts and images, caching
	<li>{@link Level.FINEST} (lowest value) - URI=>URL conversions, behavior creation, enter/exit methods
	</ul>
<!--
	<li>ALL
	<li>OFF
-->
	 */
	public static Logger getLogger() { return Logger.getLogger("multivalent"); }

	/**
	Heartbeat timer calls observers every 100 ms.
	<!-- used by the cursor and blinking text -->
	 */
	public Timer getTimer() {
		//if (heartbeat_==null) heartbeat_ = new Timer(100, 1000);	// on demand: don't slow down startup
		if (timer_==null) timer_ = new java.util.Timer();
		return timer_;
	}


	/**
	Returns preferred behavior according to substitution map in <tt>Preferences.txt</tt>.
	Clients can ask for the preferred "Hyperlink" and get the lastest-greatest, which was written after the client.
	Replacements must subclass what they replace and should generally recognize as many attributes as applicable.
	{@link Behavior#getInstance(String, String, Map, Layer)} remaps all behavior names.
<!--  remaps behavior use to third party additions, according to table in <tt>Preferences.txt</tt> -->
	 */
	public String remapBehavior(String bename) {
		assert bename!=null;
		String remap = beremap_.get(bename);
		//System.out.println(bename+" => "+remap);
		return remap!=null? remap: bename;	// if no remapping, keep original
	}

	/* need these for "set", "remap", "mediaadaptor"
  public void addRemapping(String from, String to) {
  }
	 */

    public Set<String> getEngineNames() {
        HashSet<String> names = new HashSet<String>(beremap_.values());
        return Collections.unmodifiableSet(names);
    }


	/**
	Returns preference under passed <var>key</var>,
	or if it doesn't exists sets value with <var>defaultval</var> and establishes this as the future preference value.
	Keys are case insensitive.
	 */
	public final String getPreference(String key, String defaultval) {
		assert key!=null /*&& defaultval!=null --ok*/;

		String val = pref_.get(key);
		if (val==null && defaultval!=null)
			val=defaultval;
		/*putPreference(key,val);*/	// good idea? puts into Preferences.txt so can edit
		return val;
	}

	public final void putPreference(String key, String val) {
		assert key!=null;
		pref_.put(key,val);
	}

	public final void removePreference(String key) {
		assert key!=null;
		pref_.remove(key);
	}
	public final Iterator<String> prefKeyIterator() { return pref_.keySet().iterator(); }
	// => no key-val iterator because could bypass putPref

	/**
	Reads preferences, system or user, overwriting existing settings.
	TO DO: preserve comments through read/write cycle.
	 */
	private void readPreferences(InputStream prefin) {
		//getLogger().info("reading preferences");

		// line-based, <command> <args>, "#" starts comment, lines can be blank
		BufferedReader prefr = new BufferedReader(new InputStreamReader(prefin));
		StreamTokenizer st = new StreamTokenizer(prefr);
		st.eolIsSignificant(true);
		st.resetSyntax();
		st.whitespaceChars(0,' '); st.wordChars(' '+1, 0x7e);
		st.commentChar('#'); st.slashSlashComments(true); st.slashStarComments(true);
		st.quoteChar('"');

		try {
			String key, val;
			for (int token=st.nextToken(); token!=StreamTokenizer.TT_EOF; ) {
				if (token==StreamTokenizer.TT_EOL) { token=st.nextToken(); continue; }
				String cmd = st.sval;
				if (cmd!=null) cmd=cmd.intern();
				st.nextToken(); key=st.sval; st.nextToken(); val=st.sval;	// for now all commands have same syntax
				if ("mediaadaptor"==cmd)
					//System.out.println("media adaptor "+key+" => "+val);
					adaptor_.put(key.toLowerCase(), val);	// not case sensitive
				else if ("remap"==cmd)
					//System.out.println("behavior remap "+key+" => "+val);
					beremap_.put(key, val);
				//berevmap_.put(val, key);	// reverse map for when save out => NO, keep logical name and associated behavior separate
				else if ("set"==cmd)
					putPreference(key, val);
				do
					token=st.nextToken();
				while (token!=StreamTokenizer.TT_EOL && token!=';' && token!=StreamTokenizer.TT_EOF);
			}
			prefr.close();
		} catch (IOException ignore) {
			getLogger().warning("can't read prefs "+ignore);
		}
	}

	private void writePreferences() {
		getLogger().info("writing preferences to cache");
		try {
			//File pref = getCache().mapTo(null, FILENAME_PREFERENCES, Cache.USER);
			OutputStream out = getCache().getOutputStream(null, Multivalent.FILENAME_PREFERENCES, Cache.GROUP_PERSONAL);
			//if (pref.canWrite()) { try { writePreferences(new FileOutputStream(pref)); } catch (IOException doesnthappen) {} }
			Writer w = new BufferedWriter(new OutputStreamWriter(out));

			// initial comments -- take from input comments
			w.write("# If line begins with '#', command is commented out\n\n\n");

			// media adaptors, behavior remappings, variables
			writePrefTable(w, adaptor_, defadaptor_, "mediaadaptor");
			w.write("# ... otherwise interpreted as ASCII\n\n\n");
			writePrefTable(w, beremap_, defberemap_, "remap");
			w.write("\n\n\n");
			writePrefTable(w, pref_, defpref_, "set");
			w.write("\n\n\n");

			w.close();
		} catch (IOException ioe) {
			getLogger().warning("Couldn't write Preferences: "+ioe);
		}
	}

	/**
	Writes preferences: name-value, commenting out if value is same as default.
	Used for various tables: media adaptor, remap, set.
	 */
	private void writePrefTable(Writer w, Map<String,String> cur, Map<String,String> def, String cmd) throws IOException {
		assert w!=null && cur!=null && def!=null && cmd!=null;

		Object[] keys = cur.keySet().toArray();
		Arrays.sort(keys);	// better than random hash order

		for (Object key2 : keys) {
			String key=(String)key2, val=cur.get(key), defval=def.get(key);
			if (val.equals(defval)) w.write("#");	// comment out if unchanged, so can change default later
			String quote = val.indexOf(' ')==-1? "": "\"";
			w.write(cmd+"\t"+key+"\t"+quote+val+quote+"\n");
		}
	}



	/** Convenience method for creating a standalone browser, as if <code>getBrowser(<var>name</var>, "System", true)</code>. */
	public Browser getBrowser(String name) { return getBrowser(name, "System", true); }

	/**
	Returns {@link multivalent.Browser} with given name, with passed URL to system behaviors hub.  If no such browser, create new one.
	This, not Browser's constructor, is the way to create new instances.
	 */
	public Browser getBrowser(String name, String systemHub, boolean standalone) {
		assert /*name!=null--ok &&*/ systemHub!=null;

		Browser br = null;
		if (name!=null && !"_NEW".equals(name))
			// Java 5.0: for (Browser b: browsers_) if (...)
			for (int i=0,imax=browsers_.size(); i<imax; i++) {
				Browser b = browsers_.get(i);
				if (name.equals(b.getName())) { br=b; break; }
			}

		if (br==null) {
			br = new Browser(name, systemHub, standalone);
			browsers_.add(br);
			//X if (fstandalone_) wrap in Frame => done in Browser
			getLogger().fine("new browser instance '"+name+"'");
		}

		return br;
	}

	public Iterator<Browser> browsersIterator() { return browsers_.iterator(); }

	/**
	Used by {@link Browser#destroy()} to remove Browser instance from Multivalent's list.
	If remove last Browser, destroy whole application.
	 */
	/*package-private*/ void removeBrowser(Browser br) {
		assert browsers_.indexOf(br)!=-1;	// "Browser already removed -- shouldn't be using it at all, much less removing it again.";	// no other way to get Browser instance

		browsers_.remove(br);
		//br.destroy();

		if (browsers_.size()==0) destroy();	// ?
	}


	/**
	System shutdown, in this sequence: shuts down all browsers, writes preferences, <code>System.exit(0)</code>.
	Rather than invoking directly, behaviors should send the {@link #MSG_EXIT "EXIT"} semantic event.
	 */
	/*package-private*/ void destroy() {
		// show warning if unsaved parts
		for (int i=browsers_.size()-1; i>=0; i--) browsers_.get(i).destroy();

		//cache_.destroy();		// save cookies -- should periodically save cookies => cookies saved by cookie behavior

		writePreferences();


		// clean up after Java
		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		//System.out.println("cleaning up "+ftmp.length+" in "+tmpdir);
		// deletions
		for (String n: tmpdir.list())
			if (
					n.startsWith("imageio") && n.endsWith(".tmp")
			)
				/*boolean ok =*/ new File(tmpdir, n).delete();
		//System.out.println("axe "+n+" "+ok);

		//System.out.println("POOF!");
		Multivalent.instance_ = null;
		if (fstandalone_) System.exit(0);
	}


	/*
  private void checkNewUser() {
	String v = getPreference("version", VERSION);
	if (VERSION==null || VERSION.equals(v)) return;

	// show license
	JFrame frame = new JFrame("LICENSE AGREEMENT");

	JButton b = new JButton("Accept");
	b.addActionListener( new ActionListener() {
	  public void actionPerformed(ActionEvent e) { frame.destroy(); }
	});
	p.add(b);

	JButton b = new JButton("Decline");
	b.addActionListener( new ActionListener() {
	  public void actionPerformed(ActionEvent e) { System.exit(0); }
	});
	p.add(b);

	frame.pack();
	frame.setVisible(true);

	// text of licenses, Agree & Disagree buttons
	putPreference("version", VERSION);
	writePreferences();
  }
	 */

	public static void main(String[] argv) {
		Multivalent v = Multivalent.instance_ = new Multivalent();	// don't use getInstance() because need to run command-line first
		v.fstandalone_ = true;

		v.config();

		Browser br = v.getBrowser("STARTUP");	// last thing in startup chain, after reading preferences -- maybe the instance with a null name
		//Toolkit.getDefaultToolkit().sync(); -- doesn't flush paint queue

		/*if (!fpage)*/ br.eventq(SystemEvents.MSG_GO_HOME, null);	// LATER: blank/home/current page as set in Preferences
	}

}
