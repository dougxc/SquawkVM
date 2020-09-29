
package com.sun.squawk.translator.xml;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;
import  java.io.*;

import java.io.PrintStream;

public class XMLGraphPrinter extends InstructionVisitor implements RuntimeConstants, com.sun.squawk.vm.ClassNumbers {

    private PrintStream out;

    private VirtualMachine vm;
    private String name;
    private MethodHeader methodHeader;
    private boolean yields = true;

    private final static boolean DEBUG = true;


    public static final String opNames[] = {
        "**error**",
        "add",      //OP_ADD
        "sub",      //OP_SUB
        "mul",      //OP_MUL
        "div",      //OP_DIV
        "rem",      //OP_REM
        "sll",      //OP_SHL
        "sra",      //OP_SHR
        "srl",      //OP_USHR
        "and",      //OP_AND
        "orr",      //OP_OR
        "xor",      //OP_XOR
        "neg",      //OP_NEG
        "i2l",      //OP_I2L
        "i2f",      //OP_I2F
        "i2d",      //OP_I2D
        "l2i",      //OP_L2I
        "l2f",      //OP_L2F
        "l2d",      //OP_L2D
        "f2i",      //OP_F2I
        "f2l",      //OP_F2L
        "f2d",      //OP_F2D
        "d2i",      //OP_D2I
        "d2l",      //OP_D2L
        "d2f",      //OP_D2F
        "i2b",      //OP_I2B
        "i2c",      //OP_I2C
        "i2s",      //OP_I2S
        "cmpl",     //OP_LCMP
        "cmpfl",    //OP_FCMPL
        "cmpfg",    //OP_FCMPG
        "cmpdl",    //OP_DCMPL
        "cmpdg",    //OP_DCMPG
        "eq",       //OP_EQ
        "ne",       //OP_NE
        "lt",       //OP_LT
        "ge",       //OP_GE
        "gt",       //OP_GT
        "le",       //OP_LE
    };





   /**
    * Main fucnction
    */
    public static void print(PrintStream out, Method method, VirtualMachine vm) throws LinkageException {
        new XMLGraphPrinter(out, method, vm);
    }

    Instruction in;

   /**
    * Private constructor
    */
    private XMLGraphPrinter(PrintStream out, Method method, VirtualMachine vm) throws LinkageException {
        this.out  = out;
        this.vm   = vm;

       /*
        * Don't put yield instructions into system code
        */
        String cname = method.parent().name();
        if (cname.startsWith("Ljava/") || cname.startsWith("Lcom/sun/")) {
            yields = false;
        }

       /*
        * Print the method header
        */
        vm.xmlHead(method.toString(), method.toString(false,true), method.getSlotOffset(), out);

        if (method.isStatic() && !method.name().startsWith("_SQUAWK_INTERNAL_")) {
            out.println("      <static/>");
        }

        Method irMethod = method.asIrMethod();

        if (irMethod instanceof MethodProxy) {
            MethodProxy proxy = (MethodProxy)irMethod;
            out.println("      <super>"+proxy.targetSlotOffset()+"</super>");
            out.println("    </method>");
            return;
        }

        if (method.isNative()) {
            out.println("      <native/>");
            out.println("    </method>");
            return;
        }

        Instruction[] ir = ((IntermediateMethod)irMethod).getInstructions();

        if (ir == null) {
            out.println("      <abstract/>");
            out.println("    </method>");
            return;
        }

        out.println("      <local_variables>");
        methodHeader = (MethodHeader)ir[0];
        int count = methodHeader.getLocalCount();
        for (int i = 0 ; i < count ; i++) {
            Local local = methodHeader.getLocal(i);
            if (VirtualMachine.LONGSARETWOWORDS && local.slotType() == Type.BASIC_LONG) {
                out.println("        "+formatLocalAsXml(local, "word", local.getOffset()));
                out.println("        "+formatLocalAsXml(local, "word", local.getOffset()+1));
            } else {
                out.println("        "+formatLocalAsXml(local, local.xmltag(), local.getOffset()));
            }
        }
        out.println("      </local_variables>");


        out.println("      <parameter_map>");
        Type[] parms = method.getParms();
        int from = 0;
        for (int i = 0 ; i < parms.length ; i++) {
            Local parm = null;
            for (int j = 0 ; j < count ; j++) {
                Local local = methodHeader.getLocal(j);
                if (local.getParameterNumber() == i) {
                    parm = local;
                    break;
                }
            }

            int offset;
            if (parm == null) {
                if (!VirtualMachine.LONGSARETWOWORDS && parms[i].isTwoWords()) {
                    offset = -2;
                }
                else {
                    offset = -1;
                }
            }
            else {
                offset = parm.getOffset();
            }

            if (VirtualMachine.LONGSARETWOWORDS && parms[i].isTwoWords()) {
                out.println("        <from>"+(from++)+"</from><to>"+offset+"</to>");
                if (offset != -1) {
                    offset++;
                }
                out.println("        <from>"+(from++)+"</from><to>"+offset+"</to>");
            } else {
                out.println("        <from>"+(from++)+"</from><to>"+offset+"</to>");
            }
        }
        out.println("      </parameter_map>");


        out.println("      <instructions>"); // +"<!-- n="+(methodHeader.getInstructionCount()-1)+"-->");
       /*
        * Print the instructions
        */
        for (int i = 0 ; i < ir.length ; i++) {
            in = ir[i];
            in.visit(this);
        }
        out.println("      </instructions>");
        out.println("    </method>");
    }

    private String formatLocalAsXml(Local local, String tag, int offset) {
        return "<!-- "+(local instanceof TemporaryLocal ? 't' : 'l')+offset+" --> <"+tag+"/>";
    }

    int lastline = -1;

    private void print(String str) {
        int line = in.getLine();
        if (line == -1) {
            line = lastline;
        }
        lastline = line;
        String sline = (line == -1) ? "" : " line=\""+line+"\"";
        String s = "        <i"+sline+">" + str + "</i>";

        if (DEBUG == true) {
            s = pad(s, 50);
            out.println(s+asCommentString(in));
        } else {
            out.println(s);
        }
    }

    private String pad(String s, int width) {
        while (s.length() < width) {
            s += " ";
        }
        return s;
    }

    private String asCommentString(Instruction ir) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(100);
        PrintStream ps = new PrintStream(baos);
        ps.print("<!--\t");
        GraphPrinter.printOne(ps, ir, vm, "");
        ps.close();
        byte[] instrBytes = baos.toByteArray();
        String instr = new String(instrBytes);
        // break up "-->" sequences
        int index = instr.indexOf("-->",0);
        while (index != -1) {
            instrBytes[index+1] = ' ';
            index = instr.indexOf("-->",index+3);
        }
        return new String(instrBytes) + " -->";
    }

    private String prtType(Type type, boolean staticCall) {
        if (staticCall) {
            return " &-"+type.getID();
        } else {
            return " &"+type.getID();
        }
    }

    private String prtType(Type type) {
        return prtType(type, false);
    }

    private String prtLocal(Local local) {
        if (methodHeader != null) {
            assume (methodHeader.includes(local), "local="+local); // check the optomizer did include the local
        }
        if (VirtualMachine.LONGSARETWOWORDS && local.slotType() == Type.BASIC_LONG) {
            return " "+local.getOffset() + " "+(local.getOffset()+1);
        } else {
            return " "+local.getOffset();
        }
    }


    private String prtConst(LoadConstant con) {
        return " "+con.toXml();
    }



    private String prtLocal(Instruction inst) {
        Local local = inst.getResultLocal();
        if (local != null) {
            return prtLocal(local);
        } else {
            if (inst instanceof LoadLocal) {
                return prtLocal(((LoadLocal)inst).local());
            } else if (inst instanceof LoadConstant) {
                return prtConst((LoadConstant)inst);
            } else {
                throw fatal("bad instruction "+inst);
            }
        }
    }

    private String prtField(Field field, Instruction ref) {
        assume(field.getSlotOffset() >= 0);
        return prtType(field.parent())+"@"+field.getSlotOffset();
    }

    private String prtMethod(Method method, boolean staticCall) {
        int slot = method.getSlotOffset();
        if (slot >= VirtualMachine.FIRSTINTERFACE) {
            slot = 0 - (slot-VirtualMachine.FIRSTINTERFACE);
        }
        return prtType(method.parent(), staticCall)+"@"+slot;
    }



/*
    private char opType(Instruction inst) {
        switch (inst.type().name().charAt(0)) {
            default : return 'i';
            case 'J': return 'l';
            case 'F': return 'f';
            case 'D': return 'd';
            case 'L':
            case '[': return 'r';
        }
    }

    private char opType2(Instruction inst) {
        switch (inst.type().name().charAt(0)) {
            default : return 'i';
            case 'J': return 'l';
            case 'F': return 'f';
            case 'D': return 'd';
            case 'C': return 'c';
            case 'S': return 's';
            case 'B': return 'b';
            case 'L':
            case '[': return 'r';
        }
    }
*/

    private char opType(Type t) {
        if (t == Type.BYTE)    return 'i';
        if (t == Type.BOOLEAN) return 'i';
        if (t == Type.CHAR)    return 'i';
        if (t == Type.SHORT)   return 'i';
        if (t == Type.INT)     return 'i';
        if (t == Type.FLOAT)   return 'f';
        if (t == Type.LONG)    return 'l';
        if (t == Type.DOUBLE)  return 'd';
        return 'r';
    }

    private char opType(Instruction inst) {
        return opType(inst.type());
    }

    private char opType2(Type t) {
        if (t == Type.BYTE)    return 'b';
        if (t == Type.BOOLEAN) return 'z';
        if (t == Type.CHAR)    return 'c';
        if (t == Type.SHORT)   return 's';
        if (t == Type.INT)     return 'i';
        if (t == Type.FLOAT)   return 'f';
        if (t == Type.LONG)    return 'l';
        if (t == Type.DOUBLE)  return 'd';
        return 'r';
    }

    private char opType2(Instruction inst) {
        return opType2(inst.type());
    }


    private String prtOp(String name, Instruction inst) {
        char type = opType(inst);
        // The 'i' is implicit in int instructions and conversion instructions
        // already explicitly include the type character at the end
        if (type != 'i'  && !(inst instanceof ConvertOp)) {
            name += type;
        }
        return name;
    }

    private String prtOp2(String name, Type t) {
        char type = opType2(t);
        // The 'i' is implicit in int instructions and conversion instructions
        // already explicitly include the type character at the end
        if (type != 'i') {
            name += type;
        }
        return name;
    }

    private String prtOp2(String name, Instruction inst) {
        return prtOp2(name, inst.type());
    }

    private void prt2op(String op, Instruction dst, Instruction src) {
        print(op + prtLocal(dst) + prtLocal(src));
    }

    private void prt2op(int op, Instruction dst, Instruction src) {
        print(prtOp(opNames[op], dst) + prtLocal(dst) + prtLocal(src));
    }

    private void prt3op(int op, Instruction dst, Instruction left, Instruction right) {
        print(prtOp(opNames[op], dst) + prtLocal(dst) + prtLocal(left) + prtLocal(right));
    }

    private void prtGoto(Target target) {
        print("goto $"+target.getIP());
    }

    private void prtif(int op, Instruction left, Instruction right, Target target) {
        String opname = right.type().isPrimitive() ? opNames[op] : opNames[op]+"r";
        print("if"+opname+prtLocal(left)+prtLocal(right)+" $"+target.getIP());
    }



    public void doArithmeticOp(ArithmeticOp inst) {
        prt3op(inst.op(), inst, inst.left(), inst.right());
    }

    public void doArrayLength(ArrayLength inst) {
        prt2op("arraylength", inst, inst.array());
    }

    public void doCast(Cast inst) {
        prt2op("movr", inst, inst.value());
    }

    public void doCheckCast(CheckCast inst) {
        print("checkcast" + prtLocal(inst.value()) + prtType(inst.checkType()));
    }

    public void doCheckStore(CheckStore inst) {
        print("checkstore" + prtLocal(inst.value()) + prtLocal(inst.array()));
    }

    public void doConvertOp(ConvertOp inst) {
        prt2op(inst.op(), inst, inst.value());
    }

    public void doGoto(Goto inst) {
        prtGoto(inst.target());
    }

    public void doIfOp(IfOp inst) {
        prtif(inst.op(), inst.left(), inst.right(), inst.target());
    }

    public void doInstanceOf(InstanceOf inst) {
        print("instanceof" + prtLocal(inst) + prtLocal(inst.value()) + prtType(inst.checkType()));
    }

    public void doHandlerEnter(HandlerEnter inst) {
        Target t = inst.target();
        print("try" + prtType(t.getExceptionTargetType()) + " $" + t.getIP());
    }

    public void doHandlerExit(HandlerExit inst) {
        Target t = inst.target();
        print("end" + prtType(t.getExceptionTargetType()));
    }

    public void doInitializeClass(InitializeClass inst) {
        print("clinit" + prtType(inst.parent()));
    }

    public void doInvoke(Invoke inst) {
        String invoke;
        Method method = inst.method();
        boolean staticCall = inst.isStatic()||inst.isSpecial();
        if (inst.getResultLocal() == null) {
            invoke = "invokev" + prtMethod(method, staticCall);
        } else {
            invoke = prtOp("invoke", inst) + prtLocal(inst) + prtMethod(method, staticCall);
        }
        if (inst.parms().length > 0) {
            int i = 0;
            if (inst.isStatic()) {
                invoke += " #0";
                i++;
            }
            for (; i < inst.parms().length ; i++) {
                invoke += prtLocal(inst.parms()[i]);
            }
        }
        print(invoke);
    }

    public void doLoadConstant(LoadConstant inst) {
        if (inst.getResultLocal() != null) {
            print(prtOp("mov", inst) + prtLocal(inst) + " " + inst.toXml());
        }
    }

    public void doLoadConstantObject(LoadConstantObject inst) {
        if (inst.getResultLocal() != null) {
            print("ldconst" + prtLocal(inst) + " " + inst.toXml());
        }
    }

    public void doLoadException(LoadException inst) {
        print("catch" + prtLocal(inst));
    }

    public void doLoadField(LoadField inst) {
        String ref = (inst.ref() == null) ? "" : prtLocal(inst.ref());
        print(prtOp2("ld", inst) + prtLocal(inst) + prtField(inst.field(), inst) + ref);
    }

    public void doLoadIndexed(LoadIndexed inst) {
        print(prtOp2("ld", inst) + prtLocal(inst) + prtLocal(inst.index()) + prtLocal(inst.array()));
    }

    public void doLoadLocal(LoadLocal inst) {
        if (inst.getResultLocal() != null && inst.getResultLocal() !=  inst.local()) {
            print(prtOp("mov", inst) + prtLocal(inst) +  prtLocal(inst.local()));
        }
    }

    public void doMethodHeader(MethodHeader inst) {

        int count = inst.getLocalCount();
        for (int i = 0 ; i < count ; i++) {
            Local local = inst.getLocal(i);
            Instruction cause = local.getCauseForClearing();
            if (cause != null) {
                out.println(pad("        <!-- clear "+local+" -->", 50)+asCommentString(cause));
            }
        }
/*
        assume(methodHeader == null);
        methodHeader = inst;
        out.print(pref+"    Instructions = "+inst.getInstructionCount()+" Locals =");
        int count = inst.getLocalCount();
        for (int i = 0 ; i < count ; i++) {
            Local local = inst.getLocal(i);
            if (local.getParameterNumber() >= 0) {
                out.print(" *"+prtLocal(local));
            } else {
                out.print(" "+prtLocal(local));
            }
        }
        out.print("\n");
*/
    }

    public void doLookupSwitch(LookupSwitch inst) {
        StringBuffer sb = new StringBuffer();
        sb.append("lookupswitch");
        sb.append(prtLocal(inst.key()));
        //sb.append(" #");
        //sb.append(inst.matches().length);
        sb.append(" $");
        sb.append(inst.defaultTarget().getIP());

        for (int i = 0 ; i < inst.matches().length ; i++) {
            sb.append(" $");
            sb.append(inst.targets()[i].getIP());
        }
        for (int i = 0 ; i < inst.matches().length ; i++) {
            sb.append(" #");
            sb.append(inst.matches()[i]);
        }
        print(sb.toString());
    }

    public void doMonitorEnter(MonitorEnter inst) {
        print("menter"+ prtLocal(inst.value()));
    }

    public void doMonitorExit(MonitorExit inst) {
        print("mexit"+ prtLocal(inst.value()));
    }

    public void doSimpleText(SimpleText inst) {
        Local result = inst.getResultLocal();
        Instruction p1 = inst.p1();
        Instruction p2 = inst.p2();
        String res_s = (result == null) ? "" : prtLocal(result);
        String p1_s  = (p1 == null) ? "" : prtLocal(p1);
        String p2_s  = (p2 == null) ? "" : prtLocal(p2);
        //if (inst.op().equals("new")) {
        //    p1_s = " &" + p1_s.trim();
        //}
        print(inst.op() + res_s + p1_s + p2_s);
    }

    public void doNegateOp(NegateOp inst) {
        prt2op(OP_NEG, inst, inst.value());
    }

    public void doNewArray(NewArray inst) {
    }

    public void doNewMultiArray(NewMultiArray inst) {
    }

    public void doNewObject(NewObject inst) {
    }

    public void doPhi(Phi inst) {
        if (inst.target().isBackwardTarget() && yields) {
            print("yield");
        } else {
            print("nop");
        }
    }

    public void doReturn(Return inst) {
        if (inst.value() != null) {
            print(prtOp("return", inst) + prtLocal(inst.value()));
        } else {
            print("returnv");
        }
    }

    public void doStoreField(StoreField inst) {
        String ref = (inst.ref() == null) ? "" : prtLocal(inst.ref());
//        print(prtOp2("st", inst.value()) + prtLocal(inst.value()) + prtField(inst.field(), inst) + ref);
        print(prtOp2("st", inst.field().type()) + prtLocal(inst.value()) + prtField(inst.field(), inst) + ref);

    }

    public void doStoreIndexed(StoreIndexed inst) {
//        print(prtOp2("st", inst.value()) + prtLocal(inst.value()) + prtLocal(inst.index()) + prtLocal(inst.array()));
        print(prtOp2("st", inst.basicType().elementType()) + prtLocal(inst.value()) + prtLocal(inst.index()) + prtLocal(inst.array()));
    }

    public void doStoreLocal(StoreLocal inst) {
        if (inst.local() != null) {
            print(prtOp("mov", inst.value()) +  prtLocal(inst.local()) + prtLocal(inst.value()));
        }
    }

    public void doTableSwitch(TableSwitch inst) {
        StringBuffer sb = new StringBuffer();
        sb.append("tableswitch");
        sb.append(prtLocal(inst.key()));
        sb.append(" #");
        sb.append(inst.low());
        sb.append(" #");
        sb.append(inst.high());
        sb.append(" $");
        sb.append(inst.defaultTarget().getIP());
        for (int i = 0 ; i < inst.targets().length ; i++) {
            sb.append(" $");
            sb.append(inst.targets()[i].getIP());
        }
        print(sb.toString());
    }

    public void doThrow(Throw inst) {
       print("throw"+ prtLocal(inst.value()));
    }

}

