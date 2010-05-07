package multivalent.std.adaptor.pdf;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import phelps.lang.StringBuffers;
import phelps.lang.Maths;

import com.pt.crypto.RC4;



/**
	Crypt filter on individual COS streams (introduced in PDF 1.5).

	@version $Revision: 1.6 $ $Date: 2003/08/17 08:39:54 $
*/
public class CryptFilter extends FilterInputStream {
  private SecurityHandler sh_;
  private String CFM_ = "None";
  //private int Length_;
  private byte[] key_ = null;

  // as InputStream
  private byte[] buf_ = null;
  private int bufi_ = 0, len_ = -1;


  public static final CryptFilter IDENTITY = new CryptFilter(null, SecurityHandler.IDENTITY);

  /**
	Creates instance to use for crypting raw buffers and strings,
	which is the usual case.
	The SecurityHandler does not need to be authorized at this time (but of course it must before decrypt()/encrypt()).
  */
  /*package private for Encrypt*/ CryptFilter(Dict parms, SecurityHandler sh) /*throws IOException*/ {
	super(null/*explicitly allowed*/);

	sh_ = sh;	// NOT .clone() here

	if (parms!=null) {
		assert parms.get("Type")==null || "CryptFilter".equals(parms.get("Type"));

		Object o = parms.get("CFM");
		CFM_ = o!=null? (String)o: "None"; assert "None".equals(CFM_) || "V2".equals(CFM_);
		if ("V2".equals(CFM_)) {
			int Length = (o = parms.get("Length")) instanceof Number? ((Number)o).intValue(): 128;
			Length = Maths.minmax(40, Length, /*128*/1024*3);	// isaacs has invalid 16 (not between 40 and 128), which we ignore
			//System.out.println("CFM V2, Length = "+Length_);

			key_ = sh_.getKey();
			/*
			int bc = Length_/8;
			if (false && key_.length != bc) {
				System.out.println("shortening key to "+bc+" bytes");
				byte[] b = key_; key_ = new byte[bc]; System.arraycopy(b,0, key_,0, bc);
			}*/
			sh_ = new SecurityHandlerStandard(key_);   // decrypt correct since symmetric RC4
			
		} else { assert "None".equals(CFM_);
		}
	}
	//String AuthEvent = ((o=dict.get("AuthEvent")))!=null? (String)AuthEvent: "DocOpen";
  }

  // one SecurityHandler for entire document?
  /*public*/private SecurityHandler getSecurityHandler() { return sh_; }


  /** Decrypts directly using filter's security handler. */
  public byte[] decrypt(byte[] buf, int off, int len) {
	assert buf!=null && off>=0 && len>=0 && off+len<=buf.length;

	sh_.decrypt(buf, off, len);
	return buf;
  }

  /** Encrypts directly using filter's security handler. */
  public byte[] encrypt(byte[] buf, int off, int len) {
	assert buf!=null && off>=0 && len>=0 && off+len<=buf.length;
	sh_.encrypt(buf, off, len);
	return buf;
  }

  /** Decrypts PDF string in <var>sb</var>.  Mutates <var>sb</var>. */
  public void decrypt(StringBuffer sb) {
	assert sb!=null;

	if (SecurityHandler.IDENTITY==getSecurityHandler()) return;

	// Sidestep disasterous encodings that happen with new String(byte[]) and new String.getBytes().
	//   can't do sb.toString().getBytes() because deprecated or modified through a character encoding
	//   you'd think that new String(byte[]).getBytes() would give back the original byte[], but it doesn't, alas
	byte[] b = StringBuffers.getBytes8(sb);
	b = decrypt(b, 0,b.length);
	for (int i=0,imax=sb.length(); i<imax; i++) sb.setCharAt(i, (char)(b[i]&0xff));
  }

  /** Encrypts PDF string in <var>sb</var>.  Mutates <var>sb</var>. */
  public void encrypt(StringBuffer sb) {
	assert sb!=null;

	if (SecurityHandler.IDENTITY==getSecurityHandler()) return;

	byte[] b = StringBuffers.getBytes8(sb);
	b = encrypt(b, 0,b.length);
	for (int i=0,imax=sb.length(); i<imax; i++) sb.setCharAt(i, (char)(b[i]&0xff));
  }

  public CryptFilter reset(int objnum, int gennum) {
	assert objnum>=1 /*&& objnum<=pdf.getObjCnt() -- no hook*/ && gennum>=0: objnum;

	sh_.reset(objnum, gennum); 
	return this;
  }


  /**
	Creates instance to use as a {@link FilterInputStream},
	which is only needed when the crypt filter is not the first filter,
	which is rare.
  */
  public CryptFilter(CryptFilter cf, InputStream in, int objnum, int gennum) throws IOException {
	super(in);
	buf_ = new byte[8*1024];
	len_ = 0;
	//objnum_=objnum; gennum_=gennum;

	sh_ = (SecurityHandler)cf.sh_.clone();	// no sharing because might want to decrypt another object before finished with current
	CFM_ = cf.CFM_;
	key_ = cf.key_;

//System.out.println("reset on "+objnum);
	sh_.reset(objnum, gennum);
  }

  public int read(byte[] b, int off, int len) throws IOException {
	if (len_==-1) return -1;
	else if (bufi_ < len_) { int hunk = Math.min(len, len_-bufi_); System.arraycopy(buf_,bufi_, b,off, hunk); bufi_+=hunk; return hunk; }
	else { fill(); return read(b, off, len); }
  }

  public int read() throws IOException {
	if (len_==-1) return -1;
	else if (bufi_ < len_) return buf_[bufi_++] & 0xff;
	else { fill(); return read(); }
  }

  private void fill() throws IOException {
	/*while (len_>=0)*/ len_ = in.read(buf_);
	//System.out.println("CF decrypting 0.."+len_);
	//for (int i=0; i<10/*len_*/; i++) System.out.print(Integer.toHexString(buf_[i]&0xff)+" ");  System.out.println();
//System.out.println("decrypted 0.."+len_);
	if (len_ != -1) decrypt(buf_, 0,len_);
	bufi_ = 0;
  }

  public boolean markSupported() { return false; }
  public void close() throws IOException { len_=-1; super.close(); }

  public String toString() { return getSecurityHandler().toString(); }
}
