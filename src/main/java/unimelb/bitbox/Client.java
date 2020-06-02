package unimelb.bitbox;

import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.*;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Client {

    private static String privateKeyFile = "bitboxclient_rsa";

    public static void main(String[] args) {
        // Initialise session key
        String sessionKey = "";
        // Initialise identity
        String identity = "";
        // Get private key
        PrivateKey privateKey = null;
        try {
            privateKey = new RSA().getPrivate(privateKeyFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        // Command line parser
        CommandLineArgs argsBean = new CommandLineArgs();
        CmdLineParser parser = new CmdLineParser(argsBean);

//        while (true) {
        try {
            //Parse command line arguments
            parser.parseArgument(args);
            HostPort serverHostport = argsBean.getServer();

            // Initialise client-server socket
            Socket clientServer = new Socket(serverHostport.host, serverHostport.port);

            // Input stream
            BufferedReader serverInput = new BufferedReader(new InputStreamReader(clientServer.getInputStream(), "UTF8"));

//                while(!clientServer.isClosed()){
            // Authorization
            if (sessionKey.equals("")) {
                identity = argsBean.getIdentity();
                Authorization authorization = new Authorization(clientServer);
                authorization.Authorization_request(identity);

//                        if (serverInput.ready()) {
                // Server message
                String serverMessage = serverInput.readLine();
                // Parse the response
                Document serverResponse = Document.parse(serverMessage);

                // Receive "AUTH_RESPONSE"
                if (serverResponse.containsKey("command")
                        && serverResponse.getString("command").equals("AUTH_RESPONSE")) {
                    System.out.println("Received from " + clientServer.getInetAddress().toString()
                            + ":" + clientServer.getPort() + " : " + serverResponse.getString("command"));

                    if (serverResponse.getBoolean("status")) {
                        String sessionkey = serverResponse.getString("AES128");
                        sessionkey = new String(Base64.getDecoder().decode(sessionkey));
                        if (privateKey != null) {
                            sessionKey = new RSA().decrypt(sessionkey, privateKey);
                            System.out.println(serverResponse.getString("message"));
//                                        break;
                        } else {
                            System.out.println("private key not read");
//                                        break;
                        }
                    } else {
                        System.out.println(serverResponse.getString("message"));
//                                    break;
                    }
                }
//                        }
            }

            // Get command from command line
            String command = argsBean.getCommand();
            switch (command) {
                case "list_peers":
                    ListPeers listPeers = new ListPeers(clientServer, sessionKey);
                    listPeers.ListPeers_request();

//                    if (serverInput.ready()) {
                        // Server message
                        String serverMessage = serverInput.readLine();
                        // Parse the response
                        Document serverResponse = Document.parse(serverMessage);

                        // Receive "LIST_PEERS_RESPONSE"
                        if (serverResponse.containsKey("payload")) {
                            String content = serverResponse.getString("payload");
                            content = new String(Base64.getDecoder().decode(content));
                            content = AES.Session_decrypt(content, AES.getKey(sessionKey));
                            Document res = Document.parse(content);
                            if (res.getString("command").equals("LIST_PEERS_RESPONSE")) {
                                System.out.println("Received from " + clientServer.getInetAddress().toString()
                                        + ":" + clientServer.getPort() + " : " + res.getString("command"));

                                ArrayList<Document> peerList = (ArrayList<Document>) res.get("peers");
                                for (Document peer:peerList){
	                                    System.out.println(peer.toJson());
	                            }
                            }
                        }
//                    }

                    break;

                case "connect_peer":
                    HostPort peerHostportToConnect = argsBean.getPeer();

                    ConnectPeer connectPeer = new ConnectPeer(clientServer, sessionKey, peerHostportToConnect);
                    connectPeer.ConnectPeer_request();

//                    if (serverInput.ready()) {
                        // Server message
                        serverMessage = serverInput.readLine();
                        // Parse the response
                        serverResponse = Document.parse(serverMessage);

                        // Receive "CONNECT_PEER_RESPONSE"
                        if (serverResponse.containsKey("payload")) {
                            String content = serverResponse.getString("payload");
                            content = new String(Base64.getDecoder().decode(content));
                            content = AES.Session_decrypt(content, AES.getKey(sessionKey));
                            Document res = Document.parse(content);
                            if (res.getString("command").equals("CONNECT_PEER_RESPONSE")) {
                                System.out.println("Received from " + clientServer.getInetAddress().toString()
                                        + ":" + clientServer.getPort() + " : " + res.getString("command"));
                                System.out.println(res.getString("message"));
                            }
                        }
//                    }

                    break;

                case "disconnect_peer":
                    HostPort peerHostportToDisconnect = argsBean.getPeer();

                    DisconnectPeer disconnectPeer = new DisconnectPeer(clientServer, sessionKey, peerHostportToDisconnect);
                    disconnectPeer.DisconnectPeer_request();

//                    if (serverInput.ready()) {
                        // Server message
                        serverMessage = serverInput.readLine();
                        // Parse the response
                        serverResponse = Document.parse(serverMessage);

                        // Receive "DISCONNECT_PEER_RESPONSE"
                        if (serverResponse.containsKey("payload")) {
                            String content = serverResponse.getString("payload");
                            content = new String(Base64.getDecoder().decode(content));
                            content = AES.Session_decrypt(content, AES.getKey(sessionKey));
                            Document res = Document.parse(content);
                            if (res.getString("command").equals("DISCONNECT_PEER_RESPONSE")) {
                                System.out.println("Received from " + clientServer.getInetAddress().toString()
                                        + ":" + clientServer.getPort() + " : " + res.getString("command"));
                                System.out.println(res.getString("message"));
                            }
                        }
//                    }

                    break;

                default:
                    System.out.println("invalid command");
                    break;
            }
//                }
        } catch (CmdLineException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
//        }
    }
}
