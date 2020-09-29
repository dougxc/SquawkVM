package com.sun.squawk.translator;
import java.io.*;
import java.util.*;
import sunlabs.brazil.server.*;
import com.sun.squawk.util.Find;
import com.sun.squawk.translator.loader.LinkageException;



public class BrazilHandler implements Handler {
    Server server;

    /**
     * The base prefix to form requests for this handler.
     */
    String prefix;

    /**
     * Prefix used to form "get class number from class name" requests.
     */
    String lookupPrefix;

    /**
     * Prefix used to form "get class info" requests.
     */
    String classPrefix;

    /**
     * Prefix used to form "get class methods" requests.
     */
    String methodsPrefix;

    /**
     * Prefix used to form "get class info and methods" requests.
     */
    String squawkPrefix;

    /**
     * Prefix used to stop the server
     */
    String terminatePrefix;

    /**
     * Property name used to set lookupPrefix.
     */
    public final static String LOOKUP_PROPERTY = "lookupPrefix";

    /**
     * Property name used to set classPrefix.
     */
    public final static String CLASS_PROPERTY = "classPrefix";

    /**
     * Property name used to set methodsPrefix.
     */
    public final static String METHODS_PROPERTY = "methodsPrefix";

    /**
     * Property name used to set squawkPrefix.
     */
    public final static String SQUAWK_PROPERTY = "squawkPrefix";

    /**
     * Property name used to set class path.
     */
    public final static String TERMINATE_PROPERTY = "terminate";

    /**
     * Property name used to set class path.
     */
    public final static String CLASSPATH_PROPERTY = "classpath";

    /**
     * Option flags
     */
    public final static String OPTIONS = "options";

    VirtualMachine vm;



    /*
     * cut
     */
    public static String[] cut(String str, int preambleSize) {
        StringTokenizer st = new StringTokenizer(str, " ");
        String res[] = new String[st.countTokens()+preambleSize];
        while (st.hasMoreTokens()) {
            res[preambleSize++] = st.nextToken();
        }
        return res;
    }

    /*
     * removeArg
     */
    String removedArg;
    public String[] removeArg(String[] args, String name, boolean hasValue) {
        Vector res = new Vector();
        for (int i = 0; i != args.length; i++) {
            if (args[i] != null && args[i].equals(name)) {
                if (hasValue) {
                    removedArg = args[++i];
                }
            }
            else {
                res.addElement(args[i]);
            }
        }
        if (res.size() == 0) {
            return args;
        }
        String[] newArgs = new String[res.size()];
        res.copyInto(newArgs);
        return newArgs;
    }

    /**
     * init
     */
    public boolean init(Server server, String prefix) {
        return init(server,prefix,true);
    }
    public boolean init(Server server, String prefix, boolean startVM) {
        this.server = server;
        this.prefix = prefix;

        lookupPrefix    = server.props.getProperty(prefix + LOOKUP_PROPERTY,        "/lookup/");
        classPrefix     = server.props.getProperty(prefix + CLASS_PROPERTY,         "/class/");
        methodsPrefix   = server.props.getProperty(prefix + METHODS_PROPERTY,       "/methods/");
        squawkPrefix    = server.props.getProperty(prefix + SQUAWK_PROPERTY,        "/squawk/");
        terminatePrefix = server.props.getProperty(prefix + TERMINATE_PROPERTY,     "/terminate");
        String path     = server.props.getProperty(prefix + CLASSPATH_PROPERTY,     "/classes");

        String options  = server.props.getProperty(prefix + OPTIONS, "");
        options = options.replace('+', ' ');

//System.out.println("options='"+options+"'");


        //log("classpath="+path);

        if (!startVM) {
            return true;
        }
        try {
            // Process args and extract those intended for this level of the translator
            String[] args   = cut(options + " DUMMY_MAINCLASS", 2);
            String[] vmArgs = new String[args.length];
            int vmArg = 0;
            String preload = null;
            vmArgs[vmArg++] = "-cp";
            vmArgs[vmArg++] = path;

            for (int i = 0; i != args.length; i++) {
                String arg = args[i];
                if (arg == null) {
                    continue;
                }
                if (arg.equals("-cachedir")) {
                    cacheDir = args[++i];
                }
                else
                if (arg.equals("-preload")) {
                    preload = args[++i];
                }
                else {
                    vmArgs[vmArg++] = arg;
                }
            }

            // Resize vmArgs
            Object old = vmArgs;
            vmArgs = new String[vmArg];
            System.arraycopy(old, 0, vmArgs, 0, vmArg);

            vm = new VirtualMachine(vmArgs);

            if (preload != null) {
                Vector classes = new Vector();
                Find.findAllClassesInPath(preload, classes);
                for (Enumeration e = classes.elements(); e.hasMoreElements(); ) {
                    String name = (String)e.nextElement();
                    try {
                        vm.start(getInternalClassName(name), null, null);
                        System.out.println("preloaded: " + name);
                    } catch (Exception ex) {
                        System.out.println(ex + " while preloading " + name);
                    }
                }
            }

            log("initialized VirtualMachine" + prefix);
            return true;
        } catch(Exception ex) {
            log("Failed to initialize VirtualMachine" + ex);
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * respond
     */
    public boolean respond(Request request) throws IOException {
        if (!request.method.equals("GET")) {
            log("Skipping request, only GET's allowed" + request.url);
            return false;
        }

        String requestUrl = request.url;
System.out.println("GET "+ requestUrl);

        if (requestUrl.startsWith(terminatePrefix)) {
            System.out.println("Server exiting...");
            System.exit(1);
        }
        if (requestUrl.startsWith(lookupPrefix)) {
            Hashtable queryData = request.getQueryData();
            String forceLoad = (String)queryData.get("forceLoad");
            requestUrl = requestUrl.substring(lookupPrefix.length());
            return getClassNumberForName(request, requestUrl,forceLoad == null || forceLoad.equals("true"));
        }
        if (requestUrl.startsWith(classPrefix)) {
            requestUrl = requestUrl.substring(classPrefix.length());
            int cno = parseClassNameOrNumber(request,requestUrl);
            if (cno == -1) {
                return true;
            }
            return getClassInfo(request, cno, false, true);
        }
        if (requestUrl.startsWith(methodsPrefix)) {
            requestUrl = requestUrl.substring(methodsPrefix.length());
            int cno = parseClassNameOrNumber(request,requestUrl);
            if (cno == -1) {
                return true;
            }
            return getClassInfo(request, cno, true, false);
        }
        if (requestUrl.startsWith(squawkPrefix)) {
            requestUrl = requestUrl.substring(squawkPrefix.length());
            int cno = parseClassNameOrNumber(request,requestUrl);
            if (cno == -1) {
                return true;
            }
            return getClassInfo(request, cno, true, true);
        }
        log("Not my prefix: " + requestUrl);
        return false;
    }

    public String getInternalClassName(String name) {
        int depth = name.lastIndexOf('[');
        if (depth != -1) {
            depth++;
            return name.substring(0,depth)+"L"+name.substring(depth)+";";
        }
        else {
            return "L"+name+";";
        }
    }

    /**
     * This method is used to service requests whose sole purpose is to convert
     * a class name to a class number.
     */
    public boolean getClassNumberForName(Request request, String name, boolean forceLoad) throws IOException {
        if (!forceLoad && convertClassNameToNumber(request,name,forceLoad) == -1) {
            return true;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16*1024);
        PrintStream out = new PrintStream(baos);
        try {
            vm.start(getInternalClassName(name), out, "lookup");
            out.close();
            sendResponse(request, baos.toString());
        } catch(Exception ex) {
            ex.printStackTrace();
            request.sendError(404,"Invalid class: " + name);
            return false;
        }
        return true;
    }

    /**
     * getClassInfo
     */
    public boolean getClassInfo(Request request, int cno, boolean methods, boolean info) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16*1024);
        PrintStream out = new PrintStream(baos);

        try {
            vm.start(vm.findType(cno), out, (methods ? "methods" : "")+(info ? "class" : ""));
            out.close();
            sendResponse(request, baos.toString());
        } catch(Exception ex) {
            ex.printStackTrace();
            request.sendError(404,"Invalid class: " + cno);
            return true;
        }
        return true;
    }

    int parseClassNameOrNumber(Request request, String nameOrNumber) throws IOException {
        try {
            return Integer.parseInt(nameOrNumber);
        } catch (NumberFormatException nfe) {
            return convertClassNameToNumber(request,nameOrNumber,true);
        }
    }

    /**
     * Attempt to convert a class name to a class number.
     */
    int convertClassNameToNumber(Request request, String name, boolean mustExist) {
        try {
            String internalName = getInternalClassName(name);
            Type type = vm.findType(getInternalClassName(name));
            if (type == null && mustExist) {
                vm.start(internalName, null, null);
                type = vm.findType(getInternalClassName(name));
            }
            return type.getID();
        } catch(Exception ex) {
            if (mustExist) {
ex.printStackTrace();
                request.sendError(404,"Invalid class name: " + name);
            } else {
                request.sendError(404,"Unknown/invalid class name: " + name);
            }
            return -1;
        }
    }

    String cacheDir;
    void sendResponse(Request request, String response) throws IOException {
        if (cacheDir != null) {
            File file = new File(cacheDir + request.url);
            File dir =  file.getParentFile();
            dir.mkdirs();
            if (dir.isDirectory()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(response.getBytes());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        }
        request.sendResponse(response, "text/plain");
    }

    /**
     * log
     */
    protected void log(String message) {
        //System.out.println("\n\n\n\n"+message+"\n\n\n\n");
        server.log(Server.LOG_INFORMATIONAL, prefix, message);
    }
}

