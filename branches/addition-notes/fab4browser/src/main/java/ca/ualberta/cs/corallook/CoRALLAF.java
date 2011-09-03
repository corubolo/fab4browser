package ca.ualberta.cs.corallook;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.plaf.basic.BasicLookAndFeel;

public class CoRALLAF {

	private static final String releaseFolder = "samtest";
	static final String coralhome = "http://hypatia.cs.ualberta.ca/CoRAL/"+releaseFolder+"/";
	static final String profilePrefix = "http://hypatia.cs.ualberta.ca/CoRAL/"+releaseFolder+"/userprofile.php?username=";//"http://hypatia.cs.ualberta.ca/CoRAL/"+releaseFolder+"/person_view.php?id=";
	static final String paperpagePrefix = "http://hypatia.cs.ualberta.ca/CoRAL/"+releaseFolder+"/paperpage.php?url=";
	
	public static void openCoRALHome(JFrame parent){
		openUrl(coralhome, parent);
	}
	
	public static void openUserProfile(String username, JFrame parent){
		openUrl(profilePrefix+username, parent);
	}
	
	public static void openPaperView(String url, JFrame parent){
		openUrl(paperpagePrefix+url, parent);
	}
	
	static void openUrl(String url, JFrame parent){
		try {
			//if windows
			Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " +url);
		} catch (IOException e1) {
			//mac
			String[] commandLine = { "netscape", url };
			try {
				Runtime.getRuntime().exec(commandLine);
			} catch (IOException e2) {
				//unix
				try
                {
					Process p = Runtime.getRuntime().exec("netscape -remote openURL("+url+")");
                
                    // wait for exit code -- if it's 0, command worked,
                    // otherwise we need to start the browser up.
                    int exitCode = p.waitFor();
                    if (exitCode != 0)
                    {
                        // Command failed, start up the browser
                        // cmd = 'netscape http://www.javaworld.com'		                        
                        p = Runtime.getRuntime().exec("netscape "+url);
                    }
                }
                catch(Exception x)
                {
                	JOptionPane.showMessageDialog(parent, 
							"<html><body>CoRAL couldn't open your default browser. Please try opening this address in your browser:<br/>"
							+url+"</body></html>", "Problem opening internet browser", JOptionPane.INFORMATION_MESSAGE);
                }
			}					
		}
			
	}
	
	public void changeInt(test a){
		a.a++;
	}
	
	public static void main(String[] args) {
		CoRALLAF laf = new CoRALLAF();
		final test t = new test();
		laf.changeInt(t);
		System.out.println(t.a);
		laf.changeInt(t);
		System.out.println(t.a);
		
	}
}

class test{
	int a=2;
	
}
