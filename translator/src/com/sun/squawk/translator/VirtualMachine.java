
package com.sun.squawk.translator;
import  com.sun.squawk.util.*;
import  com.sun.squawk.translator.util.*;
import  com.sun.squawk.translator.loader.*;
import  com.sun.squawk.translator.ir.*;
import  com.sun.squawk.translator.xml.*;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

public class VirtualMachine extends BaseFunctions {


    final public static boolean  LONGSARETWOWORDS = false;


   /* ------------------------------------------------------------------------ *\
    *                                C l a s s                                 *
   \* ------------------------------------------------------------------------ */

   /*
    * Empty lists
    */
    final public static Type[]   ZEROTYPES   = new Type[0];
    final public static Field[]  ZEROFIELDS  = new Field[0];
    final public static Method[] ZEROMETHODS = new Method[0];
    final public static Object[] ZEROOBJECTS = new Object[0];

   /**
    * Parent of this virtual machines
    */
    private static VirtualMachine topLevelVirtualMachine;

    public static VirtualMachine TopVM() {
        return topLevelVirtualMachine;
    }

    public static String MAIN, INIT, CLINIT, SQUAWK_MAIN, SQUAWK_INIT, SQUAWK_CLINIT, SQUAWK_DUMMY;


   /* ------------------------------------------------------------------------ *\
    *                              I n s t a n c e                             *
   \* ------------------------------------------------------------------------ */


   /**
    * The class loader for this VM
    */
    private ClassFileLoader loader;

   /**
    * Parent of this virtual machines
    */
    private VirtualMachine parentVirtualMachine;

   /**
    * Parent of this virtual machines
    */
    private SpecialTransformer specialTransformer;

   /**
    * Collection of all children of this virtual machines
    */
    private Vector childVirtualMachines = new Vector();

   /**
    * The class loading path
    */
    private String classPath = ".";

   /**
    * The name of the main class
    */
    private String[] classNames;

   /**
    * The type of the main class
    */
//    private Type mainClass;

   /**
    * Command line arguments
    */
//    private String[] args;

   /**
    * Offset to the next unused class number
    */
    private int nextClassNumber = 1;

   /**
    * Slot number of first interface slot
    */
    public final static int FIRSTINTERFACE = 10000;

   /**
    * Offset to the next unused interface slot
    */
    private int nextFreeInterfaceMethod = FIRSTINTERFACE+1;

   /**
    * The level of compiler optomization to be used
    */
    private int optimizationLevel = 1;

   /**
    * The maximum number of exceptions that the VM may throw, or zero for infinite
    */
    private int maxExceptions = 0;

    private PrintStream out = System.out;

   /*
    *
    */
    private void setMaxExceptions(String number) {
        try {
            maxExceptions = Integer.parseInt(number);
        } catch(NumberFormatException ex) {
            System.out.println("Bad -maxexceptions");
            System.exit(-1);
        }
    }

   /*
    * getMaxExceptions()
    */
    public int getMaxExceptions() {
        return maxExceptions;
    }


   /**
    * Flags
    */
    final static int TRACELOADING      = 1<<0;
    final static int TRACEPOOL         = 1<<1;
    final static int TRACEBYTECODES    = 1<<2;
    final static int TRACEIR0          = 1<<3;
    final static int TRACEIR1          = 1<<4;
    final static int TRACEFIELDS       = 1<<5;
    final static int EAGERLOADING      = 1<<6;
    final static int TRACELOCALS       = 1<<7;
    final static int TRACEIP           = 1<<8;
    final static int VERBOSE           = 1<<9;
    final static int TRACEEXEC         = 1<<10;
    final static int TRACEEXCEPTIONS   = 1<<11;
    final static int TRACEEVENTS       = 1<<12;
    final static int TRACEGRAPHICS     = 1<<13;
    final static int TRACETHREADS      = 1<<14;
    final static int TRACEINSTRUCTIONS = 1<<15;
    final static int ALLOWNATIVES      = 1<<16;


    private int flags = 0;
    private String match;

    String prepData(String data) {
        data = data.replace('/', '.');
        data = data.replace('\\', '.');
        return data;
    }

    private boolean matches(String matchData) {
        if (match == null) {
           return true;
        }
        matchData = prepData(matchData);
        return matchData.indexOf(match) >= 0;
    }

    public boolean traceloading(String matchData) {
        return (flags & TRACELOADING) != 0 && matches(matchData);
    }

    public boolean tracepool(String matchData) {
        return (flags & TRACEPOOL) != 0 && matches(matchData);
    }

    public boolean tracebytecodes(String matchData) {
        return (flags & TRACEBYTECODES) != 0 && matches(matchData);
    }

    public boolean traceir0(String matchData) {
        return (flags & TRACEIR0) != 0 && matches(matchData);
    }

    public boolean traceir1(String matchData) {
        return (flags & TRACEIR1) != 0 && matches(matchData);
    }

    public boolean tracefields(String matchData) {
        return (flags & TRACEFIELDS) != 0 && matches(matchData);
    }

    public boolean traceip(String matchData) {
        return (flags & TRACEIP) != 0 && matches(matchData);
    }

    public boolean eagerloading() {
        return (flags & EAGERLOADING) != 0;
    }

    public boolean tracelocals(String matchData) {
        return (flags & TRACELOCALS) != 0 && matches(matchData);
    }

    public boolean traceexec() {
        return (flags & TRACEEXEC) != 0;
    }

    public boolean traceexceptions() {
        return (flags & TRACEEXCEPTIONS) != 0;
    }

    public boolean traceevents() {
        return (flags & TRACEEVENTS) != 0;
    }

    public boolean tracegraphics() {
        return (flags & TRACEGRAPHICS) != 0;
    }

    public boolean tracethreads() {
        return (flags & TRACETHREADS) != 0;
    }

    public boolean traceinstructions() {
        return (flags & TRACEINSTRUCTIONS) != 0;
    }

    public boolean verbose() {
        return (flags & VERBOSE) != 0;
    }

    public boolean allowNatives() {
        return (flags & ALLOWNATIVES) != 0;
    }




   /**
    * The main routine
    */
    public static void main(String[] args) throws Throwable {
        new VirtualMachine(args).start();
    }

   /**
    * Constructor for initial VM
    */
    public VirtualMachine(String[] args) throws LinkageException {

        MAIN          = internString("main");
        INIT          = internString("<init>");
        CLINIT        = internString("<clinit>");
        SQUAWK_MAIN   = internString("_SQUAWK_INTERNAL_main");
        SQUAWK_INIT   = internString("_SQUAWK_INTERNAL_init");
        SQUAWK_CLINIT = internString("_SQUAWK_INTERNAL_clinit");
        SQUAWK_DUMMY  = internString("_SQUAWK_INTERNAL_dummy");


        int i = 0;
        for ( ; i < args.length ; i++) {
//System.out.println("A="+     args[i]);
            if (args[i].charAt(0) != '-') {
                break;
            }
            if (args[i].equals("-classpath") || args[i].equals("-cp")) {
                classPath = args[++i];
            } else if (args[i].equals("-maxexceptions")) {
                setMaxExceptions(args[++i]);
            } else if (args[i].equals("-matching")) {
                match = prepData(args[++i]);
            } else if (args[i].equals("-o0")) {
                optimizationLevel = 0;
            } else if (args[i].equals("-o1")) {
                optimizationLevel = 1;
            } else if (args[i].equals("-o2")) {
                optimizationLevel = 2;
            } else if (args[i].equals("-eagerloading")) {
               flags |= EAGERLOADING;
            } else if (args[i].equals("-traceloading")) {
               flags |= TRACELOADING;
            } else if (args[i].equals("-tracefields")) {
               flags |= TRACEFIELDS;
            } else if (args[i].equals("-tracepool")) {
               flags |= TRACEPOOL;
            } else if (args[i].equals("-tracebytecodes")) {
               flags |= TRACEBYTECODES;
            } else if (args[i].equals("-traceir0")) {
               flags |= TRACEIR0;
            } else if (args[i].equals("-traceir1")) {
               flags |= TRACEIR1;
            } else if (args[i].equals("-tracelocals")) {
               flags |= TRACELOCALS;
            } else if (args[i].equals("-traceip")) {
               flags |= TRACEIP;
            } else if (args[i].equals("-verbose")) {
               flags |= VERBOSE;
            } else if (args[i].equals("-traceexec")) {
               flags |= TRACEEXEC;
            } else if (args[i].equals("-traceexceptions")) {
               flags |= TRACEEXCEPTIONS;
            } else if (args[i].equals("-traceevents")) {
               flags |= TRACEEVENTS;
            } else if (args[i].equals("-tracegraphics")) {
               flags |= TRACEGRAPHICS;
            } else if (args[i].equals("-tracethreads")) {
               flags |= TRACETHREADS;
            } else if (args[i].equals("-traceinstructions")) {
               flags |= TRACEINSTRUCTIONS;
            } else if (args[i].equals("-allownatives")) {
               flags |= ALLOWNATIVES;
            } else if (args[i].equals("-o")) {
                String url = args[++i];
                if (url.indexOf("://") == -1) {
                    url = "file://"+url;
                }
                try {
                    out = new PrintStream(Connector.openOutputStream(url));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw fatal("IO error opening "+url);
                }
            } else {
                throw fatal("Bad switch "+args[i]);
            }
        }

        if (i >= args.length) {
          throw fatal("Missing class name(s)");
        }


        Vector classNames = new Vector();
        while (i != args.length) {
            String arg = args[i];
            if (arg.charAt(0) != '@') {
                classNames.addElement(arg);
            } else {
                try {
                    String classListFile = arg.substring(1);
                    InputStream is = Connector.openInputStream("file://"+classListFile);
                    InputStreamReader isr = new InputStreamReader(is);
                    char[] data = new char[is.available()];
                    isr.read(data);
                    String fileData = new String(data);
                    StringTokenizer st = new StringTokenizer(fileData, ";");
                    while (st.hasMoreTokens()) {
                        String className = st.nextToken().trim();
                        if (className.length() == 0 || className.charAt(0) == '#') {
                            continue;
                        } else {
                            classNames.addElement(className);
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
            }
            i++;
        }

        this.classNames = new String[classNames.size()];
        classNames.copyInto(this.classNames);

        topLevelVirtualMachine = this;

        loader = ClassFileLoader.create(this, classPath);

        Type.initialize(this);
        specialTransformer = new SpecialTransformer(this);

    }

   /**
    * Return the SpecialTransformer for this VM
    */
    public SpecialTransformer getSpecialTransformer() {
        return specialTransformer;
    }

   /**
    * The type of the main class
    */
//    public Type mainClass() {
//        return mainClass;
//    }

   /**
    * The main routine
    */
//    public String[] getArgs() {
//        return args;
//    }

   /**
    * Constructor for forked VMs
    */
    public VirtualMachine(VirtualMachine parentVirtualMachine, String mainClassName, String[] args) throws LinkageException {

        parentVirtualMachine.childVirtualMachines.addElement(this);

//        this.mainClassName           = mainClassName;
//        this.args                    = args;
        this.parentVirtualMachine    = parentVirtualMachine;
        this.classPath               = parentVirtualMachine.classPath;
        this.flags                   = parentVirtualMachine.flags;
        this.optimizationLevel       = parentVirtualMachine.optimizationLevel;
        this.nextFreeInterfaceMethod = parentVirtualMachine.nextFreeInterfaceMethod;
        this.nextClassNumber         = parentVirtualMachine.nextClassNumber;

        loader = ClassFileLoader.create(this, classPath);
    }

   /**
    * Load a class into this VM
    */
    public Type load(String className) throws LinkageException {
        return loader.load(className);
    }

   /**
    * Start the VM running
    */
    public void start() throws LinkageException {
        start(classNames, null);
    }

   /**
    * Start the VM running
    */
    public void start(String[] names, String request) throws LinkageException {
        Type[] types = new Type[names.length];
        for (int i = 0; i != names.length; i++) {
            types[i] = createType("L"+names[i].replace('.', '/')+";");
        }
        start(types, request);
    }

   /**
    * Start the VM running
    */
    public void start(Type[] types, String request) {
        for (int i = 0; i != types.length; ++i) {
            Type type = types[i];
            if (type.getLinkageError() == null) {
                try {
                    type.load();
                    type.convert();
                }
                catch (LinkageException le) {
                    type.setLinkageError(le);
                }
            }
        }

        if (out == null) {
            return;
        }
        if (request == null) {
            request = "classmethods";
        }

        for (int i = 0; i != nextClassNumber; ++i) {
            Type type = findType(i);
            if (type == null) {
                out.println("<!-- no type for number "+i+" -->");
                continue;
            }
            xmlHead(type, out);
            if (request.indexOf("class") != -1) {
                type.writeClass(out);
                if (!type.isArray()) {
                    writeMethods(type, out, (request.indexOf("methods") == -1));
                }
            }
            else if (request.equals("methods")) {
                if (!type.isArray()) {
                    writeMethods(type, out, false);
                }
            }
            xmlTail(out);
        }
    }

    /**
     * The cached copy of the generated clinit method for a class that has a linkage error.
     */
    private String linkageErrorXMLMethods;

    /**
     * Write out the single clinit method for a class with a linkage error.
     */
    public void writeMethods(Type aClass, PrintStream out, boolean slotsOnly, LinkageException le) {
        if (linkageErrorXMLMethods == null) {
            /*
             * Find the methods:
             *
             *    java.lang.Class.newInstance(Ljava/lang/Object;I)Ljava/lang/Object;
             *    java.lang.LinkageError._SQUAWK_INTERNAL_init(Ljava/lang/Object;Ljava/lang/String;)V
             *
             */
            try {
                // Ensure the classes have had their methods loaded
                Type.CLASS.load();
                Type.CLASS.convert();
                Type.LINKAGEERROR.load();
                Type.LINKAGEERROR.convert();
            } catch (LinkageException ex) {
                ex.printStackTrace();
                throw new RuntimeException("java.lang.Class and java.lang.LinkageError must be valid");
            }
            Method newInstance = Type.CLASS.findMethod("newInstance", "(I)Ljava/lang/Object;");
            Method constructor = Type.LINKAGEERROR.findMethod("_SQUAWK_INTERNAL_init", "(Ljava/lang/String;)V");
            String newInstance_cno_slot = "-"+newInstance.parent().getID()+"@"+newInstance.getSlotOffset();
            String constructor_cno_slot = constructor.parent().getID()+"@"+constructor.getSlotOffset();
            int linkageErrorId = Type.LINKAGEERROR.getID();

            linkageErrorXMLMethods =
                "      <local_variables>\n" +
                "        <ref/>\n" +                           // Variable for the LinkageError object
                "        <ref/>\n" +                           // Variable for error message String object
                "      </local_variables>\n" +
                "      <parameter_map>\n" +
                "        <from>0</from><to>-1</to>\n" +
                "      </parameter_map>\n" +
                "      <instructions>\n" +
                "        <i>invoker 0 &"+newInstance_cno_slot+" #0 #"+linkageErrorId+"</i>\n" + // Allocate LinkageError instance
                "        <i>ldconst 0 #0</i>\n" +                                               // Load the error message string from the string constants
                "        <i>invokev &"+constructor_cno_slot+" 1 0</i>\n" +                      // Invoke the constructor
                "        <i>throw 0</i>\n" +                                                    // Invoke the constructor
                "        <i>returnv</i>\n" +
                "      </instructions>\n" +
                "    </method>\n";
        }
        if (!slotsOnly) {
            xmlHead("_SQUAWK_INTERNAL_clinit(Ljava/lang/Object;)V", "void _SQUAWK_INTERNAL_clinit(Object)", 1, out);
            out.println(linkageErrorXMLMethods);
        }
        else {
            out.println("    <virtual_methods>1</virtual_methods>");
        }
    }

   /**
    * Write all the methods in XML
    */
    public void writeMethods(Type aClass, PrintStream out, boolean slotsOnly) {
        if (aClass.getLinkageError() != null) {
            writeMethods(aClass, out, slotsOnly, aClass.getLinkageError());
            return;
        }
        try {
            Method[] methods = aClass.getMethods();
            Method m = aClass.getClinit();
            Vector vslots = new Vector();
            Vector nvslots = new Vector();
            if (m != null) {
                if (slotsOnly) {
                    if ((m.isStatic() && !m.name().startsWith("_SQUAWK_INTERNAL_")) || m instanceof MethodProxy) {
                        nvslots.addElement(new Integer(m.getSlotOffset()));
                    } else {
                        vslots.addElement(new Integer(m.getSlotOffset()));
                    }
                }
                else {
                    XMLGraphPrinter.print(out, m, this);
                }
            }

            for (int i = 0 ; i < methods.length ; i++) {
                if ((methods[i].parent() == aClass || methods[i] instanceof MethodProxy)) {
                    m = methods[i];
                    if (slotsOnly) {
                        if ((m.isStatic() && !m.name().startsWith("_SQUAWK_INTERNAL_")) || m instanceof MethodProxy) {
                            nvslots.addElement(new Integer(m.getSlotOffset()));
                        }
                        else {
                            vslots.addElement(new Integer(m.getSlotOffset()));
                        }
                    } else {
                        XMLGraphPrinter.print(out, m, this);
                    }
                }
            }
            if (methods.length > 0) {
                out.println();
                out.println();
            }
            if (slotsOnly) {
                if (!vslots.isEmpty()) {
                    out.print("    <virtual_methods>");
                    int last = vslots.size();
                    //for (Iterator i = vslots.iterator(); i.hasNext(); last--) {
                    for(Enumeration i = vslots.elements() ; i.hasMoreElements() ; last--) {
                        out.print(((Integer)i.nextElement()).intValue());
                        if (last != 1) {
                            out.print(" ");
                        }
                    }
                    out.println("</virtual_methods>");
                }
                if (!nvslots.isEmpty()) {
                    out.print("    <non_virtual_methods>");
                    int last = nvslots.size();
                    //for (Iterator i = nvslots.iterator(); i.hasNext(); last--) {
                    for(Enumeration i = nvslots.elements() ; i.hasMoreElements() ; last--) {
                        out.print(((Integer)i.nextElement()).intValue());
                        if (last != 1) {
                            out.print(" ");
                        }
                    }
                    out.println("</non_virtual_methods>");
                }
            }
        } catch (LinkageException le) {
            writeMethods(aClass, out, slotsOnly, le);
        }
    }

   /**
    * Write out the XML header for a method.
    */
    public static void xmlHead(String desc, String name, int slot, PrintStream out) {
        out.println();
        out.println();
        out.println("    <!-- =================================== "+desc+" ==================================== -->");
        out.println();
        out.println();
        out.println("    <method>");
        out.println("      <name>"+name+"</name>");
        out.println("      <slot>"+slot+"</slot>");
    }

   /**
    * Write out the XML header for a class.
    */
    public static void xmlHead(Type aClass, PrintStream out) {
        out.println();
        out.println();
        out.println("<!-- ***************************************** "+aClass+" ***************************************** -->");
        out.println();
        out.println();

        out.println("<squawk xmlns=\"http://www.sun.com/squawk/version/1.0\">");
        out.println("  <class>");
        out.println("    <number>"+aClass.getID()+"</number>");
        if (aClass.getLinkageError() != null) {
System.err.println("LINKAGE ERROR: class " + aClass.getID() + ": " + aClass.getLinkageError().getMessage());
            out.println("    <linkage_error/>");
        }
    }

   /**
    * xmlTail
    */
    public static void xmlTail(PrintStream out) {
        out.println("  </class>");
        out.println("</squawk>");
    }

    /**
     * Get the number the next interface method will be
     */
    public int nextInterfaceMethod() {
        return nextFreeInterfaceMethod;
    }

    /**
     * Allocate an interface vtable entry
     */
    public int allocateInterfaceMethod() {
        int res = nextFreeInterfaceMethod++;
        return res;
    }

    /**
     * Allocate an interface vtable entry
     */
    public int allocateClassNumber() {
        return nextClassNumber++;
    }

    /**
     * Return the compiler optimization level
     */
    public int optimizationLevel() {
        return optimizationLevel;
    }

   /* ------------------------------------------------------------------------ *\
    *                      Object type database managemant                     *
   \* ------------------------------------------------------------------------ */

   /**
    * Hashtable to translate class names to types
    */
    private ArrayHashtable internedTypes = new ArrayHashtable(32);

   /**
    * Hashtable to translate class numbers to types
    */
    private IntHashtable typeLookupTable = new IntHashtable(32);

   /**
    * Find an interned type based on a class name in internal class name format (e.g. "Ljava/lang/Object;").
    */
    public Type findType(String name) {
        Type type = (Type)internedTypes.get(name);
        if (type == null && parentVirtualMachine != null) {
            type = parentVirtualMachine.findType(name);
        }
        return type;
    }

   /**
    * Find an interned type
    */
    public Type findOrCreateNonArrayType(String name) {
        Type type = findType(name);
        if (type == null) {
//prtn("calling Type.createForVM for "+name);
            type = Type.create(this,  name);
            internedTypes.put(name, type);
            typeLookupTable.put(type.getID(), type);
        }
        return type;
    }


   /**
    * Find an interned type by class number
    */
    public Type findType(int classNumber) {
        return (Type)typeLookupTable.get(classNumber);
    }


   /**
    * Find or create an interned type
    */
    public Type createType(String name) {
        int dims = 0;
        Type type = null;

        if (name.charAt(0) == '[') {
            for (int j = 0 ; j < name.length() ; j++) {
                if (name.charAt(j) == '[') {
                    dims++;
                } else {
                    break;
                }
            }
        }

        assume(name.charAt(dims) == 'L' || name.charAt(dims) == '-' || (name.length() - dims) == 1);

        switch(name.charAt(dims)) {
            case 'I': type =  Type.INT;     break;
            case 'J': type =  Type.LONG;    break;
            case 'F': type =  Type.FLOAT;   break;
            case 'D': type =  Type.DOUBLE;  break;
            case 'Z': type =  Type.BOOLEAN; break;
            case 'C': type =  Type.CHAR;    break;
            case 'S': type =  Type.SHORT;   break;
            case 'B': type =  Type.BYTE;    break;
            case 'V': type =  Type.VOID;    break;
            default:
            case 'L':
                    name = name.substring(dims);
                    if (name.charAt(name.length()-1) != ';') {
                      name = name = "L"+name+';';
                    }
                    name = name.replace('.', '/');

            case '-':
                    type = findOrCreateNonArrayType(name);
                    break;

        }

        for (int i = 0 ; i < dims ; i++) {
            name = '[' + type.name();
            type = findOrCreateNonArrayType(name);
        }

        return type;
    }


    public Type createType(Type superType, String name) {
        Type type = createType(name);
        type.setSuperType(superType);
        return type;
    }

   /* ------------------------------------------------------------------------ *\
    *                              Type[] interning                            *
   \* ------------------------------------------------------------------------ */

   /**
    * Find an interned member
    */
    private Type[] findList(Type[] list) {
        Type[] list2 = (Type[])internedTypes.get(list);
        if (list2 == null && parentVirtualMachine != null) {
            list2 = parentVirtualMachine.findList(list);
        }
        return list2;
    }

   /**
    * Get an interned member
    */
    public Type[] internList(Type[] list) {
        Type[] list2 = findList(list);
        if (list2 != null) {
            return list2;
        }
        internedTypes.put(list, list);
        return list;
    }


   /* ------------------------------------------------------------------------ *\
    *                             String interning                             *
   \* ------------------------------------------------------------------------ */

   /**
    * Hashtale to keep track of existing classes
    */
    private ArrayHashtable internedStrings = new ArrayHashtable(64);

   /**
    * Find an interned string
    */
    private String findString(String string) {
        string = (String)internedStrings.get(string);
        if (string == null && parentVirtualMachine != null) {
            string = parentVirtualMachine.findString(string);
        }
        return string;
    }

   /**
    * Get an interned class
    */
    public String internString(String string) {
        if (string != null) {
            String s = findString(string);
            if (s == null) {
                internedStrings.put(string, string);
                s = string;
            }
            return s;
        }
        return null;
    }


}
