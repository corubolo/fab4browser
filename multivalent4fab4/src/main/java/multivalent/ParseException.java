package multivalent; //.adaptor;



/**
	MediaAdaptors should return this when encountering an unfixable/unrecoverable parsing error.

	NB: this is different from {@link java.text.ParseException}.

	@see multivalent.MediaAdaptor

	@version $Revision: 1.2 $ $Date: 2002/10/03 22:15:59 $
 */
public class ParseException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long posn_ = -1;
	int line_=-1, chr_=-1;

	/** Parse error not traceable to specific byte/line. */
	public ParseException(String message) {
		super(message);
	}

	/** Binary data formats report the byte offset into the file. */
	public ParseException(String message, long posn) {
		super(message);
		posn_ = posn;
	}

	/** Text formats report the line and character offsets. */
	public ParseException(String message, int line, int chr) {
		super(message);
		line_=line; chr_=chr;
	}

	public long getPosn() { return posn_; }
	public int getLine() { return line_; }
	public int getChar() { return chr_; }

	@Override
	public String toString() {
		return getMessage() + (posn_!=-1? " @ byte "+posn_: line_!=-1? " @ line="+line_+", character="+chr_: "");
	}
}
