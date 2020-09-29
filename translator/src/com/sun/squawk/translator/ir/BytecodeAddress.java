
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public abstract class BytecodeAddress extends BaseFunctions {

   /**
    * IP address in the java bytecodes
    */
    private int ip;


   /**
    * Constructor
    */
    public BytecodeAddress(int ip) {
        this.ip = ip;
    }

   /**
    * Get the ip
    */
    public int getIP() {
        return ip;
    }

   /**
    * Get the ip
    */
    public int getBytecodeIP() {
        return ip;
    }

    public abstract int sortKey();
    public abstract int subKey();
    public abstract int opcode();
}
