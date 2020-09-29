
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class ArithmeticOp extends Instruction implements RuntimeConstants {

    private int op;
    private Instruction left;
    private Instruction right;

    public static Instruction create(int op, Instruction left, Instruction right) {
        return new ArithmeticOp(left.type(), op, left, right);
    }

    public static Instruction createCmp(int op, Instruction left, Instruction right) {
        return new ArithmeticOp(Type.INT, op, left, right);
    }

    protected ArithmeticOp(Type type, int op, Instruction left, Instruction right) {
        super(type);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public int op()             { return op;    }
    public Instruction left()   { return left;  }
    public Instruction right()  { return right; }

    public boolean canTrap() {
        return op == OP_DIV || op == OP_REM;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doArithmeticOp(this);
    }

    public void visit(ParameterVisitor visitor) {
        left  = visitor.doParameter(this, left);
        right = visitor.doParameter(this, right);
    }

}


