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

import multivalent.Behavior;

/**
 * 
 * This class contains the internal representation of an annotation with all its derived state.
 * This includes the XML annotation document ( the raw data), the identity of the signer
 * and the Behaviour visually representing the annotation in the document.
 * 
 * 
 * @author fabio
 *
 */
public class FabAnnotation {


	public static final FabAnnotation dummy_ = new FabAnnotation();

	AnnotationModel ann;

	String stringAnno;

	TrustedIdentity signer;

	int verificationStatus = -100;

	boolean loaded = false;

	public int totPages = 0;

	boolean sameUrl;

	boolean sameCRC;

	boolean sameTxtCRC;

	boolean sameLs;
	
	///SAM
	boolean sameBib;
	///

	boolean anonymous;

	Behavior behaviour;

	public Behavior getBehaviour() {
		return behaviour;
	}

	public void setBehaviour(Behavior mvnote) {
		behaviour = mvnote;
	}

	/**
	 * @return the loaded
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * @param loaded the loaded to set
	 */
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	/**
	 * @return the ann
	 */
	public AnnotationModel getAnn() {
		return ann;
	}

	/**
	 * @param ann the ann to set
	 */
	public void setAnn(AnnotationModel ann) {
		this.ann = ann;
	}

	/**
	 * @return the signer
	 */
	public TrustedIdentity getSigner() {
		return signer;
	}

	/**
	 * @param signer the signer to set
	 */
	public void setSigner(TrustedIdentity signer) {
		this.signer = signer;
	}

	/**
	 * @return the stringAnno
	 */
	public String getStringAnno() {
		return stringAnno;
	}

	/**
	 * @param stringAnno the stringAnno to set
	 */
	public void setStringAnno(String stringAnno) {
		this.stringAnno = stringAnno;
	}

	/**
	 * @return the verificationStatus
	 */
	public int getVerificationStatus() {
		return verificationStatus;
	}

	/**
	 * @param verificationStatus the verificationStatus to set
	 */
	public void setVerificationStatus(int verificationStatus) {
		this.verificationStatus = verificationStatus;
	}

	/**
	 * @return the sameCRC
	 */
	public boolean isSameCRC() {
		return sameCRC;
	}

	/**
	 * @param sameCRC the sameCRC to set
	 */
	public void setSameCRC(boolean sameCRC) {
		this.sameCRC = sameCRC;
	}

	/**
	 * @return the sameLs
	 */
	public boolean isSameLs() {
		return sameLs;
	}

	/**
	 * @param sameLs the sameLs to set
	 */
	public void setSameLs(boolean sameLs) {
		this.sameLs = sameLs;
	}

	/**
	 * @return the sameUrl
	 */
	public boolean isSameUrl() {
		return sameUrl;
	}

	/**
	 * @param sameUrl the sameUrl to set
	 */
	public void setSameUrl(boolean sameUrl) {
		this.sameUrl = sameUrl;
	}

	/**
	 * @return the sameTxtCRC
	 */
	public boolean isSameTxtCRC() {
		return sameTxtCRC;
	}

	/**
	 * @param sameTxtCRC the sameTxtCRC to set
	 */
	public void setSameTxtCRC(boolean sameTxtCRC) {
		this.sameTxtCRC = sameTxtCRC;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	///SAM
	public boolean isSameBib() {
		return sameBib;
	}

	public void setSameBib(boolean sameBib) {
		this.sameBib = sameBib;
	}

	///
	
}

