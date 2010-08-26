package multivalent.std.adaptor.pdf;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import static multivalent.std.adaptor.pdf.COS.CLASS_ARRAY;



/**
	Encryption handler: instantiate subclass according to Filter.
	Encryption applies to all strings and streams in the document's PDF file, but not to other object types.

	@see SecurityHandlerStandard
	@see SecurityHandlerPublicKey

	@version $Revision: 1.13 $ $Date: 2003/08/29 04:00:10 $
*/
public class Encrypt {
  private COSSource coss_;

  private int V_ = -1;
  private Dict CF_ = null;

  private SecurityHandler sh_;
  private CryptFilter StmF_, StrF_;
  private Map<String,CryptFilter> cache_ = null;


  /**
	Constructs a new encryption object from parameters in an encryption dictionary.
  */
  public Encrypt(Dict edict, COSSource coss) {
	coss_ = coss;
	if (edict == null || edict.size()==0) {
		// no encryption
		sh_ = SecurityHandler.IDENTITY;
		StmF_ = StrF_ = CryptFilter.IDENTITY;

	} else {
		Object o = edict.get("V");
		V_ = o!=null? ((Number)o).intValue(): 0; assert V_>=0 && V_<=4: V_;

		String Filter = (String)edict.get("Filter"), SubFilter = (String)edict.get("SubFilter");
		assert Filter!=null;
		sh_ = SecurityHandler.getInstance(Filter, edict, coss);
		assert sh_!=null: edict;

		if (V_<4) {
			StrF_ = StmF_ = new CryptFilter(null, sh_);

		} else if (V_>=4) {
			CF_ = (o = edict.get("CF")) != null? (Dict)o: new Dict(5);
			/*for (Iterator<> i = CF_.entrySet().iterator(); i.hasNext(); ) {
				// collect parts before turning on encryption
			}*/

			cache_ = new HashMap<String,CryptFilter>(7);
			//cache_.put("Identity", CryptFilter.IDENTITY);     // set standard filters and at the same time disallow redefinition of standard
			StmF_ = getCryptFilter((String)edict.get("StmF")); 
			StrF_ = getCryptFilter((String)edict.get("StrF"));
		}
	}

// forall f in CF: if DocOpen then
	//getStrF().authOwner(""); getStrF().authUser("");

  }


  public CryptFilter getStmF() { return StmF_; }
  public CryptFilter getStrF() { return StrF_; }
  /**
	Returns SecurityHandler associated with document.
	It is unclear whether in PDF 1.5 a document can have more than one handler;
	if so this method will be removed in favor of such a method on CryptFilter.
  */
  public SecurityHandler getSecurityHandler() { return sh_; }

  public int getV() { return V_; }


  /** Returns crypt filter named <var>name</var>. */
  public CryptFilter getCryptFilter(String name) /*throws IOException*/ {
	if ("Identity".equals(name) || null==name) return CryptFilter.IDENTITY;

	CryptFilter cf = cache_.get(name); if (cf!=null) return cf;
	Dict cfdict = (Dict)CF_.get(name); assert cfdict!=null: name+" not i "+CF_;
	//if (cfdict==null) throw new Exception();
	/*
System.out.print("getCryptFilter "+name);
	Dict fulldict = new Dict(edict_);
	cfdict.putAll(fulldict);	// R, P can be inherited, apparently
System.out.println("name = "+cfdict+" => "+fulldict);
	SecurityHandler sh = SecurityHandler.getInstance((String)fulldict.get("Filter"), fulldict, null);
	assert sh_.isAuthorized();
	*/
	return new CryptFilter(cfdict, sh_);
  }

  /**
	Returns crypt filter to use for <var>stream</var>.
	If PDF version is >=1.5, recognizes <code>/Crypt</code> filter, if any.
  */
  public CryptFilter getCryptFilter(Dict stream) throws IOException {
	CryptFilter cf = getStmF();
	if (coss_.getVersion().compareTo(1,5)>=0) {
		Dict dp = (Dict)coss_.getDecodeParms(stream, "Crypt");
		String name = dp!=null? (String)coss_.getObject(dp.get("Name")): null;
		if (name!=null) cf = getCryptFilter(name);
	}
	return cf;
  }

  public String toString() { return getStrF()+" / "+getStmF(); }
}
