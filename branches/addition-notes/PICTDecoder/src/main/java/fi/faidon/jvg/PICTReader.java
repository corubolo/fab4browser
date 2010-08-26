/*****************************************************************************
 *
 *                                 PICTReader.java 
 * 
 * Program that analyses a PICT file and display its' contents on stdout. 
 * 
 * Created by Kary FRAMLING 31/3/1998 
 *
 * Copyright 1998-2003 Kary FrŠmling
 * Source code distributed under GNU LESSER GENERAL PUBLIC LICENSE,
 * included in the LICENSE.txt file in the topmost directory
 *
 *****************************************************************************/

package fi.faidon.jvg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.imageio.ImageIO;

import fi.faidon.util.DataCompression;

public class PICTReader {

	// --------------------------------------------------------------------------------------
	// Public constants.
	// --------------------------------------------------------------------------------------

	/**
	 * Transfer modes.
	 */
	public static final int SRC_COPY = 0;
	public static final int SRC_OR = 1;
	public static final int SRC_XOR = 2;
	public static final int SRC_BIC = 3;
	public static final int NOT_SRC_COPY = 4;
	public static final int NOT_SRC_OR = 5;
	public static final int NOT_SRC_XOR = 6;
	public static final int NOT_SRC_BIC = 7;
	public static final int BLEND = 32;
	public static final int ADD_PIN = 33;
	public static final int ADD_OVER = 34;
	public static final int SUB_PIN = 35;
	public static final int TRANSPARENT = 36;
	public static final int AD_MAX = 37;
	public static final int SUB_OVER = 38;
	public static final int AD_MIN = 39;
	public static final int GRAYISH_TEXT_OR = 49;
	public static final int MASK = 64;

	/**
	 * Text face masks.
	 */
	public static final int TX_BOLD_MASK = 1;
	public static final int TX_ITALIC_MASK = 2;
	public static final int TX_UNDERLINE_MASK = 4;
	public static final int TX_OUTLINE_MASK = 8;
	public static final int TX_SHADOWED_MASK = 16;
	public static final int TX_CONDENSED_MASK = 32;
	public static final int TX_EXTENDED_MASK = 64;

	/**
	 * PICT opcodes.
	 */
	public static final byte[] OP_HEADER_OP = { (byte) 0x0C, 00 };
	public static final int NOP = 0x00;
	public static final int OP_CLIP_RGN = 0x01;
	public static final int OP_BK_PAT = 0x02;
	public static final int OP_TX_FONT = 0x03;
	public static final int OP_TX_FACE = 0x04;
	public static final int OP_TX_MODE = 0x05;
	public static final int OP_SP_EXTRA = 0x06;
	public static final int OP_PN_SIZE = 0x07;
	public static final int OP_PN_MODE = 0x08;
	public static final int OP_PN_PAT = 0x09;
	public static final int OP_FILL_PAT = 0x0A;
	public static final int OP_OV_SIZE = 0x0B;
	public static final int OP_ORIGIN = 0x0C;
	public static final int OP_TX_SIZE = 0x0D;
	public static final int OP_FG_COLOR = 0x0E;
	public static final int OP_BK_COLOR = 0x0F;
	public static final int OP_TX_RATIO = 0x10;
	public static final int OP_VERSION = 0x11;

	/* Untreated */
	public static final int OP_BK_PIX_PAT = 0x12;
	public static final int OP_PN_PIX_PAT = 0x13;
	public static final int OP_FILL_PIX_PAT = 0x14;

	public static final int OP_PN_LOC_H_FRAC = 0x15;
	public static final int OP_CH_EXTRA = 0x16;
	public static final int OP_RGB_FG_COL = 0x1A;
	public static final int OP_RGB_BK_COL = 0x1B;
	public static final int OP_HILITE_MODE = 0x1C;
	public static final int OP_HILITE_COLOR = 0x1D;
	public static final int OP_DEF_HILITE = 0x1E;
	public static final int OP_OP_COLOR = 0x1F;
	public static final int OP_LINE = 0x20;
	public static final int OP_LINE_FROM = 0x21;
	public static final int OP_SHORT_LINE = 0x22;
	public static final int OP_SHORT_LINE_FROM = 0x23;
	public static final int OP_LONG_TEXT = 0x28;
	public static final int OP_DH_TEXT = 0x29;
	public static final int OP_DV_TEXT = 0x2A;
	public static final int OP_DHDV_TEXT = 0x2B;
	public static final int OP_FONT_NAME = 0x2C;
	public static final int OP_LINE_JUSTIFY = 0x2D;
	public static final int OP_GLYPH_STATE = 0x2E;
	public static final int OP_FRAME_RECT = 0x30;
	public static final int OP_PAINT_RECT = 0x31;
	public static final int OP_ERASE_RECT = 0x32;
	public static final int OP_INVERT_RECT = 0x33;
	public static final int OP_FILL_RECT = 0x34;
	public static final int OP_FRAME_SAME_RECT = 0x38;
	public static final int OP_PAINT_SAME_RECT = 0x39;
	public static final int OP_ERASE_SAME_RECT = 0x3A;
	public static final int OP_INVERT_SAME_RECT = 0x3B;
	public static final int OP_FILL_SAME_RECT = 0x3C;
	public static final int OP_FRAME_R_RECT = 0x40;
	public static final int OP_PAINT_R_RECT = 0x41;
	public static final int OP_ERASE_R_RECT = 0x42;
	public static final int OP_INVERT_R_RECT = 0x43;
	public static final int OP_FILL_R_RECT = 0x44;
	public static final int OP_FRAME_SAME_R_RECT = 0x48;
	public static final int OP_PAINT_SAME_R_RECT = 0x49;
	public static final int OP_ERASE_SAME_R_RECT = 0x4A;
	public static final int OP_INVERT_SAME_R_RECT = 0x4B;
	public static final int OP_FILL_SAME_R_RECT = 0x4C;
	public static final int OP_FRAME_OVAL = 0x50;
	public static final int OP_PAINT_OVAL = 0x51;
	public static final int OP_ERASE_OVAL = 0x52;
	public static final int OP_INVERT_OVAL = 0x53;
	public static final int OP_FILL_OVAL = 0x54;
	public static final int OP_FRAME_SAME_OVAL = 0x58;
	public static final int OP_PAINT_SAME_OVAL = 0x59;
	public static final int OP_ERASE_SAME_OVAL = 0x5A;
	public static final int OP_INVERT_SAME_OVAL = 0x5B;
	public static final int OP_FILL_SAME_OVAL = 0x5C;
	public static final int OP_FRAME_ARC = 0x60;
	public static final int OP_PAINT_ARC = 0x61;
	public static final int OP_ERASE_ARC = 0x62;
	public static final int OP_INVERT_ARC = 0x63;
	public static final int OP_FILL_ARC = 0x64;
	public static final int OP_FRAME_SAME_ARC = 0x68;
	public static final int OP_PAINT_SAME_ARC = 0x69;
	public static final int OP_ERASE_SAME_ARC = 0x6A;
	public static final int OP_INVERT_SAME_ARC = 0x6B;
	public static final int OP_FILL_SAME_ARC = 0x6C;
	public static final int OP_FRAME_POLY = 0x70;
	public static final int OP_PAINT_POLY = 0x71;
	public static final int OP_ERASE_POLY = 0x72;
	public static final int OP_INVERT_POLY = 0x73;
	public static final int OP_FILL_POLY = 0x74;
	public static final int OP_FRAME_SAME_POLY = 0x78;
	public static final int OP_PAINT_SAME_POLY = 0x79;
	public static final int OP_ERASE_SAME_POLY = 0x7A;
	public static final int OP_INVERT_SAME_POLY = 0x7B;
	public static final int OP_FILL_SAME_POLY = 0x7C;
	public static final int OP_FRAME_RGN = 0x80;
	public static final int OP_PAINT_RGN = 0x81;
	public static final int OP_ERASE_RGN = 0x82;
	public static final int OP_INVERT_RGN = 0x83;
	public static final int OP_FILL_RGN = 0x84;
	public static final int OP_FRAME_SAME_RGN = 0x88;
	public static final int OP_PAINT_SAME_RGN = 0x89;
	public static final int OP_ERASE_SAME_RGN = 0x8A;
	public static final int OP_INVERT_SAME_RGN = 0x8B;
	public static final int OP_FILL_SAME_RGN = 0x8C;

	/* Untreated */
	public static final int OP_BITS_RECT = 0x90;
	public static final int OP_BITS_RGN = 0x91;
	public static final int OP_PACK_BITS_RECT = 0x98;
	public static final int OP_PACK_BITS_RGN = 0x99;

	public static final int OP_DIRECT_BITS_RECT = 0x9A;

	/* Untreated */
	public static final int OP_DIRECT_BITS_RGN = 0x9B;

	public static final int OP_SHORT_COMMENT = 0xA0;
	public static final int OP_LONG_COMMENT = 0xA1;
	public static final int OP_END_OF_PICTURE = 0xFF;
	public static final int OP_VERSION_2 = 0x2FF;
	public static final int OP_COMPRESSED_QUICKTIME = 0x8200;
	public static final int OP_UNCOMPRESSED_QUICKTIME = 0x8201;

	// --------------------------------------------------------------------------------------
	// Private constants.
	// --------------------------------------------------------------------------------------
	// private final boolean OBLIGATORY_REGISTRATION = false;

	private static final int PICT_NULL_HEADER_SIZE = 512;
	private static final int NBR_BYTES_IN_WORD = 2;
	private static final int NBR_BYTES_IN_LONG = 4;
	private static final int MIN_REGION_SIZE = 10;
	private static final int MIN_POLY_SIZE = 10;
	private static final int NBR_BYTES_PICT_COLOR_COMP = 2;
	private String APPLE_USE_RESERVED_FIELD_STR = "Reserved for Apple use.";

	// --------------------------------------------------------------------------------------
	// Public fields.
	// --------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------
	// Private fields.
	// --------------------------------------------------------------------------------------

	private InputStream pictureFIS;
	private BufferedInputStream pictureBIS;
	private Graphics2D drawGraphics;
	// private Component drawComponent;
	private boolean verbose;
	private int picSize;
	private Rectangle pictFrame;
	private int version;
	private boolean isExtendedVersion2;

	// Variables for storing draw status.
	private Point penPosition;
	private Rectangle lastRectangle;

	// Ratio between the screen resolution and the image resolution.
	private double screenImageXRatio;
	private double screenImageYRatio;

	// Color managing variables.
	private Color fgColor;
	private Color bgColor;
	private Color hiliteColor;

	// Vector of AWT images created during image import.
	private Vector imgVector;
	private Rectangle pictFrameOriginalRes;
	private String[] stringsToAviod = new String[] { "QuickTimeª and a",
			"TIFF (LZW) decompressor", "are needed to see this picture.",
			" decompressor" };
	private boolean opCompressedQuickTime = false;

	// --------------------------------------------------------------------------------------
	// Public methods.
	// --------------------------------------------------------------------------------------

	// =============================================================================
	// Constructor
	// =============================================================================
	/**
	 * Initialize stuff.
	 * 
	 * @author Kary FR&Auml;MLING 31/3/1998
	 * @throws IOException
	 */
	// =============================================================================
	public PICTReader(InputStream is) throws IOException {
		verbose = true;
		// Registration check.
		// if ( OBLIGATORY_REGISTRATION ) {
		// fi.faidon.protection.SerialNumberManager sm = new
		// fi.faidon.protection.SerialNumberManager();
		// if ( !sm.verifyCurrentPackage() ) {
		// System.err.println(fi.faidon.protection.SerialNumberManager.STR_REG_VERIFICATION_FAILED);
		// System.err.println("Exiting...");
		// System.exit(1);
		// }
		// }
		pictureFIS = is;
		pictureBIS = new BufferedInputStream(pictureFIS);
		// Read in header information.
		if (!readPICTHeader(pictureBIS)) {
			throw new IOException("Header read error! ");
		}
		penPosition = new Point(0, 0);
		lastRectangle = new Rectangle(0, 0);
		fgColor = Color.black;
		bgColor = Color.white;
		hiliteColor = Color.red;

	}

	// =============================================================================
	// getWidth {implement VectorImageProducer}
	// =============================================================================
	/**
	 * Implements VectorImageProducer getWidth(). If we don't know the frame
	 * size yet, then read it in.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	public int getWidth() {
		return pictFrame.width;
	}

	// =============================================================================
	// getHeight {implement VectorImageProducer}
	// =============================================================================
	/**
	 * Implements VectorImageProducer getHeight(). If we don't know the frame
	 * size yet, then read it in.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	public int getHeight() {
		return pictFrame.height;
	}

	// =============================================================================
	// drawIt {implement VectorImageProducer}
	// =============================================================================
	/**
	 * Implements VectorImageProducer drawIt(). Re-read the whole PICT file and
	 * replay the drawing operations in it.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 * @throws IOException
	 */
	// =============================================================================
	public void playIt(Graphics g) throws IOException// , Component c)
	{
		drawGraphics = (Graphics2D) g;
		// drawComponent = c;

		readPICTFile();
	}

	// =============================================================================
	// checkImage {implement VectorImageProducer}
	// =============================================================================
	/**
	 * Implements VectorImageProducer drawIntoImage(). Return the status of the
	 * drawing launched by drawIntoImage. NOT IMPLEMENTED YET!.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	public int checkImage() {
		return ImageObserver.ALLBITS;
	}

	// =============================================================================
	// setVerbose
	// =============================================================================
	/**
	 * Sets verbose to the state indicated.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	public void setVerbose(boolean state) {
		verbose = state;
	}

	// =============================================================================
	// isVerbose
	// =============================================================================
	/**
	 * Return if verbose or not.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	public boolean isVerbose(boolean state) {
		return verbose;
	}

	// =============================================================================
	// readPICTFile
	// =============================================================================
	/**
	 * Open and read the PICT file. If we have a drawing graphics, then we try
	 * to draw the contents as well as possible. If "verbose" is true, the
	 * elements read are listed on stdout.
	 * 
	 * @author Kary FR&Auml;MLING 31/3/1998
	 * @throws IOException
	 */
	// =============================================================================
	public boolean readPICTFile() throws IOException {

		// Get all the rest.
		if (!readPICTopcodes(pictureBIS)) {
			closeInputStreams();
			if (verbose)
				System.out.println("Error parsing file!");
			return false;
		}

		// Close file streams.
		closeInputStreams();

		// File read OK, return true.
		if (verbose)
			System.out.println("Finished reading file!");
		return true;
	}

	// =============================================================================
	// closeInputStreams
	// =============================================================================
	/**
	 * Close all input streams used.
	 * 
	 * @author Kary FR&Auml;MLING 4/4/1998
	 */
	// =============================================================================
	private void closeInputStreams() {
		try {
			if (pictureBIS != null)
				pictureBIS.close();
			if (pictureFIS != null)
				pictureFIS.close();
		} catch (IOException e) {
		}
	}

	// =============================================================================
	// readPICTHeader
	// =============================================================================
	/**
	 * Read the PICT header. The information read is shown on stdout if
	 * "verbose" is true.
	 * 
	 * @author Kary FR&Auml;MLING 31/3/1998
	 */
	// =============================================================================
	private boolean readPICTHeader(BufferedInputStream bis) throws IOException {
		int screen_resolution;
		byte[] word_buf = new byte[NBR_BYTES_IN_WORD];
		byte[] rect_buf = new byte[4 * NBR_BYTES_IN_WORD];

		// Verify that we have a correct stream.
		if (bis == null)
			return false;

		// Skip first 512 bytes.
		fillByteBuf(bis, new byte[PICT_NULL_HEADER_SIZE], 0,
				PICT_NULL_HEADER_SIZE);

		// Get size.
		fillByteBuf(bis, word_buf, 0, word_buf.length);
		picSize = getBytesAsInt(word_buf, 0, word_buf.length);
		if (verbose)
			System.out.println("picSize: " + picSize);

		// Get frame.
		fillByteBuf(bis, rect_buf, 0, rect_buf.length);
		pictFrame = new Rectangle(getBytesAsInt(rect_buf, 2, 4), getBytesAsInt(
				rect_buf, 0, 2), getBytesAsInt(rect_buf, 6, 8), getBytesAsInt(
				rect_buf, 4, 6));
		if (verbose)
			System.out.println("pictFrame: " + pictFrame);

		// Set default display ratios. 72 dpi is the standard Macintosh
		// resolution.
		screen_resolution = 72; // Toolkit.getDefaultToolkit().getScreenResolution();
		screenImageXRatio = 1.0;
		screenImageYRatio = 1.0;

		// Get the version, since the way of reading the rest depends on it.
		fillByteBuf(bis, word_buf, 0, word_buf.length);
		if (word_buf[0] == OP_VERSION && word_buf[1] == 1) {
			version = 1;
		} else if (word_buf[0] == 0 && word_buf[1] == OP_VERSION) {
			fillByteBuf(bis, word_buf, 0, word_buf.length);
			if (word_buf[0] == 2 && word_buf[1] == (byte) 0xFF) {
				version = 2;

				// Read in version 2 header and test that it is valid. First
				// HeaderOp 0x0C00.
				fillByteBuf(bis, word_buf, 0, word_buf.length);
				if (word_buf[0] != OP_HEADER_OP[0]
						|| word_buf[1] != OP_HEADER_OP[1]) {
					return false;
				}

				// Then the rest, which we just skip for the moment, we only see
				// if it is
				// an extended version 2 or not.
				byte[] header_information = new byte[24];
				fillByteBuf(bis, header_information, 0,
						header_information.length);
				if (header_information[1] == (byte) 0xFE) {
					isExtendedVersion2 = true;

					// Get the image resolution and calculate the ratio between
					// the default Mac screen resolution and the image
					// resolution.
					int xres = getBytesAsInt(header_information, 4, 6);
					int yres = getBytesAsInt(header_information, 8, 10);
					pictFrameOriginalRes = new Rectangle(getBytesAsInt(
							header_information, 14, 16), getBytesAsInt(
							header_information, 12, 14), getBytesAsInt(
							header_information, 18, 20), getBytesAsInt(
							header_information, 16, 18));
					screenImageXRatio = (double) screen_resolution / xres;
					screenImageYRatio = (double) screen_resolution / yres;

					if (verbose)
						System.out.println("xResolution: " + xres);
					if (verbose)
						System.out.println("yResolution: " + yres);
					if (verbose)
						System.out.println("screenImageXRatio: "
								+ screenImageXRatio);
					if (verbose)
						System.out.println("screenImageYRatio: "
								+ screenImageYRatio);
				}
			} else {
				return false;
			}
		} else {
			// No version information, return straight away.
			return false;
		}
		if (verbose)
			System.out.println("Version: " + version);
		if (verbose)
			System.out.println("isExtendedVersion2: " + isExtendedVersion2);

		// Everything OK, return true.
		return true;
	}

	// =============================================================================
	// readPICTopcodes
	// =============================================================================
	/**
	 * Parse PICT opcodes in a PICT file. The input stream should be positioned
	 * at the beginning of the opcodes, after picframe. If we have a non-null
	 * draw graphics, then we try to draw the elements as well as we can.
	 * 
	 * @author Kary FR&Auml;MLING 31/3/1998
	 * @throws IOException
	 */
	// =============================================================================
	private boolean readPICTopcodes(BufferedInputStream bis) throws IOException {
		int i, opcode, dh, dv, data_len;
		byte[] buf1 = new byte[1];
		byte[] v1_opcode = buf1;
		byte[] buf2 = new byte[2];
		byte[] v2_opcode = buf2;
		byte[] byte_buf = buf1;
		byte[] word_buf = new byte[NBR_BYTES_IN_WORD];
		byte[] point_word_buf = new byte[2 * NBR_BYTES_IN_WORD];
		byte[] rect_buf = new byte[4 * NBR_BYTES_IN_WORD];
		byte[] long_buf = new byte[NBR_BYTES_IN_LONG];
		byte[] color_buf = new byte[3 * NBR_BYTES_PICT_COLOR_COMP];
		Point origin, dh_dv;
		Point ov_size = new Point();
		Point new_pen_pos = new Point();
		Point arc_angles = new Point();
		String text;
		long byte_cnt = 0;
		Rectangle bounds = new Rectangle();
		Polygon a_polygon = new Polygon();
		Polygon a_region = new Polygon();
		Font current_font = null;
		Rectangle src_rect, dst_rect;
		int transfer_mode;
		int packed_bytes_cnt;
		int pixmap_cnt = 0;
		BufferedImage img;

		// Initialize current font.
		if (drawGraphics != null) {
			current_font = drawGraphics.getFont();
		}

		// Go through the file until we find an end of picture opcode.
		do {
			// Read opcode. It is a byte with version 1, a word for version 2.
			if (version == 1) {
				fillByteBuf(bis, v1_opcode, 0, v1_opcode.length);
				byte_cnt++;
				opcode = getBytesAsInt(v1_opcode, 0, v1_opcode.length);
			} else {
				// Always be sure to be word-aligned for version 2 pictures.
				if ((byte_cnt & 1) > 0) {
					fillByteBuf(bis, byte_buf, 0, 1);
					byte_cnt++;
				}
				fillByteBuf(bis, v2_opcode, 0, v2_opcode.length);
				byte_cnt += 2;
				opcode = getBytesAsInt(v2_opcode, 0, v2_opcode.length);
			}

			// See what we got and react in consequence.
			if (opcode >= 0x0100 && opcode <= 0x7FFF) {
//				int k = opcode >>> 8;
//				k = k << 1;
//				System.out.println("reserved:" + Integer.toHexString(opcode)
//						+ " k= " + k);
//				fillByteBuf(bis, new byte[k], 0, k);
//				byte_cnt += k;

			} else {
				byte[] buf8 = new byte[8];
				byte[] buf4 = new byte[4];
				switch (opcode) {
				case NOP:
					// Just go on
					if (verbose)
						System.out.print("NOP");
					break;

				case OP_CLIP_RGN: // OK for RECTS, not for regions yet.
					// Read the region.
					if ((a_region = readRegion(bis, bounds)) == null)
						return false;

					// Set clip rect or clip region.
					if (drawGraphics != null) {
						if (a_region.npoints == 0) {
							drawGraphics.setClip(bounds.x, bounds.y,
									bounds.width, bounds.height);
						} else {
						}
					}
					if (verbose)
						verboseRegionCmd("clipRgn", bounds, a_region);
					break;

				case OP_BK_PAT: // NOT SUPPORTED IN AWT GRAPHICS YET.
					// Get the data.
					fillByteBuf(bis, buf8, 0, 8);
					byte_cnt += 8;
					if (verbose)
						System.out.println("bkPat");
					break;

				case OP_TX_FONT: // DIFFICULT TO KNOW THE FONT???
					// Get the data.
					byte[] f = buf2;
					fillByteBuf(bis, f, 0, 2);
					int ff = getBytesAsInt(f, 0, 2);
					byte_cnt += 2;
					if (drawGraphics != null) {
						// Construct text face mask.
						current_font = drawGraphics.getFont();
						if (ff <= 1)
							drawGraphics.setFont(new Font("Dialog",
									current_font.getStyle(), current_font
											.getSize()));// FAMILY_SWISS
						else if (ff <= 12)
							drawGraphics.setFont(new Font("Serif", current_font
									.getStyle(), current_font.getSize()));// FAMILY_DECORATIVE
						else if (ff <= 20)
							drawGraphics.setFont(new Font("Serif", current_font
									.getStyle(), current_font.getSize()));// FAMILY_ROMAN
						else if (ff == 21)
							drawGraphics.setFont(new Font("Sans", current_font
									.getStyle(), current_font.getSize()));// FAMILY_SWISS
						else if (ff == 22)
							drawGraphics.setFont(new Font("Sans", current_font
									.getStyle(), current_font.getSize()));// FAMILY_MODERN
						else if (ff <= 1023)
							drawGraphics.setFont(new Font("Sans", current_font
									.getStyle(), current_font.getSize()));// FAMILY_SWISS
						else
							drawGraphics.setFont(new Font("Serif", current_font
									.getStyle(), current_font.getSize()));// FAMILY_ROMAN
						if (ff == 23)
							drawGraphics.setFont(new Font("Serif", current_font
									.getStyle(), current_font.getSize()));// RTL_TEXTENCODING_SYMBOL
						// else aActFont.SetCharSet( gsl_getSystemTextEncoding()
						// );
					}

					if (verbose)
						System.out.println("txFont");
					break;

				case OP_TX_FACE: // SEE IF IT IS TO BE IMPLEMENTED FOR NOW?
					// Get the data.
					fillByteBuf(bis, byte_buf, 0, byte_buf.length);
					byte_cnt += byte_buf.length;
					if (drawGraphics != null) {
						// Construct text face mask.
						current_font = drawGraphics.getFont();
						int awt_face_mask = 0;
						if ((byte_buf[0] & (byte) TX_BOLD_MASK) > 0)
							awt_face_mask |= Font.BOLD;
						if ((byte_buf[0] & (byte) TX_ITALIC_MASK) > 0)
							awt_face_mask |= Font.ITALIC;
						// if ( (byte_buf[0]&(byte) TX_UNDERLINE_MASK) > 0 )
						// awt_face_mask |= Font.ITALIC;
						// Set the font.
						drawGraphics.setFont(new Font(current_font.getName(),
								awt_face_mask, current_font.getSize()));
					}
					if (verbose)
						System.out.println("txFace: " + byte_buf[0]);
					break;

				case OP_TX_MODE: // SEE IF IT IS TO BE IMPLEMENTED FOR NOW?
					// Get the data.
					byte[] mode_buf = buf2;
					fillByteBuf(bis, mode_buf, 0, mode_buf.length);
					byte_cnt += mode_buf.length;
					if (verbose)
						System.out.println("txMode: " + mode_buf[0] + ", "
								+ mode_buf[1]);
					break;

				case OP_SP_EXTRA: // WONDER WHAT IT IS?
					// Get the data.
					fillByteBuf(bis, buf4, 0, 4);
					byte_cnt += 4;
					if (verbose)
						System.out.println("spExtra");
					break;

				case OP_PN_SIZE: // NOT SUPPORTED IN AWT GRAPHICS YET.
					// Get the two words.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					byte_cnt += point_word_buf.length;
					Point pnsize = new Point(
							getBytesAsInt(point_word_buf, 0, 2), getBytesAsInt(
									point_word_buf, 2, 4));
					drawGraphics.setStroke(new BasicStroke(
							(pnsize.x + pnsize.y) / 2));
					if (verbose)
						System.out.println("pnsize: " + pnsize);
					break;

				case OP_PN_MODE: // TRY EMULATING WITH SETXORMODE ETC.
					// Get the data.
					byte[] mode = buf2;
					fillByteBuf(bis, mode, 0, 2);
					int pm = getBytesAsInt(mode, 0, 2);
					switch (pm) {
					case 0:
					case 1:
						drawGraphics.setComposite(AlphaComposite.SrcOver);
						break;
					case 2:
						drawGraphics.setComposite(AlphaComposite.Xor);
						break;
					case 3:
						drawGraphics.setComposite(AlphaComposite.SrcOver);
						break;
					case 6:
						drawGraphics.setComposite(AlphaComposite.Xor);
						break;
					default:
						drawGraphics.setComposite(AlphaComposite.SrcOver);
						break;
					}

					byte_cnt += 2;
					if (verbose)
						System.out.println("pnMode");
					break;

				case OP_PN_PAT: // NOT SUPPORTED IN AWT GRAPHICS YET.
					// Get the data.
					fillByteBuf(bis, buf8, 0, 8);
					byte_cnt += 8;
					// drawGraphics.setStroke()
					if (verbose)
						System.out.println("pnPat");
					break;

				case OP_FILL_PAT: // NOT SUPPORTED IN AWT GRAPHICS YET.
					// Get the data.
					fillByteBuf(bis, buf8, 0, 8);
					// drawGraphics.setPaint()
					byte_cnt += 8;
					if (verbose)
						System.out.println("fillPat");
					break;

				case OP_OV_SIZE: // OK, we use this for rounded rectangle
									// corners.
					// Get the two words.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					byte_cnt += point_word_buf.length;
					setPointFromBuf(point_word_buf, ov_size);
					ov_size.x *= 2; // Don't know why, but has to be multiplied
									// by 2.
					ov_size.y *= 2;
					if (verbose)
						System.out.println("ovSize: " + ov_size);
					break;

				case OP_ORIGIN: // PROBABLY OK.
					// Get the two words.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					byte_cnt += point_word_buf.length;
					origin = new Point(getXPtCoord(getBytesAsInt(
							point_word_buf, 2, 4)), getYPtCoord(getBytesAsInt(
							point_word_buf, 0, 2)));
					if (drawGraphics != null) {
						drawGraphics.translate(origin.x, origin.y);
					}
					if (verbose)
						System.out.println("Origin: " + origin);
					break;

				case OP_TX_SIZE: // OK.
					// Get the text size.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int tx_size = getYPtCoord(getBytesAsInt(word_buf, 0,
							word_buf.length));
					if (drawGraphics != null) {
						current_font = drawGraphics.getFont();
						drawGraphics.setFont(new Font(current_font.getName(),
								current_font.getStyle(), tx_size));
					}
					if (verbose)
						System.out.println("txSize: " + tx_size);
					break;

				case OP_FG_COLOR: // TO BE DONE IF POSSIBLE.
					// Get the data.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					drawGraphics.setColor(readColor(long_buf));
					if (verbose)
						System.out.println("fgColor");
					break;

				case OP_BK_COLOR: // TO BE DONE IF POSSIBLE.
					// Get the data.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					drawGraphics.setBackground(readColor(long_buf));
					if (verbose)
						System.out.println("bgColor");
					break;

				case OP_TX_RATIO: // SEE IF WE HAVE THIS???
					// Get the data.
					fillByteBuf(bis, buf8, 0, 8);
					byte_cnt += 8;
					if (verbose)
						System.out.println("txRatio");
					break;

				case OP_VERSION: // OK, ignored since we should already have it.
					// Get the data.
					fillByteBuf(bis, buf1, 0, 1);
					byte_cnt += 1;
					if (verbose)
						System.out.println("opVersion");
					break;

				case OP_PN_LOC_H_FRAC: // TO BE DONE???.
					// Get the data.
					fillByteBuf(bis, buf2, 0, 2);
					byte_cnt += 2;
					if (verbose)
						System.out.println("opPnLocHFrac");
					break;

				case OP_CH_EXTRA: // TO BE DONE???.
					// Get the data.
					fillByteBuf(bis, buf2, 0, 2);
					byte_cnt += 2;
					if (verbose)
						System.out.println("opChExtra");
					break;

				case OP_RGB_FG_COL: // OK.
					// Get the color.
					fillByteBuf(bis, color_buf, 0, color_buf.length);
					byte_cnt += color_buf.length;
					fgColor = new Color((color_buf[0] & 0xFF),
							(color_buf[2] & 0xFF), (color_buf[4] & 0xFF));
					if (drawGraphics != null) {
						drawGraphics.setColor(fgColor);
					}
					if (verbose)
						System.out.println("rgbFgColor: " + fgColor);
					break;

				case OP_RGB_BK_COL: // OK.
					// Get the color.
					fillByteBuf(bis, color_buf, 0, color_buf.length);
					byte_cnt += color_buf.length;
					bgColor = new Color((color_buf[0] & 0xFF),
							(color_buf[2] & 0xFF), (color_buf[4] & 0xFF));
					if (drawGraphics != null) {
						drawGraphics.setBackground(bgColor);
					}
					if (verbose)
						System.out.println("rgbBgColor: " + bgColor);
					break;

				case OP_HILITE_MODE: // TO BE DONE?.
					// Change color to hilite color.
					if (verbose)
						System.out.println("opHiliteMode");
					break;

				case OP_HILITE_COLOR: // OK.
					// Get the color.
					fillByteBuf(bis, color_buf, 0, color_buf.length);
					byte_cnt += color_buf.length;
					hiliteColor = new Color((color_buf[0] & 0xFF),
							(color_buf[2] & 0xFF), (color_buf[4] & 0xFF));
					if (verbose)
						System.out.println("opHiliteColor: " + hiliteColor);
					byte_cnt += color_buf.length;
					break;

				case OP_DEF_HILITE: // Macintosh internal, ignored?.
					// Nothing to do.
					if (verbose)
						System.out.println("opDefHilite");
					break;

				case OP_OP_COLOR: // To be done once I know what it means.
					// Get the color.
					fillByteBuf(bis, color_buf, 0, color_buf.length);
					byte_cnt += color_buf.length;
					if (verbose)
						System.out.println("opOpColor");
					break;

				case OP_LINE: // OK, not tested.
					// Get the data (two points).
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					origin = new Point();
					setPointFromBuf(point_word_buf, origin);
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					setPointFromBuf(point_word_buf, penPosition);

					// Move pen to new position, draw line if we have a
					// graphics.
					if (drawGraphics != null) {
						drawGraphics.drawLine(origin.x, origin.y,
								penPosition.x, penPosition.y);
					}
					if (verbose)
						System.out.println("line from: " + origin + " to: "
								+ penPosition);
					byte_cnt += 2 * point_word_buf.length;
					break;

				case OP_LINE_FROM: // OK, not tested.
					// Get the point.
					origin = new Point(penPosition);
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					setPointFromBuf(point_word_buf, penPosition);

					// Move pen to new position, draw line if we have a
					// graphics.
					if (drawGraphics != null) {
						drawGraphics.drawLine(origin.x, origin.y,
								penPosition.x, penPosition.y);
					}
					if (verbose)
						System.out.println("lineFrom to: " + penPosition);
					byte_cnt += point_word_buf.length;
					break;

				case OP_SHORT_LINE: // OK.
					// Get origin and dh, dv.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					origin = new Point();
					setPointFromBuf(point_word_buf, origin);
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					dh = getXPtCoord(word_buf[0]);
					dv = getYPtCoord(word_buf[1]);
					dh_dv = new Point(dh, dv);

					// Move pen to new position, draw line if we have a
					// graphics.
					penPosition.setLocation(origin.x + dh_dv.x, origin.y
							+ dh_dv.y);
					if (drawGraphics != null) {
						drawGraphics.drawLine(origin.x, origin.y,
								penPosition.x, penPosition.y);
					}
					if (verbose)
						System.out.println("Short line origin: " + origin
								+ ", dh,dv: " + dh_dv);
					byte_cnt += point_word_buf.length + word_buf.length;
					break;

				case OP_SHORT_LINE_FROM: // OK.
					// Get dh, dv.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					dh = getXPtCoord(word_buf[0]);
					dv = getYPtCoord(word_buf[1]);
					dh_dv = new Point(dh, dv);

					// Move pen to new position, draw line if we have a
					// graphics.
					new_pen_pos.setLocation(penPosition.x + dh_dv.x,
							penPosition.y + dh_dv.y);
					if (drawGraphics != null) {
						drawGraphics.drawLine(penPosition.x, penPosition.y,
								new_pen_pos.x, new_pen_pos.y);
					}
					penPosition.setLocation(new_pen_pos);
					if (verbose)
						System.out.println("Short line from dh,dv: " + dh_dv);
					byte_cnt += word_buf.length;
					break;

				case 0x24:
				case 0x25:
				case 0x26:
				case 0x27:
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					data_len = getBytesAsInt(word_buf, 0, word_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				case OP_LONG_TEXT: // OK.
					// Get the data.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					origin = new Point();
					setPointFromBuf(point_word_buf, origin);
					penPosition = origin;
					text = readText(bis);
					boolean write = true;
					if (opCompressedQuickTime)
						for (String no : stringsToAviod) {
							if (no.equals(text))
								write = false;
						}

					if (drawGraphics != null && write) {
						drawGraphics.drawString(text, penPosition.x,
								penPosition.y);
					}
					if (verbose)
						System.out.println("longText origin: " + penPosition
								+ ", text:" + text);
					byte_cnt += 5 + text.length();
					break;

				case OP_DH_TEXT: // OK, not tested.
					// Get dh.
					fillByteBuf(bis, byte_buf, 0, byte_buf.length);
					dh = getXPtCoord(byte_buf[0]);
					penPosition.translate(dh, 0);
					text = readText(bis);
					if (drawGraphics != null) {
						drawGraphics.drawString(text, penPosition.x,
								penPosition.y);
					}
					if (verbose)
						System.out.println("DHText dh: " + dh + ", text:"
								+ text);
					byte_cnt += byte_buf.length + 1 + text.length();
					break;

				case OP_DV_TEXT: // OK, not tested.
					// Get dh.
					fillByteBuf(bis, byte_buf, 0, byte_buf.length);
					dv = getYPtCoord(byte_buf[0]);
					penPosition.translate(0, dv);
					text = readText(bis);
					if (drawGraphics != null) {
						drawGraphics.drawString(text, penPosition.x,
								penPosition.y);
					}
					if (verbose)
						System.out.println("DVText dv: " + dv + ", text:"
								+ text);
					byte_cnt += byte_buf.length + 1 + text.length();
					break;

				case OP_DHDV_TEXT: // OK, not tested.
					// Get dh, dv.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					dh = getXPtCoord(word_buf[0]);
					dv = getYPtCoord(word_buf[1]);
					penPosition.translate(dh, dv);
					text = readText(bis);
					if (drawGraphics != null) {
						drawGraphics.drawString(text, penPosition.x,
								penPosition.y);
					}
					if (verbose)
						System.out.println("DHDVText penPosition: "
								+ penPosition + ", text:" + text);
					byte_cnt += word_buf.length + 1 + text.length();
					break;

				case OP_FONT_NAME: // OK, not tested.
					// Get data length.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					data_len = getBytesAsInt(word_buf, 0, 2);

					// Get old font ID, ignored.
					fillByteBuf(bis, word_buf, 0, word_buf.length);

					// Get font name and set the new font if we have one.
					text = readText(bis);
					if (drawGraphics != null) {
						drawGraphics.setFont(new Font(Font.decode(text)
								.getName(), current_font.getStyle(),
								current_font.getSize()));
					}
					if (verbose)
						System.out.println("fontName: " + text);
					byte_cnt += 5 + text.length();
					break;

				case OP_LINE_JUSTIFY: // TO BE DONE???.
					// Get data.
					fillByteBuf(bis, new byte[10], 0, 10);
					if (verbose)
						System.out.println("opLineJustify");
					byte_cnt += 10;
					break;

				case OP_GLYPH_STATE: // NOT SUPPORTED IN AWT GRAPHICS YET.
					// Get data.
					fillByteBuf(bis, new byte[6], 0, 6);
					if (verbose)
						System.out.println("glyphState");
					byte_cnt += 6;
					break;

				case 0x2F:
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					data_len = getBytesAsInt(word_buf, 0, word_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				// --------------------------------------------------------------------------------
				// Rect treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_RECT: // OK.
				case OP_PAINT_RECT: // OK.
				case OP_ERASE_RECT: // OK, not tested.
				case OP_INVERT_RECT: // OK, not tested.
				case OP_FILL_RECT: // OK, not tested.
					// Get the frame rectangle.
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					setRectFromBuf(rect_buf, lastRectangle);

				case OP_FRAME_SAME_RECT: // OK, not tested.
				case OP_PAINT_SAME_RECT: // OK, not tested.
				case OP_ERASE_SAME_RECT: // OK, not tested.
				case OP_INVERT_SAME_RECT: // OK, not tested.
				case OP_FILL_SAME_RECT: // OK, not tested.
					// Draw.
					if (drawGraphics != null) {
						switch (opcode) {
						case OP_FRAME_RECT:
						case OP_FRAME_SAME_RECT:
							drawGraphics.drawRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						case OP_PAINT_RECT:
						case OP_PAINT_SAME_RECT:
							drawGraphics.fillRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						case OP_ERASE_RECT:
						case OP_ERASE_SAME_RECT:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_RECT:
						case OP_INVERT_SAME_RECT:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_RECT:
						case OP_FILL_SAME_RECT:
							drawGraphics.fillRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_RECT)
						System.out.println("frameRect: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_RECT)
						System.out.println("paintRect: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_RECT)
						System.out.println("eraseRect: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_RECT)
						System.out.println("invertRect: " + lastRectangle);
					if (verbose && opcode == OP_FILL_RECT)
						System.out.println("fillRect: " + lastRectangle);
					if (verbose && opcode == OP_FRAME_SAME_RECT)
						System.out.println("frameSameRect: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_SAME_RECT)
						System.out.println("paintSameRect: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_SAME_RECT)
						System.out.println("eraseSameRect: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_SAME_RECT)
						System.out.println("invertSameRect: " + lastRectangle);
					if (verbose && opcode == OP_FILL_SAME_RECT)
						System.out.println("fillSameRect: " + lastRectangle);

					// Rect treatments finished.
					break;

				// --------------------------------------------------------------------------------
				// Round Rect treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_R_RECT: // OK.
				case OP_PAINT_R_RECT: // OK, not tested.
				case OP_ERASE_R_RECT: // OK, not tested.
				case OP_INVERT_R_RECT: // OK, not tested.
				case OP_FILL_R_RECT: // OK, not tested.
					// Get the frame rectangle.
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					setRectFromBuf(rect_buf, lastRectangle);

				case OP_FRAME_SAME_R_RECT: // OK, not tested.
				case OP_PAINT_SAME_R_RECT: // OK, not tested.
				case OP_ERASE_SAME_R_RECT: // OK, not tested.
				case OP_INVERT_SAME_R_RECT: // OK, not tested.
				case OP_FILL_SAME_R_RECT: // OK, not tested.
					// Draw.
					if (drawGraphics != null) {
						switch (opcode) {
						case OP_FRAME_R_RECT:
						case OP_FRAME_SAME_R_RECT:
							drawGraphics.drawRoundRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, ov_size.x, ov_size.y);
							break;
						case OP_PAINT_R_RECT:
						case OP_PAINT_SAME_R_RECT:
							drawGraphics.fillRoundRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, ov_size.x, ov_size.y);
							break;
						case OP_ERASE_R_RECT:
						case OP_ERASE_SAME_R_RECT:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillRoundRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, ov_size.x, ov_size.y);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_R_RECT:
						case OP_INVERT_SAME_R_RECT:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillRoundRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, ov_size.x, ov_size.y);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_R_RECT:
						case OP_FILL_SAME_R_RECT:
							drawGraphics.fillRoundRect(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, ov_size.x, ov_size.y);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_R_RECT)
						System.out.println("frameRRect: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_R_RECT)
						System.out.println("paintRRect: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_R_RECT)
						System.out.println("eraseRRect: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_R_RECT)
						System.out.println("invertRRect: " + lastRectangle);
					if (verbose && opcode == OP_FILL_R_RECT)
						System.out.println("fillRRect: " + lastRectangle);
					if (verbose && opcode == OP_FRAME_SAME_R_RECT)
						System.out.println("frameSameRRect: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_SAME_R_RECT)
						System.out.println("paintSameRRect: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_SAME_R_RECT)
						System.out.println("eraseSameRRect: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_SAME_R_RECT)
						System.out.println("invertSameRRect: " + lastRectangle);
					if (verbose && opcode == OP_FILL_SAME_R_RECT)
						System.out.println("fillSameRRect: " + lastRectangle);

					// RoundRect treatments finished.
					break;

				// --------------------------------------------------------------------------------
				// Oval treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_OVAL: // OK.
				case OP_PAINT_OVAL: // OK, not tested.
				case OP_ERASE_OVAL: // OK, not tested.
				case OP_INVERT_OVAL: // OK, not tested.
				case OP_FILL_OVAL: // OK, not tested.
					// Get the frame rectangle.
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					setRectFromBuf(rect_buf, lastRectangle);

				case OP_FRAME_SAME_OVAL: // OK, not tested.
				case OP_PAINT_SAME_OVAL: // OK, not tested.
				case OP_ERASE_SAME_OVAL: // OK, not tested.
				case OP_INVERT_SAME_OVAL: // OK, not tested.
				case OP_FILL_SAME_OVAL: // OK, not tested.
					// Draw.
					if (drawGraphics != null) {
						switch (opcode) {
						case OP_FRAME_OVAL:
						case OP_FRAME_SAME_OVAL:
							drawGraphics.drawOval(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						case OP_PAINT_OVAL:
						case OP_PAINT_SAME_OVAL:
							drawGraphics.fillOval(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						case OP_ERASE_OVAL:
						case OP_ERASE_SAME_OVAL:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillOval(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_OVAL:
						case OP_INVERT_SAME_OVAL:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillOval(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_OVAL:
						case OP_FILL_SAME_OVAL:
							drawGraphics.fillOval(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_OVAL)
						System.out.println("frameOval: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_OVAL)
						System.out.println("paintOval: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_OVAL)
						System.out.println("eraseOval: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_OVAL)
						System.out.println("invertOval: " + lastRectangle);
					if (verbose && opcode == OP_FILL_OVAL)
						System.out.println("fillOval: " + lastRectangle);
					if (verbose && opcode == OP_FRAME_SAME_OVAL)
						System.out.println("frameSameOval: " + lastRectangle);
					if (verbose && opcode == OP_PAINT_SAME_OVAL)
						System.out.println("paintSameOval: " + lastRectangle);
					if (verbose && opcode == OP_ERASE_SAME_OVAL)
						System.out.println("eraseSameOval: " + lastRectangle);
					if (verbose && opcode == OP_INVERT_SAME_OVAL)
						System.out.println("invertSameOval: " + lastRectangle);
					if (verbose && opcode == OP_FILL_SAME_OVAL)
						System.out.println("fillSameOval: " + lastRectangle);

					// Oval treatments finished.
					break;

				case 0x35:
				case 0x36:
				case 0x37:
				case 0x45:
				case 0x46:
				case 0x47:
				case 0x55:
				case 0x56:
				case 0x57:
					fillByteBuf(bis, buf8, 0, 8);
					byte_cnt += 8;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				// --------------------------------------------------------------------------------
				// Arc treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_ARC: // OK, not tested.
				case OP_PAINT_ARC: // OK, not tested.
				case OP_ERASE_ARC: // OK, not tested.
				case OP_INVERT_ARC: // OK, not tested.
				case OP_FILL_ARC: // OK, not tested.
					// Get the frame rectangle.
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					setRectFromBuf(rect_buf, lastRectangle);

					// Get start and end angles.
					fillByteBuf(bis, point_word_buf, 0, point_word_buf.length);
					byte_cnt += point_word_buf.length;
					setPointFromBuf(point_word_buf, arc_angles);

				case OP_FRAME_SAME_ARC: // OK, not tested.
				case OP_PAINT_SAME_ARC: // OK, not tested.
				case OP_ERASE_SAME_ARC: // OK, not tested.
				case OP_INVERT_SAME_ARC: // OK, not tested.
				case OP_FILL_SAME_ARC: // OK, not tested.

					// Draw.
					if (drawGraphics != null) {
						switch (opcode) {
						case OP_FRAME_ARC:
						case OP_FRAME_SAME_ARC:
							drawGraphics.drawArc(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, arc_angles.x,
									arc_angles.y);
							break;
						case OP_PAINT_ARC:
						case OP_PAINT_SAME_ARC:
							drawGraphics.fillArc(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, arc_angles.x,
									arc_angles.y);
							break;
						case OP_ERASE_ARC:
						case OP_ERASE_SAME_ARC:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillArc(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, arc_angles.x,
									arc_angles.y);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_ARC:
						case OP_INVERT_SAME_ARC:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillArc(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, arc_angles.x,
									arc_angles.y);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_ARC:
						case OP_FILL_SAME_ARC:
							drawGraphics.fillArc(lastRectangle.x,
									lastRectangle.y, lastRectangle.width,
									lastRectangle.height, arc_angles.x,
									arc_angles.y);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_ARC)
						System.out.println("frameArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_PAINT_ARC)
						System.out.println("paintArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_ERASE_ARC)
						System.out.println("eraseArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_INVERT_ARC)
						System.out.println("invertArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_FILL_ARC)
						System.out.println("fillArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_FRAME_SAME_ARC)
						System.out.println("frameSameArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_PAINT_SAME_ARC)
						System.out.println("paintSameArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_ERASE_SAME_ARC)
						System.out.println("eraseSameArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_INVERT_SAME_ARC)
						System.out.println("invertSameArc: " + lastRectangle
								+ ", angles:" + arc_angles);
					if (verbose && opcode == OP_FILL_SAME_ARC)
						System.out.println("fillSameArc: " + lastRectangle
								+ ", angles:" + arc_angles);

					// Arc treatments finished.
					break;

				case 0x65:
				case 0x66:
				case 0x67:
					fillByteBuf(bis, new byte[12], 0, 12);
					byte_cnt += 12;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				// --------------------------------------------------------------------------------
				// Polygon treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_POLY: // OK.
				case OP_PAINT_POLY: // OK.
				case OP_ERASE_POLY: // OK, not tested.
				case OP_INVERT_POLY: // OK, not tested.
				case OP_FILL_POLY: // OK, not tested.
					// Read the polygon.
					a_polygon = readPoly(bis, bounds);

				case OP_FRAME_SAME_POLY: // OK, not tested.
				case OP_PAINT_SAME_POLY: // OK, not tested.
				case OP_ERASE_SAME_POLY: // OK, not tested.
				case OP_INVERT_SAME_POLY: // OK, not tested.
				case OP_FILL_SAME_POLY: // OK, not tested.

					// Draw.
					if (drawGraphics != null && a_polygon != null
							&& a_polygon.npoints > 1) {
						switch (opcode) {
						case OP_FRAME_POLY:
						case OP_FRAME_SAME_POLY:
							// Frame the polygon. If the start and end points
							// are not the same,
							// then draw a polyline instead.
							if (a_polygon.xpoints[0] == a_polygon.xpoints[a_polygon.npoints - 1]
									&& a_polygon.ypoints[0] == a_polygon.ypoints[a_polygon.npoints - 1]) {
								drawGraphics.drawPolygon(a_polygon);
							} else {
								drawGraphics.drawPolyline(a_polygon.xpoints,
										a_polygon.ypoints, a_polygon.npoints);
							}
							break;
						case OP_PAINT_POLY:
						case OP_PAINT_SAME_POLY:
							drawGraphics.fillPolygon(a_polygon);
							break;
						case OP_ERASE_POLY:
						case OP_ERASE_SAME_POLY:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillPolygon(a_polygon);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_POLY:
						case OP_INVERT_SAME_POLY:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillPolygon(a_polygon);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_POLY:
						case OP_FILL_SAME_POLY:
							drawGraphics.fillPolygon(a_polygon);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_POLY)
						verbosePolyCmd("framePoly", bounds, a_polygon);
					if (verbose && opcode == OP_PAINT_POLY)
						verbosePolyCmd("paintPoly", bounds, a_polygon);
					if (verbose && opcode == OP_ERASE_POLY)
						verbosePolyCmd("erasePoly", bounds, a_polygon);
					if (verbose && opcode == OP_INVERT_POLY)
						verbosePolyCmd("invertPoly", bounds, a_polygon);
					if (verbose && opcode == OP_FILL_POLY)
						verbosePolyCmd("fillPoly", bounds, a_polygon);
					if (verbose && opcode == OP_FRAME_SAME_POLY)
						verbosePolyCmd("frameSamePoly", bounds, a_polygon);
					if (verbose && opcode == OP_PAINT_SAME_POLY)
						verbosePolyCmd("paintSamePoly", bounds, a_polygon);
					if (verbose && opcode == OP_ERASE_SAME_POLY)
						verbosePolyCmd("eraseSamePoly", bounds, a_polygon);
					if (verbose && opcode == OP_INVERT_SAME_POLY)
						verbosePolyCmd("invertSamePoly", bounds, a_polygon);
					if (verbose && opcode == OP_FILL_SAME_POLY)
						verbosePolyCmd("fillSamePoly", bounds, a_polygon);

					// Polygon treatments finished.
					break;

				case 0x75:
				case 0x76:
				case 0x77:
					// Read the polygon.
					a_polygon = readPoly(bis, bounds);
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				// --------------------------------------------------------------------------------
				// Region treatments.
				// --------------------------------------------------------------------------------
				case OP_FRAME_RGN: // OK, not tested.
				case OP_PAINT_RGN: // OK, not tested.
				case OP_ERASE_RGN: // OK, not tested.
				case OP_INVERT_RGN: // OK, not tested.
				case OP_FILL_RGN: // OK, not tested.
					// Read the region.
					a_region = readRegion(bis, bounds);

				case OP_FRAME_SAME_RGN: // OK, not tested.
				case OP_PAINT_SAME_RGN: // OK, not tested.
				case OP_ERASE_SAME_RGN: // OK, not tested.
				case OP_INVERT_SAME_RGN: // OK, not tested.
				case OP_FILL_SAME_RGN: // OK, not tested.

					// Draw.
					if (drawGraphics != null && a_region != null
							&& a_region.npoints > 1) {
						switch (opcode) {
						case OP_FRAME_RGN:
						case OP_FRAME_SAME_RGN:
							drawGraphics.drawPolygon(a_region);
							break;
						case OP_PAINT_RGN:
						case OP_PAINT_SAME_RGN:
							drawGraphics.fillPolygon(a_region);
							break;
						case OP_ERASE_RGN:
						case OP_ERASE_SAME_RGN:
							drawGraphics.setColor(bgColor);
							drawGraphics.fillPolygon(a_region);
							drawGraphics.setColor(fgColor);
							break;
						case OP_INVERT_RGN:
						case OP_INVERT_SAME_RGN:
							drawGraphics.setXORMode(bgColor);
							drawGraphics.fillPolygon(a_region);
							drawGraphics.setPaintMode();
							break;
						case OP_FILL_RGN:
						case OP_FILL_SAME_RGN:
							drawGraphics.fillPolygon(a_region);
							break;
						}
					}

					// Do verbose mode output.
					if (verbose && opcode == OP_FRAME_RGN)
						verboseRegionCmd("frameRgn", bounds, a_region);
					if (verbose && opcode == OP_PAINT_RGN)
						verboseRegionCmd("paintRgn", bounds, a_region);
					if (verbose && opcode == OP_ERASE_RGN)
						verboseRegionCmd("eraseRgn", bounds, a_region);
					if (verbose && opcode == OP_INVERT_RGN)
						verboseRegionCmd("invertRgn", bounds, a_region);
					if (verbose && opcode == OP_FILL_RGN)
						verboseRegionCmd("fillRgn", bounds, a_region);
					if (verbose && opcode == OP_FRAME_SAME_RGN)
						verboseRegionCmd("frameSameRgn", bounds, a_region);
					if (verbose && opcode == OP_PAINT_SAME_RGN)
						verboseRegionCmd("paintSameRgn", bounds, a_region);
					if (verbose && opcode == OP_ERASE_SAME_RGN)
						verboseRegionCmd("eraseSameRgn", bounds, a_region);
					if (verbose && opcode == OP_INVERT_SAME_RGN)
						verboseRegionCmd("invertSameRgn", bounds, a_region);
					if (verbose && opcode == OP_FILL_SAME_RGN)
						verboseRegionCmd("fillSameRgn", bounds, a_region);

					// Region treatments finished.
					break;

				case 0x85:
				case 0x86:
				case 0x87:
					// Read the region.
					a_region = readRegion(bis, bounds);
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				case OP_BITS_RECT:
					if (verbose)
						System.out.println("opBitsRect");
					break;

				case OP_BITS_RGN:
					if (verbose)
						System.out.println("opBitsRgn");
					break;

				case 0x92:
				case 0x93:
				case 0x94:
				case 0x95:
				case 0x96:
				case 0x97:
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					data_len = getBytesAsInt(word_buf, 0, word_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				case OP_PACK_BITS_RECT:
					if (verbose)
						System.out.println("opPackBitsRect");
					break;

				case OP_PACK_BITS_RGN:
					if (verbose)
						System.out.println("opPackBitsRgn");
					break;

				case OP_DIRECT_BITS_RECT:

					if (verbose)
						System.out.println("opDirectBitsRect");

					// Get PixMap pointer (always 0x000000FF);
					fillByteBuf(bis, buf4, 0, 4);
					byte_cnt += 4;

					// Get rowBytes.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int row_bytes = getBytesAsInt(word_buf, 0, word_buf.length) & 0x3FFF;
					if (verbose) {
						System.out.print("opDirectBitsRect, rowBytes: "
								+ row_bytes);
						if ((word_buf[0] & 0x80) > 0)
							System.out.print(", it is a PixMap");
						else
							System.out.print(", it is a BitMap");
					}

					// Get bounds rectangle. THIS IS NOT TO BE SCALED BY THE
					// RESOLUTION!
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					bounds.setLocation(getBytesAsInt(rect_buf, 2, 4),
							getBytesAsInt(rect_buf, 0, 2));
					bounds.setSize(getBytesAsInt(rect_buf, 6, 8) - bounds.x,
							getBytesAsInt(rect_buf, 4, 6) - bounds.y);
					if (verbose)
						System.out.print(", bounds: " + bounds);

					// Get PixMap record version number.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int pm_version = getBytesAsInt(word_buf, 0, word_buf.length) & 0xFFFF;
					if (verbose)
						System.out.print(", pmVersion: " + pm_version);

					// Get packing format.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int pack_type = getBytesAsInt(word_buf, 0, word_buf.length) & 0xFFFF;
					if (verbose)
						System.out.print(", packType: " + pack_type);

					// Get size of packed data.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					int pack_size = getBytesAsInt(long_buf, 0, long_buf.length);
					if (verbose)
						System.out.println(", packSize: " + pack_size);

					// Get resolution info.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					int hres = getBytesAsInt(long_buf, 0, 2);
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					int vres = getBytesAsInt(long_buf, 0, 2);
					if (verbose)
						System.out.print("hRes: " + hres + ", vRes: " + vres);

					// Get pixel type.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int pixel_type = getBytesAsInt(word_buf, 0, word_buf.length);
					if (verbose) {
						if (pixel_type == 0)
							System.out.print(", indexed pixels");
						else
							System.out.print(", RGBDirect");
					}

					// Get pixel size.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int pixel_size = getBytesAsInt(word_buf, 0, word_buf.length);
					if (verbose)
						System.out.print(", pixelSize:" + pixel_size);

					// Get pixel component count.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int cmp_count = getBytesAsInt(word_buf, 0, word_buf.length);
					if (verbose)
						System.out.print(", cmpCount:" + cmp_count);

					// Get pixel component size.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					int cmp_size = getBytesAsInt(word_buf, 0, word_buf.length);
					if (verbose)
						System.out.println(", cmpSize:" + cmp_size);

					// planeBytes, ignored.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;

					// Handle to ColorTable record, there should be none for
					// direct
					// bits so 0.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;

					// Reserved.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;

					// Get source rectangle. We DO NOT scale the coordinates by
					// the
					// resolution info, since we are in pixmap coordinates here.
					src_rect = new Rectangle();
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					src_rect.setLocation(getBytesAsInt(rect_buf, 2, 4),
							getBytesAsInt(rect_buf, 0, 2));
					src_rect.setSize(
							getBytesAsInt(rect_buf, 6, 8) - src_rect.x,
							getBytesAsInt(rect_buf, 4, 6) - src_rect.y);
					if (verbose)
						System.out.print("opDirectBitsRect, srcRect:"
								+ src_rect);

					// Get destination rectangle. We DO scale the coordinates
					// according to
					// the image resolution, since we are working in display
					// coordinates.
					dst_rect = new Rectangle();
					fillByteBuf(bis, rect_buf, 0, rect_buf.length);
					byte_cnt += rect_buf.length;
					setRectFromBuf(rect_buf, dst_rect);
					if (verbose)
						System.out.print(", dstRect:" + dst_rect);

					// Get transfer mode.
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					transfer_mode = getBytesAsInt(word_buf, 0, word_buf.length);
					if (verbose)
						System.out.print(", mode: " + transfer_mode);

					// Set up pixel buffer for the RGB values.
					int img_buf_size = bounds.height * bounds.width;
					int[] pix_array = new int[img_buf_size];
					int pix_buf_off = 0;

					// Read in the RGB arrays.
					byte[] packed_bytes;
					byte[] dst_bytes = new byte[3 * bounds.width];
					for (int scanline = 0; scanline < bounds.height; scanline++) {

						// Get byteCount of the scanline.
						if (row_bytes > 250) {
							fillByteBuf(bis, word_buf, 0, word_buf.length);
							byte_cnt += word_buf.length;
							packed_bytes_cnt = getBytesAsInt(word_buf, 0,
									word_buf.length);
						} else {
							fillByteBuf(bis, byte_buf, 0, byte_buf.length);
							byte_cnt += byte_buf.length;
							packed_bytes_cnt = getBytesAsInt(byte_buf, 0,
									byte_buf.length);
						}
						if (verbose) {
							System.out.println();
							System.out.print("Line " + scanline
									+ ", byteCount: " + packed_bytes_cnt);
						}

						// Read in the scanline.
						packed_bytes = new byte[packed_bytes_cnt];
						fillByteBuf(bis, packed_bytes, 0, packed_bytes.length);
						byte_cnt += packed_bytes.length;

						// Unpack them all.
						int nbr_dst_bytes = DataCompression
								.unPackBits(packed_bytes, dst_bytes, 0,
										packed_bytes.length);

						// Set alpha values to all opaque.
						for (i = 0; i < bounds.width; i++) {
							pix_array[pix_buf_off + i] = 0xFF000000;
						}

						// Get red values.
						for (i = 0; i < bounds.width; i++) {
							pix_array[pix_buf_off + i] |= (dst_bytes[i] & 0xFF) << 16;
						}

						// Get green values.
						for (i = 0; i < bounds.width; i++) {
							pix_array[pix_buf_off + i] |= (dst_bytes[bounds.width
									+ i] & 0xFF) << 8;
						}

						// Get blue values.
						for (i = 0; i < bounds.width; i++) {
							pix_array[pix_buf_off + i] |= (dst_bytes[2
									* bounds.width + i] & 0xFF);
						}

						// Increment pixel buffer offset.
						pix_buf_off += bounds.width;
					}

					// See if we already have a vector for storing the images.
					if (imgVector == null) {
						imgVector = new Vector();
					}

					// We add all new images to it. If we are just replaying,
					// then
					// "pixmap_cnt" will never be greater than the size of the
					// vector.
					if (imgVector.size() <= pixmap_cnt) {
						// Create the MemoryImage and add it to the vector.
						img = new BufferedImage(bounds.width, bounds.height,
								BufferedImage.TYPE_INT_ARGB);
						img.setRGB(0, 0, bounds.width, bounds.height,
								pix_array, 0, bounds.width);
						imgVector.addElement(img);
					}

					// Display the image.
					img = (BufferedImage) imgVector.elementAt(pixmap_cnt);
					if (img != null && drawGraphics != null) {
						System.out.println(drawGraphics.drawImage(img,
								dst_rect.x, dst_rect.y, dst_rect.x
										+ dst_rect.width, dst_rect.y
										+ dst_rect.height, src_rect.x,
								src_rect.y, src_rect.x + src_rect.width,
								src_rect.y + src_rect.height, null));
					}

					// Line break at the end.
					if (verbose)
						System.out.println();
					break;

				case OP_DIRECT_BITS_RGN:
					if (verbose)
						System.out.println("opDirectBitsRgn");
					break;

				case 0x9C:
				case 0x9D:
				case 0x9E:
				case 0x9F:
					fillByteBuf(bis, word_buf, 0, word_buf.length);
					byte_cnt += word_buf.length;
					data_len = getBytesAsInt(word_buf, 0, word_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				case OP_SHORT_COMMENT: // NOTHING TO DO, JUST JUMP OVER.
					fillByteBuf(bis, buf2, 0, 2);
					if (verbose)
						System.out.println("Short comment");
					byte_cnt += 2;
					break;

				case OP_LONG_COMMENT: // NOTHING TO DO, JUST JUMP OVER.
					if (!readLongComment(bis))
						return false;
					if (verbose)
						System.out.println("Long comment");
					break;

				// WE DON'T CARE ABOUT CODES 0xA2 to 0xFE, even if it might be
				// needed.

				case OP_END_OF_PICTURE: // OK.
					break;

				// WE DON'T CARE ABOUT CODES 0x100 to 0x2FE, even if it might be
				// needed.

				// WE DON'T CARE ABOUT CODES 0x300 to 0xBFF, even if it might be
				// needed.

				// WE DON'T CARE ABOUT CODES 0xC01 to 0x81FF, even if it might
				// be needed.

				case OP_COMPRESSED_QUICKTIME:
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					data_len = getBytesAsInt(long_buf, 0, long_buf.length);
					byte[] qc = new byte[data_len];
					fillByteBuf(bis, qc, 0, data_len);
					byte_cnt += data_len;
					int jump = getBytesAsInt(qc, 64, 64 + 4);
					System.out.println("j1=" + jump);
					int n = 64 + jump + 4;
					jump = getBytesAsInt(qc, n, n + 4);
					// FileOutputStream out = new
					// FileOutputStream("/Users/fabio/Desktop/img.raw");
					System.out.println("j2=" + jump);
					String at = "" + (char) qc[n + 4] + (char) qc[n + 5]
							+ (char) qc[n + 6] + (char) qc[n + 7];
					System.out.println("Atom type: " + at);
					// out.write(qc,n+jump,qc.length - (n+jump));
					BufferedImage im = null;
					int w = getBytesAsInt(qc, n + 32, n + 32 + 2);
					int h = getBytesAsInt(qc, n + 34, n + 34 + 2);
					System.out.println("Uncompressed image size: w " + w
							+ " h " + h);
					if (at.startsWith("raw")) {
						int ncomponents = (qc.length - (n + jump)) / (w * h);
						System.out.println("Components:" + ncomponents);
						if (ncomponents == 3)
							img = new BufferedImage(w, h,
									BufferedImage.TYPE_INT_RGB);
						else
							img = new BufferedImage(w, h,
									BufferedImage.TYPE_INT_ARGB);
						int l2 = (qc.length - (n + jump)) / ncomponents;
						pix_array = new int[l2];
						int r, pix;
						for (int k = 0; k < l2; k++) {
							r = k * ncomponents;

							pix = qc[n + jump + r] & 0xFF;
							pix = (pix << 8) | (qc[n + jump + r + 1] & 0xFF);
							pix = (pix << 8) | (qc[n + jump + r + 2] & 0xFF);
							if (ncomponents == 4)
								pix = (pix << 8)
										| (qc[n + jump + r + 3] & 0xFF);
							pix_array[k] = pix;
						}
						img.setRGB(0, 0, w, h, pix_array, 0, w);
						drawGraphics.drawImage(img, null, 0, 0);
						System.out.println("raw image");

					} else
						im = ImageIO.read(new ByteArrayInputStream(qc,
								n + jump, qc.length - (n + jump)));

					if (im != null)
						drawGraphics.drawImage(im, new AffineTransformOp(
								AffineTransform.getScaleInstance(
										screenImageXRatio, screenImageYRatio),
								AffineTransformOp.TYPE_NEAREST_NEIGHBOR), 0, 0);
					else
						System.out.println("Unable to decode");
					if (verbose)
						System.out.println("opCompressedQuickTime");
					opCompressedQuickTime = true;
					break;

				case OP_UNCOMPRESSED_QUICKTIME: // JUST JUMP OVER.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					data_len = getBytesAsInt(long_buf, 0, long_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println("opUnCompressedQuickTime");
					break;

				case 0xFFFF: // JUST JUMP OVER.
					fillByteBuf(bis, long_buf, 0, long_buf.length);
					byte_cnt += long_buf.length;
					data_len = getBytesAsInt(long_buf, 0, long_buf.length);
					fillByteBuf(bis, new byte[data_len], 0, data_len);
					byte_cnt += data_len;
					if (verbose)
						System.out.println("opUnCompressedQuickTime");
					break;

				case 0x7F00:
				case 0x7FFF:
					fillByteBuf(bis, new byte[254], 0, 254);
					byte_cnt += 254;
					if (verbose)
						System.out.println(APPLE_USE_RESERVED_FIELD_STR);
					break;

				default:
					System.out.println("Found unknown opcode: "
							+ Integer.toHexString(opcode));
					if (opcode <= 0x00af) {
						fillByteBuf(bis, word_buf, 0, word_buf.length);
						byte_cnt += word_buf.length;
						data_len = getBytesAsInt(word_buf, 0, word_buf.length);
						fillByteBuf(bis, new byte[data_len], 0, data_len);
						byte_cnt += data_len;
					} else if (opcode <= 0x00fe) {
				
						fillByteBuf(bis, buf4, 0, 4);
						byte_cnt += 4;
					} else if (opcode == 0x00ff) {
						fillByteBuf(bis, buf2, 0, 2);
						byte_cnt += 2;
					} else if (opcode <= 0x01ff) {
						fillByteBuf(bis, buf2, 0, 2);
						byte_cnt += 2;
					} else if (opcode <= 0x0bfe) {
						fillByteBuf(bis, buf4, 0, 4);
						byte_cnt += 4;
					} else if (opcode <= 0x0bff) {
						fillByteBuf(bis, new byte[22], 0, 22);
						byte_cnt += 22;
					} else if (opcode == 0x0c00) {
						fillByteBuf(bis, new byte[24], 0, 24);
						byte_cnt += 24;
					} // HeaderOp
					else if (opcode <= 0x7eff) {
						fillByteBuf(bis, new byte[24], 0, 24);
						byte_cnt += 24;
					} else {
						fillByteBuf(bis, long_buf, 0, long_buf.length);
						byte_cnt += long_buf.length;
						data_len = getBytesAsInt(long_buf, 0, long_buf.length);
						fillByteBuf(bis, new byte[data_len], 0, data_len);
						byte_cnt += data_len;
					}

					break;
				// return false;
				}
			}
		} while (opcode != OP_END_OF_PICTURE);

		// Got it all, return true.
		return true;
	}

	private Color readColor(byte[] longBuf) {
		int color = getBytesAsInt(longBuf, 0, 4);
		switch (color) {
		case 33:
			return Color.black;
		case 30:
			return Color.white;
		case 205:
			return Color.red.brighter();
		case 341:
			return Color.green.brighter();
		case 409:
			return Color.blue.brighter();
		case 273:
			return Color.cyan.brighter();
		case 137:
			return Color.magenta.brighter();
		case 69:
			return Color.yellow;
		default:
			return Color.lightGray;
		}

	}

	// =============================================================================
	// setPointFromBuf
	// =============================================================================
	/**
	 * Set the point location and size to the values in the 4-byte point buffer.
	 * 
	 * @author Kary FR&Auml;MLING 8/4/1998
	 */
	// =============================================================================
	private void setPointFromBuf(byte[] pointBuf, Point dstPoint) {
		dstPoint.setLocation(getXPtCoord(getBytesAsInt(pointBuf, 2, 4)),
				getYPtCoord(getBytesAsInt(pointBuf, 0, 2)));
	}

	// =============================================================================
	// setRectFromBuf
	// =============================================================================
	/**
	 * Set the rectangle location and size to the values in the 8-byte rectangle
	 * buffer.
	 * 
	 * @author Kary FR&Auml;MLING 8/4/1998
	 */
	// =============================================================================
	private void setRectFromBuf(byte[] rectBuf, Rectangle destRect) {
		int x, y;

		x = getBytesAsInt(rectBuf, 2, 4);
		y = getBytesAsInt(rectBuf, 0, 2);
		destRect.setLocation(getXPtCoord(x), getYPtCoord(y));
		destRect.setSize(getXPtCoord(getBytesAsInt(rectBuf, 6, 8) - x),
				getYPtCoord(getBytesAsInt(rectBuf, 4, 6) - y));
	}

	// =============================================================================
	// readRegion
	// =============================================================================
	/**
	 * Read in a region. The inputstream should be positioned at the first byte
	 * of the region. boundsRect is a rectangle that will be set to the region
	 * bounds. The point array may therefore be empty if the region is just a
	 * rectangle.
	 * 
	 * @author Kary FR&Auml;MLING 2/4/1998
	 */
	// =============================================================================
	private Polygon readRegion(BufferedInputStream bis, Rectangle boundsRect) {
		int i, rgn_size, nbr_points, start_ind;
		byte[] region = new byte[MIN_REGION_SIZE];
		byte[] rgn;
		Polygon ret_poly;

		try {
			// Get minimal region.
			fillByteBuf(bis, region, 0, region.length);

			// Get region data size.
			rgn_size = getBytesAsInt(region, 0, NBR_BYTES_IN_WORD);

			// Get region bounds.
			start_ind = NBR_BYTES_IN_WORD;
			boundsRect.setLocation(getXPtCoord(bytesAsSignedInt(region,
					start_ind + NBR_BYTES_IN_WORD, start_ind + 2
							* NBR_BYTES_IN_WORD)),
					getYPtCoord(bytesAsSignedInt(region, start_ind, start_ind
							+ NBR_BYTES_IN_WORD)));
			start_ind += 2 * NBR_BYTES_IN_WORD;
			boundsRect.setSize(getXPtCoord(bytesAsSignedInt(region, start_ind
					+ NBR_BYTES_IN_WORD, start_ind + 2 * NBR_BYTES_IN_WORD))
					- boundsRect.getLocation().x, getYPtCoord(bytesAsSignedInt(
					region, start_ind, start_ind + NBR_BYTES_IN_WORD))
					- boundsRect.getLocation().y);

			// Initialize the point array to the right size.
			start_ind += 2 * NBR_BYTES_IN_WORD;
			nbr_points = (rgn_size - start_ind) / (2 * NBR_BYTES_IN_WORD);

			// Get the rest of the polygon points.
			ret_poly = new Polygon();
			rgn = new byte[nbr_points * 2 * NBR_BYTES_IN_WORD];
			fillByteBuf(bis, rgn, 0, rgn.length);
			start_ind = 0;
			for (i = 0; i < nbr_points; i++) {
				ret_poly.addPoint(
						getXPtCoord(bytesAsSignedInt(rgn, start_ind
								+ NBR_BYTES_IN_WORD, start_ind + 2
								* NBR_BYTES_IN_WORD)),
						getYPtCoord(bytesAsSignedInt(rgn, start_ind, start_ind
								+ NBR_BYTES_IN_WORD)));
				start_ind += 2 * NBR_BYTES_IN_WORD;
			}
		} catch (IOException e) {
			return null;
		}

		return ret_poly;
	}

	// =============================================================================
	// readPoly
	// =============================================================================
	/**
	 * Read in a polygon. The inputstream should be positioned at the first byte
	 * of the polygon.
	 * 
	 * @author Kary FR&Auml;MLING 2/4/1998
	 */
	// =============================================================================
	private Polygon readPoly(BufferedInputStream bis, Rectangle boundsRect) {
		int i, poly_size, nbr_points, start_ind;
		byte[] poly = new byte[MIN_POLY_SIZE];
		byte[] poly_points;
		Polygon ret_poly;

		try {
			// Get minimal polygon (a square) and its bounds.
			fillByteBuf(bis, poly, 0, poly.length);

			// Get polygon data size.
			poly_size = getBytesAsInt(poly, 0, NBR_BYTES_IN_WORD);

			// Get poly bounds.
			start_ind = NBR_BYTES_IN_WORD;
			boundsRect.setLocation(getXPtCoord(bytesAsSignedInt(poly, start_ind
					+ NBR_BYTES_IN_WORD, start_ind + 2 * NBR_BYTES_IN_WORD)),
					getYPtCoord(bytesAsSignedInt(poly, start_ind, start_ind
							+ NBR_BYTES_IN_WORD)));
			start_ind += 2 * NBR_BYTES_IN_WORD;
			boundsRect.setSize(getXPtCoord(bytesAsSignedInt(poly, start_ind
					+ NBR_BYTES_IN_WORD, start_ind + 2 * NBR_BYTES_IN_WORD))
					- boundsRect.getLocation().x, getYPtCoord(bytesAsSignedInt(
					poly, start_ind, start_ind + NBR_BYTES_IN_WORD))
					- boundsRect.getLocation().y);

			// Initialize the point array to the right size.
			start_ind += 2 * NBR_BYTES_IN_WORD;
			nbr_points = (poly_size - start_ind) / (2 * NBR_BYTES_IN_WORD);

			// Get the rest of the polygon points.
			ret_poly = new Polygon();
			poly_points = new byte[nbr_points * 2 * NBR_BYTES_IN_WORD];
			fillByteBuf(bis, poly_points, 0, poly_points.length);
			start_ind = 0;
			for (i = 0; i < nbr_points; i++) {
				ret_poly.addPoint(getXPtCoord(bytesAsSignedInt(poly_points,
						start_ind + NBR_BYTES_IN_WORD, start_ind + 2
								* NBR_BYTES_IN_WORD)),
						getYPtCoord(bytesAsSignedInt(poly_points, start_ind,
								start_ind + NBR_BYTES_IN_WORD)));
				start_ind += 2 * NBR_BYTES_IN_WORD;
			}
		} catch (IOException e) {
			return null;
		}

		return ret_poly;
	}

	// =============================================================================
	// readText
	// =============================================================================
	/**
	 * Read a text to draw. The inputstream should be positioned at the length
	 * byte of the text, which can thus be a maximum of 255 characters long.
	 * 
	 * @author Kary FR&Auml;MLING 13/4/1998
	 */
	// =============================================================================
	private String readText(BufferedInputStream bis) {
		byte[] count = new byte[1];
		byte[] text_bytes;

		try {
			// Comment kind and data byte count.
			fillByteBuf(bis, count, 0, count.length);

			// Get as many bytes as indicated by byte count.
			int text_byte_count = count[0];
			text_bytes = new byte[text_byte_count];
			fillByteBuf(bis, text_bytes, 0, text_byte_count);
		} catch (IOException e) {
			return null;
		}
		return new String(text_bytes);
	}

	// =============================================================================
	// readPixPat
	// =============================================================================
	/**
	 * Read PixPat. Read a PixPat data structure from the stream. Just returns
	 * void for the moment since not used in AWT graphics. NOT IMPLEMENTED YET!
	 * 
	 * @author Kary FR&Auml;MLING 17/5/1998
	 */
	// =============================================================================
	private void readPixPat(BufferedInputStream bis) {
		/*
		 * byte[] count = new byte[1]; byte[] text_bytes;
		 * 
		 * try { // Comment kind and data byte count. fillByteBuf(bis, count, 0,
		 * count.length);
		 * 
		 * // Get as many bytes as indicated by byte count. int text_byte_count
		 * = count[0]; text_bytes = new byte[text_byte_count]; fillByteBuf(bis,
		 * text_bytes, 0, text_byte_count); } catch ( IOException e ) { return
		 * null; } return new String(text_bytes);
		 */
	}

	// =============================================================================
	// readLongComment
	// =============================================================================
	/**
	 * Read in a long comment. The inputstream should be positioned just after
	 * the long comment opcode.
	 * 
	 * @author Kary FR&Auml;MLING 2/4/1998
	 */
	// =============================================================================
	private boolean readLongComment(BufferedInputStream bis) {
		byte[] kind = new byte[NBR_BYTES_IN_WORD];
		byte[] count = new byte[NBR_BYTES_IN_WORD];

		try {
			// Comment kind and data byte count.
			fillByteBuf(bis, kind, 0, kind.length);
			fillByteBuf(bis, count, 0, count.length);

			// Get as many bytes as indicated by byte count.
			int data_byte_count = getBytesAsInt(count, 0, count.length);
			fillByteBuf(bis, new byte[data_byte_count], 0, data_byte_count);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	// =============================================================================
	// getXPtCoord
	// =============================================================================
	/**
	 * Return the X coordinate value in display coordinates for the given
	 * coordinate value. This means multiplying it with the screen resolution/
	 * image resolution ratio.
	 * 
	 * @author Kary FR&Auml;MLING 16/5/1998
	 */
	// =============================================================================
	private int getXPtCoord(int p) {
		return (int) (p * screenImageXRatio);
	}

	// =============================================================================
	// getYPtCoord
	// =============================================================================
	/**
	 * Return the Y coordinate value in display coordinates for the given
	 * coordinate value. This means multiplying it with the screen resolution/
	 * image resolution ratio.
	 * 
	 * @author Kary FR&Auml;MLING 16/5/1998
	 */
	// =============================================================================
	private int getYPtCoord(int p) {
		return (int) (p * screenImageYRatio);
	}

	// =============================================================================
	// getBytesAsInt
	// =============================================================================
	/**
	 * Return the int value given by the byte array in small-endian (?) order.
	 * 
	 * @author Kary FR&Auml;MLING 31/3/1998
	 */
	// =============================================================================
	private int getBytesAsInt(byte[] bytes, int start_offset, int end_offset) {
		int i, result;

		// AND bytes with 0xFF since bytes are actually treated as integers.
		result = bytes[start_offset] & 0xFF;
		for (i = start_offset + 1; i < bytes.length && i < end_offset; i++) {
			result = (result << 8) | (bytes[i] & 0xFF);
		}
		return result;
	}

	// =============================================================================
	// bytesAsSignedInt
	// =============================================================================
	/**
	 * Return the int value given by the byte array in small-endian (?) order.
	 * Here we interpret the first bit of the first byte as being a sign bit.
	 * 
	 * @author Kary FR&Auml;MLING 2/4/1998
	 */
	// =============================================================================
	private int bytesAsSignedInt(byte[] bytes, int start_offset, int end_offset) {
		int i, result;
		int max_minus;
		boolean is_negative = false;

		// See if positive or negative.
		if ((bytes[start_offset] & 0x80) != 0)
			is_negative = true;

		// Get absolute value.
		bytes[start_offset] &= 0x7F;
		result = getBytesAsInt(bytes, start_offset, end_offset);

		// If it is negative, then get negative value.
		if (is_negative) {
			max_minus = (int) Math.pow(2, (end_offset - start_offset) * 8 - 1);
			result -= max_minus;
		}

		return result;
	}

	// =============================================================================
	// fillByteBuf
	// =============================================================================
	/**
	 * Read bytes from the input stream until the requested number of bytes has
	 * been retreved. This may not always be the case with buffered readers, for
	 * instance, if their buffer runs empty. Throws an IOException if there is a
	 * problem somewhere, like running into an EOF before finished reading.
	 * 
	 * @author Kary FR&Auml;MLING 14/4/1998
	 */
	// =============================================================================
	private void fillByteBuf(InputStream is, byte[] buf, int off, int len)
			throws IOException {
		int bytes_read, byte_cnt = 0;

		while (len > 0) {
			if ((bytes_read = is.read(buf, off, len)) == -1)
				throw new IOException("Unexpected EOF");
			off += bytes_read;
			len -= bytes_read;
		}
	}

	// =============================================================================
	// verbosePolyCmd
	// =============================================================================
	/**
	 * Write out polygon command, bounds and points.
	 * 
	 * @author Kary FR&Auml;MLING 20/4/1998
	 */
	// =============================================================================
	private void verbosePolyCmd(String cmd, Rectangle bounds, Polygon poly) {
		int i;

		System.out
				.println(cmd
						+ ": "
						+ new Rectangle(bounds.x, bounds.y, bounds.width,
								bounds.height));
		System.out.print("Polygon points: ");
		for (i = 0; poly != null && i < poly.npoints - 1; i++) {
			System.out.print("(" + poly.xpoints[i] + "," + poly.ypoints[i]
					+ "), ");
		}
		if (poly != null && poly.npoints > 0)
			System.out.print("(" + poly.xpoints[i] + "," + poly.ypoints[i]
					+ ")");
		System.out.println();
	}

	// =============================================================================
	// verboseRegionCmd
	// =============================================================================
	/**
	 * Write out region command, bounds and points.
	 * 
	 * @author Kary FR&Auml;MLING 20/4/1998
	 */
	// =============================================================================
	private void verboseRegionCmd(String cmd, Rectangle bounds, Polygon poly) {
		int i;

		System.out
				.println(cmd
						+ ": "
						+ new Rectangle(bounds.x, bounds.y, bounds.width,
								bounds.height));
		System.out.print("Region points: ");
		for (i = 0; poly != null && i < poly.npoints - 1; i++) {
			System.out.print("(" + poly.xpoints[i] + "," + poly.ypoints[i]
					+ "), ");
		}
		if (poly != null && poly.npoints > 0)
			System.out.print("(" + poly.xpoints[i] + "," + poly.ypoints[i]
					+ ")");
		System.out.println();
	}

}
