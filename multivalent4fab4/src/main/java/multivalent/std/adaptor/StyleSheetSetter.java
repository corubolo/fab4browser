/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.adaptor;

import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

import com.pt.io.Cache;

import multivalent.*;



/**
	Cascades style sheets for a document:
	<ol>
	<li start=0>(Primordial settings in <tt>multivalent.Context</tt>)
	<li>User pan-document in user's <tt>stylesheet/EVERYWHERE.css</tt>
	<!--li>Intrinsic to type alongside <tt>.class</tt-->
	<li>Genre from <tt>/sys/stylesheet/<i>genre</i>.css</tt>
	<li>User genre from <tt>stylesheet/<i>genre</i>.css</tt>
	<li>(Maybe have a user override, or genre override)
	</ol>.

	<p>Caches style sheets and then makes a clone,
	but checks modified date to see if need to refresh,	so developer/user and tweak and reload.

	<p>TO DO: Would like to allow different style sheet types than just CSS.

	@version $Revision: 1.3 $ $Date: 2008/09/08 09:15:29 $
*/
public class StyleSheetSetter extends Behavior {
  public static final String PREFIX = "/sys/stylesheet/";

  private static class Entry {
	public StyleSheet ss;
	long lastmod;
	public Entry(StyleSheet ss, long mod) { this.ss=ss; lastmod=mod; }
  }


  static Map<String,Entry> cache_ = new HashMap<String,Entry>(20);
  String suffix_;

  /** On {@link Document#MSG_OPENED}, stuff set Document style sheet cascade. */
  @Override
public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;

	Object arg = se.getArg();
	if ((Document.MSG_BUILD==msg || Document.MSG_OPENED==msg) && arg instanceof DocInfo) {
		DocInfo di = (DocInfo)arg;
		Document doc = di.doc;
		if (Document.MSG_BUILD==msg) setCascade(doc, doc.getAttr(Document.ATTR_GENRE));
//System.out.println("StyleSheetSetter "+msg);
	}
	return false;
  }


  /**
	User pan-document, intrinsic, genre default, user genre.
  */
  void setCascade(Document doc, String genre) {

	// 0. primordial
	// (hardcoded in Context)


	// 1. user pan-document
	Cache cache = getGlobal().getCache();
	//RESTORE: setStyleSheet(doc, "user-pan", cache.mapTo(null, PREFIX+"EVERYWHERE"+suffix_, Cache.GROUP_PERSONAL), null);
//System.out.println("usen pan: "+cache.mapTo(null, "/stylesheet/EVERYWHERE"+suffix_, Cache.GROUP_PERSONAL));


/*
	// 2. intrinsic => based on class name, which is BOGUS
	//Layer baseLayer = (Layer)doc.getLayers().getBehavior(Layer.BASE);
	Layer baseLayer = doc.getLayer(Layer.BASE);
	Object o = baseLayer.getBehavior(0);
	if (o instanceof MediaAdaptor) {    => doc.getMediaAdaptor()
		Class cl = o.getClass();
		String name = cl.getName();
		int inx = name.lastIndexOf('.'); if (inx!=-1) name = name.substring(inx+1);
		URL url = cl.getResource(PREFIX + name + suffix_);
//System.out.println("intrinsic URL = "+url);
		if (url!=null) {
			if ("file".equals(url.getProtocol())) setStyleSheet(doc, "intrinsic", new File(url.getPath()), null);
			else setStyleSheet(doc, "intrinsic", url, -1, null);
		}
	}
*/

	// 3. genre
//System.out.println("openedDocument on "+genre);
	MediaAdaptor me = doc.getMediaAdaptor();
	URL url = me.getClass().getResource(PREFIX + genre + suffix_);
//System.out.println("genre: "+getClass().getName()+" + "+PREFIX+" + "+genre+" + "+suffix_+" = "+url);
	if (url!=null) {
		if ("file".equals(url.getProtocol())) setStyleSheet(doc, Document.ATTR_GENRE, new File(url.getPath()), null);
		else setStyleSheet(doc, Document.ATTR_GENRE, url, -1, null);
	}


	// 4. user genre -- highest priority
	//RESTORE: setStyleSheet(doc, "user-genre", cache.mapTo(null, PREFIX + genre + suffix_, Cache.GROUP_PERSONAL), null);
  }


  void setStyleSheet(Document doc, String name, File f, StyleSheet insertbefore) {
	if (f.canRead()) try { setStyleSheet(doc, name, f.toURL(), f.lastModified(), insertbefore); } catch (MalformedURLException canthappen) {}
  }

  void setStyleSheet(Document doc, String name, URL url, long lastmod, StyleSheet insertbefore) {
	if (url==null) return;
//System.out.println("load "+name+": "+url);

	String key = url.toString();
	Entry e = cache_.get(key);
	StyleSheet ss = null;

	if (e==null || e.lastmod < lastmod) {
//if (e!=null) System.out.println("parse "+uri+" cached mod="+e.lastmod+", now mod="+lastmod);
		ss = new CSS();
		ss.parse(url);	// should at least clone() rather than parsing fresh HTML for each page, though still pretty fast
		e = new Entry(ss, lastmod);
		cache_.put(key, e);
//System.out.println("\t"+ss.size()+" selector-ContextListener pairs");

	} else ss = e.ss;

	if (ss.size() > 0) {
		ss = ss.copy();
		ss.setName(name);
		if (insertbefore==null) ss.setCascade(doc);
		else { ss.setCascade(insertbefore.getCascade()); insertbefore.setCascade(ss); }
	}
  }


  /** Read in name of style sheet type. */
  @Override
public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	/*
	String classname = getAttr("interpreter", "multivalent.std.adaptor.CSS");
	suffix_ = getAttr("suffix", ".css");

	Layer scratch = getDocument().getLayer(Layer.SCRATCH);
	interp_ = (MediaAdaptor)Behavior.getInstance("interp", classname, null, scratch);
*/
	suffix_ = ".css";
  }
}
