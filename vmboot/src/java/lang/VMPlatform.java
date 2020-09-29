/*
 *
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
import com.sun.squawk.util.Find;

/**
 * VMPlatform is where the VM version specific parts reside.
 */
public abstract class VMPlatform implements com.sun.squawk.vm.SquawkOpcodes {

    /**
     * Instance of the VM used to build the ROMized heap (and interpret it).
     */
    static com.sun.squawk.vm.Interpreter vm;

/*---------------------------------------------------------------------------*\
 *                               Test harness                                *
\*---------------------------------------------------------------------------*/

    private static final String HEAP_TOC_FILE   = "squawk.heap.toc";
    private static final String HEAP_FILE       = "squawk.heap";

    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: romizer [-options] [ @classpath | class | +classlist ]* ");
        out.println("where options include:");
        out.println("    -image <image>      romize specified classes to file <image>");
        out.println("    -noClosure          do not include a total closure of the specified classes");
        out.println("    -classStats         display romized class statistics");
        out.println("    -dis <file>         write disassembly of romized methods to <file>");
        out.println("    -toc <file>         write romizied class table and isolate to <file>");
        out.println("    -heap <file>        write romized heap contents to <file>");
        out.println("    -traceLoading       trace class loading");
        out.println("    -traceParser        trace input read by class file parser");
        out.println("    -traceImage         trace image building");
        out.println("    -disassemble        disassemble methods to stdout");
        out.println("    -help               show this message and exit");
        out.println();
        out.println("Classes should be specified in '.' form (e.g. java.lang.Object)");
        out.println("classpath should use '/' and ':' (e.g. j2me/classes:j2se/classes)");
    }

    /**
     * Disassemble the methods of a class. Only the methods defined by a class are
     * disassembled, not those inherited from a superclass.
     */
    static void disassemble(ClassBase clazz, PrintStream ps) throws IOException
    {
        ClassBase sclazz = clazz.superClass;

        // Ignore array classes
        if (clazz.isArray()) {
            return;
        }

        // Only disassemble the fvtable it is not shared with the super class
        if (sclazz == null || clazz.fvtable != sclazz.fvtable) {
            byte[][] fvtable = clazz.fvtable;
            for (int slot = NativeOpcodes.SLOT_FIRST; slot != fvtable.length; slot++) {
                byte[] method = fvtable[slot];
                String id = clazz.classIndex+"@"+slot;
                if (sclazz == null || method != sclazz.fvtable[slot]) {
                    ps.println(id + ":");
                    Disassembler d = new Disassembler(ps,method);
                    d.disassemble(true);
                }
                else {
                    ps.println(id + " == " + sclazz.classIndex + "@" + slot);
                }
            }
        }
        else {
            ps.println(clazz.className+".fvtable == " + sclazz.className + ".fvtable");
        }

        // Only disassemble the vtable methods that were not inherited
        if (sclazz == null || clazz.vtable != sclazz.vtable) {
            byte[][] vtable        = new byte[clazz.vtable.length][];
            int vstart             = clazz.vstart;
            int vcount             = clazz.vcount;
            System.arraycopy(clazz.vtable,0,vtable,0,vtable.length);
            while (sclazz != null && (sclazz.vstart + (sclazz.vcount-1)) >= vstart) {
                // Skip classes that simply inherit their parent's vtable
                if (sclazz.superClass == null || sclazz.vtable != sclazz.superClass.vtable) {
                    int to, from;
                    if (vstart < sclazz.vstart) {
                        from = 0;
                        to = sclazz.vstart - vstart;
                    }
                    else {
                        from = vstart - sclazz.vstart;
                        to = 0;
                    }
                    while (from < sclazz.vcount && to < vcount) {
                        if (vtable[to] == sclazz.vtable[from]){
                            vtable[to] = null;
                        }
                        from++;
                        to++;
                    }
                }
                sclazz = sclazz.superClass;
            }
            // Now disassemble the vtable methods that were inherited
            sclazz = clazz.superClass;
            for (int slot = vstart; slot != (vtable.length + vstart); slot++) {
                byte[] method = vtable[slot - vstart];
                String id = clazz.classIndex+"@"+slot;
                if (method == null) {
                    ps.println(id + " == " + sclazz.classIndex + "@" + slot);
                    continue;
                }
                ps.println(id + ":");
                Disassembler d = new Disassembler(ps,method);
                d.disassemble(true);
            }

        }
        else {
            ps.println(clazz.className + ".vtable == " + sclazz.className + ".vtable");
        }

    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage(null);
            return;
        }

        Vector imageClasses = new Vector();
        String imageFile    = null;
        String tocFile      = null;
        String heapFile     = null;
        String disFile      = null;
        boolean showClassStats = false;

        // Parse args
        for (int i = 0; i != args.length; i++) {
            String arg = args[i];
            if (arg.equals("-classStats")) {
                showClassStats = true;
            }
            else
            if (arg.equals("-dis")) {
                disFile = args[++i];
            }
            else
            if (arg.equals("-toc")) {
                tocFile = args[++i];
            }
            else
            if (arg.equals("-heap")) {
                heapFile = args[++i];
            }
            else
            if (arg.equals("-image")) {
                imageFile = args[++i];
            }
            else
            if (arg.equals("-help")) {
                usage(null);
                return;
            }
            else
            if (arg.equals("-traceLoading")) {
                System.setProperty("squawk.trace.classloading",             "true");
            }
            else
            if (arg.equals("-traceParser")) {
                System.setProperty("squawk.trace.classloading.input",       "true");
            }
            else
            if (arg.equals("-disassemble")) {
                System.setProperty("squawk.trace.classloading.disassemble", "true");
            }
            else
            if (arg.equals("-traceImage")) {
                com.sun.squawk.vm.ObjectMemory.TRACEROMIZING = true;
            }
            else
            if (arg.startsWith("@")) {
                Find.findAllClassesInPath(arg.substring(1), imageClasses);
            }
            else
            if (arg.startsWith("+")) {
                String fileName = arg.substring(1);
                FileInputStream in = new FileInputStream(fileName);
                InputStreamReader isr = new InputStreamReader(in);
                String line = getLine(isr);
                while (line != null) {
                    if (!line.startsWith("#")) {
                        int space = line.indexOf(' ');
                        if (space == -1) {
                            imageClasses.addElement(line);
                        }
                        else {
                            imageClasses.addElement(line.substring(0, space));
                        }
                    }
                    line = getLine(isr);
                }
                in.close();
            }
            else
            if (arg.startsWith("-")) {
                usage("unknown option: "+arg);
                return;
            }
            else {
                imageClasses.add(arg);
            }
        }

        // Initialize the static fields in ClassBase.
        ClassBase.objectClass = ClassBase.forNumber(NativeOpcodes.CNO_Object);

        // Process the classes given on the command line
        int classNumber;
        for (Enumeration e = imageClasses.elements(); e. hasMoreElements();){
            String classArg = (String)e.nextElement();
            try {
                // Get class number
                classNumber = Integer.parseInt(classArg);
            } catch (NumberFormatException nfe) {
                // Convert class name to number
                classNumber = SquawkClassLoader.lookup(classArg.replace('.','/'), true, false);
            }
            // Now try load the class
            try {
                ClassBase.forNumber(classNumber);
            } catch (LinkageError le) {
                System.err.println("Class " + classNumber + " has a linkage error: " + le.getMessage());
            }
        }

        // Load all classes with hard coded class numbers
        for (int i = NativeOpcodes.CNO_Object; i <= NativeOpcodes.CNO_InitLimit; i++) {
            try {
                ClassBase.forNumber(i);
            } catch (LinkageError le) {
                System.err.println("Class " + i + " has a linkage error: " + le.getMessage());
            }
        }

        // Ensure that all classes have their methods loaded and assembled
        // The trick with the copy is to only load the methods of classes that
        // were explicitly loaded, not those that were only loaded as the
        // result of assembly a class's methods.
        Vector v = ClassesInLinkOrder;
        //ClassesInLinkOrder = null;          ** Don't do the trick! **
        for (int i = 0; i != v.size(); i++) {
            ClassBase clazz = (ClassBase)v.elementAt(i);
            if (clazz != null) {
                clazz.ensureMethodsLoaded();
            }
        }
        ClassesInLinkOrder = v;

        // Now ensure that all array classes (that exist in the translator) are
        // also loaded
        for (int i = 0; i != ClassesInLinkOrder.size(); i++) {
            ClassBase clazz = (ClassBase)ClassesInLinkOrder.elementAt(i);
            if (clazz != null) {
                String arrayClazz = "[" + clazz.className.replace('.','/');
                while (true) {
                    int cno = SquawkClassLoader.lookup(arrayClazz, false, false);
                    if (cno == -1) {
                        break;
                    }
                    ClassBase.forNumber(cno);
                    arrayClazz = "[" + arrayClazz;
                }
            }
        }

        // Initialise the VM
        vm = new com.sun.squawk.vm.Interpreter(new String[] { "-Xms4M" });
        // Resize the isolateStateOopMap to be exactly the right size
        int oopMapLength = (ClassBase.isolateStateLength + 7) / 8;
        if (oopMapLength < ClassBase.isolateStateOopMap.length) {
            byte[] old = ClassBase.isolateStateOopMap;
            ClassBase.isolateStateOopMap = new byte[oopMapLength];
            System.arraycopy(old,0,ClassBase.isolateStateOopMap,0,oopMapLength);
        }
        vm.build(ClassBase.classTable.length,ClassBase.isolateStateLength,ClassBase.isolateStateOopMap);

        // Transfer the classes into the VM heap
        Vector classAddrs = new Vector();
        for (int i = 0; i != ClassesInLinkOrder.size(); i++) {
            ClassBase clazz = (ClassBase)ClassesInLinkOrder.elementAt(i);
            if (clazz != null) {
                System.out.println("Interning "+clazz.className);
                int[] interfaces;
                if (clazz.interfaces != null) {
                    interfaces = new int[clazz.interfaces.length];
                    for (int j = 0; j != interfaces.length; j++) {
                        interfaces[j] = clazz.interfaces[j].classIndex;
                    }
                }
                else {
                    interfaces = new int[0];
                }
                // Only string constants are currently supported by the heap builder.
                String[] constants = new String[clazz.constTable == null? 0 : clazz.constTable.length];
                if (clazz.constTable != null) {
                    System.arraycopy(clazz.constTable,0,constants,0,constants.length);
                }
                // The first slot of the fvtable is a zero length array. This slot should
                // never be indexed by the VM
                if (clazz.fvtable[0] == null) {
                    clazz.fvtable[0] = new byte[0];
                }
                classAddrs.addElement(new Integer(vm.createClass(clazz.classIndex,
                    clazz.accessFlags,
                    clazz.length,
                    clazz.className,
                    (clazz.superClass == null ? 0 : clazz.superClass.classIndex),
                    (clazz.elementType == null ? 0 : clazz.elementType.classIndex),
                    interfaces,
                    clazz.vtable,
                    clazz.vstart,
                    clazz.vcount,
                    clazz.fvtable,
                    clazz.itable,
                    clazz.istart,
                    clazz.iftable,
                    clazz.sftable,
                    constants,
                    clazz.debugInfo,
                    clazz.oopMap)));

                // Ensure that the initial statics of java.lang.ClassBase occupy the initial
                // slots in the isolate state array
                if (clazz.classIndex == CNO_ClassBase) {
                    for (int s = ISO_FIRST; s <= ISO_LAST; s++) {
                        Native.assume(clazz.sftable[s - ISO_FIRST] == s);
                    }
                }
            }
        }

        // Transfer the isolate state into the VM heap
        Native.assume(vm.getIsolateStateLength() == ClassBase.isolateStateLength);
        for (int i = ISO_isolateStateLength + 1; i != ClassBase.isolateStateLength; i++) {
            Object entry = IsolateState[i];
            if (entry != null) {
                if (entry instanceof Integer) {
                    vm.addEntryToIsolateState(i,((Integer)entry).intValue());
                }
                else if (entry instanceof String) {
                    vm.addStringToIsolateState(i,(String)entry);
                }
                else {
                    // Can't add any other types to the isolate array yet...
                    Native.assume(false);
                }
            }
        }

        vm.dumpHeapStats(System.out,"After loading bootstrap classes:");
        if (showClassStats) {
            // Print the classes
            for (Enumeration e = classAddrs.elements(); e.hasMoreElements();) {
                    vm.printClass(System.out,((Integer)e.nextElement()).intValue(),false);
                }
        }

        // Write out the disassembled methods if necessary
        if (disFile != null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(disFile),classAddrs.size()*1000));
                // Disassemble the methods
                for (int i = 0; i != ClassesInLinkOrder.size(); i++) {
                    ClassBase clazz = (ClassBase)ClassesInLinkOrder.elementAt(i);
                    if (clazz != null) {
                        disassemble(clazz,ps);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }

        }
        // Write out memory table of contents if necessary
        if (tocFile != null) {
            try {
                PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(tocFile),classAddrs.size()*1000));
                // Print the class table
                vm.printClassTable(ps,true);
                // Print the classes
                for (Enumeration e = classAddrs.elements(); e.hasMoreElements();) {
                    vm.printClass(ps,((Integer)e.nextElement()).intValue(),true);
                }
                // Print the IsolateState
                vm.printIsolateState(ps);
                ps.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        // Write out heap if necessary
        if (heapFile != null) {
            try {
                int lengthWord      = vm.p_getCurrentSpaceUsed();
                int lengthByte      = lengthWord * 4;
                PrintStream psWord  = new PrintStream(new BufferedOutputStream(new FileOutputStream(heapFile+".words"),lengthWord*2));
                PrintStream psByte  = new PrintStream(new BufferedOutputStream(new FileOutputStream(heapFile+".bytes"),lengthByte*2));
                psByte.println("memory: " + lengthByte + " bytes");
                for (int i = 0 ; i < lengthByte ; i++) {
                    if (i % 10 == 0) {
                        psByte.print("\n"+i+": ");
                    }
                    psByte.print(vm.p_getUnsignedByte(i)+" ");
                }
                psByte.println();
                psByte.close();
                psWord.println("memory: " + lengthWord + " words");
                for (int i = 0 ; i < lengthWord ; i++) {
                    if (i % 8 == 0) {
                        psWord.print("\n"+i+": ");
                    }
                    psWord.print(vm.p_getWord(i)+" ");
                }
                psWord.println();
                psWord.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Write out the image if necessary
        if (imageFile != null) {
            try {
                DataOutputStream ps = new DataOutputStream(new FileOutputStream(imageFile));
                int length;
                // Do a garbage collection so that the smallest possible image is written.
                vm.compressHeap();
                length = vm.p_getCurrentSpaceFreePtr();
                for (int i = 0 ; i < length ; i++) {
                    ps.writeInt(vm.p_getWord(i));
                }
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * getLine
     */
    static String getLine(InputStreamReader isr) throws Exception {
        StringBuffer sb = new StringBuffer();
        int ch = isr.read();
        if (ch == -1) {
            return null;
        }
        while (ch != '\n' && ch != -1) {
            sb.append((char)ch);
            ch = isr.read();
        }
        return sb.toString();
    }

/*---------------------------------------------------------------------------*\
 *                     ClassBase.IsolateState related methods                *
\*---------------------------------------------------------------------------*/

    static Object[] IsolateState;

    /**
     * Increases the capacity of all the arrays associated with the globals
     * for an isolate. This includes the isolate state table and its oop map.
     * @param minCapacity The desired minimum capacity for all isolate state arrays.
     */
    private static void ensureIsolateStateCapacity(int minCapacity) {
        int length = ClassBase.isolateStateLength;
        // Do the IsolateState first. If it has sufficient capacity already,
        // then its oop map will as well.
        if (length >= minCapacity)
            return;

        int newLength = ((length+1) * 4);
        if (newLength < minCapacity) {
            newLength = minCapacity;
        }
        // Copy the IsolateState
        Object old = IsolateState;
        IsolateState = new Object[newLength];
        System.arraycopy(old, 0, IsolateState, 0, length);

        // Copy the oop map
        length = (length+7) / 8;
        newLength = (newLength+7) / 8;
        if (length < newLength) {
            old = ClassBase.isolateStateOopMap;
            ClassBase.isolateStateOopMap = new byte[newLength];
            System.arraycopy(old, 0, ClassBase.isolateStateOopMap, 0, length);
        }
    }

    /**
     * Add a primitive entry to the IsolateState array. The entry is added at an
     * offset relative to the start of the array that is in terms of it size.
     * @param entry Contains the data to add.
     * @returns the word index of the inserted data
     */
    static int addPrimitiveToIsolateState(int value) {
        return addEntryToIsolateState(value != 0 ? new Integer(value) : null);
    }
    static int addEntryToIsolateState(Object entry) {
        if (IsolateState == null) {
            IsolateState = new Object[ISO_LAST+1];
            ClassBase.isolateStateOopMap = new byte[] { ISO_MAP0 };
            ClassBase.isolateStateLength = ISO_LAST + 1;
        }
        int length = ClassBase.isolateStateLength;

        // Required space is current length plus 1.
        int required = length + 1;

        // Ensure the capacity of isolate state arrays
        ensureIsolateStateCapacity(required);

        // Update the *used* length of the isolate arrays
        ClassBase.isolateStateLength = required;

        // Add the entry
        IsolateState[length] = entry;

        return length;
    }

    /**
     * Add a reference entry to the IsolateState array. The entry is added at
     * the next available offset of the array.
     * @param ref The reference to add.
     * @returns the index at which the reference was added.
     */
    static int addReferenceToIsolateState(Object ref) {
        int index = addEntryToIsolateState(ref);
        // Set the relevant bit in the isolate state oop map
        ClassBase.isolateStateOopMap[index/8] |= 1 << (index % 8);
        return index;
    }

    /**
     * The Squawk VM maps the first few statics of ClassBase to special
     * slots in the isolate state array:
     *
     *   ClassBase.isolateState       -> slot ISO_isolateState
     *   ClassBase.isolateStateOopMap -> slot ISO_isolateStateOopMap
     *   ClassBase.isolateStateLength -> slot ISO_isolateStateLength
     *   ClassBase.classTable         -> slot ISO_classTable
     *   ClassBase.classThreadTable   -> slot ISO_classThreadTable
     *   ClassBase.classStateTable    -> slot ISO_classStateTable
     *
     * This method does those mappings.
     */
    static void mapIsolateStateFields(ClassBase clazz) {
        if (clazz.classIndex != CNO_ClassBase) {
            return;
        }
        clazz.sftable[ISO_isolateState       - ISO_FIRST] = ISO_isolateState;
        clazz.sftable[ISO_isolateStateOopMap - ISO_FIRST] = ISO_isolateStateOopMap;
        clazz.sftable[ISO_isolateStateLength - ISO_FIRST] = ISO_isolateStateLength;
        clazz.sftable[ISO_classTable         - ISO_FIRST] = ISO_classTable;
        clazz.sftable[ISO_classThreadTable   - ISO_FIRST] = ISO_classThreadTable;
        clazz.sftable[ISO_classStateTable    - ISO_FIRST] = ISO_classStateTable;
        clazz.sftable[ISO_isolateId          - ISO_FIRST] = ISO_isolateId;
    }

/*---------------------------------------------------------------------------*\
 *                     ClassBase.classTable related methods                  *
\*---------------------------------------------------------------------------*/

    static Vector       ClassesInLinkOrder;

    /**
     * Receive notification that a given class was linked into the system.
     */
    static void classLinked(ClassBase clazz) {
        if (ClassesInLinkOrder == null) {
            ClassesInLinkOrder = new Vector(300);
        }
        ClassesInLinkOrder.addElement(clazz);
    }

    static ClassBase createClass(int id, int extnds, int arrayOf,
        String name, Vector impls, Vector constants, Vector svars, Vector ivars, Vector i_map,
        int accessFlags, boolean usesFvtable, int vtableStart, int vtableEnd, int firstNVMethod,
        byte[] debugInfo)
    {
        return new ClassBase(
            id,extnds,arrayOf,name,impls,constants,svars,ivars,i_map,
            accessFlags,usesFvtable, vtableStart, vtableEnd, firstNVMethod, debugInfo);
    }
    /**
     * This is useful for debugging the SquawkClassLoader runnning in squawk.
     * It does nothing under J2SE.
     */
    static void setTracingThreshold(int i) {
    }

    /**
     * Return the system properties specified for current isolate. This needs to be
     * in this class as it is referenced from the static initializer of Native
     * which is run under both Hotspot and Squawk and therefore cannot directly
     * reference any Squawk only classes (such as Isolate).
     */
    static Hashtable getCurrentIsolateProperties() {
        return null;
    }

}
