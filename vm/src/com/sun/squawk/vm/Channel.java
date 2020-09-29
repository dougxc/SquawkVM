package com.sun.squawk.vm;
import java.io.*;
import javax.microedition.io.*;

public class Channel implements SquawkOpcodes {

    Memory mem;
    int index;
    int error;
    int result;
    Connection con;
    DataInputStream dis;
    DataOutputStream dos;

    // debugging
    DataOutputStream inLog;
    DataOutputStream outLog;

   /*
    * Constructor
    */
    public Channel(Memory mem, int index, boolean debug) {
        this.mem = mem;
        this.index = index;
        if (debug) {
            try {
                this.inLog = new DataOutputStream(new FileOutputStream("channel"+index+".input"));
                this.outLog = new DataOutputStream(new FileOutputStream("channel"+index+".output"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /*
     * bytesToString
     */
    String bytesToString(int buf, int off, int lth) {
        char[] b = new char[lth];
        for (int i = 0; i != lth; i++) {
            b[i] = (char)mem.getUnsignedByte(buf,i+off);
        }
        return new String(b);
    }

   /*
    * execute
    */
    int execute(int nativeParms[], int nparms, ChannelIO cio) {
        error = 0;
        try {
            int op = nativeParms[0];
            switch (op) {
                case OP_GETCHANNEL:
                case OP_FREECHANNEL: {
                    break;
                }

                case OP_OPEN: {
                    String prot = mem.getStringAsString(nativeParms[1]);
                    String name = mem.getStringAsString(nativeParms[2]);
                    int    mode = nativeParms[3];
                    int    tmo  = nativeParms[4];
                    con = Connector.open(prot+":"+name, mode, tmo==1);
                    break;
                }

                case OP_CLOSE: {
                    con.close();
                    break;
                }

                case OP_ACCEPT: { // This is not really going to work because the whole VM will block
                    StreamConnection sc = ((StreamConnectionNotifier)con).acceptAndOpen();
                    Channel chan = cio.createChannel();
                    chan.con = sc;
                    result = chan.index;
                    break;
                }

                case OP_OPENINPUT: {
                    dis = ((StreamConnection)con).openDataInputStream();
                    break;
                }

                case OP_CLOSEINPUT: {
                    dis.close();
                    break;
                }

                case OP_READBYTE: {
                    result = dis.read();
                    if (inLog != null) inLog.writeByte(result);
//System.err.println("OP_READBYTE: "+result+(!Character.isISOControl((char)result) ? " ('"+(char)result+"')" : ""));
                    break;
                }

                case OP_READSHORT: {
                    result = dis.readShort();
                    if (inLog != null) inLog.writeShort(result);
//System.err.println("OP_READSHORT: "+result);
                    break;
                }

                case OP_READINT: {
                    result = dis.readInt();
                    if (inLog != null) inLog.writeInt(result);
//System.err.println("OP_READINT: "+result);
                    break;
                }

                case OP_READLONG: {
                    int buf = nativeParms[1];
                    long l = dis.readLong();
                    mem.setLong(buf, 0, l);
                    if (inLog != null) inLog.writeLong(l);
//System.err.println("OP_READLONG: "+mem.getLong(buf,0));
                    break;
                }

                case OP_READBUF: {
                    int buf = nativeParms[1];
                    int off = nativeParms[2];
                    int lth = nativeParms[3];
                    for (int i = 0 ; i < lth ; i++) {
                        int b = dis.read();
                        mem.setByte(buf, off+i, b);
                        if (inLog != null) inLog.writeByte(b);
                    }
//System.err.println("OP_READBUF: "+lth+" bytes read");
                    break;
                }

                case OP_SKIP: {
                    int buf = nativeParms[1];
                    dis.skip(mem.getLong(buf, 0));
                    break;
                }

                case OP_AVAILABLE: {
                    result = dis.available();
                    break;
                }

                case OP_MARK: {
                    int limit = nativeParms[1];
                    dis.mark(limit);
                    break;
                }

                case OP_RESET: {
                    dis.reset();
                    break;
                }

                case OP_MARKSUPPORTED: {
                    result = dis.markSupported() ? 1 : 0;
                    break;
                }

                case OP_OPENOUTPUT: {
                    dos = ((StreamConnection)con).openDataOutputStream();
                    break;
                }

                case OP_FLUSH: {
                    dos.flush();
                    break;
                }

                case OP_CLOSEOUTPUT: {
                    dos.close();
                    break;
                }

                case OP_WRITEBYTE: {
                    int ch = nativeParms[1];
                    dos.write(ch);
                    if (outLog != null) outLog.writeByte(ch);
//System.err.println("OP_WRITEBYTE: "+ch+(!Character.isISOControl((char)ch) ? " ('"+(char)ch+"')" : ""));
                    break;
                }

                case OP_WRITESHORT: {
                    int val = nativeParms[1];
                    dos.writeShort(val);
                    if (outLog != null) outLog.writeShort(val);
//System.err.println("OP_WRITESHORT: "+val);
                    break;
                }

                case OP_WRITEINT: {
                    int val = nativeParms[1];
                    dos.writeInt(val);
                    if (outLog != null) outLog.writeInt(val);
//System.err.println("OP_WRITEINT: "+val);
                    break;
                }

                case OP_WRITELONG: {
                    int buf = nativeParms[1];
                    long l = mem.getLong(buf, 0);
                    dos.writeLong(l);
                    if (outLog != null) outLog.writeLong(l);
                    break;
                }

                case OP_WRITEBUF: {
                    int buf = nativeParms[1];
                    int off = nativeParms[2];
                    int lth = nativeParms[3];
                    for (int i = 0 ; i < lth ; i++) {
                        int b = mem.getUnsignedByte(buf, off+i);
                        dos.write(b);
                        if (outLog != null) outLog.writeByte(b);
                    }
//System.err.println("OP_WRITEBUF: "+lth+" bytes written ("+bytesToString(buf, off, lth)+")");
                    break;
                }

            }
        } catch(IOException ex) {
            error = EXNO_IOException;
ex.printStackTrace();
        } catch(ClassCastException ex) {
            error = EXNO_IOException;
ex.printStackTrace();
        } catch(NullPointerException ex) {
            error = EXNO_IOException;
ex.printStackTrace();
        }

        return 0;
    }

   /*
    * error
    */
    int error() {
        return error;
    }

   /*
    * result
    */
    int result() {
        return result;
    }

    /*
     * close
     */
    void close() {
        if (inLog != null) {
            try {
                inLog.close();
            } catch (IOException ioe) {
            }
        }
        if (outLog != null) {
            try {
                outLog.close();
            } catch (IOException ioe) {
            }
        }
    }
}

