package multivalent.std.adaptor.pdf;

import java.security.*;

import phelps.lang.StringBuffers;
import phelps.lang.Strings;

import multivalent.ParseException;

import com.pt.crypto.RC4;



/**
	Implements "standard encryption" defined in Adobe's PDF Reference.

	<p>Bit masks for permission bits are given with <code>PERM_</code> prefix.

	@version $Revision: 1.27 $ $Date: 2005/02/20 10:40:56 $
*/
public class SecurityHandlerStandard extends SecurityHandler {
  static final boolean DEBUG = false;

  // move permissions to superclass? COS?
  public static final int PERM_ALL = ~3;	// all 32 bits 1 except 0,1 (6&7 must be 1)
	//Integer.parseInt("111100111100",2);	// bits 2-5, 8-11 as of Revision 3
  public static final int PERM_NONE = (1<<6) | (1<<7);	// (6&7 must be 1)

  // 0,1 reserved (must be 0)	// Adobe counts bits starting with 1, not 0
  /** Permission: (Revision 2) Print the document.  (Revision 3) Print the document (possibly not at the highest quality level, depending on whether {@link #PERM_PRINT_GOOD} is also set. */
  public static final int PERM_PRINT = 1<<2;
  /** Permission: Modify the contents of the document by operations other than those controlled by {@link #PERM_ANNO}, {@link #PERM_FILL}, {@link #PERM_ASSEMBLE}. */
  public static final int PERM_MODIFY = 1<<3;
  /** Permission:
   (Revision 2) Copy or otherwise extract text and graphics from the document, including extracting text and graphics (in support of accessibility to disabled users or for other purposes).
   (Revision 3) Copy or otherwise extract text and graphics from the document by operations other than that controlled by {@link #PERM_COPY_R3}.
  */
  public static final int PERM_COPY = 1<<4;
  /** Permission: Add or modify text annotations, fill in interactive form fields, and, if {@link #PERM_MODIFY} is also set, create or modify interactive form fields (including signature fields). */
  public static final int PERM_ANNO = 1<<5;
  // 6, 7 reserved (must be 1)
  /** Permission: (Revision 3 only) Fill in existing interactive form fields (including signature fields), even if {@link #PERM_ANNO} is clear. */
  public static final int PERM_FILL = 1<<8;
  /** Permission: (Revision 3 only) Extract text and graphics (in support of accessibility to disabled users or for other purposes). */
  public static final int PERM_COPY_R3 = 1<<9;
  /** Permission: (Revision 3 only) Assemble the document (insert, rotate, or delete pages and create bookmarks or thumbnail images), even if {@link #PERM_MODIFY} is clear. */
  public static final int PERM_ASSEMBLE = 1<<10;
  /** Permission: (Revision 3 only) Print the document to a representation from which a faithful digital copy of the PDF content could be generated.  When this bit is clear (and {@link #PERM_PRINT} is set), printing is limited to a lowlevel representation of the appearance, possibly of degraded quality. */
  public static final int PERM_PRINT_GOOD = 1<<11;
  // 12-up reserved
  /** If the owner password was given, allow the document to be decrypted (extension to Adobe permission set). */
  public static final int PERM_DECRYPT = 1<<0;

  private static final byte[] PADDING = {
	(byte)0x28, (byte)0xBF, (byte)0x4E, (byte)0x5E, (byte)0x4E, (byte)0x75, (byte)0x8A, (byte)0x41, (byte)0x64, (byte)0x00, (byte)0x4E, (byte)0x56, (byte)0xFF, (byte)0xFA, (byte)0x01, (byte)0x08,
	(byte)0x2E, (byte)0x2E, (byte)0x00, (byte)0xB6, (byte)0xD0, (byte)0x68, (byte)0x3E, (byte)0x80, (byte)0x2F, (byte)0x0C, (byte)0xA9, (byte)0xFE, (byte)0x64, (byte)0x53, (byte)0x69, (byte)0x7A
  };


  private int Length_;
  /** Revision of security handler. */
  private int R_;
  /** Permissions. */
  private int perm_ = PERM_NONE;
  private int P_;
  private byte[] O_, U_;
  private byte[] ID0_;
  /** Key based in user password.  Extended for each object number-generation pair. */
  private byte[] key_ = null;
  private RC4 rc4_ = null;


  public SecurityHandlerStandard(final Dict edict, COSSource coss) throws ParseException {
	super(edict, coss);

	assert coss!=null && edict!=null;
	//if (encrypt == null) ... allow?  would just make everything no op

	Dict trailer = coss.getTrailer();

	Object o;
	Length_ = /*V_>=2 &&*/ (o=edict.get("Length"))!=null? ((Number)o).intValue(): 40;
	assert Length_>0 && (Length_ % 8) == 0: Length_;

	//edict_ = encrypt;
	R_ = ((Number)edict.get("R")).intValue();
	O_ = StringBuffers.getBytes8((StringBuffer)edict.get("O"));	//.toString().getBytes(); -- NO! character encoding tampers
	U_ = StringBuffers.getBytes8((StringBuffer)edict.get("U"));
	P_ = ((Number)edict.get("P")).intValue();
//System.out.println(this+": "+edict.get("R")+" => Length = "+Length_+" R="+R_);

	// ID is optional
	Object[] ID = (Object[])trailer.get("ID");  assert ID!=null;
	//if (ID==null) throw new ParseException("No /ID in trailer", -1);	// => make one up => if didn't exist, new one made by PDFReader
	ID0_ = StringBuffers.getBytes8((StringBuffer)ID[0]);

	validate();
  }

  /**
	Construct a Standard encryption filter with given parameters and passwords.
	Sets key for further encryption/decryption.
  */
  public SecurityHandlerStandard(StringBuffer id0,  int R, int P, int Length,  String userpassword, String ownerpassword) {
	super(null, null);

	Length_ = Length;
	R_ = R;	// needed to compute O, U
	P_ = P;

	ID0_ = StringBuffers.getBytes8(id0);	// needed to compute key

	O_ = StringBuffers.getBytes8(computeO(ownerpassword, userpassword));
	U_ = StringBuffers.getBytes8(computeU(userpassword));

	validate();

	key_ = computeKey(userpassword);
	assert authUser(userpassword);
  }

  public SecurityHandlerStandard(byte[] key) {
	super(null, null);
	key_ = key;
	Length_ = key_.length;
  }

  public Object clone() {
	SecurityHandlerStandard shs = (SecurityHandlerStandard)super.clone();
	rc4_ = null;	// no sharing
	return shs;
  }

  private void validate() {
	//if (V_!=1 && V_!=2 && V_!=4) throw new UnsupportedOperationException("Unsupported encryption algorithm: V = "+V_);	// V=0 is obsolete, V=3 is unpublished

	//if (V_ < 2) R_ = 2;
	if (!(2<=R_ && R_<=4)) throw new IllegalArgumentException("Invalid revision of Standard security handler "+R_);
//System.out.println("constructor: R="+R_+", V="+V_);

	assert O_.length==32 && U_.length==32: O_.length+", "+U_.length;
  }


  /** Returns length of encryption key. */
  public int getLength() { return Length_; }

  /** Returns version of standard security handler. */
  public int getR() { return R_; }

  /** Returns value of P field from dictionary. */
  public int getP() { return P_; }

  /**
	Returns permission bits in effect.
	If the password was the owner's, then this is identical to {@link #PERM_ALL};
	else if the password was the user's, then this is identical to the <code>/P</code> field;
	else if the password is invalid, then this is {@link #PERM_NONE}.
	If a bit position is 1 according to <code>PERM_</code> bitmask, the user has that permission.
	@see ContractualObligation
  */
  public int getPerm() { return perm_; }


  public byte[] getKey() { return key_; };

  public byte[] encrypt(byte[] data, int off, int len) { return decrypt(data, off,len); }	// RC4 is symmetric
  public byte[] decrypt(byte[] data, int off, int len) {
	rc4_.decrypt(data, off, len);
//System.out.println("SHS decrypt "+off+".. + "+len+": "+data[off]+" "+data[off+1]+" "+data[off+2]+" "+data[off+3]+" "+data[off+4]);
	return data;
  }

  public boolean isAuthorized() { return key_ != null; }

  public SecurityHandler reset(int objnum, int gennum) {
//System.out.println("SHS reset "+objnum+" "+gennum+", key = "+key_);
	// cheap to extend core key, and no cheap reset for RC4, so don't worry about cacheing
	int klen = key_.length;
	byte[] b = new byte[klen + 5];
	System.arraycopy(key_,0, b,0, klen);
	b[klen] = (byte)(objnum & 0xff);
	b[klen+1] = (byte)((objnum>>8) & 0xff);
	b[klen+2] = (byte)((objnum>>16) & 0xff);
	b[klen+3] = (byte)(gennum & 0xff);
	b[klen+4] = (byte)((gennum>>8) & 0xff);

	MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}
	md5.update(b);

	byte[] objkey = new byte[Math.min(klen+5,16)];
	System.arraycopy(md5.digest(),0, objkey,0, objkey.length);

	rc4_ = new RC4(objkey);

	return this;	// for chaining
  }



  // ADOBE ALGORITHMS
  /**
	Algorithm 3.1 Encryption of data using an encryption key.
  */
  public static byte[] encrypt(byte[] key,  byte[] data, int off, int len,  int objnum, int gennum) {
	assert key!=null && /*data!=null &&*/ objnum>=0 /*&& objnum<=pdf.getObjCnt()*/ && gennum>=0;
	//if (key==null) return data;

	SecurityHandler sh = new SecurityHandlerStandard(key);
	sh.reset(objnum, gennum);

	return sh.encrypt(data, off,len);
  }

  public byte[] computeKey(String password) {
	int keylen = R_==2? 5: R_>=3? Length_/8: -1;
	return computeKey(password, keylen);
  }

  /** Algorithm 3.2 Computing an encryption key. */
  public byte[] computeKey(String password, int keylen) {
	//assert password!=null;
	byte[] b = pad(password);

	MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}
	md5.update(b);

	md5.update(O_);

	for (int i=0, p=P_; i<4; i++, p >>= 8) md5.update((byte)p);
//System.out.println(Integer.toHexString(P));	for (int i=0, p=P; i<4; i++, p >>= 8) System.out.println(Integer.toHexString(((byte)p)&0xff)+" vs "+(p&0xff));

	b = md5.digest(ID0_);

	//FIX: if metadata not encrypted, for (int i=0; i<4; i++) md5.update(0xff);

	if (R_>=3) for (int i=0; i<50; i++) b = md5.digest(b);

	byte[] out = new byte[keylen];
	System.arraycopy(b,0, out,0, keylen);
//System.out.println("computed key of len "+keylen+" from |"+password+"|");
	return out;
  }


  /** Algorithm 3.2, Step 1. */
  protected static byte[] pad(String str) {
	//assert str!=null;
	byte[] b = new byte[32];

	if (str==null) str="";
	int len = Math.min(str.length(), 32);
	System.arraycopy(Strings.getBytes8(str),0, b,0, len);
	System.arraycopy(PADDING,0, b,len, 32-len);

	return b;
  }


  /** Algorithm 3.3 Computing the encryption dictionary'’s O (owner password) value. */
  public StringBuffer computeO(String owner, String user) {
	//if (owner!=null || owner.equals("")) owner = user; => for client to decide
	byte[] b = pad(owner);

	MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}
	b = md5.digest(b);

	if (R_>=3) for (int i=0; i<50; i++) b = md5.digest(b);

	int n = (R_==2? 5: R_>=3? Length_/8: -1);
	byte[] rc4key = new byte[n];
	System.arraycopy(b,0, rc4key,0, n);


	b = pad(user);

	RC4 rc4 = new RC4(rc4key);
	rc4.encrypt(b);

	if (R_>=3) {
		byte[] newkey = new byte[rc4key.length];
		for (int i=1; i<=19; i++) {
			for (int j=0,jmax=newkey.length; j<jmax; j++) newkey[j] = (byte)(rc4key[j] ^ i);
			RC4 newrc4 = new RC4(newkey);
			newrc4.encrypt(b);
		}
	}

	return StringBuffers.valueOf(b);
  }


  /**
	Algorithm 3.4 Computing the encryption dictionary's U (user password) value (Revision <em>2</em>), and
	Algorithm 3.5 Computing the encryption dictionary's U (user password) value (Revision <em>3</em>)
  */
  public StringBuffer computeU(String user) {
	byte[] key;

	if (R_==2) {
		byte[] rc4key = computeKey(user);

		key = (byte[])PADDING.clone();
		RC4 rc4 = new RC4(rc4key);
		rc4.encrypt(key);

	} else { assert R_>=3: R_;
		byte[] rc4key = computeKey(user);

		MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}
		md5.update(PADDING);

		byte[] b = md5.digest(ID0_);

		assert b.length == 16;
		RC4 rc4 = new RC4(rc4key);
		rc4.encrypt(b);

		byte[] newkey = new byte[rc4key.length];
		for (int i=1; i<=19; i++) {
			for (int j=0,jmax=newkey.length; j<jmax; j++) newkey[j] = (byte)(rc4key[j] ^ i);
			RC4 newrc4 = new RC4(newkey);
			newrc4.encrypt(b);
		}

		key = new byte[32];
		System.arraycopy(b,0, key,0, 16);
		System.arraycopy(PADDING,0, key,16, 16);
	}

	return StringBuffers.valueOf(key);
  }

  /**
	Algorithm 3.6 Authenticating the user password.
  */
  public boolean authUser(String user) {
	byte[] u = StringBuffers.getBytes8(computeU(user));

	boolean valid = true;
	for (int i=0; i<16; i++) if (u[i]!=U_[i]) { valid=false; break; }	// no Arrays.equals(byte[], byte[], int length)

	if (valid) {
//System.out.println("password |"+user+"| valid -- setting key");
		key_ = computeKey(user);
		perm_ = P_ & PERM_ALL;
	} // X else perm_ = PERM_NONE; => if had successful owner don't take away

	return valid;
  }

  /**
	Algorithm 3.7 Authenticating the owner password.
  */
  public boolean authOwner(String owner) {
	byte[] b = pad(owner);


	MessageDigest md5 = null; try { md5 = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException builtin) {}
	b = md5.digest(b);

	if (R_>=3) for (int i=0; i<50; i++) b = md5.digest(b);

	int n = R_==2? 5: R_>=3? Length_/8: -1;
	byte[] rc4key = new byte[n];
	System.arraycopy(b,0, rc4key,0, n);



	b = (byte[])O_.clone();

	if (R_==2) {
		RC4 rc4 = new RC4(rc4key);
		rc4.decrypt(b);

	} else { assert R_>=3;
		byte[] newkey = new byte[rc4key.length];
		for (int i=19; i>=0; i--) {
			for (int j=0,jmax=newkey.length; j<jmax; j++) newkey[j] = (byte)(rc4key[j] ^ i);
			RC4 rc4 = new RC4(newkey);
			rc4.decrypt(b);
		}
	}

	boolean fauth = authUser(Strings.valueOf(b));
	if (fauth) perm_ = PERM_ALL | PERM_DECRYPT;
	//X perm_ = fauth? PERM_ALL | PERM_DECRYPT: PERM_NONE; => additive
	return fauth;
  }
}
