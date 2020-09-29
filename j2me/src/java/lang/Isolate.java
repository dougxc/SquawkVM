package java.lang;
import java.util.*;

/**
 * This class encapsulates an isolate, which can be thought of as a handle for the control and monitoring of an associated
 * isolated computation existing in a logical heapspace.
 */
public class Isolate extends Thread {

    /**
     * This is the class space representing the state of the ROMized system. It
     * is only set in the master Isolate and should never be updated once the
     * system starts.
     */
    final IsolateClassSpace protoTypeClassSpace;

    /** This is the table of class spaces. It is only non-null in the master Isolate. */
    final NoSyncHashtable classSpaces;

    /** The parent of this Isolate. This is null in the master Isolate and non-null for all others. */
    final Isolate  parent;

    /** The Isolate globals vector. */
    int[]           isolateRoots;

    boolean               running = false;
    final String          mainClassName;
    final String[]        args;
//    String                classpath;
    final String          name;
    final String          uniqueId;
    final NoSyncHashtable children;
    final NoSyncHashtable threads;
    boolean               rundown = false;
    int                   exitStatus = 0;
    final NoSyncHashtable hashedMonitors;
    final Hashtable       properties;


    /**
     * Constructor for the master isolate - must only be called once.
     */
    Isolate() {
        super(0);
        this.mainClassName       = "java.lang.JavaApplicationManager";
        this.args                = null;
        this.protoTypeClassSpace = new IsolateClassSpace(ClassBase.isolateState, ClassBase.isolateStateOopMap, ClassBase.classTable);
        this.classSpaces         = new NoSyncHashtable();
        this.parent              = null;
        this.children            = new NoSyncHashtable();
        this.threads             = new NoSyncHashtable();
        this.hashedMonitors      = new NoSyncHashtable();
        this.name                = "Master Isolate";
        this.uniqueId            = "Isolate using ".concat(super.toString());
        this.properties          = null;
        init("");
    }


    /**
     * Construct an Isolate that specifies additional system properties to those
     * inherited from its parent. The specified propertis in props override the
     * parent properties where there is a name clash.
     */
    public Isolate(String mainClassName, String[] args, String classpath, String name) {
        this(mainClassName, args, classpath, name, null);
    }

    /**
     * Construct an Isolate that inherits its parent's system properties.
     */
    public Isolate(String mainClassName, String[] args, String classpath, String name, Hashtable properties) {
        this.mainClassName       = mainClassName;
        this.args                = args;
        this.protoTypeClassSpace = null;
        this.classSpaces         = null;
        this.parent              = getCurrentIsolate();
        this.children            = new NoSyncHashtable();
        this.threads             = new NoSyncHashtable();
        this.hashedMonitors      = new NoSyncHashtable();
        this.name                = name;
        this.uniqueId            = "Isolate using ".concat(super.toString());
        this.properties          = properties;
        init(classpath);
        getCurrentIsolate().makeCurrent();
//        this.classpath     = (classpath == null) ? parent.classpath : classpath;
    }

    /**
     * Return the IsolateClassSpace corresponding to a given class path.
     */
    IsolateClassSpace findClassSpace(String classPath) {
       /*
        * First get to the master isolate
        */
        if (parent != null) {
            return parent.findClassSpace(classPath);
        }

       /*
        * Look for classpath in table. If there is not one there
        * then create a new classpath by cloning the one from the romized image
        */
        Object res = classSpaces.get(classPath);
        if (res == null) {
            res = protoTypeClassSpace.klone();
            classSpaces.put(classPath, res);
        }
        return (IsolateClassSpace)res;
    }

    /**
     * init
     */
    private void init(String classpath) {

        IsolateClassSpace isolateClassSpace = findClassSpace(classpath);

        /*
         * Clone the romized isolate roots and make this the current isolate.
         * This also requires copying the global state of the scheduler into
         * the new Isolate.
         */
        SchedulerState scheduler = Thread.scheduler;
        isolateRoots = isolateClassSpace.kloneIsolateRoots();
        makeCurrent();
        Thread.scheduler = scheduler;
        ClassBase.isolateState = isolateRoots;

        /*
         * Setup the oopmap and class table
         */
        ClassBase.isolateStateOopMap  = isolateClassSpace.getIsolateStateOopMap();
        ClassBase.classTable          = isolateClassSpace.getClassTable();
        ClassBase.isolateId           = this.name.concat(": ").concat(uniqueId);


        /*
         * Reset the class initialization state structures for all but the master isolate
         */
        if (parent != null) {
            ClassBase.classStateTable  = null;
            ClassBase.classThreadTable = null;
        }

        /*
         * Setup class and object class pointers
         */
        ClassBase.objectClass         = ClassBase.forNumber(CNO_Object);

        /*
         * Do stuff here that would otherwise be in class initializers of classes that
         * are involved in implementing class initialization.
         */
        Native.longBuf = new long[2];
        Native.TheOutOfMemoryError = new OutOfMemoryError(false);

       /*
        * Initialize the other things
        */
        setIsolate(this);
        if (parent != null) {
            parent.children.put(uniqueId, this);
        }
    }

    /**
     * run
     */
    public void run() {

        if (!isAlive()) {
            throw new RuntimeException("Cannot run dead isolate");
        }
        if (running) {
            throw new RuntimeException("Cannot run isolate twice");
        }
        if (getCurrentIsolate() != this) {
            throw new RuntimeException("Isolate running on wrong thread");
        }
        running = true;

        /*
         * Call the <clinit> methods of all the classes for which explicit CLINIT bytecodes
         * are *not* generated by the translator.
         */
        Class.ensureClassInitializationArraysCapacity(ClassBase.classTable[CNO_InitLimit]);
        for (int i = 1 ; i <= CNO_InitLimit ; i++) {
                Native.assume(ClassBase.classTable[i] != null);
                Class c = Native.asClass(ClassBase.classTable[i]);
                c.initializeClass();
        }
        /*
         * Set the flag telling Class.initializeClass to do complete thread-aware class initialization.
         */
        ClassBase.classStateTable[0] = 1;

        /*
         * Find the main class and call its main()
         */
        Class mainClass = null;
        try {
            mainClass = Class.forName(mainClassName);
        } catch(ClassNotFoundException ex) {
            System.err.println("Could not find Isolate's main class: " + mainClassName);
            ex.printStackTrace();
            return;
        }

        /*
         * Call the <clinit> for the class
         */
        try {
            mainClass.callMain(args);
        } catch (ExitVMError ex) {
            // Print nothing because this is the result of System.exit();
        } catch (Throwable ex) {
            exitStatus = -1;
            rundown = true;
            ex.printStackTrace();
            System.out.print("Uncaught exception in main isolate thread: ");
            System.out.println(ex);
        }

        /*
         * Wait for all the other threads belonging to this Isolate to stop
         */
        while (threads.size() != 1) {
            for (Enumeration e = threads.elements() ; e.hasMoreElements() ; ) {
                Thread aThread = (Thread)e.nextElement();
                try { aThread.join(); } catch (InterruptedException ex) {}
            }
        }
    }

    /**
     * addThread
     */
    void addThread(Thread aThread) {
        if (threads.get(aThread) != null) {
            throw new RuntimeException("Adding same thread twice "+aThread);
        }
        threads.put(aThread, aThread);
    }

    /**
     * removeThread
     */
    void removeThread(Thread aThread) {
        if (threads.remove(aThread) == null) {
            throw new RuntimeException("Cannot remove thread " + aThread);
        }
    }

    /**
     * setCurrent
     */
    void makeCurrent() {
       /*
        * Tell the VM to switch to a new global vector
        */
        Native.setIsolate(isolateRoots);

       /*
        * If the current isolate is being run then kill the thread now
        */
        if (rundown == true) {
            throw new ExitVMError();
        }
    }

    /**
     * getCurrentIsolate
     */
    public static Isolate getCurrentIsolate() {
        if (Thread.currentThread() == null) {
            return null; // needed for bootstrapping
        }
        return Thread.currentThread().isolate;
    }

    /**
     * getIsolate
     */
    public static Isolate getIsolate(String uniqueId) {
       /*
        * Find the top most isolate
        */
        Isolate iso = getCurrentIsolate();
        while (iso.parent != null) {
           iso = iso.parent;
        }
       /*
        * Search from there down
        */
        return getIsolatePrim(iso, uniqueId);
    }

    /**
     * getIsolatePrim
     */
    private static Isolate getIsolatePrim(Isolate iso, String uniqueId) {
        for (Enumeration e = iso.children.elements() ; e.hasMoreElements() ; ) {
            Isolate i = (Isolate)e.nextElement();
           /*
            * Check is this child is the required isolate
            */
            if (i.uniqueId.equals(uniqueId)) {
                return i;
            }
           /*
            * Check is this child is parent to the required isolate
            */
            i = i.getIsolatePrim(i, uniqueId);
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    /**
     * getChildren
     */
    public Isolate[] getChildren() {
        Isolate[] result = new Isolate[children.size()];
        int index = 0;
        for (Enumeration e = children.elements() ; e.hasMoreElements() ; ) {
            result[index++] = (Isolate)e.nextElement();
        }
        return result;

    }

    /**
     * getName
     */
    public String getName() {
        return name;
    }

    /**
     * getParent
     */
    public Isolate getParent() {
        return parent;
    }

    /**
     * getUniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * isStarted
     */
    public boolean isStarted() {
        return isAlive();
    }

    /**
     * isStopped
     */
    public boolean isStopped() {
        return isDead();
    }

    /**
     * isSuspended
     */
    public boolean isSuspended() {
        return false;
    }

    /**
     * join
     */
    public void join(long millis) {
        throw new RuntimeException("Unimplemented");
    }

    /**
     * join
     */
    public void join(long millis, int nanos) {
        throw new RuntimeException("Unimplemented");
    }

    /**
     * resume
     */
    public void resume() {
        throw new RuntimeException("Unimplemented");
    }

    /**
     * stop
     */
    public void stop() {
        exitStatus = -1;
        rundown = true;
    }

    /**
     * suspend
     */
    public void suspend() {
        throw new RuntimeException("Unimplemented");
    }

    /**
     * equals
     */
    public boolean equals(Isolate isolate) {
        throw new RuntimeException("Unimplemented");
    }

    /**
     * exit
     */
    void exit(int status) {
        exitStatus = status;
        rundown = true;
        throw new ExitVMError();
    }

    /**
     * result
     */
    public int result() {
        if (isAlive()) {
            throw new RuntimeException("Cannot get result of non-dead isolate");
        }
        return exitStatus;
    }



}

/**
 * This class supports a grouping of Isolates based on certain properties which are shared amongst all
 * Isolates in the group. At this point the shared property is the classpath with which an Isolate
 * is created.
 */
class IsolateClassSpace {
    /** */
    int[]       isolateRoots;
    byte[]      isolateStateOopMap;
    ClassBase[] classTable;

    public IsolateClassSpace(int[] isolateRoots, byte[] isolateStateOopMap, ClassBase[] classTable) {
        this.isolateRoots       = isolateRoots;
        this.isolateStateOopMap = isolateStateOopMap;
        this.classTable         = classTable;
    }

    IsolateClassSpace klone() {
        return new IsolateClassSpace(isolateRoots, klone(isolateStateOopMap), klone(classTable));
    }

    int[] kloneIsolateRoots() {
        return klone(isolateRoots);
    }

    byte[] getIsolateStateOopMap() {
        return isolateStateOopMap;
    }


    ClassBase[] getClassTable() {
        return classTable;
    }


    /**
     * klone
     */
    private static int[] klone(int[] oldarray) {
        int[] array = new int[oldarray.length];
        Native.setHeader(array, oldarray.getClass());
        System.arraycopy(oldarray, 0, array, 0, oldarray.length);
        return array;
    }

    /**
     * klone
     */
    private static byte[] klone(byte[] oldarray) {
        byte [] array = new byte[oldarray.length];
        System.arraycopy(oldarray, 0, array, 0, oldarray.length);
        return array;
    }

    /**
     * klone
     */
    private static ClassBase[] klone(ClassBase[] oldarray) {
        ClassBase [] array = new ClassBase[oldarray.length];
        System.arraycopy(oldarray, 0, array, 0, oldarray.length);
        return array;
    }
}
