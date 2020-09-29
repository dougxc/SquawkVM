
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;

public class Invoke extends Instruction {

    public final static int VIRTUAL = 1;
    public final static int STATIC  = 2;
    public final static int SPECIAL = 3;

    private Method method;
    //private int  offset;
    private Instruction[] parms;
    private int invokeType;
    boolean ignore;


    public static Instruction create(Method method, Instruction[] parms, int invokeType, boolean ignore) {
        return new Invoke(method, parms, invokeType, ignore);
    }

    protected Invoke(Method method, Instruction[] parms, int invokeType, boolean ignore) {
        super(method.type());
        this.method     = method;
        this.parms      = parms;
        this.invokeType = invokeType;
        this.ignore     = ignore;
    }

    public Method method()          { return method; }
    public int    offset()          { return method.getSlotOffset(); }
    public Instruction[] parms()    { return parms;  }
    public boolean isStatic()       { return invokeType == STATIC;  }
    public boolean isSpecial()      { return invokeType == SPECIAL; }
    public boolean ignore()         { return ignore; }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doInvoke(this);
    }

    public void visit(ParameterVisitor visitor) {
        for (int i = 0 ; i < parms.length ; i++) {
            parms[i] = visitor.doParameter(this, parms[i]);
        }
    }
}
