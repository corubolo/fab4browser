package multivalent.node;	//	   Should move this to multivalent to give fixed parity with flowed

import java.awt.Rectangle;


/**
	Interface for fixed internal and leaf classes.

	@version $Revision: 1.2 $ $Date: 2002/01/27 03:02:49 $
*/
public interface Fixed {
  /**
	Some fixed formats, such as OCR, do not report all the ink on the page, so if something forces a reformat such that
	there might be a loss of information, mark the document with this attribute,
	so some behavior can report a possible loss of information to the viewer.
  */
  String ATTR_REFORMATTED = "fixed";


  /** An "ibbox", or initial bbox, holds the absolute coordinates vs relative ones in bboxes. */
  Rectangle getIbbox();

  Rectangle getBbox();
}
