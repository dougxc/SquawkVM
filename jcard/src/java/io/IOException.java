/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ArithmeticException.java	1.3
// Version:1.3
// Date:12/19/00
// 
// Archive:  /Products/Europa/api21/java/lang/ArithmeticException.java 
// Modified:12/19/00 15:03:00
// Original author:  Ravi
// */

package java.io;

/**
 * A JCRE owned instance of <code>IOException</code> is thrown to
 * signal that an I/O exception of some sort has occurred.
 * This class is the general class of exceptions produced by
 * failed or interrupted I/O operations.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the
 * <em>Java 2 Platform Standard Edition API Specification</em>.<p>
 */

public class IOException extends Exception{
    /**
     * Constructs an <code>IOException</code>.
     */
  public IOException() {}
}
