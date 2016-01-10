package com.mycompany.iot.parking;

import biz.source_code.utils.RawConsoleInput;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class SmartParkingSpotClient {

    private final LeshanClient client;

    // the registration ID assigned by the server
    private String registrationId;

    public volatile static State externalState = null;

//    private final State stateInstance = new State();
    public SmartParkingSpotClient(String endpointIdentifier, final String localHostName, final int localPort, final String serverHostName,
            final int serverPort) {

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer();

        initializer.setClassForObject(3, Device.class);

        //initializer.setInstancesForObject(32801, stateInstance);
        List<ObjectEnabler> enablers = initializer.createMandatory();
        //aenablers.add(initializer.create(32801));
        // Create client
        final InetSocketAddress clientAddress = new InetSocketAddress(localHostName, localPort);
        final InetSocketAddress serverAddress = new InetSocketAddress(serverHostName, serverPort);

        client = new LeshanClient(clientAddress, serverAddress, new ArrayList<LwM2mObjectEnabler>(enablers));

        // Start the client
        client.start();

        // Register to the server
        //final String endpointIdentifier = UUID.randomUUID().toString();
        RegisterResponse response = client.send(new RegisterRequest(endpointIdentifier));
        if (response == null) {
            System.out.println("Registration request timeout");
            return;
        }

        System.out.println("Device Registration (Success? " + response.getCode() + ")");
        if (response.getCode() != ResponseCode.CREATED) {
            // TODO Should we have a error message on response ?
            // System.err.println("\tDevice Registration Error: " + response.getErrorMessage());
            System.err.println(
                    "If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
            return;
        }

        registrationId = response.getRegistrationID();
        System.out.println("\tDevice: Registered Client Location '" + registrationId + "'");

        // Deregister on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (registrationId != null) {
                    System.out.println("\tDevice: Deregistering Client '" + registrationId + "'");
                    client.send(new DeregisterRequest(registrationId), 1000);
                    client.stop();
                }
            }
        });

        // Change the location through the Console
        /*Scanner scanner = new Scanner(System.in);
        System.out.println("Press 'w','a','s','d' to change reported state.");
        while (scanner.hasNext()) {
            String nextMove = scanner.next();
            //stateInstance.changeState(nextMove);
        }
        scanner.close();*/
        while (true) {
            try {
                int result = RawConsoleInput.read(true);
                //System.out.println(result);
                if (result == 65) { //UP
                    Device.setState(State.occupied);
                    externalState = State.occupied;
                } else if (result == 66) { //DOWN
                    Device.setState(State.free);
                    externalState = State.free;
                } else if (result == 68) { //LEFT
                    Device.setState(State.reserved);
                    externalState = State.reserved;
                } else if (result == 67) {
                } //RIGHT
            } catch (Exception e) {

            }
        }
        //stateInstance.changeState(State.StateSpace.FREE);
        //locationInstance.moveLocation("w");

    }

    public static class Device extends BaseInstanceEnabler {

        private State state = State.free;

        public Device() {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (externalState != null) {
                        state = externalState;
                        setState(state.toString());
                        externalState = null;
                    }
                }
            }, 1000, 100);
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Device Resource " + resourceid);
            switch (resourceid) {
                case 32801:
                    return ReadResponse.success(resourceid, state.toString());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceid, String params) {
            System.out.println("Execute on Device resource " + resourceid);
            if (params != null && params.length() != 0) {
                System.out.println("\t params " + params);
            }
            return ExecuteResponse.success();
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Device Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 32801:
                    setState((String) value.getValue());
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        private void setState(String s) {
            this.state = State.valueOf(s);
            fireResourcesChange(32801);
            setState(state);
        }

        public static void setState(State s) {
            switch (s) {
                case free:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_green.py");
                    } catch (Exception e) { }
                    break;
                case occupied:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_red.py");
                    } catch (Exception e) { }
                    break;
                case reserved:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_orange.py");
                    } catch (Exception e) { }
                    break;
            }

            // TODO write current timestamp
            // TODO exec reboot
        }
    }

    public static void main(String[] args) {

        String serverHost = "192.168.1.14";
        //String serverHost = "leshan.eclipse.org";

        SmartParkingSpotClient client = new SmartParkingSpotClient("Parking-Spot-5", "0", 0, serverHost, 5683);
    }
}
