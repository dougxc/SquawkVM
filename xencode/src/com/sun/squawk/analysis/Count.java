package com.sun.squawk.analysis;

import java.io.*;
import java.util.*;

class Count {

    static InputStreamReader isr;

    static String[] lines = new String[500];

    static String[] getLine() throws Exception {
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
                lines[argc++] = next;
            }
        }

        String[] result = new String[argc];
        for (int i = 0 ; i < argc ; i++) {
            result[i] = lines[i];
        }
        return result;
    }


    static String percent(int count, int total) {

        String s =  ""+((count*1000)/total);
        while (s.length() < 3) {
            s = "0"+s;
        }

        return ""+
                ((char)s.charAt(0)) +
                ((char)s.charAt(1)) +
                "." +
                ((char)s.charAt(2));


    }



    public static void main(String[] args) throws Exception {
        isr = new InputStreamReader(System.in);
        Hashtable insTypes = new Hashtable();
        String[] argv;
        int count = 0;
        int methods = 0;

        while ((argv = getLine()) != null) {
            if (argv.length > 0 && argv[0].equals("+")) {
                count++;
                Integer temp = (Integer)insTypes.get(argv[1]);
                if (temp == null) {
                    temp = new Integer(0);
                }
                insTypes.put(argv[1], new Integer(temp.intValue()+1));
            }
            if (argv.length > 0 && argv[0].equals("*")) {
                System.out.println("+++"+argv[1]);
                methods++;
            }

        }
        System.out.println(""+count+" lines");
        System.out.println(""+methods+" methods");


        for (Enumeration e = insTypes.keys() ; e.hasMoreElements() ;) {
            String  key = (String)e.nextElement();
            Integer val = (Integer)insTypes.get(key);

            String vals = ""+val;
            while(vals.length() < 20) {
                vals += " ";
            }
            while(key.length() < 20) {
                key += " ";
            }
            System.out.println(vals+key+percent(val.intValue(), count));

        }


    }

}