
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.util.*;

public abstract class InstructionVisitor extends BaseFunctions {
    public abstract void doArithmeticOp      (ArithmeticOp       inst);
    public abstract void doArrayLength       (ArrayLength        inst);
    public abstract void doCheckCast         (CheckCast          inst);
    public abstract void doCheckStore        (CheckStore         inst);
    public abstract void doCast              (Cast               inst);
    public abstract void doConvertOp         (ConvertOp          inst);
    public abstract void doGoto              (Goto               inst);
    public abstract void doHandlerEnter      (HandlerEnter       inst);
    public abstract void doHandlerExit       (HandlerExit        inst);
    public abstract void doIfOp              (IfOp               inst);
    public abstract void doInitializeClass   (InitializeClass    inst);
    public abstract void doInstanceOf        (InstanceOf         inst);
    public abstract void doInvoke            (Invoke             inst);
    public abstract void doLoadConstant      (LoadConstant       inst);
    public abstract void doLoadConstantObject(LoadConstantObject inst);
    public abstract void doLoadException     (LoadException      inst);
    public abstract void doLoadField         (LoadField          inst);
    public abstract void doLoadIndexed       (LoadIndexed        inst);
    public abstract void doLoadLocal         (LoadLocal          inst);
    public abstract void doLookupSwitch      (LookupSwitch       inst);
    public abstract void doMethodHeader      (MethodHeader       inst);
    public abstract void doMonitorEnter      (MonitorEnter       inst);
    public abstract void doMonitorExit       (MonitorExit        inst);
    public abstract void doNegateOp          (NegateOp           inst);
    public abstract void doNewArray          (NewArray           inst);
    public abstract void doNewMultiArray     (NewMultiArray      inst);
    public abstract void doNewObject         (NewObject          inst);
    public abstract void doPhi               (Phi                inst);
    public abstract void doReturn            (Return             inst);
    public abstract void doSimpleText        (SimpleText         inst);
    public abstract void doStoreField        (StoreField         inst);
    public abstract void doStoreIndexed      (StoreIndexed       inst);
    public abstract void doStoreLocal        (StoreLocal         inst);
    public abstract void doTableSwitch       (TableSwitch        inst);
    public abstract void doThrow             (Throw              inst);
}




