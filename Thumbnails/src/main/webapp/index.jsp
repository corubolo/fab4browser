
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
<p><select name="file_uri" size="9" onchange="submit()">
	<option>http://bodoni.lib.liv.ac.uk/conrod.jt</option>
	<option>http://bodoni.lib.liv.ac.uk/butterflyvalve.jt</option>
	<option>http://bodoni.lib.liv.ac.uk/bnc.jt</option>
	<option>http://www.schoolhistory.co.uk/year7links/1066/battlehastings.ppt</option>
	<option>http://www.xfront.com/REST-full.ppt</option>
	<option>http://java.sun.com/docs/books/jls/download/langspec-3.0.pdf</option>
	<option>http://www.ctan.org/tex-archive/info/lshort/english/lshort.pdf</option>
	<option>http://manuals.info.apple.com/en/iphone_user_guide.pdf</option>
	

</select> Width: <select name="w">
	<option>200</option>
	<option selected="selected">500</option>
	<option>800</option>
</select> Height: <select name="h">
	<option>200</option>
	<option selected="selected">500</option>
	<option>800</option>
</select> Format: <select name="f">
	<option>png</option>
	<option>bmp</option>
	<option>jpg</option>
	<option>gif</option>
</select>

Page number: <select name="p">
	<option>1</option>
	<option>2</option>
	<option>3</option>
	<option>4</option>
</select>
</p>
</form>

<%
    String jt_uri = request.getParameter("file_uri");
    if (jt_uri != null) {
       String q = "file_uri="+jt_uri;
   	   String w = request.getParameter("w");
   	   if (w != null)
   		   q += "&w="+w;
   	   String h = request.getParameter("h");
   	   if (h != null)
   		   q += "&h="+h;
   	   String f = request.getParameter("f");
   	   if (f != null)
   		   q += "&f="+f;
   	String p = request.getParameter("p");
   	 if (p != null)
		   q += "&p="+p;

%>

<img src="./RestThumbnailsServlet?<%=q %>">
<%    	
    } 
%>

<h2>See also the Thumbnails SOAP web service: <a
	href="./Thumbnails">Thumbnails</a></h2>

</body>
</html>

