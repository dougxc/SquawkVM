
package com.sun.squawk.translator;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;

class Member extends BaseFunctions implements RuntimeConstants {

   /**
    * The type in which the methods was originally defined. The method
    * can belong to a number of classes by inheritance, but only one
    * class can have defined it.
    */
    private Type parent;

   /**
    * The name of the method (e.g. "main")
    */
    private String name;

   /**
    * The result type of this field or method
    */
    private Type type;

   /**
    * Attribute flags
    */
    private int flags;

   /**
    * Constructor
    */
    protected Member(Type parent, String name, Type type, int flags) {
        this.parent = parent;
        this.name   = name;
        this.type   = type;
        this.flags  = flags;
    }

   /**
    * Accessor
    */
    public Type parent() {
        return parent;
    }

   /**
    * Accessor
    */
    public String name() {
        return name;
    }

   /**
    * Accessor
    */
    public Type type() {
        return type;
    }

   /**
    * Accessor
    */
    protected int flags() {
        return flags;
    }

   /**
    * Accessor
    */
    public void setFlag(int bit) {
        flags |= bit;
    }

    public boolean isPublic() {
        return (flags & ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (flags & ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (flags & ACC_PROTECTED) != 0;
    }

    public boolean isStatic() {
        return (flags & ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (flags & ACC_FINAL) != 0;
    }

    public boolean isSynchronized() {
        return (flags & ACC_SYNCHRONIZED) != 0;
    }

    public boolean isNative() {
        return (flags & ACC_NATIVE) != 0;
    }

    public boolean isInterface() {
        return (flags & ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (flags & ACC_ABSTRACT) != 0;
    }

    public boolean isStrict() {
        return (flags & ACC_STRICT) != 0;
    }

    public boolean isVolatile() {
        return (flags & ACC_VOLATILE) != 0;
    }

    public boolean isTransient() {
        return (flags & ACC_TRANSIENT) != 0;
    }

    public boolean isLoaded() {
        return (flags & ACC_LOADED) != 0;
    }

    public void setLoaded() {
        assume((flags & ACC_LOADED) == 0, "member="+this);
        setFlag(ACC_LOADED);
    }

    public String toString() { return toString(true,false); }
    public String toString(boolean includePackageNames,boolean asSourceDecl) {
        return name + " " + type.toSignature(includePackageNames,asSourceDecl);
    }

    public Type[] getParms() {
        return VirtualMachine.ZEROTYPES;
    }

}