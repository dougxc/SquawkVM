/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

public class Monitor {

    Class   MON_realType;
    Thread  MON_owner;
    Thread  MON_monitorQueue;
    Thread  MON_condvarQueue;
    int     MON_hashCode;
    int     MON_depth;
    int     MON_isProxy;
}
