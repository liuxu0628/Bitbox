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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

/**
 *
 * @author l2499
 */
public class UDPThreadRetry extends Thread {

    private ArrayBlockingQueue request;
    private DatagramSocket socket;
    private InetAddress host;
    private int port;
    private Operation operation;
    private Document req;

    public UDPThreadRetry(ArrayBlockingQueue request, DatagramSocket socket, InetAddress host, int port, Operation operation, Document req) {
        this.request = request;
        this.socket = socket;
        this.host = host;
        this.port = port;
        this.operation = operation;
        this.req = req;
    }

    @Override

    public void run() {
        long timeOut = Long.parseLong(
                Configuration.getConfigurationValue("udpTimeout"));
        int retryLimit = Integer.parseInt(
                Configuration.getConfigurationValue("udpRetries"));
        int retry = 0;
        boolean notReceived = true;
        long start = System.currentTimeMillis();
        while (notReceived && retry <= retryLimit && UDPPeer.getconnectedPeerName().contains(new HostPort(host.getHostAddress(), port))) {
            if (request.remove(operation)) {
                notReceived = false;
//                System.out.println("Removed!");
            } else {
                if (System.currentTimeMillis() - start >= timeOut) {
                    byte[] writeBytes = req.toJson().getBytes();
                    writeBytes = req.toJson().getBytes(StandardCharsets.UTF_8);
                    DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
                    try {
                        socket.send(sendPacket);
                        System.out.println("Retry time " + retry);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println(req.toJson());
                    retry++;
                    start = System.currentTimeMillis();
                }
            }
        }
        if (retry > retryLimit) {
        	UDPPeer.delConnectedPeerName(new HostPort(host.getHostAddress(), port));
        }
    }
}
