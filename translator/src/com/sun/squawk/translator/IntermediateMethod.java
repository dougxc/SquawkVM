
package com.sun.squawk.translator;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;


public class IntermediateMethod extends Method {

    private Instruction[] list;

   /**
    * Private constructor
    */
    protected IntermediateMethod(Type parent, String name, Type type, int flags, Type[] parms, Instruction ir) {
        super(parent, name, type, flags, parms);

       /*
        * Read the IR into an array
        */
        if (ir != null) {
            MethodHeader hdr = (MethodHeader)ir;
            list = new Instruction[hdr.getInstructionCount()];
            int i = 0;
            while (ir != null) {
               /*
                * Do not include instructions eliminated by the optomizer
                */
                if (ir.getIP() != -1) {
                    ir.setIP(i-1);          // Count from first real instruction, not the MethodHeader
                    list[i++] = ir;
                }

               /*
                * The following will allow the now dead instructions to be garbage collected.
                */
                Instruction next = ir.getNext();
                ir.setNext(null);
                ir = next;
            }
            assume(i == list.length, "this="+this+" i="+i+" list.length="+list.length);

           /*
            * Assign offsets to the local variables
            */
            int localCount = hdr.getLocalCount();
            int offset = 0;
            for(i = 0 ; i < localCount ; i++) {
                Local local = hdr.getLocal(i);
                local.setOffset(offset);
                offset++;
                if (VirtualMachine.LONGSARETWOWORDS && local.slotType() == Type.BASIC_LONG) {
                    offset++;
                }
            }
            //activationSize = offset;

           ///*
           // * Assign offsets to the local variables
           // */
           // int localCount = hdr.getLocalCount();
           // for(i = 0 ; i < localCount ; i++) {
           //     Local local = hdr.getLocal(i);
           //     local.setOffset(i);
           // }
        }
    }

    public Method asIrMethod() {
        return this;
    }

    public int getActivationSize() {
        MethodHeader hdr = (MethodHeader)list[0];
        return hdr.getLocalCount();
    }

    public Instruction[] getInstructions() {
        return list;
    }

    public Instruction getInstruction(int ip) {
        return list[ip];
    }

    public boolean isJustReturn() {
        if (list == null) {
            return false;
        }
        assume(list.length != 1);
        return list.length == 2;
    }

}
