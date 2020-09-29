
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class LookupSwitch extends Instruction {

    private Instruction key;
    private int[] matches;
    private Target[] targets;
    private Target defaultTarget;

    public static LookupSwitch create(Instruction key, int npairs, Target defaultTarget) {
        return new LookupSwitch(key, npairs, defaultTarget);
    }

    protected LookupSwitch(Instruction key, int npairs, Target defaultTarget) {
        super(null);
        this.key = key;
        this.matches = new int[npairs];
        this.targets = new Target[npairs];
        this.defaultTarget = defaultTarget;
    }

    public void addTarget(int index, int match, Target target) {
        matches[index] = match;
        targets[index] = target;
    }

    public Instruction key()      { return key;           }
    public int[] matches()        { return matches;       }
    public Target[] targets()     { return targets;       }
    public Target defaultTarget() { return defaultTarget; }

    public void visit(InstructionVisitor visitor) {
        visitor.doLookupSwitch(this);
    }

    public void visit(ParameterVisitor visitor) {
        key = visitor.doParameter(this, key);
    }
}
