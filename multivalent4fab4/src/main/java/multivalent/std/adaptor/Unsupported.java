package multivalent.std.adaptor;

import java.util.StringTokenizer;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.node.IParaBox;



/**
	For unsupported document types, say so, and tell user how to see ASCII version.

	Could have genre hub that guarantees <code>View Source</code> and other behaviors.

	@version $Revision$ $Date$
*/
public class Unsupported extends MediaAdaptor {
  static final String MESSAGE =
	"This is an unsupported document type.	"+
	"To see its content as ASCII, choose View/Page Source.	"+
	"To see all documents of this type as ASCII or some other document type, "+
	"update the media adaptor mappings in Preferences.txt.";

  public void buildBefore(Document doc) {
	INode para = new IParaBox("MESSAGE",null, doc);     // always return this, maybe with no children

	StringTokenizer st = new StringTokenizer(MESSAGE);
	while (st.hasMoreTokens()) {
		new LeafUnicode(st.nextToken(), null, para);
	}
  }

  public Object parse(INode parent) throws Exception {
	return null;	// don't even open an InputStream (which MediaAdaptor does by default)
  }
}
