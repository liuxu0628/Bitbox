package unimelb.bitbox;

import java.io.*;
import java.util.Base64;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.*;

public class UDPHandshake {

    private DatagramSocket socket;
    private InetAddress host;
    private int port;

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public UDPHandshake(DatagramSocket socket, InetAddress host, int port) {
        this.setSocket(socket);
        this.setHost(host);
        this.setPort(port);
    }

    public Document Handshake_request() throws ParseException, IOException {
        Document req = new Document();

        String local_host = Configuration.getConfigurationValue("advertisedName");
        int local_port = Integer.parseInt(Configuration.getConfigurationValue("port"));
//        int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        req.append("command", "HANDSHAKE_REQUEST");

        Document local_hostport = new Document();
        local_hostport.append("host", local_host);
        local_hostport.append("port", local_port);
        req.append("hostPort", local_hostport);
        return req;
//        int length = req.toJson().getBytes().length;
//        byte[] writeBytes = new byte[length];
//        writeBytes = req.toJson().getBytes();
//        writeBytes = Base64.getEncoder().encode(writeBytes);
//        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
//        socket.send(sendPacket);

//        DatagramPacket receivePacket = new DatagramPacket(new byte[blockSize], blockSize, host, port);
//        socket.receive(receivePacket);
//        byte[] readBytes = new byte[receivePacket.getLength()];
//        System.arraycopy(receivePacket.getData(), receivePacket.getOffset(),
//                readBytes, 0, receivePacket.getLength());
//        readBytes = Base64.getDecoder().decode(readBytes);
//        String data = new String(readBytes);
////        Document res = Document.parse(data);
//        return res;

    }

    public void Handshake_response() throws ParseException, IOException {

        Document res = new Document();
        String local_host = Configuration.getConfigurationValue("advertisedName");
        int local_port = Integer.parseInt(Configuration.getConfigurationValue("port"));

        res.append("command", "HANDSHAKE_RESPONSE");
        Document local_hostport = new Document();
        local_hostport.append("host", local_host);
        local_hostport.append("port", local_port);
        res.append("hostPort", local_hostport);

        int length = res.toJson().getBytes("UTF-8").length;
        byte[] writeBytes = new byte[length];
        writeBytes = res.toJson().getBytes("UTF-8");
//        writeBytes = Base64.getEncoder().encode(writeBytes);
        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
        socket.send(sendPacket);

    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
