package org.simplx.object;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class TestReflx {
    public static class Inner extends Reflx {
        public int val = 12;
    }

    public static class Reflected extends Reflx<Reflected> {
        public final int val;
        public final String strVal;
        public Reflected child;
        public Object date;

        public Reflected(int val) {
            this.val = val;
            strVal = Integer.toString(val);
        }
    }

    public static class BadClone implements Cloneable {
        public BadClone() {
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public Reflected clone() {
            throw new IllegalStateException("Testing");
        }
    }

    private static Reflected obj(int val) {
        return new Reflected(val);
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion", "ObjectEqualsNull"})
    @Test
    public void testEquals() {
        checkMethod("equals", Object.class);
        assertTrue(obj(1).equals(obj(1)));
        assertFalse(obj(1).equals(obj(-1)));

        Reflected r = obj(0);
        assertEquals(r, r);
        assertFalse(r.equals(null));
        assertFalse(Reflx.equalsReflx(null, r));
        assertTrue(Reflx.equalsReflx(null, null));

        assertFalse(Reflx.equalsReflx("string", 12), "different classes");
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion"})
    @Test
    public void testHashCode() {
        checkMethod("hashCode");
        assertTrue(obj(1).hashCode() == obj(1).hashCode());
        assertFalse(obj(1).hashCode() == obj(-1).hashCode());
        assertEquals(Reflx.hashCodeReflx(null), 0);
        assertEquals(Reflx.hashCodeCalc(), 0);
        assertEquals(Reflx.hashCodeCalc(null, null), 0);
    }

    @Test
    public void testToString() {
        checkMethod("toString");
        Reflected r = obj(1);
        assertEquals(r.toString(),
                "Reflected:val=1,strVal=1,child=null,date=null");

        r.child = obj(-1);
        assertEquals(r.toString(),
                "Reflected:val=1,strVal=1,child=Reflected:val=-1,strVal=-1,child=null,date=null,date=null");
        assertEquals(Reflx.toStringReflx(r, "->", ":", "|"),
                "Reflected->val:1|strVal:1|child:Reflected->val:-1|strVal:-1|child:null|date:null|date:null");

        // Make sure that "$" and previous is stripped out
        assertEquals(new Inner().toString(), "Inner:val=12");
        // Make sure that top-level class also works
        assertEquals(new Top().toString(), "Top:val=15");
    }

    @Test
    public void testLogger() {
        Reflected reflected = obj(13);
        assertSame(reflected.logger(), reflected.logger());
        assertSame(reflected.logger(), obj(12).logger());
        assertNotSame(reflected.logger(), new Reflected(15) {
            @Override
            public String toString() {
                return "xyz";
            }
        }.logger());
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion"})
    @Test
    public void testCompareTo() {
        class Holder {
            public final String str;

            Holder(String str) {
                this.str = str;
            }
        }

        checkMethod("compareTo", Object.class);
        assertTrue(obj(0).compareTo(obj(1)) < 0);
        assertTrue(obj(0).compareTo(obj(0)) == 0);
        assertTrue(obj(0).compareTo(obj(-1)) > 0);
        assertEquals(Reflx.compareToReflx(new Holder(null), new Holder(null)),
                0);
        assertTrue(Reflx.compareToReflx(new Holder(null), new Holder(
                "string")) < 0);
        assertTrue(Reflx.compareToReflx(new Holder("string"), new Holder(
                null)) > 0);
    }

    @Test
    public void testURLCompare() throws MalformedURLException {
        class URLHolder {
            public final URL url;

            URLHolder(String str) throws MalformedURLException {
                url = (str == null ? null : new URL(str));
            }
        }

        assertTrue(Reflx.compareToReflx(new URLHolder("http://google.com"),
                new URLHolder("http://yahoo.com")) < 0);
    }

    @Test
    public void testCopying() {
        checkMethod("clone");
        checkMethod("copy");
        checkMethod("deepCopy");

        Reflected base = obj(1);
        Reflected shallow = base.clone();
        assertEquals(shallow, base);
        assertNotSame(base, shallow);

        base.child = obj(-1);
        shallow = base.copy();
        assertEquals(shallow, base);
        assertSame(base.child, shallow.child);

        base.date = new Date();
        Reflected deep = base.deepCopy();
        assertEquals(deep, base);
        assertNotSame(base.child, deep.child);
        assertEquals(deep.child, base.child);
    }

    @Test
    public void testClonedException() {
        Reflected obj = obj(1);
        obj.date = new BadClone();
        try {
            obj.deepCopy();
        } catch (IllegalStateException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getCause().getMessage(), "Testing");
        }
    }

    /*
    * This is more like an assertion -- the tests assume this is true.
    */
    private static void checkMethod(String name, Class... paramTypes) {
        try {
            Method method = obj(1).getClass().getMethod(name, paramTypes);
            assertSame(method.getDeclaringClass(), Reflx.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            fail("Cannot find method " + name);
        }
    }
}