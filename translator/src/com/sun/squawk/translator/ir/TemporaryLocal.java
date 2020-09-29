
package com.sun.squawk.translator.ir;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class TemporaryLocal extends Local {

    private int referenceCount;

    public TemporaryLocal(int slotType) {
        super(slotType);
    }

    public void incrementReferenceCount() {
        referenceCount++;
    }

    public boolean decrementReferenceCount() {
        return --referenceCount == 0;
    }

    public String toString() {
        return "t"+idstr();
    }
}
