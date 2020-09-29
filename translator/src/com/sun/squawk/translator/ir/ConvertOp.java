
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class ConvertOp extends Instruction {

    private int op;
    private Instruction value;

    public static Instruction create(int op, Type toType, Instruction value) {
        return new ConvertOp(op, toType, value);
    }

    protected ConvertOp(int op, Type toType, Instruction value) {
        super(toType);
        this.op = op;
        this.value = value;
    }

    public int op()             { return op;    };
    public Instruction value()  { return value; }

    public void visit(InstructionVisitor visitor) {
        visitor.doConvertOp(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
