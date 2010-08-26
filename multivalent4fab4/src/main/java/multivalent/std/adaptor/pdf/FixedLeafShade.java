package multivalent.std.adaptor.pdf;

import java.awt.Rectangle;
//import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.GradientPaint;
import java.io.IOException;
import java.util.Map;

import multivalent.*;
import multivalent.node.Fixed;



/**
	INCOMPLETE.  Only Axial type implemented; others only draw background.
	Variety of PDF shadings.

	@version $Revision: 1.5 $ $Date: 2003/08/29 04:00:39 $
*/
public class FixedLeafShade extends Leaf implements Fixed {
  /**
	Returns leaf that performs the type of smooth shading (gradient) described by the passed shading <var>dict</var>ionary
	within the area <var>bbox</var>.
  */
  public static FixedLeafShade getInstance(Dict dict, ColorSpace cs, Rectangle bbox, INode parent, PDFReader pdfr) throws IOException {
//System.out.println("bbox = "+bbox);

	int type = pdfr.getObjInt(dict.get("ShadingType"));
	// ColorSpace, which look up in caller so have csres and patres available, already processed

	FixedLeafShade l;
	switch (type) {
	case 1: l = new FixedLeafShadeFn(parent, dict, cs, bbox, pdfr); break;
	case 2:	l = new FixedLeafShadeAxial(parent, dict, cs, bbox, pdfr); break;
	case 3:	l = new FixedLeafShadeRadial(parent, dict, cs, bbox, pdfr); break;
	case 4:	l = new FixedLeafShadeGFree(parent, dict, cs, bbox, pdfr); break;
	case 5:	l = new FixedLeafShadeGLattice(parent, dict, cs, bbox, pdfr); break;
	case 6:	l = new FixedLeafShadeCoons(parent, dict, cs, bbox, pdfr); break;
	case 7:	l = new FixedLeafShadeTensor(parent, dict, cs, bbox, pdfr); break;
	default: l=null;  assert false: type;
	}

	return l;
  }



  Rectangle ibbox_;
  ColorSpace cs_;
  Color bg_;
  boolean antialias_;
  Object/* Function or Function[]*/ fun_;

  protected FixedLeafShade(String name, Dict dict, INode parent, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super(name, /*(Map)dict*/null, parent);

	// a. required
	ibbox_ = new Rectangle(bbox);
	getBbox().setBounds(bbox);  // => formatNodeContent
	cs_ = cs;

	// b. optional
	//Object[] bbox = pdfr.getObject(dict.get("Bbox")); -- parameter
	Object[] oa = (Object[])pdfr.getObject(dict.get("Background"));
	if (oa!=null) {
		float[] cscomp=new float[oa.length]; PDF.getFloats(oa, cscomp, oa.length);
		//bg_ = new Color(cs, cscomp, 1f);     // Java bug
		float[] rgb = cs.toRGB(cscomp);
		bg_ = new Color(rgb[0], rgb[1], rgb[2], 1f);
	}
//System.out.println("background = "+bg);

//System.out.println("gradient "+bbox+" in "+background);
	Object o = pdfr.getObject(dict.get("AntiAlias"));
	antialias_ = (o instanceof Boolean && ((Boolean)o).booleanValue());

	// c. popular but not universal
	Object dictref = dict.get("Function");
	o = pdfr.getObject(dictref);
	if (o==null) fun_=null;
	else if (COS.CLASS_DICTIONARY==o.getClass()) { /*funs=new Function[1]; funs[0]*/fun_=Function.getInstance(dictref, pdfr); }
	else { assert COS.CLASS_ARRAY==o.getClass(): o;
		oa = (Object[])o;
		Function[] funs=new Function[oa.length];
		for (int i=0,imax=oa.length; i<imax; i++) funs[i]=Function.getInstance(oa[i], pdfr);
		fun_ = funs;
	}
  }

  public Rectangle getIbbox() { return ibbox_; }

  void compute(Object fun, float[] input, float[] output) {
	int n = output.length;

	if (fun instanceof Function) {  // 1-in, n-out
		// assert n==fun.getN();
		((Function)fun).compute(input, output);

	} else { assert fun instanceof Function[];  // array of n 1-in, 1-out
		Function[] funs = (Function[])fun;
		assert n==funs.length: n+" vs "+funs.length;

		float[] out1 = new float[1];
		for (int i=0; i<n; i++) {
			funs[i].compute(input, out1);
			output[i] = out1[0];
		}
	}
  }


  /** Paint (plain) background, on top of which gradient is drawn. */
  public boolean paintNodeContent(Context cx, int start, int end) {
	if (/*start==0 && */ bg_!=null) {
		Graphics2D g = cx.g;
		g.setColor(bg_);
		g.fill(getBbox());
	}

	return false;
  }
}



/** Function-based shading. */
class FixedLeafShadeFn extends FixedLeafShade {
  FixedLeafShadeFn(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh1-fn", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh1");
  }
}


/** Axial shading. */
class FixedLeafShadeAxial extends FixedLeafShade {
  GradientPaint gp_;

  FixedLeafShadeAxial(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh2-axial", dict, parent,  cs, bbox, pdfr);

	// required
	Object o = pdfr.getObject(dict.get("Coords"));
	float[] pts=new float[4]; PDF.getFloats((Object[])o, pts, 4);
	pts[0]-=bbox.x; pts[1]-=bbox.y; pts[2]-=bbox.x; pts[3]-=bbox.y;     // relative
//System.out.println("("+pts[0]+","+pts[1]+") .. ("+pts[2]+","+pts[3]+")");

	// optional
	Object[] oa = (Object[])pdfr.getObject(dict.get("Domain"));  // [0.0 1.0]
	float t0=0f, t1=1f;
	if (oa!=null) { t0=((Number)oa[0]).floatValue(); t1=((Number)oa[1]).floatValue(); }

	oa = (Object[])pdfr.getObject(dict.get("Extend"));  // [false false]
	boolean fextend = (oa!=null && (((Boolean)oa[0]).booleanValue() || ((Boolean)oa[1]).booleanValue()));   // Java has cyclic


	int n = cs.getNumComponents();
	float[] cscomp = new float[n];
	float[] input = new float[1];

	input[0] = t0; compute(fun_, input, cscomp);
	//Color c1 = new Color(cs, cscomp, 1f);  // Java bug
	float[] rgb = cs.toRGB(cscomp); Color c1 = new Color(rgb[0], rgb[1], rgb[2], 1f);

	input[0] = t1; compute(fun_, input, cscomp);
	//Color c2 = new Color(cs, cscomp, 1f);  // Java bug
	rgb = cs.toRGB(cscomp); Color c2 = new Color(rgb[0], rgb[1], rgb[2], 1f);
//System.out.println(c1+" .. "+c2);

//System.out.println(fun.compute()+" "+fun.compute());
	gp_ = new GradientPaint(pts[0],pts[1],c1, pts[2],pts[3],c2, fextend);
  }

  public boolean paintNodeContent(Context cx, int start, int end) {
	super.paintNodeContent(cx, start, end);     // background

	if (start==0) {
//System.out.println("paint in "+getBbox());
		Graphics2D g = cx.g;
		g.setPaint(gp_);
		g.fill(getBbox());
	}

	return false;
  }
}

/** Radial shading, type 3. */
class FixedLeafShadeRadial extends FixedLeafShade {
  FixedLeafShadeRadial(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh3-radial", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh3");
  }
}

/** Free-form Gouraud-shaded triangle mesh, type 4. */
class FixedLeafShadeGFree extends FixedLeafShade {
  FixedLeafShadeGFree(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh4-Gourand-free", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh4");
  }
}

/** Lattice-form Gouraud-shaded triangle mesh, type 5. */
class FixedLeafShadeGLattice extends FixedLeafShade {
  FixedLeafShadeGLattice(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh5-Gouraud-lattice", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh5");
  }
}

/** Coons patch mesh, type 6. */
class FixedLeafShadeCoons extends FixedLeafShade {
  FixedLeafShadeCoons(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh6-Coons", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh6");
  }
}

/** Tensor-product patch mesh, type 7. */
class FixedLeafShadeTensor extends FixedLeafShade {
  FixedLeafShadeTensor(INode parent,  Dict dict, ColorSpace cs, Rectangle bbox,  PDFReader pdfr) throws IOException {
	super("sh7-tensor", dict, parent,  cs, bbox, pdfr);
	multivalent.Meta.sampledata("sh7");
  }
}
