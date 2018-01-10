package edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Channel which produces duplicate packets with a probability of 5 per cent.
 * @author Attenberger
 */
public class DuplicateChannel implements UnreliableChannel {

  private static final double PROBABILITYDUPLICATE = 0.05;
  
  private UnreliableChannel channel;
  private DatagramPacket packetToSendAgain = null;

  /**
   * Creates a new duplicate channel.
   * @param channel which should be decorated.
   */
  public DuplicateChannel(UnreliableChannel channel) {
    this.channel = channel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	if (packetToSendAgain == null) {  
	  DatagramPacket packet = channel.receive();
	  if (Math.random() < PROBABILITYDUPLICATE) {
	    packetToSendAgain = packet;
	  }
	  return packet;
	}
	else {
	  DatagramPacket packet = packetToSendAgain;
	  packetToSendAgain = null;
	  return packet;
	}
  }
}
