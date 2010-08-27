package multivalent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import multivalent.IDInfo.Confidence;
import multivalent.node.LeafUnicode;

import com.pt.io.InputUni;
import com.pt.io.InputUniString;



/**
	Superclass for <dfn>media adaptors</dfn>: behaviors that parse some concrete document format and build a document tree.
	As much as possible, errors in the input document should be corrected and old constructs modernized
	so that all the many behaviors that operate on the document tree benefit.
	Media adaptors should include in the tree complete data,
	so that a document in the original format can be reconstructed without loss of information.
	This will not necessarily produce an identical file, after error corrrection during read and pretty printing on write.

	<ul>
	<li>hints when parsing concrete document: {@link #HINT_METADATA_ONLY}, {@link #getHints()}, {@link #setHints(int)}
	<li>security: {@link #isAuthorized()}, {@link #setPassword(String)}
	<li>{@link #parse(INode)}
	<!-- <li>identify, metadata -->
	<li>{@link #close()}
	</ul>


	<p>New media adaptors can be linked in with the core system by associating
	a MIME Content-Type header and/or file suffix with the class name in
	{@link Multivalent#FILENAME_PREFERENCES Preferences}.

	<p>Media adaptors by default display a document accurately,
	with perfect fidelity sacrificed only if very expensive to do so.
	Applications not requiring this can speed execution by declaring <dfn>hints</dfn>.
	For example, {@link #HINT_NO_IMAGE} which suggests to a media adaptor that images won't be needed
	and so it can operate faster by not creating them.  Media adaptors are free to ignore hints.


	<h2 id='external'>External Use</h2>

	To extract text,
	determine layout coordinates,
	convert to another document format,
	convert the document to an image by painting onto the image's Graphics2D,
	and other uses.

	<p>Parsing
	<ol>
	<li>Create instance: <code>MediaAdaptor ma = (MediaAdaptor){@link Behavior#getInstance(String, String, Map, Layer)}</code>
	<li>{@link #setInput(InputUni)}.
	<li>optionally {@link #setHints(int)}
	<li>{@link #parse(INode)} to obtain a document tree,
		which can be inspected (e.g., to extract text), formatted (e.g., to obtain the layout geometry of HTML),
		painted (e.g., to convert a PDF page into an image and save that disk), ...
	<li>optionally, obtain metadata
	<li>{@link #close()}
	</ol>


	<p>Formatting
	...


	<p>Painting on screen or image ({@link java.awt.Graphics2D})
	...


	<p>Examples: {@link tool.doc.ExtractText}
	Often a <code>String toHTML()</code> method defined.


	@see ParseException

	@version $Revision: 1.11 $ $Date: 2005/05/01 03:36:53 $
 */
public abstract class MediaAdaptor extends Behavior {
	// content
	// Hints are mutually exclusive so media adaptors only have to check one.  There are some convenience definintions, which are ORs of others.
	/** No hints: instantiate full, high quality document content. */
	public static final int HINT_NONE = 0;
	/** Read enough to extract metadata, but save time by ignoring content. */
	public static final int HINT_METADATA_ONLY = 1<<0;
	/** Results need not include text. */
	public static final int HINT_NO_TEXT = 1<<2;
	/** Results need not include images, so there may be no need to create them. */
	public static final int HINT_NO_IMAGE = 1<<3;
	/** Results need not include drawn shapes (e.g., rectangles, ellipses, splines). */
	public static final int HINT_NO_SHAPE = 1<<4;
	/** Results need not record or apply syling (e.g., fonts, colors, line widths). */
	public static final int HINT_NO_STYLE = 1<<5;
	/**
	Do not incorporate transclusions, such as HTML IFRAME and man page <code>.so</code>.
	Useful for full-text indexing that scans each file.
	 */
	public static final int HINT_NO_TRANSCLUSION = 1<<9;

	// content manipulation
	/** Document tree need not be formatted.  This implies {@link #HINT_NO_SHOW}. */
	public static final int HINT_NO_LAYOUT = 1<<10;
	/**
	Document tree will not be shown (on screen or painted), but may be queried.
	This allows formatting with stubs: empty box for images and cheap no-display fonts with same metrics.
	 */
	public static final int HINT_NO_SHOW = 1<<11;
	/**
	No interaction by user: clicking, typing.
	{@link #HINT_NO_SHOW} implies this hint.
	 */
	public static final int HINT_NO_INTERACTIVE = 1<<12;

	// metadata
	/** Normalize metadata to {@link phelps.doc.DublinCore Dublin Core} where applicable. */
	public static final int HINT_NORMALIZE = 1<<15;

	// time
	/**
	Require exact display no matter the computation cost.
	Content is put into the document tree, even if it is not visible.
	Use this flag if the tree is the basis for translation to another format.
	 */
	public static final int HINT_EXACT = 1<<20;
	/** Favor fast display at possible expense of some accuracy. */
	public static final int HINT_FAST = 1<<21;
	/** Show metadata (HTML META, PDF /Info, image WxH & bit depth & format, ...). */
	//public static final int HINT_DISPLAY_METADATA = 1<<12;
	/** Plain text only -- no bold, colors, hyperlinks, ... */    // saves hyperlink seen check, but Span creation per se probably cheap
	//public static final int HINT_TEXT_PLAIN = 1<<13;
	/** Document will be printed rather than viewed on screen.
  public static final int HINT_PRINT = 1<<24;*/

	/** By default all hints are off: display complete document with perfect fidelity. */
	public static final int HINT_DEFAULTS = MediaAdaptor.HINT_NONE;


	private InputUni iu_ = null;

	private float zoom_ = 1f;

	/** By default, all flags turned on. */
	private int hints_ = MediaAdaptor.HINT_DEFAULTS;

	private boolean stop_ = false;  // don't access directly because changes in one thread much be synchronized to show up in another
	private volatile boolean loading_ = true;


	public void setInput(InputUni iu) throws IOException { iu_=iu; }  // almost abstract, but Generate and RawImage don't have InputStream's => constructor via reflection

	public void setInput(File f) throws IOException { setInput(InputUni.getInstance(f, null)); }


	protected InputUni getInputUni() { return iu_; }

	/**
	Returns the <em>logical</em> URI of the document
	(the data may come from a cache or elsewhere).
	 */
	public URI getURI() { return iu_!=null? iu_.getURI(): null; }


	/**
	Sets the zoom factor for the associated document, where <code>1.0</code> is the natural size and <code>1.25</code> is 25% larger.
	The media adaptor is free to uniformly scale all objects or just fonts or another interpretation.
	 */
	public void setZoom(float zoom) {
		if (0f < zoom /*< 100f*/) zoom_ = zoom;
	}

	public float getZoom() { return zoom_; }

	public int getHints() { return hints_; }

	/** Set document tree construction hints for media adaptor to bit-wise OR of {@link #HINT_NO_TEXT hint flags}. */
	public void setHints(int hints) {
		// check validity of flags?
		hints_ = hints;
	}


	/**
	Parses a document's data format and constructs a document tree.
	Structure is represented in internal nodes and content (text, images, video, ...) at the leaves.

	<p>Before using, invoke {@link #setInput(InputUni)}.
	The newly constructed document tree should attach to <var>parent</var>.
	The <var>parent</var> is usually but not necessarily a {@link multivalent.Document}.
	Paginated documents should build the current page only, as indicated by the attribute {@link multivalent.Document#ATTR_PAGE}, and report their page count to {@link multivalent.Document#ATTR_PAGECOUNT}.
	Metadata, such as author and dates, should be stored in the closed containing {@link multivalent.Document}.

	<p>If encountering an unfixable/unrecoverable <em>parsing</em> error, usually due to an invalid data format, throws a {@link multivalent.ParseException}.
	(This does not supercede {@link java.io.IOException}.)
	When media adaptor is done or has thrown an exception, the client must {@link #close()} it.

	<p>Subclasses should not rely on being able to obtain a {@link multivalent.node.Root}, {@link multivalent.Browser}, or {@link multivalent.Multivalent};
	in such cases it is acceptable to reduce functionality.

	@return whatever Object is appropriate to the media adaptor.
	For HTML it is the root of the HTML tree (which has name "html"),
	for documents with no single root it can be <var>parent</var>,
	for an image constuctor it could be an {@link java.awt.Image}.
	However, the primary job of a media adaptor is to add content to the document tree.

	@see Span#open(Node) for a convenient way to attach spans
	 */
	public abstract Object parse(INode parent) throws Exception;  // only IOException and ParseException?


	/**
	It is recommended that media adaptors construct document trees that directly and fully represent the document format.
	However, it can be expedient to write a quick-and-dirty converter into another a document format, such as Perl POD to HTML.
	In that case, the converter can generated the target format and throw it to this method convert that to a document tree.
	 */
	//protected /*static for History, multivalent.net.Robust?*/ Object parseHelper(String txt, String adaptor, INode parent) /*throws IOException--data local*/ {
	public static Object parseHelper(String txt, String adaptor, Layer layer, INode parent) /*throws IOException--data local*/ {
		/*
	return parseHelper(new InputUniString(txt), adaptor, layer, parent);
  }
  public static Object parseHelper(InputUni iu, String adaptor, Layer layer, INode parent) /*throws IOException--data local* / {
		 */
		MediaAdaptor helper = (MediaAdaptor)Behavior.getInstance("helper",adaptor,null, layer);
		Node root = null;
		try {
			helper.setInput(new InputUniString(txt, null));
			root = (Node)helper.parse(parent);
		} catch (Exception e) {
			new LeafUnicode("ERROR "+e,null, parent);
			e.printStackTrace();
		} finally {
			try { helper.close(); } catch (IOException ioe) {}
		}

		return root;
	}


	public boolean isAuthorized() { return true; }

	public void setPassword(String pw) {}


	public synchronized/*for communicating across threads*/ boolean isStopped() { return stop_; }
	public synchronized void stop() {
		if (isStopped() && loading_) {
			stop_ = true;
			getDocument().putAttr(Document.ATTR_STOP, Document.ATTR_STOP);  // signal to other behaviors
			//close() ?
			//thread.stop();  -- NO.  Media adaptor should periodically check isStopped() and return if true
		}
	}


	/**
	Release resources used by current page, but instance is still open and can reconstruct.
	If medium has more subresources, as HTML has images, then this should be overridden to delete them too.
  public void flush() {
	// getControl().getCache().expire(getURI(), null, Cache.GROUP_GENERAL); ?
  }
	 */

	/** Close media adaptor, freeing any resources. */	// also done by Behavior.destroy, but more normal name for use as library
	public void close() throws IOException {
		if (iu_!=null) {
			//getLogger().fineSystem.out.println(iu_.getURI());
			iu_.close(); iu_=null;
		}
	}



	/**
	{@link #parse(INode)} concrete document format and put into tree.
	Subclasses should set their style sheets, then parse document body, so can progressively render page, if applicable.
	 */
	@Override
	public void buildBefore(Document doc) {
		try {
			parse(doc);
		} catch (Exception e) {
			new LeafUnicode("build before "+e, null, doc);
			//System.out.println("build before "+e);
			//close(); / destroy();
			e.printStackTrace();
		}
	}


	/**
	On {@link Document#MSG_STOP}, set stop flag, which subclass has to check for periodically.
	 */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (Document.MSG_STOP==msg && se.getArg()==getDocument()) {
			getLogger().fine("cancelling load: "+getURI());
			stop();

		} else if (Document.MSG_CLOSE==msg)
			try { close(); } catch (IOException ignore) {}

			return super.semanticEventAfter(se, msg);
	}

    /**
     * Returns the ID information for the given byte stream as recognized by
     * this adaptor.
     *
     * @param min      The minimum confidence to be returned.
     * @param max      The maximum confidence to be tested.
     * @param path     The path for the bytes. This can be a file path, a URL,
     *                 or <tt>null</tt>.
     * @param complete <tt>true</tt> if the available bytes represent the entire
     *                 file.
     *
     * @return The info that this adaptor associates with the input bytes.
     *         Returns <tt>null</tt> if this adaptor decides that the bytes are
     *         not a type it recognizes.
     */
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        return validateParams(min, max);
    }

    /**
     * Used in classes implementing {@link #getTypeInfo} to validate
     * parameters.
     *
     * @param minConfidence The {@code minConfidence} passed to {@code
     *                      getTypeInfo}.
     * @param maxConfidence The {@code maxConfidence} passed to {@code
     *                      getTypeInfo}.
     */
    protected SortedSet<IDInfo> validateParams(Confidence minConfidence,
            Confidence maxConfidence) {

        if (minConfidence.compareTo(maxConfidence) > 0)
            throw new IllegalArgumentException(
                    "confidence min > max [" + minConfidence + " > " +
                            maxConfidence + "]");
        return new TreeSet<IDInfo>(IDInfo.BEST_FIRST);
    }

    /**
     * Parse a document using the default method with a dummy document.  This is
     * useful for some implementations of {@link #getTypeInfo}.
     * <p/>
     * The document pased to {@link #parse(INode)} is built by the factory
     * method {@link #newDocument(String,Confidence)}.  Override that to change
     * how that document is built.
     *
     * @param path  Path for the data.
     * @param level The confidence level for which this is being parsed.
     *
     * @return The parsed object.
     *
     * @throws Exception An exception from the {@link #parse} method.
     */
    public Object parseDocument(String path, Confidence level)
            throws Exception {

        Document doc = newDocument(path, level);

        if (path.charAt(0) == '/')
            path = "file://" + path;    // makes "/foo" into "file:///foo
        else
            path = "file:" + path;      // leaves relative paths alone

        doc.uri = new URI(path);
        return parse(doc);
    }

    /**
     * Generate a new document to be used by {@link #parseDocument(String,Confidence)}.
     * Override this to make a more complex/correct document.
     *
     * @param path  The path to the data.
     * @param level The confidence level for which this is being parsed.
     * @param level
     *
     * @return The document to use.
     */
    protected Document newDocument(String path, Confidence level) {
        return new Document(path, null, null);
    }

    /**
     * Find the suffix from the path, and return the string from the map that is
     * stored under that suffix. The suffix is the last part of the path
     * following a dot (not including the dot itself).  If there is no dot, or
     * the path is <tt>null</tt>, that is considered the suffix.
     *
     * @param path      Path for the data.
     * @param suffixMap The map that has the values.
     *
     * @return The string stored in the map under the path's suffix.
     */
    protected static String lookupSuffix(String path,
            Map<String, String> suffixMap) {

        String suffix = path;
        if (path != null) {
            int dot = path.lastIndexOf('.');
            if (dot > 0) {
                suffix = path.substring(dot + 1);
            }
        }
        return suffixMap.get(suffix);
    }

    protected static boolean inRange(Confidence min, Confidence level,
            Confidence max) {

        if (min.compareTo(level) <= 0 && level.compareTo(max) <= 0)
            return true;
        return level == Confidence.MAXIMUM && max == Confidence.MAXIMUM;
    }

	@Override
	public void destroy() {
		try { close(); } catch (Exception toolate) {}
		super.destroy();
	}


	/*
  public String strHints() {
  }
	 */
}
