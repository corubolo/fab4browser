package phelps.lang;

import java.util.Arrays;
import java.io.File;

import phelps.net.URIs;



/**
	Extensions to {@link java.lang.Class}.

	<ul>
	<li>convenience: getClass().getName() &rArr; tail / root 
	<li>{@link #getRootDir(Class)}
	</ul>

	@version $Revision: 1.1 $ $Date: 2005/01/11 17:13:31 $
*/
public class Classes {
  public static String getTail(Class cl) {
	if (cl==null) return null;
	return getTail(cl.getName());
  }

  public static String getTail(String s) {
	if (s==null) return null;
	int inx = s.lastIndexOf('.'); if (inx!=-1) s = s.substring(inx+1);
	return s;
  }

  /**
	Returns file directory containing JAR or classes,
	or <code>null</code> if that part of CLASSPATH is not a {@link java.io.File}.
  */
  public static File getRootDir(Class cl) {
	assert cl!=null;
	String top;

	String name = "/" + cl.getName().replace('.', '/') + ".class";
	String cp = URIs.decode/*in case space in path*/(cl.getResource(name).toString());
	//String path = cl.getResource(name).getPath();
//System.out.println("Bootstrap res = "+cp);
	if (cp.startsWith("jar:")) {	// e.g., "jar:file:/C:/temp/Multivalent20011127.jar!/multivalent/Multivalent.class"
		cp = cp.substring("jar:file:".length(), cp.indexOf('!'));
		top = cp.substring(0, cp.lastIndexOf('/')+1);

	} else if (cp.startsWith("file:")) {	// e.g., "file:/D:/prj/Multivalent/www/jar/multivalent/Multivalent.class"
		cp = cp.substring("file:".length(), cp.length() - name.length());
//System.out.println("cp = "+cp);
		top = cp;

	} else {	// e.g., bundleresource://6/multivalent/Multivalent.class
		//getLogger().warning("unfamiliar protocol: "+cp);
		top = null;	// nothing additional beyond CLASSPATH
	}

	return top!=null? new File(top): null;
  }
}
