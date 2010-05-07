package multivalent.std.adaptor.pdf;

import java.security.*;

import multivalent.ParseException;



/**
	NOT IMPLEMENTED.
	Public Key encryption, as defined in Adobe's PDF Reference.

	@version $Revision: 1.2 $ $Date: 2003/08/17 08:40:57 $
*/
public class SecurityHandlerPublicKey extends SecurityHandler {
  static final boolean DEBUG = false;

  /** Revision of security handler. */
  private int R_;
  /** Permissions. */
  private Object[] Recipients_;
  /** Key based in user password.  Extended for each object number-generation pair. */
  private byte[] key_ = null;


  public SecurityHandlerPublicKey(Dict edict, COSSource coss) throws ParseException {
	super(edict, coss);

	assert edict!=null && coss!=null;
	//if (encrypt == null) ... allow?  would just make everything no op

	R_ = ((Number)edict.get("R")).intValue();
//System.out.println("P_ = "+Integer.toHexString(P_));

	validate();
  }

  /**
	Construct a Standard encryption filter with given parameters and passwords.
	Sets key for further encryption/decryption.
  */
  public SecurityHandlerPublicKey(StringBuffer id0,  int R, int P, int Length,  String userpassword, String ownerpassword) throws UnsupportedOperationException, IllegalArgumentException {
	super(null, null);
	//Filter_ = "PublicKey";
	//V_ = V;
	//Length_ = Length;
	R_ = R;     // needed to compute O, U

	validate();

	key_ = computeKey(userpassword);
	assert authUser(userpassword);
  }

  private void validate() throws UnsupportedOperationException, IllegalArgumentException {
	//if (V_!=1 && V_!=2 && V_!=4) throw new UnsupportedOperationException("Unsupported encryption algorithm: V = "+V_);     // V=0 is obsolete, V=3 is unpublished

	//if (V_ < 2) R_ = 2;
	//if (!(2<=R_ && R_<=4)) throw new IllegalArgumentException("Invalid revision of Standard security handler "+R_);
//System.out.println("constructor: R="+R_+", V="+V_);
  }

  public byte[] getKey() { return key_; }


  /** Returns version of standard security handler. */
  public int getR() { return R_; }

  public boolean isAuthorized() { return key_ != null; }



  public byte[] encrypt(byte[] data, int off, int len) {
	assert key_!=null;
	return null;
  }
  public byte[] decrypt(byte[] data, int off, int len) {
	assert key_!=null;
	return null;
  }

  public SecurityHandler reset(int objnum, int gennum) { return this; }

  public boolean authUser(String user) { return false; }
  public boolean authOwner(String owner) { return false; }

  public byte[] computeKey(String password) {
	return null;
  }
}
