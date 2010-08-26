package multivalent.std.adaptor.pdf;

import java.awt.geom.Rectangle2D;



/**
	Parsed command from content stream.

	@version $Revision: 1.5 $ $Date: 2003/08/29 03:50:04 $
*/
public class Cmd {
  public static final String NO_OP = new String("no_op");	// new String() so never == literal

  public String op;
  public Object[] ops;
  public boolean valid = true;
  /** Iff command generates an opaque rectangle. */
  public Rectangle2D bbox = null;

  public Cmd(String op, Object[] argv, int argc) {
	this.op = op!=null? op: NO_OP;
	ops = new Object[argc];
	System.arraycopy(argv,0, ops,0, argc);
  }

  public boolean equals(Object o) {
	if (o==null || !(o instanceof Cmd)) return false;
	Cmd cmd2 = (Cmd)o;
	return op.equals(cmd2.op) && java.util.Arrays.equals(ops, cmd2.ops);    // Arrays.equals uses Object.equals
  }

  public String toString() { return op+" "+ops.length; }
}
