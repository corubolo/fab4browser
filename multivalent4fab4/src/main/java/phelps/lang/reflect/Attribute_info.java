package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Attribute_info {
  public String attribute_name;  // u2 attribute_name_index;
  //Object o = value;
  public int attribute_offset;  //u4
  // u1 info[attribute_length];
  public Attribute_info(Cp_info[] cp, String name, int off) {
	attribute_name = name;
	attribute_offset = off; // length is first two byte at that offset
	//name = cp[readu2()].getString(cp);
	//len = readu4();
  }


  public String toString() { return attribute_name; }//+", len="+attribute_length; }
}
