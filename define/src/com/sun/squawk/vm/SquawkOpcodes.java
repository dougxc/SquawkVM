//J2C:opcodes.h **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public interface SquawkOpcodes extends ClassNumbers {

/*IFJ*/public final static int
//IFC//enum {

    /* Channel 0 opcodes */

    OP_GETCHANNEL           = 1,
    OP_FREECHANNEL          = 2,
    OP_EXIT                 = 3,
    OP_GC                   = 4,
    OP_FREEMEM              = 5,
    OP_TOTALMEM             = 6,
    OP_GETTIME              = 7,
    OP_ARRAYCOPY            = 8,
    OP_GETEVENT             = 9,
    OP_WAITFOREVENT         = 10,
    OP_TRACE                = 11,
    OP_FATALVMERROR         = 12,
    OP_GETHEADER            = 13,
    OP_SETHEADER            = 14,
    OP_GETCLASS             = 15,
    OP_FREEAR               = 16,
    OP_GETCLASSTABLE        = 17,
    OP_GETARGC              = 18,
    OP_GETARGVCH            = 19,
    OP_GETCH                = 20,
    OP_PUTCH                = 21,
    OP_SETISOLATE           = 22,
    OP_GETARRAYLENGTH       = 23,

    /* Tracing opcodes */

    TRACE_SETTHRESHOLD           =  1,
    TRACE_GETTHRESHOLD           =  2,
    TRACE_METTHRESHOLD           =  3,
    TRACE_GETTRACEINSTRUCTIONS   =  4,
    TRACE_GETTRACEMETHODS        =  5,
    TRACE_GETTRACEALLOCATION     =  6,
    TRACE_GETTRACEGC             =  7,
    TRACE_GETTRACEGCVERBOSE      =  8,
    TRACE_SETTRACEINSTRUCTIONS   =  9,
    TRACE_SETTRACEMETHODS        = 10,
    TRACE_SETTRACEALLOCATION     = 11,
    TRACE_SETTRACEGC             = 12,
    TRACE_SETTRACEGCVERBOSE      = 13,

    /* I/O channel opcodes */

    /*OP_GETCHANNEL         = 1,*/
    /*OP_FREECHANNEL        = 2,*/
    OP_OPEN                 = 3,
    OP_CLOSE                = 4,
    OP_ACCEPT               = 5,
    OP_OPENINPUT            = 6,
    OP_CLOSEINPUT           = 7,
    OP_WRITEREAD            = 8,
    OP_READBYTE             = 9,
    OP_READSHORT            = 10,
    OP_READINT              = 11,
    OP_READLONG             = 12,
    OP_READBUF              = 13,
    OP_SKIP                 = 14,
    OP_AVAILABLE            = 15,
    OP_MARK                 = 16,
    OP_RESET                = 17,
    OP_MARKSUPPORTED        = 18,
    OP_OPENOUTPUT           = 19,
    OP_FLUSH                = 20,
    OP_CLOSEOUTPUT          = 21,
    OP_WRITEBYTE            = 22,
    OP_WRITESHORT           = 23,
    OP_WRITEINT             = 24,
    OP_WRITELONG            = 25,
    OP_WRITEBUF             = 26,

    /* Math functions */

    MATH_sin                = 1,        /* 2->2  */
    MATH_cos                = 2,        /* 2->2  */
    MATH_tan                = 3,        /* 2->2  */
    MATH_asin               = 4,        /* 2->2  */
    MATH_acos               = 5,        /* 2->2  */
    MATH_atan               = 6,        /* 2->2  */
    MATH_exp                = 7,        /* 2->2  */
    MATH_log                = 8,        /* 2->2  */
    MATH_sqrt               = 9,        /* 2->2  */
    MATH_ceil               = 10,       /* 2->2  */
    MATH_floor              = 11,       /* 2->2  */
    MATH_atan2              = 12,       /* 22->2 */
    MATH_pow                = 13,       /* 22->2 */
    MATH_IEEEremainder      = 14,       /* 22->2 */
    MATH_ADDD               = 15,       /* 22->2 */
    MATH_SUBD               = 16,       /* 22->2 */
    MATH_MULD               = 17,       /* 22->2 */
    MATH_DIVD               = 18,       /* 22->2 */
    MATH_REMD               = 19,       /* 22->2 */
    MATH_NEGD               = 20,       /* 2->2  */
    MATH_L2D                = 21,       /* 2->2  */
    MATH_F2D                = 22,       /* 1->2  */
    MATH_I2D                = 23,       /* 1->2  */
    MATH_ADDL               = 24,       /* 22->2 */
    MATH_SUBL               = 25,       /* 22->2 */
    MATH_MULL               = 26,       /* 22->2 */
    MATH_DIVL               = 27,       /* 22->2 */
    MATH_REML               = 28,       /* 22->2 */
    MATH_CMPL               = 29,       /* 22->1 */
    MATH_MOVL               = 30,       /* 22->2 */
    MATH_NEGL               = 31,       /* 22->2 */
    MATH_ANDL               = 32,       /* 22->2 */
    MATH_ORRL               = 33,       /* 22->2 */
    MATH_XORL               = 34,       /* 22->2 */
    MATH_SLLL               = 35,       /* 21->2 */
    MATH_SRLL               = 36,       /* 21->2 */
    MATH_SRAL               = 37,       /* 21->2 */
    MATH_LDL                = 38,       /* 11->2 */
    MATH_LDL_BC             = 39,       /* 11->2 */
    MATH_D2L                = 40,       /* 2->2  */
    MATH_F2L                = 41,       /* 1->2  */
    MATH_I2L                = 42,       /* 1->2  */
    MATH_L2F                = 43,       /* 2->1  */
    MATH_D2F                = 44,       /* 2->1  */
    MATH_L2I                = 45,       /* 2->1  */
    MATH_D2I                = 46,       /* 2->1  */
    MATH_CMPDL              = 47,       /* 22->1 */
    MATH_CMPDG              = 48,       /* 22->1 */
    MATH_STL                = 49,       /* 211->0 */
    MATH_STL_BC             = 50,       /* 211->0 */


    /* Object structure */

    OBJ_length              = -2, /* Only for array types */
    OBJ_header              = -1, /* Class or monitor pointer */


    /* ClassBase structure */
                                        // Type         Ref
    CLS_self                = 0,        // Class        Y       +
    CLS_classIndex          = 1,        // int          N       |
    CLS_accessFlags         = 2,        // int          N       |   Ox1
    CLS_gctype              = 3,        // int          N       +

    CLS_length              = 4,        // int          N       +
    CLS_className           = 5,        // String       Y       |
    CLS_superClass          = 6,        // Class        Y       |   OxE
    CLS_elementType         = 7,        // Class        Y       +

    CLS_interfaces          = 8,        // Class[]      Y       +
    CLS_vtable              = 9,        // byte[][]     Y       |
    CLS_vstart              = 10,       // int          N       |   Ox3
    CLS_vcount              = 11,       // int          N       +

    CLS_fvtable             = 12,       // byte[][]     Y       +
    CLS_itable              = 13,       // short[]      Y       |
    CLS_istart              = 14,       // int          N       |   0xB
    CLS_iftable             = 15,       // short[]      Y       +

    CLS_sftable             = 16,       // short[]      Y       +
    CLS_constTable          = 17,       // Object[]     Y       |   0xF
    CLS_oopMap              = 18,       // byte[]       Y       |
    CLS_debugInfo           = 19,       // byte[]       Y       |

    CLS_LENGTH              = 20,

    CLS_MAP0                = 0xE1,
    CLS_MAP1                = 0xB3,
    CLS_MAP2                = 0x0F,

    /* Monitor structure */

    MON_realType            = 0,        // Class        Y       +
    MON_owner               = 1,        // Thread       Y       |
    MON_monitorQueue        = 2,        // Thread       Y       |   OxF
    MON_condvarQueue        = 3,        // Thread       Y       +

    MON_hashCode            = 4,        // int          N       +
    MON_depth               = 5,        // int          N       |   Ox0
    MON_isProxy             = 6,        // int          N       +

    MON_LENGTH              = 7,

    MON_MAP0                = 0x0F,

    /* String structure */

    STR_value               = 0,        // char[]       Y       +
    STR_offset              = 1,        // int          N       |   0x1
    STR_count               = 2,        // int          N       +
    STR_LENGTH              = 3,

    STR_MAP0                = 0x1,


    /* Class, field and method access flags */

    ACC_PUBLIC              = 0x0001,
    ACC_PRIVATE             = 0x0002,
    ACC_PROTECTED           = 0x0004,
    ACC_STATIC              = 0x0008,
    ACC_FINAL               = 0x0010,
    ACC_SYNCHRONIZED        = 0x0020,
    ACC_SUPER               = 0x0020,
    ACC_VOLATILE            = 0x0040,
    ACC_TRANSIENT           = 0x0080,
    ACC_NATIVE              = 0x0100,
    ACC_INTERFACE           = 0x0200,
    ACC_ABSTRACT            = 0x0400,
    ACC_LINKAGEERROR        = 0x1000,
    ACC_HASCLINIT           = 0x8000,



    /* Basic class types */

    GCTYPE_spiritual        = 1,
    GCTYPE_nopointers       = 2,
    GCTYPE_object           = 3,
    GCTYPE_array            = 4,
    GCTYPE_byteArray        = GCTYPE_array + 0, /* Must be >= GCTYPE_array */
    GCTYPE_halfArray        = GCTYPE_array + 1, /* Must be >= GCTYPE_array */
    GCTYPE_wordArray        = GCTYPE_array + 2, /* Must be >= GCTYPE_array */
    GCTYPE_longArray        = GCTYPE_array + 3, /* Must be >= GCTYPE_array */
    GCTYPE_arArray          = GCTYPE_array + 4, /* Must be >= GCTYPE_array */
    GCTYPE_gvArray          = GCTYPE_array + 5, /* Must be >= GCTYPE_array */
    GCTYPE_oopArray         = GCTYPE_array + 6, /* Must be >= GCTYPE_array */

    /* Activation record structure */

    AR_method               = 0,
    AR_ip                   = 1,
    AR_previousAR           = 2,
    AR_locals               = 3,
    AR_IsolateState         = -1,

    /* Method structure */

    MTH_arSizeHigh          = 0,
    MTH_arSizeLow           = 1,
    MTH_classNumberHigh     = 2,
    MTH_classNumberLow      = 3,
    MTH_nparmsIndex         = 4,
    MTH_oopMapLength        = 5,
    MTH_oopMap              = 6,

    /* IsolateState special entry indexes */

    ISO_FIRST               = 0,
    ISO_isolateState        = ISO_FIRST + 0, /* Pointer to isolate state                    */
    ISO_isolateStateOopMap  = ISO_FIRST + 1, /* Pointer to oop map for isolate state        */
    ISO_isolateStateLength  = ISO_FIRST + 2, /* The number of words used in the array       */
    ISO_classTable          = ISO_FIRST + 3, /* Pointer to the prototype objects            */
    ISO_classThreadTable    = ISO_FIRST + 4, /* Pointer to the class initializing threads   */
    ISO_classStateTable     = ISO_FIRST + 5, /* Pointer to the class state variables        */
    ISO_isolateId           = ISO_FIRST + 6, /* String identifier for isolate               */
    ISO_LAST                = ISO_FIRST + 6,

    ISO_MAP0                = 0x7b,

    /* Operand encoding flags */
    //ENC_MOREBYTES         = 0x80, /* Operand value continues in next byte */
    //ENC_CONST             = 0x40, /* Operand is a constant if set, a local variable otherwise */
    ENC_HIGH_BYTE_BITS    = 6,    /* Bits in high byte available for storing operand value */
    //ENC_OTHER_BYTE_BITS   = 7,    /* Bits in remaining bytes available for storing operand value */

    ENC_COMPLEX = 0x80,
    ENC_CONST   = 0x40,
    ENC_STATIC  = 0x20,

    /* Internal exception codes */

    EXNO_None                           = 0,
    EXNO_IOException                    = 1,
    EXNO_NoConnection                   = 2,
    //EXNO_ArrayStoreException            = 3,
    //EXNO_ArrayIndexOutOfBoundsException = 4,

    /* Unit of yield scheduling */

    YIELDCOUNT              = 1000,

    /* The maximum length (in bytes) of constants */
    MAX_WORD_ENCODING        = 5,
    MAX_DWORD_ENCODING       = 10,

    /* Heap image */

    HEAP_MAGICNUMBER            = 0x03021957,
    HEAP_VERSION                = 100,

    HEAP_magicNumber            = 0,
    HEAP_version                = 1,
    HEAP_failedAllocationSize   = 2,
    HEAP_isolateState           = 3,    /* ref */
    HEAP_activationRecord       = 4,    /* ref */
    HEAP_emergencyActivation    = 5,    /* ref */
    HEAP_primitiveMethod        = 6,    /* ref */
    HEAP_heapSize               = 7,
    HEAP_currentSpace           = 8,
    HEAP_currentSpaceFreePtr    = 9,
    HEAP_currentSpaceEnd        = 10,
    HEAP_classMonitorProxy      = 11,
    HEAP_stringMonitorProxy     = 12,

    HEAP_heapStart              = 32,

    /* Error codes returned by Interpret.invoke - must be less that HEAP_heapStart */

    INVOKE_ERR_outOfMemory      = 0,
    INVOKE_ERR_nullPointer      = 1,

// temp
/*
 OPC_ADDL  = 99,
 OPC_ANDL  = 99,
 OPC_CMPDG = 99,
 OPC_CMPDL = 99,
 OPC_CMPL  = 99,
 OPC_D2F   = 99,
 OPC_D2I   = 99,
 OPC_D2L   = 99,
 OPC_DIVD  = 99,
 OPC_DIVL  = 99,
 OPC_F2D   = 99,
 OPC_F2L   = 99,
 OPC_I2D   = 99,
 OPC_I2L   = 99,
 OPC_L2D   = 99,
 OPC_L2F   = 99,
 OPC_L2I   = 99,
 OPC_LDL   = 99,
 OPC_MOVL  = 99,
 OPC_MULD  = 99,
 OPC_MULL  = 99,
 OPC_NEGL  = 99,
 OPC_ORRL  = 99,
 OPC_REMD  = 99,
 OPC_REML  = 99,
 OPC_SLLL  = 99,
 OPC_SRAL  = 99,
 OPC_SRLL  = 99,
 OPC_STL   = 99,
 OPC_SUBD  = 99,
 OPC_SUBL  = 99,
 OPC_XORL  = 99,
*/





    /* Bytecodes */

    OPC_UNUSED              = 0,
    OPC_NOP                 = 1,
    OPC_BREAK               = 2,
    OPC_ADDI                = 3,
    OPC_SUBI                = 4,
    OPC_MULI                = 5,
    OPC_DIVI                = 6,
    OPC_REMI                = 7,
    OPC_MOVI                = 8,
    OPC_NEGI                = 9,
    OPC_ANDI                = 10,
    OPC_ORRI                = 11,
    OPC_XORI                = 12,
    OPC_SLLI                = 13,
    OPC_SRLI                = 14,
    OPC_SRAI                = 15,
    OPC_I2B                 = 16,
    OPC_I2S                 = 17,
    OPC_I2C                 = 18,
    OPC_I2F                 = 19,
    OPC_ADDF                = 20,
    OPC_SUBF                = 21,
    OPC_MULF                = 22,
    OPC_DIVF                = 23,
    OPC_REMF                = 24,
    OPC_NEGF                = 25,
    OPC_CMPFL               = 26,
    OPC_CMPFG               = 27,
    OPC_F2I                 = 28,
    OPC_IFEQ                = 29,
    OPC_IFNE                = 30,
    OPC_IFLT                = 31,
    OPC_IFLE                = 32,
    OPC_IFGT                = 33,
    OPC_IFGE                = 34,
    OPC_GOTO                = 35,
    OPC_TABLESWITCH         = 36,
    OPC_LOOKUPSWITCH        = 37,
    OPC_GETI                = 38,
    OPC_GETI2               = 39,
    OPC_ALENGTH             = 40,
    OPC_RETURNL             = 41,
    OPC_RETURNI             = 42,
    OPC_RETURN              = 43,
    OPC_YIELD               = 44,
    OPC_INVOKEVIRTUAL       = 45,
    OPC_INVOKEABSOLUTE      = 46,
    OPC_TRYSTART            = 47,
    OPC_TRYEND              = 48,
    OPC_THROW               = 49,
    OPC_CLINIT              = 50,
    OPC_MENTER              = 51,
    OPC_MEXIT               = 52,
    OPC_INSTANCEOF          = 53,
    OPC_CHECKCAST           = 54,
    OPC_CHECKSTORE          = 55,
    OPC_NPE                 = 56,
    OPC_OBE                 = 57,
    OPC_DIV0                = 58,
    OPC_NEW                 = 59,
    OPC_PARM                = 60,
    OPC_EXEC                = 61,
    OPC_ERROR               = 62,
    OPC_RESULT              = 63,
    OPC_GETAR               = 64,
    OPC_SETAR               = 65,
    OPC_MATH0               = 66,
    OPC_MATH1               = 67,
    OPC_OPERAND             = 68,
    OPC_LDCONST             = 69,

    /* Put new opcodes here and update OPC_LOADSTORES */

    OPC_LOADSTORES          = 70,
    OPC_LDB                 = OPC_LOADSTORES+0,       /* Loads and stores must be last */
    OPC_LDC                 = OPC_LOADSTORES+1,
    OPC_LDS                 = OPC_LOADSTORES+2,
    OPC_LDI                 = OPC_LOADSTORES+3,
    OPC_STB                 = OPC_LOADSTORES+4,
    OPC_STS                 = OPC_LOADSTORES+5,
    OPC_STI                 = OPC_LOADSTORES+6,
    OPC_STOOP               = OPC_LOADSTORES+7,
    OPC_LOADSTORES_BC_INC   =(OPC_STOOP - OPC_LOADSTORES) + 1, /* Increment to opcode to make it a bounds checking version */
    OPC_LDB_BC              = OPC_LDB   + 8,          /* Bound check versions must be the very last */
    OPC_LDC_BC              = OPC_LDC   + 8,
    OPC_LDS_BC              = OPC_LDS   + 8,
    OPC_LDI_BC              = OPC_LDI   + 8,
    OPC_STB_BC              = OPC_STB   + 8,
    OPC_STS_BC              = OPC_STS   + 8,
    OPC_STI_BC              = OPC_STI   + 8,
    OPC_STOOP_BC            = OPC_STOOP + 8,

    /* Simple opcode flag */
    OPC_SIMPLE              = 0x80,


/*
 *       E E . . . F F F
 *
 *                 ^ ^ ^
 *                 Format code
 *
 *       ^ ^
 *       End code
 */

    FMT_III  = 3,           /* int, int, int */
    FMT_II   = 2,           /* int, int */
    FMT_I    = 1,           /* int */
    FMT_NONE = 0,           /* No operands */
    FMT_MASK = 0x07,

    E_WBI    = 1<<6,        /* Write back int after executing bytecode */
    E_ERROR  = 2<<6,        /* Write back int after executing bytecode */
    E_MASK   = E_WBI | E_ERROR,




DUMMY = 999
//IFC//}
;


/*---------------------------------------------------------------------------*\
 *                                 Operand table                             *
\*---------------------------------------------------------------------------*/

/*IFJ*/public static int[] operandTable = new int[] {
//IFC//unsigned char operandTable[] = {

        FMT_NONE,                                           /*UNUSED*/
        FMT_NONE,                                           /*NOP*/
        FMT_NONE,                                           /*BREAK*/
        FMT_II                          | E_WBI,            /*ADDI*/
        FMT_II                          | E_WBI,            /*SUBI*/
        FMT_II                          | E_WBI,            /*MULI*/
        FMT_II                          | E_WBI,            /*DIVI*/
        FMT_II                          | E_WBI,            /*REMI*/
        FMT_I                           | E_WBI,            /*MOVI*/
        FMT_I                           | E_WBI,            /*NEGI*/
        FMT_II                          | E_WBI,            /*ANDI*/
        FMT_II                          | E_WBI,            /*ORRI*/
        FMT_II                          | E_WBI,            /*XORI*/
        FMT_II                          | E_WBI,            /*SLLI*/
        FMT_II                          | E_WBI,            /*SRLI*/
        FMT_II                          | E_WBI,            /*SRAI*/
        FMT_I                           | E_WBI,            /*I2B*/
        FMT_I                           | E_WBI,            /*I2S*/
        FMT_I                           | E_WBI,            /*I2C*/
        FMT_I                           | E_WBI,            /*I2F*/
        FMT_II                          | E_WBI,            /*ADDF*/
        FMT_II                          | E_WBI,            /*SUBF*/
        FMT_II                          | E_WBI,            /*MULF*/
        FMT_II                          | E_WBI,            /*DIVF*/
        FMT_II                          | E_WBI,            /*REMF*/
        FMT_I                           | E_WBI,            /*NEGF*/
        FMT_II                          | E_WBI,            /*CMPFL*/
        FMT_II                          | E_WBI,            /*CMPFG*/
        FMT_I                           | E_WBI,            /*F2I*/
        FMT_II,                                             /*IFEQ*/
        FMT_II,                                             /*IFNE*/
        FMT_II,                                             /*IFLT*/
        FMT_II,                                             /*IFLE*/
        FMT_II,                                             /*IFGT*/
        FMT_II,                                             /*IFGE*/
        FMT_NONE,                                           /*GOTO*/
        FMT_I,                                              /*TABLESWITCH*/
        FMT_I,                                              /*LOOKUPSWITCH*/
        FMT_NONE                        | E_WBI,            /*GETI*/
        FMT_NONE                        | E_WBI,            /*GETI2*/
        FMT_I                           | E_WBI,            /*ALENGTH*/
        FMT_II,                                             /*RETURNL*/
        FMT_I,                                              /*RETURNI*/
        FMT_NONE,                                           /*RETURN*/
        FMT_NONE,                                           /*YIELD*/
        FMT_NONE,                                           /*INVOKEVIRTUAL*/
        FMT_NONE,                                           /*INVOKEABSOLUTE*/
        FMT_II,                                             /*TRYSTART*/
        FMT_NONE,                                           /*TRYEND*/
        FMT_I,                                              /*THROW*/
        FMT_I,                                              /*CLINIT*/
        FMT_I,                                              /*MENTER*/
        FMT_I,                                              /*MEXIT*/
        FMT_II,                                             /*INSTANCEOF*/
        FMT_II,                                             /*CHECKCAST*/
        FMT_II,                                             /*CHECKSTORE*/
        FMT_NONE,                                           /*NPE*/
        FMT_NONE,                                           /*OBE*/
        FMT_NONE,                                           /*DIV0*/
        FMT_II                          | E_WBI,            /*NEW*/
        FMT_I,                                              /*PARM*/
        FMT_I                           | E_WBI,            /*EXEC*/
        FMT_I                           | E_WBI,            /*ERROR*/
        FMT_I                           | E_WBI,            /*RESULT*/
        FMT_NONE                        | E_WBI,            /*GETAR*/
        FMT_II,                                             /*SETAR*/
        FMT_NONE,                                           /*MATH0*/
        FMT_NONE                        | E_WBI,            /*MATH1*/
        FMT_NONE,                                           /*OPERAND*/
        FMT_I                           | E_WBI,            /*LDCONST*/
        FMT_II                          | E_WBI,            /*LDB*/         /* These must be before the next 10 */
        FMT_II                          | E_WBI,            /*LDC*/
        FMT_II                          | E_WBI,            /*LDS*/
        FMT_II                          | E_WBI,            /*LDI*/
        FMT_III,                                            /*STB*/
        FMT_III,                                            /*STS*/
        FMT_III,                                            /*STI*/
        FMT_III,                                            /*STOOP*/
        FMT_II                          | E_WBI,            /*LDB_BC*/      /* These must be after the last 10 */
        FMT_II                          | E_WBI,            /*LDC_BC*/
        FMT_II                          | E_WBI,            /*LDS_BC*/
        FMT_II                          | E_WBI,            /*LDI_BC*/
        FMT_III,                                            /*STB_BC*/
        FMT_III,                                            /*STS_BC*/
        FMT_III,                                            /*STI_BC*/
        FMT_III,                                            /*STOOP_BC*/
        0
};

/*IFJ*/}
