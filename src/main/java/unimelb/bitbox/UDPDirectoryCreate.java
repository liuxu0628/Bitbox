package unimelb.bitbox;

import java.io.*;
import java.net.SocketException;
import java.util.Base64;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class UDPDirectoryCreate {
    /*private DatagramSocket socket;
    private InetAddress host;
    private int port;
    
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

	public DatagramSocket getSocket() {
		return socket;
	}

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }*/

    public UDPDirectoryCreate(/*DatagramSocket socket, InetAddress host, int port*/) {
        /*this.setSocket(socket);
        this.setHost(host);
        this.setHost(host);*/
    }

    public Document DirectoryCreate_request(FileSystemEvent event) throws IOException {
       
            Document req = new Document();
            req.append("command", "DIRECTORY_CREATE_REQUEST");
            req.append("pathName", event.pathName);
            /*try {
            int length = req.toJson().getBytes().length;
            byte[] writeBytes = new byte[length];
            writeBytes = req.toJson().getBytes();
            writeBytes = Base64.getEncoder().encode(writeBytes);
            DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
            socket.send(sendPacket);
            
        } catch (NullPointerException e) {
            new UDPInvalidProtocol(socket, host, port).InvalidMessage("the command is invalid");
        }catch (SocketException e) {
            socket.close();
        }*/
        return req;
    }

    public Document DirectoryCreate_response(Document req, FileSystemManager fm) throws IOException {
        Document res = new Document();
        
        String pathName = (String) req.get("pathName");
        res.append("command", "DIRECTORY_CREATE_RESPONSE");
        res.append("pathName", pathName);
        String message = process(pathName, fm);
        res.append("message", message);
        if (message.equals("directory created")) {
            res.append("status", true);
        } else {
            res.append("status", false);
        }
        return res;
        /*int length = req.toJson().getBytes().length;
        byte[] writeBytes = new byte[length];
        writeBytes = req.toJson().getBytes();
        writeBytes = Base64.getEncoder().encode(writeBytes);
        DatagramPacket sendPacket = new DatagramPacket(writeBytes, writeBytes.length, host, port);
        socket.send(sendPacket);*/
        
    }

    private String process(String pathName, FileSystemManager fm) {
        if (fm.isSafePathName(pathName)) {
            if (!fm.dirNameExists(pathName)) {
                if (fm.makeDirectory(pathName)) {
                    return "directory created";
                } else {
                    return "there was a problem creating the directory";
                }
            } else {
                return "pathname already exists";
            }
        } else {
            return "unsafe pathname given";
        }
    }
}
