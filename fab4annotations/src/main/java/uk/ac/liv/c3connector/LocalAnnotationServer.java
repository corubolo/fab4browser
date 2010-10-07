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

import java.io.File;
import java.util.HashMap;

import uk.ac.liverpool.annotationConnector.AnnotationServerConnectorInterface;

/**
 * this class implements a local Lucene annotation database, based on the LocalLuceneConector, in the specified directory
 * @author fabio
 *
 */
public class LocalAnnotationServer implements AnnotationServerConnectorInterface{

	public LocalLuceneConnector delegate;

	static boolean verify = false;

	public static final int ERROR_INVALID_DOCUMENT = -1;

	private AnnotationModelSerialiser ams = new JDomAnnotationModelSerialiser();

	/**
	 * Creates the database in the given path
	 * @param path the path to a directory, where the lucene database will be read and created if non existing.
	 */
	public LocalAnnotationServer(File path) {
		delegate = new LocalLuceneConnector(path, ams);
		setVerifyAnnotation(false);
	}


	public boolean isVerifyingAnnotations() {
		return LocalAnnotationServer.verify;
	}

	public void setVerifyAnnotation(boolean verify) {
		delegate.setVerifyAnnotation(verify);
	}

	private boolean verify(String anno){
		if (!LocalAnnotationServer.verify)
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
		return delegate.IDSearch(ID);
	}

	public String[] URISearch(String URI) {
		return delegate.URISearch(URI);
	}

	public String[] checksumSearch(String checksum, String method,
			String encoding) {
		return delegate.checksumSearch(checksum, method, encoding);
	}

	public String[] customIdSearch(String costomId,
			String application, String method) {
		return delegate.customIdSearch(costomId, application, method);
	}

	public int deleteAnnotation(String id, String user, String secret) {
		return delegate.deleteAnnotation(id, user, secret);
	}

	public int deleteAnonymousAnnotation(String id, String secretKey) {
		return delegate.deleteAnonymousAnnotation(id, secretKey);
	}

	public String[] genericSearch(String query, String type) {
		return delegate.genericSearch(query, type);
	}

	public String getAuthenticationType() {
		return delegate.getAuthenticationType();
	}

	public String getIndexingType() {
		return delegate.getIndexingType();
	}

	public String getNewIdentifier(String user, String secret) {
		return delegate.getNewIdentifier(user, secret);
	}

	public String getRepositoryType() {
		return delegate.getRepositoryType();
	}

	public boolean isGenericSearchSupported() {
		return delegate.isGenericSearchSupported();
	}


	public String[] lexicalSignatureSearch(String lexicalSignature) {
		return delegate.lexicalSignatureSearch(lexicalSignature);
	}

	public int postAnnotation(String annotation, String user, String secret) throws Exception {
		if (!verify(annotation))
			return -1;
		return delegate.postAnnotation(annotation, user, secret);
	}

	public int postAnonymousAnnotation(String annotation, String secretKey) throws Exception {
		if (!verify(annotation))
			return -1;
		return delegate.postAnonymousAnnotation(annotation, secretKey);
	}


	public int updateAnnotation(String replacement, String user, String secret) throws Exception {
		if (!verify(replacement))
			return -1;
		return delegate.updateAnnotation(replacement, user, secret);
	}

	public int updateAnonymousAnnotation(String id, String replacement,
			String secretKey) throws Exception {
		if (!verify(replacement))
			return -1;
		return delegate.updateAnonymousAnnotation(id, replacement, secretKey);
	}


	public void init(String submitUri, String searchUri) {
		// TODO Auto-generated method stub

	}


	public AnnotationModelSerialiser getDefaultAMS() {
		// TODO Auto-generated method stub
		return ams;
	}


	public void setDefaultAMS(AnnotationModelSerialiser defaultAMS) {
		ams = defaultAMS;
		delegate.setDefaultAMS(defaultAMS);

	}


	public String getDbLocation() {

		return "annoDb";
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
