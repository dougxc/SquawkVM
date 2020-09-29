
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class HandlerExit extends Instruction {

    private Target target;

    public static Instruction create(Target target) {
        return new HandlerExit(target);
    }

    protected HandlerExit(Target target) {
        super(null);
        this.target = target;
    }

    public Target target() { return target; }

    public void visit(InstructionVisitor visitor) {
        visitor.doHandlerExit(this);
    }

}
