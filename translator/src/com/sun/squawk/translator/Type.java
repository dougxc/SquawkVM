package com.sun.squawk.translator;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;

import java.io.*;
import java.util.*;

public class Type extends BaseFunctions implements RuntimeConstants, com.sun.squawk.vm.ClassNumbers {

   /* ------------------------------------------------------------------------ *\
    *                         Manifest class definitions                       *
   \* ------------------------------------------------------------------------ */

   /*
    * The route of all data types
    */
    public static Type UNIVERSE;

   /*
    * Primitive data types
    */
    public static Type PRIMITIVE, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, VOID, GLOBALENTRY, LOCALENTRY;

   /*
    * Objects
    */
    public static Type OBJECT, NATIVE, MATH, STRING, CLASS, CLASSBASE, THREAD, ISOLATE, MONITOR, SYSTEM, STRINGBUFFER, TEST, VMPLATFORM;

   /*
    * Special objects used by the verifier
    */
    public static Type BOGUS, NULLOBJECT, INITOBJECT, NEWOBJECT, LONG2, DOUBLE2;

   /*
    * Throwables
    */
    public static Type THROWABLE, ERROR, EXCEPTION, NOMEMORYERROR;

   /*
    * Arrays
    */
    public static Type BOOLEAN_ARRAY, BYTE_ARRAY,  CHAR_ARRAY,   SHORT_ARRAY,  INT_ARRAY,
                       LONG_ARRAY,    FLOAT_ARRAY, DOUBLE_ARRAY, OBJECT_ARRAY, STRING_ARRAY,
                       CLASSBASE_ARRAY, CLASS_ARRAY, GLOBAL_ARRAY,  LOCAL_ARRAY, BYTE_ARRAY2D,
                       THREAD_ARRAY;

   /**
    * Class loading errors
    */
    public static Type LINKAGEERROR,
                           VERIFYERROR,
                           CLASSFORMATERROR,
                           NOCLASSDEFFOUNDERROR,
                           CLASSCIRCULARITYERROR,
                           INCOMPATIBLECLASSCHANGEERROR,
                               ABSTRACTMETHODERROR,
                               ILLEGALACCESSERROR,
                               INSTANTIATIONERROR,
                               NOSUCHFIELDERROR,
                               NOSUCHMETHODERROR;

   /**
    * Initialize this class
    */
    static void initialize(VirtualMachine vm) {

       /*
        * The route of all data types
        */
        UNIVERSE        = vm.createType(null,         "-U-");

       /*
        * Unreal data types
        */
        BOGUS           = vm.createType(UNIVERSE,     "-X-");         // An invalid type
        LONG2           = vm.createType(UNIVERSE,     "-J2-");        // Second word of a long
        DOUBLE2         = vm.createType(UNIVERSE,     "-D2-");        // Second word of a double

       /*
        * Objects
        */
        OBJECT          = vm.createType(UNIVERSE,     "Ljava/lang/Object;");        assume(OBJECT.getID()       == CNO_Object);
        CLASSBASE       = vm.createType(OBJECT,       "Ljava/lang/ClassBase;");     assume(CLASSBASE.getID()    == CNO_ClassBase);
        VMPLATFORM      = vm.createType(OBJECT,       "Ljava/lang/VMPlatform;");    assume(VMPLATFORM.getID()   == CNO_VMPlatform);
        CLASS           = vm.createType(CLASSBASE,    "Ljava/lang/Class;");         assume(CLASS.getID()        == CNO_Class);
        NATIVE          = vm.createType(OBJECT,       "Ljava/lang/Native;");        assume(NATIVE.getID()       == CNO_Native);
        MATH            = vm.createType(OBJECT,       "Ljava/lang/Math;");          assume(MATH.getID()         == CNO_Math);
        STRING          = vm.createType(OBJECT,       "Ljava/lang/String;");        assume(STRING.getID()       == CNO_String);
        THREAD          = vm.createType(OBJECT,       "Ljava/lang/Thread;");        assume(THREAD.getID()       == CNO_Thread);
        ISOLATE         = vm.createType(ISOLATE,      "Ljava/lang/Isolate;");       assume(ISOLATE.getID()      == CNO_Isolate);
        MONITOR         = vm.createType(OBJECT,       "Ljava/lang/Monitor;");       assume(MONITOR.getID()      == CNO_Monitor);
        SYSTEM          = vm.createType(OBJECT,       "Ljava/lang/System;");        assume(SYSTEM.getID()       == CNO_System);
        STRINGBUFFER    = vm.createType(OBJECT,       "Ljava/lang/StringBuffer;");  assume(STRINGBUFFER.getID() == CNO_StringBuffer);
        TEST            = vm.createType(THROWABLE,    "Ljava/lang/Test;");          assume(TEST.getID()         == CNO_Test);


       /*
        * Primitive data types
        */
        PRIMITIVE       = vm.createType(OBJECT,       "Ljava/lang/_primitive_;");   assume(PRIMITIVE.getID()    == CNO_primitive);
        VOID            = vm.createType(PRIMITIVE,    "Ljava/lang/_void_;");        assume(VOID.getID()         == CNO_void);
        INT             = vm.createType(PRIMITIVE,    "Ljava/lang/_int_;");         assume(INT.getID()          == CNO_int);
        LONG            = vm.createType(PRIMITIVE,    "Ljava/lang/_long_;");        assume(LONG.getID()         == CNO_long);
        FLOAT           = vm.createType(PRIMITIVE,    "Ljava/lang/_float_;");       assume(FLOAT.getID()        == CNO_float);
        DOUBLE          = vm.createType(PRIMITIVE,    "Ljava/lang/_double_;");      assume(DOUBLE.getID()       == CNO_double);
        BOOLEAN         = vm.createType(INT,          "Ljava/lang/_boolean_;");     assume(BOOLEAN.getID()      == CNO_boolean);
        CHAR            = vm.createType(INT,          "Ljava/lang/_char_;");        assume(CHAR.getID()         == CNO_char);
        SHORT           = vm.createType(INT,          "Ljava/lang/_short_;");       assume(SHORT.getID()        == CNO_short);
        BYTE            = vm.createType(INT,          "Ljava/lang/_byte_;");        assume(BYTE.getID()         == CNO_byte);
        GLOBALENTRY     = vm.createType(PRIMITIVE,    "Ljava/lang/_global_;");      assume(GLOBALENTRY.getID()  == CNO_global);
        LOCALENTRY      = vm.createType(PRIMITIVE,    "Ljava/lang/_local_;");       assume(LOCALENTRY.getID()   == CNO_local);

       /*
        * Special objects used by the verifier
        */
        NULLOBJECT      = vm.createType(OBJECT,       "-NULL-");      // Result of an acoust_null
        INITOBJECT      = vm.createType(OBJECT,       "-INIT-");      // "this" in <init> before call to super()
        NEWOBJECT       = vm.createType(OBJECT,       "-NEW-");       // Result of "new" before call to <init>

       /*
        * Throwables
        */
        THROWABLE       = vm.createType(OBJECT,       "Ljava/lang/Throwable;");     assume(THROWABLE.getID()    == CNO_Throwable);
        ERROR           = vm.createType(THROWABLE,    "Ljava/lang/Error;");         assume(ERROR.getID()        == CNO_Error);
        EXCEPTION       = vm.createType(THROWABLE,    "Ljava/lang/Exception;");     assume(EXCEPTION.getID()    == CNO_Exception);

       /*
        * Arrays
        */

        INT_ARRAY       = INT.asArray();                                            assume(INT_ARRAY.getID()    == CNO_intArray);
        LONG_ARRAY      = LONG.asArray();                                           assume(LONG_ARRAY.getID()   == CNO_longArray);
        FLOAT_ARRAY     = FLOAT.asArray();                                          assume(FLOAT_ARRAY.getID()  == CNO_floatArray);
        DOUBLE_ARRAY    = DOUBLE.asArray();                                         assume(DOUBLE_ARRAY.getID() == CNO_doubleArray);
        BOOLEAN_ARRAY   = BOOLEAN.asArray();                                        assume(BOOLEAN_ARRAY.getID()== CNO_booleanArray);
        CHAR_ARRAY      = CHAR.asArray();                                           assume(CHAR_ARRAY.getID()   == CNO_charArray);
        SHORT_ARRAY     = SHORT.asArray();                                          assume(SHORT_ARRAY.getID()  == CNO_shortArray);
        BYTE_ARRAY      = BYTE.asArray();                                           assume(BYTE_ARRAY.getID()   == CNO_byteArray);
        GLOBAL_ARRAY    = GLOBALENTRY.asArray();                                    assume(GLOBAL_ARRAY.getID() == CNO_globalArray);
        LOCAL_ARRAY     = LOCALENTRY.asArray();                                     assume(LOCAL_ARRAY.getID()  == CNO_localArray);
        OBJECT_ARRAY    = OBJECT.asArray();                                         assume(OBJECT_ARRAY.getID() == CNO_ObjectArray);
        STRING_ARRAY    = STRING.asArray();                                         assume(STRING_ARRAY.getID() == CNO_StringArray);
        CLASSBASE_ARRAY = CLASSBASE.asArray();                                      assume(CLASSBASE_ARRAY.getID() == CNO_ClassBaseArray);
        CLASS_ARRAY     = CLASS.asArray();                                          assume(CLASS_ARRAY.getID()  == CNO_ClassArray);
        THREAD_ARRAY    = THREAD.asArray();                                         assume(THREAD_ARRAY.getID() == CNO_ThreadArray);
        BYTE_ARRAY2D    = BYTE_ARRAY.asArray();                                     assume(BYTE_ARRAY2D.getID() == CNO_byteArrayArray);

        /*
         * Class loading exceptions. All the subclasses of LINKAGEERROR are actually just aliases for LINKAGEERROR
         * until these classes are supported as part of the API.
         */
        LINKAGEERROR     = vm.createType(ERROR,           "Ljava/lang/LinkageError;"); assume(LINKAGEERROR.getID() == CNO_LinkageError);
          VERIFYERROR                    = LINKAGEERROR;
          CLASSFORMATERROR               = LINKAGEERROR;
          NOCLASSDEFFOUNDERROR           = LINKAGEERROR;
          CLASSCIRCULARITYERROR          = LINKAGEERROR;
          INCOMPATIBLECLASSCHANGEERROR   = LINKAGEERROR;
            ABSTRACTMETHODERROR          =   INCOMPATIBLECLASSCHANGEERROR;
            ILLEGALACCESSERROR           =   INCOMPATIBLECLASSCHANGEERROR;
            INSTANTIATIONERROR           =   INCOMPATIBLECLASSCHANGEERROR;
            NOSUCHFIELDERROR             =   INCOMPATIBLECLASSCHANGEERROR;
            NOSUCHMETHODERROR            =   INCOMPATIBLECLASSCHANGEERROR;



    }

   /* ------------------------------------------------------------------------ *\
    *                              Static functions                            *
   \* ------------------------------------------------------------------------ */

   /**
    * Get an object array type for named type
    */
    private Type getArraySuperTypeFor(String name) throws LinkageException {
        int dims = 0;
        while (name.charAt(dims) == '[') {
             dims++;
        }

        String basicTypeName = name.substring(dims);
        Type superType;

        if (basicTypeName.startsWith("Ljava/lang/_") || basicTypeName.equals("Ljava/lang/Object;")) {
            dims--;
            superType = Type.OBJECT;
        } else {
            Type basicType = vm.createType(basicTypeName);
            basicType.load();
            superType = basicType.superType();
        }

        while (dims-- > 0) {
            superType = superType.asArray();
        }
//prtn("getArraySuperTypeFor "+name+" dims "+dims+" gets "+  superType);
        return superType;
    }


   /* ------------------------------------------------------------------------ *\
    *                               Type definiion                             *
   \* ------------------------------------------------------------------------ */

    public final static int DEFINED         = 1;
    public final static int LOADING         = 2;
    public final static int LOADED          = 3;
    public final static int CONVERTING      = 4;
    public final static int CONVERTED       = 5;
    public final static int INITIALIZING    = 6;
    public final static int INITIALIZED     = 7;
    public final static int FAILED          = 8;

   /**
    * The state of the class
    */
    private int state = DEFINED;

   /**
    * The entire name of the type (e.g "Ljava/lang/Object;")
    */
    private String name;

   /**
    * The VM this type was defined to be in
    */
    private VirtualMachine vm;

   /**
    * The superType
    */
    private Type superType;

   /**
    * An array's element type
    */
    private Type elementType;

   /**
    * Access flags
    */
    private int flags = -1;

   /**
    * Interface types implemented by this type
    */
    private Type[] interfaces = VirtualMachine.ZEROTYPES;

   /**
    * Constant objects (only strings at the moment)
    */
    private Object[] constantObjects = VirtualMachine.ZEROOBJECTS;

   /**
    * Fields implemented by this type
    */
    private Field[] fields;

   /**
    * Methods implemented by this type
    *
    * This array contains one entry for all the methods defined by this class
    * The array starts with the lowsest method slot number and ends with the
    * highest. If this class defines a method that replaces one in the hierarchy
    * then there will be entries missing. These are filled in with the corrosponding
    * entries from hierarchy. This is rather like vtables but in terms of intervals.
    * In a collection of classes with 1146 methods regular vtables would add up to
    * about 4400 entries. Doing things this way cuts this down to about 1600.
    */
    private Method[] methods = VirtualMachine.ZEROMETHODS;

   /**
    * The <clinit> method (or null)
    */
    private Method clinit;

   /**
    * Inferface methods implemented by this type
    */
    private short[] interfaceTable;

   /**
    * The number of an instance fields
    */
    private int instanceFieldCount;

   /**
    * The number of an static fields
    */
    private int staticFieldCount;

   /**
    * The method slot table size
    */
    private int methodTableSize;

   /**
    * The class number
    */
    private int id = -1;

    /**
     * The value of the SourceFile attribute or null if there wasn't one.
     */
    private String sourceFile;

    /**
     * The LinkageError raised when the class was loaded/converted.
     */
    private LinkageException linkageError;

   /* ------------------------------------------------------------------------ *\
    *                               Class creation                             *
   \* ------------------------------------------------------------------------ */

   /**
    * Static constructor only called from the VirtualMachine.java
    */
    static Type create(VirtualMachine vm, String name) {
        return new Type(vm, name);
    }

   /**
    * Disable the default constructor
    */
    private Type() {
        shouldNotReachHere();
    }

   /**
    * Private constructor
    */
    protected Type(VirtualMachine vm,  String name) {
        this.vm   = vm;
        this.name = name;

assume(!name.equals("B"), name);
assume(!name.equals("[B"), name);
assume(!name.equals("[[B"), name);

        if (name.charAt(0) == '[' || name.charAt(0) == '-') {
            flags = ACC_PUBLIC;
        }

assume(vm != null || name.startsWith("ip proxy"));
        if (name.charAt(0) != '-' && vm != null) {
            id = vm.allocateClassNumber();
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                     Class loading and initialization                     *
   \* ------------------------------------------------------------------------ */

    static Type convertionInProgress = null;
    static Vector convertQueue = new Vector();

   /**
    * Execute or queue a type's convertion
    */
    private static void queueConvertion(Type type) throws LinkageException {
       if (convertionInProgress == null) {
           convertType(type);
       } else {
           convertQueue.addElement(type);
       }
    }

   /**
    * Convert the methods in a a class
    */
    private static void convertType(Type type) throws LinkageException {
        try {
            assume(convertionInProgress == null,"type="+convertionInProgress);
            convertionInProgress = type;

            while(true) {
                type.convertMain();
                int size = convertQueue.size();
                if (size == 0) {
                    break;
                }
                type = (Type)convertQueue.lastElement();
                convertQueue.removeElementAt(size - 1);
            }
        } finally {
            convertionInProgress = null;
        }
    }

   /**
    * Initialize this class
    */
    private void convertMain() throws LinkageException {
        if (state < CONVERTING) {
            assume(state == LOADED, "state="+state+" type="+this);
           /*
            * Write trace message and set state
            */
            assume(vm != null);
            trace(vm.traceloading(name), "Converting class " + name);
            state = CONVERTING;

           /*
            * Convert this type's supertype first
            */
            if (this != Type.OBJECT) {
                 superType().load();
                 superType().convertMain();
            }

           /*
            * Convert all methods from their class file representation to their
            * intermediate representation
            */
            if (clinit != null) {
                clinit = clinit.asIrMethod();
            }
            for (int i = 0 ; i < methods.length ; i++) {
                if (methods[i] != null) {
                    methods[i] = methods[i].asIrMethod();
                }
            }
            state = CONVERTED;
            trace(vm.traceloading(name), "Converted class " + name);
        }
    }

   /**
    * Load this class
    */
    public void load() throws LinkageException {
        if (linkageError != null) {
            throw linkageError;
        }
//prtn("load "+name);
        if (state < LOADING) {
            int initial = name.charAt(0);
            if (initial == 'L') {
                assume(state == DEFINED);
                vm.load(name);
                assume(state == LOADED);
                if (vm.eagerloading()) {
                    queueConvertion(this);
                }
            } else {
                if (initial == '[') {
                    elementType().load();
                }
                state = LOADED;
            }
        }
    }

   /**
    * Mark type as being loaded
    */
    public void loadingStarted()  {
        assume(state == DEFINED);
        state = LOADING;
    }


   /**
    * Mark type as loaded
    */
    public void loadingFinished()  {
        assume(state == LOADING);
        state = LOADED;
    }

   /**
    * getState
    */
    public int getState() {
        return state;
    }

   /**
    * setState
    */
    public void setState(int state) {
        this.state = state;
    }

   /**
    * Convert the methods in a class
    */
    public void convert() throws LinkageException {
        if (state != CONVERTED) {
            load();
            convertType(this);
        }
    }




   /* ------------------------------------------------------------------------ *\
    *                             Instance functions                           *
   \* ------------------------------------------------------------------------ */

   /**
    * Return the type's VM
    */
    public VirtualMachine getVM() {
        return vm;
    }

   /**
    * Return the type's superType
    */
    public Type superType() throws LinkageException {
        if (superType == null && name.charAt(0) == '[') {
            superType = getArraySuperTypeFor(name);
        }
        assume(superType != null, "superType null for class "+name);
        return superType;
    }

   /**
    * Set the type's superType
    */
    public void setSuperType(Type superType) {
        if (superType != null) {
//prtn("name="+ name);
//prtn("superType="+ superType);
//prtn("this.superType="+ this.superType);
            assume(this.superType == superType || this.superType == null, "this="+this+" superType="+superType+" this.superType="+this.superType);

//if (this.superType != superType && this.superType != null) {
//  prtn("name="+ name + "superType="+ superType + "this.superType="+ this.superType);
//}
            this.superType = superType;
        }
    }

   /**
    * Set the type's attribute flags
    */
    public void setAccessFlags(int flags) {
        this.flags = flags;
    }

   /**
    * Get the type's attribute flags
    */
    public int getAccessFlags() {
        return flags;
    }

   /**
    * Set the type's interface table
    */
    public void setInterfaces(Type[] interfaces) {
        this.interfaces = interfaces;
    }

   /**
    * Get the type's interface table
    */
    public Type[] getInterfaces() {
        return interfaces;
    }

   /**
    * Set the type's field table
    */
    public void setFields(Field[] fields, int instanceFieldCount, int staticFieldCount) {
        this.fields = fields;
        this.instanceFieldCount = instanceFieldCount;
        assume(instanceFieldCount >= 0);
        this.staticFieldCount = staticFieldCount;
        assume(staticFieldCount >= 0);
    }

//   /**
//    * Add a single static string field
//    */
//
//    public Field addStaticStringField(String str) {
//
//       /*
//        * First look to see if there is already a static field for the string
//        */
//        for (int i = 0 ; i < fields.length ; i++) {
//            Field f = fields[i];
////System.out.println("i="+i+"f="+f+"fields.length="+fields.length+"type="+this);
//            if (f.getInitialValue() == str) {
//                assume(f.isStatic() && f.isFinal());
//                return f;
//            }
//        }
//
//       /*
//        * Create a new static for this String
//        */
//        Field field = new Field(this, "$$String$$"+staticFieldCount, Type.STRING, ACC_STATIC+ACC_FINAL);
//        field.setSlot(staticFieldCount++, str);
//
//       /*
//        * Add it to the end of the field table
//        */
//        Field[] newFields = new Field[fields.length+1];
//        System.arraycopy(fields, 0, newFields, 0, fields.length);
//        newFields[fields.length] = field;
//        fields = newFields;
//        return field;
//    }


   /**
    * Add something to the static objects table
    */
    public int addStaticObject(Object obj) {

       /*
        * First look to see if there is already one of these
        */
        for (int i = 0 ; i < constantObjects.length ; i++) {
            Object o = constantObjects[i];
            if (o.equals(obj)) {
                return i;
            }
        }

       /*
        * Add it to the end of the field table
        */
        Object[] newObjects = new Object[constantObjects.length+1];
        System.arraycopy(constantObjects, 0, newObjects, 0, constantObjects.length);
        newObjects[constantObjects.length] = obj;
        constantObjects = newObjects;
        return constantObjects.length - 1;
    }

   /**
    * getStaticObject
    */
    public Object getStaticObject(int i) {
        return constantObjects[i];
    }


   /**
    * Get the type's field table
    */
    public Field[] getFields() {
        return fields;
    }

   /**
    * Set the type's source file.
    */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

   /**
    * Get the type's source file (may be null).
    */
    public String getSourceFile() {
        return sourceFile;
    }

   /**
    * Get the type's field table width
    */
    public int getInstanceFieldCount() {
        assume(instanceFieldCount >= 0);
        return instanceFieldCount;
    }

   /**
    * Set the type's method table
    */
    public void setMethods(Method[] methods, int methodTableSize) {
        this.methods = methods;
        this.methodTableSize = methodTableSize;
    }

   /**
    * Get the type's method table
    */
    public Method[] getMethods() {
        return methods;
    }

   /**
    * Get the method slot table size
    */
    public int getMethodTableSize() {
        return methodTableSize;
    }

   /**
    * Set the type's <clinit>
    */
    public void setClinit(Method clinit) {
        this.clinit = clinit;
    }

   /**
    * Get the type's <clinit>
    */
    public Method getClinit() {
        return clinit;
    }

   /**
    * Set the type's interface table
    */
    public void setInterfaceTable(short[] interfaceTable) {
        this.interfaceTable = interfaceTable;
//prtn("setInterfaceTable for "+this+" = "+ interfaceTable);
    }

   /**
    * Get the type's interface table
    */
    public short[] getInterfaceTable() {
        return interfaceTable;
    }

   /**
    * Return the name of the type
    */
    public String name() {
        return name;
    }

   /**
    * Set the LinkageException instance that was raised when this Type was loaded/converted.
    */
    public void setLinkageError(LinkageException le) {
        linkageError = le;
    }


   /**
    * Return the LinkageException instance (if any) that was raised when this Type was loaded/converted.
    */
    public LinkageException getLinkageError() {
        return linkageError;
    }

   /**
    * Return a string representation of the type for debug
    */
    public String toString() {
        return name;
    }

   /**
    * Return the signarure representation
    */
    public String toSignature() {
        return toSignature(true,false);
    }
    public String toSignature(boolean includePackage, boolean asSourceDecl) {
        Type t = this;
        int depth = 0;
        while(t.isArray()) {
            depth++;
            t = t.elementType();
        }
        String res = t.toSignaturePrim(includePackage, asSourceDecl);
        while (depth-- > 0) {
            if (asSourceDecl) {
               res += "[]";
            }
            else {
               res = '[' + res;
            }
        }
        return res;
    }

   /**
    * Return the signature representation
    */
    private String toSignaturePrim(boolean includePackage, boolean asSourceDecl) {
        if (asSourceDecl) {
            if (this == VOID)    return "void";
            if (this == INT)     return "int";
            if (this == LONG)    return "long";
            if (this == FLOAT)   return "float";
            if (this == DOUBLE)  return "double";
            if (this == BOOLEAN) return "boolean";
            if (this == CHAR)    return "char";
            if (this == SHORT)   return "short";
            if (this == BYTE)    return "byte";
        }
        else {
            if (this == VOID)    return "V";
            if (this == INT)     return "I";
            if (this == LONG)    return "J";
            if (this == FLOAT)   return "F";
            if (this == DOUBLE)  return "D";
            if (this == BOOLEAN) return "Z";
            if (this == CHAR)    return "C";
            if (this == SHORT)   return "S";
            if (this == BYTE)    return "B";
        }
        assume(name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';');
        String result = name;
        if (asSourceDecl) {
            result = name.substring(1,name.length() - 1);
        }
        if (!includePackage) {
            int base = result.lastIndexOf('/') + 1;
            return result.substring(base);
        }
        return result;
    }


   /**
    * Get the Java super class of this type
    */
    public Type superClass()  throws LinkageException {
        if (this == Type.OBJECT) {
            return null;
        }
        if (isArray()) {
            return Type.OBJECT;
        } else {
            return superType();
        }
    }

   /**
    * Work out if this is a hierarchical subtype of another type
    */
    private boolean isKindOf(Type aType) throws LinkageException {
       /*
        * Primitives never match non-primitives
        */
        if (this.isPrimitive() != aType.isPrimitive()) {
            return false;
        }

       /*
        * Check to see if this in a subclass of aType
        */
        Type thiz = this;
        for (;;) {
            if (thiz == aType) {
                return true;
            }
            if (thiz == UNIVERSE) {
                return false;
            }
            thiz = thiz.superType();
        }


    }


   /**
    * Work out this type can be assigned to another type
    */
    public boolean vIsAssignableTo(Type aType) throws LinkageException {
       /*
        * Quickly check for common values
        */
        if (this == aType || aType == UNIVERSE || aType == BOGUS) {
           return true;
        }

       /*
        * NEWOBJECT never matches
        */
        if (aType == NEWOBJECT || this == NEWOBJECT) {
            return false;
        }

       /*
        * NULLOBJECT matches all non-primitives
        */
        if (aType == NULLOBJECT && !this.isPrimitive()) {
            return true;
        }

       /*
        * For verification all interfaces are treated as java.lang.Object
        */
        if (aType.isInterface()) {
            aType = OBJECT;
        }

       /*
        * Check to see of this class is somewhere in aType's hierarchy
        */
        if (this.isKindOf(aType)) {
            return true;
        }

       /*
        * If aType is some like of object and this is the null object
        * then assignment is allowed
        */
        if (this == NULLOBJECT && aType.isKindOf(OBJECT)) {
            return true;
        }


       /*
        * This is needed to cast arrays of classes into arrays of interfaces
        */
        if (this.isArray() && aType.isArray()) {
            return this.elementType().vIsAssignableTo(aType.elementType());
        }

       /*
        * Otherwise there is no match
        */
        return false;
    }


   /**
    * isLoaded
    */
    public boolean isLoaded() {
        return state >= LOADED;
    }

   /**
    * isPublic
    */
    public boolean isPublic() {
        assume(flags != -1);
        return (flags & ACC_PUBLIC) != 0;
    }

   /**
    * isPrivate
    */
    public boolean isPrivate() {
        assume(flags != -1);
        return (flags & ACC_PRIVATE) != 0;
    }

   /**
    * isProtected
    */
    public boolean isProtected() {
        assume(flags != -1);
        return (flags & ACC_PROTECTED) != 0;
    }

   /**
    * isFinal
    */
    public boolean isFinal() {
        assume(flags != -1);
        return (flags & ACC_FINAL) != 0;
    }

   /**
    * isInterface
    */
    public boolean isInterface() {
        assume(flags != -1, " for "+name);
        return (flags & ACC_INTERFACE) != 0;
    }

   /**
    * isAbstract
    */
    public boolean isAbstract() {
        assume(flags != -1);
        return (flags & ACC_ABSTRACT) != 0;
    }

   /**
    * isPrimitive
    */
    public boolean isPrimitive() {
       return superType == PRIMITIVE || superType == INT;
    }


    public final static int BASIC_OOP  = 0x10000;
    public final static int BASIC_INT  = 0x20000;
    public final static int BASIC_LONG = 0x30000;

   /**
    * Slots are of three types single word, double word, and oop
    */
    public int slotType() {
        if(!isPrimitive()) {
            return BASIC_OOP;               // Must be an oop
        } else if (isTwoWords()) {
            return BASIC_LONG;              // Two words
        } else {
            return BASIC_INT;               // Must be one word
        }
    }

   /**
    * isTwoWords
    */
    public boolean isTwoWords() {
        assume(this != Type.VOID);
        if (this == Type.LONG || this == Type.DOUBLE) {
            return true;
        } else {
            return false;
        }
    }

   /**
    * isLong
    */
    public boolean isLong() {
        return this == Type.LONG;
    }

   /**
    * isDouble
    */
    public boolean isDouble() {
        return this == Type.DOUBLE;
    }

   /**
    * isArray
    */
    public boolean isArray() {
        return name.charAt(0) == '[';
    }

   /**
    * dimensions
    */
    public int dimensions() {
        assume(this != Type.VOID);
        int i;
        for (i = 0 ; i < name.length() ; i++) {
            if (name.charAt(i) != '[') {
                return i;
            }
        }
        return shouldNotReachHere();
    }

   /**
    * elementType
    */
    public Type elementType() {
        assume(name.charAt(0) == '[',"name="+name);
        if (elementType == null) {
            elementType = vm.createType(name.substring(1));
        }
        assume(elementType != null);
        return elementType;
    }


   /**
    * Get the array type of this type
    */
    public Type asArray() {
        return vm.createType("["+name);
    }

   /**
    * Return true if the types are in the same package
    */
    public boolean inSamePackageAs(Type aType) {
        String name1 = this.name();
        String name2 = aType.name();
        int last1 = name1.lastIndexOf('/');
        int last2 = name2.lastIndexOf('/');
        if (last1 != last2) {
            return false;
        }
        if (last1 == -1) {
            return true;
        }
        for (int i = 0 ; i < last1 ; i++) {
            if (name1.charAt(i) != name2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

   /**
    * getID
    */
    public int getID() {
        return id;
    }

   /* ------------------------------------------------------------------------ *\
    *                               Member lookup                              *
   \* ------------------------------------------------------------------------ */

   /**
    * Find a field
    */
    public Field findField(String name, String descriptor) {
        Field f = findField(name);
        if (f != null) {
           /*
            * If the field type is not the same as the type of the descriptor
            * then the field just returned is not correct and the classes must
            * have gotten out of sync with each other.
            */
            if (f.type() != vm.createType(descriptor)) {
                return null;
            }
        }
        return f;
    }

   /**
    * Find a field
    */
    public Field findField(String name) {
        if (fields == null) {
            return null;
        }
        assume(fields != null, "fields==null in"+this);
        for (int i = 0 ; i < fields.length ; i++) {
            if (name == fields[i].name()) {
                assume(name.equals(fields[i].name())); // test that interning is working
                return fields[i];
            } else {
                assume(!name.equals(fields[i].name()));// test that interning is working
            }
        }
        if (superType != null) {
            return superType.findField(name);
        }
        return null;
    }

   /**
    * Find a method
    */
    public Method findMethod(String name, String descriptor) {
//prtn("findMethod "+ name+" "+   descriptor);
        int count = Method.countParameters(descriptor);
        if (count < 0) {
            return null;
        }
        Type[] parmTypes  = new Type[count];
        Type   returnType = Method.getParameterTypes(this, descriptor, parmTypes);
        parmTypes = vm.internList(parmTypes);

        Method m = findMethod(name, parmTypes);
        if (m != null) {
            if (m.type() != returnType) {
                return null;
            }
        }
        return m;
    }

   /**
    * Find a method
    */
    public Method findMethod(String name, Type[] parmTypes) {
        if (name == VirtualMachine.SQUAWK_CLINIT) {
            return getClinit();
        }
        return findMethod(this, name, parmTypes);
    }

   /**
    * findMethod
    */
    private Method findMethod(Type parentType, String name, Type[] parmTypes) {
        assume(methods != null,"methods==null in "+this);
        for (int i = 0 ; i < methods.length ; i++) {
            if (methods[i] != null) {
                if (methods[i].parent() == parentType) {
                    if (name == methods[i].name()) {
                        assume(name.equals(methods[i].name()));     // test that interning is working
                        Type [] mparms = methods[i].getParms();
                        if (mparms == parmTypes) {
                            assume(areSame(mparms, parmTypes));     // test that interning is working
                            return methods[i];
                        } else {
                            assume(!areSame(mparms, parmTypes));    // test that interning is working
                        }
                    } else {
                        assume(!name.equals(methods[i].name()), ""+name);// test that interning is working
                    }
                }
            }
        }
        if (superType != Type.UNIVERSE) {
            Method method = superType.findMethod(name, parmTypes);
            if (method != null) {
                return method;
            }
        }

        /* Before we give up on an interface class, check any interfaces that
         * it implements */
        Type[] interfaces = getInterfaces();
        if (isInterface() && interfaces != null) {
            for (int i = 0; i != interfaces.length; i++) {
                Method method = interfaces[i].findMethod(name, parmTypes);
                if (method != null) {
                    return method;
                }
            }
        }

        return null;
    }

   /**
    * areSame
    */
    private boolean areSame(Type[] aList, Type[] bList) {
        if (DEBUG) {
            if(aList.length != bList.length) {
                return false;
            }
            for (int j = 0 ; j < aList.length ; j++) {
                if(aList[j] != bList[j]) {
                    return false;
                }
            }
        }
        return true;
    }

   /**
    * findMethod
    */
    public Method findMethod(int slot) throws LinkageException {
        if (slot == SLOT_clinit) {
            Method clinit = getClinit();
            if (clinit == null) {
                assume(id != CNO_Object,"<clinit> for Object is missing");
                clinit = OBJECT.getClinit();
            }
            return clinit;
        }
        if (methods != null) {
//System.out.println("findMethod slot "+slot+" in "+this);
            if (methods.length > 0) {
                int first = methods[0].getSlotOffset();
                if (slot >= first) {
                    slot -= first;
                    assume(slot < methods.length);//, "slot="+slot+" methods.length="+methods.length);
//System.out.println("Found slot "+slot+ " name ="+ methods[slot].name());
                    return methods[slot];
                }
            }
        }
        return superType().findMethod(slot);
    }

   /**
    * IntermediateMethod
    */
    public IntermediateMethod findIntermediateMethod(int slot) throws LinkageException {
//System.out.println("findIntermediateMethod slot "+slot+" in "+this);
        Method method = findMethod(slot);
        if (method instanceof ClassFileMethod) {
            method.parent().convert();
            method = method.parent().findMethod(method.getSlotOffset());
        }
        return (IntermediateMethod)method;
    }

   /**
    * IntermediateMethod
    */
    public IntermediateMethod findIntermediateMethod(String name, String descriptor) throws LinkageException {
        Method method = findMethod(name, descriptor);
        if (method instanceof ClassFileMethod) {
            method.parent().convert();
            method = method.parent().findMethod(method.getSlotOffset());
        }
        return (IntermediateMethod)method;
    }

   /**
    * IntermediateMethod
    */
    public IntermediateMethod findMethodOrInterface(int slot) throws LinkageException {
//System.out.println("findMethodOrInterface slot "+slot+" in "+this);
        if (slot >= VirtualMachine.FIRSTINTERFACE) {
            assume(interfaceTable != null, "null interfaceTable in "+this);
            int first = interfaceTable[0];
            assume(slot-first+1 < interfaceTable.length, "length="+interfaceTable.length+" slot="+slot+" first="+first);
            slot = interfaceTable[slot-first+1];
        }

        return findIntermediateMethod(slot);
    }

   /**
    * isMethodInThisVtable
    */
    private Method isMethodInThisVtable(Method method) {
        for (int i = 0 ; i < methods.length ; i++) {
            if (methods[i] == method || methods[i].isProxyFor(method)) {
                return methods[i];
            }
        }
        return null;
    }

   /**
    * createProxyFor
    */
    public Method createProxyFor(Method method) {
        Method proxy = isMethodInThisVtable(method);
        if (proxy == null) {
            Method last = methods[methods.length-1];
//prtn("************* Adding for "+method.parent()+"::"+method+" in "+this + " offset "+(last.getSlotOffset()+1));
            proxy = new MethodProxy(method, this, last.getSlotOffset()+1);
            Method[] newMethods = new Method[methods.length+1];
            System.arraycopy(methods, 0, newMethods, 0, methods.length);
            newMethods[methods.length] = proxy;
            methods = newMethods;
        }
        return proxy;
    }



   /**
    * Write the class definition in XML
    */
    public void writeClass(PrintStream out, LinkageException le) {
        assume(le == linkageError);
        String msg = le.getMessage();
        if (msg == null) {
            msg = "";
        }
        String javaName = name();
        if (javaName.charAt(0) != '[') {
            javaName = javaName.substring(1, javaName.length() - 1);
        }
        javaName = javaName.replace('/', '.');
        out.println("    <name>"+javaName+"</name>");
        out.println("    <extends>"+Type.OBJECT.getID()+"</extends>");
        out.println("    <constants>");
        out.println("      <string>" + XMLEncodeString(msg) + "</string>");
        out.println("    </constants>");
    }

   /**
    * Write the class definition in XML
    */
    public void writeClass(PrintStream out) {
        if (linkageError != null) {
            writeClass(out, linkageError);
            return;
        }
        try {
            if (this != Type.OBJECT) {
                out.println("    <extends>"+superType().getID()+"</extends>");
            }
            if (interfaces != VirtualMachine.ZEROTYPES) {
                out.print("    <implements>");
                for (int i = 0 ; i < interfaces.length ; i++) {
                    if (i != 0) {
                        out.print(" ");
                    }
                    out.print(interfaces[i].getID());
                }
                out.println("</implements>");
            }

            String javaName = name();
            if (javaName.charAt(0) != '[') {
                javaName = javaName.substring(1, javaName.length() - 1);
            }
            javaName = javaName.replace('/', '.');
            out.println("    <name>"+javaName+"</name>");

            if (isAbstract()) {
                out.println("    <abstract/>");
            }
            if (isInterface()) {
                out.println("    <interface/>");
            }
            if (isArray()) {
                out.println("    <arrayof>"+elementType().getID()+"</arrayof>");
            }
            if (getSourceFile() != null) {
                out.println("    <sourcefile>"+getSourceFile()+"</sourcefile>");
            }
            if (constantObjects.length > 0) {
                out.println("    <constants>");
                for (int i = 0 ; i < constantObjects.length ; i++) {
                    Object o = constantObjects[i];
                    assume(o instanceof String);
                    out.println("      <string>" + XMLEncodeString((String)o) + "</string>" + "  \t<!-- entry "+i+"-->");
                }
                out.println("    </constants>");
            }

            if (staticFieldCount > 0) {
                out.println("    <static_variables>");
                for (int i = 0 ; i < fields.length ; i++) {
                    Field f = fields[i];
                    if (f.isStatic() && f.getSlotOffset() >= 0) {
                        if (f.getInitialValue() == null) {
                            out.println("      <"+f.xmltag()+"/>" + "  \t<!--"+f.getSlotOffset()+"="+f.name()+"-->");
                        } else {
                            out.print("      <"+f.xmltag()+">");
                            String s = f.getInitialValue().toString();
                            for (int j = 0 ; j < s.length() ; j++) {
                                char ch = s.charAt(j);
                                if (ch < ' ' || ch >= 0x7F || ch == '<' || ch == '>' || ch == '&' || ch == '"') {
                                    out.print("&#");
                                    out.print((int)ch);
                                    out.print(';');
                                } else {
                                    out.print(ch);
                                }
                            }
                            out.println("</"+f.xmltag()+">" + "  \t<!--"+f.getSlotOffset()+"="+f.name()+"-->");
                        }
                    }
                }
                out.println("    </static_variables>");
            }
            if (instanceFieldCount > 0) {
                out.println("    <instance_variables>");
                for (int i = 0 ; i < fields.length ; i++) {
                    Field f = fields[i];
                    if (!f.isStatic() && f.getSlotOffset() >= 0) {
                        out.println("      <"+f.xmltag()+"/>" + "  \t<!--"+f.getSlotOffset()+"="+f.name()+"-->");
                    }
                }
                out.println("    </instance_variables>");
            }

            if (interfaceTable != null) {
                out.println("    <interface_map>");
                int first = interfaceTable[0] - 10000;
                for (int i = 1 ; i < interfaceTable.length ; i++) {
                    out.println("      <from>"+(first++)+"</from><to>"+interfaceTable[i]+"</to>");
                }
                out.println("    </interface_map>");
            }
        } catch (LinkageException le) {
            writeClass(out, le);
        }
    }


}


