package multivalent;	// want default package so simple no import, but doesn't work in practice

public class Meta {
	/* * * CHANGE THIS BEFORE A RELEASE * * */
	public static final boolean DEVEL = !true;
	/* * * END CHANGE * * */

	/** General monitoring flag.  <code>true</code> while developing, <code>false</code> when compile for distribution to users. */
	public static final boolean MONITOR = true && Meta.DEVEL;


	private static String lastmsg = null;
	public static void sampledata(String msg) {
		if (Meta.DEVEL && msg != Meta.lastmsg) { System.err.println("SAMPLE DATA: "+msg); Meta.lastmsg = msg; }
	}
	public static void unsupported(String msg) {
		if (Meta.DEVEL && msg != Meta.lastmsg) { System.err.println("UNSUPPORTED: "+msg); Meta.lastmsg = msg; }
	}

}
