package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.io.IOException;
import java.net.DatagramPacket;

public interface UnreliableChanel {
  
  DatagramPacket receive() throws IOException;
}
