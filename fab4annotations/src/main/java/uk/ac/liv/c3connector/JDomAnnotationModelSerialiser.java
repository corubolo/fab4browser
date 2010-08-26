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
package uk.ac.liv.c3connector;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class JDomAnnotationModelSerialiser implements AnnotationModelSerialiser {

	public AnnotationModel parse(String s) throws JDOMException, IOException {
		AnnotationModel am = new AnnotationModel();
		SAXBuilder b = new SAXBuilder(false);
		Document d = b.build(new StringReader(s));
		Element r = d.getRootElement();
		am.id = r.getChildTextTrim("id");
		XMLOutputter o = new XMLOutputter();
		am.annotationBody = o.outputString(r.getChild("xml_body").getContent());

		Element ar = r.getChild("annotated_resource");
		am.annotationUri = "";
		am.application = r.getChildText("application");
		am.author = r.getChildText("author");
		SimpleDateFormat sdt = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		try {am.dateCreated = sdt.parse(r.getChildText("date", Namespace.getNamespace("http://purl.org/dc/elements/1.1/")));
		am.dateModified  = sdt.parse(r.getChildText("modification_date"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		am.documentDigest = ar.getChildTextTrim("checksum");
		am.documentTextDigest =  ar.getChildTextTrim("specific_id");
		if (r.getChild("location")!=null && r.getChild("location").getChildText("page_number")!=null)
			am.pageNumber = Integer.parseInt(r.getChild("location").getChildText("page_number"));
		am.resourceUri = ar.getChildTextTrim("URI");
		am.stringDescription = "";
		am.userid = r.getChildTextTrim("user_id");
		am.version = r.getChildTextTrim("");
		//am.printModel(System.out, am);

		return am;
	}

	public String serialise(AnnotationModel s) {
		Element r = new Element("annotation", Namespace.getNamespace("ann","http://bodoni.lib.liv.ac.uk/AnnotationSchema" ));
		r.addContent(new Element("id").setText(s.id));
		Element ar = new Element("annotated_resource");
		ar.addContent(new Element("URI").addContent(s.resourceUri));
		ar.addContent(new Element("checksum").addContent(s.documentDigest));
		ar.addContent(new Element("specific_id").addContent(s.documentTextDigest));
		r.addContent(ar);
		SAXBuilder b = new SAXBuilder();
		try {
			Document ab = b.build(new StringReader(s.annotationBody));
			r.addContent(new Element("xml_body").addContent(ab.getRootElement().detach()));
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		r.addContent(new Element("type").addContent("multivalent annotation"));
		r.addContent(new Element("author").addContent(s.author));
		r.addContent(new Element("application").addContent(getApplicationName()));
		r.addContent(new Element("version").addContent(getVersion()));
		SimpleDateFormat sdt = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		r.addContent(new Element("modification_date").addContent(sdt.format(s.getDateModified())));
		r.addContent(new Element("date",Namespace.getNamespace("ns","http://purl.org/dc/elements/1.1/")).addContent(sdt.format(s.getDateCreated())));
		r.addContent(new Element("user_id").addContent(s.userid));
		r.addContent(new Element("location").addContent(new Element("page_number").addContent(""+s.pageNumber)));
		Document d = new Document();
		d.setRootElement(r);
		XMLOutputter o = new XMLOutputter();
		String res= o.outputString(d);
		System.out.println(res);
		return res;
	}

	public String getApplicationName() {
		return "multivalent/fab4";
	}

	public String getVersion() {
		return "1.1";
	}

	public boolean canParse(String annotation) {
		return true;

	}

}
