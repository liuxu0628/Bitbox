/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unimelb.bitbox.util;

import java.security.Timestamp;

/**
 *
 * @author l2499
 */
public class ErrorHandling {
    private Timestamp time;
    private Document request;
    private int retry;
    private long position;
    private String pathName;
    
    
    public ErrorHandling(Timestamp time, Document request, int retry) {
        this.time = time;
        this.request = request;
        this.retry = retry;
    }
    
    
    
    public Timestamp getTime() {
        return time;
    }

    public Document getRequest() {
        return request;
    }

    public int getRetry() {
        return retry;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public void setRequest(Document request) {
        this.request = request;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
    

    
}
