package multivalent.std.adaptor;    // move to multivalent.std ?

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;
import multivalent.SemanticEvent;

import com.pt.io.Cache;
import com.pt.io.InputUni;



/**
	Automatically save and restore annotations from Personal layer.
	Doesn't need to be page aware because Layer is.

	@version $Revision: 1.9 $ $Date: 2002/10/29 20:14:07 $
*/
public class PersonalAnnos extends Behavior {
  static final boolean DEBUG = false;

  static final String PREFIX = "annos/";

  /** Name of annotations hub within cache. */
  static final String FILENAME = "annos.hub";   // => <filename>.anno


  /**
	 {@link Document#MSG_OPENED} looks for and loads corresponding hub.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	//Document doc = varies between di.doc and br.getCurDocument

	if (super.semanticEventBefore(se,msg)) return true;
	else if (Document.MSG_OPENED==msg) {
		DocInfo di = (DocInfo)se.getArg();
		Document doc = di.doc;
		//Layer doclayers = doc.getLayers();
		URI uri = doc.getURI();  // may have been normalized
//System.out.println("pa: "+msg+", "+uri);
/*
		Cache cache = getGlobal().getCache();
		File old = cache.mapTo(uri, "hub.mvd", Cache.GROUP_PERSONAL);	// automatically update old names to new
		File hub = cache.mapTo(uri, FILENAME, Cache.GROUP_PERSONAL);
		if (old.exists()) old.renameTo(hub);

if (DEBUG) System.out.println("* checking old hub "+hub+" => "+hub.exists());
		if (hub.exists() && hub.length()>10) {
//System.out.println("loading annos");
			//try {
			br.eventq(new SemanticEvent(br, Layer.MSG_LOAD, hub.toURI(), null, doc));
			// } catch (Exception canthappen) {}
		} // else personal layer created on demand, URL stuffed in just before saving
		*/
		InputUni iu = getGlobal().getCache().getInputUni(uri, FILENAME, Cache.GROUP_PERSONAL);
		if (iu!=null) {
			br.eventq(new SemanticEvent(br, Layer.MSG_LOAD, iu.getURI(), null, doc));
			try { iu.close(); } catch (IOException ioe) {}
		}

	}
	return false;
  }


  /** On {@link Document#MSG_CLOSE} save behaviors in layer to disk.  In After in order to give everyone a chance to tidy up. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();

	if (Document.MSG_CLOSE==msg) {
		Document doc = (Document)se.getArg();
		if (doc==null) doc=br.getCurDocument(); // when closing everybody, in which case should handle nested documents too LATER

		if (doc.getAttr(Document.ATTR_LOADING)==null) {   // must finished loading, else could lose
			Cache cache = getGlobal().getCache();
			URI uri = doc.getURI();
//System.out.println("pa: "+msg+", "+uri);
//if (DEBUG) System.out.println("* save personal layer to "+hub);

			Layer personal = doc.getLayer(Layer.PERSONAL);
			// copy attrs from doc => NO
			ESISNode e = personal.save();
//System.out.println("save @ close, size= "+e.size()+", uri="+getAttr("uri")+" vs "+personal.getAttr("uri"));
			if (e!=null) {     // layer without any behaviors useful?  for attrs only?
				if (e.getAttr(Document.ATTR_URI)==null) e.putAttr(Document.ATTR_URI, uri.toString());

				try {
					// write file
					OutputStream out = cache.getOutputStream(uri, FILENAME, Cache.GROUP_PERSONAL);
					Writer w = new BufferedWriter(new OutputStreamWriter(out));
					String hubtxt = e.writeXML();
					w.write(hubtxt);
					w.close();
//System.out.println("save\n"+e.writeXML());
//if (DEBUG) System.out.println("**** saved "+e.size()+" annos");
				} catch (IOException ioe) {
					System.err.println("couldn't write hub: "+ioe);
				}
			} else {    // empty
				try {
				cache.delete(uri, FILENAME, Cache.GROUP_PERSONAL);
				} catch (Exception x){
					x.printStackTrace();
				}
//System.out.println("zapping newly empty annos");
			}
		}
	}
	return super.semanticEventAfter(se,msg);
  }
}
