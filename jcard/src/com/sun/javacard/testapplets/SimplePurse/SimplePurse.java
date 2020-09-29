/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
// SimplePurse.java
// A simple purse applet written by Oscar A. Montemayor
// Features:  It stores digital money, and allows you to accumulate 
// frequent flyer miles as you download the data.

package com.sun.javacard.testapplets.SimplePurse;

import javacard.framework.*;
import com.sun.javacard.testapplets.SimpleAirMiles.*;

// Class definition
public class SimplePurse extends Applet {

	// Constants
	final static byte 	SIMPLEPURSE_CLA			= (byte)0x88;	//CLA for SimplePurse

	// SimplePurse protocol-defined INS codes
	final static byte	SP_ADD					= (byte)0x11;	// INS for ADD(Amnt)
	final static byte	SP_SPEND				= (byte)0x21;	// INS for SPEND(Amnt)
	final static byte	SP_QUERY				= (byte)0x31;	// INS for QUERY
	final static byte	SP_SETPIN				= (byte)0x32;	// INS for SETPIN(NewPin)
	final static byte	SP_AUTHUSER				= (byte)0x33;	// INS for AUTHUSER(UserPin)
	final static byte	SP_MILESQUERY			= (byte)0x41;	// INS for MILESQUERY
	final static byte   SP_KILLNEXT             = (byte)0x52;   // INS for killing in next transaction

	// SimplePurse protocol-defined SW codes
	final static short	SW_ADD_OVERFLOW			= (short)0x64FC;
	final static short	SW_SPEND_UNDERFLOW		= (short)0x64FE;
	final static short 	SW_UNAUTH_USER			= (short)0x64FA;

	private AID 	AIRMILES_AID;

	// Fields
	private short balance;
	private short[] transaction_history;
	private byte transaction_bucket;
	private com.sun.javacard.testapplets.SimpleAirMiles.AirMilesInterface milesAccount;
	private OwnerPIN thePIN;
	
	//test flag
	private byte killnext;

	// install method
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new SimplePurse(bArray, bOffset, bLength);
	}
	
	// constructor
	protected SimplePurse(byte[] bArray, short bOffset, byte bLength) {
	
		JCSystem.beginTransaction();
		balance = (short)0;
		killnext = (byte)0;
		
		byte[] aidSimpleAirMiles = new byte[] {(byte)0xa0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x62, (byte)0x3, (byte)0x1, (byte)0xc, (byte)0xB, (byte)0x1};
		AIRMILES_AID = new AID(aidSimpleAirMiles, (short)0, (byte)10);

		milesAccount = (com.sun.javacard.testapplets.SimpleAirMiles.AirMilesInterface)JCSystem.getAppletShareableInterfaceObject(AIRMILES_AID, (byte)0);

		transaction_history = new short[16];
		transaction_bucket = (byte)0;
		thePIN = new OwnerPIN((byte)3, (byte)2);
		JCSystem.commitTransaction();	

		if (bArray == null || bLength <= (byte)0) {
			register();
		} else {
			register(bArray, bOffset, bLength);
		}

	}

	public void deselect()
	{
		thePIN.reset();
	}

    public void bogus_method() {
        bogus_method();
    }
    
    public void KillNext(APDU apdu, byte[] buffer) {
        killnext = (byte)1;
    }

	private void recordTransaction(short amount) {
	
		transaction_history[transaction_bucket] = amount;
		transaction_bucket = (byte)((transaction_bucket + (byte)1) % (byte)16);

	}

	// SimplePurse operations
	public void AddMoney(APDU apdu, byte[] buffer) {
		short newMoney = Util.getShort(buffer, ISO7816.OFFSET_P1);
		byte killnow = killnext;
		
	  	if (thePIN.isValidated() == false) {
			ISOException.throwIt(SW_UNAUTH_USER);
		}

		killnext = (byte)0;
	
		if ((balance + newMoney) > (short)400) {
			// Return ADD_OVERFLOW APDU
			ISOException.throwIt(SW_ADD_OVERFLOW);
		}
		
		JCSystem.beginTransaction();
		balance += newMoney;
		
		if (killnow == (byte)1) {
		    bogus_method();
		}
		
		recordTransaction(newMoney);
		JCSystem.commitTransaction();
		
	}
	
	public void SpendMoney(APDU apdu, byte[] buffer) {
		short spentMoney = Util.getShort(buffer, ISO7816.OFFSET_P1);
		byte killnow = killnext;

		if (thePIN.isValidated() == false) {
			ISOException.throwIt(SW_UNAUTH_USER);
		}
		
		killnext = (byte)0;

		if ((balance - spentMoney) < (short)0) {
			// Return SPEND_UNDERFLOW APDU
			ISOException.throwIt(SW_SPEND_UNDERFLOW);
		}

		JCSystem.beginTransaction();
		balance -= spentMoney;
		milesAccount.addMiles(spentMoney);
		
		if (killnow == (byte)1) {
		    bogus_method();
		}

		recordTransaction((short)((short)(-1) * spentMoney));
		JCSystem.commitTransaction();

	}
	
	public void QueryBalance(APDU apdu, byte[] buffer) {
	    short tmpIndex;

		if (thePIN.isValidated() == false) {
			ISOException.throwIt(SW_UNAUTH_USER);
		}

		// Get balance and send it back
		tmpIndex = Util.setShort(buffer, (short)0, balance);
		apdu.setOutgoingAndSend((short)0, tmpIndex);
	}
	
	public void QueryMiles(APDU apdu, byte[] buffer) {
		short tmpIndex;
		short miles;

		if (thePIN.isValidated() == false) {
			ISOException.throwIt(SW_UNAUTH_USER);
		}
				
		// Get milesbalance and send it back
		miles = milesAccount.getMiles();
		tmpIndex = Util.setShort(buffer, (short)0, miles);
		apdu.setOutgoingAndSend((short)0, tmpIndex);	
	}

	public void SetPin(APDU apdu, byte[] buffer) {
				
		JCSystem.beginTransaction();
		thePIN.update(buffer, ISO7816.OFFSET_P1, (byte)2);
		thePIN.resetAndUnblock();
		JCSystem.commitTransaction();
	}
	
	public void AuthenticateUser(APDU apdu, byte[] buffer) {
	
		boolean result = thePIN.check(buffer, ISO7816.OFFSET_P1, (byte)2);	
	
		if (result == false) {
			ISOException.throwIt(SW_UNAUTH_USER);
		}
	}
		
	// process method
	public void process(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
		if (buffer[ISO7816.OFFSET_CLA] == SIMPLEPURSE_CLA) {
			byte Operation = buffer[ISO7816.OFFSET_INS];
			
			switch (Operation) {
				case SP_ADD:
					AddMoney(apdu, buffer);
					break;
				case SP_SPEND:
					SpendMoney(apdu, buffer);
					break;
				case SP_QUERY:
					QueryBalance(apdu, buffer);
					break;
				case SP_MILESQUERY:
					QueryMiles(apdu, buffer);
					break;
				case SP_KILLNEXT:
				    KillNext(apdu, buffer);
				    break;
				case SP_SETPIN:
					SetPin(apdu, buffer);
					break;
				case SP_AUTHUSER:
					AuthenticateUser(apdu, buffer);
					break;
				default:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);		
					break;
			}
			
		} else if (buffer[ISO7816.OFFSET_CLA] == (byte)0) {
			return;
		} else {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
	}
}
