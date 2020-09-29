
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.loader.*;
import  com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;

public class Target extends BytecodeAddress implements RuntimeConstants {

   /**
    * The BranchTarget or ExceptionTarget that points here
    */
    private Instruction targetInstruction;

   /**
    * Flag to indicate that this is the target of a backward branch
    */
    boolean isBackwardTarget = false;

   /**
    * Exception class type (if an exception target)
    */
    private Type exceptionTargetType = null;

   /**
    * Stackmap structures
    */
    private Type[][] mapArray;

   /**
    * This is a holder for a phi merge parameter(s)
    */
    private Instruction[] phiParameters;

   /*
    * The follwing is used to chain together active exception handlers
    * in the graph builder
    */
    private Target nextTarget;

   /**
    * Constructor
    */
    public Target(int ip, Type[][] mapArray) {
        super(ip);
        this.mapArray = mapArray;
    }

    public void setExceptionTargetType(Type type) {
        exceptionTargetType = type;
    }

    public Type getExceptionTargetType() {
        return exceptionTargetType;
    }

    public boolean isExceptionTarget() {
        return exceptionTargetType != null;
    }

    public void setTargetInstruction(Instruction inst) {
//if(targetInstruction != null) {
//    System.out.println("TargetInstrucyion was "+targetInstruction+ " is now "+inst);
//}
        assume(targetInstruction == null);
        targetInstruction = inst;
    }

    public Instruction getTargetInstruction() {
        return targetInstruction;
    }

    public int getSP() {
        return mapArray[0].length;
    }

   /**
    * Get the ip
    *
    * If there is a target instruction for this target then get the ip from that.
    * This allows the re-addressing of instructions in the IR. Otherwize use the
    * ip value that came from the origional bytecodes.
    */
    public int getIP() {
        if (targetInstruction != null && targetInstruction.getIP() > 0) {
//prtn("1->"+targetInstruction.getIP()+" for "+getBytecodeIP()+" in "+this);
           return targetInstruction.getIP();
        } else {
//prtn("2->"+getIP());
            return getBytecodeIP();
        }
    }


    public String toString() {
        return (exceptionTargetType == null ? "Target(": "ExceptionTarget(type:"+exceptionTargetType+" ") +
            "ip:"+getIP()+")";
    }

    public Type[] getPhysicalLocals() {
        return mapArray[StackMap.PHYSICALLOCALS];
    }

    public Type[] getStack() {
        return mapArray[StackMap.STACK];
    }

    public void addPhiParameter(Instruction phiParm) {
        assume(!isExceptionTarget());
        if (phiParameters == null) {
            phiParameters = new Instruction[1];
            phiParameters[0] = phiParm;
        } else {
            Instruction[] oldParms = phiParameters;
            Instruction[] newParms = new Instruction[oldParms.length+1];
            System.arraycopy(oldParms, 0, newParms, 0, oldParms.length);
            newParms[oldParms.length] = phiParm;
            phiParameters = newParms;
        }
    }

    public Instruction[] getPhiParameters() {
        assume(!isExceptionTarget());
        Instruction[] result = phiParameters;
        phiParameters = null;
        return result;
    }

    public int sortKey() {
        return (getIP() << 4) + 2;
    }

    public int subKey() {
        return getIP();
    }


    public int opcode() {
        return isExceptionTarget() ? opc_exceptiontarget : opc_branchtarget;
    }

    public Target getNextTarget() {
        return nextTarget;
    }

    public void setNextTarget(Target target) {
        nextTarget = target;
    }

    public boolean isBackwardTarget() {
        return isBackwardTarget;
    };

    public void isTargetFor(Instruction inst) {
        if (inst.getIP() > targetInstruction.getIP()) {
            isBackwardTarget = true;
        }
    }


}
