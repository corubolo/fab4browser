/*
 * Created on 03-Nov-2005
 *         XMLma.java
 * 
 * Author: Fabio Corubolo
 * 		   f.corubolo@liv.ac.uk
 * 		   Copyright 2005 The University of Liverpool
 * 
 */
package uk.ac.liverpool.xmlma;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import multivalent.INode;
import multivalent.std.adaptor.HTML;

import com.pt.io.InputUni;
import com.pt.io.InputUniString;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * Simple XML media adaptor to display XML documents, using XSLT where defined
 * 
 */
public class XMLma extends HTML {

	/**
	 * 
	 */
	public XMLma() {
		super();
	}
	/* (non-Javadoc)
	 * @see multivalent.MediaAdaptor#parse(multivalent.INode)
	 */
	@Override
	public Object parse(INode parent) throws Exception {
		//Document doc = parent.getDocument();
		//StringBuffer outputBuffer = new StringBuffer(10000);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		InputUni uni = getInputUni();
		int bufferData;
		byte[] start2 = new byte[1000];
		byte[] start;
		BufferedInputStream bis = new BufferedInputStream(uni.getInputStreamRaw(),
				1000 + 1);
		bis.mark(1000 + 1);
		bufferData = bis.read(start2);
		bis.reset();
		int k;
		String ssloc = null;
		if (bufferData > 0) {
			start = new byte[bufferData];
			System.arraycopy(start2, 0, start, 0, bufferData);
			String s = new String(start2);

			if ((k = s.indexOf("<?xml-stylesheet ")) > 0) {
				int n;
				//                System.out.println(k);
				if ((n = k + 6 + s.substring(k).indexOf("href=\"")) > 0) {
					int r = n + s.substring(n).indexOf('"');
					//                    System.out.println("ww" + n + "ww" + r);
					ssloc = s.substring(n, r);
					//                    System.out.println(ssloc);
				}
			}
		}
		//bis.

		//        StreamSource xslt = new StreamSource(xsltURL);
		//        t = tf.newTransformer(xslt);
		StreamSource xml = new StreamSource(bis);
		StringWriter sw = new StringWriter();
		URI u = uni.getURI();
		if (ssloc == null) {
			BufferedReader r = new BufferedReader(new InputStreamReader(bis));
			String l = null;
			StringBuffer sb =new StringBuffer(1000);
			sb.append("<html><head><title>XML file, stylesheet information not present</title>"
					+"<style TYPE=\"text/css\"> body { color: black; background-color: white; } span#title { font-size: 16pt; } "
					+"span.subtitle { font-size: 12pt; font-style:}</style></head><body>"
					+"<p><center><span id='title'>XML file, style information not present.</span></center><br>"
					+"<br><span class='subtitle'><pre>"
			);
			while ((l=r.readLine())!=null) {
				l = l.replaceAll("&", "&amp;");
				l = l.replaceAll("<", "&lt;");
				l = l.replaceAll(">", "&gt;");
				sb.append(l+"\n");

			}
			sb.append("</pre></span></body></html>");
			r.close();
			InputUni iu = new InputUniString(sb.toString(), null,u,null);
			this.setInput(iu);
			return super.parse(parent);

		}
		//if (ssloc!=null) {
		final URI u2 = u.resolve(ssloc);
		//System.out.println("!"+u2);
		StreamSource xslt = new StreamSource(u2.toURL().openStream());
		tf.setURIResolver(new URIResolver() {
			public Source resolve(String href, String base) throws TransformerException {
				try {
					return new StreamSource(u2.resolve(href).toURL().openStream());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

		});
		t = tf.newTransformer(xslt);
		t.transform(xml, new StreamResult(sw));
		//System.out.println(sw);
		//parseHelper(sw.toString(), "HTML", getLayer(), doc);
		String txt = sw.toString();
		InputUni iu = new InputUniString(txt, null,u,null);
		//iu.s
		this.setInput(iu);

		return super.parse(parent);

	}

}
