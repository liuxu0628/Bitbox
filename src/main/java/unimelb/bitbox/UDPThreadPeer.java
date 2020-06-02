/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unimelb.bitbox;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

/**
 * A thread repeats monitoring incoming messages from the peer. Sort the message
 * according to its command and pass it to the specific file operation methods
 * to perform the file operations. And send file operation requests to the peer
 * according to the local event list and synchronized event list.
 */
public class UDPThreadPeer extends Thread {

    private FileSystemManager fileSystemManager;

    /**
     * The socket passed from ThreadServer or ThreadClient.
     */
    private DatagramSocket socket;
    private InetAddress host;
    private int port;
    private ArrayBlockingQueue response = new ArrayBlockingQueue(1000);
    private ArrayBlockingQueue peerMes = new ArrayBlockingQueue(1000);

    /**
     * To read the input of socket.
     */
    /**
     * Constructor for ThreadListen.
     *
     * @param socket The socket passed from ThreadServer or ThreadClient.
     * @param fileSystemManager fileSystemManager to conduct file operations.
     */
    public UDPThreadPeer(DatagramSocket socket,
            FileSystemManager fileSystemManager, InetAddress host, int port,
            ArrayBlockingQueue peerMes) {
        this.socket = socket;
        this.fileSystemManager = fileSystemManager;
        this.host = host;
        this.port = port;
        this.peerMes = peerMes;
    }

    @Override

    public void run() {
        System.out.println("Peer start");

        try {
            // Create ArrayLists for recording the commands sent.

            ArrayList<String> DIRECTORY_CREATE = new ArrayList<>();
            ArrayList<String> DIRECTORY_DELETE = new ArrayList<>();
            ArrayList<String> FILE_DELETE = new ArrayList<>();
            ArrayList<String> FILE_CREATE = new ArrayList<>();
            ArrayList<String> FILE_MODIFY = new ArrayList<>();
            ArrayList<String> FILE_BYTE = new ArrayList<>();
            ArrayList<String> FILE_BYTETO = new ArrayList<>();
//            int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
            // The JSON Parser
            Document doc = new Document();
            // Input stream
            // Client messages
            String clientMessage;
            int count = 0;
            int syn = 0;
//            try {
//                // Sort and process the incoming messages from peers
//                socket.setSoTimeout(100);
//            } catch (SocketException ex) {
//            }
            while (!socket.isClosed()
                    && UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
//                try {
//                System.out.println("Still Alive");
                try {
//                int Maximum_UDP = 65535;
//                DatagramPacket dp = new DatagramPacket(
//                        new byte[Maximum_UDP], Maximum_UDP);
//                socket.receive(dp);
                    DatagramPacket dp = (DatagramPacket) peerMes.peek();
                    if (dp != null) {
                        if (dp.getAddress().getHostAddress().equals(
                                host.getHostAddress())
                                && dp.getPort() == port) {
                            peerMes.remove();
                            byte[] readBytes = new byte[dp.getLength()];
                            System.arraycopy(dp.getData(), dp.getOffset(),
                                    readBytes, 0, dp.getLength());

//            for (byte b : readBytes) {
//
//                System.out.println(b);
//            }
//                    readBytes = Base64.getDecoder().decode(readBytes);
                            clientMessage = new String(readBytes, "UTF-8");
                            // Parse the request
                            Document request = Document.parse(clientMessage);
                            System.out.println(request.toJson());
                            //System.out.println(request.toJson());

                            String command = request.getString("command");
                            System.out.println("Received from " + host
                                    + ":" + port + " : " + command);
                            Document filedes;
                            String md5 = "";
                            if (command.substring(0, 4).equals("FILE")) {
                                filedes = (Document) request.get("fileDescriptor");
                                md5 = filedes.getString("md5");
                            }
                            int length = command.length();
                            try {
                                if (command.substring(length - 8).equals("RESPONSE")) {
                                    String operation = command.substring(0, length - 9);
                                    if (command.substring(0, 4).equals("FILE")) {
                                        Document filedesc = (Document) request.get("fileDescriptor");
                                        String fileChar = filedesc.getString("md5");
                                        if (command.substring(5, length - 9).equals("BYTES")) {
                                            long position = request.getLong("position");
                                            response.add(new Operation(operation, fileChar, position));
                                        } else {
                                            response.add(new Operation(operation, fileChar));
                                        }
                                    } else if (command.substring(0, 9).equals("DIRECTORY")) {
                                        String fileChar = request.getString("pathName");
                                        response.add(new Operation(operation, fileChar));
                                    }
//                                    System.out.println(response.size());
                                }
                            } catch (IllegalStateException e) {
                                response.clear();
                                e.printStackTrace();
                            }
                            // Classify the request
                            switch (command) {
                                case "FILE_DELETE_RESPONSE":
                                    if (FILE_DELETE.contains(md5)) {
                                        FILE_DELETE.remove(md5);
                                    }
                                    break;

                                case "FILE_DELETE_REQUEST":
                                    Document fileDelRes
                                            = new UDPFileDelete().FileDelete_response(request, fileSystemManager);
                                    sendMessage(fileDelRes);
                                    System.out.println("Send FILE_DELETE_RESPONSE to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;

                                case "DIRECTORY_CREATE_RESPONSE":
                                    String dcre = request.getString("pathName");
                                    if (DIRECTORY_CREATE.contains(dcre)) {
                                        DIRECTORY_CREATE.remove(dcre);
                                    }
                                    break;

                                case "DIRECTORY_CREATE_REQUEST":
                                    Document dirCreRes
                                            = new UDPDirectoryCreate().DirectoryCreate_response(request, fileSystemManager);
                                    System.out.println("Send DIRECTORY_CREATE_RESPONSE to : " + host
                                            + ":" + port);
                                    sendMessage(dirCreRes);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;

                                case "DIRECTORY_DELETE_RESPONSE":
                                    String ddel = request.getString("pathName");
                                    if (DIRECTORY_DELETE.contains(ddel)) {
                                        DIRECTORY_DELETE.remove(ddel);
                                    }
                                    break;

                                case "DIRECTORY_DELETE_REQUEST":
                                    Document dirDelRes
                                            = new UDPDirectoryDelete().DirectoryDelete_response(request, fileSystemManager);
                                    System.out.println("Send DIRECTORY_DELETE_RESPONSE to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    sendMessage(dirDelRes);
                                    break;

                                case "FILE_CREATE_RESPONSE":
//                            for (Document d:FILE_CREATE){
//
//                                System.out.println(d.toJson());
//                            }
                                    if (FILE_CREATE.contains(md5)) {
                                        FILE_CREATE.remove(md5);
                                        FILE_BYTE.add(md5);
                                    }

                                    break;

                                case "FILE_CREATE_REQUEST":
                                    try {
                                        Document fileCreRes = new UDPFileCreate().FileCreate_response(request, fileSystemManager);
                                        if (fileCreRes.getBoolean("status")) {
                                            FILE_BYTETO.add(md5);
                                        }
                                        sendMessage(fileCreRes);
                                        if (fileCreRes.getBoolean("status")) {
                                            FILE_BYTETO.add(md5);
                                            sendMessage(new UDPFileBytes().FileBytes_request(fileSystemManager, request));
                                        }

                                        System.out.println("Send FILE_CREATE_RESPONSE to : " + host
                                                + ":" + port);
                                        // System.out.println("Connection closed: " + socket.isClosed());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case "FILE_MODIFY_RESPONSE":
                                    if (FILE_MODIFY.contains(md5)) {
                                        FILE_MODIFY.remove(md5);
                                        FILE_BYTE.add(md5);
                                    }
                                    break;

                                case "FILE_MODIFY_REQUEST":
                                    Document fileModRes
                                            = new UDPFileModify().FileModify_response(request, fileSystemManager);

                                    sendMessage(fileModRes);
                                    if (fileModRes.getBoolean("status")) {
                                        FILE_BYTETO.add(md5);
                                        sendMessage(new UDPFileBytes().FileBytes_request(fileSystemManager, request));
                                    }

                                    System.out.println("Send FILE_MODIFY_RESPONSE to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;

                                case "FILE_BYTES_REQUEST":
                                    if (FILE_BYTE.contains(md5)) {
                                        Document fileBytRes
                                                = new UDPFileBytes().FileBytes_response(request, fileSystemManager);
                                        sendMessage(fileBytRes);
                                    }
                                    break;

                                case "FILE_BYTES_RESPONSE":
                                    if (FILE_BYTETO.contains(md5)) {
                                        Document fileBytReq
                                                = new UDPFileBytes().FileBytes_request_continous(request, fileSystemManager);
                                        System.out.println(fileBytReq.toJson());
//                                        System.out.println();
                                        try {
                                            if (fileBytReq.getString("command").equals("fail")) {
                                                FILE_BYTETO.remove(md5);
                                            } else {
                                                sendMessage(fileBytReq);
                                            }
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    break;

                                case "HANDSHAKE_REQUEST":
                                    sendMessage(new UDPInvalidProtocol().InvalidMessage("Connection already built."));
//                                    socket.close();
                                    System.out.println("Send INVALID_PROTOCOL to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;

                                case "HANDSHAKE_RESPONSE":
                                    sendMessage(new UDPInvalidProtocol().InvalidMessage("Connection already built."));
//                                    socket.close();
                                    System.out.println("Send INVALID_PROTOCOL to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;
                                    
                                case "INVALID_PROTOCOL":
                                    break;
                                default:
                                    sendMessage(new UDPInvalidProtocol().InvalidMessage("the command is invalid"));
//                                    socket.close();
                                    System.out.println("Send INVALID_PROTOCOL to : " + host
                                            + ":" + port);
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    break;
                            }
                        }
                    }

//                } catch (SocketTimeoutException e) {
//
//                }
                    // Send requests for local events and record them.
                    if (UDPPeer.getEvent().size() > count) {
                        FileSystemEvent eve = UDPPeer.getEvent().get(count);
                        try {
                            switch (eve.event) {
                                case FILE_CREATE:
                                    UDPFileCreate file_Cre = new UDPFileCreate();
                                    FILE_CREATE.add(eve.fileDescriptor.md5);
                                     {
                                        try {
                                            Document fileCreReq
                                                    = file_Cre.FileCreate_request(eve);
                                            sendMessage(fileCreReq);
                                            System.out.println("Send FILE_CREATE_REQUEST to : " + host
                                                    + ":" + port);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                case FILE_DELETE:
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    UDPFileDelete file_Del = new UDPFileDelete();
                                    FILE_DELETE.add(eve.fileDescriptor.md5);
                                     {
                                        try {
                                            Document fileDelReq
                                                    = file_Del.FileDelete_request(eve);
                                            sendMessage(fileDelReq);
                                            System.out.println("Send FILE_DELETE_REQUEST to : " + host
                                                    + ":" + port);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                case FILE_MODIFY:
                                    // System.out.println("Connection closed: " + socket.isClosed());
                                    UDPFileModify file_Mod = new UDPFileModify();
                                    FILE_MODIFY.add(eve.fileDescriptor.md5);
                                     {
                                        try {
                                            Document fileModReq
                                                    = file_Mod.FileModify_request(eve);
                                            sendMessage(fileModReq);
                                            System.out.println("Send FILE_MODIFY_REQUEST to : " + host
                                                    + ":" + port);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                case DIRECTORY_CREATE:
                                    UDPDirectoryCreate dir_Cre = new UDPDirectoryCreate();
                                    DIRECTORY_CREATE.add(eve.pathName);
                                     {
                                        try {
                                            Document DirCreReq
                                                    = dir_Cre.DirectoryCreate_request(eve);
                                            sendMessage(DirCreReq);
                                            System.out.println("Send DIRECTORY_CREATE_REQUEST to : " + host
                                                    + ":" + port);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                case DIRECTORY_DELETE:
                                    UDPDirectoryDelete dir_Del = new UDPDirectoryDelete();
                                    DIRECTORY_DELETE.add(eve.pathName);
                                    try {
                                        Document DirDelReq
                                                = dir_Del.DirectoryDelete_request(eve);
                                        sendMessage(DirDelReq);
                                        System.out.println("Send DIRECTORY_DELETE_REQUEST to : " + host
                                                + ":" + port);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                default:
                                    break;
                            }
                        } catch (NullPointerException E) {
                            E.printStackTrace();
                        }
                        count++;
                    }

                    // Send request for synchronized events and record them.
                    if (UDPPeer.getsyn_Event().size() > syn) {
                        System.out.println("Syn detected");
                        System.out.println(syn);
                        // System.out.println(Peer.getsyn_Event());
//                        System.out.println();
                        FileSystemEvent eve = UDPPeer.getsyn_Event().get(syn);
                        if (!eve.event.toString().equals("DIRECTORY_CREATE")) {
                            UDPFileCreate file_Cre = new UDPFileCreate();
                            UDPFileModify file_Mod = new UDPFileModify();

                            try {
                                sendMessage(file_Cre.FileCreate_request(eve));
                                System.out.println("Send FILE_CREATE_REQUEST to : " + host
                                        + ":" + port);
                                FILE_CREATE.add(eve.fileDescriptor.md5);
                                sendMessage(file_Mod.FileModify_request(eve));
                                System.out.println("Send FILE_MODIFY_REQUEST to : " + host
                                        + ":" + port);
                                FILE_MODIFY.add(eve.fileDescriptor.md5);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            UDPDirectoryCreate dir_Dir = new UDPDirectoryCreate();
                            sendMessage((dir_Dir.DirectoryCreate_request(eve)));
                            System.out.println("Send DIRECTORY_CREATE_REQUEST to : " + host
                                    + ":" + port);
                            DIRECTORY_CREATE.add(eve.pathName);
                        }
                        syn++;
                    }
                } catch (SocketException e) {
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Invalid Input/Output, connection stop!");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("peer end");
            if (UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
                UDPPeer.delConnectedPeerName(new HostPort(host.getHostAddress(), port));
            }
//            try {
//                e.printStackTrace();
//                sendMessage(new UDPInvalidProtocol().InvalidMessage("some mandatory information is missing"));
//                socket.close();
//            } catch (IOException e1) {
//                e1.printStackTrace();
//                System.out.println("Invalid Input/Output, connection stop!");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("peer end");
            if (UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
                UDPPeer.delConnectedPeerName(new HostPort(host.getHostAddress(), port));
            }
        }
        System.out.println("peer end");
        if (UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
            UDPPeer.delConnectedPeerName(new HostPort(host.getHostAddress(), port));
        }
    }

    private void sendMessage(Document msg) {
        byte[] writeBytes = msg.toJson().getBytes(StandardCharsets.UTF_8);
//        System.out.println(writeBytes.length);
//        writeBytes = Base64.getEncoder().encode(writeBytes);
        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
        try {
            socket.send(sendPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String command = msg.getString("command");
        int length = command.length();
        if (command.substring(length - 7).equals("REQUEST")) {
            String operation = command.substring(0, length - 8);
            Operation op = null;
            if (command.substring(0, 4).equals("FILE")) {
                Document filedes = (Document) msg.get("fileDescriptor");
                String fileChar = filedes.getString("md5");

                if (command.substring(5, length - 8).equals("BYTES")) {
                    long position = msg.getLong("position");
                    op = new Operation(operation, fileChar, position);
                } else {
                    op = new Operation(operation, fileChar);
                }
            } else if (command.substring(0, 9).equals("DIRECTORY")) {
                String fileChar = msg.getString("pathName");
                op = new Operation(operation, fileChar);
            }
//            System.out.println("ADD!");
            new UDPThreadRetry(response, socket, host, port, op, msg).start();
        }
        System.out.println("Sent    " + msg.toJson());
    }
}
