package unimelb.bitbox;

import java.io.BufferedReader;
import unimelb.bitbox.util.*;
import javax.net.ServerSocketFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/**
 * A thread to handle the first message from a connected peer.
 * And create a ThreadPeer to handle the file operations when the connection is built.
 */
public class TCPThreadServer extends Thread {

    /**
     * The socket passed from ThreadListen.
     */
    private Socket socket;

    private FileSystemManager fileSystemManager;

    /**
     * Constructor for ThreadListen.
     * @param socket The socket passed from ThreadListen.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public TCPThreadServer(Socket socket, FileSystemManager fileSystemManager) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
    }

    public synchronized void run() {

        // The JSON Parser
        Document doc = new Document();
        // Input stream
        BufferedReader input;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
            String clientMessage;
            clientMessage = input.readLine();

            // Client messages
            // Parse the request
            Document request = doc.parse(clientMessage);
            String command = request.getString("command");
            System.out.println("Received from " + socket.getInetAddress() + ":" + socket.getPort() + " : " + command);
            Document hostPort = (Document) request.get("hostPort");
            // Classify the request
            switch (command) {
                case "HANDSHAKE_REQUEST":
                    if (TCPPeer.getConnectedPeers().size() >= Integer.parseInt(
                            Configuration.getConfigurationValue("maximumIncommingConnections"))) {
                        new TCPConnectionRefused(socket).connection_refused(TCPPeer.getconnectedPeerName());
                        System.out.println("Send CONNECTION_REFUSED to peer " + socket.getInetAddress().toString()
                                + ":" + socket.getPort());
//                        System.out.println("Connection closed: " + socket.isClosed());

                    } else {
                        new TCPHandshake(socket).Handshake_response();
                        System.out.println("Send HANDSHAKE_RESPONSE to peer " + socket.getInetAddress().toString()
                                + ":" + socket.getPort());
                        TCPPeer.getConnectedPeers().add(socket);
//                        System.out.println("Connection closed: " + socket.isClosed());
                        TCPPeer.addconnectedPeerName(new HostPort(hostPort));
                        new TCPThreadPeer(socket, fileSystemManager).start();
                        System.out.println(TCPPeer.getConnectedPeers());
                    }

                    break;

                default:
                    new TCPInvalidProtocol(socket).InvalidMessage("the command is invalid");
                    System.out.println("Send INVALID_PROTOCOL to peer " + socket.getInetAddress().toString()
                            + ":" + socket.getPort());
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
}
