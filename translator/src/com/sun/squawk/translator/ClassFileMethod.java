
package com.sun.squawk.translator;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;


import java.io.IOException;

public class ClassFileMethod extends Method {

   /**
    * The bytecode holder
    */
    private BytecodeHolder holder;

   /**
    * Private constructor
    */
    protected ClassFileMethod(Type parent, String name, Type type, int flags, Type[] parms) {
        super(parent, name, type, flags, parms);
    }

   /**
    * setHolder
    */
    public void setHolder(BytecodeHolder holder) {
        this.holder = holder;
    }

   /**
    * isJustReturn
    */
    public boolean isJustReturn() {
        if (holder == null) {
            return false;
        }
        return holder.isJustReturn();
    }

   /**
    * getIR
    */
    private Instruction getIR(BytecodeHolder holder) throws LinkageException {
        Instruction ir = null;
        if (holder != null) {
            try {
                assume(getIRcount++ == 0); // check that serilization is working
                ir = holder.getIR();
            } finally {
                getIRcount--;
            }
        }
        return ir;
    }
    private static int getIRcount = 0;

   /**
    * asIrMethod
    */
    public Method asIrMethod() throws LinkageException {
        Instruction ir = getIR(holder);
        IntermediateMethod res = new IntermediateMethod(parent(), name(), type(), flags(), getParms(), ir);
        res.setSlotOffset(getSlotOffset());
        return res;
    }

}
