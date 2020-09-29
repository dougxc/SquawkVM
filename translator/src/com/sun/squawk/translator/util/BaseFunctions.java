/*
 *  Copyright (c) 2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.translator.util;

/**
 * Basic functions used by the Squawk runtime classes
 *
 * @author  Nik Shaylor
 */
public class BaseFunctions {


    /*-----------------------------------------------------------------------*\
                                    Constants
    \*-----------------------------------------------------------------------*/

    public final static boolean DEBUG = true;

    /*-----------------------------------------------------------------------*\
                                  Public functions
    \*-----------------------------------------------------------------------*/

    /**
     * Stop the VM printing a message
     * @param s message to print
     *
     * This slightly odd routine is intended to be used in one of two ways
     *
     *     fatal("message");
     * and
     *     throw fatal("message");
     *
     * Note than in the second case the throw will never be executed ;-)
     */
    public static RuntimeException fatal(String s) {
        if(s != null) {
            throw new RuntimeException("Fatal -- " + s);
        }
        return fatal("null fatal message?");
    }

    /**
     * Stop the VM because caller got to an illegal place
     * @return nothing important
     */
    public static int shouldNotReachHere() {
        fatal("Should not reach here");
        return 0;
    }

    /**
     * Stop the VM if an assumtion is false
     * @param x boolean value to test
     */
    public static void assume(boolean x, String msg) {
        if (DEBUG && !x) {
            fatal("Assume Failure - "+msg );
        }
    }

    /**
     * Stop the VM if an assumtion is false
     * @param x boolean value to test
     */
    public static void assume(boolean x) {
        if (DEBUG && !x) {
            fatal("Assume Failure");
        }
    }

    /**
     * Output a trace message
     * @param print true if the message should be written
     * @param message true the message to write
     */
    public static void trace(boolean print, String message) {
        if (print) {
            System.out.println(message);
        }
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prt(Object o) {
        System.out.print(o);
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prtn(Object o) {
        System.out.println(o);
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prt(String s) {
        System.out.print(s);
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prtn(String s) {
        System.out.println(s);
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prt(int i) {
        System.out.print(i);
    }

    /**
     * Print a string
     * @param p message to print
     */
    public static void prtn(int i) {
        System.out.println(i);
    }


    public static void prtx(int i) {
        int b = i & 0xFF;
        int hi = b>>4;
        int lo = b&0xF;
        char[] table = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        System.out.print(" "+table[hi]+table[lo]);
    }

    public static String XMLEncodeString(String s) {
        StringBuffer buf = new StringBuffer(s.length() * 2);
        for (int j = 0 ; j < s.length() ; j++) {
            char ch = s.charAt(j);
            if (ch < ' ' || ch >= 0x7F || ch == '<' || ch == '>' || ch == '&' || ch == '"') {
                buf.append("&#");
                buf.append((int)ch);
                buf.append(';');
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

}