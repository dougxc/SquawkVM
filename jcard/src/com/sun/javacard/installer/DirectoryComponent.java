/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)DirectoryComponent.java	1.8
// Version:1.8
// Date:02/01/02
// 
// Modified:02/01/02 11:15:51
//-

package com.sun.javacard.installer;

import com.sun.javacard.impl.PackageMgr;
import javacard.framework.JCSystem;
/**
 * This class implements methods to parse COMPONENT_Directory
 */
class DirectoryComponent extends Component {

    // link states
    static final short STATE_READY = (short)0;
    static final short STATE_TAG = (short)1;
    static final short STATE_SIZE = (short)2;
    static final short STATE_COMP_SIZES = (short)3;
    static final short STATE_IMAGE_SIZE = (short)4;
    static final short STATE_INIT_COUNT = (short)5;
    static final short STATE_INIT_SIZE = (short)6;
    static final short STATE_IMPORT_COUNT = (short)7;
    static final short STATE_APPLET_COUNT = (short)8;
    static final short STATE_CUSTOM_COUNT = (short)9;
    static final short STATE_THE_REST = (short)10;
    static final short STATE_DONE = (short)11;
    static final short STATE_NEXT = (short)12;

    // class variables
    static short f_currentState;
    static byte f_tag;

    /** 
     * initialize the COMPONENT_Directory processor
     * @exception InstallerException
     */ 
    void init() throws InstallerException {
        f_currentState = STATE_READY;
        f_tag = (byte)1; // starts from 1
        resetComponentProcessor();
    }

    /** 
     * parse COMPONENT_Directory
     * @exception InstallerException
     */ 
    void process() throws InstallerException {

        resetSegmentProcessor();

        while (hasMoreData()) {
            switch (getNextState()) {
                case STATE_TAG:
                case STATE_IMPORT_COUNT:
                case STATE_APPLET_COUNT:
                case STATE_CUSTOM_COUNT:
                case STATE_THE_REST:
                case STATE_INIT_COUNT:
                case STATE_INIT_SIZE:
                    getByte(); // skip
                    break;

                case STATE_SIZE:
                    storeComponentSize();
                    break;

                case STATE_COMP_SIZES:
                    // save component size
                    g_requiredSize[f_tag++] = getShort();
                    break;

                case STATE_IMAGE_SIZE:
                    // static image size arrives after component sizes
                    g_requiredSize[CAP.COMPONENT_STATICFIELD] = getShort();
                    break;

                case STATE_NEXT:
                case STATE_DONE:
                    noMore();
                    break;

                default:
                    break;
            }
        } // while

        // more?
        if(hasMoreAPDU()) {
            return; // for more
        }

        //
        // allocate memory to store CAP file components
        // and set the sizes and address of each component
        // in the package entry in the package table. This operation
        // has to be atomic
        //
        JCSystem.beginTransaction();
        allocate(CAP.COMPONENT_CLASS);
        PackageMgr.g_newPackage.setComponentInfo(g_requiredSize[CAP.COMPONENT_CLASS],
                                                 g_componentAddresses[CAP.COMPONENT_CLASS],
                                                 PackageMgr.CLASS_COMPONENT_INDEX);
        allocate(CAP.COMPONENT_METHOD);
        PackageMgr.g_newPackage.setComponentInfo(g_requiredSize[CAP.COMPONENT_METHOD],
                                                 g_componentAddresses[CAP.COMPONENT_METHOD],
                                                 PackageMgr.METHOD_COMPONENT_INDEX);
        
        allocate(CAP.COMPONENT_STATICFIELD);
        PackageMgr.g_newPackage.setComponentInfo(g_requiredSize[CAP.COMPONENT_STATICFIELD],
                                                 g_componentAddresses[CAP.COMPONENT_STATICFIELD],
                                                 PackageMgr.STATIC_FIELD_COMPONENT_INDEX);
        
        allocate(CAP.COMPONENT_EXPORT);
        PackageMgr.g_newPackage.setComponentInfo(g_requiredSize[CAP.COMPONENT_EXPORT],
                                                 g_componentAddresses[CAP.COMPONENT_EXPORT],
                                                 PackageMgr.EXPORT_COMPONENT_INDEX);
       JCSystem.commitTransaction();

        /*
         * special case:
         *   Save the offset to the last byte of the class
         *   component for later use to determine if a class
         *   address is external or internal
         *
         *   This is used to determine if a "resolved" 
         *   reference is "external" or not.
         *
         */
        g_classComponentLastOffset = (short)
                (g_componentAddresses[CAP.COMPONENT_CLASS] +
                g_requiredSize[CAP.COMPONENT_CLASS] - (short)1); 

        // done!
        setComplete(CAP.COMPONENT_DIRECTORY);
    }

    /** 
     * return the next state based on the data and the current state
     * @exception InstallerException
     */ 
    static short getNextState() throws InstallerException {
        byte max_components = CAP.COMPONENT_MAX;

        if ((g_capMajor < 2) || ((g_capMajor == 2) && (g_capMinor < 2))) {
            max_components--;
        }

        switch(f_currentState) {

            case STATE_TAG:
            case STATE_SIZE:
            case STATE_IMAGE_SIZE:
            case STATE_INIT_COUNT:
            case STATE_COMP_SIZES:
                //
                // a short is split in two APDUs?
                //
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT;
                }

                //
                // more components?
                //
                if (f_currentState == STATE_COMP_SIZES && f_tag < max_components) {
                    break; // remain in the same state
                }
                f_currentState++; // go to the next state
                break;

            case STATE_READY:
            case STATE_IMPORT_COUNT:
            case STATE_APPLET_COUNT:
            case STATE_CUSTOM_COUNT:
            case STATE_INIT_SIZE:
            case STATE_THE_REST:
                f_currentState++; // go to the next state
                break;

            case STATE_DONE:
            default:
                break;

        }
        return f_currentState;
    } 
}
