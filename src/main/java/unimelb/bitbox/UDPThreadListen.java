package unimelb.bitbox;

import unimelb.bitbox.util.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread keeping monitoring incoming connection request. For each request
 * create a ThreadServer to handle the message read.
 */
public class UDPThreadListen extends Thread {

    /**
     * Declare server port number.
     */
    private static final int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
    private ArrayBlockingQueue clientMes = new ArrayBlockingQueue(1000);
    private ArrayBlockingQueue serverMes = new ArrayBlockingQueue(1000);
    private ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);
    private FileSystemManager fileSystemManager;
    private DatagramSocket socket;

    /**
     * Constructor for ThreadListen.
     *
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public UDPThreadListen(DatagramSocket socket,
            FileSystemManager fileSystemManager, ArrayBlockingQueue clientMes,
            ArrayBlockingQueue peerMes) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
        this.clientMes = clientMes;
        this.peerMes = peerMes;
    }

    @Override
    public void run() {

        ArrayList<UDPThreadServer> server = new ArrayList<>();
        int i = 0;
//        try {
//            socket.setSoTimeout(3000);
//        } catch (SocketException ex) {
////            Logger.getLogger(UDPThreadListen.class.getName()).log(Level.SEVERE, null, ex);
////ex.printStackTrace();
//        }
        // Wait for the connection
        while (true) {
            try {

//                System.out.println("Still alive");
                int block = 65535;
                byte[] buf = new byte[block];
                DatagramPacket dp = new DatagramPacket(buf, block);
//                System.out.println("Before");
                try {
                    socket.receive(dp);
                } catch (SocketTimeoutException e) {

                }

//                System.out.println("After");
                byte[] readBytes = new byte[dp.getLength()];
                System.arraycopy(dp.getData(), dp.getOffset(),
                        readBytes, 0, dp.getLength());
                String clientMessage = new String(readBytes, "UTF-8");
                Document message = Document.parse(clientMessage);
                System.out.println(message.toJson());
                String command = message.getString("command");
                switch (command) {
                    case "HANDSHAKE_REQUEST":
                        new UDPThreadServer(socket, fileSystemManager, dp, peerMes).start();
                        break;
                    case "HANDSHAKE_RESPONSE":
                    case "CONNECTION_REFUSE":
                        clientMes.add(dp);
                        break;
                    case "INVALID_PROTOCOL":
                        break;
                    default:
                        if (UDPPeer.getconnectedPeerName().contains(new HostPort(dp.getAddress().getHostAddress(), dp.getPort()))) {
                            peerMes.add(dp);
                        } else {
                            System.out.println("Connected Peers:");
                            for (HostPort hp : UDPPeer.getconnectedPeerName()) {
                                System.out.println(hp);
                            }
                            byte[] writeBytes = new UDPInvalidProtocol().
                                    InvalidMessage("Connection not built.")
                                    .toJson().getBytes(StandardCharsets.UTF_8);
//        System.out.println(writeBytes.length);
//        writeBytes = Base64.getEncoder().encode(writeBytes);
                            DatagramPacket sendPacket = new DatagramPacket(
                                    writeBytes, writeBytes.length,
                                    dp.getAddress(), dp.getPort());
                            try {
                                socket.send(sendPacket);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                }

                // Receive one connection
                // Start a new thread for the connection
            } catch (IOException ex) {
                ex.printStackTrace();

            } catch (Exception e) {
//                e.printStackTrace();

            }
        }
    }
}
