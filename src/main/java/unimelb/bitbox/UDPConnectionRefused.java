package unimelb.bitbox;

import org.json.simple.JSONObject;
import unimelb.bitbox.util.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

import unimelb.bitbox.util.HostPort;

/**
 * The class with a connection_refused method which will be called when receive the connection request but the
 * connection limit has reached.
 */

public class UDPConnectionRefused
{

    /**
     * The socket passed by ThreadServer.
     */
    private DatagramSocket socket;
    private InetAddress host;
    private int port;

    /**
     * Constructor for ConnectionRefused.
     * @param socket The socket passed by ThreadServer.
     */
    public UDPConnectionRefused(DatagramSocket socket, InetAddress host, int port)
    {
        this.setSocket(socket);
        this.setHost(host);
        this.setPort(port);
    }

    /**
     *  Send a CONNECTION_REFUSED message to the peer trying to connect with us.
     * @param peers The host and listened ports of connected peers.
     * @throws IOException This is a mandatory exception when using I/O exception.
     */
    public void connection_refused(ArrayList<HostPort> peers) throws IOException
    {
        Document res = new Document();
        /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));*/

        res.append("command", "CONNECTION_REFUSED");
        res.append("message", "connection limit reached");
        ArrayList<JSONObject> hostport = new ArrayList<>();

        for (HostPort peer : peers)
        {
            JSONObject hp = new JSONObject();
            hp.put("host", peer.host);
            hp.put("port", peer.port);
            hostport.add(hp);
        }
        res.append("peers", hostport);
        
        int length = res.toJson().getBytes().length;
        byte[] writeBytes = new byte[length];
        writeBytes = res.toJson().getBytes();
        writeBytes = Base64.getEncoder().encode(writeBytes);
        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
        socket.send(sendPacket);

        /*bw.write(res.toJson() + "\n");
        bw.flush();*/
    }

	public InetAddress getHost() {
		return host;
	}

	public void setHost(InetAddress host) {
		this.host = host;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
