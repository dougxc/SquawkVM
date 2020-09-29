import java.io.*;
import java.util.*;
import java.net.*;

/**
 * This is the launcher for building parts (or all) of the Squawk VM. It is also
 * used to start the translator and the VM once it has been built.
 *
 */
public class Build {

    static final String defines = "define/src/com/sun/squawk/vm/ClassNumbers.java define/src/com/sun/squawk/vm/SquawkOpcodes.java ";
    static final String preverifierPrefix = "tools/preverify";
    static boolean gui;
    static boolean demoMode = false;

    private final Compiler compiler;
    private final String preverifier;
    private final String   cmd;
    private final String[] cmdArgs;
    private final PrintStream stdout;
    private final PrintStream stderr;

    boolean verbose  = false;
    boolean useShell = true;

    boolean nofp = true;
    boolean msc = false;
    boolean gcc = false;
    boolean cc = false;
    boolean o2 = false;
    private Process proc;

    /*
     * find
     */
    public String find(String dir) throws Exception {
        return find(dir, ".java");
    }

    /*
     * find
     */
    public String find(String dir, String type) throws Exception {
        StringBuffer sb = new StringBuffer();
        Vector vec = new Vector();
        find(new File(dir), type, vec);

        for (Enumeration e = vec.elements(); e.hasMoreElements();){
            File f = (File)e.nextElement();
            sb.append(" ");
            sb.append(f.getPath());

        }
        return sb.toString();
    }

    /*
     * find
     */
    public void find(File dir, String type, Vector vec) throws Exception {
        String[] list = dir.list();
        File[] files = dir.listFiles();
        for(int i = 0 ; i < files.length ; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                find(f, type, vec);
            } else {
                if (f.getName().endsWith(type)) {
                    vec.addElement(f);
                }
            }
        }
    }


    /*
     * clean
     */
    public void clean(File dir, String type) throws Exception {
        String[] list = dir.list();
        File[] files = dir.listFiles();
        for(int i = 0 ; i < files.length ; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                clean(f, type);
            } else {
                if (f.getName().endsWith(type)) {
                    f.delete();
                }
            }
        }
    }

    /*
     * main
     */
    public static void main(String[] args) throws Exception {
        Build instance = new Build(args, System.out, System.err);
        if (instance.cmd != null) {
            // Not running GUI
            instance.run();
        }

    }

    public Build(String[] args, PrintStream stdout, PrintStream stderr) throws Exception {

        this.stdout = stdout;
        this.stderr = stderr;
        System.setOut(stdout);
        System.setErr(stderr);

        String osName = System.getProperty("os.name");
        System.out.println("os.name="+osName);
        String jversion = System.getProperty("java.version");
        System.out.println("java.version="+jversion);

        if(osName.startsWith("Linux")) {
            preverifier = preverifierPrefix + ".linux";
            exec("chmod +x "+preverifier);
        } else if (!osName.startsWith("Windows")) {
            preverifier = preverifierPrefix + ".solaris";
            exec("chmod +x "+preverifier);
        }
        else {
            preverifier = preverifierPrefix;
        }
        int argp = 0;
        boolean runGui = false;
        while (args.length > argp) {
            String arg = args[argp];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-gui")) {
                 args[argp] = null;
                 runGui = true;
            } else if (arg.equals("-demogui")) {
                 args[argp] = null;
                 runGui = true;
                 demoMode = true;
            } else if (arg.equals("-msc")) {
                msc = true;
            } else if (arg.equals("-gcc")) {
                gcc = true;
            } else if (arg.equals("-cc")) {
                cc = true;
            } else if (arg.equals("-o2")) {
                o2 = true;
            } else if (arg.equals("-fp")) {
                nofp = false;
            } else if (arg.equals("-noshell")) {
                useShell = false;
            } else if (arg.equals("-verbose")) {
                verbose = true;
            } else {
                System.out.println("Unknown option "+arg);
                System.exit(1);
            }
            argp++;
        }
        if (runGui) {
            System.out.println("Running gui..");
            gui = true;
            Gui.main(this);
            return;
        }

        Compiler theCompiler = null;
        try {
            Class test = Class.forName("com.sun.tools.javac.v8.Main");
            theCompiler = new InternalCompiler();
        } catch(ClassNotFoundException ex) {
            theCompiler = new ExternalCompiler();
        }
        compiler = theCompiler;

        String   theCmd     = "all";
        String[] theCmdArgs = null;
        if (args.length > argp) {
            theCmd = args[argp++];
            theCmdArgs = new String[args.length - argp];
            if (theCmdArgs.length > 0) {
                System.arraycopy(args,argp,theCmdArgs,0,theCmdArgs.length);
            }
        }
        cmd     = theCmd;
        cmdArgs = theCmdArgs;
    }

    public void run () throws Exception {
        int argp = 0;

        // Running things...

        if (cmd.equals("clean") || cmd.equals("all")) {
             stdout.println("Cleaning...");
             clean(new File("."), ".class");
             clean(new File("vm/bld"), ".c");
             clean(new File("vm/bld"), ".h");
             clean(new File("vm/bld"), ".exe");
             clean(new File("vm/bld"), ".obj");
             if (cmd.equals("clean")) {
                stdout.println("Finished cleaning.");
                return;
             }
        }

        if (cmd.equals("traceviewer")) {
            stdout.println("Running traceviewer...");
            String options = "";
            while (cmdArgs.length > argp) {
                options += " "+cmdArgs[argp++];
            }
            exec("java -cp j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar com.sun.squawk.traceviewer.TraceViewer", options);
            return;
        }

        if (cmd.equals("brazil")) {
            String options = "";
            String javaOptions = "";
            String classpath = "j2me/classes;translator/classes;samples/classes";
            while (cmdArgs.length > argp) {
                String arg = cmdArgs[argp++];
                if (arg.equals("-classpath")) {
                    if (argp == cmdArgs.length) {
                        stderr.println("Missing argument to -classpath option: using default");
                    }
                    else {
                        classpath = cmdArgs[argp++];
                    }
                }
                else
                if (arg.startsWith("-J")) {
                    javaOptions += " " + arg.substring(2);
                }
                else {
                    options += "+"+arg;
                }
            }
            if (options.length() > 0) {
                options = " options "+options;
            }

            stdout.println("Running translator on classpath "+classpath+" ...");
            exec("java -cp translator/classes;j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar sunlabs.brazil.server.Main -l 2 -port 9090 -handler com.sun.squawk.translator.BrazilHandler", "classpath "+fix(classpath)+ options);
            return;
        }

        if (cmd.equals("translate")) {
          stdout.println("Running translator...");
          String options = join(cmdArgs, 0, cmdArgs.length, " ");
          exec("java -Xbootclasspath~a:j2se/classes;j2me/classes;translator/classes com.sun.squawk.translator.VirtualMachine", options);
          return;
        }

        if (cmd.equals("braziljcard")) {
            stdout.println("Running braziljcard...");
            String options = "";
            while (cmdArgs.length > argp) {
                options += "+"+cmdArgs[argp++];
            }
            if (options.length() > 0) {
                options = " options "+options;
            }

            exec("java -cp translator/classes;j2se/classes;j2me/classes;j2se/brazil-1.1-min.jar sunlabs.brazil.server.Main -l 2 -port 9090 -handler com.sun.squawk.translator.BrazilHandler", "classpath jcard/classes"+ options);
            return;
        }


        if (cmd.equals("stopbrazil")) {
            stdout.println("Stopping translator...");
            try {
                URL url = new URL("http://localhost:9090/terminate");
                URLConnection conn = url.openConnection();
                conn.connect();
                conn.getInputStream();
            } catch (Exception ex) {
            }
            return;
        }

        if (cmd.equals("testgc")) {
            stdout.println("Running testgc...");
            //exec("java -cp j2se/classes;vm/classes com.sun.squawk.vm.ObjectMemoryTester");
            exec("java -classpath vm/classes com.sun.squawk.vm.ObjectMemoryTester");
            return;
        }

        if (cmd.equals("runxencode")) {
            stdout.println("Running runxencode...");
            (new File("xencode.tmp")).delete();
            exec("java -Xbootclasspath~a:j2se/classes;xencode/classes;vmboot/classes;vm/classes;j2me/classes java.lang.VMPlatform", "j2me/classes >xencode.tmp");
            return;
        }

        if (cmd.equals("runxencodejcard")) {
            stdout.println("Running runxencodejcard...");
            (new File("xencode.tmp")).delete();
            exec("java -Xbootclasspath~a:j2se/classes;xencode/classes;vmboot/classes;vm/classes;j2me/classes java.lang.VMPlatform", "jcard/classes >xencode.tmp");
            return;
        }


        if (cmd.equals("Count3")) {
            stdout.println("Running Count3...");
            exec("java -cp xencode/classes com.sun.squawk.analysis.Count3", "<xencode.tmp");
            return;
        }

        if (cmd.equals("Count4")) {
            stdout.println("Running Count4...");
            exec("java -cp xencode/classes com.sun.squawk.analysis.Count4 <xencode.tmp");
            return;
        }

        if (cmd.equals("squawk")) {
            stdout.println("Running squawk...");
            String options = "";
            String javaOptions = "";
            while (cmdArgs.length > argp) {
                String opt = cmdArgs[argp++];
                if (opt.startsWith("-J")) {
                    javaOptions += " " + opt.substring(2);
                }
                else {
                    options += " "+opt;
                }
            }
            exec("java -Xbootclasspath~a:j2se/classes;vm/classes;j2me/classes"+javaOptions+" com.sun.squawk.vm.Interpreter", options);
            return;
        }

        if (cmd.equals("romizer")) {
            stdout.println("Running romizer...");
            String options = "";
            String javaOptions = "";
            while (cmdArgs.length > argp) {
                String opt = cmdArgs[argp++];
                if (opt.startsWith("-J")) {
                    javaOptions += " " + opt.substring(2);
                }
                else {
                    options += " "+opt;
                }
            }
            exec("java -Xbootclasspath~a:j2se/classes;vmboot/classes;vm/classes;j2me/classes"+javaOptions+" java.lang.VMPlatform", options);
            return;
        }



        // Building things...

        if (cmd.equals("j2me") || cmd.equals("all")) {
            stdout.println("Building j2me...");
            javac_j2me(null, "j2me", defines + find("j2me"));
            javac_j2me("j2me/classes", "samples", find("samples"));
        }

        if (cmd.equals("jcard") || cmd.equals("all")) {
            stdout.println("Building jcard...");
            javac_j2me(null, "jcard", defines + find("jcard"));
        }

        if (cmd.equals("translator") || cmd.equals("all")) {
            stdout.println("Building translator...");
            javac_j2me("j2me/classes;", "translator", defines + find("translator"));
        }

        if (cmd.equals("j2se") || cmd.equals("all")) {
            stdout.println("Building j2se...");
            javac_j2se("j2me/classes;translator/classes;j2se/brazil-1.1-min.jar", "j2se", defines + find("j2se"));
        }

        if (cmd.equals("vm") || cmd.equals("all")) {
            stdout.println("Building vm...");
            javac_j2se("j2se/classes;j2me/classes;", "vm", defines + find("vm"));
            j2c(defines + find("vm"), "vm/bld");

            if (msc) {
                String tempName = "temp.bat";
                new File(tempName).delete();
                PrintStream out = new PrintStream(new FileOutputStream(tempName));
                out.println("cd vm\\bld");
                if (o2) {
                    out.println("cl /nologo /W3 /O2 /Ob2 squawk.c");
                } else {
                    out.println("cl /nologo /W3 /O1 squawk.c");
                }
                out.println("cd ..\\..");
                out.close();
                exec(tempName);
                new File(tempName).delete();
            }

            if (cc) {
                String tempName = "temp.sh";
                new File(tempName).delete();
                PrintStream out = new PrintStream(new FileOutputStream(tempName));
                out.println(fix("cd vm/bld"));
                out.println("cc -o squawk squawk.c");
                out.println(fix("cd ../.."));
                out.close();
                exec("sh "+tempName);
                new File(tempName).delete();
            }

            if (gcc) {
                String tempName = "temp.bat";
                new File(tempName).delete();
                PrintStream out = new PrintStream(new FileOutputStream(tempName));
                out.println(fix("cd vm/bld"));
                out.println("gcc -o squawk -O2 squawk.c");
                out.println(fix("cd ../.."));
                out.close();
                if(System.getProperty("os.name" ).startsWith("Windows")) {
                    exec(tempName);
                } else {
                    exec("sh "+tempName);
                }
                new File(tempName).delete();
            }

        }

        if (cmd.equals("vmboot") || cmd.equals("all")) {
            stdout.println("Building vmboot...");
            javac_j2se("j2se/classes;j2me/classes;vm/classes", "vmboot", defines + find("vmboot"));
        }

        if (cmd.equals("tck") || cmd.equals("all")) {
            stdout.println("Building tck...");
            javac_j2me("j2me/classes", "tck", find("tck"));
        }

        if (cmd.equals("xencode") || cmd.equals("all")) {
            stdout.println("Building xencode...");
            javac_j2se("j2se/classes;j2me/classes;vm/classes", "xencode", defines + find("xencode"));
        }
    }

    /*
     * javac
     */
    public int javac(String classPath, String dir, String outdir, String files) throws Exception {
        return compiler.compile(classPath+" -g -d "+dir+"/"+outdir+" "+files,verbose);
    }

    /*
     * javac_j2se
     */
    public int javac_j2se(String classPath, String dir, String files) throws Exception {
        if (classPath != null) {
            classPath = "-classpath "+classPath;
        } else {
            classPath = "";
        }
        return javac(classPath, dir, "classes", files);
    }

    /*
     * javac_j2me
     */
    public int javac_j2me(String classPath, String dir, String files) throws Exception {
        if (classPath != null) {
            classPath = "-bootclasspath "+classPath;
        } else {
            classPath = "";
        }
        int res = javac(classPath, dir, "tmpclasses", files);


        if (res == 0) {
            if (dir.equals("j2me") || dir.equals("jcard")) {
                return exec(preverifier+" -d "+dir+"/classes "+dir+"/tmpclasses");
            } else {
                return exec(preverifier+" -classpath j2me/classes -d "+dir+"/classes "+dir+"/tmpclasses");
            }
        }
        return res;
    }

    /*
     * fix
     */
    public String fix(String str) {
        str = str.replace(';', File.pathSeparatorChar);
        str = str.replace('/', File.separatorChar);
        str = str.replace('~', '/');
        return str;
    }

    /*
     * cut
     */
    public String[] cut(String str, int preambleSize) {
        StringTokenizer st = new StringTokenizer(str, " ");
        String res[] = new String[st.countTokens()+preambleSize];
        while (st.hasMoreTokens()) {
            res[preambleSize++] = st.nextToken();
        }
        return res;
    }

    /*
     * cut
     */
    public String[] cut(String str) {
        return cut(str, 0);
    }

    public static String join(String[] parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts[i]);
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /*
     * exec
     */
    public int exec(String cmd) throws Exception {
        return exec(cmd,"");
    }
    /*
     * exec
     */
    public int exec(String cmd, String options) throws Exception {
        if (useShell) {
            return exec(cmd, options, stdout, stderr, null);
        }
        else {
            String outFileName = null, inFileName = null;
            // Look for '>' redirect
            int index = options.lastIndexOf('>');
            if (index != -1) {
                StringTokenizer st = new StringTokenizer(options.substring(index+1)," \t<>;[]");
                outFileName = st.nextToken();
//System.err.println("redirecting output to: "+outFileName);
                // remove the redirect from the options
                String newOptions = options.substring(0,index);
                newOptions += options.substring(options.indexOf(outFileName,index)+outFileName.length());
                options = newOptions;
            }
            // Look for '<' redirect
            index = options.lastIndexOf('<');
            if (index != -1) {
                StringTokenizer st = new StringTokenizer(options.substring(index+1)," \t<>;[]");
                inFileName = st.nextToken();
//System.err.println("redirecting input from: "+inFileName);
                // remove the redirect from the options
                String newOptions = options.substring(0,index);
                newOptions += options.substring(options.indexOf(inFileName,index)+inFileName.length());
                options = newOptions;
            }
//System.err.println("cmd: "+cmd);
            return exec(cmd, options, outFileName, null, inFileName);
        }
    }
    /**
     * Execute a command.
     * @param outFileName Name of file to which stdout should be redirected or null for no redirection.
     * @param errFileName Name of file to which stderr should be redirected or null for no redirection.
     * @param inFileName  Name of file from which stdin should be redirected or null for no redirection.
     */
    public int exec(String cmd, String options, String outFileName, String errFileName, String inFileName) throws Exception {
        if (verbose) {
            stderr.println("EXEC: "+cmd);
        }
        PrintStream out = stdout, err = stderr;
        InputStream in = null;
        File outFile = null, errFile = null, inFile = null;
        if (outFileName != null) {
            outFile = new File(outFileName);
            outFile.delete();
            out = new PrintStream(new FileOutputStream(outFile));
        }
        if (errFileName != null) {
            errFile = new File(errFileName);
            errFile.delete();
            err = new PrintStream(new FileOutputStream(errFile));
        }
        if (inFileName != null) {
            inFile = new File(inFileName);
            in = new FileInputStream(inFile);
        }
        try {
            return exec(cmd, options, out, err, in);
        } finally {
            if (out != null && outFile != null) out.close();
            if (err != null && errFile != null) err.close();
            if (in  != null &&  inFile != null) in.close();
        }
    }
    public int exec(String cmd, String options, PrintStream out, PrintStream err, InputStream in) throws Exception {
        cmd = fix(cmd) + " " + options;
        if (verbose) {
            stderr.println("EXEC: "+cmd);
        }

        String[] shellCmd = null;
        if (useShell) {
            String osName = System.getProperty("os.name" );
            if(osName.startsWith("Windows 9")) {
                shellCmd = cut(cmd, 2);
                shellCmd[0] = "command.com" ;
                shellCmd[1] = "/C" ;
            } else if(osName.startsWith("Windows")) {
                shellCmd = cut(cmd, 2);
                shellCmd[0] = "cmd.exe" ;
                shellCmd[1] = "/C" ;
            } else {
                //shellCmd = cut(cmd, 2);
                shellCmd = new String[3];
                shellCmd[0] = "/bin/sh" ;
                shellCmd[1] = "-c" ;
                shellCmd[2] = cmd;
            }
        }

//for (int i = 0 ; i < args.length ; i++) {
//   System.out.print(args[i]+" ");
//}
//System.out.println();

        try {
            if (useShell) {
                proc = Runtime.getRuntime().exec(shellCmd);
            }
            else {
                proc = Runtime.getRuntime().exec(cmd);
            }
            if (in != null) {
                StreamProducer stdinProducer = new StreamProducer(proc.getOutputStream(), in);
                stdinProducer.start();
            }
            StreamGobbler errorGobbler  = new StreamGobbler(proc.getErrorStream(), err, "");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), out, "");
            errorGobbler.start();
            outputGobbler.start();

            int exitVal = proc.waitFor();
            if (verbose || exitVal != 0 /*|| gui*/) {
                stderr.println("EXEC result =====> " + exitVal);
            }
            return exitVal;
        } catch (InterruptedException ie) {
            return -1;
        } finally {
            // Ensure that the native process (if any is killed).
            if (proc != null) {
                proc.destroy();
                proc = null;
            }
        }
    }

    /*
     * j2c
     */
    public void j2c(String filelist, String outdir) throws Exception {
        String[] files = cut(fix(filelist));
        for (int i = 0 ; i < files.length ; i++) {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(files[i]));
            String line = getLine(isr);
            if (line.startsWith("//J2C:")) {
                line = line.substring(6);
                int index = line.indexOf(' ');
                if (index > 0) {
                    line = line.substring(0, index);
                }

                FileOutputStream fos = new FileOutputStream(fix(outdir+"/"+line));
                PrintStream out = new PrintStream(fos);
                out.println("/**** Created by Squawk builder from \""+files[i]+"\" ****/");

                while ((line = getLine(isr)) != null) {
                    if (line.startsWith("/*IFJ*/")) {
                        line = "/**** Line deleted by Squawk builder ****/";
                    } else if (line.startsWith("//IFC//")) {
                        line = line.substring(7);
                        if (line.length() > 0 && line.charAt(0) != '#') {
                            out.print("       ");
                        }
                    }
                    if (nofp && line.startsWith("/*FLT*/")) {
                        line = "/**** Line deleted by Squawk builder ****/";
                    }
                    writeLine(out, line);
                }
                out.close();
                fos.close();
            }
        }
    }

    /*
     * getLine
     */
    String getLine(InputStreamReader isr) throws Exception {
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

    /*
     * writeLine
     */
    void writeLine(PrintStream out, String line) throws Exception {
        boolean isDefine = line.length() > 0 && line.charAt(0) == '#';
        String delims = " \t\n\r(){/";
        StringTokenizer st = new StringTokenizer(line, delims, true);
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!isDefine) {
                if (next.equals("ulong")) {
                    next = "ujlong";
                } else if (next.equals("long")) {
                    next = "jlong";
                }
            }
            if (next.charAt(0) != '\r' && next.charAt(0) != '\n') {
                out.print(next);
            }
        }
        out.println();
    }

/*---------------------------------------------------------------------------*\
 *                                 Compiler                                  *
\*---------------------------------------------------------------------------*/

    abstract class Compiler {
        abstract public int compile(String cmd, boolean verbose) throws Exception;
    }

/*---------------------------------------------------------------------------*\
 *                              InternalCompiler                             *
\*---------------------------------------------------------------------------*/

    class InternalCompiler extends Compiler {
        public int compile(String cmd, boolean verbose) throws Exception {
            PrintStream out = System.out;
            PrintStream err = System.err;
            com.sun.tools.javac.v8.Main c = null;
            String javaVersion = System.getProperty("java.version");
            if (javaVersion.startsWith("1.3")) {
                c = new com.sun.tools.javac.v8.Main("javac", false);
            }
            else
            if (javaVersion.startsWith("1.4")) {
                c = new com.sun.tools.javac.v8.Main("javac");
            }
            else {
                throw new RuntimeException("Internal javac interface for Java"+javaVersion);
            }
            cmd = fix(cmd);
            if (verbose) {
                stdout.println("JAVAC: "+cmd);
            }
            try {
                String[] args = cut(cmd, 0);
//                String[] args = cut(cmd, 0);
//                args[0] = "-target";
//                args[1] = "1.2";
                int res = c.compile(args);
                if (verbose || gui) {
                    stdout.println("JAVAC result =====> "+res);
                }
                return res;
            } finally {
                System.setOut(out);
                System.setOut(err);
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                              ExternalCompiler                             *
\*---------------------------------------------------------------------------*/

    class ExternalCompiler extends Compiler {
        public int compile(String cmd, boolean verbose) throws Exception {
            cmd = fix(cmd);
            String tempName = "javacinput.tmp";
            new File(tempName).delete();
            PrintStream out = new PrintStream(new FileOutputStream(tempName));
            out.println(cmd);
            out.close();
            int res = exec("javac @"+tempName);
            new File(tempName).delete();
            return res;
        }
    }

}
