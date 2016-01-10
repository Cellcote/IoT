package com.mycompany.iot.parking;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class SmartParkingSpotClient {

//    private final LeshanClient client;

    // the registration ID assigned by the server
    private String registrationId;
    
    private final State stateInstance = new State();

    private final Location locationInstance = new Location();

    public SmartParkingSpotClient(String endpointIdentifier, final String localHostName, final int localPort, final String serverHostName,
            final int serverPort) {
        
        // Initialize object list
//        ObjectsInitializer initializer = new ObjectsInitializer();
//
//        initializer.setClassForObject(3, Device.class);
//        initializer.setInstancesForObject(3345, stateInstance);
//        List<ObjectEnabler> enablers = initializer.createMandatory();
//        enablers.add(initializer.create(3345));
//
//        // Create client
//        final InetSocketAddress clientAddress = new InetSocketAddress(localHostName, localPort);
//        final InetSocketAddress serverAddress = new InetSocketAddress(serverHostName, serverPort);
//
//        client = new LeshanClient(clientAddress, serverAddress, new ArrayList<LwM2mObjectEnabler>(enablers));
//
//        // Start the client
//        client.start();
//
//        // Register to the server
//        //final String endpointIdentifier = UUID.randomUUID().toString();
//        
//        RegisterResponse response = client.send(new RegisterRequest(endpointIdentifier));
//        if (response == null) {
//            System.out.println("Registration request timeout");
//            return;
//        }
//
//        System.out.println("Device Registration (Success? " + response.getCode() + ")");
//        if (response.getCode() != ResponseCode.CREATED) {
//            // TODO Should we have a error message on response ?
//            // System.err.println("\tDevice Registration Error: " + response.getErrorMessage());
//            System.err.println(
//                    "If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
//            return;
//        }
//
//        registrationId = response.getRegistrationID();
//        System.out.println("\tDevice: Registered Client Location '" + registrationId + "'");
//
//        // Deregister on shutdown and stop client.
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                if (registrationId != null) {
//                    System.out.println("\tDevice: Deregistering Client '" + registrationId + "'");
//                    client.send(new DeregisterRequest(registrationId), 1000);
//                    client.stop();
//                }
//            }
//        });

        // Change the location through the Console
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press 'w','a','s','d' to change reported state.");
        while (scanner.hasNext()) {
            String nextMove = scanner.next();
            stateInstance.changeState(nextMove);
        }
        scanner.close();
        stateInstance.changeState(State.StateSpace.FREE);
        //locationInstance.moveLocation("w");
        
    }

    public static class Device extends BaseInstanceEnabler {

        private final String manufacturelModel = "EclipseCon Tuto Client";
        private final String modelNumber = "2015";
        private final String serialNumber = "leshan-client-001";
        private final BindingMode bindingModel = BindingMode.U;

        private AtomicLong currentTimestamp = new AtomicLong(0);

        public Device() {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    currentTimestamp.getAndAdd(1000);
                }
            }, 1000, 1000);
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Device Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getManufacturer());
                case 1:
                    return ReadResponse.success(resourceid, getModelNumber());
                case 2:
                    return ReadResponse.success(resourceid, getSerialNumber());
                case 3:
                    return ReadResponse.success(resourceid, getFirmwareVersion());
                case 9:
                    return ReadResponse.success(resourceid, getBatteryLevel());
                case 10:
                    return ReadResponse.success(resourceid, getMemoryFree());
                case 11:
                    Map<Integer, Long> errorCodes = new HashMap<>();
                    errorCodes.put(0, getErrorCode());
                    return ReadResponse.success(resourceid, errorCodes, ResourceModel.Type.INTEGER);
                case 13:
                    return ReadResponse.success(resourceid, getCurrentTime());
                case 14:
                    return ReadResponse.success(resourceid, getUtcOffset());
                case 15:
                    return ReadResponse.success(resourceid, getTimezone());
                case 16:
                    return ReadResponse.success(resourceid, getSupportedBinding());
                // TODO read resources
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
                case 13:
                    return WriteResponse.notFound();
                case 14:
                    setUtcOffset((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                case 15:
                    setTimezone((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        private String getManufacturer() {
            return "Leshan Example Device";
        }

        private String getModelNumber() {
            return "Model 500";
        }

        private String getSerialNumber() {
            return "LT-500-000-0001";
        }

        private String getFirmwareVersion() {
            return "1.0.0";
        }

        private long getErrorCode() {
            return 0;
        }

        private int getBatteryLevel() {
            final Random rand = new Random();
            return rand.nextInt(100);
        }

        private int getMemoryFree() {
            final Random rand = new Random();
            return rand.nextInt(50) + 114;
        }

        private Date getCurrentTime() {
            return new Date();
        }

        private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

        ;

        private String getUtcOffset() {
            return utcOffset;
        }

        private void setUtcOffset(String t) {
            utcOffset = t;
        }

        private String timeZone = TimeZone.getDefault().getID();

        private String getTimezone() {
            return timeZone;
        }

        private void setTimezone(String t) {
            timeZone = t;
        }

        private String getSupportedBinding() {
            return "U";
        }

        // TODO write current timestamp
        // TODO exec reboot
    }
    
    public static class State extends BaseInstanceEnabler {
        public enum StateSpace {
            FREE, OCCUPIED, RESERVED;
        }
        public StateSpace state;
        
        public State() {
            state = StateSpace.FREE;
        }
        
        public void changeState(String s) {
            if (s.charAt(0) == 'w') {
                changeState(State.StateSpace.OCCUPIED);
            } else if (s.charAt(0) == 's') {
                changeState(State.StateSpace.FREE);
            }
        }
        
        public void changeState(StateSpace s) {
            state = s;
            switch(s) {
                case FREE:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_green.py ");
                    } catch(Exception e) {
                        System.out.println("green err");
                    }
                    //Call python executable to put lights to Green
                    break;
                case OCCUPIED:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_red.py ");
                    } catch(Exception e) { 
                        System.out.println("red err");
                    }
                    //Callpython executable to put lights to Red
                    break;
                case RESERVED:
                    try {
                        Process p = Runtime.getRuntime().exec("python set_orange.py ");
                    } catch(Exception e) {
                        System.out.println("orange err");
                    }
                    //Call pyhon executable to put lights to Orange
                    break;
            }
        }
        
        public String stateOf(StateSpace s) {
            switch(s) {
                case FREE:
                    return "free";
                case OCCUPIED:
                    return "occupied";
                case RESERVED:
                    return "reserved";
                default:
                    return "none";
            }
        }
        
        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 3345:
                    return ReadResponse.success(resourceid, stateOf(state));
//                case 5500:
//                    return ReadResponse.success(resourceid, getDigitalInputState());
//                case 5501:
//                    return ReadResponse.success(resourceid, getDigitalInputCounter());
//                case 5702:
//                    return ReadResponse.success(resourceid, getXValue());
//                case 5703:
//                    return ReadResponse.success(resourceid, getYValue());
//                case 5704:
//                    return ReadResponse.success(resourceid, getZValue());
//                case 5750:
//                    return ReadResponse.success(resourceid, getApplicationType());
                default:
                    return super.read(resourceid);
            }
        }
        
        
    }

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
                case 5:
                    return ReadResponse.success(resourceid, getTimestamp());
                default:
                    return super.read(resourceid);
            }
        }

        public void moveLocation(String nextMove) {
            System.out.println("move:"+nextMove.charAt(0));
            switch (nextMove.charAt(0)) {
                case 'w':
                    moveLatitude(1.0f);
                    break;
                case 'a':
                    moveLongitude(-1.0f);
                    break;
                case 's':
                    moveLatitude(-1.0f);
                    break;
                case 'd':
                    moveLongitude(1.0f);
                    break;
            }
        }

        private void moveLatitude(float delta) {
            latitude = latitude + delta;
            timestamp = new Date();
            fireResourcesChange(0, 5);
        }

        private void moveLongitude(float delta) {
            longitude = longitude + delta;
            timestamp = new Date();
            fireResourcesChange(1, 5);
        }

        public String getLatitude() {
            return Float.toString(latitude - 90.0f);
        }

        public String getLongitude() {
            return Float.toString(longitude - 180.f);
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    /**
     * public void write() {
     *
     *
     * WriteResponse response = client.send(new
     * WriteRequest(WriteRequest.Mode.UPDATE, 3, col)); }
     */
    /**
     * public void read() {
     *
     * client.send(new ReadRequest(registrationId));
     *
     * if(response.getCode() == ResponseCode.) {
     *
     * }
     * }
     */
    public static void main(String[] args) {

        String serverHost = "192.168.128.128";
        //String serverHost = "leshan.eclipse.org";

        SmartParkingSpotClient client = new SmartParkingSpotClient("Parking-Spot-5", "0", 0, serverHost, 5683);
        
        //need gui to listen to arrow keys
        //client.addKeyListener(client);
    }
}
