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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rikschreurs
 */
class DatastoreSender {

    Map<String, String> clientMap = new HashMap<String, String>();
    private AsyncHttpClient asyncHttpCLient = new AsyncHttpClient();

    public void sendValue(String id, String resourceId, String newValue) {
        String name = clientMap.get(id);
        if (resourceId.equals("/3345/0/5703")) {
            if (Integer.parseInt(newValue) == 100) {
                sendNewStateOfParkingSpot(id, "occupant");
            } else if (Integer.parseInt(newValue) == -100) {
                sendNewStateOfParkingSpot(id, "free");
            }
        }
        System.out.println("Sending " + newValue + " from " + name);
    }

    public void addNewClient(String id, String endpoint) {
        clientMap.put(id, endpoint);
    }

    private void sendNewStateOfParkingSpot(String parkingSpot, String newState) {
        asyncHttpCLient.preparePut("http://192.168.99.100:8081/parkingspots/" + parkingSpot + "/park").execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response rspns) throws Exception {
                System.out.println("Client registered");
                return rspns;
            }

            @Override
            public void onThrowable(Throwable t) {
                System.out.println("XXX");
                System.out.println(t.toString());
            }
        });
    }
}
