
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class LoadField extends Instruction {

    private Field field;
    private Instruction ref;

    public static Instruction create(Field field, Instruction ref) {

       /*
        * Return a constant if the field has no slot.
        * (It appears that the 1.3 javac resolves constants anyway.)
        */
        if (field.getSlotOffset() == -1) {
            if (field.parent().getVM().optimizationLevel() > 0) {
                Object o = field.getInitialValue();
                assume(o != null);
                if        (o instanceof Integer) {
                    return LoadConstant.create(((Integer)o).intValue());
                } else if (o instanceof Long) {
                    return LoadConstant.create(((Long)o).longValue());
                } else if (o instanceof Float) {
                    return LoadConstant.create(((Float)o).floatValue());
                } else if (o instanceof Double) {
                    return LoadConstant.create(((Double)o).doubleValue());
                //} else if (o instanceof String) {
                //    return LoadConstant.create(((String)o));
                }
                shouldNotReachHere();
                return null;
            }
        }
        return new LoadField(field, ref);
    }

    protected LoadField(Field field, Instruction ref) {
        super(field.type());
        this.field = field;
        this.ref   = ref;

    }

    public Field field()       { return field; }
    public Instruction ref()   { return ref; }


    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadField(this);
    }

    public void visit(ParameterVisitor visitor) {
        if(ref != null) {
            ref = visitor.doParameter(this, ref);
        }
    }
}
