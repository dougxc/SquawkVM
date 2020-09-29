
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class TableSwitch extends Instruction {

    private Instruction key;
    private int low;
    private int high;
    private Target defaultTarget;
    private Target[] targets;

    public static TableSwitch create(Instruction key, int low, int high, Target defaultTarget) {
        return new TableSwitch(key, low, high, defaultTarget);
    }

    protected TableSwitch(Instruction key, int low, int high, Target defaultTarget) {
        super(null);
        this.key = key;
        this.low = low;
        this.high = high;
        this.defaultTarget = defaultTarget;
        this.targets = new Target[high-low+1];
    }

    public Instruction key()       { return key;           }
    public int low()               { return low;           }
    public int high()              { return high;          }
    public Target defaultTarget()  { return defaultTarget; }
    public Target[] targets()      { return targets;       }

    public void addTarget(int i, Target target) {
        targets[i-low] = target;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doTableSwitch(this);
    }

    public void visit(ParameterVisitor visitor) {
        key = visitor.doParameter(this, key);
    }
}
