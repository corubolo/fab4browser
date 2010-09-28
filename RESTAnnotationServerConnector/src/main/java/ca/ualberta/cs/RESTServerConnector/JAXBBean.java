package ca.ualberta.cs.RESTServerConnector;

import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("restriction")
@XmlRootElement
public class JAXBBean {
	
	String key;
	String val;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key1) {
		this.key = key1;
	}
	public String getVal() {
		return val;
	}
	public void setVal(String val1) {
		this.val = val1;
	}
	
	
}
