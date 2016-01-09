/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.iot.server;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;

/**
 *
 * @author rikschreurs
 */
public class ObservationListener implements ObservationRegistryListener  {
@Override
            public void newValue(Observation observation, LwM2mNode value) {

                System.out.println("New notification from client " + observation.getRegistrationId() + " on " + observation.getPath() + ": "
                        + value);
            }

            @Override
            public void newObservation(Observation observation) {

                System.out.println("Observing resource " + observation.getPath() + " from client "
                        + observation.getRegistrationId());
            }

            @Override
            public void cancelled(Observation observation) {
                //
            }
    
}
