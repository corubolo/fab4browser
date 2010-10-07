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
 * (c) 2006-2007 University of Liverpool
 */

package uk.ac.liverpool.annotationConnector;

import java.util.HashMap;

import uk.ac.liv.c3connector.AnnotationModelSerialiser;

/**
 * This is supposed to be a generic enough interface for an annotation server.
 * The idea is that an annotation server can be made up of different components, so basically this interface Fab4
 * (or possibly another annotation client) and provide a standard protocol taking care of the few common operations.
 * 
 * The basic active operations are post, update, delete, and they require user authentication.
 * A user may want to submit an anonymous annotation, so in order to give him some control over the annotation in the future, the user could provide
 * a secret key on posting. Using the same key will allow to later delete or update the annotation, without the need to provide authentication.
 * 
 * The "passive" operation are the search. In fab4, annotations are automatically gathered upon opening a document.
 * In order to retrieve annotations linked to the document by different means, I defined 5 different type of search:
 * one could search by :
 * 
 * URI (so all annotations related to the document location, even if the contents change)
 * Document checksum: one could want to find all the annotations related to a specific document,independent of the location,
 * using an MD5 or other digest of the document binary data.
 * Lexical signature: another feature of the document textual contents.
 * Custom ID: for expansibility, this could be any application defined identifier for the document, for example a checksum of he document text (used by Fab4).
 * Generic search: a query specific to the indexing engine, so for example an SRW query (C3) or a Lucene or SQL. This will be server implementation specific.
 * I think authentication will be needed for the search as well, as an option at least.
 * 
 * The annotations are digitally signed, and include a unique identifier. For this reason, the client will need to request a unique identifier to the server
 * before submitting new annotations.
 * 
 * This interface will be then implemented by the "engines" for the specific server implementation.
 * These implementations will be made accessible by the system using web services,
 * I would think SOAP (they are used right now by Fab4) and possibly some simple XML over HTTP (for AJAX clients) (REST).
 * So the result would be a web application in JAVA (WAR) containing the web services (SOAP and REST) that will call the specific server implementation.
 * This can be easily included in the DSpace deployment.
 * 
 * The interface could then be implemented in DSpace directly (in Java) but possibly in other languages/systems as well, since it defines
 * basically a simple protocol for annotations over HTTP.
 * 
 * References:
 * 
 * the annotationXML schema
 * http://bodoni.lib.liv.ac.uk/VRE/AnnotationSchema.html
 * http://bodoni.lib.liv.ac.uk/AnnotationSchema/AnnotationSchema.xsd
 * 
 * @author fabiocorubolo
 *
 */

public interface AnnotationServerConnectorInterface {

	/* NOTES>
	 * !!!
	 * The annotation server could be chosen depending on the domain that is is visited. For example the institution one for the intranet or
	 * internal web sites, a generic one for the rest. It could as well be defined in a standard location file (like a robots.txt, placed
	 * in the directory we are visiting).
	 * Or in the client (fab4) one could define the server to use depending on the URL.
	 * !!!
	 */

	public AnnotationModelSerialiser getDefaultAMS();

	public void init(String submitUri, String searchUri);
	/** This method allows submitting an annotation, given username and secret. The secret can be any kind, so for example
	 * one could authenticate in a separate session and obtain a cookie to use here (to avoid revealing the password).
	 * The annotation identifier will be already in the annotation itself, since the user needs to sign the annotation before submitting it!
	 * 
	 * @param annotation The annotation, as specified by the schema
	 * @param user The user submitting the annotation. This should be an unique (server-wide) user id
	 * @param secret The Secret...
	 * @return 0 for success, otherwise an error code.
	 * @throws Exception
	 */
	public int postAnnotation (String annotation, String user, String secret) throws Exception;


	/**
	 * Deletes an annotation from the system.
	 * @param id The annotation Identifier (as specified in the schema)
	 * @param user The user submitting the annotation. This should be an unique (server-wide) user id
	 * @param secret The Secret...
	 * @return 0 for success, otherwise an error code.
	 */
	public int deleteAnnotation (String id, String user, String secret);
	/**
	 * Updates an annotation. This practically means deleting the old one from the repository and inserting the replacement one.
	 * The annotation identifier for the old annotation is supplied in the new annotation itself.
	 * 
	 * @param replacement
	 * @param user The user submitting the annotation. This should be an unique (server-wide) user id
	 * @param secret The Secret...
	 * @return 0 for success, otherwise an error code.
	 * @throws Exception
	 */
	public int updateAnnotation (String replacement, String user, String secret) throws Exception;

	/**
	 * This method will allow posting an anonymous annotation to the server.
	 * @param annotation The annotation, as per Schema
	 * @param secretKey A user (or application) defined secret for the specific annotation, in order to be able to delete
	 *  it afterwards. This could be set to 0 if the user wants to have the annotation modifiable by anyone.
	 * @return The identifier in the server.
	 * @throws Exception
	 */
	public int postAnonymousAnnotation (String annotation, String secretKey) throws Exception;
	/**
	 * Same as in delete annotation, but for the anonymous ones
	 * @param id The annotation Identifier (as specified in the schema)
	 * @param secretKey The secret key obtained when submitting the annotation
	 * @return 0 for success, otherwise an error code.
	 */
	public int deleteAnonymousAnnotation (String id, String secretKey);
	/**
	 * Same as in update annotation, but for the anonymous ones
	 * @param id The annotation ID (public)
	 * @param replacement the replacement annotation
	 * @param secretKey The secret key obtained when submitting the annotation
	 * @return 0 for success, otherwise an error code.
	 * @throws Exception
	 */
	public int updateAnonymousAnnotation (String id, String replacement, String secretKey) throws Exception;
	/**
	 * Retrieves a single annotation (if any) for the specified identifier
	 * @param ID The unique identifier for the annotation
	 * @return the annotation, if any
	 */
	public String IDSearch (String ID);


	/**
	 * Retrieves annotations referring to the same URI/URL
	 * @param URI
	 * @return The annotations satisfying the criteria
	 */


	public String[] URISearch (String URI);
	/**
	 * Retrieves all the annotations referring to the document(s) having the same checksum.
	 * This means that the application will compute the checksum on annotation submission.
	 * This provides a way to retrieve the annotations independently of the referring documen location.
	 * 
	 * @param checksum the Checksum, as specified below
	 * @param method using the specific method (MD5 is standard)
	 * @param encoding and the following encoding (base64 by default)
	 * @return The annotations that refer to document (s) having that checksum
	 */
	public String[] checksumSearch (String checksum, String method, String encoding);
	/**
	 * (by default disabled) returns all the annotations referring to a document having the same lexical signature.
	 * @param lexicalSignature
	 * @return The annotations satisfying the criteria
	 */
	public String[] lexicalSignatureSearch (String lexicalSignature);
	/**
	 * Return the annotations having a specific Custom id, that is application dependent.
	 * @param customId The ID
	 * @param application The application using it
	 * @param methodht e specific method/type of ID
	 * @return
	 */
	public String[] customIdSearch (String costomId, String application, String method);



	/**
	 * This is supposed to be application specific (depending on the server type, it could be an SRW or a SQL query, or a Lucene one).
	 * It may or may not be available (see isGenericSearchSupported();)
	 * @param query The query --- in any textual format
	 * @param type The type of query (Lucene, SRW, SQL, ...) as defined by getIndexingType();
	 * @return
	 */
	public String[] genericSearch (String query, String type);


	public String getRepositoryType();
	/**
	 * This is to discover the type of indexing engine used ...
	 * @return
	 */
	public String getIndexingType();
	/**
	 * This is supposed to offer some clue about the authentication method.
	 * @return
	 */
	public String getAuthenticationType();
	/**
	 * True if the indexing-specific queries are supported.
	 * @return
	 */
	public boolean isGenericSearchSupported();
	/**
	 * 
	 * @return
	 */
	public boolean isVerifyingAnnotations();
	/**
	 * @param verify true if we want the server to do a verification step before posting new annotations.
	 */
	public void setVerifyAnnotation(boolean verify);
	/**
	 * This method will be used by the application to obtain a new identifier for an annotation.
	 * Since the identifier will be placed in the annotation itself, and the annotation will be digitally signed,
	 * this identifier needs to be available to the application before the annotation is submitted.
	 * @param user The userid
	 * @param secret the authenticating secret
	 * @return a new unique (server-wise) identifier for an annotation
	 */
	public String getNewIdentifier (String user, String secret);

	public String getDbLocation();
	
	///SAM:
	
	/**
	 * @return users info and state of the authentication 
	 * state: 0: if ok, 1: if wrong pass, 2: if no such username, 3 if an exception
	 * if state is 0: then also returns user's name, email, affiliation
	 */
	public HashMap<String, String> authenticated(String username, String pass);
	
	/**
	 * @return 0: if ok, 1: if duplicated username, 2: if an exception occurs
	 */
	public int createNewUser(String username, String pass, String email, String name, String des, String aff);
	
	public int addAnnotatedResource(String bibtex, String url);
	
	/**
	 * finds annotations on resources which have same bibtex as the bibtex of this url
	 * @param url
	 * @return
	 */
	public String[] bibtexSearch(String url);
	
	/** 
	 * @param url
	 * @return String, '0' if no bib available , otherwise 'ab': a and b can be 0 or 1. a: 0 means no DOI in DB, 1: DOI available. b: talks about existence of keywords
	 */
	public String urlLacksBibDoiKeywords(String url);
	
	public int updateResourceBib(String url, String doi, String keywords);
	
	public String[] retreiveAllTags(String url);
	
//	public void storePageAccess(String url, String username);
	///
}

