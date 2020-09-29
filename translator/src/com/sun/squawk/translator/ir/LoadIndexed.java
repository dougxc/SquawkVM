
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.loader.*;

public class LoadIndexed extends Instruction {

    private Instruction array;
    private Instruction index;

    public static Instruction create(Instruction array, Instruction index, Type type) {
        if (array.type() == Type.NULLOBJECT) {
           /*
            * If the following occurs:
            *
            *   Object obj = null;
            *   Something x = obj[0];
            *
            * The error should be deteced at run time not compile time.
            */
            return new LoadIndexed(type.elementType(), array, index);
        } else {
            return new LoadIndexed(array.type().elementType(), array, index);
        }
    }

    private LoadIndexed(Type type, Instruction array, Instruction index) {
        super(type);
        this.array = array;
        this.index = index;
    }

    public Instruction array() { return array; }
    public Instruction index() { return index; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadIndexed(this);
    }

    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        index = visitor.doParameter(this, index);
    }
}
