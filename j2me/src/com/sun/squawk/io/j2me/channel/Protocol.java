/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
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

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;

/**
 * Channel Connection
 */

public class Protocol extends ConnectionBase implements StreamConnection, StreamConnectionNotifier {

    /** Channel number */
    int chan = 0;

    /**
     * execChan
     */
    int execChan() throws IOException {
        return Native.execute(chan);
    }

    /**
     * Private constructor
     */
    private Protocol(int chan) {
        this.chan = chan;
    }

    /**
     * open
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        chan = Native.getChannel();
        Native.parm(Native.OP_OPEN);
        Native.parm(protocol);
        Native.parm(name);
        Native.parm(mode);
        Native.parm(timeouts?1:0);
        execChan();
        return this;
    }

    /**
     * openInputStream
     */
    public InputStream openInputStream() throws IOException {
        return new ChannelInputStream(this);
    }

    /**
     * openOutputStream
     */
    public OutputStream openOutputStream() throws IOException {
        return new ChannelOutputStream(this);
    }

    /**
     * acceptAndOpen
     */
    public StreamConnection acceptAndOpen() throws IOException {
        Native.parm(Native.OP_ACCEPT);
        int newChan = execChan();
        return new Protocol(newChan);
    }

    /**
     * Close the connection.
     */
    synchronized public void close() throws IOException {
        Native.parm(Native.OP_CLOSE);
        execChan();
    }

    /**
     * finalize
     */
    protected void finalize() {
        Native.freeChannel(chan);
        chan = -1;
    }
}
