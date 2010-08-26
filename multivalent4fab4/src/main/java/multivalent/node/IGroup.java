package multivalent.node;

import java.util.Map;

import multivalent.INode;


/**
	Logical grouping, like IParaBox except doesn't cause linebreaks.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:41:39 $
*/
public class IGroup extends IParaBox {
  public IGroup(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  public boolean breakBefore() { return false; }
  public boolean breakAfter() { return false; }
}
