
package com.sun.squawk.translator;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;

public class Field extends Member {

   /**
    * Offset into the instance or global vector
    */
    private int offset;

   /**
    * Value for the field to be initialized to
    */
    private Object initialValue;

   /**
    * Private constructor
    */
    Field(Type parent, String name, Type type, int flags) {
        super(parent, name, type, flags);
    }

   /**
    * Public constructor
    */
    public static Field create(VirtualMachine vm, InputContext ctx, Type parent, String name, String descriptor, int flags) {
        Field field = new Field(parent, name, parent.getVM().createType(descriptor), flags);
        field.setFlag(flags);
        return field;
    }

   /**
    * Loading
    */
    public void load() throws LinkageException {
        if (!isLoaded()) {
            parent().load();
            type().load();
            setLoaded();
        }
    }

   /**
    * Set slot
    */
    public void setSlot(int offset, Object initialValue) {
        this.offset = offset;
        this.initialValue = initialValue;
    }

   /**
    * Get slot offset
    */
    public int getSlotOffset() {
        return offset;
    }

   /**
    * Get slot initial value
    */
    public Object getInitialValue() {
        return initialValue;
    }

   /**
    * xmltag
    */
    public String xmltag() {
        if (type() == Type.BYTE)    return "byte";
        if (type() == Type.BOOLEAN) return "byte";
        if (type() == Type.CHAR)    return "half";
        if (type() == Type.SHORT)   return "half";
        if (type() == Type.INT)     return "word";
        if (type() == Type.FLOAT)   return "word";
        if (type() == Type.LONG)    return "dword";
        if (type() == Type.DOUBLE)  return "dword";
        if (type() == Type.STRING && isStatic() && initialValue != null)  return "string";
        return "ref";
    }
}
