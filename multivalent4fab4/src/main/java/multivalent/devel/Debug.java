package multivalent.devel;

import java.io.*;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;

import phelps.lang.Booleans;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.node.*;
import multivalent.gui.*;



/**
	Runtime behavior that can generate various dumps.

	@see multivalent.devel.ShowDocTree and other classes in the devel package

	@version $Revision: 1.6 $ $Date: 2003/06/02 05:12:51 $
*/
public class Debug extends Behavior {
  public static final String MSG_SET_MODE = "debugSetMode";

  public static final String PREF_MODE = "Debug/Monitor";

  /**
	Construct Debug menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Debug"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_DEBUG = "createWidget/Debug";


  static String[] titleev = {
	//"make a plain window", "debugMakeWindow",
	//"make portal", "debugMakePortal",

	//"Dump root (4 levels)", "debugDumpRoot", => ShowDocTree
	//"Dump span", "debugDumpSpan", => ShowDocTree
	//"find first invalid", "debugFindInvalid", => ShowDocTree
	//"Dump Current Node", "debugDumpNode", => ShowDocTree
	//"Initial Contexts", "debugInitialContexts", => drag in ShowDocTree
	//"Context at point", "debugContextAtPoint", => ShowDocTree

	//"antialiasing", "debugAA",
	"VALIDATE Document Tree", "debugValidate",	// in ShowDocTree, but keep for continuous checks (as on page load)
	"Stats over nodes", "debugNodesStats",
	"Stats on current node", "debugNodeStats",
	"Call GC", "debugGC",
	"<b>Dump misc</b>", "debugDumpMisc",
	"dump personal layer", "debugDumpPersonal",
	"Dump span loc", "debugDumpLoc",
	"<b>Dump Layers</b>", "debugDumpLayers",
	"Show profile stats", "debugShowProfile",
	"Reset profile stats", "debugResetProfile",
	"Reformat", "debugReformat",
	//"Reformat (Timed)", "debugReformatTimed", //=> save about half reformatting time by retaining valid leaves
	"Repaint", "debugRepaint",
	"leaf walk", "debugLeafWalk",
	"dump prefs", "debugDumpPrefs",
/**/
	"Dump root (3 levels)", "debugDumpRoot/3", //=> ShowDocTree
	"Dump root (4 levels)", "debugDumpRoot/4", //=> ShowDocTree
	"Dump root (5 levels)", "debugDumpRoot/5", //=> ShowDocTree
	"Dump doc tree (4 levels)", "debugDumpDoc/4", //=> ShowDocTree
	"Dump doc tree (20 levels)", "debugDumpDoc/20", //=> ShowDocTree
  };


  Node obs_ = null;
  boolean fsemev = false;
  boolean fpaint = false;


  private static NFont smallFont_ = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 9f);


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (fsemev) System.out.println("B: "+se);
	if (super.semanticEventBefore(se,msg)) return true;

	else if (!Booleans.parseBoolean(getPreference("DebugMode", "false"), false)) {}

	else if (MSG_CREATE_DEBUG==msg) {
		//boolean debugmode = getPreferenceBoolean("DebugMode", "false");
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();

		createUI("button", "ABORT", "event debugAbort", menu, null, false);
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Track Semantic Events", new SemanticEvent(br, "debugSemanticEvents",null), menu, null, false);
		cb.setState(fsemev);
		cb = (VCheckbox)createUI("checkbox", "Track Paint", new SemanticEvent(br, "debugPaint",null), menu, null, false);
		cb.setState(fsemev);

		for (int i=0,imax=titleev.length; i<imax; i+=2) {
			createUI("button", titleev[i], "event "+titleev[i+1], menu, null, false);
		}
		cb = (VCheckbox)createUI("checkbox", "Always Validate Document", new SemanticEvent(br, MSG_SET_MODE, null), menu, null, false);
		boolean state = Booleans.parseBoolean(getPreference(PREF_MODE, "true"), true);
		cb.setState(state);

	} else if (Browser.MSG_CREATE_TOOLBAR==msg) {
		createUI("button", "ABORT", "event debugAbort", (INode)se.getOut(), null, false);
		createUI("button", "AA"/*antialias*/, "event debugAA", (INode)se.getOut(), null, false);
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {

	if (fsemev) System.out.println("A: "+se);

//System.out.println("Debug, msg = "+msg);
//	if (!msg.startsWith("debug")) return false;	// quick fail

	Browser br = getBrowser();
	Document doc = br.getCurDocument();
	Node n=null; int offset=-1;
	INode p=null;
	Context cx;
	Span sel = br.getSelectionSpan();
	CursorMark curs = br.getCursorMark();
	Root root = br.getRoot();


	if (Document.MSG_CURRENT==msg) {
		if (obs_!=null) obs_.deleteObserver(this);
		Document curdoc = (Document)se.getArg();
		boolean mode = Booleans.parseBoolean(getPreference("DebugMode", "false"), false);
		//if (mode && curdoc!=null) { obs_=curdoc.getParentNode(); obs_.addObserver(this); }

	} else if ("debugGC"==msg) {
		Runtime.getRuntime().gc();
		br.repaint();

	} else if ("debugMakeWindow"==msg) {
		//Layer l = doc.getLayer(Layer.PERSONAL);
		VFrame win = new VFrame("Note198372342",null, doc);	//(VFrame)Behavior.getInstance("multivalent.gui.VFrame",null, l);
		//win.setLocation(100,100);
		win.setBounds(200,75, 300,200);
		win.setTitle("Test Window");

		Document ndoc = new Document("Note198372342",null, win, br);
		//new LeafUnicode("this space intensionally left blank",null, win);
		//win.setContent(ndoc);

		doc.repaint();

		DocInfo di = null;
		try { di = new DocInfo(new URI(getClass().getResource("/sys/Release.html").toString())); } catch (URISyntaxException canthappen) {}
		di.doc = ndoc;
		br.eventq(Document.MSG_OPEN, di);

/*	} else if ("debugMakePortal"==msg) {
		VFrame win = new VFrame("PORTALFRAME",null, root);
		win.setBounds(100,100, 300,300);
		win.setTitle("PORTAL");

		phelps.lens.Portal portal = new phelps.lens.Portal("PORTAL",null, win);
		portal.setDocument(doc);

		doc.repaint();
*/
	} else if ("debugDumpPersonal"==msg) {
		Layer personal = doc.getLayer(Layer.PERSONAL);
		StringBuffer sb = new StringBuffer(10000);
		sb.append("*** PERSONAL LAYER ***\n");
		personal.save(/*sb,0*/).writeXML();
		//System.out.println(sb);

/*	} else if ("debugDumpRoot"==msg) {
		root.dump(5);
*/
	} else if (msg!=null && msg.startsWith("debugDumpDoc")) {
		int levels = 4;
		int inx = msg.indexOf('/'); if (inx!=-1) try { levels=Integer.parseInt(msg.substring(inx+1)); } catch (NumberFormatException ignore) {}
		doc.dump(levels);

	} else if (msg!=null && msg.startsWith("debugDumpRoot")) {
		int levels = 4;
		int inx = msg.indexOf('/'); if (inx!=-1) try { levels=Integer.parseInt(msg.substring(inx+1)); } catch (NumberFormatException ignore) {}
		root.dump(levels);

/*
	} else if ("debugDumpNode"==msg) {
		//((Note)br.vlensactive.get(0)).root.dump(); -- used to be Dump Current Note
		if (curs.isSet()) {
			n=curs.getStart().leaf; offset=curs.getStart().offset;
			n.dump(); System.out.println("Actives = "+n.getActivesAt(offset));
		} else if (sel.isSet()) {
			n=sel.getStart().leaf; offset=sel.getStart().offset;
			Node endn=sel.getEnd().leaf; int endi=sel.getEnd().offset;
			System.out.println("show "+n.getName()+" .. "+endn.getName());
			while (n!=endn || offset!=endi) {
				//n.dump();
				System.out.println(n.getName()+"/"+offset+", actives = "+n.getActivesAt(offset));
				if (offset < n.size()) offset++; else { n=n.getNextLeaf(); offset=0; }
			}
		}
*/
	} else if ("debugAbort"==msg) {
		System.exit(0); // just a quick, clean shutdown, which C-c from Cygwin doesn't do

	} else if ("debugAA"==msg) {
		NFont.setUseBitmaps(!NFont.isUseBitmaps());
		br.repaint(100);

	} else if ("debugDumpPrefs"==msg) {
		System.out.println("PREFERENCES");
		Multivalent m = getGlobal();
		for (Iterator<String> i=m.prefKeyIterator(); i.hasNext(); ) {
			String key = i.next();
			System.out.println(key+" => "+m.getPreference(key,null));
		}

/*	} else if ("debugDumpSpan"==msg) {
		if (sel.isSet()) {
//		   selchunks=null;
//		   Node span[] = br.getSelchunks();
		   Node[] span = Node.spanChunky(br.getSelectionSpan().getStart(), br.getSelectionSpan().getEnd());
			for (int i=0; i<span.length; i++) span[i].dump();
		} else System.out.println("Dumps subtrees in span => need to select a range");
*/
	} else if ("debugDumpLayers"==msg) {
		doc.getLayers().dump();

	} else if ("debugDumpLoc"==msg) {
		if (sel.isSet()) {
//			System.out.println(RobustLocation.descriptorFor(sel.getStart()));
//			if (sel.getEnd()!=sel.getStart()) System.out.println("\n"+RobustLocation.descriptorFor(sel.getEnd()));
		} else System.out.println("Locations reported on endpoints of selection => need to select a range");

	} else if ("debugDumpMisc"==msg) {
		// maybe each Behavior should give access to dump info through regular UI mechanisms
		// so this should go in Browser
		// way to zap Debug menu
		// random information, like root+attributes, root's first child, whatever
		System.out.println(
			"root = "+br.getRoot()+"\n\tobservers: "+br.getRoot().getObservers()
			+"\nURI="+doc.getURI()+", pagenum="+doc.getAttr("page")
			+"\nattrs: "+doc.getAttributes()
			+"\ncurdoc = "+doc
		);
		System.out.println("curdoc = "+doc.getName()+", valid="+doc.isValid()+", bbox="+doc.bbox+", #children="+doc.size());

		/*System.out.println("Style Sheet:	");
		StyleSheet ss = doc.getStyleSheet();
		for (Iterator<Map.Entry<>> i=ss.key2cl_.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<> e = i.next();
			System.out.println("  "+e.getKey()+" => "+e.getValue());
		}*/

		System.out.print("Document Layers, immediate (not nested):");
		Layer pl = doc.getLayers();
		//for (Iterator<Behavior> i=pl.behaviorIterator(); i.hasNext(); ) {
		for (int i=0,imax=pl.size(); i<imax; i++) System.out.print("  "+pl.getBehavior(i).getName());
		System.out.println();

		System.out.print("Personal Layer:");
		pl = doc.getLayer(Layer.PERSONAL);
		for (int i=0,imax=pl.size(); i<imax; i++) System.out.print("  "+pl.getBehavior(i).getName());
		System.out.println();

//			+", br.xoff="+br.xoff+", yoff="+br.yoff
//+", docwidth="+br.docwidth+", docheight="+br.docheight
		if (curs.isSet()) {
			List<ContextListener> vactives = curs.getMark().leaf.getActivesAt(curs.getMark().offset);
			System.out.println("ACTIVE BEHAVIORS");
			for (int i=0,imax=vactives.size(); i<imax; i++) {
				System.out.println(vactives.get(i));
			}
		}

/* drag on ShowDocTree
	} else if ("debugInitialContexts"==msg) {
		List<Node> v = new ArrayList<Node>(20);
		v.add(br.getRoot());
		while (v.size()>0) {
			n = (Node)v.get(0);
			v.remove(0);
			if (n.isStruct()) {
				p = (INode)n;
				for (int i=0,imax=p.size(); i<imax; i++) v.add(p.childAt(i));
			} //else {

				cx = new Context();
				cx.reset(n);
				System.out.print("at "+n+", context.reset has active spans: ");
				for (int i=0,imax=cx.vactive_.size(); i<imax; i++) System.out.print(cx.vactive_.get(i)+"	");
				System.out.println();
			//}
		}
	} else if ("debugContextAtPoint"==msg) {
		if (sel.isSet()) {
			Mark m = sel.getStart();
			//n = sel.getStart().leaf;
			cx = new Context(); /*cx.styleSheet=br.getStyleSheet();* / cx.reset(m.leaf, m.offset);
			System.out.println("at "+sel.getStart()+", context.reset has active spans: ");
			for (int i=0,imax=cx.vactive_.size(); i<imax; i++) {
				ContextListener cl = (ContextListener)cx.vactive_.get(i);
				System.out.println("\t"+i+". "+cl+", priority="+cl.getPriority());
			}
//			System.out.println("\tCONTEXT OBJECT: "+cx);

			//n = sel.getEnd().leaf;
			m = sel.getEnd();
			cx = new Context(); /*cx.styleSheet=br.getStyleSheet();* / cx.reset(m.leaf, m.offset);
			System.out.print("at "+sel.getEnd()+", context.reset has active spans: ");
			for (int i=0,imax=cx.vactive_.size(); i<imax; i++) System.out.print(cx.vactive_.get(i)+" ");
			System.out.println("\n\t"+cx);
		}
*/

	} else if ("debugValidate"==msg) {
		System.out.println("*** Validation trace");

		try {
			br.checkRep();
		} catch (AssertionError ae) {
			//System.err.println(ae);
			ae.printStackTrace();
		}

		System.out.println("*** END Validation trace");


	} else if ("debugNodeStats"==msg) {
		Mark m=null;
		if (curs.isSet()) m=curs.getMark();
		else if (sel.isSet()) m=sel.getStart();
		if (m!=null) {
			n=m.leaf; offset=m.offset;
			System.out.println("At node |"+n.getName()+"|, offset="+offset+", class="+n.getClass().getName()+", hashcode="+n.hashCode());
			System.out.println("attrs = "+n.getAttributes());
			//System.out.println("stickies: "+n.sticky_);
			System.out.println("bbox = "+n.bbox+", baseline="+n.baseline+", valid="+n.isValid()+", dx="+n.dx()+", dy="+n.dy()+", break before="+n.breakBefore()+"/after="+n.breakAfter());
			System.out.println("getActivesAt: "+n.getActivesAt(offset));
			Document ndoc = n.getDocument(); StyleSheet nss = ndoc.getStyleSheet(); Context ncx = nss.getContext();
			ncx.reset(n, offset);
			System.out.println("  context = "+ncx);
			System.out.println("getActivesAt(-1): "+n.getActivesAt(-1));
		}

	} else if ("debugNodeStats"==msg) {
		//nodeStats(getRoot());
		int icnt=0, lcnt=0, iscnt=0, lscnt=0, zscnt=0;
		List<Node> q = new ArrayList<Node>(1000);
		q.add(getBrowser().getRoot());
		while (q.size()>0) {
			n = (Node)q.remove(0);
			if (n.isStruct()) {
				INode in=(INode)n;
				icnt++;
				iscnt += n.sizeSticky();
				//if (n.sticky_!=null && n.sticky_.size()==0) zscnt++; -- don't have this info
				for (int i=0,imax=in.size(); i<imax; i++) q.add(in.childAt(i));
			} else {
				Leaf il=(Leaf)n;
				lcnt++;
				lscnt += n.sizeSticky();
			}
		}
		System.out.println(icnt+" internal nodes, with "+iscnt+" stickies"); //(avg="+(icnt/iscnt)+")");
		System.out.println("\t"+zscnt+" empty stickies");
		System.out.println(lcnt+" leaf nodes, with "+lscnt+" stickies");// (avg="+(lcnt/lscnt)+")");

	} else if ("debugShowProfile"==msg) {
		br.resetProfile();

	} else if ("debugResetProfile"==msg) {
		StringBuffer sb = new StringBuffer(4000); br.showProfile(sb); System.out.println(sb);

	} else if ("debugReformat"==msg) {
		br.getRoot().markDirtySubtree(true);
		//br.format();    // explicitly now so can time (vs catch during paint)
		br.repaint();

	} else if ("debugReformatTimed"==msg) {
		int z=100;
		long start=System.currentTimeMillis();
		for (int i=0; i<z; i++) {
			br.getRoot().markDirtySubtree(true);
			br.format();    // explicitly now so can time (vs catch during paint)
		}
		long end=System.currentTimeMillis();
		System.out.println("FULL reformatting took "+((end-start)/z)+" ms (for each of "+z+" iterations)");


		start=System.currentTimeMillis();
		for (int i=0; i<z; i++) { br.getRoot().markDirtySubtree(false); br.format(); }
		end=System.currentTimeMillis();
		System.out.println("SKIP VALID LEAF NODES reformatting took "+((end-start)/z)+" ms");

		br.repaint();

	} else if ("debugRepaint"==msg) {
		br.repaint(50);

/* moved to show doc tree display
	} else if ("debugFindInvalid"==msg) {
		int ctr=0;
		LinkedList<Node> q = new LinkedList<Node>();
		q.add(br.getRoot());
		while (q.size()>0) {
			ctr++;
			Node now = (Node)q.removeFirst();
			if (!now.isValid()) {
				System.out.println("INVALID: "+now);
				for (p=now.getParentNode(); p!=null; p=p.getParentNode()) System.out.println(p);
			} else if (now.isStruct()) {
				INode inow = (INode)now;
				for (int i=0,imax=inow.size(); i<imax; i++) q.add(inow.childAt(i));
			}
		}
		System.out.println("searched "+ctr+" nodes");
*/
	} else if ("debugLeafWalk"==msg) {
		for (Leaf l = br.getCurDocument().getFirstLeaf(); l!=null; l=l.getNextLeaf()) System.out.println(l);

	} else if (MSG_SET_MODE==msg) {
		boolean state = Booleans.parseBoolean(getPreference(PREF_MODE, "false"), false);
		state = Booleans.parseBoolean(se.getArg(), !state);
		putPreference(PREF_MODE, (state? "true": "false"));
		if (state) br.eventq("debugValidate", null);

	} else if ("debugSemanticEvents"==msg) {
		fsemev = !fsemev;
	} else if ("debugPaint"==msg) {
		fpaint = !fpaint;

//	} else if (Document.MSG_OPENED==msg) { // should be after formatting...

	//} else if (IScrollPane.MSG_FORMATTED==msg) { // should be after formatting...
	} else if (Document.MSG_FORMATTED==msg) { // should be after formatting...
		if (Booleans.parseBoolean(getPreference(PREF_MODE, "true"), true)) {
			//br.eventq("debugValidate", null);
			//((Node)se.getArg()).checkRep(); System.out.print("V");
		}
	}

	return super.semanticEventAfter(se,msg);
  }

  public void nodeStats(Node n) {
	if (n.isStruct()) {
	} else {
	}
  }


  public void buildAfter(Document doc) {
	doc.getRoot().addObserver(this);
	super.buildAfter(doc);
  }

  public boolean paintBefore(Context cx, Node node) {
	if (fpaint) System.out.println("paint B: "+cx.g.getClip());
	return super.paintBefore(cx, node);
  }

  /**
	Display memory use, free memory, ....
	Only active when in debug mode.
  */
  public boolean paintAfter(Context cx, Node node) {
	if (fpaint) System.out.println("paint A: "+cx.g.getClip());

	Runtime rt = Runtime.getRuntime();
	Graphics2D g = cx.g;
	g.setColor(Color.BLACK);
	NFont f = smallFont_;
	int x = node.bbox.width - 75;
	long tot=Math.round((rt.totalMemory()*10) / (1024*1024)), free=Math.round((rt.freeMemory()*10) / (1024*1024));
	f.drawString(g, "Mem "+tot, x, 50);
	f.drawString(g, "Free "+free, x, 65);

	return super.paintAfter(cx, node);
  }

}
