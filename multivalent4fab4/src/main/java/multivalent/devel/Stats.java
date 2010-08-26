package multivalent.devel;

import java.util.*;

import multivalent.*;
import phelps.util.Units;



/**
	General statistics: memory use, ... (behaviors can dump whatever behavior-specific information)
	LATER maybe make into monitor window, like system load monitors.

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:15:31 $
*/
public class Stats extends Behavior {
  /**
	Request to dump various statistics to stdout.
	<p><tt>"statsDump"</tt>.
  */
  public static final String MSG_DUMP = "statsDump";


/* UI created elsewhere
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (Browser.MSG_CREATE_TOOLBAR==msg) {
		createUI("menubutton", " <= ", "event "+MSG_DUMP, se.getOut(), null, false);
	}
  }
*/
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_DUMP==msg) dump();
	return super.semanticEventAfter(se,msg);
  }

  void dumpAttrs(Layer layer, String title) {
	System.out.println("*** "+title+" ***");

	List<Layer> layers = new ArrayList<Layer>(10);
	layers.add(layer);

	int becnt=0, attrcnt=0;
	while (!layers.isEmpty()) {
		Layer l = layers.remove(0);
		for (int i=0,imax=l.size(); i<imax; i++) {
			Behavior b = l.getBehavior(i);
			System.out.print(b.getName());
			Map attrs = b.getAttributes();
			if (attrs!=null) {
				int size = attrs.size();
				System.out.print("	"); System.out.print(size);
				if (size>0) {
					for (Iterator<Map.Entry<String,Object>> ai=attrs.entrySet().iterator(); ai.hasNext(); ) {
						Map.Entry<String,Object> e = ai.next();
						System.out.print(",  "+e.getKey()+"="+e.getValue());
					}
				} else System.out.print("\u0007");
			}
			System.out.println();
			if (b instanceof Layer) layers.add((Layer)b);
		}
	}
  }

  void dumpTree(INode p) {
	int ccnt=0, acnt=0;
	for (int i=0,imax=p.size(); i<imax; i++) {
		Node n = p.childAt(i);
		if (!(n instanceof INode)) {
			Map attrs = n.getAttributes();
		}
	}
	for (int i=0,imax=p.size(); i<imax; i++) {
		Node n = p.childAt(i);
		if (n instanceof INode) dumpTree((INode)n);
	}
  }

  // if external data that's not essential and could be time consuming to load, get it in a new thread
  // => should be observer on Browser (which should be an Observable)
  public void dump() {
	Browser br = getBrowser();

	// Memory use
	System.out.println(new Date()); System.out.println();
	Runtime rt = Runtime.getRuntime();
	//System.out.println("Memory (pre-gc): "+(rt.freeMemory()+1024)/1024 +" K free, "+ (rt.totalMemory()+1024)/1024 + " K total");
	System.out.println("Memory (pre-gc): " + Units.prettySize(rt.freeMemory()) +" free, "+ Units.prettySize(rt.totalMemory()) + "  total");
	System.gc();
	//System.out.println("Memory (post-gc): "+(rt.freeMemory()+1024)/1024 +" K free, "+ (rt.totalMemory()+1024)/1024 + " K total");
	System.out.println("Memory (post-gc): " + Units.prettySize(rt.freeMemory()) +" free, "+ Units.prettySize(rt.totalMemory()) + " total");

	//System.out.println("\nObservers on Timer");
	//System.out.println(getControl().getTimer().observers_);
	/*Root root = br.getRoot();
	long size = root.computeMemoryImage();
	System.out.println("Tree memory use: "+(size+1024)/1024+" K +");*/
	/*
	for (int i=0,imax=root.size(); i<imax; i++) {
		Node child = root.childAt(i);
		System.out.println("   "+child+": "+child.getMemoryImage());
	}*/


	// Attribute use by behaviors: >=1 (ATTR_BEHAVIOR), max 7
	dumpAttrs(getDocument().getRoot().getLayers(), "System");
	dumpAttrs(getDocument().getLayers(), "Document");
  }
}
