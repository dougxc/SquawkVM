/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)CryptoNatives.java	1.4
// Version:1.4
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/implCrypto/CryptoNatives.java 
// Modified:02/01/02 11:15:52
// Original author: Ravi
/**
 * This static class (no object instances) contains all the crypto native methods
 * of the JCRE.
 * The JCWDE environment uses a variant of this class with non-native methods
 * but with identical signatures and JCWDE Java implementations.
 */

package com.sun.javacard.implCrypto;

public class CryptoNatives
{
  // This is a temporary set of methods to enable Export file generation of
  // javacard.security and javacardx.crypto packages.
  
  public static native void setIV(byte[] iv, short ivOff);

  public static native void DES3_EncryptCBC(byte[] keyData, byte[] input, short inputOff, short length,
	                                 byte[] output, short outputOff);
	                                 
  public static native void DES_EncryptCBC(byte[] keyData, byte[] input, short inputOff, short length,
	                                 byte[] output, short outputOff);
	
  public static native void DES_DecryptCBC(byte[] keyData, byte[] input, short inputOff, short length,
	                                byte[] output, short outputOff);

  public static native void DES_GenerateMac(byte[] keyData, byte[] input, short inputOff, short length,
                                     byte[] output, short outputOff);
    
}
