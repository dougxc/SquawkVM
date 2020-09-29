/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)InstallerApplet.java	1.11
// Version:1.11
// Date:03/12/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/InstallerApplet.java 
// Modified:03/12/02 19:02:34
// Original author: Joe Chen
//-
package com.sun.javacard.installer;

import javacard.framework.Applet;
import javacard.framework.APDU;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.AppletMgr;
import com.sun.javacard.impl.PrivAccess;


/**
 * This class implements the on-card installer applet
 */
public class InstallerApplet extends Applet 
{
    private static Installer myInstaller;

    /**
     * Only this class's install method should create the applet object.
     */
    protected InstallerApplet() {
        register();
    }

    /**
     * restore if necessary before this applet is deselected
     */
    public void deselect() {
        PackageMgr.restore();
    }

    /**
     * restore package manager to the initial state
     */
    public boolean select() {
        PackageMgr.restore();
        Component.resetLinker();//reset the installer completely, all globals and everything.
        return true;
    }

    /**
     * Installs this applet.
     * @param bArray the array containing installation parameters
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        // create the installer applet instance
        new InstallerApplet();

        // create the installer instance
        myInstaller = new Installer(); // throw exception if failed
    }

    /**
     * Processes an incoming APDU.
     * @see APDU
     * @param apdu the incoming APDU
     * @exception ISOException with the response bytes per ISO 7816-4
     */
    public void process(APDU apdu) {
        // installer command?
        if ((byte)(apdu.getBuffer()[ISO7816.OFFSET_CLA] & (byte)(0xFC)) 
            == CAP.INSTALLER_CLA) { 
            switch(apdu.getBuffer()[ISO7816.OFFSET_INS]){
                case PackageMgr.DELETE_PACKAGE:
                case PackageMgr.DELETE_PACKAGE_AND_APPLETS:
                    PackageMgr.handlePackageDeletion(apdu);
                    break;
                case AppletMgr.DELETE_APPLETS:
                    AppletMgr.handleAppletDeletion(apdu);
                    break;
                default: 
                    PrivAccess.resetProcessMethodFlag();
                    myInstaller.install(apdu);
                    PrivAccess.setProcessMethodFlag();
            }
        }
        else if (!selectingApplet()) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
