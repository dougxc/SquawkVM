/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
// Workfile:@(#)DES_CBCCipher.java	1.4
// Version:1.4
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/implCrypto/DES_CBCCipher.java 
// Modified:02/01/02 11:15:52
// Original author:  Andy
// */

package com.sun.javacard.implCrypto;

import javacard.security.Key;
import javacardx.crypto.Cipher;

/**
 * The Cipher class is the base class for hashing algorithms.
 *
 */

public class DES_CBCCipher extends Cipher{

	DES_Key theKey;
	byte mode;

	private byte[] iv;

    private void setIV() {

      if ( iv == null )
        iv = new byte[8];
      CryptoNatives.setIV(iv, (short)0);
    }

	/**
	 * Initializes the Cipher object with the appropriate Key and algorithm specific
	 * parameters.
	 * @param theKey the key object to use for encryption/decryption.
	 * @param parameters [0..7] IV.
	 */
	public void init ( Key theKey, byte mode, byte[] bArray, short bOff, short bLen ){
	    if ( theKey instanceof DES_Key) this.theKey = (DES_Key) theKey;
        setIV();
//	    CryptoStubs.setIV( parameters, (short)0);
	    this.mode = mode;
	}

	/**
	 * Initializes the Cipher object with the appropriate Key and algorithm specific
	 * parameters.
	 * @param theKey the key object to use for encryption/decryption.
	 */
	public void init ( Key theKey, byte mode ){
	    if ( theKey instanceof DES_Key) this.theKey = (DES_Key) theKey;
        setIV();
//	    CryptoStubs.setIV( parameters, (short)0);
	    this.mode = mode;
	}

	/**
	 * Gets the Cipher algorithm.
	 * @return the algorithm code defined above.
	 */
	public byte getAlgorithm(){
	    return Cipher.ALG_DES_CBC_NOPAD;
	}

	/**
     * Generates encrypted/decrypted output from all/last input data.
	 * @param inBuff the input buffer of data to be encrypted/decrypted.
	 * @param inOffset the offset into the input buffer at which to begin encryption/decryption.
	 * @param inLength the length to be encrypted/decrypted.
	 * @param outBuff the output buffer, may be the same as the input buffer
	 * @param outOffset the offset into the output buffer where the resulting hash value begins
	 * @return number of bytes output in outBuff
	 */
    public short doFinal(
        byte[] inBuff,
        short inOffset,
        short inLength,
        byte[] outBuff,
        short outOffset)
        {
            short keyLen = theKey.getSize();
            switch ( mode ) {
                case Cipher.MODE_DECRYPT:
                    switch ( keyLen )
                    {
                        case 64:
                        setIV();
	                    CryptoNatives.DES_DecryptCBC(theKey.keyData, inBuff, inOffset, inLength, outBuff, outOffset);
                        break;
                        case 128:
                        // dummy
                        break;
                    }
                    return inLength;
                case Cipher.MODE_ENCRYPT:
                    switch ( keyLen )
                    {
                        case 64:
                        setIV();
	                    CryptoNatives.DES_EncryptCBC(theKey.keyData, inBuff, inOffset, inLength, outBuff, outOffset);
                        break;
                        case 128:
                        setIV();
	                    CryptoNatives.DES3_EncryptCBC(theKey.keyData, inBuff, inOffset, inLength, outBuff, outOffset);
                        break;
                    }
                    return inLength;
                default:
                    return 0;
            }
        }

	   /**
	   * Generates encrypted/decrypted output from input data.When this method is used temporary storage of
	   * intermediate results is required. It should be used only if doFinal() is insufficient.
	   * @param inBuff the input buffer of data to be encrypted/decrypted.
	   * @param inOffset the offset into the input buffer at which to begin encryption/decryption.
	   * @param inLength the length to be encrypted/decrypted.
	   * @param outBuff the output buffer, may be the same as the input buffer
	   * @param outOffset the offset into the output buffer where the resulting hash value begins
	   * @return number of bytes output in outBuff
	   */
    public short update(
        byte[] inBuff,
        short inOffset,
        short inLength,
        byte[] outBuff,
        short outOffset){return 0;}
}
