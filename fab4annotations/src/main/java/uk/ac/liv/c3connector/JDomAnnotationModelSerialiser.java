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
import java.util.HashMap;

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
		
		///SAM
		if(DistributedPersonalAnnos.currentServer == Servers.REST){
			if(r.getChildText("replyToSth") != null ){ //for annotations that were saved before adding this to code!
				am.setReplyToSth(r.getChildText("replyToSth").equals("true"));
			
				if(!r.getChildText("replyTo").equals(""))
					am.setReplyTo(Integer.parseInt(r.getChildText("replyTo")));
			
			
				am.setReplyToFabId(r.getChildText("replyToFabId"));
				
				if(!r.getChildText("uniqueid").equals(""))
					am.setUniqueId(Integer.parseInt(r.getChildText("uniqueid")));
				
				if(r.getChildText("resourceId") != null )
					if(!r.getChildText("resourceId").equals(""))
						am.setResourceId(Integer.parseInt(r.getChildText("resourceId")));
			}
		}
		///
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
		
		///SAM
		//remove invalid encoding 0x2  (replace it with space: 0x20):
		s.annotationBody = replaceInvalidChars(s.annotationBody);
		///
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
		
		///SAM
		if(DistributedPersonalAnnos.currentServer == Servers.REST){
			r.addContent(new Element("replyToSth").addContent(s.isReplyToSth()?"true":"false"));
			r.addContent(new Element("replyToFabId").addContent(s.replyToFabId));
			r.addContent(new Element("uniqueid").addContent(s.getUniqueId()!=null ? String.valueOf(s.getUniqueId()) : null ));
			r.addContent(new Element("replyTo").addContent(s.getReplyTo()!=null ? String.valueOf(s.getReplyTo()) : null ));
			r.addContent(new Element("resourceId").addContent(s.getResourceId()!=null ? String.valueOf(s.getResourceId()) : null ));
		}
		///
		
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
	
	///SAM
	/**
	 * remove invalid encoding 0x2  (replace it with space: 0x20):
	 * @param string: the string in which you want to replace the invalid chars
	 * @return the string which is the result of replacing invalid chars in the input parameter (string)
	 */
	private String replaceInvalidChars(String string){
		/*
		String invalidChar = "\u0002";		
		byte[] invalidBytes = invalidChar.getBytes();		
		
		String space = "\u0020";
		byte[] spaceBytes = space.getBytes();
		*/
		
		/*byte[] dontKnows = "?".getBytes();
		HashMap<Byte,Byte> corrections = new HashMap<Byte,Byte>();		
		corrections.put(invalidBytes[0],dontKnows[0]);
		corrections.put("\u001A".getBytes()[0],"fi".getBytes()[0]);*/
		
		char[] charArr = string.toCharArray();
		for( int i = 0 ; i < charArr.length ; i++ ){
			if((byte)charArr[i] <= "\u001B".getBytes()[0]
			          || (byte)charArr[i] >= "\u007F".getBytes()[0]){ //totally invalid
				if(charArr[i] == '\n' || charArr[i] == '\t' || charArr[i] == '\r')
					continue;
				//I know this correction:
				if((byte)charArr[i] == "\u001A".getBytes()[0]){
					string = string.substring(0,i)+"fi"+string.substring(i+1,string.length());				
					i++; //one char is added to total size ("fi" instead of one char)
				}
				else{
					string = string.substring(0,i)+"_"+string.substring(i+1,string.length());					
				}
			}
			//different space values
			else if((byte)charArr[i] <= "\u001F".getBytes()[0]
			           && (byte)charArr[i] > "\u001B".getBytes()[0]){
				string = string.substring(0,i)+" "+string.substring(i+1,string.length());				
			}
			else
				continue;
			charArr = string.toCharArray();
		}
		/*
		byte[] stringBytes = string.getBytes();
		for(int i = 0; i < stringBytes.length ; i++ ){
			if(stringBytes[i] == invalidBytes[0] )
				stringBytes[i] = spaceBytes[0];
		}
		return new String(stringBytes);*/
		return string;
	}
	
	public static void main(String[] args) {
		
		String invalidChar = "\u0002";
		System.out.println("a"+invalidChar+"b");
		byte[] invalidBytes = invalidChar.getBytes();
		System.out.println("length:"+invalidBytes.length);
		String space = "\u0020";
		byte[] spaceBytes = space.getBytes();
		System.out.println("a"+space+"b");
		String test = "\u0009";
		System.out.println(test);
		System.out.println(test.getBytes()[0]);
		char a = test.toCharArray()[0];
		System.out.println((byte)a);
//		System.out.println("\u0001");
		System.out.println("length:"+test.getBytes().length);
	}

}
