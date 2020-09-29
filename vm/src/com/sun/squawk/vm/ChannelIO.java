package com.sun.squawk.vm;
import com.sun.squawk.util.*;
import java.util.*;

public class ChannelIO implements SquawkOpcodes {

    Memory  mem;
    boolean debug;
    IntHashtable channels = new IntHashtable();
    int nextChan = 1; // Channel 0 is reserved

    /*
     * Constructor
     */
    public ChannelIO(Memory mem, boolean debug) {
        this.mem   = mem;
        this.debug = debug;
    }

    /*
     * Create channel
     */
    Channel createChannel() {
        while (channels.get(nextChan) != null) {
            nextChan++;
        }
        int chan = nextChan++;
        Channel res = new Channel(mem, chan, debug);
        channels.put(chan, res);
        return res;
    }


    /*
     * execute
     */
    int execute(int chan, int nativeParms[], int nparms) {
        int op = nativeParms[0];
        Channel c = (op == OP_GETCHANNEL) ? createChannel() : (Channel)channels.get(chan);
        if (c == null) {
            return 0;
        }
        int res = c.execute(nativeParms, nparms, this);

        if (op == OP_FREECHANNEL) {
            channels.remove(chan);
        }

        return (op == OP_GETCHANNEL ? c.index : res);
    }

    /*
     * error
     */
    int error(int chan) {
        Channel c = (Channel)channels.get(chan);
        if (c == null) {
            return EXNO_NoConnection;
        }
        return c.error();
    }

    /*
     * result
     */
    int result(int chan) {
        Channel c = (Channel)channels.get(chan);
        if (c == null) {
            return 0;
        }
        return c.result();
    }

    /*
     * waitFor
     */
    void waitFor(long time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException ex) {
        }
    }

    /*
     * close
     */
    void close() {
        Enumeration e = channels.elements();
        while (e.hasMoreElements()) {
            Channel c = (Channel)e.nextElement();
            if (c != null) {
                c.close();
            }
        }
    }

}


