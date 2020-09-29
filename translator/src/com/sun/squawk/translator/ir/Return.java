
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class Return extends Instruction {

    private Instruction value;

    public static Instruction create() {
        return new Return(null);
    }

    public static Instruction create(Instruction value) {
        return new Return(value);
    }

    protected Return(Instruction value) {
        super(value == null ? Type.VOID : value.type());
        this.value = value;
    }

    public Instruction value() { return value; }

    public void visit(InstructionVisitor visitor) {
        visitor.doReturn(this);
    }

    public void visit(ParameterVisitor visitor) {
        if (value != null) {
            value = visitor.doParameter(this, value);
        }
    }
}
