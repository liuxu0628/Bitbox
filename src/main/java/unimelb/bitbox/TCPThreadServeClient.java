package unimelb.bitbox;

import unimelb.bitbox.util.*;
import javax.net.ServerSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import org.json.simple.parser.ParseException;

public class TCPThreadServeClient extends Thread {

    private static int serveClientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
    private static String[] authorizedKeys = Configuration.getConfigurationValue("authorized_keys").split(",");
    private static String sessionKey = "";
    private FileSystemManager fileSystemManager;
    private Socket peerToPeer = null;

    public TCPThreadServeClient(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    @Override
    public void run() {
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket serverSocket = factory.createServerSocket(serveClientPort)) {
            // Wait for the connection
            while (true) {
                // Receive one connection
                Socket serveClient = serverSocket.accept();
                // Input stream
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(serveClient.getInputStream(), "UTF8"));
                try {
                    while (!serveClient.isClosed()) {
//                    if (clientInput.ready()) {
                        // Client message
                        String clientMessage = clientInput.readLine();
                        // Parse the request
                        Document clientRequest = Document.parse(clientMessage);

                        // Receive "AUTH_REQUEST"
                        if (clientRequest.containsKey("command")
                                && clientRequest.getString("command").equals("AUTH_REQUEST")) {
                            System.out.println("Received from Client " + serveClient.getInetAddress().toString()
                                    + ":" + serveClient.getPort() + " : " + clientRequest.getString("command"));

                            String identity = clientRequest.getString("identity");
                            // Generate session key
                            String passphrase = "Welcome, " + identity + "!";
                            sessionKey = AES.getKey(passphrase).toString();
                            Authorization authorization = new Authorization(serveClient);
                            for (String authorizedKey : authorizedKeys) {
                                if (authorizedKey.contains(identity)) {
                                    authorization.Authorization_response(sessionKey, authorizedKey);
                                } else {
                                    authorization.Authorization_response(sessionKey, "");
                                }
                            }
                        }

                        // Classify client request
                        if (clientRequest.containsKey("payload")) {
                            String content = clientRequest.getString("payload");
                            content = new String(Base64.getDecoder().decode(content));
                            content = AES.Session_decrypt(content, AES.getKey(sessionKey));
                            Document res = Document.parse(content);

                            String clientCommand = res.getString("command");
                            switch (clientCommand) {
                                case "LIST_PEERS_REQUEST":
                                    ArrayList<HostPort> knownPeers = new ArrayList();
                                    String[] peerList = Configuration.getConfigurationValue("peers").split(",");
                                    try {
                                        for (String peer : peerList) {
                                            knownPeers.add(new HostPort(peer));
                                        }
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        System.out.println("Peer list is empty!");
                                    }
                                    System.out.println(knownPeers.toString());
                                    ListPeers listPeers = new ListPeers(serveClient, sessionKey);
                                    listPeers.ListPeers_response(knownPeers);
                                    break;

                                case "CONNECT_PEER_REQUEST":
                                    HostPort peerHostportToConnect = new HostPort(res.getString("host"),
                                            (int) (long) res.get("port"));
                                    boolean connectStatus = false;

                                    peerToPeer = new Socket(peerHostportToConnect.host, peerHostportToConnect.port);
                                    TCPHandshake tcpHandshake = new TCPHandshake(peerToPeer);
                                    try {
                                        Document operation = tcpHandshake.Handshake_request();
                                        System.out.println("Received from " + peerToPeer.getInetAddress() + ":" + peerToPeer.getPort() + " : "
                                                + operation.getString("command"));
                                        switch (operation.getString("command")) {
                                            case "HANDSHAKE_RESPONSE":
                                                TCPPeer.addConnectedPeers(peerToPeer);
                                                TCPPeer.addconnectedPeerName(
                                                        new HostPort(
                                                                peerToPeer.getInetAddress().toString().substring(1),
                                                                peerToPeer.getPort()));
//                                                System.out.println("Connection closed: " + peerToPeer.isClosed());
                                                new TCPThreadPeer(peerToPeer, fileSystemManager).start();
                                                connectStatus = true;
                                                break;

                                            case "CONNECTION_REFUSED":
                                                ArrayList<Document> peers = new ArrayList();
                                                peers = (ArrayList<Document>) operation.get("peers");
                                                for (Document peer : peers) {
                                                    HostPort pe = new HostPort(peer);
                                                    if (!TCPPeer.getKnownPeers().contains(pe)) {
                                                        TCPPeer.addKnownPeers(pe);
                                                    }
                                                }
                                                break;

                                            default:
                                                new TCPInvalidProtocol(peerToPeer).InvalidMessage("the command is invalid");
                                                System.out.println("Send Invalid Protocol to peer " + peerToPeer.getInetAddress().toString()
                                                        + ":" + peerToPeer.getPort());
                                                break;
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    ConnectPeer connectPeer = new ConnectPeer(serveClient, sessionKey, peerHostportToConnect);
                                    connectPeer.ConnectPeer_response(connectStatus);

                                    break;

                                case "DISCONNECT_PEER_REQUEST":
                                    HostPort peerHostportToDisconnect = new HostPort(res.getString("host"),
                                            (int) (long) res.get("port"));
                                    boolean disconnectStatus = false;

//                                    if (peerToPeer != null) {
                                    peerToPeer.close();
                                    disconnectStatus = true;
//                                    }

                                    DisconnectPeer disconnectPeer = new DisconnectPeer(serveClient, sessionKey, peerHostportToDisconnect);
                                    disconnectPeer.DisconnectPeer_response(disconnectStatus);

                                    break;

                                default:
                                    System.out.println("invalid command");
                                    break;
                            }
                        }
//                    }
                    }
                } catch (SocketException e) {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
