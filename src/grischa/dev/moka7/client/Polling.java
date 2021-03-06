/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.client;

/**
 *
 * @author MaHi
 */
public class Polling extends Thread {

    private static byte[] Buffer = new byte[65536]; // 64K buffer (maximum for S7400 systems)
    private int NUMBER_OF_REQUEST = 0;
    S7Client client = null;
    ResponseClass resp = null;

    public Polling(int nbr,ResponseClass resp) {
        this.NUMBER_OF_REQUEST = nbr;
        this.resp=resp;
        resp.setPolling(this);
    }

    public void init() {
        // Connect to PLC
        client = new S7Client();
        client.connectTo("192.168.16.222", 102);

    }

    public void run() {
        int cnt = 0;
        boolean poll = true;
        // Do Something
        System.out.println("Polling started...");
//        while (poll) {
//          cnt++;
            for (int i = 0; i < NUMBER_OF_REQUEST; i++) {
               
                Buffer = client.ReadArea(S7.S7AreaDB, 100);
               // System.out.print("|" + cnt);
                if(i==25){
                    resp.setNumberOfRequest();
                }

//            System.out.println(client.bytesToHex(Buffer, 0, 12));
                
//                System.out.print("" + Integer.parseInt(client.bytesToHex(Buffer, 2, 4), 16));
//                System.out.print("" + Integer.parseInt(client.bytesToHex(Buffer, 4, 6), 16));
//                System.out.print("" + Integer.parseInt(client.bytesToHex(Buffer, 6, 10), 16));
//                System.out.print("" + Integer.parseInt(client.bytesToHex(Buffer, 10, 12), 16));

//            System.out.print("Value 1 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 0, 2), 16));
//            System.out.print("Value 2 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 2, 4), 16));
//            System.out.print("Value 3 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 4, 6), 16));
//            System.out.print("Value 4 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 6, 10), 16));
//            System.out.print("Value 5 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 10, 12), 16));
            }
            resp.printResult("Result   = "+Integer.parseInt(client.bytesToHex(Buffer, 0, 2), 16));
            resp.printResult("NbrOfReq = "+NUMBER_OF_REQUEST);

//            if (cnt > 10) {
//                poll = false;
//            }
//        }
    }

    public int getNUMBER_OF_REQUEST() {
        return NUMBER_OF_REQUEST;
    }

    public void setNUMBER_OF_REQUEST(int NUMBER_OF_REQUEST) {
        this.NUMBER_OF_REQUEST = NUMBER_OF_REQUEST;
    }
    
    
}
