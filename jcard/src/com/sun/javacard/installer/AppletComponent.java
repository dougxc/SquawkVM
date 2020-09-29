/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)AppletComponent.java	1.12
// Version:1.12
// Date:02/01/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/AppletComponent.java
// Modified:02/01/02 11:15:51
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.AID;
import javacard.framework.APDU;
import javacard.framework.JCSystem;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.AppletMgr;
import com.sun.javacard.impl.PrivAccess;
import com.sun.javacard.impl.Errors;

/**
* This class implements methods to link COMPONENT_Applet
*/
class AppletComponent extends Component {

    // process states
    static final short STATE_READY = (short)0;
    static final short STATE_TAG = (short)1;
    static final short STATE_SIZE = (short)2;
    static final short STATE_COUNT = (short)3;
    static final short STATE_LENGTH = (short)4;
    static final short STATE_AID = (short)5;
    static final short STATE_OFFSET = (short)6;
    static final short STATE_DONE = (short)7;
    static final short STATE_NEXT = (short)8;

    // class variables
    static short f_currentState;
    static byte f_aidCount;
    static byte f_aidLength;
    static byte f_minor;
    static byte f_major;
    static short f_aidOffset;
    static short f_appletAddr;

    /**
     * initialize
     * @exception InstallerException
     */
    void init() throws InstallerException {
        f_currentState = STATE_READY;
        f_appletAddr = g_componentAddresses[CAP.COMPONENT_APPLET];
        // flag to indicate the applet download
        g_hasDownloadedApplet = true;

        resetComponentProcessor();
    }

    /**
     * parse and validate COMPONENT_APPLET
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
                    //Get the applet count and set it in the
                    //package table's appropriate entry
                    f_aidCount = getByte();
					JCSystem.beginTransaction();
                    PackageMgr.g_newPackage.initializeAppletArray(f_aidCount);
                    JCSystem.commitTransaction();
                    break;

                case STATE_LENGTH:
                    f_aidLength = getByte();
                    f_aidOffset = (byte)0;
                    break;

                case STATE_AID:
                    g_appletAID[f_aidOffset++] = getByte();

                    if (f_aidOffset == f_aidLength) {
                        byte pkgId = (byte)PackageMgr.getAppletInfo(g_appletAID,
                                     (short)0, f_aidLength, PackageMgr.PACKAGE_ID);
                        // duplicate applet AID?
                        // AID for every applet class should be unique.
                        if ( pkgId != -1 && pkgId != PackageMgr.g_packageInProcess) {
                            InstallerException.throwIt(Errors.DUP_APPLET_AID);
                        }
                    }
                    break;

                case STATE_OFFSET:
                    // get install() method offset
                    short installOffset = getShort();

					JCSystem.beginTransaction();
                    // create an AID instance
                    AID aid = new AID(g_appletAID, (short)0, f_aidLength);

                    // set the aid to be a "JCRE entry point" object
                    NativeMethods.setJCREentry(aid, false);
                    // add the applet to this package's applet information

                    PackageMgr.g_newPackage.addAppletInfo(aid,
                            calcAddress(CAP.COMPONENT_METHOD, installOffset));

				    JCSystem.commitTransaction();

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

        // done!
        setComplete(CAP.COMPONENT_APPLET);
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
                // fall through

            case STATE_TAG:
                // a short is split in two APDUs
                if (isSplitValue(CAP.U2_SIZE)) {
                    saveSplitData();
                    return STATE_NEXT;
                }
                f_currentState++; // go to the next state
                break;

            case STATE_OFFSET:
                // more AID?
                f_aidCount--;
                if (f_aidCount > (byte)0) {
                    f_currentState = STATE_LENGTH;
                    break;
                }
                // deliberately fall through

            default:
                f_currentState++; // go to the next state
                break;
        }
        return f_currentState;
    }

    /**
     * invoke the install() method of the applet that matches the
     * given AID using the optional parameters
     *
     * assuming the AID and parameters fit in one APDU.
     *
     * APDU Command format:
     *      cls       0x80
     *      ins       0xb8
     *      p1        0x00
     *      p2        0x00
     *      lc        length
     *      byte      data[length]
     *      le        0x7f;
     *
     * data format:
     *      u1        AIDLength
     *      u1        AIDs[AIDlength]
     *      u1        pLength length
     *      u1        parameters[pLength]
     *
     */
    static void create(APDU apdu) throws InstallerException {
        AID aid = null;
        /*
         * set the installer state to indicate the installer
         * is in the process of creating an applet instance
         */
        setInstallerState(CAP.INSTALLER_STATE_CREATING);

        short m_aidOffset = g_dataOffset;
        byte m_aidLength = g_buffer[m_aidOffset++];

        //get the ID of the package that has the applet class.
        byte pkgID = (byte)PackageMgr.getAppletInfo(g_buffer, m_aidOffset,
                (byte)m_aidLength, PackageMgr.PACKAGE_ID);
        if(pkgID == -1){
            InstallerException.throwIt(Errors.APPLET_NOT_FOUND);
        }

		    AppletMgr.setCurrentAppletIndex(g_buffer, m_aidOffset, (byte)m_aidLength, pkgID);
		    
        /*
         * invoke the applet's install() method
         *
         * PackageMgr.commit() will be called within installerApplet()
         */
        try {
            byte pkgContext = PackageMgr.getPkgContext(pkgID);
            aid = AppletMgr.createApplet(g_buffer, m_aidOffset, m_aidLength, pkgContext);

        } finally {
            /*
             * set the applet selectable flag
             *
             * "The installation of an applet is deemed complete if
             * all steps are completed without failure or an exception
             * being thrown, up to and including successful return
             * from executing the Applet.register method.  At that
             * point, the installed applet will be selectable."
             *                                     --JCRE 2.1, 10.2.
             */
            if (aid != null && (PrivAccess.getAppState(aid) ==
                    PrivAccess.APP_STATE_REGISTERED)) {
                PrivAccess.setAppState( aid, PrivAccess.APP_STATE_SELECTABLE );
            } else {
                InstallerException.throwIt(Errors.APPLET_CREATION);
            }
        }

        // echo the instance AID of the newly created Applet
        short length = aid.getBytes(g_buffer, (short)0);
        apdu.setOutgoingAndSend((short)0, length);

        /*
         * set the installer state to indicate the installer
         * has completed creating the applet
         */
        setInstallerState(CAP.INSTALLER_STATE_READY);
        g_hasDownloadedApplet = false; // so to avoid unnecessary calling commit()
    }
}
