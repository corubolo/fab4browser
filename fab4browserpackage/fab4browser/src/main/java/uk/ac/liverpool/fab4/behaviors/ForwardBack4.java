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

package uk.ac.liverpool.fab4.behaviors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.SemanticEvent;
import multivalent.std.ui.ForwardBack;
import phelps.lang.Maths;

//import multivalent.std.ui.DocumentPopup;

///////////      NOTE: changes are only to PRIVATE->PUBLIC !
//////////       The logic is implemented in UiBehavior

/**
 * Move forward and backward through documents browsed, via toolbar buttons,
 * menubar items, document popup. Can't have generic scriptable UI on toolbar
 * buttons as want to have button list pages so can skip intermediate.
 * <p>
 * To do: update this for FRAMEs => store history in Document's parent?
 * 
 * @version $Revision$ $Date$
 */
public class ForwardBack4 extends ForwardBack {
	static final String BACKWARDEVENT = "event " + ForwardBack.MSG_BACKWARD,
	FORWARDEVENT = "event " + ForwardBack.MSG_FORWARD;

	public List<DocRec> pages_ = new ArrayList<DocRec>(100);
	/** Position in pages_ list, which is < pages_.size() if went backward. */
	public int pagesi_ = -1;
	DocRec docnow_ = null, doclast_ = null;
	Document rootdoc_ = null;

	private boolean skipit_ = false;
	public boolean isNext = false, isPrev = false;

	public static class DocRec {
		public URI uri; // was: Object for URI or String
		public String title = null;
		public String target = null; // for frames, named window
		public int yposn = -1;
		public String page = null; // page number, or subpart of big document

		DocRec(URI uri) {
			this.uri = uri;
			title = uri.toString();
		}

		@Override
		public String toString() {
			return title + "  /  " + uri + ", y=" + yposn;
		}
	}

	@Override
	public void moveDelta(int delta) {
		// System.out.println("moveDelta "+delta+", now="+pagesi_+" vs
		// max="+(pages_.size()-1));
		moveTo(Maths.minmax(0, pagesi_ + delta, pages_.size() - 1));
	}

	@Override
	public void moveTo(int newpagesi) {
		// System.out.println("now="+pagesi_+" vs opening #"+newpagesi);//+" =
		// "+pages_.get(newpagesi));
		if (newpagesi != pagesi_) {
			// open new
			pagesi_ = newpagesi;
			docnow_ = pages_.get(pagesi_);

			Browser br = getBrowser();
			if (docnow_.page != null)
				getDocument().putAttr("page", docnow_.page);
			else
				getDocument().removeAttr("page");
			skipit_ = true; // ignore own openDocument event
			br.eventq(Document.MSG_OPEN, docnow_.uri);
			// br.eventq(IScrollPane.MSG_SCROLL_TO, xxx);
		}
	}

	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		isNext = pagesi_ + 1 < pages_.size();
		isPrev = pagesi_ > 0;

		// record all documents opened
		if (super.semanticEventBefore(se, msg))
			return true;
		else if (Document.MSG_CLOSE == msg)
			if (doclast_ != null) {
				// record y scroll position
				Document doc = (Document) se.getArg();
				if (doc != null) {
					// docnow_.yposn = (ir!=null? ir.getVsb().getValue(): -1);
					doclast_.yposn = doc.getVsb().getValue();
					doclast_.title = doc.getAttr(Document.ATTR_TITLE);
					doclast_.page = doc.getAttr(Document.ATTR_PAGE);
				}
			}

		return false;
	}

	/*
	 * "redirecteDocument" cancels latest in list of previous pages<br /> {@link
	 * Document.MSG_OPENED} truncates list and adds that document<br />
	 * "forwardDocument"/"backwardDocument"<br /> "openDocuments" adds the list
	 * in getArg to list
	 */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Object arg = se.getArg();

		if (Document.MSG_REDIRECTED == msg) {
			if (pagesi_ >= 0) {
				pages_.remove(pagesi_);
				pagesi_--;
			}

		} else if (ForwardBack.MSG_OPEN_DOCUMENTS == msg) {
			if (arg instanceof List) {
				// pages_.removeRange(pagesi_+1, pages_.size()-1); // truncate
				for (int i = pages_.size() - 1; i > pagesi_; i--)
					pages_.remove(i);

				// pages_.addAll((List)arg);
				List<Object> newpages = (List<Object>) arg;
				System.out.println("openDocuments in f/b " + newpages.size()
						+ " @ " + pagesi_);
				for (int i = 0, imax = newpages.size(); i < imax; i++) {
					Object o = newpages.get(i);
					// System.out.println(o+" "+o.getClass().getName());
					if (o instanceof URI)
						pages_.add(new DocRec((URI) o));
					else if (o instanceof String)
						try {
							pages_.add(new DocRec(new URI((String) o)));
						} catch (URISyntaxException ignore) {
						}
				}
				System.out.println("pages_.size() = " + pages_.size());
			}

		} else if (Document.MSG_OPENED == msg) { // only keep if made it through
			DocInfo di = (DocInfo) arg;
			// System.out.println("*** f/b doc root? "+di.doc+" vs
			// "+getBrowser().getRoot().childAt(0));

			rootdoc_ = di.doc;

			// maybe record statistics, such as how times seen page and when was
			// the last time
			if (skipit_) {
				skipit_ = false;
				di.doc.putAttr(Document.ATTR_PAGE, docnow_.page); // override
				// MediaAdaptor
				// and Multipage

			} else if (di.doc == getBrowser().getDocRoot().childAt(0)) { // for
				// now
				// ,
				// only
				// root documents
				URI uri = di.uri;
				if (pagesi_ == -1 || !uri.equals(pages_.get(pagesi_).uri)) { // don
					// 't
					// record
					// reloads (but
					// do if anchor
					// differs)
					// pages_.setSize(pagesi_); // truncate -- ArrayList has
					// removeRange()
					for (int i = pages_.size() - 1; i > pagesi_; i--)
						pages_.remove(i);
					// pages_.removeRange(pagesi_+1, pages_.size()-1); //
					// truncate =>
					// removeRange is a protected method!

					docnow_ = new DocRec(uri);
					pages_.add(docnow_);
					pagesi_++;
				}
			}
			doclast_ = docnow_;
		} else if (Document.MSG_FORMATTED == msg) { // wait until this as
			// scrollbar will clip
			// if not formatted
			if (rootdoc_ == arg && docnow_.yposn <= 0)
				return true;
			// rootdoc_.scrollTo(0, docnow_.yposn);

		} else if (ForwardBack.MSG_FORWARD == msg) {
			moveDelta(1);
			return super.semanticEventAfter(se, "");

		} else if (ForwardBack.MSG_BACKWARD == msg) {
			moveDelta(-1);
			return super.semanticEventAfter(se, "");
		}

		return super.semanticEventAfter(se, msg);

		// return true;
	}
}
