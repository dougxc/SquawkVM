/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.javacard.samples.odSample.packageB;

import javacard.framework.*;
import com.sun.javacard.impl.*;
import com.sun.javacard.samples.odSample.libPackageC.*;

/**
 * package AID - 0xA0 0x00 0x00 0x00 0x62 0x03 0x01 0x0C 0x07 0x02
 * applet AID - 0xA0 0x00 0x00 0x00 0x62 0x03 0x01 0x0C 0x07 0x02 0x01
 *
 * Applet used to demonstrate applet deletion and package deletion. It
 * also demonstrates dependencies by sharing references to objects and
 * shearable references across packages
 **/
public class B extends Applet implements Shareable{

	static BTreeNode sObj=null;
	short data;
	BTreeNode obj=null;
	
	/**
	 * method instantiates aninstance of B passing the arguments
	 **/
	public static void install(byte []bArr,short bOffset,byte bLength){
		new B(bArr,bOffset,bLength);
	}
  
	/**
	 * method returns pointer to this instance, ignores the param
	 **/
	public Shareable getShareableInterfaceObject(AID client_aid,
												 byte param){
		return this;
	}

  /**
   * Constructor. Makes 2nd instance have same ref to BTreeNode as
   * first instance. Also registers with eigher the default AID or
   * the one provided in parameters
   **/
  private B(byte[] bArray, short offset, byte length){
    data=C.DATA;
    if(sObj==null){
      obj=new BTreeNode();
      sObj=obj;
    }else{
      obj=sObj;
      sObj=null;
      JCSystem.requestObjectDeletion();
    }
    //register
    if(bArray[offset]==(short)0){
		this.register();
    }else{ 		
		this.register(bArray,(short)(offset+1),bArray[offset]);
    }
  } 

	/**
	 * method processes the APDU commands passed to this applet instance.
	 * It ignores any APDU's
	 **/
	public void process(APDU apdu) throws ISOException{}
	
}
  
  
