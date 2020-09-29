/*
 * Copyright � 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)IndexOutOfBoundsException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/IndexOutOfBoundsException.java 
// Modified:02/01/02 11:15:53
// Original author:  Ravi
// */

package java.lang;

/**
  * A JCRE owned instance of <code>IndexOutOfBoundsException</code> is thrown to indicate that
  * an index of some sort (such as to an array) is out of range. 
  * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
  * and can be accessed from any applet context. References to these temporary objects
  * cannot be stored in class variables or instance variables or array components.
  * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
  * <p>This Java Card class's functionality is a strict subset of the definition in the 
  * <em>Java Platform Core API Specification</em>.<p>
  */

public class IndexOutOfBoundsException extends RuntimeException{
  /**
   * Constructs an <code>IndexOutOfBoundsException</code>.
   */
  public IndexOutOfBoundsException() {}
   
}
