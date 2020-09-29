
package com.sun.squawk.translator;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;


public abstract class Method extends Member {

   /**
    * A list of Types where the first is the receiver type but which is always set to
    * Type.OBJECT. Then come the parameters followed by the return type.
    */
    private Type[] parms;

   /**
    * For methods this is the offset into an owning class's methods[] array (the "vtable" offset).
    * For interfaces this is the offset into the system interface table.
    */
    private int offset;

   /**
    * Private constructor
    */
    protected Method(Type parent, String name, Type type, int flags, Type[] parms) {
        super(parent, name, type, flags);
        assume(type.name().length() > 0);
        this.parms = parms;
    }

   /**
    * load
    */
    public void load() throws LinkageException {
        if (!isLoaded()) {
            parent().load();
            type().load();
            for (int i = 0 ; i < getParms().length ; i++) {
                getParms()[i].load();
            }
            setLoaded();
        }
    }

   /**
    * toString
    */
    public String toString() {
        return toString(true,false);
    }

   /**
    * toString
    */
    public String toString(boolean includePackageNames, boolean asSourceDecl) {
        String name = name() + "(";
        for (int i = 0 ; i < parms.length ; i++) {
            name += parms[i].toSignature(includePackageNames,asSourceDecl);
            if (asSourceDecl && i != (parms.length - 1)) {
                name+=",";
            }
        }
        name = name + ")";
        if (asSourceDecl) {
            name = type().toSignature(includePackageNames,asSourceDecl) + " " + name;
        }
        else {
            name += type().toSignature(includePackageNames,asSourceDecl);
        }
        return name;
    }

   /**
    * setHolder
    */
    public void setHolder(BytecodeHolder holder) {
        fatal("Method::setHolder");
    }

   /*
    * Convert to IR form
    */
    public Method asIrMethod() throws LinkageException {
        fatal("Method::asIrMethod");
        return null;
    }

   /*
    * Get activation record size
    */
    public int getActivationSize() {
        fatal("Method::getActivationSize");
        return 0;
    }

   /**
    * Set slot
    */
    public void setSlotOffset(int offset) {
        this.offset = offset;
    }

   /**
    * Get slot
    */
    public int getSlotOffset() {
        return offset;
    }

   /*
    * Return the list of parm types
    */
    public Type[] getParms() {
        return parms;
    }

   /**
    * isJustReturn
    */
    public abstract boolean isJustReturn();



    public Type[] getPhysicalLocalsCopy(int expectedSize) {
//prt("name ="+name());
//prt(" parms ="+parms.length);
//prtn("expectedSize ="+expectedSize);
        Type[] physicalLocals = new Type[expectedSize];
        int k = 0;
        int j = 0; //isStatic() ? 1 : 0;
        for ( ; j < parms.length ; j++) {
            physicalLocals[k] = parms[j];
            if (physicalLocals[k] == Type.DOUBLE) {
                physicalLocals[++k] = Type.DOUBLE2;
            }
            if (physicalLocals[k] == Type.LONG) {
                physicalLocals[++k] = Type.LONG2;
            }
            k++;
        }

       /*
        * All the other slots start off as bogus
        */
        for (; k < expectedSize ; j++) {
            physicalLocals[k++] = Type.BOGUS;
        }
        return physicalLocals;
    }


   /**
    * isProxyFor
    */
    public boolean isProxyFor(Method method) {
        return false;
    }


   /**
    * Public constructor
    */
    public static Method create(VirtualMachine vm, InputContext ctx, Type parent, String name, String descriptor, int flags) throws LinkageException {
        int count         = countParameters(descriptor);
        if (count < 0) {
            ctx.linkageException(Type.CLASSFORMATERROR, "Bad method signature: " + descriptor);
        }
        Type[] parmTypes  = new Type[count];
//prt(name+"->");
        Type   returnType = getParameterTypes(parent, descriptor, parmTypes);
        parmTypes = vm.internList(parmTypes);

        Method method = new ClassFileMethod(parent, name, returnType, flags, parmTypes);
        method.setFlag(flags);
        return method;
    }


   /**
    * Count the number of parameters in the method descriptor.
    */
    public static int countParameters(String desc) {
        int counter = 1; // + 1 bercaue both virtual and static have a receiver in this VM
        int index   = 1; // Skip the opening '(' in method signature
        int ch;
        while ((ch = desc.charAt(index)) != ')') {
            switch (ch) {
                case 'Z':       // boolean
                case 'B':       // byte
                case 'C':       // character
                case 'S':       // short
                case 'I':       // integer
                case 'F':       // float
                case 'J':       // long
                case 'D': {     // double
                    counter++;
                    index++;
                    break;
                }
                case 'L': {     // object pointer
                    counter++;
                    while (desc.charAt(index++) != ';') {
                        /* do nothing */
                    }
                    break;
                }
                case '[': {     // Array pointer
                    counter++;
                    while (desc.charAt(index) == '[') {
                        index++;
                    }
                    if (desc.charAt(index) == 'L') {
                        while (desc.charAt(++index) != ';') {
                            /* do nothing */
                        }
                    }
                    index++;
                    break;
                }
                default: {
                    return -1;
                }
            }
        }
        return counter;
    }

   /**
    * Create a list of types from the method descriptor.
    * The first type is always set to the parent type even if it is a static method
    * @return the method return
    */
    public static Type getParameterTypes(Type parent, String desc, Type[] types) {
        int counter = 0;
        int index   = 1;                    // Skip the opening '(' in method signature
        boolean allowVoid = false;
        int length  = desc.length();
        types[counter++] = Type.OBJECT;     // reserved for receiver
        Type res = null;

//prtn("desc="+desc+" length="+length+" types="+types.length);

        assume(desc.charAt(0) == '(');

        while (index < length) {
            int ch = desc.charAt(index);
//prtn("ch="+(char)ch+"indx="+index);
            switch (ch) {
                case 'Z': res = Type.BOOLEAN;      index++; break;
                case 'B': res = Type.BYTE;         index++; break;
                case 'C': res = Type.CHAR;         index++; break;
                case 'S': res = Type.SHORT;        index++; break;
                case 'I': res = Type.INT;          index++; break;
                case 'F': res = Type.FLOAT;        index++; break;
                case 'J': res = Type.LONG;         index++; break;
                case 'D': res = Type.DOUBLE;       index++; break;

                case 'L': {
                    int start = index;
                    while (desc.charAt(index++) != ';') {
                        /* do nothing */
                    }
                    res = parent.getVM().createType(desc.substring(start, index));
                    break;
                }
                case '[': {
                    int start = index;
                    while (desc.charAt(index) == '[') {
                        index++;
                    }
                    if (desc.charAt(index) == 'L') {
                        while (desc.charAt(++index) != ';') {
                            /* do nothing */
                        }
                    }
                    index++;
                    res = parent.getVM().createType(desc.substring(start, index));
                    break;
                }
                case ')': {         // Ignore this
                    allowVoid = true;
                    index++;
                    continue;
                }
                case 'V': {
                    assume(allowVoid);
                    res = Type.VOID;
                    index++;
                    break;
                }
                default: {
                    shouldNotReachHere();
                }
            }
            if (counter < types.length) {
                types[counter++] = res;
            }

        }

//prtn(" desc="+desc+" ret="+res);

        assume(counter == types.length, "ctr="+counter);
        return res;
    }



}
