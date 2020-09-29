
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class CheckStore extends Instruction {

    private Instruction array;
    private Instruction value;

    public static Instruction create(Instruction array, Instruction value) {
        return new CheckStore(array, value);
    }

    protected CheckStore(Instruction array, Instruction value) {
        super(null);
        this.array = array;
        this.value = value;
    }

    public Instruction array()  { return array;     }
    public Instruction value()  { return value;     }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doCheckStore(this);
    }

    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        value = visitor.doParameter(this, value);
    }
}
