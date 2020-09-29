
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class Local extends BaseFunctions {

    private int slotType;
    private int useCount;
    private int parameterNumber = -1;
    private int id;
    private int offset = -1;
    private Instruction causeForClearing;

    public Local(int slotType) {
        this.slotType = slotType;
    }

    public int slotType() {
       return slotType;
    }

   // public void incrementUseCount() {
   //     useCount++;
   // }

   // public void decrementUseCount() {
   //     --useCount;
   // }

    public void addUseCount(int n) {
        useCount += n;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setParameterNumber(int n) {
        parameterNumber = n;
    }

    public int getParameterNumber() {
        return parameterNumber;
    }

    public void setID(int n) {
        id = n;
    }

    public String toString() {
        return "l"+idstr();
    }

    protected String idstr() {
        if (slotType == Type.BASIC_INT) {
            return ""+offset;
        } else if(slotType == Type.BASIC_LONG) {
            return ""+offset+"L";
        } else {
            return ""+offset+"#";
        }
    }

    public String xmltag() {
        if (slotType == Type.BASIC_INT) {
            return "word";
        } else if(slotType == Type.BASIC_LONG) {
            return "dword";
        } else {
            return "ref";
        }
    }


    public void setOffset(int n) {
        offset = n;
    }

    public int getOffset() {
        return offset;
    }



    //void incrementReferenceCount() {
    //    shouldNotReachHere();
    //}

    //boolean decrementReferenceCount() {
    //    shouldNotReachHere();
    //}

    public void setCauseForClearing(Instruction inst) {
        causeForClearing = inst;
    }

    public Instruction getCauseForClearing() {
        return causeForClearing;
    }
}

