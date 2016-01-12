/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eclipse.leshan.standalone;

import com.ning.http.client.AsyncHttpClient;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;

/**
 *
 * @author rikschreurs
 */
public class ObservationListener implements ObservationRegistryListener  {
            DatastoreSender datastore;
            private AsyncHttpClient asyncHttpCLient = new AsyncHttpClient();
            public ObservationListener(DatastoreSender datastore) {
                this.datastore = datastore;
            }
            
            @Override
            public void newValue(Observation observation, LwM2mNode value) {

                System.out.println("New notification from client " + observation.getRegistrationId() + " on " + observation.getPath() + ": "
                        + value);
                //System.out.println(extractNewValue(value));
                
                datastore.sendValue(observation.getRegistrationId(), extractResourceId(value), extractNewValue(value));
                
            }
            
            private String extractNewValue(LwM2mNode value) {
                String stringValue = "";
                String tempObject = value.toString();
                String[] split = tempObject.split(",");
                return split[1].split("=")[1];
            }
            
            private String extractResourceId(LwM2mNode value) {
                String stringValue = "";
                String tempObject = value.toString();
                String[] split = tempObject.split(",");
                return split[0].split("=")[1];
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
