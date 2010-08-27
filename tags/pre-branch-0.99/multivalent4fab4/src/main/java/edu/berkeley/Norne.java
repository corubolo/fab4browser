package edu.berkeley;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import multivalent.Behavior;
import multivalent.CLGeneral;
import multivalent.ContextListener;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.StyleSheet;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.std.span.FontSpan;



/**
	Format SEC 10-K filings to highlight footnotes, where all the secrets are.

<pre>
Date: Sun, 07 Apr 2002 15:16:38 -0700
From: Hal Varian <hal@sims.berkeley.edu>

...

Take a look at http://www.sims.berkeley.edu/~hal/footprint

After you read that, you might understand the following suggestion:

it would be a cute demo to write a hack that would allow you to browse
10ks from
edgar.sec.gov and display them either in the "normal" way with 10 point
text and 8 point footnotes or in the "norne" form with 15-point
footnotes and 8 point text.  (To understand "norne", spell it
backwards.)
</pre>

	<p>Written 2002 April 7, 11:50pm - 12:32am.

	@version $Revision: 1.2 $ $Date: 2003/02/09 01:03:54 $
 */
public class Norne extends Behavior {
	public static final String MSG_NORNE = "norne";

	private boolean active_ = true;

	/** Record formatting, so can quickly remove. */
	private List<Span> formatting_ = new ArrayList<Span>(20);


	/** Apply markup after building content. */
	@Override
	public void buildAfter(Document doc) {
		if (active_) markup(doc);
	}


	/** Search for footnotes and add markup. */
	void markup(Document doc) {
		Leaf startl=doc.getFirstLeaf(), endl=doc.getLastLeaf();
		Layer scratch = doc.getLayer(Layer.SCRATCH);

		// search for footnotes
		for (Leaf l=startl; l!=null && l!=endl; l=l.getNextLeaf()) {
			String txt = l.getName();
			if (txt.indexOf("<FN>")!=-1)
				// down to end of some tag or start of next <PAGE>
				for (Leaf e=l.getNextLeaf(); e!=null && e!=endl; e=e.getNextLeaf()) {
					txt = e.getName();
					if (txt.indexOf("</")!=-1 || txt.indexOf("<PAGE>")!=-1) {   // end of footnote
						e = e.getPrevLeaf();    // went one word too far
						Span fn = (Span)getInstance("highlight", "multivalent.Span", null, scratch);
						fn.moveq(l,0, e,e.size());
						formatting_.add(fn);
						l = e;
						break;
					}
				}
		}

		if (formatting_.size() == 0) return;     // must not have been SEC 10-K


		// 8-point body
		FontSpan body = (FontSpan)getInstance("body", "multivalent.std.span.FontSpan", null, scratch);
		body.size = 8.0f;
		body.moveq(startl,0, endl,endl.size());
		formatting_.add(body);

		// set footnote formatting_ via stylesheet, rather than on each footnote instance
		// This could (should) be set in user's external style sheet.
		StyleSheet ss = doc.getStyleSheet();
		CLGeneral cl = new CLGeneral(ContextListener.PRIORITY_SPAN);
		cl.setSize(12f);
		cl.setForeground(Color.MAGENTA);
		ss.put("highlight", cl);
	}


	/** Quickly remove markup to restore normal formatting. */
	void unmarkup() {
		for (int i=0,imax=formatting_.size(); i<imax; i++) formatting_.get(i).moveq(null);
		formatting_.clear();
	}


	/** Add checkbox control to menu. */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (super.semanticEventBefore(se, msg)) return true;
		else if (VMenu.MSG_CREATE_VIEW==msg) {
			VCheckbox cb = (VCheckbox)createUI("checkbox", "Norne formatting", "event "+Norne.MSG_NORNE, (INode)se.getOut(), null, false);
			cb.setState(active_);
		}
		return false;
	}

	/** Handle semantic event (from menu or potentially another behavior). */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (Norne.MSG_NORNE==msg) {
			active_ = !active_;

			// quickly update formatting to correspond to new setting
			Document doc = getDocument();
			if (active_) markup(doc); else unmarkup();
			doc.markDirtySubtree(true);
			doc.repaint(250);
		}
		return super.semanticEventAfter(se, msg);
	}
}
