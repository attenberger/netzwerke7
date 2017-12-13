package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.IOException;
import java.net.DatagramPacket;

@FunctionalInterface
public interface Transition {
  State execute(DatagramPacket receivedPacket) throws IOException;
}
