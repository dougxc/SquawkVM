
package com.sun.squawk.translator.loader;
import  com.sun.squawk.util.*;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  java.util.Vector;
import  java.util.Enumeration;



import java.io.ByteArrayInputStream;

public class BytecodeHolder extends BaseFunctions implements RuntimeConstants {

   /**
    * Parent method
    */
    private Method method;

   /**
    * The constant pool
    */
    private ConstantPool pool;

   /**
    * The bytecode
    */
    private byte[] bytecodes;

   /**
    * The stack size needed
    */
    private int maxStack;

   /**
    * The number of locals
    */
    private int maxLocals;

   /**
    * The stackmap data
    */
    private byte[] stackMapData;

   /**
    * The line number table (or null)
    */
    private char[] lines;
    private int linePointer = 0;

   /**
    * Exception handlers
    */
    private char[] startPC = new char[0];
    private char[] endPC;
    private char[] handerPC;
    private char[] catchIndex;


   /**
    * Constructor
    */
    public BytecodeHolder(Method method, ConstantPool pool, byte[] bytecodes, int maxStack, int maxLocals) {
        this.method    = method;
        this.pool      = pool;
        this.bytecodes = bytecodes;
        this.maxStack  = maxStack;
        this.maxLocals = maxLocals;
    }

    public void setHandlerCount(int n) {
        startPC     = new char[n];
        endPC       = new char[n];
        handerPC    = new char[n];
        catchIndex  = new char[n];
    }

    public int getHandlerCount() {
        return startPC.length;
    }

    public void setHandler(int n, char startPC, char endPC, char handerPC, char catchIndex /*Type catchType*/) {
        this.startPC[n]   = startPC;
        this.endPC[n]     = endPC;
        this.handerPC[n]  = handerPC;
        this.catchIndex[n] = catchIndex;
    }

   /**
    * setMapData
    */
    public void setMapData(byte[] stackMapData) {
        this.stackMapData = stackMapData;
    }

   /**
    * setLineTable
    */
    public void setLineTable(char[] lines) {
        this.lines = lines;
    }

   /**
    * isJustReturn
    */
    public boolean isJustReturn() {
        return bytecodes.length == 1;
    }

    private int getLine(int ip) {
        int entryIP;
        for (;;) {
            if (linePointer == lines.length) {
                return lines[linePointer-1];
            }
            entryIP = lines[linePointer];
            if (ip <= entryIP) {
                break;
            }
            linePointer += 2;
        }
        if (ip == entryIP) {
            return lines[linePointer+1];
        } else {
            return lines[linePointer-1];
        }
    }

   /**
    * Check the exception handlers, add the exception types to them, and build
    * a sorted vector of BytecodeAddress objects that contains all the
    * handler start and end points, branch targets, and exception targets
    */
    Object[] checkHandlers(String methodName, StackMap map) throws LinkageException {

       /*
        * Exit now if there are no stackmaps
        */
        if (map == null) {
            assume(startPC.length == 0);
            return VirtualMachine.ZEROOBJECTS;
        }

       /*
        * Get the handler count and and stackmaps
        */
        int handlers = startPC.length;
        IntHashtable targets = map.getTargets();

       /*
        * Allocate a vector that can sort
        */
        AddressVector entries = new AddressVector(targets.size()+(handlers*2));

       /*
        * Add in the exception handler entries
        */
        if (handlers > 0) {
            for (int i = 0 ; i < handlers ; i++) {
                Target target = map.lookup(handerPC[i]);
                if (target == null) {
                    throw new LinkageException(Type.VERIFYERROR, "No stackmap entry for handler in " + methodName);
                }

               /*
                * If catchIndex is zero then this is a try/finally block. In this case
                * the exception type Type.THROWABLE will cover all possibilities
                */
                Type catchType  = (catchIndex[i] == 0) ? Type.THROWABLE : pool.getType(catchIndex[i]);

                if (!catchType.vIsAssignableTo(Type.THROWABLE)) {
                    throw new LinkageException(Type.VERIFYERROR, "Expect subclass of java.lang.Throwable");
                }

               /*
                * Set the exception type in the target
                */
                target.setExceptionTargetType(catchType);


if (handerPC[i] < endPC[i]) {
   System.out.println("WARNING from BytecodeHolder  **** handerPC[i] < endPC[i] " + methodName);
}

if (handerPC[i] < startPC[i]) {
   System.out.println("WARNING from BytecodeHolder **** handerPC[i] < startPC[i] " + methodName);
}

               /*
                * Add the start and end handler points to the address list
                */
                entries.addElement(new HandlerEndPoint(  endPC[i],   target));   // These get sorted first
                entries.addElement(new HandlerStartPoint(startPC[i], target));   // These get sorted after the end points
            }
        }

       /*
        * Add all the stackmap targets into the address list
        */
        for (Enumeration e = targets.elements() ; e.hasMoreElements() ;) {
            Target target = (Target)e.nextElement();
            entries.addElement(target);                                         // These get sorted after the handler points
        }

       /*
        * Return sorted entries
        */
        return entries.sorted();                                                // Here is the sorting....
    }

   /**
    * Convert the bytecodes into the IR
    */
    public Instruction getIR() throws LinkageException {

        StackMap map;

       /*
        * Get the VM for this method
        */
        VirtualMachine vm = method.type().getVM();

       /*
        * Be sure that the parameters and return type are loaded
        */
        method.load();

       /*
        * Create a stackmap object
        */
        if (stackMapData != null) {
            map = new StackMap(
                                new ClassFileInputStream(new ByteArrayInputStream(stackMapData), method.name(), vm),
                                pool,
                                method
                              );
        } else {
            map = null;
        }

       /*
        * Check that all the exception handlers have stackmap addresses
        */
        Object[] addressList = checkHandlers(method.name(), map);

       /*
        * Build the IR from the bytecodes.
        */
        BytecodeInputStream bcis = BytecodeInputStream.create(
                                                               new ByteArrayInputStream(bytecodes),
                                                               bytecodes.length,
                                                               method.parent()+"::"+method.name(),
                                                               method,
                                                               pool,
                                                               maxLocals,
                                                               map,
                                                               addressList
                                                             );

       /*
        * Enable trace if requested
        */
        bcis.setTrace(vm.tracebytecodes(method.parent().name()+"::"+method.name()));

       /*
        * Build the IR
        */
        Instruction inst = GraphBuilder.read(vm, bcis, method, maxStack, maxLocals, map);

       /*
        * Annotate the instructions with the lines numbers
        */
        if (lines != null) {
             Instruction i = inst;
             while (i != null) {
                 int ip = i.getIP();
                 i.setLine(getLine(ip));
                 i = i.getNext();
             }

        }

       /*
        * Optimize the IR
        */
        inst = GraphOptimizer.optimize(vm, method, inst);

       /*
        * Apply special transformations
        */
        return vm.getSpecialTransformer().transform(method, inst);
    }

}
