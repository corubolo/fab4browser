# fab4browser

** Not actively developed ** 

A multiformat digital object parsing and presentation technology based on Multivalent

## Fab4 browser and extensions

The fab4 browser builds on the digital object model provided by Multivalent, and extends it to support distributed annotations and new media types. The software supports long term preservation of digital object by enabling access to the functionalities (presentation and interactivity) of the original digital objects in a pure virtual machine environment. This software has been developed at the University of Liverpool and supported by different projects, including the JISC VRE programme; and the SHAMAN and Presto Prime FP7 EU projects.

# Features

Allows access to (parsing and presentation) a variety of formats including: MS Office XLS and DOC, MXF, JT CAD, HTML, PDF, DVI, SVG, JPEG, PPT, OGG Theora + vorbis, MP3 and other formats.
Parsing and presentation is implemented in pure Java, without use of native components
Support for file identification and metadata extraction; generation of access aids
Can be executed in the iRODS data cloud as microservice
Implements experimental support for library emulation using the nestedvm compiler (http://nestedvm.ibex.org/)
Annotation features

Support for shared, distributed annotations using open standards (SRU, SWORD, ...)
Uses the XML digital signature standard to guarantee the provenance of the annotations
Annotations are stored separate from the original file, the original file remains untouched
Annotations are attached to the documents using different identifiers, so they are location and file format independent (you can annotate an ODF file with Fab4, email the file converted to PDF, and the receiver will be able to see the same annotations as in the original ODF file)
This project website includes the latest developments. The original web page up to 2008 is available at: http://bodoni.lib.liv.ac.uk/fab4/


# Documentation

https://code.google.com/archive/p/fab4browser/wikis

