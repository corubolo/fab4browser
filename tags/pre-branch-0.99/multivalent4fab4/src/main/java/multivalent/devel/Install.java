package multivalent.devel;



/**
	UNDER DEVELOPMENT.
	Extensions to the Multivalent Browser should be packaged as JARs
	and executed to install by running subclasses of this class.

	Extension JARs should be downloaded into the same directory as <tt>Multivalent.jar<tt>.
	Then users can install and uninstall it by executing the JAR (<tt>java -jar XXX.jar install</tt>),
	which should run a subclass of this class to install hooks in hubs and Preferences,
	copy fonts, or whatever else is needed.

	So as to preserve the integrity of the system, installers and uninstallers should
	use the utility methods for the following functions:
	<ul>
	<li>add / remove / change / create (user) hubs
	<li>add / remove / change categories of Preferences: remap, set var, media adaptor
	</ul>

	<p>MANIFEST <tt>jar cmf <i>manifest</i> <i>XXX</i>.jar <i>classes</i>
	<pre>
	Class-Path: Multivalent.jar</tt>
	Main-Class: <i>name of subclass</i>
	</pre>

	If you write a media adaptor, typically you'll register it in Preferences.
	But its corresponding genre hub, if any, will likely be kept in the JAR.
	Only if you need to modify shared hubs will you write out a hub.

<!--
	456 + 20 = 516
DEBATE: executable or declarative?

Declarative:
easier to specify
single description for both install and uninstall

Executable:
can handle unusual situations, such as ... um ...
X user has to execute => system can do this -- same bookkeeping as for declarative

-->

	idempotent: repeat without harm - so store change actually done in some file
	LATER: system will track JARs and provide GUI to enabling and disenabling

	@version $Revision: 1.1 $ $Date: 2002/02/12 12:38:52 $
*/
public class Install {
  public static String USAGE = "Usage: java -jar XXX.jar (install|uninstall)";

  //public final readhub();


  void install(String[] argv) {}

  void uninstall(String[] argv) {}


  public static void error(String reason) {
	System.out.println(reason);
	System.exit(1);
  }

  public static void main(String[] argv) {
	if (argv.length==0) error(USAGE);

	Install e = new Install();
	String task = argv[0].toLowerCase();
	if ("install".equals(task)) e.install(argv);
	else if ("uninstall".equals(task)) e.uninstall(argv);
	else error(USAGE);
  }
}
