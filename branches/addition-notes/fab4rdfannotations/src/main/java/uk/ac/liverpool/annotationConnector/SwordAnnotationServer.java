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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JOptionPane;

import org.purl.sword.atom.Author;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Rights;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.client.Client;
import org.purl.sword.client.PostMessage;
import org.purl.sword.client.SWORDClient;
import org.purl.sword.client.SWORDClientException;

import uk.ac.liv.c3connector.AnnotationModel;
import uk.ac.liv.c3connector.AnnotationModelSerialiser;
import uk.ac.liverpool.annotationConnector.AnnotationServerConnectorInterface;
import uk.ac.liverpool.fab4.Fab4utils;

/**
 * this class implements a local Lucene annotation database, based on the LocalLuceneConector, in the specified directory
 * @author fabio
 *
 */
public class SwordAnnotationServer implements AnnotationServerConnectorInterface{

	private String destination;
	
	private String SRUaddress;
	
	AnnotationModelSerialiser ams = new AnnotationModelSerialiserRDF();
	private static boolean verify = false;
	
	public static final int ERROR_INVALID_DOCUMENT = -1;
	
	private SWORDClient client; 
	/**
	 * Creates the database in the given path
	 * @param path the path to a directory, where the lucene database will be read and created if non existing.
	 */
	public SwordAnnotationServer() {
	}
	
	public void init(String destinationURI, String sruaddress) {
		destination = destinationURI;
		SRUaddress = sruaddress;
	}
	private void initialiseServer(String location, String username, String password)
	throws MalformedURLException
	{
		System.out.println(location);
		client = new Client();
		URL url = new URL(location);
		int port = url.getPort();
		if( port == -1 ) 
		{
			port = 80;
		}

		client.setServer(url.getHost(), port);

		if (username != null && username.length() > 0 && 
            password != null && password.length() > 0 )
		{
			client.setCredentials(username, password);
			//System.out.println("User: "
				//	+ username + "Pass: "+ password);
		}
		else
		{
			client.clearCredentials();
		}
	}

	
	public boolean isVerifyingAnnotations() {
		return verify;
		}

	public void setVerifyAnnotation(boolean verify) {
		this.verify= verify;
		
	}

	private boolean verify(String anno){
		if (!verify)
			return true;
		try {
			ams.parse(anno);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}
	
	/* All the remaining methods are simply verify (if specified) and call delegate */
	
	public String IDSearch(String ID) {
		String query = "c3.idx-id = \"" + ID + "\"";
		String[] res = SRUSearch.search(SRUaddress, query).data;
		if (res == null || res.length == 0)
			return null;
		return res[0];

	}

	public String[] URISearch(String URI) {
		String query = /* "anno.checksum = " + crc + " and "+ */"c3.idx-uri = \"" + URI + "\"";
		String[] ret =  SRUSearch.search(SRUaddress, query).data;
		return ret;
	}

	public String[] checksumSearch(String checksum, String method,
			String encoding) {
		String query  = "c3.idx-digest = \"" + checksum + "\"";
		return  SRUSearch.search(SRUaddress, query).data;	
	}

	public String[] customIdSearch(String costomId,
			String application, String method) {
		String query = "c3.idx-digest-text = \"" + costomId + "\"";
		return  SRUSearch.search(SRUaddress, query).data;
	}

	public int deleteAnnotation(String id, String user, String secret) {
		return 1;	}

	public int deleteAnonymousAnnotation(String id, String secretKey) {
		return 1;
	}

	public String[] genericSearch(String query, String type) {
		if (type.equalsIgnoreCase("SRU"))
			return  SRUSearch.search(SRUaddress, query).data;
		else return null;	}

	public String getAuthenticationType() {
		return "plain";
	}

	public String getIndexingType() {
		return "Cheshire3";
	}

	public String getNewIdentifier(String user, String secret) {
		return "s" + user + "-" + Long.toHexString(Math.abs(new Random().nextLong()));
	}

	public String getRepositoryType() {
		return "sword/sru/cheshire3 store";
	}

	public boolean isGenericSearchSupported() {
		return true;
	}


	public String[] lexicalSignatureSearch(String lexicalSignature) {
		String query = "anno.lexicalSignature = \"" + lexicalSignature + "\"";
		return  SRUSearch.search(SRUaddress, query).data;
	}

	public int postAnnotation(String annotation, String user, String secret) throws Exception {
		try {
			//System.out.println("User: "
				//	+ user + "Pass: "+ secret);
			initialiseServer(destination, user, secret);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return -1;
		}
		AnnotationModel am = ams.parse(annotation);
		PostMessage message = new PostMessage(); 
		try {
			message.setFilepath(Fab4utils.copyToTemp(annotation).getAbsolutePath());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	//System.out.println(message.getFilepath());
		//System.out.println(destination);
		message.setDestination(destination);
		message.setFiletype(am.type);
		message.setUseMD5(false);
		message.setVerbose(true);
		message.setNoOp(false);
		message.setFormatNamespace("http://shaman.cheshire3.org/schemas/ORE-SIP");
		message.setOnBehalfOf(null);
		message.setChecksumError(false);
        message.setUserAgent("Fab4 annotation client");
		try {
			DepositResponse re = processPost(message);
			System.out.println(re.getLocation());
			System.out.println(re.getHttpResponse());
			System.out.println(re.getEntry().getId());
		} catch (SWORDClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//JOptionPane.showMessageDialog(null, "Error on annotation submission; check password and logs\n"+ e.getMessage());
			
			///SAM:
			return -1;
			///
		}

		///SAM since LocalLucene and REST return 0 in fine conditions, I change it to 0:
		//return 1;
		return 0;
	}

	
	/**
	 * Process the post response. The message contains the list of arguments 
	 * for the post. The method will then print out the details of the 
	 * response. 
	 * 
	 * @parma message The post options. 
	 *  
	 * @exception SWORDClientException if there is an error accessing the 
	 *                                 post response. 
	 */
	protected DepositResponse processPost(PostMessage message)
	throws SWORDClientException
	{
		
		System.out.println();

		DepositResponse response = client.postFile(message);

		System.out.println("The status is: " + client.getStatus());
		
		if( response != null)
		{
			// iterate over the data and output it 
			SWORDEntry entry = response.getEntry(); 
			System.out.println("Id: " + entry.getId());
			Title title = entry.getTitle(); 
			if( title != null ) 
			{
				System.out.print("Title: " + title.getContent() + " type: " ); 
				if( title.getType() != null )
				{
					System.out.println(title.getType().toString());
				}
				else
				{
					System.out.println("Not specified.");
				}
			}

			// process the authors
			Iterator<Author> authors = entry.getAuthors();
			while( authors.hasNext() )
			{
			   Author author = authors.next();
			   System.out.println("Author - " + author.toString() ); 
			}
			
			Iterator<String> categories = entry.getCategories();
			while( categories.hasNext() )
			{
			   System.out.println("Category: " + categories.next()); 
			}
			
			Iterator<Contributor> contributors = entry.getContributors();
			while( contributors.hasNext() )
			{
			   Contributor contributor = contributors.next(); 
			   System.out.println("Contributor - " + contributor.toString());
			}
			
			Iterator<Link> links = entry.getLinks();
			while( links.hasNext() )
			{
			   Link link = links.next();
			   System.out.println(link.toString());
			}

            Generator generator = entry.getGenerator();
            if( generator != null )
            {
                System.out.println("Generator - " + generator.toString());
            }
            else
            {
                System.out.println("There is no generator");
            }

            System.out.println( "Published: " + entry.getPublished());
			
			Content content = entry.getContent();
			if( content != null ) 
			{
			   System.out.println(content.toString());
			}
			else
			{
			   System.out.println("There is no content element.");
			}
			
			Rights right = entry.getRights();
			if( right != null ) 
         {
			   
            System.out.println(right.toString());
         }
         else
         {
            System.out.println("There is no right element.");
         }
			
	     Summary summary = entry.getSummary();
		 if( summary != null )
         {
            
            System.out.println(summary.toString());
         }
         else
         {
            System.out.println("There is no summary element.");
         }
			
			System.out.println("Update: " + entry.getUpdated() );
			System.out.println("Published: " + entry.getPublished());
			System.out.println("Verbose Description: " + entry.getVerboseDescription());
			System.out.println("Treatment: " + entry.getTreatment());
			System.out.println("Packaging: " + entry.getPackaging());

			if( entry.isNoOpSet() )
			{
				System.out.println("NoOp: " + entry.isNoOp());
			}
		}
		else
		{
			System.out.println("No valid Entry document was received from the server");
		}	
		return response;
	}
	
	public int postAnonymousAnnotation(String annotation, String secretKey) {
		if (!verify(annotation))
			return -1;
		return 1;
	}


	public int updateAnnotation(String replacement, String user, String secret) {
		if (!verify(replacement))
			return -1;
		return 1;
	}

	public int updateAnonymousAnnotation(String id, String replacement,
			String secretKey) {
		if (!verify(replacement))
			return -1;
		return 1;
		}


	public void setDestination(String destination) {
		this.destination = destination;
	}


	public String getDestination() {
		return destination;
	}
	public void setSRUaddress(String sRUaddress) {
		SRUaddress = sRUaddress;
	}
	public String getSRUaddress() {
		return SRUaddress;
	}

	public AnnotationModelSerialiser getDefaultAMS() {
		return  new AnnotationModelSerialiserRDF();
	}

	public String getDbLocation() {
		return "annoDbRDF";
	}
	
	
	///SAM
	public HashMap<String, String> authenticated(String username, String pass) {
		// TODO Auto-generated method stub
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("state", "0");
		return ret;
	}

	public int createNewUser(String username, String pass, String name,
			String des, String aff) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int createNewUser(String username, String pass, String email,
			String name, String des, String aff) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int addAnnotatedResource(String bibtex, String url) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String[] bibtexSearch(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	public int updateResourceBib(String url, String doi, String keywords) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String urlLacksBibDoiKeywords(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] retreiveAllTags(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	public void storePageAccess(String url, String username) {
		// TODO Auto-generated method stub
		
	}
	
	

}
