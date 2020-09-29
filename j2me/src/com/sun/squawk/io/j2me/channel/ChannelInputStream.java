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
 * ChannelInputStream
 */

public class ChannelInputStream extends DataInputStream {

    Protocol parent;
    long[] longBuf;

    public ChannelInputStream(Protocol parent) throws IOException {
        super(null);
        this.parent = parent;
        Native.parm(Native.OP_OPENINPUT);
        parent.execChan();
    }

    public void close() throws IOException {
        Native.parm(Native.OP_CLOSEINPUT);
        parent.execChan();
    }

    public int read() throws IOException {
        Native.parm(Native.OP_READBYTE);
        return parent.execChan();
    }

    public int readUnsignedShort() throws IOException {
        Native.parm(Native.OP_READSHORT);
        return parent.execChan();
    }

    public int readInt() throws IOException {
        Native.parm(Native.OP_READINT);
        return parent.execChan();
    }

    public long readLong() throws IOException {
        if (longBuf == null) {
            longBuf = new long[1];
        }
        Native.parm(Native.OP_READLONG);
        Native.parm(longBuf);
        parent.execChan();
        return longBuf[0];
    }

    public int read(byte b[], int off, int len) throws IOException {
        Native.parm(Native.OP_READBUF);
        Native.parm(b);
        Native.parm(off);
        Native.parm(len);
        return parent.execChan();
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        if (longBuf == null) {
            longBuf = new long[1];
        }
        longBuf[0] = n;
        Native.parm(Native.OP_SKIP);
        Native.parm(longBuf);
        parent.execChan();
        return longBuf[0];
    }

    public int available() throws IOException {
        Native.parm(Native.OP_AVAILABLE);
        return parent.execChan();
    }

    public void mark(int readlimit) {
        Native.parm(Native.OP_MARK);
        Native.parm(readlimit);
        try { parent.execChan(); } catch (IOException ex) {}
    }

    public void reset() throws IOException {
        Native.parm(Native.OP_RESET);
        parent.execChan();
    }

    public boolean markSupported() {
        Native.parm(Native.OP_MARKSUPPORTED);
        try { return parent.execChan() != 0; } catch (IOException ex) { return false; }

    }

}


