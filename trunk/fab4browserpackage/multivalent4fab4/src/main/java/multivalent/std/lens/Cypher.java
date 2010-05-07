package multivalent.std.lens;

import java.awt.Graphics2D;
import java.awt.Graphics;

import multivalent.*;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;
import multivalent.std.GraphicsWrap;
import multivalent.std.ui.DocumentPopup;



/**
	(De)Greek/Cyrillic lens: translate Greek characters to Latin equivalents.
	(De)Cypher lens: Rot-13, Caesar, reverse, Pig Latin, alpha shift left/right, keyboard shift (McIntee cypher).

	<table border>
	<tr><th>Semantic Event<th>In<th>Description
	<tr><td>rot13   <td rowspan=5>this (lens handle)<td>Rot-13
	<tr><td>Caesar  <td rowspan=5>this (lens handle)<td>letter+3, e.g., a=>d ("Rot-3")
	<tr><td>reverse <td>Reverse sequence of letters in word
	<tr><td>Pig Latin<td rowspan=5>this (lens handle)<td>Pig Latin decoding
	<tr><td>alpha1  <td>shift alphabet over one letter
	<tr><td>alpha-1 <td>shift alphabet left one letter
	<tr><td>keyboard1<td>key = key to right on keyboard (Mike McIntee cypher)
	</table>

	@version $Revision: 1.5 $ $Date: 2003/06/02 05:50:42 $
*/
public class Cypher extends Lens {
// LATER: Greek, Cyrillic  => Latin,
  static char[]
	REVERSE = new char[0], PIGLATIN = new char[0], PRINCE=new char[0],
	ROT13 = new char[256], CAESAR = new char[256],
	ALPHAL1 = new char[256], ALPHAR1 = new char[256],
	KEYBOARDR1 = new char[256];

  static char[][] MAPS = { ROT13, CAESAR, REVERSE, PIGLATIN, ALPHAL1, ALPHAR1, /*PRINCE,*/ KEYBOARDR1 };
  static String[] TITLES = { "Rot-13", "Caesar", "Reverse", "Pig Latin", "Alpha Shift Left", "Alpha Shift Right", /*"Prince",*/ "Keyboard Shift Right" };
  static String[] MSGS = { "rot13", "caesar", "reverse", "pigLatin", "alpha1", "alpha-1", /*"prince",*/ "keyboard1" };
  static String imsg_ = MSGS[0];
  static char[] imap_ = MAPS[0];

  static {
	for (int i=0; i<256; i++) ROT13[i] = CAESAR[i] = ALPHAL1[i] = ALPHAR1[i] = KEYBOARDR1[i] = (char)i;

	for (int i=0; i<13; i++) {	// look ma no modulo
		ROT13[i+'A']=(char)(i+'N'); ROT13[i+'N']=(char)(i+'A');
		ROT13[i+'a']=(char)(i+'n'); ROT13[i+'n']=(char)(i+'a');
	}

	for (int i=0, trans=23; i<26; i++) { CAESAR[i+'A']=(char)((i+23)%26 + 'A'); CAESAR[i+'a']=(char)((i+23)%26 + 'a'); }

	for (int i=0+1; i<26; i++) { ALPHAL1[i+'A'] = (char)(i+'A'-1); ALPHAL1[i+'a'] = (char)(i+'a'-1); }
	ALPHAL1['A']='Z'; ALPHAL1['a']='z';

	for (int i=0; i<26-1; i++) { ALPHAR1[i+'A'] = (char)(i+'A'+1); ALPHAR1[i+'a'] = (char)(i+'a'+1); }
	ALPHAR1['Z']='A'; ALPHAR1['z']='a';

	for (int i=0; i<26; i++) {
		char ch = "snvfrghjokl;,mp[wtdyibecux".charAt(i);
		KEYBOARDR1[Character.toUpperCase(ch)] = (char)(i+'A');
		KEYBOARDR1[ch] = (char)(i+'a');
	}
  }

	static String[] e2p = { // hashtable if get bigger
		"be","B", "oh","O", "you","U","You","U",
		"for","4", "to","2","too","2","two","2","To","2","Too","2","Two","2",
		"new","nu","New","Nu",
	};

  static class GraphicsCypher extends GraphicsWrap {
	public char[] map = null;

	GraphicsCypher(Graphics2D g) { super(g); }
	public Graphics create() { return new GraphicsCypher((Graphics2D)wrapped_.create()); }

	public void drawString(String str, int x, int y) {
		int strlen = str.length();
		if (str==null || strlen==0) return;

		StringBuffer sb = null;
		if (map==REVERSE) { // not really a cypher, is it?
			if (strlen > 1) {
				sb = new StringBuffer(str);
				sb.reverse();
				//for (int i=strlen-1; i>=0; i--) sb.append(str.charAt(i));
				str = sb.toString();
			}

		} else if (map==PIGLATIN) { // neither is this
			str = phelps.lang.Strings.fromPigLatin(str);

		} else if (map==PRINCE) {   // nor this
			for (int i=0,imax=e2p.length; i<imax; i+=2) {
				if (e2p[i].equals(str)) { str=e2p[i+1]; break; }
			}

		} else {    // substitutions
			sb = new StringBuffer(str);
			final int maplen=map.length;
			for (int i=0,imax=str.length(); i<imax; i++) {
				char ch=sb.charAt(i);
				if (ch < maplen) sb.setCharAt(i, map[(int)ch]);
			}
			str = sb.toString();
		}
		//wrapped_.setFont(...); // make the font smaller so works well with different letter widths?
		wrapped_.drawString(str, x,y);
	}
	public void drawChars(char data[], int offset, int length, int x, int y) { try { drawString(new String(data,offset,length), x,y); } catch (StringIndexOutOfBoundsException e) {}; }
	public void drawBytes(byte data[], int offset, int length, int x, int y) { try { drawBytes(/*new String(*/data,offset,length, x,y); } catch (StringIndexOutOfBoundsException e) {}; }
  }


  String msg_ = imsg_;	// set in restore()
  char[] map_ = imap_;
  GraphicsCypher gcy_ = null;
  VRadiogroup rg_ = new VRadiogroup();

  /** Wraps Graphics2D with GraphicsCypher. */
  public boolean paintBefore(Context cx, Node node) {
	Graphics2D g = cx.g;
	gcy_ = new GraphicsCypher((Graphics2D)g.create());	// efficient enough, though create many as drag about the screen
	gcy_.map = map_;
/*
	gcy_.map = ROT13;
	for (int i=0,imax=MSGS.length; i<imax; i++) {
		if (MSGS[i].equals(msg_)) {
			gcy_.map=MAPS[i];
			win_.setTitle(TITLES[i]);
			break;
		}
	}
*/
	cx.g = gcy_;

	return super.paintBefore(cx, node);
  }

  /** Restores Graphics2D passed in paintBefore. */
  public boolean paintAfter(Context cx, Node node) {
	if (gcy_!=null) {
		gcy_.dispose();
		gcy_=null;
	}
/* old implementation
	//boolean ret = super.paintAfter(g, cx, node);
	Browser br = getBrowser();

	Graphics2D g = cx.g;
	Graphics2D g2 = new GraphicsCypher((Graphics2D)g.create());
	g2.clip(getContentBounds());
	br.getRoot().paintBeforeAfter(g2.getClipBounds(), cx);
//System.out.println("entry clip="+g.getClipRect()+", content bounds="+getContentBounds()+", intersection = "+g2.getClipBounds());
	g2.dispose();
	*/

	// lose VFrame's resize nib, and lose border too but Lens always draws it
	return super.paintAfter(cx, node);
	//return ret;
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==win_) {
		Browser br = getBrowser();
		INode menu = (INode)se.getOut();
		rg_.clear();
		for (int i=0,imax=MSGS.length; i<imax; i++) {
			VRadiobox rb = (VRadiobox)createUI("radiobox", TITLES[i], new SemanticEvent(br, MSGS[i], win_, this, null), menu, "VIEW", false);
			rb.setRadiogroup(rg_);
			rb.setState(MSGS[i]==msg_);
		}
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (se.getIn()==this) {
		for (int i=0,imax=MSGS.length; i<imax; i++) {
			if (MSGS[i]==msg) {
				if (msg_!=msg) {
					msg_ = imsg_ = msg; getBrowser().repaint();
					map_ = imap_ = MAPS[i];
					win_.setTitle(TITLES[i]);
				}
				break;
			}
		}
	}
	return super.semanticEventAfter(se, msg);
  }
}
