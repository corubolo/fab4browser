package multivalent.std.adaptor.pdf;

import java.awt.Point;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.Map;

import multivalent.Behavior;
//import multivalent.Layer;
import multivalent.Document;
//import multivalent.IScrollPane;
import multivalent.Browser;
import multivalent.SemanticEvent;
import multivalent.Node;
import multivalent.std.ui.Multipage;
import static multivalent.std.adaptor.pdf.COS.*;



/**
	Default implementations of PDF actions:
	GoTo, GoToR, Launch, Thread, URI, Sound, Movie, Hide, Named, SubmitForm, ResetForm, ImportData, JavaScript.
	Implemented: GoTo, GoToR, and URI, Named.

	@version $Revision: 1.6 $ $Date: 2004/03/14 21:46:37 $
*/
public class Action extends Behavior {
  static final boolean DEBUG = !false && multivalent.Meta.DEVEL;  


  /**
	Message of semantic event that announces a PDF action that some handler (another behavior) should execute.
	arg = action dictionary, in = handle to this behavior (for fetching component objects), out = root of PDF document tree.
  */
  public static final String MSG_EXECUTE = "pdfAction";



  //PDFReader pdfr_ = null;

/*
  public void buildAfter(Document doc) {
	// find corresponding media adaptor
	Layer l = getDocument().getLayer("base");
	for (int i=0,imax=l.size(); i<imax; i++) {
		Behavior be = l.getBehavior(i);
//System.out.println(be.getName());
		if (be instanceof PDF) { pdf_ = (PDF)be; break; }
	}
	assert pdf_!=null;
  }
*/

  /**
	Implements the PDF Action, as requested by {@link #MSG_EXECUTE}.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) /*throws IOException*/ {
	Object arg = se.getArg();
//System.out.println("action: "+msg+", "+(arg.getClass()==PDF.DICTIONARY_CLASS)+", "+se.getIn()+", out="+se.getOut());
	if (msg==MSG_EXECUTE && arg!=null && arg.getClass()==COS.CLASS_DICTIONARY && se.getIn() instanceof PDF && se.getOut() instanceof Node) {
		executeAction((Dict)arg, (PDF)se.getIn(), (Node)se.getOut());
		//PDF pdf = (PDF)se.getIn();	// same as getDocument().getMediaAdaptor()
	}
	// if jumping to new page, resolve rest of link to point in page...
	// standard URI anchor syntax from hyperlink annotation could refer to named destination

	return super.semanticEventAfter(se, msg);
  }

  void executeAction(Dict action, PDF pdf, Node out) {
	PDFReader pdfr = pdf.getReader();

	try {	// not the OO way -- could use reflection
		String S = (String)pdfr.getObject(action.get("S"));
if (DEBUG) System.out.println(S+" "+action);

		if ("GoTo".equals(S)) gotol(action, pdf, out);
		else if (S==null && action.get("D")!=null) gotol(action, pdf, out);	// lone destination: implicit GoTo
		else if ("GoToR".equals(S)) gotor(action, pdf, out);
		else if ("Launch".equals(S)) launch(action, pdf, out);

		else if ("Thread".equals(S)) thread(action, pdf, out);

		else if ("URI".equals(S)) uri(action, pdf, out);
		else if ("Sound".equals(S)) sound(action, pdf, out);
		else if ("Movie".equals(S)) movie(action, pdf, out);

		else if ("Hide".equals(S)) hide(action, pdf, out);

		else if ("Named".equals(S)) named(action, pdf, out);

		else if ("SubmitForm".equals(S)) submitform(action, pdf, out);
		else if ("ResetForm".equals(S)) resetform(action, pdf);
		else if ("ImportData".equals(S)) importdata(action, pdf);

		else if ("JavaScript".equals(S)) javascript(action, pdf, out);

		else if ("SetOCGState".equals(S)) setOCGState(action, pdf, out);
		else if ("Rendition".equals(S)) rendition(action, pdf, out);

		// obsolete
		else if ("NOP".equals(S)	// PDF 1.2
				 || "SetState".equals(S)) {
if (DEBUG) System.out.println("obsolete action: "+S);
		}


		else System.err.println("unknown action: "+S+" -- ignored");

	} catch (IOException failed) {
		System.out.println(action.get("S")+" failed: "+failed);
	}

	Object next = action.get("Next");
	if (next!=null) try {
		if (CLASS_DICTIONARY==next.getClass()) executeAction((Dict)next, pdf, out);
		else if (CLASS_ARRAY==next.getClass()) {
			for (Object ref: (Object[])next) {
				Object no = pdfr.getObject(ref);
				if (CLASS_DICTIONARY==no.getClass()) executeAction((Dict)no, pdf, out);
			}
		}
	} catch (IOException fail) {
	}
  }



  /**
	"Go to a destination in the current document".
  */
  /*not protected because extend by intercepting event in different behvaior, not by subclassing and replacing*/ void gotol(Dict dict, PDF pdf, Node out) throws IOException {
	PDFReader pdfr = pdf.getReader();
	Object D = pdfr.getObject(dict.get("D"));
	Object dest = resolveNamedDest(D, pdfr);

//System.out.println("go to page "+dest);
	if (dest!=null && dest.getClass()==COS.CLASS_DICTIONARY) {	// extract from << /D [...] >>
		dest = pdfr.getObject(((Dict)dest).get("D"));
//System.out.println("=> "+dest);
	}

	if (dest!=null && dest.getClass()==COS.CLASS_ARRAY) {	// [page ... /XYZ left top zoom, /Fit, /FitH top, /FitV left, /FitR left bottom right top, /FitB, /FitBH top, /FitBV left
		Object[] pa = (Object[])dest;
		Object page = pdfr.getObject(pa[0]);
//System.out.println("page = "+pa[0]+" / "+page);
		if (/*page!=null &&*/ page.getClass()==COS.CLASS_DICTIONARY) {
			Browser br = getBrowser();
			br.eventq(Multipage.MSG_GOPAGE, String.valueOf(pdfr.getPageNum((Dict)page)));	// throws through Multipage so annotations get saved
			//br.eventq() ... /FitH and so on
			//br.eventq() ... scroll to point on page
		}
	}
  }

  public static Object resolveNamedDest(Object key, PDFReader pdfr) throws IOException {
	Object dest;
	if (key==null || key.getClass()==COS.CLASS_ARRAY || key.getClass()==COS.CLASS_DICTIONARY) {	// literal/direct
		dest = key;

	} else if (key.getClass()==COS.CLASS_NAME) {	// old style => shouldn't see this since PDFReader.updateDests
		Dict dests = (Dict)pdfr.getObject(pdfr.getCatalog().get("Dests"));
		dest = (dests!=null? pdfr.getObject(dests.get(key)): null/*error*/);
//System.out.println("old-style dest: "+key+" => "+dest);	// e.g., Thinking in Postscript
		//br.eventq(msg, new StringBuffer((String)arg));	// send around again rather than converting directly, since some listeners might check for type of arg

	} else { assert key.getClass() == COS.CLASS_STRING;
		Dict names = (Dict)pdfr.getObject(pdfr.getCatalog().get("Names"));
//if (DEBUG) System.out.println("dests = "+names.get("Dests")+" => "+pdfr.getObject(names.get("Dests")));
		Dict dests = (Dict)(names!=null? pdfr.getObject(names.get("Dests")): null);
		dest = pdfr.getObject(pdfr.findNameTree(dests, (StringBuffer)key));
	}

//System.out.println(key+" => "+dest);
	return dest;
  }



  /**
	"(Go-to remote) Go to a destination in another document".
  */
  void gotor(Dict dict, PDF pdf, Node out) throws IOException {
	PDFReader pdfr = pdf.getReader();
	Object D = pdfr.getObject(dict.get("D"));	// /Name, (String), [array w/int=pg no]
	if (D instanceof Object[]) D = "page="+((Object[])D)[0];
	Object F = pdfr.getObject(dict.get("F"));	// file
	Object NewWindow = pdfr.getObject(dict.get("NewWindow"));

	getBrowser().eventq(Document.MSG_OPEN, F/*+"#"+D*/);
	//br.eventq("GoTo", ...);	// regular GoTo within new document
  }


  /**
	"Launch an application, usually to open a file".
  */
  void launch(Dict dict, PDF pdf, Node out) throws IOException {
	// URI.encode() on URI or else security hole
  }


  /**
	"Begin reading an article thread".
  */
  void thread(Dict dict, PDF pdf, Node out) throws IOException {
  }


  /**
	"Resolve a uniform resource identifier".
	<p>(Acrobat has a weblink plug-in that "adds the capability of linking to documents on the World Wide Web".
	We just have to throw a semantic event.)
  */
  void uri(Dict dict, PDF pdf, Node out) throws IOException {
	PDFReader pdfr = pdf.getReader();
	Browser br = getBrowser();
	String suri = pdfr.getObject(dict.get("URI")).toString();	// (String) => java.lang.String

	Boolean ismap = (Boolean)pdfr.getObject(dict.get("IsMap"));
	if (ismap!=null && ismap.booleanValue()) {
		Point pt = br.getCurScrn();
		// LATER: subtract off Rect
		suri += "?"+ pt.x + "," + pt.y;	// don't add to StringBuffer, as that would mutate, though we are leaving this document...
	}

	Dict uridict = (Dict)pdfr.getObject(pdfr.getCatalog().get("URI"));
	StringBuffer base = (StringBuffer)(uridict!=null? pdfr.getObject(uridict.get("Base")): null);
	if (base!=null) try { suri = new URI(base.toString()).resolve(suri).toString(); } catch (URISyntaxException urie) { /*maybe /URI ok as is*/ }

	br.eventq(Document.MSG_OPEN, suri);
  }


  /**
	"Play a sound" (PDF 1.2).
  */
  void sound(Dict dict, PDF pdf, Node out) throws IOException {
  }


  /**
	"Play a movie" (PDF 1.2).
  */
  void movie(Dict dict, PDF pdf, Node out) throws IOException {
  }


  /**
	"Execute an action predefined by the viewer application" (PDF 1.2).
  */
  void named(Dict dict, PDF pdf, Node out) throws IOException {
	String msg = null;

	PDFReader pdfr = pdf.getReader();
	Object N = pdfr.getObject(dict.get("N"));
	if ("NextPage".equals(N)) msg = Multipage.MSG_NEXTPAGE;
	else if ("PrevPage".equals(N)) msg = Multipage.MSG_PREVPAGE;
	else if ("FirstPage".equals(N)) msg = Multipage.MSG_FIRSTPAGE;
	else if ("LastPage".equals(N)) msg = Multipage.MSG_LASTPAGE;
	else if (DEBUG) System.out.println("unknown /S /Named action: "+N);

	if (msg!=null) getBrowser().eventq(msg, null);
  }


  /**
	"Set an annotation’s Hidden flag" (PDF 1.2).
  */
  void hide(Dict dict, PDF pdf, Node out) throws IOException {
  }


  /**
	"Send data to a uniform resource locator" (PDF 1.2).
  */
  void submitform(Dict dict, PDF pdf, Node out) throws IOException {
  }


  /**
	"Set fields to their default values" (PDF 1.2).
  */
  void resetform(Dict dict, PDF pdf) throws IOException {
	Map<String,Object> now = pdf.getForm();
	Map<String,Object> def = Forms.exportDefaults(pdf.getReader());
	// replace now by def according to flags in dict...

	// reload page to update widgets...
  }


  /**
	"Import field values from a file" (PDF 1.2).
  */
  void importdata(Dict dict, PDF pdf) throws IOException {
	Map<String,Object> now = pdf.getForm();
	Map<String,Object> def = Forms.exportDefaults(pdf.getReader());
	// merge def into now according to flags in dict...

	// reload page to update widgets...
  }


  /**
	"Execute a JavaScript script" (PDF 1.3).
  */
  void javascript(Dict dict, PDF pdf, Node out) throws IOException {
	//multivalent.Meta.unsupported("JavaScript");
  }

  /**
	"Set the states of optional content groups" (PDF 1.5).
  */
  void setOCGState(Dict dict, PDF pdf, Node out) throws IOException {
  }

  /**
	"" (PDF 1.5).
  */
  void rendition(Dict dict, PDF pdf, Node out) throws IOException {
  }
}
