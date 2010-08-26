package multivalent.std;

import java.io.*;
import java.awt.image.ImageObserver;
import java.awt.image.FilteredImageSource;
//import java.awt.*;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.List;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.node.FixedLeafOCR;
import multivalent.node.Fixed;
import multivalent.node.FixedLeafImage;
import multivalent.gui.VMenu;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;



/**
	Base class for OCR media adaptors.
	Services: load background image, selectable modes, sets

	@see multivalent.node.FixedLeafOCR
	@see berkeley.adaptor.Xdoc
	@see berkeley.adaptor.PDA

	@version $Revision: 1.7 $ $Date: 2002/04/01 08:44:48 $
*/
public class OcrView extends Behavior implements ImageObserver {
  static final boolean DEBUG = false;

  /** Menu category for OCR-related options ("OCR"). */
  public static final String MENU_CATEGORY = "OCR";

  /**
	Request Image-OCR hybrid view.
	<p><tt>"viewImageOCR"</tt>.
  */
  public static final String MSG_IMAGE_OCR = "viewImageOCR";

  /**
	Request OCR-only view.
	<p><tt>"viewOCR"</tt>.
  */
  public static final String MSG_OCR = "viewOCR";

  /**
	Request Image-only view.
	<p><tt>"viewImage"</tt>.
  */
  public static final String MSG_IMAGE = "viewImage";

  /**
	@see multivalent.node.FixedLeafOCR#SIGNAL
	@see multivalent.node.FixedLeafOCR#MODE_IMAGE
	@see multivalent.node.FixedLeafOCR#MODE_OCR
  */
  public static final String MODE_IMAGE_ONLY = "image-only";

  /**
	Key into Document variables to {@link java.awt.Image} of full page.
  */
  public static final String VAR_FULLIMAGE = "OCRimage";


  static NFont smallfont = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF|NFont.FLAG_ITALIC, 12f);
  static NFont bigfont = NFont.getInstance("Times", NFont.WEIGHT_BOLD, NFont.FLAG_SERIF, 24f);


  // background can be full page or array of images
  //Image fullpage = null;
  FixedLeafImage[] frags_ = null;

  INode/*FixedI*/ ocrroot_ = null;		// handle to current OCR tree.	these are cached
//	double scale = 1.0;
  boolean alldone_ = false;	// image completely loaded?
  //int active=0; // image-OCR
  String[] modes_ = { FixedLeafOCR.MODE_IMAGE, FixedLeafOCR.MODE_OCR, MODE_IMAGE_ONLY };
  String mode_ = FixedLeafOCR.MODE_IMAGE;
  CLGeneral rootStyle_ = null;
//	boolean affected_=false;
//	boolean ffontmet_ = false;

  // ugly hack for stupid OCR software that doesn't report all ink on page
  // later: put in a signal that can be set by anyone
  //public int aswordscnt = 0;	 // number of behaviors that depend on image being displayed as words


  // for FixedLeafOCR -- linked to nodes for full image anyhow
//	public boolean getAffected() { return affected_; }


  static String titles[] = {
	"Image + OCR", MSG_IMAGE_OCR,
	"OCR only", MSG_OCR,
	"Image only", MSG_IMAGE,
//	"OCR Regions", "viewRegions", => use bounds lens
//	"Estimated Font Metrics", "viewFontMetrics", => not useful?
  };

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		INode menu = (INode)se.getOut();

		VRadiogroup rg = new VRadiogroup();
		for (int i=0,imax=3; i<imax; i++) {
			VRadiobox ui = (VRadiobox)createUI("radiobox", titles[i*2], "event "+titles[i*2+1], menu, MENU_CATEGORY, false);
			ui.setRadiogroup(rg);
			if (mode_ == modes_[i]) rg.setActive(ui);
		}
/*
		for (int i=3,imax=5; i<imax; i++) {
			VCheckbox ui = (VCheckbox)createUI("checkbox", titles[i*2], "event "+titles[i*2+1], menu, "Affected", false);
			if (i==3) ui.setState(affected_); else ui.setState(ffontmet_);
		}*/
	}
	return false;
  }


  /**
	Messages recognized:
	<ul>
	<li>viewImageOCR
	<li>viewOCR
	<li>viewImage
<!--	<li>viewRegions
	<li>viewFontMetrics-->
	</ul>
  */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	boolean repaint=true;
	String mode = null;

	// no sense setting menus here, as recreated every time
	if (MSG_IMAGE_OCR==msg) {
		mode = FixedLeafOCR.MODE_IMAGE;

	} else if (MSG_OCR==msg) {
		mode = FixedLeafOCR.MODE_OCR;

	} else if (MSG_IMAGE==msg) {
		mode = MODE_IMAGE_ONLY;
/*	} else if ("viewRegions"==msg) {
		affected_ = !affected_;
	} else if ("viewFontMetrics"==msg) {
		ffontmet_ = !ffontmet_;*/
	} else repaint=false;

	if (mode!=null && ocrroot_!=null) {
		mode_ = mode;
		//ocrroot_.getRoot().putAttr(FixedLeafOCR.SIGNAL, mode);
if (rootStyle_==null) { System.out.println("rootStyle_=null in semEvAf"); System.exit(1); }
		rootStyle_.setSignal(FixedLeafOCR.SIGNAL, mode);	// lenses should override with higher priority
	}

	if (repaint) getBrowser().repaint();

	return super.semanticEventAfter(se, msg);
  }




  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
	Browser br = getBrowser();

	// make image transparent on white
	//System.out.println("imageUpdate "+infoflags);
	alldone_ = (infoflags & (ImageObserver.ALLBITS | ImageObserver.ERROR)) != 0;

	if ((infoflags & ImageObserver.WIDTH)!=0 && (infoflags & ImageObserver.HEIGHT)!=0) {
		//ocrroot_.bbox.setSize(width,height);	  // doesn't last long!
	}

	if ((infoflags & ImageObserver.ALLBITS)!=0) {
		formatAfter(ocrroot_);
		br.repaint(100);

	} //else if ((infoflags & ImageObserver.SOMEBITS)!=0 => no repaint

	if (DEBUG && (infoflags&ImageObserver.ERROR)!=0) System.err.println("error loading image");
	return !alldone_;
  }



  public void buildAfter(Document doc) {
	//if (doc.getAttr(Document.ATTR_PAGE)==null) return; => not necessarily paginated, for now
	//if (ocrroot_==null) return;
	if (doc.childAt(0) instanceof /*FixedI*/INode) ocrroot_=(INode/*FixedI*/)doc.childAt(0); else return;
	Object o = doc.getVar(VAR_FULLIMAGE);
	if (o instanceof FixedLeafImage) { /*fullpage = (Image)o;*/ frags_ = new FixedLeafImage[1]; frags_[0]=(FixedLeafImage)o; }
	else if (o instanceof List) { List<FixedLeafImage> list = (List<FixedLeafImage>)o; frags_ = list.toArray(new FixedLeafImage[list.size()]); }
	//else null or error

	Browser br = getBrowser();
	for (int i=0,imax=frags_.length; i<imax; i++) {
		//FixedleafImage fl = frags_[i];
		//Image img = fl.getImage();
		//img = br.createImage(new FilteredImageSource(img.getSource(), new TransparentFilter(Color.WHITE)));
		//fl.setImage(img);
	}

//System.out.println("fullpage = "+fullpage);

	StyleSheet ss = doc.getStyleSheet();
	String name = ocrroot_.getName();
	rootStyle_ = (CLGeneral)ss.get(name);
	if (rootStyle_ == null) { rootStyle_ = new CLGeneral(); ss.put(name, rootStyle_); }

	// maintain browser's current setting, if any -- unrecognized settings OK
	mode_ = doc.getRoot().getAttr(FixedLeafOCR.SIGNAL, FixedLeafOCR.MODE_IMAGE);
	rootStyle_.setSignal(FixedLeafOCR.SIGNAL, mode_);

	alldone_ = false;
	// X convert absolute coordinates to relative once, after whatever tree hacking (table sorting, biblio), and before other formatting
	// => now done by parent layouts

	ocrroot_.addObserver(this);
  }


  public boolean formatAfter(Node node) {
//	System.out.println("formatAfter on "+node);
	/*if (fullpage!=null) {	// may be browsing OCR-only
		int w=fullpage.getWidth(this), h=fullpage.getHeight(this);
		//node.bbox.add(0,0);
		  if (w!=-1 && h!=-1) node.bbox.add(w,h);	 // otherwise bbox just big enough to enclose text
//System.out.println("adding image dim to "+w+"x"+h);
	}*/

	if (frags_!=null) for (int i=0,imax=frags_.length; i<imax; i++) {
		FixedLeafImage l = frags_[i];
		//l.formatBeforeAfter(-1,-1, null); => don't care about node itself
		Image img = l.getImage();
		int w=img.getWidth(this), h=img.getHeight(this);
		Rectangle bbox = l.getIbbox();
//System.out.println("image bbox = "+bbox);
		if (w!=-1 && h!=-1) node.bbox.add(bbox.x+w, bbox.y+h);	 // otherwise bbox just big enough to enclose text
	}

	return false;
  }


  /*
   * PAINT
   */
	// interest registered on OCR node
  // background is full image, if possible, to avoid OCR dropouts
  public boolean paintBefore(Context cx, Node node) {
//System.out.println("OcrMediaAdaptor paintBefore");
	cx.reset(); //cx.valid=false;
	//Browser br = getBrowser();
	//Point offpt = node.getAbsLocation();	// root of tree

	Graphics2D g = cx.g;
	//Rectangle clip = g.getClipBounds();

	//System.out.println("PDA: paintBefore"+fullpage+" within "+g.getClipBounds());
//	  cx.signal.put("PDAAFFECTED", affected_?Boolean.TRUE:Boolean.FALSE);
//	  br.putGlobal("PDAAFFECTED", affected_?Boolean.TRUE:Boolean.FALSE);
	String ocrSignal = (String)cx.signal.get(FixedLeafOCR.SIGNAL);	  // obey current setting -- may be overridden by lens
//System.out.println("PDA read "+FixedLeafOCR.SIGNAL+" = "+ocrSignal);
	//if (ocrSignal==null) { ocrSignal = (active==1?"ASCII":"IMAGE"); cx.signal.put("OCR", ocrSignal); }
	//boolean imageMode = ocrSignal.equalsIgnoreCase("IMAGE");
//	  br.putAttr("ocr", "IMAGE"); // usually "image"
//System.out.println("OcrView mode = "+ocrSignal);
	if (ocrroot_.size()==0) {
		bigfont.drawString(g, "This page intentionally left blank", 20, 100);
		return true;

//		  cx.signal.put("OCR", "ASCII");
//		  ocrSignal = "ASCII";
	} else if (FixedLeafOCR.MODE_OCR.equals(ocrSignal)) {
		// don't draw any image -- and don't initiate load of one either so can quickly scan ascii version of pages

/*	} else if (!alldone_) { => PDF supplies BufferedImage, which never calls ImageUpdate to set alldone_=true
//System.out.println("drawing background @0,0");
		//cx.signal.put(FixedLeafOCR.SIGNAL, "ocr");  // show OCR in the meantime?

		g.drawImage(fullpage, 0,0, cx.background, this);
		return true;	// opaque
*/

	} else if (MODE_IMAGE_ONLY.equals(ocrSignal)) {
		/*if (fullpage!=null) g.drawImage(fullpage, 0,0, cx.background, this);	// in Java 1.1, clip this to cliprect
		else*/ if (frags_!=null) for (int i=0,imax=frags_.length; i<imax; i++) {
			//frags_[i].paintNode(null, cx); => node just packaging
			FixedLeafImage l = frags_[i];
			Rectangle ibbox = l.getIbbox();
//System.out.println(i+" "+ibbox);
			g.drawImage(l.getImage(), ibbox.x,ibbox.y, cx.background, this);
		}
		return true;	// opaque

	} else if (FixedLeafOCR.MODE_IMAGE.equals(ocrSignal)) {
		Document doc = node.getDocument();
//System.out.println("fullpage in OcrView "+fullpage.getWidth(this)+"x"+fullpage.getHeight(this));
		// draw background
		if (doc.getAttr(Fixed.ATTR_REFORMATTED)==null) {
			//g.drawImage(fullpage, 0,0, cx.background, this);	// in Java 1.1, clip this to cliprect
			/*if (fullpage!=null) g.drawImage(fullpage, 0,0, cx.background, this);	// in Java 1.1, clip this to cliprect
			else*/ if (frags_!=null) for (int i=0,imax=frags_.length; i<imax; i++) {
				FixedLeafImage l = frags_[i];
				Rectangle ibbox = l.getIbbox();
				g.drawImage(l.getImage(), ibbox.x,ibbox.y, cx.background, this);
			}

		} else {
			g.setColor(Color.RED);
			smallfont.drawString(g, "Parts of image not recognized by OCR not shown; use View/Full Image Only to see.", 10, 15);   // discard unless PDF's recognition engine is as retarded as XDOC's
		}
		// retards responsiveness but PDA bbox info lossy
//System.out.println("fullimage at "+g.getClipBounds()+", bg="+cx.background);

	} //else filterpage = null;

	return false;
  }
}
