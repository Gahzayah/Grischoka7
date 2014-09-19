package _test;

import grischa.dev.moka7.asyn.S7Handler;
import grischa.dev.moka7.asyn.S7Service;
import grischa.dev.moka7.client.S7Client;
import grischa.dev.moka7.client.S7Utility;
import java.io.IOException;

/**
 *
 * @author mhi
 */
public class main_s7client extends Thread {

    public static void main(String[] args) throws InterruptedException, IOException {
        /*** Connect to PLC ***/
        S7Client client = new S7Client();
        client.setConnectionParams(S7Utility.PG, 0, 2);
        client.connectTo("192.168.16.222", 0, 2);

        /*** START FUNCTIONSTEST ***/
        //client.serviceAbilityTest();

        /*** CONNECT TO DATABASE ***/
        S7Handler handler = new S7Handler();
        handler.connectToDB();
        handler.showTableModel("ProcessVars");

        /*** READ PROCVARS FOR WORKER ***/
        //handler.readProcVars();

        /*** Read Task ( Write Task ) ***/
        //handler.readProcVarsTask();

        /*** Initialize Polling Service ***/
        S7Service service = new S7Service( handler , client );
       // service.readData();
        service.readMultivars();
 //       service.pollPLC();
        /*** Start Polling **/

            // readProcVars()
        // readArea()
        // readTasks()
        // writeArea()
//        Polling t1 = new Polling();
////        Polling1 t2 = new Polling1(10);    
//        t1.start();
////        t2.start();
//        // Synchronized Requesting .. Blocking here
//        long start = System.currentTimeMillis();
//        t1.join(18000);
//        long end = System.currentTimeMillis();
//        System.out.println(" <> Zeit = " + (end - start) + "ms");


//        // BlockAGBlockInfo ( DB, FB, .. )
//        S7BlockInfo blockSFC = client.GetAgBlockInfo(S7.Block_SFC, 1);
//        
//        S7BlockInfo block101 = client.GetAgBlockInfo(S7.Block_DB, 101);
//        System.out.println("Block Flags     : " + Integer.toBinaryString(block100.BlkFlags()));
//        System.out.println("Block Number    : " + block100.BlkNumber());
//        System.out.println("Block Languege  : " + block100.BlkLang());
//        System.out.println("Load Size       : " + block100.LoadSize());
//        System.out.println("SBB Length      : " + block100.SBBLength());
//        System.out.println("Local Data      : " + block100.LocalData());
//        System.out.println("MC7 Size        : " + block100.MC7Size());
//        System.out.println("Author          : " + block100.Author());
//        System.out.println("Family          : " + block100.Family());
//        System.out.println("Header          : " + block100.Header());
//        System.out.println("Version         : " + block100.Version());
//        System.out.println("Checksum        : 0x" + Integer.toHexString(block100.Checksum()));
//        SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy");
//        System.out.println("Code Date       : " + ft.format(block100.CodeDate()));
//        System.out.println("Interface Date  : " + ft.format(block100.IntfDate()));
//
//        Buffer = client.ReadArea(S7.S7AreaDB, 100);
//        System.out.println("ReadArea BufferSize: " + block100.MC7Size());
//
//        System.out.println(client.bytesToHex(Buffer, 0, 12));
//
//        System.out.println("Value 1 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 0, 2), 16));
//        System.out.println("Value 2 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 2, 4), 16));
//        System.out.println("Value 3 (2 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 4, 6), 16));
//        System.out.println("Value 4 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 6, 10), 16));
//        System.out.println("Value 5 (4 bytes): " + Integer.parseInt(client.bytesToHex(Buffer, 10, 12), 16));
//
//        client.disconnect();
    }

}
