/*
 * Copyright � 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)NullApp.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/samples/com/sun/javacard/samples/NullApp/NullApp.java 
// Modified:02/01/02 11:16:14
// Original author:  Mitch Butler
// */

package com.sun.javacard.samples.NullApp;

import javacard.framework.*;

/**
 */

public class NullApp extends Applet
{
    /**
     * Only this class's install method should create the applet object.
     * @see APDU
     * @param apdu the incoming APDU containing the INSTALL command.
     */
    protected NullApp(APDU apdu)
    {
        register();
    }

    /**
     * Installs this applet.
     * @see APDU
     * @param apdu the incoming APDU containing the INSTALL command.
     * @exception ISOException with the response bytes per ISO 7816-4
     */
    public static void install( byte[] bArray, short bOffset, byte bLength )
    {
        new NullApp(null);
    }

    /**
     * Returns <0x6D,INS> response status always.
     * @see APDU
     * @param apdu the incoming APDU containing the INSTALL command.
     * @exception ISOException with the response bytes per ISO 7816-4
     */
    public void process(APDU apdu) throws ISOException
    {
        byte buffer[] = apdu.getBuffer();
        ISOException.throwIt(Util.makeShort((byte)(ISO7816.SW_INS_NOT_SUPPORTED>>8), buffer[1]));
    }

}
