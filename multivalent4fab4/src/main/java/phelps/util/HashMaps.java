package phelps.util;

import java.util.HashMap;



/**
	Extensions to {@link java.util.HashMap}.

*/
public class HashMaps {
  private HashMaps() {}


  /**
	Creates a new Map from array <var>nv</var>,
	where array holds <var>name1</var>, <var>value1</var>, <var>name2</var>, <var>value2</var>, ....
  */
  public static HashMap create(Object[] nv) {
	assert nv.length % 2 == 0;
	HashMap<Object,Object> map = new HashMap<Object,Object>(nv.length);
	for (int i=0,imax=nv.length; i<imax; i+=2) map.put(nv[i], nv[i+1]);
	return map;
  }
}
