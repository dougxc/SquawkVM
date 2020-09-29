/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)Component.java	1.4
// Version:1.4
// Date:12/19/00
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/Component.java 
// Modified:12/19/00 12:22:53
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.Util;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.Errors;

/**
 * This class define globals and methods shared by all
 * component linkers during the installation
 */
class Component {

    // constants
    static final short MAX_SAVED_BYTES = (short)3;

    // globals
    static byte g_cls;               // APDU command class
    static byte[] g_buffer;          // APDU buffer
    static byte g_ins;               // APDU command instruction
    static byte g_p1;                // APDU P1
    static byte g_p2;                // APDU P2
    static byte g_lc;                // APDU LC
    static byte g_le;                // APDU LE
    static short g_dataSize;         // APDU data size (may differ from g_lc)
    static short g_respSize;         // APDU response size 
    static short g_dataOffset;       // APDU data offset (may differ from cdata)
    static byte g_currentState;
    static byte g_capMinor;
    static byte g_capMajor;
    static byte g_capFlags;
    static byte g_pkgMinor;
    static byte g_pkgMajor;
    static byte g_pkgAIDLength = (byte)-1;
    static short g_compSize;
    static boolean g_skipHeader = true;

    static byte[] g_pkgAID;
    static byte[] g_appletAID;
    static short[] g_requiredSize;
    static boolean[] g_loadComplete;
    static private InstallerException ex;

    static boolean g_bytesLeft;
    static short g_savedBytesCount;
    static byte[] g_savedBytes;

    static short g_offset; 
    static short g_max; 
    static boolean g_more; 
    static short g_componentSize;
    static short g_componentCount;
    static short g_classComponentLastOffset;

    static short[] g_componentOffsets;
    static short[] g_componentAddresses;
    static short g_staticReferenceCount;
    static boolean g_hasDownloadedApplet;
    
    static boolean staticInit = false;

    /** 
     * constructor
     */
    Component() {
    	// Static array initialization done in the constructor
    	if (staticInit == false) {
			g_pkgAID = new byte[CAP.MAX_AID_LENGTH];
			g_appletAID = new byte[CAP.MAX_AID_LENGTH];
			g_requiredSize = new short[CAP.INSTALLER_MAX];
			g_loadComplete = new boolean[CAP.INSTALLER_MAX];    	
			g_savedBytes = new byte[MAX_SAVED_BYTES];
			g_componentOffsets = new short[CAP.COMPONENT_MAX];
			g_componentAddresses = new short[CAP.COMPONENT_MAX];
			
			staticInit = true;
    	}
    	
        if (ex == null) {
            ex = new InstallerException((short)0);
        }
    }

    /**
     * initialize (after every APDU command "CAP BEGIN") 
     */
    static void resetLinker() {

        // reset linker states
        // reset globals
        g_p1 = (byte)0;
        g_dataSize = (short)0;
        g_dataOffset = (short)0;
        g_currentState = CAP.INSTALLER_STATE_READY;
        g_skipHeader = true;
        g_bytesLeft = false;
        g_savedBytesCount = (short)0;
        g_classComponentLastOffset = CAP.ILLEGAL_ADDRESS;
        g_staticReferenceCount = (short)0;
        g_hasDownloadedApplet = false;

        // reset global arrays

        byte i;
        
        for (i=(byte)0; i<(byte)CAP.INSTALLER_MAX; i++) {
            g_loadComplete[i] = false;
        }

        for (i=(byte)0; i<(byte)CAP.COMPONENT_MAX; i++) {
            g_componentOffsets[i] = (short)0;
        }
    }

    /**
     * throw an ISOException with an error code after
     * setting the current state to CAP.STATE_ERROR
     * @param err the error code
     */
    static void echoError(short err) {
        /*
        If this is CAP_END then we do not need to go in error state
                                    OR
        If the instruction was APPLET_INSTALL and no package was being processed
        at that time, then we only need to report an error and not go into error
        state because other instructions received by the installer will not be
        related to this one.
        */
        if((g_ins == CAP.INS_CAP_END) || ((g_ins == CAP.INS_APPLET_INSTALL) && 
            (PackageMgr.g_packageInProcess == PackageMgr.ILLEGAL_ID))){
                //reset the state as well, so that the subsequent commands do not fail.
                g_currentState = CAP.INSTALLER_STATE_READY;
        }else{
            g_currentState = CAP.INSTALLER_STATE_ERROR;
        }
        ISOException.throwIt(err);
    }

    /**
     * set load state to complete
     */
    static void setComplete(short tag) {
        g_loadComplete[tag] = true;
    }

    /**
     * set current installer state
     */
    static void setInstallerState(byte state) {
        g_currentState = state;
    }

    /**
     * append the leftover bytes to the APDU
     * use part of the first five APDU bytes as a buffer
     */
    static short getOffset() {
        short m_offset = ISO7816.OFFSET_CDATA;
        if (g_bytesLeft) {
            m_offset = restoreBytes(m_offset);
        }
        return m_offset;
    }

    /**
     * restore bytes leftover from the last APDU
     */
    static short restoreBytes(short offset) {
        g_bytesLeft = false;
        //
        // save the bytes left to the end of the APDU header
        //
        while (g_savedBytesCount > 0) {
            g_dataSize++;
            offset--;
            g_savedBytesCount--;
            g_buffer[offset] = g_savedBytes[g_savedBytesCount]; 

            /*
             * set it to the initial value
             * (necessary for the write overhead? -Joe)
             */
            g_savedBytes[g_savedBytesCount] = (byte)0xff;
        }
        return offset;
    }

    /**
     * save the split data to append to the next APDU
     */
    static void saveBytes(short offset, short count) {
        g_bytesLeft = true;
        for (short i = (short)0; i<count; i++) {
            g_savedBytes[g_savedBytesCount] = 
                g_buffer[offset];
            g_savedBytesCount++;
            offset++;
            g_dataSize--;
        }
    }

    /**
     * load data to internal memory
     */
    static void load() throws InstallerException { 
        if (g_skipHeader) {
            if (parseHeader() >= 0) {
                g_skipHeader = false;
            }
            // else return??
        }
        //
        // sequential write
        //
        NativeMethods.copyBytes(g_buffer,
                                g_dataOffset,
                                g_componentAddresses[g_p1],
                                g_componentOffsets[g_p1],
                                g_lc);

        //
        // update offset
        //
        g_componentOffsets[g_p1] += g_lc;
    }

    /**
     * default initializer
     */
    void init() throws InstallerException {
        g_compSize = (short)-1;
        g_skipHeader = true;
    }

    /**
     * no-op methods
     */
    void process() throws InstallerException { }
    void postProcess() throws InstallerException { }

    /**
     * process the 3 byte component header
     */ 
    static short parseHeader() throws InstallerException {
        resetSegmentProcessor();
        if ((short)(g_offset + CAP.COMP_HEADER_SIZE) > g_max) {
            saveBytes(g_offset, (short)(g_lc - g_offset));
            return (short)-1; // to get more data
        }
        g_lc -= CAP.COMP_HEADER_SIZE;
        g_dataOffset += CAP.COMP_HEADER_SIZE;
        getByte();
        g_compSize = getShort();
        return g_compSize;
    }

    /**
     * test if the highbit of token is 1
     * @param the byte value to be examined
     * @return true if token's highbit is 1
     */
    static boolean isHighbitOn(byte token) {
        return ((token & CAP.MASK_EXTERNAL) == CAP.MASK_EXTERNAL);
    }

    /** 
     * return true if external address
     * @param address the short value to be examined
     * @return true if address's highbit is 1
     */
    static boolean isExternal(short address) {
        return isHighbitOn((byte)(address >> (short)8));
    }

    /** 
     * convert a CAP file address to the real system address
     */
    static short calcAddress(byte tag, short address) throws InstallerException {
        if (isExternal(address)) {
            byte b1 = (byte)(address >> (short)8);
            b1 = ImportComponent.getPkgID((byte)(b1 & CAP.MASK_HIGH_BIT_OFF));
            short b2 = (short)(address & (short)0xff);
            return ExportComponent.getAddress(PackageMgr.getExportAddress(b1), b2,
                (byte)0, CAP.FLAG_CLASS);
        } else {
            return (short)(g_componentAddresses[tag] + address);
        }
    }

    /**
     * init variables before parsing the APDU data
     */
    static void resetSegmentProcessor() {
        g_offset = getOffset();
        g_max = (short)(g_offset + g_dataSize);
        g_more = true;
    }

    /**
     * return true if there are remaining data to be parsed
     */
    static boolean hasMoreData() {
        return (g_more && g_offset < g_max);
    }

    /**
     * return the next short in the APDU data after incrementing g_offset
     */
    static short getShort() {
        short s = Util.getShort(g_buffer, g_offset);
        g_offset += (short)2;
        return s;
    }

    /**
     * return the next byte in the APDU data after incrementing g_offset
     */
    static byte getByte() {
        byte b = g_buffer[g_offset];
        g_offset++;
        return b;
    }

    /**
     * set g_more to false
     */
    static void noMore() {
        g_more = false;
    }

    /**
     * return true if remaining byte(s) is the high byte(s) of a 
     * short or int value
     */
    static boolean isSplitValue(short valueSize) {
        return ((short)(g_offset + valueSize) > g_max);
    }

    /**
     * save the remaining bytes (to be appended to the next APDU data)
     */
    static void saveSplitData() {
        saveBytes(g_offset, (short)(g_max - g_offset));
    }

    /**
     * init variables before parsing/linking the component
     */
    static void resetComponentProcessor() {
        g_componentSize = CAP.COMP_HEADER_SIZE;
        g_componentCount = (short)0;
    }

    /**
     * store the component size
     */
    static void storeComponentSize() {
        g_componentSize += getShort();
    }

    /**
     * return true if the current component has more APDUs to come
     */
    static boolean hasMoreAPDU() {
        g_componentCount = (short)(g_componentCount + g_dataSize);
        return (g_componentCount < g_componentSize);
    }

    /**
     * resolve the reference and write the address
     */
    static short resolve(short address, short offset, byte tag)
                                                throws InstallerException {
        short m_s1 = NativeMethods.readShort(address, offset);
        short m_addr = calcAddress(tag, m_s1);
        NativeMethods.writeShort(address, offset, m_addr);
        return m_addr;
    }

    /**
     * allocate size of memory and save the address to a global array
     * @return handle as the address handle
     * @exception InstallerException
     */
    static short allocate(byte tag) throws InstallerException {
        if (g_requiredSize[tag] == (short)0) {
            return (short)CAP.ILLEGAL_ADDRESS; // skip
        }
        short handle = NativeMethods.allocate(g_requiredSize[tag]);
        if (handle == CAP.ILLEGAL_ADDRESS) {
            InstallerException.throwIt(Errors.ALLOCATE_FAILURE);
        }
        g_componentAddresses[tag] = NativeMethods.unhand(handle);
        return handle;
    }
}
