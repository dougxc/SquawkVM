//J2C:squawk.c **DO NOT DELETE THIS LINE**

//IFC//#include "stdio.h"
//IFC//#include "stdlib.h"
//IFC//#include "malloc.h"
//IFC//
//IFC///*FLT*/#include "math.h"
//IFC//
//IFC//#define public /**/
//IFC//#define private /**/
//IFC//#define protected /**/
//IFC//#define abstract /**/
//IFC//#define true 1
//IFC//#define false 0
//IFC//#define boolean int
//IFC//#define null NULL
//IFC//#define byte signed char
//IFC//#define jlong  long long
//IFC//#define ujlong unsigned long long
//IFC//#ifdef _MSC_VER
//IFC//#undef jlong
//IFC//#undef ujlong
//IFC//#define jlong  __int64
//IFC//#define ujlong unsigned __int64
//IFC//#endif /* _MSC_VER */
//IFC//#ifdef __GNUC__
//IFC//#undef jlong
//IFC//#undef ujlong
//IFC//#define jlong  __int64_t
//IFC//#define ujlong __uint64_t
//IFC//#endif /* __GNUC__ */
//IFC//#define String char *
//IFC//#include "opcodes.h"
//IFC//#include "clscodes.h"
//IFC//#include "platform.c"
//IFC//#include "memory.c"
//IFC//#include "object.c"
//IFC//#include "interp.c"
//IFC//
//IFC//#ifndef DEFAULTMEMORYSIZE
//IFC//#define DEFAULTMEMORYSIZE   (64*1024)
//IFC//#endif
//IFC//

/*IFJ*/ package com.sun.squawk.vm;
/*IFJ*/ import  java.io.*;

/*IFJ*/ public class Interpreter extends Interpret {

/*IFJ*/     public final static int  DEFAULTMEMORYSIZE = (64*1024);

/*---------------------------------------------------------------------------*\
 *                                 Variables                                 *
\*---------------------------------------------------------------------------*/

/*IFJ*/     private String[] programArgv;
//IFC//     private char   **programArgv;
            private int      programArgc = 0;
            private int      initialMemorySize  = DEFAULTMEMORYSIZE;

            private int      traceThreshold    = 0;
            private boolean  traceInstructions = false;
            private boolean  traceMethods      = false;
            private boolean  traceAllocation   = false;
            private boolean  traceGC           = false;
            private boolean  traceGCVerbose    = false;


/*---------------------------------------------------------------------------*\
 *                    Implementation of forward references                   *
\*---------------------------------------------------------------------------*/

            void setInitialMemorySize(int size) {
                initialMemorySize = size;
            }
            int  getInitialMemorySize() {
                return initialMemorySize;
            }

/*---------------------------------------------------------------------------*\
 *                                   charAt                                  *
\*---------------------------------------------------------------------------*/

            private int charAt(String str, int pos) {
/*IFJ*/         if (pos == str.length()) {
/*IFJ*/             return 0;
/*IFJ*/         }
/*IFJ*/         return str.charAt(pos);
//IFC//         return str[pos];
            }

/*---------------------------------------------------------------------------*\
 *                               getArgumentCount                            *
\*---------------------------------------------------------------------------*/

            int getArgumentCount() {
                return programArgc;
            }

/*---------------------------------------------------------------------------*\
 *                 Accessors for tracing options                             *
\*---------------------------------------------------------------------------*/

           void    setThreshold(int value) { traceThreshold = value;                                                  }
           int     getThreshold()          { return traceThreshold;                                                   }
           boolean metThreshold()          { return traceThreshold <= 0 || traceThreshold <= getInstructionCount();   }

           boolean getTraceInstructions() { return traceInstructions           && metThreshold(); }
           boolean getTraceMethods()      { return traceMethods                && metThreshold(); }
           boolean getTraceAllocation()   { return traceAllocation             && metThreshold(); }
           boolean getTraceGC()           { return (traceGC || traceGCVerbose) && metThreshold(); }
           boolean getTraceGCVerbose()    { return traceGCVerbose              && metThreshold(); }

           void    setTraceInstructions(boolean b) { traceInstructions = b; }
           void    setTraceMethods(boolean b)      { traceMethods      = b; }
           void    setTraceAllocation(boolean b)   { traceAllocation   = b; }
           void    setTraceGC(boolean b)           { traceGC           = b; }
           void    setTraceGCVerbose(boolean b)    { traceGCVerbose    = b; }

/*---------------------------------------------------------------------------*\
 *                               getArgumentChar                             *
\*---------------------------------------------------------------------------*/

           int getArgumentChar(int arg, int pos) {
                String str = programArgv[arg];
                return charAt(str, pos);
           }

/*---------------------------------------------------------------------------*\
 *                                  startsWith                               *
\*---------------------------------------------------------------------------*/

            private boolean startsWith(String line, String match) {
                int i;
                for (i = 0 ;; i++) {
                    int ch      = charAt(line,  i);
                    int matchCh = charAt(match, i);
                    if (matchCh == 0) {
                        return true;
                    }
                    if (ch == 0) {
                        return false;
                    }
                    if (matchCh != ch) {
                        return false;
                    }
                }
            }


/*---------------------------------------------------------------------------*\
 *                                    equals                                 *
\*---------------------------------------------------------------------------*/

            private boolean equals(String line, String match) {
                int i;
                for (i = 0 ;; i++) {
                    int ch = charAt(line, i);
                    if (ch == 0) {
                        return charAt(match, i) == 0;
                    }
                    if (charAt(match, i) != ch) {
                        return false;
                    }
                }
            }


/*---------------------------------------------------------------------------*\
 *                               parseQuantity                               *
\*---------------------------------------------------------------------------*/

            private int parseQuantity(String line, int offset, String errMsg) {
                int i;
                int val = 0;
                for (i = offset ;; i++) {
                    int ch = charAt(line, i);
                    if (ch == 0) {
                        break;
                    } else if (ch >= '0' && ch <= '9') {
                        val = (val * 10) + (ch - '0');
                    } else if (ch == 'K') {
                        val *= 1024;
                        break;
                    } else if (ch == 'M') {
                        val *= (1024*1024);
                        break;
                    } else {
                        fatalVMError(errMsg);
                    }
                }
                return val;
            }

/*---------------------------------------------------------------------------*\
 *                               calculateMemory                             *
\*---------------------------------------------------------------------------*/

            private int calculateMemory(String line) {
                /* Memory size is expressed in bytes on the command line */
                return ((parseQuantity(line,4,"bad -Xms") + 3) / 4);
            }

/*---------------------------------------------------------------------------*\
 *                            usage                                          *
\*---------------------------------------------------------------------------*/

            void usage() {
                printMsg("Usage: squawk [-options] class [args...] \n");
                printMsg("where options include:\n");
                printMsg("    -Ximage:<image>     start VM using heap image in file <image>\n");
                printMsg("    -Xms<size>          set initial heap size\n");
//IFC//#ifndef PRODUCTION
                printMsg("    -XdebugIO           log IO channel input/output to files named 'channel<id>.input'\n");
                printMsg("                        and 'channel<id>.output' in the current directory\n");
                printMsg("    -XtraceThreshold:<num>\n");
                printMsg("                        delay any tracing until after <num> instructions have executed\n");
                printMsg("    -XtraceInstructions turn on instruction tracing\n");
                printMsg("    -XtraceMethods      turn on method tracing\n");
                printMsg("    -XtraceAllocations  turn on allocation tracing\n");
                printMsg("    -XtraceGC           turn on garbage collection tracing\n");
                printMsg("    -XtraceGCVerbose    turn on verbose garbage collection tracing\n");
                printMsg("    -XtraceURL:<url>    url for tracing output (default = standard output stream)\n");
                printMsg("    -XtraceGC           turn on garbage collection tracing during execution\n");
//IFC//#endif
                printMsg("    -Xhelp              display this message and exit\n");
            }

/*---------------------------------------------------------------------------*\
 *                               processArguments                            *
\*---------------------------------------------------------------------------*/

            private void processArguments(int argc, String argv[]) {
                int i;
                boolean usingImage = false;
                boolean debugIO    = false;
/*IFJ*/         int[] image        = null;
//IFC//         int*  image        = null;
                String imageFile   = null;
                String traceURL    = null;
                for (i = 0 ; i < argc ; i++) {
                    String arg = argv[i];
                    if (startsWith(arg, "-X")) {
                        if (equals(arg, "-Xhelp")) {
                            usage();
                            exitToOperatingSystem(0);
                        } else if (startsWith(arg, "-Xms")) {
                            setInitialMemorySize(calculateMemory(arg));
                        } else if (startsWith(arg, "-Ximage:")) {
                            imageFile = (arg);
                        } else if (equals(arg, "-XdebugIO")) {
                            debugIO = true;
//IFC//#ifndef PRODUCTION
                        } else if (startsWith(arg, "-XtraceURL:")) {
                            traceURL = (arg);
                        } else if (startsWith(arg, "-XtraceThreshold:")) {
                            traceThreshold = parseQuantity(arg, 17, "bad -XtraceThreshold");
                        } else if (equals(arg, "-XtraceInstructions")) {
                            traceInstructions = true;
                        } else if (equals(arg, "-XtraceMethods")) {
                            traceMethods = true;
                        } else if (equals(arg, "-XtraceAllocation")) {
                            traceAllocation = true;
                        } else if (equals(arg, "-XtraceGC")) {
                            traceGC = true;
                        } else if (equals(arg, "-XtraceGCVerbose")) {
                            traceGCVerbose = true;
//IFC//#endif
                        } else {
                            printMsg("Unknown option: ");
                            printMsg(arg);
                            printMsg("\n");
                            usage();
                            fatalVMError("Bad VM option");
                        }
                    }
                    else {
                        break;
                    }
                }

                while (i < argc) {
                    programArgv[programArgc++] = argv[i++];
                }

                /* the image loading can only be performed once all -Xms options have been parsed */
                if (imageFile != null) {
                    image = readImage(imageFile);
                    usingImage = (image != null);
                }
                if (image == null) {
/*IFJ*/             image = new int[getInitialMemorySize()];
//IFC//             image = (int*)malloc(memorySize);
                }
                PlatformAbstraction_init(traceURL);
                Memory_init(image, getInitialMemorySize());
/*IFJ*/         chan_init(this, debugIO); /* must come after Memory_init */
                if (!usingImage) {
                    ObjectMemory_init();
                }
                else {
                    ObjectMemory_reinit();
                }

                /* Run the VM level tests */
                runTests();
            }


/*---------------------------------------------------------------------------*\
 *                                 Entrypoint                                *
\*---------------------------------------------------------------------------*/

/*IFJ*/     public Interpreter(String[] args) {
/*IFJ*/         programArgv = new String[args.length];
/*IFJ*/         processArguments(args.length, args);
/*IFJ*/     }

/*IFJ*/     public static void main(String[] args) {
/*IFJ*/         new Interpreter(args).run();
/*IFJ*/     }

//IFC//     void main(int argc, char *argv[]) {
//IFC//         programArgv = (char **)malloc(argc * sizeof(char *));
//IFC//         processArguments(argc, argv);
//IFC//         run();
//IFC//     }

/*IFJ*/ }
