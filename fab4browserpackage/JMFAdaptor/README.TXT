JFFMPEG + JMF video media adaptor
-----------------------

Decodes and plays JFFMPEG supported video files, using the JFFMPEG decoding library.
see:
http://jffmpeg.sourceforge.net/

in order to compile , downlaod the required libraries and install them to the local Maven repository:

http://jffmpeg.sourceforge.net/
http://www.oracle.com/technetwork/java/javase/download-142937.html

using the commands:
mvn install:install-file  -Dfile=jmf.jar -DgroupId=javax.media -DartifactId=jmf -Dversion=2.1.1e -Dpackaging=jar
mvn install:install-file  -Dfile=jffmpeg-1.1.0.jar -DgroupId=jffmpeg -DartifactId=jffmpeg -Dversion=1.1 -Dpackaging=jar
