
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class MonitorEnter extends Instruction {

    private Instruction value;

    public static Instruction create(Instruction value) {
        return new MonitorEnter(value);
    }

    protected MonitorEnter(Instruction value) {
        super(null);
        this.value = value;
    }

    public Instruction value() { return value; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doMonitorEnter(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }
}
