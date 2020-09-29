
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class Cast extends Instruction {

    private Instruction value;

    public static Instruction create(Local to, Instruction value) {
        return new Cast(to, value);
    }

    protected Cast(Local to, Instruction value) {
        super(null);
        setResultLocal(to);
        this.value = value;
    }

    public Instruction value()  { return value; }

    public void visit(InstructionVisitor visitor) {
        visitor.doCast(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
