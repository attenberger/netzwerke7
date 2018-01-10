package edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Channel that loses packets with a probability of 10 per cent.
 * @author Attenberger
 */
public class LostChannel implements UnreliableChannel {
  
  private static final double PROBABILITYLOST = 0.1;
  
  private UnreliableChannel channel;

  /**
   * Creates a new lost channel.
   * @param channel which should be decorated.
   */
  public LostChannel(UnreliableChannel channel) {
    this.channel = channel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	DatagramPacket packet = channel.receive();
	while (Math.random() < PROBABILITYLOST) {
		packet = channel.receive();
	}
    return packet;
  }

}
