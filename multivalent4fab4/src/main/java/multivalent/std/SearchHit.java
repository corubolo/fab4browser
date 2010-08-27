package multivalent.std;

import multivalent.*;

/**
	Record for search hits, to be passed around in the semantic event "searchHits".
	Useful for search visualizations.
	x@deprecated - just send around Spans, and now that during "formattedAfter", search viz can compute geometric position as it wants

	@version    $Revision$ $Date$
*/
public class SearchHit extends Behavior {
  public Span span;
  /** Geometric position for search visualizations. */
  public int x,y;

  public SearchHit(Span span, int x, int y) { this.span=span; this.x=x; this.y=y; }
  /* who besides Search.java creates SearchHit's?
  public SearchHit(Span span) {
	Node n = span.getStart().node;
	Document doc = n.getIScrollPane();
	Point pt = n.getRelLocation(doc);
	this(span, pt.x, pt.y);
 }*/
}
