
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  java.util.Vector;
import  java.util.Enumeration;

class GraphOptimizer extends ParameterVisitor {


    Method method;

    int activeHandlers = 0;

    ArrayHashtable usedLocals = new ArrayHashtable();

    int optLevel;

//boolean tracing;

   /**
    * Main function
    */
    static Instruction optimize(VirtualMachine vm, Method method, Instruction ir) {
        return new GraphOptimizer(method).run(vm, ir);
    }

   /**
    * Private constructor
    */
    private GraphOptimizer(Method method) {
        this.method = method;
    }

   /**
    * Main function
    */
    private Instruction run(VirtualMachine vm, Instruction ir) {
        optLevel = vm.optimizationLevel();
        String name = method.parent().name()+"::"+method.name();
//tracing = vm.traceir1(name);
        trace(vm.traceir1(name), "Optimizing "+name);

       /*
        * Pass 1 - Iterate all the parameters in the IR to doParameter()
        */
        ir = ir.visitOn(this);
        assume(activeHandlers == 0);

       /*
        * Pass 2 - Iterate all the instructions and account for the result locals
        *          Also assign new ip addresses the instructions
        */
        int icount = 1;
        for (Instruction inst = ir ; inst != null ; inst = inst.getNext()) {
            boolean include = true;

           /*
            * Account for the result locals
            */
            Local resultLocal = inst.getResultLocal();
            if (resultLocal != null) {
                isUsed(inst, resultLocal);
            }

           /*
            * Work out if the instruction is to be included
            */
            if (inst instanceof LoadConstant && resultLocal == null) {
                include = false;
            } else if (inst instanceof LoadLocal) {
                Local loadsLocal = ((LoadLocal)inst).local();
                if (resultLocal == null) {
                    include = false;
                    inst.setResultLocal(loadsLocal); // Set the result local to the input local
                } else {
                    isUsed(inst, loadsLocal);
                }
            } else if (inst instanceof StoreLocal) {
                Local storesLocal = ((StoreLocal)inst).local();
                if (storesLocal == null) {
                    include = false;
                } else {
                    isUsed(inst, storesLocal);
                }
            } else if (inst instanceof Invoke) {
                Invoke invoke = (Invoke)inst;
                include = !invoke.ignore();
            }

           /*
            * Assign a new IP for the instruction if it is really executed.
            */
            if (include) {
                inst.setIP(icount++);
            } else {
                inst.setIP(-1);
            }
        }

       /*
        * Sort the local variables accouring to use
        */
        LocalVector vector = new LocalVector(usedLocals.size());
        for (Enumeration e = usedLocals.elements() ; e.hasMoreElements() ;) {
            Local local = (Local)e.nextElement();
            vector.addElement(local);
        }
        Instruction hdr = MethodHeader.create(icount, vector.sort());

       /*
        * Append the MethodHeader to the start of the IR
        */
        hdr.setNext(ir);
        ir = hdr;

       /*
        * traceir1
        */
        if (vm.traceir1(name)) {
            System.out.println("\n++IR1 trace for "+name);
            GraphPrinter.print(System.out, ir, vm, name);
            System.out.println("\n--IR1 trace for "+name+"\n");

        }

       /*
        * tracelocals
        */
        if (vm.tracelocals(name)) {
            System.out.println("\nLocals used in "+name);
            for (Enumeration e = vector.elements() ; e.hasMoreElements() ;) {
                Local local = (Local)e.nextElement();
                System.out.print("\t"+local+"\tused "+local.getUseCount());
                if (local.getParameterNumber() >= 0) {
                    System.out.print("\t arg "+local.getParameterNumber());
                }
                System.out.println();
            }
        }

        return ir;
    }

   /*
    * Mark the local as used, and accumulate its use count
    */
    private void isUsed(Instruction inst, Local local) {
        usedLocals.put(local, local);
//if(tracing)
//prtn(""+local+" "+ inst.getLoopDepth()+"\t"+inst+" was "+local.getUseCount());
        local.addUseCount(inst.getLoopDepth());
    }

   /**
    * Parameter optomizing function
    */
    public Instruction doParameter(Instruction inst, Instruction parm) { // instruction uses parm
        Local parmLocal = parm.getResultLocal();
        if (doParameter(inst, parm, parmLocal)) {
            isUsed(inst, parmLocal);
        }
        return parm;
    }

   /**
    * Parameter optomizing function
    */
    public boolean doParameter(Instruction inst, Instruction parm, Local parmLocal) {

        assume(parm != null, "parm null for ="+inst);

        boolean parmUsed = parmLocal != null;

        if (parm instanceof LoadLocal) {
            assume(parmLocal != null, "parmLocal null for inst="+inst+" parm="+parm);
            assume(parmLocal instanceof TemporaryLocal, "parmLocal="+parmLocal);
        }

       /*
        * Keep track of the number of active exception handlers.
        * This information is useful to calculate if it is possible
        * to move stores to local variables before instructions that
        * can trap. Its not perfect because no check is made about the
        * handlers exception type, but it is not too bad a heuristic.
        */
        if (inst instanceof HandlerEnter) {
            activeHandlers++;
        } else if (inst instanceof HandlerExit) {
            activeHandlers--;
            assume(activeHandlers >= 0);
        }

       /*
        * StoreLocals can be optimized by having the instructions that
        * produce their input write directly to the local variable instead
        * of to a temporary.
        */
        if (inst instanceof StoreLocal) {
            Local storesLocal = ((StoreLocal)inst).local();

           /*
            * Work out if the store can be pushed back into another instruction
            */
            if (optLevel > 0 && !parm.wasDuped() && connectsWithoutAccessing(parm, inst, storesLocal)) {
               /*
                * Find the instuction that is writing to the temporary local and
                * change its setResultLocal to store directly into the local.
                */
                parmUsed = false;
                parm.setResultLocal(storesLocal);

               /*
                * Disable this instruction
                */
                ((StoreLocal)inst).setLocal(null);
            }
            return parmUsed;
        }


       /*
        * Phis are never optimized
        */
        if (inst instanceof Phi) {
            return parmUsed;
        }

       /*
        * LoadLocals can be optimized out if there are no Phis or stores between
        * the load and its use
        */
        if (parm instanceof LoadLocal) {
            Local loadsLocal = ((LoadLocal)parm).local();
            if (optLevel > 0 && !parm.wasDuped() && connectsWithoutStoringTo(parm, inst, loadsLocal)) {
                assume(parmLocal != null);
               /*
                * Delete the temporary local varible assigned by the graph builder
                * the instruction whose parameter this is can load the variable directly
                */
                parmUsed = false;
                parm.setResultLocal(null);
            }
            isUsed(inst, loadsLocal);
            return parmUsed;
        }

       /*
        * LoadConstants can be optimized out if there are no Phis between
        * the load and its use.
        */
        if (parm instanceof LoadConstant) {
            if (optLevel > 0 && connectsSimplyTo(parm, inst)) {
                parmUsed = false;
                parm.setResultLocal(null);
            }
            return parmUsed;
        }

        return parmUsed;
    }


   /**
    *
    */
    private boolean connectsWithoutAccessing(Instruction from0, Instruction to, Local local) {
        Instruction from = from0;
        while (from != to) {
            if (from instanceof Phi) {
                return false;
            }
            if (from.isStoreToLocal(local)) {
                return false;
            }
            if (from != from0 && from.isLoadFromLocal(local)) {
                return false;
            }
            if (activeHandlers > 0 && from.canTrap()) {
                return false;
            }
            assume(from.getNext() != null, "inst="+from);
            from = from.getNext();
        }
        return true;
    }


   /**
    *
    */
    private boolean connectsWithoutStoringTo(Instruction from, Instruction to, Local local) {
        while (from != to) {
            if (from instanceof Phi) {
                return false;
            }
            if (from.isStoreToLocal(local)) {
                return false;
            }
            assume(from.getNext() != null, "inst="+from);
            from = from.getNext();
        }
        return true;
    }

   /**
    *
    */
    private boolean connectsSimplyTo(Instruction from, Instruction to) {
        while (from != to) {
            if (from instanceof Phi) {
                return false;
            }
            assume(from.getNext() != null, "inst="+from);
            from = from.getNext();
        }
        return true;
    }

}















/*

    private static ParameterVisitor verySimpleOptomizer =
        new ParameterVisitor() {
            Instruction doParameter(Instruction inst, Instruction parm) {
                if (
                     parm.isSimpleArgument() &&
                     !(inst instanceof Phi)  &&
                     parm.getNext() == inst
                   ) {
                    parm.setResultLocal(null);
                }
                return parm;
            }
        };

*/