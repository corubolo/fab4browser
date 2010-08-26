package multivalent.net;

import java.util.Map;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;

import multivalent.*;



/**
	Accept semantic events via socket listening on port.
	Special case: <tt>http://localhost:<var>port</var>/<var>command</var>?<var>argument</var></tt>.
	where the default <var>port</var> is 5549 and <var>command</var> is a semantic event string name.
	For example, <code>http://localhost:5549/openDocument?http://www.cs.berkeley.edu/</code>
	sent from a web browser or programming language instructs the browser to open a well-known web page.
	A list of semantic events and their string equivalents is available in
	the <a href='../constant-values.html'>Javadoc constants</a> page (look for <code>MSG_*</code>).
	At this time only semantic events with zero or one-String argument can be invoked this way.

	@version $Revision: 1.2 $ $Date: 2003/02/12 22:51:35 $
*/
public class RemoteControl extends Behavior implements Runnable {
  static final boolean DEBUG = false;

  /** Attribute to pass port number. */
  public static final String ATTR_PORT = "port";

  public static final int PORT_DEFAULT = 5549;


  private static final String EOL = "\r\n";     // HTTP/1.1


  int port_;
  /** Sends semantic events to this browser. */
  //Browser br_;  // => name of remote control browser Multivalent.getBrowser("name") or Multivalet.getBrowser(0)
  static Thread thread_ = null;
  volatile boolean stop_ = false;


  /** Listens on socket, send semantic event to browser.... */
  public void run() {
	ServerSocket server = null;
	try {
		server = new ServerSocket(port_); //System.out.println("listening on port "+port_);
		while (!stop_) {
			Socket client = server.accept();

			// for security, request must be from local machine, for now
			InetAddress from = client.getLocalAddress();
			if (!from.isLoopbackAddress()) {
//System.out.println("from="+from);
				try { client.close(); } catch (Exception ignore) {}
				continue;
			}

			//System.out.println("ACK "+client.getLocalAddress());    //127.0.0.1
			// first line: request method, URI, and protocol version
			// next lines: <header>: <value>
			// blank line
			// possible body
			BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
			String line;
			while ((line=reader.readLine())!=null && line.length()>0) {
				System.out.println("read: |"+line+"|");
				// execute some command or something
				if (line.startsWith("GET")) command(line.substring("GET".length()).trim());
			}

			// send HTTP goodbye
//System.out.println("client outputstream");
			OutputStream out = client.getOutputStream();
			//assert msg.length < 2*1024;   // not writing enough to need BufferedWriter
//System.out.println("got client outputstream");
			String body = "ok";          // something for browsers to display
			String[] msg = {
				// first line: status line, including the message's protocol version and a success or error code,
				"HTTP/1.0 200 OK",

				// headers: <header>: <value> containing server information, entity  metainformation,
				"Server: Multivalent/"+Multivalent.VERSION,
				//"Date: XXX"       // needs particular format
				"Content-Type: text/html",
				"Content-Length: "+body.length(),   // existence implies body

				// blank line
				"",

				// possible body
				body
			};
			for (int i=0,imax=msg.length; i<imax; i++) out.write(/*Strings.getBytes8(?*/(msg[i]+EOL).getBytes());

			//System.out.println("closing client");
			client.close();
		}
	} catch (IOException ioe) {
		System.out.println("I/O exception: "+ioe);
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println("interrupt? "+e);

	} finally {
		if (server!=null) try { server.close(); System.out.println("releasing port "+port_);  } catch (IOException bad) {}    // release port
	}

	// and as exit, kills the thread
	System.out.println("draining thread");
  }

  void command(String line) {
	assert line!=null;
	if (line.startsWith("/")) line=line.substring(1);
	int si = line.indexOf(' '); if (si!=-1) line=line.substring(0,si);
	int qi = line.indexOf('?'); if (qi==-1) qi=line.length();
	String cmd=line.substring(0,qi), arg=line.substring(qi+1);

if (DEBUG) System.out.println("sending semantic event "+cmd+", arg="+arg);
	Browser br = (Browser)Multivalent.getInstance().browsersIterator().next();
	br.eventq(cmd, arg);
  }


  /** When browser destroyed, release port.
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (Browser.MSG_CLOSE==msg) {
		// release port
System.out.println("interrupt thread");
		thread_.currentThread().interrupt();     // messy probably waiting on accept
	}
	return super.semanticEventAfter(se, msg);
  }*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	if (thread_==null) {
		super.restore(n,attr, layer);
		port_ = phelps.lang.Integers.parseInt(getAttr(ATTR_PORT), PORT_DEFAULT);
		//br_ = getBrowser();

		thread_ = new Thread(this);
		thread_.start();

		//layer.removeBehavior(this);
	} // ELSE IGNORE -- don't add to layer
  }
}
