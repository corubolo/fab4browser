package multivalent.std.adaptor.pdf;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;

import multivalent.node.FixedIClip;
import multivalent.Node;
import multivalent.Context;



/**
	PDF graphics state, with many attributes.
	As a PDF content stream is interpreted, GraphicsState objects and pushed and poppsed on the stack.

	@version $Revision: 1.8 $ $Date: 2004/04/27 15:19:04 $
*/
public class GraphicsState {
  public AffineTransform ctm;

  public ColorSpace fCS, sCS;    // not needed before mozilla-ch02.pdf
  public Color fillcolor;
  public Color strokecolor;

  public Dict fontdict;
  public double pointsize;
  public double Tw, Tc, Tz, Ts, TL; public int Tr;
  //public AffineTransform Tm, Tlm;    // not part of state

  public float linewidth;
  public int linecap;	// 0=butt, 1=round, 2=projecting
  public int linejoin;	// 0=miter, 1=round, 2=bevel
  public float miterlimit;
  public float[] dasharray;
  public float dashphase;
  public String renderingintent;
  public int flatness;
  public double smoothness;

  // transparency
  public float alphastroke;
  public float alphanonstroke;


  // implicit state
  //public Encoding encoding;	// => bound to font
  public FixedIClip clip;


  public GraphicsState() {
	ctm = new AffineTransform();
	strokecolor=Color.BLACK; fillcolor=Color.BLACK;  // some PDFs rely on defaults
	sCS = fCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	fontdict = null; pointsize = 1.0;
	Tc=0.0; Tw=0.0; Tz=100.0; TL=0.0; Ts=0.0; Tr=0;
	linewidth = Context.STROKE_DEFAULT.getLineWidth();  // if don't set in content stream, whatever defaults are acceptable
	linecap = Context.STROKE_DEFAULT.getEndCap();
	linejoin = Context.STROKE_DEFAULT.getLineJoin();
	miterlimit = Context.STROKE_DEFAULT.getMiterLimit();  // if don't set in content stream, whatever defaults are acceptable
	dasharray = Context.STROKE_DEFAULT.getDashArray(); 
	dashphase = Context.STROKE_DEFAULT.getDashPhase();
  }

  /** Makes copy of <var>gs</var>. */
  public GraphicsState(GraphicsState gs) {
	ctm = new AffineTransform(gs.ctm);  // clone() + new AffineTransform()

	linewidth = gs.linewidth;
	linecap = gs.linecap;
	linejoin = gs.linejoin;
	miterlimit = gs.miterlimit;
	dasharray = gs.dasharray;
	dashphase = gs.dashphase;

	renderingintent = gs.renderingintent;
	flatness = gs.flatness;
	smoothness = gs.smoothness;

	fCS = gs.fCS; sCS = gs.sCS;
	fillcolor = gs.fillcolor;
	strokecolor = gs.strokecolor;

	fontdict = gs.fontdict;
	pointsize = gs.pointsize;
	Tr = gs.Tr;
	Tc = gs.Tc; Tw = gs.Tw; Tz = gs.Tz; TL = gs.TL; Ts = gs.Ts;

	alphastroke = gs.alphastroke;
	alphanonstroke = gs.alphanonstroke;

	clip = gs.clip;
  }

  public String toString() { return ctm.toString(); }
}
