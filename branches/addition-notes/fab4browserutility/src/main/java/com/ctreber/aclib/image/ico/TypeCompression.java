package com.ctreber.aclib.image.ico;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration class for bitmap compression types.
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public final class TypeCompression {
	/**
	 * Maps type values to TypeCompression objects.
	 */
	private static final Map TYPES;

	/**
	 * Uncompressed (any BPP).
	 */
	public static final TypeCompression BI_RGB = new TypeCompression("BI_RGB",
			0, "Uncompressed (any BPP)");

	/**
	 * 8 Bit RLE Compression (8 BPP only).
	 */
	public static final TypeCompression BI_RLE8 = new TypeCompression(
			"BI_RLE8", 1, "8 Bit RLE Compression (8 BPP only)");

	/**
	 * 4 Bit RLE Compression (4 BPP only).
	 */
	public static final TypeCompression BI_RLE4 = new TypeCompression(
			"BI_RLE4", 2, "4 Bit RLE Compression (4 BPP only)");

	/**
	 * Uncompressed (16 & 32 BPP only).
	 */
	public static final TypeCompression BI_BITFIELDS = new TypeCompression(
			"BI_BITFIELDS", 3, "Uncompressed (16 & 32 BPP only)");

	static {
		TYPES = new HashMap();
		register(TypeCompression.BI_RGB);
		register(TypeCompression.BI_RLE8);
		register(TypeCompression.BI_RLE4);
		register(TypeCompression.BI_BITFIELDS);
	}

	private final int _value;

	private final String _name;

	private final String _comment;

	/**
	 * @param pName
	 * @param pValue
	 */
	// @PMD:REVIEWED:CallSuperInConstructor: by Chris on 06.03.06 10:29
	private TypeCompression(final String pName, final int pValue,
			final String pComment) {
		_name = pName;
		_value = pValue;
		_comment = pComment;

	}

	/**
	 * @param pType
	 */
	private static void register(final TypeCompression pType) {
		TypeCompression.TYPES.put(new Long(pType.getValue()), pType);
	}

	/**
	 * Returns the name of the type and a comment.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return _name + " (" + _comment + ")";
	}

	/**
	 * Get the symbolic name.
	 * @return Returns the name.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Get the numerical value.
	 * @return Returns the value.
	 */
	public int getValue() {
		return _value;
	}

	/**
	 * Get a type for the specified numerical value.
	 * @param pValue
	 *            Compression type integer value.
	 * @return Type for the value specified.
	 */
	public static TypeCompression getType(final long pValue) {
		final TypeCompression lResult = (TypeCompression) TypeCompression.TYPES.get(new Long(
				pValue));
		if (lResult == null)
			throw new IllegalArgumentException("Compression type " + pValue
					+ " unknown");

		return lResult;
	}
}
