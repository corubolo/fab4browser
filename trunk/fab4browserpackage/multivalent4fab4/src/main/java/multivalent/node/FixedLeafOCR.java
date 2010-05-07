package multivalent.node;

import java.awt.image.ImageObserver;
import java.awt.*;
import java.util.Map;

import phelps.awt.Colors;

import com.pt.awt.NFont;

import multivalent.Context;
import multivalent.INode;
//import multivalent.node.Fixed;



/**
	Draw by reaching clipping out from who page image that portion corresponding to word.

	@see berkeley.adaptor.PDA
	@see berkeley.adaptor.Xdoc
	@see multivalent.std.adaptor.pdf.PDF

	@version $Revision: 1.6 $ $Date: 2002/08/14 00:53:57 $
*/
public class FixedLeafOCR extends LeafText implements Fixed, ImageObserver/*, Cloneable*/ {
  public static final String SIGNAL = "viewOcrAs";
  public static final String MODE_IMAGE = "image";
  public static final String MODE_OCR = "ocr";



  Rectangle ibbox_;	// initial bbox -- for scanned pages, always need so can find word in image
  public int ibaseline;
  public NFont font;    // => spans
  //Image fullpage;	// pass this in Context as a signal? in Document as var?  not great that each leaf has this pointer, but not that many words on a single page
  FixedLeafImage imgnode_;


  public FixedLeafOCR(String name,Map<String,Object> attr, INode parent, FixedLeafImage imgnode/*Image fullpage/*Xdoc xdoc*/, Rectangle ibbox) {
	super(name,attr, parent);
	ibbox_ = ibbox;
	//this.fullpage = fullpage;
	imgnode_ = imgnode;
  }

  public Rectangle getIbbox() { return ibbox_; }

  public int size() { return 1; }	// override LeafText name_.length(), which overrides Leaf's 1


  /* *************************
   * PROTOCOLS
   **************************/

/*
  public boolean formatBeforeAfter(int width,int height, Context cx) {
	if (valid_) { bbox.move(ibbox.x,ibbox.y); baseline=ibaseline; return false; }
	else return super.formatBeforeAfter(width,height, cx);
  }
*/
  public boolean formatNodeContent(Context cx, int start, int end) {
	if (start==0) bbox.setBounds(ibbox_);
	else bbox.setBounds(ibbox_.x/*+ibbox.width*/,ibbox_.y, 0,0);
	baseline = ibaseline;
	return false;
  }


  // could use owner's, but don't slow things any more than necessary with its more complex implementation
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
	return (infoflags & (ImageObserver.ALLBITS | ImageObserver.ERROR))==0;  // "true if further updates are needed"
  }


  // draw from ibbox to bbox
  public boolean paintNodeContent(Context cx, int start, int end) {
	if (start >= size()/*name_.length()*/) return false;
//if (cx.underline!=null) System.out.println(name_+" "+start+".."+end);

	//int absbaseline = bbox.y+baseline;//+bbox.height-ibbox.height;
	//int absbaseline = 0+baseline;//+bbox.height-ibbox.height;
	String ocrSignal = (String)cx.signal.get(SIGNAL);	  // don't take from media adaptor: obey current setting -- may be overridden by lens or span
	Graphics2D g = cx.g;
//if ("Hypertext".equals(getName())) System.out.println(getName()+", mode="+ocrSignal+", ibbox="+ibbox+", fg="+cx.foreground+", bg="+cx.background+", img="+fullpage);
//if (cx.foreground==Colors.TRANSPARENT) System.out.print(" Tr");
	if (MODE_IMAGE.equals(ocrSignal) || ocrSignal==null) {	  // draw word even if already drawn in full page in order to draw background
		// can't draw in foreground color, so have to make special check for invisible
		if (cx.foreground == cx.background && cx.foreground!=Colors.TRANSPARENT) {
			g.setColor(cx.foreground);
			g.fillRect(0,0, bbox.width,bbox.height);

		} else {
			int y=bbox.height-ibbox_.height;	// bbox can get taller, but not smaller
//if (cx.background!=Color.WHITE) System.out.println(getName()+", bg="+cx.background+", "+ibbox+" => "+y);    //+", trans="+fullpage);
			Rectangle imgibbox = imgnode_.getIbbox();
			int sx = ibbox_.x - imgibbox.x, sy = ibbox_.y - imgibbox.y, w=ibbox_.width, h=ibbox_.height;
//System.out.println("word image |"+name_+"|, "+ibbox_+" vs "+imgibbox+" = "+imgnode_.getImage().getWidth(this)+"x"+imgnode_.getImage().getHeight(this));
			/*if (cx.foreground!=Colors.TRANSPARENT)*/ g.drawImage(imgnode_.getImage(),
				0,y, 0+w,y+h,	 // draw just word--bbox may have different dimensions
				sx,sy, sx+w,sy+h,
				cx.background, this);
//				Color.GREEN, this);
		}

	} else if (MODE_OCR.equals(ocrSignal)) {
		// draw OCR
//System.out.println("drawing "+name_+" w/bbox "+bbox);
		if (cx.background!=Colors.TRANSPARENT && cx.background!=null && !cx.background.equals(cx.pagebackground)) {/*--needed by OCR lens*/ g.setColor(cx.background); g.fillRect(0,0+bbox.height-ibbox_.height, ibbox_.width,ibbox_.height); }
		if (cx.foreground!=Colors.TRANSPARENT) {
			g.setColor(cx.foreground);
//System.out.println(cx.getFont()+" "+name_);
			NFont f = font!=null? font: cx.getFont();      // move Xdoc and PDA to using spans
			f.drawString(g, name_, 0, baseline);
		}

	} /*else if ("image-only".equals(ocrSignal)) {
		// already drawn full page image
	}*/

	cx.x += bbox.width;

	return false;
  }

  //public int subelement(Point rel) { if (rel.x>bbox.width/3) return size(); else return 0; }
  public void subelementCalc(Context cx) {
	//if (Widths_.length < len) Widths_=new int[len]; -- always at least 1
	Widths_[0] = bbox.width;
  }

  public int subelementHit(Point rel) { return (rel.x < bbox.width/2? 0: size()); }


  public void clipboardNode(StringBuffer sb) {
	// already done before and after
	sb.append(name_);
	sb.append(' ');	// get rid of this
  }

  public void clipboardBeforeAfter(StringBuffer sb, int start, int end) {
	// iterate over observers, owner?
	sb.append(name_.substring(Math.max(start, 0), Math.min(end, size())));
  }


  public void dump(int level, int maxlevel) {
	for (int i=0; i<level; i++) System.out.print("	 ");
	System.out.println(
		name_
		+"/"+childNum()
//		+ ", nextLeaf = "+getNextLeaf()
		+", class="+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1)
		+", bbox="+bbox.width+"x"+bbox.height+"@("+bbox.x+","+bbox.y+")/"+ baseline
		+", ibbox="+ibbox_.width+"x"+ibbox_.height+"@("+ibbox_.x+","+ibbox_.y+")/"//+ ibaseline
		+", sticky="+sticky_
//		+", owner="+owner
//		+", parent="+parent_
//		+", baseline="+baseline
		+", valid="+valid_
//		+", observers="+observers_
		);
  }
}
