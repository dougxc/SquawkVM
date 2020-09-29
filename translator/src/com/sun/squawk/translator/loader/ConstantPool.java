
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ConstantPool extends BaseFunctions implements RuntimeConstants {

   /**
    * The constant pool entries are encoded as regular Java objects. The list
    * of valid objects for each tag type are:
    *
    *   CONSTANT_Utf8               null (Not retained)
    *   CONSTANT_NameAndType        null (Not retained)
    *   CONSTANT_Integer            java.lang.Integer
    *   CONSTANT_Float              java.lang.Float
    *   CONSTANT_Long               java.lang.Long
    *   CONSTANT_Double             java.lang.Double
    *   CONSTANT_String             java.lang.String
    *   CONSTANT_Class              com.sun.squawk.translator.Type
    *   CONSTANT_Field              com.sun.squawk.translator.Field
    *   CONSTANT_Method             com.sun.squawk.translator.Method
    *   CONSTANT_InterfaceMethod    com.sun.squawk.translator.Method
    *
    * Thus only a null, Integer, Long, Float, Double, Type, Field, or Method will
    * be found in this array.
    *
    * CONSTANT_Utf8 entries are converted into Strings
    * CONSTANT_NameAndType are not needed becuse the UTF8 strings they refer
    * to is converted into strings and places in the approperate Field and Method
    * data structures.
    */

   /**
    * The virtual machine for this constant pool
    */
    private VirtualMachine vm;

   /**
    * Input stream context
    */
    private InputContext ctx;

   /**
    * Pool entry tags
    */
    private byte[] tags;

   /**
    * Entires that refer to other entries that haven't yet been resolved.
    */
    private int[] unresolvedEntries;

   /**
    * Resolved pool entries for all object types
    */
    private Object[] entries;

   /**
    * Prevent direct construction
    */
    private ConstantPool() {}

    /**
     * Verify that an index to an entry is within range and is of an expected type.
     */
    private void verifyEntry(int index, int tag) throws LinkageException {
        if (index < 1 || index >= entries.length) {
            throw new LinkageException(Type.CLASSFORMATERROR, "constant pool index out of range: " + index);
        }
        if (tags[index] != tag) {
            throw new LinkageException(Type.CLASSFORMATERROR, "bad constant pool index: expected " + tag + ", got " + tags[index]);
        }
    }

    /**
     * Verify that legal field name occurs at a given offset of a string. No unicode
     * support yet.
     * @param s the string
     * @param offset the offset at which a legal field name should occur
     * @param slashOkay
     * @return the first character after the legal field name or -1 if there is no legal field name
     * at the given offset.
     */
    private static int skipOverFieldName(String s, int offset, boolean slashOkay) {
        char lastCh = (char)0;
        char ch;
        int i;
        for (i = offset; i != s.length(); i++, lastCh = ch) {
            ch = s.charAt(i);
            if ((int)ch < 128) {
                /* quick check for ascii */
                if ((ch >= 'a' && ch <= 'z') ||
                    (ch >= 'A' && ch <= 'Z') ||
                    (lastCh != 0 && ch >= '0' && ch <= '9')) {
                    continue;
                }
            } else {
                // Until some kind of unicode support is added, this is simply treated as
                // a class format error. This will at least be an issue during TCK certification
                return -1;
            }

            if (slashOkay && (ch == '/') && (lastCh != 0)) {
                if (lastCh == '/') {
                    return -1;    /* Don't permit consecutive slashes */
                }
            } else if (ch == '_' || ch == '$') {
                continue;
            } else {
                return lastCh != 0 ? i : -1;
            }
        }
        return lastCh != 0 ? i : -1;
    }

    /**
     * Verify that legal type name occurs at a given offset of a string. No unicode
     * support yet.
     * @param s the string
     * @param offset the offset at which a legal field name should occur
     * @param slashOkay
     * @return the first character after the legal field name or -1 if there is no legal field name
     * at the given offset.
     */
    static private int skipOverFieldType(String s, boolean voidOkay)
    {
        int length = s.length();
        int depth = 0;
        for (int i = 0; i != length; i++) {
            switch (s.charAt(i)) {
            case 'V':
                if (!voidOkay) return -1;
                /* FALL THROUGH */
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return i + 1;

            case 'L': {
                /* Skip over the class name, if one is there. */
                int end = skipOverFieldName(s, i + 1, true);
                if (end != -1 && end < length && s.charAt(end) == ';') {
                    return end + 1;
                }
                else {
                    return -1;
                }
            }

            case '[':
                /* The rest of what's there better be a legal signature.  */
                if (++depth == 256) {
                    return -1;
                }
                voidOkay = false;
                break;

            default:
                return -1;
            }
        }
        return -1;
    }

    /**
     * Verify that a name for a given class component has a valid format.
     * @param type 'm' - method name
     *             'f' - field name
     *             'c' - class name
     * @return the given string if it is valid
     * @exception ClassFormatError if the name is invalid
     */
    public static String verifyName(String name, char type) throws LinkageException {
        assume(type == 'm' || type == 'f' || type == 'c');
        boolean result = false;
        int length = name.length();
        if (length > 0) {
            if (name.charAt(0) == '<') {
                result = (type == 'm') &&
                    (name.equals("<init>") ||
                     name.equals("<clinit>"));
            } else {
                int end;
                if (type == 'c' && name.charAt(0) == '[') {
                    end = skipOverFieldType(name, false);
                } else {
                    end = skipOverFieldName(name, 0, type == 'c');
                }
                result = (end != -1) && (end == name.length());
            }
        }
        if (!result) {
            String typeName = "class";
            if (type == 'f') {
                typeName = "field";
            }
            else if (type == 'm') {
                typeName = "method";
            }
            throw new LinkageException(Type.CLASSFORMATERROR, "Bad " + typeName + " name");
        }
        return name;
    }

   /**
    * Get the tag for the entry
    */
    public int getTag(int index) throws LinkageException {
        if (index < 0 || index >= entries.length ) {
            ctx.classFormatException("Bad constant index");
        }
        return tags[index];
    }

    public int getInt(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Integer);
        return unresolvedEntries[index];
    }

    public long getLong(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Long);
        return ((Long)entries[index]).longValue();
    }

    public float getFloat(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Float);
        return ((Float)entries[index]).floatValue();
    }

    public double getDouble(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Double);
        return ((Double)entries[index]).doubleValue();
    }

    public String getString(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_String);
        return (String)entries[index];
    }

    public String getUtf8(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Utf8);
        return (String)entries[index];
    }

    public String getUtf8Interning(int index) throws LinkageException {
        return vm.internString(getUtf8(index));
    }

    public Type bootstrapType(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Class);
        return (Type)entries[index];
    }

    public Type getType(int index) throws LinkageException {
        Type type = bootstrapType(index);
        if (type != null && !type.isLoaded()) {
            type.load();
        }
        return type;
    }

    public Field getField(int index) throws LinkageException {
        verifyEntry(index, CONSTANT_Field);
        Field field = (Field)entries[index];
        if (field == null) {
            field = (Field)resolve(index);
            entries[index] = field;
        }
        return field;
    }

    public Method getMethod(int index, boolean interfaceMethod) throws LinkageException {
        verifyEntry(index, (interfaceMethod ? CONSTANT_InterfaceMethod : CONSTANT_Method));
        Method method = (Method)entries[index];
        if (method == null) {
            method = (Method)resolve(index);
            entries[index] = method;
        }
        return method;
    }

    public Object getEntry(int index, int tag) throws LinkageException {
        verifyEntry(index, tag);
        if (getTag(index) == CONSTANT_Integer) {
            return new Integer(unresolvedEntries[index]);
        }
        return entries[index];
    }


   /* ------------------------------------------------------------------------ *\
    *                             Pool loading code                            *
   \* ------------------------------------------------------------------------ */

   /**
    * Create a new constant pool from the input stream
    */
    public static ConstantPool create(VirtualMachine vm, ClassFileInputStream in) throws IOException, LinkageException {
        return new ConstantPool(vm, in);
    }


    private ConstantPool(VirtualMachine vm, ClassFileInputStream in) throws LinkageException {

       /*
        * Keep input stream pointer in order to get verification errors
        */
        this.vm = vm;
        this.ctx = in;

        try {
           /*
            * Read the constant pool entry count
            */
            int count = in.readUnsignedShort("cp-count");

           /*
            * Allocate the required lists
            */
            tags              = new byte[count];
            unresolvedEntries = new int[count];
            entries           = new Object[count];

           /*
            * Read the constant pool entries from the classfile
            * and initialize the constant pool correspondingly.
            * Remember that constant pool indices start from 1
            * rather than 0 and that last index is count-1.
            */

           /*
            * Pass 1 read in the primitive values
            */
            for (int i = 1 ; i < count ; i++) {
                int tag = in.readUnsignedByte("cp-tag");
                tags[i] = (byte)tag;
                switch (tag) {
                    case CONSTANT_Utf8: {
                        //ntries[i] = vm.internString(in.readUTF("CONSTANT_Utf8"));
                        entries[i] = in.readUTF("CONSTANT_Utf8");
                        break;
                    }
                    case CONSTANT_Integer: {
                        // used unresolvedEntries to prevent constructing an Integer wrapper
                        unresolvedEntries[i] = in.readInt("CONSTANT_Integer");
                        break;
                    }
                    case CONSTANT_Float: {
                        entries[i] = new Float(in.readFloat("CONSTANT_Float"));
                        break;
                    }
                    case CONSTANT_Long: {
                        entries[i] = new Long(in.readLong("CONSTANT_Long"));
                        i++; // Longs take two slots
                        break;
                    }
                    case CONSTANT_Double: {
                        entries[i] = new Double(in.readDouble("CONSTANT_Double"));
                        i++; // Doubles take two slots
                        break;
                    }

                    case CONSTANT_String:
                    case CONSTANT_Class: {
                        unresolvedEntries[i] = in.readUnsignedShort("CONSTANT_String/Class");
                        break;
                    }

                    case CONSTANT_Field:
                    case CONSTANT_Method:
                    case CONSTANT_InterfaceMethod:
                    case CONSTANT_NameAndType: {
                        unresolvedEntries[i] = (in.readUnsignedShort("CONSTANT_F/M/I/N-1") << 16) | (in.readUnsignedShort("CONSTANT_F/M/I/N-2") & 0xFFFF);
                        break;
                    }

                    default: {
                        in.classFormatException("Invalid constant pool entry: tag="+tag);
                    }
                }
            }

           /*
            * Pass 2 fixup types and strings
            */
            for (int i = 1 ; i < count ; i++) {
                try {
                    switch (tags[i]) {
                        case CONSTANT_String: {
                            verifyEntry(unresolvedEntries[i], CONSTANT_Utf8);
                            entries[i] = entries[unresolvedEntries[i]];
                            break;
                        }
                        case CONSTANT_Class: {
                            verifyEntry(unresolvedEntries[i], CONSTANT_Utf8);
                            String name = verifyName((String)entries[unresolvedEntries[i]], 'c');
                            if (name.charAt(0) != '[') {
                                name = "L"+name+";";
                            }
                            entries[i] = vm.createType(name);
                            break;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException obe) {
                    ctx.classFormatException("bad constant pool index");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            in.classFormatException("badly formed constant pool");
        }
    }


   /**
    * Resolve member references
    */
    private Object resolve(int i) throws LinkageException {
        assume(i > 0 && i < entries.length && (tags[i] == CONSTANT_Field  ||
                                               tags[i] == CONSTANT_Method ||
                                               tags[i] == CONSTANT_InterfaceMethod));
        int classIndex       = unresolvedEntries[i] >> 16;
        int nameAndTypeIndex = unresolvedEntries[i] & 0xFFFF;
        verifyEntry(nameAndTypeIndex, CONSTANT_NameAndType);
        int nameIndex        = unresolvedEntries[nameAndTypeIndex] >> 16;
        int descriptorIndex  = unresolvedEntries[nameAndTypeIndex] & 0xFFFF;
        verifyEntry(nameIndex, CONSTANT_Utf8);
        verifyEntry(descriptorIndex, CONSTANT_Utf8);
        verifyEntry(classIndex, CONSTANT_Class);
        Type parentType      = (Type)entries[classIndex];
        String name          = getUtf8Interning(nameIndex);
        String sig           = (String)entries[descriptorIndex];

        if (!parentType.isLoaded()) {
            parentType.load();
        }

        assume(parentType.superType() != null);

        switch(tags[i]) {
            case CONSTANT_Field: {
                verifyName(name, 'f');
                Field field = parentType.findField(name, sig);
                if (field == null) {
                    throw new LinkageException(Type.NOSUCHFIELDERROR, "No such field: "+name+sig+" in "+parentType);
                }
                field.load();
                return field;
            }

            case CONSTANT_Method:
                verifyName(name, 'm');
               /*
                * Remame references to <init> to be _SQUAWK_INTERNAL_init
                */
                if (name == VirtualMachine.INIT) {
                    name =  VirtualMachine.SQUAWK_INIT;
                }

               /*
                * Remame references to "main" to be _SQUAWK_INTERNAL_main
                */
                if (name == VirtualMachine.MAIN) {
                    name =  VirtualMachine.SQUAWK_MAIN;
                }

                /* drop thru... */

            case CONSTANT_InterfaceMethod: {
                Method method = parentType.findMethod(name, sig);
                if (method == null) {
                    //prtn("hash="+type.hashCode()+" j="+type.methods.length);
                    //for(int j = 0 ; j < type.methods.length ; j++) {
                    //    prtn(type.methods[j]+"\n");
                    //}
                    throw new LinkageException(Type.NOSUCHMETHODERROR, "No such method: "+name+sig+" in "+parentType);
                }
                if (tags[i] == CONSTANT_Method) {
                    if (method.parent().isInterface()) {
                        throw new LinkageException(Type.INCOMPATIBLECLASSCHANGEERROR, "Method should not be in interface "+name+" in "+parentType);
                    }
                } else {
                    if (!method.parent().isInterface()) {
                        throw new LinkageException(Type.INCOMPATIBLECLASSCHANGEERROR, "Method should be in interface "+name+" in "+parentType);
                    }
                }
                method.load();
                return method;
            }
            default: {
                shouldNotReachHere();
            }
        }
        return null;
    }


}
