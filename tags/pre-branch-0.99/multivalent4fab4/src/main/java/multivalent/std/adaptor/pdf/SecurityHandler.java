package multivalent.std.adaptor.pdf;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;
import java.security.*;

import multivalent.ParseException;



/**
	Superclass for PDF security handler implementations.
	Encryption applies to all strings and streams in the document's PDF file, but not to other object types.

	@version $Revision: 1.6 $ $Date: 2003/08/17 08:40:17 $
*/
public abstract class SecurityHandler implements Cloneable {
  private static final /*const*/ Class[] SH_SIG = { COS.CLASS_DICTIONARY, COSSource.class };

  private static Map<String,String> handlers_ = new HashMap<String,String>(7);
  static {
	register("Standard", "multivalent.std.adaptor.pdf.SecurityHandlerStandard");
	//register("PublicKey", "multivalent.std.adaptor.pdf.EncryptPublicKey");
  }


  /**
	Registers the security handler <var>className</var>, which must be a subclass of Encrypt, for <var>filterName</var>.
	For example, Adobe's standard security handler is registered (automatically by the system),
	with the call <code>register("Standard", "multivalent.std.adaptor.pdf.SecurityHandlerStandard")</code>.
  */
  public static void register(String filterName, String className) {
	assert filterName!=null && className!=null;
	handlers_.put(filterName, className);
	// classname as String so class loaded on demand, which may be never
  }


  /**
	{@link java.lang.reflect.Constructor#newInstance(Object[])} for a description of the exceptions thrown.
	@throws UnsupportedOperationException if filter not registered
	@throws ClassNotFoundException if class name registered to filter is not the CLASSPATH
	@throws ParseException if can't read parameters for filter
  */
  public static SecurityHandler getInstance(String filter, Dict shdict, COSSource coss) {
	if ("Identity".equals(filter) || null==filter) return IDENTITY;

	SecurityHandler sh = null;
	String className = (String)handlers_.get(filter);
	//if (className==null) { System.out.println("unknown filter: "+filter); System.exit(1); }
	Exception e = null; String emsg = null;

	if (className == null) { emsg="Unregistered filter"; e = new UnsupportedOperationException(); }
	else try {
		Class cl = Class.forName(className);
		Constructor con = cl.getConstructor(SH_SIG);
		sh = (SecurityHandler)con.newInstance(new Object[] { shdict, coss });

// ParseException UnsupportedOperationException
	} catch (ClassNotFoundException cnfe) { emsg = "class "+className+" not found -- is it in CLASSPATH?"; e=cnfe;
	} catch (NoSuchMethodException nsme) { emsg = "need constructor "+className + "(Dict, COSSouce)"; e=nsme;
	} catch (IllegalAccessException iae) { emsg = className+" must be public"; e=iae;
	} catch (InstantiationException ie) { emsg = className+" must be public and non-abstract"; e=ie;
	} catch (InvocationTargetException ite) { emsg = "error in "+className+"'s constructor"; e=ite;
	}

	if (emsg!=null) {
		System.err.println("couldn't make security handler "+filter+": "+emsg);
		//e.printStackTrace(); System.exit(1);
		//throw e;
	}

	// null password is automatic
	boolean auth = sh.authOwner("") || sh.authUser("");
	//X boolean auth = sh.authUser("") || sh.authOwner("");	// special case: if user and owner null, then follow permissions for user.  Have to decrypt to gain owner access. => if you're serious then stop goofing around and set owner password
	// fast enough to automatically try all 1- and, if 40-bit, 2-letter passwords, without needing to ask
	//for (int letter=0; letter<256 && !encrypt_.isAuthorized(); letter++) setPassword(Character.toString((char)letter));

	return sh;
  }



  public static final SecurityHandler IDENTITY = new SecurityHandler(null, null) {
	public boolean authUser(String user) { return true; }
	public boolean authOwner(String owner) { return true; }
	public boolean isAuthorized() { return true; }
	public byte[] computeKey(String password) { return null/*""?*/; }
	public byte[] getKey() { return null/*new byte[0]? new byte[40]?*/; }

	public byte[] decrypt(byte[] data, int off, int len) { return data; }
	public byte[] encrypt(byte[] data, int off, int len) { return data; }
	public SecurityHandler reset(int objnum, int gennum) { return this; }

	public String toString() { return "IDENTITY"; }
  };



  public /*abstract--not in constructors*/ SecurityHandler(Dict edict, COSSource coss) {
	// maybe stash edict
  }

  public Object clone() {
	Object o = null;
	try { o = super.clone(); } catch (CloneNotSupportedException canthappen) {}
	return o;
  }

  /* Returns true iff <var>user</var> password is correct */
  public abstract boolean authUser(String user);
  /* Return true iff <var>owner</var> password is correct */
  public abstract boolean authOwner(String owner);
  /** Returns true if a valid decryption password, either user or owner, been set? */
  public abstract boolean isAuthorized();

  public abstract byte[] computeKey(String password);

  /** CryptFilter may take key from a security handler and use in Adobe algorithms. */
  public abstract byte[] getKey();

  /**
	Decrypts stream <var>data</var> of object <var>objnum</var> with generation <var>gennum</var>.
	Data is decrypted in place, which is to say the contents <var>data</var> are mutated.
	@return handle to decrypted data, which is same handle as the passed <var>data</var>.
  */
  public abstract byte[] decrypt(byte[] data, int off, int len);
  /**
	Encrypts <var>data</var> of object <var>objnum</var> with generation <var>gennum</var>.
	Data is encrypted in place, which is to say the contents <var>data</var> are mutated.
	@return handle to encrypted data, which is same handle as the passed <var>data</var>.
  */
  public abstract byte[] encrypt(byte[] data, int off, int len);

  /** Resets for decrypting/encrypting another subobject of object at given number. */
  public abstract SecurityHandler reset(int objnum, int gennum);
}
