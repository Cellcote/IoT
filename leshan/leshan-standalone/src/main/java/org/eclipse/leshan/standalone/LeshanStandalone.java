/**
 * *****************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 ******************************************************************************
 */
package org.eclipse.leshan.standalone;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.californium.core.network.EndpointObserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.standalone.servlet.ClientServlet;
import org.eclipse.leshan.standalone.servlet.EventServlet;
import org.eclipse.leshan.standalone.servlet.ObjectSpecServlet;
import org.eclipse.leshan.standalone.servlet.SecurityServlet;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeshanStandalone {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanStandalone.class);

    private Server server;
    private LeshanServer lwServer = null;
    private ObservationListener observationListener;
    private DatastoreSender datastore = new DatastoreSender();
    private AsyncHttpClient asyncHttpCLient = new AsyncHttpClient();

    public void start() {
        // Use those ENV variables for specifying the interface to be bound for coap and coaps
        String iface = System.getenv("COAPIFACE");
        String ifaces = System.getenv("COAPSIFACE");

        // Build LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        if (iface != null && !iface.isEmpty()) {
            builder.setLocalAddress(iface.substring(0, iface.lastIndexOf(':')),
                    Integer.parseInt(iface.substring(iface.lastIndexOf(':') + 1, iface.length())));
        }
        if (ifaces != null && !ifaces.isEmpty()) {
            builder.setLocalAddressSecure(ifaces.substring(0, ifaces.lastIndexOf(':')),
                    Integer.parseInt(ifaces.substring(ifaces.lastIndexOf(':') + 1, ifaces.length())));
        }

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            builder.setSecurityRegistry(new SecurityRegistryImpl(privateKey, publicKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            LOG.warn("Unable to load RPK.", e);
        }

        lwServer = builder.build();

        observationListener = new ObservationListener(datastore);
        // listen for observe notifications
        lwServer.getObservationRegistry().addListener(observationListener);

        lwServer.getClientRegistry().addListener(new ClientRegistryListener() {
            @Override
            public void registered(Client client) {
                System.out.println("New client registered");
                asyncHttpCLient.preparePut("http://192.168.99.100:8081/parkingspots/" + client.getEndpoint()).execute(new AsyncCompletionHandler<Response>() {
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
                ObserveRequest request = new ObserveRequest("/6");
                lwServer.send(client, request);
                request = new ObserveRequest("/3341");
                lwServer.send(client, request);
                request = new ObserveRequest("/3345/0/5703");
                lwServer.send(client, request);
                request = new ObserveRequest("/32700");
                lwServer.send(client, request);
                datastore.addNewClient(client.getRegistrationId(), client.getEndpoint());
            }

            @Override
            public void updated(Client clientUpdated) {
                System.out.println("Client updated");
            }

            @Override
            public void unregistered(Client client) {
                System.out.println("Client unregistered");
            }
        });

        lwServer.start();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (lwServer == null) {
                    return;
                }
                ArrayList<Reservation> updates = datastore.checkForNewReservations();
                if (updates != null) {
                    for (int i = 0; i < updates.size(); i++) {
                        String spot = updates.get(i).spot;
                        Client c = findClient(spot);
                        String v = updates.get(i).vehicle;
                        String s = updates.get(i).state;
                        //System.out.println("Vehicle" + v);
                        //System.out.println("State" + s);
                        WriteRequest writeRequest = new WriteRequest(WriteRequest.Mode.REPLACE, 32700, 0, 32801, s + "");
                        lwServer.send(c, writeRequest);
                        writeRequest = new WriteRequest(WriteRequest.Mode.REPLACE, 32700, 0, 32802, v + " ");
                        lwServer.send(c, writeRequest);
                        writeRequest = new WriteRequest(WriteRequest.Mode.REPLACE, 3341, 0, 5527, s);
                        lwServer.send(c, writeRequest);
                        asyncHttpCLient.preparePut("http://192.168.99.100:8081/parkingspots/"+spot).execute(new AsyncCompletionHandler<Response>() {
                            @Override
                            public Response onCompleted(Response rspns) throws Exception {
                                //System.out.println("Client heartbeat");
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
            }
        }, 2000, 1000);

        // Now prepare and start jetty
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = System.getProperty("PORT");
        }
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        server = new Server(Integer.valueOf(webPort));
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(this.getClass().getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecureAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer, lwServer.getSecureAddress()
                .getPort()));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(lwServer.getSecurityRegistry()));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Start jetty
        try {
            server.start();
        } catch (Exception e) {
            LOG.error("jetty error", e);
        }
    }

    public void stop() {
        try {
            lwServer.destroy();
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new LeshanStandalone().start();
    }

    private Client findClient(String spotName) {
        return lwServer.getClientRegistry().get(spotName);
    }
}
