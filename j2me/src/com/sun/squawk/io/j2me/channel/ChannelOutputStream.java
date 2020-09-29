/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk.io.j2me.channel;

import java.io.*;

/**
 * ChannelOutputStream
 */
public class ChannelOutputStream extends DataOutputStream {

    private Protocol parent;
    long[] longBuf;

    public ChannelOutputStream(Protocol parent) throws IOException {
        super(null);
        this.parent = parent;
        Native.parm(Native.OP_OPENOUTPUT);
        parent.execChan();
    }

    public void flush() throws IOException {
        Native.parm(Native.OP_FLUSH);
        parent.execChan();
    }

    public void close() throws IOException {
        Native.parm(Native.OP_CLOSEOUTPUT);
        parent.execChan();
    }

    public void write(int b) throws IOException {
        Native.parm(Native.OP_WRITEBYTE);
        Native.parm(b);
        parent.execChan();
    }

    public void writeShort(int v) throws IOException {
        Native.parm(Native.OP_WRITESHORT);
        Native.parm(v);
        parent.execChan();
    }

    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        Native.parm(Native.OP_WRITEINT);
        Native.parm(v);
        parent.execChan();
    }

    public void writeLong(long v) throws IOException {
        if (longBuf == null) {
            longBuf = new long[1];
        }
        longBuf[0] = v;
        Native.parm(Native.OP_WRITELONG);
        Native.parm(longBuf);
        parent.execChan();
    }

    public void write(byte b[], int off, int len) throws IOException {
        Native.parm(Native.OP_WRITEBUF);
        Native.parm(b);
        Native.parm(off);
        Native.parm(len);
        parent.execChan();
    }

}


