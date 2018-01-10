package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * FunctionalInterface for all possible actions after receiving a package.
 * @author Attenberger
 */
@FunctionalInterface
public interface Transition {
 
  /**
   * Executed after receiving a package
   * @param receivedPacket datagram packet received
   * @return new state of the finate automaton
   * @throws IOException thrown if an error occurs while sending ACK / NAK
   * or writing data to the filesystem.
   */
  State execute(DatagramPacket receivedPacket) throws IOException;
}
