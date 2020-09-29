
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class NegateOp extends Instruction {

    private Instruction value;

    public static Instruction create(Instruction value) {
        return new NegateOp(value);
    }

    protected NegateOp(Instruction value) {
        super(value.type());
        this.value = value;
    }

    public Instruction value() { return value; }

    public void visit(InstructionVisitor visitor) {
        visitor.doNegateOp(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
