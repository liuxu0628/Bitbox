package unimelb.bitbox;

import unimelb.bitbox.util.*;
import javax.net.ServerSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import org.json.simple.parser.ParseException;

public class UDPThreadServeClient extends Thread {

    private static int serveClientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
    private static String[] authorizedKeys = Configuration.getConfigurationValue("authorized_keys").split(",");
    private static String sessionKey = "";
    private DatagramSocket socket;
    private FileSystemManager fileSystemManager;
    private ArrayBlockingQueue clientMes = new ArrayBlockingQueue(1000);
    private ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);

    public UDPThreadServeClient(DatagramSocket socket,
            FileSystemManager fileSystemManager, ArrayBlockingQueue clientMes,
            ArrayBlockingQueue peerMes) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
        this.clientMes = clientMes;
        this.peerMes = peerMes;
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
                            System.out.println("Received from " + serveClient.getInetAddress().toString()
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

                                    InetAddress host = InetAddress.getByName(peerHostportToConnect.host);
                                    int port = peerHostportToConnect.port;
                                    UDPHandshake udpHandshake = new UDPHandshake(socket, host, port);
//                                    socket.setSoTimeout(30000);
                                    try {
                                        Document req = udpHandshake.Handshake_request();
                                        int length = req.toJson().getBytes(StandardCharsets.UTF_8).length;
                                        byte[] writeBytes = new byte[length];
                                        writeBytes = req.toJson().getBytes(StandardCharsets.UTF_8);
//                                      writeBytes = Base64.getEncoder().encode(writeBytes);
                                        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
                                        socket.send(sendPacket);

                                        int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
//                                      DatagramPacket receivePacket = new DatagramPacket(new byte[blockSize], blockSize, host, port);
//                                      socket.receive(receivePacket);
                                        boolean notReceived = true;
                                        while (notReceived) {
                                            DatagramPacket receivePacket = (DatagramPacket) clientMes.peek();
                                            if (receivePacket != null) {
                                                if (receivePacket.getAddress().getHostAddress().equals(host.getHostAddress())
                                                        && receivePacket.getPort() == port) {
                                                    notReceived = false;
                                                    clientMes.remove();
//                                                    System.out.println("clientMe:  " + clientMes.size());
                                                    byte[] readBytes = new byte[receivePacket.getLength()];
                                                    System.arraycopy(receivePacket.getData(), receivePacket.getOffset(),
                                                            readBytes, 0, receivePacket.getLength());
//                                                  readBytes = Base64.getDecoder().decode(readBytes);
                                                    String data = new String(readBytes, "UTF-8");
                                                    Document response = Document.parse(data);
                                                    host = receivePacket.getAddress();
                                                    port = receivePacket.getPort();

                                                    System.out.println("Received from " + " : " + response.getString("command"));
                                                    switch (response.getString("command")) {
                                                        case "HANDSHAKE_RESPONSE":
                                                            UDPPeer.addconnectedPeerName(
                                                                    new HostPort(receivePacket.getAddress().getHostAddress(),
                                                                            receivePacket.getPort()));
//                                                            System.out.println("Connection closed: " + socket.isClosed());
                                                            new UDPThreadPeer(socket, fileSystemManager, host, port, peerMes).start();
                                                            connectStatus = true;
                                                            break;

                                                        case "CONNECTION_REFUSED":
                                                            ArrayList<Document> peers = new ArrayList();
                                                            peers = (ArrayList<Document>) response.get("peers");
                                                            for (Document peer : peers) {
                                                                HostPort pe = new HostPort(peer);
                                                                if (!UDPPeer.getKnownPeers().contains(pe)) {
                                                                    UDPPeer.addKnownPeers(pe);
                                                                }
                                                            }
                                                            break;

//                                                      default:
//                                                          new InvalidProtocol(socket).InvalidMessage("the command is invalid");
//                                                          System.out.println("Invalid Protocol to peer " + socket.getInetAddress().toString()
//                                                          + ":" + socket.getPort());
//                                                          break;
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    ConnectPeer connectPeer = new ConnectPeer(serveClient, sessionKey, peerHostportToConnect);
                                    connectPeer.ConnectPeer_response(connectStatus);

                                    break;

                                case "DISCONNECT_PEER_REQUEST":
                                    HostPort peerHostportToDisconnect = new HostPort(res.getString("host"),
                                            (int) (long) res.get("port"));
                                    boolean disconnectStatus = false;

                                    if (UDPPeer.getconnectedPeerName().contains(peerHostportToDisconnect)) {
                                        UDPPeer.delConnectedPeerName(peerHostportToDisconnect);
                                        disconnectStatus = true;
                                    }

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
