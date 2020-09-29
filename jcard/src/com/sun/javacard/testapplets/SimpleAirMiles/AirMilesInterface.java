/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.javacard.testapplets.SimpleAirMiles;
import javacard.framework.*;

public interface AirMilesInterface extends Shareable {
	public void addMiles(short amount);
	public short getMiles();
}
