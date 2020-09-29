
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;
import  java.util.Vector;
import  java.util.Enumeration;

public class SpecialTransformer extends BaseFunctions {

    private VirtualMachine vm;

    private String Native_asInt;
    private String Native_asClass;
    private String Native_asIntArray;
    private String Native_asByteArray;
    private String Native_asOop;
    private String Native_floatToIntBits;
    private String Native_intBitsToFloat;
    private String Native_doubleToLongBits;
    private String Native_longBitsToDouble;
    private String Native_parm;
    private String Native_parm2;
    private String Native_exec;
    private String Native_error;
    private String Native_result;
    private String Native_getAR;
    private String Native_setAR;
    private String Native_newInstance;
    private String Native_newArray;

    private Method Class_newInstance;
    private Method Class_newArray;
    private Method Class_addDimension;

    private Instruction dummyReceiver = LoadConstant.createNull();


   /**
    * getMethod
    */
    private Method getMethod(Type type, String name, String desc) throws LinkageException {
        type.load();
        Method method = type.findMethod(vm.internString(name), desc);
        if (method == null) {
            throw new RuntimeException("Cannot find "+name+"() in "+type);
        }
        return method;
    }

   /**
    * Private constructor
    */
    public SpecialTransformer(VirtualMachine vm) throws LinkageException {
        this.vm = vm;
        Native_asInt             = vm.internString("asInt");
        Native_asOop             = vm.internString("asOop");
        Native_asClass           = vm.internString("asClass");
        Native_asIntArray        = vm.internString("asIntArray");
        Native_asByteArray       = vm.internString("asByteArray");
        Native_floatToIntBits    = vm.internString("floatToIntBits");
        Native_intBitsToFloat    = vm.internString("intBitsToFloat");
        Native_doubleToLongBits  = vm.internString("doubleToLongBits");
        Native_longBitsToDouble  = vm.internString("longBitsToDouble");
        Native_parm              = vm.internString("parm");
        Native_exec              = vm.internString("exec");
        Native_error             = vm.internString("error");
        Native_result            = vm.internString("result");
        Native_getAR             = vm.internString("getAR");
        Native_setAR             = vm.internString("setAR");
        Native_newInstance       = vm.internString("newInstance");
        Native_newArray          = vm.internString("newArray");
        Class_newInstance        = getMethod(Type.CLASS,   "newInstance",        "(I)Ljava/lang/Object;");
        Class_newArray           = getMethod(Type.CLASS,   "newArray",           "(II)Ljava/lang/Object;");
        Class_addDimension       = getMethod(Type.CLASS,   "addDimension",       "([Ljava/lang/Object;I)V");
    }


   /**
    * getInvoke
    */
    private Instruction getInvoke(Instruction prev, Instruction result, Method method, Instruction p1, Instruction p2) {
        Instruction[] parms = (p2 == null) ? new Instruction[] {dummyReceiver, p1} : new Instruction[] {dummyReceiver, p1, p2};
        Instruction invoke = Invoke.create(method, parms, Invoke.STATIC, false);
        if (result != null) {
            invoke.setResultLocal(result.getResultLocal());
        }
        invoke.setNext(prev.getNext());
        invoke.setLine(prev.getLine());
        prev.setNext(null); // So it can be GCed
        return invoke;
    }

   /**
    * getInvoke
    */
    private Instruction getInvoke(Instruction result, Method method, Instruction p1, Instruction p2) {
        return getInvoke(result, result, method, p1, p2);
    }

   /**
    * Main function
    */
    public Instruction transform(Method method, Instruction ir) {
        int extras = 0;
        Instruction prev = null;
        Instruction next = null;
        for (Instruction inst = ir ; inst != null ; inst = next) {
            Instruction last = inst;
            next = inst.getNext();

           /*
            * Unlink all the instructions that the optomizer removed
            */
            if (inst.getIP() == -1) {
                last = prev;
            }

           /*
            * Work out the backward branches of goto/if instructions
            */
            else if (inst instanceof Goto) {
                Goto ggoto = (Goto)inst;
                Target target = ggoto.target();
                target.isTargetFor(ggoto);
            }

           /*
            * Add a Cast instruction after a CheckCast
            */
            else if (inst instanceof CheckCast) {
                CheckCast check = (CheckCast)inst;
                Instruction cast = Cast.create(check.getResultLocal(), check.value());
                cast.setNext(check.getNext());
                check.setNext(cast);
                last = cast;
                extras++;
            }

           /*
            * Change all invokes to native functions into Native instructions
            */
            else if (inst instanceof Invoke) {
                Invoke invoke = (Invoke)inst;
                Method callee = invoke.method();
                String mname = callee.name();

                if (callee.isNative()) {
                    Local result = invoke.getResultLocal();
                    Instruction[] parms = invoke.parms();
                    Instruction replacement = null;
                    if (mname == Native_asInt || mname == Native_asOop || mname == Native_asClass || mname == Native_asIntArray || mname == Native_floatToIntBits || mname == Native_intBitsToFloat || mname == Native_asByteArray) {
                        replacement = SimpleText.create("mov", result, parms[1]);
                    } else if (mname == Native_doubleToLongBits || mname == Native_longBitsToDouble) {
                        replacement = SimpleText.create("movl", result, parms[1]);
                    } else if (mname == Native_parm) {
                        replacement = SimpleText.create("parm", result, parms[1]);
                    } else if (mname == Native_exec) {
                        replacement = SimpleText.create("exec", result, parms[1]);
                    } else if (mname == Native_error) {
                        replacement = SimpleText.create("error", result, parms[1]);
                    } else if (mname == Native_result) {
                        replacement = SimpleText.create("result", result, parms[1]);
                    } else if (mname == Native_getAR) {
                        replacement = SimpleText.create("getar", result, null);
                    } else if (mname == Native_setAR) {
                        replacement = SimpleText.create("setar", result, parms[1], parms[2]);
                    } else if (mname == Native_newInstance) {
                        replacement = SimpleText.create("new", result, parms[1]);
                    } else if (mname == Native_newArray) {
                        replacement = SimpleText.create("new", result, parms[1], parms[2]);
                    } else if (callee.parent() == Type.MATH) {
                        Instruction p2 = (parms.length > 2) ? parms[2] : null;
                        replacement = SimpleText.create("math_"+mname, result, parms[1], p2);
                    } else {
                        if (vm.allowNatives()) {
                            replacement =  SimpleText.create("nop <!-- fake native for "+callee+"--> ", null, null);
                        } else {
                            throw fatal("Unknown native method"+callee);
                        }
                    }
                    replacement.setNext(invoke.getNext());
                    replacement.setLine(invoke.getLine());
                    invoke.setNext(null); // So it can be GCed
                    prev.setNext(replacement);
                    last = replacement;
                }
            }

           /*
            * Add CheckStores before all StoreIndexed instructions that write into reference arrays
            */
            else if (inst instanceof StoreIndexed) {
                StoreIndexed store = (StoreIndexed)inst;
                if (store.basicType() == Type.OBJECT_ARRAY && store.value().type() != Type.NULLOBJECT) {
                    Instruction check = CheckStore.create(store.array(), store.value());
                    prev.setNext(check);
                    check.setNext(store);
                    extras++;
                }
            }

           /*
            * Make NewObject into a call to Class.newInstance();
            */
            else if (inst instanceof NewObject) {
                NewObject newobject = (NewObject)inst;
                int cno = newobject.type().getID();
                Instruction replacement = getInvoke(inst, Class_newInstance, LoadConstant.create(cno), null);
                prev.setNext(replacement);
                last = replacement;
            }

           /*
            * Make NewArray into a call to Class.newArray();
            */
            else if (inst instanceof NewArray) {
                NewArray newarray = (NewArray)inst;
                int cno = newarray.type().getID();
                Instruction replacement = getInvoke(inst, Class_newArray, LoadConstant.create(cno), newarray.size());
                prev.setNext(replacement);
                last = replacement;

            }

           /*
            * Make NewMultiArray into a call to Class.newArray() followed by a series of calls to Class.addDimension()
            */
            else if (inst instanceof NewMultiArray) {
                NewMultiArray newmulti = (NewMultiArray)inst;
                Instruction[] dimList = newmulti.dimList();
                int cno = newmulti.type().getID();
                Instruction replacement = getInvoke(inst, Class_newArray, LoadConstant.create(cno), dimList[0]);
                prev.setNext(replacement);
                last = replacement;
                for (int i = 1 ; i < dimList.length ; i++) {
                    Instruction adddim = getInvoke(last, null, Class_addDimension, replacement, dimList[i]);
                    last.setNext(adddim);
                    last = adddim;
                    extras++;
                }
            }

           /*
            * Reset the previous instruction
            */
            prev = last;
        }

       /*
        * Add the extra instructions to the count in the method header
        */
        ((MethodHeader)ir).addToInstructionCount(extras);

       /*
        * Return the new IR
        */
        return ir;
    }

 }
