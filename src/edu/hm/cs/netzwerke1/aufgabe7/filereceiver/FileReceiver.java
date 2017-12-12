package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class FileReceiver {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  public static void main(String... args) {
    
    FinateAutomaton automaton = new FinateAutomaton();
    try (DatagramSocket socket = new DatagramSocket(PORT)) {
      while (true) {
        DatagramPacket receivedPacket = new DatagramPacket(new byte[MAXUDPSEGMENTSIZE], MAXUDPSEGMENTSIZE);
        socket.receive(receivedPacket);
        automaton.processMsg(receivedPacket);
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
 
}
