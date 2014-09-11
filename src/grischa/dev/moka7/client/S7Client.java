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
public class S7Client extends S7Packet {
    /* Communiaction */

    private Socket socket;
    private SocketAddress socketaddr;
    private DataInputStream inStream;
    private DataOutputStream outStream;
    private boolean connected = false;

    /* Dynamic Communication Vars */
    private short connectionType = S7Utility.PG;
    private int rack = 0;
    private int slot = 2;

    /* Convert */
    private static byte[] buffer = null;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

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

    /**
     * Starts to Read the buffer at postion 0 and reads the Data up to the value that the S7Blockinfo deliver.
     * Use the S7Utility Utility Class to generate the specify KindOfArea like S7Utility.AreaDB.
     *
     * @param KindOfArea Decribes the Kind of S7FunctionsBlock S7AreaPE = 0x81 (Input Adress) S7AreaPA = 0x82
     * (Output Adress) S7AreaMK = 0x83 (Merker) S7AreaDB = 0x84 (Datablock) S7AreaCT = 0x1C (Counter) S7AreaTM
     * = 0x1D (Timer)
     * @param DBNumber Thats the internal Number of (PE,PA,MK,CT,TM) from the PLC.
     * @return - byte Array that contains the Data
     */
    public byte[] ReadArea(int KindOfArea, int DBNumber) {
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

        S7BlockInfo block = GetAgBlockInfo(KindOfArea, DBNumber);

        // If we are addressing Timers or counters the element size is 2
        if (KindOfArea == S7Utility.S7AreaCT || KindOfArea == S7Utility.S7AreaTM) {
            WordSize = 2;
        }

        MaxElements = (getPduLength() - 18) / WordSize;      // 18 = Reply telegram header
        TotElements = block.MC7Size();                      // Size from Datablock

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            SizeRequested = NumElements * WordSize;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, PDU, 0, lenToRead);  // **************
            PDU[27] = (byte) KindOfArea;                    // Set Telegram Frame  
            S7Utility.SetWordAt(PDU, 25, DBNumber);                // Set DB Number         
            S7Utility.SetWordAt(PDU, 23, NumElements);             // Set Number of elements   

            // Adjusts Start and word length
            if (KindOfArea == S7Utility.S7AreaCT || KindOfArea == S7Utility.S7AreaTM) {
                address = start;
                if (KindOfArea == S7Utility.S7AreaCT) {
                    PDU[22] = 0x1C;
                } else {
                    PDU[22] = 0x1D;
                }
            } else {
                address = start << 3;
            }

            // Address into the PLC (only 3 bytes)           
            PDU[30] = (byte) (address & 0x0FF);             // Adjusts Start and word length
            address = address >> 8;
            PDU[29] = (byte) (address & 0x0FF);
            address = address >> 8;
            PDU[28] = (byte) (address & 0x0FF);

            try {
                sendRequest(PDU, lenToRead, outStream);

                length = RecvIsoPacket(inStream);

                if (length - 25 == SizeRequested && PDU[21] == (byte) 0xFF && length >= 25) {
                    System.arraycopy(PDU, 25, buffer, offset, SizeRequested);
                    offset += SizeRequested;
                } else {
                    throw new ISOException("S7DataRead Error");
                }
            } catch (IOException ex) {
                System.out.println("S7InvalidPDU");
            }

            TotElements -= NumElements;
            start += NumElements * WordSize;
        }
        return buffer;
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public S7BlockInfo GetAgBlockInfo(int BlockType, int BlockNumber) {

        S7BlockInfo blockinfo = new S7BlockInfo();

        int length = 0;

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
                    throw new ISOException("errS7FunctionError");
                }
            } else {
                throw new ISOException("S7InvalidPDU");
            }

        } catch (ISOException ex) {
            logger.log(Level.SEVERE, "ISO Communication Error.", ex);
            return null;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "TCP Connection Failed.", ex);
            return null;
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
        S7BlockInfo block = GetAgBlockInfo(KindOfArea, DBNumber);

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

            System.arraycopy(S7_RW, 0, PDU, 0, lenToRead);      // Setup the telegram            
            S7Utility.SetWordAt(PDU, 2, isoSize);                      // Whole telegram Size                                           
            S7Utility.SetWordAt(PDU, 15, dataSize + 4);                // Data Length  
            S7Utility.SetWordAt(PDU, 23, NumElements);
            PDU[17] = (byte) 0x05;                              // Set Write Function           
            PDU[27] = (byte) KindOfArea;                        // Set Telegram Frame 
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
                        throw new ISOException("S7DataWrite");
                    }
                } else {
                    throw new ISOException("errS7InvalidPDU");
                }
            } catch (ISOException ex) {
                logger.log(Level.SEVERE, "ISO Communication Error.", ex);
                return;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "TCP Connection Failed.", ex);
                return;
            }

            offset += dataSize;
            TotElements -= NumElements;
            start += NumElements * WordSize;
        }
    }

    /**
     * Convert a byte Array (1010011..) into String that contains Hex Characters (6f62d10f..).
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

}
