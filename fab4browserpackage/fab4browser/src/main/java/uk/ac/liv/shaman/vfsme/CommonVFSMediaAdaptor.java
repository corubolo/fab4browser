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
package uk.ac.liv.shaman.vfsme;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import multivalent.DocInfo;
import multivalent.Document;
import multivalent.INode;
import multivalent.MediaAdaptor;
import multivalent.SemanticEvent;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

import uk.ac.liverpool.fab4.Fab4utils;


/**
 * This class handles the archive files in zip, tar, gzip, bz2, and iso formats using commons vfs.
 * 
 * @author fabio
 *
 */
public class CommonVFSMediaAdaptor extends MediaAdaptor {

	boolean building = false;

	@Override
	public Object parse(INode parent) throws Exception {
		String html = null;
		try {

			String pre = getExtension();

			// Case for archive files
			if (pre != null)

				html = toHTML(pre);
			else {
				// case for files embedded in archives
				String  s = getURI().toString();
				int pos = s.lastIndexOf('/');
				if (pos != -1){
					s = s.substring(pos);
					pos = s.lastIndexOf('.');
					if (pos != -1)
						s = s.substring(s.lastIndexOf('.'));
					else s=null;
				}
				else s = null;


				FileSystemManager m = VFS.getManager();
				FileObject fo = m.resolveFile(getURI().toString());
				// Workaround for loopy: loopy breaks when reading at the end of the input stram;
				// we stop at the lenght of the file to avoid an arrayindexoutofbound
				File f = Fab4utils.copyToTemp(fo.getContent().getInputStream(),
						"vfstemp", s, fo.getContent().getSize());
				return Fab4utils.parseHelper(f,getURI(), getDocument(),parent);

				//				FileInputStream is = new FileInputStream(f);
				//				String mime = null;
				//				String mime2 = null;
				//				byte[] bufferData = new byte[Math.max(1024, FileGuess.minBytes)];
				//				int a;
				//				MessageDigest md = null;
				//				try {
				//					md = MessageDigest.getInstance("MD5");
				//				} catch (NoSuchAlgorithmException e1) {
				//				}
				//
				//				while ((a = is.read(bufferData)) != -1) {
				//					if (mime2 == null && a >= FileGuess.minBytes) {
				//						mime2 = FileGuess
				//								.guess(bufferData, getURI().toString());
				//						if (mime2 == null)
				//							mime2 = "";
				//					}
				//					if (md != null)
				//						md.update(bufferData, 0, a);
				//				}
				//				is.close();
				//				is = new FileInputStream(f);
				//				InputUni iu = InputUni.getInstance(is, getURI(), getGlobal()
				//						.getCache());
				//				mime = iu.getContentType();
				//				byte[] digest = md.digest(); // 128 bit or 16 bytes
				//				MediaLoader.MD5Cache.put(getURI(), digest);
				//				MediaLoader.FileCache.put(getURI(), f);
				//				if (mime == null
				//						|| mime.equalsIgnoreCase("application/octet-stream")
				//						|| mime.equals("text/html")) {
				//					mime = mime2;
				//					iu.setContentType(mime);
				//				}
				//
				//				String genre = getGlobal().getGenre(mime, getURI().toString());
				//				System.out.println(genre + " " + mime);
				//				Layer dsl = getDocument().getLayers();
				//				Layer baseLayer = dsl.getInstance(Layer.BASE);
				//				MediaAdaptor helper = (MediaAdaptor) Behavior.getInstance(
				//						"helper", genre, null, baseLayer);
				//				helper.setInput(iu);
				//				float zoom = 1;
				//				try {
				//					zoom = Float.parseFloat(Multivalent.getInstance()
				//							.getPreference(genre + "-zoom", "1"));
				//				} catch (NumberFormatException nfe) {
				//				}
				//				helper.setZoom(zoom);
				//				dsl.getInstance(genre);
				//				// getDocument().setMediaAdaptor(helper);
				//
				//				Node root = null;
				//				try {
				//
				//					root = (Node) helper.parse(parent);
				//				} catch (Exception e) {
				//					new LeafUnicode("ERROR " + e, null, parent);
				//
				//					e.printStackTrace();
				//				} finally {
				//					try {
				//						helper.close();
				//					} catch (IOException ioe) {
				//					}
				//				}
				//				// getDocument().removeAttr(Document.ATTR_LOADING);
				//				return root;
			}
		} catch (Exception e) {
			e.printStackTrace();
			html = e.toString();
		}
		return parseHelper(html, "HTML", getLayer(), parent);
	}

	public String getExtension() throws FileSystemException{
		URI u = getURI();


		String fn = u.toString().toLowerCase().trim();
		String pre = null;
		if (fn.endsWith(".tar"))
			pre = "tar:";
		else if (fn.endsWith(".tar.gz" ) || fn.endsWith(".tgz"))
			pre = "tgz:";
		else if (fn.endsWith("iso" ) )
			pre = "iso:";
		else if (fn.endsWith(".tar.bz2" ) || fn.endsWith(".tbz2"))
			pre = "tbz2:";
		else if (fn.endsWith(".jar" ))
			pre = "jar:";
		else if (fn.endsWith(".zip" ))
			pre = "zip:";
		else if (fn.endsWith(".gz" ))
			pre = "gz:";
		else if (fn.endsWith(".bz2" ))
			pre = "bz2:";
		else {
		}
		return pre;

	}

	public String toHTML(String pre) throws IOException {
		URI u = getURI();
		FileSystemManager m = VFS.getManager();
		m.getSchemes();
		String su = u.toString();
		//		if (pre.equals("iso:"))
		//			su = su.replaceFirst("file:", "");
		String uri = pre + su + "!";

		System.out.println("VFS Open: " + uri);
		FileObject o = m.resolveFile(uri);
		StringBuilder sb = new StringBuilder(5000);
		sb.append("<html><head><style type=\"text/css\">\n" +
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
		sb.append("<title>").append("Contents of the archive").append(u.getPath()).append("</title>");

		sb.append("\t<base href='").append(u).append("!/'>\n");
		sb.append("</head>\n");
		sb.append("<body>\n");
		sb.append("<h2>").append("Contents of the archive").append(u.getPath()).append("</h2>");

		sb.append("\n<pre><table width='90%'>\n");

		// headers.  click to sort
		sb.append("<tr><span Behavior='ScriptSpan' script='event tableSort <node>'  title='Sort table'>");
		sb.append("<th align='left'>File / <b>Directory<th align='right'>Size<th align='right'>Last Modified</b></span>\n");


		processChild(o, m, sb);
		sb.append("</table>\n</body></html>\n");

		return sb.toString();
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (se.getMessage() == Document.MSG_OPENED){
			DocInfo di = (DocInfo) se.getArg();
			System.out.println(di);
		}
		return super.semanticEventAfter(se, msg);
	}

	private void processChild(FileObject f, FileSystemManager m, StringBuilder sb) throws FileSystemException, UnsupportedEncodingException {
		if (f.getType() == FileType.FOLDER || f.getType() == FileType.IMAGINARY) {
			sb.append("<tr>");
			sb.append("<td><b>").append(f.getName()).append("</b></td>");
			sb.append("<td align='right'><span Behavior='ElideSpan'>0</span> --<td align='right'><span Behavior='ElideSpan'>0</span> --");
			FileObject[] children = f.getChildren();

			for (FileObject subfile : children)
				processChild(subfile, m, sb);
		} else {
			sb.append("<tr>");
			FileName fname = f.getName();
			sb.append("<td><a href='").append(fname.getURI().replaceAll(" ", "%20")).append("'>").append(fname).append("</a>");
			DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long size = 0;
			Date last = new Date();
			try {
				size = f.getContent().getSize();
				last = new Date(f.getContent().getLastModifiedTime());
			}
			catch (Exception e) {
				// TODO: handle exception
			}
			sb.append("<td align='right'>").append(Long.toString(size)).append("<td align='right'>").append(outdfm.format(last));


		}
	}
}
