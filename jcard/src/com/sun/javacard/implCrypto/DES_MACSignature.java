/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)DES_MACSignature.java	1.4
// Version:1.4
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/implCrypto/DES_MACSignature.java 
// Modified:02/01/02 11:15:52
// Original author:  Andy
// */

package com.sun.javacard.implCrypto;

import javacard.framework.Util;
import javacard.security.Key;
import javacard.security.Signature;

/**
 * The Signature class is the base class for Signature algorithms.
 *
 */
public class DES_MACSignature extends Signature{

	private DES_Key theKey;
	private byte[] mySign;
	private byte myMode;

	private byte[] iv;;

    private void setIV() {

      if ( iv == null )
        iv = new byte[8];
      CryptoNatives.setIV(iv, (short)0);
    }

	/**
	 * Initializes the Signature object with the appropriate Key and algorithm specific
	 * parameters.
	 * theKey DES_Key to use for MAC generation
	 * @parameters parameter[0]=MAC length, parameters[1..8] is IV for DES CBC. Null => IV=0, MAC length=8.
	*/
	public void init ( Key theKey, byte mode, byte[] bArray, short bOff, short bLen ){
	    if ( theKey instanceof DES_Key) this.theKey = (DES_Key) theKey;
        //setIV( parameters, (short)1);
        setIV();
        myMode = mode;
	}

	/**
	 * Initializes the Signature object with the appropriate Key and algorithm specific
	 * parameters.
	 * theKey DES_Key to use for MAC generation
	*/
	public void init ( Key theKey, byte mode ){
	    if ( theKey instanceof DES_Key) this.theKey = (DES_Key) theKey;
        //setIV( parameters, (short)1);
        setIV();
        myMode = mode;
	}

	/**
	 * Gets the Message digest algorithm.
	 * @return the algorithm code defined above.
	*/
	public byte getAlgorithm() { return Signature.ALG_DES_MAC8_NOPAD; }

      /**
	   * returns the byte length of the hash.
	   * @return hash length
	   */
    public short getLength(){
        return 8;
    }

      /**
	   * accumulates a hash of the input data. If this method is used temporary storage of
	   * intermediate results is required. It should be used only if doFinal() cannot be used.
	   * @param inBuff the input buffer of data to be hashed
	   * @param inOffset the offset into the input buffer at which to begin hash generation
	   * @param inLength the length to hash
	   */
    public void update(
        byte[] inBuff,
        short inOffset,
        short inLength){}

    public short sign (
        byte[] inBuff,
        short inOffset,
        short inLength,
        byte[] signBuff,
        short signOffset){

        switch ( (short) theKey.getSize() ) {
            case 64:
                setIV();
                CryptoNatives.DES_GenerateMac(theKey.keyData, inBuff, inOffset, inLength, signBuff, signOffset);
                break;
            case 128:
                // dummy
                break;
        }
        return 8;
    }

    public boolean verify (
        byte[] inBuff,
        short inOffset,
        short inLength,
        byte[] signBuff,
        short signOffset,
        short sigLength){

        switch ( (short) theKey.getSize() ) {
            case 64:
                setIV();
                CryptoNatives.DES_GenerateMac(theKey.keyData, inBuff, inOffset, inLength, signBuff, signOffset);
                break;
            case 128:
                // dummy
                break;
        }
        if ( Util.arrayCompare( mySign, (short)0 , signBuff, signOffset, (short)4)==(byte)0) return true;
        else return false;
    }

}
