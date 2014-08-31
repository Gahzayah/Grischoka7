/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.client;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MaHi
 */
public class Polling1 extends Thread {

    private static byte[] Buffer = new byte[65536]; // 64K buffer (maximum for S7400 systems)
    private int NUMBER_OF_REQUEST = 0;
    S7Client client = null;

    public Polling1(int nbr) {
        this.NUMBER_OF_REQUEST = nbr;
    }


    public void run() {
        int cnt = 0;
        boolean poll = true;
        System.out.println("Start Polling 2...");
        while (poll) {
            cnt++;
            // Abarbeiten Anzahl der Request
            for (int i = 0; i < NUMBER_OF_REQUEST; i++) {
                try {
                    sleep(75);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Polling1.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("Another Task working.." + cnt);
            try {
                sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Polling1.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(cnt>10){
                poll=false;
            }
        }
    }
}
