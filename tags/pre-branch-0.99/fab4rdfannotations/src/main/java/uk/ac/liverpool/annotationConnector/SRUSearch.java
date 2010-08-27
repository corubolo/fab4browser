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


package uk.ac.liverpool.annotationConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class SRUSearch {

	public static Namespace ns_diag = Namespace.getNamespace("diag",
			"http://www.loc.gov/zing/srw/diagnostic/");
	public static Namespace ns_sru = Namespace.getNamespace("sru",
			"http://www.loc.gov/zing/srw/");

	
	public static int  defaultNumberOfRecords = 30;
	public static class Response {

		public Diagnostic diag = null;
		public Integer numberOfRecords;
		String[] data;
		public int httpResCode;
		public String httpMessage;

	}

	public static class Diagnostic {

		public String uri;
		public String details;
		public String message;

	}

	static String explain(String address) throws IOException {
		URL u = new URL(address);
		URLConnection c = u.openConnection();
		if (c instanceof HttpURLConnection) {
			HttpURLConnection cc = (HttpURLConnection) c;
			int res = cc.getResponseCode();
			//System.out.println(res);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(c
				.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String s;
		while ((s = br.readLine()) != null)
			sb.append(s).append('\n');
		return sb.toString();
	}

	public static void main(String[] args) throws IOException, JDOMException {
		Response r = search("http://nara.cheshire3.org/services/test",
				"c3.idx-digest any \"RcEBFZbWHBuRAsxdVzSiYQ==\"");
		//System.out.println(r.numberOfRecords);
		if (r.numberOfRecords > 0)
			System.out.println(r.data[0]);
	}

	static Response search(String address, String query) {
		try {
			return search(address, query, defaultNumberOfRecords, null, 0);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static Response search(String address, String query, int maxRecords,
			String schema, int startRecord) throws JDOMException, IOException {

		Response rs = new Response();
		String q = address + "?operation=searchRetrieve&version=1.2";
		if (maxRecords != 0)
			q += "&maximumRecords=" + maxRecords;
		if (startRecord != 0)
			q += "&startRecord=" + startRecord;
		if (schema != null)
			q += "&recordSchema=" + schema;
		q = q + "&query=" + URLEncoder.encode(query, "UTF-8");
		System.out.println(q);
		URL u = new URL(q);
		URLConnection c = u.openConnection();
		if (c instanceof HttpURLConnection) {
			HttpURLConnection cc = (HttpURLConnection) c;
			rs.httpResCode = cc.getResponseCode();
			rs.httpMessage = cc.getResponseMessage();
		}

		SAXBuilder builder = new SAXBuilder();
		Document d = builder.build(c.getInputStream());
		Element r = d.getRootElement();
		Element diag = r.getChild("diagnostics", ns_diag);

		if (diag != null) {
			rs.diag = new Diagnostic();
			rs.diag.uri = diag.getChildTextTrim("uri", ns_diag);
			rs.diag.message = diag.getChildTextTrim("message", ns_diag);
			rs.diag.details = diag.getChildTextTrim("details", ns_diag);
			rs.numberOfRecords = -1;
			System.out.println(rs.diag.message);
			return rs;
		}
		String t = r.getChildTextTrim("numberOfRecords", ns_sru);
		if (t != null) {
			rs.numberOfRecords = Integer.decode(t);
		}
		//System.out.println(t);
		if (rs.numberOfRecords == null) return rs;
		
		if (rs.numberOfRecords == 0)
			return rs;
		Element mr = r.getChild("records", ns_sru);
		List<Element> recs = mr.getChildren("record", ns_sru);
		rs.data = new String[recs.size()];
		int i = 0;
		XMLOutputter o = new XMLOutputter();

		for (Element record : recs) {
			Element data = record.getChild("recordData", ns_sru);

			rs.data[i++] = o.outputString((Element)data.getChildren().get(0));;
			//System.out.println(rs.data[i-1]);
		}
		System.out.println("rec: "+ rs.numberOfRecords);
		return rs;
		// return sb.toString();
	}
}
