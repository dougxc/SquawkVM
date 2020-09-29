/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)StaticFieldComponent.java	1.11
// Version:1.11
// Date:02/01/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/StaticFieldComponent.java
// Modified:02/01/02 11:15:51
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.JCSystem;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.Errors;

/**
 * This class implements methods to link COMPONENT_StaticField
 */
class StaticFieldComponent extends Component {

    // states
    static final short STATE_READY = (byte)0;
    static final short STATE_TAG = (byte)1;
    static final short STATE_SIZE = (byte)2;
    static final short STATE_IMAGE_SIZE = (byte)3;
    static final short STATE_REF_COUNT = (byte)4;
    static final short STATE_AI_COUNT = (byte)5;
    static final short STATE_AI_INFO_TYPE = (byte)6;
    static final short STATE_AI_INFO_COUNT = (byte)7;
    static final short STATE_AI_INFO_VALUES = (byte)8;
    static final short STATE_DEFAULT_COUNT = (byte)9;
    static final short STATE_ND_COUNT = (byte)10;
    static final short STATE_ND_VALUES = (byte)11;
    static final short STATE_DONE = (byte)12;
    static final short STATE_NEXT = (byte)13;

    // constants
    static final byte BYTE_AS_TRUE = (byte)0x01;

    // instance variables
    short f_currentState;
    short f_offset;
    byte f_aiType;
    short f_aiCount;
    short f_aiOffset;
    short f_ndOffset;
    short f_aiByteCount;
    short f_aiElementCount;
    short f_ndByteCount;
    Object f_objectArray;
    short f_statAddr;
    short f_seg2Count;

    /**
     * initialize
     * @exception InstallerException
     */
    void init() throws InstallerException {
        f_currentState = STATE_READY;
        f_statAddr = g_componentAddresses[CAP.COMPONENT_STATICFIELD];
        f_offset = (short)0;
        f_ndOffset = (short)0;
        resetComponentProcessor();
    }

    /**
     * link COMPONENT_STATICFIELD
     * @exception InstallerException
     */
    void process() throws InstallerException {
        byte pkgContext = PackageMgr.getPkgContext(PackageMgr.g_newPackageIdentifier);
        resetSegmentProcessor();

        while (hasMoreData()) {
            switch (getNextState()) {
                case STATE_TAG:
                    getByte(); // skip
                    break;

                case STATE_IMAGE_SIZE:
                    getShort(); // skip
                    break;

                case STATE_REF_COUNT:
                    g_staticReferenceCount = getShort(); // skip
                    break;

                case STATE_SIZE:
                    storeComponentSize();
                    break;

                case STATE_AI_COUNT:
                    // get array_init_count
                    f_aiCount = getShort();
                    f_seg2Count = (short)(g_staticReferenceCount - f_aiCount);
                    break;

                case STATE_AI_INFO_TYPE:
                    f_aiCount--;

                    // get array_init_info type
                    f_aiType = getByte();

                    // be ready for a new array
                    f_aiOffset = (short)0;
                    break;

                case STATE_AI_INFO_COUNT:
                    // get array_init_info (byte) count
                    f_aiByteCount = getShort();
                    f_aiElementCount = f_aiByteCount;
            				JCSystem.beginTransaction();
                    // create an array object
                    switch (f_aiType) {
                        case CAP.TYPE_BOOLEAN:
                            f_objectArray = (Object)new boolean[f_aiElementCount];
                            break;

                        case CAP.TYPE_BYTE:
                            f_objectArray = (Object)new byte[f_aiElementCount];
                            break;

                        case CAP.TYPE_SHORT:

                            /*
                             * calculate the array element count by dividing
                             * the count by the data type size
                             */
                            f_aiElementCount = (short)(f_aiByteCount/CAP.U2_SIZE);
                            f_objectArray = (Object)new short[f_aiElementCount];
                            break;

                        case CAP.TYPE_INT:

                            /*
                             * calculate the array element count by dividing
                             * the count by the data type size
                             */
                            f_aiElementCount = (short)(f_aiByteCount/CAP.U4_SIZE);
                            if (CAP.INTEGER_MODE) {
                                f_objectArray = (Object)new int[f_aiElementCount];
                            } else {
                                InstallerException.throwIt(
                                        Errors.INTEGER_UNSUPPORTED);
                            }
                            break;
                        }
					              JCSystem.commitTransaction();
                        break;

                case STATE_AI_INFO_VALUES:
                    // initialize an element in the newly created array object
                    if (f_aiByteCount!=0) {
                        switch (f_aiType) {
                            case CAP.TYPE_BOOLEAN:
                                ((boolean[])f_objectArray)[f_aiOffset] =
                                        (getByte() == BYTE_AS_TRUE);
                                break;

                            case CAP.TYPE_BYTE:
                                ((byte[])f_objectArray)[f_aiOffset] = getByte();
                                break;

                            case CAP.TYPE_SHORT:
                                ((short[])f_objectArray)[f_aiOffset] = getShort();
                                break;

                            case CAP.TYPE_INT:
                                if (CAP.INTEGER_MODE) {
                                    ((int[])f_objectArray)[f_aiOffset] =
                                            (int)((getShort() << 16) + (getShort()&0xFFFF));
                                } else {
                                    InstallerException.throwIt(
                                            Errors.INTEGER_UNSUPPORTED);
                                }
                                break;
                        } // end switch
                        f_aiOffset++;
                    } // non-zero count array

                    // at the end of the array initialization
                    if (f_aiOffset >= f_aiElementCount) {
                        // set the (downloaded) applet context ID
                        NativeMethods.setObjectContext(
                            (Object)f_objectArray,
                            (byte)(pkgContext << (byte)4));
                        // store the array object reference
                        NativeMethods.writeObjectAddress(f_statAddr, f_offset,
                                (Object)f_objectArray);

                        // advance the offset
                        f_offset += CAP.U2_SIZE;
                    }
                    break;

                case STATE_DEFAULT_COUNT:
                    // get default_value_count
                    short count = getShort();
                    /*
                     * Do this segment2 fixing-up work before segment3
                     */
                    for (short i=(short)0; i<f_seg2Count; i++) {
                        NativeMethods.writeShort(f_statAddr, f_offset,
                                CAP.NULL_REF);
                        f_offset += CAP.U2_SIZE;
                    }
                    // store (initialize to) zeros
                    for (short i=(short)0; i<count; i++) {
                        NativeMethods.writeByte(f_statAddr, f_offset++, (byte)0);
                    }
                    break;

                case STATE_ND_COUNT:

                    // get non_defalt_value_count
                    f_ndByteCount = getShort();
                    break;

                case STATE_ND_VALUES:
                    // store (initialize) one byte of non_default_values
                    if (f_ndOffset <= f_ndByteCount) {
                        NativeMethods.writeByte(f_statAddr, f_offset++, getByte());
                    }
                    f_ndOffset++;
                    break;

                case STATE_DONE:
                case STATE_NEXT:
                    noMore();
                    break;

                default:
                    break;

            } // switch
        } // while

        // more?
        if (hasMoreAPDU()) {
            return; // to get another APDU
        }

        //set the static referece count in the package table entry
        PackageMgr.g_newPackage.setStaticCount(g_staticReferenceCount);
        /*
        For garbage collection of pre-initialized arrays
        */
        f_objectArray = null;
        // done!
        setComplete(CAP.COMPONENT_STATICFIELD);
    }

    /**
     * return the next state based on the current state and data offset
     * @exception InstallerException
     */
    short getNextState() throws InstallerException {

        switch(f_currentState) {
            case STATE_SIZE:
                // next is IMAGE_SIZE u2
            case STATE_IMAGE_SIZE:
                // next is REF_COUNT u2
            case STATE_REF_COUNT:
                // next is AI_COUNT u2
            case STATE_AI_COUNT:
                // next is DEFAULT_COUNT u2 or AI_INFO_TYPE u1 (wait for 2)
            case STATE_DEFAULT_COUNT:
                // next is ND_COUNT u2
            case STATE_AI_INFO_TYPE:
                // next is AI_INFO_COUNT u2
                // a short is split in two APDUs?
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT; // do not update state
                }
                f_currentState++; // go to the next state
                if (f_currentState == STATE_AI_INFO_TYPE &&
                    f_aiCount <= (short)0) {
                        f_currentState = STATE_DEFAULT_COUNT;
                }
                break;

            case STATE_AI_INFO_COUNT:
                // next is AI_INFO_VALUES u1/2/4
            case STATE_AI_INFO_VALUES:
                // next is AI_INFO_VALUES u1/2/4
                // or DEFAULT_COUNT u2 (may wait for 4)
                // or AI_INFO_TYPE u1 (may wait for 4)

                // a short or int is split in two APDUs?
                if (isSplitValue(f_aiType == CAP.TYPE_INT ?
                                CAP.U4_SIZE : CAP.U2_SIZE)) {
                   saveSplitData();
                   return STATE_NEXT; // do not update state
                }

                if (f_currentState == STATE_AI_INFO_COUNT) {
                    f_currentState++; // go to the next state
                    break;
                }

                // current is STATE_AI_INFO_TYPE
                // more array_init_info.values?
                if (f_aiOffset < f_aiElementCount) {
                    break; // remain in the same state
                }

                // more array_init_info?
                if (f_aiCount > (short)0) {
                    f_currentState = STATE_AI_INFO_TYPE; // repeat AI
                } else {
                    f_currentState++; // go to the next state
                }

                break;

            case STATE_ND_VALUES:
                // next is ND_VALUES u1 or DONE u0
                // more non_default_values?
                if (f_ndOffset >= f_ndByteCount) {
                    f_currentState++; // go to the next state
                } // else remain in the same state
                break;

            case STATE_ND_COUNT:
                // next is ND_VALUES u1 or DONE u0
                if (f_ndByteCount == (short)0) {
                    f_currentState = STATE_DONE;
                    break;
                }
                // else fall through

            default:
                f_currentState++; // go to the next state
                break;
        }
        return f_currentState;
    }
}
