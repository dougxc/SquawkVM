
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class InstanceOf extends Instruction {

    private Type checkType;
    private Instruction value;

    public static Instruction create(Type checkType, Instruction value) {
        return new InstanceOf(checkType, value);
    }

    protected InstanceOf(Type checkType, Instruction value) {
        super(Type.INT);
        this.checkType = checkType;
        this.value = value;

    }

    public Type checkType()     { return checkType; }
    public Instruction value()  { return value;     }

    public void visit(InstructionVisitor visitor) {
        visitor.doInstanceOf(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
