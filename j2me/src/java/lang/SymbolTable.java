/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

/**
 * The SymbolTable entry class implements an efficient symbol lookup table
 * specialised for keys that are character arrays.
 */
class SymbolTable {

    /**
     * An entry in the symbol table.
     */
    private class SymbolTableEntry {
        int hash;
        char[] key;
        int value;
        SymbolTableEntry next;

        boolean keyEquals(char[] anObject, int offset) {
            int i;
            for (i = 0 ; key[i] != 0; i++, offset++) {
                if (key[i] != anObject[offset]) {
                    return false;
                }
            }
            return anObject[offset] == 0;
        }
    }

    private SymbolTableEntry table[];

    /**
     * Constructor.
     */
    public SymbolTable(int maximumCapacity) {
        this.table = new SymbolTableEntry[maximumCapacity];
    }

    /**
     * For debugging.
     */
    public String keyToString(char[] key) {
        int length = 0;
        while (key[length] != 0)
            length ++;
        return new String(key,0,length);
    }

    /**
     * get
     */
    public int get(char[] key) {
        return get(key,0);
    }
    public int get(char[] key, int offset) {
        SymbolTableEntry tab[] = table;
        int hash = hashCodeOf(key, offset);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (SymbolTableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash == hash) && e.keyEquals(key,offset)) {
                return e.value;
            }
        }
        return 0;
    }

    /**
     * Add a key/value pair to the table.
     */
    public void put(char[] key, int value) {
        if (get(key) != 0) {
            throw new RuntimeException("Key is already in table: "+new String(key));
        }
        int hash = hashCodeOf(key, 0);
        SymbolTableEntry e = new SymbolTableEntry();
        e.hash = hash;
        e.key = key;
        e.value = value;
        e.next = table[hash];
        table[hash] = e;
    }

    /**
     * Add a string/value pair to the symbol table.
     */
    public void put(String name, int value) {
        int count = name.length();
        char key[] = new char[count + 1];
        name.getChars(0, count, key, 0);
        // Add terminating 0
        key[count] = 0;
        put(key, value);
    }

    /**
     * Compute the hashcode of a 0 terminated char array.
     */
    private int hashCodeOf(char[] o, int offset) {
        int hash = 0;
        for (int i = offset ; o[i] != 0; i++) {
            hash = hash*31 + o[i];
        }
        return (hash & 0x7FFFFFFF) % table.length;
    }

    /**
     * Return the first key corresponding to a given value.
     */
    public char[] getKey(int value) {
        for (int i = 0; i != table.length; i++) {
            SymbolTableEntry e = table[i];
            while (e != null) {
                if (e.value == value)
                    return e.key;
                e = e.next;
            }
        }
        return null;
    }
}