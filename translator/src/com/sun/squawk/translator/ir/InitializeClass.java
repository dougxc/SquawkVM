
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;
import  java.util.Enumeration;

public class InitializeClass extends Instruction implements com.sun.squawk.vm.ClassNumbers {

    private Type parent;
/*
    static boolean needsClinit(Type type) {
        if (type == Type.OBJECT) {
            return false;
        }
        if (type.getClinit() != null) {
            return true;
        }
        return (needsClinit(type.superClass()));
    }
*/
    public static Instruction create(Type type) {
       /*
        * Some types are initialized by the system. If this is one of those
        * then return null because no instruction is required.
        */
        if (type.getID() <= CNO_InitLimit) {
            return null;
        }
// Cannot do this as the VM relys on class initialization to ensure the methods of
// a class are loaded before the class is used!
//        if (!needsClinit(type)) {
//            return null;
//        }
//System.out.println("+++++++++clinit "+type);
        return new InitializeClass(type);
    }

    private InitializeClass(Type parent) {
        super(null);
        this.parent = parent;
    }

    public Type parent()  { return parent; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doInitializeClass(this);
    }
}
