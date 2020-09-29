
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class HandlerEndPoint extends BytecodeAddress implements RuntimeConstants {

   /**
    * Exception target
    */
    private Target target;

   /**
    * Constructor
    */
    public HandlerEndPoint(int ip, Target target) {
        super(ip);
        this.target = target;
    }

    public Target target() { return target; }

    public int sortKey() {
        return (getIP() << 4) + 0;
    }

    public int subKey() {
        return target.getIP();
    }


    public int opcode() {
        return  opc_handlerend;
    }
}
