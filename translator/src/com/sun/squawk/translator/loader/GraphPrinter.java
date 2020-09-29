
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;

import java.io.PrintStream;

public class GraphPrinter extends InstructionVisitor implements RuntimeConstants {

    private PrintStream out;

    private VirtualMachine vm;
    private String name;
    private String pref;
    private MethodHeader methodHeader;
    private String ln = "\n";

   /**
    * Main fucnction
    */
    public static void print(PrintStream out, Instruction ir, VirtualMachine vm, String name) {
        new GraphPrinter(out, ir, vm, vm.traceip(name), vm.tracelocals(name),  true, "\n");
    }

   /**
    * Main fucnction
    */
    public static void printOne(PrintStream out, Instruction ir, VirtualMachine vm) {
        new GraphPrinter(out, ir, vm, true, false, false, "\n");
    }

   /**
    * Main fucnction
    */
    public static void printOne(PrintStream out, Instruction ir, VirtualMachine vm, String ln) {
        new GraphPrinter(out, ir, vm, true, false, false, ln);
    }


   /**
    * Private constructor
    */
    private GraphPrinter(PrintStream out, Instruction ir, VirtualMachine vm, boolean traceip, boolean tracelocals, boolean loop, String ln) {
        this.out  = out;
        this.vm   = vm;
        this.ln   = ln;

        //ir.visitOn(this);

        do {
            pref = "";
            if (traceip) {
                pref += ir.getIP()+":\t";
                //pref += ir.getOrigionalIP()+":\t";

            }
            if (tracelocals) {
                pref += ir.getLoopDepth()+"\t";
            }
            ir.visit(this);
            ir = ir.getNext();
        } while (ir != null && loop);

    }


    private void print(String s) {
        out.print(s);
    }

    private void println(String s) {
        out.print(s);
        out.print(ln);
    }

    private void println() {
        out.print(ln);
    }

    private String prtType(Type type) {
        return ""+type;
    }

    private String prtLocal(Local local) {
        if (methodHeader != null) {
            assume (methodHeader.includes(local), "local="+local); // check the optomizer did include the local
        }
        return ""+local;
    }

    private String prtLocal(Instruction inst) {
        Local local = inst.getResultLocal();
        if (local != null) {
            return prtLocal(local);
        } else {
            if (inst instanceof LoadLocal) {
                return prtLocal(((LoadLocal)inst).local());
            } else if (inst instanceof LoadConstant) {
                return inst.toString();
            } else {
                throw fatal("bad instruction "+inst);
            }
        }
    }

    private String prtField(Field field, Instruction ref) {
        String res;
        String slot = " (fslot "+field.getSlotOffset()+ (field.isStatic()? "s)" : ")");;
        String refStr = (ref == null) ? "" : prtLocal(ref);
        if (vm.verbose()) {
            res = "[" + field.parent() + "::" + field + slot +"] " + refStr;
        } else {
            res = "[" + field + slot +"] " + refStr;
        }
        return res;
    }

    private String prtMethod(Method method) {
        String res;
        String slot = " (mslot "+method.getSlotOffset()+ (method.isStatic()? "s)" : ")");
        if (vm.verbose()) {
            res = "[" + method.parent() + "::" + method + slot +"]";
        } else {
            res = "[" + method + slot +"]";
        }
        return res;
    }


    private void prt2op(Instruction result,  String op, Instruction src) {
        println(pref+"    "+ prtLocal(result) + "   \t= " + op + " " + prtLocal(src));
    }

    private void prt3op(Instruction result, Instruction left,  String op, Instruction right) {
        println(pref+"    "+ prtLocal(result) + "   \t= " + prtLocal(left) + " " + op + " " + prtLocal(right));
    }

    private void prt2op(Instruction result, int op, Instruction src) {
        prt2op(result, opNames[op], src);
    }

    private void prt3op(Instruction result, Instruction left, int op, Instruction right) {
        prt3op(result, left, opNames[op], right);
    }



    private void prtGoto(Target target) {
        println(pref+"    goto "+target.getIP());
    }

    private void prtif(Instruction left, String op, Instruction right, Target target) {
        print(pref+"    if "+ prtLocal(left) + " " + op + " " + prtLocal(right));
        println(" goto "+target.getIP());
    }






    public void doArithmeticOp(ArithmeticOp inst) {
        prt3op(inst, inst.left(), inst.op(), inst.right());
    }

    public void doArrayLength(ArrayLength inst) {
        prt2op(inst, "arraylength", inst.array());
    }

    public void doCast(Cast inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= " + prtLocal(inst.value()) +" cast " + prtType(inst.type()));
    }

    public void doCheckCast(CheckCast inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= " + prtLocal(inst.value()) +" checkcast " + prtType(inst.type()));
    }

    public void doCheckStore(CheckStore inst) {
        println();
    }

    public void doConvertOp(ConvertOp inst) {
        prt2op(inst, inst.op(), inst.value());
    }

    public void doGoto(Goto inst) {
        prtGoto(inst.target());
    }

    public void doIfOp(IfOp inst) {
        prtif(inst.left(), opNames[inst.op()], inst.right(), inst.target());
    }

    public void doInstanceOf(InstanceOf inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= " + prtLocal(inst.value()) +" instanceof " + prtType(inst.checkType()));
    }

    public void doHandlerEnter(HandlerEnter inst) {
        println(pref+"    handlerenter " + inst.target().getIP());
    }

    public void doHandlerExit(HandlerExit inst) {
        println(pref+"    handlerexit " + inst.target().getIP());
    }

    public void doInitializeClass(InitializeClass inst) {
        println(pref+"    InitializeClass "+ inst.parent());
    }

    public void doInvoke(Invoke inst) {
        if (inst.getResultLocal() == null) {
            print(pref+"    invoke "+ prtMethod(inst.method()));
        } else {
            print(pref+"    "+ prtLocal(inst) + "   \t= invoke "+ prtMethod(inst.method()));
        }
        for (int i = 0 ; i < inst.parms().length ; i++) {
            print(" " + prtLocal(inst.parms()[i]));
        }
        println();
    }

    public void doLoadConstant(LoadConstant inst) {
        if (inst.getResultLocal() != null) {
            println(pref+"    "+ prtLocal(inst) + "   \t= " +  inst);
        }
    }

    public void doLoadConstantObject(LoadConstantObject inst) {
        if (inst.getResultLocal() != null) {
            println(pref+"    "+ prtLocal(inst) + "   \t= " +  inst);
        }
    }

    public void doLoadException(LoadException inst) {
        print(pref + inst.target().getIP() + ":");
        println(" " + prtLocal(inst) + " = exception("+inst.target().getExceptionTargetType()+")");
    }

    public void doLoadField(LoadField inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= " + prtField(inst.field(), inst.ref()));
    }

    public void doLoadIndexed(LoadIndexed inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= " +  prtLocal(inst.array()) + "[" + prtLocal(inst.index()) + "]");
    }

    public void doLoadLocal(LoadLocal inst) {
        if (inst.getResultLocal() != null && inst.getResultLocal() !=  inst.local()) {
            println(pref+"    "+ prtLocal(inst) + "   \t= " + inst.local());
        }
    }

    public void doMethodHeader(MethodHeader inst) {
/*
        assume(methodHeader == null);
        methodHeader = inst;
        print(pref+"    Instructions = "+inst.getInstructionCount()+" Locals =");
        int count = inst.getLocalCount();
        for (int i = 0 ; i < count ; i++) {
            Local local = inst.getLocal(i);
            if (local.getParameterNumber() >= 0) {
                print(" *"+prtLocal(local));
            } else {
                print(" "+prtLocal(local));
            }
        }
        println();
*/
    }

    public void doLookupSwitch(LookupSwitch inst) {
        print(pref+"    lookupswitch "+ prtLocal(inst.key()) + " ");
        for (int i = 0 ; i < inst.matches().length ; i++) {
            print(" (" + inst.matches()[i] + "=" + inst.targets()[i].getIP() + ")");
        }
        println(" (default=" + inst.defaultTarget().getIP() + ")");
    }

    public void doMonitorEnter(MonitorEnter inst) {
        println(pref+"    monitorenter "+ prtLocal(inst.value()));
    }

    public void doMonitorExit(MonitorExit inst) {
        println(pref+"    monitorexit "+ prtLocal(inst.value()));
    }

    public void doSimpleText(SimpleText inst) {
        println();
    }

    public void doNegateOp(NegateOp inst) {
        prt2op(inst, "!", inst.value());
    }

    public void doNewArray(NewArray inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= new " +inst.type() + " [" + prtLocal(inst.size()) + "]");
    }

    public void doNewMultiArray(NewMultiArray inst) {
        print(pref+"    "+ prtLocal(inst) + "   \t= new " +inst.type());
        for (int i = 0 ; i < inst.dimList().length ; i++) {
            print(" [" + inst.dimList()[i] + "]");
        }
        println();
    }

    public void doNewObject(NewObject inst) {
        println(pref+"    "+ prtLocal(inst) + "   \t= new " +inst.type());
    }

    public void doPhi(Phi inst) {
        print(pref+inst.target().getIP() + ":");
        if (inst.parms() != null) {
            print(" phi "+ prtLocal(inst) + " = " + inst.parms().length + " sources");
        }
        println();
    }

    public void doReturn(Return inst) {
        print(pref+"    return");
        if (inst.value() != null) {
            print(" "+ prtLocal(inst.value()));
        }
        println();
    }

    public void doStoreField(StoreField inst) {
        println(pref + "    " +  prtField(inst.field(), inst.ref()) + " \t= " + prtLocal(inst.value()));
    }

    public void doStoreIndexed(StoreIndexed inst) {
        println(pref+"    "+ prtLocal(inst.array()) + "[" + prtLocal(inst.index()) + "]" + "   \t= " + prtLocal(inst.value()));
    }

    public void doStoreLocal(StoreLocal inst) {
        if (inst.local() != null) {
            println(pref+"    " + inst.local() + "   \t= " + prtLocal(inst.value()));
        }
    }

    public void doTableSwitch(TableSwitch inst) {
        print(pref+"    tableswitch "+ prtLocal(inst.key()) + " ");
        for (int i = 0 ; i < inst.targets().length ; i++) {
            print(" (" + (inst.low()+i) + "=" + inst.targets()[i].getIP() + ")");
        }
        println(" (default=" + inst.defaultTarget().getIP() + ")");
    }

    public void doThrow(Throw inst) {
        println(pref+"    throw "+ prtLocal(inst.value()));
    }

}

