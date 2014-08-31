/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.moka7.asyn;

/**
 *
 * @author MaHi
 */
public class S7Handler implements S7Listener{
    private int result;
    private int numberOfResult;
    private int expectedNumberOfResult;
    
    /**
     * 
     * @param r 
     */
    public S7Handler(int r) {
        this.expectedNumberOfResult = r;
    }
    
    /**
     * 
     * @param r 
     */
    @Override
    public void updateS7(int r) {
        result+=r;
        numberOfResult++;
        if(numberOfResult==expectedNumberOfResult)
            System.out.println("FERTIG ( " + result +" )");
        
    }
    
    
    
}
