package multivalent.std;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Map;

import multivalent.*;



/**
	User hubs are loaded after built-in hubs, so need way to delete unwanted built-in behaviors.
	Used by setting whitespace-separated list as content in hub.
	Names can be within current Layer, or fully qualified to kill from any Layer.

<!--
	or programmatically with semantic event "deleteBehavior", arg=behavior name as <String>/names in <List>, out=Document;
	Works, but need to generalized, match attributes as well as GI, edit attributes, ....
-->

	@see multivalent.Behavior#getInstance(String, String, Map, multivalent.Layer)

	@version $Revision: 1.2 $ $Date: 2003/06/02 05:44:13 $
*/
public class DeleteBehavior extends Behavior {
/*
  public final String MSG_DELETE = "deleteBehavior";
*/
  //protected List<> kill_ = null;


/*  public void buildBefore(Document doc) {
//System.out.println("kill these: "+kill_);
	Layer l = getLayer();
	if (kill_!=null) for (int i=0,imax=kill_.size(); i<imax; i++) {
		Behavior be = l.findBehavior((String)kill_.get(i));
		if (be!=null) be.getLayer().removeBehavior(be);
		System.out.println((be!=null? "\tkill: ": "\tnot found: ")+kill_.get(i));
	}
  }*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	if (n!=null) {
		//kill_ = new ArrayList<>(10);
		List<Object> q = new ArrayList<Object>(20);
		q.add(n);
		while (q.size()>0) {
			Object e = q.remove(q.size()-1);
			if (e instanceof String) {
				for (StringTokenizer st = new StringTokenizer((String)e); st.hasMoreTokens(); ) {
					//kill_.add(st.nextToken());
					// user hub loaded after system, which is to say last, so rock on
					Behavior be = layer.findBehavior(st.nextToken());
					if (be!=null) be.getLayer().removeBehavior(be);
				}
			} else {
				ESISNode p = (ESISNode)e;
				for (int i=0,imax=p.size(); i<imax; i++) q.add(p.childAt(i));
			}
		}
	}
  }

/*
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_DELETE==msg) {
		Object arg=se.getArg(), out=se.getOut();
		if (arg instanceof String) {
			String name=(String)arg;
			Document doc = (out instanceof Document ? (Document)out: getDocument());
System.out.println("DeleteBehavior "+name);
			Behavior be = doc.getLayers().findBehavior(name);
			if (be!=null) be.getLayer().removeBehavior(be);
		}
	} else return super.semanticEventAfter(se, msg);

	return false;
  }
*/
}
