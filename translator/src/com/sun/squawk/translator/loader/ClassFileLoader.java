
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Enumeration;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.*;

public class ClassFileLoader extends BaseFunctions implements RuntimeConstants, com.sun.squawk.vm.ClassNumbers {

   /**
    * Static constructor
    */
    public static ClassFileLoader create(VirtualMachine vm, String classpath) throws LinkageException {
        return new ClassFileLoader(vm, classpath);
    }

   /**
    * The virtual machine for this class loader
    */
    private VirtualMachine vm;

   /**
    * Hashtable for debugging only
    */
    private ArrayHashtable checkLoaded;

   /**
    * Classpath connection
    */
    private ClasspathConnection classpath;

   /**
    * Constructor
    */
    private ClassFileLoader(VirtualMachine vm, String classpath) throws LinkageException {
        this.vm = vm;
        try {
            this.classpath = (ClasspathConnection)Connector.open("classpath://"+classpath);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException("invalid classpath");
        }
    }

   /**
    * The loader's main function
    */
    public Type load(String fileName) throws LinkageException {

       /*
        * Transform parameter
        *
        *   "Ljava/foo/Bar;" -> "java/foo/Bar"
        *
        *   "java.foo.Bar" -> "java/foo/Bar"
        */
        if (fileName.charAt(fileName.length() - 1) == ';') {
            assume(fileName.charAt(0) == 'L');
            fileName = fileName.substring(1, fileName.length() - 1);
        } else {
            fileName = fileName.replace('.', '/');
        }

        InputStream is = null;
        try {
            is = classpath.openInputStream(fileName + ".class");
            return load(fileName, is);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new LinkageException(Type.NOCLASSDEFFOUNDERROR, "NoClassDefFound: " + fileName);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

   /**
    * The loader's main function
    */
    public Type load(String fileName, InputStream is) throws LinkageException {

       /*
        * Write trace message
        */
        trace(vm.traceloading(fileName), "Loading class " + fileName);

        assume(fileName.indexOf('\\') == -1);


        if (DEBUG) {
            if (checkLoaded == null) {
                checkLoaded = new ArrayHashtable();
            }
            assume(checkLoaded.get(fileName) == null, fileName);
            checkLoaded.put(fileName, fileName);
        }

       /*
        * Wrap the input stream int a ClassFileInputStream
        */
        ClassFileInputStream in = new ClassFileInputStream(is,  fileName, vm);

        try {
           /*
            * Set trace if requested
            */
            in.setTrace(vm.tracepool(fileName));

           /*
            * Read the magic values
            */
            loadMagicValues(in);

           /*
            * Read the constant pool
            */
            ConstantPool pool = loadConstantPool(in);

           /*
            * Read the class information
            */
            Type type = loadClassInfo(in, pool);

           /*
            * Mark type as being loaded
            */
            type.loadingStarted();

           /*
            * Read the interface definitions
            */
            loadInterfaces(in, pool, type);

           /*
            * Trace
            */
            String classOrInterface = type.isInterface() ? "interface " : "class ";
            trace(vm.tracefields(type.name()), "\n"+classOrInterface+type.name()+"        (extends "+type.superType().name()+")");
            traceInterfaces(type);

           /*
            * Read the field definitions
            */
            loadFields(in, pool, type);

           /*
            * Read the method definitions
            */
            loadMethods(in, pool, type);

           /*
            * Workout which methods can be called from an invokeinterface
            */
            resolveInterfaces(in, pool, type);

           /*
            * Read the extra attributes
            */
            loadExtraAttributes(in, pool, type);

           /*
            * Close the input stream
            */
            in.close();

           /*
            * Trace
            */
            trace(vm.traceloading(fileName) && vm.verbose(), "Finshed Loading class " + fileName);

           /*
            * Mark type as loaded
            */
            type.loadingFinished();

           /*
            * Return the new type
            */
            return type;
        } catch (IOException ioe) {
ioe.printStackTrace();
            in.classFormatException(fileName);
        }
        return null;
    }


   /**
    * Load the magic values
    */
    private void loadMagicValues(ClassFileInputStream in) throws IOException, LinkageException {
        int magic = in.readInt("magic");
        int minor = in.readUnsignedShort("minor");
        int major = in.readUnsignedShort("magor");
        if (magic != 0xCAFEBABE) {
            in.classFormatException("Bad magic value");
        }
        // Support JDK1.3 and 1.4 classfiles
        if (!((major == 45 && minor == 3) || (major == 46 && minor == 0))) {
            in.classFormatException("Bad class file version number: " + major + ":" + minor);
        }
    }

   /**
    *  Load the constant pool
    */
    private ConstantPool loadConstantPool(ClassFileInputStream in) throws IOException, LinkageException {
        return ConstantPool.create(vm, in);
    }

   /**
    *  Load the class information
    */
    private Type loadClassInfo(ClassFileInputStream in, ConstantPool pool) throws IOException, LinkageException {
        int accessFlags = in.readUnsignedShort("cls-flags");
        int classIndex  = in.readUnsignedShort("cls-index");
        int superIndex  = in.readUnsignedShort("cls-super index");

       /*
        * Loading the constant pool will have created the Type object.
        */
        Type superType = null;
        Type type      = pool.bootstrapType(classIndex);
        if (type.getState() == Type.LOADING) {
            in.linkageException(Type.CLASSCIRCULARITYERROR, type.name());
        }
        if (superIndex != 0) {
            superType = pool.getType(superIndex);
        }
        else
        if (type != Type.OBJECT) {
            in.linkageException(Type.CLASSFORMATERROR, "non Object class must have super-type");
        }

        if (type == null) {
            in.classFormatException("invalid this_class entry");
        }

       /*
        * Fill in just what is known about the type thus far
        */
        type.setSuperType(superType);
        type.setAccessFlags(accessFlags);

        return type;
    }

   /**
    *  Load the class's interfaces
    */
    private void loadInterfaces(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {
        int count = in.readUnsignedShort("i/f-count");
        if (count > 0) {

           /*
            * Temporary vector to hold all the interfaces
            */
            Vector interfaces = new Vector(count);

           /*
            * Include in the interfaces table for this type all the interfaces specified
            * in the class file plus all the interfaces implemented by those interfaces.
            * Because this is recersive all the possible interfaces implemented by this class
            * (except those further up the hierarchy) will be directly included in the list.
            */
            for (int i = 0 ; i < count ; i++) {
                Type iface = pool.getType(in.readUnsignedShort("i/f-index"));
                if (interfaces.indexOf(iface) < 0) {
                    interfaces.addElement(iface);
                    Type[] subInterfaces = iface.getInterfaces();
                    for (int k = 0 ; k < subInterfaces.length ; k++) {
                        Type sface = subInterfaces[k];
                        if (interfaces.indexOf(sface) < 0) {
                            interfaces.addElement(sface);
                        }
                    }
                }
            }

           /*
            * Make into a Type[] of all the above
            */
            Type[] interfaces2 = new Type[interfaces.size()];

            int j = 0;
            for(Enumeration e = interfaces.elements() ; e.hasMoreElements() ;) {
                 Type iface = (Type)e.nextElement();
                 interfaces2[j++] = iface;
            }

            assume(j == interfaces2.length);

           /*
            * Set in the type
            */
            type.setInterfaces(interfaces2);
        }
    }


   /**
    *  Trace the class's interfaces
    */
    private void traceInterfaces(Type type) {
        Type[] interfaces = type.getInterfaces();
        for (int i = 0 ; i < interfaces.length ; i++) {
            trace(vm.tracefields(type.name()), "    Implements\t"+interfaces[i].name());
        }
    }


   /**
    *  Load the class's fields
    */
    private void loadFields(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {

       /*
        * The number of words that each instance of this class will need for instance data.
        */
        int instanceFieldCount = type.superType().getInstanceFieldCount();
        int staticFieldCount = 0;

       /*
        * Get count of fields
        */
        int count = in.readUnsignedShort("fld-count");
        if (count == 0) {
            type.setFields(VirtualMachine.ZEROFIELDS, instanceFieldCount, 0);
            return;
        }

       /*
        * Allocate the field table
        */
        Field[] fields = new Field[count];

        /*
         * Read in all the fields
         */
        for (int i = 0; i < count; i++) {
            int accessFlags     = in.readUnsignedShort("fld-flags");
            int nameIndex       = in.readUnsignedShort("fld-nameIndex");
            int descriptorIndex = in.readUnsignedShort("fld-descIndex");
            int attributesCount = in.readUnsignedShort("fld-AttbCount");
            int slot            = -1;
            int initValueIndex  = 0;

            String fieldName = pool.getUtf8Interning(nameIndex);
            String fieldSig  = pool.getUtf8(descriptorIndex);

           /*
            * Process the field's attruibutes
            */
            for (int j = 0; j < attributesCount; j++) {
                int    attributeNameIndex = in.readUnsignedShort("fld-att-nameIndex");
                int    attributeLength    = in.readInt("fld-att-length");
                String attributeName      = pool.getUtf8(attributeNameIndex);

                if (attributeName.equals("ConstantValue")) {
                    if (attributeLength != 2) {
                        in.classFormatException("length of ConstantValue attribute is not 2");
                    }
                    if (initValueIndex != 0) {
                        in.classFormatException("more than one ConstantValue attribute");
                    }
                    if ((accessFlags & ACC_STATIC) == 0) {
                        in.classFormatException("ConstantValue attribute for non-static field " + fieldName);
                    }
                    initValueIndex = in.readUnsignedShort("fld-ConstantValue"); // Get the variable initialzation value
                } else {
                    while (attributeLength-- > 0) {
                        in.readByte(); // Ignore this attribute
                    }
                }
            }

            Field field = Field.create(vm, in, type, fieldName, fieldSig, accessFlags);

            Object initValue = null;
            if (initValueIndex != 0) {
                /*
                 * Verify that the initial value is of the right type for the field
                 */
                switch (field.type().getID()) {
                    case CNO_long   : initValue = pool.getEntry(initValueIndex, CONSTANT_Long);   break;
                    case CNO_float  : initValue = pool.getEntry(initValueIndex, CONSTANT_Float);  break;
                    case CNO_double : initValue = pool.getEntry(initValueIndex, CONSTANT_Double); break;
                    case CNO_int :
                    case CNO_short:
                    case CNO_char:
                    case CNO_byte:
                    case CNO_boolean: initValue = pool.getEntry(initValueIndex, CONSTANT_Integer); break;
                    case CNO_String:  initValue = pool.getEntry(initValueIndex, CONSTANT_String);  break;
                    default: in.classFormatException("ConstantValue for field of type: " + field.type().name());
                }
            }

            if (!field.isStatic()) {
               /*
                * Allocate an instance variable
                */
                slot = instanceFieldCount++;
            } else {

               /*
                * Allocate a static variable (but not if its a constant, unless it is a string constant)
                */
                if (initValue == null || initValue instanceof String || !field.isFinal()) {
                    slot = staticFieldCount++;
                }
            }

            field.setSlot(slot, initValue);
            fields[i] = field;

            if (vm.tracefields(type.name()+"::"+fieldName+fieldSig)) {
                String slotstr = slot == -1           ? "   " : "["+slot+"]";
                String initstr = initValue == null ? "" : " \t(init="+initValue+")";
                String fieldStr = (field.isStatic()) ? "    StaticField" : "Field";
                trace(true, fieldStr+slotstr+"\t"+fieldName+" "+fieldSig+initstr);
            }
        }

        type.setFields(fields, instanceFieldCount, staticFieldCount);
    }

   /**
    *  Load the class's methods
    */
    private void loadMethods(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {

       /*
        * Setup nextSlot. If this is an interface and the super type is Object then
        * nextSlot is set to the next unused interface number. Otherwize the super
        * type's method table size if the next starting place/
        */

        int nextSlot;
        if (type.isInterface()/* && type.superType() == Type.OBJECT*/) {
            assume(type.superType() == Type.OBJECT);
            nextSlot = vm.nextInterfaceMethod();
        } else {
            nextSlot = type.superType().getMethodTableSize();
            if (nextSlot == 0) {
               nextSlot = SLOT_init;
            }
        }

       /*
        * Get count of methods and exit if there are none
        */
        int count = in.readUnsignedShort("mth-count");
        if (count == 0) {
            type.setMethods(VirtualMachine.ZEROMETHODS, nextSlot);
            return;
        }

       /*
        * In this routine statics start at 20000 so they will sort after the instance methods
        */
        int nextStatic = 20000;

       /*
        * Flag to say if <clinit> was found
        */
        boolean sawClinit = false;

       /*
        * Allocate the method vector
        */
        MethodVector methods = new MethodVector(count);

       /*
        * Read in all the methods
        */
        for (int i = 0; i < count; i++) {
            int accessFlags     = in.readUnsignedShort("mth-flags");
            int nameIndex       = in.readUnsignedShort("mth-nameIndex");
            int descriptorIndex = in.readUnsignedShort("mth-descIndex");
            int attributesCount = in.readUnsignedShort("mth-AttbCount");

            String methodName = pool.getUtf8Interning(nameIndex);
            String methodSig  = pool.getUtf8(descriptorIndex);

            boolean vtableForce = false;


           /*
            * If this a <clinit> method then rename it to be _SQUAWK_INTERNAL_clinit
            */
            if (methodName == VirtualMachine.CLINIT) {
                methodName =  VirtualMachine.SQUAWK_CLINIT;
                vtableForce = true;
            }

           /*
            * If this is an <init> method then rename it to be _SQUAWK_INTERNAL_init if the
            * type is not java.lang.Object. If it is java.lang.Object then rename
            * it to be _SQUAWK_INTERNAL_dummy.
            */
            if (methodName == VirtualMachine.INIT) {
                if (type == Type.OBJECT) {
                    methodName = VirtualMachine.SQUAWK_DUMMY;
                } else {
                    methodName = VirtualMachine.SQUAWK_INIT;
                }
                assume((accessFlags & ACC_STATIC) == 0);
            }

           /*
            * If the method is "main" then rename it to be _SQUAWK_INTERNAL_main
            */
            if (methodName == VirtualMachine.MAIN) {
                methodName =  VirtualMachine.SQUAWK_MAIN;
                vtableForce = true;
            }

           /*
            * Get the method structure
            */
            Method method = Method.create(vm, in, type, methodName, methodSig, accessFlags);

           /*
            * Process the method's attruibutes
            */
            for (int j = 0; j < attributesCount; j++) {
                int    attributeNameIndex = in.readUnsignedShort("mth-att-nameIndex");
                int    attributeLength    = in.readInt("mth-att-length");
                String attributeName      = pool.getUtf8(attributeNameIndex);

                if (attributeName.equals("Code")) {
                    loadMethodCode(in, pool, method, attributeLength);
                } else {
                    while (attributeLength-- > 0) {
                        in.readByte(); // Ignore this attribute
                    }
                }
            }

            assume((methodName == VirtualMachine.INIT)   == methodName.equals("<init>"));
            assume((methodName == VirtualMachine.CLINIT) == methodName.equals("<clinit>"));


            if (methodName == VirtualMachine.SQUAWK_DUMMY) {
                /* Just forget this one */
            } else if (methodName == VirtualMachine.SQUAWK_CLINIT) {
                /*
                 * Don't put <clinit> into method array. This is only necexxary because interface classes
                 * can have a <clinit> and it is not possible to mix interface methods and regular mathods in
                 * the one method array.
                 */
                method.setSlotOffset(SLOT_clinit);
                type.setClinit(method);
            } else {
                if (method.isStatic() & !vtableForce) {
                    /*
                     * Statics get slot numbers starting from 20000 (for now)
                     */
                    method.setSlotOffset(nextStatic++);

                } else {
                   /*
                    * Look for this method in the supertype,
                    */
                    Method smethod = null;
                    if (type.superType() != Type.UNIVERSE) {
                        smethod = type.superType().findMethod(methodName, method.getParms());
                        if (smethod != null && smethod.isStatic() != method.isStatic() & !vtableForce) {
                            smethod = null; // dont match a virtual method with a static method
                        }
                    }
                    if (smethod != null) {
                        method.setSlotOffset(smethod.getSlotOffset());
                    } else {
                        if (type.isInterface()) {
                            method.setSlotOffset(vm.allocateInterfaceMethod());
                            nextSlot++;
                        } else {
                            method.setSlotOffset(nextSlot++);
                        }
                    }
                }
                methods.addElement(method);
            }
        }

       /*
        * Throw away all the methods (such as <init>) of primitive classes
        * because they are not needed and will not verify.
        */
        if (type.isPrimitive() || methods.size() == 0) {
            return;
        }

       /*
        * Sort the methods by slot number
        */
        Object[] sortedMethods = methods.sorted();

       /*
        * Get the slot number of the first entry.
        */
        int firstSlot = ((Method)sortedMethods[0]).getSlotOffset();

       /*
        * If there are no virtual methods then
        */
        if (firstSlot >= 20000) {
            firstSlot = nextSlot;
        }

//prtn("firstSlot="+firstSlot);
//prtn("nextSlot="+nextSlot);
//prtn("nextStatic="+nextStatic);
//prtn("size="+((nextSlot-firstSlot)+(nextStatic-20000)));

        //assume(!((Method)sortedMethods[0]).isStatic(), "m="+(Method)sortedMethods[0]);



       /*
        * Allocate a method array large enough to hold the instance range from
        * the lowest slot number defined in this method to the highest, plus
        * all the static methods which come afterwards.
        */
        Method[] allMethods = new Method[(nextSlot-firstSlot)+(nextStatic-20000)];

       /*
        * Go through the method vector filling in the method array
        */
        for (int i = 0 ; i < sortedMethods.length ; i++) {
            Method m = (Method)sortedMethods[i];

            if (m == null) {
                continue;
            }

           /*
            * If the slot number is 30000 then this is <clinit> which is
            * not wanted and must also be the last entry.
            */
            int slotOffset = m.getSlotOffset();
            if (slotOffset == 30000) {
                break;
            }

            if (slotOffset < 20000) {
               /*
                * These are instance methods. Put them into the method array at
                * their slot number offset (relative to the starting point of the
                * method array).
                */
                slotOffset -= firstSlot;
                assume(slotOffset < allMethods.length,"slotOffset="+slotOffset);
                assume(allMethods[slotOffset] == null, "slot = "+allMethods[slotOffset]+" m = "+m+" slotOffset = "+slotOffset+" type = "+type+" firstSlot="+firstSlot);
                allMethods[slotOffset] = m;
            } else {
               /*
                * These are static methods. Put them at the end using the next
                * available slot numbers.
                */
                assume(!type.isInterface());
                slotOffset = ((slotOffset - 20000) + nextSlot) - firstSlot;
                assume(slotOffset < allMethods.length,"slotOffset="+slotOffset);
                assume(allMethods[slotOffset] == null);
                m.setSlotOffset(slotOffset+firstSlot);
                allMethods[slotOffset] = m;
            }
        }

       /*
        * Now fill in the other entries
        */
        for (int i = 0 ; i < allMethods.length ; i++) {
            if (allMethods[i] == null) {
                Method m = type.superType().findMethod(firstSlot+i);
                assume(m != null);
                allMethods[i] = m;
            }
        }

       /*
        * Trace
        */
        trace(sawClinit && vm.tracefields(type.name()+"<clinit>"), "    Method[-]\t<clinit>");

        for (int i = 0 ; i < allMethods.length ; i++) {
            Method m = allMethods[i];
            int slot = (m != null) ? m.getSlotOffset() : i;
            String slotString = (m.isStatic()) ? ""+slot+"s" : ""+slot;
            String verbose = vm.verbose() ? ""+m.parent()+"::" : "";
            trace(vm.tracefields(type.name()+"::"+m), "    Method["+slotString+"]\t"+verbose+m);
        }

        type.setMethods(allMethods, nextSlot);
    }

//static int totalCode=0;
//static int methodCount=0;

   /**
    *  Load the method's code
    */
    private void loadMethodCode(ClassFileInputStream in, ConstantPool pool, Method method, int attributeLengthXX) throws IOException, LinkageException {
        int maxStack   = in.readUnsignedShort("cod-maxStack");  // Maximum stack need
        int maxLocals  = in.readUnsignedShort("cod-maxLocals"); // Max locals need
        int codeLength = in.readInt("cod-length");              // Length of the bytecode array
        StackMap   map = null;

       /*
        * Read the bytecodes into a buffer. The GraphBuilder needs to know
        * about the exception handlers and stack maps which come after the
        * bytecodes.
        */
        byte[] bytecodes = new byte[codeLength];
        in.readFully(bytecodes);

//totalCode += codeLength;
//methodCount++;
//System.out.println("totalCode = "+totalCode+" methodCount="+methodCount);
//System.out.println("+++"+method.parent()+"::"+method+" = "+methodCount);

        BytecodeHolder holder = new BytecodeHolder(method, pool, bytecodes, maxStack, maxLocals);

       /*
        * Read in the exception handlers
        */
        int handlers = in.readShort("hnd-handlers");
        if (handlers > 0) {
            holder.setHandlerCount(handlers);
            for (int i = 0; i < handlers; i++) {
                char startPC    = in.readChar("hnd-startPC");               // Code range where handler is valid
                char endPC      = in.readChar("hnd-endPC");                 // (as offsets within bytecode)
                char handlerPC  = in.readChar("hnd-handlerPC");             // Offset to handler code
                char catchIndex = in.readChar("hnd-catchIndex");            // Exception (constant pool index)

               /*
                * Check that all the pc addresses look reasionable
                */
                if (startPC >= codeLength || endPC >= codeLength || startPC >= endPC || handlerPC >= codeLength) {
                    in.classFormatException("Bad exception handler found");
                }

               /*
                * Set in methid structure
                */
                holder.setHandler(i, startPC, endPC, handlerPC, catchIndex);
            }
        }

       /*
        * Read in the code attributes
        */
        int attributesCount = in.readUnsignedShort("cod-attributesCount");
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = in.readUnsignedShort("cod-attributeNameIndex");
            int attributeLength    = in.readInt("cod-attributeLength");
            String attributeName   = pool.getUtf8(attributeNameIndex);
            if (attributeName.equals("StackMap")) {
                byte[] mapdata = new byte[attributeLength];
                in.readFully(mapdata);
                holder.setMapData(mapdata);
            } else if(attributeName.equals("LineNumberTable")) {
                int lineNumberTableLength = in.readUnsignedShort("lin-lineNumberTableLength") * 2;
                char[] lines = new char[lineNumberTableLength];
                for (int k = 0 ; k < lineNumberTableLength ; ) {
                    lines[k++] = (char)in.readUnsignedShort("lin-startPC");
                    lines[k++] = (char)in.readUnsignedShort("lin-lineNumber");
                }
                holder.setLineTable(lines);
            } else if(attributeName.equals("LocalVariableTable")) {
                while (attributeLength-- > 0) {
                    in.readByte(); // Ignore this attribute
                }
            } else {
                prtn("ignored attributeName="+attributeName);
                while (attributeLength-- > 0) {
                    in.readByte(); // Ignore this attribute
                }
            }

        }

        method.setHolder(holder);
    }


   /**
    *  Workout which methods can be called from an invokeinterface
    */
    private void resolveInterfaces(ClassFileInputStream in, ConstantPool pool, Type toptype) throws IOException, LinkageException {
        Type type = toptype;
        if (!type.isInterface()) {

           /*
            * Get all the interface methods that this type implements
            */
            MethodVector interfaceMethods = new MethodVector(type.getInterfaces().length);
            while (type != Type.UNIVERSE) {
                Type[] interfaces = type.getInterfaces();
                for (int i = 0 ; i < interfaces.length ; i++) {
                    Type interfaceType = interfaces[i];
                    Method[] methods = interfaceType.getMethods();
                    for (int j = 0 ; j < methods.length ; j++) {
                        Method imethod = methods[j];
                        //trace(vm.tracefields(toptype.name()), "    Interface "+ourMethod.name()+" is "+imethod.getSlotOffset());

                       /*
                        * Add interface method to list (but don't add it more than once).
                        */
                        int n = interfaceMethods.indexOf(imethod);
                        if (n < 0) {
                            interfaceMethods.addElement(imethod);
                        }
                    }

                }
                type = type.superType();
            }

           /*
            * Sort the interface methods into order
            */
            interfaceMethods.trimToSize();
            Object[] sortedMethods = interfaceMethods.sorted();

            if (sortedMethods.length > 0) {
               /*
                * Get the inteface numbers for the first and last entries
                */
                int firstInterface = ((Method)sortedMethods[0]).getSlotOffset();
                int lastInterface  = ((Method)sortedMethods[sortedMethods.length - 1]).getSlotOffset();

//prtn("firstInterface="+firstInterface);
//prtn("lastInterface="+lastInterface);

               /*
                * Allocate an array of shorts that is large enough to hold all the interface entries
                * and an extra entry.
                */
                short[] interfaceTable = new short[1+(lastInterface-firstInterface)+1];

               /*
                * Make the first entry the number of the first interface
                */
                interfaceTable[0] = (short)firstInterface;
                for (int j = 0 ; j < sortedMethods.length ; j++) {
                    Method imethod = (Method)sortedMethods[j];
                    assume(!imethod.name().equals("<clinit>"));
                    Method ourMethod = toptype.findMethod(imethod.name(), imethod.getParms());
                    if (ourMethod == null) {
                        in.linkageException(Type.ABSTRACTMETHODERROR, "Failure to find method "+imethod.name()+" in "+toptype);
                    }
                    interfaceTable[(imethod.getSlotOffset() - firstInterface)+1] = (short)ourMethod.getSlotOffset();
                }

                toptype.setInterfaceTable(interfaceTable);

               /*
                * Trace
                */
                int first = interfaceTable[0];
//prtn("first="+first);

                for (int i = 1 ; i < interfaceTable.length ; i++) {
                    Method entry = null;
                    String entryName = null;
                    if (interfaceTable[i] != 0) {
                        entry = toptype.findMethod(interfaceTable[i]);
                        entryName = entry.name();
                    }
                    if (entryName != null) {
                        entryName = "Method[" + entry.getSlotOffset() + "] "+entryName;
                    }
                    trace(vm.tracefields(toptype.name()), "    Interface[" + (first+i-1) + "] = " + entryName);
                }
            }
        }
    }


   /**
    *  Load the class's other attributes
    */
    private void loadExtraAttributes(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {
        int attributesCount = in.readUnsignedShort("ex-count");
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = in.readUnsignedShort("ex-index");
            String attributeName   = pool.getUtf8(attributeNameIndex);
            int attributeLength    = in.readInt("ex-length");
            if(attributeName.equals("SourceFile")) {
                int index = in.readUnsignedShort("sourcefile-index");
                type.setSourceFile(pool.getUtf8(index));
            } else {
                prtn("ignored attributeName="+attributeName);
                while (attributeLength-- > 0) {
                    in.readByte(); // Ignore this attribute
                }
            }
        }
    }

}
