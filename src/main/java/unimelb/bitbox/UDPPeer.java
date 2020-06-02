package unimelb.bitbox;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

/**
 * Starts a ServerMain thread to monitor the local event, a ThreadListen to
 * listen incoming connection and a ThreadSyn to create periodic synchronise
 * information. Then do a BFS to try to connect maximum numbers of peers.
 *
 */
public class UDPPeer {

    /**
     *
     * @return Sockets of current connected peers.
     */
    public static ArrayList<DatagramSocket> getConnectedPeers() {
        return connectedPeers;
    }

    /**
     * Add the socket of a new connected peer to the list.
     *
     * @param socket Socket of a new connected peer.
     */
    public static void addConnectedPeers(DatagramSocket socket) {
        connectedPeers.add(socket);
    }

    /**
     * Delete a connected peer from the list.
     *
     * @param socket Socket of disconnected peer.
     */
    public static void delConnectedPeerName(HostPort e) {
        connectedPeerName.remove(e);
    }

    /**
     * Add the host and port of a new peer.
     *
     * @param e Host and port.
     */
    public static void addKnownPeers(HostPort e) {
        knownPeers.add(e);
    }

    /**
     *
     * @return A list of hosts and ports for known peers.
     */
    public static ArrayList<HostPort> getKnownPeers() {
        return knownPeers;
    }

    /**
     * Add the host and listening port of a new connected peer to the list.
     *
     * @param e The host and listening port of the new connected peer.
     */
    public static void addconnectedPeerName(HostPort e) {
        connectedPeerName.add(e);
    }

    /**
     *
     * @return The list of hosts and listening ports of connected peers.
     */
    public static ArrayList<HostPort> getconnectedPeerName() {
        return connectedPeerName;
    }

    /**
     *
     * @return The list of detected local events.
     */
    public static ArrayList<FileSystemEvent> getEvent() {
        return Event;
    }

    /**
     * Add an event to local event list.
     *
     * @param e An local event monitored by FileManager.
     */
    public static void addEvent(FileSystemEvent e) {
        Event.add(e);
    }

    /**
     *
     * @return The list of synchronized events.
     */
    public static ArrayList<FileSystemEvent> getsyn_Event() {
        return syn_Event;
    }

    /**
     * Add a synchronized event to the list of synchronized events.
     *
     * @param e Periodic generated synchronized events.
     */
    public static void addsyn_Event(FileSystemEvent e) {
        syn_Event.add(e);
    }

    /**
     * The list of hosts and listening ports of connected peers.
     */
    private static ArrayList<HostPort> connectedPeerName = new ArrayList();

    /**
     * A list of hosts and ports for known peers.
     */
    private static ArrayList<HostPort> knownPeers = new ArrayList();
    /**
     * Sockets of current connected peers.
     */
    private static ArrayList<DatagramSocket> connectedPeers = new ArrayList();
    /**
     * The list of detected local events.
     */
    private static ArrayList<FileSystemEvent> Event = new ArrayList();
    /**
     * The list of synchronized events.
     */
    private static ArrayList<FileSystemEvent> syn_Event = new ArrayList();
    private static final int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
    private static Logger log = Logger.getLogger(UDPPeer.class.getName());

    public static void main() throws IOException, NumberFormatException, NoSuchAlgorithmException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        ServerMain serverMain = new ServerMain();
        ArrayBlockingQueue clientMes = new ArrayBlockingQueue(1000);
        DatagramSocket socket = new DatagramSocket(port);
        ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);
        new UDPThreadServeClient(socket, serverMain.fileSystemManager,
                clientMes,
                peerMes).start();
        new UDPThreadListen(socket, serverMain.fileSystemManager, clientMes, peerMes).start();
        new ThreadSyn(serverMain.fileSystemManager).start();
        String[] peers = Configuration.getConfigurationValue("peers").split(",");
        try {
            for (String peer : peers) {
                knownPeers.add(new HostPort(peer));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Peer list is empty!");
        }
        
        for (HostPort hp:connectedPeerName){
        System.out.println(hp);
        }
        ArrayList<UDPThreadClient> client = new ArrayList<>();
        for (int i = 0; i < knownPeers.size(); i++) {
//            try {
            new UDPThreadClient(socket, serverMain.fileSystemManager,
                    knownPeers.get(i).host,
                    knownPeers.get(i).port, clientMes,
                    peerMes).start();

//                try {
//                    client.get(i).join();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            } catch (ConnectException e) {
//                System.out.println("Peer is offline!");
//            }
            System.out.println("Known Peers List:");
            knownPeers.forEach((k) -> {
                System.out.println(k.toString());
            });
            System.out.println("Connected Peers List:");
            connectedPeerName.forEach((action) -> {
                System.out.println(action.toString());
            });

        }
    }

}
