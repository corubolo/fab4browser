package multivalent.std.adaptor.pdf;



/**
	Indirect references from one object to another, written like the following <code>15 0 R</code>.

	@version $Revision: 1.4 $ $Date: 2003/06/01 07:10:46 $
*/
public class IRef {
  public int id;
  public int generation;  // NEVER used -- could save 4 bytes per instance by using PDF.getObjGen(id)

  public IRef(int id, int gen) {
	this.id = id;
	this.generation = gen;
  }

  public IRef(IRef iref) {
	if (iref==null) { id=0; generation=0; }
	else { id=iref.id; generation=iref.generation; }
  }


  public boolean equals(Object o) {
	if (this==o) return true;
	if (!(o instanceof IRef)) return false;
	IRef iref = (IRef)o;
	return id==iref.id && generation==iref.generation;
  }

  public int hashCode() {
	 //return (id * 31)<<13 + generation;
	 return (generation<<20) + id;
  }

  public String toString() { return id+" "+generation+" R"; }
}
