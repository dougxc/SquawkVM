package com.sun.squawk.translator.loader;
import  com.sun.squawk.util.*;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Stack;

class GraphBuilder extends BaseFunctions implements RuntimeConstants {

   /*
    * The VM for this IR
    */
    private VirtualMachine vm;

   /**
    * Stream from where the bytecodes come
    */
    private BytecodeInputStream in;

   /**
    * The method being processed
    */
    private Method method;

   /**
    * Number of words needed for the javac stack
    */
    private int maxStack;

   /**
    * Number of words needed for the javac locals
    */
    private int maxLocals;

   /**
    * Number of words needed for the javac locals
    */
    private int oneIfStatic;

   /**
    * Stackmap for the method
    */
    private StackMap map;

   /**
    * Pointer to the first instruction
    */
    private Instruction first;

   /**
    * Pointer to the last instruction
    */
    private Instruction prev;

   /**
    * Target for the exception handler used to do the monitorexit for
    * methods with the synchronised attribute.
    */
    private Target finalTarget;

   /**
    * Parameter visitor used to link the instructions to their uses
    */
    //private ParameterVisitor parameterLinker =
    //    new ParameterVisitor() {
    //        public Instruction doParameter(Instruction inst, Instruction parm) {
    //            parm.isUsedBy(inst);
    //            return parm;
    //        }
    //    };

   /**
    * Constructor
    *
    * @param is the input stream for the bytecodes
    * @param m the method for the bytecodes
    */
    static Instruction read(VirtualMachine vm, BytecodeInputStream in, Method method, int maxStack, int maxLocals, StackMap map) throws LinkageException {
        return new GraphBuilder(vm, in, method, maxStack, maxLocals, map).build();
    }

   /**
    * Constructor
    *
    * @param is the input stream for the bytecodes
    * @param m the method for the bytecodes
    */
    public GraphBuilder(VirtualMachine vm, BytecodeInputStream in, Method method, int maxStack, int maxLocals, StackMap map) {
        this.vm          = vm;
        this.in          = in;
        this.method      = method;
        this.map         = map;
        this.maxStack    = maxStack;
        this.oneIfStatic = method.isStatic() ? 1 : 0;
        this.maxLocals   = maxLocals + oneIfStatic;
        initializeState();
    }

   /**
    * Build the graph
    */
    Instruction build() throws LinkageException {
        try {
            Instruction ir = readInstructions();
            String name = method.parent().name()+"::"+method.name();
            if (vm.traceir0(name)) {
                    System.out.println("\n++IR0 trace for "+name);
                    GraphPrinter.print(System.out, ir, vm, name);
                    System.out.println("\n--IR0 trace for "+name+"\n");
            }
            return ir;
        } catch (IOException ioe) {
//ioe.printStackTrace();
            in.classFormatException("badly formed method: " + method.parent().name()+"::"+method.name());
        }
        return null;
    }

   /**
    * Default instruction canonicalizer
    * @param inst the instruction to reduced
    * @return the reduced instruction
    */
    Instruction canonicalize(Instruction inst) {
        return inst;
    }

   /**
    * Append an instruction to the list
    * @param inst the instruction(s) to append
    */
    Instruction append(Instruction inst, boolean constrainsStack) {
        inst = canonicalize(inst);
        if (inst != null) {
            if (first == null) {
                first = inst;
            } else {
                prev.setNext(inst);
            }
            inst.setContext(in); // line number etc.
          //  inst.visit(parameterLinker);
            prev = inst;

            if (constrainsStack) {
                if (firstInvokeInMethod == null) {
                    firstInvokeInMethod = inst;
                }
                for (int javacIndex = 0; javacIndex != maxLocals; ++javacIndex) {
                    Type type = locals[javacIndex];
                    if (type == Type.BOGUS) {
                        if (invokeCausingLocalToBeCleared[javacIndex] == null) {
                            invokeCausingLocalToBeCleared[javacIndex] = inst;
                        }
                    }
                }
            }

        }
        return prev;
    }
    Instruction append(Instruction inst) {
        return append(inst, false);
    }

   /**
    * Append an instruction and then push it
    */
    void appendPush(Instruction inst, boolean constrainsStack) {
        push(append(inst, constrainsStack));
    }
    void appendPush(Instruction inst) {
        appendPush(inst, false);
    }

   /**
    * Read all the instructions for the method
    * @return a list of instructions
    */
    Instruction readInstructions() throws IOException, LinkageException {

       /*
        * Setup initial state
        */
        prev = first = null;
        setControlFlow(true);

       /*
        * Check that this method does not try to implement a superclass's
        * final method,
        */

        if (!method.isStatic()) {
            Type superType = method.parent().superType();
            while (superType != Type.UNIVERSE) {
                Method superMethod = superType.findMethod(method.name(), method.getParms());
                if (superMethod == null) {
                    break;
                }
                if (superMethod.isFinal()) {
                    in.verificationException(VE_FINAL_METHOD_OVERRIDE);
                }
                superType = superMethod.parent().superType();
            }
        }

       /*
        * If this is a static method called main() then insert code to call the class initializser
        */
        if (method.isStatic() && method.name().equals("main")) {
            op_initializeClassPrim(method.parent());
        }

       /*
        * If this is a synchronized method then start an exception handler
        * followed by a monitorenter.
        */
        if (method.isSynchronized()) {
            finalTarget = StackMap.getFinalTarget();
            op_handlerstart(finalTarget);
            methodMonitorEnter();
        }

       /*
        * Read through all the bytecodes
        */
        while (!in.atEof()) {

           /*
            * Print the trace if requested
            */
            printState();

           /*
            * Get the next bytecode
            */
            int code = in.readBytecode("bcode");

           /*
            * If it is not possible to flow into this instruction then there
            * must be a stackmap entry for this ip address.
            */
            if (flow == false && (code < opc_branchtarget)) { // code < opc_branchtarget/exceptiontarget/handlerstart/end,
                in.verificationException(VE_SEQ_BAD_TYPE);
            }

           /*
            * Record the current flow state
            */
            currentFlow = flow;

           /*
            * Assume that this instruction will flow into the next one
            */
            setControlFlow(true);

           /*
            * Check that if an exception were to occur on the following bytecode
            * that all the active exception handlers have compatible stackmaps.
            * However don't do this for the pretend instructions.
            */
            if (code < opc_branchtarget) {
                checkExceptionMaps();
            }

           /*
            * Dispatch to the specific bytecode
            */
            switch (code) {
                case opc_nop:             op_nop();                                                   break;
                case opc_aconst_null:     op_constant(LoadConstant.createNull());                     break;
                case opc_iconst_m1:       op_constant(LoadConstant.create(-1));                       break;
                case opc_iconst_0:        op_constant(LoadConstant.create(0));                        break;
                case opc_iconst_1:        op_constant(LoadConstant.create(1));                        break;
                case opc_iconst_2:        op_constant(LoadConstant.create(2));                        break;
                case opc_iconst_3:        op_constant(LoadConstant.create(3));                        break;
                case opc_iconst_4:        op_constant(LoadConstant.create(4));                        break;
                case opc_iconst_5:        op_constant(LoadConstant.create(5));                        break;
                case opc_lconst_0:        op_constant(LoadConstant.create(0L));                       break;
                case opc_lconst_1:        op_constant(LoadConstant.create(1L));                       break;
                case opc_fconst_0:        op_constant(LoadConstant.create((float)0));                 break;
                case opc_fconst_1:        op_constant(LoadConstant.create((float)1));                 break;
                case opc_fconst_2:        op_constant(LoadConstant.create((float)2));                 break;
                case opc_dconst_0:        op_constant(LoadConstant.create((double)0));                break;
                case opc_dconst_1:        op_constant(LoadConstant.create((double)1));                break;
                case opc_bipush:          op_constant(LoadConstant.create(in.readByte()));            break;
                case opc_sipush:          op_constant(LoadConstant.create(in.readShort()));           break;
                case opc_ldc:             op_constant(in.readLdc()   );                               break;
                case opc_ldc_w:           op_constant(in.readLdc_w() );                               break;
                case opc_ldc2_w:          op_constant(in.readLdc2_w());                               break;
                case opc_iload:           op_load(Type.INT,    in.readSlot1(1));                      break;
                case opc_lload:           op_load(Type.LONG,   in.readSlot1(2));                      break;
                case opc_fload:           op_load(Type.FLOAT,  in.readSlot1(1));                      break;
                case opc_dload:           op_load(Type.DOUBLE, in.readSlot1(2));                      break;
                case opc_aload:           op_load(Type.OBJECT, in.readSlot1(1));                      break;
                case opc_iload_0:         op_load(Type.INT,    0);                                    break;
                case opc_iload_1:         op_load(Type.INT,    1);                                    break;
                case opc_iload_2:         op_load(Type.INT,    2);                                    break;
                case opc_iload_3:         op_load(Type.INT,    3);                                    break;
                case opc_lload_0:         op_load(Type.LONG,   0);                                    break;
                case opc_lload_1:         op_load(Type.LONG,   1);                                    break;
                case opc_lload_2:         op_load(Type.LONG,   2);                                    break;
                case opc_lload_3:         op_load(Type.LONG,   3);                                    break;
                case opc_fload_0:         op_load(Type.FLOAT,  0);                                    break;
                case opc_fload_1:         op_load(Type.FLOAT,  1);                                    break;
                case opc_fload_2:         op_load(Type.FLOAT,  2);                                    break;
                case opc_fload_3:         op_load(Type.FLOAT,  3);                                    break;
                case opc_dload_0:         op_load(Type.DOUBLE, 0);                                    break;
                case opc_dload_1:         op_load(Type.DOUBLE, 1);                                    break;
                case opc_dload_2:         op_load(Type.DOUBLE, 2);                                    break;
                case opc_dload_3:         op_load(Type.DOUBLE, 3);                                    break;
                case opc_aload_0:         op_load(Type.OBJECT, 0);                                    break;
                case opc_aload_1:         op_load(Type.OBJECT, 1);                                    break;
                case opc_aload_2:         op_load(Type.OBJECT, 2);                                    break;
                case opc_aload_3:         op_load(Type.OBJECT, 3);                                    break;
                case opc_iaload:          op_aload(Type.INT_ARRAY   );                                break;
                case opc_laload:          op_aload(Type.LONG_ARRAY  );                                break;
                case opc_faload:          op_aload(Type.FLOAT_ARRAY );                                break;
                case opc_daload:          op_aload(Type.DOUBLE_ARRAY);                                break;
                case opc_aaload:          op_aload(Type.OBJECT_ARRAY);                                break;
                case opc_baload:          op_aload(Type.BYTE_ARRAY  );                                break;
                case opc_caload:          op_aload(Type.CHAR_ARRAY  );                                break;
                case opc_saload:          op_aload(Type.SHORT_ARRAY );                                break;
                case opc_istore:          op_store(Type.INT,    in.readSlot1(1));                     break;
                case opc_lstore:          op_store(Type.LONG,   in.readSlot1(2));                     break;
                case opc_fstore:          op_store(Type.FLOAT,  in.readSlot1(1));                     break;
                case opc_dstore:          op_store(Type.DOUBLE, in.readSlot1(2));                     break;
                case opc_astore:          op_store(Type.OBJECT, in.readSlot1(1));                     break;
                case opc_istore_0:        op_store(Type.INT,    0);                                   break;
                case opc_istore_1:        op_store(Type.INT,    1);                                   break;
                case opc_istore_2:        op_store(Type.INT,    2);                                   break;
                case opc_istore_3:        op_store(Type.INT,    3);                                   break;
                case opc_lstore_0:        op_store(Type.LONG,   0);                                   break;
                case opc_lstore_1:        op_store(Type.LONG,   1);                                   break;
                case opc_lstore_2:        op_store(Type.LONG,   2);                                   break;
                case opc_lstore_3:        op_store(Type.LONG,   3);                                   break;
                case opc_fstore_0:        op_store(Type.FLOAT,  0);                                   break;
                case opc_fstore_1:        op_store(Type.FLOAT,  1);                                   break;
                case opc_fstore_2:        op_store(Type.FLOAT,  2);                                   break;
                case opc_fstore_3:        op_store(Type.FLOAT,  3);                                   break;
                case opc_dstore_0:        op_store(Type.DOUBLE, 0);                                   break;
                case opc_dstore_1:        op_store(Type.DOUBLE, 1);                                   break;
                case opc_dstore_2:        op_store(Type.DOUBLE, 2);                                   break;
                case opc_dstore_3:        op_store(Type.DOUBLE, 3);                                   break;
                case opc_astore_0:        op_store(Type.OBJECT, 0);                                   break;
                case opc_astore_1:        op_store(Type.OBJECT, 1);                                   break;
                case opc_astore_2:        op_store(Type.OBJECT, 2);                                   break;
                case opc_astore_3:        op_store(Type.OBJECT, 3);                                   break;
                case opc_iastore:         op_astore(Type.INT_ARRAY   );                               break;
                case opc_lastore:         op_astore(Type.LONG_ARRAY  );                               break;
                case opc_fastore:         op_astore(Type.FLOAT_ARRAY );                               break;
                case opc_dastore:         op_astore(Type.DOUBLE_ARRAY);                               break;
                case opc_aastore:         op_astore(Type.OBJECT_ARRAY);                               break;
                case opc_bastore:         op_astore(Type.BYTE_ARRAY  );                               break;
                case opc_castore:         op_astore(Type.CHAR_ARRAY  );                               break;
                case opc_sastore:         op_astore(Type.SHORT_ARRAY );                               break;
                case opc_pop:             op_pop();                                                   break;
                case opc_pop2:            op_pop2();                                                  break;
                case opc_dup :            op_dup();                                                   break;
                case opc_dup_x1:          op_dup_x1();                                                break;
                case opc_dup_x2:          op_dup_x2();                                                break;
                case opc_dup2:            op_dup2();                                                  break;
                case opc_dup2_x1:         op_dup2_x1();                                               break;
                case opc_dup2_x2:         op_dup2_x2();                                               break;
                case opc_swap:            op_swap();                                                  break;
                case opc_iadd:            op_arithmetic(Type.INT,    OP_ADD);                         break;
                case opc_ladd:            op_arithmetic(Type.LONG,   OP_ADD);                         break;
                case opc_fadd:            op_arithmetic(Type.FLOAT,  OP_ADD);                         break;
                case opc_dadd:            op_arithmetic(Type.DOUBLE, OP_ADD);                         break;
                case opc_isub:            op_arithmetic(Type.INT,    OP_SUB);                         break;
                case opc_lsub:            op_arithmetic(Type.LONG,   OP_SUB);                         break;
                case opc_fsub:            op_arithmetic(Type.FLOAT,  OP_SUB);                         break;
                case opc_dsub:            op_arithmetic(Type.DOUBLE, OP_SUB);                         break;
                case opc_imul:            op_arithmetic(Type.INT,    OP_MUL);                         break;
                case opc_lmul:            op_arithmetic(Type.LONG,   OP_MUL);                         break;
                case opc_fmul:            op_arithmetic(Type.FLOAT,  OP_MUL);                         break;
                case opc_dmul:            op_arithmetic(Type.DOUBLE, OP_MUL);                         break;
                case opc_idiv:            op_arithmetic(Type.INT,    OP_DIV);                         break;
                case opc_ldiv:            op_arithmetic(Type.LONG,   OP_DIV);                         break;
                case opc_fdiv:            op_arithmetic(Type.FLOAT,  OP_DIV);                         break;
                case opc_ddiv:            op_arithmetic(Type.DOUBLE, OP_DIV);                         break;
                case opc_irem:            op_arithmetic(Type.INT,    OP_REM);                         break;
                case opc_lrem:            op_arithmetic(Type.LONG,   OP_REM);                         break;
                case opc_frem:            op_arithmetic(Type.FLOAT,  OP_REM);                         break;
                case opc_drem:            op_arithmetic(Type.DOUBLE, OP_REM);                         break;
                case opc_ineg:            op_negate(Type.INT);                                        break;
                case opc_lneg:            op_negate(Type.LONG);                                       break;
                case opc_fneg:            op_negate(Type.FLOAT);                                      break;
                case opc_dneg:            op_negate(Type.DOUBLE);                                     break;
                case opc_ishl:            op_shift(Type.INT,  OP_SHL);                                break;
                case opc_lshl:            op_shift(Type.LONG, OP_SHL);                                break;
                case opc_ishr:            op_shift(Type.INT,  OP_SHR);                                break;
                case opc_lshr:            op_shift(Type.LONG, OP_SHR);                                break;
                case opc_iushr:           op_shift(Type.INT,  OP_USHR);                               break;
                case opc_lushr:           op_shift(Type.LONG, OP_USHR);                               break;
                case opc_iand:            op_arithmetic(Type.INT,  OP_AND);                           break;
                case opc_land:            op_arithmetic(Type.LONG, OP_AND);                           break;
                case opc_ior:             op_arithmetic(Type.INT,  OP_OR);                            break;
                case opc_lor:             op_arithmetic(Type.LONG, OP_OR);                            break;
                case opc_ixor:            op_arithmetic(Type.INT,  OP_XOR);                           break;
                case opc_lxor:            op_arithmetic(Type.LONG, OP_XOR);                           break;
                case opc_iinc:            op_iinc(in.readSlot1(1), in.readByte());                    break;
                case opc_i2l:             op_convert(Type.INT   , Type.LONG  , OP_I2L);               break;
                case opc_i2f:             op_convert(Type.INT   , Type.FLOAT , OP_I2F);               break;
                case opc_i2d:             op_convert(Type.INT   , Type.DOUBLE, OP_I2D);               break;
                case opc_l2i:             op_convert(Type.LONG  , Type.INT   , OP_L2I);               break;
                case opc_l2f:             op_convert(Type.LONG  , Type.FLOAT , OP_L2F);               break;
                case opc_l2d:             op_convert(Type.LONG  , Type.DOUBLE, OP_L2D);               break;
                case opc_f2i:             op_convert(Type.FLOAT , Type.INT   , OP_F2I);               break;
                case opc_f2l:             op_convert(Type.FLOAT , Type.LONG  , OP_F2L);               break;
                case opc_f2d:             op_convert(Type.FLOAT , Type.DOUBLE, OP_F2D);               break;
                case opc_d2i:             op_convert(Type.DOUBLE, Type.INT   , OP_D2I);               break;
                case opc_d2l:             op_convert(Type.DOUBLE, Type.LONG  , OP_D2L);               break;
                case opc_d2f:             op_convert(Type.DOUBLE, Type.FLOAT , OP_D2F);               break;
                case opc_i2b:             op_convert(Type.INT   , Type.BYTE  , OP_I2B);               break;
                case opc_i2c:             op_convert(Type.INT   , Type.CHAR  , OP_I2C);               break;
                case opc_i2s:             op_convert(Type.INT   , Type.SHORT , OP_I2S);               break;
                case opc_lcmp:            op_compare(Type.LONG,   OP_LCMP);                           break;
                case opc_fcmpl:           op_compare(Type.FLOAT,  OP_FCMPL);                          break;
                case opc_fcmpg:           op_compare(Type.FLOAT,  OP_FCMPG);                          break;
                case opc_dcmpl:           op_compare(Type.DOUBLE, OP_DCMPL);                          break;
                case opc_dcmpg:           op_compare(Type.DOUBLE, OP_DCMPG);                          break;
                case opc_ifeq:            op_if(   Type.INT,    OP_EQ, in.readTarget2());             break;
                case opc_ifne:            op_if(   Type.INT,    OP_NE, in.readTarget2());             break;
                case opc_iflt:            op_if(   Type.INT,    OP_LT, in.readTarget2());             break;
                case opc_ifge:            op_if(   Type.INT,    OP_GE, in.readTarget2());             break;
                case opc_ifgt:            op_if(   Type.INT,    OP_GT, in.readTarget2());             break;
                case opc_ifle:            op_if(   Type.INT,    OP_LE, in.readTarget2());             break;
                case opc_if_icmpeq:       op_ifcmp(Type.INT,    OP_EQ, in.readTarget2());             break;
                case opc_if_icmpne:       op_ifcmp(Type.INT,    OP_NE, in.readTarget2());             break;
                case opc_if_icmplt:       op_ifcmp(Type.INT,    OP_LT, in.readTarget2());             break;
                case opc_if_icmpge:       op_ifcmp(Type.INT,    OP_GE, in.readTarget2());             break;
                case opc_if_icmpgt:       op_ifcmp(Type.INT,    OP_GT, in.readTarget2());             break;
                case opc_if_icmple:       op_ifcmp(Type.INT,    OP_LE, in.readTarget2());             break;
                case opc_if_acmpeq:       op_ifcmp(Type.OBJECT, OP_EQ, in.readTarget2());             break;
                case opc_if_acmpne:       op_ifcmp(Type.OBJECT, OP_NE, in.readTarget2());             break;
                case opc_goto:            op_goto(in.readTarget2());                                  break;
                case opc_jsr:             op_error();                                                 break;
                case opc_ret:             op_error();                                                 break;
                case opc_tableswitch:     op_tableswitch();                                           break;
                case opc_lookupswitch:    op_lookupswitch();                                          break;
                case opc_ireturn:         op_return(Type.INT);                                        break;
                case opc_lreturn:         op_return(Type.LONG);                                       break;
                case opc_freturn:         op_return(Type.FLOAT);                                      break;
                case opc_dreturn:         op_return(Type.DOUBLE);                                     break;
                case opc_areturn:         op_return(method.type());                                   break;
                case opc_return:          op_return();                                                break;
                case opc_getstatic:       op_getstatic(in.readField());                               break;
                case opc_putstatic:       op_putstatic(in.readField());                               break;
                case opc_getfield:        op_getfield(in.readField());                                break;
                case opc_putfield:        op_putfield(in.readField());                                break;
                case opc_invokevirtual:   op_invokevirtual(in.readMethod());                          break;
                case opc_invokespecial:   op_invokespecial(in.readMethod());                          break;
                case opc_invokestatic:    op_invokestatic(in.readMethod());                           break;
                case opc_invokeinterface: op_invokeinterface(in.readMethod(), in.readUnsignedShort());break;
                case opc_xxxunusedxxx:    op_error();                                                 break;
                case opc_new:             op_new(in.readNewType());                                   break;
                case opc_newarray:        op_newarray(in.readNewArrayType());                         break;
                case opc_anewarray:       op_anewarray(in.readType());                                break;
                case opc_arraylength:     op_arraylength();                                           break;
                case opc_athrow:          op_athrow();                                                break;
                case opc_checkcast:       op_checkcast(in.readType());                                break;
                case opc_instanceof:      op_instanceof(in.readType());                               break;
                case opc_monitorenter:    op_monitorenter();                                          break;
                case opc_monitorexit:     op_monitorexit();                                           break;
                case opc_wide:            op_wide(in.readUnsignedByte());                             break;
                case opc_multianewarray:  op_multianewarray(in.readType(), in.readUnsignedByte());    break;
                case opc_ifnull:          op_if(Type.OBJECT, OP_EQ, in.readTarget2());                break;
                case opc_ifnonnull:       op_if(Type.OBJECT, OP_NE, in.readTarget2());                break;
                case opc_goto_w:          op_goto(in.readTarget4());                                  break;
                case opc_jsr_w:           op_error();                                                 break;
                case opc_breakpoint:      op_error();                                                 break;
                case opc_branchtarget:    op_branchtarget(in.readTarget());                           break;
                case opc_exceptiontarget: op_exceptiontarget(in.readTarget());                        break;
                case opc_handlerstart:    op_handlerstart(in.readPoint());                            break;
                case opc_handlerend:      op_handlerend(in.readPoint());                              break;
                default:                  op_error();                                                 break;
            }
        }

       /*
        * If this is a synchronized method then place the handler
        * here that will release the monitor if an exception is thrown
        */
        if (finalTarget != null) {
            op_handlerend(finalTarget);
            op_exceptiontarget(finalTarget);
            methodMonitorExit();
            op_athrow();
        }

        markLocalsThatNeedClearing();

        return first;
    }

    private void markLocalsThatNeedClearing() {
        for (int javacIndex = 0; javacIndex != locals.length; ++javacIndex) {
            Instruction cause = invokeCausingLocalToBeCleared[javacIndex];
            if (cause != null) {
                Local local = (Local)localhash.get(Type.BASIC_OOP | javacIndex);
                if (local != null) {
                    local.setCauseForClearing(cause);
                }
            }
        }
    }

   /**
    * Process a bad bytecode
    */
    void op_error() throws LinkageException {
        in.verificationException(VE_BAD_INSTR);
    }



   /**
    * methodMonitorEnter
    */
    void methodMonitorEnter() throws LinkageException {
        if (method.isStatic()) {
            op_constant(LoadConstant.createType(method.parent()));
        } else {
            op_loadAbsolute(Type.OBJECT, 0);
        }
        op_monitorenter();
    }


   /**
    * methodMonitorExit
    */
    void methodMonitorExit() throws LinkageException {
        if (method.isStatic()) {
            op_constant(LoadConstant.createType(method.parent()));
        } else {
            op_loadAbsolute(Type.OBJECT, 0);
        }
        op_monitorexit();
    }


   /**
    * Read a wide instruction
    * @return an instruction
    */
    void op_wide(int code) throws LinkageException {
        try {
            switch (code) {
                case opc_iinc               : op_iinc(in.readSlot2(1), in.readShort());                   break;
                case opc_iload              : op_load(Type.INT,    in.readSlot2(1));                      break;
                case opc_lload              : op_load(Type.LONG,   in.readSlot2(2));                      break;
                case opc_fload              : op_load(Type.FLOAT,  in.readSlot2(1));                      break;
                case opc_dload              : op_load(Type.DOUBLE, in.readSlot2(2));                      break;
                case opc_aload              : op_load(Type.OBJECT, in.readSlot2(1));                      break;
                case opc_istore             : op_store(Type.INT,    in.readSlot2(1));                     break;
                case opc_lstore             : op_store(Type.LONG,   in.readSlot2(2));                     break;
                case opc_fstore             : op_store(Type.FLOAT,  in.readSlot2(1));                     break;
                case opc_dstore             : op_store(Type.DOUBLE, in.readSlot2(2));                     break;
                case opc_astore             : op_store(Type.OBJECT, in.readSlot2(1));                     break;
                default                     : op_error();                                                 break;
            }
        } catch (IOException ioe) {
//ioe.printStackTrace();
            in.verificationException("bad wide instruction");
        }
    }

   /**
    * Print the state of the stack and local vars
    */
    void printState() {
        if (!in.trace()) {
            return;
        }
        in.trace("\n\n--------------------------------------------------------\n");
        if (locals.length > 0) {
            in.trace("        local("+(locals.length)+") ");
            for(int i = 0 ; i < locals.length ; i++) {
                in.trace(" "+locals[i]);
            }
            in.trace("\n");
        }
        if (jsp > 0) {
            in.trace("        stack("+jsp+") ");
            for(int i = 0 ; i < jsp ; i++) {
                in.trace(" "+stack[i]);
            }
            in.trace("\n");
        }
        in.trace(    "--------------------------------------------------------\n\n");
    }


   /**
    * Convert an iinc instruction into a load, add, and store
    * @param slot the local variable slot number
    * @param value the value to be added to the slot
    */
    void op_iinc(int slot, int value) throws LinkageException {
        op_load(Type.INT, slot);
        op_constant(LoadConstant.create(value));
        op_arithmetic(Type.INT, OP_ADD);
        op_store(Type.INT, slot);
    }


   /**
    * Nop - do nothing
    */
    void op_nop() throws LinkageException {
    }

   /**
    * Pop item from stack,
    */
    void op_pop() throws LinkageException {
        popCategory1();
    }

   /**
    * Pop two words
    */
    void op_pop2() throws LinkageException {
        Instruction x1 = popCategory2();
        if(!x1.isTwoWords()) {
            x1 = popCategory2();
            assume(!x1.isTwoWords());
        }
    }

   /**
    * Duplicate a word
    */
    void op_dup() throws LinkageException {
        Instruction x1 = popForDupCategory1();
        pushForDup(x1);
        pushForDup(x1);
        x1.isDuped();
    }

   /**
    * Duplicate four words
    */
    void op_dup2() throws LinkageException {
        Instruction x1 = popForDupCategory1or2();
        if(x1.isTwoWords()) {
            pushForDup(x1);
            pushForDup(x1);
            x1.isDuped();
        } else {
            Instruction x2 = popForDupCategory1();
            pushForDup(x2);
            pushForDup(x1);
            pushForDup(x2);
            pushForDup(x1);
            x1.isDuped();
            x2.isDuped();
        }
    }

   /**
    * Duplicate ...
    */
    void op_dup_x1() throws LinkageException {
        Instruction x1 = popForDupCategory1();
        Instruction x2 = popForDupCategory1();
        pushForDup(x1);
        pushForDup(x2);
        pushForDup(x1);
        x1.isDuped();
    }

   /**
    * Duplicate ...
    */
    void op_dup_x2() throws LinkageException {
        Instruction x1 = popForDupCategory1();
        Instruction x2 = popForDupCategory1or2();
        if(x1.isTwoWords()) {
            pushForDup(x1);
            pushForDup(x2);
            pushForDup(x1);
            x1.isDuped();
        } else {
            Instruction x3 = popForDupCategory1();
            pushForDup(x1);
            pushForDup(x3);
            pushForDup(x2);
            pushForDup(x1);
            x1.isDuped();
        }
    }

   /**
    * Duplicate ...
    */
    void op_dup2_x1() throws LinkageException {
        Instruction x1 = popForDupCategory1or2();
        if(x1.isTwoWords()) {
            Instruction x2 = popForDupCategory1();
            pushForDup(x1);
            pushForDup(x2);
            pushForDup(x1);
            x1.isDuped();
        } else {
            Instruction x2 = popForDupCategory1();
            Instruction x3 = popForDupCategory1();
            pushForDup(x2);
            pushForDup(x1);
            pushForDup(x3);
            pushForDup(x2);
            pushForDup(x1);
            x1.isDuped();
            x2.isDuped();
        }
    }

   /**
    * Duplicate ...
    */
    void op_dup2_x2() throws LinkageException {
        Instruction x1 = popForDupCategory1or2();
        if(x1.isTwoWords()) {
            Instruction x2 = popForDupCategory1or2();
            if(x2.isTwoWords()) {
                pushForDup(x1);
                pushForDup(x2);
                pushForDup(x1);
                x1.isDuped();
            } else {
                Instruction x3 = popForDupCategory1();
                pushForDup(x1);
                pushForDup(x3);
                pushForDup(x2);
                pushForDup(x1);
                x1.isDuped();
            }
        } else {
            Instruction x2 = popForDupCategory1();
            Instruction x3 = popForDupCategory1or2();
            if(x3.isTwoWords()) {
                pushForDup(x2);
                pushForDup(x1);
                pushForDup(x3);
                pushForDup(x2);
                pushForDup(x1);
                x1.isDuped();
                x2.isDuped();
            } else {
                Instruction x4 = popForDupCategory1();
                pushForDup(x2);
                pushForDup(x1);
                pushForDup(x4);
                pushForDup(x3);
                pushForDup(x2);
                pushForDup(x1);
                x1.isDuped();
                x2.isDuped();
            }
        }
    }

   /**
    * Swap top two words
    */
    void op_swap() throws LinkageException {
        Instruction x1 = popForDupCategory1();
        Instruction x2 = popForDupCategory1();
        pushForDup(x1);
        pushForDup(x2);
    }

   /**
    * Push a constant value
    */
    void op_constant(Instruction inst) throws LinkageException {
        appendPush(inst);
    }

   /**
    * Allocate a load local instruction adjusting the slot offset for static methods
    *
    */
    void op_load(Type basicType, int javacIndex) throws LinkageException {
        assume(oneIfStatic == (method.isStatic() ? 1 : 0));
        op_loadAbsolute(basicType, javacIndex + oneIfStatic);
    }

   /**
    * Allocate a load local instruction
    *
    */
    void op_loadAbsolute(Type basicType, int javacIndex) throws LinkageException {
        Type actualType = getLocal(basicType, javacIndex);
        Local local = allocLocal(basicType, javacIndex);
        appendPush(LoadLocal.create(local, actualType));
    }


   /**
    * Allocate a store local instruction
    *
    */
    void op_store(Type basicType, int javacIndex) throws LinkageException {
        assume(oneIfStatic == (method.isStatic() ? 1 : 0));
        javacIndex += oneIfStatic;
        Instruction parm = pop(basicType);
        setLocal(parm.type(), javacIndex);
        if (javacIndex == 0) {
            local0instruction = parm;
        }
        Local local = allocLocal(basicType, javacIndex);
        append(StoreLocal.create(local, parm));
    }

   /**
    * Allocate an array load instruction
    */
    void op_aload(Type type) throws LinkageException {
        Instruction index = pop(Type.INT);
        Instruction array;
        if (type == Type.BYTE_ARRAY) {
           /*
            * opc_baload is used to access byte arrays and boolean arrays
            * check that the receiver type is one of these and set the result
            * to Type.INT in both cases.
            */
            array = pop(Type.OBJECT);
            if (array.type() != Type.BYTE_ARRAY && array.type() != Type.BOOLEAN_ARRAY) {
                in.verificationException(VE_BALOAD_BAD_TYPE);
            }
        } else {
            array = pop(type);
        }

        appendPush(LoadIndexed.create(array, index, type));
    }

   /**
    * Allocate an array store instruction
    */
    void op_astore(Type type) throws LinkageException {
        Instruction value = pop(type.elementType());
        Instruction index = pop(Type.INT);
        Instruction array;
        if (type == Type.BYTE_ARRAY) {
           /*
            * opc_baload is used to access byte arrays and boolean arrays
            * check that the receiver type is one of these and set the result
            * to Type.INT in both cases.
            */
            array = pop(Type.OBJECT);
            if (array.type() != Type.BYTE_ARRAY && array.type() != Type.BOOLEAN_ARRAY) {
                in.verificationException(VE_BALOAD_BAD_TYPE);
            }
        } else {
            array = pop(type);
        }

        boolean isObjectArray = (array.type() == Type.NULLOBJECT);
        if (array.type().dimensions() == 1 && !value.type().isArray()) {
            /* Success (Don't check now, do it at runtime) */
        } else if (isObjectArray || value.type().vIsAssignableTo(array.type().elementType())) {
            /* Success */
        } else {
            in.verificationException(VE_AASTORE_BAD_TYPE);
        }
        append(StoreIndexed.create(array, index, value, type), isObjectArray);
    }


   /**
    * Allocate an arithmetic instruction
    */
    void op_arithmetic(Type type, int op) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        appendPush(ArithmeticOp.create(op, p1, p2));
    }

   /**
    * Allocate an compare instruction
    */
    void op_compare(Type type, int op) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        appendPush(ArithmeticOp.createCmp(op, p1, p2));
    }

   /**
    * Allocate a shift instruction
    */
    void op_shift(Type type, int op) throws LinkageException {
        Instruction p2 = pop(Type.INT);
        Instruction p1 = pop(type);
        appendPush(ArithmeticOp.create(op, p1, p2));
    }

   /**
    * Allocate a negate instruction
    */
    void op_negate(Type type) throws LinkageException {
        Instruction p1 = pop(type);
        appendPush(NegateOp.create(p1));
    }

   /**
    * Allocate a type convertion instruction
    */
    void op_convert(Type typeFrom, Type typeTo, int op) throws LinkageException {
        Instruction p1 = pop(typeFrom);
        appendPush(ConvertOp.create(op, typeTo, p1));
    }

   /**
    * Allocate an if-zero instruction
    */
    void op_if(Type type, int op, Target target) throws LinkageException {
        if (type == Type.OBJECT) {
            op_constant(LoadConstant.createNull());
        } else {
            op_constant(LoadConstant.create(0));
        }
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        Instruction result = checkJump(IfOp.create(op, p1, p2, target));
        append(result);
    }

   /**
    * Allocate an if-cmp instruction
    */
    void op_ifcmp(Type type, int op, Target target) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        Instruction result = checkJump(IfOp.create(op, p1, p2, target));
        append(result);
    }

   /**
    * Allocate a goto instruction
    */
    void op_goto(Target target) throws LinkageException {
        Instruction result = checkJump(Goto.create(target));
        endBasicBlock(result, false);
        append(result);
    }

   /**
    * Allocate a tableswitch instruction
    */
    void op_tableswitch() throws LinkageException {
        try {
            Instruction key = pop(Type.INT);
            in.roundToCellBoundry();
            Target defaultTarget =  checkTarget(in.readTarget4());
            int low  = in.readInt();
            int high = in.readInt();
            TableSwitch tableSwitch = TableSwitch.create(key, low, high, defaultTarget);
            for (int i = low ; i <= high ; i++) {
                Target target = checkTarget(in.readTarget4());
                tableSwitch.addTarget(i, target);
            }
            endBasicBlock(tableSwitch, false);
            append(tableSwitch);
        } catch (IOException ioe) {
//ioe.printStackTrace();
            in.verificationException("bad tableswitch instruction");
        }
    }

   /**
    * Allocate a lookupswitch instruction
    */
    void op_lookupswitch() throws LinkageException {
        try {
            int currentIP = in.getLastIP();
            Instruction key = pop(Type.INT);
            in.roundToCellBoundry();
            Target defaultTarget =  checkTarget(in.readTarget4());
            int npairs  = in.readInt();
            int lastMatch = -1;
            LookupSwitch lookupSwitch = LookupSwitch.create(key, npairs, defaultTarget);
            for (int i = 0 ; i < npairs ; i++) {
                int match = in.readInt();
                if (i == 0) {
                    lastMatch = match;
                }
                Target target = checkTarget(in.readTarget4());
                if (match < lastMatch) {
                    in.verificationException(VE_BAD_LOOKUPSWITCH);
                }
                lookupSwitch.addTarget(i, match, target);
                lastMatch = match;
            }
            endBasicBlock(lookupSwitch, false);
            append(lookupSwitch);
        } catch (IOException ioe) {
//ioe.printStackTrace();
            in.verificationException("bad lookupswitch instruction");
        }
    }

   /**
    * Allocate an instanceof instruction
    */
    void op_instanceof(Type type) throws LinkageException {
        Instruction p1 = pop(Type.OBJECT);
        appendPush(InstanceOf.create(type, p1), true);
    }

   /**
    * Allocate a checkcast instruction
    */
    void op_checkcast(Type type) throws LinkageException {
        Instruction p1 = pop(Type.OBJECT);
        appendPush(CheckCast.create(type, p1), true);
    }

   /**
    * Allocate a monitorenter instruction
    */
    void op_monitorenter() throws LinkageException {
        Instruction p1 = pop(Type.OBJECT);
        append(MonitorEnter.create(p1), true);
    }

   /**
    * Allocate a monitorexit instruction
    */
    void op_monitorexit() throws LinkageException {
        Instruction p1 = pop(Type.OBJECT);
        append(MonitorExit.create(p1), true);
    }


   /**
    * Allocate initialize class instruction
    */
    void op_initializeClass(Type type) throws LinkageException {
        // need to test if type is loaded because isAssignableTo fails with proxy types
/////////// ** OLD **  if (type.isLoaded() && method.parent().vIsAssignableTo(type) && !type.isInterface()) {
        if (type.isLoaded() && method.parent().vIsAssignableTo(type)) {
            return;
        }
        if (type instanceof TypeProxy) {
            return;
        }
        op_initializeClassPrim(type);
    }

   /**
    * Allocate initialize class instruction
    */
    void op_initializeClassPrim(Type type) throws LinkageException {
        append(InitializeClass.create(type), true);
    }



   /**
    * Allocate a new instruction
    */
    void op_new(Type type) throws LinkageException {
        op_initializeClass(type); ///////////////////////////////////////////////////////---- Not needed now clinit is called in Class.newInstance()
        appendPush(NewObject.create(type), true);
    }

   /**
    * Allocate a newarray instruction
    */
    void op_newarray(Type type) throws LinkageException {
        Instruction size = pop(Type.INT);
        appendPush(NewArray.create(type, size), true);
    }

   /**
    * Allocate an anewarray instruction
    */
    void op_anewarray(Type type) throws LinkageException {
        Instruction size = pop(Type.INT);
        op_initializeClass(type.asArray()); ///////////////////////////////////////////////////////---- Not needed now clinit is called in Class.newInstance()
        appendPush(NewArray.create(type.asArray(), size), true);
    }

   /**
    * Allocate a multianewarray instruction
    */
    void op_multianewarray(Type type, int dims) throws LinkageException {
        if (dims == 0 || dims > type.dimensions()) {
            in.verificationException(VE_MULTIANEWARRAY, "dims="+dims+" type="+type.dimensions());
        }
        Instruction[] dimList = new Instruction[dims];
        int k = dims;
        for (int i = dims-1 ; i >= 0 ; --i) {
            dimList[i] = pop(Type.INT);
        }
        NewMultiArray array = (NewMultiArray)NewMultiArray.create(type, dimList);
        appendPush(array, true);
    }

   /**
    * Allocate an arraylength instruction
    */
    void op_arraylength() throws LinkageException {
        Instruction array = pop(Type.OBJECT);
        if (array.type() != Type.NULLOBJECT && !array.type().isArray()) {
            in.verificationException(VE_EXPECT_ARRAY);
        }
        appendPush(ArrayLength.create(array));
    }

   /**
    * Getstatic
    */
    void op_getstatic(Field field) throws LinkageException {
        op_initializeClass(field.parent());
        appendPush(LoadField.create(field, null), true);          // push value
    }

   /**
    * Putstatic
    */
    void op_putstatic(Field field) throws LinkageException {
       /*
        * javac 1.4 produces putstatics for constants. These are removed by
        * StoreField.create() If they are then the clinit is not needed.
        */
        Instruction sf = StoreField.create(field, null, pop(field.type()));
        if (sf != null) {
            op_initializeClass(field.parent());
            append(sf, true);
        }
    }

   /**
    * Getfield
    */
    void op_getfield(Field field) throws LinkageException {
        Instruction ref;

       /*
        * If the field is protected and in a different package then the receiver must
        * be compatible with the type of the method being verified
        */
        if (field.isProtected() && !method.parent().inSamePackageAs(field.parent())) {
            ref = pop(method.parent());                 // objectref (must be same kind as method being verified)
        } else {
            ref = pop(field.parent());                  // objectref
        }
        appendPush(LoadField.create(field, ref));       // push value
    }

   /**
    * Putfield
    */
    void op_putfield(Field field) throws LinkageException {
        Instruction ref, value;

        value = pop(field.type());                      // pop value
       /*
        * If the field is protected and in a different package then the receiver must
        * be compatible with the type of the method being verified
        */
        if (field.isProtected() && !method.parent().inSamePackageAs(field.parent())) {
            ref = pop(method.parent());                 // objectref (must be same kind as method being verified)
        } else {
            ref = pop(field.parent());                  // objectref
        }
        append(StoreField.create(field, ref, value));
    }


   /**
    * Rename the <init> "this" type to a specified receiver tyoe
    */
    void renameInitTo(Type aType) {
        for (int i = 0 ; i < locals.length ; i++) {
            if (locals[i] == Type.INITOBJECT) {
                locals[i] = aType;
            }
        }
        for (int i = 0 ; i < jsp ; i++) {
            if (stack[i].type() == Type.INITOBJECT) {
                stack[i].changeType(aType);
            }
        }
    }


   /**
    * Invoke virtual
    */
    void op_invokevirtual(Method callee) throws LinkageException {
        if (callee.name().charAt(0) == '<') {
            in.verificationException(VE_EXPECT_INVOKESPECIAL);
        }
        op_invoke(callee, null, false, Invoke.VIRTUAL, false);
    }

   /**
    * Invoke interface
    */
    void op_invokeinterface(Method callee, int garbage) throws LinkageException {
        op_invokevirtual(callee); // same as above
    }

   /**
    * Invoke special
    */
    void op_invokespecial(Method callee) throws LinkageException {
        Type parent = method.parent();
        boolean inHierarchy = parent.vIsAssignableTo(callee.parent());
        boolean isInvokeInit = (callee.name() == VirtualMachine.SQUAWK_INIT);

        if (isInvokeInit) {
           /*
            * Setup the receiver type to INITOBJECT if this is an <init> method
            */
            renameInitTo(method.parent());
        } else {
           /*
            * This is not a call to <init> so the callee must be somewhere
            * in superclass hierarchy
            */
            if (!inHierarchy) {
                in.verificationException(VE_INVOKESPECIAL);
            }
        }

       /*
        * If this function just returns remember this for later
        */
        boolean ignore = callee.isJustReturn();

       /*
        * Look for a vtable entry in the class vtable for this method. If there is not one
        * there then a new method entry will be created so that a regular invoke can be used
        * to call the super class method
        */
        //if (inHierarchy && callee.parent() != parent && !ignore) {
        //    //callee = parent.createProxyFor(callee);
        //    op_invoke(callee, null, isInvokeInit, Invoke.SUPER, ignore);
        //} else {
        //    op_invoke(callee, null, isInvokeInit, Invoke.VIRTUAL, ignore);
        //}
        op_invoke(callee, null, isInvokeInit, Invoke.SPECIAL, ignore);
    }

   /**
    * Invoke static
    */
    void op_invokestatic(Method callee) throws LinkageException {
        if (callee.name().charAt(0) == '<') {
            in.verificationException(VE_EXPECT_INVOKESPECIAL);
        }
        op_initializeClass(callee.parent());
        op_invoke(callee, append(LoadConstant.createType(callee.parent())), false, Invoke.STATIC, callee.isJustReturn());
    }

   /**
    * Invoke
    */
    void op_invoke(Method callee, Instruction staticNull, boolean isInvokeInit, int invokeType, boolean ignore) throws LinkageException {

        boolean isInvokeStatic = (staticNull != null);
        Type[] parmTypes = callee.getParms();
        int start = 1;
        int end   = parmTypes.length - 1;
        int nparms = end - start + 1 + 1; //(isInvokeStatic ? 0 : 1);

       /*
        * Trace
        */
        in.traceln("        Calling "+callee.name()+" isInvokeStatic = "+isInvokeStatic);
        in.trace( "        types("+nparms+")");
        for (int i = end ; i >= start ; --i) {
            in.trace(" "+parmTypes[i]);
        }
        if (!isInvokeStatic) {
            in.trace(" "+parmTypes[0]);
        }
        in.traceln(" res = "+callee.type());

       /*
        * Pop arguments
        */
        Instruction[] parms = new Instruction[nparms];

        int k = nparms;
        for (int i = end ; i >= start ; --i) {
            parms[--k] = pop(parmTypes[i]);
        }

       /*
        * If this is an invokestatic then add a null receiver to the start of the parameters
        *
        * If this is not an invokestatic then there are a number of things
        * that need to be done to the receiver
        */
        if (isInvokeStatic) {
            parms[--k] = staticNull;
        } else {
           /*
            * Pop the receiver. Its type is checked later
            */
            Instruction receiver = pop(Type.OBJECT);
            parms[--k] = receiver;

           /*
            * Special processing for calls to <init>
            */
            if (isInvokeInit) {
               /*
                * If the receiver was the result of a 'new' then the type may
                * (or may not) be a type proxy (an ITEM_NewObject in the stackmap).
                * If so the the type returned from the 'new' is concerted into
                * the type the proxy represented.
                */
                if (receiver instanceof NewObject) {
                    Type proxy = receiver.type();
                    if (proxy instanceof TypeProxy) {
                        receiver.changeType(((TypeProxy)proxy).getProxy());
                    }
                } else {
                   /*
                    * Calls to <init> that were not as the result of a 'new' must be this()
                    * or super() calls from one constructor method to another.
                    */
                    if (receiver.type() != method.parent() && receiver.type() != method.parent().superType()) {
                        in.verificationException(VE_BAD_INIT_CALL);
                    }
                }
            }

           /*
            * Having changed the TypeProxys back, now check the receiver type was okay
            */
            Type receiverType;

           /*
            * If the method is protected and in a different package then the receiver must
            * be compatible with the type of the method being verified.
            */
            if (callee.isProtected() && !method.parent().inSamePackageAs(callee.parent())) {
                receiverType = method.parent();
            } else {
                receiverType = callee.parent();
            }

           /*
            * Check that the receiver is  of the correct type
            */
            if (!receiver.type().vIsAssignableTo(receiverType)) {
                String msg = ""+receiver.type()+" is not kind of "+receiverType;
                in.verificationException(VE_STACK_BAD_TYPE, msg);
            }
        }

       /*
        * Link the instruction in
        */
        if (callee.type() == Type.VOID) {
            append(Invoke.create(callee, parms, invokeType, ignore), true);
        } else {
            appendPush(Invoke.create(callee, parms, invokeType, ignore), true);
        }
    }

   /**
    * Allocate a return void instruction
    */
    void op_return() throws LinkageException {
        if (method.type() != Type.VOID) {
            in.verificationException(VE_EXPECT_RETVAL);
        }
        if (receiverIsUninitalizedNew()) {
            in.verificationException(VE_RETURN_UNINIT_THIS);
        }
        if (finalTarget != null) {     // i.e. if this is a synchronized method
            methodMonitorExit();
        }
        Instruction result = Return.create();
        endBasicBlock(result, false);
        append(result);
    }

   /**
    * Allocate a return with value instruction
    */
    void op_return(Type type) throws LinkageException {
        Instruction inst = pop(type);
        if (finalTarget != null) {     // i.e. if this is a synchronized method
            methodMonitorExit();
        }
        Instruction result = Return.create(inst);
        endBasicBlock(result, false);
        append(result);
    }

   /**
    * Allocate a throw instruction
    */
    void op_athrow() throws LinkageException {
        Instruction inst = pop(Type.THROWABLE);
        Instruction result = Throw.create(inst);
        endBasicBlock(result, false);
        append(result);
    }

   /**
    * Allocate a branch target instruction
    */
    void op_branchtarget(Target target) throws LinkageException {
        Instruction parm2 = null;
        Local local2 = null;
        Type widest = null;
        Instruction[] phiParameters = target.getPhiParameters();

        if (target.isExceptionTarget()) {
            in.verificationException("Branchtarget is also exception target" );
        }

        Type[] mapLocals = target.getPhysicalLocals();
        assume(mapLocals.length <= locals.length, "mapLocals="+mapLocals.length+" locals="+locals.length);

//        if (mapLocals.length > 0) {

/*
            assume(mapLocals.length <= locals.length);
prt("old ");
            for (int i = 0 ; i < locals.length ; i++) {
prt( "locals["+i+"]="+  locals[i]+" ");
            }
prtn("");
*/


//prt("new ");
         //   for (int i = 0 ; i < mapLocals.length ; i++) {
         //       if (mapLocals[i] != Type.INITOBJECT) { // TEMP
         //           locals[i] = mapLocals[i];
         //       }
//prt( "locals["+i+"]="+  locals[i]+" ");
         //   }
//prtn("");
//        }

//prtn("jsp="+jsp+" target= "+target.getSP());

        if (jsp != target.getSP()) {
            Instruction parm1;
           /*
            * Check the current assumption is that there can only ever be
            * at most a single value on the stack between basic blocks.
            */
            if (jsp != target.getSP()+1) {
                in.verificationException("Phi stack difference > 1" );
            }

           /*
            * The stack has an extra element because the code just preceeding this
            * has jumped to a phi. Pop off the instruction and add it to the list
            * of parameters for that target. This will be retrieved when a phi is
            * allocated for that target.
            *
            * Presumablly this technique it totally dependant on the fact that
            * javac always produces this scenario and that there must be a "proper"
            * way of working out phi joins.
            *
            * Until this type checking is complete, a VerifyError is thrown (which
            * will prevent at least one TCK test from passing)
            */
            if (!(prev instanceof Goto)) {
                in.verificationException("Type checking incomplete: prev="+prev+" jsp="+jsp+" targetsp="+target.getSP());
            }

            Goto lastgoto = (Goto)prev ;
            parm1 = pop(Type.UNIVERSE);
            lastgoto.target().addPhiParameter(parm1);
        }

        checkBranchTarget(target);

        if (phiParameters != null) {

           /*
            * This branch target is called from more than one place where there is a data
            * element on the stack. The reguster allocated for this item needs to be
            * the same. The follwoing code does this by changing the temporoary register
            * that was used by the instructions calling this point to the same one.
            */

           /*
            * First add the value for the expression immeadiately before this to the list
            */
            parm2 = pop(Type.UNIVERSE);
            target.addPhiParameter(parm2);
            phiParameters = target.getPhiParameters(); // get the version with the parm2

           /*
            * Get the local allocated to the above instruction. This is the one
            * that is going to be used for all the other intructions that mearged
            * to this point
            */
            local2 = parm2.getResultLocal();
            assume(local2 instanceof TemporaryLocal);


           /* --- The following is not needed! The stack maps tell what the TOS type must be --- */


           /*
            * Establish the type of this phi. This will be the widest of all the
            * types flowing into this point so that verification will fail if any
            * of tham are invalid.
            */
            widest = phiParameters[0].type(); // Starting assumption

           /*
            * Run through the other instructions widening
            */
            for (int i = 1 ; i < phiParameters.length ; i++) {
                Instruction parm1 = phiParameters[i];
                if (parm1.type().vIsAssignableTo(widest)) {
                    // widest = widest;
                } else if (widest.vIsAssignableTo(parm1.type())) {
                    widest = parm1.type();
                } else {
                    in.verificationException("Phi type mismatch" );
                }

               /*
                * Reassign this iteration's instruction's temporary local to the one we chose
                * that to be common one.
                */
                Local local1 = parm1.getResultLocal();
                assume(local1 instanceof TemporaryLocal);
                parm1.setResultLocal(local2);
            }
        }

       /*
        * Allocalte the IR
        */
        Instruction result = Phi.create(target, widest, phiParameters);
        result = append(result);

        if (widest != null) {
           /*
            * The way freed temporary locals kept in a stack should ensure
            * that we end up with local2 here which is how the two inputs
            * are were set to write into. The phi just has to have this as
            * its result and the push insturction should assign this.
            */
            push(result);
            assume(result.getResultLocal() == local2);
        }
    }

   /**
    * Allocate an exception target instruction
    */
    void op_exceptiontarget(Target target) throws LinkageException {
        if (jsp != 0 || target.getSP() != 1 || !target.isExceptionTarget()) {
            in.verificationException("Bad exception target stack" );
        }
        Instruction result = LoadException.create(target);
        appendPush(result);
    }


   /*
    * Listhead for all the currently active exception handlers
    */
    private Target currentHandlers;

   /**
    * Allocate an exception target instruction
    */
    void op_handlerstart(Target target) throws LinkageException {
        target.setNextTarget(currentHandlers);
        currentHandlers = target;
        append(HandlerEnter.create(target));
        setControlFlow(currentFlow);
    }

   /**
    * Allocate an exception target instruction
    */
    void op_handlerend(Target target) throws LinkageException {
        Target last = null;
        Target next = currentHandlers;
        while (next != null) {
            if (next == target) {
                if (last == null) {
                    currentHandlers = target.getNextTarget();
                } else {
                    last.setNextTarget(target.getNextTarget());
                }

               /*
                * Found it
                */
                append(HandlerExit.create(target));
                setControlFlow(currentFlow);
                return;
            }
            last = next;
            next = next.getNextTarget();
        }
        in.verificationException("Missing handler end");
    }


   /*
    * Check that the current verifier state is compable with all the exception
    * habdlers
    */
    void checkExceptionMaps() throws LinkageException {
        Target next = currentHandlers;
        while (next != null) {
            matchStackMapLocals(next, VE_TARGET_BAD_TYPE);
            next = next.getNextTarget();
        }
    }





   /*************************************************************************\
    *                     Type checking and slot assignment                 *
   \*************************************************************************/


   /*
    * The following is set to false where there is no direct control
    * flow from current instruction to the next instruction
    * in sequence.
    */
    private boolean flow = true;

   /*
    * The flow state at the beginging of the current instruction
    */
    private boolean currentFlow = true;

   /**
    * Pointer to the "current" stackmap entry
    */
    private int currentStackMapIndex = 0;

   /**
    * Stack of instructions that emulate the Java stack except
    * that is contains logical items not physical ones (i.e. long
    * and double take only one entry). This is a stack of Instructions.
    */
    private Instruction[] stack;
    private int jsp = 0;

   /**
    * Vector that emulates the local variables of a method.
    * This is an array of physical items (long and double take
    * two entries). This is an array of Types.
    */
    private Type[] locals;

    private Instruction[] invokeCausingLocalToBeCleared;

    private Instruction firstInvokeInMethod;

    private int nextLocalID = 0;


   /*
    * Three stacks where temporaary local variable structures are stored when
    * they are not being used.
    */
    private Stack tempOopList  = new Stack();
    private Stack tempIntList  = new Stack();
    private Stack tempLongList = new Stack();

    private int nextTempID = 0;

   /**
    * The last instruction that wrote to local 0
    */
    Instruction local0instruction = null;

   /**
    * Hashtable that translates javac slot numbers into Squawk locals
    */
    private IntHashtable localhash = new IntHashtable();

    private void endBasicBlock(Instruction delim, boolean fallsThrough) {
        setControlFlow(fallsThrough);
    }

   /**
    * Initialise things
    */
    private void initializeState() {
       /*
        * Print header
        */
        printMethodInfo1();

       /*
        * It is not known how many logical Java stack entries are used
        * so allocate the number of physical words which is known.
        *
        * One more entry is needed in order for op_if to push a const(0)
        * Two more entries are needed in order for op_iinc to work
        */
        stack = new Instruction[maxStack+2];

       /*
        * Allocate the locals
        */
        locals = method.getPhysicalLocalsCopy(maxLocals);
        invokeCausingLocalToBeCleared = new Instruction[locals.length];

       /*
        * Setup the receiver type to INITOBJECT if this is an <init> method
        * Otherwise set it to the receiver type.
        */
        if (method.name().equals(VirtualMachine.SQUAWK_INIT)) {
            locals[0] = Type.INITOBJECT;
        } else {
            locals[0] = method.parent();
        }

       /*
        * Iterate through the method parameters and assign the logical
        * parameter offset to the local variable that is allocated for
        * the physical offset for that parameter type. This is done in
        * order to know which local variables were parameters and what
        * logical offset they are when the CFG is build.
        */
        Type[] parms = method.getParms();
        int k = 0;
//prtn("parms.length="+parms.length);
        for (int i = 0 ; i < parms.length ; i++) {
//prtn("type["+i+"] = "+parms[i]);
            Local local = allocLocal(parms[i], k);
            local.setParameterNumber(i);
            if (parms[i].slotType() != Type.BASIC_LONG) {
                k++;
            } else {
                k += 2;
            }
        }
       /*
        * Print header
        */
        printMethodInfo2();
    }



   /**
    * Print the method information
    */
    void printMethodInfo1() {
        in.trace("\n\n========================================================\n");
        in.trace(    "               "+method.parent().name()+"::"+method.name());
    }

    void printMethodInfo2() {
        in.trace("     s="+maxStack+" l="+locals.length);
        in.trace(    "   isStatic ="+method.isStatic());
        in.trace(  "\n========================================================\n\n");
    }




   /**
    * Set control flow variable
    *
    * @param flow true if the next instructon can be flowed into
    */
    void setControlFlow(boolean flow) {
        this.flow = flow;
    }


   /**
    * Push a type onto the execution stack
    *
    * @param inst the instruction with the type
    * @return the same instruction
    */
    Instruction push(Instruction inst, boolean alloc) {
        in.traceln("        push "+inst);
        assume(inst != null);
        stack[jsp++] = inst;
        if (alloc) {
            inst.setResultLocal(allocTemporaryLocal(inst.type()));
        } else {
            ((TemporaryLocal)inst.getResultLocal()).incrementReferenceCount();
        }
        return inst;
    }


   /**
    * Push a type onto the execution stack
    *
    * @param inst the instruction with the type
    * @return the same instruction
    */
    void push(Instruction inst) {
        push(inst, true);
    }



   /**
    * Push a type onto the execution stack
    *
    * @param inst the instruction with the type
    * @return the same instruction
    */
    void pushForDup(Instruction inst) {
        push(inst, false);
    }



   /**
    * Pop a type from the execution stack
    *
    * @param type the type that should result
    * @param free TRUE if the instruction's local should be freed
    * @return the same instruction on popped from the stack
    */
    Instruction pop(Type type, boolean free) throws LinkageException {
        assume(type != null);
        if (jsp <= 0) {
            in.verificationException("popping from empty stack");
        }

        Instruction inst = stack[--jsp];
        in.traceln("        pop "+inst);

        if (type == Type.BOOLEAN || type == Type.BYTE || type == Type.SHORT || type == Type.CHAR) {
            type = Type.INT;
        }

        if (!inst.type().vIsAssignableTo(type)) {
            String msg = ""+inst.type().toString()+" is not kind of "+type.toString();
            in.verificationException(VE_STACK_BAD_TYPE, msg);
        }


       /*
        * The local that was assigned to this instruction must be a
        * TemporaryLocal because no optomisation has yet taken place. One
        * of two things must now take place. If this pop is a normal kind
        * of pop the local must be freed back into the pool of temporary
        * locals. If it is a pop for a dup then instead of being freed
        * it only has its reference count decremented.
        */
        TemporaryLocal local = (TemporaryLocal)inst.getResultLocal();
        if (free) {
            freeTemporaryLocal(local);
        } else {
            local.decrementReferenceCount();
        }
        return inst;
    }


   /**
    * Pop a type from the execution stack
    *
    * @param type the type that should result
    * @return the same instruction on popped from the stack
    */
    Instruction pop(Type type) throws LinkageException {
        return pop(type, true);
    }


   /**
    * Pop a type from the execution stack pryor toa dup
    * This is the same as pop except the local is not freed
    *
    * @param type the type that should result
    * @return the same instruction on popped from the stack
    */
    Instruction popForDup(Type type) throws LinkageException {
        return pop(type, false);
    }


    // The variable "flow" is true if it was possible to 'fall' into the instruction from the one above.
    // It is always true except after a return/switch/throw or goto instruction.

   /**
    * Check that the 'goto' or 'if' it pointing to a valid target
    */
    Instruction checkJump(Goto gotoinst) throws LinkageException {
        Target target = gotoinst.target();
        checkTarget(target);
       /*
        * If the target has a target instuction then the target must be a backward branch.
        * In this case there is some kind of loop from the current location to the
        * target. This is the place where the usecount of local variables is multiplied
        * by ten.
        */
        if (vm.optimizationLevel() > 0) {
            int callerIP = in.getLastIP();
            Instruction inst = target.getTargetInstruction();
            if (inst != null) {
                assume(inst.getIP() <= callerIP, "target="+inst.getIP()+" caller="+callerIP);
                while (inst != null && inst.getIP() <= callerIP) {
                    inst.incrementLoopDepth();
                    inst = inst.getNext();
                }
                gotoinst.incrementLoopDepth();
            }
        }

        return gotoinst;
    }

   /**
    * Check that a branch target is valid
    */
    void checkBranchTarget(Target target) throws LinkageException {
        matchStackMap(target, true, VE_TARGET_BAD_TYPE);
    }

   /**
    * Check that a handler target is valid
    */
    void checkHandlerTarget(Target target) throws LinkageException {
        matchStackMap(target, false, VE_TARGET_BAD_TYPE);
    }

   /**
    * Check that the target is not an exception target
    */
    Target checkTarget(Target target) throws LinkageException {
        assume(!target.isExceptionTarget());
        int callerIP = in.getLastIP();
        matchStackMap(target, false, VE_TARGET_BAD_TYPE);
        checkNewObject(callerIP, target.getIP());

       /*
        * If the target has a target instuction then the target must be a backward branch.
        * In this case there is some kind of loop from the current location to the
        * target. This is the place where the usecount of local variables is multiplied
        * by ten.
        */
/* -- better done in checkJump()
        if (vm.optimizationLevel() > 0) {
            Instruction inst = target.getTargetInstruction();
            if (inst != null) {
                assume(inst.getIP() <= callerIP, "target="+inst.getIP()+" caller="+callerIP);
                while (inst != null && inst.getIP() <= callerIP) {
                    inst.incrementLoopDepth();
                    inst = inst.getNext();
                }
            }
        }
*/
        return target;
    }

   /**
    * Check to see if a stackmap matches the current state
    */
    void matchStackMap(Target target, boolean replaceWithTarget, int throwCode) throws LinkageException {

        Type[] mapLocals = target.getPhysicalLocals();
        Type[] mapStack  = target.getStack();
        int i;

       /*
        * Trace the locals
        */
        if (DEBUG && mapLocals.length > 0) {
            in.trace("     maplocal("+(mapLocals.length)+") ");
            for(i = 0 ; i < mapLocals.length ; i++) {
                in.trace(" "+mapLocals[i]);
            }
            in.trace("\n");
        }

       /*
        * Trace the stack items
        */
        if (DEBUG && mapStack.length > 0) {
            in.trace("     mapstack("+mapStack.length+") ");
            for(i = 0 ; i < mapStack.length ; i++) {
                in.trace(" "+mapStack[i]);
            }
            in.trace("\n");
        }

       /*
        * Fail if the map has more
        */
        if (mapLocals.length > locals.length) {
            in.verificationException(throwCode);
        }

       /*
        * Check the locals
        */
        for (i = 0 ; i < mapLocals.length ; i++) {
            Type mapLocal = mapLocals[i];
            if (mapLocal.isInterface()) {
                mapLocal = Type.OBJECT; // Change interfaces to java.lang.Object
            }
            if (currentFlow && !locals[i].vIsAssignableTo(mapLocal)) {
                in.verificationException(throwCode, ""+locals[i]+" not assignable to "+mapLocal);
            }
            if (replaceWithTarget) {
                locals[i] = mapLocal;
            }
        }

       /*
        * Make any remining bogus if replacing
        */
        if (replaceWithTarget) {
            for ( ; i < locals.length ; i++) {
                locals[i] = Type.BOGUS;
            }
        }

       /*
        * Fail if the map sp is different
        */
        if (mapStack.length != jsp) {
            in.verificationException(throwCode);
        }

       /*
        * Check the stack items
        */
        for (i = 0 ; i < mapStack.length ; i++) {
            Type mapStk = mapStack[i];
            if (currentFlow && !stack[i].type().vIsAssignableTo(mapStk)) {
                in.verificationException(throwCode,  ""+stack[i].type()+" not assignable to "+mapStk);
            }

            if (replaceWithTarget) {
                mapStack[i] = mapStk;
            }
        }
    }

   /*
    * Check the locals only
    */
    void matchStackMapLocals(Target target, int throwCode) throws LinkageException {

        Type[] mapLocals = target.getPhysicalLocals();

       /*
        * Trace the locals
        */
        if (DEBUG && locals.length > 0) {
            in.trace("     maplocal("+(mapLocals.length)+") ");
            for(int i = 0 ; i < mapLocals.length ; i++) {
                in.trace(" "+mapLocals[i]);
            }
            in.trace("\n");
        }

       /*
        * Fail if the map has more
        */
        if (mapLocals.length > locals.length) {
            in.verificationException(throwCode);
        }

       /*
        * Check the locals
        */
        for (int i = 0 ; i < mapLocals.length ; i++) {
            Type mapLocal = mapLocals[i];
            if (!locals[i].vIsAssignableTo(mapLocal)) {
                in.verificationException(throwCode, ""+locals[i]+" not assignable to "+mapLocal);
            }
        }
    }


   /**
    * Check that there are no uninitialized new objects in the current state
    */
    void checkNewObject(int targetIP, int currentIP) throws LinkageException {
        if (targetIP < currentIP) {
            for (int i = 0; i < locals.length; i++) {
                if (locals[i] == Type.NEWOBJECT) {
                    in.verificationException(VE_BACK_BRANCH_UNINIT);
                }
            }
            for (int i = 0; i < jsp; i++) {
                if (stack[i].isUninitalizedNew()) {
                    in.verificationException(VE_BACK_BRANCH_UNINIT);
                }
            }
        }
    }





    Instruction popCategory1() throws LinkageException {
        return pop(Type.UNIVERSE); //temp
    }

    Instruction popCategory2() throws LinkageException {
        return pop(Type.UNIVERSE); //temp
    }

    Instruction popForDupCategory1() throws LinkageException {
        return popForDup(Type.UNIVERSE); //temp
    }

    Instruction popForDupCategory1or2() throws LinkageException {
        return popForDup(Type.UNIVERSE); //temp
    }



    boolean receiverIsUninitalizedNew() {
        if (!method.isStatic() && local0instruction != null && local0instruction.isUninitalizedNew()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Validate a local variable index for a given type.
     */
     void checkLocalIndex(Type actualType, int javacIndex) throws LinkageException {
        int extra = (actualType.isDouble() || actualType.isLong() ? 1 : 0);
        if (javacIndex < 0 || (javacIndex + extra) >= locals.length) {
            in.verificationException("invalid local index: locals="+locals.length+" javacIndex="+javacIndex);
        }

     }

   /**
    * Set the type of a local variable
    *
    * @param type the type
    * @param javacIndex the slot Javac assigned
    */
    void setLocal(Type actualType, int javacIndex) throws LinkageException {
        checkLocalIndex(actualType, javacIndex);
        locals[javacIndex] = actualType;
        if (actualType.isLong()) {
            locals[javacIndex+1] = Type.LONG2;
        } else if (actualType.isDouble()) {
            locals[javacIndex+1] = Type.DOUBLE2;
        }
    }

   /**
    * Get the type of a local variable
    *
    * @param type the type
    * @param javacIndex the slot Javac assigned
    * @return the actual type
    */
    Type getLocal(Type basicType, int javacIndex) throws LinkageException {
        checkLocalIndex(basicType, javacIndex);
        Type result = locals[javacIndex];
//prtn("        Slot["+ javacIndex+"]="+result+" basicType="+basicType);
        if (!result.vIsAssignableTo(basicType)) {
            in.verificationException(VE_LOCALS_BAD_TYPE);
        }
        if (basicType.isLong()) {
            if (locals[javacIndex+1] != Type.LONG2) {
                in.verificationException(VE_LOCALS_BAD_TYPE);
            }
        } else if (basicType.isDouble()) {
            if (locals[javacIndex+1] != Type.DOUBLE2) {
                in.verificationException(VE_LOCALS_BAD_TYPE);
            }
        }
        return result;
    }

   /**
    * Translate the Javac slot number to the Squawk local.
    * A squawk local can only be used for an oop, a one cell data
    * type, or a two cell data type. This function assigns them
    * as required.
    *
    * @param type the type
    * @param javacIndex the slot Javac assigned
    * @return a Local
    */

    Local allocLocal(Type basicType, int javacIndex) {
        Local result = (Local)localhash.get(basicType.slotType()+javacIndex);
        if (result == null) {
            result = new Local(basicType.slotType());
            result.setID(nextLocalID++);
            localhash.put(basicType.slotType()+javacIndex, result);
        } else {
            assume(result.slotType() == basicType.slotType());
        }
        return result;
    }


   /**
    * Allocate a temporary local
    *
    * @param type the type the local will need to be for
    * @return a TemporaryLocal
    */
    TemporaryLocal allocTemporaryLocal(Type basicType) {
        Stack tempStack = null;
        switch (basicType.slotType()) {
            case Type.BASIC_OOP:  tempStack = tempOopList; break;
            case Type.BASIC_INT:  tempStack = tempIntList; break;
            case Type.BASIC_LONG: tempStack = tempLongList; break;
            default: shouldNotReachHere();
        }
        TemporaryLocal tempLocal;
        if (tempStack.isEmpty()) {
            tempLocal = new TemporaryLocal(basicType.slotType());
            tempLocal.setID(nextTempID++);
        } else {
            tempLocal = (TemporaryLocal)tempStack.pop();
        }
        //tempLocal.incrementUseCount();
        tempLocal.incrementReferenceCount();

        if (firstInvokeInMethod != null && basicType.slotType() == Type.BASIC_OOP) {
            tempLocal.setCauseForClearing(firstInvokeInMethod);
        }

        return tempLocal;
    }

   /**
    * Free the use of a temporary local
    *
    * @param tempLocal the local
    */
    void freeTemporaryLocal(TemporaryLocal tempLocal) {
        if (tempLocal.decrementReferenceCount()) {
            Stack tempStack = null;
            switch (tempLocal.slotType()) {
                case Type.BASIC_OOP:  tempStack = tempOopList;  break;
                case Type.BASIC_INT:  tempStack = tempIntList;  break;
                case Type.BASIC_LONG: tempStack = tempLongList; break;
                default: shouldNotReachHere();
            }

           /*
            * Add to free list
            */
            tempStack.push(tempLocal);
        }
    }

//   /**
//    * Cancel the use of a temporary local
//    *
//    * @param tempLocal the local
//    */
//    void cancelTemporaryLocal(TemporaryLocal tempLocal) {
//        //tempLocal.decrementUseCount(); // dont count this use
//        freeTemporaryLocal(tempLocal);
//    }


    /*************************************************************************\
     *                         Module initialisation                         *
    \*************************************************************************/

}










/*
Type.UNIVERSE
   Type.VOID
   Type.BOOLEAN         Z
   Type.BYTE            B
   Type.CHAR            C
   Type.SHORT           S
   Type.INT             I
   Type.LONG            J
   Type.FLOAT           F
   Type.DOUBLE          D
   Type.OBJECT          Ljava.lang.Object;
       Type.STRING      Ljava.lang.String;
       Type.FOO         LFoo;
           Type.BAR     LBar;
       Type.ARRAY       A
           Type.BOOLEAN_ARRAY       [Z
           Type.BYTE_ARRAY          [B
           Type.CHAR_ARRAY          [C
           Type.SHORT_ARRAY         [S
           Type.INT_ARRAY           [I
           Type.LONG_ARRAY          [J
           Type.FLOAT_ARRAY         [F
           Type.DOUBLE_ARRAY        [D
           Type.OBJECT_ARRAY        [Ljava.lang.Object;
               Type.STRING_ARRAY    [Ljava.lang.String;
               Type.FOO_ARRAY       [LFoo;
                   Type.BAR_ARRAY   [LBar;
               Type.BOOLEAN_ARRAY2D     [[Z
               Type.BYTE_ARRAY2D        [[B
               Type.CHAR_ARRAY2D        [[C
               Type.SHORT_ARRAY2D       [[S
               Type.INT_ARRAY2D         [[I
               Type.LONG_ARRAY2D        [[J
               Type.FLOAT_ARRAY2D       [[F
               Type.DOUBLE_ARRAY2D      [[D
               Type.OBJECT_ARRAY2D      [[Ljava.lang.Object;
                   Type.STRING_ARRAY2D  [[Ljava.lang.String;
                   Type.FOO_ARRAY2D     [[LFoo;
                       Type.BAR_ARRAY2D [[LBar;
                   Type.BOOLEAN_ARRAY3D     [[[Z
                   Type.BYTE_ARRAY3D        [[[B
                   Type.CHAR_ARRAY3D        [[[C
                   Type.SHORT_ARRAY3D       [[[S
                   Type.INT_ARRAY3D         [[[I
                   Type.LONG_ARRAY3D        [[[J
                   Type.FLOAT_ARRAY3D       [[[F
                   Type.DOUBLE_ARRAY3D      [[[D
                   Type.OBJECT_ARRAY3D      [[[Ljava.lang.Object;
                       Type.STRING_ARRAY3D  [[[Ljava.lang.String;
                       Type.FOO_ARRAY3D     [[[LFoo;
                           Type.BAR_ARRAY3D [[[LBar;




Type.DATA
   Type.OBJECT                                    Ljava.lang.Object;
       Type.OBJECT_ARRAY                          [Ljava.lang.Object;
           Type.OBJECT_ARRAY2D                    [[Ljava.lang.Object;
               Type.OBJECT_ARRAY3D                [[[Ljava.lang.Object;
                   Type.STRING_ARRAY3D            [[[Ljava.lang.String;
                   Type.BOOLEAN_ARRAY4D           [[[[Z



Rules for assignmemt

    Can assign Foo to Bar?

    1, if Foo == Bar ||
          Bar == java.lang.Object ||
          Foo inherets from Bar
          TRUE

    2, If Foo is an interface
          FALSE

    3, If Foo is an array
          FALSE (until we have Clonable/Serilazable)

    4, If Bar is an interface and one of Foo's interfaces inherets from Bar.
          TRUE

    5, else FALSE

or more simply

    1, if Foo == Bar ||
          Bar == java.lang.Object ||
          Foo inherets from Bar ||
          One of Foo's interfaces inherets from Bar.
          TRUE
*/