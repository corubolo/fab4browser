# Build instructions #

It should be simple to build the fab4browserpackage using Maven2.

  * Download and install Maven from http://maven.apache.org/
> [**Optional: open a shell, and increase the Maven memory for compiling: export MAVEN\_OPTS=-Xmx512m]
  * change directory to the fab4browserpackage folder, 'cd fab4browserpackage'
  * run 'mvn install'**

The final output for the project consists in two files:

1) ./fab4assembly/target/fab4-browser-version.zip
The zip file includes Fab4 and all the dependencies. To run the program, simply unzip the file and execure
java -jar fab4-start.jar

2) ./fab4webstart/target/fab4webstart.war
Java web archive, containing all the above and a dynamic servlet for opening documents directly into Fab4 (using Webstart).
This can be deployed to a standard servlet container.