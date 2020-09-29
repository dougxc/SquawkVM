package com.sun.squawk.analysis;

import java.io.*;
import java.util.*;

class XInstruction {
    String[] args;
    int size;
    int address;
    XInstruction next;

    XInstruction(String[] args) {
        this.args = args;
    }
}


public class Count2 {

    static InputStreamReader isr;

    static String[] tempargs = new String[500];

    static String[] getLine2() throws Exception {
        StringBuffer sb = new StringBuffer();
        int ch = isr.read();
        if (ch == -1) {
            return null;
        }
        while (ch != '\n' && ch != -1) {
            sb.append((char)ch);
            ch = isr.read();
        }
        String line = sb.toString();

        int argc = 0;
        StringTokenizer st = new StringTokenizer(line, " \t\n\r", false);
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            if (next.length() > 0 && !next.equals(" ")) {
                tempargs[argc++] = next;
            }
        }

        String[] result = new String[argc];
        for (int i = 0 ; i < argc ; i++) {
            result[i] = tempargs[i];
        }
        return result;
    }

    static String[] getLine() throws Exception {
         String[] res = getLine2();
         while (res != null && res.length == 0) {
             res = getLine();
         }
         return res;
    }

    static String percent(int count, int total) {

        String s =  ""+((count*1000)/total);
        while (s.length() < 3) {
            s = "0"+s;
        }

        return ""+
                ((char)s.charAt(0)) +
                ((char)s.charAt(1)) + "." +
                ((char)s.charAt(2));
    }

    static int getNumber(String s) {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException  ex) {
            throw new RuntimeException("Bad Number "+s);
        }
    }

    static int getNumber(String s, int ch) {
        if (s.charAt(0) != ch) {
            throw new RuntimeException("Bad value "+s+ " expected inital"+(char)ch);
        }
        return getNumber(s.substring(1));
    }

    static int getLocal(String s) {
        return getNumber(s, 'l');
    }

    static boolean isLocal(String s) {
        return s.charAt(0) == 'l';
    }

    static int getConst(String s) {
        return getNumber(s, '#');
    }

    static boolean isConst(String s) {
        return s.charAt(0) == '#';
    }

    static int getAddr(String s) {
        return getNumber(s, '$');
    }

    static boolean isAddr(String s) {
        return s.charAt(0) == '$';
    }

    static int getClass(String s) {
        return getNumber(s, '&');
    }

    static boolean isClass(String s) {
        return s.charAt(0) == '&';
    }

    static int getSlot(String s) {
        return getNumber(s, '@');
    }

    static boolean isSlot(String s) {
        return s.charAt(0) == '@';
    }


    static XInstruction[] targets = new XInstruction[10000];


    public static void main(String[] argsXX) throws Exception {
        isr = new InputStreamReader(System.in);
        String[] args = getLine();
        int methods = 0;
        while (args != null) {
            if (args[0].equals("*")) {
                args = processMethod(args);
                methods++;
            } else {
                args = getLine();
            }
        }
        printStats();
        System.out.println("methods = "+methods);
    }

    static String[] processMethod(String[] args) throws Exception {
        int nextEntry = -1;
        String methodName = args[1];
        XInstruction prev = null;
        XInstruction first = null;
        int count = 0;

        while ((args = getLine()) != null) {

            if (args[0].equals("*")) {
                break;
            }

            if (args[0].equals("-")) {
                nextEntry = getNumber(args[1]);
            }

            if (args[0].equals("+")) {
                count++;
                XInstruction in = new XInstruction(args);
                if (first == null) {
                    first = in;
                }
                if (nextEntry >= 0) {
                    targets[nextEntry] = in;
                    nextEntry = -1;
                }
                if (prev != null) {
                    prev.next = in;
                }
                prev = in;
            }
        }

//System.out.println(methodName+" = "+count);
        if (count > 0) {
            processXInstructions(first);
            totalBytes += XInstructionSize(first);
        }

        return args;
    }



    static int print(String name, int value) {
        while (name.length() < 20) {
            name += " ";
        }
        System.out.println(name+value);
        return value;
    }

    static void printStats() {
        int tot = 0;
        tot += print("geti_0",      geti_0);
        tot += print("geti_1",      geti_1);
        tot += print("geti_2",      geti_2);
        tot += print("addimm_2",    addimm_2);
        tot += print("addimm_3",    addimm_3);
        tot += print("add_X***",    add_X);
        tot += print("ldiimm_2",    ldiimm_2);
        tot += print("ldiimm_3",    ldiimm_3);
        tot += print("ldi_X***",    ldi_X);
        tot += print("stiimm_2",    stiimm_2);
        tot += print("stiimm_3",    stiimm_3);
        tot += print("sti_X***",    sti_X);
        tot += print("mov_X***",    mov_X);
        tot += print("invokei_X***",invokei_X);
        tot += print("invoke_3",    invoke_3);
        //tot += print("invoke_4",    invoke_4);
        tot += print("invoke_N",    invoke_N);
        tot += print("invoke_X***", invoke_X);
        tot += print("goto_2",      goto_2);
        tot += print("goto_3",      goto_3);
        tot += print("ifz_2",       ifz_2);
        tot += print("ifz_3",       ifz_3);
        tot += print("ifrr_3",      ifrr_3);
        tot += print("ifrr_4",      ifrr_5);
        tot += print("ifrr_5",      ifrr_4);
        tot += print("ifrc_4",      ifrc_5);
        tot += print("ifrc_5",      ifrc_4);
        tot += print("if_X***",     if_X);
        tot += print("return_1",    return_1);
        tot += print("return_2",    return_2);
        tot += print("return_X***", return_X);
        tot += print("clinit_2",    clinit_2);
        tot += print("monitor_2",   monitor_2);
        tot += print("alu_4",       alu_4);
        tot += print("alu_X***",    alu_X);

        tot += print("others_X",    others_X);


        System.out.println("total XInstructions = "+tot);
        System.out.println("total bytes = "+totalBytes);

    }

    static int totalBytes;
    static int geti_0;
    static int geti_1;
    static int geti_2;
    static int addimm_2;
    static int addimm_3;
    static int add_X;
    static int ldiimm_2;
    static int ldiimm_3;
    static int ldi_X;
    static int stiimm_2;
    static int stiimm_3;
    static int sti_X;
    static int mov_X;
    static int invokei_X;
    static int invoke_X;
    static int invoke_N;
    static int invoke_3;
    static int invoke_4;
    static int goto_2;
    static int goto_3;
    static int ifz_2;
    static int ifz_3;
    static int ifrr_3;
    static int ifrr_4;
    static int ifrr_5;
    static int ifrc_4;
    static int ifrc_5;
    static int if_X;
    static int return_1;
    static int return_2;
    static int return_X;
    static int clinit_2;
    static int monitor_2;
    static int alu_4;
    static int alu_X;
    static int others_X;


    static void processXInstructions(XInstruction first) throws Exception {
        int count = -1;
        for (XInstruction in = first ; in != null ; in = in.next) {
            count++;
            String menemonic = in.args[1];
            String args[] = in.args;

//System.out.println("menemonic = "+menemonic);

/*
 * GETI
 *
 * 0 - geti0000
 * 1 - getixxxx 1 - 15
 * 2 - geti1111 xxxxxxxx 1 - 255
 */


            if (menemonic.equals("GETI")) {
                int rd = getLocal(args[2]);

                if (rd == 0) {
                    geti_0++;
                    in.size = 0;
                    continue;
                }

                if (rd < 15) {
                    geti_1++;
                    in.size = 1;
                    continue;
                }

                geti_2++;
                in.size = 2;
                continue;
            }

/*
 * ADDI
 *
 *                                      (ssss = src reg 0-15)
 * 2 - addidddd ssss0iii                (i = -1 -> 6)
 * 3 - addidddd ssss1iii iiiiiiii       (i = -2048 -> 2047)
 */

            if (menemonic.equals("ADDI")) {
                int rd = getLocal(args[2]);
                if (isConst(args[3])) {
                    String temp = args[3];
                    args[3] = args[4];
                    args[4] = temp;
                }


                if (rd < 16 && isLocal(args[3]) && isConst(args[4])) {
                    int rs1 = getLocal(args[3]);
                    int imm = getConst(args[4]);
                    if (rs1 < 16) {
//                        if (imm >= -1 && imm < 7) {
                        if (imm >= -1 && imm < 15) {

                            addimm_2++;
                            in.size = 2;
                            continue;
                        }
/*
                        if (imm >= -2048 && imm <= 2047) {
                            addimm_3++;
                            in.size = 3;
                            continue;
                        }
*/
                    }
                }
//print(args);
//                add_X++;
//                continue;
            }

/*
 * MOVI
 *
 * movi reg const
 *                                      (ssss = src reg 0-15)
 * 2 - addidddd ssss0iii                (i = -1 -> 6)
 * 3 - addidddd ssss1iii iiiiiiii       (i = -2048 -> 2047)
 *
 * movi reg reg
 *                                      (ssss = src reg 0-15)
 * 2 - addidddd ssss0000
 */

            if (menemonic.equals("MOVI")) {
                int rd = getLocal(args[2]);
                if (rd < 16) {
                    if (isConst(args[3])) {
                        int imm = getConst(args[3]);
//                        if (imm >= -1 && imm < 7) {
                        if (imm >= -1 && imm < 15) {
                            addimm_2++;
                            in.size = 2;
                            continue;
                        }
/*
                        if (imm >= -4096 && imm <= 4095) {
                            addimm_3++;
                            in.size = 3;
                            continue;
                        }
*/
                    } else {
                        int rs1 = getLocal(args[3]);
                        if (rs1 < 16) {
                            addimm_2++;
                            in.size = 2;
                            continue;
                        }
                    }
                }
//print(args);
//                mov_X++;
//                continue;
            }

/*
 * LDI
 *                                      (bbbb = base 0xF reserved for global vector)
 * 2 - ldidddd bbbb0iii                 (i = 0 -> 7)
 * 3 - ldidddd bbbb1iii iiiiiiii        (i = -2048 -> 2047)
 */

            if (menemonic.equals("LDI")) {
                int rd = getLocal(args[2]);
                int base = 9999;

                if (isLocal(args[4])) {
                    base = getLocal(args[4]);
                } else {
                    int v = getConst(args[4]);
                    if (v == -1) {
                        base = 0;
                    }
                }

                if (rd < 16 && isConst(args[3])) {
                    int imm = getConst(args[3]);
                    if (base < 15) {                            // 0xF is reserved for global vector
                        if (imm >= 0 && imm < 8) {
                            ldiimm_2++;
                            in.size = 2;
                            continue;
                        }
                        if (imm >= -2048 && imm <= 2047) {
                            ldiimm_3++;
                            in.size = 3;
                            continue;
                        }
                    }
                }
                ldi_X++;
print(args);
                continue;
            }



/*
 * STI
 *                                      (bbbb = base 0xF reserved for global vector)
 * 2 - stidddd bbbb0iii                 (i = 0 -> 7)
 * 3 - stidddd bbbb1iii iiiiiiii        (i = -2048 -> 2047)
 */

            if (menemonic.equals("STI")) {
                if (isLocal(args[2])) {
                    int rd = getLocal(args[2]);
                    int base = 9999;

                    if (isLocal(args[4])) {
                        base = getLocal(args[4]);
                    } else {
                        int v = getConst(args[4]);
                        if (v == -1) {
                            base = 0;
                        }
                    }

                    if (rd < 16 && isConst(args[3])) {
                        int imm = getConst(args[3]);
                        if (base < 15) {                            // 0xF is reserved for global vector
                            if (imm >= 0 && imm < 8) {
                                stiimm_2++;
                                in.size = 2;
                                continue;
                            }
                            if (imm >= -2048 && imm <= 2047) {
                                stiimm_3++;
                                in.size = 3;
                                continue;
                            }
                        }
                    }
                }
                sti_X++;
                continue;
            }



/*
 * INVOKEI
 */

            if (menemonic.equals("INVOKEI")) {
                invokei_X++;
                continue;
            }

/*
 * INVOKE/INVOKES
 * invk1111 Xsssssss 22223333
 * invknnnn Xsssssss 11111111 22222222 33333333 .. nnnnnnnn
 */

            if (menemonic.startsWith("INVOKE")) {
                boolean isStatic = menemonic.equals("INVOKES");
                int oper;
                if (isStatic) {
                    oper = getClass(args[3]);
                } else {
                    oper = getLocal(args[3]);
                }

                int slot = getSlot(args[2]);

                if (slot < 128 && oper < 256 && args.length < (16+3)) {
                    boolean ok = true;
                    int highreg = 0;
                    for (int i = 4 ; i < args.length ; i++) {
                        if (isLocal(args[i])) {
                            oper = getLocal(args[i]);
                            if (oper >= 128) {
                                ok = false;
                            }
                            if (highreg > oper) {
                               highreg = oper;
                            }
                        } else {
                            oper = getConst(args[i]);
                           // if (oper < -64 || oper > 63) {
                            if (oper < -10 || oper > 117) {

                                ok = false;
                            }
                            highreg = 9999;
                        }
                    }
                    if (ok) {
                        if (args.length <= 6 && highreg < 16) { // + INVOKE @slot p1 p2 p3
                            invoke_3++;                         // invk1111 Xsssssss 22223333
                            in.size = 3;
                        //} else if (args.length <= 8 && highreg < 16) {
                        //    invoke_4++;                         // invk1111 Xsssssss 22223333 44445555
                        //    in.size = 4;
                        } else {
                            invoke_N++;
                            in.size = args.length - 1;          // invknnnn Xsssssss 11111111 22222222 33333333 .. nnnnnnnn
                        }
                        continue;
                    }
                }
                invoke_X++;
//print(args);
                continue;
            }



/*
 * RETURN/RETURNI
 *
 * retxxxx
 */

            if (menemonic.equals("RETURN")) {
                return_1++;
                in.size = 1;
                continue;
            }


            if (menemonic.equals("RETURNI")) {
                if (isLocal(args[2])) {
                    int rd = getLocal(args[2]);
                    if (rd < 16) {
                        return_1++;
                        in.size = 1;
                    } else {
                        return_2++;
                        in.size = 2;
                    }
                    continue;
                }
                return_X++;
                continue;
            }

/*
 * GOTO
 *
 * gotoxxxx xxxxxxxx
 */

            if (menemonic.equals("GOTO")) {
                int addr = getAddr(args[2]);
                int delta = (addr<count) ? count-addr : addr-count;

                if (delta < 1024) {         // approx for +-2048
                    goto_2++;
                    in.size = 2;
                } else {
                    goto_3++;
                    in.size = 3;
                }

                continue;
            }



/*
 * Normalize IFs
 */


            if (menemonic.startsWith("IF") && isConst(args[2])) {
                String temp = args[2];
                args[2] = args[3];
                args[3] = temp;
                int reg  = getLocal(args[2]);
            }



/*
 * IFEQ/NE
 *
 * ifrrrr Xaaaaaaaa
 */

            if (menemonic.equals("IFEQ") || menemonic.equals("IFNE")) {
                if (isConst(args[3]) && getConst(args[3]) == 0) {
                    int reg  = getLocal(args[2]);
                    int addr = getAddr(args[4]);
                    if (reg < 16) {
                        int delta = (addr<count) ? count-addr : addr-count;
                        if (delta < 25) {
                            ifz_2++;
                            in.size = 2;
                        } else {
                            ifz_3++;
                            in.size = 3;
                        }
                        continue;
                    }
                }
            }




/*
 * IFrr
 *
 * ifcccc ssssdddd aaaaaaaaa
 */


            if (menemonic.startsWith("IF")) {
                int reg  = getLocal(args[2]);
                int addr = getAddr(args[4]);
                int delta = (addr<count) ? count-addr : addr-count;

                if (isLocal(args[3])) {
                    if (reg < 16 && getLocal(args[3]) < 16 && delta < 50) {
                        ifrr_3++;
                        in.size = 3;
                        continue;
                    }
                }
            }



/*
 * IF
 *
 * ifcccc ssssssss dddddddd aaaaaaaaa [aaaaaaaa]
 */


            if (menemonic.startsWith("IF")) {
                int reg  = getLocal(args[2]);
                int addr = getAddr(args[4]);
                int delta = (addr<count) ? count-addr : addr-count;

                if (isLocal(args[3])) {
                    if (delta < 25) {
                        ifrr_4++;
                        in.size = 4;
                        continue;
                    } else {
                        ifrr_5++;
                        in.size = 5;
                        continue;
                    }
                }

                int val = getConst(args[3]);
                if (val >= -64 && val < 64 ) {
                    if (delta < 25) {
                        ifrc_4++;
                        in.size = 4;
                        continue;
                    } else {
                        ifrc_5++;
                        in.size = 5;
                        continue;
                    }
                }


                if_X++;
                continue;
            }


/*
 * CLINIT
 */

            if (menemonic.equals("CLINIT")) {
                clinit_2++;
                continue;
            }


/*
 * MENTER/MEXIT
 */

            if (menemonic.equals("MENTER") || menemonic.equals("MEXIT")) {
                monitor_2++;
 //print(args);
                continue;
            }


            if (
                    menemonic.equals("ADDI") ||
                    menemonic.equals("ANDI") ||
                    menemonic.equals("DIVI") ||
                    menemonic.equals("MULI") ||
                    //menemonic.equals("NEGI") ||
                    menemonic.equals("ORRI") ||
                    menemonic.equals("REMI") ||
                    menemonic.equals("SLLI") ||
                    menemonic.equals("SRAI") ||
                    menemonic.equals("SRLI") ||
                    menemonic.equals("SUBI")
            ) {

                boolean fit3 = false;
                boolean fit4 = false;

                if (isConst(args[3])) {
                    if (getConst(args[3]) >= -128 && getConst(args[3]) < 128) {
                        fit3 = true;
                    }
                } else {
                    if (getLocal(args[3]) < 128) {
                        fit3 = true;
                    }
                }

                if (isConst(args[4])) {
                    if (getConst(args[4]) >= -128 && getConst(args[4]) < 128) {
                        fit4 = true;
                    }
                } else {
                    if (getLocal(args[4]) < 128) {
                        fit4 = true;
                    }
                }
                if (fit3 && fit4) {
                    alu_4++;
                    in.size = 4;
                    continue;
                } else {
                    alu_X++;
//print(args);
                    continue;
                }

            }

/*
 * others
 */
            others_X++;




        }
    }



    static int XInstructionSize(XInstruction first) throws Exception {
        int res = 0;
        for (XInstruction in = first ; in != null ; in = in.next) {
            res += in.size;
        }
        return res;
    }



    static void print(String[] args) {
        for (int i = 0 ; i < args.length ; i++) {
            System.out.print(args[i]+ " ");
        }
         System.out.println();
    }

}