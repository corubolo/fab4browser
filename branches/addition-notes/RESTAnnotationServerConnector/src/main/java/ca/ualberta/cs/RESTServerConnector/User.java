package ca.ualberta.cs.RESTServerConnector;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

//import ca.ualberta.cs.PrivacyManagement.Encryptor;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class User {

	private boolean anonymizeData = false;
	public String username, name, affiliation, description;
	public int id;
	private String password;
	public String email; //TODO check for validity of email
	
	public User(String u, String p, String n, String e, String a, String d){
		username = u;
		setPassword(p);
		setName(n);
		setAffiliation(a);		
		setEmail(e);
		description = d;
	}
	
	public User(String u, String p, String n, String e, String a){
		username = u;
		setPassword(p);
		setName(n);
		setAffiliation(a);		
		setEmail(e);
	}
	
	public User(String u, String p, String n, String e){
		username = u;
		setPassword(p);
		setName(n);
		setEmail(e);
	}
		
	public User(){
		
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = encryptOrAnonymize(password);
	}
	
		public String getName() {
		return name;
	}
	public void setName(String name) {
		if(anonymizeData)
			this.name = encryptOrAnonymize(name);
		else
			this.name = name;
	}
	
	public String getAffiliation() {
		return affiliation;
	}
	public void setAffiliation(String affiliation) {
		if(anonymizeData)
			this.affiliation = encryptOrAnonymize(affiliation);
		else
			this.affiliation = affiliation;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) { //we need email to send regenerated passwords!
		/*if(anonymizeData)
			this.email = encryptOrAnonymize(email);
		else*/
			this.email = email;
	}
	
	public int getId(){
		return id;
	}
	
	public void setId(int ii){
		id = ii;
	}
	
	
	private String encryptOrAnonymize(String origVal) {
//		return Encryptor.encryptOrAnonymize(origVal);
		return origVal;
	}

	
	/*public Integer getId() {
	return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}*/
}
