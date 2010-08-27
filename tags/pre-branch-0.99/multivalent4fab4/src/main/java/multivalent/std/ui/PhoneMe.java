package multivalent.std.ui;

import java.applet.AudioClip;
import java.applet.Applet;

import multivalent.*;
//import multivalent.std.ui.DocumentPopup;

import phelps.lang.Strings;



/**
	Play selected text as if on telephone touchtone.
	<a href="http://www.laffnow.com/humor/touch_to.htm">Touch-tone phone songs</a>.

	<p>LATER: pauses, awareness of current phone number.

	@see multivalent.std.LinkMarkup

	@version $Revision: 1.2 $ $Date: 2002/02/01 06:50:57 $
*/
public class PhoneMe extends Behavior {
  /**
	Play characters in <var>arg</var> as on telephone touchtone.
	<p><tt>"touchtone"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>letters-to-play</var>.
  */
  public static final String MSG_PLAY = "touchtone";

  static int[] al2num_ = { 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, -1, 7, 7, 8, 8, 8, 9, 9, 9, -1 };
  static AudioClip[] tone = null;

  boolean stop = false;



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==getBrowser().getSelectionSpan()) {
		Span sel=null;
		String name=null;
		if (se.getIn()==(sel=getBrowser().getSelectionSpan())) {
			Mark s=sel.getStart(), e=sel.getEnd();
			Leaf sl=s.leaf, el=e.leaf;
			if (sl==el) {
				if (sl.getName()!=null) name=sl.getName().substring(s.offset, e.offset);
			} else {
				StringBuffer sb = new StringBuffer(50);
				sb.append(sl.getName().substring(s.offset));
				for (Leaf l=sl.getNextLeaf(); l!=el; l=l.getNextLeaf()) sb.append(l.getName());
				sb.append(el.getName().substring(0,e.offset));
				name = sb.substring(0);
			}
		} else {
			Node n = getBrowser().getCurNode();
			//Leaf l = getBrowser().getCurMark().leaf;
			//if (l!=null) name = l.getName();
			if (n!=null && n.isLeaf()) name = n.getName();
		}

		if (name!=null && name.length()>=5 /*&& name.length()<30*/) {	// no max cause of songs
			createUI("button", "Touch-tone \""+Strings.trimPunct(name)+"\"", new SemanticEvent(getBrowser(), MSG_PLAY, name), (INode)se.getOut(), "NAVIGATE", false);
		}
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();
	if (MSG_PLAY==msg && arg instanceof String) phone((String)arg);
	else if (Document.MSG_STOP==msg) stop = true;

	return super.semanticEventAfter(se,msg);
  }


  /**
	Ignores non-alnum, translate alpha to digit equivalent.
	Other characters taken as pauses.
  */
  public void phone(String number) {
	if (tone==null) {   // load on demand
		tone = new AudioClip[12];
		// can't be static because getClass()
		String pfx = "/sys/sounds/touchtone/touchtone.";
		for (int i=0; i<10; i++) tone[i] = Applet.newAudioClip(getClass().getResource(pfx+(char)(i+'0')+".au"));
		tone[10] = Applet.newAudioClip(getClass().getResource(pfx+"star.au"));
		tone[11] = Applet.newAudioClip(getClass().getResource(pfx+"pound.au"));
	}

	stop = false;
	for (int i=0,imax=number.length(); !stop && i<imax; i++) {
		char ch = Character.toLowerCase(number.charAt(i));
		int num=-1;
		if (ch>='0' && ch<='9') num = ch-'0';
		else if (ch>='a' && ch<='z') num=al2num_[ch-'a'];
		else if (ch>='A' && ch<='Z') num=al2num_[ch-'A'];
		else if (ch=='*') num=10;
		else if (ch=='#') num=11;

		if (num!=-1) tone[num].play();
		try { Thread.sleep(300); } catch (InterruptedException e) {}
	}
  }
}
