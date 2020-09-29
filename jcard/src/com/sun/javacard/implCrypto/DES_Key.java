/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)DES_Key.java	1.4
// Version:1.4
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/implCrypto/DES_Key.java 
// Modified:02/01/02 11:15:52
// Original author:  Andy
// */

package com.sun.javacard.implCrypto;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.*;

/**
 * DESKey contains an 8 byte key for single DES operations.
 *<p>
 * DES operates on a block size of 8 bytes.
 * @see DES3_Key
 * @see KeyBuilder
 */

public class DES_Key implements DESKey{

    byte[] keyData;

    public DES_Key( short keyLength, boolean transientKey ){

        // assert keyLength = 64 or keyLength = 128
        byte keyBytes = (byte) (keyLength/8);
        if ( transientKey ) keyData = JCSystem.makeTransientByteArray( keyBytes, JCSystem.CLEAR_ON_DESELECT );
        else
            keyData = new byte[keyBytes];
    }

    public void setKey( byte[] keyData, short kOff ){
        Util.arrayCopy( keyData, kOff, this.keyData, (byte)0, (byte) this.keyData.length );
        }

    public boolean isInitialized() {
        boolean initialized = false;
  	    for ( byte i=0; i < keyData.length; i++ )
  	    {
  	        if ( keyData[i]!=0 )
  	        {
  	            initialized = true;
  	            break;
  	        }
  	    }
  	    return initialized;
  	    }

  	public void clearKey() {
  	    Util.arrayFillNonAtomic( keyData, (short)0, (short) keyData.length, (byte)0);
  	    }

	public byte getType() { return KeyBuilder.TYPE_DES; }

	public short getSize() { return ( (short)(keyData.length*8) ); }

	public byte getKey( byte[] keyData, short kOff ) {
	    Util.arrayCopy( this.keyData, (short)0, keyData, kOff, (short)keyData.length );
	    return (byte) keyData.length;
	}

    }
