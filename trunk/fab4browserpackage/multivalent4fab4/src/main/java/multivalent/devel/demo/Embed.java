package multivalent.devel.demo;

import java.awt.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import multivalent.*;
import multivalent.std.ui.ForwardBack;
import multivalent.gui.VScrollbar;



/**
	Source code demonstrates how to embed the browser into other applications, at different levels.
	Ways of embedding:
	{@linkplain #external() external process},
	{@linkplain #local() local JVM},
	{@linkplain #swing() Swing component},
	document tree data structure as described in {@link MediaAdaptor}.
	(NOT as an applet.) <!-- too many hassles: reading from local disk for PDF, up-to-date JVM, shaky control by enclosing browser -->

	Applications should add the <tt>Multivalent.jar</tt> JAR to the CLASSPATH,
	rather than try to figure out what subset of the classes are needed,
	so that updating to new versions of Multivalent is easy (the JAR is relatively small anyhow).

	<p>This class can be run to see the code working, but it's mainly useful
	for the demonstrations in source code.

	@version $Revision: 1.4 $ $Date: 2005/01/03 08:56:32 $
*/
public final class Embed {
  static URI DEMO_URI = URI.create("http://multivalent.sourceforge.net/");


  /**
	Launches a browser from as an independent process from a non-Java program,
	and sends the browser commands over a socket.
	Commands are limited to those semantic events that can be represented as strings,
	but this is sufficient to show documents of any type and go from page to page.
	Obviously this shows Java code that should be translated into the equivalent in another language.

	@see multivalent.net.RemoteControl
  */
  public/*for Javadoc*/ static void external() throws IOException {
	// 1. launch Multivalent process
	Runtime.getRuntime().exec("java -classpath DVI.jar -jar Multivalent.jar");

	// 2. wait for startup
	// ...

	// 3. send commands over socket (send by web browser or programming language)
	socketCmd(Document.MSG_OPEN, "string-arg");
	// send another command ...
  }

  static void socketCmd(String cmd, String arg) {
	try {
		String surl = "http://localhost:5549/" + cmd;
		if (arg!=null) surl += "?" + arg;
		InputStream in = new URL(surl).openStream();
		while (in.read()!=-1) {}    // gobble up response, which should be "OK"
		in.close();

	} catch (Exception e) {
		System.err.println("FAIL: "+e);
	}
  }



  /**
	Creates a new browser window within the same JVM,
	and communicates with semantic events on the Java event queue.
	The full set of semantic events are available.
  */
  public static void local() {
	Multivalent v = Multivalent.getInstance();
	Browser br = v.getBrowser("browser-instance-name");

	// talk to it via semantic events
	br.eventq(SystemEvents.MSG_GO_HOME, null);
	br.eventq(Document.MSG_OPEN, "file:/usr/product/help/UserGuide.pdf");
	br.eventq(Document.MSG_OPEN, DEMO_URI);


	// talk to browser of different name (created if doesn't exist)
	/*
	Browser br2 = v.getBrowser("other-name");
	br2.eventq(Document.MSG_OPEN, "http://...");
	*/
  }


  /**
	Creates a browser window as part of a Swing-based application,
	and uses a different hub document to do without menubar and toolbar.
   */
  public static void swing() {
	// 1. make browser JPanel
	Multivalent v = Multivalent.getInstance();
	final Browser br = v.getBrowser("name", "Basic", false);


	// 2. pack in another Swing-based application
	JFrame frame = new JFrame("my app");
	final JFileChooser jfc = new javax.swing.JFileChooser();
	frame.setBounds(100,50, 500,350);

	Container c = frame.getContentPane();
	c.setLayout(new BorderLayout());
	c.add(br, BorderLayout.CENTER);


	// buttons
	JPanel p = new JPanel(new FlowLayout());
	JButton b = new JButton("back");
	b.addActionListener(new SemanticSender(br, ForwardBack.MSG_BACKWARD, null));
	p.add(b);

	b = new JButton("forward");
	b.addActionListener(new SemanticSender(br, ForwardBack.MSG_FORWARD, null));
	p.add(b);

	// for paginated documents
	b = new JButton("page back");
	b.addActionListener(new SemanticSender(br, multivalent.std.ui.Multipage.MSG_PREVPAGE, null));
	p.add(b);

	b = new JButton("page next");
	b.addActionListener(new SemanticSender(br, multivalent.std.ui.Multipage.MSG_NEXTPAGE, null));
	p.add(b);

	// see multivalent.std.ui.Multipage for more semantic events (like MSG_GOPAGE)

	b = new JButton("open");
	b.addActionListener( new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
		if (jfc.showOpenDialog(br) == JFileChooser.APPROVE_OPTION) {
			br.eventq(Document.MSG_OPEN, jfc.getSelectedFile().toURI());
		}
	  }
	});
	p.add(b);

	b = new JButton("exit");
	b.addActionListener( new ActionListener() {
	  public void actionPerformed(ActionEvent e) { System.exit(0); }
	});
	p.add(b);
	c.add(p, BorderLayout.NORTH);

	frame.pack();
	frame.setVisible(true);


	// 3. show initial page (optional) via a semantic event
	br.eventq(Document.MSG_OPEN, DEMO_URI);


	// Appendix A: handle scrollbars in Swing
	// turn off internal scrollbars
	INode root = br.getRoot();
	Document doc = (Document)root.findBFS("content");
	doc.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
	// then after loading new document, determine page dimensions from doc.bbox and set Swing scrollbars accordingly

  }


  static class SemanticSender implements ActionListener {
	Browser br_;
	String cmd_;
	Object arg_;

	SemanticSender(Browser br, String cmd, Object arg) { br_=br; cmd_=cmd; arg_=arg; }
	public void actionPerformed(ActionEvent e) { br_.eventq(cmd_, arg_); }
  }



  /** Demos embedding in Swing-based application. */
  public static void main(String[] argv) {
	//local();
	swing();
  }
}
