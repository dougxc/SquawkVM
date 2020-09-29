
package com.sun.squawk.translator.loader;
import  com.sun.squawk.translator.Type;

/**
 * This class is used to mimic the semantics of java.lang.LinkageError.
 * That class cannot be used directly as doing so would potentially
 * lead to confusion as to whether an error occurred during the loading
 * of a translator class or while the translator itself was loading
 * a class.
 */
public class LinkageException extends Exception {

   /**
    * Constructor
    */
    public LinkageException(Type errorClass, String details) {
        super(details);
        this.errorClass = errorClass;
printStackTrace();
    }

    public Type errorClass() {
        return errorClass;
    }

    protected Type errorClass;
}