package edu.hm.cs.netzwerke1.aufgabe7;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Package {
  
  private final byte[] rawdata;
  
  private final long checksum;
  private final int sequencenumber;
  private final boolean isLast;
  private final boolean isAck;
  private final int filenameLength;
  private final String filename;
  private final int contentLength;
  private final byte[] content;
  private final boolean isCorrupt;
  
  
  public Package(DatagramPacket receivedPacket) {
    rawdata = receivedPacket.getData();
    ByteBuffer buffer = ByteBuffer.wrap(rawdata);
    checksum = (long)buffer.getInt() - Integer.MIN_VALUE;
    
    int flags = buffer.get() - Byte.MIN_VALUE;
    sequencenumber = flags >= 128 ? 1 : 0;
    flags = flags % 128;
    isLast = flags >= 64 ? false : true;
    flags = flags % 64;
    isAck = flags >= 32 ? true : false;
    flags = flags % 32;
    
    filenameLength = buffer.get() - Byte.MIN_VALUE;
    if (filenameLength == 0)
      filename = null;
    else {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<filenameLength; i++)
        sb.append(buffer.getChar());
      filename = sb.toString();
    }
    
    contentLength = buffer.getShort() - Short.MIN_VALUE;
    content = new byte[contentLength];
    for (int i=0; i<contentLength; i++)
      content[i] = buffer.get();
    
    CRC32 crc = new CRC32();
    crc.update(receivedPacket.getData(), 4, receivedPacket.getData().length - 4);
    isCorrupt = crc.getValue() != checksum ? true : false;
  }
  
  public Package(boolean isAck, int sequencenumber) {
    if (sequencenumber > 1 || sequencenumber < 0)
      throw new IllegalArgumentException("It is an alternating-bit protocol. Only the sequencenumbers 0 and 1 are allowed.");
    this.sequencenumber = sequencenumber;
    this.isLast = true;
    this.isAck = isAck;
    this.isCorrupt = false;
    this.filenameLength = 0;
    this.filename = null;
    this.contentLength = 0;
    this.content = new byte[0];
    
    byte[] rawdataToCalculate = new byte[8];
    int flags = 0;
    flags += sequencenumber == 1 ? 128 : 0;
    flags += isLast ? 0 : 64;
    flags += isAck ? 32 : 0;
    rawdataToCalculate[4] = (byte)(flags + Byte.MIN_VALUE);
    rawdataToCalculate[5] = 0;
    rawdataToCalculate[6] = 0;
    rawdataToCalculate[7] = 0;
    CRC32 crc = new CRC32();
    crc.update(rawdataToCalculate, 4, rawdataToCalculate.length - 4);
    this.checksum = crc.getValue();
    
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putInt((int)(checksum + Integer.MIN_VALUE));
    buffer.put((byte)(flags + Byte.MIN_VALUE));
    buffer.put((byte)0);
    buffer.put((byte)0);
    buffer.put((byte)0);
    this.rawdata = buffer.array();
  }
  
  public boolean isCorrupt() {
    return isCorrupt;
  }
  
  public boolean isStart() {
    return filenameLength != 0;
  }
  
  public boolean isLast() {
    return isLast;
  }
  
  public int getSequencenumber() {
    return sequencenumber;
  }
  
  public String getFilename() {
    return filename;
  }
  
  public byte[] getContent() {
    return content;
  }
  
  public byte[] getRawData() {
    return rawdata;
  }
}
