package com.sun.squawk.analysis;

import java.io.*;
import java.util.*;

public class Count4 {

    static class Instruction {
        String[] args;
        int size;
        int address;
        Instruction next;

        Instruction(String[] args) {
            this.args = args;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0 ; i < args.length ; i++) {
                sb.append(args[i]);
                sb.append(" ");
            }
            return sb.toString();
        }
    }





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

    static boolean isConstZero(String s) {
        return s.equals("#0");
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


/*
   0        #0
   1        #1
   2        #-1
   3        local 0
   4        local 1
   5        local 2
   6        local 3
   7        local 4
   8        local 5
   9        local 6
   10       local 7
   11       local 8
   12       local 9
   13       local 10
   14       local byte follows
   15       constant byte follows

   local byte is 0-255                                  (size = +1)
   constant byte is #-126 -> #127                       (size = +1)
   if constant byte == #-127 constant short follows     (size = +3)
   if constant byte == #-128 constant int follows       (size = +5)

 */

    static int get4bitParm(String s) {
        if (isLocal(s)) {
            int reg = getLocal(s);
            if (reg < 3) {
                throw new RuntimeException("bad reg "+s);
            }
            return (reg < 14) ? 0 : 1;
        } else {
            int val = getConst(s);
            if (val == 0 || val == 1 || val == -1) {
                return 0;
            }
            if (val >= -126 && val < 128) {
                return 1;
            }
            if (val >= -32786 && val < 32786) {
                return 3;
            }
            return 5;
        }
    }

/*
   0-28     local
   29       local byte follows
   30       constant short follows                      (size = +3)
   31       constant int follows                        (size = +3)
   32-255   #-96 -> #127

 */

    static int get8bitParm(String s) {
        if (isLocal(s)) {
            int reg = getLocal(s);
            if (reg < 3) {
                throw new RuntimeException("bad reg "+s);
            }
            return (reg < 29) ? 0 : 1;
        } else {
            int val = getConst(s);
            if (val >= -96 && val < 128) {
                return 0;
            }
            if (val >= -32786 && val < 32786) {
                return 2;
            }
            return 5;
        }
    }

/*
   0-14     -> #-1 -> #13
   15       constant byte follows

   constant byte is #-126 -> #127                       (size = +1)
   if constant byte == #-127 constant short follows     (size = +3)
   if constant byte == #-128 constant int follows       (size = +5)
 */


    static int get4bitConst(String s) {
        int val = getConst(s);
        if (val >= -1 && val < 14) {
            return 0;
        }
        if (val >= -126 && val < 128) {
            return 1;
        }
        if (val >= -32786 && val < 32786) {
            return 3;
        }
        return 5;
    }


/*
   0000             #0
   0001             #1
   0010             #2
   0011             #3
   0100             #4
   0101             #5
   0110             #6
   0111             #7
   1xxx xxxxxxxx    #8 -> #2047                         (size = +1)
   1000 00000000    constant int follows                (size = +5)
 */

    static int get11bitConstLoad(String s) {
        int imm = getConst(s);
        if (imm >= 0 && imm < 8) {
//System.out.println(""+s);
            return 0;
        }
        if (imm >= 0 && imm <= 2047) {
            return 1;
        }
        //throw new RuntimeException("bad 11 bit load "+s);
//System.out.println("bad 11 bit load "+s);

        return 5;
    }


/*
   0000             #-1
   0001             #0
   0010             #1
   0011             #2
   0100             #3
   0101             #4
   0110             #5
   0111             #6
   1xxx xxxxxxxx    #-1028 -> #1027                     (size = +1)
 */

    static int get11bitConstAdd(String s) {
        int imm = getConst(s);
        if (imm >= -1 && imm < 7) {
            return 0;
        }
        if (imm >= -1024 && imm <= 1024) {
            return 1;
        }
        return -1;
    }






/*
   0000             #-1
   0001             #0
   0010             #1
   0011             #2
   0100             #3
   0101             #4
   0110             #5
   0111             #6
   1xxx xxxxxxxx    #-1028 -> #1027                     (size = +1)
 */

    static int get4bitLDConst(String s) {
        int imm = getConst(s);
        if (imm < 0 || imm > 255) {
            throw new RuntimeException("bad get4bitLDConst "+s);
        }

        if (imm < 32) {
            return 0;
        }
        return 1;
    }





/*
   0        unused
   1        unused
   2        unused
   3        local 0
   4        local 1
   5        local 2
   6        local 3
   7        local 4
   8        local 5
   9        local 6
   10       local 7
   11       local 8
   12       local 9
   13       local 10
   14       unused
   15       local byte follows
 */


    static int get4bitLocal(String s) {
        int reg = getLocal(s);
        if (reg < 3) {
            throw new RuntimeException("bad reg "+s);
        }
        return (reg < 14) ? 0 : 1;
    }


/*
   0        unused
   1        unused
   2        unused
   3        local 0
   4        local 1
   5        local 2
   6        local 3
   7        local 4
   8        local 5
   9        local 6
   10       local 7
   11       local 8
   12       local 9
   13       local 10
   14       global vector reference
   15       local byte follows
 */

    static int get4bitLocalOrGlobalVector(String s) {
        if (isConst(s)) {
            if (getConst(s) != -1) {
                throw new RuntimeException("bad const");
            }
            return 0;
        }
        int reg = getLocal(s);
        if (reg < 3) {
            throw new RuntimeException("bad reg "+s);
        }
        return (reg < 14) ? 0 : 1;
    }



    static Instruction[] targets = new Instruction[10000];


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
        Instruction prev = null;
        Instruction first = null;
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
                Instruction in = new Instruction(args);
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
            processInstructions(first);
            totalBytes += instructionSize(first);
        }

        return args;
    }


    static void print(String s) {
        while (s.length() < 10) {
            s += " ";
        }
        System.out.print(s);
    }

    static int print(String name, int value) {
        while (name.length() < 20) {
            name += " ";
        }
        print(name);
        System.out.println(value);
        return value;
    }

    static int print(String name, int value, int size) {
        while (name.length() < 20) {
            name += " ";
        }
        float ave = (float)size / (float)value;
        print(name);
        print(""+value);
        print(""+size);
        System.out.println(" ave="+ave);
        return value;
    }

    static int print(String name, int value, int size, int nparmstot) {
        while (name.length() < 20) {
            name += " ";
        }
        float ave   = (float)size / (float)value;
        float avepp = (float)size / (float)nparmstot;
        print(name);
        print(""+value);
        print(""+size);
        System.out.println(" ave="+ave+ " avepp="+avepp);
        return value;
    }


    static void printStats() {
        int tot = 0;

        tot += print("geti_N",      geti_N,     geti_S);
        //tot += print("addi_N",      addi_N,     addi_S);
        tot += print("addimm_N",    addimm_N,   addimm_S);
        tot += print("ldiimm_N",    ldiimm_N,   ldiimm_S);
        tot += print("stiimm_N",    stiimm_N,   stiimm_S);
        tot += print("invoke_N",    invoke_N,   invoke_S, invoke_P);
        tot += print("invoke2_N",   invoke2_N,  invoke2_S, invoke2_P);
        tot += print("invoke3_N",   invoke3_N,  invoke3_S, invoke3_P);

        tot += print("return_N",    return_N,   return_S);
        tot += print("goto_N",      goto_N,     goto_S);
        tot += print("ifz_N",       ifz_N,      ifz_S);
        tot += print("if_N",        if_N,       if_S);
        tot += print("alu_N",       alu_N,      alu_S);

        tot += print("ccast",       ccast_N,    ccast_S);
        tot += print("iof",         iof_N,      iof_S);
        tot += print("cstor",       cstor_N,    cstor_S);

        tot += print("clinit",      clinit_N,   clinit_S);
        tot += print("monitor",     monitor_N,  monitor_S);
        tot += print("throw",       throw_N,    throw_S);
        tot += print("geti2",       geti2_N,    geti2_S);
        tot += print("math",        math_N,     math_S);
        tot += print("alth",        alth_N,     alth_S);
        tot += print("misc1",       misc1_N,    misc1_S);
        tot += print("misc2",       misc2_N,    misc2_S);
        tot += print("switch",      switch_N,   switch_S);
        tot += print("ldconst",     ldconst_N,  ldconst_S);
        tot += print("nop",         nop_N,      nop_S);

        tot += print("others",      others_X);


        System.out.println("inv_interface   " + inv_interface);
        System.out.println("inv_virtual     " + inv_virtual);
        System.out.println("inv_static      " + inv_static);
        System.out.println("inv_special     " + inv_special);
        System.out.println("inv_args        " + inv_args);


        System.out.println("total nopdrop = "+nopdrop);
        System.out.println("total instructions = "+tot);
        System.out.println("total bytes = "+totalBytes);

    }

    static int totalBytes;


    static int others_X;

    static int geti_N;
    static int geti_S;
    static int addi_N;
    static int addi_S;
    static int addimm_N;
    static int addimm_S;
    static int ldiimm_N;
    static int ldiimm_S;
    static int stiimm_N;
    static int stiimm_S;
    static int invoke_N;
    static int invoke_S;
    static int invoke_P;

    static int invoke2_N;
    static int invoke2_S;
    static int invoke2_P;

    static int invoke3_N;
    static int invoke3_S;
    static int invoke3_P;


    static int return_N;
    static int return_S;
    static int goto_N;
    static int goto_S;
    static int ifz_N;
    static int ifz_S;
    static int if_N;
    static int if_S;
    static int alu_N;
    static int alu_S;
    static int clinit_N;
    static int clinit_S;
    static int monitor_N;
    static int monitor_S;
    static int throw_N;
    static int throw_S;
    static int geti2_N;
    static int geti2_S;

    static int math_N;
    static int math_S;
    static int alth_N;
    static int alth_S;

    static int ccast_N;
    static int ccast_S;
    static int cstor_N;
    static int cstor_S;


    static int misc1_N;
    static int misc1_S;
    static int misc2_N;
    static int misc2_S;
    static int switch_N;
    static int switch_S;
    static int iof_N;
    static int iof_S;

    static int ldconst_N;
    static int ldconst_S;

    static int nop_N;
    static int nop_S;

    static int nopdrop;

    static int inv_interface;
    static int inv_virtual;
    static int inv_static;
    static int inv_special;
    static int inv_args;


    static void processInstructions(Instruction first) throws Exception {
        int count = -1;
        String prev_menemonic, menemonic = null;
        for (Instruction in = first ; in != null ; in = in.next) {
            count++;

//System.out.println(in);

            prev_menemonic = menemonic;
            menemonic = in.args[1];
            String args[] = in.args;

//System.out.println("menemonic = "+menemonic);

/*
 * GETI
 *
 * 1 - getixxxx
 */


            if (menemonic.equals("GETI")) {
                in.size = 1+get4bitLocal(args[2]);
                geti_N++;
                geti_S += in.size;
                writeInstruction(in);
                continue;
            }

/*
 * ADDI/MOVI immeadiate
 *
 * 2 - addidddd bbbb0iii                (i = 0 -> 7)
 * 3 - addidddd bbbb1iii iiiiiiii       (i = -1024 -> 1023)
 */


            if (menemonic.equals("MOVI")) {
                args = new String[] {"+", "ADDI", args[2], args[3], "#0"};
                menemonic = "ADDI";
            }


            if (menemonic.equals("ADDI")) {
                if (isConst(args[3])) {
                    String temp = args[3];
                    args[3] = args[4];
                    args[4] = temp;
                }

                if (isConst(args[4])) {
                    int val = getConst(args[4]);
                    int size = get11bitConstAdd(args[4]);
                    if (size != -1) {
                        in.size = 2+get4bitLocal(args[2]) + get4bitParm(args[3]) + size;
                        addimm_N++;
                        addimm_S += in.size;
                        writeInstruction(in);
                        continue;
                    }

                }

                // 2 - addidddd 11112222
                //in.size = 2+get4bitLocal(args[2]) + get4bitParm(args[3]) + get4bitParm(args[4]);
                //addi_N++;
                //addi_S += in.size;
                //continue;
            }



/*
 * LDI immeadiate
 * 2 - ldidddd bbbb0iii                 (i = 0 -> 7)
 * 3 - ldidddd bbbb1iii iiiiiiii        (i = 0 -> 2047)
 */

            if (menemonic.equals("LDI")) {
                if(isConst(args[3])) {
                    in.size = 2+get4bitLocal(args[2]) + get11bitConstLoad(args[3]) + get4bitLocalOrGlobalVector(args[4]);
                    ldiimm_N++;
                    ldiimm_S += in.size;
                    writeInstruction(in);
                    continue;
                }
            }

/*
 * STI immeadiate
 * 2 - stidddd bbbb0iii                 (i = 0 -> 7)
 * 3 - stidddd bbbb1iii iiiiiiii        (i = -2048 -> 2047)
 */

            if (menemonic.equals("STI")) {
                if(isConst(args[3])) {
                    in.size = 2+get4bitParm(args[2]) + get11bitConstLoad(args[3]) + get4bitLocalOrGlobalVector(args[4]);
                    stiimm_N++;
                    stiimm_S += in.size;
                    writeInstruction(in);
                    continue;
                }
            }



/*
 * INVOKE/INVOKES/INVOKEI
 * invk1111 Xsssssss
 * invk1111 Xsssssss 22223333
 * invknnnn Xsssssss 11112222 33334444 ....
 */

            if (menemonic.startsWith("INVOKE")) {
                boolean isStatic = menemonic.equals("INVOKES");
                boolean isInterf = menemonic.equals("INVOKEI");

                int icount = isInterf ? 3 : 2;
                int p0 = 3;

                int oper;
                if (isStatic) {
//                    icount++;
                    oper = getClass(args[2]);
                    p0++;
//                    if (oper > 255) {
//                        icount += 2;
//                    }
                }
                int slot = getSlot(args[p0-1]);
//if (isStatic) System.out.println("slot="+slot);
//                if (slot > 127) {
//                    icount += 2;
//                }

                int pargs = args.length - p0;

//                if (pargs > 15) {
//                    icount += 2;
//                }


if (isInterf) {
  inv_interface++;
} else if (!isStatic) {
  inv_virtual++;
} else if (isConstZero(args[p0])) {
  inv_static++;
} else {
  inv_special++;
}
inv_args += pargs;


                for (int i = p0 ; i < args.length ; i++) {
                    if (isConst(args[i])) {
                        icount += 1+get4bitConst(args[i]);
                    } else {
                        icount += 1+get4bitParm(args[i]);
                    }
                }




                invoke2_N++;
                in.size = icount;
                invoke2_S += in.size;
                invoke2_P += pargs;


/*
                if (pargs <= 1) {
                    invoke2_N++;
                    in.size = icount;
                    invoke2_S += in.size;
                    invoke2_P += pargs;
                } else if (pargs <= 3) {
                    icount += 1;
                    invoke3_N++;
                    in.size = icount;
                    invoke3_S += in.size;
                    invoke3_P += pargs;
                } else {
                    icount += ((pargs+1)/2); // two parms per byte
                    invoke_N++;
                    in.size = icount;
                    invoke_S += in.size;
                    invoke_P += pargs;
                }
*/
                writeInstruction(in);
                continue;
            }




/*
 * MATH
 * math code1111 22223333 44445555
 */

            if (menemonic.startsWith("MATH")) {

                int icount = 4;

                for (int i = 2 ; i < args.length ; i++) {
                    icount += get4bitParm(args[i]);
                }
                in.size = icount;
                math_N++;
                math_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * RETURN/RETURNI
 *
 * retxxxx
 */

            if (menemonic.equals("RETURN")) {
                in.size = 1;
                return_N++;
                return_S += in.size;
                writeInstruction(in);
                continue;
            }


            if (menemonic.equals("RETURNI")) {
                in.size = 1+get4bitParm(args[2]);
                return_N++;
                return_S += in.size;
                writeInstruction(in);
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

                in.size = 2;

                goto_N++;
                if (delta > 1024) {
                    in.size = 4;
                }

                goto_S += in.size;
                writeInstruction(in);
                continue;
            }



/*
 * Normalize IFs
 */


            if (menemonic.startsWith("IF") && isConst(args[2])) {
                String temp = args[2];
                args[2] = args[3];
                args[3] = temp;
            }



/*
 * IFEQz/NEz
 *
 * ifrrrr Xaaaaaaaa (+-64)
 */

            if (menemonic.equals("IFEQ") || menemonic.equals("IFNE")) {
                if (isConst(args[3]) && getConst(args[3]) == 0) {
                    int addr = getAddr(args[4]);
                    int delta = (addr<count) ? count-addr : addr-count;
                    if (delta < 32) {
                        in.size = 2+get4bitLocal(args[2]);
                        ifz_N++;
                        ifz_S += in.size;
                        writeInstruction(in);
                        continue;
                    }
                }
            }




/*
 * IF
 *
 * ifcccc ssssdddd 0aaaaaaaa
 * ifcccc ssssdddd 1aaaaaaaa aaaaaaaa

 */


            if (menemonic.startsWith("IF")) {
                int addr = getAddr(args[4]);
                int delta = (addr<count) ? count-addr : addr-count;
                in.size = (delta < 32) ? 3 : 4;
                in.size += get4bitLocal(args[2]);
                in.size += get4bitParm(args[3]);
                if_N++;
                if_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * ALU
 * alucccc dddd1111 22222222
 */


                    //menemonic.equals("NEGI") ||


            if (
                    menemonic.equals("ADDI") ||
                    menemonic.equals("ANDI") ||
                    menemonic.equals("DIVI") ||
                    menemonic.equals("MULI") ||
                    menemonic.equals("ORRI") ||
                    menemonic.equals("XORI") ||
                    menemonic.equals("REMI") ||
                    menemonic.equals("SLLI") ||
                    menemonic.equals("SRAI") ||
                    menemonic.equals("SRLI") ||
                    menemonic.equals("SUBI") ||
                    menemonic.equals("LDI") ||
                    menemonic.equals("LDI_BC") ||
                    menemonic.equals("LDB") ||
                    menemonic.equals("LDB_BC") ||
                    menemonic.equals("LDC") ||
                    menemonic.equals("LDC_BC") ||
                    menemonic.equals("LDS") ||
                    menemonic.equals("LDS_BC")

            ) {
                in.size = 3;
                in.size += get4bitLocal(args[2]);
                in.size += get4bitParm(args[3]);
                in.size += get8bitParm(args[4]);

                alu_N++;
                alu_S += in.size;
                writeInstruction(in);
                continue;
            }


            if (
                    menemonic.equals("STOOP") ||
                    menemonic.equals("STOOP_BC") ||
                    menemonic.equals("STI") ||
                    menemonic.equals("STI_BC") ||
                    menemonic.equals("STB") ||
                    menemonic.equals("STB_BC") ||
                    menemonic.equals("STS") ||
                    menemonic.equals("STS_BC")

            ) {
                in.size = 3;
                in.size += get4bitParm(args[2]);
                in.size += get4bitParm(args[3]);
                in.size += get8bitParm(args[4]);

                alu_N++;
                alu_S += in.size;
                writeInstruction(in);
                continue;
            }



/*
 * CLINIT
 *
 * clinit classnum
 */

            if (menemonic.equals("CLINIT")) {
                in.size = 2;
                clinit_N++;
                clinit_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * MENTER/MEXIT
 */

            if (menemonic.equals("MENTER") || menemonic.equals("MEXIT")) {
                in.size = 2;
                monitor_N++;
                monitor_S += in.size;
                writeInstruction(in);
                continue;
            }

/*
 * THROW
 * throw ssssssss
 */

            if (menemonic.equals("THROW")) {
                in.size = 2;
                throw_N++;
                throw_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * CHECKCAST
 * checkcast rrrrnnnn nnnnnnnn
 */

            if (menemonic.equals("CHECKCAST")) {
                in.size = 3 + get4bitParm(args[2]);
                ccast_N++;
                ccast_S += in.size;
                writeInstruction(in);
                continue;
            }

/*
 * INSTANCEOF
 * instanceof rrrrssss nnnnnnnn
 */

            if (menemonic.equals("INSTANCEOF")) {
                in.size = 3 + get4bitParm(args[2]);
                if (getConst(args[3]) > 254) {
                    in.size += 2;
                }
                iof_N++;
                iof_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * CHECKSTORE
 * checkstore 11112222
 */

            if (menemonic.equals("CHECKSTORE")) {
                in.size = 2 + get4bitLocal(args[2]) + get4bitLocal(args[2]);
                cstor_N++;
                cstor_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * ALENGTH
 * alength 11112222
 */

            if (menemonic.equals("ALENGTH")) {
                in.size = 2 + get4bitLocal(args[2]) + get4bitLocal(args[2]);
                alth_N++;
                alth_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * LDCONST
 * ldconst 1111nnnn
 */

            if (menemonic.equals("LDCONST")) {
                in.size = 2 + get4bitLocal(args[2]) + get4bitLDConst(args[3]);
                ldconst_N++;
                ldconst_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * GETI2
 */

            if (menemonic.equals("GETI2")) {
                in.size = 2;
                geti2_N++;
                geti2_S += in.size;
                writeInstruction(in);
                continue;
            }

/*
 * MISC1
 */

            if (menemonic.equals("YIELD")) {
                in.size = 1;
                misc1_N++;
                misc1_S += in.size;
                writeInstruction(in);
                continue;
            }


/*
 * MISC2
 */

            if (menemonic.equals("YIELD") ||
                menemonic.equals("NEGI") ||
                menemonic.equals("EXEC") ||
                menemonic.equals("PARM")
            ) {
                in.size = 2;
                misc2_N++;
                misc2_S += in.size;
                writeInstruction(in);
                continue;
            }

            if (menemonic.equals("RETURNL")
            ) {
                in.size = 2 + get4bitParm(args[2]) + get4bitParm(args[2]);
                misc2_N++;
                misc2_S += in.size;
                writeInstruction(in);
                continue;
            }


            if (menemonic.equals("I2B") ||
                menemonic.equals("I2C") ||
                menemonic.equals("I2S")
            ) {
                in.size = 2 + get4bitLocal(args[2]) + get4bitParm(args[2]);
                misc2_N++;
                misc2_S += in.size;
                writeInstruction(in);
                continue;
            }




/*
 * TABLESWITCH
 */

            if (menemonic.equals("TABLESWITCH")) {
                in.size = 4 + (args.length-4) * 2;
                switch_N++;
                switch_S += in.size;
                writeInstruction(in);
                continue;
            }

/*
 * LOOKUPSWITCH
 */

            if (menemonic.equals("LOOKUPSWITCH")) {
                in.size = 4;
                in.size += ((args.length-4)/2) * 2;
                in.size += ((args.length-4)/2) * 4;

                switch_N++;
                switch_S += in.size;
                writeInstruction(in);
                continue;
            }



/*
 * NOP
 */

            if (menemonic.equals("NOP")) {
                in.size = 0;
                nop_N++;
                nop_S += in.size;

                if (prev_menemonic != null && !prev_menemonic.equals("GOTO")) {
                    nopdrop++;
                }

                continue;
            }


/*
 * others
 */

//print(args);
            others_X++;


        }
    }



    static int instructionSize(Instruction first) throws Exception {
        int res = 0;
        for (Instruction in = first ; in != null ; in = in.next) {
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


    static void writeInstruction(Instruction in) {
        //System.out.print(""+in.size+"\t");
        //print(in.args);

    }


}





/*

                        KVM                 Squawk



Basic metadata          30                  11.5
Handler tables          2.1                 0           (included in basic metadata)
Stackmaps               11.2 (est)          0           (included in basic metadata)
Average code size       51.7                46.5
Code buffer overhead    12                  8
Vtable pointers         0                   5 (est)

TOTAL                   107.0               71.0    (66%)
w/opt @75%                                  59      (54%)


instructions            64582               37793   (59%)
w/opt @75%                                  28344   (43%)




0000 geti_N              6833      7071       ave=1.0348309
0001 addimm_N            2359      5050       ave=2.1407375
0010 ldiimm_N            5852      15822      ave=2.703691
0011 stiimm_N            1178      2963       ave=2.5152802
0100 invoke_N            615       3285       ave=5.3414636 ave
0101 invoke2_N           2847      5858       ave=2.0576046 ave
0110 invoke3_N           5872      19822      ave=3.3756812 ave
0111 return_N            2234      2286       ave=1.0232767
1000 goto_N              1542      3106       ave=2.0142672
1001 ifz_N               1092      2273       ave=2.0815017
1010 if_N                1362      4892       ave=3.5917768
1011 alu_N               2299      8693       ave=3.7812092
1100 alu_N
1101 xxxx
1110 xxxx 48 extended opcodes
1111 xxxx

*/