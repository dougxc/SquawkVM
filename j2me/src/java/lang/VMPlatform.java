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
 * VMPlatform is where the VM version specific parts reside.
 */
public abstract class VMPlatform implements NativeOpcodes {

    /**
     * This is the first method that the VM will run. It is not run using a real thread
     * but only has activation record. This is good enough to run a few system
     * <clinit> in the Isloate constructor. When this is done the new isolate is started
     * using a special version of start() that will not reschedule the callers
     * thread (which, in this case, does not exist).
     */
    static void startVM() {

       /*
        * Run some tests
        */
        Test.runXTests();

       /*
        * Create the master isolate
        */
        Isolate jam = new Isolate();

       /*
        * Run some more tests
        */
        Test.runYTests();

       /*
        * Start the Java application manager
        */
        jam.startPrim();

       /*
        * Error if we return...
        */
        Native.fatalVMError();
    }


/*---------------------------------------------------------------------------*\
 *                     ClassBase.isolateState related methods                *
\*---------------------------------------------------------------------------*/

    /**
     * Increases the capacity of all the arrays associated with the globals
     * for an isolate. This includes the isolate state table and its oop map.
     * @param minCapacity The desired minimum capacity for all isolate state arrays.
     */
    private static void ensureIsolateStateCapacity(int minCapacity) {
        int length = ClassBase.isolateStateLength;
        // Do the isolateState first. If it has sufficient capacity already,
        // then its oop map will as well.
        if (length >= minCapacity)
            return;

        int newLength = ((length+1) * 3) / 2;
        if (newLength < minCapacity) {
            newLength = minCapacity;
        }
        // Get a handle onto the "_global_[]" class
        ClassBase isolateStateClass = ClassBase.isolateState.getClass();

        // Allocate the new int array that will become the new isolateState array
        int[] isolateState = new int[newLength];
        ClassBase intArrayClass = isolateState.getClass();
        int[] oldIsolateState = ClassBase.isolateState;

        // Need to use Native.arraycopy as the two arrays have different types: "_global_[]" and "int[]"
        Native.arraycopy(ClassBase.isolateState, 0, isolateState, 0, length);

        // Set the 'self' entry of the isolateState
        isolateState[0] = Native.asInt(isolateState);

        // Give the new isolate state the correct header
        Native.setHeader(isolateState, isolateStateClass);

        // This will also automatically set the value of ClassBase.isolateState
        Native.setIsolate(isolateState);
        Native.assume(ClassBase.isolateState == isolateState);

        // Copy the oop map
        length = (length+7) / 8;
        newLength = (newLength+7) / 8;
        byte[] oldOopMap = null;
        if (length < newLength) {
            byte[] oopMap = new byte[newLength];
            System.arraycopy(ClassBase.isolateStateOopMap, 0, oopMap, 0, length);
            oldOopMap = ClassBase.isolateStateOopMap;
            ClassBase.isolateStateOopMap = oopMap;
        }

        // Zero the old arrays
/*
        Native.setHeader(oldIsolateState, intArrayClass);
        for (int i = 0; i != oldIsolateState.length; i++) {
            oldIsolateState[i] = -1;
        }
        if (oldOopMap != null) {
            for (int i = 0; i != oldOopMap.length; i++) {
                oldOopMap[0] = (byte)0xFF;
            }
        }
*/
    }


    /**
     * Add a primitive entry to the isolateState array. The entry is added at an
     * offset relative to the start of the array that is in terms of it size.
     * @param entry Contains the data to add.
     * @returns the word index of the inserted data
     */
    static int addPrimitiveToIsolateState(int entry) {
        int length = ClassBase.isolateStateLength;

        // Required space is current length plus 1.
        int required = length + 1;

        // Ensure the capacity of isolate state arrays
        ensureIsolateStateCapacity(required);

        // Update the *used* length of the isolate arrays
        ClassBase.isolateStateLength = required;

        // Add the entry
        ClassBase.isolateState[length] = entry;

        return length;
    }


    /**
     * Add a reference entry to the isolateState array. The entry is added at
     * the next available offset of the array.
     * @param ref The reference to add.
     * @returns the index at which the reference was added.
     */
    static int addReferenceToIsolateState(Object ref) {
        // Only add a place holder null pointer to the isolate state until the
        // oop map for the isolate state has been updated to reflect the fact
        // that this actually a reference entry. Only then can the real
        // reference entry be added.
        int index = addPrimitiveToIsolateState(0);

        // Update the isolate state oop map
        byte[] oopMap = ClassBase.isolateStateOopMap;
        oopMap[index/8] |= 1 << (index % 8);

        // Now add the reference entry
        ClassBase.isolateState[index] = Native.asInt(ref);

        return index;
    }

    /**
     * The Squawk VM maps the first three statics of ClassBase to special
     * slots in the isloate state array:
     *
     *   ClassBase.isolateState       -> slot ISO_isolateState
     *   ClassBase.isolateStateOopMap -> slot ISO_isolateStateOopMap
     *   ClassBase.isolateStateLength -> slot ISO_isolateStateLength
     *   ClassBase.classTable         -> slot ISO_classTable
     *   ClassBase.classThreadTable   -> slot ISO_classThreadTable
     *   ClassBase.classStateTable    -> slot ISO_classStateTable
     *
     * This method doesn't need to do these mappings in the Squawk system as
     * it has already been done in the image.
     */
    static void mapIsolateStateFields(ClassBase clazz) {
    }

/*---------------------------------------------------------------------------*\
 *                     ClassBase.classTable related methods                  *
\*---------------------------------------------------------------------------*/

    /**
     * Receive notification that a given class was linked into the system.
     */
    static void classLinked(ClassBase clazz) {
    }

    /**
     * Create a instance of Class.
     */
    static ClassBase createClass(int id, int extnds, int arrayOf,
        String name, Vector impls, Vector constants, Vector svars, Vector ivars, Vector i_map,
        int accessFlags, boolean usesFvtable, int vtableStart, int vtableEnd, int firstNVMethod,
        byte[] debugInfo)
    {
        return new Class(
            id,extnds,arrayOf,name,impls,constants,svars,ivars,i_map,
            accessFlags,usesFvtable, vtableStart, vtableEnd, firstNVMethod, debugInfo);
    }

    /**
     * This is useful for debugging the SquawkClassLoader runnning in squawk.
     */
    static void setTracingThreshold(int i) {
        Native.setTracingThreshold(i);
    }

    /**
     * Return the system properties specified for current isolate. This needs to be
     * in this class as it is referenced from the static initializer of Native
     * which is run under both Hotspot and Squawk and therefore cannot directly
     * reference any Squawk only classes (such as Isolate).
     */
    static Hashtable getCurrentIsolateProperties() {
        Isolate isolate = Isolate.getCurrentIsolate();
        if (isolate != null) {
            return isolate.properties;
        }
        return null;
    }
}
