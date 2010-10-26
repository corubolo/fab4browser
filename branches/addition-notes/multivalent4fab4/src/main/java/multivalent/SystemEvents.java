package multivalent;	// would be in multivalent.std but needs priviledged access to package multivalent

import java.awt.Rectangle;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import multivalent.node.LeafUnicode;
import multivalent.std.MediaLoader;
import phelps.net.URLs;



/**
	Default implentation of standard set of system events: open document, load layer, and more.
	Behaviors shouldn't call Browser directly;
	instead, send a semantic event, so other behaviors can filter/mutate/record/... it.

	For example, to open new document, send {@link Document#MSG_OPEN};
	this class opens the document, and in addition {@link multivalent.net.Cookies} adds/records Cookies,
	{@link multivalent.std.ui.ForwardBack} records the trail, {@link multivalent.std.ui.History} records in persistently,
	and other behaviors could handle a proxy server and encryption and so on.

	<p>This behavior implements all of its handled semantic events in {@link #semanticEventAfter(SemanticEvent, String)}.
	It does so in <i>after</i> in order to give other behaviors a chance to modify the event.

<!--
	<p>Under consideration
	<ul>
	<li>dynamic <name> - create new behavior instance, call restore and build.	but add to which layer?  allow duplicates?	maybe make those options
	</ul>

	<p>TO DO
	Put these back into Browser so don't have to make its methods public.
	=>In fact, take functionality out of Browser as much as possible and put here to enforce semantic events
	... but are some cases where should call directly.
-->

	@version $Revision: 1.15 $ $Date: 2005/01/03 09:06:37 $
 */
public class SystemEvents extends Behavior {
	private static final boolean DEBUG = false;

	// move to... Document? Browser?  Or kill?
	/**
	Open user home page, by sending {@link Document#MSG_OPEN} with HOMEPAGE user preference.
	<p><tt>"goHome"</tt>
	 */
	public static final String MSG_GO_HOME = "goHome";

	/**
	Broadcast content of form for some other behavior to process, as opposed to sending to some server.
	<p><tt>"formData"</tt>: <tt>arg=</tt> {@link java.util.Map} <var>name-value pairs</var>, <tt>in=</tt> {@link Node} <var>root-of-form</var>
	 */
	public static final String MSG_FORM_DATA = "formData";

	/**
	Destroy objects of various types: Node, Behavior, ...
	<p><tt>"destroy"</tt>: <tt>arg=</tt> {@link java.lang.Object} <var>object-to-destroy</var>
	 */
	public static final String MSG_DESTROY = "destroy";




	// move to... Document? Browser?
	/**
	Show given page relative to help directory in help window.
	<p><tt>"showHelp"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
	 * /
  public static final String MSG_SHOW_HELP = "showHelp";
	 */

	//static final CHashMap EMPTYATTRS = new CHashMap();

	List<Document> unform_ = new ArrayList<Document>(10);


	/**
	Default implementations of various semantic events, in <i>After</i> to give everybody else a chance to do something different.
	Semantic events handled here:
	{@link Multivalent#MSG_EXIT},
	{@link Browser#MSG_NEW}, {@link Browser#MSG_CLOSE}, {@link Browser#MSG_STATUS},
	{@link Document#MSG_OPEN}
		This event can arrive between openDocument and openedDocument, as openDocument request a new document,
	the system realizes the current one hasn't been closed and so issues closeDocument,
	before the request is satisfied as announced by openedDocument.
	, {@link Document#MSG_OPENED},
	{@link Document#MSG_RELOAD}, {@link Document#MSG_STOP}, {@link Document#MSG_CLOSE}, {@link Document#MSG_REFORMAT}, {@link Document#MSG_REPAINT},
	{@link Layer#MSG_LOAD},
	{@link IScrollPane#MSG_SCROLL_TO}
	{@link #MSG_GO_HOME},
	 */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Object arg=se.getArg();
		//System.out.println(msg+" "+arg+"/"+(arg!=null? arg.getClass().getName(): ""));
		Browser br = getBrowser();
		Document curdoc = br.getCurDocument();  // make sure br.setCurDoc sets this soon enough

		if (Multivalent.MSG_EXIT==msg)
			// save state as hub or leave that to Browser class?
			getGlobal().destroy();
		else if (Browser.MSG_NEW==msg) {
			Map eattrs = arg instanceof CHashMap? (CHashMap)arg: CHashMap.EMPTY /*EMPTYATTRS*/;

			String name=(String)eattrs.get("name");
			Object uri = eattrs.get("uri");
			Browser newbr = getGlobal().getBrowser(name);
			//Browser newbr = new Browser(getGlobal(), (name!=null?name:"<NONAME>"));
			// not sure which should be default
			//if (arg instanceof URI) newbr.eventq(newdoc, Document.MSG_OPEN, o);
			//if (arg instanceof String) // named instance
			//if (newbr.getURI()==null) newbr.open(getClass().getResource("/sys/Splash.html"));
			if (uri!=null) newbr.eventq(Document.MSG_OPEN, uri);
			else newbr.eventq(SystemEvents.MSG_GO_HOME, null);

		} else if (Browser.MSG_CLOSE==msg)
			//br.event(doc, Document.MSG_CLOSE, null); -- why not?
			br.destroy();
		//getGlobal().removeBrowser(br);
		else if (Document.MSG_OPEN==msg) { // || Multipage.MSG_OPENPAGE==msg/*not sure if this is a good idea*/) {
			//System.out.println("SystemEvents "+msg+" "+arg);
			// this is in eventAfter, so if other guys wanted to cancel opening, wouldn't have reached this
			//br.eventq(Document.MSG_CLOSE, null)); -- br.open() does this itself as we don't know where the open calls could be coming from
			if (arg==null)
				br.eventq(Document.MSG_RELOAD, null);
			// should kill the cache?
			//br.open((URL)null);
			else if (arg instanceof String) {
				//System.out.println("String => URI "+arg);
				String uriin = (String)arg; //-- how to pass to DocInfo creation?
				//uriin = VScript.eval(uriin, getDocument(), null, null); => FIX
				//System.out.println("String openDoc = "+arg+" => "+uriin);
				try {
					//System.out.println("opendoc URI");
					br.eventq(Document.MSG_OPEN, new URI(uriin));
				} catch (URISyntaxException ignore) { /*error*/ }

			} else if (arg instanceof URL) {
				if (SystemEvents.DEBUG) System.out.println("openDocument w/URL arg: "+arg);	// I've converted to URIs, but URL OK too
				try { br.eventq(Document.MSG_OPEN, URLs.toURI((URL)arg)); } catch (URISyntaxException fail) {}

			} else if (arg instanceof URI) {
				//System.out.println("URI => URL "+arg);
				String fixuri = (String)br.callSemanticEvent("URI", /*workaround*/arg.toString());
				try {
					URI uri = br.getCurDocument().getURI().resolve(fixuri);
					//br.eventq/*q--no intervening while resolve*/(Document.MSG_OPEN, arg);
					//System.out.println("opendoc DocInfo");
					DocInfo di = new DocInfo(uri);
					di.doc = br.getCurDocument();
					br.eventq/*q*/(Document.MSG_OPEN, di);    // send back around so can be short-circuited
				} catch (IllegalArgumentException ignore) {
					br.eventq(Browser.MSG_STATUS, "Bad URI: "+fixuri);
				}

				/*		} else if (arg instanceof URL) {
//System.out.println("URL => DocInfo "+arg);
			//SemanticEvent se = new SemanticEvent(br, "URI",arg);
			//arg = se.getArg();
			br.eventq/*q* /(Document.MSG_OPEN, new DocInfo((URL)arg));    // send back around so can be short-circuited
				 */
			} else if (arg instanceof DocInfo) {
				DocInfo di = (DocInfo)arg;
				//arg = br.callSemanticEvent("URI", arg);

				//if (DEBUG) System.out.println("br.open on DocInfo "+di.uri+", genre="+di.genre+", window="+di.window);
				if (di.window instanceof Browser) open(di, ((Browser)di.window));
				else if (di.window instanceof String) {
					Browser outbr = getGlobal().getBrowser((String)di.window);
					open(di, outbr);
				} else open(di, br);    // unrecognized object type
				//br.eventq(Document.MSG_OPENED, (DocInfo)arg);	   => done in Browser.open()-
			}
			/*
		else if (arg instanceof URL) br.open(Utility.robustURI((URL)arg));	// else report bogus event?
		else if (arg instanceof String) {
//try { System.out.println("open "+new URL(br.getURI(), (String)o)); } catch (Exception xxx) {}
//try { System.out.println(Utility.robustURI(new URL(br.getURI(), (String)arg))); } catch (MalformedURLException male2) { System.err.println("bad URL: "+male2); }
			try { br.open(Utility.robustURI(new URL(br.getURI(), (String)arg))); } catch (MalformedURLException male) { System.err.println("bad URL: "+male); }
		} else if (arg instanceof DocInfo) {
			// have URL, headers, and other things
DocInfo di = (DocInfo)arg;
		}
			 */
			/*
	} else if ("openIDocument"==msg) {
		VFrame frame = new VFrame("",null, null);	//(VFrame)Behavior.getInstance("multivalent.gui.VFrame",null, br.getCurDocument().getLayer(Layer.SCRATCH));
		Document idoc = new Document("IDOC",null, new Root(null,br));
		frame.setContent(idoc);
		new LeafUnicode("Loading "+arg,null, idoc);
		try {
			DocInfo di = new DocInfo();
			di.doc = idoc;
			di.uri = new URL((String)arg);
			br.eventq(Document.MSG_OPEN, ci);
		} catch (MalformedURLException male) {}
			 */
		} else if (Document.MSG_OPENED==msg) {
			if (arg instanceof DocInfo) {
				Document doc = ((DocInfo)arg).doc;
				if (!unform_.contains(doc)) unform_.add(doc); //System.out.println("queueing "+doc.getName()+"  "+doc.getFirstLeaf().getName()+".."+doc.getLastLeaf().getName());}
			}

		} else if (SystemEvents.MSG_GO_HOME==msg) {  // maybe just use "openDocument $pref.HOMEPAGE"
			//URL url = getClass().getResource("/sys/HomePage.html");
			URI uri = null; //Multivalent.HOMEPAGE;
			String home = getPreference("homepage", "systemresource:/sys/About.html");
			// maybe just let scripts handle this
			//System.out.println("*** HOMEPAGE = "+home);
			try { uri = new URI("file:/dir/con.txt").resolve(home); } catch (URISyntaxException badhome) { uri = Multivalent.HOME_SITE; }
			br.eventq(Document.MSG_OPEN, uri);

		} else if (Document.MSG_RELOAD==msg) {
			//System.out.println(msg+", "+curdoc.getURI());
			// kill cached version
			//getGlobal().getCache().expire(curdoc.getURI()/*, null, Cache.GROUP_GENERAL*/);
			//		cache.setSeen(uri.toString(), false); => not strong enough, as page can change but not date

			DocInfo di = new DocInfo(null/*curdoc.getURI()*/);
			//di.headers.put(HTTP.HEADER_CACHE_CONTROL, "must-revalidate");
			open(di, br);

		} else if (Layer.MSG_LOAD==msg)
			//System.out.println("loadLayer, uri="+arg+", doc="+se.getOut());
			try {
				URI uri = (URI)arg;
				Document doc = (Document)se.getOut();
				ESISNode proot = multivalent.std.adaptor.XML.parseDOM(uri);

				Layer newlay = (Layer)Behavior.getInstance(proot.getGI(),"Layer", proot, proot.attrs, doc.getLayers());
				//restoreChildren(proot, doclayers);
				//System.out.println("doc.size()=="+doc.size()+", "+doc.childAt(0)+", url="+newlay.getAttr("uri"));
				if (doc.size() > 0 && doc.childAt(0).isStruct()) {   // if document already running, instantiate
					newlay.buildBeforeAfter(doc);
					br.repaint(1000);
				}

				//		} catch (ClassCastException cce) {
				//		} catch (IOException ioe) {
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
			else if (Document.MSG_STOP==msg) {
				if (SystemEvents.DEBUG) System.out.println(Document.MSG_CLOSE+" on "+arg);
				if (arg instanceof Document) {
					Document doc = (Document)arg;
					if (SystemEvents.DEBUG) System.out.println("sending STOP to "+doc.getFirstLeaf());
					//doc.eventBeforeAfter(new TreeEvent(br, TreeEvent.STOP), br.getCurScrn());
					// bring back
					/*			TreeEvent stop = new TreeEvent(br, TreeEvent.STOP);
			Point pt = br.getCurScrn();
			for (Node n=doc.getFirstLeaf(),endn=doc.getLastLeaf(); n!=endn && n!=null; n=n.getNextLeaf()) {
//if ("img".equals(n.getName())) System.out.println("killing "+n.getAttr("src"));
				n.eventBeforeAfter(stop, null);
			}*/
				}

			} else if (Document.MSG_CLOSE==msg)
				/*if (!finished loading)*/ br.event/*not q, now!*/(new SemanticEvent(this, Document.MSG_STOP, arg));
			else if (IScrollPane.MSG_FORMATTED==msg) {
				//System.out.println(IScrollPane.MSG_FORMATTED+" on "+arg+" "+arg.getClass().getName());
				if (arg instanceof Document)
					//Document doc = (Document)arg;
					//System.out.println(IScrollPane.MSG_FORMATTED+" "+doc.getName()+", contains? "+unform_.contains(doc)+", vsb max="+doc.getVsb().getMax()+"  "+doc.getFirstLeaf().getName()+".."+doc.getLastLeaf().getName());
					if (unform_.contains(arg)) { unform_.remove(arg); br.eventq(Document.MSG_FORMATTED/*=> SystemEvents.MSG_FIRST_FULL_FORMATTTED ?*/, arg); }

			} else if (Document.MSG_REFORMAT==msg) {
				br.getDocRoot().markDirtySubtree(true);
				//curdoc.markDirtySubtree(true); -- ?
				br.repaint();

			} else if (Document.MSG_REPAINT==msg)
				br.repaint();
			else if (Browser.MSG_STATUS==msg /*&& false*/) {
				if (arg==null || arg instanceof String) getBrowser().showStatusX((String)arg);  // going away soon

			} else if (Browser.MSG_STATUS==msg)
				showStatus(arg);
			else if (IScrollPane.MSG_SCROLL_TO==msg) {	// can't dial direct sometimes because waiting for repaint on event queue to reformation document so can get right y position on node
				//System.out.println("scrollTo "+arg);
				Leaf s=null,e=null;
				if (arg instanceof Span) { Span span = (Span)arg; s=span.getStart().leaf; e=span.getEnd().leaf;
				} else if (arg instanceof Node) { Node node = (Node)arg; s = node.getFirstLeaf(); e = s;    //node.getLastLeaf(); too much
				} else if (arg instanceof Integer) {
					IScrollPane isp = se.getIn() instanceof Node? ((Node)se.getIn()).getIScrollPane(): curdoc;
					isp.scrollTo(0, ((Integer)arg).intValue());   //... which Document?

				} else if (arg instanceof Mark)
					s = e = ((Mark)arg).leaf;

				if (s!=null) {
					//System.out.println("scrollTo "+s);  //+" @ "+s.getRelLocation(s.getDocument()));
					//br.format();
					//s.scrollTo(true); // true=pickplace

					// highlight destination with the selection (make this a Preference)
					br.getSelectionSpan().moveq(s,0, e,e.size());
					s.scrollTo();
					br.repaint(50);
				}

			} else if (SystemEvents.MSG_DESTROY==msg) {
				if (arg instanceof Node) {
					((Node)arg).remove();
					br.repaint(100);

				} else if (arg instanceof Behavior)
					((Behavior)arg).destroy();

			} else if ("showHelp"==msg)
				if (arg!=null) {
					String helpname=null;
					if (arg instanceof String) helpname=(String)arg;
					else /*if (o instanceof Behavior)*/ {
						helpname=arg.getClass().getName();
						// for now, help on behaviors still on elib.cs
						StringBuffer st = new StringBuffer("http://elib.cs.berkeley.edu/java/help/");
						for (int i=0,imax=helpname.length(); i<imax; i++) { char ch=helpname.charAt(i); st.append(ch=='.'?'/':ch); }
						st.append(".html");
						helpname = st.toString();
					}
					// for now check for up-to-date copy on Internet first
					// later, be self contained by using local copy
					Browser helpbr = getGlobal().getBrowser("<HELP>");
					// compute URL from Behavior name = prefix + .=>/
					//System.out.println("opening "+helpname);
					try { helpbr.eventq(Document.MSG_OPEN, new URL(getClass().getResource("/help/Help.html"), helpname)); } catch (MalformedURLException male) { System.out.println("bad help URL "+helpname); }
				}
		//return true;	// getting away from semanticEventAfter shortcircuiting
		return false;
	}


	void open(DocInfo di/*, INode replaceroot--LATER, defaults to curDocument */, Browser br) {
		//System.out.println("*** open with incoming cookie = "+di.headers.get("COOKIE"));

		// clean up => Browser.reset(Document).  also needed by Multipage
		//br.reset(di.doc);
		//br.getSelectionSpan().moveq(null);    // if in Document to be replaced
		//br.getCurNode().moveq(null);
		//br.setCurNode()...
		// ...


		//if (openURI==null) return; => reload current
		//if (di.window==null) di.window=contentdoc;
		//Browser br = getBrowser();
		Document doc = di.doc;
		if (doc==null) doc = di.doc = br.getCurDocument();
		//System.out.println("br.open, doc="+(doc == br.getDocRoot().childAt(0)));

		URI openURI=di.uri, docURI = doc.uri;


		//docbox_.toFront(); => some Linux window managers will unmap first, which looks bad

		// Save old document.
		if (docURI!=null) {
			if (SystemEvents.DEBUG) System.out.println("closing "+docURI);
			br.event/*not q, now!*/(new SemanticEvent(this, Document.MSG_CLOSE, doc)); // give behaviors a chance to save state
		}


		// intradoc refs vs reload
		if (openURI==null || docURI==null || docURI.isOpaque() // invalid
				|| !docURI.getSchemeSpecificPart().equals(openURI.getSchemeSpecificPart())  // different URI (not counting fragment)
				|| di.attrs.get("POST")!=null || openURI.getQuery()!=null   // query
				//|| di.genre != doc.gen
				|| di.genre != null     // setting special genre
		) {//!docURI.sameFile(openURI)) {
			//System.out.println("loading "+openURI);

			// Create new document
			if (openURI==null) openURI = docURI;	// reload current page
			di.uri = openURI;
			// invalidate cache entry -- if reloads fine -- do elsewhere because may want to reload with same cache

			// move this to MediaLoader?
			//System.out.println("open(DocInfo) on "+openURI);

			///SAM
			/*try {
				Class disAnnos = Class.forName("uk.ac.liv.c3connector.DistributedPersonalAnnos");
				String curServer = (String) disAnnos.getDeclaredMethod("getCurrentRemoteServer").invoke(null);			
				if(curServer.equals("REST")){
					if(!openURI.toString().startsWith("systemresource:")){
						Class parameterTypes = Class.forName("java.lang.String");	
						Class parameterType2 = Class.forName("multivalent.Document");
						disAnnos.getDeclaredMethod("askForDocumentInfo", parameterTypes, parameterType2 ).invoke(null, openURI.toString(),doc);
	//					disAnnos.getDeclaredMethod("pageAccessed", parameterTypes ).invoke(null, openURI.toString());
					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			///
			
			if (handleScheme( di,  br, doc, openURI))
				return;

			if (openURI.isOpaque()) //doc.clear();
			{
				doc.putAttr(Document.ATTR_URI, openURI.toString());
				doc.uri = openURI;
				doc.putAttr(Document.ATTR_TITLE, openURI.toString());
				//new LeafUnicode("Some behavior should intercept generateDocument "+docURI,null, doc); -- everything additive, so this would stick around
				//boolean shortcircuit = event(new SemanticEvent(this, "generateDocument", di));
				//System.out.println("opaque: "+openURI);
				//if (shortcircuit) { doc.uri=null; return; }	// don't do Document.MSG_OPENED, Document.MSG_CURRENT

				br.event/*not q, now!*/(new SemanticEvent(this, Document.MSG_OPENED, di));
				//eventq(Document.MSG_OPENED, di);
				br.setCurDocument(doc);
			} else {
				//System.out.println("*** Browser opening URL "+di.uri+", genre "+di.genre);
				//restoreDocument(di, doc);
				//Layer bogus = new Layer("temporary",null, getLayer());
				MediaLoader loader = (MediaLoader)Behavior.getInstance("loader", "multivalent.std.MediaLoader",null, doc.getLayers()/*null/*no Layer, which is unusual*/);
				di.doc = doc;
				loader.setDocInfo(di);

				//doc.clear();
				//new LeafUnicode("Loading "+di.uri,null, doc);
				//new Thread(loader).start(); //-- later
				loader.load();	// synchronously for now
			}

		} //else System.out.println("\tequal");
	}

	/** handles schemes that are not handled by the default Java */

	private boolean handleScheme(DocInfo di, Browser br, Document doc, URI openURI) {
		String scheme =  openURI.getScheme();
		boolean isVFS = false;
		for (String s: MediaLoader.VFSschemes )
			if (scheme.equals(s))
				isVFS = true;
		if (isVFS){
			MediaLoader loader = (MediaLoader)Behavior.getInstance("loader", "multivalent.std.MediaLoader",null, doc.getLayers()/*null/*no Layer, which is unusual*/);
			di.doc = doc;
			loader.setDocInfo(di);
			loader.load();
			return true;
		}

		return false;
	}


	void showStatus(Object arg) {
		//Map anchors = (Map)getRoot().getVar(Document.VAR_ANCHORS);  // id's when ASCII
		//Object o = anchors.get("status");
		Node o = getRoot().findBFS(null, "id", "status");
		if (!(o instanceof INode)) return;

		INode p = (INode)o;

		Rectangle dimin = (Rectangle)p.bbox.clone();
		p.setValid(false);  // stop markDirty
		p.removeAllChildren();
		assert p.getParentNode().isValid(); // not up to root
		Node newn = null;
		if (arg==null)
			// clear
			newn = new LeafUnicode("", null, p);
		else if (arg instanceof String)
			// later: interpret as possible HTML
			newn = new LeafUnicode((String)arg, null, p);
		else if (arg instanceof Node) {
			newn = (Node)arg;
			p.appendChild(newn);
		}

		if (newn!=null) {
			Document doc = getRoot();
			Context cx = doc.getStyleSheet().getContext();
			p.formatBeforeAfter(dimin.width, dimin.height, cx);
			//p.reformat(newn);
			assert p.isValid() && newn.isValid();
			p.bbox.setBounds(dimin);
			p.repaint();  // now!
		}
	}
}
