/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;
import java.io.PrintStream;

/**
 * The <code>Throwable</code> class is the superclass of all errors
 * and exceptions in the Java language. Only objects that are
 * instances of this class (or of one of its subclasses) are thrown
 * by the Java Virtual Machine or can be thrown by the Java
 * <code>throw</code> statement. Similarly, only this class or one of
 * its subclasses can be the argument type in a <code>catch</code>
 * clause.
 * <p>
 * Instances of two subclasses, {@link java.lang.Error} and
 * {@link java.lang.Exception}, are conventionally used to indicate
 * that exceptional situations have occurred. Typically, these instances
 * are freshly created in the context of the exceptional situation so
 * as to include relevant information (such as stack trace data).
 * <p>
 * By convention, class <code>Throwable</code> and its subclasses have
 * two constructors, one that takes no arguments and one that takes a
 * <code>String</code> argument that can be used to produce an error
 * message.
 * <p>
 * A <code>Throwable</code> class contains a snapshot of the
 * execution stack of its thread at the time it was created. It can
 * also contain a message string that gives more information about
 * the error.
 * <p>
 * Here is one example of catching an exception:
 * <p><blockquote><pre>
 *     try {
 *         int a[] = new int[2];
 *         a[4];
 *     } catch (ArrayIndexOutOfBoundsException e) {
 *         System.out.println("exception: " + e.getMessage());
 *         e.printStackTrace();
 *     }
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 1.43, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public class Throwable implements NativeOpcodes {

    /** WARNING: this must be the first variable.
     * Specific details about the Throwable.  For example,
     * for FileNotFoundThrowables, this contains the name of
     * the file that could not be found.
     *
     * @serial
     */
    private String detailMessage;

    /**
     * Constructs a new <code>Throwable</code> with <code>null</code> as
     * its error message string.
     */
    public Throwable() {
        fillInStackTrace();
//printStackTrace();
    }

    /**
     * Constructs a new <code>Throwable</code> with the specified error
     * message.
     *
     * @param   message   the error message. The error message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public Throwable(String message) {
        this();
        detailMessage = message;
    }

    /**
     * Returns the error message string of this throwable object.
     *
     * @return  the error message string of this <code>Throwable</code>
     *          object if it was {@link #Throwable(String) created} with an
     *          error message string; or <code>null</code> if it was
     *          {@link #Throwable() created} with no error message.
     *
     */
    public String getMessage() {
        return detailMessage;
    }

    /**
     * Returns a short description of this throwable object.
     * If this <code>Throwable</code> object was
     * {@link #Throwable(String) created} with an error message string,
     * then the result is the concatenation of three strings:
     * <ul>
     * <li>The name of the actual class of this object
     * <li>": " (a colon and a space)
     * <li>The result of the {@link #getMessage} method for this object
     * </ul>
     * If this <code>Throwable</code> object was {@link #Throwable() created}
     * with no error message string, then the name of the actual class of
     * this object is returned.
     *
     * @return  a string representation of this <code>Throwable</code>.
     */
    public String toString() {
        String s = getClass().getName();
        String message = getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is
     * the value of the field <code>System.err</code>. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object. <p>
     *
     * The format of the backtrace information depends on the implementation.
     */

    public void printStackTrace() {
        if (System.err == null) {
            printStackTraceNative();
        }
        else {
            printStackTrace(System.err);
        }
    }

    public void printStackTrace(PrintStream err) {
        String message = getMessage();
        err.print(this.getClass().getName());
        if (message != null) {
            err.print(": ");
            err.println(message);
        } else {
            err.println();
        }
        StackTraceElement st = stackTrace;
        while (st != null) {
            err.print("\t");
            err.println(st);
            st = st.previous;
        }
    }

    /**
     * Stack trace printing for VM debugging when System.err has not been
     * initialized.
     */
    private void printStackTraceNative() {
        String message = getMessage();
        Native.print(this.getClass().getName());
        if (message != null) {
            Native.print(": ");
            Native.println(message);
        } else {
            Native.println();
        }
        StackTraceElement st = stackTrace;
        while (st != null) {
            Native.print("\t");
            Native.println(st);
            st = st.previous;
        }
    }

    /**
     * Fills in the execution stack trace. This method records within this Throwable object
     * information about the current state of the stack frames for the current thread.
     */
    void fillInStackTrace() {
        int[] ar = Native.getActivation();
        try {
            StackTraceElement last  = null;
            while (ar != null) {
                String className;
                String methodName;
                String fileName;
                String lineNumber;

                int ip        = ar[AR_ip] - 1;
                byte[] mth    = Native.asByteArray(ar[AR_method]);

                // Get class info
                int cno =  Native.getUnsignedHalf(mth,MTH_classNumberHigh);
                ClassBase clazz = ClassBase.forNumber(cno);
                className = clazz.className;
                fileName  = clazz.getSourceFileName();
                if (fileName == null) {
                    fileName = "<unknown>";
                }

                // Get method info
                String[] mthDbgInfo = Native.getMethodNameAndSourceLine(mth, ip);
                methodName = mthDbgInfo[0];
                lineNumber = mthDbgInfo[1];
                if (methodName == null) {
                    methodName = "<unknown>";
                }
                if (lineNumber == null) {
                    lineNumber = "ip=".concat(Integer.toString(ip));
                }

                if (stackTrace == null) {
                    last = stackTrace = new StackTraceElement(className, methodName, fileName, lineNumber);
                } else {
                    last.previous = new StackTraceElement(className, methodName, fileName, lineNumber);
                    last = last.previous;
                }
                ar = Native.asIntArray(ar[AR_previousAR]);
            }
        } catch(Throwable ex) {
            Native.fatalVMError();
        }
    }

    protected StackTraceElement stackTrace;
}

class StackTraceElement {
    final String className;
    final String methodName;
    final String fileName;
    final String lineNumber;
    final String asString;
    StackTraceElement previous;
    StackTraceElement(String className, String methodName, String fileName, String lineNumber) {
        this.className  = className;
        this.methodName = methodName;
        this.fileName   = fileName;
        this.lineNumber = lineNumber;
        this.previous   = previous;

        // Avoid using StringBuffer as it's append methods are synchronized and threading may not yet be initialized
        asString = className.concat(".").concat(methodName).concat("(").concat(fileName).concat(":").concat(lineNumber).concat(")");
    }
    public String toString() {
        return asString;
    }
}