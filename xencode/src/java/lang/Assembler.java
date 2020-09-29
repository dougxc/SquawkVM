/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.cldc.io.connections.*;

/**
 * Squawk bytecode assembler.
 */
class Assembler extends java.lang.AbstractAssembler {

    StringBuffer sb;
    boolean output = true;

    /**
     * Constructor.
     */
    Assembler(ClassBase clazz, SquawkFileParser p, int[] localOffsetTable)
    {
        super(clazz,p,localOffsetTable);
        sb = new StringBuffer();
        if (!clazz.className.equals("com.sun.javacard.samples.JavaPurse.JavaPurse")) {
      //      output = false;
        }
    }


    /**
     * emitLongOperand
     */
     void emitLongOperand(int rs, byte[]src) {
     }

    void trace(Object s) {
        //System.out.print(s);
        //System.out.print(" ");
        sb.append(s);
        sb.append(" ");
    }

    void traceInvoke(byte[] cls, byte[] slot) {
        int slotNum = decodeSlot(slot);
        if (cls[0] > 0) {
            trace("+ INVOKES");
            traceOperand(cls);
        } else {
            trace("+ INVOKE"+(slotNum < 0 ? "I" : ""));
        }
        traceSlot(slot);
    }

    void traceMethod(String name) {
        trace("* "+name);
        traceln();
    }

    void traceInstructionStart(int address) {
        trace("- "+address);
        traceln();
    }

    void traceInstructionEnd() {
        traceln();
    }

    void traceln() {
        sb.append("\n");
    }

    void traceMethodEnd() {
        traceln();
        if (output) {
            System.out.print(sb.toString());
        }
    }

    int getUnsignedBytecode(byte[]buf, int addr)  {
        return buf[addr] & 0xFF;
    }

    int SIMPLESIGNSHIFT = 32 - ENC_HIGH_BYTE_BITS;

   /*
    * traceSimple
    */
    int traceSimple(String str, int b) {
        if ((b & ENC_CONST) != 0) {
            b = (b & 0x3f) << SIMPLESIGNSHIFT >> SIMPLESIGNSHIFT;
            trace(str+"#"+b);
        } else {
            trace(str+"l"+b);
            b = 99999999;
        }
        return b;
    }


    int traceSimple(int b) {
        return traceSimple("", b);
    }

   /*
    * traceOperand
    */
    void traceOperand(byte[]buf) {
        if (buf[0] == (byte)0) {
            return;
        }
        int ip = 0;
        int lth = buf[ip++];

        int b = getUnsignedBytecode(buf, ip++);
        if ((b & ENC_COMPLEX) == 0) {
            p.check(lth == 1,"bad lth != 1 "+lth+ " b="+b);
            b = traceSimple(b);
        } else {
            if ((b & ENC_CONST) == 0) {
                p.check(lth == 2,"bad lth != 2 "+lth);
                b = ((b & 0x3f) << 8) + getUnsignedBytecode(buf, ip++);
                trace("l"+b);
                b = 99999999;
            } else if ((b & (ENC_STATIC)) == 0) {
                p.check(lth == 5,"bad lth != 5 "+lth);
                b = (getUnsignedBytecode(buf, ip++) << 24) +
                    (getUnsignedBytecode(buf, ip++) << 16) +
                    (getUnsignedBytecode(buf, ip++) << 8)  +
                    (getUnsignedBytecode(buf, ip++) << 0);
                trace("#"+b);
            } else {
                p.check(lth == 3,"bad lth != 3 "+lth);
                b = (getUnsignedBytecode(buf, ip++) << 8)  +
                    (getUnsignedBytecode(buf, ip++) << 0);
                trace("&"+b);
                b = 99999999;
            }
        }
    }


   /*
    * traceOperand
    */
    void traceIConst(byte[]buf) {
        int ip = 0;
        int lth = buf[ip++];
        p.check(lth == 4,"bad lth != 4 "+lth);
        int b = (getUnsignedBytecode(buf, ip++) << 24) +
                (getUnsignedBytecode(buf, ip++) << 16) +
                (getUnsignedBytecode(buf, ip++) << 8)  +
                (getUnsignedBytecode(buf, ip++) << 0);
        trace("#"+b);
    }





   /*
    * decodeSlot
    */
    int decodeSlot(byte[]buf) {
        int ip = 0;
        int lth = buf[ip++];
        int offset = getUnsignedBytecode(buf, ip++);
        if (offset > 0x7F) {
            offset = (offset & 0x7F) << 8 | getUnsignedBytecode(buf, ip++);
            offset = (offset << 21) >> 21;  /* Sign extend 11 bits */
        }
        return offset;
    }


   /*
    * traceSlot
    */
    void traceSlot(byte[]buf) {
        trace("@"+decodeSlot(buf));
    }



   /*
    * traceTarget
    */
    void traceTarget(int here, byte[]buf) {
        int ip = 0;
        int lth = buf[ip++];
        int b1 = (byte)(getUnsignedBytecode(buf, ip++));
        int b2 = getUnsignedBytecode(buf, ip++);
        int t = (b1 << 8) + b2; // note: signed << 8 + unsigned
        trace("$"+t+" ("+(t-here)+")");
    }


   /*
    * traceTarget
    */
    void traceBC(int bc) {
        if (bc != 0xFF) {
           trace("\n+ "+Disassembler.getOpString(bc));
        }
    }
}
