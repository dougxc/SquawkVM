/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)ClassComponent.java	1.11
// Version:1.11
// Date:02/07/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/ClassComponent.java 
// Modified:02/07/02 13:18:14
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.Errors;

/**
 * This class implements methods to install COMPONENT_Class
 */
class ClassComponent extends Component {

    // constants
    static final short MODE_PUB = (short)1;
    static final short MODE_PKG = (short)2;

    /**
     * read and store
     * @exception InstallerException
     */
    void process() throws InstallerException {
        load();
    }

    /** 
     * link COMPONENT_CLASS
     * @exception InstallerException
     */ 
    void postProcess() throws InstallerException {
        short m_offset= (short)0;
        short m_interfaceCount;
        byte m_b1;
        short m_s1;
        short m_superClassAddr;
        short m_curClassAddr;
        short m_count;
        short m_base;
        short m_classCompAddr = g_componentAddresses[CAP.COMPONENT_CLASS];

        if(g_capMinor == (byte)2){
            //2.2 class component may have signature pool in them.
            m_s1 = NativeMethods.readShort(m_classCompAddr, m_offset);
            m_offset += m_s1 + 2; //two byte added for size data
        }

        // f_size is the remembered class component size
        while (m_offset < g_compSize) {
            
            m_curClassAddr = (short)(m_classCompAddr + m_offset);
            
            // read the first byte of the class_info
            m_b1 = NativeMethods.readByte(m_curClassAddr, CAP.OFFSET_CLASS_INFO);

            // save interface_count for later use
            m_interfaceCount = (short)(m_b1 & CAP.MASK_INTERFACE_COUNT);

            // interface?
            if (isInterface(m_b1)) {
                m_offset++;
                for (short i=(short)0; i<m_interfaceCount; i++) {
                    // link superinterfaces
                    resolve(m_classCompAddr, m_offset, CAP.COMPONENT_CLASS);
                    m_offset += (short)2;
                }
                if(isRemote(m_b1)){
                    m_offset = fixupRemoteInfo(m_offset, m_b1, m_classCompAddr);
                }
                continue; // while
            }

            // link super class address
            m_superClassAddr = resolve(m_curClassAddr,
                    CAP.OFFSET_CLASS_SUPER_REF, CAP.COMPONENT_CLASS);
            
            /*
             * update the first reference index according to instance size
             * of the super class
             */
            m_s1 = (short)(getInstanceSize(m_superClassAddr) + (NativeMethods.readByte(m_curClassAddr,
                                                               CAP.OFFSET_CLASS_REF_INDEX) & 0xff));
            NativeMethods.writeByte(m_curClassAddr,
                CAP.OFFSET_CLASS_REF_INDEX, (byte)m_s1);     
                
            /*
             * link instance size
             * assuming internal super class is seen
             *
             * add super class's instance size to current class's declared
             * instance size
             */
            m_s1 = (short)(getInstanceSize(m_superClassAddr) +
                getInstanceSize(m_curClassAddr));
            
            // m_s1 is the calculated instance size
            if (m_s1 > CAP.INSTANCE_MAX) {
                InstallerException.throwIt(Errors.INSTANCE_MAX_EXCEEDED);
            }
            NativeMethods.writeByte(m_curClassAddr,
                CAP.OFFSET_CLASS_DEC_INST_SIZE, (byte)m_s1);
                
            // get public virtual method base
            m_base = (short) (NativeMethods.readByte(m_curClassAddr,
                CAP.OFFSET_CLASS_PUB_METH_BASE) & 0xff);

            // get public virtual method counts
            m_count = (short)(NativeMethods.readByte(m_curClassAddr,
                CAP.OFFSET_CLASS_PUB_METH_COUNT) & 0xff);
            
            m_offset += CAP.OFFSET_CLASS_PUB_METH_TABLE;

            // link public virtual method table
            for (short i = (short)0; i<m_count; i++) {
                m_s1 = NativeMethods.readShort(m_classCompAddr, m_offset);

                // special case: external method
                if (m_s1 == CAP.FFFF) {
                    /*
                     * copy the external virtual method reference
                     *
                     * i is both the index and the methodToken
                     */
                    m_s1 = getVirtualMethodAddress(m_superClassAddr,
                            (byte)(i + m_base)); 
                } else {
                    /*
                     * calculate the physical address
                     * m_s1 is then method offset
                     */
                    m_s1 = (short)(g_componentAddresses[CAP.COMPONENT_METHOD]
                        + m_s1);
                }

                // write it (back) to the public virtual method table
                NativeMethods.writeShort(m_classCompAddr, m_offset, m_s1);
                m_offset += (short)2;
            }

            // get package virtual method base
            m_base = (short) (NativeMethods.readByte(m_curClassAddr,
                CAP.OFFSET_CLASS_PKG_METH_BASE) & 0xff);

            // get package virtual method count
            m_count = (short)(NativeMethods.readByte(m_curClassAddr,
                CAP.OFFSET_CLASS_PKG_METH_COUNT) & 0xff);

            if ((short)(m_base + m_count) > CAP.PACKAGE_METHOD_MAX) {
                InstallerException.throwIt(Errors.PKG_METHOD_MAX_EXCEEDED);
            }

            //
            // link package virtual method table
            //
            for (short i = (short)0; i<m_count; i++) {
                resolve(m_classCompAddr, m_offset, CAP.COMPONENT_METHOD);
                m_offset += (short)2;
            }

            // link implemented interface info
            for (short i = (short)0; i<m_interfaceCount; i++) {
                short l_temp = 0;
                resolve(m_classCompAddr, (short)(m_offset +
                    CAP.OFFSET_CLASS_INTERFACE_INFO_CLASS_REF),
                    CAP.COMPONENT_CLASS);

                l_temp = (short)(NativeMethods.readByte(m_classCompAddr, (short)(m_offset 
                        + CAP.OFFSET_CLASS_INTERFACE_INFO_COUNT))& 0xff);
                
                // m_b1 is the index count
                m_offset += (short)((short)l_temp + (short)3); // + class_ref(2) + count(1)
            }
            
            /***************************************************************************************
            *                           CODE FOR REMOTE CLASSES
            *****************************************************************************************/
            //remote method information structure needs to be fixed up.
            if(isRemote(m_b1)){
                m_offset = fixupRemoteInfo(m_offset, m_b1, m_classCompAddr);
            }// Done one class!
        } // while

        // Done!
        setComplete(CAP.COMPONENT_CLASS);
    }
    
    /**
     * This method fixes up the data related to remote methods, classes, interface 
     * in the class component.
     * @param offset in the class component
     * @param class flag from which we can tell if the information is for interface or class
     * @param classCompAddr is the class component address
     * @return new value of offset.
     */
    static short fixupRemoteInfo(short offset, byte classFlag, short classCompAddr)throws InstallerException{
        if (isInterface(classFlag)){
            //if the interface is remote, it must have name data with it.
            //read the name length and advance the offset by name length size
            //and the one byte that is required to hold the name lenght
            short nameLength = (short)(NativeMethods.readByte(classCompAddr, (short)(offset))& 0xff);
            offset += (short)(nameLength+(byte)1);
            return offset;
        }
        else{
            //get remote method count
            short remoteMethodCount = (short)(NativeMethods.readByte(classCompAddr, (short)(offset))& 0xff);
            offset++;
            for(short i = (short)0; i < remoteMethodCount; i++){
                //get method signature offset in the signature pool
                short methodOffset = NativeMethods.readShort(classCompAddr, 
                                                            (short)(offset + CAP.OFFSET_REMOTE_METHOD_SIGNATURE));
                //fix up the method offset
                methodOffset += classCompAddr + CAP.OFFSET_SIGNATURE_POOL_DATA;
                //write it back where it was read from
                NativeMethods.writeShort(classCompAddr, 
                                        (short)(offset + CAP.OFFSET_REMOTE_METHOD_SIGNATURE), methodOffset);
                //advance the offset to the size of remote method information
                offset += CAP.REMOTE_METHOD_INFO_SIZE; 
            }
            //if the class is remote, it may have anti-hash collision string in it.
            //read that and add it's length and length data (one byte) to the offset
            short antiCollisionSize = NativeMethods.readByte(classCompAddr, (short)(offset));
            offset += (short)(antiCollisionSize + (byte)1);
            
            
                
            //Remote class structure also has name in it, we have to advance the offset by amount of
            //of bytes a name takes 
            short classNameSize = (short)(NativeMethods.readByte(classCompAddr, (short)(offset))& 0xff);
            //advance the offset by class name size and it's size byte
            offset += classNameSize + 1;
                
            //get the implemented remote interface count
            short remoteInterfaceCount = (short)(NativeMethods.readByte(classCompAddr, (short)(offset))& 0xff);
            offset++;
            for (short i = (short)0; i < remoteInterfaceCount; i++) {
                resolve(classCompAddr, (short)(offset +
                    CAP.OFFSET_CLASS_INTERFACE_INFO_CLASS_REF),
                    CAP.COMPONENT_CLASS);
                        
                offset += CAP.CLASS_REF_SIZE;
            }
            return offset;
        }
    }
    
    /** 
     * return the address to either a public or a package virtual method
     * @param classAddr the reference to a class component
     * @param offset the offset to a class
     * @param methodToken the virtual method token
     * @exception InstallerException
     */ 
    static short getMethodAddress(short classAddr, byte methodToken)
                                                throws InstallerException {

        short m_pubMethodCount = (short)(NativeMethods.readByte(classAddr,
            CAP.OFFSET_CLASS_PUB_METH_COUNT) & 0xff);
        short m_offset = CAP.OFFSET_CLASS_PUB_METH_TABLE;
        short methodType;

        // package method token has the highbit on
        if (isHighbitOn(methodToken)) {
            m_offset += (short)(m_pubMethodCount * CAP.U2_SIZE);
            methodToken &= CAP.MASK_HIGH_BIT_OFF;
            methodType = MODE_PKG; 
        } else {
            methodType = MODE_PUB; 
        }
        byte m_base = getMethodBase(classAddr, methodType);

        /*
         * "The value of an index into this table must be equal 
         *  to the value of the virtual/package method token of
         *  the indicated method, minus the value of the 
         *  public/package_method_table_base item."  -JCVM spec.
         */
        m_offset += (short)((short)(methodToken - m_base) * CAP.U2_SIZE);
        return NativeMethods.readShort(classAddr, m_offset);
    }

    /** 
     * get the given class's instance size
     * @param classAddr the address to a class
     * @return short the instance size
     * @exception InstallerException
     */ 
    static short getInstanceSize(short classAddr) throws InstallerException {
        short m_instanceSize = (short)(NativeMethods.readByte(classAddr,
            CAP.OFFSET_CLASS_DEC_INST_SIZE) & 0xff);
        return m_instanceSize;
    }

    /** 
     * get the given class's public or package virtual method table base
     * @param classAddr the address to a class
     * @param offset the offset to a class
     * @param mode the flag for either public or package method table 
     * @return byte the public or package virtual method table base
     * @exception InstallerException
     */ 
    static byte getMethodBase(short classAddr, short mode) {
        return NativeMethods.readByte(classAddr,
            (mode == MODE_PUB ? CAP.OFFSET_CLASS_PUB_METH_BASE :
            CAP.OFFSET_CLASS_PKG_METH_BASE));
    }

    /** 
     * return true if interface
     * @param bitfield the class flag
     */ 
    static boolean isInterface(byte bitfield) {
        return ((bitfield & CAP.ACC_INTERFACE) == CAP.ACC_INTERFACE);
    }
    
    /**
     * return true if interface or class is remote
     * @param bitfield the class flag
     */
    static boolean isRemote(byte bitfield){
        return ((bitfield & CAP.ACC_REMOTE) == CAP.ACC_REMOTE);
    }

    /**
     * return the calculated instance size, ref is a reference to the class
     * (Assuming the class component has been linked)
     */
    static byte calcInstanceSize(short classAddr, byte token) 
                                throws InstallerException {

        short superAddr = NativeMethods.readShort(classAddr,
            CAP.OFFSET_CLASS_SUPER_REF);
        short m_instanceSize = getInstanceSize(superAddr);
        m_instanceSize += (short)(((short)token) & 0xff);
        
        if (m_instanceSize > CAP.INSTANCE_MAX) {
            InstallerException.throwIt(Errors.INSTANCE_MAX_EXCEEDED);
        }
        
        return (byte)m_instanceSize;
    }
    
    /**
     * return virtual method address (recursive!)
     */
    static short getVirtualMethodAddress(short classAddr, byte methodToken)
                                                throws InstallerException { 
        boolean m_goSuper = false;

        /*
         * package method or public method?
         * (package method token has the highbit on)
         */
        if (isHighbitOn(methodToken)) {
            m_goSuper = (getMethodBase(classAddr, MODE_PKG) > 
                (byte)(methodToken & CAP.MASK_HIGH_BIT_OFF));
        } else {
            m_goSuper = 
                (getMethodBase(classAddr, MODE_PUB) > methodToken);
        }

        // method defined in the super class?
        if (m_goSuper) {
            short superAddress = NativeMethods.readShort(classAddr,
                    CAP.OFFSET_CLASS_SUPER_REF);
            return getVirtualMethodAddress(superAddress, methodToken);
        } else {
            return getMethodAddress(classAddr, methodToken);
        }
    }
}
