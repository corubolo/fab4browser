package multivalent.std.adaptor;

import java.io.File;
import java.io.IOException;

import phelps.lang.Strings;
import phelps.net.URIs;

import com.pt.awt.NFont;
import com.pt.awt.font.*;

import multivalent.*;
import multivalent.node.LeafUnicode;
import multivalent.node.IVBox;
import multivalent.node.IParaBox;
import multivalent.std.span.FontSpan;



/**
	Show sampler of characters in font file.

	@version $Revision: 1.1 $ $Date: 2003/12/20 21:08:57 $
*/
public class FontSampler extends MediaAdaptor {
  static final boolean DEBUG = false;

  public Object parse(INode parent) throws IOException {
	NFont font = null;
	try { font = NFontManager.createFont(URIs.toURL(getURI()), null); }
	catch (Exception e) {}

	INode top = new IVBox("font",null, parent);
	if (font==null) return new LeafUnicode("unknown font format",null, top);


	// metadata
	StringBuffer sb = new StringBuffer(1000);
	sb.append("<html><head><style>background: white;</style></head>");
	sb.append("<h2>Info</h2>\n");
	sb.append("<ul>\n");
	sb.append("<li><b>Name:</b> ").append(font.getName());
	sb.append("<li><b>Family:</b> ").append(font.getFamily());
	sb.append("<li><b>Format:</b> ").append(font.getFormat());
	sb.append("<li><b># glyphs:</b> ").append(font.getNumGlyphs());
	sb.append("</ul>\n");
	sb.append("<h2>Sampler</h2>\n");

	INode p;/* = (INode)parseHelper(sb.toString(), "HTML", getLayer(), top);
	p.remove();
	p = (INode)p.findDFS("body",null,null,Integer.MAX_VALUE);
	//top.appendChild();
	for (int i=0,imax=p.size(); i<imax; i++) top.appendChild(p.childAt(0));
	*/
	// characters
	p = new IParaBox("sampler",null, top);
	font = ((NFontSimple)font).deriveFont(Encoding.IDENTITY, CMap.IDENTITY);
	for (int i=0,imax=Math.min(font.getMaxGlyphNum(),4*1024); i<imax; i++) if (font.canDisplayEchar(i)) new LeafUnicode(Strings.valueOf((char)i),null, p);
	FontSpan span = (FontSpan)Behavior.getInstance("font", "multivalent.std.span.FontSpan",null, getLayer());
	font = font.deriveFont(24f); span.size = 24f;
	span.spot = font;
	span.moveq(p.getFirstLeaf(),0, p.getLastLeaf(),p.getLastLeaf().size());

	//return new LeafUnicode("not implemented",null, parent);
	return top;
  }
}
