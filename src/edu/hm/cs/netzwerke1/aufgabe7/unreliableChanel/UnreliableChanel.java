package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.net.DatagramPacket;

public interface UnreliableChanel {
  
  DatagramPacket receive();
  void send(DatagramPacket packet);
}
