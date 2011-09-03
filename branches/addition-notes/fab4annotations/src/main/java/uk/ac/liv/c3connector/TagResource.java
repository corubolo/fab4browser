
///SAM
package uk.ac.liv.c3connector;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Map;

import com.pt.awt.NFont;

import phelps.awt.Colors;
import phelps.lang.Booleans;
import phelps.lang.Integers;
import uk.ac.liv.c3connector.ui.FabAnnoListRenderer;
import uk.ac.liverpool.fab4.Fab4utils;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.StyleSheet;
import multivalent.gui.VFrame;
import multivalent.gui.VScrollbar;
import multivalent.gui.VTextArea;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;

public class TagResource extends Behavior{

	VFrame win_ ;
	Rectangle opos;
	Document doc_;
	protected Color deffg_=Color.BLACK, defbg_=Color.CYAN.brighter();	
	String ocont;
//	boolean viz_ = true;
	
	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer){
		super.restore(n,attr, layer);
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		float zl = doc.getMediaAdaptor().getZoom();
		doc = getDocument();
		win_ = new VFrame("Note",null, doc);
		win_.setShrinkTitle(true);
		win_.setTransparent(true);
		win_.setTitle("Your tags");
		win_.setPinned(getAttr(FabNote.ATTR_FLOATING)==null);
		Rectangle r = new Rectangle();
		int aa = 100;
		r.x = (int)(Integers.parseInt(getAttr("x"),aa) * zl);
		r.y = (int)(Integers.parseInt(getAttr("y"),aa) * zl);
		r.width = Integers.parseInt(getAttr("width"),220);
		r.height = Integers.parseInt(getAttr("height"),100);
		win_.setBounds(r.x,r.y,r.width,r.height);
		opos = new Rectangle(win_.getBbox());

		doc_ = new Document("Note",null, win_);	// free scrolling in Note!
		String name = "TAG"+String.valueOf(Math.abs(FabNote.random.nextInt()));
		putAttr("name", name);
		doc_.padding = INode.INSETS[3];
		doc_.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
		
		Layer sublay = doc_.getLayers();
		ESISNode content = null;
		if (n!=null) {
			sublay.restoreChildren(n, sublay);
			content = sublay.getAux("content");     // beautiful: pagewise annos and now this in aux
		}

		Browser br = getBrowser();
		final INode body = new IVBox("body", null, null);
		final VTextArea ed = new VTextArea("ed", null, doc_, body);
		FabAnnotation fa = (FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO);
		ed.editable = false;
		if (fa!=null)
			if (fa.getSigner()!=null)
				if (fa.getSigner().isItsme())
					ed.editable = true;
		ed.setScrollbarShowPolicy(VScrollbar.SHOW_AS_NEEDED);
		//ed.editable = isEditable();
		ed.setSizeChars(0, 0); // override VTextArea to set dimensions directly
		//ed.
		if (content != null && content.size() > 0
				&& ((String) content.childAt(0)).trim().length() > 0) {
			win_.setIn(false);
			String txt = (String) content.childAt(0);
			//ocont = txt;
			if (!txt.endsWith("<br/>"))
				txt += "<br/>"; // sentinal

			// later: convert all escape characters first
			int lstart = 0;
			//System.out.println(txt);
			for (int i = txt.indexOf("<br/>"); i != -1; lstart = i + 5, i = txt
			.indexOf("<br/>", lstart)) {
				final INode line = new IParaBox("line", null, null);
				// line.breakafter = true;
				int wstart = lstart;
				for (int j = txt.indexOf(' ', lstart); j < i && j != -1; wstart = j + 1, j = txt
				.indexOf(' ', wstart))
					if (j - wstart > 0)
						new LeafUnicode(txt.substring(wstart, j), null, line);
				if (i - wstart >= 1)
					new LeafUnicode(txt.substring(wstart, i), null, line);

				if (line.size() > 0)
					body.appendChild(line);
			}

		} else
			content = null;
		if (body.size() == 0) { // could have empty saved body, in addition to
			// fresh
			final INode protoline = new IParaBox("line", null, body);
			new LeafUnicode("", null, protoline);
		}
		//  ********* 	new note *************
		if (content == null) {
			if (win_.isPinned())
				win_.bbox.translate(doc.getHsb().getValue(), doc.getVsb()
						.getValue()); // adjust coordinates to scroll position
			// set focus and cursor here
			final Leaf l = body.getFirstLeaf();
			br.getCursorMark().move(l, 0);
			br.setCurNode(l, 0);
			br.setScope(doc_);
			br.setCurDocument(doc);
			win_.setClosable(false);
//			setFontTitle(VFrame.FONT_TITLE_ITALIC);
//			setTitleBg(FabAnnoListRenderer.TOPUBLISH);
			ed.editable = true;
			win_.raise();
		}

		// colors
		final StyleSheet ss = doc_.getStyleSheet();
		CLGeneral gs = new CLGeneral();
		gs.setForeground(Colors.getColor(getAttr("foreground"), deffg_));
		gs.setBackground(Colors.getColor(getAttr("background"), defbg_));
		// font...
		///SAM
		if (FabNote.FONT_TEXT == null)
			////SAM it was 'Sans' I changed it to Arial
		    FabNote.FONT_TEXT = NFont.getInstance("Arial", NFont.WEIGHT_NORMAL,
                        NFont.FLAG_EXPANDED, 14);
		gs.setFont(FabNote.FONT_TEXT);
		ss.put("note", gs);
		gs = new CLGeneral();
		gs.setPadding(5);
		ss.put("ed", gs);
		//if (win_!=null)
		win_.setTitle("Your tags");
		ocont = Fab4utils.getTextSpaced(doc_);


	}
	
	@Override
	public ESISNode save(){
		ESISNode e;
		
		
		e = super.save();
		if (getAttr("uri")==null) {  // inline -- for now only editable kind
			StringBuffer csb = getStringContent();
			ESISNode content = new ESISNode("content");
			e.appendChild(content);
			content.appendChild(csb.toString());
		}
		
		// save annos on note
		Layer sublay = doc_.getLayers();
		String layname = getLayer().getName();
		for (int i=0,imax=sublay.size(); i<imax; i++) {
			Layer l = (Layer)sublay.getBehavior(i);
			ESISNode sube = null;
			if (layname.equals(l.getName()))
				sube=l.save();
			if (sube!=null)
				e.appendChild(sube);
		}

		return e;
	}
	
	private StringBuffer getStringContent() {
		StringBuffer csb = new StringBuffer(2000);
		doc_.clipboardBeforeAfter(csb);  // doesn't save character attributes yet
		String trim = csb.substring(0).trim();
		csb.setLength(0);
		for (int i=0,imax=trim.length(); i<imax; i++) {
			char ch = trim.charAt(i);
			if (ch=='\n')
				csb.append("<br/>");
			else
				csb.append(ch);
		}
		// ampty string creates a <content/> that breaks MV's xml parser
		if (csb.length()==0)
			csb.append("<br/>");
		//System.out.println("00!!!!!");
		return csb;
	}
	
	@Override
	public void destroy() {
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		if (win_!=null)
			win_.close();
		win_=null;
		if (getValue(DistributedPersonalAnnos.FABANNO)!=null)
			((FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO)).setLoaded(false);
		try {
			if (doc!=null)
				doc.deleteObserver(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
}
