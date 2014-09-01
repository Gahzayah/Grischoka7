/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.moka7.client;



     

/**
 *
 * @author mhi
 */
public class ResponseClass {
private Polling poll = null;

    public void printResult(String result){
        System.out.println(result);
        
    }
    
    public void setPolling(Polling poll){
        this.poll=poll;
    }
    
    public void setNumberOfRequest(){
        poll.setNUMBER_OF_REQUEST(50);
    }
    
}
