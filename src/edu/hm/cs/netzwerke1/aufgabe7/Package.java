package edu.hm.cs.netzwerke1.aufgabe7;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Package to be sent over the network.
 */
public class Package {

	public static final int MAX_PACKAGE_SIZE = 1400;

	private boolean isOk;
	private int sequencenumber;
	private boolean isLast;
	private boolean isAck;
	private String filename;
	private byte[] content;

	public Package() {
		// Default constructor
	}

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

	public boolean isOk() {
		return isOk;
	}

	public int getSequencenumber() {
		return sequencenumber;
	}

	public void setSequencenumber(int sequencenumber) {
		this.sequencenumber = sequencenumber;
	}

	public boolean isLast() {
		return isLast;
	}

	public void setLast(boolean last) {
		isLast = last;
	}

	public boolean isStart() {
		return filename != null;
	}

	public boolean isAck() {
		return isAck;
	}

	public void setAck(boolean ack) {
		isAck = ack;
	}

	public byte getFilenameLength() {
		return filename != null ? (byte) filename.getBytes(StandardCharsets.UTF_8).length : 0;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public short getContentLength() {
		return content != null ? (short) content.length : 0;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * Get package as byte array.
	 *
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

	public int getLength() {
		return getFilenameLength() + getContentLength() + 8;
	}

	public int getRemainingSize() {
		return MAX_PACKAGE_SIZE - getLength();
	}

	/**
	 * Get the package as Datagram Packet.
	 *
	 * @return the datagram packet to be sent over the network
	 */
	public DatagramPacket toDatagramPacket() {
		byte[] rawData = getRawData();

		return new DatagramPacket(rawData, rawData.length);
	}

	/**
	 * Calculate Checksum for the passed byte array.
	 *
	 * @param data to calculate checksum from
	 * @return 32 Bit Checksum for the passed data array
	 */
	private static int calculateChecksum(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}

}
