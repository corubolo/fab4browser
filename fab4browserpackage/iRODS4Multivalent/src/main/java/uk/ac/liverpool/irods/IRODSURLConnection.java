/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.irods;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.irods.IRODSAccount;
import edu.sdsc.grid.io.irods.IRODSException;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.local.LocalFileInputStream;

/**
 * The main class, managing IRODS URL connections. Supports browsing of iRODS folders, and user authentication.
 * 
 * @author fabio
 * 
 */
public class IRODSURLConnection extends URLConnection {

	public class UserInfo {
		String user; String pass;
	}

	static Hashtable<String, UserInfo> credentials = new Hashtable<String, UserInfo>();

	protected LocalFile local;

	protected GeneralFile remote;

	private URI uri;

	/**
	 * @param url
	 */
	protected IRODSURLConnection(URL url) {
		super(url);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.URLConnection#connect()
	 */

	@Override
	public void connect() throws IOException {
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * @throws IOException
	 */
	public GeneralFile getIRODSFile() throws IOException {
		if (remote!=null)
			return remote;
		connect();
		return remote = FileFactory.newFile( uri );

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public int getContentLength() {
		try {
			return (int) getIRODSFile().length();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	@Override
	public String getContentType() {
		try {
			if (getIRODSFile().isDirectory())
				return "text/html";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


	@Override
	public synchronized InputStream getInputStream() throws IOException{
		if (local!=null)
			return new LocalFileInputStream(local);
		connect();
		GeneralFile f = null;

		if (!uri.getScheme().equals("irods"))
			throw new IOException("Wrong URI scheme");

		String userInfo = uri.getUserInfo();
		//String userName = null, password = "";
		int index = -1;
		String un = null;
		//IRODSFileSystem fs = null;
		UserInfo u = null;

		// No user info in the URI
		if (userInfo == null || userInfo.equals("")) {
			IRODSAccount a = null;

			// try local account
			try {
				a = new IRODSAccount();

				if (a.getHost().equals(uri.getHost()) && (uri.getPort()==-1 || uri.getPort() == a.getPort()))
					un = a.getUserName();

			} catch (FileNotFoundException x){
				System.out.println("No local .irods user info");
			}
			// if local account is not right, ask the user
			//			if (userName != null) {
			// we need username and password
			u= askUsernameAndPass(un);

			//			}
		} else {
			index = userInfo.indexOf(":");
			// !password is provided
			if (index < 0) {

				index = userInfo.indexOf(".");
				if (index >= 0)
					un = userInfo.substring(0, index);
				else
					un = userInfo;
				u = askUsernameAndPass(un);

			}

		}

		if (f==null)
			try {
				if (u!=null ){
					userInfo = u.user +":" + u.pass;
					URI uri2 = new URI( uri.getScheme(), userInfo, uri.getHost(),
							uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment() );
					f = FileFactory.newFile(uri2);
					System.out.println(uri2);
				} else {
					f = FileFactory.newFile(uri);
					System.out.println(uri);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IRODSException e) {
				String hp = uri.getHost() + uri.getPort();
				IRODSURLConnection.credentials.remove(hp);
				String ms =  e.getMessage();
				if (e.getType() < -826000 && e.getType() >  -832000)
					ms = "Invalid credentials :" + e.getType();
				throw new IOException("Failed to connect to IRODS, reason: "+ ms, e);
			}

			remote = f;

			if (!remote.exists())
				throw new IOException("File not found");
			File temp = File.createTempFile("irodscon", ".rod");
			temp.deleteOnExit();
			if (remote.isDirectory())
				return htmlListing();
			local = new LocalFile(temp);
			remote.copyTo(local, true);
			local.deleteOnExit();
			return new LocalFileInputStream(local);

	}


	@Override
	public boolean getAllowUserInteraction() {

		return true;
	}
	private UserInfo askUsernameAndPass(String string) {
		String hp = uri.getHost() + uri.getPort();
		UserInfo ui = IRODSURLConnection.credentials.get(hp);
		if (ui == null){
			ui= getPasswordAuthentication(string);
			IRODSURLConnection.credentials.put(hp,ui);
		}
		return ui;
	}
	protected UserInfo getPasswordAuthentication(String s) {
		if (Frame.getFrames().length == 0)
			return null;
		final Dialog jd = new Dialog (Frame.getFrames()[0], "Enter IRODS credentials for host: "+ uri.getHost(), true);
		jd.setLayout (new GridLayout (0, 1));
		Label jl = new Label ("iRODS credentials for :" +uri.getHost());
		jd.add (jl);
		TextField username = new TextField(s);
		username.setBackground (Color.lightGray);
		jd.add (username);
		TextField password = new TextField();
		password.setEchoChar ('*');
		password.setBackground (Color.lightGray);
		password.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n')
					jd.dispose();
			}
		});
		jd.add (password);
		Button jb = new Button ("OK");
		jd.add (jb);
		jb.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				jd.dispose();
			}
		});
		jd.pack();

		jd.setVisible(true);
		UserInfo ui = new UserInfo();
		ui.user = new String(username.getText());
		ui.pass = new String(password.getText());
		return ui;
	}


	private InputStream htmlListing() {
		StringBuilder b = new StringBuilder();

		b.append("<html><head><style type=\"text/css\">\n" +
				"body {\n" +
				"	color: black; background-color: white;\n" +
				"	font-size: 14pts;	/* Mozilla: 16 for proportional, 13 for fixed */\n" +
				"	padding: 10px;}\n" +
				"\n" +
				"a:link { color: blue; }\n" +
				"a:visited { color: magenta; }\n" +
				"a:hover { color: red; }\n" +
				"a:active { color: red; }\n" +
				"\n" +
				"a:link, a:visited, \n" +
				"a:active, a:hover {\n" +
				"	text-decoration: underline;\n" +
				"}\n" +
				"\n" +
				"p {\n" +
				"	margin-top: 10px;\n" +
				"}\n" +
				"text { padding: 5px; }\n" +
				"\n" +
				"pre { font-family: monospace; }\n" +
				"\n\n" +
				"h1 { font-size: 24pt; font-weight: bold; margin: 10px 0px; }\n" +
				"h2 { font-size: 18pt; font-weight: bold; margin: 9px 0px; }\n" +
				"h3 { font-size: 14pt; font-weight: bold; margin: 7px 0px; }\n" +
				"h4 { font-size: 12pt; font-weight: bold; margin: 6px 0px; }\n" +
				"h5 { font-size: 10pt; font-weight: bold; margin: 5px 0px; }\n" +
				"h6 { font-size:  9pt; font-weight: bold; margin: 5px 0px; }\n" +
				"" +
				"" +
		"</style>");
		b.append("<title>").append("Contents of the iRODS folder:").append(uri.getPath()).append("</title>");

		b.append("\t<base href='").append(uri).append("/'>\n");
		b.append("</head>\n");
		b.append("<body>\n");
		b.append("<h2>").append("Contents of the iRODS folder:").append(uri.getPath()).append("</h2>");
		b.append("\n<pre><table width='90%'>\n");

		// headers.  click to sort
		b.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'  title='Sort table'>");
		b.append("<th align='left'>File / Directory" +
				"<th align='right'>Size" +
		"<th align='right'>Last Modified</span>\n");

		GeneralFile[] contents = remote.listFiles();
		for (GeneralFile f:contents)
			if (f.isDirectory()){
				b.append("<tr>");
				b.append("<td><a href='").append(f.getName().replaceAll(" ", "%20")).append("'>").append(f.getName()).append("</a>");
				b.append("<td align='right'><span Behavior='ElideSpan'>0</span> --<td align='right'><span Behavior='ElideSpan'>0</span> --");
			}
			else {
				b.append("<tr>");
				String fname = f.getName();
				b.append("<td><a href='").append(fname.replaceAll(" ", "%20")).append("'>").append(fname).append("</a>");
				DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				long size = 0;
				Date last = new Date();
				size = f.length();
				last = new Date(f.lastModified());

				b.append("<td align='right'>").append(Long.toString(size)).append("<td align='right'>").append(outdfm.format(last));

			}

		b.append("</table>\n</body></html>\n");
		try {
			return new ByteArrayInputStream(b.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		}
		return null;
	}




}
