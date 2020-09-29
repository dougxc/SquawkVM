
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class StoreIndexed extends Instruction {

    private Instruction array;
    private Instruction index;
    private Instruction value;
    private Type basicType;

    public static Instruction create(Instruction array, Instruction index, Instruction value, Type basicType) {
        return new StoreIndexed(array, index, value, basicType);
    }

    protected StoreIndexed(Instruction array, Instruction index, Instruction value, Type basicType) {
        super(null);
        this.array     = array;
        this.index     = index;
        this.value     = value;
        this.basicType = basicType;
    }

    public Instruction array()  { return array; }
    public Instruction index()  { return index; }
    public Instruction value()  { return value; }
    public Type basicType()     { return basicType; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreIndexed(this);
    }

    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        index = visitor.doParameter(this, index);
        value = visitor.doParameter(this, value);
    }
}
