/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)ImportComponent.java	1.8
// Version:1.8
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/ImportComponent.java 
// Modified:02/01/02 11:15:51
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.Errors;

/**
 * This class implements methods to link COMPONENT_Import
 */
class ImportComponent extends Component {

    // process states
    static final short STATE_READY = (short)0;
    static final short STATE_TAG = (short)1;
    static final short STATE_SIZE = (short)2;
    static final short STATE_COUNT = (short)3;
    static final short STATE_MINOR = (short)4;
    static final short STATE_MAJOR = (short)5;
    static final short STATE_LENGTH = (short)6;
    static final short STATE_AID = (short)7;
    static final short STATE_DONE = (short)8;
    static final short STATE_NEXT = (short)9;

    // class variables
    static short f_currentState;
    static byte f_pkgCount;
    static byte f_aidLength;
    static byte f_minor;
    static byte f_major;
    static byte[] f_pkgIDs;
    static byte f_aidOffset;
    static short f_pkgIndex;
    static boolean staticInit = false;
    
    // COnstructor to avoid static call to clinit()
    
    ImportComponent() {
    	if (staticInit == false) {
    		f_pkgIDs = new byte[CAP.ON_CARD_PKG_MAX];
    		staticInit = true;
    	}
    }
    
    /** 
     * initializer
     * @exception InstallerException
     */ 
    void init() throws InstallerException {
        f_currentState = STATE_READY;
        f_pkgIndex = (short)0;
        Util.arrayFillNonAtomic(f_pkgIDs, (short)0, (short)f_pkgIDs.length,
                CAP.ILLEGAL_ID);

        resetComponentProcessor();
    }

    /** 
     * parse COMPONENT_IMPORT
     * @exception InstallerException
     */ 
    void process() throws InstallerException {

        resetSegmentProcessor();

        while (hasMoreData()) {
            switch (getNextState()) {
                case STATE_TAG:
                    getByte();
                    break;

                case STATE_SIZE:
                    storeComponentSize();
                    break;

                case STATE_COUNT:
                    f_pkgCount = getByte();
                    break;

                case STATE_MINOR:
                    f_minor = getByte();
                    break;

                case STATE_MAJOR:
                    f_major = getByte();
                    break;

                case STATE_LENGTH:
                    f_aidLength = getByte();
                    f_aidOffset = 0;
                    break;

                case STATE_AID:
                    g_appletAID[f_aidOffset] = getByte();
                    f_aidOffset++;
                    if (f_aidOffset == f_aidLength) {
                        byte m_b1 = PackageMgr.getPkgID(g_appletAID, (short)0,
                            f_aidLength, f_major, f_minor);
                        // m_b1 is the internal package identifier
                        if (m_b1 == CAP.ILLEGAL_ID) {
                            InstallerException.throwIt(
                                    Errors.IMPORT_NOT_FOUND);
                        }
                        f_pkgIDs[f_pkgIndex] = m_b1;
                        f_pkgIndex++;
                    }
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
        
        //set the import component information in the package table.
        //send the import package table, the offset in the table (0)
        //and the total count of items which is the current index in
        //the table. This has to be done atomically.
        
        JCSystem.beginTransaction();
        
        PackageMgr.g_newPackage.setImportInfo(f_pkgIDs, (byte)0, (byte)f_pkgIndex);
        
        JCSystem.commitTransaction();
        
        // done!
        setComplete(CAP.COMPONENT_IMPORT);
    }

    /**
     * return the next state based on the data and the current state
     * @exception InstallerException
     */
    static short getNextState() throws InstallerException {

        switch(f_currentState) {
            case STATE_AID:
                // more aid bytes?
                if (f_aidOffset < f_aidLength) {
                    break; // remain in the same state for more
                }
                f_pkgCount--;
                if (f_pkgCount > (byte)0) {
                        f_currentState = STATE_MINOR;
                } else {
                    f_currentState++;
                }
                break;

            case STATE_COUNT:
                if (f_pkgCount <= (byte)0) {
                    f_currentState = STATE_DONE;
                } else {
                    f_currentState++;
                }
                break;

            case STATE_TAG:
                // a short is split in two APDUs?
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT;
                }
                // deliberately fall through
            default:
                f_currentState++;
                break;
        }
        return f_currentState;
    } 

    /** 
     * return package identifier of the given package token, which is
     * the index into the import package table
     * @param pkgToken the package token
     * @return byte the package identifier
     */
    static byte getPkgID(byte pkgToken) throws InstallerException {
        return f_pkgIDs[pkgToken];
    }
}
