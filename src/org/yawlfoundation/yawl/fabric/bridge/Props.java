package org.yawlfoundation.yawl.fabric.bridge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 27/9/18
 */
public class Props {

    private static final String PROPS_FILE_NAME =
            "org/yawlfoundation/yawl/fabric/bridge/serviceProps.xml";

    private Logger _logger;
    private String _caURL;
    private String _channelName;
    private String _chainCodeName;
    private User _admin;
    private List<User> _users;
    private List<NameURLPair> _peers;   // name::url
    private List<NameURLPair> _orderers;
    private NameURLPair _eventHub;


    public Props() {
        _logger  = LogManager.getLogger(this.getClass());
        load();
    }


    public String getCaURL() { return _caURL; }

    public String getChannelName() { return _channelName; }

    public String getChainCodeName() { return _chainCodeName; }

    public User getAdmin() { return _admin; }

    public List<User> getUsers() { return _users; }

    public List<NameURLPair> getPeers() { return _peers; }

    public List<NameURLPair> getOrderers() { return _orderers; }

    public NameURLPair getEventHub() { return _eventHub; }


    private void load() {
//        String xml = StringUtil.fileToString("/Users/adamsmj/Documents/Git/YAWLFabric/src/org/yawlfoundation/yawl/fabric/bridge/serviceProps.xml");
//        if (xml != null) {
//            parse(xml);
//        }

        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(PROPS_FILE_NAME);
        if (in != null) {
            String xml = StringUtil.streamToString(in);
            if (xml != null) {
                parse(xml);
            }
            else {
                _logger.warn("Error reading file '{}'.", PROPS_FILE_NAME);
            }
        }
        else _logger.warn("Error opening file '{}'.", PROPS_FILE_NAME);
    }


    private void parse(String xml) {
        XNode root = new XNodeParser().parse(xml);
        if (root == null) {
            _logger.error("Invalid config xml in file '{}'.", PROPS_FILE_NAME);
            return;
        }
        _caURL = root.getChildText("caurl");
        _channelName = root.getChildText("channel");
        _chainCodeName = root.getChildText("chaincode");
        parseUsers(root.getChild("users"));
        parsePeers(root.getChild("peers"));
        parseOrderers(root.getChild("orderers"));
        parseEventHub(root.getChild("eventhub"));
    }


    private void parseUsers(XNode parent) {
        _users = new ArrayList<>();
        for (XNode node : parent.getChildren()) {
            String id = node.getChildText("id");
            String affiliation = node.getChildText("affiliation");
            String mspid = node.getChildText("mspid");
            String pw = node.getChildText("pw");
            User user = new User(id, affiliation, mspid, pw);
            if (id.equals("admin")) {
                _admin = user;
            }
            else {
                _users.add(user);
            }
        }
    }


    private void parsePeers(XNode parent) {
        _peers = new ArrayList<>();
        for (XNode node : parent.getChildren()) {
            String domain = node.getChildText("domain");
            String url = node.getChildText("url");
            int count = StringUtil.strToInt(node.getChildText("count"), 0);
            for (int i=0; i<count; i++) {
                String name = "peer" + i;
                _peers.add(new NameURLPair(name + "." + domain, url));
            }
        }
    }

    
    private void parseOrderers(XNode parent) {
        _orderers = new ArrayList<>();
        for (XNode node : parent.getChildren()) {
            String name = node.getChildText("name");
            String domain = node.getChildText("domain");
            String url = node.getChildText("url");
            _orderers.add(new NameURLPair(name + "." + domain, url));
        }
    }

    private void parseEventHub(XNode node) {
        if (node != null) {
            String name = node.getChildText("name");
            String url = node.getChildText("url");
            _eventHub = new NameURLPair(name, url);
        }
    }



    protected class NameURLPair {
        String name;
        String url;

        NameURLPair(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }


    protected class User {
        String id;
        String affiliation;
        String mspid;
        String pw;

        User(String id, String affiliation, String mspid, String pw) {
            this.id = id;
            this.affiliation = affiliation;
            this.mspid = mspid;
            this.pw = pw;
        }
    }

}
