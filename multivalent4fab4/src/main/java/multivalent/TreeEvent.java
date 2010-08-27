package multivalent;

import java.awt.AWTEvent;


/**
	TreeEvent's and all events defined by Java itself, such as those describing
	mouse and keyboard events, are <dfn>low-level events</dfn>.
	Low-level events are passed through the tree from the root to the
	leaves, during which observes on nodes can are called before and after
	the corresponding node is visited.  The type of low-level event is given by a integer.

<!--
	Allow arbitrary events by name,
	e.g., new TreeEvent(this, Document.MSG_OPEN, <URL>)
	Recognized by those in the know, benignly ignored by the rest.

	maybe can make arbitrary ones with String names
-->

	@see multivalent.SemanticEvent

	@version $Revision: 1.2 $ $Date: 2002/02/17 15:37:51 $
 */
public class TreeEvent extends AWTEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int TREE_FIRST = SemanticEvent.SEMANTIC_LAST+1;

	/** Usually filters to leaf, which then sets currentNode in Browser. */
	public static final int FIND_NODE = TreeEvent.TREE_FIRST+1;

	/** Stop all activity in document, as loading images or running applet. */
	public static final int STOP = TreeEvent.FIND_NODE+1;

	/** Validate tree structure. e.g., INode's bbox contains all its children.
  public static final int VALIDATE = STOP+1; */
	public static final int TREE_LAST = TreeEvent.STOP;

	//public static final TreeEvent FIND_NODE_EVENT = new TreeEvent(source, FIND_NODE);


	public TreeEvent(Object source, int id) {
		super(source, id);	// can't just use AWTEvent because it's abstract
		assert id>=TreeEvent.TREE_FIRST && id<=TreeEvent.TREE_LAST;
		// stupid to have id be int rather than String!  with interned strings could have open-ended set, wouldn't have to worry about conflicting numbers, and still high performance as can use "==" with literals or interned String's!
		// on the other hand, with int can switch() and probably easier to use from C
	}

	@Override
	public String toString() {
		int eid = getID();
		return TreeEvent.FIND_NODE==eid? "FIND_NODE":
			TreeEvent.STOP==eid? "STOP":
				"???";
	}
}
