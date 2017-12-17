package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.net.DatagramPacket;

public class DuplicateChanel implements UnreliableChanel {


  private static final double PROBABILITYDUPLICATE = 0.05;
  
  private UnreliableChanel chanel;
  private DatagramPacket packetToSendAgain = null;

  public DuplicateChanel(UnreliableChanel chanel) {
    this.chanel = chanel;
  }

  @Override
  public DatagramPacket receive() {
    return chanel.receive();
  }

  @Override
  public void send(DatagramPacket packet) {
    chanel.send(packet);
    if (Math.random() < PROBABILITYDUPLICATE)
      chanel.send(packet);
  }
}
