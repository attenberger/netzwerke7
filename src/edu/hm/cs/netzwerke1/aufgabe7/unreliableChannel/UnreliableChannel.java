package edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel;

import java.io.IOException;
import java.net.DatagramPacket;

public interface UnreliableChannel {
  
  DatagramPacket receive() throws IOException;
}
