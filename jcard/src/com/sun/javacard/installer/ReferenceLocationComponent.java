/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)ReferenceLocationComponent.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/ReferenceLocationComponent.java 
// Modified:02/01/02 11:15:51
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import com.sun.javacard.impl.NativeMethods;

/**
 * This class implements methods to link COMPONENT_ReferenceLocation
 */
class ReferenceLocationComponent extends Component {

    // link states
    static final short STATE_READY = (short)0;
    static final short STATE_TAG = (short)1;
    static final short STATE_SIZE = (short)2;
    static final short STATE_BYTE_INDEX_COUNT = (short)3;
    static final short STATE_BYTE_INDICES = (short)4;
    static final short STATE_BYTE2_INDEX_COUNT = (short)5;
    static final short STATE_BYTE2_INDICES = (short)6;
    static final short STATE_DONE = (short)7;
    static final short STATE_NEXT = (short)8;

    // instance variables
    short f_currentState;                // current state
    short f_N;                           // jumpOffset multiplier
    short f_jumpOffset;                  // cumulative offset
    short f_byteIndexCount;
    short f_byte2IndexCount;
    short f_methodAddr;                  // reference to method
    short f_constAddr;                   // reference to constantpool

    /** 
     * initializer 
     * @exception InstallerException
     */ 
    void init() throws InstallerException {

        f_currentState = STATE_READY;
        f_N = (short)0;
        f_methodAddr = g_componentAddresses[CAP.COMPONENT_METHOD];
        f_constAddr = g_componentAddresses[CAP.COMPONENT_CONSTANTPOOL];
        f_jumpOffset = (short)0;

        resetComponentProcessor();
    }

    /** 
     * link (fixup) COMPONENT_ReferenceLocation
     * @exception InstallerException
     */ 
    void process() throws InstallerException {
        
        resetSegmentProcessor();

        while (hasMoreData()) {
            switch (getNextState()) {

                case STATE_TAG:
                    getByte(); // skip for now
                    break;

                case STATE_SIZE:
                    storeComponentSize();
                    break;

                case STATE_BYTE_INDEX_COUNT:
                    f_byteIndexCount = getShort();
                    break;

                case STATE_BYTE2_INDEX_COUNT:
                    f_byte2IndexCount = getShort();
                        
                    // initialize f_jumpOffset to method block offset 
                    f_jumpOffset = (short)0;
                    break;

                case STATE_BYTE_INDICES:
                case STATE_BYTE2_INDICES:
                    short m_s1 = (short)getByte();
                    m_s1 &= (short)0xff;
                    if (m_s1 >= (short)0xff) {
                        f_N++;
                        break;
                    }
                    f_jumpOffset += (short)((short)(f_N * (short)255) + m_s1);
                    f_N = (short)0;

                    if (f_currentState == STATE_BYTE_INDICES) {
                        // read one byte as the constant pool index
                        m_s1 = (short)NativeMethods.readByte(f_methodAddr,
                                f_jumpOffset);
                        m_s1 &= (short)0xff;
                    } else {
                        // read one short as the constant pool index
                        m_s1 = NativeMethods.readShort(f_methodAddr,
                                f_jumpOffset); 
                    }

                    m_s1 *= (short)CAP.CP_CELL_SIZE;
                    m_s1 += CAP.U2_SIZE;

                    if (f_currentState == STATE_BYTE_INDICES) {
                        // byte index fixup
                        NativeMethods.writeByte(f_methodAddr, f_jumpOffset,
                            (byte)NativeMethods.readShort(f_constAddr, m_s1));
                    } else {
                        // byte2 index fixup
                        NativeMethods.writeShort(f_methodAddr, f_jumpOffset,
                            NativeMethods.readShort(f_constAddr, m_s1));
                    }
                    break;

                case STATE_NEXT:
                case STATE_DONE:
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

        // done!
        setComplete(CAP.COMPONENT_REFERENCELOCATION);
    }

    /**
     * return the next state based on the data and the current state
     * @exception InstallerException
     */
    short getNextState() throws InstallerException {

        switch(f_currentState) {
            case STATE_BYTE_INDEX_COUNT:
                // skip if count is zero
                if (f_byteIndexCount == (short)0) {
                    // a short that follows is split in two APDUs?
                    if (isSplitValue(CAP.U2_SIZE)) {
                        saveSplitData();
                        return STATE_NEXT; // do not update state
                    }
                    f_currentState++; // skip to the next state
                }
                f_currentState++; // go to the next state
                break;

            case STATE_BYTE2_INDEX_COUNT:
                f_currentState++; // go to the next state
                // skip if count is zero
                if (f_byte2IndexCount == (short)0) {
                    f_currentState++; // go to the next state
                }
                break;

            case STATE_BYTE2_INDICES:
                // special case: multiple items in offset_to_byte2_indices
                f_byte2IndexCount--;
                if (f_byte2IndexCount > (short)0) {
                    break; // to stay in the same state
                }
                f_currentState++; // go to the next state
                break;

            case STATE_BYTE_INDICES:
                // special case: multiple items in offset_to_byte_indices
                f_byteIndexCount--;
                if (f_byteIndexCount > (short)0) {
                    break; // to stay in the same state
                }
                // deliberately fall through

            case STATE_TAG:
            case STATE_SIZE:
                // special case: a short that follows is split in two APDUs
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT; // do not update state
                }

                // deliberate fall through
            default:
                f_currentState++; // go to the next state
                break;
        }
        return f_currentState;
    }

    /** 
     * perform any remaining tasks after component downloading
     * @exception InstallerException
     */ 
    void postProcess() throws InstallerException {
        /*
         * set the global state to indicate the installer
         * has reached the end of a cap file
         *
         * (!! update required if CAP file download order changes!!)
         */
        setInstallerState(CAP.INSTALLER_STATE_READY);
    }
}
