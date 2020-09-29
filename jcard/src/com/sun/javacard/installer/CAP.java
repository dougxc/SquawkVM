/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)CAP.java	1.14
// Version:1.14
// Date:02/01/02
//
// Modified:02/01/02 11:15:50
//-

package com.sun.javacard.installer;

/**
 * This interface collects installer constants
 */
interface CAP {

    /*
     * on-card installer version
     */
     static final byte INSTALLER_MAJOR = 0;
     static final byte INSTALLER_MINOR = 7;

    /*
     * LC for CAP data command
     */
    static final byte APDU_DATA_SIZE = 32;

    /*
     * CLS for the installer
     */
    static final byte INSTALLER_CLA = (byte)0x80;

    // INS for the installer
    static final byte INS_CAP_BEGIN = (byte)0xb0;
    static final byte INS_CAP_END = (byte)0xba;
    static final byte INS_COMPONENT_BEGIN = (byte)0xb2;
    static final byte INS_COMPONENT_END = (byte)0xbc;
    static final byte INS_COMPONENT_DATA = (byte)0xb4;
    static final byte INS_APPLET_INSTALL = (byte)0xb8;
    static final byte INS_CAP_ABORT = (byte)0xbe;
    

    static final byte COMPONENT_HEADER = 1;
    static final byte COMPONENT_DIRECTORY = 2;
    static final byte COMPONENT_APPLET = 3;
    static final byte COMPONENT_IMPORT = 4;
    static final byte COMPONENT_CONSTANTPOOL = 5;
    static final byte COMPONENT_CLASS = 6;
    static final byte COMPONENT_METHOD = 7;
    static final byte COMPONENT_STATICFIELD = 8;
    static final byte COMPONENT_REFERENCELOCATION = 9;
    static final byte COMPONENT_EXPORT = 10;
    static final byte COMPONENT_DESCRIPTOR = 11;

    // Debug Component is never downloaded but it is part of the
    // Directory Component.
    //static final byte COMPONENT_DEBUG = 12;

    // number of (required) components + 1
    static final byte INSTALLER_MAX = 13;
    static final byte COMPONENT_MAX = 13;
    static final short ON_CARD_PKG_MAX = 16;

    // CAP file download order
    static final byte ORDER_HEADER = 1;
    static final byte ORDER_DIRECTORY = 2;
    static final byte ORDER_IMPORT = 3;
    static final byte ORDER_APPLET = 4;
    static final byte ORDER_CLASS = 5;
    static final byte ORDER_METHOD = 6;
    static final byte ORDER_STATICFIELD = 7;
    static final byte ORDER_EXPORT = 8;
    static final byte ORDER_CONSTANTPOOL = 9;
    static final byte ORDER_REFERENCELOCATION = 10;
    static final byte ORDER_DESCRIPTOR = 11;

    // misc.
    static final byte CAP_MAJOR = 2;
    static final byte CAP_MINOR = 2;

    static final byte MIN_AID_LENGTH = 5;
    static final byte MAX_AID_LENGTH = 16;
    static final short INSTANCE_MAX = 255;
    static final short PACKAGE_METHOD_MAX = 128;

    static final short BAD_ADDRESS = -1;
    static final short FFFF = (short)0xffff;
    static final short ILLEGAL_ADDRESS = 0;
    static final short NULL_REF = (short)0;
    static final byte ILLEGAL_ID = -1;
    static final byte ILLEGAL_INDEX = -1;
    static final byte ILLEGAL_TOKEN = -1;

    static final byte FLAG_CLASS = 1;
    static final byte FLAG_FIELD = 2;
    static final byte FLAG_METHOD = 3;

    static final byte CAP_MAGIC1 = (byte)0xDE;
    static final byte CAP_MAGIC2 = (byte)0xCA;
    static final byte CAP_MAGIC3 = (byte)0xFF;
    static final byte CAP_MAGIC4 = (byte)0xED;

    static final byte TYPE_BOOLEAN = 2;
    static final byte TYPE_BYTE = 3;
    static final byte TYPE_SHORT = 4;
    static final byte TYPE_INT = 5;

    static final short COMP_HEADER_SIZE = 3;
    static final short U1_SIZE = 1;
    static final short U2_SIZE = 2;
    static final short U4_SIZE = 4;

    static final short EXC_TABLE_ENTRY_SIZE = 5;
    static final short OFFSET_EXC_TABLE_ADDRESS = 2;
    static final boolean INTEGER_MODE = true;

    static final byte INSTALLER_STATE_ERROR = (byte)0xff;
    static final byte INSTALLER_STATE_READY = (byte)0x00;
    static final byte INSTALLER_STATE_LOADING = (byte)0x2;
    static final byte INSTALLER_STATE_CREATING = (byte)0x4;

    // CAP file masks
    static final byte ACC_INT = (byte)0x01;
    static final byte ACC_EXPORT = (byte)0x02;
    static final byte ACC_APPLET = (byte)0x04;
    static final byte ACC_INTERFACE = (byte)0x80;
    static final byte ACC_REMOTE = (byte)0x20;
    static final byte ACC_SHAREABLE = (byte)0x40;
    static final byte ACC_EXTENDED = (byte)0x80;
    static final byte ACC_ABSTRACT = (byte)0x40;
    static final byte ACC_PUBLIC = (byte)0x01;
    static final byte ACC_FINAL = (byte)0x10;

    static final byte MASK_EXTERNAL = (byte)0x80;
    static final byte MASK_HIGH_BIT_ON = (byte)0x80;
    static final byte MASK_IS_PKG_METHOD = (byte)0x80;
    static final byte MASK_HIGH_BIT_OFF = (byte)0x7F;
    static final byte MASK_INTERFACE_COUNT = (byte)0x0F;
    
    // header_component offsets
    static final short OFFSET_HEADER_MAGIC = 3;
    static final short OFFSET_HEADER_MAGIC1 = 3;
    static final short OFFSET_HEADER_MAGIC2 = 4;
    static final short OFFSET_HEADER_MAGIC3 = 5;
    static final short OFFSET_HEADER_MAGIC4 = 6;
    static final short OFFSET_HEADER_MINOR = 7;
    static final short OFFSET_HEADER_MAJOR = 8;
    static final short OFFSET_HEADER_FLAGS = 9;
    static final short OFFSET_HEADER_PKG = 10;

    // package_info offsets
    static final short OFFSET_PKG_MICRO = 0;
    static final short OFFSET_PKG_MINOR = 1;
    static final short OFFSET_PKG_MAJOR = 2;
    static final short OFFSET_PKG_AID_LEN = 3;
    static final short OFFSET_PKG_AID = 4;
    static final short OFFSET_PACKAGE_INFO_MICRO = 0;
    static final short OFFSET_PACKAGE_INFO_MINOR = 1;
    static final short OFFSET_PACKAGE_INFO_MAJOR = 2;
    static final short OFFSET_PACKAGE_INFO_AID_LENGTH = 3;
    static final short OFFSET_PACKAGE_INFO_AID = 4;

    // directory_component offsets
    static final short OFFSET_DIRECTORY_BASIC_COUNT = 3;
    static final short OFFSET_DIRECTORY_CUSTOM_COUNT = 4;
    static final short OFFSET_DIRECTORY_BASIC_COMPONENTS = 5;

    // basic_components
    static final short OFFSET_BASIC_COMPONENTS_TAG = 0;
    static final short OFFSET_BASIC_COMPONENTS_SIZE = 1;
    static final short BASIC_COMPONENTS_MEMBER_SIZE = 3;

    // custom_components
    static final short OFFSET_CUSTOM_COMPONENTS_TAG = 0;
    static final short OFFSET_CUSTOM_COMPONENTS_SIZE = 1;
    static final short OFFSET_CUSTOM_COMPONENTS_AID_LENGTH = 3;
    static final short OFFSET_CUSTOM_COMPONENTS_AID = 4;

    // applet_component offsets
    static final short OFFSET_APPLET_COUNT = 3;
    static final short OFFSET_APPLET_APPLETS = 4;
    static final short OFFSET_APPLETS_AID_LENGTH = 0;
    static final short OFFSET_APPLETS_AID = 1;

    // imports_component offsets
    static final short OFFSET_IMPORT_COUNT = 3;
    static final short OFFSET_IMPORT_PACKAGE_INFO = 4;

    // constant_pool_component offsets
    static final short OFFSET_CP_COUNT = 3;
    static final short OFFSET_CP_INFO = 5;
    static final short CAP_CP_CELL_SIZE = 4;
    //static final short CP_CELL_SIZE = 2;
    static final short CP_CELL_SIZE = 4;
    static final short TABLE_CELL_SIZE = 2;

    // class_component offsets
    static final short OFFSET_CLASS_INFO = 0;
    static final short OFFSET_CLASS_FLAGS = 0;
    static final short OFFSET_CLASS_SUPER_REF = 1;
    static final short OFFSET_CLASS_DEC_INST_SIZE = 3;
    static final short OFFSET_CLASS_REF_INDEX = 4;
    static final short OFFSET_CLASS_REF_COUNT = 5;
    static final short OFFSET_CLASS_PUB_METH_BASE = 6;
    static final short OFFSET_CLASS_PUB_METH_COUNT = 7;
    static final short OFFSET_CLASS_PKG_METH_BASE = 8;
    static final short OFFSET_CLASS_PKG_METH_COUNT = 9;
    static final short OFFSET_CLASS_PUB_METH_TABLE = 10;
    static final short OFFSET_CLASS_INTERFACE_INFO = 0;
    static final short OFFSET_CLASS_INTERFACE_INFO_CLASS_REF = 0;
    static final short OFFSET_CLASS_INTERFACE_INFO_COUNT = 2;
    static final short OFFSET_CLASS_INTERFACE_INFO_INDEX = 3;
    static final short OFFSET_REMOTE_METHOD_SIGNATURE = 2;
    static final short OFFSET_SIGNATURE_POOL_DATA = 2;

    static final short REMOTE_METHOD_INFO_SIZE = 5;
    static final short INTERFACE_INFO_SIZE = 1;

    // method_component offsets
    static final short OFFSET_METHOD_EXCEPTION_HANDLER_COUNT = 3;
    static final short OFFSET_METHOD_EXCEPTION_HANDLER_INFO = 4;
    static final short EXCEPTION_HANDLER_INFO_SIZE = 8;
    static final short OFFSET_EXCEPTION_HANDLER_START_OFFSET = 0;
    static final short OFFSET_EXCEPTION_HANDLER_ACTIVE_LENGTH = 2;
    static final short OFFSET_EXCEPTION_HANDLER_HANDLER_OFFSET = 4;
    static final short OFFSET_EXCEPTION_HANDLER_CATCH_TYPE_INDEX = 6;

    static final short OFFSET_METHOD_NARGS = 1;
    static final short OFFSET_EXTENDED_METHOD_NARGS = 2;

    // static_field_component offsets
    static final short OFFSET_STATICFIELD_BYTE_COUNT = 3;
    static final short OFFSET_STATICFIELD_REFERENCE_COUNT = 5;
    static final short OFFSET_STATICFIELD_ARRAY_INIT_COUNT = 7;
    static final short OFFSET_STATICFIELD_ARRAY_INIT_INFO = 9;

    // array_init_info offsets
    static final short OFFSET_ARRAY_INIT_INFO_TYPE = 0;
    static final short OFFSET_ARRAY_INIT_INFO_COUNT = 1;
    static final short OFFSET_ARRAY_INIT_INFO_VALUES = 3;

    // export_component offsets
    static final short OFFSET_EXPORT_CLASS_COUNT = 3;
    static final short OFFSET_EXPORT_CLASS_EXPORT_INFO = 4;
    static final short OFFSET_EXPORT_COUNT = 0;
    static final short OFFSET_EXPORT_CLASS_INFO = 1;
    static final short OFFSET_EXPORT_INFO_CLASS_OFFSET = 0;
    static final short OFFSET_EXPORT_INFO_STATIC_FIELD_COUNT = 2;
    static final short OFFSET_EXPORT_INFO_STATIC_METHOD_COUNT = 3;
    static final short OFFSET_EXPORT_INFO_STATIC_FIELD_OFFSETS = 4;
    static final short EXPORT_INFO_STATIC_FIELD_OFFSETS_SIZE = 2;
    static final short EXPORT_INFO_STATIC_METHOD_OFFSETS_SIZE = 2;
    static final short CLASS_EXPORT_INFO_CELL_SIZE = 6;

    static final short OFFSET_CONSTANT_TAG = 0;
    static final short OFFSET_CONSTANT_COUNT = 3;
    static final short OFFSET_ON_CARD_CONSTANT = 2;
    static final short OFFSET_CONSTANT_CLASSREF = 1;
    static final short OFFSET_CONSTANT_PKG_TOKEN = 1;
    static final short OFFSET_CONSTANT_CLASS_TOKEN = 2;
    static final short OFFSET_CONSTANT_OFFSET = 2;
    static final short OFFSET_CONSTANT_TOKEN = 3;
    static final short OFFSET_CONSTANT_FLD_TOKEN = 3;
    static final short OFFSET_CONSTANT_METH_TOKEN = 3;

    static final short CLASS_REF_SIZE = 2;

    // constant pool tags
    static final byte CONSTANT_CLASSREF = 1;
    static final byte CONSTANT_INSTANCEFIELDREF = 2;
    static final byte CONSTANT_VIRTUALMETHODREF = 3;
    static final byte CONSTANT_SUPERMETHODREF = 4;
    static final byte CONSTANT_STATICFIELDREF = 5;
    static final byte CONSTANT_STATICMETHODREF = 6;

    // reference_location_component offsets
    static final short OFFSET_REFLOC_BYTE_INDEX_COUNT = 3;
    static final short OFFSET_REFLOC_BYTE_INDICES = 5;
}
