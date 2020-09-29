/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)MethodComponent.java	1.10
// Version:1.10
// Date:02/01/02
// Modified:02/01/02 11:15:50
//-

package com.sun.javacard.installer;

import javacard.framework.JCSystem;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.NativeMethods;

/**
 * This class implements method to link COMPONENT_Method
 */
class MethodComponent extends Component {

    /**
     * read and store
     * @exception InstallerException
     */
    void process() throws InstallerException {
        load();
    }

    /**
     * link COMPONENT_Method
     * @exception InstallerException
     */
    void postProcess() throws InstallerException {

        short m_runningAddr = g_componentAddresses[CAP.COMPONENT_METHOD];
        short m_handlerCount;

        //
        // link exception handlers
        //
        m_handlerCount = (short)(NativeMethods.readByte(m_runningAddr++, (short)0) & 0xff);
        for (short i=(short)0; i<m_handlerCount; i++) {

            //
            // link start offset
            //
            resolve(m_runningAddr,
                    CAP.OFFSET_EXCEPTION_HANDLER_START_OFFSET,
                    CAP.COMPONENT_METHOD);

            //
            // do nothing to active length
            //

            //
            // link handler offset
            //
            resolve(m_runningAddr,
                    CAP.OFFSET_EXCEPTION_HANDLER_HANDLER_OFFSET,
                    CAP.COMPONENT_METHOD);

            //
            // link catch type index
            //
            // -- This will be done when linking reference location component.
            //

            //
            // update the read offset pointer
            //
            m_runningAddr += CAP.EXCEPTION_HANDLER_INFO_SIZE;
        }

        if (m_handlerCount > (short)0) {
			      JCSystem.beginTransaction();
            //
            // allocate space to store the 4 byte exception table entry
            //
            PackageMgr.g_excTableEntry = NativeMethods.unhand(
                    NativeMethods.allocate(CAP.EXC_TABLE_ENTRY_SIZE));

            //
            // write the address to handler_count of the method component
            //
            NativeMethods.writeShort(PackageMgr.g_excTableEntry,
                    CAP.OFFSET_EXC_TABLE_ADDRESS,
                    g_componentAddresses[CAP.COMPONENT_METHOD]);

            NativeMethods.addExcTable(PackageMgr.g_excTableEntry, PackageMgr.g_newPackageIdentifier);
			      JCSystem.commitTransaction();
        }

        //
        // link method info
        // -- This will be done during reference location component linking.
        //

        //
        // Done!
        //
        setComplete(CAP.COMPONENT_METHOD);
    }

    /**
     * return nargs of the method
     */
    static byte getNArgs(short methodAddr) {
        if ((NativeMethods.readByte(methodAddr, (short)0) & CAP.ACC_EXTENDED) == CAP.ACC_EXTENDED)
            return (byte) (NativeMethods.readByte(methodAddr, CAP.OFFSET_EXTENDED_METHOD_NARGS));
        else
            // get rid of the lower 4 bit max_locals
            return (byte) ((NativeMethods.readByte(methodAddr, CAP.OFFSET_METHOD_NARGS) >> (byte)4) & 0x0F);
    }
}
