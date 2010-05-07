package com.im.file;

import static com.im.file.Magic.*;
import com.im.util.CStruct;
import org.simplx.c.Pointer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

class ValueType extends CStruct {

    static final int MAXstring = 32;

    static final int SIZEOF_B = 1;

    byte b() {
        return _buf.get(0);
    }

    void b(byte val) {
        _buf.put(0, val);
    }

    static final int SIZEOF_H = 2;

    short h() {
        return _buf.getShort(0);
    }

    void h(short val) {
        _buf.putShort(0, val);
    }

    static final int SIZEOF_L = 4;

    int l() {
        return _buf.getInt(0);
    }

    void l(int val) {
        _buf.putInt(0, val);
    }

    static final int SIZEOF_Q = 8;

    long q() {
        return _buf.getLong(0);
    }

    void q(long val) {
        _buf.putLong(0, val);
    }

    static final int SIZEOF_HS = 2;
    private transient ByteBuffer hsBuf;

    byte[] hs() {
        return hsBuf.array();
    }

    void hs(byte[] val) {
        hsBuf.put(val);
    }

    static final int SIZEOF_HL = 4;
    private transient ByteBuffer hlBuf;

    byte[] hl() {
        return hlBuf.array();
    }

    void hl(byte[] val) {
        hlBuf.put(val);
    }

    static final int SIZEOF_HQ = 8;
    private transient ByteBuffer hqBuf;

    byte[] hq() {
        return hqBuf.array();
    }

    void hq(byte[] val) {
        hqBuf.put(val);
    }

    static final int SIZEOF_S = 32;

    Pointer s() {
        return new Pointer(_pointer, 0);
    }

    void s(Pointer val) {
        new Pointer(_pointer, 0).strcpy(val);
    }

    static final int SIZEOF_F = 4;

    float f() {
        return _buf.getFloat(0);
    }

    void f(float val) {
        _buf.putFloat(0, val);
    }

    static final int SIZEOF_D = 8;

    double d() {
        return _buf.getDouble(0);
    }

    void d(double val) {
        _buf.putDouble(0, val);
    }

    static final int SIZEOF = 32;

    ValueType(Pointer pointer) {
        super(pointer, SIZEOF);
        setupBuffers();
    }

    ValueType() {
        super(SIZEOF);
        setupBuffers();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setupBuffers();
    }

    private void setupBuffers() {
        ByteBuffer bb;
        _buf.position(0);
        hsBuf = _buf.slice();
        hsBuf.limit(2);
        _buf.position(0);
        hlBuf = _buf.slice();
        hlBuf.limit(4);
        _buf.position(0);
        hqBuf = _buf.slice();
        hqBuf.limit(8);
    }

    // The actual size of the actual union in the actual file(1) program, in bytes
    static final int SIZE = MAXstring;

    @SuppressWarnings({"UnusedDeclaration"})
    private static void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.defaultWriteObject();
    }

    void copyFrom(Pointer from, int nbytes) {
        System.arraycopy(from.bytes, from.pos, _pointer.bytes, _pointer.pos,
                nbytes);
    }

    long byteAt(int pos) {
        return _buf.get(pos) & 0xff;
    }

    void clear() {
        Arrays.fill(_pointer.bytes, _pointer.pos, SIZE, (byte) 0);
    }

    int convert(Magic m) {
        switch (m.type) {
        case FILE_BYTE:
            cvt_8(m);
            return 1;
        case FILE_SHORT:
            cvt_16(m);
            return 1;
        case FILE_LONG:
        case FILE_DATE:
        case FILE_LDATE:
            cvt_32(m);
            return 1;
        case FILE_QUAD:
        case FILE_QDATE:
        case FILE_QLDATE:
            cvt_64(m);
            return 1;
        case FILE_STRING:
        case FILE_BESTRING16:
        case FILE_LESTRING16: {
            /* Null terminate and eat *trailing* return */
            s().set(MAXstring - 1, '\0');
            return 1;
        }
        case FILE_PSTRING: {
            Pointer ptr1 = _pointer.copy(), ptr2 = ptr1.plus(1);
            int len = s().get();
            if (len >= MAXstring) {
                len = MAXstring - 1;
            }
            while (len-- != 0) {
                ptr1.setIncr(ptr2.getIncr());
            }
            ptr1.set('\0');
            return 1;
        }
        case FILE_BESHORT:
            h((short) (byteAt(0) << 8 | byteAt(1)));
            cvt_16(m);
            return 1;
        case FILE_BELONG:
        case FILE_BEDATE:
        case FILE_BELDATE:
            l((int) (byteAt(0) << 24 | byteAt(1) << 16 | byteAt(2) << 8 |
                    byteAt(3)));
            cvt_32(m);
            return 1;
        case FILE_BEQUAD:
        case FILE_BEQDATE:
        case FILE_BEQLDATE:
            q(byteAt(0) << 56 | byteAt(1) << 48 | byteAt(2) << 40 | byteAt(3) <<
                    32 | byteAt(4) << 24 | byteAt(5) << 16 | byteAt(6) << 8 |
                    byteAt(7));
            cvt_64(m);
            return 1;
        case FILE_LESHORT:
            h((short) (byteAt(1) << 8 | byteAt(0)));
            cvt_16(m);
            return 1;
        case FILE_LELONG:
        case FILE_LEDATE:
        case FILE_LELDATE:
            l((int) (byteAt(3) << 24 | byteAt(2) << 16 | byteAt(1) << 8 |
                    byteAt(0)));
            cvt_32(m);
            return 1;
        case FILE_LEQUAD:
        case FILE_LEQDATE:
        case FILE_LEQLDATE:
            q(byteAt(7) << 56 | byteAt(6) << 48 | byteAt(5) << 40 | byteAt(4) <<
                    32 | byteAt(3) << 24 | byteAt(2) << 16 | byteAt(1) << 8 |
                    byteAt(0));
            cvt_64(m);
            return 1;
        case FILE_MELONG:
        case FILE_MEDATE:
        case FILE_MELDATE:
            l((int) (byteAt(1) << 24 | byteAt(0) << 16 | byteAt(3) << 8 |
                    byteAt(2)));
            cvt_32(m);
            return 1;
        case FILE_FLOAT:
            cvt_float(m);
            return 1;
        case FILE_BEFLOAT:
            l((int) (byteAt(0) << 24 | byteAt(1) << 16 | byteAt(2) << 8 |
                    byteAt(3)));
            cvt_float(m);
            return 1;
        case FILE_LEFLOAT:
            l((int) (byteAt(3) << 24 | byteAt(2) << 16 | byteAt(1) << 8 |
                    byteAt(0)));
            cvt_float(m);
            return 1;
        case FILE_DOUBLE:
            cvt_double(m);
            return 1;
        case FILE_BEDOUBLE:
            q(byteAt(0) << 56 | byteAt(1) << 48 | byteAt(2) << 40 | byteAt(3) <<
                    32 | byteAt(4) << 24 | byteAt(5) << 16 | byteAt(6) << 8 |
                    byteAt(7));
            cvt_double(m);
            return 1;
        case FILE_LEDOUBLE:
            q(byteAt(7) << 56 | byteAt(6) << 48 | byteAt(5) << 40 | byteAt(4) <<
                    32 | byteAt(3) << 24 | byteAt(2) << 16 | byteAt(1) << 8 |
                    byteAt(0));
            cvt_double(m);
            return 1;
        case FILE_REGEX:
        case FILE_SEARCH:
        case FILE_DEFAULT:
            return 1;
        default:
            return 0;
        }
    }

    private static int DO_CVT(Magic m, int val) {
        if (m.num_mask != 0) {
            switch (m.mask_op & FILE_OPS_MASK) {
            case FILE_OPAND:
                val &= m.num_mask;
                break;
            case FILE_OPOR:
                val |= m.num_mask;
                break;
            case FILE_OPXOR:
                val ^= m.num_mask;
                break;
            case FILE_OPADD:
                val += m.num_mask;
                break;
            case FILE_OPMINUS:
                val -= m.num_mask;
                break;
            case FILE_OPMULTIPLY:
                val *= m.num_mask;
                break;
            case FILE_OPDIVIDE:
                val /= m.num_mask;
                break;
            case FILE_OPMODULO:
                val %= m.num_mask;
                break;
            }
        }
        if ((m.mask_op & FILE_OPINVERSE) != 0) {
            val = ~val;
        }
        return val;
    }

    private static long DO_CVT(Magic m, long val) {
        if (m.num_mask != 0) {
            switch (m.mask_op & FILE_OPS_MASK) {
            case FILE_OPAND:
                val &= m.num_mask;
                break;
            case FILE_OPOR:
                val |= m.num_mask;
                break;
            case FILE_OPXOR:
                val ^= m.num_mask;
                break;
            case FILE_OPADD:
                val += m.num_mask;
                break;
            case FILE_OPMINUS:
                val -= m.num_mask;
                break;
            case FILE_OPMULTIPLY:
                val *= m.num_mask;
                break;
            case FILE_OPDIVIDE:
                val /= m.num_mask;
                break;
            case FILE_OPMODULO:
                val %= m.num_mask;
                break;
            }
        }
        if ((m.mask_op & FILE_OPINVERSE) != 0) {
            val = ~val;
        }
        return val;
    }

    private void cvt_8(Magic m) {
        b((byte) DO_CVT(m, b()));
    }

    private void cvt_16(Magic m) {
        h((short) DO_CVT(m, h()));
    }

    private void cvt_32(Magic m) {
        l(DO_CVT(m, l()));
    }

    private void cvt_64(Magic m) {
        q(DO_CVT(m, q()));
    }

    private static double DO_CVT_2(Magic m, double val) {
        if (m.num_mask != 0) {
            switch (m.mask_op & FILE_OPS_MASK) {
            case FILE_OPADD:
                val += m.num_mask;
                break;
            case FILE_OPMINUS:
                val -= m.num_mask;
                break;
            case FILE_OPMULTIPLY:
                val *= m.num_mask;
                break;
            case FILE_OPDIVIDE:
                val /= m.num_mask;
                break;
            }
        }
        return val;
    }

    private void cvt_float(Magic m) {
        f((float) DO_CVT_2(m, f()));
    }

    private void cvt_double(Magic m) {
        d(DO_CVT_2(m, d()));
    }
}

//class ValueTypeStruct {
//    static final int MAXstring = 32;
//
//    // The actual size of the actual union in the actual file(1) program, in bytes
//    static final int SIZE = MAXstring;
//    byte b;
//    short h;
//    int l;
//    long q;
//    byte hs[] = new byte[2];        /* 2 bytes of a fixed-endian "short" */
//    byte hl[] = new byte[4];        /* 4 bytes of a fixed-endian "long" */
//    byte hq[] = new byte[8];        /* 8 bytes of a fixed-endian "quad" */
//    Pointer s = new Pointer(new byte[MAXstring]);
//    /* the search string or regex pattern */ float f;
//    double d;
//}
