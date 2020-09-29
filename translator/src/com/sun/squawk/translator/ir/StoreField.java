
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class StoreField extends Instruction {

    private Field field;
    private Instruction ref;
    private Instruction value;

    public static Instruction create(Field field, Instruction ref, Instruction value) {
        if (field.getSlotOffset() == -1) {
            /*
             * This is must be initialization of a constant static field (only
             * javac 1.4 seems to do this). Given that compilers will have inlined
             * this value, there is no need to do the initialization at runtime.
             */
            assume(field.getInitialValue() != null);
            assume(field.isFinal());
            if (!value.wasDuped()) {
                value.setResultLocal(null);
            }
            return null;
        }
        return new StoreField(field, ref, value);
    }

    protected StoreField(Field field, Instruction ref, Instruction value) {
        super(null);
        this.field = field;
        this.ref   = ref;
        this.value = value;
        assume(field.getSlotOffset() != -1);
    }

    public Field field()        { return field; }
    public Instruction ref()    { return ref; }
    public Instruction value()  { return value; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreField(this);
    }

    public void visit(ParameterVisitor visitor) {
        if (ref != null) {
            ref = visitor.doParameter(this, ref);
        }
        value = visitor.doParameter(this, value);
    }
}
