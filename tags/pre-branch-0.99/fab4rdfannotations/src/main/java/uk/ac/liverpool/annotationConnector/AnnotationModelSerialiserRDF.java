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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.base64;

import uk.ac.liv.c3connector.AnnotationModel;
import uk.ac.liv.c3connector.AnnotationModelSerialiser;
import uk.ac.liverpool.fab4.Fab4utils;


public class AnnotationModelSerialiserRDF implements AnnotationModelSerialiser{
	
	public static final String NS_PMS = "http://id.loc.gov/standards/premis/rdf/";
	public static final String NS_ANNOTATION = "http://www.w3.org/2000/10/annotation-ns#";
	public static final String NS_FAB4 = "http://nara.cheshire3.org/ns/fab4/";

	public static final String type = " application/rdf+xml";
	
	
	public AnnotationModel parse(String me) throws URISyntaxException, JDOMException, IOException, ParseException {
		SAXBuilder s = new SAXBuilder(false);
		Document d = s.build(new StringReader(me));
		//XPath.setXPathClass();

		AnnotationModel am = new AnnotationModel();
		Element e = (Element) XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/anno:body");
		XMLOutputter o = new XMLOutputter();
		SimpleDateFormat sdt = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ssZ");
		if (e==null) throw new ParseException("Invlaid annotation - ingored",0 );
		am.annotationBody = o.outputString(e.getContent());
		am.application =((Element) XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/pms:creatingApplication/pms:Software/dc:title")).getTextTrim();
		am.author =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/dc:creator/dcterms:Agent/foaf:name")).getTextTrim();
		am.userid =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/dc:creator/dcterms:Agent/foaf:nick")).getTextTrim();
		am.dateCreated =sdt.parse(((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/dcterms:created")).getTextTrim());
		am.dateModified =sdt.parse(((Element) XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/dcterms:modified")).getTextTrim());
		am.documentDigest =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/ore:aggregates/ore:AggregatedResource/pms:hasFixity/pms:Fixity/rdf:value")).getTextTrim();
		am.documentTextDigest =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/ore:aggregates/ore:AggregatedResource/f4:base64-MD5-text")).getTextTrim();
		String about = ((Attribute) XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/@rdf:about")).getValue();
		am.id =about.substring(about.lastIndexOf('/')+1);
		am.annotationUri= about;
		am.pageNumber =Integer.parseInt(((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/ore:aggregates/ore:AggregatedResource/f4:pageNumber")).getTextTrim());
		am.resourceUri =((Attribute) XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/anno:annotates/@rdf:resource")).getValue();
		Element sa =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/anno:body/note/content"));
		if (sa!=null)
			am.stringDescription = sa.getTextTrim();
		am.version =((Element)  XPath.selectSingleNode(d,"/rdf:RDF/ore:ResourceMap/ore:describes/ore:Aggregation/pms:creatingApplication/pms:Software/pms:version")).getTextTrim();		
		return am;
//	
		//System.out.println(me);
//		Model model = ModelFactory.createDefaultModel();
//		StringReader sr = new StringReader(me);
//        model = model.read(sr, null, "RDF/XML");
//        Selector selector = new SimpleSelector(null, ORE.describes, (RDFNode) null);
//        StmtIterator itr = model.listStatements(selector);
//        if (itr.hasNext())
//        {
//            Statement statement = itr.nextStatement();
//            Resource resource = (Resource) statement.getSubject();
//				ResourceMapJena rem = new ResourceMapJena();
//				rem.setModel(model, new URI(resource.getURI()));
//				AnnotationModel am = new AnnotationModel();
//				AggregationJena agg = (AggregationJena) rem.getAggregation();
//				URI u = agg.getURI();
//				am.setUri(u.toString());
//				String path = u.getPath();
//
//				am.setId(path.substring(path.lastIndexOf('/')+1));
//				agg.getAggregatedResources();
//				am.setDateCreated(agg.getCreated());
//				am.setDateModified(agg.getModified());
//				Model agm = agg.getModel();
//				Resource agr = agg.getResource();
//				Statement st = agr.getProperty(agm.createProperty(NS_ANNOTATION,"body"));
//				am.setAnnotationBody(((Literal) st.getObject()).getString());
//				List<AggregatedResource> l  = agg.getAggregatedResources();
//				for (AggregatedResource a:l){
//					am.setResourceUri(a.getURI().toString());
//					AggregatedResourceJena aj  = (AggregatedResourceJena) a;
//					st = aj.getResource().getProperty(agm.createProperty(NS_FAB4, "base64-MD5-text"));
//					am.setDocumentTextDigest(((Literal) st.getObject()).getString());
//					st = aj.getResource().getProperty(agm.createProperty(NS_FAB4, "pageNumber"));
//					am.setPageNumber((((Literal) st.getObject()).getInt()));
//					
//					st = aj.getResource().getProperty(agm.createProperty(NS_PMS+"hasFixity"));
//					am.setDocumentDigest(st.getProperty(RDF.value).getString());
//				}
//				st = agr.getProperty(DC.description);
//				am.setStringDescription(st.getProperty(RDF.value).getString());
//				st = agr.getProperty(agm.createProperty(NS_PMS,"creatingApplication"));
//				//st = st.getProperty(agm.createProperty(NS_PMS,"Software"));
//				Statement st2 = st.getProperty(DC.title);
//				am.setApplication(((Literal) st2.getObject()).getString());
//				st = st.getProperty(agm.createProperty(NS_PMS,"version"));
//				am.setVersion(((Literal) st.getObject()).getString());
//				st = agr.getProperty(agm.createProperty(NS_ANNOTATION,"annotates"));
//				String uri2 = (st.getResource().getURI());
//				if (!uri2.equals(am.getResourceUri())){
//					am.setResourceUri(uri2);
//					System.out.println("This should not happen");
//				}
//				List<Agent> la = agg.getCreators();
//				for (Agent cr:la){
//					AgentJena a = (AgentJena) cr;
//					st = a.getResource().getProperty(FOAF.name);
//					am.setAuthor(((Literal) st.getObject()).getString());
//					st = a.getResource().getProperty(FOAF.nick);
//					am.setUserid(((Literal) st.getObject()).getString());
//				}
//
//				return am;
//		
//        }
//		
//		return null;
	}
	
	public String serialise(AnnotationModel am) throws URISyntaxException{
		
		
		
		am.setApplication(getApplicationName());
		am.setVersion(getVersion());
		
		String annName = am.annotationUri + am.id;
		try {
			annName = am.annotationUri + URLEncoder.encode(am.id, "UTF-8");
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (am.stringDescription == null){
			am.stringDescription = Fab4utils.stripXml(am.annotationBody);
		}
		Document d = new Document();
		Namespace RDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		Namespace FOAF = Namespace.getNamespace("foaf","http://xmlns.com/foaf/0.1/");
		//Namespace OWL = Namespace.getNamespace("owl","http://www.w3.org/2002/07/owl#");
		Namespace DC = Namespace.getNamespace("dc","http://purl.org/dc/elements/1.1/");
		Namespace PMS = Namespace.getNamespace("pms","http://id.loc.gov/standards/premis/rdf/");
		Namespace ORE = Namespace.getNamespace("ore","http://www.openarchives.org/ore/terms/");
		Namespace DCTERMS = Namespace.getNamespace("dcterms","http://purl.org/dc/terms/");
		Namespace ANNO = Namespace.getNamespace("anno","http://www.w3.org/2000/10/annotation-ns#");
		Namespace F4 = Namespace.getNamespace("f4","http://nara.cheshire3.org/ns/fab4/");
		Element r = new Element("RDF", RDF);
		r.addNamespaceDeclaration(RDF);
		r.addNamespaceDeclaration(FOAF);
		r.addNamespaceDeclaration(DC);
		r.addNamespaceDeclaration(PMS);
		r.addNamespaceDeclaration(ORE);
		r.addNamespaceDeclaration(DCTERMS);
		r.addNamespaceDeclaration(ANNO);
		r.addNamespaceDeclaration(F4);
		
		Element rm = new Element ("ResourceMap",ORE);
		rm.setAttribute("about", annName + ".xml", RDF);
		r.addContent(rm);
		
		SimpleDateFormat sdt = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ssZ");
		
		rm.addContent(new Element("modified", DCTERMS)
			.setAttribute("datatype","http://www.w3.org/2001/XMLSchema#date", RDF)
				.addContent(sdt.format(new Date())));
		rm.addContent(new Element("creator", DC)
			.addContent(new Element("Agent", DCTERMS)
				.addContent(new Element("name",FOAF).addContent("Fab4 Serilaliser"))));
		

		Element agg = new Element("Aggregation", ORE);
		agg.setAttribute("about", annName, RDF);
		Element des = new Element("describes", ORE).addContent(agg);
		rm.addContent(des);
		// annotation body
		SAXBuilder b = new SAXBuilder();
		try {
			Document ab = b.build(new StringReader(am.annotationBody));
			agg.addContent(new Element("body", ANNO).setAttribute("parseType", "Literal", RDF).addContent(ab.getRootElement().detach()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Element desc = new Element("description", DC);
		desc.setAttribute("parseType","Resource", RDF);
		Element fixity = new Element("Fixity",PMS);
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(am.stringDescription.getBytes());
			String digest = base64.toString(md.digest()); // 128 bit or 16 bytes
			fixity.addContent(new Element("value",RDF).addContent(digest));
		} catch (NoSuchAlgorithmException e1) {
			System.out.println("Unable to compute MD5; missing support class");
		}
		fixity.addContent(new Element("algorithm",PMS).addContent("md5"));
		fixity.addContent(new Element("encoding",PMS).addContent("base64"));
		desc.addContent(new Element("hasFixity",PMS).addContent(fixity));
		desc.addContent(new Element("format",DC).addContent("text/plain"));

		desc.addContent(new Element("value",RDF).addContent(am.stringDescription));
		agg.addContent(desc);
		
		Element crea = new Element("creator", DC);
		crea.addContent(new Element("Agent", DCTERMS)
			.addContent(new Element("name", FOAF).addContent(am.author)).
			addContent(new Element("nick",FOAF).addContent(am.userid)));
		agg.addContent(crea);
		
		agg.addContent(new Element("modified", DCTERMS).setAttribute("datatype","http://www.w3.org/2001/XMLSchema#date", RDF)
				.addContent(sdt.format(am.dateModified)));
		agg.addContent(new Element("created", DCTERMS).setAttribute("datatype","http://www.w3.org/2001/XMLSchema#date", RDF)
				.addContent(sdt.format(am.dateCreated)));
		
		Element sw = new Element("Software", PMS).setAttribute("about", "http://bodoni.lib.liv.ac.uk/fab4", RDF);
		sw.addContent(new Element("version", PMS).addContent(am.version));
		sw.addContent(new Element("title", DC).addContent(am.application));
		agg.addContent(new Element("creatingApplication", PMS).addContent(sw));
		agg.addContent(new Element("type", RDF).setAttribute("resource","http://www.w3.org/2000/10/annotation-ns#Annotation",RDF));			
		
		Element aggRes = new Element("AggregatedResource", ORE).setAttribute("about",am.resourceUri, RDF);
		
		aggRes.addContent(new Element("base64-MD5-text", F4).addContent(am.documentTextDigest));
		aggRes.addContent(new Element("pageNumber", F4).addContent(""+am.pageNumber));

		Element fixity2 = new Element("Fixity",PMS);
		fixity2.addContent(new Element("value",RDF).addContent(am.documentDigest));
		
		fixity2.addContent(new Element("algorithm",PMS).addContent("md5"));
		fixity2.addContent(new Element("encoding",PMS).addContent("base64"));
		aggRes.addContent(new Element("hasFixity",PMS).addContent(fixity2));
		
		
		agg.addContent(new Element("annotates", ANNO).setAttribute("resource",am.resourceUri, RDF));

		
		agg.addContent(new Element("aggregates", ORE).addContent(aggRes));
		d.addContent(r);
		XMLOutputter o = new XMLOutputter(Format.getCompactFormat());
		//System.out.println(o.outputString(d));
		System.out.println(" Doc digest  "+  am.documentTextDigest);
		
		return o.outputString(d);
		
//		AggregationJena agg = new AggregationJena();
//		try {
//			agg.initialise(new URI(
//					am.annotationUri + URLEncoder.encode(am.id, "UTF-8")));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		ResourceMap rm = ((ResourceMapJena) agg.createResourceMap(new URI(agg
//				.getURI()
//				+ ".xml")));
//		Model rmm = ((ResourceMapJena) rm).getModel();
//		rmm.setNsPrefix("pms", NS_PMS);
//		rmm.setNsPrefix("anno", NS_ANNOTATION);
//		rmm.setNsPrefix("f4", NS_FAB4);
//
//		agg.addType(new URI(NS_ANNOTATION + "Annotation"));
//		agg.setModified(am.dateModified);
//		agg.setCreated(am.dateCreated);
//		Model agm = agg.getModel();
//		
//		
//		agm.add(agg.getResource(), agm.createProperty(NS_ANNOTATION, "body"),
//				am.annotationBody, true);
//
//		Resource body = agm.createResource();
//		//body.addProperty(RDF.type, agm.createResource(NS_FAB4 + "StringBody"));
//		if (am.stringDescription == null){
//			am.stringDescription = Fab4utils.stripXml(am.annotationBody);
//		}
//		body.addProperty(RDF.value, am.stringDescription);
//		body.addProperty(DC.format, "text/plain");
//		Resource ar = agg.getResource();
//		agm.add(ar, DC.description, body);
//
//		Resource fix = body.getModel().createResource();
//		fix.addProperty(RDF.type, body.getModel().createProperty(
//				NS_PMS + "Fixity"));
//		fix.addProperty(body.getModel().createProperty(NS_PMS, "encoding"),
//				"base64");
//		fix.addProperty(body.getModel().createProperty(NS_PMS, "algorithm"),
//				"md5");
//		try {
//			MessageDigest md = MessageDigest.getInstance("MD5");
//			md.update(am.stringDescription.getBytes());
//			String digest = base64.toString(md.digest()); // 128 bit or 16 bytes
//			fix.addProperty(RDF.value, digest);
//		} catch (NoSuchAlgorithmException e1) {
//			System.out.println("Unable to compute MD5; missing support class");
//		}
//		
//		body.getModel().add(body,
//				body.getModel().createProperty(NS_PMS + "hasFixity"), fix);
//
//		AgentJena ag = new AgentJena();
//		ag.initialise();
//		ag.addName(am.author);
//		ag.getResource().addProperty(FOAF.nick, am.userid);
//		
//		agg.addCreator(ag);
//
//		Resource app = agm.createResource("http://bodoni.lib.liv.ac.uk/fab4");
//		app.addProperty(RDF.type, body.getModel().createProperty(NS_PMS,
//				"Software"));
//		app.addProperty(DC.title, am.application);
//		app.addProperty(agm.createProperty(NS_PMS, "version"), am.version);
//		agm.add(ar, agm.createProperty(NS_PMS, "creatingApplication"), app);
//		AggregatedResourceJena res1 = new AggregatedResourceJena();
//		res1.initialise(new URI(am.resourceUri));
//		agm.add(ar, agm.createProperty(NS_ANNOTATION, "annotates"), res1
//				.getResource());
//		if (am.lexicalSignature != null)
//			res1.getResource().addProperty(
//					agm.createProperty(NS_FAB4, "lexicalSignature"),
//					am.lexicalSignature);
//		
//		res1.getResource().addProperty(
//				agm.createProperty(NS_FAB4, "base64-MD5-text"),am.documentTextDigest);
//		res1.getResource().addProperty(
//				agm.createProperty(NS_FAB4, "pageNumber"), "" + am.pageNumber);		
//		Model m = res1.getResource().getModel();
//		Resource fix2 = m.createResource();
//		fix2.addProperty(RDF.type, m.createProperty(NS_PMS + "Fixity"));
//		fix2.addProperty(m.createProperty(NS_PMS, "encoding"), "base64");
//		fix2.addProperty(m.createProperty(NS_PMS, "algorithm"), "md5");
//		fix2.addProperty(RDF.value, am.documentDigest);
//		m.add(res1.getResource(), m.createProperty(NS_PMS + "hasFixity"), fix2);
//		agg.addAggregatedResource(res1);
//		REMValidatorJena r = new REMValidatorJena();
//		r.prepForSerialisation(((ResourceMapJena) rm));
//		StringWriter sw = new StringWriter();
//		Model model = ((ResourceMapJena) rm).getModel();
//		model.write(sw, "RDF/XML-ABBREV");
//		
//		
//		return sw.toString();
		
	}

	public String getApplicationName() {
		return  "multivalent/fab4/rdf/ore";
	}

	public String getVersion() {
		
		return "1.0";
	}

	public boolean canParse(String annotation) {
		try {
			parse (annotation);
		} catch (Exception x){
		return false;
		}
//		SAXBuilder b = new SAXBuilder(false);
//	
//
//		try {
//			System.out.println(annotation);
//			Document d = b.build(new StringReader(annotation));
//			Element e = d.getRootElement();
//			System.out.println(e.getName());
//			Element sw = e.getChild("ore:ResourceMap").getChild("ore:describes").getChild("Annotation", Namespace.getNamespace(NS_ANNOTATION)).getChild("creatingApplication", Namespace.getNamespace(NS_PMS)).getChild("Software");
//			String app = sw.getAttributeValue("rdf:about");
//			String title = sw.getChildTextTrim("dc:title");
//			System.out.println(app);
//			System.out.println(title);
//			if (title.equals(getApplicationName()))
//				return true;
//			else 
//				return false;
//				
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
		return true;
	}
	
	
}
