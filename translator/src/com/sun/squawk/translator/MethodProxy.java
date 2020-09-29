
package com.sun.squawk.translator;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;


public class MethodProxy extends Method {

   /**
    * For methods this is the offset into an owning class's methods[] array (the "vtable" offset).
    * For interfaces this is the offset into the system interface table.
    */
    private int offset;

   /**
    * The target method which this class is a proxy for.
    */
    Method target;

   /**
    * Private constructor
    */
    protected MethodProxy(Method target, Type parent, int offset) {
        super(parent, target.name(), target.type(), target.flags(), target.getParms());
        this.target = target;
        this.offset = offset;
    }

   /**
    * load
    */
    public void load() throws LinkageException {
        target.load();
    }

   /**
    * toString
    */
    public String toString() {
        return target.toString();
    }

   /**
    * Set slot
    */
    public void setSlotOffset(int offset) {
        fatal("MethodProxy::setSlotOffset");
    }

   /**
    * Get slot
    */
    public int getSlotOffset() {
        return offset;
    }

   /**
    * Get slot
    */
    public int targetSlotOffset() {
        return target.getSlotOffset();
    }

   /**
    * asIrMethod
    */
    public Method asIrMethod() {
        return this;
    }

   /*
    * Return the list of parm types
    */
    public Type[] getParms() {
        return target.getParms();
    }

   /**
    * getPhysicalLocalsCopy
    */
    public Type[] getPhysicalLocalsCopy(int expectedSize) {
        return target.getPhysicalLocalsCopy(expectedSize);
    }

   /**
    * isProxyFor
    */
    public boolean isProxyFor(Method method) {
        return target == method;
    }

   /**
    * isJustReturn
    */
    public boolean isJustReturn() {
        return target.isJustReturn();
    }

}
