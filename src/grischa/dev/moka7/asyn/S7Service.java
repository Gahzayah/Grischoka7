/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.asyn;

import grischa.dev.moka7.client.S7Client;
import grischa.dev.moka7.client.S7Item;
import grischa.dev.moka7.client.S7Utility;
import grischa.dev.moka7.data.ProcVars;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author MaHi
 */
public class S7Service implements Runnable {

    private static final Logger logger = Logger.getLogger(S7Service.class.getName());

    private ArrayList<ProcVars> list;

    private final S7Handler handler;
    private final S7Client client;

    private static final int POLLING_FREQ = 1000; //ms

    @SuppressWarnings("LeakingThisInConstructor")
    public S7Service(S7Handler handler, S7Client client) {
        this.handler = handler;
        this.client = client;
        this.handler.setService(this);
    }

    @Override
    public void run() {
        list = null;

        //listener.receiveVars(list);
    }

    public void pollPLC() {
        Boolean active = true;
        int counter = 0;

        while (active) {
            logger.log(Level.INFO, "Polling started..");
            counter++;
            try {
                sleep(POLLING_FREQ);
                readData();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "Sleep invoke an Error");
                return;
            }
            if (counter >= 10) {
                active = false;
            }
        }
        logger.log(Level.INFO, "Polling ends.");

    }

    @SuppressWarnings("static-access")
    public void readData() {
        byte[] Buffer = new byte[65536];
        Buffer = client.ReadArea(S7Utility.S7AreaDB, 100);

//     System.out.println(client.bytesToHex(Buffer, 0, 12));
        System.out.println("Value 1 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 0, 2), 16));
        System.out.println("Value 2 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 2, 4), 16));
        System.out.println("Value 3 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 4, 6), 16));
        System.out.println("Value 4 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 6, 10), 16));
        System.out.println("Value 5 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 10, 12), 16));
    }
    
        @SuppressWarnings("static-access")
    public void readMultivars() throws IOException {
        byte[] Buffer = new byte[65536];
        ArrayList<S7Item> source = null;
        ArrayList<S7Item> destin = null;
        // Read Processvariabeln
        //handler.readProcVars();
        
        // Fill up List for Polling
        for(int i= 1 ; i <=13 ; i++){
            destin = new ArrayList();
            destin.add(new S7Item(100, 0, 12));
         }
        // Read Data from PLC
        
        long start = System.currentTimeMillis();
        Buffer = client.readMuliVars(S7Utility.S7AreaDB, destin);
        long end = System.currentTimeMillis();
        

     System.out.println(client.bytesToHex(Buffer, 0, 36));
        System.out.println("Value 1 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 0, 2), 16));
        System.out.println("Value 2 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 2, 4), 16));
        System.out.println("Value 3 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 4, 6), 16));
        System.out.println("Value 4 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 6, 10), 16));
        System.out.println("Value 5 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 10, 12), 16));
        

       System.out.println("Value 3 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 16, 18), 16));
       System.out.println("Execution Time = " + (end-start));

                
    }

    public void setWriteAreaTask(ArrayList<ProcVars> wpv) {

    }
}
