/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

interface SquawkTags {

    final static int T_ARRAYOF          = 1;
    final static int T_ABSTRACT         = 2;
    final static int T_BYTE_ARRAY       = 3;
    final static int T_CHAR_ARRAY       = 4;
    final static int T_CLASS            = 5;
    final static int T_CONSTANTS        = 6;
    final static int T_DOUBLE_ARRAY     = 7;
    final static int T_EXTENDS          = 8;
    final static int T_FLOAT_ARRAY      = 9;
    final static int T_FROM             = 10;
    final static int T_I                = 11;
    final static int T_IMPLEMENTS       = 12;
    final static int T_INSTANCE_VARS    = 13;
    final static int T_INSTRUCTIONS     = 14;
    final static int T_INTERFACE_MAP    = 15;
    final static int T_INTERFACE        = 16;
    final static int T_INT_ARRAY        = 17;
    final static int T_LINE_NUMBER_MAP  = 18;
    final static int T_LINKAGE_ERROR    = 19;
    final static int T_LOCAL_VARS       = 20;
    final static int T_LONG_ARRAY       = 21;
    final static int T_METHOD           = 22;
    final static int T_METHODS_V        = 23;
    final static int T_METHODS_NON_V    = 24;
    final static int T_NAME             = 25;
    final static int T_NUMBER           = 26;
    final static int T_NATIVE           = 27;
    final static int T_PARAMETER_MAP    = 28;
    final static int T_SHORT_ARRAY      = 29;
    final static int T_SLOT             = 30;
    final static int T_SOURCEFILE       = 31;
    final static int T_SQUAWK           = 32;
    final static int T_STATIC           = 33;
    final static int T_STATIC_VARS      = 34;
    final static int T_STRING           = 35;
    final static int T_SUPER            = 36;
    final static int T_TO               = 37;

    // These 5 must be contiguous as the TYPE_SIZE array below is
    // indexed by these values.
    final static int T_DWORD            = 38;
    final static int T_REF              = 39;
    final static int T_WORD             = 40;
    final static int T_HALF             = 41;
    final static int T_BYTE             = 42;

    // The emtpy versions of the last 4 element types
    final static int T_BYTE_EMPTY       = 43;
    final static int T_DWORD_EMPTY      = 44;
    final static int T_HALF_EMPTY       = 45;
    final static int T_WORD_EMPTY       = 46;
    final static int T_REF_EMPTY        = 47;

    final static int NUM_OF_TAGS        = 48;

    // Attributes
    final static byte A_XMLNS           = 1;
    final static byte A_LINE            = 2;

}
