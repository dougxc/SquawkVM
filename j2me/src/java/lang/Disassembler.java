//J2C:interp.c **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */


package java.lang;

import com.sun.squawk.util.*;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Enumeration;

public class Disassembler implements NativeOpcodes {



/*---------------------------------------------------------------------------*\
 *                            Forward References                             *
\*---------------------------------------------------------------------------*/

    int     getArgumentCount()                  { return 1; }
    int     getArgumentChar(int arg, int ch)    { return 1; }
    boolean getTraceInstructions()              { return true; }
    void    setTraceInstructions(boolean value) {}


    /*
     * fatalVMError
     */
    void fatalVMError(String msg) {
        throw new RuntimeException(msg);
    }

    /*
     * shouldNotReachHere
     */
    void shouldNotReachHere() {
        throw new RuntimeException("shouldNotReachHere()");
    }

//IFC//#ifndef PRODUCTION
    /*
     * assume
     */
    void assume(boolean b) {
         if (!b) {
             throw new RuntimeException("Assume Failure");
         }
    }

/*---------------------------------------------------------------------------*\
 *                            xxxxxxxxxxxxxxxxxxx                            *
\*---------------------------------------------------------------------------*/

    public  static final int BC_OPS    = 0;
    public  static final int MATH_OPS  = 1;
    public  static final int CHAN0_OPS = 2;
    public  static final int CHANN_OPS = 3;

    private static IntHashtable opcodes  = new IntHashtable();
    private static IntHashtable chan0ops = new IntHashtable();
    private static IntHashtable chanNops = new IntHashtable();
    private static IntHashtable mathOps  = new IntHashtable();
    private static Hashtable allOps = new Hashtable();
    static {
        allOps.put("bc",    opcodes);
        allOps.put("chan0", chan0ops);
        allOps.put("chanN", chanNops);
        allOps.put("math",  mathOps);
    }

    public static String getOpString(int op) {
        return getOpString(op,BC_OPS);
    }
    public static String getOpString(int op, int type) {
        IntHashtable ops = null;
        switch(type) {
            case BC_OPS:    ops = opcodes;  break;
            case MATH_OPS:  ops = mathOps;  break;
            case CHAN0_OPS: ops = chan0ops; break;
            case CHANN_OPS: ops = chanNops; break;
            default:
                throw new RuntimeException("unknown op type: " + type);
        }
        String s = (String)ops.get(op);
        if (s == null) {
           s = "<unknown "+type+" op: "+op+">";
        }
        return s;
    }

    static {
        opcodes.put(OPC_UNUSED        , "UNUSED        ");
        opcodes.put(OPC_NOP           , "NOP           ");
        opcodes.put(OPC_BREAK         , "BREAK         ");
        opcodes.put(OPC_ADDI          , "ADDI          ");
        opcodes.put(OPC_SUBI          , "SUBI          ");
        opcodes.put(OPC_MULI          , "MULI          ");
        opcodes.put(OPC_DIVI          , "DIVI          ");
        opcodes.put(OPC_REMI          , "REMI          ");
        opcodes.put(OPC_MOVI          , "MOVI          ");
        opcodes.put(OPC_NEGI          , "NEGI          ");
        opcodes.put(OPC_ANDI          , "ANDI          ");
        opcodes.put(OPC_ORRI          , "ORRI          ");
        opcodes.put(OPC_XORI          , "XORI          ");
        opcodes.put(OPC_SLLI          , "SLLI          ");
        opcodes.put(OPC_SRLI          , "SRLI          ");
        opcodes.put(OPC_SRAI          , "SRAI          ");
        opcodes.put(OPC_I2B           , "I2B           ");
        opcodes.put(OPC_I2S           , "I2S           ");
        opcodes.put(OPC_I2C           , "I2C           ");
        opcodes.put(OPC_I2F           , "I2F           ");
        opcodes.put(OPC_ADDF          , "ADDF          ");
        opcodes.put(OPC_SUBF          , "SUBF          ");
        opcodes.put(OPC_MULF          , "MULF          ");
        opcodes.put(OPC_DIVF          , "DIVF          ");
        opcodes.put(OPC_REMF          , "REMF          ");
        opcodes.put(OPC_NEGF          , "NEGF          ");
        opcodes.put(OPC_CMPFL         , "CMPFL         ");
        opcodes.put(OPC_CMPFG         , "CMPFG         ");
        opcodes.put(OPC_F2I           , "F2I           ");
        opcodes.put(OPC_IFEQ          , "IFEQ          ");
        opcodes.put(OPC_IFNE          , "IFNE          ");
        opcodes.put(OPC_IFLT          , "IFLT          ");
        opcodes.put(OPC_IFLE          , "IFLE          ");
        opcodes.put(OPC_IFGT          , "IFGT          ");
        opcodes.put(OPC_IFGE          , "IFGE          ");
        opcodes.put(OPC_GOTO          , "GOTO          ");
        opcodes.put(OPC_TABLESWITCH   , "TABLESWITCH   ");
        opcodes.put(OPC_LOOKUPSWITCH  , "LOOKUPSWITCH  ");
        opcodes.put(OPC_GETI          , "GETI          ");
        opcodes.put(OPC_GETI2         , "GETI2         ");
        opcodes.put(OPC_ALENGTH       , "ALENGTH       ");
        opcodes.put(OPC_RETURNL       , "RETURNL       ");
        opcodes.put(OPC_RETURNI       , "RETURNI       ");
        opcodes.put(OPC_RETURN        , "RETURN        ");
        opcodes.put(OPC_YIELD         , "YIELD         ");
        opcodes.put(OPC_INVOKEVIRTUAL , "INVOKEVIRTUAL ");
        opcodes.put(OPC_INVOKEABSOLUTE, "INVOKEABSOLUTE");
        opcodes.put(OPC_TRYSTART      , "TRYSTART      ");
        opcodes.put(OPC_TRYEND        , "TRYEND        ");
        opcodes.put(OPC_THROW         , "THROW         ");
        opcodes.put(OPC_CLINIT        , "CLINIT        ");
        opcodes.put(OPC_MENTER        , "MENTER        ");
        opcodes.put(OPC_MEXIT         , "MEXIT         ");
        opcodes.put(OPC_INSTANCEOF    , "INSTANCEOF    ");
        opcodes.put(OPC_CHECKCAST     , "CHECKCAST     ");
        opcodes.put(OPC_CHECKSTORE    , "CHECKSTORE    ");
        opcodes.put(OPC_NPE           , "NPE           ");
        opcodes.put(OPC_OBE           , "OBE           ");
        opcodes.put(OPC_DIV0          , "DIV0          ");
        opcodes.put(OPC_NEW           , "NEW           ");
        opcodes.put(OPC_PARM          , "PARM          ");
        opcodes.put(OPC_EXEC          , "EXEC          ");
        opcodes.put(OPC_ERROR         , "ERROR         ");
        opcodes.put(OPC_RESULT        , "RESULT        ");
        opcodes.put(OPC_GETAR         , "GETAR         ");
        opcodes.put(OPC_SETAR         , "SETAR         ");
        opcodes.put(OPC_MATH0         , "MATH0         ");
        opcodes.put(OPC_MATH1         , "MATH1         ");
        opcodes.put(OPC_OPERAND       , "OPERAND       ");
        opcodes.put(OPC_LDCONST       , "LDCONST       ");
        opcodes.put(OPC_LDB           , "LDB           ");
        opcodes.put(OPC_LDC           , "LDC           ");
        opcodes.put(OPC_LDS           , "LDS           ");
        opcodes.put(OPC_LDI           , "LDI           ");
        opcodes.put(OPC_STB           , "STB           ");
        opcodes.put(OPC_STS           , "STS           ");
        opcodes.put(OPC_STI           , "STI           ");
        opcodes.put(OPC_STOOP         , "STOOP         ");
        opcodes.put(OPC_LDB_BC        , "LDB_BC        ");
        opcodes.put(OPC_LDC_BC        , "LDC_BC        ");
        opcodes.put(OPC_LDS_BC        , "LDS_BC        ");
        opcodes.put(OPC_LDI_BC        , "LDI_BC        ");
        opcodes.put(OPC_STB_BC        , "STB_BC        ");
        opcodes.put(OPC_STS_BC        , "STS_BC        ");
        opcodes.put(OPC_STI_BC        , "STI_BC        ");
        opcodes.put(OPC_STOOP_BC      , "STOOP_BC      ");

        chan0ops.put(OP_GETCHANNEL     , "OP_GETCHANNEL     ");
        chan0ops.put(OP_FREECHANNEL    , "OP_FREECHANNEL    ");
        chan0ops.put(OP_EXIT           , "OP_EXIT           ");
        chan0ops.put(OP_GC             , "OP_GC             ");
        chan0ops.put(OP_FREEMEM        , "OP_FREEMEM        ");
        chan0ops.put(OP_TOTALMEM       , "OP_TOTALMEM       ");
        chan0ops.put(OP_GETTIME        , "OP_GETTIME        ");
        chan0ops.put(OP_ARRAYCOPY      , "OP_ARRAYCOPY      ");
        chan0ops.put(OP_GETEVENT       , "OP_GETEVENT       ");
        chan0ops.put(OP_WAITFOREVENT   , "OP_WAITFOREVENT   ");
        chan0ops.put(OP_TRACE          , "OP_TRACE          ");
        chan0ops.put(OP_FATALVMERROR   , "OP_FATALVMERROR   ");
        chan0ops.put(OP_GETHEADER      , "OP_GETHEADER      ");
        chan0ops.put(OP_SETHEADER      , "OP_SETHEADER      ");
        chan0ops.put(OP_GETCLASS       , "OP_GETCLASS       ");
        chan0ops.put(OP_FREEAR         , "OP_FREEAR         ");
        chan0ops.put(OP_GETCLASSTABLE  , "OP_GETCLASSTABLE  ");
        chan0ops.put(OP_GETARGC        , "OP_GETARGC        ");
        chan0ops.put(OP_GETARGVCH      , "OP_GETARGVCH      ");
        chan0ops.put(OP_GETCH          , "OP_GETCH          ");
        chan0ops.put(OP_PUTCH          , "OP_PUTCH          ");
        chan0ops.put(OP_SETISOLATE     , "OP_SETISOLATE     ");
        chan0ops.put(OP_GETARRAYLENGTH , "OP_GETARRAYLENGTH ");

        chanNops.put(OP_GETCHANNEL     , "OP_GETCHANNEL     ");
        chanNops.put(OP_FREECHANNEL    , "OP_FREECHANNEL    ");
        chanNops.put(OP_OPEN           , "OP_OPEN           ");
        chanNops.put(OP_CLOSE          , "OP_CLOSE          ");
        chanNops.put(OP_ACCEPT         , "OP_ACCEPT         ");
        chanNops.put(OP_OPENINPUT      , "OP_OPENINPUT      ");
        chanNops.put(OP_CLOSEINPUT     , "OP_CLOSEINPUT     ");
        chanNops.put(OP_WRITEREAD      , "OP_WRITEREAD      ");
        chanNops.put(OP_READBYTE       , "OP_READBYTE       ");
        chanNops.put(OP_READSHORT      , "OP_READSHORT      ");
        chanNops.put(OP_READINT        , "OP_READINT        ");
        chanNops.put(OP_READLONG       , "OP_READLONG       ");
        chanNops.put(OP_READBUF        , "OP_READBUF        ");
        chanNops.put(OP_SKIP           , "OP_SKIP           ");
        chanNops.put(OP_AVAILABLE      , "OP_AVAILABLE      ");
        chanNops.put(OP_MARK           , "OP_MARK           ");
        chanNops.put(OP_RESET          , "OP_RESET          ");
        chanNops.put(OP_MARKSUPPORTED  , "OP_MARKSUPPORTED  ");
        chanNops.put(OP_OPENOUTPUT     , "OP_OPENOUTPUT     ");
        chanNops.put(OP_FLUSH          , "OP_FLUSH          ");
        chanNops.put(OP_CLOSEOUTPUT    , "OP_CLOSEOUTPUT    ");
        chanNops.put(OP_WRITEBYTE      , "OP_WRITEBYTE      ");
        chanNops.put(OP_WRITESHORT     , "OP_WRITESHORT     ");
        chanNops.put(OP_WRITEINT       , "OP_WRITEINT       ");
        chanNops.put(OP_WRITELONG      , "OP_WRITELONG      ");
        chanNops.put(OP_WRITEBUF       , "OP_WRITEBUF       ");

    /* Math functions */

        mathOps.put(MATH_sin            , "MATH_sin           ");
        mathOps.put(MATH_cos            , "MATH_cos           ");
        mathOps.put(MATH_tan            , "MATH_tan           ");
        mathOps.put(MATH_asin           , "MATH_asin          ");
        mathOps.put(MATH_acos           , "MATH_acos          ");
        mathOps.put(MATH_atan           , "MATH_atan          ");
        mathOps.put(MATH_exp            , "MATH_exp           ");
        mathOps.put(MATH_log            , "MATH_log           ");
        mathOps.put(MATH_sqrt           , "MATH_sqrt          ");
        mathOps.put(MATH_ceil           , "MATH_ceil          ");
        mathOps.put(MATH_floor          , "MATH_floor         ");
        mathOps.put(MATH_atan2          , "MATH_atan2         ");
        mathOps.put(MATH_pow            , "MATH_pow           ");
        mathOps.put(MATH_IEEEremainder  , "MATH_IEEEremainder ");
        mathOps.put(MATH_ADDD           , "MATH_ADDD          ");
        mathOps.put(MATH_SUBD           , "MATH_SUBD          ");
        mathOps.put(MATH_MULD           , "MATH_MULD          ");
        mathOps.put(MATH_DIVD           , "MATH_DIVD          ");
        mathOps.put(MATH_REMD           , "MATH_REMD          ");
        mathOps.put(MATH_L2D            , "MATH_L2D           ");
        mathOps.put(MATH_F2D            , "MATH_F2D           ");
        mathOps.put(MATH_I2D            , "MATH_I2D           ");
        mathOps.put(MATH_ADDL           , "MATH_ADDL          ");
        mathOps.put(MATH_SUBL           , "MATH_SUBL          ");
        mathOps.put(MATH_MULL           , "MATH_MULL          ");
        mathOps.put(MATH_DIVL           , "MATH_DIVL          ");
        mathOps.put(MATH_REML           , "MATH_REML          ");
        mathOps.put(MATH_CMPL           , "MATH_CMPL          ");
        mathOps.put(MATH_MOVL           , "MATH_MOVL          ");
        mathOps.put(MATH_NEGL           , "MATH_NEGL          ");
        mathOps.put(MATH_ANDL           , "MATH_ANDL          ");
        mathOps.put(MATH_ORRL           , "MATH_ORRL          ");
        mathOps.put(MATH_XORL           , "MATH_XORL          ");
        mathOps.put(MATH_SLLL           , "MATH_SLLL          ");
        mathOps.put(MATH_SRLL           , "MATH_SRLL          ");
        mathOps.put(MATH_SRAL           , "MATH_SRAL          ");
        mathOps.put(MATH_LDL            , "MATH_LDL           ");
        mathOps.put(MATH_LDL_BC         , "MATH_LDL_BC        ");
        mathOps.put(MATH_D2L            , "MATH_D2L           ");
        mathOps.put(MATH_F2L            , "MATH_F2L           ");
        mathOps.put(MATH_I2L            , "MATH_I2L           ");
        mathOps.put(MATH_L2F            , "MATH_L2F           ");
        mathOps.put(MATH_D2F            , "MATH_D2F           ");
        mathOps.put(MATH_L2I            , "MATH_L2I           ");
        mathOps.put(MATH_D2I            , "MATH_D2I           ");
        mathOps.put(MATH_CMPDL          , "MATH_CMPDL         ");
        mathOps.put(MATH_CMPDG          , "MATH_CMPDG         ");
        mathOps.put(MATH_STL            , "MATH_STL           ");
        mathOps.put(MATH_STL_BC         , "MATH_STL_BC        ");
    }


    public static void main(String[] args) {
        for (Enumeration keys = allOps.keys(); keys.hasMoreElements();) {
            String key = (String)keys.nextElement();
            IntHashtable ops = (IntHashtable)allOps.get(key);
            System.out.println(key+":");
            System.out.println("\t"+ops.toString().replace('[','\n').replace(',','\n'));
        }
    }


/*---------------------------------------------------------------------------*\
 *                                   Trace                                   *
\*---------------------------------------------------------------------------*/

    final PrintStream out;
    void trace(Object s) {
        trace(s,true);
    }
    void trace(Object s, boolean appendSpace) {
        out.print(s);
        if (appendSpace) {
            out.print(" ");
        }
    }

/*---------------------------------------------------------------------------*\
 *                                Bytecode access                            *
\*---------------------------------------------------------------------------*/


    final byte[] mth;

   /*
    * getBytecode
    */

    int getBytecode(int addr)  {
        return mth[addr];
    }

   /*
    * getUnsignedBytecode
    */

    int getUnsignedBytecode(int addr)  {
        return getBytecode(addr) & 0xFF;
    }

    int getUnsignedHalfFromBytecode(int off)  {
        int hi = getUnsignedBytecode(off  );
        int lo = getUnsignedBytecode(off+1);
        return (hi << 8) + lo;
    }

   /*
    * traceTarget
    */
    int traceTarget(int ip) {
        int t = (getBytecode(ip++) << 8) + getUnsignedBytecode(ip++); // note: signed << 8 + unsigned
        trace("t="+t);
        return ip;
    }


   /*
    * match - Get a key match for a lookupswitch
    *
    * lookupswitch has pairs of bytes, the first two are for the default location
    *
    *     def def 00 00 11 11 22 22 etc.
    *
    * followed by four bytes for each match
    *
    *     00 00 00 00 11 11 11 11 22 22 22 22 etc.
    */
    int traceMatch(int ip) {
        int m = (getUnsignedBytecode(ip++) << 24) +
                (getUnsignedBytecode(ip++) << 16) +
                (getUnsignedBytecode(ip++) << 8)  +
                (getUnsignedBytecode(ip++) << 0);
        trace("m="+m);
        return ip;
    }


/*---------------------------------------------------------------------------*\
 *                               Operand decoding                            *
\*---------------------------------------------------------------------------*/

/*
    Encoding

    00rrrrrr                                        // local
    01cccccc                                        // const
    10rrrrrr rrrrrrrr                               // extended local
    11000000 cccccccc cccccccc cccccccc cccccccc    // extended constant
    11100000 ssssssss ssssssss                      // static receiver

*/

    int operandResult;
    int SIMPLESIGNSHIFT = 32 - ENC_HIGH_BYTE_BITS;

   /*
    * traceSimple
    */
    int traceSimple(String str, int b, int[] rs, int rsIndex) {
        assume((b & ENC_COMPLEX) == 0);
        if (b == 0) {
            b = 99999999;
            trace(str+"L0");
        }
        else
        if ((b & ENC_CONST) != 0) {
            b = (b & 0x3f) << SIMPLESIGNSHIFT >> SIMPLESIGNSHIFT;
            trace(str+"#"+b);
            if (rs != null) {
                rs[rsIndex] = b;
            }
        } else {
            //assume(b >= AR_locals);
            trace(str+"L"+b);
            b = 99999999;
            if (rs != null) {
                rs[rsIndex] = b;
            }
        }
        return b;
    }


    int traceSimple(int b) {
        return traceSimple("", b, null, -1);
    }


   /*
    * traceOperand
    */
    int traceOperand(int ip) {
        int b = getUnsignedBytecode(ip++);
        if ((b & ENC_COMPLEX) == 0) {
            b = traceSimple(b);
        } else {
            if ((b & ENC_CONST) == 0) {
                b = ((b & 0x3f) << 8) + getUnsignedBytecode(ip++);
                trace("l"+b);
                b = 99999999;
            } else if ((b & (ENC_STATIC)) == 0) {
                b = (getUnsignedBytecode(ip++) << 24) +
                    (getUnsignedBytecode(ip++) << 16) +
                    (getUnsignedBytecode(ip++) << 8)  +
                    (getUnsignedBytecode(ip++) << 0);
                trace("#"+b);
            } else {
                b = (getUnsignedBytecode(ip++) << 8)  +
                    (getUnsignedBytecode(ip++) << 0);
                trace("&"+b);
                b = 99999999;
            }
        }
        operandResult = b;
        return ip;
    }


/*---------------------------------------------------------------------------*\
 *                                   traceFMT                                *
\*---------------------------------------------------------------------------*/


    int traceFMT(int ip, int operands, int[] rs, String l1, String l2, String l3) {
        int b1 = 0, b2 = 0, b3 = 0;
        switch (operands) {
            case FMT_I:     b1 = getUnsignedBytecode(ip++);
                            traceSimple(l1, b1, rs, 1);
                            break;

            case FMT_II:    b2 = getUnsignedBytecode(ip++);
                            b1 = getUnsignedBytecode(ip++);
                            traceSimple(l1, b1, rs, 1);
                            traceSimple(l2, b2, rs, 2);
                            break;


            case FMT_III:   b3 = getUnsignedBytecode(ip++);
                            b2 = getUnsignedBytecode(ip++);
                            b1 = getUnsignedBytecode(ip++);
                            traceSimple(l1, b1, rs, 1);
                            traceSimple(l2, b2, rs, 2);
                            traceSimple(l3, b3, rs, 3);
                            break;

            case FMT_NONE:  break;
            default:        shouldNotReachHere();
        }

        return ip;
    }


    int traceFMT(int ip, int operands, int[] rs) {
        return traceFMT(ip, operands, rs, "rs1=", "rs2=", "rs3=");
    }



/*---------------------------------------------------------------------------*\
 *                                    Preamble                               *
\*---------------------------------------------------------------------------*/

    int preamble() {
        int ip = 0;
        int ar1     = getUnsignedBytecode(ip++);
        int ar2     = getUnsignedBytecode(ip++);
        int arsize  = ((ar1<<8)+ar2);
        int cno1    = getUnsignedBytecode(ip++);
        int cno2    = getUnsignedBytecode(ip++);
        int parmOff = getUnsignedBytecode(ip++);
        int mapSize = getUnsignedBytecode(ip++);

        trace("  arsize="+arsize+",");
        trace("cno="+((cno1<<8)+cno2)+",");
        trace("parmOffset="+parmOff+",");
        trace("oopMapSize="+mapSize+",");
        if (mapSize > 0) {
            trace("oopMap={");
            while (mapSize-- > 0) {
                int m = getUnsignedBytecode(ip++);
                trace("0x"+Integer.toHexString(m).toUpperCase());
            }
            trace("},");
        }

        int handler = 1;
        while (ip < parmOff) {
            int startIp1 = getUnsignedBytecode(ip++);
            int startIp2 = getUnsignedBytecode(ip++);
            int endIp1   = getUnsignedBytecode(ip++);
            int endIp2   = getUnsignedBytecode(ip++);
            int handIp1  = getUnsignedBytecode(ip++);
            int handIp2  = getUnsignedBytecode(ip++);
            int ex1      = getUnsignedBytecode(ip++);
            int ex2      = getUnsignedBytecode(ip++);
            trace("handler"+(handler++)+"={startIp="+((startIp1<<8)+startIp2));
            trace("endIp="+((endIp1<<8)+endIp2));
            trace("handlerIp="+((handIp1<<8)+handIp2));
            trace("cno="+((ex1<<8)+ex2));
            trace("},");
        }

        int nparms = getUnsignedBytecode(ip++);
        trace("nparms="+nparms);
        if (nparms > 0) {
            trace("parmMap={");
            int from = 0;
            while (nparms-- > 0) {
                int p = getUnsignedBytecode(ip++);
                trace(from+"->"+p);
                from++;
            }
            trace("}");
        }
        trace("\n",false);
        return ip;
    }



/*---------------------------------------------------------------------------*\
 *                                    Main loop                              *
\*---------------------------------------------------------------------------*/

    public Disassembler(PrintStream out, byte[] mth) {
        this.out = out;
        this.mth = mth;
    }
    public void disassemble(boolean verbose) {
        int  operands;
        int  code = 0;
        int  endCode = 0;
        int  resultReg = 0;
        int[] rs = new int[4];
        int parmNumber = 0;
        int[] parms = new int[16];

        /* Test whether the method has any debug info */
        int mthLength = mth.length;
        if (mth[mthLength - 1] == 1) {
            int debugInfoLength = getUnsignedHalfFromBytecode(mthLength - 3);
            int offset = (mthLength - (1 + debugInfoLength));
            mthLength  = offset;

            int nameLength            = getUnsignedHalfFromBytecode(offset); offset += 2;
            int lineNumberTableLength = getUnsignedHalfFromBytecode(offset); offset += 2;

            // name
            if (nameLength > 0) {
                int cno = getUnsignedHalfFromBytecode(MTH_classNumberHigh);
                trace("  Name: "+ClassBase.forNumber(cno).className+".",false);
                trace(new String(mth,offset,nameLength)+"\n",false);
                offset += nameLength;
            }
            // lineNumberTable
            if (lineNumberTableLength != 0) {
                trace("  LineNumberTable:\n",false);
                while (lineNumberTableLength != 0) {
                    int startIp    = getUnsignedHalfFromBytecode(offset); offset += 2;
                    int sourceLine = getUnsignedHalfFromBytecode(offset); offset += 2;
                    trace("    "+startIp+" -> "+sourceLine+"\n",false);
                    lineNumberTableLength--;
                }
            }
        }

        if (verbose) {
            trace("\nBytecodes = ");
            for (int i = 0 ; i < mth.length ; i++) {
                if (i%10 == 0) {
                    trace("\n"+i+": ");
                }
                trace(""+getUnsignedBytecode(i));
            }
            trace("\n\n");
        }

        if (mth[0] == 0 && mth[1] == 0) {
            trace("  abstract or native method\n",false);
            assume(mth.length == 2);
            return;
        }

        int ip = preamble();

        while (ip < mthLength) {

            trace("\t"+ip+":\t");

           /*
            * Get the next bytecode.
            */
            code = getUnsignedBytecode(ip++);
            if (verbose) {
                trace("("+code+")");
            }
            trace(getOpString(code));

           /*
            * Get the operand and return codes.
            */
            if (code < 0 || code >= operandTable.length) {
                 fatalVMError("Bad bytecode "+code+" ip="+ip);
            }

            operands = operandTable[code];
            endCode  = operands & E_MASK;
            operands = operands & FMT_MASK;

           /*
            * Get result specifier.
            */
            resultReg = getUnsignedBytecode(ip++);
            if (resultReg > 0x3F) {           /* Two-byte local variable index */
                resultReg = (resultReg & 0x3F) << 8 | getUnsignedBytecode(ip++);
            }
            trace("res="+resultReg);

            switch (code) {

                default:
                    ip = traceFMT(ip, operands, rs);
                    break;

                case OPC_OPERAND: {
                    ip = traceOperand(ip);

                    switch (resultReg) {
                        case 1: rs[1] = operandResult; break;   // just needed for table/lookupswitch
                        case 2: rs[2] = operandResult; break;
                        case 3: rs[3] = operandResult; break;
                        default: shouldNotReachHere();
                    }
                    break;
                }

                case OPC_IFEQ:
                case OPC_IFNE:
                case OPC_IFLT:
                case OPC_IFLE:
                case OPC_IFGT:
                case OPC_IFGE:
                    assume(operands == FMT_II);
                    ip = traceFMT(ip, operands, rs);
                case OPC_GOTO:
                    ip = traceTarget(ip);
                    break;


                case OPC_TABLESWITCH: {
                    assume(operands == FMT_I);
                    ip = traceFMT(ip, operands, rs, "key=", null, null);
                    trace("low=");
                    ip = traceOperand(ip);
                    int low  = operandResult;
                    trace("high=");
                    ip = traceOperand(ip);
                    int high = operandResult;
                    trace("default=");
                    ip = traceTarget(ip);

                    for (int i = 0 ; i < (high-low+1) ; i++) {
                        ip = traceTarget(ip);
                    }
                    break;
                }

                case OPC_LOOKUPSWITCH: {
                    assume(operands == FMT_I);
                    ip = traceFMT(ip, operands, rs, "key=", null, null);
                    int npairs  = getUnsignedBytecode(ip++);
                    trace("npairs="+npairs);
                    trace("default=");
                    ip = traceTarget(ip);

                    for (int i = 0 ; i < npairs ; i++) {
                        ip = traceTarget(ip);
                    }
                    for (int i = 0 ; i < npairs ; i++) {
                        ip = traceMatch(ip);
                    }
                    break;
                }

                case OPC_INVOKEABSOLUTE:
                    ip = traceOperand(ip);
                    /* Drop thru */

                case OPC_INVOKEVIRTUAL: {

                   /*
                    * Get the vtable offset
                    */
                    int offset = getUnsignedBytecode(ip++);
                    if (offset > 0x7F) {
                        offset = (offset & 0x7F) << 8 | getUnsignedBytecode(ip++); //getBytecode(ip++);
                        offset = (offset << 17) >> 17;  /* Sign extend 11 bits */
                    }

                    trace("offset="+offset);

                   /*
                    * Get the number of parameters
                    */
                    int nparms = getUnsignedBytecode(ip++);
                    trace("nparms="+nparms);
                    assume(nparms > 0);

                    for (int i = 0 ;  i < nparms ; i++) {
                        ip = traceOperand(ip);
                    }
                    break;
                }


                case OPC_PARM: {
                    ip = traceFMT(ip, operands, rs);
                    parms[parmNumber++] = rs[1];
                    break;
                }

                case OPC_MATH0:
                case OPC_MATH1: {
                    ip = traceFMT(ip, operands, rs);
                    trace("(op="+getOpString(parms[0],MATH_OPS).trim()+")");
                    parmNumber = 0;
                    break;

                }

                case OPC_EXEC: {
                    ip = traceFMT(ip, operands, rs);
                    trace("(chan0 op="+getOpString(parms[0],CHAN0_OPS).trim()+", chanN op="+getOpString(parms[0],CHANN_OPS).trim()+")");
                    parmNumber = 0;
                    break;
                }

            }
            trace("\n");
        }
        trace("\n",false);
    }


}