package org.yawlfoundation.yawl.fabric.bridge;

/**
 * @author Michael Adams
 * @date 24/9/18
 */

import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.yawlfoundation.yawl.fabric.AppUser;
import org.yawlfoundation.yawl.fabric.CarRecord;
import org.yawlfoundation.yawl.fabric.event.BlockUpdateListener;
import org.yawlfoundation.yawl.fabric.event.YBlockListener;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <h1>HFInterface</h1>
 * <p>
 * Simple example showcasing basic fabric-ca and fabric actions.
 * The demo required fabcar fabric up and running.
 * <p>
 * The demo shows
 * <ul>
 * <li>connecting to fabric-ca</li>
 * <li>enrolling admin to get new key-pair, certificate</li>
 * <li>registering and enrolling a new user using admin</li>
 * <li>creating HF client and initializing channel</li>
 * <li>invoking chaincode query</li>
 * </ul>
 *
 * @author lkolisko
 */
public class HFInterface {

    private HFClient _client;
    private AppUser _admin;
    private List<AppUser> _users;
    private final YBlockListener _blockListener = new YBlockListener();

    private final Props _props = new Props();

    private static final Logger log = Logger.getLogger(HFInterface.class);


    public HFInterface() {
        log.info("Initialising YAWL-Hyperledger interface");
        AppUser admin = enrolUsers();
        if (admin != null) {
            _client = initHfClient(admin);
        }
        try {
            initChannel();
            log.info("Initialisation Completed");
        }
        catch(Exception e) {
            log.error("Initialisation failed: " + e.getMessage());
        }
    }


    private AppUser enrolUsers() {
        try {
            HFCAClient caClient = getHfCaClient(_props.getCaURL(), null);

            // enrol or load admin
            _admin = initAdmin(caClient);

            // register and enrol new users
            _users = new ArrayList<>();
            for (Props.User user : _props.getUsers()) {
                _users.add(initUser(caClient, _admin, user));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return _admin;
    }

    
    private void print(List<CarRecord> cars) {
        for (CarRecord car : cars) {
            System.out.println(car.getKey() + ": " + car.getRecord().toString());
        }
    }


    /**
     * Invoke blockchain query
     *
     * @throws ProposalException
     * @throws InvalidArgumentException
     */
    public Collection<ProposalResponse> query(String query, String... argsArray)
            throws ProposalException, InvalidArgumentException {

        // create new chaincode request
        if (argsArray == null) argsArray = new String[0];
        QueryByChaincodeRequest qpr = _client.newQueryProposalRequest();
        buildChainCodeParams(qpr, query, argsArray);            // set up request
        return _client.getChannel(_props.getChannelName()).queryByChaincode(qpr);
    }
    

    private List<CarRecord> parseResponses(Collection<ProposalResponse> responses) {
        // display response
//        for (ProposalResponse response : responses) {
//            if (response.isVerified() && response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
//                ByteString payload = response.getProposalResponse().getResponse().getPayload();
//                try (JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(payload.toByteArray()))) {
//                    // parse response
//                    JsonArray arr = jsonReader.readArray();
//                    List<CarRecord> cars = new ArrayList<>();
//                    for (int i = 0; i < arr.size(); i++) {
//                        JsonObject rec = arr.getJsonObject(i);
//                        cars.add(new CarRecord(rec));
//                    }
//                    return cars;
//                }
//            } else {
//                log.error("response failed. status: " + response.getStatus().getStatus());
//            }
//        }
        return Collections.emptyList();
    }


    private void buildChainCodeParams(TransactionRequest request, String fcn, String... args) {
        ChaincodeID cid = ChaincodeID.newBuilder().setName(_props.getChainCodeName()).build();
        request.setChaincodeID(cid);
        request.setFcn(fcn);
        if (! (args == null || args.length == 0)) {
            request.setArgs(args);
        }
    }


    public void registerBlockUpdateListener(BlockUpdateListener listener) {
        _blockListener.registerUpdateListener(listener);
    }


    public CompletableFuture<BlockEvent.TransactionEvent> invoke(String fcn, String... args)
            throws ProposalException, InvalidArgumentException {
        TransactionProposalRequest tpr = _client.newTransactionProposalRequest();
        buildChainCodeParams(tpr, fcn, args);
        Channel channel = _client.getChannel(_props.getChannelName());
        Collection<ProposalResponse> responses = channel.sendTransactionProposal(tpr);
        List<ProposalResponse> invalid = responses.stream().filter(
                r -> r.isInvalid()).collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            invalid.forEach(response -> {
                log.error(response.getMessage());
            });
            throw new RuntimeException("invalid response(s) found");
        }
        return channel.sendTransaction(responses);
    }

    /**
     * Initialize and get HF channel
     *
     * @return Initialized channel
     * @throws InvalidArgumentException
     * @throws TransactionException
     */
    private Channel initChannel() throws InvalidArgumentException, TransactionException {
        Channel channel = _client.newChannel(_props.getChannelName());

        // peer name and endpoint in fabcar network
        for (Props.NameURLPair pair : _props.getPeers()) {
            Peer peer = _client.newPeer(pair.name, pair.url);
            channel.addPeer(peer);
        }

        // eventhub name and endpoint in fabcar network
//        Props.NameURLPair eventPair = _props.getEventHub();
//        EventHub eventHub = _client.newEventHub(eventPair.name, eventPair.url);
//        channel.addEventHub(eventHub);

        // orderer name and endpoint in fabcar network
        for (Props.NameURLPair pair : _props.getOrderers()) {
            Orderer orderer = _client.newOrderer(pair.name, pair.url);
            channel.addOrderer(orderer);
        }

        channel.registerBlockListener(_blockListener);

        channel.initialize(); 
        return channel;
    }

    /**
     * Create new HLF client
     *
     * @return new HLF client instance. Never null.
     * @throws CryptoException
     * @throws InvalidArgumentException
     */
    private HFClient initHfClient(User userContext) {
        try {
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(userContext);
            return client;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Register and enroll user with userId.
     * If AppUser object with the name already exist on fs it will be loaded and
     * registration and enrollment will be skipped.
     *
     * @param caClient  The fabric-ca client.
     * @param registrar The registrar to be used.
     * @param user    The user id.
     * @return AppUser instance with userId, affiliation,mspId and enrollment set.
     * @throws Exception
     */
    private AppUser initUser(HFCAClient caClient, AppUser registrar, Props.User user)
            throws Exception {
        AppUser appUser = tryDeserialize(user.id);
        if (appUser == null) {
            RegistrationRequest rr = new RegistrationRequest(user.id, user.affiliation);
            String enrollmentSecret = caClient.register(rr, registrar);
            Enrollment enrollment = caClient.enroll(user.id, enrollmentSecret);
            appUser = new AppUser(user.id, user.affiliation, user.mspid, enrollment);
            serialize(appUser);
        }
        return appUser;
    }

    /**
     * Enroll admin into fabric-ca using {@code admin/adminpw} credentials.
     * If AppUser object already exist serialized on fs it will be loaded and
     * new enrollment will not be executed.
     *
     * @param caClient The fabric-ca client
     * @return AppUser instance with userid, affiliation, mspId and enrollment set
     * @throws Exception
     */
    private AppUser initAdmin(HFCAClient caClient) throws EnrollmentException,
            org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException,
            IOException, ClassNotFoundException {
        Props.User user = _props.getAdmin();
        AppUser admin = tryDeserialize(user.id);
        if (admin == null) {
            Enrollment adminEnrollment = caClient.enroll(user.id, user.pw);
            admin = new AppUser(user.id, user.affiliation, user.mspid, adminEnrollment);
            serialize(admin);
        }
        return admin;
    }

    /**
     * Get new fabic-ca client
     *
     * @param caUrl              The fabric-ca-server endpoint url
     * @param caClientProperties The fabri-ca client properties. Can be null.
     * @return new client instance. never null.
     * @throws Exception
     */
    private HFCAClient getHfCaClient(String caUrl, Properties caClientProperties)
            throws IllegalAccessException, InvocationTargetException,
            InvalidArgumentException, InstantiationException, NoSuchMethodException,
            CryptoException, ClassNotFoundException, MalformedURLException {
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        HFCAClient caClient = HFCAClient.createNewInstance(caUrl, caClientProperties);
        caClient.setCryptoSuite(cryptoSuite);
        return caClient;
    }


    // user serialization and deserialization utility functions
    // files are stored in the base directory

    /**
     * Serialize AppUser object to file
     *
     * @param appUser The object to be serialized
     * @throws IOException
     */
    private void serialize(AppUser appUser) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(
                Paths.get(appUser.getName() + ".jso")))) {
            oos.writeObject(appUser);
        }
    }

    /**
     * Deserialize AppUser object from file
     *
     * @param name The name of the user. Used to build file name ${name}.jso
     * @return
     * @throws Exception
     */
    private AppUser tryDeserialize(String name) throws IOException, ClassNotFoundException {
        if (Files.exists(Paths.get(name + ".jso"))) {
            return deserialize(name);
        }
        return null;
    }

    private AppUser deserialize(String name) throws IOException, ClassNotFoundException {
        try (ObjectInputStream decoder = new ObjectInputStream(
                Files.newInputStream(Paths.get(name + ".jso")))) {
            return (AppUser) decoder.readObject();
        }
    }


    public static void main(String[] args) throws Exception {
        HFInterface hfi = new HFInterface();

//        Collection<ProposalResponse> responses = hfi.query("queryAllCars", null);
//        List<CarRecord> cars = hfi.parseResponses(responses);
//        hfi.print(cars);
//
//        responses = hfi.query("queryCar", "CAR4");
//        cars = hfi.parseResponses(responses);
//        hfi.print(cars);

        // add a car
//        String[] fcnArgs = {"CAR11", "Skoda", "MB1000", "Yellow", "Lukas"};
//        BlockEvent.TransactionEvent event = hfi.invoke("createCar", fcnArgs).get(60, TimeUnit.SECONDS);
//        if (event.isValid()) {
//            log.info("Transaction tx: " + event.getTransactionID() + " has completed successfully.");
//        } else {
//            log.error("Transaction tx: " + event.getTransactionID() + " is invalid.");
//        }
        String[] fcnArgs = {"report", "2"};
        BlockEvent.TransactionEvent event = hfi.invoke("invoke", fcnArgs).get(60, TimeUnit.SECONDS);

        fcnArgs = new String[]{"edits", "5"};
        event = hfi.invoke("invoke", fcnArgs).get(60, TimeUnit.SECONDS);

        fcnArgs = new String[]{"Tiny Owl"};
        Collection<ProposalResponse> responses = hfi.query("queryHistory", fcnArgs);
        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                String msg = response.getMessage();
                try (JsonParser parser = Json.createParser(new StringReader(msg))) {

                    // parse response
                    JsonArray arr = parser.getArray();
                    List<String> cars = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject rec = arr.getJsonObject(i);
                        cars.add(rec.toString());
                    }
                }
                catch (Exception e) {}
                  System.out.println(msg);
            }
        }

//        if (event.isValid()) {
//            log.info("Transaction tx: " + event.getTransactionID() + " has completed successfully.");
//        } else {
//            log.error("Transaction tx: " + event.getTransactionID() + " is invalid.");
//        }
    }
    
}
