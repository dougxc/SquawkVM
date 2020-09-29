
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class StoreLocal extends Instruction {

    private Local local;
    private Instruction value;

    public static Instruction create(Local local, Instruction value) {
        return new StoreLocal(local, value);
    }

    protected StoreLocal(Local local, Instruction value) {
        super(null);
        assume(local != null);
        assume(value != null);

        this.local = local;
        this.value = value;
    }

    public Local local()        { return local; }
    public Instruction value()  { return value; }

    public void setLocal(Local local) {
        this.local = local;
    }

    public boolean isStoreToLocal(Local local) {
        return this.local == local;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreLocal(this);
    }

    public void visit(ParameterVisitor visitor) {
        assume(value != null);
        value = visitor.doParameter(this, value);
    }
}
