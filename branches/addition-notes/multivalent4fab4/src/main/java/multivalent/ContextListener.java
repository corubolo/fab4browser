package multivalent;



/**
	Implemented by everything that can affect appearance through the graphics context,
	especially structural behaviors, lenses, spans.

	Defines {@link #PRIORITY_MIN base constants} used to set self-reported priority in {@link #getPriority()}.
	@see multivalent.StyleSheet for stylesheet-specific priorities.

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:02:06 $
 */
public interface ContextListener { // rename "AppearanceListener"?  maybe more general later
	/** Delta constant used to set self-reported priority. */
	int LITTLE = 10, SOME = ContextListener.LITTLE*15, LOT = ContextListener.SOME*15;

	int	// any use for negative priorities?
	PRIORITY_MIN = ContextListener.LOT+ContextListener.SOME+ContextListener.LITTLE,
	PRIORITY_STRUCT = 10000,
	//PRIORITY_OBJECTREF = 7500,
	PRIORITY_SPAN =  ContextListener.PRIORITY_STRUCT*10,   // move theses to Span, Lens, SelectionSpan, ... classes?
	PRIORITY_LENS = ContextListener.PRIORITY_SPAN*10,
	//PRIORITY_ID = 7500,
	PRIORITY_MAX = Integer.MAX_VALUE,
	PRIORITY_SELECTION = ContextListener.PRIORITY_MAX-ContextListener.LOT*10
	;

	/*  static {  // => no code in interface
	assert LOT*10 < STRUCT;
	assert PRIORITY_LENS*10 < PRIORITY_MAX;
  }*/

	/**
	Resets the graphics context every time the object is added to or dropped from
	the set active over the portion of the document being drawn.
	These behaviors can come from the style sheet, be ad hoc spans,
	be lenses, or come from elsewhere.
	Should be fast.
	@param all all attributes or exclude those that are not inherited
	 */
	boolean appearance(Context cx, boolean all);
	//boolean paintBefore(Context cx, Node n);
	//boolean paintAfter(Context cx, Node n);

	//boolean event(Browser br, AWTEvent e);

	/**
	Self-reported priority relative to others of the same class, e.g., other spans.
	Use the constants defined here, such as {@link #PRIORITY_LENS}, modified by {@link #LITTLE}/{@link #SOME}/{#link LOT}.
	 */
	int getPriority();
}
