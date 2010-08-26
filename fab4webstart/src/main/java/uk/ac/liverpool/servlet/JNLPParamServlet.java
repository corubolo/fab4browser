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
package uk.ac.liverpool.servlet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class JNLPParamServlet extends HttpServlet implements Servlet {

	private static final String DEFAULT_DESTINATION = "http://bodoni.lib.liv.ac.uk/fab4/";

	String[] arguments = new String[] {
	//		"-aserver=http://nara.cheshire3.org/sword/annotations/",
	//		"-asearchserver=http://nara.cheshire3.org/services/annotations/",
	// "-single"
	};

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	protected void process(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// set the no caching parameters
		response.addHeader("Content-Type", "application/x-java-jnlp-file");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0L);
		SAXBuilder b = new SAXBuilder(false);
		Document d;
		// System.out.println(getServletContext().getContextPath());
		// System.out.println(getServletConfig().getServletContext().getResource("WEB-INF/fab4browser.jnlp"));
		try {
			d = b.build(new File(getServletContext().getRealPath("/"),
					"fab4browser.jnlp"));

			String destination = request.getQueryString();
			if ((destination == null) || (destination.length() == 0))
				destination = DEFAULT_DESTINATION;

			String decodedDestination = URLDecoder.decode(destination, "UTF-8");

			String requestURI = request.getRequestURL().toString();
			String codebase = requestURI.substring(0, requestURI.lastIndexOf('/'));

			// set codebase
			((Attribute) XPath.selectSingleNode(d, "/jnlp/@codebase"))
					.setValue(codebase);
			// set the href
			((Attribute) XPath.selectSingleNode(d, "/jnlp/@href"))
					.setValue("fab4webstart.jnlp?" + destination);

			// set the destination
			Element appdesc = (Element) XPath.selectSingleNode(d,
					"/jnlp/application-desc");
			Element argument = new Element("argument")
					.addContent(decodedDestination);
			// add the main argument: the destination URI
			appdesc.addContent(argument);

			// add the remaining arguments
			for (String s : arguments) {
				argument = new Element("argument").addContent(s);
				appdesc.addContent(argument);
			}
			Element resources = (Element) XPath.selectSingleNode(d,
					"/jnlp/resources");
			// add all the resources (Jar files) from the jars folder
			File f = new File(getServletContext().getRealPath("/jars/"));
			for (File s : f.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					if (pathname.isFile()
							&& pathname.getName().endsWith(".jar")
							// we avoid the fab4-start.jar file, since it does not include any code, just the mainfest file pointing to the libraries 
							// this seems to break webstart
							&& !pathname.getName().contains("fab4-start.jar"))
						return true;
					return false;
				}
				})) {
				argument = new Element("jar").setAttribute("href", "jars/"
						+ s.getName());
				resources.addContent(argument);
			}
			XMLOutputter o = new XMLOutputter(Format.getPrettyFormat());
			response.getOutputStream().print(o.outputString(d));
		} catch (JDOMException e) {

			e.printStackTrace();
		}

	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

}