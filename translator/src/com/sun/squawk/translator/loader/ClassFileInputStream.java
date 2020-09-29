
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

class ClassFileInputStream extends DataInputStream implements RuntimeConstants, InputContext {

   /**
    * The file name
    */
    String fileName;

   /**
    * The trace flag
    */
    boolean trace = false;

   /**
    * The Virtual Machine context.
    */
    VirtualMachine vm;

   /**
    * Constructor
    */
//    public ClassFileInputStream(InputStream in) {
//        super(in);
//    }

    public ClassFileInputStream(InputStream in, String fileName, VirtualMachine vm) {
        super(in);
        this.fileName = fileName;
        this.vm = vm;
    }

    String getFileName() {
        return fileName;
    }

    VirtualMachine getVM() {
        return vm;
    }

    String getDetails() {
        return "Verification error in "+fileName;
    }

    public void verificationException(int code) throws LinkageException  {
        verificationException(code, null);
    }

    public void verificationException(int code, String msg) throws LinkageException {
        if (msg == null) {
            msg = "";
        } else {
            msg = ": "+msg;
        }
        if (code < 0) {
            throw new LinkageException(Type.VERIFYERROR, msg);
        }
        if (code < verifierMessage.length) {
             throw new LinkageException(Type.VERIFYERROR, ": " + verifierMessage[code] + msg);
        } else {
             throw new LinkageException(Type.VERIFYERROR, ": code=" + code + msg);
        }
    }

    public void verificationException(String msg) throws LinkageException  {
        throw new LinkageException(Type.VERIFYERROR, getDetails() + ": " + msg);
    }

    public void classFormatException(String msg) throws LinkageException  {
        throw new LinkageException(Type.CLASSFORMATERROR, getDetails() + ": " + msg);
    }

    public void linkageException(Type linkageErrorClass, String msg) throws LinkageException  {
        throw new LinkageException(linkageErrorClass, getDetails() + ": " + msg);
    }

    void setTrace(boolean value) {
        trace = value;
    }

    public boolean trace() {
        return trace;
    }

    public int readInt(String s) throws IOException {
        int value = readInt();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;

    }

    public int readUnsignedShort(String s) throws IOException {
        int value = readUnsignedShort();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;
    }

    public char readChar(String s) throws IOException {
        return (char)readUnsignedShort(s);
    }

     public int readUnsignedByte(String s) throws IOException {
         int value = readUnsignedByte();
         if (trace) {
            System.out.println(s+":"+value);
         }
         return value;

     }


    public short readShort(String s) throws IOException {
        short value = readShort();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;

    }

    public byte readByte(String s) throws IOException {
        byte value = readByte();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;
    }


    public long readLong(String s) throws IOException {
        long value = readLong();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;
    }

    public float readFloat(String s) throws IOException {
        float value = readFloat();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;
    }

    public double readDouble(String s) throws IOException {
        double value = readDouble();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;
    }


    public String readUTF(String s) throws IOException {
        String value = readUTF();
        if (trace) {
           System.out.println(s+":"+value);
        }
        return value;

    }

}
