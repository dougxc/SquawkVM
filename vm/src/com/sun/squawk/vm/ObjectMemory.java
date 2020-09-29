//J2C:object.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;
/*IFJ*/abstract public class ObjectMemory extends Memory {

/*---------------------------------------------------------------------------*\
 *                            Forward References                             *
\*---------------------------------------------------------------------------*/

//IFC//#ifndef PRODUCTION

    abstract boolean getTraceAllocation();
    abstract boolean getTraceGC();
    abstract boolean getTraceGCVerbose();

    abstract void    setTraceAllocation(boolean b);
    abstract void    setTraceGC(boolean b);
    abstract void    setTraceGCVerbose(boolean b);

//IFC//#else
//IFC//#define getTraceAllocation() false
//IFC//#define getTraceGC()         false
//IFC//#define getTraceGCVerbose    false
//IFC//#define setTraceAllocation(b)
//IFC//#define setTraceGC(b)
//IFC//#define setTraceGCVerbose(b)
//IFC//#endif

/*-----------------------------------------------------------------------*\
 *                              Constants                                *
\*-----------------------------------------------------------------------*/

/*IFJ*/public final static int FORWARDBIT = 0x10000000;
//IFC//#define FORWARDBIT 0x10000000


/*-----------------------------------------------------------------------*\
 *                           Tracing                                     *
\*-----------------------------------------------------------------------*/

    void trace_String(int s) {
        int i;
        int chars  = getWord(s,STR_value);
        int offset = getWord(s,STR_offset);
        int count  = getWord(s,STR_count);
        for (i = 0; i != count; i++) {
            traceChar((char)getHalf(chars, i+offset));
        }
    }

    void traceMethodName(int mth, boolean args) {
//IFC// int getClassFromMethod(int);
//IFC// int getArrayLength(int);
        int cls = getClassFromMethod(mth);
        int mthLength = getArrayLength(mth);

        /* Test whether the method has any debug info */
        if (getUnsignedByte(mth, mthLength - 1) == 1) {
            int debugInfoLength = getUnsignedHalfFromByteArray(mth,mthLength - 3);

            /* Lengths */
            int offset                = (mthLength - (1 + debugInfoLength));
            int nameLength            = getUnsignedHalfFromByteArray(mth,offset);
            /* Skip lineNumberTableLength */
            offset += 4;

            /* Print class name */
            trace_String(getWord(cls,CLS_className));
            traceMsg(".");

            /* Print the method name */
            while (nameLength != 0) {
                char ch = (char)getByte(mth,offset++);
                if (!args && ch == '(') {
                    break;
                }
                traceChar(ch);
                nameLength--;
            }
        }
    }

/*-----------------------------------------------------------------------*\
 *                               Variables                               *
\*-----------------------------------------------------------------------*/


    /**
     * Get the address of the start of the heap
     */
    private int  getHeapStart()                         { return HEAP_heapStart;                                }

    /**
     * Get/set the size of the heap
     */
    private int  getHeapSize()                          { return getWord(0, HEAP_heapSize);                     }
    private void setHeapSize(int size)                  {        setWord(0, HEAP_heapSize, size);               }

    /**
     * Start of the active semi space
     */
    private int  getCurrentSpace()                      { return getWord(0, HEAP_currentSpace);                 }
    private void setCurrentSpace(int addr)              {        setWord(0, HEAP_currentSpace, addr);           }

    /**
     * Allocation point in the active semi space
     */
    private int  getCurrentSpaceFreePtr()               { return getWord(0, HEAP_currentSpaceFreePtr);          }
    private void setCurrentSpaceFreePtr(int addr)       {        setWord(0, HEAP_currentSpaceFreePtr, addr);    }

    /**
     * End of the active semi space
     */
    private int  getCurrentSpaceEnd()                   { return getWord(0, HEAP_currentSpaceEnd);              }
    private void setCurrentSpaceEnd(int addr)           {        setWord(0, HEAP_currentSpaceEnd, addr);        }


    /**
     * Size of the last allocation that failed or zero if the last allocation
     * succeeded.
     */
    private int  getFailedAllocationSize()              { return getWord(0, HEAP_failedAllocationSize);         }
    private void setFailedAllocationSize(int size)      {        setWord(0, HEAP_failedAllocationSize, size);   }

    /* Various things not related to GC, but which it is
       convenient to keep here so they can be relocated */

    /**
     * A block of memory used to store all the state pertaining to a
     * single isolate. This will include the static variables for
     * all classes (and not much else right now...).
     */
    protected int  getIsolateState()                    { return getWord(0, HEAP_isolateState);                 }
    protected void setIsolateState(int value)           {        setWord(0, HEAP_isolateState, value);          }

    /**
     * This is the meta data for the IsolateState array that is used by
     * the garbage collector to determine which words in the array
     * are object pointers.
     */
    private int  getIsolateStateOopMap()                { return getWord(getIsolateState(), ISO_isolateStateOopMap);           }
    private void setIsolateStateOopMap(int value)       {        setWord(getIsolateState(), ISO_isolateStateOopMap, value);    }

    /**
     * This is the mapping of class IDs to the prototype objects for the class.
     * See the documentation for java.lang.ClassBase.classTable for a more in depth
     * description.
     */
    protected int  getIsolateClassTable()               { return getWord(getIsolateState(), ISO_classTable);            }
    protected void setIsolateClassTable(int value)      {        setWord(getIsolateState(), ISO_classTable, value);     }

    /**
     * The current activation record. The variable is used as mechanism for
     * communication between the interpreter loop and any support routines
     * it calls. While in the loop, the current activation record is stored
     * in a local variable. Just before calling a support routine, the value of
     * the local variable is written into GlobalAR.
     */
    protected int  getActivationRecord()                { return getWord(0, HEAP_activationRecord);             }
    protected void setActivationRecord(int value)       {        setWord(0, HEAP_activationRecord, value);      }

    /**
     * An activation record allocated at VM startup time that will be used when
     * an OutOfMemoryError needs to be thrown.
     */
    protected int  getEmergencyActivation()              { return getWord(0, HEAP_emergencyActivation);          }
    protected void setEmergencyActivation(int value)     {        setWord(0, HEAP_emergencyActivation, value);   }

    /**
     * A convenience handle to the method 'java.lang.Native.primitive'.
     */
    protected int  getPrimitiveMethod()                 { return getWord(0, HEAP_primitiveMethod);              }
    private   void setPrimitiveMethod(int value)        {        setWord(0, HEAP_primitiveMethod, value);       }

    /**
     * Pointers to the class and string monitor proxy objects
     */
    protected int  getClassMonitorProxy()               { return getWord(0, HEAP_classMonitorProxy);            }
    protected void setClassMonitorProxy(int value)      {        setWord(0, HEAP_classMonitorProxy, value);     }
    protected int  getStringMonitorProxy()              { return getWord(0, HEAP_stringMonitorProxy);           }
    protected void setStringMonitorProxy(int value)     {        setWord(0, HEAP_stringMonitorProxy, value);    }

    /**
     * Return the activation record size for the primitive method
     */
    protected int getPrimitiveMethodARSize() {
        int mth = getPrimitiveMethod();
        return (getUnsignedByte(mth, MTH_arSizeHigh) << 8) + getUnsignedByte(mth, MTH_arSizeLow);
    }


/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

    protected void ObjectMemory_init() {

        int start, size, half;

        setHeapSize(getMemorySize() - getHeapStart());

        start = getHeapStart() * ADDRESS_UNITS;
        size  = getHeapSize()  * ADDRESS_UNITS;
        half  = size / 2;

        setCurrentSpace(        start       );
        setCurrentSpaceFreePtr( start       );
        setCurrentSpaceEnd(     start + half);
    }

    /*
     * Re-initializing from an image
     */
    protected void ObjectMemory_reinit() {

        int start, size, half;

        assume(getHeapSize() <= (getMemorySize() - getHeapStart()));
        setHeapSize(getMemorySize() - getHeapStart());

        start = getHeapStart() * ADDRESS_UNITS;
        size  = getHeapSize()  * ADDRESS_UNITS;
        half  = size / 2;

        assume(getCurrentSpace() == getHeapStart());
        setCurrentSpaceEnd(start + half);
        assume(getCurrentSpaceFreePtr() <= getCurrentSpaceEnd());
    }


/*---------------------------------------------------------------------------*\
 *                          Basic object header access                       *
\*---------------------------------------------------------------------------*/

    /**
     * getHeader
     */
    int getHeader(int obj) {
        int res;
        assume(obj != 0);
        res = getWord(obj, OBJ_header);
        assume(res != 0);
        return res;
    }

    /**
     * setHeader
     */
    void setHeader(int obj, int val) {
        assume(obj != 0);
        assume(val != 0);
        setWord(obj, OBJ_header, val);
    }

    /**
     * getClass
     */
    int getClass(int obj) {
        int res;
        assume(obj != 0);
        res = getWord(getHeader(obj), CLS_self);
        assume(res != 0);
        return res;
    }

    /**
     * getHeaderLength
     */
    int getHeaderLength(int obj) {
        assume(obj != 0);
        if (HEAP_ADDRESSES_ARE_POSITIVE) {
            return 0 - getWord(obj, OBJ_length);
        } else {
            return getWord(obj, OBJ_length);
        }
    }

    /**
     * setHeaderLength
     */
    void setHeaderLength(int obj, int val) {
        assume(obj != 0);
        if (HEAP_ADDRESSES_ARE_POSITIVE) {
            setWord(obj, OBJ_length, 0 - val);
        } else {
            setWord(obj, OBJ_length, val);
        }
    }


/*-----------------------------------------------------------------------*\
 *                             Memory access                             *
\*-----------------------------------------------------------------------*/

    /**
     * setOop
     */
    void setOop(int addr, int off, int value) {
        setWord(addr, off, value);
    }

    /**
     * getArrayElementSize
     */
    int getArrayElementSize(int cls) {
        int typ = getWord(cls, CLS_gctype);
        switch (typ) {
            case GCTYPE_longArray: return 8;
            case GCTYPE_arArray:
            case GCTYPE_gvArray:
            case GCTYPE_oopArray:
            case GCTYPE_wordArray: return 4;
            case GCTYPE_halfArray: return 2;
            case GCTYPE_byteArray: return 1;
            default: shouldNotReachHere();
        }
        return 0;
    }

    /**
     * arrayCopy
     */
    void arrayCopy(int parms[]) {
        int i;
        int src    = parms[1];
        int srcPos = parms[2];
        int dst    = parms[3];
        int dstPos = parms[4];
        int length = parms[5];

        int itemLength = getArrayElementSize(getClass(src));
        assume(getArrayElementSize(getClass(dst)) == itemLength);

        switch(itemLength) {
            case 1: for (i = 0 ; i < length ; i++) {
                        setByte(dst, dstPos + i, getByte(src, srcPos + i));
                    }
                    break;

            case 2: for (i = 0 ; i < length ; i++) {
                        setHalf(dst, dstPos + i, getHalf(src, srcPos + i));
                    }
                    break;

            case 4: for (i = 0 ; i < length ; i++) {
                        setWord(dst, dstPos + i, getWord(src, srcPos + i));
                    }
                    break;

            case 8: for (i = 0 ; i < length ; i++) {
                        setLong(dst, dstPos + i, getLong(src, srcPos + i));
                    }
                    break;

            default: shouldNotReachHere();
        }

    }



/*---------------------------------------------------------------------------*\
 *                          Islocate/Class table access                      *
\*---------------------------------------------------------------------------*/

    /**
     * These are the accessor methods for entries in the IsolateClassTable that do
     * bounds checking.
     */
    protected int getIsolateClassTableAt(int index) {
        int table = getIsolateClassTable();
        int length = getHeaderLength(table);
        if (length <= index || index < 0) {
            fatalVMError1("Bad IsolateClassTable length=", length);
        }
        return getWord(table, index);
    }

    /**
     * setIsolateClassTableAt
     */
    protected void setIsolateClassTableAt(int index, int value) {
        int table = getIsolateClassTable();
        int length = getHeaderLength(table);
        if (length <= index || index < 0) {
            traceMsg("Bad IsolateClassTable ");
            traceInt(length);
            traceMsg(" index=");
            traceInt(index);
            fatalVMError(" Exiting");
        }
        setWord(table, index, value);
    }

    /**
     * getClassFromCNO
     */
    int getClassFromCNO(int cno) {
        int cls;
        if (cno == 0) {
            return 0;
        }
        cls = getIsolateClassTableAt(cno);
        assume(cls != 0 && getWord(cls,CLS_classIndex) == cno);
        return cls;
    }

/*---------------------------------------------------------------------------*\
 *                       Method lookup & analysis                            *
\*---------------------------------------------------------------------------*/

    /*
     * lookupMethod
     * @param ar The activation record of the current method.
     */
    int lookupMethod(int cls, int slot) {
        int mth;
        assume(slot > 0);

        if (slot < SLOT_FVTABLE_LENGTH) {
            int ftable = getWord(cls, CLS_fvtable);
            mth = getWord(ftable, slot);
        } else {
            int vtable;
            int vstart = getWord(cls, CLS_vstart);
            while (slot < vstart) {
                cls    = getWord(cls, CLS_superClass);
                vstart = getWord(cls, CLS_vstart);
            }
            vtable  = getWord(cls, CLS_vtable);
            mth     = getWord(vtable, slot-vstart);
        }
        /*
         * The following assertion will typically be due to the invocation of a method  (such as getClass)
         * on the prototype object for a class that hasn't had its methods loaded yet.
         */
        assume(mth != 0);

        return mth;
    }

    /*
     * getClassFromMethod
     */
    int getClassFromMethod(int mth) {
        int cno1  = getUnsignedByte(mth, MTH_classNumberHigh);
        int cno2  = getUnsignedByte(mth, MTH_classNumberLow);
        int cno   = (cno1<<8)+cno2;
        int cls   = getClassFromCNO(cno);
        assume(getWord(cls, CLS_classIndex) == cno);
        return cls;
    }

/*---------------------------------------------------------------------------*\
 *                               Object analysis                             *
\*---------------------------------------------------------------------------*/

    /**
     * isPrototypeObject
     */
//    boolean isPrototypeObject(int oop) {
//        int cls = getClass(oop);
//        int map = getWord(cls, CLS_oopMap);
//        return map == oop;
//    }

    /**
     * isArrayClass
     */
    boolean isArrayClass(int cls) {
        int type = getWord(cls, CLS_gctype);
        return type >= GCTYPE_array;
    }

    /**
     * isArray
     */
    boolean isArray(int oop) {
        int cls = getClass(oop);
        return isArrayClass(cls);
    }

    /**
     * getArrayLength
     */
    int getArrayLength(int obj) {
        assume(isArray(obj));
        return getHeaderLength(obj);
    }

    /**
     * getObjectLength (in addressing units)
     */
    int getObjectLength(int oop) {
        int lth;
        int cls = getClass(oop);
        if (!isArray(oop)) {
            lth = getWord(cls, CLS_length);
        } else {
            int type = getWord(cls, CLS_gctype);
            lth = getHeaderLength(oop);
            switch (type) {
                case GCTYPE_longArray: lth *= 8; break;
                case GCTYPE_arArray:
                case GCTYPE_gvArray:
                case GCTYPE_oopArray:
                case GCTYPE_wordArray: lth *= 4; break;
                case GCTYPE_halfArray: lth *= 2; break;
                case GCTYPE_byteArray:
                case GCTYPE_object:              break; // (when it is a prototype object)
            }
            lth = (lth+3)/4; // Round up to a full word boundry
        }
        return lth * ADDRESS_UNITS;
    }


/*-----------------------------------------------------------------------*\
 *                           Memory allocation                           *
\*-----------------------------------------------------------------------*/

    /**
     * newChunk - get raw memory chunk and zero all but the header
     *
     * Both sizes are in words
     */
    int newChunk(int size, int headerSize) {
        int i;
        int obj;
        int newEnd;
        size        *= ADDRESS_UNITS;
        headerSize  *= ADDRESS_UNITS;
        newEnd = getCurrentSpaceFreePtr() + size + headerSize;

        if (newEnd >= getCurrentSpaceEnd()) {
            setFailedAllocationSize(size + headerSize);
            return 0;
        }
        setFailedAllocationSize(0);
        obj = getCurrentSpaceFreePtr() + headerSize;
        for (i = obj ; i < newEnd ; i += ADDRESS_UNITS) {
            setWord(i, 0, 0);
        }
/*IFJ*/ assume((obj & FORWARDBIT) == 0);
        setCurrentSpaceFreePtr(newEnd);
        incAllocationCount();
        assume(getCurrentSpaceFreePtr() < getCurrentSpaceEnd());
        return obj;
    }

    /**
     * newInst
     */
    int newInstance(int cls) {
        int wordLth = getWord(cls, CLS_length);
        int obj = newChunk(wordLth, 1);
        if (obj != 0) {
            setHeader(obj, cls);
        }
        return obj;
    }

    /**
     * newArray
     *
     * Length is in elements
     */
    int newArray(int cls, int lth) {
        int obj;
        int wordLth = lth;
        int typ = getWord(cls, CLS_gctype);
        switch (typ) {
            case GCTYPE_longArray: wordLth *= 8; break;
            case GCTYPE_arArray:
            case GCTYPE_gvArray:
            case GCTYPE_oopArray:
            case GCTYPE_wordArray: wordLth *= 4; break;
            case GCTYPE_halfArray: wordLth *= 2; break;
        }
        wordLth = (wordLth+3)/4; // Round up to a full word boundry
        obj = newChunk(wordLth, 2);
        if (obj != 0) {
            setHeader(obj, cls);
            setHeaderLength(obj, lth);
        }
        return obj;
    }

    /**
     * newObject
     */
    int newObject(int cno, int lth) {
        int cls = getClassFromCNO(cno);
        int res;
        if (isArrayClass(cls)) {
            res = newArray(cls, lth);
            if (getTraceAllocation()) {
                traceMsg("newArray(");
                traceInt(cno);
                traceMsg(",");
                traceInt(lth);
                traceMsg(") -> ");
                traceHex(res);
                traceMsg("\n");
            }
        } else {
            assume(lth == 0);
            res = newInstance(cls);
            if (res != 0) {
               /*
                * Special case for classes and strings.
                * These always point to proxy monitors so
                * that they can be shared between isolates.
                */

                if (cno == CNO_String) {
                    setHeader(res, getStringMonitorProxy());
                }
                if (cno == CNO_Class) {
                    setHeader(res, getClassMonitorProxy());
                }
            }
        }
        if (getTraceAllocation()) {
            traceMsg("newObject(");
            traceInt(cno);
            traceMsg(",");
            traceInt(lth);
            traceMsg(") -> ");
            traceHex(res);
            traceMsg("\n");
        }
        return res;
    }

    /**
     * newActivation
     */
    int newActivation(int size) {
        int oop = newObject(CNO_localArray, size);
        if (oop != 0) {
            incActivationCount();
        }
        return oop;
    }

    /**
     * newNativeActivation
     */
    int newNativeActivation() {
        int ar  = newActivation(getPrimitiveMethodARSize());
        int mth = getPrimitiveMethod();
        assume(mth != 0);
        setWord(ar, AR_method, mth);
        return ar;
    }

    /**
     * freeActivation
     */
    void freeActivation(int ar) {
    }

    /**
     * freeMem
     */
    private int freeMemInWords() {
        return (getCurrentSpaceEnd() - getCurrentSpaceFreePtr()) / ADDRESS_UNITS;
    }

    /**
     * freeMem
     */
    int freeMem() {
        return freeMemInWords() * 4;
    }

    /**
     * totalMem
     */
    int totalMem() {
        return ((getMemorySize()/2) / ADDRESS_UNITS) * 4;
    }



/*************************************************************************\
 *      The following can be removed if the GC becomes a Java thread     *
\*************************************************************************/

    private int targetSpace;
    private int targetSpaceFreePtr;
    private int targetSpaceEnd;


/*---------------------------------------------------------------------------*\
 *                             Pointer manipulation                          *
\*---------------------------------------------------------------------------*/

    /**
     * isEncodedLength
     */
    boolean isEncodedLength(int bits) {
        if (HEAP_ADDRESSES_ARE_POSITIVE) {
            return bits <= 0;
        } else {
            return bits >= 0;
        }
    }

    /**
     * nextChunk
     */
    int nextChunk(int oop) {
        return oop + getObjectLength(oop);
    }

    /**
     * chunkToOop
     */
    int chunkToOop(int addr) {
        int word = getWord(addr, 0);
        if (isEncodedLength(word)) {
            addr += ADDRESS_UNITS;
        }
        return addr + ADDRESS_UNITS;
    }

    /**
     * oopToChunk
     */
    int oopToChunk(int oop) {
        if (isArray(oop)) {
            oop -= ADDRESS_UNITS;
        }
        return oop - ADDRESS_UNITS;
    }


/*---------------------------------------------------------------------------*\
 *                              Garbage collection                           *
\*---------------------------------------------------------------------------*/

    /**
     * inCurrentSpace
     */
    boolean inCurrentSpace(int oop) {
        assume(oop != getCurrentSpace()); // because of the header
        return oop > getCurrentSpace() && oop < getCurrentSpaceEnd();
    }


    /**
     * inTargetSpace
     */
    boolean inTargetSpace(int oop) {
        assume(oop != targetSpace); // because of the header
        return oop > targetSpace && oop < targetSpaceEnd;
    }

    /**
     * objectAfter
     */
    int objectAfter(int oop) {
        int chunk = nextChunk(oop);
        assume(chunk >= targetSpace && chunk < targetSpaceEnd);
        if (chunk == targetSpaceFreePtr) {
            return 0; // end
        } else {
            return chunkToOop(chunk);
        }
    }

    /**
     * copyObject into target space
     */
    int copyObject(int oop) {
        if (oop == 0) {
            if (getTraceGCVerbose()) {
                traceMsg("copyObject ");
                traceHex(oop);
                traceMsg("\n");
            }
            return 0;
        }
        if (!inCurrentSpace(oop)) {
            if (getTraceGCVerbose()) {
                traceMsg("copyObject in target space " );
                traceHex(oop);
                traceMsg("\n");
            }
            assume(inTargetSpace(oop));
            return oop;
        } else {
            int hdr = getHeader(oop);
            if ((hdr & FORWARDBIT) != 0) {
                if (getTraceGCVerbose()) {
                    traceMsg("copyObject already forwarded from " );
                    traceHex(oop);
                    traceMsg(" -> ");
                    traceHex(hdr & ~FORWARDBIT);
                    traceMsg("\n");
                }
                return hdr & ~FORWARDBIT;
            } else {
                int chunk      = oopToChunk(oop);
                int headerSize = oop - chunk;
                int chunkSize  = headerSize + getObjectLength(oop);
                int target     = targetSpaceFreePtr;
                int targetOop  = target + headerSize;
                if (getTraceGCVerbose()) {
                    traceMsg("copyObject about to copy " );
                    traceHex(oop);
                    traceMsg("\n");
                }
                copyWords(chunk, target, chunkSize/ADDRESS_UNITS);
                targetSpaceFreePtr += chunkSize/ADDRESS_UNITS;
                setHeader(oop, targetOop | FORWARDBIT);
                if (getTraceGCVerbose()) {
                    traceMsg("copyObject ");
                    traceHex(oop);
                    traceMsg(" -> ");
                    traceHex(targetOop);
                    traceMsg(" size ");
                    traceInt(chunkSize/ADDRESS_UNITS);
                    traceMsg("\n");
                }
                return targetOop;
            }
        }
    }

    /**
     * updateOop copying its object into target space
     */
    void updateOop(int oopAddress, int oopOffset) {
        int oldobj;
        int newobj;
        if (getTraceGCVerbose()) {
            traceMsg("updateOop ");
            traceHex(oopAddress);
            traceMsg(" % ");
            traceHex(oopOffset);
            traceMsg("\n");
        }
        oldobj = getWord(oopAddress, oopOffset);
        newobj = copyObject(oldobj);
        setWord(oopAddress, oopOffset, newobj);
    }

    /**
     * copyRoots
     */
    void copyRoots() {
        setIsolateState(         copyObject(getIsolateState())          );
        setPrimitiveMethod(      copyObject(getPrimitiveMethod())       );
        setActivationRecord(     copyObject(getActivationRecord())      );
        setClassMonitorProxy(    copyObject(getClassMonitorProxy())     );
        setStringMonitorProxy(   copyObject(getStringMonitorProxy())    );
        setEmergencyActivation(  copyObject(getEmergencyActivation())   );
    }


    /**
     * copyNonRoots
     */
    void copyNonRoots() {
        int oop;
        if (getCurrentSpaceFreePtr() == getCurrentSpace()) {
            return; /* Empty Heap! */
        }
        for (oop = chunkToOop(targetSpace) ; oop != 0 ; oop = objectAfter(oop)) {

            int cls = getClass(oop);                        /* Get the class of the oop*/
            int typ = getWord(cls, CLS_gctype);             /* Get the gctype of the class */
            int map = getWord(cls, CLS_oopMap);             /* Get the oop map for the class */
            int lth = 0;
            int mapOffset = 0;

            if (getTraceGCVerbose()) {
                traceMsg("copyNonRoots oop="); traceHex(oop);
                traceMsg(" gctype=");          traceHex(typ);
                traceMsg(" cls=\"");           trace_String(getWord(cls, CLS_className)); traceMsg("\"");
                traceMsg(" cno=");             traceHex(getWord(cls, CLS_classIndex));
                traceMsg(" map=");             traceHex(map);
                traceMsg("\n");
            }

            updateOop(oop, OBJ_header);                 /* Mark the object header */

            switch (typ) {
                case GCTYPE_nopointers:
                case GCTYPE_byteArray:
                case GCTYPE_halfArray:
                case GCTYPE_wordArray:
                case GCTYPE_longArray:
                    break;

                case GCTYPE_object: {
                    lth = getHeaderLength(map);
                    break;
                }
                case GCTYPE_gvArray: {
                    map = getWord(oop, ISO_isolateStateOopMap);
                    assume(map != 0);
                    lth = getHeaderLength(map);
                    if (getTraceGCVerbose()) {
                        traceMsg("GCTYPE_gvArray lth=" );
                        traceHex(lth);
                        traceMsg("\n");
                    }
                    break;
                }
                case GCTYPE_oopArray: {
                    int len = getHeaderLength(oop);
                    int i;
                    for (i = 0 ; i < len ; i++) {
                        updateOop(oop, i);
                    }
                    break;
                }
                case GCTYPE_arArray: {
                    int mth = getWord(oop, AR_method);
                    assume(mth != 0);
                    lth = getUnsignedByte(mth, MTH_oopMapLength);
                    map = mth;
                    mapOffset = MTH_oopMap;
                    assume(lth > 0);
                    if (getTraceGCVerbose()) {
                        int i;
                        int hdr = getHeader(mth);
                        if ((hdr & FORWARDBIT) != 0) {
                            int relocMth = hdr & ~FORWARDBIT;
                            int mthLength = getHeaderLength(mth);
/*traceMsg("method was forwarded from "); traceInt(mth); traceMsg("\n");*/
                            assume(mthLength == getHeaderLength(relocMth));
                            mth = relocMth;
                        }
                        traceMsg("GCTYPE_arArray oopMapLength=");
                        traceHex(lth);
                        traceMsg(" arSize=");
                        traceHex(getUnsignedHalfFromByteArray(mth, MTH_arSizeHigh));
                        traceMsg(" mth=");
                        traceHex(mth);
                        traceMsg(" \"");
                        traceMethodName(mth, true);
                        traceMsg("\" map={");
                        for (i = 0; i < lth; i++) {
                            traceMsg(" ");
                            traceHex(getUnsignedByte(map, MTH_oopMap+i));
                        }
                        traceMsg(" }\n");
                    }
                    assume((getUnsignedByte(map, MTH_oopMap) & 2) == 0);
                    break;
                }

                default: {
                    shouldNotReachHere();
                }
            }

            if (lth > 0 ) {
               /*
                * Iterate through the oopmap updating all the pointers
                * in the object.
                */
                int oopIndex = 0;
                int i;
                int end = mapOffset + lth;
                for (i = mapOffset ; i < end ; i++) {
                    int mapbyte = getUnsignedByte(map, i);
                    if ((mapbyte&0x01) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x02) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x04) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x08) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x10) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x20) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x40) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                    if ((mapbyte&0x80) != 0) { updateOop(oop, oopIndex); } oopIndex++;
                }
            }
        }
    }

    /**
     * Perform a garbage collection
     */
    boolean gcPrim() {

        int start = getHeapStart() * ADDRESS_UNITS;
        int size  = getHeapSize()  * ADDRESS_UNITS;
        int half  = size / 2;
        int before = 0;

        if (getTraceGC()) {
            traceMsg("Garbage collecting (after ");
            traceLong(getInstructionCount());
            traceMsg(" instructions)...\n");
            before = freeMemInWords();
        }

        if (getCurrentSpace() == start) {
            targetSpace         = start + half;
            targetSpaceFreePtr  = start + half;
            targetSpaceEnd      = start + size;
        } else {
            targetSpace         = start;
            targetSpaceFreePtr  = start;
            targetSpaceEnd      = start + half;
        }

        copyRoots();
        copyNonRoots();

        setCurrentSpace(        targetSpace);
        setCurrentSpaceFreePtr( targetSpaceFreePtr);
        setCurrentSpaceEnd(     targetSpaceEnd);

        targetSpace         = 999999999;
        targetSpaceFreePtr  = 999999999;
        targetSpaceEnd      = 999999999;


        incCollectionCount();

        if (getTraceGC()) {
            traceMsg("Collected ");
            traceInt((freeMemInWords() - before) * 4);
            traceMsg(" bytes of garbage (");
            traceInt(freeMemInWords() * 4);
            traceMsg("/");
            traceInt((getCurrentSpaceEnd() - getCurrentSpace()) * 4);
            traceMsg(" bytes free)\n");
        }

        return freeMemInWords() >= getFailedAllocationSize();
    }



    /**
     * Perform a garbage collection
     */
    boolean gc() {
        boolean res = gcPrim();
/*IFJ*/ clearTargetSpace();
        return res;
    }

/*-----------------------------------------------------------------------*\
 *                      Test functions                                   *
\*-----------------------------------------------------------------------*/

    void invalidHeap(int oop, String msg) {
        traceMsg("Heap validation failed at oop: ");
        traceHex(oop);
        traceMsg(" (type = ");
        trace_String(getWord(getClass(oop), CLS_className));
        traceMsg(")\n");
        fatalVMError(msg);
    }

    void checkValue(int oop, String name, int value, int lo, int hi) {
        if (value < lo || value > hi) {
            traceMsg("Illegal ");
            traceMsg(name);
            traceMsg(" value: ");
            traceInt(value);
            traceMsg(" (expected ");
            traceInt(lo);
            if (lo != hi) {
                traceMsg(" .. ");
                traceInt(hi);
            }
            traceMsg(")\n");
            invalidHeap(oop, "Illegal value");
        }
    }

    void checkNotNull(int oop, String name, int value) {
        if (value == 0) {
            traceMsg("Null ");
            traceMsg(name);
            traceMsg(" value: ");
            traceInt(value);
            traceMsg("\n");
            invalidHeap(oop, "Null value");
        }
    }

    void checkOop(int base, int offset, int delta) {
        int oop = getWord(base, offset);
        int tag = oop + delta;
        if (oop != 0 && (tag <= getHeapStart() || tag >= (getHeapStart() + getHeapSize()) || getWord(0, tag) == 0)) {
            traceMsg("Oop ");
            traceHex(base);
            traceMsg(" % ");
            traceInt(offset);
            traceMsg(" is invalid: ");
            traceHex(oop);
            traceMsg("\n");
            invalidHeap(base, "Bad oop field/entry value");
        }
    }

    void validateHeap() {
        int start = getHeapStart() * ADDRESS_UNITS;
        int size  = getHeapSize()  * ADDRESS_UNITS;
        int half  = size / 2;
        int count = 0;
        int oop;
        int i;
        int clsClass       = getClassFromCNO(CNO_Class);
        int clsGlobalArray = getClassFromCNO(CNO_globalArray);
        int clsLocalArray  = getClassFromCNO(CNO_localArray);

        traceMsg("Validating heap (after ");
        traceInt((int)getInstructionCount());
        traceMsg(" instructions)\n");

        if (getCurrentSpace() != start) {
            int end = start + half;
            half = -half;
            for (i = start ; i < end ; i++) {
                setWord(i, 0, 0);
            }
        }
        else {
            int end = start + size;
            for (i = start + half ; i < end ; i++) {
                setWord(i, 0, 0);
            }
        }

        targetSpace         = getCurrentSpace();
        targetSpaceFreePtr  = getCurrentSpaceFreePtr();
        targetSpaceEnd      = getCurrentSpaceEnd();

        if (getCurrentSpaceFreePtr() == getCurrentSpace()) {
            return; /* Empty Heap! */
        }

        // First scan...
        count = 0;
        for (oop = chunkToOop(targetSpace) ; oop != 0 ; oop = objectAfter(oop)) {
            int cls = getClass(oop);                        /* Get the class of the oop*/
            int map = getWord(cls, CLS_oopMap);             /* Get the oop map for the class */
            int typ = getWord(cls, CLS_gctype);

            checkValue  (oop, "CLS_gctype",   typ, GCTYPE_nopointers, GCTYPE_oopArray);
            checkValue  (oop, "CLS_self",     getWord(cls, CLS_self), cls, cls);
            checkNotNull(oop, "CLS_oopMap",   getWord(cls, CLS_oopMap));
            checkValue  (oop, "class header", getClass(cls), clsClass, clsClass);

            // Record address as object start
            setWord(oop, half, 1);
            count++;
        }

        // Second scan
        count = 0;
        for (oop = chunkToOop(targetSpace) ; oop != 0 ; oop = objectAfter(oop)) {
            int cls = getClass(oop);                        /* Get the class of the oop*/
            int map = getWord(cls, CLS_oopMap);             /* Get the oop map for the class */
            int typ = getWord(cls, CLS_gctype);
            int lth = 0;
            int mapOffset = 0;

//traceInt(count);
//traceMsg(": ");
//trace_String(getWord(cls, CLS_className));
//traceMsg("\n");
            checkOop(oop, OBJ_header, half);                 /* Validate the object header */

            switch (typ) {
                case GCTYPE_nopointers:
                case GCTYPE_byteArray:
                case GCTYPE_halfArray:
                case GCTYPE_wordArray:
                case GCTYPE_longArray:
                    break;

                case GCTYPE_object: {
                    lth = getHeaderLength(map);
                    break;
                }
                case GCTYPE_gvArray: {
                    map = getIsolateStateOopMap();
                    lth = getHeaderLength(map);
                    checkValue(oop, "class type", cls, clsGlobalArray, clsGlobalArray);
                    break;
                }
                case GCTYPE_oopArray: {
                    int len = getHeaderLength(oop);
                    for (i = 0 ; i < len ; i++) {
                        checkOop(oop, i, half);
                    }
                    break;
                }
                case GCTYPE_arArray: {
                    int mth = getWord(oop, AR_method);
                    checkValue(oop, "class type", cls, clsLocalArray, clsLocalArray);
                    checkNotNull(oop, "AR_method", mth);
                    lth = getUnsignedByte(mth, MTH_oopMapLength);
                    map = mth;
                    mapOffset = MTH_oopMap;
                    assume((getUnsignedByte(map, MTH_oopMap) & 2) == 0);
                    break;
                }

                default: {
                    shouldNotReachHere();
                }
            }

            if (lth > 0 ) {
               /*
                * Iterate through the oopmap, checking all the pointers
                */
                int oopIndex = 0;
                int end = mapOffset + lth;
                for (i = mapOffset ; i < end ; i++) {
                    int mapbyte = getUnsignedByte(map, i);
                    if ((mapbyte&0x01) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x02) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x04) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x08) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x10) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x20) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x40) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                    if ((mapbyte&0x80) != 0) { checkOop(oop, oopIndex, half); } oopIndex++;
                }
            }

            count++;
        }

        targetSpace         = 999999999;
        targetSpaceFreePtr  = 999999999;
        targetSpaceEnd      = 999999999;

    }
//IFC//#ifndef PRODUCTION

    /**
     * Ensure each each in the class table is consisent. The meaning of consistent
     * right now is that the CLS_classIndex of every entry is correct.
     */
    void ensureConsistentClassTable() {
        int table = getIsolateClassTable();
        int length = getArrayLength(table);
        int cno;
        int cls;
        for (cno = 1; cno != length; cno++) {
            if (getWord(getIsolateClassTable(),cno) == 0) {
                continue;
            }
            cls = getClassFromCNO(cno);
            if (getWord(cls,CLS_classIndex) != cno) {
                traceMsg("Entry has wrong cno in class table: cno=");
                traceInt(cno);
                traceMsg(", CLS_classIndex=");
                traceInt(getWord(cls,CLS_classIndex));
                traceMsg("\n");
                fatalVMError("Bad isolate class table");
            }
        }
    }

//IFC//#endif

/*************************************************************************\
 *               Bootstrap heap building Functions                       *
\*************************************************************************/

//IFC//#if 0

    public static boolean TRACEROMIZING = false;
    void TRACE(String msg) { if (TRACEROMIZING) { trace_threadID(); System.out.println(msg);} }


    public void dumpHeap() {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new BufferedOutputStream(new FileOutputStream("core")));
            // Print the class table
            printClassTable(ps,true);
            // Print the classes
            int table = getIsolateClassTable();
            int length = getHeaderLength(table);
            for (int i = 0; i != length; i++) {
                printClass(ps,getWord(table,i),true);
            }
            // Print the IsolateState
            printIsolateState(ps);

            int lengthWord      = p_getCurrentSpaceUsed();
            int lengthByte      = lengthWord * 4;
            ps.println("memory: " + lengthByte + " bytes");
            for (int i = 0 ; i < lengthByte ; i++) {
                if (i % 10 == 0) {
                    ps.print("\n"+i+": ");
                }
                ps.print(p_getUnsignedByte(i)+" ");
            }
            ps.println();
            ps.println("memory: " + lengthWord + " words");
            for (int i = 0 ; i < lengthWord ; i++) {
                if (i % 8 == 0) {
                    ps.print("\n"+i+": ");
                }
                ps.print(p_getWord(i)+" ");
            }
            ps.println();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }



    /**
     * Proxies for calling non-public methods
     */
    public int p_getCurrentSpaceFreePtr()   { return getCurrentSpaceFreePtr(); }
    public int p_getCurrentSpaceUsed()      { return getCurrentSpaceFreePtr() - getCurrentSpace(); }
    public int p_getUnsignedByte(int addr)  { return getUnsignedByte(0,addr);  }
    public int p_getAR()                    { return getActivationRecord();    }
    public int p_getWord(int addr)          { return getWord(0,addr);          }
    public int p_getMemorySize()            { return getMemorySize();          }
    public void compressHeap() {

        gc();
        if (getCurrentSpace() != getHeapStart()) {
            gc();
            assume(getCurrentSpace() == getHeapStart());
        }
        // Modify heap size so that next allocation will fail
        int newSize = (((getCurrentSpaceFreePtr() - getHeapStart()) - getHeapStart()) * 2);
        setHeapSize(newSize);
        setCurrentSpaceEnd(getCurrentSpaceFreePtr());
    }

    String getString(int addr, int off) { return getString(getWord(addr, off)); }
    String getString(int s) {
        int chars  = getWord(s, STR_value);
        int offset = getWord(s, STR_offset);
        int count  = getWord(s, STR_count);
        char[] res = new char[count - offset];
        for (int i = 0 ; i != count; i++) {
            res[i] = (char)getHalf(chars, i+offset);
        }
        return new String(res);
    }
    public void printIsolateState(PrintStream ps) {
        int isv = getIsolateState();
        int oopMap = getWord(isv,ISO_isolateStateOopMap);
        int length = getWord(isv,ISO_isolateStateLength);
        int oopMapLength = getHeaderLength(oopMap);
        assume(oopMapLength < length);
        ps.println("The IsolateState: " + isv + " (length = "+length+" words)");
        printWordArray(ps, isv, "  ", oopMap);
        ps.println("IsolateState.oopMap: "+oopMap+" (length = " + oopMapLength + " bytes)");
        printByteArray(ps,oopMap,"  ");
    }
    public void printClassTable(PrintStream ps, boolean verbose) {
        int table = getIsolateClassTable();
        int length = getHeaderLength(table);
        ps.println("The Isolate ClassTable: " + table + " (length = "+length+" words)");
        if (verbose) {
            printWordArray(ps, table, "  ", 0);
        }
    }
    public void printClass(PrintStream ps, int cls, boolean verbose) {

        int _self = getWord(cls,CLS_self);
        int _classIndex = getWord(cls,CLS_classIndex);
        int _accessFlags = getWord(cls,CLS_accessFlags);
        int _gctype = getWord(cls,CLS_gctype);

        int _length = getWord(cls,CLS_length);
        int _className = getWord(cls,CLS_className);
        int _superClass = getWord(cls,CLS_superClass);
        int _elementType = getWord(cls,CLS_elementType);

        int _interfaces = getWord(cls,CLS_interfaces);
        int _vtable = getWord(cls,CLS_vtable);
        int _vstart = getWord(cls,CLS_vstart);
        int _vcount = getWord(cls,CLS_vcount);

        int _fvtable = getWord(cls,CLS_fvtable);
        int _itable = getWord(cls,CLS_itable);
        int _istart = getWord(cls,CLS_istart);
        int _iftable = getWord(cls,CLS_iftable);

        int _sftable = getWord(cls,CLS_sftable);
        int _oopMap =  getWord(cls, CLS_oopMap);

        String name = getString(_className);
        ps.println(name+": " + cls + " (mem used: " + memUsed.get(new Integer(_classIndex)) + " bytes)");
        if (verbose) {
            ps.println("  CLS_self: " + _self);
            ps.println("  CLS_classIndex: " + _classIndex);
            ps.println("  CLS_accessFlags: " + _accessFlags);
            ps.println("  CLS_gctype: " + _gctype);
            ps.println("  CLS_length: " + _length);
            ps.println("  CLS_className: " + _className);
            ps.println("  CLS_superClass: " + _superClass);
            ps.println("  CLS_elementType: " + _elementType);
            ps.println("  CLS_interfaces: " + _interfaces);
            printWordArray(ps, _interfaces, "    ", 0);
            ps.println("  CLS_vtable: " + _vtable);
            printWordArray(ps, _vtable, "    ", 0);
            ps.println("  CLS_vstart: " + _vstart);
            ps.println("  CLS_vcount: " + _vcount);
            ps.println("  CLS_fvtable: " + _fvtable);
            printWordArray(ps, _fvtable, "    ", 0);
            ps.println("  CLS_itable: " + _itable);
            printShortArray(ps,_itable,"    ");
            ps.println("  CLS_istart: " + _istart);
            ps.println("  CLS_iftable: " + _iftable);
            printShortArray(ps,_iftable,"    ");
            ps.println("  CLS_sftable: " + _sftable);
            printShortArray(ps,_sftable,"    ");
            ps.println("  CLS_oopMap: " + _oopMap);
            printByteArray(ps,_oopMap,"    ");
        }
    }
    public void printWordArray(PrintStream ps, int addr, String indent, int oopMap) {
        if (addr != 0) {
            int length = getHeaderLength(addr);
            for (int i = 0; i != length; i++) {
                int val = getWord(addr, i);
                ps.print(indent + "[" + i + "]: " + val);
                if (val != 0 && oopMap != 0) {
                    int mapByte = getUnsignedByte(oopMap, i / 8);
                    int mask = 1 << (i % 8);
                    if ((mapByte & mask) != 0) {
                        int cls = getClass(val);
                        String name = getString(cls, CLS_className);
                        ps.print(" (cls=" + name + ")");
                    }
                }
                ps.println();
            }
        }
    }
    public void printShortArray(PrintStream ps, int addr, String indent) {
        if (addr != 0) {
            int length = getHeaderLength(addr);
            for (int i = 0; i != length; i++) {
                ps.println(indent+"["+i+"]: "+getHalf(addr,i));
            }
        }
    }
    public void printByteArray(PrintStream ps, int addr, String indent) {
        if (addr != 0) {
            int length = getHeaderLength(addr);
            for (int i = 0; i != length; i++) {
                ps.println(indent+"["+i+"]: "+getByte(addr,i));
            }
        }
    }

//    String inHex(int i)    { return Integer.toHexString(i); }
    String inHex(int i)    { return Integer.toString(i); }
    String inHex(byte[] b) {
        StringBuffer buf = new StringBuffer(b.length * 8);
        for (int i = 0; i != b.length; i++) {
            buf.append(inHex((int)b[i]));
        }
        return buf.toString();
    }
    String inHex(int[] b) {
        StringBuffer buf = new StringBuffer(b.length * 8);
        for (int i = 0; i != b.length; i++) {
            buf.append(inHex(b[i]));
        }
        return buf.toString();
    }
    String inHex(short[] b) {
        StringBuffer buf = new StringBuffer(b.length * 8);
        for (int i = 0; i != b.length; i++) {
            buf.append(inHex((short)b[i]));
        }
        return buf.toString();
    }

    public void dumpHeapStats(PrintStream out, String msg) {
        out.println("\n\n*** "+msg+" ***");
        out.println("currentSpace        "+getCurrentSpace()         +"\t("+ inHex(getCurrentSpace()         )+")");
        out.println("currentSpaceFreePtr "+getCurrentSpaceFreePtr()  +"\t("+ inHex(getCurrentSpaceFreePtr()  )+")");
        out.println("currentSpaceEnd     "+getCurrentSpaceEnd()      +"\t("+ inHex(getCurrentSpaceEnd()      )+")");
        out.println("targetSpace         "+targetSpace               +"\t("+ inHex(targetSpace               )+")");
        out.println("targetSpaceFreePtr  "+targetSpaceFreePtr        +"\t("+ inHex(targetSpaceFreePtr        )+")");
        out.println("targetSpaceEnd      "+targetSpaceEnd            +"\t("+ inHex(targetSpaceEnd            )+")");
        out.println("IsolateState        "+getIsolateState()         +"\t("+ inHex(getIsolateState()         )+")");
        out.println("IsolateStateOopMap  "+getIsolateStateOopMap()   +"\t("+ inHex(getIsolateStateOopMap()   )+")");
        out.println("IsolateClassTable   "+getIsolateClassTable()    +"\t("+ inHex(getIsolateClassTable()    )+")");
        out.println("GlobalAR            "+getActivationRecord()     +"\t("+ inHex(getActivationRecord()     )+")");
        out.println("EmergencyActivation "+getEmergencyActivation()  +"\t("+ inHex(getEmergencyActivation()  )+")");
        out.println("PrimitiveMethod     "+getPrimitiveMethod()      +"\t("+ inHex(getPrimitiveMethod()      )+")");
        out.println("IsolateState.length "+getWord(getIsolateState(),ISO_isolateStateLength));
    }

    public void dump(PrintStream out, String msg) {
        dumpHeapStats(out, msg);
        for (int i = 0 ; i < getMemorySize();) {
            prt16(out,i);
            out.print(": ");
            for (int j = 0 ; j < 8 ; j++) {
                prt32(out,getWord(i, 0));
                out.print(" ");
                i++;
            }
            out.print("\n");
        }
    }


    void prt32(PrintStream out,int i) {
        prt16(out,i>>16);
        prt16(out,i);
    }


    void prt16(PrintStream out,int i) {
        prt8(out,i>>8);
        prt8(out,i);
    }

    void prt8(PrintStream out,int i) {
        int b = i & 0xFF;
        int hi = b>>4;
        int lo = b&0xF;
        char[] table = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        out.print(""+table[hi]+table[lo]);
    }


    void clearTargetSpace() {

        int start = getHeapStart() * ADDRESS_UNITS;
        int size  = getHeapSize()  * ADDRESS_UNITS;
        int half  = size / 2;

        if (getCurrentSpace() == start) {
            targetSpace         = start + half;
            targetSpaceEnd      = start + size;
        } else {
            targetSpace         = start;
            targetSpaceEnd      = start + half;
        }

        if (getTraceGCVerbose()) {
            traceMsg("cleartargetSpace ");
            traceHex(targetSpace);
            traceMsg(" -> ");
            traceHex(targetSpaceEnd);
            traceMsg("\n");
        }
        for (int i = targetSpace ; i < targetSpaceEnd ; i++) {
            setWord(i, 0, 0xdead1dea);
        }

        targetSpace         = 999999999;
        targetSpaceFreePtr  = 999999999;
        targetSpaceEnd      = 999999999;
    }


    int primAlloc(int lth) {
        // just continue growing the heap until the allocation will be successful
        // of the host VM runs out of memory.
        while (getCurrentSpaceFreePtr() + lth > getCurrentSpaceEnd()) {
            int size = getMemorySize();
            int[] newMemory = new int[size * 2];
            for (int i = 0; i != size; i++) {
                newMemory[i] = getWord(0, i);
            }
            Memory_init(newMemory, size * 2);
            ObjectMemory_reinit();
        }
        int res = getCurrentSpaceFreePtr();
        setCurrentSpaceFreePtr(res + lth);
        for (int i = res ; i < (res + lth) ; i++) {
            setWord(i, 0, 0);
        }
        alloced += lth * 4;
        return res;
    }

    int allocInstance(int lth) {
        int oop = primAlloc(lth+1) + 1;
        setHeader(oop, -1);
        TRACE("allocInstance("+lth+") = "+inHex(oop));
        return oop;
    }

    int alloc32bitArray(int lth) {
        int oop = primAlloc(lth+2) + 2;
        setHeader(oop, -1);
        setHeaderLength(oop, lth);
        return oop;
    }

    int alloc16bitArray(int lth) {
        int wordlth = (lth+1)/2;
        int oop = primAlloc(wordlth+2) + 2;
        setHeader(oop, -1);
        setHeaderLength(oop, lth);
        return oop;
    }

    int alloc8bitArray(int lth) {
        int wordlth = (lth+3)/4;
        int oop = primAlloc(wordlth+2) + 2;
        setHeader(oop, -1);
        setHeaderLength(oop, lth);
        return oop;
    }

    int classClassBase;
    int classClass;
    int classObject;
    int classMonitor;
    int classClassMonitor = -1;

    int allocClassClass(int classNumber) {
        int cls = allocInstance(CLS_LENGTH);
        setWord(cls, CLS_self, cls);
        setWord(cls, CLS_length, CLS_LENGTH);
        setWord(cls, CLS_classIndex, classNumber);
        setWord(cls, CLS_gctype, GCTYPE_object);
        setWord(cls, CLS_oopMap, -1);
        setHeader(cls, -1); // Class is its own class (via a monitor added later)
        return cls;
    }

    int allocInstanceClass(int classNumber, int instanceSize) {
        int cls = allocClass(classNumber, GCTYPE_object, instanceSize);
        TRACE("allocInstanceClass("+classNumber+") = "+inHex(cls));
        return cls;
    }

    int allocArrayClass(int classNumber, int gcType) {
        int cls = allocClass(classNumber, gcType, 0);
        TRACE("allocArrayClass("+classNumber+") = "+inHex(cls));
        return cls;
    }

    int allocClass(int classNumber, int gcType, int instanceSize) {
        int cls = allocInstance(CLS_LENGTH);
        setWord(cls, CLS_self, cls);
        setWord(cls, CLS_length, instanceSize);
        setWord(cls, CLS_classIndex, classNumber);
        setWord(cls, CLS_gctype, gcType);
        setWord(cls, CLS_superClass, classObject);
        setWord(cls, CLS_oopMap, -1);
        setHeader(cls, classClassMonitor);
        TRACE("allocClass("+classNumber+") = "+inHex(cls));
        return cls;
    }


    void addToClassTable(int forclass) {
        int cno = getWord(forclass, CLS_classIndex);
        assume(getIsolateClassTableAt(cno) == 0);
        setIsolateClassTableAt(cno, forclass);
    }


    int allocProxyMonitor(int cls, int classNumber) {
        int mon = allocInstance(MON_LENGTH);
        setWord(mon, MON_realType, cls);
        setWord(mon, MON_isProxy, 1);
        setHeader(mon, classMonitor);
        return mon;
    }


    void buildPrimPrim(int classTableSize) {
        int classStringMonitor;

       /*
        * Allocate a 32 bit array of classTableSize words and make it the IsolateClassTable.
        */
        assume(getIsolateState() != 0);
        setIsolateClassTable(alloc32bitArray(classTableSize));

       /*
        * Allocate classes Class and ClassBase
        */
        resetAlloced(); classClass     = allocClassClass(CNO_Class);     incMemUsed(CNO_Class);
        resetAlloced(); classClassBase = allocClassClass(CNO_ClassBase); incMemUsed(CNO_ClassBase);

       /*
        * Allocate class Object (this will get a null super class)
        */
        resetAlloced(); classObject = allocInstanceClass(CNO_Object, 0); incMemUsed(CNO_Object);

       /*
        * Create the following hierarchy: Class extends ClassBase extends Object
        */
        setWord(classClassBase, CLS_superClass, classObject);
        setWord(classClass,     CLS_superClass, classClassBase);

       /*
        * Allocate the monitor class and a monitor for class class
        */
        resetAlloced(); classMonitor = allocInstanceClass(CNO_Monitor, MON_LENGTH);   incMemUsed(CNO_Monitor);
        resetAlloced(); classClassMonitor  = allocProxyMonitor(classClass, CNO_Class); incMemUsed(CNO_Class);
        setClassMonitorProxy(classClassMonitor);

       /*
        * Set the class pointers to the four existing classes to the monitor for class class
        */
        setHeader(classClass,     classClassMonitor);
        setHeader(classClassBase, classClassMonitor);
        setHeader(classObject,    classClassMonitor);
        setHeader(classMonitor,   classClassMonitor);

       /*
        * Add the primitive classes to the classTable
        */
        resetAlloced(); addToClassTable(classClass);      incMemUsed(CNO_Class);
        resetAlloced(); addToClassTable(classClassBase);  incMemUsed(CNO_ClassBase);
        resetAlloced(); addToClassTable(classObject);     incMemUsed(CNO_Object);
        resetAlloced(); addToClassTable(classMonitor);    incMemUsed(CNO_Monitor);

       /*
        * Allocate the class String and its proxy monitor
        */
        makeClass(CNO_String,  CNO_Object, 0, STR_LENGTH, GCTYPE_object);
        resetAlloced(); classStringMonitor = allocProxyMonitor(getClassFromCNO(CNO_String), CNO_String); incMemUsed(CNO_String);
        setStringMonitorProxy(classStringMonitor);
    }

    int makeClass(int classNumber, int superCNO, int elementCNO, int instanceSize, int gcType) {
        resetAlloced();
        int cls = allocClass(classNumber, gcType, instanceSize);
        addToClassTable(cls);
        setWord(cls, CLS_superClass,  getClassFromCNO(superCNO));
        setWord(cls, CLS_elementType, getClassFromCNO(elementCNO));

        TRACE("makeClass("+classNumber+") = "+inHex(cls));
        incMemUsed(classNumber);
        return cls;
    }


    void buildPrim(int classTableSize) {
        buildPrimPrim(classTableSize);

        makeClass(CNO_primitive,        CNO_Object,         0,               0, GCTYPE_spiritual);
        makeClass(CNO_void,             CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_int,              CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_long,             CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_float,            CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_double,           CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_boolean,          CNO_int,            0,               0, GCTYPE_spiritual);
        makeClass(CNO_char,             CNO_int,            0,               0, GCTYPE_spiritual);
        makeClass(CNO_short,            CNO_int,            0,               0, GCTYPE_spiritual);
        makeClass(CNO_byte,             CNO_int,            0,               0, GCTYPE_spiritual);
        makeClass(CNO_global,           CNO_primitive,      0,               0, GCTYPE_spiritual);
        makeClass(CNO_local,            CNO_primitive,      0,               0, GCTYPE_spiritual);

        makeClass(CNO_intArray,         CNO_Object,         CNO_int,         0, GCTYPE_wordArray);
        makeClass(CNO_longArray,        CNO_Object,         CNO_long,        0, GCTYPE_longArray);
        makeClass(CNO_floatArray,       CNO_Object,         CNO_float,       0, GCTYPE_wordArray);
        makeClass(CNO_doubleArray,      CNO_Object,         CNO_double,      0, GCTYPE_longArray);
        makeClass(CNO_booleanArray,     CNO_Object,         CNO_boolean,     0, GCTYPE_byteArray);
        makeClass(CNO_charArray,        CNO_Object,         CNO_char,        0, GCTYPE_halfArray);
        makeClass(CNO_shortArray,       CNO_Object,         CNO_short,       0, GCTYPE_halfArray);
        makeClass(CNO_byteArray,        CNO_Object,         CNO_byte,        0, GCTYPE_byteArray);
        makeClass(CNO_globalArray,      CNO_Object,         CNO_global,      0, GCTYPE_gvArray);
        makeClass(CNO_localArray,       CNO_Object,         CNO_local,       0, GCTYPE_arArray);

        makeClass(CNO_ObjectArray,      CNO_Object,         CNO_Object,      0, GCTYPE_oopArray);
        makeClass(CNO_StringArray,      CNO_ObjectArray,    CNO_String,      0, GCTYPE_oopArray);
        makeClass(CNO_ClassBaseArray,   CNO_ObjectArray,    CNO_ClassBase,   0, GCTYPE_oopArray);
        makeClass(CNO_ClassArray,       CNO_ClassBaseArray, CNO_Class,       0, GCTYPE_oopArray);

        makeClass(CNO_byteArrayArray,   CNO_ObjectArray,    CNO_byteArray,   0, GCTYPE_oopArray);

        /*
         * Set the header for the class table to be class ClassBase[]
         */
        int classClassBaseArray = getClassFromCNO(CNO_ClassBaseArray);
        setHeader(getIsolateClassTable(), classClassBaseArray);

        /*
         * Set the oop map Class and ClassBase now that byte array class has been created.
         * This is required for a garbage collection of the primordial heap to be successful.
         */
        int map = createByteArray(new byte[] { (byte)CLS_MAP0, (byte)CLS_MAP1, (byte)CLS_MAP2 });
        setWord(classClass,     CLS_oopMap, map);
        setWord(classClassBase, CLS_oopMap, map);
        setWord(classMonitor,   CLS_oopMap, createByteArray(new byte[] { MON_MAP0 }));

        noClasses = createClassArray(new int[0]);
        noShorts  = createShortArray(new short[0]);
        noMethods = createByteArrayArray(new byte[0][0]);
        noStrings = createStringArray(new String[0]);
    }


    int noClasses;
    int noMethods;
    int noShorts;
    int noStrings;
    Hashtable uniqueInstancePool = new Hashtable();
    Hashtable memUsed = new Hashtable();
    int alloced = 0;

    void resetAlloced() { alloced = 0; }
    void incMemUsed(int cls) {
        Integer key = new Integer(cls);
        Integer used = (Integer)memUsed.get(key);
        if (used == null) {
            used = new Integer(alloced);
        }
        else {
            used = new Integer(alloced + used.intValue());
        }
        memUsed.put(key,used);
    }
    Integer getUniqueInstance(String key, String type) {
        Integer res = (Integer)uniqueInstancePool.get(type+":"+key);
        return res;
    }
    void setUniqueInstance(String key, String type, Integer address) {
        assume(uniqueInstancePool.put(type+":"+key,address) == null);
    }

    int createByteArray(byte[] array) {
        assume(array != null);
        String key = inHex(array);
        Integer addr = getUniqueInstance(key,"ByteArray");
        if (addr == null) {
            int res = alloc8bitArray(array.length);
            setHeader(res, getClassFromCNO(CNO_byteArray));
            for (int i = 0 ; i < array.length ; i++) {
                setByte(res, i, array[i]);
            }

        TRACE("createByteArray "+inHex(res)+", lth = " + inHex(array.length));
            addr = new Integer(res);
            setUniqueInstance(key,"ByteArray",addr);
        }
        return addr.intValue();
    }

    int createCharArray(char[] array) {
        assume(array != null);
        String key = new String(array);
        Integer addr = getUniqueInstance(key,"CharArray");
        if (addr == null) {
            int res = alloc16bitArray(array.length);
            setHeader(res, getClassFromCNO(CNO_charArray));
            for (int i = 0 ; i < array.length ; i++) {
                setHalf(res, i, array[i]);
            }

        TRACE("createCharArray "+inHex(res)+", lth = " + inHex(array.length));
            addr = new Integer(res);
            setUniqueInstance(key,"CharArray",addr);
        }
        return addr.intValue();
    }

    int createShortArray(short[] array) {
        if (array == null) {
            return noShorts;
        }
        String key = inHex(array);
        Integer addr = getUniqueInstance(key,"ShortArray");
        if (addr == null) {
            int res = alloc16bitArray(array.length);
            setHeader(res, getClassFromCNO(CNO_shortArray));
            for (int i = 0 ; i < array.length ; i++) {
                setHalf(res, i, array[i]);
            }

        TRACE("createShortArray "+inHex(res)+", lth = " + inHex(array.length));
            addr = new Integer(res);
            setUniqueInstance(key,"ShortArray",addr);
        }
        return addr.intValue();
    }

    int createClassArray(int[] cnos) {
        if (cnos == null) {
            return noClasses;
        }
        int[] words = new int[cnos.length];
        for (int i = 0 ; i < cnos.length ; i++) {
            words[i] = getClassFromCNO(cnos[i]);
        }
        String key = inHex(cnos);
        Integer addr = getUniqueInstance(key,"ClassArray");
        if (addr == null) {
            int res = alloc32bitArray(cnos.length);
            setHeader(res, getClassFromCNO(CNO_ClassArray));
            for (int i = 0 ; i < cnos.length ; i++) {
                setWord(res, i, words[i]);
            }

        TRACE("createClassArray "+inHex(res)+", lth = " + inHex(cnos.length));
            addr = new Integer(res);
            setUniqueInstance(key,"ClassArray",addr);
        }
        return addr.intValue();
    }


    int createByteArrayArray(byte[][] array) {
        if (array == null) {
            return noMethods;
        }
        int[] words = new int[array.length];
        for (int i = 0 ; i < array.length ; i++) {
            words[i] = createByteArray(array[i]);
        }
        String key = inHex(words);
        Integer addr = getUniqueInstance(key,"ByteArrayArray");
        if (addr == null) {
            int res = alloc32bitArray(array.length);
            setHeader(res, getClassFromCNO(CNO_byteArrayArray));
            for (int i = 0 ; i < array.length ; i++) {
                setWord(res, i, words[i]);
            }

        TRACE("createByteArrayArray "+inHex(res)+", lth = " + inHex(array.length));
            addr = new Integer(res);
            setUniqueInstance(key,"ByteArrayArray",addr);
        }
        return addr.intValue();
    }

    int createString(String str) {
        assume(str != null);
        Integer addr = getUniqueInstance(str,"String");
        if (addr == null) {
            int chars  = createCharArray(str.toCharArray());
            int string = allocInstance(STR_LENGTH);
            //setHeader(string, getClassFromCNO(CNO_String));
            setHeader(string, getStringMonitorProxy());
            setWord(string, STR_value, chars);
            setWord(string, STR_offset, 0);
            setWord(string, STR_count, str.length());

        TRACE("createString "+inHex(string)+" = " + str);
            addr = new Integer(string);
            setUniqueInstance(str,"String",addr);
        }
        return addr.intValue();
    }

    int createStringArray(String[] array) {
        if (array == null) {
            return noStrings;
        }
        int[] strings = new int[array.length];
        for (int i = 0 ; i < array.length ; i++) {
            strings[i] = createString(array[i]);
        }
        String key = inHex(strings);
        Integer addr = getUniqueInstance(key,"StringArray");
        if (addr == null) {
            int res = alloc32bitArray(strings.length);
            setHeader(res, getClassFromCNO(CNO_StringArray));
            for (int i = 0 ; i < strings.length ; i++) {
                setWord(res, i, strings[i]);
            }

        TRACE("createStringArray "+inHex(res)+", lth = " + inHex(strings.length));
            addr = new Integer(res);
            setUniqueInstance(key,"StringArray",addr);
        }
        return addr.intValue();
    }

    void createBootstrapActivation(int size, int mth, int ip) {
        int ar = alloc32bitArray(size);
        assume(size >= AR_locals);
        setHeader(ar, getClassFromCNO(CNO_localArray));
        setWord(ar, AR_method,     mth);
        setWord(ar, AR_ip,         ip);
        setWord(ar, AR_previousAR, 0);
        setActivationRecord(ar);
    }

    public int createClass(
                        int      cno,
                        int      accessFlags,
                        int      instanceSize,
                        String   name,
                        int      superCNO,
                        int      elementCNO,
                        int[]    interfaceCNOs,
                        byte[][] vtable,
                        int      vstart,
                        int      vcount,
                        byte[][] fvtable,
                        short[]  itable,
                        int      istart,
                        short[]  iftable,
                        short[]  sftable,
                        String[] constTable,
                        byte[]   debugInfo,
                        byte[]   oopMap
                    ) {
        int gctype = (elementCNO == 0) ? GCTYPE_object : GCTYPE_oopArray;
        int cls = getIsolateClassTableAt(cno);
        if (cls == 0) {
            cls = makeClass(cno, superCNO, elementCNO, instanceSize, gctype);
            assume(cls != 0);
        } else {
            assume(cls != 0);
            assume(getWord(cls, CLS_self)        == cls);
            assume(getWord(cls, CLS_classIndex)  == cno);
//            assume(getWord(cls, CLS_gctype)      == gctype || getWord(cls, CLS_gctype) == GCTYPE_spiritual);
            assume(getWord(cls, CLS_superClass)  == getClassFromCNO(superCNO));
            assume(getWord(cls, CLS_elementType) == getClassFromCNO(elementCNO));

if (getWord(cls, CLS_length) != instanceSize) {
  System.err.println("cls = "+name+" instanceSize="+instanceSize+" CLS_length="+getWord(cls, CLS_length));
}

            assume(getWord(cls, CLS_length)      == instanceSize);
        }
        resetAlloced();
        setWord(cls, CLS_accessFlags, accessFlags);
        setWord(cls, CLS_className,   createString(name));
        setWord(cls, CLS_interfaces,  createClassArray(interfaceCNOs));
        setWord(cls, CLS_vtable,      createByteArrayArray(vtable));
        setWord(cls, CLS_vstart,      vstart);
        setWord(cls, CLS_vcount,      vcount);
        setWord(cls, CLS_fvtable,     createByteArrayArray(fvtable));
        setWord(cls, CLS_itable,      createShortArray(itable));
        setWord(cls, CLS_istart,      istart);
        setWord(cls, CLS_iftable,     createShortArray(iftable));
        setWord(cls, CLS_sftable,     createShortArray(sftable));
        setWord(cls, CLS_constTable,  createStringArray(constTable));
        if (debugInfo == null) {
            setWord(cls, CLS_debugInfo,   0);
        }
        else {
            setWord(cls, CLS_debugInfo,   createByteArray(debugInfo));
        }

       /*
        * Create the initial activation record if this class is java.lang.Object.
        * The method is _SQUAWK_INTERNAL_vmstart() and is assumed to be slot 1
        * in this class.
        */
        if (cno == CNO_VMSTART) {
            int fvtab = getWord(cls, CLS_fvtable);
            int mth = lookupMethod(cls,SLOT_vmstart);
            assume(mth != 0);
            int arSize = (getUnsignedByte(mth, MTH_arSizeHigh) << 8) + getUnsignedByte(mth, MTH_arSizeLow);
            int targetIP = getByte(mth, 4);
            int targetParms = getByte(mth, targetIP++);
            assume(targetParms == 1);
            targetIP++; /* Jump past receiver to first bytecode */

            createBootstrapActivation(arSize, mth, targetIP);
        }

       /*
        * Look for the _SQUAWK_INTERNAL_primitive in class Native
        */
        if (cno == CNO_PRIMITIVE) {
            int mth = lookupMethod(cls,SLOT_primitive);
            setPrimitiveMethod(mth);
        }

        /*
         * Allocate the oop map
         */
        int map = getWord(cls, CLS_oopMap);
        assume(map != 0);
        setWord(cls, CLS_oopMap, createByteArray(oopMap));

        /*
         * Verify that certain well-known maps are as expected.
         */
        switch (cno) {
            case CNO_Class:
            case CNO_ClassBase: assume(java.util.Arrays.equals(oopMap, new byte[] { (byte)CLS_MAP0, (byte)CLS_MAP1, (byte)CLS_MAP2 })); break;
            case CNO_Monitor:   assume(java.util.Arrays.equals(oopMap, new byte[] { (byte)MON_MAP0                                 })); break;
            case CNO_String:    assume(java.util.Arrays.equals(oopMap, new byte[] { (byte)STR_MAP0                                 })); break;
        }

        incMemUsed(cno);
        return cls;
    }


    public void build(int classTableSize, int isolateStateSize, byte[] isoOopMap) {

        /*
         * Allocate the isolate state vector.
         */
        int isv = alloc32bitArray(isolateStateSize);
        setIsolateState(isv);

        /*
         * Build fundamental classes.
         */
        buildPrim(classTableSize);

        /*
         * Allocate isolate state oop map and assign it into its slot in the isolate state array.
         */
        int oopMap = createByteArray(isoOopMap);
        setHeader(isv, getClassFromCNO(CNO_globalArray));
        setIsolateStateOopMap(oopMap);

        /*
         * Fill in the intial entries of the isolate state as follows:
         *
         *  [ISO_isolateState      ] Pointer to the IsolateState itself.
         *  [ISO_isolateStateOopMap] Pointer to oop map for IsolateState
         *  [ISO_isolateStateLength] Current used size of IsolateState
         *  [ISO_classTable        ] Isolate class table
         *  [ISO_classThreadTable  ] Isolate class initializing thread table
         *  [ISO_classStateTable   ] Isolate class initialization state table
         *
         */
        setWord(isv,ISO_isolateState,isv);
        setWord(isv,ISO_isolateStateOopMap,oopMap);
        setWord(isv,ISO_isolateStateLength,isolateStateSize);
        setWord(isv,ISO_classTable,getIsolateClassTable());

        /*
         * Give every class a temporary 0 length oop map. This will be updated when the
         * class definitions are read in (i.e. in createClass). This simply allows
         * the heap to be garbage collected safely once this method returns. Of course,
         * this must not happen in any case except in ObjectMemoryTester
         */
        int table = getIsolateClassTable();
        int lth = getHeaderLength(table);
        int tmpMap = createByteArray(new byte[0]);
        for (int i = 0; i != lth; i++) {
            int cls = getWord(table, i);
            if (cls != 0 && getWord(cls, CLS_oopMap) == -1) {
                setWord(cls, CLS_oopMap, tmpMap);
            }
        }
    }

    public void addEntryToIsolateState(int offset, int value) {
        // No need to add primitives with default value to isolate state
        assume(value != 0);
        int isv = getIsolateState();
        int length = getWord(isv,ISO_isolateStateLength);
        TRACE("isolateStateLength = " + length);
        assume(offset < length);
        setWord(isv, offset, value);
        TRACE("add...ToIsolateState "+inHex(offset) + " = " + inHex(value));
    }

    void setBit(int oopmap, int n) {
        int addr = n/8;
        int bit  = n%8;
        int b = getByte(oopmap, addr);
        setByte(oopmap, addr, b | (1<<bit));
        TRACE("setBit "+inHex(n));
    }

    public void addStringToIsolateState(int offset, String string) {
        addEntryToIsolateState(offset, createString(string));
    }

    public int getIsolateStateLength() {
        int isv = getIsolateState();
        int length = getWord(isv,ISO_isolateStateLength);
        return length;
    }
//IFC//#endif
/*IFJ*/}




















