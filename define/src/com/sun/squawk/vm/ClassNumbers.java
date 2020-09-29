//J2C:clscodes.h **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public interface ClassNumbers {

/*IFJ*/public final static int
//IFC//enum {

    /* Class indexs into classTable */

    CNO_Object              = 1,
    CNO_ClassBase           = 2,
    CNO_VMPlatform          = 3,
    CNO_Class               = 4,
    CNO_Native              = 5,
    CNO_Math                = 6,
    CNO_String              = 7,
    CNO_Thread              = 8,
    CNO_Isolate             = 9,
    CNO_Monitor             = 10,
    CNO_System              = 11,
    CNO_StringBuffer        = 12,
    CNO_Test                = 13,
    CNO_primitive           = 14,
    CNO_void                = 15,
    CNO_int                 = 16,
    CNO_long                = 17,
    CNO_float               = 18,
    CNO_double              = 19,
    CNO_boolean             = 20,
    CNO_char                = 21,
    CNO_short               = 22,
    CNO_byte                = 23,
    CNO_global              = 24,
    CNO_local               = 25,
    CNO_Throwable           = 26,
    CNO_Error               = 27,
    CNO_Exception           = 28,
    CNO_intArray            = 29,
    CNO_longArray           = 30,
    CNO_floatArray          = 31,
    CNO_doubleArray         = 32,
    CNO_booleanArray        = 33,
    CNO_charArray           = 34,
    CNO_shortArray          = 35,
    CNO_byteArray           = 36,
    CNO_globalArray         = 37,
    CNO_localArray          = 38,
    CNO_ObjectArray         = 39,
    CNO_StringArray         = 40,
    CNO_ClassBaseArray      = 41,
    CNO_ClassArray          = 42,
    CNO_ThreadArray         = 43,
    CNO_byteArrayArray      = 44,
    CNO_LinkageError        = 45,

    CNO_InitLimit           = 45,

    CNO_PRIMITIVE           = CNO_Native,  /* Class containing primitive method */
    CNO_VMSTART             = CNO_Object,  /* Class containg the appropriate vmstart method */

    /* Special method slots - must be in sync with definition of java.lang.Object */

    SLOT_FIRST              = 1,

    SLOT_clinit             = 1,
    SLOT_init               = 2,
    SLOT_vmstart            = 3,
    SLOT_primitive          = 4,

    SLOT_FVTABLE_LENGTH     = 5

/*IFJ*/;}
//IFC//};



