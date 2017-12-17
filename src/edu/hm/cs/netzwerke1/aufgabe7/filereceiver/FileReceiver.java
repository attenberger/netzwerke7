package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel.*;

public class FileReceiver {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  private static UnreliableChanel unreliableChanel;
  private static FinateAutomaton automaton;
  
  public static void main(String... args) {
    automaton = new FinateAutomaton();
    unreliableChanel = new ReliableChanel();
    unreliableChanel = new DuplicateChanel(unreliableChanel);
    unreliableChanel = new LostChanel(unreliableChanel);
    unreliableChanel = new BitErrorChanel(unreliableChanel);
    
    socketReader.start();
    unreliableChanelReader.start();
  }
  
  private static Thread socketReader = new Thread() {
    @Override
    public void run() {
      try (DatagramSocket socket = new DatagramSocket(PORT)) {
        while (true) {
          DatagramPacket receivedPacket = new DatagramPacket(new byte[MAXUDPSEGMENTSIZE], MAXUDPSEGMENTSIZE);
          socket.receive(receivedPacket);
          synchronized (unreliableChanel) {
            unreliableChanel.send(receivedPacket);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(e.getMessage());
      }
    }
  };
  
  private static Thread unreliableChanelReader = new Thread() {
    @Override
    public void run() {
      DatagramPacket lastReceived = null;
      int sleeptime = 50;
      while (true) {
        synchronized (unreliableChanel) {
          lastReceived = unreliableChanel.receive();
        }
        if (lastReceived != null) {
          sleeptime = 50;
          try {
            automaton.processMsg(lastReceived);
          } catch (Exception e) {
            System.out.println(e.getMessage());
          }
        }
        else {
          try {
            sleeptime+= 50;
            sleeptime = sleeptime > 2000 ? 2000 : sleeptime;
            Thread.sleep(sleeptime);
          } catch (InterruptedException e) {}
        }
      }
    }
  };
 
}
