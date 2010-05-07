package multivalent.std.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import phelps.lang.Maths;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.gui.VMenu;
//import multivalent.std.ui.DocumentPopup;



/**
	Move forward and backward through documents browsed, via toolbar buttons, menubar items, document popup.
	Can't have generic scriptable UI on toolbar buttons as want to have button list pages so can skip intermediate.

	<p>To do:
	update this for FRAMEs => store history in Document's parent?

	@version $Revision: 1.7 $ $Date: 2002/01/16 04:48:28 $
*/
public class ForwardBack extends Behavior {
  static final boolean DEBUG = false;

  /**
	Request to move back a page in the browsing history.
	<p><tt>"backwardDocument"</tt>
  */
  public static final String MSG_BACKWARD = "backwardDocument";

  /**
	Request to move forward a page in the browsing history.
	<p><tt>"forwardDocument"</tt>
  */
  public static final String MSG_FORWARD = "forwardDocument";

  /**
	Add list of URIs to next document menu.  Used by slide show.
	<p><tt>"openDocuments"</tt>: <tt>arg=</tt> {@link java.util.List} <var>URIs</var>.
	@see multivalent.std.SlideShow
  */
  public static final String MSG_OPEN_DOCUMENTS = "openDocuments";


  static final String BACKWARDEVENT="event "+MSG_BACKWARD, FORWARDEVENT="event "+MSG_FORWARD;


  List<DocRec> pages_ = new ArrayList<DocRec>(100);
  /** Position in pages_ list, which is < pages_.size() if went backward. */
  int pagesi_ = -1;
  DocRec docnow_=null, doclast_=null;
  Document rootdoc_ = null;

  private boolean skipit_=false;


  private static class DocRec {
	public URI uri;         	// was: Object for URI or String
	public String title=null;
	public String target=null;	 // for frames, named window
	public int yposn=-1;
	public String page=null;	 // page number, or subpart of big document

	DocRec(URI uri) { this.uri=uri; title = uri.toString(); }
	public String toString() { return title+"  /  "+uri+", y="+yposn; }
  }


  class FBMenu extends VMenu {  // a little bit more efficient than using generated VMenu
	protected boolean backward_=true;
	protected int postedat_;

	public FBMenu(String name,Map<String,Object> attrs, INode parent, boolean backward) { super(name,attrs, parent); backward_=backward; }

	public void post(int x, int y, Browser br) {
		// dynamically construct contents -- which we do for all menus, so just br.callSemanticEvent?
		removeAllChildren();

		// known to have some elements in menu or else would be disabled in button function
		for (int inc=(backward_?-1:1), i=pagesi_+inc, imax=pages_.size(); i>=0 && i<imax; i+=inc) {
			DocRec rec = pages_.get(i);
			//VMenuButton mb = new VMenuButton("menuitem",null, this);	// pick up menubutton style
			/*Leaf l =*/ new LeafUnicode(rec.title,null, /*mb*/this);
			//l.putAttr("script", "event openDocument "+rec.uri.toString()); => no, this would add to trail
//System.out.println("adding "+rec.uri+" / "+rec.title);
		}
		//markDirtySubtree(false);	// leaves already dirty
		postedat_ = pagesi_;
//System.out.println("post FBMenu @ "+x+","+y);
		super.post(x,y, br);
	}

	public void invoke() {
		super.invoke();
		//if (activeitem_!=null) moveDelta((backward_?-1:1) * (activeitem_.childNum()+1));  // with Auto slide show, pagesi_ changes so don't want delta to whatever it is
		//if (activeitem_!=null) moveTo((backward_?-1:1) * (activeitem_.childNum()+1) + postedat_);
		Node activeitem = getSelected();
		if (activeitem!=null) moveTo((backward_?-1:1) * (activeitem.childNum()+1) + postedat_);
	}
  }


  public void moveDelta(int delta) {
//System.out.println("moveDelta "+delta+", now="+pagesi_+" vs max="+(pages_.size()-1));
	moveTo(Maths.minmax(0, pagesi_+delta, pages_.size()-1));
  }

  public void moveTo(int newpagesi) {
//System.out.println("now="+pagesi_+" vs opening #"+newpagesi);//+" = "+pages_.get(newpagesi));
	if (newpagesi!=pagesi_) {
		// open new
		pagesi_=newpagesi; docnow_=pages_.get(pagesi_);

		Browser br = getBrowser();
		if (docnow_.page!=null) getDocument().putAttr("page", docnow_.page); else getDocument().removeAttr("page");
		skipit_=true;	// ignore own openDocument event
		br.eventq(Document.MSG_OPEN, docnow_.uri);
		//br.eventq(IScrollPane.MSG_SCROLL_TO, xxx);
	}
  }



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	boolean isNext = (pagesi_+1<pages_.size()), isPrev = (pagesi_>0);

	// record all documents opened
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_CLOSE==msg) {
		if (doclast_!=null) {
			// record y scroll position
			Document doc = (Document)se.getArg();
if (DEBUG) System.out.println("doc = "+doc.getFirstLeaf());
			if (doc!=null) {
				//docnow_.yposn = (ir!=null? ir.getVsb().getValue(): -1);
				doclast_.yposn = doc.getVsb().getValue();
				doclast_.title = doc.getAttr(Document.ATTR_TITLE);
				doclast_.page = doc.getAttr(Document.ATTR_PAGE);
			}
//if (DEBUG) System.out.println("*** saving PAGE="+doclast_.uri+", yposn="+doclast_.yposn+", title="+docnow_.title);
		}
//System.out.println("**** f/b close: "+doclast_);

	} else if (VMenu.MSG_CREATE_GO==msg) {
		INode menu = (INode)se.getOut();
		createUI("button", "Back", BACKWARDEVENT, menu, "Go", !isPrev);
		createUI("button", "Forward", FORWARDEVENT, menu, "Go", !isNext);

/*		Browser br = getBrowser();
		for (int i=pages_.size()-1; i>=0; i--) {
			DocRec rec = pages_.get(i);
			//String title = (pagesi_ == i? "<b>"+rec.title+"</b>": rec.title);
			createUI("button", rec.title, new SemanticEvent(br, Document.MSG_OPEN, rec.uri), menu, "History", pagesi_==i);
		}*/

	} else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==null) {//!=getBrowser().getSelectionSpan())) {	 //null)) { -- just plain-doc popup
//System.out.println("**** f/b createMenu "+super.toString()+", in="+se.getIn());
		INode menu = (INode)se.getOut();
		createUI("button", "Back", BACKWARDEVENT, menu, "NAVIGATE", !isPrev);
		createUI("button", "Forward", FORWARDEVENT, menu, "NAVIGATE", !isNext);

	} else if (Browser.MSG_CREATE_TOOLBAR==msg) {
		// rebuild every time change pages, so this is your one-stop shop
		INode p = (INode)se.getOut();
		// could make these once, just attach to toolbar here
		INode bb = (INode)createUI("menubutton", "<img src='systemresource:/sys/images/Back16.gif'>", BACKWARDEVENT, p, null, !isPrev);
		//INode bb = (INode)createUI("menubutton", "<=", BackwardEvent, p, null, !isPrev);
		new FBMenu("menu",null, bb, true);
		INode fb = (INode)createUI("menubutton", "<img src='systemresource:/sys/images/Forward16.gif'>", FORWARDEVENT, p, null, !isNext);
		new FBMenu("menu",null, fb, false);
	}
	return false;
  }


  /*
	"redirecteDocument" cancels latest in list of previous pages<br />
	{@link Document.MSG_OPENED} truncates list and adds that document<br />
	"forwardDocument"/"backwardDocument"<br />
	"openDocuments" adds the list in getArg to list
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();

	if (Document.MSG_REDIRECTED==msg) {
		if (pagesi_>=0) { pages_.remove(pagesi_); pagesi_--; }

	} else if (MSG_OPEN_DOCUMENTS==msg) {
		if (arg instanceof List) {
			//pages_.removeRange(pagesi_+1, pages_.size()-1);     // truncate
			for (int i=pages_.size()-1; i>pagesi_; i--) pages_.remove(i);

			//pages_.addAll((List)arg);
			List<Object> newpages = (List<Object>)arg;
System.out.println("openDocuments in f/b "+newpages.size()+" @ "+pagesi_);
			for (int i=0,imax=newpages.size(); i<imax; i++) {
				Object o = newpages.get(i);
//System.out.println(o+" "+o.getClass().getName());
				if (o instanceof URI) pages_.add(new DocRec((URI)o));
				else if (o instanceof String) {
					try { pages_.add(new DocRec(new URI((String)o))); } catch (URISyntaxException ignore) {}
				}
			}
System.out.println("pages_.size() = "+pages_.size());
		}

	} else if (Document.MSG_OPENED==msg) {	// only keep if made it through
		DocInfo di = (DocInfo)arg;
//System.out.println("*** f/b doc root? "+di.doc+" vs "+getBrowser().getRoot().childAt(0));

		rootdoc_=di.doc;

		// maybe record statistics, such as how times seen page and when was the last time
		if (skipit_) {
			skipit_=false;
			di.doc.putAttr(Document.ATTR_PAGE, docnow_.page);    // override MediaAdaptor and Multipage

		} else if (di.doc==getBrowser().getDocRoot().childAt(0)) {	// for now, only root documents
			URI uri = di.uri;
			if (pagesi_==-1 || !uri.equals(pages_.get(pagesi_).uri)) {	// don't record reloads (but do if anchor differs)
				//pages_.setSize(pagesi_);	  // truncate -- ArrayList has removeRange()
				for (int i=pages_.size()-1; i>pagesi_; i--) pages_.remove(i);
				//pages_.removeRange(pagesi_+1, pages_.size()-1);     // truncate => removeRange is a protected method!

				docnow_ = new DocRec(uri);
				pages_.add(docnow_);
				pagesi_++;
//if (DEBUG) System.out.println("*** f/b added "+uri+", pagesi_="+pagesi_);
			}
		}
		doclast_ = docnow_;
//System.out.println("*** to scroll to "+docnow_.yposn+"  "+di.doc);

	} else if (Document.MSG_FORMATTED==msg) {  // wait until this as scrollbar will clip if not formatted
		// not so great -- should throw IScrollPane.MSG_SCROLL_TO event
//System.out.println("f/b => "+docnow_.yposn+", ymax="+rootdoc_.getVsb().getMax());
		//if (rootdoc_ == arg && docnow_.yposn!=-1) rootdoc_.getVsb().setValue(docnow_.yposn);
		if (rootdoc_ == arg && docnow_.yposn!=-1) rootdoc_.scrollTo(0, docnow_.yposn);

	} else	if (MSG_FORWARD==msg) {
//System.out.println(msg);
		// LATER: stow pagecnt in clientData
		moveDelta(1);

	} else if (MSG_BACKWARD==msg) {
		moveDelta(-1);
	}

	return super.semanticEventAfter(se,msg);
  }
}
