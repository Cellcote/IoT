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

import org.eclipse.californium.core.network.Endpoint;

/**
 *
 * @author rikschreurs
 */
public class EndpointListener implements org.eclipse.californium.core.network.EndpointObserver {

    @Override
    public void started(Endpoint endpoint) {
        System.out.println("test");
    }

    @Override
    public void stopped(Endpoint endpoint) {
        System.out.println("test1");
    }

    @Override
    public void destroyed(Endpoint endpoint) {
        System.out.println("test2");
    }
    
}
