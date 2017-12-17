package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.net.DatagramPacket;
import java.util.LinkedList;
import java.util.Queue;

public class ReliableChanel implements UnreliableChanel {

  
  private Queue<DatagramPacket> queue = new LinkedList<>();

  @Override
  public DatagramPacket receive() {
    return queue.poll();
  }
  
  public void send(DatagramPacket packet) {
    queue.add(packet);
  }
}
