/*
 * Copyright (c) 2016 rikschreurs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    rikschreurs - initial API and implementation and/or initial documentation
 */
package org.eclipse.leshan.standalone;

/**
 *
 * @author rikschreurs
 */
public class Reservation {
    public String spot = "";
    public String vehicle = "";
    public String state = "";

    public Reservation(String spot, String vehicle, String state) {
        this.spot = spot;
        this.vehicle = vehicle;
        this.state = state;
    }
    
    


}
