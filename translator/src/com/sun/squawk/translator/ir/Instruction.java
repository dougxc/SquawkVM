
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.loader.*;
import  com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;

public abstract class Instruction extends BaseFunctions {

    private Type type;
    private Instruction next;
//  private Instruction prev;
//  private Method method;
    private int ip;

//  private Instruction firstUse;
//  private Instruction lastUse;
    private Local resultLocal;
    private boolean duped = false;
    private int loopDepth = 1;
    private int line = -1;

    private Instruction() {
        type = null;
    }

    protected Instruction(Type type) {
        //if (type == Type.BOOLEAN || type == Type.BYTE || type == Type.SHORT || type == Type.CHAR) {
        //    this.type = Type.INT;
        //} else {
            this.type = type;
        //}
    }

    public Type type() {
        return type;
    }

    public void changeType(Type type) {
        this.type = type;
    }

    public void setContext(BytecodeInputStream is) {
        ip = is.getLastIP();
        //origionalIP = ip;
    }

    public int getIP() {
        return ip;
    }

    public void setIP(int ip) {
        this.ip = ip;
    }

    public void setLine(int n) {
        this.line = n;
    }

    public int getLine() {
        return line;
    }

    //int origionalIP = -1000;
    //public int getOrigionalIP() {
    //   return origionalIP;
    //}

    public void setNext(Instruction next) {
        this.next = next;
    }

    public Instruction getNext() {
        return next;
    }

    public boolean isTwoWords() {
        return type.isTwoWords();
    }


    public String toString() {
        String cname = this.getClass().toString();
        cname = cname.substring(cname.lastIndexOf('.')+1);
        if (type != null) {
            return cname+"@"+ip+"("+type+")";
        } else {
            return cname+"@"+ip;
        }
    }


    public Instruction visitOn(InstructionVisitor vistor) {
        Instruction ir = this;
        while (ir != null) {
            ir.visit(vistor);
            ir = ir.getNext();
        }
        return this;
    }


    public Instruction visitOn(ParameterVisitor vistor) {
        Instruction ir = this;
        while (ir != null) {
            ir.visit(vistor);
            ir = ir.getNext();
        }
        return this;
    }

    //public void isUsedBy(Instruction usingInstruction) {
    //    if (firstUse == null) {
    //        firstUse = usingInstruction;
    //    }
    //    lastUse = usingInstruction;
    //}

    public void setResultLocal(Local local) {
        resultLocal = local;
    }

    //public Local getRealResultLocal() {
    //    return resultLocal;
    //}

    public Local getResultLocal() {
        //if (resultLocal == null && this instanceof LoadLocal) {
        //    return ((LoadLocal)this).local();
        //}
        //assume(resultLocal != null || !(this instanceof LoadLocal));
        return resultLocal;
    }

    public int getResultOffset() {
        if (resultLocal == null) {
            return -1;
        }
        return resultLocal.getOffset();
    }

    public void isDuped() {
        duped = true;
    }

    public boolean wasDuped() {
        return duped;
    }


    public void incrementLoopDepth() {
        loopDepth *= 10;
    }

    public int getLoopDepth() {
        return loopDepth;
    }





    public boolean isUninitalizedNew() {
        return false;
    }

    public void setNewInitalized() {
    }

    public boolean isSimpleArgument() {
        return false;
    }

    public boolean isStoreToLocal(Local local) {
        return resultLocal == local;
    //    return false;
    }

    public boolean isLoadFromLocal(Local local) {
        return false;
    }

    public boolean canTrap() {
        return false;
    }

    public abstract void visit(InstructionVisitor vistor);
    public void visit(ParameterVisitor vistor) {}

}
