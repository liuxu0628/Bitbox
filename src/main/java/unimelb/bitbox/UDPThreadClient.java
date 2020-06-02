package unimelb.bitbox;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

/**
 * A thread to send handshake request and process response from peers. And
 * create a ThreadPeer to handle the file operations when the connection is
 * built.
 */
public class UDPThreadClient extends Thread {

    /**
     * socket The socket passed from Peer.
     */
    private DatagramSocket socket;
    private InetAddress host;
    private int port;
    private FileSystemManager fileSystemManager;
    private ArrayBlockingQueue clientMes = new ArrayBlockingQueue(1000);
    private ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);

    /**
     * Constructor for ThreadListen.
     *
     * @param socket The socket passed from Peer.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public UDPThreadClient(DatagramSocket socket,
            FileSystemManager fileSystemManager, String host, int port, ArrayBlockingQueue clientMes,
            ArrayBlockingQueue peerMes) {
        this.socket = socket;
        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
        }
        this.port = port;
        this.fileSystemManager = fileSystemManager;
        this.clientMes = clientMes;
        this.peerMes = peerMes;
    }

    @Override
    public void run() {
        // System.out.println("Thread Built");
        UDPHandshake hand = new UDPHandshake(socket, host, port);
        try {
            Document req = hand.Handshake_request();
            int length = req.toJson().getBytes(StandardCharsets.UTF_8).length;
            byte[] writeBytes = new byte[length];
            writeBytes = req.toJson().getBytes(StandardCharsets.UTF_8);
//            writeBytes = Base64.getEncoder().encode(writeBytes);
            DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
            socket.send(sendPacket);

            int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
//            DatagramPacket receivePacket = new DatagramPacket(new byte[blockSize], blockSize, host, port);
//            socket.receive(receivePacket);
            boolean notReceived = true;
            while (notReceived) {
                DatagramPacket receivePacket = (DatagramPacket) clientMes.peek();
                if (receivePacket != null) {
                    if (receivePacket.getAddress().getHostAddress().equals(
                            host.getHostAddress())
                            && receivePacket.getPort() == port) {
                        notReceived = false;
                        clientMes.remove();
//                        System.out.println("clientMe:  " + clientMes.size());
                        byte[] readBytes = new byte[receivePacket.getLength()];
                        System.arraycopy(receivePacket.getData(), receivePacket.getOffset(),
                                readBytes, 0, receivePacket.getLength());
//            readBytes = Base64.getDecoder().decode(readBytes);
                        String data = new String(readBytes, "UTF-8");
                        Document res = Document.parse(data);
                        host = receivePacket.getAddress();
                        port = receivePacket.getPort();

                        System.out.println("Received from " + host + ":" + port + " : " + res.getString("command"));
                        switch (res.getString("command")) {
                            case "HANDSHAKE_RESPONSE":
                                UDPPeer.addConnectedPeers(socket);
                                UDPPeer.addconnectedPeerName(
                                        new HostPort(
                                                receivePacket.getAddress()
                                                        .getHostAddress(),
                                                receivePacket.getPort()));
//                                System.out.println("Connection closed: " + socket.isClosed());
                                new UDPThreadPeer(socket, fileSystemManager, host, port,peerMes).start();
                                break;

                            case "CONNECTION_REFUSED":
                                ArrayList<Document> peers = new ArrayList();
                                peers = (ArrayList<Document>) res.get("peers");
                                for (Document peer : peers) {
                                    HostPort pe = new HostPort(peer);
                                    if (!UDPPeer.getKnownPeers().contains(pe)) {
                                        UDPPeer.addKnownPeers(pe);
                                    }
                                }
                                break;

//                default:
//                    new InvalidProtocol(socket).InvalidMessage("the command is invalid");
//                    System.out.println("Invalid Protocol to peer " + socket.getInetAddress().toString()
//                            + ":" + socket.getPort());
//                    break;
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
