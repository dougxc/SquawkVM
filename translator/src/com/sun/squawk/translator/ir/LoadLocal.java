
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class LoadLocal extends Instruction {

    private Local local;

    public static Instruction create(Local local, Type type) {
        return new LoadLocal(local, type);
    }

    protected LoadLocal(Local local, Type type) {
        super(type);
        this.local = local;
    }

    public Local local() { return local; }


    public boolean isSimpleArgument() {
        return true;
    }

    public boolean isLoadFromLocal(Local local) {
        return this.local == local;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadLocal(this);
    }
}
