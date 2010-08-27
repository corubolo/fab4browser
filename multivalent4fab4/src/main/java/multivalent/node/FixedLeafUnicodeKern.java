package multivalent.node;

import java.util.Map;
import java.util.Arrays;
import java.awt.geom.AffineTransform;
import java.awt.Rectangle;

import multivalent.INode;
import multivalent.Leaf;
import multivalent.Context;



/**
	Leaf subclass for fixed-formatted Unicode with kerning.

	@version $Revision: 1.4 $ $Date: 2004/02/05 02:45:05 $
*/
public class FixedLeafUnicodeKern extends FixedLeafUnicode implements Fixed {
  //private static final byte[] KERN0 = new byte[0];
  /** Kern <em>before</em> glyph. */
  float[] kern_ = null;	// either null for no kerning, or kern_.length == size()
  //int ksum_; -- always dealing with substrings, so not so useful... and probably not many instances


  /**
	@param kern  <code>null</code> for none / 0 kern.
  */
  public FixedLeafUnicodeKern(String name, Map<String,Object> attr, INode parent, float[] kern) {
	this(name,null, attr, parent, kern);
  }

  public FixedLeafUnicodeKern(String name, String estr,  Map<String,Object> attr, INode parent,  float[] kern) {
	super(name,estr, attr, parent);
	append(null,null, kern);
  }

  /** Constant kerning. */
  public FixedLeafUnicodeKern(String name, Map<String,Object> attr, INode parent, double kern) { this(name,null, attr, parent, kern); }
  public FixedLeafUnicodeKern(String name, String estr,  Map<String,Object> attr, INode parent, double kern) {
	super(name,estr, attr, parent);

	if (kern==0.0) kern_=null; else { kern_ = new float[name.length()]; Arrays.fill(kern_, (float)kern); }
  }


  //public float[] getKern() { return kern_; }
  public double getKernAt(int index) {
	assert index>=0 && index<size(): index+" not in 0.."+size();
	return (kern_!=null? kern_[index]: 0);
  }

	/*
  public void setKern(float[] kern) {
	//kern_ = (kern!=null? kern: KERN0);
	kern_ = kern;
	setValid(false);
  }*/
  public void setKernAt(int index, double kern) {
	assert index>=0 && index<size(): index+" not in 0.."+size();

	float f = (float)kern;
	if (kern_==null) kern_ = new float[size()];
	if (kern_[index]!=f) { kern_[index] = f; setValid(false); }
  }

  public void append(Leaf l) {
	//boolean fvalid = isValid();
	int oldlen = size();
//System.out.print("ibbox: "+getName()+"/"+baseline+"/"+getIbbox()+" + "+l.getName()+"/"+l.baseline+"/"+((FixedLeafUnicodeKern)l).getIbbox());
	super.append(l);
	float dx = 0f;
	if (l instanceof Fixed) {
		Rectangle libbox = ((Fixed)l).getIbbox();
		assert ibbox_.y + baseline == libbox.y + l.baseline;	// shift second to match?
		int dy = ibbox_.y - libbox.y;
		dx = libbox.x - (ibbox_.x + ibbox_.width);
		ibbox_ = ibbox_.union(libbox);
//if (dy>0) System.out.println(getName()+", dy = "+dy);
		if (dy > 0) baseline += dy;	// new bbox may be higher so adjust baseline
	}
	assert l instanceof FixedLeafUnicodeKern: l.getClass().getName();
	if (l instanceof FixedLeafUnicodeKern) {	// usually so
		float[] k2 = ((FixedLeafUnicodeKern)l).kern_;
		append(null, null, k2!=null? k2: new float[l.size()]);
	}
//System.out.println("ibbox: "+kern_[oldlen-1]+" += "+dx);
//System.out.println(" => kern+="+dx+" / "+baseline+" / "+getIbbox());
//System.out.println(getName()+" => kern_.length = "+kern_.length+" vs "+oldlen);
	//assert kern_[oldlen-1]==0f: kern_[oldlen-1]+" / "+getName();
	kern_[oldlen-1] = dx;  // coarse -- rounded to int on both sides.  client can set more precisely

	bbox.setBounds(getIbbox());
	//if (fvalid) setValid(true); => doesn't disturb validity

//System.out.print(" => "+getName());
//for (int i=0,imax=kern_.length; i<imax; i++) System.out.print(" "+kern_[i]);
//System.out.println(", kern+="+dx+" / "+baseline+" / "+getIbbox());
  }

  public void append(String text, String estr) { append(text, estr, null); }
  /** Append text with constant kerning between new characters. */
  public void append(String text, String estr, float kern) {
	float[] k = null;
	if (estr!=null || text!=null) { k = new float[estr!=null? estr.length(): text.length()]; Arrays.fill(k, kern); }
	append(text, estr, k);
  }
  public void append(String text, String estr, float[] kern) {
	super.append(text, estr);
	//X if (kern_!=null || kern!=null) {...	// not special case => join point
	float[] k1 = kern_, k2 = kern;
	int k1len = k1!=null? k1.length: size(), k2len = k2!=null? k2.length: 0;
	float[] k = new float[k1len + k2len];
	if (k1!=null) System.arraycopy(k1,0, k,0, k1len);
	if (k2!=null) System.arraycopy(k2,0, k,k1len, k2len);
	kern_ = k;
  }



  /** Measurements adjusted by kerns. */
  public boolean formatNodeContent(Context cx, int start, int end) {
	boolean ret = super.formatNodeContent(cx, start, end);

	// adjust by kern
	if (kern_!=null && kern_.length==size()) {
		int w = 0; for (int i=start, imax=Math.min(end,size()-1); i<=imax; i++) w += kern_[i];
		bbox.width += w;
	}
//System.out.println("format "+getName()+" "+getIbbox().width+" => "+bbox.width);

	if (start==0) bbox.setLocation(ibbox_.x, ibbox_.y);
	return ret;
  }

  /** Chunk by kerns as well as spans. */
  public boolean paintNodeContent(Context cx, int start, int end) {
	int laststart = start;
	//AffineTransform at = cx.spot!=null && cx.spot.isTransformed()? cx.spot.getTransform(): null;
	if (kern_!=null /*&& kern_.length==size()*/) for (int i=start,imax=Math.min(end, kern_.length/*size()*/-1); i<=imax; i++) {
		if (kern_[i]!=0f) {  // chunks between non-zero kerns
			super.paintNodeContent(cx, laststart, i);
//*if (kern_[i]==-6f)*/ System.out.println(getName()+"/"+getName().substring(laststart,i+1)+", "+cx.x+" - "+kern_[i]);
			cx.x += kern_[i];
//System.out.print(cx.x);
			/*float kern = kern_[i];			
			if (at==null) cx.x += kern;
			else { cx.x += (float)(kern * at.getScaleX()); cx.y += (float)(kern * at.getShearX()); }*/
//System.out.println(getName().substring(laststart,i+1)+"  "+kern_[i]+" => "+cx.x);
//System.out.print("  k"+kern_[i]+"/"+getName().substring(laststart,i+1));
			laststart = i+1;
		}
	} //else System.out.println("kern len off: "+kern_.length+" != "+size());
//if (start==end) System.out.println("skipped "+start);//name_.charAt(start));
//System.out.println("+"+getName().substring(laststart,Math.min(end,size())));
	return super.paintNodeContent(cx, laststart, end);
  }

  /** Widths adjusted by kerns. */
  public void subelementCalc(Context cx) {
	super.subelementCalc(cx);
	if (kern_!=null && kern_.length==size()) {
		for (int i=0,imax=kern_.length; i<imax; i++) Widths_[i] += kern_[i];
	}
  }

/*  public void dump(int level, boolean recurse) {
	System.out.println(getName()+", ibbox="+Rectangles2D.pretty(ibbox_)+", bbox="+Rectangles2D.pretty(bbox));
  }*/

/*
  public boolean eventNode(java.awt.AWTEvent e, java.awt.Point rel) {
	System.out.println("LeafKern "+getName()+" vs existing "+getBrowser().getCurNode());
	return super.eventNode(e,rel);
  }*/
}
