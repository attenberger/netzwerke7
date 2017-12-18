package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.io.IOException;
import java.net.DatagramPacket;

public class DuplicateChanel implements UnreliableChanel {


  private static final double PROBABILITYDUPLICATE = 0.05;
  
  private UnreliableChanel chanel;
  private DatagramPacket packetToSendAgain = null;

  public DuplicateChanel(UnreliableChanel chanel) {
    this.chanel = chanel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	if (packetToSendAgain == null) {  
	  DatagramPacket packet = chanel.receive();
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
