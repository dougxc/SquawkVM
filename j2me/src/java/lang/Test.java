package java.lang;


public class Test {


/*---------------------------------------------------------------------------*\
 *            "X" Tests - run before system class initialization             *
\*---------------------------------------------------------------------------*/

    private static int two;

    public static void main(String[] args) {
        runXTests();
        runYTests();
    }

    public static void runXTests() {
        two = 2;

        x2_1();
        x2_2();
        x2_3();
        x2_4();
        x2_5();
        x2_6();
        x3();
        x4();
        x5(2);
        x6(2);
        x7(2);
        x8(2);
        x9(2);
        x10();
        x11();
        x12();
        x13();
        x14();
        x15();
        x16();
        x20();
        x30();
        x31();
        x32();
        x33();
        x34();
        x35();
        x36();
        x37();
        Native.print("Xpassed\n");
    }

    static void passed(String name) {
        //Native.print("Test ");
        //Native.print(name);
        //Native.print(" passed\n");
    }

    static void failed(String name) {
        Native.print("Test ");
        Native.print(name);
        Native.print(" failed\n");
    }

    static void result(String name, boolean b) {
        if (b) {
            passed(name);
        } else {
            failed(name);
        }
    }

    static void x2_1() { result("x2_1",Integer.toString(2).equals("2"));     }
    static void x2_2() { result("x2_2",Long.toString(2L).equals("2"));       }
    static void x2_3() { result("x2_3",String.valueOf(true).equals("true")); }
    static void x2_4() { result("x2_4",String.valueOf('2').equals("2"));     }
    static void x2_5() { result("x2_5",Double.toString(2.0d).equals("2.0")); }
    static void x2_6() { result("x2_6",Float.toString(2.0f).equals("2.0"));  }

    static void x3() {
        result("x3", ClassBase.classTable.getClass() == ClassBase.classTable.getClass());
    }

    static void x4() {
        passed("x4");
    }

    static void x5(int n) {
        boolean res = false;
        if (n == 2) {
            res = true;
        }
        result("x5", res);
    }

    static void x6(int n) {
        result("x5", n == 2);
    }

    static void x7(int n) {
        result("x7", 5+n == 7);
    }

    static void x8(int n) {
        result("x8", 5*n == 10);
    }

    static void x9(int n) {
        result("x9", -5*n == -10);
    }

    static void x10() {
        result("x10", -5*two == -10);
    }

    static void x11() {
        for (int i = 0 ; i < 10 ; i++) {
            Native.gc();
        }
        passed("x11");
    }

    static void x12() {
        result("x12", fib(20) == 10946);
    }

    public static int fib (int n) {
        if (n == 0) {
            Native.gc();
        }
        if (n<2) {
            return 1;
        }
        int x = fib(n/2-1);
        int y = fib(n/2);
        if (n%2==0) {
            return x*x+y*y;
        } else {
            return (x+x+y)*y;
        }
    }

    static void x13() {
        result("x13",(!(null instanceof Test)));
    }

    static void x14() {
        result("x14",("a string" instanceof String));
    }

    static void x15() {
        boolean res = true;
        try {
            Class c = (Class)null;
        } catch (Throwable t) {
            res = false;
        }
        result("x15",res);
    }

    static void x16() {
        boolean res = true;
        try {
            (new String[3])[1] = null;
        } catch (Throwable t) {
            res = false;
        }
        result("x16",res);
    }

    static void x20() {
        Test t = new Test();
        result("x20", t != null);
    }


    static void x30() {
        Object[] o = new Object[1];
        result("x30", o != null);
    }


    static void x31() {
        Object[] o = new Object[1];
        o[0] = o;
        result("x31", o[0] == o);
    }

    static void x32() {
        Object[] o1 = new Object[1];
        Object[] o2 = new Object[1];
        o1[0] = o1;
        System.arraycopy(o1, 0, o2, 0, 1);
        result("x32", o2[0] == o1);
    }

    static void x33() {
        Object[] o1 = new Object[2];
        Object[] o2 = new Object[2];
        o1[0] = o1;
        o1[1] = o2;
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x33", o2[0] == o1 && o2[1] == o2);
    }

    static void x34() {
        Object[] o1 = new Object[2];
        String[] o2 = new String[2];
        o1[0] = "Hello";
        o1[1] = "World";
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x34", o2[0].equals("Hello") && o2[1].equals("World"));
    }

    static void x35() {
        Object o = new Throwable();
        result("x35", o != null);
    }

    static void x36() {
        long l = 0xFF;
        int  i = 0xFF;
        result("x36",(l << 32) == 0xFF00000000L && ((long)i << 32) == 0xFF00000000L);
    }

    static void x37() {
        byte[] o1 = new byte[2];
        o1[0] = (byte)-3;
        result("x37", o1[0] == -3 && o1[1] == 0);
    }

/*---------------------------------------------------------------------------*\
 *             "Y" Tests - run after system class initialization             *
\*---------------------------------------------------------------------------*/

    public static void runYTests() {
//        y36();
        y40();
        y41();
    }
    static void y36() {
        boolean caught = false;
        try {
            y36_1();
        } catch (Exception e) {
            e.printStackTrace();
            caught = true;
        }
        result("y36",caught);
    }
    static void y36_1() {
        throw new RuntimeException("stack trace printing test");
    }


    static void y40() {
        boolean caught = false;
        String s = null;
        try {
            throw new RuntimeException();
        } catch(Exception ex) {
            caught = true;
        }
        result("y40", caught);
    }

    static void y41() {
        boolean caught = false;
        String s = null;
        try {
            s.charAt(0);
        } catch(Exception ex) {
            caught = true;
        }
        result("y41", caught);
    }
}


