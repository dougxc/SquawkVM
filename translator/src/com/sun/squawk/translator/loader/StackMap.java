
package com.sun.squawk.translator.loader;
import  com.sun.squawk.util.*;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;

import java.io.IOException;

public class StackMap extends BaseFunctions implements RuntimeConstants {

    public final static int STACK          = 0;
    public final static int LOCALS         = 1;
    public final static int PHYSICALLOCALS = 2;

    public static IntHashtable ZEROTABLE = new IntHashtable(0);

    private Method method;

   /**
    * Targets
    */
    private IntHashtable targets;

    private int longs = 0;


/*==============================================================================
 * Constructors
 *============================================================================*/

    /**
     * constructor: create a new field table with
     * the desired number of entries.
     */
    public StackMap(ClassFileInputStream in, ConstantPool pool, Method method) throws LinkageException {
        try {
            this.method  = method;
            int nmaps  = in.readUnsignedShort("map-nmaps");
            if (nmaps == 0) {
                targets = ZEROTABLE;
            } else {
                targets = new IntHashtable(nmaps);
            }
            int lastAddress = -1;
            for (int i = 0 ; i < nmaps ; i++) {
                int address = in.readUnsignedShort("map-address");
                if (address <= lastAddress) {
                    in.verificationException("Stackmap ip addresses not in order. address="+
                                                         address+" lastAddress="+lastAddress);
                }
               /*
                * An array of three arrays of Type's are produced
                *
                * [0] is an array of stack types that are in logical offsets
                * [1] is an array of local types that are in logical offsets
                * [2] is an array of local types that are in physical offsets
                */

                longs = 0;
                Type[][] entry = new Type[3][];
                Type[] locals = loadStackMapList(in, pool, method); // locals
                entry[LOCALS] = locals;
                int localLongs = longs;

                Type[] stack  = loadStackMapList(in, pool, null); // stack
                entry[STACK]  = stack;

                if (localLongs == 0) {
                    entry[PHYSICALLOCALS] = locals; // Physical = logical
                } else {
                    Type [] physicalLocals = new Type[locals.length+localLongs];
                    int k = 0;
                    for (int j = 0 ; j < locals.length ; j++) {
                        assume(locals[j] != null);
                        physicalLocals[k] = locals[j];
                        if (physicalLocals[k] == Type.DOUBLE) {
                            physicalLocals[++k] = Type.DOUBLE2;
                        }
                        if (physicalLocals[k] == Type.LONG) {
                            physicalLocals[++k] = Type.LONG2;
                        }
                        k++;
                    }
                    assume(k == physicalLocals.length);
                    entry[PHYSICALLOCALS] = physicalLocals;
                }

                targets.put(address, new Target(address, entry));
            }
        } catch (IOException ioe) {
ioe.printStackTrace();
            in.classFormatException("badly formed StackMap");
        }
    }

/*==============================================================================
 * Accessor methods
 *============================================================================*/

   /**
    * size: get the total number of stack map entries
    */
    public int size() {
        return targets.size();
    }

   /**
    * Get the stackmap entry for an ip addressz
    */
    public Target lookup(int ip) {
        return (Target)targets.get(ip);
    }

    public IntHashtable getTargets() {
        return targets;
    }

   /*
    * Return a target that can be used as the exception target for a finally
    * block that was created be the graph builder to implement a synchronized method.
    */
    public static Target getFinalTarget() {
        Type[][] entry = new Type[3][];
        Type[] stack  = new Type[1];
        stack[0] = Type.THROWABLE;
        entry[STACK] = stack;
        Type[] locals = new Type[1];
        locals[0] = Type.OBJECT;
        entry[LOCALS] = locals;
        entry[PHYSICALLOCALS] = locals;
        Target finalTarget = new Target(9999999, entry);
        finalTarget.setExceptionTargetType(Type.THROWABLE);
        return finalTarget;
    }


   /**
    *  Load stackmap list
    */
    private Type[] loadStackMapList(ClassFileInputStream in, ConstantPool pool, Method method) throws LinkageException {
        try {
            int items = in.readUnsignedShort("map-items");
            Type[] list;
            int j = 0;
            if (method != null && method.isStatic()) {
                list = new Type[++items];
                list[j++] = method.parent(); // receiver for static methods
            } else {
                list = new Type[items];
            }
            for (; j < items ; j++) {
                byte type = in.readByte("map-type");
                switch (type) {
                    case ITEM_Bogus:        list[j] = Type.BOGUS;                      break;
                    case ITEM_Integer:      list[j] = Type.INT;                        break;
                    case ITEM_Long:         list[j] = Type.LONG;       longs++;        break;
                    case ITEM_Float:        list[j] = Type.FLOAT;                      break;
                    case ITEM_Double:       list[j] = Type.DOUBLE;     longs++;        break;
                    case ITEM_Null:         list[j] = Type.NULLOBJECT;                 break;
                    case ITEM_InitObject:   list[j] = Type.INITOBJECT;                 break;

                    case ITEM_Object: {
                        int classIndex = in.readUnsignedShort("map-ITEM_Object");
                        list[j] = pool.getType(classIndex);
                        break;
                    }
                    case ITEM_NewObject: {
                        int ipIndex = in.readUnsignedShort("map-ITEM_NewObject");
                        list[j] = addNewType(ipIndex);
                        break;
                    }
                    default: {
                        throw fatal("Bad item "+type);
                    }
                }
            }
            return list;
        } catch (IOException ioe) {
ioe.printStackTrace();
            in.classFormatException("badly formed StackMap");
        }
        return null;
    }



   /**
    * Create a temporory type that will represent the result of a "new" instruction
    * This is interned here in order to avoid having it in the VM's interned type
    * hashtable where it will just take up space when the verification is over.
    */
    public TypeProxy addNewType(int ip) throws LinkageException {
        TypeProxy type = (TypeProxy)internedTypes.get(ip);
        if (type == null) {
            type = TypeProxy.createForMap(null, "ip proxy "+ip);
            type.setSuperType(Type.NEWOBJECT);
            internedTypes.put(ip, type);
        }
        return type;
    }

   /**
    * Return a new type if one exists for the ip specifed
    */
    public TypeProxy findNewType(int ip, Type proxy) throws LinkageException {
        return (TypeProxy)internedTypes.get(ip);
    }

    private IntHashtable internedTypes = ZEROTABLE;

}
