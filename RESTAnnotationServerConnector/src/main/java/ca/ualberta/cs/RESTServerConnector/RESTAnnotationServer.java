package ca.ualberta.cs.RESTServerConnector;

import java.io.IOException;
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
	private WebResource tagCloudUrlWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/tagsByUrl");
	private WebResource tagCloudBibWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/tagsByBib");
	private WebResource tagCloudDigestWR = client.resource(DistributedPersonalAnnos.searchServiceURL+"/tagsByDigest");
	
	public String IDSearch(String ID) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] URISearch(String URI) {
		if(URI.startsWith("file:/"))
			URI = DistributedPersonalAnnos.userid + ":" + URI;
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
			List<JAXBBean> reqArr = wrapRequest("annoIdFab", id, "username", user, "password", secret);
			String response = (String) delWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
			return Integer.parseInt(response);
		}
	}	
	
	private int deleteAnnotationByUniqueId(Integer id, String user, String secret) { //deleteby unique id
		List<JAXBBean> reqArr = wrapRequest("annoId", String.valueOf(id), "username", user, "password", secret);
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
		
		List<JAXBBean> reqArr = wrapRequest("anno", annotation, "username", user, "password", secret);		
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
		
		List<JAXBBean> reqArr = wrapRequest("updated", replacement, "username", user, "password", secret);		
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
	
	/**
	 * Asks server to authenticate the given user
	 * @param username, password
	 * @return HashMap<String,String> where key: state, values is: 0: ok, 1: wrong pass, 2:no such user, 3: exception
	 *  exactly as taken from the server
	 */
	public HashMap<String, String> authenticated(String username, String pass) {
		HashMap<String,String> ret = new HashMap<String, String>();
		
		List<JAXBBean> reqArr = wrapRequest("username", username, "password", pass, "", "");
		
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
	
	/**
	 * @return status code: 0: ok, return 1: username exists, return 2: email already taken, 3: exception
	 *  exactly as taken from the server
	 */
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

	public int addAnnotatedResource(HashMap<String, String> paperInfo,String url) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		String title = null;
		String abstr = null;
		String fullauthorinfo = null;
		String keywords = null;
		if(paperInfo.containsKey("title"))
			title = paperInfo.get("title");
		if(paperInfo.containsKey("abstract"))
			abstr = paperInfo.get("abstract");
		if(paperInfo.containsKey("fullAuthorInfo"))
			fullauthorinfo = paperInfo.get("fullAuthorInfo");
		if(paperInfo.containsKey("keywords"))
			keywords = paperInfo.get("keywords");
		
		List<JAXBBean> reqArr = wrapRequest("title", title, "url", url, "username", DistributedPersonalAnnos.userid);
		reqArr.addAll(wrapRequest("abstract", abstr, "fullAuthorInfo", fullauthorinfo, "keywords", keywords));
		String response = (String) resourcesAddWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}
	
	
	public String[] bibtexSearch(String url) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
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
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		return resourceBibInfoWR.type("text/plain").accept("text/plain").post(String.class, url);		
	}

	public int updateResourceBib(String url, String doi, String keywords) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		List<JAXBBean> reqArr = wrapRequest("url", url, "doi", doi, "keywords", keywords);		
		String response = (String) resourceBibInfoUpdateWR.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}

	/*public String[] retreiveAllTags(String url) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
        Collection<JAXBBean> response = searchTagsWR.type("text/plain").accept("application/json").post(genericXmlType, url);
		//GenericEntity<List<JAXBBean>> (reqArr){}
		List<JAXBBean> res = (List<JAXBBean>) response;
		String[] tags = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			tags[i] = res.get(i).getKey();
		return tags;
	}*/

	public HashMap<String,Integer> retreiveAllTags(String url) {
		if(url.startsWith("file:/"))
			url = DistributedPersonalAnnos.userid + ":" + url;
		GenericType<Collection<JAXBBean>> genericXmlType = new GenericType<Collection<JAXBBean>>() {};
		HashMap<String,Integer> cloud = new HashMap<String, Integer>();
		List<JAXBBean> res = new ArrayList<JAXBBean>();
		
		if(DistributedPersonalAnnos.sameUrl){
	        Collection<JAXBBean> response = tagCloudUrlWR.type("text/plain").accept("application/json").post(genericXmlType, url);			
			res.addAll((List<JAXBBean>) response);
		}
		if(DistributedPersonalAnnos.sameBib){
	        Collection<JAXBBean> response = tagCloudBibWR.type("text/plain").accept("application/json").post(genericXmlType, url);			
	        res.addAll((List<JAXBBean>) response);
		}
		if(DistributedPersonalAnnos.sameDigest){
	        Collection<JAXBBean> response = tagCloudDigestWR.type("text/plain").accept("application/json").post(genericXmlType, url);			
	        res.addAll((List<JAXBBean>) response);
		}
		
		for(JAXBBean bean : res){
			try{
				cloud.put(bean.getKey(), Integer.parseInt(bean.getVal()));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}		
		return cloud;
	}
	
	
	
	public int requestFriendship(String requester, String pass, String requested){
		WebResource webresource = client.resource(DistributedPersonalAnnos.publishServiceURL+"/friends/request");
		List<JAXBBean> reqArr = wrapRequest("requester",requester, "password", pass, "requested", requested);		
		String response = (String) webresource.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}
	
	public int acceptFriendship(String requester, String pass, String requested){
		WebResource webresource = client.resource(DistributedPersonalAnnos.publishServiceURL+"/friends/accept");
		List<JAXBBean> reqArr = wrapRequest("requester",requester, "password", pass, "requested", requested);		
		String response = webresource.type("application/json").post(String.class, new GenericEntity<List<JAXBBean>> (reqArr){});
		return Integer.parseInt(response);
	}
	
	public String[] listFriendships(String username, String pass){
		WebResource webresource = client.resource(DistributedPersonalAnnos.publishServiceURL+"/friends/listFriends");
		List<JAXBBean> reqArr = wrapRequest("username",username, "password", pass, "", null);
		GenericType<Collection<User>> genericXmlType = new GenericType<Collection<User>>() {};
		Collection<User> response = webresource.type("application/json").accept("application/json").post(genericXmlType, new GenericEntity<List<JAXBBean>> (reqArr){});
		List<User> res = (List<User>) response;
		String[] friends = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			friends[i] = res.get(i).getUsername();
		return friends;
	}
	
	public String[] listRequests(String username, String pass){
		WebResource webresource = client.resource(DistributedPersonalAnnos.publishServiceURL+"/friends/listRequests");
		List<JAXBBean> reqArr = wrapRequest("username",username, "password", pass, "", null);
		GenericType<Collection<User>> genericXmlType = new GenericType<Collection<User>>() {};
		Collection<User> response = webresource.type("application/json").accept("application/json").post(genericXmlType, new GenericEntity<List<JAXBBean>> (reqArr){});
		List<User> res = (List<User>) response;
		String[] requests = new String[res.size()];
		for(int i = 0 ; i < res.size() ; i++ )
			requests[i] = res.get(i).getUsername();
		return requests;
	}
	
	public static void main(String[] args) throws IOException {
		String requester = "samaneh";
		String pass = "sama";
		String requested = "ramin";		
		
		RESTAnnotationServer rest = new RESTAnnotationServer();
		
		rest.createNewUser("ramin", "rama", "samaneh.bayat@gmail.com", "ramin b", "researcher", "RAD3");
		rest.createNewUser("samaneh", "sama", "samaneh@ualberta.ca", "samaneh b", "student", "CS");
		
		int req = rest.requestFriendship(requester, pass, requested);
		System.out.println("request returned code:"+req);
		String[] requests = rest.listRequests(requested, "rama");
		System.out.println("requests:");
		for(String r: requests)
			System.out.print(r);
		System.out.println();
		
		System.out.println("Hit enter to accept friendship...");
		System.in.read();
		
		int acc = rest.acceptFriendship(requester, "rama", requested);
		System.out.println("accept returned code:"+acc);
		String[] friends = rest.listFriendships(requested, "rama");
		System.out.println(requested+"'s friendships:");
		for(String r: friends)
			System.out.print(r);
		System.out.println();
		friends = rest.listFriendships(requester, "sama");
		System.out.println(requester+"'s friendships:");
		for(String r: friends)
			System.out.print(r);
		System.out.println();
	}

}
