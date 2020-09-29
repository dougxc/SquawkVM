
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class MethodHeader extends Instruction {

    private int icount;
    private Object[] locals;

    public static Instruction create(int icount, Object[] locals) {
        return new MethodHeader(icount, locals);
    }

    protected MethodHeader(int icount, Object[] locals) {
        super(null);
        this.icount = icount;
        this.locals = locals;
    }

    public void  addToInstructionCount(int count) {
        icount += count;
    }

    public int   getInstructionCount() { return icount;           }
    public int   getLocalCount()       { return locals.length;    }
    public Local getLocal(int n)       { return (Local)locals[n]; }
    public boolean includes(Object o) {
        for(int i = 0 ; i < locals.length ; i++) {
            if (locals[i] == o) {
                return true;
            }
        }
        return false;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doMethodHeader(this);
    }

}
