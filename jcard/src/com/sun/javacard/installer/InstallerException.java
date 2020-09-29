/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)InstallerException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/InstallerException.java 
// Modified:02/01/02 11:15:51
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.CardException;

/**
 * This class implements a user defined exception
 */
class InstallerException extends CardException {

    private static InstallerException installerInstance;

    InstallerException(short reason) {
        super(reason);
        if (installerInstance==null)
            installerInstance = this; 
    }

    public static void throwIt(short reason) throws InstallerException{
        installerInstance.setReason(reason);
        throw installerInstance;
    }
}
