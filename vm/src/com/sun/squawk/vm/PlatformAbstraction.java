//J2C:platform.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import  java.util.*;
/*IFJ*/import  java.io.*;
/*IFJ*/import  java.net.*;
/*IFJ*/import  com.sun.squawk.util.*;
/*IFJ*/import  javax.microedition.io.*;
/*IFJ*/abstract class PlatformAbstraction implements SquawkOpcodes {

/*IFJ*/ChannelIO cio;

/*---------------------------------------------------------------------------*\
 *                            Forward References                             *
\*---------------------------------------------------------------------------*/

    abstract void setInitialMemorySize(int size);
    abstract int  getInitialMemorySize();
    abstract void trace_threadID();
    abstract void traceJavaStack();


/*---------------------------------------------------------------------------*\
 *                                  Tracing                                  *
\*---------------------------------------------------------------------------*/

/*IFJ*/PrintStream out = System.err;
    protected void PlatformAbstraction_init(String traceURLOpt) {
/*IFJ*/        OutputStream os = null;
/*IFJ*/        if (traceURLOpt != null) {
/*IFJ*/            String url = traceURLOpt.substring("-XtraceURL:".length());
/*IFJ*/            try {
/*IFJ*/                os = Connector.openOutputStream(url);
/*IFJ*/                System.err.println("Tracing to " + url);
/*IFJ*/            } catch (IOException ioe) {
/*IFJ*/                System.err.println("Couldn't open " + url + " for trace output: tracing disabled");
/*IFJ*/                os = new PrintStream(System.err) {
/*IFJ*/                    public void write(int b) {}
/*IFJ*/                    public void write(byte[] buf, int off, int len) {}
/*IFJ*/                };
/*IFJ*/            }
/*IFJ*/        }
/*IFJ*/        if (os == null) {
/*IFJ*/ //           os = new BufferedOutputStream(System.out,1000000);
/*IFJ*/           os = System.err;
/*IFJ*/        }
/*IFJ*/        out = new PrintStream(os);
    }

    /*
     * printMsg
     */
    void printMsg(String str) {
/*IFJ*/ System.out.print(str);
//IFC// printf("%s", str);
    }

    /*
     * printInt
     */
    void printInt(int i) {
/*IFJ*/ System.out.print(i);
//IFC// printf("%d", i);
    }

    /*
     * traceMsg
     */
    void traceMsg(String str) {
/*IFJ*/ out.print(str);
//IFC// printf("%s", str);
    }

    /*
     * traceInt
     */
    void traceInt(int i) {
/*IFJ*/ out.print(i);
//IFC// printf("%d", i);
    }

    /*
     * traceHex
     */
    void traceHex(int i) {
        traceInt(i);
    }

    /*
     * traceLong
     */
    void traceLong(long l) {
/*IFJ*/ out.print(l);
//IFC// printf("%%Ld", l);
    }

    /*
     * traceChar
     */
    void traceChar(char c) {
/*IFJ*/ assume(!Character.isISOControl(c));
/*IFJ*/ out.print(c);
//IFC// printf("%c", c);
    }

    /*
     * printStat
     */
    void printStat(String msg, long l) {
/*IFJ*/System.err.println(msg+l);
//IFC//printf("%s%%Ld\n", msg, l);
    }

    /*
     * traceOpcode
     */
    void traceOpcode(int opcode) {
//IFC// traceInt(opcode); /* instruction opcode */
/*IFJ*/ traceMsg(Disassembler.getOpString(opcode).trim()); /* instruction mnemonic */
    }

    /*
     * traceMathOpcode
     */
    void traceMathOp(int op) {
//IFC// traceInt(op); /* instruction opcode */
/*IFJ*/ traceMsg(Disassembler.getOpString(op,Disassembler.MATH_OPS).trim()); /* instruction mnemonic */
    }


/*---------------------------------------------------------------------------*\
 *                                     Stats                                 *
\*---------------------------------------------------------------------------*/

//IFC//#ifndef PRODUCTION
    private long instructionCount       = 0;
    private long branchCount            = 0;
//IFC//#endif
    private long invokeCount            = 0;
    private long invokePrimitiveCount   = 0;
    private long yieldCount             = 0;
    private long switchCount            = 0;
    private long allocationCount        = 0;
    private long activationCount        = 0;
    private long collectionCount        = 0;
    private long globalCount            = 0;
    private long classCount             = 0;


/*---------------------------------------------------------------------------*\
 *                             Accessors for Stats                           *
\*---------------------------------------------------------------------------*/

//IFC//#ifndef PRODUCTION
    void incInstructionCount()      { instructionCount++;       }
    void incBranchCount()           { branchCount++;            }
//IFC//#else
//IFC//#define incInstructionCount() /**/
//IFC//#define incBranchCount()      /**/
//IFC//#endif
    void incInvokeCount()           { invokeCount++;            }
    void incInvokePrimitiveCount()  { invokePrimitiveCount++;   }
    void incYieldCount()            { yieldCount++;             }
    void incSwitchCount()           { switchCount++;            }
    void incAllocationCount()       { allocationCount++;        }
    void incActivationCount()       { activationCount++;        }
    void incCollectionCount()       { collectionCount++;        }
    void incGlobalCount()           { globalCount++;            }
    void incClassCount()            { classCount++;             }


    long getInvokeCount()           { return invokeCount;          }
    long getInvokePrimitiveCount()  { return invokePrimitiveCount; }
    long getInstructionCount()      { return instructionCount;     }
    long getTotalInvokeCount()      { return invokeCount +
                                             invokePrimitiveCount; }

   /*---------------------------------------------------------------------------*\
    *                                Termination                                *
   \*---------------------------------------------------------------------------*/

    /*
     * printVMStats
     */
    void printVMStats(int exitCode) {
        traceMsg("\n\n");
        printStat("Result                 ", exitCode            );
//IFC//#ifndef PRODUCTION
        printStat("Instructions           ", instructionCount    );
        printStat("Branches               ", branchCount         );
//IFC//#endif
        printStat("Invokes                ", invokeCount         );
        printStat("InvokePrimitives       ", invokePrimitiveCount);
        printStat("Yields                 ", yieldCount          );
        printStat("Switches               ", switchCount         );
        printStat("Allocations            ", allocationCount     );
        printStat("ActivationAllocations  ", activationCount     );
        printStat("Collections            ", collectionCount     );
        printStat("Globals                ", globalCount         );
        printStat("Classes                ", classCount          );
    }


    /*
     * exitToOperatingSystem
     */
    void exitToOperatingSystem(int exitCode) {
/*IFJ*/ out.close();
/*IFJ*/ System.exit(exitCode);
//IFC// exit(exitCode);
    }

    /*
     * exitVM
     */
    void exitVM(int exitCode) {
        printVMStats(exitCode);
        exitToOperatingSystem(exitCode);
    }


   /*---------------------------------------------------------------------------*\
    *                                Fatal Errors                               *
   \*---------------------------------------------------------------------------*/


    /*
     * dumpStack
     */
    void dumpStack() {
/*IFJ*/ (new Throwable("Native VM stack trace:")).printStackTrace();
    }

    /*
     * dumpHeap
     */
    void dumpHeap() {
    }

    /*
     * Guard to ensure we don't recurse into fatalError which may occur if there
     * is an error while dumping the stack or the heap.
     */
    boolean fatalVMErrorGuard = false;

    /*
     * fatalVMError1
     */
    void fatalVMError1(String msg, int value) {
        if (!fatalVMErrorGuard) {
            fatalVMErrorGuard = true;
/*IFJ*/     out.flush();
/*IFJ*/     out = new PrintStream(System.err);
            traceMsg("\n\n");
            trace_threadID();
            traceMsg(msg);
            traceMsg(" (exit value=");
            traceInt(value);
            traceMsg(")");
            dumpStack();
            traceJavaStack();
//            dumpHeap();
            traceMsg("\n");
        }
        else {
            traceMsg("fatalVMError called recursively");
        }
        exitVM(value);
    }

    /*
     * fatalVMError
     */
    void fatalVMError(String msg) {
        fatalVMError1(msg, 1);
    }

    /*
     * shouldNotReachHere
     */
    void  shouldNotReachHere() {
        fatalVMError("shouldNotReachHere()");
    }

//IFC//#ifndef PRODUCTION
    /*
     * assume
     */
    void assume(boolean b) {
         if (!b) {
             fatalVMError("Assume Failure");
         }
    }
//IFC//#else
//IFC//#define assume(x) /**/
//IFC//#endif

    /*
     * notImplementedYet
     */
    int notImplementedYet(String methodName) {
        traceMsg("Not implemented yet: ");
        traceMsg(methodName);
        exitToOperatingSystem(1);
        return 0;
    }

   /*---------------------------------------------------------------------------*\
    *                                Instructions                               *
   \*---------------------------------------------------------------------------*/

/*IFJ*/float  ib2f(int i)               { return Float.intBitsToFloat(i);       }
/*IFJ*/double lb2d(long l)              { return Double.longBitsToDouble(l);    }
/*IFJ*/int    f2ib(float f)             { return Float.floatToIntBits(f);       }
/*IFJ*/long   d2lb(double d)            { return Double.doubleToLongBits(d);    }
/*IFJ*/float  fmodf(float a, float b)   { return a % b;                         }
/*IFJ*/double fmodd(double a, double b) { return a % b;                         }

//IFC///*FLT*/union uu { int i; float f; jlong l; double d; };
//IFC///*FLT*/float  ib2f(int i)               { union uu x; x.i = i; return x.f;      }
//IFC///*FLT*/double lb2d(long l)              { union uu x; x.l = l; return x.d;      }
//IFC///*FLT*/int    f2ib(float f)             { union uu x; x.f = f; return x.i;      }
//IFC///*FLT*/long   d2lb(double d)            { union uu x; x.d = d; return x.l;      }
//IFC///*FLT*/float  fmodf(float a, float b)   { return (float)fmod(a, b);             }
//IFC///*FLT*/double fmodd(double a, double b) { return fmod(a, b);                    }

       void breakpoint()                { fatalVMError("Breakpoint");           }

/*IFJ*/int  srl(int a, int b)           { return a>>>b;                         }
//IFC//int  srl(int a, int b)           { return ((unsigned)a)>>b;              }
       int  i2b(int i)                  { return (byte)i;                       }
       int  i2s(int i)                  { return (short)i;                      }
       int  i2c(int i)                  { return (char)i;                       }
       long i2l(int i)                  { return (long)i;                       }
/*FLT*/int  i2f(int i)                  { return f2ib((float)i);                }
/*FLT*/long i2d(int i)                  { return d2lb((double)i);               }
/*IFJ*/long srll(long a, long b)        { return a>>>b;                         }
//IFC//long srll(long a, long b)        { return ((ulong)a)>>b;                 }
       int  l2i(long l)                 { return (int)l;                        }
/*FLT*/int  l2f(long l)                 { return f2ib((float)l);                }
/*FLT*/long l2d(long l)                 { return d2lb((double)l);               }
       int  cmpl(long l, long r)        { return (l < r) ? -1 : (l == r) ? 0 :1;}

/*FLT*/int  addf(int l, int r)          { return f2ib(ib2f(l) + ib2f(r));       }
/*FLT*/int  subf(int l, int r)          { return f2ib(ib2f(l) - ib2f(r));       }
/*FLT*/int  mulf(int l, int r)          { return f2ib(ib2f(l) * ib2f(r));       }
/*FLT*/int  divf(int l, int r)          { return f2ib(ib2f(l) / ib2f(r));       }
/*FLT*/int  remf(int l, int r)          { return f2ib(fmodf(ib2f(l), ib2f(r))); }
/*FLT*/int  negf(int l)                 { return f2ib(((float)0) - ib2f(l));    }
/*FLT*/int  f2i(int f)                  { return (int)ib2f(f);                  }
/*FLT*/long f2l(int f)                  { return (long)ib2f(f);                 }
/*FLT*/long f2d(int f)                  { return d2lb((double)ib2f(f));         }
/*FLT*/int  cmpf(float l, float r)      { return (l < r) ? -1 : (l == r) ? 0 :1;}
/*FLT*/int  cmpfl(int l, int r)         { return cmpf(ib2f(l), ib2f(r));        }
/*FLT*/int  cmpfg(int l, int r)         { return cmpf(ib2f(l), ib2f(r));        }

/*FLT*/long addd(long l, long r)        { return d2lb(lb2d(l) + lb2d(r));       }
/*FLT*/long subd(long l, long r)        { return d2lb(lb2d(l) - lb2d(r));       }
/*FLT*/long muld(long l, long r)        { return d2lb(lb2d(l) * lb2d(r));       }
/*FLT*/long divd(long l, long r)        { return d2lb(lb2d(l) / lb2d(r));       }
/*FLT*/long remd(long l, long r)        { return d2lb(fmodd(lb2d(l), lb2d(r))); }
/*FLT*/long negd(long l)                { return d2lb(((double)0) - lb2d(l));   }
/*FLT*/int  d2i(long l)                 { return (int)lb2d(l);                  }
/*FLT*/long d2l(long l)                 { return (long)lb2d(l);                 }
/*FLT*/int  d2f(long l)                 { return f2ib((float)lb2d(l));          }
/*FLT*/int  cmpd(double l, double r)    { return (l < r) ? -1 : (l == r) ? 0 :1;}
/*FLT*/int  cmpdl(long l, long r)       { return cmpd(lb2d(l), lb2d(r));        }
/*FLT*/int  cmpdg(long l, long r)       { return cmpd(lb2d(l), lb2d(r));        }


   /*---------------------------------------------------------------------------*\
    *                                Math functions                             *
   \*---------------------------------------------------------------------------*/

/*FLT*/long math0(int op, long rs1_l, long rs2_l) {
/*FLT*/    double rs1 = lb2d(rs1_l);
/*FLT*/    double rs2 = lb2d(rs2_l);
/*FLT*/    double res = 0.0;
/*FLT*/        switch (op) {
/*IFJ*/            case MATH_sin:           res =  Math.sin(rs1);                  break;
/*IFJ*/            case MATH_cos:           res =  Math.cos(rs1);                  break;
/*IFJ*/            case MATH_tan:           res =  Math.tan(rs1);                  break;
/*IFJ*/            case MATH_asin:          res =  Math.asin(rs1);                 break;
/*IFJ*/            case MATH_acos:          res =  Math.acos(rs1);                 break;
/*IFJ*/            case MATH_atan:          res =  Math.atan(rs1);                 break;
/*IFJ*/            case MATH_exp:           res =  Math.exp(rs1);                  break;
/*IFJ*/            case MATH_log:           res =  Math.log(rs1);                  break;
/*IFJ*/            case MATH_sqrt:          res =  Math.sqrt(rs1);                 break;
/*IFJ*/            case MATH_ceil:          res =  Math.ceil(rs1);                 break;
/*IFJ*/            case MATH_floor:         res =  Math.floor(rs1);                break;
/*IFJ*/            case MATH_atan2:         res =  Math.atan2(rs1, rs2);           break;
/*IFJ*/            case MATH_pow:           res =  Math.pow(rs1, rs2);             break;
/*IFJ*/            case MATH_IEEEremainder: res =  Math.IEEEremainder(rs1, rs2);   break;
//IFC///*FLT*/     case MATH_sin:           res =  sin(rs1);                       break;
//IFC///*FLT*/     case MATH_cos:           res =  cos(rs1);                       break;
//IFC///*FLT*/     case MATH_tan:           res =  tan(rs1);                       break;
//IFC///*FLT*/     case MATH_asin:          res =  asin(rs1);                      break;
//IFC///*FLT*/     case MATH_acos:          res =  acos(rs1);                      break;
//IFC///*FLT*/     case MATH_atan:          res =  atan(rs1);                      break;
//IFC///*FLT*/     case MATH_exp:           res =  exp(rs1);                       break;
//IFC///*FLT*/     case MATH_log:           res =  log(rs1);                       break;
//IFC///*FLT*/     case MATH_sqrt:          res =  sqrt(rs1);                      break;
//IFC///*FLT*/     case MATH_ceil:          res =  ceil(rs1);                      break;
//IFC///*FLT*/     case MATH_floor:         res =  floor(rs1);                     break;
//IFC///*FLT*/     case MATH_atan2:         res =  atan2(rs1, rs2);                break;
//IFC///*FLT*/     case MATH_pow:           res =  pow(rs1, rs2);                  break;
//IFC///*FLT*/     case MATH_IEEEremainder: {
//IFC///*FLT*/         double q = fmod(rs1, rs2);
//IFC///*FLT*/         double d = fabs(rs2);
//IFC///*FLT*/         if (q < 0) {
//IFC///*FLT*/             if (-q > d / 2) {
//IFC///*FLT*/                 q += d;
//IFC///*FLT*/             }
//IFC///*FLT*/         } else {
//IFC///*FLT*/             if (q > d / 2) {
//IFC///*FLT*/                 q -= d;
//IFC///*FLT*/             }
//IFC///*FLT*/         }
//IFC///*FLT*/         res = q;
//IFC///*FLT*/         break;
//IFC///*FLT*/     }
/*FLT*/            default:                 shouldNotReachHere();
/*FLT*/        }
/*FLT*/        return d2lb(res);
/*FLT*/    }


   /*---------------------------------------------------------------------------*\
    *                            Native code interface                          *
   \*---------------------------------------------------------------------------*/

    /*
     * getTime
     */
    long getTime() {
/*IFJ*/ return System.currentTimeMillis();
//IFC// return 1;
    }

    /*
     * getEvent
     */
    int getEvent() {
        return 0;
    }

    /*
     * readImage
     */
/*IFJ*/   int[] readImage(String imageOpt) {
/*IFJ*/       int[] image = null;
/*IFJ*/       try {
/*IFJ*/           String name = imageOpt.substring("-Ximage:".length());
/*IFJ*/           File imageFile = new File(name);
/*IFJ*/           int imageSize = (int)(imageFile.length() / 4);
/*IFJ*/           /* Account for semi-space collector by multiplying by 2 */
/*IFJ*/           int memorySize  = ((imageSize - HEAP_heapStart) * 2) + HEAP_heapStart + 1;
/*IFJ*/           DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(imageFile),imageSize*4));
/*IFJ*/           if (memorySize > getInitialMemorySize()) {
/*IFJ*/               setInitialMemorySize(memorySize);
/*IFJ*/           }
/*IFJ*/           image = new int[getInitialMemorySize()];
/*IFJ*/           for (int j = 0; j != imageSize; j++) {
/*IFJ*/               image[j] = dis.readInt();
/*IFJ*/           }
/*IFJ*/           dis.close();
/*IFJ*/       } catch (IOException ioe) {
/*IFJ*/           ioe.printStackTrace();
/*IFJ*/           image = null;
/*IFJ*/       }
/*IFJ*/       return image;
/*IFJ*/   }

//IFC//   int* readImage(String imageOpt) {
//IFC//       return (int*)notImplementedYet("readImage");
//IFC//   }

   /*---------------------------------------------------------------------------*\
    *                                 Channel I/O                               *
   \*---------------------------------------------------------------------------*/


    /*
     * chan_init
     */
/*IFJ*/void chan_init(Memory mem, boolean debug) {
/*IFJ*/    cio = new ChannelIO(mem, debug);
/*IFJ*/}

    /*
     * chan_execute
     */
    int chan_execute(int chan, int nativeParms[], int nparms) {
/*IFJ*/ return cio.execute(chan, nativeParms, nparms);
//IFC// return 1;
    }

    /*
     * chan_error
     */
    int chan_error(int chan) {
/*IFJ*/ return cio.error(chan);
//IFC// return 1;
    }

    /*
     * chan_result
     */
    int chan_result(int chan) {
/*IFJ*/ return cio.result(chan);
//IFC// return 1;
    }

    /*
     * waitForEvent
     */
    void waitForEvent(long time) {
/*IFJ*/ cio.waitFor(time);
//IFC// /*xxxx*/
    }



/*IFJ*/}









