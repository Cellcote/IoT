/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.iot.server;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;

/**
 *
 * @author rikschreurs
 */
public class Server {

    private LeshanServer lwServer;
    private ObservationRegistryListener observationListener;

    Server() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("xxx");
                lwServer.getCoapServer().stop();
                lwServer.stop();
            }
        });
    }

    public void startServer() {
        //wServer.stop();
        // Build LWM2M server
        lwServer = new LeshanServerBuilder().build();
        observationListener = new ObservationListener();
        // listen for observe notifications
        lwServer.getObservationRegistry().addListener(observationListener);
        // Listen to registrations/deregistration
        lwServer.getClientRegistry().addListener(new ClientRegistryListener() {
            @Override
            public void registered(Client client) {
                System.out.println("New registered client with endpoint: " + client.getEndpoint());
                
            }

            @Override
            public void updated(Client clientUpdated) {
                System.out.println("Registration updated");
            }

            @Override
            public void unregistered(Client client) {
                System.out.println("Registration deleted");
            }
        });
        // Start
        lwServer.start();
        
        //lwServer.getCoapServer().stop();
        System.out.println("Demo server started");

    }

    public static void main(String[] args) {
        new Server().startServer();
    }

}
