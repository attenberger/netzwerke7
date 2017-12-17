package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.net.DatagramPacket;

public class LostChanel implements UnreliableChanel {
  
  private static final double PROBABILITYLOST = 0.1;
  
  private UnreliableChanel chanel;

  public LostChanel(UnreliableChanel chanel) {
    this.chanel = chanel;
  }

  @Override
  public DatagramPacket receive() {
    return chanel.receive();
  }
  
  @Override
  public void send(DatagramPacket packet) {
    if (Math.random() >= PROBABILITYLOST) {
      chanel.send(packet);
    }
    // else lost
  }

}
