package unimelb.bitbox;

import unimelb.bitbox.util.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import org.json.simple.parser.ParseException;

/**
 * A thread to handle the first message from a connected peer. And create a
 * ThreadPeer to handle the file operations when the connection is built.
 */
public class UDPThreadServer extends Thread {

    /**
     * The socket passed from ThreadListen.
     */
    private DatagramPacket dp;
    private FileSystemManager fileSystemManager;
    private DatagramSocket socket;
    private ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);

    /**
     * Constructor for ThreadListen.
     *
     * @param socket The socket passed from ThreadListen.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public UDPThreadServer(DatagramSocket socket, FileSystemManager fileSystemManager, DatagramPacket dp, ArrayBlockingQueue peerMes) {
        this.socket = socket;
        this.dp = dp;
        this.fileSystemManager = fileSystemManager;
        this.peerMes = peerMes;
    }

    public void run() {
        try {
            InetAddress host = dp.getAddress();
            int port = dp.getPort();
//            System.out.println(socket.getLocalPort());
            // The JSON Parser
            Document doc = new Document();
            // Input stream

            String clientMessage;
            int length = dp.getLength();
//            System.out.println("LEN:" + length);
            byte[] readBytes = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), dp.getOffset(),
                    readBytes, 0, dp.getLength());

//            for (byte b : readBytes) {
//
//                System.out.println(b);
//            }
//            readBytes = Base64.getDecoder().decode(readBytes);
            clientMessage = new String(readBytes, "UTF-8");
            // Client messages
            // Parse the request
            Document request = doc.parse(clientMessage);
//            System.out.println(request.toJson());
            String command = request.getString("command");
            System.out.println("Received from " + dp.getAddress() + ":" + dp.getPort() + " : " + command);
            Document hostPort = (Document) request.get("hostPort");
            // Classify the request
            switch (command) {
                case "HANDSHAKE_REQUEST":
                    if (UDPPeer.getConnectedPeers().size() >= Integer.parseInt(
                            Configuration.getConfigurationValue("maximumIncommingConnections"))) {
                        new UDPConnectionRefused(socket, dp.getAddress(),
                                dp.getPort()).connection_refused(UDPPeer.getconnectedPeerName());
//                        System.out.println("Send CONNECTION_REFUSED to peer " + socket.getInetAddress().toString()
//                                + ":" + socket.getPort());
//                        System.out.println("Connection closed: " + socket.isClosed());

                    } else {

                        new UDPHandshake(socket, host, port).Handshake_response();
                        System.out.println("Send HANDSHAKE_RESPONSE to peer " + dp.getAddress()
                                + ":" + dp.getPort());
                        UDPPeer.getConnectedPeers().add(socket);
//                        System.out.println("Connection closed: " + socket.isClosed());

                        new UDPThreadPeer(socket, fileSystemManager, host, port, peerMes).start();
                        System.out.println(UDPPeer.getConnectedPeers());
                        if (!UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
                            UDPPeer.addconnectedPeerName(new HostPort(
                                    dp.getAddress().getHostAddress(), dp.getPort()));
                        }
                    }
                    break;

                default:
//                    Document res
//                            = new UDPInvalidProtocol().InvalidMessage("the command is invalid");
//                    byte[] writeBytes = res.toJson().getBytes();
//                    writeBytes = Base64.getEncoder().encode(writeBytes);
//                    DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
//                    try {
//                        socket.send(sendPacket);
//                    } catch (IOException ex) {
//                    }
//                    socket.close();
//                    System.out.println("Send INVALID_PROTOCOL to peer " + dp.getAddress()
//                            + ":" + dp.getPort());
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
}
