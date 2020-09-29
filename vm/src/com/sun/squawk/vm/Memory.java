//J2C:memory.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class Memory extends PlatformAbstraction {

/*-----------------------------------------------------------------------*\
 *                              Constants                                *
\*-----------------------------------------------------------------------*/

/*IFJ*/public final static int ADDRESS_UNITS = 1;
//IFC//#define ADDRESS_UNITS 1

/*IFJ*/public final static boolean HEAP_ADDRESSES_ARE_POSITIVE = true;
//IFC//#ifndef HEAP_ADDRESSES_ARE_POSITIVE
//IFC//#define HEAP_ADDRESSES_ARE_POSITIVE true
//IFC//#endif

/*-----------------------------------------------------------------------*\
 *                               Buffers                                 *
\*-----------------------------------------------------------------------*/

/*IFJ*/private int[] memory;
//IFC//private int *memory;
       private int memorySize;


/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

    /**
     * Setup the pointer to the memory area and its length
     */
    void Memory_init(int mem[], int size) {
        memory = mem;
        memorySize = size;
    }

    /**
     * Return the length of the memory area
     */
    int getMemorySize() {
        return memorySize;
    }


/*-----------------------------------------------------------------------*\
 *                             Memory access                             *
\*-----------------------------------------------------------------------*/


/*IFJ*/String inHex(int i) { return Integer.toHexString(i); }

    /**
     * checkOffset
     */
    int checkOffset(int off) {
//IFC//#ifndef PRODUCTION
        if (off < 0) {
/*IFJ*/     fatalVMError("Negative offset " + off);
//IFC//     fatalVMError("Negative offset");
        }
//IFC//#endif
        return off;
    }

    /**
     * getWord
     */
    int getWord(int addr, int off) {
//IFC//#ifndef PRODUCTION
        if (addr < 0) {
/*IFJ*/     fatalVMError("Bad base address "+inHex(addr));
//IFC//     fatalVMError("Bad base address");
        }
        if (addr+off >= memorySize || addr+off < 0) {
/*IFJ*/     fatalVMError("Bad base+offset address base = "+inHex(addr)+" offset ="+inHex(off));
//IFC//     fatalVMError("Bad base+offset address base");
        }
//IFC//#endif
        return memory[addr+off];
    }

    /**
     * setWord
     */
    void setWord(int addr, int off, int value) {
//IFC//#ifndef PRODUCTION
        if (addr < 0) {
/*IFJ*/     fatalVMError("Bad base address "+inHex(addr));
//IFC//     fatalVMError("Bad base address");
        }
        if (addr+off >= memorySize || addr+off < 0) {
/*IFJ*/     fatalVMError("Bad base+offset address base = "+inHex(addr)+" offset ="+inHex(off));
//IFC//     fatalVMError("Bad base+offset address base");
        }
//IFC//#endif
        memory[addr+off] = value;
    }

    /**
     * getByte
     */
    int getByte(int addr, int off) {
        int word = getWord(addr, checkOffset(off)/4);
        switch (off % 4) {
            case 0: word <<= 24;    break;
            case 1: word <<= 16;    break;
            case 2: word <<= 8;     break;
            case 3: word <<= 0;     break;
        }
        return word >> 24;
    }

    /**
     * getUnsignedByte
     */
    int getUnsignedByte(int addr, int off) {
        int word = getWord(addr, checkOffset(off)/4);
        switch (off % 4) {
            case 0: word >>= 0;     break;
            case 1: word >>= 8;     break;
            case 2: word >>= 16;    break;
            case 3: word >>= 24;    break;
        }
        return word & 0xFF;
    }

    /**
     * getUnsignedHalfFromBytes
     */
    int getUnsignedHalfFromByteArray(int addr, int off) {
        int hi = getUnsignedByte(addr,off  );
        int lo = getUnsignedByte(addr,off+1);
        return ((hi << 8) + lo) & 0xFFFF;
    }

    /**
     * setByte
     */
    void setByte(int addr, int off, int value) {
        int word = getWord(addr, checkOffset(off)/4);
        int mask = 0;
        value &= 0xFF;
        switch (off % 4) {
            case 0: mask = 0xFFFFFF00; value <<= 0;  break;
            case 1: mask = 0xFFFF00FF; value <<= 8;  break;
            case 2: mask = 0xFF00FFFF; value <<= 16; break;
            case 3: mask = 0x00FFFFFF; value <<= 24; break;
        }
        word = (word & mask) | value;
        setWord(addr, off/4, word);
    }

    /**
     * getHalf
     */
    int getHalf(int addr, int off) {
        int word = getWord(addr, checkOffset(off)/2);
        switch (off % 2) {
            case 0: word <<= 16;    break;
            case 1: word <<= 0;     break;
        }
        return word >> 16;
    }

    /**
     * getUnsignedHalf
     */
    int getUnsignedHalf(int addr, int off) {
        int word = getWord(addr, checkOffset(off)/2);
        switch (off % 2) {
            case 0: word >>= 0;     break;
            case 1: word >>= 16;    break;
        }
        return word & 0xFFFF;
    }

    /**
     * setHalf
     */
    void setHalf(int addr, int off, int value) {
        int word = getWord(addr, checkOffset(off)/2);
        int mask = 0;
        value &= 0xFFFF;
        switch (off % 2) {
            case 0: mask = 0xFFFF0000; value <<= 0;  break;
            case 1: mask = 0x0000FFFF; value <<= 16; break;
        }
        word = (word & mask) | value;
        setWord(addr, off/2, word);
    }

    /**
     * getLong
     */
    long getLong(int addr, int off) {
        int longoff = checkOffset(off) * 2;
        int word1 = getWord(addr, longoff);
        int word2 = getWord(addr, longoff+1);
        return (((long)word1) << 32) + word2;
    }

    /**
     * setLong
     */
    void setLong(int addr, int off, long val) {
        int longoff = checkOffset(off) * 2;
        setWord(addr, longoff,   (int)(val >> 32));
        setWord(addr, longoff+1, (int)(val));
    }

    /**
     * copyWords
     */
    void copyWords(int from, int to, int num) {
        int i;
//IFC//#ifndef PRODUCTION
       if (num < 0) {
/*IFJ*/     fatalVMError("Negative range " + num);
//IFJ//     fatalVMError("Negative range");
       }
//IFC//#endif
        for (i = 0 ; i < num ; i++) {
            setWord(to, i, getWord(from, i));
        }
///*IFJ*/ TRACEGC("copyWords from "+ from + " to "+ to +" lth " + num);
    }


    /**
     * getStringAsByteArray
     */
    int getStringAsByteArray(int string, byte buf[], int lth) {
        int value  = getWord(string, STR_value);
        int offset = getWord(string, STR_offset);
        int count  = getWord(string, STR_count);
        int i;
        if ((count+1) > lth) {
            return -1;
        }
        for (i = 0; i < count; i++) {
            buf[i] = (byte)getUnsignedHalf(value, offset + i);
        }
        buf[count] = 0;
        return i;
    }

    /**
     * getStringAsString
     */
/*IFJ*/String getStringAsString(int string) {
/*IFJ*/    int value  = getWord(string, STR_value);
/*IFJ*/    int offset = getWord(string, STR_offset);
/*IFJ*/    int count  = getWord(string, STR_count);
/*IFJ*/    StringBuffer sb = new StringBuffer();
/*IFJ*/    for (int i = 0; i < count; i++) {
/*IFJ*/        sb.append((char)getUnsignedHalf(value, offset + i));
/*IFJ*/    }
/*IFJ*/    return sb.toString();
/*IFJ*/}


/*IFJ*/}

















































