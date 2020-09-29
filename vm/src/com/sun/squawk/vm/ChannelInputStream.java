package com.sun.squawk.vm;
import java.io.*;

public class ChannelInputStream extends DataInputStream {

   /*
    * Constructor
    */
    public ChannelInputStream(InputStream is) {
        super(is);
    }



}




/*
    OP_GETCHANNEL           = 1,
    OP_FREECHANNEL          = 2,
    OP_OPEN                 = 3,
    OP_CLOSE                = 4,
    OP_ACCEPT               = 5,
    OP_OPENINPUT            = 6,
    OP_CLOSEINPUT           = 7,
    OP_WRITEREAD            = 8,
    OP_READBYTE             = 9,
    OP_READSHORT            = 10,
    OP_READINT              = 11,
    OP_READLONG             = 12,
    OP_READBUF              = 13,
    OP_SKIP                 = 14,
    OP_AVAILABLE            = 15,
    OP_MARK                 = 16,
    OP_RESET                = 17,
    OP_MARKSUPPORTED        = 18,
    OP_OPENOUTPUT           = 19,
    OP_FLUSH                = 20,
    OP_CLOSEOUTPUT          = 21,
    OP_WRITEBYTE            = 22,
    OP_WRITESHORT           = 23,
    OP_WRITEINT             = 24,
    OP_WRITELONG            = 25,
    OP_WRITEBUF             = 26,
*/




