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
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author rikschreurs
 */
class DatastoreSender {
    //2016-01-12 18:40:36
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date lastChecked = null;
    Map<String, String> clientMap = new HashMap<String, String>();
    private AsyncHttpClient asyncHttpCLient = new AsyncHttpClient();

    public void sendValue(String id, String resourceId, String newValue) {
        String name = clientMap.get(id);
        System.out.println(resourceId);
        System.out.println(newValue);
        if (resourceId.equals("5703")) {
            if (Integer.parseInt(newValue) == 100) {
                System.out.println("x");
                sendParkRequest(name);
            } else if (Integer.parseInt(newValue) == -100) {
                System.out.println("y");
                sendUnparkRequest(name);
            }
        }
        //System.out.println("Sending " + newValue + " from " + name);
    }

    public void addNewClient(String id, String endpoint) {
        clientMap.put(id, endpoint);
    }

    private void sendParkRequest(String parkingSpot) {
        asyncHttpCLient.preparePut("http://192.168.99.100:8081/parkingspots/" + parkingSpot + "/park/Vehicle-5-1").execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response rspns) throws Exception {
                System.out.println("Parking");
                return rspns;
            }

            @Override
            public void onThrowable(Throwable t) {
                System.out.println("XXX");
                System.out.println(t.toString());
            }
        });
    }
    
    private void sendUnparkRequest(String parkingSpot) {
        asyncHttpCLient.prepareDelete("http://192.168.99.100:8081/parkingspots/" + parkingSpot + "/unpark").execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response rspns) throws Exception {
                System.out.println("Unparking!");
                return rspns;
            }

            @Override
            public void onThrowable(Throwable t) {
                System.out.println("XXX");
                System.out.println(t.toString());
            }
        });
    }
    
    

    public ArrayList<Reservation> checkForNewReservations() {
        ArrayList<Reservation> updates = new ArrayList<>();
        try {
            Future<Response> f = asyncHttpCLient.prepareGet("http://192.168.99.100:8081/parkingspots/").execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response rspns) throws Exception {
                    return rspns;
                }
            });
            String jsonString = f.get().getResponseBody();
            Object obj = JSONValue.parse(jsonString);
            //System.out.println("x" + obj.toString());
            JSONArray array = (JSONArray) obj;
//            System.out.println("y"+array.toString());
            for (int i = 0; i < array.size(); i++) {
                JSONObject object = (JSONObject) array.get(i);
//                System.out.println("z"+object.toString());
                //if (lastChecked == null || sdf.parse(object.get("updated_at").toString()).after(lastChecked)) {
//                    System.out.println("A");
                    String spot = object.get("uuid").toString();
                    String vehicle = "";
                    String state = "free";
                    if (object.get("reservation") == null) {
//                        System.out.println("Found null reservation");
                    } else {
                        state = "reserved";
                    }
                    if (object.get("vehicle") == null) {
//                        System.out.println("Found null vehicle");
                    } else {
                        vehicle = object.get("vehicle").toString();
                        state = "occupied";
                    }

                    
                    
                    System.out.println(state);
                    updates.add(new Reservation(spot, vehicle, state));
               // }
                //else {
//                    System.out.println("B");
//                    System.out.println(lastChecked == null);
//                    System.out.println(sdf.parse(object.get("updated_at").toString()).before(lastChecked));
//                    System.out.println(lastChecked);
//                    System.out.println(object.get("updated_at").toString());
                //}
            }
            lastChecked = new Date();
            return updates;
        } catch (InterruptedException | ExecutionException | IOException ex) {
            Logger.getLogger(DatastoreSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }
}
