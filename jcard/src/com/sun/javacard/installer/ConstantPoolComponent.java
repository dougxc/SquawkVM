/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)ConstantPoolComponent.java	1.10
// Version:1.10
// Date:02/04/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/ConstantPoolComponent.java
// Modified:02/04/02 13:18:14
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.Errors;

/**
 * This class collects methods to link COMPONENT_ConstantPool
 */
class ConstantPoolComponent extends Component {

    /**
     * initialize
     */
    void init() throws InstallerException {
        super.init();
        //
        // special case:
        //        defer allocating constant pool memory until
        //        after the static field component is processed
        //
		JCSystem.beginTransaction();
        PackageMgr.g_tempMemoryAddress = allocate(CAP.COMPONENT_CONSTANTPOOL);
        PackageMgr.g_tempMemorySize = g_requiredSize[CAP.COMPONENT_CONSTANTPOOL];
		JCSystem.commitTransaction();
    }

    /**
     * read and store
     * @exception InstallerException
     */
    void process() throws InstallerException {
        load();
    }

    /**
     * link COMPONENT_CONSTANTPOOL
     * @exception InstallerException
     */
    void postProcess() throws InstallerException {

        short m_constAddr = g_componentAddresses[CAP.COMPONENT_CONSTANTPOOL];
        short count = NativeMethods.readShort(m_constAddr, (short)0);
        short m_offset = (short)2; // skip constant count
        short m_s1;

        for (short i = 0; i<count; i++) {
            //
            // constant pool cell size is 4 in a CAP file
            //
            m_s1 = linkOneConstant(
                NativeMethods.readByte(m_constAddr, m_offset),
                NativeMethods.readByte(m_constAddr, (short)(m_offset+(short)1)),
                NativeMethods.readByte(m_constAddr, (short)(m_offset+(short)2)),
                NativeMethods.readByte(m_constAddr, (short)(m_offset+(short)3)));

            //
            // m_s1 is the resolved value/address of a 4 byte
            // constant pool entry
            //
            NativeMethods.writeShort(m_constAddr, m_offset, m_s1);
            m_offset += CAP.CP_CELL_SIZE;
        }

        //
        // done!
        //
        setComplete(CAP.COMPONENT_CONSTANTPOOL);
    }

    /**
     * convert the 4 byte constant pool entry to a two byte value
     * @return short the linked value of a constant pool item
     * @exception InstallerException
     */
    static short linkOneConstant(byte tag, byte b1, byte b2, byte b3)
                                            throws InstallerException {
        short m_addr;
        short m_offset;
        byte m_b1;

        if (!isExternal(b1) && (tag == CAP.CONSTANT_STATICFIELDREF ||
                tag == CAP.CONSTANT_STATICMETHODREF)) {
            //
            // from offset to address
            //
            m_offset = Util.makeShort(b2, b3);
            m_b1 = (tag == CAP.CONSTANT_STATICFIELDREF) ?
                CAP.COMPONENT_STATICFIELD : CAP.COMPONENT_METHOD;
        } else {
            //
            // from class_ref to address for all the rest
            //
            m_offset = Util.makeShort(b1, b2);
            m_b1 = CAP.COMPONENT_CLASS;
        }

        //
        // m_b1 is the component tag
        //
        m_addr = calcAddress(m_b1, m_offset);

        switch (tag) {
            case CAP.CONSTANT_CLASSREF:
                break; // done!

            case CAP.CONSTANT_STATICFIELDREF:
            case CAP.CONSTANT_STATICMETHODREF:
                if (!isExternal(b1)) {
                    break; // done!
                }

                //
                // converter from package token to package ID
                //
                b1 = ImportComponent.getPkgID((byte)(b1 & CAP.MASK_HIGH_BIT_OFF));

                //
                // get address from the given package's export component
                //
                m_addr = ExportComponent.getAddress(PackageMgr.getExportAddress(b1),
                    (short)(b2&0xFF),(short)(b3&0xFF),
                    (tag == CAP.CONSTANT_STATICFIELDREF ? CAP.FLAG_FIELD :
                    CAP.FLAG_METHOD));
                break;

            case CAP.CONSTANT_INSTANCEFIELDREF:
                //
                // store the token to the low byte of a short value
                // token is the offset to a 16 bit cell
                //
                m_addr = Util.makeShort(
                        (byte)0, ClassComponent.calcInstanceSize(m_addr, b3));
                break;

            case CAP.CONSTANT_VIRTUALMETHODREF:

                m_addr = ClassComponent.getVirtualMethodAddress(m_addr, b3);

                //
                // store method nargs to the high byte
                // store method token to the low byte
                //
                m_addr = Util.makeShort(MethodComponent.getNArgs(m_addr), b3);
                break;

            case CAP.CONSTANT_SUPERMETHODREF:
                m_addr = ClassComponent.getVirtualMethodAddress(
                        NativeMethods.readShort(m_addr,
                        CAP.OFFSET_CLASS_SUPER_REF), b3);
                break;

        }
        return m_addr;
    }
}
