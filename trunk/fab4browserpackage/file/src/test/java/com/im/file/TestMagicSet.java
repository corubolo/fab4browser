package com.im.file;

import static com.im.file.Magic.*;
import static com.im.file.MagicSet.MAGIC_DEBUG;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestMagicSet {
    int mflags;

    private static class OpTest {
        int flags;
        Number v;
        char reln;
        Number l;
        int expected;

        OpTest(int flags, Number v, char reln, Number l, int expected) {
            this.flags = flags;
            this.v = v;
            this.reln = reln;
            this.l = l;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return (flags == 0 ? "signed" : "unsigned") + ": " + v + " " +
                    reln + " " + l + " = " + expected + " / " + (expected == 1);
        }
    }

    abstract static class TypeSetup {
        private final String name;
        private final byte type;

        TypeSetup(String name, byte type) {
            this.name = name;
            this.type = type;
        }

        public String name() {
            return name;
        }

        public byte type() {
            return type;
        }

        abstract void setup(OpTest test, MagicSet ms, ValueType v, Magic m);

        @Override
        public String toString() {
            return name();
        }
    }

    private static final TypeSetup[] INTEGER_SETUPS = {
            new TypeSetup("byte", FILE_BYTE) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.b(test.v.byteValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.b(test.l.byteValue());
                }
            }, new TypeSetup("short", FILE_SHORT) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.h(test.v.shortValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.h(test.l.shortValue());
                }
            }, new TypeSetup("int", FILE_LONG) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.l(test.v.intValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.l(test.l.intValue());
                }
            }, new TypeSetup("long", FILE_QUAD) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.q(test.v.longValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.q(test.l.longValue());
                }
            },
    };
    private static final TypeSetup[] FLOATING_SETUPS = {
            new TypeSetup("float", FILE_FLOAT) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.f(test.v.floatValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.f(test.l.floatValue());
                }
            }, new TypeSetup("double", FILE_DOUBLE) {
                @Override
                public void setup(OpTest test, MagicSet ms, ValueType v,
                        Magic m) {
                    v.d(test.v.doubleValue());
                    ms.mget(v.s(), m, ValueType.SIZE, 0);
                    m.value.d(test.l.doubleValue());
                }
            },
    };

    private static final OpTest[] INTEGER_TESTS = {
            new OpTest(0, +0x6f, 'x', +0x6f, 1),

            new OpTest(0, +0x6f, 'x', -0x6f, 1),

            new OpTest(0, +0x6f, '=', +0x6f, 1),

            new OpTest(0, +0x6f, '=', -0x6f, 0),

            new OpTest(0, +0x6f, '<', +0x70, 1),

            new OpTest(0, +0x6f, '<', +0x6f, 0),

            new OpTest(0, +0x6f, '<', +0x6e, 0),

            new OpTest(0, +0x6f, '<', -0x6f, 0),

            new OpTest(0, +0x6f, '>', +0x70, 0),

            new OpTest(0, +0x6f, '>', +0x6f, 0),

            new OpTest(0, +0x6f, '>', +0x6e, 1),

            new OpTest(0, +0x6f, '>', -0x6f, 1),

            new OpTest(0, 0x6f, '&', 0x6f, 1),

            new OpTest(0, 0x6f, '&', 0xff, 0),

            new OpTest(UNSIGNED, +0x6f, 'x', +0x6f, 1),

            new OpTest(UNSIGNED, +0x6f, 'x', -0x6f, 1),

            new OpTest(UNSIGNED, +0x6f, '=', +0x6f, 1),

            new OpTest(UNSIGNED, +0x6f, '=', -0x6f, 0),

            new OpTest(UNSIGNED, +0x6f, '<', +0x70, 1),

            new OpTest(UNSIGNED, +0x6f, '<', +0x6f, 0),

            new OpTest(UNSIGNED, +0x6f, '<', +0x6e, 0),

            new OpTest(UNSIGNED, +0x6f, '<', -0x6f, 1),

            new OpTest(UNSIGNED, +0x6f, '>', +0x70, 0),

            new OpTest(UNSIGNED, +0x6f, '>', +0x6f, 0),

            new OpTest(UNSIGNED, +0x6f, '>', +0x6e, 1),

            new OpTest(UNSIGNED, +0x6f, '>', -0x6f, 0),

            new OpTest(UNSIGNED, 0x6f, '&', 0x6f, 1),

            new OpTest(UNSIGNED, 0x6f, '&', 0xff, 0),
    };

    private static final OpTest[] FLOATING_TESTS = {
            new OpTest(0, +2.7, 'x', +2.7, 1),

            new OpTest(0, +2.7, 'x', -2.7, 1),

            new OpTest(0, +2.7, '=', +2.7, 1),

            new OpTest(0, +2.7, '=', -2.7, 0),

            new OpTest(0, +2.7, '<', +2.8, 1),

            new OpTest(0, +2.7, '<', +2.7, 0),

            new OpTest(0, +2.7, '<', +2.6, 0),

            new OpTest(0, +2.7, '<', -2.7, 0),

            new OpTest(0, +2.7, '>', +2.8, 0),

            new OpTest(0, +2.7, '>', +2.7, 0),

            new OpTest(0, +2.7, '>', +2.6, 1),

            new OpTest(0, +2.7, '>', -2.7, 1),
    };

    @DataProvider(name = "allIntTests")
    public Object[][] allIntTests() {
        TypeSetup[] setups = INTEGER_SETUPS;
        OpTest[] testsObjs = INTEGER_TESTS;
        return TestingUtils.allCombos(setups, testsObjs);
    }

    @DataProvider(name = "allFloatTests")
    public Object[][] allFloatTests() {
        TypeSetup[] setups = FLOATING_SETUPS;
        OpTest[] testsObjs = FLOATING_TESTS;
        return TestingUtils.allCombos(setups, testsObjs);
    }

    @DataProvider(name = "allOpTests")
    public Object[][] allOpTests() {
        return (Object[][]) TestingUtils.splice(allIntTests(),
                allFloatTests());
    }

    @Test(dataProvider = "allFloatTests")
    public void testOps(TypeSetup setup, OpTest test) {
        mflags = test.flags;
        String desc = setup.name() + ": " + test;
        System.err.println("TEST: " + desc);
        MagicSet ms = new MagicSet();
        ms.flags |= MAGIC_DEBUG;
        ValueType v = new ValueType();
        Magic m = new Magic("test", 0);
        m.type = setup.type();
        setup.setup(test, ms, v, m);

        if ((m.flag & UNSIGNED) != 0) {
            desc += " (unsigned)";
        }
        m.reln = (byte) test.reln;
        m.flag = (byte) mflags;
        assertEquals(ms.magiccheck(m), test.expected, desc);

        // Some checks imply the value of another
        String nDesc;
        switch (test.reln) {
        case '=':
            m.reln = '!';
            nDesc = desc.replace('=', '!');
            assertEquals(ms.magiccheck(m), test.expected == 1 ? 0 : 1, nDesc);
            break;
        case '&':
            m.reln = '^';
            nDesc = desc.replace('&', '^');
            assertEquals(ms.magiccheck(m), test.expected == 1 ? 0 : 1, nDesc);
            break;
        }
    }
}
