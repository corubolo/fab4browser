Fab4 Webstart module.

The module includes a dynamic webstart servlet to allow executing fab4, opening at a specific URI, from a web browser.

The archive includes the Fab4 distribution files, so once deployed it allows simple and direct linking to the dynamic webstart. 
This require some simple steps: 

1)deploy the file “Fab4webstart.war” to the Java servlet server of choice (see server documentation, for example Apache Tomcat)
2)Given a destination object URL DEST_URL, for example: “http://www.[...]r-pdfprimer.pdf#page=4”
3)Given the deployment URL, DEPL_URL, for example: “http://host:post/path/Fab4Webstart/”
4)Given a function to URL-encode a string, URLEncode(String) 
5)Construct the webstart url, used to open a document in Fab4Browser at a the deployment URL, as follows:
webstart url =   DEPL_URL  +  “Fab4Browser.jnlp?” + URLEncode(DEST_URL)
6)in the example case, the webstart URL is:
“http://host:post/path/Fab4Webstart/Fab4Browser.jnlp?http%3A%2F%2Fwww.[...]r-pdfprimer.pdf%23page%3D4”

