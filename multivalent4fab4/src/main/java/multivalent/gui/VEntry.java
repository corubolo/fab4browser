package multivalent.gui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;

import multivalent.*;
import multivalent.node.IHBox;
import multivalent.node.LeafUnicode;

import multivalent.std.VScript;



/**
	Single-line editable text field.
	Like {@link VTextArea}, except typing Return invokes SCRIPT attribute, if any.

	@version $Revision: 1.2 $ $Date: 2002/01/27 02:01:06 $
*/
public class VEntry extends VTextArea /*implements EventListener*/ {
  //protected int xoff_=0;

  public VEntry(String name,Map<String,Object> attr, INode parent, int widthchars, String initcontent) {
	//super(name,attr, parent, widthchars, 1, initcontent);
	super(name,attr, parent, new IHBox("layoutE"/*null*/,null,null));
	setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
	setWidthChars(widthchars);
	setContent(initcontent);
//	System.out.println("name = "+name);
  }

  public VEntry(String name,Map<String,Object> attr, INode parent) { this(name,attr, parent, 20, null); }

  public void setContent(String text) {
	//INode p = (INode)childAt(0);
	//p.removeAllChildren();
	//new LeafUnicode((text!=null? text: ""),null, this);	// should be insert()
	layout_.removeAllChildren();
	new LeafUnicode(text!=null? text: "", null, layout_);
	//new LeafUnicode("XXX", null, layout_);
	//if (text!=null) new LeafUnicode(text,null, this); //else removeAllChildren();
	//System.out.println("result of setContent"); dump();
	repaint(100);  // show new file
  }

  public int getWidthChars() { return wchars_; }

  public void setWidthChars(int widthchars) {
	assert widthchars>=0: widthchars;
	super.setSizeChars(widthchars,1);
  }


  /** Typing return invokes SCRIPT attribute, if any. */
  public boolean eventNode(AWTEvent e, Point rel) {
	int eid = e.getID();
//if (KeyEvent.KEY_FIRST<=eid && eid<=KeyEvent.KEY_LAST) System.out.println("typed in Entry: "+((KeyEvent)e).getKeyCode()+" vs "+KeyEvent.VK_ENTER);
//if (e instanceof KeyEvent) System.out.println("key "+((KeyEvent)e).getKeyCode()+" vs "+KeyEvent.VK_ENTER);
	if (KeyEvent.KEY_FIRST<=eid && eid<=KeyEvent.KEY_LAST && ((KeyEvent)e).getKeyCode()==KeyEvent.VK_ENTER) {
		// if only entry field, submit; else ignore
		KeyEvent ke = (KeyEvent)e;
//System.out.println("VEntry eats");
		ke.setKeyCode(0); ke.setKeyChar('\0');
		// <-- and --> should remain within box
//System.out.println("invoke "+getAttr(ATTR_SCRIPT));

		Browser br = getBrowser();
		br.getCursorMark().move(null);
		br.getSelectionSpan().move(null);

		invoke();
		return true;
	}

	return super.eventNode(e, rel);
  }

  /**
	Before executing the script, the Entry's content is collected as the TEXT entry to this widget,
	where it can be referred to by the script as "$TEXT".
  */
  public void invoke() {
	putAttr("text", getContent());
	VScript.eval(getValue("script"), getDocument(), getAttributes(), this);
  }
}
