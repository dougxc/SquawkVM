package java.lang;

import java.util.Vector;
import java.io.*;

/**
 * Instances of the class <code>Class</code> represent classes and interfaces
 * in a running Java application.  Every array also belongs to a class that is
 * reflected as a <code>Class</code> object that is shared by all arrays with
 * the same element type and number of dimensions.
 *
 * <p> <code>Class</code> has no public constructor. Instead <code>Class</code>
 * objects are constructed automatically by the Java Virtual Machine as classes
 * are loaded.
 *
 * <p> The following example uses a <code>Class</code> object to print the
 * class name of an object:
 *
 * <p> <blockquote><pre>
 *     void printClassName(Object obj) {
 *         System.out.println("The class of " + obj +
 *                            " is " + obj.getClass().getName());
 *     }
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 1.106, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public final class Class extends ClassBase {

    /**
     * Package private constructor.
     */
    Class(int id, int extnds, int arrayOf,
        String name, Vector impls, Vector constants, Vector svars, Vector ivars, Vector i_map,
        int accessFlags, boolean usesFvtable, int vtableStart, int vtableEnd, int firstNVMethod,
        byte[] debugInfo)
    {
        super(id,extnds,arrayOf,name,impls,constants,svars,ivars,i_map,accessFlags,
            usesFvtable, vtableStart, vtableEnd, firstNVMethod, debugInfo);
    }

    /**
     * Creates a new instance of a class.
     *
     * @return     a newly allocated instance of the class represented by this
     *             object. This is done exactly as if by a <code>new</code>
     *             expression with an empty argument list.
     * @exception  IllegalAccessException  if the class or initializer is
     *               not accessible.
     * @exception  InstantiationException  if an application tries to
     *               instantiate an abstract class or an interface, or if the
     *               instantiation fails for some other reason.
     * @since     JDK1.0
     */
    public Object newInstance() throws InstantiationException, IllegalAccessException {
        return newInstance(classIndex,true);   // This is the equivilant to a "new Foo()"
    }

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * fully qualified name of the class in the format returned by
     * <code>getName</code>.  If this <code>Class</code> object represents a
     * primitive type, this method returns the name of the primitive type.  If
     * this <code>Class</code> object represents void this method returns
     * "void".
     *
     * @return a string representation of this class object.
     */
    public String toString() {
        return (isInterface() ? "interface " :  "class ") + getName();
    }

    /**
     * Returns the <code>Class</code> object associated with the class
     * with the given string name.
     * Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class.  If it
     * succeeds, returns the Class object representing the class.  If
     * it fails, the method throws a ClassNotFoundException.
     * <p>
     * For example, the following code fragment returns the runtime
     * <code>Class</code> descriptor for the class named
     * <code>java.lang.Thread</code>:
     * <ul><code>
     *   Class&nbsp;t&nbsp;= Class.forName("java.lang.Thread")
     * </code></ul>
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the <code>Class</code> descriptor for the class with the
     *             specified name.
     * @exception  ClassNotFoundException  if the class could not be found.
     * @since      JDK1.0
     */

    public static Class forName(String className) throws ClassNotFoundException {
        // Search exisiting classes first
        for (int i = 1; i != ClassBase.classTable.length; i++) {
            ClassBase clazz = ClassBase.classTable[i];
            if (clazz != null && clazz.className.equals(className)) {
                return Native.asClass(initialize(clazz.classIndex));
            }
        }

        // The IO classes must exist in the ROMized image otherwise it will not be possible
        // to connect to the translator.
        String ioClassRoot = Native.getProperty("javax.microedition.io.Connector.protocolpath");
        if (!className.startsWith(ioClassRoot)) {
            // Now go to the translator.
            int cno = SquawkClassLoader.lookup(className, true, true);
            if (cno != -1) {
                return Native.asClass(initialize(cno));
            }
        }
        ClassNotFoundException cnfe = new ClassNotFoundException(className);
        throw cnfe;
    }

    /**
     * Determines if the specified <code>Object</code> is assignment-compatible
     * with the object represented by this <code>Class</code>.  This method is
     * the dynamic equivalent of the Java language <code>instanceof</code>
     * operator. The method returns <code>true</code> if the specified
     * <code>Object</code> argument is non-null and can be cast to the
     * reference type represented by this <code>Class</code> object without
     * raising a <code>ClassCastException.</code> It returns <code>false</code>
     * otherwise.
     *
     * <p> Specifically, if this <code>Class</code> object represents a
     * declared class, this method returns <code>true</code> if the specified
     * <code>Object</code> argument is an instance of the represented class (or
     * of any of its subclasses); it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents an array class, this method
     * returns <code>true</code> if the specified <code>Object</code> argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * <code>false</code> otherwise. If this <code>Class</code> object
     * represents an interface, this method returns <code>true</code> if the
     * class or any superclass of the specified <code>Object</code> argument
     * implements this interface; it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents a primitive type, this method
     * returns <code>false</code>.
     *
     * @param   obj the object to check
     * @return  true if <code>obj</code> is an instance of this class
     *
     * @since JDK1.1
     */
    public boolean isInstance(Object obj) {
        return obj != null && this.isAssignableFrom(Native.getClass(obj));
    }

    /**
     * Determines if the class or interface represented by this
     * <code>Class</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>. If this <code>Class</code>
     * object represents a primitive type, this method returns
     * <code>true</code> if the specified <code>Class</code> parameter is
     * exactly this <code>Class</code> object; otherwise it returns
     * <code>false</code>.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the <code>Class</code> object to be checked
     * @return the <code>boolean</code> value indicating whether objects of the
     * type <code>cls</code> can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is
     *            null.
     * @since JDK1.1
     */
    public boolean isAssignableFrom(Class clazz) {
        boolean res = clazz.isAssignableTo(this);
//System.out.println("isAssignableFrom "+this+" to "+clazz+" = "+res);
        return res;
    }

    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this <code>Class</code>
     * object, as a <code>String</code>.
     *
     * <p> If this <code>Class</code> object represents a class of arrays, then
     * the internal form of the name consists of the name of the element type
     * in Java signature format, preceded by one or more "<tt>[</tt>"
     * characters representing the depth of array nesting. Thus:
     *
     * <blockquote><pre>
     * (new Object[3]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[Ljava.lang.Object;</code>" and:
     *
     * <blockquote><pre>
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[[[[[[[I</code>". The encoding of element type names
     * is as follows:
     *
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L<i>classname;</i>  class or interface
     * S            short
     * Z            boolean
     * </pre></blockquote>
     *
     * The class or interface name <tt><i>classname</i></tt> is given in fully
     * qualified form as shown in the example above.
     *
     * @return  the fully qualified name of the class or interface
     *          represented by this object.
     */
    public String getName() {
        if (className.charAt(className.length()-1) == '_') {
            if (className.equals("java.lang._boolean_")) return "boolean";
            if (className.equals("java.lang._byte_"))    return "byte";
            if (className.equals("java.lang._char_"))    return "char";
            if (className.equals("java.lang._short_"))   return "short";
            if (className.equals("java.lang._int_"))     return "int";
            if (className.equals("java.lang._long_"))    return "long";
            if (className.equals("java.lang._float_"))   return "float";
            if (className.equals("java.lang._double_"))  return "double";
            if (className.equals("java.lang._void_"))    return "void";
        }
        return className;
    }

    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    public java.io.InputStream getResourceAsStream(String name) {
        try {
            if (name.length() > 0 && name.charAt(0) == '/') {
                name = name.substring(1);
            } else {
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex >= 0) {
                    name = className.substring(0, dotIndex + 1).replace('.', '/') + name;
                }
            }
            return javax.microedition.io.Connector.openInputStream("resource:"+name);
        } catch (java.io.IOException x) {
            return null;
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                               SquawkVM specific stuff                    *
   \* ------------------------------------------------------------------------ */

   public int hashCode() {
        return classIndex;
   }

    public Class getSuperclass() {
        if (this == objectClass) {
            return null;
        }
        if (isArray()) {
            return Native.asClass(objectClass);
        } else {
            return Native.asClass(superClass);
        }
    }


    private boolean callerHasAccessToClass() {
        return true; // temp
    }

    /**
     * Call the <clinit> method for this class.
     */
    private void clinit() {
        // Create a prototype instance of this class to be a receiver for this "reflection" call
        Native.newInstance(classIndex)._SQUAWK_INTERNAL_clinit();
    }

    /**
     * Call the main method for this class.
     */
    void callMain(String[] args) {
        // Create a prototype instance of this class to be a receiver for this "reflection" call
        Native.newInstance(classIndex)._SQUAWK_INTERNAL_main(args);
    }

   /* ------------------------------------------------------------------------ *\
    *                            Class Initialization                          *
   \* ------------------------------------------------------------------------ */

    private final static int NOTINITIALIZED  = 0;
    private final static int INITIALIZING    = 1;
    private final static int INITIALIZED     = 2;
    private final static int FAILED          = 3;

    private byte   getState()               { return classStateTable[classIndex];  }
    private Thread getThread()              { return classThreadTable[classIndex]; }
    private void   setState(int state)      { classStateTable[classIndex]  = (byte)state;  }
    private void   setThread(Thread thread) { classThreadTable[classIndex] = thread; }

    /**
     * The internal class initializion function. (See page 53 of the VM Spec.)
     */
    final void initializeClass() {
        Class superClass = getSuperclass();

       /*
        * If this class does not have a <clinit> and the super class is initialized then
        * there is nothing to do.
        */
        if (!hasClinit() && (superClass == null || superClass.getState() == INITIALIZED)) {
            setState(INITIALIZED);
            return;
        }

       /*
        * If threading is not yet initialized then just do a simple form of initialization
        * (i.e. one that assumes a single threaded system).
        */
        if (ClassBase.classStateTable[0] == 0) {
            if (getState() == INITIALIZING) {
                return;
            }
            setState(INITIALIZING);
            if (superClass != null) {
                superClass.initializeClass();
            }
            clinit();
            setState(INITIALIZED);
            return;
        }

       /*
        * Step 1
        */
        synchronized(this) {

           /*
            * Step 2
            */
            if (getState() == INITIALIZING) {
                if (getThread() != Thread.currentThread()) {
                    do {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    } while (getState() == INITIALIZING);
                } else {
                   /*
                    * Step 3
                    */
                    return;
                }
            }

           /*
            * Step 4
            */
            if (getState() == INITIALIZED) {
                return;
            }

           /*
            * Step 5
            */
            if (getState() == FAILED) {
                throw new NoClassDefFoundError();
            }

           /*
            * Step 6
            */
            setThread(Thread.currentThread());
            setState(INITIALIZING);
        }

       /*
        * Step 7
        */
        if (!isInterface()) {
            if (superClass != null && superClass.getState() != INITIALIZED) {
                try {
                    superClass.initializeClass();
                } catch(Error ex) {
                    synchronized(this) {
                        setThread(null);
                        setState(FAILED);
                        notifyAll();
                    }
                    throw ex;
                } catch(Throwable ex) {
                    Native.fatalVMError();
                }
            }
        }

       /*
        * Step 8
        */
        try {
            clinit();

           /*
            * Step 9
            */
            synchronized(this) {
                setThread(null);
                setState(INITIALIZED);
                notifyAll();
                return;
            }
        } catch(Throwable ex) {
           /*
            * Step 10
            */
            if (!(ex instanceof Error)) {
                ex = new ExceptionInInitializerError(ex);
            }

           /*
            * Step 11
            */
            synchronized(this) {
                setThread(null);
                setState(FAILED);
                notifyAll();
            }
            throw (Error)ex;
        }
    }

    static void ensureClassInitializationArraysCapacity(ClassBase clazz) {
        // Establish the minimum capacity needed to proceed with
        // class initialization for the given class.
        int min = clazz.classIndex;
        ClassBase sclazz = clazz.superClass;
        while (sclazz != null) {
            if (sclazz.classIndex > min) {
                min = sclazz.classIndex;
            }
            sclazz = sclazz.superClass;
        }


        // The state of ClassBase.classThreadTable will exactly mirror
        // ClassBase.classStateTable as this is the only method that modifies
        // them (Class.initialize only modifies their contents). As such,
        // it's only necessary to test one of them.
        if (ClassBase.classThreadTable == null || ClassBase.classThreadTable.length <= min) {
            // Copy the initialization state array
            byte[] state = new byte[min+1];
            if (ClassBase.classStateTable != null) {
                System.arraycopy(ClassBase.classStateTable,0,state,0,ClassBase.classStateTable.length);
            }
            ClassBase.classStateTable = state;

            // Copy the initializing threads array
            Thread[] threads = new Thread[min+1];
            if (ClassBase.classThreadTable != null) {
                System.arraycopy(ClassBase.classThreadTable,0,threads,0,ClassBase.classThreadTable.length);
            }
            ClassBase.classThreadTable = threads;
        }
    }

    /**
     * This method is the entry point for the implementation of the
     * OPC_CLINIT bytecode.
     */
    static Class initialize(int cno) {
        Class clazz = Native.asClass(forNumber(cno));
        ensureClassInitializationArraysCapacity(clazz);
        if (clazz.getState() != INITIALIZED) {
            clazz.ensureMethodsLoaded();
            clazz.initializeClass();
        }
        return clazz;
    }

    /**
     * Creates a new array of a class.
     */
    static Object newArray(int cno, int length) throws InstantiationException, IllegalAccessException {
        Class cls = Native.asClass(forNumber(cno));
        if (cls.gcType < GCTYPE_array) {
            throw new InstantiationException();
        }
        if (!cls.callerHasAccessToClass()) {
            throw new IllegalAccessException();
        }
        return Native.newArray(cno, length);
    }

    /**
     * addDimension
     */
    static void addDimension(Object[] array, int nextDimention) throws InstantiationException, IllegalAccessException {
        Class arrayClass = array.getClass();
        Class elementClass = Native.asClass(arrayClass.elementType);
        for (int i = 0 ; i < array.length ; i++) {
            if (array[i] == null) {
                array[i] = Class.newArray(elementClass.classIndex, nextDimention);
            } else {
                addDimension((Object[])array[i], nextDimention);
            }
        }
    }

    /**
     * Creates a new instance of a class.
     */
    static Object newInstance(int cno) throws InstantiationException, IllegalAccessException {
        return newInstance(cno,false);
    }
    static Object newInstance(int cno, boolean callConstructor) throws InstantiationException, IllegalAccessException {
        Class clazz = initialize(cno);
        if (clazz.gcType != GCTYPE_object) {
            throw new InstantiationException();
        }
        if (!clazz.callerHasAccessToClass()) {
            throw new IllegalAccessException();
        }
        Object obj = Native.newInstance(cno);
        if (callConstructor) {
            obj._SQUAWK_INTERNAL_init();
        }
        return obj;
    }

    /*
     * Return the Virtual Machine's Class object for the named
     * primitive type.
     */
    static Class getPrimitiveClass(String name) {
        if (name.equals("boolean")) return java.lang._boolean_.class;
        if (name.equals("byte"))    return java.lang._byte_.class;
        if (name.equals("char"))    return java.lang._char_.class;
        if (name.equals("short"))   return java.lang._short_.class;
        if (name.equals("int"))     return java.lang._int_.class;
        if (name.equals("long"))    return java.lang._long_.class;
        if (name.equals("float"))   return java.lang._float_.class;
        if (name.equals("double"))  return java.lang._double_.class;
        if (name.equals("void"))    return java.lang._void_.class;
        return null;
    }
}