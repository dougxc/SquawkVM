
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class Throw extends Instruction {

    private Instruction value;

    public static Instruction create(Instruction value) {
        return new Throw(value);
    }

    protected Throw(Instruction value) {
        super(null);
        this.value = value;
    }

    public Instruction value() { return value; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doThrow(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
