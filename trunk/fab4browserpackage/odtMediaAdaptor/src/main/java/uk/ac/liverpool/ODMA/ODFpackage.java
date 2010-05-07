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
package uk.ac.liverpool.ODMA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.base64;


/**
 * 
 * Useful class to represent and use the ODF package format. Can be created with
 * an InputStream or with a File (if on local FS). Uses ZipFile falcilities from
 * Java, but takes into account the different peculiarities of the ODF packages
 * (manifest, encryption,mime type stream etc...) It parses the
 * 
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * ODFpackage
 */
public class ODFpackage {
	public static final String MANIFEST_ID = "META-INF/manifest.xml";

	public static final String THUMBTAIL_ID = "Thumbnails/thubmnail.png";

	public static final String SIGNATURES_ID = "META-INF/documentsignatures.xml";

	/*
	 * TODO: signatures verify them!
	 */

	//	public static final File cachedir = new File(new File(System
	//			.getProperty("user.home"), ".Multivalent"), "general");

	static int BUFFER_SIZE = 4096;

	// no use of a jar file since manifest file is in adifferent format
	private ZipFile zipFile = null;

	private Document manifest = null;

	/** digest of the user password */
	private byte[] pass = null;

	/**
	 * Initiates the ZipFile variable used for Zip (.odt) file extraction later
	 * on 1st checks if a cache copy is needed or if file is in the local file
	 * system (file:/)
	 * 
	 * @throws IOException
	 */
	public ODFpackage(InputStream is) throws IOException {

		byte[] buffer = new byte[ODFpackage.BUFFER_SIZE];
		File f = File.createTempFile("odf", "odt");
		OutputStream os = new FileOutputStream(f);
		int r;
		while ((r = is.read(buffer)) > 0)
			os.write(buffer, 0, r);
		os.close();
		f.deleteOnExit();
		zipFile = new ZipFile(f, ZipFile.OPEN_READ);
		parseManifest();
	}

	public ODFpackage(File f) throws IOException {
		zipFile = new ZipFile(f, ZipFile.OPEN_READ);
		parseManifest();
	}

	/**
	 * 
	 * Parses the manifest file, important in the case of encrypted files. Other
	 * information include mime type for files. I am not sure this will be very
	 * useful, but we'll see Will have to take care of encryption later on, for
	 * now at least want to recognise it. All exceptions but the IO ones are not
	 * thrown... flexible
	 * 
	 * @throws IOException
	 */
	private void parseManifest() throws IOException {
		ZipEntry e = zipFile.getEntry(ODFpackage.MANIFEST_ID);
		InputStream is = zipFile.getInputStream(e);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setValidating(false);
			// dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
			// dbf.setSchema(null);

			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
				return;
			}
			/**
			 * HACK: I have to set my own Entity resolver: for some reason, OO
			 * 2.0beta Mainfest points to a non existing DTD, so generates a
			 * FileNotFoundException, which actually stops the parser. I guess
			 * DTD is needed to resolve entities so java xml lib generates a
			 * fatal non recoverable error in the case the dtd does not exist.
			 * or it' a bug? A more flexible solution would be better... NOTE:
			 * with java 1.4 I get the error anyway FIX: We simply ignore. Also
			 * using method in http://books.evc-cit.info/oobook/apc.html I get
			 * error in 1.4 so I guess there is not much choice
			 */

			db.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId,
						String systemId) {
					if (systemId.endsWith(".dtd")) {
						StringReader stringInput = new StringReader(" ");
						return new InputSource(stringInput);
					}
					return null; // default behavior
				}
			});

			manifest = db.parse(is);

		} catch (IOException er) {
			throw er;
		} catch (Exception ed) {
			System.out.println("!MANIFEST ERROR!");
			ed.printStackTrace();
		}

	}

	/**
	 * Gets the InputStream for a file. Takes into account the manifest for
	 * encryption, throwing an exceprion fi the file is encrypred.
	 * 
	 * @return the input stream for the file
	 * @throws IOException
	 * @throws EncrypredPackageException
	 */
	public InputStream getFileIS(String id) throws IOException,
	EncrypredPackageException {
		// 1st I want to check the Manifest to see if the file is encrypted...
		// (if I am not in the manifest :)
		if (manifest != null) {
			NodeList file = manifest.getDocumentElement().getChildNodes();
			for (int i = 0; i < file.getLength(); i++)
				// for all the file entryes in the manifest
				if (file.item(i).getNodeName().equals("manifest:file-entry")) {
					NamedNodeMap att = file.item(i).getAttributes();
					Node filename = att.getNamedItem("manifest:full-path");
					// Node filetype = att.getNamedItem("manifest:media-type");
					// System.out.println(filetype.getNodeValue());
					// get to the requested file
					if (id.equals(filename.getNodeValue())) {
						NodeList encs = file.item(i).getChildNodes();
						for (int k = 0; k < encs.getLength(); k++)
							if (encs.item(k).getNodeName().equals(
							"manifest:encryption-data")) {
								if (pass == null)
									throw new EncrypredPackageException();
								// so now: got the password, got the encrypted
								// file.
								// I have to decrypt
								ZipEntry e = zipFile.getEntry(id);
								InputStream is = decript(zipFile
										.getInputStream(e), encs.item(k));
								return is;
							}
					}
				}
		}
		ZipEntry e = zipFile.getEntry(id);
		return zipFile.getInputStream(e);
	}

	/**
	 * To obtain the mime type specified it the manifest file.
	 * 
	 * @param id
	 *            The file path
	 * @return mime type as specified in the Manifest
	 */
	public String getFileMime(String id) {
		// 1st I want to check the Manifest to see if the file is encrypted...
		// (if I am not in the manifest :)
		if (manifest != null) {
			NodeList file = manifest.getDocumentElement().getChildNodes();
			for (int i = 0; i < file.getLength(); i++)
				// for all the file entryes in the manifest
				if (file.item(i).getNodeName().equals("manifest:file-entry")) {
					NamedNodeMap att = file.item(i).getAttributes();
					Node filename = att.getNamedItem("manifest:full-path");
					Node filetype = att.getNamedItem("manifest:media-type");
					if (id.equals(filename.getNodeValue()))
						return filetype.getNodeValue();
				}
		}
		return "";
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



	private InputStream decript(InputStream is, Node encData) {
		NodeList nl = encData.getChildNodes();
		@SuppressWarnings("unused")

		byte[] checksum = base64.fromString(encData.getAttributes().getNamedItem(
		"manifest:checksum").getNodeValue());
		byte[] initVector = null;
		byte[] salt = null;
		int iterCount = 0;
		/*
		 * <manifest:file-entry manifest:media-type="text/xml"
		 * manifest:full-path="content.xml" manifest:size="2907">
		 * <manifest:encryption-data manifest:checksum-type="SHA1/1K"
		 * manifest:checksum="k5pFUXpMMOwtLfPOgrZxTlxq7EQ="> <manifest:algorithm
		 * manifest:algorithm-name="Blowfish CFB"
		 * manifest:initialisation-vector="nYm1j/IwZII="/>
		 * <manifest:key-derivation manifest:key-derivation-name="PBKDF2"
		 * manifest:iteration-count="1024"
		 * manifest:salt="ett/hoDquWVjylIHqYUd1A=="/>
		 */
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName() == "manifest:algorithm")
				initVector = base64.fromString(n.getAttributes().getNamedItem(
				"manifest:initialisation-vector").getNodeValue());
			else if (n.getNodeName() == "manifest:key-derivation") {
				salt = base64.fromString(n.getAttributes().getNamedItem(
				"manifest:salt").getNodeValue());
				iterCount = Integer.parseInt(n.getAttributes().getNamedItem(
				"manifest:iteration-count").getNodeValue());
			}
		}
		if (salt == null || iterCount == 0)
			return null;

		try {
			byte[] password = new byte[pass.length];
			System.arraycopy(pass, 0, password, 0, pass.length);
			byte[] secret = PKCS5S2ParametersGenerator.generateDerivedKey(
					password, salt, iterCount, 16);
			segret sk = new segret(secret);
			Cipher blowFish = Cipher.getInstance("Blowfish/CFB/NoPadding");
			IvParameterSpec ivp = new IvParameterSpec(initVector);
			blowFish.init(Cipher.DECRYPT_MODE, sk, ivp);
			CipherInputStream cis = new CipherInputStream(is, blowFish);
			// DigestInputStream dis = new
			// DigestInputStream(cis,MessageDigest.getInstance("SHA1"));
			PushbackInputStream pis = new PushbackInputStream(cis, 512);
			InputStream zis = new InflaterInputStream(pis, new Inflater(true),
					512);
			// File temp = LivUtils.copyToTemp(zis);
			// byte[] cchecksum = dis.getMessageDigest().digest();
			// for (int i=0;i<checksum.length;i++)
			// System.out.println("-"+checksum[i]+"="+cchecksum[i]);
			return zis;

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		/*
		 * The encryption process takes place in the following multiple stages:
		 * 1. A 20-byte SHA1 digest of the user entered password is created and
		 * passed to the package component. 2. The package component initializes
		 * a random number generator with the current time. 3. The random number
		 * generator is used to generate a random 8-byte initialization vector
		 * and 16-byte salt for each file. 4. This salt is used together with
		 * the 20-byte SHA1 digest of the password to derive a unique 128-bit
		 * key for each file. The algorithm used to derive the key is PBKDF2
		 * using HMAC-SHA- 1 (see [RFC2898]) with an iteration count of 1024. 5.
		 * The derived key is used together with the initialization vector to
		 * encrypt the file using the Blowfish algorithm in cipher-feedback
		 * (CFB) mode. Each file that is encrypted is compressed before being
		 * encrypted. To allow the contents of the package file to be verified,
		 * it is necessary that encrypted files are flagged as 'STORED' rather
		 * than 'DEFLATED'. As entries which are 'STORED' must have their size
		 * equal to the compressed size, it is necessary to store the
		 * uncompressed size in the manifest. The compressed size is stored in
		 * both the local file header and central directory record of the Zip
		 * file.
		 */
		return null;
	}

	public BufferedImage getThumbnail() throws IOException,
	EncrypredPackageException {
		return ImageIO.read(getFileIS(ODFpackage.THUMBTAIL_ID));
	}

	/**
	 * Stores the plain password for the package (performs SHA1 of the string
	 * and stores it)
	 */
	public void setPassword(String p) {
		pass = encPass(p);
	}

	/** stores the digested password (p MUST be a 20 byte SHA1 of the pass) */
	public void setPassword(byte[] p) {
		pass = p;
	}


	private class segret implements SecretKey {
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
}
