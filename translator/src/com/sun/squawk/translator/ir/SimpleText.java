
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class SimpleText extends Instruction {

    private String op;
    private Instruction p1;
    private Instruction p2;

    public static Instruction create(String op, Local result, Instruction p1) {
        return new SimpleText(op, result, p1, null);
    }

    public static Instruction create(String op, Local result, Instruction p1, Instruction p2) {
        return new SimpleText(op, result, p1, p2);
    }

    protected SimpleText(String op, Local result, Instruction p1, Instruction p2) {
        super(null);
        setResultLocal(result);
        this.op        = op;
        this.p1        = p1;
        this.p2        = p2;
    }

    public String op()              { return op; }
    public Instruction p1()         { return p1;  }
    public Instruction p2()         { return p2;  }

    public void visit(InstructionVisitor visitor) {
        visitor.doSimpleText(this);
    }
}
