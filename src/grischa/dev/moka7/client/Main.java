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
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Connect to PLC
        S7Client client = new S7Client();
        client.disconnect();
        client.connectTo("192.168.16.222", 102);
        // BlockAGBlockInfo ( DB, FB, .. )
        client.GetAgBlockInfo(S7.Block_SFC, 1);

        client.disconnect();
        

    }

}
