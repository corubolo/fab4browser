package multivalent.std;

import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
//import java.util.HashMap;
import java.util.Map;
//import java.text.DateFormat;

import phelps.util.Dates;

import multivalent.*;
import multivalent.node.IParaBox;
import multivalent.node.LeafUnicode;



/**
	BROKEN.
	Collect various problem reports, display in new Note.
	Note is pinned to screen, remaining available as user scrolls along looking for reattachment points.

	Spans reconstitued on context, from which they are movable to manually redetermined positions.

	@version $Revision$ $Date$
*/
public class RestoreReport extends Behavior {
  Note note=null;
  INode contentNode=null;
//	LensMan lensman=null;

//	public void buildBefore(Document root) { root.addObserver(this); }
/*
  public void buildAfter(Document doc) {
	if (note!=null) note.getRoot().dump();
  }*/

  // this should be in Note or perhaps Node/INode/Leaf
  protected INode appendLine(String txt) {
	INode line = new IParaBox("LINE",null, contentNode);
	StringTokenizer st = new StringTokenizer(txt);
	if (txt.length()==0) new LeafUnicode(" ",null, line);
	else while (st.hasMoreTokens()) new LeafUnicode((String)st.nextToken(),null, line);
	return line;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println("RestoreReport: "+cmd);
		// eavesdrop on various kinds of messages
	Object arg=se.getArg();
	if (Span.MSG_UNATTACHED==msg /*|| "xxx"==msg*/) {
System.out.println("RestoreReport unattachedSpan "+arg);
/*
		makeNote();
		if (Span.MSG_UNATTACHED==msg) unattachedSpan(arg);
*/
		// else if ("xxx"==cmd) ...
	}
	return false;
  }

  protected void makeNote() {
	if (note!=null) return;

	Browser br = getBrowser();
	Document doc = br.getCurDocument();
	note=(Note)Behavior.getInstance("note","Note",null, doc.getLayer(Layer.PERSONAL));	// in PERSONAL so can edit.  doc.getCurrentLayer());
	//Map attrs = new CHashMap(); attrs.put("BACKGROUND","orange");
	//note.restore(null,null, br.getCurrentLayer());
//	note.foreground=Color.BLACK; note.background=Color.ORANGE;
	//note.addToLayer();
	//br.getCurDocument().addObserver(note);
//System.out.println("observer on "+br.getRoot().childAt(0));
//		br.getCurDocument().addObserver(newnote);
//	lensman.addLens(note);
	//note.doc=br.getCurDocument();
	//contentNode = new IVBox("LINES",null, (INode)note.getRoot().findDFS("CONTENT"));
//RESTORE	contentNode = (INode)note.getRoot().findDFS("NOTE");
	contentNode.removeAllChildren();
	INode line=appendLine("Unattached Annotation List");
	Span span = (Span)Behavior.getInstance("bold","BoldSpan",null, getLayer());
	span.moveq(line.getFirstLeaf(),0, line.getLastLeaf(),line.getLastLeaf().size());
  }

  protected void unattachedSpan(Object o) {
	if (o==null || !(o instanceof Span)) return;
	Span unspan = (Span)o;
	appendLine("");
	/*Hash*/Map pstart=unspan.pstart, pend=unspan.pend;
	INode line = new IParaBox("LINE",null, contentNode);
	if (pstart==null || pend==null) { new LeafUnicode("BAD SPEC",null, line); return; }

	// extract all possibly useful bits -- don't have to maximize efficiency here
	String scontext=(String)pstart.get("CONTEXT"), econtext=(String)pend.get("CONTEXT");
	StringTokenizer sst=new StringTokenizer(scontext);
	String sa=null,sl=null,sr=null;
	try { sa=sst.nextToken(); sl=sst.nextToken(); sr=sst.nextToken(); } catch (NoSuchElementException snsee) {}
	StringTokenizer est=new StringTokenizer((String)pend.get("CONTEXT"));
	String ea=null,el=null,er=null;
	try { ea=est.nextToken(); el=est.nextToken(); er=est.nextToken(); } catch (NoSuchElementException ensee) {}
	if (sa==null && ea!=null) { sa=ea; sl=el; sr=er; ea=el=er=null; }

	// zap redundant words
	int si=0, ei=(ea!=null?ea.length():sa.length());
	boolean felipsis=true;
	String stree=(String)pstart.get("TREE"), etree=(String)pend.get("TREE");
	int sx1=stree.indexOf(' '), sx2=stree.indexOf('/'), sx3=stree.indexOf(' ',sx2);
	int ex1=stree.indexOf(' '), ex2=stree.indexOf('/'), ex3=etree.indexOf(' ',ex2);
	try {
		si=Integer.parseInt(stree.substring(0,sx1)); ei=Integer.parseInt(etree.substring(0,ex1));
		int schi=Integer.parseInt(stree.substring(sx1+1,sx2)), echi=Integer.parseInt(etree.substring(ex1+1,ex2));
		felipsis=false;
		if (stree.substring(sx3).equals(etree.substring(ex3))) {	// same except for child numbers
			if (schi==echi) ea=el=er=null;	// starts and ends at same node: "<before1> <node1> <after1>"
			else if (schi+1==echi) sr=el=null;	// "<before1> <node1> <node2> <after2>"
			else if (schi+2==echi) el=null;	// "<before1> <node1> <after1> <node2> <after2>"
				  // "<before1> <node1> <after1> <before2> <node2> <after2>"
			else if (schi+4>=echi) felipsis=true;	// "<before1> <node1> <after1> ... <before2> <node2> <after2>"
		}
	} catch (NumberFormatException nfe) {}

	// add to note tree
	LeafUnicode sn=null, en=null;
	if (sl!=null) new LeafUnicode(sl,null, line);
	sn=en=new LeafUnicode(sa,null, line);
	if (sr!=null) new LeafUnicode(sr,null, line);
	if (felipsis) new LeafUnicode("  ...	",null, line);
	if (el!=null) new LeafUnicode(el,null, line);
	if (ea!=null) en=new LeafUnicode(ea,null, line);
	if (er!=null) new LeafUnicode(er,null, line);

	// CREATEDAT now an integer

	String createdat=unspan.getAttr("createdat");
	if (createdat!=null) {
		try { createdat = Dates.relative(Dates.parse(createdat)); } catch (java.text.ParseException pe) { /*keep whatever already have*/ }
		LeafUnicode cn=new LeafUnicode("(created " + createdat + ")",null, line);	// convert to relative date first
		((Span)Behavior.getInstance("italic","ItalicSpan",null, getLayer())).moveq(cn,0, cn,cn.size());
	}
	// also report by whom?

	unspan.move(sn,0, en,en.size());
  }
}
