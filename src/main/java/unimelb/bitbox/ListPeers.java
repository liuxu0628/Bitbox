package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONObject;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import javax.crypto.spec.SecretKeySpec;

public class ListPeers {

    private Socket socket;
    private SecretKeySpec sessionKey;

    public ListPeers(Socket socket, String sessionKey) {
        this.setSocket(socket);
        this.sessionKey = AES.getKey(sessionKey);
    }
    public ListPeers(String sessionKey)
    {
    	this.sessionKey = AES.getKey(sessionKey);
    }

    public void ListPeers_request() throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        Document payload = new Document();
        payload.append("command", "LIST_PEERS_REQUEST");

        String content = AES.Session_encrypt(payload.toJson(), sessionKey);
        content = Base64.getEncoder().encodeToString(content.getBytes());

        // This is the final request message.
        Document req = new Document();
        req.append("payload", content);

        bw.write(req.toJson() + "\n");
        bw.flush();
    }

    public void ListPeers_response(ArrayList<HostPort> peers) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        Document payload = new Document();
        payload.append("command", "LIST_PEERS_RESPONSE");
        ArrayList<JSONObject> hostport = new ArrayList<>();
        try
        {
        	        for (HostPort peer : peers)
        {
            JSONObject hp = new JSONObject();
            hp.put("host", peer.host);
            hp.put("port", peer.port);
            hostport.add(hp);
        }
        payload.append("peers", hostport);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
        	payload.append("peers", "");
        }

        String content = AES.Session_encrypt(payload.toJson(), sessionKey);
        content = Base64.getEncoder().encodeToString(content.getBytes());

        // This is the final response message.
        Document res = new Document();
        res.append("payload", content);

        bw.write(res.toJson() + "\n");
        bw.flush();
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
