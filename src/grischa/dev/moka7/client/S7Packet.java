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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 *
 * @author MaHi
 */
public class S7Packet {

    // Logging Exceptions and Errors
    private static final Logger logger = Logger.getLogger(S7Packet.class.getName());
    // Main Data Unit
    public final byte[] PDU = new byte[2048];
    private final S7Utility S7 = new S7Utility();
    // Dynamic Fields
    private int lastPDUType = 0;
    private int pduLength = 0;
    // Static Fields
    private static final int DEFAULT_PDU_SIZE_REQUESTED = 480;
    private static final int ISOHEADER_SIZE = 7;                                //IsoHeaderSize (TPKT+COTP)
    private static final int MIN_PDU_SIZE = 16;
    private static final int MAX_PDU_SIZE = DEFAULT_PDU_SIZE_REQUESTED + ISOHEADER_SIZE;

    public S7Packet() {
        logger.setLevel(Level.INFO);
    }

    
    /**
     * Send a specify byte array into the outstream and further to the Server.
     *
     * @param Buffer - specfiy the Buffer that will be send
     * @param len - specify the length of the Buffer
     * @param outStream - specify a valid DataOutputStream for the Request.
     * @throws grischa.dev.moka7.exceptions.TCPException
     */
    public void sendRequest(byte[] Buffer, int len, DataOutputStream outStream) throws TCPException {
        try {
            outStream.write(Buffer, 0, len);
            outStream.flush();
        } catch (IOException ex) {
            throw new TCPException("TCP Sending error.", ex);
        }
    }

    public void sendRequest(byte[] Buffer, DataOutputStream outStream) throws TCPException {
        sendRequest(Buffer, Buffer.length, outStream);
    }

    /**
     * This method check the Datainputstream for valid Data Unit and returns correct Size of the PDU. That
     * will be used by the most of Client and Server methods.
     *
     * @param inStream - The InputStream which communictate/receive Date from the TCP Socket
     * @return Size of the PDU (Data Unit)
     * @throws IOException
     */
    public int RecvIsoPacket(DataInputStream inStream) throws IOException {
        Boolean Done = false;
        int Size = 0;

        while (!Done) {
            /* EverLoop möglich */
            receivePaket(inStream, PDU, 0, 4);                          // Get TPKT Header (4 bytes) 
            Size = S7.GetWordAt(PDU, 2);
            if (Size == ISOHEADER_SIZE) {                               // Check 0 bytes Data Packet (only TPKT+COTP = 7 bytes)
                receivePaket(inStream, PDU, 4, 3);                      // Skip remaining 3 bytes and Done is still false
            } else {
                if ((Size > MAX_PDU_SIZE) || (Size < MIN_PDU_SIZE)) {   // a valid length !=7 && > 16 && < 247
                    throw new ISOException("Invalid S7 PDU received.");
                } else {
                    Done = true;
                }
            }
        }
        receivePaket(inStream, PDU, 4, 3);                              // Skip remaining 3 COTP bytes
        lastPDUType = PDU[5];                                           // Stores PDU Type, we need it        
        receivePaket(inStream, PDU, 7, Size - ISOHEADER_SIZE);          // Receives the S7Utility Payload 

        return Size;
    }

    /**
     *
     * Reads up to Size bytes of data from the contained input stream into an array of bytes.
     *
     * @param inStream - read the primitive Java data types
     * @param pdu - Data Unit that contained an array of bytes
     * @param start - Thats the Startpoint of reading bytes
     * @param len - The Endpoint of reading the bytes
     * @throws IOException
     * @throws InterruptedException
     */
    @SuppressWarnings("SleepWhileInLoop")
    private void receivePaket(DataInputStream inStream, byte[] pdu, int start, int len) throws IOException {
        int Timeout = 2000;
        int cnt = 0;
        int SizeAvail = 0;
        int bytesRead = 0;

        /* Wait for Data */
        SizeAvail = inStream.available();
        while ((SizeAvail < len) && (cnt < Timeout)) {
            cnt++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                logger.log(Level.ERROR, "Interrupped Exception", ex);
            }
            SizeAvail = inStream.available();
            // If timeout we clean the buffer
            if (cnt > Timeout && SizeAvail > 0) {
                inStream.read(PDU, start, SizeAvail);
                throw new TCPException("Data Receiving timeout." + inStream.toString());
            }
        }

        bytesRead = inStream.read(pdu, start, len);
        if (bytesRead == 0) {
            throw new TCPException("Connection reset by the peer.");
        }
    }
    /**
     * 
     * @param outStream
     * @param inStream
     * @return PDU Length
     * @throws IOException 
     */
    public int NegotiatePduLength(DataOutputStream outStream, DataInputStream inStream) throws IOException {
        int length = 0;
        // Set PDU Size Requested
        S7.SetWordAt(S7_PN, 23, DEFAULT_PDU_SIZE_REQUESTED);
        // Sends the connection request telegram
        sendRequest(S7_PN, S7_PN.length, outStream);
        length = RecvIsoPacket(inStream);
        setPduLength(S7.GetWordAt(PDU, 25));
        // check S7Utility Error
        if ((length != 27) && (PDU[17] != 0) && (PDU[18] != 0) && getPduLength() <= 0) // 20 = size of Negotiate Answer
        {
            throw new ISOException("ISO error negotiating the PDU length.");
        }

        return pduLength;
    }

    /**
     * This Parameter are important and must set before you connect first time to the PLC. Remember that rack
     * and slot can depend from you hardware-configuration. So Read the manuals.
     *
     * @param connectionType specify the kind of Connection to the PLC. Use the S7Utility Class to setup the Type. 
     * <ul>
     * <li>PG(the programming console)</li>
     * <li>OP(the Siemens HMI panel)</li> 
     * <li>S7_BASIC(a generic data transfer</li>
     * </ul>
     * connection)
     * @param rack specify the rack for the plc (std = 0)
     * @param slot specify the slot number for the plc (std = 2 )
     */
    public void setConnectionParams(short connectionType, int rack, int slot) {
        int LocTSAP = 0x0100 & 0x0000FFFF;
        int RemTSAP = (connectionType << 8) + (rack * 0x20) + slot & 0x0000FFFF;
        int a = S7Utility.S7_BASIC;
        ISO_CR[16] = (byte) (LocTSAP >> 8);          //Src TSAP HI (LocalTSAP_HI  will be overwritten)
        ISO_CR[17] = (byte) (LocTSAP & 0x00FF);      //Src TSAP LO (LocalTSAP_LO  will be overwritten)
        ISO_CR[20] = (byte) (RemTSAP >> 8);          //Dst TSAP HI (RemoteTSAP_HI will be overwritten)
        ISO_CR[21] = (byte) (RemTSAP & 0x00FF);      //Dst TSAP LO (RemoteTSAP_LO will be overwritten)  

    }
    
    
    public int getLastPDUType() {
        return lastPDUType;
    }

    public void setLastPDUType(int lastPDUType) {
        this.lastPDUType = lastPDUType;
    }

    public int getPduLength() {
        return pduLength;
    }

    public void setPduLength(int pduLength) {
        this.pduLength = pduLength;
    }

    // ISO Connection Request telegram (contains also ISO Header and COTP Header)
    public static final byte ISO_CR[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x16,
        (byte) 0x11, (byte) 0xE0, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xC0,
        (byte) 0x01, (byte) 0x0A, (byte) 0xC1, (byte) 0x02,
        (byte) 0x01, (byte) 0x00, (byte) 0xC2, (byte) 0x02,
        (byte) 0x01, (byte) 0x02
    };

    // S7Utility PDU Negotiation Telegram (contains also ISO Header and COTP Header)
    public static final byte S7_PN[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x19,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0xf0, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
        (byte) 0x1e
    };

    // S7Utility Read/Write Request Header (contains also ISO Header and COTP Header)
    public static final byte S7_RW[] = { // 31-35 bytes
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1f,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x00,
        (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x12,
        (byte) 0x0a, (byte) 0x10, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x84,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00,};

    // S7Utility Get Block Info Request Header (contains also ISO Header and COTP Header)
    public byte S7_BI[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x43, (byte) 0x03,
        (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00,
        (byte) 0x08, (byte) 0x30, (byte) 0x41, (byte) 0x30,
        (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30,
        (byte) 0x41
    };

    // SZL First telegram request   
    public static final byte S7_SZL_FIRST[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x44, (byte) 0x01,
        (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00
    };

    // SZL Next telegram request 
    public static final byte S7_SZL_NEXT[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x08, (byte) 0x12, (byte) 0x44, (byte) 0x01,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00,
        (byte) 0x00
    };

    // Get Date/Time request
    public static final byte S7_GET_DT[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1d,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x38,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x47, (byte) 0x01,
        (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00,
        (byte) 0x00
    };

    // Set Date/Time command
    public static final byte S7_SET_DT[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x27,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x89,
        (byte) 0x03, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x0e, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x47, (byte) 0x02,
        (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00,
        (byte) 0x0a, (byte) 0x00, (byte) 0x19, (byte) 0x13,
        (byte) 0x12, (byte) 0x06, (byte) 0x17, (byte) 0x37,
        (byte) 0x13, (byte) 0x00, (byte) 0x01
    };

    // S7Utility STOP request
    public static final byte S7_STOP[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0e,
        (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00,
        (byte) 0x00, (byte) 0x29, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09,
        (byte) 0x50, (byte) 0x5f, (byte) 0x50, (byte) 0x52,
        (byte) 0x4f, (byte) 0x47, (byte) 0x52, (byte) 0x41,
        (byte) 0x4d
    };

    // S7Utility HOT Start request
    public static final byte S7_HOT_START[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0c,
        (byte) 0x00, (byte) 0x00, (byte) 0x14, (byte) 0x00,
        (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0xfd, (byte) 0x00, (byte) 0x00, (byte) 0x09,
        (byte) 0x50, (byte) 0x5f, (byte) 0x50, (byte) 0x52,
        (byte) 0x4f, (byte) 0x47, (byte) 0x52, (byte) 0x41,
        (byte) 0x4d
    };

    // S7Utility COLD Start request
    public static final byte S7_COLD_START[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x27,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
        (byte) 0x00, (byte) 0x00, (byte) 0x16, (byte) 0x00,
        (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0xfd, (byte) 0x00, (byte) 0x02, (byte) 0x43,
        (byte) 0x20, (byte) 0x09, (byte) 0x50, (byte) 0x5f,
        (byte) 0x50, (byte) 0x52, (byte) 0x4f, (byte) 0x47,
        (byte) 0x52, (byte) 0x41, (byte) 0x4d
    };

    // S7Utility Get PLC Status 
    public static final byte S7_GET_STAT[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x2c,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x44, (byte) 0x01,
        (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00,
        (byte) 0x04, (byte) 0x04, (byte) 0x24, (byte) 0x00,
        (byte) 0x00
    };

    // S7Utility Set Session Password 
    public static final byte S7_SET_PWD[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x27,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x45, (byte) 0x01,
        (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00
    };

    // S7Utility Clear Session Password 
    public static final byte S7_CLR_PWD[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1d,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x29,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12,
        (byte) 0x04, (byte) 0x11, (byte) 0x45, (byte) 0x02,
        (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00,
        (byte) 0x00
    };
    
    public static final byte RW_MVARS[] = {
/*     (byte) 0x03, (byte) 0x00,                            // Version 3 std
       (byte) 0x00, (byte) 0x4f,                            // TPTK Länge     - variable
       (byte) 0x02, (byte) 0xf0, (byte) 0x80,               // ISO Länge  / PDU Type  / Destination Ref.
       // S7 Communication Header
       (byte) 0x32,                                         // Protokol ID                
       (byte) 0x01,                                         // ROSCTR Job
       (byte) 0x00, (byte) 0x00,                            // Redundance Identifikation
       (byte) 0x05, (byte) 0x00,                            // Protokol Data Unit Reference - variable
       (byte) 0x00, (byte) 0x3e,                            // Parameter Length
       (byte) 0x00, (byte) 0x00,                            // Data length
       // S7 Parameter
       (byte) 0x04,                                         // Function Read Var
       (byte) 0x05,                                         // Anzahl Items

       // Item 2 ... Copy Paste   
 */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x4f,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x00,
        (byte) 0x00, (byte) 0x04, (byte) 0x03, 
    };
    
    public static final byte[] ITEM = {
        // Item                                            
       (byte) 0x12,                                         // Variable Specfikation
       (byte) 0x0a,                                         // Length of follwing address specfikation
       (byte) 0x10,                                         // Syntax ID
       (byte) 0x02,                                         // Transport Size
       (byte) 0x00, (byte) 0x02,                            // Length - variable
       (byte) 0x00, (byte) 0x65,                            // DB Number - variable
       (byte) 0x84,                                         // Area Data Blocks - variable
       (byte) 0x00, (byte) 0x00,(byte) 0x00               // Adress

    };
}
