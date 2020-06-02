package unimelb.bitbox;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

/**
 * A thread to send handshake request and process response from peers.
 * And create a ThreadPeer to handle the file operations when the connection is built.
 */
public class TCPThreadClient extends Thread {

    /**
     * socket The socket passed from Peer.
     */
    private Socket socket;

    private FileSystemManager fileSystemManager;

    /**
     * Constructor for ThreadListen.
     * @param socket The socket passed from Peer.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public TCPThreadClient(Socket socket, FileSystemManager fileSystemManager) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
    }

    @Override
    public synchronized void run() {
        // System.out.println("Thread Built");
        TCPHandshake hand = new TCPHandshake(socket);
        try {
            Document operation = hand.Handshake_request();
            System.out.println("Received from " + socket.getInetAddress() + ":" +socket.getPort()  + " : " + operation.getString("command"));
            switch (operation.getString("command")) {
                case "HANDSHAKE_RESPONSE":
                    TCPPeer.addConnectedPeers(socket);
                    TCPPeer.addconnectedPeerName(
                            new HostPort(
                                    socket.getInetAddress().toString().substring(1),
                                    socket.getPort()));
//                    System.out.println("Connection closed: " + socket.isClosed());
                    new TCPThreadPeer(socket, fileSystemManager).start();
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
                    new TCPInvalidProtocol(socket).InvalidMessage("the command is invalid");
                    System.out.println("Invalid Protocol to peer " + socket.getInetAddress().toString()
                            + ":" + socket.getPort());
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
