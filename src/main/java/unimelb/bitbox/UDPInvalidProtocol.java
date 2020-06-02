package unimelb.bitbox;


import java.io.IOException;
import java.util.Base64;
import unimelb.bitbox.util.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class UDPInvalidProtocol
{
    /*private DatagramSocket socket;
    private InetAddress host;
    private int port;

    public void setSocket(DatagramSocket socket)
    {
        this.socket = socket;
    }*/

    public UDPInvalidProtocol(/*DatagramSocket socket, InetAddress host, int port*/)
    {
        /*this.setSocket(socket);
        this.setHost(host);
        this.setPort(port);*/
    }

    public Document InvalidMessage(String message) throws IOException
    {
        Document res = new Document();
        res.append("command", "INVALID_PROTOCOL");
        res.append("message", message);
        return res;
        /*System.out.println(message);
        
        int length = res.toJson().getBytes().length;
        byte[] writeBytes = new byte[length];
		writeBytes = res.toJson().getBytes();
		writeBytes = Base64.getEncoder().encode(writeBytes);
        DatagramPacket sendPacket = new DatagramPacket (writeBytes, length, host, port);
        socket.send(sendPacket);
        socket.close();*/
    }

	/*public InetAddress getHost() {
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
	}*/
}
