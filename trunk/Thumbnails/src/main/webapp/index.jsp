<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    	               "http://www.w3.org/TR/html4/loose.dtd">

<html>
  <head>
    	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    	<title>SHAMAN Page</title>
    	
<script type="text/javascript">
function submit() {
//	var l = document.jt_form.jt_uri;
//	var i = l.selectedIndex;
//	var selected_uri = l.options[i].text;
	document.jt_form.submit();
}
</script>    	
  </head>
  
  
  
  <body>
    <h1>JT REST servlet</h1>
    
<form name="jt_form" method="get">
  <p>
    <select name="jt_uri" size="3" onchange="submit()">
    <option>http://media.ugs.com/teamcenter/jtfiles/conrod.jt</option>
	<option>http://media.ugs.com/teamcenter/jtfiles/butterflyvalve.jt</option>
	<option>http://media.ugs.com/teamcenter/jtfiles/bnc.jt</option>
    </select>
Width:
    <select name="w"" >
    <option>200</option>
	<option selected="selected"> 500</option>
	<option>700</option>
    </select>

Height:
    <select name="h"">
    <option>200</option>
	<option selected="selected">500</option>
	<option>700</option>
    </select>

Format:
    <select name="f"">
	<option>png</option>
    <option>bmp</option>
	<option>jpg</option>
	<option>gif</option>
    </select>

  </p>
  </form>    


<%
    String jt_uri = request.getParameter("jt_uri");
    if (jt_uri != null) {
       String q = "jt_uri="+jt_uri;
   	   String w = request.getParameter("w");
   	   if (w != null)
   		   q += "&w="+w;
   	   String h = request.getParameter("h");
   	   if (h != null)
   		   q += "&h="+h;
   	   String f = request.getParameter("f");
   	   if (f != null)
   		   q += "&f="+f;

%>    	
        <img src="./RestThumbnailsServlet?<%=q %>">
<%    	
    } 
%>
    
    <h2>See also the Thumbnails SOAP web service: <a href="./Thumbnails">Thumbnails</a></h2>
    
  </body>
</html> 
  	
