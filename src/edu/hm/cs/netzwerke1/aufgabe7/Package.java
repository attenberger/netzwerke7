package edu.hm.cs.netzwerke1.aufgabe7;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class Package {

  private boolean isOk;
  private int sequencenumber;
  private boolean isLast;
  private boolean isAck;
  private byte filenameLength;
  private String filename;
  private short contentLength;
  private byte[] content;
  
  
  public Package(DatagramPacket receivedPacket) {
    ByteBuffer buffer = ByteBuffer.wrap(receivedPacket.getData());
        
    int transmittedChecksum = buffer.getInt();
    int calculatedChecksum = calculateChecksum(Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength() - 4));
    isOk = transmittedChecksum == calculatedChecksum;

    if (isOk) {
      byte flags = buffer.get();
      sequencenumber = (flags & (byte)0b10000000) == 0 ? 0 : 1; 
      isLast = (flags & (byte)0b01000000) == 0 ? true : false;
      isAck = (flags & (byte)0b00100000) == 0 ? false : true;
      
      filenameLength = buffer.get();
      if (filenameLength == 0)
        filename = null;
      else
        filename = new String(Arrays.copyOfRange(receivedPacket.getData(), 6, 6 + filenameLength));
      
      contentLength = buffer.getShort(6 + filenameLength);
      content = Arrays.copyOfRange(receivedPacket.getData(), 8 + filenameLength, 8 + filenameLength + contentLength);
    }
  }
  
  public Package(boolean isAck, int sequencenumber) {
    if (sequencenumber > 1 || sequencenumber < 0)
      throw new IllegalArgumentException("It is an alternating-bit protocol. Only the sequencenumbers 0 and 1 are allowed.");
    this.sequencenumber = sequencenumber;
    this.isLast = true;
    this.isAck = isAck;
    this.isOk = true;
    this.filenameLength = 0;
    this.filename = "";
    this.contentLength = 0;
    this.content = new byte[0];
  }
  
  public boolean isOk() {
    return isOk;
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
    ByteBuffer bufferWithoutChecksum = ByteBuffer.allocate(8 + filenameLength + contentLength);
    bufferWithoutChecksum.putInt(0); // Sequencenumber not calculated yet
    
    byte flags = 0;
    if (sequencenumber == 1)
      flags = (byte)(flags | (byte)0b10000000);
    if (!isLast)
      flags = (byte)(flags | (byte)0b01000000);
    if (isAck)
      flags = (byte)(flags | (byte)0b00100000);
    bufferWithoutChecksum.put(flags);
    
    bufferWithoutChecksum.put(filenameLength);
    bufferWithoutChecksum.put(filename.getBytes());
    
    bufferWithoutChecksum.putShort(contentLength);
    bufferWithoutChecksum.put(content);
    
    int checksum = calculateChecksum(Arrays.copyOfRange(bufferWithoutChecksum.array(), 4, bufferWithoutChecksum.array().length - 4));
    
    ByteBuffer bufferWithChecksum = ByteBuffer.allocate(bufferWithoutChecksum.capacity()); 
    bufferWithChecksum.putInt(checksum);
    bufferWithChecksum.put(Arrays.copyOfRange(bufferWithoutChecksum.array(), 4, bufferWithoutChecksum.array().length));
    return bufferWithChecksum.array();
  }
  
  private static int calculateChecksum(byte[] data) {
    CRC32 crc = new CRC32();
    crc.update(data);
    return (int)crc.getValue();
  }
  
  
}
