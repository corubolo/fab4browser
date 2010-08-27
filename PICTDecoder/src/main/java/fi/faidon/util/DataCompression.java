/*****************************************************************************
 *
 *                              DataCompression.java
 *
 * Class that provides utilities for data compressing/uncompressing.
 *
 * Created by Kary FRAMLING 4/4/1998
 *
 * Copyright 1998-2003 Kary Främling
 * Source code distributed under GNU LESSER GENERAL PUBLIC LICENSE,
 * included in the LICENSE.txt file in the topmost directory
 *
 *****************************************************************************/

package fi.faidon.util;

public class DataCompression {
    
    //--------------------------------------------------------------------------------------
    // Public methods.
    //--------------------------------------------------------------------------------------
    
    //=============================================================================
    // main
    //=============================================================================
    /**
     * Program entry point.
     *
     * @author Kary FR&Auml;MLING 4/4/1998
     */
    //=============================================================================
    public static void main(String[] argv) {
	int		i;
	byte[]	dst_bytes;
	String	byte_string;
	
	// Verify that we have a picture file name.
	if ( argv.length < 1 ) {
	    System.out.println("Usage: DataCompression <hex data>");
	    System.exit(1);
	}
	
	// See if we pack or unpack.
	if ( argv.length == 1 )
	    byte_string = argv[0];
	else
	    byte_string = argv[1];
	
	// Convert the hex number string into a byte array.
	byte[] bytes = new byte[byte_string.length()/2];
	for ( i = 0 ; i < byte_string.length()/2 ; i++ ) {
	    bytes[i] = Integer.valueOf(byte_string.substring(i*2, i*2 + 2), 16).byteValue();
	    System.out.print(Integer.toHexString(bytes[i] & 0xFF) + " ");
	    //			System.out.println(bytes[i]);
	}
	System.out.println();
	System.out.println(bytes.length + " bytes.");
	
	// Pack the data if we have only one argument, unpack if more.
	if ( argv.length == 1 ) {
	    // Pack the data.
	    dst_bytes = new byte[getPackBitsMaxDestBytes(bytes.length)];
	    int nbr_dst_bytes = DataCompression.packBits(bytes, dst_bytes, bytes.length);
	    for ( i = 0 ; i < nbr_dst_bytes ; i++ ) {
		System.out.print(Integer.toHexString(dst_bytes[i] & 0xFF) + " ");
	    }
	    System.out.println();
	}
	else {
	    // Unpack the data.
	    dst_bytes = new byte[5*127];
	    int nbr_dst_bytes = DataCompression.unPackBits(bytes, dst_bytes,
	    0, bytes.length);
	    for ( i = 0 ; i < nbr_dst_bytes ; i++ ) {
		System.out.print(Integer.toHexString(dst_bytes[i] & 0xFF) + " ");
	    }
	    System.out.println();
	}
    }
    
    //=============================================================================
    // getPackBitsMaxDestBytes
    //=============================================================================
    /**
     * Return the maximal possible buffer size needed for the destination byte
     * array of packBits.
     *
     * @author Kary FR&Auml;MLING 6/4/1998
     */
    //=============================================================================
    public static int getPackBitsMaxDestBytes(int nbrSrcBytes) {
	return nbrSrcBytes + (nbrSrcBytes + 126)/127;
    }
    
    //=============================================================================
    // packBits
    //=============================================================================
    /**
     * Pack "nbrSrcBytes" bytes of "srcBytes" into dstBytes
     * that should be big enough to hold the packed data (see
     * getPackBitsMaxDestBytes). The number of packed bytes is
     * returned.
     *
     * @author Kary FR&Auml;MLING 4/4/1998
     */
    //=============================================================================
    public static int packBits(byte[] srcBytes, byte[] dstBytes, int nbrSrcBytes) {
	int		i, max_dest_bytes, dest_ind;
	int		diff_count, ident_count, flag_byte;
	byte	pack_byte;
	
	// Go through all bytes and pack them.
	i = 0;
	flag_byte = 0;
	dest_ind = 1;
	diff_count = 0;
	while ( i < srcBytes.length && i < nbrSrcBytes ) {
	    // If we have three identical bytes, then pack them and the others that are
	    // identical.
	    if ( i + 2 < srcBytes.length && srcBytes[i] == srcBytes[i + 1] && srcBytes[i] == srcBytes[i + 2] ) {
		// Store the non-packed byte count. Remember that this is actually
		// the count - 1.
		if ( diff_count > 0 ) {
		    dstBytes[flag_byte] = (byte) (diff_count - 1);
		    diff_count = 0;
		    flag_byte = dest_ind;
		}
		
		// Continue as long as we have identical bytes.	Check that we never get
		// more than 127, however, since this would go over our counter capacity.
		pack_byte = srcBytes[i];
		ident_count = 3;
		i += 3;
		while ( ident_count < 127 && i < srcBytes.length && i < nbrSrcBytes && srcBytes[i] == pack_byte ) {
		    ident_count++;
		    i++;
		}
		
		// Store the identical bytes count and the byte to repeat.
		dstBytes[flag_byte] = (byte) ((129 - ident_count) | 0x80);
		dstBytes[flag_byte + 1] = pack_byte;
		
		// Store the place where to put the next count.
		flag_byte += 2;
		dest_ind = flag_byte + 1;
	    }
	    else {
		// We can't go over 127 bytes in a go, so put in a count and
		// restart if we do.
		if ( diff_count >= 127 ) {
		    dstBytes[flag_byte] = (byte) (diff_count - 1);
		    diff_count = 0;
		    flag_byte = dest_ind;
		    dest_ind++;
		}
		
		// No packing possible for this one, go on normally.
		dstBytes[dest_ind] = srcBytes[i];
		dest_ind++;
		diff_count++;
		i++;
	    }
	}
	
	// If we have unpacked data at the end, it hasn't been treated yet.
	if ( diff_count > 0 ) {
	    dstBytes[flag_byte] = (byte) (diff_count - 1);
	    flag_byte += diff_count + 1;
	}
	
	// "flag_byte" contains the number of backed bytes.
	return flag_byte;
    }
    
    //=============================================================================
    // unPackBits
    //=============================================================================
    /**
     * "srcBytes" is the packed byte array, to unpack from the byte "indSrcStart".
     * "dstBytes" is the unpacked bytes buffer, which is filled up from the first
     * byte and as far as possible. The return value is the number of bytes
     * actually unpacked. Since Java doesn't allow parameter passing by reference,
     * we have a one-element array "newInds" whose first element contains the
     * new start index to use if there remains some bytes to unpack.
     *
     * "nbrSrcBytes" is the maximum number of source bytes to treat. This is useful
     * if the packed data length is smaller than the source buffer size.
     *
     * "dstBytes" should be at least 127 bytes to always hold at least one
     * packed item. Having a bigger buffer will speed things up. It should be
     * a good idea to use a n*127 size buffer.
     * "indSrcStart" should point to a byte count byte.
     *
     * @author Kary FR&Auml;MLING 6/4/1998
     */
    //=============================================================================
    public static int unPackBits(byte[] srcBytes, byte[] dstBytes,
    int indSrcStart, int nbrSrcBytes) {
	int		i, src_ind, dest_ind;
	int		byte_count;
	byte	pack_byte;
	
	// Initialize counters and indices.
	src_ind = indSrcStart;
	dest_ind = 0;
	
	// Loop as long as we have bytes to unpack and as long as we have enough room
	// in the destination array.
	while ( src_ind < srcBytes.length && src_ind - indSrcStart < nbrSrcBytes ) {
	    
	    // See if we have a packed byte or raw bytes.
	    if ( (srcBytes[src_ind] & 0x80) > 0 ) {
		// Get byte count.
		byte_count = -((srcBytes[src_ind] & 0x7F) - 128) + 1;
		src_ind++;
		
		// Verify that it fits into destination array, return if not.
		if ( dest_ind + byte_count >= dstBytes.length ) return dest_ind;
		
		// Get the byte to repeat and then copy it into
		// destination array.
		pack_byte = srcBytes[src_ind];
		src_ind++;
		for ( i = 0 ; i < byte_count ; i++ ) {
		    dstBytes[dest_ind] = pack_byte;
		    dest_ind++;
		}
	    }
	    else {
		// Get byte count.
		byte_count = srcBytes[src_ind] + 1;
		src_ind++;
		
		// Verify that it fits into destination array, return if not.
		if ( dest_ind + byte_count >= dstBytes.length ) return dest_ind;
		
		// Copy as many bytes from the source array.
		for ( i = 0 ; i < byte_count ; i++ ) {
		    dstBytes[dest_ind] = srcBytes[src_ind];
		    src_ind++;
		    dest_ind++;
		}
	    }
	}
	
	return dest_ind;
    }
    
}

