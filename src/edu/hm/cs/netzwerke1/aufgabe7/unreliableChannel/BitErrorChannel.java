package edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Channel in which bit errors occurs with a propability of 5 per cent.
 * @author Attenberger
 */
public class BitErrorChannel implements UnreliableChannel {

  private static final double PROBABILITYBITERROR = 0.05;
  
  private UnreliableChannel channel;

  /**
   * Creates a new bit error channel.
   * @param channel which should be decorated.
   */
  public BitErrorChannel(UnreliableChannel channel) {
    this.channel = channel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	DatagramPacket packet = channel.receive();
	if (Math.random() < PROBABILITYBITERROR) {
	  byte[] data = packet.getData();
      byte errorpattern = 0b00001000;
      data[(int)(data.length * 0.5f)] = (byte)(data[(int)(data.length * 0.5f)] ^ errorpattern);
      packet.setData(data);
	}  
    return packet;
  }
}
