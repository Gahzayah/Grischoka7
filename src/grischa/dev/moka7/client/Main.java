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
public class Main extends Thread {

    public static void main(String[] args) throws InterruptedException {
        ResponseClass resp = new ResponseClass();
        Polling t1 = new Polling(100,resp);
        t1.init();     
        t1.start();

        // Synchronized Requesting .. Blocking here
        long start = System.currentTimeMillis();
        t1.join(18000);
        long end = System.currentTimeMillis();
        System.out.println("ExcuTime = " + (end - start) + "ms");
        
        


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
