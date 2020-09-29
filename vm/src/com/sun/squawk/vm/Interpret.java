//J2C:interp.c **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;

/*IFJ*/abstract class Interpret extends ObjectMemory implements Runnable {

/*---------------------------------------------------------------------------*\
 *                            Forward References                             *
\*---------------------------------------------------------------------------*/

    abstract int     getArgumentCount();
    abstract int     getArgumentChar(int arg, int ch);
//IFC//#ifndef PRODUCTION

    abstract void    setThreshold(int value);
    abstract int     getThreshold();
    abstract boolean metThreshold();

    abstract boolean getTraceInstructions();
    abstract boolean getTraceMethods();

    abstract void    setTraceInstructions(boolean b);
    abstract void    setTraceMethods(boolean b);

//IFC//#else
//IFC//#define getTraceInstructions() false
//IFC//#define getTraceMethods()      false
//IFC//#define setTraceInstructions(value)
//IFC//#define setTraceMethods(value)
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                       Relative / absolute Ip convertion                   *
\*---------------------------------------------------------------------------*/

    /*
     * Get the absolute instruction pointer (in terms of bytes) from a method and a relative offset.
     */
/*IFJ*/int getAbsoluteIp(int mth, int off) {
/*IFJ*/    return (mth*4) + off;
/*IFJ*/}
//IFC//#define getAbsoluteIp(mth,off) ((mth*4) + off)

    /*
     * Get the relative instruction pointer (in terms of bytes) from a method and an absolute offset.
     */
/*IFJ*/int getRelativeIp(int mth, int off) {
/*IFJ*/    return off - (mth*4);
/*IFJ*/}
//IFC//#define getRelativeIp(mth,off) (off - (mth*4))


/*---------------------------------------------------------------------------*\
 *                                   Trace                                   *
\*---------------------------------------------------------------------------*/


//IFC//#ifndef PRODUCTION

    int currentThreadID;

    /**
     * This method must be called before any new line of output is generated
     * if that line is to be available to the TraceViewer tool.
     */
    void trace_threadID() {
        traceMsg("Thread-");
        traceInt(currentThreadID);
        traceMsg(":");
    }

    void trace_AR(int ar) {
        int arSize, i;
        int ip   = getWord(ar, AR_ip);
        int mth  = getWord(ar, AR_method);
        int prev = getWord(ar, AR_previousAR);
        trace_threadID();
        traceMsg("Activation record (");
        traceInt(ar);
        traceMsg("):\n");

        traceMsg("  ip:  ");
        traceInt(+ip);
        traceMsg("\n");

        traceMsg("  mth: ");
        traceInt(mth);
        traceMsg("\n");

        traceMsg("  previousAR: ");
        traceInt(prev);
        traceMsg("\n");

        arSize = getUnsignedHalfFromByteArray(mth,MTH_arSizeHigh);
        for (i = AR_locals; i != arSize; i++) {
            traceMsg("  [");
            traceInt(i);
            traceMsg("]: ");
            traceInt(getWord(ar,i));
            traceMsg("\n");

        }
    }

    /*
     * traceSourceLine
     */
    void traceSourceLine(int cls, int clsDebugInfo, int mth, int ip) {
        int mthLength = getArrayLength(mth);
        boolean sourceLineFound = false;
        /* Test whether the method has any debug info */
        if (getUnsignedByte(mth, mthLength - 1) == 1) {
            int debugInfoLength = getUnsignedHalfFromByteArray(mth,mthLength - 3);

            /* Lengths */
            int offset                = (mthLength - (1 + debugInfoLength));
            int nameLength            = getUnsignedHalfFromByteArray(mth,offset);
            int lineNumberTableLength = getUnsignedHalfFromByteArray(mth,offset+2);
            offset += 4;

            /* Skip the method name */
            offset += nameLength;
            /* Decode line number table */
            if (lineNumberTableLength != 0) {
                int sourceFileLength;
                int sourceLine = 0;
                do {
                    int startIp = getUnsignedHalfFromByteArray(mth,offset);
                    int line    = getUnsignedHalfFromByteArray(mth,offset+2);
                    if (ip == startIp) {
                        sourceLine = line;
                        break;
                    }
                    if (startIp > ip) {
                        break;
                    }
                    sourceLine = line;
                    offset += 4;
                    lineNumberTableLength--;
                } while (lineNumberTableLength != 0);

                /* Print source file name */
                traceMsg("(");
                sourceFileLength = getUnsignedHalfFromByteArray(clsDebugInfo,0);
                offset = 2;
                while (sourceFileLength != 0) {
                    traceChar((char)getByte(clsDebugInfo,offset++));
                    sourceFileLength--;
                }
                traceMsg(":");
                traceInt(sourceLine);
                traceMsg(") ");
                sourceLineFound = true;
            }
        }
        if (!sourceLineFound) {
            // No debug info available so just trace ip
            traceMsg("(ip=");
            traceInt(ip);
            traceMsg(") ");
        }
    }

    /*
     * Trace the initial part of an instruction including source file line number
     * if the appropriate debug info is available.
     */
    void trace_(int ar, int ip, int endCode, int dst) {
        int mth   = getWord(ar,AR_method);
        int cls   = getClassFromMethod(mth);
        int dbg   = getWord(cls,CLS_debugInfo);
        int relIp = getRelativeIp(getWord(ar, AR_method),ip);

        trace_threadID();
        traceMsg("  ");

        /* Test whether the class has sourceFile info */
        if (dbg != 0) {
            traceSourceLine(cls, dbg, mth, relIp);
        }

        /* absolute instruction address */
/*
        traceMsg("(");
        traceInt(ip);
        traceMsg(")");
*/
        traceInt(relIp); /* relative instruction address */
        traceMsg(": ");
        traceOpcode(getByte(0, ip)&~OPC_SIMPLE);
        traceMsg("  ");

        if (endCode == E_WBI) {
            traceInt(dst);
            traceMsg(" <- ");
        }
        else {
            /* indent to show no result reg */
            traceMsg("     ");
        }
    }
    /*
     * Trace the end of an instruction.
     */
    void trace_end() {
        traceMsg("\n");
    }

    void trace_i(int i) {
        traceMsg(" ");
        traceInt(i);
    }

    void trace_I(int ar, int ip, int endCode, int dst, int rs1) {
        trace_(ar, ip, endCode, dst);
        trace_i(rs1);
    }

    void trace_II(int ar, int ip, int endCode, int dst, int rs1, int rs2) {
        trace_(ar, ip, endCode, dst);
        trace_i(rs1);
        trace_i(rs2);
    }

    void trace_III(int ar, int ip, int endCode, int dst, int rs1, int rs2, int rs3) {
        trace_(ar, ip, endCode, dst);
        trace_i(rs1);
        trace_i(rs2);
        trace_i(rs3);
    }

    void disassemble(int ar) {
/*IFJ*/ int mth = getWord(ar,AR_method);
/*IFJ*/ byte[] bytecode = new byte[getHeaderLength(mth)];
/*IFJ*/ for (int i = 0; i != bytecode.length; i++) {
/*IFJ*/     bytecode[i] = (byte)getByte(mth,i);
/*IFJ*/ }
/*IFJ*/ Disassembler d = new Disassembler(System.out,bytecode);
/*IFJ*/ d.disassemble(true);
    }

    void indentMethodDepth(int ar, String glyph) {
        int previousAR = ar;
        int depth = 0;
        if (getTraceInstructions()) {
            trace_end();
        }
        trace_threadID();
        while (previousAR != 0) {
            previousAR = getWord(previousAR,AR_previousAR);
            assume(previousAR != ar);
            traceMsg("|");
            depth++;
            if (depth > 100) {
                fatalVMError("Looks like infinite recursion...");
            }
        }
        traceMsg(" (");
        traceInt(depth);
        traceMsg(") ");
        traceMsg(glyph);
        traceMsg(" ");
    }

    void traceMethodNameAR(int ar, int slot, boolean args) {
        int mth = getWord(ar,AR_method);
        int cls = getClassFromMethod(mth);
        traceMethodName(mth, args);
        /* Print (cno@slot) */
        if (slot != 0) {
            traceMsg(" (");
            traceInt(getWord(cls,CLS_classIndex));
            traceMsg("@");
            traceInt(slot);
            traceMsg(")");
        }

    }

    void traceMethodEnter(int ar, int slot) {
        indentMethodDepth(ar,"=>");
        traceMethodNameAR(ar, slot, true);
        if (!getTraceInstructions()) {
            trace_end();
        }
    }
    void tracePrimitiveMethodEnter(int opcode, int ar, int ip, int rs1, int rs2) {
        indentMethodDepth(ar,"=>");
        traceMethodNameAR(ar, SLOT_primitive, true);
        traceMsg(" <PRIMITIVE: ");
        traceOpcode(opcode);
        traceMsg("ar=");
        traceInt(ar);
        traceMsg(",ip=");
        traceInt(ip);
        traceMsg(",rs1=");
        traceInt(rs1);
        traceMsg(",rs2=");
        traceInt(rs2);
        traceMsg("> ");
        if (!getTraceInstructions()) {
            trace_end();
        }
    }
    void traceMethodExit(int ar) {
        indentMethodDepth(ar,"<=");
        traceMethodNameAR(ar, 0, true);
        if (!getTraceInstructions()) {
            trace_end();
        }
    }

    /**
     * Debug output that it picked up by the TraceViewer.
     */
    void traceln(String msg) {
        trace_threadID();
        traceMsg(msg);
        traceMsg("\n");
    }

    /*
     * traceJavaStack
     */
    int globalAR;
    int globalIP;
    void SAVE_AR(int ar) { globalAR = ar; }
    void SAVE_IP(int ip) { globalIP = ip; }
    void traceJavaStack() {
        int ar = globalAR;
        int ip = getRelativeIp(getWord(ar, AR_method), globalIP);
        traceMsg("\nJava stack trace:\n");
        while (ar != 0) {
            int mth   = getWord(ar,AR_method);
            int cls   = getClassFromMethod(mth);
            int dbg   = getWord(cls,CLS_debugInfo);
            traceMsg("    ");
            traceMethodNameAR(ar, 0, false);
            traceSourceLine(cls, dbg, mth, ip);
            traceMsg("\n");
            ar = getWord(ar, AR_previousAR);
            if (ar != 0) {
                ip = getWord(ar, AR_ip);
            }
        }
    }

//IFC//#else
//IFC//#define traceJavaStack()
//IFC//#define SAVE_AR(ar)
//IFC//#define SAVE_AR(ip)
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                           Instruction decoding                            *
\*---------------------------------------------------------------------------*/

   /*
    * Note - Care is needed with any macros for functions such as getBytecode()
    * where idioms such as getBytecode(ip++) are often used so the parameter must
    * only be expanded once.
    */


   /*
    * getBytecode
    */

/*IFJ*/int getBytecode(int addr)  {
/*IFJ*/    return getByte(0, addr);
/*IFJ*/}
//IFC//#define getBytecode(n) *((signed char *)(n))

   /*
    * getUnsignedBytecode
    */

/*IFJ*/int getUnsignedBytecode(int addr)  {
/*IFJ*/    return getUnsignedByte(0, addr);
/*IFJ*/}
//IFC//#define getUnsignedBytecode(n) *((unsigned char *)(n))


    /*
     * target - for if/goto
     */
    int decodeTarget(int ar, int ip) {
        if (getTraceInstructions()) {
            traceMsg(" $");
            traceInt((getWord(ar,AR_method)*4) + (getBytecode(ip) << 8) + getUnsignedBytecode(ip+1));
        }
        return (getWord(ar,AR_method)*4) + (getBytecode(ip++) << 8) + getUnsignedBytecode(ip); // note: signed << 8 + unsigned;
    }

   /*
    * target - Get the branch target from a tableswitch or lookupswitch
    *          (a pos of -1 is for the default location).
    *
    * tableswitch has pairs of bytes, the first two are for the default location
    *
    *     def def 00 00 11 11 22 22 etc.
    */
    int decodeTableTarget(int ar,int ip, int pos) {
        return decodeTarget(ar,ip + 2 + (pos * 2));
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
    int decodeMatch(int ip, int pos, int npairs) {
        ip = ip + 2 + (npairs * 2) + (pos * 4);
        if (getTraceInstructions()) {
            traceMsg(" ");
            traceInt((getUnsignedBytecode(ip  ) << 24) +
                     (getUnsignedBytecode(ip+1) << 16) +
                     (getUnsignedBytecode(ip+2) << 8)  +
                     (getUnsignedBytecode(ip+3)   << 0));
        }
        return (getUnsignedBytecode(ip++) << 24) +
               (getUnsignedBytecode(ip++) << 16) +
               (getUnsignedBytecode(ip++) << 8)  +
               (getUnsignedBytecode(ip)   << 0);

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
    * decodeOperand
    */
    int decodeOperand(int ip, int ar) {
        int b = getUnsignedBytecode(ip++);
        if ((b & ENC_COMPLEX) == 0) {
            if ((b & ENC_CONST) != 0) {
                b = (b & 0x3f) << SIMPLESIGNSHIFT >> SIMPLESIGNSHIFT;   // Const
            } else {
                assume(b >= AR_locals);
                b = getWord(ar, b);                                     // Local
            }
        } else {
            if ((b & ENC_CONST) == 0) {
                b = ((b & 0x3f) << 8) + getUnsignedBytecode(ip++);      // Extended local
                b = getWord(ar, b);
            } else if ((b & (ENC_STATIC)) == 0) {
                b = (getUnsignedBytecode(ip++) << 24) +                 // Extended const
                    (getUnsignedBytecode(ip++) << 16) +
                    (getUnsignedBytecode(ip++) << 8)  +
                    (getUnsignedBytecode(ip++) << 0);
            } else {
                b = (getUnsignedBytecode(ip++) << 8)  +                 // Class number
                    (getUnsignedBytecode(ip++) << 0);
                b = getClassFromCNO(b);
                assume(b != 0);
            }
        }
        operandResult = b;
        if (getTraceInstructions()) {
            traceMsg(" ");
            traceInt(operandResult);
        }
        return ip;
    }



/*---------------------------------------------------------------------------*\
 *                              Native code interface                        *
\*---------------------------------------------------------------------------*/

   /*
    * executeChannel0
    */
    int executeChannel0(int nativeParms[], int nparms) {
        int channel0ResultCode = 0;
        switch (nativeParms[0]) {
            case OP_GETCHANNEL:     /* Drop thru */
            case OP_FREECHANNEL:    channel0ResultCode = chan_execute(0, nativeParms, nparms);              break;
            case OP_EXIT:                                exitVM(nativeParms[1]);                            break;
            case OP_FREEMEM:        channel0ResultCode = freeMem();                                         break;
            case OP_TOTALMEM:       channel0ResultCode = totalMem();                                        break;
            case OP_GETTIME:                             setLong(nativeParms[1], 0, getTime());             break;
            case OP_ARRAYCOPY:                           arrayCopy(nativeParms);                            break;
            case OP_GETEVENT:       channel0ResultCode = getEvent();                                        break;
            case OP_WAITFOREVENT:                        waitForEvent(getLong(nativeParms[1], 0));          break;
            case OP_FATALVMERROR:                        fatalVMError("OP_FATALVMERROR");                   break;
            case OP_GETHEADER:      channel0ResultCode = getHeader(nativeParms[1]);                         break;
            case OP_GETCLASS:       channel0ResultCode = getClass(nativeParms[1]);                          break;
            case OP_SETHEADER:                           setHeader(nativeParms[1], nativeParms[2]);         break;
            case OP_FREEAR:                              freeActivation(nativeParms[1]);                    break;
            case OP_GETARGC:        channel0ResultCode = getArgumentCount();                                break;
            case OP_GETARGVCH:      channel0ResultCode = getArgumentChar(nativeParms[1], nativeParms[2]);   break;
/*IFJ*/     case OP_PUTCH:                               System.err.write((char)nativeParms[1]);            break;
/*IFJ*/     case OP_GETCH:    try { channel0ResultCode = System.in.read(); } catch(Exception ex) {}         break;
            case OP_SETISOLATE:
//*IFJ*/if (getTraceMethods()) {
//*IFJ*/out.println("\n\n" + getInstructionCount() + ": Switching from isolate " + getString(getWord(getIsolateState(), ISO_isolateId)));
//*IFJ*/printIsolateState(out);
//*IFJ*/traceJavaStack();
//*IFJ*/                 setIsolateState(nativeParms[1]);
//*IFJ*/out.println("Switched to isolate " + getString(getWord(getIsolateState(), ISO_isolateId)));
//*IFJ*/printIsolateState(out);
//*IFJ*/out.println("\n\n\n");
//*IFJ*/}
                                                                                                            break;
            case OP_GETARRAYLENGTH: channel0ResultCode = getHeaderLength(nativeParms[1]);                   break;

            case OP_TRACE: {
                int op = nativeParms[1];
                switch (op) {
                    case TRACE_SETTHRESHOLD:          setThreshold(nativeParms[2]);                         break;
                    case TRACE_GETTHRESHOLD:          channel0ResultCode = getThreshold();                  break;
                    case TRACE_METTHRESHOLD:          channel0ResultCode = metThreshold() ? 1 : 0;          break;
                    case TRACE_GETTRACEINSTRUCTIONS:  channel0ResultCode = getTraceInstructions() ? 1 : 0;  break;
                    case TRACE_GETTRACEMETHODS:       channel0ResultCode = getTraceMethods() ? 1 : 0;       break;
                    case TRACE_GETTRACEALLOCATION:    channel0ResultCode = getTraceAllocation() ? 1 : 0;    break;
                    case TRACE_GETTRACEGC:            channel0ResultCode = getTraceGC() ? 1 : 0;            break;
                    case TRACE_GETTRACEGCVERBOSE:     channel0ResultCode = getTraceGCVerbose() ? 1 : 0;     break;
                    case TRACE_SETTRACEINSTRUCTIONS:  setTraceInstructions(nativeParms[2] != 0);            break;
                    case TRACE_SETTRACEMETHODS:       setTraceMethods(nativeParms[2] != 0);                 break;
                    case TRACE_SETTRACEALLOCATION:    setTraceAllocation(nativeParms[2] != 0);              break;
                    case TRACE_SETTRACEGC:            setTraceGC(nativeParms[2] != 0);                      break;
                    case TRACE_SETTRACEGCVERBOSE:     setTraceGCVerbose(nativeParms[2] != 0);               break;
                    default: shouldNotReachHere();
                }
                break;
            }

            default: shouldNotReachHere();                                                                  break;
        }
        return channel0ResultCode;
    }



/*---------------------------------------------------------------------------*\
 *                                invokePrimitive                            *
\*---------------------------------------------------------------------------*/


    int invokePrimitive(int code, int ar, int ip, int rs1, int rs2) {

        int slot, targetAR, targetIP, targetParms, mth;

       /*
        * Get activation record large enough for Native.primitive()
        */
        if (code == OPC_THROW) {
            targetAR = getEmergencyActivation();
            assume(targetAR != 0);
        } else {
            targetAR = newNativeActivation();
            /*
             * Return 0 if the activation record could not be allocated. This will cause
             * the garbage collector to be run
             */
            if (targetAR == 0) {
                return INVOKE_ERR_outOfMemory;
            }
        }

       /*
        * Extract the method from the activation record allocated for it.
        */
        mth = getWord(targetAR,AR_method);
        assume(mth != 0);

       /*
        * Fudge to get the clinit class number in to rs2
        */
        if (code == OPC_CLINIT) {
            rs2 = rs1;
            rs1 = 0;
        }

       /*
        * Return 0 if the record could not be allocated. This will cause
        * the garbage collector to be run.
        */
        if (targetAR == 0) {
            return 0;
        }

       /*
        * Safety check that its long enough
        * This is not required any more as newPrimitiveActivation allocates a size
        * directly read from the primitive method.
        if (true) {
            int arSize = getUnsignedHalfFromByteArray(mth, MTH_arSizeHigh);
            assume(getArrayLength(targetAR) == arSize);
        }
        */

       /*
        * Skip the oopmap and handler tables
        */
        targetIP = getByte(mth, MTH_nparmsIndex);

       /*
        * Get the parm count
        */
        targetParms = getByte(mth, targetIP++);
        assume(targetParms == 5);

       /*
        * Ignore the receiver
        */
        targetIP++;

       /*
        * Store address of the calling activation record
        */
        slot = getByte(mth, targetIP++); assume(slot > 0); setWord(targetAR, slot, targetAR);
        slot = getByte(mth, targetIP++); assume(slot > 0); setWord(targetAR, slot, code);
        slot = getByte(mth, targetIP++); assume(slot > 0); setWord(targetAR, slot, rs1);
        slot = getByte(mth, targetIP++); assume(slot > 0); setWord(targetAR, slot, rs2);

       /*
        * Save the old IP value
        */
        setWord(ar, AR_ip, getRelativeIp(getWord(ar, AR_method),ip));

       /*
        * Link old ar to the new one
        */
        setWord(targetAR, AR_previousAR, ar);

       /*
        * Setup the new ip
        */
        setWord(targetAR, AR_ip, targetIP);

       /*
        * Stats
        */
        incInvokePrimitiveCount();

        if (getTraceMethods()) {
            tracePrimitiveMethodEnter(code, targetAR, ip, rs1, rs2);
        }

       /*
        * Return to the interpreter loop
        */
        return targetAR;
    }



/*---------------------------------------------------------------------------*\
 *                                    invoke                                 *
\*---------------------------------------------------------------------------*/

    int invoke(int code, int ar, int ip, int rs1, int rs2) {

        int cls, mth, rcvr, targetIP, targetAR, slot, arSize, targetParms, nparms, caller, offset;


        if (code == OPC_INVOKEVIRTUAL) {
            cls = 0;
        } else if (code == OPC_INVOKEABSOLUTE) {
            ip  = decodeOperand(ip, ar);
            cls = operandResult;
            assume(cls != 0);
        } else {
            return invokePrimitive(code, ar, ip, rs1, rs2);
        }

       /*
        * Get the current method's bytecode array
        */
        caller = getWord(ar, AR_method);

       /*
        * Get the vtable offset
        */
        offset = getUnsignedBytecode(ip++);
        if (offset > 0x7F) {
            offset = (offset & 0x7F) << 8 | getUnsignedBytecode(ip++); // getBytecode(0,ip++);
            offset = (offset << 17) >> 17;  /* Sign extend 11 bits */
        }

       /*
        * Get the number of parameters
        */
        nparms = getUnsignedBytecode(ip++);
        assume(nparms > 0);

        if (getTraceInstructions()) {
            traceMsg(" offset = ");
            traceInt(offset);
            traceMsg(", nparms = ");
            traceInt(nparms);
            traceMsg(", parms = {");
        }

        /*
         * Get the receiver
         */
        ip   = decodeOperand(ip, ar);
        rcvr = operandResult;

        if (cls == 0) {

            /*
             * Null pointer check on receiver.
             */
            if (rcvr == 0) {
                return INVOKE_ERR_nullPointer;
            }

            /*
             * Get the class of the receiver
             */
            cls = getClass(rcvr);
        }

        /*
         * If the offset is negative then lookup the method entry offset for this interface call
         */
        if (offset < 0) {
            int istart = getWord(cls, CLS_istart);
            int itable = getWord(cls, CLS_itable);
            offset = getHalf(itable, (0-offset)-istart);
        }

        /*
         * Lookup the method from either the fixed or variable vtable
         */
        mth = lookupMethod(cls, offset);

        /*
         * Get the size of the activation record needed
         */
        arSize = getUnsignedHalfFromByteArray(mth, MTH_arSizeHigh);

        /*
         * If the size is zero, then this is an abstract method or an unlinked native method.
         */
        if (arSize == 0) {
            fatalVMError("Abstract or native method invoked");
        }

        /*
         * Skip straight to the nparms entry
         */
        targetIP = getUnsignedByte(mth, MTH_nparmsIndex);

        /*
         * Get the parm count
         */
        targetParms = getUnsignedByte(mth, targetIP++);
        if (targetParms != nparms) {
            traceMsg("targetParms (");
            traceInt(targetParms);
            traceMsg(") != nparms (");
            traceInt(nparms);
            traceMsg("): ");
            traceMethodName(mth, true);
            traceMsg("\n");
            traceMsg("rcvr=");
            traceInt(rcvr);
            traceMsg(", cls=");
            traceInt(cls);
            traceMsg(", cno=");
            traceInt(getWord(cls, CLS_classIndex));
            traceMsg("\n");
        }
        assume(targetParms == nparms);

        /*
         * Allocate the new activation record
         */
        targetAR = newActivation(arSize);

        /*
         * Return 0 if the record could not be allocated. This will cause
         * the garbage collector to be run
         */
        if (targetAR == 0) {
            return INVOKE_ERR_outOfMemory;
        }

        /*
         * Store the receiver at the slot determined by the first entry in the parm map
         */
        slot = getUnsignedByte(mth, targetIP++);
        if (slot > 0) {
            assume(slot >= AR_locals);
            setWord(targetAR, slot, rcvr);
        }

        /*
         * Now do the other parameters
         */
        while (--nparms > 0) {
            ip = decodeOperand(ip, ar);
            slot = getUnsignedByte(mth, targetIP++);
            if (slot > 0) {
                assume(slot >= AR_locals);
                setWord(targetAR, slot, operandResult);
            }
        }
        if (getTraceInstructions()) {
            traceMsg(" }");
        }


        /*
         * Save the old IP value
         */
        setWord(ar, AR_ip, getRelativeIp(caller,ip));

        /*
         * Link old ar to the new one and set the method pointer
         */
        setWord(targetAR, AR_previousAR, ar);
        setWord(targetAR, AR_method,     mth);

        /*
         * Setup the new ip
         */
        setWord(targetAR, AR_ip, targetIP);

        /*
         * Stats
         */
        incInvokeCount();

        if (getTraceMethods()) {
            traceMethodEnter(targetAR,offset);
        }

        /*
         * Return to the interpreter loop
         */
        return targetAR;
    }


/*---------------------------------------------------------------------------*\
 *                                    math                                   *
\*---------------------------------------------------------------------------*/

    long math(int nativeParms[], int nparms) {
        int  op    = nativeParms[0];
        int  rs1_i = nativeParms[1];
        int  rs2_i = nativeParms[2];
        int  rs3_i = nativeParms[3];
        int  rs4_i = nativeParms[4];
        long rs1_l = rs2_i;
        long rs2_l = rs4_i;

        rs1_l &=  0x00000000FFFFFFFFL;
        rs1_l |= ((long)rs1_i) << 32;

        rs2_l &=  0x00000000FFFFFFFFL;
        rs2_l |= ((long)rs3_i) << 32;

        if (getTraceInstructions()) {
            traceMsg(" <");
            traceMathOp(op);
            traceMsg(": ");
            traceMsg("rs1_i=");
            traceInt(rs1_i);
            traceMsg(", rs2_i=");
            traceInt(rs2_i);
            traceMsg(", rs3_i=");
            traceInt(rs3_i);
            traceMsg(", rs4_i=");
            traceInt(rs4_i);
            traceMsg(", rs1_l=");
            traceLong(rs1_l);
            traceMsg(", rs2_l=");
            traceLong(rs2_l);
            traceMsg("> ");
        }
        nativeParms[0] = 0; /* This is the exception result code */

        /*
         * All the math ops that return an int (i.e. those with the comment " ??->1 " in SquawkOpcodes.java)
         * must have the int result shifted into the high word of the long returned by this method as that
         * correlates with how the intepret loop extracts the result.
         */
        switch(op) {
/*FLT*/     case MATH_sin:
/*FLT*/     case MATH_cos:
/*FLT*/     case MATH_tan:
/*FLT*/     case MATH_asin:
/*FLT*/     case MATH_acos:
/*FLT*/     case MATH_atan:
/*FLT*/     case MATH_exp:
/*FLT*/     case MATH_log:
/*FLT*/     case MATH_sqrt:
/*FLT*/     case MATH_ceil:
/*FLT*/     case MATH_floor:
/*FLT*/     case MATH_atan2:
/*FLT*/     case MATH_pow:
/*FLT*/     case MATH_IEEEremainder:    return math0(op, rs1_l, rs2_l);
/*FLT*/     case MATH_ADDD:             return addd(rs1_l, rs2_l);
/*FLT*/     case MATH_SUBD:             return subd(rs1_l, rs2_l);
/*FLT*/     case MATH_MULD:             return muld(rs1_l, rs2_l);
/*FLT*/     case MATH_DIVD:             return divd(rs1_l, rs2_l);
/*FLT*/     case MATH_REMD:             return remd(rs1_l, rs2_l);
/*FLT*/     case MATH_NEGD:             return negd(rs1_l);
/*FLT*/     case MATH_L2D:              return l2d(rs1_l);
/*FLT*/     case MATH_F2D:              return f2d(rs1_i);
/*FLT*/     case MATH_I2D:              return i2d(rs1_i);
            case MATH_ADDL:             return rs1_l + rs2_l;
            case MATH_SUBL:             return rs1_l - rs2_l;
            case MATH_MULL:             return rs1_l * rs2_l;
            case MATH_DIVL:             if (rs2_l == 0) {nativeParms[0] = OPC_DIV0; return 0; } else return rs1_l / rs2_l;
            case MATH_REML:             if (rs2_l == 0) {nativeParms[0] = OPC_DIV0; return 0; } else return rs1_l % rs2_l;
            case MATH_CMPL:             return ((long)cmpl(rs1_l, rs2_l))       << 32;
            case MATH_MOVL:             return         rs1_l;
            case MATH_NEGL:             return 0     - rs1_l;
            case MATH_ANDL:             return rs1_l & rs2_l;
            case MATH_ORRL:             return rs1_l | rs2_l;
            case MATH_XORL:             return rs1_l ^ rs2_l;
            case MATH_SLLL:             return rs1_l << rs3_i;
            case MATH_SRLL:             return srll(rs1_l, rs3_i);
            case MATH_SRAL:             return rs1_l >> rs3_i;
/*FLT*/     case MATH_D2L:              return d2l(rs1_l);
/*FLT*/     case MATH_F2L:              return f2l(rs1_i);
            case MATH_I2L:              return rs1_i;
/*FLT*/     case MATH_L2F:              return ((long)l2f(rs1_l))               << 32;
/*FLT*/     case MATH_D2F:              return ((long)d2f(rs1_l))               << 32;
            case MATH_L2I:              return ((long)l2i(rs1_l))               << 32;
/*FLT*/     case MATH_D2I:              return ((long)d2i(rs1_l))               << 32;
/*FLT*/     case MATH_CMPDL:            return ((long)cmpdl(rs1_l, rs2_l))      << 32;
/*FLT*/     case MATH_CMPDG:            return ((long)cmpdg(rs1_l, rs2_l))      << 32;

            case MATH_LDL:
            case MATH_STL: {
                if (rs1_i == 0) {
                    nativeParms[0] = OPC_NPE;
                    return 0;
                }
                if (rs1_i == -1) {
                    rs1_i = getIsolateState();
                }
                if (op == MATH_STL) {
                    setLong(rs1_i, rs2_i, rs2_l);
                    return 0;
                } else {
                    return getLong(rs1_i, rs2_i);
                }
            }

            case MATH_LDL_BC:
            case MATH_STL_BC: {
                if (rs1_i == 0) {
                    nativeParms[0] = OPC_NPE;
                    return 0;
                }
                if (rs2_i < 0 || rs2_i >= getArrayLength(rs1_i)) {
                    nativeParms[0] = OPC_OBE;
                    return 0;
                }
                if (op == MATH_STL_BC) {
                    setLong(rs1_i, rs2_i, rs2_l);
                    return 0;
                } else {
                    return getLong(rs1_i, rs2_i);
                }
            }

        }
        shouldNotReachHere();
        return 0;
   }

/*---------------------------------------------------------------------------*\
 *                                    Main loop                              *
\*---------------------------------------------------------------------------*/

    int interpret(int ar) {
        int  code = 0;
        int  endCode = 0;
        int  resultReg = 0;
        int  yieldCount = 0;
        int  rs1 = 0, rs2 = 0, rs3 = 0;
        int  intResult  = 0;
        int  intResult2 = 0;
        int  ip = 0;
        int currentInstruction = 0;

/*IFJ*/ int nativeParms[] = new int[16];
//IFC// int nativeParms[16];
        int nativePointer = 0;

        if (getTraceMethods()) {
            traceMsg("\n");
            trace_threadID();
            traceMsg("interpret() called with ar = ");
            traceInt(ar);
            traceMsg(" mth = ");
            traceInt(getWord(ar, AR_method));
            traceMsg(" ip = ");
            traceInt(getWord(ar, AR_ip));
            traceMsg("\n");
        }

        ip = getAbsoluteIp(getWord(ar, AR_method), getWord(ar, AR_ip));
        currentInstruction = ip;

        assume(OPC_IFLT        > OPC_IFEQ && OPC_IFLT        < OPC_LOOKUPSWITCH);
        assume(OPC_IFLE        > OPC_IFEQ && OPC_IFLE        < OPC_LOOKUPSWITCH);
        assume(OPC_IFGT        > OPC_IFEQ && OPC_IFGT        < OPC_LOOKUPSWITCH);
        assume(OPC_IFGE        > OPC_IFEQ && OPC_IFGE        < OPC_LOOKUPSWITCH);
        assume(OPC_GOTO        > OPC_IFEQ && OPC_GOTO        < OPC_LOOKUPSWITCH);
        assume(OPC_TABLESWITCH > OPC_IFEQ && OPC_TABLESWITCH < OPC_LOOKUPSWITCH);

        for (;;) {
            SAVE_IP(ip);
            incInstructionCount();

            /*
             * Finish the processing of the last instruction
             */
            switch (endCode) {
                case E_ERROR: {
                    endCode = 0;
                   /*
                    * Trace
                    */
                    if (getTraceInstructions()) {
                        traceMsg("\nE_ERROR ");
/*IFJ*/                 traceMsg(""+Disassembler.getOpString(code)); /* instruction mnemonic */
                    }
                    break;
                }

                case E_WBI:
                    if (resultReg < AR_locals) {
                        trace_(ar, ip, 0, 0);
                        fatalVMError("Bad writeback local");
                    }

                    assume(resultReg >= AR_locals);

                    setWord(ar, resultReg, rs1);

                    if (getTraceInstructions()) {
                        traceMsg("  --  (");
                        traceInt(rs1);
                        traceMsg(")");
                    }

                    /* drop thru */

                default: {
                    int operands, b;

                    if (code != OPC_OPERAND) {
                        currentInstruction = ip;
                    }

                    if (getTraceInstructions()) {
                        traceMsg("\n");
                    }

                   /*
                    * Get the next bytecode.
                    */
                    code = getUnsignedBytecode(ip++);

                   /*
                    * Get the operand and return codes.
                    */
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

                    assume(endCode == 0 || resultReg >= AR_locals);

                   /*
                    * Load input operands
                    */
                    switch (operands) {
                        case FMT_III:   b = getUnsignedBytecode(ip++);
                                        if (b != 0) {
                                            rs3 = ((b & ENC_CONST) == 0) ? getWord(ar, b) : (b << SIMPLESIGNSHIFT) >> SIMPLESIGNSHIFT;
                                        }
                                        /* Drop thru */
                        case FMT_II:    b = getUnsignedBytecode(ip++);
                                        if (b != 0) {
                                            rs2 = ((b & ENC_CONST) == 0) ? getWord(ar, b) : (b << SIMPLESIGNSHIFT) >> SIMPLESIGNSHIFT;
                                        }
                                        /* Drop thru */
                        case FMT_I:     b = getUnsignedBytecode(ip++);
                                        if (b != 0) {
                                            rs1 = ((b & ENC_CONST) == 0) ? getWord(ar, b) : (b << SIMPLESIGNSHIFT) >> SIMPLESIGNSHIFT;
                                        }
                                        /* Drop thru */
                        case FMT_NONE:  break;

                        default:        shouldNotReachHere();
                    }

                   /*
                    * Trace
                    */
                    if (getTraceInstructions()) {
                        switch (operands) {
                            case FMT_NONE:   trace_(    ar, currentInstruction, endCode, resultReg);                break;
                            case FMT_I:      trace_I(   ar, currentInstruction, endCode, resultReg, rs1);           break;
                            case FMT_II:     trace_II(  ar, currentInstruction, endCode, resultReg, rs1, rs2);      break;
                            case FMT_III:    trace_III( ar, currentInstruction, endCode, resultReg, rs1, rs2, rs3); break;
                            default:         shouldNotReachHere();
                        }
                    }

                   /*
                    * Stats
                    */
                    if (code >= OPC_IFEQ && code <= OPC_LOOKUPSWITCH) {
                        incBranchCount();
                    }
                }
            }

           /*
            * Check here for null pointer and bounds check errors
            *
            * rs1 -> base, rs2 -> offset, [rs3 -> src]
            */
            if (code >= OPC_LOADSTORES) {
                if (rs1 == -1) {
                    rs1 = getIsolateState();
                }
                if (rs1 == 0) {
                    code = OPC_NPE;
//*IFJ*/traceln("*NPE*");
//fatalVMError("npe");
                } else if (code >= OPC_LDB_BC) {
                    if (rs2 < 0 || rs2 >= getArrayLength(rs1)) {
//*IFJ*/traceln("*OBE* rs2 = "+rs2+"  getArrayLength(rs1) = "+getArrayLength(rs1));
//fatalVMError("obe");
                        code = OPC_OBE;
                    } else {
                        code -= (OPC_LDB_BC - OPC_LDB);
                    }
                }
            }

            /*
             * Execute the bytecode
             */
            switch (code) {

                case OPC_NOP:           /* nop */                                   break;
                case OPC_BREAK:         breakpoint();                               break;

                case OPC_OPERAND: {     ip = decodeOperand(ip, ar);
                                        switch (resultReg) {
                                            case 1: rs1 = operandResult; break;
                                            case 2: rs2 = operandResult; break;
                                            case 3: rs3 = operandResult; break;
                                            default: shouldNotReachHere();
                                        }
                                        break;
                }

                /* int ops */

                case OPC_ADDI:          rs1 = rs1 + rs2;                            break;
                case OPC_SUBI:          rs1 = rs1 - rs2;                            break;
                case OPC_MULI:          rs1 = rs1 * rs2;                            break;
                case OPC_MOVI:          rs1 =         rs1;                          break;
                case OPC_NEGI:          rs1 = 0     - rs1;                          break;
                case OPC_ANDI:          rs1 = rs1 & rs2;                            break;
                case OPC_ORRI:          rs1 = rs1 | rs2;                            break;
                case OPC_XORI:          rs1 = rs1 ^ rs2;                            break;
                case OPC_SLLI:          rs1 = rs1 << rs2;                           break;
                case OPC_SRLI:          rs1 = srl(rs1, rs2);                        break;
                case OPC_SRAI:          rs1 = rs1 >> rs2;                           break;
                case OPC_I2B:           rs1 = i2b(rs1);                             break;
                case OPC_I2S:           rs1 = i2s(rs1);                             break;
                case OPC_I2C:           rs1 = i2c(rs1);                             break;

                case OPC_DIVI:          if (rs2 == 0) {
                                            code = OPC_DIV0;
                                            endCode = E_ERROR;
                                        } else {
                                            rs1 = rs1 / rs2;
                                        }
                                        break;

                case OPC_REMI:          if (rs2 == 0) {
                                            code = OPC_DIV0;
                                            endCode = E_ERROR;
                                        } else {
                                            rs1 = rs1 % rs2;
                                        }
                                        break;

                /* float ops */

/*FLT*/         case OPC_ADDF:          rs1 = addf(rs1, rs2);                       break;
/*FLT*/         case OPC_SUBF:          rs1 = subf(rs1, rs2);                       break;
/*FLT*/         case OPC_MULF:          rs1 = mulf(rs1, rs2);                       break;
/*FLT*/         case OPC_DIVF:          rs1 = divf(rs1, rs2);                       break;
/*FLT*/         case OPC_REMF:          rs1 = remf(rs1, rs2);                       break;
/*FLT*/         case OPC_NEGF:          rs1 = negf(rs1);                            break;
/*FLT*/         case OPC_CMPFL:         rs1 = cmpfl(rs1, rs2);                      break;
/*FLT*/         case OPC_CMPFG:         rs1 = cmpfg(rs1, rs2);                      break;
/*FLT*/         case OPC_F2I:           rs1 = f2i(rs1);                             break;
/*FLT*/         case OPC_I2F:           rs1 = i2f(rs1);                             break;

                /* Control flow ops */

                case OPC_IFEQ:          ip = (rs1 == rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_IFNE:          ip = (rs1 != rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_IFLT:          ip = (rs1 <  rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_IFLE:          ip = (rs1 <= rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_IFGT:          ip = (rs1 >  rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_IFGE:          ip = (rs1 >= rs2) ? decodeTarget(ar, ip) : ip+2;  break;
                case OPC_GOTO:          ip = decodeTarget(ar, ip);                        break;

                case OPC_TABLESWITCH: {
                                        int key, high, low;
                                        key = rs1;
                                        ip = decodeOperand(ip, ar);
                                        low = operandResult;
                                        ip = decodeOperand(ip, ar);
                                        high = operandResult;
                                        if ((key < low) || (key > high)) {
                                            ip = decodeTableTarget(ar, ip, -1);
                                        } else {
                                            ip = decodeTableTarget(ar, ip, key-low);
                                        }
                                        break;
                }

                case OPC_LOOKUPSWITCH: {
                                        int key, npairs, i, target;
                                        key = rs1;
                                        npairs = getUnsignedBytecode(ip++);
                                        target = decodeTableTarget(ar, ip, -1);
                                        for (i = 0 ; i < npairs ; i++) {
                                            if (key == decodeMatch(ip, i, npairs)) {
                                                target = decodeTableTarget(ar, ip, i);
                                                break;
                                            }
                                        }
                                        ip = target;
                                        break;
                }

                /* Memory ops */

                case OPC_ALENGTH:       if (rs1 == 0) {
                                            code = OPC_NPE;
                                            endCode = E_ERROR;
                                            break;
                                        }
                                        rs1 = getArrayLength(rs1);                  break;

                case OPC_GETI:          rs1 = intResult;                            break;
                case OPC_GETI2:         rs1 = intResult2;                           break;

                case OPC_LDB:           rs1 = getByte(rs1, rs2);                    break;
                case OPC_LDC:           rs1 = getUnsignedHalf(rs1, rs2);            break;
                case OPC_LDS:           rs1 = getHalf(rs1, rs2);                    break;
                case OPC_LDI:           rs1 = getWord(rs1, rs2);                    break;

                case OPC_STB:           setByte(rs1, rs2, rs3);                     break;
                case OPC_STS:           setHalf(rs1, rs2, rs3);                     break;
                case OPC_STI:           setWord(rs1, rs2, rs3);                     break;
                case OPC_STOOP:         setOop(rs1, rs2, rs3);                      break;

                case OPC_LDCONST: {     int mth   = getWord(ar, AR_method);
                                        int cno1  = getUnsignedByte(mth, MTH_classNumberHigh);
                                        int cno2  = getUnsignedByte(mth, MTH_classNumberLow);
                                        int cno   = (cno1<<8)+cno2;
                                        int cls   = getClassFromCNO(cno);
                                        int array = getWord(cls, CLS_constTable);
                                        assume(array != 0);
                                        assume(rs1 >= 0);
                                        assume(rs1 < getArrayLength(array));
                                        rs1 = getWord(array, rs1);
                                        break;
                }


                /* Return */

                case OPC_RETURNL:       intResult2 = rs2;
                                        /* Drop thru */
                case OPC_RETURNI:       intResult  = rs1;
                                        /* Drop thru */
                case OPC_RETURN: {      int dead_ar = ar;
                                        ar = getWord(ar, AR_previousAR);
                                        ip = getAbsoluteIp(getWord(ar, AR_method),getWord(ar, AR_ip));
                                        SAVE_AR(ar);
                                        SAVE_IP(ip);
                                        if (getTraceMethods()) {
                                            traceMethodExit(dead_ar);
                                        }
                                        freeActivation(dead_ar);

                                        break;
                }

                /* Native code interface */

                case OPC_PARM:          nativeParms[nativePointer++] = rs1;
                                        break;

                case OPC_EXEC:          if (rs1 == 0) {
                                            if (nativeParms[0] == OP_GC) {
                                                /*
                                                 * Set to restart on the next instruction and exit to call the GC
                                                 */
                                                setWord(ar, AR_ip, getRelativeIp(getWord(ar, AR_method),ip));
                                                return ar;
                                            }
                                            rs1 = executeChannel0(nativeParms, nativePointer);
                                        } else {
                                            rs1 = chan_execute(rs1, nativeParms, nativePointer);
                                        }
                                        nativePointer = 0;
                                        break;

                case OPC_ERROR:         rs1 = chan_error(rs1);
                                        break;

                case OPC_RESULT:        rs1 = chan_result(rs1);
                                        break;

                case OPC_GETAR:         rs1 = ar;
                                        break;

                case OPC_SETAR:         setWord(ar, AR_ip, getRelativeIp(getWord(ar,AR_method),ip));  /* Save current ip in old ar */
                                        ar = rs1;                                                     /* Set new ar */
//IFC//#ifndef PRODUCTION
                                        currentThreadID = rs2;
//IFC//#endif
                                        ip = getAbsoluteIp(getWord(ar, AR_method),getWord(ar, AR_ip));/* Set new ip */
                                        SAVE_AR(ar);
                                        SAVE_IP(ip);
                                        incSwitchCount();
                                        break;

                case OPC_MATH0:
                case OPC_MATH1: {       long res = math(nativeParms, nativePointer);
                                        nativePointer = 0;
                                        code = nativeParms[0];
                                        if (code != 0) {
                                            endCode = E_ERROR;
                                        } else {
                                            rs1        = (int)(res >> 32);
                                            intResult2 = (int)res;
                                        }
                                        break;
                }

                case OPC_YIELD:         if (--yieldCount > 0) {
                                            break;
                                        }
                                        yieldCount = YIELDCOUNT;
                                        incYieldCount();
                                        /* ...Drop thru... */
                case OPC_NPE:
                case OPC_OBE:
                case OPC_DIV0:
                                        /*
                                         * rs1 is an object pointer to Native.primitive(), and
                                         * OPC_YIELD, OPC_NPE, OPC_OBE, and OPC_DIV0 do not set rs1.
                                         * It is zeroed here to prevent the GC getting an invalid pointer.
                                         */
                                        rs1 = 0;
                                        /* ...Drop thru... */
                case OPC_THROW:
                case OPC_MENTER:
                case OPC_MEXIT:
                case OPC_INSTANCEOF:
                case OPC_CHECKCAST:
                case OPC_CHECKSTORE:
                case OPC_INVOKEVIRTUAL:
                case OPC_INVOKEABSOLUTE:
                case OPC_CLINIT: {      int newar = invoke(code, ar, ip, rs1, rs2);

                                       /*
                                        * If there was no memory was available for the activation record, write
                                        * the correct offset into the ar so the failing instruction will be
                                        * re-executed after the garbage collector has run.
                                        */
                                        if (newar == INVOKE_ERR_outOfMemory) {
                                            setWord(ar, AR_ip, getRelativeIp(getWord(ar, AR_method),currentInstruction));
                                            return ar;
                                        }
                                        if (getTraceAllocation()) {
                                            validateHeap();
                                        }

                                       /*
                                        * If the receiver was null, throw a NullPointerException
                                        */
                                        if (newar == INVOKE_ERR_nullPointer) {
                                            ip = currentInstruction;
                                            code = OPC_NPE;
                                            endCode = E_ERROR;
                                            break;
                                        }

                                       /*
                                        * Setup the new ar and ip
                                        */
                                        ar = newar;
                                        ip = getAbsoluteIp(getWord(ar, AR_method),getWord(ar, AR_ip));
                                        SAVE_AR(ar);
                                        SAVE_IP(ip);
                                        break;
                }

                case OPC_NEW: {         rs1 = newObject(rs1, rs2);
                                        if (getTraceAllocation()) {
                                            validateHeap();
                                        }
                                        if (rs1 == 0) {
                                            setWord(ar, AR_ip, getRelativeIp(getWord(ar, AR_method),currentInstruction));
                                            return ar;
                                        }
                                        break;
                }

                default: fatalVMError1("Unknown bytecode ", code);
            }
        }
    }


/*---------------------------------------------------------------------------*\
 *                                  Entrypoint                               *
\*---------------------------------------------------------------------------*/


    public void run() {
       /*
        * Get the primary activation record
        */
        int restart = getActivationRecord();

       /*
        * Allocate the emergency activation record
        */
        if (getEmergencyActivation() == 0) {
            setEmergencyActivation(newNativeActivation());
            if (getEmergencyActivation() == INVOKE_ERR_outOfMemory) {
                fatalVMError("Couldn't allocate emergency activation record - OutOfMemoryError");
            }
        }

        if (getTraceMethods()) {
            traceMethodEnter(restart,SLOT_vmstart);
        }

        /*
         * Loop forever
         */
        for (;;) {
            boolean res;

            /*
             * Run the interpreter until the garbage needs collecting
             */
            restart = interpret(restart);

            if (getTraceMethods()) {
                traceMsg("\n");
                trace_threadID();
                traceMsg("Starting gc restart = ");
                traceInt(restart);
                traceMsg(" mth = ");
                traceInt(getWord(restart, AR_method));
                traceMsg(" ip = ");
                traceInt(getWord(restart, AR_ip));
                traceMsg("\n");
            }

            /*
             * Save the current activation record and run the gc
             */
            setActivationRecord(restart);
            res = gc();
            restart = getActivationRecord();

            if (getTraceInstructions()) {
                traceMsg("\n");
                trace_threadID();
                traceMsg("**GC** res = ");
                traceMsg(res ? "true" : "false");
                traceMsg("\n");
            }

            /*
             * gc() returns false if the collection did not result in enough
             * memory being freed for the failing allocation to now succeed.
             * If this is the case cause Native.primitive() to be called with
             * The OPC_THROW opcode and zero for the exception object. This
             * will always succeed because the activation record used for this
             * call is preallocated as is the exception object.
             */
            if (!res) {
                int ip  = getAbsoluteIp(getWord(restart, AR_method),getWord(restart, AR_ip));
                restart = invokePrimitive(OPC_THROW, restart, ip, 0, 0);
                assume(restart != 0);
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                         Testing                                           *
\*---------------------------------------------------------------------------*/

           private void passed(String name) {
//               traceMsg("Test ");
//               traceMsg(name);
//               traceMsg(" passed\n");
           }

           private void failed(String name) {
               traceMsg("Test ");
               traceMsg(name);
               traceMsg(" failed\n");
           }

           private void test_result(String name, boolean b) {
               if (b) {
                   passed(name);
               } else {
                   failed(name);
               }
           }

//IFC//    private int   test_nativeParms[16];
/*IFJ*/    private int[] test_nativeParms = new int[16];
           double PI = 3.14159265358979323846;
           private long math_ddd(int opcode, long rs1, long rs2) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = (int)((rs1 >> 32) & 0xFFFFFFFF);
               test_nativeParms[2] = (int)( rs1        & 0xFFFFFFFF);
               test_nativeParms[3] = (int)((rs2 >> 32) & 0xFFFFFFFF);
               test_nativeParms[4] = (int)( rs2        & 0xFFFFFFFF);
               return math(test_nativeParms, 5);
           }
           private long math_ddw(int opcode, long rs1, int rs2) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = (int)((rs1 >> 32) & 0xFFFFFFFFL);
               test_nativeParms[2] = (int)( rs1        & 0xFFFFFFFFL);
               test_nativeParms[3] = rs2;
               return math(test_nativeParms, 4);
           }
           private long math_dd(int opcode, long rs1) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = (int)((rs1 >> 32) & 0xFFFFFFFFL);
               test_nativeParms[2] = (int)( rs1);
               return math(test_nativeParms, 3);
           }
           private int  math_wdd(int opcode, long rs1, long rs2) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = (int)((rs1 >> 32) & 0xFFFFFFFFL);
               test_nativeParms[2] = (int)( rs1        & 0xFFFFFFFFL);
               test_nativeParms[3] = (int)((rs2 >> 32) & 0xFFFFFFFFL);
               test_nativeParms[4] = (int)( rs2        & 0xFFFFFFFFL);
               return (int)(math(test_nativeParms, 5) >> 32);
           }
           private int  math_wd(int opcode, long rs1) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = (int)((rs1 >> 32) & 0xFFFFFFFF);
               test_nativeParms[2] = (int)( rs1        & 0xFFFFFFFF);
               return (int)(math(test_nativeParms, 3) >> 32);
           }
           private long math_dw(int opcode, int rs1) {
               test_nativeParms[0] = opcode;
               test_nativeParms[1] = rs1;
               return math(test_nativeParms, 2);
           }

           /*
            * Non-floating point math tests.
            */
           private void test_MOVL() {
               long res;
               test_result("MOVL", (res = math_dd(MATH_MOVL,  0xFFFFFFFFL)) ==  0xFFFFFFFFL);
/*IFJ*/        test_result("MOVL", math_dd(MATH_MOVL, -0xFFFFFFFFL) == -0xFFFFFFFFL); // MSC does not like!
           }
           private void test_NEGL() {
               test_result("NEGL", math_dd(MATH_NEGL,  123456L) == -123456L);
               test_result("NEGL", math_dd(MATH_NEGL, -123456L) ==  123456L);
           }
           private void test_ADDL() {
               test_result("ADDL", math_ddd(MATH_ADDL,  10L,  3L) ==  13L);
               test_result("ADDL", math_ddd(MATH_ADDL,  10L, -3L) ==  7L);
               test_result("ADDL", math_ddd(MATH_ADDL, -10L, -3L) == -13L);
           }
           private void test_ANDL() {
               test_result("ANDL", math_ddd(MATH_ANDL, 0L, 0L) == 0L);
           }
           private void test_DIVL() {
               test_result("DIVL", math_ddd(MATH_DIVL,  13L,  5L) ==  2L);
               test_result("DIVL", math_ddd(MATH_DIVL, -13L,  5L) == -2L);
               test_result("DIVL", math_ddd(MATH_DIVL,  13L, -5L) == -2L);
           }
           private void test_MULL() {
               test_result("MULL", math_ddd(MATH_MULL, 0L, 0L) == 0L);
           }
           private void test_ORRL() {
               test_result("ORRL", math_ddd(MATH_ORRL, 0L, 0L) == 0L);
           }
           private void test_REML() {
               test_result("REML", math_ddd(MATH_REML, 13L, 5L) == 3L);
           }
           private void test_SUBL() {
               test_result("SUBL", math_ddd(MATH_SUBL, 0L, 0L) == 0L);
           }
           private void test_XORL() {
               test_result("XORL", math_ddd(MATH_XORL, 0L, 1L) == 1L);
           }
           private void test_SLLL() {
               test_result("SLLL", math_ddw(MATH_SLLL, 1L, 2) == 4L);
           }
           private void test_SRAL() {
               test_result("SRAL", math_ddw(MATH_SRAL, -4L, 2) == -1L);
           }
           private void test_SRLL() {
               test_result("SRLL", math_ddw(MATH_SRLL, -1L, 62) == 3L);
           }
           private void test_I2L() {
               test_result("I2L", math_dw(MATH_I2L, 4) == 4L);
           }
           private void test_L2I() {
               test_result("L2I", math_wd(MATH_L2I, 4L) == 4);
           }
           private void test_CMPL() {
               test_result("CMPL", math_wdd(MATH_CMPDL, 1L, 1L) ==  0);
               test_result("CMPL", math_wdd(MATH_CMPDL, 1L, 0L) ==  1);
               test_result("CMPL", math_wdd(MATH_CMPDL, 0L, 1L) == -1);
           }


           /*
            * Floating point math tests.
            */
/*FLT*/    private void test_D2L() {
/*FLT*/        test_result("D2L", math_dd(MATH_D2L, d2lb(10.5)) == 10L);
/*FLT*/    }
/*FLT*/    private void test_L2D() {
/*FLT*/        test_result("L2D", math_dd(MATH_L2D, 10L) == d2lb(10.0));
/*FLT*/    }
/*FLT*/    private void test_NEGD() {
/*FLT*/        test_result("NEGD", math_dd(MATH_NEGD, d2lb( 123456)) == d2lb(-123456));
/*FLT*/        test_result("NEGD", math_dd(MATH_NEGD, d2lb(-123456)) == d2lb( 123456));
/*FLT*/    }
/*FLT*/    private void test_acos() {
/*FLT*/        test_result("acos", math_dd(MATH_acos, d2lb(-1.0))  == d2lb(PI));
/*FLT*/    }
/*FLT*/    private void test_asin() {
/*FLT*/    }
/*FLT*/    private void test_atan() {
/*FLT*/    }
/*FLT*/    private void test_ceil() {
/*FLT*/        test_result("ceil", math_dd(MATH_ceil, d2lb(0.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_cos() {
/*FLT*/        test_result("cos", math_dd(MATH_cos, d2lb(PI))   == d2lb(-1.0));
/*FLT*/        test_result("cos", math_dd(MATH_cos, d2lb(PI*2)) == d2lb( 1.0));
/*FLT*/    }
/*FLT*/    private void test_exp() {
/*FLT*/    }
/*FLT*/    private void test_floor() {
/*FLT*/        test_result("floor", math_dd(MATH_floor, d2lb(0.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_log() {
/*FLT*/        test_result("log", math_dd(MATH_log, d2lb(1.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_sin() {
/*FLT*/        test_result("sin", math_dd(MATH_sin, d2lb(PI/2))   == d2lb( 1.0));
/*FLT*/        test_result("sin", math_dd(MATH_sin, d2lb(PI*1.5)) == d2lb( -1.0));
/*FLT*/    }
/*FLT*/    private void test_sqrt() {
/*FLT*/        test_result("sqrt", math_dd(MATH_sqrt, d2lb(16.0)) == d2lb(4.0));
/*FLT*/    }
/*FLT*/    private void test_tan() {
/*FLT*/        /* test_result("tan", math_dd(MATH_tan, d2lb(0.0)) == d2lb(0.0)); */
/*FLT*/    }
/*FLT*/    private void test_ADDD() {
/*FLT*/        test_result("ADDD", math_ddd(MATH_ADDD, d2lb(0.0), d2lb(0.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_DIVD() {
/*FLT*/        test_result("DIVD", math_ddd(MATH_DIVD, d2lb( 13.0), d2lb( 5.0)) == d2lb( 2.6));
/*FLT*/        test_result("DIVD", math_ddd(MATH_DIVD, d2lb(-13.0), d2lb( 5.0)) == d2lb(-2.6));
/*FLT*/        test_result("DIVD", math_ddd(MATH_DIVD, d2lb( 13.0), d2lb(-5.0)) == d2lb(-2.6));
/*FLT*/    }
/*FLT*/    private void test_IEEEremainder() {
/*FLT*/        test_result("IEEEremainder", math_ddd(MATH_IEEEremainder, d2lb( 13.0), d2lb( 5.0)) == d2lb(-2.0));
/*FLT*/        test_result("IEEEremainder", math_ddd(MATH_IEEEremainder, d2lb(-13.0), d2lb( 5.0)) == d2lb( 2.0));
/*FLT*/        test_result("IEEEremainder", math_ddd(MATH_IEEEremainder, d2lb( 13.0), d2lb(-5.0)) == d2lb(-2.0));
/*FLT*/    }
/*FLT*/    private void test_MULD() {
/*FLT*/        test_result("MULD", math_ddd(MATH_MULD, d2lb(0.0), d2lb(0.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_REMD() {
/*FLT*/        test_result("REMD", math_ddd(MATH_REMD, d2lb(13.0), d2lb(5.0)) == d2lb(3.0));
/*FLT*/    }
/*FLT*/    private void test_SUBD() {
/*FLT*/        test_result("SUBD", math_ddd(MATH_SUBD, d2lb(0.0), d2lb(0.0)) == d2lb(0.0));
/*FLT*/    }
/*FLT*/    private void test_atan2() {
/*FLT*/        /*test_result("atan2", math_ddd(MATH_atan2, d2lb(90.0), d2lb(90.0)) == d2lb(3.0));*/
/*FLT*/    }
/*FLT*/    private void test_pow() {
/*FLT*/        /*test_result("pow", math_ddd(MATH_pow, d2lb(2.0), d2lb( 31.0)) == d2lb(((double) 2147483647) + 1));*/
/*FLT*/        /*test_result("pow", math_ddd(MATH_pow, d2lb(2.0), d2lb(-31.0)) == d2lb( (double)-2147483648));*/
/*FLT*/    }
/*FLT*/    private void test_F2D() {
/*FLT*/        test_result("F2D", math_dw(MATH_F2D, f2ib(4.5F)) == d2lb(4.5));
/*FLT*/    }
/*FLT*/    private void test_F2L() {
/*FLT*/        test_result("F2L", math_dw(MATH_F2L, f2ib(4.5F)) == 4L);
/*FLT*/    }
/*FLT*/    private void test_I2D() {
/*FLT*/        test_result("I2D", math_dw(MATH_I2D, 4) == d2lb(4.0));
/*FLT*/    }
/*FLT*/    private void test_D2F() {
/*FLT*/        test_result("D2F", math_wd(MATH_D2F, d2lb(4.5)) == f2ib(4.5F));
/*FLT*/    }
/*FLT*/    private void test_D2I() {
/*FLT*/        test_result("D2I", math_wd(MATH_D2I, d2lb(4.5)) == 4);
/*FLT*/    }
/*FLT*/    private void test_L2F() {
/*FLT*/        test_result("L2F", math_wd(MATH_L2F, 4L) == f2ib(4.0F));
/*FLT*/    }
/*FLT*/    private void test_CMPDG() {
/*FLT*/        test_result("CMPDG", math_wdd(MATH_CMPDG, d2lb(1.0), d2lb(0.0)) ==  1);
/*FLT*/        test_result("CMPDG", math_wdd(MATH_CMPDG, d2lb(0.0), d2lb(1.0)) == -1);
/*FLT*/        test_result("CMPDG", math_wdd(MATH_CMPDG, d2lb(1.0), d2lb(1.0)) ==  0);
/*FLT*/    }
/*FLT*/    private void test_CMPDL() {
/*FLT*/        test_result("CMPDL", math_wdd(MATH_CMPDL, d2lb(1.0), d2lb(0.0)) ==  1);
/*FLT*/        test_result("CMPDL", math_wdd(MATH_CMPDL, d2lb(0.0), d2lb(1.0)) == -1);
/*FLT*/        test_result("CMPDL", math_wdd(MATH_CMPDL, d2lb(1.0), d2lb(1.0)) ==  0);
/*FLT*/    }

           protected void runTests() {
               test_MOVL();
               test_MOVL();
               test_NEGL();
               test_ADDL();
               test_ANDL();
               test_DIVL();
               test_MULL();
               test_ORRL();
               test_REML();
               test_SUBL();
               test_XORL();
               test_SLLL();
               test_SRAL();
               test_SRLL();
               test_I2L();
               test_L2I();
               test_CMPL();
/*FLT*/        test_D2L();
/*FLT*/        test_L2D();
/*FLT*/        test_NEGD();
/*FLT*/        test_acos();
/*FLT*/        test_asin();
/*FLT*/        test_atan();
/*FLT*/        test_ceil();
/*FLT*/        test_cos();
/*FLT*/        test_exp();
/*FLT*/        test_floor();
/*FLT*/        test_log();
/*FLT*/        test_sin();
/*FLT*/        test_sqrt();
/*FLT*/        test_tan();
/*FLT*/        test_DIVD();
/*FLT*/        test_IEEEremainder();
/*FLT*/        test_MULD();
/*FLT*/        test_REMD();
/*FLT*/        test_SUBD();
/*FLT*/        test_atan2();
/*FLT*/        test_pow();
/*FLT*/        test_F2D();
/*FLT*/        test_F2L();
/*FLT*/        test_I2D();
/*FLT*/        test_D2F();
/*FLT*/        test_D2I();
/*FLT*/        test_L2F();
/*FLT*/        test_CMPDG();
/*FLT*/        test_CMPDL();
           }


/*IFJ*/}
