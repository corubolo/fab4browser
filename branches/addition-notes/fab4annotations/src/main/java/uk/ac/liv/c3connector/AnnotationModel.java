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

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

public class AnnotationModel {



	public String userid;
	public String author;
	public String id;
	public Date dateCreated;
	public Date dateModified;
	public String annotationBody;
	public String stringDescription;
	public String annotationUri;
	public String lexicalSignature;
	public String documentDigest;
	public String documentTextDigest;
	public int pageNumber=0;
	public String application;
	public String version;
	public String resourceUri;

	///SAM should move to internal model
	public boolean isReplyToSth = false;	
	public Integer replyTo = null;
	public String replyToFabId;
	public Integer uniqueId = null;  //when not yet stored in db, it will be null
	public Integer resourceId = null;	
	///
	
	public static final String type = " application/rdf+xml";

	public static void printModel (PrintStream ps, Object o){
		Field[] fi = o.getClass().getDeclaredFields();
		for (Field f: fi)
			if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())){
				ps.print(f.getName() + " = ");
				try {
					ps.println(f.get(o));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}}
		ps.println();

	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getUserid() {
		return userid;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}

	public Date getDateModified() {
		return dateModified;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setAnnotationBody(String annotationBody) {
		this.annotationBody = annotationBody;

	}

	public String getAnnotationBody() {
		return annotationBody;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor() {
		return author;
	}

	public void setUri(String uri) {
		annotationUri = uri;
	}

	public String getUri() {
		return annotationUri;
	}

	public void setDocumentTextDigest(String documentTextDigest) {
		this.documentTextDigest = documentTextDigest;
	}

	public String getDocumentTextDigest() {
		return documentTextDigest;
	}

	public void setDocumentDigest(String documentDigest) {
		this.documentDigest = documentDigest;
	}

	public String getDocumentDigest() {
		return documentDigest;
	}

	public void setLexicalSignature(String lexicalSignature) {
		this.lexicalSignature = lexicalSignature;
	}

	public String getLexicalSignature() {
		return lexicalSignature;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getApplication() {
		return application;
	}

	public void setStringDescription(String stringDescription) {
		this.stringDescription = stringDescription;
	}

	public String getStringDescription() {
		return stringDescription;
	}

	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	public String getResourceUri() {
		return resourceUri;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}

	///SAM
	public boolean isReplyToSth() {
		return isReplyToSth;
	}

	public void setReplyToSth(boolean isReplyToSth) {
		this.isReplyToSth = isReplyToSth;
	}

	public Integer getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(Integer replyTo) {
		this.replyTo = replyTo;
	}

	public String getAnnotationUri() {
		return annotationUri;
	}

	public void setAnnotationUri(String annotationUri) {
		this.annotationUri = annotationUri;
	}

	public String getReplyToFabId() {
		return replyToFabId;
	}

	public void setReplyToFabId(String replyToFabId) {
		this.replyToFabId = replyToFabId;
	}

	public Integer getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(Integer uniqueId) {
		this.uniqueId = uniqueId;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}
	
	
	///
}
