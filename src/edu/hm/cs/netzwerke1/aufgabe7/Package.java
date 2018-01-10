package edu.hm.cs.netzwerke1.aufgabe7;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Package to be sent over the network.
 * The package contains no infromation about UDP or TCP.
 * @author Attenberger, Eder
 */
public class Package {

	public static final int MAX_PACKAGE_SIZE = 1400;

	private boolean isOk;
	private int sequencenumber;
	private boolean isLast;
	private boolean isAck;
	private String filename;
	private byte[] content;

	/**
	 * Creates a new empty Package.
	 */
	public Package() {
		// Default constructor
	}

	/**
	 * Creates a new Package out of a DatagramPacket.
	 * @param receivedPacket DatagramPacket received from the sender.
	 */
	public Package(DatagramPacket receivedPacket) {
		ByteBuffer buffer = ByteBuffer.wrap(receivedPacket.getData());

		int transmittedChecksum = buffer.getInt();
		int calculatedChecksum = calculateChecksum(Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength() - 4));
		isOk = transmittedChecksum == calculatedChecksum;

		if (isOk) {
			byte flags = buffer.get();
			sequencenumber = (flags & (byte) 0b10000000) == 0 ? 0 : 1;
			isLast = (flags & (byte) 0b01000000) == 0 ? true : false;
			isAck = (flags & (byte) 0b00100000) == 0 ? false : true;

			short filenameLength = buffer.get();
			if (filenameLength == 0)
				filename = null;
			else
				filename = new String(Arrays.copyOfRange(receivedPacket.getData(), 6, 6 + filenameLength), StandardCharsets.UTF_8);

			short contentLength = buffer.getShort(6 + filenameLength);
			content = Arrays.copyOfRange(receivedPacket.getData(), 8 + filenameLength, 8 + filenameLength + contentLength);
		}
	}

	/**
	 * Creates a new package to confirm a received package.
	 * @param isAck true if the package should be acknowledged.
	 * @param sequencenumber of the package that should be confirmed.
	 */
	public Package(boolean isAck, int sequencenumber) {
		if (sequencenumber > 1 || sequencenumber < 0)
			throw new IllegalArgumentException("It is an alternating-bit protocol. Only the sequencenumbers 0 and 1 are allowed.");
		this.sequencenumber = sequencenumber;
		this.isLast = true;
		this.isAck = isAck;
		this.isOk = true;
		this.filename = "";
		this.content = new byte[0];
	}

	/**
	 * Returns if the package does not contain bit errors.
	 * @return if the package does not contain bit errors
	 */
	public boolean isOk() {
		return isOk;
	}

	/**
	 * Returns the sequence number of the package or the package to acknowledge.
	 * @return sequence number of the package or the package to acknowledge
	 */
	public int getSequencenumber() {
		return sequencenumber;
	}

	/**
	 * Sets the sequence number for the package or the package to acknowledge.
	 * @param sequencenumber for the package or the package to acknowledge
	 */
	public void setSequencenumber(int sequencenumber) {
		this.sequencenumber = sequencenumber;
	}

	/**
	 * Returns if the package was the last of the current transmitted file.
	 * @return if the package was the last of the current transmitted file
	 */
	public boolean isLast() {
		return isLast;
	}

	/**
	 * Sets if the package is the last of the current transmitted file.
	 * @param last to set if the package is the last of the current transmitted file
	 */
	public void setLast(boolean last) {
		isLast = last;
	}

	/**
	 * Returns if the package is the start of a new file.
	 * @return if the package is the start of a new file
	 */
	public boolean isStart() {
		return filename != null;
	}

	/**
	 * Returns if the package acknowledged another transmitted package.
	 * @return if the package acknowledged another transmitted package
	 */
	public boolean isAck() {
		return isAck;
	}

	/**
	 * Sets if the package should acknowledge or not acknowledge another transmitted package.
	 * @param ack true if the package should be acknowledged
	 */
	public void setAck(boolean ack) {
		isAck = ack;
	}

	/**
	 * Returns the length of the filename.
	 * @return length of the filename
	 */
	public byte getFilenameLength() {
		return filename != null ? (byte) filename.getBytes(StandardCharsets.UTF_8).length : 0;
	}

	/**
	 * Returns the filename.
	 * @return filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Sets the filename for the file to transmit. The filename should only be set in the first package.
	 * @param filename of the file to transmit.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns the number of data bytes transmitted with the package.
	 * @return number of data bytes transmitted with the package
	 */
	public short getContentLength() {
		return content != null ? (short) content.length : 0;
	}

	/**
	 * Returns the content of the package as byte-Array.
	 * @return content of the package as byte-Array.
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * Sets the content of the package.
	 * @param content as byte-Array.
	 */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * Get package as byte array.
	 * @return package as byte array
	 */
	public byte[] getRawData() {
		ByteBuffer bufferWithoutChecksum = ByteBuffer.allocate(8 + getFilenameLength() + getContentLength());
		bufferWithoutChecksum.putInt(0); // Sequencenumber not calculated yet

		byte flags = 0;
		if (sequencenumber == 1)
			flags = (byte) (flags | (byte) 0b10000000);
		if (!isLast)
			flags = (byte) (flags | (byte) 0b01000000);
		if (isAck)
			flags = (byte) (flags | (byte) 0b00100000);
		bufferWithoutChecksum.put(flags);

		byte[] fileNameArray = filename != null ? filename.getBytes(StandardCharsets.UTF_8) : new byte[0];
		bufferWithoutChecksum.put((byte) fileNameArray.length);
		bufferWithoutChecksum.put(fileNameArray);

		bufferWithoutChecksum.putShort((short) content.length);
		bufferWithoutChecksum.put(content);

		int checksum = calculateChecksum(Arrays.copyOfRange(bufferWithoutChecksum.array(), 4, bufferWithoutChecksum.array().length - 4));

		ByteBuffer bufferWithChecksum = ByteBuffer.allocate(bufferWithoutChecksum.capacity());
		bufferWithChecksum.putInt(checksum);
		bufferWithChecksum.put(Arrays.copyOfRange(bufferWithoutChecksum.array(), 4, bufferWithoutChecksum.array().length));
		return bufferWithChecksum.array();
	}

	/**
	 * Returns the length of the package in bytes including the header.
	 * @return length of the package in bytes including header
	 */
	public int getLength() {
		return getFilenameLength() + getContentLength() + 8;
	}

	/**
	 * Returns the number of bytes, the package could be extended.
	 * @return number of bytes, the package could be extended
	 */
	public int getRemainingSize() {
		return MAX_PACKAGE_SIZE - getLength();
	}

	/**
	 * Get the package as Datagram Packet.
	 * @return the datagram packet to be sent over the network
	 */
	public DatagramPacket toDatagramPacket() {
		byte[] rawData = getRawData();

		return new DatagramPacket(rawData, rawData.length);
	}

	/**
	 * Calculate Checksum for the passed byte array.
	 * @param data to calculate checksum from
	 * @return 32 Bit Checksum for the passed data array
	 */
	private static int calculateChecksum(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(content);
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    result = prime * result + (isAck ? 1231 : 1237);
    result = prime * result + (isLast ? 1231 : 1237);
    result = prime * result + (isOk ? 1231 : 1237);
    result = prime * result + sequencenumber;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Package other = (Package) obj;
    if (!Arrays.equals(content, other.content))
      return false;
    if (filename == null) {
      if (other.filename != null)
        return false;
    } else if (!filename.equals(other.filename))
      return false;
    if (isAck != other.isAck)
      return false;
    if (isLast != other.isLast)
      return false;
    if (isOk != other.isOk)
      return false;
    if (sequencenumber != other.sequencenumber)
      return false;
    return true;
  }

}
