package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Base64;

import unimelb.bitbox.util.Document;

public class Authorization {

	private Socket socket;

    public Authorization(Socket socket) {
        this.setSocket(socket);
    }

    public void Authorization_request(String identity) throws IOException
    {
    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

        Document req = new Document();
        req.append("command", "AUTH_REQUEST");
        req.append("identity", identity);

        bw.write(req.toJson() + "\n");
        bw.flush();
    }
    
    public void Authorization_response(String Sessionkey, String publicKey) throws Exception
    {
    	Document res = new Document();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        res.append("command", "AUTH_RESPONSE");
        if (publicKey.equals(""))
        {
        	res.append("status", false);
        	res.append("message", "public key not found");
        }
        else
        {
        	String sessionkey = new RSA().encrypt(Sessionkey, publicKey);
            sessionkey = Base64.getEncoder().encodeToString(sessionkey.getBytes());
            res.append("AES128", sessionkey);
        	res.append("status", true);
        	res.append("message", "public key found");
        }

        bw.write(res.toJson() + "\n");
        bw.flush();
//        socket.close();
    }
    
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
