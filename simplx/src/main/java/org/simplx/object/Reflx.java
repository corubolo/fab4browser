/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.object;

import org.apache.commons.lang.ObjectUtils;
import org.simplx.logging.SimplxLogging;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class provides a base class and utility functions for objects,
 * implementing {@link #equals(Object)}, {@link #hashCode()}, {@link
 * #toString()}, {@link #compareTo(Object)} and {@link #clone} using reflection
 * on public fields. Also, {@link #copy()} and {@link #deepCopy()} methods are
 * also defined.
 * <p/>
 * Specific subclasses are declared to extend this using their own type. For
 * example
 * <pre>
 * class MyClass extends ObjectBase<MyClass> {
 *     ...
 * }
 * </pre>
 *
 * @author Ken Arnold
 * @param <T> The specific type of the subclass of {@code Reflx}.
 */
public class Reflx<T> implements Cloneable {
    @SuppressWarnings({"NonConstantLogger"})
    private Logger logger;

    private static final Map<Class, Field[]> storedFields =
            new HashMap<Class, Field[]>();

    /**
     * Returns {@code true} if this object is equal to that one. The types must
     * be the same, and the field objects must all be equal.
     *
     * @param that The object to compare to.
     *
     * @see #equalsReflx(Object,Object)
     */
    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object that) {
        return equalsReflx(this, that);
    }

    /**
     * Returns {@code true} if the two objects are equal, according to
     * reflection. If both are {@code null}, this method returns {@code true}.
     * If only one is {@code null}, this method returns {@code false}. If the
     * two objects are not of the same class, this method returns {@code
     * false}.
     * <p/>
     * Otherwise, each field of the class is examined and compared to the other
     * object's value for that field. If both are {@code null}, they are equal.
     * If only one is {@code null}, this method returns {@code false}.
     * Otherwise, the object's {@code equals} method is used to check equality.
     *
     * @param self The primary object.
     * @param that The other object.
     */
    @SuppressWarnings({"ObjectEquality"})
    public static boolean equalsReflx(final Object self, final Object that) {
        if (self == that)
            return true;
        if (that == null || self == null)
            return false;
        if (that.getClass() != self.getClass())
            return false;
        return visit(self, new Visitor<Field>() {
            @Override
            public boolean visit(Field field) {
                Object v1 = get(self, field);
                Object v2 = get(that, field);
                return ObjectUtils.equals(v1, v2);
            }
        });
    }

    /**
     * Returns a hash code for this object based on its fields' values.
     *
     * @see #hashCodeReflx(Object)
     */
    @Override
    public int hashCode() {
        return hashCodeReflx(this);
    }

    /**
     * Returns a hash code for an object based on its fields' values, using
     * reflection. Each field of the object is examined. If it is not {@code
     * null}, its {@link Object#hashCode} method is merged into the hash. If it
     * is, that is accounted for as well.
     * <p/>
     * If {@code self} is {@code null}, returns zero.
     *
     * @param self The object to calculate a hash code for.
     *
     * @see ObjectUtils#hashCode(Object)
     */
    public static int hashCodeReflx(final Object self) {
        if (self == null)
            return 0;

        final int[] h = {0};
        visit(self, new Visitor<Field>() {
            @Override
            public boolean visit(Field field) {
                Object val = get(self, field);
                int result = ObjectUtils.hashCode(val);
                h[0] = h[0] << 5 ^ result;
                return true;
            }
        });
        return h[0];
    }

    /**
     * Returns a hash code that is calculated from a list of values. This is
     * designed for classes that declare their own {@code hashCode} methods. A
     * typical way to use it would be:
     * <pre>
     * public class SomeClass {
     *     private String name;
     *     private double value;
     * &nbsp;
     *     public int hashCode() {
     *         return ReflectBase.hashCode(name, value);
     *     }
     * }
     * </pre>
     * Null values are incorporated into the hash.
     *
     * @param objs The objects to include.
     *
     * @see ObjectUtils#hashCode(Object)
     */
    public static int hashCodeCalc(Object... objs) {
        int h = 0;
        for (Object obj : objs) {
            h = h << 5 ^ ObjectUtils.hashCode(obj);
        }
        return h;
    }

    /**
     * Returns a string representation of this object. This will be the class
     * name, followed by the name and value of each field.
     */
    @Override
    public String toString() {
        return toStringReflx(this);
    }

    /**
     * Returns string for an object based on its fields' values, using
     * reflection. This is equivalent to
     * <pre>
     * {@link #toStringReflx(Object,String,String,String)
     * toStringReflx}(self, ":", "=", ",")
     * </pre>
     *
     * @param self The object to calculate a hash code for.
     *
     * @see #toStringReflx(Object,String,String,String)
     */
    public static String toStringReflx(Object self) {
        return toStringReflx(self, ":", "=", ",");
    }

    /**
     * Returns string for an object based on its fields' values, using
     * reflection. The object type name is followed by {@code afterType}. Then
     * each field of the object is added as a name/value pair with {@code
     * equals} between the name and value. Each name/value pair is separated
     * from the next by {@code sep}.
     * <p/>
     * For example, <tt>reflectToString(obj, ":", "=", ",")</tt> could generate
     * a string such as
     * <pre>
     * MyClass:x=12,y=15,z=0
     * </pre>
     * If a field refers to another object that is a subclass of {@code
     * ReflectBase} that has not overridden {@link Reflx#toString()}, this
     * object will also be converted to string with the same string arguments.
     *
     * @param self      The object to calculate a hash code for.
     * @param afterType The string to use after the object's type name.
     * @param equals    The string to use between each field's name and its
     *                  value.
     * @param sep       The separator to use between each name/value pair.
     */
    public static String toStringReflx(final Object self,
            final String afterType, final String equals, final String sep) {

        final StringBuilder sb = new StringBuilder(self.getClass().getName());
        int last = Math.max(sb.lastIndexOf("."), sb.lastIndexOf("$"));
        if (last > 0)
            sb.delete(0, last + 1);

        visit(self, new Visitor<Field>() {
            private String prefix = afterType;

            @Override
            public boolean visit(Field field) {
                // Skip static fields
                if (Modifier.isStatic(field.getModifiers()))
                    return true;

                try {
                    Object val = get(self, field);
                    sb.append(prefix);
                    sb.append(field.getName()).append(equals);
                    String str;
                    if (val == null) {
                        str = String.valueOf(val);
                    } else {
                        Method method = val.getClass().getMethod("toString");
                        if (method.getDeclaringClass().equals(Reflx.class)) {
                            Reflx rb = (Reflx) val;
                            str = toStringReflx(rb, afterType, equals, sep);
                        } else {
                            str = val.toString();
                        }
                    }
                    sb.append(str);
                    prefix = sep;
                    return true;
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        });
        return sb.toString();
    }

    /**
     * Returns a value less than, equal to, or greater than zero as this object
     * is less than, equal to, or greater than {@code that} object.
     *
     * @param that The other object.
     */
    @SuppressWarnings({"CovariantCompareTo"})
    public int compareTo(T that) {
        return compareToReflx(this, that);
    }

    /**
     * Returns a value less than, equal to, or greater than zero as one object
     * is less than, equal to, or greater than another object.
     * <p/>
     * Each field of the class is examined and compared to the other object's
     * value for that field. If both are {@code null}, they are equal. If either
     * is {@code null}, the {@code null} value is considered to be "lower than"
     * the non-{@code null} one. If both are not {@code null}, the first
     * object's {@link Comparable#compareTo} method is invoked to compare the
     * objects. Primitive fields, such as {@code int} and {@code boolean} are
     * simply compared according to {@link Integer#compareTo}, {@link
     * Boolean#compareTo}, etc.
     * <p/>
     * Every field of this object must be either primitive or a {@link
     * Comparable} type. Otherwise you will get a {@link ClassCastException}.
     *
     * @param self The primary object.
     * @param that The other object.
     */
    public static int compareToReflx(final Object self, final Object that) {
        final int[] c = {0};
        visit(self, new Visitor<Field>() {
            @Override
            @SuppressWarnings({"unchecked"})
            public boolean visit(Field field) {
                Comparable v1 = comparableFor(field, self);
                Comparable v2 = comparableFor(field, that);
                if (v1 == null && v2 == null)
                    return true;
                if (v1 == null)
                    c[0] = -1;
                else if (v2 == null)
                    c[0] = 1;
                else
                    c[0] = v1.compareTo(v2);
                return c[0] == 0;
            }
        });
        return c[0];
    }

    /**
     * Returns the object's value for the given field. If {@link Field#get}
     * throws an {@link IllegalAccessException}, this method throws a {@link
     * SecurityException} caused by that exception. This is used as a way to get
     * values where access problems are handled consistently and simpley.
     *
     * @param self  The object.
     * @param field The field in the object.
     */
    protected static Object get(Object self, Field field) {
        try {
            return field.get(self);
        } catch (IllegalAccessException e) {
            throw new SecurityException(e);
        }
    }

    /**
     * Sets the object's value for the given field. If {@link Field#set} throws
     * an {@link IllegalAccessException}, this method throws a {@link
     * SecurityException} caused by that exception.T his is used as a way to set
     * values where access problems are handled consistently and simpley.
     *
     * @param self  The object.
     * @param field The field in the object.
     * @param val   The new value for the field.
     */
    protected static void set(Object self, Field field, Object val) {
        try {
            field.set(self, val);
        } catch (IllegalAccessException e) {
            throw new SecurityException(e);
        }
    }

    private static Comparable comparableFor(Field field, Object obj) {
        Object o = get(obj, field);
        if (o instanceof URL) {
            try {
                return new URI(o.toString());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(
                        "Non-URI in URL \"" + o + "\"", e);
            }
        } else {
            return (Comparable) o;
        }
    }

    /**
     * Visits each field of the object. For each field of the object, the
     * visitor's {@link Visitor#visit} method is invoked. If that method returns
     * {@code false}, the visitation is stopped, and this method returns {@code
     * false}. Otherwise the visitation continues. If all fields of the object
     * are visited and return {@code true}, this method returns {@code true}.
     *
     * @param self    The object.
     * @param visitor The visiting object.
     *
     * @return {@code true} if all fields are visited, with the {@link
     *         Visitor#visit} returning {@code true}.
     */
    public static boolean visit(Object self, Visitor<Field> visitor) {
        Class<?> type = self.getClass();
        Field[] fields = storedFields.get(type);
        if (fields == null) {
            fields = type.getFields();
            storedFields.put(type, fields);
        }

        for (Field field : fields) {
            if (!visitor.visit(field))
                return false;
        }
        return true;
    }

    /**
     * Returns the logger to use for this object's logging.
     *
     * @see SimplxLogging#loggerFor(Object)
     */
    protected Logger logger() {
        if (logger == null)
            logger = SimplxLogging.loggerFor(this);
        return logger;
    }

    /**
     * Returns a shallow copy of this object.  Any object references in the copy
     * will refer to the same object as in the original.
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SecurityException(e);
        }
    }

    /**
     * Returns a shallow copy of this object.  Any object references in the copy
     * will refer to the same object as in the original.
     */
    @SuppressWarnings({"unchecked"})
    public T copy() {
        return clone();
    }

    /**
     * Returns a deep copy of this object.  This is like a shallow copy, except
     * that if a field refers to another {@code Reflx} object, that object's
     * {@code deepCopy()} method will be used to copy it.  Or if the object is
     * clonable, the {@link #clone} method will be used to copy it.
     */
    @SuppressWarnings({"unchecked", "JavaDoc"})
    public T deepCopy() {
        final T nobj = clone();
        Visitor<Field> visitor = new Visitor<Field>() {
            @Override
            public boolean visit(Field field) {
                Object val = get(nobj, field);
                if (val instanceof Reflx)
                    set(nobj, field, ((Reflx) val).deepCopy());
                else if (val != null && val instanceof Cloneable) {
                    try {
                        Method cm = val.getClass().getMethod("clone");
                        if (Modifier.isPublic(cm.getModifiers()))
                            set(nobj, field, cm.invoke(val));
                    } catch (NoSuchMethodException ignored) {
                        // simply means that clone isn't really available
                    } catch (IllegalAccessException ignored) {
                        // simply means that clone isn't really available
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof CloneNotSupportedException) {
                            // simply means that clone isn't really available
                        } else {
                            throw clonedException(cause, "deepCopy()", field);
                        }
                    }
                }
                return true;
            }
        };
        visit(this, visitor);
        return nobj;
    }

    private RuntimeException clonedException(Throwable thrown, String method,
            Field field) {

        String msg = method + ": Problem with field " + field;
        if (!(thrown instanceof RuntimeException))
            return new RuntimeException(msg, thrown);

        try {
            RuntimeException rt = (RuntimeException) thrown;
            Class<? extends RuntimeException> type = rt.getClass();
            Constructor<? extends RuntimeException> c = type.getConstructor(
                    String.class);
            RuntimeException n = c.newInstance(msg);
            n.initCause(thrown);
            return n;
        } catch (Exception ignored) {
            return new RuntimeException(msg, thrown);
        }
    }
}
