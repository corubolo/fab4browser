package com.im.util;

import org.simplx.c.Pointer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Formatter;
import java.util.IdentityHashMap;
import java.util.Map;

public class ConvertToStruct {
    private static final Map<Class<?>, Integer> SIZES;
    private static boolean asUnion;

    static {
        SIZES = new IdentityHashMap<Class<?>, Integer>();
        SIZES.put(byte.class, 1);
        SIZES.put(char.class, 2);
        SIZES.put(short.class, 2);
        SIZES.put(int.class, 4);
        SIZES.put(long.class, 8);
        SIZES.put(float.class, 4);
        SIZES.put(double.class, 8);
    }

    /**
     * Run this class as a program.
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        asUnion = false;
        for (String arg : args) {
            if (arg.equals("-u")) {
                asUnion = true;
                continue;
            }

            Class orig = Class.forName(arg);
            Object instance = orig.newInstance();

            String name = orig.getSimpleName();

            System.out.printf("%n");
            System.out.printf("package %s;%n", orig.getPackage().getName());
            System.out.printf("%n");
            System.out.printf("import java.io.*;%n");
            System.out.printf("import java.nio.*;%n");
            System.out.printf("%n");
            System.out.printf("class %s extends CStruct {%n", name);

            StringBuilder buffers = new StringBuilder();
            Formatter bufInitCode = new Formatter(buffers);

            int totalSize = 0;
            for (Field field : orig.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                int size = sizeof(field, instance);
                fieldCode(field, instance, size, totalSize, bufInitCode);
                if (asUnion) {
                    totalSize = Math.max(size, totalSize);
                } else {
                    totalSize += size;
                }
            }

            boolean hasBuffers = buffers.length() != 0;
            System.out.printf("%n");
            System.out.printf("     static final int SIZEOF = %d;%n",
                    totalSize);
            System.out.printf("%n");
            System.out.printf("     %s(Pointer pointer) {%n", name);
            System.out.printf("        super(pointer, SIZEOF);%n");
            if (hasBuffers) {
                System.out.printf("        setupBuffers();%n");
            }
            System.out.printf("    }%n");
            System.out.printf("%n");
            System.out.printf("     %s() {%n", name);
            System.out.printf("        super(SIZEOF);%n");
            if (hasBuffers) {
                System.out.printf("        setupBuffers();%n");
            }
            System.out.printf("    }%n");
            if (hasBuffers) {
                System.out.printf("%n");
                System.out.printf(
                        "    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {%n");
                System.out.printf("        in.defaultReadObject();%n");
                System.out.printf("        setupBuffers();%n");
                System.out.printf("    }%n");
                System.out.printf("%n");
                System.out.printf("    private void setupBuffers() {%n");
                System.out.printf("        ByteBuffer bb;%n");
                System.out.printf("%s", buffers);
                System.out.printf("    }%n");
            }

            System.out.printf("}%n");
        }
    }

    private static void fieldCode(Field field, Object instance, int size,
            int offset, Formatter bufInitCode) throws IllegalAccessException {

        Class type = field.getType();
        String indent = "        ";
        String name = field.getName();
        String typeStr = type.getSimpleName();
        String bufAccess = bufAccessorType(type);
        String bufType = bufType(type);
        String bufName = null;
        String offsetName = name.toUpperCase() + "_OFFSET";
        String sizeName = "SIZEOF_" + name.toUpperCase();
        boolean isUserType = bufAccess == null && type != Pointer.class;

        if (asUnion) {
            offsetName = "0";
        }

        System.out.printf("%n");
        if (!asUnion) {
            System.out.printf("     static final int %s = %d;%n", offsetName,
                    offset);
        }
        System.out.printf("     static final int %s = %d;%n", sizeName, size);
        if (isUserType) {
            System.out.printf("    private %s %s;%n", typeStr, name);
        } else if (type.isArray()) {
            bufName = name + "Buf";
            System.out.printf("    private transient %sBuffer %s;%n", bufType,
                    bufName);
        }

        System.out.printf("%n");
        System.out.printf("     %s %s() {%n", typeStr, name);
        if (type == Pointer.class) {
            System.out.printf("%sreturn new Pointer(´pointer, %s);%n", indent,
                    offsetName);
        } else if (isUserType) {
            System.out.printf("%sreturn %s;%n", indent, name);
        } else if (type.isArray()) {
            System.out.printf("%sreturn %s.array();%n", indent, bufName);
        } else {
            System.out.printf("%sreturn ´buf.get%s(%s);%n", indent, bufAccess,
                    offsetName);
        }
        System.out.printf("    }%n");

        System.out.printf("%n");
        System.out.printf("     void %s(%s val) {%n", name, typeStr);
        if (type == Pointer.class) {
            System.out.printf("%snew Pointer(´pointer, %s).strcpy(val);%n",
                    indent, offsetName);
        } else if (isUserType) {
            System.out.printf("%s%s = val;%n", indent, name);
        } else if (type.isArray()) {
            System.out.printf("%s%s.put(val);%n", indent, bufName);
        } else {
            System.out.printf("%s´buf.put%s(%s, val);%n", indent, bufAccess,
                    offsetName);
        }
        System.out.printf("    }%n");

        if (!isUserType) {
            System.out.printf("%n");
            System.out.printf("     int pack_%s(Pointer p) {%n", name);
            System.out.printf("%s´buf.position(%s);%n", indent, offsetName);
            System.out.printf("%s´buf.get(p.bytes, p.pos, %s);%n", indent,
                    sizeName);
            System.out.printf("%sp.incr(%s);%n", indent, sizeName);
            System.out.printf("%sreturn %s;%n", indent, sizeName);
            System.out.printf("    }%n");
            System.out.printf("%n");
            System.out.printf("     int unpack_%s(Pointer p) {%n", name);
            System.out.printf("%s´buf.position(%s);%n", indent, offsetName);
            System.out.printf("%s´buf.put(p.bytes, p.pos, %s);%n", indent,
                    sizeName);
            System.out.printf("%sp.incr(%s);%n", indent, sizeName);
            System.out.printf("%sreturn %s;%n", indent, sizeName);
            System.out.printf("    }%n");
        }

        if (bufName != null) {
            bufInitCode.format("        ´buf.position(%s);%n", offsetName);
            int count;
            if (bufAccess.length() == 0) {
                count = size;
                bufInitCode.format("        %s = ´buf.slice();%n", bufName);
            } else {
                count = Array.getLength(field.get(instance));
                bufInitCode.format("        bb = ´buf.slice();%n");
                bufInitCode.format("        %s = bb.as%sBuffer();%n", bufName,
                        bufAccess);
            }
            bufInitCode.format("        %s.limit(%d);%n", bufName, count);
        }
    }

    private static String bufType(Class type) {
        String str = bufAccessorType(type);
        if (str == null) {
            return null;
        } else if (str.length() == 0) {
            return "Byte";
        } else {
            return str;
        }
    }

    private static String bufAccessorType(Class type) {
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == byte.class) {
            return "";
        }
        StringBuilder sb = new StringBuilder(type.getSimpleName());
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    private static int sizeof(Field field, Object instance)
            throws IllegalAccessException {

        Class<?> type = field.getType();
        return sizeof(type, field.get(instance));
    }

    private static int sizeof(Class<?> type, Object instance) {
        if (type.isArray()) {
            return sizeofArray(instance);
        } else if (type == Pointer.class) {
            Pointer p = (Pointer) instance;
            return p.bytes.length;
        } else {
            Integer size = SIZES.get(type);
            if (size != null) {
                return size;
            } else if (type.isPrimitive()) {
                throw new IllegalArgumentException(
                        "Unknown size for class " + type);
            } else {
                return 4;
            }
        }
    }

    private static int sizeofArray(Object array) {
        int length = Array.getLength(array);
        Class<?> componentType = array.getClass().getComponentType();
        if (length > 0) {
            return length * sizeof(componentType, Array.get(array, 0));
        } else {
            return length * sizeofArray(componentType);
        }
    }
}