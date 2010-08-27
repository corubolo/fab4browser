package multivalent;

import java.awt.AWTEvent;



/**
	A semantic event, as opposed to a low-level mouse and keyboard events,
	defines a <i>logical</i> action, such as opening a new document.
	Rather than directly accomplishing a high-level, logical action, even on itself,
	a behavior should send a semantic event and give other behaviors a chance to modify it.
	This also makes these actions available to scripts.

	<p>The type of semantic event, or its <i>message</i>, is given
	by a {@link String} field.	By using {@link String}'s, the set of event types is kept
	open ended.  Message detail is given in fields; fields are given suggestive names,
	but the exact contents depend on the particular semantic event <tt>arg</tt>, <tt>in</tt>, <tt>out</tt>.

	<p>Following the Multivalent architectural principle
	that new behaviors can introduce whatever new structure is needed,
	any behavior can introduce a semantic event by <i>defining</i> it.
	Defining means specifying the use of all the fields,
	usually in the Javadoc comment of a <code>public static final String</code> field that specifies the <tt>message</tt>.
	Of course, behaviors should take pains to use existing names when available.
	The defining behavior usually provides some implementation in its
	{@link Behavior#semanticEventBefore(SemanticEvent, String)} and/or {@link Behavior#semanticEventAfter(SemanticEvent, String)} methods.
	For examples of the standard format for event definitions, see {@link Document#MSG_OPEN}, {@link Browser#MSG_NEW}, and {@link multivalent.std.adaptor.pdf.Action#MSG_EXECUTE}.

	<p>Semantic events are <dfn>thrown</dfn> or <dfn>fired</dfn> by adding them to a browser's event queue.
	Complete semantic events, which should have a handle to the browser in their <i>source</i> field,
	can be fired via {@link Browser#eventq(String, Object)}.
	The convenience method {@link Browser#eventq(AWTEvent)} constructs and fires a semantic
	with the given String message and Object arg.
	Semantic events are <dfn>caught</dfn> in the
	{@link Behavior#semanticEventBefore(SemanticEvent, String)} and {@link Behavior#semanticEventAfter(SemanticEvent, String)} methods.
	Often a behavior will throw and catch the same event.

<!--
	<p>Applications:
	event recorder/VCR playback,
	statistics collection for (re)design of UI

	<p>Compare to OpenDoc semantic events.
-->

	@see multivalent.Document
	@see multivalent.Browser
	@see multivalent.SystemEvents
	@see multivalent.std.ui.Multipage
	@see multivalent.gui.VMenu

	@see multivalent.std.ui.SemanticUI

	@version $Revision: 1.4 $ $Date: 2003/06/02 05:10:16 $
 */
public class SemanticEvent extends AWTEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Interoperate with java.awt.AWTEvent; the id of a semantic event is given by its message, not by an <code>int</code>. */
	public static final int
	SEMANTIC_FIRST = AWTEvent.RESERVED_ID_MAX+1,
	GENERAL = SemanticEvent.SEMANTIC_FIRST,
	SEMANTIC_LAST = SemanticEvent.GENERAL;

	private String message_;	// immutable once set
	private Object arg_;      // mutable
	private Object in_, out_;	// add to or remove, but don't replace object itself


	public SemanticEvent(Object source, String message, Object arg) { this(source,message,arg, null,null); }

	public SemanticEvent(Object source, String message, Object arg, Object in, Object out) {
		super(source, SemanticEvent.GENERAL);     // all semantic events have id==GENERAL: distinguished on String msg!
		message_ = message!=null? message.intern(): null;   // null should throw error
		arg_ = arg;
		in_=in; out_=out;
	}

	/**
	Messages are intern()'ed, so you can compare it with <code>==</code> rather than {@link Object#equals(Object)}.
	 */
	public String getMessage() { return message_; }

	public Object getArg() { return arg_; }
	public void setArg(Object arg) { arg_=arg; }

	public Object getIn() { return in_; }
	public Object getOut() { return out_; }

	/**
	Two <code>SemanticEvents</code> are <code>equal()</code> iff <code>==</code> is true for all their fields
	(<var>source</var>, <var>message<var>, <var>arg<var>, <var>in</var>, and <var>out</var>).
	 */
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof SemanticEvent)) return false;
		SemanticEvent se = (SemanticEvent)o;
		//System.out.println("equals: "+message_+"=?"+se.message_+" && "+arg_+"=?"+se.arg_+" && "+in_+"=?"+se.in_+" && "+out_+"=?"+se.out_);
		return getSource()==se.getSource() && message_==se.message_ && arg_==se.arg_ && in_==se.in_ && out_==se.out_;
	}

	@Override
	public int hashCode() {
		return message_!=null? message_.hashCode(): -1;
	}

	@Override
	public String toString() { return "SE: "+message_ +", arg=" + arg_; }
}
