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

import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import java.util.concurrent.Future;
import com.ning.http.client.*;

/**
 *
 * @author rikschreurs
 */
public class RegistrationObserver implements ClientRegistryListener{
  
    
    @Override
    public void registered(Client client) {
        System.out.println("New client registered with endpoint " + client.getEndpoint());
        observeResource(client);
        
        
    }

    @Override
    public void updated(Client clientUpdated) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        System.out.println("");
    }

    @Override
    public void unregistered(Client client) {
        System.out.println("Client unregistered");
    }

    private void observeResource(Client client) {
        
    }
    
}
