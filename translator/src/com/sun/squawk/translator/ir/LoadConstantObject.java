
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class LoadConstantObject extends Instruction {

    Type parent;
    int index;

    public static LoadConstantObject create(Type type, int index, Type parent) {
        return new LoadConstantObject(type, index, parent);
    }

    LoadConstantObject(Type type, int index, Type parent) {
        super(type);
        this.index = index;
        this.parent = parent;
    }

    public String toString() {
        return "const("+parent.getStaticObject(index)+")";
    }

    public String toXml() {
        return "#"+index;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadConstantObject(this);
    }
}
