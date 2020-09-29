/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)ExportComponent.java	1.8
// Version:1.8
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/ExportComponent.java 
// Modified:02/01/02 11:15:50
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.Errors;

/**
 * This class implements method to link COMPONENT_Export
 */
class ExportComponent extends Component {

    /** 
     * read and store
     * @exception InstallerException
     */ 
    void process() throws InstallerException {
        load();
    }

    /** 
     * link COMPONENT_EXPORT
     * @exception InstallerException
     */ 
    void postProcess() throws InstallerException {

        short m_runningAddr = g_componentAddresses[CAP.COMPONENT_EXPORT];
        short m_staticFieldCount;
        short m_staticMethodCount;
        short m_classCount;

        m_classCount = (short)(NativeMethods.readByte(m_runningAddr++, (short)0) & 0xff);

        for (short i=(short)0; i<m_classCount; i++) {
            // link class_offset
            resolve(m_runningAddr, (short)0, CAP.COMPONENT_CLASS);

            // read static_field_count, static_method_count
            m_staticFieldCount = (short)(NativeMethods.readByte(m_runningAddr,
                CAP.OFFSET_EXPORT_INFO_STATIC_FIELD_COUNT) & 0xff);
            m_staticMethodCount = (short)(NativeMethods.readByte(m_runningAddr,
                CAP.OFFSET_EXPORT_INFO_STATIC_METHOD_COUNT) & 0xff);

            // advance the data pointer
            m_runningAddr += CAP.OFFSET_EXPORT_INFO_STATIC_FIELD_OFFSETS;

            // link static_field_offsets
            for (short j=(short)0; j<m_staticFieldCount; j++) {
                resolve(m_runningAddr, (short)0, CAP.COMPONENT_STATICFIELD);

                // advance the pointer
                m_runningAddr += CAP.EXPORT_INFO_STATIC_FIELD_OFFSETS_SIZE;
            }

            // link static_method_offsets
            for (short j=(short)0; j<m_staticMethodCount; j++) {
                resolve(m_runningAddr, (short)0, CAP.COMPONENT_METHOD);

                // advance the pointer
                m_runningAddr += CAP.EXPORT_INFO_STATIC_METHOD_OFFSETS_SIZE;
            }
        }

        // Done!
        setComplete(CAP.COMPONENT_EXPORT);
    }

    /*
     * return the system address stored in the export table of 
     * given class and token 
     */
    static short getAddress(short exportAddr, short classToken, 
                        short token, byte mode) throws InstallerException {
        short m_fieldCount;
        short m_methodCount;
        short m_offset = CAP.OFFSET_EXPORT_COUNT;

        // get class_count
        short classCount = (short)(NativeMethods.readByte(exportAddr, m_offset) & 0xff);

        /**
         * This is necessary because a CAP file verified by the verifier
         * can still refer to unsupported/unpermitted external tokens
         */
        if (classToken >= classCount) {
            InstallerException.throwIt(Errors.IMPORT_CLASS_NOT_FOUND);
        }
        m_offset++;  // export count is 1

        // advance the offset
        for (short i=(short)0; i<(short)classToken; i++) {
            m_fieldCount = (short)(NativeMethods.readByte(exportAddr, (short)(m_offset +
                    CAP.OFFSET_EXPORT_INFO_STATIC_FIELD_COUNT))& 0xff);
            m_methodCount = (short)(NativeMethods.readByte(exportAddr, (short)(m_offset +
                CAP.OFFSET_EXPORT_INFO_STATIC_METHOD_COUNT)) & 0xff);
            short size = (short)(m_fieldCount + m_methodCount);
            size *= (short)2; // cell size = 2
            size += (short)4; // 2 for class address, 2 for counts
            m_offset += size;
        }

        m_fieldCount = (short)(NativeMethods.readByte(exportAddr, (short)(m_offset +
                CAP.OFFSET_EXPORT_INFO_STATIC_FIELD_COUNT)) & 0xff);
        m_methodCount = (short)(NativeMethods.readByte(exportAddr, (short)(m_offset +
            CAP.OFFSET_EXPORT_INFO_STATIC_METHOD_COUNT)) & 0xff);

        if (mode == CAP.FLAG_FIELD) {
                // m_offset will be where the static field address is
                m_offset += (short)((short)4 // skip class_offset & counts
                         + (short)(token * (byte)2)); // token is the index

        } else if (mode == CAP.FLAG_METHOD) {
                // m_offset will be where the static method address is
                m_offset += (short)((short)4 // skip class_offset & counts
                         + (short)(m_fieldCount*(byte)2) // skip field offsets
                         + (short)(token * (byte)2)); // token is the index
        }
        return NativeMethods.readShort(exportAddr, m_offset);
    }
}
