
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class ArrayLength extends Instruction {

    private Instruction array;

    public static Instruction create(Instruction array) {
        return new ArrayLength(array);
    }

    protected ArrayLength(Instruction array) {
        super(Type.INT);
        this.array = array;
    }

    public Instruction array()  { return array; }

    public void visit(InstructionVisitor visitor) {
        visitor.doArrayLength(this);
    }

    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
    }
}
