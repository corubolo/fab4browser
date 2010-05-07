package multivalent.std.adaptor.pdf;

import java.awt.Color;

import multivalent.*;

import phelps.awt.Colors;

import com.pt.awt.NFont;



/**
	A span that can set any PDF graphic state attribute: stroke color, fill color, font, Tr.

	@version $Revision: 1.3 $ $Date: 2003/08/29 04:14:35 $
*/
public class SpanPDF extends Span {
  public Color stroke=Context.COLOR_INVALID, fill=Context.COLOR_INVALID;
  public NFont font=null;
  public int Tr = NFont.MODE_INVALID;

/* doesn't add to layer...
  public SpanPDF(SpanPDF copyme) {
	stroke = copyme.stroke;
	fill = copyme.fill;
	font = copyme.font;
	Tr = copyme.Tr;
  }*/

  public boolean appearance(Context cx, boolean all) {
	if (stroke!=Context.COLOR_INVALID) { cx.strokeColor=stroke; }
	if (fill!=Context.COLOR_INVALID) { cx.foreground=fill; }
	if (font!=null) {
		/*		if (unicode && OS font) {
System.out.println("SpanPDF "+font.getFamily()+" "+font.getSize()/*+" "+font.getStyle()* /);
			cx.family=font.getFamily(); cx.size=(float)font.getSize(); cx.style=font.getStyle();
		} else*/ cx.spot = font;
	}

	// PDF-specific
	//PDFContext pcx = (PDFContext)cx;

	//X if (NFont.MODE_INVISIBLE==Tr) cx.foreground = Colors.TRANSPARENT; => handled in NFont*
	if (NFont.MODE_INVALID!=Tr) cx.mode = Tr;

	return false;
  }

  public String toString() {
	StringBuffer sb = new StringBuffer(20);
	sb.append(getName());
	if (font!=null) sb.append(font.getFamily()).append(':').append(font.getSize()).append('/').append(font.getWeight()).append('/').append(Integer.toHexString(font.getFlags()));
	if (fill!=Context.COLOR_INVALID) sb.append("/F="+fill);
	if (stroke!=Context.COLOR_INVALID) sb.append("/S="+stroke);
	return sb.toString();
  }
}
