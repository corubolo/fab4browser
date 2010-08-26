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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.liv.c3connector.crypto.PKCS5SimpleKeyStore;

import com.base64;

/**
 * this class represents a trusted identity for the annotation system.
 * Different methods allow reading and storing the trusted identity  in different ways.
 * 
 * @author fabio
 *
 */
public class TrustedIdentity {
	private String name;
	private String organisation;
	private String email;
	private PublicKey publicKey;
	private boolean itsme = false;
	/** we always trust Fabio :) */
	private final static String _AUTHOR=
		"AA5GYWJpbyBDb3J1Ym9sbwAXVW5pdmVyc2l0eSBvZiBMaXZlcnBvb2wAFGYuY29ydWJvbG\n" +
		"9AbGl2LmFjLnVrAANEU0EABVguNTA5AAAA9DCB8TCBqAYHKoZIzjgEATCBnAJBAPymgs6O\n" +
		"Esq6Ju/M9xEOUm2weLBe3svNHrSiCPOuFheuAfNbkaR+bfY0E8XhLtCJm80TKs1Q2ZFRvc\n" +
		"Q+5zdZLhcCFQCWLt3MNpy6jrsmDua2oSbZNG44xQJAZ4Rxsnqc9E7pGknFFH2xqaryRPBa\n" +
		"Q01khpMdLRQnG541Awtx/XPaF5Bpsy4pNWMOHCBiNU0NogpsQW5QvnlMpANEAAJBALR8Zy\n" +
		"7fy0xHN7f4X8liquDYUZi6G5sUO73wi0r3CPcU5T9yQvnR+AICKtONYHAoezABKfIIsCvx\n" +
		"koZngZOSWyY=";

	/** reads a sore of trusted identity
	 * 
	 * @param store the File to read from
	 * @return a list of trusted ids
	 */
	public static List<TrustedIdentity> readStore(File store) {
		ArrayList<TrustedIdentity> ti = new ArrayList<TrustedIdentity>(50);
		InputStream fis;
		try {
			fis = new FileInputStream(store);
		} catch (FileNotFoundException e) {
			byte[] buffer=base64.fromString(TrustedIdentity._AUTHOR);
			fis = new ByteArrayInputStream(buffer);
		}
		DataInputStream dis = new DataInputStream(fis);
		try {
			while (true) {
				TrustedIdentity t = new TrustedIdentity();
				readTi(dis, t);
				ti.add(t);
			}
		} catch (EOFException e) {
			return ti;
		} catch (IOException e) {
			e.printStackTrace();
			return ti;
		}
	}

	/** reads a single id from a string, base64 encoded (for copy and paste)
	 * 
	 * @param k
	 * @return
	 * @throws IOException
	 */
	public static TrustedIdentity readTi(String k) throws IOException {
		byte[] buff = base64.fromString(k);
		DataInputStream dis = new DataInputStream(
				new ByteArrayInputStream(buff));
		TrustedIdentity t = new TrustedIdentity();
		TrustedIdentity.readTi(dis, t);
		return t;
	}


	/**
	 * Converts a TI to a string (base64 encoded)
	 * @param myTI
	 */
	public static String convertTotring(TrustedIdentity myTI) throws IOException {
		ByteArrayOutputStream ou = new ByteArrayOutputStream();
		DataOutputStream dod = new DataOutputStream(ou);
		TrustedIdentity.writeTi(dod, myTI);
		dod.close();
		byte[] t = ou.toByteArray();
		return base64.formatString(t, 70, "", false);

	}

	/**
	 * @param dis
	 * @param t
	 * @throws IOException
	 */
	public static void readTi(DataInputStream dis, TrustedIdentity t) throws IOException {
		t.setName(dis.readUTF());
		t.setOrganisation(dis.readUTF());
		t.setEmail(dis.readUTF());
		try {
			t.setPublicKey(PKCS5SimpleKeyStore.readPublicKey(dis));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	public static boolean writeStore(File store,  List<TrustedIdentity> ti) throws IOException {
		FileOutputStream fos = new FileOutputStream(store);
		DataOutputStream dod = new DataOutputStream(fos);
		for (TrustedIdentity t:ti)
			writeTi(dod, t);

		return true;
	}

	/**
	 * @param dod
	 * @param t
	 * @throws IOException
	 */
	public static void writeTi(DataOutputStream dod, TrustedIdentity t) throws IOException {
		dod.writeUTF(t.getName());
		dod.writeUTF(t.getOrganisation());
		dod.writeUTF(t.getEmail());
		PKCS5SimpleKeyStore.writePublicKey(t.getPublicKey(), dod);
	}


	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the organisation
	 */
	public String getOrganisation() {
		return organisation;
	}
	/**
	 * @param organisation the organisation to set
	 */
	public void setOrganisation(String organisation) {
		this.organisation = organisation;
	}
	/**
	 * @return the publicKey
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
	/**
	 * @param publicKey the publicKey to set
	 */
	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
	/**
	 * @param name
	 * @param organisation
	 * @param email
	 * @param publicKey
	 */
	public TrustedIdentity(String name, String organisation, String email, PublicKey publicKey) {
		super();
		this.name = name;
		this.organisation = organisation;
		this.email = email;
		this.publicKey = publicKey;
	}

	/**
	 * 
	 */
	public TrustedIdentity() {
	}
	/**
	 * @return the itsme
	 */
	public boolean isItsme() {
		return itsme;
	}

	/**
	 * @param itsme the itsme to set
	 */
	public void setItsme(boolean itsme) {
		this.itsme = itsme;
	}
}
