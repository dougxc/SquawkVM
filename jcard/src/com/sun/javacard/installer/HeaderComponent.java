/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)HeaderComponent.java	1.13
// Version:1.13
// Date:02/06/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/HeaderComponent.java 
// Modified:02/06/02 13:18:14
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.AID;
import com.sun.javacard.impl.PackageMgr;
import javacard.framework.JCSystem;
import com.sun.javacard.impl.PackageEntry;
import com.sun.javacard.impl.Errors;

/**
 * This class implements methods to parse COMPONENT_Header
 */
class HeaderComponent extends Component {

    // process states
    static final short STATE_READY = (short)0;
    static final short STATE_TAG = (short)1;
    static final short STATE_SIZE = (short)2;
    static final short STATE_MAGIC1 = (short)3;
    static final short STATE_MAGIC2= (short)4;
    static final short STATE_MAGIC3= (short)5;
    static final short STATE_MAGIC4= (short)6;
    static final short STATE_MINOR = (short)7;
    static final short STATE_MAJOR = (short)8;
    static final short STATE_FLAGS = (short)9;
    static final short STATE_PKG_MINOR = (short)10;
    static final short STATE_PKG_MAJOR = (short)11;
    static final short STATE_AID_LENGTH = (short)12;
    static final short STATE_AID_DATA = (short)13;
    static final short STATE_NAME_LENGTH = (short)14;
    static final short STATE_NAME = (short)15;
    static final short STATE_DONE = (short)16;
    static final short STATE_NEXT = (short)17;

    // class variables
    short f_currentState;
    byte f_aidOffset;
    byte f_nameOffset;
    byte[] f_pkgName = null;
    byte f_pkgNameLength = 0;
    

    /** 
     * initialize
     * @exception InstallerException
     */ 
    void init() throws InstallerException {

        /*
         * set the state to indicate the installer
         * is in the process of downloading a CAP file
         */
        setInstallerState(CAP.INSTALLER_STATE_LOADING);

        // initialize the component processor
        f_currentState = STATE_READY;
        f_aidOffset = (byte)0;
        f_nameOffset = (byte)0;
        resetComponentProcessor();
    }

    /** 
     * parse COMPONENT_Header
     * @exception InstallerException
     */ 
    void process() throws InstallerException {
        //check if we have reached on card package max.
        if(PackageMgr.g_newPackageIdentifier == PackageMgr.ILLEGAL_ID){
            InstallerException.throwIt(Errors.ON_CARD_PKG_MAX_EXCEEDED);
        }

        // reset for every APDU data segment
        resetSegmentProcessor();

        while (hasMoreData()) {
            switch (getNextState()) {
                case STATE_TAG:
                    getByte(); // skip
                    break;

                case STATE_SIZE:
                    storeComponentSize();
                    break;

                case STATE_MAGIC1:
                    if (getByte() != CAP.CAP_MAGIC1) {
                        InstallerException.throwIt(Errors.CAP_MAGIC);
                    }
                    break;

                case STATE_MAGIC2:
                    if (getByte() != CAP.CAP_MAGIC2) {
                        InstallerException.throwIt(Errors.CAP_MAGIC);
                    }
                    break;

                case STATE_MAGIC3:
                    if (getByte() != CAP.CAP_MAGIC3) {
                        InstallerException.throwIt(Errors.CAP_MAGIC);
                    }
                    break;

                case STATE_MAGIC4:
                    if (getByte() != CAP.CAP_MAGIC4) {
                        InstallerException.throwIt(Errors.CAP_MAGIC);
                    }
                    break;

                case STATE_MINOR:
                    g_capMinor = getByte();
                    if (g_capMinor > CAP.CAP_MINOR) {
                        InstallerException.throwIt(Errors.CAP_MINOR);
                    }
                    break;

                case STATE_MAJOR:
                    g_capMajor = getByte();
                    if (g_capMajor != CAP.CAP_MAJOR) {
                        InstallerException.throwIt(Errors.CAP_MAJOR);
                    }
                    break;

                case STATE_FLAGS:
                    g_capFlags = getByte();
                    if ((byte)(g_capFlags & CAP.ACC_INT) == CAP.ACC_INT){
                        if (!CAP.INTEGER_MODE) {
                            InstallerException.throwIt(
                                Errors.INTEGER_UNSUPPORTED);
                        }
                    }
                    //check if the package contains applets, that we
                    //have enough room for another applet package
                    if((byte)(g_capFlags & CAP.ACC_APPLET) == CAP.ACC_APPLET){
                        if(PackageMgr.appletPkgCount >= PackageMgr.ON_CARD_APPLET_PKG_MAX){
                            InstallerException.throwIt(
                                Errors.ON_CARD_APPLET_PKG_MAX_EXCEEDED);
                        }else{
                             //if the package is an applet package, we have to add
                             //it to the contexts table.
                             PackageMgr.addAppletPackage(PackageMgr.g_newPackageIdentifier);
                        }
                    }
                    break;

                case STATE_PKG_MINOR:
                    g_pkgMinor = getByte();
                    break;

                case STATE_PKG_MAJOR:
                    g_pkgMajor = getByte();
                    break;

                case STATE_AID_LENGTH:
                    g_pkgAIDLength = getByte();
                    break;

                case STATE_AID_DATA:
                    g_pkgAID[f_aidOffset++] = getByte();

                    // verify if the package already exists
                    if (f_aidOffset == g_pkgAIDLength) {
                        if (PackageMgr.findPkgID(g_pkgAID, (short)0, 
                                g_pkgAIDLength) != CAP.ILLEGAL_ID) {
                            InstallerException.throwIt(Errors.DUP_PKG_AID);
                        }
                    }
                    break;
                    
                case STATE_NAME_LENGTH:
                    f_pkgNameLength = getByte();
                    f_pkgName = new byte[f_pkgNameLength];
                    break;
                    
                case STATE_NAME:
                    if(f_nameOffset < f_pkgNameLength){
                        f_pkgName[f_nameOffset++] = getByte();
                    }else{
                        InstallerException.throwIt(Errors.PACKAGE_NAME_LENGTH_EXCEEDED);
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
            return; // to get another APDU (segment)
        }
        
        /*
         * create the PackageEntry instance since all information
         * needed to initialize the entry has been collected
         * rest of the entries will be set in there as we collect them
         * This process needs to be atomic
         */
        JCSystem.beginTransaction();
        PackageMgr.g_newPackage = new PackageEntry(
                new AID(g_pkgAID, (short)0, g_pkgAIDLength),
                g_pkgMajor, 
                g_pkgMinor,
                g_capFlags);
        if(g_capMinor >= 2){
            PackageMgr.g_newPackage.setPkgNameAndLength(f_pkgName, f_pkgNameLength);
        }
        JCSystem.commitTransaction();

        // done!
        setComplete(CAP.COMPONENT_HEADER);
    }

    /**
     * return the next state based on the data and the current state
     * @exception InstallerException
     */
    short getNextState() throws InstallerException {

        switch(f_currentState) {
            case STATE_AID_DATA:
                if((g_capMinor == 2) && (f_aidOffset == g_pkgAIDLength)){
                    //it's a 2.2 CAP file. We require a package name to be present 
                    f_currentState++; // go to the next state which is the name length
                }
                break;        // remain in the same state
            case STATE_NAME:
                break;        // remain in the same state 
            case STATE_TAG:
                // a short is split in two APDUs?
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT;
                }
                // fall through
            default:
                f_currentState++; // go to the next state
                break;
        }
        return f_currentState;
    } 
}
