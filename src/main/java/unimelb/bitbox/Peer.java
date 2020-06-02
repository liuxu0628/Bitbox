/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;

/**
 *
 * @author l2499
 */
public class Peer {
    public static void main(String[] args){
        if (Configuration.getConfigurationValue("mode").equals("udp")){
            try {
                UDPPeer.main();
            } catch (IOException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else if (Configuration.getConfigurationValue("mode").equals("tcp")){
            try {
                TCPPeer.main();
            } catch (IOException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
//                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            System.out.println("mode should be tcp or udp");
        }
    }
}
