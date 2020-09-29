package java.lang;

import java.io.PrintStream;
import java.util.Vector;

/**
 * This is the error thrown when there is a problem loading a Squawk assembly file.
 * It provides extra contextual info for where exactly in the loading an error occured.
 */
class SquawkClassFormatError extends Error {
    Vector contextStack = new Vector();

    public SquawkClassFormatError(String msg) {
        super(msg);
    }
    /**
     * Constructor that chains an existing SquawkClassFormatError exception.
     */
    public SquawkClassFormatError addContext(String context) {
        contextStack.addElement(context);
        return this;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }
    public void printStackTrace(PrintStream err) {
        err.println(getMessage());
        for (int i = 0; i != contextStack.size(); i++) {
            err.println("\t"+contextStack.elementAt(i));
        }
        super.printStackTrace(err);
    }
}