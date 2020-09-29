import java.io.*;
import java.util.*;
import javax.microedition.io.*;


public class TCK {
    public static void main(String[] args) {
        boolean positive = true;
        String  logURL    = "file://tck/log.txt;append=true";
        String  passedURL = "file://tck/passed.txt";
        String  failedURL = "file://tck/failed.txt";
        Hashtable properties = new Hashtable();

        String[] zeroArgs = new String[0];
        int i = 0;
        for (; i != args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.startsWith("-neg")) {
                positive = false;
            }
            else
            if (arg.startsWith("-logURL:")) {
                logURL = arg.substring("-logURL:".length());
            }
            else
            if (arg.startsWith("-passedURL:")) {
                passedURL = arg.substring("-passedURL:".length());
            }
            else
            if (arg.startsWith("-failedURL:")) {
                failedURL = arg.substring("-failedURL:".length());
            }
            else
            if (arg.equals("-traceLoading")) {
                properties.put("squawk.trace.classloading",             "true");
            }
            else
            if (arg.equals("-traceParser")) {
                properties.put("squawk.trace.classloading.input",       "true");
            }
            else
            if (arg.equals("-disassemble")) {
                properties.put("squawk.trace.classloading.disassemble", "true");
            }
            else {
                System.out.println("Unknown option ignored: " + arg);
            }
        }

        if (args.length <= i) {
            System.out.println("Missing URL for tests");
            return;
        }

        String  testsURL = args[i];

        PrintStream log    = createPrintStream(logURL);
        PrintStream passed = createPrintStream(passedURL);
        PrintStream failed = createPrintStream(failedURL);

        System.out.println("Test log is being written to " + logURL);
        System.out.println("Passed log is being written to " + passedURL);
        System.out.println("Failed log is being written to " + failedURL);

        properties.put("java.lang.System.out", logURL);

        System.out.println("Reading tests from " + testsURL + " ...");
        try {
            InputStreamReader isr = new InputStreamReader(Connector.openInputStream(testsURL));
            String line = getLine(isr);
            while (line != null) {
                if (!line.startsWith("#") && line.trim().length() > 0) {
                    int space = line.indexOf(' ');
                    StringTokenizer st = new StringTokenizer(line);
                    args = new String[st.countTokens() - 1];
                    String test = st.nextToken();
                    for (int argc = 0; argc != args.length; argc++) {
                        args[argc] = st.nextToken();
                    }
                    log.println("executing: " + test);
                    try {
                        Isolate app = new Isolate(test, args, "", test, properties);
                        app.start();
                        try {
                            app.join();
                        } catch (InterruptedException ie) {
                        }
                        if (app.result() == 95 && positive) {
                            passed.println(test);
                        }
                        else {
                            failed.println(test);
//                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("Exception occurred while executing test " + test);
                        e.printStackTrace();
                    }
                    log.flush();
                    passed.flush();
                    failed.flush();
                }
                else {
                    System.out.println("skipping: " + line);
                }
                line = getLine(isr);
            }
            isr.close();
        } catch (IOException ioe) {
            System.err.println("Exception while reading tests:");
            ioe.printStackTrace();
        } finally {
            log.close();
            passed.close();
            failed.close();
        }
    }

    static PrintStream createPrintStream(String url) {
        try {
            return new PrintStream(Connector.openOutputStream(url));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException("failed to open " + url);
        }
    }

    /*
     * getLine
     */
    static String getLine(InputStreamReader isr) throws IOException {
        StringBuffer sb = new StringBuffer();
        int ch = isr.read();
        if (ch == -1) {
            return null;
        }
        while (ch != '\n' && ch != -1) {
            sb.append((char)ch);
            ch = isr.read();
        }
        return sb.toString();
    }
}
