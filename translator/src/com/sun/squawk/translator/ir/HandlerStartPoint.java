
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class HandlerStartPoint extends BytecodeAddress implements RuntimeConstants {

   /**
    * Exception target
    */
    private Target target;

   /**
    * Constructor
    */
    public HandlerStartPoint(int ip, Target target) {
        super(ip);
        this.target = target;
    }

    public Target target() { return target; }

    public int sortKey() {
        return (getIP() << 4) + 1;
    }

    public int subKey() {
        return 0xFFFF - target.getIP();
    }

    public int opcode() {
        return  opc_handlerstart;
    }

}
