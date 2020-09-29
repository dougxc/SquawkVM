
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.util.*;


/**
 * This is a subclass of Vector that can sort all its entries into order.
 * All the entries must be of type BytecodeAddress, and the vector must
 * be exactly filled before the sort can take place.
 */
public class AddressVector extends java.util.Vector implements Comparer {

   /**
    * Constructor
    */
    public AddressVector(int size) {
        super(size);
    }

   /**
    * Return the array sorted
    */
    public Object[] sorted() {
        BaseFunctions.assume(size() == elementData.length);
        if (size() > 1) {
            QuickSorter.sort(elementData, this);
        }
        return elementData;
    }

    public int compare(Object a, Object b) {
        int res = ((BytecodeAddress)a).sortKey() - ((BytecodeAddress)b).sortKey();
        if (res == 0) {
           res = ((BytecodeAddress)a).subKey() - ((BytecodeAddress)b).subKey();
        }
        return res;

    }

}