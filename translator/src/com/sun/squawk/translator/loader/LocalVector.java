
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;

public class LocalVector extends java.util.Vector implements Comparer {

   /**
    * Constructor
    */
    public LocalVector(int size) {
        super(size);
    }

   /**
    * Return the array sorted
    */
    public Object[] sort() {
        BaseFunctions.assume(size() == elementData.length);
        if (size() > 1) {
            QuickSorter.sort(elementData, this);
        }
        return elementData;
    }

    public int compare(Object a, Object b) {
        return ((Local)b).getUseCount() - ((Local)a).getUseCount(); // highest first
    }

}