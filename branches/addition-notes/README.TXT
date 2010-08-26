Welcome to the Fab4 Browser
===========================

*source code distribution* 
==========================

Project Website:
===============

http://bodoni.lib.liv.ac.uk/fab4
http://code.google.com/p/fab4browser/

Author:
======

Fabio Corubolo

corubolo@gmail.com

Features:
========

This distribution includes the source code for :
- the Fab4 browser, including the supporting Multivalent and utility classes
- the annotation extensions 
- the XML, and Open Document text media adaptors
- the iRODS media adaptor
- the Fab4 webstart module, to deploy Fab4 as a webstart application.


Quickstart: compiling and packaging the Fab4 browser.
==========

The Fab4 Browser is distributed as Maven2 multi-module project. It is possible to compile all the sources for the Fab4 browser,
and required modules using the single 'mvn install' command.

Step by step:
1) Download and install Maven2: http://maven.apache.org/
2) Increase the maximum amount of memory available for Maven compiling: open a shell (console) and execute:
export MAVEN_OPTS=-Xmx512m
3) execute the maven command:
mvn install

The final output for the project consists in two files:

1) ./fab4assembly/target/fab4browser.zip
The zip file includes Fab4 and all the dependencies. To run the program, simply unzip the file and execure
java -jar fab4-start.jar

2) ./fab4webstart/target/fab4webstart.war
Java web archive, containing all the above and a dynamic servlet for opening documents directly into Fab4 (using Webstart). 
This can be deployed to a standard servlet container.

Project Documentation
=====================

For more information, see the project website.


Licenses: 
========

Fab4 is available under GPL License. Source code can be downloaded from the project websites.
Fab4 uses libraries licensed under different licenses: see the 'licenses' folder in the fab4Assembly module.
This product includes software developed by
The Apache Software Foundation (http://www.apache.org/).
This project uses Multivalent; see http://multivalent.sourceforge.net
