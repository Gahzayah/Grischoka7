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
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(S7Client.class.getName());

    private byte connectionType = 0x02;
    private int rack = 0;
    private int slot = 2;

    private byte[] PDU = new byte[2048];
    private int Address = 0;
    private int NumElements = 0;
    private int MaxElements = 0;
    private int TotElements = 0;
    private int SizeRequested = 0;
    private int Length = 0;
    private int Offset = 0;
    private int WordSize = 0;

    public S7Client() {

    }

    public S7Client(byte connectionType, int rack, int slot) {
        this.connectionType = connectionType;
        this.rack = rack;
        this.slot = slot;
    }

    /**
     * Specify a valid IpAddress that connect to a PLC, HMI and so on If you
     * want to use another Port you have to read the manual cause that have some
     * important information for this.
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
                sendRequest(ISO_CR, ISO_CR.length, outStream);           // Send the ConnectionRequest-Package
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
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, "Interrupped Exception", ex);
            }
            System.out.println("Connect to " + ipAddress + " :" + isoTcpPort);
            System.out.println("Negotiate PDU: " +size);
            

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
    public void ReadArea(int Area, int DBNumber, int Start, int Amount, byte[] Data) throws ISOException, IOException, InterruptedException {
        WordSize = 1;
        Length = 0;

        MaxElements = (getPduLength() - 18) / WordSize; // 18 = Reply telegram header
        TotElements = Amount;

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            SizeRequested = NumElements * WordSize;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, PDU, 0, LENGTH_READ);
            PDU[27] = (byte) Area;                      // Set DB Number 
            S7.SetWordAt(PDU, 25, DBNumber);            // Set Area         
            S7.SetWordAt(PDU, 23, NumElements);         // Num elements                             

            // Address into the PLC (only 3 bytes)           
            PDU[30] = (byte) (Start << 3 & 0x0FF);  // Adjusts Start and word length
            Address = Address >> 8;
            PDU[29] = (byte) (Start << 3 & 0x0FF);  // Adjusts Start and word length
            Address = Address >> 8;
            PDU[28] = (byte) (Start << 3 & 0x0FF);  // Adjusts Start and word length

            sendRequest(PDU, LENGTH_READ, outStream);

            Length = RecvIsoPacket(inStream);

            if (Length - 25 == SizeRequested && PDU[21] == (byte) 0xFF && Length >= 25) {
                System.arraycopy(PDU, 25, Data, Offset, SizeRequested);
                Offset += SizeRequested;
            } else {
                throw new ISOException("errS7DataRead");
            }

            TotElements -= NumElements;
            Start += NumElements * WordSize;
        }
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public void GetAgBlockInfo(int BlockType, int BlockNumber) {
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
            sendRequest(S7_BI, S7_BI.length, outStream);
            Length = RecvIsoPacket(inStream);
            if (Length > 32) // the minimum expected
            {
                if ( S7.GetWordAt(PDU,27)==0 && PDU[29]==(byte)0xFF ) {
                    blockinfo.Update(PDU, 42);
                } else {
                    throw new ISOException("errS7FunctionError");
                }
            } else {
                throw new ISOException("S7InvalidPDU");
            }

        } catch (ISOException ex) {
            logger.log(Level.SEVERE, "ISO Communication Error.", ex);
            return;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "TCP Connection Failed.", ex);
            return;
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, "Interrupped Exception", ex);
            return;
        }


    }

    public int readCounter(int Area, int DBNumber, int Start, int Amount, byte[] Data) throws ISOException, IOException, InterruptedException {
        WordSize = 2;

        MaxElements = (getPduLength() - 18) / WordSize; // 18 = Reply telegram header
        TotElements = Amount;

        while (TotElements > 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }
            // Setup the telegram
            System.arraycopy(S7_RW, 0, PDU, 0, LENGTH_READ);
            // Set DB Number
            PDU[27] = (byte) Area;
            PDU[22] = (byte) 0x1C;

            SizeRequested = NumElements * WordSize;
            // Num elements
            S7.SetWordAt(PDU, 23, NumElements);
            // Address into the PLC (only 3 bytes)           
            PDU[30] = (byte) (Start & 0x0FF);
            Address = Address >> 8;
            PDU[29] = (byte) (Start & 0x0FF);
            Address = Address >> 8;
            PDU[28] = (byte) (Start & 0x0FF);

            sendRequest(PDU, LENGTH_READ, outStream);

            Length = RecvIsoPacket(inStream);

            if ((Length - 25 == SizeRequested) && (PDU[21] == (byte) 0xFF) && Length >= 25) {
                System.arraycopy(PDU, 25, Data, Offset, SizeRequested);
                Offset += SizeRequested;
            } else {
                throw new ISOException("errS7CounterRead");
            }
            TotElements -= NumElements;
            Start += NumElements * WordSize;
        }
        return 11111;
    }

    public static void HexDump(byte[] Buffer, int Size) {
        int r = 0;
        String Hex = "";

        for (int i = 0; i < Size; i++) {
            int v = (Buffer[i] & 0x0FF);
            String hv = Integer.toHexString(v);

            if (hv.length() == 1) {
                hv = "0" + hv + " ";
            } else {
                hv = hv + " ";
            }

            Hex = Hex + hv;

            r++;
            if (r == 16) {
                System.out.print(Hex + " ");
                System.out.println(S7.GetPrintableStringAt(Buffer, i - 15, 16));
                Hex = "";
                r = 0;
            }
        }
        int L = Hex.length();
        if (L > 0) {
            while (Hex.length() < 49) {
                Hex = Hex + " ";
            }
            System.out.print(Hex);
            System.out.println(S7.GetPrintableStringAt(Buffer, Size - r, r));
        } else {
            System.out.println();
        }
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
