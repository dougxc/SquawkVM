/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
// Simple AirMiles control applet

package com.sun.javacard.testapplets.SimpleAirMiles;
import javacard.framework.*;

public class SimpleAirMiles 
	extends Applet implements AirMilesInterface {

	// Applet constants
	final static byte 	SIMPLEMILES_CLA			= (byte)0x87;	//CLA for SimpleMiles

	// Airmiles Operation codes
	final static byte	SM_MILESRESET			= (byte)0x31;	// INS for MILESRESET
	final static byte	SM_MILESQUERY			= (byte)0x41;	// INS for MILESQUERY
	final static byte	SM_MILESREDEEM			= (byte)0x51;	// INS for MILESREDEEM

	final static short	SW_MILES_UNDERFLOW		= (short)0x64FD;

	private AID	SIMPLEPURSE_AID;

	// Private fields
	private short miles;
	private short[] transaction_history;
	private byte transaction_bucket;
	

	// SIO interface call
	public Shareable getShareableInterfaceObject(AID client_aid, byte parameter) {
		
		// If request from SimplePurse - return interface
		if (client_aid != null) {
			if (SIMPLEPURSE_AID.equals(client_aid))
				return this;
			else
				return null;
		}
		return this;
	}

	// install method
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new SimpleAirMiles(bArray, bOffset, bLength);
	}
	
	// constructor
	protected SimpleAirMiles(byte[] bArray, short bOffset, byte bLength) {
	
		JCSystem.beginTransaction();
		miles	= (short)0;

		byte[] aidSimplePurse = new byte[] {(byte)0xa0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x62, (byte)0x3, (byte)0x1, (byte)0xc, (byte)0xA, (byte)0x1};
		SIMPLEPURSE_AID = new AID(aidSimplePurse, (short)0, (byte)10);

		transaction_history = new short[16];
		transaction_bucket = (byte)0;
		JCSystem.commitTransaction();
	
		if (bArray == null || bLength <= (byte)0) {
			register();
		} else {
			register(bArray, bOffset, bLength);
		}
	}

	private void recordTransaction(short amount) {
	
		transaction_history[transaction_bucket] = amount;
		transaction_bucket = (byte)((transaction_bucket + (byte)1) % (byte)16);

	}
	
	// SIO API calls
	public void addMiles(short amount) {
		// Add miles to the account
		miles	+= amount;
		recordTransaction(amount);
		
	}
	
	public short getMiles() {
		return miles;
	}
	
	// Administrative calls
	
	public void resetMiles(APDU apdu, byte[] buffer) {
	
		short numMiles = Util.getShort(buffer, ISO7816.OFFSET_P1);
			
		JCSystem.beginTransaction();
		miles = numMiles;
		transaction_bucket = (byte)0;		
		recordTransaction(numMiles);
		JCSystem.commitTransaction();
	
	}
	
	public void redeemMiles(APDU apdu, byte[] buffer) {
	
		short milesToRedeem = Util.getShort(buffer, ISO7816.OFFSET_P1);

		if ((miles - milesToRedeem) < (short)0) {
			// Return MILES_UNDERFLOW APDU
			ISOException.throwIt(SW_MILES_UNDERFLOW);
		}

		JCSystem.beginTransaction();
		miles -= milesToRedeem;
		recordTransaction((short)((short)(-1) * milesToRedeem));
		JCSystem.commitTransaction();

	}

	public void queryMiles(APDU apdu, byte[] buffer) {
	    short tmpIndex;
		// Get balance and send it back
		tmpIndex = Util.setShort(buffer, (short)0, miles);
		apdu.setOutgoingAndSend((short)0, tmpIndex);	
	}

	// process method
	public void process(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
		if (buffer[ISO7816.OFFSET_CLA] == SIMPLEMILES_CLA) {
			byte Operation = buffer[ISO7816.OFFSET_INS];
			
			switch (Operation) {
				case SM_MILESRESET:
					resetMiles(apdu, buffer);
					break;
				case SM_MILESQUERY:
					queryMiles(apdu, buffer);
					break;
				case SM_MILESREDEEM:
					redeemMiles(apdu, buffer);
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
