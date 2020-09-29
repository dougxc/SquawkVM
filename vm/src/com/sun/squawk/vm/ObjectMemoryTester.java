package com.sun.squawk.vm;

/* ------------------------------------------------------------------------ *\
 *                              ObjectMemoryTester                          *
\* ------------------------------------------------------------------------ */

class ExpectedException extends RuntimeException {}

public abstract class ObjectMemoryTester extends ObjectMemory  {

/* ------------------------------------------------------------------------ *\
 *        Dummy implementations of abstract methods                         *
\* ------------------------------------------------------------------------ */

    void setInitialMemorySize(int size) {}
    int  getInitialMemorySize() { return 0; }
    void trace_threadID() {}
    void traceJavaStack() {}

    boolean getTraceAllocation() { return false; }
    boolean getTraceGC() { return false; }
    boolean getTraceGCVerbose() { return false; }

    void    setTraceAllocation(boolean b) {};
    void    setTraceGC(boolean b) {};
    void    setTraceGCVerbose(boolean b) {};


/* ------------------------------------------------------------------------ *\
 *                  main                                                    *
\* ------------------------------------------------------------------------ */

    public static void main(String[] args) {
        try {
            for (int i = 0 ; ; i++) {
                String name = "test"+i;
                Class test = Class.forName("com.sun.squawk.vm."+name);
                ObjectMemoryTester tester = (ObjectMemoryTester)test.newInstance();

                System.out.print(name+" ... ");
                boolean passed = false;
                try {
                    boolean res = tester.run();
                    if (tester.correctFatalErrorMsg() == null && res == true) {
                        passed = true;
                    }
                } catch(ExpectedException ex) {
                    passed = true;
                } catch (Exception ex) {
                    System.out.println("Unexpected exception " + ex);
                    ex.printStackTrace();
                }
                System.out.println(passed ? "Passed" : "**Failed**");

                if (!passed) {
                    tester.dump(System.out,"At end");
                }
            }

        } catch(IllegalAccessException ex) {
            System.out.println("Error in ObjectMemoryTester " + ex);
        } catch(InstantiationException ex) {
            System.out.println("Error in ObjectMemoryTester " + ex);
        } catch(ClassNotFoundException ex) {
            // Normal way to exit
        }
    }

    public ObjectMemoryTester() {
        int []mem = new int[4096];
        Memory_init(mem, mem.length);
        ObjectMemory_init();
    }

    public boolean run()                  { return true; }
    public String  correctFatalErrorMsg() { return null; }


    void fatalVMError(String msg) {
        String errMsg = correctFatalErrorMsg();
        if (errMsg != null && msg.startsWith(errMsg)) {
            throw new ExpectedException();
        }
        dump(System.out,"At fatalVMError");
        super.fatalVMError(msg);
    }

    void fatalVMError1(String msg, int value) {
        String errMsg = correctFatalErrorMsg();
        if (errMsg != null && msg.startsWith(errMsg)) {
            throw new ExpectedException();
        }
        dump(System.out,"At fatalVMError1");
        super.fatalVMError1(msg, value);
    }

}



/* ------------------------------------------------------------------------ *\
 *                                   test0                                  *
\* ------------------------------------------------------------------------ */

// Test herness itself

class test0 extends ObjectMemoryTester  {
    public boolean run() {
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test1                                  *
\* ------------------------------------------------------------------------ */

// Test herness itself

class test1 extends ObjectMemoryTester  {
    public boolean run() {
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test2                                  *
\* ------------------------------------------------------------------------ */

// Test herness itself

class test2 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "fatal error";
    }
    public boolean run() {
        fatalVMError("fatal error");
        return false;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test3                                  *
\* ------------------------------------------------------------------------ */

// Test write followed by read of word zero

class test3 extends ObjectMemoryTester  {
    public boolean run() {
        setWord(0, 0, 1);
        return getWord(0, 0) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test4                                  *
\* ------------------------------------------------------------------------ */

// Test write followed by read of word zero with a collection of values

class test4 extends ObjectMemoryTester  {
    public boolean run() {
        for (int i = 0 ; i < 65536 ; i++) {
            setWord(0, 0, i<<16);
            if (getWord(0, 0) != i <<16) {
                return false;
            }
        }
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test5                                  *
\* ------------------------------------------------------------------------ */

// Test write followed by read of each word in the memory array

class test5 extends ObjectMemoryTester  {
    public boolean run() {
        for (int i = 0 ; i < getMemorySize() ; i++) {
            setWord(0, i, i);
            if (getWord(0, i) != i) {
                return false;
            }
        }
        return true;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test6                                  *
\* ------------------------------------------------------------------------ */

// Test that shorts read and write okay

class test6 extends ObjectMemoryTester  {
    public boolean run() {
        for (int i = 0 ; i < getMemorySize()*2 ; i++) {
            setHalf(0, i, i);
        }
        for (int i = 0 ; i < getMemorySize()*2 ; i++) {
            if (getHalf(0, i) != (short)i) {
                return false;
            }
            if (getUnsignedHalf(0, i) != (i & 0xFFFF)) {
                return false;
            }
        }
        return true;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test7                                  *
\* ------------------------------------------------------------------------ */

// Test that bytes read and write okay

class test7 extends ObjectMemoryTester  {
    public boolean run() {
        for (int i = 0 ; i < getMemorySize()*4 ; i++) {
            setByte(0, i, i);
        }
        for (int i = 0 ; i < getMemorySize()*4 ; i++) {
            if (getByte(0, i) != (byte)i) {
                return false;
            }
            if (getUnsignedByte(0, i) != (i & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test8                                  *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test8 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getWord(0, -1) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test9                                  *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test9 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getWord(0, getMemorySize()) == 1;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test10                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test10 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Negative offset";
    }
    public boolean run() {
        return getHalf(0, -1) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test11                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test11 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getHalf(0, getMemorySize() * 2) == 1;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test12                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test12 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Negative offset";
    }
    public boolean run() {
        return getByte(0, -1) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test13                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test13 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getByte(0, getMemorySize() * 4) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test14                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test14 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Negative offset";
    }
    public boolean run() {
        return getLong(0, -1) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test15                                 *
\* ------------------------------------------------------------------------ */

// Test that memory exceptions work

class test15 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getLong(0, getMemorySize() / 2) == 1;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test16                                 *
\* ------------------------------------------------------------------------ */

// Test that longs read and write okay

class test16 extends ObjectMemoryTester  {
    public boolean run() {
        for (int i = 0 ; i < getMemorySize()/2 ; i++) {
            setLong(0, i, i);
            if (getLong(0, i) != i) {
                return false;
            }
        }
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test17                                 *
\* ------------------------------------------------------------------------ */

// Test negitive pointers are caught

class test17 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getByte(-1, +20) == 1;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test18                                 *
\* ------------------------------------------------------------------------ */

// Test negitive pointers are caught

class test18 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getHalf(-1, +20) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test19                                 *
\* ------------------------------------------------------------------------ */

// Test negitive pointers are caught

class test19 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getWord(-1, +20) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test20                                 *
\* ------------------------------------------------------------------------ */

// Test negitive pointers are caught

class test20 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        return getLong(-1, +20) == 1;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test21                                 *
\* ------------------------------------------------------------------------ */

// Test memory copy

class test21 extends ObjectMemoryTester  {
    public boolean run() {
        setWord(0, 0, 1);
        setWord(0, 1, 2);
        setWord(0, 2, 3);
        copyWords(0, 10, 3);
        if (getWord(0, 10) != 1) return false;
        if (getWord(0, 11) != 2) return false;
        if (getWord(0, 12) != 3) return false;
        if (getWord(10, 0) != 1) return false;
        if (getWord(10, 1) != 2) return false;
        if (getWord(10, 2) != 3) return false;
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test22                                 *
\* ------------------------------------------------------------------------ */

// Test bad length in memory copy

class test22 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Bad base";
    }
    public boolean run() {
        copyWords(0, getMemorySize() - 10, 20);
        return false;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test23                                 *
\* ------------------------------------------------------------------------ */

// Test bad length in memory copy

class test23 extends ObjectMemoryTester  {
    public String correctFatalErrorMsg() {
        return "Negative range";
    }
    public boolean run() {
        copyWords(0, getMemorySize() - 10, -1);
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test24                                 *
\* ------------------------------------------------------------------------ */

// Try to gc an empty heap

class test24 extends ObjectMemoryTester  {
    public boolean run() {
        gc();
        return true;
    }
}

/* ------------------------------------------------------------------------ *\
 *                                   test25                                 *
\* ------------------------------------------------------------------------ */

// Try to gc a heap with a few primitive things

class test25 extends ObjectMemoryTester  {
    public boolean run() {

        build(80, 8, new byte[] { ISO_MAP0 } );
//dump("After build");
        clearTargetSpace(); gcPrim();
//dump("After gc 1");
        clearTargetSpace(); gcPrim();
//dump("After gc 2");
        clearTargetSpace(); gcPrim();
//dump("After gc 3");

        return true;
    }
}


/* ------------------------------------------------------------------------ *\
 *                                   test26                                 *
\* ------------------------------------------------------------------------ */

// Try to gc a heap with a few more primitive things

class test26 extends ObjectMemoryTester  {
    public boolean run() {

        build(80, 16, new byte[] { ISO_MAP0, (byte)0xFF});  //dump("After build");

/*
        growIsolateState(4);
        growIsolateState(8);
        growIsolateState(12);
        int str = createString("foobar");

//dump("createString(foobar)");

        addReferenceToIsolateState(str);   // add a reference
        addPrimitiveToIsolateState(1);     // add a primitive
        addReferenceToIsolateState(str);   // add a reference
        addPrimitiveToIsolateState(1);     // add a primitive
        addReferenceToIsolateState(str);   // add a reference
        addPrimitiveToIsolateState(1);     // add a primitive
*/


        for (int i = 8 ; i < 16 ; i++) {
            addStringToIsolateState(i, "Hello world"+i);
        }

        for (int i = 0 ; i < 10 ; i++) {
            clearTargetSpace(); gcPrim();  //dump("After gc "+i);
        }

        return true;
    }
}

