
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class NewMultiArray extends Instruction {

    private Instruction[] dimList;

    public static Instruction create(Type type, Instruction[] dimList) {
        return new NewMultiArray(type, dimList);
    }

    protected NewMultiArray(Type type, Instruction[] dimList) {
        super(type);
        this.dimList = dimList;
    }

    public Instruction[] dimList() { return dimList;   }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNewMultiArray(this);
    }

    public void visit(ParameterVisitor visitor) {
        for (int i = 0 ; i < dimList.length ; i++) {
            dimList[i] = visitor.doParameter(this, dimList[i]);
        }
    }
}
