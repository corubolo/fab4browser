package ca.ualberta.cs.RESTServerConnector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import uk.ac.liv.c3connector.AnnotationModelSerialiser;
import uk.ac.liv.c3connector.DistributedPersonalAnnos;
import uk.ac.liv.c3connector.JDomAnnotationModelSerialiser;
import uk.ac.liverpool.annotationConnector.AnnotationServerConnectorInterface;



public class RESTAnnotationServer implements AnnotationServerConnectorInterface{

	static boolean verify = false;
	private AnnotationModelSerialiser ams = new JDomAnnotationModelSerialiser();
	
	private ClientConfig cc = new DefaultClientConfig(); //TODO, is default good?
	private Client client = Client.create(cc);
	private WebResource publishWR = client.resource(DistributedPersonalAnnos.publishServiceURL);	
	private WebResource searchWR = client.resource(DistributedPersonalAnnos.searchServiceURL);
	private WebResource searchChecksumWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/checksum");
	private WebResource updateWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/update");
	private WebResource searchBibWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/sameBib");
	private WebResource delWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/del");
	private WebResource usersAuthWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/users/auth");
	private WebResource usersAddWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/users/add");
	private WebResource resourcesAddWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/resources/add");
	private WebResource resourceBibInfoWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/resources/info/needed");
	private WebResource resourceBibInfoUpdateWR = client.resource(DistributedPersonalAnnos.publishServiceURL+"/resources/info/update");
	private WebResource searchTagsWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/allTags");
	
	public String IDSearch(String ID) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] URISearch(String URI) {
		//List<JAXBBean> response = searchWR.type("text/plain").accept("application/json").post(List.class, URI);
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
        Collection<JAXBBean> response = searchWR.type("text/plain").accept("application/json").post(genericXmlType, URI);
		//GenericEntity<List<JAXBBean>> (reqArr){}
		List<JAXBBean> res = (List<JAXBBean>) response;
		String[] annos = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			annos[i] = res.get(i).getKey();
		return annos;
	}

	public String[] checksumSearch(String checksum, String method,
			String encoding) {
		
		List<JAXBBean> reqArr = wrapRequest("checksum", checksum, "method", method, "encoding", encoding);
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
        Collection<JAXBBean> response = searchChecksumWR.type("application/json").accept("application/json").post(genericXmlType, new GenericEntity<List<JAXBBean>> (reqArr){});
		//GenericEntity<List<JAXBBean>> (reqArr){}
		List<JAXBBean> res = (List<JAXBBean>) response;
		String[] annos = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			annos[i] = res.get(i).getKey();
		return annos;
	}

	public String[] customIdSearch(String costomId, String application,
			String method) {
		// TODO Auto-generated method stub
		return checksumSearch(costomId, application, method); //TODO! change it!
	}

	public int deleteAnnotation(String id, String user, String secret) { //deletebyFabId
		
		//assuming Fabid is not convertible to integer (since it is a concatination of userid and fabid
		try{
			Integer annoid = Integer.parseInt(id);
			//no exception, so it is unique id
			return deleteAnnotationByUniqueId(annoid, user, secret);
		}
		catch(Exception e){ //if id is fabid
			List<JAXBBean> reqArr = wrapRequest("annoIdFab", id, "username", user, "secret", secret);
			String response = (String) delWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
			return Integer.parseInt(response);
		}
	}	
	
	private int deleteAnnotationByUniqueId(Integer id, String user, String secret) { //deleteby unique id
		List<JAXBBean> reqArr = wrapRequest("annoId", String.valueOf(id), "username", user, "secret", secret);
		String response = (String) delWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	public int deleteAnonymousAnnotation(String id, String secretKey) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String[] genericSearch(String query, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAuthenticationType() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDbLocation() {
		// TODO Auto-generated method stub
		return "CLRDB";
	}

	public AnnotationModelSerialiser getDefaultAMS() {
		// TODO Auto-generated method stub
		return ams;
	}

	public String getIndexingType() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNewIdentifier(String user, String secret) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRepositoryType() {
		// TODO Auto-generated method stub
		return "DB2";
	}

	public void init(String submitUri, String searchUri) {
		// TODO Auto-generated method stub
		
	}

	public boolean isGenericSearchSupported() {
		// TODO Auto-generated method stub
		//return false;
		return true;
	}

	public boolean isVerifyingAnnotations() {		
		return RESTAnnotationServer.verify;
	}

	public String[] lexicalSignatureSearch(String lexicalSignature) {
		// TODO Auto-generated method stub
		return null;
	}

	public int postAnnotation(String annotation, String user, String secret) throws Exception {
		
		List<JAXBBean> reqArr = wrapRequest("anno", annotation, "username", user, "secret", secret);		
		String response = (String) publishWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	public int postAnonymousAnnotation(String annotation, String secretKey)	throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setVerifyAnnotation(boolean verify) {
		// TODO Auto-generated method stub
		RESTAnnotationServer.verify = verify; //TODO, what is it? how is it used?
	}

	public int updateAnnotation(String replacement, String user, String secret)
			throws Exception {
		List<JAXBBean> reqArr = wrapRequest("updated", replacement, "username", user, "secret", secret);		
		String response = (String) updateWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	public int updateAnonymousAnnotation(String id, String replacement,
			String secretKey) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int createUserOrCheckExistence(String username, String name, String des, String aff){
		return 0; //ok
	}
	
	public HashMap<String, String> authenticated(String username, String pass) {
		HashMap<String,String> ret = new HashMap<String, String>();
		
		List<JAXBBean> reqArr = wrapRequest("username", username, "pass", pass, "", "");
		
		GenericType<Collection<JAXBBean>> resType = new GenericType<Collection<JAXBBean>>() {};
		Collection<JAXBBean> respons ;//= null;
		try{
			respons = usersAuthWR.type("application/json").accept("application/json").post(resType, new GenericEntity<List<JAXBBean>> (reqArr){});
			
		}catch(Exception e){
			ret.put("state","3");
			return ret;
		}
		List<JAXBBean> response = (List<JAXBBean>) respons;
		
		for(JAXBBean bean : response){
			ret.put(bean.getKey(), bean.getVal());
		}
		
		return ret;		
	}	
	
	public int createNewUser(String username, String pass, String email,
			String name, String des, String aff) {
		List<JAXBBean> reqArr = wrapRequest("username", username, "password", pass, "name", name);
		reqArr.addAll(wrapRequest("affiliation", aff, "description", des, "email" , email));
		String response = (String) usersAddWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}
	
	private List<JAXBBean> wrapRequest(String key1, String val1, String key2, String val2, String key3, String val3){
		JAXBBean request = new JAXBBean();
		
		request.setKey(key1);
		request.setVal(val1);

		List<JAXBBean> reqArr = new ArrayList<JAXBBean>();
		
		reqArr.add(request);
		
		JAXBBean request2 = new JAXBBean();
		request2.setKey(key2);
		request2.setVal(val2);
		reqArr.add(request2);
		
		JAXBBean request3 = new JAXBBean();
		request3.setKey(key3);
		request3.setVal(val3);
		reqArr.add(request3);
		
		return reqArr;
	}

	/**
	 * @return resourceId if ok, otherwise a negative number
	 */
	public int addAnnotatedResource(String bibtex, String url) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		List<JAXBBean> reqArr = wrapRequest("bibtex", bibtex, "url", url, "username", DistributedPersonalAnnos.userid);		
		String response = (String) resourcesAddWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	
	public String[] bibtexSearch(String url) {
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
        Collection<JAXBBean> response = searchBibWR.type("text/plain").accept("application/json").post(genericXmlType, url);
		//GenericEntity<List<JAXBBean>> (reqArr){}
		List<JAXBBean> res = (List<JAXBBean>) response;
		String[] annos = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			annos[i] = res.get(i).getKey();
		return annos;
	}

	public String urlLacksBibDoiKeywords(String url) {
		return resourceBibInfoWR.type("text/plain").accept("text/plain").post(String.class, url);		
	}

	public int updateResourceBib(String url, String doi, String keywords) {
		List<JAXBBean> reqArr = wrapRequest("url", url, "doi", doi, "keywords", keywords);		
		String response = (String) resourceBibInfoUpdateWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	public String[] retreiveAllTags(String url) {
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
        Collection<JAXBBean> response = searchTagsWR.type("text/plain").accept("application/json").post(genericXmlType, url);
		//GenericEntity<List<JAXBBean>> (reqArr){}
		List<JAXBBean> res = (List<JAXBBean>) response;
		String[] tags = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			tags[i] = res.get(i).getKey();
		return tags;
	}
	

}
