/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.client;

import grischa.dev.moka7.exceptions.ISOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;

/**
 *
 * @author MaHi
 */
public class S7Client extends S7Telegram {

    private Socket socket;
    private SocketAddress socketaddr;
    private DataInputStream inStream;
    private DataOutputStream outStream;
    private boolean connected = false;

    private short connectionType = S7.PG;
    private int rack = 0;
    private int slot = 2;

    private static byte[] buffer = null;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private int Address = 0;
    private int Length = 0;

    public S7Client() {

    }

    public S7Client(byte connectionType, int rack, int slot) {
        this.connectionType = connectionType;
        this.rack = rack;
        this.slot = slot;
    }

    /**
     * Specify a valid IpAddress that connect to a PLC, HMI and so on If you want to use another Port you have
     * to read the manual cause that have some important information for this.
     *
     * @param ipAddress -specfiy the ipAddress (PLC)
     * @param isoTcpPort -The Standart isoTcpPort is 102.
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void connectTo(String ipAddress, int isoTcpPort) {
        int size = 0;

        if (!connected) {
            // First: TCP-Connection
            try {
                socketaddr = new InetSocketAddress(ipAddress, isoTcpPort);
                socket = new Socket();
                //socket.isConnected();
                socket.connect(socketaddr, 5000);
                socket.setTcpNoDelay(true);
                inStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());

                setConnectionParams(connectionType, rack, slot);

                // Second: ISO-Connection
                sendRequest(ISO_CR, outStream);                         // Send the ConnectionRequest-Package
                size = RecvIsoPacket(inStream);                          // Gets the reply (if any)
                if (size == 22) {
                    if (getLastPDUType() != (byte) 0xD0) {
                        throw new ISOException("ISO connection refused by the CPU.");
                    }
                } else {
                    throw new ISOException("Invalid ISO PDU received.");
                }
                // Third: NegotiatePduLength
                size = NegotiatePduLength(outStream, inStream);
            } catch (ISOException ex) {
                logger.log(Level.SEVERE, "ISO Communication Error.", ex);
                return;
            } catch (IOException ex) {
                logger.log(Level.WARNING, "TCP Connection Failed.", ex);
                return;
            }
            System.out.println("Connect to " + ipAddress + " :" + isoTcpPort);
            System.out.println("Negotiate PDU: " + size);

        }

    }

//    public int DBGet(int DBNumber, byte[] Buffer, IntByRef SizeRead) throws IOException, ISOException, InterruptedException
//    {
//        S7BlockInfo Block = new S7BlockInfo();
//        
//        if (GetAgBlockInfo(S7.Block_DB, DBNumber, Block)==0)    // Query the DB Length
//        {
//            int SizeToRead = Block.MC7Size();
//            // Checks the room
//            if (SizeToRead<=Buffer.length)
//            {
//                if (ReadArea(S7.S7AreaDB, DBNumber, 0, SizeToRead, Buffer)==0)
//                    SizeRead.Value=SizeToRead;
//            }
//            else
//                System.out.println("errS7BufferTooSmall");
//        }
//        return 0;
//    }  
    /**
     * Starts to Read the buffer at postion 0 and reads the Data up to the value that the S7Blockinfo deliver.
     *
     * @param Area Decribes the Kind of S7FunctionsBlock S7AreaPE = 0x81 (Input Adress) S7AreaPA = 0x82
     * (Output Adress) S7AreaMK = 0x83 (Merker) S7AreaDB = 0x84 (Datablock) S7AreaCT = 0x1C (Counter) S7AreaTM
     * = 0x1D (Timer)
     * @param DBNumber Thats the internal Number of (PE,PA,MK,CT,TM) from the PLC.
     * @return - byte Array that contains the Data
     */
    public byte[] ReadArea(int Area, int DBNumber) {
        buffer = new byte[65536];
        int start = 0;
        int lenToRead = 31;
        int offset = 0;
        int SizeRequested = 0;
        int NumElements = 0;
        int MaxElements = 0;
        int TotElements = 0;
        
        S7BlockInfo block = GetAgBlockInfo(S7.Block_DB, DBNumber);

        MaxElements = getPduLength() - 18;              // 18 = Reply telegram header
        TotElements = block.MC7Size();                  // Size from Datablock

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            SizeRequested = NumElements;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, PDU, 0, lenToRead);
            PDU[27] = (byte) Area;                      // Set DB Number 
            S7.SetWordAt(PDU, 25, DBNumber);            // Set Area         
            S7.SetWordAt(PDU, 23, NumElements);         // Num elements                             

            // Address into the PLC (only 3 bytes)           
            PDU[30] = (byte) (start << 3 & 0x0FF);  // Adjusts Start and word length
            Address = Address >> 8;
            PDU[29] = (byte) (start << 3 & 0x0FF);  // Adjusts Start and word length
            Address = Address >> 8;
            PDU[28] = (byte) (start << 3 & 0x0FF);  // Adjusts Start and word length
            try {
                sendRequest(PDU, lenToRead, outStream);

                Length = RecvIsoPacket(inStream);

                if (Length - 25 == SizeRequested && PDU[21] == (byte) 0xFF && Length >= 25) {
                    System.arraycopy(PDU, 25, buffer, offset, SizeRequested);
                    offset += SizeRequested;
                } else {
                    throw new ISOException("S7DataRead");
                }
            } catch (IOException ex) {
                System.out.println("23");
            }

            TotElements -= NumElements;
            start += NumElements;
        }
        return buffer;
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public S7BlockInfo GetAgBlockInfo(int BlockType, int BlockNumber) {
        S7BlockInfo blockinfo = new S7BlockInfo();

        // Block Type
        S7_BI[30] = (byte) BlockType;
        // Block Number
        S7_BI[31] = (byte) ((BlockNumber / 10000) + 0x30);
        BlockNumber = BlockNumber % 10000;
        S7_BI[32] = (byte) ((BlockNumber / 1000) + 0x30);
        BlockNumber = BlockNumber % 1000;
        S7_BI[33] = (byte) ((BlockNumber / 100) + 0x30);
        BlockNumber = BlockNumber % 100;
        S7_BI[34] = (byte) ((BlockNumber / 10) + 0x30);
        BlockNumber = BlockNumber % 10;
        S7_BI[35] = (byte) ((BlockNumber / 1) + 0x30);
        try {

            sendRequest(S7_BI, outStream);
            Length = RecvIsoPacket(inStream);
            if (Length > 32) // the minimum expected
            {
                if (S7.GetWordAt(PDU, 27) == 0 && PDU[29] == (byte) 0xFF) {
                    blockinfo.Update(PDU, 42);
                } else {
                    throw new ISOException("errS7FunctionError");
                }
            } else {
                throw new ISOException("S7InvalidPDU");
            }

        } catch (ISOException ex) {
            logger.log(Level.SEVERE, "ISO Communication Error.", ex);
            return null;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "TCP Connection Failed.", ex);
            return null;
        }
        return blockinfo;
    }

//    public int readCounter(int Area, int DBNumber, int Start, int Amount, byte[] Data) throws ISOException, IOException, InterruptedException {
//        WordSize = 2;
//        int lengthRead = 31;
//
//        MaxElements = (getPduLength() - 18) / WordSize; // 18 = Reply telegram header
//        TotElements = Amount;
//
//        while (TotElements > 0) {
//            NumElements = TotElements;
//            if (NumElements > MaxElements) {
//                NumElements = MaxElements;
//            }
//             Setup the telegram
//            System.arraycopy(S7_RW, 0, PDU, 0, lengthRead);
//             Set DB Number
//            PDU[27] = (byte) Area;
//            PDU[22] = (byte) 0x1C;
//
//            SizeRequested = NumElements * WordSize;
//             Num elements
//            S7.SetWordAt(PDU, 23, NumElements);
//             Address into the PLC (only 3 bytes)           
//            PDU[30] = (byte) (Start & 0x0FF);
//            Address = Address >> 8;
//            PDU[29] = (byte) (Start & 0x0FF);
//            Address = Address >> 8;
//            PDU[28] = (byte) (Start & 0x0FF);
//
//            sendRequest(PDU, lengthRead, outStream);
//
//            Length = RecvIsoPacket(inStream);
//
//            if ((Length - 25 == SizeRequested) && (PDU[21] == (byte) 0xFF) && Length >= 25) {
//                System.arraycopy(PDU, 25, Data, Offset, SizeRequested);
//                Offset += SizeRequested;
//            } else {
//                throw new ISOException("errS7CounterRead");
//            }
//            TotElements -= NumElements;
//            Start += NumElements * WordSize;
//        }
//        return 11111;
//    }

    /**
     * Convert a byte Array into String that contain Hex Characters.
     * @param bytes byte Array that must be convert
     * @param start startpoint of reading the buffer
     * @param len the length to read of buffer (for complete reading use buffer.length)
     * @return Hexadezimal String from byteArray
     */
    public static String bytesToHex(byte[] bytes, int start, int len){
        char[] hexChars = new char[(len-start) * 2];
        byte[] bytesToRead = new byte[len-start];
        System.arraycopy(bytes, start, bytesToRead, 0, len-start);
        
        for( int j = 0; j < bytesToRead.length;j++){
            int v = bytesToRead[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 +1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    /**
     * Disconnect a active TCP-Connection and close the TCP-Socket
     */
    public void disconnect() {
        if (connected) {
            try {
                outStream.close();
                inStream.close();
                socket.close();
            } catch (IOException ex) {
            }
            connected = false;
        }
    }

}
