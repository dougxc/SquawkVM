package com.sun.squawk.translator.util;

public class QuickSorter {

    Object[] elementData;
    Comparer comparer;

   /**
    * Public Constructor
    */
    public static void sort(Object[] elementData, Comparer comparer) {
        new QuickSorter(elementData, elementData.length, comparer);
    }

   /**
    * Public Constructor
    */
    public static void sort(Object[] elementData, int dataLength, Comparer comparer) {
        new QuickSorter(elementData, dataLength, comparer);
    }

   /**
    * Public Constructor
    */
    private QuickSorter(Object[] elementData, int dataLength, Comparer comparer) {
        this.elementData = elementData;
        this.comparer    = comparer;
        if (dataLength > 1) {
            quicksort(0, dataLength - 1);
        }
    }

   /**
    * Quicksort
    */
    private void quicksort(int p, int r) {
        if(p < r) {
            int q = partition(p, r);
            if(q == r) {
                q--;
            }
            quicksort(p, q);
            quicksort(q+1, r);
        }
    }

   /**
    * partition
    */
    private int partition(int lo, int hi) {
        Object pivot = elementData[lo];
        while (true) {
            while(comparer.compare(elementData[hi], pivot) >= 0 && lo < hi) {
                hi--;
            }
            while(comparer.compare(elementData[lo], pivot) <  0 && lo < hi) {
                lo++;
            }
            if(lo < hi) {
                Object T        = elementData[lo];
                elementData[lo] = elementData[hi];
                elementData[hi] = T;
            } else {
                return hi;
            }
        }
    }

}