/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.cldc.io.connections.*;

/*---------------------------------------------------------------------------*\
 *                          SquawkClassLoader                                *
\*---------------------------------------------------------------------------*/

/**
 * SquawkClassLoader handles the retrieval of classes in Squawk assembly
 * format and conversion of the class data into an internal ClassBase object.
 */
public class SquawkClassLoader implements SquawkTags, NativeOpcodes {

    /**
     * Utility class to facilitate class loader related tracing.
     */
    final static class Tracer {

        /**
         * These trace variables are configured from system properties.
         */
        boolean classLoading;        /* Trace class loading                    */
        boolean echoInput;           /* Echo each character read by the loader */
        boolean disassemble;         /* Show a disassembly for methods         */

        /**
         * Only one instance of this class.
         */
        private Tracer() {
            configureFromSystem();
        }

        /**
         * (Re)configure the Tracer based on system properties.
         */
        void configureFromSystem() {
            String value;
            classLoading = ((value = System.getProperty("squawk.trace.classloading")) != null && value.equals("true"));
            echoInput    = ((value = System.getProperty("squawk.trace.classloading.input")) != null && value.equals("true"));
            disassemble  = ((value = System.getProperty("squawk.trace.classloading.disassemble")) != null && value.equals("true"));
        }

        static final Tracer instance = new Tracer();
        static Tracer getInstance() { return instance; }

        /**
         * The output stream to trace to.
         */
        final java.io.PrintStream out = System.out;

        int ClassLoadingLevel = -1;
        String TraceIndent = null;
        final String TraceSingleIndent = "  ";
        void incIndent() {
            if (TraceIndent == null) {
                TraceIndent = "";
            } else {
                TraceIndent += TraceSingleIndent;
            }
        }
        void decIndent() {
            if (TraceIndent.length() == 0) {
                TraceIndent = null;
            }
            else {
                TraceIndent = TraceIndent.substring(TraceSingleIndent.length());
            }
        }
        void trace(String msg)                   { xtrace(msg,true,false); }
        void trace(String msg, boolean indent)   { xtrace(msg,indent,false); }
        void traceln(String msg)                 { xtrace(msg,true,true); }
        void traceln(String msg, boolean indent) { xtrace(msg,indent,true); }
        private void xtrace(String msg, boolean indent, boolean newLine) {
            if (classLoading) {
                if (indent) {
                    out.print(ClassLoadingLevel+": ");
                    out.print(TraceIndent);
                }
                if (newLine) {
                    out.println(msg);
                } else {
                    out.print(msg);
                }
            }
        }
        void traceNameValueVector(Vector map, String separator) {
            if (map == null) {
                return;
            }
            incIndent();
            for (Enumeration e = map.elements(); e.hasMoreElements();) {
                trace(e.nextElement().toString());
                Object value = e.nextElement();
                traceln(value == null ? "" : separator+value.toString(),false);
            }
            decIndent();
        }
        void traceVector(Vector v, String separator) {
            if (v == null) {
                return;
            }
            for (Enumeration e = v.elements(); e.hasMoreElements();) {
                trace(separator + e.nextElement().toString(), false);
            }
        }

    }

    /**
     * The URL of the translator (or its proxy).
     */
    private static String translatorURL;

    /**
     * The URL path for performing a class ID
     * to class content (in Squawk XML format) request.
     */
    private static String clazzRequest;

    /**
     * The URL path for performing a class ID
     * to class methods (in Squawk XML format) request.
     */
    private static String methodsRequest;

    /**
     * The URL path for performing a class name
     * to class ID request.
     */
    private static String lookupRequest;

    static {
        translatorURL = System.getProperty("squawk.translatorURL");
        if (translatorURL == null) {
           translatorURL = "http://localhost:9090";
//           translatorURL = "file://c:/jbprojects/SquawkVM/translator_responses";
//           translatorURL = "file://C:/w/work/zcvs/SquawkVM/translator_responses";
        }

        lookupRequest = System.getProperty("squawk.lookup");
        if (lookupRequest == null) {
           lookupRequest = "/lookup/";
        }

        clazzRequest  = System.getProperty("squawk.class");
        if (clazzRequest == null) {
           clazzRequest = "/class/";
        }

        methodsRequest  = System.getProperty("squawk.methods");
        if (methodsRequest == null) {
           methodsRequest = "/methods/";
        }

    }

    /**
     * Handle to the tracing functions.
     */
    private static final Tracer t = Tracer.getInstance();

    /**
     * Hidden constructor.
     */
    private SquawkClassLoader() {}

    /**
     * Each unique request should only be made once and the requests field
     * below ensures this.
     */
    static Hashtable requests;

    /**
     * Open an input stream for a given request.
     */
    static private InputStream createInputStream(String request, String path) throws IOException {
//        if (requests == null) requests = new Hashtable();
        String url = translatorURL+request+path.replace('.', '/');
//        Exception e = (Exception)requests.get(url);
//        if (e != null) {
//            e.printStackTrace();
//            (new Exception("second retrieval of "+url)).printStackTrace();
//        }
        t.configureFromSystem();
        Connection conn = Connector.open(url);
        if (conn instanceof HttpConnection) {
            HttpConnection httpConn = (HttpConnection)conn;
            int code = httpConn.getResponseCode();
            if (code != HttpConnection.HTTP_OK) {
                throw new IOException("HttpConnection to " + url + " failed with response code " + code);
            }
        }
        else if (!(conn instanceof InputConnection)) {
            throw new IOException("InputConnection required");
        }
        return ((InputConnection)conn).openInputStream();
    }

    /**
     * Return the class ID corresponding to a given class name.
     */
    static int lookup(String className, boolean forceLoad, boolean forName) {
        int res = -1;
        try {
            SquawkFileParser p = new SquawkFileParser(createInputStream(lookupRequest, className + (forceLoad ? "" : "?forceLoad=false")));
            try {
                p.getTag(T_SQUAWK);
                int token = p.getTag();
                if (token == T_CLASS) {
                    while ((token = p.getTag()) != -T_CLASS) {
                        if (token == T_NUMBER) {
                            res = (int)p.parseNumber(-T_NUMBER,false,false);
                        } else {
                            p.skipTag(-token);
                        }
                    }
                }
                else {
                    p.unexpectedTag(T_CLASS,token);
                }
                p.getTag(-T_SQUAWK);
                p.check(res != -1,"Missing class number");
            } finally {
                p.close();
            }
        } catch (IOException ex) {
            // Don't do a stack trace dump if called by Class.forName as it knows what to do in this situation.
            if (forceLoad && !forName) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    /**
     * Load and link a class given a class ID.
     */
    static ClassBase load(int classNumber) {
        try {
            SquawkFileParser p = new SquawkFileParser(createInputStream(clazzRequest, Integer.toString(classNumber)));
            try {
                p.getTag(T_SQUAWK);
                int token = p.getTag();
                if (token == T_CLASS) {
                    t.incIndent();
                    t.ClassLoadingLevel++;
                    t.traceln("");
                    t.incIndent();
System.out.println("Loading class " + classNumber);
                    ClassBase clazz = parseClass(p, null);
System.out.println("Loaded  class " + classNumber + " (" + clazz.className + ")");
                    t.ClassLoadingLevel--;
                    t.decIndent();
                    t.decIndent();
                    return clazz;
                }
                else {
                    p.unexpectedTag(T_CLASS,token);
                }
                p.getTag(-T_SQUAWK);
            } finally {
                p.close();
            }
        } catch (IOException ex) {
ex.printStackTrace();
            Native.fatalVMError();
        }
        return null;
    }

    /**
     * Load the methods for a class.
     * @param clazz
     * @param slots
     */
    static ClassBase loadMethods(ClassBase clazz) {
        try {
            SquawkFileParser p = new SquawkFileParser(createInputStream(methodsRequest, Integer.toString(clazz.classIndex)));
            try {
                p.getTag(T_SQUAWK);
                int token = p.getTag();
                if (token == T_CLASS) {
                    t.incIndent();
                    t.ClassLoadingLevel++;
                    parseClass(p, clazz);
                    t.ClassLoadingLevel--;
                    t.decIndent();
                }
                else {
                    p.unexpectedTag(T_CLASS,token);
                }
                p.getTag(-T_SQUAWK);
            } finally {
                p.close();
            }
        } catch (IOException ex) {
ex.printStackTrace();
            Native.fatalVMError();
        }
        return clazz;
    }

    /**
     * These methods wrap a call to a SquawkFileParser so that a trace of the parsed
     * result can be performed.
     */
    private static Vector parseNumberList(SquawkFileParser p, int closing) {
        Vector v = p.parseNumberList(closing);
        t.traceVector(v," ");
        return v;
    }
    private static Vector parseVariables(SquawkFileParser p, int closing) {
        Vector v = p.parseVariables(closing);
        t.traceNameValueVector(v," : ");
        return v;
    }
    private static Vector parseFromToMap(SquawkFileParser p, int closing) {
        Vector v = p.parseFromToMap(closing);
        t.traceNameValueVector(v," -> ");
        return v;
    }

    /**
     * These methods wrap a calls methods in Native so that the appropriate exception is
     * thrown on error.
     */
    static int setUnsignedHalf(byte[] dst, int offset, int value) {
        int result = Native.setUnsignedHalf(dst, offset, value);
        SquawkFileParser.check(result > 0, "Unsigned short overflow: " + value);
        return result;
    }

    static int setUnsignedByte(byte[] dst, int offset, int value) {
        int result = Native.setUnsignedByte(dst, offset, value);
        SquawkFileParser.check(result > 0, "Unsigned byte overflow: " + value);
        return result;
    }

    /**
     * Parse a Squawk class block (i.e. content between <class> and
     * </class) and build the corresponding ClassBase instance.
     * @param clazz The class meta-data resulting from a previous call to this
     * method. If it is null, then this is the first time the class is being
     * parsed otherwise this call is only for parsing the methods.
     */
    private static ClassBase parseClass(SquawkFileParser p, ClassBase clazz) {

        int id       = -1;
        int extnds   = -1;
        int arrayOf  = -1;
        String name  = null;
        Vector impls = null;
        Vector svars = null;
        Vector ivars = null;
        Vector i_map = null;
        Vector constants  = null;
        String sourceFile = null;
        int accessFlags   = 0;
        Vector methods    = new Vector();
        boolean hasLinkageError = false;
        int token;
        while ((token = p.getTag()) != -T_CLASS) {
            if (clazz == null) {
                try {
                    // Parsing class meta-data
                    switch (token) {
                        case T_LINKAGE_ERROR:
                            hasLinkageError = true;
                            accessFlags |= ACC_LINKAGEERROR;
                            break;
                        case T_ARRAYOF:
                            arrayOf = (int)p.parseNumber(-T_ARRAYOF,false,false);
                            t.traceln("Array of: "+arrayOf);
                            break;
                        case T_ABSTRACT:
                            accessFlags |= ACC_ABSTRACT;
                            t.traceln("ACC_ABSTRACT");
                            break;
                        case T_INTERFACE:
                            accessFlags |= ACC_INTERFACE;
                            t.traceln("ACC_INTERFACE");
                            break;
                        case T_NUMBER:
                            id = (int)p.parseNumber(-T_NUMBER,false,false);
                            t.traceln("Class number: "+id);
                            break;
                        case T_NAME:
                            name = p.parseString(-T_NAME);
                            t.traceln("Class name: "+name);
                            break;
                        case T_SOURCEFILE:
                            sourceFile = p.parseString(-T_SOURCEFILE);
                            t.traceln("Source file: "+sourceFile);
                            break;
                        case T_EXTENDS:
                            extnds = (int)p.parseNumber(-T_EXTENDS,false,false);
                            t.traceln("Superclass: "+extnds);
                            break;
                        case T_IMPLEMENTS:
                            t.trace("Implements: ");
                            impls = parseNumberList(p,-T_IMPLEMENTS);
                            break;
                        case T_CONSTANTS:
                            t.traceln("Constants: ");
                            constants = parseVariables(p,-T_CONSTANTS);
                            break;
                        case T_STATIC_VARS:
                            t.traceln("Static fields:");
                            svars = parseVariables(p,-T_STATIC_VARS);
                            break;
                        case T_INSTANCE_VARS:
                            t.traceln("Instance fields:");
                            ivars = parseVariables(p,-T_INSTANCE_VARS);
                            break;
                        case T_INTERFACE_MAP:
                            t.traceln("Interface map:");
                            i_map = parseFromToMap(p,-T_INTERFACE_MAP);
                            break;
                        case T_METHODS_V: {
                            t.trace("Virtual methods: ");
                            Vector v = parseNumberList(p,-T_METHODS_V);
                            Boolean b = new Boolean(true);
                            for (Enumeration e = v.elements(); e.hasMoreElements();){
                                Integer slot = (Integer)e.nextElement();
                                int value = slot.intValue();
                                // Ignore methods in interfaces *except* for <clinit>
                                if (value == SLOT_clinit || ((accessFlags & ACC_INTERFACE) == 0)) {
                                    methods.addElement(slot);
                                    methods.addElement(b);
                                }
                            }
                            t.traceln("",false);
                            break;
                        }
                        case T_METHODS_NON_V: {
                            t.trace("Non-virtual methods: ");
                            Vector v = parseNumberList(p,-T_METHODS_NON_V);
                            Boolean b = new Boolean(false);
                            for (Enumeration e = v.elements(); e.hasMoreElements();){
                                methods.addElement(e.nextElement());
                                methods.addElement(b);
                            }
                            t.traceln("",false);
                            break;
                        }
                        case T_METHOD:
                            p.check(false, "XX");
                            // Need to get the vtable range and the offset of the first static method.
                            int slot = -1;
                            boolean isVMethod = true;
                            while ((token = p.getTag()) != -T_METHOD) {
                                switch (token) {
                                    case T_SLOT:
                                        slot = (int)p.parseNumber(-T_SLOT,false,false);
                                        if (slot == SLOT_clinit) {
                                            accessFlags |= ACC_HASCLINIT;
                                        }
                                        break;
                                    case T_SUPER:
                                        p.parseNumber(-T_SUPER,false,false);
                                        // Fall through...
                                    case T_STATIC:
                                        isVMethod = false;
                                        break;
                                    default:
                                        p.skipTag(-token);
                                        break;
                                }
                                methods.addElement(new Integer(slot));
                                methods.addElement(new Boolean(isVMethod));
                            }
                            break;
                        default: p.unexpectedTag(0,token); break;
                    }
                } catch (SquawkClassFormatError sle) {
                    throw sle.addContext("class: "+id+" ("+name+")");
                }
            } else {
                try {
                    // Parsing class methods
                    switch (token) {
                        case T_LINKAGE_ERROR:
                        case T_ABSTRACT:
                        case T_ARRAYOF:
                        case T_INTERFACE:
                        case T_NUMBER:
                        case T_NAME:
                        case T_SOURCEFILE:
                        case T_EXTENDS:
                        case T_IMPLEMENTS:
                        case T_CONSTANTS:
                        case T_STATIC_VARS:
                        case T_INSTANCE_VARS:
                        case T_INTERFACE_MAP: p.skipTag(-token); break;
                        case T_METHOD:
                            t.traceln("");
                            t.traceln("Loading method in "+clazz.className);
                            t.incIndent();
                            parseMethod(clazz,p);
                            t.decIndent();
                            break;
                        default: p.unexpectedTag(0,token); break;
                    }
                } catch (SquawkClassFormatError sle) {
                    throw sle.addContext("class: "+clazz.classIndex+" ("+clazz.className+")");
                }
            }
        }
        if (clazz == null) {
            boolean usesFvtable = false;
            int vtableStart  = 0;
            int vtableEnd    = 0;
            int firstNVMethod  = 0;
            for (Enumeration e = methods.elements(); e.hasMoreElements(); ) {
                int slot = ((Integer)e.nextElement()).intValue();
                boolean isVMethod = ((Boolean)e.nextElement()).booleanValue();

                if (slot < SLOT_FVTABLE_LENGTH) {
                   usesFvtable = true;
                }
                else {
                    if (vtableStart == 0 || slot < vtableStart) {
                        vtableStart = slot;
                    }
                    if (slot > vtableEnd) {
                        vtableEnd = slot;
                    }
                }
                if (!isVMethod && slot >= SLOT_FVTABLE_LENGTH && (firstNVMethod == 0 || slot < firstNVMethod)) {
                    firstNVMethod = slot;
                }
            }
            // Make sure the class info for a class that had a linkage error is exactly as expected
            if ((accessFlags & ACC_LINKAGEERROR) != 0) {
                p.check(usesFvtable == true && vtableStart == 0 && methods.size() == 2, "Linkage error class should only have a <clinit> method");
                p.check(constants.size() == 2 && constants.elementAt(0).equals(new Integer(T_STRING)) && constants.elementAt(1) instanceof String, "missing linkage error message");
            }
            byte[] debugInfo = null;
            int length = (sourceFile == null ? 0 : sourceFile.length());
            if (length > 0) {
                debugInfo = new byte[2 + length];
                int i = 0;
                i += setUnsignedHalf(debugInfo, i, length);
                System.arraycopy(sourceFile.getBytes(),0,debugInfo,2,length);
            }
            try {
                t.traceln(vtableEnd == -1 ? "" : "vtable range: "+vtableStart+" - "+vtableEnd);
                t.traceln("Creating class "+name);
                clazz = VMPlatform.createClass(id,
                    extnds,
                    arrayOf,
                    name,
                    impls,
                    constants,
                    svars,
                    ivars,
                    i_map,
                    accessFlags,
                    usesFvtable,
                    vtableStart,
                    vtableEnd,
                    firstNVMethod,
                    debugInfo);

                if (hasLinkageError) {
                    throw new LinkageError((String)constants.elementAt(1));
                }
                return clazz;
            } catch (SquawkClassFormatError sle) {
                throw sle.addContext("class: "+id+" ("+name+")");
            }
        }
        return clazz;
    }

    /**
     * This variable enforces non-reentrance to the assembler.
     */
    static boolean inAssembler = false;

    /**
     * Parse a single method.
     *
     * Local variable slots in the activation record for a method are allocated
     * on word aligned boundaries (with respect to the start of the activation
     * record). This is dictated by the fact that the interpreter only has
     * primitives for addressing words in an activation record.
     *
     * A method is encoded in an array of bytes that includes all the meta-data
     * of a method (e.g. activation record size, oop map for local variables etc)
     * as well as the bytecode for the method. The layout is as follows:
     *
     * Byte       | Meaning
     * -----------+--------------------------
     * 0          | Activation record size (hi byte)
     * 1          | Activation record size (lo byte)
     * 2          | Class number (hi byte)
     * 3          | Class number (lo byte)
     * 4          | X = The byte offset in this method structure of the 'number of parms' entry
     * 5          | N = Number of oopmap bytes
     * 6 .. N+5   | oopmap for activation record (includes local variables)
     * N+6 .. X-1 | exception handler tables
     * X          | P = number of parms
     * X+1 .. X+P | Offsets in activation record for parameters (byte per offset)
     * X+P+1 ...  | Bytecode
     *
     *
     * Or, as a pseudo-C struct declaration:
     *
     * struct {
     *     half arSize;         // Activation record size
     *     half classNum;       // Class number
     *     byte numParmsOffset; //
     *     byte oopMapSize;
     *     byte oopMap[oopMapSize];
     *     {
     *         half startIp;
     *         half endIp;
     *         half handlerIp;
     *         half catchType;
     *     } exceptionHandlers[exceptionHandlersLength];
     *     byte numParms;
     *     byte parmMap[numParms];
     *     byte bytecode[]
     * }
     *
     * If there is debug info available for the method, it is appended to the array of
     * bytes. The presence of of debug info is indicated by a 1 in the last byte of
     * the array and its absence by a 0. The length of the debug info is in the 2nd
     * and 3rd last bytes of the array. That is, the tail of the bytecode array
     * is as follows (where N is the length of the method array and D is the
     * length of the debug info):
     *
     * Byte         | Meaning
     * -------------+--------------------------
     * N-3-D .. N-4 | Debug info
     * N-3          | Debug info size (hi byte) i.e. ((D >> 8) & 0xFF)
     * N-2          | Debug info size (lo byte) i.e. (D & 0xFF)
     * N-1          | Debug info flag
     *
     * The debug info has the following layout (expressed in a pseudo-C struct format):
     *
     * struct {
     *    half nameLength;
     *    half lineNumberTableLength
     *    byte name[];
     *    {
     *        half startIp;    // Bytecode at which new source line begins
     *        half sourceLine; // The source line number
     *    } lineNumberTable[lineNumberTableLength];
     * }
     *
     */
    private static void parseMethod(ClassBase clazz, SquawkFileParser p) {
        int token;
        int slot               = -1;
        int superSlot          = -1;
        String name            = null;
        byte[] oopMap          = null; // Oop map for local variables
        byte[] parmMap         = null;
        byte[] bytecode        = null;
        int[] localOffsetTable = null;
        int arSize             = AR_locals;

        // This is how the first byte of the oop map for an AR must start to
        // reflect that fact that the first 3 slots of an AR contain 2 pointers
        // (one to the previous AR and one to the method object). This will
        // change if the garbage collector changes the way AR's are collected
        // (e.g. it may employ a chunky stack mechanism similiar to the KVM).
        byte arOopMapHead = (byte)((1 << AR_method) |
                                   (1 << AR_previousAR));
        try {
            while ((token = p.getTag()) != -T_METHOD) {
                switch(token) {
                    case T_NAME:
                        name = p.parseString(-T_NAME);
                        t.traceln("Name: "+name);
                        break;
                    case T_STATIC:
                        break;
                    case T_ABSTRACT:
                        bytecode = new byte[] { 0, 0 };
                        t.traceln("ACC_ABSTRACT");
                        break;
                    case T_NATIVE:
                        bytecode = new byte[] { 0, 0 };
                        t.traceln("ACC_NATIVE");
                        break;
                    case T_SLOT:
                        p.check(slot == -1,"Duplicate T_SLOT element");
                        slot = (int)p.parseNumber(-T_SLOT,false,false);
                        // Ignore methods in interfaces *except* for <clinit>
                        if ((clazz.accessFlags & ACC_INTERFACE) != 0) {
                            if (slot != SLOT_clinit) {
                                p.skipTag(-T_METHOD);
                                return;
                            }
                        }

                        break;
                    case T_LOCAL_VARS:
                        p.check(oopMap == null,"Duplicate T_LOCAL_VARS element");
                        // Parse the local variable declarations
                        t.traceln("Local variables:");
                        Vector v = parseVariables(p,-T_LOCAL_VARS);
                        if (v == null) {
                            // There are no local variables...
                            oopMap = new byte[] { arOopMapHead };
                            continue;
                        }
                        localOffsetTable = new int[v.size()/2];
                        int logicalVarNum = 0;
                        int arOffset = arSize;

                        // Allocate an oop map large enough for the case where
                        // all the local variables are dwords
                        oopMap = new byte[localOffsetTable.length];
                        oopMap[0] = arOopMapHead;

                        // Word offset of local variables in activation record.
                        for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
                            int type = ((Integer)e.nextElement()).intValue();
                            switch (type) {
                                case T_REF:
                                    byte bit = (byte)(1 << (arOffset % 8));
                                    oopMap[arOffset / 8] |= bit;
                                    // Fall through
                                case T_BYTE:
                                case T_HALF:
                                case T_WORD:
                                    localOffsetTable[logicalVarNum++] = ((arOffset++) << 16) | 1;
                                    break;
                                case T_DWORD:
                                    localOffsetTable[logicalVarNum++] = ((arOffset++) << 16) | 2;
                                    arOffset++;
                                    break;
                                default:
                                    SquawkFileParser.check(false,"Shouldn't reach here");
                            }
                            // Skip initial value (should be null anyway)
                            e.nextElement();
                        }
                        // Resize the oopMap to be exactly the size it needs to be
                        int oopMapSize = (arOffset+7) / 8;
                        Object oldOopMap = oopMap;
                        oopMap = new byte[oopMapSize];
                        System.arraycopy(oldOopMap,0,oopMap,0,oopMapSize);
                        arSize = arOffset;
                        break;
                    case T_PARAMETER_MAP:
                        p.check(parmMap == null,"Duplicate T_PARAMETER_MAP element");
                        // The <local_variables> element must have already been parsed
                        p.check(oopMap != null,"T_PARAMETER_MAP element must succeed T_LOCAL_VARS element");
                        t.traceln("Parameter map:");
                        Vector p_map = parseFromToMap(p,-T_PARAMETER_MAP);
                        p.check(p_map.size() > 0,"Parameter map cannot be empty");
                        /*
                         * The map needs to take into account that long parameters are passed as
                         * 2 ints and so a parameter's index in the map will not correspond to its
                         * logical position if it is preceeded by one or more long parameters.
                         */
                        parmMap = new byte[p_map.size()];
                        int numLongParms = 0;
                        int lastLogicalParm = -1;
                        for (Enumeration e = p_map.elements(); e.hasMoreElements(); ) {
                            int from = ((Integer)e.nextElement()).intValue();
                            int to   = ((Integer)e.nextElement()).intValue();
                            p.check(from > lastLogicalParm, "Parm map must be ordered");
                            lastLogicalParm = from;

                            boolean isLong;
                            if (to < 0) {
                                isLong = to == -2;
                            }
                            else {
                                p.check(localOffsetTable != null, "Illegal parm map");
                                isLong = (localOffsetTable[to] & 0xFFFF) == 2;
                            }

                            // 'to' is still a logical variable slot number so it
                            // needs to be converted to the real AR slot offset first
                            // unless it equals -1 (i.e. an unused parameter)
                            if (to >= 0) {
                                to = localOffsetTable[to] >> 16;
                            }
                            else {
                                // An unused parameter gets mapped to an illegal
                                // local variable slot index
                                to = 0;
                            }
                            p.check(to == 0 || to >= AR_locals,"Illegal parm map");
                            setUnsignedByte(parmMap, from + numLongParms, to);
                            if (isLong) {
                                numLongParms++;
                                setUnsignedByte(parmMap, from + numLongParms, to + 1);
                            }
                        }
                        // Resize parms to be exactly the right size
                        int parmMapSize = (lastLogicalParm + 1) + numLongParms;
                        Object oldParmMap = parmMap;
                        parmMap = new byte[parmMapSize];
                        System.arraycopy(oldParmMap,0,parmMap,0,parmMapSize);
                        break;
                    case T_SUPER:
                        p.check(superSlot == -1,"Duplicate T_SUPER element");
                        p.check(oopMap == null && parmMap == null && bytecode == null,
                            "Bad method");
                        superSlot = (int)p.parseNumber(-T_SUPER,false,false);
                        t.traceln("Super: "+superSlot);
                        ClassBase superClass = clazz.superClass;
                        if (superSlot < SLOT_FVTABLE_LENGTH) {
                            bytecode = superClass.fvtable[superSlot];
                        }
                        else {
                            while (superSlot < superClass.vstart) {
                                superClass = superClass.superClass;
                            }
                            bytecode = superClass.vtable[superSlot - superClass.vstart];
                        }
                        p.check(bytecode != null,"Super class's methods not loaded");
                        break;
                    case T_INSTRUCTIONS: {
                        /*
                         * This must be the last method element parsed.
                         */
                        p.check(slot != -1 && oopMap != null && parmMap != null &&
                            superSlot == -1 && bytecode == null && name != null,
                            "Bad method");
                        Vector instructions = new Vector();

                        // Only one instance of the assembler can exist at anytime. This ensures
                        // that assembly does not use up to much memory in its data structures.
                        p.check(inAssembler == false,"Assembler is not re-entrant");
                        inAssembler = true;
                        Assembler a = new Assembler(clazz,p,localOffsetTable);
a.traceMethod(name);
                        Instruction last = null;
                        t.traceln("Instructions:");
                        t.incIndent();
                        Vector lineNumberTable = new Vector();
                        NoSyncHashtable attrs = new NoSyncHashtable();
                        Integer lineAttrKey = new Integer(A_LINE);
                        int sourceLine = -1;
                        try {
                            while ((token = p.getTag(false,attrs)) != -T_INSTRUCTIONS) {
                                if (token != T_I) {
                                    p.unexpectedTag(T_I,token);
                                }
                                last = a.parseInstruction(last,false);
                                instructions.setSize(last.asmPosition+1);
                                instructions.setElementAt(last,last.asmPosition);
                                String line = (String)attrs.get(lineAttrKey);
                                if (line != null && (lineNumberTable.size() == 0 ||
                                    !lineNumberTable.lastElement().equals(line)))
                                {
                                    lineNumberTable.addElement(new Integer(last.position));
                                    lineNumberTable.addElement(line);
                                }
                            }
                        } finally {
                            inAssembler = false;
                        }
a.traceMethodEnd();
                        /*
                         * Calculate the size of the method preamble (i.e. the number
                         * of bytes required for the method's meta-data).
                         */
                        Vector handlers = a.handlers;
                        int handlerTableSize = handlers.size() * 8;
                        int preamble =

                            2 +              // arSize
                            2 +              // classNum
                            1 +              // numParmsOffset
                            1 +              // oopMapSize
                            oopMap.length +  // oopMap
                            1 +              // numParms
                            parmMap.length + // parmMap
                            handlerTableSize;
                        int debugInfoLength = 0;
                        if (name != null || lineNumberTable.size() > 0) {
                            // remove the return type of the method name
                            int spaceIndex = name.indexOf(' ');
                            if (spaceIndex != -1) {
                                name = name.substring(spaceIndex + 1);
                            }
                            // debug info length
                            debugInfoLength += 2;
                            // name info
                            debugInfoLength += 2;
                            debugInfoLength += (name == null ? 0 : name.length());
                            // line number table
                            debugInfoLength += 2;
                            debugInfoLength += (lineNumberTable.size()/2) * 4;
                        }
                        p.check((debugInfoLength & ~0xFFFF) == 0,"Debug info too big");

                        bytecode = a.produce(instructions,preamble,debugInfoLength+1);

                        /*
                         * Fill in method preamble
                         */
                        int offset = 0;
                        p.check(arSize < 0xFFFF,"Too many locals");

                        // Set the activation record size
                        offset += setUnsignedHalf(bytecode,offset,arSize);

                        // Set the class number
                        p.check(offset == MTH_classNumberHigh, "Bad preamble");
                        offset += setUnsignedHalf(bytecode, offset,clazz.classIndex);

                        // Set the index of nparms
                        p.check(offset == MTH_nparmsIndex, "Bad preamble");
                        offset += setUnsignedByte(bytecode, offset, MTH_oopMap + oopMap.length + handlerTableSize);

                        // Set the oop map length
                        p.check(offset == MTH_oopMapLength, "Bad preamble");
                        offset += setUnsignedByte(bytecode, offset, oopMap.length);

                        // Set the oop map
                        p.check(offset == MTH_oopMap, "Bad preamble");
                        System.arraycopy(oopMap,0,bytecode,MTH_oopMap,oopMap.length);
                        offset = MTH_oopMap + oopMap.length;

                        // Set the exception handler table
                        for (Enumeration e = handlers.elements(); e.hasMoreElements(); ) {
                            ExceptionHandler handler = (ExceptionHandler)e.nextElement();
                            t.traceln("handler: "+handler);
                            offset += setUnsignedHalf(bytecode,offset,handler.start.position + preamble);
                            offset += setUnsignedHalf(bytecode,offset,handler.end.position + preamble);
                            offset += setUnsignedHalf(bytecode,offset,handler.catchInstruction.position + preamble);
                            offset += setUnsignedHalf(bytecode,offset,handler.classNum);
                        }

                        // Set the nparms value
                        p.check(offset == Native.getUnsignedByte(bytecode, MTH_nparmsIndex), "Bad preamble");
                        offset += setUnsignedByte(bytecode, offset, parmMap.length);

                        // Set the parm map
                        System.arraycopy(parmMap,0,bytecode,offset,parmMap.length);
                        offset += parmMap.length;
                        p.check(preamble == offset,"Bad preamble");
                        /*
                         * Fill in debug info
                         */
                        if (debugInfoLength == 0) {
                            // set the flag to indicate there is no debug info
                            bytecode[bytecode.length - 1] = 0;
                        }
                        else {
                            // Meta debug info
                            bytecode[bytecode.length - 1] = 1;  // Debug info flag
                            setUnsignedHalf(bytecode,bytecode.length - 3,debugInfoLength);
                            offset = bytecode.length - (1+debugInfoLength);

                            // lengths
                            int nameLength            = (name == null ? 0 : name.length());
                            int lineNumberTableLength = (lineNumberTable.size()/2);
                            offset += setUnsignedHalf(bytecode,offset,nameLength);
                            offset += setUnsignedHalf(bytecode,offset,lineNumberTableLength);

                            // name
                            if (nameLength != 0) {
                                byte[] nameBytes = name.getBytes();
                                System.arraycopy(nameBytes,0,bytecode,offset,nameLength);
                                offset += nameLength;
                            }
                            // line number table
                            Enumeration e = lineNumberTable.elements();
                            while (lineNumberTableLength != 0) {
                                int startIp    = ((Integer)e.nextElement()).intValue() + preamble;
                                    sourceLine = Integer.parseInt((String)e.nextElement());
                                offset += setUnsignedHalf(bytecode,offset,startIp);
                                offset += setUnsignedHalf(bytecode,offset,sourceLine);
                                lineNumberTableLength--;
                            }
                        }
                        p.check((bytecode.length - 3) == offset,"Bad method debug info");
                        t.decIndent();
                        break;
                    }
                }
            }
        } catch (SquawkClassFormatError sle) {
            throw sle.addContext("method: "+slot+" ("+name+")");
        }
        /*
         * Insert method into correct slot in fvtable or vtable
         */
        p.check(bytecode != null,"Method has no definition");
        if (t.disassemble) {
            Disassembler d = new Disassembler(System.out,bytecode);
            System.out.println("Disassembly for "+clazz.className+"."+name);
            d.disassemble(true);
        }
        if (slot == SLOT_clinit) {
            clazz.accessFlags |= ACC_HASCLINIT;
        }

        if (slot < SLOT_FVTABLE_LENGTH) {
            clazz.fvtable[slot] = bytecode;
        }
        else {
            int realSlot = slot - clazz.vstart;
            p.check(0 <= realSlot && realSlot < clazz.vtable.length, "invalid slot number: " + slot);
            clazz.vtable[realSlot] = bytecode;
        }
    }

}

/*---------------------------------------------------------------------------*\
 *                          AbstractAssembler                                *
\*---------------------------------------------------------------------------*/

/**
 * Squawk bytecode assembler.
 */
abstract class AbstractAssembler implements NativeOpcodes {

    /**
     * Determines how longs are split into 2 integers.
     */
    private final static boolean HIGHWORDFIRST = true;

    /**
     * Word size constraints on instruction operands
     */
    final static int SIZE_1    = 1; // One word
    final static int SIZE_2    = 2; // Two words
    final static int SIZE_1_2  = 3; // One or two words

    /**
     * Byte size of the type represented by the Squawk tags T_DWORD, T_WORD,
     * T_REF, T_HALF and T_BYTE.
     */
    final static int[] TYPE_SIZE = { 8, 4, 4, 2, 1 };

    /**
     * Convenient handle onto load tracer.
     */
    static final SquawkClassLoader.Tracer t = SquawkClassLoader.Tracer.getInstance();

    /**
     * These are the various instruction classes.
     */
    //                                               Description                             Operand format
    private final static int TYPE_ALU       = 1;  // Arithmetic, shift, compare & logic      <local    parm     parm>
    private final static int TYPE_ALU_DDD   = 2;  // Arithmetic, shift, compare & logic      <local2   parm2    parm2>
    private final static int TYPE_ALU_WDD   = 3;  // Arithmetic, shift, compare & logic      <local    parm2    parm2>
    private final static int TYPE_ALU_DDW   = 4;  // Arithmetic, shift, compare & logic      <local2   parm2    parm>
    private final static int TYPE_ALENGTH   = 5;  // arraylength                             <local   !local>
    private final static int TYPE_CCAST     = 6;  // checkcast                               <objectref class>
    private final static int TYPE_CSTORE    = 7;  // checkstore                              <objectref local>
    private final static int TYPE_CLINIT    = 8;  // Class initialization                    <class>
    private final static int TYPE_TRY       = 9;  // Exception handling (try)                 <class    address>
    private final static int TYPE_END       = 10; // Exception handling (end)                <class>
    private final static int TYPE_THROW     = 11; // Exception handling (throw)              <local>
    private final static int TYPE_CATCH     = 12; // Exception handling (catch)              <local>
    private final static int TYPE_IF        = 13; // Conditional branches                    <parm     parm     address>
    private final static int TYPE_GOTO      = 14; // Unconditional branch                    <address>
    private final static int TYPE_INSTANCEOF= 15; // instanceof                              <local    local    class>
    private final static int TYPE_INVOKE    = 16; // Method invocation (return word)         <local    member   local-    parm>
    private final static int TYPE_INVOKEV   = 17; // Method invocation (void)                <         member   local-   parm>
    private final static int TYPE_INVOKEL   = 18; // Method invocation (return dword)        <local2   member   local-    parm>
    private final static int TYPE_LSWITCH   = 19; // lookupswitch                            <local    address  address*  iconst>
    private final static int TYPE_MATH_DD   = 20; // Floating point math                     <local2   parm2>
    private final static int TYPE_MATH_DDD  = 21; // Floating point math                     <local2   parm2    parm2>
    private final static int TYPE_LDCONST   = 22; // Load constant                           <local    iconst>
    private final static int TYPE_LOAD      = 23; // Load                                    <local   !offset>
    private final static int TYPE_LOAD_D    = 24; // Load                                    <local2  !offset   !local>
    private final static int TYPE_STORE     = 25; // Store                                   <parm    !offset>
    private final static int TYPE_STORE_D   = 26; // Store                                   <parm2   !offset   !local>
    private final static int TYPE_MONITOR   = 27; // Monitor                                 <lockable>
    private final static int TYPE_MOVE      = 28; // Move, type conversion and negation     <local    parm>
    private final static int TYPE_MOVE_WD   = 29; // Move, type conversion and negation     <local    parm2>
    private final static int TYPE_MOVE_DW   = 30; // Move, type conversion and negation     <local2   parm>
    private final static int TYPE_MOVE_DD   = 31; // Move, type conversion and negation     <local2   parm2>
    private final static int TYPE_NATIVE_P  = 32; // Native method calls                     <parm>
    private final static int TYPE_NATIVE_PP = 33; // Native method calls                     <parm     parm>
    private final static int TYPE_NATIVE_LP = 34; // Native method calls                     <local    parm>
    private final static int TYPE_NATIVE_L  = 35; // Native method calls                     <local>
    private final static int TYPE_NATIVE    = 36; // Native method calls                     <>
    private final static int TYPE_NEW       = 37; // New operations                          <local    class    [parm>
    private final static int TYPE_RETURN    = 38; // Return instructions                     <parm>
    private final static int TYPE_RETURN_D  = 39; // Return instructions                     <parm2>
    private final static int TYPE_RETURNV   = 40; // Void return instruction                 <>
    private final static int TYPE_TSWITCH   = 41; // tableswitch                             <local    iconst   iconst    address address>
    private final static int TYPE_NOP       = 42; // nop                                     <>

    private final static int MAX_KEYS       = 149;
    private final static SymbolTable symbols = new SymbolTable(MAX_KEYS);
    static void addSymbol(String name, int opcode, int type) {
        symbols.put(name, (type << 8) + (opcode & 0xFF));
    }
    static {
        addSymbol("add",               OPC_ADDI,           TYPE_ALU);
        addSymbol("addd",              MATH_ADDD,          TYPE_ALU_DDD);
        addSymbol("addf",              OPC_ADDF,           TYPE_ALU);
        addSymbol("addl",              MATH_ADDL,          TYPE_ALU_DDD);
        addSymbol("and",               OPC_ANDI,           TYPE_ALU);
        addSymbol("andl",              MATH_ANDL,          TYPE_ALU_DDD);
        addSymbol("arraylength",       OPC_ALENGTH,        TYPE_ALENGTH);
        addSymbol("catch",             OPC_GETI,           TYPE_CATCH);
        addSymbol("checkcast",         OPC_CHECKCAST,      TYPE_CCAST);
        addSymbol("checkstore",        OPC_CHECKSTORE,     TYPE_CSTORE);
        addSymbol("clinit",            OPC_CLINIT,         TYPE_CLINIT);
        addSymbol("cmpdg",             MATH_CMPDG,         TYPE_ALU_WDD);
        addSymbol("cmpfg",             OPC_CMPFG,          TYPE_ALU);
        addSymbol("cmpl",              MATH_CMPL,          TYPE_ALU_WDD);
        addSymbol("cmpdl",             MATH_CMPDL,         TYPE_ALU_WDD);
        addSymbol("cmpfl",             OPC_CMPFL,          TYPE_ALU);
        addSymbol("d2f",               MATH_D2F,           TYPE_MOVE_WD);
        addSymbol("d2i",               MATH_D2I,           TYPE_MOVE_WD);
        addSymbol("d2l",               MATH_D2L,           TYPE_MOVE_DD);
        addSymbol("div",               OPC_DIVI,           TYPE_ALU);
        addSymbol("divd",              MATH_DIVD,          TYPE_ALU_DDD);
        addSymbol("divf",              OPC_DIVF,           TYPE_ALU);
        addSymbol("divl",              MATH_DIVL,          TYPE_ALU_DDD);
        addSymbol("end",               -1,                 TYPE_END);
        addSymbol("error",             OPC_ERROR,          TYPE_NATIVE_LP);
        addSymbol("exec",              OPC_EXEC,           TYPE_NATIVE_LP);
        addSymbol("f2d",               MATH_F2D,           TYPE_MOVE_DW);
        addSymbol("f2i",               OPC_F2I,            TYPE_MOVE);
        addSymbol("f2l",               MATH_F2L,           TYPE_MOVE_DW);
        addSymbol("goto",              OPC_GOTO,           TYPE_GOTO);
        addSymbol("getar",             OPC_GETAR,          TYPE_NATIVE_L);
        addSymbol("i2b",               OPC_I2B,            TYPE_MOVE);
        addSymbol("i2c",               OPC_I2C,            TYPE_MOVE);
        addSymbol("i2d",               MATH_I2D,           TYPE_MOVE_DW);
        addSymbol("i2f",               OPC_I2F,            TYPE_MOVE);
        addSymbol("i2l",               MATH_I2L,           TYPE_MOVE_DW);
        addSymbol("i2s",               OPC_I2S,            TYPE_MOVE);
        addSymbol("ifeq",              OPC_IFEQ,           TYPE_IF);
        addSymbol("ifeqr",             OPC_IFEQ,           TYPE_IF);
        addSymbol("ifge",              OPC_IFGE,           TYPE_IF);
        addSymbol("ifgt",              OPC_IFGT,           TYPE_IF);
        addSymbol("ifle",              OPC_IFLE,           TYPE_IF);
        addSymbol("iflt",              OPC_IFLT,           TYPE_IF);
        addSymbol("ifne",              OPC_IFNE,           TYPE_IF);
        addSymbol("ifner",             OPC_IFNE,           TYPE_IF);
        addSymbol("instanceof",        OPC_INSTANCEOF,     TYPE_INSTANCEOF);
        addSymbol("invoke",            OPC_INVOKEVIRTUAL,  TYPE_INVOKE);
        addSymbol("invoked",           OPC_INVOKEVIRTUAL,  TYPE_INVOKEL);
        addSymbol("invokef",           OPC_INVOKEVIRTUAL,  TYPE_INVOKE);
        addSymbol("invokel",           OPC_INVOKEVIRTUAL,  TYPE_INVOKEL);
        addSymbol("invoker",           OPC_INVOKEVIRTUAL,  TYPE_INVOKE);
        addSymbol("invokev",           OPC_INVOKEVIRTUAL,  TYPE_INVOKEV);
        addSymbol("l2d",               MATH_L2D,           TYPE_MOVE_DD);
        addSymbol("l2f",               MATH_L2F,           TYPE_MOVE_WD);
        addSymbol("l2i",               MATH_L2I,           TYPE_MOVE_WD);
        addSymbol("ld",                OPC_LDI,            TYPE_LOAD);
        addSymbol("ldb",               OPC_LDB,            TYPE_LOAD);
        addSymbol("ldc",               OPC_LDC,            TYPE_LOAD);
        addSymbol("ldd",               MATH_LDL,           TYPE_LOAD_D);
        addSymbol("ldf",               OPC_LDI,            TYPE_LOAD);
        addSymbol("ldl",               MATH_LDL,           TYPE_LOAD_D);
        addSymbol("ldr",               OPC_LDI,            TYPE_LOAD);
        addSymbol("lds",               OPC_LDS,            TYPE_LOAD);
        addSymbol("ldz",               OPC_LDB,            TYPE_LOAD);
        addSymbol("ldconst",           OPC_LDCONST,        TYPE_LDCONST);
        addSymbol("lookupswitch",      OPC_LOOKUPSWITCH,   TYPE_LSWITCH);
        addSymbol("math_sin",          MATH_sin,           TYPE_MATH_DD);
        addSymbol("math_cos",          MATH_cos,           TYPE_MATH_DD);
        addSymbol("math_tan",          MATH_tan,           TYPE_MATH_DD);
        addSymbol("math_asin",         MATH_asin,          TYPE_MATH_DD);
        addSymbol("math_acos",         MATH_acos,          TYPE_MATH_DD);
        addSymbol("math_atan",         MATH_atan,          TYPE_MATH_DD);
        addSymbol("math_exp",          MATH_exp,           TYPE_MATH_DD);
        addSymbol("math_log",          MATH_log,           TYPE_MATH_DD);
        addSymbol("math_sqrt",         MATH_sqrt,          TYPE_MATH_DD);
        addSymbol("math_ceil",         MATH_ceil,          TYPE_MATH_DD);
        addSymbol("math_floor",        MATH_floor,         TYPE_MATH_DD);
        addSymbol("math_atan2",        MATH_atan2,         TYPE_MATH_DDD);
        addSymbol("math_pow",          MATH_pow,           TYPE_MATH_DDD);
        addSymbol("math_IEEEremainder",MATH_IEEEremainder, TYPE_MATH_DDD);
        addSymbol("menter",            OPC_MENTER,         TYPE_MONITOR);
        addSymbol("mexit",             OPC_MEXIT,          TYPE_MONITOR);
        addSymbol("mov",               OPC_MOVI,           TYPE_MOVE);
        addSymbol("movd",              MATH_MOVL,          TYPE_MOVE_DD);
        addSymbol("movf",              OPC_MOVI,           TYPE_MOVE);
        addSymbol("movl",              MATH_MOVL,          TYPE_MOVE_DD);
        addSymbol("movr",              OPC_MOVI,           TYPE_MOVE);
        addSymbol("mul",               OPC_MULI,           TYPE_ALU);
        addSymbol("muld",              MATH_MULD,          TYPE_ALU_DDD);
        addSymbol("mulf",              OPC_MULF,           TYPE_ALU);
        addSymbol("mull",              MATH_MULL,          TYPE_ALU_DDD);
        addSymbol("neg",               OPC_NEGI,           TYPE_MOVE);
        addSymbol("negd",              MATH_NEGD,          TYPE_MOVE_DD);
        addSymbol("negf",              OPC_NEGF,           TYPE_MOVE);
        addSymbol("negl",              MATH_NEGL,          TYPE_MOVE_DD);
        addSymbol("new",               OPC_NEW,            TYPE_NEW);
        addSymbol("nop",               -1,                 TYPE_NOP);
        addSymbol("orr",               OPC_ORRI,           TYPE_ALU);
        addSymbol("orrl",              MATH_ORRL,          TYPE_ALU_DDD);
        addSymbol("parm",              OPC_PARM,           TYPE_NATIVE_P);
        addSymbol("rem",               OPC_REMI,           TYPE_ALU);
        addSymbol("remd",              MATH_REMD,          TYPE_ALU_DDD);
        addSymbol("remf",              OPC_REMF,           TYPE_ALU);
        addSymbol("reml",              MATH_REML,          TYPE_ALU_DDD);
        addSymbol("result",            OPC_RESULT,         TYPE_NATIVE_LP);
        addSymbol("return",            OPC_RETURNI,        TYPE_RETURN);
        addSymbol("returnd",           OPC_RETURNL,        TYPE_RETURN_D);
        addSymbol("returnf",           OPC_RETURNI,        TYPE_RETURN);
        addSymbol("returnl",           OPC_RETURNL,        TYPE_RETURN_D);
        addSymbol("returnr",           OPC_RETURNI,        TYPE_RETURN);
        addSymbol("returnv",           OPC_RETURN,         TYPE_RETURNV);
        addSymbol("setar",             OPC_SETAR,          TYPE_NATIVE_PP);
        addSymbol("sll",               OPC_SLLI,           TYPE_ALU);
        addSymbol("slll",              MATH_SLLL,          TYPE_ALU_DDW);
        addSymbol("sra",               OPC_SRAI,           TYPE_ALU);
        addSymbol("sral",              MATH_SRAL,          TYPE_ALU_DDW);
        addSymbol("srl",               OPC_SRLI,           TYPE_ALU);
        addSymbol("srll",              MATH_SRLL,          TYPE_ALU_DDW);
        addSymbol("st",                OPC_STI,            TYPE_STORE);
        addSymbol("stb",               OPC_STB,            TYPE_STORE);
        addSymbol("stc",               OPC_STS,            TYPE_STORE);
        addSymbol("std",               MATH_STL,           TYPE_STORE_D);
        addSymbol("stf",               OPC_STI,            TYPE_STORE);
        addSymbol("stl",               MATH_STL,           TYPE_STORE_D);
        addSymbol("str",               OPC_STOOP,          TYPE_STORE);
        addSymbol("sts",               OPC_STS,            TYPE_STORE);
        addSymbol("stz",               OPC_STB,            TYPE_STORE);
        addSymbol("sub",               OPC_SUBI,           TYPE_ALU);
        addSymbol("subd",              MATH_SUBD,          TYPE_ALU_DDD);
        addSymbol("subf",              OPC_SUBF,           TYPE_ALU);
        addSymbol("subl",              MATH_SUBL,          TYPE_ALU_DDD);
        addSymbol("tableswitch",       OPC_TABLESWITCH,    TYPE_TSWITCH);
        addSymbol("throw",             OPC_THROW,          TYPE_THROW);
        addSymbol("try",               -1,                 TYPE_TRY);
        addSymbol("yield",             OPC_YIELD,          TYPE_NATIVE);
        addSymbol("xor",               OPC_XORI,           TYPE_ALU);
        addSymbol("xorl",              MATH_XORL,          TYPE_ALU_DDD);
    }

    /**
     * Result (or destination) specifier for instructions that don't write a result.
     */
    final static byte[] NO_DST = { 1, 0 };

    /**
     * Buffers used to assemble instructions.
     */
    static byte[] dst1       = new byte[6];
    static byte[] dst2       = new byte[6];
    static byte[] src1       = new byte[6];
    static byte[] src2       = new byte[6];
    static byte[] src3       = new byte[6];
    static byte[] src4       = new byte[6];
    static byte[] src5       = new byte[6];
    static byte[] parameters = new byte[0xFF * MAX_WORD_ENCODING];

    /**
     * The exception handlers of a method.
     */
    Stack  partialHandlers = new Stack(); // Only 'try' parsed so far
    Vector handlers        = new Vector();

    /**
     * Class of the method being assembled.
     */
    final ClassBase clazz;

    /**
     * Offset table for current method. This map has an entry for each local
     * variable in a method, indexed by the variable's logical offset. The high
     * 16 bits of an entry gives the offset of the AR slot for the variable and
     * the low 16 bits gives the size of the variable which will be 1 or 2.
     */
    final int[] localOffsetTable;

    /**
     * Input parser.
     */
    final SquawkFileParser p;

    /**
     * Emitted code buffer.
     */
    final SpecialByteArrayOutputStream os = new SpecialByteArrayOutputStream();

    /**
     * The current output position.
     */
    int currentPosition = 0;

    /**
     * The number of Squawk assembly instructions parsed.
     */
    int asmPosition = -1;

    /**
     * Constructor.
     */
    AbstractAssembler(ClassBase clazz, SquawkFileParser p, int[] localOffsetTable)
    {
        this.clazz            = clazz;
        this.p                = p;
        this.localOffsetTable = localOffsetTable;
    }

    /**
     * Parse an optional '!' preceeded by any amount of whitespace.
     * @return true if a '!' was found.
     */
    boolean parseRuntimeCheck() {
        if (p.eatWhiteSpace(false) == '!') {
            return true;
        } else {
            p.ungetc();
            return false;
        }
    }

    /**
     * Parse address.
     */
    int parseAddress() {
        p.check(p.eatWhiteSpace(false) == '$', "Bad address: expecting $");
        int value = p.getNumber(false);
        p.check(value >= 0 && value <= 0xFFFF, "Bad address value");
        return value;
    }

    /**
     * getAddress
     */
    byte[] getAddress(byte[] buf) {
        int value = parseAddress();
        buf[0] = 2;
        SquawkClassLoader.setUnsignedHalf(buf, 1, value);
        return buf;
    }

    /**
     * Parse and encode an integer constant.
     */
    byte[] getIConst(byte[] buf, boolean checkForHash) {
        if (checkForHash) {
            p.check(p.eatWhiteSpace(false) == '#',"Bad integer constant");
        }
        int value = p.getNumber(false);
        buf[0] = 4;
        buf[1] = (byte)((value >> 24) & 0xFF);
        buf[2] = (byte)((value >> 16) & 0xFF);
        buf[3] = (byte)((value >> 8)  & 0xFF);
        buf[4] = (byte)((value >> 0)  & 0xFF);
        return buf;
    }

    /**
     * Parse and encode an integer constant.
     */
    byte[] getSlotConst(byte[] buf) {
        int value = p.getNumber(false);
        if (value >= 0 && value <= 0x7F) {
            buf[0] = 1;
            buf[1] = (byte)value;
        } else {
            p.check(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE, "Slot number is out of range");
            buf[0] = 2;
            SquawkClassLoader.setUnsignedHalf(buf, 1, value & 0xFFFF);
        }
        return buf;
    }



    /**
     * Parse and encode a word size instruction parameter. This handles both
     * local variables and constants.
     * @param buf The buffer into which the encoded parameter is written.
     * @return the encoded parameter.
     */
    byte[] getParm(byte[] buf) {
        return getParm(buf,null,SIZE_1);
    }

    /**
     * Parse and encode a single instruction parameter. This handles both
     * local variables and constants. Dword parameters are encoded as a pair
     * of ints as per getLocal.
     * @param buf The buffer into which the encoded parameter is written.
     * @param buf2 The buffer into which the second (if any) encoded parameter is written.
     * @param size The size constraint on the parm.
     * @return the first encoded parameter.
     */
    byte[] getParm(byte[] buf, byte[] buf2, int size) {
        int ch = p.eatWhiteSpace(false);
        if (ch == '#') {
            long value;
            ch = p.getc();
            if (ch == '#') {
                p.check(size == SIZE_2 || size == SIZE_1_2,"Operand must be a word");
                // parameter is a long constant
                value = p.getLongNumber();
                if (HIGHWORDFIRST) {
                    buf[0] = encodeConstant(buf, 1,value>>>32);
                    buf2[0]= encodeConstant(buf2,1,value&0xFFFFFFFF);
                } else {
                    buf2[0]= encodeConstant(buf2,1,value>>>32);
                    buf[0] = encodeConstant(buf, 1,value&0xFFFFFFFF);
                }
            } else {
                p.check(size == SIZE_1 || size == SIZE_1_2,"Operand must be a dword");
                // parameter is an int constant
                p.ungetc();
                value = p.getNumber();
                buf[0] = encodeConstant(buf, 1, value);
                if (size == SIZE_1_2) {
                    buf2[0] = 0;
                }
            }
            return buf;
        } else {
            // parameter is a local
            p.ungetc();
            return getLocal(buf,buf2,size);
        }
    }

    /**
     * Encode a constant into a byte array.
     * The encoding format for a constant value is as follows:
     *
     *           Format                |  Value range (inclusive)
     *   ------------------------------+-----------------------
     *   01xxxxxx                      | -(2**6)  .. (2**6)-1
     *   11xxxxxx 0xxxxxxx             | -(2**13) .. (2**13)-1
     *   11xxxxxx 1xxxxxxx 0xxxxxxx    | -(2**20) .. (2**20)-1
     *
     * ... and so on until the following 32 bit number:
     *
     *   11..xxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx
     *
     * or, if long, to the following 64 bit number:
     *
     *   11.....a 1xxxxxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx
     *
     * @param buf The buffer into which the encoded parameter is written.
     * @param offset The offset in buf at which to start writing.
     * @param value The value of the constant.
     * @return the number of bytes encoded into buf.
     *
    byte encodeConstant(byte[] buf, int offset, long value) {
        p.check(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE,"Constant too large: "+value);
        // Calculate the magnitude of the largest constant
        // that can be encoded in high byte
        long mag = 1 << (ENC_HIGH_BYTE_BITS - 1);
        boolean fits = (value >= -mag && value < mag);
        int pos = offset;
        if (!fits) {
            // The value won't fit in one byte so now fill all but
            // the highest order byte
            pos++;
            while(true) {
                buf[pos] = (byte)((value & ~ENC_MOREBYTES)|ENC_MOREBYTES);
                // Calculate the magnitude of the largest constant
                // that can be encoded in 13 (i.e. 6 + 7) bits
                mag = 1 << ((ENC_HIGH_BYTE_BITS + ENC_OTHER_BYTE_BITS) - 1);
                fits = (value >= -mag && value < mag);
                value = value >> ENC_OTHER_BYTE_BITS;
                if (fits) {
                    break;
                }
                pos++;
            }
            // Reverse the encoded bytes as we starting encoding from the
            // low order bytes but the final encoded form is high order
            // bytes first.
            for (int lo = offset + 1, hi = pos; lo < hi; lo++, hi--) {
                byte tmp = buf[hi];
                buf[hi] = buf[lo];
                buf[lo] = tmp;
            }
            // The low order byte needs to have its "continuation" bit unset
            buf[pos] &= ~ENC_MOREBYTES;

        }
        // Encode highest byte (which is guaranteed to fit in 6 bits)
        buf[offset] = (byte)(value | ENC_CONST);
        byte length = (byte)((pos - offset) + 1);
        // Set or unset the "continuation" bit depending on whether or
        // not the value could be encoded in 1 byte
        if (length > 1)
            buf[offset] |= ENC_MOREBYTES;
        else
            buf[offset] &= ~ENC_MOREBYTES;
        return length;
    }
    */

    /**
     * Encode a constant into a byte array.
     * The encoding formats are as follows:
     *
     *    01xxxxxx
     *    11000000 xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
     *
     * @param buf The buffer into which the encoded parameter is written.
     * @param offset The offset in buf at which to start writing.
     * @param value The value of the constant.
     * @return the number of bytes encoded into buf.
     */
    byte encodeConstant(byte[] buf, int offset, long value) {
        p.check((value & ~0xFFFFFFFF) == 0,"Constant too large: "+value);
        if (value >= -32 && value < 31) {
            buf[offset++] = (byte)(ENC_CONST|(value & 0x3F));
            return 1;
        } else {
            buf[offset++] = (byte)(ENC_COMPLEX|ENC_CONST);
            buf[offset++] = (byte)((value >> 24) & 0xFF);
            buf[offset++] = (byte)((value >> 16) & 0xFF);
            buf[offset++] = (byte)((value >> 8)  & 0xFF);
            buf[offset++] = (byte)((value >> 0)  & 0xFF);
            return 5;
        }
    }

    /**
     * Encode a class number into a byte array.
     * The encoding format is as follows:
     *
     *      111000000 xxxxxxxx xxxxxxxx
     */
    byte encodeClassNumber(byte[] buf, int offset, int cno) {
        p.check((cno & ~ 0xFFFF) == 0,"Class number too large");
        buf[offset++] = (byte)(ENC_COMPLEX|ENC_CONST|ENC_STATIC);
        buf[offset++] = (byte)((cno >> 8) & 0xFF);
        buf[offset++] = (byte)(cno & 0xFF);
        return 3;
    }

    /**
     * Parse a class operand.
     * @return the class number parsed.
     */
    int parseClassNumForInvoke() {
        p.check(p.eatWhiteSpace(false) == '&',"Bad class format");
        return p.getNumber(false);
    }

    /**
     * Parse a class operand.
     * @return the class number parsed.
     */
    int parseClassNum() {
        int classNum = parseClassNumForInvoke();
        p.check(classNum >= 0,"Bad class num");
        return classNum;
    }



    /**
     * Parse and encode a class operand.
     * @param buf The buffer to write the encoded result into.
     * @return the encoded result.
     */
    byte[] getClassNum(byte[] buf) {
        buf[0] = encodeConstant(buf,1,parseClassNum());
        return buf;
    }

    /**
     * Parse and encode a local variable or the constant 0 representing null.
     * @param buf The buffer to write the encoded result into.
     * @return the encoded result.
     */
    byte[] getObjectref(byte[] buf) {
        byte[] res = getParm(buf,null,SIZE_1);
        p.check(res[0] > 0 && (((res[1] & (byte)ENC_CONST) == 0) || ((res[1] & (byte)0x3F) == 0)),
            "Bad objectref");
        return res;
    }

    /**
     * Parse and encode a local variable offset to a word sized variable.
     * @param buf The buffer to write the encoded result into.
     * @return the encoded result.
     */
    byte[] getLocal(byte[] buf) {
        return getLocal(buf,null,SIZE_1);
    }

    /**
     * Parse and encode a local variable offset. If the local variable is a
     * dword sized variable, then the encoding emitted is to the 2 word
     * size variables comprising the dword, high word first.
     * @param buf The buffer to write the encoded result into.
     * @param buf2 The buffer to write the second (if any) encoded result into.
     * @param size The size constraint on the local variable.
     * @return the encoded result. If size == SIZE_1_2 then the number of
     * operands actually encoded is appended to the returned array.
     */
    byte[] getLocal(byte[] buf, byte[] buf2, int size) {
        // translate logical offset into activation record offset
        int num = p.getNumber(false);
        int entry = localOffsetTable[num];

        int offset = entry >> 16;
        int lsize  = entry & 0xFFFF;
        p.check(offset < 8192, "Local offset > 8192");
        if (lsize == 2) {
            p.check(size == SIZE_2 || size == SIZE_1_2,"Operand must be a word");
            // Dwords are presented as two ints
            if (HIGHWORDFIRST) {
                buf[0] = encodeLocal(buf, 1,offset);
                buf2[0]= encodeLocal(buf2,1,offset+1);
            }
            else {
                buf2[0]= encodeLocal(buf2,1,offset);
                buf[0] = encodeLocal(buf, 1,offset+1);
            }
        } else {
            p.check(size == SIZE_1 || size == SIZE_1_2,"Operand must be a dword");
            buf[0] = encodeLocal(buf, 1, offset);
            if (buf2 != null) {
                buf2[0] = 0;
            }
        }
        return buf;
    }

    /**
     * Encode a local variable offset.  The encoding for a local variable offset
     * into the activation record uses one of the two following formats. If the
     * value can be encoded in 6 bits (i.e. it is less than to 2^6), then the
     * first format is used otherwise the second applies (note that this is
     * different from the encoding for a constant value as 'signedness' is not
     * an issue for variable offsets):
     *
     *   00xxxxxx            (xxxxxx is the offset's value)
     *   10xxxxxx yyyyyyyy   (xxxxxxyyyyyyyy is the offset's value)
     *
     * @return the number of bytes encoded into buf.
     *
    byte encodeLocal(byte[] buf, int off, long offset) {
        p.check(offset >= 0,"Invalid local variable");
        if ((offset >> ENC_HIGH_BYTE_BITS) == 0) {
            buf[off] = (byte)offset;
            return (byte)1;
        } else {
            // High order byte
            buf[off  ] = (byte)(((offset >> 8) & ~ENC_CONST) | ENC_MOREBYTES);
            // Low order byte
            buf[off+1] = (byte)(offset & 0xFF);
            return (byte)2;
        }
    }
    */

    /**
     * Encode a local variable offset into a byte array.
     * The encoding formats are as follows:
     *
     *    00xxxxxx
     *    10xxxxxx xxxxxxxx
     */
    byte encodeLocal(byte[] buf, int offset, long local) {
        p.check(local >= 0 && ((local & ~ 0xFFFF) == 0),"Invalid local variable: " + local);
        if (local < 63) {
            buf[offset++] = (byte)(local);
            return 1;
        } else {
            buf[offset++] = (byte)(ENC_COMPLEX|((local >> 6) & 0xFF));
            buf[offset++] = (byte)(local & 0xFF);
            return 2;
        }
    }

    /**
     * Parse and encode a memory addressing expression. A memory address always
     * consists of a base and an offset. However, an assembly instruction may
     * optionally only provide an explicit offset. For example, load and store instructions
     * that refer to class static variables, don't include an explicit base
     * as it will always be the isolate state array. The encoded result always
     * include an explicit offset and base. Class variables are resolved to an
     * explicit offset from the isolate state array or object.
     * @param offset1 The buffer to write the first (and possibly only) encoded offset into.
     * @param base The buffer to write the encoded base into.
     * @param offset The buffer to write the second encoded offset into.
     * @return true if this is an array indexing memory expression, false otherwise.
     */
    boolean getLoadStoreAddress(byte[] offset, byte[] base, byte[] offset2) {
        parseRuntimeCheck(); // Ignore these for now
        int ch = p.getc();
        if (ch == '#') {
            // Offset is an int constant and so there must be a base address as well
            int value = p.getNumber(false);
            offset[0] = encodeConstant(offset, 1, value);
            // Ignore these for now
            parseRuntimeCheck();
            // The base address in this case must be a local reference variable
            getLocal(base,null,SIZE_1);
            if (offset2 != null) {
                // Indicate to the caller that the 3rd parameter was not used
                offset2[0] = 0;
            }
            return true;
        }
        else if (ch == '&') {
            // Parse class number and retrieve corresponding Class
            int value = p.getNumber();
            p.check(value >= 0, "Bad class number");
            ch = p.getc();
            ClassBase clazz;
            if (this.clazz.classIndex == value) {
                clazz = this.clazz;
            }
            else {
                try {
                    clazz = ClassBase.forNumber(value);
                } catch (LinkageError le) {
                    // The load/store instruction will be preceeded by a <clinit> instruction
                    // that will re-throw this LinkageError when it is executed. As such,
                    // all that is required here is to parse (and ignore) the reste of the
                    // instruction and then give the operands to the
                    // load/store instructions sensible values
                    p.check(ch == '@',"Bad class offset");
                    p.getNumber();
                    ch = p.eatWhiteSpace(false);
                    p.ungetc();
                    if (ch != '<') {
                        // Parse local
                        p.getNumber(false);
                    }

                    offset[0] = encodeConstant(offset, 1, 0);
                    base[0]   = encodeConstant(base, 1, 0);
                    if (offset2 != null) {
                        offset2[0] = encodeConstant(offset, 1, 0);
                    }

System.err.println("Load/store references class with linkage error: " + value);
                    return false;
                }
            }

            // Parse field offset
            p.check(ch == '@',"Bad class offset");
            value = p.getNumber();

            // Parse optional base
            int optBase;
            ch = p.eatWhiteSpace(false);
            p.ungetc();
            if (ch == '<') {
                // Resolve logical static field index to isolate state array index.
                value = clazz.sftable[value];
                // The base is the isolate state array (which contains the static
                // variables for all classes).
                base[0] = encodeConstant(base,1,AR_IsolateState & 0xFFFFFFFF);
            } else {
                // Resolve logical instance field index to object slot index.
                value = clazz.iftable[value];
                parseRuntimeCheck(); // Ignore these for now
                getLocal(base,null,SIZE_1);
            }
            offset[0]  = encodeConstant(offset,1,value);
            if (offset2 != null) {
                // This is a load/store from/to a dword field of an object
                offset2[0] = encodeConstant(offset2,1,value+1);
            }
            return false;
        }
        else {
            p.ungetc();
            // Offset is a local variable
            getLocal(offset,null,SIZE_1);
            // Parse local variable
            getLocal(base,null,SIZE_1);
            if (offset2 != null) {
                // Indicate to the caller that the 3rd parameter was not used
                offset2[0] = 0;
            }
            return true;
        }
    }

    /**
     * Methods that can be overriden by a subclass for tracing.
     */
    abstract void trace(Object msg)                       ;
    abstract void traceMethod(String name)                ;
    abstract void traceMethodEnd()                        ;
    abstract void traceInstructionStart(int address)      ;
    abstract void traceInstructionEnd()                   ;
    abstract void traceBC(int bc)                         ;
    abstract void traceOperand(byte[] operand)            ;
    abstract void traceIConst(byte[] iconst)              ;
    abstract void traceTarget(int address, byte[] target) ;
    abstract void traceInvoke(byte[] rcvr, byte[] slot)   ;

    /**
     * Parse instruction.
     */
    Instruction parseInstruction(Instruction last, boolean recursing) {

        // Parse the closing </I> and opening <I> if recursing
        if (recursing) {
            p.getTag(-p.T_I);
            p.getTag( p.T_I);
        }
        char[] buffer = p.getNonSpaceChars();
        int value = symbols.get(buffer);
        p.check(value != 0, "Unknown mnemonic: "+symbols.keyToString(buffer));
        String mnemonic = symbols.keyToString(buffer);
        int type = value >> 8;
        int bc   = value & 0xFF;
        int position = currentPosition;
        int addressOffset = 0;
        int addressCount = 0;
        asmPosition++;

traceInstructionStart(asmPosition);

        try {
            switch (type) {
                case TYPE_NOP:
traceBC(OPC_NOP);
traceInstructionEnd();

                    return(parseInstruction(last,true));

                // <>
                case TYPE_NATIVE:
                case TYPE_RETURNV:
                    emitCode(bc,NO_DST);
traceBC(bc);
                    break;

                // <address>
                case TYPE_GOTO:{
                    emitCode(bc,NO_DST);
                    addressOffset = currentPosition;
                    addressCount  = 1;
                    emitBytes(getAddress(dst1));
traceBC(bc);
traceTarget(asmPosition, dst1);
                    break;
                }

                // <class>
                case TYPE_CLINIT:
                    /** NOTE clinit only needs to load the class in order for the romizing process
                        to get a transitive closure of all the classes loaded. This used to just be:
                        emitCode(bc,NO_DST,getClassNum(dst1));
                        **/

                    int cno = parseClassNum();
                    dst1[0] = encodeConstant(dst1,1,cno);
                    emitCode(bc,NO_DST,dst1);
                    try {
                        ClassBase.forNumber(cno);
                    } catch (LinkageError le) {
                        // The class with the linkage error will rethrow the error when its
                        // <clinit> is called.
                    }

traceBC(bc);
traceOperand(dst1);
                    break;
                case TYPE_END: {
                    int classNum = parseClassNum();
                    ExceptionHandler handler = (ExceptionHandler)partialHandlers.pop();
                    p.check(handler.classNum == classNum,"Mismatched try/end: try class="+handler.classNum+", end class="+classNum);
                    handlers.addElement(handler);
                    last = parseInstruction(last,true);
                    handler.end = last;
                    return last;
                }

                // <class address>
                case TYPE_TRY: {
                    ExceptionHandler handler = new ExceptionHandler(parseClassNum(),parseAddress());
                    partialHandlers.push(handler);
                    Instruction start = parseInstruction(last,true);
                    handler.start = start;
                    return start;
                }

                // <local>
                case TYPE_CATCH:
                    emitCode(bc,getParm(src1));
traceBC(bc);
traceOperand(src1);
                    break;

                // <parm>
                case TYPE_NATIVE_P: // Fall through...
                case TYPE_THROW:
                    emitCode(bc,NO_DST,getParm(src1));
traceBC(bc);
traceOperand(src1);
                    break;

                // <local iconst address address* iconst*>
                case TYPE_LSWITCH: {
                    emitCode(bc,NO_DST,getParm(src1));
traceBC(bc);
traceOperand(src1);

                    // Now use src1 to hold the default case as it cannot be emitted
                    // immediately (need to parse matches first to calculate nparms).
                    getAddress(src1); // default case
traceTarget(asmPosition, src1);
                    addressCount = 1;
                    // Parse match addresses
                    int offset = 0;
                    int nparms = 0;
                    while (p.eatWhiteSpace(false) == '$') {
                        p.ungetc();
                        int encLength = getAddress(src2)[0];
traceTarget(asmPosition, src2);
                        p.check(offset + encLength <= parameters.length,"Too many matches");
                        System.arraycopy(src2,1,parameters,offset,encLength);
                        offset += encLength;
                        nparms++;
                        addressCount++;
                    }
                    p.ungetc();
                    // Now have all the info to continue encoding
                    p.check(nparms <= 0xFF,"Too many matches");
                    emitByte(nparms);
                    // Addresses are now emmitted
                    addressOffset = currentPosition;
                    // Emit the default case
                    emitBytes(src1);
                    // Have to do this manually as the number of bytes used to encoded
                    // the matches may be greater than can be represented in a byte
                    for (int i = 0 ; i != offset ; i++) {
                        emitByte(parameters[i]);
                    }
                    // Now parse the match values
                    for (int i = 0 ; i < addressCount-1 ; i++) {
                         emitBytes(getIConst(src1,true));
traceIConst(src1);
                    }
                    break;
                }

                // <local class>
                case TYPE_CCAST:
                    emitCode(bc,NO_DST,getObjectref(src1),getClassNum(src2));
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
                    break;

                // <local class [parm]>
                case TYPE_NEW: {
                    getLocal(dst1);
                    getParm(src1);
                    if (p.eatWhiteSpace(false) == '<') {
                        p.ungetc();
                        // The interpreter expects a length operand
                        src2[0] = encodeConstant(src2, 1, 0);
                    }
                    else {
                        p.ungetc();
                        getParm(src2);
                    }
                    emitCode(bc, dst1, src1, src2);
traceBC(bc);
traceOperand(src1);
traceOperand(src2);

                    break;
                }
                // <local iconst iconst address address*>
                case TYPE_TSWITCH: {
                    emitCode(bc, NO_DST, getParm(src1));
traceBC(bc);
traceOperand(src1);
                    emitBytes(getParm(src2)); // low
traceOperand(src2);
                    emitBytes(getParm(src3)); // high
traceOperand(src3);
                    addressOffset = currentPosition;
                    emitBytes(getAddress(src1)); // default case
traceTarget(asmPosition, src1);
                    addressCount = 1;
                    while (p.eatWhiteSpace(false) != '<') {
                        p.ungetc();
                        emitBytes(getAddress(src1));
traceTarget(asmPosition, src1);
                        addressCount++;
                    }
                    p.ungetc();
                    break;
                }

                // <local local>
                case TYPE_CSTORE:
                    emitCode(bc,NO_DST,getObjectref(src1),getLocal(src2));
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
                    break;

                // <local !local>
                case TYPE_ALENGTH:
                    emitCode(bc,getLocal(dst1));
traceBC(bc);
traceOperand(dst1);
                    parseRuntimeCheck(); // Ignore these for now
                    emitBytes(getLocal(src2));
traceOperand(src2);
                    break;

                // <local local class>
                case TYPE_INSTANCEOF:
                    getLocal(dst1);
                    emitCode(bc,NO_DST,getObjectref(src1),getClassNum(src2));
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
                    emitCode(OPC_GETI,dst1);
traceBC(OPC_GETI);
traceOperand(dst1);
                    break;

                // <        member local- parm*>
                case TYPE_INVOKE:
                    getLocal(dst1);
                    // Fall through...

                // <local2  member local- parm*>
                case TYPE_INVOKEL:
                    if (type != TYPE_INVOKE) {
                        getLocal(dst1,dst2,SIZE_2);
                    }
                    // Fall through...

                // <local   member local- parm*>
                case TYPE_INVOKEV: {
                    boolean isVirtual = true;
                    int classNumber = parseClassNumForInvoke();
                    if (classNumber < 0) {
                        classNumber = 0 - classNumber;
                        isVirtual = false;
                    }

                    /** NOTE only need to load the class in order for the romizing process
                        to get a transitive closure of all the classes loaded.
                        **/
                    try {
                        ClassBase.forNumber(classNumber);
                    } catch (LinkageError le) {
                        // The class with the linkage error will rethrow the error when its
                        // <clinit> is called.
                    }
                    p.check(p.getc() == '@',"Bad invoke instruction");
                    if (isVirtual) {
                        emitCode(OPC_INVOKEVIRTUAL, NO_DST);
                        src1[0] = 0;
                    } else {
                        emitCode(OPC_INVOKEABSOLUTE, NO_DST);
                        src1[0] = encodeClassNumber(src1, 1, classNumber);
                        emitBytes(src1);

                    }
                    emitBytes(getSlotConst(src4)); // slot number

traceInvoke(src1,src4);

                    // Parse parameters
                    int offset = 0;
                    int nparms = 0; // The receiver is always the first parameter
                    while (p.eatWhiteSpace(false) != '<') {
                        p.ungetc();
                        int encLength = getParm(src2,src3,SIZE_1_2)[0];
traceOperand(src2);
traceOperand(src3);
                        p.check(offset + encLength <= parameters.length,"Too many parameters");
                        System.arraycopy(src2,1,parameters,offset,encLength);
                        offset += encLength;
                        nparms++;
                        // Was it a dword parameter?
                        encLength = src3[0];
                        if (encLength != 0) {
                            System.arraycopy(src3,1,parameters,offset,encLength);
                            offset += encLength;
                            nparms++;
                        }
                    }
                    p.ungetc();
                    // Now have all the info to complete encoding
                    p.check(nparms <= 0xFF,"Too many parameters");
                    emitByte(nparms);

                    // Have to do this manually as the number of bytes used to encoded
                    // the parameters may be greater than can be represented in a byte
                    for (int i = 0 ; i != offset ; i++) {
                        emitByte(parameters[i]);
                    }

                    // Emit GETI (and or GETL instruction for non-void invokes to copy
                    // the returned result into the appropriate register
                    if (type != TYPE_INVOKEV) {
                        emitCode(OPC_GETI,dst1);
traceBC(OPC_GETI);
traceOperand(dst1);
                        // Copy low word of a long result
                        if (type == TYPE_INVOKEL) {
                            emitCode(OPC_GETI2,dst2);
traceBC(OPC_GETI2);
traceOperand(dst2);
                        }
                    }
                    break;
                }

                // <local iconst>
                case TYPE_LDCONST:
                    getLocal(dst1);
                    p.check(p.eatWhiteSpace(false) == '#',"Constant index into constant pool required");
                    int index = p.getNumber(false);
                    p.check(clazz.constTable != null && clazz.constTable.length > index,"Constant index out of bounds");
                    src1[0] = encodeConstant(src1,1,index);
                    emitCode(bc,dst1,src1);
traceBC(bc);
traceOperand(dst1);
//trace("#");
traceOperand(src1);
                    break;

                // <local !offset !local>
                case TYPE_LOAD: {
                    getLocal(dst1);
                    if (getLoadStoreAddress(src1,src2,null)) {
                        // This is an array indexing expression and so the bounds
                        // checking version of the opcode needs to be emitted
                        bc = bc + OPC_LOADSTORES_BC_INC;
                    }
                    emitCode(bc,dst1,  // dst
                                src2,  // base
                                src1); // offset
traceBC(bc);
traceOperand(dst1);
traceOperand(src1);
traceOperand(src2);
                    break;
                }

                // <local2 !offset !local>
                case TYPE_LOAD_D: {
                    getLocal(dst1,dst2,SIZE_2);
                    boolean isArrayLoad = getLoadStoreAddress(src1,src2,src3);
                    if (isArrayLoad) {
trace("+ MATH");
//traceOperand(dst1);
//traceOperand(dst2);

                        // This is an array indexing expression and so the bounds
                        // checking version of the opcode needs to be emitted
                        // Note that this conversion is different than the one used
                        // for 'native' bytecodes (e.g. OPC_LDI).
                        src3[0] = encodeConstant(src3,1,bc + 1);
                        emitCode(OPC_PARM, NO_DST,src3); // math opcode (e.g. MATH_LDL)
                        emitCode(OPC_PARM, NO_DST,src2); // base
                        emitCode(OPC_PARM, NO_DST,src1); // offset
                        emitCode(OPC_MATH1,dst1);        // dst (high word)
                        emitCode(OPC_GETI2,dst2);        // dst (low word)
//traceBC(OPC_PARM);
traceOperand(src3);
//traceBC(OPC_PARM);
traceOperand(src1);
//traceBC(OPC_PARM);
traceOperand(src2);
traceBC(OPC_MATH1);
traceOperand(dst1);
traceBC(OPC_GETI2);
traceOperand(dst2);





                    }
                    else {
                        emitCode(OPC_LDI,dst1,  // dst    (high word)
                                         src2,  // base
                                         src1); // offset (high word)
traceBC(OPC_LDI);
traceOperand(dst1);
traceOperand(src1);
traceOperand(src2);
                        emitCode(OPC_LDI,dst2,  // dst    (low word)
                                         src2,  // base
                                         src3); // offset (low word)
traceBC(OPC_LDI);
traceOperand(dst2);
traceOperand(src3);
traceOperand(src2);
                    }
                    break;
                }

                // <local parm>
                case TYPE_MOVE:
                case TYPE_NATIVE_LP:
                    emitCode(bc,getLocal(dst1),getParm(src1));
traceBC(bc);
traceOperand(dst1);
traceOperand(src1);
                    break;

                // <parm parm>
                case TYPE_NATIVE_PP:
                    emitCode(bc, NO_DST, getParm(src1), getParm(src2));
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
                    break;

                // <local2 parm2>
                case TYPE_MATH_DD:
                case TYPE_MOVE_DD:
                    getLocal(dst1,dst2,SIZE_2);
trace("+ MATH");
traceOperand(dst1);
traceOperand(dst2);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Math bytecode which returns high word of result
                    emitCode(OPC_MATH1,dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    // Copy low word of result
                    emitCode(OPC_GETI2,dst2);
traceBC(OPC_GETI2);
traceOperand(dst2);
                    break;

                // <local2 parm>
                case TYPE_MOVE_DW:
                    getLocal(dst1,dst2,SIZE_2);
trace("+ MATH");
//traceOperand(dst1);
//traceOperand(dst2);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    emitCode(OPC_PARM,NO_DST,getParm(src2));
//traceBC(OPC_PARM);
traceOperand(src2);
                    // Math bytecode which returns high word of result
                    emitCode(OPC_MATH1,dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    // Copy low word of result
                    emitCode(OPC_GETI2,dst2);
traceBC(OPC_GETI2);
traceOperand(dst2);
                    break;

                // <local parm2>
                case TYPE_MOVE_WD:
                    getLocal(dst1);
trace("+ MATH");
//traceOperand(dst1);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Math bytecode and copy of result which will contain the int value
                    emitCode(OPC_MATH1, dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    break;

                // <local parm parm>
                case TYPE_ALU:
                    emitCode(bc,getLocal(dst1),getParm(src1),getParm(src2));
traceBC(bc);
traceOperand(dst1);
traceOperand(src1);
traceOperand(src2);
                    break;

                // <local2 parm2 parm2>
                case TYPE_ALU_DDD:
                case TYPE_MATH_DDD:
                    getLocal(dst1,dst2,SIZE_2);
trace("+ MATH");
//traceOperand(dst1);
//traceOperand(dst2);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Second parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Math bytecode which returns high word of result
                    emitCode(OPC_MATH1,dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    // Copy low word of result
                    emitCode(OPC_GETI2,dst2);
traceBC(OPC_GETI2);
traceOperand(dst2);
                    break;

                // <local parm2 parm2>
                case TYPE_ALU_WDD:
                    getLocal(dst1);
trace("+ MATH");
//traceOperand(dst1);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Second parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Math bytecode and copy of result which will contain the int value
                    emitCode(OPC_MATH1, dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    break;

                // <local2 parm2 parm>
                case TYPE_ALU_DDW:
                    getLocal(dst1,dst2,SIZE_2);
trace("+ MATH");
//traceOperand(dst1);
//traceOperand(dst2);
                    // Math opcode
                    src1[0] = encodeConstant(src1,1,bc);
                    emitCode(OPC_PARM,NO_DST,src1);
//traceBC(OPC_PARM);
traceOperand(src1);
                    // First parameter
                    getParm(src2,src3,SIZE_2);
                    emitCode(OPC_PARM,NO_DST,src2);
//traceBC(OPC_PARM);
traceOperand(src2);
                    emitCode(OPC_PARM,NO_DST,src3);
//traceBC(OPC_PARM);
traceOperand(src3);
                    // Second parameter
                    emitCode(OPC_PARM,NO_DST,getParm(src2));
//traceBC(OPC_PARM);
traceOperand(src2);
                    // Math bytecode which returns high word of result
                    emitCode(OPC_MATH1,dst1);
traceBC(OPC_MATH1);
traceOperand(dst1);
                    // Copy of low word of result
                    emitCode(OPC_GETI2,dst2);
traceBC(OPC_GETI2);
traceOperand(dst2);
                    break;

                // <object>
                case TYPE_MONITOR:
                    if (p.eatWhiteSpace(false) == '&') {
                        p.ungetc();
                        int classNum = parseClassNum();
                        // Class monitor -> encode class num as constant
                        src1[0] = encodeClassNumber(src1,1,classNum);
                    }
                    else {
                        p.ungetc();
                        getLocal(src1);
                    }
                    emitCode(bc,NO_DST,src1);
traceBC(bc);
traceOperand(src1);
                    break;

                // <parm>
                case TYPE_NATIVE_L:
                    emitCode(bc,getLocal(dst1));
traceBC(bc);
traceOperand(dst1);
                    break;


                case TYPE_RETURN:
                    emitCode(bc,NO_DST,getParm(src1));
traceBC(bc);
traceOperand(src1);
                    break;

                // <parm2>
                case TYPE_RETURN_D:
                    getParm(src1,src2,SIZE_2);
                    emitCode(bc,NO_DST,src1,src2);
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
                    break;

                // <parm !offset !local>
                case TYPE_STORE: {
                    getParm(src1);
                    if (getLoadStoreAddress(src2,src3,null)) {
                        // This is an array indexing expression and so the bounds
                        // checking version of the opcode needs to be emitted
                        bc = bc + OPC_LOADSTORES_BC_INC;
                    }
                    emitCode(bc,NO_DST, // dst
                                src3,   // base
                                src2,   // offset
                                src1);  // src
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
traceOperand(src3);
                    break;
                }

                // <parm2 !offset !local>
                case TYPE_STORE_D: {
                    // This is really a 'src', not a 'dst1'...
                    getParm(src1,src2,SIZE_2);
                    boolean isArrayLoad = getLoadStoreAddress(src3,src4,src5);
                    if (isArrayLoad) {
trace("+ MATH");

                        // This is an array indexing expression and so the bounds
                        // checking version of the opcode needs to be emitted
                        // Note that this conversion is different than the one used
                        // for 'native' bytecodes (e.g. OPC_LDI).
                        p.check(src5[0] == 0,"Error in getLoadStoreAddress");
                        src5[0] = encodeConstant(src5,1,bc + 1);
                        emitCode(OPC_PARM, NO_DST,src5);  // math opcode (e.g. MATH_STL)
                        emitCode(OPC_PARM, NO_DST,src4);  // base
                        emitCode(OPC_PARM, NO_DST,src3);  // offset
                        emitCode(OPC_PARM, NO_DST,src1);  // src (high word)
                        emitCode(OPC_PARM, NO_DST,src2);  // src (low word)
//traceBC(OPC_PARM);
traceOperand(src5);
//traceBC(OPC_PARM);
traceOperand(src3);
//traceBC(OPC_PARM);
traceOperand(src4);
traceOperand(src1);
traceOperand(src2);
                        emitCode(OPC_MATH0,NO_DST);
//traceBC(OPC_MATH0);
                    }
                    else {
                        emitCode(OPC_STI,NO_DST,  // dst
                                           src4,  // base
                                           src3,  // offset (high word)
                                           src1); // src    (high word)
traceBC(OPC_STI);
traceOperand(src1);
traceOperand(src3);
traceOperand(src4);
                        emitCode(OPC_STI,NO_DST,  // dst
                                           src4,  // base
                                           src5,  // offset (low word)
                                           src2); // src    (low word)
traceBC(OPC_STI);
traceOperand(src2);
traceOperand(src5);
traceOperand(src4);
                    }
                    break;
                }

                // <parm parm address>
                case TYPE_IF: {
                    emitCode(bc, NO_DST, getParm(src1), getParm(src2));
                    addressOffset = currentPosition;
                    addressCount  = 1;
                    emitBytes(getAddress(src3));
traceBC(bc);
traceOperand(src1);
traceOperand(src2);
traceTarget(asmPosition, src3);
                    break;
                }
                default:
                    p.check(false,"Unimplemented type: "+type);
            }
        p.getTag(-p.T_I);
traceInstructionEnd();
        } catch (SquawkClassFormatError sle) {
            throw sle.addContext("instruction: "+asmPosition+" ("+position+"): "+mnemonic);
        }

        last = new Instruction(position, addressOffset, addressCount, asmPosition);
        t.traceln("("+asmPosition+") "+position+": "+mnemonic);
        if (type == TYPE_CATCH) {
            int matches = 0;
            for (Enumeration e = handlers.elements(); e.hasMoreElements();) {
                ExceptionHandler handler = (ExceptionHandler)e.nextElement();
                if (handler.catchAddress == asmPosition) {
                    p.check(handler.catchInstruction == null,"Bad catch instruction");
                    handler.catchInstruction = last;
                    matches++;
                }
            }
            /*
             * This may be one of the code sequences generated by JDK1.4 javac for
             * a monitorexit where that instruction is wrapped in a try/finally
             * block where the block is it's own handler (whatever sense that makes!).
             * In this case, the 'catch' instruction will actually appear before the
             * 'end' and so the partially built handler will be on top of the
             * partialHandlers stack.
             */
            if (!partialHandlers.empty()) {
                ExceptionHandler handler = (ExceptionHandler)partialHandlers.peek();
                if (handler.catchAddress == asmPosition) {
                    p.check(handler.catchInstruction == null,"Bad catch instruction");
                    handler.catchInstruction = last;
                    matches++;
                }
            }

            p.check(matches > 0, "can't find try/catch for handler at " + asmPosition);
        }
        return last;
    }


    /**
     * Emit code for an instruction that has (at least) a single result operand.
     */
    void emitCode(int bc, byte[] dst1) {
        emitByte(bc);
        emitBytes(dst1);
    }

    /**
     * Emit code for an instruction that has a single result operand and (at least) one operand.
     */
    void emitCode(int bc, byte[] dst1, byte[] src1) {
        emitLongOperand(1, src1);
        emitByte(bc);
        emitBytes(dst1);
        emitBytes(src1);
    }

    /**
     * Emit code for an instruction that has a single result operand and (at least) two operands.
     */
    void emitCode(int bc, byte[] dst1, byte[] src1, byte[] src2) {
        emitLongOperand(1, src1);
        emitLongOperand(2, src2);
        emitByte(bc);
        emitBytes(dst1);
        // The operands are emitted in reverse order as expected by the disassembler and
        // (more importantly!), the interpreter.
        emitBytes(src2);
        emitBytes(src1);
    }

    /**
     * Emit code for an instruction that has a single result operand and (at least) three operands.
     */
    void emitCode(int bc, byte[] dst1, byte[] src1, byte[] src2, byte[] src3) {
        emitLongOperand(1, src1);
        emitLongOperand(2, src2);
        emitLongOperand(3, src3);
        emitByte(bc);
        emitBytes(dst1);
        // The operands are emitted in reverse order as expected by the disassembler and
        // (more importantly!), the interpreter.
        emitBytes(src3);
        emitBytes(src2);
        emitBytes(src1);
    }


    /**
     * emitLongOperand
     */
     void emitLongOperand(int rs, byte[]src) {
         int len = src[0];
         p.check(len > 0,"byte length wrong: "+len);
         if ((src[1] & ENC_COMPLEX) != 0) {
             emitByte(OPC_OPERAND);
             emitByte(rs);
             emitBytes(src);
             src[0] = 1;
             src[1] = 0; // Means don't change rsN
         }
     }


    /**
     * emitBytes
     */
    void emitBytes(byte[] b) {
        int len = b[0];
        p.check(len < b.length,"byte length wrong: "+len);
        for (int i = 1 ; i <= len ; i++) {
            emitByte(b[i]);
        }
    }

    /**
     * emitByte
     */
    void emitByte(int b) {
        os.write(b);
        currentPosition++;
    }

    /**
     * Do relocation on a list of instructions and produce the resulting bytecode
     * array.
     * @param v A list of Instruction objects.
     * @param preambleSize The number of bytes required by the method's preamble.
     * @param debugInfoLength The number of bytes required by the method's debug info.
     */
    byte[] produce(Vector v, int preambleSize, int debugInfoLength) {
        byte[] bytecodes = os.toByteArray(preambleSize,debugInfoLength);
        int lth = v.size();
        for (int index = 0 ; index < lth ; index++) {
            Instruction i = (Instruction)v.elementAt(index);
            if (i != null)
                i.fixup(v, bytecodes, preambleSize);
        }
        return bytecodes;
    }

}

class Assembler extends AbstractAssembler {
    Assembler(ClassBase clazz, SquawkFileParser p, int[] localOffsetTable) {
        super(clazz,p,localOffsetTable);
    }
    void trace(Object msg)                       {}
    void traceMethod(String name)                {}
    void traceMethodEnd()                        {}
    void traceInstructionStart(int address)      {}
    void traceInstructionEnd()                   {}
    void traceBC(int bc)                         {}
    void traceOperand(byte[] operand)            {}
    void traceIConst(byte[] iconst)              {}
    void traceTarget(int address, byte[] target) {}
    void traceInvoke(byte[] rcvr, byte[] slot)   {}
}

/*---------------------------------------------------------------------------*\
 *                                Instruction                                *
\*---------------------------------------------------------------------------*/

class Instruction {
    /**
     * Address of instruction (relative to the first instruction) in the
     * generated bytecode array.
     */
    int position;
    /**
     * Address of first target operand (relative to the first instruction) in the
     * generated bytecode array.
     */
    int addressOffset;
    /**
     * Number of target operands. Will only be greater than 1 for 'switch' instructions.
     */
    int addressCount;
    /**
     * Squawk assembly instruction offset.
     */
    int asmPosition;


    Instruction(int position, int addressOffset, int addressCount, int asmPosition) {
        this.position      = position;
        this.addressOffset = addressOffset;
        this.addressCount  = addressCount;
        this.asmPosition = asmPosition;
    }

    void fixup(Vector v, byte[] bytecodes, int preambleSize) {
        try {
            for (int index = 0 ; index < addressCount ; index++) {
                int address   = addressOffset + preambleSize + (index * 2);
                int value     = (bytecodes[address] << 8) + (bytecodes[address+1] & 0xFF);
                // Find the next non-null instruction. This is for the case where the
                // target may have been a NOP or TRY for example.
                SquawkFileParser.check(v.size() > value,"address offset is off end of method ("+v.size()+" insts): "+value);
                while (v.elementAt(value) == null)
                    value++;
                Instruction i = (Instruction)v.elementAt(value);
                SquawkFileParser.check(i != null,asmPosition+": no instruction at "+value);
                int newValue  = i.position + preambleSize;
                SquawkClassLoader.setUnsignedHalf(bytecodes, address, newValue);
            }
        } catch (SquawkClassFormatError sle) {
            throw sle.addContext("instruction: "+asmPosition+" ("+position+")");
        }

    }
}

/*---------------------------------------------------------------------------*\
 *                          ExceptionHandler                                 *
\*---------------------------------------------------------------------------*/

/**
 * An exception handler.
 */
class ExceptionHandler {
    public ExceptionHandler(int classNum, int catchAddress) {
        this.classNum = classNum;
        this.catchAddress = catchAddress;
    }
    public String toString() {
        return "type="+classNum+", "+
               "start=("  +start.asmPosition+")"+start.position+", "+
               "end=("    +end.asmPosition  +")"+end.position+", "+
               "handler=("+catchInstruction.asmPosition+")"+catchInstruction.position;
    }

    Instruction start;
    Instruction end;
    Instruction catchInstruction;
    int catchAddress;
    int classNum;
}

/*---------------------------------------------------------------------------*\
 *                          SpecialByteArrayOutputStream                     *
\*---------------------------------------------------------------------------*/

/**
 * This subclass of ByteArrayOutputStream is used to prefix a fixed amount
 * of space into the byte array return by the toByteArray method. This is
 * used when generating the structure for a method.
 */
class SpecialByteArrayOutputStream extends ByteArrayOutputStream {
    byte[] toByteArray(int prefixSize, int postfixSize) {
        byte newbuf[] = new byte[count+prefixSize+postfixSize];
        System.arraycopy(buf, 0, newbuf, prefixSize, count);
        return newbuf;
    }
}

