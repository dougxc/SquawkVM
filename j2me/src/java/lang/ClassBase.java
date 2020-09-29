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
import javax.microedition.io.*;

/**
 * ClassBase is where all the Squawk specific support for java.lang.Class resides.
 * To support testing and development under a non-squawk VM (as much as
 * possible), 2 versions of this class are maintained, one under the j2se
 * branch and one under the j2me branch. Ideally, they will mirror each other
 * as closely as possible with the only differences being certain lines are
 * commented out in one and not the other and also certain other lines
 * differ altogether. However, both sources should have exactly the same number
 * of lines so that a manual merge can be done easily using a diff tool
 * ('tkdiff' is highly recommended).
 */
public class ClassBase implements NativeOpcodes {

    /**
     * The state of the current isolate. This is mapped to the first element of
     * the array pointed to by com.sun.squawk.vm.GarbageCollector.IsolateState.
     * The first ISO_LAST entries in this array have a special meaning:
     *
     *  0 - Pointer to the isolate state itself.
     *  1 - Pointer to oop map for isolate state
     *  2 - Current length of isolate state.
     *  3 - Class table
     *  4 - Class initializaing threads table
     *  5 - Class initialization state table
     *  6 - Unique String identifier for the isolate
     *
     * Note that the length entry may be less than the actually length of the
     * memory chunk allocated for the IsolateState. It also serves as the
     * index of the next available insertion point.
     */
    static int[]  isolateState;                     // Mapped to slot ISO_isolateState
    static byte[] isolateStateOopMap;               // Mapped to slot ISO_isolateStateOopMap
    static int    isolateStateLength;               // Mapped to slot ISO_isolateStateLength

    /**
     * The class table is actually a table of references to prototype instances of each class.
     * The primary use of a prototype instances is to act as the receiver object in
     * static method invocations. This allows the VM to process static and object
     * oriented method invocations the same way. Each prototype object is placed
     * into the class table at an index that is equal to the class number. The main
     * use of the class table in this code is to transform a class number into a
     * reference to the class data structure.
     *
     *   Class table
     *
     *                   -------------        --------------
     *                   |     *-----|------> | Class      |
     *   ---------       -------------        | definition |
     *   |   *---|-----> | Prototype |        --------------
     *   ---------       | Instance  |
     *   |       |       -------------
     *   ---------
     *   |       |
     *   ---------
     *   |       |
     *   ---------
     *   |       |
     *   ---------
     *
     * The prototype objects do not have the same structure as a regular object of
     * the class to which they refer. Instead they have the format of a byte array. These
     * byte arrays contain an oop map for the regular objects of the class in question.
     * Each byte therefore represents eight words of a class instance. The headers of these
     * byte arrays do not point to the class for byte[] but to the class for which
     * the byte array represents the oop map. In this way they appear to be normal instances
     * of the class and can be used for the receiver of a static method. However in all
     * other prespects they are like byte arrays, and have a length field. As these byte arrays
     * are only used by the garbage collector the fact that the array objects do not have
     * the class byte[] is not important to the rest of the system.
     *
     * 05/08/02: The prototype object mechanism has been removed. Using a prototype object
     * in order to treat static method invocations in exactly the same way as virtual
     * method invocations proves to be fundamentally flawed. This is due to the special
     * semantics of INVOKESPECIAL where an absolute method needs to be specified that
     * cannot be addressed by means of vtable lookups.
     *
     * As such, the class table is now simply a table of class objects, indexed by
     * class index numbers.
     */
    static ClassBase[] classTable;                  // Mapped to slot ISO_classTable

    /**
     * The thread being used to execute the class's static initializer method
     * (i.e. <clinit>).
     */
    static Thread[] classThreadTable;               // Mapped to slot ISO_classThreadTable

    /**
     * Initialization state of a class. The first element is 0 while the system classes
     * have not been initialized for the current Isolate. While this condition
     * prevails, a simplified form of class initialization is performed that
     * assumes a single threaded model as the system classes include those that
     * implement thread support.
     */
    static byte[]   classStateTable;                // Mapped to slot ISO_classStateTable

    /**
     * A unique String identifier for the isolate. This field is only given a value
     * when the isolate is once for the first time. The primary purpose of this
     * field is to enable easy access to a unique identifier for an isolate from
     * within the VM.
     */
    static String isolateId;

    /**
     * Pointer to the class for java.lang.Class (initialized by VMPlatform).
     */
//    static ClassBase metaClass;

    /**
     * Pointer to the class for java.lang.Object (initialized by VMPlatform).
     */
    static ClassBase objectClass;


    /**
     * Ensures the capacity of all the class table is a given minimum.
     * The class intializing threads array and class initialization state array
     * is expanded (if necessary) at class initialization time.
     * @param minCapacity The desired minimum capacity for the class table.
     */
    private static void ensureClassCapacity(int minCapacity) {
        if (classTable == null) {
            // Only happens in romizer
            classTable = new ClassBase[minCapacity];
        }
        int oldLength = classTable.length;
        if (oldLength >= minCapacity)
            return;
        int newLength = ((oldLength+1) * 3) / 2;
        if (newLength < minCapacity) {
            newLength = minCapacity;
        }
        // Copy the classTable
        ClassBase[] newClassTable = new ClassBase[newLength];
        System.arraycopy(classTable, 0, newClassTable, 0, oldLength);
        classTable = newClassTable;
    }

    /**
     * Get a class from a class number, loading it if necessary.
     * @param classNumber A class ID.
     */
    static ClassBase forNumber(int classNumber) {
        ensureClassCapacity(classNumber+1);
        ClassBase clazz = ClassBase.classTable[classNumber];
        if (clazz == null) {
            clazz = SquawkClassLoader.load(classNumber);
        }
        Native.assume(clazz == ClassBase.classTable[classNumber]);
        if ((clazz.accessFlags & ACC_LINKAGEERROR) != 0) {
            throw new LinkageError((String)clazz.constTable[0]);
        }
        return clazz;
    }


    /**
     * Ensure that a class's methods are loaded and assembled. This also
     * ensures that all the superclasses' (recursively) methods are
     * loaded and assembled.
     */
    void ensureMethodsLoaded() {
        if (superClass != null) {
            superClass.ensureMethodsLoaded();
        }
        if (fvtable[SLOT_FIRST] != null && (vtable.length == 0 || vtable[0] != null)) {
            return;
        }

        // Copy fvtable from superClass if this is not java.lang.Object and this
        // class does not share its superclass's fvtable
        if (superClass != null && fvtable[SLOT_FIRST] == null) {
            System.arraycopy(superClass.fvtable,0,fvtable,0,fvtable.length);
        }

        // Copy the super class's methods (recursively) if this is not java.lang.Object
        // and this class does not share its superclass's vtable
        if (superClass != null && vtable[0] == null) {
            ClassBase sclazz = superClass;
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
                        if (vtable[to] == null){
                            vtable[to] = sclazz.vtable[from];
                        }
                        from++;
                        to++;
                    }
                }
                sclazz = sclazz.superClass;
            }
        }

System.out.println("Loading methods for "+className+" (cno="+classIndex+")");
long now = System.currentTimeMillis();
        SquawkClassLoader.loadMethods(this);
        Native.assume(fvtable[SLOT_FIRST] != null && (vtable.length == 0 || vtable[0] != null));
System.out.println("Loaded  methods for "+className+" (cno="+classIndex+") time="+(System.currentTimeMillis()-now)+"ms");

    }

    /*
     * Return the class for java.lang.Class
     */
//    static ClassBase metaClass() {
//        return metaClass;
//    }

   /* ------------------------------------------------------------------------ *\
    *                                Instance Data                             *
   \* ------------------------------------------------------------------------ */

    /*
     * Instance Data
     *
     * Note this *must* match the definition in NativeOpcodes.java
     */

    final Object        self;              // Word offset 0
    final int           classIndex;        // Class ID
          int           accessFlags;
    final int           gcType;
    final int           length;            // size (in words) of an instance of this class
    final String        className;
    final ClassBase     superClass;
    final ClassBase     elementType;       // Element type for array classes
    final ClassBase[]   interfaces;        // Implemented interfaces

    /**
     * The table of methods of this class. If this class does not override any
     * of its inherited methods, then this table will only contain the
     * methods (static and non-static) explicitly declared by the class. If it
     * does override at least one of its inherited methods, then the table
     * will include a prepended copy of all the slots from its inherited vtable from
     * the slot of the first overriden method onwards. The remainder of the
     * table is the methods explicitly declared by this class. The value of
     * "vstart" is the offset first overridden method. This means that method
     * lookup will use this class's vtable if the method index is >= vstart.
     * Otherwise, a recursive lookup in the vtables of the superclass
     * chain is performed.
     *
     * The static methods will always be at the end of the table and the value
     * of "vstart" for any subclasses will always be <= the index of the first
     * static method.
     *
     * The diagram below depicts the vtable and fvtable for a set of classes:
     *
     *     class A {
     *         void open()  { ... }
     *         void draw()  { ... }
     *         void close() { ... }
     *     }
     *
     *     class B extends A {
     *         void draw()  { ... }
     *         static B instance() { ... }
     *     }
     *
     *     class C extends B {
     *         String toString() { ... }
     *         void close() { ... }
     *     }
     *
     *
     *                               +--------------+
     * Object {                      | clone        |
     *     vstart: N+1               +--------------+
     *     fvtable:--------------->  | wait         |
     *     vtable: null              +--------------+
     * }                             :              :
     *                               :              :
     *                               +--------------+
     *                               | toString     |
     *                               +--------------+
     *
     * A {
     *     vstart: N+1               +--------------+
     *     fvtable: Object.fvtable   | open         |
     *     vtable:---------------->  +--------------+
     * }                             | draw         |
     *                               +--------------+
     *                               | close        |
     *                               +--------------+
     *
     *
     * B {                           +--------------+
     *     vstart: N+2               | draw         |
     *     fvtable: Object.fvtable   +--------------+
     *     vtable:-------------->    | A.close      |
     * }                             +--------------+
     *                               | instance     |
     *                               +--------------+
     *
     *                               +--------------+
     * C {                           | Object.clone |
     *     vstart: N+3               +--------------+
     *     fvtable:------------->    | Object.wait  |
     *     vtable: ----+             +--------------+
     * }               |             :              :
     *                 |             :              :
     *                 |             +--------------+
     *                 |             | toString     |
     *                 |             +--------------+
     *                 |
     *                 |             +--------------+
     *                 +-------->    | close        |
     *                               +--------------+
     * This field has type byte[][] as methods are implemented as an
     * array of bytes.
     */
    final byte[][] vtable;

    /**
     * The logical index of the first method of this class whose definition
     * is found in the vtable of this class. Both this, the vtable and the
     * vcount fields will be directly set from the corresponding fields in the
     * superclass in the case where this class does not define any methods.
     */
    final int vstart;

    /**
     * The number of virtual methods defined (or overidden) by this class
     * that are found in the vtable.
     */
    final int vcount;

    /**
     * The first SLOT_FVTABLE_LENGTH methods of a class are indexed by this table.
     * Typically SLOT_FVTABLE_LENGTH will be set to be large enough to contain
     * only the methods of Object that are commonly overriden which is primarily
     * <init>.
     */
    final byte[][] fvtable;

    /**
     * The mapping of an interface method ID to the vtable (or fvtable)
     * offset of the method that provides the implementation of the interface
     * method. This encompasses all interfaces methods implemented by this
     * class, even if the implementing method is inherited. Note that all
     * interfaces in the system have a unique ID ranging from 0 to the
     * number of interfaces in the system. As such, there is potential for
     * this table to be as large as the number of interfaces in the system.
     * However, the methods of any given interface will have IDs in a continuous
     * range and given that most classes only implement 1 interface, this itable
     * will most likely have a size equal to the number of methods for the
     * interface that it implements.
     */
    final short[]  itable;

    /**
     * The offset of the first interface method implemented by this class.
     */
    final int istart;

    /**
     * Mapping of logical instance field offsets to object slot index. The object
     * slot index is in terms of the size of the data type of the given field.
     */
    final short[]  iftable;

    /**
     * Mapping of logical static field offsets to IsolateState index. The IsolateState
     * slot index is in terms of the size of the data type of the given field.
     */
    final short[]  sftable;

    /**
     * Array of constant objects for this class
     */
    final Object[]  constTable;

    /**
     * The oop map for the class.
     */
    final byte[] oopMap;

    /**
     * Optional debug info. If not null, the format is:
     *
     * struct {
     *    half sourceFileLength;
     *    byte sourceFile[sourceFileLength]; // Name of class's source file
     * }
     */
    final byte[]    debugInfo;

    /**
     * Construct a ClassBase to represent a class parsed by the SquawkClassLoader.
     * @param clazz The Class object to initialize.
     * @param id The class number as given in the Squawk class file.
     * @param extnds The class number of the superclass. This will be -1 for the
     * class of java.lang.Object.
     * @param arrayOf The base class for an array class or -1 if this is not an
     * array class.
     * @param name The name of the class.
     * @param impls A list of class numbers corresponding to the interfaces
     * implemented by this class.
     * @param constants The constant pool.
     * @param svars The static variables of the class or null if there are none.
     * @param ivars The instance variables of the class or null if there are none.
     * @param i_map The map of interface methods to method definitions.
     * @param usesFvtable Indicates if there is any method defined by this class
     * that will be placed in the fixed vtable.
     * @param vtableStart The slot number of the first method defined by this class
     * that will be placed in the normal vtable or 0 if there is no such
     * method.
     * @param vtableEnd The slot number of the last method defined by this class
     * that will be placed in the normal vtable or 0 if there is no such
     * method.
     * @param firstNVMethod The number of the first non-virtual method defined
     * by this class or 0 if there is no such method.
     */
    ClassBase(int id, int extnds, int arrayOf,
        String name, Vector impls, Vector constants, Vector svars, Vector ivars, Vector i_map,
        int accFlags, boolean usesFvtable, int vtableStart, int vtableEnd, int firstNVMethod,
        byte[] debugInfo)
    {
        Native.assume(name != null);

        boolean isJavaLangObject = name.equals("java.lang.Object");

        className = name;
        self = this;
        classIndex = id;
        accessFlags = accFlags;
        this.debugInfo = debugInfo;

        // Resolve base type for an array class.
        if (arrayOf != -1) {
            // Arrays of primitive types are known by the garbage collector so
            // only oop arrays need the correct GC type.
            gcType = NativeOpcodes.GCTYPE_oopArray;
            elementType = forNumber(arrayOf);
        }
        else {
            gcType = NativeOpcodes.GCTYPE_object;
            elementType = null;
        }

        // Resolve super class
        if (!isJavaLangObject) {
            superClass = forNumber(extnds);
        }
        else {
            Native.assume(extnds == -1);
            superClass = null;
        }

        // Resolve implemented interfaces
        if (impls != null) {
            interfaces = new ClassBase[impls.size()];
            int i = 0;
            for (Enumeration e = impls.elements(); e.hasMoreElements(); ) {
                int classNumber = ((Integer)e.nextElement()).intValue();
                interfaces[i++] = forNumber(classNumber);
            }
        }
        else {
            interfaces = null;
        }

        // create constTable
        if (constants != null) {
            constTable = new Object[constants.size()/2];
            for (int i = 0; i != constTable.length; i++) {
                // Only String constants are currently generated by the frontend and
                // handled by the backend.
                Native.assume(((Integer)constants.elementAt(i*2)).intValue() == SquawkTags.T_STRING);
                constTable[i] = (String)constants.elementAt((i*2)+1);
            }
        }
        else {
            constTable = null;
        }

        // Allocate static variables in isolate state array
        if (svars != null) {
            sftable = new short[svars.size() / 2];
            int i = 0;
            for (Enumeration e = svars.elements(); e.hasMoreElements();) {
                int type = ((Integer)e.nextElement()).intValue();
                Object initValue = e.nextElement();
                int value = 0;
                int address = 0;
                switch (type) {
                    case SquawkTags.T_BYTE:
                        if (initValue != null) {
                            value = ((Byte)initValue).byteValue();
                        }
                        address = VMPlatform.addPrimitiveToIsolateState(value) * 4;
                        break;
                    case SquawkTags.T_HALF:
                        if (initValue != null) {
                            value = ((Short)initValue).shortValue();
                        }
                        address = VMPlatform.addPrimitiveToIsolateState(value) * 2;
                        break;
                    case SquawkTags.T_WORD:
                        if (initValue != null) {
                            value = ((Integer)initValue).intValue();
                        }
                        address = VMPlatform.addPrimitiveToIsolateState(value);
                        break;
                    case SquawkTags.T_REF:
                        Native.assume(initValue == null);
                        address = VMPlatform.addReferenceToIsolateState(null);
                        break;
                    case SquawkTags.T_DWORD: {
                        long longValue = 0;
                        if (initValue != null) {
                            longValue = ((Long)initValue).longValue();
                        }
                        // add high word first ...
                        address = VMPlatform.addPrimitiveToIsolateState((int)(longValue >>> 32));
                        // ... followed by low word
                        VMPlatform.addPrimitiveToIsolateState((int)(longValue & 0xFFFFFFFF));
                        break;
                    }
                    case SquawkTags.T_STRING:
                        address = VMPlatform.addReferenceToIsolateState(initValue);
                        // Ensure static field can be addressed by 16 bits. If not, sftable
                        // will have to be declared as int[] instead.
                        Native.assume(address != 0 && (address >>> 16) == 0);
                        sftable[i++] = (short)address;
                        // Skip the statement adding this variable as a primitive
                        // to the isolate state array.
                        continue;
                    default:
                        Native.assume(false);
                }
                // Ensure static field can be addressed by 16 bits. If not, sftable
                // will have to be declared as int[] instead.
                Native.assume(address != 0 && (address >>> 16) == 0);
                sftable[i++] = (short)address;
            }
            VMPlatform.mapIsolateStateFields(this);
        }
        else {
            sftable = null;
        }

        // Build oop map and iftable for instance variables
        byte[] oopMap;
        int s_iftableLen = (isJavaLangObject ? 0 : superClass.iftable.length);
        int[] sorted;
        int sizeInBytes = 0; // Total size of instance fields in bytes
        if (ivars != null) {
            // Sort the instance variables into descending order, big to small.
            // It is very important that the relative order of word size
            // fields (i.e. T_WORD and T_REF) is not disturbed otherwise
            // hard coded variable offsets for certain objects will not be
            // correct. E.g. the "count" field of java.lang.String must
            // be at the offset defined by STR_count.
            sorted = new int[ivars.size()];
            int nextVar = 0;
            for (int typeSize = 8; typeSize != 0; typeSize >>= 1) {
                int logicalOffset = 0;
                for (Enumeration e = ivars.elements(); e.hasMoreElements();) {
                    int type = ((Integer)e.nextElement()).intValue();
                    e.nextElement(); // ignore value - should be null
                    if (AbstractAssembler.TYPE_SIZE[type - SquawkTags.T_DWORD] == typeSize) {
                        sorted[nextVar++] = type;
                        sorted[nextVar++] = logicalOffset;
                        sizeInBytes += typeSize;
                    }
                    logicalOffset++;
                }
            }
        }
        else {
            sorted = new int[0];
        }
        if (sorted.length != 0 || (s_iftableLen != 0)) {
            iftable = new short[s_iftableLen + (sorted.length / 2)];
            System.arraycopy(superClass.iftable,0,iftable,0,s_iftableLen);
//System.out.println(name+": sizeInBytes="+sizeInBytes+", super="+(superClass == null ? "null" : superClass.className));
            int sizeInWords = (sizeInBytes + 3) / 4;
            if (isJavaLangObject) {
                // Oop map is just for the fields of java.lang.Object,
                // no need to consult superclass.
                oopMap = new byte[(sizeInWords + 7) / 8];
            }
            else {
                // Need to include superclass's oop map and length.
                sizeInWords += superClass.length;
                oopMap = new byte[(sizeInWords + 7) / 8];
                byte[] s_oopMap = superClass.oopMap;
                System.arraycopy(s_oopMap,0,oopMap,0,s_oopMap.length);
            }
            length = sizeInWords;
            short byteOffset = (short)(superClass.length * 4);
            for (int i = 0; i != sorted.length; i += 2) {
                int type = sorted[i];
                short wordOffset = (short)(byteOffset / 4);
                short halfOffset = (short)(byteOffset / 2);
                int logicalOffset = sorted[i + 1] + s_iftableLen;
                switch (type){
                    case SquawkTags.T_DWORD:
                        iftable[logicalOffset] = wordOffset;
                        byteOffset += 8;
                        break;
                    case SquawkTags.T_REF:
                        oopMap[wordOffset / 8] |= (1 << (wordOffset % 8));
                    case SquawkTags.T_WORD:
                        iftable[logicalOffset] = wordOffset;
                        byteOffset += 4;
                        break;
                    case SquawkTags.T_HALF:
                        iftable[logicalOffset] = halfOffset;
                        byteOffset += 2;
                        break;
                    case SquawkTags.T_BYTE:
                        iftable[logicalOffset] = byteOffset;
                        byteOffset += 1;
                        break;
                    default:
                        Native.assume(false);
                }
            }
//System.out.println(name+": length="+length+", iftableLen="+iftable.length+", sizeInBytes="+sizeInBytes+", byteOffset="+byteOffset+", super="+(superClass == null ? "null" : superClass.className));
            Native.assume(length == ((byteOffset - 1) / 4) + 1);
        }
        else {
            iftable = new short[0];
            length = 0;
            oopMap = new byte[0];
        }
        this.oopMap = oopMap;

        // Setup vftable.
        if (usesFvtable) {
            // This class need its own copy of the fixed table.
            // Interface classes can only define 1 method - <clinit>
            if (!isInterface()) {
                fvtable = new byte[NativeOpcodes.SLOT_FVTABLE_LENGTH][];
            }
            else {
                fvtable = new byte[NativeOpcodes.SLOT_clinit + 1][];
            }
        }
        else {
            // java.lang.Object must have methods in the fvtable!
            Native.assume(!isJavaLangObject);
            // Inherit superclass's fvtable
            fvtable = superClass.fvtable;
        }

        // Setup vtable, vstart and vcount.
        if (isJavaLangObject) {
            if (vtableStart == 0) {
                vtable = new byte[0][];
                vstart = NativeOpcodes.SLOT_FVTABLE_LENGTH;
                vcount = 0;
            }
            else {
                if (firstNVMethod == 0) {
                    vcount = (vtableEnd - vtableStart) + 1;
                }
                else {
                    vcount = firstNVMethod - vtableStart;
                }
                vtable = new byte[vcount][];
                vstart = vtableStart;
             }
        }
        else {
            if (vtableStart != 0) {
                vstart = vtableStart;
                int s_firstNVMethod = superClass.vstart + superClass.vcount;
                if (s_firstNVMethod > vtableEnd) {
                    vtableEnd = s_firstNVMethod - 1;
                }
                int size = (vtableEnd - vtableStart) + 1;
                if (firstNVMethod == 0) {
                    vcount = size;
                }
                else {
                    // The superclass's first non-virtual method cannot be
                    // higher than this class's first non-virtual method
                    Native.assume(firstNVMethod >= s_firstNVMethod);
                    vcount = firstNVMethod - vtableStart;
                }
                vtable = new byte[size][];
            }
            else {
                // This class does not define any methods that go into the vtable so
                // inherit from superclass
                vtable = superClass.vtable;
                vstart = superClass.vstart;
                vcount = superClass.vcount;
            }
        }

        // Resolve interface map
        if (i_map != null) {
            itable = new short[i_map.size() / 2];
            int ifaceStart = -1;
            for (Enumeration e = i_map.elements(); e.hasMoreElements();) {
                int from = ((Integer)e.nextElement()).intValue();
                int to   = ((Integer)e.nextElement()).intValue();
                if (ifaceStart == -1) {
                    ifaceStart = from;
                }
                else {
                    Native.assume(from > ifaceStart);
                }
                itable[from - ifaceStart] = (short)to;
            }
            istart = ifaceStart;
        }
        else {
            itable = null;
            istart = 0;
        }

        Native.assume(classTable.length > classIndex);
        Native.assume(classTable[classIndex] == null);
        classTable[classIndex] = this;
        VMPlatform.classLinked(this);
    }


    /**
     * Returns the runtime class of an object. That <tt>Class</tt>
     * object is the object that is locked by <tt>static synchronized</tt>
     * methods of the represented class.
     *
     * @return  the object of type <code>Class</code> that represents the
     *          runtime class of the object.
     */
//    Class getClassPrim() {
//        return Native.asClass(metaClass);
//    }


    /**
     * Determines if the specified <code>Class</code> object represents an
     * interface type.
     *
     * @return  <code>true</code> if this object represents an interface;
     *          <code>false</code> otherwise.
     */
    public boolean isInterface() {
        return (accessFlags & NativeOpcodes.ACC_INTERFACE) != 0;
    }

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     * @since   JDK1.1
     */
    public boolean isArray() {
        return gcType >= GCTYPE_array;
    }

    /**
     * isPrimitive
     */
    boolean isPrimitive() {
        return gcType == GCTYPE_spiritual;
    }

    /**
     * hasClinit()
     */
    boolean hasClinit() {
        return (accessFlags & NativeOpcodes.ACC_HASCLINIT) != 0;
    }

    /**
     * Check to see if an instance of this class can be cast to the
     * class represented by toClass.
     */
    void checkCast(int toClass) throws ClassCastException {
        ClassBase cls = forNumber(toClass);
        Native.assume(cls != null);
        if (!this.isAssignableTo(cls)) {
            throw new ClassCastException();
        }
    }

    /**
     * Check to see if an instance of this class can be cast to the
     * class represented by cls
     */
    void checkStore(ClassBase cls) throws ArrayStoreException {
        if (!this.isAssignableTo(cls)) {
            throw new ArrayStoreException();
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                               isAssignableTo                             *
   \* ------------------------------------------------------------------------ */

    /**
     * Work out if this type can be assigned to another type.
     */
    boolean isAssignableTo(ClassBase aClass) {

       /*
        * Quickly check for equalty, the most common case.
        */
        if (this == aClass) {
           return true;
        }

       /*
        * Check to see of this class is somewhere in aType's hierarchy
        */
        for (ClassBase thisClass = this ; thisClass != null ; thisClass = thisClass.superClass) {
            if (thisClass == aClass) {
                return true;
            }
        }

       /*
        * If aType is an interface see if this class implements it
        */
        if (aClass.isInterface()) {
            for (ClassBase thisClass = this ; thisClass != null ; thisClass = thisClass.superClass) {
               /*
                * The interface list in each class is a transitive closure of all the
                * interface types specified in the class file. Therefore it is only
                * necessary to check this list and not the interfaces implemented by
                * the interfaces.
                */
                for (int i = 0 ; i < thisClass.interfaces.length ; i++) {
                    if (thisClass.interfaces[i] == aClass) {
                        return true;
                    }
                }
                thisClass = thisClass.superClass;
            }
        }

       /*
        * This is needed to cast arrays of classes into arrays of interfaces
        */
        if (this.isArray() && aClass.isArray()) {
            return (this.elementType).isAssignableTo(aClass.elementType);
        }

       /*
        * Otherwise there is no match
        */
        return false;
    }

    /**
     * Get the source file name for this class.
     * @return the source file name for the class or null if it is not available (i.e. the class has no debug info).
     */
    String getSourceFileName() {
        if (debugInfo != null) {
            int length = Native.getUnsignedHalf(debugInfo,0);
            return new String(Native.getEmbeddedASCIIArray(debugInfo,2,length));
        }
        return null;
    }
}
