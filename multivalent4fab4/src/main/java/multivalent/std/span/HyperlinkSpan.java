package multivalent.std.span;

import java.util.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;


import java.net.URI;
import java.net.URISyntaxException;

import multivalent.*;
import multivalent.std.ui.DocumentPopup;

import phelps.net.URIs;




/**
	This is the familiar point-to-point link.
	Note that it's not built in -- you can add new link types easily.

	<p>Elaborately commented to serve as a simple example of translating Multivalent protocols into Java methods;
	also see {@link multivalent.std.ClipProvenance ClipProvenance}.

	@see ActionSpan
	@see ScriptSpan

	@version $Revision: 1.9 $ $Date: 2003/03/05 08:42:07 $
*/
public class HyperlinkSpan extends /*Active?*/Span {
  public static final String ATTR_URI = "uri";

  /** Link {@link #getState() state}. */
  public static final byte STATE_LINK=0, STATE_VISITED=1, STATE_HOVER=2, STATE_ACTIVE=3;

  /**
	Copy link URI to clipboard.
	<p><tt>"copyLink"</tt>.
  */
  public static final String MSG_COPY_LINK = "copyLink";


  /** Target of link can be given as a String or URL. */
  protected Object target_ = null;	//"(no target)";

  private String msg_ = null;

  /** Flag that records a cache lookup to determine if we've seen this link before, and if so, we can show it in a different color. */
  private boolean seen_ = false;

  private static Cursor curin = null;


  // Some behaviors have commented out code, as a reminder to me to implement
  // something approximately like that later or as a warning that although it
  // seems logical and obvious to do that, it has hidden perils, so don't!
  // Practically all programmer editors color code source text, so it's easy
  // to ignore these blocks of comments.



  /**
	Current state of the link--normal, seen, cursor hovering above, mouse clicked on--
	show we can show visually.
  */
  private byte state_ = STATE_LINK;

  /**
	Record the cursor location when the mouse button is pressed down,
	so that we can determine if the user moved away to cancel activation, or
	moved back to reactivate.
  */
  private static int x0_, y0_;	// static ok since only one active hyperlink at a time


  /** Run-of-the-mill field setter. */
  public void setSeen(boolean seen) { //-- compute this during paint and cache? faster startup and time to spare during paint
	seen_ = seen;
	setState(STATE_LINK);
  }

  /** Run-of-the-mill field setter.  Checks <tt>seen_</tt> flag to see if link seen before. */
  protected void setState(byte state) {
	assert state>=STATE_LINK && state<=STATE_ACTIVE;
	//if (STATE_LINK==state) state_ = (seen_? STATE_VISITED: STATE_LINK); else state_=state;
	state_ = STATE_LINK==state && seen_? STATE_VISITED: state;
  }

  /** Run-of-the-mill field getter. */
  public byte getState() { return state_; }


  /** Run-of-the-mill field getter. */
  public Object getTarget() { return target_; }
  /** Run-of-the-mill field getter. Same as getTarget, but more convenient if target type known to be URL. */
  public URI getURI() { return (URI)target_; }

  /** Sets target that isn't String or URI or URL. */
  public/*was protected*/ void setTarget(Object o) {
	target_=o;
	//putAttr(ATTR_URI, o.toString());   // make available to forms and things
  }

  /** Message to show when mouse hovers over link. */
  public void setMessage(String msg) {
	msg_ = msg;
  }

  /**
	Computes full, canonical URL from a relative specification.
	The canonical URL is used in the table of links already seen.
  */
  public void setURI(String txt) {
	target_ = null;
	if (txt==null) return;
	try {
		//URL relto = getBrowser().getCurDocument().getURL(); => curDoc not necessary our doc
		URI relto = getDocument().getURI();
		URI uri = relto!=null? relto.resolve(txt): new URI(txt);
		setTarget(uri);
		putAttr(ATTR_URI, uri.toString());
		//target_ = url;
	} catch (IllegalArgumentException e) {
	} catch (URISyntaxException e) {
		// maybe want to cancel link, or show in different color to show it's broken
	}
  }



  /**
	Spans are ContextListener's, which are behaviors that compose together to
	determine the Context display properties at every point in the document.
	For instance, a document will usually determine the font family, size, style,
	and foreground and background colors, among many other properties, of a
	piece of text with a combination of ContextListeners reporesenting the
	influence of style sheet settings, built-in span settings, and perhaps lenses.
	Here, the generic hyperlink hardcodes the action of coloring
	the text and gives it an underline,	choosing either blue or red.
	The HTML media adaptor overrides this method to have no action, as the
	hyperlink appearance is dictated entirely by style sheets, either one linked
	to the particular web page or failing that the default HTML style sheet.
  */
  public boolean appearance(Context cx, boolean all) {
	//boolean seen = getControl().seen(
	// if applicable stylesheet setting, go with that, else default to blue underline / red when active
	//StyleSheet ss = getDocument().getStyleSheet();
	//ContextListener cl = ss.get(getName());
	//if (cl==null /* || cl doesn't have appearance*/) -- works but no feedback if not "a"
	cx.foreground = cx.underline = state_==STATE_ACTIVE || target_==null? Color.RED: Color.BLUE;
	return false;
  }

  /** Prefer for style sheet to set appearance. */
  public int getPriority() { return ContextListener.PRIORITY_STRUCT + ContextListener.SOME; }   // not PRIORITY_SPAN but still more that structural



  /**
	Restore almost always invokes its superclass, which when it chains up to <tt>Behavior</tt>
	sets the behavior's attributes and adds it to the passed layer.  Many behaviors
	also set default parameters and set fields from attributes in the attribute
	hash table that all behaviors have.  See <tt>Behavior</tt>'s superclass,
	<tt>VObject</tt>, to examine the various attribute accessor methods.
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	target_ = getAttr(ATTR_URI, "(no target)");    // if no attribute "uri" exists, default to "(no target)"
  }

  public ESISNode save() {
	putAttr(ATTR_URI, target_.toString());     // null ok: removes
	return super.save();
  }


  /**
	On a mouse button down, directly receive all furture low-level events by
	setting a grab in <tt>Browser</tt>, until a mouse button up.  Also, take
	the synthesized events (that is, generated by a multivalent.* class as opposed to
	a java.* class) corresponding to entering and exiting the span, at which time
	change the cursor and perhaps the colors to indicate that the link is active.
	As a Span, the hyperlink receives low-level events in the region of the
	document it spans without additional registering.
	This is event<em>After</em> rather than event<em>Before</em> because the rule
	of thumb is to build in before so it's available to other behaviors,
	and to take action in after if some other behavior hasn't short-circuited you.
  */
  public boolean eventAfter(AWTEvent e, Point rel, Node n) {
	//if (super.event(e,rel)) return true; -- it is dangerous to ignore the superclass, though sometimes necessary; here we're just careless

	// collect up values needed in several places below
	// even though we're not sure these values will be used, it is not expensive in performance to collect them
	Browser br = getBrowser();
	int eid=e.getID();
	MouseEvent me = (MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST? (MouseEvent)e: null); // else return false; -- quick exit, but if later on want to see another event type, this can be confusing

	// When cursor enters hyperlink region, show by setting cursor to hand,
	// showing the link destination in the status bar, set state to "hover",
	// and repaint in case style sheet wants to change color.
	if (eid==MouseEvent.MOUSE_ENTERED) {
		curin = br.getCursor();
		br.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		// always show relative link? => make a Preference
		// If the link type is URL, use a method in the package <tt>util</tt> to
		// make the link easier to read by showing it relative to the current page
		// instead of a showing the full link, only the last part of which is usually informative.
		//String showtxt = (target_ instanceof URL? URIs.relativeURL(br.getCurDocument().getURI(), (URL)gtarget_): target_.toString());
		String showtxt;
		if (msg_!=null) showtxt = msg_;
		else showtxt = "Go to " + (target_ instanceof URI? getDocument().getURI().relativize((URI)target_).toString(): target_.toString());
//System.out.println("URI? "+(target_ instanceof URI)+" "+target_+"  rel to   "+getDocument().getURI());
		setState(STATE_HOVER);
		br.eventq(Browser.MSG_STATUS, showtxt);
		repaint(100);

	// When cursor enters hyperlink region, undo the entry actions.
	} else if (eid==MouseEvent.MOUSE_EXITED) {
		setState(STATE_LINK);
		if (br==null && e.getSource() instanceof Browser) br = (Browser)e.getSource();  // after destroy() don't get have a handle on Browser
		if (br!=null) {
			br.setCursor(curin);
			br.eventq(Browser.MSG_STATUS, "");
		} //else System.out.println("reset cursor ... but br==null");
		repaint(100);


	// When press button down, set state, redraw link, and grab events until button up.
	} else if (eid==MouseEvent.MOUSE_PRESSED) {
		// if not button 1, then exit
		if ((me.getModifiers()&InputEvent.BUTTON1_MASK)==0) return false;

		// when the user starts dragging, control is relinquished by the hyperlink,
		// and a mouse pressed event is generated to signal the default event handlers
		// to start a selection.  This event will first be seen by this hyperlink,
		// and we'll known that this is the case because the state will still be STATE_ACTIVE,
		// in which case we clean up and don't short-circuit.
		if (state_==STATE_ACTIVE) { setState(STATE_LINK); repaint(100); return false; }	// drag throws a click

		// set state to currently active, and redraw to usually show link in different color
		setState(STATE_ACTIVE);
		repaint();

		// record cursor location at start of activation
		x0_=me.getX(); y0_=me.getY();

		// set grab to collect all low-level events regardless of cursor position
		// until grab is released
		br.setGrab(this);

		// return true for shortcircuit -- we're handling the mouse click, so short-circuilt
		// to prevent default event handlers (which would otherwise start a selection) from getting event
		return true;
	}

	return super.eventAfter(e, rel, n);
  }


  /**
	Once we set the grab, subsequent events go directly to here, not to eventBefore/eventAfter.
  */
  public void event(AWTEvent e) {
	assert e!=null;

	int eid = e.getID();
	Browser br = getBrowser();

	if (eid==MouseEvent.MOUSE_DRAGGED) {

		// want to turn off inmediasres_, but get MOUSE_DRAG even if don't move mouse
		// => throw out first n MOUSE_DRAG's
		// exit existing link => wipe out, set selection & seed edit box

		// Tolerate a little movement as with some mouses it's hard to click a button
		// without also moving the mouse and therefore the cursor location.
		// But if the movement passes a threshold, then assume the user is dragging
		// out a selection; in that case, relinquish control by the hyperlink
		// (<tt>releaseGrab</tt>) and construct a mouse pressed event for the default
		// event handlers.

		MouseEvent me = (MouseEvent)e;
		int sx=me.getX(), sy=me.getY();     // same value as br.getCurScrn();
		if (Math.abs(sx-x0_)>5 || Math.abs(sy-y0_)>5) {
			br.releaseGrab(this);   // other clean up is done in MOUSE_PRESSED with state already STATE_ACTIVE

			br.event(new MouseEvent((Component)me.getSource(), MouseEvent.MOUSE_PRESSED, me.getWhen()+1, InputEvent.BUTTON1_MASK, x0_,y0_, me.getClickCount(), me.isPopupTrigger()));
		}

	// On mouse up, clean up mouse down and invoke go().
	} else if (eid==MouseEvent.MOUSE_RELEASED) {
		setSeen(true);
		br.releaseGrab(this);
		//br.setCursor(curin);    // too soon
		//repaint(0); // clear link appearance -- always on repaint queue?	if so, don't repaint portion until next time, at which time it's too late! -- besides, want to see active to mark place
		go();


	// Behaviors that deal with a number of low-level events often end their event() methods this way:
	// with an <tt>else return false</tt> to pass the event on if the behavior isn't interested,
	// and a final <tt>return true</tt> to short-circuit when it wants to take exclusive action.

	}
  }

  /**
	Override this for special action when hyperlink is clicked.
	Defaults to sending {@link Document#MSG_OPEN} semantic event to <tt>target_</tt>.
	If target is a SemanticEvent, then it is fired.
  */
  public void go() {
	//	  seen_=true; -- subclasses don't do a super.go()
	Browser br = getBrowser();
//System.out.println("HyperlinkSpan "+target_);
	if (target_==null) {}
	else if (target_ instanceof SemanticEvent) br.eventq((SemanticEvent)target_);
	else br.eventq(Document.MSG_OPEN, target_);
  }


  /**
	Add to the DOCPOPUP menu--the menu that pops up when the alternative mouse button
	is clicked over some part of the document (as opposed to the menubar) and
	the click is not short-circuited out by some behavior.  Similarly to ClipProvenance,
	add	{@link Span#MSG_EDIT} if the span is in an editable layer (e.g., if link comes from
	the HTML sent by a random server, it isn't editable, whereas link annotations you
	added are), "copyLink", "open in new window", and "open in shared window".
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;

	if (this!=se.getIn()) {}
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		if (isEditable()) {
			createUI("button", "Edit Link URL", new SemanticEvent(br, Span.MSG_EDIT, this, this, null), menu, "EDIT", false);
		}
		createUI("button", "Copy Link to Clipboard", new SemanticEvent(br, MSG_COPY_LINK, this, this, null), menu, "SAVE", false);

		if (target_ instanceof URI) {
			DocInfo di = new DocInfo((URI)target_); di.window = "_NEW";
			createUI("button", "Open in New Window", new SemanticEvent(br, Document.MSG_OPEN, di, null, null), menu, "SPECIFIC"/*"NAVIGATE"*/, false);
			di = new DocInfo((URI)target_); di.window = "Aux";
			createUI("button", "Open in Shared Window", new SemanticEvent(br, Document.MSG_OPEN, di, null, null), menu, "SPECIFIC"/*"NAVIGATE"*/, false);
		}
	}
	return false;
  }

  /**
	Catch "copyLink" sent in <code>semanticEventBefore</code>.
	The pair of {@link Document#MSG_OPEN} are handled by another behavior.
	Many subclasses have various parameters or attributes, such as URL here
	or annotation text elsewhere, and the Span class supports editing by
	catching {@link Span#MSG_EDIT} and throwing up an associated HTML document with
	a FORM in a note window.  When that window is closed, it sends {@link SystemEvents#MSG_FORM_DATA}.
	semantic event with the name-value pairs of the form as a parameter.
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();

	if (this!=se.getIn()) {}
//if ("hyperlinkData"==msg) System.out.println("*** processing form in hyperlink 1, in="+in);
	else if (SystemEvents.MSG_FORM_DATA==msg) {	  // takes data from non-window/non-interactive source too
//System.out.println("*** processing form in hyperlink 2");

		// process data
		Map<String,String> map = (Map<String,String>)arg;
/*		for (Iterator<Map.Entry> i=map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<> e = i.next();
			System.out.println(e.getKey()+" = "+e.getValue());
		}*/
		if (map!=null && map.get("cancel")==null) {
			String uri = map.get(ATTR_URI);
			if (uri!=null) {
				setTarget(URIs.decode(uri));
			} else {
				// alert("must set URI")
				return true;	// keep dialog posted
			}

			// if link doesn't already exists, make it
			Browser br = getBrowser();
			if (!isSet()) {
				Span sel = br.getSelectionSpan();
				if (sel.isSet()) move(sel);
				//else	alert("must set selection")
			} // else maintain current location
		} // else cancel: don't make / don't edit

		return true;

	} else if (MSG_COPY_LINK==msg) {
//System.out.println("copying "+getTarget().toString()+" to clipboard	"+arg);
		StringSelection ss = new StringSelection(getTarget().toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
		//getBrowser().setSelection(txt);
	}

	return super.semanticEventAfter(se,msg);
  }


  public String toString() { return "Hyperlink:"+target_; }
}
