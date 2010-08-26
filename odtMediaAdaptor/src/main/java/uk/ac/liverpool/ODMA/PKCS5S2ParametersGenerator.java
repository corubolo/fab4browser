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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Generator for PBE derived keys and ivs as defined by PKCS 5 V2.0 Scheme 2.
 * This generator uses a SHA-1 HMac as the calculation function.
 * <p>
 * The document this implementation is based on can be found at
 * <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html>
 * RSA's PKCS5 Page</a>
 */
public class PKCS5S2ParametersGenerator extends PBEParameterSpec {
	/**
	 * @param arg0
	 * @param arg1
	 */
	public PKCS5S2ParametersGenerator(byte[] arg0, int arg1) {
		super(arg0, arg1);
	}

	//private

	private static void F(final byte[] P, byte[] S, int c, // iter count
			byte[] iBuf, byte[] out, int outOff, Mac hMac) throws InvalidKeyException,
			ShortBufferException, IllegalStateException {
		byte[] state = new byte[hMac.getMacLength()];
		//hMac.reset();
		SecretKey pass = new SecretKey() {
			private static final long serialVersionUID = 1L;
			public byte[] getEncoded() {
				return P;
			}
			public String getFormat() {
				return "RAW";
			}
			public String getAlgorithm() {
				return "HmacSHA1";
			}
		};
		hMac.init(pass);
		//hMac.update(P);
		if (S != null)
			hMac.update(S);
		hMac.update(iBuf);
		hMac.doFinal(state, 0);
		//hMac.reset();
		System.arraycopy(state, 0, out, outOff, state.length);
		if (c == 0)
			throw new IllegalArgumentException(
			"iteration count must be at least 1.");
		for (int count = 1; count < c; count++) {
			//hMac.init(param);
			hMac.update(state);
			hMac.doFinal(state, 0);
			for (int j = 0; j != state.length; j++)
				out[outOff + j] ^= state[j];
		}
	}
	private static void intToOctet(byte[] buf, int i) {
		buf[0] = (byte) (i >>> 24);
		buf[1] = (byte) (i >>> 16);
		buf[2] = (byte) (i >>> 8);
		buf[3] = (byte) i;
	}

	public static byte[] generateDerivedKey(byte[] password, byte[] salt,
			int iterationCount, int dkLen) throws NoSuchAlgorithmException {
		Mac hMac;
		hMac = Mac.getInstance("HmacSHA1");
		int hLen = hMac.getMacLength();
		int l = (dkLen + hLen - 1) / hLen;
		byte[] iBuf = new byte[4];
		byte[] out = new byte[l * hLen];
		for (int i = 1; i <= l; i++) {
			intToOctet(iBuf, i);
			try {
				F(password, salt, iterationCount, iBuf, out, (i - 1) * hLen, hMac);
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (ShortBufferException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		byte[] ret = new byte[dkLen];
		System.arraycopy(out, 0, ret, 0, dkLen);
		return ret;
	}


}
