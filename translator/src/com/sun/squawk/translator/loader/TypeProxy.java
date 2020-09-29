
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.*;
import  com.sun.squawk.translator.util.*;

public class TypeProxy extends Type {

    private Type proxy;

   /**
    * Static constructor only called from the VirtualMachine.java
    */
    public static TypeProxy createForMap(VirtualMachine vm, String name) {
        return new TypeProxy(vm, name);
    }

    public TypeProxy(VirtualMachine vm, String name) {
        super(vm, name);
    }

    public void setProxy(Type proxy) {
        this.proxy = proxy;
    }

    public Type getProxy() {
        return proxy;
    }
}
