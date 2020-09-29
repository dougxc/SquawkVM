
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.util.*;

public abstract class ParameterVisitor extends BaseFunctions {
    public abstract Instruction doParameter(Instruction inst, Instruction parm);
}
