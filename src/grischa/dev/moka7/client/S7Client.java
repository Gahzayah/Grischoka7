/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.client;

import grischa.dev.moka7.exceptions.ISOException;
import grischa.dev.moka7.exceptions.TCPException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author MaHi
 */
public class S7Client extends S7Packet {
    /* Communiaction */

    private Socket socket;
    private SocketAddress socketaddr;
    private DataInputStream inStream;
    private DataOutputStream outStream;
    private boolean connected = false;

    /* Dynamic Communication Vars */
    private short connectionType = S7Utility.PG;
    private int ISP_TSAP_PORT = 102;

    /* Convert */
    private static byte[] buffer = null;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /* Apache Logger */
    private static final Logger logger = LogManager.getLogger(S7Client.class.getName());

    public S7Client() {
    }

    public S7Client(byte connectionType, int rack, int slot) {
        this.connectionType = connectionType;
    }

    /**
     * Specify a valid IpAddress that connect to a PLC, HMI and so on If you
     * want to use another Port you have to read the manual cause that have some
     * important information for this.
     *
     * @param ipAddress specfiy the ipAddress (PLC)
     * @param rack racknumber of (PLC)
     * @param slot slotnumber of (PLC)
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void connectTo(String ipAddress, int rack, int slot) {
        int size = 0;

        if (!connected) {
            logger.log(Level.INFO, "Connected to " + ipAddress);
            // First: TCP-Connection
            try {
                logger.log(Level.INFO, "Open TCP Socket (ISO-TSAP Port 102).");
                socketaddr = new InetSocketAddress(ipAddress, ISP_TSAP_PORT);
                socket = new Socket();
                //socket.isConnected();
                socket.connect(socketaddr, 5000);
                socket.setTcpNoDelay(true);
                inStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());

                setConnectionParams(connectionType, rack, slot);

                // Second: ISO-Connection
                logger.log(Level.INFO, "Sending Connection Request.");
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
                logger.log(Level.ERROR, "ISO Communication Error.", ex);
                return;
            } catch (IOException ex) {
                logger.log(Level.ERROR, "TCP Connection Failed.", ex);
                return;
            }
            logger.log(Level.INFO, "Negotiate PDU Length: " + size);
            logger.log(Level.INFO, "Connection established...successfully.");

        }
    }

    /**
     * Starts to Read the buffer at postion 0 and reads the Data up to the value
     * that the S7Blockinfo deliver. Use the S7Utility Utility Class to generate
     * the specify kindOfArea like S7Utility.AreaDB.
     *
     * @param kindOfArea Decribes the Kind of S7FunctionsBlock S7AreaPE = 0x81
     * (Input Adress) S7AreaPA = 0x82 (Output Adress) S7AreaMK = 0x83 (Merker)
     * S7AreaDB = 0x84 (Datablock) S7AreaCT = 0x1C (Counter) S7AreaTM = 0x1D
     * (Timer)
     * @param DBNumber Thats the internal Number of (PE,PA,MK,CT,TM) from the
     * PLC.
     * @return - byte Array that contains the Data
     */
    public byte[] ReadArea(int kindOfArea, int DBNumber) {
        buffer = new byte[65536];
        int address = 0;
        int length = 0;
        int WordSize = 1;
        int start = 0;
        int lenToRead = 31;
        int offset = 0;
        int SizeRequested = 0;
        int NumElements = 0;
        int MaxElements = 0;
        int TotElements = 0;

        S7BlockInfo block = GetAgBlockInfo(S7Utility.Block_DB, DBNumber);

        // If we are addressing Timers or counters the element size is 2
        if (kindOfArea == S7Utility.S7AreaCT || kindOfArea == S7Utility.S7AreaTM) {
            WordSize = 2;
        }

        MaxElements = (getPduLength() - 18) / WordSize;      // 18 = Reply telegram header
        TotElements = block.MC7Size();                       // Size from Datablock

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            SizeRequested = NumElements * WordSize;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, PDU, 0, lenToRead);          // **************
            PDU[27] = (byte) kindOfArea;                            // Set Telegram Frame  
            S7Utility.SetWordAt(PDU, 25, DBNumber);                 // Set DB Number         
            S7Utility.SetWordAt(PDU, 23, NumElements);              // Set Number of elements   

            // Address into the PLC (only 3 bytes)           
            PDU[30] = (byte) (address & 0x0FF);                     // Adjusts Start and word length
            address = address >> 8;
            PDU[29] = (byte) (address & 0x0FF);
            address = address >> 8;
            PDU[28] = (byte) (address & 0x0FF);

            // Adjusts Start and word length
            if (kindOfArea == S7Utility.S7AreaCT || kindOfArea == S7Utility.S7AreaTM) {
                address = start;
                if (kindOfArea == S7Utility.S7AreaCT) {
                    PDU[22] = 0x1C;
                } else {
                    PDU[22] = 0x1D;
                }
            } else {
                address = start << 3;
            }
            try {
                sendRequest(PDU, lenToRead, outStream);

                length = RecvIsoPacket(inStream);

                if (length - 25 == SizeRequested && PDU[21] == (byte) 0xFF && length >= 25) {
                    System.arraycopy(PDU, 25, buffer, offset, SizeRequested);
                    offset += SizeRequested;
                } else {
                    throw new ISOException("Receive invalid PDU in ReadArea");
                }
            } catch (IOException ex) {
                logger.log(Level.ERROR, "ISO Communication Error.", ex);
            }

            TotElements -= NumElements;
            start += NumElements * WordSize;
        }
        return buffer;
    }

    public byte[] readMuliVars(int Area, ArrayList<S7Item> list) throws IOException {
        byte[] buffer = new byte[65536];
        int length = 0;
        int address = 0;
        int frameSize = 19;
        int itemSize = 12;
        int itemsSize = itemSize * list.size();
        int lenToRead = frameSize + itemsSize;

        // Start to setup Telegram
        System.arraycopy(RW_MVARS, 0, PDU, 0, RW_MVARS.length);

        // Add Items to Telegramm
        for (int i = 0; i < list.size(); i++) {
            S7Utility.SetWordAt(ITEM, 4, list.get(i).getLength());          // Length
            S7Utility.SetWordAt(ITEM, 6, list.get(i).getDBNumber());        // DB-Number
            ITEM[8] = (byte) Area;                                          // Area-Type
            System.arraycopy(ITEM, 0, PDU, frameSize + itemSize * i, ITEM.length);
        }
        // Set up Parameter
        S7Utility.SetWordAt(PDU, 2, lenToRead);                           // TPTK Länge
        S7Utility.SetWordAt(PDU, 13, itemSize * list.size() + 2);         // Parameter Länge
        S7Utility.SetWordAt(PDU, 23, itemSize);                                   // Set Number of elements   


        // Address into the PLC (only 3 bytes)           
        PDU[30] = (byte) (address & 0x0FF);                                 // Adjusts Start and word length
        address = address >> 8;
        PDU[29] = (byte) (address & 0x0FF);
        address = address >> 8;
        PDU[28] = (byte) (address & 0x0FF);

        //PDU[22] = 0x1D;
        try {
            sendRequest(PDU, lenToRead, outStream);
            length = RecvIsoPacket(inStream);
        } catch (TCPException ex) {
            java.util.logging.Logger.getLogger(S7Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        if (PDU[21] == (byte) 0xFF && length >= 25) {
            System.arraycopy(PDU, 25, buffer, 0, lenToRead);
        } else {
            throw new ISOException("Receive invalid PDU in ReadArea");
        }

        return buffer;
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public S7BlockInfo GetAgBlockInfo(int BlockType, int BlockNumber) {
        int length = 0;
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
            length = RecvIsoPacket(inStream);
            if (length > 32) // the minimum expected
            {
                if (S7Utility.GetWordAt(PDU, 27) == 0 && PDU[29] == (byte) 0xFF) {
                    blockinfo.Update(PDU, 42);
                } else {
                    throw new ISOException("Receive a S7 Function Error in AGBlockinfo");
                }
            } else {
                throw new ISOException("Receive invalid PDU in AGBlockinfo");
            }

        } catch (ISOException ex) {
            logger.log(Level.ERROR, "ISO Communication Error.", ex);
        } catch (IOException ex) {
            logger.log(Level.ERROR, "TCP Connection Failed.", ex);
        }
        return blockinfo;
    }

    /**
     *
     * @param KindOfArea
     * @param DBNumber
     * @param Data
     */
    public void WriteArea(int KindOfArea, int DBNumber, byte[] Data) {
        int address = 0;
        int length = 0;
        int WordSize = 1;
        int start = 0;
        int lenToRead = 35;
        int offset = 0;
        int NumElements = 0;
        int MaxElements = 0;
        int TotElements = 0;
        int dataSize = 0;
        int isoSize;

        // For Timers or Counters the element size is 2
        if (KindOfArea == S7Utility.S7AreaCT || KindOfArea == S7Utility.S7AreaTM) {
            WordSize = 2;
        }
        S7BlockInfo block = GetAgBlockInfo(S7Utility.Block_DB, DBNumber);

        MaxElements = (getPduLength() - lenToRead) / WordSize;  // 18 = Reply telegram header
        TotElements = block.MC7Size();                          // Size from Datablock

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            dataSize = NumElements * WordSize;
            isoSize = 35 + dataSize;
            length = dataSize + 4;

            System.arraycopy(S7_RW, 0, PDU, 0, lenToRead);             // Setup the telegram            
            S7Utility.SetWordAt(PDU, 2, isoSize);                      // Whole telegram Size                                           
            S7Utility.SetWordAt(PDU, 15, dataSize + 4);                // Data Length  
            S7Utility.SetWordAt(PDU, 23, NumElements);
            PDU[17] = (byte) 0x05;                                     // Set Write Function           
            PDU[27] = (byte) KindOfArea;                               // Set Telegram Frame 
            // Num elements

            if (KindOfArea == S7Utility.S7AreaDB) {                     // Set DB Number
                S7Utility.SetWordAt(PDU, 25, DBNumber);
            }

            // Adjusts Start and word length
            if (KindOfArea == S7Utility.S7AreaCT || KindOfArea == S7Utility.S7AreaTM) {
                address = start;
                length = dataSize;

                if (KindOfArea == S7Utility.S7AreaCT) {
                    PDU[22] = 0x1C;
                } else {
                    PDU[22] = 0x1D;
                }
            } else {
                address = start << 3;
                length = dataSize << 3;
            }
            S7Utility.SetWordAt(PDU, 33, length);                      // Length

            // Address into the PLC
            PDU[30] = (byte) (address & 0x0FF);
            address = address >> 8;
            PDU[29] = (byte) (address & 0x0FF);
            address = address >> 8;
            PDU[28] = (byte) (address & 0x0FF);

            // Copies the Data
            System.arraycopy(Data, offset, PDU, 35, dataSize);
            try {
                sendRequest(PDU, isoSize, outStream);
                length = RecvIsoPacket(inStream);
                if (length == 22) {
                    if ((S7Utility.GetWordAt(PDU, 17) != 0) || (PDU[21] != (byte) 0xFF)) {
                        throw new ISOException("S7 Error writing data to the CPU.");
                    }
                } else {
                    throw new ISOException("Receive invalid PDU in WriteArea.");
                }
            } catch (ISOException ex) {
                logger.log(Level.ERROR, "ISO Communication Error.", ex);
                return;
            } catch (IOException ex) {
                logger.log(Level.ERROR, "TCP Connection Failed.", ex);
                return;
            }

            offset += dataSize;
            TotElements -= NumElements;
            start += NumElements * WordSize;
        }
    }

    /**
     * Convert a byte Array (1010011..) into String that contains Hex Characters
     * (6f62d10f..).
     *
     * @param bytes byteArray to convert
     * @param start startpoint of the buffer
     * @param len endpoint of buffer (for complete reading use buffer.length)
     * @return Hexadezimal String from byteArray
     */
    public static String bytesToHex(byte[] bytes, int start, int len) {
        char[] hexChars = new char[(len - start) * 2];
        byte[] bytesToRead = new byte[len - start];
        System.arraycopy(bytes, start, bytesToRead, 0, len - start);

        for (int j = 0; j < bytesToRead.length; j++) {
            int v = bytesToRead[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
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

    public void serviceAbilityTest() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
