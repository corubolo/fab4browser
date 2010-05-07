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


package uk.ac.liv.c3connector.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.base64;


/**
 * 
 * This is a simple keystore  implementation used by the Fab4 distriburted annotation system.
 * It contains methods to read and represent the key as a single string, that are used to distribute the private key.
 * This is a simple temporary solution before a full PKI system is implemented.
 * 
 * @author fabio
 * 
 */
public class PKCS5SimpleKeyStore {

	private File keystoreFile;

	/**
	 * @param keystoreFile
	 */
	public PKCS5SimpleKeyStore(File keystoreFile) {
		super();
		this.keystoreFile = keystoreFile;
	}

	/**
	 * Save encripting the key store file will be: (UTF8) key type - (utf8) pub key encoding - (int) pub key length -
	 * (byte[]) pub key - (byte[8]) init vector - (byte[16]) salt - (utf8) pub key encoding - (int) priv key length -
	 * (byte[]) private key
	 * 
	 * @param keysize
	 * @param lp the keystore password. If null, the privare key is not encrypted.
	 * @param keyType
	 * @return the newly created key pair
	 * @throws Exception
	 */
	public KeyPair generateKeys(int keysize, char[] lp, String keyType)
	throws Exception {// GEN-FIRST:event_generateKeys
		//System.out.println("a");
		KeyPairGenerator kpg;
		// genetarte the key pair !!!!
		kpg = KeyPairGenerator.getInstance(keyType);
		kpg.initialize(keysize, new SecureRandom());
		KeyPair kp = kpg.generateKeyPair();
		//System.out.println("b");
		byte[] pub = kp.getPublic().getEncoded();
		byte[] priv = kp.getPrivate().getEncoded();


		FileOutputStream fos = new FileOutputStream(keystoreFile);
		DataOutputStream dd = new DataOutputStream(fos);
		// key type (UTF8)
		dd.writeUTF(keyType);
		// (utf8) pub key encoding
		dd.writeUTF(kp.getPublic().getFormat());
		// (int)pub key length
		dd.writeInt(pub.length);
		// //System.out.println(pub.length);
		// (byte[]) pub key
		dd.write(pub);


		SecureRandom sr = new SecureRandom();
		byte[] initVector = new byte[8];
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		sr.nextBytes(initVector);
		byte[] hpass = null;
		if (lp!=null) {
			String pass = new String(lp);
			encPass(pass);
		}
		// (byte[8]) init vector
		dd.write(initVector);
		// (byte[16]) salt
		dd.write(salt);
		// (utf8) priv key encoding
		dd.writeUTF(kp.getPrivate().getFormat());
		// (int) priv key length
		dd.writeInt(priv.length);

		if (lp!=null) {
			byte[] password = new byte[hpass.length];
			System.arraycopy(hpass, 0, password, 0, hpass.length);
			byte[] secret = PKCS5S2ParametersGenerator.generateDerivedKey(password,
					salt, PKCS5SimpleKeyStore.iterationCount, 16);
			segret sk = new segret(secret);
			Cipher blowFish = Cipher.getInstance("Blowfish/CFB/NoPadding");
			IvParameterSpec ivp = new IvParameterSpec(initVector);
			blowFish.init(Cipher.ENCRYPT_MODE, sk, ivp);
			CipherOutputStream cos = new CipherOutputStream(dd, blowFish);
			cos.write(priv);
			cos.close();
		}
		else
			dd.write(priv);
		dd.close();
		fos.close();
		return kp;

	}

	public static String getAsString(PublicKey k) throws IOException {
		ByteArrayOutputStream ou = new ByteArrayOutputStream();
		DataOutputStream dod = new DataOutputStream(ou);
		writePublicKey(k, dod);
		dod.close();
		byte[] t = ou.toByteArray();
		return base64.formatString(t, 70, "", false);
	}

	/**
	 * @param k
	 * @throws IOException
	 */
	public static void writePublicKey(PublicKey k, DataOutputStream dod)
	throws IOException {
		dod.writeUTF(k.getAlgorithm());
		dod.writeUTF(k.getFormat());
		dod.writeInt(k.getEncoded().length);
		dod.write(k.getEncoded());
	}


	public static PublicKey readPublicKey(String k) throws IOException,
	NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] buff = base64.fromString(k);
		DataInputStream dis = new DataInputStream(
				new ByteArrayInputStream(buff));
		PublicKey pk = readPublicKey(dis);
		return pk;

	}

	/**
	 * @param dis
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static PublicKey readPublicKey(DataInputStream dis)
	throws IOException, NoSuchAlgorithmException,
	InvalidKeySpecException {
		String algo = dis.readUTF();
		@SuppressWarnings("unused")
		String format = dis.readUTF();
		int l = dis.readInt();
		byte[] pubKey = new byte[l];
		readFully(dis, pubKey);
		KeyFactory keyFactory = KeyFactory.getInstance(algo);
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKey);
		PublicKey pk = keyFactory.generatePublic(publicKeySpec);
		return pk;
	}

	public KeyPair read(char[] lp) throws IOException, GeneralSecurityException {
		//System.out.println("0");
		byte[] hpass =null;
		if (lp!=null) {
			String pass = new String(lp);
			hpass = encPass(pass);
		}

		FileInputStream fis = new FileInputStream(keystoreFile);
		DataInputStream dis = new DataInputStream(fis);
		// ////// Save encripting the key store file
		// will be: (UTF8) key type - (utf8) pub key encoding - (int) pub
		// key
		// length - (byte[]) pub key -
		// (byte[8]) init vector - (byte[16]) salt - (utf8) pub key encoding
		// - (int) priv key length - (byte[]) private key
		String keyType = dis.readUTF();
		@SuppressWarnings("unused")
		String enc = dis.readUTF();
		// System.out.println(enc);
		int l = dis.readInt();
		// System.out.println(l);
		byte[] pubKey = new byte[l];
		// read fully!!!
		//System.out.println("01");

		readFully(dis, pubKey);
		KeyFactory keyFactory = KeyFactory.getInstance(keyType);
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

		// read private key!!!!
		byte[] init = new byte[8];
		dis.read(init);
		byte[] salt = new byte[16];
		dis.read(salt);
		@SuppressWarnings("unused")
		String enc2 = dis.readUTF();
		int l2 = dis.readInt();
		byte[] privKey = new byte[l2];

		// now we create the key and read the private part
		if (lp!=null) {
			byte[] secret = PKCS5S2ParametersGenerator.generateDerivedKey(hpass,
					salt, PKCS5SimpleKeyStore.iterationCount, 16);
			segret sk = new segret(secret);
			Cipher blowFish = Cipher.getInstance("Blowfish/CFB/NoPadding");
			IvParameterSpec ivp = new IvParameterSpec(init);
			blowFish.init(Cipher.DECRYPT_MODE, sk, ivp);
			CipherInputStream cis = new CipherInputStream(dis, blowFish);
			readFully(cis, privKey);
		}
		else
			readFully(dis, privKey);
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		KeyPair kp = new KeyPair(publicKey, privateKey);
		return kp;
	}


	private static int iterationCount = 500;

	private static void readFully(InputStream is, byte[] out)
	throws IOException {
		int off = 0;
		int len = out.length;
		while (len > 0) {
			int count = is.read(out, off, len);
			if (count == -1)
				throw new EOFException();
			off += count;
			len -= count;
		}
	}

	private static byte[] encPass(String password) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
			md.update(password.getBytes());
			return md.digest();
		} catch (NoSuchAlgorithmException e2) {
			e2.printStackTrace();
		}
		return null;
	}

}

class segret implements SecretKey {
	public byte[] secret;

	public segret(byte[] s) {
		secret = s;
	}

	private static final long serialVersionUID = 1L;

	public byte[] getEncoded() {
		return secret;
	}

	public String getFormat() {
		return "RAW";
	}

	public String getAlgorithm() {
		return "Blowfish";
	}



}
