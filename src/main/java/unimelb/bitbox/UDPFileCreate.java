package unimelb.bitbox;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class UDPFileCreate {

    /*private Socket socket;*/

    public UDPFileCreate(/*Socket socket*/) {
        /*this.setSocket(socket);*/
    }
    /*
     * file create request: This function is used for marshaling a request whenever a file create event is detected.
     * parameter: FileSystemEvent event.
     * returns: void.
     */
    public Document FileCreate_request(FileSystemEvent event) throws IOException, NoSuchAlgorithmException, SocketException {
            String pathName = event.pathName;
            Document fileDescriptor = event.fileDescriptor.toDoc();
            /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
*/
            Document req = new Document();
            req.append("command", "FILE_CREATE_REQUEST");
            req.append("fileDescriptor", fileDescriptor);
            req.append("pathName", pathName);
            return req;
            /*bw.write(req.toJson() + "\n");
            bw.flush();*/
    }
    /*
     * file create request: This function is called after verifying the file create request is received.
     * It unmarshaling the request and call process function to create a file loader. If successful, sending back the response to peers.
     * Then, it call FileByte_request to ask for the file content.
     * parameter: Document req, FileSystemManager fm.
     * return: if create file loader successful, returns true. else false.
     */
    public Document FileCreate_response(Document req, FileSystemManager fm) throws IOException, NoSuchAlgorithmException,NullPointerException
    {
            Document fileDescriptor = (Document) req.get("fileDescriptor");
            Document res = new Document();
            /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));*/
            //marshaling and unmarshaling.
            String pathName = (String) req.get("pathName");
            res.append("command", "FILE_CREATE_RESPONSE");
            res.append("fileDescriptor", fileDescriptor);
            res.append("pathName", pathName);
            String message = process(pathName, fileDescriptor, fm);
            res.append("message", message);
            //check if the process succeed.
            if (message.equals("file loader ready")) {
                res.append("status", true);
                /*bw.write(res.toJson() + "\n");
                System.out.println(res.toJson());
                bw.flush();*/

                return res;
            }
            else {
            	res.append("status", false);
                /*bw.write(res.toJson() + "\n");
                bw.flush();*/
                return res;
            }
    }

    private String process(String pathName, Document fileDescriptor, FileSystemManager fm) throws NoSuchAlgorithmException, IOException 
    {

    	String md5 = (String) fileDescriptor.get("md5");

        long lastModified = (long) fileDescriptor.get("lastModified");
        long fileSize = (long) fileDescriptor.get("fileSize");

        if (fm.isSafePathName(pathName)) {
            //System.out.println(1);
            if (!fm.fileNameExists(pathName)) {
                //System.out.println(2);

                    //System.out.println(3);
                    if (fm.createFileLoader(pathName, md5, fileSize, lastModified)) {                
                        if (!fm.checkShortcut(pathName)) {
                        //System.out.println(4);
                        // System.out.println("file loader ready");
                        return "file loader ready";
                    } else {
                        return "there is a shortcut";

                    }
                } else {
                        //System.out.println("there is something wrong with file loader");
                        return "there is something wrong with file loader";
                }
            } else {
                return "pathname already exists";
            }
        } else {
            return "unsafe pathname given";
        }
    }

/*    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }*/

}
