/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;
import java.util.NoSyncHashtable;
import java.util.Hashtable;
import java.util.Enumeration;

public class Native implements NativeOpcodes {


    /*
     * The following native functions are translated by the Squawk front end into
     * of movi or movl instructions.
     */
    public    native static int    asInt(Object o);
    public    native static Class  asClass(Object o);
    public    native static Object asOop(int x);
    public    native static int[]  asIntArray(int x);
    public    native static byte[] asByteArray(int x);
    public    native static int    floatToIntBits(float value);
    public    native static long   doubleToLongBits(double value);
    public    native static float  intBitsToFloat(int bits);
    public    native static double longBitsToDouble(long bits);

    /*
     * The following native functions are translated into bytecodes.
     */
    public    native static void   parm(int x);                             // Becomes OPC_PARM
    public    native static void   parm(Object o);                          // Becomes OPC_PARM
    protected native static int    exec(int chan);                          // Becomes OPC_EXEC
    protected native static int    error(int chan);                         // Becomes OPC_ERROR
    protected native static int    result(int chan);                        // Becomes OPC_RESULT

    protected native static int[]  getAR();                                 // Becomes OPC_GETAR
    protected native static void   setAR(int[] ar, int threadID);           // Becomes OPC_SETAR
    public    native static Object newInstance(int cno);                    // Becomes OPC_NEW
    public    native static Object newArray(int cno, int length);           // Becomes OPC_NEW


    /*
     * Constructor
     */
    private Native() {}

    /*
     * Temporary buffer for passing longs (initialized in VMPlatform).
     */
    static long[] longBuf;

    /*
     * execute
     *
     * This function should be called after a number of parm() calls have been made.
     */
    public static int execute(int chan) throws java.io.IOException {

       /*
        * Channel 0 calls are always synchronous, never throw exceptions and pass their
        * single result back from the OPC_EXEC bytecode.
        */
        if (chan == 0) {
            return exec(0);
        }

       /*
        * Other channels return an event code to wait for, or zero if the call is synchronous.
        */
        int event = exec(chan);
        if (event != 0) {
            Thread.waitForEvent(event);
        }

       /*
        * Get the exeception code. If there is one then throw the corrisponding exception
        */
        int exno = error(chan);
        switch (exno) {
            case 0: break;
            case EXNO_NoConnection: throw new javax.microedition.io.ConnectionNotFoundException();
            case EXNO_IOException:  throw new java.io.IOException();
            default: fatalVMError();
        }

       /*
        * If no exeception the return the result of the operation
        */
        return result(chan);
    }

    /*
     * getChannel
     */
    public static int getChannel() {
        parm(OP_GETCHANNEL);
        return exec(0);
    }

    /*
     * freeChannel
     */
    public static void freeChannel(int chan) {
        parm(OP_FREECHANNEL);
        parm(chan);
        exec(0);
    }

    /*
     *
     */
    static String[] getCommandLineArgs() {
        parm(OP_GETARGC);
        int argc = exec(0);
        String[] args = new String[argc];
        for (int i = 0; i != argc; i++) {
            StringBuffer arg = new StringBuffer();
            for (int j = 0; ; j++) {
                parm(OP_GETARGVCH);
                parm(i);
                parm(j);
                int ch = exec(0);
                if (ch == 0) {
                    break;
                }
                arg.append((char)ch);
            }
            args[i] = arg.toString();
        }
        return args;
    }

    /*
     * exit
     */
    public static void exit(int code) {
        (new Throwable("exit")).printStackTrace();
        parm(OP_EXIT);
        parm(code);
        exec(0);
    }

    /*
     * gc
     */
    public static void gc() {
        parm(OP_GC);
        exec(0);
    }

    /*
     * freeMemory
     */
    public static long freeMemory() {
        parm(OP_FREEMEM);
        return exec(0);
    }

    /*
     * totalMemory
     */
    public static long totalMemory() {
        parm(OP_TOTALMEM);
        return exec(0);
    }

    /*
     * getTime
     */
    public static long getTime() {
        parm(OP_GETTIME);
        parm(longBuf);
        exec(0);
        return longBuf[0];
    }

    /*
     * arraycopy - Must only be called from System.arraycopy(), see the code there.
     */
    static void arraycopy(Object src, int src_position, Object dst, int dst_position, int totalLength) {
        final int MAXMOVE = 4096;
        while (true) {
            int length = Math.min(totalLength, MAXMOVE);
            parm(OP_ARRAYCOPY);
            parm(src);
            parm(src_position);
            parm(dst);
            parm(dst_position);
            parm(length);
            exec(0);
            totalLength -= length;
            if (totalLength == 0) {
                break;
            }
            src_position += length;
            dst_position += length;
            Thread.yield();
        }
    }

    /*
     * getEvent
     */
    public static int getEvent() {
        parm(OP_GETEVENT);
        return exec(0);
    }

    /*
     * waitForEvent
     */
    public static void waitForEvent(long time) {
        longBuf[0] = time;
        parm(OP_WAITFOREVENT);
        parm(longBuf);
        exec(0);
    }

    /*
     * setTracingThresholds
     */
    public static void setTracingThreshold(int threshold) {
        parm(OP_TRACE);
        parm(TRACE_SETTHRESHOLD);
        parm(threshold);
        exec(0);
    }

    /*
     * getTracingThresholds
     */
    public static int getTracingThreshold() {
        int[] thresholds = new int[2];
        parm(OP_TRACE);
        parm(TRACE_GETTHRESHOLD);
        return exec(0);
    }

    /*
     * fatalVMError
     */
    private static boolean fatalVMErrorGuard;
    public static void fatalVMError() {
        if (!fatalVMErrorGuard) {
            (new Throwable("fatalVMError")).printStackTrace();
            fatalVMErrorGuard = true;
        }
        parm(OP_FATALVMERROR);
        exec(0);
    }

    /*
     * assume
     */
    public static void assume(boolean b) {
        if (!b) {
            fatalVMError();
        }
    }

    /*
     * getActivation
     */
    public static int[] getActivation() {
        int ar[] = getAR();
        // Get the previous AR (i.e. the one of the method that called
        // getActivation).
        return asIntArray(ar[AR_previousAR]);
    }

    /*
     * setActivation
     */
    public static void setActivation(int[] ar) {
        Thread currentThread = Thread.currentThread();
        setAR(ar,(currentThread == null ? 0 : currentThread.threadNumber));
    }

    /*
     * getClassTable
     */
    public static Object[] getClassTable() {
        parm(OP_GETCLASSTABLE);
        return (Object[])asOop(exec(0));
    }

    /*
     * freeActivation
     */
    public static void freeActivation(Object ar) {
        parm(OP_FREEAR);
        parm(ar);
        exec(0);
    }

    /*
     * getHeader
     */
    public static Object getHeader(Object obj) {
        parm(OP_GETHEADER);
        parm(obj);
        return asOop(exec(0));
    }

    /*
     * setHeader
     */
    public static void setHeader(Object obj, Object to) {
        parm(OP_SETHEADER);
        parm(obj);
        parm(to);
        exec(0);
    }

    /*
     * getClass
     */
    public static Class getClass(Object obj) {
        parm(OP_GETCLASS);
        parm(obj);
        return asClass(asOop(exec(0)));
    }

    /*
     * setIsloate
     */
    public static void setIsolate(int[] array) {
        parm(OP_SETISOLATE);
        parm(array);
        exec(0);
    }




    /*
     * getArrayLength
     */
    public static int getArrayLength(Object array) {
        parm(OP_GETARRAYLENGTH);
        parm(array);
        return exec(0);
    }


    /*
     * write
     */
    public static void print(char ch) {
        parm(OP_PUTCH);
        parm(ch);
        exec(0);
    }

    /*
     * write
     */
    public static void print(String s) {
        if (s == null) {
            s = "null";
        }
        for (int i = 0 ; i < s.length() ; i++) {
            char ch = (char)s.charAt(i);
            print(ch);
        }
    }

    /*
     * write
     */
    public static void print(char[] chars) {
        for (int i = 0 ; i < chars.length ; i++) {
            print(chars[i]);
        }
    }

    public static void print(boolean b)    { print(b ? "true" : "false"); }
    public static void print(int i)        { print(String.valueOf(i)); }
    public static void print(long l)       { print(String.valueOf(l)); }
    public static void print(Object obj)   { print(String.valueOf(obj)); }
    public static void println()           { print("\n"); }
    public static void println(boolean x)  { print(x); println(); }
    public static void println(char x)     { print(x); println(); }
    public static void println(int x)      { print(x); println(); }
    public static void println(long x)     { print(x); println(); }
    public static void println(char x[])   { print(x); println(); }
    public static void println(String x)   { print(x); println(); }
    public static void println(Object x)   { print(x); println(); }


    /**
     * This is the table of system properties underlying the getProperty and
     * setProperty methods in System.
     */
    private static NoSyncHashtable systemProperties = new NoSyncHashtable();
    static {
        setProperty("microedition.configuration", "CLDC-1.0");
        setProperty("microedition.encoding",      "ISO8859_1");
        setProperty("microedition.platform",      "j2me");
        setProperty("de.kawt.classbase",          "de.kawt.impl.squawk");
        setProperty("javax.microedition.io.Connector.protocolpath", "com.sun.squawk.io");

        // Set any isolate specific system properties (keeping in mind that there is no current isolate during bootstrap.
        Hashtable properties = VMPlatform.getCurrentIsolateProperties();
        if (properties != null) {
            Enumeration keys   = properties.keys();
            Enumeration values = properties.elements();
            while (keys.hasMoreElements()) {
                String name  = (String)keys.nextElement();
                String value = (String)values.nextElement();
                setProperty(name, value);
            }
        }

    }

    static String setProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }
        return (String)systemProperties.put(key, value);
    }
    static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }
        return (String)systemProperties.get(key);
    }

    /**
     * Kernel primitive operations in Java. Called from the interpreter loop via slowOps().
     * @param this_ar The activation record used to execute this method.
     * @param code The primitive opcode being executed.
     * @param rs1 The first parameter which is always an Object reference.
     * @param rs2 The second parameter which is either an object reference or a primitive
     * value depending on the opcode being executed. If it is indeed an Object reference,
     * it must be cast to a local variable of type Object as it will otherwise
     * become invalid should a garbage collection occur during execution of the
     * opcode.
     */
    int _SQUAWK_INTERNAL_primitive(int[] this_ar, int code, Object rs1, int rs2) throws Throwable {
        switch (code) {
            default:                fatalVMError();                                     break;
            case OPC_YIELD:         Thread.yield();                                     break;
            case OPC_MENTER:        Thread.monitorEnter(rs1);                           break;
            case OPC_MEXIT:         Thread.monitorExit(rs1);                            break;
            case OPC_CHECKCAST:     if (rs1 != null) {
                                        getClass(rs1).checkCast(rs2);
                                    }
                                    break;
            case OPC_CHECKSTORE:    if (rs1 != null) {
                                        // The 'getClass(asOop(rs2))' subexpression may trigger
                                        // a garbage collection and so rs2 must be copied to
                                        // a local variable of type Object first
//                                        getClass(rs1).checkStore(getClass(asOop(rs2)).elementType);
                                        Object rs2_obj = asOop(rs2);
                                        getClass(rs1).checkStore(getClass(rs2_obj).elementType);
                                    }
                                    break;
            case OPC_CLINIT:        Class.initialize(rs2);                              break;
            case OPC_INSTANCEOF:    return asClass(ClassBase.forNumber(rs2)).isInstance(rs1) ? 1 : 0;
            case OPC_NPE:           throw new NullPointerException();
            case OPC_OBE:           throw new ArrayIndexOutOfBoundsException(rs2);
            case OPC_DIV0:          throw new ArithmeticException();

            case OPC_THROW: {
               /*
                * General note: this code must not call any non-native methods or allocate
                * any memory because there may not be any available. It's a bit messy, but
                * it is probably easier than trying to hack up some sort of emergency memory
                * area in the garbage collector
                */
                Object ex = rs1;

               /*
                * If the exception parameter is null then this is because the kernel needs
                * to throw an OutOfMemoryError.
                */
                if (ex == null) {
                    ex = TheOutOfMemoryError;
                }

               /*
                * Get the class of the exception
                */
                parm(OP_GETCLASS);
                parm(ex);
                Class exClass = asClass(asOop(exec(0)));

               /*
                * Start looking for exception handlers at the activation record before
                * the current one.
                */
                int[] prev_ar = asIntArray(this_ar[AR_previousAR]);

               /*
                * Iterate down the whole call chain.
                */
                for (int[] ar = prev_ar ; ar != null ; ar = asIntArray(ar[AR_previousAR])) {

                   /*
                    * Get the ip and method of the activation record
                    */
                    int ip = ar[AR_ip];
                    byte[] mth = asByteArray(ar[AR_method]);

                   /*
                    * Get the start and end points in the method byte array where
                    * the exception handler tables are
                    */
                    int beg = MTH_oopMap + mth[MTH_oopMapLength];
                    int end = mth[MTH_nparmsIndex];

                   /*
                    * Iterate through each entry
                    */
                    while (beg != end) {
                        int startIP   = ((mth[beg++]&0xFF) << 8) + (mth[beg++]&0xFF);
                        int endIP     = ((mth[beg++]&0xFF) << 8) + (mth[beg++]&0xFF);
                        int handlerIP = ((mth[beg++]&0xFF) << 8) + (mth[beg++]&0xFF);
                        int classNum  = ((mth[beg++]&0xFF) << 8) + (mth[beg++]&0xFF);

                       /*
                        * Test to see if the ip matches. The value of ip will be in the (inclusive) range
                        * [X+1 .. Y] where X is the address of the instruction that caused the
                        * exception to occur and Y is the address of the next instruction. The
                        * ambiguity is due to the fact an exception may be raised while the
                        * interpreter has not completed decoding the exception causing
                        * instruction (e.g. null receiver for a virtual method invocation).
                        * For this reason, the test below comparing ip to endIP is "<="
                        * as opposed to "<".
                        */
                        if (ip >= startIP && ip <= endIP) {
                           /*
                            * Test to see if the exception class is an instance of the
                            * class in the table. Start by getting the class object for
                            * the entry in the table.
                            */
                            ClassBase hClass = ClassBase.classTable[classNum];

                           /*
                            * Iterate up the class hierarchy lookin for a match
                            */
                            for (ClassBase thisClass = exClass ; thisClass != null ; thisClass = thisClass.superClass) {
                                if (thisClass == hClass) {
                                   /*
                                    * Got a match.
                                    *
                                    * 1, Fix up the activation record so that the interpreter
                                    *    will return to the exception handler.
                                    *
                                    * 2, Fix up the activation record for this function so that the
                                    *    return will go back to the activation record with the handler code.
                                    *
                                    * 3, Return the exception object. This will set GlobalInt in the
                                    *    interpreter which is where the handler code will retrieve it from.
                                    */
                                    ar[AR_ip] = handlerIP;
                                    this_ar[AR_previousAR] = asInt(ar);
                                    return asInt(ex);
                                }
                            }
                        }
                    }

                   /*
                    * Tell the garbage collector that this activation record can be reused.
                    */
                    //prev_ar = (int[])asOop(ar[AR_previousAR]);
                    //parm(OP_FREEAR);
                    //parm(ar);
                    //exec(0);
                    //result(0);
                }

               /*
                * Should have been caught by the code in Thread.java
                */
                fatalVMError();
            }
        }
        return 0;
    }

    static OutOfMemoryError TheOutOfMemoryError;

   /* ----------------------------------------------------------------------------- *\
    *    Helper methods for accessing/manipulating objects in their internal form   *
   \* ----------------------------------------------------------------------------- */

    /**
     * Write an unsigned byte into a byte array.
     * @param dst The destination array.
     * @param offset The offset in the destination array at which to start writing.
     * @param value The unsigned byte value to insert into dst.
     * @return the number of bytes written (i.e. 1) or -1 if the value is larger
     * than can be represented in an unsigned short.
     */
    static int setUnsignedByte(byte[] dst, int offset, int value) {
        if ((value & ~0xFF) != 0) {
            return -1;
        }
        dst[offset] = (byte)((value     ) & 0xFF);
        return 1;
    }

    /**
     * Get an unsigned byte from a byte array.
     * @param src The source array.
     * @param offset The offset in the source array from which to start reading.
     * @return the unsigned byte value or -1 if there was out of bounds error.
     */
    static int getUnsignedByte(byte[] src, int offset) {
        if (offset < 0 || offset > src.length - 1) {
            return -1;
        }
        return (src[offset] & 0xFF);
    }

    /**
     * Write an unsigned half word into a byte array.
     * @param dst The destination array.
     * @param offset The offset in the destination array at which to start writing.
     * @param value The half word value to insert into dst.
     * @return the number of bytes written (i.e. 2) or -1 if the value is larger
     * than can be represented in an unsigned short.
     */
    static int setUnsignedHalf(byte[] dst, int offset, int value) {
        if ((value & ~0xFFFF) != 0) {
            return -1;
        }
        dst[offset  ] = (byte)((value >> 8) & 0xFF);
        dst[offset+1] = (byte)((value     ) & 0xFF);
        return 2;
    }

    /**
     * Get an unsigned half word from a byte array.
     * @param src The source array.
     * @param offset The offset in the source array from which to start reading.
     * @return the unsigned half word value or -1 if there was out of bounds error.
     */
    static int getUnsignedHalf(byte[] src, int offset) {
        if (offset < 0 || offset > src.length - 2) {
            return -1;
        }
        return (((src[offset] << 8) & 0xFFFF) | (src[offset+1] & 0xFF));
    }

   /**
    * Get an embedded ASCII string from a byte array. This bypasses all internationalization conversions.
    * @param src A byte array representing some kind of debug info with an embedded ASCII string.
    * @param offset The offset in src of the embedded string.
    * @param length The length of the embedded string.
    * @returns the embedded ASCII string as a char array.
    */
    static char[] getEmbeddedASCIIArray(byte[] src, int offset, int length) {
        char[] value = new char[length];
        for (int i = 0; i != length; i++) {
            value[i] = (char)src[i+offset];
        }
        return value;
    }

    /**
     * Extract the name and source line number for a given method and instruction pointer.
     * @param mth The internal representation of a method.
     * @param ip An instruction pointer into the method.
     * @return a 2 element String array where the first element is the method name and the
     * second is the source line number corresponding to ip. Either of these elements
     * may be null if the info was not available.
     */
    static String[] getMethodNameAndSourceLine(byte[] mth, int ip) {
        String[] info = new String[2];
        int mthLength = mth.length;
        if (mth[mthLength - 1] == 1) {
            int debugInfoLength = Native.getUnsignedHalf(mth,mthLength - 3);
            /* Lengths */
            int offset                = (mthLength - (1 + debugInfoLength));
            int nameLength            = getUnsignedHalf(mth,offset);
            int lineNumberTableLength = getUnsignedHalf(mth,offset+2);
            offset += 4;

            /* Get the method name */
            char[] name = getEmbeddedASCIIArray(mth, offset, nameLength);
            // Only copy up to the first '('
            int index = 0;
            while (index != name.length && name[index] != '(') {
                index++;
            }
            info[0] = new String(name, 0, index);
            offset += nameLength;

            /* Decode line number table */
            int lineNumber = -1;
            if (lineNumberTableLength != 0) {
                do {
                    int startIp = getUnsignedHalf(mth,offset);
                    int line    = getUnsignedHalf(mth,offset+2);
                    if (ip == startIp) {
                        lineNumber = line;
                        break;
                    }
                    if (startIp > ip) {
                        break;
                    }
                    lineNumber = line;
                    offset += 4;
                    lineNumberTableLength--;
                } while (lineNumberTableLength != 0);
            }
            if (lineNumber != -1) {
                info[1] = Integer.toString(lineNumber);
            }
        }
        return info;
    }

}
