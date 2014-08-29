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

/**
 *
 * @author MaHi
 */
public class S7Telegram extends S7 {

    private byte[] PDU = new byte[2048];
    private int LastPDUType = 0;
    private int pduLength = 0;
    private static final int MinPduSize = 16;
    private static final int DefaultPduSizeRequested = 480;
    private static final int IsoHSize = 7; // TPKT+COTP Header Size
    private static final int MaxPduSize = DefaultPduSizeRequested + IsoHSize;

    public static final int LENGTH_READ = 31;
    public static final int SIZE_WRITE = 35;

    public S7Telegram() {

    }

    /**
     * Send a specify byte array into the outstream and further to the Server.
     *
     * @param Buffer - specfiy the Buffer that will be send
     * @param Len - specify the length of the Buffer
     * @param outStream - specify a valid DataOutputStream for the Request.
     * @throws ISOException
     */
    public void sendRequest(byte[] Buffer, int Len, DataOutputStream outStream) throws TCPException {
        try {
            outStream.write(Buffer, 0, Len);
            outStream.flush();
        } catch (IOException ex) {
            throw new TCPException("TCP Sending error.", ex);
        }
    }

    /**
     * This method check the Datainputstream for valid Data Unit and returns
     * correct Size of the PDU. That will be used by the most of Client and
     * Server methods.
     *
     * @param inStream - The InputStream which communictate/receive Date from
     * the TCP Socket
     * @return Size of the PDU (Data Unit)
     * @throws IOException
     * @throws InterruptedException
     */
    public int RecvIsoPacket(DataInputStream inStream) throws IOException,InterruptedException {
        Boolean Done = false;
        int Size = 0;
        
        while (!Done) {
            /* EverLoop möglich */
            receivePaket(inStream, PDU, 0, 4);               // Get TPKT (4 bytes) 
            Size = S7.GetWordAt(PDU, 2);
            if (Size == IsoHSize) {                          // Check 0 bytes Data Packet (only TPKT+COTP = 7 bytes)
                receivePaket(inStream, PDU, 4, 3);           // Skip remaining 3 bytes and Done is still false
            } else {
                if ((Size > MaxPduSize) || (Size < MinPduSize)) {
                    throw new ISOException("InvalidPDU");
                } else {
                    Done = true;                             // a valid length !=7 && >16 && <247
                }
            }
        }
        receivePaket(inStream, PDU, 4, 3);                   // Skip remaining 3 COTP bytes
        setLastPDUType(PDU[5]);                              // Stores PDU Type, we need it        
        receivePaket(inStream, PDU, 7, Size - IsoHSize);     // Receives the S7 Payload 
        
        return Size;
    }

    /**
     *
     * Reads up to Size bytes of data from the contained input stream into an
     * array of bytes.
     *
     * @param inStream - read the primitive Java data types
     * @param pdu - Data Unit that contained an array of bytes
     * @param start - Thats the Startpoint of reading bytes
     * @param len - The Endpoint of reading the bytes
     * @throws IOException
     * @throws InterruptedException
     */
    @SuppressWarnings("SleepWhileInLoop")
    private void receivePaket(DataInputStream inStream, byte[] pdu, int start, int len) throws IOException, InterruptedException {
        int Timeout = 2000;
        int cnt = 0;
        int SizeAvail = 0;
        int bytesRead = 0;

        /* Wait for Data */
        while ((SizeAvail < len) && (cnt < Timeout)) {
            cnt++;
            Thread.sleep(1);
            SizeAvail = inStream.available();
            // If timeout we clean the buffer
            if (cnt > Timeout && SizeAvail > 0) {
                inStream.read(pdu, start, SizeAvail);
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
     * @return
     * @throws ISOException
     * @throws IOException
     * @throws InterruptedException
     */
    public int NegotiatePduLength(DataOutputStream outStream, DataInputStream inStream) throws ISOException, IOException, InterruptedException {
        int length = 0;
        // Set PDU Size Requested
        S7.SetWordAt(S7_PN, 23, DefaultPduSizeRequested);
        // Sends the connection request telegram
        sendRequest(S7_PN, S7_PN.length, outStream);
        length = RecvIsoPacket(inStream);
        setPduLength(S7.GetWordAt(PDU, 25));
        // check S7 Error
        if ((length != 27) && (PDU[17] != 0) && (PDU[18] != 0) && getPduLength() <= 0) // 20 = size of Negotiate Answer
        {
            throw new ISOException("ISO error negotiating the PDU length.");
        }
        
        return pduLength;
    }

    /**
     * This Parameter are important and must set before you connect first time
     * to the PLC. Remember that rack and slot can depend from you
     * hardware-configuration. So Read the manuals.
     *
     * @param connectionType - specify the kind of Connection to the PLC see
     * below 
     * o 0x01 -> PG (the programming console) 
     * o 0x02 -> OP (the Siemens HMI panel) 
     * o 0x03 -> Basic (a generic data transfer connection).
     * @param rack - specify the rack for the plc (std = 0)
     * @param slot - specify the slot number for the plc (std = 2 ) 
     */
    public void setConnectionParams(byte connectionType, int rack, int slot) {
        int LocTSAP = 0x0100 & 0x0000FFFF;
        int RemTSAP = (connectionType << 8) + (rack * 0x20) + slot & 0x0000FFFF;

        ISO_CR[16] = (byte) (LocTSAP >> 8);          //Src TSAP HI (LocalTSAP_HI  will be overwritten)
        ISO_CR[17] = (byte) (LocTSAP & 0x00FF);      //Src TSAP LO (LocalTSAP_LO  will be overwritten)
        ISO_CR[20] = (byte) (RemTSAP >> 8);          //Dst TSAP HI (RemoteTSAP_HI will be overwritten)
        ISO_CR[21] = (byte) (RemTSAP & 0x00FF);      //Dst TSAP LO (RemoteTSAP_LO will be overwritten)
    }

    public int getLastPDUType() {
        return LastPDUType;
    }

    public void setLastPDUType(int LastPDUType) {
        this.LastPDUType = LastPDUType;
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

    // S7 PDU Negotiation Telegram (contains also ISO Header and COTP Header)
    public static final byte S7_PN[] = {
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x19,
        (byte) 0x02, (byte) 0xf0, (byte) 0x80, (byte) 0x32,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0xf0, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
        (byte) 0x1e
    };

    // S7 Read/Write Request Header (contains also ISO Header and COTP Header)
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

    // S7 Get Block Info Request Header (contains also ISO Header and COTP Header)
    public static final byte S7_BI[] = {
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

    // S7 STOP request
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

    // S7 HOT Start request
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

    // S7 COLD Start request
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

    // S7 Get PLC Status 
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

    // S7 Set Session Password 
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

    // S7 Clear Session Password 
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

}
