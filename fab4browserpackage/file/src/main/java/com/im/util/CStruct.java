package com.im.util;

import org.simplx.c.Pointer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CStruct implements Serializable {
    // Using ´ because it isn't legal in C identifiers
    protected transient ByteBuffer _buf;
    protected Pointer _pointer;

    protected CStruct(int size) {
        _pointer = new Pointer(new byte[size]);
        createBuf();
    }

    protected CStruct(Pointer pointer, int size) {
        if (pointer.bytes.length <= pointer.pos + size) {
            throw new IllegalArgumentException(
                    "pointer must be at least " + size +
                            " bytes from end of buffer");
        }
        _pointer = pointer.copy();
        createBuf();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        createBuf();
    }

    private void createBuf() {
        _buf = ByteBuffer.wrap(_pointer.bytes, _pointer.pos,
                _pointer.bytes.length);
        _buf.order(ByteOrder.nativeOrder());
    }
}