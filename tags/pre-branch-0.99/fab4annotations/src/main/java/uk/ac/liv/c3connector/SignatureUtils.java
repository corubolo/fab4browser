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

import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.Provider;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.ac.liv.c3connector.crypto.PKCS5SimpleKeyStore;


/**
 * This class contains different static methods used to check the validity of the digital signatures applied to the notes.
 * 
 * It also contains methods to create keystores and to sign annotations.
 * 
 * 
 * @author fabio
 *
 */
public class SignatureUtils {

	/**
	 * @param string
	 */
	static validationReturn validateSignature(String strdoc) throws Exception {

		//System.out.println(strdoc);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		//System.out.println(strdoc);
		org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(strdoc)));
		// Find Signature element
		NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0)
			return new validationReturn(0);

		String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());

		// Create a DOMValidateContext and specify a KeyValue KeySelector
		// and document context
		DOMValidateContext valContext = new DOMValidateContext(new KeyValueKeySelector(), nl.item(0));

		// unmarshal the XMLSignature
		XMLSignature signature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature (generated above)
		boolean coreValidity = signature.validate(valContext);

		if (coreValidity) {
			TrustedIdentity ti = SignatureUtils.trustedIdentityFor(signature.getKeySelectorResult().getKey());
			// System.out.println(ti);
			if (ti != null)
				return new validationReturn(1, ti);
			return new validationReturn(-2);
		}
		boolean sv = signature.getSignatureValue().validate(valContext);
		System.out.println("signature validation status: " + sv);
		// check the validation status of each Reference
		Iterator i = signature.getSignedInfo().getReferences().iterator();
		for (int j = 0; i.hasNext(); j++) {
			boolean refValid = ((Reference) i.next()).validate(valContext);
			System.out.println("ref[" + j + "] validity status: " + refValid);
		}
		System.out.println("bad signature!!!");
		return new validationReturn(-1);

	}

	/**
	 * Checks if the public key is one on the trusted...
	 * 
	 * @param key
	 *            the key we want to check
	 * @return the identity of the trusted person if trusted, otherwise null
	 */
	static TrustedIdentity trustedIdentityFor(Key key) {
		if (key.equals(DistributedPersonalAnnos.myTI.getPublicKey()))
			return DistributedPersonalAnnos.myTI;
		for (TrustedIdentity ti : DistributedPersonalAnnos.trusted)
			if (key.equals(ti.getPublicKey()))
				return ti;
		return null;
	}

	/** Creates the main keystore  for the user identity
	 * 
	 * */
	static KeyPair createKeystore() {
		int keysize = 512;
		char[] lp = DistributedPersonalAnnos.dafPass;
		String keyType = "DSA";
		PKCS5SimpleKeyStore ks1 = new PKCS5SimpleKeyStore(DistributedPersonalAnnos.defksfile);
		try {
			DistributedPersonalAnnos.kp = ks1.generateKeys(keysize, lp, keyType);
			return DistributedPersonalAnnos.kp;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	static String sign(String stringAnno) throws Exception {

		// Create a DOM XMLSignatureFactory that will be used to generate the
		// enveloped signature
		String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
		// Create a Reference to the enveloped document (in this case we are
		// signing the whole document, so a URI of "" signifies that) and
		// also specify the SHA1 digest algorithm and the ENVELOPED Transform.
		Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac
				.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

		// Create the SignedInfo
		SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
				(C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.DSA_SHA1, null), Collections
				.singletonList(ref));

		// Create a KeyValue containing the DSA PublicKey that was generated
		KeyInfoFactory kif = fac.getKeyInfoFactory();
		KeyValue kv = kif.newKeyValue(DistributedPersonalAnnos.kp.getPublic());

		// Create a KeyInfo and add the KeyValue to it
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

		// Instantiate the document to be signed
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(stringAnno)));

		// Create a DOMSignContext and specify the DSA PrivateKey and
		// location of the resulting XMLSignature's parent element
		DOMSignContext dsc = new DOMSignContext(DistributedPersonalAnnos.kp.getPrivate(), doc.getDocumentElement());

		// Create the XMLSignature (but don't sign it yet)
		XMLSignature signature = fac.newXMLSignature(si, ki);

		// Marshal, generate (and sign) the enveloped signature
		signature.sign(dsc);

		// output the resulting document
		StringWriter sw = new StringWriter();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.getBuffer().toString();
	}

}

class SimpleKeySelectorResult implements KeySelectorResult {
	private PublicKey pk;

	SimpleKeySelectorResult(PublicKey pk) {
		this.pk = pk;
	}

	public Key getKey() {
		return pk;
	}
}

class KeyValueKeySelector extends KeySelector {
	static boolean algEquals(String algURI, String algName) {
		if (algName.equalsIgnoreCase("DSA") && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
			return true;
		else if (algName.equalsIgnoreCase("RSA") && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1))
			return true;
		else
			return false;
	}

	@Override
	public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method,
			XMLCryptoContext context) throws KeySelectorException {
		if (keyInfo == null)
			throw new KeySelectorException("Null KeyInfo object!");
		SignatureMethod sm = (SignatureMethod) method;
		List list = keyInfo.getContent();

		for (int i = 0; i < list.size(); i++) {
			XMLStructure xmlStructure = (XMLStructure) list.get(i);
			if (xmlStructure instanceof KeyValue) {
				PublicKey pk = null;
				try {
					pk = ((KeyValue) xmlStructure).getPublicKey();
				} catch (KeyException ke) {
					throw new KeySelectorException(ke);
				}
				// make sure algorithm is compatible with method
				if (algEquals(sm.getAlgorithm(), pk.getAlgorithm()))
					return new SimpleKeySelectorResult(pk);
			}
		}
		throw new KeySelectorException("No KeyValue element found!");
	}
}

class validationReturn {
	int returnValue;

	TrustedIdentity ti = null;

	/**
	 * @param returnValue
	 */
	public validationReturn(int returnValue) {
		super();
		this.returnValue = returnValue;
	}

	/**
	 * @param returnValue
	 * @param ti
	 */
	public validationReturn(int returnValue, TrustedIdentity ti) {
		super();
		this.returnValue = returnValue;
		this.ti = ti;
	}
}