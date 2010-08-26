package phelps.net;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import multivalent.Node;



/**
	Augment URL with information that can be used to find content of URL in case link breaks.
	See the <a href='http://www.cs.berkeley.edu/~phelps/Robust/'>Robust Home Page</a>.

	<p>Strategy:
	Inverse word frequency: find top n most common words in document that are uncommon in web overall.
	<ol>
	<li>count words in page (either from tree or, while testing, HTML text)
	<li>look up relative frequency counts from web search engine, cacheing new ones to disk
	<li>pick locally frequent-globally infrequent
	</ol>

	<ul>
	<li>signature manipulation: 
		{@link #addsignature(URL, String)}, {@link #stripSignature(String)}, 
		{@link #getSignature(String)}, {@link #getSignatureWords(String)}
	<li>signature computation, from various sources of words: 
		{@link #computeSignature(Node) document tree}, {@link #computeSignature(String) String of words}, {@link #computeSignature(List, URL) List of words}
	</ul>


	@see tool.LexSig
	@see tool.html.Robust

	@author T.A. Phelps
	@version $Revision: 1.9 $ $Date: 2003/07/04 08:04:35 $
*/
public class RobustHyperlink {
  public static boolean DEBUG = true;

  public static final String VERSION = "0.3";

  /** Canonical definition of parameter used for lexical signatures. */
  public static final String PARAMETER = "lexical-signature=";

  /** Term frequency-inverse document frequency picks the most frequent words in the document that are the rarest in the web. */
  public static final int ALGORITHM_TFIDF=0;
  /** Refines tfidf by capping page frequency at 3 to bias toward rarity. Default. */
  public static final int ALGORITHM_TFIDF2=1;
  /** Rarest picks the words rarest in the web. */
  public static final int ALGORITHM_RAREST=2;
  /** Picks words randomly. */
  public static final int ALGORITHM_RANDOM=3;
  /** Picks words randomly from those that appear in fewer than 100,000 web pages. */
  public static final int ALGORITHM_RANDOM100K=4;


  static final int DATABASE_VERSION = 3;	// bump up if need to make changes to format, change frequency source, and so on



  // PARAMETERS

  public static boolean Verbose = true;

  static PrintWriter StudyOut = null;

  static File wordCache_ = null;
  /**
	Client can set the file to use as the user's supplemental word frequency cache.
	The Multivalent client places this in user's private cache directory, as public placement can reveal personal information.
	Defaults to a file named "wordfreq.txt" in the Java temp directory.
  */
  public static void setWordCache(File cache) { wordCache_=cache; }


  /** URL of search engine search request, to which the search term can be appended. */
  static String Engine =
	//"http://www.google.com/search?num=1&q=";
	//"http://www.google.com/search?q="; => don't use Google because don't report frequency but something like PageRank equivalent
	//"http://www.altavista.com/cgi-bin/query?pg=q&kl=XX&stype=stext&sc=on&q=";
	//"http://www.altavista.com/cgi-bin/query?pg=q&kl=XX&Translate=on&q=";
	//"http://www.altavista.com/web/results?kgs=0&kls=1&avkw=qtrp&q=";
	//"http://www.altavista.com/web/results?kgs=0&kls=1&avkw=xytx&q=";	// 2003 July 4
	"http://www.altavista.com/web/results?kgs=0&kls=1&avkw=aapt&q=";	// 2003 November 4

  // should search for search word and take closest number in text, within some limit
  /** Text to find in search engine results page next to word frequency. */
  static String EngineHook =
	// "about";	
	//"approximately"; 
	//"About"
	//from "pages found."; on 2001 June 9
	// changed from "WEB PAGES" on 2000 June 27
	// changed from "Web Pages" by 2000 September 21
	//on Jul 5 ...	//"found about";
	//"We found";
	"AltaVista found ";	// 2002 Nov 17

  /** Amount of search engine results page guaranteed to include word frequency. */
  static int HUNK_LEN =	// should compute dynamically: first time read full page, locate hook, subsequently just hook + fuzz (* 2 + 2K)
	//13 * 1024;
	30 * 1024;	// 2002 Nov 17 / AltaVista

  /**
	Sets the search engine and key text fragment that signals the start of the web frequency information.
	Web word freqencies are obtained by screen scraping the results of a search engine.
	Used to switch to a search engine that is different than the default 
	or to update the URL and text hook that's been changed.

	@param prefix  URL of search submissions with the query term at the end and left blank
	@param hook  contant words in the HTML page results near the word frequency number
  */
  public static void setEngine(String prefix, String hook) { Engine=prefix; EngineHook=hook; }

  /** Ignore case in collecting words? */
  public static boolean FoldCase = true;
  public static int MinWordLength = 4;
  /** Signature length (in words). */
  public static int SignatureLength = 10;

  static int Algorithm_ = ALGORITHM_TFIDF2;
  //static boolean Concise = false;
  /** Set algorithm to use (N.B.: <code>static</code>). */
  public static void setAlgorithm(int alg) {
	assert ALGORITHM_TFIDF<=alg && alg<=ALGORITHM_RANDOM100K: alg;
	if (ALGORITHM_TFIDF<=alg && alg<=ALGORITHM_RANDOM100K) Algorithm_ = alg;
  }

/*  public static final String TrimChars;	// "/ .!?,();:$%`'\"@#^=[]{}<>|+*-0123456789"
  static {
	StringBuffer tcsb = new StringBuffer(200);
	//for (int i=' '+1; i<'0'; i++) tcsb.append((char)i);
	//for (int i='9'+1; i<'A'; i++) tcsb.append((char)i);
	for (int i=0; i<'A'; i++) tcsb.append((char)i);
	for (int i='Z'+1; i<'a'; i++) tcsb.append((char)i);
	for (int i='z'+1; i<128; i++) tcsb.append((char)i);
	// also high bit versions?	interferes with accented characters, I bet
	//tcsb.append((char)' '+128);
	for (int i=146; i<148+1; i++) tcsb.append((char)i);
	tcsb.append((char)160);	// &nbsp;
	TrimChars = tcsb.substring(0);
  }*/


  // should compute good values for the following based on searches for words of high, medium, and low frequency
  // 1..FREQ_MED = round to 1000, FREQ_MED..INT_BIG = round to 10000, > FREQ_BIG = INT_MAX
  static final int FREQ_MED =
	//1000000;	// million pretty lax as of Feb 2000, with web at 1 billion pages
	5000000;	// 2002 Nov 17
  static final int FREQ_BIG = FREQ_MED*5;
  static final int ROUND_LITTLE=1000, ROUND_MEDIUM=10000;
  // shared frequency count objects
  static final Integer INT_MAX = new Integer(Integer.MAX_VALUE);
  static Integer[] INTS_LITTLE, INTS_MEDIUM;

  static Map<String,Integer> sys2cnt_ = null, user2cnt_ = null;
  static int newwords_ = 0;



  private RobustHyperlink() {}

  /** Add signature <var>words</var> to <var>url</var>. */
  public static String addSignature(URL url, String words) {
	String surl = url.toString();
	if (words==null) return surl;

	char sep = (surl.indexOf('?')==-1? '?': '&');
	return surl + ("".equals(url.getFile())?"/":"")+ sep + /*((sep=='&' || !Concise)?*/ PARAMETER/*: "")*/ + URIs.encode(words);
  }

  /** Given a URL in String form, return URL with signature, if any, stripped off. */
  public static String stripSignature(String surl) {
	String sig = getSignature(surl);
	if (sig!=null) {
		int inx = surl.indexOf(sig);
		//inx--;	// strip out preceding '?' or '&'
		return surl.substring(0,inx) + surl.substring(inx+sig.length());
	} else return surl;
  }

  /**
	Return signature as found in string.
	Signature is introduced by "lexical-signature=".
  */
  public static String getSignature(String surl) {
	if (surl==null) return null;
	String sig = null;

	int inx = surl.indexOf(PARAMETER);
	if (inx!=-1) {
		int inx2 = surl.indexOf('&', inx+PARAMETER.length());
		if (inx2==-1) inx2 = surl.indexOf('#', inx+PARAMETER.length());
		inx--;	// return preceeding '?' or '&'
		if (inx2==-1) sig=surl.substring(inx); else sig=surl.substring(inx,inx2);
	}

	return sig;
  }

  /** Return signature as plain words: no "?lexical-signature=", no meta characters. */
  public static String getSignatureWords(String surl) {
	String sig = getSignature(surl);
	if (sig!=null) {
		int inx=sig.indexOf(PARAMETER);
		if (inx!=-1) sig = sig.substring(inx+PARAMETER.length());
	}
	if (sig!=null) sig = URIs.decode(sig);

	return sig;
  }



  private static Integer getInteger(int num) {
	Integer into;
	if (num < FREQ_MED) {
		num = num < ROUND_LITTLE? ROUND_LITTLE: (num/ROUND_LITTLE) * ROUND_LITTLE;
		int numi = (num / ROUND_LITTLE) - 1;
		into = INTS_LITTLE[numi];
		if (into==null) into = INTS_LITTLE[numi] = new Integer(num);

	} else if (num < FREQ_BIG) {
		num = (num / ROUND_MEDIUM) * ROUND_MEDIUM;
		int numi = (num - FREQ_MED) / ROUND_MEDIUM;
		into = INTS_MEDIUM[numi];
		if (into==null) into = INTS_MEDIUM[numi] = new Integer(num);

	} else into = INT_MAX;

	return into;
  }

  /** 
	  Upon first use (not at system startup), reads in cached frequencies and make Integer objects for frequencies.
	  File format (java.io.DataOutput): version (int), word count (int), word (UTF) - count (int) pairs....
  */
  private static void readCache() {
	if (sys2cnt_!=null) return;

	INTS_LITTLE = new Integer[FREQ_MED/ROUND_LITTLE];	// populated on demand
	INTS_MEDIUM = new Integer[(FREQ_BIG-FREQ_MED)/ROUND_MEDIUM];

	// 1. system word list
	//long start = System.currentTimeMillis();	// 330ms
	InputStream is = new ByRelFreq().getClass().getResourceAsStream("words.txt");
	if (is!=null) {
		sys2cnt_ = readCache(is);
		//assert DATABASE_VERSION == v: v;
	}
	//System.out.println("Took "+(System.currentTimeMillis()-start)+"ms to read system word list");

	// 2. built-in
	if (sys2cnt_==null) sys2cnt_ = new HashMap<String,Integer>(1000);
	//for (int i=-10; i<100; i++) sys2cnt_.put(Integer.toString(i), INT_MAX); -- too short anyhow
	//for (int i=1900; i<2050; i++) sys2cnt_.put(Integer.toString(i), INT_MAX);	// years => no numbers anyhow
  	String[] extras = {	// words that may not be in word frequency list that we want to be sure to ignore
		"lt", "gt", "nbsp", "quot", "meta", "script", "style",
		//"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", -- too short to be included anyhow
	};
	for (int i=0,imax=extras.length; i<imax; i++) sys2cnt_.put(extras[i].toLowerCase(), INT_MAX);

	// 3. user word list
	try {
		if (wordCache_==null) {
			File tmpfile = File.createTempFile("xxx","yyy");
			wordCache_ = new File(tmpfile.getParent(), "wordfreq.txt");
			tmpfile.delete();
			if (Verbose) System.out.println("words cached to "+wordCache_);
		}

		if (wordCache_.canRead()) {	// user's supplemenal word list won't exist first time
			user2cnt_ = readCache(new FileInputStream(wordCache_));
			//if (wordCache_.lastModified() -  would like to refresh the cache annually, but can't get creation date from Java
		}
	} catch (IOException ioe) {
		// can't create tmpfile on read-only.  OK, just don't write new cache words
	}

	if (user2cnt_==null) user2cnt_ = new HashMap<String,Integer>(1000);

	assert user2cnt_ != null;
  }

  private static Map<String,Integer> readCache(InputStream is) {
//System.out.println("reading user cache");
	DataInputStream dis = null;
	Map<String,Integer> w2c = null;

	try {
		dis = new DataInputStream(new GZIPInputStream(new BufferedInputStream(is)));
		int v = dis.readInt();
		int n = dis.readInt();
		if (DATABASE_VERSION == v) {
			w2c = new HashMap<String,Integer>(n * 2);	// LATER: use phelps.io.DiskHash?
			for (int i=0; i<n; i++) {
				String word=dis.readUTF();
				int cnt = dis.readInt();
//System.out.println(word+" = "+cnt);
				w2c.put(word, getInteger(cnt));
			}
		} //else if (DEBUG) System.out.println("old version of user word list: v"+v+" => deleted");

	} catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(1);

	} finally {
		if (dis!=null) try { dis.close(); } catch (IOException ioe) {}
	}
	return w2c;
  }

  /**
	Writes user word frequency cache.  Should call before quit application.
	May want to periodically refresh cache, for words that become popular and therefore no longer good distinguishers.
  */
  public static void writeCache() {
	if (DEBUG && newwords_ > 0) System.out.println("writing to "+wordCache_);
	if (wordCache_==null/*read-only*/ /*|| !wordCache_.exists()*/ /*|| !wordCache_.canWrite()*/) return;
	try {
		DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(wordCache_.getAbsolutePath(), false))));
		out.writeInt(DATABASE_VERSION);
		out.writeInt(user2cnt_.size());

		for (Iterator<Map.Entry<String,Integer>> i=user2cnt_.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,Integer> e = i.next();
			out.writeUTF(e.getKey()); out.writeInt(e.getValue().intValue());
		}
		out.close();
		newwords_ = 0;
	} catch (IOException ioe) {
		System.err.println("writeCache "+ioe);
	}
  }


  /** Determine web page frequency of <var>word</var>.  If not in cache, looks up in web search engine. */
  public static int getFreq(String word) {
	readCache();

	Integer cnto = sys2cnt_.get(word); if (cnto!=null) return cnto.intValue();
	cnto = user2cnt_.get(word); if (cnto!=null) return cnto.intValue();
	//if (DEBUG && cnto!=null && cnto!=INT_MAX) System.out.println(word+"	"+cnto);


	cnto = INT_MAX;	// assume very common on WWW
	// look up in search engine -- want to batch these, but Google won't report frequency if more than one word at a time
	try {
		// read in search results
		URL url = new URL(Engine + word);
//System.out.println("url = "+url+", DEBUG="+DEBUG);
		Reader in = new BufferedReader(new InputStreamReader(url.openStream()));
		char[] buf = new char[HUNK_LEN];	// Google needs 2K, Alta Vista 11K
		// frequency information in first 1K chars, but be robust if this changes a bit
		int len=0,hunk;
		while (len<HUNK_LEN && (hunk=in.read(buf, len,buf.length-len))>0) len+=hunk;	// should collect entire page, but OK if doesn't
		in.close();
//System.out.println("len = "+len);

		// find count in HTML
		// Google: "<center><font color=darkgray>About <font color=black><b>2720</b></font> matches for <font color=black><b>Multivalent</b></font><br>Showing results <font color=black>1-10</font>,"
		String result = new String(buf, 0,len);
		int inx0=result.indexOf(EngineHook), imax=Math.min(inx0+200,result.length());
//System.out.println(result); System.exit(0);
//System.out.println(inx0+" .. "+imax);
		if (inx0!=-1) {
//System.out.println(word+"   "+result.substring(Math.max(0,inx0-100),inx0)+" *** "+result.substring(inx0,inx0+100));
			char ch; boolean skip=false;
			int num=0, inx=inx0;

			// number after key?
			for ( ; inx<imax && (!Character.isDigit((ch=result.charAt(inx))) || skip); inx++) {
				//if (ch=='"') skip=!skip;	// skip over attribute settings (as in Alta Vista)
				//else if (skip && !Character.isDigit(ch) && ch!='#') skip=false;
				if (ch=='<') skip=true;	// skip over tags
				else if (skip && ch=='>') skip=false;
			}
			// not after, hope number is before key
			if (inx==imax) {
				inx = inx0; skip=false;
				int imin=Math.max(0,inx0-200);
				for (; inx>imin && (!Character.isDigit((ch=result.charAt(inx))) || skip); inx--) {
					if (ch=='<') skip=true; else if (skip && ch=='>') skip=false;
				}
				imax = inx+1;
				while (inx>imin && (Character.isDigit(ch=result.charAt(inx)) || ch==',')) inx--;
				inx++;	// get back to a digit!
			}
//System.out.println("before? "+inx+".."+imax+": |"+result.substring(inx,imax)+"|");

			// text (possibly with commas) => number
			for (int inx2=inx; inx2<imax; inx2++) {
				ch = result.charAt(inx2);
				if (Character.isDigit(ch)) num = num*10+(((int)ch)-'0');	//isb.append(ch);
				else if (ch==',') { /*skip commas*/ }
				else break;
			}
			cnto = num==0? INT_MAX: getInteger(num);

			if (Verbose) System.out.print("new word '"+word+"' => "+num);
			else if (Verbose) System.out.print(newwords_ == 0? ".": "Fetching words not in caches.");
			if (Verbose) System.out.println("->"+cnto);
		}
//		} catch (IOException e) {
//		} catch (NumberFormatException nfe) {
	} catch (Exception e) { System.err.println(e);
	}

	// add to caches, both memory and disk
	user2cnt_.put(word, cnto);
	newwords_++;

	return cnto.intValue();
  }




  static class WordFreq {
	String word;
	int pagecnt;
	int webcnt;	// for debugging.  relatively few words < FREQ_MED, so space not a consideration
	WordFreq(String w, int pagecnt, int webcnt) { word=w; this.pagecnt=pagecnt; this.webcnt=webcnt; }
	public String toString() { return word+"="+pagecnt+"/"+webcnt; }
  }

  static class ByRelFreq implements Comparator<WordFreq> {
	public int compare(WordFreq wf1, WordFreq wf2) {
		double r1=((double)wf1.webcnt)/((double)wf1.pagecnt), r2=((double)wf2.webcnt)/((double)wf2.pagecnt);
		//int r1=((WordFreq)o1).rel, r2=((WordFreq)o2).rel;
		return (r1==r2? 0: (r1<r2? -1: 1));
	}
   }

  // temporary, until have more sophisticated robust selection of terms
  static class ByRoFreq implements Comparator<WordFreq> {
	public int compare(WordFreq wf1, WordFreq wf2) {
		double r1=((double)wf1.webcnt)/((double)Math.min(3,wf1.pagecnt)), r2=((double)wf2.webcnt)/((double)Math.min(3,wf2.pagecnt));
		//int r1=((WordFreq)o1).rel, r2=((WordFreq)o2).rel;
		return (r1==r2? 0: (r1<r2? -1: 1));
	}
   }

  static class ByWebFreq implements Comparator<WordFreq> {
	public int compare(WordFreq wf1, WordFreq wf2) {
		return wf1.pagecnt - wf2.pagecnt;
	}
  }


  /**
	 Filter words according to options: no numbers, min length, fold to lower case, multiple words in same list element.
  */
  static List<String> filterWords(List<String> words) {
	List<String> good = new ArrayList<String>(words.size());

	for (int i=0,imax=words.size(); i<imax; i++) {
		String token = words.get(i);
		if (token==null || token.indexOf('@')!=-1) continue;	// no e-mail--not indexed (and probably would change if page moved?)

		// strings of letters only -- no numbers or other characters
		for (int j=0, jmax=token.length(); j<jmax; ) {
			while (j<jmax && !Character.isLetter(token.charAt(j))) j++;
			int start = j;
			while (j<jmax && Character.isLetter(token.charAt(j))) j++;

			if (j-start > MinWordLength) {
				String word = token.substring(start,j);
				good.add(FoldCase? word.toLowerCase(): word);
			}
		}

/*		StringTokenizer st = new StringTokenizer(sword, TrimChars);
		while (st.hasMoreTokens()) {
			String word = st.nextToken();	// aw, that's Tcl's trim:  .trim(TrimChars);	// trim from edges only--not embedded
			// disqualify phone numbers since if person moves and page goes away, phone numbers change too
			if (word.length()>=MinWordLength && !Character.isDigit(word.charAt(0))) {
				words.add(FoldCase? word.toLowerCase(): word);
//System.out.println("word = "+word);
			}
		}*/
	}

	return good;
  }


  /** Compute signature from document tree. */
  public static String computeSignature(Node root) {
//System.out.println("root="+root+"/"+((INode)root).childAt(0)+", firstleaf="+root.getFirstLeaf());
	List<String> words = new ArrayList<String>(1000);
	for (Node n=root.getFirstLeaf(), endl=root.getLastLeaf().getNextLeaf(); n!=endl; n=n.getNextLeaf()) {
		String pname = n.getParentNode().getName(); if (pname=="script" || pname=="style") continue;	// HTML
		words.add(n.getName());
	}
	return computeSignature(words);
  }

  /** Compute signature from parsed <var>txt</var>. */
  public static String computeSignature(String txt) {
	List<String> l = new ArrayList<String>(1);
	l.add(txt);
	return computeSignature(l);
  }

  /** Compute signature from list of words. */
  public static String computeSignature(List<String> words) {
	if (words.size()==0) return "(empty word list)";

	readCache();

	words = filterWords(words);

	// sort
	int len = words.size() + 1;
	String[] word = words.toArray(new String[len]);
	word[len-1] = String.valueOf((char)0xffff);	// sentinal to flush -- sorts last + can't match other words
	Arrays.sort(word);

	// count + arrange by count
	//SortedSet ss = new TreeSet(new ByRelFreq()); => no!  if have same rel freq, then only one kept in *set*
	List<WordFreq> list = new ArrayList<WordFreq>();
	String prev=word[0];
	int c=0;
	for (int i=0,imax=len; i<imax; i++) {
		String w = word[i];
		if (w.equals(prev)) c++;
		else {
			int freq = getFreq(prev);
//System.out.println("word = "+w+", freq="+freq);
			//if (/*c>1 &&*/ /*rel*/freq<=FREQ_MED) list.add(new WordFreq(prev, c, freq));
			if (freq < Integer.MAX_VALUE) list.add(new WordFreq(prev, c, freq));
//System.out.println(prev+" "+freq);
//if (freq<=FREQ_MED) System.out.println("added "+prev);
//if (relfreq<100) System.out.println(prev+" "+c+"/"+freq+" => "+relfreq);
			prev=w; c=1;
		}
	}
	// last one flushed by sentinal
	if (list.size()==0) return "(no valid words)";

	WordFreq[] bogus = new WordFreq[0];
	WordFreq[] wordfreq = (WordFreq[])list.toArray(bogus);


	int validlen = Math.min(SignatureLength,wordfreq.length);
	// could OO this with an interface and classes...
	if (ALGORITHM_TFIDF==Algorithm_) {
		Arrays.sort(wordfreq, new ByRelFreq());

	} else if (ALGORITHM_RAREST==Algorithm_) {
		Arrays.sort(wordfreq, new ByWebFreq());

	} else if (ALGORITHM_RANDOM==Algorithm_) {
		Random rand = new Random();
		for (int i=0,imax=validlen; i<imax; i++) {
			int swapi=rand.nextInt(imax);
			WordFreq tmp=wordfreq[i]; wordfreq[i]=wordfreq[swapi]; wordfreq[swapi]=tmp;
		}

	} else if (ALGORITHM_RANDOM100K==Algorithm_) {
		// any random combination of words this rare works great
		Random rand = new Random();
		validlen=0;
		// march through, building up random list in 0..validlen
		for (int i=0,imax=wordfreq.length; i<imax; i++) {
			WordFreq tmp=wordfreq[i];
			if (tmp.webcnt < 100000) {
				int swapi=rand.nextInt(validlen+1);	// returns 0..validlen
				wordfreq[i]=wordfreq[validlen]; wordfreq[validlen]=wordfreq[swapi]; wordfreq[swapi]=tmp;
				validlen++;
			}
		}
		validlen = Math.min(validlen, SignatureLength);

	} else /*default == tfidf+*/ {
		//System.out.println("robustrare");
		Arrays.sort(wordfreq, new ByRoFreq());
		// refinements for robustness here
	}


	// dump top 20 words, page freq, web freq, so can see what you might want, and figure out a formula to choose them
	if (Verbose) {
		System.out.println("* Rankings *");
		for (int i=0;i<Math.min(25, wordfreq.length); i++) System.out.println(wordfreq[i]);
	}


	// return top n
//if (debug) System.out.println(ss.size()+" qualifying words");
	//if (words.size()<100 && validlen<=2) return "(page too short)"; -- want to do this, but sometimes single word good enough


	StringBuffer sigsb = new StringBuffer(100);
	for (int i=0,imax=validlen; i<imax; i++) {
		//WordFreq wf = wordfreq[i];
		// need some flag to dump pagecnt and webcnt here
		if (i>0) sigsb.append(' ');	// '+'
		sigsb.append(wordfreq[i].word);

		if (StudyOut!=null) StudyOut.print(wordfreq[i].pagecnt+"/"+wordfreq[i].webcnt+" ");
	}
	if (StudyOut!=null) StudyOut.println(/*validlen*/);

	if (Verbose && newwords_>0) {
		System.out.println();	// newline after string of dot-per-new-word
	}
	if (newwords_ > 100) writeCache();

	return sigsb.substring(0);
  }
}
