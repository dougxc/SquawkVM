
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class Phi extends Instruction {

    private Target target;
    private Instruction[] parms;

    public static Phi create(Target target, Type type, Instruction[] parms) {
        return new Phi(target, type, parms);
    }

    protected Phi(Target target, Type type, Instruction[] parms) {
        super(type);
        this.target = target;
        this.parms = parms;
        target.setTargetInstruction(this);
    }

    public Target target()        { return target; }
    public Instruction[] parms()  { return parms;  }

    public boolean isTarget() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doPhi(this);
    }

    public void visit(ParameterVisitor visitor) {
        if (parms != null) {
            for (int i = 0 ; i < parms.length ; i++) {
                parms[i] = visitor.doParameter(this, parms[i]);
            }
        }
    }

}
