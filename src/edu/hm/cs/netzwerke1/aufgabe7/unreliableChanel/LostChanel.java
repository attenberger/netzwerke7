package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.io.IOException;
import java.net.DatagramPacket;

public class LostChanel implements UnreliableChanel {
  
  private static final double PROBABILITYLOST = 0.1;
  
  private UnreliableChanel chanel;

  public LostChanel(UnreliableChanel chanel) {
    this.chanel = chanel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	DatagramPacket packet = chanel.receive();
	while (Math.random() < PROBABILITYLOST) {
		packet = chanel.receive();
	}
    return packet;
  }

}
