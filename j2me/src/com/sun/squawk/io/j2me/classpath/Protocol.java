/*
 *  Copyright (c) 1999 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.io.j2me.classpath;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.io.connections.*;
/**
 * Simple protocol to set connection variables
 *
 * @author  Nik Shaylor
 */
public class Protocol extends ConnectionBase implements ClasspathConnection {

    /**
     * The classpath array
     */
    private Vector classPathArray = new Vector();

    /**
     * Open the connection
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

//System.out.println("classpath: name="+name);

        if(name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" "+name);
        }

        String path = name.substring(2);

        String sepch = ";";

        String osName = System.getProperty("os.name");
        if (osName != null && !osName.startsWith("Windows")) {
            sepch = ":";
        }

        StringTokenizer st = new StringTokenizer(path, sepch);
        while (st.hasMoreTokens()) {
            String dirName = st.nextToken();
            if (dirName.endsWith("\\") || dirName.endsWith("/")) {
                dirName = dirName.substring(0, dirName.length() - 1);
            }
            classPathArray.addElement(dirName);
        }

        return this;
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream(String fileName) throws IOException {

        InputStream is = null;
        for (int i = 0  ; i < classPathArray.size() ; i++) {

           /*
            * Get the section of the classpath array
            */
            String classPathEntry = (String)classPathArray.elementAt(i);

//System.out.println("classPathEntry = "+classPathEntry);

            if (classPathEntry.endsWith(".zip") || classPathEntry.endsWith(".jar")) {

               /*
                * Open the file inside a zip file
                */
                try {
//System.out.println("trying "+"zip://"+classPathEntry+"~"+fileName);
                    is = Connector.openDataInputStream("zip://"+classPathEntry+"~"+fileName);
                    break;
                } catch (ConnectionNotFoundException ex) {
                }

            } else {

               /*
                * Open the file
                */
                try {
//System.out.println("trying "+"file://"+classPathEntry+"/"+fileName);
                    is = Connector.openDataInputStream("file://"+classPathEntry+"/"+fileName);
                    break;
                } catch (ConnectionNotFoundException ex) {
                }
            }
        }

        if (is == null) {
            throw new ConnectionNotFoundException(fileName);
        }

        return is;
    }


}
