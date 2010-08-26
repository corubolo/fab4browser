package multivalent.std;

//import java.awt.Color;
import java.util.regex.*;
import java.util.Map;

import multivalent.*;
import multivalent.node.LeafText;
import multivalent.std.span.BoxSpan;
import multivalent.std.ui.Multipage;



/**
	Find words in tree, highlight matches.

	Set colors and other properties through style sheets.

	by augmenting different genre hubs, can focus.

	<p>Need Preferences panel to edit URI patterns and search patterns.

	@see java.util.regex.Pattern
	@see java.net.URI

	@version $Revision: 1.3 $ $Date: 2002/02/14 19:38:51 $
*/
public class Autosearch extends Behavior {
  /** Pattern for words to match. */
  public static final String ATTR_REGEX = "regex";
  /** Show hits as 'box', else assumed generic. */
  public static final String ATTR_TYPE = "type";
  /** Limit to URIs matching this regex.  (Alternatively, put in PerPage hierarchy.) */
  public static final String ATTR_URI = "uri";
  public static final String ATTR_TAG = "tag";
  //public static final String ATTR_COLOR = "color"; => set in style sheet


  static final Pattern TYPES = Pattern.compile("(?i)default|box", Pattern.CASE_INSENSITIVE);


  Pattern pat_ = null;    // matcher instead?
  //Color color_ = CLGeneral.COLORINVALID;  //Color.RED; => stylesheet
  String type_ = null;    // => stylesheet
  Pattern uripat_ = null;
  String tag_ = "autosearch";
  //boolean active_=true;


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	/*else if (VMenu.MSG_CREATE_VIEW==msg) {
		INode menu = (INode)se.getOut();
		// choose which of following depending on if page is already in bookmark?  may want to file in a couple of places
		// "Show HTTP headers"
		VCheckbox ui = (VCheckbox)createUI("checkbox", "Autosearch", "event "+Search.MSG_AUTOSEARCH_TOGGLE, menu, null, false);
		ui.setState(active_);

	}*/ else if (Document.MSG_OPENED==msg || Multipage.MSG_OPENEDPAGE==msg) {
		//Browser br = getBrowser();
//System.out.println("autosearch "+active_+" for |"+getPreference("autosearch:for")+"|");
		//if (active_) br.eventq(Search.MSG_SEARCH_FOR, getPreference("autosearch:for", "Multivalent"));
		if (pat_ != null) {
			DocInfo di = (DocInfo)se.getArg();
			Document doc = di.doc;
//System.out.println("uri = "+di.uri+", uripat_="+(uripat_==null?"null":uripat_.pattern()));
			if (uripat_==null || uripat_.matcher(di.uri.toString()).find()) {
//System.out.println("autosearch "+doc.getName()+"/"+doc.getFirstLeaf().getName()+"  for  "+pat_.pattern());
				Layer scratch = doc.getLayer(Layer.SCRATCH);
				/*int hits =*/ searchSubtree(doc, pat_.matcher(""), scratch);
				// could show some flag if have hits
			}
		}

	} //else if (Search.HITS==msg && mine) {}	-- maybe show as orange background or something

	return false;
  }

  int searchSubtree(INode p, Matcher m, Layer scratch) {
	int hits=0;

	for (int i=0,imax=p.size(); i<imax; i++) {
		Node n = p.childAt(i);
		if (n.isStruct()) {
			hits += searchSubtree((INode)n, m, scratch);

		} else if (n instanceof LeafText) {
			m.reset(n.getName());
			if (m.find()) {
				Span newspan = null;
				if ("box"==type_) {
//System.out.println("hit "+n.getName());
					newspan = (BoxSpan)Behavior.getInstance(tag_, "multivalent.std.span.BoxSpan", null, scratch);	//layer_);
					//boxspan.setColor(color_);
					//newspan = boxspan;
				} else {
					newspan = (Span)Behavior.getInstance(tag_, "multivalent.Span", null, scratch);	//layer_);
				}

				//newspan.moveq(n,0, n,Math.max(searchlist[i].length(),n.size()));
				newspan.moveq((Leaf)n,m.start(), (Leaf)n,m.end());
				hits++;
			}
		}
	}
	return hits;
  }

/*
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	// update last visited field, if new page is bookmarked
	if ("toggleAutosearch"==msg) {
		active_ = !active_;
	}
	return super.semanticEventAfter(se,msg);
  }*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	String arg;
	if ((arg = getAttr(ATTR_REGEX))!=null) {
		try { pat_ = Pattern.compile(arg); } catch (PatternSyntaxException bad) { System.err.println("bad regexp: "+arg); }
	}

	//if ((arg = getAttr("color"))!=null) color_ = Colors.getColor(arg, Color.RED);  // will take color but prefers stylesheet, which can specify much more

	if ((arg = getAttr(ATTR_TYPE))!=null && TYPES.matcher(arg).matches()) type_ = arg.toLowerCase().intern();

	if ((arg = getAttr(ATTR_URI))!=null) {
		try { uripat_ = Pattern.compile(arg); } catch (PatternSyntaxException bad) { System.err.println("bad URI regexp: "+arg); }
	}

	tag_ = getAttr(ATTR_TAG, "autosearch").toLowerCase();  // so can control with stylesheet
  }
}
