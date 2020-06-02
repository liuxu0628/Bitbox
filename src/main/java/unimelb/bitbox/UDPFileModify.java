package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class UDPFileModify {

    /*   private Socket socket;*/
    public UDPFileModify(/*Socket socket*/) {
        /*this.setSocket(socket);*/
    }

    /*
     * FileModify_request: almost the same as file create, only change the command from "FILE_CREATE_REQUEST" to "FILE_MODIFY_REQUEST".
     */
    public Document FileModify_request(FileSystemEvent event) throws IOException, NoSuchAlgorithmException {

        String pathName = event.pathName;
        Document fileDescriptor = event.fileDescriptor.toDoc();
        /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));*/

        Document req = new Document();
        req.append("command", "FILE_MODIFY_REQUEST");
        req.append("fileDescriptor", fileDescriptor);
        req.append("pathName", pathName);

        return req;
        /*bw.write(req.toJson() + "\n");
        bw.flush();*/
    }

    /*
     * FileModify_response: almost the same as FileCreate_response, only change the command information.
     */
    public Document FileModify_response(Document req, FileSystemManager fm) throws IOException, NoSuchAlgorithmException, NullPointerException {
        Document fileDescriptor = (Document) req.get("fileDescriptor");
        Document res = new Document();
        /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));*/

        String pathName = (String) req.get("pathName");
        res.append("command", "FILE_MODIFY_RESPONSE");
        res.append("fileDescriptor", fileDescriptor);
        res.append("pathName", pathName);
        String message = process(pathName, fileDescriptor, fm);
        res.append("message", message);

        if (message.equals("file loader ready")) {
            res.append("status", true);
            /*bw.write(res.toJson() + "\n");
            System.out.println(res.toJson());
            bw.flush();*/
            UDPFileBytes fb = new UDPFileBytes();
            fb.FileBytes_request(fm, req);
            return res;
        } else {
            res.append("status", false);
            return res;
            /*bw.write(res.toJson() + "\n");
            bw.flush();*/
        }
    }

    /*
     * process: this function takes the information of the existing file, and check if the modify file loader succeed.
     * parameter: String pathName, Document fileDescriptor, FileSystemManager fm.
     * return: the message whether the modify file loader succeed.
     */
    private String process(String pathName, Document fileDescriptor, FileSystemManager fm) {
        String md5 = (String) fileDescriptor.get("md5");
        long lastModified = (long) fileDescriptor.get("lastModified");
        try {
            String message = "";
            if (fm.isSafePathName(pathName)) {
                if (fm.fileNameExists(pathName)) {
                    if (!fm.fileNameExists(pathName, md5)) {
                        if (!fm.checkShortcut(pathName)) {
                            if (fm.modifyFileLoader(pathName, md5, lastModified)) {

                                message += "file loader ready";

                            } else {
                                //System.out.println("there is something wrong with file loader");
                                message += "there is something wrong with file loader";
                            }
                        } else {
                            message += "there is a shortcut";
                        }

                    } else {
                        message += "there is a shortcut";
                    }
                } else {
                    message += "pathname already exists";
                }
            } else {
                message += "unsafe pathname given";
            }
            return message;
        } catch (NoSuchAlgorithmException | IOException e) {
            try {
                fm.cancelFileLoader(pathName);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return "something wrong with checking the file loader";
        }

    }

    /*    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }*/
}
