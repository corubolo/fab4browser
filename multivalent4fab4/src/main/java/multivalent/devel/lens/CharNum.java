package multivalent.devel.lens;

import java.awt.Graphics2D;
import java.awt.Graphics;

import phelps.lang.Booleans;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.std.GraphicsWrap;
import multivalent.std.lens.Lens;
import multivalent.gui.VCheckbox;
import multivalent.std.ui.DocumentPopup;



/**
	CharNum lens (RFC ref).

	@version $Revision: 1.3 $ $Date: 2002/02/17 18:30:03 $
*/
public class CharNum extends Lens {
  /**
	Switch between hex and decimal displays of character codes.
	<p><tt>"setCharAsHex"</tt>.
  */
  public static final String MSG_SET_HEX = "setCharAsHex";


static class GraphicsCharNum extends GraphicsWrap {
  //** See characters as hex numbers or decimal. <code>null</code> to toggle. */
  //public static final MSG_CHARAS = "lensCharnumCharAsHex";
  //** Setting for MSG_CHARAS. */
  //public static final VALUE_HEX = "hex", VALUE_DECIMAL = "dec";

  static NFont smallfont = NFont.getInstance("Helvetica", NFont.WEIGHT_NORMAL, NFont.FLAG_FIXEDPITCH, 7f);	 // narrowest proportionally-spaced, smallest readable
  static final String[] SNUM = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

  public boolean ashex = false;


  GraphicsCharNum(Graphics2D g) { super(g); }
  public Graphics create() { return new GraphicsCharNum((Graphics2D)wrapped_.create()); }


  public void drawString(String str, int x, int y) {
	// rotate so fit in same space? => show hex and dec of character pointed to within lens
	//NFont f = wrapped_.getFont();
	//wrapped_.setFont(NFont.getInstance("sanserif"/*--want narrowest proportionally-spaced font f.getFamily()*/, f.getStyle()/*Font.PLAIN*/, 7/*--smallest visible f.getSize()/2*/));
	//setFont(smallfont);
	//wrapped_.drawString(sb.substring(0), x,y);	  // too wide.	draw vertically?

	// draw vertically, as horizontal translation of one char to 2 or 3 is too wide
	int h=(int)smallfont.getAscent(), h2=h/2, w=(int)smallfont.charAdvance('0').getX()+1;
	int div=10000, base=10, y0=y-h*3;
	if (ashex) { div=0x1000; base=0x10; y0=y-h*2; }

	for (int i=0,imax=str.length(); i<imax; i++, x+=w) {
		int val=(int)str.charAt(i), wv=val;   // 2 bytes=>4
		boolean fzero=false;
		for (int j=div, yd=y0; j>0; j/=base, yd+=h) {
			int d=wv/j;
			if (d>0 || fzero) {
				smallfont.drawString(wrapped_, SNUM[d], x,yd);
				wv -= d*j;
				fzero=true;
			}
		}
	}
  }

  public void drawChars(char data[], int offset, int length, int x, int y) { try { drawString(new String(data,offset,length), x,y); } catch (StringIndexOutOfBoundsException e) {}; }
  public void drawBytes(byte data[], int offset, int length, int x, int y) { try { drawBytes(/*new String(*/data,offset,length, x,y); } catch (StringIndexOutOfBoundsException e) {}; }
}

  GraphicsCharNum gcn_ = null;
  static boolean AsHex = false;     // should set in restore via property

  public boolean paintBefore(Context cx, Node node) {
	Graphics2D g = cx.g;
	gcn_ = new GraphicsCharNum((Graphics2D)g.create());
	gcn_.ashex = AsHex;
	cx.g = gcn_;

	return super.paintBefore(cx, node);
  }

  /** Restores Graphics2D passed in paintBefore. */
  public boolean paintAfter(Context cx, Node node) {
	if (gcn_!=null) {
		gcn_.dispose();
		gcn_=null;
	}
	return super.paintAfter(cx, node);
  }

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;
	if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_) {
		Browser br = getBrowser();
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Number as Hex", new SemanticEvent(br, MSG_SET_HEX, win_, this, null), menu, "EDIT", false);
		cb.setState(AsHex);
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SET_HEX==msg && se.getIn()==this) {
		AsHex = Booleans.parseBoolean(se.getArg(), !AsHex);
		//gcn_.ashex = AsHex; => exists only during paint
		getBrowser().repaint();
	}
	return super.semanticEventAfter(se, msg);
  }
//	public boolean paintBefore(Context cx, Node node) {
/*	public boolean paintAfter(Context cx, Node node) {
	Browser br = getBrowser();

	Graphics2D g = cx.g;
	Rectangle clip = getContentBounds(); //clip.translate(-br.xoff,-br.yoff);
	Graphics2D g2 = new GraphicsCharNum((Graphics2D)g.create());
	g2.setClip(clip.x,clip.y, clip.width,clip.height); clip = g2.getClipBounds();
	br.getRoot().paintBeforeAfter(g2.getClipBounds(), cx);
	g2.dispose();

	//return true;
	return super.paintAfter(cx, node);
  }*/
}
