/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)Installer.java	1.6
// Version:1.6
// Date:03/28/01
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/installer/Installer.java 
// Modified:03/28/01 11:37:32
// Original author: Joe Chen
//-

package com.sun.javacard.installer;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.APDU;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.AppletMgr;
import com.sun.javacard.impl.Errors;

/**
 * This class implements methods to install a CAP
 * file as a sequence of APDU commands.
 */
class Installer extends Component {

    /**
     * installer instance flag 
     */
    private static boolean installerRunning; 

    /**
     * an array of CAP file component linker objects
     */
    static Component[] f_linkers;

    /**
     * an array of CAP file component download order values
     */
    static byte[] f_downloadOrder = {
        	(byte)0, // no component tag
        	CAP.ORDER_HEADER, //ORDER_HEADER = 1;
        	CAP.ORDER_DIRECTORY, //ORDER_DIRECTORY = 2;
        	CAP.ORDER_APPLET, //ORDER_APPLET = 4;
        	CAP.ORDER_IMPORT, //ORDER_IMPORT = 3;
        	CAP.ORDER_CONSTANTPOOL, //ORDER_CONSTANTPOOL = 9;
        	CAP.ORDER_CLASS, //ORDER_CLASS = 5;
        	CAP.ORDER_METHOD, //ORDER_METHOD = 6;
        	CAP.ORDER_STATICFIELD, //ORDER_STATICFIELD = 7;
        	CAP.ORDER_REFERENCELOCATION,//ORDER_REFERENCELOCATION = 10;
        	CAP.ORDER_EXPORT,//ORDER_EXPORT = 8;
        	(byte)0, // skip descriptor component
    	};

    /**
     * constructor
     */
    protected Installer() {

        if (installerRunning) {
            // only one instance of the installer is allowed
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        installerRunning = true;

        /*
         * the motivation for the array of linkers is to avoid
         * a big switch statement for dispatching jobs
         */
        f_linkers = new Component[(short)(CAP.COMPONENT_EXPORT + (short)1)];
        f_linkers[CAP.COMPONENT_HEADER] = new HeaderComponent();
        f_linkers[CAP.COMPONENT_DIRECTORY] = new DirectoryComponent();
        f_linkers[CAP.COMPONENT_APPLET] = new AppletComponent();
        f_linkers[CAP.COMPONENT_IMPORT] = new ImportComponent();
        f_linkers[CAP.COMPONENT_CONSTANTPOOL] = new ConstantPoolComponent();
        f_linkers[CAP.COMPONENT_CLASS] = new ClassComponent();
        f_linkers[CAP.COMPONENT_METHOD] = new MethodComponent();
        f_linkers[CAP.COMPONENT_STATICFIELD] = new StaticFieldComponent();
        f_linkers[CAP.COMPONENT_REFERENCELOCATION] = new ReferenceLocationComponent();
        f_linkers[CAP.COMPONENT_EXPORT] = new ExportComponent();
    }

    /**
     * main entry to the on-card installer
     * @param apdu the current APPDU command
     */
    void install(APDU apdu) {

        try {
            // save the reference to the APDU buffer
            g_buffer = apdu.getBuffer();

            //save the instruction before doing anything else because
            //this is used for error handling if an exception is thrown.
            g_ins = g_buffer[ISO7816.OFFSET_INS];
            
            // verify APDU order
            verifyAPDUOrder(apdu); // may throw InstallerException
            
            // save the rest of the APDU header
            g_p1 = g_buffer[ISO7816.OFFSET_P1];
            g_p2 = g_buffer[ISO7816.OFFSET_P2];
            g_lc = g_buffer[ISO7816.OFFSET_LC];
            g_dataOffset = ISO7816.OFFSET_CDATA;

            // read the APDU data
            // (assuming apdu command data size <= apdu buffer size)
            if ((g_ins == CAP.INS_COMPONENT_DATA) ||
                    (g_ins == CAP.INS_APPLET_INSTALL)) {
                g_dataSize = apdu.setIncomingAndReceive();
            }

            // dispatching jobs
            switch (g_ins) {

                case CAP.INS_CAP_BEGIN:
                    resetLinker();  // initialize the linker state
                    PackageMgr.reset(); //reset the package manager and get the new package ID

                    break;

                case CAP.INS_COMPONENT_BEGIN:
                case CAP.INS_COMPONENT_DATA:
                case CAP.INS_COMPONENT_END:
                	
                	  /*we never received CAP Begin*/
                	  if(PackageMgr.g_packageInProcess == PackageMgr.ILLEGAL_ID){
                	      InstallerException.throwIt(Errors.COMMAND_ORDER);
                	  }
                    if ((g_p1 < CAP.COMPONENT_HEADER) ||
                            (g_p1 > CAP.COMPONENT_EXPORT)) {
                        InstallerException.throwIt(Errors.INSTRUCTION);
                    }

                    if (g_ins == CAP.INS_COMPONENT_BEGIN) {
                        f_linkers[g_p1].init();
                        break;
                    } else if (g_ins == CAP.INS_COMPONENT_DATA) {
                        f_linkers[g_p1].process();
                        break;
                    } else {
                        f_linkers[g_p1].postProcess();
                        break;
                    }
                case CAP.INS_CAP_END:
                    // recover space if allocated for the constant pool
                    if (PackageMgr.g_tempMemoryAddress != PackageMgr.ILLEGAL_ADDRESS) {
                        PackageMgr.freeTempMemory();
                    }

                    boolean doCommit = false;
                    if (JCSystem.getTransactionDepth() == 0) {
                        JCSystem.beginTransaction();
                        doCommit = true;
                    }

                    // Call native method to pass debug information.
                    // This call assumes that no commit errors will be encoutered
                    // before the call to PackageMgr.commit() is made.  Call only
                    // affects JREF simulator.
                    if (PackageMgr.g_newPackage != null) {
                    	NativeMethods.installDebugInfo(Component.g_pkgAIDLength, Component.g_pkgAID, Component.g_componentAddresses);                   
                    }
                    PackageMgr.commit();
                    if ( doCommit ) {
                        JCSystem.commitTransaction();
                    }
                    g_currentState = CAP.INSTALLER_STATE_READY;
                    break;
                case CAP.INS_APPLET_INSTALL:
                    AppletComponent.create(apdu);
                    break;

                case CAP.INS_CAP_ABORT:
                    PackageMgr.restore();
                    echoError(Errors.ABORTED);
                    break;

                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        } catch (ISOException e) {
            PackageMgr.restore();
            short reason = e.getReason();
            if (reason != ISO7816.SW_NO_ERROR) {
                echoError(reason);
            }
        } catch (InstallerException e) {
            PackageMgr.restore();
            echoError(e.getReason());
        } catch (Exception e) {
            PackageMgr.restore();
            echoError(Errors.EXCEPTION);
        }
    }

    /**
     * check if the APDU command is in the right order
     * @param apdu the APDU command for this component
     * @exception InstallerException
     */
    void verifyAPDUOrder(APDU apdu) throws InstallerException {

        byte m_ins = apdu.getBuffer()[ISO7816.OFFSET_INS];
        byte m_tag = apdu.getBuffer()[ISO7816.OFFSET_P1];

        if (m_ins == CAP.INS_CAP_BEGIN){
            //
            // INS_CAP_BEGIN always resets the state regardless
            //
            return;
        }

        // quit if already in an error state
        if (g_currentState == CAP.INSTALLER_STATE_ERROR) {
            if(m_ins == CAP.INS_CAP_END){
                //since the state of header and reference location component is used to check 
                //if a package is in progress right now, and we do not want to call resetLinker()
                //because it does a lot of other work that is only required for a new CAP file and 
                //not just to put the installer back into it's ready state, we just set the state
                //of both these components equal to false so that calls to createApplet don't fail.
                g_loadComplete[CAP.COMPONENT_HEADER] = g_loadComplete[CAP.COMPONENT_REFERENCELOCATION] = false;
            }
            InstallerException.throwIt(Errors.ERROR_STATE);
        }

        // for each APDU command
        switch (m_ins) {

            // check data order
            case CAP.INS_COMPONENT_BEGIN:
            case CAP.INS_COMPONENT_END:
            case CAP.INS_COMPONENT_DATA:
                // skip if within the same component
                if (m_tag == g_p1) { // g_p1 is the previous component tag
                    break;
                }

                // skip if first time
                if (g_p1 < CAP.COMPONENT_HEADER) {
                    break;
                }
                /*
                 * component arrives late?
                 * (m_tag = current tag, g_p1 = previous tag)
                 */
                if (f_downloadOrder[m_tag] < f_downloadOrder[g_p1]) {
                    InstallerException.throwIt(Errors.COMP_ORDER);
                }

                /*
                 * component arrives early (any gap)?
                 * (m_tag = current tag, g_p1 = previous tag)
                 *
                 * (!! update the following if download order changes!! )
                 */
                if (f_downloadOrder[m_tag] > (byte)(f_downloadOrder[g_p1]
                        + (byte)1)) {
                    /*
                     * applet component absent?
                     *
                     * class component immediately follows the applet component
                     */
                    if (m_tag == CAP.COMPONENT_CLASS) {
                        //if next expected component was applet component
                        if(f_downloadOrder[g_p1] + 1 == CAP.ORDER_APPLET) {
                            // OK if the flag indicates the absence of an applet
                            if ((g_capFlags & CAP.ACC_APPLET) == (byte)0x00) {
                                break;
                            }
                        }
                    }
                    /*
                     * export component absent?
                     *
                     * constantpool component immediately follows the 
                     * export component
                     */
                    else if (m_tag == CAP.COMPONENT_CONSTANTPOOL) {
                        //if next expected component was export component
                        if((byte)(f_downloadOrder[g_p1] + 1) == CAP.ORDER_EXPORT) {
                            // OK if the flag indicates the absence of export
                            if ((g_capFlags & CAP.ACC_EXPORT) == (byte)0x00) {
                                break;
                            }
                        }
                    } 
                    
                    //Illegal gap found
                    InstallerException.throwIt(Errors.COMP_ORDER);
                }

                /*
                 * previous component downloaded completely?
                 *
                 * g_p1 is the previous component
                 */
                if (!g_loadComplete[g_p1]){
                    InstallerException.throwIt(Errors.COMP_ORDER);
                }

                // OK!
                break;

            // check other command order
            case CAP.INS_APPLET_INSTALL:
            case CAP.INS_CAP_END:
                /*
                If the installer was not in ready state, or CAP file had not been downloaded completely
                before install, begin or end command came, we have to flag an error regarding command
                order. To check the download complete state, we can compare the download complete 
                states of both the header (first) and reference location (last) components. Equal states
                would mean that either installation never started, or it has completed successfully. Begin,
                end and install commands should only be allowed in these scenarios.
                */
                if ((g_currentState != CAP.INSTALLER_STATE_READY) ||
                    (g_loadComplete[CAP.COMPONENT_HEADER] != g_loadComplete[CAP.COMPONENT_REFERENCELOCATION])) {
                    InstallerException.throwIt(Errors.COMMAND_ORDER);
                }
                g_currentState = CAP.INSTALLER_STATE_READY;
                break;

            case CAP.INS_CAP_ABORT:
                break;

            default:
                InstallerException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
