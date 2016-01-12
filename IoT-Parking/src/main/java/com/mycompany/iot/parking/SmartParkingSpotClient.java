package com.mycompany.iot.parking;

import biz.source_code.utils.RawConsoleInput;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

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

    public volatile static int externalY = 0;

//    private final State stateInstance = new State();
    public SmartParkingSpotClient(String endpointIdentifier, final String localHostName, final int localPort, final String serverHostName,
            final int serverPort) {

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer();

        initializer.setClassForObject(3341, AddressableTextDisplayObject.class);
        initializer.setClassForObject(3345, MultipleAxisJoystickObject.class);
        initializer.setClassForObject(32700, ParkingSpotObject.class);
        initializer.setClassForObject(6, Location.class);

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
                    //Device.setState(State.occupied);
                    externalY = 100;
                } else if (result == 66) { //DOWN
                    //Device.setState(State.free);
                    externalY = -100;
                }
            } catch (Exception e) {

            }
        }
        //stateInstance.changeState(State.StateSpace.FREE);
        //locationInstance.moveLocation("w");

    }

    /*public static class Device extends BaseInstanceEnabler {

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
                    } catch (Exception e) {
                    }
                    break;
                case occupied:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_red.py");
                    } catch (Exception e) {
                    }
                    break;
                case reserved:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_orange.py");
                    } catch (Exception e) {
                    }
                    break;
            }

            // TODO write current timestamp
            // TODO exec reboot
        }
    }*/

    public static class Location extends BaseInstanceEnabler {

        private Random random;
        private float latitude;
        private float longitude;
        private Date timestamp;

        public Location() {
            random = new Random();
            latitude = Float.valueOf(random.nextInt(180));
            longitude = Float.valueOf(random.nextInt(360));
            timestamp = new Date();
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getLatitude());
                case 1:
                    return ReadResponse.success(resourceid, getLongitude());
                case 2:
                    return ReadResponse.success(resourceid, "");
                case 3:
                    return ReadResponse.success(resourceid, "");
                case 4:
                    return ReadResponse.success(resourceid, "");
                case 5:
                    return ReadResponse.success(resourceid, timestamp);
                default:
                    return super.read(resourceid);
            }
        }

        public String getLatitude() {
            return Float.toString(random.nextFloat());
        }

        public String getLongitude() {
            return Float.toString(random.nextFloat());
        }
    }

    public static class ParkingSpotObject extends BaseInstanceEnabler {

        private String name = "Parking-Spot-5";
        private String state = "free";
        private String vehicleId = "";
        private float billingRate = 0;

        public ParkingSpotObject() {
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 32800:
                    return ReadResponse.success(resourceid, name);
                case 32801:
                    return ReadResponse.success(resourceid, state);
                case 32802:
                    return ReadResponse.success(resourceid, vehicleId);
                case 32803:
                    return ReadResponse.success(resourceid, billingRate);
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Device Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 32801:
                    this.state = (String) value.getValue();
                    return WriteResponse.success();
                case 32802:
                    this.vehicleId = (String) value.getValue();
                    return WriteResponse.success();
                case 32803:
                    this.billingRate = Float.parseFloat(String.valueOf(value.getValue()));
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

    }

    public static class MultipleAxisJoystickObject extends BaseInstanceEnabler {

        private int yValue = 0;
        private int counter = 0;

        public MultipleAxisJoystickObject() {

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                    if (externalY != 0) {
                        yValue = externalY;
                        makeChange();
                        externalY = 0;
                    }
                }
            }, 1000, 100);
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 5500:
                    return ReadResponse.success(resourceid, true);
                case 5501:
                    return ReadResponse.success(resourceid, counter);
                case 5702:
                    return ReadResponse.success(resourceid, -100);
                case 5703:
                    return ReadResponse.success(resourceid, yValue);
                case 5704:
                    return ReadResponse.success(resourceid, -100);
                case 5750:
                    return ReadResponse.success(resourceid, "");
                default:
                    return super.read(resourceid);
            }
        }

        private void makeChange() {
            fireResourcesChange(5703);
        }
    }

    public static class AddressableTextDisplayObject extends BaseInstanceEnabler {

        private String text = "free";

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 5527:
                    return ReadResponse.success(resourceid, this.text);
                case 5528:
                    return ReadResponse.success(resourceid, 0);
                case 5529:
                    return ReadResponse.success(resourceid, 0);
                case 5545:
                    return ReadResponse.success(resourceid, 0);
                case 5546:
                    return ReadResponse.success(resourceid, 0);
                case 5530:
                    return ReadResponse.success(resourceid, 0);
                case 5548:
                    return ReadResponse.success(resourceid, 0f);
                case 5531:
                    return ReadResponse.success(resourceid, 0f);
                case 5750:
                    return ReadResponse.success(resourceid, "0");
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Device Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 5527:
                    State s = State.valueOf((String) value.getValue());
                    setLight(s);
                    this.text = (String) value.getValue();
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceId, String params) {
            return ExecuteResponse.success();
        }

        private void setLight(State s) {
            switch (s) {
                case free:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_green.py");
                    } catch (Exception e) {
                    }
                    break;
                case occupied:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_red.py");
                    } catch (Exception e) {
                    }
                    break;
                case reserved:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_orange.py");
                    } catch (Exception e) {
                    }
                    break;
            }
        }
    }
    
    static class BonjourServiceListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
//            System.out.println("Service added   : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
//            System.out.println("Service removed : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
//            System.out.println("Service resolved: " + event.getInfo());
        }
    }

    public static void main(String[] args) {

        try {
            String bonjourServiceType = "_coap._udp.local.";
            JmDNS bonjourService = JmDNS.create();
            bonjourService.addServiceListener(bonjourServiceType, new BonjourServiceListener());
            ServiceInfo[] serviceInfos = bonjourService.list(bonjourServiceType);
            for (ServiceInfo info : serviceInfos) {
                String s = info.getURL();
                //remove "http://" and ":5683".
                String ip = s.substring("http://".length(), s.length() - ":5683".length());
                System.out.println("Found service: " + info.getName() + " with ip: " + ip);
                int port = 5683;
                
                SmartParkingSpotClient client = new SmartParkingSpotClient("Parking-Spot-5", "0", 0, ip, port);
                
                continue;
              
            }
            bonjourService.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
